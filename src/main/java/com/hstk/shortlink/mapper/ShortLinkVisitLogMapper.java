package com.hstk.shortlink.mapper;

import com.hstk.shortlink.model.entity.ShortLinkVisitLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ShortLinkVisitLogMapper {
    int insert(ShortLinkVisitLog shortLinkVisitLog);

    List<ShortLinkVisitLog> selectRecentByShortCode(@Param("shortCode")String shortCode, @Param("limit")Integer limit);

    Long countTodayByShortCode(@Param("shortCode")String shortCode);
}
