package com.tencent.wxcloudrun.dto;

import com.tencent.wxcloudrun.model.General;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 招募结果
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecruitResult {
    
    /**
     * 招募到的武将列表
     */
    private List<General> generals;
    
    /**
     * 剩余招贤令数量
     */
    private Integer remainingTokens;
    
    /**
     * 是否有新的橙色武将
     */
    private Boolean hasOrange;
    
    /**
     * 是否有新的紫色武将
     */
    private Boolean hasPurple;
}


