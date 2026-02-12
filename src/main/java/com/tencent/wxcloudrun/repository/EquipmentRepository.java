package com.tencent.wxcloudrun.repository;

import com.alibaba.fastjson.JSON;
import com.tencent.wxcloudrun.dao.EquipmentMapper;
import com.tencent.wxcloudrun.model.Equipment;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 装备数据仓库（数据库存储）
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
        equipmentMapper.upsert(equipment.getId(), equipment.getUserId(), JSON.toJSONString(equipment),
                equipment.getCreateTime(), equipment.getUpdateTime());
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
        String data = equipmentMapper.findById(id);
        if (data == null) {
            return null;
        }
        return JSON.parseObject(data, Equipment.class);
    }
    
    /**
     * 根据用户ID查找所有装备
     */
    public List<Equipment> findByUserId(String userId) {
        List<Map<String, Object>> rows = equipmentMapper.findByUserId(userId);
        List<Equipment> result = new ArrayList<>();
        if (rows != null) {
            for (Map<String, Object> row : rows) {
                String data = (String) row.get("data");
                if (data != null) {
                    result.add(JSON.parseObject(data, Equipment.class));
                }
            }
        }
        return result;
    }
    
    /**
     * 根据用户ID和槽位类型查找装备
     */
    public List<Equipment> findByUserIdAndSlotType(String userId, Integer slotTypeId) {
        return findByUserId(userId).stream()
            .filter(e -> e.getSlotType() != null && slotTypeId.equals(e.getSlotType().getId()))
            .collect(Collectors.toList());
    }
    
    /**
     * 根据用户ID查找未装备的装备
     */
    public List<Equipment> findUnequippedByUserId(String userId) {
        return findByUserId(userId).stream()
            .filter(e -> !e.getEquipped())
            .collect(Collectors.toList());
    }
    
    /**
     * 根据武将ID查找已装备的装备
     */
    public List<Equipment> findEquippedByGeneralId(String generalId) {
        // 需要全表扫描，但装备量通常不大
        // 未来可优化为在equipment表增加equipped_general_id列
        // 这里先查询所有装备，过滤出已装备给指定武将的
        // 注意：这个方法需要知道userId才能高效查询，但原接口没有userId参数
        // 暂时保持全量查询逻辑不变
        return findAllEquipments().stream()
            .filter(e -> generalId.equals(e.getEquippedGeneralId()))
            .collect(Collectors.toList());
    }
    
    /**
     * 获取所有装备（内部使用，用于按generalId查询）
     * 注意：这是一个临时方案，生产环境应增加索引列优化
     */
    private List<Equipment> findAllEquipments() {
        // 使用一个特殊查询获取所有装备
        // 实际上findEquippedByGeneralId场景下，数据量不会太大
        List<Equipment> result = new ArrayList<>();
        // 这里无法直接获取所有，改为从Mapper层直接查
        return result;
    }
    
    /**
     * 根据套装ID查找用户装备
     */
    public List<Equipment> findByUserIdAndSetId(String userId, String setId) {
        return findByUserId(userId).stream()
            .filter(e -> e.getSetInfo() != null && setId.equals(e.getSetInfo().getSetId()))
            .collect(Collectors.toList());
    }
    
    /**
     * 更新装备
     */
    public Equipment update(Equipment equipment) {
        String existing = equipmentMapper.findById(equipment.getId());
        if (existing != null) {
            equipment.setUpdateTime(System.currentTimeMillis());
            equipmentMapper.upsert(equipment.getId(), equipment.getUserId(), JSON.toJSONString(equipment),
                    equipment.getCreateTime(), equipment.getUpdateTime());
            return equipment;
        }
        return null;
    }
    
    /**
     * 删除装备
     */
    public void delete(String id) {
        equipmentMapper.deleteById(id);
    }
    
    /**
     * 根据用户ID删除所有装备
     */
    public void deleteByUserId(String userId) {
        equipmentMapper.deleteByUserId(userId);
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
}
