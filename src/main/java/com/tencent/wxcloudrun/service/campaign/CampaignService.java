package com.tencent.wxcloudrun.service.campaign;

import com.tencent.wxcloudrun.exception.BusinessException;
import com.tencent.wxcloudrun.model.*;
import com.tencent.wxcloudrun.repository.CampaignRepository;
import com.tencent.wxcloudrun.repository.EquipmentRepository;
import com.tencent.wxcloudrun.repository.EquipmentPreRepository;
import com.tencent.wxcloudrun.service.equipment.EquipmentService;
import com.tencent.wxcloudrun.service.general.GeneralService;
import com.tencent.wxcloudrun.service.UserResourceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import com.tencent.wxcloudrun.service.battle.BattleCalculator;
import com.tencent.wxcloudrun.service.battle.BattleService;
import com.tencent.wxcloudrun.config.TacticsConfig;
import com.tencent.wxcloudrun.config.TacticsConfig.TacticsTemplate;
import com.tencent.wxcloudrun.dao.UserTacticsMapper;
import com.tencent.wxcloudrun.dao.StoryProgressMapper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;

import javax.annotation.PostConstruct;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class CampaignService {

    @Autowired @Lazy
    private com.tencent.wxcloudrun.service.dailytask.DailyTaskService dailyTaskService;
    
    private final CampaignRepository campaignRepository;
    private final UserResourceService userResourceService;
    private final GeneralService generalService;
    private final EquipmentRepository equipmentRepository;
    private final EquipmentPreRepository equipmentPreRepository;
    private final EquipmentService equipmentService;
    private final TacticsConfig tacticsConfig;
    private final UserTacticsMapper userTacticsMapper;
    private final com.tencent.wxcloudrun.service.herorank.PeerageService peerageService;
    private final com.tencent.wxcloudrun.service.formation.FormationService formationService;
    private final com.tencent.wxcloudrun.service.SuitConfigService suitConfigService;
    private final BattleService battleService;
    private final StoryProgressMapper storyProgressMapper;
    private final com.tencent.wxcloudrun.service.level.LevelService levelService;
    
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
        /* 6 魏军 */ {{"魏军步兵","魏军枪兵","魏军盾兵","魏军重甲","魏军精锐"},{"魏军骑兵","魏军铁骑","魏军精骑","虎卫骑","魏军突骑"},{"魏军弓手","魏军弓将","魏军神射","魏军精弓","魏军长弓"}},
        /* 7 刘军 */ {{"刘军步兵","刘军枪兵","刘军盾兵","刘军重甲","荆州精锐"},{"刘军骑兵","刘军铁骑","刘军精骑","荆州骑兵","刘军突骑"},{"刘军弓手","刘军弓将","刘军神射","荆州弓手","刘军长弓"}},
        /* 8 公孙 */ {{"公孙步兵","公孙枪兵","公孙盾兵","公孙重甲","幽州精锐"},{"白马义从","公孙铁骑","公孙精骑","幽州骑兵","公孙突骑"},{"公孙弓手","公孙弓将","公孙神射","幽州弓手","公孙长弓"}},
        /* 9 乌桓 */ {{"乌桓步兵","乌桓枪兵","乌桓勇士","乌桓精锐","乌桓力士"},{"乌桓骑兵","乌桓铁骑","乌桓精骑","乌桓突骑","乌桓战骑"},{"乌桓弓手","乌桓射手","乌桓神射","乌桓长弓","乌桓猎手"}}
    };
    private static final String[] FACTION_KEYS = {"黄巾","西凉","袁军","曹军","吕布","吴军","魏军","刘军","公孙","乌桓"};

    private static int factionIdx(String f) {
        for (int i = 0; i < FACTION_KEYS.length; i++) if (FACTION_KEYS[i].equals(f)) return i;
        return 3;
    }

    // ======================== NPC头像 (对齐APK MonsterShow_cfg.json pic字段) ========================
    private static final Map<String, String> NPC_PORTRAIT = new HashMap<>();
    static {
        // --- 战役1: 黄巾之乱 ---
        NPC_PORTRAIT.put("管亥",    "images/monsters/104.jpg");
        NPC_PORTRAIT.put("裴元绍",  "images/monsters/107.jpg");
        NPC_PORTRAIT.put("邓茂",    "images/monsters/108.jpg");
        NPC_PORTRAIT.put("高升",    "images/monsters/109.jpg");
        NPC_PORTRAIT.put("张宝",    "images/monsters/110.jpg");
        NPC_PORTRAIT.put("龚都",    "images/monsters/111.jpg");
        NPC_PORTRAIT.put("马元义",  "images/monsters/113.jpg");
        NPC_PORTRAIT.put("张梁",    "images/monsters/115.jpg");
        NPC_PORTRAIT.put("波才",    "images/monsters/119.jpg");
        NPC_PORTRAIT.put("张角",    "images/monsters/120.jpg");
        // --- 战役2: 江东诸侯战董卓 ---
        NPC_PORTRAIT.put("樊稠",    "images/monsters/204.jpg");
        NPC_PORTRAIT.put("徐荣",    "images/monsters/205.jpg");
        NPC_PORTRAIT.put("胡轸",    "images/monsters/208.jpg");
        NPC_PORTRAIT.put("郭汜",    "images/monsters/209.jpg");
        NPC_PORTRAIT.put("华雄",    "images/monsters/210.jpg");
        NPC_PORTRAIT.put("李傕",    "images/monsters/213.jpg");
        NPC_PORTRAIT.put("牛辅",    "images/monsters/215.jpg");
        NPC_PORTRAIT.put("张济",    "images/monsters/216.jpg");
        NPC_PORTRAIT.put("杨奉",    "images/monsters/217.jpg");
        NPC_PORTRAIT.put("李儒",    "images/monsters/213.jpg");
        NPC_PORTRAIT.put("董卓",    "images/monsters/220.jpg");
        // --- 战役3: 乱世群雄 ---
        NPC_PORTRAIT.put("张闿",    "images/monsters/302.jpg");
        NPC_PORTRAIT.put("曹豹",    "images/monsters/304.jpg");
        NPC_PORTRAIT.put("陈登",    "images/monsters/306.jpg");
        NPC_PORTRAIT.put("臧霸",    "images/monsters/308.jpg");
        NPC_PORTRAIT.put("陶谦",    "images/monsters/310.jpg");
        NPC_PORTRAIT.put("黄祖",    "images/monsters/312.jpg");
        NPC_PORTRAIT.put("文聘",    "images/monsters/314.jpg");
        NPC_PORTRAIT.put("甘宁",    "images/monsters/316.jpg");
        NPC_PORTRAIT.put("蔡瑁",    "images/monsters/319.jpg");
        NPC_PORTRAIT.put("刘表",    "images/monsters/320.jpg");
        // --- 战役4: 威震中原 ---
        NPC_PORTRAIT.put("公孙越",  "images/monsters/343.jpg");
        NPC_PORTRAIT.put("严纲",    "images/monsters/345.jpg");
        NPC_PORTRAIT.put("公孙范",  "images/monsters/349.jpg");
        NPC_PORTRAIT.put("公孙续",  "images/monsters/350.jpg");
        NPC_PORTRAIT.put("田楷",    "images/monsters/354.jpg");
        NPC_PORTRAIT.put("田豫",    "images/monsters/355.jpg");
        NPC_PORTRAIT.put("白马义从","images/monsters/391.png");
        NPC_PORTRAIT.put("公孙豹",  "images/monsters/358.jpg");
        NPC_PORTRAIT.put("公孙度",  "images/monsters/359.jpg");
        NPC_PORTRAIT.put("公孙瓒",  "images/monsters/360.jpg");
        // --- 战役5: 西凉铁骑 ---
        NPC_PORTRAIT.put("成宜",    "images/monsters/374.jpg");
        NPC_PORTRAIT.put("马铁",    "images/monsters/375.jpg");
        NPC_PORTRAIT.put("庞柔",    "images/monsters/379.jpg");
        NPC_PORTRAIT.put("庞德",    "images/monsters/380.jpg");
        NPC_PORTRAIT.put("马玩",    "images/monsters/384.jpg");
        NPC_PORTRAIT.put("马超",    "images/monsters/385.jpg");
        NPC_PORTRAIT.put("马岱",    "images/monsters/388.jpg");
        NPC_PORTRAIT.put("马休",    "images/monsters/389.jpg");
        NPC_PORTRAIT.put("马腾",    "images/monsters/390.jpg");
        // --- 战役6: 四世三公 ---
        NPC_PORTRAIT.put("袁尚",    "images/monsters/404.jpg");
        NPC_PORTRAIT.put("麴义",    "images/monsters/405.jpg");
        NPC_PORTRAIT.put("沮授",    "images/monsters/409.jpg");
        NPC_PORTRAIT.put("张郃",    "images/monsters/410.jpg");
        NPC_PORTRAIT.put("淳于琼",  "images/monsters/414.jpg");
        NPC_PORTRAIT.put("文丑",    "images/monsters/415.jpg");
        NPC_PORTRAIT.put("审配",    "images/monsters/416.jpg");
        NPC_PORTRAIT.put("高览",    "images/monsters/419.jpg");
        NPC_PORTRAIT.put("颜良",    "images/monsters/420.jpg");
        NPC_PORTRAIT.put("田丰",    "images/monsters/424.jpg");
        NPC_PORTRAIT.put("袁绍",    "images/monsters/425.jpg");
        // --- 战役7: 战神吕布 ---
        NPC_PORTRAIT.put("成廉",    "images/monsters/504.jpg");
        NPC_PORTRAIT.put("宋宪",    "images/monsters/505.jpg");
        NPC_PORTRAIT.put("李肃",    "images/monsters/509.jpg");
        NPC_PORTRAIT.put("曹性",    "images/monsters/510.jpg");
        NPC_PORTRAIT.put("魏续",    "images/monsters/514.jpg");
        NPC_PORTRAIT.put("高顺",    "images/monsters/515.jpg");
        NPC_PORTRAIT.put("侯成",    "images/monsters/500.jpg");
        NPC_PORTRAIT.put("张辽",    "images/monsters/520.jpg");
        NPC_PORTRAIT.put("陈宫",    "images/monsters/524.jpg");
        NPC_PORTRAIT.put("吕布",    "images/monsters/525.jpg");
        // --- 战役8: 征讨乌桓 ---
        NPC_PORTRAIT.put("苏仆延",  "images/monsters/605.jpg");
        NPC_PORTRAIT.put("丘力居",  "images/monsters/610.jpg");
        NPC_PORTRAIT.put("乌延",    "images/monsters/615.jpg");
        NPC_PORTRAIT.put("蹋顿",    "images/monsters/620.jpg");
        // --- 战役 专属名称的杂兵 (不会跨战役重名) ---
        NPC_PORTRAIT.put("黄巾斥候","images/monsters/100.jpg");
        NPC_PORTRAIT.put("黄巾前哨","images/monsters/100.jpg");
        NPC_PORTRAIT.put("黄巾先锋","images/monsters/100.jpg");
        NPC_PORTRAIT.put("黄巾中军","images/monsters/100.jpg");
        NPC_PORTRAIT.put("黄巾卫队","images/monsters/100.jpg");
        NPC_PORTRAIT.put("西凉左翼","images/monsters/200.jpg");
        NPC_PORTRAIT.put("西凉右翼","images/monsters/200.jpg");
        NPC_PORTRAIT.put("前锋部队","images/monsters/300.jpg");
        NPC_PORTRAIT.put("陶军卫队","images/monsters/300.jpg");
        NPC_PORTRAIT.put("刘军先锋","images/monsters/311.jpg");
        NPC_PORTRAIT.put("刘军卫队","images/monsters/311.jpg");
        NPC_PORTRAIT.put("前军部队","images/monsters/340.jpg");
        NPC_PORTRAIT.put("袁军前锋","images/monsters/400.jpg");
        NPC_PORTRAIT.put("袁军卫队","images/monsters/400.jpg");
        NPC_PORTRAIT.put("吕军先锋","images/monsters/500.jpg");
        NPC_PORTRAIT.put("吕军卫队","images/monsters/500.jpg");
        NPC_PORTRAIT.put("乌桓斥候","images/monsters/601.jpg");
        NPC_PORTRAIT.put("乌桓前锋","images/monsters/603.jpg");
        NPC_PORTRAIT.put("乌桓骑兵","images/monsters/614.jpg");
        NPC_PORTRAIT.put("乌桓勇士","images/monsters/616.jpg");
        NPC_PORTRAIT.put("首领卫队","images/monsters/618.jpg");
        // --- 非战役将领 (联盟BOSS/掠夺等其他系统用) ---
        NPC_PORTRAIT.put("曹操","images/generals/1020.jpg"); NPC_PORTRAIT.put("曹仁","images/generals/1021.jpg"); NPC_PORTRAIT.put("曹洪","images/generals/2005.jpg");
        NPC_PORTRAIT.put("夏侯惇","images/generals/1003.jpg"); NPC_PORTRAIT.put("夏侯渊","images/generals/1004.jpg");
        NPC_PORTRAIT.put("徐晃","images/generals/1005.jpg"); NPC_PORTRAIT.put("许褚","images/generals/1006.jpg"); NPC_PORTRAIT.put("典韦","images/generals/1007.jpg");
        NPC_PORTRAIT.put("于禁","images/generals/2006.jpg"); NPC_PORTRAIT.put("李典","images/generals/2007.jpg"); NPC_PORTRAIT.put("乐进","images/generals/2008.jpg");
        NPC_PORTRAIT.put("曹休","images/generals/2009.jpg"); NPC_PORTRAIT.put("曹真","images/generals/2010.jpg"); NPC_PORTRAIT.put("司马懿","images/generals/1022.jpg");
        NPC_PORTRAIT.put("孙权","images/generals/1023.jpg"); NPC_PORTRAIT.put("周瑜","images/generals/1024.jpg"); NPC_PORTRAIT.put("陆逊","images/generals/1025.jpg");
        NPC_PORTRAIT.put("吕蒙","images/generals/1026.jpg"); NPC_PORTRAIT.put("太史慈","images/generals/2018.jpg");
        NPC_PORTRAIT.put("黄盖","images/generals/2019.jpg"); NPC_PORTRAIT.put("程普","images/generals/2020.jpg"); NPC_PORTRAIT.put("鲁肃","images/generals/2021.jpg");
        NPC_PORTRAIT.put("周泰","images/generals/2022.jpg"); NPC_PORTRAIT.put("韩当","images/generals/2023.jpg"); NPC_PORTRAIT.put("丁奉","images/generals/2024.jpg");
        NPC_PORTRAIT.put("凌统","images/generals/2025.jpg");
    }

    private static final Map<String, String> FACTION_SOLDIER_PORTRAIT = new HashMap<>();
    static {
        FACTION_SOLDIER_PORTRAIT.put("黄巾", "images/monsters/100.jpg");
        FACTION_SOLDIER_PORTRAIT.put("西凉", "images/monsters/200.jpg");
        FACTION_SOLDIER_PORTRAIT.put("刘军", "images/monsters/311.jpg");
        FACTION_SOLDIER_PORTRAIT.put("公孙", "images/monsters/340.jpg");
        FACTION_SOLDIER_PORTRAIT.put("袁军", "images/monsters/400.jpg");
        FACTION_SOLDIER_PORTRAIT.put("吕布", "images/monsters/500.jpg");
        FACTION_SOLDIER_PORTRAIT.put("乌桓", "images/monsters/601.jpg");
        FACTION_SOLDIER_PORTRAIT.put("曹军", "images/monsters/901.jpg");
        FACTION_SOLDIER_PORTRAIT.put("吴军", "images/monsters/903.jpg");
        FACTION_SOLDIER_PORTRAIT.put("魏军", "images/monsters/901.jpg");
    }

    private static String getNpcPortrait(String name, String faction) {
        String p = NPC_PORTRAIT.get(name);
        if (p != null) return p;
        if (faction != null) {
            String fp = FACTION_SOLDIER_PORTRAIT.get(faction);
            if (fp != null) return fp;
        }
        return "images/monsters/m_def.png";
    }

    /**
     * APK equip_ID → equipment_pre ID 映射
     * APK中战役掉落的是装备宝箱道具，宝箱开启后随机获得对应套装的一个部件
     */
    private static final Map<Integer, int[]> EQUIP_BOX_MAP = new HashMap<>();
    static {
        EQUIP_BOX_MAP.put(15201, new int[]{22001,22002,22003,22004,22005,22006}); // 黑铁装备 (1-15级绿)
        EQUIP_BOX_MAP.put(15202, new int[]{22011,22012,22013,22014,22015,22016}); // 精钢装备 (20级绿)
        EQUIP_BOX_MAP.put(15203, new int[]{22021,22022,22023,22024,22025,22026}); // 紫铜装备 (40级绿)
        EQUIP_BOX_MAP.put(15204, new int[]{22031,22032,22033,22034,22035,22036}); // 亮银装备 (60级绿)
        EQUIP_BOX_MAP.put(15205, new int[]{22041,22042,22043,22044,22045,22046}); // 百炼装备 (80级绿)
        EQUIP_BOX_MAP.put(15206, new int[]{23021,23022,23023,23024,23025,23026}); // 宣武套装 (20级蓝)
        EQUIP_BOX_MAP.put(15207, new int[]{23031,23032,23033,23034,23035,23036}); // 折冲套装 (40级蓝)
        EQUIP_BOX_MAP.put(15208, new int[]{23041,23042,23043,23044,23045,23046}); // 骁勇套装 (60级蓝)
        EQUIP_BOX_MAP.put(15209, new int[]{23051,23052,23053,23054,23055,23056}); // 破俘套装 (80级蓝)
        EQUIP_BOX_MAP.put(15210, new int[]{23061,23062,23063,23064,23065,23066}); // 陷阵套装 (40级蓝)
        EQUIP_BOX_MAP.put(15211, new int[]{23071,23072,23073,23074,23075,23076}); // 狂战套装 (50级蓝)
        EQUIP_BOX_MAP.put(15212, new int[]{23081,23082,23083,23084,23085,23086}); // 天狼套装 (60级蓝)
        EQUIP_BOX_MAP.put(15213, new int[]{24091,24092,24093,24094,24095,24096}); // 破军套装 (60级紫)
        EQUIP_BOX_MAP.put(15214, new int[]{24101,24102,24103,24104,24105,24106}); // 龙威套装 (80级紫)
        EQUIP_BOX_MAP.put(15215, new int[]{25111,25112,25113,25114,25115,25116}); // 战神套装 (80级橙)
        EQUIP_BOX_MAP.put(15221, new int[]{23091,23092,23093,23094,23095,23096}); // 征戎套装 (90级蓝)
        EQUIP_BOX_MAP.put(15222, new int[]{25181,25182,25183,25184,25185,25186}); // 诛邪套装 (90级橙)
    }

    private static final Map<Integer, String> EQUIP_BOX_NAME_MAP = new HashMap<>();
    static {
        EQUIP_BOX_NAME_MAP.put(15201,"黑铁装备"); EQUIP_BOX_NAME_MAP.put(15202,"精钢装备"); EQUIP_BOX_NAME_MAP.put(15203,"紫铜装备");
        EQUIP_BOX_NAME_MAP.put(15204,"亮银装备"); EQUIP_BOX_NAME_MAP.put(15205,"百炼装备");
        EQUIP_BOX_NAME_MAP.put(15206,"宣武套装"); EQUIP_BOX_NAME_MAP.put(15207,"折冲套装"); EQUIP_BOX_NAME_MAP.put(15208,"骁勇套装");
        EQUIP_BOX_NAME_MAP.put(15209,"破俘套装"); EQUIP_BOX_NAME_MAP.put(15210,"陷阵套装"); EQUIP_BOX_NAME_MAP.put(15211,"狂战套装");
        EQUIP_BOX_NAME_MAP.put(15212,"天狼套装"); EQUIP_BOX_NAME_MAP.put(15213,"破军套装"); EQUIP_BOX_NAME_MAP.put(15214,"龙威套装");
        EQUIP_BOX_NAME_MAP.put(15215,"战神套装"); EQUIP_BOX_NAME_MAP.put(15221,"征戎套装"); EQUIP_BOX_NAME_MAP.put(15222,"诛邪套装");
    }

    /**
     * APK 战役章节配置 (CampaignShow_cfg.json)
     * 每个章节的装备宝箱掉落和道具掉落
     */
    private static final int[][] APK_CHAPTER_EQUIP_IDS = {
        {15201, 15001, 11104},                     // Ch1 黄巾之乱: 黑铁装备, 初级合成符
        {15201, 15202, 15001, 11001, 11104},       // Ch2 江东诸侯战董卓: 黑铁, 青铜, 初级合成符, 初级声望符
        {15202, 15206, 11001, 15042, 11104},       // Ch3 乱世群雄: 青铜, 宣武, 初级声望符, 特训符
        {15202, 15206, 15210, 15052, 11104},       // Ch4 威震中原: 青铜, 宣武, 陷阵, 军需令
        {15203, 15207, 15211, 15022, 11104},       // Ch5 西凉铁骑: 暗铁, 折冲, 狂战, 鬼谷兵法
        {15208, 15212, 15213, 15023, 11104},       // Ch6 四世三公: 骁勇, 天狼, 破军, 太公六韬
        {15209, 15214, 15215, 15024, 11104},       // Ch7 战神吕布: 破俘, 龙威, 战神, 孙子兵法
        {11104, 15221, 15024, 15222},       // Ch8 征讨乌桓: 招财符, 征戎, 孙子兵法, 诛邪
    };

    private static int[] getEquipPreIdsForChapter(int chapterOrder) {
        if (chapterOrder < 1 || chapterOrder > APK_CHAPTER_EQUIP_IDS.length) return new int[0];
        int[] rawIds = APK_CHAPTER_EQUIP_IDS[chapterOrder - 1];
        List<Integer> result = new ArrayList<>();
        for (int boxId : rawIds) {
            int[] preIds = EQUIP_BOX_MAP.get(boxId);
            if (preIds != null) {
                for (int id : preIds) result.add(id);
            }
        }
        return result.stream().mapToInt(Integer::intValue).toArray();
    }

    private static final Map<Integer, String[]> APK_PROP_INFO = new HashMap<>();
    static {
        APK_PROP_INFO.put(15201, new String[]{"黑铁装备",   "images/item/15201.jpg"});
        APK_PROP_INFO.put(15202, new String[]{"精钢装备",   "images/item/15202.jpg"});
        APK_PROP_INFO.put(15203, new String[]{"紫铜装备",   "images/item/15203.jpg"});
        APK_PROP_INFO.put(15204, new String[]{"亮银装备",   "images/item/15204.jpg"});
        APK_PROP_INFO.put(15205, new String[]{"百炼装备",   "images/item/15205.jpg"});
        APK_PROP_INFO.put(15206, new String[]{"宣武套装",   "images/item/15206.jpg"});
        APK_PROP_INFO.put(15207, new String[]{"折冲套装",   "images/item/15207.jpg"});
        APK_PROP_INFO.put(15208, new String[]{"骁勇套装",   "images/item/15208.jpg"});
        APK_PROP_INFO.put(15209, new String[]{"破俘套装",   "images/item/15209.jpg"});
        APK_PROP_INFO.put(15210, new String[]{"陷阵套装",   "images/item/15210.jpg"});
        APK_PROP_INFO.put(15211, new String[]{"狂战套装",   "images/item/15211.jpg"});
        APK_PROP_INFO.put(15212, new String[]{"天狼套装",   "images/item/15212.jpg"});
        APK_PROP_INFO.put(15213, new String[]{"破军套装",   "images/item/15213.jpg"});
        APK_PROP_INFO.put(15214, new String[]{"龙威套装",   "images/item/15214.jpg"});
        APK_PROP_INFO.put(15215, new String[]{"战神套装",   "images/item/15215.jpg"});
        APK_PROP_INFO.put(15221, new String[]{"征戎套装",   "images/item/15221.jpg"});
        APK_PROP_INFO.put(15222, new String[]{"诛邪套装",   "images/item/15222.jpg"});
        APK_PROP_INFO.put(15001, new String[]{"初级合成符", "images/item/15001.jpg"});
        APK_PROP_INFO.put(11001, new String[]{"初级声望符", "images/item/11001.jpg"});
        APK_PROP_INFO.put(15042, new String[]{"特训符",     "images/item/15042.jpg"});
        APK_PROP_INFO.put(15052, new String[]{"军需令",     "images/item/15052.jpg"});
        APK_PROP_INFO.put(15022, new String[]{"鬼谷兵法",   "images/item/15022.jpg"});
        APK_PROP_INFO.put(15023, new String[]{"太公六韬",   "images/item/15023.jpg"});
        APK_PROP_INFO.put(15024, new String[]{"孙子兵法",   "images/item/15024.jpg"});
        APK_PROP_INFO.put(11104, new String[]{"招财符",     "images/item/11104.jpg"});
    }

    private static List<Campaign.DropPreview> getDropPreviewsForChapter(int chapterOrder) {
        if (chapterOrder < 1 || chapterOrder > APK_CHAPTER_EQUIP_IDS.length) return new ArrayList<>();
        int[] rawIds = APK_CHAPTER_EQUIP_IDS[chapterOrder - 1];
        List<Campaign.DropPreview> list = new ArrayList<>();
        for (int id : rawIds) {
            String[] info = APK_PROP_INFO.get(id);
            if (info != null) {
                list.add(Campaign.DropPreview.builder()
                    .name(info[0]).icon(info[1])
                    .quality(EQUIP_BOX_MAP.containsKey(id) ? "装备" : "道具")
                    .build());
            }
        }
        return list;
    }

    /**
     * APK MonsterShow_cfg.json BOSS掉落表
     * 只有特定BOSS关才有装备掉落, 普通关不掉装备(只掉白银和道具)
     * key: 章节order_关卡序号(从1开始), value: [装备包ID, 概率档位(0=默认高, 1=大概率, 2=小概率)]
     */
    private static final Map<String, int[]> APK_BOSS_DROP_MAP = new HashMap<>();
    static {
        // 第1章 黄巾之乱 (20关) - MonsterShow 101-120
        APK_BOSS_DROP_MAP.put("1_10", new int[]{15201, 0});   // 张宝 Lv5  → 黑铁装备
        APK_BOSS_DROP_MAP.put("1_20", new int[]{15201, 0});   // 张角 Lv10 → 黑铁装备
        // 第2章 江东诸侯战董卓 (20关) - MonsterShow 201-220
        APK_BOSS_DROP_MAP.put("2_5",  new int[]{15201, 1});   // 徐荣 Lv13 → 黑铁 大概率
        APK_BOSS_DROP_MAP.put("2_10", new int[]{15201, 1});   // 华雄 Lv15 → 黑铁 大概率
        APK_BOSS_DROP_MAP.put("2_15", new int[]{15201, 1});   // 牛辅 Lv18 → 黑铁 大概率
        APK_BOSS_DROP_MAP.put("2_20", new int[]{15202, 1});   // 董卓 Lv20 → 精钢 大概率
        // 第3章 乱世群雄 (20关) - MonsterShow 301-320
        APK_BOSS_DROP_MAP.put("3_10", new int[]{15201, 0});   // 陶谦 Lv25 → 黑铁装备
        APK_BOSS_DROP_MAP.put("3_20", new int[]{15206, 0});   // 刘表 Lv30 → 宣武套装
        // 第4章 威震中原 (20关) - MonsterShow 341-360
        APK_BOSS_DROP_MAP.put("4_10", new int[]{15206, 0});   // 公孙续 Lv35 → 宣武套装
        APK_BOSS_DROP_MAP.put("4_20", new int[]{15210, 0});   // 公孙瓒 Lv40 → 陷阵套装
        // 第5章 西凉铁骑 (20关) - MonsterShow 371-390
        APK_BOSS_DROP_MAP.put("5_10", new int[]{15203, 0});   // 庞德 Lv44 → 紫铜装备
        APK_BOSS_DROP_MAP.put("5_15", new int[]{15207, 0});   // 马超 Lv47 → 折冲套装
        APK_BOSS_DROP_MAP.put("5_20", new int[]{15211, 0});   // 马腾 Lv50 → 狂战套装
        // 第6章 四世三公 (25关) - MonsterShow 401-425
        APK_BOSS_DROP_MAP.put("6_5",  new int[]{15204, 1});   // 麴义 Lv44 → 亮银 大概率
        APK_BOSS_DROP_MAP.put("6_10", new int[]{15208, 2});   // 张颌 Lv50 → 骁勇 小概率
        APK_BOSS_DROP_MAP.put("6_20", new int[]{15212, 2});   // 颜良 Lv55 → 天狼 小概率
        APK_BOSS_DROP_MAP.put("6_25", new int[]{15213, 2});   // 袁绍 Lv60 → 破军 小概率
        // 第7章 战神吕布 (25关) - MonsterShow 501-525
        APK_BOSS_DROP_MAP.put("7_5",  new int[]{15205, 1});   // 宋宪 Lv64 → 百炼 大概率
        APK_BOSS_DROP_MAP.put("7_10", new int[]{15209, 2});   // 曹性 Lv69 → 破俘 小概率
        APK_BOSS_DROP_MAP.put("7_20", new int[]{15214, 2});   // 张辽 Lv75 → 龙威 小概率
        APK_BOSS_DROP_MAP.put("7_25", new int[]{15215, 2});   // 吕布 Lv80 → 战神 小概率
        // 第8章 征讨乌桓 (20关) - MonsterShow 601-620
        APK_BOSS_DROP_MAP.put("8_10", new int[]{15221, 0});   // 丘力居 Lv90 → 征戎套装
        APK_BOSS_DROP_MAP.put("8_20", new int[]{15222, 0});   // 蹋顿 Lv100 → 诛邪套装
    }

    /**
     * 根据APK概率档位返回实际掉率
     * 0=默认(70%), 1=大概率(55%), 2=小概率(20%)
     */
    private static int apkDropProbToRate(int prob) {
        switch (prob) {
            case 0: return 30;
            case 1: return 30;
            case 2: return 15;
            default: return 25;
        }
    }

    /**
     * 从 APK_CHAPTER_EQUIP_IDS 中提取非装备道具（装备宝箱由 APK_BOSS_DROP_MAP 处理）
     */
    private static int[] getItemPoolForChapter(int chapterOrder) {
        if (chapterOrder < 1 || chapterOrder > APK_CHAPTER_EQUIP_IDS.length) return new int[0];
        int[] rawIds = APK_CHAPTER_EQUIP_IDS[chapterOrder - 1];
        List<Integer> items = new ArrayList<>();
        for (int id : rawIds) {
            if (!EQUIP_BOX_MAP.containsKey(id)) {
                items.add(id);
            }
        }
        int[] result = new int[items.size()];
        for (int i = 0; i < items.size(); i++) result[i] = items.get(i);
        return result;
    }

    /** APK PropShow.json 道具名称映射 */
    private static final Map<Integer, String> ITEM_NAME_MAP = new HashMap<>();
    static {
        ITEM_NAME_MAP.put(15001,"初级合成符"); ITEM_NAME_MAP.put(15002,"中级合成符");
        ITEM_NAME_MAP.put(15003,"高级合成符"); ITEM_NAME_MAP.put(15004,"特级合成符");
        ITEM_NAME_MAP.put(15022,"鬼谷兵法"); ITEM_NAME_MAP.put(15023,"太公六韬"); ITEM_NAME_MAP.put(15024,"孙子兵法");
        ITEM_NAME_MAP.put(11001,"初级声望符"); ITEM_NAME_MAP.put(11104,"招财符");
        ITEM_NAME_MAP.put(15042,"特训符"); ITEM_NAME_MAP.put(15052,"军需令");
    }

    private static String getEquipBoxNameByPreId(int equipPreId) {
        for (Map.Entry<Integer, int[]> e : EQUIP_BOX_MAP.entrySet()) {
            for (int id : e.getValue()) {
                if (id == equipPreId) return EQUIP_BOX_NAME_MAP.getOrDefault(e.getKey(), "装备");
            }
        }
        return "装备";
    }

    /**
     * 初始化战役配置 - 8个战役匹配APK MonsterShow_cfg.json + CampaignShow_cfg.json
     * 所有战役每日挑战上限5次，每次扣精力；战役内最多失败3次（对应3次重生）
     *
     *  1. 黄巾之乱           Lv1-10    20关  体力3  解锁Lv1   MonsterShow 101-120
     *  2. 江东诸侯战董卓     Lv10-20   20关  体力5  解锁Lv10  MonsterShow 201-220
     *  3. 乱世群雄           Lv20-30   20关  体力8  解锁Lv20  MonsterShow 301-320
     *  4. 威震中原           Lv30-40   20关  体力8  解锁Lv30  MonsterShow 341-360
     *  5. 西凉铁骑           Lv40-50   20关  体力8  解锁Lv40  MonsterShow 371-390
     *  6. 四世三公           Lv40-60   25关  体力10 解锁Lv50  MonsterShow 401-425
     *  7. 战神吕布           Lv60-80   25关  体力10 解锁Lv70  MonsterShow 501-525
     *  8. 征讨乌桓           Lv80-100  20关  体力10 解锁Lv80  MonsterShow 601-620
     */
    private void initCampaignConfigs() {

        // ===== 战役1: 黄巾之乱 (APK Ch1, 20关) =====
        regApk("campaign_huangjin","黄巾之乱",
            "苍天已死，黄天当立，岁在甲子，天下大吉。黄巾军起事不足一月，战火蔓延全国七州二十八郡，势如破竹，州郡失守、吏士逃亡，震动京都。",
            1,10,1,5,3,1, 20,false,"黄巾","huanJ.png",
            new String[]{"黄巾斥候","黄巾前哨","黄巾前哨","管亥","黄巾先锋","黄巾先锋","裴元绍","邓茂","高升","张宝",
                          "龚都","黄巾中军","马元义","黄巾中军","张梁","张济","波才","黄巾卫队","黄巾卫队","张角"},
            new String[]{"步","步","弓","步","步","弓","骑","步","骑","弓",
                          "骑","步","步","弓","步","骑","弓","步","骑","弓"});

        // ===== 战役2: 江东诸侯战董卓 (APK Ch2, 20关) =====
        regApk("campaign_dongzhuo","江东诸侯战董卓",
            "董卓挟天子以令诸侯，窃据高位而祸乱朝政，倒行逆施而残杀忠良，天下人群情激奋，江东诸侯起兵共讨国贼！",
            10,20,10,5,5,2, 20,false,"西凉","jianD.png",
            new String[]{"西凉前锋","西凉前锋","西凉前锋","樊稠","徐荣","西凉左翼","西凉左翼","胡轸","郭汜","华雄",
                          "西凉右翼","西凉右翼","李傕","李儒","牛辅","张济","杨奉","西凉卫队","西凉卫队","董卓"},
            new String[]{"步","骑","弓","骑","步","步","弓","骑","步","步",
                          "骑","步","骑","弓","步","骑","步","步","骑","步"});

        // ===== 战役3: 乱世群雄 (APK Ch3, 20关) =====
        regApk("campaign_luanshi","乱世群雄",
            "群雄逐鹿，各据一方。陶谦坐拥徐州而不善兵，刘表雄踞荆州坐观天下。谁能讨灭诸侯，一统山河？",
            20,30,20,5,8,3, 20,true,"刘军","luanS.png",
            new String[]{"前哨部队","张闿","前锋部队","曹豹","陈登","中军部队","后军部队","臧霸","陶军卫队","陶谦",
                          "刘军先锋","黄祖","中军部队","文聘","后军部队","甘宁","刘军卫队","刘军卫队","蔡瑁","刘表"},
            new String[]{"步","步","骑","骑","弓","步","弓","步","步","弓",
                          "步","弓","步","步","弓","骑","步","骑","弓","弓"});

        // ===== 战役4: 威震中原 (APK Ch4, 20关) =====
        regApk("campaign_weizhong","威震中原",
            "公孙瓒据幽州，拥白马义从天下无双。破其铁骑，方能威震中原！",
            30,40,30,5,8,4, 20,true,"公孙","weiZ.png",
            new String[]{"前哨部队","前哨部队","公孙越","前军部队","严纲","中军部队","中军部队","中军部队","公孙范","公孙续",
                          "后军部队","后军部队","后军部队","田楷","田豫","白马义从","白马义从","公孙豹","公孙度","公孙瓒"},
            new String[]{"步","弓","骑","步","步","骑","步","弓","骑","骑",
                          "步","弓","骑","步","步","骑","骑","骑","步","骑"});

        // ===== 战役5: 西凉铁骑 (APK Ch5, 20关) =====
        regApk("campaign_xiliang","西凉铁骑",
            "西凉铁骑天下无敌，马腾、马超父子横扫关中，锐不可当！",
            40,50,40,5,8,5, 20,true,"西凉","xiL.png",
            new String[]{"西凉前锋","西凉前锋","西凉前锋","成宜","马铁","中军部队","中军部队","中军部队","庞柔","庞德",
                          "后军部队","后军部队","后军部队","马玩","马超","西凉卫队","西凉卫队","马岱","马休","马腾"},
            new String[]{"骑","步","弓","骑","骑","步","骑","弓","步","骑",
                          "步","骑","弓","骑","骑","骑","步","骑","骑","骑"});

        // ===== 战役6: 四世三公 (APK Ch6, 25关) =====
        regApk("campaign_sisisan","四世三公",
            "袁绍出身汝南望族袁氏，一门四世三公，门生故吏遍天下，据有冀、青、幽、并四州，即将成为北方霸主。",
            40,60,50,5,10,6, 25,true,"袁军","siS.png",
            new String[]{"袁军前锋","袁军前锋","袁军前锋","袁尚","麴义","左翼部队","左翼部队","左翼部队","沮授","张郃",
                          "右翼部队","右翼部队","右翼部队","淳于琼","中军部队","审配","中军部队","文丑","高览","颜良",
                          "袁军卫队","袁军卫队","袁军卫队","田丰","袁绍"},
            new String[]{"步","骑","弓","弓","步","步","骑","弓","弓","步",
                          "步","骑","弓","步","步","弓","骑","骑","步","骑",
                          "步","骑","弓","弓","骑"});

        // ===== 战役7: 战神吕布 (APK Ch7, 25关) =====
        regApk("campaign_lvbu","战神吕布",
            "吕布，字奉先，以勇武著称，熟习弓马，膂力过人，使一枝方天画戟，箭法高超，乃三国第一猛将，号称飞将军，又称战神。",
            60,80,70,5,10,7, 25,true,"吕布","zhanS.png",
            new String[]{"吕军先锋","吕军先锋","吕军先锋","成廉","宋宪","左翼部队","左翼部队","左翼部队","李肃","曹性",
                          "右翼部队","右翼部队","右翼部队","魏续","中军部队","中军部队","中军部队","高顺","侯成","张辽",
                          "吕军卫队","吕军卫队","吕军卫队","陈宫","吕布"},
            new String[]{"步","骑","弓","骑","步","步","骑","弓","弓","弓",
                          "步","骑","弓","步","步","骑","弓","步","骑","骑",
                          "步","骑","弓","弓","骑"});

        // ===== 战役8: 征讨乌桓 (APK Ch8, 20关) =====
        regApk("campaign_wuhuan","征讨乌桓",
            "辽西乌桓寇略青、徐、幽、冀四州，杀略吏民。今率大军征讨，卫土安民。",
            80,100,80,5,10,8, 20,true,"乌桓","zhengT.png",
            new String[]{"乌桓斥候","乌桓斥候","乌桓前锋","乌桓前锋","苏仆延","左翼部队","左翼部队","中军部队","中军部队","丘力居",
                          "右翼部队","右翼部队","右翼部队","乌桓骑兵","乌延","乌桓勇士","乌桓勇士","首领卫队","首领卫队","蹋顿"},
            new String[]{"步","弓","骑","步","骑","骑","弓","步","骑","步",
                          "步","骑","弓","骑","骑","步","骑","步","骑","骑"});

        log.info("战役配置初始化完成，共{}个战役", campaignConfigs.size());
    }

    private void regApk(String id, String name, String desc,
                        int lvMin, int lvMax, int reqLv,
                        int dailyLimit, int staminaCost, int order,
                        int stageCount, boolean fullFormation, String faction,
                        String namePic,
                        String[] generals, String[] troopTypes) {

        int[] equipIds = getEquipPreIdsForChapter(order);
        int[] itemIds = getItemPoolForChapter(order);
        List<Campaign.DropPreview> dropPreviews = getDropPreviewsForChapter(order);

        int baseExp = calcBaseExp(lvMin, lvMax);
        long baseSilver = calcBaseSilver(lvMin, lvMax);

        Campaign campaign = Campaign.builder()
                .id(id).name(name).description(desc)
                .icon("images/battle/" + namePic)
                .backgroundImage("images/ui/campaign/camp_bg" + order + "B.jpg")
                .enemyLevelMin(lvMin).enemyLevelMax(lvMax)
                .expRewardMin(baseExp).expRewardMax(baseExp + (lvMax - lvMin) * 30)
                .dailyLimit(dailyLimit).staminaCost(staminaCost)
                .requiredLevel(reqLv).order(order)
                .stages(buildStages(generals, troopTypes, id.replace("campaign_", ""),
                        lvMin, lvMax, stageCount, fullFormation, faction,
                        baseExp, baseSilver, equipIds, itemIds, order))
                .dropPreviews(dropPreviews)
                .build();
        campaignConfigs.put(id, campaign);
    }

    private int calcBaseExp(int lvMin, int lvMax) {
        if (lvMax <= 10) return 300;
        if (lvMax <= 20) return 1200;
        if (lvMax <= 30) return 2000;
        if (lvMax <= 40) return 3000;
        if (lvMax <= 50) return 4000;
        if (lvMax <= 60) return 6000;
        if (lvMax <= 80) return 8000;
        return 25000;
    }

    private long calcBaseSilver(int lvMin, int lvMax) {
        if (lvMax <= 10) return 100;
        if (lvMax <= 20) return 300;
        if (lvMax <= 30) return 500;
        if (lvMax <= 40) return 800;
        if (lvMax <= 50) return 1200;
        if (lvMax <= 60) return 2000;
        if (lvMax <= 80) return 3000;
        return 5000;
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
        return buildStages(generals, troopTypes, prefix, minLv, maxLv, stageCount,
                fullFormation, faction, baseExp, baseSilver, equipPreIds, itemDropIds, 0);
    }

    private List<Campaign.Stage> buildStages(
            String[] generals, String[] troopTypes, String prefix,
            int minLv, int maxLv, int stageCount,
            boolean fullFormation, String faction,
            int baseExp, long baseSilver,
            int[] equipPreIds, int[] itemDropIds,
            int chapterOrder) {

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

            int gIdx = (i - 1) % generals.length;
            String mainGeneral = generals[gIdx];
            String troopType = troopTypes[gIdx];

            // APK风格: NPC属性 = NPC士兵属性(20xxx) + 阵型加成(低级NPC缩减), 无武将四维
            int npcFormationLevel = BattleCalculator.levelToFormationLevel(npcLevel);
            int npcTier = npcFormationLevel;
            int npcTroopInt = BattleCalculator.parseTroopType(troopType);
            int[] npcSoldier = BattleCalculator.getNpcSoldierStats(npcTroopInt, npcTier);
            int npcSoldiers = BattleCalculator.getFormationMaxPeople(npcFormationLevel);
            double npcFormScale = npcLevel <= 5 ? 0.15 : (npcLevel <= 10 ? 0.3 : (npcLevel <= 20 ? 0.7 : 1.0));
            int npcValor = 0, npcCommand = 0, npcDodge = npcSoldier[5], npcMobility = npcSoldier[3];

            if (isBoss) {
                npcValor = Math.min(200, npcLevel * 2);
                npcCommand = Math.min(200, npcLevel * 2);
                npcSoldiers = (int)(npcSoldiers * 1.3);
            }

            int npcCount;
            if (fullFormation) {
                npcCount = 6;
            } else if (npcLevel <= 5) {
                npcCount = 1;
            } else if (npcLevel <= 8) {
                npcCount = 2;
            } else if (npcLevel <= 10) {
                npcCount = 3;
            } else if (npcLevel <= 20) {
                npcCount = Math.min(4, 3 + (i - 1) / Math.max(1, stageCount / 3));
            } else if (npcLevel <= 40) {
                npcCount = Math.min(5, 4 + (i - 1) / Math.max(1, stageCount / 3));
            } else {
                npcCount = Math.min(6, 5 + (i - 1) / Math.max(1, stageCount / 3));
            }
            if (isBoss) {
                npcCount = Math.min(6, npcCount + 1);
            }
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
                int fTroopInt = BattleCalculator.parseTroopType(tt);
                int fFormLv = BattleCalculator.levelToFormationLevel(lv);
                int ft = fFormLv;
                int[] fSoldier = BattleCalculator.getNpcSoldierStats(fTroopInt, ft);
                double fScale = lv <= 5 ? 0.15 : (lv <= 10 ? 0.3 : (lv <= 20 ? 0.7 : 1.0));
                int fa = fSoldier[1] + (int)(BattleCalculator.getFormationAddAtt(fFormLv) * fScale);
                int fd = fSoldier[2] + (int)(BattleCalculator.getFormationAddDef(fFormLv) * fScale);
                int fs = BattleCalculator.getFormationMaxPeople(fFormLv);
                int fValor = 0, fCommand = 0;
                if (boss) {
                    fValor = Math.min(200, lv * 2);
                    fCommand = Math.min(200, lv * 2);
                    fs = (int)(fs * 1.3);
                }

                formation.add(Campaign.StageNpc.builder()
                        .position(pos).name(nm).avatar(getNpcPortrait(nm, faction))
                        .level(lv).troopType(tt).soldierCount(fs).soldierTier(ft)
                        .attack(fa).defense(fd).valor(fValor).command(fCommand)
                        .dodge(fSoldier[5]).mobility(fSoldier[3])
                        .hp(300 + lv * 100 + (boss ? lv * 50 : 0)).isBoss(boss).build());
            }

            // 掉落: 白银(所有关) + APK BOSS专属装备掉落 + item道具
            List<Campaign.StageDrop> drops = new ArrayList<>();
            drops.add(Campaign.StageDrop.builder()
                    .type("RESOURCE").itemId("silver").itemName("白银")
                    .dropRate(100).minCount(50 * i).maxCount(100 * i).build());

            // APK风格: 只有特定BOSS才有装备掉落
            String bossKey = chapterOrder + "_" + i;
            int[] bossDrop = APK_BOSS_DROP_MAP.get(bossKey);
            if (bossDrop != null) {
                // 该关卡在APK中有装备掉落
                int dropBoxId = bossDrop[0];
                int dropProb = bossDrop[1];
                int dropRate = apkDropProbToRate(dropProb);
                String boxName = EQUIP_BOX_NAME_MAP.getOrDefault(dropBoxId, "装备");

                // 从装备箱对应的equipment_pre中随机选一个
                int[] boxPreIds = EQUIP_BOX_MAP.get(dropBoxId);
                if (boxPreIds != null && boxPreIds.length > 0) {
                    int equipId = boxPreIds[rng.nextInt(boxPreIds.length)];
                    drops.add(Campaign.StageDrop.builder()
                            .type("EQUIP_PRE").equipPreId(equipId)
                            .itemId(String.valueOf(equipId)).itemName(boxName)
                            .dropRate(dropRate).minCount(1).maxCount(1).build());
                }
            }
            // 非APK配置的关卡（包括BOSS关）不掉落装备

            if (itemDropIds.length > 0) {
                int pick = 1 + rng.nextInt(2);
                Set<Integer> picked = new HashSet<>();
                for (int n = 0; n < pick && picked.size() < itemDropIds.length; n++) {
                    int itemId = itemDropIds[rng.nextInt(itemDropIds.length)];
                    if (picked.add(itemId)) {
                        String iName = ITEM_NAME_MAP.getOrDefault(itemId, "道具");
                        drops.add(Campaign.StageDrop.builder()
                                .type("ITEM").itemId(String.valueOf(itemId)).itemName(iName)
                                .dropRate(isBoss ? 25 : 10)
                                .minCount(1).maxCount(isBoss ? 3 : 2).build());
                    }
                }
            }

            stages.add(Campaign.Stage.builder()
                    .id(prefix + "_stage_" + i).stageNum(i)
                    .name(isBoss ? "BOSS: " + mainGeneral : "第" + i + "关")
                    .enemyGeneralName(mainGeneral)
                    .enemyGeneralIcon(getNpcPortrait(mainGeneral, faction))
                    .enemyLevel(npcLevel).enemyTroops(npcSoldiers)
                    .enemyAttack(0).enemyDefense(0)
                    .enemyValor(npcValor).enemyCommand(npcCommand)
                    .enemyDodge(npcDodge).enemyMobility(npcMobility)
                    .enemySoldierTier(npcTier).enemyFormationLevel(npcFormationLevel)
                    .enemyTroopType(troopType)
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
        int userLevel = levelService.getUserLevel(odUserId).getLevel();
        
        String today = new SimpleDateFormat("yyyyMMdd").format(new Date());
        
        campaignConfigs.values().stream()
                .sorted(Comparator.comparingInt(Campaign::getOrder))
                .forEach(campaign -> {
                    Map<String, Object> campaignInfo = new HashMap<>();
                    campaignInfo.put("id", campaign.getId());
                    campaignInfo.put("name", campaign.getName());
                    campaignInfo.put("description", campaign.getDescription());
                    campaignInfo.put("icon", campaign.getIcon());
                    campaignInfo.put("backgroundImage", campaign.getBackgroundImage());
                    campaignInfo.put("enemyLevelMin", campaign.getEnemyLevelMin());
                    campaignInfo.put("enemyLevelMax", campaign.getEnemyLevelMax());
                    campaignInfo.put("expRewardMin", campaign.getExpRewardMin());
                    campaignInfo.put("expRewardMax", campaign.getExpRewardMax());
                    campaignInfo.put("dailyLimit", campaign.getDailyLimit());
                    campaignInfo.put("staminaCost", campaign.getStaminaCost());
                    campaignInfo.put("requiredLevel", campaign.getRequiredLevel());
                    campaignInfo.put("dropPreviews", campaign.getDropPreviews());
                    campaignInfo.put("stageCount", campaign.getStages().size());
                    campaignInfo.put("order", campaign.getOrder());
                    
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
        int userLevel = levelService.getUserLevel(odUserId).getLevel();
        
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
        
        CampaignProgress progress = getOrCreateProgress(odUserId, campaignId);
        String curStatus = progress.getStatus();
        
        // 如果战役已经在进行中或暂停，不要重置进度，直接返回当前状态
        if ("IN_PROGRESS".equals(curStatus)) {
            log.info("startCampaign: 战役已在进行中, campaignId={}, stage={}", campaignId, progress.getCurrentStage());
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("progress", progress);
            result.put("campaign", campaign);
            int si = Math.min(progress.getCurrentStage(), campaign.getStages().size()) - 1;
            result.put("currentStage", campaign.getStages().get(Math.max(0, si)));
            return result;
        }
        if ("PAUSED".equals(curStatus)) {
            log.info("startCampaign: 战役已暂停, 恢复进行, campaignId={}, stage={}", campaignId, progress.getCurrentStage());
            progress.setStatus("IN_PROGRESS");
            campaignRepository.save(progress);
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("progress", progress);
            result.put("campaign", campaign);
            int si = Math.min(progress.getCurrentStage(), campaign.getStages().size()) - 1;
            result.put("currentStage", campaign.getStages().get(Math.max(0, si)));
            return result;
        }
        
        UserResource resource = userResourceService.getUserResource(odUserId);
        int userLevel = levelService.getUserLevel(odUserId).getLevel();
        
        if (userLevel < campaign.getRequiredLevel()) {
            throw new BusinessException("君主等级不足");
        }
        
        // 检查精力
        if (resource.getStamina() < campaign.getStaminaCost()) {
            throw new BusinessException("精力不足");
        }
        
        // 检查今日挑战次数（仅发动战役消耗，进攻关卡不消耗；引导期间豁免）
        String today = new SimpleDateFormat("yyyyMMdd").format(new Date());
        if (!today.equals(progress.getTodayDate())) {
            progress.setTodayChallengeCount(0);
            progress.setTodayDate(today);
        }
        if (progress.getTodayChallengeCount() >= campaign.getDailyLimit() && !isInGuide(odUserId)) {
            throw new BusinessException("今日挑战次数已用完");
        }
        
        // 获取武将信息设置兵力
        log.info("startCampaign: odUserId={}, generalId={}", odUserId, generalId);
        General general = generalService.getGeneralById(generalId);
        if (general == null) {
            log.warn("generalId直接查询失败: {}, 尝试从阵型获取", generalId);
            try {
                List<String> fmIds = formationService.getFormationGeneralIds(odUserId);
                log.info("阵型武将IDs: {}", fmIds);
                for (String gid : fmIds) {
                    General g = generalService.getGeneralById(gid);
                    if (g != null) { general = g; break; }
                }
            } catch (Exception e) {
                log.warn("从阵型获取武将失败", e);
            }
            if (general == null) {
                List<General> all = generalService.getUserGenerals(odUserId);
                log.info("用户全部武将数量: {}, userId={}", all.size(), odUserId);
                if (!all.isEmpty()) general = all.get(0);
            }
            if (general == null) {
                throw new BusinessException("武将不存在(userId=" + odUserId + ",generalId=" + generalId + ")，请先招募武将");
            }
        }
        
        // 获取阵型中所有武将的总兵力（使用soldierMaxCount，满编出征）
        int troops = 0;
        try {
            List<String> fmIds = formationService.getFormationGeneralIds(odUserId);
            for (String gid : fmIds) {
                General g = generalService.getGeneralById(gid);
                if (g != null) {
                    int maxC = g.getSoldierMaxCount() != null ? g.getSoldierMaxCount() : 100;
                    int curC = g.getSoldierCount() != null ? g.getSoldierCount() : maxC;
                    troops += Math.max(curC, maxC);
                }
            }
        } catch (Exception e) {
            log.warn("计算阵型总兵力失败，使用单武将兵力", e);
        }
        if (troops <= 0) {
            int maxC = general.getSoldierMaxCount() != null ? general.getSoldierMaxCount() : 100;
            int curC = general.getSoldierCount() != null ? general.getSoldierCount() : maxC;
            troops = Math.max(curC, maxC);
        }
        
        // 扣除精力
        resource.setStamina(resource.getStamina() - campaign.getStaminaCost());
        userResourceService.saveUserResource(resource);

        dailyTaskService.incrementTask(odUserId, "campaign");
        
        // 更新进度（只有 IDLE/COMPLETED 时才从头开始）
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
                .reviveCount(progress.getReviveCount())
                .build();

        if (victory) {
            long expGained = stage.getExpReward() != null ? stage.getExpReward() : 0;
            long silverGained = stage.getSilverReward() != null ? stage.getSilverReward() : 0;
            result.setExpGained(expGained);
            result.setSilverGained(silverGained);

            Random dropRandom = new Random();
            List<CampaignProgress.DropItem> drops = processDrops(stage.getDrops(), dropRandom);
            result.setDrops(drops);

            try {
                List<String> fmGeneralIds = formationService.getFormationGeneralIds(odUserId);
                for (String gid : fmGeneralIds) {
                    if (gid != null) generalService.addGeneralExp(gid, expGained);
                }
            } catch (Exception e) {
                log.warn("阵型武将加经验异常, 降级为主将加经验", e);
                General general = generalService.getGeneralById(progress.getGeneralId());
                if (general != null) generalService.addGeneralExp(general.getId(), expGained);
            }

            Map<String, Object> lordInfo = levelService.addExp(odUserId, expGained, "战役战斗");
            result.setLordLevelInfo(lordInfo);

            UserResource resource = userResourceService.getUserResource(odUserId);
            resource.setSilver(resource.getSilver() + silverGained);
            userResourceService.saveUserResource(resource);

            for (CampaignProgress.DropItem drop : drops) {
                if ("EQUIP_PRE".equals(drop.getType())) {
                    createEquipmentFromDrop(odUserId, drop);
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
            progress.setReviveCount(Math.max(0, progress.getReviveCount() - 1));
            result.setReviveCount(progress.getReviveCount());
            if (progress.getReviveCount() <= 0) {
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
        
        // 构建玩家全阵型战斗单位
        List<BattleCalculator.BattleUnit> playerUnits = formationService.buildPlayerBattleUnits(odUserId);

        // 按 progress.currentTroops 等比缩减玩家兵力（跨关卡持续消耗）
        int totalMax = playerUnits.stream().mapToInt(u -> u.maxSoldierCount).sum();
        int campaignTroops = progress.getCurrentTroops();
        if (totalMax > 0 && campaignTroops < totalMax) {
            double ratio = (double) campaignTroops / totalMax;
            int distributed = 0;
            for (int pi = 0; pi < playerUnits.size(); pi++) {
                BattleCalculator.BattleUnit u = playerUnits.get(pi);
                if (pi < playerUnits.size() - 1) {
                    u.soldierCount = Math.max(0, (int)(u.maxSoldierCount * ratio));
                    distributed += u.soldierCount;
                } else {
                    u.soldierCount = Math.max(0, campaignTroops - distributed);
                }
            }
        }

        // 构建敌方全阵型单位（使用 formation 中所有 NPC）
        List<BattleCalculator.BattleUnit> enemyUnits = new ArrayList<>();
        List<Campaign.StageNpc> formation = stage.getFormation();
        if (formation != null && !formation.isEmpty()) {
            for (Campaign.StageNpc npc : formation) {
                int npcLv = npc.getLevel() != null ? npc.getLevel() : 1;
                int npcTroopType = BattleCalculator.parseTroopType(
                        npc.getTroopType() != null ? npc.getTroopType() : "步");
                int npcTier = npc.getSoldierTier() != null ? npc.getSoldierTier()
                        : BattleCalculator.levelToFormationLevel(npcLv);
                int npcFormLv = BattleCalculator.levelToFormationLevel(npcLv);
                int npcMaxSoldiers = npc.getSoldierCount() != null ? npc.getSoldierCount()
                        : BattleCalculator.getFormationMaxPeople(npcFormLv);

                BattleCalculator.BattleUnit eu = BattleCalculator.assembleNpcBattleUnit(
                        npc.getName(), npcLv, npcTroopType, npcTier,
                        npcMaxSoldiers, npcMaxSoldiers, npcFormLv,
                        npc.getValor() != null ? npc.getValor() : 0,
                        npc.getCommand() != null ? npc.getCommand() : 0);

                double fScale = npcLv <= 5 ? 0.15 : (npcLv <= 10 ? 0.3 : (npcLv <= 20 ? 0.7 : 1.0));
                if (fScale < 1.0) {
                    int cutAtt = (int)(BattleCalculator.getFormationAddAtt(npcFormLv) * (1.0 - fScale));
                    int cutDef = (int)(BattleCalculator.getFormationAddDef(npcFormLv) * (1.0 - fScale));
                    eu.totalAttack = Math.max(eu.totalAttack - cutAtt, 1);
                    eu.totalDefense = Math.max(eu.totalDefense - cutDef, 1);
                }
                eu.position = npc.getPosition() != null ? npc.getPosition() : enemyUnits.size();
                if (npc.getTacticsId() != null) {
                    eu.tacticsId = npc.getTacticsId();
                }
                enemyUnits.add(eu);
            }
        } else {
            // fallback: 无 formation 时用 stage 主将字段构建单个敌方单位
            int enemyLevel = stage.getEnemyLevel() != null ? stage.getEnemyLevel() : 1;
            int enemyTier = stage.getEnemySoldierTier() != null ? stage.getEnemySoldierTier()
                    : BattleCalculator.levelToFormationLevel(enemyLevel);
            int enemyFormLv = stage.getEnemyFormationLevel() != null ? stage.getEnemyFormationLevel()
                    : BattleCalculator.levelToFormationLevel(enemyLevel);
            int enemyTroopType = BattleCalculator.parseTroopType(
                    stage.getEnemyTroopType() != null ? stage.getEnemyTroopType() : "步");
            int enemyMaxSoldiers = BattleCalculator.getFormationMaxPeople(enemyFormLv);
            BattleCalculator.BattleUnit enemy = BattleCalculator.assembleNpcBattleUnit(
                    stage.getEnemyGeneralName(), enemyLevel,
                    enemyTroopType, enemyTier,
                    stage.getEnemyTroops() != null ? stage.getEnemyTroops() : enemyMaxSoldiers,
                    enemyMaxSoldiers, enemyFormLv,
                    stage.getEnemyValor() != null ? stage.getEnemyValor() : 0,
                    stage.getEnemyCommand() != null ? stage.getEnemyCommand() : 0);
            double eFormScale = enemyLevel <= 5 ? 0.15 : (enemyLevel <= 10 ? 0.3 : (enemyLevel <= 20 ? 0.7 : 1.0));
            if (eFormScale < 1.0) {
                int cutAtt = (int)(BattleCalculator.getFormationAddAtt(enemyFormLv) * (1.0 - eFormScale));
                int cutDef = (int)(BattleCalculator.getFormationAddDef(enemyFormLv) * (1.0 - eFormScale));
                enemy.totalAttack = Math.max(enemy.totalAttack - cutAtt, 1);
                enemy.totalDefense = Math.max(enemy.totalDefense - cutDef, 1);
            }
            enemy.position = 0;
            enemyUnits.add(enemy);
        }

        BattleService.BattleReport report = battleService.fight(playerUnits, enemyUnits, 20);

        List<String> battleLog = new ArrayList<>();
        battleLog.addAll(report.toBattleLog("我方", "敌方"));

        boolean victory = report.victoryA;
        int remainingTroops = playerUnits.stream().mapToInt(u -> Math.max(0, u.soldierCount)).sum();
        int troopsLost = campaignTroops - remainingTroops;
        
        String troopCat = playerUnits.size() > 0 ? "步" : "步";
        General leadGeneral = generalService.getGeneralById(progress.getGeneralId());
        if (leadGeneral != null && leadGeneral.getTroopType() != null) troopCat = leadGeneral.getTroopType();

        CampaignProgress.BattleResult result = CampaignProgress.BattleResult.builder()
                .victory(victory)
                .stageNum(progress.getCurrentStage())
                .remainingTroops(remainingTroops)
                .troopsLost(troopsLost)
                .battleLog(battleLog)
                .battleReport(report)
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
            
            try {
                List<String> fmGeneralIds = formationService.getFormationGeneralIds(odUserId);
                for (String gid : fmGeneralIds) {
                    if (gid != null) generalService.addGeneralExp(gid, expGained);
                }
            } catch (Exception e) {
                log.warn("阵型武将加经验异常, 降级为主将加经验", e);
                if (leadGeneral != null) generalService.addGeneralExp(leadGeneral.getId(), expGained);
            }

            Map<String, Object> lordInfo = levelService.addExp(odUserId, expGained, "战役战斗");
            result.setLordLevelInfo(lordInfo);
            
            // 更新资源
            UserResource resource = userResourceService.getUserResource(odUserId);
            resource.setSilver(resource.getSilver() + silverGained);
            userResourceService.saveUserResource(resource);
            
            // 处理装备掉落
            for (CampaignProgress.DropItem drop : drops) {
                if ("EQUIP_PRE".equals(drop.getType())) {
                    createEquipmentFromDrop(odUserId, drop);
                }
            }
            
            // 检查是否首次通关
            boolean isFirstClear = progress.getMaxClearedStage() < progress.getCurrentStage();
            result.setIsFirstClear(isFirstClear);
            
            // 更新进度
            progress.setCurrentTroops(remainingTroops);
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
            
            progress.setReviveCount(Math.max(0, progress.getReviveCount() - 1));
            result.setReviveCount(progress.getReviveCount());
            
            if (progress.getReviveCount() > 0) {
                battleLog.add(String.format("剩余容错次数: %d/3", progress.getReviveCount()));
            } else {
                progress.setStatus("IDLE");
                battleLog.add("容错次数已用完，战役结束");
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
        if (progress == null) {
            throw new BusinessException("战役未开始");
        }
        String pStatus = progress.getStatus();
        if (!"IN_PROGRESS".equals(pStatus) && !"PAUSED".equals(pStatus)) {
            throw new BusinessException("战役未开始(当前状态:" + pStatus + ")");
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
        Map<String, Object> result = new HashMap<>();
        if (progress == null) {
            result.put("success", true);
            result.put("message", "无进行中的战役");
            return result;
        }
        String status = progress.getStatus();
        if ("IN_PROGRESS".equals(status)) {
            progress.setStatus("PAUSED");
            campaignRepository.save(progress);
            result.put("success", true);
            result.put("message", "战役已暂停，进度已保存");
        } else {
            result.put("success", true);
            result.put("message", "战役当前状态: " + status);
        }
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
        
        // 阵型内所有武将获得经验
        try {
            List<String> fmGeneralIds = formationService.getFormationGeneralIds(odUserId);
            for (String gid : fmGeneralIds) {
                if (gid != null) generalService.addGeneralExp(gid, totalExp);
            }
        } catch (Exception e) {
            log.warn("扫荡: 阵型武将加经验异常, 降级为主将加经验", e);
            General general = generalService.getGeneralById(progress.getGeneralId());
            if (general != null) generalService.addGeneralExp(general.getId(), totalExp);
        }

        // 主公获得经验
        levelService.addExp(odUserId, totalExp, "战役扫荡");

        // 处理装备掉落
        for (CampaignProgress.DropItem drop : allDrops) {
            if ("EQUIP_PRE".equals(drop.getType())) {
                createEquipmentFromDrop(odUserId, drop);
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
    
    private boolean isInGuide(String odUserId) {
        try {
            int idx = odUserId.lastIndexOf('_');
            String rawUserId = idx > 0 ? odUserId.substring(0, idx) : odUserId;
            int serverId = idx > 0 ? Integer.parseInt(odUserId.substring(idx + 1)) : 1;
            StoryProgress sp = storyProgressMapper.findByUserAndServer(rawUserId, serverId);
            return sp == null || !Boolean.TRUE.equals(sp.getCompleted());
        } catch (Exception e) {
            return false;
        }
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
    
    private void createEquipmentFromDrop(String odUserId, CampaignProgress.DropItem drop) {
        try {
            int preId = Integer.parseInt(drop.getItemId());
            EquipmentPre pre = equipmentPreRepository.findById(preId);
            if (pre != null) {
                Equipment equipment = equipmentService.buildEquipmentFromPre(
                        odUserId, pre, "CAMPAIGN", "战役掉落");
                equipmentRepository.save(equipment);
                drop.setItemName(pre.getName());
                drop.setIcon(pre.getIconUrl());
            } else {
                log.warn("战役掉落装备模板未找到: preId={}", preId);
            }
        } catch (NumberFormatException e) {
            log.warn("战役掉落装备ID解析失败: itemId={}", drop.getItemId());
        }
    }
}
