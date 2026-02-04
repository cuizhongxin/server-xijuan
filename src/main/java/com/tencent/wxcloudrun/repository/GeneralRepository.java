package com.tencent.wxcloudrun.repository;

import com.tencent.wxcloudrun.model.General;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 武将数据存储（内存存储）
 */
@Repository
public class GeneralRepository {
    
    // 使用ConcurrentHashMap存储武将数据
    // key: generalId, value: General
    private final Map<String, General> generalStore = new ConcurrentHashMap<>();
    
    // 用户武将索引
    // key: userId, value: List<generalId>
    private final Map<String, List<String>> userGeneralsIndex = new ConcurrentHashMap<>();
    
    /**
     * 保存武将
     */
    public General save(General general) {
        general.setUpdateTime(System.currentTimeMillis());
        if (general.getCreateTime() == null) {
            general.setCreateTime(System.currentTimeMillis());
        }
        
        generalStore.put(general.getId(), general);
        
        // 更新用户索引
        userGeneralsIndex.computeIfAbsent(general.getUserId(), k -> new ArrayList<>())
                        .add(general.getId());
        
        return general;
    }
    
    /**
     * 批量保存
     */
    public List<General> saveAll(List<General> generals) {
        generals.forEach(this::save);
        return generals;
    }
    
    /**
     * 根据ID查找
     */
    public General findById(String generalId) {
        return generalStore.get(generalId);
    }
    
    /**
     * 根据用户ID查找所有武将
     */
    public List<General> findByUserId(String userId) {
        List<String> generalIds = userGeneralsIndex.get(userId);
        if (generalIds == null || generalIds.isEmpty()) {
            return new ArrayList<>();
        }
        
        return generalIds.stream()
                        .map(generalStore::get)
                        .filter(general -> general != null)
                        .collect(Collectors.toList());
    }
    
    /**
     * 更新武将
     */
    public General update(General general) {
        if (!generalStore.containsKey(general.getId())) {
            return null;
        }
        return save(general);
    }
    
    /**
     * 删除武将
     */
    public boolean delete(String generalId) {
        General general = generalStore.remove(generalId);
        if (general != null) {
            // 从用户索引中删除
            List<String> userGenerals = userGeneralsIndex.get(general.getUserId());
            if (userGenerals != null) {
                userGenerals.remove(generalId);
            }
            return true;
        }
        return false;
    }
    
    /**
     * 统计用户武将数量
     */
    public int countByUserId(String userId) {
        List<String> generalIds = userGeneralsIndex.get(userId);
        return generalIds == null ? 0 : generalIds.size();
    }
    
    /**
     * 清空所有数据（测试用）
     */
    public void clear() {
        generalStore.clear();
        userGeneralsIndex.clear();
    }
}


