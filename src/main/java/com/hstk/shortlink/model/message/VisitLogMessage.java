package com.hstk.shortlink.model.message;

import lombok.Data;

import java.io.Serializable;

@Data
public class VisitLogMessage implements Serializable {
    private String shortCode;
    private String ip;
    private String userAgent;
    private String referer;
}
