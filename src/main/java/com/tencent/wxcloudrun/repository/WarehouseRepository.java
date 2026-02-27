package com.tencent.wxcloudrun.repository;

import com.tencent.wxcloudrun.dao.WarehouseMapper;
import com.tencent.wxcloudrun.model.Warehouse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.UUID;

/**
 * 仓库数据仓库（数据库存储）
 */
@Repository
public class WarehouseRepository {
    
    @Autowired
    private WarehouseMapper warehouseMapper;
    
    private static final int BASE_CAPACITY = 100;
    private static final int EXPAND_AMOUNT = 100;
    private static final int MAX_EXPAND_TIMES = 4;
    
    /**
     * 根据用户ID查找仓库
     */
    public Warehouse findByUserId(String userId) {
        return warehouseMapper.findByUserId(userId);
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
        
        warehouseMapper.upsertWarehouse(warehouse);
        return warehouse;
    }
    
    /**
     * 保存仓库
     */
    public Warehouse save(Warehouse warehouse) {
        warehouse.setUpdateTime(System.currentTimeMillis());
        warehouseMapper.upsertWarehouse(warehouse);
        warehouseMapper.deleteItemsByWarehouseId(warehouse.getId());
        if (warehouse.getItemStorage() != null && warehouse.getItemStorage().getItems() != null
                && !warehouse.getItemStorage().getItems().isEmpty()) {
            warehouseMapper.insertItems(warehouse.getId(), warehouse.getUserId(), warehouse.getItemStorage().getItems());
        }
        return warehouse;
    }
    
    public int getExpandCost(int expandTimes) {
        if (expandTimes >= MAX_EXPAND_TIMES) { return -1; }
        return 100 * (int) Math.pow(2, expandTimes);
    }
    
    public int getExpandAmount() { return EXPAND_AMOUNT; }
    
    public int getMaxExpandTimes() { return MAX_EXPAND_TIMES; }
}
