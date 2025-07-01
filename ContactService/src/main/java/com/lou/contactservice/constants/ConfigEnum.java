package com.lou.contactservice.constants;

import lombok.Getter;

/**
 * @ClassName ConfigEnum
 * @Description TODO
 * @Author Lou
 * @Date 2025/6/1 15:11
 */

@Getter
public enum ConfigEnum {
    MEDIA_TYPE("application/json; charset=utf-8"),
    WORKED_ID("1"),
    DATACENTER_ID("1"),
    GROUP_AVATAR_URL("http://http://14.103.140.112:9090/infinite-chat/img/avatar"),
    REQUEST_SUCCESSFUL("请求成功"),
    OPTION_FAILURE("操作失败");


    private final String value;

    ConfigEnum(String value) {
        this.value = value;
    }

}

