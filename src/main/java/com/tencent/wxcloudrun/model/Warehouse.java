package com.tencent.wxcloudrun.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 仓库实体类
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Warehouse {
    
    private String id;
    private String userId;
    
    // 装备仓库
    private EquipmentStorage equipmentStorage;
    
    // 物品仓库
    private ItemStorage itemStorage;
    
    private Long createTime;
    private Long updateTime;
    
    /**
     * 装备仓库
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EquipmentStorage {
        private Integer capacity;           // 当前容量
        private Integer baseCapacity;       // 基础容量(100)
        private Integer expandTimes;        // 扩充次数
        private Integer usedSlots;          // 已使用槽位
        private List<String> equipmentIds;  // 存储的装备ID列表
    }
    
    /**
     * 物品仓库
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ItemStorage {
        private Integer capacity;           // 当前容量
        private Integer baseCapacity;       // 基础容量(100)
        private Integer expandTimes;        // 扩充次数
        private Integer usedSlots;          // 已使用槽位
        private List<WarehouseItem> items;  // 存储的物品列表
    }
    
    /**
     * 仓库物品
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WarehouseItem {
        private String itemId;              // 物品ID
        private String itemType;            // 物品类型
        private String name;                // 物品名称
        private String icon;                // 图标
        private String quality;             // 品质
        private Integer count;              // 数量
        private Integer maxStack;           // 最大堆叠数
        private String description;         // 描述
        private Boolean usable;             // 是否可使用
        private Boolean bound;              // 是否绑定
    }
}

