package com.lou.realtimecommunicationservice.data.ReceiveMessage;

import lombok.Data;
import lombok.experimental.Accessors;
import org.hibernate.validator.constraints.Length;

import java.util.List;

/**
 * @ClassName ReceiveMessageRequest
 * @Description TODO
 * @Author Lou
 * @Date 2025/6/10 16:36
 */

@Data
@Accessors
public class ReceiveMessageRequest {

    @Length
    private List<Long> receiveUserIds;

    private String sendUserId;

    private String sessionId;

    private String avatar;

    private String username;

    private Integer type;

    private String messageId;

    private Integer sessionType;

    private String sessionName;

    private String sessionAvatar;

    private String createdAt;

    private Object body;
}
