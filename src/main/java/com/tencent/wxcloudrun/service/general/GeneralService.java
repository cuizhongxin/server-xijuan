package com.tencent.wxcloudrun.service.general;

import com.tencent.wxcloudrun.model.General;
import com.tencent.wxcloudrun.repository.GeneralRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 武将服务（打平模型）
 * 六维属性：攻击/防御/武勇/统御/闪避/机动
 * 装备槽：武器/铠甲/项链/戒指/鞋子/头盔
 * 兵种：步/骑/弓
 * 兵法：单槽 tacticsId
 */
@Service
public class GeneralService {
    
    private static final Logger logger = LoggerFactory.getLogger(GeneralService.class);
    
    @Autowired
    private GeneralRepository generalRepository;
    
    public List<General> getUserGenerals(String userId) {
        return generalRepository.findByUserId(userId);
    }
    
    public General getGeneralById(String generalId) {
        return generalRepository.findById(generalId);
    }
    
    public List<General> initUserGenerals(String userId) {
        if ("1".equals(userId)) { return new ArrayList<>(); }
        List<General> existing = generalRepository.findByUserId(userId);
        if (!existing.isEmpty()) { return existing; }
        
        List<General> initials = new ArrayList<>();
        initials.add(buildGeneral(userId, "赵云", "蜀", 6, "橙色", "#FF8C00", 1.5, 5, "步", 50));
        initials.add(buildGeneral(userId, "关羽", "蜀", 5, "紫色", "#9370DB", 1.3, 4, "骑", 48));
        initials.add(buildGeneral(userId, "张飞", "蜀", 5, "紫色", "#9370DB", 1.3, 4, "步", 46));
        initials.add(buildGeneral(userId, "诸葛亮", "蜀", 6, "橙色", "#FF8C00", 1.5, 5, "弓", 45));
        initials.add(buildGeneral(userId, "貂蝉", "群", 4, "红色", "#DC143C", 1.1, 4, "弓", 43));
        initials.add(buildGeneral(userId, "吕布", "群", 6, "橙色", "#FF8C00", 1.5, 5, "骑", 42));
        
        List<General> saved = generalRepository.saveAll(initials);
        logger.info("初始化完成，创建了{}个武将", saved.size());
        return saved;
    }
    
    private General buildGeneral(String userId, String name, String faction,
                                 int qualityId, String qualityName, String qualityColor,
                                 double qualityMultiplier, int qualityStar,
                                 String troopType, int level) {
        String id = "general_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8);
        int[] attrs = calcAttributes(qualityMultiplier, troopType, level);
        
        return General.builder()
            .id(id).userId(userId).name(name).avatar("").faction(faction)
            .level(level).exp(0L).maxExp(calcMaxExp(level))
            .qualityId(qualityId).qualityName(qualityName).qualityColor(qualityColor)
            .qualityBaseMultiplier(qualityMultiplier).qualityStar(qualityStar)
            .troopType(troopType)
            .attrAttack(attrs[0]).attrDefense(attrs[1]).attrValor(attrs[2])
            .attrCommand(attrs[3]).attrDodge((double) attrs[4]).attrMobility(attrs[5])
            .soldierRank(1).soldierCount(1000).soldierMaxCount(1000)
            .statusLocked(false).statusInBattle(false).statusInjured(false).statusMorale(100)
            .statTotalBattles(0).statVictories(0).statDefeats(0).statKills(0).statMvpCount(0)
            .createTime(System.currentTimeMillis()).updateTime(System.currentTimeMillis())
            .build();
    }
    
    /**
     * 计算六维属性 [attack, defense, valor, command, dodge, mobility]
     */
    public int[] calcAttributes(double qualityMultiplier, String troopType, int level) {
        int baseAtk = 100, baseDef = 100, baseVal = 50, baseCmd = 50, baseMob = 50;
        double baseDodge = 10.0;
        
        double troopAtk = 1.0, troopDef = 1.0, troopDodge = 1.0;
        if ("步".equals(troopType)) { troopAtk = 0.8; troopDef = 1.3; troopDodge = 1.5; }
        else if ("弓".equals(troopType)) { troopAtk = 1.3; troopDef = 0.7; }
        
        int attack = (int)(baseAtk * qualityMultiplier * troopAtk + 5 * (level - 1));
        int defense = (int)(baseDef * qualityMultiplier * troopDef + 5 * (level - 1));
        int valor = (int)(baseVal * qualityMultiplier + 2 * (level - 1));
        int command = (int)(baseCmd * qualityMultiplier + 2 * (level - 1));
        int dodge = (int) Math.min(baseDodge * qualityMultiplier * troopDodge + 0.5 * (level - 1), 100);
        int mobility = (int)(baseMob * qualityMultiplier + 2 * (level - 1));
        
        return new int[]{attack, defense, valor, command, dodge, mobility};
    }
    
    public Map<String, Object> addGeneralExp(String generalId, long expGain) {
        General general = generalRepository.findById(generalId);
        if (general == null) { throw new RuntimeException("武将不存在"); }
        
        long currentExp = general.getExp() != null ? general.getExp() : 0;
        long newExp = currentExp + expGain;
        int currentLevel = general.getLevel() != null ? general.getLevel() : 1;
        int newLevel = currentLevel;
        int levelsGained = 0;
        
        while (newLevel < 100 && newExp >= calcMaxExp(newLevel)) {
            newExp -= calcMaxExp(newLevel);
            newLevel++;
            levelsGained++;
        }
        
        general.setExp(newExp);
        general.setMaxExp(calcMaxExp(newLevel));
        
        if (levelsGained > 0) {
            general.setLevel(newLevel);
            double qm = general.getQualityBaseMultiplier() != null ? general.getQualityBaseMultiplier() : 1.0;
            String tt = general.getTroopType() != null ? general.getTroopType() : "步";
            int[] attrs = calcAttributes(qm, tt, newLevel);
            general.setAttrAttack(attrs[0]);
            general.setAttrDefense(attrs[1]);
            general.setAttrValor(attrs[2]);
            general.setAttrCommand(attrs[3]);
            general.setAttrDodge((double) attrs[4]);
            general.setAttrMobility(attrs[5]);
            logger.info("武将 {} 升级 {} -> {}", general.getName(), currentLevel, newLevel);
        }
        
        general.setUpdateTime(System.currentTimeMillis());
        generalRepository.save(general);
        
        Map<String, Object> result = new HashMap<>();
        result.put("generalId", generalId);
        result.put("generalName", general.getName());
        result.put("expGained", expGain);
        result.put("levelUp", levelsGained > 0);
        result.put("levelsGained", levelsGained);
        result.put("oldLevel", currentLevel);
        result.put("newLevel", newLevel);
        result.put("currentExp", newExp);
        result.put("maxExp", calcMaxExp(newLevel));
        return result;
    }
    
    public List<Map<String, Object>> addBattleExpToGenerals(List<String> generalIds, int baseExp) {
        List<Map<String, Object>> results = new ArrayList<>();
        for (String gid : generalIds) {
            try { results.add(addGeneralExp(gid, baseExp)); }
            catch (Exception e) { logger.error("给武将{}加经验失败: {}", gid, e.getMessage()); }
        }
        return results;
    }
    
    public int getUserGeneralCount(String userId) { return generalRepository.countByUserId(userId); }
    
    public boolean canRecruitGeneral(String userId, int maxSlots) {
        return getUserGeneralCount(userId) < maxSlots;
    }
    
    public General saveGeneral(General general) {
        general.setUpdateTime(System.currentTimeMillis());
        return generalRepository.save(general);
    }
    
    public Long getMaxExpForLevel(int level) { return calcMaxExp(level); }
    
    private long calcMaxExp(int level) { return (long)(100 * Math.pow(1.2, level - 1)); }
    
    public boolean dismissGeneral(String userId, String generalId) {
        General general = generalRepository.findById(generalId);
        if (general == null) { throw new RuntimeException("武将不存在"); }
        if (!general.getUserId().equals(userId)) { throw new RuntimeException("无权操作该武将"); }
        if (general.getStatusLocked() != null && general.getStatusLocked()) {
            throw new RuntimeException("武将已锁定，无法解雇");
        }
        generalRepository.delete(generalId);
        logger.info("解雇武将: userId={}, generalId={}, name={}", userId, generalId, general.getName());
        return true;
    }
    
    public Map<String, Object> inheritGeneral(String userId, String sourceId, String targetId, String scrollType) {
        General source = generalRepository.findById(sourceId);
        General target = generalRepository.findById(targetId);
        if (source == null || target == null) { throw new RuntimeException("武将不存在"); }
        if (!source.getUserId().equals(userId) || !target.getUserId().equals(userId)) { throw new RuntimeException("无权操作"); }
        if (sourceId.equals(targetId)) { throw new RuntimeException("不能传承给自己"); }
        
        double rate = "advanced".equals(scrollType) ? 1.0 : "medium".equals(scrollType) ? 0.75 : 0.5;
        long sourceExp = source.getExp() != null ? source.getExp() : 0;
        for (int i = 1; i < source.getLevel(); i++) { sourceExp += calcMaxExp(i); }
        long expGained = (long)(sourceExp * rate);
        
        Map<String, Object> expResult = addGeneralExp(targetId, expGained);
        generalRepository.delete(sourceId);
        
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("expGained", expGained);
        result.put("sourceGeneral", source.getName());
        result.put("targetGeneral", target.getName());
        result.put("levelUp", expResult.get("levelUp"));
        result.put("newLevel", expResult.get("newLevel"));
        return result;
    }
    
    public Map<String, Object> drill(String userId, String generalId, String drillType, int count) {
        General general = generalRepository.findById(generalId);
        if (general == null) { throw new RuntimeException("武将不存在"); }
        if (!general.getUserId().equals(userId)) { throw new RuntimeException("无权操作"); }
        
        int expPer = "large".equals(drillType) ? 2000 : "medium".equals(drillType) ? 500 : 100;
        long totalExp = (long) expPer * count;
        Map<String, Object> expResult = addGeneralExp(generalId, totalExp);
        
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("expGained", totalExp);
        result.put("drillType", drillType);
        result.put("count", count);
        result.put("levelUp", expResult.get("levelUp"));
        result.put("newLevel", expResult.get("newLevel"));
        return result;
    }
}
