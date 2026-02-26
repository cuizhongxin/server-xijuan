package com.tencent.wxcloudrun.dao;

import com.tencent.wxcloudrun.model.Warehouse;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface WarehouseMapper {
    
    Warehouse findByUserId(@Param("userId") String userId);
    
    void upsertWarehouse(Warehouse warehouse);
    
    void deleteItemsByWarehouseId(@Param("warehouseId") String warehouseId);
    
    void insertItems(@Param("warehouseId") String warehouseId, @Param("items") List<Warehouse.WarehouseItem> items);
}
