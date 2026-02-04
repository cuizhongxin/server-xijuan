package com.tencent.wxcloudrun.interceptor;

import com.tencent.wxcloudrun.exception.BusinessException;
import com.tencent.wxcloudrun.service.UserIdService;
import com.tencent.wxcloudrun.util.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 认证拦截器
 */
@Component
public class AuthInterceptor implements HandlerInterceptor {
    
    private static final Logger logger = LoggerFactory.getLogger(AuthInterceptor.class);
    
    @Autowired
    private JwtUtil jwtUtil;
    
    @Autowired
    private UserIdService userIdService;
    
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // OPTIONS请求直接放行
        if ("OPTIONS".equals(request.getMethod())) {
            return true;
        }
        
        // 获取token
        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        
        logger.debug("请求路径: {}, Token: {}", request.getRequestURI(), token);
        
        // 验证token
        if (token == null || token.isEmpty()) {
            throw new BusinessException(401, "未登录");
        }
        
        if (!jwtUtil.validateToken(token)) {
            throw new BusinessException(401, "token无效或已过期");
        }
        
        // 从token中获取openId
        String openId = jwtUtil.getOpenIdFromToken(token);
        
        // 优先从请求头获取userId
        String userIdHeader = request.getHeader("X-User-Id");
        Long userId = null;
        
        if (userIdHeader != null && !userIdHeader.isEmpty()) {
            try {
                userId = Long.parseLong(userIdHeader);
                // 验证userId是否有效
                if (!userIdService.isValidUserId(userId)) {
                    logger.warn("无效的userId: {}, 从openId重新获取", userId);
                    userId = null;
                }
            } catch (NumberFormatException e) {
                logger.warn("userId格式错误: {}", userIdHeader);
            }
        }
        
        // 如果请求头中没有userId或无效，则从openId获取
        if (userId == null) {
            userId = userIdService.getUserId(openId);
            if (userId == null) {
                // 如果openId也没有对应的userId，创建新的
                userId = userIdService.getOrCreateUserId(openId);
                logger.info("为openId创建新userId: openId={}, userId={}", openId, userId);
            }
        }
        
        // 将openId和userId存入request attribute
        request.setAttribute("openId", openId);
        request.setAttribute("userId", userId);
        
        logger.debug("设置用户信息: openId={}, userId={}", openId, userId);
        
        return true;
    }
}


