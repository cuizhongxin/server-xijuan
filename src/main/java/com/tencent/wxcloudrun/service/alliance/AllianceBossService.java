package com.tencent.wxcloudrun.service.alliance;

import com.tencent.wxcloudrun.dao.AllianceBossMapper;
import com.tencent.wxcloudrun.exception.BusinessException;
import com.tencent.wxcloudrun.model.General;
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

import javax.annotation.PostConstruct;
import java.util.*;

@Service
public class AllianceBossService {

    private static final Logger logger = LoggerFactory.getLogger(AllianceBossService.class);

    private static final int MAX_DAILY_ATTACKS = 3;
    private static final int FEED_COST_GOLD = 100;

    private static final String[][] BOSS_TABLE = {
            {"远古巨兽", "1000000"},
            {"蛮荒凶兽", "2000000"},
            {"上古魔龙", "5000000"},
            {"混沌巨龙", "10000000"},
            {"灭世龙王", "20000000"}
    };

    @Autowired
    private AllianceBossMapper bossMapper;

    @Autowired
    private UserResourceService userResourceService;

    @Autowired
    private BattleService battleService;

    @Autowired
    private FormationService formationService;

    @Autowired
    private SuitConfigService suitConfigService;

    @PostConstruct
    public void init() {
        Map<String, Object> boss = bossMapper.findCurrentBoss();
        if (boss == null) {
            bossMapper.insertBoss(1, BOSS_TABLE[0][0], Long.parseLong(BOSS_TABLE[0][1]));
            logger.info("初始化联盟Boss: {} Lv.1", BOSS_TABLE[0][0]);
        }
    }

    public Map<String, Object> getInfo() {
        Map<String, Object> boss = bossMapper.findCurrentBoss();
        if (boss == null) {
            throw new BusinessException(500, "Boss数据异常");
        }
        return boss;
    }

    @Transactional
    public Map<String, Object> feed(String userId, int amount) {
        if (amount <= 0) amount = 1;

        long cost = (long) amount * FEED_COST_GOLD;
        boolean ok = userResourceService.consumeGold(userId, cost);
        if (!ok) {
            throw new BusinessException(400, "黄金不足，需要" + cost + "黄金");
        }

        Map<String, Object> boss = bossMapper.findCurrentBoss();
        if (boss == null) {
            throw new BusinessException(500, "Boss数据异常");
        }

        long bossId = ((Number) boss.get("id")).longValue();
        String status = (String) boss.get("status");

        if ("active".equals(status) || "fighting".equals(status)) {
            throw new BusinessException(400, "Boss已激活，无需继续投喂");
        }

        bossMapper.incrementFeed(bossId, amount);
        bossMapper.insertRecord(userId, "feed", 0, amount);

        int feedCount = ((Number) boss.get("feedCount")).intValue() + amount;
        int feedTarget = ((Number) boss.get("feedTarget")).intValue();

        if (feedCount >= feedTarget) {
            bossMapper.updateBossStatus(bossId, "active");
            logger.info("联盟Boss已激活! 投喂进度 {}/{}", feedCount, feedTarget);
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
    public Map<String, Object> call(String userId) {
        Map<String, Object> boss = bossMapper.findCurrentBoss();
        if (boss == null) {
            throw new BusinessException(500, "Boss数据异常");
        }

        String status = (String) boss.get("status");
        if (!"active".equals(status)) {
            throw new BusinessException(400, "Boss未激活，无法召唤");
        }

        long bossId = ((Number) boss.get("id")).longValue();
        bossMapper.updateBossStatus(bossId, "fighting");

        Map<String, Object> result = new HashMap<>();
        result.put("message", "Boss战斗已开始!");
        result.put("boss", bossMapper.findCurrentBoss());
        return result;
    }

    @Transactional
    public Map<String, Object> attack(String userId) {
        Map<String, Object> boss = bossMapper.findCurrentBoss();
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

        // 构建玩家阵型
        List<General> myGenerals = formationService.getBattleOrder(userId);
        List<BattleCalculator.BattleUnit> sideA = new ArrayList<>();
        for (int i = 0; i < myGenerals.size(); i++) {
            General g = myGenerals.get(i);
            Map<String, Integer> eq = suitConfigService.calculateTotalEquipBonus(g.getId());
            int rawTier = g.getSoldierTier() != null ? g.getSoldierTier() : 1;
            int sRank = g.getSoldierRank() != null ? g.getSoldierRank() : 1;
            int tier = Math.max(rawTier, sRank);
            int troopType = BattleCalculator.parseTroopType(g.getTroopType());
            int maxSc = g.getSoldierMaxCount() != null ? g.getSoldierMaxCount() : 100;
            int formLv = BattleCalculator.maxPeopleToFormationLevel(maxSc);
            int sc = g.getSoldierCount() != null ? Math.min(g.getSoldierCount(), maxSc) : maxSc;
            BattleCalculator.BattleUnit u = BattleCalculator.assembleBattleUnit(
                    g.getName() != null ? g.getName() : "武将" + (i + 1),
                    g.getLevel() != null ? g.getLevel() : 1,
                    g.getAttrAttack() != null ? g.getAttrAttack() : 100,
                    g.getAttrDefense() != null ? g.getAttrDefense() : 50,
                    g.getAttrValor() != null ? g.getAttrValor() : 10,
                    g.getAttrCommand() != null ? g.getAttrCommand() : 10,
                    g.getAttrDodge() != null ? (int) Math.round(g.getAttrDodge()) : 5,
                    g.getAttrMobility() != null ? g.getAttrMobility() : 15,
                    troopType, tier, sc, maxSc, formLv,
                    eq.getOrDefault("attack", 0), eq.getOrDefault("defense", 0),
                    eq.getOrDefault("speed", 0), eq.getOrDefault("hit", 0),
                    eq.getOrDefault("dodge", 0), 0, 0, 0);
            u.position = i;
            sideA.add(u);
        }

        // Boss 作为高属性单个单位
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
        bossMapper.insertRecord(userId, "attack", damage, 0);

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

            logger.info("联盟Boss被击杀! 升级到Lv.{} {}", nextLevel, BOSS_TABLE[idx][0]);
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
        return result;
    }

    public List<Map<String, Object>> getRecords() {
        return bossMapper.findRecords(50);
    }

    public List<Map<String, Object>> getRankings() {
        return bossMapper.findRankings(20);
    }
}
