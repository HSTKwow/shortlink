package com.hstk.shortlink.service;


import com.hstk.shortlink.common.PageResult;
import com.hstk.shortlink.model.dto.ShortLinkStatsResponse;
import com.hstk.shortlink.model.entity.ShortLink;
import com.hstk.shortlink.model.entity.ShortLinkVisitLog;

import java.time.LocalDateTime;
import java.util.List;

public interface ShortLinkService {
    //生成短码
    String createShortLink(String originalUrl, LocalDateTime expireTime);
    //得到短码的长码
    String getOriginalUrl(String shortCode,String ip,String userAgent,String referer);
    //更新短码状态
    void updateStatus(String shortCode,Integer status);
    //查找短码
    ShortLink getShortLink(String shortCode);
    //分页查找短码详细信息
    PageResult<ShortLink>listShortLinks(Integer page,Integer pageSize,Integer status);

    //查找该短码的最近访问日志
    List<ShortLinkVisitLog>getVisitLogs(String shortCode,Integer limit);
   //统计该短链的信息
    ShortLinkStatsResponse getStats(String shortCode);
}
