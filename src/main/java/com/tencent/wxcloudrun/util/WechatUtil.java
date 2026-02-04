package com.tencent.wxcloudrun.util;

import com.alibaba.fastjson.JSON;
import com.tencent.wxcloudrun.config.WechatConfig;
import com.tencent.wxcloudrun.dto.WxSessionResponse;
import com.tencent.wxcloudrun.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 * 微信工具类
 */
@Component
public class WechatUtil {
    
    private static final Logger logger = LoggerFactory.getLogger(WechatUtil.class);
    
    @Autowired
    private WechatConfig wechatConfig;
    
    private final RestTemplate restTemplate = new RestTemplate();
    
    /**
     * 通过code获取微信session信息
     */
    public WxSessionResponse code2Session(String code) {
        String url = String.format("%s?appid=%s&secret=%s&js_code=%s&grant_type=authorization_code",
                wechatConfig.getApi().getCode2session(),
                wechatConfig.getAppId(),
                wechatConfig.getAppSecret(),
                code);
        
        logger.info("调用微信code2session接口，code: {}", code);
        
        try {
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            String body = response.getBody();
            
            logger.info("微信code2session响应: {}", body);
            
            WxSessionResponse sessionResponse = JSON.parseObject(body, WxSessionResponse.class);
            
            // 检查是否有错误
            if (sessionResponse.getErrCode() != null && sessionResponse.getErrCode() != 0) {
                logger.error("微信code2session失败: {}", sessionResponse.getErrMsg());
                throw new BusinessException("微信授权失败: " + sessionResponse.getErrMsg());
            }
            
            // 检查openId是否为空
            if (sessionResponse.getOpenId() == null || sessionResponse.getOpenId().isEmpty()) {
                logger.error("微信code2session返回的openId为空");
                throw new BusinessException("微信授权失败：未获取到用户标识");
            }
            
            return sessionResponse;
            
        } catch (Exception e) {
            logger.error("调用微信code2session接口异常", e);
            throw new BusinessException("微信授权失败: " + e.getMessage());
        }
    }
}


