package com.hstk.shortlink.service.impl;


import com.hstk.shortlink.common.BusinessException;
import com.hstk.shortlink.common.PageResult;
import com.hstk.shortlink.mapper.ShortLinkMapper;
import com.hstk.shortlink.mapper.ShortLinkVisitLogMapper;
import com.hstk.shortlink.model.dto.ShortLinkStatsResponse;
import com.hstk.shortlink.model.entity.ShortLink;
import com.hstk.shortlink.model.entity.ShortLinkVisitLog;
import com.hstk.shortlink.service.ShortLinkService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class ShortLinkServiceImpl implements ShortLinkService {

    private final ShortLinkMapper shortLinkMapper;
    private final ShortLinkVisitLogMapper shortLinkVisitLogMapper;

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

    public ShortLinkServiceImpl(ShortLinkMapper shortLinkMapper, ShortLinkVisitLogMapper shortLinkVisitLogMapper) {
        this.shortLinkMapper = shortLinkMapper;
        this.shortLinkVisitLogMapper = shortLinkVisitLogMapper;
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
    public String getOriginalUrl(String shortCode, String ip, String userAgent, String referer) {
        ShortLink shortLink=shortLinkMapper.selectByShortCode(shortCode);

        if(shortLink==null){
            throw new BusinessException(404,"短链不存在");
        }

        if(Integer.valueOf(0).equals(shortLink.getStatus())){
            throw new BusinessException(403,"短链已禁用");
        }


        if(shortLink.getExpireTime()!=null&&LocalDateTime.now().isAfter(shortLink.getExpireTime())){
            throw new BusinessException(410,"短链已过期");
        }

        shortLinkMapper.increaseVisitCount(shortCode);

        //添加访问记录
        ShortLinkVisitLog shortLinkVisitLog=new ShortLinkVisitLog();
        shortLinkVisitLog.setShortCode(shortCode);
        shortLinkVisitLog.setIp(ip);
        shortLinkVisitLog.setUserAgent(userAgent);
        shortLinkVisitLog.setReferer(referer);

        shortLinkVisitLogMapper.insert(shortLinkVisitLog);

        return shortLink.getOriginalUrl();

    }

    @Override
    public void updateStatus(String shortCode, Integer status) {
        ShortLink shortLink=shortLinkMapper.selectByShortCode(shortCode);

        if(shortLink==null) {
            throw new BusinessException(404,"短链不存在");
        }

        if(!Integer.valueOf(0).equals(status)&&
        !Integer.valueOf(1).equals(status)){
            throw new BusinessException(400,"状态值不合法");
        }

        shortLinkMapper.updateStatus(shortCode,status);

    }

    @Override
    public List<ShortLinkVisitLog> getVisitLogs(String shortCode, Integer limit) {
        ShortLink shortLink=shortLinkMapper.selectByShortCode(shortCode);

        if(shortLink==null){
            throw new BusinessException(404,"短链不存在");
        }
        if(limit==null||limit<=0||limit>100){
            limit=20;
        }

        return shortLinkVisitLogMapper.selectRecentByShortCode(shortCode,limit);
    }

    @Override
    public ShortLink getShortLink(String shortCode) {
        ShortLink shortLink=shortLinkMapper.selectByShortCode(shortCode);
        if(shortLink==null){
            throw new BusinessException(404,"短链不存在");
        }
        return shortLink;
    }

    @Override
    public PageResult<ShortLink> listShortLinks(Integer page, Integer pageSize, Integer status) {
        if(page==null||page<1){
            page=1;
        }
        if (pageSize == null || pageSize < 1 || pageSize > 100) {
            pageSize = 10;
        }
        if (status != null
                && !Integer.valueOf(0).equals(status)
                && !Integer.valueOf(1).equals(status)) {
            throw new BusinessException(400, "状态值不合法");
        }

        Integer offset = (page - 1) * pageSize;
        List<ShortLink> shortLinks=shortLinkMapper.selectPage(status,offset,pageSize);
        Long total=shortLinkMapper.countPage(status);

        return new PageResult<>(shortLinks,total,page,pageSize);
    }

    @Override
    public ShortLinkStatsResponse getStats(String shortCode) {
        ShortLink shortLink=shortLinkMapper.selectByShortCode(shortCode);
        if (shortLink == null) {
            throw new BusinessException(404, "短链不存在");
        }
        Long todayVisitCount= shortLinkVisitLogMapper.countTodayByShortCode(shortCode);
        ShortLinkStatsResponse response=new ShortLinkStatsResponse();
        response.setShortCode(shortLink.getShortCode());
        response.setOriginalUrl(shortLink.getOriginalUrl());
        response.setStatus(shortLink.getStatus());
        response.setTotalVisitCount(shortLink.getVisitCount());
        response.setTodayVisitCount(todayVisitCount);

        return response;
    }


}
