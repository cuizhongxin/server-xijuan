package com.tencent.wxcloudrun.service.nationwar;

import com.alibaba.fastjson.JSON;
import com.tencent.wxcloudrun.dao.NationWarMapper;
import com.tencent.wxcloudrun.exception.BusinessException;
import com.tencent.wxcloudrun.model.NationWar;
import com.tencent.wxcloudrun.model.NationWar.*;
import com.tencent.wxcloudrun.model.General;
import com.tencent.wxcloudrun.model.UserResource;
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
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class NationWarService {

    private static final Logger logger = LoggerFactory.getLogger(NationWarService.class);

    @Autowired private UserResourceService userResourceService;
    @Autowired @Lazy private AllianceService allianceService;
    @Autowired private NationWarMapper nationWarMapper;
    @Autowired private BattleService battleService;
    @Autowired @Lazy private FormationService formationService;
    @Autowired private SuitConfigService suitConfigService;
    @Autowired @Lazy private com.tencent.wxcloudrun.service.general.GeneralService generalService;

    private final Map<String, Nation> nations = new LinkedHashMap<>();
    private final Map<String, City> cities = new LinkedHashMap<>();

    private static final int MIN_SIGN_UP = 10;
    private static final int VICTORY_POINT = 10000;
    private static final int NPC_DEFENDER_COUNT = 100;
    private static final int NPC_DEFENDER_LEVEL = 10;
    private static final int NPC_DEFENDER_POWER = 800;
    private static final int TRANSFER_GOLD_COST = 500;
    private static final long TRANSFER_SILVER_COST = 50000;
    private static final int TRANSFER_CITY_REQUIREMENT = 10;
    private static final String CHIBI_CITY_ID = "CHIBI";
    private static final String SESSION_ID_PREFIX = "SESSION_";
    private static final int DEFAULT_SWITCH_ROUNDS = 4;

    private static final Set<String> PLAYER_SELECTABLE_NATION_IDS =
            new HashSet<>(Arrays.asList("WEI", "SHU", "WU"));

    /** 当天活跃的国战会话（内存中缓存，定期持久化到DB） */
    private volatile NationWarSession activeSession;
    private boolean testMode = false;

    static int extractServerId(String compositeUserId) {
        if (compositeUserId == null) return 1;
        int idx = compositeUserId.lastIndexOf('_');
        if (idx > 0) {
            try { return Integer.parseInt(compositeUserId.substring(idx + 1)); }
            catch (NumberFormatException e) { return 1; }
        }
        return 1;
    }

    public NationWarService() {
        initMapData();
    }

    @PostConstruct
    public void restoreFromDatabase() {
        try {
            List<Map<String, Object>> rows = nationWarMapper.findCityOwnersByServerId(1);
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
                    if (newNation != null && !newNation.getCities().contains(cityId))
                        newNation.getCities().add(cityId);
                    city.setOwner(owner);
                    logger.info("从DB恢复城市归属: {} -> {}", cityId, owner);
                }
                for (Nation nation : nations.values()) updateMeritExchangeRate(nation.getId());
                logger.info("城市归属恢复完成，共处理 {} 条记录", rows.size());
            }
        } catch (Exception e) {
            logger.warn("从DB恢复城市归属失败: {}", e.getMessage());
        }
        try {
            List<Map<String, Object>> counts = nationWarMapper.countPlayersByNation(1);
            if (counts != null) {
                for (Map<String, Object> row : counts) {
                    String nationId = (String) row.get("nation");
                    Number cnt = (Number) row.get("cnt");
                    if (nationId == null || cnt == null) continue;
                    Nation nation = nations.get(nationId);
                    if (nation != null) nation.setTotalPlayers(cnt.intValue());
                }
                logger.info("玩家国籍计数恢复完成");
            }
        } catch (Exception e) {
            logger.warn("从DB恢复玩家计数失败: {}", e.getMessage());
        }
        try {
            String today = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
            String sessionId = SESSION_ID_PREFIX + today;
            String data = nationWarMapper.findById(sessionId);
            if (data != null) {
                activeSession = JSON.parseObject(data, NationWarSession.class);
                logger.info("从DB恢复国战会话: phase={}", activeSession.getPhase());
            }
        } catch (Exception e) {
            logger.warn("恢复国战会话失败: {}", e.getMessage());
        }
    }

    // ==================== 地图初始化 ====================

    private void initMapData() {
        nations.put("WEI", Nation.builder().id("WEI").name("魏").color("#4488ff")
                .capitalId("LUOYANG").capitalName("洛阳")
                .cities(new ArrayList<>(Arrays.asList("RUNAN","XINYE","SHANGYONG","WANCHENG","XUCHANG","LUOYANG")))
                .totalPlayers(0).meritExchangeRate(1.0).build());
        nations.put("SHU", Nation.builder().id("SHU").name("蜀").color("#44cc66")
                .capitalId("CHENGDU").capitalName("成都")
                .cities(new ArrayList<>(Arrays.asList("HANZHONG","XIANGYANG","WULING","JIANNING","JIANGZHOU","CHENGDU")))
                .totalPlayers(0).meritExchangeRate(1.0).build());
        nations.put("WU", Nation.builder().id("WU").name("吴").color("#bb55ee")
                .capitalId("JIANYE").capitalName("建邺")
                .cities(new ArrayList<>(Arrays.asList("JIANGXIA","CHANGSHA","CHAISANG","LUJIANG","HEFEI","JIANYE")))
                .totalPlayers(0).meritExchangeRate(1.0).build());

        cities.put("CHIBI", City.builder().id("CHIBI").name("赤壁").owner("NEUTRAL").x(360).y(248)
                .neighbors(Arrays.asList("RUNAN","XINYE","HANZHONG","JIANGXIA"))
                .isCapital(false).defenseBonus(20).pic("cityBig0.png").flagX(0).flagY(10).build());
        cities.put("RUNAN", City.builder().id("RUNAN").name("汝南").owner("WEI").x(454).y(290)
                .neighbors(Arrays.asList("CHIBI","XINYE","WANCHENG","XUCHANG"))
                .isCapital(false).defenseBonus(10).pic("cityMid1.png").flagX(0).flagY(10).build());
        cities.put("XINYE", City.builder().id("XINYE").name("新野").owner("WEI").x(328).y(332)
                .neighbors(Arrays.asList("CHIBI","RUNAN","SHANGYONG"))
                .isCapital(false).defenseBonus(10).pic("cityMid2.png").flagX(-10).flagY(10).build());
        cities.put("SHANGYONG", City.builder().id("SHANGYONG").name("上庸").owner("WEI").x(397).y(369)
                .neighbors(Arrays.asList("XINYE","WANCHENG"))
                .isCapital(false).defenseBonus(10).pic("citySmall1.png").flagX(0).flagY(10).build());
        cities.put("WANCHENG", City.builder().id("WANCHENG").name("宛城").owner("WEI").x(508).y(357)
                .neighbors(Arrays.asList("RUNAN","SHANGYONG","XUCHANG"))
                .isCapital(false).defenseBonus(10).pic("citySmall1.png").flagX(0).flagY(10).build());
        cities.put("XUCHANG", City.builder().id("XUCHANG").name("许昌").owner("WEI").x(598).y(324)
                .neighbors(Arrays.asList("RUNAN","WANCHENG","LUOYANG"))
                .isCapital(false).defenseBonus(15).pic("citySmall1.png").flagX(0).flagY(10).build());
        cities.put("LUOYANG", City.builder().id("LUOYANG").name("洛阳").owner("WEI").x(680).y(363)
                .neighbors(Arrays.asList("XUCHANG"))
                .isCapital(true).defenseBonus(25).pic("cityBig1.png").flagX(10).flagY(10).build());
        cities.put("HANZHONG", City.builder().id("HANZHONG").name("汉中").owner("SHU").x(221).y(248)
                .neighbors(Arrays.asList("CHIBI","XIANGYANG","JIANGZHOU"))
                .isCapital(false).defenseBonus(10).pic("cityMid1.png").flagX(0).flagY(10).build());
        cities.put("XIANGYANG", City.builder().id("XIANGYANG").name("襄阳").owner("SHU").x(252).y(150)
                .neighbors(Arrays.asList("HANZHONG","WULING","JIANNING"))
                .isCapital(false).defenseBonus(10).pic("cityMid2.png").flagX(0).flagY(10).build());
        cities.put("WULING", City.builder().id("WULING").name("武陵").owner("SHU").x(239).y(88)
                .neighbors(Arrays.asList("XIANGYANG","JIANNING"))
                .isCapital(false).defenseBonus(10).pic("citySmall2.png").flagX(5).flagY(0).build());
        cities.put("JIANNING", City.builder().id("JIANNING").name("建宁").owner("SHU").x(154).y(110)
                .neighbors(Arrays.asList("XIANGYANG","WULING","JIANGZHOU"))
                .isCapital(false).defenseBonus(10).pic("citySmall2.png").flagX(0).flagY(0).build());
        cities.put("JIANGZHOU", City.builder().id("JIANGZHOU").name("江州").owner("SHU").x(108).y(184)
                .neighbors(Arrays.asList("HANZHONG","JIANNING","CHENGDU"))
                .isCapital(false).defenseBonus(10).pic("citySmall2.png").flagX(0).flagY(0).build());
        cities.put("CHENGDU", City.builder().id("CHENGDU").name("成都").owner("SHU").x(75).y(257)
                .neighbors(Arrays.asList("JIANGZHOU"))
                .isCapital(true).defenseBonus(25).pic("cityBig2.png").flagX(10).flagY(10).build());
        cities.put("JIANGXIA", City.builder().id("JIANGXIA").name("江夏").owner("WU").x(519).y(199)
                .neighbors(Arrays.asList("CHIBI","CHANGSHA","LUJIANG"))
                .isCapital(false).defenseBonus(10).pic("cityMid1.png").flagX(0).flagY(10).build());
        cities.put("CHANGSHA", City.builder().id("CHANGSHA").name("长沙").owner("WU").x(537).y(119)
                .neighbors(Arrays.asList("JIANGXIA","CHAISANG"))
                .isCapital(false).defenseBonus(10).pic("cityMid2.png").flagX(0).flagY(10).build());
        cities.put("CHAISANG", City.builder().id("CHAISANG").name("柴桑").owner("WU").x(601).y(81)
                .neighbors(Arrays.asList("CHANGSHA","LUJIANG","JIANYE"))
                .isCapital(false).defenseBonus(10).pic("citySmall3.png").flagX(0).flagY(0).build());
        cities.put("LUJIANG", City.builder().id("LUJIANG").name("庐江").owner("WU").x(657).y(149)
                .neighbors(Arrays.asList("JIANGXIA","CHAISANG","HEFEI"))
                .isCapital(false).defenseBonus(10).pic("citySmall3.png").flagX(0).flagY(0).build());
        cities.put("HEFEI", City.builder().id("HEFEI").name("合肥").owner("WU").x(756).y(168)
                .neighbors(Arrays.asList("LUJIANG","JIANYE"))
                .isCapital(false).defenseBonus(10).pic("citySmall3.png").flagX(0).flagY(0).build());
        cities.put("JIANYE", City.builder().id("JIANYE").name("建邺").owner("WU").x(725).y(91)
                .neighbors(Arrays.asList("CHAISANG","HEFEI"))
                .isCapital(true).defenseBonus(25).pic("cityBig3.png").flagX(10).flagY(10).build());
    }

    // ==================== 地图/国家查询 ====================

    public WarMap getWarMap() {
        return WarMap.builder()
                .nations(new ArrayList<>(nations.values()))
                .cities(new ArrayList<>(cities.values()))
                .borders(calculateBorders()).build();
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
                        if (neighbor != null && !neighbor.getOwner().equals(nation.getId()))
                            borderNations.add(neighbor.getOwner());
                    }
                }
            }
            borders.put(nation.getId(), new ArrayList<>(borderNations));
        }
        return borders;
    }

    public void setPlayerNation(String odUserId, String nationId) {
        if (!nations.containsKey(nationId)) throw new BusinessException(400, "无效的国家");
        if (!PLAYER_SELECTABLE_NATION_IDS.contains(nationId.toUpperCase()))
            throw new BusinessException(400, "无效的国家");
        String currentNation = nationWarMapper.findPlayerNation(odUserId);
        if (currentNation != null && !currentNation.equals(nationId))
            throw new BusinessException(400, "您已选择国家，如需更换请使用转国功能");
        nationWarMapper.upsertPlayerNation(odUserId, nationId, extractServerId(odUserId));
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
        if (currentNation == null) throw new BusinessException(400, "您还未选择国家");
        if (currentNation.equals(newNationId)) throw new BusinessException(400, "不能转换到当前国家");
        if (!nations.containsKey(newNationId)) throw new BusinessException(400, "无效的目标国家");
        if (!PLAYER_SELECTABLE_NATION_IDS.contains(newNationId.toUpperCase()))
            throw new BusinessException(400, "无效的目标国家");
        City chibi = cities.get(CHIBI_CITY_ID);
        if (chibi == null || !currentNation.equals(chibi.getOwner()))
            throw new BusinessException(400, "转国条件不满足：您的国家必须占领赤壁");
        Nation nation = nations.get(currentNation);
        if (nation.getCities().size() < TRANSFER_CITY_REQUIREMENT)
            throw new BusinessException(400, "转国条件不满足：城池数量需超过" + TRANSFER_CITY_REQUIREMENT);
        try {
            userResourceService.consumeGold(odUserId, TRANSFER_GOLD_COST);
            userResourceService.consumeSilver(odUserId, TRANSFER_SILVER_COST);
        } catch (Exception e) {
            throw new BusinessException(400, "转国费用不足");
        }
        try { if (allianceService != null) allianceService.leaveAlliance(odUserId); }
        catch (Exception e) { logger.info("退出联盟: {}", e.getMessage()); }
        Nation oldNation = nations.get(currentNation);
        oldNation.setTotalPlayers(Math.max(0, oldNation.getTotalPlayers() - 1));
        Nation newNation = nations.get(newNationId);
        newNation.setTotalPlayers(newNation.getTotalPlayers() + 1);
        nationWarMapper.upsertPlayerNation(odUserId, newNationId, extractServerId(odUserId));
        updateMeritExchangeRate(currentNation);
        updateMeritExchangeRate(newNationId);
        logger.info("玩家 {} 从 {} 转国到 {}", odUserId, currentNation, newNationId);
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("oldNation", currentNation);
        result.put("newNation", newNationId);
        result.put("message", "转国成功！欢迎加入" + newNation.getName() + "国");
        return result;
    }

    public Map<String, Object> checkCanChangeNation(String odUserId) {
        String currentNation = getPlayerNation(odUserId);
        Map<String, Object> result = new HashMap<>();
        result.put("canChange", false);
        List<String> reasons = new ArrayList<>();
        if (currentNation == null) { reasons.add("您还未选择国家"); result.put("reasons", reasons); return result; }
        City chibi = cities.get(CHIBI_CITY_ID);
        boolean hasChibi = chibi != null && currentNation.equals(chibi.getOwner());
        result.put("hasChibi", hasChibi);
        if (!hasChibi) reasons.add("您的国家必须占领赤壁");
        Nation nation = nations.get(currentNation);
        int cc = nation.getCities().size();
        result.put("cityCount", cc);
        result.put("cityRequired", TRANSFER_CITY_REQUIREMENT);
        if (cc < TRANSFER_CITY_REQUIREMENT) reasons.add("城池数量不足");
        result.put("goldCost", TRANSFER_GOLD_COST);
        result.put("silverCost", TRANSFER_SILVER_COST);
        result.put("reasons", reasons);
        result.put("canChange", reasons.isEmpty());
        return result;
    }

    public List<City> getAttackableCities(String odUserId) {
        String playerNation = getPlayerNation(odUserId);
        if (playerNation == null) return new ArrayList<>();
        Set<String> attackableCityIds = new HashSet<>();
        Nation nation = nations.get(playerNation);
        for (String cityId : nation.getCities()) {
            City city = cities.get(cityId);
            if (city != null && city.getNeighbors() != null) {
                for (String neighborId : city.getNeighbors()) {
                    City neighbor = cities.get(neighborId);
                    if (neighbor != null && !neighbor.getOwner().equals(playerNation))
                        attackableCityIds.add(neighborId);
                }
            }
        }
        return attackableCityIds.stream().map(cities::get).filter(Objects::nonNull).collect(Collectors.toList());
    }

    public Nation getNation(String nationId) { return nations.get(nationId); }
    public List<Nation> getAllNations() { return new ArrayList<>(nations.values()); }
    public List<Nation> getPlayerSelectableNations() {
        List<Nation> list = new ArrayList<>();
        for (String id : PLAYER_SELECTABLE_NATION_IDS) {
            Nation n = nations.get(id); if (n != null) list.add(n);
        }
        return list;
    }
    public boolean isNeutralOwner(String owner) { return "NEUTRAL".equals(owner); }

    // ==================== 会话管理 ====================

    public NationWarSession getActiveSession() { return activeSession; }

    private String todayStr() { return new SimpleDateFormat("yyyy-MM-dd").format(new Date()); }

    private void saveSession(NationWarSession session) {
        String id = SESSION_ID_PREFIX + session.getDate();
        nationWarMapper.upsert(id, session.getDate(), JSON.toJSONString(session));
    }

    private NationWarSession createSession(String date) {
        NationWarSession s = NationWarSession.builder()
                .date(date).phase(SessionPhase.REGISTRATION)
                .nationTargets(new LinkedHashMap<>())
                .registrations(new LinkedHashMap<>())
                .cityBattles(new LinkedHashMap<>())
                .playerStates(new LinkedHashMap<>())
                .currentRound(0)
                .createTime(System.currentTimeMillis()).build();
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 19); cal.set(Calendar.MINUTE, 45); cal.set(Calendar.SECOND, 0);
        s.setPhaseStartTime(cal.getTimeInMillis());
        cal.set(Calendar.HOUR_OF_DAY, 20); cal.set(Calendar.MINUTE, 0);
        s.setPhaseEndTime(cal.getTimeInMillis());
        return s;
    }

    // ==================== 定时任务 ====================

    @Scheduled(cron = "0 45 19 * * *")
    public void scheduledStartRegistration() {
        if (testMode) return;
        startRegistration();
    }

    @Scheduled(cron = "0 0 20 * * *")
    public void scheduledFinalizeAndStartBattle() {
        if (testMode) return;
        finalizeRegistrationAndStartBattle();
    }

    @Scheduled(cron = "0 1-40 20 * * *")
    public void scheduledBattleTick() {
        if (testMode) return;
        battleTick();
    }

    @Scheduled(cron = "0 41 20 * * *")
    public void scheduledFinishAllBattles() {
        if (testMode) return;
        finishAllBattles();
    }

    public void startRegistration() {
        String today = todayStr();
        activeSession = createSession(today);
        saveSession(activeSession);
        logger.info("国战报名开始: {}", today);
    }

    // ==================== 报名逻辑 ====================

    public synchronized Map<String, Object> signUpV2(String odUserId, String playerName,
                                                      Integer level, Integer power, String targetCityId) {
        if (activeSession == null || activeSession.getPhase() != SessionPhase.REGISTRATION)
            throw new BusinessException(400, "当前不是报名时间");

        String playerNation = getPlayerNation(odUserId);
        if (playerNation == null) throw new BusinessException(400, "请先选择国家");

        City targetCity = cities.get(targetCityId);
        if (targetCity == null) throw new BusinessException(400, "目标城市不存在");
        if (targetCity.getOwner().equals(playerNation))
            throw new BusinessException(400, "不能进攻自己国家的城市");

        List<City> attackable = getAttackableCities(odUserId);
        boolean canAttack = attackable.stream().anyMatch(c -> c.getId().equals(targetCityId));
        if (!canAttack) throw new BusinessException(400, "只能进攻与本国接壤的城市");

        String lockedTarget = activeSession.getNationTargets().get(playerNation);
        if (lockedTarget != null && !lockedTarget.equals(targetCityId))
            throw new BusinessException(400, "本国已锁定进攻目标: " + cities.get(lockedTarget).getName()
                    + "，无法报名其他城市");

        boolean alreadySignedUp = false;
        for (CityRegistration reg : activeSession.getRegistrations().values()) {
            List<WarParticipant> list = reg.getNationSignups().get(playerNation);
            if (list != null && list.stream().anyMatch(p -> p.getOdUserId().equals(odUserId))) {
                alreadySignedUp = true; break;
            }
        }
        if (alreadySignedUp) throw new BusinessException(400, "已报名，不能重复报名");

        CityRegistration reg = activeSession.getRegistrations()
                .computeIfAbsent(targetCityId, k -> CityRegistration.builder()
                        .nationSignups(new LinkedHashMap<>()).build());
        List<WarParticipant> nationList = reg.getNationSignups()
                .computeIfAbsent(playerNation, k -> new ArrayList<>());

        WarParticipant participant = WarParticipant.builder()
                .odUserId(odUserId).playerName(playerName).nation(playerNation)
                .level(level).power(power).signUpTime(System.currentTimeMillis())
                .wins(0).losses(0).scoreGained(0).meritGained(0).eliminated(false).build();
        nationList.add(participant);

        int nationCount = nationList.size();
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("targetCity", targetCity.getName());
        result.put("nationSignupCount", nationCount);

        if (nationCount >= MIN_SIGN_UP && lockedTarget == null) {
            activeSession.getNationTargets().put(playerNation, targetCityId);
            removeNationFromOtherCities(playerNation, targetCityId);
            result.put("nationLocked", true);
            result.put("message", "报名成功！本国已锁定进攻" + targetCity.getName());
            logger.info("国家 {} 已锁定进攻目标: {}（报名满{}人）", playerNation, targetCityId, MIN_SIGN_UP);

            if (CHIBI_CITY_ID.equals(targetCityId)) {
                checkChibiLock();
            }
        } else {
            result.put("nationLocked", lockedTarget != null);
            result.put("message", "报名成功（" + nationCount + "/" + MIN_SIGN_UP + "）");
        }

        saveSession(activeSession);
        return result;
    }

    private void removeNationFromOtherCities(String nationId, String keepCityId) {
        for (Map.Entry<String, CityRegistration> entry : activeSession.getRegistrations().entrySet()) {
            if (entry.getKey().equals(keepCityId)) continue;
            entry.getValue().getNationSignups().remove(nationId);
        }
    }

    private void checkChibiLock() {
        CityRegistration chibiReg = activeSession.getRegistrations().get(CHIBI_CITY_ID);
        if (chibiReg == null) return;
        long lockedCount = chibiReg.getNationSignups().entrySet().stream()
                .filter(e -> {
                    String t = activeSession.getNationTargets().get(e.getKey());
                    return CHIBI_CITY_ID.equals(t);
                }).count();
        if (lockedCount >= 2) {
            logger.info("赤壁已有{}个国家锁定，关闭赤壁报名", lockedCount);
        }
    }

    // ==================== 报名截止 & 开战 ====================

    public synchronized void finalizeRegistrationAndStartBattle() {
        if (activeSession == null) {
            logger.info("无活跃会话，跳过开战");
            return;
        }
        if (activeSession.getPhase() != SessionPhase.REGISTRATION) {
            logger.info("会话状态非报名中，跳过: {}", activeSession.getPhase());
            return;
        }

        for (String nationId : PLAYER_SELECTABLE_NATION_IDS) {
            if (activeSession.getNationTargets().containsKey(nationId)) continue;
            String bestCity = null;
            int bestCount = 0;
            for (Map.Entry<String, CityRegistration> entry : activeSession.getRegistrations().entrySet()) {
                List<WarParticipant> list = entry.getValue().getNationSignups().get(nationId);
                int c = (list != null) ? list.size() : 0;
                if (c > bestCount) { bestCount = c; bestCity = entry.getKey(); }
            }
            if (bestCity != null && bestCount > 0) {
                activeSession.getNationTargets().put(nationId, bestCity);
                removeNationFromOtherCities(nationId, bestCity);
                logger.info("国家 {} 自动锁定进攻: {}（{}人）", nationId, bestCity, bestCount);
            }
        }

        if (CHIBI_CITY_ID.equals(findAny(activeSession.getNationTargets(), CHIBI_CITY_ID))) {
            resolveChibiNations();
        }

        createCityBattles();

        activeSession.setPhase(SessionPhase.BATTLE);
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 20); cal.set(Calendar.MINUTE, 0); cal.set(Calendar.SECOND, 0);
        activeSession.setPhaseStartTime(cal.getTimeInMillis());
        cal.set(Calendar.MINUTE, 40);
        activeSession.setPhaseEndTime(cal.getTimeInMillis());

        saveSession(activeSession);
        logger.info("国战开始，活跃战场数: {}", activeSession.getCityBattles().size());
    }

    private String findAny(Map<String, String> map, String value) {
        for (Map.Entry<String, String> e : map.entrySet()) {
            if (value.equals(e.getValue())) return value;
        }
        return null;
    }

    private void resolveChibiNations() {
        CityRegistration chibiReg = activeSession.getRegistrations().get(CHIBI_CITY_ID);
        if (chibiReg == null) return;
        List<Map.Entry<String, List<WarParticipant>>> sorted = chibiReg.getNationSignups().entrySet().stream()
                .filter(e -> e.getValue() != null && !e.getValue().isEmpty())
                .sorted((a, b) -> b.getValue().size() - a.getValue().size())
                .collect(Collectors.toList());

        if (sorted.size() < 2) {
            logger.info("赤壁报名国家不足2个，取消赤壁战场");
            activeSession.getNationTargets().entrySet()
                    .removeIf(e -> CHIBI_CITY_ID.equals(e.getValue()));
            return;
        }
        Set<String> top2 = new HashSet<>();
        top2.add(sorted.get(0).getKey());
        top2.add(sorted.get(1).getKey());
        for (String nationId : new ArrayList<>(activeSession.getNationTargets().keySet())) {
            if (CHIBI_CITY_ID.equals(activeSession.getNationTargets().get(nationId)) && !top2.contains(nationId)) {
                activeSession.getNationTargets().remove(nationId);
                logger.info("国家 {} 被排除出赤壁争夺", nationId);
            }
        }
        chibiReg.getNationSignups().entrySet().removeIf(e -> !top2.contains(e.getKey()));
    }

    private void createCityBattles() {
        Map<String, List<String>> cityToAttackers = new LinkedHashMap<>();
        for (Map.Entry<String, String> e : activeSession.getNationTargets().entrySet()) {
            cityToAttackers.computeIfAbsent(e.getValue(), k -> new ArrayList<>()).add(e.getKey());
        }

        for (Map.Entry<String, List<String>> entry : cityToAttackers.entrySet()) {
            String cityId = entry.getKey();
            List<String> attackNations = entry.getValue();
            City city = cities.get(cityId);
            if (city == null) continue;

            CityBattle battle;
            if (CHIBI_CITY_ID.equals(cityId) && attackNations.size() >= 2) {
                battle = CityBattle.builder()
                        .cityId(cityId).cityName(city.getName())
                        .sideANation(attackNations.get(0)).sideBNation(attackNations.get(1))
                        .sideAScore(0).sideBScore(0).victoryPoint(VICTORY_POINT)
                        .npcDefenders(new ArrayList<>())
                        .rounds(new ArrayList<>()).isChibiBattle(true).build();
            } else if (attackNations.size() == 1) {
                String attackNation = attackNations.get(0);
                battle = CityBattle.builder()
                        .cityId(cityId).cityName(city.getName())
                        .sideANation(attackNation).sideBNation(city.getOwner())
                        .sideAScore(0).sideBScore(0).victoryPoint(VICTORY_POINT)
                        .npcDefenders(generateNpcDefenders(city.getOwner(), cityId))
                        .rounds(new ArrayList<>()).isChibiBattle(false).build();
            } else {
                continue;
            }
            activeSession.getCityBattles().put(cityId, battle);
        }
    }

    private List<NpcDefender> generateNpcDefenders(String nation, String cityId) {
        List<NpcDefender> npcs = new ArrayList<>();
        String nationName = nations.get(nation) != null ? nations.get(nation).getName() : nation;
        for (int i = 0; i < NPC_DEFENDER_COUNT; i++) {
            npcs.add(NpcDefender.builder()
                    .npcId("NPC_DEF_" + cityId + "_" + i)
                    .name(nationName + "守军" + (i + 1))
                    .nation(nation).level(NPC_DEFENDER_LEVEL).power(NPC_DEFENDER_POWER)
                    .remainingSoldiers(100).dead(false).build());
        }
        return npcs;
    }

    // ==================== 玩家加入/切换城市 ====================

    public synchronized Map<String, Object> joinCity(String odUserId, String playerName,
                                                      Integer level, Integer power, String cityId) {
        if (activeSession == null || activeSession.getPhase() != SessionPhase.BATTLE)
            throw new BusinessException(400, "当前不是战斗阶段");

        String playerNation = getPlayerNation(odUserId);
        if (playerNation == null) throw new BusinessException(400, "请先选择国家");

        CityBattle battle = activeSession.getCityBattles().get(cityId);
        if (battle == null) throw new BusinessException(400, "该城市没有战场");
        if (battle.getWinner() != null) throw new BusinessException(400, "该城市战斗已结束");

        String side;
        if (playerNation.equals(battle.getSideANation())) {
            side = "ATTACK";
        } else if (playerNation.equals(battle.getSideBNation())) {
            side = "DEFEND";
        } else {
            throw new BusinessException(400, "该城市战场不涉及您的国家");
        }

        PlayerWarState existing = activeSession.getPlayerStates().get(odUserId);
        if (existing != null && existing.getCurrentCityId() != null) {
            throw new BusinessException(400, "您已在城市" + existing.getCurrentCityId() + "参战，需先切换");
        }

        int vipLevel = 0;
        try {
            UserResource ur = userResourceService.getUserResource(odUserId);
            vipLevel = ur.getVipLevel() != null ? ur.getVipLevel() : 0;
        } catch (Exception e) { /* ignore */ }

        Map<String, Integer> remainingSoldiers = new LinkedHashMap<>();
        Map<String, Integer> maxSoldiers = new LinkedHashMap<>();
        initPlayerSoldiers(odUserId, remainingSoldiers, maxSoldiers);

        PlayerWarState state = PlayerWarState.builder()
                .odUserId(odUserId).playerName(playerName).nation(playerNation)
                .level(level).power(power)
                .currentCityId(cityId).side(side)
                .roundsAtCurrentCity(0)
                .remainingSoldiers(remainingSoldiers).maxSoldiers(maxSoldiers)
                .allDead(false).canSwitch(false)
                .totalMerit(0).totalScore(0).wins(0).losses(0).byeCount(0)
                .vipLevel(vipLevel).build();

        activeSession.getPlayerStates().put(odUserId, state);
        saveSession(activeSession);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("cityId", cityId);
        result.put("side", side);
        result.put("message", "已加入" + battle.getCityName() + "战场（" + (side.equals("ATTACK") ? "进攻" : "防守") + "方）");
        return result;
    }

    private void initPlayerSoldiers(String odUserId, Map<String, Integer> remaining, Map<String, Integer> max) {
        try {
            List<General> generals = formationService.getBattleOrder(odUserId);
            for (General g : generals) {
                String key = g.getId() != null ? String.valueOf(g.getId()) : g.getName();
                int maxSc = g.getSoldierMaxCount() != null ? g.getSoldierMaxCount() : 100;
                int sc = g.getSoldierCount() != null ? Math.min(g.getSoldierCount(), maxSc) : maxSc;
                remaining.put(key, sc);
                max.put(key, maxSc);
            }
        } catch (Exception e) {
            remaining.put("default", 100);
            max.put("default", 100);
        }
    }

    public synchronized Map<String, Object> switchCity(String odUserId, String newCityId) {
        if (activeSession == null || activeSession.getPhase() != SessionPhase.BATTLE)
            throw new BusinessException(400, "当前不是战斗阶段");

        PlayerWarState state = activeSession.getPlayerStates().get(odUserId);
        if (state == null) throw new BusinessException(400, "您还未加入任何战场");

        int requiredRounds = getRequiredRoundsForSwitch(state.getVipLevel());
        boolean canSwitch = state.getAllDead() || state.getRoundsAtCurrentCity() >= requiredRounds;
        if (!canSwitch)
            throw new BusinessException(400, "还需" + (requiredRounds - state.getRoundsAtCurrentCity()) + "轮才能切换");

        CityBattle newBattle = activeSession.getCityBattles().get(newCityId);
        if (newBattle == null) throw new BusinessException(400, "目标城市没有战场");
        if (newBattle.getWinner() != null) throw new BusinessException(400, "目标城市战斗已结束");

        String playerNation = state.getNation();
        String side;
        if (playerNation.equals(newBattle.getSideANation())) {
            side = "ATTACK";
        } else if (playerNation.equals(newBattle.getSideBNation())) {
            side = "DEFEND";
        } else {
            throw new BusinessException(400, "该城市战场不涉及您的国家");
        }

        state.setCurrentCityId(newCityId);
        state.setSide(side);
        state.setRoundsAtCurrentCity(0);
        state.setCanSwitch(false);

        saveSession(activeSession);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("cityId", newCityId);
        result.put("side", side);
        result.put("message", "已切换到" + newBattle.getCityName());
        return result;
    }

    // ==================== VIP轮数减免 ====================

    public int getRequiredRoundsForSwitch(Integer vipLevel) {
        if (vipLevel == null) return DEFAULT_SWITCH_ROUNDS;
        if (vipLevel >= 10) return 0;
        if (vipLevel >= 8) return 1;
        if (vipLevel >= 3) return 2;
        return DEFAULT_SWITCH_ROUNDS;
    }

    // ==================== 首都距离加成 ====================

    public int calculateCityDistance(String fromCityId, String toCityId) {
        if (fromCityId.equals(toCityId)) return 0;
        Queue<String> queue = new LinkedList<>();
        Set<String> visited = new HashSet<>();
        queue.add(fromCityId);
        visited.add(fromCityId);
        int dist = 0;
        while (!queue.isEmpty()) {
            dist++;
            int sz = queue.size();
            for (int i = 0; i < sz; i++) {
                String cur = queue.poll();
                City city = cities.get(cur);
                if (city == null || city.getNeighbors() == null) continue;
                for (String nb : city.getNeighbors()) {
                    if (nb.equals(toCityId)) return dist;
                    if (visited.add(nb)) queue.add(nb);
                }
            }
        }
        return 999;
    }

    public double[] getCapitalBonus(String playerNation, String battleCityId) {
        if (CHIBI_CITY_ID.equals(battleCityId)) return new double[]{0, 0, 0};
        Nation nation = nations.get(playerNation);
        if (nation == null) return new double[]{0, 0, 0};
        int dist = calculateCityDistance(nation.getCapitalId(), battleCityId);
        double atkDef, mobility;
        switch (dist) {
            case 0: atkDef = 0.30; mobility = 0.20; break;
            case 1: atkDef = 0.20; mobility = 0.15; break;
            case 2: atkDef = 0.10; mobility = 0.10; break;
            case 3: atkDef = 0.05; mobility = 0.05; break;
            default: atkDef = 0; mobility = 0;
        }
        return new double[]{atkDef, atkDef, mobility};
    }

    // ==================== 每分钟战斗结算 ====================

    public synchronized void battleTick() {
        if (activeSession == null || activeSession.getPhase() != SessionPhase.BATTLE) return;

        activeSession.setCurrentRound(activeSession.getCurrentRound() + 1);
        int roundNum = activeSession.getCurrentRound();
        logger.info("国战第{}轮结算开始", roundNum);

        for (Map.Entry<String, CityBattle> entry : activeSession.getCityBattles().entrySet()) {
            String cityId = entry.getKey();
            CityBattle battle = entry.getValue();
            if (battle.getWinner() != null) continue;

            processCityBattleTick(cityId, battle, roundNum);

            if (battle.getSideAScore() >= battle.getVictoryPoint()) {
                battle.setWinner(battle.getSideANation());
                onCityBattleEnd(battle);
            } else if (battle.getSideBScore() >= battle.getVictoryPoint()) {
                battle.setWinner(battle.getSideBNation());
                onCityBattleEnd(battle);
            }
        }

        saveSession(activeSession);
        logger.info("国战第{}轮结算完成", roundNum);
    }

    private void processCityBattleTick(String cityId, CityBattle battle, int roundNum) {
        List<PlayerWarState> sideAPlayers = new ArrayList<>();
        List<PlayerWarState> sideBPlayers = new ArrayList<>();

        for (PlayerWarState ps : activeSession.getPlayerStates().values()) {
            if (!cityId.equals(ps.getCurrentCityId())) continue;
            if (ps.getAllDead()) continue;
            if ("ATTACK".equals(ps.getSide())) sideAPlayers.add(ps);
            else sideBPlayers.add(ps);
        }

        List<Object[]> sideBWithNpc = new ArrayList<>();
        for (PlayerWarState ps : sideBPlayers) sideBWithNpc.add(new Object[]{ps, null});
        if (!Boolean.TRUE.equals(battle.getIsChibiBattle())) {
            for (NpcDefender npc : battle.getNpcDefenders()) {
                if (!npc.getDead()) sideBWithNpc.add(new Object[]{null, npc});
            }
        }

        Collections.shuffle(sideAPlayers, new Random());
        Collections.shuffle(sideBWithNpc, new Random());

        RoundResult roundResult = RoundResult.builder()
                .roundNumber(roundNum).cityId(cityId).timestamp(System.currentTimeMillis())
                .fights(new ArrayList<>()).byes(new ArrayList<>()).build();

        int pairs = Math.min(sideAPlayers.size(), sideBWithNpc.size());

        for (int i = 0; i < pairs; i++) {
            PlayerWarState attacker = sideAPlayers.get(i);
            Object[] defEntry = sideBWithNpc.get(i);
            PlayerWarState defender = (PlayerWarState) defEntry[0];
            NpcDefender npcDef = (NpcDefender) defEntry[1];

            fightPair(attacker, defender, npcDef, battle, cityId, roundResult);
        }

        for (int i = pairs; i < sideAPlayers.size(); i++) {
            PlayerWarState p = sideAPlayers.get(i);
            int merit = p.getLevel() != null ? p.getLevel() : 1;
            int score = (p.getLevel() != null ? p.getLevel() : 1) * 5;
            p.setTotalMerit(p.getTotalMerit() + merit);
            p.setTotalScore(p.getTotalScore() + score);
            p.setByeCount(p.getByeCount() + 1);
            battle.setSideAScore(battle.getSideAScore() + score);
            addPlayerMerit(p.getOdUserId(), merit);
            roundResult.getByes().add(RoundBye.builder()
                    .playerId(p.getOdUserId()).playerName(p.getPlayerName())
                    .level(p.getLevel()).side("ATTACK").meritGained(merit).scoreGained(score).build());
        }

        for (int i = pairs; i < sideBWithNpc.size(); i++) {
            Object[] defEntry = sideBWithNpc.get(i);
            PlayerWarState p = (PlayerWarState) defEntry[0];
            if (p != null) {
                int merit = p.getLevel() != null ? p.getLevel() : 1;
                int score = (p.getLevel() != null ? p.getLevel() : 1) * 5;
                p.setTotalMerit(p.getTotalMerit() + merit);
                p.setTotalScore(p.getTotalScore() + score);
                p.setByeCount(p.getByeCount() + 1);
                battle.setSideBScore(battle.getSideBScore() + score);
                addPlayerMerit(p.getOdUserId(), merit);
                roundResult.getByes().add(RoundBye.builder()
                        .playerId(p.getOdUserId()).playerName(p.getPlayerName())
                        .level(p.getLevel()).side("DEFEND").meritGained(merit).scoreGained(score).build());
            }
        }

        for (PlayerWarState ps : sideAPlayers) updatePlayerSwitchEligibility(ps);
        for (PlayerWarState ps : sideBPlayers) updatePlayerSwitchEligibility(ps);

        roundResult.setSideAScoreAfter(battle.getSideAScore());
        roundResult.setSideBScoreAfter(battle.getSideBScore());
        battle.getRounds().add(roundResult);
    }

    private void fightPair(PlayerWarState attacker, PlayerWarState defender,
                           NpcDefender npcDef, CityBattle battle, String cityId,
                           RoundResult roundResult) {
        List<BattleCalculator.BattleUnit> sideA = buildUnitsFromState(attacker, cityId);
        List<BattleCalculator.BattleUnit> sideB;
        String defenderId, defenderName;
        int defenderLevel;

        if (defender != null) {
            sideB = buildUnitsFromState(defender, cityId);
            defenderId = defender.getOdUserId();
            defenderName = defender.getPlayerName();
            defenderLevel = defender.getLevel() != null ? defender.getLevel() : 1;
        } else {
            sideB = buildNpcUnits(npcDef);
            defenderId = npcDef.getNpcId();
            defenderName = npcDef.getName();
            defenderLevel = npcDef.getLevel();
        }

        BattleService.BattleReport report = battleService.fight(sideA, sideB, 20);
        boolean attackerWins = report.victoryA;

        updateSoldiersAfterBattle(attacker, sideA);
        if (defender != null) {
            updateSoldiersAfterBattle(defender, sideB);
        } else {
            int remaining = sideB.stream().mapToInt(u -> Math.max(0, u.soldierCount)).sum();
            npcDef.setRemainingSoldiers(remaining);
            if (remaining <= 0) npcDef.setDead(true);
        }

        int attackerLevel = attacker.getLevel() != null ? attacker.getLevel() : 1;

        if (attackerWins) {
            int merit = defenderLevel * 2;
            int score = defenderLevel * 10;
            attacker.setWins(attacker.getWins() + 1);
            attacker.setTotalMerit(attacker.getTotalMerit() + merit);
            attacker.setTotalScore(attacker.getTotalScore() + score);
            battle.setSideAScore(battle.getSideAScore() + score);
            addPlayerMerit(attacker.getOdUserId(), merit);
            if (defender != null) {
                int lossMerit = defenderLevel;
                defender.setLosses(defender.getLosses() + 1);
                defender.setTotalMerit(defender.getTotalMerit() + lossMerit);
                addPlayerMerit(defender.getOdUserId(), lossMerit);
            }
            roundResult.getFights().add(RoundFight.builder()
                    .attackerId(attacker.getOdUserId()).attackerName(attacker.getPlayerName()).attackerLevel(attackerLevel)
                    .defenderId(defenderId).defenderName(defenderName).defenderLevel(defenderLevel)
                    .winnerId(attacker.getOdUserId()).winnerName(attacker.getPlayerName())
                    .meritGained(merit).scoreGained(score).build());
        } else {
            int lossMerit = attackerLevel;
            attacker.setLosses(attacker.getLosses() + 1);
            attacker.setTotalMerit(attacker.getTotalMerit() + lossMerit);
            addPlayerMerit(attacker.getOdUserId(), lossMerit);
            if (defender != null) {
                int merit = attackerLevel * 2;
                int score = attackerLevel * 10;
                defender.setWins(defender.getWins() + 1);
                defender.setTotalMerit(defender.getTotalMerit() + merit);
                defender.setTotalScore(defender.getTotalScore() + score);
                battle.setSideBScore(battle.getSideBScore() + score);
                addPlayerMerit(defender.getOdUserId(), merit);
                roundResult.getFights().add(RoundFight.builder()
                        .attackerId(attacker.getOdUserId()).attackerName(attacker.getPlayerName()).attackerLevel(attackerLevel)
                        .defenderId(defenderId).defenderName(defenderName).defenderLevel(defenderLevel)
                        .winnerId(defender.getOdUserId()).winnerName(defender.getPlayerName())
                        .meritGained(merit).scoreGained(score).build());
            } else {
                battle.setSideBScore(battle.getSideBScore() + attackerLevel * 10);
                roundResult.getFights().add(RoundFight.builder()
                        .attackerId(attacker.getOdUserId()).attackerName(attacker.getPlayerName()).attackerLevel(attackerLevel)
                        .defenderId(defenderId).defenderName(defenderName).defenderLevel(defenderLevel)
                        .winnerId(defenderId).winnerName(defenderName)
                        .meritGained(0).scoreGained(attackerLevel * 10).build());
            }
        }
    }

    private List<BattleCalculator.BattleUnit> buildUnitsFromState(PlayerWarState ps, String cityId) {
        String odUserId = ps.getOdUserId();
        double[] bonus = getCapitalBonus(ps.getNation(), cityId);
        double atkDefBonus = bonus[0];
        double mobilityBonus = bonus[2];

        try {
            List<General> generals = formationService.getBattleOrder(odUserId);
            if (!generals.isEmpty()) {
                List<BattleCalculator.BattleUnit> units = new ArrayList<>();
                for (int i = 0; i < generals.size(); i++) {
                    General g = generals.get(i);
                    String key = g.getId() != null ? String.valueOf(g.getId()) : g.getName();
                    int remaining = ps.getRemainingSoldiers().getOrDefault(key, 0);
                    if (remaining <= 0) continue;

                    Map<String, Integer> eq = suitConfigService.calculateTotalEquipBonus(g.getId());
                    int rawTier = g.getSoldierTier() != null ? g.getSoldierTier() : 1;
                    int sRank = g.getSoldierRank() != null ? g.getSoldierRank() : 1;
                    int tier = Math.max(rawTier, sRank);
                    int troopType = BattleCalculator.parseTroopType(g.getTroopType());
                    int maxSc = g.getSoldierMaxCount() != null ? g.getSoldierMaxCount() : 100;
                    int formLv = BattleCalculator.maxPeopleToFormationLevel(maxSc);

                    int baseAtk = g.getAttrAttack() != null ? g.getAttrAttack() : 100;
                    int baseDef = g.getAttrDefense() != null ? g.getAttrDefense() : 50;
                    int baseMob = g.getAttrMobility() != null ? g.getAttrMobility() : 15;
                    int atk = (int)(baseAtk * (1 + atkDefBonus));
                    int def = (int)(baseDef * (1 + atkDefBonus));
                    int mob = (int)(baseMob * (1 + mobilityBonus));

                    BattleCalculator.BattleUnit u = BattleCalculator.assembleBattleUnit(
                            g.getName() != null ? g.getName() : ps.getPlayerName(),
                            g.getLevel() != null ? g.getLevel() : ps.getLevel(),
                            atk, def,
                            g.getAttrValor() != null ? g.getAttrValor() : 10,
                            g.getAttrCommand() != null ? g.getAttrCommand() : 10,
                            g.getAttrDodge() != null ? (int) Math.round(g.getAttrDodge()) : 5,
                            mob, troopType, tier, remaining, maxSc, formLv,
                            eq.getOrDefault("attack", 0), eq.getOrDefault("defense", 0),
                            eq.getOrDefault("speed", 0), eq.getOrDefault("hit", 0),
                            eq.getOrDefault("dodge", 0), 0, 0, 0);
                    u.position = i;
                    generalService.applyFamousTraitsToUnit(u, g.getName(), troopType);
                    units.add(u);
                }
                if (!units.isEmpty()) return units;
            }
        } catch (Exception e) {
            logger.warn("构建国战参战者阵型失败: {}", odUserId, e);
        }

        int level = ps.getLevel() != null ? ps.getLevel() : 30;
        int power = ps.getPower() != null ? ps.getPower() : 5000;
        int remaining = ps.getRemainingSoldiers().values().stream().mapToInt(Integer::intValue).sum();
        if (remaining <= 0) remaining = 100;
        int tier = Math.max(1, Math.min(10, 1 + level / 20));
        int formLv = BattleCalculator.levelToFormationLevel(level);
        int maxSc = BattleCalculator.getFormationMaxPeople(formLv);
        int atk = (int)((power / 2) * (1 + atkDefBonus));
        int def = (int)((power / 3) * (1 + atkDefBonus));
        int mob = (int)(15 * (1 + mobilityBonus));
        BattleCalculator.BattleUnit u = BattleCalculator.assembleBattleUnit(
                ps.getPlayerName(), level, atk, def, level * 2, level * 2, 5, mob,
                1, tier, remaining, maxSc, formLv, 0, 0, 0, 0, 0, 0, 0, 0);
        u.position = 0;
        return Collections.singletonList(u);
    }

    private List<BattleCalculator.BattleUnit> buildNpcUnits(NpcDefender npc) {
        BattleCalculator.BattleUnit u = BattleCalculator.assembleBattleUnit(
                npc.getName(), npc.getLevel(),
                npc.getPower() / 2, npc.getPower() / 3,
                npc.getLevel(), npc.getLevel(), 3, 10,
                1, 1, npc.getRemainingSoldiers(), 100, 1,
                0, 0, 0, 0, 0, 0, 0, 0);
        u.position = 0;
        return Collections.singletonList(u);
    }

    private void updateSoldiersAfterBattle(PlayerWarState ps, List<BattleCalculator.BattleUnit> units) {
        Map<String, Integer> remaining = ps.getRemainingSoldiers();
        List<String> keys = new ArrayList<>(remaining.keySet());
        int idx = 0;
        for (BattleCalculator.BattleUnit u : units) {
            if (idx < keys.size()) {
                remaining.put(keys.get(idx), Math.max(0, u.soldierCount));
            }
            idx++;
        }
        boolean allDead = remaining.values().stream().allMatch(v -> v <= 0);
        ps.setAllDead(allDead);
    }

    private void updatePlayerSwitchEligibility(PlayerWarState ps) {
        ps.setRoundsAtCurrentCity(ps.getRoundsAtCurrentCity() + 1);
        int required = getRequiredRoundsForSwitch(ps.getVipLevel());
        ps.setCanSwitch(ps.getAllDead() || ps.getRoundsAtCurrentCity() >= required);
    }

    // ==================== 战斗结束 ====================

    public synchronized void finishAllBattles() {
        if (activeSession == null || activeSession.getPhase() != SessionPhase.BATTLE) return;

        for (CityBattle battle : activeSession.getCityBattles().values()) {
            if (battle.getWinner() != null) continue;
            if (battle.getSideAScore() >= battle.getSideBScore()) {
                battle.setWinner(battle.getSideANation());
            } else {
                battle.setWinner(battle.getSideBNation());
            }
            onCityBattleEnd(battle);
        }

        activeSession.setPhase(SessionPhase.FINISHED);
        saveSession(activeSession);
        logger.info("国战全部结束");
    }

    private void onCityBattleEnd(CityBattle battle) {
        if (Boolean.TRUE.equals(battle.getIsChibiBattle())) {
            transferCity(battle.getCityId(), battle.getWinner());
        } else {
            if (battle.getWinner().equals(battle.getSideANation())) {
                transferCity(battle.getCityId(), battle.getSideANation());
            }
        }
        logger.info("城市 {} 战斗结束，胜利方: {}", battle.getCityName(), battle.getWinner());
    }

    // ==================== 城市转让/军功/兑换 ====================

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
            nationWarMapper.upsertCityOwner(cityId, newOwner, 1, System.currentTimeMillis());
        } catch (Exception e) {
            logger.error("持久化城市归属失败: {}", cityId, e);
        }
        logger.info("城市 {} 所有权从 {} 转移到 {}", city.getName(), oldOwner, newOwner);
    }

    private void updateMeritExchangeRate(String nationId) {
        Nation nation = nations.get(nationId);
        if (nation == null) return;
        nation.setMeritExchangeRate(1.0 + (nation.getCities().size() - 6) * 0.1);
    }

    private void addPlayerMerit(String odUserId, int merit) {
        if (odUserId == null || odUserId.startsWith("NPC_")) return;
        Integer current = nationWarMapper.findPlayerMerit(odUserId);
        nationWarMapper.upsertPlayerMerit(odUserId, (current != null ? current : 0) + merit);
    }

    public int getPlayerMerit(String odUserId) {
        Integer merit = nationWarMapper.findPlayerMerit(odUserId);
        return merit != null ? merit : 0;
    }

    public Map<String, Object> exchangeMerit(String odUserId, int meritAmount) {
        int currentMerit = getPlayerMerit(odUserId);
        if (meritAmount > currentMerit) throw new BusinessException(400, "军功不足");
        String playerNation = getPlayerNation(odUserId);
        if (playerNation == null) throw new BusinessException(400, "请先选择国家");
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
        return result;
    }

    // ==================== 查询接口 ====================

    public Map<String, Object> getSessionOverview() {
        Map<String, Object> result = new HashMap<>();
        if (activeSession == null) {
            result.put("active", false);
            return result;
        }
        result.put("active", true);
        result.put("phase", activeSession.getPhase().name());
        result.put("date", activeSession.getDate());
        result.put("currentRound", activeSession.getCurrentRound());
        result.put("nationTargets", activeSession.getNationTargets());

        Map<String, Object> regSummary = new LinkedHashMap<>();
        for (Map.Entry<String, CityRegistration> e : activeSession.getRegistrations().entrySet()) {
            Map<String, Integer> counts = new LinkedHashMap<>();
            for (Map.Entry<String, List<WarParticipant>> ne : e.getValue().getNationSignups().entrySet()) {
                counts.put(ne.getKey(), ne.getValue().size());
            }
            regSummary.put(e.getKey(), counts);
        }
        result.put("registrations", regSummary);

        Map<String, Object> battlesSummary = new LinkedHashMap<>();
        for (Map.Entry<String, CityBattle> e : activeSession.getCityBattles().entrySet()) {
            CityBattle b = e.getValue();
            Map<String, Object> bs = new LinkedHashMap<>();
            bs.put("cityName", b.getCityName());
            bs.put("sideA", b.getSideANation());
            bs.put("sideB", b.getSideBNation());
            bs.put("sideAScore", b.getSideAScore());
            bs.put("sideBScore", b.getSideBScore());
            bs.put("victoryPoint", b.getVictoryPoint());
            bs.put("winner", b.getWinner());
            bs.put("isChibi", b.getIsChibiBattle());
            bs.put("roundCount", b.getRounds().size());
            battlesSummary.put(e.getKey(), bs);
        }
        result.put("cityBattles", battlesSummary);
        result.put("playerCount", activeSession.getPlayerStates().size());
        return result;
    }

    public Map<String, Object> getBattleState(String odUserId) {
        Map<String, Object> result = new HashMap<>();
        if (activeSession == null) {
            result.put("active", false);
            return result;
        }
        result.put("active", true);
        result.put("phase", activeSession.getPhase().name());
        result.put("currentRound", activeSession.getCurrentRound());

        PlayerWarState ps = activeSession.getPlayerStates().get(odUserId);
        if (ps != null) {
            result.put("playerState", ps);
            CityBattle battle = activeSession.getCityBattles().get(ps.getCurrentCityId());
            if (battle != null) {
                result.put("currentBattle", battle);
            }
        } else {
            result.put("playerState", null);
        }

        List<Map<String, Object>> availCities = new ArrayList<>();
        String playerNation = getPlayerNation(odUserId);
        if (playerNation != null) {
            for (Map.Entry<String, CityBattle> e : activeSession.getCityBattles().entrySet()) {
                CityBattle b = e.getValue();
                if (b.getWinner() != null) continue;
                if (playerNation.equals(b.getSideANation()) || playerNation.equals(b.getSideBNation())) {
                    Map<String, Object> ci = new LinkedHashMap<>();
                    ci.put("cityId", b.getCityId());
                    ci.put("cityName", b.getCityName());
                    ci.put("side", playerNation.equals(b.getSideANation()) ? "ATTACK" : "DEFEND");
                    ci.put("sideAScore", b.getSideAScore());
                    ci.put("sideBScore", b.getSideBScore());
                    availCities.add(ci);
                }
            }
        }
        result.put("availableCities", availCities);
        return result;
    }

    public NationWar getTodayWar(String cityId) {
        String today = todayStr();
        String warId = today + "_" + cityId;
        String data = nationWarMapper.findById(warId);
        if (data == null) return null;
        return JSON.parseObject(data, NationWar.class);
    }

    public List<NationWar> getActiveWars() {
        List<Map<String, Object>> rows = nationWarMapper.findAll();
        List<NationWar> result = new ArrayList<>();
        if (rows != null) {
            for (Map<String, Object> row : rows) {
                String data = (String) row.get("data");
                if (data != null && !data.contains("\"phase\"")) {
                    NationWar war = JSON.parseObject(data, NationWar.class);
                    if (war.getStatus() != WarStatus.FINISHED) result.add(war);
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
                if (data != null && !data.contains("\"phase\"")) {
                    NationWar war = JSON.parseObject(data, NationWar.class);
                    if (war.getStatus() == WarStatus.FINISHED) result.add(war);
                }
            }
        }
        result.sort((a, b) -> Long.compare(b.getCreateTime(), a.getCreateTime()));
        return result.stream().limit(limit).collect(Collectors.toList());
    }

    // ==================== 测试工具 ====================

    public Map<String, Object> quickTest(String odUserId, String playerName,
                                          String targetCityId, int npcAttackerCount) {
        String playerNation = getPlayerNation(odUserId);
        if (playerNation == null) throw new BusinessException(400, "请先选择国家");

        testMode = true;
        try {
            String today = todayStr();
            activeSession = createSession(today);

            if (targetCityId == null || targetCityId.isEmpty()) {
                for (City city : cities.values()) {
                    if (!city.getOwner().equals(playerNation) && !city.getOwner().equals("NEUTRAL")) {
                        targetCityId = city.getId();
                        break;
                    }
                }
            }
            City targetCity = cities.get(targetCityId);
            if (targetCity == null) throw new BusinessException(400, "目标城市不存在");

            CityRegistration reg = CityRegistration.builder()
                    .nationSignups(new LinkedHashMap<>()).build();
            List<WarParticipant> attackList = new ArrayList<>();
            attackList.add(WarParticipant.builder()
                    .odUserId(odUserId).playerName(playerName).nation(playerNation)
                    .level(50).power(20000).signUpTime(System.currentTimeMillis())
                    .wins(0).losses(0).scoreGained(0).meritGained(0).eliminated(false).build());

            Random rng = new Random();
            String[] surnames = {"张","王","李","赵","刘","陈","杨","黄"};
            String[] givenNames = {"飞","云","龙","虎","豹","鹰","风","雷"};
            for (int i = 0; i < npcAttackerCount; i++) {
                String name = surnames[rng.nextInt(surnames.length)] + givenNames[rng.nextInt(givenNames.length)] + (rng.nextInt(90)+10);
                attackList.add(WarParticipant.builder()
                        .odUserId("NPC_ATK_" + i).playerName(name).nation(playerNation)
                        .level(20 + rng.nextInt(30)).power(5000 + rng.nextInt(10000))
                        .signUpTime(System.currentTimeMillis())
                        .wins(0).losses(0).scoreGained(0).meritGained(0).eliminated(false).build());
            }
            reg.getNationSignups().put(playerNation, attackList);
            activeSession.getRegistrations().put(targetCityId, reg);
            activeSession.getNationTargets().put(playerNation, targetCityId);

            CityBattle battle = CityBattle.builder()
                    .cityId(targetCityId).cityName(targetCity.getName())
                    .sideANation(playerNation).sideBNation(targetCity.getOwner())
                    .sideAScore(0).sideBScore(0).victoryPoint(VICTORY_POINT)
                    .npcDefenders(generateNpcDefenders(targetCity.getOwner(), targetCityId))
                    .rounds(new ArrayList<>()).isChibiBattle(false).build();
            activeSession.getCityBattles().put(targetCityId, battle);

            PlayerWarState myState = PlayerWarState.builder()
                    .odUserId(odUserId).playerName(playerName).nation(playerNation)
                    .level(50).power(20000).currentCityId(targetCityId).side("ATTACK")
                    .roundsAtCurrentCity(0)
                    .remainingSoldiers(new LinkedHashMap<>()).maxSoldiers(new LinkedHashMap<>())
                    .allDead(false).canSwitch(false).totalMerit(0).totalScore(0)
                    .wins(0).losses(0).byeCount(0).vipLevel(0).build();
            initPlayerSoldiers(odUserId, myState.getRemainingSoldiers(), myState.getMaxSoldiers());
            activeSession.getPlayerStates().put(odUserId, myState);

            for (WarParticipant npcAtk : attackList) {
                if (npcAtk.getOdUserId().startsWith("NPC_")) {
                    PlayerWarState npcState = PlayerWarState.builder()
                            .odUserId(npcAtk.getOdUserId()).playerName(npcAtk.getPlayerName()).nation(playerNation)
                            .level(npcAtk.getLevel()).power(npcAtk.getPower())
                            .currentCityId(targetCityId).side("ATTACK").roundsAtCurrentCity(0)
                            .remainingSoldiers(new LinkedHashMap<String, Integer>(){{ put("default", 100); }})
                            .maxSoldiers(new LinkedHashMap<String, Integer>(){{ put("default", 100); }})
                            .allDead(false).canSwitch(false).totalMerit(0).totalScore(0)
                            .wins(0).losses(0).byeCount(0).vipLevel(0).build();
                    activeSession.getPlayerStates().put(npcAtk.getOdUserId(), npcState);
                }
            }

            activeSession.setPhase(SessionPhase.BATTLE);

            int maxTicks = 40;
            for (int t = 0; t < maxTicks; t++) {
                battleTick();
                if (battle.getWinner() != null) break;
            }
            if (battle.getWinner() == null) finishAllBattles();

            Map<String, Object> result = new HashMap<>();
            result.put("message", "国战一键测试完成");
            result.put("targetCity", targetCity.getName());
            result.put("winner", battle.getWinner());
            result.put("sideAScore", battle.getSideAScore());
            result.put("sideBScore", battle.getSideBScore());
            result.put("rounds", battle.getRounds().size());
            PlayerWarState myResult = activeSession.getPlayerStates().get(odUserId);
            if (myResult != null) {
                result.put("myWins", myResult.getWins());
                result.put("myLosses", myResult.getLosses());
                result.put("myMerit", myResult.getTotalMerit());
            }
            return result;
        } finally {
            testMode = false;
        }
    }

    public void resetMap() {
        nations.clear();
        cities.clear();
        initMapData();
        activeSession = null;
        logger.info("国战地图已重置为初始状态");
    }

    /** 兼容旧接口: 保留signUp但路由到signUpV2 */
    public Map<String, Object> signUp(String odUserId, String playerName,
                                       Integer level, Integer power, String targetCityId) {
        return signUpV2(odUserId, playerName, level, power, targetCityId);
    }

    /** 兼容旧接口 */
    public Map<String, Object> getWarStatus(String warId) {
        String data = nationWarMapper.findById(warId);
        if (data == null) throw new BusinessException(404, "国战不存在");
        NationWar war = JSON.parseObject(data, NationWar.class);
        Map<String, Object> result = new HashMap<>();
        result.put("war", war);
        result.put("attackerCount", war.getAttackers() != null ? war.getAttackers().size() : 0);
        result.put("defenderCount", war.getDefenders() != null ? war.getDefenders().size() : 0);
        return result;
    }

    /** 兼容旧接口 */
    public void startWarBattle(String warId) {
        logger.info("旧接口startWarBattle已废弃，请使用新会话系统");
    }
}
