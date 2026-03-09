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
    
    // ======================== 阵营NPC名池 (步/骑/弓 各5个名称) ========================
    private static final String[][][] FACTION_NPC_POOL = {
        /* 0 黄巾 */ {{"黄巾步兵","黄巾枪兵","黄巾盾兵","黄巾力士","黄巾精锐"},{"黄巾骑兵","黄巾马军","黄巾铁骑","黄巾精骑","黄巾突骑"},{"黄巾弓手","黄巾射手","黄巾火弓","黄巾神射","黄巾长弓"}},
        /* 1 西凉 */ {{"西凉步兵","西凉盾兵","西凉重甲","飞熊军","飞熊精锐"},{"西凉骑兵","西凉铁骑","西凉精骑","西凉突骑","飞熊骑兵"},{"西凉弓手","西凉弓将","西凉神射","飞熊弓手","西凉长弓"}},
        /* 2 袁军 */ {{"袁军步兵","袁军枪兵","袁军盾兵","袁军重甲","袁军精锐"},{"袁军骑兵","袁军铁骑","袁军精骑","袁军突骑","大戟士"},{"袁军弓手","袁军弓将","袁军神射","袁军长弓","袁军精弓"}},
        /* 3 曹军 */ {{"曹军步兵","曹军枪兵","曹军盾兵","曹军重甲","曹军精锐"},{"曹军骑兵","曹军铁骑","虎豹骑","虎豹精骑","曹军突骑"},{"曹军弓手","曹军弓将","曹军神射","曹军精弓","曹军长弓"}},
        /* 4 吕布 */ {{"陷阵步兵","陷阵枪兵","陷阵精锐","陷阵重甲","飞将步卒"},{"飞将骑兵","赤兔铁骑","飞将精骑","吕布亲卫","飞将突骑"},{"飞将弓手","陷阵弓手","飞将神射","飞将精弓","飞将长弓"}},
        /* 5 吴军 */ {{"吴军步兵","吴军枪兵","吴军盾兵","吴军重甲","吴军精锐"},{"吴军骑兵","吴军铁骑","吴军精骑","锦帆骑","吴军突骑"},{"吴军弓手","吴军弓将","吴军神射","丹阳弓手","吴军长弓"}},
        /* 6 魏军 */ {{"魏军步兵","魏军枪兵","魏军盾兵","魏军重甲","魏军精锐"},{"魏军骑兵","魏军铁骑","魏军精骑","虎卫骑","魏军突骑"},{"魏军弓手","魏军弓将","魏军神射","魏军精弓","魏军长弓"}}
    };
    private static final String[] FACTION_KEYS = {"黄巾","西凉","袁军","曹军","吕布","吴军","魏军"};

    private static int factionIdx(String f) {
        for (int i = 0; i < FACTION_KEYS.length; i++) if (FACTION_KEYS[i].equals(f)) return i;
        return 3;
    }

    private static int[] getEquipPreIds(int maxLv) {
        if (maxLv <= 10) return new int[]{1,2,3,4,5,6};
        if (maxLv <= 20) return new int[]{7,8,9,10,11,12};
        if (maxLv <= 40) return new int[]{19,20,21,22,23,24};
        if (maxLv <= 50) return new int[]{31,32,33,34,35,36};
        if (maxLv <= 60) return new int[]{37,38,39,40,41,42, 55,56,57,58,59,60};
        if (maxLv <= 80) return new int[]{55,56,57,58,59,60, 79,80,81,82,83,84};
        if (maxLv <= 100) return new int[]{79,80,81,82,83,84, 97,98,99,100,101,102};
        return new int[0]; // Lv100+ 预留，暂无装备掉落
    }

    private static int[] getItemDropPool(int maxLv) {
        if (maxLv <= 20) return new int[]{1,14,17,20,28,32};
        if (maxLv <= 50) return new int[]{36,37,15,18,21,7,29,33};
        if (maxLv <= 100) return new int[]{2,38,16,19,22,8,29,30,34};
        return new int[0]; // Lv100+ 预留，暂无道具掉落
    }

    /**
     * 初始化战役配置 - 12个战役覆盖 Lv1~200
     *
     *  1. 黄巾之乱     Lv1-10    5关   解锁Lv1
     *  2. 诸侯讨董     Lv10-20   7关   解锁Lv10
     *  3. 乱世华雄     Lv20-40   10关  解锁Lv20  (起始每关6NPC)
     *  4. 官渡之战     Lv40-50   15关  解锁Lv40
     *  5. 赤壁之战     Lv50-60   20关  解锁Lv50  (起始20关)
     *  6. 定军山之战   Lv60-80   20关  解锁Lv60
     *  7. 战神吕布     Lv80-100  20关  解锁Lv80
     *  8. 夷陵烽火     Lv100-120 20关  解锁Lv100 (预留)
     *  9. 五丈原       Lv120-140 20关  解锁Lv120 (预留)
     * 10. 姜维北伐     Lv140-160 20关  解锁Lv140 (预留)
     * 11. 钟会伐蜀     Lv160-180 20关  解锁Lv160 (预留)
     * 12. 一统天下     Lv180-200 20关  解锁Lv180 (预留)
     */
    private void initCampaignConfigs() {

        // ===== 战役1: 黄巾之乱 (5关, NPC递增1→6) =====
        reg("campaign_huangjin","黄巾之乱","苍天已死黄天当立！讨伐张角三兄弟",1,10,1,5,4,1, 5,false,"黄巾",
            new String[]{"黄巾贼兵","黄巾弓手","波才","张梁","张角"},
            new String[]{"步","弓","步","步","弓"},
            Arrays.asList(dp("新手长剑","普通"),dp("新手布甲","普通"),dp("新手布帽","优秀")));

        // ===== 战役2: 诸侯讨董 (7关, NPC递增1→6) =====
        reg("campaign_dongzhuo","诸侯讨董","十八路诸侯会盟讨董，西凉铁骑势不可挡",10,20,10,4,5,2, 7,false,"西凉",
            new String[]{"胡轸","李傕","郭汜","张济","徐荣","李儒","董卓"},
            new String[]{"骑","骑","步","步","步","弓","步"},
            Arrays.asList(dp("宣武长剑","优秀"),dp("宣武战甲","优秀"),dp("宣武头盔","精良")));

        // ===== 战役3: 乱世华雄 (10关, 全6NPC) =====
        reg("campaign_huaxiong","乱世华雄","华雄威震汜水关，斩杀联军数将",20,40,20,3,6,3, 10,true,"西凉",
            new String[]{"赵岑","李肃","胡轸","牛辅","张济","樊稠","吕布亲卫","高顺","张辽","华雄"},
            new String[]{"步","弓","骑","步","步","骑","骑","步","骑","步"},
            Arrays.asList(dp("陷阵长枪","精良"),dp("陷阵重甲","精良"),dp("陷阵头盔","精良")));

        // ===== 战役4: 官渡之战 (15关, 全6NPC) =====
        reg("campaign_guandu","官渡之战","袁绍携河北四州之众南下，曹操以少胜多",40,50,40,3,8,4, 15,true,"袁军",
            new String[]{"蒋奇","韩猛","吕旷","吕翔","高览","淳于琼","蒋义渠","张郃","高干","逢纪","审配","颜良","文丑","袁谭","袁绍"},
            new String[]{"弓","骑","步","步","步","弓","骑","步","骑","弓","弓","骑","骑","弓","骑"},
            Arrays.asList(dp("狂战巨斧","史诗"),dp("狂战重甲","史诗"),dp("狂战头盔","史诗")));

        // ===== 战役5: 赤壁之战 (20关, 全6NPC) =====
        reg("campaign_chibi","赤壁之战","曹操率八十万大军南下，孙刘联军火烧赤壁",50,60,50,3,8,5, 20,true,"曹军",
            new String[]{"蔡瑁","张允","蒋干","于禁","李典","乐进","曹洪","曹仁","夏侯惇","夏侯渊","曹休","曹真","张辽","徐晃","张郃","许褚","典韦","荀攸","程昱","曹操"},
            new String[]{"弓","弓","弓","步","骑","步","步","步","骑","弓","骑","骑","骑","步","步","步","步","弓","弓","步"},
            Arrays.asList(dp("天狼战刃","史诗"),dp("天狼战甲","史诗"),dp("熊王巨锤","传说")));

        // ===== 战役6: 定军山之战 (20关, 全6NPC) =====
        reg("campaign_dingjun","定军山之战","刘备取汉中，黄忠于定军山阵斩夏侯渊",60,80,60,2,10,6, 20,true,"曹军",
            new String[]{"曹军先锋","曹军校尉","曹洪","夏侯尚","于禁","张郃","曹仁","徐晃","曹真","曹休","张辽","许褚","典韦","夏侯惇","司马师","司马懿","张郃","夏侯渊","曹操","夏侯渊"},
            new String[]{"步","骑","步","骑","步","步","步","步","骑","弓","骑","步","步","骑","弓","弓","步","弓","步","弓"},
            Arrays.asList(dp("熊王巨锤","传说"),dp("雄狮战刃","传说"),dp("雄狮战甲","传说")));

        // ===== 战役7: 战神吕布 (20关, 全6NPC) =====
        reg("campaign_lvbu","战神吕布","人中吕布马中赤兔，虎牢关前无人能敌",80,100,80,2,12,7, 20,true,"吕布",
            new String[]{"宋宪","魏续","侯成","曹性","成廉","薛兰","臧霸","郝萌","秦宜禄","张超","高顺","陈宫","张辽","陷阵统领","陷阵精锐","赤兔铁骑","高顺","陈宫","张辽","吕布"},
            new String[]{"步","步","骑","弓","骑","弓","步","骑","步","步","步","弓","骑","步","步","骑","步","弓","骑","骑"},
            Arrays.asList(dp("雄狮战刃","传说"),dp("圣象神兵","传说"),dp("方天画戟","传说")));

        // ===== 战役8: 夷陵烽火 (20关, 预留) =====
        reg("campaign_yiling","夷陵烽火","刘备为关羽报仇东征孙吴，陆逊火烧连营",100,120,100,2,14,8, 20,true,"吴军",
            new String[]{"吴军斥候","吴军校尉","潘璋","马忠","朱然","步骘","凌统","吕蒙","甘宁","丁奉","韩当","周泰","太史慈","黄盖","程普","鲁肃","吕蒙","周瑜","孙权","陆逊"},
            new String[]{"步","骑","步","弓","步","弓","骑","步","骑","步","步","步","弓","弓","步","弓","步","弓","骑","弓"},
            Arrays.asList(dp("玄武战刃","传说"),dp("玄武战甲","传说")));

        // ===== 战役9: 五丈原 (20关, 预留) =====
        reg("campaign_wuzhang","五丈原","诸葛亮六出祁山，秋风五丈原",120,140,120,2,16,9, 20,true,"魏军",
            new String[]{"魏军先锋","魏军校尉","郭淮","孙礼","王朗","曹真","曹爽","张郃","郝昭","牛金","费耀","戴陵","司马师","司马昭","郭淮","张郃","曹真","曹爽","司马师","司马懿"},
            new String[]{"步","骑","弓","步","弓","骑","步","步","步","骑","弓","步","骑","弓","弓","步","骑","步","骑","弓"},
            Arrays.asList(dp("玄武战刃","传说"),dp("秘银神剑","传说")));

        // ===== 战役10: 姜维北伐 (20关, 预留) =====
        reg("campaign_jiangwei","姜维北伐","继承丞相遗志，九伐中原",140,160,140,2,18,10, 20,true,"魏军",
            new String[]{"魏军先锋","魏军校尉","陈泰","王经","郭淮","徐质","司马望","钟会","邓忠","师纂","田续","胡烈","杜预","诸葛绪","邓艾","钟会","司马昭","邓艾","钟会","邓艾"},
            new String[]{"步","骑","步","弓","弓","步","弓","骑","骑","骑","弓","步","步","弓","步","骑","步","步","骑","步"},
            Arrays.asList(dp("秘银神剑","传说"),dp("秘银战甲","传说")));

        // ===== 战役11: 钟会伐蜀 (20关, 预留) =====
        reg("campaign_zhonghui","钟会伐蜀","魏国大举伐蜀，蜀汉风雨飘摇",160,180,160,2,20,11, 20,true,"魏军",
            new String[]{"魏军先锋","魏军校尉","胡烈","田续","师纂","邓忠","庞会","句安","王买","李辅","胡渊","卫瓘","杜预","诸葛绪","邓艾","钟会","邓艾","钟会","司马炎","钟会"},
            new String[]{"步","骑","步","弓","骑","骑","步","步","弓","步","骑","弓","步","弓","步","骑","步","骑","步","骑"},
            Arrays.asList(dp("秘银神剑","传说"),dp("秘银战甲","传说")));

        // ===== 战役12: 一统天下 (20关, 预留) =====
        reg("campaign_yitong","一统天下","天下分久必合，谁能问鼎天下？",180,200,180,1,22,12, 20,true,"魏军",
            new String[]{"王浑","王濬","杜预","贾充","羊祜","王戎","王衍","刘琨","祖逖","陶侃","桓温","谢玄","刘裕","羊祜","杜预","王濬","贾充","王浑","陆抗","司马炎"},
            new String[]{"骑","弓","步","弓","步","步","弓","步","骑","步","骑","步","步","步","步","弓","弓","骑","步","步"},
            Arrays.asList(dp("秘银神剑","传说"),dp("秘银战甲","传说")));

        log.info("战役配置初始化完成，共{}个战役", campaignConfigs.size());
    }

    private Campaign.DropPreview dp(String name, String quality) {
        return Campaign.DropPreview.builder().name(name).quality(quality).build();
    }

    private void reg(String id, String name, String desc,
                     int lvMin, int lvMax, int reqLv,
                     int dailyLimit, int staminaCost, int order,
                     int stageCount, boolean fullFormation, String faction,
                     String[] generals, String[] troopTypes,
                     List<Campaign.DropPreview> dropPreviews) {
        int baseExp = 60 + lvMin * 25;
        long baseSilver = 40 + (long) lvMin * 18;
        int[] equipIds = getEquipPreIds(lvMax);
        int[] itemIds = getItemDropPool(lvMax);
        String prefix = id.replace("campaign_", "");

        Campaign campaign = Campaign.builder()
                .id(id).name(name).description(desc)
                .icon("/images/campaign/" + prefix + ".png")
                .backgroundImage("/images/campaign/bg_" + prefix + ".jpg")
                .enemyLevelMin(lvMin).enemyLevelMax(lvMax)
                .expRewardMin(baseExp).expRewardMax(baseExp * stageCount)
                .dailyLimit(dailyLimit).staminaCost(staminaCost)
                .requiredLevel(reqLv).order(order)
                .stages(buildStages(generals, troopTypes, prefix,
                        lvMin, lvMax, stageCount, fullFormation, faction,
                        baseExp, baseSilver, equipIds, itemIds))
                .dropPreviews(dropPreviews)
                .build();
        campaignConfigs.put(id, campaign);
    }

    /**
     * 关卡生成 - 支持7/20关、单/6NPC阵型、equipment_pre+item掉落
     */
    private List<Campaign.Stage> buildStages(
            String[] generals, String[] troopTypes, String prefix,
            int minLv, int maxLv, int stageCount,
            boolean fullFormation, String faction,
            int baseExp, long baseSilver,
            int[] equipPreIds, int[] itemDropIds) {

        List<Campaign.Stage> stages = new ArrayList<>();
        int levelRange = maxLv - minLv;
        Random rng = new Random(prefix.hashCode());
        int fi = factionIdx(faction);
        String[][] npcPool = FACTION_NPC_POOL[fi];
        String[] types = {"步","骑","弓"};

        for (int i = 1; i <= stageCount; i++) {
            int npcLevel = minLv + (int) Math.round((double) levelRange * i / stageCount);
            npcLevel = Math.max(npcLevel, minLv + 1);
            boolean isBoss = (i == stageCount);

            double npcQuality = 1.0 + (double)(i - 1) / Math.max(1, stageCount - 1) * 0.9;
            if (isBoss) npcQuality += 0.15;

            int gIdx = (i - 1) % generals.length;
            String mainGeneral = generals[gIdx];
            String troopType = troopTypes[gIdx];

            int[] attrs = generalService.calcAttributes(npcQuality, troopType, npcLevel);
            int npcAtk = attrs[0] + (int)(attrs[0] * 0.4);
            int npcDef = attrs[1] + (int)(attrs[1] * 0.4);
            int npcTier = Math.min(9, 1 + npcLevel / 20);
            int npcSoldiers = BattleCalculator.getTierMaxSoldiers(npcTier);
            int npcSoldierHp = BattleCalculator.getTierSoldierHp(npcTier);

            if (isBoss) {
                npcAtk = (int)(npcAtk * 1.35);
                npcDef = (int)(npcDef * 1.35);
                npcSoldiers = (int)(npcSoldiers * 1.3);
            }

            // 阵型
            int npcCount = fullFormation ? 6 : Math.min(6, 1 + (i - 1) * 5 / Math.max(1, stageCount - 1));
            List<Campaign.StageNpc> formation = new ArrayList<>();
            for (int pos = 0; pos < npcCount; pos++) {
                String nm; String tt; int lv; boolean boss = false;
                if (pos == 0) {
                    nm = mainGeneral; tt = troopType; lv = npcLevel; boss = isBoss;
                } else {
                    int ti = (pos + rng.nextInt(3)) % 3;
                    tt = types[ti];
                    nm = npcPool[ti][(pos + rng.nextInt(npcPool[ti].length)) % npcPool[ti].length];
                    lv = Math.max(1, npcLevel - 1 - rng.nextInt(3));
                }
                double q = pos == 0 ? npcQuality : npcQuality * 0.85;
                int[] a = generalService.calcAttributes(q, tt, lv);
                int fa = a[0] + (int)(a[0] * 0.35); int fd = a[1] + (int)(a[1] * 0.35);
                if (boss) { fa = (int)(fa * 1.35); fd = (int)(fd * 1.35); }
                int ft = Math.min(9, 1 + lv / 20);
                int fs = BattleCalculator.getTierMaxSoldiers(ft);
                if (boss) fs = (int)(fs * 1.3);

                formation.add(Campaign.StageNpc.builder()
                        .position(pos).name(nm).avatar(nm + ".png")
                        .level(lv).troopType(tt).soldierCount(fs).soldierTier(ft)
                        .attack(fa).defense(fd).valor(a[2]).command(a[3]).dodge(a[4]).mobility(a[5])
                        .hp(300 + lv * 100 + (boss ? lv * 50 : 0)).isBoss(boss).build());
            }

            // 掉落: 白银 + equipment_pre装备 + item道具
            List<Campaign.StageDrop> drops = new ArrayList<>();
            drops.add(Campaign.StageDrop.builder()
                    .type("RESOURCE").itemId("silver").itemName("白银")
                    .dropRate(100).minCount(50 * i).maxCount(100 * i).build());

            if (equipPreIds.length > 0) {
                int equipId = equipPreIds[rng.nextInt(equipPreIds.length)];
                drops.add(Campaign.StageDrop.builder()
                        .type("EQUIP_PRE").equipPreId(equipId)
                        .itemId(String.valueOf(equipId)).itemName("装备")
                        .dropRate(isBoss ? 80 : Math.min(50, 10 + i * 2))
                        .minCount(1).maxCount(1).build());
                if (isBoss && equipPreIds.length > 1) {
                    int equipId2 = equipPreIds[rng.nextInt(equipPreIds.length)];
                    drops.add(Campaign.StageDrop.builder()
                            .type("EQUIP_PRE").equipPreId(equipId2)
                            .itemId(String.valueOf(equipId2)).itemName("BOSS装备")
                            .dropRate(60).minCount(1).maxCount(1).build());
                }
            }

            if (itemDropIds.length > 0) {
                int pick = 1 + rng.nextInt(2);
                Set<Integer> picked = new HashSet<>();
                for (int n = 0; n < pick && picked.size() < itemDropIds.length; n++) {
                    int itemId = itemDropIds[rng.nextInt(itemDropIds.length)];
                    if (picked.add(itemId)) {
                        drops.add(Campaign.StageDrop.builder()
                                .type("ITEM").itemId(String.valueOf(itemId))
                                .dropRate(isBoss ? 60 : 20 + rng.nextInt(20))
                                .minCount(1).maxCount(isBoss ? 3 : 2).build());
                    }
                }
            }

            stages.add(Campaign.Stage.builder()
                    .id(prefix + "_stage_" + i).stageNum(i)
                    .name(isBoss ? "BOSS: " + mainGeneral : "第" + i + "关")
                    .enemyGeneralName(mainGeneral)
                    .enemyGeneralIcon("/images/general/" + mainGeneral + ".png")
                    .enemyLevel(npcLevel).enemyTroops(npcSoldiers)
                    .enemyAttack(npcAtk).enemyDefense(npcDef)
                    .enemyValor(attrs[2]).enemyCommand(attrs[3])
                    .enemyDodge(attrs[4]).enemyMobility(attrs[5])
                    .enemySoldierHp(npcSoldierHp).enemyTroopType(troopType)
                    .enemyTierMultiplier(BattleCalculator.getTierMultiplier(npcTier))
                    .expReward(baseExp + i * (baseExp / 2))
                    .silverReward(baseSilver + i * (baseSilver / 2))
                    .isBoss(isBoss).drops(drops).formation(formation)
                    .build());
        }
        return stages;
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
                    
                    boolean unlocked = userLevel >= campaign.getRequiredLevel();
                    campaignInfo.put("unlocked", unlocked);
                    campaignInfo.put("locked", !unlocked);
                    campaignInfo.put("stages", campaign.getStages());
                    
                    CampaignProgress progress = progressMap.get(campaign.getId());
                    if (progress != null) {
                        if (!today.equals(progress.getTodayDate())) {
                            progress.setTodayChallengeCount(0);
                            progress.setTodayDate(today);
                            campaignRepository.save(progress);
                        }
                        Map<String, Object> progressInfo = new HashMap<>();
                        progressInfo.put("currentStage", progress.getCurrentStage());
                        progressInfo.put("maxClearedStage", progress.getMaxClearedStage());
                        progressInfo.put("todayCount", progress.getTodayChallengeCount());
                        progressInfo.put("status", progress.getStatus());
                        progressInfo.put("fullCleared", progress.getFullCleared());
                        progressInfo.put("currentTroops", progress.getCurrentTroops());
                        progressInfo.put("maxTroops", progress.getMaxTroops());
                        progressInfo.put("reviveCount", progress.getReviveCount());
                        campaignInfo.put("progress", progressInfo);
                        campaignInfo.put("status", progress.getStatus());
                        campaignInfo.put("fullCleared", progress.getFullCleared());
                        campaignInfo.put("canSweep", progress.getFullCleared());
                    } else {
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
     * 前端上报战斗结果（前端本地模拟战斗后调用）
     */
    public CampaignProgress.BattleResult reportBattleResult(
            String odUserId, String campaignId,
            boolean victory, Integer troopsLost, Integer remainingTroops) {
        Campaign campaign = campaignConfigs.get(campaignId);
        if (campaign == null) throw new BusinessException("战役不存在");

        CampaignProgress progress = campaignRepository.findByUserIdAndCampaignId(odUserId, campaignId);
        if (progress == null || !"IN_PROGRESS".equals(progress.getStatus())) {
            throw new BusinessException("战役未开始");
        }

        int stageIndex = progress.getCurrentStage() - 1;
        if (stageIndex < 0 || stageIndex >= campaign.getStages().size()) {
            throw new BusinessException("关卡不存在");
        }
        Campaign.Stage stage = campaign.getStages().get(stageIndex);

        int actualRemaining = remainingTroops != null ? remainingTroops
                : Math.max(0, progress.getCurrentTroops() - (troopsLost != null ? troopsLost : 0));
        int actualLost = troopsLost != null ? troopsLost
                : Math.max(0, progress.getCurrentTroops() - actualRemaining);

        CampaignProgress.BattleResult result = CampaignProgress.BattleResult.builder()
                .victory(victory)
                .stageNum(progress.getCurrentStage())
                .remainingTroops(actualRemaining)
                .troopsLost(actualLost)
                .isLastStage(progress.getCurrentStage() >= campaign.getStages().size())
                .build();

        if (victory) {
            long expGained = stage.getExpReward() != null ? stage.getExpReward() : 0;
            long silverGained = stage.getSilverReward() != null ? stage.getSilverReward() : 0;
            result.setExpGained(expGained);
            result.setSilverGained(silverGained);

            Random dropRandom = new Random();
            List<CampaignProgress.DropItem> drops = processDrops(stage.getDrops(), dropRandom);
            result.setDrops(drops);

            General general = generalService.getGeneralById(progress.getGeneralId());
            if (general != null) {
                long curExp = general.getExp() != null ? general.getExp() : 0;
                general.setExp(curExp + expGained);
                generalService.saveGeneral(general);
            }

            UserResource resource = userResourceService.getUserResource(odUserId);
            resource.setSilver(resource.getSilver() + silverGained);
            userResourceService.saveUserResource(resource);

            for (CampaignProgress.DropItem drop : drops) {
                if ("EQUIPMENT".equals(drop.getType())) {
                    Equipment equipment = generateRandomEquipment(odUserId, drop.getQuality(), stage.getEnemyLevel());
                    equipmentRepository.save(equipment);
                }
            }

            boolean isFirstClear = progress.getMaxClearedStage() < progress.getCurrentStage();
            result.setIsFirstClear(isFirstClear);

            progress.setCurrentTroops(actualRemaining);
            progress.setTotalExpGained(progress.getTotalExpGained() + expGained);
            progress.setTotalSilverGained(progress.getTotalSilverGained() + silverGained);

            if (isFirstClear) progress.setMaxClearedStage(progress.getCurrentStage());

            if (progress.getCurrentStage() >= campaign.getStages().size()) {
                progress.setStatus("COMPLETED");
                progress.setFullCleared(true);
            } else {
                progress.setCurrentStage(progress.getCurrentStage() + 1);
            }
        } else {
            progress.setCurrentTroops(actualRemaining);
            if (actualRemaining <= 0 && progress.getReviveCount() <= 0) {
                progress.setStatus("IDLE");
            }
        }

        progress.setUpdateTime(System.currentTimeMillis());
        campaignRepository.save(progress);
        return result;
    }

    /**
     * 进攻当前关卡 - 服务端模拟战斗
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

        String troopCat = general.getTroopType() != null ? general.getTroopType() : "步";

        // 品质兵法发动加成 + 名将特性翻倍
        if (general.getSlotId() != null && general.getSlotId() > 0) {
            player.tacticsTriggerBonus = generalService.getTacticsTriggerBonus(general.getSlotId());
            player.tacticsTriggerMultiplier = generalService.getTacticsTriggerMultiplier(general.getSlotId());
        }

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
