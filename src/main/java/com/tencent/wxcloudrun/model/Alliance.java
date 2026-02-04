package com.tencent.wxcloudrun.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * 联盟实体类
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Alliance {
    
    private String id;
    private String name;                    // 联盟名称
    private String faction;                 // 国家（魏、蜀、吴）
    private String leaderId;                // 盟主用户ID
    private String leaderName;              // 盟主名称
    private String announcement;            // 联盟公告
    private Integer level;                  // 联盟等级
    private Long exp;                       // 联盟经验
    private Integer maxMembers;             // 最大成员数
    private Integer memberCount;            // 当前成员数
    private Long totalPower;                // 联盟总战力
    private Long createTime;
    private Long updateTime;
    
    // 成员列表
    private List<AllianceMember> members;
    
    // 申请列表
    private List<AllianceApplication> applications;
    
    /**
     * 联盟成员
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AllianceMember {
        private String odUserId;
        private String name;                // 成员名称
        private String position;            // 职位：盟主、副盟主、精英、成员
        private Integer level;              // 等级
        private Long contribution;          // 累计贡献
        private Long power;                 // 战力
        private Long joinTime;              // 加入时间
        private Long lastOnlineTime;        // 最后在线时间
    }
    
    /**
     * 入盟申请
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AllianceApplication {
        private String odUserId;
        private String name;
        private Integer level;
        private Long power;
        private Long applyTime;
        private String status;              // pending、approved、rejected
    }
    
    /**
     * 职位枚举
     */
    public static class Position {
        public static final String LEADER = "盟主";
        public static final String VICE_LEADER = "副盟主";
        public static final String ELITE = "精英";
        public static final String MEMBER = "成员";
    }
    
    /**
     * 创建新联盟
     */
    public static Alliance create(String name, String faction, String leaderId, String leaderName, Integer leaderLevel, Long leaderPower) {
        Alliance alliance = Alliance.builder()
                .id("alliance_" + System.currentTimeMillis())
                .name(name)
                .faction(faction)
                .leaderId(leaderId)
                .leaderName(leaderName)
                .announcement("欢迎加入" + name + "！")
                .level(1)
                .exp(0L)
                .maxMembers(30)             // 初始最大30人
                .memberCount(1)
                .totalPower(leaderPower != null ? leaderPower : 0L)
                .createTime(System.currentTimeMillis())
                .updateTime(System.currentTimeMillis())
                .members(new ArrayList<>())
                .applications(new ArrayList<>())
                .build();
        
        // 添加盟主作为第一个成员
        AllianceMember leader = AllianceMember.builder()
                .odUserId(leaderId)
                .name(leaderName)
                .position(Position.LEADER)
                .level(leaderLevel != null ? leaderLevel : 1)
                .contribution(0L)
                .power(leaderPower != null ? leaderPower : 0L)
                .joinTime(System.currentTimeMillis())
                .lastOnlineTime(System.currentTimeMillis())
                .build();
        alliance.getMembers().add(leader);
        
        return alliance;
    }
}
