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
import com.tencent.wxcloudrun.service.formation.FormationService;
import com.tencent.wxcloudrun.service.battle.BattleCalculator;
import com.tencent.wxcloudrun.service.battle.BattleService;
import com.tencent.wxcloudrun.service.PlayerNameResolver;
import com.tencent.wxcloudrun.service.SuitConfigService;
import com.tencent.wxcloudrun.service.level.LevelService;
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

    static int extractServerId(String compositeUserId) {
        if (compositeUserId == null) return 1;
        int idx = compositeUserId.lastIndexOf('_');
        if (idx > 0) {
            try { return Integer.parseInt(compositeUserId.substring(idx + 1)); }
            catch (NumberFormatException e) { return 1; }
        }
        return 1;
    }

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

    @Autowired
    private FormationService formationService;

    @Autowired
    private BattleService battleService;

    @Autowired
    private SuitConfigService suitConfigService;

    @Autowired
    private PlayerNameResolver playerNameResolver;

    @Autowired
    private LevelService levelService;

    @org.springframework.beans.factory.annotation.Autowired @org.springframework.context.annotation.Lazy
    private com.tencent.wxcloudrun.service.dailytask.DailyTaskService dailyTaskService;

    /**
     * 获取掠夺主页数据
     */
    public Map<String, Object> getPlunderInfo(String userId) {
        PlunderData pd = plunderRepository.getOrInit(userId);
        int level = levelService.getUserLevel(userId).getLevel();

        Map<String, Object> info = new HashMap<>();
        int available = pd.getAvailableCount() != null ? pd.getAvailableCount() : 0;
        info.put("availableCount", available);
        info.put("maxCount", PlunderConfig.MAX_PLUNDER_COUNT);
        info.put("todayPurchased", pd.getTodayPurchased());
        info.put("maxPurchase", PlunderConfig.MAX_PURCHASE_TIMES);
        info.put("nextPurchaseCost", PlunderConfig.getPurchaseCost(pd.getTodayPurchased()));
        info.put("playerLevel", level);
        // 恢复倒计时信息
        long lastRecover = pd.getLastRecoverTime() != null ? pd.getLastRecoverTime() : 0;
        long nextRecoverMs = (available < PlunderConfig.MAX_PLUNDER_COUNT && lastRecover > 0)
                ? lastRecover + PlunderConfig.PLUNDER_RECOVER_MS - System.currentTimeMillis() : 0;
        info.put("nextRecoverSec", Math.max(0, nextRecoverMs / 1000));
        info.put("recoverIntervalSec", PlunderConfig.PLUNDER_RECOVER_MS / 1000);
        return info;
    }

    /**
     * 获取掠夺目标列表（优先玩家，不足NPC填充）
     */
    public Map<String, Object> getTargetList(String userId, int page) {
        int myLevel = levelService.getUserLevel(userId).getLevel();

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
        List<Map<String, Object>> allUsers = plunderRepository.findUserLevelsByServerId(extractServerId(userId));
        List<Map<String, Object>> matchedPlayers = new ArrayList<>();

        for (Map<String, Object> u : allUsers) {
            String uid = String.valueOf(u.get("userId"));
            if (uid.equals(userId)) continue;

            int uLevel = parseIntSafe(u.get("level"), 1);
            if (Math.abs(uLevel - myLevel) > PlunderConfig.LEVEL_RANGE) continue;

            boolean onCooldown = cooldownIds.contains(uid);

            Map<String, Object> target = new HashMap<>();
            target.put("id", uid);
            target.put("name", playerNameResolver.resolve(uid));
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
     * 执行掠夺 - 使用阵型全部武将，回合制伤害计算（与战役一致）
     */
    public Map<String, Object> doPlunder(String userId, String targetId) {
        PlunderData pd = plunderRepository.getOrInit(userId);

        int available = pd.getAvailableCount() != null ? pd.getAvailableCount() : 0;
        if (available <= 0) {
            throw new BusinessException(400, "掠夺次数已用完，请等待恢复或购买额外次数");
        }

        int myLevel = levelService.getUserLevel(userId).getLevel();
        UserResource myResource = userResourceRepository.findByUserId(userId);

        // 获取完整阵型（含装备+兵法+天赋加成）
        List<BattleCalculator.BattleUnit> playerBattleUnits = formationService.buildPlayerBattleUnits(userId);
        if (playerBattleUnits.isEmpty()) {
            throw new BusinessException(400, "请先配置阵型再进行掠夺");
        }

        // 冷却检查（非NPC目标）
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
        long tSilver, tWood, tPaper, tFood;
        String faction = null;

        // 构建敌方武将列表
        List<int[]> enemyUnits; // 每个元素: [attack, defense, troops]

        if (isNpcTarget) {
            List<PlunderNpc> npcs = plunderConfig.generateNpcsForLevel(myLevel);
            PlunderNpc npc = npcs.stream().filter(n -> n.getId().equals(targetId)).findFirst().orElse(null);
            if (npc == null) throw new BusinessException(400, "NPC不存在");

            targetName = npc.getName();
            targetLevel = npc.getLevel();
            tSilver = npc.getSilver();
            tWood = npc.getWood();
            tPaper = npc.getPaper();
            tFood = npc.getFood();
            faction = npc.getFaction();

            // NPC生成虚拟武将（数量与玩家阵型一致）
            int npcCount = Math.max(1, playerBattleUnits.size());
            enemyUnits = new ArrayList<>();
            for (int i = 0; i < npcCount; i++) {
                int eAttack = targetLevel * 4;
                int eDefense = targetLevel * 4;
                int eTroops = 500;
                enemyUnits.add(new int[]{eAttack, eDefense, eTroops});
            }
        } else {
            UserResource targetResource = userResourceRepository.findByUserId(targetId);
            if (targetResource == null) throw new BusinessException(400, "玩家不存在");

            targetName = playerNameResolver.resolve(targetId);
            targetLevel = levelService.getUserLevel(targetId).getLevel();
            tSilver = targetResource.getSilver() != null ? targetResource.getSilver() : 0;
            tWood = targetResource.getWood() != null ? targetResource.getWood() : 0;
            tPaper = targetResource.getPaper() != null ? targetResource.getPaper() : 0;
            tFood = targetResource.getFood() != null ? targetResource.getFood() : 0;

            // 获取对手阵型武将
            List<General> targetGenerals = formationService.getBattleOrder(targetId);
            if (targetGenerals.isEmpty()) {
                // 对手没配阵型，用其所有武将
                targetGenerals = generalRepository.findByUserId(targetId);
            }
            enemyUnits = new ArrayList<>();
            for (General tg : targetGenerals) {
                int eAttack = (tg.getAttrAttack() != null ? tg.getAttrAttack() : 50) + (tg.getAttrValor() != null ? tg.getAttrValor() / 2 : 0);
                int eDefense = (tg.getAttrDefense() != null ? tg.getAttrDefense() : 30) + (tg.getAttrCommand() != null ? tg.getAttrCommand() / 2 : 0);
                int eTroops = tg.getSoldierCount() != null ? tg.getSoldierCount() : 500;
                enemyUnits.add(new int[]{eAttack, eDefense, eTroops});
            }
            if (enemyUnits.isEmpty()) {
                enemyUnits.add(new int[]{targetLevel * 10, targetLevel * 6, 500});
            }
        }

        // ========== 使用统一战斗系统 ==========

        List<BattleCalculator.BattleUnit> enemyBattleUnits = new ArrayList<>();
        for (int i = 0; i < enemyUnits.size(); i++) {
            int[] eu = enemyUnits.get(i);
            int eTroopType = 1 + (i % 3);
            int eSoldiers = eu.length > 2 ? eu[2] : 500;

            BattleCalculator.BattleUnit u = new BattleCalculator.BattleUnit();
            u.name = isNpcTarget ? (faction != null ? faction : "敌将") + (i + 1) : "敌将" + (i + 1);
            u.level = targetLevel;
            u.troopType = eTroopType;
            u.soldierTier = Math.max(1, Math.min(10, 1 + targetLevel / 20));
            u.soldierCount = eSoldiers;
            u.maxSoldierCount = eSoldiers;
            u.totalAttack = eu[0];
            u.totalDefense = eu.length > 1 ? eu[1] : eu[0];
            u.soldierLife = isNpcTarget ? 100 : BattleCalculator.getSoldierLife(eTroopType, u.soldierTier);
            u.valor = isNpcTarget ? 0 : targetLevel * 2;
            u.command = isNpcTarget ? 0 : targetLevel * 2;
            u.dodge = 5;
            u.hit = 15;
            u.mobility = 10;
            u.position = i;
            enemyBattleUnits.add(u);
        }

        BattleService.BattleReport report = battleService.fight(playerBattleUnits, enemyBattleUnits, 20);
        boolean victory = report.victoryA;

        List<String> battleLog = new ArrayList<>();
        battleLog.add(String.format("【掠夺战斗开始】我方 %d 将 vs %s (Lv.%d)", playerBattleUnits.size(), targetName, targetLevel));
        battleLog.addAll(report.toBattleLog("我方", "敌方"));

        // ========== 奖励计算（不变） ==========
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

        // 扣减可用次数
        pd.setAvailableCount(Math.max(0, (pd.getAvailableCount() != null ? pd.getAvailableCount() : 0) - 1));
        pd.setTodayCount((pd.getTodayCount() != null ? pd.getTodayCount() : 0) + 1);
        plunderRepository.save(pd);

        // 写入掠夺记录到数据库
        long now = System.currentTimeMillis();
        plunderRecordMapper.insert(
                userId,
                playerNameResolver.resolve(userId),
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
        result.put("maxCount", PlunderConfig.MAX_PLUNDER_COUNT);
        result.put("playerPower", playerBattleUnits.stream().mapToInt(u -> u.totalAttack + u.totalDefense).sum());
        result.put("targetPower", enemyBattleUnits.stream().mapToInt(u -> u.totalAttack + u.totalDefense).sum());
        result.put("battleLog", battleLog);
        result.put("rounds", report.totalRounds);
        result.put("generalCount", playerBattleUnits.size());
        result.put("battleReport", report);
        dailyTaskService.incrementTask(userId, "plunder");

        logger.info("用户 {} {}掠夺 {} (Lv.{}), {}将参战, {}回合", userId, victory ? "成功" : "失败", targetName, targetLevel, playerBattleUnits.size(), report.totalRounds);
        return result;
    }

    /**
     * 购买掠夺次数
     */
    public Map<String, Object> purchaseCount(String userId) {
        PlunderData pd = plunderRepository.getOrInit(userId);
        int purchased = pd.getTodayPurchased() != null ? pd.getTodayPurchased() : 0;
        if (purchased >= PlunderConfig.MAX_PURCHASE_TIMES) {
            throw new BusinessException(400, "今日购买次数已达上限");
        }

        int cost = PlunderConfig.getPurchaseCost(purchased);
        UserResource resource = userResourceRepository.findByUserId(userId);
        if (resource == null || resource.getGold() == null || resource.getGold() < cost) {
            throw new BusinessException(400, "黄金不足，需要" + cost + "黄金");
        }

        resource.setGold(resource.getGold() - cost);
        userResourceRepository.save(resource);

        pd.setTodayPurchased(purchased + 1);
        int current = pd.getAvailableCount() != null ? pd.getAvailableCount() : 0;
        pd.setAvailableCount(current + 1);
        plunderRepository.save(pd);

        Map<String, Object> result = new HashMap<>();
        result.put("cost", cost);
        result.put("todayPurchased", pd.getTodayPurchased());
        result.put("availableCount", pd.getAvailableCount());
        result.put("maxCount", PlunderConfig.MAX_PLUNDER_COUNT);
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
            String attackerId = String.valueOf(item.get("attackerId"));
            String defenderId = String.valueOf(item.get("defenderId"));
            if (attackerId != null && !attackerId.startsWith("npc_")) {
                item.put("attackerName", playerNameResolver.resolve(attackerId));
            }
            if (defenderId != null && !defenderId.startsWith("npc_")) {
                item.put("defenderName", playerNameResolver.resolve(defenderId));
            }
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
