package com.tencent.wxcloudrun.service.battle;

import java.util.*;

/**
 * 统一伤害计算器
 *
 * 核心公式: rawDamage = ATK^2 / (ATK + DEF)
 * 最终伤害 = rawDamage × 兵种克制 × 士兵系数 × 统御加成 × 暴击 × 随机波动
 *
 * 士兵系数 = 1 + (soldierCount × soldierHp × tierMultiplier) / 50000
 * 统御加成 = 1 + command / 500
 * 暴击率 = valor / 5 (%)，暴击伤害 x1.5
 * 闪避率 = dodge (%)，上限50%
 * 机动值 = 决定行动顺序
 */
public class BattleCalculator {

    private static final Random random = new Random();

    // ========== 兵种克制 ==========
    // 步(1)克骑(2), 骑(2)克弓(3), 弓(3)克步(1)
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

    // ========== 士兵系数 ==========
    public static double getSoldierBonus(int soldierCount, double soldierHp, double tierMultiplier) {
        if (soldierCount <= 0) return 1.0;
        return 1.0 + (soldierCount * soldierHp * tierMultiplier) / 50000.0;
    }

    // ========== 统御加成 ==========
    public static double getCommandBonus(int command) {
        return 1.0 + command / 500.0;
    }

    // ========== 暴击判定 ==========
    public static boolean isCrit(int valor) {
        double critRate = valor / 5.0; // 百分比
        return random.nextDouble() * 100 < critRate;
    }

    // ========== 闪避判定 ==========
    public static boolean isDodge(int dodge) {
        double dodgeRate = Math.min(50, dodge); // 上限50%
        return random.nextDouble() * 100 < dodgeRate;
    }

    // ========== 核心伤害计算 ==========
    public static DamageResult calcDamage(BattleUnit attacker, BattleUnit target) {
        // 闪避判定
        if (isDodge(target.dodge)) {
            return new DamageResult(0, false, true, 0);
        }

        double atk = attacker.attack;
        double def = target.defense;

        // 基础伤害: ATK^2 / (ATK + DEF)
        double rawDamage = atk * atk / (atk + def + 1); // +1防止除零

        // 兵种克制
        double typeBonus = getTypeBonus(attacker.troopType, target.troopType);

        // 士兵系数
        double soldierBonus = getSoldierBonus(
            attacker.soldierCount, attacker.soldierHp, attacker.tierMultiplier);

        // 统御加成
        double commandBonus = getCommandBonus(attacker.command);

        // 暴击
        boolean crit = isCrit(attacker.valor);
        double critMultiplier = crit ? 1.5 : 1.0;

        // 随机波动 0.9 ~ 1.1
        double randomFactor = 0.9 + random.nextDouble() * 0.2;

        // 最终伤害
        double finalDamage = rawDamage * typeBonus * soldierBonus * commandBonus
                           * critMultiplier * randomFactor;
        int damage = Math.max(1, (int) Math.floor(finalDamage));

        // 士兵减员
        int soldierLoss = 0;
        if (target.soldierCount > 0 && target.soldierHp > 0) {
            double effectiveHp = target.soldierHp * target.tierMultiplier;
            soldierLoss = Math.max(1, (int) Math.floor(damage / Math.max(1, effectiveHp)));
            soldierLoss = Math.min(soldierLoss, target.soldierCount);
        }

        return new DamageResult(damage, crit, false, soldierLoss);
    }

    // ========== 兵阶配置 ==========
    public static final double[] TIER_MULTIPLIERS = {
        0,     // 占位(0阶不存在)
        1.00,  // 1阶
        1.15,  // 2阶
        1.35,  // 3阶
        1.55,  // 4阶
        1.80,  // 5阶
        2.10,  // 6阶
        2.45,  // 7阶
        2.85,  // 8阶
        3.30   // 9阶
    };

    public static final int[] TIER_MAX_SOLDIERS = {
        0, 50, 100, 150, 200, 250, 300, 350, 400, 450
    };

    public static final int[] TIER_SOLDIER_HP = {
        0, 100, 130, 170, 220, 280, 350, 430, 520, 620
    };

    public static double getTierMultiplier(int tier) {
        if (tier < 1) return 1.0;
        if (tier >= TIER_MULTIPLIERS.length) return TIER_MULTIPLIERS[TIER_MULTIPLIERS.length - 1];
        return TIER_MULTIPLIERS[tier];
    }

    public static int getTierMaxSoldiers(int tier) {
        if (tier < 1) return 50;
        if (tier >= TIER_MAX_SOLDIERS.length) return TIER_MAX_SOLDIERS[TIER_MAX_SOLDIERS.length - 1];
        return TIER_MAX_SOLDIERS[tier];
    }

    public static int getTierSoldierHp(int tier) {
        if (tier < 1) return 100;
        if (tier >= TIER_SOLDIER_HP.length) return TIER_SOLDIER_HP[TIER_SOLDIER_HP.length - 1];
        return TIER_SOLDIER_HP[tier];
    }

    // ========== 兵种类型转换 ==========
    public static int parseTroopType(String type) {
        if (type == null) return 1;
        switch (type) {
            case "步": case "步兵": return 1;
            case "骑": case "骑兵": return 2;
            case "弓": case "弓兵": return 3;
            default: return 1;
        }
    }

    // ========== 兵法战斗计算 ==========

    /**
     * 带兵法的伤害计算。
     * 先应用被动兵法加成，再判定主动兵法发动，最后执行普通/特殊攻击。
     */
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

        // ===== 步兵被动兵法 - 在攻击前修改属性 =====
        if ("t_infantry_1".equals(tid)) {
            double bonus = attacker.tacticsEffectValue / 100.0;
            attacker.defense = (int)(attacker.defense * (1 + bonus));
        } else if ("t_infantry_2".equals(tid)) {
            double bonus = attacker.tacticsEffectValue / 100.0;
            attacker.dodge = (int) Math.min(50, attacker.dodge + attacker.dodge * bonus);
        } else if ("t_infantry_3".equals(tid)) {
            double cavReduce = attacker.tacticsEffectValue / 100.0;
            if (target.troopType == 2) {
                target.attack = (int)(target.attack * (1 - cavReduce));
            }
        }

        // ===== 弓兵被动: 落月弓 =====
        if ("t_archer_3".equals(tid)) {
            double bonus = attacker.tacticsEffectValue / 100.0;
            attacker.attack = (int)(attacker.attack * (1 + bonus));
            result.damages.add(calcDamage(attacker, target));
            return result;
        }

        // ===== 主动兵法发动判定（品质加成 + 名将特性翻倍） =====
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
                // 铁骑冲锋 - 额外伤害
                double bonus = 1 + attacker.tacticsEffectValue / 100.0;
                int origAtk = attacker.attack;
                attacker.attack = (int)(origAtk * bonus);
                result.damages.add(calcDamage(attacker, target));
                result.effectDesc = "铁骑冲锋！额外" + (int)attacker.tacticsEffectValue + "%伤害";
                attacker.attack = origAtk;
                break;
            }
            case "t_cavalry_2": {
                // 声东击西 - 攻击随机弓兵
                result.effectDesc = "声东击西！攻击敌方弓兵";
                List<BattleUnit> archers = new ArrayList<>();
                if (allEnemies != null) {
                    for (BattleUnit e : allEnemies) {
                        if (e.troopType == 3 && e.hp > 0) archers.add(e);
                    }
                }
                if (!archers.isEmpty()) {
                    BattleUnit archTarget = archers.get(random.nextInt(archers.size()));
                    result.damages.add(calcDamage(attacker, archTarget));
                    result.specialTarget = archTarget.name;
                } else {
                    result.damages.add(calcDamage(attacker, target));
                }
                break;
            }
            case "t_cavalry_3": {
                // 以逸待劳 - 伏击，额外伤害
                double ambushBonus = 1 + attacker.tacticsEffectValue / 100.0;
                int origAtkC3 = attacker.attack;
                attacker.attack = (int)(origAtkC3 * ambushBonus);
                result.damages.add(calcDamage(attacker, target));
                result.effectDesc = "以逸待劳！伏击额外" + (int)attacker.tacticsEffectValue + "%伤害";
                attacker.attack = origAtkC3;
                break;
            }
            case "t_cavalry_4": {
                // 战神突击 - 贯穿一行
                double pierceRatio = attacker.tacticsEffectValue / 100.0;
                result.effectDesc = "战神突击！贯穿攻击";
                if (allEnemies != null) {
                    for (BattleUnit e : allEnemies) {
                        if (e.hp > 0) {
                            int origAtkP = attacker.attack;
                            attacker.attack = (int)(origAtkP * pierceRatio);
                            result.damages.add(calcDamage(attacker, e));
                            attacker.attack = origAtkP;
                        }
                    }
                } else {
                    result.damages.add(calcDamage(attacker, target));
                }
                break;
            }
            case "t_archer_1": {
                // 连射 - 两次攻击
                result.effectDesc = "连射！发动两次攻击";
                result.damages.add(calcDamage(attacker, target));
                result.damages.add(calcDamage(attacker, target));
                break;
            }
            case "t_archer_2":
            case "t_special_lvbu": {
                // 长虹贯日 / 辕门射戟 - AOE
                double aoeRatio = attacker.tacticsEffectValue / 100.0;
                result.effectDesc = "t_special_lvbu".equals(tid) ? "辕门射戟！" : "长虹贯日！";
                if (allEnemies != null) {
                    for (BattleUnit e : allEnemies) {
                        if (e.hp > 0) {
                            int origAtk = attacker.attack;
                            attacker.attack = (int)(origAtk * aoeRatio);
                            result.damages.add(calcDamage(attacker, e));
                            attacker.attack = origAtk;
                        }
                    }
                } else {
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

    // ========== 数据类 ==========
    public static class BattleUnit {
        public int attack;
        public int defense;
        public int valor;      // 武勇(暴击率)
        public int command;    // 统御(伤害加成)
        public int dodge;      // 闪避率(%)
        public int mobility;   // 机动(行动顺序)
        public int troopType;  // 1步 2骑 3弓
        public int soldierCount;
        public double soldierHp;
        public double tierMultiplier;
        public int maxHp;
        public int hp;
        public String name;
        public int level;
        // 兵法相关
        public String tacticsId;
        public String tacticsName;
        public int tacticsLevel;
        public double tacticsTriggerRate;
        public double tacticsEffectValue;
        /** 武将品质带来的兵法发动概率加成(%) */
        public double tacticsTriggerBonus;
        /** 名将特性"兵法发动概率提升"的倍率，默认1，拥有该特性时为2（翻倍） */
        public double tacticsTriggerMultiplier = 1;

        public BattleUnit() {
            this.tierMultiplier = 1.0;
            this.soldierHp = 100;
        }
    }

    public static class DamageResult {
        public int damage;
        public boolean isCrit;
        public boolean isDodge;
        public int soldierLoss;

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
