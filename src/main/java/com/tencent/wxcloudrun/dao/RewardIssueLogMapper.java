package com.tencent.wxcloudrun.dao;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface RewardIssueLogMapper {

    int insertIgnore(@Param("bizType") String bizType,
                     @Param("bizId") String bizId,
                     @Param("targetId") String targetId,
                     @Param("serverId") Integer serverId,
                     @Param("extra") String extra,
                     @Param("createTime") Long createTime);
}
