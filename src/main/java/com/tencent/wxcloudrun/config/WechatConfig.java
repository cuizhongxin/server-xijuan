package com.tencent.wxcloudrun.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 微信配置类
 */
@Data
@Component
@ConfigurationProperties(prefix = "wechat.miniprogram")
public class WechatConfig {
    /**
     * 小程序AppID
     */
    private String appId;
    
    /**
     * 小程序AppSecret
     */
    private String appSecret;
    
    /**
     * API配置
     */
    private Api api;
    
    @Data
    public static class Api {
        /**
         * code2session接口地址
         */
        private String code2session;
    }
}


