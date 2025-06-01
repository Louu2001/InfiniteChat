package com.lou.authenticationservice.common;

import lombok.Data;
import lombok.experimental.Accessors;
import org.springframework.http.HttpStatus;

/**
 * @ClassName Result
 * @Description TODO
 * @Author Lou
 * @Date 2025/5/30 15:28
 */

@Data
@Accessors(chain = true)
public class Result<T> {
    private int code;

    private String msg;

    private T data;

    public static <T> Result<T> ok(T data) {
        Result<T> r = new Result<>();
        return r.setCode(HttpStatus.OK.value()).setData(data);
    }

    public static <T> Result<T> DatabaseError(String msg) {
        Result<T> r = new Result<>();
        return r.setCode(HttpStatus.INTERNAL_SERVER_ERROR.value()).setMsg(msg);
    }

    public static <T> Result<T> ServerError(String msg) {
        Result<T> r = new Result<>();
        return r.setCode(HttpStatus.INTERNAL_SERVER_ERROR.value()).setMsg(msg);
    }

    public static <T> Result<T> UserError(int code, String msg) {
        Result<T> r = new Result<>();
        return r.setCode(code).setMsg(msg);
    }
}
