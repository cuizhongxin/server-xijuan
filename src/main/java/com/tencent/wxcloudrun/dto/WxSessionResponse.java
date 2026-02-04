package com.tencent.wxcloudrun.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * 微信code2session响应DTO
 */
@Data
public class WxSessionResponse {
    /**
     * 用户唯一标识
     */
    @JsonProperty("openid")
    private String openId;
    
    /**
     * 会话密钥
     */
    @JsonProperty("session_key")
    private String sessionKey;
    
    /**
     * 用户在开放平台的唯一标识符
     */
    @JsonProperty("unionid")
    private String unionId;
    
    /**
     * 错误码
     */
    @JsonProperty("errcode")
    private Integer errCode;
    
    /**
     * 错误信息
     */
    @JsonProperty("errmsg")
    private String errMsg;
}


