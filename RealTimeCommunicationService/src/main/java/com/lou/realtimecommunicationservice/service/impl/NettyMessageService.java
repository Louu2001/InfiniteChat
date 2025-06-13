package com.lou.realtimecommunicationservice.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.json.JSONUtil;
import com.lou.realtimecommunicationservice.constants.MessageRcvTypeEnum;
import com.lou.realtimecommunicationservice.constants.PushTypeEnum;
import com.lou.realtimecommunicationservice.data.ReceiveMessage.ReceiveMessageRequest;
import com.lou.realtimecommunicationservice.excption.ServiceException;
import com.lou.realtimecommunicationservice.model.*;
import com.lou.realtimecommunicationservice.websocket.ChannelManager;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @ClassName NettyMessageService
 * @Description TODO
 * @Author Lou
 * @Date 2025/6/10 16:48
 */

@Service
@Slf4j
public class NettyMessageService {

    public void sendPush(PushTypeEnum pushType, Object data, String receiveUserUuid) {
        if (pushType == null || data == null || receiveUserUuid == null) {
            log.error("推送消息的类型、数据或接受用户UUID为空");
            return;
        }

        MessageDTO messageDTO = new MessageDTO();
        messageDTO.setType(pushType.getCode()).setData(data);

        Channel channel = ChannelManager.getChannelByUserId(receiveUserUuid);
        log.info("channel:{}", channel);
        if (channel != null && channel.isActive()) {
            log.info("准备发送消息，channel 状态: active={}, id={}, 发送内容: {}",
                    channel.isActive(),
                    channel.id(),
                    JSONUtil.toJsonStr(messageDTO));
            //创建 WebSocket 帧
            TextWebSocketFrame frame = new TextWebSocketFrame(JSONUtil.toJsonStr(messageDTO));
            System.out.println("Frame: " + frame);
            //发送消息并添加监视器来处理发送结果
            channel.writeAndFlush(frame).addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture future) throws Exception{
                    if (future.isSuccess()){
                        log.info("消息发送成功:{}",messageDTO);
                    }else {
                        log.error("消息发送失败:{}",future.cause());
                    }
                }
            });
        }
    }

    public void sendMessageToUser(ReceiveMessageRequest message) {
        switch (MessageRcvTypeEnum.fromCode(message.getType())) {
            case TEXT_MESSAGE:
                TextMessage textMessage = new TextMessage();
                BeanUtils.copyProperties(message, textMessage);
                TextMessageBody textBean = BeanUtil.toBean(message.getBody(), TextMessageBody.class);
                textMessage.setBody(textBean);
                log.info("textMessage:{}", textMessage);
                List<Long> textReceiveUserIds = textMessage.getReceiveUserIds();
                textMessage.setReceiveUserIds(null);

                for (Long textReceiveUser : textReceiveUserIds) {
                    log.info("textReceiveUser:{}", textReceiveUser);
                    log.info("是否存在管道:{}", ChannelManager.getChannelByUserId(textReceiveUser.toString()));
                    if (ChannelManager.getChannelByUserId(textReceiveUser.toString()) != null) {
                        log.info("调用 sendPush: {}", textReceiveUser);
                        sendPush(PushTypeEnum.MOMENT_NOTIFICATION, textMessage, textReceiveUser.toString());
                    }
                }
                break;
            case PICTURE_MESSAGE:
                PictureMessage pictureMessage = new PictureMessage();
                BeanUtils.copyProperties(message, pictureMessage);
                PictureMessageBody pictureBean = BeanUtil.toBean(message.getBody(), PictureMessageBody.class);
                pictureMessage.setBody(pictureBean);
                log.info("pictureMessage:{}", pictureMessage);
                List<Long> pictureReceiveUserIds = pictureMessage.getReceiveUserIds();
                for (Long pictureReceiveUser : pictureReceiveUserIds) {
                    if (ChannelManager.getChannelByUserId(pictureReceiveUser.toString()) != null) {
                        sendPush(PushTypeEnum.MOMENT_NOTIFICATION, pictureMessage, pictureReceiveUser.toString());
                    }
                }

        }

    }
}
