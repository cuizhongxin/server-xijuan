package com.tencent.wxcloudrun.service;

import com.tencent.wxcloudrun.dto.LoginRequest;
import com.tencent.wxcloudrun.dto.LoginResponse;
import com.tencent.wxcloudrun.dto.WxSessionResponse;
import com.tencent.wxcloudrun.dao.GameServerMapper;
import com.tencent.wxcloudrun.dao.RewardIssueLogMapper;
import com.tencent.wxcloudrun.dao.UserInviteMapper;
import com.tencent.wxcloudrun.entity.User;
import com.tencent.wxcloudrun.exception.BusinessException;
import com.tencent.wxcloudrun.service.general.GeneralService;
import com.tencent.wxcloudrun.service.mail.MailService;
import com.tencent.wxcloudrun.util.JwtUtil;
import com.tencent.wxcloudrun.util.WechatUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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

    @Autowired
    private UserInviteMapper userInviteMapper;

    @Autowired
    private RewardIssueLogMapper rewardIssueLogMapper;

    @Autowired
    private GameServerMapper gameServerMapper;

    @Autowired
    private MailService mailService;

    private static final String SHARE_INVITE_BIZ_TYPE = "SHARE_INVITE_REGISTER";
    private static final int[] INVITE_REWARD_TIERS = {1, 5, 10, 20};
    private static final int[] INVITE_REWARD_GOLD = {200, 1000, 3000, 8000};
    
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
        Long existingUserId = userIdService.getUserId(openId);
        boolean isNewUser = existingUserId == null;
        Long userId = isNewUser ? userIdService.getOrCreateUserId(openId) : existingUserId;
        logger.info("用户ID: openId={}, userId={}", openId, userId);
        
        // 4. 用户数据初始化已移至 GameServerController.createRole()
        //    登录阶段只生成账号ID，不初始化区服相关数据（资源、武将等）
        
        // 5. 创建或更新用户信息
        User user = User.builder()
                .openId(openId)
                .unionId(wxSession.getUnionId())
                .nickName(request.getNickName())
                .avatarUrl(request.getAvatarUrl())
                .createTime(System.currentTimeMillis())
                .updateTime(System.currentTimeMillis())
                .build();
        
        logger.info("用户信息: {}", user);

        // 新用户首次注册时处理邀请奖励
        if (isNewUser) {
            handleInviteRewardOnRegister(userId, request.getInviterUserId());
        }
        
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

    public Map<String, Object> getInviteProgress(String requesterUserIdRaw) {
        Long inviterUserId = parsePositiveLong(requesterUserIdRaw);
        if (inviterUserId == null) {
            inviterUserId = parsePositiveLong(splitBaseUserId(requesterUserIdRaw));
        }
        if (inviterUserId == null) {
            throw new BusinessException("无效用户ID");
        }

        long inviteCount = userInviteMapper.countInviteesByInviter(inviterUserId);
        List<Map<String, Object>> tiers = new ArrayList<>();
        Integer nextTier = null;

        for (int i = 0; i < INVITE_REWARD_TIERS.length; i++) {
            int tier = INVITE_REWARD_TIERS[i];
            int rewardGold = INVITE_REWARD_GOLD[i];
            boolean reached = inviteCount >= tier;
            boolean issued = rewardIssueLogMapper.countByBiz(
                    SHARE_INVITE_BIZ_TYPE,
                    "tier_" + tier,
                    String.valueOf(inviterUserId)
            ) > 0;

            Map<String, Object> oneTier = new LinkedHashMap<>();
            oneTier.put("tier", tier);
            oneTier.put("rewardGold", rewardGold);
            oneTier.put("reached", reached);
            oneTier.put("issued", issued);
            oneTier.put("remaining", Math.max(0, tier - inviteCount));
            tiers.add(oneTier);

            if (nextTier == null && inviteCount < tier) {
                nextTier = tier;
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("inviteCount", inviteCount);
        result.put("tiers", tiers);
        result.put("nextTier", nextTier);
        result.put("nextRemaining", nextTier == null ? 0 : (nextTier - inviteCount));
        return result;
    }

    private void handleInviteRewardOnRegister(Long inviteeUserId, String inviterUserIdRaw) {
        Long inviterUserId = parsePositiveLong(inviterUserIdRaw);
        if (inviterUserId == null) return;
        if (inviterUserId.equals(inviteeUserId)) return; // 不能自邀
        if (!userIdService.isValidUserId(inviterUserId)) return;

        long now = System.currentTimeMillis();
        int inserted = userInviteMapper.insertIgnore(inviterUserId, inviteeUserId, now);
        if (inserted <= 0) {
            // 已有邀请关系（被邀请人只能记一次），直接返回
            return;
        }

        long inviteCount = userInviteMapper.countInviteesByInviter(inviterUserId);
        logger.info("邀请关系新增: inviter={}, invitee={}, count={}", inviterUserId, inviteeUserId, inviteCount);

        for (int i = 0; i < INVITE_REWARD_TIERS.length; i++) {
            int tier = INVITE_REWARD_TIERS[i];
            int rewardGold = INVITE_REWARD_GOLD[i];
            if (inviteCount < tier) continue;

            int issue = rewardIssueLogMapper.insertIgnore(
                    SHARE_INVITE_BIZ_TYPE,
                    "tier_" + tier,
                    String.valueOf(inviterUserId),
                    0,
                    "inviteCount=" + inviteCount,
                    now
            );
            if (issue > 0) {
                grantInviteReward(inviterUserId, rewardGold, tier, inviteCount);
            }
        }
    }

    private void grantInviteReward(Long inviterUserId, int rewardGold, int tier, long inviteCount) {
        String rawUserId = String.valueOf(inviterUserId);
        try {
            List<Map<String, Object>> playerServers = gameServerMapper.findPlayerServers(rawUserId);
            if (playerServers != null && !playerServers.isEmpty()) {
                int serverId = ((Number) playerServers.get(0).get("serverId")).intValue();
                String gameUserId = rawUserId + "_" + serverId;

                List<Map<String, Object>> atts = new ArrayList<>();
                Map<String, Object> goldAtt = new LinkedHashMap<>();
                goldAtt.put("itemType", "gold");
                goldAtt.put("itemName", "黄金");
                goldAtt.put("count", rewardGold);
                atts.add(goldAtt);

                mailService.sendSystemMail(
                        gameUserId,
                        "邀请奖励达成",
                        "恭喜主公！你已成功邀请" + inviteCount + "位好友注册，达成" + tier + "人档位，获得黄金" + rewardGold + "。",
                        atts
                );
                logger.info("邀请奖励通过邮件发放: inviter={}, gameUserId={}, tier={}, gold={}",
                        inviterUserId, gameUserId, tier, rewardGold);
                return;
            }

            // 兜底：若邀请人尚未创角，先发到基础账号资源
            userResourceService.addGold(rawUserId, rewardGold);
            logger.info("邀请奖励兜底发放到基础账号: inviter={}, tier={}, gold={}", inviterUserId, tier, rewardGold);
        } catch (Exception e) {
            logger.error("邀请奖励发放失败: inviter={}, tier={}, gold={}", inviterUserId, tier, rewardGold, e);
        }
    }

    private Long parsePositiveLong(String raw) {
        if (raw == null) return null;
        String s = raw.trim();
        if (s.isEmpty()) return null;
        try {
            long v = Long.parseLong(s);
            return v > 0 ? v : null;
        } catch (Exception ignore) {
            return null;
        }
    }

    private String splitBaseUserId(String raw) {
        if (raw == null) return null;
        int idx = raw.indexOf('_');
        if (idx <= 0) return raw;
        return raw.substring(0, idx);
    }
}


