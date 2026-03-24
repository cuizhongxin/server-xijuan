-- =============================================
-- 编制配置表 (从 APK formation_cfg.json 提取)
-- 10级编制，影响兵力上限和攻防加成
-- =============================================

CREATE TABLE IF NOT EXISTS `formation_level_config` (
  `level` INT NOT NULL COMMENT '编制等级 1-10',
  `name` VARCHAR(16) NOT NULL COMMENT '编制名称',
  `need_king_level` INT DEFAULT 1 COMMENT '需要的君主等级',
  `need_silver` INT DEFAULT 0 COMMENT '升级需要的白银',
  `max_people` INT DEFAULT 100 COMMENT '兵力上限',
  `add_att` INT DEFAULT 0 COMMENT '攻击加成',
  `add_def` INT DEFAULT 0 COMMENT '防御加成',
  PRIMARY KEY (`level`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='编制等级配置(APK提取)';

DELETE FROM `formation_level_config`;

INSERT INTO `formation_level_config` VALUES (1, '队', 1, 500, 100, 230, 230);
INSERT INTO `formation_level_config` VALUES (2, '伙', 5, 800, 200, 250, 250);
INSERT INTO `formation_level_config` VALUES (3, '哨', 10, 2000, 300, 270, 270);
INSERT INTO `formation_level_config` VALUES (4, '岗', 20, 7000, 400, 290, 290);
INSERT INTO `formation_level_config` VALUES (5, '都', 30, 15000, 500, 310, 310);
INSERT INTO `formation_level_config` VALUES (6, '营', 40, 30000, 600, 330, 330);
INSERT INTO `formation_level_config` VALUES (7, '团', 50, 100000, 700, 350, 350);
INSERT INTO `formation_level_config` VALUES (8, '师', 60, 200000, 800, 370, 370);
INSERT INTO `formation_level_config` VALUES (9, '旅', 70, 300000, 900, 390, 390);
INSERT INTO `formation_level_config` VALUES (10, '军', 80, 500000, 1000, 410, 410);