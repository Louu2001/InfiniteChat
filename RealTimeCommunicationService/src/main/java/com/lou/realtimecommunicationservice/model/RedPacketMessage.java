package com.lou.realtimecommunicationservice.model;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class RedPacketMessage extends Message {

    private RedPacketMessageBody body;

    @Override
    public String toString() {
        return super.toString();
    }
}
