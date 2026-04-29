package com.hstk.shortlink.model.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ShortLink {
    private Long id;
    private String shortCode;
    private String originalUrl;
    private Integer status;
    private LocalDateTime expireTime;
    private Long visitCount;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

}
