package com.tencent.wxcloudrun.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 生产制造系统模型
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Production {
    
    private String odUserId;
    
    // 生产设施
    private Facility silverMine;      // 白银矿
    private Facility metalMine;       // 金属矿
    private Facility farm;            // 农场
    private Facility paperMill;       // 造纸坊
    
    // 制造设施
    private ManufactureFacility arsenal;        // 军械局
    private ManufactureFacility workshop;       // 奇物坊
    private ManufactureFacility academy;        // 讲武堂
    
    /**
     * 生产设施
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Facility {
        private String id;
        private String name;              // 设施名称
        private String type;              // 类型: silver, metal, food, paper
        private Integer level;            // 当前等级
        private Integer maxLevel;         // 最大等级
        private Integer outputPerTime;    // 单次产量
        private Integer dailyLimit;       // 每日次数上限
        private Integer usedToday;        // 今日已使用次数
        private String lastResetDate;     // 上次重置日期
        private String icon;              // 图标
        
        // 升级所需资源
        private Long upgradeSilver;
        private Long upgradeMetal;
        private Long upgradeFood;
        private Long upgradePaper;
    }
    
    /**
     * 制造设施
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ManufactureFacility {
        private String id;
        private String name;              // 设施名称
        private String type;              // 类型: arsenal, workshop, academy
        private Integer level;            // 当前等级
        private Integer maxLevel;         // 最大等级
        private String icon;              // 图标
        private String description;       // 描述
        
        // 升级所需资源
        private Long upgradeSilver;
        private Long upgradeMetal;
        private Long upgradeFood;
        private Long upgradePaper;
    }
    
    /**
     * 制造配方
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Recipe {
        private String id;
        private String name;              // 配方名称
        private String facilityType;      // 所需设施类型
        private Integer requiredLevel;    // 所需设施等级
        private String resultType;        // 产出类型: equipment, item, tactics
        private String resultId;          // 产出物品ID
        private String resultName;        // 产出物品名称
        private Integer resultCount;      // 产出数量
        private String quality;           // 品质
        private String icon;              // 图标
        private String description;       // 描述
        
        // 所需资源
        private Long costSilver;
        private Long costMetal;
        private Long costFood;
        private Long costPaper;
        private Integer costTime;         // 制造时间(秒)
    }
    
    /**
     * 创建默认生产系统
     */
    public static Production createDefault(String odUserId) {
        return Production.builder()
                .odUserId(odUserId)
                .silverMine(createFacility("silver", "白银矿", 1, 120, 300))
                .metalMine(createFacility("metal", "金属矿", 1, 80, 100))
                .farm(createFacility("food", "农场", 1, 80, 200))
                .paperMill(createFacility("paper", "造纸坊", 1, 80, 100))
                .arsenal(createManufactureFacility("arsenal", "军械局", "制造各种将领装备"))
                .workshop(createManufactureFacility("workshop", "奇物坊", "制造各种奇门道具"))
                .academy(createManufactureFacility("academy", "讲武堂", "制造各种兵法兵书"))
                .build();
    }
    
    private static Facility createFacility(String type, String name, int level, int output, int dailyLimit) {
        return Facility.builder()
                .id(type + "_facility")
                .name(name)
                .type(type)
                .level(level)
                .maxLevel(20)
                .outputPerTime(output)
                .dailyLimit(dailyLimit)
                .usedToday(0)
                .lastResetDate("")
                .icon("/images/facility_" + type + ".png")
                .upgradeSilver(1000L * level)
                .upgradeMetal(500L * level)
                .upgradeFood(500L * level)
                .upgradePaper(200L * level)
                .build();
    }
    
    private static ManufactureFacility createManufactureFacility(String type, String name, String desc) {
        return ManufactureFacility.builder()
                .id(type + "_facility")
                .name(name)
                .type(type)
                .level(1)
                .maxLevel(20)
                .icon("/images/manufacture_" + type + ".png")
                .description(desc)
                .upgradeSilver(2000L)
                .upgradeMetal(1000L)
                .upgradeFood(1000L)
                .upgradePaper(500L)
                .build();
    }
}
