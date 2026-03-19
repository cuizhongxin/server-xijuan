package com.tencent.wxcloudrun.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 战役模型
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Campaign {
    
    /**
     * 战役ID
     */
    private String id;
    
    /**
     * 战役名称
     */
    private String name;
    
    /**
     * 战役描述
     */
    private String description;
    
    /**
     * 战役图标
     */
    private String icon;
    
    /**
     * 战役背景图
     */
    private String backgroundImage;
    
    /**
     * 敌人等级范围 - 最小
     */
    private Integer enemyLevelMin;
    
    /**
     * 敌人等级范围 - 最大
     */
    private Integer enemyLevelMax;
    
    /**
     * 经验奖励范围 - 最小
     */
    private Integer expRewardMin;
    
    /**
     * 经验奖励范围 - 最大
     */
    private Integer expRewardMax;
    
    /**
     * 每日可挑战次数
     */
    private Integer dailyLimit;
    
    /**
     * 精力消耗
     */
    private Integer staminaCost;
    
    /**
     * 解锁所需君主等级
     */
    private Integer requiredLevel;
    
    /**
     * 关卡列表
     */
    private List<Stage> stages;
    
    /**
     * 掉落装备预览
     */
    private List<DropPreview> dropPreviews;
    
    /**
     * 战役顺序
     */
    private Integer order;
    
    /**
     * 是否开放
     */
    @Builder.Default
    private Boolean enabled = true;
    
    // ==================== 内部类 ====================
    
    /**
     * 战役关卡
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Stage {
        private String id;
        private Integer stageNum;
        private String name;
        private String enemyGeneralName;
        private String enemyGeneralIcon;
        private Integer enemyLevel;
        private Integer enemyTroops;
        private Integer enemyAttack;
        private Integer enemyDefense;
        private Integer enemyValor;
        private Integer enemyCommand;
        private Integer enemyDodge;
        private Integer enemyMobility;
        private Integer enemySoldierTier;
        private Integer enemyFormationLevel;
        private String enemyTroopType;
        private Integer expReward;
        private Long silverReward;
        private List<StageDrop> drops;
        @Builder.Default
        private Boolean isBoss = false;

        /**
         * 阵型NPC列表（6个位置的完整阵型）
         */
        private List<StageNpc> formation;
    }

    /**
     * 阵型中的单个NPC
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StageNpc {
        private Integer position;       // 0-5 (0-2前排, 3-5后排)
        private String name;
        private String avatar;
        private Integer level;
        private String troopType;       // 步/骑/弓
        private Integer soldierCount;
        private Integer soldierTier;
        private Integer attack;
        private Integer defense;
        private Integer valor;
        private Integer command;
        private Integer dodge;
        private Integer mobility;
        private Integer hp;
        @Builder.Default
        private Boolean isBoss = false;
        private String tacticsId;
    }

    /**
     * 关卡掉落
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StageDrop {
        private String type;            // EQUIP_PRE / ITEM / RESOURCE
        private String itemId;
        private String itemName;
        private String icon;
        private String quality;
        private Integer equipPreId;     // 关联 equipment_pre.id
        private Integer dropRate;       // 0-100
        @Builder.Default
        private Integer minCount = 1;
        @Builder.Default
        private Integer maxCount = 1;
    }
    
    /**
     * 掉落预览（展示在战役列表）
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DropPreview {
        private String icon;
        private String name;
        private String quality;
    }
}
