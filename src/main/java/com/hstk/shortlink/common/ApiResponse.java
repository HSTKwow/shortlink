package com.hstk.shortlink.common;

import lombok.Data;

@Data
public class ApiResponse<T> {
    private Integer code;
    private String message;
    private T data;

    public ApiResponse(Integer code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public static <T> ApiResponse<T> success(T data){
        return new ApiResponse<>(200,"success",data);
    }

    public static <T> ApiResponse<T> fail(Integer code,String message){
        return new ApiResponse<>(code,message,null);
    }

}
