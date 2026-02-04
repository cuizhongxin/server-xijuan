package com.tencent.wxcloudrun.service.production;

import com.tencent.wxcloudrun.exception.BusinessException;
import com.tencent.wxcloudrun.model.Equipment;
import com.tencent.wxcloudrun.model.Production;
import com.tencent.wxcloudrun.model.Production.*;
import com.tencent.wxcloudrun.model.UserResource;
import com.tencent.wxcloudrun.repository.EquipmentRepository;
import com.tencent.wxcloudrun.service.UserResourceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 生产制造服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductionService {
    
    private final UserResourceService userResourceService;
    private final EquipmentRepository equipmentRepository;
    
    // 用户生产数据存储
    private final Map<String, Production> productionStore = new ConcurrentHashMap<>();
    
    // 制造配方
    private final List<Recipe> recipes = new ArrayList<>();
    
    /**
     * 初始化配方
     */
    @javax.annotation.PostConstruct
    public void initRecipes() {
        // 军械局配方 - 装备
        recipes.add(createEquipmentRecipe("recipe_weapon_1", "青铜剑", "arsenal", 1, "weapon", "普通", 500, 300, 0, 0));
        recipes.add(createEquipmentRecipe("recipe_weapon_2", "精钢刀", "arsenal", 3, "weapon", "优秀", 1000, 600, 0, 0));
        recipes.add(createEquipmentRecipe("recipe_weapon_3", "寒冰剑", "arsenal", 5, "weapon", "精良", 2000, 1200, 0, 0));
        recipes.add(createEquipmentRecipe("recipe_weapon_4", "烈焰刀", "arsenal", 8, "weapon", "史诗", 5000, 3000, 0, 0));
        recipes.add(createEquipmentRecipe("recipe_weapon_5", "龙渊剑", "arsenal", 12, "weapon", "传说", 10000, 6000, 0, 0));
        
        recipes.add(createEquipmentRecipe("recipe_armor_1", "皮甲", "arsenal", 1, "armor", "普通", 400, 200, 100, 0));
        recipes.add(createEquipmentRecipe("recipe_armor_2", "锁子甲", "arsenal", 3, "armor", "优秀", 800, 500, 200, 0));
        recipes.add(createEquipmentRecipe("recipe_armor_3", "鱼鳞甲", "arsenal", 5, "armor", "精良", 1600, 1000, 400, 0));
        recipes.add(createEquipmentRecipe("recipe_armor_4", "玄铁甲", "arsenal", 8, "armor", "史诗", 4000, 2500, 1000, 0));
        
        recipes.add(createEquipmentRecipe("recipe_helmet_1", "铁盔", "arsenal", 2, "helmet", "普通", 300, 200, 0, 0));
        recipes.add(createEquipmentRecipe("recipe_helmet_2", "钢盔", "arsenal", 4, "helmet", "优秀", 600, 400, 0, 0));
        
        recipes.add(createEquipmentRecipe("recipe_ring_1", "铜戒", "arsenal", 2, "ring", "普通", 200, 100, 0, 50));
        recipes.add(createEquipmentRecipe("recipe_shoes_1", "布靴", "arsenal", 1, "shoes", "普通", 200, 50, 100, 0));
        recipes.add(createEquipmentRecipe("recipe_necklace_1", "玉佩", "arsenal", 2, "necklace", "普通", 300, 100, 0, 100));
        
        // 奇物坊配方 - 道具
        recipes.add(createItemRecipe("recipe_item_1", "强化石(1级)", "workshop", 1, "enhanceStone1", 5, 200, 100, 0, 0));
        recipes.add(createItemRecipe("recipe_item_2", "强化石(2级)", "workshop", 3, "enhanceStone2", 3, 500, 300, 0, 0));
        recipes.add(createItemRecipe("recipe_item_3", "强化石(3级)", "workshop", 5, "enhanceStone3", 2, 1000, 600, 0, 0));
        recipes.add(createItemRecipe("recipe_item_4", "初级招贤令", "workshop", 2, "juniorToken", 1, 300, 0, 200, 100));
        recipes.add(createItemRecipe("recipe_item_5", "中级招贤令", "workshop", 5, "intermediateToken", 1, 800, 0, 500, 300));
        recipes.add(createItemRecipe("recipe_item_6", "初级粮食", "workshop", 1, "basicFood", 10, 100, 0, 200, 0));
        recipes.add(createItemRecipe("recipe_item_7", "中级粮食", "workshop", 3, "advancedFood", 5, 300, 0, 500, 0));
        recipes.add(createItemRecipe("recipe_item_8", "合成符", "workshop", 4, "mergeScroll", 2, 400, 200, 0, 200));
        
        // 讲武堂配方 - 兵法
        recipes.add(createTacticsRecipe("recipe_tactics_1", "兵法残卷", "academy", 1, "tacticsScroll1", 1, 0, 0, 300, 500));
        recipes.add(createTacticsRecipe("recipe_tactics_2", "兵法秘籍", "academy", 3, "tacticsScroll2", 1, 0, 0, 600, 1000));
        recipes.add(createTacticsRecipe("recipe_tactics_3", "兵法宝典", "academy", 5, "tacticsScroll3", 1, 0, 0, 1200, 2000));
        recipes.add(createTacticsRecipe("recipe_tactics_4", "初级经验书", "academy", 2, "expBook1", 3, 0, 0, 400, 300));
        recipes.add(createTacticsRecipe("recipe_tactics_5", "中级经验书", "academy", 4, "expBook2", 2, 0, 0, 800, 600));
        
        log.info("初始化制造配方完成，共 {} 个配方", recipes.size());
    }
    
    private Recipe createEquipmentRecipe(String id, String name, String facility, int level, 
                                         String slot, String quality, long silver, long metal, long food, long paper) {
        return Recipe.builder()
                .id(id)
                .name(name)
                .facilityType(facility)
                .requiredLevel(level)
                .resultType("equipment")
                .resultId(slot + "_" + quality.toLowerCase())
                .resultName(name)
                .resultCount(1)
                .quality(quality)
                .icon("/images/equip_" + slot + ".png")
                .description("制造一件" + quality + "品质的" + name)
                .costSilver(silver)
                .costMetal(metal)
                .costFood(food)
                .costPaper(paper)
                .costTime(60)
                .build();
    }
    
    private Recipe createItemRecipe(String id, String name, String facility, int level,
                                    String resultId, int count, long silver, long metal, long food, long paper) {
        return Recipe.builder()
                .id(id)
                .name(name)
                .facilityType(facility)
                .requiredLevel(level)
                .resultType("item")
                .resultId(resultId)
                .resultName(name)
                .resultCount(count)
                .quality("道具")
                .icon("/images/item_" + resultId + ".png")
                .description("制造" + count + "个" + name)
                .costSilver(silver)
                .costMetal(metal)
                .costFood(food)
                .costPaper(paper)
                .costTime(30)
                .build();
    }
    
    private Recipe createTacticsRecipe(String id, String name, String facility, int level,
                                       String resultId, int count, long silver, long metal, long food, long paper) {
        return Recipe.builder()
                .id(id)
                .name(name)
                .facilityType(facility)
                .requiredLevel(level)
                .resultType("tactics")
                .resultId(resultId)
                .resultName(name)
                .resultCount(count)
                .quality("兵法")
                .icon("/images/tactics_" + resultId + ".png")
                .description("制造" + count + "个" + name)
                .costSilver(silver)
                .costMetal(metal)
                .costFood(food)
                .costPaper(paper)
                .costTime(45)
                .build();
    }
    
    /**
     * 获取用户生产数据
     */
    public Production getProduction(String odUserId) {
        return productionStore.computeIfAbsent(odUserId, Production::createDefault);
    }
    
    /**
     * 生产资源
     */
    public Map<String, Object> produce(String odUserId, String facilityType) {
        Production production = getProduction(odUserId);
        Facility facility = getFacility(production, facilityType);
        
        if (facility == null) {
            throw new BusinessException("设施不存在");
        }
        
        // 检查并重置每日次数
        resetDailyIfNeeded(facility);
        
        // 检查次数
        if (facility.getUsedToday() >= facility.getDailyLimit()) {
            throw new BusinessException("今日生产次数已用完");
        }
        
        // 计算产量
        int output = facility.getOutputPerTime();
        
        // 更新用户资源
        UserResource resource = userResourceService.getUserResource(odUserId);
        switch (facilityType) {
            case "silver":
                resource.setSilver(resource.getSilver() + output);
                break;
            case "metal":
                resource.setMetal(resource.getMetal() + output);
                break;
            case "food":
                resource.setFood(resource.getFood() + output);
                break;
            case "paper":
                resource.setPaper(resource.getPaper() + output);
                break;
            default:
                throw new BusinessException("未知的设施类型");
        }
        
        // 更新使用次数
        facility.setUsedToday(facility.getUsedToday() + 1);
        
        log.info("用户 {} 使用 {} 生产了 {} 资源", odUserId, facility.getName(), output);
        
        Map<String, Object> result = new HashMap<>();
        result.put("output", output);
        result.put("facilityType", facilityType);
        result.put("remainingTimes", facility.getDailyLimit() - facility.getUsedToday());
        result.put("resource", resource);
        return result;
    }
    
    /**
     * 升级生产设施
     */
    public Map<String, Object> upgradeFacility(String odUserId, String facilityType) {
        Production production = getProduction(odUserId);
        Facility facility = getFacility(production, facilityType);
        
        if (facility == null) {
            throw new BusinessException("设施不存在");
        }
        
        if (facility.getLevel() >= facility.getMaxLevel()) {
            throw new BusinessException("设施已达最大等级");
        }
        
        // 检查资源
        UserResource resource = userResourceService.getUserResource(odUserId);
        if (resource.getSilver() < facility.getUpgradeSilver() ||
            resource.getMetal() < facility.getUpgradeMetal() ||
            resource.getFood() < facility.getUpgradeFood() ||
            resource.getPaper() < facility.getUpgradePaper()) {
            throw new BusinessException("资源不足");
        }
        
        // 扣除资源
        resource.setSilver(resource.getSilver() - facility.getUpgradeSilver());
        resource.setMetal(resource.getMetal() - facility.getUpgradeMetal());
        resource.setFood(resource.getFood() - facility.getUpgradeFood());
        resource.setPaper(resource.getPaper() - facility.getUpgradePaper());
        
        // 升级
        int newLevel = facility.getLevel() + 1;
        facility.setLevel(newLevel);
        
        // 更新产量和次数上限
        int baseOutput = getBaseOutput(facilityType);
        int baseLimit = getBaseLimit(facilityType);
        facility.setOutputPerTime(baseOutput + newLevel * 20);
        facility.setDailyLimit(baseLimit + newLevel * 10);
        
        // 更新升级费用
        facility.setUpgradeSilver(1000L * newLevel * newLevel);
        facility.setUpgradeMetal(500L * newLevel * newLevel);
        facility.setUpgradeFood(500L * newLevel * newLevel);
        facility.setUpgradePaper(200L * newLevel * newLevel);
        
        log.info("用户 {} 升级 {} 到 {} 级", odUserId, facility.getName(), newLevel);
        
        Map<String, Object> result = new HashMap<>();
        result.put("facility", facility);
        result.put("resource", resource);
        return result;
    }
    
    /**
     * 升级制造设施
     */
    public Map<String, Object> upgradeManufactureFacility(String odUserId, String facilityType) {
        Production production = getProduction(odUserId);
        ManufactureFacility facility = getManufactureFacility(production, facilityType);
        
        if (facility == null) {
            throw new BusinessException("设施不存在");
        }
        
        if (facility.getLevel() >= facility.getMaxLevel()) {
            throw new BusinessException("设施已达最大等级");
        }
        
        // 检查资源
        UserResource resource = userResourceService.getUserResource(odUserId);
        if (resource.getSilver() < facility.getUpgradeSilver() ||
            resource.getMetal() < facility.getUpgradeMetal() ||
            resource.getFood() < facility.getUpgradeFood() ||
            resource.getPaper() < facility.getUpgradePaper()) {
            throw new BusinessException("资源不足");
        }
        
        // 扣除资源
        resource.setSilver(resource.getSilver() - facility.getUpgradeSilver());
        resource.setMetal(resource.getMetal() - facility.getUpgradeMetal());
        resource.setFood(resource.getFood() - facility.getUpgradeFood());
        resource.setPaper(resource.getPaper() - facility.getUpgradePaper());
        
        // 升级
        int newLevel = facility.getLevel() + 1;
        facility.setLevel(newLevel);
        
        // 更新升级费用
        facility.setUpgradeSilver(2000L * newLevel * newLevel);
        facility.setUpgradeMetal(1000L * newLevel * newLevel);
        facility.setUpgradeFood(1000L * newLevel * newLevel);
        facility.setUpgradePaper(500L * newLevel * newLevel);
        
        log.info("用户 {} 升级 {} 到 {} 级", odUserId, facility.getName(), newLevel);
        
        Map<String, Object> result = new HashMap<>();
        result.put("facility", facility);
        result.put("resource", resource);
        return result;
    }
    
    /**
     * 获取配方列表
     */
    public List<Recipe> getRecipes(String facilityType, Integer facilityLevel) {
        List<Recipe> result = new ArrayList<>();
        for (Recipe recipe : recipes) {
            if (recipe.getFacilityType().equals(facilityType)) {
                // 添加是否可制造标记
                result.add(recipe);
            }
        }
        return result;
    }
    
    /**
     * 制造物品
     */
    public Map<String, Object> manufacture(String odUserId, String recipeId) {
        Recipe recipe = recipes.stream()
                .filter(r -> r.getId().equals(recipeId))
                .findFirst()
                .orElseThrow(() -> new BusinessException("配方不存在"));
        
        Production production = getProduction(odUserId);
        ManufactureFacility facility = getManufactureFacility(production, recipe.getFacilityType());
        
        if (facility == null) {
            throw new BusinessException("设施不存在");
        }
        
        // 检查设施等级
        if (facility.getLevel() < recipe.getRequiredLevel()) {
            throw new BusinessException("设施等级不足，需要" + recipe.getRequiredLevel() + "级");
        }
        
        // 检查资源
        UserResource resource = userResourceService.getUserResource(odUserId);
        if (resource.getSilver() < recipe.getCostSilver() ||
            resource.getMetal() < recipe.getCostMetal() ||
            resource.getFood() < recipe.getCostFood() ||
            resource.getPaper() < recipe.getCostPaper()) {
            throw new BusinessException("资源不足");
        }
        
        // 扣除资源
        resource.setSilver(resource.getSilver() - recipe.getCostSilver());
        resource.setMetal(resource.getMetal() - recipe.getCostMetal());
        resource.setFood(resource.getFood() - recipe.getCostFood());
        resource.setPaper(resource.getPaper() - recipe.getCostPaper());
        
        Map<String, Object> result = new HashMap<>();
        result.put("recipe", recipe);
        result.put("resource", resource);
        
        // 根据类型生成物品
        switch (recipe.getResultType()) {
            case "equipment":
                Equipment equipment = createEquipment(recipe);
                equipmentRepository.save(equipment);
                result.put("equipment", equipment);
                log.info("用户 {} 制造了装备: {}", odUserId, equipment.getName());
                break;
            case "item":
                // 更新用户资源中的道具数量
                addItemToResource(resource, recipe.getResultId(), recipe.getResultCount());
                result.put("itemId", recipe.getResultId());
                result.put("itemCount", recipe.getResultCount());
                log.info("用户 {} 制造了道具: {} x{}", odUserId, recipe.getResultName(), recipe.getResultCount());
                break;
            case "tactics":
                // 兵法类道具
                addItemToResource(resource, recipe.getResultId(), recipe.getResultCount());
                result.put("itemId", recipe.getResultId());
                result.put("itemCount", recipe.getResultCount());
                log.info("用户 {} 制造了兵法: {} x{}", odUserId, recipe.getResultName(), recipe.getResultCount());
                break;
        }
        
        return result;
    }
    
    private Equipment createEquipment(Recipe recipe) {
        Random random = new Random();
        
        // 根据品质设置属性
        int qualityMultiplier = getQualityMultiplier(recipe.getQuality());
        
        Equipment.Attributes attrs = Equipment.Attributes.builder()
                .attack(10 * qualityMultiplier + random.nextInt(5 * qualityMultiplier))
                .defense(8 * qualityMultiplier + random.nextInt(4 * qualityMultiplier))
                .hp(50 * qualityMultiplier + random.nextInt(20 * qualityMultiplier))
                .build();
        
        // 创建槽位类型
        Equipment.SlotType slotType = Equipment.SlotType.builder()
                .id(getSlotId(recipe.getResultId()))
                .name(getEquipmentType(recipe.getResultId()))
                .build();
        
        // 创建品质
        Equipment.Quality quality = Equipment.Quality.builder()
                .id(getQualityId(recipe.getQuality()))
                .name(recipe.getQuality())
                .multiplier((double) qualityMultiplier)
                .build();
        
        // 创建来源
        Equipment.Source source = Equipment.Source.builder()
                .type("CRAFT")
                .name("制造")
                .detail(recipe.getFacilityType())
                .build();
        
        return Equipment.builder()
                .id("equip_" + System.currentTimeMillis() + "_" + random.nextInt(1000))
                .name(recipe.getResultName())
                .slotType(slotType)
                .quality(quality)
                .level(recipe.getRequiredLevel())
                .baseAttributes(attrs)
                .source(source)
                .icon(recipe.getIcon())
                .description(recipe.getDescription())
                .createTime(System.currentTimeMillis())
                .build();
    }
    
    private int getSlotId(String resultId) {
        if (resultId.contains("weapon")) return 1;
        if (resultId.contains("helmet")) return 2;
        if (resultId.contains("armor")) return 3;
        if (resultId.contains("ring")) return 4;
        if (resultId.contains("shoes")) return 5;
        if (resultId.contains("necklace")) return 6;
        return 1;
    }
    
    private int getQualityId(String quality) {
        switch (quality) {
            case "传说": return 6;
            case "史诗": return 5;
            case "精良": return 4;
            case "优秀": return 3;
            case "普通": return 2;
            default: return 1;
        }
    }
    
    private int getQualityMultiplier(String quality) {
        switch (quality) {
            case "传说": return 5;
            case "史诗": return 4;
            case "精良": return 3;
            case "优秀": return 2;
            default: return 1;
        }
    }
    
    private String getEquipmentType(String resultId) {
        if (resultId.contains("weapon")) return "武器";
        if (resultId.contains("armor")) return "铠甲";
        if (resultId.contains("helmet")) return "头盔";
        if (resultId.contains("ring")) return "戒指";
        if (resultId.contains("shoes")) return "鞋子";
        if (resultId.contains("necklace")) return "项链";
        return "其他";
    }
    
    private void addItemToResource(UserResource resource, String itemId, int count) {
        switch (itemId) {
            case "enhanceStone1":
                resource.setEnhanceStone1(resource.getEnhanceStone1() + count);
                break;
            case "enhanceStone2":
                resource.setEnhanceStone2(resource.getEnhanceStone2() + count);
                break;
            case "enhanceStone3":
                resource.setEnhanceStone3(resource.getEnhanceStone3() + count);
                break;
            case "juniorToken":
                resource.setJuniorToken(resource.getJuniorToken() + count);
                break;
            case "intermediateToken":
                resource.setIntermediateToken(resource.getIntermediateToken() + count);
                break;
            case "basicFood":
                resource.setBasicFood(resource.getBasicFood() + count);
                break;
            case "advancedFood":
                resource.setAdvancedFood(resource.getAdvancedFood() + count);
                break;
            case "mergeScroll":
                resource.setMergeScroll(resource.getMergeScroll() + count);
                break;
            // 兵法和经验书暂时不实际添加，只记录
            default:
                log.info("添加道具: {} x{}", itemId, count);
        }
    }
    
    private Facility getFacility(Production production, String type) {
        switch (type) {
            case "silver": return production.getSilverMine();
            case "metal": return production.getMetalMine();
            case "food": return production.getFarm();
            case "paper": return production.getPaperMill();
            default: return null;
        }
    }
    
    private ManufactureFacility getManufactureFacility(Production production, String type) {
        switch (type) {
            case "arsenal": return production.getArsenal();
            case "workshop": return production.getWorkshop();
            case "academy": return production.getAcademy();
            default: return null;
        }
    }
    
    private void resetDailyIfNeeded(Facility facility) {
        String today = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
        if (!today.equals(facility.getLastResetDate())) {
            facility.setUsedToday(0);
            facility.setLastResetDate(today);
        }
    }
    
    private int getBaseOutput(String type) {
        switch (type) {
            case "silver": return 120;
            case "metal": return 80;
            case "food": return 80;
            case "paper": return 80;
            default: return 100;
        }
    }
    
    private int getBaseLimit(String type) {
        switch (type) {
            case "silver": return 300;
            case "metal": return 100;
            case "food": return 200;
            case "paper": return 100;
            default: return 100;
        }
    }
}
