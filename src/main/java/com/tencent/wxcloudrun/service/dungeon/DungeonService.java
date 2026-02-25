package com.tencent.wxcloudrun.service.dungeon;

import com.tencent.wxcloudrun.config.DungeonConfig;
import com.tencent.wxcloudrun.config.EquipmentConfig;
import com.tencent.wxcloudrun.exception.BusinessException;
import com.tencent.wxcloudrun.model.*;
import com.tencent.wxcloudrun.repository.DungeonProgressRepository;
import com.tencent.wxcloudrun.repository.EquipmentPreRepository;
import com.tencent.wxcloudrun.repository.EquipmentRepository;
import com.tencent.wxcloudrun.service.general.GeneralService;
import com.tencent.wxcloudrun.service.level.LevelService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.*;

/**
 * 副本服务
 */
@Service
public class DungeonService {
    
    private static final Logger logger = LoggerFactory.getLogger(DungeonService.class);
    
    @Autowired
    private DungeonConfig dungeonConfig;
    
    @Autowired
    private EquipmentConfig equipmentConfig;
    
    @Autowired
    private DungeonProgressRepository progressRepository;
    
    @Autowired
    private EquipmentRepository equipmentRepository;
    
    @Autowired
    private EquipmentPreRepository equipmentPreRepository;
    
    @Autowired
    private LevelService levelService;
    
    @Autowired
    private GeneralService generalService;
    
    private final Random random = new Random();
    
    /**
     * 获取所有副本
     */
    public Map<String, Dungeon> getAllDungeons() {
        return dungeonConfig.getAllDungeons();
    }
    
    /**
     * 获取已解锁的副本
     */
    public List<Dungeon> getUnlockedDungeons(int playerLevel) {
        return dungeonConfig.getUnlockedDungeons(playerLevel);
    }
    
    /**
     * 获取副本详情
     */
    public Dungeon getDungeonDetail(String dungeonId) {
        Dungeon dungeon = dungeonConfig.getDungeon(dungeonId);
        if (dungeon == null) {
            throw new BusinessException(404, "副本不存在");
        }
        return dungeon;
    }
    
    /**
     * 获取用户副本进度
     */
    public DungeonProgress getUserProgress(String userId, String dungeonId) {
        DungeonProgress progress = progressRepository.findByUserIdAndDungeonId(userId, dungeonId);
        if (progress == null) {
            progress = progressRepository.initProgress(userId, dungeonId);
        }
        
        // 检查是否需要重置每日次数
        String today = new SimpleDateFormat("yyyyMMdd").format(new Date());
        if (!today.equals(progress.getLastEntryDate())) {
            progress.setTodayEntries(0);
            progress.setLastEntryDate(today);
            progressRepository.save(progress);
        }
        
        return progress;
    }
    
    /**
     * 获取用户所有副本进度
     */
    public List<DungeonProgress> getUserAllProgress(String userId) {
        return progressRepository.findByUserId(userId);
    }
    
    /**
     * 进入副本（消耗体力）
     * 如果玩家有未完成的进度（已击败部分NPC但未通关），则恢复进度不消耗次数
     * @return 返回剩余次数
     */
    public int enterDungeon(String userId, String dungeonId, int playerLevel, int currentStamina) {
        Dungeon dungeon = getDungeonDetail(dungeonId);
        
        if (playerLevel < dungeon.getUnlockLevel()) {
            throw new BusinessException(400, "等级不足，需要" + dungeon.getUnlockLevel() + "级");
        }
        
        DungeonProgress progress = getUserProgress(userId, dungeonId);
        
        // 如果有未完成的进度（已击败部分NPC但未通关），恢复进度不消耗次数和体力
        boolean hasOngoingProgress = !progress.getCleared() 
                && progress.getDefeatedNpcs() != null 
                && !progress.getDefeatedNpcs().isEmpty();
        
        if (hasOngoingProgress) {
            logger.info("用户 {} 恢复副本 {} 进度，已击败 {} 个NPC", 
                       userId, dungeonId, progress.getDefeatedNpcs().size());
            return dungeon.getDailyLimit() - progress.getTodayEntries();
        }
        
        if (currentStamina < dungeon.getStaminaCost()) {
            throw new BusinessException(400, "体力不足");
        }
        
        if (progress.getTodayEntries() >= dungeon.getDailyLimit()) {
            throw new BusinessException(400, "今日进入次数已用完");
        }
        
        progress.setTodayEntries(progress.getTodayEntries() + 1);
        progressRepository.save(progress);
        
        logger.info("用户 {} 进入副本 {}，今日第{}次", userId, dungeonId, progress.getTodayEntries());
        
        return dungeon.getDailyLimit() - progress.getTodayEntries();
    }
    
    /**
     * 挑战NPC
     */
    public BattleResult challengeNpc(String userId, String dungeonId, int npcIndex,
                                     General playerGeneral, int playerLevel) {
        Dungeon dungeon = getDungeonDetail(dungeonId);
        DungeonProgress progress = getUserProgress(userId, dungeonId);
        
        // 检查NPC序号
        if (npcIndex < 1 || npcIndex > dungeon.getNpcCount()) {
            throw new BusinessException(400, "无效的NPC序号");
        }
        
        // 检查是否需要按顺序挑战（必须先击败前面的NPC）
        if (npcIndex > 1 && !progress.getDefeatedNpcs().contains(npcIndex - 1)) {
            throw new BusinessException(400, "请先击败前面的守关NPC");
        }
        
        // 检查是否已击败
        if (progress.getDefeatedNpcs().contains(npcIndex)) {
            throw new BusinessException(400, "该NPC已被击败");
        }
        
        // 获取NPC
        DungeonNpc npc = dungeon.getNpcs().get(npcIndex - 1);
        
        // 执行战斗
        BattleResult result = executeBattle(playerGeneral, npc);
        
        // 如果胜利，更新进度
        if (result.isVictory()) {
            progress.getDefeatedNpcs().add(npcIndex);
            progress.setCurrentProgress(progress.getDefeatedNpcs().size());
            
            // 判断是否首次击败
            boolean isFirstDefeat = progress.getClearCount() == 0;
            
            // 给予经验奖励
            int baseExp = npc.getExpReward() != null ? npc.getExpReward() : 100;
            Map<String, Object> expResult = levelService.addDungeonExp(userId, baseExp, isFirstDefeat);
            
            // 设置经验信息到结果
            result.setExpGained(baseExp);
            result.setBonusExp(((Number) expResult.get("bonusExp")).intValue());
            result.setTotalExpGained(((Number) expResult.get("totalExpGained")).intValue());
            result.setLevelUp((Boolean) expResult.get("levelUp"));
            result.setLevelsGained(((Number) expResult.get("levelsGained")).intValue());
            result.setCurrentLevel(((Number) expResult.get("currentLevel")).intValue());
            result.setCurrentLevelExp(((Number) expResult.get("currentLevelExp")).longValue());
            result.setExpToNextLevel(((Number) expResult.get("expToNextLevel")).longValue());
            
            // 检查是否通关
            if (progress.getCurrentProgress() >= dungeon.getNpcCount()) {
                progress.setCleared(true);
                progress.setClearCount(progress.getClearCount() + 1);
                result.setDungeonCleared(true);
                result.setClearReward(dungeon.getClearReward());
                
                // 通关额外经验
                if (dungeon.getClearReward() != null && dungeon.getClearReward().getExp() != null) {
                    levelService.addDungeonExp(userId, dungeon.getClearReward().getExp(), isFirstDefeat);
                }
                
                logger.info("用户 {} 通关副本 {}", userId, dungeonId);
            }
            
            progressRepository.save(progress);
            
            // 处理装备掉落
            if (npc.getDropEquipment() && npc.getDropRate() != null) {
                int roll = random.nextInt(100);
                if (roll < npc.getDropRate()) {
                    Equipment droppedEquipment = generateDropEquipment(userId, npc, dungeonId);
                    result.setDroppedEquipment(droppedEquipment);
                    logger.info("用户 {} 击败 {} 获得装备 {}", userId, npc.getName(), droppedEquipment.getName());
                }
            }
        }
        
        return result;
    }
    
    /**
     * 执行战斗
     */
    private BattleResult executeBattle(General player, DungeonNpc npc) {
        // 简化的战斗逻辑
        // 基于双方战力计算胜率
        int playerPower = player.getAttributes().getPower();
        int npcPower = npc.getPower();
        
        // 战力差距影响胜率
        double powerRatio = (double) playerPower / (playerPower + npcPower);
        double winRate = Math.min(Math.max(powerRatio * 1.2, 0.1), 0.95); // 10%-95%
        
        boolean victory = random.nextDouble() < winRate;
        
        // 计算伤亡
        int playerLoss = 0;
        int npcLoss = 0;
        
        if (victory) {
            npcLoss = npc.getSoldiers();
            playerLoss = (int)(player.getSoldiers().getCount() * (1 - powerRatio) * 0.3);
        } else {
            playerLoss = (int)(player.getSoldiers().getCount() * 0.5);
            npcLoss = (int)(npc.getSoldiers() * powerRatio * 0.5);
        }
        
        // 构建战斗过程描述
        List<String> battleLog = new ArrayList<>();
        battleLog.add(String.format("【战斗开始】%s(战力%d) VS %s(战力%d)", 
            player.getName(), playerPower, npc.getName(), npcPower));
        
        if (victory) {
            battleLog.add(String.format("【战斗结束】%s 获胜！", player.getName()));
            battleLog.add(String.format("己方损失兵力: %d，敌方全灭", playerLoss));
        } else {
            battleLog.add(String.format("【战斗结束】%s 战败！", player.getName()));
            battleLog.add(String.format("己方损失兵力: %d，敌方损失兵力: %d", playerLoss, npcLoss));
        }
        
        return BattleResult.builder()
            .victory(victory)
            .playerLoss(playerLoss)
            .npcLoss(npcLoss)
            .battleLog(battleLog)
            .npcIndex(npc.getIndex())
            .npcName(npc.getName())
            .npcQuality(npc.getQualityName())
            .build();
    }
    
    /**
     * 生成掉落装备 - 优先从 equipment_pre 模板生成
     */
    private Equipment generateDropEquipment(String userId, DungeonNpc npc, String dungeonId) {
        // 如果配置了 equipment_pre IDs，从模板生成
        if (npc.getDropEquipPreIds() != null && !npc.getDropEquipPreIds().isEmpty()) {
            return generateFromPreTemplate(userId, npc, dungeonId);
        }
        // 降级：随机生成
        return generateRandomDropEquipment(userId, npc, dungeonId);
    }

    /**
     * 基于 equipment_pre 模板生成装备
     */
    private Equipment generateFromPreTemplate(String userId, DungeonNpc npc, String dungeonId) {
        List<Integer> preIds = npc.getDropEquipPreIds();
        Integer pickedId = preIds.get(random.nextInt(preIds.size()));
        EquipmentPre pre = equipmentPreRepository.findById(pickedId);

        if (pre == null) {
            logger.warn("equipment_pre id={} 未找到，降级为随机生成", pickedId);
            return generateRandomDropEquipment(userId, npc, dungeonId);
        }

        String equipmentId = "equip_" + System.currentTimeMillis() + "_" +
                UUID.randomUUID().toString().substring(0, 8);

        int slotTypeId = pre.getSlotTypeId();
        Equipment.SlotType slotType = equipmentConfig.getSlotType(slotTypeId);
        int qualityId = pre.getDefaultQualityId();
        Equipment.Quality quality = equipmentConfig.getQuality(qualityId);

        Equipment.SetInfo setInfo = null;
        if (pre.getSetName() != null && !pre.getSetName().isEmpty()) {
            setInfo = Equipment.SetInfo.builder()
                    .setId("SET_" + pre.getSetName())
                    .setName(pre.getSetName())
                    .setLevel(pre.getLevel())
                    .threeSetEffect(pre.getSetEffect3())
                    .sixSetEffect(pre.getSetEffect6())
                    .build();
        }

        Equipment.Attributes baseAttributes = Equipment.Attributes.builder()
                .attack(pre.getAttack() != null ? pre.getAttack() : 0)
                .defense(pre.getDefense() != null ? pre.getDefense() : 0)
                .hp(pre.getSoldierHp() != null ? pre.getSoldierHp() : 0)
                .mobility(pre.getMobility() != null ? pre.getMobility() : 0)
                .build();

        Equipment equipment = Equipment.builder()
                .id(equipmentId)
                .userId(userId)
                .name(pre.getName())
                .slotType(slotType)
                .level(pre.getLevel())
                .quality(quality)
                .setInfo(setInfo)
                .baseAttributes(baseAttributes)
                .bonusAttributes(Equipment.Attributes.builder().build())
                .source(Equipment.Source.builder()
                        .type("DUNGEON")
                        .name("副本掉落")
                        .detail(dungeonId)
                        .build())
                .equipped(false)
                .equippedGeneralId(null)
                .icon(slotType.getIcon())
                .description(String.format("%s - %s级%s装备",
                        pre.getSetName() != null ? pre.getSetName() + "套装" : "副本装备",
                        pre.getLevel(), quality.getName()))
                .createTime(System.currentTimeMillis())
                .updateTime(System.currentTimeMillis())
                .build();

        equipmentRepository.save(equipment);
        return equipment;
    }

    /**
     * 随机生成掉落装备（降级方案，无 equipment_pre 模板时使用）
     */
    private Equipment generateRandomDropEquipment(String userId, DungeonNpc npc, String dungeonId) {
        String equipmentId = "equip_" + System.currentTimeMillis() + "_" +
                UUID.randomUUID().toString().substring(0, 8);

        int slotTypeId = random.nextInt(6) + 1;
        Equipment.SlotType slotType = equipmentConfig.getSlotType(slotTypeId);

        int qualityId;
        String dt = npc.getDropType() != null ? npc.getDropType() : "";
        switch (dt) {
            case "CRAFT":  qualityId = random.nextInt(100) < 70 ? 1 : 2; break;
            case "BLUE":   qualityId = 3; break;
            case "RED":    qualityId = 6; break;
            case "DUNGEON": qualityId = 3; break;
            default:       qualityId = 2;
        }
        Equipment.Quality quality = equipmentConfig.getQuality(qualityId);
        int level = npc.getDropLevel();

        String setId = null;
        List<Equipment.SetInfo> sets = equipmentConfig.getEquipmentSetsByLevel(level);
        if (!sets.isEmpty()) {
            setId = sets.get(random.nextInt(sets.size())).getSetId();
        }
        Equipment.SetInfo setInfo = setId != null ? equipmentConfig.getEquipmentSet(setId) : null;

        String name = generateEquipmentName(slotTypeId, level, quality);
        Equipment.Attributes baseAttributes = calculateBaseAttributes(slotType, level, quality);
        Equipment.Attributes bonusAttributes = calculateBonusAttributes(level, quality);

        Equipment equipment = Equipment.builder()
                .id(equipmentId)
                .userId(userId)
                .name(name)
                .slotType(slotType)
                .level(level)
                .quality(quality)
                .setInfo(setInfo)
                .baseAttributes(baseAttributes)
                .bonusAttributes(bonusAttributes)
                .source(Equipment.Source.builder()
                        .type("DUNGEON")
                        .name("副本掉落")
                        .detail(dungeonId)
                        .build())
                .equipped(false)
                .equippedGeneralId(null)
                .icon(slotType.getIcon())
                .description(String.format("来自副本的%s级%s装备", level, quality.getName()))
                .createTime(System.currentTimeMillis())
                .updateTime(System.currentTimeMillis())
                .build();

        equipmentRepository.save(equipment);
        return equipment;
    }
    
    /**
     * 重置副本进度（用于已通关副本的再次挑战）
     */
    public DungeonProgress resetProgress(String userId, String dungeonId) {
        DungeonProgress progress = getUserProgress(userId, dungeonId);
        progress.setCurrentProgress(0);
        progress.setDefeatedNpcs(new HashSet<>());
        progress.setCleared(false);
        return progressRepository.save(progress);
    }
    
    /**
     * 放弃副本挑战 - 重置进度并退还本次挑战次数
     */
    public DungeonProgress abandonDungeon(String userId, String dungeonId) {
        DungeonProgress progress = getUserProgress(userId, dungeonId);
        
        if (progress.getCleared()) {
            throw new BusinessException(400, "副本已通关，无需放弃");
        }
        
        progress.setCurrentProgress(0);
        progress.setDefeatedNpcs(new HashSet<>());
        progress.setCleared(false);
        
        // 退还挑战次数
        if (progress.getTodayEntries() > 0) {
            progress.setTodayEntries(progress.getTodayEntries() - 1);
        }
        
        progressRepository.save(progress);
        
        logger.info("用户 {} 放弃副本 {}，退还挑战次数，剩余次数: {}/{}", 
                   userId, dungeonId, 
                   getDungeonDetail(dungeonId).getDailyLimit() - progress.getTodayEntries(),
                   getDungeonDetail(dungeonId).getDailyLimit());
        
        return progress;
    }
    
    // ==================== 私有辅助方法 ====================
    
    private static final Map<Integer, List<String>> WEAPON_NAMES = new HashMap<>();
    private static final Map<Integer, List<String>> HELMET_NAMES = new HashMap<>();
    private static final Map<Integer, List<String>> ARMOR_NAMES = new HashMap<>();
    private static final Map<Integer, List<String>> RING_NAMES = new HashMap<>();
    private static final Map<Integer, List<String>> SHOES_NAMES = new HashMap<>();
    private static final Map<Integer, List<String>> NECKLACE_NAMES = new HashMap<>();
    
    static {
        WEAPON_NAMES.put(20, Arrays.asList("青铜剑", "铁刀"));
        WEAPON_NAMES.put(40, Arrays.asList("精钢剑", "百炼刀"));
        WEAPON_NAMES.put(50, Arrays.asList("玄铁剑", "偃月刀"));
        WEAPON_NAMES.put(60, Arrays.asList("青龙剑", "青龙刀"));
        WEAPON_NAMES.put(80, Arrays.asList("倚天剑", "屠龙刀"));
        WEAPON_NAMES.put(100, Arrays.asList("轩辕剑", "盘古斧"));
        
        HELMET_NAMES.put(20, Arrays.asList("皮盔", "铁盔"));
        HELMET_NAMES.put(40, Arrays.asList("钢盔", "银盔"));
        HELMET_NAMES.put(50, Arrays.asList("玄铁盔", "紫金盔"));
        HELMET_NAMES.put(60, Arrays.asList("龙鳞盔", "凤翎冠"));
        HELMET_NAMES.put(80, Arrays.asList("天王盔", "战神冠"));
        HELMET_NAMES.put(100, Arrays.asList("混沌盔", "创世冠"));
        
        ARMOR_NAMES.put(20, Arrays.asList("皮甲", "链甲"));
        ARMOR_NAMES.put(40, Arrays.asList("钢甲", "锁子甲"));
        ARMOR_NAMES.put(50, Arrays.asList("玄铁甲", "龙纹甲"));
        ARMOR_NAMES.put(60, Arrays.asList("龙鳞甲", "麒麟甲"));
        ARMOR_NAMES.put(80, Arrays.asList("天神甲", "战神铠"));
        ARMOR_NAMES.put(100, Arrays.asList("混沌甲", "创世铠"));
        
        RING_NAMES.put(20, Arrays.asList("铜戒", "银戒"));
        RING_NAMES.put(40, Arrays.asList("金戒", "宝石戒"));
        RING_NAMES.put(50, Arrays.asList("灵石戒", "紫晶戒"));
        RING_NAMES.put(60, Arrays.asList("龙魂戒", "凤血戒"));
        RING_NAMES.put(80, Arrays.asList("天命戒", "造化戒"));
        RING_NAMES.put(100, Arrays.asList("混沌戒", "创世戒"));
        
        SHOES_NAMES.put(20, Arrays.asList("布靴", "皮靴"));
        SHOES_NAMES.put(40, Arrays.asList("千里靴", "疾风靴"));
        SHOES_NAMES.put(50, Arrays.asList("追风靴", "飞云靴"));
        SHOES_NAMES.put(60, Arrays.asList("龙行靴", "腾云靴"));
        SHOES_NAMES.put(80, Arrays.asList("神行靴", "凌霄靴"));
        SHOES_NAMES.put(100, Arrays.asList("混沌靴", "创世靴"));
        
        NECKLACE_NAMES.put(20, Arrays.asList("铜链", "银链"));
        NECKLACE_NAMES.put(40, Arrays.asList("金链", "珍珠链"));
        NECKLACE_NAMES.put(50, Arrays.asList("灵石链", "紫晶坠"));
        NECKLACE_NAMES.put(60, Arrays.asList("龙魂坠", "凤凰坠"));
        NECKLACE_NAMES.put(80, Arrays.asList("天命坠", "九天链"));
        NECKLACE_NAMES.put(100, Arrays.asList("混沌坠", "创世链"));
    }
    
    private String generateEquipmentName(int slotTypeId, int level, Equipment.Quality quality) {
        List<String> names;
        switch (slotTypeId) {
            case 1: names = WEAPON_NAMES.getOrDefault(level, Arrays.asList("武器")); break;
            case 2: names = HELMET_NAMES.getOrDefault(level, Arrays.asList("头盔")); break;
            case 3: names = ARMOR_NAMES.getOrDefault(level, Arrays.asList("铠甲")); break;
            case 4: names = RING_NAMES.getOrDefault(level, Arrays.asList("戒指")); break;
            case 5: names = SHOES_NAMES.getOrDefault(level, Arrays.asList("鞋子")); break;
            case 6: names = NECKLACE_NAMES.getOrDefault(level, Arrays.asList("项链")); break;
            default: names = Arrays.asList("装备");
        }
        
        String baseName = names.get(random.nextInt(names.size()));
        
        if (quality.getId() >= 4) {
            String[] prefixes = {"精良的", "优秀的", "卓越的", "传说的", "神圣的"};
            baseName = prefixes[Math.min(quality.getId() - 4, prefixes.length - 1)] + baseName;
        }
        
        return baseName;
    }
    
    private Equipment.Attributes calculateBaseAttributes(Equipment.SlotType slotType, 
                                                         int level, Equipment.Quality quality) {
        int baseValue = level * 5;
        double multiplier = quality.getMultiplier();
        
        Equipment.Attributes.AttributesBuilder builder = Equipment.Attributes.builder();
        
        switch (slotType.getId()) {
            case 1:
                builder.attack((int)(baseValue * 2 * multiplier));
                builder.valor((int)(baseValue * 0.5 * multiplier));
                builder.critRate(quality.getId() * 1.0);
                break;
            case 2:
                builder.defense((int)(baseValue * 1.5 * multiplier));
                builder.hp((int)(baseValue * 10 * multiplier));
                break;
            case 3:
                builder.defense((int)(baseValue * 2 * multiplier));
                builder.hp((int)(baseValue * 15 * multiplier));
                builder.dodge(quality.getId() * 0.5);
                break;
            case 4:
                builder.attack((int)(baseValue * 1.5 * multiplier));
                builder.critDamage(quality.getId() * 5.0);
                break;
            case 5:
                builder.mobility((int)(baseValue * 1.5 * multiplier));
                builder.dodge(quality.getId() * 1.0);
                break;
            case 6:
                builder.command((int)(baseValue * 1.5 * multiplier));
                builder.valor((int)(baseValue * 0.5 * multiplier));
                break;
        }
        
        return builder.build();
    }
    
    private Equipment.Attributes calculateBonusAttributes(int level, Equipment.Quality quality) {
        Equipment.Attributes.AttributesBuilder builder = Equipment.Attributes.builder();
        
        int bonusCount = Math.min(quality.getId(), 4);
        int baseBonus = (int)(level * quality.getMultiplier());
        
        List<String> availableStats = new ArrayList<>(Arrays.asList("attack", "defense", "valor", "command", "mobility", "hp"));
        Collections.shuffle(availableStats);
        
        for (int i = 0; i < bonusCount; i++) {
            String stat = availableStats.get(i);
            int value = baseBonus / 2 + random.nextInt(Math.max(baseBonus, 1));
            
            switch (stat) {
                case "attack": builder.attack(value); break;
                case "defense": builder.defense(value); break;
                case "valor": builder.valor(value / 2); break;
                case "command": builder.command(value / 2); break;
                case "mobility": builder.mobility(value / 3); break;
                case "hp": builder.hp(value * 5); break;
            }
        }
        
        return builder.build();
    }
    
    // ==================== 战斗结果内部类 ====================
    
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class BattleResult {
        private boolean victory;
        private int playerLoss;
        private int npcLoss;
        private List<String> battleLog;
        private int npcIndex;
        private String npcName;
        private String npcQuality;
        private boolean dungeonCleared;
        private Dungeon.Reward clearReward;
        private Equipment droppedEquipment;
        
        // 经验相关
        private int expGained;           // 本次获得的基础经验
        private int bonusExp;            // VIP加成经验
        private int totalExpGained;      // 总共获得的经验
        private boolean levelUp;         // 是否升级
        private int levelsGained;        // 升了几级
        private int currentLevel;        // 当前等级
        private long currentLevelExp;    // 当前等级经验
        private long expToNextLevel;     // 升级还需经验
    }
}

