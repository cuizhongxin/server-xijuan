package com.tencent.wxcloudrun.repository;

import com.tencent.wxcloudrun.dao.EquipmentMapper;
import com.tencent.wxcloudrun.model.Equipment;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 装备数据存储（数据库存储）
 */
@Repository
public class EquipmentRepository {
    
    @Autowired
    private EquipmentMapper equipmentMapper;
    
    /**
     * 保存装备
     */
    public Equipment save(Equipment equipment) {
        equipment.setUpdateTime(System.currentTimeMillis());
        if (equipment.getCreateTime() == null) {
            equipment.setCreateTime(System.currentTimeMillis());
        }
        equipmentMapper.upsert(equipment);
        return equipment;
    }
    
    /**
     * 批量保存
     */
    public List<Equipment> saveAll(List<Equipment> equipments) {
        equipments.forEach(this::save);
        return equipments;
    }
    
    /**
     * 根据ID查找
     */
    public Equipment findById(String equipmentId) {
        return equipmentMapper.findById(equipmentId);
    }
    
    /**
     * 根据用户ID查找所有装备
     */
    public List<Equipment> findByUserId(String userId) {
        return equipmentMapper.findByUserId(userId);
    }
    
    /**
     * 更新装备
     */
    public Equipment update(Equipment equipment) {
        Equipment existing = equipmentMapper.findById(equipment.getId());
        if (existing == null) {
            return null;
        }
        return save(equipment);
    }
    
    /**
     * 删除装备
     */
    public boolean delete(String equipmentId) {
        Equipment existing = equipmentMapper.findById(equipmentId);
        if (existing != null) {
            equipmentMapper.deleteById(equipmentId);
            return true;
        }
        return false;
    }
    
    /**
     * 统计用户装备数量
     */
    public int countByUserId(String userId) {
        return equipmentMapper.countByUserId(userId);
    }
    
    /**
     * 清空所有数据（测试用）
     */
    public void clear() {
        // 数据库模式下不支持清空全表，忽略此操作
    }
    
    public List<Equipment> findUnequippedByUserId(String userId) {
        return equipmentMapper.findUnequippedByUserId(userId);
    }
    
    public List<Equipment> findEquippedByGeneralId(String generalId) {
        return equipmentMapper.findEquippedByGeneralId(generalId);
    }
}
