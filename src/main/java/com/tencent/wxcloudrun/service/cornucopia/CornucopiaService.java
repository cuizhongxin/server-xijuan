package com.tencent.wxcloudrun.service.cornucopia;

import com.tencent.wxcloudrun.dao.CornucopiaMapper;
import com.tencent.wxcloudrun.exception.BusinessException;
import com.tencent.wxcloudrun.service.UserResourceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class CornucopiaService {

    private static final Logger logger = LoggerFactory.getLogger(CornucopiaService.class);

    private static final int DRAW_COST = 50;
    private static final int DRAW_10_COUNT = 10;
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private static final List<Map<String, Object>> REWARD_POOL = new ArrayList<>();

    static {
        REWARD_POOL.add(rewardDef("gold", "黄金", 20, 30));
        REWARD_POOL.add(rewardDef("gold", "黄金", 50, 25));
        REWARD_POOL.add(rewardDef("gold", "黄金", 100, 15));
        REWARD_POOL.add(rewardDef("gold", "黄金", 200, 8));
        REWARD_POOL.add(rewardDef("gold", "黄金", 500, 5));
        REWARD_POOL.add(rewardDef("silver", "白银", 100, 20));
        REWARD_POOL.add(rewardDef("silver", "白银", 300, 15));
        REWARD_POOL.add(rewardDef("silver", "白银", 500, 10));
        REWARD_POOL.add(rewardDef("food", "粮食", 200, 15));
        REWARD_POOL.add(rewardDef("food", "粮食", 500, 10));
        REWARD_POOL.add(rewardDef("wood", "木材", 200, 15));
        REWARD_POOL.add(rewardDef("wood", "木材", 500, 10));
        REWARD_POOL.add(rewardDef("diamond", "钻石", 10, 10));
        REWARD_POOL.add(rewardDef("diamond", "钻石", 30, 5));
        REWARD_POOL.add(rewardDef("diamond", "钻石", 50, 2));
    }

    private static Map<String, Object> rewardDef(String type, String name, int amount, int weight) {
        Map<String, Object> r = new HashMap<>();
        r.put("type", type);
        r.put("name", name);
        r.put("amount", amount);
        r.put("weight", weight);
        return r;
    }

    @Autowired
    private CornucopiaMapper cornucopiaMapper;

    @Autowired
    private UserResourceService userResourceService;

    public Map<String, Object> getInfo(String userId) {
        String today = LocalDate.now().format(DATE_FMT);
        Map<String, Object> record = cornucopiaMapper.findByUserAndDate(userId, today);

        int drawCount = 0;
        boolean freeUsed = false;
        if (record != null) {
            drawCount = record.get("drawCount") != null ? ((Number) record.get("drawCount")).intValue() : 0;
            freeUsed = record.get("freeUsed") != null && ((Number) record.get("freeUsed")).intValue() == 1;
        }

        Map<String, Object> result = new HashMap<>();
        result.put("todayDrawCount", drawCount);
        result.put("freeAvailable", !freeUsed);
        result.put("costPerDraw", DRAW_COST);
        result.put("rewardPool", REWARD_POOL);
        return result;
    }

    @Transactional
    public Map<String, Object> draw(String userId, int count) {
        if (count != 1 && count != DRAW_10_COUNT) {
            throw new BusinessException(400, "抽奖次数只能为1次或10次");
        }

        String today = LocalDate.now().format(DATE_FMT);
        Map<String, Object> record = cornucopiaMapper.findByUserAndDate(userId, today);

        int currentDrawCount = 0;
        boolean freeUsed = false;
        if (record != null) {
            currentDrawCount = record.get("drawCount") != null ? ((Number) record.get("drawCount")).intValue() : 0;
            freeUsed = record.get("freeUsed") != null && ((Number) record.get("freeUsed")).intValue() == 1;
        }

        int freeDraws = 0;
        if (!freeUsed) {
            freeDraws = 1;
        }

        int paidDraws = count - freeDraws;
        if (paidDraws < 0) paidDraws = 0;

        long totalCost = (long) paidDraws * DRAW_COST;
        if (totalCost > 0) {
            boolean success = userResourceService.consumeGold(userId, totalCost);
            if (!success) {
                throw new BusinessException(400, "黄金不足，需要" + totalCost + "黄金");
            }
        }

        List<Map<String, Object>> rewards = new ArrayList<>();
        Random random = new Random();
        int totalWeight = REWARD_POOL.stream().mapToInt(r -> (int) r.get("weight")).sum();

        for (int i = 0; i < count; i++) {
            Map<String, Object> reward = rollReward(random, totalWeight);
            rewards.add(reward);
            grantReward(userId, (String) reward.get("type"), (int) reward.get("amount"));
        }

        int newDrawCount = currentDrawCount + count;
        boolean newFreeUsed = true;
        cornucopiaMapper.upsertRecord(userId, today, newDrawCount, newFreeUsed ? 1 : 0);

        logger.info("用户 {} 聚宝盆抽奖 {}次, 花费 {} 黄金", userId, count, totalCost);

        Map<String, Object> result = new HashMap<>();
        result.put("rewards", rewards);
        result.put("totalCost", totalCost);
        result.put("todayDrawCount", newDrawCount);
        result.put("freeAvailable", false);
        result.put("userResource", userResourceService.getUserResource(userId));
        return result;
    }

    private Map<String, Object> rollReward(Random random, int totalWeight) {
        int roll = random.nextInt(totalWeight);
        int cumulative = 0;
        for (Map<String, Object> reward : REWARD_POOL) {
            cumulative += (int) reward.get("weight");
            if (roll < cumulative) {
                Map<String, Object> result = new HashMap<>(reward);
                result.remove("weight");
                return result;
            }
        }
        Map<String, Object> fallback = new HashMap<>(REWARD_POOL.get(0));
        fallback.remove("weight");
        return fallback;
    }

    private void grantReward(String userId, String type, int amount) {
        switch (type) {
            case "gold":
                userResourceService.addGold(userId, amount);
                break;
            case "silver":
                userResourceService.addSilver(userId, amount);
                break;
            case "food":
                userResourceService.addFood(userId, amount);
                break;
            case "wood":
                userResourceService.addWood(userId, amount);
                break;
            case "diamond":
                userResourceService.addDiamond(userId, amount);
                break;
            default:
                logger.warn("未知奖励类型: {}", type);
        }
    }
}
