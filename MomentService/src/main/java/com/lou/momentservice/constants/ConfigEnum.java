package com.lou.momentservice.constants;

import lombok.Getter;
import org.apache.commons.lang3.ObjectUtils;

/**
 * @ClassName ConfigEnum
 * @Description TODO
 * @Author Lou
 * @Date 2025/6/1 15:11
 */

@Getter
public enum ConfigEnum {

    TOKEN_SECRET_KEY("lou"),

    PASSWORD_ASLT("lou"),

    WX_STATE("lou"),

    WORKED_ID( "1"),

    DATACENTER_ID( "1"),

    IMAGE_URI("http://http://14.103.140.112:9090/infinite-chat/"),

    IMAGE_PATH("/img/avatar"),

    NOTICE_URL("/api/v1/message/push/moment"),

    MEDIA_TYPE("application/json; charset=utf-8"),

    MINIO_SERVER_URL("http://http://14.103.140.112:9090"),

    MINIO_ACCESS_KEY("minioadmin"),

    MINIO_SECRET_KEY("minioadmin"),

    MINIO_BUCKET_NAME("infinite-chat"),

    REQUEST_SUCCESSFUL("请求成功");


    private final String value;

    ConfigEnum(String value) {

        this.value = value;
    }




    public String getValue() {
        return value;
    }
}
