package com.tencent.wxcloudrun.service.supply;

import com.tencent.wxcloudrun.dao.SupplyDataMapper;
import com.tencent.wxcloudrun.dao.SupplyGradeMapper;
import com.tencent.wxcloudrun.dao.SupplyRobberyMapper;
import com.tencent.wxcloudrun.dao.SupplyTransportMapper;
import com.tencent.wxcloudrun.exception.BusinessException;
import com.tencent.wxcloudrun.model.General;
import com.tencent.wxcloudrun.model.UserResource;
import com.tencent.wxcloudrun.repository.GeneralRepository;
import com.tencent.wxcloudrun.repository.UserResourceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class SupplyService {

    private static final Logger logger = LoggerFactory.getLogger(SupplyService.class);

    private static final int DAILY_TRANSPORT_LIMIT = 3;
    private static final int DAILY_ROBBERY_LIMIT = 5;
    private static final int MAX_ROBBED_PER_TRANSPORT = 3;
    private static final double ROBBERY_STEAL_RATIO = 0.15;
    private static final int GOLD_PER_MINUTE = 2;
    private static final int TOKEN_PRICE_GOLD = 10;

    @Autowired private SupplyGradeMapper gradeMapper;
    @Autowired private SupplyTransportMapper transportMapper;
    @Autowired private SupplyDataMapper dataMapper;
    @Autowired private SupplyRobberyMapper robberyMapper;
    @Autowired private UserResourceRepository userResourceRepository;
    @Autowired private GeneralRepository generalRepository;

    private List<Map<String, Object>> gradeConfigs = new ArrayList<>();

    @PostConstruct
    public void init() {
        gradeConfigs = gradeMapper.findAll();
        logger.info("加载了 {} 个军需等级配置", gradeConfigs.size());
    }

    private Map<String, Object> getGradeConfig(int gradeId) {
        for (Map<String, Object> g : gradeConfigs) {
            if (parseIntSafe(g.get("id"), 0) == gradeId) return g;
        }
        return gradeConfigs.isEmpty() ? null : gradeConfigs.get(0);
    }

    // ======================== 获取信息 ========================

    public Map<String, Object> getSupplyInfo(String userId) {
        Map<String, Object> sd = getOrInitData(userId);
        Map<String, Object> activeTransport = transportMapper.findActiveByUserId(userId);
        UserResource res = userResourceRepository.findByUserId(userId);
        int level = res != null && res.getLevel() != null ? res.getLevel() : 1;

        int currentGradeId = parseIntSafe(sd.get("currentGradeId"), 1);
        Map<String, Object> gradeConfig = getGradeConfig(currentGradeId);

        Map<String, Object> info = new HashMap<>();
        info.put("todayTransport", parseIntSafe(sd.get("todayTransport"), 0));
        info.put("todayRobbery", parseIntSafe(sd.get("todayRobbery"), 0));
        info.put("transportLimit", DAILY_TRANSPORT_LIMIT);
        info.put("robberyLimit", DAILY_ROBBERY_LIMIT);
        info.put("currentGradeId", currentGradeId);
        info.put("currentGrade", gradeConfig);
        info.put("refreshTokens", parseIntSafe(sd.get("refreshTokens"), 0));
        info.put("playerLevel", level);
        info.put("gold", res != null && res.getGold() != null ? res.getGold() : 0);

        if (activeTransport != null) {
            enrichTransport(activeTransport);
            info.put("activeTransport", activeTransport);
        }

        // 奖励预览
        if (gradeConfig != null) {
            info.put("rewardPreview", calcRewardPreview(gradeConfig, level));
        }

        return info;
    }

    // ======================== 刷新军需等级 ========================

    public Map<String, Object> rollGrade(String userId) {
        Map<String, Object> sd = getOrInitData(userId);
        double rand = ThreadLocalRandom.current().nextDouble();
        double cumulative = 0;
        int newGradeId = 1;

        for (Map<String, Object> g : gradeConfigs) {
            cumulative += parseDoubleSafe(g.get("refreshProbability"), 0);
            if (rand < cumulative) {
                newGradeId = parseIntSafe(g.get("id"), 1);
                break;
            }
        }

        saveData(userId, sd, newGradeId, null);
        Map<String, Object> gradeConfig = getGradeConfig(newGradeId);
        UserResource res = userResourceRepository.findByUserId(userId);
        int level = res != null && res.getLevel() != null ? res.getLevel() : 1;

        Map<String, Object> result = new HashMap<>();
        result.put("gradeId", newGradeId);
        result.put("grade", gradeConfig);
        result.put("rewardPreview", calcRewardPreview(gradeConfig, level));
        return result;
    }

    public Map<String, Object> refreshGrade(String userId) {
        Map<String, Object> sd = getOrInitData(userId);
        int tokens = parseIntSafe(sd.get("refreshTokens"), 0);
        if (tokens <= 0) {
            throw new BusinessException(400, "军需令不足");
        }

        int currentGradeId = parseIntSafe(sd.get("currentGradeId"), 1);
        double rand = ThreadLocalRandom.current().nextDouble();
        int newGradeId;
        String changeDesc;

        // 60%升一级, 10%升两级, 20%不变, 10%降一级
        if (rand < 0.60) {
            newGradeId = Math.min(currentGradeId + 1, 5);
            changeDesc = currentGradeId == 5 ? "已是顶级，保持不变" : "升一级";
        } else if (rand < 0.70) {
            newGradeId = Math.min(currentGradeId + 2, 5);
            changeDesc = "升两级";
        } else if (rand < 0.90) {
            newGradeId = currentGradeId;
            changeDesc = "保持不变";
        } else {
            newGradeId = Math.max(currentGradeId - 1, 1);
            changeDesc = currentGradeId == 1 ? "已是最低，保持不变" : "降一级";
        }

        saveData(userId, sd, newGradeId, tokens - 1);
        Map<String, Object> gradeConfig = getGradeConfig(newGradeId);
        UserResource res = userResourceRepository.findByUserId(userId);
        int level = res != null && res.getLevel() != null ? res.getLevel() : 1;

        Map<String, Object> result = new HashMap<>();
        result.put("previousGradeId", currentGradeId);
        result.put("gradeId", newGradeId);
        result.put("grade", gradeConfig);
        result.put("changeDesc", changeDesc);
        result.put("refreshTokens", tokens - 1);
        result.put("rewardPreview", calcRewardPreview(gradeConfig, level));
        return result;
    }

    // ======================== 开始运送 ========================

    public Map<String, Object> startTransport(String userId) {
        Map<String, Object> sd = getOrInitData(userId);
        int todayTransport = parseIntSafe(sd.get("todayTransport"), 0);
        if (todayTransport >= DAILY_TRANSPORT_LIMIT) {
            throw new BusinessException(400, "今日运送次数已用完(上限" + DAILY_TRANSPORT_LIMIT + "次)");
        }

        Map<String, Object> existing = transportMapper.findActiveByUserId(userId);
        if (existing != null) {
            throw new BusinessException(400, "当前有运送中的军需，请先完成");
        }

        UserResource res = userResourceRepository.findByUserId(userId);
        int level = res != null && res.getLevel() != null ? res.getLevel() : 1;

        int gradeId = parseIntSafe(sd.get("currentGradeId"), 1);
        Map<String, Object> gradeConfig = getGradeConfig(gradeId);
        if (gradeConfig == null) throw new BusinessException(500, "等级配置异常");

        Map<String, Long> rewards = calcRewards(gradeConfig, level);
        int minutes = parseIntSafe(gradeConfig.get("transportMinutes"), 30);
        long now = System.currentTimeMillis();
        long endTime = now + (long) minutes * 60 * 1000;

        transportMapper.insert(userId, gradeId,
                String.valueOf(gradeConfig.get("name")),
                rewards.get("silver"), rewards.get("paper"),
                rewards.get("food"), rewards.get("metal"),
                now, endTime, todayStr());

        // 更新今日运送次数，重新roll一个新军需
        double rand = ThreadLocalRandom.current().nextDouble();
        double cumulative = 0;
        int nextGradeId = 1;
        for (Map<String, Object> g : gradeConfigs) {
            cumulative += parseDoubleSafe(g.get("refreshProbability"), 0);
            if (rand < cumulative) {
                nextGradeId = parseIntSafe(g.get("id"), 1);
                break;
            }
        }
        saveData(userId, sd, nextGradeId, null);
        dataMapper.upsert(userId, todayTransport + 1,
                parseIntSafe(sd.get("todayRobbery"), 0),
                nextGradeId,
                parseIntSafe(sd.get("refreshTokens"), 0),
                todayStr(), System.currentTimeMillis());

        Map<String, Object> transport = transportMapper.findActiveByUserId(userId);
        enrichTransport(transport);

        Map<String, Object> result = new HashMap<>();
        result.put("transport", transport);
        result.put("todayTransport", todayTransport + 1);
        result.put("nextGradeId", nextGradeId);
        result.put("nextGrade", getGradeConfig(nextGradeId));
        logger.info("用户 {} 开始运送 {} 军需", userId, gradeConfig.get("name"));
        return result;
    }

    // ======================== 加速运送 ========================

    public Map<String, Object> speedUp(String userId, int gold) {
        Map<String, Object> transport = transportMapper.findActiveByUserId(userId);
        if (transport == null) throw new BusinessException(400, "没有正在运送的军需");

        long endTime = parseLongSafe(transport.get("endTime"), 0);
        long now = System.currentTimeMillis();
        if (now >= endTime) throw new BusinessException(400, "军需已运送完成，请直接收取");

        int minutes = gold / GOLD_PER_MINUTE;
        if (minutes <= 0) throw new BusinessException(400, "黄金不足，每" + GOLD_PER_MINUTE + "黄金加速1分钟");

        UserResource res = userResourceRepository.findByUserId(userId);
        int actualGold = gold;
        if (res == null || res.getGold() == null || res.getGold() < actualGold) {
            throw new BusinessException(400, "黄金不足");
        }

        long reduceMs = (long) minutes * 60 * 1000;
        long newEndTime = Math.max(now, endTime - reduceMs);
        int totalSpeedUp = parseIntSafe(transport.get("speedUpMinutes"), 0) + minutes;

        res.setGold(res.getGold() - actualGold);
        userResourceRepository.save(res);

        long transportId = parseLongSafe(transport.get("id"), 0);
        transportMapper.updateEndTime(transportId, newEndTime, totalSpeedUp);

        Map<String, Object> updated = transportMapper.findActiveByUserId(userId);
        enrichTransport(updated);

        Map<String, Object> result = new HashMap<>();
        result.put("transport", updated);
        result.put("goldCost", actualGold);
        result.put("minutesReduced", minutes);
        result.put("remainingGold", res.getGold());
        return result;
    }

    // ======================== 收取军需 ========================

    public Map<String, Object> collectTransport(String userId, long transportId) {
        Map<String, Object> transport = transportMapper.findById(transportId);
        if (transport == null) throw new BusinessException(400, "运送记录不存在");
        if (!userId.equals(String.valueOf(transport.get("userId")))) {
            throw new BusinessException(400, "无法收取他人的军需");
        }
        if ("collected".equals(transport.get("status"))) {
            throw new BusinessException(400, "军需已收取");
        }

        long endTime = parseLongSafe(transport.get("endTime"), 0);
        if (System.currentTimeMillis() < endTime) {
            throw new BusinessException(400, "军需尚未运送完成");
        }

        long finalSilver = parseLongSafe(transport.get("silverReward"), 0) - parseLongSafe(transport.get("robbedSilver"), 0);
        long finalPaper = parseLongSafe(transport.get("paperReward"), 0) - parseLongSafe(transport.get("robbedPaper"), 0);
        long finalFood = parseLongSafe(transport.get("foodReward"), 0) - parseLongSafe(transport.get("robbedFood"), 0);
        long finalMetal = parseLongSafe(transport.get("metalReward"), 0) - parseLongSafe(transport.get("robbedMetal"), 0);

        finalSilver = Math.max(0, finalSilver);
        finalPaper = Math.max(0, finalPaper);
        finalFood = Math.max(0, finalFood);
        finalMetal = Math.max(0, finalMetal);

        UserResource res = userResourceRepository.findByUserId(userId);
        if (res != null) {
            res.setSilver((res.getSilver() != null ? res.getSilver() : 0) + finalSilver);
            res.setPaper((res.getPaper() != null ? res.getPaper() : 0) + finalPaper);
            res.setFood((res.getFood() != null ? res.getFood() : 0) + finalFood);
            res.setMetal((res.getMetal() != null ? res.getMetal() : 0) + finalMetal);
            userResourceRepository.save(res);
        }

        transportMapper.updateStatus(transportId, "collected");

        Map<String, Object> result = new HashMap<>();
        result.put("silver", finalSilver);
        result.put("paper", finalPaper);
        result.put("food", finalFood);
        result.put("metal", finalMetal);
        result.put("gradeName", transport.get("gradeName"));
        result.put("robbedCount", parseIntSafe(transport.get("robbedCount"), 0));
        logger.info("用户 {} 收取军需: 银{}纸{}粮{}铁{}", userId, finalSilver, finalPaper, finalFood, finalMetal);
        return result;
    }

    // ======================== 运送地图 ========================

    public Map<String, Object> getMapTransports(String userId) {
        List<Map<String, Object>> allActive = transportMapper.findAllActive();
        long now = System.currentTimeMillis();

        List<Map<String, Object>> transports = new ArrayList<>();
        for (Map<String, Object> t : allActive) {
            enrichTransport(t);
            String tUserId = String.valueOf(t.get("userId"));
            t.put("isOwn", userId.equals(tUserId));
            t.put("playerName", "玩家" + tUserId.substring(0, Math.min(6, tUserId.length())));
            transports.add(t);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("transports", transports);
        result.put("total", transports.size());
        return result;
    }

    // ======================== 抢夺军需 ========================

    public Map<String, Object> robTransport(String userId, long transportId, String generalId) {
        Map<String, Object> sd = getOrInitData(userId);
        int todayRobbery = parseIntSafe(sd.get("todayRobbery"), 0);
        if (todayRobbery >= DAILY_ROBBERY_LIMIT) {
            throw new BusinessException(400, "今日抢夺次数已用完(上限" + DAILY_ROBBERY_LIMIT + "次)");
        }

        Map<String, Object> transport = transportMapper.findById(transportId);
        if (transport == null) throw new BusinessException(400, "运送不存在");
        if ("collected".equals(transport.get("status"))) throw new BusinessException(400, "军需已被收取");

        String defenderId = String.valueOf(transport.get("userId"));
        if (userId.equals(defenderId)) throw new BusinessException(400, "不能抢夺自己的军需");

        long endTime = parseLongSafe(transport.get("endTime"), 0);
        if (System.currentTimeMillis() >= endTime) throw new BusinessException(400, "该军需已运送完成，无法抢夺");

        int robbedCount = parseIntSafe(transport.get("robbedCount"), 0);
        if (robbedCount >= MAX_ROBBED_PER_TRANSPORT) {
            throw new BusinessException(400, "该军需已被抢夺" + MAX_ROBBED_PER_TRANSPORT + "次，无法再次抢夺");
        }

        General general = generalRepository.findById(generalId);
        if (general == null) throw new BusinessException(400, "武将不存在");

        UserResource myRes = userResourceRepository.findByUserId(userId);
        int myLevel = myRes != null && myRes.getLevel() != null ? myRes.getLevel() : 1;
        int playerPower = general.getAttrValor() != null ? general.getAttrValor() : myLevel * 500;

        List<General> defGenerals = generalRepository.findByUserId(defenderId);
        int defPower = defGenerals.stream()
                .mapToInt(g -> g.getAttrValor() != null ? g.getAttrValor() : 0)
                .max().orElse(myLevel * 400);

        double powerRatio = (double) playerPower / (playerPower + defPower);
        double winRate = Math.min(Math.max(powerRatio * 1.2, 0.1), 0.95);
        boolean victory = ThreadLocalRandom.current().nextDouble() < winRate;

        long silverStolen = 0, paperStolen = 0, foodStolen = 0, metalStolen = 0;

        if (victory) {
            silverStolen = (long) (parseLongSafe(transport.get("silverReward"), 0) * ROBBERY_STEAL_RATIO);
            paperStolen = (long) (parseLongSafe(transport.get("paperReward"), 0) * ROBBERY_STEAL_RATIO);
            foodStolen = (long) (parseLongSafe(transport.get("foodReward"), 0) * ROBBERY_STEAL_RATIO);
            metalStolen = (long) (parseLongSafe(transport.get("metalReward"), 0) * ROBBERY_STEAL_RATIO);

            transportMapper.updateRobbed(transportId,
                    robbedCount + 1,
                    parseLongSafe(transport.get("robbedSilver"), 0) + silverStolen,
                    parseLongSafe(transport.get("robbedPaper"), 0) + paperStolen,
                    parseLongSafe(transport.get("robbedFood"), 0) + foodStolen,
                    parseLongSafe(transport.get("robbedMetal"), 0) + metalStolen);

            if (myRes != null) {
                myRes.setSilver((myRes.getSilver() != null ? myRes.getSilver() : 0) + silverStolen);
                myRes.setPaper((myRes.getPaper() != null ? myRes.getPaper() : 0) + paperStolen);
                myRes.setFood((myRes.getFood() != null ? myRes.getFood() : 0) + foodStolen);
                myRes.setMetal((myRes.getMetal() != null ? myRes.getMetal() : 0) + metalStolen);
                userResourceRepository.save(myRes);
            }
        }

        // 更新今日抢夺次数
        dataMapper.upsert(userId, parseIntSafe(sd.get("todayTransport"), 0),
                todayRobbery + 1,
                parseIntSafe(sd.get("currentGradeId"), 1),
                parseIntSafe(sd.get("refreshTokens"), 0),
                todayStr(), System.currentTimeMillis());

        String defName = "玩家" + defenderId.substring(0, Math.min(6, defenderId.length()));
        robberyMapper.insert(userId, "我", defenderId, defName,
                transportId, String.valueOf(transport.get("gradeName")),
                victory, silverStolen, paperStolen, foodStolen, metalStolen,
                System.currentTimeMillis(), todayStr());

        Map<String, Object> result = new HashMap<>();
        result.put("victory", victory);
        result.put("defenderName", defName);
        result.put("gradeName", transport.get("gradeName"));
        result.put("silverStolen", silverStolen);
        result.put("paperStolen", paperStolen);
        result.put("foodStolen", foodStolen);
        result.put("metalStolen", metalStolen);
        result.put("todayRobbery", todayRobbery + 1);
        result.put("playerPower", playerPower);
        result.put("defPower", defPower);
        logger.info("用户 {} {}抢夺 {} 的军需", userId, victory ? "成功" : "失败", defName);
        return result;
    }

    // ======================== 购买军需令 ========================

    public Map<String, Object> buyToken(String userId, int count) {
        if (count <= 0 || count > 10) count = 1;
        int totalCost = count * TOKEN_PRICE_GOLD;

        UserResource res = userResourceRepository.findByUserId(userId);
        if (res == null || res.getGold() == null || res.getGold() < totalCost) {
            throw new BusinessException(400, "黄金不足，需要" + totalCost + "黄金");
        }

        res.setGold(res.getGold() - totalCost);
        userResourceRepository.save(res);

        Map<String, Object> sd = getOrInitData(userId);
        int newTokens = parseIntSafe(sd.get("refreshTokens"), 0) + count;
        saveData(userId, sd, null, newTokens);

        Map<String, Object> result = new HashMap<>();
        result.put("cost", totalCost);
        result.put("count", count);
        result.put("refreshTokens", newTokens);
        result.put("remainingGold", res.getGold());
        return result;
    }

    // ======================== 抢夺记录 ========================

    public Map<String, Object> getRecords(String userId, String type) {
        List<Map<String, Object>> records;
        if ("defense".equals(type)) {
            records = robberyMapper.findByDefender(userId, 50);
        } else {
            records = robberyMapper.findByAttacker(userId, 50);
        }
        for (Map<String, Object> r : records) {
            Object v = r.get("victory");
            r.put("victory", v != null && ("1".equals(String.valueOf(v))
                    || Boolean.TRUE.equals(v) || "true".equalsIgnoreCase(String.valueOf(v))));
        }

        Map<String, Object> result = new HashMap<>();
        result.put("records", records);
        return result;
    }

    // ======================== 辅助方法 ========================

    private void enrichTransport(Map<String, Object> t) {
        if (t == null) return;
        long startTime = parseLongSafe(t.get("startTime"), 0);
        long endTime = parseLongSafe(t.get("endTime"), 0);
        long now = System.currentTimeMillis();
        long totalMs = endTime - startTime;
        long elapsedMs = now - startTime;
        double progress = totalMs > 0 ? Math.min(1.0, (double) elapsedMs / totalMs) : 1.0;
        t.put("progress", Math.round(progress * 100) / 100.0);
        t.put("completed", now >= endTime);
        t.put("remainingMs", Math.max(0, endTime - now));
    }

    private Map<String, Long> calcRewards(Map<String, Object> gradeConfig, int level) {
        ThreadLocalRandom rng = ThreadLocalRandom.current();
        long silverBase = parseLongSafe(gradeConfig.get("silverBase"), 10000);
        int silverMult = parseIntSafe(gradeConfig.get("silverLevelMult"), 100);
        int silverRand = parseIntSafe(gradeConfig.get("silverRandMax"), 1000);
        long otherBase = parseLongSafe(gradeConfig.get("otherBase"), 3000);
        int otherMult = parseIntSafe(gradeConfig.get("otherLevelMult"), 100);
        int otherRand = parseIntSafe(gradeConfig.get("otherRandMax"), 1000);

        Map<String, Long> rewards = new HashMap<>();
        rewards.put("silver", silverBase + (long) level * silverMult + rng.nextLong(1, silverRand + 1));
        rewards.put("paper", otherBase + (long) level * otherMult + rng.nextLong(1, otherRand + 1));
        rewards.put("food", otherBase + (long) level * otherMult + rng.nextLong(1, otherRand + 1));
        rewards.put("metal", otherBase + (long) level * otherMult + rng.nextLong(1, otherRand + 1));
        return rewards;
    }

    private Map<String, String> calcRewardPreview(Map<String, Object> gradeConfig, int level) {
        long silverBase = parseLongSafe(gradeConfig.get("silverBase"), 10000);
        int silverMult = parseIntSafe(gradeConfig.get("silverLevelMult"), 100);
        long otherBase = parseLongSafe(gradeConfig.get("otherBase"), 3000);
        int otherMult = parseIntSafe(gradeConfig.get("otherLevelMult"), 100);

        Map<String, String> preview = new HashMap<>();
        preview.put("silver", (silverBase + (long) level * silverMult) + "+");
        preview.put("paper", (otherBase + (long) level * otherMult) + "+");
        preview.put("food", (otherBase + (long) level * otherMult) + "+");
        preview.put("metal", (otherBase + (long) level * otherMult) + "+");
        preview.put("transportMinutes", String.valueOf(parseIntSafe(gradeConfig.get("transportMinutes"), 30)));
        return preview;
    }

    private Map<String, Object> getOrInitData(String userId) {
        Map<String, Object> sd = dataMapper.findByUserId(userId);
        if (sd == null) {
            dataMapper.upsert(userId, 0, 0, 1, 0, todayStr(), System.currentTimeMillis());
            sd = dataMapper.findByUserId(userId);
        }
        String lastReset = sd != null ? String.valueOf(sd.get("lastResetDate")) : "";
        if (!todayStr().equals(lastReset)) {
            dataMapper.upsert(userId, 0, 0,
                    parseIntSafe(sd.get("currentGradeId"), 1),
                    parseIntSafe(sd.get("refreshTokens"), 0),
                    todayStr(), System.currentTimeMillis());
            sd = dataMapper.findByUserId(userId);
        }
        return sd;
    }

    private void saveData(String userId, Map<String, Object> sd, Integer newGradeId, Integer newTokens) {
        int gradeId = newGradeId != null ? newGradeId : parseIntSafe(sd.get("currentGradeId"), 1);
        int tokens = newTokens != null ? newTokens : parseIntSafe(sd.get("refreshTokens"), 0);
        dataMapper.upsert(userId,
                parseIntSafe(sd.get("todayTransport"), 0),
                parseIntSafe(sd.get("todayRobbery"), 0),
                gradeId, tokens, todayStr(), System.currentTimeMillis());
    }

    private String todayStr() {
        return new SimpleDateFormat("yyyyMMdd").format(new Date());
    }

    private int parseIntSafe(Object val, int def) {
        if (val == null) return def;
        try { return Integer.parseInt(String.valueOf(val)); } catch (Exception e) { return def; }
    }

    private long parseLongSafe(Object val, long def) {
        if (val == null) return def;
        try { return Long.parseLong(String.valueOf(val)); } catch (Exception e) { return def; }
    }

    private double parseDoubleSafe(Object val, double def) {
        if (val == null) return def;
        try { return Double.parseDouble(String.valueOf(val)); } catch (Exception e) { return def; }
    }
}
