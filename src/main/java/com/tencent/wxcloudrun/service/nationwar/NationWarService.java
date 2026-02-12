package com.tencent.wxcloudrun.service.nationwar;

import com.alibaba.fastjson.JSON;
import com.tencent.wxcloudrun.dao.NationWarMapper;
import com.tencent.wxcloudrun.exception.BusinessException;
import com.tencent.wxcloudrun.model.NationWar;
import com.tencent.wxcloudrun.model.NationWar.*;
import com.tencent.wxcloudrun.service.UserResourceService;
import com.tencent.wxcloudrun.service.alliance.AllianceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 国战服务（数据库存储）
 */
@Service
public class NationWarService {
    
    private static final Logger logger = LoggerFactory.getLogger(NationWarService.class);
    
    @Autowired
    private UserResourceService userResourceService;
    
    @Autowired
    @Lazy
    private AllianceService allianceService;
    
    @Autowired
    private NationWarMapper nationWarMapper;
    
    // 国家信息（游戏配置，只读，保留内存）
    private final Map<String, Nation> nations = new LinkedHashMap<>();
    
    // 城市信息（游戏配置，只读初始化后可能被国战修改，保留内存）
    private final Map<String, City> cities = new LinkedHashMap<>();
    
    // 报名人数要求
    private static final int MIN_SIGN_UP = 10;
    private static final int VICTORY_POINT = 10000;
    private static final int SCORE_PER_WIN = 500;
    private static final int MERIT_PER_WIN = 100;
    private static final int MERIT_PER_LOSS = 30;
    private static final int TRANSFER_GOLD_COST = 1000;
    private static final long TRANSFER_SILVER_COST = 100000;
    private static final int TRANSFER_CITY_REQUIREMENT = 25;
    private static final String LUOYANG_CITY_ID = "LUOYANG";
    
    public NationWarService() {
        initMapData();
    }
    
    /**
     * 初始化地图数据 - 扩充版（每国10城 + 10群雄城池）
     */
    private void initMapData() {
        // ========== 初始化四方势力 ==========
        nations.put("WEI", Nation.builder()
            .id("WEI").name("魏").color("#0066cc")
            .capitalId("YECHENG").capitalName("邺城")
            .cities(new ArrayList<>(Arrays.asList(
                "YECHENG", "XUCHANG", "CHENLIU", "YINGCHUAN", "NANYANG_WEI",
                "PUYANG", "JUANCHENG", "DONGPING", "BEIHAI", "LANGYE"
            )))
            .totalPlayers(0).meritExchangeRate(1.0).build());
        
        nations.put("SHU", Nation.builder()
            .id("SHU").name("蜀").color("#00aa00")
            .capitalId("CHENGDU").capitalName("成都")
            .cities(new ArrayList<>(Arrays.asList(
                "CHENGDU", "HANZHONG", "JIAMENG", "ZITONG", "BAZHONG",
                "JIANNING", "YONGCHANG", "YIZHOU", "NANZHONG", "WUDU"
            )))
            .totalPlayers(0).meritExchangeRate(1.0).build());
        
        nations.put("WU", Nation.builder()
            .id("WU").name("吴").color("#cc0000")
            .capitalId("JIANYE").capitalName("建业")
            .cities(new ArrayList<>(Arrays.asList(
                "JIANYE", "WUCHANG", "CHANGSHA", "JIANGXIA", "LUJIANG",
                "KUAIJI", "DANYANG", "YUZHANG", "LINGLING", "GUIYANG"
            )))
            .totalPlayers(0).meritExchangeRate(1.0).build());
        
        nations.put("HAN", Nation.builder()
            .id("HAN").name("汉").color("#ffcc00")
            .capitalId("LUOYANG").capitalName("洛阳")
            .cities(new ArrayList<>(Arrays.asList(
                "LUOYANG", "CHANGAN", "HONGNONG", "HEDONG", "SHANGDANG",
                "TAIYUAN", "YANMEN", "DAIJUN", "YOUZHOU", "LIAOXI"
            )))
            .totalPlayers(0).meritExchangeRate(1.5).build());
        
        // ========== 初始化所有城市 ==========
        // 魏国城市
        cities.put("YECHENG", City.builder().id("YECHENG").name("邺城").owner("WEI").x(480).y(120).neighbors(Arrays.asList("PUYANG", "DONGPING", "SHANGDANG", "HEDONG")).isCapital(true).defenseBonus(25).build());
        cities.put("XUCHANG", City.builder().id("XUCHANG").name("许昌").owner("WEI").x(450).y(220).neighbors(Arrays.asList("CHENLIU", "YINGCHUAN", "NANYANG_WEI", "LUOYANG")).isCapital(false).defenseBonus(15).build());
        cities.put("CHENLIU", City.builder().id("CHENLIU").name("陈留").owner("WEI").x(480).y(200).neighbors(Arrays.asList("XUCHANG", "PUYANG", "LANGYE")).isCapital(false).defenseBonus(10).build());
        cities.put("YINGCHUAN", City.builder().id("YINGCHUAN").name("颍川").owner("WEI").x(420).y(250).neighbors(Arrays.asList("XUCHANG", "NANYANG_WEI", "JIANGXIA")).isCapital(false).defenseBonus(10).build());
        cities.put("NANYANG_WEI", City.builder().id("NANYANG_WEI").name("南阳").owner("WEI").x(380).y(280).neighbors(Arrays.asList("XUCHANG", "YINGCHUAN", "HONGNONG", "WUCHANG")).isCapital(false).defenseBonus(10).build());
        cities.put("PUYANG", City.builder().id("PUYANG").name("濮阳").owner("WEI").x(500).y(160).neighbors(Arrays.asList("YECHENG", "CHENLIU", "DONGPING")).isCapital(false).defenseBonus(10).build());
        cities.put("JUANCHENG", City.builder().id("JUANCHENG").name("鄄城").owner("WEI").x(520).y(180).neighbors(Arrays.asList("DONGPING", "BEIHAI")).isCapital(false).defenseBonus(10).build());
        cities.put("DONGPING", City.builder().id("DONGPING").name("东平").owner("WEI").x(540).y(150).neighbors(Arrays.asList("YECHENG", "PUYANG", "JUANCHENG", "BEIHAI")).isCapital(false).defenseBonus(10).build());
        cities.put("BEIHAI", City.builder().id("BEIHAI").name("北海").owner("WEI").x(580).y(140).neighbors(Arrays.asList("DONGPING", "JUANCHENG", "LANGYE")).isCapital(false).defenseBonus(10).build());
        cities.put("LANGYE", City.builder().id("LANGYE").name("琅琊").owner("WEI").x(600).y(180).neighbors(Arrays.asList("BEIHAI", "CHENLIU", "DANYANG")).isCapital(false).defenseBonus(10).build());
        // 蜀国城市
        cities.put("CHENGDU", City.builder().id("CHENGDU").name("成都").owner("SHU").x(180).y(350).neighbors(Arrays.asList("HANZHONG", "JIAMENG", "ZITONG", "JIANNING")).isCapital(true).defenseBonus(25).build());
        cities.put("HANZHONG", City.builder().id("HANZHONG").name("汉中").owner("SHU").x(220).y(280).neighbors(Arrays.asList("CHENGDU", "CHANGAN", "WUDU", "ZITONG")).isCapital(false).defenseBonus(20).build());
        cities.put("JIAMENG", City.builder().id("JIAMENG").name("剑阁").owner("SHU").x(200).y(320).neighbors(Arrays.asList("CHENGDU", "ZITONG", "BAZHONG")).isCapital(false).defenseBonus(20).build());
        cities.put("ZITONG", City.builder().id("ZITONG").name("梓潼").owner("SHU").x(210).y(300).neighbors(Arrays.asList("CHENGDU", "HANZHONG", "JIAMENG")).isCapital(false).defenseBonus(10).build());
        cities.put("BAZHONG", City.builder().id("BAZHONG").name("巴中").owner("SHU").x(230).y(340).neighbors(Arrays.asList("JIAMENG", "YIZHOU")).isCapital(false).defenseBonus(10).build());
        cities.put("JIANNING", City.builder().id("JIANNING").name("建宁").owner("SHU").x(160).y(420).neighbors(Arrays.asList("CHENGDU", "NANZHONG", "YONGCHANG")).isCapital(false).defenseBonus(10).build());
        cities.put("YONGCHANG", City.builder().id("YONGCHANG").name("永昌").owner("SHU").x(120).y(450).neighbors(Arrays.asList("JIANNING", "NANZHONG")).isCapital(false).defenseBonus(10).build());
        cities.put("YIZHOU", City.builder().id("YIZHOU").name("益州").owner("SHU").x(200).y(380).neighbors(Arrays.asList("BAZHONG", "NANZHONG", "LINGLING")).isCapital(false).defenseBonus(10).build());
        cities.put("NANZHONG", City.builder().id("NANZHONG").name("南中").owner("SHU").x(150).y(480).neighbors(Arrays.asList("JIANNING", "YONGCHANG", "YIZHOU")).isCapital(false).defenseBonus(10).build());
        cities.put("WUDU", City.builder().id("WUDU").name("武都").owner("SHU").x(250).y(260).neighbors(Arrays.asList("HANZHONG", "CHANGAN")).isCapital(false).defenseBonus(15).build());
        // 吴国城市
        cities.put("JIANYE", City.builder().id("JIANYE").name("建业").owner("WU").x(560).y(320).neighbors(Arrays.asList("LUJIANG", "DANYANG", "KUAIJI")).isCapital(true).defenseBonus(25).build());
        cities.put("WUCHANG", City.builder().id("WUCHANG").name("武昌").owner("WU").x(440).y(340).neighbors(Arrays.asList("JIANGXIA", "CHANGSHA", "NANYANG_WEI", "YUZHANG")).isCapital(false).defenseBonus(15).build());
        cities.put("CHANGSHA", City.builder().id("CHANGSHA").name("长沙").owner("WU").x(420).y(400).neighbors(Arrays.asList("WUCHANG", "LINGLING", "GUIYANG", "YUZHANG")).isCapital(false).defenseBonus(10).build());
        cities.put("JIANGXIA", City.builder().id("JIANGXIA").name("江夏").owner("WU").x(460).y(310).neighbors(Arrays.asList("WUCHANG", "YINGCHUAN", "LUJIANG")).isCapital(false).defenseBonus(10).build());
        cities.put("LUJIANG", City.builder().id("LUJIANG").name("庐江").owner("WU").x(520).y(300).neighbors(Arrays.asList("JIANYE", "JIANGXIA", "YUZHANG")).isCapital(false).defenseBonus(10).build());
        cities.put("KUAIJI", City.builder().id("KUAIJI").name("会稽").owner("WU").x(600).y(360).neighbors(Arrays.asList("JIANYE", "DANYANG")).isCapital(false).defenseBonus(10).build());
        cities.put("DANYANG", City.builder().id("DANYANG").name("丹阳").owner("WU").x(580).y(300).neighbors(Arrays.asList("JIANYE", "KUAIJI", "LANGYE")).isCapital(false).defenseBonus(10).build());
        cities.put("YUZHANG", City.builder().id("YUZHANG").name("豫章").owner("WU").x(500).y(380).neighbors(Arrays.asList("LUJIANG", "WUCHANG", "CHANGSHA")).isCapital(false).defenseBonus(10).build());
        cities.put("LINGLING", City.builder().id("LINGLING").name("零陵").owner("WU").x(380).y(450).neighbors(Arrays.asList("CHANGSHA", "GUIYANG", "YIZHOU")).isCapital(false).defenseBonus(10).build());
        cities.put("GUIYANG", City.builder().id("GUIYANG").name("桂阳").owner("WU").x(440).y(460).neighbors(Arrays.asList("CHANGSHA", "LINGLING")).isCapital(false).defenseBonus(10).build());
        // 群雄/汉城市
        cities.put("LUOYANG", City.builder().id("LUOYANG").name("洛阳").owner("HAN").x(380).y(200).neighbors(Arrays.asList("XUCHANG", "HONGNONG", "HEDONG", "CHANGAN")).isCapital(true).defenseBonus(30).build());
        cities.put("CHANGAN", City.builder().id("CHANGAN").name("长安").owner("HAN").x(280).y(200).neighbors(Arrays.asList("LUOYANG", "HANZHONG", "WUDU", "HONGNONG")).isCapital(false).defenseBonus(20).build());
        cities.put("HONGNONG", City.builder().id("HONGNONG").name("弘农").owner("HAN").x(340).y(220).neighbors(Arrays.asList("LUOYANG", "CHANGAN", "NANYANG_WEI", "HEDONG")).isCapital(false).defenseBonus(10).build());
        cities.put("HEDONG", City.builder().id("HEDONG").name("河东").owner("HAN").x(380).y(150).neighbors(Arrays.asList("LUOYANG", "HONGNONG", "YECHENG", "SHANGDANG", "TAIYUAN")).isCapital(false).defenseBonus(10).build());
        cities.put("SHANGDANG", City.builder().id("SHANGDANG").name("上党").owner("HAN").x(420).y(120).neighbors(Arrays.asList("YECHENG", "HEDONG", "TAIYUAN")).isCapital(false).defenseBonus(15).build());
        cities.put("TAIYUAN", City.builder().id("TAIYUAN").name("太原").owner("HAN").x(400).y(80).neighbors(Arrays.asList("HEDONG", "SHANGDANG", "YANMEN", "DAIJUN")).isCapital(false).defenseBonus(15).build());
        cities.put("YANMEN", City.builder().id("YANMEN").name("雁门").owner("HAN").x(380).y(40).neighbors(Arrays.asList("TAIYUAN", "DAIJUN")).isCapital(false).defenseBonus(20).build());
        cities.put("DAIJUN", City.builder().id("DAIJUN").name("代郡").owner("HAN").x(450).y(50).neighbors(Arrays.asList("TAIYUAN", "YANMEN", "YOUZHOU")).isCapital(false).defenseBonus(10).build());
        cities.put("YOUZHOU", City.builder().id("YOUZHOU").name("幽州").owner("HAN").x(520).y(40).neighbors(Arrays.asList("DAIJUN", "LIAOXI")).isCapital(false).defenseBonus(15).build());
        cities.put("LIAOXI", City.builder().id("LIAOXI").name("辽西").owner("HAN").x(600).y(50).neighbors(Arrays.asList("YOUZHOU")).isCapital(false).defenseBonus(10).build());
    }
    
    public WarMap getWarMap() {
        return WarMap.builder()
            .nations(new ArrayList<>(nations.values()))
            .cities(new ArrayList<>(cities.values()))
            .borders(calculateBorders())
            .build();
    }
    
    private Map<String, List<String>> calculateBorders() {
        Map<String, List<String>> borders = new HashMap<>();
        for (Nation nation : nations.values()) {
            Set<String> borderNations = new HashSet<>();
            for (String cityId : nation.getCities()) {
                City city = cities.get(cityId);
                if (city != null && city.getNeighbors() != null) {
                    for (String neighborId : city.getNeighbors()) {
                        City neighbor = cities.get(neighborId);
                        if (neighbor != null && !neighbor.getOwner().equals(nation.getId())) {
                            borderNations.add(neighbor.getOwner());
                        }
                    }
                }
            }
            borders.put(nation.getId(), new ArrayList<>(borderNations));
        }
        return borders;
    }
    
    public void setPlayerNation(String odUserId, String nationId) {
        if (!nations.containsKey(nationId)) {
            throw new BusinessException(400, "无效的国家");
        }
        
        String currentNation = nationWarMapper.findPlayerNation(odUserId);
        if (currentNation != null && !currentNation.equals(nationId)) {
            throw new BusinessException(400, "您已选择国家，如需更换请使用转国功能");
        }
        
        nationWarMapper.upsertPlayerNation(odUserId, nationId);
        
        Nation nation = nations.get(nationId);
        nation.setTotalPlayers(nation.getTotalPlayers() + 1);
        updateMeritExchangeRate(nationId);
    }
    
    public boolean hasSelectedNation(String odUserId) {
        return nationWarMapper.playerNationExists(odUserId) > 0;
    }
    
    public String getPlayerNation(String odUserId) {
        return nationWarMapper.findPlayerNation(odUserId);
    }
    
    public Map<String, Object> changeNation(String odUserId, String newNationId) {
        String currentNation = getPlayerNation(odUserId);
        
        if (currentNation == null) {
            throw new BusinessException(400, "您还未选择国家");
        }
        if (currentNation.equals(newNationId)) {
            throw new BusinessException(400, "不能转换到当前国家");
        }
        if (!nations.containsKey(newNationId)) {
            throw new BusinessException(400, "无效的目标国家");
        }
        
        City luoyang = cities.get(LUOYANG_CITY_ID);
        if (luoyang == null || !currentNation.equals(luoyang.getOwner())) {
            throw new BusinessException(400, "转国条件不满足：您的国家必须占领洛阳");
        }
        
        Nation nation = nations.get(currentNation);
        if (nation.getCities().size() < TRANSFER_CITY_REQUIREMENT) {
            throw new BusinessException(400, "转国条件不满足：您的国家城池数量需超过" + TRANSFER_CITY_REQUIREMENT + "个（当前：" + nation.getCities().size() + "）");
        }
        
        try {
            userResourceService.consumeGold(odUserId, TRANSFER_GOLD_COST);
            userResourceService.consumeSilver(odUserId, TRANSFER_SILVER_COST);
        } catch (Exception e) {
            throw new BusinessException(400, "转国费用不足：需要" + TRANSFER_GOLD_COST + "黄金 + " + TRANSFER_SILVER_COST + "白银");
        }
        
        try {
            if (allianceService != null) {
                allianceService.leaveAlliance(odUserId);
            }
        } catch (Exception e) {
            logger.info("玩家 {} 退出联盟: {}", odUserId, e.getMessage());
        }
        
        Nation oldNation = nations.get(currentNation);
        oldNation.setTotalPlayers(Math.max(0, oldNation.getTotalPlayers() - 1));
        
        Nation newNation = nations.get(newNationId);
        newNation.setTotalPlayers(newNation.getTotalPlayers() + 1);
        
        nationWarMapper.upsertPlayerNation(odUserId, newNationId);
        
        updateMeritExchangeRate(currentNation);
        updateMeritExchangeRate(newNationId);
        
        logger.info("玩家 {} 从 {} 转国到 {}", odUserId, currentNation, newNationId);
        
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("oldNation", currentNation);
        result.put("newNation", newNationId);
        result.put("goldCost", TRANSFER_GOLD_COST);
        result.put("silverCost", TRANSFER_SILVER_COST);
        result.put("message", "转国成功！欢迎加入" + newNation.getName() + "国");
        return result;
    }
    
    public Map<String, Object> checkCanChangeNation(String odUserId) {
        String currentNation = getPlayerNation(odUserId);
        Map<String, Object> result = new HashMap<>();
        result.put("canChange", false);
        List<String> reasons = new ArrayList<>();
        
        if (currentNation == null) {
            reasons.add("您还未选择国家");
            result.put("reasons", reasons);
            return result;
        }
        
        City luoyang = cities.get(LUOYANG_CITY_ID);
        boolean hasLuoyang = luoyang != null && currentNation.equals(luoyang.getOwner());
        result.put("hasLuoyang", hasLuoyang);
        if (!hasLuoyang) { reasons.add("您的国家必须占领洛阳"); }
        
        Nation nation = nations.get(currentNation);
        int cityCount = nation.getCities().size();
        result.put("cityCount", cityCount);
        result.put("cityRequired", TRANSFER_CITY_REQUIREMENT);
        if (cityCount < TRANSFER_CITY_REQUIREMENT) {
            reasons.add("您的国家城池数量需超过" + TRANSFER_CITY_REQUIREMENT + "个（当前：" + cityCount + "）");
        }
        
        result.put("goldCost", TRANSFER_GOLD_COST);
        result.put("silverCost", TRANSFER_SILVER_COST);
        result.put("reasons", reasons);
        result.put("canChange", reasons.isEmpty());
        return result;
    }
    
    public List<City> getAttackableCities(String odUserId) {
        String playerNation = getPlayerNation(odUserId);
        if (playerNation == null) { return new ArrayList<>(); }
        
        Set<String> attackableCityIds = new HashSet<>();
        Nation nation = nations.get(playerNation);
        
        for (String cityId : nation.getCities()) {
            City city = cities.get(cityId);
            if (city != null && city.getNeighbors() != null) {
                for (String neighborId : city.getNeighbors()) {
                    City neighbor = cities.get(neighborId);
                    if (neighbor != null && !neighbor.getOwner().equals(playerNation)) {
                        attackableCityIds.add(neighborId);
                    }
                }
            }
        }
        
        return attackableCityIds.stream().map(cities::get).filter(Objects::nonNull).collect(Collectors.toList());
    }
    
    public NationWar getTodayWar(String cityId) {
        String today = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
        String warId = today + "_" + cityId;
        String data = nationWarMapper.findById(warId);
        if (data == null) return null;
        return JSON.parseObject(data, NationWar.class);
    }
    
    private void saveWar(NationWar war) {
        nationWarMapper.upsert(war.getId(), war.getWarDate(), JSON.toJSONString(war));
    }
    
    public Map<String, Object> signUp(String odUserId, String playerName, Integer level, Integer power, String targetCityId) {
        String playerNation = getPlayerNation(odUserId);
        if (playerNation == null) { throw new BusinessException(400, "请先选择国家"); }
        
        City targetCity = cities.get(targetCityId);
        if (targetCity == null) { throw new BusinessException(400, "目标城市不存在"); }
        if (targetCity.getOwner().equals(playerNation)) { throw new BusinessException(400, "不能进攻自己国家的城市"); }
        
        List<City> attackable = getAttackableCities(odUserId);
        boolean canAttack = attackable.stream().anyMatch(c -> c.getId().equals(targetCityId));
        if (!canAttack) { throw new BusinessException(400, "只能进攻与本国接壤的城市"); }
        
        String today = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
        String warId = today + "_" + targetCityId;
        
        String warData = nationWarMapper.findById(warId);
        NationWar war;
        if (warData != null) {
            war = JSON.parseObject(warData, NationWar.class);
        } else {
            war = createWar(warId, today, playerNation, targetCity);
            saveWar(war);
        }
        
        if (war.getStatus() != WarStatus.SIGN_UP) { throw new BusinessException(400, "当前不是报名时间"); }
        
        boolean alreadySignedUp = war.getAttackers().stream().anyMatch(p -> p.getOdUserId().equals(odUserId));
        if (alreadySignedUp) { throw new BusinessException(400, "已报名，不能重复报名"); }
        
        WarParticipant participant = WarParticipant.builder()
            .odUserId(odUserId).playerName(playerName).nation(playerNation)
            .level(level).power(power).signUpTime(System.currentTimeMillis())
            .wins(0).losses(0).scoreGained(0).meritGained(0).eliminated(false)
            .build();
        
        war.getAttackers().add(participant);
        saveWar(war);
        
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("warId", warId);
        result.put("attackerCount", war.getAttackers().size());
        result.put("message", "报名成功");
        
        logger.info("国战报名: {} 报名进攻 {}", playerName, targetCity.getName());
        return result;
    }
    
    private NationWar createWar(String warId, String date, String attackNation, City targetCity) {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 19); cal.set(Calendar.MINUTE, 45); cal.set(Calendar.SECOND, 0);
        long signUpStart = cal.getTimeInMillis();
        cal.set(Calendar.HOUR_OF_DAY, 20); cal.set(Calendar.MINUTE, 0);
        long signUpEnd = cal.getTimeInMillis();
        long battleStart = signUpEnd;
        cal.set(Calendar.MINUTE, 45);
        long battleEnd = cal.getTimeInMillis();
        
        return NationWar.builder()
            .id(warId).warDate(date).status(WarStatus.SIGN_UP)
            .attackNation(attackNation).defendNation(targetCity.getOwner())
            .targetCityId(targetCity.getId()).targetCityName(targetCity.getName())
            .attackers(new ArrayList<>()).defenders(new ArrayList<>())
            .attackScore(0).defendScore(0).victoryPoint(VICTORY_POINT)
            .battles(new ArrayList<>())
            .signUpStartTime(signUpStart).signUpEndTime(signUpEnd)
            .battleStartTime(battleStart).battleEndTime(battleEnd)
            .createTime(System.currentTimeMillis())
            .build();
    }
    
    public Map<String, Object> getWarStatus(String warId) {
        String data = nationWarMapper.findById(warId);
        if (data == null) { throw new BusinessException(404, "国战不存在"); }
        NationWar war = JSON.parseObject(data, NationWar.class);
        
        Map<String, Object> result = new HashMap<>();
        result.put("war", war);
        result.put("attackerCount", war.getAttackers().size());
        result.put("defenderCount", war.getDefenders().size());
        result.put("canStart", war.getAttackers().size() >= MIN_SIGN_UP);
        return result;
    }
    
    public void startWarBattle(String warId) {
        String data = nationWarMapper.findById(warId);
        if (data == null) return;
        NationWar war = JSON.parseObject(data, NationWar.class);
        if (war.getStatus() != WarStatus.SIGN_UP) return;
        
        if (war.getAttackers().size() < MIN_SIGN_UP) {
            war.setStatus(WarStatus.FINISHED);
            war.setWinner(war.getDefendNation());
            saveWar(war);
            logger.info("国战 {} 因进攻方人数不足取消", warId);
            return;
        }
        
        war.setStatus(WarStatus.FIGHTING);
        logger.info("国战 {} 开始战斗", warId);
        simulateBattles(war);
        saveWar(war);
    }
    
    private void simulateBattles(NationWar war) {
        List<WarParticipant> attackers = new ArrayList<>(war.getAttackers());
        List<WarParticipant> defenders = new ArrayList<>(war.getDefenders());
        
        while (defenders.size() < attackers.size()) {
            defenders.add(createNpcDefender(war.getDefendNation(), defenders.size()));
        }
        
        Collections.shuffle(attackers); Collections.shuffle(defenders);
        
        int round = 1;
        Random random = new Random();
        
        while (war.getAttackScore() < war.getVictoryPoint() && war.getDefendScore() < war.getVictoryPoint()) {
            for (int i = 0; i < Math.min(attackers.size(), defenders.size()); i++) {
                WarParticipant attacker = attackers.get(i);
                WarParticipant defender = defenders.get(i);
                if (attacker.getEliminated() || defender.getEliminated()) continue;
                
                int attackPower = attacker.getPower() + random.nextInt(1000);
                int defendPower = defender.getPower() + random.nextInt(1000);
                boolean attackerWins = attackPower > defendPower;
                
                WarBattle battle = WarBattle.builder()
                    .battleId(UUID.randomUUID().toString()).round(round)
                    .attackerId(attacker.getOdUserId()).attackerName(attacker.getPlayerName()).attackerPower(attacker.getPower())
                    .defenderId(defender.getOdUserId()).defenderName(defender.getPlayerName()).defenderPower(defender.getPower())
                    .winnerId(attackerWins ? attacker.getOdUserId() : defender.getOdUserId())
                    .winnerName(attackerWins ? attacker.getPlayerName() : defender.getPlayerName())
                    .scoreGained(SCORE_PER_WIN).meritGained(MERIT_PER_WIN).battleTime(System.currentTimeMillis())
                    .build();
                war.getBattles().add(battle);
                
                if (attackerWins) {
                    war.setAttackScore(war.getAttackScore() + SCORE_PER_WIN);
                    attacker.setWins(attacker.getWins() + 1);
                    attacker.setScoreGained(attacker.getScoreGained() + SCORE_PER_WIN);
                    attacker.setMeritGained(attacker.getMeritGained() + MERIT_PER_WIN);
                    defender.setLosses(defender.getLosses() + 1);
                    defender.setMeritGained(defender.getMeritGained() + MERIT_PER_LOSS);
                    addPlayerMerit(attacker.getOdUserId(), MERIT_PER_WIN);
                    addPlayerMerit(defender.getOdUserId(), MERIT_PER_LOSS);
                } else {
                    war.setDefendScore(war.getDefendScore() + SCORE_PER_WIN);
                    defender.setWins(defender.getWins() + 1);
                    defender.setScoreGained(defender.getScoreGained() + SCORE_PER_WIN);
                    defender.setMeritGained(defender.getMeritGained() + MERIT_PER_WIN);
                    attacker.setLosses(attacker.getLosses() + 1);
                    attacker.setMeritGained(attacker.getMeritGained() + MERIT_PER_LOSS);
                    addPlayerMerit(defender.getOdUserId(), MERIT_PER_WIN);
                    addPlayerMerit(attacker.getOdUserId(), MERIT_PER_LOSS);
                }
                
                if (war.getAttackScore() >= war.getVictoryPoint() || war.getDefendScore() >= war.getVictoryPoint()) break;
            }
            round++;
            if (round > 100) break;
        }
        
        if (war.getAttackScore() >= war.getVictoryPoint()) {
            war.setWinner(war.getAttackNation());
            transferCity(war.getTargetCityId(), war.getAttackNation());
        } else if (war.getDefendScore() >= war.getVictoryPoint()) {
            war.setWinner(war.getDefendNation());
        } else {
            war.setWinner(war.getAttackScore() > war.getDefendScore() ? war.getAttackNation() : war.getDefendNation());
            if (war.getWinner().equals(war.getAttackNation())) {
                transferCity(war.getTargetCityId(), war.getAttackNation());
            }
        }
        
        war.setStatus(WarStatus.FINISHED);
        logger.info("国战 {} 结束，胜利方: {}", war.getId(), war.getWinner());
    }
    
    private WarParticipant createNpcDefender(String nation, int index) {
        String nationName = nations.get(nation) != null ? nations.get(nation).getName() : nation;
        return WarParticipant.builder()
            .odUserId("NPC_" + nation + "_" + index).playerName(nationName + "守军" + (index + 1))
            .nation(nation).level(30 + new Random().nextInt(20)).power(5000 + new Random().nextInt(3000))
            .signUpTime(System.currentTimeMillis()).wins(0).losses(0).scoreGained(0).meritGained(0).eliminated(false)
            .build();
    }
    
    private void transferCity(String cityId, String newOwner) {
        City city = cities.get(cityId);
        if (city == null) return;
        String oldOwner = city.getOwner();
        
        Nation oldNation = nations.get(oldOwner);
        if (oldNation != null) { oldNation.getCities().remove(cityId); updateMeritExchangeRate(oldOwner); }
        
        Nation newNation = nations.get(newOwner);
        if (newNation != null) { newNation.getCities().add(cityId); updateMeritExchangeRate(newOwner); }
        
        city.setOwner(newOwner);
        logger.info("城市 {} 所有权从 {} 转移到 {}", city.getName(), oldOwner, newOwner);
    }
    
    private void updateMeritExchangeRate(String nationId) {
        Nation nation = nations.get(nationId);
        if (nation == null) return;
        int cityCount = nation.getCities().size();
        nation.setMeritExchangeRate(1.0 + (cityCount - 10) * 0.05);
    }
    
    private void addPlayerMerit(String odUserId, int merit) {
        if (odUserId.startsWith("NPC_")) return;
        Integer current = nationWarMapper.findPlayerMerit(odUserId);
        int newMerit = (current != null ? current : 0) + merit;
        nationWarMapper.upsertPlayerMerit(odUserId, newMerit);
    }
    
    public int getPlayerMerit(String odUserId) {
        Integer merit = nationWarMapper.findPlayerMerit(odUserId);
        return merit != null ? merit : 0;
    }
    
    public Map<String, Object> exchangeMerit(String odUserId, int meritAmount) {
        int currentMerit = getPlayerMerit(odUserId);
        if (meritAmount > currentMerit) { throw new BusinessException(400, "军功不足"); }
        
        String playerNation = getPlayerNation(odUserId);
        if (playerNation == null) { throw new BusinessException(400, "请先选择国家"); }
        
        Nation nation = nations.get(playerNation);
        double rate = nation != null ? nation.getMeritExchangeRate() : 1.0;
        long silverGained = (long)(meritAmount * 10 * rate);
        
        nationWarMapper.upsertPlayerMerit(odUserId, currentMerit - meritAmount);
        userResourceService.addSilver(odUserId, silverGained);
        
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("meritUsed", meritAmount);
        result.put("silverGained", silverGained);
        result.put("exchangeRate", rate);
        result.put("remainingMerit", currentMerit - meritAmount);
        
        logger.info("军功兑换: {} 使用 {} 军功兑换 {} 白银", odUserId, meritAmount, silverGained);
        return result;
    }
    
    public List<NationWar> getActiveWars() {
        List<Map<String, Object>> rows = nationWarMapper.findAll();
        List<NationWar> result = new ArrayList<>();
        if (rows != null) {
            for (Map<String, Object> row : rows) {
                String data = (String) row.get("data");
                if (data != null) {
                    NationWar war = JSON.parseObject(data, NationWar.class);
                    if (war.getStatus() != WarStatus.FINISHED) { result.add(war); }
                }
            }
        }
        return result;
    }
    
    public List<NationWar> getWarHistory(int limit) {
        List<Map<String, Object>> rows = nationWarMapper.findAll();
        List<NationWar> result = new ArrayList<>();
        if (rows != null) {
            for (Map<String, Object> row : rows) {
                String data = (String) row.get("data");
                if (data != null) {
                    NationWar war = JSON.parseObject(data, NationWar.class);
                    if (war.getStatus() == WarStatus.FINISHED) { result.add(war); }
                }
            }
        }
        result.sort((a, b) -> Long.compare(b.getCreateTime(), a.getCreateTime()));
        return result.stream().limit(limit).collect(Collectors.toList());
    }
    
    public Nation getNation(String nationId) { return nations.get(nationId); }
    
    public List<Nation> getAllNations() { return new ArrayList<>(nations.values()); }
}
