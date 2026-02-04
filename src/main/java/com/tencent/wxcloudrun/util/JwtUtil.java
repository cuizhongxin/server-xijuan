package com.tencent.wxcloudrun.util;

import com.tencent.wxcloudrun.config.JwtConfig;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT工具类
 */
@Component
public class JwtUtil {
    
    @Autowired
    private JwtConfig jwtConfig;
    
    /**
     * 生成token
     */
    public String generateToken(String openId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("openId", openId);
        
        long now = System.currentTimeMillis();
        Date expireDate = new Date(now + jwtConfig.getExpiration() * 1000);
        
        SecretKey key = Keys.hmacShaKeyFor(jwtConfig.getSecret().getBytes(StandardCharsets.UTF_8));
        
        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(new Date(now))
                .setExpiration(expireDate)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }
    
    /**
     * 从token中获取openId
     */
    public String getOpenIdFromToken(String token) {
        Claims claims = parseToken(token);
        return claims.get("openId", String.class);
    }
    
    /**
     * 验证token是否有效
     */
    public boolean validateToken(String token) {
        try {
            parseToken(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * 解析token
     */
    private Claims parseToken(String token) {
        SecretKey key = Keys.hmacShaKeyFor(jwtConfig.getSecret().getBytes(StandardCharsets.UTF_8));
        
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}


