package com.lou.offlinedatastoreservice.service.impl;

import cn.hutool.core.lang.hash.Hash;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.yulichang.wrapper.MPJLambdaWrapper;
import com.lou.offlinedatastoreservice.common.TextMessage;
import com.lou.offlinedatastoreservice.constants.config.ConfigEnum;
import com.lou.offlinedatastoreservice.data.offlineMessage.*;
import com.lou.offlinedatastoreservice.model.Message;
import com.lou.offlinedatastoreservice.model.RedPacket;
import com.lou.offlinedatastoreservice.model.Session;
import com.lou.offlinedatastoreservice.model.User;
import com.lou.offlinedatastoreservice.service.MessageService;
import com.lou.offlinedatastoreservice.mapper.MessageMapper;
import com.lou.offlinedatastoreservice.service.SessionService;
import com.lou.offlinedatastoreservice.service.UserSessionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * @author loujun
 * @description 针对表【message】的数据库操作Service实现
 * @createDate 2025-06-19 23:51:28
 */
@Service
@Slf4j
public class MessageServiceImpl extends ServiceImpl<MessageMapper, Message> implements MessageService {

    @Autowired
    private MessageMapper messageMapper;

    @Autowired
    private UserSessionService userSessionService;

    @Autowired
    private SessionService sessionService;

    @Override
    public void saveOfflineMessage(String message) {
        TextMessage textMessage = JSONUtil.toBean(message, TextMessage.class);
        Message msg = new Message();
        BeanUtils.copyProperties(textMessage, msg);

        msg.setContent(textMessage.getBody().getContent())
                .setReplyId(textMessage.getBody().getReplyId())
                .setSenderId(textMessage.getSendUserId());
        log.info("Received Message:" + msg);

        int insert = this.baseMapper.insert(msg);

        if (insert <= 0) {
            throw new RuntimeException("保存离线消息失败");
        }
    }

    @Override
    public OfflineMsgResponse getOfflineMessage(OfflineMsgRequest request) {
        Set<Long> sessionIds = userSessionService.findSessionIdByUserId(request.getUserId());

        OfflineMsgResponse offlineMsgResponse = new OfflineMsgResponse();

        // 没有聊天，直接返回
        if (sessionIds.isEmpty()) {
            return offlineMsgResponse;
        }

        // 否则去获取session中的消息
        HashMap<Long, List<OfflineMsgDetail>> offlineMsgDetails = this.findOfflineMsgBySessionId(sessionIds, request.getTime());
        List<Session> sessions = sessionService.listByIds(sessionIds);
        List<OfflineMessage> offlineMessages = new ArrayList<>();

        for (Session session : sessions) {
            OfflineMessage offlineMsg = new OfflineMessage();
            offlineMsg.setSessionId(session.getId().toString())
                    .setSessionType(session.getType());

            if (Objects.equals(session.getType(), Integer.valueOf(ConfigEnum.GROUP_TYPE.getValue()))) {
                offlineMsg.setSessionAvatar(ConfigEnum.GROUP_AVATAR.getValue())
                        .setSessionName(session.getName());
            }


            List<OfflineMsgDetail> offlineMsgList = offlineMsgDetails.get(session.getId());
            if (offlineMsgList != null) {
                offlineMsg.setOfflineMsgDetails(offlineMsgList)
                        .setTotal((long) offlineMsgList.size());

                offlineMessages.add(offlineMsg);
            }
        }
        offlineMsgResponse.setOfflineMsg(offlineMessages);
        return offlineMsgResponse;
    }

    public HashMap<Long, List<OfflineMsgDetail>> findOfflineMsgBySessionId(Set<Long> sessionId, String time) {
        HashMap<Long, List<OfflineMsgDetail>> messageMap = new HashMap<>();

        LocalDateTime dateTime = LocalDateTime.parse(time, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        MPJLambdaWrapper<Message> wrapper = new MPJLambdaWrapper<Message>()
                .selectAll(Message.class)
                .selectAll(RedPacket.class)
                .selectAll(User.class)
                .selectAssociation(RedPacket.class, Message::getRedPacket)
                .selectAssociation(User.class, Message::getUser)
                .in("t.session_id", sessionId)
                .ge("t.created_at", dateTime)
                .leftJoin(RedPacket.class, RedPacket::getRedPacketId, Message::getContent)
                .leftJoin(User.class, User::getUserId, Message::getSenderId);

        List<Message> messages = messageMapper.selectJoinList(Message.class, wrapper);

        log.info("message:{}", messages);

        for (Message message : messages) {
            OfflineMsgDetail offlineMsgDetail = new OfflineMsgDetail();
            OfflineMsgBody offlineMsgBody = new OfflineMsgBody();

            SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

            offlineMsgBody.setCreatedAt(formatter.format(message.getCreatedAt()))
                    .setContent(message.getContent());

            if (message.getReplyId() != null) {
                offlineMsgBody.setReplyId(message.getReplyId().toString());
            }

            offlineMsgDetail.setOfflineMsgBody(offlineMsgBody);

            // 如果是红包，则设置红包封面文案
            if (message.getType().equals(Integer.valueOf(ConfigEnum.MESSAGE_TYPE.getValue()))) {
                OfflineRedPacketMsgBody body = new OfflineRedPacketMsgBody(message.getRedPacket().getRedPacketWrapperText());
                BeanUtils.copyProperties(offlineMsgBody, body);
                offlineMsgDetail.setOfflineMsgBody(body);
            }

            // 设置发送人信息
            offlineMsgDetail.setUserName(message.getUser().getUserName())
                    .setAvatar(message.getUser().getAvatar());

            // 设置消息详情信息
            offlineMsgDetail.setMessageId(message.getMessageId().toString())
                    .setSendUserId(message.getSenderId().toString())
                    .setType(message.getType());

            // 如果不存在当前 sessionId 则需要初始化
            if (!messageMap.containsKey(message.getSessionId())) {
                messageMap.put(message.getSessionId(), new ArrayList<>());
            }

            messageMap.get(message.getSessionId()).add(offlineMsgDetail);
        }
        return messageMap;
    }
}




