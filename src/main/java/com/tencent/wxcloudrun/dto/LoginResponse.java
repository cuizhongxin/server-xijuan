package com.tencent.wxcloudrun.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 登录响应DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {
    /**
     * JWT token
     */
    private String token;
    
    /**
     * 用户ID（自增ID）
     */
    private Long userId;
    
    /**
     * 用户信息
     */
    private UserInfo userInfo;
    
    /**
     * 用户信息内部类
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserInfo {
        private String openId;
        private Long userId;
        private String nickName;
        private String avatarUrl;
    }
}


