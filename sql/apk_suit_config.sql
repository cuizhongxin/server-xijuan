-- =============================================
-- 套装配置表 (从 APK suit_cfg.json 提取)
-- 集齐6件可激活套装加成
-- =============================================

CREATE TABLE IF NOT EXISTS `suit_config` (
  `suit_id` INT NOT NULL COMMENT '套装ID',
  `name` VARCHAR(32) NOT NULL COMMENT '套装名称',
  `color` INT DEFAULT 3 COMMENT '品质: 3蓝 4紫 5橙',
  `level` INT DEFAULT 1 COMMENT '套装等级要求',
  `gen_att` INT DEFAULT 0 COMMENT '武将攻击加成',
  `gen_def` INT DEFAULT 0 COMMENT '武将防御加成',
  `gen_for` INT DEFAULT 0 COMMENT '武力加成',
  `gen_leader` INT DEFAULT 0 COMMENT '统帅加成',
  `army_life` INT DEFAULT 0 COMMENT '士兵生命加成',
  `army_att` INT DEFAULT 0 COMMENT '士兵攻击加成',
  `army_def` INT DEFAULT 0 COMMENT '士兵防御加成',
  `army_sp` INT DEFAULT 0 COMMENT '士兵速度加成',
  `army_hit` INT DEFAULT 0 COMMENT '命中加成',
  `army_mis` INT DEFAULT 0 COMMENT '闪避加成',
  `equip_ids` VARCHAR(256) COMMENT '套装装备ID列表(逗号分隔)',
  PRIMARY KEY (`suit_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='套装配置(APK提取)';

DELETE FROM `suit_config`;

INSERT INTO `suit_config` VALUES (2, '宣武套装', 3, 20, 60, 0, 0, 0, 20, 0, 0, 0, 0, 0, '23021,23022,23023,23024,23025,23026');
INSERT INTO `suit_config` VALUES (3, '折冲套装', 3, 30, 100, 0, 0, 0, 30, 0, 0, 0, 0, 0, '23031,23032,23033,23034,23035,23036');
INSERT INTO `suit_config` VALUES (4, '骁勇套装', 3, 40, 140, 0, 0, 0, 40, 0, 0, 0, 0, 0, '23041,23042,23043,23044,23045,23046');
INSERT INTO `suit_config` VALUES (5, '破俘套装', 3, 55, 180, 0, 0, 0, 50, 0, 0, 0, 0, 0, '23051,23052,23053,23054,23055,23056');
INSERT INTO `suit_config` VALUES (6, '陷阵套装', 3, 50, 200, 0, 10, 0, 0, 0, 0, 0, 0, 0, '23061,23062,23063,23064,23065,23066');
INSERT INTO `suit_config` VALUES (7, '狂战套装', 3, 60, 260, 0, 20, 0, 0, 0, 0, 0, 0, 0, '23071,23072,23073,23074,23075,23076');
INSERT INTO `suit_config` VALUES (8, '天狼套装', 3, 70, 500, 0, 20, 0, 0, 0, 0, 0, 0, 0, '23081,23082,23083,23084,23085,23086');
INSERT INTO `suit_config` VALUES (9, '破军套装', 4, 80, 550, 0, 20, 0, 0, 0, 0, 0, 0, 0, '24091,24092,24093,24094,24095,24096');
INSERT INTO `suit_config` VALUES (10, '龙威套装', 4, 90, 550, 0, 20, 20, 0, 0, 0, 0, 0, 0, '24101,24102,24103,24104,24105,24106');
INSERT INTO `suit_config` VALUES (11, '战神套装', 5, 99, 600, 0, 20, 20, 0, 0, 0, 0, 0, 0, '25111,25112,25113,25114,25115,25116');
INSERT INTO `suit_config` VALUES (12, '鹰扬套装', 3, 75, 400, 0, 0, 20, 0, 0, 0, 0, 0, 0, '23121,23122,23123,23124,23125,23126');
INSERT INTO `suit_config` VALUES (13, '虎啸套装', 4, 85, 0, 400, 0, 20, 0, 0, 0, 0, 0, 10, '24131,24132,24133,24134,24135,24136');
INSERT INTO `suit_config` VALUES (14, '地煞套装', 4, 82, 200, 240, 0, 0, 0, 0, 0, 0, 0, 5, '24141,24142,24143,24144,24145,24146');
INSERT INTO `suit_config` VALUES (15, '天诛套装', 4, 98, 900, 0, 0, 0, 0, 0, 0, 0, 0, 0, '24151,24152,24153,24154,24155,24156');
INSERT INTO `suit_config` VALUES (16, '幽冥套装', 4, 83, 240, 200, 0, 0, 0, 0, 0, 5, 0, 0, '24161,24162,24163,24164,24165,24166');
INSERT INTO `suit_config` VALUES (17, '征戎套装', 3, 62, 280, 0, 0, 0, 80, 0, 0, 0, 0, 0, '23091,23092,23093,23094,23095,23096');
INSERT INTO `suit_config` VALUES (18, '诛邪套装', 5, 100, 700, 0, 25, 25, 0, 0, 0, 0, 0, 0, '25181,25182,25183,25184,25185,25186');