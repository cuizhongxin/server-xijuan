package com.tencent.wxcloudrun.service.tactics;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.tencent.wxcloudrun.config.TacticsConfig;
import com.tencent.wxcloudrun.dao.GeneralTacticsMapper;
import com.tencent.wxcloudrun.exception.BusinessException;
import com.tencent.wxcloudrun.model.General;
import com.tencent.wxcloudrun.model.Tactics;
import com.tencent.wxcloudrun.repository.GeneralRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 兵法服务（数据库存储）
 */
@Service
public class TacticsService {
    
    private static final Logger logger = LoggerFactory.getLogger(TacticsService.class);
    
    @Autowired
    private TacticsConfig tacticsConfig;
    
    @Autowired
    private GeneralRepository generalRepository;
    
    @Autowired
    private GeneralTacticsMapper generalTacticsMapper;
    
    // ==================== 数据库读写辅助 ====================
    
    private List<Tactics> loadLearnedTactics(String generalId) {
        Map<String, Object> row = generalTacticsMapper.findByGeneralId(generalId);
        if (row == null || row.get("learnedData") == null) {
            return new ArrayList<>();
        }
        return JSON.parseObject((String) row.get("learnedData"), new TypeReference<List<Tactics>>(){});
    }
    
    private Map<String, Tactics> loadEquippedTactics(String generalId) {
        Map<String, Object> row = generalTacticsMapper.findByGeneralId(generalId);
        if (row == null || row.get("equippedData") == null) {
            return new HashMap<>();
        }
        return JSON.parseObject((String) row.get("equippedData"), new TypeReference<Map<String, Tactics>>(){});
    }
    
    private void saveTactics(String generalId, List<Tactics> learned, Map<String, Tactics> equipped) {
        generalTacticsMapper.upsert(generalId,
                JSON.toJSONString(learned),
                JSON.toJSONString(equipped));
    }
    
    public List<Tactics> getAllTactics() {
        return new ArrayList<>(tacticsConfig.getAllTactics().values());
    }
    
    public Tactics getTacticsById(String tacticsId) {
        return tacticsConfig.getTacticsById(tacticsId);
    }
    
    public List<Tactics> getGeneralLearnedTactics(String generalId) {
        return loadLearnedTactics(generalId);
    }
    
    public Map<String, Tactics> getGeneralEquippedTactics(String generalId) {
        return loadEquippedTactics(generalId);
    }
    
    public Map<String, Object> learnTactics(String userId, String generalId, String tacticsId) {
        General general = generalRepository.findById(generalId);
        if (general == null) { throw new BusinessException(400, "武将不存在"); }
        if (!userId.equals(general.getUserId())) { throw new BusinessException(403, "无权操作此武将"); }
        
        Tactics tactics = tacticsConfig.getTacticsById(tacticsId);
        if (tactics == null) { throw new BusinessException(400, "兵法不存在"); }
        
        int generalLevel = general.getLevel() != null ? general.getLevel() : 1;
        if (generalLevel < tactics.getLearnLevel()) {
            throw new BusinessException(400, "武将等级不足，需要达到" + tactics.getLearnLevel() + "级");
        }
        
        List<Tactics> learned = loadLearnedTactics(generalId);
        for (Tactics t : learned) {
            if (t.getId().equals(tacticsId)) { throw new BusinessException(400, "已学习该兵法"); }
        }
        
        Tactics learnedTactics = copyTactics(tactics);
        learned.add(learnedTactics);
        
        Map<String, Tactics> equipped = loadEquippedTactics(generalId);
        saveTactics(generalId, learned, equipped);
        
        logger.info("武将 {} 学习了兵法: {}", general.getName(), tactics.getName());
        
        Map<String, Object> result = new HashMap<>();
        result.put("generalId", generalId);
        result.put("generalName", general.getName());
        result.put("tactics", learnedTactics);
        result.put("learnedCount", learned.size());
        return result;
    }
    
    public Map<String, Object> equipTactics(String userId, String generalId, String tacticsId, String slot) {
        if (!"primary".equals(slot) && !"secondary".equals(slot)) {
            throw new BusinessException(400, "无效的兵法槽位");
        }
        
        General general = generalRepository.findById(generalId);
        if (general == null) { throw new BusinessException(400, "武将不存在"); }
        if (!userId.equals(general.getUserId())) { throw new BusinessException(403, "无权操作此武将"); }
        
        List<Tactics> learned = loadLearnedTactics(generalId);
        Tactics tacticsToEquip = null;
        for (Tactics t : learned) {
            if (t.getId().equals(tacticsId)) { tacticsToEquip = t; break; }
        }
        if (tacticsToEquip == null) { throw new BusinessException(400, "未学习该兵法"); }
        
        Map<String, Tactics> equipped = loadEquippedTactics(generalId);
        String otherSlot = "primary".equals(slot) ? "secondary" : "primary";
        if (equipped.get(otherSlot) != null && equipped.get(otherSlot).getId().equals(tacticsId)) {
            equipped.remove(otherSlot);
        }
        equipped.put(slot, tacticsToEquip);
        
        saveTactics(generalId, learned, equipped);
        
        logger.info("武将 {} 装备兵法 {} 到 {} 槽位", general.getName(), tacticsToEquip.getName(), slot);
        
        Map<String, Object> result = new HashMap<>();
        result.put("generalId", generalId);
        result.put("generalName", general.getName());
        result.put("slot", slot);
        result.put("tactics", tacticsToEquip);
        result.put("equippedTactics", equipped);
        return result;
    }
    
    public Map<String, Object> unequipTactics(String userId, String generalId, String slot) {
        General general = generalRepository.findById(generalId);
        if (general == null) { throw new BusinessException(400, "武将不存在"); }
        if (!userId.equals(general.getUserId())) { throw new BusinessException(403, "无权操作此武将"); }
        
        List<Tactics> learned = loadLearnedTactics(generalId);
        Map<String, Tactics> equipped = loadEquippedTactics(generalId);
        if (!equipped.containsKey(slot)) { throw new BusinessException(400, "该槽位没有装备兵法"); }
        
        Tactics removedTactics = equipped.remove(slot);
        saveTactics(generalId, learned, equipped);
        
        logger.info("武将 {} 卸下兵法 {}", general.getName(), removedTactics.getName());
        
        Map<String, Object> result = new HashMap<>();
        result.put("generalId", generalId);
        result.put("generalName", general.getName());
        result.put("slot", slot);
        result.put("removedTactics", removedTactics);
        result.put("equippedTactics", equipped);
        return result;
    }
    
    public Map<String, Object> upgradeTactics(String userId, String generalId, String tacticsId, int expToAdd) {
        General general = generalRepository.findById(generalId);
        if (general == null) { throw new BusinessException(400, "武将不存在"); }
        if (!userId.equals(general.getUserId())) { throw new BusinessException(403, "无权操作此武将"); }
        
        List<Tactics> learned = loadLearnedTactics(generalId);
        Tactics tactics = null;
        for (Tactics t : learned) {
            if (t.getId().equals(tacticsId)) { tactics = t; break; }
        }
        if (tactics == null) { throw new BusinessException(400, "未学习该兵法"); }
        if (tactics.getLevel() >= tactics.getMaxLevel()) { throw new BusinessException(400, "兵法已满级"); }
        
        int currentExp = tactics.getExp() + expToAdd;
        int currentLevel = tactics.getLevel();
        int levelsGained = 0;
        
        while (currentLevel < tactics.getMaxLevel() && currentExp >= tactics.getMaxExp()) {
            currentExp -= tactics.getMaxExp();
            currentLevel++;
            levelsGained++;
            tactics.setMaxExp((int)(tactics.getMaxExp() * 1.5));
        }
        
        tactics.setExp(currentExp);
        tactics.setLevel(currentLevel);
        tactics.setUpdateTime(System.currentTimeMillis());
        
        if (levelsGained > 0) {
            double multiplier = 1.0 + (currentLevel - 1) * 0.1;
            for (Tactics.TacticsEffect effect : tactics.getEffects()) {
                effect.setBaseValue((int)(effect.getBaseValue() * multiplier));
            }
        }
        
        Map<String, Tactics> equipped = loadEquippedTactics(generalId);
        saveTactics(generalId, learned, equipped);
        
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
    
    public void initGeneralTactics(General general) {
        String generalId = general.getId();
        int qualityId = general.getQuality() != null ? general.getQuality().getId() : 1;
        
        List<Tactics> allTactics = new ArrayList<>(tacticsConfig.getAllTactics().values());
        List<Tactics> availableTactics = new ArrayList<>();
        
        for (Tactics t : allTactics) {
            int tacticsQuality = t.getQuality().getId();
            if (tacticsQuality <= qualityId && tacticsQuality >= qualityId - 1) { availableTactics.add(t); }
        }
        
        if (availableTactics.isEmpty()) return;
        
        Collections.shuffle(availableTactics);
        Tactics primaryTactics = copyTactics(availableTactics.get(0));
        
        List<Tactics> learned = new ArrayList<>();
        learned.add(primaryTactics);
        
        Map<String, Tactics> equipped = new HashMap<>();
        equipped.put("primary", primaryTactics);
        
        saveTactics(generalId, learned, equipped);
        
        logger.info("武将 {} 初始化固有兵法: {}", general.getName(), primaryTactics.getName());
    }
    
    public Map<String, Object> calculateTacticsBonus(String generalId) {
        Map<String, Object> result = new HashMap<>();
        Map<String, Integer> attributeBonus = new HashMap<>();
        List<Map<String, Object>> activeEffects = new ArrayList<>();
        
        Map<String, Tactics> equipped = loadEquippedTactics(generalId);
        if (equipped.isEmpty()) {
            result.put("attributeBonus", attributeBonus);
            result.put("activeEffects", activeEffects);
            return result;
        }
        
        for (Tactics tactics : equipped.values()) {
            if (tactics.getType().getId() == 2) {
                for (Tactics.TacticsEffect effect : tactics.getEffects()) {
                    if ("BUFF".equals(effect.getEffectType()) && effect.getDuration() == 0) {
                        String attr = effect.getAttribute();
                        int value = effect.getBaseValue();
                        attributeBonus.merge(attr, value, Integer::sum);
                    }
                }
            }
            
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
    
    private Tactics copyTactics(Tactics source) {
        List<Tactics.TacticsEffect> copiedEffects = new ArrayList<>();
        for (Tactics.TacticsEffect effect : source.getEffects()) {
            copiedEffects.add(Tactics.TacticsEffect.builder()
                .effectType(effect.getEffectType()).targetType(effect.getTargetType()).attribute(effect.getAttribute())
                .baseValue(effect.getBaseValue()).ratio(effect.getRatio()).duration(effect.getDuration()).description(effect.getDescription())
                .build());
        }
        
        return Tactics.builder()
            .id(source.getId()).name(source.getName()).type(source.getType()).quality(source.getQuality())
            .description(source.getDescription()).icon(source.getIcon()).effects(copiedEffects)
            .triggerRate(source.getTriggerRate()).triggerCondition(source.getTriggerCondition())
            .learnLevel(source.getLearnLevel()).learnCondition(source.getLearnCondition())
            .level(1).maxLevel(10).exp(0).maxExp(100)
            .createTime(System.currentTimeMillis()).updateTime(System.currentTimeMillis())
            .build();
    }
}
