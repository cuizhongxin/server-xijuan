package com.tencent.wxcloudrun.service.production;

import com.tencent.wxcloudrun.dao.ProductionMapper;
import com.tencent.wxcloudrun.exception.BusinessException;
import com.tencent.wxcloudrun.model.Equipment;
import com.tencent.wxcloudrun.model.Production;
import com.tencent.wxcloudrun.model.Production.*;
import com.tencent.wxcloudrun.model.UserLevel;
import com.tencent.wxcloudrun.model.UserResource;
import com.tencent.wxcloudrun.repository.EquipmentRepository;
import com.tencent.wxcloudrun.service.UserResourceService;
import com.tencent.wxcloudrun.service.level.LevelService;
import com.tencent.wxcloudrun.service.warehouse.WarehouseService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.*;

/**
 * 生产制造服务（数据库存储）
 */
@Slf4j
@Service
public class ProductionService {
    
    @Autowired
    private UserResourceService userResourceService;
    
    @Autowired
    private EquipmentRepository equipmentRepository;
    
    @Autowired
    private ProductionMapper productionMapper;

    @Autowired
    private LevelService levelService;

    @Autowired @Lazy
    private WarehouseService warehouseService;
    
    // 制造配方（只读配置，保留内存）
    private final List<Recipe> recipes = new ArrayList<>();
    
    @javax.annotation.PostConstruct
    public void initRecipes() {
        // === 军械局(arsenal) - APK equip配方: 黑铁套/宣武套/折冲套/骁勇套/破俘套 ===
        String[][] equipData = {
            // {id, name, slot, quality, needLvl, silver, metal}
            {"22001","黑铁剑","武器","绿","1","500","800"},
            {"22002","黑铁戒","戒指","绿","1","500","800"},
            {"22003","黑铁项链","项链","绿","1","500","800"},
            {"22004","黑铁铠","铠甲","绿","1","500","800"},
            {"22005","黑铁盔","头盔","绿","1","500","800"},
            {"22006","黑铁靴","鞋子","绿","1","500","800"},
            {"23021","宣武剑","武器","蓝","15","1000","3000"},
            {"23022","宣武戒指","戒指","蓝","15","1000","3000"},
            {"23023","宣武项链","项链","蓝","15","1000","3000"},
            {"23024","宣武铠","铠甲","蓝","15","1000","3000"},
            {"23025","宣武盔","头盔","蓝","15","1000","3000"},
            {"23026","宣武靴","鞋子","蓝","15","1000","3000"},
            {"23031","折冲剑","武器","蓝","30","1500","5000"},
            {"23032","折冲戒指","戒指","蓝","30","1500","5000"},
            {"23033","折冲项链","项链","蓝","30","1500","5000"},
            {"23034","折冲铠","铠甲","蓝","30","1500","5000"},
            {"23035","折冲盔","头盔","蓝","30","1500","5000"},
            {"23036","折冲靴","鞋子","蓝","30","1500","5000"},
            {"23041","骁勇长枪","武器","蓝","50","2000","10000"},
            {"23042","骁勇戒","戒指","蓝","50","2000","10000"},
            {"23043","骁勇项链","项链","蓝","50","2000","10000"},
            {"23044","骁勇之甲","铠甲","蓝","50","2000","10000"},
            {"23045","骁勇盔","头盔","蓝","50","2000","10000"},
            {"23046","骁勇靴","鞋子","蓝","50","2000","10000"},
            {"23051","破俘枪","武器","蓝","70","5000","20000"},
            {"23052","破俘戒","戒指","蓝","70","5000","20000"},
            {"23053","破俘项链","项链","蓝","70","5000","20000"},
            {"23054","破俘甲","铠甲","蓝","70","5000","20000"},
            {"23055","破俘盔","头盔","蓝","70","5000","20000"},
            {"23056","破俘靴","鞋子","蓝","70","5000","20000"}
        };
        for (String[] e : equipData) {
            recipes.add(Recipe.builder()
                .id("equip_" + e[0]).name(e[1]).facilityType("arsenal")
                .requiredLevel(Integer.parseInt(e[4]))
                .resultType("equipment").resultId(e[0]).resultName(e[1]).resultCount(1)
                .quality(e[3]).icon("images/equip/" + e[0] + ".jpg")
                .description(e[2] + " - " + e[1])
                .costSilver(Long.parseLong(e[5])).costMetal(Long.parseLong(e[6]))
                .costFood(0L).costPaper(0L).costTime(60).build());
        }

        // === 奇物坊(workshop) - APK curiosa配方 ===
        // {id, name, quality, needLvl, silver, metal, paper}
        recipes.add(buildItemRecipe("15011", "初级招贤令", "workshop", "绿", 5, 1000, 0, 1000));
        recipes.add(buildItemRecipe("15001", "初级合成符", "workshop", "绿", 10, 1000, 0, 1000));
        recipes.add(buildItemRecipe("15041", "装备分解符", "workshop", "蓝", 15, 500, 0, 500));
        recipes.add(buildItemRecipe("15051", "虎符", "workshop", "蓝", 20, 1000, 1000, 0));
        recipes.add(buildItemRecipe("14001", "1级强化石", "workshop", "绿", 20, 1000, 2000, 0));
        recipes.add(buildItemRecipe("15046", "初级强化转移符", "workshop", "蓝", 20, 5000, 0, 3000));
        recipes.add(buildItemRecipe("15044", "初级传承符", "workshop", "蓝", 20, 20000, 0, 10000));

        // === 讲武堂(academy) - APK warBook配方 ===
        recipes.add(buildTacticsRecipe("warbook_random", "兵法秘卷", "academy", "绿", 1, 500, 1000, "可随机获得一本兵法书，将领装备后可释放兵法。"));
        recipes.add(buildTacticsRecipe("15021", "三十六计", "academy", "绿", 15, 500, 2000, "可提升1、2级兵法"));
        recipes.add(buildTacticsRecipe("15022", "鬼谷兵法", "academy", "蓝", 30, 1000, 5000, "可提升3、4级兵法"));
        recipes.add(buildTacticsRecipe("15023", "太公六韬", "academy", "紫", 50, 2000, 10000, "可提升5、6级兵法"));
        recipes.add(buildTacticsRecipe("15024", "孙子兵法", "academy", "紫", 70, 4000, 20000, "可提升7、8、9级兵法"));
        recipes.add(buildTacticsRecipe("15016", "进阶之书", "academy", "紫", 50, 5000, 20000, "将领进阶道具"));

        log.info("初始化APK制造配方完成，共 {} 个配方", recipes.size());
    }

    private Recipe buildItemRecipe(String itemId, String name, String facility, String quality, int needLvl, long silver, long metal, long paper) {
        return Recipe.builder()
            .id("item_" + itemId).name(name).facilityType(facility)
            .requiredLevel(needLvl).resultType("item").resultId(itemId)
            .resultName(name).resultCount(1).quality(quality)
            .icon("images/item/" + itemId + ".jpg").description(name)
            .costSilver(silver).costMetal(metal).costFood(0L).costPaper(paper)
            .costTime(30).build();
    }

    private Recipe buildTacticsRecipe(String itemId, String name, String facility, String quality, int needLvl, long silver, long paper, String desc) {
        return Recipe.builder()
            .id("tactics_" + itemId).name(name).facilityType(facility)
            .requiredLevel(needLvl).resultType("tactics").resultId(itemId)
            .resultName(name).resultCount(1).quality(quality)
            .icon("images/item/" + itemId + ".jpg").description(desc)
            .costSilver(silver).costMetal(0L).costFood(0L).costPaper(paper)
            .costTime(45).build();
    }
    
    /**
     * 获取用户生产数据
     */
    public Production getProduction(String odUserId) {
        Production production = productionMapper.findByUserId(odUserId);
        if (production != null) {
            return production;
        }
        // 不存在则创建默认数据并保存
        production = Production.createDefault(odUserId);
        productionMapper.upsert(production);
        return production;
    }
    
    private void saveProduction(String odUserId, Production production) {
        productionMapper.upsert(production);
    }
    
    public Map<String, Object> produce(String odUserId, String facilityType) {
        Production production = getProduction(odUserId);
        Facility facility = getFacility(production, facilityType);
        if (facility == null) { throw new BusinessException("设施不存在"); }
        
        resetDailyIfNeeded(facility);
        if (facility.getUsedToday() >= facility.getDailyLimit()) { throw new BusinessException("今日生产次数已用完"); }
        
        int output = facility.getOutputPerTime();
        UserResource resource = userResourceService.getUserResource(odUserId);
        switch (facilityType) {
            case "silver": resource.setSilver(resource.getSilver() + output); break;
            case "metal": resource.setMetal(resource.getMetal() + output); break;
            case "food": resource.setFood(resource.getFood() + output); break;
            case "paper": resource.setPaper(resource.getPaper() + output); break;
            default: throw new BusinessException("未知的设施类型");
        }
        
        facility.setUsedToday(facility.getUsedToday() + 1);
        saveProduction(odUserId, production);
        userResourceService.saveUserResource(resource);
        
        log.info("用户 {} 使用 {} 生产了 {} 资源", odUserId, facility.getName(), output);
        
        Map<String, Object> result = new HashMap<>();
        result.put("output", output);
        result.put("facilityType", facilityType);
        result.put("remainingTimes", facility.getDailyLimit() - facility.getUsedToday());
        result.put("resource", resource);
        return result;
    }
    
    public Map<String, Object> upgradeFacility(String odUserId, String facilityType) {
        Production production = getProduction(odUserId);
        Facility facility = getFacility(production, facilityType);
        if (facility == null) { throw new BusinessException("设施不存在"); }
        if (facility.getLevel() >= facility.getMaxLevel()) { throw new BusinessException("设施已达最大等级"); }
        
        UserResource resource = userResourceService.getUserResource(odUserId);
        if (resource.getSilver() < facility.getUpgradeSilver() || resource.getMetal() < facility.getUpgradeMetal() ||
            resource.getFood() < facility.getUpgradeFood() || resource.getPaper() < facility.getUpgradePaper()) {
            throw new BusinessException("资源不足");
        }
        
        resource.setSilver(resource.getSilver() - facility.getUpgradeSilver());
        resource.setMetal(resource.getMetal() - facility.getUpgradeMetal());
        resource.setFood(resource.getFood() - facility.getUpgradeFood());
        resource.setPaper(resource.getPaper() - facility.getUpgradePaper());
        
        int newLevel = facility.getLevel() + 1;
        facility.setLevel(newLevel);
        int baseOutput = getBaseOutput(facilityType);
        int baseLimit = getBaseLimit(facilityType);
        facility.setOutputPerTime(baseOutput + newLevel * 20);
        facility.setDailyLimit(baseLimit + newLevel * 10);
        facility.setUpgradeSilver(1000L * newLevel * newLevel);
        facility.setUpgradeMetal(500L * newLevel * newLevel);
        facility.setUpgradeFood(500L * newLevel * newLevel);
        facility.setUpgradePaper(200L * newLevel * newLevel);
        
        saveProduction(odUserId, production);
        userResourceService.saveUserResource(resource);
        log.info("用户 {} 升级 {} 到 {} 级", odUserId, facility.getName(), newLevel);
        
        Map<String, Object> result = new HashMap<>();
        result.put("facility", facility);
        result.put("resource", resource);
        return result;
    }
    
    public Map<String, Object> upgradeManufactureFacility(String odUserId, String facilityType) {
        Production production = getProduction(odUserId);
        ManufactureFacility facility = getManufactureFacility(production, facilityType);
        if (facility == null) { throw new BusinessException("设施不存在"); }
        if (facility.getLevel() >= facility.getMaxLevel()) { throw new BusinessException("设施已达最大等级"); }
        
        UserResource resource = userResourceService.getUserResource(odUserId);
        if (resource.getSilver() < facility.getUpgradeSilver() || resource.getMetal() < facility.getUpgradeMetal() ||
            resource.getFood() < facility.getUpgradeFood() || resource.getPaper() < facility.getUpgradePaper()) {
            throw new BusinessException("资源不足");
        }
        
        resource.setSilver(resource.getSilver() - facility.getUpgradeSilver());
        resource.setMetal(resource.getMetal() - facility.getUpgradeMetal());
        resource.setFood(resource.getFood() - facility.getUpgradeFood());
        resource.setPaper(resource.getPaper() - facility.getUpgradePaper());
        
        int newLevel = facility.getLevel() + 1;
        facility.setLevel(newLevel);
        facility.setUpgradeSilver(2000L * newLevel * newLevel);
        facility.setUpgradeMetal(1000L * newLevel * newLevel);
        facility.setUpgradeFood(1000L * newLevel * newLevel);
        facility.setUpgradePaper(500L * newLevel * newLevel);
        
        saveProduction(odUserId, production);
        userResourceService.saveUserResource(resource);
        log.info("用户 {} 升级 {} 到 {} 级", odUserId, facility.getName(), newLevel);
        
        Map<String, Object> result = new HashMap<>();
        result.put("facility", facility);
        result.put("resource", resource);
        return result;
    }
    
    public List<Recipe> getRecipes(String facilityType, Integer facilityLevel) {
        List<Recipe> result = new ArrayList<>();
        for (Recipe recipe : recipes) {
            if (recipe.getFacilityType().equals(facilityType)) { result.add(recipe); }
        }
        return result;
    }
    
    public Map<String, Object> manufacture(String odUserId, String recipeId) {
        Recipe recipe = recipes.stream().filter(r -> r.getId().equals(recipeId)).findFirst().orElseThrow(() -> new BusinessException("配方不存在"));
        
        UserLevel userLevel = levelService.getUserLevel(odUserId);
        int playerLevel = (userLevel != null && userLevel.getLevel() != null) ? userLevel.getLevel() : 1;
        if (playerLevel < recipe.getRequiredLevel()) {
            throw new BusinessException("君主等级不够，需要" + recipe.getRequiredLevel() + "级（当前" + playerLevel + "级）");
        }
        
        UserResource resource = userResourceService.getUserResource(odUserId);
        if (resource.getSilver() < recipe.getCostSilver() || resource.getMetal() < recipe.getCostMetal() ||
            resource.getFood() < recipe.getCostFood() || resource.getPaper() < recipe.getCostPaper()) {
            throw new BusinessException("资源不足");
        }
        
        resource.setSilver(resource.getSilver() - recipe.getCostSilver());
        resource.setMetal(resource.getMetal() - recipe.getCostMetal());
        resource.setFood(resource.getFood() - recipe.getCostFood());
        resource.setPaper(resource.getPaper() - recipe.getCostPaper());
        
        Map<String, Object> result = new HashMap<>();
        result.put("recipe", recipe);
        result.put("resource", resource);
        
        switch (recipe.getResultType()) {
            case "equipment":
                Equipment equipment = createEquipment(odUserId, recipe);
                equipmentRepository.save(equipment);
                try { warehouseService.addEquipment(odUserId, equipment.getId()); }
                catch (Exception e) { log.warn("装备加入仓库失败: {}", e.getMessage()); }
                result.put("equipment", equipment);
                log.info("用户 {} 制造了装备: {}", odUserId, equipment.getName());
                break;
            case "item":
                addItemToResource(resource, recipe.getResultId(), recipe.getResultCount());
                result.put("itemId", recipe.getResultId());
                result.put("itemCount", recipe.getResultCount());
                log.info("用户 {} 制造了道具: {} x{}", odUserId, recipe.getResultName(), recipe.getResultCount());
                break;
            case "tactics":
                addItemToResource(resource, recipe.getResultId(), recipe.getResultCount());
                result.put("itemId", recipe.getResultId());
                result.put("itemCount", recipe.getResultCount());
                log.info("用户 {} 制造了兵法: {} x{}", odUserId, recipe.getResultName(), recipe.getResultCount());
                break;
        }
        
        userResourceService.saveUserResource(resource);
        return result;
    }
    
    // ========== APK 装备穿戴等级 + 基础属性 (equipInfo_cfg.json) ==========
    private static final Map<String, int[]> EQUIP_INFO = new HashMap<>();
    // key=装备ID, value={needLevel, genAtt, genDef, genFor, genLeader, armyLife}
    static {
        // 黑铁套 (color=2绿, 无套装)
        EQUIP_INFO.put("22001", new int[]{1,  70, 0,  0, 0, 0});
        EQUIP_INFO.put("22002", new int[]{5,  50, 0,  0, 0, 0});
        EQUIP_INFO.put("22003", new int[]{10, 30, 0,  0, 0, 0});
        EQUIP_INFO.put("22004", new int[]{1,  0,  80, 0, 0, 0});
        EQUIP_INFO.put("22005", new int[]{5,  0,  60, 0, 0, 0});
        EQUIP_INFO.put("22006", new int[]{10, 0,  40, 0, 0, 0});
        // 宣武套 (color=3蓝, suit=2)
        EQUIP_INFO.put("23021", new int[]{20, 150, 0,   0, 0, 0});
        EQUIP_INFO.put("23022", new int[]{20, 120, 0,   0, 0, 0});
        EQUIP_INFO.put("23023", new int[]{20, 90,  0,   0, 0, 0});
        EQUIP_INFO.put("23024", new int[]{20, 0,   180, 0, 0, 0});
        EQUIP_INFO.put("23025", new int[]{20, 0,   140, 0, 0, 0});
        EQUIP_INFO.put("23026", new int[]{20, 0,   100, 0, 0, 0});
        // 折冲套 (color=3蓝, suit=3)
        EQUIP_INFO.put("23031", new int[]{40, 200, 0,   0, 0, 0});
        EQUIP_INFO.put("23032", new int[]{40, 170, 0,   0, 0, 0});
        EQUIP_INFO.put("23033", new int[]{40, 130, 0,   0, 0, 0});
        EQUIP_INFO.put("23034", new int[]{40, 0,   230, 0, 0, 0});
        EQUIP_INFO.put("23035", new int[]{40, 0,   190, 0, 0, 0});
        EQUIP_INFO.put("23036", new int[]{40, 0,   150, 0, 0, 0});
        // 骁勇套 (color=3蓝, suit=4)
        EQUIP_INFO.put("23041", new int[]{60, 280, 0,   0, 0, 0});
        EQUIP_INFO.put("23042", new int[]{60, 220, 0,   0, 0, 0});
        EQUIP_INFO.put("23043", new int[]{60, 160, 0,   0, 0, 0});
        EQUIP_INFO.put("23044", new int[]{60, 0,   305, 0, 0, 0});
        EQUIP_INFO.put("23045", new int[]{60, 0,   245, 0, 0, 0});
        EQUIP_INFO.put("23046", new int[]{60, 0,   190, 0, 0, 0});
        // 破俘套 (color=3蓝, suit=5)
        EQUIP_INFO.put("23051", new int[]{80, 335, 0,   0, 0, 0});
        EQUIP_INFO.put("23052", new int[]{80, 270, 0,   0, 0, 0});
        EQUIP_INFO.put("23053", new int[]{80, 215, 0,   0, 0, 0});
        EQUIP_INFO.put("23054", new int[]{80, 0,   390, 0, 0, 0});
        EQUIP_INFO.put("23055", new int[]{80, 0,   305, 0, 0, 0});
        EQUIP_INFO.put("23056", new int[]{80, 0,   225, 0, 0, 0});
    }

    // ========== APK 装备品质系统 (EquipQuality_cfg.json) ==========
    // 品质ID -> {名称, attrRate万分比, acquireRate万分比}
    private static final String[] QUALITY_NAMES = {"", "粗糙", "普通", "优良", "无暇", "完美"};
    private static final int[] QUALITY_ATTR_RATE = {0, 8000, 8500, 9000, 9500, 10000};
    private static final int[] QUALITY_ACQUIRE_RATE = {0, 5890, 3000, 1000, 100, 10};
    // 累计权重: 5890, 8890, 9890, 9990, 10000

    /**
     * 按APK概率随机生成品质 (1=粗糙, 2=普通, 3=优良, 4=无暇, 5=完美)
     */
    private int rollEquipQuality(Random random) {
        int roll = random.nextInt(10000); // 0~9999
        int cumulative = 0;
        for (int i = 1; i <= 5; i++) {
            cumulative += QUALITY_ACQUIRE_RATE[i];
            if (roll < cumulative) return i;
        }
        return 1;
    }

    private Equipment createEquipment(String userId, Recipe recipe) {
        Random random = new Random();
        String equipId = recipe.getResultId();

        // 1. 按APK概率随机品质
        int qualityLevel = rollEquipQuality(random);
        String qualityPrefix = QUALITY_NAMES[qualityLevel];
        int attrRate = QUALITY_ATTR_RATE[qualityLevel]; // 万分比

        // 2. 品质前缀 + 原名 (如 "普通·宣武剑")
        String displayName = qualityPrefix + "·" + recipe.getResultName();

        // 3. 从APK equipInfo取穿戴等级和基础属性
        int[] info = EQUIP_INFO.get(equipId);
        int wearLevel = (info != null) ? info[0] : 1;
        int baseAtt = (info != null) ? info[1] : 0;
        int baseDef = (info != null) ? info[2] : 0;

        // 4. 属性 = 基础属性 × 品质比率 / 10000
        int finalAtt = (int)(baseAtt * attrRate / 10000.0);
        int finalDef = (int)(baseDef * attrRate / 10000.0);

        Equipment.Attributes attrs = Equipment.Attributes.builder()
            .attack(finalAtt).defense(finalDef).hp(0).build();

        int slotId = getSlotIdFromApkId(equipId);
        Equipment.SlotType slotType = Equipment.SlotType.builder()
            .id(slotId).name(getSlotNameFromId(slotId)).build();

        // 品质颜色名 = 装备本身的颜色品质(绿/蓝/紫/橙), 不是品质前缀
        Equipment.Quality quality = Equipment.Quality.builder()
            .id(getQualityId(recipe.getQuality())).name(recipe.getQuality())
            .multiplier(attrRate / 10000.0).build();

        Equipment.Source source = Equipment.Source.builder()
            .type("CRAFT").name("制造").detail(recipe.getFacilityType()).build();

        return Equipment.builder()
            .id("equip_" + System.currentTimeMillis() + "_" + random.nextInt(1000))
            .userId(userId)
            .name(displayName).slotType(slotType).quality(quality)
            .level(wearLevel).baseAttributes(attrs).source(source)
            .icon(recipe.getIcon()).description(recipe.getDescription())
            .qualityValue(qualityLevel)
            .createTime(System.currentTimeMillis()).build();
    }

    // APK装备ID的末位数决定部位: x1=武器,x2=戒指,x3=项链,x4=铠甲,x5=头盔,x6=鞋子
    private int getSlotIdFromApkId(String apkId) {
        try {
            int id = Integer.parseInt(apkId);
            int lastDigit = id % 10;
            if (lastDigit >= 1 && lastDigit <= 6) return lastDigit;
        } catch (NumberFormatException ignored) {}
        return 1;
    }
    private String getSlotNameFromId(int slotId) {
        switch (slotId) { case 1: return "武器"; case 2: return "戒指"; case 3: return "项链"; case 4: return "铠甲"; case 5: return "头盔"; case 6: return "鞋子"; default: return "其他"; }
    }
    private int getQualityId(String quality) {
        switch (quality) { case "橙": return 5; case "紫": return 4; case "蓝": return 3; case "绿": return 2; default: return 1; }
    }
    private int getQualityMultiplier(String quality) {
        switch (quality) { case "橙": return 4; case "紫": return 3; case "蓝": return 2; case "绿": return 1; default: return 1; }
    }
    
    private void addItemToResource(UserResource resource, String itemId, int count) {
        switch (itemId) {
            case "14001": resource.setEnhanceStone1(resource.getEnhanceStone1() + count); break;
            case "15011": resource.setJuniorToken(resource.getJuniorToken() + count); break;
            case "15001": resource.setMergeScroll(resource.getMergeScroll() + count); break;
            case "15041": log.info("获得装备分解符 x{}", count); break;
            case "15051": log.info("获得虎符 x{}", count); break;
            case "15046": log.info("获得初级强化转移符 x{}", count); break;
            case "15044": log.info("获得初级传承符 x{}", count); break;
            case "15021": case "15022": case "15023": case "15024":
                log.info("获得兵法书 {} x{}", itemId, count); break;
            case "15016": log.info("获得进阶之书 x{}", count); break;
            case "warbook_random": log.info("获得随机兵法秘卷 x{}", count); break;
            default: log.info("添加道具: {} x{}", itemId, count);
        }
    }
    
    public int getPlayerLevel(String odUserId) {
        UserLevel userLevel = levelService.getUserLevel(odUserId);
        return (userLevel != null && userLevel.getLevel() != null) ? userLevel.getLevel() : 1;
    }

    private Facility getFacility(Production production, String type) {
        switch (type) { case "silver": return production.getSilverMine(); case "metal": return production.getMetalMine(); case "food": return production.getFarm(); case "paper": return production.getPaperMill(); default: return null; }
    }
    private ManufactureFacility getManufactureFacility(Production production, String type) {
        switch (type) { case "arsenal": return production.getArsenal(); case "workshop": return production.getWorkshop(); case "academy": return production.getAcademy(); default: return null; }
    }
    private void resetDailyIfNeeded(Facility facility) {
        String today = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
        if (!today.equals(facility.getLastResetDate())) { facility.setUsedToday(0); facility.setLastResetDate(today); }
    }
    private int getBaseOutput(String type) {
        switch (type) { case "silver": return 120; case "metal": return 80; case "food": return 80; case "paper": return 80; default: return 100; }
    }
    private int getBaseLimit(String type) {
        switch (type) { case "silver": return 300; case "metal": return 100; case "food": return 200; case "paper": return 100; default: return 100; }
    }
}
