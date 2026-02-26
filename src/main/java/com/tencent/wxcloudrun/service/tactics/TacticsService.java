package com.tencent.wxcloudrun.service.tactics;

import com.tencent.wxcloudrun.config.TacticsConfig;
import com.tencent.wxcloudrun.dao.UserLearnedTacticsMapper;
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
 * 兵法服务
 * 兵法分为：步兵专属/骑兵专属/弓兵专属/通用
 * 每个武将只能装备一个兵法（单槽），存储在 general.tactics_id
 * 用户已学习兵法存储在 user_learned_tactics 表（逗号分隔）
 */
@Service
public class TacticsService {
    
    private static final Logger logger = LoggerFactory.getLogger(TacticsService.class);
    
    @Autowired
    private TacticsConfig tacticsConfig;
    
    @Autowired
    private GeneralRepository generalRepository;
    
    @Autowired
    private UserLearnedTacticsMapper userLearnedTacticsMapper;
    
    // ==================== 用户已学习兵法 ====================
    
    public Set<String> loadUserLearnedTactics(String userId) {
        String data = userLearnedTacticsMapper.findByUserId(userId);
        if (data == null || data.isEmpty()) {
            return new HashSet<>();
        }
        return new HashSet<>(Arrays.asList(data.split(",")));
    }
    
    public void saveUserLearnedTactics(String userId, Set<String> learned) {
        String tacticsIds = learned.isEmpty() ? "" : String.join(",", learned);
        userLearnedTacticsMapper.upsert(userId, tacticsIds);
    }
    
    // ==================== 兵法配置查询 ====================
    
    public List<Tactics> getAllTactics() {
        return new ArrayList<>(tacticsConfig.getAllTactics().values());
    }
    
    public Tactics getTacticsById(String tacticsId) {
        return tacticsConfig.getTacticsById(tacticsId);
    }
    
    /**
     * 获取适用于指定兵种的兵法列表
     * @param troopType 兵种：步/骑/弓
     */
    public List<Tactics> getTacticsByTroopType(String troopType) {
        List<Tactics> result = new ArrayList<>();
        for (Tactics t : tacticsConfig.getAllTactics().values()) {
            String type = t.getType() != null ? t.getType().getName() : "通用";
            if ("通用".equals(type) || type.equals(troopType)) {
                result.add(t);
            }
        }
        return result;
    }
    
    // ==================== 武将兵法操作 ====================
    
    /**
     * 获取武将当前装备的兵法ID
     */
    public String getGeneralEquippedTacticsId(String generalId) {
        General general = generalRepository.findById(generalId);
        return general != null ? general.getTacticsId() : null;
    }
    
    /**
     * 装备兵法（单槽，直接替换）
     */
    public Map<String, Object> equipTactics(String userId, String generalId, String tacticsId) {
        General general = generalRepository.findById(generalId);
        if (general == null) { throw new BusinessException(400, "武将不存在"); }
        if (!userId.equals(general.getUserId())) { throw new BusinessException(403, "无权操作此武将"); }
        
        // 检查用户是否已学习
        Set<String> learned = loadUserLearnedTactics(userId);
        if (!learned.contains(tacticsId)) { throw new BusinessException(400, "未学习该兵法"); }
        
        // 检查兵法是否适用于该武将兵种
        Tactics tactics = tacticsConfig.getTacticsById(tacticsId);
        if (tactics != null && tactics.getType() != null) {
            String tacticsType = tactics.getType().getName();
            String troopType = general.getTroopType();
            if (!"通用".equals(tacticsType) && troopType != null && !tacticsType.equals(troopType)) {
                throw new BusinessException(400, "该兵法不适用于" + troopType + "兵种");
            }
        }
        
        general.setTacticsId(tacticsId);
        general.setUpdateTime(System.currentTimeMillis());
        generalRepository.update(general);
        
        logger.info("武将 {} 装备兵法: {}", general.getName(), tacticsId);
        
        Map<String, Object> result = new HashMap<>();
        result.put("generalId", generalId);
        result.put("generalName", general.getName());
        result.put("tacticsId", tacticsId);
        return result;
    }
    
    /**
     * 卸下兵法
     */
    public Map<String, Object> unequipTactics(String userId, String generalId) {
        General general = generalRepository.findById(generalId);
        if (general == null) { throw new BusinessException(400, "武将不存在"); }
        if (!userId.equals(general.getUserId())) { throw new BusinessException(403, "无权操作此武将"); }
        
        String oldTacticsId = general.getTacticsId();
        general.setTacticsId(null);
        general.setUpdateTime(System.currentTimeMillis());
        generalRepository.update(general);
        
        logger.info("武将 {} 卸下兵法: {}", general.getName(), oldTacticsId);
        
        Map<String, Object> result = new HashMap<>();
        result.put("generalId", generalId);
        result.put("generalName", general.getName());
        result.put("removedTacticsId", oldTacticsId);
        return result;
    }
    
    /**
     * 初始化武将兵法（招募时随机分配一个适用兵法）
     */
    public void initGeneralTactics(General general) {
        String troopType = general.getTroopType();
        int qualityId = general.getQualityId() != null ? general.getQualityId() : 1;
        
        List<Tactics> available = new ArrayList<>();
        for (Tactics t : tacticsConfig.getAllTactics().values()) {
            String type = t.getType() != null ? t.getType().getName() : "通用";
            int tQuality = t.getQuality() != null ? t.getQuality().getId() : 1;
            if (("通用".equals(type) || type.equals(troopType)) && tQuality <= qualityId && tQuality >= qualityId - 1) {
                available.add(t);
            }
        }
        
        if (available.isEmpty()) return;
        
        Collections.shuffle(available);
        Tactics chosen = available.get(0);
        general.setTacticsId(chosen.getId());
        
        // 同时加入用户已学习列表
        Set<String> learned = loadUserLearnedTactics(general.getUserId());
        learned.add(chosen.getId());
        saveUserLearnedTactics(general.getUserId(), learned);
        
        logger.info("武将 {} 初始化兵法: {}", general.getName(), chosen.getName());
    }
    
    /**
     * 计算武将兵法加成
     */
    public Map<String, Object> calculateTacticsBonus(String generalId) {
        Map<String, Object> result = new HashMap<>();
        Map<String, Integer> attributeBonus = new HashMap<>();
        
        General general = generalRepository.findById(generalId);
        if (general == null || general.getTacticsId() == null) {
            result.put("attributeBonus", attributeBonus);
            result.put("tacticsId", null);
            return result;
        }
        
        Tactics tactics = tacticsConfig.getTacticsById(general.getTacticsId());
        if (tactics != null && tactics.getEffects() != null) {
            for (Tactics.TacticsEffect effect : tactics.getEffects()) {
                if ("BUFF".equals(effect.getEffectType()) && effect.getDuration() == 0) {
                    String attr = effect.getAttribute();
                    int value = effect.getBaseValue();
                    attributeBonus.merge(attr, value, Integer::sum);
                }
            }
        }
        
        result.put("attributeBonus", attributeBonus);
        result.put("tacticsId", general.getTacticsId());
        result.put("tactics", tactics);
        return result;
    }
}
