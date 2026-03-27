package com.tencent.wxcloudrun.service.general;

import com.tencent.wxcloudrun.dao.EquipmentMapper;
import com.tencent.wxcloudrun.dao.GeneralFamousTraitMapper;
import com.tencent.wxcloudrun.dao.GeneralSlotMapper;
import com.tencent.wxcloudrun.dao.GeneralTemplateMapper;
import com.tencent.wxcloudrun.model.General;
import com.tencent.wxcloudrun.repository.GeneralRepository;
import com.tencent.wxcloudrun.service.battle.BattleCalculator;
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
    private EquipmentMapper equipmentMapper;
    
    @Autowired
    private com.tencent.wxcloudrun.service.warehouse.WarehouseService warehouseService;

    @Autowired
    private com.tencent.wxcloudrun.service.UserResourceService userResourceService;

    @Autowired
    private GeneralSlotMapper generalSlotMapper;

    @Autowired
    private GeneralTemplateMapper generalTemplateMapper;
    @Autowired
    private GeneralFamousTraitMapper generalFamousTraitMapper;

    public List<General> getUserGenerals(String userId) {
        return generalRepository.findByUserId(userId);
    }
    
    public General getGeneralById(String generalId) {
        return generalRepository.findById(generalId);
    }
    
    /**
     * 新玩家初始赠送：一个步兵将领（梁婉），level 1
     */
    public General grantStarterGeneral(String gameUserId) {
        List<General> existing = generalRepository.findByUserId(gameUserId);
        if (!existing.isEmpty()) return existing.get(0);

        General starter = buildGeneral(gameUserId, "梁婉", "群",
                3, "蓝色", "#4169E1", 1.0, 3, "步", 1, 48);
        return generalRepository.saveAll(Collections.singletonList(starter)).get(0);
    }

    /**
     * VIP奖励发放专用 — 创建并保存一个指定武将给玩家
     * @return 创建的武将实例，若玩家已拥有同名武将则返回 null
     */
    public General grantVipGeneral(String userId, String name, String faction,
                                   int qualityId, String qualityName, String qualityColor,
                                   double qualityMultiplier, int qualityStar,
                                   String troopType, int slotId) {
        List<General> owned = generalRepository.findByUserId(userId);
        boolean alreadyOwned = owned.stream().anyMatch(g -> name.equals(g.getName()));
        if (alreadyOwned) {
            logger.info("【VIP武将发放】用户 {} 已拥有武将 {}，跳过", userId, name);
            return null;
        }
        General general = buildGeneral(userId, name, faction,
                qualityId, qualityName, qualityColor,
                qualityMultiplier, qualityStar, troopType, 1, slotId);
        generalRepository.saveAll(Collections.singletonList(general));
        logger.info("【VIP武将发放】用户 {} 获得武将 {}(slotId={})", userId, name, slotId);
        return general;
    }

    public List<General> initUserGenerals(String userId) {
        if ("1".equals(userId)) { return new ArrayList<>(); }
        List<General> existing = generalRepository.findByUserId(userId);
        if (!existing.isEmpty()) { return existing; }
        
        List<General> initials = new ArrayList<>();
        // 步兵将领 — slotId 对应实际 DB: 1=orange步统帅, 10=purple步猛将
        initials.add(buildGeneral(userId, "赵云", "蜀", 6, "橙色", "#FF8C00", 1.5, 5, "步", 50, 1));
        initials.add(buildGeneral(userId, "张飞", "蜀", 5, "紫色", "#9370DB", 1.3, 4, "步", 46, 10));
        // 骑兵将领 — slotId: 3=orange骑猛将, 13=purple骑统帅
        initials.add(buildGeneral(userId, "关羽", "蜀", 5, "紫色", "#9370DB", 1.3, 4, "骑", 48, 13));
        initials.add(buildGeneral(userId, "吕布", "群", 6, "橙色", "#FF8C00", 1.5, 5, "骑", 42, 3));
        // 弓兵将领 — slotId: 5=orange弓智将, 76=red弓智将
        initials.add(buildGeneral(userId, "诸葛亮", "蜀", 6, "橙色", "#FF8C00", 1.5, 5, "弓", 45, 5));
        initials.add(buildGeneral(userId, "貂蝉", "群", 4, "红色", "#DC143C", 1.1, 4, "弓", 43, 76));
        
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
        
        // 从 general_template 查询头像
        String avatar = "";
        try {
            Map<String, Object> tpl = generalTemplateMapper.findByName(name);
            if (tpl != null && tpl.get("avatar") != null) {
                avatar = tpl.get("avatar").toString().trim();
            }
        } catch (Exception e) {
            logger.warn("查询武将模板头像失败: name={}", name, e);
        }
        
        List<String> traitDescs = loadFamousTraits(name);
        
        return General.builder()
            .id(id).userId(userId).name(name).avatar(avatar).faction(faction)
            .level(level).exp(0L).maxExp(calcMaxExp(level))
            .qualityId(qualityId).qualityName(qualityName).qualityColor(qualityColor)
            .qualityBaseMultiplier(qualityMultiplier).qualityStar(qualityStar)
            .troopType(troopType).slotId(slotId > 0 ? slotId : null)
            .attrAttack(attrs[0]).attrDefense(attrs[1]).attrValor(attrs[2])
            .attrCommand(attrs[3]).attrDodge((double) attrs[4]).attrMobility(attrs[5])
            .soldierRank(1).soldierTier(1).soldierCount(100).soldierMaxCount(100)
            .traits(traitDescs.isEmpty() ? null : traitDescs)
            .statusLocked(false).statusInBattle(false).statusInjured(false).statusMorale(100)
            .statTotalBattles(0).statVictories(0).statDefeats(0).statKills(0).statMvpCount(0)
            .createTime(System.currentTimeMillis()).updateTime(System.currentTimeMillis())
            .build();
    }
    
    /**
     * 按武将名从 general_famous_trait 加载独立具名特性（APK 风格）
     * 返回格式: ["战神：属下士兵伤害＋500", "赤兔飞将：骑兵兵法发动概率增加"]
     */
    public List<String> loadFamousTraits(String generalName) {
        List<String> result = new ArrayList<>();
        if (generalName == null || generalName.isEmpty()) return result;
        try {
            List<Map<String, Object>> rows = generalFamousTraitMapper.findByGeneralName(generalName);
            if (rows != null) {
                for (Map<String, Object> row : rows) {
                    String name = (String) row.get("traitName");
                    String desc = (String) row.get("traitDesc");
                    result.add(name + "：" + desc);
                }
            }
        } catch (Exception e) {
            logger.warn("查询名将特性失败: name={}", generalName, e);
        }
        return result;
    }

    /**
     * 按武将名加载特性的结构化数据（用于战斗计算）
     */
    public List<Map<String, Object>> loadFamousTraitData(String generalName) {
        if (generalName == null || generalName.isEmpty()) return Collections.emptyList();
        try {
            List<Map<String, Object>> rows = generalFamousTraitMapper.findByGeneralName(generalName);
            return rows != null ? rows : Collections.emptyList();
        } catch (Exception e) {
            logger.warn("查询名将特性数据失败: name={}", generalName, e);
            return Collections.emptyList();
        }
    }

    /**
     * 将名将特性加成应用到已构建的 BattleUnit 上（供各战斗场景统一调用）
     */
    public void applyFamousTraitsToUnit(BattleCalculator.BattleUnit unit, String generalName, int troopType) {
        List<Map<String, Object>> traits = loadFamousTraitData(generalName);
        if (traits.isEmpty()) return;

        int traitAtkBonus = 0, traitDefBonus = 0, traitDmgBonus = 0;
        int traitDamageResist = 0;
        double traitLifePct = 0;
        boolean traitImmuneAmbush = false;

        for (Map<String, Object> t : traits) {
            String effectType = (String) t.get("effectType");
            int effectValue = t.get("effectValue") != null ? ((Number) t.get("effectValue")).intValue() : 0;
            int troopR = t.get("troopRestrict") != null ? ((Number) t.get("troopRestrict")).intValue() : 0;
            if (troopR != 0 && troopR != troopType) continue;

            switch (effectType) {
                case "soldier_damage": case "troop_damage":  traitDmgBonus += effectValue; break;
                case "army_attack":    case "troop_attack":  traitAtkBonus += effectValue; break;
                case "army_defense":   case "troop_defense": traitDefBonus += effectValue; break;
                case "damage_resist":   traitDamageResist += effectValue; break;
                case "soldier_life_pct": traitLifePct += effectValue; break;
                case "army_mobility":   unit.mobility += effectValue; break;
                case "army_dodge":      unit.dodge += effectValue; break;
                case "soldier_count":   unit.soldierCount += effectValue; unit.maxSoldierCount += effectValue; break;
                case "tactics_prob": case "troop_tactics": unit.tacticsTriggerMultiplier = 2.0; break;
                case "immune_ambush":   traitImmuneAmbush = true; break;
            }
        }
        unit.totalAttack += traitAtkBonus;
        unit.totalDefense += traitDefBonus;
        unit.traitDmgBonus += traitDmgBonus;
        unit.traitDamageResist += traitDamageResist;
        unit.traitLifePct += traitLifePct;
        unit.traitImmuneAmbush = traitImmuneAmbush;
    }

    /**
     * 品质成长系数（仅作为兼容兜底）
     *
     * APK 风格应优先读取 general_slot.growth_* 独立成长值。
     * 当历史数据中的 growth_* 为空或为0时，退化到以下按品质比例的旧规则，
     * 以避免旧库升级后出现“升级不加属性”。
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

    private static final int ADVANCED_GROWTH_ATTACK_BONUS = 1;
    private static final int ADVANCED_GROWTH_DEFENSE_BONUS = 1;

    /**
     * APK 风格属性计算（默认不附加进阶成长）
     *
     * 返回: [atk, def, valor, command, dodge, mobility, tacticsTriggerBonus, tacticsTriggerMultiplier]
     * 第7个元素为兵法发动概率加成(%), 第8个元素为兵法发动倍率(默认1, 翻倍为2)
     */
    public int[] calcAttributesBySlot(int slotId, int level) {
        return calcAttributesBySlot(slotId, level, 0, 0);
    }

    /**
     * APK 风格属性计算（支持附加成长）
     *
     * 公式:
     *   攻击 = base_attack + (growth_attack + attackGrowthBonus) * (level - 1)
     *   防御 = base_defense + (growth_defense + defenseGrowthBonus) * (level - 1)
     *   武勇 = base_valor + growth_valor * (level - 1)
     *   统御 = base_command + growth_command * (level - 1)
     *
     * 注:
     * - 狂化武将沿用 APK 文案“进阶后攻击成长+1、防御成长+1”
     * - 当 growth_* 为空或为0时，退化到旧规则（base * 品质成长率）
     */
    public int[] calcAttributesBySlot(int slotId, int level, int attackGrowthBonus, int defenseGrowthBonus) {
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
        double growthAttack = toDouble(slot.get("growthAttack"));
        double growthDefense = toDouble(slot.get("growthDefense"));
        double growthValor = toDouble(slot.get("growthValor"));
        double growthCommand = toDouble(slot.get("growthCommand"));

        double growthRate = GROWTH_RATES.getOrDefault(qualityCode, 0.03);
        int lvGrowth = level - 1;

        // 历史兼容: 旧库 growth_* 可能是0，回退到 base * growthRate
        if (growthAttack <= 0) growthAttack = baseAtk * growthRate;
        if (growthDefense <= 0) growthDefense = baseDef * growthRate;
        if (growthValor <= 0) growthValor = baseValor * growthRate;
        if (growthCommand <= 0) growthCommand = baseCommand * growthRate;

        int atk = (int) (baseAtk + (growthAttack + attackGrowthBonus) * lvGrowth);
        int def = (int) (baseDef + (growthDefense + defenseGrowthBonus) * lvGrowth);
        int valor = (int) (baseValor + growthValor * lvGrowth);
        int command = (int) (baseCommand + growthCommand * lvGrowth);
        int dodge = (int) Math.min(50, baseDodge + baseDodge * growthRate * lvGrowth);
        int mobility = (int) (baseMobility + baseMobility * growthRate * lvGrowth);
        return new int[]{atk, def, valor, command, dodge, mobility, tacticsTriggerBonus, 1};
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
     * 兼容旧接口 - 无 slotId 时的降级计算（用于初始化等场景）
     * 仍然保留品质倍率逻辑，但基础值对齐 general_slot 表的白色基准
     */
    public int[] calcAttributes(double qualityMultiplier, String troopType, int level) {
        // 白色基准值（对齐 general_slot 表 white 行: 90/90/45/45/9/45）
        int baseAtk = 90, baseDef = 90, baseValor = 0, baseCommand = 0;
        double baseDodge = 2.0;
        int baseMobility = 10;

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
                int growthAtkBonus = getAdvanceGrowthAttackBonus(general);
                int growthDefBonus = getAdvanceGrowthDefenseBonus(general);
                int[] attrs = calcAttributesBySlot(general.getSlotId(), newLevel, growthAtkBonus, growthDefBonus);
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
     * 将领升级经验曲线
     * 公式: 250 + 750 × level²
     *
     * 设计依据: 通关第一战役(20关, 总经验37500) + 5000额外经验 = 42500, 刚好升到5级
     *
     * 关键节点:
     * Lv1: 1,000    Lv5: 19,000    Lv10: 75,250    Lv20: 300,250
     * Lv50: 1,875,250  Lv100: 7,500,250
     *
     * 累计: Lv5: 42,500  Lv10: 297,750  Lv30: 6,965,750  Lv100: 253,787,500
     */
    private long calcMaxExp(int level) { return 250 + 750L * level * level; }
    
    public boolean dismissGeneral(String userId, String generalId) {
        General general = generalRepository.findById(generalId);
        if (general == null) { throw new RuntimeException("武将不存在"); }
        if (!general.getUserId().equals(userId)) { throw new RuntimeException("无权操作该武将"); }
        if (general.getStatusLocked() != null && general.getStatusLocked()) {
            throw new RuntimeException("武将已锁定，无法解雇");
        }
        equipmentMapper.unequipByGeneralId(generalId);
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

    // ============ APK 1:1 将领进阶系统 ============
    // 紫色名将: 仅需进阶之书 x1
    // 橙色名将: 进阶之书 x1 + 献祭指定武将(被消耗)
    // 进阶后: templateId 切换到狂化模板, 攻防成长+1, 获得进阶特性(从DB加载)

    /** 原武将名 → 狂化模板ID */
    private static final Map<String, Integer> ADVANCE_TEMPLATE_MAP = new LinkedHashMap<>();
    static {
        ADVANCE_TEMPLATE_MAP.put("华雄",   900);
        ADVANCE_TEMPLATE_MAP.put("高顺",   901);
        ADVANCE_TEMPLATE_MAP.put("公孙瓒", 902);
        ADVANCE_TEMPLATE_MAP.put("吕布",   903);
        ADVANCE_TEMPLATE_MAP.put("曹仁",   904);
        ADVANCE_TEMPLATE_MAP.put("夏侯渊", 905);
        ADVANCE_TEMPLATE_MAP.put("张辽",   906);
        ADVANCE_TEMPLATE_MAP.put("关平",   907);
        ADVANCE_TEMPLATE_MAP.put("关兴",   908);
        ADVANCE_TEMPLATE_MAP.put("关羽",   909);
        ADVANCE_TEMPLATE_MAP.put("丁奉",   910);
        ADVANCE_TEMPLATE_MAP.put("程普",   911);
        ADVANCE_TEMPLATE_MAP.put("凌统",   912);
    }

    /** 橙色名将进阶需要献祭的武将(被消耗) */
    private static final Map<String, String[]> ADVANCE_SACRIFICE = new LinkedHashMap<>();
    static {
        ADVANCE_SACRIFICE.put("吕布", new String[]{"华雄(狂)", "马腾", "公孙瓒(狂)"});
        ADVANCE_SACRIFICE.put("张辽", new String[]{"曹仁(狂)", "夏侯渊(狂)"});
        ADVANCE_SACRIFICE.put("关羽", new String[]{"关平(狂)", "关兴(狂)"});
        ADVANCE_SACRIFICE.put("凌统", new String[]{"丁奉(狂)", "程普(狂)"});
    }

    private static final String ADVANCE_BOOK_ITEM_ID = "15016";


    /**
     * 武将进阶 — 切换到狂化模板，成长+1，特性从DB加载
     * 紫色: 消耗进阶之书 x1
     * 橙色: 消耗进阶之书 x1 + 献祭指定武将
     */
    public Map<String, Object> advanceGeneral(String userId, String generalId) {
        General general = generalRepository.findById(generalId);
        if (general == null) throw new RuntimeException("武将不存在");
        if (!general.getUserId().equals(userId)) throw new RuntimeException("无权操作");

        String name = general.getName();
        if (name != null && name.endsWith("(狂)")) {
            throw new RuntimeException("该将领已经进阶过了");
        }

        Integer advTemplateId = name != null ? ADVANCE_TEMPLATE_MAP.get(name) : null;
        if (advTemplateId == null) {
            throw new RuntimeException("该将领不可进阶，仅限特定名将");
        }

        Map<String, Object> advTemplate = generalTemplateMapper.findById(advTemplateId);
        if (advTemplate == null) {
            throw new RuntimeException("狂化模板数据异常: templateId=" + advTemplateId);
        }

        // 检查并消耗献祭武将(橙色名将需要)
        String[] sacrificeNames = ADVANCE_SACRIFICE.get(name);
        List<General> sacrificeGenerals = new ArrayList<>();
        if (sacrificeNames != null) {
            List<General> allGenerals = generalRepository.findByUserId(userId);
            for (String sacName : sacrificeNames) {
                General found = null;
                for (General g : allGenerals) {
                    if (sacName.equals(g.getName()) && !g.getId().equals(generalId)) {
                        found = g;
                        break;
                    }
                }
                if (found == null) {
                    throw new RuntimeException("缺少献祭武将: " + sacName);
                }
                sacrificeGenerals.add(found);
            }
        }

        boolean consumed = warehouseService.removeItem(userId, ADVANCE_BOOK_ITEM_ID, 1);
        if (!consumed) {
            throw new RuntimeException("进阶之书不足，可在生产中制造(需50级，5000银+20000纸)");
        }

        // 消耗献祭武将
        List<String> sacrificedNames = new ArrayList<>();
        for (General sac : sacrificeGenerals) {
            sacrificedNames.add(sac.getName());
            generalRepository.delete(sac.getId());
            logger.info("进阶献祭武将: {} (id={})", sac.getName(), sac.getId());
        }

        String newName = (String) advTemplate.get("name");
        general.setName(newName);
        general.setTemplateId(String.valueOf(advTemplateId));

        // 从狂化模板读取成长加成，重算当前等级属性
        int growthAtkBonus = advTemplate.get("growthAttackBonus") != null
                ? ((Number) advTemplate.get("growthAttackBonus")).intValue() : 0;
        int growthDefBonus = advTemplate.get("growthDefenseBonus") != null
                ? ((Number) advTemplate.get("growthDefenseBonus")).intValue() : 0;

        if (general.getSlotId() != null && general.getSlotId() > 0 && general.getLevel() != null) {
            int[] attrs = calcAttributesBySlot(general.getSlotId(), general.getLevel(), growthAtkBonus, growthDefBonus);
            general.setAttrAttack(attrs[0]);
            general.setAttrDefense(attrs[1]);
            general.setAttrValor(attrs[2]);
            general.setAttrCommand(attrs[3]);
            general.setAttrDodge((double) attrs[4]);
            general.setAttrMobility(attrs[5]);
        }

        // 从DB加载狂化模板的全部特性(继承原特性 + 进阶特性)
        List<String> traits = loadFamousTraits(newName);
        general.setTraits(traits);

        // soldier_count 类型的进阶特性永久提升满编人数
        List<Map<String, Object>> traitData = loadFamousTraitData(newName);
        for (Map<String, Object> t : traitData) {
            String effectType = (String) t.get("effectType");
            int effectValue = t.get("effectValue") != null ? ((Number) t.get("effectValue")).intValue() : 0;
            if ("soldier_count".equals(effectType) && effectValue > 0) {
                int maxCount = general.getSoldierMaxCount() != null ? general.getSoldierMaxCount() : 100;
                general.setSoldierMaxCount(maxCount + effectValue);
            }
        }

        general.setUpdateTime(System.currentTimeMillis());
        generalRepository.save(general);

        logger.info("将领进阶成功: userId={}, {} → {}, templateId={}, 献祭={}",
                userId, name, newName, advTemplateId, sacrificedNames);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("generalId", generalId);
        result.put("oldName", name);
        result.put("newName", newName);
        result.put("advancedTemplateId", advTemplateId);
        if (!sacrificedNames.isEmpty()) {
            result.put("sacrificedGenerals", sacrificedNames);
        }
        return result;
    }

    /**
     * 获取武将进阶信息 — 从DB读取狂化模板数据
     */
    public Map<String, Object> getAdvanceInfo(String userId, String generalId) {
        General general = (generalId != null && !generalId.isEmpty() && !"__none__".equals(generalId))
                ? generalRepository.findById(generalId) : null;
        if (general != null && !general.getUserId().equals(userId)) general = null;

        String name = general != null ? general.getName() : null;
        boolean alreadyAdvanced = name != null && name.endsWith("(狂)");
        Integer advTemplateId = (name != null && !alreadyAdvanced) ? ADVANCE_TEMPLATE_MAP.get(name) : null;
        boolean canAdvance = advTemplateId != null;

        Map<String, Object> result = new HashMap<>();
        result.put("generalId", generalId);
        result.put("generalName", name);
        result.put("alreadyAdvanced", alreadyAdvanced);
        result.put("canAdvance", canAdvance);

        List<General> allGenerals = generalRepository.findByUserId(userId);

        if (canAdvance && advTemplateId != null) {
            Map<String, Object> advTpl = generalTemplateMapper.findById(advTemplateId);
            String advName = advTpl != null ? (String) advTpl.get("name") : name + "(狂)";
            result.put("advancedName", advName);

            List<String> advTraits = loadFamousTraits(advName);
            result.put("advancedTraits", advTraits);

            result.put("materialItemId", ADVANCE_BOOK_ITEM_ID);
            result.put("materialName", "进阶之书");
            result.put("materialNeed", 1);

            int bookCount = 0;
            try {
                bookCount = warehouseService.getItemCount(userId, ADVANCE_BOOK_ITEM_ID);
            } catch (Exception e) { /* */ }
            result.put("materialHave", bookCount);

            String[] sacNames = ADVANCE_SACRIFICE.get(name);
            List<Map<String, Object>> sacrificeReqs = new ArrayList<>();
            boolean allSacrificeReady = true;
            if (sacNames != null) {
                for (String sacName : sacNames) {
                    boolean owned = allGenerals.stream().anyMatch(g ->
                            sacName.equals(g.getName()) && !g.getId().equals(generalId));
                    Map<String, Object> sacInfo = new HashMap<>();
                    sacInfo.put("name", sacName);
                    sacInfo.put("owned", owned);
                    sacrificeReqs.add(sacInfo);
                    if (!owned) allSacrificeReady = false;
                }
            }
            result.put("sacrificeGenerals", sacrificeReqs);
            result.put("hasSacrificeReq", !sacrificeReqs.isEmpty());
            result.put("materialEnough", bookCount >= 1 && allSacrificeReady);

            List<String> bonusDesc = new ArrayList<>();
            bonusDesc.add("攻击成长+1");
            bonusDesc.add("防御成长+1");
            for (String trait : advTraits) {
                bonusDesc.add("获得特性: " + trait);
            }
            result.put("bonusDesc", bonusDesc);
        }

        List<Map<String, Object>> eligibleList = new ArrayList<>();
        for (General g : allGenerals) {
            String gName = g.getName();
            if (gName == null || gName.endsWith("(狂)")) continue;
            Integer gAdvId = ADVANCE_TEMPLATE_MAP.get(gName);
            if (gAdvId == null) continue;

            Map<String, Object> gAdvTpl = generalTemplateMapper.findById(gAdvId);
            String gAdvName = gAdvTpl != null ? (String) gAdvTpl.get("name") : gName + "(狂)";

            Map<String, Object> item = new HashMap<>();
            item.put("id", g.getId());
            item.put("name", gName);
            item.put("level", g.getLevel());
            item.put("qualityId", g.getQualityId());
            item.put("qualityColor", g.getQualityColor());
            item.put("advancedName", gAdvName);
            item.put("avatar", g.getAvatar());
            item.put("hasSacrificeReq", ADVANCE_SACRIFICE.containsKey(gName));
            eligibleList.add(item);
        }
        result.put("eligibleGenerals", eligibleList);

        return result;
    }

    /**
     * 获取武将成长加成（优先从模板DB读取，兼容旧数据用名字判断）
     */
    private int getAdvanceGrowthAttackBonus(General general) {
        if (general == null) return 0;
        if (general.getTemplateId() != null) {
            try {
                int templateId = Integer.parseInt(general.getTemplateId());
                Map<String, Object> tpl = generalTemplateMapper.findById(templateId);
                if (tpl != null && tpl.get("growthAttackBonus") != null) {
                    return ((Number) tpl.get("growthAttackBonus")).intValue();
                }
            } catch (Exception ignored) { }
        }
        return isAdvancedGeneral(general) ? ADVANCED_GROWTH_ATTACK_BONUS : 0;
    }

    private int getAdvanceGrowthDefenseBonus(General general) {
        if (general == null) return 0;
        if (general.getTemplateId() != null) {
            try {
                int templateId = Integer.parseInt(general.getTemplateId());
                Map<String, Object> tpl = generalTemplateMapper.findById(templateId);
                if (tpl != null && tpl.get("growthDefenseBonus") != null) {
                    return ((Number) tpl.get("growthDefenseBonus")).intValue();
                }
            } catch (Exception ignored) { }
        }
        return isAdvancedGeneral(general) ? ADVANCED_GROWTH_DEFENSE_BONUS : 0;
    }

    private boolean isAdvancedGeneral(General general) {
        String name = general != null ? general.getName() : null;
        return name != null && name.endsWith("(狂)");
    }
}
