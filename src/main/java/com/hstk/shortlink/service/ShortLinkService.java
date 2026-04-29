package com.hstk.shortlink.service;


import java.time.LocalDateTime;

public interface ShortLinkService {
    String createShortLink(String originalUrl, LocalDateTime expireTime);

    String getOriginalUrl(String shortCode);

}
