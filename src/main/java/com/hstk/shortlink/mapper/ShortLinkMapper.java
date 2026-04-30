package com.hstk.shortlink.mapper;

import com.hstk.shortlink.model.entity.ShortLink;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ShortLinkMapper {
    int insert(ShortLink shortLink);

    ShortLink selectByShortCode(@Param("shortCode")String shortCode);

    int increaseVisitCount(@Param("shortCode")String shortCode);

    int updateStatus(@Param("shortCode")String shortCode, @Param("status")Integer Status);

    List<ShortLink>selectPage(@Param("status") Integer status,@Param("offset") Integer offset,@Param("pageSize")Integer pageSize);

    Long countPage(@Param("status")Integer status);
}
