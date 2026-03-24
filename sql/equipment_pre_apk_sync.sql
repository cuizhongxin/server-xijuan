-- =============================================
-- equipment_pre 表完整重建 (基于APK数据)
-- 生成时间: 2026-03-12T15:11:23.236255
-- =============================================

-- 备份并删除旧表
DROP TABLE IF EXISTS equipment_pre_old;
RENAME TABLE equipment_pre TO equipment_pre_old;

-- 创建新表
CREATE TABLE `equipment_pre` (
  `id` INT NOT NULL COMMENT 'APK装备ID',
  `name` VARCHAR(50) NOT NULL COMMENT '装备名称',
  `description` VARCHAR(200) DEFAULT NULL COMMENT '装备描述',
  `color` TINYINT NOT NULL DEFAULT 2 COMMENT '品质: 2=绿, 3=蓝, 4=紫, 5=橙',
  `type` TINYINT NOT NULL COMMENT '部位: 1=武器, 2=戒指, 3=项链, 4=铠甲, 5=头盔, 6=靴子',
  `position` VARCHAR(10) NOT NULL COMMENT '部位中文名',
  `need_level` INT NOT NULL DEFAULT 1 COMMENT '穿戴所需等级',
  `max_level` INT NOT NULL DEFAULT 20 COMMENT '最大强化等级',
  `suit_id` INT NOT NULL DEFAULT 0 COMMENT '套装ID (0=散件)',
  `suit_name` VARCHAR(20) DEFAULT NULL COMMENT '套装名称',
  `base_price` INT NOT NULL DEFAULT 0 COMMENT '基础售价(白银)',
  `icon_url` VARCHAR(128) DEFAULT NULL COMMENT '图片文件名',
  `gen_att` INT NOT NULL DEFAULT 0 COMMENT '武将攻击',
  `gen_def` INT NOT NULL DEFAULT 0 COMMENT '武将防御',
  `gen_for` INT NOT NULL DEFAULT 0 COMMENT '武勇',
  `gen_leader` INT NOT NULL DEFAULT 0 COMMENT '统御',
  `army_life` INT NOT NULL DEFAULT 0 COMMENT '军队生命',
  `army_att` INT NOT NULL DEFAULT 0 COMMENT '军队攻击',
  `army_def` INT NOT NULL DEFAULT 0 COMMENT '军队防御',
  `army_sp` INT NOT NULL DEFAULT 0 COMMENT '军队速度/机动',
  `army_hit` INT NOT NULL DEFAULT 0 COMMENT '命中',
  `army_mis` INT NOT NULL DEFAULT 0 COMMENT '闪避',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='装备模板表(APK数据)';

INSERT INTO `equipment_pre` VALUES (22001, '黑铁剑', '黑铁剑', 2, 1, '武器', 1, 20, 0, NULL, 50, '22001.jpg', 70, 0, 0, 0, 0, 0, 0, 0, 0, 0);
INSERT INTO `equipment_pre` VALUES (22002, '黑铁戒', '黑铁戒', 2, 2, '戒指', 5, 20, 0, NULL, 50, '22002.jpg', 50, 0, 0, 0, 0, 0, 0, 0, 0, 0);
INSERT INTO `equipment_pre` VALUES (22003, '黑铁项链', '黑铁项链', 2, 3, '项链', 10, 20, 0, NULL, 50, '22003.jpg', 30, 0, 0, 0, 0, 0, 0, 0, 0, 0);
INSERT INTO `equipment_pre` VALUES (22004, '黑铁锴', '黑铁锴', 2, 4, '铠甲', 1, 20, 0, NULL, 50, '22004.jpg', 0, 80, 0, 0, 0, 0, 0, 0, 0, 0);
INSERT INTO `equipment_pre` VALUES (22005, '黑铁盔', '黑铁盔', 2, 5, '头盔', 5, 20, 0, NULL, 50, '22005.jpg', 0, 60, 0, 0, 0, 0, 0, 0, 0, 0);
INSERT INTO `equipment_pre` VALUES (22006, '黑铁靴', '黑铁靴', 2, 6, '靴子', 10, 20, 0, NULL, 50, '22006.jpg', 0, 40, 0, 0, 0, 0, 0, 0, 0, 0);
INSERT INTO `equipment_pre` VALUES (22011, '精钢之剑', '精钢之剑', 2, 1, '武器', 20, 20, 0, NULL, 70, '22011.jpg', 116, 0, 0, 0, 0, 0, 0, 0, 0, 0);
INSERT INTO `equipment_pre` VALUES (22012, '精钢戒指', '精钢戒指', 2, 2, '戒指', 20, 20, 0, NULL, 70, '22012.jpg', 96, 0, 0, 0, 0, 0, 0, 0, 0, 0);
INSERT INTO `equipment_pre` VALUES (22013, '精钢项链', '精钢项链', 2, 3, '项链', 20, 20, 0, NULL, 70, '22013.jpg', 68, 0, 0, 0, 0, 0, 0, 0, 0, 0);
INSERT INTO `equipment_pre` VALUES (22014, '精钢之甲', '精钢之甲', 2, 4, '铠甲', 20, 20, 0, NULL, 70, '22014.jpg', 0, 110, 0, 0, 0, 0, 0, 0, 0, 0);
INSERT INTO `equipment_pre` VALUES (22015, '精钢盔', '精钢盔', 2, 5, '头盔', 20, 20, 0, NULL, 70, '22015.jpg', 0, 90, 0, 0, 0, 0, 0, 0, 0, 0);
INSERT INTO `equipment_pre` VALUES (22016, '精钢靴', '精钢靴', 2, 6, '靴子', 20, 20, 0, NULL, 70, '22016.jpg', 0, 60, 0, 0, 0, 0, 0, 0, 0, 0);
INSERT INTO `equipment_pre` VALUES (22021, '紫铜枪', '紫铜枪', 2, 1, '武器', 40, 20, 0, NULL, 100, '22021.jpg', 172, 0, 0, 0, 0, 0, 0, 0, 0, 0);
INSERT INTO `equipment_pre` VALUES (22022, '紫铜戒', '紫铜戒', 2, 2, '戒指', 40, 20, 0, NULL, 100, '22022.jpg', 128, 0, 0, 0, 0, 0, 0, 0, 0, 0);
INSERT INTO `equipment_pre` VALUES (22023, '紫铜项链', '紫铜项链', 2, 3, '项链', 40, 20, 0, NULL, 100, '22023.jpg', 108, 0, 0, 0, 0, 0, 0, 0, 0, 0);
INSERT INTO `equipment_pre` VALUES (22024, '紫铜锴', '紫铜锴', 2, 4, '铠甲', 40, 20, 0, NULL, 100, '22024.jpg', 0, 172, 0, 0, 0, 0, 0, 0, 0, 0);
INSERT INTO `equipment_pre` VALUES (22025, '紫铜盔', '紫铜盔', 2, 5, '头盔', 40, 20, 0, NULL, 100, '22025.jpg', 0, 128, 0, 0, 0, 0, 0, 0, 0, 0);
INSERT INTO `equipment_pre` VALUES (22026, '紫铜靴', '紫铜靴', 2, 6, '靴子', 40, 20, 0, NULL, 100, '22026.jpg', 0, 108, 0, 0, 0, 0, 0, 0, 0, 0);
INSERT INTO `equipment_pre` VALUES (22031, '亮银剑', '亮银剑', 2, 1, '武器', 60, 20, 0, NULL, 150, '22031.jpg', 215, 0, 0, 0, 0, 0, 0, 0, 0, 0);
INSERT INTO `equipment_pre` VALUES (22032, '亮银戒', '亮银戒', 2, 2, '戒指', 60, 20, 0, NULL, 150, '22032.jpg', 172, 0, 0, 0, 0, 0, 0, 0, 0, 0);
INSERT INTO `equipment_pre` VALUES (22033, '亮银项链', '亮银项链', 2, 3, '项链', 60, 20, 0, NULL, 150, '22033.jpg', 144, 0, 0, 0, 0, 0, 0, 0, 0, 0);
INSERT INTO `equipment_pre` VALUES (22034, '亮银锴', '亮银锴', 2, 4, '铠甲', 60, 20, 0, NULL, 150, '22034.jpg', 0, 215, 0, 0, 0, 0, 0, 0, 0, 0);
INSERT INTO `equipment_pre` VALUES (22035, '亮银盔', '亮银盔', 2, 5, '头盔', 60, 20, 0, NULL, 150, '22035.jpg', 0, 172, 0, 0, 0, 0, 0, 0, 0, 0);
INSERT INTO `equipment_pre` VALUES (22036, '亮银靴', '亮银靴', 2, 6, '靴子', 60, 20, 0, NULL, 150, '22036.jpg', 0, 144, 0, 0, 0, 0, 0, 0, 0, 0);
INSERT INTO `equipment_pre` VALUES (22041, '百炼枪', '百炼枪', 2, 1, '武器', 80, 20, 0, NULL, 200, '22041.jpg', 254, 0, 0, 0, 0, 0, 0, 0, 0, 0);
INSERT INTO `equipment_pre` VALUES (22042, '百炼戒', '百炼戒', 2, 2, '戒指', 80, 20, 0, NULL, 200, '22042.jpg', 216, 0, 0, 0, 0, 0, 0, 0, 0, 0);
INSERT INTO `equipment_pre` VALUES (22043, '百炼项链', '百炼项链', 2, 3, '项链', 80, 20, 0, NULL, 200, '22043.jpg', 198, 0, 0, 0, 0, 0, 0, 0, 0, 0);
INSERT INTO `equipment_pre` VALUES (22044, '百炼锴', '百炼锴', 2, 4, '铠甲', 80, 20, 0, NULL, 200, '22044.jpg', 0, 254, 0, 0, 0, 0, 0, 0, 0, 0);
INSERT INTO `equipment_pre` VALUES (22045, '百炼盔', '百炼盔', 2, 5, '头盔', 80, 20, 0, NULL, 200, '22045.jpg', 0, 216, 0, 0, 0, 0, 0, 0, 0, 0);
INSERT INTO `equipment_pre` VALUES (22046, '百炼靴', '百炼靴', 2, 6, '靴子', 80, 20, 0, NULL, 200, '22046.jpg', 0, 198, 0, 0, 0, 0, 0, 0, 0, 0);
INSERT INTO `equipment_pre` VALUES (23021, '宣武剑', '宣武佩剑', 3, 1, '武器', 20, 20, 2, '宣武套装', 300, '23021.jpg', 150, 0, 0, 0, 0, 0, 0, 0, 0, 0);
INSERT INTO `equipment_pre` VALUES (23022, '宣武戒指', '宣武戒指', 3, 2, '戒指', 20, 20, 2, '宣武套装', 300, '23022.jpg', 120, 0, 0, 0, 0, 0, 0, 0, 0, 0);
INSERT INTO `equipment_pre` VALUES (23023, '宣武项链', '宣武项链', 3, 3, '项链', 20, 20, 2, '宣武套装', 300, '23023.jpg', 90, 0, 0, 0, 0, 0, 0, 0, 0, 0);
INSERT INTO `equipment_pre` VALUES (23024, '宣武铠', '宣武铠', 3, 4, '铠甲', 20, 20, 2, '宣武套装', 300, '23024.jpg', 0, 180, 0, 0, 0, 0, 0, 0, 0, 0);
INSERT INTO `equipment_pre` VALUES (23025, '宣武盔', '宣武盔', 3, 5, '头盔', 20, 20, 2, '宣武套装', 300, '23025.jpg', 0, 140, 0, 0, 0, 0, 0, 0, 0, 0);
INSERT INTO `equipment_pre` VALUES (23026, '宣武靴', '宣武靴', 3, 6, '靴子', 20, 20, 2, '宣武套装', 300, '23026.jpg', 0, 100, 0, 0, 0, 0, 0, 0, 0, 0);
INSERT INTO `equipment_pre` VALUES (23031, '折冲剑', '折冲刀', 3, 1, '武器', 40, 20, 3, '折冲套装', 400, '23031.jpg', 200, 0, 0, 0, 0, 0, 0, 0, 0, 0);
INSERT INTO `equipment_pre` VALUES (23032, '折冲戒指', '折冲戒指', 3, 2, '戒指', 40, 20, 3, '折冲套装', 400, '23032.jpg', 170, 0, 0, 0, 0, 0, 0, 0, 0, 0);
INSERT INTO `equipment_pre` VALUES (23033, '折冲项链', '折冲项链', 3, 3, '项链', 40, 20, 3, '折冲套装', 400, '23033.jpg', 130, 0, 0, 0, 0, 0, 0, 0, 0, 0);
INSERT INTO `equipment_pre` VALUES (23034, '折冲锴', '折冲锴', 3, 4, '铠甲', 40, 20, 3, '折冲套装', 400, '23034.jpg', 0, 230, 0, 0, 0, 0, 0, 0, 0, 0);
INSERT INTO `equipment_pre` VALUES (23035, '折冲盔', '折冲盔', 3, 5, '头盔', 40, 20, 3, '折冲套装', 400, '23035.jpg', 0, 190, 0, 0, 0, 0, 0, 0, 0, 0);
INSERT INTO `equipment_pre` VALUES (23036, '折冲靴', '折冲靴', 3, 6, '靴子', 40, 20, 3, '折冲套装', 400, '23036.jpg', 0, 150, 0, 0, 0, 0, 0, 0, 0, 0);
INSERT INTO `equipment_pre` VALUES (23041, '骁勇长枪', '骁勇长枪', 3, 1, '武器', 60, 20, 4, '骁勇套装', 500, '23041.jpg', 280, 0, 0, 0, 0, 0, 0, 0, 0, 0);
INSERT INTO `equipment_pre` VALUES (23042, '骁勇戒', '骁勇戒', 3, 2, '戒指', 60, 20, 4, '骁勇套装', 500, '23042.jpg', 220, 0, 0, 0, 0, 0, 0, 0, 0, 0);
INSERT INTO `equipment_pre` VALUES (23043, '骁勇项链', '骁勇项链', 3, 3, '项链', 60, 20, 4, '骁勇套装', 500, '23043.jpg', 160, 0, 0, 0, 0, 0, 0, 0, 0, 0);
INSERT INTO `equipment_pre` VALUES (23044, '骁勇之甲', '骁勇之甲', 3, 4, '铠甲', 60, 20, 4, '骁勇套装', 500, '23044.jpg', 0, 305, 0, 0, 0, 0, 0, 0, 0, 0);
INSERT INTO `equipment_pre` VALUES (23045, '骁勇盔', '骁勇盔', 3, 5, '头盔', 60, 20, 4, '骁勇套装', 500, '23045.jpg', 0, 245, 0, 0, 0, 0, 0, 0, 0, 0);
INSERT INTO `equipment_pre` VALUES (23046, '骁勇靴', '骁勇靴', 3, 6, '靴子', 60, 20, 4, '骁勇套装', 500, '23046.jpg', 0, 190, 0, 0, 0, 0, 0, 0, 0, 0);
INSERT INTO `equipment_pre` VALUES (23051, '破俘枪', '破俘枪', 3, 1, '武器', 80, 20, 5, '破俘套装', 500, '23051.jpg', 335, 0, 0, 0, 0, 0, 0, 0, 0, 0);
INSERT INTO `equipment_pre` VALUES (23052, '破俘戒', '破俘戒', 3, 2, '戒指', 80, 20, 5, '破俘套装', 500, '23052.jpg', 270, 0, 0, 0, 0, 0, 0, 0, 0, 0);
INSERT INTO `equipment_pre` VALUES (23053, '破俘项链', '破俘项链', 3, 3, '项链', 80, 20, 5, '破俘套装', 500, '23053.jpg', 215, 0, 0, 0, 0, 0, 0, 0, 0, 0);
INSERT INTO `equipment_pre` VALUES (23054, '破俘甲', '破俘甲', 3, 4, '铠甲', 80, 20, 5, '破俘套装', 500, '23054.jpg', 0, 390, 0, 0, 0, 0, 0, 0, 0, 0);
INSERT INTO `equipment_pre` VALUES (23055, '破俘盔', '破俘盔', 3, 5, '头盔', 80, 20, 5, '破俘套装', 500, '23055.jpg', 0, 305, 0, 0, 0, 0, 0, 0, 0, 0);
INSERT INTO `equipment_pre` VALUES (23056, '破俘靴', '破俘靴', 3, 6, '靴子', 80, 20, 5, '破俘套装', 500, '23056.jpg', 0, 225, 0, 0, 0, 0, 0, 0, 0, 0);
INSERT INTO `equipment_pre` VALUES (23061, '陷阵之刃', '陷阵之刃', 3, 1, '武器', 40, 20, 6, '陷阵套装', 700, '23061.jpg', 260, 0, 0, 0, 0, 0, 0, 0, 0, 0);
INSERT INTO `equipment_pre` VALUES (23062, '陷阵之戒', '陷阵之戒', 3, 2, '戒指', 40, 20, 6, '陷阵套装', 700, '23062.jpg', 200, 0, 0, 0, 0, 0, 0, 0, 0, 0);
INSERT INTO `equipment_pre` VALUES (23063, '陷阵项链', '陷阵项链', 3, 3, '项链', 40, 20, 6, '陷阵套装', 700, '23063.jpg', 160, 0, 0, 0, 0, 0, 0, 0, 0, 0);
INSERT INTO `equipment_pre` VALUES (23064, '陷阵之锴', '陷阵之锴', 3, 4, '铠甲', 40, 20, 6, '陷阵套装', 700, '23064.jpg', 0, 230, 0, 0, 0, 0, 0, 0, 0, 0);
INSERT INTO `equipment_pre` VALUES (23065, '陷阵之盔', '陷阵之盔', 3, 5, '头盔', 40, 20, 6, '陷阵套装', 700, '23065.jpg', 0, 200, 0, 0, 0, 0, 0, 0, 0, 0);
INSERT INTO `equipment_pre` VALUES (23066, '陷阵之靴', '陷阵之靴', 3, 6, '靴子', 40, 20, 6, '陷阵套装', 700, '23066.jpg', 0, 170, 0, 0, 0, 0, 0, 0, 0, 0);
INSERT INTO `equipment_pre` VALUES (23071, '狂战刀', '狂战刀', 3, 1, '武器', 50, 20, 7, '狂战套装', 900, '23071.jpg', 335, 0, 0, 0, 0, 0, 0, 0, 0, 0);
INSERT INTO `equipment_pre` VALUES (23072, '狂战之戒', '狂战之戒', 3, 2, '戒指', 50, 20, 7, '狂战套装', 900, '23072.jpg', 270, 0, 0, 0, 0, 0, 0, 0, 0, 0);
INSERT INTO `equipment_pre` VALUES (23073, '狂战项链', '狂战项链', 3, 3, '项链', 50, 20, 7, '狂战套装', 900, '23073.jpg', 215, 0, 0, 0, 0, 0, 0, 0, 0, 0);
INSERT INTO `equipment_pre` VALUES (23074, '狂战甲', '狂战甲', 3, 4, '铠甲', 50, 20, 7, '狂战套装', 900, '23074.jpg', 0, 310, 0, 0, 0, 0, 0, 0, 0, 0);
INSERT INTO `equipment_pre` VALUES (23075, '狂战盔', '狂战盔', 3, 5, '头盔', 50, 20, 7, '狂战套装', 900, '23075.jpg', 0, 250, 0, 0, 0, 0, 0, 0, 0, 0);
INSERT INTO `equipment_pre` VALUES (23076, '狂战靴', '狂战靴', 3, 6, '靴子', 50, 20, 7, '狂战套装', 900, '23076.jpg', 0, 200, 0, 0, 0, 0, 0, 0, 0, 0);
INSERT INTO `equipment_pre` VALUES (23081, '天狼剑', '天狼剑', 3, 1, '武器', 60, 20, 8, '天狼套装', 1000, '23081.jpg', 360, 0, 0, 0, 0, 0, 0, 0, 0, 0);
INSERT INTO `equipment_pre` VALUES (23082, '天狼戒', '天狼戒', 3, 2, '戒指', 60, 20, 8, '天狼套装', 1000, '23082.jpg', 290, 0, 0, 0, 0, 0, 0, 0, 0, 0);
INSERT INTO `equipment_pre` VALUES (23083, '天狼项链', '天狼项链', 3, 3, '项链', 60, 20, 8, '天狼套装', 1000, '23083.jpg', 245, 0, 0, 0, 0, 0, 0, 0, 0, 0);
INSERT INTO `equipment_pre` VALUES (23084, '天狼甲', '天狼甲', 3, 4, '铠甲', 60, 20, 8, '天狼套装', 1000, '23084.jpg', 0, 330, 0, 0, 0, 0, 0, 0, 0, 0);
INSERT INTO `equipment_pre` VALUES (23085, '天狼盔', '天狼盔', 3, 5, '头盔', 60, 20, 8, '天狼套装', 1000, '23085.jpg', 0, 265, 0, 0, 0, 0, 0, 0, 0, 0);
INSERT INTO `equipment_pre` VALUES (23086, '天狼靴', '天狼靴', 3, 6, '靴子', 60, 20, 8, '天狼套装', 1000, '23086.jpg', 0, 210, 0, 0, 0, 0, 0, 0, 0, 0);
INSERT INTO `equipment_pre` VALUES (23091, '征戎之刃', '征戎之刃', 3, 1, '武器', 90, 20, 17, '征戎套装', 1000, '23091.jpg', 405, 0, 0, 0, 0, 0, 0, 0, 0, 0);
INSERT INTO `equipment_pre` VALUES (23092, '征戎之戒', '征戎之戒', 3, 2, '戒指', 90, 20, 17, '征戎套装', 1000, '23092.jpg', 330, 0, 0, 0, 0, 0, 0, 0, 0, 0);
INSERT INTO `equipment_pre` VALUES (23093, '征戎项链', '征戎项链', 3, 3, '项链', 90, 20, 17, '征戎套装', 1000, '23093.jpg', 265, 0, 0, 0, 0, 0, 0, 0, 0, 0);
INSERT INTO `equipment_pre` VALUES (23094, '征戎甲', '征戎甲', 3, 4, '铠甲', 90, 20, 17, '征戎套装', 1000, '23094.jpg', 0, 460, 0, 0, 0, 0, 0, 0, 0, 0);
INSERT INTO `equipment_pre` VALUES (23095, '征戎盔', '征戎盔', 3, 5, '头盔', 90, 20, 17, '征戎套装', 1000, '23095.jpg', 0, 335, 0, 0, 0, 0, 0, 0, 0, 0);
INSERT INTO `equipment_pre` VALUES (23096, '征戎靴', '征戎靴', 3, 6, '靴子', 90, 20, 17, '征戎套装', 1000, '23096.jpg', 0, 255, 0, 0, 0, 0, 0, 0, 0, 0);
INSERT INTO `equipment_pre` VALUES (23121, '鹰扬刀', '鹰扬刀', 3, 1, '武器', 40, 20, 12, '鹰扬套装', 1000, '23121.jpg', 310, 0, 0, 0, 0, 0, 0, 0, 0, 0);
INSERT INTO `equipment_pre` VALUES (23122, '鹰扬戒', '鹰扬戒', 3, 2, '戒指', 40, 20, 12, '鹰扬套装', 1000, '23122.jpg', 250, 0, 0, 0, 0, 0, 0, 0, 0, 0);
INSERT INTO `equipment_pre` VALUES (23123, '鹰扬项链', '鹰扬项链', 3, 3, '项链', 40, 20, 12, '鹰扬套装', 1000, '23123.jpg', 210, 0, 0, 0, 0, 0, 0, 0, 0, 0);
INSERT INTO `equipment_pre` VALUES (23124, '鹰扬锴', '鹰扬锴', 3, 4, '铠甲', 40, 20, 12, '鹰扬套装', 1000, '23124.jpg', 0, 300, 0, 0, 0, 0, 0, 0, 0, 0);
INSERT INTO `equipment_pre` VALUES (23125, '鹰扬盔', '鹰扬盔', 3, 5, '头盔', 40, 20, 12, '鹰扬套装', 1000, '23125.jpg', 0, 250, 0, 0, 0, 0, 0, 0, 0, 0);
INSERT INTO `equipment_pre` VALUES (23126, '鹰扬靴', '鹰扬靴', 3, 6, '靴子', 40, 20, 12, '鹰扬套装', 1000, '23126.jpg', 0, 220, 0, 0, 0, 0, 0, 0, 0, 0);
INSERT INTO `equipment_pre` VALUES (24091, '破军之戟', '破军之剑', 4, 1, '武器', 60, 20, 9, '破军套装', 2000, '24091.jpg', 400, 0, 0, 0, 0, 0, 0, 0, 0, 0);
INSERT INTO `equipment_pre` VALUES (24092, '破军之戒', '破军之戒', 4, 2, '戒指', 60, 20, 9, '破军套装', 2000, '24092.jpg', 315, 0, 0, 0, 0, 0, 0, 0, 0, 0);
INSERT INTO `equipment_pre` VALUES (24093, '破军项链', '破军项链', 4, 3, '项链', 60, 20, 9, '破军套装', 2000, '24093.jpg', 260, 0, 0, 0, 0, 0, 0, 0, 0, 0);
INSERT INTO `equipment_pre` VALUES (24094, '破军之锴', '破军之锴', 4, 4, '铠甲', 60, 20, 9, '破军套装', 2000, '24094.jpg', 0, 380, 0, 0, 0, 0, 0, 0, 0, 0);
INSERT INTO `equipment_pre` VALUES (24095, '破军之盔', '破军之盔', 4, 5, '头盔', 60, 20, 9, '破军套装', 2000, '24095.jpg', 0, 295, 0, 0, 0, 0, 0, 0, 0, 0);
INSERT INTO `equipment_pre` VALUES (24096, '破军之靴', '破军之靴', 4, 6, '靴子', 60, 20, 9, '破军套装', 2000, '24096.jpg', 0, 230, 0, 0, 0, 0, 0, 0, 0, 0);
INSERT INTO `equipment_pre` VALUES (24101, '龙威之剑', '龙威剑', 4, 1, '武器', 80, 20, 10, '龙威套装', 3000, '24101.jpg', 460, 0, 0, 0, 0, 0, 0, 0, 0, 0);
INSERT INTO `equipment_pre` VALUES (24102, '龙威之戒', '龙威戒', 4, 2, '戒指', 80, 20, 10, '龙威套装', 3000, '24102.jpg', 400, 0, 0, 0, 0, 0, 0, 0, 0, 0);
INSERT INTO `equipment_pre` VALUES (24103, '龙威项链', '龙威项链', 4, 3, '项链', 80, 20, 10, '龙威套装', 3000, '24103.jpg', 340, 0, 0, 0, 0, 0, 0, 0, 0, 0);
INSERT INTO `equipment_pre` VALUES (24104, '龙威之锴', '龙威锴', 4, 4, '铠甲', 80, 20, 10, '龙威套装', 3000, '24104.jpg', 0, 515, 0, 0, 0, 0, 0, 0, 0, 0);
INSERT INTO `equipment_pre` VALUES (24105, '龙威之盔', '龙威盔', 4, 5, '头盔', 80, 20, 10, '龙威套装', 3000, '24105.jpg', 0, 430, 0, 0, 0, 0, 0, 0, 0, 0);
INSERT INTO `equipment_pre` VALUES (24106, '龙威之靴', '龙威靴', 4, 6, '靴子', 80, 20, 10, '龙威套装', 3000, '24106.jpg', 0, 380, 0, 0, 0, 0, 0, 0, 0, 0);
INSERT INTO `equipment_pre` VALUES (24131, '虎啸之剑', '虎啸之剑', 4, 1, '武器', 60, 20, 13, '虎啸套装', 2000, '24131.jpg', 495, 0, 0, 0, 0, 0, 0, 0, 0, 0);
INSERT INTO `equipment_pre` VALUES (24132, '虎啸之戒', '虎啸戒', 4, 2, '戒指', 60, 20, 13, '虎啸套装', 2000, '24132.jpg', 410, 0, 0, 0, 0, 0, 0, 0, 0, 0);
INSERT INTO `equipment_pre` VALUES (24133, '虎啸项链', '虎啸项链', 4, 3, '项链', 60, 20, 13, '虎啸套装', 2000, '24133.jpg', 360, 0, 0, 0, 0, 0, 0, 0, 0, 0);
INSERT INTO `equipment_pre` VALUES (24134, '虎啸之锴', '虎啸锴', 4, 4, '铠甲', 60, 20, 13, '虎啸套装', 2000, '24134.jpg', 0, 415, 0, 0, 0, 0, 0, 0, 0, 0);
INSERT INTO `equipment_pre` VALUES (24135, '虎啸之盔', '虎啸盔', 4, 5, '头盔', 60, 20, 13, '虎啸套装', 2000, '24135.jpg', 0, 335, 0, 0, 0, 0, 0, 0, 0, 0);
INSERT INTO `equipment_pre` VALUES (24136, '虎啸之靴', '虎啸之靴', 4, 6, '靴子', 60, 20, 13, '虎啸套装', 2000, '24136.jpg', 0, 300, 0, 0, 0, 0, 0, 0, 0, 0);
INSERT INTO `equipment_pre` VALUES (24141, '地煞之枪', '地煞之枪', 4, 1, '武器', 70, 20, 14, '地煞套装', 2000, '24141.jpg', 420, 0, 0, 0, 0, 0, 0, 0, 0, 0);
INSERT INTO `equipment_pre` VALUES (24142, '地煞之戒', '地煞之戒', 4, 2, '戒指', 70, 20, 14, '地煞套装', 2000, '24142.jpg', 330, 0, 0, 0, 0, 0, 0, 0, 0, 0);
INSERT INTO `equipment_pre` VALUES (24143, '地煞项链', '地煞项链', 4, 3, '项链', 70, 20, 14, '地煞套装', 2000, '24143.jpg', 300, 0, 0, 0, 0, 0, 0, 0, 0, 0);
INSERT INTO `equipment_pre` VALUES (24144, '地煞之锴', '地煞之锴', 4, 4, '铠甲', 70, 20, 14, '地煞套装', 2000, '24144.jpg', 0, 400, 0, 0, 0, 0, 0, 0, 0, 0);
INSERT INTO `equipment_pre` VALUES (24145, '地煞之盔', '地煞之盔', 4, 5, '头盔', 70, 20, 14, '地煞套装', 2000, '24145.jpg', 0, 330, 0, 0, 0, 0, 0, 0, 0, 0);
INSERT INTO `equipment_pre` VALUES (24146, '地煞之靴', '地煞之靴', 4, 6, '靴子', 70, 20, 14, '地煞套装', 2000, '24146.jpg', 0, 300, 0, 0, 0, 0, 0, 0, 0, 0);
INSERT INTO `equipment_pre` VALUES (24151, '天诛之刃', '天诛之剑', 4, 1, '武器', 70, 20, 15, '天诛套装', 2000, '24151.jpg', 620, 0, 0, 0, 0, 0, 0, 0, 0, 0);
INSERT INTO `equipment_pre` VALUES (24152, '天诛之戒', '天诛之戒', 4, 2, '戒指', 70, 20, 15, '天诛套装', 2000, '24152.jpg', 500, 0, 0, 0, 0, 0, 0, 0, 0, 0);
INSERT INTO `equipment_pre` VALUES (24153, '天诛项链', '天诛项链', 4, 3, '项链', 70, 20, 15, '天诛套装', 2000, '24153.jpg', 425, 0, 0, 0, 0, 0, 0, 0, 0, 0);
INSERT INTO `equipment_pre` VALUES (24154, '天诛之甲', '天诛之锴', 4, 4, '铠甲', 70, 20, 15, '天诛套装', 2000, '24154.jpg', 0, 375, 0, 0, 0, 0, 0, 0, 0, 0);
INSERT INTO `equipment_pre` VALUES (24155, '天诛之盔', '天诛之盔', 4, 5, '头盔', 70, 20, 15, '天诛套装', 2000, '24155.jpg', 0, 300, 0, 0, 0, 0, 0, 0, 0, 0);
INSERT INTO `equipment_pre` VALUES (24156, '天诛之靴', '天诛之靴', 4, 6, '靴子', 70, 20, 15, '天诛套装', 2000, '24156.jpg', 0, 250, 0, 0, 0, 0, 0, 0, 0, 0);
INSERT INTO `equipment_pre` VALUES (24161, '幽冥之剑', '幽冥之剑', 4, 1, '武器', 70, 20, 16, '幽冥套装', 2000, '24161.jpg', 420, 0, 0, 0, 0, 0, 0, 0, 0, 0);
INSERT INTO `equipment_pre` VALUES (24162, '幽冥之戒', '幽冥之戒', 4, 2, '戒指', 70, 20, 16, '幽冥套装', 2000, '24162.jpg', 330, 0, 0, 0, 0, 0, 0, 0, 0, 0);
INSERT INTO `equipment_pre` VALUES (24163, '幽冥项链', '幽冥项链', 4, 3, '项链', 70, 20, 16, '幽冥套装', 2000, '24163.jpg', 300, 0, 0, 0, 0, 0, 0, 0, 0, 0);
INSERT INTO `equipment_pre` VALUES (24164, '幽冥之甲', '幽冥之锴', 4, 4, '铠甲', 70, 20, 16, '幽冥套装', 2000, '24164.jpg', 0, 400, 0, 0, 0, 0, 0, 0, 0, 0);
INSERT INTO `equipment_pre` VALUES (24165, '幽冥之盔', '幽冥之盔', 4, 5, '头盔', 70, 20, 16, '幽冥套装', 2000, '24165.jpg', 0, 330, 0, 0, 0, 0, 0, 0, 0, 0);
INSERT INTO `equipment_pre` VALUES (24166, '幽冥之靴', '幽冥之靴', 4, 6, '靴子', 70, 20, 16, '幽冥套装', 2000, '24166.jpg', 0, 300, 0, 0, 0, 0, 0, 0, 0, 0);
INSERT INTO `equipment_pre` VALUES (25111, '战神之戟', '战神之戟', 5, 1, '武器', 80, 20, 11, '战神套装', 5000, '25111.jpg', 625, 0, 0, 0, 0, 0, 0, 0, 0, 0);
INSERT INTO `equipment_pre` VALUES (25112, '战神之戒', '战神之戒', 5, 2, '戒指', 80, 20, 11, '战神套装', 5000, '25112.jpg', 510, 0, 0, 0, 0, 0, 0, 0, 0, 0);
INSERT INTO `equipment_pre` VALUES (25113, '战神项链', '战神项链', 5, 3, '项链', 80, 20, 11, '战神套装', 5000, '25113.jpg', 410, 0, 0, 0, 0, 0, 0, 0, 0, 0);
INSERT INTO `equipment_pre` VALUES (25114, '战神之甲', '战神之甲', 5, 4, '铠甲', 80, 20, 11, '战神套装', 5000, '25114.jpg', 0, 655, 0, 0, 0, 0, 0, 0, 0, 0);
INSERT INTO `equipment_pre` VALUES (25115, '战神之盔', '战神之盔', 5, 5, '头盔', 80, 20, 11, '战神套装', 5000, '25115.jpg', 0, 530, 0, 0, 0, 0, 0, 0, 0, 0);
INSERT INTO `equipment_pre` VALUES (25116, '战神之靴', '战神之靴', 5, 6, '靴子', 80, 20, 11, '战神套装', 5000, '25116.jpg', 0, 420, 0, 0, 0, 0, 0, 0, 0, 0);
INSERT INTO `equipment_pre` VALUES (25181, '诛邪之刃', '诛邪之剑', 5, 1, '武器', 90, 20, 18, '诛邪套装', 2000, '25181.jpg', 735, 0, 0, 0, 0, 0, 0, 0, 0, 0);
INSERT INTO `equipment_pre` VALUES (25182, '诛邪之戒', '诛邪之戒', 5, 2, '戒指', 90, 20, 18, '诛邪套装', 2000, '25182.jpg', 620, 0, 0, 0, 0, 0, 0, 0, 0, 0);
INSERT INTO `equipment_pre` VALUES (25183, '诛邪项链', '诛邪项链', 5, 3, '项链', 90, 20, 18, '诛邪套装', 2000, '25183.jpg', 500, 0, 0, 0, 0, 0, 0, 0, 0, 0);
INSERT INTO `equipment_pre` VALUES (25184, '诛邪之甲', '诛邪之锴', 5, 4, '铠甲', 90, 20, 18, '诛邪套装', 2000, '25184.jpg', 0, 755, 0, 0, 0, 0, 0, 0, 0, 0);
INSERT INTO `equipment_pre` VALUES (25185, '诛邪之盔', '诛邪之盔', 5, 5, '头盔', 90, 20, 18, '诛邪套装', 2000, '25185.jpg', 0, 630, 0, 0, 0, 0, 0, 0, 0, 0);
INSERT INTO `equipment_pre` VALUES (25186, '诛邪之靴', '诛邪之靴', 5, 6, '靴子', 90, 20, 18, '诛邪套装', 2000, '25186.jpg', 0, 520, 0, 0, 0, 0, 0, 0, 0, 0);

-- 共插入 132 条装备模板

-- =============================================
-- suit_config 套装效果配置表
-- =============================================

DROP TABLE IF EXISTS `suit_config`;
CREATE TABLE `suit_config` (
  `id` INT NOT NULL COMMENT '套装ID',
  `name` VARCHAR(30) NOT NULL COMMENT '套装名称',
  `color` TINYINT NOT NULL COMMENT '品质',
  `level` INT NOT NULL DEFAULT 0 COMMENT '套装推荐等级',
  `gen_att` INT NOT NULL DEFAULT 0 COMMENT '套装加成-武将攻击',
  `gen_def` INT NOT NULL DEFAULT 0 COMMENT '套装加成-武将防御',
  `gen_for` INT NOT NULL DEFAULT 0 COMMENT '套装加成-武勇',
  `gen_leader` INT NOT NULL DEFAULT 0 COMMENT '套装加成-统御',
  `army_life` INT NOT NULL DEFAULT 0 COMMENT '套装加成-军队生命',
  `army_att` INT NOT NULL DEFAULT 0 COMMENT '套装加成-军队攻击',
  `army_def` INT NOT NULL DEFAULT 0 COMMENT '套装加成-军队防御',
  `army_sp` INT NOT NULL DEFAULT 0 COMMENT '套装加成-军队速度',
  `army_hit` INT NOT NULL DEFAULT 0 COMMENT '套装加成-命中',
  `army_mis` INT NOT NULL DEFAULT 0 COMMENT '套装加成-闪避',
  `describe_text` VARCHAR(100) DEFAULT NULL COMMENT '套装描述',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='套装效果配置表(APK数据)';

INSERT INTO `suit_config` VALUES (2, '宣武套装', 3, 20, 60, 0, 0, 0, 20, 0, 0, 0, 0, 0, '宣武套装');
INSERT INTO `suit_config` VALUES (3, '折冲套装', 3, 30, 100, 0, 0, 0, 30, 0, 0, 0, 0, 0, '折冲套装');
INSERT INTO `suit_config` VALUES (4, '骁勇套装', 3, 40, 140, 0, 0, 0, 40, 0, 0, 0, 0, 0, '骁勇套装');
INSERT INTO `suit_config` VALUES (5, '破俘套装', 3, 55, 180, 0, 0, 0, 50, 0, 0, 0, 0, 0, '破俘套装');
INSERT INTO `suit_config` VALUES (6, '陷阵套装', 3, 50, 200, 0, 10, 0, 0, 0, 0, 0, 0, 0, '陷阵套装');
INSERT INTO `suit_config` VALUES (7, '狂战套装', 3, 60, 260, 0, 20, 0, 0, 0, 0, 0, 0, 0, '狂战套装');
INSERT INTO `suit_config` VALUES (8, '天狼套装', 3, 70, 500, 0, 20, 0, 0, 0, 0, 0, 0, 0, '天狼套装');
INSERT INTO `suit_config` VALUES (9, '破军套装', 4, 80, 550, 0, 20, 0, 0, 0, 0, 0, 0, 0, '破军套装');
INSERT INTO `suit_config` VALUES (10, '龙威套装', 4, 90, 550, 0, 20, 20, 0, 0, 0, 0, 0, 0, '龙威套装');
INSERT INTO `suit_config` VALUES (11, '战神套装', 5, 99, 600, 0, 20, 20, 0, 0, 0, 0, 0, 0, '战神套装');
INSERT INTO `suit_config` VALUES (12, '鹰扬套装', 3, 75, 400, 0, 0, 20, 0, 0, 0, 0, 0, 0, '鹰扬套装');
INSERT INTO `suit_config` VALUES (13, '虎啸套装', 4, 85, 0, 400, 0, 20, 0, 0, 0, 0, 0, 10, '虎啸套装');
INSERT INTO `suit_config` VALUES (14, '地煞套装', 4, 82, 200, 240, 0, 0, 0, 0, 0, 0, 0, 5, '地煞套装');
INSERT INTO `suit_config` VALUES (15, '天诛套装', 4, 98, 900, 0, 0, 0, 0, 0, 0, 0, 0, 0, '天诛套装');
INSERT INTO `suit_config` VALUES (16, '幽冥套装', 4, 83, 240, 200, 0, 0, 0, 0, 0, 5, 0, 0, '幽冥套装');
INSERT INTO `suit_config` VALUES (17, '征戎套装', 3, 62, 280, 0, 0, 0, 80, 0, 0, 0, 0, 0, '征戎套装');
INSERT INTO `suit_config` VALUES (18, '诛邪套装', 5, 100, 700, 0, 25, 25, 0, 0, 0, 0, 0, 0, '诛邪套装');

-- 共插入 17 条套装配置

-- 套装合成费用配置
DROP TABLE IF EXISTS `suit_fuse_cost`;
CREATE TABLE `suit_fuse_cost` (
  `color` TINYINT NOT NULL COMMENT '品质等级',
  `cost` INT NOT NULL COMMENT '合成费用(白银)',
  PRIMARY KEY (`color`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='套装合成费用';

INSERT INTO `suit_fuse_cost` VALUES (2, 5000);
INSERT INTO `suit_fuse_cost` VALUES (3, 10000);
INSERT INTO `suit_fuse_cost` VALUES (4, 30000);
INSERT INTO `suit_fuse_cost` VALUES (5, 100000);

-- 可选: 删除旧备份表
-- DROP TABLE IF EXISTS equipment_pre_old;
