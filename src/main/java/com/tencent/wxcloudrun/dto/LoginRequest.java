package com.tencent.wxcloudrun.dto;

import lombok.Data;

/**
 * 登录请求DTO
 */
@Data
public class LoginRequest {
    /**
     * 微信登录凭证code
     */
    private String code;
    
    /**
     * 用户信息（加密数据）
     */
    private String encryptedData;
    
    /**
     * 加密算法的初始向量
     */
    private String iv;
    
    /**
     * 用户昵称
     */
    private String nickName;
    
    /**
     * 用户头像URL
     */
    private String avatarUrl;
}


