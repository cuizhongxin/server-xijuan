package com.tencent.wxcloudrun.repository;

import com.tencent.wxcloudrun.dao.GeneralMapper;
import com.tencent.wxcloudrun.model.General;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;

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
        generalMapper.upsert(general);
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
        return generalMapper.findById(generalId);
    }
    
    /**
     * 根据用户ID查找所有武将
     */
    public List<General> findByUserId(String userId) {
        return generalMapper.findByUserId(userId);
    }
    
    /**
     * 更新武将
     */
    public General update(General general) {
        General existing = generalMapper.findById(general.getId());
        if (existing == null) {
            return null;
        }
        return save(general);
    }
    
    /**
     * 删除武将
     */
    public boolean delete(String generalId) {
        General existing = generalMapper.findById(generalId);
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
