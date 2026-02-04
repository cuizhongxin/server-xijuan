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
        /**
         * 关卡ID
         */
        private String id;
        
        /**
         * 关卡序号
         */
        private Integer stageNum;
        
        /**
         * 关卡名称
         */
        private String name;
        
        /**
         * 敌方将领名称
         */
        private String enemyGeneralName;
        
        /**
         * 敌方将领头像
         */
        private String enemyGeneralIcon;
        
        /**
         * 敌方将领等级
         */
        private Integer enemyLevel;
        
        /**
         * 敌方兵力
         */
        private Integer enemyTroops;
        
        /**
         * 敌方攻击力
         */
        private Integer enemyAttack;
        
        /**
         * 敌方防御力
         */
        private Integer enemyDefense;
        
        /**
         * 通关经验奖励
         */
        private Integer expReward;
        
        /**
         * 通关白银奖励
         */
        private Long silverReward;
        
        /**
         * 掉落物品列表
         */
        private List<StageDrop> drops;
        
        /**
         * 是否BOSS关
         */
        @Builder.Default
        private Boolean isBoss = false;
    }
    
    /**
     * 关卡掉落
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StageDrop {
        /**
         * 物品类型：EQUIPMENT(装备), ITEM(道具), RESOURCE(资源)
         */
        private String type;
        
        /**
         * 物品ID
         */
        private String itemId;
        
        /**
         * 物品名称
         */
        private String itemName;
        
        /**
         * 物品图标
         */
        private String icon;
        
        /**
         * 物品品质
         */
        private String quality;
        
        /**
         * 掉落概率 (0-100)
         */
        private Integer dropRate;
        
        /**
         * 掉落数量最小
         */
        @Builder.Default
        private Integer minCount = 1;
        
        /**
         * 掉落数量最大
         */
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
