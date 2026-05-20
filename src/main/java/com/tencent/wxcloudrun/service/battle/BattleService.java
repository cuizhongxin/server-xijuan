package com.tencent.wxcloudrun.service.battle;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger logger = LoggerFactory.getLogger(BattleService.class);
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
        return fight(sideA, sideB, maxRounds, "UNSPECIFIED", null);
    }

    public BattleReport fight(List<BattleCalculator.BattleUnit> sideA,
                              List<BattleCalculator.BattleUnit> sideB,
                              int maxRounds,
                              String battleType,
                              String battleKey) {
        if (maxRounds <= 0) maxRounds = DEFAULT_MAX_ROUNDS;
        String type = (battleType == null || battleType.isEmpty()) ? "UNSPECIFIED" : battleType;
        String key = (battleKey == null || battleKey.isEmpty()) ? "-" : battleKey;
        logger.info("[BattleFlow] START type={} key={} maxRounds={} sideA={} sideB={}",
                type, key, maxRounds, formatUnits(sideA), formatUnits(sideB));

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
            logger.info("[BattleFlow] ROUND={} type={} key={} begin", roundNum, type, key);

            for (BattleCalculator.BattleUnit attacker : allUnits) {
                if (attacker.soldierCount <= 0) continue;

                boolean attackerIsA = Boolean.TRUE.equals(isSideA.get(attacker));
                List<BattleCalculator.BattleUnit> enemies = attackerIsA ? sideB : sideA;
                List<BattleCalculator.BattleUnit> aliveEnemies = enemies.stream()
                        .filter(e -> e.soldierCount > 0).collect(Collectors.toList());

                if (aliveEnemies.isEmpty()) break;

                BattleCalculator.BattleUnit target = pickTarget(attacker, aliveEnemies);
                logger.info("[BattleFlow] ROUND={} type={} key={} action attacker={} target={} attackerStats={} targetStats={}",
                        roundNum, type, key, safeName(attacker), safeName(target), formatUnit(attacker), formatUnit(target));

                BattleCalculator.TacticsResult tr = BattleCalculator.calcDamageWithTactics(
                        attacker, target, aliveEnemies);

                // 以逸待劳打断判定：攻击方触发声东击西时，检查防守方全队是否有人装备以逸待劳
                BattleCalculator.BattleUnit counterUnit = BattleCalculator.findCounterUnit(aliveEnemies, attacker, tr.triggered);
                boolean countered = counterUnit != null && BattleCalculator.rollCounter(counterUnit);

                ActionLog action = new ActionLog();
                action.attackerName = attacker.name;
                action.targetName = target.name;
                action.attackerIsA = attackerIsA;
                action.attackerIdx = attackerIsA ? sideA.indexOf(attacker) : sideB.indexOf(attacker);
                action.targetIdx = attackerIsA ? sideB.indexOf(target) : sideA.indexOf(target);
                action.hits = new ArrayList<>();

                if (countered && counterUnit != null) {
                    action.tacticsTriggered = true;
                    action.tacticsName = tr.tacticsName;
                    action.effectDesc = "声东击西被" + counterUnit.name + "以逸待劳打断！";
                    logger.info("[BattleFlow] ROUND={} type={} key={} action interrupted by counterUnit={} counterStats={}",
                            roundNum, type, key, safeName(counterUnit), formatUnit(counterUnit));
                    HitDetail hit = new HitDetail();
                    hit.soldierLoss = 0;
                    hit.targetRemaining = target.soldierCount;
                    action.hits.add(hit);
                } else {
                    action.tacticsTriggered = tr.triggered;
                    action.tacticsName = tr.tacticsName;
                    action.effectDesc = tr.effectDesc;
                    action.specialTarget = tr.specialTarget;
                    List<ActionLog> reflectActions = new ArrayList<>();
                    for (BattleCalculator.DamageResult dr : tr.damages) {
                        BattleCalculator.BattleUnit actualTarget = dr.targetUnit != null ? dr.targetUnit : target;
                        int before = actualTarget.soldierCount;
                        HitDetail hit = new HitDetail();
                        hit.isDodge = dr.isDodge;
                        hit.soldierLoss = dr.soldierLoss;
                        hit.isCrit = dr.isCrit;
                        if (!dr.isDodge) {
                            actualTarget.soldierCount = Math.max(0, actualTarget.soldierCount - dr.soldierLoss);
                        }
                        logger.info("[BattleFlow] ROUND={} type={} key={} hit attacker={} -> target={} dodge={} soldierLoss={} targetBefore={} targetAfter={} reflectLoss={}",
                                roundNum, type, key, safeName(attacker), safeName(actualTarget), dr.isDodge, dr.soldierLoss,
                                before, actualTarget.soldierCount, dr.reflectLoss);
                        hit.targetRemaining = actualTarget.soldierCount;
                        hit.targetIdx = attackerIsA ? sideB.indexOf(actualTarget) : sideA.indexOf(actualTarget);
                        hit.targetName = actualTarget.name;
                        action.hits.add(hit);

                        // 防守方反伤（却月阵/雁行阵）
                        if (dr.reflectLoss > 0 && attacker.soldierCount > 0) {
                            int reflectLoss = Math.min(dr.reflectLoss, attacker.soldierCount);
                            attacker.soldierCount = Math.max(0, attacker.soldierCount - reflectLoss);
                            ActionLog reflectAction = new ActionLog();
                            reflectAction.attackerName = actualTarget.name;
                            reflectAction.targetName = attacker.name;
                            reflectAction.attackerIsA = !attackerIsA;
                            reflectAction.attackerIdx = attackerIsA ? sideB.indexOf(actualTarget) : sideA.indexOf(actualTarget);
                            reflectAction.targetIdx = attackerIsA ? sideA.indexOf(attacker) : sideB.indexOf(attacker);
                            reflectAction.isCounter = true;
                            reflectAction.effectDesc = "反伤";
                            reflectAction.hits = new ArrayList<>();
                            HitDetail reflectHit = new HitDetail();
                            reflectHit.soldierLoss = reflectLoss;
                            reflectHit.targetRemaining = attacker.soldierCount;
                            reflectHit.targetIdx = reflectAction.targetIdx;
                            reflectHit.targetName = attacker.name;
                            reflectHit.isCounter = true;
                            reflectAction.hits.add(reflectHit);
                            reflectActions.add(reflectAction);
                            logger.info("[BattleFlow] ROUND={} type={} key={} reflect source={} target={} reflectLoss={} targetAfter={}",
                                    roundNum, type, key, safeName(actualTarget), safeName(attacker), reflectLoss, attacker.soldierCount);
                        }
                    }
                    if (!reflectActions.isEmpty()) {
                        roundLog.actions.addAll(reflectActions);
                    }
                }

                roundLog.actions.add(action);

                // 以逸待劳反击：打断声东击西后，由装备以逸待劳的武将对发动者造成伤害
                if (countered && counterUnit != null) {
                    BattleCalculator.DamageResult counterDr = BattleCalculator.calcCounterDamage(counterUnit, attacker);
                    ActionLog counterAction = new ActionLog();
                    counterAction.attackerName = counterUnit.name;
                    counterAction.targetName = attacker.name;
                    counterAction.attackerIsA = !attackerIsA;
                    counterAction.attackerIdx = attackerIsA ? sideB.indexOf(counterUnit) : sideA.indexOf(counterUnit);
                    counterAction.targetIdx = attackerIsA ? sideA.indexOf(attacker) : sideB.indexOf(attacker);
                    counterAction.isCounter = true;
                    counterAction.hits = new ArrayList<>();

                    HitDetail counterHit = new HitDetail();
                    counterHit.isDodge = counterDr.isDodge;
                    counterHit.soldierLoss = counterDr.soldierLoss;
                    counterHit.isCounter = true;
                    if (!counterDr.isDodge) {
                        attacker.soldierCount = Math.max(0, attacker.soldierCount - counterDr.soldierLoss);
                    }
                    logger.info("[BattleFlow] ROUND={} type={} key={} counter attacker={} target={} dodge={} soldierLoss={} targetAfter={}",
                            roundNum, type, key, safeName(counterUnit), safeName(attacker), counterDr.isDodge,
                            counterDr.soldierLoss, attacker.soldierCount);
                    counterHit.targetRemaining = attacker.soldierCount;
                    counterAction.hits.add(counterHit);
                    roundLog.actions.add(counterAction);
                }

                if (sideAAllDead(sideA) || sideAAllDead(sideB)) break;
            }

            rounds.add(roundLog);
            logger.info("[BattleFlow] ROUND={} type={} key={} end sideARemaining={} sideBRemaining={}",
                    roundNum, type, key,
                    sideA.stream().mapToInt(u -> Math.max(0, u.soldierCount)).sum(),
                    sideB.stream().mapToInt(u -> Math.max(0, u.soldierCount)).sum());
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
            report.sideASummary.add(new UnitSummary(u));
        }
        report.sideBSummary = new ArrayList<>();
        for (BattleCalculator.BattleUnit u : sideB) {
            report.sideBSummary.add(new UnitSummary(u));
        }
        logger.info("[BattleFlow] END type={} key={} victoryA={} totalRounds={} sideARemaining={} sideBRemaining={} sideAFinal={} sideBFinal={}",
                type, key, report.victoryA, report.totalRounds, report.sideARemaining, report.sideBRemaining,
                formatUnits(sideA), formatUnits(sideB));

        return report;
    }

    public BattleReport fight(List<BattleCalculator.BattleUnit> sideA,
                              List<BattleCalculator.BattleUnit> sideB) {
        return fight(sideA, sideB, DEFAULT_MAX_ROUNDS, "UNSPECIFIED", null);
    }

    private BattleCalculator.BattleUnit pickTarget(BattleCalculator.BattleUnit attacker,
                                                    List<BattleCalculator.BattleUnit> aliveEnemies) {
        // 阵位规则: 0~5 按列存储(0,2,4为上排；1,3,5为下排)
        // 行判定必须按 position % 2，保证“同行优先攻击”是同一横排
        int atkRow = Math.floorMod(attacker.position, 2);
        List<BattleCalculator.BattleUnit> sameRow = aliveEnemies.stream()
                .filter(e -> Math.floorMod(e.position, 2) == atkRow).collect(Collectors.toList());
        List<BattleCalculator.BattleUnit> pool = sameRow.isEmpty() ? aliveEnemies : sameRow;
        return pool.stream().min(Comparator.comparingInt(e -> e.position)).orElse(aliveEnemies.get(0));
    }

    private boolean sideAAllDead(List<BattleCalculator.BattleUnit> side) {
        return side.stream().noneMatch(u -> u.soldierCount > 0);
    }

    private String safeName(BattleCalculator.BattleUnit u) {
        return u == null ? "UNKNOWN" : (u.name == null ? "UNKNOWN" : u.name);
    }

    private String formatUnit(BattleCalculator.BattleUnit u) {
        if (u == null) return "null";
        return String.format(Locale.ROOT,
                "%s[pos=%d,troop=%d,atk=%d,def=%d,valor=%d,command=%d,mob=%d,dodge=%d,hit=%d,life=%d,soldier=%d/%d,tactics=%s,lv=%d,trigger=%.2f,effect=%.2f]",
                safeName(u), u.position, u.troopType, u.totalAttack, u.totalDefense, u.valor, u.command,
                u.mobility, u.dodge, u.hit, u.soldierLife, u.soldierCount, u.maxSoldierCount,
                u.tacticsId, u.tacticsLevel, u.tacticsTriggerRate, u.tacticsEffectValue);
    }

    private String formatUnits(List<BattleCalculator.BattleUnit> units) {
        if (units == null || units.isEmpty()) return "[]";
        List<String> parts = new ArrayList<>();
        for (BattleCalculator.BattleUnit u : units) {
            parts.add(formatUnit(u));
        }
        return parts.toString();
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
                    String actionPrefix = a.isCounter ? "以逸待劳！反击" : "攻击";
                    for (HitDetail h : a.hits) {
                        String hitTarget = (h.targetName != null && !h.targetName.isEmpty()) ? h.targetName : a.targetName;
                        if (h.isDodge) {
                            log.add(String.format("第%d回合: %s%s，%s闪避！",
                                    r.roundNum, atkLabel, actionPrefix, hitTarget));
                        } else {
                            log.add(String.format("第%d回合: %s%s%s，减员%d，剩余兵力%d",
                                    r.roundNum, atkLabel, actionPrefix, hitTarget, h.soldierLoss, h.targetRemaining));
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
        public boolean attackerIsA;
        public int attackerIdx;
        public int targetIdx;
        public int soldierLoss;
        public boolean isDodge;
        public boolean tacticsTriggered;
        public String tacticsName;
        public String effectDesc;
        public String specialTarget;
        public boolean isCounter;
        public List<HitDetail> hits;
    }

    public static class HitDetail {
        public boolean isDodge;
        public boolean isCrit;
        public boolean isCounter;
        public int soldierLoss;
        public int targetRemaining;
        public int targetIdx = -1;
        public String targetName;
    }

    public static class UnitSummary {
        public String name;
        public int soldierCount;
        public int maxSoldierCount;
        public int troopType;
        public int soldierTier;
        public int level;
        public int position;
        public String tacticsId;
        public String tacticsName;
        public int tacticsLevel;
        public double tacticsTriggerRate;
        public double tacticsEffectValue;

        public UnitSummary(String name, int soldierCount, int maxSoldierCount, int troopType, int soldierTier, int level, int position) {
            this.name = name;
            this.soldierCount = soldierCount;
            this.maxSoldierCount = maxSoldierCount;
            this.troopType = troopType;
            this.soldierTier = soldierTier;
            this.level = level;
            this.position = position;
        }

        public UnitSummary(BattleCalculator.BattleUnit u) {
            this.name = u.name;
            this.soldierCount = u.soldierCount;
            this.maxSoldierCount = u.maxSoldierCount;
            this.troopType = u.troopType;
            this.soldierTier = u.soldierTier;
            this.level = u.level;
            this.position = u.position;
            this.tacticsId = u.tacticsId;
            this.tacticsName = u.tacticsName;
            this.tacticsLevel = u.tacticsLevel;
            this.tacticsTriggerRate = u.tacticsTriggerRate;
            this.tacticsEffectValue = u.tacticsEffectValue;
        }
    }
}
