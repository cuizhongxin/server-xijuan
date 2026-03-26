package com.tencent.wxcloudrun.service.bosswar;

import com.tencent.wxcloudrun.dao.WorldBossMapper;
import com.tencent.wxcloudrun.model.Equipment;
import com.tencent.wxcloudrun.model.UserResource;
import com.tencent.wxcloudrun.model.Warehouse;
import com.tencent.wxcloudrun.service.UserResourceService;
import com.tencent.wxcloudrun.service.battle.BattleCalculator;
import com.tencent.wxcloudrun.service.battle.BattleService;
import com.tencent.wxcloudrun.service.equipment.EquipmentService;
import com.tencent.wxcloudrun.service.formation.FormationService;
import com.tencent.wxcloudrun.service.warehouse.WarehouseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

@Service
public class BossWarService {

    private static final Logger logger = LoggerFactory.getLogger(BossWarService.class);

    private static final int BOSS_HJLK = 1001;
    private static final int BOSS_DZJT = 2001;
    private static final int BOSS_YZJT = 3001;
    private static final int DURATION_MINUTES = 30;

    private static final int SQUAD_SIZE = 6;
    private static final int BASE_COOLDOWN_SEC = 0;
    private static final double COMMON_DROP_RATE = 0.25;
    private static final double EQUIP_DROP_RATE = 0.10;
    private static final double STRONG_SQUAD_CHANCE = 0.3;

    // APK ArmyService.json — Weak (流寇/军团) stats: {life, att, def, sp}
    private static final int[] HJ_INF = {340, 160, 250, 12};
    private static final int[] HJ_CAV = {315, 200, 210, 18};
    private static final int[] HJ_ARC = {315, 280, 150, 10};
    private static final int[] DZ_INF = {380, 220, 400, 12};
    private static final int[] DZ_CAV = {330, 300, 290, 20};
    private static final int[] DZ_ARC = {330, 400, 200, 10};
    private static final int[] YZ_INF = {530, 360, 660, 12};
    private static final int[] YZ_CAV = {430, 660, 420, 20};
    private static final int[] YZ_ARC = {430, 900, 360, 10};
    // APK ArmyService.json — Strong (卫队/首领卫队) stats
    private static final int[] HJ_E_INF = {430, 240, 600, 12};
    private static final int[] HJ_E_CAV = {360, 400, 320, 24};
    private static final int[] HJ_E_ARC = {350, 550, 250, 10};
    private static final int[] DZ_E_INF = {540, 360, 900, 12};
    private static final int[] DZ_E_CAV = {430, 600, 480, 27};
    private static final int[] DZ_E_ARC = {420, 860, 350, 10};
    private static final int[] YZ_E_INF = {840, 720, 1500, 12};
    private static final int[] YZ_E_CAV = {730, 1600, 800, 27};
    private static final int[] YZ_E_ARC = {730, 2400, 700, 10};

    // squad troop layout: [步,步,弓,骑,弓,步]
    private static final int[] TROOP_LAYOUT = {1, 1, 3, 2, 3, 1};

    private static final Map<Integer, BossTemplate> BOSS_TEMPLATES = new LinkedHashMap<>();
    static {
        // APK MonsterShow_cfg.json: 1411 黄巾流寇 exp=2500, 1412 黄巾卫队 exp=6000
        //                          1421 董卓军团 exp=4000, 1422 董卓卫队 exp=8500
        //                          1431 异族士兵 exp=4000, 1432 首领卫队 exp=8500
        BOSS_TEMPLATES.put(BOSS_HJLK, new BossTemplate(BOSS_HJLK, "黄巾流寇", 25,
                "boss_hangjzk.png", 20,
                new int[]{0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23},
                3_000_000L, 500,
                new String[]{"黄巾刀盾手","黄巾长枪兵","黄巾弓箭手","黄巾骑兵","黄巾法师","黄巾渠帅"},
                new String[]{"黄巾卫队刀盾","黄巾卫队长枪","黄巾卫队弓手","黄巾卫队骑兵","黄巾卫队法师","黄巾卫队统领"},
                new int[][]{HJ_INF, HJ_INF, HJ_ARC, HJ_CAV, HJ_ARC, HJ_INF},
                new int[][]{HJ_E_INF, HJ_E_INF, HJ_E_ARC, HJ_E_CAV, HJ_E_ARC, HJ_E_INF},
                2500, 6000));
        BOSS_TEMPLATES.put(BOSS_DZJT, new BossTemplate(BOSS_DZJT, "董卓军团", 35,
                "boss_dongz.png", 30, new int[]{12},
                5_000_000L, 600,
                new String[]{"西凉刀盾手","西凉长枪兵","西凉弓箭手","西凉铁骑","西凉军师","西凉猛将"},
                new String[]{"西凉卫队刀盾","西凉卫队长枪","西凉卫队弓手","西凉卫队铁骑","西凉卫队军师","西凉卫队猛将"},
                new int[][]{DZ_INF, DZ_INF, DZ_ARC, DZ_CAV, DZ_ARC, DZ_INF},
                new int[][]{DZ_E_INF, DZ_E_INF, DZ_E_ARC, DZ_E_CAV, DZ_E_ARC, DZ_E_INF},
                4000, 8500));
        BOSS_TEMPLATES.put(BOSS_YZJT, new BossTemplate(BOSS_YZJT, "异族军团", 40,
                "boss_yizu.png", 40, new int[]{18},
                8_000_000L, 700,
                new String[]{"异族刀盾手","异族长枪兵","异族弓箭手","异族骑兵","异族萨满","异族首领"},
                new String[]{"首领卫队刀盾","首领卫队长枪","首领卫队弓手","首领卫队骑兵","首领卫队萨满","异族大首领"},
                new int[][]{YZ_INF, YZ_INF, YZ_ARC, YZ_CAV, YZ_ARC, YZ_INF},
                new int[][]{YZ_E_INF, YZ_E_INF, YZ_E_ARC, YZ_E_CAV, YZ_E_ARC, YZ_E_INF},
                4000, 8500));
    }

    private static final String[][] COMMON_DROPS = {
            {"15042", "特训符",     "15042.jpg", "3"},
            {"15012", "中级招贤令", "15012.jpg", "3"},
            {"11104", "招财符",     "11104.jpg", "3"},
            {"11012", "银锭",       "11012.jpg", "3"},
            {"11001", "初级声望符", "11001.jpg", "2"},
    };

    private static final Map<Integer, String> BOSS_EQUIP_SET = new LinkedHashMap<>();
    static {
        BOSS_EQUIP_SET.put(BOSS_HJLK, "陷阵");
        BOSS_EQUIP_SET.put(BOSS_DZJT, "天狼");
        BOSS_EQUIP_SET.put(BOSS_YZJT, "龙威");
    }

    private static final String[] EQUIP_PARTS = {"武器", "戒指", "铠甲", "项链", "头盔", "鞋子"};

    private static final Map<Integer, String[]> BOSS_CHEST = new LinkedHashMap<>();
    static {
        BOSS_CHEST.put(BOSS_HJLK, new String[]{"11070", "平乱宝箱", "11070.jpg", "3"});
        BOSS_CHEST.put(BOSS_DZJT, new String[]{"11071", "征戎宝箱", "11071.jpg", "4"});
        BOSS_CHEST.put(BOSS_YZJT, new String[]{"11073", "讨逆宝箱", "11073.jpg", "4"});
    }

    private static final String TIANDI_CHEST_ID = "17010";
    private static final Map<String, ChestLootConfig> CHEST_LOOT = new LinkedHashMap<>();
    static {
        CHEST_LOOT.put("11070", new ChestLootConfig(5, new String[][]{
                {"11012","银锭","11012.jpg","3"}, {"15012","中级招贤令","15012.jpg","3"},
                {"15042","特训符","15042.jpg","3"}, {"14003","3级强化石","14003.jpg","3"},
                {"15052","军需令","15052.jpg","3"}, {"14033","3阶品质石","14033.jpg","3"}
        }));
        CHEST_LOOT.put("11071", new ChestLootConfig(10, new String[][]{
                {"11012","银锭","11012.jpg","3"}, {"15012","中级招贤令","15012.jpg","3"},
                {"15045","高级传承符","15045.jpg","4"}, {"14004","4级强化石","14004.jpg","3"},
                {"15052","军需令","15052.jpg","3"}, {"14034","4阶品质石","14034.jpg","3"}
        }));
        CHEST_LOOT.put("11073", new ChestLootConfig(15, new String[][]{
                {"11012","银锭","11012.jpg","3"}, {"15012","中级招贤令","15012.jpg","3"},
                {"15045","高级传承符","15045.jpg","4"}, {"14005","5级强化石","14005.jpg","3"},
                {"15052","军需令","15052.jpg","3"}, {"14035","5阶品质石","14035.jpg","3"},
                {"15043","高级强化转移符","15043.jpg","4"}
        }));
        CHEST_LOOT.put("17001", new ChestLootConfig(10, new String[][]{
                {"11012","银锭","11012.jpg","3"}, {"15012","中级招贤令","15012.jpg","3"},
                {"15045","高级传承符","15045.jpg","4"}, {"14004","4级强化石","14004.jpg","3"},
                {"15052","军需令","15052.jpg","3"}, {"14034","4阶品质石","14034.jpg","3"}
        }));
        CHEST_LOOT.put("17002", new ChestLootConfig(15, new String[][]{
                {"11012","银锭","11012.jpg","3"}, {"15012","中级招贤令","15012.jpg","3"},
                {"15045","高级传承符","15045.jpg","4"}, {"14004","4级强化石","14004.jpg","3"},
                {"15052","军需令","15052.jpg","3"}, {"14035","5阶品质石","14035.jpg","3"},
                {"15043","高级强化转移符","15043.jpg","4"}
        }));
        CHEST_LOOT.put("17003", new ChestLootConfig(20, new String[][]{
                {"11012","银锭","11012.jpg","3"}, {"15012","中级招贤令","15012.jpg","3"},
                {"15045","高级传承符","15045.jpg","4"}, {"14005","5级强化石","14005.jpg","3"},
                {"15052","军需令","15052.jpg","3"}, {"14036","6阶品质石","14036.jpg","3"},
                {"15043","高级强化转移符","15043.jpg","4"}
        }));
    }

    private static final String[] TIANDI_EQUIP_SETS = {"幽冥", "天诛", "地煞"};

    private static int getVipCdReductionPct(int vipLevel) {
        if (vipLevel <= 0) return 0;
        return Math.min(25, ((vipLevel + 1) / 2) * 5);
    }

    @Autowired private WorldBossMapper worldBossMapper;
    @Autowired private BattleService battleService;
    @Autowired private FormationService formationService;
    @Autowired private UserResourceService userResourceService;
    @Autowired private WarehouseService warehouseService;
    @Autowired private EquipmentService equipmentService;
    @Autowired private com.tencent.wxcloudrun.service.level.LevelService levelService;
    @Autowired @org.springframework.context.annotation.Lazy private com.tencent.wxcloudrun.service.dailytask.DailyTaskService dailyTaskService;
    @Autowired @org.springframework.context.annotation.Lazy private com.tencent.wxcloudrun.service.chat.ChatService chatService;

    private final Random random = new Random();

    private final Map<String, ConcurrentLinkedQueue<WoundedSquad>> woundedPools = new ConcurrentHashMap<>();
    private final Map<String, Object> bossLocks = new ConcurrentHashMap<>();

    private ConcurrentLinkedQueue<WoundedSquad> getWoundedPool(int serverId, int bossId) {
        return woundedPools.computeIfAbsent(serverId + "_" + bossId, k -> new ConcurrentLinkedQueue<>());
    }

    private Object getBossLock(int serverId, int bossId) {
        return bossLocks.computeIfAbsent(serverId + "_" + bossId, k -> new Object());
    }

    static int extractServerId(String compositeUserId) {
        if (compositeUserId != null && compositeUserId.contains("_")) {
            try {
                return Integer.parseInt(compositeUserId.substring(compositeUserId.lastIndexOf('_') + 1));
            } catch (NumberFormatException ignored) {}
        }
        return 1;
    }

    // ═══════════════════════════════════════════
    //  DB 状态读写
    // ═══════════════════════════════════════════

    private BossState loadOrCreateState(int serverId, BossTemplate t) {
        Map<String, Object> row = worldBossMapper.findState(serverId, t.id);
        if (row == null) {
            worldBossMapper.upsertState(serverId, t.id, "waiting",
                    String.valueOf(t.totalHp), "", 0);
            return new BossState("waiting", t.totalHp, "", 0);
        }
        long hp = t.totalHp;
        String raw = (String) row.get("unitSoldiers");
        if (raw != null) {
            try { hp = Long.parseLong(raw.contains(";") ? raw.split(";")[0] : raw.trim()); }
            catch (Exception ignored) {}
        }
        return new BossState(
                (String) row.get("status"), hp,
                (String) row.get("lastKiller"),
                ((Number) row.get("windowStartMs")).longValue());
    }

    private void saveState(int serverId, int bossId, BossState state) {
        worldBossMapper.upsertState(serverId, bossId, state.status,
                String.valueOf(state.remainingHp), state.lastKiller, state.windowStartMs);
    }

    // ═══════════════════════════════════════════
    //  时间窗口刷新
    // ═══════════════════════════════════════════

    private BossState refreshBossState(int serverId, BossTemplate t) {
        BossState state = loadOrCreateState(serverId, t);
        TimeWindow tw = getCurrentOrNextWindow(t);
        boolean changed = false;

        if (tw.active) {
            if (!"active".equals(state.status) && !"killed".equals(state.status)) {
                resetBoss(state, t, serverId);
                state.windowStartMs = tw.startMs;
                changed = true;
            }
            if ("active".equals(state.status) && state.windowStartMs != tw.startMs) {
                resetBoss(state, t, serverId);
                state.windowStartMs = tw.startMs;
                changed = true;
            }
        } else {
            if ("active".equals(state.status)) {
                state.status = "escaped";
                changed = true;
                getWoundedPool(serverId, t.id).clear();
                try {
                    settleAndAnnounce(serverId, t, state);
                } catch (Exception e) {
                    logger.error("Boss结算异常: serverId={}, bossId={}", serverId, t.id, e);
                }
            }
        }

        if (changed) {
            saveState(serverId, t.id, state);
        }
        return state;
    }

    private void resetBoss(BossState state, BossTemplate t, int serverId) {
        state.status = "active";
        state.lastKiller = "";
        state.remainingHp = t.totalHp;
        getWoundedPool(serverId, t.id).clear();
    }

    // ═══════════════════════════════════════════
    //  getInfo
    // ═══════════════════════════════════════════

    public Map<String, Object> getInfo(String userId) {
        int serverId = extractServerId(userId);

        BossTemplate activeBoss = null;
        BossTemplate nextBoss = null;
        long nearestMs = Long.MAX_VALUE;

        for (BossTemplate t : BOSS_TEMPLATES.values()) {
            BossState state = refreshBossState(serverId, t);
            if ("active".equals(state.status)) {
                activeBoss = t;
                break;
            }
            TimeWindow tw = getCurrentOrNextWindow(t);
            if (tw.nextAppearMs > 0 && tw.nextAppearMs < nearestMs) {
                nearestMs = tw.nextAppearMs;
                nextBoss = t;
            }
        }

        BossTemplate displayBoss = activeBoss != null ? activeBoss : nextBoss;
        if (displayBoss == null) displayBoss = BOSS_TEMPLATES.values().iterator().next();

        Map<String, Object> result = new HashMap<>();
        result.put("currentBoss", buildBossInfo(serverId, displayBoss, userId));
        result.put("serverTime", System.currentTimeMillis());

        List<Map<String, Object>> allBossSchedule = new ArrayList<>();
        for (BossTemplate t : BOSS_TEMPLATES.values()) {
            Map<String, Object> s = new HashMap<>();
            s.put("id", t.id);
            s.put("name", t.name);
            s.put("appearHours", formatAppearHours(t));
            allBossSchedule.add(s);
        }
        result.put("schedule", allBossSchedule);
        return result;
    }

    private Map<String, Object> buildBossInfo(int serverId, BossTemplate t, String userId) {
        BossState state = loadOrCreateState(serverId, t);
        TimeWindow tw = getCurrentOrNextWindow(t);

        Map<String, Object> bm = new HashMap<>();
        bm.put("id", t.id);
        bm.put("name", t.name);
        bm.put("level", t.level);
        bm.put("pic", t.pic);
        bm.put("minLevel", t.minLevel);
        bm.put("appearHours", formatAppearHours(t));
        bm.put("durationMinutes", DURATION_MINUTES);
        bm.put("status", state.status);
        bm.put("lastKiller", state.lastKiller);

        bm.put("currentHp", state.remainingHp);
        bm.put("maxHp", t.totalHp);

        bm.put("windowActive", tw.active);
        bm.put("windowEndMs", tw.endMs);
        bm.put("nextAppearMs", tw.nextAppearMs);
        bm.put("remainSec", tw.active ? Math.max(0, (tw.endMs - System.currentTimeMillis()) / 1000) : 0);

        long myDamage = 0;
        int myAttackCount = 0;
        double cooldown = 0;
        Map<String, Object> pd = worldBossMapper.findPlayerDamage(serverId, t.id, userId, state.windowStartMs);
        if (pd != null) {
            myDamage = ((Number) pd.get("totalDamage")).longValue();
            myAttackCount = ((Number) pd.get("attackCount")).intValue();
            long cooldownUntil = ((Number) pd.get("cooldownUntil")).longValue();
            cooldown = Math.max(0, (cooldownUntil - System.currentTimeMillis()) / 1000.0);
        }
        bm.put("myDamage", myDamage);
        bm.put("myAttackCount", myAttackCount);
        bm.put("cooldown", cooldown);

        if ("active".equals(state.status)) {
            List<Map<String, Object>> liveRanking = worldBossMapper.findDamageRanking(
                    serverId, t.id, state.windowStartMs, 10);
            for (Map<String, Object> r : liveRanking) {
                r.put("playerName", resolvePlayerName((String) r.get("userId")));
            }
            bm.put("liveRanking", liveRanking);
        } else {
            bm.put("lastBattleReport", buildLastBattleReport(serverId, t, state));
        }

        return bm;
    }

    private Map<String, Object> buildLastBattleReport(int serverId, BossTemplate t, BossState state) {
        Map<String, Object> report = new HashMap<>();
        report.put("bossName", t.name);
        report.put("killed", "killed".equals(state.status));
        report.put("killer", state.lastKiller != null && !state.lastKiller.isEmpty()
                ? resolvePlayerName(state.lastKiller) : null);

        String[] chestInfo = BOSS_CHEST.get(t.id);
        String chestName = chestInfo != null ? chestInfo[1] : "";

        List<Map<String, Object>> ranking = worldBossMapper.findDamageRanking(
                serverId, t.id, state.windowStartMs, 3);
        int[] chestCounts = {2, 1, 1};
        List<Map<String, Object>> topList = new ArrayList<>();
        for (int i = 0; i < Math.min(3, ranking.size()); i++) {
            Map<String, Object> entry = new HashMap<>();
            entry.put("rank", i + 1);
            entry.put("playerName", resolvePlayerName((String) ranking.get(i).get("userId")));
            entry.put("damage", ranking.get(i).get("totalDamage"));
            entry.put("chestName", "killed".equals(state.status) ? chestName : "");
            entry.put("chestCount", "killed".equals(state.status) ? chestCounts[i] : 0);
            topList.add(entry);
        }
        report.put("top3", topList);
        return report;
    }

    // ═══════════════════════════════════════════
    //  attack
    // ═══════════════════════════════════════════

    public Map<String, Object> attack(String userId, int bossId) {
        int serverId = extractServerId(userId);
        BossTemplate t = BOSS_TEMPLATES.get(bossId);
        if (t == null) throw new RuntimeException("Boss不存在");

        BossState state = refreshBossState(serverId, t);
        if (!"active".equals(state.status)) throw new RuntimeException("Boss当前不可攻击");
        if (state.remainingHp <= 0) throw new RuntimeException("Boss已被击杀");

        Map<String, Object> pd = worldBossMapper.findPlayerDamage(serverId, bossId, userId, state.windowStartMs);
        long now = System.currentTimeMillis();
        if (pd != null) {
            long cooldownUntil = ((Number) pd.get("cooldownUntil")).longValue();
            if (cooldownUntil > now) {
                throw new RuntimeException("正在休整中，还需等待" + (cooldownUntil - now) / 1000 + "秒");
            }
        }

        List<BattleCalculator.BattleUnit> sideA = formationService.buildPlayerBattleUnits(userId);

        ConcurrentLinkedQueue<WoundedSquad> pool = getWoundedPool(serverId, bossId);
        WoundedSquad wounded = pool.poll();

        boolean isStrongSquad;
        int[] squadSoldiers = new int[SQUAD_SIZE];
        if (wounded != null) {
            isStrongSquad = wounded.isStrong;
            System.arraycopy(wounded.soldiers, 0, squadSoldiers, 0, SQUAD_SIZE);
        } else {
            isStrongSquad = random.nextDouble() < STRONG_SQUAD_CHANCE;
            Arrays.fill(squadSoldiers, t.squadSoldiersPerUnit);
        }

        String[] unitNames = isStrongSquad ? t.strongUnitNames : t.weakUnitNames;
        int[][] unitStats = isStrongSquad ? t.strongUnitStats : t.weakUnitStats;

        List<BattleCalculator.BattleUnit> sideB = new ArrayList<>();
        int[] sideBMapping = new int[SQUAD_SIZE];
        int sideBCount = 0;
        for (int i = 0; i < SQUAD_SIZE; i++) {
            if (squadSoldiers[i] <= 0) continue;
            BattleCalculator.BattleUnit bu = new BattleCalculator.BattleUnit();
            bu.name = unitNames[i];
            bu.level = t.level;
            int[] stats = unitStats[i];
            bu.totalAttack = stats[1];
            bu.totalDefense = stats[2];
            bu.valor = t.level * 2;
            bu.command = t.level * 2;
            bu.dodge = 0; bu.hit = 0;
            bu.mobility = stats[3];
            bu.troopType = TROOP_LAYOUT[i];
            bu.soldierTier = Math.min(10, 1 + t.level / 10);
            bu.soldierCount = squadSoldiers[i];
            bu.maxSoldierCount = t.squadSoldiersPerUnit;
            bu.soldierLife = stats[0];
            bu.position = i;
            sideBMapping[sideBCount++] = i;
            sideB.add(bu);
        }
        if (sideB.isEmpty()) throw new RuntimeException("Boss小队异常，请重试");

        BattleService.BattleReport report = battleService.fight(sideA, sideB, 1);

        long hpDamage = 0;
        int soldiersKilled = 0;
        boolean squadWiped = true;
        for (int j = 0; j < sideB.size(); j++) {
            int idx = sideBMapping[j];
            BattleCalculator.BattleUnit bu = sideB.get(j);
            int before = squadSoldiers[idx];
            int after = Math.max(0, bu.soldierCount);
            int killed = before - after;
            soldiersKilled += killed;
            hpDamage += (long) killed * unitStats[idx][0];
            squadSoldiers[idx] = after;
            if (after > 0) squadWiped = false;
        }

        int vipLevel = getPlayerVipLevel(userId);
        int coolSec = (int) (BASE_COOLDOWN_SEC * (100 - getVipCdReductionPct(vipLevel)) / 100.0);
        long cooldownUntil = now + coolSec * 1000L;

        boolean bossKilled;
        synchronized (getBossLock(serverId, bossId)) {
            BossState latest = loadOrCreateState(serverId, t);
            latest.remainingHp = Math.max(0, latest.remainingHp - hpDamage);
            bossKilled = latest.remainingHp <= 0;

            worldBossMapper.upsertPlayerDamage(serverId, bossId, userId,
                    state.windowStartMs, hpDamage, cooldownUntil);

            if (bossKilled) {
                latest.status = "killed";
                latest.lastKiller = userId;
                pool.clear();
            }
            saveState(serverId, bossId, latest);
            state = latest;
        }

        if (!squadWiped && !bossKilled) {
            pool.offer(new WoundedSquad(isStrongSquad, squadSoldiers));
        }

        if (bossKilled) {
            try { settleAndAnnounce(serverId, t, state); }
            catch (Exception e) { logger.error("Boss击杀结算异常: serverId={}, bossId={}", serverId, t.id, e); }
        }

        Map<String, Object> dropResult = null;
        int expGained = 0;
        Map<String, Object> lordLevelInfo = null;
        if (squadWiped) {
            dropResult = rollDrops(userId, bossId, isStrongSquad);
            expGained = isStrongSquad ? t.strongExp : t.weakExp;
            try {
                lordLevelInfo = levelService.addExp(userId, expGained, "Boss战-" + t.name);
            } catch (Exception e) {
                logger.warn("Boss战发放主公经验失败: userId={}, exp={}", userId, expGained, e);
            }
        }
        long myTotalDamage = hpDamage;
        int myAttackCount = 1;
        Map<String, Object> pdAfter = worldBossMapper.findPlayerDamage(serverId, bossId, userId, state.windowStartMs);
        if (pdAfter != null) {
            myTotalDamage = ((Number) pdAfter.get("totalDamage")).longValue();
            myAttackCount = ((Number) pdAfter.get("attackCount")).intValue();
        }

        Map<String, Object> result = new HashMap<>();
        result.put("victory", squadWiped);
        result.put("damage", hpDamage);
        result.put("soldiersKilled", soldiersKilled);
        result.put("cooldown", coolSec);
        result.put("myDamage", myTotalDamage);
        result.put("myAttackCount", myAttackCount);
        result.put("bossRemainingHp", state.remainingHp);
        result.put("bossMaxHp", t.totalHp);
        result.put("bossKilled", bossKilled);
        result.put("battleReport", report);
        result.put("expGained", expGained);
        if (lordLevelInfo != null) result.put("lordLevelInfo", lordLevelInfo);
        if (dropResult != null) result.put("drops", dropResult);

        logger.info("Boss战: userId={}, bossId={}, victory={}, wounded={}, hpDmg={}, exp={}, bossKilled={}",
                userId, bossId, squadWiped, wounded != null, hpDamage, expGained, bossKilled);
        dailyTaskService.incrementTask(userId, "boss");
        return result;
    }

    // ═══════════════════════════════════════════
    //  掉落
    // ═══════════════════════════════════════════

    private Map<String, Object> rollDrops(String userId, int bossId, boolean isStrongSquad) {
        Map<String, Object> result = new HashMap<>();
        List<String> rewards = new ArrayList<>();

        if (random.nextDouble() < COMMON_DROP_RATE) {
            String[] item = COMMON_DROPS[random.nextInt(COMMON_DROPS.length)];
            addItemToWarehouse(userId, item[0], item[1], item[2], item[3], 1);
            rewards.add(item[1] + " x1");
            result.put("commonDrop", item[1]);
        }

        if (isStrongSquad && random.nextDouble() < EQUIP_DROP_RATE) {
            String setName = BOSS_EQUIP_SET.get(bossId);
            if (setName != null) {
                String part = EQUIP_PARTS[random.nextInt(EQUIP_PARTS.length)];
                String fullPartName = setName + part;
                try {
                    Equipment equip = equipmentService.createSetEquipment(
                            userId, setName, fullPartName, "BOSS_DROP",
                            "Boss掉落-" + BOSS_TEMPLATES.get(bossId).name);
                    rewards.add(fullPartName);
                    result.put("equipDrop", fullPartName);
                    result.put("equipId", equip.getId());
                } catch (Exception e) {
                    logger.warn("Boss装备掉落创建失败: set={}, part={}", setName, fullPartName, e);
                }
            }
        }

        result.put("rewards", rewards);
        return result;
    }

    // ═══════════════════════════════════════════
    //  排名宝箱分发（从DB读取排名）
    // ═══════════════════════════════════════════

    private List<Map<String, Object>> distributeChests(int serverId, int bossId,
                                                       String lastHitter, long windowStartMs) {
        String[] chestInfo = BOSS_CHEST.get(bossId);
        if (chestInfo == null) return Collections.emptyList();

        List<Map<String, Object>> result = new ArrayList<>();
        List<Map<String, Object>> ranking = worldBossMapper.findDamageRanking(
                serverId, bossId, windowStartMs, 3);

        int[] chestCounts = {2, 1, 1};
        for (int i = 0; i < Math.min(3, ranking.size()); i++) {
            String playerId = (String) ranking.get(i).get("userId");
            int count = chestCounts[i];
            addItemToWarehouse(playerId, chestInfo[0], chestInfo[1], chestInfo[2], chestInfo[3], count);
            Map<String, Object> entry = new HashMap<>();
            entry.put("rank", i + 1);
            entry.put("userId", playerId);
            entry.put("chest", chestInfo[1]);
            entry.put("count", count);
            entry.put("damage", ranking.get(i).get("totalDamage"));
            result.add(entry);
            logger.info("Boss宝箱: rank={}, userId={}, chest={}x{}", i + 1, playerId, chestInfo[1], count);
        }

        if (lastHitter != null && !lastHitter.isEmpty()) {
            addItemToWarehouse(lastHitter, chestInfo[0], chestInfo[1], chestInfo[2], chestInfo[3], 1);
            Map<String, Object> lastHitEntry = new HashMap<>();
            lastHitEntry.put("rank", 0);
            lastHitEntry.put("userId", lastHitter);
            lastHitEntry.put("chest", chestInfo[1]);
            lastHitEntry.put("count", 1);
            lastHitEntry.put("type", "lastHit");
            result.add(lastHitEntry);
            logger.info("Boss最后一击宝箱: userId={}, chest={}", lastHitter, chestInfo[1]);
        }

        return result;
    }

    // ═══════════════════════════════════════════
    //  开启Boss宝箱
    // ═══════════════════════════════════════════

    public Map<String, Object> openBossChest(String userId, String chestItemId) {
        ChestLootConfig config = CHEST_LOOT.get(chestItemId);
        if (config == null) throw new RuntimeException("无效的宝箱类型");

        if (!warehouseService.removeItem(userId, chestItemId, 1)) {
            throw new RuntimeException("宝箱数量不足");
        }

        Map<String, Object> result = new HashMap<>();
        if (random.nextInt(100) < config.tiandiPct) {
            addItemToWarehouse(userId, TIANDI_CHEST_ID, "天地宝盒", "17010.jpg", "5", 1);
            result.put("item", "天地宝盒");
            result.put("isTiandi", true);
        } else {
            String[] item = config.fallbackItems[random.nextInt(config.fallbackItems.length)];
            addItemToWarehouse(userId, item[0], item[1], item[2], item[3], 1);
            result.put("item", item[1]);
            result.put("isTiandi", false);
        }
        return result;
    }

    /**
     * Boss时间窗口结束时结算: 分发宝箱 + 全服通告
     */
    private void settleAndAnnounce(int serverId, BossTemplate t, BossState state) {
        List<Map<String, Object>> chestResult = distributeChests(serverId, t.id, null, state.windowStartMs);
        if (chestResult == null || chestResult.isEmpty()) return;

        StringBuilder sb = new StringBuilder();
        sb.append("【").append(t.name).append("】战斗结算!\n");
        for (Map<String, Object> entry : chestResult) {
            int rank = (Integer) entry.get("rank");
            String playerId = (String) entry.get("userId");
            String playerName = resolvePlayerName(playerId);
            long damage = entry.get("damage") != null ? ((Number) entry.get("damage")).longValue() : 0;
            int count = (Integer) entry.get("count");
            String chestName = (String) entry.get("chest");
            if ("lastHit".equals(entry.get("type"))) continue;
            sb.append("第").append(rank).append("名: ").append(playerName)
              .append(" 伤害").append(damage)
              .append(" 获得").append(chestName).append("x").append(count).append("\n");
        }

        try {
            chatService.sendSystemMessage(serverId, "world", sb.toString().trim());
        } catch (Exception e) {
            logger.warn("Boss结算通告发送失败: serverId={}", serverId, e);
        }
        logger.info("Boss结算完成: serverId={}, bossId={}, rewards={}", serverId, t.id, chestResult.size());
    }

    public Map<String, Object> openTiandiChest(String userId) {
        if (!warehouseService.removeItem(userId, TIANDI_CHEST_ID, 1)) {
            throw new RuntimeException("天地宝盒数量不足");
        }

        String setName = TIANDI_EQUIP_SETS[random.nextInt(TIANDI_EQUIP_SETS.length)];
        String part = EQUIP_PARTS[random.nextInt(EQUIP_PARTS.length)];
        String fullPartName = setName + part;

        Equipment equip = equipmentService.createSetEquipment(
                userId, setName, fullPartName, "TIANDI_CHEST", "天地宝盒开启");

        Map<String, Object> result = new HashMap<>();
        result.put("setName", setName);
        result.put("equipment", fullPartName);
        result.put("equipmentId", equip.getId());
        return result;
    }

    // ═══════════════════════════════════════════
    //  排名查询
    // ═══════════════════════════════════════════

    public Map<String, Object> getRankings(String userId, int bossId) {
        int serverId = extractServerId(userId);
        BossState state = loadOrCreateState(serverId, BOSS_TEMPLATES.getOrDefault(bossId,
                BOSS_TEMPLATES.values().iterator().next()));

        List<Map<String, Object>> ranking = worldBossMapper.findDamageRanking(
                serverId, bossId, state.windowStartMs, 20);

        for (Map<String, Object> r : ranking) {
            r.put("playerName", resolvePlayerName((String) r.get("userId")));
        }

        Map<String, Object> result = new HashMap<>();
        result.put("rankings", ranking);
        return result;
    }

    // ═══════════════════════════════════════════
    //  时间窗口
    // ═══════════════════════════════════════════

    private TimeWindow getCurrentOrNextWindow(BossTemplate t) {
        long now = System.currentTimeMillis();
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(now);
        int curHour = cal.get(Calendar.HOUR_OF_DAY);
        int curMin = cal.get(Calendar.MINUTE);
        double curTime = curHour + curMin / 60.0;

        // 测试模式: 黄巾流寇全天常驻
        if (t.id == BOSS_HJLK) {
            Calendar start = (Calendar) cal.clone();
            start.set(Calendar.MINUTE, 0); start.set(Calendar.SECOND, 0); start.set(Calendar.MILLISECOND, 0);
            Calendar end = (Calendar) start.clone();
            end.add(Calendar.MINUTE, 60);
            return new TimeWindow(true, start.getTimeInMillis(), end.getTimeInMillis(), 0);
        }

        for (int hour : t.appearHours) {
            double endTime = hour + DURATION_MINUTES / 60.0;
            if (curTime >= hour && curTime < endTime) {
                Calendar start = (Calendar) cal.clone();
                start.set(Calendar.HOUR_OF_DAY, hour);
                start.set(Calendar.MINUTE, 0);
                start.set(Calendar.SECOND, 0);
                start.set(Calendar.MILLISECOND, 0);
                Calendar end = (Calendar) start.clone();
                end.add(Calendar.MINUTE, DURATION_MINUTES);
                return new TimeWindow(true, start.getTimeInMillis(), end.getTimeInMillis(), 0);
            }
        }

        long nextAppear = Long.MAX_VALUE;
        for (int hour : t.appearHours) {
            Calendar target = (Calendar) cal.clone();
            target.set(Calendar.HOUR_OF_DAY, hour);
            target.set(Calendar.MINUTE, 0);
            target.set(Calendar.SECOND, 0);
            target.set(Calendar.MILLISECOND, 0);
            if (target.getTimeInMillis() <= now) {
                target.add(Calendar.DAY_OF_MONTH, 1);
            }
            if (target.getTimeInMillis() < nextAppear) {
                nextAppear = target.getTimeInMillis();
            }
        }
        return new TimeWindow(false, 0, 0, nextAppear);
    }

    // ═══════════════════════════════════════════
    //  辅助
    // ═══════════════════════════════════════════

    private int getPlayerVipLevel(String userId) {
        try {
            UserResource res = userResourceService.getUserResource(userId);
            return res.getVipLevel() != null ? res.getVipLevel() : 0;
        } catch (Exception e) {
            return 0;
        }
    }

    private void addItemToWarehouse(String userId, String itemId, String name,
                                     String icon, String quality, int count) {
        Warehouse.WarehouseItem item = Warehouse.WarehouseItem.builder()
                .itemId(itemId).itemType("item").name(name)
                .icon("images/item/" + icon)
                .quality(quality).count(count).maxStack(9999)
                .usable(true).build();
        warehouseService.addItem(userId, item);
    }

    private String formatAppearHours(BossTemplate t) {
        StringBuilder sb = new StringBuilder("每天");
        for (int i = 0; i < t.appearHours.length; i++) {
            if (i > 0) sb.append(",");
            sb.append(String.format("%02d:00", t.appearHours[i]));
        }
        return sb.toString();
    }

    private String resolvePlayerName(String userId) {
        return "玩家" + userId.substring(Math.max(0, userId.length() - 4));
    }

    // ═══════════════════════════════════════════
    //  内部数据类
    // ═══════════════════════════════════════════

    static class BossTemplate {
        int id;
        String name;
        int level;
        String pic;
        int minLevel;
        int[] appearHours;
        long totalHp;
        int squadSoldiersPerUnit;
        String[] weakUnitNames;
        String[] strongUnitNames;
        int[][] weakUnitStats;
        int[][] strongUnitStats;
        int weakExp;
        int strongExp;

        BossTemplate(int id, String name, int level, String pic, int minLevel,
                     int[] appearHours, long totalHp, int squadSoldiersPerUnit,
                     String[] weakUnitNames, String[] strongUnitNames,
                     int[][] weakUnitStats, int[][] strongUnitStats,
                     int weakExp, int strongExp) {
            this.id = id; this.name = name; this.level = level;
            this.pic = pic; this.minLevel = minLevel;
            this.appearHours = appearHours;
            this.totalHp = totalHp;
            this.squadSoldiersPerUnit = squadSoldiersPerUnit;
            this.weakUnitNames = weakUnitNames;
            this.strongUnitNames = strongUnitNames;
            this.weakUnitStats = weakUnitStats;
            this.strongUnitStats = strongUnitStats;
            this.weakExp = weakExp;
            this.strongExp = strongExp;
        }
    }

    static class BossState {
        String status;
        long remainingHp;
        String lastKiller;
        long windowStartMs;

        BossState(String status, long remainingHp, String lastKiller, long windowStartMs) {
            this.status = status;
            this.remainingHp = remainingHp;
            this.lastKiller = lastKiller;
            this.windowStartMs = windowStartMs;
        }
    }

    static class WoundedSquad {
        boolean isStrong;
        int[] soldiers;

        WoundedSquad(boolean isStrong, int[] soldiers) {
            this.isStrong = isStrong;
            this.soldiers = soldiers;
        }
    }

    static class TimeWindow {
        boolean active;
        long startMs, endMs, nextAppearMs;

        TimeWindow(boolean active, long startMs, long endMs, long nextAppearMs) {
            this.active = active; this.startMs = startMs;
            this.endMs = endMs; this.nextAppearMs = nextAppearMs;
        }
    }

    static class ChestLootConfig {
        int tiandiPct;
        String[][] fallbackItems;

        ChestLootConfig(int tiandiPct, String[][] fallbackItems) {
            this.tiandiPct = tiandiPct;
            this.fallbackItems = fallbackItems;
        }
    }
}
