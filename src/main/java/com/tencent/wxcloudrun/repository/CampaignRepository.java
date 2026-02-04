package com.tencent.wxcloudrun.repository;

import com.tencent.wxcloudrun.model.CampaignProgress;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Repository
public class CampaignRepository {
    
    // 用户战役进度存储: key = odUserId + "_" + campaignId
    private final Map<String, CampaignProgress> progressStorage = new ConcurrentHashMap<>();
    
    public CampaignProgress findByUserIdAndCampaignId(String odUserId, String campaignId) {
        return progressStorage.get(odUserId + "_" + campaignId);
    }
    
    public void save(CampaignProgress progress) {
        String key = progress.getUserId() + "_" + progress.getCampaignId();
        progress.setUpdateTime(System.currentTimeMillis());
        progressStorage.put(key, progress);
        log.debug("保存战役进度: {}", key);
    }
    
    public void deleteByUserIdAndCampaignId(String odUserId, String campaignId) {
        progressStorage.remove(odUserId + "_" + campaignId);
    }
    
    public Map<String, CampaignProgress> findAllByUserId(String odUserId) {
        Map<String, CampaignProgress> result = new ConcurrentHashMap<>();
        progressStorage.forEach((key, value) -> {
            if (key.startsWith(odUserId + "_")) {
                result.put(value.getCampaignId(), value);
            }
        });
        return result;
    }
}
