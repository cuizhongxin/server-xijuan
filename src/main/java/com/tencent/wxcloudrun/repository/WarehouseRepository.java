package com.tencent.wxcloudrun.repository;

import com.tencent.wxcloudrun.model.Warehouse;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 仓库数据仓库
 */
@Repository
public class WarehouseRepository {
    
    private final Map<String, Warehouse> warehouseStorage = new ConcurrentHashMap<>();
    
    private static final int BASE_CAPACITY = 100;
    private static final int EXPAND_AMOUNT = 100;
    private static final int MAX_EXPAND_TIMES = 4;
    
    /**
     * 根据用户ID查找仓库
     */
    public Warehouse findByUserId(String userId) {
        return warehouseStorage.values().stream()
            .filter(w -> userId.equals(w.getUserId()))
            .findFirst()
            .orElse(null);
    }
    
    /**
     * 初始化用户仓库
     */
    public Warehouse initWarehouse(String userId) {
        String warehouseId = "warehouse_" + UUID.randomUUID().toString().substring(0, 8);
        
        Warehouse.EquipmentStorage equipmentStorage = Warehouse.EquipmentStorage.builder()
            .capacity(BASE_CAPACITY)
            .baseCapacity(BASE_CAPACITY)
            .expandTimes(0)
            .usedSlots(0)
            .equipmentIds(new ArrayList<>())
            .build();
        
        Warehouse.ItemStorage itemStorage = Warehouse.ItemStorage.builder()
            .capacity(BASE_CAPACITY)
            .baseCapacity(BASE_CAPACITY)
            .expandTimes(0)
            .usedSlots(0)
            .items(new ArrayList<>())
            .build();
        
        Warehouse warehouse = Warehouse.builder()
            .id(warehouseId)
            .userId(userId)
            .equipmentStorage(equipmentStorage)
            .itemStorage(itemStorage)
            .createTime(System.currentTimeMillis())
            .updateTime(System.currentTimeMillis())
            .build();
        
        warehouseStorage.put(warehouseId, warehouse);
        return warehouse;
    }
    
    /**
     * 保存仓库
     */
    public Warehouse save(Warehouse warehouse) {
        warehouse.setUpdateTime(System.currentTimeMillis());
        warehouseStorage.put(warehouse.getId(), warehouse);
        return warehouse;
    }
    
    /**
     * 获取扩充费用
     */
    public int getExpandCost(int expandTimes) {
        if (expandTimes >= MAX_EXPAND_TIMES) {
            return -1; // 已达最大扩充次数
        }
        // 100, 200, 400, 800
        return 100 * (int) Math.pow(2, expandTimes);
    }
    
    /**
     * 获取扩充容量
     */
    public int getExpandAmount() {
        return EXPAND_AMOUNT;
    }
    
    /**
     * 获取最大扩充次数
     */
    public int getMaxExpandTimes() {
        return MAX_EXPAND_TIMES;
    }
}

