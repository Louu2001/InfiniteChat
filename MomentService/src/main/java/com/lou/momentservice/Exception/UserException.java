package com.lou.momentservice.Exception;



import com.lou.momentservice.constants.ErrorEnum;
import lombok.Getter;

@Getter
public class UserException extends RuntimeException{
    private final int code;

    public UserException(String message){
        super(message);

        this.code = ErrorEnum.SUCCESS.getCode();

    }

    public UserException(ErrorEnum errorEnum){
        super(errorEnum.getMessage());

        this.code = errorEnum.getCode();

    }

    public UserException(ErrorEnum errorEnum, String message){
        super(message);

        this.code = errorEnum.getCode();

    }

}
