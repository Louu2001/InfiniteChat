package com.lou.messagingservice.constants;

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

    TOKEN_SECRET_KEY("tokenSecretKey", "lou"),

    WORKED_ID("workedId", "1"),

    DATACENTER_ID("DATACENTER_ID", "1"),

    IMAGE_URI("imageUri","http://http://14.103.140.112:9090/infinite-chat/img/avatar"),

    MEDIA_TYPE("mediaType","application/json; charset=utf-8"),

    MSG_URL("msgUrl","/api/v1/message/user/"),  //RealTimeCommunicationService服务推送接口

    KAFKA_TOPICS("kafkaTopcis","thousands_word_message"),

    HTTP_CONFIG("httpConfig","application/json; charset=utf-8"),

    IMAGE_PATH("imagePath","/home/img/avatar");

    private final String value;
    private final String text;

    ConfigEnum(String text, String value) {
        this.text = text;
        this.value = value;
    }

    public static ConfigEnum getEnumByValue(String value) {
        if (ObjectUtils.isEmpty(value)) {
            return null;
        }
        for (ConfigEnum anEnum : ConfigEnum.values()) {
            if (anEnum.getValue().equals(value)) {
                return anEnum;
            }
        }
        return null;
    }

    public String getText() {
        return text;
    }


    public String getValue() {
        return value;
    }
}
