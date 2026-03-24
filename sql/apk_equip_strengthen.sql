-- =============================================
-- 装备强化配置表 (从 APK equipStrength_cfg.json 提取)
-- addPro: 1=武器 2=铠甲 3=项链 4=头盔 5=戒指 6=鞋子
-- type 1/4 提供攻击属性, type 2/5 提供防御, type 3/6 最低
-- =============================================

CREATE TABLE IF NOT EXISTS `equip_strengthen_config` (
  `level` INT NOT NULL COMMENT '强化等级 1-20',
  `suc_rate` INT DEFAULT 10000 COMMENT '成功率(万分比)',
  `need_silver` INT DEFAULT 0 COMMENT '消耗白银',
  `stone_id` INT DEFAULT 0 COMMENT '需要的强化石ID',
  `bless_min` INT DEFAULT 0 COMMENT '祝福值下限',
  `bless_max` INT DEFAULT 0 COMMENT '祝福值上限',
  `sp_add` INT DEFAULT 0 COMMENT '速度加成',
  `add_type1` INT DEFAULT 0 COMMENT '武器加成(攻击)',
  `add_type2` INT DEFAULT 0 COMMENT '铠甲加成(防御)',
  `add_type3` INT DEFAULT 0 COMMENT '项链加成',
  `add_type4` INT DEFAULT 0 COMMENT '头盔加成(防御)',
  `add_type5` INT DEFAULT 0 COMMENT '戒指加成',
  `add_type6` INT DEFAULT 0 COMMENT '鞋子加成',
  PRIMARY KEY (`level`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='装备强化配置(APK提取)';

DELETE FROM `equip_strengthen_config`;

INSERT INTO `equip_strengthen_config` VALUES (1, 10000, 300, 14001, 0, 1, 0, 13, 9, 7, 13, 9, 7);
INSERT INTO `equip_strengthen_config` VALUES (2, 10000, 600, 14002, 0, 1, 0, 22, 15, 12, 22, 15, 12);
INSERT INTO `equip_strengthen_config` VALUES (3, 10000, 1000, 14003, 0, 1, 0, 31, 21, 17, 31, 21, 17);
INSERT INTO `equip_strengthen_config` VALUES (4, 10000, 2000, 14004, 0, 1, 1, 45, 30, 25, 45, 30, 25);
INSERT INTO `equip_strengthen_config` VALUES (5, 10000, 4000, 14005, 0, 1, 1, 67, 45, 37, 67, 45, 37);
INSERT INTO `equip_strengthen_config` VALUES (6, 10000, 8000, 14006, 0, 1, 2, 90, 60, 50, 90, 60, 50);
INSERT INTO `equip_strengthen_config` VALUES (7, 10000, 10000, 14007, 0, 1, 2, 117, 78, 65, 117, 78, 65);
INSERT INTO `equip_strengthen_config` VALUES (8, 10000, 15000, 14008, 0, 1, 3, 144, 96, 80, 144, 96, 80);
INSERT INTO `equip_strengthen_config` VALUES (9, 10000, 20000, 14009, 0, 1, 4, 171, 114, 95, 171, 114, 95);
INSERT INTO `equip_strengthen_config` VALUES (10, 10000, 30000, 14010, 0, 1, 5, 202, 135, 112, 202, 135, 112);
INSERT INTO `equip_strengthen_config` VALUES (11, 10000, 40000, 14011, 0, 1, 6, 234, 156, 130, 234, 156, 130);
INSERT INTO `equip_strengthen_config` VALUES (12, 10000, 60000, 14012, 0, 1, 7, 265, 177, 147, 265, 177, 147);
INSERT INTO `equip_strengthen_config` VALUES (13, 10000, 80000, 14013, 0, 1, 8, 297, 198, 165, 297, 198, 165);
INSERT INTO `equip_strengthen_config` VALUES (14, 10000, 100000, 14014, 0, 1, 9, 328, 219, 182, 328, 219, 182);
INSERT INTO `equip_strengthen_config` VALUES (15, 10000, 120000, 14015, 0, 1, 10, 360, 240, 200, 360, 240, 200);
INSERT INTO `equip_strengthen_config` VALUES (16, 10000, 140000, 14016, 0, 1, 11, 391, 261, 217, 391, 261, 217);
INSERT INTO `equip_strengthen_config` VALUES (17, 10000, 160000, 14017, 0, 1, 12, 423, 282, 235, 423, 282, 235);
INSERT INTO `equip_strengthen_config` VALUES (18, 10000, 180000, 14018, 0, 1, 13, 454, 303, 252, 454, 303, 252);
INSERT INTO `equip_strengthen_config` VALUES (19, 10000, 200000, 14019, 0, 1, 14, 486, 324, 270, 486, 324, 270);
INSERT INTO `equip_strengthen_config` VALUES (20, 10000, 220000, 14020, 0, 1, 15, 562, 375, 312, 562, 375, 312);