package com.tencent.wxcloudrun.dao;

import com.tencent.wxcloudrun.model.CampaignProgress;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface CampaignProgressMapper {
    
    CampaignProgress findByUserIdAndCampaignId(@Param("userId") String userId, @Param("campaignId") String campaignId);
    
    List<CampaignProgress> findAllByUserId(@Param("userId") String userId);
    
    void upsert(CampaignProgress progress);
    
    void deleteByUserIdAndCampaignId(@Param("userId") String userId, @Param("campaignId") String campaignId);
}
