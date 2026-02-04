package com.tencent.wxcloudrun.service.training;

import com.tencent.wxcloudrun.exception.BusinessException;
import com.tencent.wxcloudrun.model.General;
import com.tencent.wxcloudrun.model.UserResource;
import com.tencent.wxcloudrun.repository.GeneralRepository;
import com.tencent.wxcloudrun.service.UserResourceService;
import com.tencent.wxcloudrun.service.level.LevelService;
import com.tencent.wxcloudrun.service.general.GeneralService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 训练服务 - 消耗粮食训练武将
 * 
 * 训练模式：
 * 1. 主公练兵：消耗粮食，提升更多主公经验，少量武将经验
 * 2. 武将练兵：消耗粮食，提升更多武将经验，少量主公经验
 * 
 * 粮食等级：
 * - 初级粮食：10主公经验 + 5武将经验 或 5主公经验 + 10武将经验
 * - 中级粮食：50主公经验 + 25武将经验 或 25主公经验 + 50武将经验
 * - 高级粮食：200主公经验 + 100武将经验 或 100主公经验 + 200武将经验
 */
@Service
public class TrainingService {
    
    private static final Logger logger = LoggerFactory.getLogger(TrainingService.class);
    
    @Autowired
    private UserResourceService userResourceService;
    
    @Autowired
    private LevelService levelService;
    
    @Autowired
    private GeneralService generalService;
    
    @Autowired
    private GeneralRepository generalRepository;
    
    // 训练类型
    public static final String TRAINING_LORD = "lord";      // 主公练兵
    public static final String TRAINING_GENERAL = "general"; // 武将练兵
    
    // 粮食等级
    public static final String FOOD_BASIC = "basic";        // 初级粮食
    public static final String FOOD_ADVANCED = "advanced";  // 中级粮食
    public static final String FOOD_PREMIUM = "premium";    // 高级粮食
    
    // 经验配置
    private static final Map<String, int[]> FOOD_EXP_CONFIG = new HashMap<>();
    
    static {
        // [主公练兵时主公经验, 主公练兵时武将经验, 武将练兵时主公经验, 武将练兵时武将经验]
        FOOD_EXP_CONFIG.put(FOOD_BASIC, new int[]{10, 5, 5, 10});
        FOOD_EXP_CONFIG.put(FOOD_ADVANCED, new int[]{50, 25, 25, 50});
        FOOD_EXP_CONFIG.put(FOOD_PREMIUM, new int[]{200, 100, 100, 200});
    }
    
    /**
     * 获取训练配置
     */
    public Map<String, Object> getTrainingConfig() {
        Map<String, Object> config = new HashMap<>();
        
        // 训练类型配置
        List<Map<String, Object>> trainingTypes = new ArrayList<>();
        
        Map<String, Object> lordTraining = new HashMap<>();
        lordTraining.put("id", TRAINING_LORD);
        lordTraining.put("name", "主公练兵");
        lordTraining.put("description", "消耗粮食，获得更多主公经验，武将获得少量经验");
        lordTraining.put("icon", "👑");
        trainingTypes.add(lordTraining);
        
        Map<String, Object> generalTraining = new HashMap<>();
        generalTraining.put("id", TRAINING_GENERAL);
        generalTraining.put("name", "武将练兵");
        generalTraining.put("description", "消耗粮食，武将获得更多经验，主公获得少量经验");
        generalTraining.put("icon", "⚔️");
        trainingTypes.add(generalTraining);
        
        config.put("trainingTypes", trainingTypes);
        
        // 粮食等级配置
        List<Map<String, Object>> foodGrades = new ArrayList<>();
        
        Map<String, Object> basicFood = new HashMap<>();
        basicFood.put("id", FOOD_BASIC);
        basicFood.put("name", "初级粮食");
        basicFood.put("icon", "🍚");
        basicFood.put("lordExpForLord", 10);
        basicFood.put("generalExpForLord", 5);
        basicFood.put("lordExpForGeneral", 5);
        basicFood.put("generalExpForGeneral", 10);
        basicFood.put("quality", "green");
        foodGrades.add(basicFood);
        
        Map<String, Object> advancedFood = new HashMap<>();
        advancedFood.put("id", FOOD_ADVANCED);
        advancedFood.put("name", "中级粮食");
        advancedFood.put("icon", "🍱");
        advancedFood.put("lordExpForLord", 50);
        advancedFood.put("generalExpForLord", 25);
        advancedFood.put("lordExpForGeneral", 25);
        advancedFood.put("generalExpForGeneral", 50);
        advancedFood.put("quality", "blue");
        foodGrades.add(advancedFood);
        
        Map<String, Object> premiumFood = new HashMap<>();
        premiumFood.put("id", FOOD_PREMIUM);
        premiumFood.put("name", "高级粮食");
        premiumFood.put("icon", "🍲");
        premiumFood.put("lordExpForLord", 200);
        premiumFood.put("generalExpForLord", 100);
        premiumFood.put("lordExpForGeneral", 100);
        premiumFood.put("generalExpForGeneral", 200);
        premiumFood.put("quality", "purple");
        foodGrades.add(premiumFood);
        
        config.put("foodGrades", foodGrades);
        
        return config;
    }
    
    /**
     * 获取用户粮食数量
     */
    public Map<String, Object> getUserFood(String userId) {
        UserResource resource = userResourceService.getUserResource(userId);
        
        Map<String, Object> result = new HashMap<>();
        result.put("basicFood", resource.getBasicFood() != null ? resource.getBasicFood() : 0);
        result.put("advancedFood", resource.getAdvancedFood() != null ? resource.getAdvancedFood() : 0);
        result.put("premiumFood", resource.getPremiumFood() != null ? resource.getPremiumFood() : 0);
        
        return result;
    }
    
    /**
     * 执行训练
     * 
     * @param userId 用户ID
     * @param generalId 武将ID
     * @param trainingType 训练类型: lord/general
     * @param foodGrade 粮食等级: basic/advanced/premium
     * @param count 消耗数量
     */
    public Map<String, Object> train(String userId, String generalId, String trainingType, 
                                     String foodGrade, int count) {
        // 验证参数
        if (!TRAINING_LORD.equals(trainingType) && !TRAINING_GENERAL.equals(trainingType)) {
            throw new BusinessException(400, "无效的训练类型");
        }
        
        int[] expConfig = FOOD_EXP_CONFIG.get(foodGrade);
        if (expConfig == null) {
            throw new BusinessException(400, "无效的粮食等级");
        }
        
        if (count <= 0 || count > 100) {
            throw new BusinessException(400, "训练数量必须在1-100之间");
        }
        
        // 检查武将
        General general = generalRepository.findById(generalId);
        if (general == null) {
            throw new BusinessException(400, "武将不存在");
        }
        if (!userId.equals(general.getUserId())) {
            throw new BusinessException(403, "无权操作此武将");
        }
        
        // 检查并消耗粮食
        UserResource resource = userResourceService.getUserResource(userId);
        int currentFood = getFoodCount(resource, foodGrade);
        
        if (currentFood < count) {
            throw new BusinessException(400, "粮食不足，当前有" + currentFood + "个，需要" + count + "个");
        }
        
        // 扣除粮食
        consumeFood(resource, foodGrade, count);
        userResourceService.saveUserResource(resource);
        
        // 计算经验
        int lordExp, generalExp;
        if (TRAINING_LORD.equals(trainingType)) {
            lordExp = expConfig[0] * count;
            generalExp = expConfig[1] * count;
        } else {
            lordExp = expConfig[2] * count;
            generalExp = expConfig[3] * count;
        }
        
        // 增加主公经验
        Map<String, Object> lordExpResult = levelService.addDungeonExp(userId, lordExp, false);
        
        // 增加武将经验
        Map<String, Object> generalExpResult = generalService.addGeneralExp(generalId, generalExp);
        
        logger.info("用户 {} 训练武将 {}，类型：{}，粮食等级：{}，数量：{}，主公经验+{}，武将经验+{}",
            userId, general.getName(), trainingType, foodGrade, count, lordExp, generalExp);
        
        // 构建返回结果
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("trainingType", trainingType);
        result.put("foodGrade", foodGrade);
        result.put("consumedFood", count);
        result.put("remainingFood", getFoodCount(resource, foodGrade));
        
        // 主公经验结果
        Map<String, Object> lordResult = new HashMap<>();
        lordResult.put("expGained", lordExp);
        lordResult.put("levelUp", lordExpResult.get("levelUp"));
        lordResult.put("currentLevel", lordExpResult.get("currentLevel"));
        lordResult.put("currentExp", lordExpResult.get("currentLevelExp"));
        lordResult.put("expToNextLevel", lordExpResult.get("expToNextLevel"));
        result.put("lordExpResult", lordResult);
        
        // 武将经验结果
        Map<String, Object> generalResult = new HashMap<>();
        generalResult.put("generalId", generalId);
        generalResult.put("generalName", general.getName());
        generalResult.put("expGained", generalExp);
        generalResult.put("levelUp", generalExpResult.get("levelUp"));
        generalResult.put("levelsGained", generalExpResult.get("levelsGained"));
        generalResult.put("currentLevel", generalExpResult.get("currentLevel"));
        generalResult.put("currentExp", generalExpResult.get("currentExp"));
        generalResult.put("expToNextLevel", generalExpResult.get("expToNextLevel"));
        result.put("generalExpResult", generalResult);
        
        return result;
    }
    
    /**
     * 批量训练（训练多个武将）
     */
    public Map<String, Object> trainBatch(String userId, List<String> generalIds, 
                                          String trainingType, String foodGrade, int countPerGeneral) {
        if (generalIds == null || generalIds.isEmpty()) {
            throw new BusinessException(400, "请选择要训练的武将");
        }
        
        if (generalIds.size() > 6) {
            throw new BusinessException(400, "一次最多训练6个武将");
        }
        
        int totalFood = countPerGeneral * generalIds.size();
        
        // 检查粮食是否足够
        UserResource resource = userResourceService.getUserResource(userId);
        int currentFood = getFoodCount(resource, foodGrade);
        
        if (currentFood < totalFood) {
            throw new BusinessException(400, "粮食不足，需要" + totalFood + "个，当前有" + currentFood + "个");
        }
        
        // 执行批量训练
        List<Map<String, Object>> trainResults = new ArrayList<>();
        for (String generalId : generalIds) {
            try {
                Map<String, Object> trainResult = train(userId, generalId, trainingType, foodGrade, countPerGeneral);
                trainResults.add(trainResult);
            } catch (Exception e) {
                logger.error("训练武将 {} 失败: {}", generalId, e.getMessage());
            }
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("trainedCount", trainResults.size());
        result.put("results", trainResults);
        
        return result;
    }
    
    /**
     * 购买粮食
     */
    public Map<String, Object> buyFood(String userId, String foodGrade, int count) {
        if (count <= 0 || count > 999) {
            throw new BusinessException(400, "购买数量必须在1-999之间");
        }
        
        // 价格配置（银两）
        Map<String, Long> priceConfig = new HashMap<>();
        priceConfig.put(FOOD_BASIC, 10L);      // 初级10银两
        priceConfig.put(FOOD_ADVANCED, 50L);   // 中级50银两
        priceConfig.put(FOOD_PREMIUM, 200L);   // 高级200银两
        
        Long unitPrice = priceConfig.get(foodGrade);
        if (unitPrice == null) {
            throw new BusinessException(400, "无效的粮食等级");
        }
        
        long totalCost = unitPrice * count;
        
        // 检查并消耗银两
        UserResource resource = userResourceService.getUserResource(userId);
        if (resource.getSilver() < totalCost) {
            throw new BusinessException(400, "银两不足，需要" + totalCost + "银两");
        }
        
        resource.setSilver(resource.getSilver() - totalCost);
        
        // 增加粮食
        addFood(resource, foodGrade, count);
        userResourceService.saveUserResource(resource);
        
        logger.info("用户 {} 购买了 {} 个 {} 粮食，花费 {} 银两",
            userId, count, foodGrade, totalCost);
        
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("foodGrade", foodGrade);
        result.put("count", count);
        result.put("cost", totalCost);
        result.put("remainingSilver", resource.getSilver());
        result.put("currentFood", getFoodCount(resource, foodGrade));
        
        return result;
    }
    
    private int getFoodCount(UserResource resource, String foodGrade) {
        switch (foodGrade) {
            case FOOD_BASIC:
                return resource.getBasicFood() != null ? resource.getBasicFood() : 0;
            case FOOD_ADVANCED:
                return resource.getAdvancedFood() != null ? resource.getAdvancedFood() : 0;
            case FOOD_PREMIUM:
                return resource.getPremiumFood() != null ? resource.getPremiumFood() : 0;
            default:
                return 0;
        }
    }
    
    private void consumeFood(UserResource resource, String foodGrade, int count) {
        switch (foodGrade) {
            case FOOD_BASIC:
                resource.setBasicFood(resource.getBasicFood() - count);
                break;
            case FOOD_ADVANCED:
                resource.setAdvancedFood(resource.getAdvancedFood() - count);
                break;
            case FOOD_PREMIUM:
                resource.setPremiumFood(resource.getPremiumFood() - count);
                break;
        }
    }
    
    private void addFood(UserResource resource, String foodGrade, int count) {
        switch (foodGrade) {
            case FOOD_BASIC:
                resource.setBasicFood((resource.getBasicFood() != null ? resource.getBasicFood() : 0) + count);
                break;
            case FOOD_ADVANCED:
                resource.setAdvancedFood((resource.getAdvancedFood() != null ? resource.getAdvancedFood() : 0) + count);
                break;
            case FOOD_PREMIUM:
                resource.setPremiumFood((resource.getPremiumFood() != null ? resource.getPremiumFood() : 0) + count);
                break;
        }
    }
}
