package com.tencent.wxcloudrun.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * 联盟战实体类
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AllianceWar {
    
    private String id;
    private String date;                    // 战斗日期 yyyy-MM-dd
    private WarStatus status;               // 战斗状态
    private Integer currentRound;           // 当前轮次
    private Long startTime;                 // 开始时间
    private Long endTime;                   // 结束时间
    
    // 参战玩家
    private List<WarParticipant> participants;
    
    // 对战记录
    private List<WarBattle> battles;
    
    // 联盟排名（战斗结束后计算）
    private List<AllianceRank> allianceRanks;
    
    // 个人排名
    private List<PlayerRank> playerRanks;
    
    /**
     * 战斗状态
     */
    public enum WarStatus {
        NOT_STARTED,    // 未开始
        REGISTERING,    // 报名中 (20:45-21:00)
        IN_PROGRESS,    // 进行中 (21:00开始)
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
        private String allianceId;
        private String allianceName;
        private String faction;             // 国家
        private Integer playerNumber;       // 编号
        private Integer level;
        private Long power;
        private PlayerStatus status;        // 当前状态
        private Integer wins;               // 胜场数
        private Integer losses;             // 败场数
        private Integer flags;              // 夺取的军旗数
        private Long registerTime;
    }
    
    /**
     * 玩家状态
     */
    public enum PlayerStatus {
        WAITING,        // 等待配对
        IN_BATTLE,      // 战斗中
        SPECTATING,     // 观战中（已淘汰）
        WINNER          // 最终获胜者
    }
    
    /**
     * 对战记录
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WarBattle {
        private String id;
        private Integer round;              // 轮次
        private String player1Id;
        private String player1Name;
        private String player1Alliance;
        private String player2Id;
        private String player2Name;
        private String player2Alliance;
        private String winnerId;
        private String winnerName;
        private Integer player1Score;       // 玩家1得分
        private Integer player2Score;       // 玩家2得分
        private Long startTime;
        private Long endTime;
        private List<BattleRound> rounds;   // 战斗回合
    }
    
    /**
     * 战斗回合
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BattleRound {
        private Integer roundNum;
        private String attackerId;
        private String defenderId;
        private Integer damage;
        private String description;
    }
    
    /**
     * 联盟排名
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AllianceRank {
        private Integer rank;
        private String allianceId;
        private String allianceName;
        private String faction;
        private Integer totalFlags;         // 总军旗数
        private Integer participantCount;   // 参战人数
        private Integer wins;               // 总胜场
        private Integer losses;             // 总败场
        private List<String> rewards;       // 获得的奖励
    }
    
    /**
     * 个人排名
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PlayerRank {
        private Integer rank;
        private String odUserId;
        private String playerName;
        private String allianceName;
        private Integer wins;
        private Integer flags;
        private List<String> rewards;
    }
    
    /**
     * 创建新的盟战
     */
    public static AllianceWar createNew(String date) {
        return AllianceWar.builder()
                .id("war_" + System.currentTimeMillis())
                .date(date)
                .status(WarStatus.NOT_STARTED)
                .currentRound(0)
                .participants(new ArrayList<>())
                .battles(new ArrayList<>())
                .allianceRanks(new ArrayList<>())
                .playerRanks(new ArrayList<>())
                .build();
    }
}
