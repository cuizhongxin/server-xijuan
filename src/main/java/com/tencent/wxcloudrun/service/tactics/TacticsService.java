package com.tencent.wxcloudrun.service.tactics;

import com.tencent.wxcloudrun.config.TacticsConfig;
import com.tencent.wxcloudrun.exception.BusinessException;
import com.tencent.wxcloudrun.model.General;
import com.tencent.wxcloudrun.model.Tactics;
import com.tencent.wxcloudrun.repository.GeneralRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 兵法服务
 */
@Service
public class TacticsService {
    
    private static final Logger logger = LoggerFactory.getLogger(TacticsService.class);
    
    @Autowired
    private TacticsConfig tacticsConfig;
    
    @Autowired
    private GeneralRepository generalRepository;
    
    // 用户已学习的兵法
    // key: generalId, value: 已学习的兵法列表
    private final Map<String, List<Tactics>> generalLearnedTactics = new ConcurrentHashMap<>();
    
    // 武将装备的兵法
    // key: generalId, value: {primary: Tactics, secondary: Tactics}
    private final Map<String, Map<String, Tactics>> generalEquippedTactics = new ConcurrentHashMap<>();
    
    /**
     * 获取所有兵法列表
     */
    public List<Tactics> getAllTactics() {
        return new ArrayList<>(tacticsConfig.getAllTactics().values());
    }
    
    /**
     * 获取兵法详情
     */
    public Tactics getTacticsById(String tacticsId) {
        return tacticsConfig.getTacticsById(tacticsId);
    }
    
    /**
     * 获取武将已学习的兵法
     */
    public List<Tactics> getGeneralLearnedTactics(String generalId) {
        return generalLearnedTactics.getOrDefault(generalId, new ArrayList<>());
    }
    
    /**
     * 获取武将装备的兵法
     */
    public Map<String, Tactics> getGeneralEquippedTactics(String generalId) {
        return generalEquippedTactics.getOrDefault(generalId, new HashMap<>());
    }
    
    /**
     * 学习兵法
     */
    public Map<String, Object> learnTactics(String userId, String generalId, String tacticsId) {
        // 检查武将
        General general = generalRepository.findById(generalId);
        if (general == null) {
            throw new BusinessException(400, "武将不存在");
        }
        if (!userId.equals(general.getUserId())) {
            throw new BusinessException(403, "无权操作此武将");
        }
        
        // 检查兵法
        Tactics tactics = tacticsConfig.getTacticsById(tacticsId);
        if (tactics == null) {
            throw new BusinessException(400, "兵法不存在");
        }
        
        // 检查等级要求
        int generalLevel = general.getLevel() != null ? general.getLevel() : 1;
        if (generalLevel < tactics.getLearnLevel()) {
            throw new BusinessException(400, "武将等级不足，需要达到" + tactics.getLearnLevel() + "级");
        }
        
        // 检查是否已学习
        List<Tactics> learned = generalLearnedTactics.computeIfAbsent(generalId, k -> new ArrayList<>());
        for (Tactics t : learned) {
            if (t.getId().equals(tacticsId)) {
                throw new BusinessException(400, "已学习该兵法");
            }
        }
        
        // 创建兵法副本
        Tactics learnedTactics = copyTactics(tactics);
        learned.add(learnedTactics);
        
        logger.info("武将 {} 学习了兵法: {}", general.getName(), tactics.getName());
        
        Map<String, Object> result = new HashMap<>();
        result.put("generalId", generalId);
        result.put("generalName", general.getName());
        result.put("tactics", learnedTactics);
        result.put("learnedCount", learned.size());
        
        return result;
    }
    
    /**
     * 装备兵法
     */
    public Map<String, Object> equipTactics(String userId, String generalId, String tacticsId, String slot) {
        // slot: "primary" 或 "secondary"
        if (!"primary".equals(slot) && !"secondary".equals(slot)) {
            throw new BusinessException(400, "无效的兵法槽位");
        }
        
        // 检查武将
        General general = generalRepository.findById(generalId);
        if (general == null) {
            throw new BusinessException(400, "武将不存在");
        }
        if (!userId.equals(general.getUserId())) {
            throw new BusinessException(403, "无权操作此武将");
        }
        
        // 检查是否已学习该兵法
        List<Tactics> learned = generalLearnedTactics.get(generalId);
        Tactics tacticsToEquip = null;
        if (learned != null) {
            for (Tactics t : learned) {
                if (t.getId().equals(tacticsId)) {
                    tacticsToEquip = t;
                    break;
                }
            }
        }
        
        if (tacticsToEquip == null) {
            throw new BusinessException(400, "未学习该兵法");
        }
        
        // 装备兵法
        Map<String, Tactics> equipped = generalEquippedTactics.computeIfAbsent(generalId, k -> new HashMap<>());
        
        // 检查是否在另一个槽位
        String otherSlot = "primary".equals(slot) ? "secondary" : "primary";
        if (equipped.get(otherSlot) != null && equipped.get(otherSlot).getId().equals(tacticsId)) {
            equipped.remove(otherSlot);  // 从另一个槽位移除
        }
        
        equipped.put(slot, tacticsToEquip);
        
        logger.info("武将 {} 装备兵法 {} 到 {} 槽位", general.getName(), tacticsToEquip.getName(), slot);
        
        Map<String, Object> result = new HashMap<>();
        result.put("generalId", generalId);
        result.put("generalName", general.getName());
        result.put("slot", slot);
        result.put("tactics", tacticsToEquip);
        result.put("equippedTactics", equipped);
        
        return result;
    }
    
    /**
     * 卸下兵法
     */
    public Map<String, Object> unequipTactics(String userId, String generalId, String slot) {
        // 检查武将
        General general = generalRepository.findById(generalId);
        if (general == null) {
            throw new BusinessException(400, "武将不存在");
        }
        if (!userId.equals(general.getUserId())) {
            throw new BusinessException(403, "无权操作此武将");
        }
        
        Map<String, Tactics> equipped = generalEquippedTactics.get(generalId);
        if (equipped == null || !equipped.containsKey(slot)) {
            throw new BusinessException(400, "该槽位没有装备兵法");
        }
        
        Tactics removedTactics = equipped.remove(slot);
        
        logger.info("武将 {} 卸下兵法 {}", general.getName(), removedTactics.getName());
        
        Map<String, Object> result = new HashMap<>();
        result.put("generalId", generalId);
        result.put("generalName", general.getName());
        result.put("slot", slot);
        result.put("removedTactics", removedTactics);
        result.put("equippedTactics", equipped);
        
        return result;
    }
    
    /**
     * 升级兵法
     */
    public Map<String, Object> upgradeTactics(String userId, String generalId, String tacticsId, int expToAdd) {
        // 检查武将
        General general = generalRepository.findById(generalId);
        if (general == null) {
            throw new BusinessException(400, "武将不存在");
        }
        if (!userId.equals(general.getUserId())) {
            throw new BusinessException(403, "无权操作此武将");
        }
        
        // 找到已学习的兵法
        List<Tactics> learned = generalLearnedTactics.get(generalId);
        Tactics tactics = null;
        if (learned != null) {
            for (Tactics t : learned) {
                if (t.getId().equals(tacticsId)) {
                    tactics = t;
                    break;
                }
            }
        }
        
        if (tactics == null) {
            throw new BusinessException(400, "未学习该兵法");
        }
        
        if (tactics.getLevel() >= tactics.getMaxLevel()) {
            throw new BusinessException(400, "兵法已满级");
        }
        
        // 添加经验
        int currentExp = tactics.getExp() + expToAdd;
        int currentLevel = tactics.getLevel();
        int levelsGained = 0;
        
        while (currentLevel < tactics.getMaxLevel() && currentExp >= tactics.getMaxExp()) {
            currentExp -= tactics.getMaxExp();
            currentLevel++;
            levelsGained++;
            tactics.setMaxExp((int)(tactics.getMaxExp() * 1.5));  // 每级需要的经验增加50%
        }
        
        tactics.setExp(currentExp);
        tactics.setLevel(currentLevel);
        tactics.setUpdateTime(System.currentTimeMillis());
        
        // 更新效果数值（每级增加10%）
        if (levelsGained > 0) {
            double multiplier = 1.0 + (currentLevel - 1) * 0.1;
            for (Tactics.TacticsEffect effect : tactics.getEffects()) {
                effect.setBaseValue((int)(effect.getBaseValue() * multiplier));
            }
        }
        
        logger.info("武将 {} 的兵法 {} 升级到 {} 级", general.getName(), tactics.getName(), currentLevel);
        
        Map<String, Object> result = new HashMap<>();
        result.put("generalId", generalId);
        result.put("generalName", general.getName());
        result.put("tactics", tactics);
        result.put("levelUp", levelsGained > 0);
        result.put("levelsGained", levelsGained);
        result.put("currentLevel", currentLevel);
        result.put("currentExp", currentExp);
        result.put("maxExp", tactics.getMaxExp());
        
        return result;
    }
    
    /**
     * 初始化武将的固有兵法（根据品质）
     */
    public void initGeneralTactics(General general) {
        String generalId = general.getId();
        int qualityId = general.getQuality() != null ? general.getQuality().getId() : 1;
        
        // 根据武将品质分配初始兵法
        List<Tactics> allTactics = new ArrayList<>(tacticsConfig.getAllTactics().values());
        List<Tactics> availableTactics = new ArrayList<>();
        
        // 筛选适合品质的兵法（品质相同或低一级）
        for (Tactics t : allTactics) {
            int tacticsQuality = t.getQuality().getId();
            if (tacticsQuality <= qualityId && tacticsQuality >= qualityId - 1) {
                availableTactics.add(t);
            }
        }
        
        if (availableTactics.isEmpty()) {
            return;
        }
        
        // 随机选择一个作为固有兵法
        Collections.shuffle(availableTactics);
        Tactics primaryTactics = copyTactics(availableTactics.get(0));
        
        List<Tactics> learned = generalLearnedTactics.computeIfAbsent(generalId, k -> new ArrayList<>());
        learned.add(primaryTactics);
        
        Map<String, Tactics> equipped = generalEquippedTactics.computeIfAbsent(generalId, k -> new HashMap<>());
        equipped.put("primary", primaryTactics);
        
        logger.info("武将 {} 初始化固有兵法: {}", general.getName(), primaryTactics.getName());
    }
    
    /**
     * 计算战斗中的兵法效果
     */
    public Map<String, Object> calculateTacticsBonus(String generalId) {
        Map<String, Object> result = new HashMap<>();
        Map<String, Integer> attributeBonus = new HashMap<>();
        List<Map<String, Object>> activeEffects = new ArrayList<>();
        
        Map<String, Tactics> equipped = generalEquippedTactics.get(generalId);
        if (equipped == null || equipped.isEmpty()) {
            result.put("attributeBonus", attributeBonus);
            result.put("activeEffects", activeEffects);
            return result;
        }
        
        // 计算被动兵法的永久效果
        for (Tactics tactics : equipped.values()) {
            if (tactics.getType().getId() == 2) {  // 被动兵法
                for (Tactics.TacticsEffect effect : tactics.getEffects()) {
                    if ("BUFF".equals(effect.getEffectType()) && effect.getDuration() == 0) {
                        // 永久效果
                        String attr = effect.getAttribute();
                        int value = effect.getBaseValue();
                        attributeBonus.merge(attr, value, Integer::sum);
                    }
                }
            }
            
            // 记录所有装备的兵法信息
            Map<String, Object> tacticsInfo = new HashMap<>();
            tacticsInfo.put("id", tactics.getId());
            tacticsInfo.put("name", tactics.getName());
            tacticsInfo.put("type", tactics.getType().getName());
            tacticsInfo.put("triggerRate", tactics.getTriggerRate());
            tacticsInfo.put("effects", tactics.getEffects());
            activeEffects.add(tacticsInfo);
        }
        
        result.put("attributeBonus", attributeBonus);
        result.put("activeEffects", activeEffects);
        
        return result;
    }
    
    /**
     * 复制兵法（创建副本）
     */
    private Tactics copyTactics(Tactics source) {
        List<Tactics.TacticsEffect> copiedEffects = new ArrayList<>();
        for (Tactics.TacticsEffect effect : source.getEffects()) {
            copiedEffects.add(Tactics.TacticsEffect.builder()
                .effectType(effect.getEffectType())
                .targetType(effect.getTargetType())
                .attribute(effect.getAttribute())
                .baseValue(effect.getBaseValue())
                .ratio(effect.getRatio())
                .duration(effect.getDuration())
                .description(effect.getDescription())
                .build());
        }
        
        return Tactics.builder()
            .id(source.getId())
            .name(source.getName())
            .type(source.getType())
            .quality(source.getQuality())
            .description(source.getDescription())
            .icon(source.getIcon())
            .effects(copiedEffects)
            .triggerRate(source.getTriggerRate())
            .triggerCondition(source.getTriggerCondition())
            .learnLevel(source.getLearnLevel())
            .learnCondition(source.getLearnCondition())
            .level(1)
            .maxLevel(10)
            .exp(0)
            .maxExp(100)
            .createTime(System.currentTimeMillis())
            .updateTime(System.currentTimeMillis())
            .build();
    }
}
