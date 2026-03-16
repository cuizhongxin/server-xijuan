package com.tencent.wxcloudrun.dao;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface FundMapper {

    Map<String, Object> findByUserId(@Param("userId") String userId);

    void insertFund(@Param("userId") String userId);

    void updatePurchased(@Param("userId") String userId);

    List<Integer> findClaimedLevels(@Param("userId") String userId);

    void insertClaim(@Param("userId") String userId, @Param("rewardLevel") int rewardLevel);
}
