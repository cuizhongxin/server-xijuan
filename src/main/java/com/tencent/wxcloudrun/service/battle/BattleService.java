package com.tencent.wxcloudrun.service.battle;

import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 统一战斗编排服务 —— 多单位回合制战斗
 *
 * 支持: 1v1、阵型vs阵型、阵型vsBoss 等所有战斗场景
 */
@Service
public class BattleService {

    private static final int DEFAULT_MAX_ROUNDS = 20;

    /**
     * 执行一场完整战斗
     *
     * @param sideA     A方单位列表（玩家方）
     * @param sideB     B方单位列表（敌方/Boss）
     * @param maxRounds 最大回合数，超过则A方判负
     * @return BattleReport 完整战报
     */
    public BattleReport fight(List<BattleCalculator.BattleUnit> sideA,
                              List<BattleCalculator.BattleUnit> sideB,
                              int maxRounds) {
        if (maxRounds <= 0) maxRounds = DEFAULT_MAX_ROUNDS;

        // 标记阵营
        Map<BattleCalculator.BattleUnit, Boolean> isSideA = new HashMap<>();
        for (BattleCalculator.BattleUnit u : sideA) isSideA.put(u, true);
        for (BattleCalculator.BattleUnit u : sideB) isSideA.put(u, false);

        // 按 mobility 降序排序（速度快的先行动）
        List<BattleCalculator.BattleUnit> allUnits = new ArrayList<>();
        allUnits.addAll(sideA);
        allUnits.addAll(sideB);
        allUnits.sort((a, b) -> b.mobility - a.mobility);

        List<RoundLog> rounds = new ArrayList<>();
        int roundNum = 0;

        while (roundNum < maxRounds) {
            roundNum++;
            RoundLog roundLog = new RoundLog();
            roundLog.roundNum = roundNum;

            for (BattleCalculator.BattleUnit attacker : allUnits) {
                if (attacker.soldierCount <= 0) continue;

                boolean attackerIsA = Boolean.TRUE.equals(isSideA.get(attacker));
                List<BattleCalculator.BattleUnit> enemies = attackerIsA ? sideB : sideA;
                List<BattleCalculator.BattleUnit> aliveEnemies = enemies.stream()
                        .filter(e -> e.soldierCount > 0).collect(Collectors.toList());

                if (aliveEnemies.isEmpty()) break;

                BattleCalculator.BattleUnit target = pickTarget(attacker, aliveEnemies);

                BattleCalculator.TacticsResult tr = BattleCalculator.calcDamageWithTactics(
                        attacker, target, aliveEnemies);

                ActionLog action = new ActionLog();
                action.attackerName = attacker.name;
                action.targetName = target.name;
                action.tacticsTriggered = tr.triggered;
                action.tacticsName = tr.tacticsName;
                action.effectDesc = tr.effectDesc;
                action.specialTarget = tr.specialTarget;
                action.hits = new ArrayList<>();

                for (BattleCalculator.DamageResult dr : tr.damages) {
                    HitDetail hit = new HitDetail();
                    hit.isDodge = dr.isDodge;
                    hit.soldierLoss = dr.soldierLoss;
                    hit.isCrit = dr.isCrit;
                    if (!dr.isDodge) {
                        target.soldierCount = Math.max(0, target.soldierCount - dr.soldierLoss);
                    }
                    hit.targetRemaining = target.soldierCount;
                    action.hits.add(hit);
                }

                roundLog.actions.add(action);

                if (sideAAllDead(sideA) || sideAAllDead(sideB)) break;
            }

            rounds.add(roundLog);
            if (sideAAllDead(sideA) || sideAAllDead(sideB)) break;
        }

        boolean bAllDead = sideAAllDead(sideB);
        boolean aAllDead = sideAAllDead(sideA);

        BattleReport report = new BattleReport();
        report.victoryA = bAllDead && !aAllDead;
        report.totalRounds = roundNum;
        report.rounds = rounds;
        report.sideARemaining = sideA.stream().mapToInt(u -> Math.max(0, u.soldierCount)).sum();
        report.sideBRemaining = sideB.stream().mapToInt(u -> Math.max(0, u.soldierCount)).sum();

        report.sideASummary = new ArrayList<>();
        for (BattleCalculator.BattleUnit u : sideA) {
            report.sideASummary.add(new UnitSummary(u.name, u.soldierCount, u.maxSoldierCount, u.troopType));
        }
        report.sideBSummary = new ArrayList<>();
        for (BattleCalculator.BattleUnit u : sideB) {
            report.sideBSummary.add(new UnitSummary(u.name, u.soldierCount, u.maxSoldierCount, u.troopType));
        }

        return report;
    }

    public BattleReport fight(List<BattleCalculator.BattleUnit> sideA,
                              List<BattleCalculator.BattleUnit> sideB) {
        return fight(sideA, sideB, DEFAULT_MAX_ROUNDS);
    }

    private BattleCalculator.BattleUnit pickTarget(BattleCalculator.BattleUnit attacker,
                                                    List<BattleCalculator.BattleUnit> aliveEnemies) {
        int atkRow = attacker.position % 2;
        List<BattleCalculator.BattleUnit> sameRow = aliveEnemies.stream()
                .filter(e -> e.position % 2 == atkRow).collect(Collectors.toList());
        List<BattleCalculator.BattleUnit> pool = sameRow.isEmpty() ? aliveEnemies : sameRow;
        return pool.stream().min(Comparator.comparingInt(e -> e.position)).orElse(aliveEnemies.get(0));
    }

    private boolean sideAAllDead(List<BattleCalculator.BattleUnit> side) {
        return side.stream().noneMatch(u -> u.soldierCount > 0);
    }

    // ==================== 数据类 ====================

    public static class BattleReport {
        public boolean victoryA;
        public int totalRounds;
        public List<RoundLog> rounds;
        public int sideARemaining;
        public int sideBRemaining;
        public List<UnitSummary> sideASummary;
        public List<UnitSummary> sideBSummary;

        public List<String> toBattleLog(String sideALabel, String sideBLabel) {
            List<String> log = new ArrayList<>();
            for (RoundLog r : rounds) {
                for (ActionLog a : r.actions) {
                    String atkLabel = a.attackerName;
                    if (a.tacticsTriggered && a.tacticsName != null) {
                        log.add(String.format("第%d回合: %s发动【%s】！%s",
                                r.roundNum, atkLabel, a.tacticsName,
                                a.effectDesc != null ? a.effectDesc : ""));
                    }
                    for (HitDetail h : a.hits) {
                        if (h.isDodge) {
                            log.add(String.format("第%d回合: %s攻击，%s闪避！",
                                    r.roundNum, atkLabel, a.targetName));
                        } else {
                            log.add(String.format("第%d回合: %s攻击%s，减员%d，剩余兵力%d",
                                    r.roundNum, atkLabel, a.targetName, h.soldierLoss, h.targetRemaining));
                        }
                    }
                }
            }
            log.add(victoryA ? "【" + sideALabel + "获胜】" : "【" + sideBLabel + "获胜】");
            return log;
        }
    }

    public static class RoundLog {
        public int roundNum;
        public List<ActionLog> actions = new ArrayList<>();
    }

    public static class ActionLog {
        public String attackerName;
        public String targetName;
        public int soldierLoss;
        public boolean isDodge;
        public boolean tacticsTriggered;
        public String tacticsName;
        public String effectDesc;
        public String specialTarget;
        public List<HitDetail> hits;
    }

    public static class HitDetail {
        public boolean isDodge;
        public boolean isCrit;
        public int soldierLoss;
        public int targetRemaining;
    }

    public static class UnitSummary {
        public String name;
        public int soldierCount;
        public int maxSoldierCount;
        public int troopType;

        public UnitSummary(String name, int soldierCount, int maxSoldierCount, int troopType) {
            this.name = name;
            this.soldierCount = soldierCount;
            this.maxSoldierCount = maxSoldierCount;
            this.troopType = troopType;
        }
    }
}
