package com.tencent.wxcloudrun.service.alliance;

import com.tencent.wxcloudrun.dao.AllianceBossMapper;
import com.tencent.wxcloudrun.dao.EquipmentMapper;
import com.tencent.wxcloudrun.exception.BusinessException;
import com.tencent.wxcloudrun.model.Equipment;
import com.tencent.wxcloudrun.model.Warehouse;
import com.tencent.wxcloudrun.service.UserResourceService;
import com.tencent.wxcloudrun.service.SuitConfigService;
import com.tencent.wxcloudrun.service.battle.BattleCalculator;
import com.tencent.wxcloudrun.service.battle.BattleService;
import com.tencent.wxcloudrun.service.formation.FormationService;
import com.tencent.wxcloudrun.service.warehouse.WarehouseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

@Service
public class AllianceBossService {

    private static final Logger logger = LoggerFactory.getLogger(AllianceBossService.class);

    private static final int MAX_DAILY_ATTACKS = 3;
    private static final int FEED_COST_GOLD = 100;
    private static final int MIN_FEED_QUALITY = 2;
    private static final int[] QUALITY_FEED_VALUES = {0, 0, 1, 3, 8, 15, 20};
    private static final long ALLIANCE_BOSS_HP = 5_000_000L;
    private static final int SUMMON_MIN_HOUR = 0;
    private static final int SUMMON_MIN_MINUTE = 0;
    private static final int SUMMON_MAX_HOUR = 23;
    private static final int SUMMON_MAX_MINUTE = 59;
    private static final int SQUAD_SIZE = 6;
    private static final int SQUAD_SOLDIERS_PER_UNIT = 1000;
    private static final int BASE_COOLDOWN_SEC = 90;
    private static final double DROP_RATE = 0.20;

    private static final int[] TROOP_LAYOUT = {1, 1, 3, 2, 3, 1};

    private static final String[][] DROP_TABLE = {
            {"15012", "中级招贤令", "0.30"},
            {"15042", "特训符",     "0.20"},
            {"11104", "招财符",     "0.20"},
            {"15052", "军需令",     "0.20"},
            {"14001", "1级强化石",  "0.10"}
    };

    private static final String[][] BOSS_TABLE = {
            {"远古巨兽", "5000000"},
            {"蛮荒凶兽", "5000000"},
            {"上古魔龙", "5000000"},
            {"混沌巨龙", "5000000"},
            {"灭世龙王", "5000000"}
    };

    private final Random random = new Random();
    private final Map<Integer, ConcurrentLinkedQueue<int[]>> woundedPools = new ConcurrentHashMap<>();

    @Autowired private AllianceBossMapper bossMapper;
    @Autowired private EquipmentMapper equipmentMapper;
    @Autowired private UserResourceService userResourceService;
    @Autowired private BattleService battleService;
    @Autowired private FormationService formationService;
    @Autowired private SuitConfigService suitConfigService;
    @Autowired private WarehouseService warehouseService;

    static int extractServerId(String compositeUserId) {
        if (compositeUserId != null && compositeUserId.contains("_")) {
            try {
                return Integer.parseInt(compositeUserId.substring(compositeUserId.lastIndexOf('_') + 1));
            } catch (NumberFormatException ignored) {}
        }
        return 1;
    }

    /**
     * 确保指定区服存在联盟Boss，在创建区服时调用
     */
    public void ensureBossExists(int serverId) {
        Map<String, Object> boss = bossMapper.findCurrentBossByServerId(serverId);
        if (boss == null) {
            bossMapper.insertBoss(1, BOSS_TABLE[0][0], Long.parseLong(BOSS_TABLE[0][1]), serverId);
            logger.info("初始化联盟Boss: {} Lv.1, serverId={}", BOSS_TABLE[0][0], serverId);
        }
    }

    public Map<String, Object> getInfo(String userId) {
        int serverId = extractServerId(userId);
        Map<String, Object> boss = bossMapper.findCurrentBossByServerId(serverId);
        if (boss == null) {
            ensureBossExists(serverId);
            boss = bossMapper.findCurrentBossByServerId(serverId);
        }
        if (boss == null) {
            throw new BusinessException(500, "Boss数据异常");
        }
        boss.put("summonTime", String.format("%02d:%02d-%02d:%02d",
                SUMMON_MIN_HOUR, SUMMON_MIN_MINUTE, SUMMON_MAX_HOUR, SUMMON_MAX_MINUTE));
        int feedCount = boss.get("feedCount") != null ? ((Number) boss.get("feedCount")).intValue() : 0;
        int feedTarget = boss.get("feedTarget") != null ? ((Number) boss.get("feedTarget")).intValue() : 100;
        boss.put("feedFull", feedCount >= feedTarget);
        return boss;
    }

    @Transactional
    public Map<String, Object> feed(String userId, int amount) {
        int serverId = extractServerId(userId);
        if (amount <= 0) amount = 1;

        long cost = (long) amount * FEED_COST_GOLD;
        boolean ok = userResourceService.consumeGold(userId, cost);
        if (!ok) {
            throw new BusinessException(400, "黄金不足，需要" + cost + "黄金");
        }

        Map<String, Object> boss = bossMapper.findCurrentBossByServerId(serverId);
        if (boss == null) {
            throw new BusinessException(500, "Boss数据异常");
        }

        long bossId = ((Number) boss.get("id")).longValue();
        String status = (String) boss.get("status");

        if ("active".equals(status) || "fighting".equals(status)) {
            throw new BusinessException(400, "Boss已激活，无需继续投喂");
        }

        bossMapper.incrementFeed(bossId, amount);
        bossMapper.insertRecord(userId, "feed", 0, amount, serverId);

        int feedCount = ((Number) boss.get("feedCount")).intValue() + amount;
        int feedTarget = ((Number) boss.get("feedTarget")).intValue();

        boolean full = feedCount >= feedTarget;
        if (full) {
            logger.info("联盟Boss喂养已满! serverId={}, 进度 {}/{}, 等待召唤", serverId, feedCount, feedTarget);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("feedAmount", amount);
        result.put("cost", cost);
        result.put("feedCount", feedCount);
        result.put("feedTarget", feedTarget);
        result.put("feedFull", full);
        return result;
    }

    @Transactional
    public Map<String, Object> feedWithEquipment(String userId, List<String> equipmentIds) {
        int serverId = extractServerId(userId);
        if (equipmentIds == null || equipmentIds.isEmpty()) {
            throw new BusinessException(400, "请先选择喂养材料!");
        }

        Map<String, Object> boss = bossMapper.findCurrentBossByServerId(serverId);
        if (boss == null) {
            throw new BusinessException(500, "Boss数据异常");
        }

        long bossId = ((Number) boss.get("id")).longValue();
        String status = (String) boss.get("status");
        if ("active".equals(status) || "fighting".equals(status)) {
            throw new BusinessException(400, "Boss已激活，无需继续投喂");
        }

        int totalValue = 0;
        List<String> consumedNames = new ArrayList<>();
        for (String eqId : equipmentIds) {
            Equipment eq = equipmentMapper.findById(eqId);
            if (eq == null) continue;
            if (!userId.equals(eq.getUserId())) continue;
            int qualityId = (eq.getQuality() != null && eq.getQuality().getId() != null)
                    ? eq.getQuality().getId() : (eq.getQualityValue() != null ? eq.getQualityValue() : 1);
            if (qualityId < MIN_FEED_QUALITY) continue;
            int value = qualityId < QUALITY_FEED_VALUES.length ? QUALITY_FEED_VALUES[qualityId] : qualityId * 10;
            totalValue += value;
            consumedNames.add(eq.getName());
            equipmentMapper.deleteById(eqId);
        }

        if (totalValue <= 0) {
            throw new BusinessException(400, "没有可喂养的装备(需绿色品质以上)");
        }

        bossMapper.incrementFeed(bossId, totalValue);
        bossMapper.insertRecord(userId, "feed_equip", 0, totalValue, serverId);

        int feedCount = ((Number) boss.get("feedCount")).intValue() + totalValue;
        int feedTarget = ((Number) boss.get("feedTarget")).intValue();

        boolean full = feedCount >= feedTarget;
        if (full) {
            logger.info("联盟Boss喂养已满(装备喂养)! serverId={}, 进度 {}/{}, 等待召唤", serverId, feedCount, feedTarget);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("feedValue", totalValue);
        result.put("consumedCount", consumedNames.size());
        result.put("feedCount", feedCount);
        result.put("feedTarget", feedTarget);
        result.put("feedFull", full);
        return result;
    }

    @Transactional
    public Map<String, Object> call(String userId) {
        int serverId = extractServerId(userId);
        Map<String, Object> boss = bossMapper.findCurrentBossByServerId(serverId);
        if (boss == null) {
            throw new BusinessException(500, "Boss数据异常");
        }

        String status = (String) boss.get("status");
        if ("fighting".equals(status)) {
            throw new BusinessException(400, "Boss已在战斗中");
        }

        int feedCount = ((Number) boss.get("feedCount")).intValue();
        int feedTarget = ((Number) boss.get("feedTarget")).intValue();
        if (feedCount < feedTarget) {
            throw new BusinessException(400, "喂养未满，无法召唤（" + feedCount + "/" + feedTarget + "）");
        }

        long bossId = ((Number) boss.get("id")).longValue();
        bossMapper.updateBossStatus(bossId, "fighting");

        Map<String, Object> result = new HashMap<>();
        result.put("message", "Boss战斗已开始!");
        result.put("boss", bossMapper.findCurrentBossByServerId(serverId));
        result.put("summonTime", String.format("%02d:%02d-%02d:%02d",
                SUMMON_MIN_HOUR, SUMMON_MIN_MINUTE, SUMMON_MAX_HOUR, SUMMON_MAX_MINUTE));
        return result;
    }

    private ConcurrentLinkedQueue<int[]> getWoundedPool(int serverId) {
        return woundedPools.computeIfAbsent(serverId, k -> new ConcurrentLinkedQueue<>());
    }

    @Transactional
    public Map<String, Object> attack(String userId) {
        int serverId = extractServerId(userId);
        Map<String, Object> boss = bossMapper.findCurrentBossByServerId(serverId);
        if (boss == null) {
            throw new BusinessException(500, "Boss数据异常");
        }

        String status = (String) boss.get("status");
        if (!"fighting".equals(status)) {
            throw new BusinessException(400, "Boss未处于战斗状态，请先召唤");
        }

        int dailyCount = bossMapper.findUserDailyAttackCount(userId);
        if (dailyCount >= MAX_DAILY_ATTACKS) {
            throw new BusinessException(400, "今日攻击次数已用完（最多" + MAX_DAILY_ATTACKS + "次/天）");
        }

        long bossId = ((Number) boss.get("id")).longValue();
        long currentHp = ((Number) boss.get("currentHp")).longValue();
        long maxHp = ((Number) boss.get("maxHp")).longValue();
        int bossLevel = ((Number) boss.get("bossLevel")).intValue();

        if (currentHp <= 0) {
            throw new BusinessException(400, "Boss已被击杀");
        }

        List<BattleCalculator.BattleUnit> sideA = formationService.buildPlayerBattleUnits(userId);

        ConcurrentLinkedQueue<int[]> pool = getWoundedPool(serverId);
        int[] squadSoldiers = pool.poll();
        if (squadSoldiers == null) {
            squadSoldiers = new int[SQUAD_SIZE];
            Arrays.fill(squadSoldiers, SQUAD_SOLDIERS_PER_UNIT);
        }

        int[] beforeSoldiers = Arrays.copyOf(squadSoldiers, SQUAD_SIZE);

        String[] unitNames = {"刀盾手", "长枪兵", "弓箭手", "骑兵", "法师", "统领"};
        int baseAtk = bossLevel * 40;
        int baseDef = bossLevel * 25;

        List<BattleCalculator.BattleUnit> sideB = new ArrayList<>();
        int[] sideBMapping = new int[SQUAD_SIZE];
        int sideBCount = 0;
        for (int i = 0; i < SQUAD_SIZE; i++) {
            if (squadSoldiers[i] <= 0) continue;
            BattleCalculator.BattleUnit bu = new BattleCalculator.BattleUnit();
            bu.name = unitNames[i];
            bu.level = bossLevel;
            bu.totalAttack = baseAtk;
            bu.totalDefense = baseDef;
            bu.valor = bossLevel * 2;
            bu.command = bossLevel * 2;
            bu.dodge = 0;
            bu.hit = 0;
            bu.mobility = 15;
            bu.troopType = TROOP_LAYOUT[i];
            bu.soldierTier = Math.min(10, 1 + bossLevel / 10);
            bu.soldierCount = squadSoldiers[i];
            bu.maxSoldierCount = SQUAD_SOLDIERS_PER_UNIT;
            bu.soldierLife = 500;
            bu.position = i;
            sideBMapping[sideBCount++] = i;
            sideB.add(bu);
        }
        if (sideB.isEmpty()) {
            throw new BusinessException(400, "Boss小队异常，请重试");
        }

        BattleService.BattleReport report = battleService.fight(sideA, sideB, 1);

        boolean squadWiped = true;
        for (int i = 0; i < sideBCount; i++) {
            squadSoldiers[sideBMapping[i]] = sideB.get(i).soldierCount;
            if (sideB.get(i).soldierCount > 0) squadWiped = false;
        }

        long hpDamage = 0;
        for (int i = 0; i < sideBCount; i++) {
            int idx = sideBMapping[i];
            hpDamage += Math.max(0, beforeSoldiers[idx] - squadSoldiers[idx]);
        }
        hpDamage = Math.min(hpDamage, currentHp);

        long newHp = Math.max(0, currentHp - hpDamage);
        bossMapper.updateBossHp(bossId, newHp);
        bossMapper.insertRecord(userId, "attack", hpDamage, 0, serverId);

        int roundsUsed = report.rounds.size();
        long cooldownMs = BASE_COOLDOWN_SEC * 1000L + roundsUsed * 10_000L;

        if (!squadWiped) {
            pool.offer(squadSoldiers);
        }

        boolean killed = newHp <= 0;
        long rewardGold = 0;
        long rewardSilver = 0;

        if (killed) {
            rewardGold = 1000L * bossLevel;
            rewardSilver = 5000L * bossLevel;
            userResourceService.addGold(userId, rewardGold);
            userResourceService.addSilver(userId, rewardSilver);
            pool.clear();

            int nextLevel = Math.min(bossLevel + 1, BOSS_TABLE.length);
            int idx = nextLevel - 1;
            bossMapper.resetBoss(bossId, nextLevel, BOSS_TABLE[idx][0],
                    Long.parseLong(BOSS_TABLE[idx][1]), Long.parseLong(BOSS_TABLE[idx][1]));

            logger.info("联盟Boss被击杀! serverId={}, 升级到Lv.{} {}", serverId, nextLevel, BOSS_TABLE[idx][0]);
        }

        Map<String, Object> dropResult = null;
        if (squadWiped && random.nextDouble() < DROP_RATE) {
            dropResult = rollDrop(userId);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("damage", hpDamage);
        result.put("remainingHp", newHp);
        result.put("maxHp", maxHp);
        result.put("killed", killed);
        result.put("dailyAttacksUsed", dailyCount + 1);
        result.put("maxDailyAttacks", MAX_DAILY_ATTACKS);
        result.put("cooldown", cooldownMs / 1000);
        if (killed) {
            result.put("rewardGold", rewardGold);
            result.put("rewardSilver", rewardSilver);
        }
        if (dropResult != null) {
            result.put("drop", dropResult);
        }
        result.put("battleReport", report);
        return result;
    }

    private Map<String, Object> rollDrop(String userId) {
        double roll = random.nextDouble();
        double cumulative = 0;
        String itemId = DROP_TABLE[0][0];
        String itemName = DROP_TABLE[0][1];
        for (String[] entry : DROP_TABLE) {
            cumulative += Double.parseDouble(entry[2]);
            if (roll < cumulative) {
                itemId = entry[0];
                itemName = entry[1];
                break;
            }
        }

        Warehouse.WarehouseItem item = Warehouse.WarehouseItem.builder()
                .itemId(itemId).name(itemName).itemType("material")
                .icon("images/item/" + itemId + ".jpg")
                .quality("3").count(1).maxStack(9999)
                .usable(true).build();
        warehouseService.addItem(userId, item);

        Map<String, Object> drop = new HashMap<>();
        drop.put("itemId", itemId);
        drop.put("itemName", itemName);
        drop.put("count", 1);
        return drop;
    }

    public List<Map<String, Object>> getRecords(String userId) {
        int serverId = extractServerId(userId);
        return bossMapper.findRecordsByServerId(serverId, 50);
    }

    public List<Map<String, Object>> getRankings(String userId) {
        int serverId = extractServerId(userId);
        return bossMapper.findRankingsByServerId(serverId, 20);
    }
}
