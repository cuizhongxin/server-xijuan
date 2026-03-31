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
        private String battleReportJson;
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
        private String pic;             // APK 城市精灵图名称
        private Integer flagX;          // 旗帜偏移X
        private Integer flagY;          // 旗帜偏移Y
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

    // ==================== 新国战会话模型 ====================

    public enum SessionPhase {
        PREPARING,
        REGISTRATION,
        BATTLE,
        FINISHED
    }

    /**
     * 国战日会话（每天一个，管理所有城市战场）
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NationWarSession {
        private String date;
        private SessionPhase phase;
        /** nationId -> 锁定的进攻目标cityId（null=未锁定） */
        @Builder.Default
        private Map<String, String> nationTargets = new java.util.LinkedHashMap<>();
        /** cityId -> 各国报名信息 */
        @Builder.Default
        private Map<String, CityRegistration> registrations = new java.util.LinkedHashMap<>();
        /** cityId -> 城市战场 */
        @Builder.Default
        private Map<String, CityBattle> cityBattles = new java.util.LinkedHashMap<>();
        /** odUserId -> 玩家战斗状态 */
        @Builder.Default
        private Map<String, PlayerWarState> playerStates = new java.util.LinkedHashMap<>();
        @Builder.Default
        private Integer currentRound = 0;
        private Long phaseStartTime;
        private Long phaseEndTime;
        private Long createTime;
    }

    /**
     * 单个城市的报名信息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CityRegistration {
        /** nationId -> 该国报名玩家列表 */
        @Builder.Default
        private Map<String, List<WarParticipant>> nationSignups = new java.util.LinkedHashMap<>();
    }

    /**
     * 城市战场
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CityBattle {
        private String cityId;
        private String cityName;
        private String sideANation;
        private String sideBNation;
        @Builder.Default
        private Integer sideAScore = 0;
        @Builder.Default
        private Integer sideBScore = 0;
        @Builder.Default
        private Integer victoryPoint = 10000;
        @Builder.Default
        private List<NpcDefender> npcDefenders = new java.util.ArrayList<>();
        @Builder.Default
        private List<RoundResult> rounds = new java.util.ArrayList<>();
        private String winner;
        @Builder.Default
        private Boolean isChibiBattle = false;
    }

    /**
     * NPC守军
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NpcDefender {
        private String npcId;
        private String name;
        private String nation;
        private Integer level;
        private Integer power;
        @Builder.Default
        private Integer remainingSoldiers = 100;
        @Builder.Default
        private Boolean dead = false;
    }

    /**
     * 单轮结算结果
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RoundResult {
        private Integer roundNumber;
        private String cityId;
        private Long timestamp;
        @Builder.Default
        private List<RoundFight> fights = new java.util.ArrayList<>();
        @Builder.Default
        private List<RoundBye> byes = new java.util.ArrayList<>();
        private Integer sideAScoreAfter;
        private Integer sideBScoreAfter;
    }

    /**
     * 单次对战记录
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RoundFight {
        private String attackerId;
        private String attackerName;
        private Integer attackerLevel;
        private String defenderId;
        private String defenderName;
        private Integer defenderLevel;
        private String winnerId;
        private String winnerName;
        private Integer meritGained;
        private Integer scoreGained;
        private String battleReportJson;
    }

    /**
     * 轮空记录
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RoundBye {
        private String playerId;
        private String playerName;
        private Integer level;
        private String side;
        private Integer meritGained;
        private Integer scoreGained;
    }

    /**
     * 玩家国战实时状态
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PlayerWarState {
        private String odUserId;
        private String playerName;
        private String nation;
        private Integer level;
        private Integer power;
        private String currentCityId;
        private String side; // "ATTACK" or "DEFEND"
        @Builder.Default
        private Integer roundsAtCurrentCity = 0;
        /** generalSlotId -> 剩余兵力 */
        @Builder.Default
        private Map<String, Integer> remainingSoldiers = new java.util.LinkedHashMap<>();
        /** generalSlotId -> 最大兵力（用于百分比显示） */
        @Builder.Default
        private Map<String, Integer> maxSoldiers = new java.util.LinkedHashMap<>();
        @Builder.Default
        private Boolean allDead = false;
        @Builder.Default
        private Boolean canSwitch = false;
        @Builder.Default
        private Integer totalMerit = 0;
        @Builder.Default
        private Integer totalScore = 0;
        @Builder.Default
        private Integer wins = 0;
        @Builder.Default
        private Integer losses = 0;
        @Builder.Default
        private Integer byeCount = 0;
        private Integer vipLevel;
    }
}
