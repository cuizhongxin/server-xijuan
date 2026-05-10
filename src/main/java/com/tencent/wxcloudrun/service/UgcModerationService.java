package com.tencent.wxcloudrun.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.tencent.wxcloudrun.config.WechatConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

@Service
public class UgcModerationService {

    private static final Logger log = LoggerFactory.getLogger(UgcModerationService.class);
    private static final long ACCESS_TOKEN_REFRESH_BUFFER_MS = 60_000L;

    private final WechatConfig wechatConfig;
    private final RestTemplate restTemplate = new RestTemplate();
    private final AtomicReference<String> accessTokenCache = new AtomicReference<>();
    private final AtomicLong accessTokenExpireAt = new AtomicLong(0L);

    @Value("${ugc.moderation.enabled:true}")
    private boolean enabled;

    @Value("${ugc.moderation.wechat-enabled:false}")
    private boolean wechatEnabled;

    @Value("${ugc.moderation.wechat-fail-open:true}")
    private boolean wechatFailOpen;

    @Value("${ugc.moderation.blocked-keywords:}")
    private String blockedKeywordsRaw;

    @Value("${ugc.moderation.name-mask:合规玩家}")
    private String nameMask;

    @Value("${ugc.moderation.content-mask:[内容已屏蔽]}")
    private String contentMask;

    public UgcModerationService(WechatConfig wechatConfig) {
        this.wechatConfig = wechatConfig;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public String getNameMask() {
        return safeMask(nameMask, "合规玩家");
    }

    public String getContentMask() {
        return safeMask(contentMask, "[内容已屏蔽]");
    }

    public List<String> getBlockedKeywords() {
        if (blockedKeywordsRaw == null || blockedKeywordsRaw.trim().isEmpty()) return Collections.emptyList();
        String[] arr = blockedKeywordsRaw.split("[,，]");
        List<String> result = new ArrayList<>();
        for (String s : arr) {
            if (s == null) continue;
            String t = s.trim();
            if (!t.isEmpty()) result.add(t);
        }
        return result;
    }

    public boolean containsBlockedKeyword(String text) {
        if (!enabled) return false;
        if (text == null || text.isEmpty()) return false;
        if (containsBlockedKeywordLocal(text)) {
            return true;
        }
        return containsBlockedKeywordByWechat(text);
    }

    private boolean containsBlockedKeywordLocal(String text) {
        String lower = text == null ? "" : text.toLowerCase();
        for (String kw : getBlockedKeywords()) {
            if (kw.isEmpty()) continue;
            if (lower.contains(kw.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    private boolean containsBlockedKeywordByWechat(String text) {
        if (!wechatEnabled) return false;
        String content = text == null ? "" : text.trim();
        if (content.isEmpty()) return false;
        if (!isWechatConfigReady()) {
            if (!wechatFailOpen) return true;
            return false;
        }
        try {
            String token = getWechatAccessToken();
            if (token == null || token.isEmpty()) {
                if (!wechatFailOpen) return true;
                return false;
            }
            String checkUrl = wechatConfig.getApi().getMsgSecCheck();
            if (checkUrl == null || checkUrl.trim().isEmpty()) {
                checkUrl = "https://api.weixin.qq.com/wxa/msg_sec_check";
            }
            JSONObject body = new JSONObject();
            body.put("content", content);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            ResponseEntity<String> response = restTemplate.postForEntity(
                    checkUrl + "?access_token=" + token,
                    new HttpEntity<>(body.toJSONString(), headers),
                    String.class
            );
            JSONObject resp = JSON.parseObject(response.getBody());
            int errCode = resp == null ? -1 : resp.getIntValue("errcode");
            if (errCode == 0) {
                return false;
            }
            // 87014: 内容含有违法违规内容，需拦截
            if (errCode == 87014) {
                return true;
            }
            log.warn("微信内容安全接口返回异常，errcode={}, errmsg={}", errCode,
                    resp == null ? null : resp.getString("errmsg"));
            return !wechatFailOpen;
        } catch (Exception e) {
            log.warn("调用微信内容安全接口失败: {}", e.getMessage());
            return !wechatFailOpen;
        }
    }

    private boolean isWechatConfigReady() {
        if (wechatConfig == null) return false;
        if (wechatConfig.getAppId() == null || wechatConfig.getAppId().trim().isEmpty()) return false;
        if (wechatConfig.getAppSecret() == null || wechatConfig.getAppSecret().trim().isEmpty()) return false;
        if (wechatConfig.getApi() == null) return false;
        String tokenUrl = wechatConfig.getApi().getToken();
        return tokenUrl != null && !tokenUrl.trim().isEmpty();
    }

    private String getWechatAccessToken() {
        long now = System.currentTimeMillis();
        String cached = accessTokenCache.get();
        if (cached != null && !cached.isEmpty() && now < accessTokenExpireAt.get()) {
            return cached;
        }
        synchronized (this) {
            now = System.currentTimeMillis();
            cached = accessTokenCache.get();
            if (cached != null && !cached.isEmpty() && now < accessTokenExpireAt.get()) {
                return cached;
            }
            String tokenUrl = String.format(
                    "%s?grant_type=client_credential&appid=%s&secret=%s",
                    wechatConfig.getApi().getToken(),
                    wechatConfig.getAppId(),
                    wechatConfig.getAppSecret()
            );
            ResponseEntity<String> response = restTemplate.getForEntity(tokenUrl, String.class);
            JSONObject tokenResp = JSON.parseObject(response.getBody());
            int errCode = tokenResp == null ? -1 : tokenResp.getIntValue("errcode");
            if (errCode != 0 && tokenResp != null && tokenResp.containsKey("errcode")) {
                log.warn("获取微信access_token失败，errcode={}, errmsg={}",
                        errCode, tokenResp.getString("errmsg"));
                return null;
            }
            String accessToken = tokenResp == null ? null : tokenResp.getString("access_token");
            long expiresIn = tokenResp == null ? 0L : tokenResp.getLongValue("expires_in");
            if (accessToken == null || accessToken.trim().isEmpty()) {
                return null;
            }
            long expireAt = System.currentTimeMillis() + (expiresIn * 1000L) - ACCESS_TOKEN_REFRESH_BUFFER_MS;
            accessTokenCache.set(accessToken);
            accessTokenExpireAt.set(expireAt);
            return accessToken;
        }
    }

    public String maskIfBlockedName(String name) {
        if (containsBlockedKeyword(name)) return getNameMask();
        return name;
    }

    public String maskIfBlockedContent(String content) {
        if (containsBlockedKeyword(content)) return getContentMask();
        return content;
    }

    private String safeMask(String v, String def) {
        if (v == null || v.trim().isEmpty()) return def;
        return v.trim();
    }
}
