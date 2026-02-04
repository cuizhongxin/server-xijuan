package com.tencent.wxcloudrun.repository;

import com.tencent.wxcloudrun.model.DungeonProgress;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 副本进度数据仓库（内存存储）
 */
@Repository
public class DungeonProgressRepository {
    
    // 使用ConcurrentHashMap存储用户副本进度
    // key: ${userId}_${dungeonId}, value: DungeonProgress
    private final Map<String, DungeonProgress> progressStore = new ConcurrentHashMap<>();
    
    /**
     * 生成存储key
     */
    private String getKey(String userId, String dungeonId) {
        return userId + "_" + dungeonId;
    }
    
    /**
     * 获取用户某个副本的进度
     */
    public DungeonProgress findByUserIdAndDungeonId(String userId, String dungeonId) {
        return progressStore.get(getKey(userId, dungeonId));
    }
    
    /**
     * 获取用户所有副本进度
     */
    public List<DungeonProgress> findByUserId(String userId) {
        return progressStore.values().stream()
            .filter(p -> userId.equals(p.getUserId()))
            .collect(Collectors.toList());
    }
    
    /**
     * 保存或更新进度
     */
    public DungeonProgress save(DungeonProgress progress) {
        progress.setUpdateTime(System.currentTimeMillis());
        if (progress.getCreateTime() == null) {
            progress.setCreateTime(System.currentTimeMillis());
        }
        progressStore.put(getKey(progress.getUserId(), progress.getDungeonId()), progress);
        return progress;
    }
    
    /**
     * 初始化用户副本进度
     */
    public DungeonProgress initProgress(String userId, String dungeonId) {
        DungeonProgress progress = DungeonProgress.builder()
            .userId(userId)
            .dungeonId(dungeonId)
            .currentProgress(0)
            .defeatedNpcs(new HashSet<>())
            .todayEntries(0)
            .lastEntryDate("")
            .cleared(false)
            .clearCount(0)
            .createTime(System.currentTimeMillis())
            .updateTime(System.currentTimeMillis())
            .build();
        
        return save(progress);
    }
    
    /**
     * 删除用户所有进度
     */
    public void deleteByUserId(String userId) {
        List<String> keysToDelete = progressStore.keySet().stream()
            .filter(key -> key.startsWith(userId + "_"))
            .collect(Collectors.toList());
        
        for (String key : keysToDelete) {
            progressStore.remove(key);
        }
    }
    
    /**
     * 清空所有数据（测试用）
     */
    public void clear() {
        progressStore.clear();
    }
}


