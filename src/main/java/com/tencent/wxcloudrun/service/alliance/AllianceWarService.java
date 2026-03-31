package com.tencent.wxcloudrun.service.alliance;

import com.alibaba.fastjson.JSON;
import com.tencent.wxcloudrun.config.TacticsConfig;
import com.tencent.wxcloudrun.dao.AllianceWarMapper;
import com.tencent.wxcloudrun.dao.UserTacticsMapper;
import com.tencent.wxcloudrun.exception.BusinessException;
import com.tencent.wxcloudrun.model.Alliance;
import com.tencent.wxcloudrun.model.AllianceWar;
import com.tencent.wxcloudrun.model.AllianceWar.*;
import com.tencent.wxcloudrun.model.General;
import com.tencent.wxcloudrun.service.SuitConfigService;
import com.tencent.wxcloudrun.service.battle.BattleCalculator;
import com.tencent.wxcloudrun.service.battle.BattleService;
import com.tencent.wxcloudrun.service.formation.FormationService;
import com.tencent.wxcloudrun.service.mail.MailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.LinkedHashMap;
import java.util.stream.Collectors;

/**
 * 联盟战服务（数据库存储）
 */
@Slf4j
@Service
public class AllianceWarService {
    private static final int ROUND_DURATION_MINUTES = 5;
    
    @Autowired
    private AllianceService allianceService;
    
    @Autowired
    private AllianceWarMapper allianceWarMapper;

    @Autowired
    private BattleService battleService;

    @Autowired
    private FormationService formationService;

    @Autowired
    private SuitConfigService suitConfigService;

    @Autowired
    private MailService mailService;

    @Autowired
    @org.springframework.context.annotation.Lazy
    private com.tencent.wxcloudrun.service.general.GeneralService generalService;

    @Autowired
    private UserTacticsMapper userTacticsMapper;

    @Autowired
    private TacticsConfig tacticsConfig;
    
    // 今日盟战（内存缓存，同时持久化到数据库）
    private AllianceWar todayWar;
    
    // 测试模式：设为true时可以随时触发盟战
    private boolean testMode = true;
    
    @PostConstruct
    public void init() {
        String today = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
        // 先尝试从数据库加载
        String data = allianceWarMapper.findByDate(today);
        if (data != null) {
            todayWar = JSON.parseObject(data, AllianceWar.class);
            log.info("从数据库加载今日盟战: {}", today);
        } else {
            todayWar = AllianceWar.createNew(today);
            saveTodayWar();
            log.info("初始化今日盟战: {}", today);
        }
    }
    
    private void saveTodayWar() {
        allianceWarMapper.upsert(todayWar.getDate(), JSON.toJSONString(todayWar));
    }

    private void ensureRewardPools() {
        if (todayWar.getAllianceRewardPools() == null) {
            todayWar.setAllianceRewardPools(new ArrayList<>());
        }
    }
    
    /**
     * 获取今日盟战信息
     */
    public AllianceWar getTodayWar() {
        return todayWar;
    }
    
    /**
     * 获取盟战状态
     */
    public Map<String, Object> getWarStatus(String odUserId) {
        Map<String, Object> result = new HashMap<>();
        ensureRewardPools();
        
        result.put("war", todayWar);
        result.put("status", todayWar.getStatus().name());
        result.put("currentRound", todayWar.getCurrentRound());
        result.put("participantCount", todayWar.getParticipants().size());
        result.put("participants", todayWar.getParticipants());
        result.put("roundDurationMinutes", todayWar.getRoundDurationMinutes());
        result.put("nextRoundTime", todayWar.getNextRoundTime());
        result.put("allianceRewardPools", todayWar.getAllianceRewardPools());
        
        boolean registered = todayWar.getParticipants().stream()
                .anyMatch(p -> p.getOdUserId().equals(odUserId));
        result.put("registered", registered);
        
        if (registered) {
            WarParticipant participant = todayWar.getParticipants().stream()
                    .filter(p -> p.getOdUserId().equals(odUserId))
                    .findFirst()
                    .orElse(null);
            result.put("myParticipant", participant);
        }
        
        result.put("nextEvent", getNextPhaseInfo());
        
        return result;
    }

    public Map<String, Object> getMyAllianceRewardPool(String odUserId) {
        ensureRewardPools();
        Alliance alliance = allianceService.getUserAlliance(odUserId);
        if (alliance == null) throw new BusinessException("请先加入联盟");
        Map<String, Object> result = new LinkedHashMap<>();
        AllianceRewardPool pool = todayWar.getAllianceRewardPools().stream()
                .filter(p -> Objects.equals(p.getAllianceId(), alliance.getId()))
                .findFirst().orElse(null);
        boolean isLeader = Objects.equals(alliance.getLeaderId(), odUserId);
        result.put("allianceId", alliance.getId());
        result.put("allianceName", alliance.getName());
        result.put("isLeader", isLeader);
        result.put("pool", pool);
        result.put("members", alliance.getMembers());
        return result;
    }

    public Map<String, Object> distributeAllianceReward(String leaderUserId, List<Map<String, Object>> allocations) {
        ensureRewardPools();
        Alliance alliance = allianceService.getUserAlliance(leaderUserId);
        if (alliance == null) throw new BusinessException("请先加入联盟");
        if (!Objects.equals(alliance.getLeaderId(), leaderUserId)) {
            throw new BusinessException("只有盟主可以分配联盟奖励");
        }
        AllianceRewardPool pool = todayWar.getAllianceRewardPools().stream()
                .filter(p -> Objects.equals(p.getAllianceId(), alliance.getId()))
                .findFirst().orElse(null);
        if (pool == null) throw new BusinessException("本盟暂无待分配奖励");
        if (Boolean.TRUE.equals(pool.getDistributed())) throw new BusinessException("奖励已分配");
        if (allocations == null || allocations.isEmpty()) throw new BusinessException("请先填写分配方案");

        Map<String, Integer> available = toItemCountMap(pool.getRewards());
        Map<String, String> nameToId = toItemNameToIdMap(pool.getRewards());
        Set<String> memberIds = alliance.getMembers().stream()
                .map(Alliance.AllianceMember::getUserId)
                .collect(Collectors.toSet());
        List<RewardDistribution> records = new ArrayList<>();

        for (Map<String, Object> row : allocations) {
            String userId = row.get("userId") != null ? String.valueOf(row.get("userId")) : null;
            if (userId == null || !memberIds.contains(userId)) {
                throw new BusinessException("分配对象必须是本盟成员");
            }
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> items = (List<Map<String, Object>>) row.get("items");
            if (items == null || items.isEmpty()) continue;

            List<Map<String, Object>> userAtts = new ArrayList<>();
            for (Map<String, Object> item : items) {
                String itemName = item.get("itemName") != null ? String.valueOf(item.get("itemName")) : null;
                if (itemName == null) throw new BusinessException("itemName不能为空");
                int count = item.get("count") != null ? ((Number) item.get("count")).intValue() : 0;
                if (count <= 0) continue;
                int left = available.getOrDefault(itemName, 0);
                if (count > left) throw new BusinessException(itemName + "分配超出可用数量");
                available.put(itemName, left - count);
                int itemId = Integer.parseInt(nameToId.getOrDefault(itemName, "0"));
                userAtts.addAll(buildRewardAttachments(itemName, itemId, count));
            }
            if (!userAtts.isEmpty()) {
                String userName = alliance.getMembers().stream()
                        .filter(m -> Objects.equals(m.getUserId(), userId))
                        .map(Alliance.AllianceMember::getName)
                        .findFirst().orElse(userId);
                records.add(RewardDistribution.builder()
                        .userId(userId)
                        .userName(userName)
                        .rewards(userAtts)
                        .allocateTime(System.currentTimeMillis())
                        .build());
            }
        }

        if (records.isEmpty()) throw new BusinessException("没有有效分配内容");

        for (RewardDistribution rd : records) {
            if (rd.getUserId() == null || rd.getUserId().startsWith("NPC_")) continue;
            mailService.sendSystemMail(rd.getUserId(), "盟战联盟奖励分配",
                    "盟主已完成盟战奖励分配，请查收附件。", rd.getRewards());
        }

        pool.setDistributed(true);
        pool.setDistributedBy(leaderUserId);
        pool.setDistributeTime(System.currentTimeMillis());
        pool.setDistributions(records);
        saveTodayWar();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);
        result.put("distributedCount", records.size());
        result.put("pool", pool);
        return result;
    }
    
    private String getNextPhaseInfo() {
        Calendar cal = Calendar.getInstance();
        int hour = cal.get(Calendar.HOUR_OF_DAY);
        int minute = cal.get(Calendar.MINUTE);
        
        if (hour < 20 || (hour == 20 && minute < 45)) {
            return "报名开始 20:45";
        } else if (hour == 20 && minute >= 45) {
            return "战斗开始 21:00";
        } else {
            return "明日报名 20:45";
        }
    }
    
    /**
     * 报名参战
     */
    public WarParticipant register(String odUserId, String playerName, Integer level, Long power) {
        if (!testMode && todayWar.getStatus() != WarStatus.REGISTERING) {
            throw new BusinessException("当前不在报名时间，请在20:45-21:00之间报名");
        }
        
        Alliance alliance = allianceService.getUserAlliance(odUserId);
        if (alliance == null) {
            throw new BusinessException("请先加入联盟后再报名参战");
        }
        
        boolean alreadyRegistered = todayWar.getParticipants().stream()
                .anyMatch(p -> p.getOdUserId().equals(odUserId));
        if (alreadyRegistered) {
            throw new BusinessException("您已报名参战");
        }
        
        int playerNumber = todayWar.getParticipants().size() + 1;
        WarParticipant participant = WarParticipant.builder()
                .odUserId(odUserId)
                .playerName(playerName)
                .allianceId(alliance.getId())
                .allianceName(alliance.getName())
                .playerNumber(playerNumber)
                .level(level != null ? level : 1)
                .power(power != null ? power : 10000L)
                .status(PlayerStatus.WAITING)
                .wins(0)
                .losses(0)
                .flags(0)
                .eliminatedRound(0)
                .totalMerit(0)
                .totalScore(0)
                .registerTime(System.currentTimeMillis())
                .build();
        
        todayWar.getParticipants().add(participant);
        saveTodayWar();
        log.info("玩家 {} 报名参战，编号: {}", playerName, playerNumber);
        
        return participant;
    }
    
    /**
     * 取消报名
     */
    public void cancelRegister(String odUserId) {
        if (todayWar.getStatus() != WarStatus.REGISTERING && todayWar.getStatus() != WarStatus.NOT_STARTED) {
            throw new BusinessException("战斗已开始，无法取消报名");
        }
        
        todayWar.getParticipants().removeIf(p -> p.getOdUserId().equals(odUserId));
        
        int number = 1;
        for (WarParticipant p : todayWar.getParticipants()) {
            p.setPlayerNumber(number++);
        }
        
        saveTodayWar();
        log.info("玩家 {} 取消报名", odUserId);
    }
    
    @Scheduled(cron = "0 45 20 * * ?")
    public void startRegistration() {
        String today = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
        if (todayWar == null || !todayWar.getDate().equals(today)) {
            todayWar = AllianceWar.createNew(today);
        }
        
        todayWar.setStatus(WarStatus.REGISTERING);
        saveTodayWar();
        log.info("盟战报名开始: {}", today);
    }
    
    @Scheduled(cron = "0 0 21 * * ?")
    public void startWar() {
        if (todayWar.getStatus() != WarStatus.REGISTERING) {
            log.warn("盟战未在报名状态，无法开始");
            return;
        }
        
        if (todayWar.getParticipants().size() < 2) {
            log.warn("参战人数不足，盟战取消");
            todayWar.setStatus(WarStatus.FINISHED);
            saveTodayWar();
            return;
        }

        beginWar();
    }
    
    public void triggerWarStart() {
        if (!testMode) {
            throw new BusinessException("非测试模式，无法手动触发");
        }
        todayWar.setStatus(WarStatus.REGISTERING);
        saveTodayWar();
    }
    
    public void triggerBattleStart() {
        if (!testMode) {
            throw new BusinessException("非测试模式，无法手动触发");
        }
        
        if (todayWar.getParticipants().size() < 2) {
            throw new BusinessException("参战人数不足2人");
        }

        beginWar();
        // 测试入口立即执行当前轮，避免等待5分钟
        processCurrentRoundIfDue(true);
    }

    @Scheduled(cron = "0 * * * * ?")
    public void processRoundTick() {
        processCurrentRoundIfDue(false);
    }

    private synchronized void beginWar() {
        todayWar.setStatus(WarStatus.IN_PROGRESS);
        long now = System.currentTimeMillis();
        todayWar.setStartTime(now);
        todayWar.setCurrentRound(1);
        todayWar.setCurrentRoundStartTime(now);
        todayWar.setNextRoundTime(now + ROUND_DURATION_MINUTES * 60_000L);
        todayWar.setRoundDurationMinutes(ROUND_DURATION_MINUTES);

        for (WarParticipant p : todayWar.getParticipants()) {
            p.setStatus(PlayerStatus.WAITING);
            p.setEliminatedRound(0);
            if (p.getFlags() == null) p.setFlags(0);
            if (p.getWins() == null) p.setWins(0);
            if (p.getLosses() == null) p.setLosses(0);
            if (p.getTotalMerit() == null) p.setTotalMerit(0);
            if (p.getTotalScore() == null) p.setTotalScore(0);
            initializeParticipantSoldierState(p);
        }
        saveTodayWar();
        log.info("盟战开始，参战人数: {}", todayWar.getParticipants().size());
    }

    private synchronized void processCurrentRoundIfDue(boolean force) {
        if (todayWar == null || todayWar.getStatus() != WarStatus.IN_PROGRESS) return;
        long now = System.currentTimeMillis();
        if (!force && todayWar.getNextRoundTime() != null && now < todayWar.getNextRoundTime()) return;
        settleCurrentRound();
    }

    private void settleCurrentRound() {
        List<String> aliveAlliances = getAliveAllianceIds();
        if (aliveAlliances.size() <= 1) {
            endWar();
            return;
        }

        int round = todayWar.getCurrentRound() != null ? todayWar.getCurrentRound() : 1;
        Collections.shuffle(aliveAlliances, new Random());
        log.info("盟战第{}轮结算，活跃联盟: {}", round, aliveAlliances.size());

        for (int i = 0; i < aliveAlliances.size(); i += 2) {
            if (i + 1 >= aliveAlliances.size()) {
                // 奇数联盟轮空晋级
                continue;
            }
            String a1 = aliveAlliances.get(i);
            String a2 = aliveAlliances.get(i + 1);
            runAllianceDuel(a1, a2, round);
        }

        List<String> remain = getAliveAllianceIds();
        if (remain.size() <= 1) {
            endWar();
            return;
        }

        long now = System.currentTimeMillis();
        todayWar.setCurrentRound(round + 1);
        todayWar.setCurrentRoundStartTime(now);
        todayWar.setNextRoundTime(now + ROUND_DURATION_MINUTES * 60_000L);
        saveTodayWar();
    }

    private void runAllianceDuel(String allianceA, String allianceB, int round) {
        List<WarParticipant> sideA = getAliveAllianceParticipants(allianceA);
        List<WarParticipant> sideB = getAliveAllianceParticipants(allianceB);
        Collections.shuffle(sideA, new Random());
        Collections.shuffle(sideB, new Random());

        while (!sideA.isEmpty() && !sideB.isEmpty()) {
            WarParticipant p1 = sideA.get(0);
            WarParticipant p2 = sideB.get(0);
            WarBattle battle = createBattle(round, p1, p2);
            simulateBattle(battle, p1, p2, round);
            todayWar.getBattles().add(battle);
            if (!isParticipantAlive(p1)) sideA.remove(0);
            if (!isParticipantAlive(p2)) sideB.remove(0);
        }
    }

    private WarBattle createBattle(int round, WarParticipant p1, WarParticipant p2) {
        p1.setStatus(PlayerStatus.IN_BATTLE);
        p2.setStatus(PlayerStatus.IN_BATTLE);
        return WarBattle.builder()
                .id("battle_" + round + "_" + System.nanoTime())
                .round(round)
                .player1Id(p1.getOdUserId())
                .player1Name(p1.getPlayerName())
                .player1Alliance(p1.getAllianceName())
                .player2Id(p2.getOdUserId())
                .player2Name(p2.getPlayerName())
                .player2Alliance(p2.getAllianceName())
                .startTime(System.currentTimeMillis())
                .rounds(new ArrayList<>())
                .flagGained(0)
                .meritGained(0)
                .build();
    }

    private void simulateBattle(WarBattle battle, WarParticipant p1, WarParticipant p2, int round) {
        List<BattleCalculator.BattleUnit> sideA = buildWarParticipantUnits(p1);
        List<BattleCalculator.BattleUnit> sideB = buildWarParticipantUnits(p2);
        BattleService.BattleReport report = battleService.fight(sideA, sideB, 20);
        battle.setBattleReportJson(JSON.toJSONString(report));
        battle.setEndTime(System.currentTimeMillis());
        battle.setPlayer1Score(report.victoryA ? 1 : 0);
        battle.setPlayer2Score(report.victoryA ? 0 : 1);

        for (BattleService.RoundLog rl : report.rounds) {
            BattleRound br = new BattleRound();
            br.setRoundNum(rl.roundNum);
            br.setAttackerId(p1.getOdUserId());
            br.setDefenderId(p2.getOdUserId());
            int totalLoss = 0;
            if (rl.actions != null) {
                for (BattleService.ActionLog act : rl.actions) {
                    if (act.hits != null) {
                        totalLoss += act.hits.stream().mapToInt(h -> h.soldierLoss).sum();
                    }
                }
            }
            br.setDamage(totalLoss);
            br.setDescription("第" + rl.roundNum + "回合减员 " + totalLoss);
            battle.getRounds().add(br);
        }

        updateRemainingAfterBattle(p1, sideA);
        updateRemainingAfterBattle(p2, sideB);

        WarParticipant winner = report.victoryA ? p1 : p2;
        WarParticipant loser = report.victoryA ? p2 : p1;
        int loserLevel = loser.getLevel() != null ? loser.getLevel() : 1;
        int merit = Math.max(1, loserLevel * 2);
        int score = loserLevel * 10;

        winner.setWins((winner.getWins() != null ? winner.getWins() : 0) + 1);
        winner.setFlags((winner.getFlags() != null ? winner.getFlags() : 0) + 1);
        winner.setTotalMerit((winner.getTotalMerit() != null ? winner.getTotalMerit() : 0) + merit);
        winner.setTotalScore((winner.getTotalScore() != null ? winner.getTotalScore() : 0) + score);
        battle.setFlagGained(1);
        battle.setMeritGained(merit);

        loser.setLosses((loser.getLosses() != null ? loser.getLosses() : 0) + 1);
        loser.setStatus(PlayerStatus.SPECTATING);
        if (loser.getEliminatedRound() == null || loser.getEliminatedRound() == 0) {
            loser.setEliminatedRound(round);
        }

        winner.setStatus(isParticipantAlive(winner) ? PlayerStatus.WAITING : PlayerStatus.SPECTATING);
        if (!isParticipantAlive(winner) && (winner.getEliminatedRound() == null || winner.getEliminatedRound() == 0)) {
            winner.setEliminatedRound(round);
        }

        battle.setWinnerId(winner.getOdUserId());
        battle.setWinnerName(winner.getPlayerName());
    }

    private List<String> getAliveAllianceIds() {
        return todayWar.getParticipants().stream()
                .filter(this::isParticipantAlive)
                .map(WarParticipant::getAllianceId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
    }

    private List<WarParticipant> getAliveAllianceParticipants(String allianceId) {
        return todayWar.getParticipants().stream()
                .filter(p -> allianceId.equals(p.getAllianceId()) && isParticipantAlive(p))
                .collect(Collectors.toList());
    }

    private boolean isParticipantAlive(WarParticipant p) {
        if (p == null) return false;
        if (p.getEliminatedRound() != null && p.getEliminatedRound() > 0 && p.getStatus() == PlayerStatus.SPECTATING) {
            return false;
        }
        Integer remain = p.getTotalRemainingSoldiers();
        if (remain == null) return true;
        return remain > 0;
    }

    private void initializeParticipantSoldierState(WarParticipant p) {
        if (p == null) return;
        if (p.getRemainingSoldiers() != null && !p.getRemainingSoldiers().isEmpty()) {
            int total = p.getRemainingSoldiers().values().stream().mapToInt(v -> Math.max(v, 0)).sum();
            p.setTotalRemainingSoldiers(total);
            if (p.getTotalInitialSoldiers() == null || p.getTotalInitialSoldiers() <= 0) p.setTotalInitialSoldiers(total);
            return;
        }
        Map<String, Integer> remain = new LinkedHashMap<>();
        if (p.getOdUserId() != null && !p.getOdUserId().startsWith("NPC_")) {
            try {
                List<General> generals = formationService.getBattleOrder(p.getOdUserId());
                for (General g : generals) {
                    String key = g.getId() != null ? String.valueOf(g.getId()) : (g.getName() + "_" + remain.size());
                    int maxSc = g.getSoldierMaxCount() != null ? g.getSoldierMaxCount() : 100;
                    int curSc = g.getSoldierCount() != null ? Math.min(g.getSoldierCount(), maxSc) : maxSc;
                    remain.put(key, curSc);
                }
            } catch (Exception e) {
                log.warn("初始化盟战兵力失败: {}", p.getOdUserId(), e);
            }
        }
        if (remain.isEmpty()) {
            remain.put("core", 100);
        }
        int total = remain.values().stream().mapToInt(Integer::intValue).sum();
        p.setRemainingSoldiers(remain);
        p.setTotalInitialSoldiers(total);
        p.setTotalRemainingSoldiers(total);
    }

    private void updateRemainingAfterBattle(WarParticipant p, List<BattleCalculator.BattleUnit> units) {
        if (p == null || units == null) return;
        initializeParticipantSoldierState(p);
        List<String> keys = new ArrayList<>(p.getRemainingSoldiers().keySet());
        for (int i = 0; i < units.size(); i++) {
            if (i >= keys.size()) break;
            p.getRemainingSoldiers().put(keys.get(i), Math.max(0, units.get(i).soldierCount));
        }
        int total = p.getRemainingSoldiers().values().stream().mapToInt(v -> Math.max(v, 0)).sum();
        p.setTotalRemainingSoldiers(total);
    }
    
    private void endWar() {
        todayWar.setStatus(WarStatus.FINISHED);
        todayWar.setEndTime(System.currentTimeMillis());
        
        calculateAllianceRanks();
        calculatePlayerRanks();
        distributeRewards();
        
        saveTodayWar();
        log.info("盟战结束");
    }
    
    private void calculateAllianceRanks() {
        Map<String, AllianceRank> allianceMap = new HashMap<>();

        int finalRound = todayWar.getCurrentRound() != null ? todayWar.getCurrentRound() : 1;
        for (WarParticipant p : todayWar.getParticipants()) {
            AllianceRank rank = allianceMap.computeIfAbsent(p.getAllianceId(), id ->
                AllianceRank.builder()
                        .allianceId(id)
                        .allianceName(p.getAllianceName())
                        .faction(p.getFaction())
                        .totalFlags(0)
                        .participantCount(0)
                        .wins(0)
                        .losses(0)
                        .eliminatedRound(0)
                        .rewards(new ArrayList<>())
                        .build()
            );

            rank.setTotalFlags(rank.getTotalFlags() + (p.getFlags() != null ? p.getFlags() : 0));
            rank.setParticipantCount(rank.getParticipantCount() + 1);
            rank.setWins(rank.getWins() + (p.getWins() != null ? p.getWins() : 0));
            rank.setLosses(rank.getLosses() + (p.getLosses() != null ? p.getLosses() : 0));
            int eliminatedRound = (p.getEliminatedRound() != null && p.getEliminatedRound() > 0)
                    ? p.getEliminatedRound() : (finalRound + 1);
            rank.setEliminatedRound(Math.max(rank.getEliminatedRound(), eliminatedRound));
        }

        List<AllianceRank> sortedRanks = allianceMap.values().stream()
                .sorted((a, b) -> {
                    if (!Objects.equals(a.getEliminatedRound(), b.getEliminatedRound())) {
                        return Integer.compare(b.getEliminatedRound(), a.getEliminatedRound());
                    }
                    return Integer.compare(b.getTotalFlags(), a.getTotalFlags());
                })
                .collect(Collectors.toList());

        int currentRank = 1;
        for (int i = 0; i < sortedRanks.size(); i++) {
            AllianceRank rank = sortedRanks.get(i);
            if (i > 0) {
                AllianceRank prev = sortedRanks.get(i - 1);
                boolean tie = Objects.equals(prev.getEliminatedRound(), rank.getEliminatedRound())
                        && Objects.equals(prev.getTotalFlags(), rank.getTotalFlags());
                if (!tie) currentRank = i + 1;
            }
            rank.setRank(currentRank);
            rank.setRewards(buildAllianceRewardTexts(currentRank, rank.getTotalFlags()));
        }

        todayWar.setAllianceRanks(sortedRanks);
    }
    
    private void calculatePlayerRanks() {
        List<PlayerRank> playerRanks = todayWar.getParticipants().stream()
                .sorted((a, b) -> {
                    int af = a.getFlags() != null ? a.getFlags() : 0;
                    int bf = b.getFlags() != null ? b.getFlags() : 0;
                    if (af != bf) return Integer.compare(bf, af);
                    int aw = a.getWins() != null ? a.getWins() : 0;
                    int bw = b.getWins() != null ? b.getWins() : 0;
                    if (aw != bw) return Integer.compare(bw, aw);
                    int am = a.getTotalMerit() != null ? a.getTotalMerit() : 0;
                    int bm = b.getTotalMerit() != null ? b.getTotalMerit() : 0;
                    if (am != bm) {
                        return Integer.compare(bm, am);
                    }
                    return String.valueOf(a.getOdUserId()).compareTo(String.valueOf(b.getOdUserId()));
                })
                .map(p -> {
                    List<String> rewards = new ArrayList<>();
                    rewards.add("盟战礼盒x1");
                    return PlayerRank.builder()
                            .odUserId(p.getOdUserId())
                            .playerName(p.getPlayerName())
                            .allianceName(p.getAllianceName())
                            .wins(p.getWins() != null ? p.getWins() : 0)
                            .flags(p.getFlags() != null ? p.getFlags() : 0)
                            .merit(p.getTotalMerit() != null ? p.getTotalMerit() : 0)
                            .extraBoundGold(0)
                            .rewards(rewards)
                            .build();
                })
                .collect(Collectors.toList());

        int[] extraBoundGold = {100, 70, 50, 20, 10};
        for (int i = 0; i < playerRanks.size(); i++) {
            PlayerRank rank = playerRanks.get(i);
            rank.setRank(i + 1);
            if (i < 5) {
                rank.getRewards().add("盟战宝箱x1");
                rank.getRewards().add("绑金x" + extraBoundGold[i]);
                rank.setExtraBoundGold(extraBoundGold[i]);
            }
        }

        todayWar.setPlayerRanks(playerRanks);
    }
    
    private void distributeRewards() {
        log.info("发放盟战奖励...");
        ensureRewardPools();
        todayWar.getAllianceRewardPools().clear();

        for (WarParticipant p : todayWar.getParticipants()) {
            if (p.getOdUserId() == null || p.getOdUserId().startsWith("NPC_")) continue;
            try {
                mailService.sendSystemMail(p.getOdUserId(), "盟战参战奖励",
                        "感谢参与盟战，获得盟战礼盒x1", buildRewardAttachments("盟战礼盒", 11062, 1));
                allianceService.addWarScore(p.getOdUserId(), 10 + (p.getWins() != null ? p.getWins() : 0) * 5);
            } catch (Exception e) {
                log.warn("发放参战奖励失败: {}", p.getOdUserId(), e);
            }
        }

        if (todayWar.getPlayerRanks() != null) {
            for (int i = 0; i < Math.min(5, todayWar.getPlayerRanks().size()); i++) {
                PlayerRank pr = todayWar.getPlayerRanks().get(i);
                if (pr.getOdUserId() == null || pr.getOdUserId().startsWith("NPC_")) continue;
                int[] extraBoundGold = {100, 70, 50, 20, 10};
                try {
                    List<Map<String, Object>> atts = new ArrayList<>();
                    atts.addAll(buildRewardAttachments("盟战宝箱", 11061, 1));
                    atts.addAll(buildRewardAttachments("绑金", 1, extraBoundGold[i]));
                    mailService.sendSystemMail(pr.getOdUserId(), "盟战个人排名奖励",
                            "恭喜获得盟战个人排名第" + (i + 1) + "名!", atts);
                } catch (Exception e) {
                    log.warn("发放个人排名奖励失败: {}", pr.getOdUserId(), e);
                }
            }
        }

        if (todayWar.getAllianceRanks() != null) {
            for (AllianceRank ar : todayWar.getAllianceRanks()) {
                int rank = ar.getRank();
                List<Map<String, Object>> allianceAtts = buildAllianceRewardAttachments(rank, ar.getTotalFlags() != null ? ar.getTotalFlags() : 0);
                AllianceRewardPool pool = AllianceRewardPool.builder()
                        .allianceId(ar.getAllianceId())
                        .allianceName(ar.getAllianceName())
                        .rank(rank)
                        .totalFlags(ar.getTotalFlags() != null ? ar.getTotalFlags() : 0)
                        .rewards(allianceAtts)
                        .distributed(false)
                        .createTime(System.currentTimeMillis())
                        .build();
                todayWar.getAllianceRewardPools().add(pool);
            }
        }
    }

    private List<String> buildAllianceRewardTexts(int rank, int flags) {
        List<String> rewards = new ArrayList<>();
        int extraGift = Math.max(0, flags);
        int extraIngot = Math.max(0, flags / 5);
        int extraStrip = Math.max(0, flags / 2);
        if (rank == 1) {
            rewards.add("盟战宝箱x10");
            rewards.add("金砖x5");
            rewards.add("金条x10");
            rewards.add("资源礼包x" + (30 + extraGift));
            rewards.add("银锭x" + (5 + extraIngot));
            rewards.add("银条x" + (20 + extraStrip));
        } else if (rank == 2) {
            rewards.add("盟战宝箱x8");
            rewards.add("金砖x3");
            rewards.add("金条x6");
            rewards.add("资源礼包x" + (18 + extraGift));
            rewards.add("银锭x" + (3 + extraIngot));
            rewards.add("银条x" + (12 + extraStrip));
        } else if (rank == 3) {
            rewards.add("盟战宝箱x5");
            rewards.add("金砖x2");
            rewards.add("金条x4");
            rewards.add("资源礼包x" + (12 + extraGift));
            rewards.add("银锭x" + (2 + extraIngot));
            rewards.add("银条x" + (8 + extraStrip));
        } else if (rank == 4) {
            rewards.add("盟战宝箱x2");
            rewards.add("金砖x1");
            rewards.add("金条x2");
            rewards.add("资源礼包x" + (6 + extraGift));
            rewards.add("银锭x" + (1 + extraIngot));
            rewards.add("银条x" + (4 + extraStrip));
        } else if (rank == 5) {
            rewards.add("盟战宝箱x1");
            rewards.add("金条x1");
            rewards.add("资源礼包x" + (3 + extraGift));
            rewards.add("银锭x" + extraIngot);
            rewards.add("银条x" + (2 + extraStrip));
        } else {
            rewards.add("资源礼包x" + (1 + extraGift));
            rewards.add("银锭x" + extraIngot);
            rewards.add("银条x" + (1 + extraStrip));
        }
        return rewards;
    }

    private List<Map<String, Object>> buildAllianceRewardAttachments(int rank, int flags) {
        // itemId 为当前项目占位ID，前端展示以 itemName 为主
        List<Map<String, Object>> atts = new ArrayList<>();
        int extraGift = Math.max(0, flags);
        int extraIngot = Math.max(0, flags / 5);
        int extraStrip = Math.max(0, flags / 2);
        if (rank == 1) {
            atts.addAll(buildRewardAttachments("盟战宝箱", 11061, 10));
            atts.addAll(buildRewardAttachments("金砖", 12001, 5));
            atts.addAll(buildRewardAttachments("金条", 12002, 10));
            atts.addAll(buildRewardAttachments("资源礼包", 11051, 30 + extraGift));
            atts.addAll(buildRewardAttachments("银锭", 12003, 5 + extraIngot));
            atts.addAll(buildRewardAttachments("银条", 12004, 20 + extraStrip));
        } else if (rank == 2) {
            atts.addAll(buildRewardAttachments("盟战宝箱", 11061, 8));
            atts.addAll(buildRewardAttachments("金砖", 12001, 3));
            atts.addAll(buildRewardAttachments("金条", 12002, 6));
            atts.addAll(buildRewardAttachments("资源礼包", 11051, 18 + extraGift));
            atts.addAll(buildRewardAttachments("银锭", 12003, 3 + extraIngot));
            atts.addAll(buildRewardAttachments("银条", 12004, 12 + extraStrip));
        } else if (rank == 3) {
            atts.addAll(buildRewardAttachments("盟战宝箱", 11061, 5));
            atts.addAll(buildRewardAttachments("金砖", 12001, 2));
            atts.addAll(buildRewardAttachments("金条", 12002, 4));
            atts.addAll(buildRewardAttachments("资源礼包", 11051, 12 + extraGift));
            atts.addAll(buildRewardAttachments("银锭", 12003, 2 + extraIngot));
            atts.addAll(buildRewardAttachments("银条", 12004, 8 + extraStrip));
        } else if (rank == 4) {
            atts.addAll(buildRewardAttachments("盟战宝箱", 11061, 2));
            atts.addAll(buildRewardAttachments("金砖", 12001, 1));
            atts.addAll(buildRewardAttachments("金条", 12002, 2));
            atts.addAll(buildRewardAttachments("资源礼包", 11051, 6 + extraGift));
            atts.addAll(buildRewardAttachments("银锭", 12003, 1 + extraIngot));
            atts.addAll(buildRewardAttachments("银条", 12004, 4 + extraStrip));
        } else if (rank == 5) {
            atts.addAll(buildRewardAttachments("盟战宝箱", 11061, 1));
            atts.addAll(buildRewardAttachments("金条", 12002, 1));
            atts.addAll(buildRewardAttachments("资源礼包", 11051, 3 + extraGift));
            if (extraIngot > 0) atts.addAll(buildRewardAttachments("银锭", 12003, extraIngot));
            atts.addAll(buildRewardAttachments("银条", 12004, 2 + extraStrip));
        } else {
            atts.addAll(buildRewardAttachments("资源礼包", 11051, 1 + extraGift));
            if (extraIngot > 0) atts.addAll(buildRewardAttachments("银锭", 12003, extraIngot));
            atts.addAll(buildRewardAttachments("银条", 12004, 1 + extraStrip));
        }
        return atts;
    }

    private List<Map<String, Object>> buildRewardAttachments(String itemName, int itemId, int count) {
        List<Map<String, Object>> list = new ArrayList<>();
        if (count <= 0) return list;
        Map<String, Object> att = new LinkedHashMap<>();
        att.put("itemType", "item");
        att.put("itemId", itemId);
        att.put("itemName", itemName);
        att.put("itemQuality", "");
        // MailMapper uses #{count}; keep itemCount for frontend display compatibility.
        att.put("count", count);
        att.put("itemCount", count);
        att.put("claimed", 0);
        list.add(att);
        return list;
    }

    private Map<String, Integer> toItemCountMap(List<Map<String, Object>> rewards) {
        Map<String, Integer> map = new LinkedHashMap<>();
        if (rewards == null) return map;
        for (Map<String, Object> item : rewards) {
            if (item == null) continue;
            String name = item.get("itemName") != null ? String.valueOf(item.get("itemName")) : null;
            int count = item.get("itemCount") != null
                    ? ((Number) item.get("itemCount")).intValue()
                    : (item.get("count") != null ? ((Number) item.get("count")).intValue() : 0);
            if (name == null || count <= 0) continue;
            map.put(name, map.getOrDefault(name, 0) + count);
        }
        return map;
    }

    private Map<String, String> toItemNameToIdMap(List<Map<String, Object>> rewards) {
        Map<String, String> map = new LinkedHashMap<>();
        if (rewards == null) return map;
        for (Map<String, Object> item : rewards) {
            if (item == null) continue;
            String name = item.get("itemName") != null ? String.valueOf(item.get("itemName")) : null;
            String id = item.get("itemId") != null ? String.valueOf(item.get("itemId")) : "0";
            if (name != null && !map.containsKey(name)) map.put(name, id);
        }
        return map;
    }
    
    public List<WarBattle> getBattleHistory(String odUserId) {
        return todayWar.getBattles().stream()
                .filter(b -> Objects.equals(b.getPlayer1Id(), odUserId) || Objects.equals(b.getPlayer2Id(), odUserId))
                .sorted((a, b) -> Long.compare(
                        b.getEndTime() != null ? b.getEndTime() : 0L,
                        a.getEndTime() != null ? a.getEndTime() : 0L))
                .collect(Collectors.toList());
    }
    
    public List<WarBattle> getAllBattles() {
        return todayWar.getBattles().stream()
                .sorted((a, b) -> Long.compare(
                        b.getEndTime() != null ? b.getEndTime() : 0L,
                        a.getEndTime() != null ? a.getEndTime() : 0L))
                .collect(Collectors.toList());
    }
    
    public List<WarParticipant> getParticipants() {
        return todayWar.getParticipants();
    }
    
    public void resetWar() {
        String today = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
        todayWar = AllianceWar.createNew(today);
        saveTodayWar();
        log.info("重置盟战: {}", today);
    }

    /**
     * 测试: 注入NPC模拟玩家，来自不同虚拟联盟
     * @param count 注入NPC数量
     * @param allianceCount 联盟数量(NPC均匀分配到各联盟)
     */
    public List<WarParticipant> injectNpcs(int count, int allianceCount) {
        if (!testMode) throw new BusinessException("非测试模式");
        if (todayWar.getStatus() != WarStatus.REGISTERING && todayWar.getStatus() != WarStatus.NOT_STARTED) {
            todayWar.setStatus(WarStatus.REGISTERING);
        }

        String[] allianceNames = {"青龙盟", "白虎盟", "朱雀盟", "玄武盟", "麒麟盟", "凤凰盟", "天狼盟", "苍龙盟"};
        String[] surnames = {"张", "王", "李", "赵", "刘", "陈", "杨", "黄", "周", "吴", "孙", "马"};
        String[] givenNames = {"飞", "云", "龙", "虎", "豹", "鹰", "风", "雷", "雨", "星", "月", "日"};
        Random rng = new Random();

        List<WarParticipant> injected = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            int allianceIdx = i % Math.min(allianceCount, allianceNames.length);
            String allianceId = "NPC_ALLIANCE_" + (allianceIdx + 1);
            String allianceName = allianceNames[allianceIdx];
            String npcName = surnames[rng.nextInt(surnames.length)] + givenNames[rng.nextInt(givenNames.length)]
                    + (rng.nextInt(90) + 10);
            String npcId = "NPC_" + System.currentTimeMillis() + "_" + i;

            int level = 20 + rng.nextInt(30);
            long power = 5000L + rng.nextInt(15000);
            int playerNumber = todayWar.getParticipants().size() + 1;

            WarParticipant npc = WarParticipant.builder()
                    .odUserId(npcId)
                    .playerName(npcName)
                    .allianceId(allianceId)
                    .allianceName(allianceName)
                    .playerNumber(playerNumber)
                    .level(level)
                    .power(power)
                    .status(PlayerStatus.WAITING)
                    .wins(0).losses(0).flags(0)
                    .eliminatedRound(0)
                    .totalMerit(0)
                    .totalScore(0)
                    .registerTime(System.currentTimeMillis())
                    .build();

            todayWar.getParticipants().add(npc);
            injected.add(npc);
        }

        saveTodayWar();
        log.info("注入{}个NPC, 分属{}个联盟", count, allianceCount);
        return injected;
    }

    /**
     * 测试: 一键完整流程（重置 → 开报名 → 注入NPC → 自己报名 → 开始战斗）
     */
    public Map<String, Object> quickTest(String odUserId, String playerName, int npcCount, int allianceCount) {
        if (!testMode) throw new BusinessException("非测试模式");

        resetWar();
        todayWar.setStatus(WarStatus.REGISTERING);
        saveTodayWar();

        try {
            register(odUserId, playerName, 50, 20000L);
        } catch (BusinessException e) {
            Alliance alliance = allianceService.getUserAlliance(odUserId);
            String allianceId = alliance != null ? alliance.getId() : "TEST_ALLIANCE_0";
            String allianceName = alliance != null ? alliance.getName() : "测试联盟";
            int playerNumber = todayWar.getParticipants().size() + 1;
            WarParticipant me = WarParticipant.builder()
                    .odUserId(odUserId).playerName(playerName)
                    .allianceId(allianceId).allianceName(allianceName)
                    .playerNumber(playerNumber).level(50).power(20000L)
                    .status(PlayerStatus.WAITING).wins(0).losses(0).flags(0)
                    .eliminatedRound(0).totalMerit(0).totalScore(0)
                    .registerTime(System.currentTimeMillis()).build();
            todayWar.getParticipants().add(me);
            saveTodayWar();
        }
        List<WarParticipant> npcs = injectNpcs(npcCount, allianceCount);
        beginWar();
        // 测试模式下直接跑完整个流程
        int guard = 0;
        while (todayWar.getStatus() == WarStatus.IN_PROGRESS && guard++ < 100) {
            processCurrentRoundIfDue(true);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("message", "一键测试完成");
        result.put("participantCount", todayWar.getParticipants().size());
        result.put("status", todayWar.getStatus().name());
        result.put("rounds", todayWar.getCurrentRound());
        result.put("battles", todayWar.getBattles().size());
        result.put("allianceRanks", todayWar.getAllianceRanks());
        result.put("playerRanks", todayWar.getPlayerRanks());
        return result;
    }

    private List<BattleCalculator.BattleUnit> buildWarParticipantUnits(WarParticipant p) {
        initializeParticipantSoldierState(p);
        String odUserId = p.getOdUserId();
        if (odUserId != null && !odUserId.startsWith("NPC_")) {
            try {
                List<General> generals = formationService.getBattleOrder(odUserId);
                if (!generals.isEmpty()) {
                    List<BattleCalculator.BattleUnit> units = new ArrayList<>();
                    for (int i = 0; i < generals.size(); i++) {
                        General g = generals.get(i);
                        String key = g.getId() != null ? String.valueOf(g.getId()) : g.getName();
                        int remaining = p.getRemainingSoldiers().getOrDefault(key, 0);
                        Map<String, Integer> eq = suitConfigService.calculateTotalEquipBonus(g.getId());
                        int rawTier = g.getSoldierTier() != null ? g.getSoldierTier() : 1;
                        int sRank = g.getSoldierRank() != null ? g.getSoldierRank() : 1;
                        int tier = Math.max(rawTier, sRank);
                        int troopType = BattleCalculator.parseTroopType(g.getTroopType());
                        int maxSc = g.getSoldierMaxCount() != null ? g.getSoldierMaxCount() : 100;
                        int formLv = BattleCalculator.maxPeopleToFormationLevel(maxSc);
                        int sc = Math.max(0, Math.min(remaining, maxSc));
                        BattleCalculator.BattleUnit u = BattleCalculator.assembleBattleUnit(
                                g.getName() != null ? g.getName() : p.getPlayerName(),
                                g.getLevel() != null ? g.getLevel() : 1,
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
                        generalService.applyFamousTraitsToUnit(u, g.getName(), troopType);

                        if (g.getSlotId() != null && g.getSlotId() > 0) {
                            u.tacticsTriggerBonus = generalService.getTacticsTriggerBonus(g.getSlotId());
                        }
                        if (g.getTacticsId() != null) {
                            TacticsConfig.TacticsTemplate tt = tacticsConfig.getById(g.getTacticsId());
                            if (tt != null) {
                                Map<String, Object> owned = userTacticsMapper.findByUserIdAndTacticsId(
                                        g.getUserId(), g.getTacticsId());
                                int tLevel = owned != null ? ((Number) owned.get("level")).intValue() : 1;
                                u.tacticsId = tt.getId();
                                u.tacticsName = tt.getName();
                                u.tacticsLevel = tLevel;
                                u.tacticsEffectValue = TacticsConfig.calcEffect(tt, tLevel);
                                u.tacticsTriggerRate = TacticsConfig.calcTriggerRate(tt, tLevel);
                            }
                        }
                        units.add(u);
                    }
                    return units;
                }
            } catch (Exception e) {
                log.warn("构建盟战参战者阵型失败: {}", odUserId, e);
            }
        }
        int power = p.getPower() != null ? p.getPower().intValue() : 5000;
        int level = 30;
        int tier = Math.max(1, Math.min(10, 1 + level / 20));
        int formLv = BattleCalculator.levelToFormationLevel(level);
        int maxSc = BattleCalculator.getFormationMaxPeople(formLv);
        int remaining = p.getTotalRemainingSoldiers() != null && p.getTotalRemainingSoldiers() > 0
                ? p.getTotalRemainingSoldiers() : maxSc;
        BattleCalculator.BattleUnit u = BattleCalculator.assembleBattleUnit(
                p.getPlayerName(), level,
                power / 2, power / 3, level * 2, level * 2,
                5, 15, 1, tier, remaining, maxSc, formLv,
                0, 0, 0, 0, 0, 0, 0, 0);
        u.position = 0;
        return Collections.singletonList(u);
    }
}
