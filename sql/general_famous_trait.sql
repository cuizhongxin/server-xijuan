-- =============================================
-- 名将特性重建：每个武将独立特性（完整 APK 数据）
-- =============================================
-- effect_type 说明：
--   soldier_damage    士兵/统领士卒伤害直加（不经攻防公式）
--   troop_damage      兵种限定伤害直加（需配合 troop_restrict）
--   army_attack       军队攻击（加到 totalAttack）
--   troop_attack      兵种限定攻击（需配合 troop_restrict）
--   army_defense      军队防御（加到 totalDefense）
--   troop_defense     兵种限定防御（需配合 troop_restrict）
--   damage_resist     伤害抵抗（减少受到的最终伤害）
--   soldier_life_pct  士兵生命百分比提升
--   army_mobility     军队机动
--   army_dodge        军队闪避
--   soldier_count     统领兵力上限
--   tactics_prob      兵法发动概率提升（全兵种）
--   troop_tactics     兵种限定兵法发动概率提升
--   immune_ambush     免疫偷袭类兵法（特殊标记）
-- =============================================

CREATE TABLE IF NOT EXISTS `general_famous_trait` (
  `id` INT AUTO_INCREMENT PRIMARY KEY,
  `general_pre_id` INT NOT NULL COMMENT '关联 general_pre.id (APK武将ID)',
  `trait_apk_id` VARCHAR(8) NOT NULL COMMENT 'APK特性ID (如4511)',
  `trait_name` VARCHAR(20) NOT NULL COMMENT '特性名称 (如战神)',
  `trait_desc` VARCHAR(100) NOT NULL COMMENT '特性描述 (如属下士兵伤害＋500)',
  `effect_type` VARCHAR(32) NOT NULL COMMENT '效果类型',
  `effect_value` INT NOT NULL DEFAULT 0 COMMENT '效果数值',
  `troop_restrict` TINYINT DEFAULT 0 COMMENT '兵种限制: 0=全, 1=步, 2=骑, 3=弓',
  `sort_order` INT DEFAULT 0 COMMENT '排序',
  INDEX idx_general_pre_id (`general_pre_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='名将特性表(APK FamousGenAttr)';

DELETE FROM `general_famous_trait`;

-- =============================================
-- 群 · 绿色（每人 1 特性）
-- =============================================
INSERT INTO general_famous_trait (general_pre_id, trait_apk_id, trait_name, trait_desc, effect_type, effect_value, troop_restrict) VALUES
(10201, '4211', '黑山骑兵',   '骑兵部队伤害＋80',              'troop_damage',    80, 2),
(10202, '4221', '强化攻击',   '士兵攻击＋160',                  'army_attack',    160, 0),
(10203, '4231', '骑兵突击',   '小幅提升骑兵兵法发动概率',       'troop_tactics',    0, 2),
(10204, '4241', '迅捷如风',   '士兵机动＋1',                    'army_mobility',    1, 0),
(10205, '4251', '善射之士',   '远程部队伤害＋85',              'troop_damage',    85, 3),
(10206, '4261', '射术精通',   '远程士兵攻击＋81',              'troop_attack',    81, 3),
(10207, '4271', '弓兵统领',   '小幅提升弓兵兵法发动概率',       'troop_tactics',    0, 3),
(10208, '4281', '快速反应',   '士兵机动＋1',                    'army_mobility',    1, 0),
(10209, '4291', '严防死守',   '军队防御＋85',                  'army_defense',    85, 0),
(10210, '4210', '秉性坚韧',   '士兵生命＋4%',                  'soldier_life_pct', 4, 0),
(10211, '4212', '用兵灵动',   '士兵闪避＋3',                   'army_dodge',       3, 0),
(10212, '4213', '统兵之术',   '统领兵力＋20',                  'soldier_count',   20, 0);

-- =============================================
-- 群 · 蓝色（每人 1 特性）
-- =============================================
INSERT INTO general_famous_trait (general_pre_id, trait_apk_id, trait_name, trait_desc, effect_type, effect_value, troop_restrict) VALUES
(10301, '4311', '饥渴大斧',   '属下军队士兵伤害＋150',         'soldier_damage', 150, 0),
(10304, '4321', '西凉铁骑',   '骑兵部队伤害＋160',             'troop_damage',   160, 2),
(10302, '4331', '狙杀射手',   '弓兵部队伤害＋150',             'troop_damage',   150, 3),
(10303, '4341', '弓兵之术',   '提升弓兵兵法发动概率',           'troop_tactics',    0, 3),
(10305, '4351', '坚韧持重',   '士兵生命＋7%',                  'soldier_life_pct', 7, 0),
(10306, '4361', '藤甲之术',   '士兵伤害抵抗＋180',             'damage_resist',  180, 0),
(10307, '4371', '太平遁术',   '军队闪避＋7',                   'army_dodge',       7, 0),
(10308, '4381', '蛮族之箭',   '弓兵部队伤害＋160',             'troop_damage',   160, 3);

-- =============================================
-- 群 · 紫色（每人 1 特性，公孙瓒无特性）
-- =============================================
INSERT INTO general_famous_trait (general_pre_id, trait_apk_id, trait_name, trait_desc, effect_type, effect_value, troop_restrict) VALUES
(10401, '4411', '虎牢扬威',   '统领士兵伤害＋330',             'soldier_damage', 330, 0),
(10402, '4421', '冠军之勇',   '骑兵部队攻击＋310',             'troop_attack',   310, 2),
(10403, '4431', '铁骑世家',   '提升骑兵兵法触发概率',           'troop_tactics',    0, 2),
(10404, '4441', '先登死士',   '射击伤害＋320',                 'troop_damage',   320, 3),
(10405, '4451', '陷阵之将',   '步兵部队防御＋350',             'troop_defense',  350, 1),
(10406, '4461', '庭柱之将',   '军队攻击抵抗＋330',             'damage_resist',  330, 0);
-- 公孙瓒(10407) 无基础特性

-- =============================================
-- 群 · 橙色 吕布（2 特性）
-- =============================================
INSERT INTO general_famous_trait (general_pre_id, trait_apk_id, trait_name, trait_desc, effect_type, effect_value, troop_restrict, sort_order) VALUES
(10501, '4511', '战神',       '属下士兵伤害＋500',             'soldier_damage', 500, 0, 1),
(10501, '4512', '赤兔飞将',   '骑兵兵法发动概率增加',           'troop_tactics',    0, 2, 2);

-- =============================================
-- 魏 · 蓝色（每人 1 特性）
-- =============================================
INSERT INTO general_famous_trait (general_pre_id, trait_apk_id, trait_name, trait_desc, effect_type, effect_value, troop_restrict) VALUES
(11301, '1311', '武勇过人',   '军队士兵伤害＋190',             'soldier_damage', 190, 0),
(11302, '1321', '抢占先机',   '部队机动性＋2',                 'army_mobility',    2, 0),
(11303, '1331', '弓马娴熟',   '弓兵部队伤害＋200',             'troop_damage',   200, 3),
(11304, '1341', '立志刚毅',   '步兵士兵防御＋200',             'troop_defense',  200, 1);

-- =============================================
-- 魏 · 紫色（每人 1 特性）
-- =============================================
INSERT INTO general_famous_trait (general_pre_id, trait_apk_id, trait_name, trait_desc, effect_type, effect_value, troop_restrict) VALUES
(11401, '1411', '虎豹骑兵',   '骑兵伤害＋320',                 'troop_damage',   320, 2),
(11402, '1421', '魏国先锋',   '军队机动性＋3',                 'army_mobility',    3, 0),
(11403, '1431', '西凉铁骑',   '骑兵部队攻击＋300',             'troop_attack',   300, 2),
(11404, '1441', '神鬼之勇',   '军队攻击＋310',                 'army_attack',    310, 0),
(11405, '1451', '百步穿杨',   '弓兵兵法发动概率提升',           'troop_tactics',    0, 3),
(11406, '1461', '悍勇不屈',   '士兵生命＋12%',                 'soldier_life_pct',12, 0),
(11407, '1471', '豪勇持重',   '军队防御＋350',                 'army_defense',   350, 0);

-- =============================================
-- 魏 · 橙色（每人 2 特性）
-- =============================================
INSERT INTO general_famous_trait (general_pre_id, trait_apk_id, trait_name, trait_desc, effect_type, effect_value, troop_restrict, sort_order) VALUES
(11501, '1511', '身先士卒',   '统领士卒伤害＋400',             'soldier_damage', 400, 0, 1),
(11501, '1512', '大将之才',   '统领的士兵人数额外＋100',       'soldier_count',  100, 0, 2),
(11502, '1521', '赤膊上阵',   '统领士兵伤害＋410',             'soldier_damage', 410, 0, 1),
(11502, '1522', '虎痴威名',   '骑兵兵法发动概率提升',           'troop_tactics',    0, 2, 2),
(11503, '1531', '迅猛先登',   '军队机动性＋4',                 'army_mobility',    4, 0, 1),
(11503, '1532', '并驾齐驱',   '骑兵兵法发动概率提升',           'troop_tactics',    0, 2, 2),
(11504, '1541', '统御弓兵',   '射击类兵法概率提升',             'troop_tactics',    0, 3, 1),
(11504, '1542', '齐射之阵',   '弓兵部队伤害＋390',             'troop_damage',   390, 3, 2),
(11507, '1551', '先发制人',   '军队机动性＋4',                 'army_mobility',    4, 0, 1),
(11507, '1552', '临敌巧变',   '兵法发动概率提升',               'tactics_prob',     0, 0, 2),
(11505, '1561', '统兵毅重',   '士兵生命值＋12%',               'soldier_life_pct',12, 0, 1),
(11505, '1562', '五子良将',   '统领的士兵人数额外＋100',       'soldier_count',  100, 0, 2),
(11506, '1571', '恶来铁卫',   '军队防御＋400',                 'army_defense',   400, 0, 1),
(11506, '1572', '折冲之军',   '军队闪避＋12',                  'army_dodge',      12, 0, 2);

-- =============================================
-- 蜀 · 蓝色（每人 1 特性）
-- =============================================
INSERT INTO general_famous_trait (general_pre_id, trait_apk_id, trait_name, trait_desc, effect_type, effect_value, troop_restrict) VALUES
(12301, '2311', '武勇过人',   '军队士兵伤害＋190',             'soldier_damage', 190, 0),
(12302, '2321', '抢占先机',   '部队机动性＋2',                 'army_mobility',    2, 0),
(12303, '2331', '弓马娴熟',   '弓兵部队伤害＋200',             'troop_damage',   200, 3),
(12304, '2341', '忠勇坚韧',   '步兵士兵防御＋200',             'troop_defense',  200, 1);

-- =============================================
-- 蜀 · 紫色（每人 1 特性）
-- =============================================
INSERT INTO general_famous_trait (general_pre_id, trait_apk_id, trait_name, trait_desc, effect_type, effect_value, troop_restrict) VALUES
(12401, '2411', '武圣遗风',   '士兵伤害＋320',                 'soldier_damage', 320, 0),
(12402, '2421', '蜀国先锋',   '军队机动性＋3',                 'army_mobility',    3, 0),
(12403, '2431', '亢维之锐',   '骑兵部队攻击＋300',             'troop_attack',   300, 2),
(12404, '2441', '豪勇奋发',   '军队攻击＋310',                 'army_attack',    310, 0),
(12405, '2451', '射雕英雄',   '弓兵兵法发动概率提升',           'troop_tactics',    0, 3),
(12406, '2461', '悍勇不屈',   '士兵生命＋12%',                 'soldier_life_pct',12, 0),
(12407, '2471', '行阵和睦',   '军队防御＋350',                 'army_defense',   350, 0);

-- =============================================
-- 蜀 · 橙色（每人 2 特性）
-- =============================================
INSERT INTO general_famous_trait (general_pre_id, trait_apk_id, trait_name, trait_desc, effect_type, effect_value, troop_restrict, sort_order) VALUES
(12501, '2511', '武圣',       '统领士卒伤害＋400',             'soldier_damage', 400, 0, 1),
(12501, '2512', '五虎上将',   '统领的士兵人数额外＋100',       'soldier_count',  100, 0, 2),
(12502, '2521', '龙胆英雄',   '统领士兵伤害＋410',             'soldier_damage', 410, 0, 1),
(12502, '2522', '白马义从',   '骑兵兵法发动概率提升',           'troop_tactics',    0, 2, 2),
(12503, '2531', '雄烈奋进',   '军队机动性＋4',                 'army_mobility',    4, 0, 1),
(12503, '2532', '铁骑世家',   '骑兵兵法发动概率提升',           'troop_tactics',    0, 2, 2),
(12504, '2541', '神射手',     '射击类兵法概率提升',             'troop_tactics',    0, 3, 1),
(12504, '2542', '老当益壮',   '弓兵部队伤害＋390',             'troop_damage',   390, 3, 2),
(12507, '2551', '料敌机先',   '军队机动性＋4',                 'army_mobility',    4, 0, 1),
(12507, '2552', '兵法大家',   '兵法发动概率提升',               'tactics_prob',     0, 0, 2),
(12505, '2561', '刚猛不屈',   '士兵生命值＋12%',               'soldier_life_pct',12, 0, 1),
(12505, '2562', '五虎上将',   '统领的士兵人数额外＋100',       'soldier_count',  100, 0, 2),
(12506, '2571', '勇烈刚毅',   '军队防御＋400',                 'army_defense',   400, 0, 1),
(12506, '2572', '折冲外御',   '军队闪避＋12',                  'army_dodge',      12, 0, 2);

-- =============================================
-- 吴 · 蓝色（每人 1 特性）
-- =============================================
INSERT INTO general_famous_trait (general_pre_id, trait_apk_id, trait_name, trait_desc, effect_type, effect_value, troop_restrict) VALUES
(13301, '3311', '武勇过人',   '军队士兵伤害＋190',             'soldier_damage', 190, 0),
(13302, '3321', '抢占先机',   '部队机动性＋2',                 'army_mobility',    2, 0),
(13303, '3331', '弓马娴熟',   '弓兵部队伤害＋200',             'troop_damage',   200, 3),
(13304, '3341', '胆守无惧',   '步兵士兵防御＋200',             'troop_defense',  200, 1);

-- =============================================
-- 吴 · 紫色（每人 1 特性）
-- =============================================
INSERT INTO general_famous_trait (general_pre_id, trait_apk_id, trait_name, trait_desc, effect_type, effect_value, troop_restrict) VALUES
(13401, '3411', '雪奋短兵',   '骑兵伤害＋320',                 'troop_damage',   320, 2),
(13402, '3421', '吴国先锋',   '军队机动性＋3',                 'army_mobility',    3, 0),
(13403, '3431', '勇烈奋威',   '骑兵部队攻击＋300',             'troop_attack',   300, 2),
(13404, '3441', '骁果麤猛',   '军队攻击＋310',                 'army_attack',    310, 0),
(13405, '3451', '连珠神箭',   '弓兵兵法发动概率提升',           'troop_tactics',    0, 3),
(13406, '3461', '苦肉计',     '士兵生命＋12%',                 'soldier_life_pct',12, 0),
(13407, '3471', '豪勇持重',   '军队防御＋350',                 'army_defense',   350, 0);

-- =============================================
-- 吴 · 橙色（每人 2 特性）
-- =============================================
INSERT INTO general_famous_trait (general_pre_id, trait_apk_id, trait_name, trait_desc, effect_type, effect_value, troop_restrict, sort_order) VALUES
(13501, '3511', '旋略勇进',   '统领士卒伤害＋400',             'soldier_damage', 400, 0, 1),
(13501, '3512', '大将之才',   '统领的士兵人数额外＋100',       'soldier_count',  100, 0, 2),
(13502, '3521', '气勇胆烈',   '统领士兵伤害＋410',             'soldier_damage', 410, 0, 1),
(13502, '3522', '狼骑悍将',   '骑兵兵法发动概率提升',           'troop_tactics',    0, 2, 2),
(13503, '3531', '见状明判',   '军队机动性＋4',                 'army_mobility',    4, 0, 1),
(13503, '3532', '熟读兵书',   '兵法发动概率提升',               'tactics_prob',     0, 0, 2),
(13504, '3541', '审时度势',   '善于捕捉战机，提升兵法发动概率', 'tactics_prob',     0, 0, 1),
(13504, '3542', '火烧连营',   '弓兵部队伤害＋390',             'troop_damage',   390, 3, 2),
(13507, '3551', '轻舟快马',   '军队机动性＋4',                 'army_mobility',    4, 0, 1),
(13507, '3552', '百步穿杨',   '兵法发动概率提升',               'tactics_prob',     0, 0, 2),
(13505, '3561', '浴血奋战',   '士兵生命值＋12%',               'soldier_life_pct',12, 0, 1),
(13505, '3562', '治军有道',   '统领的士兵人数额外＋100',       'soldier_count',  100, 0, 2),
(13506, '3571', '豪勇持重',   '军队防御＋400',                 'army_defense',   400, 0, 1),
(13506, '3572', '折冲外御',   '军队闪避＋12',                  'army_dodge',      12, 0, 2);

-- =============================================
-- 特殊 · 貂蝉（3 特性）
-- =============================================
INSERT INTO general_famous_trait (general_pre_id, trait_apk_id, trait_name, trait_desc, effect_type, effect_value, troop_restrict, sort_order) VALUES
(20501, '5001', '神魂颠倒',   '干扰敌军士气，免疫偷袭类兵法',  'immune_ambush',    1, 0, 1),
(20501, '5002', '鼓舞士气',   '激励士兵奋勇争先，军队机动性＋3','army_mobility',    3, 0, 2),
(20501, '5003', '连环计',     '提升兵法发动概率',               'tactics_prob',     0, 0, 3);

-- =============================================
-- 进阶特性（狂化后解锁，关联狂化版武将 general_pre_id）
-- =============================================
INSERT INTO general_famous_trait (general_pre_id, trait_apk_id, trait_name, trait_desc, effect_type, effect_value, troop_restrict, sort_order) VALUES
-- 吕布(狂) 30501 继承原版 + 进阶
(30501, '4511', '战神',       '属下士兵伤害＋500',             'soldier_damage', 500, 0, 1),
(30501, '4512', '赤兔飞将',   '骑兵兵法发动概率增加',           'troop_tactics',    0, 2, 2),
(30501, '6051', '急速狂攻',   '机动性＋4',                     'army_mobility',    4, 0, 3),
(30501, '6052', '铁骑纵横',   '骑兵战法发动概率提升',           'troop_tactics',    0, 2, 4),
-- 张辽(狂) 31501
(31501, '1511', '身先士卒',   '统领士卒伤害＋400',             'soldier_damage', 400, 0, 1),
(31501, '1512', '大将之才',   '统领的士兵人数额外＋100',       'soldier_count',  100, 0, 2),
(31501, '6052', '铁骑纵横',   '骑兵战法发动概率提升',           'troop_tactics',    0, 2, 3),
-- 关羽(狂) 32501
(32501, '2511', '武圣',       '统领士卒伤害＋400',             'soldier_damage', 400, 0, 1),
(32501, '2512', '五虎上将',   '统领的士兵人数额外＋100',       'soldier_count',  100, 0, 2),
(32501, '6052', '铁骑纵横',   '骑兵战法发动概率提升',           'troop_tactics',    0, 2, 3),
-- 凌统(狂) 33501
(33501, '3511', '旋略勇进',   '统领士卒伤害＋400',             'soldier_damage', 400, 0, 1),
(33501, '3512', '大将之才',   '统领的士兵人数额外＋100',       'soldier_count',  100, 0, 2),
(33501, '6052', '铁骑纵横',   '骑兵战法发动概率提升',           'troop_tactics',    0, 2, 3),
-- 华雄(狂) 30401
(30401, '4411', '虎牢扬威',   '统领士兵伤害＋330',             'soldier_damage', 330, 0, 1),
(30401, '6041', '统领进阶',   '军队满编人数＋60',              'soldier_count',   60, 0, 2),
(30401, '6141', '狂暴攻击',   '军队攻击力＋310',               'army_attack',    310, 0, 3),
-- 高顺(狂) 30405
(30405, '4451', '陷阵之将',   '步兵部队防御＋350',             'troop_defense',  350, 1, 1),
(30405, '6041', '统领进阶',   '军队满编人数＋60',              'soldier_count',   60, 0, 2),
(30405, '6141', '狂暴攻击',   '军队攻击力＋310',               'army_attack',    310, 0, 3),
-- 公孙瓒(狂) 30407
(30407, '6041', '统领进阶',   '军队满编人数＋60',              'soldier_count',   60, 0, 1),
(30407, '6141', '狂暴攻击',   '军队攻击力＋310',               'army_attack',    310, 0, 2),
-- 夏侯渊(狂) 31405
(31405, '1451', '百步穿杨',   '弓兵兵法发动概率提升',           'troop_tactics',    0, 3, 1),
(31405, '6041', '统领进阶',   '军队满编人数＋60',              'soldier_count',   60, 0, 2),
(31405, '6141', '狂暴攻击',   '军队攻击力＋310',               'army_attack',    310, 0, 3),
-- 曹仁(狂) 31401
(31401, '1411', '虎豹骑兵',   '骑兵伤害＋320',                 'troop_damage',   320, 2, 1),
(31401, '6041', '统领进阶',   '军队满编人数＋60',              'soldier_count',   60, 0, 2),
(31401, '6141', '狂暴攻击',   '军队攻击力＋310',               'army_attack',    310, 0, 3),
-- 关平(狂) 32401
(32401, '2411', '武圣遗风',   '士兵伤害＋320',                 'soldier_damage', 320, 0, 1),
(32401, '6041', '统领进阶',   '军队满编人数＋60',              'soldier_count',   60, 0, 2),
(32401, '6141', '狂暴攻击',   '军队攻击力＋310',               'army_attack',    310, 0, 3),
-- 关兴(狂) 32405
(32405, '2451', '射雕英雄',   '弓兵兵法发动概率提升',           'troop_tactics',    0, 3, 1),
(32405, '6041', '统领进阶',   '军队满编人数＋60',              'soldier_count',   60, 0, 2),
(32405, '6141', '狂暴攻击',   '军队攻击力＋310',               'army_attack',    310, 0, 3),
-- 丁奉(狂) 33401
(33401, '3411', '雪奋短兵',   '骑兵伤害＋320',                 'troop_damage',   320, 2, 1),
(33401, '6041', '统领进阶',   '军队满编人数＋60',              'soldier_count',   60, 0, 2),
(33401, '6141', '狂暴攻击',   '军队攻击力＋310',               'army_attack',    310, 0, 3),
-- 程普(狂) 33405
(33405, '3451', '连珠神箭',   '弓兵兵法发动概率提升',           'troop_tactics',    0, 3, 1),
(33405, '6041', '统领进阶',   '军队满编人数＋60',              'soldier_count',   60, 0, 2),
(33405, '6141', '狂暴攻击',   '军队攻击力＋310',               'army_attack',    310, 0, 3);
