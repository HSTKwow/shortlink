package com.hstk.shortlink.service.impl;


import com.hstk.shortlink.common.BusinessException;
import com.hstk.shortlink.mapper.ShortLinkMapper;
import com.hstk.shortlink.model.entity.ShortLink;
import com.hstk.shortlink.service.ShortLinkService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class ShortLinkServiceImpl implements ShortLinkService {

    private final ShortLinkMapper shortLinkMapper;

    private String generateShortCode() {
        return UUID.randomUUID().
                toString().
                replace("-", "").
                substring(0, 6);
    }

    private String generateUniqueShortCode(){
        for(int i=0;i<5;i++){
            String shortCode=generateShortCode();
            ShortLink existing=shortLinkMapper.selectByShortCode(shortCode);

            if(existing==null){
                return shortCode;
            }
        }
        throw new BusinessException(500,"短链码生成失效，请稍后重试");
    }

    public ShortLinkServiceImpl(ShortLinkMapper shortLinkMapper) {
        this.shortLinkMapper = shortLinkMapper;
    }

    @Override
    public String createShortLink(String originalUrl, LocalDateTime expireTime) {
        String shortCode=generateUniqueShortCode();
        //初始化对象
        ShortLink shortLink=new ShortLink();
        shortLink.setShortCode(shortCode);
        shortLink.setOriginalUrl(originalUrl);
        shortLink.setStatus(1);
        shortLink.setExpireTime(expireTime);
        shortLink.setVisitCount(0L);
        //加入到数据库
        shortLinkMapper.insert(shortLink);

        return shortCode;
    }

    @Override
    public String getOriginalUrl(String shortCode) {
        ShortLink shortLink=shortLinkMapper.selectByShortCode(shortCode);
        if(shortLink==null){
            throw new BusinessException(404,"短链不存在");
        }

        if(shortLink.getExpireTime()!=null&&LocalDateTime.now().isAfter(shortLink.getExpireTime())){
            throw new BusinessException(410,"短链已过期");
        }

        shortLinkMapper.increaseVisitCount(shortCode);

        return shortLink.getOriginalUrl();

    }
}
