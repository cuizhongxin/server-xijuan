package com.tencent.wxcloudrun.dao;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Map;

@Mapper
public interface ContinuousLoginMapper {

    Map<String, Object> findByUserId(@Param("userId") String userId);

    void upsertRecord(@Param("userId") String userId,
                      @Param("consecutiveDays") int consecutiveDays,
                      @Param("lastLoginDate") String lastLoginDate,
                      @Param("totalLoginDays") int totalLoginDays,
                      @Param("claimedDays") String claimedDays);
}
