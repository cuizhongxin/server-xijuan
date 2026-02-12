package com.tencent.wxcloudrun.repository;

import com.alibaba.fastjson.JSON;
import com.tencent.wxcloudrun.dao.GeneralMapper;
import com.tencent.wxcloudrun.model.General;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 武将数据存储（数据库存储）
 */
@Repository
public class GeneralRepository {
    
    @Autowired
    private GeneralMapper generalMapper;
    
    /**
     * 保存武将
     */
    public General save(General general) {
        general.setUpdateTime(System.currentTimeMillis());
        if (general.getCreateTime() == null) {
            general.setCreateTime(System.currentTimeMillis());
        }
        
        generalMapper.upsert(general.getId(), general.getUserId(), JSON.toJSONString(general),
                general.getCreateTime(), general.getUpdateTime());
        
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
        String data = generalMapper.findById(generalId);
        if (data == null) {
            return null;
        }
        return JSON.parseObject(data, General.class);
    }
    
    /**
     * 根据用户ID查找所有武将
     */
    public List<General> findByUserId(String userId) {
        List<Map<String, Object>> rows = generalMapper.findByUserId(userId);
        List<General> result = new ArrayList<>();
        if (rows != null) {
            for (Map<String, Object> row : rows) {
                String data = (String) row.get("data");
                if (data != null) {
                    result.add(JSON.parseObject(data, General.class));
                }
            }
        }
        return result;
    }
    
    /**
     * 更新武将
     */
    public General update(General general) {
        String existing = generalMapper.findById(general.getId());
        if (existing == null) {
            return null;
        }
        return save(general);
    }
    
    /**
     * 删除武将
     */
    public boolean delete(String generalId) {
        String existing = generalMapper.findById(generalId);
        if (existing != null) {
            generalMapper.deleteById(generalId);
            return true;
        }
        return false;
    }
    
    /**
     * 统计用户武将数量
     */
    public int countByUserId(String userId) {
        return generalMapper.countByUserId(userId);
    }
    
    /**
     * 清空所有数据（测试用）
     */
    public void clear() {
        // 数据库模式下不支持清空全表，忽略此操作
    }
}
