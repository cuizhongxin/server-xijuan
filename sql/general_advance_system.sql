-- =============================================
-- 狂化武将(进阶)数据支持
--
-- 设计:
--   1. general_template 加 growth_attack_bonus / growth_defense_bonus
--      普通武将 = 0, 狂化武将 = 1 (每级多+1攻/防)
--   2. 新增 13 个狂化 general_template (ID 900-912)
--      与原武将共享 slot_id (base stats一致)
--   3. 狂化 general_famous_trait = 原特性 + 进阶特性
--   4. 进阶时: general.templateId 切换到狂化模板
--
-- APK 来源: GenAdvShow_cfg.json / FamousGenAttr.json
-- =============================================

-- =============================================
-- 1. 给 general_template 新增成长加成列
-- =============================================
ALTER TABLE `general_template`
  ADD COLUMN `growth_attack_bonus` INT NOT NULL DEFAULT 0 COMMENT '攻击成长加成(进阶+1)',
  ADD COLUMN `growth_defense_bonus` INT NOT NULL DEFAULT 0 COMMENT '防御成长加成(进阶+1)';


-- =============================================
-- 2. 新增 13 个狂化武将模板
-- =============================================
-- 狂化武将与原武将共享 slot_id，但有 growth_bonus = 1
--
-- 映射关系:
--   900: 华雄(狂)   ← 121 华雄   slot17  群
--   901: 高顺(狂)   ← 125 高顺   slot21  群
--   902: 公孙瓒(狂) ← 127 公孙瓒 slot23  群
--   903: 吕布(狂)   ← 22  吕布   slot8   群
--   904: 曹仁(狂)   ← 100 曹仁   slot10  魏
--   905: 夏侯渊(狂) ← 104 夏侯渊 slot14  魏
--   906: 张辽(狂)   ← 1   张辽   slot1   魏
--   907: 关平(狂)   ← 107 关平   slot10  蜀
--   908: 关兴(狂)   ← 111 关兴   slot14  蜀
--   909: 关羽(狂)   ← 8   关羽   slot1   蜀
--   910: 丁奉(狂)   ← 114 丁奉   slot10  吴
--   911: 程普(狂)   ← 118 程普   slot14  吴
--   912: 凌统(狂)   ← 15  凌统   slot1   吴

INSERT INTO `general_template` (`id`,`name`,`faction`,`slot_id`,`sort_order`,`avatar`,`growth_attack_bonus`,`growth_defense_bonus`) VALUES
-- 群
(900,'华雄(狂)',  '群',17, 200,'images/general/4041.jpg', 1,1),
(901,'高顺(狂)',  '群',21, 201,'images/general/4045.jpg', 1,1),
(902,'公孙瓒(狂)','群',23, 202,'images/general/4047.jpg', 1,1),
(903,'吕布(狂)',  '群', 8, 203,'images/general/4051.jpg', 1,1),
-- 魏
(904,'曹仁(狂)',  '魏',10, 204,'images/general/4141.jpg', 1,1),
(905,'夏侯渊(狂)','魏',14, 205,'images/general/4145.jpg', 1,1),
(906,'张辽(狂)',  '魏', 1, 206,'images/general/4151.jpg', 1,1),
-- 蜀
(907,'关平(狂)',  '蜀',10, 207,'images/general/4241.jpg', 1,1),
(908,'关兴(狂)',  '蜀',14, 208,'images/general/4245.jpg', 1,1),
(909,'关羽(狂)',  '蜀', 1, 209,'images/general/4251.jpg', 1,1),
-- 吴
(910,'丁奉(狂)',  '吴',10, 210,'images/general/4341.jpg', 1,1),
(911,'程普(狂)',  '吴',14, 211,'images/general/4345.jpg', 1,1),
(912,'凌统(狂)',  '吴', 1, 212,'images/general/4351.jpg', 1,1);


-- =============================================
-- 3. 狂化武将名将特性 = 原特性 + 进阶特性
-- =============================================
-- 进阶特性(APK FamousGenAttr 6xxx系列):
--   统领进阶(6041): soldier_count +60
--   狂暴攻击(6141): army_attack +310
--   急速狂攻(6051): army_mobility +4
--   铁骑纵横(6052): troop_tactics +0 (骑兵兵法发动概率提升)

-- --- 900 华雄(狂) ← 121华雄: soldier_damage 330 + 统领进阶 ---
INSERT INTO general_famous_trait (general_template_id, trait_name, trait_desc, effect_type, effect_value, troop_restrict, sort_order) VALUES
(900,'虎牢扬威','统领士兵伤害＋330','soldier_damage',330,0,1),
(900,'统领进阶','军队满编人数＋60','soldier_count',60,0,2);

-- --- 901 高顺(狂) ← 125高顺: troop_defense 350 + 统领进阶 ---
INSERT INTO general_famous_trait (general_template_id, trait_name, trait_desc, effect_type, effect_value, troop_restrict, sort_order) VALUES
(901,'陷阵之将','步兵部队防御＋350','troop_defense',350,1,1),
(901,'统领进阶','军队满编人数＋60','soldier_count',60,0,2);

-- --- 902 公孙瓒(狂) ← 127公孙瓒: troop_tactics + 统领进阶 ---
INSERT INTO general_famous_trait (general_template_id, trait_name, trait_desc, effect_type, effect_value, troop_restrict, sort_order) VALUES
(902,'白马义从','骑兵兵法发动概率提升','troop_tactics',0,2,1),
(902,'统领进阶','军队满编人数＋60','soldier_count',60,0,2);

-- --- 903 吕布(狂) ← 22吕布: damage500 + 骑tactics + 急速狂攻 ---
INSERT INTO general_famous_trait (general_template_id, trait_name, trait_desc, effect_type, effect_value, troop_restrict, sort_order) VALUES
(903,'战    神','属下士兵伤害＋500','soldier_damage',500,0,1),
(903,'赤兔飞将','骑兵兵法发动概率增加','troop_tactics',0,2,2),
(903,'急速狂攻','军队机动性＋4','army_mobility',4,0,3);

-- --- 904 曹仁(狂) ← 100曹仁: troop_damage 320 + 狂暴攻击 ---
INSERT INTO general_famous_trait (general_template_id, trait_name, trait_desc, effect_type, effect_value, troop_restrict, sort_order) VALUES
(904,'虎豹骑兵','骑兵伤害＋320','troop_damage',320,2,1),
(904,'狂暴攻击','军队攻击力＋310','army_attack',310,0,2);

-- --- 905 夏侯渊(狂) ← 104夏侯渊: troop_tactics(弓) + 狂暴攻击 ---
INSERT INTO general_famous_trait (general_template_id, trait_name, trait_desc, effect_type, effect_value, troop_restrict, sort_order) VALUES
(905,'百步穿杨','弓兵兵法发动概率提升','troop_tactics',0,3,1),
(905,'狂暴攻击','军队攻击力＋310','army_attack',310,0,2);

-- --- 906 张辽(狂) ← 1张辽: damage400 + count100 + 铁骑纵横 ---
INSERT INTO general_famous_trait (general_template_id, trait_name, trait_desc, effect_type, effect_value, troop_restrict, sort_order) VALUES
(906,'身先士卒','统领士卒伤害＋400','soldier_damage',400,0,1),
(906,'大将之才','统领的士兵人数额外＋100','soldier_count',100,0,2),
(906,'铁骑纵横','骑兵战法发动概率提升','troop_tactics',0,2,3);

-- --- 907 关平(狂) ← 107关平: soldier_damage 320 + 狂暴攻击 ---
INSERT INTO general_famous_trait (general_template_id, trait_name, trait_desc, effect_type, effect_value, troop_restrict, sort_order) VALUES
(907,'武圣遗风','士兵伤害＋320','soldier_damage',320,0,1),
(907,'狂暴攻击','军队攻击力＋310','army_attack',310,0,2);

-- --- 908 关兴(狂) ← 111关兴: troop_tactics(弓) + 狂暴攻击 ---
INSERT INTO general_famous_trait (general_template_id, trait_name, trait_desc, effect_type, effect_value, troop_restrict, sort_order) VALUES
(908,'射雕英雄','弓兵兵法发动概率提升','troop_tactics',0,3,1),
(908,'狂暴攻击','军队攻击力＋310','army_attack',310,0,2);

-- --- 909 关羽(狂) ← 8关羽: damage400 + count100 + 铁骑纵横 ---
INSERT INTO general_famous_trait (general_template_id, trait_name, trait_desc, effect_type, effect_value, troop_restrict, sort_order) VALUES
(909,'武    圣','统领士卒伤害＋400','soldier_damage',400,0,1),
(909,'五虎上将','统领的士兵人数额外＋100','soldier_count',100,0,2),
(909,'铁骑纵横','骑兵战法发动概率提升','troop_tactics',0,2,3);

-- --- 910 丁奉(狂) ← 114丁奉: troop_damage 320 + 狂暴攻击 ---
INSERT INTO general_famous_trait (general_template_id, trait_name, trait_desc, effect_type, effect_value, troop_restrict, sort_order) VALUES
(910,'雪奋短兵','骑兵伤害＋320','troop_damage',320,2,1),
(910,'狂暴攻击','军队攻击力＋310','army_attack',310,0,2);

-- --- 911 程普(狂) ← 118程普: troop_tactics(弓) + 狂暴攻击 ---
INSERT INTO general_famous_trait (general_template_id, trait_name, trait_desc, effect_type, effect_value, troop_restrict, sort_order) VALUES
(911,'连珠神箭','弓兵兵法发动概率提升','troop_tactics',0,3,1),
(911,'狂暴攻击','军队攻击力＋310','army_attack',310,0,2);

-- --- 912 凌统(狂) ← 15凌统: damage400 + count100 + 铁骑纵横 ---
INSERT INTO general_famous_trait (general_template_id, trait_name, trait_desc, effect_type, effect_value, troop_restrict, sort_order) VALUES
(912,'旋略勇进','统领士卒伤害＋400','soldier_damage',400,0,1),
(912,'大将之才','统领的士兵人数额外＋100','soldier_count',100,0,2),
(912,'铁骑纵横','骑兵战法发动概率提升','troop_tactics',0,2,3);


-- =============================================
-- 4. 存量数据迁移(已进阶的武将更新templateId)
-- =============================================
-- 如果已有武将进阶过(name以"(狂)"结尾)，更新其template_id
UPDATE `general` SET template_id = '900' WHERE name = '华雄(狂)' AND template_id = '121';
UPDATE `general` SET template_id = '901' WHERE name = '高顺(狂)' AND template_id = '125';
UPDATE `general` SET template_id = '902' WHERE name = '公孙瓒(狂)' AND template_id = '127';
UPDATE `general` SET template_id = '903' WHERE name = '吕布(狂)' AND template_id = '22';
UPDATE `general` SET template_id = '904' WHERE name = '曹仁(狂)' AND template_id = '100';
UPDATE `general` SET template_id = '905' WHERE name = '夏侯渊(狂)' AND template_id = '104';
UPDATE `general` SET template_id = '906' WHERE name = '张辽(狂)' AND template_id = '1';
UPDATE `general` SET template_id = '907' WHERE name = '关平(狂)' AND template_id = '107';
UPDATE `general` SET template_id = '908' WHERE name = '关兴(狂)' AND template_id = '111';
UPDATE `general` SET template_id = '909' WHERE name = '关羽(狂)' AND template_id = '8';
UPDATE `general` SET template_id = '910' WHERE name = '丁奉(狂)' AND template_id = '114';
UPDATE `general` SET template_id = '911' WHERE name = '程普(狂)' AND template_id = '118';
UPDATE `general` SET template_id = '912' WHERE name = '凌统(狂)' AND template_id = '15';
