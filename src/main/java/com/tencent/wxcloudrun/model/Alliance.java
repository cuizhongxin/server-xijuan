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
    private String leaderId;                // 盟主用户ID
    private String leaderName;              // 盟主名称
    private Integer level;                  // 联盟等级
    private String notice;                  // 联盟公告
    private Integer maxMembers;             // 成员上限
    private Boolean autoApprove;            // 是否自动审批
    private Integer minLevel;               // 加入最低等级
    private Long createTime;
    private Long updateTime;
    
    // 成员列表（关联查询）
    private List<AllianceMember> members;
    
    // 申请列表（关联查询）
    private List<AllianceApplication> applications;
    
    /**
     * 联盟成员
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AllianceMember {
        private String userId;
        private String name;                // 成员名称
        private String role;                // 角色：leader/officer/member
        private Integer level;              // 等级
        private Long contribution;          // 累计贡献
        private Long joinTime;              // 加入时间
    }
    
    /**
     * 入盟申请
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AllianceApplication {
        private String userId;
        private String userName;
        private Integer userLevel;
        private String status;              // pending/approved/rejected
        private Long applyTime;
    }
    
    /**
     * 角色常量
     */
    public static class Role {
        public static final String LEADER = "leader";
        public static final String OFFICER = "officer";
        public static final String MEMBER = "member";
    }
    
    /**
     * 创建新联盟
     */
    public static Alliance create(String name, String leaderId, String leaderName, Integer leaderLevel) {
        Alliance alliance = Alliance.builder()
                .id("alliance_" + System.currentTimeMillis())
                .name(name)
                .leaderId(leaderId)
                .leaderName(leaderName)
                .notice("欢迎加入" + name + "！")
                .level(1)
                .maxMembers(30)
                .autoApprove(false)
                .minLevel(1)
                .createTime(System.currentTimeMillis())
                .updateTime(System.currentTimeMillis())
                .members(new ArrayList<>())
                .applications(new ArrayList<>())
                .build();
        
        AllianceMember leader = AllianceMember.builder()
                .userId(leaderId)
                .name(leaderName)
                .role(Role.LEADER)
                .level(leaderLevel != null ? leaderLevel : 1)
                .contribution(0L)
                .joinTime(System.currentTimeMillis())
                .build();
        alliance.getMembers().add(leader);
        
        return alliance;
    }
}
