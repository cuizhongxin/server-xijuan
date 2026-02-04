package com.tencent.wxcloudrun.service;

import com.tencent.wxcloudrun.dto.LoginRequest;
import com.tencent.wxcloudrun.dto.LoginResponse;
import com.tencent.wxcloudrun.dto.WxSessionResponse;
import com.tencent.wxcloudrun.entity.User;
import com.tencent.wxcloudrun.exception.BusinessException;
import com.tencent.wxcloudrun.service.general.GeneralService;
import com.tencent.wxcloudrun.util.JwtUtil;
import com.tencent.wxcloudrun.util.WechatUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 认证服务类
 */
@Service
public class AuthService {
    
    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);
    
    @Autowired
    private WechatUtil wechatUtil;
    
    @Autowired
    private JwtUtil jwtUtil;
    
    @Autowired
    private UserIdService userIdService;
    
    @Autowired
    private GeneralService generalService;
    
    @Autowired
    private UserResourceService userResourceService;
    
    /**
     * 处理微信登录
     */
    public LoginResponse login(LoginRequest request) {
        // 1. 校验参数
        if (request.getCode() == null || request.getCode().isEmpty()) {
            throw new BusinessException("code不能为空");
        }
        
        logger.info("开始处理登录请求，code: {}", request.getCode());
        
        // 2. 调用微信接口获取openId和sessionKey
        WxSessionResponse wxSession = wechatUtil.code2Session(request.getCode());
        String openId = wxSession.getOpenId();
        
        logger.info("获取到openId: {}", openId);
        
        // 3. 获取或创建用户ID
        Long userId = userIdService.getOrCreateUserId(openId);
        logger.info("用户ID: openId={}, userId={}", openId, userId);
        
        // 4. 初始化用户数据
        String userIdStr = String.valueOf(userId);
        
        // 初始化用户资源
        userResourceService.getUserResource(userIdStr);
        logger.info("用户资源初始化完成, userId={}", userIdStr);
        
        // 初始化用户武将（如果是新用户）
        generalService.initUserGenerals(userIdStr);
        logger.info("用户武将初始化完成, userId={}", userIdStr);
        
        // 6. 创建或更新用户信息（实际项目中应该保存到数据库）
        User user = User.builder()
                .openId(openId)
                .unionId(wxSession.getUnionId())
                .nickName(request.getNickName())
                .avatarUrl(request.getAvatarUrl())
                .createTime(System.currentTimeMillis())
                .updateTime(System.currentTimeMillis())
                .build();
        
        logger.info("用户信息: {}", user);
        
        // 8. 生成JWT token
        String token = jwtUtil.generateToken(openId);
        
        logger.info("生成token成功");
        
        // 9. 构建响应
        LoginResponse.UserInfo userInfo = LoginResponse.UserInfo.builder()
                .openId(openId)
                .userId(userId)
                .nickName(user.getNickName())
                .avatarUrl(user.getAvatarUrl())
                .build();
        
        return LoginResponse.builder()
                .token(token)
                .userId(userId)
                .userInfo(userInfo)
                .build();
    }
}


