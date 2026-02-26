package com.tencent.wxcloudrun.service.herorank;

import com.tencent.wxcloudrun.dao.HeroRankMapper;
import com.tencent.wxcloudrun.dao.PeerageConfigMapper;
import com.tencent.wxcloudrun.exception.BusinessException;
import com.tencent.wxcloudrun.model.General;
import com.tencent.wxcloudrun.model.UserResource;
import com.tencent.wxcloudrun.service.UserResourceService;
import com.tencent.wxcloudrun.service.formation.FormationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.text.SimpleDateFormat;
import java.util.*;

@Service
public class HeroRankService {

    private static final Logger logger = LoggerFactory.getLogger(HeroRankService.class);

    private static final int MAX_DAILY_CHALLENGE = 20;
    private static final int MAX_PURCHASE = 999;

    // 挑战胜利声望奖励: 第1名500, 第2名450, ... 第10及以后100
    private static final int[] WIN_FAME_BY_RANK = {500, 450, 400, 350, 300, 250, 200, 180, 150, 100};

    // 每日排名奖励: {声望, 白银}
    private static final long[][] DAILY_RANK_REWARDS = {
        {3500, 100000}, {2500, 50000}, {2400, 40000}, {2300, 35000}, {2200, 30000},
        {2100, 28000}, {2000, 26000}, {1900, 24000}, {1800, 22000}, {1700, 20000},
        {1600, 19000}, {1500, 18000}, {1400, 17000}, {1350, 16000}, {1300, 15000},
        {1250, 14000}, {1200, 13000}, {1150, 12000}, {1100, 11000}, {1000, 10000}
    };

    @Autowired
    private HeroRankMapper heroRankMapper;

    @Autowired
    private PeerageConfigMapper peerageConfigMapper;

    @Autowired
    private UserResourceService resourceService;

    @Autowired
    private FormationService formationService;

    private List<Map<String, Object>> peerageConfigs = new ArrayList<>();

    @PostConstruct
    public void init() {
        peerageConfigs = peerageConfigMapper.findAllPeerage();
        logger.info("加载爵位配置 {} 条", peerageConfigs.size());
    }

    /**
     * 获取英雄榜信息（玩家数据 + 分页排行榜）
     */
    public Map<String, Object> getHeroRankInfo(String userId, int page) {
        UserResource res = resourceService.getUserResource(userId);
        int level = res != null && res.getLevel() != null ? res.getLevel() : 1;

        ensureRankEntry(userId, res);

        Map<String, Object> myRank = heroRankMapper.findByUserId(userId);
        resetIfNewDay(myRank, userId);

        int pageSize = 20;
        int offset = page * pageSize;
        List<Map<String, Object>> topList = heroRankMapper.findPage(offset, pageSize);
        int totalCount = heroRankMapper.countAll();

        Map<String, Object> result = new HashMap<>();
        result.put("myRank", myRank);
        result.put("topList", topList);
        result.put("totalCount", totalCount);
        result.put("page", page);
        result.put("pageSize", pageSize);
        result.put("totalPages", (totalCount + pageSize - 1) / pageSize);
        result.put("maxChallenge", MAX_DAILY_CHALLENGE);
        result.put("peerageConfigs", peerageConfigs);
        result.put("fame", res.getFame());
        result.put("rank", res.getRank());
        result.put("level", level);
        return result;
    }

    /**
     * 挑战英雄榜中的玩家
     */
    public Map<String, Object> challenge(String userId, String targetId) {
        if (userId.equals(targetId)) {
            throw new BusinessException(400, "不能挑战自己");
        }

        UserResource myRes = resourceService.getUserResource(userId);
        int myLevel = myRes != null && myRes.getLevel() != null ? myRes.getLevel() : 1;

        ensureRankEntry(userId, myRes);

        Map<String, Object> myRank = heroRankMapper.findByUserId(userId);
        resetIfNewDay(myRank, userId);

        int todayChallenge = getInt(myRank, "todayChallenge", 0);
        int todayPurchased = getInt(myRank, "todayPurchased", 0);
        int totalAllowed = MAX_DAILY_CHALLENGE + todayPurchased;

        if (todayChallenge >= totalAllowed) {
            throw new BusinessException(400, "今日挑战次数已用完，请购买额外次数");
        }

        Map<String, Object> targetRank = heroRankMapper.findByUserId(targetId);
        if (targetRank == null) {
            throw new BusinessException(400, "对手不存在排行榜中");
        }

        // 获取我方战力
        List<General> myGenerals = formationService.getBattleOrder(userId);
        int myPower = myGenerals.stream()
                .mapToInt(g -> g.getAttrValor() != null ? g.getAttrValor() : myLevel * 400)
                .sum();
        if (myPower == 0) myPower = myLevel * 500;

        // 获取对手战力（NPC直接从排行榜取，真实玩家从阵型取）
        boolean isNpc = targetId.startsWith("npc_hero_");
        int targetPower;
        int targetLevel;
        if (isNpc) {
            targetPower = getInt(targetRank, "power", 1000);
            targetLevel = getInt(targetRank, "level", 1);
        } else {
            List<General> targetGenerals = formationService.getBattleOrder(targetId);
            targetPower = targetGenerals.stream()
                    .mapToInt(g -> g.getAttrValor() != null ? g.getAttrValor() : 0)
                    .sum();
            UserResource targetRes = resourceService.getUserResource(targetId);
            targetLevel = targetRes != null && targetRes.getLevel() != null ? targetRes.getLevel() : 1;
            if (targetPower == 0) targetPower = targetLevel * 500;
        }

        double powerRatio = (double) myPower / (myPower + targetPower);
        double winRate = Math.min(Math.max(powerRatio * 1.2, 0.1), 0.95);
        boolean victory = Math.random() < winRate;

        // 计算声望奖励
        long fameGain = 0;
        if (victory) {
            int todayWins = getInt(myRank, "todayWins", 0);
            if (todayWins < WIN_FAME_BY_RANK.length) {
                fameGain = WIN_FAME_BY_RANK[todayWins];
            } else {
                fameGain = 100;
            }
            // 增加声望
            resourceService.addFame(userId, fameGain);

            // 更新胜利次数
            heroRankMapper.upsert(userId,
                    getString(myRank, "userName", "主公"),
                    myLevel, myPower, myRes.getFame() + fameGain,
                    myRes.getRank(),
                    getInt(myRank, "ranking", 0),
                    todayChallenge + 1,
                    todayWins + 1,
                    todayPurchased,
                    today(),
                    System.currentTimeMillis());
        } else {
            heroRankMapper.upsert(userId,
                    getString(myRank, "userName", "主公"),
                    myLevel, myPower, myRes.getFame(),
                    myRes.getRank(),
                    getInt(myRank, "ranking", 0),
                    todayChallenge + 1,
                    getInt(myRank, "todayWins", 0),
                    todayPurchased,
                    today(),
                    System.currentTimeMillis());
        }

        // 更新自己的战力
        syncPower(userId);
        syncPower(targetId);

        // 记录战斗
        heroRankMapper.insertBattle(userId,
                getString(myRank, "userName", "主公"),
                myLevel,
                targetId,
                getString(targetRank, "userName", "对手"),
                getInt(targetRank, "level", 1),
                victory, fameGain,
                System.currentTimeMillis(), today());

        Map<String, Object> result = new HashMap<>();
        result.put("victory", victory);
        result.put("fameGain", fameGain);
        result.put("myPower", myPower);
        result.put("targetPower", targetPower);
        result.put("todayChallenge", todayChallenge + 1);
        result.put("totalAllowed", totalAllowed);
        result.put("targetName", getString(targetRank, "userName", "对手"));
        result.put("targetLevel", getInt(targetRank, "level", 1));
        return result;
    }

    /**
     * 购买挑战次数
     */
    public Map<String, Object> purchaseChallenge(String userId) {
        Map<String, Object> myRank = heroRankMapper.findByUserId(userId);
        if (myRank == null) {
            throw new BusinessException(400, "未在排行榜中");
        }
        resetIfNewDay(myRank, userId);

        int todayPurchased = getInt(myRank, "todayPurchased", 0);

        // 第1~10次 10元宝/次，第11次以后 100元宝/次
        long cost = todayPurchased < 10 ? 10 : 100;

        if (!resourceService.consumeGold(userId, cost)) {
            throw new BusinessException(400, "元宝不足，需要" + cost + "元宝");
        }

        heroRankMapper.upsert(userId,
                getString(myRank, "userName", "主公"),
                getInt(myRank, "level", 1),
                getInt(myRank, "power", 0),
                getLong(myRank, "fame", 0L),
                (String) myRank.get("rankName"),
                getInt(myRank, "ranking", 0),
                getInt(myRank, "todayChallenge", 0),
                getInt(myRank, "todayWins", 0),
                todayPurchased + 1,
                today(),
                System.currentTimeMillis());

        Map<String, Object> result = new HashMap<>();
        result.put("cost", cost);
        result.put("todayPurchased", todayPurchased + 1);
        result.put("nextCost", (todayPurchased + 1) < 10 ? 10 : 100);
        result.put("totalAllowed", MAX_DAILY_CHALLENGE + todayPurchased + 1);
        return result;
    }

    /**
     * 获取挑战记录
     */
    public List<Map<String, Object>> getBattleRecords(String userId) {
        List<Map<String, Object>> records = heroRankMapper.findBattlesByAttacker(userId, 50);
        for (Map<String, Object> r : records) {
            Object v = r.get("victory");
            if (v instanceof Number) {
                r.put("victory", ((Number) v).intValue() == 1);
            }
        }
        return records;
    }

    /**
     * 获取奖励记录
     */
    public List<Map<String, Object>> getRewardRecords(String userId) {
        return heroRankMapper.findRewardLogs(userId, 30);
    }

    /**
     * 同步玩家战力到排行榜（跳过NPC）
     */
    public void syncPower(String userId) {
        if (userId == null || userId.startsWith("npc_hero_")) return;
        try {
            UserResource res = resourceService.getUserResource(userId);
            int level = res != null && res.getLevel() != null ? res.getLevel() : 1;

            List<General> generals = formationService.getBattleOrder(userId);
            int totalPower = generals.stream()
                    .mapToInt(g -> g.getAttrValor() != null ? g.getAttrValor() : level * 400)
                    .sum();
            if (totalPower == 0) totalPower = level * 500;

            Map<String, Object> rank = heroRankMapper.findByUserId(userId);
            if (rank != null) {
                heroRankMapper.upsert(userId,
                        getString(rank, "userName", "主公"),
                        level, totalPower, res.getFame(),
                        res.getRank(),
                        getInt(rank, "ranking", 0),
                        getInt(rank, "todayChallenge", 0),
                        getInt(rank, "todayWins", 0),
                        getInt(rank, "todayPurchased", 0),
                        getString(rank, "lastResetDate", today()),
                        System.currentTimeMillis());
            }
        } catch (Exception e) {
            logger.warn("同步战力失败 userId={}: {}", userId, e.getMessage());
        }
    }

    /**
     * 每日00:00结算排名奖励
     */
    @Scheduled(cron = "0 0 0 * * ?")
    public void dailySettle() {
        logger.info("开始每日英雄榜结算...");
        try {
            List<Map<String, Object>> allPlayers = heroRankMapper.findAllOrderByPower();
            String settleDate = today();

            for (int i = 0; i < allPlayers.size(); i++) {
                Map<String, Object> player = allPlayers.get(i);
                String uid = (String) player.get("userId");
                int ranking = i + 1;

                heroRankMapper.updateRanking(uid, ranking);

                long fameReward;
                long silverReward;
                if (i < DAILY_RANK_REWARDS.length) {
                    fameReward = DAILY_RANK_REWARDS[i][0];
                    silverReward = DAILY_RANK_REWARDS[i][1];
                } else {
                    fameReward = 1000;
                    silverReward = 10000;
                }

                resourceService.addFame(uid, fameReward);
                resourceService.addSilver(uid, silverReward);

                heroRankMapper.insertRewardLog(uid, ranking, fameReward, silverReward,
                        settleDate, System.currentTimeMillis());
            }

            logger.info("英雄榜结算完成，共处理 {} 名玩家", allPlayers.size());
        } catch (Exception e) {
            logger.error("英雄榜每日结算失败", e);
        }
    }

    // ==== 内部方法 ====

    private void ensureRankEntry(String userId, UserResource res) {
        Map<String, Object> rank = heroRankMapper.findByUserId(userId);
        if (rank == null) {
            int level = res != null && res.getLevel() != null ? res.getLevel() : 1;
            List<General> generals = formationService.getBattleOrder(userId);
            int power = generals.stream()
                    .mapToInt(g -> g.getAttrValor() != null ? g.getAttrValor() : level * 400)
                    .sum();
            if (power == 0) power = level * 500;

            String userName = "主公Lv." + level;
            heroRankMapper.upsert(userId, userName, level, power,
                    res.getFame() != null ? res.getFame() : 0,
                    res.getRank() != null ? res.getRank() : "白身",
                    0, 0, 0, 0, today(),
                    System.currentTimeMillis());
        }
    }

    private void resetIfNewDay(Map<String, Object> rank, String userId) {
        String lastDate = getString(rank, "lastResetDate", "");
        if (!today().equals(lastDate)) {
            heroRankMapper.upsert(userId,
                    getString(rank, "userName", "主公"),
                    getInt(rank, "level", 1),
                    getInt(rank, "power", 0),
                    getLong(rank, "fame", 0L),
                    (String) rank.get("rankName"),
                    getInt(rank, "ranking", 0),
                    0, 0, 0, today(),
                    System.currentTimeMillis());
            rank.put("todayChallenge", 0);
            rank.put("todayWins", 0);
            rank.put("todayPurchased", 0);
            rank.put("lastResetDate", today());
        }
    }

    private String today() {
        return new SimpleDateFormat("yyyyMMdd").format(new Date());
    }

    private int getInt(Map<String, Object> m, String key, int def) {
        Object v = m.get(key);
        if (v instanceof Number) return ((Number) v).intValue();
        if (v instanceof String) {
            try { return Integer.parseInt((String) v); } catch (Exception e) { return def; }
        }
        return def;
    }

    private long getLong(Map<String, Object> m, String key, long def) {
        Object v = m.get(key);
        if (v instanceof Number) return ((Number) v).longValue();
        if (v instanceof String) {
            try { return Long.parseLong((String) v); } catch (Exception e) { return def; }
        }
        return def;
    }

    private String getString(Map<String, Object> m, String key, String def) {
        Object v = m.get(key);
        return v != null ? v.toString() : def;
    }
}
