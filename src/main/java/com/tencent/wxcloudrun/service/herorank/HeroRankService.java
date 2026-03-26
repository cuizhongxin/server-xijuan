package com.tencent.wxcloudrun.service.herorank;

import com.tencent.wxcloudrun.config.TacticsConfig;
import com.tencent.wxcloudrun.config.TacticsConfig.TacticsTemplate;
import com.tencent.wxcloudrun.dao.GameServerMapper;
import com.tencent.wxcloudrun.dao.HeroRankMapper;
import com.tencent.wxcloudrun.dao.PeerageConfigMapper;
import com.tencent.wxcloudrun.dao.UserTacticsMapper;
import com.tencent.wxcloudrun.model.General;
import com.tencent.wxcloudrun.model.UserResource;
import com.tencent.wxcloudrun.service.formation.FormationService;
import com.tencent.wxcloudrun.service.general.GeneralService;
import com.tencent.wxcloudrun.service.SuitConfigService;
import com.tencent.wxcloudrun.service.UserResourceService;
import com.tencent.wxcloudrun.service.battle.BattleCalculator;
import com.tencent.wxcloudrun.service.battle.BattleService;
import com.tencent.wxcloudrun.service.nationwar.NationWarService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.text.SimpleDateFormat;
import java.util.*;

/**
 * 英雄榜 V2 — 排名互换制（匹配APK设计）
 *
 * 核心规则:
 *   1. 排名按ranking字段升序，挑战胜利后攻守双方排名互换
 *   2. 每日15次免费挑战，黄金可购买额外次数
 *   3. 每次挑战后15分钟CD，黄金可加速
 *   4. 每日21:00按排名发放奖励（声望+白银+经验），需手动领取
 *   5. 声望累积决定爵位晋升
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class HeroRankService {

    private final HeroRankMapper heroRankMapper;
    private final PeerageConfigMapper peerageConfigMapper;
    private final GameServerMapper gameServerMapper;
    private final BattleService battleService;
    private final FormationService formationService;
    private final GeneralService generalService;
    private final SuitConfigService suitConfigService;
    private final UserResourceService userResourceService;
    private final UserTacticsMapper userTacticsMapper;
    private final TacticsConfig tacticsConfig;
    private final NationWarService nationWarService;

    @org.springframework.beans.factory.annotation.Autowired @org.springframework.context.annotation.Lazy
    private com.tencent.wxcloudrun.service.dailytask.DailyTaskService dailyTaskService;

    private static final int MAX_DAILY_CHALLENGE = 15;
    private static final long CHALLENGE_CD_MS = 15 * 60 * 1000;
    private static final int SPEED_UP_GOLD = 50;
    private static final int NPC_COUNT = 1000;
    private static final String[] NATIONS = {"WEI", "SHU", "WU"};
    private static final ObjectMapper JSON = new ObjectMapper();

    private static final int[][] RANK_REWARDS = {
        {1, 3500, 100000, 50000},
        {2, 2500, 50000, 30000},
        {3, 2400, 40000, 25000},
        {5, 2200, 35000, 20000},
        {10, 2000, 30000, 18000},
        {20, 1800, 25000, 15000},
        {50, 1500, 20000, 12000},
        {100, 1200, 15000, 10000},
        {200, 1000, 12000, 8000},
        {500, 800, 10000, 5000},
        {1000, 500, 5000, 3000},
    };

    private static final int[] WIN_FAME = {500, 450, 400, 350, 300, 260, 220, 180, 140, 100};

    // ==================== 区服ID提取 ====================

    static int extractServerId(String compositeUserId) {
        if (compositeUserId == null) return 1;
        int idx = compositeUserId.lastIndexOf('_');
        if (idx > 0) {
            try { return Integer.parseInt(compositeUserId.substring(idx + 1)); }
            catch (NumberFormatException e) { return 1; }
        }
        return 1;
    }

    // ==================== 初始化NPC ====================

    private static final String[] NPC_NAMES = {"张角", "董卓", "袁绍", "袁术", "公孙瓒", "刘表", "刘璋", "马腾",
        "孟获", "祝融", "沙摩柯", "兀突骨", "张宝", "张梁", "韩遂", "张鲁",
        "纪灵", "高览", "淳于琼", "蒋干", "于禁", "乐进", "李典", "曹洪",
        "曹仁", "夏侯惇", "夏侯渊", "张辽", "徐晃", "张郃", "许褚", "典韦",
        "关羽", "张飞", "赵云", "马超", "黄忠", "魏延", "姜维", "庞统",
        "诸葛亮", "周瑜", "陆逊", "吕蒙", "甘宁", "太史慈", "孙策", "孙权",
        "曹操", "刘备", "吕布", "司马懿", "郭嘉", "荀彧", "贾诩", "法正"};

    /**
     * 为指定区服初始化1000个英雄榜NPC
     * NPC的user_id格式: npc_hero_s{serverId}_{序号}
     */
    public void ensureNpcExists(int serverId) {
        int count = heroRankMapper.countByServerId(serverId);
        if (count >= NPC_COUNT) return;

        log.info("初始化英雄榜NPC: serverId={}, 现有{}条，需{}条", serverId, count, NPC_COUNT);
        Random rng = new Random(42 + serverId);

        long now = System.currentTimeMillis();
        for (int i = count + 1; i <= NPC_COUNT; i++) {
            String npcId = String.format("npc_hero_s%d_%05d", serverId, i);
            String name = NPC_NAMES[rng.nextInt(NPC_NAMES.length)];
            int level = Math.max(1, 50 - i / 25);
            int power = Math.max(50, (int)(800 * Math.pow(0.997, i)) + rng.nextInt(30));
            long fame = Math.max(0, 5000 - i * 5L);
            String peerage = calcPeerage(fame, level);

            String nation = NATIONS[rng.nextInt(NATIONS.length)];
            heroRankMapper.upsert(npcId, name, level, power, fame, peerage, i, nation,
                0, 0, 0, "", 0, 0, 0, 0, 1, "", now, serverId);
        }
        log.info("英雄榜NPC初始化完成 serverId={}, 共{}条", serverId, NPC_COUNT);
    }

    /** 兼容旧调用：从默认服1初始化 */
    public void ensureNpcExists() {
        ensureNpcExists(1);
    }

    // ==================== 查询 ====================

    private static final int DISPLAY_COUNT = 10;

    public Map<String, Object> getInfo(String userId) {
        int serverId = extractServerId(userId);
        ensureNpcExists(serverId);
        ensurePlayerEntry(userId);
        resetIfNewDay(userId);

        Map<String, Object> me = heroRankMapper.findByUserId(userId);
        int myRanking = getInt(me, "ranking");
        int total = heroRankMapper.countByServerId(serverId);

        List<Map<String, Object>> list;
        if (myRanking <= DISPLAY_COUNT) {
            list = heroRankMapper.findByRanking(serverId, 0, DISPLAY_COUNT);
        } else {
            int minRank = Math.max(1, (int)(myRanking * 0.8));
            list = heroRankMapper.findRandomInRange(serverId, minRank, myRanking, userId, DISPLAY_COUNT);
        }

        long lastTime = getLong(me, "lastChallengeTime");
        long cdRemain = Math.max(0, CHALLENGE_CD_MS - (System.currentTimeMillis() - lastTime));

        Map<String, Object> result = new HashMap<>();
        result.put("myInfo", me);
        result.put("rankings", list);
        result.put("total", total);
        result.put("maxDaily", MAX_DAILY_CHALLENGE);
        result.put("cdRemainMs", cdRemain);
        result.put("cdTotalMs", CHALLENGE_CD_MS);
        result.put("speedUpGold", SPEED_UP_GOLD);

        return result;
    }

    // ==================== 挑战（排名互换制） ====================

    public Map<String, Object> challenge(String userId, String targetId) {
        if (userId.equals(targetId)) throw new RuntimeException("不能挑战自己");

        ensurePlayerEntry(userId);
        resetIfNewDay(userId);

        Map<String, Object> me = heroRankMapper.findByUserId(userId);
        Map<String, Object> target = heroRankMapper.findByUserId(targetId);
        if (target == null) throw new RuntimeException("对手不存在");

        int todayChallenge = getInt(me, "todayChallenge");
        int todayPurchased = getInt(me, "todayPurchased");
        if (todayChallenge >= MAX_DAILY_CHALLENGE + todayPurchased) {
            throw new RuntimeException("今日挑战次数已用完");
        }

        long lastTime = getLong(me, "lastChallengeTime");
        long now = System.currentTimeMillis();
        if (now - lastTime < CHALLENGE_CD_MS) {
            throw new RuntimeException("挑战冷却中");
        }

        int myRank = getInt(me, "ranking");
        int targetRank = getInt(target, "ranking");
        if (myRank <= targetRank) {
            throw new RuntimeException("只能挑战排名高于自己的对手");
        }

        List<BattleCalculator.BattleUnit> sideA = formationService.buildPlayerBattleUnits(userId);
        List<BattleCalculator.BattleUnit> sideB;
        if (targetId.startsWith("npc_hero_")) {
            sideB = buildNpcUnits(target, sideA.size());
        } else {
            sideB = formationService.buildPlayerBattleUnits(targetId);
            if (sideB.isEmpty()) sideB = buildNpcUnits(target, sideA.size());
        }

        if (sideA.isEmpty()) throw new RuntimeException("请先配置阵型");

        BattleService.BattleReport report = battleService.fight(sideA, sideB, 20);
        boolean victory = report.victoryA;

        int myNewRank = myRank;
        int targetNewRank = targetRank;
        long fameGain = 0;

        if (victory) {
            myNewRank = targetRank;
            targetNewRank = myRank;
            heroRankMapper.swapRanking(userId, myRank, targetId, targetRank, now);

            int todayWins = getInt(me, "todayWins") + 1;
            int idx = Math.min(todayWins - 1, WIN_FAME.length - 1);
            fameGain = WIN_FAME[idx];

            long currentFame = getLong(me, "fame") + fameGain;
            String newPeerage = calcPeerage(currentFame, getInt(me, "level"));

            heroRankMapper.upsert(userId, str(me, "userName"), getInt(me, "level"),
                getInt(me, "power"), currentFame, newPeerage, myNewRank, str(me, "nation"),
                todayChallenge + 1, todayWins, todayPurchased,
                str(me, "lastResetDate"), now,
                getLong(me, "pendingFame"), getLong(me, "pendingSilver"),
                getLong(me, "pendingExp"), getInt(me, "rewardClaimed"),
                str(me, "settleDate"), now, extractServerId(userId));
        } else {
            heroRankMapper.upsert(userId, str(me, "userName"), getInt(me, "level"),
                getInt(me, "power"), getLong(me, "fame"), str(me, "rankName"), myRank, str(me, "nation"),
                todayChallenge + 1, getInt(me, "todayWins"), todayPurchased,
                str(me, "lastResetDate"), now,
                getLong(me, "pendingFame"), getLong(me, "pendingSilver"),
                getLong(me, "pendingExp"), getInt(me, "rewardClaimed"),
                str(me, "settleDate"), now, extractServerId(userId));
        }

        String today = new SimpleDateFormat("yyyyMMdd").format(new Date());
        String reportJson = null;
        try {
            reportJson = JSON.writeValueAsString(report);
        } catch (Exception e) {
            log.warn("序列化战报失败: {}", e.getMessage());
        }
        heroRankMapper.insertBattle(
            userId, str(me, "userName"), getInt(me, "level"),
            targetId, str(target, "userName"), getInt(target, "level"),
            victory, fameGain,
            myRank, myNewRank, targetRank, targetNewRank,
            reportJson, now, today);

        syncPower(userId);
        dailyTaskService.incrementTask(userId, "herorank");

        Map<String, Object> result = new HashMap<>();
        result.put("victory", victory);
        result.put("fameGain", fameGain);
        result.put("myOldRank", myRank);
        result.put("myNewRank", myNewRank);
        result.put("targetOldRank", targetRank);
        result.put("targetNewRank", targetNewRank);
        result.put("targetName", str(target, "userName"));
        result.put("todayChallenge", todayChallenge + 1);
        result.put("maxDaily", MAX_DAILY_CHALLENGE + todayPurchased);
        result.put("cdRemainMs", CHALLENGE_CD_MS);
        try {
            result.put("battleReport", JSON.convertValue(report, Map.class));
        } catch (Exception e) {
            log.warn("战报转Map失败: {}", e.getMessage());
            result.put("battleReport", null);
        }
        return result;
    }

    // ==================== 加速挑战（跳过CD） ====================

    public Map<String, Object> speedUp(String userId) {
        Map<String, Object> me = heroRankMapper.findByUserId(userId);
        if (me == null) throw new RuntimeException("未加入英雄榜");

        long lastTime = getLong(me, "lastChallengeTime");
        long remain = CHALLENGE_CD_MS - (System.currentTimeMillis() - lastTime);
        if (remain <= 0) throw new RuntimeException("无需加速");

        UserResource res = userResourceService.getUserResource(userId);
        long gold = res.getGold() != null ? res.getGold() : 0;
        if (gold < SPEED_UP_GOLD) throw new RuntimeException("黄金不足");

        res.setGold(gold - SPEED_UP_GOLD);
        userResourceService.saveUserResource(res);

        heroRankMapper.upsert(userId, str(me, "userName"), getInt(me, "level"),
            getInt(me, "power"), getLong(me, "fame"), str(me, "rankName"),
            getInt(me, "ranking"), str(me, "nation"),
            getInt(me, "todayChallenge"), getInt(me, "todayWins"),
            getInt(me, "todayPurchased"), str(me, "lastResetDate"), 0,
            getLong(me, "pendingFame"), getLong(me, "pendingSilver"),
            getLong(me, "pendingExp"), getInt(me, "rewardClaimed"),
            str(me, "settleDate"), System.currentTimeMillis(), extractServerId(userId));

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("goldCost", SPEED_UP_GOLD);
        result.put("remainGold", gold - SPEED_UP_GOLD);
        return result;
    }

    // ==================== 领取奖励 ====================

    public Map<String, Object> claimReward(String userId) {
        Map<String, Object> me = heroRankMapper.findByUserId(userId);
        if (me == null) throw new RuntimeException("未加入英雄榜");
        if (getInt(me, "rewardClaimed") == 1) throw new RuntimeException("奖励已领取");

        long fame = getLong(me, "pendingFame");
        long silver = getLong(me, "pendingSilver");
        long exp = getLong(me, "pendingExp");

        UserResource res = userResourceService.getUserResource(userId);
        res.setSilver(res.getSilver() + silver);
        userResourceService.saveUserResource(res);

        long newFame = getLong(me, "fame") + fame;
        String newPeerage = calcPeerage(newFame, getInt(me, "level"));

        heroRankMapper.upsert(userId, str(me, "userName"), getInt(me, "level"),
            getInt(me, "power"), newFame, newPeerage, getInt(me, "ranking"), str(me, "nation"),
            getInt(me, "todayChallenge"), getInt(me, "todayWins"),
            getInt(me, "todayPurchased"), str(me, "lastResetDate"),
            getLong(me, "lastChallengeTime"),
            0, 0, 0, 1, str(me, "settleDate"),
            System.currentTimeMillis(), extractServerId(userId));

        Map<String, Object> result = new HashMap<>();
        result.put("fame", fame);
        result.put("silver", silver);
        result.put("exp", exp);
        result.put("totalFame", newFame);
        result.put("peerage", newPeerage);
        return result;
    }

    // ==================== 战报 ====================

    public List<Map<String, Object>> getRecords(String userId) {
        return heroRankMapper.findBattlesByUser(userId, 30);
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> getBattleReport(long battleId) {
        Map<String, Object> row = heroRankMapper.findBattleReportById(battleId);
        if (row == null) throw new RuntimeException("战报不存在");
        String json = str(row, "battleReport");
        if (json == null || json.isEmpty()) throw new RuntimeException("战报数据为空");
        try {
            return JSON.readValue(json, Map.class);
        } catch (Exception e) {
            throw new RuntimeException("战报解析失败");
        }
    }

    // ==================== 每日结算（21:00） ====================

    @Scheduled(cron = "0 0 0 * * ?")
    public void dailySettle() {
        log.info("[英雄榜] 开始每日结算（按区服）");
        List<Map<String, Object>> servers = gameServerMapper.findAllServers();
        String today = new SimpleDateFormat("yyyyMMdd").format(new Date());
        long now = System.currentTimeMillis();

        for (Map<String, Object> server : servers) {
            int sid = ((Number) server.get("id")).intValue();
            List<Map<String, Object>> all = heroRankMapper.findAllOrderByRanking(sid);

            for (int i = 0; i < all.size(); i++) {
                Map<String, Object> entry = all.get(i);
                String uid = str(entry, "userId");
                int rank = i + 1;
                heroRankMapper.updateRanking(uid, rank, now);

                int[] reward = getRewardForRank(rank);
                heroRankMapper.setPendingReward(uid, reward[0], reward[1], reward[2], today);

                if (!uid.startsWith("npc_hero_")) {
                    heroRankMapper.insertRewardLog(uid, rank, reward[0], reward[1], today, now);
                }
            }
            log.info("[英雄榜] serverId={} 结算完成，处理{}条", sid, all.size());
        }
    }

    // ==================== 同步战力 ====================

    public void syncPower(String userId) {
        if (userId.startsWith("npc_hero_")) return;

        Map<String, Object> me = heroRankMapper.findByUserId(userId);
        if (me == null) return;

        int totalPower = 0;
        try {
            List<String> ids = formationService.getFormationGeneralIds(userId);
            for (String gid : ids) {
                General g = generalService.getGeneralById(gid);
                if (g == null) continue;
                int atk = g.getAttrAttack() != null ? g.getAttrAttack() : 0;
                int def = g.getAttrDefense() != null ? g.getAttrDefense() : 0;
                int val = g.getAttrValor() != null ? g.getAttrValor() : 0;
                int cmd = g.getAttrCommand() != null ? g.getAttrCommand() : 0;
                totalPower += atk + def + val + cmd;

                Map<String, Integer> eq = suitConfigService.calculateTotalEquipBonus(gid);
                totalPower += eq.getOrDefault("attack", 0) + eq.getOrDefault("defense", 0);
            }
        } catch (Exception e) {
            log.warn("同步战力异常: {}", e.getMessage());
        }
        if (totalPower == 0) {
            totalPower = getInt(me, "level") * 500;
        }

        heroRankMapper.upsert(userId, str(me, "userName"), getInt(me, "level"),
            totalPower, getLong(me, "fame"), str(me, "rankName"), getInt(me, "ranking"), str(me, "nation"),
            getInt(me, "todayChallenge"), getInt(me, "todayWins"),
            getInt(me, "todayPurchased"), str(me, "lastResetDate"),
            getLong(me, "lastChallengeTime"),
            getLong(me, "pendingFame"), getLong(me, "pendingSilver"),
            getLong(me, "pendingExp"), getInt(me, "rewardClaimed"),
            str(me, "settleDate"), System.currentTimeMillis(), extractServerId(userId));
    }

    // ==================== 内部方法 ====================

    private void ensurePlayerEntry(String userId) {
        if (userId.startsWith("npc_hero_")) return;
        Map<String, Object> me = heroRankMapper.findByUserId(userId);
        if (me != null) return;

        int serverId = extractServerId(userId);
        String name = getPlayerName(userId);
        String nation = getPlayerNation(userId);
        UserResource res = userResourceService.getUserResource(userId);
        int level = res.getLevel() != null ? res.getLevel() : 1;
        int power = level * 500;
        int ranking = heroRankMapper.countByServerId(serverId) + 1;

        heroRankMapper.upsert(userId, name, level, power, 0, "平民", ranking, nation,
            0, 0, 0, "", 0, 0, 0, 0, 1, "", System.currentTimeMillis(), serverId);

        syncPower(userId);
    }

    private String getPlayerName(String userId) {
        try {
            List<Map<String, Object>> servers = gameServerMapper.findPlayerServers(userId);
            if (servers != null && !servers.isEmpty()) {
                Object lordName = servers.get(0).get("lordName");
                if (lordName != null && !lordName.toString().isEmpty()) {
                    return lordName.toString();
                }
            }
        } catch (Exception e) {
            log.warn("获取玩家名称失败: {}", e.getMessage());
        }
        return "君主";
    }

    private String getPlayerNation(String userId) {
        try {
            String nation = nationWarService.getPlayerNation(userId);
            if (nation != null && !nation.isEmpty()) return nation;
        } catch (Exception e) {
            log.warn("获取玩家国家失败: {}", e.getMessage());
        }
        return "WEI";
    }

    private void resetIfNewDay(String userId) {
        Map<String, Object> me = heroRankMapper.findByUserId(userId);
        if (me == null) return;
        String today = new SimpleDateFormat("yyyyMMdd").format(new Date());
        String lastDate = str(me, "lastResetDate");
        if (today.equals(lastDate)) return;

        heroRankMapper.upsert(userId, str(me, "userName"), getInt(me, "level"),
            getInt(me, "power"), getLong(me, "fame"), str(me, "rankName"),
            getInt(me, "ranking"), str(me, "nation"),
            0, 0, 0, today, 0,
            getLong(me, "pendingFame"), getLong(me, "pendingSilver"),
            getLong(me, "pendingExp"), getInt(me, "rewardClaimed"),
            str(me, "settleDate"), System.currentTimeMillis(), extractServerId(userId));
    }

    public String calcPeerage(long fame, int level) {
        List<Map<String, Object>> configs = peerageConfigMapper.findAllPeerage();
        String result = "平民";
        for (Map<String, Object> cfg : configs) {
            long fameReq = ((Number) cfg.get("fameRequired")).longValue();
            int levelReq = ((Number) cfg.get("levelRequired")).intValue();
            if (fame >= fameReq && level >= levelReq) {
                result = (String) cfg.get("rankName");
            }
        }
        return result;
    }

    private List<BattleCalculator.BattleUnit> buildPlayerUnits(String userId) {
        List<BattleCalculator.BattleUnit> units = new ArrayList<>();
        try {
            List<String> ids = formationService.getFormationGeneralIds(userId);
            for (String gid : ids) {
                General g = generalService.getGeneralById(gid);
                if (g == null) continue;

                Map<String, Integer> eq = suitConfigService.calculateTotalEquipBonus(gid);
                int lvl = g.getLevel() != null ? g.getLevel() : 1;
                int rawTier = g.getSoldierTier() != null ? g.getSoldierTier() : 1;
                int sRank = g.getSoldierRank() != null ? g.getSoldierRank() : 1;
                int tier = Math.max(rawTier, sRank);
                int troopType = BattleCalculator.parseTroopType(g.getTroopType());
                int maxSoldiers = g.getSoldierMaxCount() != null ? g.getSoldierMaxCount() : 100;
                int formLv = BattleCalculator.maxPeopleToFormationLevel(maxSoldiers);

                BattleCalculator.BattleUnit u = BattleCalculator.assembleBattleUnit(
                    g.getName(), lvl,
                    g.getAttrAttack() != null ? g.getAttrAttack() : 100,
                    g.getAttrDefense() != null ? g.getAttrDefense() : 50,
                    g.getAttrValor() != null ? g.getAttrValor() : 10,
                    g.getAttrCommand() != null ? g.getAttrCommand() : 10,
                    g.getAttrDodge() != null ? (int) Math.round(g.getAttrDodge()) : 5,
                    g.getAttrMobility() != null ? g.getAttrMobility() : 15,
                    troopType, tier, maxSoldiers, maxSoldiers, formLv,
                    eq.getOrDefault("attack", 0), eq.getOrDefault("defense", 0),
                    eq.getOrDefault("speed", 0), eq.getOrDefault("hit", 0),
                    eq.getOrDefault("dodge", 0), 0, 0, 0);

                u.position = units.size();
                if (g.getTacticsId() != null) {
                    TacticsTemplate tt = tacticsConfig.getById(g.getTacticsId());
                    if (tt != null) {
                        Map<String, Object> owned = userTacticsMapper.findByUserIdAndTacticsId(
                            g.getUserId(), g.getTacticsId());
                        int tLv = owned != null ? ((Number) owned.get("level")).intValue() : 1;
                        u.tacticsId = tt.getId();
                        u.tacticsName = tt.getName();
                        u.tacticsLevel = tLv;
                        u.tacticsEffectValue = TacticsConfig.calcEffect(tt, tLv);
                        u.tacticsTriggerRate = TacticsConfig.calcTriggerRate(tt, tLv);
                    }
                }
                units.add(u);
            }
        } catch (Exception e) {
            log.warn("构建玩家战斗单位异常: {}", e.getMessage());
        }
        return units;
    }

    private List<BattleCalculator.BattleUnit> buildNpcUnits(Map<String, Object> npc, int count) {
        List<BattleCalculator.BattleUnit> units = new ArrayList<>();
        int power = getInt(npc, "power");
        int level = getInt(npc, "level");
        if (count <= 0) count = 3;
        String[] types = {"步", "骑", "弓"};

        for (int i = 0; i < count; i++) {
            String troopCat = types[i % 3];
            int troopType = BattleCalculator.parseTroopType(troopCat);
            int tier = Math.max(1, Math.min(10, 1 + level / 20));
            int formLv = BattleCalculator.levelToFormationLevel(level);
            int maxSoldiers = BattleCalculator.getFormationMaxPeople(formLv);

            int eAtk = power / count / 2 + level * 5;
            int eDef = power / count / 3 + level * 3;

            String unitName = count > 1
                ? str(npc, "userName") + "[" + troopCat + (i + 1) + "]"
                : str(npc, "userName");
            BattleCalculator.BattleUnit u = BattleCalculator.assembleBattleUnit(
                unitName, level,
                eAtk, eDef, level * 2, level, 5 + level / 10, 15 + level / 5,
                troopType, tier, maxSoldiers, maxSoldiers, formLv,
                0, 0, 0, 0, 0, 0, 0, 0);
            u.position = i;
            units.add(u);
        }
        return units;
    }

    private int[] getRewardForRank(int rank) {
        for (int[] r : RANK_REWARDS) {
            if (rank <= r[0]) return new int[]{r[1], r[2], r[3]};
        }
        return new int[]{500, 5000, 3000};
    }

    private static int getInt(Map<String, Object> m, String k) {
        Object v = m.get(k);
        return v instanceof Number ? ((Number) v).intValue() : 0;
    }
    private static long getLong(Map<String, Object> m, String k) {
        Object v = m.get(k);
        return v instanceof Number ? ((Number) v).longValue() : 0;
    }
    private static String str(Map<String, Object> m, String k) {
        Object v = m.get(k);
        return v != null ? v.toString() : "";
    }
}
