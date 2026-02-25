package com.tencent.wxcloudrun.dao;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface VipGiftClaimMapper {

    List<Integer> findClaimedLevels(@Param("userId") String userId);

    int countClaim(@Param("userId") String userId, @Param("vipLevel") int vipLevel);

    void insertClaim(@Param("userId") String userId,
                     @Param("vipLevel") int vipLevel,
                     @Param("claimTime") long claimTime);
}
