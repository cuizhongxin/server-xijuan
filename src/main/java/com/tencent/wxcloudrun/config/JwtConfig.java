package com.tencent.wxcloudrun.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * JWT配置类
 */
@Data
@Component
@ConfigurationProperties(prefix = "jwt")
public class JwtConfig {
    /**
     * JWT密钥
     */
    private String secret;
    
    /**
     * token过期时间（秒）
     */
    private Long expiration;
}


