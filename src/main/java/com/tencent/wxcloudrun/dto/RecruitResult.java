package com.tencent.wxcloudrun.dto;

import com.tencent.wxcloudrun.model.General;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 招募结果
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecruitResult {
    
    /**
     * 招募到的武将（单抽只有1个）
     */
    private General general;
    
    /**
     * 本次获得的将魂（仅高级招募产出，5-20点）
     */
    private Integer soulPointGained;
    
    /**
     * 当前总将魂
     */
    private Integer totalSoulPoint;
    
    /**
     * 剩余招贤令数量
     */
    private Integer remainingTokens;
    
    /**
     * 招募类型（JUNIOR/INTERMEDIATE/SENIOR）
     */
    private String tokenType;
}
