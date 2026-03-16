package com.tencent.wxcloudrun.dao;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Map;

@Mapper
public interface CornucopiaMapper {

    Map<String, Object> findByUserAndDate(@Param("userId") String userId, @Param("drawDate") String drawDate);

    void upsertRecord(@Param("userId") String userId, @Param("drawDate") String drawDate,
                      @Param("drawCount") int drawCount, @Param("freeUsed") int freeUsed);

    int getTodayDrawCount(@Param("userId") String userId, @Param("drawDate") String drawDate);
}
