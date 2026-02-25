package com.tencent.wxcloudrun.config;

import com.tencent.wxcloudrun.dao.PlunderNpcMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 掠夺系统配置 - 从数据库加载NPC数据
 */
@Component
public class PlunderConfig {

    private static final Logger logger = LoggerFactory.getLogger(PlunderConfig.class);

    public static final int DAILY_PLUNDER_LIMIT = 20;
    public static final int MAX_PURCHASE_TIMES = 10;
    public static final int PAGE_SIZE = 10;
    public static final int LEVEL_RANGE = 10;
    public static final int PLUNDER_COOLDOWN_MS = 3600 * 1000;

    public static final int REWARD_BASE_MULTIPLIER = 100;
    public static final double REWARD_RESOURCE_RATIO = 0.01;
    public static final double VICTIM_LOSS_RATIO = 0.005;

    public static int getPurchaseCost(int purchasedCount) {
        return Math.min((purchasedCount + 1) * 5, 50);
    }

    @Autowired
    private PlunderNpcMapper plunderNpcMapper;

    private List<NpcTemplate> npcTemplates = new ArrayList<>();

    @PostConstruct
    public void init() {
        loadNpcTemplates();
    }

    public void loadNpcTemplates() {
        try {
            List<Map<String, Object>> rows = plunderNpcMapper.findAll();
            List<NpcTemplate> templates = new ArrayList<>();
            for (Map<String, Object> row : rows) {
                NpcTemplate tpl = new NpcTemplate();
                tpl.setDbId(parseIntSafe(row.get("id"), 0));
                tpl.setName(String.valueOf(row.get("name")));
                tpl.setFaction(String.valueOf(row.get("faction")));
                tpl.setBonusResource(String.valueOf(row.get("bonusResource")));
                tpl.setSortOrder(parseIntSafe(row.get("sortOrder"), 0));
                tpl.setPowerBase(parseIntSafe(row.get("powerBase"), 800));
                tpl.setPowerExtra(parseIntSafe(row.get("powerExtra"), 0));
                templates.add(tpl);
            }
            this.npcTemplates = templates;
            logger.info("从数据库加载了 {} 个掠夺NPC模板", templates.size());
        } catch (Exception e) {
            logger.error("加载掠夺NPC模板失败", e);
        }
    }

    /**
     * 获取指定等级区间的所有NPC列表（基于数据库模板生成）
     */
    public List<PlunderNpc> generateNpcsForLevel(int playerLevel) {
        int npcLevel = Math.max(10, ((playerLevel - 1) / 10 + 1) * 10);
        List<PlunderNpc> npcs = new ArrayList<>();

        for (NpcTemplate tpl : npcTemplates) {
            PlunderNpc npc = new PlunderNpc();
            npc.setId("npc_" + tpl.getFaction() + "_" + npcLevel + "_" + tpl.getSortOrder());
            npc.setName(tpl.getName());
            npc.setFaction(tpl.getFaction());
            npc.setLevel(npcLevel);
            npc.setNpc(true);
            npc.setPower(npcLevel * tpl.getPowerBase() + tpl.getPowerExtra());

            Map<String, Long> resources = generateNpcResources(npcLevel, tpl.getBonusResource());
            npc.setSilver(resources.get("silver"));
            npc.setWood(resources.get("wood"));
            npc.setPaper(resources.get("paper"));
            npc.setFood(resources.get("food"));

            npcs.add(npc);
        }
        return npcs;
    }

    private Map<String, Long> generateNpcResources(int level, String bonusResource) {
        ThreadLocalRandom rng = ThreadLocalRandom.current();
        Map<String, Long> res = new HashMap<>();
        for (String type : new String[]{"silver", "wood", "paper", "food"}) {
            long base;
            if (type.equals(bonusResource)) {
                base = (long) level * 40000 + rng.nextLong(1000, 100001);
            } else {
                base = (long) level * 10000 + rng.nextLong(1000, 100001);
            }
            res.put(type, base);
        }
        return res;
    }

    private int parseIntSafe(Object val, int def) {
        if (val == null) return def;
        try { return Integer.parseInt(String.valueOf(val)); } catch (Exception e) { return def; }
    }

    /**
     * NPC数据库模板
     */
    public static class NpcTemplate {
        private int dbId;
        private String name;
        private String faction;
        private String bonusResource;
        private int sortOrder;
        private int powerBase;
        private int powerExtra;

        public int getDbId() { return dbId; }
        public void setDbId(int dbId) { this.dbId = dbId; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getFaction() { return faction; }
        public void setFaction(String faction) { this.faction = faction; }
        public String getBonusResource() { return bonusResource; }
        public void setBonusResource(String bonusResource) { this.bonusResource = bonusResource; }
        public int getSortOrder() { return sortOrder; }
        public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }
        public int getPowerBase() { return powerBase; }
        public void setPowerBase(int powerBase) { this.powerBase = powerBase; }
        public int getPowerExtra() { return powerExtra; }
        public void setPowerExtra(int powerExtra) { this.powerExtra = powerExtra; }
    }

    /**
     * 掠夺NPC数据对象（运行时生成）
     */
    public static class PlunderNpc {
        private String id;
        private String name;
        private String faction;
        private int level;
        private boolean isNpc;
        private int power;
        private long silver;
        private long wood;
        private long paper;
        private long food;

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getFaction() { return faction; }
        public void setFaction(String faction) { this.faction = faction; }
        public int getLevel() { return level; }
        public void setLevel(int level) { this.level = level; }
        public boolean isNpc() { return isNpc; }
        public void setNpc(boolean npc) { isNpc = npc; }
        public int getPower() { return power; }
        public void setPower(int power) { this.power = power; }
        public long getSilver() { return silver; }
        public void setSilver(long silver) { this.silver = silver; }
        public long getWood() { return wood; }
        public void setWood(long wood) { this.wood = wood; }
        public long getPaper() { return paper; }
        public void setPaper(long paper) { this.paper = paper; }
        public long getFood() { return food; }
        public void setFood(long food) { this.food = food; }
    }
}
