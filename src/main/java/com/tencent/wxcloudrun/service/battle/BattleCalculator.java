package com.tencent.wxcloudrun.service.battle;

import java.util.*;

/**
 * 统一战斗计算器
 *
 * 武将四维:
 *   武勇(valor)  → 攻击时伤害输出加成: valorBonus = 1 + valor/400
 *   统御(command) → 防守时伤害减免:     commandReduction = command/(command+400)
 *   攻击(attack)  → 直接加到军队总攻击
 *   防御(defense)  → 直接加到军队总防御
 *
 * 核心公式（破防制）:
 *   netDamage     = max(0, totalAtk - totalDef × 1.25)
 *   finalDamage   = netDamage × typeBonus × valorBonus × (1 - commandReduction) × randomFactor
 *   soldierLoss   = finalDamage > 0 ? max(1, round(finalDamage × 100 / soldierLife)) : random(1~5)
 *
 * 攻击只能破 80% 数值的防御（如1000攻只破800防），破不了防则随机1~5微伤
 * KILL_MULTIPLIER = 100，DEFENSE_FACTOR = 1.25
 */
public class BattleCalculator {

    private static final Random random = new Random();

    /** 击杀倍率：控制战斗节奏，目标5~8回合内分出胜负 */
    public static final double DEFENSE_FACTOR = 1.25;
    public static final int KILL_MULTIPLIER = 100;

    // ==================== 兵种基础属性表（来自APK ArmyService.json） ====================
    // [armyType 1=步 2=骑 3=弓][tier 1~10] → {life, att, def, sp, hit, mis}

    public static final int[][] INFANTRY_STATS = {
        //  life, att,  def,   sp, hit, mis
        {}, // 0阶占位
        { 240,  110,  120,  15, 0, 0 },  // 1阶 乡勇
        { 260,  210,  250,  17, 0, 0 },  // 2阶 民兵
        { 280,  290,  400,  19, 0, 0 },  // 3阶 轻步兵
        { 300,  320,  600,  21, 0, 0 },  // 4阶 朴刀兵
        { 320,  400,  750,  23, 0, 0 },  // 5阶 刀盾兵
        { 340,  480,  900,  25, 0, 0 },  // 6阶 藤甲兵
        { 360,  560, 1050,  26, 0, 0 },  // 7阶 重步兵
        { 380,  640, 1200,  27, 0, 0 },  // 8阶 大戟士
        { 400,  720, 1350,  28, 0, 0 },  // 9阶 陌刀兵
        { 420,  800, 1500,  29, 0, 0 },  // 10阶 虎贲禁卫军
    };

    public static final int[][] CAVALRY_STATS = {
        {},
        { 230,  150,  110,  20, 0, 0 },
        { 240,  300,  210,  22, 0, 0 },
        { 250,  450,  290,  24, 0, 0 },
        { 260,  600,  320,  26, 0, 0 },
        { 270,  750,  400,  28, 0, 0 },
        { 280,  900,  480,  30, 0, 0 },
        { 290, 1050,  560,  31, 0, 0 },
        { 300, 1200,  640,  32, 0, 0 },
        { 310, 1350,  720,  33, 0, 0 },
        { 320, 1500,  800,  34, 0, 0 },
    };

    public static final int[][] ARCHER_STATS = {
        {},
        { 230,  200,  100,  10, 0, 0 },
        { 240,  400,  150,  12, 0, 0 },
        { 250,  650,  200,  14, 0, 0 },
        { 260,  900,  250,  16, 0, 0 },
        { 270, 1200,  300,  18, 0, 0 },
        { 280, 1500,  350,  20, 0, 0 },
        { 290, 1800,  400,  21, 0, 0 },
        { 300, 2000,  450,  22, 0, 0 },
        { 310, 2300,  500,  23, 0, 0 },
        { 320, 2600,  550,  24, 0, 0 },
    };

    // ==================== NPC 专用士兵属性表（APK ArmyService.json 20xxx） ====================
    // 高等级时比玩家士兵更强（更多生命/速度/闪避），用于战役NPC

    public static final int[][] NPC_INFANTRY_STATS = {
        {},
        { 240,  110,  120,  15, 0, 0 },
        { 260,  210,  250,  17, 0, 0 },
        { 280,  290,  400,  19, 0, 0 },
        { 310,  320,  600,  21, 0, 3 },
        { 340,  400,  750,  23, 0, 5 },
        { 370,  480,  900,  30, 0, 6 },
        { 400,  560, 1050,  37, 0, 7 },
        { 440,  640, 1200,  48, 0, 8 },
        { 460,  720, 1350,  49, 0, 9 },
        { 480,  800, 1500,  50, 0, 10 },
    };

    public static final int[][] NPC_CAVALRY_STATS = {
        {},
        { 230,  150,  110,  10, 0, 0 },
        { 240,  300,  210,  12, 0, 0 },
        { 250,  450,  290,  14, 0, 0 },
        { 260,  650,  320,  16, 0, 0 },
        { 270,  900,  400,  20, 0, 0 },
        { 280, 1220,  480,  28, 0, 0 },
        { 290, 1400,  560,  42, 0, 0 },
        { 300, 1600,  640,  53, 0, 0 },
        { 310, 1800,  720,  54, 0, 0 },
        { 320, 2000,  800,  55, 0, 0 },
    };

    public static final int[][] NPC_ARCHER_STATS = {
        {},
        { 230,  200,  100,  10, 0, 0 },
        { 240,  400,  150,  12, 0, 0 },
        { 250,  650,  200,  14, 0, 0 },
        { 260,  900,  250,  16, 0, 0 },
        { 270, 1370,  300,  18, 0, 0 },
        { 280, 1820,  350,  25, 0, 0 },
        { 290, 2120,  400,  32, 0, 0 },
        { 300, 2400,  450,  43, 0, 0 },
        { 310, 2700,  500,  44, 0, 0 },
        { 320, 3100,  550,  45, 0, 0 },
    };

    /** 获取NPC兵种属性 [life, att, def, sp, hit, mis] */
    public static int[] getNpcSoldierStats(int troopType, int tier) {
        tier = Math.max(1, Math.min(10, tier));
        switch (troopType) {
            case 2: return NPC_CAVALRY_STATS[tier];
            case 3: return NPC_ARCHER_STATS[tier];
            default: return NPC_INFANTRY_STATS[tier];
        }
    }

    /** 获取兵种基础属性 [life, att, def, sp, hit, mis] */
    public static int[] getSoldierStats(int troopType, int tier) {
        tier = Math.max(1, Math.min(10, tier));
        switch (troopType) {
            case 2: return CAVALRY_STATS[tier];
            case 3: return ARCHER_STATS[tier];
            default: return INFANTRY_STATS[tier];
        }
    }

    public static int getSoldierLife(int troopType, int tier) {
        return getSoldierStats(troopType, tier)[0];
    }

    public static int getSoldierAtt(int troopType, int tier) {
        return getSoldierStats(troopType, tier)[1];
    }

    public static int getSoldierDef(int troopType, int tier) {
        return getSoldierStats(troopType, tier)[2];
    }

    public static int getSoldierSpeed(int troopType, int tier) {
        return getSoldierStats(troopType, tier)[3];
    }

    // ==================== 阵型等级配置（来自APK formation_cfg.json） ====================
    // [formationLevel 1~10] → {maxPeople, addAtt, addDef}

    public static final int[][] FORMATION_CONFIG = {
        {},
        { 100, 230, 230 },   // 1级 队
        { 200, 250, 250 },   // 2级 伙
        { 300, 270, 270 },   // 3级 哨
        { 400, 290, 290 },   // 4级 岗
        { 500, 310, 310 },   // 5级 都
        { 600, 330, 330 },   // 6级 营
        { 700, 350, 350 },   // 7级 团
        { 800, 370, 370 },   // 8级 师
        { 900, 390, 390 },   // 9级 旅
        { 1000, 410, 410 },  // 10级 军
    };

    public static int getFormationMaxPeople(int level) {
        level = Math.max(1, Math.min(10, level));
        return FORMATION_CONFIG[level][0];
    }

    public static int getFormationAddAtt(int level) {
        level = Math.max(1, Math.min(10, level));
        return FORMATION_CONFIG[level][1];
    }

    public static int getFormationAddDef(int level) {
        level = Math.max(1, Math.min(10, level));
        return FORMATION_CONFIG[level][2];
    }

    public static int maxPeopleToFormationLevel(int maxPeople) {
        for (int i = FORMATION_CONFIG.length - 1; i >= 1; i--) {
            if (maxPeople >= FORMATION_CONFIG[i][0]) return i;
        }
        return 1;
    }

    /**
     * 根据角色等级推算阵型等级 (参照 formation_level_config 的解锁等级)
     */
    public static int levelToFormationLevel(int characterLevel) {
        if (characterLevel >= 80) return 10;
        if (characterLevel >= 70) return 9;
        if (characterLevel >= 60) return 8;
        if (characterLevel >= 50) return 7;
        if (characterLevel >= 40) return 6;
        if (characterLevel >= 30) return 5;
        if (characterLevel >= 20) return 4;
        if (characterLevel >= 10) return 3;
        if (characterLevel >= 5) return 2;
        return 1;
    }

    // ==================== 兵种克制 ====================

    public static double getTypeBonus(int attackerType, int targetType) {
        if (attackerType == targetType) return 1.0;
        if ((attackerType == 1 && targetType == 2) ||
            (attackerType == 2 && targetType == 3) ||
            (attackerType == 3 && targetType == 1)) {
            return 1.25;
        }
        if ((attackerType == 2 && targetType == 1) ||
            (attackerType == 3 && targetType == 2) ||
            (attackerType == 1 && targetType == 3)) {
            return 0.8;
        }
        return 1.0;
    }

    // ==================== 武勇 → 进攻伤害加成 ====================

    public static double getValorBonus(int valor) {
        return 1.0 + valor / 400.0;
    }

    // ==================== 统御 → 防守伤害减免 ====================

    public static double getCommandReduction(int command) {
        if (command <= 0) return 0;
        return (double) command / (command + 400.0);
    }

    // ==================== 闪避判定 ====================

    public static boolean isDodge(int attackerHit, int targetDodge) {
        double netDodge = Math.min(50, Math.max(0, targetDodge - attackerHit));
        return random.nextDouble() * 100 < netDodge;
    }

    // ==================== 核心伤害计算 ====================

    public static DamageResult calcDamage(BattleUnit attacker, BattleUnit target) {
        if (isDodge(attacker.hit, target.dodge)) {
            return new DamageResult(0, false, true, 0);
        }

        double atk = attacker.totalAttack;
        double def = target.totalDefense;

        double netDamage = Math.max(0, atk - def * DEFENSE_FACTOR);

        double typeBonus = getTypeBonus(attacker.troopType, target.troopType);
        double valorBonus = getValorBonus(attacker.valor);
        double commandReduction = getCommandReduction(target.command);
        double randomFactor = 0.95 + random.nextDouble() * 0.10;

        double finalDamage = netDamage
                * typeBonus
                * valorBonus
                * (1.0 - commandReduction)
                * randomFactor;

        // 名将特性：攻击方士兵伤害直加（不经攻防公式）
        finalDamage += attacker.traitDmgBonus;

        // 名将特性：防御方伤害抵抗（直减）
        if (target.traitDamageResist > 0) {
            finalDamage = Math.max(0, finalDamage - target.traitDamageResist);
        }

        // 名将特性：防御方士兵生命%提升
        int baseSoldierLife = attacker.targetSoldierLife > 0
                ? attacker.targetSoldierLife : getSoldierLife(target.troopType, target.soldierTier);
        int soldierLife = (int)(baseSoldierLife * (1.0 + target.traitLifePct / 100.0));

        int soldierLoss;
        if (finalDamage <= 0) {
            soldierLoss = 1 + random.nextInt(5);
        } else {
            soldierLoss = Math.max(1, (int) Math.round(finalDamage * KILL_MULTIPLIER / Math.max(1, soldierLife)));
        }
        if (target.soldierCount > 0) {
            soldierLoss = Math.max(1, Math.min(soldierLoss, target.soldierCount));
        } else {
            soldierLoss = 0;
        }

        boolean isCrit = false;
        return new DamageResult(soldierLoss, isCrit, false, soldierLoss);
    }

    // ==================== 兵种类型转换 ====================

    public static int parseTroopType(String type) {
        if (type == null) return 1;
        switch (type) {
            case "步": case "步兵": return 1;
            case "骑": case "骑兵": return 2;
            case "弓": case "弓兵": return 3;
            default: return 1;
        }
    }

    // ==================== 组装军队总属性 ====================

    /**
     * 根据武将属性 + 兵种 + 阵型等级 组装一个完整的BattleUnit
     */
    public static BattleUnit assembleBattleUnit(
            String name, int level,
            int genAttack, int genDefense, int genValor, int genCommand,
            int genDodge, int genMobility,
            int troopType, int soldierTier, int soldierCount, int maxSoldierCount,
            int formationLevel,
            int equipArmyAtt, int equipArmyDef, int equipArmySp, int equipArmyHit, int equipArmyMis,
            int traitAtkBonus, int traitDefBonus, int traitDmgBonus) {

        int[] soldierStats = getSoldierStats(troopType, soldierTier);

        BattleUnit u = new BattleUnit();
        u.name = name;
        u.level = level;
        u.troopType = troopType;
        u.soldierTier = soldierTier;
        u.soldierCount = soldierCount;
        u.maxSoldierCount = maxSoldierCount;
        u.soldierLife = soldierStats[0];

        u.totalAttack = soldierStats[1] + getFormationAddAtt(formationLevel)
                + genAttack + equipArmyAtt + traitAtkBonus;
        u.totalDefense = soldierStats[2] + getFormationAddDef(formationLevel)
                + genDefense + equipArmyDef + traitDefBonus;

        u.valor = genValor;
        u.command = genCommand;
        u.dodge = (int)(genDodge + soldierStats[5] + equipArmyMis);
        u.hit = soldierStats[4] + equipArmyHit;
        u.mobility = genMobility + soldierStats[3] + equipArmySp;
        u.traitDmgBonus = traitDmgBonus;

        return u;
    }

    // ==================== NPC 专用 BattleUnit 组装（APK风格：无武将四维） ====================

    /**
     * 战役NPC使用：总攻防 = NPC士兵属性 + 阵型加成，无武将四维加成
     */
    public static BattleUnit assembleNpcBattleUnit(
            String name, int level, int troopType, int soldierTier,
            int soldierCount, int maxSoldierCount, int formationLevel,
            int valor, int command) {

        int[] npcStats = getNpcSoldierStats(troopType, soldierTier);

        BattleUnit u = new BattleUnit();
        u.name = name;
        u.level = level;
        u.troopType = troopType;
        u.soldierTier = soldierTier;
        u.soldierCount = soldierCount;
        u.maxSoldierCount = maxSoldierCount;
        u.soldierLife = npcStats[0];

        u.totalAttack = npcStats[1] + getFormationAddAtt(formationLevel);
        u.totalDefense = npcStats[2] + getFormationAddDef(formationLevel);
        u.valor = valor;
        u.command = command;
        u.dodge = npcStats[5];
        u.hit = npcStats[4];
        u.mobility = npcStats[3];
        u.traitDmgBonus = 0;

        return u;
    }

    // ==================== 带兵法的伤害计算 ====================

    public static TacticsResult calcDamageWithTactics(BattleUnit attacker, BattleUnit target,
                                                       List<BattleUnit> allEnemies) {
        TacticsResult result = new TacticsResult();
        result.triggered = false;
        result.tacticsName = null;

        if (attacker.tacticsId == null || attacker.tacticsId.isEmpty()) {
            result.damages.add(calcDamage(attacker, target));
            return result;
        }

        String tid = attacker.tacticsId;

        // ===== 步兵被动兵法 =====
        if ("t_infantry_1".equals(tid)) {
            // 方圆阵 - 全减伤
            // 效果在被攻击时触发，此处标记
        } else if ("t_infantry_2".equals(tid)) {
            // 偃月阵 - 减骑兵伤害
        } else if ("t_infantry_3".equals(tid)) {
            // 长蛇阵 - 减弓兵伤害
        }

        // ===== 弓兵被动: 落月弓 =====
        if ("t_archer_3".equals(tid)) {
            int bonusAtk = (int)(attacker.tacticsEffectValue);
            double dmgMul = 1.0 + attacker.tacticsEffectValue / 100.0;
            int origAtk = attacker.totalAttack;
            attacker.totalAttack = origAtk + bonusAtk;
            DamageResult dr = calcDamage(attacker, target);
            dr.soldierLoss = Math.max(1, (int)(dr.soldierLoss * dmgMul));
            dr.soldierLoss = Math.max(1, Math.min(dr.soldierLoss, target.soldierCount));
            result.damages.add(dr);
            attacker.totalAttack = origAtk;
            return result;
        }

        // ===== 主动兵法发动判定 =====
        double triggerRate = (attacker.tacticsTriggerRate + attacker.tacticsTriggerBonus) * attacker.tacticsTriggerMultiplier;
        boolean triggered = random.nextDouble() * 100 < triggerRate;

        if (!triggered) {
            result.damages.add(calcDamage(attacker, target));
            return result;
        }

        result.triggered = true;
        result.tacticsName = attacker.tacticsName;

        switch (tid) {
            case "t_cavalry_1": {
                int bonusAtk = (int) attacker.tacticsEffectValue;
                double dmgMul = 1.0 + attacker.tacticsEffectValue / 200.0;
                int origAtk = attacker.totalAttack;
                attacker.totalAttack = origAtk + bonusAtk;
                DamageResult dr = calcDamage(attacker, target);
                dr.soldierLoss = Math.max(1, (int)(dr.soldierLoss * dmgMul));
                dr.soldierLoss = Math.max(1, Math.min(dr.soldierLoss, target.soldierCount));
                result.damages.add(dr);
                result.effectDesc = "铁骑冲锋！";
                attacker.totalAttack = origAtk;
                break;
            }
            case "t_cavalry_2": {
                result.effectDesc = "声东击西！攻击敌方弓兵";
                int bonusAtk = (int) attacker.tacticsEffectValue;
                double dmgMul = 1.0 + attacker.tacticsEffectValue / 200.0;
                List<BattleUnit> archers = new ArrayList<>();
                if (allEnemies != null) {
                    for (BattleUnit e : allEnemies) {
                        if (e.troopType == 3 && e.soldierCount > 0) archers.add(e);
                    }
                }
                BattleUnit actualTarget = archers.isEmpty() ? target : archers.get(random.nextInt(archers.size()));
                int origAtk = attacker.totalAttack;
                attacker.totalAttack = origAtk + bonusAtk;
                DamageResult dr = calcDamage(attacker, actualTarget);
                dr.soldierLoss = Math.max(1, (int)(dr.soldierLoss * dmgMul));
                dr.soldierLoss = Math.max(1, Math.min(dr.soldierLoss, actualTarget.soldierCount));
                result.damages.add(dr);
                if (!archers.isEmpty()) result.specialTarget = actualTarget.name;
                attacker.totalAttack = origAtk;
                break;
            }
            case "t_cavalry_3": {
                double dmgRatio = attacker.tacticsEffectValue / 100.0;
                result.effectDesc = "以逸待劳！伏击";
                int origAtk = attacker.totalAttack;
                DamageResult dr = calcDamage(attacker, target);
                dr.soldierLoss = Math.max(1, (int)(dr.soldierLoss * Math.max(0.3, dmgRatio)));
                dr.soldierLoss = Math.max(1, Math.min(dr.soldierLoss, target.soldierCount));
                result.damages.add(dr);
                attacker.totalAttack = origAtk;
                break;
            }
            case "t_cavalry_4": {
                double pierceRatio = attacker.tacticsEffectValue / 100.0;
                result.effectDesc = "战神突击！贯穿攻击";
                if (allEnemies != null) {
                    int atkRow = attacker.position % 2;
                    for (BattleUnit e : allEnemies) {
                        if (e.soldierCount > 0 && e.position % 2 == atkRow) {
                            DamageResult dr = calcDamage(attacker, e);
                            dr.soldierLoss = Math.max(1, (int)(dr.soldierLoss * pierceRatio));
                            dr.soldierLoss = Math.max(1, Math.min(dr.soldierLoss, e.soldierCount));
                            result.damages.add(dr);
                        }
                    }
                }
                if (result.damages.isEmpty()) {
                    result.damages.add(calcDamage(attacker, target));
                }
                break;
            }
            case "t_archer_1": {
                result.effectDesc = "连射！发动两次攻击";
                result.damages.add(calcDamage(attacker, target));
                result.damages.add(calcDamage(attacker, target));
                break;
            }
            case "t_archer_2":
            case "t_special_lvbu": {
                double aoeRatio = attacker.tacticsEffectValue / 100.0;
                result.effectDesc = "t_special_lvbu".equals(tid) ? "战神突击！" : "长虹贯日！";
                if (allEnemies != null) {
                    int atkRow = attacker.position % 2;
                    for (BattleUnit e : allEnemies) {
                        if (e.soldierCount > 0 && e.position % 2 == atkRow) {
                            DamageResult dr = calcDamage(attacker, e);
                            dr.soldierLoss = Math.max(1, (int)(dr.soldierLoss * aoeRatio));
                            dr.soldierLoss = Math.max(1, Math.min(dr.soldierLoss, e.soldierCount));
                            result.damages.add(dr);
                        }
                    }
                }
                if (result.damages.isEmpty()) {
                    result.damages.add(calcDamage(attacker, target));
                }
                break;
            }
            default:
                result.damages.add(calcDamage(attacker, target));
                break;
        }

        return result;
    }

    // ==================== 数据类 ====================

    public static class BattleUnit {
        public int totalAttack;     // 军队总攻击 = 兵种att + 阵型addAtt + 武将攻击 + 装备 + 天赋
        public int totalDefense;    // 军队总防御 = 兵种def + 阵型addDef + 武将防御 + 装备 + 天赋
        public int valor;           // 武勇 → 进攻伤害加成
        public int command;         // 统御 → 防守伤害减免
        public int dodge;           // 闪避值
        public int hit;             // 命中值
        public int mobility;        // 机动 → 行动顺序
        public int troopType;       // 1步 2骑 3弓
        public int soldierTier;     // 兵阶 1~10
        public int soldierCount;    // 当前士兵数（=血量）
        public int maxSoldierCount; // 满编士兵数（=最大血量）
        public int soldierLife;     // 单兵生命值（用于计算杀伤）
        public int traitDmgBonus;   // 名将特性：士兵伤害直加
        public int traitDamageResist; // 名将特性：伤害抵抗
        public double traitLifePct;   // 名将特性：士兵生命%提升
        public boolean traitImmuneAmbush; // 名将特性：免疫偷袭
        public int targetSoldierLife; // 仅用于特殊覆写
        public int position;        // 阵型位置 0~5
        public String name;
        public int level;

        // 兵法相关
        public String tacticsId;
        public String tacticsName;
        public int tacticsLevel;
        public double tacticsTriggerRate;
        public double tacticsEffectValue;
        public double tacticsTriggerBonus;
        public double tacticsTriggerMultiplier = 1;

        public BattleUnit() {
            this.soldierLife = 240;
        }
    }

    public static class DamageResult {
        public int damage;          // 伤害值（=soldierLoss，用于兼容显示）
        public boolean isCrit;
        public boolean isDodge;
        public int soldierLoss;     // 士兵损失数（核心指标）
        public boolean isCounter;

        public DamageResult(int damage, boolean isCrit, boolean isDodge, int soldierLoss) {
            this.damage = damage;
            this.isCrit = isCrit;
            this.isDodge = isDodge;
            this.soldierLoss = soldierLoss;
        }
    }

    public static class TacticsResult {
        public boolean triggered;
        public String tacticsName;
        public String effectDesc;
        public String specialTarget;
        public List<DamageResult> damages = new ArrayList<>();
    }
}
