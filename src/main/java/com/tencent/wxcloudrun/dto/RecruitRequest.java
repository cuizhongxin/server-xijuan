package com.tencent.wxcloudrun.dto;

import lombok.Data;

/**
 * 招募请求
 */
@Data
public class RecruitRequest {
    
    /**
     * 招贤令类型：JUNIOR(初级), INTERMEDIATE(中级), SENIOR(高级)
     */
    private String tokenType;
    
    /**
     * 招募次数（1次或10次连抽）
     */
    private Integer count;
}


