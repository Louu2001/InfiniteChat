package com.lou.messagingservice.service.impl;

import cn.hutool.core.lang.Snowflake;
import cn.hutool.core.util.IdUtil;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lou.messagingservice.common.ServiceException;
import com.lou.messagingservice.constants.ConfigEnum;
import com.lou.messagingservice.constants.SessionType;
import com.lou.messagingservice.constants.UserConstants;
import com.lou.messagingservice.data.sendMsg.AppMessage;
import com.lou.messagingservice.data.sendMsg.SendMsgRequest;
import com.lou.messagingservice.data.sendMsg.SendMsgResponse;
import com.lou.messagingservice.mapper.FriendMapper;
import com.lou.messagingservice.mapper.MessageMapper;
import com.lou.messagingservice.model.Friend;
import com.lou.messagingservice.model.Message;
import com.lou.messagingservice.model.Session;
import com.lou.messagingservice.model.User;
import com.lou.messagingservice.service.MessageService;
import com.lou.messagingservice.service.SessionService;
import com.lou.messagingservice.service.UserService;
import com.lou.messagingservice.service.UserSessionService;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.BeanUtils;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;


@Service
@Slf4j
public class MessageServiceImpl extends ServiceImpl<MessageMapper, Message> implements MessageService {

    private static final int CORE_POOL_SIZE = 5;
    private static final int MAX_POOL_SIZE = 10;
    private static final long KEEP_ALIVE_TIME = 60L;
    private static final int QUEUE_CAPACITY = 100;


    private static final String DEFAUL_SESSION_AVATAR = "http://120.26.15.45:9000/infinite-chat/IMG_5876.jpg";

    private static final String TIME_ZONE_SHANGHAI = "Asia/Shanghai";

    private static final int STATUS_ACTIVE = 1;

    private final UserService userService;

    private final FriendMapper friendMapper;

    private final UserSessionService userSessionService;

    private final SessionService sessionService;

    private final DiscoveryClient discoveryClient;

    private final RedisTemplate<String, String> redisTemplate;

    private final OkHttpClient httpClient = new OkHttpClient();

    private final ThreadPoolExecutor groupMessageExecutor;


    public MessageServiceImpl(UserService userService, FriendMapper friendMapper, UserSessionService userSessionService,
                              DiscoveryClient discoveryClient, SessionService sessionService,
                              RedisTemplate<String, String> redisTemplate) {
        this.userService = userService;
        this.friendMapper = friendMapper;
        this.userSessionService = userSessionService;
        this.discoveryClient = discoveryClient;
        this.sessionService = sessionService;
        this.redisTemplate = redisTemplate;
        this.groupMessageExecutor = new ThreadPoolExecutor(
                CORE_POOL_SIZE,
                MAX_POOL_SIZE,
                KEEP_ALIVE_TIME,
                TimeUnit.SECONDS,
                new LinkedBlockingDeque<>(QUEUE_CAPACITY),
                new ThreadPoolExecutor.CallerRunsPolicy()
        );
    }

    @Override
    public SendMsgResponse sendMessage(SendMsgRequest request) {
        // 1.校验用户是否存在
        validateSender(request.getSendUserId());

        // 2.判断单聊还是群聊，群聊去获取用户名单
        List<Long> receiveUserIds = getReceiveUserIds(request);
        validateReceiveUserIds(receiveUserIds);

        // 3.构建消息
        AppMessage appMessage = buildAppMessage(request, receiveUserIds);
        Long messageId = generateMessageId();
        Date createdAt = new Date();
        appMessage.setMessageId(messageId).setCreated(formatDate(createdAt));

        // TODO: 发送到kafka
        // TODO: 转发给RealtimeCommunicationService
        // 4.通过redis查询接收者的netty服务器在哪
        sendRealTimeMessage(request, appMessage, createdAt);

        return buildAppMessage(appMessage);

        // 5.发消息

    }

    private void sendRealTimeMessage(SendMsgRequest sendMsgRequest, AppMessage appMessage, Date createdAt) {
        String json = JSON.toJSONString(appMessage);
        String nettyServerIP = redisTemplate.opsForValue().get(UserConstants.USER_SESSION + sendMsgRequest.getSendUserId().toString());
        RequestBody requestBody = RequestBody.create(
                MediaType.parse(ConfigEnum.MEDIA_TYPE.getValue()),
                json
        );

        List<ServiceInstance> instances = discoveryClient.getInstances("RealTimeCommunicationService");
        if (instances.isEmpty()) {
            throw new ServiceException("没有可用的RealTimeCommunication服务实例");
        }

        if (sendMsgRequest.getSessionType() == SessionType.SINGLE.getValue()) {
            sendSingleMessage(sendMsgRequest, requestBody, nettyServerIP);
        } else {
            sendGroupMessage(instances, requestBody, nettyServerIP);
        }
    }

    private void sendSingleMessage(SendMsgRequest sendMsgRequest, RequestBody requestBody, String nettyServerIP) {
        String receiveUserId = String.valueOf(sendMsgRequest.getReceiveUserId());
        String nettyUri = "Nacos:" + receiveUserId;

//        CompletableFuture<String> urlFuture = userLogoutListener.cache.get(nettyUri,
//                key -> redisTemplate.opsForValue().get(nettyUri));
//        log.info("单聊接收者ID: {}, Netty URI: {}",receiveUserId,nettyUri);
        try {
            if (nettyServerIP != null) {
                Request request = new Request.Builder()
                        .url("http://" + nettyServerIP + ":8083" +  ConfigEnum.MSG_URL.getValue())
                        .post(requestBody)
                        .build();
                executeHttpRequest(request);
            } else {
                log.info("接收者已下线: {}", receiveUserId);
            }
        } catch (Exception e) {
            log.error("发送单聊消息失败: {}", e.getMessage());
            throw new ServiceException("发送单聊消息失败");
        }
    }

    private void sendGroupMessage(List<ServiceInstance> instances, RequestBody requestBody, String token) {
        for (ServiceInstance instance : instances) {
            groupMessageExecutor.submit(() -> {
                String url = instance.getUri().toString();
                Request request = new Request.Builder()
                        .url(url + ConfigEnum.MSG_URL.getValue())
                        .post(requestBody)
                        .addHeader("Authorization", token)
                        .build();
                try {
                    executeHttpRequest(request);
                    log.info("成功发送群聊消息到 {}", url);
                } catch (Exception e) {
                    log.error("发送群聊消息到 {} 失败: {}", url, e.getMessage());
                    //根据需求，可以在此处添加重试机制活着其他错误处理逻辑
                }
            });
        }
    }

    private void executeHttpRequest(Request request) throws IOException {
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Http请求失败:" + response);
            }
            ResponseBody responseBody = response.body();
            if (responseBody != null) {
                String responseString = responseBody.string();
                //处理响应内容
                log.info("HTTP响应: {}", responseString);
            }
        }
    }

    private SendMsgResponse buildAppMessage(AppMessage appMessage) {
        SendMsgResponse responseMsgVo = new SendMsgResponse();
        BeanUtils.copyProperties(appMessage, responseMsgVo);
        responseMsgVo.setSessionId(String.valueOf(appMessage.getSessionId()));
        responseMsgVo.setCreatedAt(appMessage.getCreated());

        log.info("消息 appMessage: {}", appMessage);
        log.info("消息 responseMsgVo: {}", responseMsgVo);

        return responseMsgVo;
    }

    private Long generateMessageId() {
        Snowflake snowflake = IdUtil.getSnowflake(
                Integer.parseInt(ConfigEnum.WORKED_ID.getValue()),
                Integer.parseInt(ConfigEnum.DATACENTER_ID.getValue())
        );
        return snowflake.nextId();
    }

    private void validateSender(Long sendUserId) {
        User sendUser = userService.getById(sendUserId);
        log.info("发送者状态: {}", sendUserId);
        if (sendUser == null || sendUser.getStatus() != STATUS_ACTIVE) {
            throw new ServiceException("发送者状态异常");
        }
    }

    private void validateReceiveUserIds(List<Long> receiveUserIds) {
        if (receiveUserIds == null || receiveUserIds.isEmpty()) {
            throw new ServiceException("接收者列表不能为空");
        }
    }

    private List<Long> getReceiveUserIds(SendMsgRequest sendMsgRequest) {
        List<Long> receiveUserIds = new ArrayList<>();
        int sessionType = sendMsgRequest.getSessionType();

        if (sessionType == SessionType.SINGLE.getValue()) {
            Long receiveUserId = sendMsgRequest.getReceiveUserId();
            receiveUserIds.add(receiveUserId);
            validateSingleSession(sendMsgRequest.getSendUserId(), receiveUserId);
        } else {
            receiveUserIds.addAll(userSessionService.getUserIdsBySessionId(sendMsgRequest.getSessionId()));
            log.info("群聊接收者列表: {}", receiveUserIds);
            boolean removed = receiveUserIds.remove(sendMsgRequest.getSendUserId());
            if (removed) {
                log.info("移除发送者后的接收者列表: {}", receiveUserIds);
            } else {
                throw new ServiceException("发送者不在群聊内");
            }
        }
        return receiveUserIds;
    }


    private void validateSingleSession(Long sendUserId, Long receiveUserId) {
        User receiveUser = userService.getById(receiveUserId);

        if (receiveUser == null || receiveUser.getStatus() != STATUS_ACTIVE) {
            throw new ServiceException("接收者" + receiveUserId + "状态异常");
        }

        Friend friend = friendMapper.selectFriendship(sendUserId, receiveUserId);
        log.info("发送者ID: {}, 接收者ID: {}", sendUserId, receiveUserId);
        if (friend == null || friend.getStatus() != STATUS_ACTIVE) {
            throw new ServiceException("发送者" + sendUserId + "与接收者" + receiveUserId + "不是好友关系");
        }
    }

    private AppMessage buildAppMessage(SendMsgRequest sendMsgRequest, List<Long> receiveUserIds) {
        AppMessage appMessage = new AppMessage();
        BeanUtils.copyProperties(sendMsgRequest, appMessage);
        appMessage.setBody(sendMsgRequest.getBody()).setReceiveUserIds(receiveUserIds);

        User senderUser = userService.getById(sendMsgRequest.getSendUserId());
        appMessage.setAvatar(senderUser.getAvatar()).setUserName(senderUser.getUserName());

        Session session = sessionService.getById(sendMsgRequest.getSessionId());
        log.info("会话ID: {}", sendMsgRequest.getSessionId());
        log.info("会话信息: {}", session);

        if (appMessage.getSessionType() == SessionType.SINGLE.getValue()) {
            appMessage.setAvatar(null).setSeesionName(null);
        } else {
            appMessage.setSessionAvatr(DEFAUL_SESSION_AVATAR).setSeesionName(session.getName());
        }

        log.info("Appmessage: {}", appMessage);
        return appMessage;
    }


    private String formatDate(Date date) {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        formatter.setTimeZone(TimeZone.getTimeZone(TIME_ZONE_SHANGHAI));
        return formatter.format(date);
    }

}
