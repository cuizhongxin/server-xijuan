-- =============================================
-- 修复 general_slot 表：按 APK 数据全量更新六维属性
--
-- 成长公式 (GeneralService.java 写死):
--   attr = base + base × qualityGrowthRate × (level - 1)
--   orange=0.06, purple=0.05, red=0.045, blue=0.04, green=0.035, white=0.03
--   DB 中 growth_attack/defense/valor/command 列未被使用
--
-- 战斗公式 (BattleCalculator.java):
--   valorBonus = 1 + valor/400        (武勇越高伤害越大)
--   commandReduction = cmd/(cmd+400)   (统御越高受伤越少)
--
-- 武勇/统御取值原则: Lv50 橙色最高约 25-30% 加伤 / 18-22% 减伤
-- =============================================

-- ==========================================
-- 橙色 (orange) id 1-9  —— 逐条更新
-- ==========================================

-- id=1 步兵统帅 (如赵云): 攻守均衡
UPDATE general_slot SET
  base_attack = 160, base_defense = 150, base_valor = 26, base_command = 24,
  base_dodge = 8, base_mobility = 25, base_power = 1000, base_tactics_trigger_bonus = 15
WHERE id = 1;

-- id=2 步兵猛将 (如典韦): 高攻高武勇
UPDATE general_slot SET
  base_attack = 175, base_defense = 130, base_valor = 28, base_command = 18,
  base_dodge = 5, base_mobility = 22, base_power = 1050, base_tactics_trigger_bonus = 12
WHERE id = 2;

-- id=3 骑兵猛将 (如吕布): 最高攻最高武勇
UPDATE general_slot SET
  base_attack = 180, base_defense = 120, base_valor = 30, base_command = 15,
  base_dodge = 6, base_mobility = 30, base_power = 1050, base_tactics_trigger_bonus = 18
WHERE id = 3;

-- id=4 骑兵猛将 (如马超): 略低于吕布
UPDATE general_slot SET
  base_attack = 185, base_defense = 118, base_valor = 29, base_command = 16,
  base_dodge = 5, base_mobility = 29, base_power = 1050, base_tactics_trigger_bonus = 16
WHERE id = 4;

-- id=5 弓兵智将 (如诸葛亮): 高统御高兵法触发
UPDATE general_slot SET
  base_attack = 155, base_defense = 125, base_valor = 18, base_command = 28,
  base_dodge = 9, base_mobility = 20, base_power = 980, base_tactics_trigger_bonus = 18
WHERE id = 5;

-- id=6 弓兵智将 (如庞统): 略偏防
UPDATE general_slot SET
  base_attack = 150, base_defense = 130, base_valor = 19, base_command = 26,
  base_dodge = 8, base_mobility = 21, base_power = 980, base_tactics_trigger_bonus = 15
WHERE id = 6;

-- id=7 骑兵统帅 (如关羽): 高统御高机动
UPDATE general_slot SET
  base_attack = 165, base_defense = 140, base_valor = 24, base_command = 26,
  base_dodge = 7, base_mobility = 28, base_power = 1000, base_tactics_trigger_bonus = 15
WHERE id = 7;

-- id=8 骑兵猛将 (如张飞): 攻高防也不低
UPDATE general_slot SET
  base_attack = 190, base_defense = 122, base_valor = 28, base_command = 17,
  base_dodge = 6, base_mobility = 28, base_power = 1000, base_tactics_trigger_bonus = 15
WHERE id = 8;

-- id=9 弓兵猛将 (如黄忠): 弓中偏攻型
UPDATE general_slot SET
  base_attack = 168, base_defense = 130, base_valor = 24, base_command = 20,
  base_dodge = 6, base_mobility = 22, base_power = 980, base_tactics_trigger_bonus = 12
WHERE id = 9;


-- ==========================================
-- 紫色 (purple) id 10-71  —— 按兵种+类型批量
-- ==========================================

-- 紫色 步兵 猛将
UPDATE general_slot SET
  base_attack = 140, base_defense = 115, base_valor = 22, base_command = 15,
  base_dodge = 5, base_mobility = 20, base_tactics_trigger_bonus = 8
WHERE quality_code = 'purple' AND troop_type = '步' AND type = '猛将';

-- 紫色 步兵 智将
UPDATE general_slot SET
  base_attack = 125, base_defense = 120, base_valor = 18, base_command = 20,
  base_dodge = 7, base_mobility = 19, base_tactics_trigger_bonus = 10
WHERE quality_code = 'purple' AND troop_type = '步' AND type = '智将';

-- 紫色 步兵 统帅
UPDATE general_slot SET
  base_attack = 130, base_defense = 125, base_valor = 20, base_command = 19,
  base_dodge = 7, base_mobility = 22, base_tactics_trigger_bonus = 10
WHERE quality_code = 'purple' AND troop_type = '步' AND type = '统帅';

-- 紫色 步兵 普通
UPDATE general_slot SET
  base_attack = 122, base_defense = 118, base_valor = 16, base_command = 16,
  base_dodge = 6, base_mobility = 18, base_tactics_trigger_bonus = 6
WHERE quality_code = 'purple' AND troop_type = '步' AND type = '普通';

-- 紫色 骑兵 猛将
UPDATE general_slot SET
  base_attack = 150, base_defense = 110, base_valor = 24, base_command = 14,
  base_dodge = 5, base_mobility = 28, base_tactics_trigger_bonus = 8
WHERE quality_code = 'purple' AND troop_type = '骑' AND type = '猛将';

-- 紫色 骑兵 智将
UPDATE general_slot SET
  base_attack = 138, base_defense = 115, base_valor = 19, base_command = 18,
  base_dodge = 6, base_mobility = 24, base_tactics_trigger_bonus = 10
WHERE quality_code = 'purple' AND troop_type = '骑' AND type = '智将';

-- 紫色 骑兵 统帅
UPDATE general_slot SET
  base_attack = 145, base_defense = 120, base_valor = 21, base_command = 18,
  base_dodge = 6, base_mobility = 26, base_tactics_trigger_bonus = 10
WHERE quality_code = 'purple' AND troop_type = '骑' AND type = '统帅';

-- 紫色 骑兵 普通
UPDATE general_slot SET
  base_attack = 135, base_defense = 112, base_valor = 17, base_command = 15,
  base_dodge = 5, base_mobility = 24, base_tactics_trigger_bonus = 6
WHERE quality_code = 'purple' AND troop_type = '骑' AND type = '普通';

-- 紫色 弓兵 猛将
UPDATE general_slot SET
  base_attack = 135, base_defense = 100, base_valor = 20, base_command = 14,
  base_dodge = 5, base_mobility = 20, base_tactics_trigger_bonus = 8
WHERE quality_code = 'purple' AND troop_type = '弓' AND type = '猛将';

-- 紫色 弓兵 智将
UPDATE general_slot SET
  base_attack = 125, base_defense = 110, base_valor = 15, base_command = 21,
  base_dodge = 8, base_mobility = 18, base_tactics_trigger_bonus = 10
WHERE quality_code = 'purple' AND troop_type = '弓' AND type = '智将';

-- 紫色 弓兵 普通
UPDATE general_slot SET
  base_attack = 120, base_defense = 108, base_valor = 14, base_command = 17,
  base_dodge = 6, base_mobility = 17, base_tactics_trigger_bonus = 6
WHERE quality_code = 'purple' AND troop_type = '弓' AND type = '普通';


-- ==========================================
-- 红色 (red) id 72-86  —— 全部为智将
-- ==========================================

UPDATE general_slot SET
  base_attack = 108, base_defense = 100, base_valor = 15, base_command = 16,
  base_dodge = 6, base_mobility = 18, base_tactics_trigger_bonus = 5
WHERE quality_code = 'red' AND troop_type = '步';

UPDATE general_slot SET
  base_attack = 115, base_defense = 95, base_valor = 16, base_command = 15,
  base_dodge = 5, base_mobility = 22, base_tactics_trigger_bonus = 5
WHERE quality_code = 'red' AND troop_type = '骑';

UPDATE general_slot SET
  base_attack = 105, base_defense = 95, base_valor = 12, base_command = 18,
  base_dodge = 7, base_mobility = 16, base_tactics_trigger_bonus = 5
WHERE quality_code = 'red' AND troop_type = '弓';


-- ==========================================
-- 蓝色 (blue) id 87-100, 121
-- ==========================================

UPDATE general_slot SET
  base_attack = 78, base_defense = 75, base_valor = 10, base_command = 12,
  base_dodge = 5, base_mobility = 17, base_tactics_trigger_bonus = 0
WHERE quality_code = 'blue' AND troop_type = '步' AND type = '智将';

UPDATE general_slot SET
  base_attack = 88, base_defense = 70, base_valor = 13, base_command = 9,
  base_dodge = 3, base_mobility = 16, base_tactics_trigger_bonus = 0
WHERE quality_code = 'blue' AND troop_type = '步' AND type = '猛将';

UPDATE general_slot SET
  base_attack = 82, base_defense = 72, base_valor = 11, base_command = 11,
  base_dodge = 4, base_mobility = 20, base_tactics_trigger_bonus = 0
WHERE quality_code = 'blue' AND troop_type = '骑';

UPDATE general_slot SET
  base_attack = 75, base_defense = 72, base_valor = 9, base_command = 13,
  base_dodge = 6, base_mobility = 14, base_tactics_trigger_bonus = 0
WHERE quality_code = 'blue' AND troop_type = '弓';


-- ==========================================
-- 绿色 (green) id 101-110
-- ==========================================

UPDATE general_slot SET
  base_attack = 52, base_defense = 52, base_valor = 7, base_command = 8,
  base_dodge = 4, base_mobility = 14, base_tactics_trigger_bonus = 0
WHERE quality_code = 'green' AND troop_type = '步';

UPDATE general_slot SET
  base_attack = 55, base_defense = 50, base_valor = 8, base_command = 7,
  base_dodge = 3, base_mobility = 18, base_tactics_trigger_bonus = 0
WHERE quality_code = 'green' AND troop_type = '骑';

UPDATE general_slot SET
  base_attack = 50, base_defense = 50, base_valor = 6, base_command = 9,
  base_dodge = 5, base_mobility = 12, base_tactics_trigger_bonus = 0
WHERE quality_code = 'green' AND troop_type = '弓';


-- ==========================================
-- 白色 (white) id 111-120
-- ==========================================

UPDATE general_slot SET
  base_attack = 36, base_defense = 36, base_valor = 5, base_command = 5,
  base_dodge = 3, base_mobility = 11, base_tactics_trigger_bonus = 0
WHERE quality_code = 'white' AND troop_type = '步';

UPDATE general_slot SET
  base_attack = 38, base_defense = 33, base_valor = 5, base_command = 5,
  base_dodge = 2, base_mobility = 15, base_tactics_trigger_bonus = 0
WHERE quality_code = 'white' AND troop_type = '骑';

UPDATE general_slot SET
  base_attack = 35, base_defense = 35, base_valor = 4, base_command = 6,
  base_dodge = 4, base_mobility = 10, base_tactics_trigger_bonus = 0
WHERE quality_code = 'white' AND troop_type = '弓';


-- ==========================================
-- 修复已有梁婉记录：slotId 改为 121 (blue步猛将)
-- 品质改为蓝色，属性按 slot 121 Lv1 计算
-- blue growthRate=0.04, slot121: atk=88, def=70, val=13, cmd=9
-- ==========================================
UPDATE general SET
  slot_id = 121,
  quality_id = 3, quality_name = '蓝色', quality_color = '#4169E1',
  quality_base_multiplier = 1.0, quality_star = 3,
  attr_attack = 88, attr_defense = 70, attr_valor = 13, attr_command = 9,
  attr_dodge = 3, attr_mobility = 16
WHERE name = '梁婉';

-- 修复已有张飞记录 (slotId 7→10)，需按等级重算属性
-- purple growthRate=0.05, slot10 base: atk=140 def=115 val=22 cmd=15
-- Lv46: atk = 140 + 140*0.05*45 = 455, def = 115 + 115*0.05*45 = 373
UPDATE general SET
  slot_id = 10,
  attr_attack = 455, attr_defense = 373, attr_valor = 71, attr_command = 49,
  attr_dodge = 16, attr_mobility = 65
WHERE name = '张飞' AND slot_id = 7;

-- 修复已有关羽记录 (slotId 9→13)
-- purple 骑统帅 slot13 base: atk=145 def=120 val=21 cmd=18
-- Lv48: atk = 145 + 145*0.05*47 = 486, def = 120 + 120*0.05*47 = 402
UPDATE general SET
  slot_id = 13,
  attr_attack = 486, attr_defense = 402, attr_valor = 70, attr_command = 60,
  attr_dodge = 20, attr_mobility = 87
WHERE name = '关羽' AND slot_id = 9;

-- 修复已有貂蝉记录 (slotId 17→76)
-- red 弓智将 slot76 base: atk=105 def=95 val=12 cmd=18
-- Lv43: atk = 105 + 105*0.045*42 = 303, def = 95 + 95*0.045*42 = 274
UPDATE general SET
  slot_id = 76,
  attr_attack = 303, attr_defense = 274, attr_valor = 35, attr_command = 52,
  attr_dodge = 20, attr_mobility = 46
WHERE name = '貂蝉' AND slot_id = 17;


-- =============================================
-- 各品质 Lv1 / Lv50 武勇统御效果预览
-- =============================================
-- 品质    | 代表       | Lv1 val | Lv50 val | Lv50加伤 | Lv1 cmd | Lv50 cmd | Lv50减伤
-- orange  | 骑猛(吕布) | 30      | 118      | +29.5%   | 15      | 59       | 12.8%
-- orange  | 弓智(诸葛) | 18      | 71       | +17.8%   | 28      | 110      | 21.6%
-- purple  | 骑猛       | 24      | 83       | +20.7%   | 14      | 48       | 10.8%
-- purple  | 弓智       | 15      | 52       | +13.0%   | 21      | 72       | 15.3%
-- red     | 步智       | 15      | 48       | +12.0%   | 16      | 51       | 11.3%
-- blue    | 步智       | 10      | 30       | +7.5%    | 12      | 36       | 8.3%
-- green   | 步智       | 7       | 19       | +4.8%    | 8       | 22       | 5.2%
-- white   | 步智       | 5       | 12       | +3.0%    | 5       | 12       | 2.9%
