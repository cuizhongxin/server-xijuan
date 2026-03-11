package com.tencent.wxcloudrun.service.general;

import com.tencent.wxcloudrun.dao.GeneralSlotMapper;
import com.tencent.wxcloudrun.dao.GeneralSlotTraitMapper;
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

    @Autowired
    private GeneralSlotMapper generalSlotMapper;

    @Autowired
    private GeneralSlotTraitMapper generalSlotTraitMapper;

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
        // slotId 1=orange步统帅, 7=purple步统帅
        initials.add(buildGeneral(userId, "赵云", "蜀", 6, "橙色", "#FF8C00", 1.5, 5, "步", 50, 1));
        initials.add(buildGeneral(userId, "张飞", "蜀", 5, "紫色", "#9370DB", 1.3, 4, "步", 46, 7));
        // 骑兵将领：机动高，攻击力强
        // slotId 9=purple骑统帅, 3=orange骑猛将
        initials.add(buildGeneral(userId, "关羽", "蜀", 5, "紫色", "#9370DB", 1.3, 4, "骑", 48, 9));
        initials.add(buildGeneral(userId, "吕布", "群", 6, "橙色", "#FF8C00", 1.5, 5, "骑", 42, 3));
        // 弓兵将领：最强杀伤力，可靠的伤害输出者
        // slotId 5=orange弓智将, 17=red弓智将
        initials.add(buildGeneral(userId, "诸葛亮", "蜀", 6, "橙色", "#FF8C00", 1.5, 5, "弓", 45, 5));
        initials.add(buildGeneral(userId, "貂蝉", "群", 4, "红色", "#DC143C", 1.1, 4, "弓", 43, 17));
        
        List<General> saved = generalRepository.saveAll(initials);
        logger.info("初始化完成，创建了{}个武将", saved.size());
        return saved;
    }
    
    private General buildGeneral(String userId, String name, String faction,
                                 int qualityId, String qualityName, String qualityColor,
                                 double qualityMultiplier, int qualityStar,
                                 String troopType, int level, int slotId) {
        String id = "general_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8);
        
        // 优先用 slotId 精确计算（含特性加成）
        int[] attrs;
        if (slotId > 0) {
            attrs = calcAttributesBySlot(slotId, level);
        } else {
            attrs = calcAttributes(qualityMultiplier, troopType, level);
        }
        
        return General.builder()
            .id(id).userId(userId).name(name).avatar("").faction(faction)
            .level(level).exp(0L).maxExp(calcMaxExp(level))
            .qualityId(qualityId).qualityName(qualityName).qualityColor(qualityColor)
            .qualityBaseMultiplier(qualityMultiplier).qualityStar(qualityStar)
            .troopType(troopType).slotId(slotId > 0 ? slotId : null)
            .attrAttack(attrs[0]).attrDefense(attrs[1]).attrValor(attrs[2])
            .attrCommand(attrs[3]).attrDodge((double) attrs[4]).attrMobility(attrs[5])
            .soldierRank(1).soldierCount(1000).soldierMaxCount(1000)
            .statusLocked(false).statusInBattle(false).statusInjured(false).statusMorale(100)
            .statTotalBattles(0).statVictories(0).statDefeats(0).statKills(0).statMvpCount(0)
            .createTime(System.currentTimeMillis()).updateTime(System.currentTimeMillis())
            .build();
    }
    
    /**
     * 等级成长系数 - 每级增长 = 基础属性 × growthRate
     *
     * 不同品质成长率不同，高品质武将每级成长更多:
     *   orange=0.06, purple=0.05, red=0.045, blue=0.04, green=0.035, white=0.03
     */
    private static final Map<String, Double> GROWTH_RATES = new LinkedHashMap<>();
    static {
        GROWTH_RATES.put("orange", 0.06);
        GROWTH_RATES.put("purple", 0.05);
        GROWTH_RATES.put("red", 0.045);
        GROWTH_RATES.put("blue", 0.04);
        GROWTH_RATES.put("green", 0.035);
        GROWTH_RATES.put("white", 0.03);
    }

    /**
     * v3 基于 general_slot 表的属性计算
     *
     * 公式: 属性 = slotBase + slotBase × growthRate × (level - 1) + traitBonus
     *
     * slotBase: general_slot 表中该品质+兵种+类型的基础六维
     * growthRate: 品质成长系数 (orange 0.06 ~ white 0.03)
     * traitBonus: general_slot_trait 表中的名将特性加成（仅橙色有）
     *
     * 示例（橙色步兵统帅 Lv50）:
     *   base_attack=150, growthRate=0.06
     *   attack = 150 + 150 × 0.06 × 49 + traitAttack = 150 + 441 + 400 = 991
     *
     * 对比（紫色步兵统帅 Lv50）:
     *   base_attack=130, growthRate=0.05
     *   attack = 130 + 130 × 0.05 × 49 = 130 + 318 = 448
     *
     * 橙色比紫色同级高 ~120%，体现品质差异
     */
    /**
     * v3 基于 general_slot 表的属性计算
     *
     * 返回: [atk, def, valor, command, dodge, mobility, tacticsTriggerBonus, tacticsTriggerMultiplier]
     * 第7个元素为兵法发动概率加成(%), 第8个元素为兵法发动倍率(默认1, 翻倍为2)
     */
    public int[] calcAttributesBySlot(int slotId, int level) {
        Map<String, Object> slot = generalSlotMapper.findById(slotId);
        if (slot == null) {
            logger.warn("找不到 slotId={}, 使用默认属性", slotId);
            return new int[]{100, 100, 50, 50, 10, 50, 0, 1};
        }

        int baseAtk = toInt(slot.get("baseAttack"));
        int baseDef = toInt(slot.get("baseDefense"));
        int baseValor = toInt(slot.get("baseValor"));
        int baseCommand = toInt(slot.get("baseCommand"));
        double baseDodge = toDouble(slot.get("baseDodge"));
        int baseMobility = toInt(slot.get("baseMobility"));
        int tacticsTriggerBonus = toInt(slot.get("tacticsTriggerBonus"));
        String qualityCode = (String) slot.get("qualityCode");

        double growthRate = GROWTH_RATES.getOrDefault(qualityCode, 0.03);
        int lvGrowth = level - 1;

        int atk = (int) (baseAtk + baseAtk * growthRate * lvGrowth);
        int def = (int) (baseDef + baseDef * growthRate * lvGrowth);
        int valor = (int) (baseValor + baseValor * growthRate * lvGrowth);
        int command = (int) (baseCommand + baseCommand * growthRate * lvGrowth);
        int dodge = (int) Math.min(50, baseDodge + baseDodge * growthRate * lvGrowth);
        int mobility = (int) (baseMobility + baseMobility * growthRate * lvGrowth);
        int tacticsTriggerMultiplier = 1;

        // 加载特性加成
        List<Map<String, Object>> traits = generalSlotTraitMapper.findBySlotIds(Collections.singletonList(slotId));
        if (traits != null) {
            for (Map<String, Object> trait : traits) {
                String traitType = (String) trait.get("traitType");
                String traitValue = (String) trait.get("traitValue");
                if (traitType == null || traitValue == null) continue;
                if ("special".equals(traitType)) continue;
                try {
                    int v = Integer.parseInt(traitValue.trim());
                    switch (traitType) {
                        case "attack": atk += v; break;
                        case "defense": def += v; break;
                        case "valor": valor += v; break;
                        case "command": command += v; break;
                        case "dodge": dodge = (int) Math.min(50, dodge + v); break;
                        case "mobility": mobility += v; break;
                        case "tactics_trigger": tacticsTriggerMultiplier = v; break;
                    }
                } catch (NumberFormatException ignored) {}
            }
        }

        return new int[]{atk, def, valor, command, dodge, mobility, tacticsTriggerBonus, tacticsTriggerMultiplier};
    }

    /**
     * 根据 slotId 获取兵法发动概率加成(%)
     */
    public int getTacticsTriggerBonus(int slotId) {
        Map<String, Object> slot = generalSlotMapper.findById(slotId);
        if (slot == null) return 0;
        return toInt(slot.get("tacticsTriggerBonus"));
    }

    /**
     * 根据 slotId 获取兵法发动概率倍率（含"兵法发动概率提升"名将特性时返回2，否则1）
     */
    public int getTacticsTriggerMultiplier(int slotId) {
        List<Map<String, Object>> traits = generalSlotTraitMapper.findBySlotIds(Collections.singletonList(slotId));
        if (traits != null) {
            for (Map<String, Object> trait : traits) {
                if ("tactics_trigger".equals(trait.get("traitType"))) {
                    try { return Integer.parseInt(((String) trait.get("traitValue")).trim()); }
                    catch (Exception e) { return 2; }
                }
            }
        }
        return 1;
    }

    /**
     * 兼容旧接口 - 无 slotId 时的降级计算（用于初始化等场景）
     * 仍然保留品质倍率逻辑，但基础值对齐 general_slot 表的白色基准
     */
    public int[] calcAttributes(double qualityMultiplier, String troopType, int level) {
        // 白色基准值（对齐 general_slot 表 white 行: 90/90/45/45/9/45）
        int baseAtk = 90, baseDef = 90, baseValor = 45, baseCommand = 45;
        double baseDodge = 9.0;
        int baseMobility = 45;

        double growthRate = 0.03; // 白色成长率作为基准
        int lvGrowth = level - 1;

        int atk = (int) ((baseAtk + baseAtk * growthRate * lvGrowth) * qualityMultiplier);
        int def = (int) ((baseDef + baseDef * growthRate * lvGrowth) * qualityMultiplier);
        int valor = (int) ((baseValor + baseValor * growthRate * lvGrowth) * qualityMultiplier);
        int command = (int) ((baseCommand + baseCommand * growthRate * lvGrowth) * qualityMultiplier);
        int dodge = (int) Math.min(50, (baseDodge + baseDodge * growthRate * lvGrowth) * qualityMultiplier);
        int mobility = (int) ((baseMobility + baseMobility * growthRate * lvGrowth) * qualityMultiplier);

        return new int[]{atk, def, valor, command, dodge, mobility};
    }

    private int toInt(Object obj) {
        if (obj == null) return 0;
        if (obj instanceof Number) return ((Number) obj).intValue();
        try { return Integer.parseInt(obj.toString()); } catch (Exception e) { return 0; }
    }

    private double toDouble(Object obj) {
        if (obj == null) return 0.0;
        if (obj instanceof Number) return ((Number) obj).doubleValue();
        try { return Double.parseDouble(obj.toString()); } catch (Exception e) { return 0.0; }
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
            // 优先用 slotId 精确计算，降级用旧公式
            if (general.getSlotId() != null && general.getSlotId() > 0) {
                int[] attrs = calcAttributesBySlot(general.getSlotId(), newLevel);
                general.setAttrAttack(attrs[0]);
                general.setAttrDefense(attrs[1]);
                general.setAttrValor(attrs[2]);
                general.setAttrCommand(attrs[3]);
                general.setAttrDodge((double) attrs[4]);
                general.setAttrMobility(attrs[5]);
            } else {
                double qm = general.getQualityBaseMultiplier() != null ? general.getQualityBaseMultiplier() : 1.0;
                String tt = general.getTroopType() != null ? general.getTroopType() : "步";
                int[] attrs = calcAttributes(qm, tt, newLevel);
                general.setAttrAttack(attrs[0]);
                general.setAttrDefense(attrs[1]);
                general.setAttrValor(attrs[2]);
                general.setAttrCommand(attrs[3]);
                general.setAttrDodge((double) attrs[4]);
                general.setAttrMobility(attrs[5]);
            }
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

        // APK rates: basic=50%, medium(15044)=80%, advanced(15045)=100%
        double rate;
        if ("advanced".equals(scrollType)) {
            rate = 1.0;
            boolean consumed = warehouseService.removeItem(userId, "15045", 1);
            if (!consumed) { throw new RuntimeException("高级传承符不足"); }
        } else if ("medium".equals(scrollType)) {
            rate = 0.8;
            boolean consumed = warehouseService.removeItem(userId, "15044", 1);
            if (!consumed) { throw new RuntimeException("初级传承符不足"); }
        } else {
            rate = 0.5;
        }

        long sourceExp = source.getExp() != null ? source.getExp() : 0;
        for (int i = 1; i < source.getLevel(); i++) { sourceExp += calcMaxExp(i); }
        long expGained = (long)(sourceExp * rate);
        
        Map<String, Object> expResult = addGeneralExp(targetId, expGained);
        generalRepository.delete(sourceId);
        
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("expGained", expGained);
        result.put("rate", rate);
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
     * itemId: 15031=初级(5000), 15032=中级(15000), 15033=高级(35000), 15034=特级(60000)
     */
    public Map<String, Object> useExpItem(String userId, String generalId, int itemId, int count) {
        int expPer;
        switch (itemId) {
            case 28: expPer = 200; break;
            case 29: expPer = 1000; break;
            case 30: expPer = 5000; break;
            case 15031: expPer = 5000; break;
            case 15032: expPer = 15000; break;
            case 15033: expPer = 35000; break;
            case 15034: expPer = 60000; break;
            default: throw new RuntimeException("无效的经验药道具");
        }

        if (generalId == null || generalId.isEmpty()) {
            throw new RuntimeException("请选择武将");
        }

        boolean consumed = warehouseService.removeItem(userId, String.valueOf(itemId), count);
        if (!consumed) { throw new RuntimeException("道具数量不足"); }

        long totalExp = (long) expPer * count;
        General general = generalRepository.findById(generalId);
        if (general == null) { throw new RuntimeException("武将不存在"); }
        if (!general.getUserId().equals(userId)) { throw new RuntimeException("无权操作"); }
        Map<String, Object> expResult = addGeneralExp(generalId, totalExp);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("expGained", totalExp);
        result.put("itemId", itemId);
        result.put("count", count);
        result.put("type", "general");
        result.put("levelUp", expResult.get("levelUp"));
        result.put("newLevel", expResult.get("newLevel"));
        return result;
    }
}
