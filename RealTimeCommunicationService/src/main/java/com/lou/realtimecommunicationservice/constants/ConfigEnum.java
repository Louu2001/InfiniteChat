package com.lou.realtimecommunicationservice.constants;

import lombok.Getter;

/**
 * @ClassName ConfigEnum
 * @Description TODO
 * @Author Lou
 * @Date 2025/6/1 15:11
 */

@Getter
public enum ConfigEnum {

    TOKEN_SECRET_KEY("tokenSecretKey","lou");

    private final String value;
    private final String text;

    ConfigEnum(String text, String value) {
        this.text = text;
        this.value = value;
    }

    public String getText() {
        return text;
    }


    public String getValue() {
        return value;
    }
}
