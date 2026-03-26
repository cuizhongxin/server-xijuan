package com.tencent.wxcloudrun.service.dailytask;

import com.tencent.wxcloudrun.dao.CampaignProgressMapper;
import com.tencent.wxcloudrun.dao.DailyTaskMapper;
import com.tencent.wxcloudrun.exception.BusinessException;
import com.tencent.wxcloudrun.model.CampaignProgress;
import com.tencent.wxcloudrun.model.General;
import com.tencent.wxcloudrun.model.UserResource;
import com.tencent.wxcloudrun.model.Warehouse;
import com.tencent.wxcloudrun.repository.GeneralRepository;
import com.tencent.wxcloudrun.service.UserResourceService;
import com.tencent.wxcloudrun.service.warehouse.WarehouseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.*;

@Service
public class DailyTaskService {

    private static final Logger logger = LoggerFactory.getLogger(DailyTaskService.class);

    @Autowired private DailyTaskMapper taskMapper;
    @Autowired private UserResourceService userResourceService;
    @Autowired private WarehouseService warehouseService;
    @Autowired private GeneralRepository generalRepository;
    @Autowired private CampaignProgressMapper campaignProgressMapper;

    private static final int STAMINA_RECOVER_INTERVAL_MS = 600_000;
    private static final int STAMINA_ITEM_ID = 11101;
    private static final int STAMINA_PER_PILL = 5;

    // ── 9个日常任务定义 ──
    private static final TaskDef[] DAILY_TASKS = {
            new TaskDef("campaign",  "完成1次战役",       1, "silver",  5000),
            new TaskDef("plunder",   "完成1次掠夺",       1, "silver",  3000),
            new TaskDef("training",  "完成1次训练",       1, "item",    0, "15031", "初级经验药", 1),
            new TaskDef("supply",    "完成1次军需运送",   1, "food",    2000),
            new TaskDef("recruit",   "完成1次招募",       1, "item",    0, "15011", "初级招贤令", 1),
            new TaskDef("boss",      "参与Boss战",        1, "silver",  3000),
            new TaskDef("enhance",   "完成1次装备强化",   1, "item",    0, "14001", "1级强化石", 2),
            new TaskDef("produce",   "完成1次生产",       1, "metal",   1000),
            new TaskDef("herorank",  "完成1次英雄榜挑战", 1, "item",    0, "11001", "初级声望符", 1),
    };

    // ── 阶段奖励 ──
    private static final int[] STAGE_THRESHOLDS = {3, 6, 9};
    private static final String[][] STAGE_REWARDS = {
            {"11012", "银锭", "3", "2"},
            {"15012", "中级招贤令", "3", "1"},
            {"14004", "4级强化石", "3", "3"},
    };

    // ── 成就定义 ──
    private static final AchievementDef[] ACHIEVEMENTS = {
            new AchievementDef("level_10",    "level", 10,  50),
            new AchievementDef("level_20",    "level", 20,  100),
            new AchievementDef("level_30",    "level", 30,  200),
            new AchievementDef("level_40",    "level", 40,  300),
            new AchievementDef("level_50",    "level", 50,  500),
            new AchievementDef("level_60",    "level", 60,  800),
            new AchievementDef("level_80",    "level", 80,  1500),
            new AchievementDef("level_100",   "level", 100, 3000),
            new AchievementDef("campaign_1",  "campaign", 1, 100),
            new AchievementDef("campaign_2",  "campaign", 2, 150),
            new AchievementDef("campaign_3",  "campaign", 3, 200),
            new AchievementDef("campaign_4",  "campaign", 4, 300),
            new AchievementDef("campaign_5",  "campaign", 5, 400),
            new AchievementDef("campaign_6",  "campaign", 6, 500),
            new AchievementDef("campaign_7",  "campaign", 7, 800),
            new AchievementDef("campaign_8",  "campaign", 8, 1500),
            new AchievementDef("orange_3",    "orange", 3,  200),
            new AchievementDef("orange_5",    "orange", 5,  500),
            new AchievementDef("orange_10",   "orange", 10, 1500),
    };

    // ═══════════════════════════════════════════
    //  incrementTask — 各 Service 调用此方法
    // ═══════════════════════════════════════════

    public void incrementTask(String userId, String taskType) {
        try {
            String today = todayStr();
            taskMapper.upsertDailyTask(userId, today, taskType, 1);
        } catch (Exception e) {
            logger.warn("日常任务计数失败: userId={}, type={}", userId, taskType, e);
        }
    }

    // ═══════════════════════════════════════════
    //  getInfo — 返回三个Tab的完整数据
    // ═══════════════════════════════════════════

    public Map<String, Object> getInfo(String userId) {
        Map<String, Object> result = new LinkedHashMap<>();

        // Tab1: 日常任务
        result.put("dailyTasks", buildDailyTasks(userId));

        // Tab2: 成就
        result.put("achievements", buildAchievements(userId));

        // Tab3: 精力
        result.put("stamina", buildStaminaInfo(userId));

        return result;
    }

    private List<Map<String, Object>> buildDailyTasks(String userId) {
        String today = todayStr();
        List<Map<String, Object>> dbTasks = taskMapper.findDailyTasks(userId, today);
        Map<String, Map<String, Object>> taskMap = new HashMap<>();
        for (Map<String, Object> t : dbTasks) {
            taskMap.put((String) t.get("taskType"), t);
        }

        int completedCount = 0;
        List<Map<String, Object>> tasks = new ArrayList<>();
        for (TaskDef def : DAILY_TASKS) {
            Map<String, Object> row = taskMap.get(def.type);
            int progress = row != null ? ((Number) row.get("progress")).intValue() : 0;
            boolean claimed = row != null && ((Number) row.get("claimed")).intValue() == 1;
            boolean done = progress >= def.required;
            if (done) completedCount++;

            Map<String, Object> t = new LinkedHashMap<>();
            t.put("type", def.type);
            t.put("name", def.name);
            t.put("progress", progress);
            t.put("required", def.required);
            t.put("done", done);
            t.put("claimed", claimed);
            t.put("rewardDesc", def.rewardDesc());
            tasks.add(t);
        }

        // 阶段奖励
        List<Map<String, Object>> stageClaims = taskMapper.findStageClaims(userId, today);
        Set<Integer> claimedStages = new HashSet<>();
        for (Map<String, Object> sc : stageClaims) {
            claimedStages.add(((Number) sc.get("stage")).intValue());
        }

        List<Map<String, Object>> stages = new ArrayList<>();
        for (int i = 0; i < STAGE_THRESHOLDS.length; i++) {
            Map<String, Object> s = new LinkedHashMap<>();
            s.put("threshold", STAGE_THRESHOLDS[i]);
            s.put("reached", completedCount >= STAGE_THRESHOLDS[i]);
            s.put("claimed", claimedStages.contains(STAGE_THRESHOLDS[i]));
            s.put("rewardName", STAGE_REWARDS[i][1]);
            s.put("rewardCount", Integer.parseInt(STAGE_REWARDS[i][3]));
            stages.add(s);
        }

        Map<String, Object> wrapper = new LinkedHashMap<>();
        wrapper.put("tasks", tasks);
        wrapper.put("completedCount", completedCount);
        wrapper.put("stages", stages);
        return Collections.singletonList(wrapper);
    }

    private List<Map<String, Object>> buildAchievements(String userId) {
        UserResource resource = userResourceService.getUserResource(userId);
        int playerLevel = resource.getLevel() != null ? resource.getLevel() : 1;

        List<General> generals = generalRepository.findByUserId(userId);
        int orangeCount = 0;
        for (General g : generals) {
            if (g.getQualityId() != null && g.getQualityId() == 6) orangeCount++;
        }

        List<CampaignProgress> campaigns = campaignProgressMapper.findAllByUserId(userId);
        int clearedCampaigns = 0;
        for (CampaignProgress cp : campaigns) {
            if (cp.getCurrentStage() != null && cp.getCurrentStage() > 20) {
                clearedCampaigns++;
            }
        }

        List<Map<String, Object>> claimedList = taskMapper.findAllAchievements(userId);
        Set<String> claimedSet = new HashSet<>();
        for (Map<String, Object> a : claimedList) {
            if (((Number) a.get("claimed")).intValue() == 1) {
                claimedSet.add((String) a.get("achievementType"));
            }
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (AchievementDef def : ACHIEVEMENTS) {
            int current;
            switch (def.category) {
                case "level": current = playerLevel; break;
                case "campaign": current = clearedCampaigns; break;
                case "orange": current = orangeCount; break;
                default: current = 0;
            }

            Map<String, Object> a = new LinkedHashMap<>();
            a.put("id", def.id);
            a.put("category", def.category);
            a.put("name", def.displayName());
            a.put("current", current);
            a.put("required", def.threshold);
            a.put("done", current >= def.threshold);
            a.put("claimed", claimedSet.contains(def.id));
            a.put("rewardGold", def.rewardGold);
            result.add(a);
        }
        return result;
    }

    private Map<String, Object> buildStaminaInfo(String userId) {
        UserResource resource = userResourceService.getUserResource(userId);
        recoverStamina(resource);

        Map<String, Object> info = new LinkedHashMap<>();
        info.put("current", resource.getStamina());
        info.put("max", resource.getMaxStamina() != null ? resource.getMaxStamina() : 100);
        long lastRecover = resource.getLastStaminaRecoverTime() != null ? resource.getLastStaminaRecoverTime() : 0;
        long nextRecoverMs = lastRecover > 0 ? lastRecover + STAMINA_RECOVER_INTERVAL_MS - System.currentTimeMillis() : 0;
        info.put("nextRecoverSec", Math.max(0, nextRecoverMs / 1000));
        info.put("recoverInterval", STAMINA_RECOVER_INTERVAL_MS / 1000);
        return info;
    }

    // ═══════════════════════════════════════════
    //  领取日常任务奖励
    // ═══════════════════════════════════════════

    public Map<String, Object> claimDailyTask(String userId, String taskType) {
        String today = todayStr();
        List<Map<String, Object>> dbTasks = taskMapper.findDailyTasks(userId, today);
        Map<String, Object> row = null;
        for (Map<String, Object> t : dbTasks) {
            if (taskType.equals(t.get("taskType"))) { row = t; break; }
        }
        if (row == null) throw new BusinessException("任务尚未完成");
        if (((Number) row.get("claimed")).intValue() == 1) throw new BusinessException("奖励已领取");

        TaskDef def = null;
        for (TaskDef d : DAILY_TASKS) {
            if (d.type.equals(taskType)) { def = d; break; }
        }
        if (def == null) throw new BusinessException("无效的任务类型");

        int progress = ((Number) row.get("progress")).intValue();
        if (progress < def.required) throw new BusinessException("任务未完成");

        taskMapper.claimDailyTask(userId, today, taskType);
        grantTaskReward(userId, def);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("taskType", taskType);
        result.put("reward", def.rewardDesc());
        return result;
    }

    private void grantTaskReward(String userId, TaskDef def) {
        switch (def.rewardType) {
            case "silver": userResourceService.addSilver(userId, def.rewardAmount); break;
            case "food": userResourceService.addFood(userId, def.rewardAmount); break;
            case "metal": userResourceService.addMetal(userId, def.rewardAmount); break;
            case "item":
                addItem(userId, def.itemId, def.itemName, def.itemCount);
                break;
        }
    }

    // ═══════════════════════════════════════════
    //  领取阶段奖励
    // ═══════════════════════════════════════════

    public Map<String, Object> claimStageReward(String userId, int stage) {
        int idx = -1;
        for (int i = 0; i < STAGE_THRESHOLDS.length; i++) {
            if (STAGE_THRESHOLDS[i] == stage) { idx = i; break; }
        }
        if (idx < 0) throw new BusinessException("无效的阶段");

        String today = todayStr();
        List<Map<String, Object>> stageClaims = taskMapper.findStageClaims(userId, today);
        for (Map<String, Object> sc : stageClaims) {
            if (((Number) sc.get("stage")).intValue() == stage) {
                throw new BusinessException("阶段奖励已领取");
            }
        }

        List<Map<String, Object>> dbTasks = taskMapper.findDailyTasks(userId, today);
        int completedCount = 0;
        for (TaskDef def : DAILY_TASKS) {
            for (Map<String, Object> t : dbTasks) {
                if (def.type.equals(t.get("taskType"))) {
                    int p = ((Number) t.get("progress")).intValue();
                    if (p >= def.required) completedCount++;
                    break;
                }
            }
        }
        if (completedCount < stage) throw new BusinessException("完成任务数不足");

        taskMapper.insertStageClaim(userId, today, stage);
        String[] reward = STAGE_REWARDS[idx];
        int count = Integer.parseInt(reward[3]);
        addItem(userId, reward[0], reward[1], count);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("stage", stage);
        result.put("rewardName", reward[1]);
        result.put("rewardCount", count);
        return result;
    }

    // ═══════════════════════════════════════════
    //  领取成就奖励
    // ═══════════════════════════════════════════

    public Map<String, Object> claimAchievement(String userId, String achievementId) {
        AchievementDef def = null;
        for (AchievementDef d : ACHIEVEMENTS) {
            if (d.id.equals(achievementId)) { def = d; break; }
        }
        if (def == null) throw new BusinessException("无效的成就");

        Map<String, Object> existing = taskMapper.findAchievement(userId, achievementId);
        if (existing != null && ((Number) existing.get("claimed")).intValue() == 1) {
            throw new BusinessException("成就奖励已领取");
        }

        UserResource resource = userResourceService.getUserResource(userId);
        int playerLevel = resource.getLevel() != null ? resource.getLevel() : 1;
        List<General> generals = generalRepository.findByUserId(userId);
        int orangeCount = 0;
        for (General g : generals) {
            if (g.getQualityId() != null && g.getQualityId() == 6) orangeCount++;
        }
        List<CampaignProgress> campaigns = campaignProgressMapper.findAllByUserId(userId);
        int clearedCampaigns = 0;
        for (CampaignProgress cp : campaigns) {
            if (cp.getCurrentStage() != null && cp.getCurrentStage() > 20) clearedCampaigns++;
        }

        int current;
        switch (def.category) {
            case "level": current = playerLevel; break;
            case "campaign": current = clearedCampaigns; break;
            case "orange": current = orangeCount; break;
            default: current = 0;
        }
        if (current < def.threshold) throw new BusinessException("成就条件未达成");

        taskMapper.upsertAchievement(userId, achievementId);
        taskMapper.claimAchievement(userId, achievementId);
        userResourceService.addGold(userId, def.rewardGold);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("achievementId", achievementId);
        result.put("rewardGold", def.rewardGold);
        return result;
    }

    // ═══════════════════════════════════════════
    //  精力系统 — 使用精力丹
    // ═══════════════════════════════════════════

    public Map<String, Object> useStaminaItem(String userId) {
        boolean consumed = warehouseService.removeItem(userId, String.valueOf(STAMINA_ITEM_ID), 1);
        if (!consumed) throw new BusinessException("精力丹不足");

        UserResource resource = userResourceService.getUserResource(userId);
        recoverStamina(resource);
        int maxStam = resource.getMaxStamina() != null ? resource.getMaxStamina() : 100;
        int newStamina = Math.min(maxStam, resource.getStamina() + STAMINA_PER_PILL);
        resource.setStamina(newStamina);
        userResourceService.saveUserResource(resource);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("stamina", newStamina);
        result.put("maxStamina", maxStam);
        result.put("recovered", STAMINA_PER_PILL);
        return result;
    }

    // ═══════════════════════════════════════════
    //  精力懒恢复
    // ═══════════════════════════════════════════

    public void recoverStamina(UserResource resource) {
        int maxStam = resource.getMaxStamina() != null ? resource.getMaxStamina() : 100;
        int current = resource.getStamina() != null ? resource.getStamina() : maxStam;
        if (current >= maxStam) {
            resource.setLastStaminaRecoverTime(System.currentTimeMillis());
            return;
        }

        long lastRecover = resource.getLastStaminaRecoverTime() != null ? resource.getLastStaminaRecoverTime() : 0;
        if (lastRecover <= 0) {
            resource.setLastStaminaRecoverTime(System.currentTimeMillis());
            return;
        }

        long elapsed = System.currentTimeMillis() - lastRecover;
        int recovered = (int) (elapsed / STAMINA_RECOVER_INTERVAL_MS);
        if (recovered > 0) {
            int newStamina = Math.min(maxStam, current + recovered);
            resource.setStamina(newStamina);
            resource.setLastStaminaRecoverTime(lastRecover + (long) recovered * STAMINA_RECOVER_INTERVAL_MS);
            userResourceService.saveUserResource(resource);
        }
    }

    // ═══════════════════════════════════════════
    //  工具
    // ═══════════════════════════════════════════

    private String todayStr() {
        return new SimpleDateFormat("yyyy-MM-dd").format(new Date());
    }

    private void addItem(String userId, String itemId, String name, int count) {
        Warehouse.WarehouseItem item = Warehouse.WarehouseItem.builder()
                .itemId(itemId).itemType("item").name(name)
                .icon("images/item/" + itemId + ".jpg")
                .quality("2").count(count).maxStack(9999)
                .usable(true).build();
        warehouseService.addItem(userId, item);
    }

    // ═══════════════════════════════════════════
    //  内部定义类
    // ═══════════════════════════════════════════

    static class TaskDef {
        String type, name, rewardType;
        int required, rewardAmount;
        String itemId, itemName;
        int itemCount;

        TaskDef(String type, String name, int required, String rewardType, int rewardAmount) {
            this.type = type; this.name = name; this.required = required;
            this.rewardType = rewardType; this.rewardAmount = rewardAmount;
        }

        TaskDef(String type, String name, int required, String rewardType, int rewardAmount,
                String itemId, String itemName, int itemCount) {
            this(type, name, required, rewardType, rewardAmount);
            this.itemId = itemId; this.itemName = itemName; this.itemCount = itemCount;
        }

        String rewardDesc() {
            if ("item".equals(rewardType)) return itemName + " x" + itemCount;
            String rName;
            switch (rewardType) {
                case "silver": rName = "银两"; break;
                case "food": rName = "粮食"; break;
                case "metal": rName = "金属"; break;
                default: rName = rewardType;
            }
            return rName + " " + rewardAmount;
        }
    }

    static class AchievementDef {
        String id, category;
        int threshold, rewardGold;

        AchievementDef(String id, String category, int threshold, int rewardGold) {
            this.id = id; this.category = category;
            this.threshold = threshold; this.rewardGold = rewardGold;
        }

        String displayName() {
            switch (category) {
                case "level": return "君主等级达到" + threshold + "级";
                case "campaign": return "通关第" + threshold + "章战役";
                case "orange": return "拥有" + threshold + "个橙色武将";
                default: return id;
            }
        }
    }
}
