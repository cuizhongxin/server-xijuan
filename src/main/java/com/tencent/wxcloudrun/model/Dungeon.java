package com.tencent.wxcloudrun.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 副本模型
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Dungeon {
    
    /**
     * 副本ID
     */
    private String id;
    
    /**
     * 副本名称
     */
    private String name;
    
    /**
     * 副本描述
     */
    private String description;
    
    /**
     * 解锁等级
     */
    private Integer unlockLevel;
    
    /**
     * 副本图标
     */
    private String icon;
    
    /**
     * 背景图
     */
    private String background;
    
    /**
     * 体力消耗
     */
    @Builder.Default
    private Integer staminaCost = 10;
    
    /**
     * 每日进入次数限制
     */
    @Builder.Default
    private Integer dailyLimit = 5;
    
    /**
     * 守关NPC数量
     */
    private Integer npcCount;
    
    /**
     * 守关NPC列表
     */
    private List<DungeonNpc> npcs;
    
    /**
     * 副本奖励（通关奖励）
     */
    private Reward clearReward;
    
    /**
     * 副本故事背景
     */
    private String lore;
    
    /**
     * 推荐战力
     */
    private Integer recommendedPower;
    
    // ==================== 内部类 ====================
    
    /**
     * 副本奖励
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Reward {
        /**
         * 经验奖励
         */
        private Integer exp;
        
        /**
         * 银两奖励
         */
        private Long silver;
        
        /**
         * 黄金奖励
         */
        private Integer gold;
    }
}


