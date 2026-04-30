package com.hstk.shortlink.model.dto;

import lombok.Data;

@Data
public class ShortLinkStatsResponse {
    private String shortCode;
    private String originalUrl;
    private Integer status;
    private Long totalVisitCount;
    private Long todayVisitCount;

}
