-- =============================================
-- 装备品质配置表 (从 APK EquipQuality_cfg.json 提取)
-- attrRate: 属性发挥率(万分比), 完美=10000=100%
-- =============================================

CREATE TABLE IF NOT EXISTS `equip_quality_config` (
  `quality_id` INT NOT NULL COMMENT '品质ID 1-5',
  `name` VARCHAR(16) NOT NULL COMMENT '品质名称',
  `attr_rate` INT DEFAULT 10000 COMMENT '属性发挥率(万分比)',
  `acquire_rate` INT DEFAULT 0 COMMENT '获取概率(万分比)',
  `increase_rate` INT DEFAULT 0 COMMENT '洗练提升概率(万分比)',
  `need_silver` INT DEFAULT 0 COMMENT '洗练消耗白银',
  PRIMARY KEY (`quality_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='装备品质配置(APK提取)';

DELETE FROM `equip_quality_config`;

INSERT INTO `equip_quality_config` VALUES (1, '粗糙', 8000, 5890, 8000, 500);
INSERT INTO `equip_quality_config` VALUES (2, '普通', 8500, 3000, 4000, 1000);
INSERT INTO `equip_quality_config` VALUES (3, '优良', 9000, 1000, 2500, 2000);
INSERT INTO `equip_quality_config` VALUES (4, '无暇', 9500, 100, 1000, 3000);
INSERT INTO `equip_quality_config` VALUES (5, '完美', 10000, 10, 0, 0);