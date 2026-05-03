package com.hstk.shortlink.service.impl;


import com.hstk.shortlink.common.BusinessException;
import com.hstk.shortlink.common.PageResult;
import com.hstk.shortlink.mapper.ShortLinkMapper;
import com.hstk.shortlink.mapper.ShortLinkVisitLogMapper;
import com.hstk.shortlink.model.dto.ShortLinkStatsResponse;
import com.hstk.shortlink.model.entity.ShortLink;
import com.hstk.shortlink.model.entity.ShortLinkVisitLog;
import com.hstk.shortlink.mq.VisitLogProducer;
import com.hstk.shortlink.service.ShortLinkService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class ShortLinkServiceImpl implements ShortLinkService {

    private final ShortLinkMapper shortLinkMapper;
    private final ShortLinkVisitLogMapper shortLinkVisitLogMapper;

    private static final String REDIRECT_CACHE_PREFIX="shortlink:redirect:";
    private static final String NULL_CACHE_VALUE="__NULL__";

    private final StringRedisTemplate stringRedisTemplate;

    private final VisitLogProducer visitLogProducer;

    //生成短码
    private String generateShortCode() {
        return UUID.randomUUID().
                toString().
                replace("-", "").
                substring(0, 6);
    }

    //生成为一短码
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

    //写入redis
    private void cacheShortLink(String cacheKey,ShortLink shortLink){
        try {
            if (shortLink.getExpireTime() != null) {
                //log.info(LocalDateTime.now().toString());
                Duration ttl = Duration.between(LocalDateTime.now(), shortLink.getExpireTime());
                if (!ttl.isNegative() && !ttl.isZero()) {
                    stringRedisTemplate.opsForValue().set(cacheKey, shortLink.getOriginalUrl(), ttl);
                }

            } else {
                stringRedisTemplate.opsForValue().set(cacheKey, shortLink.getOriginalUrl(), Duration.ofHours(24));
            }
        } catch (Exception e) {
            log.warn("write redis fail",e);
        }

    }

    //检查访问次数
    private void checkRateLimit(String shortCode,String ip){
        try{
            String key="shortlink:rate:"+shortCode+":"+ip;
            Long count=stringRedisTemplate.opsForValue().increment(key);
            //log.info("一秒内访问次数:"+count);
            //第一次访问就设置cache存活时间
            if(count!=null&&count==1){
                stringRedisTemplate.expire(key,Duration.ofSeconds(1));
            }
            //在存活期内大量访问
            if(count!=null&&count>10){
                throw new BusinessException(429,"访问过于频繁");
            }
        }catch (BusinessException e){
            throw e;
        }catch (Exception e){
            log.warn("redis rate limit fail",e);
        }
    }

    //构造函数
    public ShortLinkServiceImpl(ShortLinkMapper shortLinkMapper, ShortLinkVisitLogMapper shortLinkVisitLogMapper, StringRedisTemplate stringRedisTemplate, VisitLogProducer visitLogProducer) {
        this.shortLinkMapper = shortLinkMapper;
        this.shortLinkVisitLogMapper = shortLinkVisitLogMapper;
        this.stringRedisTemplate = stringRedisTemplate;
        this.visitLogProducer = visitLogProducer;
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
        checkRateLimit(shortCode,ip);
        String cacheKey=REDIRECT_CACHE_PREFIX+shortCode;
        String cacheValue=null;

        try{//防止redis出错
            cacheValue=stringRedisTemplate.opsForValue().get(cacheKey);
        }catch (Exception e){
            log.warn("redis get fail,shortcode={}",shortCode,e);
        }

        if(NULL_CACHE_VALUE.equals(cacheValue)){
            throw new BusinessException(404,"短链不存在");
        }

        //redis命中
        if(cacheValue!=null){
            log.info("redis命中");
            visitLogProducer.sendVisitLog(shortCode,ip,userAgent,referer);
            return cacheValue;
        }
        //未命中
        ShortLink shortLink=shortLinkMapper.selectByShortCode(shortCode);
        if(shortLink==null){
            log.info("空链缓存");
            try {//空链也缓存到redis
                stringRedisTemplate.opsForValue().set(cacheKey,NULL_CACHE_VALUE,Duration.ofMinutes(5));
            } catch (Exception e) {
                log.warn("redis write fail",e);
            }
            throw new BusinessException(404,"短链不存在");
        }

        if(Integer.valueOf(0).equals(shortLink.getStatus())){
            throw new BusinessException(403,"短链已禁用");
        }
        if(shortLink.getExpireTime()!=null&&LocalDateTime.now().isAfter(shortLink.getExpireTime())){
            throw new BusinessException(410,"短链已过期");
        }

        log.info("redis未命中");
        cacheShortLink(cacheKey,shortLink);
        //异步添加访问记录
        visitLogProducer.sendVisitLog(shortCode,ip,userAgent,referer);


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

        //先更新数据库，在删除缓存，防止读到就缓存
        shortLinkMapper.updateStatus(shortCode,status);
        try {//删除redis缓存
            stringRedisTemplate.delete(REDIRECT_CACHE_PREFIX + shortCode);
        } catch (Exception e) {
            log.warn("redis delete fail", e);
        }
        log.info("redis缓存已清除");

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
