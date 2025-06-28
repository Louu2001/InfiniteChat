package com.lou.contactservice.constants;

import lombok.Getter;

@Getter
public enum UrlEnum {
    PUSH_NEW_SESSION("/api/v1/message/push/newSession/"),
    PUSH_NEW_APPLY("/api/v1/message/push/friendApplication/"),
    PUSH_NEW_GROUP_SESSION("/api/v1/message/push/newGroupSession/");

    private final String url;

    UrlEnum(String url) {
        this.url = url;
    }

}
