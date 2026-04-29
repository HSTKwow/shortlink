package com.hstk.shortlink.model.dto;


import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.hibernate.validator.constraints.URL;

import java.time.LocalDateTime;

@Data
public class CreateShortLinkRequest {

     @NotBlank(message = "原始链接不能为空")
     @URL(message = "原始链接格式不对")
     private String originalUrl;

     
     @Future(message = "过期时间必须是未来时间")
     @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
     private LocalDateTime expireTime;
}
