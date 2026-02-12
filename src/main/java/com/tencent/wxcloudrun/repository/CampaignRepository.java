package com.tencent.wxcloudrun.repository;

import com.alibaba.fastjson.JSON;
import com.tencent.wxcloudrun.dao.CampaignProgressMapper;
import com.tencent.wxcloudrun.model.CampaignProgress;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Repository
public class CampaignRepository {
    
    @Autowired
    private CampaignProgressMapper campaignProgressMapper;
    
    public CampaignProgress findByUserIdAndCampaignId(String odUserId, String campaignId) {
        String data = campaignProgressMapper.findByUserIdAndCampaignId(odUserId, campaignId);
        if (data == null) {
            return null;
        }
        return JSON.parseObject(data, CampaignProgress.class);
    }
    
    public void save(CampaignProgress progress) {
        progress.setUpdateTime(System.currentTimeMillis());
        campaignProgressMapper.upsert(progress.getUserId(), progress.getCampaignId(),
                JSON.toJSONString(progress), progress.getUpdateTime());
        log.debug("保存战役进度: {}_{}", progress.getUserId(), progress.getCampaignId());
    }
    
    public void deleteByUserIdAndCampaignId(String odUserId, String campaignId) {
        campaignProgressMapper.deleteByUserIdAndCampaignId(odUserId, campaignId);
    }
    
    public Map<String, CampaignProgress> findAllByUserId(String odUserId) {
        List<Map<String, Object>> rows = campaignProgressMapper.findAllByUserId(odUserId);
        Map<String, CampaignProgress> result = new HashMap<>();
        if (rows != null) {
            for (Map<String, Object> row : rows) {
                String campaignId = (String) row.get("campaignId");
                String data = (String) row.get("data");
                if (data != null) {
                    result.put(campaignId, JSON.parseObject(data, CampaignProgress.class));
                }
            }
        }
        return result;
    }
}
