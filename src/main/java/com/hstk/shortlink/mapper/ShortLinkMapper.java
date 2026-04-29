package com.hstk.shortlink.mapper;

import com.hstk.shortlink.model.entity.ShortLink;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ShortLinkMapper {
    int insert(ShortLink shortLink);

    ShortLink selectByShortCode(@Param("shortCode")String shortCode);


    int increaseVisitCount(@Param("shortCode")String shortCode);
}
