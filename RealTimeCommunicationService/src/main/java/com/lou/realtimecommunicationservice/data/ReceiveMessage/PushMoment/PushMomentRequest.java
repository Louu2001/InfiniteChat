package com.lou.realtimecommunicationservice.data.ReceiveMessage.PushMoment;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

@Data
@Accessors(chain = true)
public class PushMomentRequest {
    private List<Long> receiveUserIds;

    private Integer noticeType;

    private String avatar;

    private Integer total;
}