-- =============================================
-- 装备基础配置表 (从 APK equipInfo_cfg.json 提取)
-- 132件装备，含属性加成 (genAtt/genDef)
-- color: 2绿 3蓝 4紫 5橙
-- type: 1武器 2铠甲 3项链 4头盔 5戒指 6鞋子
-- =============================================

CREATE TABLE IF NOT EXISTS `equip_base_config` (
  `cfg_id` INT NOT NULL COMMENT '装备配置ID',
  `color` INT NOT NULL COMMENT '品质: 2绿 3蓝 4紫 5橙',
  `type` INT NOT NULL COMMENT '部位: 1武器 2铠甲 3项链 4头盔 5戒指 6鞋子',
  `need_level` INT DEFAULT 1 COMMENT '需要等级',
  `max_level` INT DEFAULT 20 COMMENT '最大强化等级',
  `suit_id` INT DEFAULT 0 COMMENT '所属套装ID (0=无套装)',
  `base_price` INT DEFAULT 0 COMMENT '基础售价',
  `gen_att` INT DEFAULT 0 COMMENT '武将攻击加成',
  `gen_def` INT DEFAULT 0 COMMENT '武将防御加成',
  `gen_for` INT DEFAULT 0 COMMENT '武力加成',
  `gen_leader` INT DEFAULT 0 COMMENT '统帅加成',
  `army_life` INT DEFAULT 0 COMMENT '士兵生命加成',
  `army_att` INT DEFAULT 0 COMMENT '士兵攻击加成',
  `army_def` INT DEFAULT 0 COMMENT '士兵防御加成',
  `army_sp` INT DEFAULT 0 COMMENT '速度加成',
  `army_hit` INT DEFAULT 0 COMMENT '命中加成',
  `army_mis` INT DEFAULT 0 COMMENT '闪避加成',
  PRIMARY KEY (`cfg_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='装备基础配置(APK提取)';

DELETE FROM `equip_base_config`;

INSERT INTO `equip_base_config` VALUES (22001, 2, 1, 1, 20, 0, 50, 70, 0, 0, 0, 0, 0, 0, 0, 0, 0);
INSERT INTO `equip_base_config` VALUES (22002, 2, 2, 5, 20, 0, 50, 50, 0, 0, 0, 0, 0, 0, 0, 0, 0);
INSERT INTO `equip_base_config` VALUES (22003, 2, 3, 10, 20, 0, 50, 30, 0, 0, 0, 0, 0, 0, 0, 0, 0);
INSERT INTO `equip_base_config` VALUES (22004, 2, 4, 1, 20, 0, 50, 0, 80, 0, 0, 0, 0, 0, 0, 0, 0);
INSERT INTO `equip_base_config` VALUES (22005, 2, 5, 5, 20, 0, 50, 0, 60, 0, 0, 0, 0, 0, 0, 0, 0);
INSERT INTO `equip_base_config` VALUES (22006, 2, 6, 10, 20, 0, 50, 0, 40, 0, 0, 0, 0, 0, 0, 0, 0);
INSERT INTO `equip_base_config` VALUES (22011, 2, 1, 20, 20, 0, 70, 116, 0, 0, 0, 0, 0, 0, 0, 0, 0);
INSERT INTO `equip_base_config` VALUES (22012, 2, 2, 20, 20, 0, 70, 96, 0, 0, 0, 0, 0, 0, 0, 0, 0);
INSERT INTO `equip_base_config` VALUES (22013, 2, 3, 20, 20, 0, 70, 68, 0, 0, 0, 0, 0, 0, 0, 0, 0);
INSERT INTO `equip_base_config` VALUES (22014, 2, 4, 20, 20, 0, 70, 0, 110, 0, 0, 0, 0, 0, 0, 0, 0);
INSERT INTO `equip_base_config` VALUES (22015, 2, 5, 20, 20, 0, 70, 0, 90, 0, 0, 0, 0, 0, 0, 0, 0);
INSERT INTO `equip_base_config` VALUES (22016, 2, 6, 20, 20, 0, 70, 0, 60, 0, 0, 0, 0, 0, 0, 0, 0);
INSERT INTO `equip_base_config` VALUES (22021, 2, 1, 40, 20, 0, 100, 172, 0, 0, 0, 0, 0, 0, 0, 0, 0);
INSERT INTO `equip_base_config` VALUES (22022, 2, 2, 40, 20, 0, 100, 128, 0, 0, 0, 0, 0, 0, 0, 0, 0);
INSERT INTO `equip_base_config` VALUES (22023, 2, 3, 40, 20, 0, 100, 108, 0, 0, 0, 0, 0, 0, 0, 0, 0);
INSERT INTO `equip_base_config` VALUES (22024, 2, 4, 40, 20, 0, 100, 0, 172, 0, 0, 0, 0, 0, 0, 0, 0);
INSERT INTO `equip_base_config` VALUES (22025, 2, 5, 40, 20, 0, 100, 0, 128, 0, 0, 0, 0, 0, 0, 0, 0);
INSERT INTO `equip_base_config` VALUES (22026, 2, 6, 40, 20, 0, 100, 0, 108, 0, 0, 0, 0, 0, 0, 0, 0);
INSERT INTO `equip_base_config` VALUES (22031, 2, 1, 60, 20, 0, 150, 215, 0, 0, 0, 0, 0, 0, 0, 0, 0);
INSERT INTO `equip_base_config` VALUES (22032, 2, 2, 60, 20, 0, 150, 172, 0, 0, 0, 0, 0, 0, 0, 0, 0);
INSERT INTO `equip_base_config` VALUES (22033, 2, 3, 60, 20, 0, 150, 144, 0, 0, 0, 0, 0, 0, 0, 0, 0);
INSERT INTO `equip_base_config` VALUES (22034, 2, 4, 60, 20, 0, 150, 0, 215, 0, 0, 0, 0, 0, 0, 0, 0);
INSERT INTO `equip_base_config` VALUES (22035, 2, 5, 60, 20, 0, 150, 0, 172, 0, 0, 0, 0, 0, 0, 0, 0);
INSERT INTO `equip_base_config` VALUES (22036, 2, 6, 60, 20, 0, 150, 0, 144, 0, 0, 0, 0, 0, 0, 0, 0);
INSERT INTO `equip_base_config` VALUES (22041, 2, 1, 80, 20, 0, 200, 254, 0, 0, 0, 0, 0, 0, 0, 0, 0);
INSERT INTO `equip_base_config` VALUES (22042, 2, 2, 80, 20, 0, 200, 216, 0, 0, 0, 0, 0, 0, 0, 0, 0);
INSERT INTO `equip_base_config` VALUES (22043, 2, 3, 80, 20, 0, 200, 198, 0, 0, 0, 0, 0, 0, 0, 0, 0);
INSERT INTO `equip_base_config` VALUES (22044, 2, 4, 80, 20, 0, 200, 0, 254, 0, 0, 0, 0, 0, 0, 0, 0);
INSERT INTO `equip_base_config` VALUES (22045, 2, 5, 80, 20, 0, 200, 0, 216, 0, 0, 0, 0, 0, 0, 0, 0);
INSERT INTO `equip_base_config` VALUES (22046, 2, 6, 80, 20, 0, 200, 0, 198, 0, 0, 0, 0, 0, 0, 0, 0);
INSERT INTO `equip_base_config` VALUES (23021, 3, 1, 20, 20, 2, 300, 150, 0, 0, 0, 0, 0, 0, 0, 0, 0);
INSERT INTO `equip_base_config` VALUES (23022, 3, 2, 20, 20, 2, 300, 120, 0, 0, 0, 0, 0, 0, 0, 0, 0);
INSERT INTO `equip_base_config` VALUES (23023, 3, 3, 20, 20, 2, 300, 90, 0, 0, 0, 0, 0, 0, 0, 0, 0);
INSERT INTO `equip_base_config` VALUES (23024, 3, 4, 20, 20, 2, 300, 0, 180, 0, 0, 0, 0, 0, 0, 0, 0);
INSERT INTO `equip_base_config` VALUES (23025, 3, 5, 20, 20, 2, 300, 0, 140, 0, 0, 0, 0, 0, 0, 0, 0);
INSERT INTO `equip_base_config` VALUES (23026, 3, 6, 20, 20, 2, 300, 0, 100, 0, 0, 0, 0, 0, 0, 0, 0);
INSERT INTO `equip_base_config` VALUES (23031, 3, 1, 40, 20, 3, 400, 200, 0, 0, 0, 0, 0, 0, 0, 0, 0);
INSERT INTO `equip_base_config` VALUES (23032, 3, 2, 40, 20, 3, 400, 170, 0, 0, 0, 0, 0, 0, 0, 0, 0);
INSERT INTO `equip_base_config` VALUES (23033, 3, 3, 40, 20, 3, 400, 130, 0, 0, 0, 0, 0, 0, 0, 0, 0);
INSERT INTO `equip_base_config` VALUES (23034, 3, 4, 40, 20, 3, 400, 0, 230, 0, 0, 0, 0, 0, 0, 0, 0);
INSERT INTO `equip_base_config` VALUES (23035, 3, 5, 40, 20, 3, 400, 0, 190, 0, 0, 0, 0, 0, 0, 0, 0);
INSERT INTO `equip_base_config` VALUES (23036, 3, 6, 40, 20, 3, 400, 0, 150, 0, 0, 0, 0, 0, 0, 0, 0);
INSERT INTO `equip_base_config` VALUES (23041, 3, 1, 60, 20, 4, 500, 280, 0, 0, 0, 0, 0, 0, 0, 0, 0);
INSERT INTO `equip_base_config` VALUES (23042, 3, 2, 60, 20, 4, 500, 220, 0, 0, 0, 0, 0, 0, 0, 0, 0);
INSERT INTO `equip_base_config` VALUES (23043, 3, 3, 60, 20, 4, 500, 160, 0, 0, 0, 0, 0, 0, 0, 0, 0);
INSERT INTO `equip_base_config` VALUES (23044, 3, 4, 60, 20, 4, 500, 0, 305, 0, 0, 0, 0, 0, 0, 0, 0);
INSERT INTO `equip_base_config` VALUES (23045, 3, 5, 60, 20, 4, 500, 0, 245, 0, 0, 0, 0, 0, 0, 0, 0);
INSERT INTO `equip_base_config` VALUES (23046, 3, 6, 60, 20, 4, 500, 0, 190, 0, 0, 0, 0, 0, 0, 0, 0);
INSERT INTO `equip_base_config` VALUES (23051, 3, 1, 80, 20, 5, 500, 335, 0, 0, 0, 0, 0, 0, 0, 0, 0);
INSERT INTO `equip_base_config` VALUES (23052, 3, 2, 80, 20, 5, 500, 270, 0, 0, 0, 0, 0, 0, 0, 0, 0);
INSERT INTO `equip_base_config` VALUES (23053, 3, 3, 80, 20, 5, 500, 215, 0, 0, 0, 0, 0, 0, 0, 0, 0);
INSERT INTO `equip_base_config` VALUES (23054, 3, 4, 80, 20, 5, 500, 0, 390, 0, 0, 0, 0, 0, 0, 0, 0);
INSERT INTO `equip_base_config` VALUES (23055, 3, 5, 80, 20, 5, 500, 0, 305, 0, 0, 0, 0, 0, 0, 0, 0);
INSERT INTO `equip_base_config` VALUES (23056, 3, 6, 80, 20, 5, 500, 0, 225, 0, 0, 0, 0, 0, 0, 0, 0);
INSERT INTO `equip_base_config` VALUES (23061, 3, 1, 40, 20, 6, 700, 260, 0, 0, 0, 0, 0, 0, 0, 0, 0);
INSERT INTO `equip_base_config` VALUES (23062, 3, 2, 40, 20, 6, 700, 200, 0, 0, 0, 0, 0, 0, 0, 0, 0);
INSERT INTO `equip_base_config` VALUES (23063, 3, 3, 40, 20, 6, 700, 160, 0, 0, 0, 0, 0, 0, 0, 0, 0);
INSERT INTO `equip_base_config` VALUES (23064, 3, 4, 40, 20, 6, 700, 0, 230, 0, 0, 0, 0, 0, 0, 0, 0);
INSERT INTO `equip_base_config` VALUES (23065, 3, 5, 40, 20, 6, 700, 0, 200, 0, 0, 0, 0, 0, 0, 0, 0);
INSERT INTO `equip_base_config` VALUES (23066, 3, 6, 40, 20, 6, 700, 0, 170, 0, 0, 0, 0, 0, 0, 0, 0);
INSERT INTO `equip_base_config` VALUES (23071, 3, 1, 50, 20, 7, 900, 335, 0, 0, 0, 0, 0, 0, 0, 0, 0);
INSERT INTO `equip_base_config` VALUES (23072, 3, 2, 50, 20, 7, 900, 270, 0, 0, 0, 0, 0, 0, 0, 0, 0);
INSERT INTO `equip_base_config` VALUES (23073, 3, 3, 50, 20, 7, 900, 215, 0, 0, 0, 0, 0, 0, 0, 0, 0);
INSERT INTO `equip_base_config` VALUES (23074, 3, 4, 50, 20, 7, 900, 0, 310, 0, 0, 0, 0, 0, 0, 0, 0);
INSERT INTO `equip_base_config` VALUES (23075, 3, 5, 50, 20, 7, 900, 0, 250, 0, 0, 0, 0, 0, 0, 0, 0);
INSERT INTO `equip_base_config` VALUES (23076, 3, 6, 50, 20, 7, 900, 0, 200, 0, 0, 0, 0, 0, 0, 0, 0);
INSERT INTO `equip_base_config` VALUES (23081, 3, 1, 60, 20, 8, 1000, 360, 0, 0, 0, 0, 0, 0, 0, 0, 0);
INSERT INTO `equip_base_config` VALUES (23082, 3, 2, 60, 20, 8, 1000, 290, 0, 0, 0, 0, 0, 0, 0, 0, 0);
INSERT INTO `equip_base_config` VALUES (23083, 3, 3, 60, 20, 8, 1000, 245, 0, 0, 0, 0, 0, 0, 0, 0, 0);
INSERT INTO `equip_base_config` VALUES (23084, 3, 4, 60, 20, 8, 1000, 0, 330, 0, 0, 0, 0, 0, 0, 0, 0);
INSERT INTO `equip_base_config` VALUES (23085, 3, 5, 60, 20, 8, 1000, 0, 265, 0, 0, 0, 0, 0, 0, 0, 0);
INSERT INTO `equip_base_config` VALUES (23086, 3, 6, 60, 20, 8, 1000, 0, 210, 0, 0, 0, 0, 0, 0, 0, 0);
INSERT INTO `equip_base_config` VALUES (23091, 3, 1, 90, 20, 17, 1000, 405, 0, 0, 0, 0, 0, 0, 0, 0, 0);
INSERT INTO `equip_base_config` VALUES (23092, 3, 2, 90, 20, 17, 1000, 330, 0, 0, 0, 0, 0, 0, 0, 0, 0);
INSERT INTO `equip_base_config` VALUES (23093, 3, 3, 90, 20, 17, 1000, 265, 0, 0, 0, 0, 0, 0, 0, 0, 0);
INSERT INTO `equip_base_config` VALUES (23094, 3, 4, 90, 20, 17, 1000, 0, 460, 0, 0, 0, 0, 0, 0, 0, 0);
INSERT INTO `equip_base_config` VALUES (23095, 3, 5, 90, 20, 17, 1000, 0, 335, 0, 0, 0, 0, 0, 0, 0, 0);
INSERT INTO `equip_base_config` VALUES (23096, 3, 6, 90, 20, 17, 1000, 0, 255, 0, 0, 0, 0, 0, 0, 0, 0);
INSERT INTO `equip_base_config` VALUES (23121, 3, 1, 40, 20, 12, 1000, 310, 0, 0, 0, 0, 0, 0, 0, 0, 0);
INSERT INTO `equip_base_config` VALUES (23122, 3, 2, 40, 20, 12, 1000, 250, 0, 0, 0, 0, 0, 0, 0, 0, 0);
INSERT INTO `equip_base_config` VALUES (23123, 3, 3, 40, 20, 12, 1000, 210, 0, 0, 0, 0, 0, 0, 0, 0, 0);
INSERT INTO `equip_base_config` VALUES (23124, 3, 4, 40, 20, 12, 1000, 0, 300, 0, 0, 0, 0, 0, 0, 0, 0);
INSERT INTO `equip_base_config` VALUES (23125, 3, 5, 40, 20, 12, 1000, 0, 250, 0, 0, 0, 0, 0, 0, 0, 0);
INSERT INTO `equip_base_config` VALUES (23126, 3, 6, 40, 20, 12, 1000, 0, 220, 0, 0, 0, 0, 0, 0, 0, 0);
INSERT INTO `equip_base_config` VALUES (24091, 4, 1, 60, 20, 9, 2000, 400, 0, 0, 0, 0, 0, 0, 0, 0, 0);
INSERT INTO `equip_base_config` VALUES (24092, 4, 2, 60, 20, 9, 2000, 315, 0, 0, 0, 0, 0, 0, 0, 0, 0);
INSERT INTO `equip_base_config` VALUES (24093, 4, 3, 60, 20, 9, 2000, 260, 0, 0, 0, 0, 0, 0, 0, 0, 0);
INSERT INTO `equip_base_config` VALUES (24094, 4, 4, 60, 20, 9, 2000, 0, 380, 0, 0, 0, 0, 0, 0, 0, 0);
INSERT INTO `equip_base_config` VALUES (24095, 4, 5, 60, 20, 9, 2000, 0, 295, 0, 0, 0, 0, 0, 0, 0, 0);
INSERT INTO `equip_base_config` VALUES (24096, 4, 6, 60, 20, 9, 2000, 0, 230, 0, 0, 0, 0, 0, 0, 0, 0);
INSERT INTO `equip_base_config` VALUES (24101, 4, 1, 80, 20, 10, 3000, 460, 0, 0, 0, 0, 0, 0, 0, 0, 0);
INSERT INTO `equip_base_config` VALUES (24102, 4, 2, 80, 20, 10, 3000, 400, 0, 0, 0, 0, 0, 0, 0, 0, 0);
INSERT INTO `equip_base_config` VALUES (24103, 4, 3, 80, 20, 10, 3000, 340, 0, 0, 0, 0, 0, 0, 0, 0, 0);
INSERT INTO `equip_base_config` VALUES (24104, 4, 4, 80, 20, 10, 3000, 0, 515, 0, 0, 0, 0, 0, 0, 0, 0);
INSERT INTO `equip_base_config` VALUES (24105, 4, 5, 80, 20, 10, 3000, 0, 430, 0, 0, 0, 0, 0, 0, 0, 0);
INSERT INTO `equip_base_config` VALUES (24106, 4, 6, 80, 20, 10, 3000, 0, 380, 0, 0, 0, 0, 0, 0, 0, 0);
INSERT INTO `equip_base_config` VALUES (24131, 4, 1, 60, 20, 13, 2000, 495, 0, 0, 0, 0, 0, 0, 0, 0, 0);
INSERT INTO `equip_base_config` VALUES (24132, 4, 2, 60, 20, 13, 2000, 410, 0, 0, 0, 0, 0, 0, 0, 0, 0);
INSERT INTO `equip_base_config` VALUES (24133, 4, 3, 60, 20, 13, 2000, 360, 0, 0, 0, 0, 0, 0, 0, 0, 0);
INSERT INTO `equip_base_config` VALUES (24134, 4, 4, 60, 20, 13, 2000, 0, 415, 0, 0, 0, 0, 0, 0, 0, 0);
INSERT INTO `equip_base_config` VALUES (24135, 4, 5, 60, 20, 13, 2000, 0, 335, 0, 0, 0, 0, 0, 0, 0, 0);
INSERT INTO `equip_base_config` VALUES (24136, 4, 6, 60, 20, 13, 2000, 0, 300, 0, 0, 0, 0, 0, 0, 0, 0);
INSERT INTO `equip_base_config` VALUES (24141, 4, 1, 70, 20, 14, 2000, 420, 0, 0, 0, 0, 0, 0, 0, 0, 0);
INSERT INTO `equip_base_config` VALUES (24142, 4, 2, 70, 20, 14, 2000, 330, 0, 0, 0, 0, 0, 0, 0, 0, 0);
INSERT INTO `equip_base_config` VALUES (24143, 4, 3, 70, 20, 14, 2000, 300, 0, 0, 0, 0, 0, 0, 0, 0, 0);
INSERT INTO `equip_base_config` VALUES (24144, 4, 4, 70, 20, 14, 2000, 0, 400, 0, 0, 0, 0, 0, 0, 0, 0);
INSERT INTO `equip_base_config` VALUES (24145, 4, 5, 70, 20, 14, 2000, 0, 330, 0, 0, 0, 0, 0, 0, 0, 0);
INSERT INTO `equip_base_config` VALUES (24146, 4, 6, 70, 20, 14, 2000, 0, 300, 0, 0, 0, 0, 0, 0, 0, 0);
INSERT INTO `equip_base_config` VALUES (24151, 4, 1, 70, 20, 15, 2000, 620, 0, 0, 0, 0, 0, 0, 0, 0, 0);
INSERT INTO `equip_base_config` VALUES (24152, 4, 2, 70, 20, 15, 2000, 500, 0, 0, 0, 0, 0, 0, 0, 0, 0);
INSERT INTO `equip_base_config` VALUES (24153, 4, 3, 70, 20, 15, 2000, 425, 0, 0, 0, 0, 0, 0, 0, 0, 0);
INSERT INTO `equip_base_config` VALUES (24154, 4, 4, 70, 20, 15, 2000, 0, 375, 0, 0, 0, 0, 0, 0, 0, 0);
INSERT INTO `equip_base_config` VALUES (24155, 4, 5, 70, 20, 15, 2000, 0, 300, 0, 0, 0, 0, 0, 0, 0, 0);
INSERT INTO `equip_base_config` VALUES (24156, 4, 6, 70, 20, 15, 2000, 0, 250, 0, 0, 0, 0, 0, 0, 0, 0);
INSERT INTO `equip_base_config` VALUES (24161, 4, 1, 70, 20, 16, 2000, 420, 0, 0, 0, 0, 0, 0, 0, 0, 0);
INSERT INTO `equip_base_config` VALUES (24162, 4, 2, 70, 20, 16, 2000, 330, 0, 0, 0, 0, 0, 0, 0, 0, 0);
INSERT INTO `equip_base_config` VALUES (24163, 4, 3, 70, 20, 16, 2000, 300, 0, 0, 0, 0, 0, 0, 0, 0, 0);
INSERT INTO `equip_base_config` VALUES (24164, 4, 4, 70, 20, 16, 2000, 0, 400, 0, 0, 0, 0, 0, 0, 0, 0);
INSERT INTO `equip_base_config` VALUES (24165, 4, 5, 70, 20, 16, 2000, 0, 330, 0, 0, 0, 0, 0, 0, 0, 0);
INSERT INTO `equip_base_config` VALUES (24166, 4, 6, 70, 20, 16, 2000, 0, 300, 0, 0, 0, 0, 0, 0, 0, 0);
INSERT INTO `equip_base_config` VALUES (25111, 5, 1, 80, 20, 11, 5000, 625, 0, 0, 0, 0, 0, 0, 0, 0, 0);
INSERT INTO `equip_base_config` VALUES (25112, 5, 2, 80, 20, 11, 5000, 510, 0, 0, 0, 0, 0, 0, 0, 0, 0);
INSERT INTO `equip_base_config` VALUES (25113, 5, 3, 80, 20, 11, 5000, 410, 0, 0, 0, 0, 0, 0, 0, 0, 0);
INSERT INTO `equip_base_config` VALUES (25114, 5, 4, 80, 20, 11, 5000, 0, 655, 0, 0, 0, 0, 0, 0, 0, 0);
INSERT INTO `equip_base_config` VALUES (25115, 5, 5, 80, 20, 11, 5000, 0, 530, 0, 0, 0, 0, 0, 0, 0, 0);
INSERT INTO `equip_base_config` VALUES (25116, 5, 6, 80, 20, 11, 5000, 0, 420, 0, 0, 0, 0, 0, 0, 0, 0);
INSERT INTO `equip_base_config` VALUES (25181, 5, 1, 90, 20, 18, 2000, 735, 0, 0, 0, 0, 0, 0, 0, 0, 0);
INSERT INTO `equip_base_config` VALUES (25182, 5, 2, 90, 20, 18, 2000, 620, 0, 0, 0, 0, 0, 0, 0, 0, 0);
INSERT INTO `equip_base_config` VALUES (25183, 5, 3, 90, 20, 18, 2000, 500, 0, 0, 0, 0, 0, 0, 0, 0, 0);
INSERT INTO `equip_base_config` VALUES (25184, 5, 4, 90, 20, 18, 2000, 0, 755, 0, 0, 0, 0, 0, 0, 0, 0);
INSERT INTO `equip_base_config` VALUES (25185, 5, 5, 90, 20, 18, 2000, 0, 630, 0, 0, 0, 0, 0, 0, 0, 0);
INSERT INTO `equip_base_config` VALUES (25186, 5, 6, 90, 20, 18, 2000, 0, 520, 0, 0, 0, 0, 0, 0, 0, 0);