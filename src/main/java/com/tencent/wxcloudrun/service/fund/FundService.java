package com.tencent.wxcloudrun.service.fund;

import com.tencent.wxcloudrun.dao.FundMapper;
import com.tencent.wxcloudrun.exception.BusinessException;
import com.tencent.wxcloudrun.service.UserResourceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class FundService {

    private static final Logger logger = LoggerFactory.getLogger(FundService.class);

    private static final long FUND_PRICE = 298;

    private static final Map<Integer, Map<String, Object>> REWARD_TIERS = new LinkedHashMap<>();

    static {
        REWARD_TIERS.put(10, buildReward("gold", "黄金", 500, 10));
        REWARD_TIERS.put(20, buildReward("gold", "黄金", 1000, 20));
        REWARD_TIERS.put(30, buildReward("gold", "黄金", 2000, 30));
        REWARD_TIERS.put(40, buildReward("diamond", "钻石", 200, 40));
        REWARD_TIERS.put(50, buildReward("diamond", "钻石", 500, 50));
    }

    private static Map<String, Object> buildReward(String type, String name, int amount, int level) {
        Map<String, Object> r = new HashMap<>();
        r.put("type", type);
        r.put("name", name);
        r.put("amount", amount);
        r.put("requiredLevel", level);
        return r;
    }

    @Autowired
    private FundMapper fundMapper;

    @Autowired
    private UserResourceService userResourceService;

    public Map<String, Object> getInfo(String userId) {
        fundMapper.insertFund(userId);

        Map<String, Object> fundRecord = fundMapper.findByUserId(userId);
        boolean purchased = fundRecord != null
                && fundRecord.get("purchased") != null
                && ((Number) fundRecord.get("purchased")).intValue() == 1;

        List<Integer> claimedLevels = fundMapper.findClaimedLevels(userId);
        Set<Integer> claimedSet = new HashSet<>(claimedLevels);

        List<Map<String, Object>> rewardList = new ArrayList<>();
        for (Map.Entry<Integer, Map<String, Object>> entry : REWARD_TIERS.entrySet()) {
            int level = entry.getKey();
            Map<String, Object> reward = new HashMap<>(entry.getValue());
            reward.put("level", level);
            reward.put("claimed", claimedSet.contains(level));
            rewardList.add(reward);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("purchased", purchased);
        result.put("price", FUND_PRICE);
        result.put("rewards", rewardList);
        result.put("claimedLevels", claimedLevels);
        return result;
    }

    @Transactional
    public Map<String, Object> purchase(String userId) {
        fundMapper.insertFund(userId);

        Map<String, Object> fundRecord = fundMapper.findByUserId(userId);
        if (fundRecord != null
                && fundRecord.get("purchased") != null
                && ((Number) fundRecord.get("purchased")).intValue() == 1) {
            throw new BusinessException(400, "已购买成长基金，无需重复购买");
        }

        boolean ok = userResourceService.consumeGold(userId, FUND_PRICE);
        if (!ok) {
            throw new BusinessException(400, "黄金不足，成长基金需要" + FUND_PRICE + "黄金");
        }

        fundMapper.updatePurchased(userId);
        logger.info("用户 {} 购买成长基金，花费 {} 黄金", userId, FUND_PRICE);

        Map<String, Object> result = new HashMap<>();
        result.put("purchased", true);
        result.put("cost", FUND_PRICE);
        result.put("message", "成长基金购买成功!");
        return result;
    }

    @Transactional
    public Map<String, Object> claim(String userId, int rewardLevel) {
        if (!REWARD_TIERS.containsKey(rewardLevel)) {
            throw new BusinessException(400, "无效的奖励等级: " + rewardLevel);
        }

        Map<String, Object> fundRecord = fundMapper.findByUserId(userId);
        if (fundRecord == null
                || fundRecord.get("purchased") == null
                || ((Number) fundRecord.get("purchased")).intValue() != 1) {
            throw new BusinessException(400, "请先购买成长基金");
        }

        List<Integer> claimedLevels = fundMapper.findClaimedLevels(userId);
        if (claimedLevels.contains(rewardLevel)) {
            throw new BusinessException(400, "该等级奖励已领取");
        }

        Map<String, Object> reward = REWARD_TIERS.get(rewardLevel);
        String rewardType = (String) reward.get("type");
        int rewardAmount = (int) reward.get("amount");

        switch (rewardType) {
            case "gold":
                userResourceService.addGold(userId, rewardAmount);
                break;
            case "diamond":
                userResourceService.addDiamond(userId, rewardAmount);
                break;
            case "silver":
                userResourceService.addSilver(userId, rewardAmount);
                break;
            default:
                logger.warn("未知奖励类型: {}", rewardType);
        }

        fundMapper.insertClaim(userId, rewardLevel);
        logger.info("用户 {} 领取成长基金Lv.{}奖励: {} x{}", userId, rewardLevel, rewardType, rewardAmount);

        Map<String, Object> result = new HashMap<>();
        result.put("level", rewardLevel);
        result.put("reward", reward);
        result.put("message", "领取成功!");
        result.put("userResource", userResourceService.getUserResource(userId));
        return result;
    }
}
