package com.tencent.wxcloudrun.dao;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface CampaignProgressMapper {
    
    String findByUserIdAndCampaignId(@Param("userId") String userId, @Param("campaignId") String campaignId);
    
    List<Map<String, Object>> findAllByUserId(@Param("userId") String userId);
    
    void upsert(@Param("userId") String userId, @Param("campaignId") String campaignId,
                @Param("data") String data, @Param("updateTime") Long updateTime);
    
    void deleteByUserIdAndCampaignId(@Param("userId") String userId, @Param("campaignId") String campaignId);
}
