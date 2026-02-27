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
    
    @Autowired
    private com.tencent.wxcloudrun.service.warehouse.WarehouseService warehouseService;

    @Autowired
    private com.tencent.wxcloudrun.service.UserResourceService userResourceService;

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
        // 步兵将领：擅长防御，皮糙血厚
        initials.add(buildGeneral(userId, "赵云", "蜀", 6, "橙色", "#FF8C00", 1.5, 5, "步", 50));
        initials.add(buildGeneral(userId, "张飞", "蜀", 5, "紫色", "#9370DB", 1.3, 4, "步", 46));
        // 骑兵将领：机动高，攻击力强
        initials.add(buildGeneral(userId, "关羽", "蜀", 5, "紫色", "#9370DB", 1.3, 4, "骑", 48));
        initials.add(buildGeneral(userId, "吕布", "群", 6, "橙色", "#FF8C00", 1.5, 5, "骑", 42));
        // 弓兵将领：最强杀伤力，可靠的伤害输出者
        initials.add(buildGeneral(userId, "诸葛亮", "蜀", 6, "橙色", "#FF8C00", 1.5, 5, "弓", 45));
        initials.add(buildGeneral(userId, "貂蝉", "群", 4, "红色", "#DC143C", 1.1, 4, "弓", 43));
        
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
     *
     * 核心四维（设计文档）：
     *   武勇：增加军队攻击伤害输出，提升升级时攻击成长
     *   统御：提升军队防守伤害减免，提升升级时防御成长
     *   攻击：直接提升军队攻击力
     *   防御：直接提升军队防御力
     *
     * 兵种特性：
     *   步兵：擅长防御，皮糙血厚 → 防御/统御高，攻击略低
     *   骑兵：机动高，攻击力强 → 攻击/武勇高，机动高
     *   弓兵：最强杀伤力 → 攻击/武勇最高，防御最低
     */
    public int[] calcAttributes(double qualityMultiplier, String troopType, int level) {
        // 基础属性
        int baseVal = 60, baseCmd = 60, baseAtk = 80, baseDef = 80;
        int baseMob = 50;
        double baseDodge = 8.0;

        // 武勇/统御影响攻防成长
        double valorGrowth = 3.0, commandGrowth = 3.0;
        double atkGrowth = 5.0, defGrowth = 5.0;

        // 兵种修正
        double troopValor = 1.0, troopCmd = 1.0, troopAtk = 1.0, troopDef = 1.0;
        double troopMob = 1.0, troopDodge = 1.0;

        if ("步".equals(troopType)) {
            // 步兵：防御型，统御高，防御高，攻击略低
            troopCmd = 1.3; troopDef = 1.4; troopValor = 0.9; troopAtk = 0.85;
            troopMob = 0.8; troopDodge = 0.9;
        } else if ("骑".equals(troopType)) {
            // 骑兵：突击型，武勇高，攻击强，机动高
            troopValor = 1.3; troopAtk = 1.2; troopMob = 1.5;
            troopCmd = 0.9; troopDef = 0.9; troopDodge = 1.1;
        } else if ("弓".equals(troopType)) {
            // 弓兵：输出型，武勇最高，攻击最强，防御最低
            troopValor = 1.4; troopAtk = 1.35;
            troopCmd = 0.8; troopDef = 0.7; troopMob = 0.9; troopDodge = 1.2;
        }

        int valor = (int)(baseVal * qualityMultiplier * troopValor + valorGrowth * (level - 1));
        int command = (int)(baseCmd * qualityMultiplier * troopCmd + commandGrowth * (level - 1));
        // 武勇加成攻击成长，统御加成防御成长
        int attack = (int)(baseAtk * qualityMultiplier * troopAtk + (atkGrowth + valor * 0.05) * (level - 1));
        int defense = (int)(baseDef * qualityMultiplier * troopDef + (defGrowth + command * 0.05) * (level - 1));
        int dodge = (int) Math.min(baseDodge * qualityMultiplier * troopDodge + 0.3 * (level - 1), 95);
        int mobility = (int)(baseMob * qualityMultiplier * troopMob + 1.5 * (level - 1));

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
    
    /**
     * 将领升级经验曲线：比主公(100+3*level²)慢约30%
     * 公式: 150 + 4 * level²
     *
     * 示例：
     * Lv1: 154   Lv5: 250   Lv10: 550   Lv20: 1750
     * Lv30: 3750  Lv50: 10150  Lv100: 40150
     *
     * 经验来源：副本战斗、军事演习(消耗粮草)、经验药(道具28/29/30)、将领传承
     */
    private long calcMaxExp(int level) { return 150 + 4L * level * level; }
    
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
        
        // 消耗粮食: small=500, medium=1500, large=4000
        int foodCost = "large".equals(drillType) ? 4000 : "medium".equals(drillType) ? 1500 : 500;
        long totalFoodCost = (long) foodCost * count;
        if (!userResourceService.consumeFood(userId, totalFoodCost)) {
            throw new RuntimeException("粮食不足，需要" + totalFoodCost + "粮食");
        }

        int expPer = "large".equals(drillType) ? 2000 : "medium".equals(drillType) ? 500 : 100;
        long totalExp = (long) expPer * count;
        Map<String, Object> expResult = addGeneralExp(generalId, totalExp);
        
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("expGained", totalExp);
        result.put("foodCost", totalFoodCost);
        result.put("drillType", drillType);
        result.put("count", count);
        result.put("levelUp", expResult.get("levelUp"));
        result.put("newLevel", expResult.get("newLevel"));
        return result;
    }

    /**
     * 使用经验药
     * itemId: 28=初级经验丹(200exp), 29=中级经验丹(1000exp), 30=高级经验丹(5000exp)
     */
    public Map<String, Object> useExpItem(String userId, String generalId, int itemId, int count) {
        General general = generalRepository.findById(generalId);
        if (general == null) { throw new RuntimeException("武将不存在"); }
        if (!general.getUserId().equals(userId)) { throw new RuntimeException("无权操作"); }

        int expPer;
        switch (itemId) {
            case 28: expPer = 200; break;
            case 29: expPer = 1000; break;
            case 30: expPer = 5000; break;
            default: throw new RuntimeException("无效的经验药道具");
        }

        // 扣除道具
        boolean consumed = warehouseService.removeItem(userId, String.valueOf(itemId), count);
        if (!consumed) { throw new RuntimeException("道具数量不足"); }

        long totalExp = (long) expPer * count;
        Map<String, Object> expResult = addGeneralExp(generalId, totalExp);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("expGained", totalExp);
        result.put("itemId", itemId);
        result.put("count", count);
        result.put("levelUp", expResult.get("levelUp"));
        result.put("newLevel", expResult.get("newLevel"));
        return result;
    }
}
