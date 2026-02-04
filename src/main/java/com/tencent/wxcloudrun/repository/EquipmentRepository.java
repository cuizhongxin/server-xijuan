package com.tencent.wxcloudrun.repository;

import com.tencent.wxcloudrun.model.Equipment;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 装备数据仓库（内存存储）
 */
@Repository
public class EquipmentRepository {
    
    // 使用ConcurrentHashMap存储装备数据
    // key: equipmentId, value: Equipment
    private final Map<String, Equipment> equipmentStore = new ConcurrentHashMap<>();
    
    /**
     * 保存装备
     */
    public Equipment save(Equipment equipment) {
        equipment.setUpdateTime(System.currentTimeMillis());
        if (equipment.getCreateTime() == null) {
            equipment.setCreateTime(System.currentTimeMillis());
        }
        equipmentStore.put(equipment.getId(), equipment);
        return equipment;
    }
    
    /**
     * 批量保存装备
     */
    public List<Equipment> saveAll(List<Equipment> equipments) {
        for (Equipment equipment : equipments) {
            save(equipment);
        }
        return equipments;
    }
    
    /**
     * 根据ID查找装备
     */
    public Equipment findById(String id) {
        return equipmentStore.get(id);
    }
    
    /**
     * 根据用户ID查找所有装备
     */
    public List<Equipment> findByUserId(String userId) {
        return equipmentStore.values().stream()
            .filter(e -> userId.equals(e.getUserId()))
            .collect(Collectors.toList());
    }
    
    /**
     * 根据用户ID和槽位类型查找装备
     */
    public List<Equipment> findByUserIdAndSlotType(String userId, Integer slotTypeId) {
        return equipmentStore.values().stream()
            .filter(e -> userId.equals(e.getUserId()))
            .filter(e -> e.getSlotType() != null && slotTypeId.equals(e.getSlotType().getId()))
            .collect(Collectors.toList());
    }
    
    /**
     * 根据用户ID查找未装备的装备
     */
    public List<Equipment> findUnequippedByUserId(String userId) {
        return equipmentStore.values().stream()
            .filter(e -> userId.equals(e.getUserId()))
            .filter(e -> !e.getEquipped())
            .collect(Collectors.toList());
    }
    
    /**
     * 根据武将ID查找已装备的装备
     */
    public List<Equipment> findEquippedByGeneralId(String generalId) {
        return equipmentStore.values().stream()
            .filter(e -> generalId.equals(e.getEquippedGeneralId()))
            .collect(Collectors.toList());
    }
    
    /**
     * 根据套装ID查找用户装备
     */
    public List<Equipment> findByUserIdAndSetId(String userId, String setId) {
        return equipmentStore.values().stream()
            .filter(e -> userId.equals(e.getUserId()))
            .filter(e -> e.getSetInfo() != null && setId.equals(e.getSetInfo().getSetId()))
            .collect(Collectors.toList());
    }
    
    /**
     * 更新装备
     */
    public Equipment update(Equipment equipment) {
        if (equipmentStore.containsKey(equipment.getId())) {
            equipment.setUpdateTime(System.currentTimeMillis());
            equipmentStore.put(equipment.getId(), equipment);
            return equipment;
        }
        return null;
    }
    
    /**
     * 删除装备
     */
    public void delete(String id) {
        equipmentStore.remove(id);
    }
    
    /**
     * 根据用户ID删除所有装备
     */
    public void deleteByUserId(String userId) {
        List<String> toDelete = equipmentStore.values().stream()
            .filter(e -> userId.equals(e.getUserId()))
            .map(Equipment::getId)
            .collect(Collectors.toList());
        
        for (String id : toDelete) {
            equipmentStore.remove(id);
        }
    }
    
    /**
     * 统计用户装备数量
     */
    public int countByUserId(String userId) {
        return (int) equipmentStore.values().stream()
            .filter(e -> userId.equals(e.getUserId()))
            .count();
    }
    
    /**
     * 清空所有数据（测试用）
     */
    public void clear() {
        equipmentStore.clear();
    }
}


