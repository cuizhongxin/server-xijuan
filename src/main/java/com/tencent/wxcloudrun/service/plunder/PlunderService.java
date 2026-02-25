package com.tencent.wxcloudrun.service.plunder;

import com.tencent.wxcloudrun.config.PlunderConfig;
import com.tencent.wxcloudrun.config.PlunderConfig.PlunderNpc;
import com.tencent.wxcloudrun.dao.PlunderRecordMapper;
import com.tencent.wxcloudrun.exception.BusinessException;
import com.tencent.wxcloudrun.model.General;
import com.tencent.wxcloudrun.model.PlunderData;
import com.tencent.wxcloudrun.model.UserResource;
import com.tencent.wxcloudrun.repository.GeneralRepository;
import com.tencent.wxcloudrun.repository.PlunderRepository;
import com.tencent.wxcloudrun.repository.UserResourceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Service
public class PlunderService {

    private static final Logger logger = LoggerFactory.getLogger(PlunderService.class);

    @Autowired
    private PlunderConfig plunderConfig;

    @Autowired
    private PlunderRepository plunderRepository;

    @Autowired
    private PlunderRecordMapper plunderRecordMapper;

    @Autowired
    private UserResourceRepository userResourceRepository;

    @Autowired
    private GeneralRepository generalRepository;

    /**
     * 获取掠夺主页数据
     */
    public Map<String, Object> getPlunderInfo(String userId) {
        PlunderData pd = plunderRepository.getOrInit(userId);
        UserResource resource = userResourceRepository.findByUserId(userId);
        int level = resource != null && resource.getLevel() != null ? resource.getLevel() : 1;

        Map<String, Object> info = new HashMap<>();
        info.put("todayCount", pd.getTodayCount());
        info.put("todayPurchased", pd.getTodayPurchased());
        info.put("dailyLimit", PlunderConfig.DAILY_PLUNDER_LIMIT);
        info.put("availableCount", pd.getAvailableCount());
        info.put("maxPurchase", PlunderConfig.MAX_PURCHASE_TIMES);
        info.put("nextPurchaseCost", PlunderConfig.getPurchaseCost(pd.getTodayPurchased()));
        info.put("playerLevel", level);
        return info;
    }

    /**
     * 获取掠夺目标列表（优先玩家，不足NPC填充）
     */
    public Map<String, Object> getTargetList(String userId, int page) {
        UserResource myResource = userResourceRepository.findByUserId(userId);
        int myLevel = myResource != null && myResource.getLevel() != null ? myResource.getLevel() : 1;

        // 从数据库查询冷却中的玩家ID
        long cooldownSince = System.currentTimeMillis() - PlunderConfig.PLUNDER_COOLDOWN_MS;
        List<Map<String, Object>> recentAttacks = plunderRecordMapper.findByAttacker(userId, 200);
        Set<String> cooldownIds = new HashSet<>();
        for (Map<String, Object> r : recentAttacks) {
            Object isNpcObj = r.get("isNpc");
            boolean isNpc = isNpcObj != null && (
                    Boolean.TRUE.equals(isNpcObj) || "1".equals(String.valueOf(isNpcObj))
                    || "true".equalsIgnoreCase(String.valueOf(isNpcObj)));
            if (isNpc) continue;

            Object tsObj = r.get("timestamp");
            long ts = parseLongSafe(tsObj, 0);
            if (ts >= cooldownSince) {
                cooldownIds.add(String.valueOf(r.get("defenderId")));
            }
        }

        // 查找等级范围内的玩家
        List<Map<String, Object>> allUsers = plunderRepository.findAllUserLevels();
        List<Map<String, Object>> matchedPlayers = new ArrayList<>();

        for (Map<String, Object> u : allUsers) {
            String uid = String.valueOf(u.get("userId"));
            if (uid.equals(userId)) continue;

            int uLevel = parseIntSafe(u.get("level"), 1);
            if (Math.abs(uLevel - myLevel) > PlunderConfig.LEVEL_RANGE) continue;

            boolean onCooldown = cooldownIds.contains(uid);

            Map<String, Object> target = new HashMap<>();
            target.put("id", uid);
            target.put("name", "玩家" + uid.substring(0, Math.min(6, uid.length())));
            target.put("level", uLevel);
            target.put("isNpc", false);
            target.put("silver", parseLongSafe(u.get("silver"), 0));
            target.put("wood", parseLongSafe(u.get("wood"), 0));
            target.put("paper", parseLongSafe(u.get("paper"), 0));
            target.put("food", parseLongSafe(u.get("food"), 0));
            target.put("rank", stripQuotes(String.valueOf(u.get("rank"))));
            target.put("onCooldown", onCooldown);
            target.put("power", uLevel * 500 + 1000);
            matchedPlayers.add(target);
        }

        matchedPlayers.sort((a, b) -> ((Integer) b.get("level")).compareTo((Integer) a.get("level")));

        // 从数据库模板生成NPC列表
        List<PlunderNpc> npcs = plunderConfig.generateNpcsForLevel(myLevel);
        List<Map<String, Object>> npcTargets = npcs.stream().map(npc -> {
            Map<String, Object> target = new HashMap<>();
            target.put("id", npc.getId());
            target.put("name", npc.getName());
            target.put("level", npc.getLevel());
            target.put("isNpc", true);
            target.put("faction", npc.getFaction());
            target.put("silver", npc.getSilver());
            target.put("wood", npc.getWood());
            target.put("paper", npc.getPaper());
            target.put("food", npc.getFood());
            target.put("power", npc.getPower());
            target.put("onCooldown", false);
            return target;
        }).collect(Collectors.toList());

        List<Map<String, Object>> allTargets = new ArrayList<>(matchedPlayers);
        allTargets.addAll(npcTargets);

        int total = allTargets.size();
        int start = page * PlunderConfig.PAGE_SIZE;
        int end = Math.min(start + PlunderConfig.PAGE_SIZE, total);
        List<Map<String, Object>> pageTargets = start < total ? allTargets.subList(start, end) : new ArrayList<>();

        Map<String, Object> result = new HashMap<>();
        result.put("targets", pageTargets);
        result.put("total", total);
        result.put("page", page);
        result.put("pageSize", PlunderConfig.PAGE_SIZE);
        result.put("playerLevel", myLevel);
        return result;
    }

    /**
     * 执行掠夺
     */
    public Map<String, Object> doPlunder(String userId, String targetId, String generalId) {
        PlunderData pd = plunderRepository.getOrInit(userId);

        if (pd.getAvailableCount() <= 0) {
            throw new BusinessException(400, "今日掠夺次数已用完，可购买额外次数");
        }

        UserResource myResource = userResourceRepository.findByUserId(userId);
        int myLevel = myResource != null && myResource.getLevel() != null ? myResource.getLevel() : 1;

        General general = generalRepository.findById(generalId);
        if (general == null) {
            throw new BusinessException(400, "武将不存在");
        }
        int playerPower = general.getAttributes() != null ? general.getAttributes().getPower() : myLevel * 500;

        // 冷却检查（非NPC目标）- 直接查数据库
        boolean isNpcTarget = targetId.startsWith("npc_");
        if (!isNpcTarget) {
            long cooldownSince = System.currentTimeMillis() - PlunderConfig.PLUNDER_COOLDOWN_MS;
            int recentCount = plunderRecordMapper.countRecentAttack(userId, targetId, cooldownSince);
            if (recentCount > 0) {
                throw new BusinessException(400, "该玩家1小时内已被掠夺，请选择其他目标");
            }
        }

        // 获取目标数据
        String targetName;
        int targetLevel;
        int targetPower;
        long tSilver, tWood, tPaper, tFood;
        String faction = null;

        if (isNpcTarget) {
            List<PlunderNpc> npcs = plunderConfig.generateNpcsForLevel(myLevel);
            PlunderNpc npc = npcs.stream().filter(n -> n.getId().equals(targetId)).findFirst().orElse(null);
            if (npc == null) throw new BusinessException(400, "NPC不存在");

            targetName = npc.getName();
            targetLevel = npc.getLevel();
            targetPower = npc.getPower();
            tSilver = npc.getSilver();
            tWood = npc.getWood();
            tPaper = npc.getPaper();
            tFood = npc.getFood();
            faction = npc.getFaction();
        } else {
            UserResource targetResource = userResourceRepository.findByUserId(targetId);
            if (targetResource == null) throw new BusinessException(400, "玩家不存在");

            targetName = "玩家" + targetId.substring(0, Math.min(6, targetId.length()));
            targetLevel = targetResource.getLevel() != null ? targetResource.getLevel() : 1;
            tSilver = targetResource.getSilver() != null ? targetResource.getSilver() : 0;
            tWood = targetResource.getWood() != null ? targetResource.getWood() : 0;
            tPaper = targetResource.getPaper() != null ? targetResource.getPaper() : 0;
            tFood = targetResource.getFood() != null ? targetResource.getFood() : 0;

            List<General> targetGenerals = generalRepository.findByUserId(targetId);
            targetPower = targetGenerals.stream()
                    .mapToInt(g -> g.getAttributes() != null ? g.getAttributes().getPower() : 0)
                    .max().orElse(targetLevel * 400);
        }

        // 战斗计算
        double powerRatio = (double) playerPower / (playerPower + targetPower);
        double winRate = Math.min(Math.max(powerRatio * 1.2, 0.1), 0.95);
        boolean victory = ThreadLocalRandom.current().nextDouble() < winRate;

        long silverGain = 0, woodGain = 0, paperGain = 0, foodGain = 0;

        if (victory) {
            silverGain = (long) (myLevel * PlunderConfig.REWARD_BASE_MULTIPLIER + tSilver * PlunderConfig.REWARD_RESOURCE_RATIO);
            woodGain = (long) (myLevel * PlunderConfig.REWARD_BASE_MULTIPLIER + tWood * PlunderConfig.REWARD_RESOURCE_RATIO);
            paperGain = (long) (myLevel * PlunderConfig.REWARD_BASE_MULTIPLIER + tPaper * PlunderConfig.REWARD_RESOURCE_RATIO);
            foodGain = (long) (myLevel * PlunderConfig.REWARD_BASE_MULTIPLIER + tFood * PlunderConfig.REWARD_RESOURCE_RATIO);

            myResource.setSilver((myResource.getSilver() != null ? myResource.getSilver() : 0) + silverGain);
            myResource.setWood((myResource.getWood() != null ? myResource.getWood() : 0) + woodGain);
            myResource.setPaper((myResource.getPaper() != null ? myResource.getPaper() : 0) + paperGain);
            myResource.setFood((myResource.getFood() != null ? myResource.getFood() : 0) + foodGain);
            userResourceRepository.save(myResource);

            if (!isNpcTarget) {
                UserResource targetResource = userResourceRepository.findByUserId(targetId);
                if (targetResource != null) {
                    long sLoss = (long) (tSilver * PlunderConfig.VICTIM_LOSS_RATIO);
                    long wLoss = (long) (tWood * PlunderConfig.VICTIM_LOSS_RATIO);
                    long pLoss = (long) (tPaper * PlunderConfig.VICTIM_LOSS_RATIO);
                    long fLoss = (long) (tFood * PlunderConfig.VICTIM_LOSS_RATIO);

                    targetResource.setSilver(Math.max(0, (targetResource.getSilver() != null ? targetResource.getSilver() : 0) - sLoss));
                    targetResource.setWood(Math.max(0, (targetResource.getWood() != null ? targetResource.getWood() : 0) - wLoss));
                    targetResource.setPaper(Math.max(0, (targetResource.getPaper() != null ? targetResource.getPaper() : 0) - pLoss));
                    targetResource.setFood(Math.max(0, (targetResource.getFood() != null ? targetResource.getFood() : 0) - fLoss));
                    userResourceRepository.save(targetResource);
                }
            }
        }

        // 更新今日次数
        pd.setTodayCount(pd.getTodayCount() + 1);
        plunderRepository.save(pd);

        // 写入掠夺记录到数据库
        long now = System.currentTimeMillis();
        plunderRecordMapper.insert(
                userId,
                "我",
                myLevel,
                targetId,
                targetName,
                targetLevel,
                faction,
                isNpcTarget,
                victory,
                silverGain, woodGain, paperGain, foodGain,
                now
        );

        // 构建返回
        Map<String, Object> result = new HashMap<>();
        result.put("victory", victory);
        result.put("targetName", targetName);
        result.put("targetLevel", targetLevel);
        result.put("silverGain", silverGain);
        result.put("woodGain", woodGain);
        result.put("paperGain", paperGain);
        result.put("foodGain", foodGain);
        result.put("availableCount", pd.getAvailableCount());
        result.put("todayCount", pd.getTodayCount());
        result.put("playerPower", playerPower);
        result.put("targetPower", targetPower);

        logger.info("用户 {} {}掠夺 {} (Lv.{}), 结果: {}", userId, victory ? "成功" : "失败", targetName, targetLevel, victory);
        return result;
    }

    /**
     * 购买掠夺次数
     */
    public Map<String, Object> purchaseCount(String userId) {
        PlunderData pd = plunderRepository.getOrInit(userId);
        if (pd.getTodayPurchased() >= PlunderConfig.MAX_PURCHASE_TIMES) {
            throw new BusinessException(400, "今日购买次数已达上限");
        }

        int cost = PlunderConfig.getPurchaseCost(pd.getTodayPurchased());
        UserResource resource = userResourceRepository.findByUserId(userId);
        if (resource == null || resource.getGold() == null || resource.getGold() < cost) {
            throw new BusinessException(400, "黄金不足，需要" + cost + "黄金");
        }

        resource.setGold(resource.getGold() - cost);
        userResourceRepository.save(resource);

        pd.setTodayPurchased(pd.getTodayPurchased() + 1);
        plunderRepository.save(pd);

        Map<String, Object> result = new HashMap<>();
        result.put("cost", cost);
        result.put("todayPurchased", pd.getTodayPurchased());
        result.put("availableCount", pd.getAvailableCount());
        result.put("nextCost", PlunderConfig.getPurchaseCost(pd.getTodayPurchased()));
        result.put("remainingGold", resource.getGold());
        return result;
    }

    /**
     * 获取掠夺记录（从数据库查询）
     */
    public Map<String, Object> getRecords(String userId, String type) {
        Map<String, Object> result = new HashMap<>();
        List<Map<String, Object>> records;
        if ("defense".equals(type)) {
            records = plunderRecordMapper.findByDefender(userId, 50);
        } else {
            records = plunderRecordMapper.findByAttacker(userId, 50);
        }
        // 转换数据库字段类型以兼容前端
        List<Map<String, Object>> formatted = new ArrayList<>();
        for (Map<String, Object> r : records) {
            Map<String, Object> item = new HashMap<>(r);
            Object isNpcObj = item.get("isNpc");
            item.put("isNpc", isNpcObj != null && (
                    Boolean.TRUE.equals(isNpcObj) || "1".equals(String.valueOf(isNpcObj))
                    || "true".equalsIgnoreCase(String.valueOf(isNpcObj))));
            Object victoryObj = item.get("victory");
            item.put("victory", victoryObj != null && (
                    Boolean.TRUE.equals(victoryObj) || "1".equals(String.valueOf(victoryObj))
                    || "true".equalsIgnoreCase(String.valueOf(victoryObj))));
            formatted.add(item);
        }
        result.put("records", formatted);
        return result;
    }

    private int parseIntSafe(Object val, int def) {
        if (val == null) return def;
        try { return Integer.parseInt(String.valueOf(val)); } catch (Exception e) { return def; }
    }

    private long parseLongSafe(Object val, long def) {
        if (val == null) return def;
        try { return Long.parseLong(String.valueOf(val)); } catch (Exception e) { return def; }
    }

    private String stripQuotes(String s) {
        if (s != null && s.startsWith("\"") && s.endsWith("\"")) {
            return s.substring(1, s.length() - 1);
        }
        return s;
    }
}
