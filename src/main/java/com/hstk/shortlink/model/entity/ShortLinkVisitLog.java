package com.hstk.shortlink.model.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ShortLinkVisitLog {
    private Long id;
    private String shortCode;
    private String ip;
    private String userAgent;
    private String referer;
    private LocalDateTime createTime;

}
