package com.tencent.wxcloudrun.service.alliance;

import com.tencent.wxcloudrun.exception.BusinessException;
import com.tencent.wxcloudrun.model.Alliance;
import com.tencent.wxcloudrun.model.AllianceWar;
import com.tencent.wxcloudrun.model.AllianceWar.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 联盟战服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AllianceWarService {
    
    private final AllianceService allianceService;
    
    // 盟战存储 date -> AllianceWar
    private final Map<String, AllianceWar> warStore = new ConcurrentHashMap<>();
    
    // 今日盟战
    private AllianceWar todayWar;
    
    // 测试模式：设为true时可以随时触发盟战
    private boolean testMode = true;
    
    @PostConstruct
    public void init() {
        // 初始化今日盟战
        String today = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
        todayWar = AllianceWar.createNew(today);
        warStore.put(today, todayWar);
        log.info("初始化今日盟战: {}", today);
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
        
        result.put("war", todayWar);
        result.put("status", todayWar.getStatus().name());
        result.put("currentRound", todayWar.getCurrentRound());
        result.put("participantCount", todayWar.getParticipants().size());
        
        // 检查用户是否已报名
        boolean registered = todayWar.getParticipants().stream()
                .anyMatch(p -> p.getOdUserId().equals(odUserId));
        result.put("registered", registered);
        
        // 获取用户的参战信息
        if (registered) {
            WarParticipant participant = todayWar.getParticipants().stream()
                    .filter(p -> p.getOdUserId().equals(odUserId))
                    .findFirst()
                    .orElse(null);
            result.put("myParticipant", participant);
        }
        
        // 计算距离下一阶段的时间
        result.put("nextPhaseInfo", getNextPhaseInfo());
        
        return result;
    }
    
    /**
     * 获取下一阶段信息
     */
    private Map<String, Object> getNextPhaseInfo() {
        Map<String, Object> info = new HashMap<>();
        Calendar cal = Calendar.getInstance();
        int hour = cal.get(Calendar.HOUR_OF_DAY);
        int minute = cal.get(Calendar.MINUTE);
        
        if (hour < 20 || (hour == 20 && minute < 45)) {
            info.put("nextPhase", "报名开始");
            info.put("time", "20:45");
        } else if (hour == 20 && minute >= 45) {
            info.put("nextPhase", "战斗开始");
            info.put("time", "21:00");
        } else {
            info.put("nextPhase", "明日报名");
            info.put("time", "20:45");
        }
        
        return info;
    }
    
    /**
     * 报名参战
     */
    public WarParticipant register(String odUserId, String playerName, Integer level, Long power) {
        // 检查盟战状态
        if (!testMode && todayWar.getStatus() != WarStatus.REGISTERING) {
            throw new BusinessException("当前不在报名时间，请在20:45-21:00之间报名");
        }
        
        // 检查是否已加入联盟
        Alliance alliance = allianceService.getUserAlliance(odUserId);
        if (alliance == null) {
            throw new BusinessException("请先加入联盟后再报名参战");
        }
        
        // 检查是否已报名
        boolean alreadyRegistered = todayWar.getParticipants().stream()
                .anyMatch(p -> p.getOdUserId().equals(odUserId));
        if (alreadyRegistered) {
            throw new BusinessException("您已报名参战");
        }
        
        // 创建参战记录
        int playerNumber = todayWar.getParticipants().size() + 1;
        WarParticipant participant = WarParticipant.builder()
                .odUserId(odUserId)
                .playerName(playerName)
                .allianceId(alliance.getId())
                .allianceName(alliance.getName())
                .faction(alliance.getFaction())
                .playerNumber(playerNumber)
                .level(level != null ? level : 1)
                .power(power != null ? power : 10000L)
                .status(PlayerStatus.WAITING)
                .wins(0)
                .losses(0)
                .flags(0)
                .registerTime(System.currentTimeMillis())
                .build();
        
        todayWar.getParticipants().add(participant);
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
        
        // 重新编号
        int number = 1;
        for (WarParticipant p : todayWar.getParticipants()) {
            p.setPlayerNumber(number++);
        }
        
        log.info("玩家 {} 取消报名", odUserId);
    }
    
    /**
     * 开始报名（每天20:45触发）
     */
    @Scheduled(cron = "0 45 20 * * ?")
    public void startRegistration() {
        String today = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
        if (todayWar == null || !todayWar.getDate().equals(today)) {
            todayWar = AllianceWar.createNew(today);
            warStore.put(today, todayWar);
        }
        
        todayWar.setStatus(WarStatus.REGISTERING);
        log.info("盟战报名开始: {}", today);
    }
    
    /**
     * 开始战斗（每天21:00触发）
     */
    @Scheduled(cron = "0 0 21 * * ?")
    public void startWar() {
        if (todayWar.getStatus() != WarStatus.REGISTERING) {
            log.warn("盟战未在报名状态，无法开始");
            return;
        }
        
        if (todayWar.getParticipants().size() < 2) {
            log.warn("参战人数不足，盟战取消");
            todayWar.setStatus(WarStatus.FINISHED);
            return;
        }
        
        todayWar.setStatus(WarStatus.IN_PROGRESS);
        todayWar.setStartTime(System.currentTimeMillis());
        todayWar.setCurrentRound(1);
        
        log.info("盟战开始，参战人数: {}", todayWar.getParticipants().size());
        
        // 开始第一轮配对
        startNextRound();
    }
    
    /**
     * 手动触发盟战（测试用）
     */
    public void triggerWarStart() {
        if (!testMode) {
            throw new BusinessException("非测试模式，无法手动触发");
        }
        
        // 设置为报名状态
        todayWar.setStatus(WarStatus.REGISTERING);
    }
    
    /**
     * 手动开始战斗（测试用）
     */
    public void triggerBattleStart() {
        if (!testMode) {
            throw new BusinessException("非测试模式，无法手动触发");
        }
        
        if (todayWar.getParticipants().size() < 2) {
            throw new BusinessException("参战人数不足2人");
        }
        
        todayWar.setStatus(WarStatus.IN_PROGRESS);
        todayWar.setStartTime(System.currentTimeMillis());
        todayWar.setCurrentRound(1);
        
        log.info("手动触发盟战开始，参战人数: {}", todayWar.getParticipants().size());
        
        startNextRound();
    }
    
    /**
     * 开始下一轮配对
     */
    private void startNextRound() {
        // 获取所有等待中的玩家
        List<WarParticipant> waitingPlayers = todayWar.getParticipants().stream()
                .filter(p -> p.getStatus() == PlayerStatus.WAITING)
                .collect(Collectors.toList());
        
        // 检查是否还需要继续
        if (waitingPlayers.size() <= 1) {
            // 检查剩余玩家是否都是同一联盟
            Set<String> remainingAlliances = waitingPlayers.stream()
                    .map(WarParticipant::getAllianceId)
                    .collect(Collectors.toSet());
            
            if (remainingAlliances.size() <= 1) {
                // 盟战结束
                endWar();
                return;
            }
        }
        
        // 随机打乱顺序
        Collections.shuffle(waitingPlayers);
        
        // 配对（同联盟不配对）
        List<WarParticipant> paired = new ArrayList<>();
        for (int i = 0; i < waitingPlayers.size(); i++) {
            WarParticipant p1 = waitingPlayers.get(i);
            if (paired.contains(p1)) continue;
            
            // 寻找不同联盟的对手
            for (int j = i + 1; j < waitingPlayers.size(); j++) {
                WarParticipant p2 = waitingPlayers.get(j);
                if (paired.contains(p2)) continue;
                
                // 同联盟不配对
                if (!p1.getAllianceId().equals(p2.getAllianceId())) {
                    // 创建对战
                    createBattle(p1, p2);
                    paired.add(p1);
                    paired.add(p2);
                    break;
                }
            }
        }
        
        // 如果有玩家无法配对（所有对手都是同联盟），直接进入下一轮
        for (WarParticipant p : waitingPlayers) {
            if (!paired.contains(p)) {
                log.info("玩家 {} 本轮轮空", p.getPlayerName());
            }
        }
        
        log.info("第{}轮配对完成，共{}场对战", todayWar.getCurrentRound(), paired.size() / 2);
    }
    
    /**
     * 创建对战
     */
    private void createBattle(WarParticipant p1, WarParticipant p2) {
        p1.setStatus(PlayerStatus.IN_BATTLE);
        p2.setStatus(PlayerStatus.IN_BATTLE);
        
        WarBattle battle = WarBattle.builder()
                .id("battle_" + System.currentTimeMillis() + "_" + p1.getPlayerNumber())
                .round(todayWar.getCurrentRound())
                .player1Id(p1.getOdUserId())
                .player1Name(p1.getPlayerName())
                .player1Alliance(p1.getAllianceName())
                .player2Id(p2.getOdUserId())
                .player2Name(p2.getPlayerName())
                .player2Alliance(p2.getAllianceName())
                .startTime(System.currentTimeMillis())
                .rounds(new ArrayList<>())
                .build();
        
        todayWar.getBattles().add(battle);
        
        // 自动模拟战斗（简化版）
        simulateBattle(battle, p1, p2);
    }
    
    /**
     * 模拟战斗
     */
    private void simulateBattle(WarBattle battle, WarParticipant p1, WarParticipant p2) {
        Random random = new Random();
        int p1Score = 0;
        int p2Score = 0;
        
        // 进行5回合战斗
        for (int i = 1; i <= 5; i++) {
            BattleRound round = new BattleRound();
            round.setRoundNum(i);
            
            // 计算战斗力影响（战力高有一定优势）
            double p1Chance = 0.5 + (p1.getPower() - p2.getPower()) / 100000.0 * 0.1;
            p1Chance = Math.max(0.3, Math.min(0.7, p1Chance)); // 限制在30%-70%
            
            if (random.nextDouble() < p1Chance) {
                int damage = 100 + random.nextInt(50);
                round.setAttackerId(p1.getOdUserId());
                round.setDefenderId(p2.getOdUserId());
                round.setDamage(damage);
                round.setDescription(p1.getPlayerName() + " 对 " + p2.getPlayerName() + " 造成 " + damage + " 点伤害");
                p1Score++;
            } else {
                int damage = 100 + random.nextInt(50);
                round.setAttackerId(p2.getOdUserId());
                round.setDefenderId(p1.getOdUserId());
                round.setDamage(damage);
                round.setDescription(p2.getPlayerName() + " 对 " + p1.getPlayerName() + " 造成 " + damage + " 点伤害");
                p2Score++;
            }
            
            battle.getRounds().add(round);
        }
        
        battle.setPlayer1Score(p1Score);
        battle.setPlayer2Score(p2Score);
        battle.setEndTime(System.currentTimeMillis());
        
        // 判定胜负
        if (p1Score > p2Score) {
            battle.setWinnerId(p1.getOdUserId());
            battle.setWinnerName(p1.getPlayerName());
            p1.setWins(p1.getWins() + 1);
            p1.setFlags(p1.getFlags() + 1);
            p1.setStatus(PlayerStatus.WAITING);
            p2.setLosses(p2.getLosses() + 1);
            p2.setStatus(PlayerStatus.SPECTATING);
            log.info("对战结果: {} vs {} = {}:{}，胜者: {}", 
                    p1.getPlayerName(), p2.getPlayerName(), p1Score, p2Score, p1.getPlayerName());
        } else {
            battle.setWinnerId(p2.getOdUserId());
            battle.setWinnerName(p2.getPlayerName());
            p2.setWins(p2.getWins() + 1);
            p2.setFlags(p2.getFlags() + 1);
            p2.setStatus(PlayerStatus.WAITING);
            p1.setLosses(p1.getLosses() + 1);
            p1.setStatus(PlayerStatus.SPECTATING);
            log.info("对战结果: {} vs {} = {}:{}，胜者: {}", 
                    p1.getPlayerName(), p2.getPlayerName(), p1Score, p2Score, p2.getPlayerName());
        }
        
        // 检查是否需要进入下一轮
        checkAndStartNextRound();
    }
    
    /**
     * 检查并开始下一轮
     */
    private void checkAndStartNextRound() {
        // 检查是否所有战斗都已结束
        boolean allBattlesFinished = todayWar.getParticipants().stream()
                .noneMatch(p -> p.getStatus() == PlayerStatus.IN_BATTLE);
        
        if (allBattlesFinished) {
            // 获取剩余玩家
            List<WarParticipant> remaining = todayWar.getParticipants().stream()
                    .filter(p -> p.getStatus() == PlayerStatus.WAITING)
                    .collect(Collectors.toList());
            
            // 检查是否都是同一联盟
            Set<String> alliances = remaining.stream()
                    .map(WarParticipant::getAllianceId)
                    .collect(Collectors.toSet());
            
            if (alliances.size() <= 1 || remaining.size() <= 1) {
                endWar();
            } else {
                todayWar.setCurrentRound(todayWar.getCurrentRound() + 1);
                startNextRound();
            }
        }
    }
    
    /**
     * 结束盟战
     */
    private void endWar() {
        todayWar.setStatus(WarStatus.FINISHED);
        todayWar.setEndTime(System.currentTimeMillis());
        
        // 计算联盟排名
        calculateAllianceRanks();
        
        // 计算个人排名
        calculatePlayerRanks();
        
        // 发放奖励
        distributeRewards();
        
        log.info("盟战结束");
    }
    
    /**
     * 计算联盟排名
     */
    private void calculateAllianceRanks() {
        // 按联盟汇总数据
        Map<String, AllianceRank> allianceMap = new HashMap<>();
        
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
                        .rewards(new ArrayList<>())
                        .build()
            );
            
            rank.setTotalFlags(rank.getTotalFlags() + p.getFlags());
            rank.setParticipantCount(rank.getParticipantCount() + 1);
            rank.setWins(rank.getWins() + p.getWins());
            rank.setLosses(rank.getLosses() + p.getLosses());
        }
        
        // 排序（先按剩余人数，再按军旗数）
        List<AllianceRank> sortedRanks = allianceMap.values().stream()
                .sorted((a, b) -> {
                    // 首先按是否有存活玩家
                    long aAlive = todayWar.getParticipants().stream()
                            .filter(p -> p.getAllianceId().equals(a.getAllianceId()) && p.getStatus() == PlayerStatus.WAITING)
                            .count();
                    long bAlive = todayWar.getParticipants().stream()
                            .filter(p -> p.getAllianceId().equals(b.getAllianceId()) && p.getStatus() == PlayerStatus.WAITING)
                            .count();
                    if (aAlive != bAlive) return Long.compare(bAlive, aAlive);
                    // 然后按军旗数
                    return Integer.compare(b.getTotalFlags(), a.getTotalFlags());
                })
                .collect(Collectors.toList());
        
        // 设置排名和奖励
        for (int i = 0; i < sortedRanks.size(); i++) {
            AllianceRank rank = sortedRanks.get(i);
            rank.setRank(i + 1);
            
            // 设置奖励
            if (i == 0) {
                rank.setRewards(Arrays.asList("盟战宝箱x5", "黄金x5000", "高级招贤令x10", "传说装备箱x1"));
            } else if (i == 1) {
                rank.setRewards(Arrays.asList("盟战宝箱x3", "黄金x3000", "高级招贤令x5", "史诗装备箱x1"));
            } else if (i == 2) {
                rank.setRewards(Arrays.asList("盟战宝箱x2", "黄金x2000", "高级招贤令x3", "稀有装备箱x1"));
            } else if (i <= 4) {
                rank.setRewards(Arrays.asList("盟战宝箱x1", "黄金x1000", "高级招贤令x1"));
            } else {
                rank.setRewards(Arrays.asList("盟战礼盒x1", "白银x5000"));
            }
            
            // 额外军旗奖励
            if (rank.getTotalFlags() > 0) {
                rank.getRewards().add("军旗资源奖励x" + rank.getTotalFlags());
            }
        }
        
        todayWar.setAllianceRanks(sortedRanks);
    }
    
    /**
     * 计算个人排名
     */
    private void calculatePlayerRanks() {
        List<PlayerRank> playerRanks = todayWar.getParticipants().stream()
                .sorted((a, b) -> {
                    // 首先按胜场
                    if (!a.getWins().equals(b.getWins())) {
                        return Integer.compare(b.getWins(), a.getWins());
                    }
                    // 然后按军旗
                    return Integer.compare(b.getFlags(), a.getFlags());
                })
                .map(p -> {
                    List<String> rewards = new ArrayList<>();
                    rewards.add("盟战礼盒x1"); // 所有参战者都有
                    return PlayerRank.builder()
                            .odUserId(p.getOdUserId())
                            .playerName(p.getPlayerName())
                            .allianceName(p.getAllianceName())
                            .wins(p.getWins())
                            .flags(p.getFlags())
                            .rewards(rewards)
                            .build();
                })
                .collect(Collectors.toList());
        
        // 设置排名和额外奖励
        for (int i = 0; i < playerRanks.size(); i++) {
            PlayerRank rank = playerRanks.get(i);
            rank.setRank(i + 1);
            
            // 前5名额外奖励
            if (i < 5) {
                rank.getRewards().add("盟战宝箱x" + (5 - i));
                rank.getRewards().add("黄金x" + (2000 - i * 300));
            }
        }
        
        todayWar.setPlayerRanks(playerRanks);
    }
    
    /**
     * 发放奖励（实际项目中应该通过邮件或背包发放）
     */
    private void distributeRewards() {
        log.info("发放盟战奖励...");
        // TODO: 实际发放奖励到玩家邮箱或背包
    }
    
    /**
     * 获取对战记录
     */
    public List<WarBattle> getBattleHistory(String odUserId) {
        return todayWar.getBattles().stream()
                .filter(b -> b.getPlayer1Id().equals(odUserId) || b.getPlayer2Id().equals(odUserId))
                .collect(Collectors.toList());
    }
    
    /**
     * 获取所有对战记录
     */
    public List<WarBattle> getAllBattles() {
        return todayWar.getBattles();
    }
    
    /**
     * 获取参战玩家列表
     */
    public List<WarParticipant> getParticipants() {
        return todayWar.getParticipants();
    }
    
    /**
     * 重置盟战（测试用）
     */
    public void resetWar() {
        String today = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
        todayWar = AllianceWar.createNew(today);
        warStore.put(today, todayWar);
        log.info("重置盟战: {}", today);
    }
}
