package com.tencent.wxcloudrun.service.nationwar;

import com.alibaba.fastjson.JSON;
import com.tencent.wxcloudrun.dao.NationWarMapper;
import com.tencent.wxcloudrun.exception.BusinessException;
import com.tencent.wxcloudrun.model.NationWar;
import com.tencent.wxcloudrun.model.NationWar.*;
import com.tencent.wxcloudrun.model.General;
import com.tencent.wxcloudrun.service.UserResourceService;
import com.tencent.wxcloudrun.service.SuitConfigService;
import com.tencent.wxcloudrun.service.alliance.AllianceService;
import com.tencent.wxcloudrun.service.battle.BattleCalculator;
import com.tencent.wxcloudrun.service.battle.BattleService;
import com.tencent.wxcloudrun.service.formation.FormationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
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

    @Autowired
    private BattleService battleService;

    @Autowired
    @Lazy
    private FormationService formationService;

    @Autowired
    private SuitConfigService suitConfigService;
    
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
    private static final int TRANSFER_GOLD_COST = 500;
    private static final long TRANSFER_SILVER_COST = 50000;
    private static final int TRANSFER_CITY_REQUIREMENT = 10;
    private static final String CHIBI_CITY_ID = "CHIBI";
    
    private static final Set<String> PLAYER_SELECTABLE_NATION_IDS = new HashSet<>(Arrays.asList("WEI", "SHU", "WU"));
    
    public NationWarService() {
        initMapData();
    }
    
    /**
     * Spring 依赖注入完成后，从数据库恢复城市归属和玩家计数
     */
    @PostConstruct
    public void restoreFromDatabase() {
        try {
            List<Map<String, Object>> rows = nationWarMapper.findAllCityOwners();
            if (rows != null && !rows.isEmpty()) {
                for (Map<String, Object> row : rows) {
                    String cityId = (String) row.get("cityId");
                    String owner = (String) row.get("owner");
                    if (cityId == null || owner == null) continue;
                    
                    City city = cities.get(cityId);
                    if (city == null) continue;
                    
                    String oldOwner = city.getOwner();
                    if (oldOwner.equals(owner)) continue;
                    
                    Nation oldNation = nations.get(oldOwner);
                    if (oldNation != null) oldNation.getCities().remove(cityId);
                    
                    Nation newNation = nations.get(owner);
                    if (newNation != null && !newNation.getCities().contains(cityId)) {
                        newNation.getCities().add(cityId);
                    }
                    
                    city.setOwner(owner);
                    logger.info("从DB恢复城市归属: {} -> {}", cityId, owner);
                }
                
                for (Nation nation : nations.values()) {
                    updateMeritExchangeRate(nation.getId());
                }
                logger.info("城市归属恢复完成，共处理 {} 条记录", rows.size());
            }
        } catch (Exception e) {
            logger.warn("从DB恢复城市归属失败（首次部署或表不存在），使用默认配置: {}", e.getMessage());
        }
        
        try {
            List<Map<String, Object>> counts = nationWarMapper.countPlayersByNation();
            if (counts != null) {
                for (Map<String, Object> row : counts) {
                    String nationId = (String) row.get("nation");
                    Number cnt = (Number) row.get("cnt");
                    if (nationId == null || cnt == null) continue;
                    Nation nation = nations.get(nationId);
                    if (nation != null) {
                        nation.setTotalPlayers(cnt.intValue());
                    }
                }
                logger.info("玩家国籍计数恢复完成");
            }
        } catch (Exception e) {
            logger.warn("从DB恢复玩家计数失败: {}", e.getMessage());
        }
    }
    
    /**
     * 初始化地图数据 - 对齐 APK 原版（3 国 + 中立赤壁，共 19 城）
     *
     * 地图布局（基于 APK CampWarCityShow_cfg）：
     *   蜀(左) — 赤壁(中央) — 魏(中右)
     *                          吴(右上)
     */
    private void initMapData() {
        // ========== 三国势力 ==========
        nations.put("WEI", Nation.builder()
            .id("WEI").name("魏").color("#4488ff")
            .capitalId("LUOYANG").capitalName("洛阳")
            .cities(new ArrayList<>(Arrays.asList(
                "RUNAN", "XINYE", "SHANGYONG", "WANCHENG", "XUCHANG", "LUOYANG"
            )))
            .totalPlayers(0).meritExchangeRate(1.0).build());
        
        nations.put("SHU", Nation.builder()
            .id("SHU").name("蜀").color("#44cc66")
            .capitalId("CHENGDU").capitalName("成都")
            .cities(new ArrayList<>(Arrays.asList(
                "HANZHONG", "XIANGYANG", "WULING", "JIANNING", "JIANGZHOU", "CHENGDU"
            )))
            .totalPlayers(0).meritExchangeRate(1.0).build());
        
        nations.put("WU", Nation.builder()
            .id("WU").name("吴").color("#bb55ee")
            .capitalId("JIANYE").capitalName("建邺")
            .cities(new ArrayList<>(Arrays.asList(
                "JIANGXIA", "CHANGSHA", "CHAISANG", "LUJIANG", "HEFEI", "JIANYE"
            )))
            .totalPlayers(0).meritExchangeRate(1.0).build());
        
        // ========== 中立城市：赤壁（三国必争之地）==========
        cities.put("CHIBI", City.builder()
            .id("CHIBI").name("赤壁").owner("NEUTRAL").x(360).y(248)
            .neighbors(Arrays.asList("RUNAN", "XINYE", "HANZHONG", "JIANGXIA"))
            .isCapital(false).defenseBonus(20)
            .pic("cityBig0.png").flagX(0).flagY(10).build());
        
        // ========== 魏国 6 城（camp=1）==========
        cities.put("RUNAN", City.builder()
            .id("RUNAN").name("汝南").owner("WEI").x(454).y(290)
            .neighbors(Arrays.asList("CHIBI", "XINYE", "WANCHENG", "XUCHANG"))
            .isCapital(false).defenseBonus(10)
            .pic("cityMid1.png").flagX(0).flagY(10).build());
        cities.put("XINYE", City.builder()
            .id("XINYE").name("新野").owner("WEI").x(328).y(332)
            .neighbors(Arrays.asList("CHIBI", "RUNAN", "SHANGYONG"))
            .isCapital(false).defenseBonus(10)
            .pic("cityMid2.png").flagX(-10).flagY(10).build());
        cities.put("SHANGYONG", City.builder()
            .id("SHANGYONG").name("上庸").owner("WEI").x(397).y(369)
            .neighbors(Arrays.asList("XINYE", "WANCHENG"))
            .isCapital(false).defenseBonus(10)
            .pic("citySmall1.png").flagX(0).flagY(10).build());
        cities.put("WANCHENG", City.builder()
            .id("WANCHENG").name("宛城").owner("WEI").x(508).y(357)
            .neighbors(Arrays.asList("RUNAN", "SHANGYONG", "XUCHANG"))
            .isCapital(false).defenseBonus(10)
            .pic("citySmall1.png").flagX(0).flagY(10).build());
        cities.put("XUCHANG", City.builder()
            .id("XUCHANG").name("许昌").owner("WEI").x(598).y(324)
            .neighbors(Arrays.asList("RUNAN", "WANCHENG", "LUOYANG"))
            .isCapital(false).defenseBonus(15)
            .pic("citySmall1.png").flagX(0).flagY(10).build());
        cities.put("LUOYANG", City.builder()
            .id("LUOYANG").name("洛阳").owner("WEI").x(680).y(363)
            .neighbors(Arrays.asList("XUCHANG"))
            .isCapital(true).defenseBonus(25)
            .pic("cityBig1.png").flagX(10).flagY(10).build());
        
        // ========== 蜀国 6 城（camp=2）==========
        cities.put("HANZHONG", City.builder()
            .id("HANZHONG").name("汉中").owner("SHU").x(221).y(248)
            .neighbors(Arrays.asList("CHIBI", "XIANGYANG", "JIANGZHOU"))
            .isCapital(false).defenseBonus(10)
            .pic("cityMid1.png").flagX(0).flagY(10).build());
        cities.put("XIANGYANG", City.builder()
            .id("XIANGYANG").name("襄阳").owner("SHU").x(252).y(150)
            .neighbors(Arrays.asList("HANZHONG", "WULING", "JIANNING"))
            .isCapital(false).defenseBonus(10)
            .pic("cityMid2.png").flagX(0).flagY(10).build());
        cities.put("WULING", City.builder()
            .id("WULING").name("武陵").owner("SHU").x(239).y(88)
            .neighbors(Arrays.asList("XIANGYANG", "JIANNING"))
            .isCapital(false).defenseBonus(10)
            .pic("citySmall2.png").flagX(5).flagY(0).build());
        cities.put("JIANNING", City.builder()
            .id("JIANNING").name("建宁").owner("SHU").x(154).y(110)
            .neighbors(Arrays.asList("XIANGYANG", "WULING", "JIANGZHOU"))
            .isCapital(false).defenseBonus(10)
            .pic("citySmall2.png").flagX(0).flagY(0).build());
        cities.put("JIANGZHOU", City.builder()
            .id("JIANGZHOU").name("江州").owner("SHU").x(108).y(184)
            .neighbors(Arrays.asList("HANZHONG", "JIANNING", "CHENGDU"))
            .isCapital(false).defenseBonus(10)
            .pic("citySmall2.png").flagX(0).flagY(0).build());
        cities.put("CHENGDU", City.builder()
            .id("CHENGDU").name("成都").owner("SHU").x(75).y(257)
            .neighbors(Arrays.asList("JIANGZHOU"))
            .isCapital(true).defenseBonus(25)
            .pic("cityBig2.png").flagX(10).flagY(10).build());
        
        // ========== 吴国 6 城（camp=3）==========
        cities.put("JIANGXIA", City.builder()
            .id("JIANGXIA").name("江夏").owner("WU").x(519).y(199)
            .neighbors(Arrays.asList("CHIBI", "CHANGSHA", "LUJIANG"))
            .isCapital(false).defenseBonus(10)
            .pic("cityMid1.png").flagX(0).flagY(10).build());
        cities.put("CHANGSHA", City.builder()
            .id("CHANGSHA").name("长沙").owner("WU").x(537).y(119)
            .neighbors(Arrays.asList("JIANGXIA", "CHAISANG"))
            .isCapital(false).defenseBonus(10)
            .pic("cityMid2.png").flagX(0).flagY(10).build());
        cities.put("CHAISANG", City.builder()
            .id("CHAISANG").name("柴桑").owner("WU").x(601).y(81)
            .neighbors(Arrays.asList("CHANGSHA", "LUJIANG", "JIANYE"))
            .isCapital(false).defenseBonus(10)
            .pic("citySmall3.png").flagX(0).flagY(0).build());
        cities.put("LUJIANG", City.builder()
            .id("LUJIANG").name("庐江").owner("WU").x(657).y(149)
            .neighbors(Arrays.asList("JIANGXIA", "CHAISANG", "HEFEI"))
            .isCapital(false).defenseBonus(10)
            .pic("citySmall3.png").flagX(0).flagY(0).build());
        cities.put("HEFEI", City.builder()
            .id("HEFEI").name("合肥").owner("WU").x(756).y(168)
            .neighbors(Arrays.asList("LUJIANG", "JIANYE"))
            .isCapital(false).defenseBonus(10)
            .pic("citySmall3.png").flagX(0).flagY(0).build());
        cities.put("JIANYE", City.builder()
            .id("JIANYE").name("建邺").owner("WU").x(725).y(91)
            .neighbors(Arrays.asList("CHAISANG", "HEFEI"))
            .isCapital(true).defenseBonus(25)
            .pic("cityBig3.png").flagX(10).flagY(10).build());
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
        if (!PLAYER_SELECTABLE_NATION_IDS.contains(nationId.toUpperCase())) {
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
        if (!PLAYER_SELECTABLE_NATION_IDS.contains(newNationId.toUpperCase())) {
            throw new BusinessException(400, "无效的目标国家");
        }
        
        City chibi = cities.get(CHIBI_CITY_ID);
        if (chibi == null || !currentNation.equals(chibi.getOwner())) {
            throw new BusinessException(400, "转国条件不满足：您的国家必须占领赤壁");
        }
        
        Nation nation = nations.get(currentNation);
        if (nation.getCities().size() < TRANSFER_CITY_REQUIREMENT) {
            throw new BusinessException(400, "转国条件不满足：您的国家城池数量需超过" + TRANSFER_CITY_REQUIREMENT + "个（当前：" + nation.getCities().size() + "）");
        }
        
        try {
            userResourceService.consumeGold(odUserId, TRANSFER_GOLD_COST);
            userResourceService.consumeSilver(odUserId, TRANSFER_SILVER_COST);
        } catch (Exception e) {
            logger.error("转国异常", e);
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
        
        City chibi = cities.get(CHIBI_CITY_ID);
        boolean hasChibi = chibi != null && currentNation.equals(chibi.getOwner());
        result.put("hasChibi", hasChibi);
        if (!hasChibi) { reasons.add("您的国家必须占领赤壁"); }
        
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
                
                List<BattleCalculator.BattleUnit> sideA = buildParticipantUnits(attacker);
                List<BattleCalculator.BattleUnit> sideB = buildParticipantUnits(defender);
                BattleService.BattleReport report = battleService.fight(sideA, sideB, 20);
                boolean attackerWins = report.victoryA;

                WarBattle battle = WarBattle.builder()
                    .battleId(UUID.randomUUID().toString()).round(round)
                    .attackerId(attacker.getOdUserId()).attackerName(attacker.getPlayerName()).attackerPower(attacker.getPower())
                    .defenderId(defender.getOdUserId()).defenderName(defender.getPlayerName()).defenderPower(defender.getPower())
                    .winnerId(attackerWins ? attacker.getOdUserId() : defender.getOdUserId())
                    .winnerName(attackerWins ? attacker.getPlayerName() : defender.getPlayerName())
                    .scoreGained(SCORE_PER_WIN).meritGained(MERIT_PER_WIN).battleTime(System.currentTimeMillis())
                    .battleReportJson(JSON.toJSONString(report))
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
    
    private List<BattleCalculator.BattleUnit> buildParticipantUnits(WarParticipant p) {
        String odUserId = p.getOdUserId();
        if (odUserId != null && !odUserId.startsWith("NPC_")) {
            try {
                List<General> generals = formationService.getBattleOrder(odUserId);
                if (!generals.isEmpty()) {
                    List<BattleCalculator.BattleUnit> units = new ArrayList<>();
                    for (int i = 0; i < generals.size(); i++) {
                        General g = generals.get(i);
                        Map<String, Integer> eq = suitConfigService.calculateTotalEquipBonus(g.getId());
                        int rawTier = g.getSoldierTier() != null ? g.getSoldierTier() : 1;
                        int sRank = g.getSoldierRank() != null ? g.getSoldierRank() : 1;
                        int tier = Math.max(rawTier, sRank);
                        int troopType = BattleCalculator.parseTroopType(g.getTroopType());
                        int maxSc = g.getSoldierMaxCount() != null ? g.getSoldierMaxCount() : 100;
                        int formLv = BattleCalculator.maxPeopleToFormationLevel(maxSc);
                        int sc = g.getSoldierCount() != null ? Math.min(g.getSoldierCount(), maxSc) : maxSc;
                        BattleCalculator.BattleUnit u = BattleCalculator.assembleBattleUnit(
                                g.getName() != null ? g.getName() : p.getPlayerName(),
                                g.getLevel() != null ? g.getLevel() : p.getLevel(),
                                g.getAttrAttack() != null ? g.getAttrAttack() : 100,
                                g.getAttrDefense() != null ? g.getAttrDefense() : 50,
                                g.getAttrValor() != null ? g.getAttrValor() : 10,
                                g.getAttrCommand() != null ? g.getAttrCommand() : 10,
                                g.getAttrDodge() != null ? (int) Math.round(g.getAttrDodge()) : 5,
                                g.getAttrMobility() != null ? g.getAttrMobility() : 15,
                                troopType, tier, sc, maxSc, formLv,
                                eq.getOrDefault("attack", 0), eq.getOrDefault("defense", 0),
                                eq.getOrDefault("speed", 0), eq.getOrDefault("hit", 0),
                                eq.getOrDefault("dodge", 0), 0, 0, 0);
                        u.position = i;
                        units.add(u);
                    }
                    return units;
                }
            } catch (Exception e) {
                logger.warn("构建国战参战者阵型失败: {}", odUserId, e);
            }
        }
        // NPC 或无阵型玩家：根据 power/level 生成默认单位
        int level = p.getLevel() != null ? p.getLevel() : 30;
        int power = p.getPower() != null ? p.getPower() : 5000;
        int tier = Math.max(1, Math.min(10, 1 + level / 20));
        int formLv = BattleCalculator.levelToFormationLevel(level);
        int maxSc = BattleCalculator.getFormationMaxPeople(formLv);
        BattleCalculator.BattleUnit u = BattleCalculator.assembleBattleUnit(
                p.getPlayerName(), level,
                power / 2, power / 3, level * 2, level * 2,
                5, 15, 1, tier, maxSc, maxSc, formLv,
                0, 0, 0, 0, 0, 0, 0, 0);
        u.position = 0;
        return Collections.singletonList(u);
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
        
        try {
            nationWarMapper.upsertCityOwner(cityId, newOwner, System.currentTimeMillis());
        } catch (Exception e) {
            logger.error("持久化城市归属失败: cityId={}, newOwner={}", cityId, newOwner, e);
        }
        
        logger.info("城市 {} 所有权从 {} 转移到 {}", city.getName(), oldOwner, newOwner);
    }
    
    private void updateMeritExchangeRate(String nationId) {
        Nation nation = nations.get(nationId);
        if (nation == null) return;
        int cityCount = nation.getCities().size();
        nation.setMeritExchangeRate(1.0 + (cityCount - 6) * 0.1);
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
    
    /** 所有国家（含汉/NPC，用于地图展示等） */
    public List<Nation> getAllNations() { return new ArrayList<>(nations.values()); }
    
    /** 玩家可选国家（仅魏、蜀、吴；汉/群雄为NPC不可选） */
    public List<Nation> getPlayerSelectableNations() {
        List<Nation> list = new ArrayList<>();
        for (String id : PLAYER_SELECTABLE_NATION_IDS) {
            Nation n = nations.get(id);
            if (n != null) list.add(n);
        }
        return list;
    }
    
    /** 判断是否为中立势力 */
    public boolean isNeutralOwner(String owner) {
        return "NEUTRAL".equals(owner);
    }
}
