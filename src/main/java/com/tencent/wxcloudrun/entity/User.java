package com.tencent.wxcloudrun.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户实体类
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {
    /**
     * 用户ID
     */
    private Long id;
    
    /**
     * 微信openId
     */
    private String openId;
    
    /**
     * 微信unionId
     */
    private String unionId;
    
    /**
     * 用户昵称
     */
    private String nickName;
    
    /**
     * 用户头像
     */
    private String avatarUrl;
    
    /**
     * 创建时间
     */
    private Long createTime;
    
    /**
     * 更新时间
     */
    private Long updateTime;
}


