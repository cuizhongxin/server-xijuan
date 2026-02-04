package com.tencent.wxcloudrun.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 用户战役进度模型
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CampaignProgress {
    
    /**
     * 进度ID
     */
    private String id;
    
    /**
     * 用户ID
     */
    private String userId;
    
    /**
     * 战役ID
     */
    private String campaignId;
    
    /**
     * 当前关卡序号（正在进行的关卡）
     */
    private Integer currentStage;
    
    /**
     * 最高通关关卡（已通关的最高关卡）
     */
    private Integer maxClearedStage;
    
    /**
     * 今日已挑战次数
     */
    @Builder.Default
    private Integer todayChallengeCount = 0;
    
    /**
     * 今日日期（用于重置）
     */
    private String todayDate;
    
    /**
     * 当前战役状态：IDLE(未开始), IN_PROGRESS(进行中), PAUSED(暂停), COMPLETED(已完成)
     */
    @Builder.Default
    private String status = "IDLE";
    
    /**
     * 当前战役中的兵力
     */
    private Integer currentTroops;
    
    /**
     * 最大兵力
     */
    private Integer maxTroops;
    
    /**
     * 当前战役中的重生次数
     */
    @Builder.Default
    private Integer reviveCount = 3;
    
    /**
     * 战役中使用的武将ID
     */
    private String generalId;
    
    /**
     * 是否已全部通关（解锁扫荡）
     */
    @Builder.Default
    private Boolean fullCleared = false;
    
    /**
     * 累计获得经验
     */
    @Builder.Default
    private Long totalExpGained = 0L;
    
    /**
     * 累计获得白银
     */
    @Builder.Default
    private Long totalSilverGained = 0L;
    
    /**
     * 战役开始时间
     */
    private Long startTime;
    
    /**
     * 最后更新时间
     */
    private Long updateTime;
    
    // ==================== 内部类 ====================
    
    /**
     * 扫荡结果
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SweepResult {
        /**
         * 扫荡的关卡数
         */
        private Integer stagesSwept;
        
        /**
         * 获得的总经验
         */
        private Long totalExp;
        
        /**
         * 获得的总白银
         */
        private Long totalSilver;
        
        /**
         * 获得的物品列表
         */
        private List<DropItem> items;
        
        /**
         * 消耗的虎符数量
         */
        private Integer tigerTallyUsed;
        
        /**
         * 是否因失败而停止
         */
        @Builder.Default
        private Boolean stoppedByFailure = false;
    }
    
    /**
     * 掉落物品
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DropItem {
        private String type;
        private String itemId;
        private String itemName;
        private String icon;
        private String quality;
        private Integer count;
    }
    
    /**
     * 战斗结果
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BattleResult {
        /**
         * 是否胜利
         */
        private Boolean victory;
        
        /**
         * 关卡序号
         */
        private Integer stageNum;
        
        /**
         * 获得经验
         */
        private Long expGained;
        
        /**
         * 获得白银
         */
        private Long silverGained;
        
        /**
         * 掉落物品
         */
        private List<DropItem> drops;
        
        /**
         * 剩余兵力
         */
        private Integer remainingTroops;
        
        /**
         * 损失兵力
         */
        private Integer troopsLost;
        
        /**
         * 战斗日志
         */
        private List<String> battleLog;
        
        /**
         * 是否为最后一关
         */
        @Builder.Default
        private Boolean isLastStage = false;
        
        /**
         * 是否首次通关此关卡
         */
        @Builder.Default
        private Boolean isFirstClear = false;
    }
}
