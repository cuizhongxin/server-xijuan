package com.tencent.wxcloudrun.service.bosswar;

import com.tencent.wxcloudrun.model.General;
import com.tencent.wxcloudrun.service.SuitConfigService;
import com.tencent.wxcloudrun.service.battle.BattleCalculator;
import com.tencent.wxcloudrun.service.battle.BattleService;
import com.tencent.wxcloudrun.service.formation.FormationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 世界Boss战 - 对标APK BossWarShow_cfg
 *
 * 黄巾流寇(1001): 每天除12:00和18:00外，每3小时一次(0,3,6,9,15,21)，需20级
 * 董卓军团(2001): 每天12:00出现，需30级
 * 异族军团(3001): 每天18:00出现，需40级
 * 每次出现持续30分钟，未击杀则逃遁
 */
@Service
public class BossWarService {

    private static final int BOSS_HJLK = 1001;
    private static final int BOSS_DZJT = 2001;
    private static final int BOSS_YZJT = 3001;
    private static final int DURATION_MINUTES = 30;

    private static final Map<Integer, BossTemplate> BOSS_TEMPLATES = new LinkedHashMap<>();
    static {
        BOSS_TEMPLATES.put(BOSS_HJLK, new BossTemplate(BOSS_HJLK, "黄巾流寇", 25, 500000,
                "boss_hangjzk.png", 20, new int[]{0, 3, 6, 9, 15, 21}));
        BOSS_TEMPLATES.put(BOSS_DZJT, new BossTemplate(BOSS_DZJT, "董卓军团", 35, 1500000,
                "boss_dongz.png", 30, new int[]{12}));
        BOSS_TEMPLATES.put(BOSS_YZJT, new BossTemplate(BOSS_YZJT, "异族军团", 40, 3000000,
                "boss_yizu.png", 40, new int[]{18}));
    }

    @Autowired
    private BattleService battleService;

    @Autowired
    private FormationService formationService;

    @Autowired
    private SuitConfigService suitConfigService;

    private final Map<Integer, BossState> bossStates = new ConcurrentHashMap<>();
    private final Map<String, Map<Integer, PlayerBossData>> playerData = new ConcurrentHashMap<>();

    public BossWarService() {
        for (BossTemplate t : BOSS_TEMPLATES.values()) {
            bossStates.put(t.id, new BossState(t));
        }
    }

    public Map<String, Object> getInfo(String userId) {
        Map<String, Object> result = new HashMap<>();
        List<Map<String, Object>> bossList = new ArrayList<>();

        for (BossTemplate t : BOSS_TEMPLATES.values()) {
            BossState state = bossStates.get(t.id);
            refreshBossState(t, state);

            Map<String, Object> bm = new HashMap<>();
            bm.put("id", t.id);
            bm.put("name", t.name);
            bm.put("level", t.level);
            bm.put("pic", t.pic);
            bm.put("minLevel", t.minLevel);
            bm.put("maxHp", t.maxHp);
            bm.put("currentHp", state.currentHp);
            bm.put("status", state.status);
            bm.put("lastKiller", state.lastKiller);
            bm.put("lastKillCount", state.lastKillCount);

            bm.put("appearHours", formatAppearHours(t));
            bm.put("durationMinutes", DURATION_MINUTES);

            TimeWindow tw = getCurrentOrNextWindow(t);
            bm.put("windowActive", tw.active);
            bm.put("windowEndMs", tw.endMs);
            bm.put("nextAppearMs", tw.nextAppearMs);
            bm.put("remainSec", tw.active ? Math.max(0, (tw.endMs - System.currentTimeMillis()) / 1000) : 0);

            PlayerBossData pd = getPlayerData(userId, t.id);
            bm.put("myKill", pd.killCount);
            bm.put("cooldown", Math.max(0, pd.cooldownUntil - System.currentTimeMillis()) / 1000.0);

            bossList.add(bm);
        }

        result.put("bossList", bossList);
        result.put("serverTime", System.currentTimeMillis());
        return result;
    }

    public Map<String, Object> attack(String userId, int bossId) {
        BossTemplate t = BOSS_TEMPLATES.get(bossId);
        if (t == null) throw new RuntimeException("Boss不存在");

        BossState state = bossStates.get(bossId);
        refreshBossState(t, state);

        if (!"active".equals(state.status)) {
            throw new RuntimeException("Boss已不存在!");
        }

        PlayerBossData pd = getPlayerData(userId, bossId);
        long now = System.currentTimeMillis();
        if (pd.cooldownUntil > now) {
            throw new RuntimeException("正在休整，战斗回合越多，休整时间越长!");
        }

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

        // Boss 单位
        int bossHp = state.currentHp;
        BattleCalculator.BattleUnit bossUnit = new BattleCalculator.BattleUnit();
        bossUnit.name = t.name;
        bossUnit.level = t.level;
        bossUnit.totalAttack = t.level * 30;
        bossUnit.totalDefense = t.level * 20;
        bossUnit.valor = t.level * 3;
        bossUnit.command = t.level * 3;
        bossUnit.dodge = 5;
        bossUnit.hit = 10;
        bossUnit.mobility = 20;
        bossUnit.troopType = 1;
        bossUnit.soldierTier = Math.min(10, 1 + t.level / 8);
        bossUnit.soldierCount = bossHp;
        bossUnit.maxSoldierCount = bossHp;
        bossUnit.soldierLife = 500;
        bossUnit.position = 0;

        BattleService.BattleReport report = battleService.fight(sideA, Collections.singletonList(bossUnit), 20);
        int damage = Math.min(bossHp - bossUnit.soldierCount, state.currentHp);
        state.currentHp -= damage;
        pd.killCount++;
        pd.totalDamage += damage;

        int coolSec = 10 + report.totalRounds * 3;
        pd.cooldownUntil = now + coolSec * 1000L;

        boolean killed = false;
        if (state.currentHp <= 0) {
            state.currentHp = 0;
            state.status = "killed";
            state.lastKiller = userId;
            state.lastKillCount = pd.killCount;
            killed = true;
        }

        Map<String, Object> result = new HashMap<>();
        result.put("damage", damage);
        result.put("cooldown", coolSec);
        result.put("currentHp", state.currentHp);
        result.put("killed", killed);
        result.put("myKill", pd.killCount);
        result.put("myTotalDamage", pd.totalDamage);

        if (killed) {
            Map<String, Object> reward = new HashMap<>();
            reward.put("gold", 100 + pd.killCount * 10);
            reward.put("silver", 5000 + pd.killCount * 500);
            result.put("reward", reward);
        }

        return result;
    }

    public Map<String, Object> getRankings(int bossId) {
        List<Map<String, Object>> rankings = new ArrayList<>();
        for (Map.Entry<String, Map<Integer, PlayerBossData>> entry : playerData.entrySet()) {
            PlayerBossData pd = entry.getValue().get(bossId);
            if (pd != null && pd.totalDamage > 0) {
                Map<String, Object> r = new HashMap<>();
                r.put("userId", entry.getKey());
                r.put("playerName", "玩家" + entry.getKey().substring(Math.max(0, entry.getKey().length() - 4)));
                r.put("killCount", pd.killCount);
                r.put("totalDamage", pd.totalDamage);
                rankings.add(r);
            }
        }
        rankings.sort((a, b) -> Long.compare((long) b.get("totalDamage"), (long) a.get("totalDamage")));

        Map<String, Object> result = new HashMap<>();
        result.put("rankings", rankings.subList(0, Math.min(20, rankings.size())));
        return result;
    }

    /**
     * 刷新Boss状态：根据当前时间判断是否在活动窗口内
     * - 窗口开始时：如果Boss不是active，重置为active并恢复满血
     * - 窗口结束后：如果Boss仍是active，标记为escaped（逃遁）
     */
    private void refreshBossState(BossTemplate t, BossState state) {
        TimeWindow tw = getCurrentOrNextWindow(t);
        long now = System.currentTimeMillis();

        if (tw.active) {
            if (!"active".equals(state.status) && !"killed".equals(state.status)) {
                state.currentHp = t.maxHp;
                state.status = "active";
                state.windowStartMs = tw.startMs;
                playerData.values().forEach(m -> m.remove(t.id));
            }
            if ("active".equals(state.status) && state.windowStartMs != tw.startMs) {
                state.currentHp = t.maxHp;
                state.status = "active";
                state.windowStartMs = tw.startMs;
                playerData.values().forEach(m -> m.remove(t.id));
            }
        } else {
            if ("active".equals(state.status)) {
                state.status = "escaped";
            }
        }
    }

    private TimeWindow getCurrentOrNextWindow(BossTemplate t) {
        long now = System.currentTimeMillis();
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(now);

        int curHour = cal.get(Calendar.HOUR_OF_DAY);
        int curMin = cal.get(Calendar.MINUTE);
        double curTime = curHour + curMin / 60.0;

        for (int hour : t.appearHours) {
            double startTime = hour;
            double endTime = hour + DURATION_MINUTES / 60.0;
            if (curTime >= startTime && curTime < endTime) {
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

    private String formatAppearHours(BossTemplate t) {
        StringBuilder sb = new StringBuilder("每天");
        for (int i = 0; i < t.appearHours.length; i++) {
            if (i > 0) sb.append(",");
            sb.append(String.format("%02d:00", t.appearHours[i]));
        }
        return sb.toString();
    }

    private PlayerBossData getPlayerData(String userId, int bossId) {
        return playerData
                .computeIfAbsent(userId, k -> new ConcurrentHashMap<>())
                .computeIfAbsent(bossId, k -> new PlayerBossData());
    }

    static class BossTemplate {
        int id;
        String name;
        int level;
        int maxHp;
        String pic;
        int minLevel;
        int[] appearHours;

        BossTemplate(int id, String name, int level, int maxHp, String pic, int minLevel, int[] appearHours) {
            this.id = id; this.name = name; this.level = level;
            this.maxHp = maxHp; this.pic = pic; this.minLevel = minLevel;
            this.appearHours = appearHours;
        }
    }

    static class BossState {
        int currentHp;
        String status = "waiting";
        String lastKiller = "";
        int lastKillCount = 0;
        long windowStartMs = 0;

        BossState(BossTemplate t) { this.currentHp = t.maxHp; }
    }

    static class PlayerBossData {
        int killCount = 0;
        long totalDamage = 0;
        long cooldownUntil = 0;
    }

    static class TimeWindow {
        boolean active;
        long startMs;
        long endMs;
        long nextAppearMs;

        TimeWindow(boolean active, long startMs, long endMs, long nextAppearMs) {
            this.active = active; this.startMs = startMs;
            this.endMs = endMs; this.nextAppearMs = nextAppearMs;
        }
    }
}
