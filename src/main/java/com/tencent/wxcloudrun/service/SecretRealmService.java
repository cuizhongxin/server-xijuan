package com.tencent.wxcloudrun.service;

import com.tencent.wxcloudrun.model.Equipment;
import com.tencent.wxcloudrun.model.Warehouse;
import com.tencent.wxcloudrun.model.UserResource;
import com.tencent.wxcloudrun.repository.EquipmentRepository;
import com.tencent.wxcloudrun.repository.SecretRealmConfigRepository;
import com.tencent.wxcloudrun.repository.UserResourceRepository;
import com.tencent.wxcloudrun.service.warehouse.WarehouseService;
import com.tencent.wxcloudrun.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static com.tencent.wxcloudrun.repository.SecretRealmConfigRepository.*;

@Service
public class SecretRealmService {

    private static final Logger logger = LoggerFactory.getLogger(SecretRealmService.class);

    @Autowired
    private WarehouseService warehouseService;
    @Autowired
    private UserResourceRepository resourceRepository;
    @Autowired
    private EquipmentRepository equipmentRepository;
    @Autowired
    private SecretRealmConfigRepository realmConfigRepo;

    /**
     * 获取所有秘境概览(从数据库读取)
     */
    public List<Map<String, Object>> listRealms() {
        List<Map<String, Object>> configs = realmConfigRepo.findAllRealms();
        List<Map<String, Object>> result = new ArrayList<>();

        for (Map<String, Object> cfg : configs) {
            String realmId = getString(cfg, "id", "");
            List<Map<String, Object>> equips = realmConfigRepo.findEquipmentsByRealmId(realmId);
            List<Map<String, Object>> items = realmConfigRepo.findItemsByRealmId(realmId);

            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", realmId);
            m.put("name", getString(cfg, "name", ""));
            m.put("minLevel", getInt(cfg, "min_level", 1));
            m.put("costGold", getInt(cfg, "cost_gold", 10));
            m.put("equipSetName", getString(cfg, "equip_set_name", ""));
            m.put("equipBaseRate", getDouble(cfg, "equip_base_rate", 0.08));
            m.put("pityCount", getInt(cfg, "pity_count", 50));
            m.put("equipCount", equips.size());
            m.put("itemCount", items.size());
            result.add(m);
        }
        return result;
    }

    /**
     * 获取秘境奖励预览(12件: 6装备+6道具, 从数据库读取)
     */
    public List<Map<String, Object>> getRealmRewards(String realmId) {
        Map<String, Object> cfg = realmConfigRepo.findRealmById(realmId);
        if (cfg == null) {
            throw new BusinessException(400, "秘境不存在: " + realmId);
        }

        List<Map<String, Object>> dbRewards = realmConfigRepo.findRewardsByRealmId(realmId);
        List<Map<String, Object>> rewards = new ArrayList<>();

        int level = getInt(cfg, "min_level", 1);

        for (Map<String, Object> row : dbRewards) {
            Map<String, Object> m = new LinkedHashMap<>();
            String rewardType = getString(row, "reward_type", "item");

            m.put("rewardType", rewardType);
            m.put("name", getString(row, "name", ""));
            m.put("icon", getString(row, "icon", "📦"));
            m.put("quality", getInt(row, "quality", 1));
            m.put("dropWeight", getInt(row, "drop_weight", 100));

            if ("equipment".equals(rewardType)) {
                m.put("id", getInt(row, "equip_pre_id", 0));
                m.put("equipPreId", getInt(row, "equip_pre_id", 0));
                m.put("itemPreId", 0);
                m.put("type", "equipment");
                m.put("position", getString(row, "position", ""));
                m.put("setName", getString(row, "set_name", ""));
                m.put("setEffect3", getString(row, "set_effect_3", ""));
                m.put("setEffect6", getString(row, "set_effect_6", ""));
                m.put("attack", getInt(row, "attack", 0));
                m.put("defense", getInt(row, "defense", 0));
                m.put("soldierHp", getInt(row, "soldier_hp", 0));
                m.put("mobility", getInt(row, "mobility", 0));
                m.put("level", level);
            } else {
                m.put("id", getString(row, "item_id", ""));
                m.put("equipPreId", 0);
                m.put("itemPreId", getInt(row, "item_pre_id", 0));
                m.put("type", getString(row, "item_sub_type", "material"));
            }
            rewards.add(m);
        }
        return rewards;
    }

    /**
     * 探索秘境(含保底机制)
     */
    public ExploreResult explore(String userId, String realmId, int count) {
        Map<String, Object> cfg = realmConfigRepo.findRealmById(realmId);
        if (cfg == null) {
            throw new BusinessException(400, "秘境不存在");
        }

        int costGold = getInt(cfg, "cost_gold", 10);
        int minLevel = getInt(cfg, "min_level", 1);
        double equipBaseRate = getDouble(cfg, "equip_base_rate", 0.08);
        int pityCount = getInt(cfg, "pity_count", 50);
        int dailyLimit = getInt(cfg, "daily_limit", 0);

        // 加载保底计数
        Map<String, Object> pityRow = realmConfigRepo.findPity(userId, realmId);
        int countSinceEquip = 0;
        int totalExploreCount = 0;
        int totalEquipCount = 0;
        long lastEquipTime = 0;
        int dailyCount = 0;

        if (pityRow != null) {
            countSinceEquip = getInt(pityRow, "count_since_equip", 0);
            totalExploreCount = getInt(pityRow, "total_explore_count", 0);
            totalEquipCount = getInt(pityRow, "total_equip_count", 0);
            lastEquipTime = getLong(pityRow, "last_equip_time", 0);
            dailyCount = getInt(pityRow, "daily_count", 0);

            String resetDateStr = getString(pityRow, "daily_reset_date", "");
            String today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
            if (!today.equals(resetDateStr)) {
                dailyCount = 0;
            }
        }

        if (dailyLimit > 0 && dailyCount + count > dailyLimit) {
            throw new BusinessException(400, "今日探索次数已达上限(" + dailyLimit + "次)");
        }

        UserResource resource = resourceRepository.findByUserId(userId);
        if (resource == null) {
            throw new BusinessException(400, "用户资源不存在");
        }

        double discount = 1.0;
        if (count == 10) discount = 0.95;
        if (count == 50) discount = 0.9;
        int totalCost = (int) Math.floor(costGold * count * discount);

        if (resource.getGold() < totalCost) {
            throw new BusinessException(400, "黄金不足，需要" + totalCost + "黄金");
        }

        resource.setGold(resource.getGold() - totalCost);
        resourceRepository.save(resource);

        // 从数据库加载装备池和道具池
        List<Map<String, Object>> equipPool = realmConfigRepo.findEquipmentsByRealmId(realmId);
        List<Map<String, Object>> itemPool = realmConfigRepo.findItemsByRealmId(realmId);

        // 计算道具池总权重
        int itemTotalWeight = 0;
        for (Map<String, Object> it : itemPool) {
            itemTotalWeight += getInt(it, "drop_weight", 100);
        }

        // 计算装备池总权重
        int equipTotalWeight = 0;
        for (Map<String, Object> eq : equipPool) {
            equipTotalWeight += getInt(eq, "drop_weight", 100);
        }

        List<Map<String, Object>> resultItems = new ArrayList<>();
        Random random = new Random();

        for (int i = 0; i < count; i++) {
            countSinceEquip++;
            totalExploreCount++;
            dailyCount++;

            boolean shouldDropEquip = false;

            // 保底判定: 达到保底次数必出装备
            if (pityCount > 0 && countSinceEquip >= pityCount && !equipPool.isEmpty()) {
                shouldDropEquip = true;
                logger.info("用户 {} 在秘境 {} 触发保底(连续{}次未出装备)", userId, realmId, countSinceEquip);
            }
            // 常规概率判定
            else if (random.nextDouble() < equipBaseRate && !equipPool.isEmpty()) {
                shouldDropEquip = true;
            }

            Map<String, Object> itemInfo;

            if (shouldDropEquip) {
                // 按权重随机选择装备
                Map<String, Object> eqRow = selectByWeight(equipPool, equipTotalWeight, random);

                Equipment equipment = createEquipmentFromRow(userId, eqRow, minLevel);
                equipmentRepository.save(equipment);
                warehouseService.addEquipment(userId, equipment.getId());

                itemInfo = new LinkedHashMap<>();
                itemInfo.put("id", "equip_" + getInt(eqRow, "equip_pre_id", 0));
                itemInfo.put("name", getString(eqRow, "name", ""));
                itemInfo.put("icon", getString(eqRow, "icon", "⚔️"));
                itemInfo.put("quality", getInt(eqRow, "quality", 3));
                itemInfo.put("rewardType", "equipment");
                itemInfo.put("type", "equipment");
                itemInfo.put("equipPreId", getInt(eqRow, "equip_pre_id", 0));
                itemInfo.put("itemPreId", 0);
                itemInfo.put("count", 1);
                itemInfo.put("equipmentId", equipment.getId());

                countSinceEquip = 0;
                totalEquipCount++;
                lastEquipTime = System.currentTimeMillis();
            } else {
                if (itemPool.isEmpty()) continue;

                // 按权重随机选择道具
                Map<String, Object> itRow = selectByWeight(itemPool, itemTotalWeight, random);

                String itemId = getString(itRow, "item_id", "");
                String name = getString(itRow, "name", "");
                String icon = getString(itRow, "icon", "📦");
                String subType = getString(itRow, "item_sub_type", "material");
                int quality = getInt(itRow, "quality", 1);
                String desc = getString(itRow, "description", "秘境探索获得的物品");

                int itemPreId = getInt(itRow, "item_pre_id", 0);

                itemInfo = new LinkedHashMap<>();
                itemInfo.put("id", itemId);
                itemInfo.put("name", name);
                itemInfo.put("icon", icon);
                itemInfo.put("quality", quality);
                itemInfo.put("rewardType", "item");
                itemInfo.put("type", subType);
                itemInfo.put("equipPreId", 0);
                itemInfo.put("itemPreId", itemPreId);
                itemInfo.put("count", 1);

                Warehouse.WarehouseItem wItem = new Warehouse.WarehouseItem();
                wItem.setItemId(itemId);
                wItem.setName(name);
                wItem.setIcon(icon);
                wItem.setItemType(subType);
                wItem.setQuality(qualityName(quality));
                wItem.setCount(1);
                wItem.setMaxStack(99);
                wItem.setUsable(!"material".equals(subType));
                wItem.setDescription(desc);
                warehouseService.addItem(userId, wItem);
            }

            resultItems.add(itemInfo);
        }

        // 保存保底计数
        realmConfigRepo.savePity(userId, realmId, countSinceEquip,
                totalExploreCount, totalEquipCount, lastEquipTime, dailyCount);

        resultItems = mergeResults(resultItems);

        logger.info("用户 {} 在秘境 {} 探索 {} 次，花费 {} 黄金，获得 {} 种物品，保底计数: {}/{}",
                userId, realmId, count, totalCost, resultItems.size(), countSinceEquip, pityCount);

        ExploreResult result = new ExploreResult();
        result.setSuccess(true);
        result.setTotalCost(totalCost);
        result.setRemainingGold(resource.getGold() != null ? resource.getGold().intValue() : 0);
        result.setItems(resultItems);
        result.setPityCount(countSinceEquip);
        result.setPityLimit(pityCount);

        return result;
    }

    // ==================== 私有方法 ====================

    /**
     * 按权重随机选择
     */
    private Map<String, Object> selectByWeight(List<Map<String, Object>> pool, int totalWeight, Random random) {
        if (pool.size() == 1) return pool.get(0);

        int roll = random.nextInt(totalWeight);
        int cumulative = 0;
        for (Map<String, Object> row : pool) {
            cumulative += getInt(row, "drop_weight", 100);
            if (roll < cumulative) {
                return row;
            }
        }
        return pool.get(pool.size() - 1);
    }

    private Equipment createEquipmentFromRow(String userId, Map<String, Object> eqRow, int level) {
        Equipment equipment = new Equipment();
        equipment.setId(UUID.randomUUID().toString());
        equipment.setUserId(userId);
        equipment.setName(getString(eqRow, "name", ""));
        equipment.setIcon(getString(eqRow, "icon", "⚔️"));
        equipment.setLevel(level);
        equipment.setEquipped(false);
        equipment.setCreateTime(System.currentTimeMillis());
        equipment.setUpdateTime(System.currentTimeMillis());

        String position = getString(eqRow, "position", "武器");
        Equipment.SlotType slotType = new Equipment.SlotType();
        slotType.setId(positionToSlot(position));
        slotType.setName(position);
        equipment.setSlotType(slotType);

        int qId = getInt(eqRow, "quality", 3);
        Equipment.Quality quality = new Equipment.Quality();
        quality.setId(qId);
        quality.setName(qualityName(qId));
        quality.setColor(qualityColor(qId));
        quality.setMultiplier(qualityMultiplier(qId));
        equipment.setQuality(quality);

        String setName = getString(eqRow, "set_name", "");
        Equipment.SetInfo setInfo = new Equipment.SetInfo();
        setInfo.setSetId(setName);
        setInfo.setSetName(setName + "套装");
        setInfo.setSetLevel(level);
        equipment.setSetInfo(setInfo);

        Equipment.Attributes attrs = new Equipment.Attributes();
        attrs.setAttack(getInt(eqRow, "attack", 0));
        attrs.setDefense(getInt(eqRow, "defense", 0));
        attrs.setHp(getInt(eqRow, "soldier_hp", 0));
        attrs.setMobility(getInt(eqRow, "mobility", 0));
        equipment.setBaseAttributes(attrs);

        Equipment.Source source = new Equipment.Source();
        source.setType("SECRET_REALM");
        source.setName("秘境产出");
        source.setDetail(setName + "套 - " + position);
        equipment.setSource(source);

        String setEffect3 = getString(eqRow, "set_effect_3", "");
        String setEffect6 = getString(eqRow, "set_effect_6", "");
        equipment.setDescription(setName + "套 - " + position + " [3件:" + setEffect3 + " 6件:" + setEffect6 + "]");

        return equipment;
    }

    private int positionToSlot(String position) {
        switch (position) {
            case "武器": return 1;
            case "戒指": return 2;
            case "铠甲": return 3;
            case "项链": return 4;
            case "头盔": return 5;
            case "鞋子": return 6;
            default: return 1;
        }
    }

    private List<Map<String, Object>> mergeResults(List<Map<String, Object>> items) {
        Map<String, Map<String, Object>> merged = new LinkedHashMap<>();
        for (Map<String, Object> item : items) {
            String key = String.valueOf(item.get("id"));
            if ("equipment".equals(item.get("rewardType"))) {
                merged.put(key + "_" + System.nanoTime(), item);
            } else {
                if (merged.containsKey(key)) {
                    Map<String, Object> existing = merged.get(key);
                    existing.put("count", (int) existing.get("count") + 1);
                } else {
                    merged.put(key, new LinkedHashMap<>(item));
                }
            }
        }
        List<Map<String, Object>> result = new ArrayList<>(merged.values());
        result.sort((a, b) -> {
            Object qa = a.get("quality");
            Object qb = b.get("quality");
            int qai = qa instanceof Integer ? (Integer) qa : 1;
            int qbi = qb instanceof Integer ? (Integer) qb : 1;
            return qbi - qai;
        });
        return result;
    }

    private String qualityName(int q) {
        switch (q) {
            case 1: return "普通"; case 2: return "优秀"; case 3: return "精良";
            case 4: return "稀有"; case 5: return "史诗"; case 6: return "传说";
            default: return "普通";
        }
    }

    private String qualityColor(int q) {
        switch (q) {
            case 1: return "#aaaaaa"; case 2: return "#55ff55"; case 3: return "#5599ff";
            case 4: return "#ff4444"; case 5: return "#cc77ff"; case 6: return "#ff9933";
            default: return "#aaaaaa";
        }
    }

    private double qualityMultiplier(int q) {
        switch (q) {
            case 1: return 1.0; case 2: return 1.2; case 3: return 1.5;
            case 4: return 1.8; case 5: return 2.0; case 6: return 3.0;
            default: return 1.0;
        }
    }

    // ==================== 结果模型 ====================

    public static class ExploreResult {
        private boolean success;
        private int totalCost;
        private int remainingGold;
        private List<Map<String, Object>> items;
        private int pityCount;
        private int pityLimit;

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        public int getTotalCost() { return totalCost; }
        public void setTotalCost(int totalCost) { this.totalCost = totalCost; }
        public int getRemainingGold() { return remainingGold; }
        public void setRemainingGold(int remainingGold) { this.remainingGold = remainingGold; }
        public List<Map<String, Object>> getItems() { return items; }
        public void setItems(List<Map<String, Object>> items) { this.items = items; }
        public int getPityCount() { return pityCount; }
        public void setPityCount(int pityCount) { this.pityCount = pityCount; }
        public int getPityLimit() { return pityLimit; }
        public void setPityLimit(int pityLimit) { this.pityLimit = pityLimit; }
    }
}
