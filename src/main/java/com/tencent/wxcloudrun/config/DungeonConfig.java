package com.tencent.wxcloudrun.config;

import com.tencent.wxcloudrun.model.Dungeon;
import com.tencent.wxcloudrun.model.DungeonNpc;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * 副本配置 - 三国战役（按历史时间顺序）
 */
@Component
public class DungeonConfig {
    
    // 副本配置
    private Map<String, Dungeon> dungeons = new LinkedHashMap<>();
    
    // 各战役的NPC武将配置
    private static final Map<String, String[]> BATTLE_GENERALS = new HashMap<>();
    
    static {
        // 桃园结义 - 黄巾军将领
        BATTLE_GENERALS.put("DUNGEON_1", new String[]{
            "程远志", "邓茂", "张曼成", "波才", "彭脱", "卜己"
        });
        
        // 平定黄巾 - 黄巾军将领
        BATTLE_GENERALS.put("DUNGEON_5", new String[]{
            "程远志", "邓茂", "高升", "张宝", "张梁", "管亥", "裴元绍", "周仓"
        });
        
        // 温酒斩华雄 - 董卓军将领
        BATTLE_GENERALS.put("DUNGEON_10", new String[]{
            "胡轸", "赵岑", "李傕", "郭汜", "樊稠", "张济", "徐荣", "牛辅", "董旻", "华雄"
        });
        
        // 虎牢关之战 - 董卓军精锐
        BATTLE_GENERALS.put("DUNGEON_20", new String[]{
            "胡轸", "李傕", "郭汜", "樊稠", "张济", "徐荣", "牛辅", "李儒", 
            "董旻", "高顺", "张辽", "臧霸", "侯成", "魏续", "吕布"
        });
        
        // 官渡之战 - 袁绍军将领（魏视角）
        BATTLE_GENERALS.put("DUNGEON_40", new String[]{
            "淳于琼", "韩猛", "蒋奇", "张南", "焦触", "吕旷", "吕翔", "马延", 
            "韩莒子", "眭元进", "蒋义渠", "高干", "审配", "逢纪", "郭图", 
            "辛评", "辛毗", "张郃", "高览", "颜良", "文丑"
        });
        
        // 长坂坡之战 - 曹操军将领（蜀视角）
        BATTLE_GENERALS.put("DUNGEON_60", new String[]{
            "夏侯恩", "钟缙", "钟绅", "淳于导", "夏侯杰", "曹洪", "张郃", "马延",
            "焦触", "张绣", "许褚", "徐晃", "于禁", "乐进", "李典", 
            "夏侯惇", "夏侯渊", "曹仁", "张辽", "曹纯"
        });
        
        // 赤壁之战 - 曹操军将领（吴蜀视角）
        BATTLE_GENERALS.put("DUNGEON_80", new String[]{
            "蔡瑁", "张允", "毛玠", "于禁", "乐进", "徐晃", "张郃", "朱灵",
            "路昭", "冯楷", "王朗", "贾诩", "程昱", "荀攸", "荀彧", 
            "曹洪", "曹纯", "夏侯渊", "夏侯惇", "曹仁"
        });
        
        // 威震逍遥津 - 东吴将领（魏视角）
        BATTLE_GENERALS.put("DUNGEON_100", new String[]{
            "宋谦", "贾华", "徐盛", "丁奉", "潘璋", "马忠", "朱然", "骆统",
            "虞翻", "陆绩", "顾雍", "张昭", "周泰", "韩当", "黄盖", 
            "程普", "甘宁", "凌统", "吕蒙", "陈武"
        });
        
        // ==================== 预留战役 ====================
        
        // 潼关之战 - 马超西凉军（魏视角）
        BATTLE_GENERALS.put("DUNGEON_110", new String[]{
            "成宜", "李堪", "张横", "梁兴", "侯选", "程银", "杨秋", "马玩",
            "韩遂", "马岱", "马铁", "马休", "庞德", "杨阜", "姜叙", 
            "梁宽", "赵衢", "尹奉", "郭援", "马超"
        });
        
        // 定军山之战 - 曹魏守军（蜀视角）
        BATTLE_GENERALS.put("DUNGEON_120", new String[]{
            "杜袭", "郭淮", "王平", "张著", "陈式", "杨洪", "吴懿", "吴班",
            "雷铜", "刘封", "孟达", "法正", "魏延", "严颜", "张翼", 
            "曹休", "曹真", "徐晃", "张郃", "夏侯渊"
        });
        
        // 水淹七军 - 曹魏援军（蜀视角）
        BATTLE_GENERALS.put("DUNGEON_130", new String[]{
            "成何", "翟元", "董衡", "董超", "朱盖", "胡修", "傅方", "浩周",
            "吕建", "徐商", "吕常", "满宠", "曹仁", "曹洪", "徐晃", 
            "张辽", "夏侯惇", "于禁", "庞德", "曹操"
        });
        
        // 白衣渡江 - 蜀汉荆州守军（吴视角）
        BATTLE_GENERALS.put("DUNGEON_140", new String[]{
            "王甫", "赵累", "周仓", "廖化", "傅士仁", "糜芳", "潘濬", "郝普",
            "詹晏", "陈凤", "向朗", "杨仪", "马良", "费诗", "伊籍", 
            "王累", "关平", "关兴", "刘封", "关羽"
        });
        
        // 夷陵之战 - 蜀汉东征军（吴视角）
        BATTLE_GENERALS.put("DUNGEON_150", new String[]{
            "傅肜", "程畿", "马良", "冯习", "张南", "傅彤", "辅匡", "赵融",
            "廖淳", "向宠", "陈到", "吴班", "黄权", "刘宁", "杜路", 
            "关兴", "张苞", "黄忠", "赵云", "刘备"
        });
        
        // 七擒孟获 - 南蛮军（蜀视角）
        BATTLE_GENERALS.put("DUNGEON_160", new String[]{
            "董荼那", "阿会喃", "金环三结", "朵思大王", "带来洞主", "木鹿大王", 
            "兀突骨", "祝融夫人", "孟优", "鄂焕", "高定", "雍闿", "朱褒", 
            "杨锋", "忙牙长", "土安", "奚泥", "杨峰", "银冶洞主", "孟获"
        });
        
        // 街亭之战 - 曹魏军（蜀视角）
        BATTLE_GENERALS.put("DUNGEON_170", new String[]{
            "戴陵", "申耽", "申仪", "郭淮", "孙礼", "辛毗", "费曜", "郑文",
            "秦朗", "夏侯霸", "夏侯威", "夏侯惠", "夏侯和", "曹真", "曹爽", 
            "司马师", "司马昭", "张郃", "曹叡", "司马懿"
        });
        
        // 合肥新城之战 - 东吴军（魏视角）
        BATTLE_GENERALS.put("DUNGEON_180", new String[]{
            "张承", "孙桓", "朱桓", "朱异", "全琮", "全怿", "唐咨", "留赞",
            "吾粲", "丁封", "孙峻", "孙綝", "诸葛恪", "陆抗", "步骘", 
            "孙韶", "朱然", "丁奉", "陆逊", "孙权"
        });
        
        // 姜维北伐 - 曹魏/西晋军（蜀视角）
        BATTLE_GENERALS.put("DUNGEON_190", new String[]{
            "王经", "陈泰", "郭淮", "邓艾", "钟会", "诸葛绪", "师纂", "田续",
            "牵弘", "杨欣", "王颀", "杜预", "卫瓘", "羊祜", "王濬", 
            "贾充", "司马炎", "司马昭", "司马师", "司马懿"
        });
        
        // 天下归心 - 三国名将汇聚
        BATTLE_GENERALS.put("DUNGEON_200", new String[]{
            "张辽", "徐晃", "张郃", "于禁", "乐进", "典韦", "许褚", "曹仁",
            "夏侯渊", "夏侯惇", "马超", "黄忠", "赵云", "张飞", "关羽", 
            "周瑜", "陆逊", "吕蒙", "诸葛亮", "吕布"
        });
    }
    
    @PostConstruct
    public void init() {
        initDungeons();
    }
    
    /**
     * 初始化18个三国战役副本（按时间顺序，平衡魏蜀吴）
     */
    private void initDungeons() {
        // ==================== 当前开放的8个副本 ====================
        
        // 1级副本 - 桃园结义（184年，群雄）
        dungeons.put("DUNGEON_1", createDungeon(
            "DUNGEON_1", "桃园结义", "三人同心，共创大业",
            1, "🌸", 6, "群雄",
            "刘备、关羽、张飞三人于桃园之中结为异姓兄弟，立下同心协力，救困扶危的誓言，三国传奇由此开篇。",
            100, 500L, 0, 50
        ));
        
        // 5级副本 - 平定黄巾（184年，群雄）
        dungeons.put("DUNGEON_5", createDungeon(
            "DUNGEON_5", "平定黄巾", "首战告捷，崭露头角",
            5, "⚔️", 8, "群雄",
            "黄巾军四起，刘关张三兄弟率义军出征讨贼，首战告捷，斩将夺旗，英雄之名初显天下。",
            500, 1000L, 0, 80
        ));
        
        // 10级副本 - 温酒斩华雄（190年，群雄）
        dungeons.put("DUNGEON_10", createDungeon(
            "DUNGEON_10", "温酒斩华雄", "马弓手威震诸侯",
            10, "🍶", 10, "群雄",
            "十八路诸侯会盟，华雄连斩数将，众人皆惧。关羽请战，曹操斟下热酒，关羽出战斩华雄而归，酒尚温热。",
            1000, 2000L, 5, 120
        ));
        
        // 20级副本 - 虎牢关之战（190年，群雄）
        dungeons.put("DUNGEON_20", createDungeon(
            "DUNGEON_20", "虎牢关之战", "三英战吕布，天下扬名",
            20, "🐅", 15, "群雄",
            "虎牢关前，吕布手持方天画戟，无人能敌。刘关张三兄弟联手迎战，大战数十回合，吕布败走，三英威震天下。",
            3000, 5000L, 10, 200
        ));
        
        // 40级副本 - 官渡之战（200年，魏）
        dungeons.put("DUNGEON_40", createDungeon(
            "DUNGEON_40", "官渡之战", "以少胜多，奇谋制胜",
            40, "🏰", 20, "魏",
            "曹操与袁绍决战于官渡，兵力悬殊。许攸献计火烧乌巢，曹军以少胜多，一战奠定北方霸业。",
            8000, 10000L, 20, 350
        ));
        
        // 60级副本 - 长坂坡之战（208年，蜀）
        dungeons.put("DUNGEON_60", createDungeon(
            "DUNGEON_60", "长坂坡之战", "赵云救主，七进七出",
            60, "🐎", 20, "蜀",
            "曹军追击刘备于长坂坡，赵云单枪匹马杀入曹营，七进七出，怀抱幼主阿斗，血染战袍，终于突出重围。",
            15000, 20000L, 50, 500
        ));
        
        // 80级副本 - 赤壁之战（208年，吴/蜀）
        dungeons.put("DUNGEON_80", createDungeon(
            "DUNGEON_80", "赤壁之战", "火烧战船，三分天下",
            80, "🔥", 20, "吴",
            "孙刘联军于赤壁迎战曹操百万大军。周瑜巧施连环计，诸葛亮借东风，火烧曹军战船，三分天下之势由此奠定。",
            30000, 50000L, 100, 700
        ));
        
        // 100级副本 - 威震逍遥津（215年，魏）
        dungeons.put("DUNGEON_100", createDungeon(
            "DUNGEON_100", "威震逍遥津", "八百破十万，威名远扬",
            100, "🦁", 20, "魏",
            "张辽率八百精骑突袭孙权十万大军，直冲中军，孙权几乎被擒。此役后张辽止啼传遍江东，威名震天下。",
            50000, 100000L, 200, 1000
        ));
        
        // ==================== 预留的10个副本（未来开放）====================
        
        // 110级副本 - 潼关之战（211年，魏）
        dungeons.put("DUNGEON_110", createDungeon(
            "DUNGEON_110", "潼关之战", "割须弃袍，虎痴救主",
            110, "⛰️", 20, "魏",
            "马超率西凉铁骑攻打潼关，曹操割须弃袍狼狈而逃。许褚裸衣斗马超，曹军险中求胜，终平定关中。",
            70000, 150000L, 250, 1200
        ));
        
        // 120级副本 - 定军山之战（219年，蜀）
        dungeons.put("DUNGEON_120", createDungeon(
            "DUNGEON_120", "定军山之战", "老将建功，汉中奠基",
            120, "🏔️", 20, "蜀",
            "汉中争夺战中，老将黄忠在定军山一战大显神威，力斩敌军主帅，为刘备夺取汉中、进位汉中王立下首功。",
            90000, 200000L, 300, 1500
        ));
        
        // 130级副本 - 水淹七军（219年，蜀）
        dungeons.put("DUNGEON_130", createDungeon(
            "DUNGEON_130", "水淹七军", "关公显圣，威震华夏",
            130, "🌊", 20, "蜀",
            "关羽围攻樊城，趁秋雨连绵、汉水暴涨之机，水淹于禁七军，生擒庞德，威震华夏，曹操甚至欲迁都以避其锋。",
            120000, 250000L, 350, 1800
        ));
        
        // 140级副本 - 白衣渡江（219年，吴）
        dungeons.put("DUNGEON_140", createDungeon(
            "DUNGEON_140", "白衣渡江", "奇兵突袭，出其不意",
            140, "🚢", 20, "吴",
            "吕蒙率军身穿白衣扮作商人，悄然渡过长江，出其不意奇袭荆州。此计精妙绝伦，堪称三国奇谋之典范。",
            150000, 300000L, 400, 2200
        ));
        
        // 150级副本 - 夷陵之战（222年，吴）
        dungeons.put("DUNGEON_150", createDungeon(
            "DUNGEON_150", "夷陵之战", "火烧连营，陆逊成名",
            150, "🔥", 20, "吴",
            "刘备率大军东征伐吴，陆逊坚守不战，待蜀军疲惫之际，火烧连营七百里，一战成名，奠定东吴柱石之位。",
            180000, 350000L, 450, 2600
        ));
        
        // 160级副本 - 七擒孟获（225年，蜀）
        dungeons.put("DUNGEON_160", createDungeon(
            "DUNGEON_160", "七擒孟获", "攻心为上，平定南方",
            160, "🐘", 20, "蜀",
            "诸葛亮南征，七次生擒南蛮王孟获，七次释放。孟获终于心服口服，誓不再反，南方由此安定，蜀汉后方无忧。",
            220000, 400000L, 500, 3000
        ));
        
        // 170级副本 - 街亭之战（228年，蜀）
        dungeons.put("DUNGEON_170", createDungeon(
            "DUNGEON_170", "街亭之战", "挥泪斩马谡，出师未捷",
            170, "📜", 20, "蜀",
            "诸葛亮第一次北伐，马谡违令导致街亭失守。诸葛亮挥泪斩马谡，自贬三级，虽出师未捷，却显军纪严明。",
            260000, 450000L, 550, 3500
        ));
        
        // 180级副本 - 合肥新城之战（234年，魏）
        dungeons.put("DUNGEON_180", createDungeon(
            "DUNGEON_180", "合肥新城之战", "满宠坚守，东吴受挫",
            180, "🏯", 20, "魏",
            "孙权率十万大军围攻合肥新城，魏将满宠率数千守军坚守，以少敌多。诸葛恪攻城不克，东吴再次铩羽而归。",
            300000, 500000L, 600, 4000
        ));
        
        // 190级副本 - 姜维北伐（253年，蜀）
        dungeons.put("DUNGEON_190", createDungeon(
            "DUNGEON_190", "姜维北伐", "九伐中原，薪火相传",
            190, "🗡️", 20, "蜀",
            "姜维继承诸葛亮遗志，九次北伐中原。虽胜多败少，却难挽蜀汉颓势。一片丹心，薪火相传，可歌可泣。",
            350000, 550000L, 650, 4500
        ));
        
        // 200级副本 - 天下归心（终章）
        dungeons.put("DUNGEON_200", createDungeon(
            "DUNGEON_200", "天下归心", "英雄传奇，千古流芳",
            200, "🏆", 20, "群雄",
            "三国英雄辈出，群雄逐鹿。曹操之雄才、刘备之仁德、孙权之英明，皆为后世传颂。这是一个英雄的时代。",
            400000, 600000L, 800, 5000
        ));
    }
    
    /**
     * 创建副本
     */
    private Dungeon createDungeon(String id, String name, String description,
                                  int unlockLevel, String icon, int npcCount, String faction,
                                  String lore, int recommendedPower,
                                  long silverReward, int goldReward, int baseExp) {
        
        List<DungeonNpc> npcs = generateNpcs(id, unlockLevel, npcCount, baseExp);
        
        return Dungeon.builder()
            .id(id)
            .name(name)
            .description(description)
            .unlockLevel(unlockLevel)
            .icon(icon)
            .staminaCost(10)
            .dailyLimit(5)
            .npcCount(npcCount)
            .npcs(npcs)
            .clearReward(Dungeon.Reward.builder()
                .exp(baseExp * npcCount / 2) // 通关额外经验
                .silver(silverReward)
                .gold(goldReward)
                .build())
            .lore(lore)
            .recommendedPower(recommendedPower)
            .build();
    }
    
    /**
     * 生成副本NPC列表（使用真实武将名）
     */
    private List<DungeonNpc> generateNpcs(String dungeonId, int dungeonLevel, int npcCount, int baseExp) {
        List<DungeonNpc> npcs = new ArrayList<>();
        String[] generals = BATTLE_GENERALS.getOrDefault(dungeonId, new String[]{});
        
        for (int i = 1; i <= npcCount; i++) {
            DungeonNpc npc = generateNpc(dungeonId, dungeonLevel, i, npcCount, generals, baseExp);
            npcs.add(npc);
        }
        
        return npcs;
    }
    
    /**
     * 生成单个NPC
     */
    private DungeonNpc generateNpc(String dungeonId, int dungeonLevel, int index, int totalCount, 
                                   String[] generals, int baseExp) {
        int qualityId;
        boolean isBoss = false;
        boolean dropEquipment = false;
        String dropType = null;
        Integer dropLevel = null;
        Integer dropRate = null;
        List<Integer> dropEquipPreIds = null;
        
        switch (dungeonLevel) {
            case 1:
                if (index == totalCount) {
                    qualityId = 2;
                    isBoss = true;
                    dropEquipment = true;
                    dropType = "DUNGEON";
                    dropLevel = 1;
                    dropRate = 80;
                    dropEquipPreIds = rangeList(1, 6);
                } else {
                    qualityId = 1;
                }
                break;

            case 5:
            case 10:
                qualityId = 1;
                break;
                
            case 20:
                if (index == totalCount) {
                    qualityId = 3;
                    isBoss = true;
                    dropEquipment = true;
                    dropType = "DUNGEON";
                    dropLevel = 20;
                    dropRate = 60;
                    dropEquipPreIds = rangeList(7, 12);
                } else {
                    qualityId = 2;
                }
                break;
                
            case 40:
                if (index == totalCount) {
                    qualityId = 3;
                    isBoss = true;
                    dropEquipment = true;
                    dropType = "DUNGEON";
                    dropLevel = 40;
                    dropRate = 45;
                    dropEquipPreIds = rangeList(19, 24); // 陷阵套装
                } else {
                    qualityId = 2;
                }
                break;
                
            case 60:
                // NPC#10 掉落50级狂战套装; Boss掉落60级天狼+熊王套装
                if (index == 10) {
                    qualityId = 3;
                    isBoss = true;
                    dropEquipment = true;
                    dropType = "DUNGEON";
                    dropLevel = 50;
                    dropRate = 40;
                    dropEquipPreIds = rangeList(31, 36); // 狂战套装
                } else if (index == totalCount) {
                    qualityId = 4;
                    isBoss = true;
                    dropEquipment = true;
                    dropType = "DUNGEON";
                    dropLevel = 60;
                    dropRate = 30;
                    List<Integer> ids = new ArrayList<>(rangeList(37, 42)); // 天狼
                    ids.addAll(rangeList(55, 60)); // 熊王
                    dropEquipPreIds = ids;
                } else {
                    qualityId = 2;
                }
                break;
                
            case 80:
                // NPC#10 掉落70级天诛/地煞/幽冥; Boss掉落80级雄狮
                if (index == 10) {
                    qualityId = 4;
                    isBoss = true;
                    dropEquipment = true;
                    dropType = "DUNGEON";
                    dropLevel = 70;
                    dropRate = 25;
                    dropEquipPreIds = rangeList(61, 78); // 天诛+地煞+幽冥
                } else if (index == totalCount) {
                    qualityId = 4;
                    isBoss = true;
                    dropEquipment = true;
                    dropType = "DUNGEON";
                    dropLevel = 80;
                    dropRate = 15;
                    dropEquipPreIds = rangeList(79, 84); // 雄狮套装
                } else {
                    qualityId = 2;
                }
                break;
                
            case 100:
                // NPC#10 掉落90级圣象; Boss掉落100级玄武
                if (index == 10) {
                    qualityId = 5;
                    isBoss = true;
                    dropEquipment = true;
                    dropType = "DUNGEON";
                    dropLevel = 90;
                    dropRate = 18;
                    dropEquipPreIds = rangeList(97, 102); // 圣象套装
                } else if (index == totalCount) {
                    qualityId = 5;
                    isBoss = true;
                    dropEquipment = true;
                    dropType = "DUNGEON";
                    dropLevel = 100;
                    dropRate = 10;
                    dropEquipPreIds = rangeList(109, 114); // 玄武套装
                } else {
                    qualityId = 2;
                }
                break;
                
            default:
                if (index == 10) {
                    qualityId = 5;
                    isBoss = true;
                    dropEquipment = true;
                    dropType = "DUNGEON";
                    dropLevel = dungeonLevel - 20;
                    dropRate = Math.max(8, 15 - (dungeonLevel - 100) / 20);
                } else if (index == totalCount) {
                    qualityId = 5;
                    isBoss = true;
                    dropEquipment = true;
                    dropType = "DUNGEON";
                    dropLevel = dungeonLevel;
                    dropRate = Math.max(5, 12 - (dungeonLevel - 100) / 20);
                } else {
                    qualityId = 3;
                }
        }
        
        // 品质信息
        String qualityName = getQualityName(qualityId);
        String qualityColor = getQualityColor(qualityId);
        
        // 计算属性 - 降低NPC强度，让战斗更公平
        double qualityMultiplier = getQualityMultiplier(qualityId);
        // 降低基础属性：从10倍降到3倍，使其与玩家装备后的属性相近
        int baseAttack = (int)(50 + dungeonLevel * 3 * qualityMultiplier);
        int baseDefense = (int)(30 + dungeonLevel * 2 * qualityMultiplier);
        int baseValor = (int)(20 + dungeonLevel * 1.5 * qualityMultiplier);
        int baseCommand = (int)(20 + dungeonLevel * 1.5 * qualityMultiplier);
        double baseDodge = Math.min(5 + qualityId * 2, 25);
        int baseMobility = (int)(30 + dungeonLevel * qualityMultiplier);
        
        // 进度加成从5%降到2%
        double progressMultiplier = 1.0 + (index - 1) * 0.02;
        int attack = (int)(baseAttack * progressMultiplier);
        int defense = (int)(baseDefense * progressMultiplier);
        int valor = (int)(baseValor * progressMultiplier);
        int command = (int)(baseCommand * progressMultiplier);
        int mobility = (int)(baseMobility * progressMultiplier);
        
        int power = (int)(attack * 1.2 + defense * 1.2 + valor * 1.5 + command * 1.5 + baseDodge * 2 + mobility);
        // 降低NPC血量，让战斗更快结束
        int soldiers = 300 + dungeonLevel * 5 + index * 10;
        
        // NPC名称 - 使用真实武将名
        String npcName;
        if (index <= generals.length) {
            npcName = generals[index - 1];
        } else {
            npcName = isBoss ? "守将" : "士兵";
        }
        
        // 计算击败经验
        int expReward = baseExp;
        if (isBoss) {
            expReward = (int)(baseExp * 2.5); // BOSS给2.5倍经验
        } else {
            expReward = (int)(baseExp * (1.0 + (index - 1) * 0.1)); // 后面的NPC经验略高
        }
        
        return DungeonNpc.builder()
            .index(index)
            .name(npcName)
            .level(dungeonLevel)
            .qualityId(qualityId)
            .qualityName(qualityName)
            .qualityColor(qualityColor)
            .avatar("")
            .icon(isBoss ? "👹" : "👤")
            .attack(attack)
            .defense(defense)
            .valor(valor)
            .command(command)
            .dodge(baseDodge)
            .mobility(mobility)
            .power(power)
            .soldiers(soldiers)
            .dropEquipment(dropEquipment)
            .dropType(dropType)
            .dropLevel(dropLevel)
            .dropRate(dropRate)
            .dropEquipPreIds(dropEquipPreIds)
            .isBoss(isBoss)
            .defeated(false)
            .expReward(expReward)
            .build();
    }
    
    private String getQualityName(int qualityId) {
        switch (qualityId) {
            case 1: return "白色";
            case 2: return "绿色";
            case 3: return "蓝色";
            case 4: return "紫色";
            case 5: return "橙色";
            case 6: return "红色";
            default: return "白色";
        }
    }
    
    private String getQualityColor(int qualityId) {
        switch (qualityId) {
            case 1: return "#FFFFFF";
            case 2: return "#32CD32";
            case 3: return "#4169E1";
            case 4: return "#9370DB";
            case 5: return "#FF8C00";
            case 6: return "#DC143C";
            default: return "#FFFFFF";
        }
    }
    
    private double getQualityMultiplier(int qualityId) {
        switch (qualityId) {
            case 1: return 1.0;
            case 2: return 1.2;
            case 3: return 1.5;
            case 4: return 2.0;
            case 5: return 2.5;
            case 6: return 3.0;
            default: return 1.0;
        }
    }
    
    // ==================== Getter方法 ====================
    
    public Dungeon getDungeon(String id) {
        return dungeons.get(id);
    }
    
    public Map<String, Dungeon> getAllDungeons() {
        return new LinkedHashMap<>(dungeons);
    }
    
    public List<Dungeon> getUnlockedDungeons(int playerLevel) {
        List<Dungeon> result = new ArrayList<>();
        for (Dungeon dungeon : dungeons.values()) {
            if (dungeon.getUnlockLevel() <= playerLevel) {
                result.add(dungeon);
            }
        }
        return result;
    }

    private static List<Integer> rangeList(int from, int to) {
        return IntStream.rangeClosed(from, to).boxed().collect(Collectors.toList());
    }
}
