package com.tencent.wxcloudrun.repository;

import com.alibaba.fastjson.JSON;
import com.tencent.wxcloudrun.dao.DungeonProgressMapper;
import com.tencent.wxcloudrun.model.DungeonProgress;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.*;

/**
 * 副本进度数据仓库（数据库存储）
 */
@Repository
public class DungeonProgressRepository {
    
    @Autowired
    private DungeonProgressMapper dungeonProgressMapper;
    
    /**
     * 获取用户某个副本的进度
     */
    public DungeonProgress findByUserIdAndDungeonId(String userId, String dungeonId) {
        String data = dungeonProgressMapper.findByUserIdAndDungeonId(userId, dungeonId);
        if (data == null) {
            return null;
        }
        return JSON.parseObject(data, DungeonProgress.class);
    }
    
    /**
     * 获取用户所有副本进度
     */
    public List<DungeonProgress> findByUserId(String userId) {
        List<Map<String, Object>> rows = dungeonProgressMapper.findByUserId(userId);
        List<DungeonProgress> result = new ArrayList<>();
        if (rows != null) {
            for (Map<String, Object> row : rows) {
                String data = (String) row.get("data");
                if (data != null) {
                    result.add(JSON.parseObject(data, DungeonProgress.class));
                }
            }
        }
        return result;
    }
    
    /**
     * 保存或更新进度
     */
    public DungeonProgress save(DungeonProgress progress) {
        progress.setUpdateTime(System.currentTimeMillis());
        if (progress.getCreateTime() == null) {
            progress.setCreateTime(System.currentTimeMillis());
        }
        dungeonProgressMapper.upsert(progress.getUserId(), progress.getDungeonId(),
                JSON.toJSONString(progress), progress.getCreateTime(), progress.getUpdateTime());
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
        dungeonProgressMapper.deleteByUserId(userId);
    }
    
    /**
     * 清空所有数据（测试用）
     */
    public void clear() {
        // 数据库模式下不支持清空全表，忽略此操作
    }
}
