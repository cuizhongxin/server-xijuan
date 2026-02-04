package com.tencent.wxcloudrun.service;

import com.tencent.wxcloudrun.model.Equipment;
import com.tencent.wxcloudrun.model.Warehouse;
import com.tencent.wxcloudrun.model.UserResource;
import com.tencent.wxcloudrun.repository.EquipmentRepository;
import com.tencent.wxcloudrun.repository.UserResourceRepository;
import com.tencent.wxcloudrun.service.warehouse.WarehouseService;
import com.tencent.wxcloudrun.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 秘境探险服务
 */
@Service
public class SecretRealmService {
    
    private static final Logger logger = LoggerFactory.getLogger(SecretRealmService.class);
    
    @Autowired
    private WarehouseService warehouseService;
    
    @Autowired
    private UserResourceRepository resourceRepository;
    
    @Autowired
    private EquipmentRepository equipmentRepository;
    
    // 秘境配置
    private static final Map<String, RealmConfig> REALM_CONFIGS = new HashMap<>();
    
    static {
        // 蓬莱秘宝
        RealmConfig penglai = new RealmConfig("penglai", "蓬莱秘宝", 40, 10);
        penglai.addReward(new RewardItem("penglai_sword", "鹰扬刀", "🗡️", "equipment", "blue", 1));
        penglai.addReward(new RewardItem("penglai_ring", "鹰扬戒", "💍", "equipment", "blue", 2));
        penglai.addReward(new RewardItem("penglai_necklace", "鹰扬项链", "📿", "equipment", "blue", 4));
        penglai.addReward(new RewardItem("penglai_armor", "鹰扬铠", "🛡️", "equipment", "blue", 3));
        penglai.addReward(new RewardItem("penglai_helm", "鹰扬盔", "⛑️", "equipment", "blue", 5));
        penglai.addReward(new RewardItem("penglai_boots", "鹰扬靴", "👢", "equipment", "blue", 6));
        penglai.addReward(new RewardItem("silver_ingot", "银锭", "🥈", "material", "white", 0));
        penglai.addReward(new RewardItem("enhance_stone_4", "4级强化石", "💎", "material", "green", 0));
        penglai.addReward(new RewardItem("exp_pill_low", "初级经验丹", "📕", "consumable", "green", 0));
        penglai.addReward(new RewardItem("recruit_token_mid", "中级招贤令", "📜", "consumable", "blue", 0));
        penglai.addReward(new RewardItem("compose_talisman_mid", "中级合成符", "📋", "material", "blue", 0));
        penglai.addReward(new RewardItem("special_train", "特训符", "📑", "consumable", "purple", 0));
        REALM_CONFIGS.put("penglai", penglai);
        
        // 昆仑秘宝
        RealmConfig kunlun = new RealmConfig("kunlun", "昆仑秘宝", 60, 20);
        kunlun.addReward(new RewardItem("kunlun_sword", "昆仑剑", "⚔️", "equipment", "purple", 1));
        kunlun.addReward(new RewardItem("kunlun_ring", "昆仑戒", "💍", "equipment", "purple", 2));
        kunlun.addReward(new RewardItem("kunlun_necklace", "昆仑链", "📿", "equipment", "purple", 4));
        kunlun.addReward(new RewardItem("kunlun_armor", "昆仑甲", "🛡️", "equipment", "purple", 3));
        kunlun.addReward(new RewardItem("kunlun_helm", "昆仑盔", "⛑️", "equipment", "purple", 5));
        kunlun.addReward(new RewardItem("kunlun_boots", "昆仑靴", "👢", "equipment", "purple", 6));
        kunlun.addReward(new RewardItem("gold_ingot", "金锭", "🥇", "material", "green", 0));
        kunlun.addReward(new RewardItem("enhance_stone_5", "5级强化石", "💎", "material", "blue", 0));
        kunlun.addReward(new RewardItem("exp_pill_mid", "中级经验丹", "📕", "consumable", "blue", 0));
        kunlun.addReward(new RewardItem("recruit_token_high", "高级招贤令", "📜", "consumable", "purple", 0));
        REALM_CONFIGS.put("kunlun", kunlun);
        
        // 瑶池秘宝
        RealmConfig yaochi = new RealmConfig("yaochi", "瑶池秘宝", 80, 50);
        yaochi.addReward(new RewardItem("yaochi_sword", "瑶池剑", "⚔️", "equipment", "orange", 1));
        yaochi.addReward(new RewardItem("yaochi_ring", "瑶池戒", "💍", "equipment", "orange", 2));
        yaochi.addReward(new RewardItem("yaochi_necklace", "瑶池链", "📿", "equipment", "purple", 4));
        yaochi.addReward(new RewardItem("yaochi_armor", "瑶池甲", "🛡️", "equipment", "purple", 3));
        yaochi.addReward(new RewardItem("yaochi_helm", "瑶池盔", "⛑️", "equipment", "purple", 5));
        yaochi.addReward(new RewardItem("yaochi_boots", "瑶池靴", "👢", "equipment", "purple", 6));
        yaochi.addReward(new RewardItem("fairy_crystal", "仙晶", "✨", "material", "blue", 0));
        yaochi.addReward(new RewardItem("enhance_stone_6", "6级强化石", "💎", "material", "purple", 0));
        REALM_CONFIGS.put("yaochi", yaochi);
        
        // 九天秘宝
        RealmConfig jiutian = new RealmConfig("jiutian", "九天秘宝", 100, 100);
        jiutian.addReward(new RewardItem("jiutian_sword", "九天神剑", "⚔️", "equipment", "orange", 1));
        jiutian.addReward(new RewardItem("jiutian_ring", "九天神戒", "💍", "equipment", "orange", 2));
        jiutian.addReward(new RewardItem("jiutian_necklace", "九天神链", "📿", "equipment", "orange", 4));
        jiutian.addReward(new RewardItem("jiutian_armor", "九天神甲", "🛡️", "equipment", "orange", 3));
        jiutian.addReward(new RewardItem("jiutian_helm", "九天神盔", "⛑️", "equipment", "orange", 5));
        jiutian.addReward(new RewardItem("jiutian_boots", "九天神靴", "👢", "equipment", "orange", 6));
        REALM_CONFIGS.put("jiutian", jiutian);
    }
    
    /**
     * 探索秘境
     */
    public ExploreResult explore(String userId, String realmId, int count) {
        RealmConfig config = REALM_CONFIGS.get(realmId);
        if (config == null) {
            throw new BusinessException(400, "秘境不存在");
        }
        
        UserResource resource = resourceRepository.findByUserId(userId);
        if (resource == null) {
            throw new BusinessException(400, "用户资源不存在");
        }
        
        // 计算费用
        double discount = 1.0;
        if (count == 10) discount = 0.95;
        if (count == 50) discount = 0.9;
        int totalCost = (int) Math.floor(config.costGold * count * discount);
        
        if (resource.getGold() < totalCost) {
            throw new BusinessException(400, "黄金不足，需要" + totalCost + "黄金");
        }
        
        // 扣除黄金
        resource.setGold(resource.getGold() - totalCost);
        resourceRepository.save(resource);
        
        // 生成奖励
        List<RewardItem> rewards = generateRewards(config, count);
        
        // 将奖励存入仓库
        List<Map<String, Object>> resultItems = new ArrayList<>();
        for (RewardItem reward : rewards) {
            Map<String, Object> itemInfo = addRewardToWarehouse(userId, reward, config);
            resultItems.add(itemInfo);
        }
        
        // 合并相同物品
        resultItems = mergeResults(resultItems);
        
        logger.info("用户 {} 在秘境 {} 探索 {} 次，花费 {} 黄金，获得 {} 种物品", 
                   userId, realmId, count, totalCost, resultItems.size());
        
        ExploreResult result = new ExploreResult();
        result.setSuccess(true);
        result.setTotalCost(totalCost);
        result.setRemainingGold(resource.getGold() != null ? resource.getGold().intValue() : 0);
        result.setItems(resultItems);
        
        return result;
    }
    
    /**
     * 生成奖励
     */
    private List<RewardItem> generateRewards(RealmConfig config, int count) {
        List<RewardItem> results = new ArrayList<>();
        Random random = new Random();
        
        // 按品质分类
        List<RewardItem> orangeItems = new ArrayList<>();
        List<RewardItem> purpleItems = new ArrayList<>();
        List<RewardItem> blueItems = new ArrayList<>();
        List<RewardItem> greenItems = new ArrayList<>();
        List<RewardItem> whiteItems = new ArrayList<>();
        
        for (RewardItem r : config.rewards) {
            switch (r.quality) {
                case "orange": orangeItems.add(r); break;
                case "purple": purpleItems.add(r); break;
                case "blue": blueItems.add(r); break;
                case "green": greenItems.add(r); break;
                case "white": whiteItems.add(r); break;
            }
        }
        
        for (int i = 0; i < count; i++) {
            double rand = random.nextDouble();
            RewardItem selected = null;
            
            if (rand < 0.05 && !orangeItems.isEmpty()) {
                selected = orangeItems.get(random.nextInt(orangeItems.size()));
            } else if (rand < 0.15 && !purpleItems.isEmpty()) {
                selected = purpleItems.get(random.nextInt(purpleItems.size()));
            } else if (rand < 0.40 && !blueItems.isEmpty()) {
                selected = blueItems.get(random.nextInt(blueItems.size()));
            } else if (rand < 0.70 && !greenItems.isEmpty()) {
                selected = greenItems.get(random.nextInt(greenItems.size()));
            } else if (!whiteItems.isEmpty()) {
                selected = whiteItems.get(random.nextInt(whiteItems.size()));
            }
            
            if (selected == null && !config.rewards.isEmpty()) {
                selected = config.rewards.get(random.nextInt(config.rewards.size()));
            }
            
            if (selected != null) {
                results.add(selected);
            }
        }
        
        return results;
    }
    
    /**
     * 将奖励添加到仓库
     */
    private Map<String, Object> addRewardToWarehouse(String userId, RewardItem reward, RealmConfig config) {
        Map<String, Object> itemInfo = new HashMap<>();
        itemInfo.put("id", reward.id);
        itemInfo.put("name", reward.name);
        itemInfo.put("icon", reward.icon);
        itemInfo.put("quality", reward.quality);
        itemInfo.put("type", reward.type);
        itemInfo.put("count", 1);
        
        if ("equipment".equals(reward.type)) {
            // 创建装备并加入仓库
            Equipment equipment = createEquipment(userId, reward, config);
            equipmentRepository.save(equipment);
            warehouseService.addEquipment(userId, equipment.getId());
            itemInfo.put("equipmentId", equipment.getId());
        } else {
            // 添加物品到仓库
            Warehouse.WarehouseItem warehouseItem = new Warehouse.WarehouseItem();
            warehouseItem.setItemId(reward.id);
            warehouseItem.setName(reward.name);
            warehouseItem.setIcon(reward.icon);
            warehouseItem.setItemType(reward.type);
            warehouseItem.setQuality(reward.quality);
            warehouseItem.setCount(1);
            warehouseItem.setMaxStack(99);
            warehouseItem.setUsable(!"material".equals(reward.type));
            warehouseItem.setDescription(getItemDescription(reward));
            
            warehouseService.addItem(userId, warehouseItem);
        }
        
        return itemInfo;
    }
    
    /**
     * 创建装备
     */
    private Equipment createEquipment(String userId, RewardItem reward, RealmConfig config) {
        Equipment equipment = new Equipment();
        equipment.setId(UUID.randomUUID().toString());
        equipment.setUserId(userId);
        equipment.setName(reward.name);
        equipment.setIcon(reward.icon);
        equipment.setLevel(config.minLevel);
        equipment.setEquipped(false);
        equipment.setCreateTime(System.currentTimeMillis());
        equipment.setUpdateTime(System.currentTimeMillis());
        
        // 设置槽位类型
        Equipment.SlotType slotType = new Equipment.SlotType();
        slotType.setId(reward.slotType);
        slotType.setName(getSlotTypeName(reward.slotType));
        equipment.setSlotType(slotType);
        
        // 设置品质
        Equipment.Quality quality = new Equipment.Quality();
        quality.setId(getQualityId(reward.quality));
        quality.setName(getQualityName(reward.quality));
        quality.setColor(getQualityColor(reward.quality));
        quality.setMultiplier(getQualityMultiplier(reward.quality));
        equipment.setQuality(quality);
        
        // 设置套装信息
        Equipment.SetInfo setInfo = new Equipment.SetInfo();
        setInfo.setSetId(config.id);
        setInfo.setSetName(config.name.replace("秘宝", "套装"));
        setInfo.setSetLevel(config.minLevel);
        equipment.setSetInfo(setInfo);
        
        // 设置基础属性
        Equipment.Attributes baseAttrs = generateBaseAttributes(reward.slotType, config.minLevel, reward.quality);
        equipment.setBaseAttributes(baseAttrs);
        
        // 设置来源
        Equipment.Source source = new Equipment.Source();
        source.setType("SECRET_REALM");
        source.setName("秘境探索");
        source.setDetail(config.name);
        equipment.setSource(source);
        
        equipment.setDescription(config.name + "探索获得的" + getQualityName(reward.quality) + "装备");
        
        return equipment;
    }
    
    /**
     * 生成基础属性
     */
    private Equipment.Attributes generateBaseAttributes(int slotType, int level, String quality) {
        Equipment.Attributes attrs = new Equipment.Attributes();
        double multiplier = getQualityMultiplier(quality);
        int base = (int) (level * 2 * multiplier);
        
        switch (slotType) {
            case 1: // 武器
                attrs.setAttack(base * 2);
                attrs.setValor((int)(base * 0.5));
                break;
            case 2: // 戒指
                attrs.setAttack(base);
                attrs.setCritRate(0.05 * multiplier);
                break;
            case 3: // 铠甲
                attrs.setDefense(base * 2);
                attrs.setHp(base * 10);
                break;
            case 4: // 项链
                attrs.setCommand(base);
                attrs.setHp(base * 5);
                break;
            case 5: // 头盔
                attrs.setDefense(base);
                attrs.setValor((int)(base * 0.3));
                break;
            case 6: // 鞋子
                attrs.setMobility((int)(level * 0.1 * multiplier));
                attrs.setDodge(0.03 * multiplier);
                break;
        }
        
        return attrs;
    }
    
    /**
     * 合并相同物品的结果
     */
    private List<Map<String, Object>> mergeResults(List<Map<String, Object>> items) {
        Map<String, Map<String, Object>> merged = new LinkedHashMap<>();
        
        for (Map<String, Object> item : items) {
            String key = (String) item.get("id");
            if ("equipment".equals(item.get("type"))) {
                // 装备不合并，每件单独显示
                merged.put(key + "_" + System.nanoTime(), item);
            } else {
                if (merged.containsKey(key)) {
                    Map<String, Object> existing = merged.get(key);
                    existing.put("count", (int) existing.get("count") + 1);
                } else {
                    merged.put(key, new HashMap<>(item));
                }
            }
        }
        
        // 按品质排序
        List<Map<String, Object>> result = new ArrayList<>(merged.values());
        result.sort((a, b) -> {
            int qa = getQualityOrder((String) a.get("quality"));
            int qb = getQualityOrder((String) b.get("quality"));
            return qa - qb;
        });
        
        return result;
    }
    
    private int getQualityOrder(String quality) {
        switch (quality) {
            case "orange": return 0;
            case "purple": return 1;
            case "blue": return 2;
            case "green": return 3;
            case "white": return 4;
            default: return 5;
        }
    }
    
    private String getSlotTypeName(int slotType) {
        switch (slotType) {
            case 1: return "武器";
            case 2: return "戒指";
            case 3: return "铠甲";
            case 4: return "项链";
            case 5: return "头盔";
            case 6: return "鞋子";
            default: return "装备";
        }
    }
    
    private int getQualityId(String quality) {
        switch (quality) {
            case "white": return 1;
            case "green": return 2;
            case "blue": return 3;
            case "purple": return 4;
            case "orange": return 5;
            default: return 1;
        }
    }
    
    private String getQualityName(String quality) {
        switch (quality) {
            case "white": return "普通";
            case "green": return "优秀";
            case "blue": return "精良";
            case "purple": return "史诗";
            case "orange": return "传说";
            default: return "普通";
        }
    }
    
    private String getQualityColor(String quality) {
        switch (quality) {
            case "white": return "#ffffff";
            case "green": return "#00ff00";
            case "blue": return "#0088ff";
            case "purple": return "#aa00ff";
            case "orange": return "#ff8800";
            default: return "#ffffff";
        }
    }
    
    private double getQualityMultiplier(String quality) {
        switch (quality) {
            case "white": return 1.0;
            case "green": return 1.2;
            case "blue": return 1.5;
            case "purple": return 2.0;
            case "orange": return 3.0;
            default: return 1.0;
        }
    }
    
    private String getItemDescription(RewardItem reward) {
        switch (reward.type) {
            case "material":
                if (reward.id.contains("enhance_stone")) {
                    return "用于强化装备，可提升装备属性";
                } else if (reward.id.contains("ingot")) {
                    return "珍贵的金属材料，可用于制作或出售";
                } else {
                    return "珍贵的材料";
                }
            case "consumable":
                if (reward.id.contains("exp_pill")) {
                    return "使用后可获得经验值";
                } else if (reward.id.contains("recruit_token")) {
                    return "用于招募武将";
                } else if (reward.id.contains("special_train")) {
                    return "使用后可对武将进行特训";
                } else {
                    return "可使用的消耗品";
                }
            default:
                return "秘境探索获得的物品";
        }
    }
    
    // ==================== 内部类 ====================
    
    public static class RealmConfig {
        String id;
        String name;
        int minLevel;
        int costGold;
        List<RewardItem> rewards = new ArrayList<>();
        
        public RealmConfig(String id, String name, int minLevel, int costGold) {
            this.id = id;
            this.name = name;
            this.minLevel = minLevel;
            this.costGold = costGold;
        }
        
        public void addReward(RewardItem item) {
            rewards.add(item);
        }
    }
    
    public static class RewardItem {
        String id;
        String name;
        String icon;
        String type; // equipment, material, consumable
        String quality; // white, green, blue, purple, orange
        int slotType; // 装备槽位 (仅装备有效)
        
        public RewardItem(String id, String name, String icon, String type, String quality, int slotType) {
            this.id = id;
            this.name = name;
            this.icon = icon;
            this.type = type;
            this.quality = quality;
            this.slotType = slotType;
        }
    }
    
    public static class ExploreResult {
        private boolean success;
        private int totalCost;
        private int remainingGold;
        private List<Map<String, Object>> items;
        
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        
        public int getTotalCost() { return totalCost; }
        public void setTotalCost(int totalCost) { this.totalCost = totalCost; }
        
        public int getRemainingGold() { return remainingGold; }
        public void setRemainingGold(int remainingGold) { this.remainingGold = remainingGold; }
        
        public List<Map<String, Object>> getItems() { return items; }
        public void setItems(List<Map<String, Object>> items) { this.items = items; }
    }
}
