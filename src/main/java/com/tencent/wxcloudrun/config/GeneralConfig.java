package com.tencent.wxcloudrun.config;

import org.springframework.stereotype.Component;
import java.util.*;

/**
 * 武将配置 - 包含三国名将和随机生成的将领
 */
@Component
public class GeneralConfig {

    // 品质定义
    public static final Map<String, Quality> QUALITIES = new LinkedHashMap<>();
    static {
        QUALITIES.put("orange", new Quality("orange", "橙色", "#ff8c00", 1.5, 2));
        QUALITIES.put("purple", new Quality("purple", "紫色", "#9932cc", 1.3, 1));
        QUALITIES.put("red", new Quality("red", "红色", "#dc143c", 1.2, 1));
        QUALITIES.put("blue", new Quality("blue", "蓝色", "#4169e1", 1.1, 0));
        QUALITIES.put("green", new Quality("green", "绿色", "#32cd32", 1.0, 0));
        QUALITIES.put("white", new Quality("white", "白色", "#808080", 0.9, 0));
    }

    // ==================== 三国名将 ====================
    
    // 橙色将领 - 顶级名将 (20位)
    public static final List<GeneralTemplate> ORANGE_GENERALS = Arrays.asList(
        // 魏国
        new GeneralTemplate("曹操", "orange", "魏", "统帅", 
            Arrays.asList(new Trait("attack", 350), new Trait("special", "兵法发动概率+25%"))),
        new GeneralTemplate("司马懿", "orange", "魏", "智将",
            Arrays.asList(new Trait("command", 400), new Trait("special", "计策伤害+30%"))),
        new GeneralTemplate("张辽", "orange", "魏", "猛将",
            Arrays.asList(new Trait("attack", 380), new Trait("valor", 50))),
        new GeneralTemplate("夏侯惇", "orange", "魏", "猛将",
            Arrays.asList(new Trait("defense", 300), new Trait("special", "受伤时反击概率+40%"))),
        new GeneralTemplate("典韦", "orange", "魏", "猛将",
            Arrays.asList(new Trait("attack", 400), new Trait("special", "暴击伤害+50%"))),
        new GeneralTemplate("许褚", "orange", "魏", "猛将",
            Arrays.asList(new Trait("valor", 60), new Trait("defense", 280))),
        
        // 蜀国
        new GeneralTemplate("刘备", "orange", "蜀", "统帅",
            Arrays.asList(new Trait("command", 380), new Trait("special", "全军士气+20%"))),
        new GeneralTemplate("关羽", "orange", "蜀", "猛将",
            Arrays.asList(new Trait("attack", 420), new Trait("special", "斩杀低血量敌人概率+30%"))),
        new GeneralTemplate("张飞", "orange", "蜀", "猛将",
            Arrays.asList(new Trait("valor", 70), new Trait("attack", 350))),
        new GeneralTemplate("诸葛亮", "orange", "蜀", "智将",
            Arrays.asList(new Trait("command", 450), new Trait("special", "兵法冷却-2回合"))),
        new GeneralTemplate("赵云", "orange", "蜀", "猛将",
            Arrays.asList(new Trait("attack", 380), new Trait("dodge", 25))),
        new GeneralTemplate("马超", "orange", "蜀", "猛将",
            Arrays.asList(new Trait("mobility", 15), new Trait("attack", 360))),
        new GeneralTemplate("黄忠", "orange", "蜀", "猛将",
            Arrays.asList(new Trait("attack", 390), new Trait("special", "远程攻击+40%"))),
        
        // 吴国
        new GeneralTemplate("孙权", "orange", "吴", "统帅",
            Arrays.asList(new Trait("command", 360), new Trait("special", "水战伤害+35%"))),
        new GeneralTemplate("周瑜", "orange", "吴", "智将",
            Arrays.asList(new Trait("command", 400), new Trait("special", "火攻伤害+50%"))),
        new GeneralTemplate("陆逊", "orange", "吴", "智将",
            Arrays.asList(new Trait("command", 380), new Trait("special", "伏击成功率+40%"))),
        new GeneralTemplate("甘宁", "orange", "吴", "猛将",
            Arrays.asList(new Trait("attack", 370), new Trait("special", "夜袭伤害+45%"))),
        new GeneralTemplate("太史慈", "orange", "吴", "猛将",
            Arrays.asList(new Trait("attack", 360), new Trait("dodge", 20))),
        
        // 群雄
        new GeneralTemplate("吕布", "orange", "群", "猛将",
            Arrays.asList(new Trait("attack", 500), new Trait("special", "单挑必胜"))),
        new GeneralTemplate("貂蝉", "orange", "群", "智将",
            Arrays.asList(new Trait("command", 350), new Trait("special", "魅惑敌将概率+35%")))
    );

    // 紫色将领 - 名将 (30位三国 + 100位随机)
    public static final List<GeneralTemplate> PURPLE_GENERALS = new ArrayList<>();
    static {
        // 三国紫色将领
        PURPLE_GENERALS.addAll(Arrays.asList(
            // 魏
            new GeneralTemplate("夏侯渊", "purple", "魏", "猛将", Arrays.asList(new Trait("mobility", 12))),
            new GeneralTemplate("徐晃", "purple", "魏", "猛将", Arrays.asList(new Trait("attack", 280))),
            new GeneralTemplate("张郃", "purple", "魏", "猛将", Arrays.asList(new Trait("defense", 250))),
            new GeneralTemplate("于禁", "purple", "魏", "统帅", Arrays.asList(new Trait("command", 260))),
            new GeneralTemplate("乐进", "purple", "魏", "猛将", Arrays.asList(new Trait("valor", 35))),
            new GeneralTemplate("曹仁", "purple", "魏", "统帅", Arrays.asList(new Trait("defense", 280))),
            new GeneralTemplate("曹洪", "purple", "魏", "猛将", Arrays.asList(new Trait("attack", 260))),
            new GeneralTemplate("荀彧", "purple", "魏", "智将", Arrays.asList(new Trait("command", 300))),
            new GeneralTemplate("郭嘉", "purple", "魏", "智将", Arrays.asList(new Trait("command", 320))),
            new GeneralTemplate("贾诩", "purple", "魏", "智将", Arrays.asList(new Trait("command", 290))),
            // 蜀
            new GeneralTemplate("魏延", "purple", "蜀", "猛将", Arrays.asList(new Trait("attack", 300))),
            new GeneralTemplate("姜维", "purple", "蜀", "智将", Arrays.asList(new Trait("command", 280))),
            new GeneralTemplate("庞统", "purple", "蜀", "智将", Arrays.asList(new Trait("command", 310))),
            new GeneralTemplate("法正", "purple", "蜀", "智将", Arrays.asList(new Trait("command", 270))),
            new GeneralTemplate("马岱", "purple", "蜀", "猛将", Arrays.asList(new Trait("mobility", 10))),
            new GeneralTemplate("严颜", "purple", "蜀", "猛将", Arrays.asList(new Trait("defense", 260))),
            new GeneralTemplate("黄权", "purple", "蜀", "智将", Arrays.asList(new Trait("command", 250))),
            // 吴
            new GeneralTemplate("黄盖", "purple", "吴", "猛将", Arrays.asList(new Trait("valor", 40))),
            new GeneralTemplate("程普", "purple", "吴", "统帅", Arrays.asList(new Trait("defense", 240))),
            new GeneralTemplate("韩当", "purple", "吴", "猛将", Arrays.asList(new Trait("attack", 250))),
            new GeneralTemplate("周泰", "purple", "吴", "猛将", Arrays.asList(new Trait("defense", 270))),
            new GeneralTemplate("凌统", "purple", "吴", "猛将", Arrays.asList(new Trait("attack", 260))),
            new GeneralTemplate("鲁肃", "purple", "吴", "智将", Arrays.asList(new Trait("command", 280))),
            new GeneralTemplate("吕蒙", "purple", "吴", "智将", Arrays.asList(new Trait("command", 290))),
            // 群
            new GeneralTemplate("董卓", "purple", "群", "统帅", Arrays.asList(new Trait("command", 260))),
            new GeneralTemplate("袁绍", "purple", "群", "统帅", Arrays.asList(new Trait("command", 240))),
            new GeneralTemplate("公孙瓒", "purple", "群", "猛将", Arrays.asList(new Trait("mobility", 11))),
            new GeneralTemplate("颜良", "purple", "群", "猛将", Arrays.asList(new Trait("attack", 290))),
            new GeneralTemplate("文丑", "purple", "群", "猛将", Arrays.asList(new Trait("attack", 285))),
            new GeneralTemplate("华雄", "purple", "群", "猛将", Arrays.asList(new Trait("valor", 38)))
        ));
        
        // 100位随机紫色将领
        String[] purpleNames = {
            "李云龙", "王铁柱", "张大锤", "刘铁牛", "陈虎", "赵猛", "孙烈", "周刚", "吴勇", "郑威",
            "钱雷", "孔武", "曹强", "严明", "韩锋", "杨勐", "朱豹", "秦雄", "尤龙", "许虎",
            "何熊", "吕鹰", "施彪", "张毅", "孟刚", "叶猛", "柳威", "谢烈", "鲁壮", "葛勇",
            "范雄", "彭虎", "郎铁", "鲍强", "史坚", "唐猛", "费雄", "薛刚", "雷震", "倪虎",
            "汤烈", "滕威", "殷勇", "罗猛", "毕雄", "郝刚", "邬虎", "安烈", "常威", "乐勇",
            "于猛", "时雄", "傅刚", "皮虎", "卞烈", "齐威", "康勇", "伍猛", "余雄", "元刚",
            "卜虎", "顾烈", "孟威", "平勇", "黄猛", "和雄", "穆刚", "萧虎", "尹烈", "姚威",
            "邵勇", "湛猛", "汪雄", "祁刚", "毛虎", "禹烈", "狄威", "米勇", "贝猛", "明雄",
            "臧刚", "计虎", "伏烈", "成威", "戴勇", "谈猛", "宋雄", "茅刚", "庞虎", "熊烈",
            "纪威", "舒勇", "屈猛", "项雄", "祝刚", "董虎", "梁烈", "杜威", "阮勇", "蓝猛"
        };
        for (String name : purpleNames) {
            PURPLE_GENERALS.add(new GeneralTemplate(name, "purple", "虚构", getRandomType(), 
                Arrays.asList(getRandomTrait("purple"))));
        }
    }

    // 红色将领 - 良将 (20位三国 + 50位随机)
    public static final List<GeneralTemplate> RED_GENERALS = new ArrayList<>();
    static {
        // 三国红色将领
        RED_GENERALS.addAll(Arrays.asList(
            new GeneralTemplate("李典", "red", "魏", "猛将", Arrays.asList(new Trait("attack", 200))),
            new GeneralTemplate("曹彰", "red", "魏", "猛将", Arrays.asList(new Trait("valor", 25))),
            new GeneralTemplate("曹真", "red", "魏", "统帅", Arrays.asList(new Trait("command", 180))),
            new GeneralTemplate("王朗", "red", "魏", "智将", Arrays.asList(new Trait("command", 160))),
            new GeneralTemplate("关平", "red", "蜀", "猛将", Arrays.asList(new Trait("attack", 190))),
            new GeneralTemplate("关兴", "red", "蜀", "猛将", Arrays.asList(new Trait("attack", 185))),
            new GeneralTemplate("张苞", "red", "蜀", "猛将", Arrays.asList(new Trait("valor", 22))),
            new GeneralTemplate("廖化", "red", "蜀", "猛将", Arrays.asList(new Trait("defense", 170))),
            new GeneralTemplate("王平", "red", "蜀", "统帅", Arrays.asList(new Trait("defense", 180))),
            new GeneralTemplate("蒋琬", "red", "蜀", "智将", Arrays.asList(new Trait("command", 170))),
            new GeneralTemplate("丁奉", "red", "吴", "猛将", Arrays.asList(new Trait("attack", 180))),
            new GeneralTemplate("徐盛", "red", "吴", "猛将", Arrays.asList(new Trait("defense", 175))),
            new GeneralTemplate("潘璋", "red", "吴", "猛将", Arrays.asList(new Trait("attack", 175))),
            new GeneralTemplate("朱然", "red", "吴", "统帅", Arrays.asList(new Trait("command", 165))),
            new GeneralTemplate("诸葛瑾", "red", "吴", "智将", Arrays.asList(new Trait("command", 175))),
            new GeneralTemplate("张角", "red", "群", "智将", Arrays.asList(new Trait("command", 190))),
            new GeneralTemplate("张宝", "red", "群", "智将", Arrays.asList(new Trait("command", 160))),
            new GeneralTemplate("张梁", "red", "群", "猛将", Arrays.asList(new Trait("attack", 170))),
            new GeneralTemplate("高顺", "red", "群", "猛将", Arrays.asList(new Trait("defense", 185))),
            new GeneralTemplate("张绣", "red", "群", "猛将", Arrays.asList(new Trait("mobility", 8)))
        ));
        
        // 50位随机红色将领
        String[] redNames = {
            "铁牛", "石头", "大壮", "二虎", "三猛", "四勇", "五刚", "六强", "七威", "八雄",
            "金刚", "银狼", "铜虎", "铁豹", "钢熊", "玉龙", "翠凤", "青鹏", "白鹤", "黑鹰",
            "东方明", "西门庆", "南宫适", "北冥鱼", "上官云", "中山狼", "下邳龙", "左丘明", "右贤王", "前将军",
            "烈火", "寒冰", "疾风", "雷电", "暴雨", "流星", "闪电", "惊雷", "狂风", "骤雨",
            "天罡", "地煞", "玄武", "朱雀", "青龙", "白虎", "麒麟", "凤凰", "貔貅", "饕餮"
        };
        for (String name : redNames) {
            RED_GENERALS.add(new GeneralTemplate(name, "red", "虚构", getRandomType(),
                Arrays.asList(getRandomTrait("red"))));
        }
    }

    // 蓝色将领 - 普通将领 (15位三国 + 20位随机)
    public static final List<GeneralTemplate> BLUE_GENERALS = new ArrayList<>();
    static {
        // 三国蓝色将领
        BLUE_GENERALS.addAll(Arrays.asList(
            new GeneralTemplate("曹休", "blue", "魏", "统帅", null),
            new GeneralTemplate("曹爽", "blue", "魏", "统帅", null),
            new GeneralTemplate("陈群", "blue", "魏", "智将", null),
            new GeneralTemplate("刘封", "blue", "蜀", "猛将", null),
            new GeneralTemplate("孟达", "blue", "蜀", "猛将", null),
            new GeneralTemplate("马谡", "blue", "蜀", "智将", null),
            new GeneralTemplate("费祎", "blue", "蜀", "智将", null),
            new GeneralTemplate("孙桓", "blue", "吴", "统帅", null),
            new GeneralTemplate("孙韶", "blue", "吴", "猛将", null),
            new GeneralTemplate("全琮", "blue", "吴", "猛将", null),
            new GeneralTemplate("步骘", "blue", "吴", "智将", null),
            new GeneralTemplate("袁术", "blue", "群", "统帅", null),
            new GeneralTemplate("刘表", "blue", "群", "统帅", null),
            new GeneralTemplate("刘璋", "blue", "群", "统帅", null),
            new GeneralTemplate("马腾", "blue", "群", "猛将", null)
        ));
        
        // 20位随机蓝色将领
        String[] blueNames = {
            "阿大", "阿二", "阿三", "阿四", "阿五", "大毛", "二毛", "三毛", "四毛", "五毛",
            "甲一", "乙二", "丙三", "丁四", "戊五", "己六", "庚七", "辛八", "壬九", "癸十"
        };
        for (String name : blueNames) {
            BLUE_GENERALS.add(new GeneralTemplate(name, "blue", "虚构", getRandomType(), null));
        }
    }

    // 绿色将领 - 普通兵
    public static final List<GeneralTemplate> GREEN_GENERALS = Arrays.asList(
        new GeneralTemplate("小兵甲", "green", "群", "猛将", null),
        new GeneralTemplate("小兵乙", "green", "群", "猛将", null),
        new GeneralTemplate("小兵丙", "green", "群", "猛将", null),
        new GeneralTemplate("哨兵", "green", "群", "猛将", null),
        new GeneralTemplate("斥候", "green", "群", "猛将", null),
        new GeneralTemplate("骑兵", "green", "群", "猛将", null),
        new GeneralTemplate("弓兵", "green", "群", "猛将", null),
        new GeneralTemplate("刀兵", "green", "群", "猛将", null),
        new GeneralTemplate("枪兵", "green", "群", "猛将", null),
        new GeneralTemplate("盾兵", "green", "群", "猛将", null)
    );

    // 白色将领 - 杂兵
    public static final List<GeneralTemplate> WHITE_GENERALS = Arrays.asList(
        new GeneralTemplate("村民甲", "white", "群", "普通", null),
        new GeneralTemplate("村民乙", "white", "群", "普通", null),
        new GeneralTemplate("农夫", "white", "群", "普通", null),
        new GeneralTemplate("樵夫", "white", "群", "普通", null),
        new GeneralTemplate("渔夫", "white", "群", "普通", null),
        new GeneralTemplate("猎人", "white", "群", "普通", null),
        new GeneralTemplate("铁匠", "white", "群", "普通", null),
        new GeneralTemplate("商人", "white", "群", "普通", null),
        new GeneralTemplate("书生", "white", "群", "普通", null),
        new GeneralTemplate("郎中", "white", "群", "普通", null)
    );

    // 特殊效果列表
    public static final String[] SPECIAL_EFFECTS = {
        "兵法发动概率+%d%%",
        "士兵治疗增加%d%%",
        "暴击伤害+%d%%",
        "计策伤害+%d%%",
        "闪避概率+%d%%",
        "反击概率+%d%%",
        "火攻伤害+%d%%",
        "水战伤害+%d%%",
        "夜袭伤害+%d%%",
        "伏击成功率+%d%%",
        "士气恢复+%d%%",
        "行军速度+%d%%"
    };

    // 辅助方法：随机类型
    private static String getRandomType() {
        String[] types = {"猛将", "智将", "统帅"};
        return types[new Random().nextInt(types.length)];
    }

    // 辅助方法：随机特征
    private static Trait getRandomTrait(String quality) {
        Random rand = new Random();
        String[] attrs = {"attack", "defense", "valor", "command", "dodge", "mobility"};
        String attr = attrs[rand.nextInt(attrs.length)];
        
        int value;
        switch (quality) {
            case "purple":
                switch (attr) {
                    case "attack": value = 200 + rand.nextInt(100); break;
                    case "defense": value = 180 + rand.nextInt(80); break;
                    case "valor": value = 25 + rand.nextInt(15); break;
                    case "command": value = 200 + rand.nextInt(100); break;
                    case "dodge": value = 10 + rand.nextInt(10); break;
                    case "mobility": value = 8 + rand.nextInt(5); break;
                    default: value = 200;
                }
                break;
            case "red":
                switch (attr) {
                    case "attack": value = 150 + rand.nextInt(60); break;
                    case "defense": value = 130 + rand.nextInt(50); break;
                    case "valor": value = 18 + rand.nextInt(10); break;
                    case "command": value = 150 + rand.nextInt(50); break;
                    case "dodge": value = 6 + rand.nextInt(6); break;
                    case "mobility": value = 5 + rand.nextInt(4); break;
                    default: value = 150;
                }
                break;
            default:
                value = 100;
        }
        return new Trait(attr, value);
    }

    // 获取所有将领模板
    public List<GeneralTemplate> getAllGeneralsByQuality(String quality) {
        switch (quality) {
            case "orange": return ORANGE_GENERALS;
            case "purple": return PURPLE_GENERALS;
            case "red": return RED_GENERALS;
            case "blue": return BLUE_GENERALS;
            case "green": return GREEN_GENERALS;
            case "white": return WHITE_GENERALS;
            default: return Collections.emptyList();
        }
    }

    // 品质类
    public static class Quality {
        public String id;
        public String name;
        public String color;
        public double attrMultiplier;
        public int traitCount;

        public Quality(String id, String name, String color, double attrMultiplier, int traitCount) {
            this.id = id;
            this.name = name;
            this.color = color;
            this.attrMultiplier = attrMultiplier;
            this.traitCount = traitCount;
        }
    }

    // 将领模板类
    public static class GeneralTemplate {
        public String name;
        public String quality;
        public String faction;
        public String type;
        public List<Trait> traits;

        public GeneralTemplate(String name, String quality, String faction, String type, List<Trait> traits) {
            this.name = name;
            this.quality = quality;
            this.faction = faction;
            this.type = type;
            this.traits = traits;
        }
    }

    // 特征类
    public static class Trait {
        public String type; // "attack", "defense", "valor", "command", "dodge", "mobility", "special"
        public Object value; // 数值或特殊效果描述

        public Trait(String type, Object value) {
            this.type = type;
            this.value = value;
        }
    }
}


