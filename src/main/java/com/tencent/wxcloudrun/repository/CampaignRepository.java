package com.tencent.wxcloudrun.repository;

import com.tencent.wxcloudrun.dao.CampaignProgressMapper;
import com.tencent.wxcloudrun.model.CampaignProgress;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 战役进度数据仓库（数据库存储）
 */
@Repository
public class CampaignRepository {
    
    @Autowired
    private CampaignProgressMapper campaignProgressMapper;
    
    public CampaignProgress findByUserIdAndCampaignId(String userId, String campaignId) {
        return campaignProgressMapper.findByUserIdAndCampaignId(userId, campaignId);
    }
    
    public Map<String, CampaignProgress> findAllByUserId(String userId) {
        List<CampaignProgress> list = campaignProgressMapper.findAllByUserId(userId);
        Map<String, CampaignProgress> result = new HashMap<>();
        if (list != null) {
            for (CampaignProgress cp : list) {
                result.put(cp.getCampaignId(), cp);
            }
        }
        return result;
    }
    
    public void save(CampaignProgress progress) {
        progress.setUpdateTime(System.currentTimeMillis());
        campaignProgressMapper.upsert(progress);
    }
    
    public void deleteByUserIdAndCampaignId(String userId, String campaignId) {
        campaignProgressMapper.deleteByUserIdAndCampaignId(userId, campaignId);
    }
}
