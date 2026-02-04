package com.tencent.wxcloudrun.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 国战模型
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NationWar {
    
    /**
     * 国战ID
     */
    private String id;
    
    /**
     * 国战日期 (yyyy-MM-dd)
     */
    private String warDate;
    
    /**
     * 国战状态
     */
    private WarStatus status;
    
    /**
     * 进攻方国家
     */
    private String attackNation;
    
    /**
     * 防守方国家
     */
    private String defendNation;
    
    /**
     * 目标城市ID
     */
    private String targetCityId;
    
    /**
     * 目标城市名称
     */
    private String targetCityName;
    
    /**
     * 进攻方报名玩家
     */
    private List<WarParticipant> attackers;
    
    /**
     * 防守方报名玩家
     */
    private List<WarParticipant> defenders;
    
    /**
     * 进攻方积分
     */
    @Builder.Default
    private Integer attackScore = 0;
    
    /**
     * 防守方积分
     */
    @Builder.Default
    private Integer defendScore = 0;
    
    /**
     * 胜利点数
     */
    @Builder.Default
    private Integer victoryPoint = 10000;
    
    /**
     * 战斗记录
     */
    private List<WarBattle> battles;
    
    /**
     * 获胜方
     */
    private String winner;
    
    /**
     * 报名开始时间
     */
    private Long signUpStartTime;
    
    /**
     * 报名结束时间
     */
    private Long signUpEndTime;
    
    /**
     * 战斗开始时间
     */
    private Long battleStartTime;
    
    /**
     * 战斗结束时间
     */
    private Long battleEndTime;
    
    /**
     * 创建时间
     */
    private Long createTime;
    
    // ==================== 内部类 ====================
    
    /**
     * 国战状态
     */
    public enum WarStatus {
        PREPARING,      // 准备中（未到报名时间）
        SIGN_UP,        // 报名中
        MATCHING,       // 匹配中（报名结束，等待战斗开始）
        FIGHTING,       // 战斗中
        FINISHED        // 已结束
    }
    
    /**
     * 参战玩家
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WarParticipant {
        private String odUserId;
        private String playerName;
        private String nation;
        private Integer level;
        private Integer power;
        private Long signUpTime;
        
        // 战绩
        @Builder.Default
        private Integer wins = 0;
        @Builder.Default
        private Integer losses = 0;
        @Builder.Default
        private Integer scoreGained = 0;
        @Builder.Default
        private Integer meritGained = 0;  // 军功
        
        // 是否已淘汰
        @Builder.Default
        private Boolean eliminated = false;
    }
    
    /**
     * 战斗记录
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WarBattle {
        private String battleId;
        private Integer round;
        
        private String attackerId;
        private String attackerName;
        private Integer attackerPower;
        
        private String defenderId;
        private String defenderName;
        private Integer defenderPower;
        
        private String winnerId;
        private String winnerName;
        
        private Integer scoreGained;
        private Integer meritGained;
        
        private Long battleTime;
        private String battleLog;
    }
    
    /**
     * 国家信息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Nation {
        private String id;
        private String name;
        private String color;
        private String capitalId;       // 国都ID
        private String capitalName;     // 国都名称
        private List<String> cities;    // 拥有的城市ID列表
        private Integer totalPlayers;   // 总玩家数
        private Double meritExchangeRate; // 军功兑换比例
    }
    
    /**
     * 城市信息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class City {
        private String id;
        private String name;
        private String owner;           // 所属国家
        private Integer x;              // 地图X坐标
        private Integer y;              // 地图Y坐标
        private List<String> neighbors; // 相邻城市ID
        private Boolean isCapital;      // 是否为国都
        private Integer defenseBonus;   // 防御加成
    }
    
    /**
     * 国战地图
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WarMap {
        private List<Nation> nations;
        private List<City> cities;
        private Map<String, List<String>> borders; // 国家边界 (国家ID -> 接壤国家ID列表)
    }
    
    /**
     * 军功兑换记录
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MeritExchange {
        private String odUserId;
        private Integer meritUsed;
        private Long silverGained;
        private Double exchangeRate;
        private Long exchangeTime;
    }
}
