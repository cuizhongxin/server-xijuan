package com.tencent.wxcloudrun.service.campaign;

import com.tencent.wxcloudrun.exception.BusinessException;
import com.tencent.wxcloudrun.model.*;
import com.tencent.wxcloudrun.repository.CampaignRepository;
import com.tencent.wxcloudrun.repository.EquipmentRepository;
import com.tencent.wxcloudrun.service.general.GeneralService;
import com.tencent.wxcloudrun.service.UserResourceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import com.tencent.wxcloudrun.service.battle.BattleCalculator;
import com.tencent.wxcloudrun.config.TacticsConfig;
import com.tencent.wxcloudrun.config.TacticsConfig.TacticsTemplate;
import com.tencent.wxcloudrun.dao.UserTacticsMapper;

import javax.annotation.PostConstruct;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class CampaignService {
    
    private final CampaignRepository campaignRepository;
    private final UserResourceService userResourceService;
    private final GeneralService generalService;
    private final EquipmentRepository equipmentRepository;
    private final TacticsConfig tacticsConfig;
    private final UserTacticsMapper userTacticsMapper;
    private final com.tencent.wxcloudrun.service.herorank.PeerageService peerageService;
    
    // 战役配置
    private final Map<String, Campaign> campaignConfigs = new ConcurrentHashMap<>();
    
    @PostConstruct
    public void init() {
        initCampaignConfigs();
    }
    
    /**
     * 初始化战役配置
     */
    private void initCampaignConfigs() {
        // 战役1: 乱世枭雄
        Campaign campaign1 = Campaign.builder()
                .id("campaign_luanshi")
                .name("乱世枭雄")
                .description("黄巾起义，天下大乱，各路诸侯纷纷崛起")
                .icon("/images/campaign/luanshi.png")
                .backgroundImage("/images/campaign/bg_luanshi.jpg")
                .enemyLevelMin(1)
                .enemyLevelMax(20)
                .expRewardMin(500)
                .expRewardMax(2000)
                .dailyLimit(3)
                .staminaCost(6)
                .requiredLevel(1)
                .order(1)
                .stages(createLuanshiStages())
                .dropPreviews(Arrays.asList(
                    Campaign.DropPreview.builder().icon("/images/equip/sword1.png").name("青铜剑").quality("优秀").build(),
                    Campaign.DropPreview.builder().icon("/images/equip/armor1.png").name("皮甲").quality("优秀").build(),
                    Campaign.DropPreview.builder().icon("/images/equip/helmet1.png").name("铁盔").quality("精良").build()
                ))
                .build();
        
        // 战役2: 四世三公
        Campaign campaign2 = Campaign.builder()
                .id("campaign_sishi")
                .name("四世三公")
                .description("袁氏家族四代位居三公，门生故吏遍布天下")
                .icon("/images/campaign/sishi.png")
                .backgroundImage("/images/campaign/bg_sishi.jpg")
                .enemyLevelMin(20)
                .enemyLevelMax(40)
                .expRewardMin(2000)
                .expRewardMax(6000)
                .dailyLimit(3)
                .staminaCost(8)
                .requiredLevel(15)
                .order(2)
                .stages(createSishiStages())
                .dropPreviews(Arrays.asList(
                    Campaign.DropPreview.builder().icon("/images/equip/sword2.png").name("精钢剑").quality("精良").build(),
                    Campaign.DropPreview.builder().icon("/images/equip/armor2.png").name("锁子甲").quality("精良").build(),
                    Campaign.DropPreview.builder().icon("/images/equip/ring1.png").name("玉扳指").quality("史诗").build()
                ))
                .build();
        
        // 战役3: 战神吕布
        Campaign campaign3 = Campaign.builder()
                .id("campaign_lvbu")
                .name("战神吕布")
                .description("三姓家奴？天下无双！人中吕布，马中赤兔")
                .icon("/images/campaign/lvbu.png")
                .backgroundImage("/images/campaign/bg_lvbu.jpg")
                .enemyLevelMin(60)
                .enemyLevelMax(80)
                .expRewardMin(8000)
                .expRewardMax(26000)
                .dailyLimit(2)
                .staminaCost(10)
                .requiredLevel(40)
                .order(3)
                .stages(createLvbuStages())
                .dropPreviews(Arrays.asList(
                    Campaign.DropPreview.builder().icon("/images/equip/halberd.png").name("方天画戟").quality("传说").build(),
                    Campaign.DropPreview.builder().icon("/images/equip/armor3.png").name("兽面吞头连环铠").quality("史诗").build(),
                    Campaign.DropPreview.builder().icon("/images/equip/horse.png").name("赤兔马").quality("传说").build()
                ))
                .build();
        
        // 战役4: 官渡之战
        Campaign campaign4 = Campaign.builder()
                .id("campaign_guandu")
                .name("官渡之战")
                .description("曹操以少胜多，奠定统一北方的基础")
                .icon("/images/campaign/guandu.png")
                .backgroundImage("/images/campaign/bg_guandu.jpg")
                .enemyLevelMin(40)
                .enemyLevelMax(60)
                .expRewardMin(5000)
                .expRewardMax(15000)
                .dailyLimit(2)
                .staminaCost(10)
                .requiredLevel(30)
                .order(4)
                .stages(createGuanduStages())
                .dropPreviews(Arrays.asList(
                    Campaign.DropPreview.builder().icon("/images/equip/sword3.png").name("倚天剑").quality("史诗").build(),
                    Campaign.DropPreview.builder().icon("/images/equip/armor4.png").name("玄铁重甲").quality("史诗").build()
                ))
                .build();
        
        // 战役5: 赤壁之战
        Campaign campaign5 = Campaign.builder()
                .id("campaign_chibi")
                .name("赤壁之战")
                .description("孙刘联军大败曹操，奠定三分天下格局")
                .icon("/images/campaign/chibi.png")
                .backgroundImage("/images/campaign/bg_chibi.jpg")
                .enemyLevelMin(80)
                .enemyLevelMax(100)
                .expRewardMin(20000)
                .expRewardMax(50000)
                .dailyLimit(1)
                .staminaCost(15)
                .requiredLevel(60)
                .order(5)
                .stages(createChibiStages())
                .dropPreviews(Arrays.asList(
                    Campaign.DropPreview.builder().icon("/images/equip/fan.png").name("羽扇").quality("传说").build(),
                    Campaign.DropPreview.builder().icon("/images/equip/robe.png").name("八卦道袍").quality("传说").build()
                ))
                .build();
        
        campaignConfigs.put(campaign1.getId(), campaign1);
        campaignConfigs.put(campaign2.getId(), campaign2);
        campaignConfigs.put(campaign3.getId(), campaign3);
        campaignConfigs.put(campaign4.getId(), campaign4);
        campaignConfigs.put(campaign5.getId(), campaign5);
        
        log.info("战役配置初始化完成，共{}个战役", campaignConfigs.size());
    }
    
    private List<Campaign.Stage> createLuanshiStages() {
        String[] generals = {"张角", "张宝", "张梁", "波才", "彭脱", "韩忠", "赵弘"};
        String[] types = {"步", "弓", "步", "骑", "步", "弓", "骑"};
        return createStagesWithFormula(generals, types, "luanshi", 1, 3, 200, 100, "优秀");
    }
    
    private List<Campaign.Stage> createSishiStages() {
        String[] generals = {"颜良", "文丑", "淳于琼", "高览", "张郃", "袁谭", "袁绍"};
        String[] types = {"骑", "骑", "弓", "步", "步", "弓", "骑"};
        return createStagesWithFormula(generals, types, "sishi", 20, 3, 500, 200, "精良");
    }
    
    private List<Campaign.Stage> createLvbuStages() {
        String[] generals = {"宋宪", "魏续", "侯成", "曹性", "高顺", "陈宫", "吕布"};
        String[] types = {"步", "步", "骑", "弓", "步", "弓", "骑"};
        return createStagesWithFormula(generals, types, "lvbu", 60, 3, 2000, 500, "史诗");
    }
    
    private List<Campaign.Stage> createGuanduStages() {
        String[] generals = {"蒋奇", "韩猛", "吕旷", "吕翔", "审配", "逢纪", "袁绍"};
        String[] types = {"弓", "骑", "步", "步", "弓", "步", "骑"};
        return createStagesWithFormula(generals, types, "guandu", 40, 3, 1000, 300, "精良");
    }
    
    private List<Campaign.Stage> createChibiStages() {
        String[] generals = {"蔡瑁", "张允", "于禁", "李典", "夏侯惇", "张辽", "曹操"};
        String[] types = {"弓", "弓", "步", "骑", "骑", "骑", "步"};
        return createStagesWithFormula(generals, types, "chibi", 80, 3, 5000, 1000, "传说");
    }

    /**
     * 统一关卡生成方法 - 使用新伤害体系的NPC属性公式
     *
     * NPC属性 = 模拟"穿戴本级副本装备的同等级武将"
     * 确保全身本级装备的玩家可以无伤横扫，低一档装备强化满可以勉强通过
     */
    private List<Campaign.Stage> createStagesWithFormula(
            String[] generals, String[] troopTypes, String prefix,
            int baseLv, int lvStep, int baseExp, long baseSilver, String quality) {
        List<Campaign.Stage> stages = new ArrayList<>();
        for (int i = 1; i <= 7; i++) {
            int npcLevel = baseLv + i * lvStep;
            // 关卡越后NPC品质越高: 1.0 ~ 1.9
            double npcQuality = 1.0 + (i - 1) * 0.15;
            String troopType = troopTypes[i - 1];

            // 用 GeneralService 同款公式算NPC六维
            int[] attrs = generalService.calcAttributes(npcQuality, troopType, npcLevel);
            int npcAtk = attrs[0];
            int npcDef = attrs[1];
            // NPC也有装备加成，模拟穿戴本级副本装备（约等于武将裸属性的40%）
            int equipBonus = (int)(npcAtk * 0.4);
            npcAtk += equipBonus;
            npcDef += (int)(npcDef * 0.4);

            // 士兵数 = 基于兵阶推算
            int npcTier = Math.min(9, 1 + npcLevel / 20);
            int npcSoldiers = BattleCalculator.getTierMaxSoldiers(npcTier);
            int npcSoldierHp = BattleCalculator.getTierSoldierHp(npcTier);

            stages.add(Campaign.Stage.builder()
                    .id(prefix + "_stage_" + i)
                    .stageNum(i)
                    .name("第" + i + "关")
                    .enemyGeneralName(generals[i - 1])
                    .enemyGeneralIcon("/images/general/" + generals[i - 1] + ".png")
                    .enemyLevel(npcLevel)
                    .enemyTroops(npcSoldiers)
                    .enemyAttack(npcAtk)
                    .enemyDefense(npcDef)
                    .enemyValor(attrs[2])
                    .enemyCommand(attrs[3])
                    .enemyDodge(attrs[4])
                    .enemyMobility(attrs[5])
                    .enemySoldierHp(npcSoldierHp)
                    .enemyTroopType(troopType)
                    .enemyTierMultiplier(BattleCalculator.getTierMultiplier(npcTier))
                    .expReward(baseExp + i * (baseExp / 2))
                    .silverReward(baseSilver + i * (baseSilver / 2))
                    .isBoss(i == 7)
                    .drops(createStageDrops(i, quality))
                    .build());
        }
        return stages;
    }
    
    private List<Campaign.StageDrop> createStageDrops(int stageNum, String quality) {
        List<Campaign.StageDrop> drops = new ArrayList<>();
        // 基础掉落 - 白银
        drops.add(Campaign.StageDrop.builder()
                .type("RESOURCE")
                .itemId("silver")
                .itemName("白银")
                .icon("/images/resource/silver.png")
                .dropRate(100)
                .minCount(50 * stageNum)
                .maxCount(100 * stageNum)
                .build());
        
        // 装备掉落
        if (stageNum >= 3) {
            drops.add(Campaign.StageDrop.builder()
                    .type("EQUIPMENT")
                    .itemId("equip_random")
                    .itemName("随机装备")
                    .icon("/images/equip/random.png")
                    .quality(quality)
                    .dropRate(20 + stageNum * 5)
                    .minCount(1)
                    .maxCount(1)
                    .build());
        }
        
        // BOSS关特殊掉落
        if (stageNum == 7) {
            drops.add(Campaign.StageDrop.builder()
                    .type("EQUIPMENT")
                    .itemId("equip_boss")
                    .itemName("BOSS装备")
                    .icon("/images/equip/boss.png")
                    .quality(getHigherQuality(quality))
                    .dropRate(50)
                    .minCount(1)
                    .maxCount(1)
                    .build());
        }
        
        return drops;
    }
    
    private String getHigherQuality(String quality) {
        switch (quality) {
            case "普通": return "优秀";
            case "优秀": return "精良";
            case "精良": return "史诗";
            case "史诗": return "传说";
            default: return "传说";
        }
    }
    
    /**
     * 获取所有战役列表
     */
    public List<Map<String, Object>> getCampaignList(String odUserId) {
        List<Map<String, Object>> result = new ArrayList<>();
        Map<String, CampaignProgress> progressMap = campaignRepository.findAllByUserId(odUserId);
        UserResource resource = userResourceService.getUserResource(odUserId);
        int userLevel = resource.getLevel() != null ? resource.getLevel() : 1;
        
        String today = new SimpleDateFormat("yyyyMMdd").format(new Date());
        
        campaignConfigs.values().stream()
                .sorted(Comparator.comparingInt(Campaign::getOrder))
                .forEach(campaign -> {
                    Map<String, Object> campaignInfo = new HashMap<>();
                    campaignInfo.put("id", campaign.getId());
                    campaignInfo.put("name", campaign.getName());
                    campaignInfo.put("description", campaign.getDescription());
                    campaignInfo.put("icon", campaign.getIcon());
                    campaignInfo.put("enemyLevelMin", campaign.getEnemyLevelMin());
                    campaignInfo.put("enemyLevelMax", campaign.getEnemyLevelMax());
                    campaignInfo.put("expRewardMin", campaign.getExpRewardMin());
                    campaignInfo.put("expRewardMax", campaign.getExpRewardMax());
                    campaignInfo.put("dailyLimit", campaign.getDailyLimit());
                    campaignInfo.put("staminaCost", campaign.getStaminaCost());
                    campaignInfo.put("requiredLevel", campaign.getRequiredLevel());
                    campaignInfo.put("dropPreviews", campaign.getDropPreviews());
                    campaignInfo.put("stageCount", campaign.getStages().size());
                    
                    // 是否解锁
                    boolean unlocked = userLevel >= campaign.getRequiredLevel();
                    campaignInfo.put("unlocked", unlocked);
                    
                    // 进度信息
                    CampaignProgress progress = progressMap.get(campaign.getId());
                    if (progress != null) {
                        // 检查是否需要重置今日次数
                        if (!today.equals(progress.getTodayDate())) {
                            progress.setTodayChallengeCount(0);
                            progress.setTodayDate(today);
                            campaignRepository.save(progress);
                        }
                        campaignInfo.put("currentStage", progress.getCurrentStage());
                        campaignInfo.put("maxClearedStage", progress.getMaxClearedStage());
                        campaignInfo.put("todayCount", progress.getTodayChallengeCount());
                        campaignInfo.put("status", progress.getStatus());
                        campaignInfo.put("fullCleared", progress.getFullCleared());
                        campaignInfo.put("canSweep", progress.getFullCleared());
                    } else {
                        campaignInfo.put("currentStage", 0);
                        campaignInfo.put("maxClearedStage", 0);
                        campaignInfo.put("todayCount", 0);
                        campaignInfo.put("status", "IDLE");
                        campaignInfo.put("fullCleared", false);
                        campaignInfo.put("canSweep", false);
                    }
                    
                    result.add(campaignInfo);
                });
        
        return result;
    }
    
    /**
     * 获取战役详情
     */
    public Map<String, Object> getCampaignDetail(String odUserId, String campaignId) {
        Campaign campaign = campaignConfigs.get(campaignId);
        if (campaign == null) {
            throw new BusinessException("战役不存在");
        }
        
        UserResource resource = userResourceService.getUserResource(odUserId);
        int userLevel = resource.getLevel() != null ? resource.getLevel() : 1;
        
        if (userLevel < campaign.getRequiredLevel()) {
            throw new BusinessException("君主等级不足，需要达到" + campaign.getRequiredLevel() + "级");
        }
        
        CampaignProgress progress = getOrCreateProgress(odUserId, campaignId);
        
        Map<String, Object> result = new HashMap<>();
        result.put("campaign", campaign);
        result.put("progress", progress);
        result.put("resource", resource);
        
        return result;
    }
    
    /**
     * 开始战役
     */
    public Map<String, Object> startCampaign(String odUserId, String campaignId, String generalId) {
        Campaign campaign = campaignConfigs.get(campaignId);
        if (campaign == null) {
            throw new BusinessException("战役不存在");
        }
        
        UserResource resource = userResourceService.getUserResource(odUserId);
        int userLevel = resource.getLevel() != null ? resource.getLevel() : 1;
        
        if (userLevel < campaign.getRequiredLevel()) {
            throw new BusinessException("君主等级不足");
        }
        
        // 检查精力
        if (resource.getStamina() < campaign.getStaminaCost()) {
            throw new BusinessException("精力不足");
        }
        
        CampaignProgress progress = getOrCreateProgress(odUserId, campaignId);
        
        // 检查今日次数
        String today = new SimpleDateFormat("yyyyMMdd").format(new Date());
        if (!today.equals(progress.getTodayDate())) {
            progress.setTodayChallengeCount(0);
            progress.setTodayDate(today);
        }
        if (progress.getTodayChallengeCount() >= campaign.getDailyLimit()) {
            throw new BusinessException("今日挑战次数已用完");
        }
        
        // 获取武将信息设置兵力
        General general = generalService.getGeneralById(generalId);
        if (general == null) {
            throw new BusinessException("武将不存在");
        }
        
        // 获取兵力
        int troops = general.getSoldierCount() != null ? general.getSoldierCount() : 1000;
        
        // 扣除精力
        resource.setStamina(resource.getStamina() - campaign.getStaminaCost());
        userResourceService.saveUserResource(resource);
        
        // 更新进度
        progress.setStatus("IN_PROGRESS");
        progress.setCurrentStage(1);
        progress.setCurrentTroops(troops);
        progress.setMaxTroops(troops);
        progress.setReviveCount(3);
        progress.setGeneralId(generalId);
        progress.setTodayChallengeCount(progress.getTodayChallengeCount() + 1);
        progress.setStartTime(System.currentTimeMillis());
        progress.setTotalExpGained(0L);
        progress.setTotalSilverGained(0L);
        campaignRepository.save(progress);
        
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("progress", progress);
        result.put("campaign", campaign);
        result.put("currentStage", campaign.getStages().get(0));
        
        return result;
    }
    
    /**
     * 进攻当前关卡 - 使用新伤害计算体系
     */
    public CampaignProgress.BattleResult attack(String odUserId, String campaignId) {
        Campaign campaign = campaignConfigs.get(campaignId);
        if (campaign == null) {
            throw new BusinessException("战役不存在");
        }
        
        CampaignProgress progress = campaignRepository.findByUserIdAndCampaignId(odUserId, campaignId);
        if (progress == null || !"IN_PROGRESS".equals(progress.getStatus())) {
            throw new BusinessException("战役未开始");
        }
        
        int stageIndex = progress.getCurrentStage() - 1;
        if (stageIndex < 0 || stageIndex >= campaign.getStages().size()) {
            throw new BusinessException("关卡不存在");
        }
        
        Campaign.Stage stage = campaign.getStages().get(stageIndex);
        
        // 获取武将
        General general = generalService.getGeneralById(progress.getGeneralId());
        if (general == null) {
            throw new BusinessException("武将不存在");
        }
        
        // 构建玩家战斗单元
        BattleCalculator.BattleUnit player = new BattleCalculator.BattleUnit();
        player.name = general.getName();
        player.level = general.getLevel() != null ? general.getLevel() : 1;
        player.attack = (general.getAttrAttack() != null ? general.getAttrAttack() : 100);
        player.defense = (general.getAttrDefense() != null ? general.getAttrDefense() : 50);
        player.valor = general.getAttrValor() != null ? general.getAttrValor() : 10;
        player.command = general.getAttrCommand() != null ? general.getAttrCommand() : 10;
        player.dodge = general.getAttrDodge() != null ? (int) Math.round(general.getAttrDodge()) : 5;
        player.mobility = general.getAttrMobility() != null ? general.getAttrMobility() : 15;
        player.troopType = BattleCalculator.parseTroopType(general.getTroopType());
        player.soldierCount = progress.getCurrentTroops();
        // 兵阶
        int playerTier = general.getSoldierTier() != null ? general.getSoldierTier() : 1;
        player.soldierHp = BattleCalculator.getTierSoldierHp(playerTier);
        player.tierMultiplier = BattleCalculator.getTierMultiplier(playerTier);

        // 兵种图标
        String troopCat = general.getTroopType() != null ? general.getTroopType() : "步";
        Map<String, String> playerIcons = peerageService.getSoldierIcons(troopCat, playerTier);
        player.soldierIconIdle = playerIcons.get("iconIdle");
        player.soldierIconAttack = playerIcons.get("iconAttack");

        // 兵法属性填充
        if (general.getTacticsId() != null) {
            TacticsTemplate tt = tacticsConfig.getById(general.getTacticsId());
            if (tt != null) {
                Map<String, Object> owned = userTacticsMapper.findByUserIdAndTacticsId(
                        general.getUserId(), general.getTacticsId());
                int tLevel = owned != null ? ((Number) owned.get("level")).intValue() : 1;
                player.tacticsId = tt.getId();
                player.tacticsName = tt.getName();
                player.tacticsLevel = tLevel;
                player.tacticsEffectValue = TacticsConfig.calcEffect(tt, tLevel);
                player.tacticsTriggerRate = TacticsConfig.calcTriggerRate(tt, tLevel);
            }
        }
        
        // 构建NPC战斗单元
        BattleCalculator.BattleUnit enemy = new BattleCalculator.BattleUnit();
        enemy.name = stage.getEnemyGeneralName();
        enemy.level = stage.getEnemyLevel();
        enemy.attack = stage.getEnemyAttack();
        enemy.defense = stage.getEnemyDefense();
        enemy.valor = stage.getEnemyValor() != null ? stage.getEnemyValor() : 10;
        enemy.command = stage.getEnemyCommand() != null ? stage.getEnemyCommand() : 10;
        enemy.dodge = stage.getEnemyDodge() != null ? stage.getEnemyDodge() : 5;
        enemy.mobility = stage.getEnemyMobility() != null ? stage.getEnemyMobility() : 15;
        enemy.troopType = BattleCalculator.parseTroopType(
                stage.getEnemyTroopType() != null ? stage.getEnemyTroopType() : "步");
        enemy.soldierCount = stage.getEnemyTroops();
        enemy.soldierHp = stage.getEnemySoldierHp() != null ? stage.getEnemySoldierHp() : 100;
        enemy.tierMultiplier = stage.getEnemyTierMultiplier() != null ? stage.getEnemyTierMultiplier() : 1.0;
        
        // 战斗模拟
        List<String> battleLog = new ArrayList<>();
        battleLog.add(String.format("【战斗开始】%s(Lv%d) vs %s(Lv%d)",
                player.name, player.level, enemy.name, enemy.level));
        battleLog.add(String.format("我方 攻:%d 防:%d 兵:%d 兵阶:%d",
                player.attack, player.defense, player.soldierCount, playerTier));
        battleLog.add(String.format("敌方 攻:%d 防:%d 兵:%d",
                enemy.attack, enemy.defense, enemy.soldierCount));
        
        // 先手判定：机动高的先攻
        boolean playerFirst = player.mobility >= enemy.mobility;
        
        int round = 0;
        while (player.soldierCount >= 0 && enemy.soldierCount >= 0 && round < 20) {
            round++;
            
            BattleCalculator.BattleUnit first = playerFirst ? player : enemy;
            BattleCalculator.BattleUnit second = playerFirst ? enemy : player;
            String firstName = playerFirst ? "我方" : "敌方";
            String secondName = playerFirst ? "敌方" : "我方";
            
            // 先手攻击（兵法版）
            List<BattleCalculator.BattleUnit> secondList = Collections.singletonList(second);
            BattleCalculator.TacticsResult tr1 = BattleCalculator.calcDamageWithTactics(first, second, secondList);
            if (tr1.triggered && tr1.tacticsName != null) {
                battleLog.add(String.format("第%d回合: %s发动【%s】！%s",
                        round, firstName, tr1.tacticsName, tr1.effectDesc != null ? tr1.effectDesc : ""));
            }
            for (BattleCalculator.DamageResult r1 : tr1.damages) {
                if (r1.isDodge) {
                    battleLog.add(String.format("第%d回合: %s攻击，%s闪避！", round, firstName, secondName));
                } else {
                    second.soldierCount = Math.max(0, second.soldierCount - r1.soldierLoss);
                    String critTag = r1.isCrit ? "【暴击】" : "";
                    battleLog.add(String.format("第%d回合: %s攻击%s造成%d伤害%s，减员%d，剩余兵力%d",
                            round, firstName, critTag, r1.damage, critTag.isEmpty() ? "" : "！",
                            r1.soldierLoss, second.soldierCount));
                }
            }
            
            if (second.soldierCount <= 0) break;
            
            // 后手攻击（兵法版）
            List<BattleCalculator.BattleUnit> firstList = Collections.singletonList(first);
            BattleCalculator.TacticsResult tr2 = BattleCalculator.calcDamageWithTactics(second, first, firstList);
            if (tr2.triggered && tr2.tacticsName != null) {
                battleLog.add(String.format("第%d回合: %s发动【%s】！%s",
                        round, secondName, tr2.tacticsName, tr2.effectDesc != null ? tr2.effectDesc : ""));
            }
            for (BattleCalculator.DamageResult r2 : tr2.damages) {
                if (r2.isDodge) {
                    battleLog.add(String.format("第%d回合: %s反击，%s闪避！", round, secondName, firstName));
                } else {
                    first.soldierCount = Math.max(0, first.soldierCount - r2.soldierLoss);
                    String critTag = r2.isCrit ? "【暴击】" : "";
                    battleLog.add(String.format("第%d回合: %s反击%s造成%d伤害%s，减员%d，剩余兵力%d",
                            round, secondName, critTag, r2.damage, critTag.isEmpty() ? "" : "！",
                            r2.soldierLoss, first.soldierCount));
                }
            }
            
            if (first.soldierCount <= 0) break;
        }
        
        int playerRemaining = playerFirst ? player.soldierCount : player.soldierCount;
        boolean victory = player.soldierCount > 0 && enemy.soldierCount <= 0;
        // 20回合未分胜负算失败
        if (round >= 20 && enemy.soldierCount > 0) victory = false;
        
        int troopsLost = progress.getCurrentTroops() - player.soldierCount;
        
        CampaignProgress.BattleResult result = CampaignProgress.BattleResult.builder()
                .victory(victory)
                .stageNum(progress.getCurrentStage())
                .remainingTroops(player.soldierCount)
                .troopsLost(troopsLost)
                .battleLog(battleLog)
                .isLastStage(progress.getCurrentStage() >= campaign.getStages().size())
                .playerSoldierIconIdle(player.soldierIconIdle)
                .playerSoldierIconAttack(player.soldierIconAttack)
                .enemySoldierIconIdle(enemy.soldierIconIdle)
                .enemySoldierIconAttack(enemy.soldierIconAttack)
                .playerTroopType(troopCat)
                .enemyTroopType(stage.getEnemyTroopType() != null ? stage.getEnemyTroopType() : "步")
                .build();
        
        if (victory) {
            battleLog.add("【战斗胜利】");
            
            // 发放奖励
            long expGained = stage.getExpReward();
            long silverGained = stage.getSilverReward();
            
            result.setExpGained(expGained);
            result.setSilverGained(silverGained);
            
            // 处理掉落
            Random dropRandom = new Random();
            List<CampaignProgress.DropItem> drops = processDrops(stage.getDrops(), dropRandom);
            result.setDrops(drops);
            
            // 更新武将经验
            long currentExp = general.getExp() != null ? general.getExp() : 0;
            general.setExp(currentExp + expGained);
            generalService.saveGeneral(general);
            
            // 更新资源
            UserResource resource = userResourceService.getUserResource(odUserId);
            resource.setSilver(resource.getSilver() + silverGained);
            userResourceService.saveUserResource(resource);
            
            // 处理装备掉落
            for (CampaignProgress.DropItem drop : drops) {
                if ("EQUIPMENT".equals(drop.getType())) {
                    Equipment equipment = generateRandomEquipment(odUserId, drop.getQuality(), stage.getEnemyLevel());
                    equipmentRepository.save(equipment);
                }
            }
            
            // 检查是否首次通关
            boolean isFirstClear = progress.getMaxClearedStage() < progress.getCurrentStage();
            result.setIsFirstClear(isFirstClear);
            
            // 更新进度
            progress.setCurrentTroops(player.soldierCount);
            progress.setTotalExpGained(progress.getTotalExpGained() + expGained);
            progress.setTotalSilverGained(progress.getTotalSilverGained() + silverGained);
            
            if (isFirstClear) {
                progress.setMaxClearedStage(progress.getCurrentStage());
            }
            
            // 检查是否最后一关
            if (progress.getCurrentStage() >= campaign.getStages().size()) {
                progress.setStatus("COMPLETED");
                progress.setFullCleared(true);
                battleLog.add("【恭喜】战役通关！已解锁扫荡功能");
            } else {
                progress.setCurrentStage(progress.getCurrentStage() + 1);
            }
            
        } else {
            battleLog.add("【战斗失败】");
            progress.setCurrentTroops(0);
            
            // 检查重生次数
            if (progress.getReviveCount() > 0) {
                battleLog.add(String.format("剩余重生次数: %d", progress.getReviveCount()));
            } else {
                progress.setStatus("IDLE");
                battleLog.add("重生次数已用完，战役结束");
            }
        }
        
        campaignRepository.save(progress);
        return result;
    }
    
    /**
     * 补充兵力
     */
    public Map<String, Object> replenishTroops(String odUserId, String campaignId) {
        CampaignProgress progress = campaignRepository.findByUserIdAndCampaignId(odUserId, campaignId);
        if (progress == null || !"IN_PROGRESS".equals(progress.getStatus())) {
            throw new BusinessException("战役未开始");
        }
        
        UserResource resource = userResourceService.getUserResource(odUserId);
        
        int troopsNeeded = progress.getMaxTroops() - progress.getCurrentTroops();
        if (troopsNeeded <= 0) {
            throw new BusinessException("兵力已满");
        }
        
        // 计算消耗白银 (每100兵力消耗10白银)
        long silverCost = (troopsNeeded / 100 + 1) * 10;
        if (resource.getSilver() < silverCost) {
            throw new BusinessException("白银不足");
        }
        
        // 扣除白银
        resource.setSilver(resource.getSilver() - silverCost);
        userResourceService.saveUserResource(resource);
        
        // 恢复兵力
        progress.setCurrentTroops(progress.getMaxTroops());
        campaignRepository.save(progress);
        
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("currentTroops", progress.getCurrentTroops());
        result.put("maxTroops", progress.getMaxTroops());
        result.put("silverCost", silverCost);
        
        return result;
    }
    
    /**
     * 使用重生
     */
    public Map<String, Object> revive(String odUserId, String campaignId) {
        CampaignProgress progress = campaignRepository.findByUserIdAndCampaignId(odUserId, campaignId);
        if (progress == null || !"IN_PROGRESS".equals(progress.getStatus())) {
            throw new BusinessException("战役未开始");
        }
        
        if (progress.getReviveCount() <= 0) {
            throw new BusinessException("重生次数已用完");
        }
        
        if (progress.getCurrentTroops() > 0) {
            throw new BusinessException("兵力未归零，无需重生");
        }
        
        // 使用重生
        progress.setReviveCount(progress.getReviveCount() - 1);
        progress.setCurrentTroops(progress.getMaxTroops());
        campaignRepository.save(progress);
        
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("reviveCount", progress.getReviveCount());
        result.put("currentTroops", progress.getCurrentTroops());
        
        return result;
    }
    
    /**
     * 暂停战役
     */
    public Map<String, Object> pauseCampaign(String odUserId, String campaignId) {
        CampaignProgress progress = campaignRepository.findByUserIdAndCampaignId(odUserId, campaignId);
        if (progress == null || !"IN_PROGRESS".equals(progress.getStatus())) {
            throw new BusinessException("战役未开始");
        }
        
        progress.setStatus("PAUSED");
        campaignRepository.save(progress);
        
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "战役已暂停，进度已保存");
        
        return result;
    }
    
    /**
     * 继续战役
     */
    public Map<String, Object> resumeCampaign(String odUserId, String campaignId) {
        Campaign campaign = campaignConfigs.get(campaignId);
        if (campaign == null) {
            throw new BusinessException("战役不存在");
        }
        
        CampaignProgress progress = campaignRepository.findByUserIdAndCampaignId(odUserId, campaignId);
        if (progress == null || !"PAUSED".equals(progress.getStatus())) {
            throw new BusinessException("没有暂停的战役");
        }
        
        progress.setStatus("IN_PROGRESS");
        campaignRepository.save(progress);
        
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("progress", progress);
        result.put("campaign", campaign);
        result.put("currentStage", campaign.getStages().get(progress.getCurrentStage() - 1));
        
        return result;
    }
    
    /**
     * 结束战役
     */
    public Map<String, Object> endCampaign(String odUserId, String campaignId) {
        CampaignProgress progress = campaignRepository.findByUserIdAndCampaignId(odUserId, campaignId);
        if (progress == null) {
            throw new BusinessException("战役进度不存在");
        }
        
        progress.setStatus("IDLE");
        progress.setCurrentTroops(0);
        campaignRepository.save(progress);
        
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "战役已结束");
        result.put("totalExpGained", progress.getTotalExpGained());
        result.put("totalSilverGained", progress.getTotalSilverGained());
        
        return result;
    }
    
    /**
     * 扫荡
     */
    public CampaignProgress.SweepResult sweep(String odUserId, String campaignId, int targetStage) {
        Campaign campaign = campaignConfigs.get(campaignId);
        if (campaign == null) {
            throw new BusinessException("战役不存在");
        }
        
        CampaignProgress progress = campaignRepository.findByUserIdAndCampaignId(odUserId, campaignId);
        if (progress == null || !progress.getFullCleared()) {
            throw new BusinessException("需要先通关战役才能扫荡");
        }
        
        UserResource resource = userResourceService.getUserResource(odUserId);
        
        // 检查虎符
        int tigerTally = resource.getTigerTally() != null ? resource.getTigerTally() : 0;
        if (tigerTally <= 0) {
            throw new BusinessException("虎符不足");
        }
        
        // 检查今日次数
        String today = new SimpleDateFormat("yyyyMMdd").format(new Date());
        if (!today.equals(progress.getTodayDate())) {
            progress.setTodayChallengeCount(0);
            progress.setTodayDate(today);
        }
        if (progress.getTodayChallengeCount() >= campaign.getDailyLimit()) {
            throw new BusinessException("今日挑战次数已用完");
        }
        
        // 限制扫荡关数
        int maxStage = Math.min(targetStage, campaign.getStages().size());
        
        // 执行扫荡
        Random random = new Random();
        long totalExp = 0;
        long totalSilver = 0;
        List<CampaignProgress.DropItem> allDrops = new ArrayList<>();
        int tigerTallyUsed = 0;
        int stagesSwept = 0;
        
        for (int i = 1; i <= maxStage && tigerTally > tigerTallyUsed; i++) {
            Campaign.Stage stage = campaign.getStages().get(i - 1);
            
            // 消耗虎符
            tigerTallyUsed++;
            stagesSwept++;
            
            // 累计奖励
            totalExp += stage.getExpReward();
            totalSilver += stage.getSilverReward();
            
            // 处理掉落
            List<CampaignProgress.DropItem> drops = processDrops(stage.getDrops(), random);
            allDrops.addAll(drops);
        }
        
        // 扣除虎符
        resource.setTigerTally(tigerTally - tigerTallyUsed);
        resource.setSilver(resource.getSilver() + totalSilver);
        userResourceService.saveUserResource(resource);
        
        // 武将获得经验
        General general = generalService.getGeneralById(progress.getGeneralId());
        if (general != null) {
            long currentExp = general.getExp() != null ? general.getExp() : 0;
            general.setExp(currentExp + totalExp);
            generalService.saveGeneral(general);
        }
        
        // 处理装备掉落
        for (CampaignProgress.DropItem drop : allDrops) {
            if ("EQUIPMENT".equals(drop.getType())) {
                Equipment equipment = generateRandomEquipment(odUserId, drop.getQuality(), campaign.getEnemyLevelMax());
                equipmentRepository.save(equipment);
            }
        }
        
        // 更新进度
        progress.setTodayChallengeCount(progress.getTodayChallengeCount() + 1);
        campaignRepository.save(progress);
        
        return CampaignProgress.SweepResult.builder()
                .stagesSwept(stagesSwept)
                .totalExp(totalExp)
                .totalSilver(totalSilver)
                .items(allDrops)
                .tigerTallyUsed(tigerTallyUsed)
                .stoppedByFailure(false)
                .build();
    }
    
    private CampaignProgress getOrCreateProgress(String odUserId, String campaignId) {
        CampaignProgress progress = campaignRepository.findByUserIdAndCampaignId(odUserId, campaignId);
        if (progress == null) {
            progress = CampaignProgress.builder()
                    .id(odUserId + "_" + campaignId)
                    .userId(odUserId)
                    .campaignId(campaignId)
                    .currentStage(0)
                    .maxClearedStage(0)
                    .todayChallengeCount(0)
                    .todayDate(new SimpleDateFormat("yyyyMMdd").format(new Date()))
                    .status("IDLE")
                    .fullCleared(false)
                    .build();
            campaignRepository.save(progress);
        }
        return progress;
    }
    
    private List<CampaignProgress.DropItem> processDrops(List<Campaign.StageDrop> stageDrops, Random random) {
        List<CampaignProgress.DropItem> drops = new ArrayList<>();
        if (stageDrops == null) return drops;
        
        for (Campaign.StageDrop stageDrop : stageDrops) {
            if (random.nextInt(100) < stageDrop.getDropRate()) {
                int count = stageDrop.getMinCount() + random.nextInt(stageDrop.getMaxCount() - stageDrop.getMinCount() + 1);
                drops.add(CampaignProgress.DropItem.builder()
                        .type(stageDrop.getType())
                        .itemId(stageDrop.getItemId())
                        .itemName(stageDrop.getItemName())
                        .icon(stageDrop.getIcon())
                        .quality(stageDrop.getQuality())
                        .count(count)
                        .build());
            }
        }
        return drops;
    }
    
    private Equipment generateRandomEquipment(String odUserId, String quality, int level) {
        Random random = new Random();
        String[] slotNames = {"武器", "头盔", "铠甲", "戒指", "鞋子", "项链"};
        int slotId = random.nextInt(6) + 1;
        
        int qualityId = getQualityId(quality);
        int multiplier = qualityId;
        
        Equipment.Attributes attrs = Equipment.Attributes.builder()
                .attack(10 * multiplier + random.nextInt(5 * multiplier))
                .defense(8 * multiplier + random.nextInt(4 * multiplier))
                .hp(50 * multiplier + random.nextInt(20 * multiplier))
                .build();
        
        return Equipment.builder()
                .id("equip_" + System.currentTimeMillis() + "_" + random.nextInt(1000))
                .userId(odUserId)
                .name(quality + slotNames[slotId - 1])
                .slotType(Equipment.SlotType.builder()
                        .id(slotId)
                        .name(slotNames[slotId - 1])
                        .build())
                .quality(Equipment.Quality.builder()
                        .id(qualityId)
                        .name(quality)
                        .multiplier((double) multiplier)
                        .build())
                .level(level)
                .baseAttributes(attrs)
                .source(Equipment.Source.builder()
                        .type("CAMPAIGN")
                        .name("战役掉落")
                        .build())
                .createTime(System.currentTimeMillis())
                .build();
    }
    
    private int getQualityId(String quality) {
        if (quality == null) return 1;
        switch (quality) {
            case "传说": return 6;
            case "史诗": return 5;
            case "精良": return 4;
            case "优秀": return 3;
            case "普通": return 2;
            default: return 1;
        }
    }
}
