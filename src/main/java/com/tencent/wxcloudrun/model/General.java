package com.tencent.wxcloudrun.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 武将实体类
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class General {
    
    private String id;
    private String userId;          // 所属用户ID
    private String name;
    private Quality quality;
    private GeneralType type;
    private TroopType troopType;
    private Integer level;
    private Long exp;
    private Long maxExp;
    private String avatar;
    
    // 阵营（魏、蜀、吴、群、虚构）
    private String faction;
    
    // 特征列表（如"攻击力+300", "兵法发动概率+25%"等）
    private List<String> traits;
    
    // 属性
    private Attributes attributes;
    
    // 士兵信息
    private Soldiers soldiers;
    
    // 装备
    private Equipment equipment;
    
    // 兵法
    private Tactics tactics;
    
    // 状态
    private Status status;
    
    // 战斗统计
    private Stats stats;
    
    // 装备加成（战斗时计算）
    private Map<String, Integer> equipmentBonus;
    
    private Long createTime;
    private Long updateTime;
    
    /**
     * 品质
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Quality {
        private Integer id;
        private String name;
        private String color;
        private Double baseMultiplier;
        private Integer star;
        private String icon;
    }
    
    /**
     * 武将类型
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GeneralType {
        private Integer id;
        private String name;
        private String description;
        private String icon;
        private Map<String, Double> attributes;
    }
    
    /**
     * 兵种类型
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TroopType {
        private Integer id;
        private String name;
        private String icon;
        private String description;
        private Map<String, Double> attributes;
        private String restrains;       // 克制的兵种
        private String restrainedBy;    // 被克制的兵种
        private Double restrainBonus;   // 克制加成
    }
    
    /**
     * 属性
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Attributes {
        private Integer attack;
        private Integer defense;
        private Integer valor;
        private Integer command;
        private Double dodge;
        private Integer mobility;
        private Integer power;
    }
    
    /**
     * 士兵信息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Soldiers {
        private TroopType type;
        private Integer rank;
        private SoldierRankInfo rankInfo;
        private Integer count;
        private Integer maxCount;
    }
    
    /**
     * 士兵等级信息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SoldierRankInfo {
        private Integer level;
        private String name;
        private String icon;
        private Double powerMultiplier;
    }
    
    /**
     * 装备
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Equipment {
        /**
         * 主武器（刀/剑）- 槽位1
         */
        private String weaponId;
        
        /**
         * 头盔 - 槽位2
         */
        private String helmetId;
        
        /**
         * 铠甲 - 槽位3
         */
        private String armorId;
        
        /**
         * 戒指 - 槽位4
         */
        private String ringId;
        
        /**
         * 鞋子 - 槽位5
         */
        private String shoesId;
        
        /**
         * 项链 - 槽位6
         */
        private String necklaceId;
    }
    
    /**
     * 兵法
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Tactics {
        private Map<String, Object> primary;
        private Map<String, Object> secondary;
    }
    
    /**
     * 状态
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Status {
        private Boolean locked;
        private Boolean inBattle;
        private Boolean injured;
        private Integer morale;
    }
    
    /**
     * 战斗统计
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Stats {
        private Integer totalBattles;
        private Integer victories;
        private Integer defeats;
        private Integer kills;
        private Integer mvpCount;
    }
}

