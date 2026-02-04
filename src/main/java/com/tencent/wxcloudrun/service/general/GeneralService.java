package com.tencent.wxcloudrun.service.general;

import com.tencent.wxcloudrun.model.General;
import com.tencent.wxcloudrun.repository.GeneralRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 武将服务
 */
@Service
public class GeneralService {
    
    private static final Logger logger = LoggerFactory.getLogger(GeneralService.class);
    
    @Autowired
    private GeneralRepository generalRepository;
    
    /**
     * 获取用户的所有武将
     */
    public List<General> getUserGenerals(String userId) {
        logger.info("获取用户武将列表, userId: {}", userId);
        return generalRepository.findByUserId(userId);
    }
    
    /**
     * 获取单个武将详情
     */
    public General getGeneralById(String generalId) {
        logger.info("获取武将详情, generalId: {}", generalId);
        return generalRepository.findById(generalId);
    }
    
    /**
     * 初始化用户武将（首次登录）
     */
    public List<General> initUserGenerals(String userId) {
        logger.info("初始化用户武将, userId: {}", userId);
        if("1".equals(userId)) {
            return new ArrayList<>();
        }
        // 检查是否已经初始化过
        List<General> existingGenerals = generalRepository.findByUserId(userId);
        if (!existingGenerals.isEmpty()) {
            logger.info("用户已有武将，跳过初始化");
            return existingGenerals;
        }
        
        // 创建6个初始武将
        List<General> initialGenerals = new ArrayList<>();
        
        // 1. 赵云 - 橙色均衡型步兵
        initialGenerals.add(createInitialGeneral(userId, "赵云", 
            createQuality(6, "橙色", "#FF8C00", 1.5, 5, "🟠"),
            createGeneralType(5, "均衡型", "各项属性均衡发展", "⚖️"),
            createTroopType(1, "步兵", "🛡️", "攻击较低，防御和闪避较高", "ARCHER", "CAVALRY"),
            50, 10));
        
        // 2. 关羽 - 紫色攻击型骑兵
        initialGenerals.add(createInitialGeneral(userId, "关羽",
            createQuality(5, "紫色", "#9370DB", 1.3, 4, "🟣"),
            createGeneralType(1, "攻击型", "高攻击、高武勇", "⚔️"),
            createTroopType(2, "骑兵", "🐎", "各项属性均衡", "INFANTRY", "ARCHER"),
            48, 9));
        
        // 3. 张飞 - 紫色纯武勇型步兵
        initialGenerals.add(createInitialGeneral(userId, "张飞",
            createQuality(5, "紫色", "#9370DB", 1.3, 4, "🟣"),
            createGeneralType(4, "纯武勇型", "极高武勇", "💪"),
            createTroopType(1, "步兵", "🛡️", "攻击较低，防御和闪避较高", "ARCHER", "CAVALRY"),
            46, 8));
        
        // 4. 诸葛亮 - 橙色统帅型弓兵
        initialGenerals.add(createInitialGeneral(userId, "诸葛亮",
            createQuality(6, "橙色", "#FF8C00", 1.5, 5, "🟠"),
            createGeneralType(7, "统帅型", "高统御、高机动", "👑"),
            createTroopType(3, "弓兵", "🏹", "攻击较高，防御较低", "CAVALRY", "INFANTRY"),
            45, 9));
        
        // 5. 貂蝉 - 红色敏捷型弓兵
        initialGenerals.add(createInitialGeneral(userId, "貂蝉",
            createQuality(4, "红色", "#DC143C", 1.1, 4, "🔴"),
            createGeneralType(6, "敏捷型", "高闪避、高机动", "🏃"),
            createTroopType(3, "弓兵", "🏹", "攻击较高，防御较低", "CAVALRY", "INFANTRY"),
            43, 7));
        
        // 6. 吕布 - 橙色纯攻击型骑兵
        initialGenerals.add(createInitialGeneral(userId, "吕布",
            createQuality(6, "橙色", "#FF8C00", 1.5, 5, "🟠"),
            createGeneralType(3, "纯攻击型", "极高攻击", "🗡️"),
            createTroopType(2, "骑兵", "🐎", "各项属性均衡", "INFANTRY", "ARCHER"),
            42, 9));
        
        // 保存到数据库
        List<General> savedGenerals = generalRepository.saveAll(initialGenerals);
        logger.info("初始化完成，创建了{}个武将", savedGenerals.size());
        
        return savedGenerals;
    }
    
    /**
     * 创建初始武将
     */
    private General createInitialGeneral(String userId, String name, 
                                        General.Quality quality,
                                        General.GeneralType type,
                                        General.TroopType troopType,
                                        int level, int soldierRank) {
        
        String generalId = "general_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8);
        
        // 计算属性
        General.Attributes attributes = calculateAttributes(quality, type, troopType, level);
        
        // 士兵信息
        General.Soldiers soldiers = createSoldiers(troopType, soldierRank);
        
        // 装备（初始为空，6个槽位）
        General.Equipment equipment = General.Equipment.builder()
            .weaponId(null)
            .helmetId(null)
            .armorId(null)
            .ringId(null)
            .shoesId(null)
            .necklaceId(null)
            .build();
        
        // 兵法（初始为空）
        General.Tactics tactics = General.Tactics.builder()
            .primary(null)
            .secondary(null)
            .build();
        
        // 状态
        General.Status status = General.Status.builder()
            .locked(false)
            .inBattle(false)
            .injured(false)
            .morale(100)
            .build();
        
        // 战斗统计
        General.Stats stats = General.Stats.builder()
            .totalBattles(0)
            .victories(0)
            .defeats(0)
            .kills(0)
            .mvpCount(0)
            .build();
        
        return General.builder()
            .id(generalId)
            .userId(userId)
            .name(name)
            .quality(quality)
            .type(type)
            .troopType(troopType)
            .level(level)
            .exp(0L)
            .maxExp(calculateMaxExp(level))
            .avatar("")
            .attributes(attributes)
            .soldiers(soldiers)
            .equipment(equipment)
            .tactics(tactics)
            .status(status)
            .stats(stats)
            .createTime(System.currentTimeMillis())
            .updateTime(System.currentTimeMillis())
            .build();
    }
    
    /**
     * 计算属性
     */
    private General.Attributes calculateAttributes(General.Quality quality, 
                                                  General.GeneralType type,
                                                  General.TroopType troopType,
                                                  int level) {
        // 基础值
        int baseAttack = 100;
        int baseDefense = 100;
        int baseValor = 50;
        int baseCommand = 50;
        double baseDodge = 10.0;
        int baseMobility = 50;
        
        // 成长率
        int attackGrowth = 5;
        int defenseGrowth = 5;
        int valorGrowth = 2;
        int commandGrowth = 2;
        double dodgeGrowth = 0.5;
        int mobilityGrowth = 2;
        
        // 获取倍率
        double qualityMultiplier = quality.getBaseMultiplier();
        Map<String, Double> typeAttr = type.getAttributes();
        Map<String, Double> troopAttr = troopType.getAttributes();
        
        // 计算最终属性
        int attack = (int)((baseAttack * qualityMultiplier * typeAttr.get("attack") * troopAttr.get("attack")) 
                     + (attackGrowth * (level - 1)));
        
        int defense = (int)((baseDefense * qualityMultiplier * typeAttr.get("defense") * troopAttr.get("defense"))
                      + (defenseGrowth * (level - 1)));
        
        int valor = (int)((baseValor * qualityMultiplier * typeAttr.get("valor"))
                    + (valorGrowth * (level - 1)));
        
        int command = (int)((baseCommand * qualityMultiplier * typeAttr.get("command"))
                      + (commandGrowth * (level - 1)));
        
        double dodge = Math.min(
            (baseDodge * qualityMultiplier * typeAttr.get("dodge") * troopAttr.get("dodge"))
            + (dodgeGrowth * (level - 1)), 
            100.0
        );
        
        int mobility = (int)((baseMobility * qualityMultiplier * typeAttr.get("mobility"))
                       + (mobilityGrowth * (level - 1)));
        
        // 计算战力
        int power = (int)(attack * 1.2 + defense * 1.2 + valor * 1.5 + command * 1.5 + dodge * 2 + mobility * 1.0);
        
        return General.Attributes.builder()
            .attack(attack)
            .defense(defense)
            .valor(valor)
            .command(command)
            .dodge(dodge)
            .mobility(mobility)
            .power(power)
            .build();
    }
    
    /**
     * 创建士兵信息
     */
    private General.Soldiers createSoldiers(General.TroopType troopType, int rank) {
        General.SoldierRankInfo rankInfo = getSoldierRankInfo(troopType.getName(), rank);
        
        return General.Soldiers.builder()
            .type(troopType)
            .rank(rank)
            .rankInfo(rankInfo)
            .count(1000)
            .maxCount(1000)
            .build();
    }
    
    /**
     * 获取士兵等级信息
     */
    private General.SoldierRankInfo getSoldierRankInfo(String troopTypeName, int rank) {
        // 这里简化处理，实际应该从配置文件读取
        Map<Integer, Map<String, Object>> soldierRanks = getSoldierRankMap(troopTypeName);
        Map<String, Object> rankData = soldierRanks.get(rank);
        
        return General.SoldierRankInfo.builder()
            .level(rank)
            .name((String)rankData.get("name"))
            .icon((String)rankData.get("icon"))
            .powerMultiplier((Double)rankData.get("powerMultiplier"))
            .build();
    }
    
    /**
     * 士兵等级映射（简化版）
     */
    private Map<Integer, Map<String, Object>> getSoldierRankMap(String troopType) {
        Map<Integer, Map<String, Object>> ranks = new HashMap<>();
        
        if ("步兵".equals(troopType)) {
            ranks.put(7, createRankMap("盾卫", "🛡️", 1.6));
            ranks.put(8, createRankMap("重盾兵", "🛡️", 1.75));
            ranks.put(9, createRankMap("刀盾兵", "🛡️", 1.9));
            ranks.put(10, createRankMap("精锐盾卫", "⭐", 2.1));
        } else if ("骑兵".equals(troopType)) {
            ranks.put(7, createRankMap("突骑", "🐎", 1.6));
            ranks.put(8, createRankMap("铁骑", "🐎", 1.75));
            ranks.put(9, createRankMap("重骑兵", "🐎", 1.9));
            ranks.put(10, createRankMap("玄甲骑", "⭐", 2.1));
        } else {
            ranks.put(7, createRankMap("连弩手", "🏹", 1.6));
            ranks.put(8, createRankMap("重弩兵", "🏹", 1.75));
            ranks.put(9, createRankMap("神臂弩手", "🏹", 1.9));
            ranks.put(10, createRankMap("床弩营", "⭐", 2.1));
        }
        
        return ranks;
    }
    
    /**
     * 创建士兵等级Map（Java 8兼容）
     */
    private Map<String, Object> createRankMap(String name, String icon, double powerMultiplier) {
        Map<String, Object> map = new HashMap<>();
        map.put("name", name);
        map.put("icon", icon);
        map.put("powerMultiplier", powerMultiplier);
        return map;
    }
    
    /**
     * 计算升级所需经验
     */
    private Long calculateMaxExp(int level) {
        return (long)(100 * Math.pow(1.2, level - 1));
    }
    
    /**
     * 公开的计算升级所需经验
     */
    public Long getMaxExpForLevel(int level) {
        return calculateMaxExp(level);
    }
    
    /**
     * 武将获得经验值
     * @param generalId 武将ID
     * @param expGain 获得的经验值
     * @return 升级信息
     */
    public Map<String, Object> addGeneralExp(String generalId, long expGain) {
        General general = generalRepository.findById(generalId);
        if (general == null) {
            throw new RuntimeException("武将不存在");
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("generalId", generalId);
        result.put("generalName", general.getName());
        result.put("expGained", expGain);
        
        long currentExp = general.getExp() != null ? general.getExp() : 0;
        long newExp = currentExp + expGain;
        int currentLevel = general.getLevel() != null ? general.getLevel() : 1;
        int newLevel = currentLevel;
        int levelsGained = 0;
        
        // 最高100级
        while (newLevel < 100 && newExp >= calculateMaxExp(newLevel)) {
            newExp -= calculateMaxExp(newLevel);
            newLevel++;
            levelsGained++;
        }
        
        // 更新武将数据
        general.setExp(newExp);
        general.setMaxExp(calculateMaxExp(newLevel));
        
        // 如果升级了，重新计算属性
        if (levelsGained > 0) {
            general.setLevel(newLevel);
            // 重新计算属性
            General.Attributes newAttrs = calculateAttributes(
                general.getQuality(), 
                general.getType(), 
                general.getTroopType(), 
                newLevel
            );
            general.setAttributes(newAttrs);
            
            logger.info("武将 {} 升级！{} -> {}，升了{}级", 
                       general.getName(), currentLevel, newLevel, levelsGained);
        }
        
        general.setUpdateTime(System.currentTimeMillis());
        generalRepository.save(general);
        
        result.put("levelUp", levelsGained > 0);
        result.put("levelsGained", levelsGained);
        result.put("oldLevel", currentLevel);
        result.put("newLevel", newLevel);
        result.put("currentExp", newExp);
        result.put("maxExp", calculateMaxExp(newLevel));
        
        return result;
    }
    
    /**
     * 批量给武将加经验（战斗后）
     */
    public List<Map<String, Object>> addBattleExpToGenerals(List<String> generalIds, int baseExp) {
        List<Map<String, Object>> results = new ArrayList<>();
        
        for (String generalId : generalIds) {
            try {
                Map<String, Object> expResult = addGeneralExp(generalId, baseExp);
                results.add(expResult);
            } catch (Exception e) {
                logger.error("给武将{}加经验失败: {}", generalId, e.getMessage());
            }
        }
        
        return results;
    }
    
    /**
     * 获取用户武将数量
     */
    public int getUserGeneralCount(String userId) {
        return generalRepository.countByUserId(userId);
    }
    
    /**
     * 检查是否可以招募新武将
     */
    public boolean canRecruitGeneral(String userId, int maxGeneralSlots) {
        int currentCount = getUserGeneralCount(userId);
        return currentCount < maxGeneralSlots;
    }
    
    /**
     * 保存武将
     */
    public General saveGeneral(General general) {
        general.setUpdateTime(System.currentTimeMillis());
        return generalRepository.save(general);
    }
    
    /**
     * 创建品质对象
     */
    private General.Quality createQuality(int id, String name, String color, double multiplier, int star, String icon) {
        return General.Quality.builder()
            .id(id)
            .name(name)
            .color(color)
            .baseMultiplier(multiplier)
            .star(star)
            .icon(icon)
            .build();
    }
    
    /**
     * 创建武将类型对象
     */
    private General.GeneralType createGeneralType(int id, String name, String desc, String icon) {
        Map<String, Double> attributes = new HashMap<>();
        
        switch (id) {
            case 1: // 攻击型
                attributes.put("attack", 1.3);
                attributes.put("defense", 0.7);
                attributes.put("valor", 1.3);
                attributes.put("command", 0.7);
                attributes.put("dodge", 1.0);
                attributes.put("mobility", 1.1);
                break;
            case 3: // 纯攻击型
                attributes.put("attack", 1.5);
                attributes.put("defense", 0.8);
                attributes.put("valor", 0.9);
                attributes.put("command", 0.8);
                attributes.put("dodge", 1.0);
                attributes.put("mobility", 1.0);
                break;
            case 4: // 纯武勇型
                attributes.put("attack", 1.0);
                attributes.put("defense", 1.0);
                attributes.put("valor", 1.5);
                attributes.put("command", 0.8);
                attributes.put("dodge", 1.0);
                attributes.put("mobility", 1.0);
                break;
            case 5: // 均衡型
                attributes.put("attack", 1.0);
                attributes.put("defense", 1.0);
                attributes.put("valor", 1.0);
                attributes.put("command", 1.0);
                attributes.put("dodge", 1.0);
                attributes.put("mobility", 1.0);
                break;
            case 6: // 敏捷型
                attributes.put("attack", 0.9);
                attributes.put("defense", 0.9);
                attributes.put("valor", 0.9);
                attributes.put("command", 0.9);
                attributes.put("dodge", 1.4);
                attributes.put("mobility", 1.4);
                break;
            case 7: // 统帅型
                attributes.put("attack", 0.9);
                attributes.put("defense", 1.1);
                attributes.put("valor", 0.8);
                attributes.put("command", 1.4);
                attributes.put("dodge", 1.0);
                attributes.put("mobility", 1.2);
                break;
            default:
                attributes.put("attack", 1.0);
                attributes.put("defense", 1.0);
                attributes.put("valor", 1.0);
                attributes.put("command", 1.0);
                attributes.put("dodge", 1.0);
                attributes.put("mobility", 1.0);
        }
        
        return General.GeneralType.builder()
            .id(id)
            .name(name)
            .description(desc)
            .icon(icon)
            .attributes(attributes)
            .build();
    }
    
    /**
     * 解雇武将
     */
    public boolean dismissGeneral(String userId, String generalId) {
        General general = generalRepository.findById(generalId);
        if (general == null) {
            throw new RuntimeException("武将不存在");
        }
        if (!general.getUserId().equals(userId)) {
            throw new RuntimeException("无权操作该武将");
        }
        if (general.getStatus() != null && general.getStatus().getLocked() != null && general.getStatus().getLocked()) {
            throw new RuntimeException("武将已锁定，无法解雇");
        }
        
        generalRepository.delete(generalId);
        logger.info("解雇武将: userId={}, generalId={}, name={}", userId, generalId, general.getName());
        return true;
    }
    
    /**
     * 将领传承 - 将源武将的经验传给目标武将，源武将消失
     */
    public Map<String, Object> inheritGeneral(String userId, String sourceGeneralId, String targetGeneralId, String scrollType) {
        General source = generalRepository.findById(sourceGeneralId);
        General target = generalRepository.findById(targetGeneralId);
        
        if (source == null || target == null) {
            throw new RuntimeException("武将不存在");
        }
        if (!source.getUserId().equals(userId) || !target.getUserId().equals(userId)) {
            throw new RuntimeException("无权操作该武将");
        }
        if (sourceGeneralId.equals(targetGeneralId)) {
            throw new RuntimeException("不能传承给自己");
        }
        
        // 计算传承率
        double rate;
        switch (scrollType) {
            case "basic": rate = 0.5; break;
            case "medium": rate = 0.75; break;
            case "advanced": rate = 1.0; break;
            default: rate = 0.5;
        }
        
        // 计算传承经验
        long sourceExp = source.getExp() != null ? source.getExp() : 0;
        // 加上源武将等级对应的总经验
        for (int i = 1; i < source.getLevel(); i++) {
            sourceExp += calculateMaxExp(i);
        }
        
        long expGained = (long)(sourceExp * rate);
        
        // 给目标武将加经验
        Map<String, Object> expResult = addGeneralExp(targetGeneralId, expGained);
        
        // 删除源武将
        generalRepository.delete(sourceGeneralId);
        
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("expGained", expGained);
        result.put("sourceGeneral", source.getName());
        result.put("targetGeneral", target.getName());
        result.put("levelUp", expResult.get("levelUp"));
        result.put("newLevel", expResult.get("newLevel"));
        
        logger.info("将领传承: {} -> {}, 传承经验: {}", source.getName(), target.getName(), expGained);
        
        return result;
    }
    
    /**
     * 军事演习 - 使用演习令获得经验
     */
    public Map<String, Object> drill(String userId, String generalId, String drillType, int count) {
        General general = generalRepository.findById(generalId);
        if (general == null) {
            throw new RuntimeException("武将不存在");
        }
        if (!general.getUserId().equals(userId)) {
            throw new RuntimeException("无权操作该武将");
        }
        
        // 计算经验
        int expPerDrill;
        switch (drillType) {
            case "small": expPerDrill = 100; break;
            case "medium": expPerDrill = 500; break;
            case "large": expPerDrill = 2000; break;
            default: expPerDrill = 100;
        }
        
        long totalExp = (long)expPerDrill * count;
        
        // 给武将加经验
        Map<String, Object> expResult = addGeneralExp(generalId, totalExp);
        
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("expGained", totalExp);
        result.put("drillType", drillType);
        result.put("count", count);
        result.put("levelUp", expResult.get("levelUp"));
        result.put("newLevel", expResult.get("newLevel"));
        
        logger.info("军事演习: {} 使用 {} x{}, 获得经验: {}", general.getName(), drillType, count, totalExp);
        
        return result;
    }
    
    /**
     * 创建兵种类型对象
     */
    private General.TroopType createTroopType(int id, String name, String icon, String desc, 
                                             String restrains, String restrainedBy) {
        Map<String, Double> attributes = new HashMap<>();
        
        switch (id) {
            case 1: // 步兵
                attributes.put("attack", 0.8);
                attributes.put("defense", 1.3);
                attributes.put("dodge", 1.5);
                break;
            case 2: // 骑兵
                attributes.put("attack", 1.0);
                attributes.put("defense", 1.0);
                attributes.put("dodge", 1.0);
                break;
            case 3: // 弓兵
                attributes.put("attack", 1.3);
                attributes.put("defense", 0.7);
                attributes.put("dodge", 1.0);
                break;
        }
        
        return General.TroopType.builder()
            .id(id)
            .name(name)
            .icon(icon)
            .description(desc)
            .attributes(attributes)
            .restrains(restrains)
            .restrainedBy(restrainedBy)
            .restrainBonus(0.3)
            .build();
    }
}

