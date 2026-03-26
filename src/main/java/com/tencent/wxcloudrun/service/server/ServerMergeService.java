package com.tencent.wxcloudrun.service.server;

import com.tencent.wxcloudrun.dao.*;
import com.tencent.wxcloudrun.exception.BusinessException;
import com.tencent.wxcloudrun.service.herorank.HeroRankService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class ServerMergeService {

    private static final Logger logger = LoggerFactory.getLogger(ServerMergeService.class);

    @Autowired private GameServerMapper gameServerMapper;
    @Autowired private HeroRankMapper heroRankMapper;
    @Autowired private HeroRankService heroRankService;
    @Autowired private AllianceMapper allianceMapper;
    @Autowired private ChatMapper chatMapper;
    @Autowired private MarketMapper marketMapper;
    @Autowired private NationWarMapper nationWarMapper;
    @Autowired private SupplyTransportMapper supplyTransportMapper;
    @Autowired private CornucopiaMapper cornucopiaMapper;
    @Autowired private WorldBossMapper worldBossMapper;
    @Autowired private AllianceBossMapper allianceBossMapper;
    @Autowired private ServerMergeMapper mergeMapper;

    /**
     * 将 sourceServerId 合入 targetServerId
     *
     * 流程：
     * 1. 标记源服状态为 merging
     * 2. 迁移 user_server 记录
     * 3. 迁移所有 server_id 隔离的表
     * 4. 重建目标服英雄榜（1000 NPC + 按进入先后排真实玩家）
     * 5. 标记源服状态为 merged
     */
    @Transactional
    public Map<String, Object> mergeServer(int sourceServerId, int targetServerId) {
        Map<String, Object> sourceServer = gameServerMapper.findServerById(sourceServerId);
        Map<String, Object> targetServer = gameServerMapper.findServerById(targetServerId);
        if (sourceServer == null) throw new BusinessException(400, "源区服不存在: " + sourceServerId);
        if (targetServer == null) throw new BusinessException(400, "目标区服不存在: " + targetServerId);
        if (sourceServerId == targetServerId) throw new BusinessException(400, "不能合并到自身");

        String sourceStatus = (String) sourceServer.get("serverStatus");
        if ("merged".equals(sourceStatus)) throw new BusinessException(400, "源区服已被合并");

        logger.info("========== 开始合区: {} -> {} ==========", sourceServerId, targetServerId);

        // 1. 标记源服
        gameServerMapper.updateServerStatus(sourceServerId, "merging");

        // 2. 收集两个服的真实玩家（合并前）
        List<Map<String, Object>> sourcePlayers = heroRankMapper.findRealPlayersByServerId(sourceServerId);
        List<Map<String, Object>> targetPlayers = heroRankMapper.findRealPlayersByServerId(targetServerId);
        logger.info("源服玩家 {} 人, 目标服玩家 {} 人", sourcePlayers.size(), targetPlayers.size());

        // 3. 迁移 server_id 隔离的表
        migrateServerIdTables(sourceServerId, targetServerId);

        // 4. 迁移 user_server（玩家区服记录）
        gameServerMapper.migratePlayerServer(sourceServerId, targetServerId);
        logger.info("user_server 迁移完成");

        // 5. 重建英雄榜
        rebuildHeroRank(targetServerId, sourcePlayers, targetPlayers);

        // 6. 标记源服
        String sourceName = (String) sourceServer.get("serverName");
        String targetName = (String) targetServer.get("serverName");
        gameServerMapper.updateServerStatus(sourceServerId, "merged");
        gameServerMapper.updateServerName(sourceServerId, sourceName + "(已合入" + targetName + ")");

        logger.info("========== 合区完成: {} -> {} ==========", sourceServerId, targetServerId);

        Map<String, Object> result = new HashMap<>();
        result.put("sourceServerId", sourceServerId);
        result.put("targetServerId", targetServerId);
        result.put("mergedPlayers", sourcePlayers.size());
        result.put("totalPlayers", sourcePlayers.size() + targetPlayers.size());
        return result;
    }

    private void migrateServerIdTables(int source, int target) {
        // 联盟：处理重名后迁移
        mergeMapper.renameConflictAlliances(source, target);
        mergeMapper.migrateAlliance(source, target);
        logger.info("alliance 迁移完成");

        // 聊天
        mergeMapper.migrateChat(source, target);
        logger.info("chat_message 迁移完成");

        // 市场（取消源服所有挂牌）
        mergeMapper.cancelMarketListings(source);
        logger.info("market_listing 源服挂牌已取消");

        // 国战（清理源服城池，合并阵营选择）
        mergeMapper.deleteCityOwners(source);
        mergeMapper.migratePlayerNation(source, target);
        logger.info("nation_war 迁移完成");

        // 军需
        mergeMapper.migrateSupplyTransport(source, target);
        logger.info("supply_transport 迁移完成");

        // 聚宝盆（源服期号清理）
        mergeMapper.deleteCornucopiaPeriod(source);
        logger.info("cornucopia_period 源服数据已清理");

        // 世界Boss（清理源服状态）
        mergeMapper.deleteWorldBossState(source);
        mergeMapper.deleteWorldBossDamage(source);
        logger.info("world_boss 源服数据已清理");

        // 联盟Boss（清理源服）
        mergeMapper.deleteAllianceBoss(source);
        mergeMapper.deleteAllianceBossRecord(source);
        logger.info("alliance_boss 源服数据已清理");
    }

    /**
     * 重建英雄榜：
     * 1. 清空目标服英雄榜所有数据
     * 2. 初始化 1000 个 NPC
     * 3. 按进入先后顺序（create_time）插入真实玩家，排在 NPC 前面
     */
    private void rebuildHeroRank(int targetServerId,
                                  List<Map<String, Object>> sourcePlayers,
                                  List<Map<String, Object>> targetPlayers) {
        // 清空目标服英雄榜
        heroRankMapper.deleteByServerId(targetServerId);
        // 也清理源服（如果有残留）
        // source 的 server_id 数据已在 migrateServerIdTables 之前收集，这里直接操作 target
        logger.info("英雄榜已清空, serverId={}", targetServerId);

        // 合并所有真实玩家，按 create_time（进入先后）排序
        List<Map<String, Object>> allPlayers = new ArrayList<>();
        // 从 user_server 获取 create_time 排序
        List<Map<String, Object>> playerServers = gameServerMapper.findPlayerServersByServerId(targetServerId);
        Map<String, Long> createTimeMap = new HashMap<>();
        for (Map<String, Object> ps : playerServers) {
            String rawUserId = String.valueOf(ps.get("userId"));
            String compositeId = rawUserId + "_" + targetServerId;
            long createTime = ps.get("createTime") != null ? ((Number) ps.get("createTime")).longValue() : 0;
            createTimeMap.put(compositeId, createTime);
        }

        // 合并两个服的玩家数据
        Map<String, Map<String, Object>> playerMap = new LinkedHashMap<>();
        for (Map<String, Object> p : targetPlayers) playerMap.put((String) p.get("userId"), p);
        for (Map<String, Object> p : sourcePlayers) playerMap.put((String) p.get("userId"), p);

        // 按 create_time 排序
        allPlayers.addAll(playerMap.values());
        allPlayers.sort((a, b) -> {
            long ta = createTimeMap.getOrDefault(a.get("userId"), Long.MAX_VALUE);
            long tb = createTimeMap.getOrDefault(b.get("userId"), Long.MAX_VALUE);
            return Long.compare(ta, tb);
        });

        logger.info("合区后真实玩家共 {} 人", allPlayers.size());

        // 先插入真实玩家（排名 1 ~ N）
        long now = System.currentTimeMillis();
        int rank = 1;
        for (Map<String, Object> p : allPlayers) {
            String userId = (String) p.get("userId");
            String userName = (String) p.get("userName");
            int level = p.get("level") != null ? ((Number) p.get("level")).intValue() : 1;
            int power = p.get("power") != null ? ((Number) p.get("power")).intValue() : 100;
            long fame = p.get("fame") != null ? ((Number) p.get("fame")).longValue() : 0;
            String nation = p.get("nation") != null ? (String) p.get("nation") : "WEI";
            String rankName = heroRankService.calcPeerage(fame, level);

            heroRankMapper.upsert(userId, userName, level, power, fame, rankName, rank, nation,
                    0, 0, 0, "", 0, 0, 0, 0, 1, "", now, targetServerId);
            rank++;
        }
        logger.info("真实玩家排名完成, 最后排名={}", rank - 1);

        // 再插入 1000 个 NPC（排名从 rank 开始）
        int npcStartRank = rank;
        Random rng = new Random(42 + targetServerId);
        String[] npcNames = {"张角", "董卓", "袁绍", "袁术", "公孙瓒", "刘表", "刘璋", "马腾",
                "孟获", "祝融", "沙摩柯", "兀突骨", "张宝", "张梁", "韩遂", "张鲁",
                "纪灵", "高览", "淳于琼", "蒋干", "于禁", "乐进", "李典", "曹洪",
                "曹仁", "夏侯惇", "夏侯渊", "张辽", "徐晃", "张郃", "许褚", "典韦",
                "关羽", "张飞", "赵云", "马超", "黄忠", "魏延", "姜维", "庞统",
                "诸葛亮", "周瑜", "陆逊", "吕蒙", "甘宁", "太史慈", "孙策", "孙权",
                "曹操", "刘备", "吕布", "司马懿", "郭嘉", "荀彧", "贾诩", "法正"};
        String[] nations = {"WEI", "SHU", "WU"};

        for (int i = 1; i <= 1000; i++) {
            String npcId = String.format("npc_hero_s%d_%05d", targetServerId, i);
            String name = npcNames[rng.nextInt(npcNames.length)];
            int npcLevel = Math.max(1, 50 - i / 25);
            int npcPower = Math.max(50, (int) (800 * Math.pow(0.997, i)) + rng.nextInt(30));
            long npcFame = Math.max(0, 5000 - i * 5L);
            String peerage = heroRankService.calcPeerage(npcFame, npcLevel);
            String nation = nations[rng.nextInt(nations.length)];

            heroRankMapper.upsert(npcId, name, npcLevel, npcPower, npcFame, peerage,
                    npcStartRank + i - 1, nation,
                    0, 0, 0, "", 0, 0, 0, 0, 1, "", now, targetServerId);
        }
        logger.info("NPC 初始化完成, 排名 {} ~ {}", npcStartRank, npcStartRank + 999);
    }
}
