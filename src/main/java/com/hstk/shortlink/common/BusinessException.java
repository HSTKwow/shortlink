package com.hstk.shortlink.common;


import lombok.Getter;
import org.springframework.web.bind.annotation.GetMapping;

@Getter
public class BusinessException extends RuntimeException{

    private final Integer code;

    public BusinessException(Integer code,String message) {
        super(message);
        this.code = code;
    }



}
