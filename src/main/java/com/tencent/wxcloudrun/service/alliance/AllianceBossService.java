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
import com.tencent.wxcloudrun.service.PlayerNameResolver;
import com.tencent.wxcloudrun.service.chat.ChatService;
import com.tencent.wxcloudrun.service.mail.MailService;
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

    private static final int FEED_COST_GOLD = 100;
    private static final int MIN_FEED_QUALITY = 2;
    private static final int[] QUALITY_FEED_VALUES = {0, 0, 1, 3, 8, 15, 20};
    private static final long ALLIANCE_BOSS_HP = 30_000L;
    private static final int SUMMON_MIN_HOUR = 0;
    private static final int SUMMON_MIN_MINUTE = 0;
    private static final int SUMMON_MAX_HOUR = 23;
    private static final int SUMMON_MAX_MINUTE = 59;
    private static final int SQUAD_SIZE = 6;
    private static final int SQUAD_SOLDIERS_PER_UNIT = 1000;
    private static final int BASE_COOLDOWN_SEC = 90;
    private static final double DROP_RATE = 0.20;
    private static final long LAST_HIT_REWARD_GOLD = 0L;
    private static final long LAST_HIT_REWARD_SILVER = 0L;
    private static final String SETTLE_MARK_FEED = "settle_feed_rank";
    private static final String SETTLE_MARK_KILL = "settle_kill_rank";
    private static final String BOUND_GOLD_ITEM_NAME = "绑金";
    private static final String MID_RECRUIT_TOKEN_NAME = "中级招贤令";
    private static final String ENHANCE_STONE_1_NAME = "1级强化石";

    private static final List<Map<String, Object>> RANK_REWARD_TOP3 = new ArrayList<>();
    static {
        RANK_REWARD_TOP3.add(rankReward(1, "绑金", 30));
        RANK_REWARD_TOP3.add(rankReward(2, "中级招贤令", 1));
        RANK_REWARD_TOP3.add(rankReward(3, "1级强化石", 5));
    }

    private static final int[] TROOP_LAYOUT = {1, 1, 3, 2, 3, 1};

    private static final String[][] DROP_TABLE = {
            {"15012", "中级招贤令", "0.30"},
            {"15042", "特训符",     "0.20"},
            {"11104", "招财符",     "0.20"},
            {"15052", "军需令",     "0.20"},
            {"14001", "1级强化石",  "0.10"}
    };

    private static final String[][] BOSS_TABLE = {
            {"远古巨兽", String.valueOf(ALLIANCE_BOSS_HP)},
            {"蛮荒凶兽", String.valueOf(ALLIANCE_BOSS_HP)},
            {"上古魔龙", String.valueOf(ALLIANCE_BOSS_HP)},
            {"混沌巨龙", String.valueOf(ALLIANCE_BOSS_HP)},
            {"灭世龙王", String.valueOf(ALLIANCE_BOSS_HP)}
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
    @Autowired @org.springframework.context.annotation.Lazy private ChatService chatService;
    @Autowired private PlayerNameResolver playerNameResolver;
    @Autowired @org.springframework.context.annotation.Lazy private MailService mailService;

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
        int dailyCount = bossMapper.findUserDailyAttackCount(userId);
        Map<String, Object> boss = bossMapper.findCurrentBossByServerId(serverId);
        if (boss == null) {
            ensureBossExists(serverId);
            boss = bossMapper.findCurrentBossByServerId(serverId);
        }
        if (boss == null) {
            throw new BusinessException(500, "Boss数据异常");
        }
        reconcileBossHpConfig(boss);
        boss.put("summonTime", String.format("%02d:%02d-%02d:%02d",
                SUMMON_MIN_HOUR, SUMMON_MIN_MINUTE, SUMMON_MAX_HOUR, SUMMON_MAX_MINUTE));
        int feedCount = boss.get("feedCount") != null ? ((Number) boss.get("feedCount")).intValue() : 0;
        int feedTarget = boss.get("feedTarget") != null ? ((Number) boss.get("feedTarget")).intValue() : 100;
        boss.put("feedFull", feedCount >= feedTarget);
        boss.put("dailyAttacksUsed", dailyCount);
        boss.put("maxDailyAttacks", -1);
        boss.put("attacksLeft", -1);
        boss.put("feedPrize", buildRankPrizePayload("喂养排行奖励"));
        boss.put("killPrize", buildRankPrizePayload("击败排行奖励"));
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
        reconcileBossHpConfig(boss);

        String status = (String) boss.get("status");
        if (!"fighting".equals(status)) {
            throw new BusinessException(400, "Boss未处于战斗状态，请先召唤");
        }

        int dailyCount = bossMapper.findUserDailyAttackCount(userId);

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
        long rewardGold = LAST_HIT_REWARD_GOLD;
        long rewardSilver = LAST_HIT_REWARD_SILVER;

        if (killed) {
            pool.clear();
            settleRankRewards(serverId);

            int nextLevel = Math.min(bossLevel + 1, BOSS_TABLE.length);
            int idx = nextLevel - 1;
            bossMapper.resetBoss(bossId, nextLevel, BOSS_TABLE[idx][0],
                    Long.parseLong(BOSS_TABLE[idx][1]), Long.parseLong(BOSS_TABLE[idx][1]));

            logger.info("联盟Boss被击杀! serverId={}, 升级到Lv.{} {}", serverId, nextLevel, BOSS_TABLE[idx][0]);
            announceBossKilled(serverId, userId);
        }

        Map<String, Object> dropResult = null;
        if (squadWiped && random.nextDouble() < DROP_RATE) {
            dropResult = rollDrop(userId);
            announceBossDrop(serverId, userId, dropResult);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("damage", hpDamage);
        result.put("remainingHp", newHp);
        result.put("maxHp", maxHp);
        result.put("killed", killed);
        result.put("dailyAttacksUsed", dailyCount + 1);
        result.put("maxDailyAttacks", -1);
        result.put("attacksLeft", -1);
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

    /**
     * 在线修正联盟Boss血量配置：
     * - maxHp 永远按当前等级表值
     * - currentHp 只向上限收敛，不主动回血
     */
    private void reconcileBossHpConfig(Map<String, Object> boss) {
        if (boss == null) return;
        Object idObj = boss.get("id");
        Object levelObj = boss.get("bossLevel");
        Object maxHpObj = boss.get("maxHp");
        Object currentHpObj = boss.get("currentHp");
        if (!(idObj instanceof Number) || !(levelObj instanceof Number)
                || !(maxHpObj instanceof Number) || !(currentHpObj instanceof Number)) {
            return;
        }

        int level = ((Number) levelObj).intValue();
        int idx = Math.max(0, Math.min(BOSS_TABLE.length - 1, level - 1));
        long expectedMaxHp = Long.parseLong(BOSS_TABLE[idx][1]);
        long currentMaxHp = ((Number) maxHpObj).longValue();
        long currentHp = ((Number) currentHpObj).longValue();
        if (currentMaxHp == expectedMaxHp) return;

        long fixedCurrentHp = Math.min(currentHp, expectedMaxHp);
        long bossId = ((Number) idObj).longValue();
        bossMapper.updateBossHpConfig(bossId, expectedMaxHp, fixedCurrentHp);
        boss.put("maxHp", expectedMaxHp);
        boss.put("currentHp", fixedCurrentHp);
        logger.info("联盟Boss血量配置已修正: id={}, level={}, maxHp {} -> {}, currentHp -> {}",
                bossId, level, currentMaxHp, expectedMaxHp, fixedCurrentHp);
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

    private void announceBossKilled(int serverId, String userId) {
        try {
            String playerName = resolvePlayerName(userId);
            String msg = "【联盟Boss】玩家【" + playerName + "】完成最后一击！最后一击无额外奖励，排行奖励请在结算后查看。";
            chatService.sendSystemMessage(serverId, "world", msg);
        } catch (Exception e) {
            logger.warn("联盟Boss击败公告发送失败", e);
        }
    }

    private void announceBossDrop(int serverId, String userId, Map<String, Object> dropResult) {
        if (dropResult == null) return;
        try {
            String itemName = String.valueOf(dropResult.get("itemName"));
            int count = dropResult.get("count") instanceof Number ? ((Number) dropResult.get("count")).intValue() : 1;
            if (itemName == null || itemName.trim().isEmpty() || "null".equals(itemName)) return;
            String playerName = resolvePlayerName(userId);
            String msg = "【联盟Boss掉落】玩家【" + playerName + "】获得【" + itemName + "x" + count + "】";
            chatService.sendSystemMessage(serverId, "world", msg);
        } catch (Exception e) {
            logger.warn("联盟Boss掉落公告发送失败", e);
        }
    }

    private String resolvePlayerName(String userId) {
        if (userId == null || userId.isEmpty()) return "未知君主";
        try {
            String name = playerNameResolver.resolve(userId);
            if (name != null && !name.trim().isEmpty() && !"君主".equals(name)) {
                return name;
            }
        } catch (Exception ignore) {
        }
        return userId;
    }

    public List<Map<String, Object>> getRecords(String userId) {
        int serverId = extractServerId(userId);
        return bossMapper.findRecordsByServerId(serverId, 50);
    }

    public List<Map<String, Object>> getRankings(String userId) {
        int serverId = extractServerId(userId);
        return bossMapper.findDailyAttackRankingsByServerId(serverId, 20);
    }

    private static Map<String, Object> rankReward(int rank, String itemName, int count) {
        Map<String, Object> m = new HashMap<>();
        m.put("rank", rank);
        m.put("itemName", itemName);
        m.put("count", count);
        return m;
    }

    private void settleRankRewards(int serverId) {
        try {
            settleOneRanking(serverId, true);
            settleOneRanking(serverId, false);
        } catch (Exception e) {
            logger.warn("联盟Boss排行结算异常, serverId={}", serverId, e);
        }
    }

    private void settleOneRanking(int serverId, boolean feedRank) {
        String settleMark = feedRank ? SETTLE_MARK_FEED : SETTLE_MARK_KILL;
        if (bossMapper.countActionByServerIdToday(serverId, settleMark) > 0) {
            return;
        }

        List<Map<String, Object>> rankings = feedRank
                ? bossMapper.findDailyFeedRankingsByServerId(serverId, 3)
                : bossMapper.findDailyAttackRankingsByServerId(serverId, 3);
        if (rankings == null || rankings.isEmpty()) {
            bossMapper.insertRecord("SYSTEM", settleMark, 0, 0, serverId);
            return;
        }

        for (int i = 0; i < Math.min(3, rankings.size()); i++) {
            Map<String, Object> row = rankings.get(i);
            if (row == null || row.get("userId") == null) continue;
            String userId = String.valueOf(row.get("userId"));
            Map<String, Object> reward = RANK_REWARD_TOP3.get(i);
            int rank = ((Number) reward.get("rank")).intValue();
            String itemName = String.valueOf(reward.get("itemName"));
            int count = ((Number) reward.get("count")).intValue();

            List<Map<String, Object>> attachments = buildRankRewardAttachments(itemName, count);
            String title = feedRank ? "联盟Boss喂养排行奖励" : "联盟Boss击败排行奖励";
            String content = "恭喜主公在" + (feedRank ? "喂养排行" : "击败排行")
                    + "中获得第" + rank + "名，奖励：" + itemName + "x" + count + "。";
            mailService.sendSystemMail(userId, title, content, attachments);
        }

        bossMapper.insertRecord("SYSTEM", settleMark, 0, 0, serverId);
        logger.info("联盟Boss{}排行奖励结算完成, serverId={}", feedRank ? "喂养" : "击败", serverId);
    }

    private List<Map<String, Object>> buildRankRewardAttachments(String itemName, int count) {
        List<Map<String, Object>> list = new ArrayList<>();
        if (count <= 0) return list;
        Map<String, Object> att = new LinkedHashMap<>();
        if (BOUND_GOLD_ITEM_NAME.equals(itemName)) {
            att.put("itemType", "boundGold");
            att.put("itemId", "0");
        } else if (MID_RECRUIT_TOKEN_NAME.equals(itemName)) {
            att.put("itemType", "item");
            att.put("itemId", "15012");
        } else if (ENHANCE_STONE_1_NAME.equals(itemName)) {
            att.put("itemType", "item");
            att.put("itemId", "14001");
        } else {
            att.put("itemType", "item");
            att.put("itemId", "0");
        }
        att.put("itemName", itemName);
        att.put("itemQuality", "");
        att.put("count", count);
        att.put("itemCount", count);
        att.put("claimed", 0);
        list.add(att);
        return list;
    }

    private Map<String, Object> buildRankPrizePayload(String title) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("title", title);
        payload.put("canGet", false);
        payload.put("items", RANK_REWARD_TOP3);
        return payload;
    }
}
