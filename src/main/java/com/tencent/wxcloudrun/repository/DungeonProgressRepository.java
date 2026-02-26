package com.tencent.wxcloudrun.repository;

import com.tencent.wxcloudrun.dao.DungeonProgressMapper;
import com.tencent.wxcloudrun.model.DungeonProgress;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 副本进度数据仓库（数据库存储）
 */
@Repository
public class DungeonProgressRepository {
    
    @Autowired
    private DungeonProgressMapper dungeonProgressMapper;
    
    public DungeonProgress findByUserIdAndDungeonId(String userId, String dungeonId) {
        return dungeonProgressMapper.findByUserIdAndDungeonId(userId, dungeonId);
    }
    
    public Map<String, DungeonProgress> findAllByUserId(String userId) {
        List<DungeonProgress> list = dungeonProgressMapper.findByUserId(userId);
        Map<String, DungeonProgress> result = new HashMap<>();
        if (list != null) {
            for (DungeonProgress dp : list) {
                result.put(dp.getDungeonId(), dp);
            }
        }
        return result;
    }
    
    public void save(DungeonProgress progress) {
        progress.setUpdateTime(System.currentTimeMillis());
        if (progress.getCreateTime() == null) {
            progress.setCreateTime(System.currentTimeMillis());
        }
        dungeonProgressMapper.upsert(progress);
    }
    
    public DungeonProgress initProgress(String userId, String dungeonId) {
        DungeonProgress progress = new DungeonProgress();
        progress.setUserId(userId);
        progress.setDungeonId(dungeonId);
        progress.setCurrentProgress(0);
        progress.setTodayEntries(0);
        progress.setCleared(false);
        progress.setDefeatedNpcs(new java.util.HashSet<>());
        progress.setCreateTime(System.currentTimeMillis());
        progress.setUpdateTime(System.currentTimeMillis());
        dungeonProgressMapper.upsert(progress);
        return progress;
    }
    
    public void deleteByUserId(String userId) {
        dungeonProgressMapper.deleteByUserId(userId);
    }
    
    public void clear() {
        // 数据库模式下不支持清空全表，忽略此操作
    }
}
