package com.tencent.wxcloudrun.service.alliance;

import com.tencent.wxcloudrun.dao.AllianceBossMapper;
import com.tencent.wxcloudrun.dao.EquipmentMapper;
import com.tencent.wxcloudrun.exception.BusinessException;
import com.tencent.wxcloudrun.model.Equipment;
import com.tencent.wxcloudrun.service.UserResourceService;
import com.tencent.wxcloudrun.service.SuitConfigService;
import com.tencent.wxcloudrun.service.battle.BattleCalculator;
import com.tencent.wxcloudrun.service.battle.BattleService;
import com.tencent.wxcloudrun.service.formation.FormationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class AllianceBossService {

    private static final Logger logger = LoggerFactory.getLogger(AllianceBossService.class);

    private static final int MAX_DAILY_ATTACKS = 3;
    private static final int FEED_COST_GOLD = 100;
    private static final int MIN_FEED_QUALITY = 3;
    private static final int SUMMON_MIN_HOUR = 20;
    private static final int SUMMON_MIN_MINUTE = 0;
    private static final int SUMMON_MAX_HOUR = 22;
    private static final int SUMMON_MAX_MINUTE = 0;

    private static final String[][] BOSS_TABLE = {
            {"远古巨兽", "1000000"},
            {"蛮荒凶兽", "2000000"},
            {"上古魔龙", "5000000"},
            {"混沌巨龙", "10000000"},
            {"灭世龙王", "20000000"}
    };

    @Autowired private AllianceBossMapper bossMapper;
    @Autowired private EquipmentMapper equipmentMapper;
    @Autowired private UserResourceService userResourceService;
    @Autowired private BattleService battleService;
    @Autowired private FormationService formationService;
    @Autowired private SuitConfigService suitConfigService;

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

        if (feedCount >= feedTarget) {
            bossMapper.updateBossStatus(bossId, "active");
            logger.info("联盟Boss已激活! serverId={}, 投喂进度 {}/{}", serverId, feedCount, feedTarget);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("feedAmount", amount);
        result.put("cost", cost);
        result.put("feedCount", feedCount);
        result.put("feedTarget", feedTarget);
        result.put("activated", feedCount >= feedTarget);
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
            int value = qualityId * 10;
            totalValue += value;
            consumedNames.add(eq.getName());
            equipmentMapper.deleteById(eqId);
        }

        if (totalValue <= 0) {
            throw new BusinessException(400, "没有可喂养的装备(需蓝色品质以上)");
        }

        bossMapper.incrementFeed(bossId, totalValue);
        bossMapper.insertRecord(userId, "feed_equip", 0, totalValue, serverId);

        int feedCount = ((Number) boss.get("feedCount")).intValue() + totalValue;
        int feedTarget = ((Number) boss.get("feedTarget")).intValue();

        if (feedCount >= feedTarget) {
            bossMapper.updateBossStatus(bossId, "active");
            logger.info("联盟Boss已激活(装备喂养)! serverId={}, 进度 {}/{}", serverId, feedCount, feedTarget);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("feedValue", totalValue);
        result.put("consumedCount", consumedNames.size());
        result.put("feedCount", feedCount);
        result.put("feedTarget", feedTarget);
        result.put("activated", feedCount >= feedTarget);
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
        if (!"active".equals(status)) {
            throw new BusinessException(400, "Boss未激活，无法召唤");
        }

        Calendar cal = Calendar.getInstance();
        int hour = cal.get(Calendar.HOUR_OF_DAY);
        int minute = cal.get(Calendar.MINUTE);
        int nowTime = hour * 100 + minute;
        int minTime = SUMMON_MIN_HOUR * 100 + SUMMON_MIN_MINUTE;
        int maxTime = SUMMON_MAX_HOUR * 100 + SUMMON_MAX_MINUTE;
        if (nowTime < minTime || nowTime > maxTime) {
            throw new BusinessException(400, String.format("召唤时间为每天%02d:%02d-%02d:%02d",
                    SUMMON_MIN_HOUR, SUMMON_MIN_MINUTE, SUMMON_MAX_HOUR, SUMMON_MAX_MINUTE));
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

    @Transactional
    public Map<String, Object> attack(String userId) {
        int serverId = extractServerId(userId);
        Map<String, Object> boss = bossMapper.findCurrentBossByServerId(serverId);
        if (boss == null) {
            throw new BusinessException(500, "Boss数据异常");
        }

        String status = (String) boss.get("status");
        if (!"fighting".equals(status) && !"active".equals(status)) {
            throw new BusinessException(400, "Boss未处于战斗状态");
        }

        int dailyCount = bossMapper.findUserDailyAttackCount(userId);
        if (dailyCount >= MAX_DAILY_ATTACKS) {
            throw new BusinessException(400, "今日攻击次数已用完（最多" + MAX_DAILY_ATTACKS + "次/天）");
        }

        long bossId = ((Number) boss.get("id")).longValue();
        long currentHp = ((Number) boss.get("currentHp")).longValue();
        long maxHp = ((Number) boss.get("maxHp")).longValue();
        int bossLevel = ((Number) boss.get("bossLevel")).intValue();

        List<BattleCalculator.BattleUnit> sideA = formationService.buildPlayerBattleUnits(userId);

        int bossHp = (int) Math.min(currentHp, Integer.MAX_VALUE);
        BattleCalculator.BattleUnit bossUnit = new BattleCalculator.BattleUnit();
        bossUnit.name = String.valueOf(boss.get("bossName"));
        bossUnit.level = bossLevel;
        bossUnit.totalAttack = bossLevel * 40;
        bossUnit.totalDefense = bossLevel * 25;
        bossUnit.valor = bossLevel * 4;
        bossUnit.command = bossLevel * 4;
        bossUnit.dodge = 5;
        bossUnit.hit = 10;
        bossUnit.mobility = 15;
        bossUnit.troopType = 1;
        bossUnit.soldierTier = Math.min(10, 1 + bossLevel / 8);
        bossUnit.soldierCount = bossHp;
        bossUnit.maxSoldierCount = bossHp;
        bossUnit.soldierLife = 500;
        bossUnit.position = 0;

        BattleService.BattleReport report = battleService.fight(sideA, Collections.singletonList(bossUnit), 20);
        long damage = (long)(bossHp - bossUnit.soldierCount);
        damage = Math.min(damage, currentHp);

        long newHp = currentHp - damage;
        bossMapper.updateBossHp(bossId, Math.max(0, newHp));
        bossMapper.insertRecord(userId, "attack", damage, 0, serverId);

        if (bossId > 0) {
            bossMapper.updateBossStatus(bossId, "fighting");
        }

        boolean killed = newHp <= 0;
        long rewardGold = 0;
        long rewardSilver = 0;

        if (killed) {
            rewardGold = 1000L * bossLevel;
            rewardSilver = 5000L * bossLevel;
            userResourceService.addGold(userId, rewardGold);
            userResourceService.addSilver(userId, rewardSilver);

            int nextLevel = Math.min(bossLevel + 1, BOSS_TABLE.length);
            int idx = nextLevel - 1;
            bossMapper.resetBoss(bossId, nextLevel, BOSS_TABLE[idx][0],
                    Long.parseLong(BOSS_TABLE[idx][1]), Long.parseLong(BOSS_TABLE[idx][1]));

            logger.info("联盟Boss被击杀! serverId={}, 升级到Lv.{} {}", serverId, nextLevel, BOSS_TABLE[idx][0]);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("damage", damage);
        result.put("remainingHp", Math.max(0, newHp));
        result.put("maxHp", maxHp);
        result.put("killed", killed);
        result.put("dailyAttacksUsed", dailyCount + 1);
        result.put("maxDailyAttacks", MAX_DAILY_ATTACKS);
        if (killed) {
            result.put("rewardGold", rewardGold);
            result.put("rewardSilver", rewardSilver);
        }
        result.put("battleReport", report);
        return result;
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
