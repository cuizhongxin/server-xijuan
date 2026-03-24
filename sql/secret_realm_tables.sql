-- =====================================================
-- 秘境系统数据表 - 完整建表 + 数据插入
-- =====================================================

-- 1. 秘境配置表
DROP TABLE IF EXISTS `secret_realm_config`;
CREATE TABLE `secret_realm_config` (
  `id` VARCHAR(32) NOT NULL COMMENT '秘境ID',
  `name` VARCHAR(64) NOT NULL COMMENT '秘境名称',
  `description` TEXT COMMENT '秘境描述',
  `min_level` INT NOT NULL DEFAULT 1 COMMENT '解锁等级',
  `cost_gold` INT NOT NULL DEFAULT 10 COMMENT '单次探索费用(黄金)',
  `equip_set_name` VARCHAR(32) DEFAULT NULL COMMENT '产出套装名称',
  `equip_base_rate` DECIMAL(5,4) NOT NULL DEFAULT 0.0800 COMMENT '装备基础掉落概率(0~1)',
  `pity_count` INT NOT NULL DEFAULT 50 COMMENT '保底次数(每X次必出一件装备)',
  `daily_limit` INT NOT NULL DEFAULT 0 COMMENT '每日探索次数上限(0=不限)',
  `sort_order` INT NOT NULL DEFAULT 0 COMMENT '排序',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 1启用 0禁用',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='秘境配置表';

-- 2. 秘境奖励表(装备+道具)
DROP TABLE IF EXISTS `secret_realm_reward`;
CREATE TABLE `secret_realm_reward` (
  `id` INT AUTO_INCREMENT COMMENT '自增ID',
  `realm_id` VARCHAR(32) NOT NULL COMMENT '所属秘境ID',
  `reward_type` VARCHAR(16) NOT NULL COMMENT '奖励类型: equipment=装备, item=道具',
  `item_id` VARCHAR(64) NOT NULL COMMENT '物品标识(字符串,前端使用)',
  `name` VARCHAR(64) NOT NULL COMMENT '物品名称',
  `icon` VARCHAR(32) DEFAULT '📦' COMMENT '图标(emoji)',
  `item_sub_type` VARCHAR(16) DEFAULT NULL COMMENT '道具子类型: material/consumable (仅道具有效)',
  `quality` INT NOT NULL DEFAULT 1 COMMENT '品质: 1白 2绿 3蓝 4红 5紫 6橙',
  `drop_weight` INT NOT NULL DEFAULT 100 COMMENT '掉落权重(越大越容易掉)',
  `equip_pre_id` INT DEFAULT 0 COMMENT '关联equipment_pre表ID(仅装备有效)',
  `item_pre_id` INT DEFAULT 0 COMMENT '关联item表item_id(仅道具有效)',
  `position` VARCHAR(16) DEFAULT NULL COMMENT '装备部位: 武器/戒指/铠甲/项链/头盔/鞋子',
  `set_name` VARCHAR(32) DEFAULT NULL COMMENT '套装名称',
  `set_effect_3` VARCHAR(128) DEFAULT NULL COMMENT '3件套效果',
  `set_effect_6` VARCHAR(128) DEFAULT NULL COMMENT '6件套效果',
  `attack` INT NOT NULL DEFAULT 0 COMMENT '攻击力',
  `defense` INT NOT NULL DEFAULT 0 COMMENT '防御力',
  `soldier_hp` INT NOT NULL DEFAULT 0 COMMENT '士兵生命',
  `mobility` INT NOT NULL DEFAULT 0 COMMENT '机动力',
  `description` VARCHAR(256) DEFAULT NULL COMMENT '物品说明',
  `sort_order` INT NOT NULL DEFAULT 0 COMMENT '排序(影响网格展示顺序)',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 1启用 0禁用',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  INDEX `idx_realm_id` (`realm_id`),
  INDEX `idx_realm_type` (`realm_id`, `reward_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='秘境奖励配置表';

-- 3. 秘境保底计数表(每用户每秘境)
DROP TABLE IF EXISTS `secret_realm_pity`;
CREATE TABLE `secret_realm_pity` (
  `id` BIGINT AUTO_INCREMENT COMMENT '自增ID',
  `user_id` VARCHAR(64) NOT NULL COMMENT '用户ID',
  `realm_id` VARCHAR(32) NOT NULL COMMENT '秘境ID',
  `count_since_equip` INT NOT NULL DEFAULT 0 COMMENT '距上次出装备的探索次数(保底计数器)',
  `total_explore_count` INT NOT NULL DEFAULT 0 COMMENT '累计探索总次数',
  `total_equip_count` INT NOT NULL DEFAULT 0 COMMENT '累计获得装备数',
  `last_equip_time` BIGINT DEFAULT 0 COMMENT '上次获得装备的时间戳(ms)',
  `daily_count` INT NOT NULL DEFAULT 0 COMMENT '今日已探索次数',
  `daily_reset_date` DATE DEFAULT NULL COMMENT '每日计数重置日期',
  `update_time` BIGINT DEFAULT 0 COMMENT '更新时间戳(ms)',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_realm` (`user_id`, `realm_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='秘境保底计数表';


-- =====================================================
-- 秘境配置数据
-- =====================================================

INSERT INTO `secret_realm_config` (`id`, `name`, `description`, `min_level`, `cost_gold`, `equip_set_name`, `equip_base_rate`, `pity_count`, `daily_limit`, `sort_order`, `status`) VALUES
('penglai', '蓬莱秘宝', '东海之中一座传说，这里蕴藏着无尽的灵气和极大宝藏，让在此进行探索的人满载而归。', 40, 10, '鹰扬', 0.0800, 50, 0, 1, 1),
('kunlun', '昆仑秘宝', '昆仑仙山，瑶草遍地，隐藏上古宝藏，传说中的不死之药就在此处。',         60, 20, '虎啸', 0.1000, 40, 0, 2, 1);


-- =====================================================
-- 蓬莱秘宝(Lv.40) - 鹰扬套装备(6件) + 道具(6件)
-- 装备: equip_pre_id 关联 equipment_pre 表
-- 道具: item_pre_id 关联 item 表
-- =====================================================

-- 鹰扬套装备 (equip_pre_id 25~30, 品质3蓝, reward_type=equipment, 图标使用APK装备图片)
INSERT INTO `secret_realm_reward` (`realm_id`,`reward_type`,`item_id`,`name`,`icon`,`item_sub_type`,`quality`,`drop_weight`,`equip_pre_id`,`item_pre_id`,`position`,`set_name`,`set_effect_3`,`set_effect_6`,`attack`,`defense`,`soldier_hp`,`mobility`,`description`,`sort_order`) VALUES
('penglai','equipment','equip_25','鹰扬战刀','images/equip/23021.jpg',NULL,3,100,25,0,'武器','鹰扬','攻击+400','统帅+20',110,0,0,0,'鹰扬套·武器，蓬莱秘境专属产出',1),
('penglai','equipment','equip_26','鹰扬戒指','images/equip/23022.jpg',NULL,3,100,26,0,'戒指','鹰扬','攻击+400','统帅+20',35,0,0,0,'鹰扬套·戒指，蓬莱秘境专属产出',2),
('penglai','equipment','equip_27','鹰扬护甲','images/equip/23024.jpg',NULL,3,100,27,0,'铠甲','鹰扬','攻击+400','统帅+20',0,200,0,0,'鹰扬套·铠甲，蓬莱秘境专属产出',3),
('penglai','equipment','equip_28','鹰扬项链','images/equip/23023.jpg',NULL,3,100,28,0,'项链','鹰扬','攻击+400','统帅+20',0,0,500,0,'鹰扬套·项链，蓬莱秘境专属产出',4),
('penglai','equipment','equip_29','鹰扬头盔','images/equip/23025.jpg',NULL,3,100,29,0,'头盔','鹰扬','攻击+400','统帅+20',0,70,0,0,'鹰扬套·头盔，蓬莱秘境专属产出',5),
('penglai','equipment','equip_30','鹰扬战靴','images/equip/23026.jpg',NULL,3,100,30,0,'鞋子','鹰扬','攻击+400','统帅+20',0,0,0,50,'鹰扬套·鞋子，蓬莱秘境专属产出',6);

-- 蓬莱道具(6件, reward_type=item, 图标使用APK道具图片, 最后一项为特训符)
INSERT INTO `secret_realm_reward` (`realm_id`,`reward_type`,`item_id`,`name`,`icon`,`item_sub_type`,`quality`,`drop_weight`,`equip_pre_id`,`item_pre_id`,`description`,`sort_order`) VALUES
('penglai','item','item_12','银锭',      'images/item/11012.jpg','material',   3, 250, 0, 12, '珍贵的金属材料，可用于制作或出售',               7),
('penglai','item','item_2', '4级强化石',  'images/item/14004.jpg','material',   3, 200, 0,  2, '用于强化装备，可提升装备属性',                   8),
('penglai','item','item_28','经验丹(小)', 'images/item/11042.jpg','consumable', 2, 300, 0, 28, '使用后可获得少量经验值',                         9),
('penglai','item','15012',  '中级招贤令', 'images/item/15012.jpg','consumable', 4,  80, 0, 15012, '用于招募武将，有概率获得蓝色品质武将',           10),
('penglai','item','item_11','中级合成符', 'images/item/15002.jpg','material',   3, 200, 0, 11, '用于合成更高级装备的材料',                       11),
('penglai','item','item_42','特训符',     'images/item/15042.jpg','consumable', 3, 200, 0, 42, '用于武将特训，提升武将属性',                     12);


-- =====================================================
-- 昆仑秘宝(Lv.60) - 虎啸套装备(6件) + 道具(6件)
-- =====================================================

-- 虎啸套装备 (equip_pre_id 49~54, 品质5紫, reward_type=equipment, 图标使用APK装备图片)
INSERT INTO `secret_realm_reward` (`realm_id`,`reward_type`,`item_id`,`name`,`icon`,`item_sub_type`,`quality`,`drop_weight`,`equip_pre_id`,`item_pre_id`,`position`,`set_name`,`set_effect_3`,`set_effect_6`,`attack`,`defense`,`soldier_hp`,`mobility`,`description`,`sort_order`) VALUES
('kunlun','equipment','equip_49','虎啸战刀','images/equip/24091.jpg',NULL,5,100,49,0,'武器','虎啸','防御+400','统帅+20，闪避+10',200,0,0,0,'虎啸套·武器，昆仑秘境专属产出',1),
('kunlun','equipment','equip_50','虎啸戒指','images/equip/24092.jpg',NULL,5,100,50,0,'戒指','虎啸','防御+400','统帅+20，闪避+10',65,0,0,0,'虎啸套·戒指，昆仑秘境专属产出',2),
('kunlun','equipment','equip_51','虎啸护甲','images/equip/24094.jpg',NULL,5,100,51,0,'铠甲','虎啸','防御+400','统帅+20，闪避+10',0,500,0,0,'虎啸套·铠甲，昆仑秘境专属产出',3),
('kunlun','equipment','equip_52','虎啸项链','images/equip/24093.jpg',NULL,5,100,52,0,'项链','虎啸','防御+400','统帅+20，闪避+10',0,0,1000,0,'虎啸套·项链，昆仑秘境专属产出',4),
('kunlun','equipment','equip_53','虎啸头盔','images/equip/24095.jpg',NULL,5,100,53,0,'头盔','虎啸','防御+400','统帅+20，闪避+10',0,170,0,0,'虎啸套·头盔，昆仑秘境专属产出',5),
('kunlun','equipment','equip_54','虎啸战靴','images/equip/24096.jpg',NULL,5,100,54,0,'鞋子','虎啸','防御+400','统帅+20，闪避+10',0,0,0,85,'虎啸套·鞋子，昆仑秘境专属产出',6);

-- 昆仑道具(6件, reward_type=item, 图标使用APK道具图片)
INSERT INTO `secret_realm_reward` (`realm_id`,`reward_type`,`item_id`,`name`,`icon`,`item_sub_type`,`quality`,`drop_weight`,`equip_pre_id`,`item_pre_id`,`description`,`sort_order`) VALUES
('kunlun','item','item_13','银砖',       'images/item/11013.jpg','material',   5, 100, 0, 13, '高级金属材料，可用于高级制作',                   7),
('kunlun','item','item_38','5级强化石',   'images/item/14005.jpg','material',   4, 180, 0, 38, '用于强化高级装备',                               8),
('kunlun','item','item_29','经验丹(中)',  'images/item/11043.jpg','consumable', 3, 250, 0, 29, '使用后可获得中等经验值',                         9),
('kunlun','item','15013',  '高级招贤令',  'images/item/15013.jpg','consumable', 5,  60, 0, 15013, '用于招募武将，有概率获得紫色品质武将',           10),
('kunlun','item','item_5', '精炼石',      'images/item/14031.jpg','material',   3, 200, 0,  5, '用于精炼装备，提升装备品质等级',                 11),
('kunlun','item','item_24','中级精力丹',  'images/item/11101.jpg','consumable', 3, 250, 0, 24, '使用后恢复中等精力值',                           12);


-- 瑶池秘宝和九天秘宝暂未开放
