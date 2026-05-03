package com.hstk.shortlink.service;

import com.hstk.shortlink.mapper.ShortLinkMapper;
import com.hstk.shortlink.mapper.ShortLinkVisitLogMapper;
import com.hstk.shortlink.model.entity.ShortLinkVisitLog;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class VisitLogService {
    private final ShortLinkMapper shortLinkMapper;
    private final ShortLinkVisitLogMapper shortLinkVisitLogMapper;

    public VisitLogService(ShortLinkMapper shortLinkMapper, ShortLinkVisitLogMapper shortLinkVisitLogMapper) {
        this.shortLinkMapper = shortLinkMapper;
        this.shortLinkVisitLogMapper = shortLinkVisitLogMapper;
    }


    public void recordVisit(String shortCode,String ip,String userAgent,String referer){
        try{
            shortLinkMapper.increaseVisitCount(shortCode);

            ShortLinkVisitLog shortLinkVisitLog=new ShortLinkVisitLog();
            shortLinkVisitLog.setShortCode(shortCode);
            shortLinkVisitLog.setIp(ip);
            shortLinkVisitLog.setUserAgent(userAgent);
            shortLinkVisitLog.setReferer(referer);

            shortLinkVisitLogMapper.insert(shortLinkVisitLog);

        }catch (Exception e){
            log.error("record visit log failed, shortCode={}", shortCode, e);
        }
    }

}
