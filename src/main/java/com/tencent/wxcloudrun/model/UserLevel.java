package com.tencent.wxcloudrun.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户等级信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserLevel {
    
    /**
     * 用户ID
     */
    private String userId;
    
    /**
     * 当前等级
     */
    @Builder.Default
    private Integer level = 1;
    
    /**
     * 总经验值
     */
    @Builder.Default
    private Long totalExp = 0L;
    
    /**
     * 当前等级已获得经验
     */
    @Builder.Default
    private Long currentLevelExp = 0L;
    
    /**
     * 升到下一级需要的经验
     */
    @Builder.Default
    private Long expToNextLevel = 100L;
    
    /**
     * VIP等级
     */
    @Builder.Default
    private Integer vipLevel = 0;
    
    /**
     * 今日已获得经验
     */
    @Builder.Default
    private Long todayExp = 0L;
    
    /**
     * 上次更新日期（yyyyMMdd）
     */
    private String lastUpdateDate;
    
    /**
     * 创建时间
     */
    private Long createTime;
    
    /**
     * 更新时间
     */
    private Long updateTime;
}


