-- =============================================
-- 三国策略游戏 - 数据库建表脚本
-- =============================================

-- 原有的 Counters 表
CREATE TABLE IF NOT EXISTS `Counters` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '自增主键',
  `count` int(11) NOT NULL DEFAULT '1' COMMENT '计数值',
  `createdAt` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updatedAt` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8 COMMENT='计数器表';

-- =============================================
-- 1. 用户ID映射表（openId <-> userId）
-- =============================================
CREATE TABLE IF NOT EXISTS `user_id_mapping` (
  `open_id` VARCHAR(128) NOT NULL COMMENT '微信openId',
  `user_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '游戏内用户ID（自增）',
  PRIMARY KEY (`open_id`),
  UNIQUE KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COMMENT='用户ID映射表';

-- =============================================
-- 2. 用户资源表
-- =============================================
CREATE TABLE IF NOT EXISTS `user_resource` (
  `od_user_id` VARCHAR(64) NOT NULL COMMENT '用户唯一标识',
  `id` VARCHAR(64) COMMENT '资源记录ID',
  `gold` BIGINT DEFAULT 0 COMMENT '黄金（元宝，充值货币）',
  `silver` BIGINT DEFAULT 0 COMMENT '白银（基础货币）',
  `diamond` BIGINT DEFAULT 0 COMMENT '钻石（高级货币）',
  `level` INT DEFAULT 1 COMMENT '主公等级',
  `stamina` INT DEFAULT 100 COMMENT '当前体力',
  `max_stamina` INT DEFAULT 100 COMMENT '体力上限',
  `general_order` INT DEFAULT 10 COMMENT '当前将令',
  `max_general_order` INT DEFAULT 10 COMMENT '将令上限',
  `tiger_tally` INT DEFAULT 10 COMMENT '虎符（出征令牌）',
  `wood` BIGINT DEFAULT 0 COMMENT '木材',
  `metal` BIGINT DEFAULT 0 COMMENT '金属',
  `food` BIGINT DEFAULT 0 COMMENT '粮食',
  `paper` BIGINT DEFAULT 0 COMMENT '纸张',
  `enhance_stone1` INT DEFAULT 0 COMMENT '1级强化石',
  `enhance_stone2` INT DEFAULT 0 COMMENT '2级强化石',
  `enhance_stone3` INT DEFAULT 0 COMMENT '3级强化石',
  `enhance_stone4` INT DEFAULT 0 COMMENT '4级强化石',
  `enhance_stone5` INT DEFAULT 0 COMMENT '5级强化石',
  `enhance_stone6` INT DEFAULT 0 COMMENT '6级强化石',
  `enhance_scroll_basic` INT DEFAULT 0 COMMENT '初级强化卷轴',
  `enhance_scroll_medium` INT DEFAULT 0 COMMENT '中级强化卷轴',
  `enhance_scroll_advanced` INT DEFAULT 0 COMMENT '高级强化卷轴',
  `merge_scroll` INT DEFAULT 0 COMMENT '合成卷轴',
  `quality_stone` INT DEFAULT 0 COMMENT '品质石',
  `basic_food` INT DEFAULT 0 COMMENT '初级粮食包',
  `advanced_food` INT DEFAULT 0 COMMENT '中级粮食包',
  `premium_food` INT DEFAULT 0 COMMENT '高级粮食包',
  `normal_recruit_token` INT DEFAULT 0 COMMENT '普通招贤令',
  `advanced_recruit_token` INT DEFAULT 0 COMMENT '高级招贤令',
  `junior_token` INT DEFAULT 0 COMMENT '初级声望符',
  `intermediate_token` INT DEFAULT 0 COMMENT '中级声望符',
  `senior_token` INT DEFAULT 0 COMMENT '高级声望符',
  `last_claim_date` VARCHAR(16) DEFAULT '' COMMENT '上次领取日期',
  `daily_token_claimed` INT DEFAULT 0 COMMENT '今日已领取令牌数',
  `rank` VARCHAR(16) DEFAULT '白身' COMMENT '爵位名称',
  `fame` BIGINT DEFAULT 0 COMMENT '声望值',
  `general_count` INT DEFAULT 0 COMMENT '当前武将数量',
  `base_general_slots` INT DEFAULT 10 COMMENT '基础武将槽位数',
  `purchased_slots` INT DEFAULT 0 COMMENT '已购买额外槽位数',
  `vip_bonus_slots` INT DEFAULT 0 COMMENT 'VIP赠送槽位数',
  `max_general` INT DEFAULT 10 COMMENT '武将上限',
  `vip_level` INT DEFAULT 0 COMMENT 'VIP等级',
  `vip_exp` BIGINT DEFAULT 0 COMMENT 'VIP经验值',
  `total_recharge` BIGINT DEFAULT 0 COMMENT '累计充值金额（分）',
  `create_time` BIGINT COMMENT '创建时间戳',
  `update_time` BIGINT COMMENT '更新时间戳',
  `last_stamina_recover_time` BIGINT COMMENT '上次体力恢复时间戳',
  PRIMARY KEY (`od_user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户资源表';

-- =============================================
-- 3. 用户等级表
-- =============================================
CREATE TABLE IF NOT EXISTS `user_level` (
  `user_id` VARCHAR(64) NOT NULL COMMENT '用户唯一标识',
  `level` INT DEFAULT 1 COMMENT '当前等级',
  `total_exp` BIGINT DEFAULT 0 COMMENT '累计经验值',
  `current_level_exp` BIGINT DEFAULT 0 COMMENT '当前等级已获得经验',
  `exp_to_next_level` BIGINT DEFAULT 100 COMMENT '升级所需经验',
  `vip_level` INT DEFAULT 0 COMMENT 'VIP等级',
  `today_exp` BIGINT DEFAULT 0 COMMENT '今日获得经验',
  `last_update_date` VARCHAR(8) DEFAULT '' COMMENT '上次更新日期(yyyyMMdd)',
  `create_time` BIGINT COMMENT '创建时间戳',
  `update_time` BIGINT COMMENT '更新时间戳',
  PRIMARY KEY (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户等级表';

-- =============================================
-- 4. 用户材料表
-- =============================================
CREATE TABLE IF NOT EXISTS `user_material` (
  `user_id` VARCHAR(64) NOT NULL COMMENT '用户唯一标识',
  `material_id` VARCHAR(64) NOT NULL COMMENT '材料ID',
  `count` INT NOT NULL DEFAULT 0 COMMENT '持有数量',
  PRIMARY KEY (`user_id`, `material_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户材料表';

-- =============================================
-- 5. 武将模板表（招募池配置）
-- =============================================
CREATE TABLE IF NOT EXISTS `general_template` (
  `id` VARCHAR(64) NOT NULL COMMENT '模板ID，如 guanyu/zhangfei',
  `name` VARCHAR(64) NOT NULL COMMENT '武将名称',
  `avatar` VARCHAR(128) COMMENT '头像资源路径',
  `faction` VARCHAR(16) COMMENT '阵营：魏/蜀/吴/群/虚构',
  `quality_id` INT DEFAULT 1 COMMENT '品质ID：1白2绿3蓝4紫5橙',
  `quality_name` VARCHAR(16) COMMENT '品质名称',
  `quality_color` VARCHAR(16) COMMENT '品质颜色代码',
  `quality_star` INT DEFAULT 1 COMMENT '星级1-5',
  `troop_type` VARCHAR(8) NOT NULL COMMENT '兵种：步/骑/弓',
  `base_attack` INT DEFAULT 0 COMMENT '基础攻击',
  `base_defense` INT DEFAULT 0 COMMENT '基础防御',
  `base_valor` INT DEFAULT 0 COMMENT '基础武勇',
  `base_command` INT DEFAULT 0 COMMENT '基础统御',
  `base_dodge` DOUBLE DEFAULT 0 COMMENT '基础闪避',
  `base_mobility` INT DEFAULT 0 COMMENT '基础机动',
  `growth_attack` DOUBLE DEFAULT 1.0 COMMENT '攻击成长系数',
  `growth_defense` DOUBLE DEFAULT 1.0 COMMENT '防御成长系数',
  `growth_valor` DOUBLE DEFAULT 1.0 COMMENT '武勇成长系数',
  `growth_command` DOUBLE DEFAULT 1.0 COMMENT '统御成长系数',
  `description` VARCHAR(512) COMMENT '武将描述',
  `recruit_pool` VARCHAR(16) DEFAULT 'normal' COMMENT '招募池：normal/advanced/special',
  `enabled` TINYINT(1) DEFAULT 1 COMMENT '是否启用',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='武将模板表';

-- =============================================
-- 5.1 武将模板特征表
-- =============================================
CREATE TABLE IF NOT EXISTS `general_template_trait` (
  `id` BIGINT AUTO_INCREMENT COMMENT '自增主键',
  `template_id` VARCHAR(64) NOT NULL COMMENT '关联武将模板ID',
  `trait_name` VARCHAR(64) NOT NULL COMMENT '特征名称',
  `sort_order` INT DEFAULT 0 COMMENT '排序序号',
  PRIMARY KEY (`id`),
  KEY `idx_gtt_template` (`template_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='武将模板特征表';

-- =============================================
-- 6. 武将表（玩家拥有的武将实例）
-- =============================================
CREATE TABLE IF NOT EXISTS `general` (
  `id` VARCHAR(64) NOT NULL COMMENT '武将实例ID',
  `user_id` VARCHAR(64) NOT NULL COMMENT '所属用户ID',
  `template_id` VARCHAR(64) COMMENT '关联武将模板ID',
  `name` VARCHAR(64) COMMENT '武将名称',
  `level` INT DEFAULT 1 COMMENT '当前等级',
  `exp` BIGINT DEFAULT 0 COMMENT '当前经验值',
  `max_exp` BIGINT DEFAULT 100 COMMENT '升级所需经验',
  `avatar` VARCHAR(128) COMMENT '头像资源路径',
  `faction` VARCHAR(16) COMMENT '阵营：魏/蜀/吴/群/虚构',
  `quality_id` INT COMMENT '品质ID：1白2绿3蓝4紫5橙',
  `quality_name` VARCHAR(16) COMMENT '品质名称',
  `quality_color` VARCHAR(16) COMMENT '品质颜色代码',
  `quality_base_multiplier` DOUBLE DEFAULT 1.0 COMMENT '品质属性倍率',
  `quality_star` INT DEFAULT 1 COMMENT '星级1-5',
  `quality_icon` VARCHAR(128) COMMENT '品质图标',
  `troop_type` VARCHAR(8) COMMENT '兵种：步/骑/弓',
  `attr_attack` INT DEFAULT 0 COMMENT '攻击',
  `attr_defense` INT DEFAULT 0 COMMENT '防御',
  `attr_valor` INT DEFAULT 0 COMMENT '武勇',
  `attr_command` INT DEFAULT 0 COMMENT '统御',
  `attr_dodge` DOUBLE DEFAULT 0 COMMENT '闪避',
  `attr_mobility` INT DEFAULT 0 COMMENT '机动',
  `soldier_rank` INT DEFAULT 1 COMMENT '兵种等级',
  `soldier_count` INT DEFAULT 100 COMMENT '当前士兵数',
  `soldier_max_count` INT DEFAULT 100 COMMENT '士兵上限',
  `equip_weapon_id` VARCHAR(64) COMMENT '武器装备ID',
  `equip_armor_id` VARCHAR(64) COMMENT '铠甲装备ID',
  `equip_necklace_id` VARCHAR(64) COMMENT '项链装备ID',
  `equip_ring_id` VARCHAR(64) COMMENT '戒指装备ID',
  `equip_shoes_id` VARCHAR(64) COMMENT '鞋子装备ID',
  `equip_helmet_id` VARCHAR(64) COMMENT '头盔装备ID',
  `tactics_id` VARCHAR(64) COMMENT '已装备的兵法ID（单槽）',
  `status_locked` TINYINT(1) DEFAULT 0 COMMENT '是否锁定',
  `status_in_battle` TINYINT(1) DEFAULT 0 COMMENT '是否出征中',
  `status_injured` TINYINT(1) DEFAULT 0 COMMENT '是否受伤',
  `status_morale` INT DEFAULT 100 COMMENT '士气0-100',
  `stat_total_battles` INT DEFAULT 0 COMMENT '总战斗次数',
  `stat_victories` INT DEFAULT 0 COMMENT '胜利次数',
  `stat_defeats` INT DEFAULT 0 COMMENT '失败次数',
  `stat_kills` INT DEFAULT 0 COMMENT '击杀数',
  `stat_mvp_count` INT DEFAULT 0 COMMENT 'MVP次数',
  `traits_json` TEXT COMMENT '特征列表JSON',
  `equipment_bonus_json` TEXT COMMENT '装备加成缓存JSON',
  `create_time` BIGINT COMMENT '创建时间戳',
  `update_time` BIGINT COMMENT '更新时间戳',
  PRIMARY KEY (`id`),
  KEY `idx_general_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='武将实例表';

-- =============================================
-- 7. 兵法模板表（所有兵法配置）
-- =============================================
CREATE TABLE IF NOT EXISTS `tactics_template` (
  `id` VARCHAR(64) NOT NULL COMMENT '兵法ID',
  `name` VARCHAR(64) NOT NULL COMMENT '兵法名称',
  `description` VARCHAR(512) COMMENT '兵法描述',
  `icon` VARCHAR(128) COMMENT '图标资源路径',
  `quality` INT DEFAULT 1 COMMENT '品质：1白2绿3蓝4紫5橙',
  `type` VARCHAR(8) NOT NULL COMMENT '适用兵种：步/骑/弓/通用',
  `effect_type` VARCHAR(32) COMMENT '效果类型：damage/buff/debuff/heal',
  `target` VARCHAR(16) COMMENT '目标：single/all/self',
  `trigger_rate` DOUBLE DEFAULT 0.5 COMMENT '发动概率0-1',
  `power` INT DEFAULT 0 COMMENT '效果强度',
  `attr_attack_bonus` INT DEFAULT 0 COMMENT '攻击加成',
  `attr_defense_bonus` INT DEFAULT 0 COMMENT '防御加成',
  `attr_valor_bonus` INT DEFAULT 0 COMMENT '武勇加成',
  `attr_command_bonus` INT DEFAULT 0 COMMENT '统御加成',
  `learn_level` INT DEFAULT 1 COMMENT '学习所需等级',
  `learn_cost_silver` BIGINT DEFAULT 0 COMMENT '学习消耗白银',
  `enabled` TINYINT(1) DEFAULT 1 COMMENT '是否启用',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='兵法模板表';

-- =============================================
-- 7.1 用户已学习兵法表
-- =============================================
CREATE TABLE IF NOT EXISTS `user_learned_tactics` (
  `user_id` VARCHAR(64) NOT NULL COMMENT '用户唯一标识',
  `tactics_ids` TEXT COMMENT '已学习兵法ID逗号分隔',
  PRIMARY KEY (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户已学习兵法表';

-- =============================================
-- 8. 装备表
-- =============================================
CREATE TABLE IF NOT EXISTS `equipment` (
  `id` VARCHAR(64) NOT NULL COMMENT '装备实例ID',
  `user_id` VARCHAR(64) NOT NULL COMMENT '所属用户ID',
  `name` VARCHAR(64) COMMENT '装备名称',
  `level` INT DEFAULT 0 COMMENT '装备等级要求',
  `icon` VARCHAR(128) COMMENT '图标资源路径',
  `description` VARCHAR(512) COMMENT '装备描述',
  `enhance_level` INT DEFAULT 0 COMMENT '强化等级',
  `quality_value` INT DEFAULT 0 COMMENT '品质值（洗练）',
  `equipped` TINYINT(1) DEFAULT 0 COMMENT '是否已装备',
  `equipped_general_id` VARCHAR(64) COMMENT '装备在哪个武将身上',
  `bound` TINYINT(1) DEFAULT 0 COMMENT '是否绑定',
  `locked` TINYINT(1) DEFAULT 0 COMMENT '是否锁定',
  `slot_type_id` INT COMMENT '槽位类型ID',
  `slot_type_name` VARCHAR(32) COMMENT '槽位类型：武器/铠甲/项链/戒指/鞋子/头盔',
  `slot_type_icon` VARCHAR(128) COMMENT '槽位图标',
  `slot_main_attribute` VARCHAR(32) COMMENT '槽位主属性',
  `quality_id` INT COMMENT '品质ID：1白2绿3蓝4紫5橙',
  `quality_name` VARCHAR(16) COMMENT '品质名称',
  `quality_color` VARCHAR(16) COMMENT '品质颜色代码',
  `quality_multiplier` DOUBLE DEFAULT 1.0 COMMENT '品质属性倍率',
  `quality_icon` VARCHAR(128) COMMENT '品质图标',
  `base_attack` INT DEFAULT 0 COMMENT '基础攻击',
  `base_defense` INT DEFAULT 0 COMMENT '基础防御',
  `base_valor` INT DEFAULT 0 COMMENT '基础武勇',
  `base_command` INT DEFAULT 0 COMMENT '基础统御',
  `base_dodge` DOUBLE DEFAULT 0 COMMENT '基础闪避',
  `base_mobility` INT DEFAULT 0 COMMENT '基础机动',
  `base_hp` INT DEFAULT 0 COMMENT '基础生命',
  `base_crit_rate` DOUBLE DEFAULT 0 COMMENT '基础暴击率',
  `base_crit_damage` DOUBLE DEFAULT 0 COMMENT '基础暴击伤害',
  `bonus_attack` INT DEFAULT 0 COMMENT '附加攻击',
  `bonus_defense` INT DEFAULT 0 COMMENT '附加防御',
  `bonus_valor` INT DEFAULT 0 COMMENT '附加武勇',
  `bonus_command` INT DEFAULT 0 COMMENT '附加统御',
  `bonus_dodge` DOUBLE DEFAULT 0 COMMENT '附加闪避',
  `bonus_mobility` INT DEFAULT 0 COMMENT '附加机动',
  `bonus_hp` INT DEFAULT 0 COMMENT '附加生命',
  `bonus_crit_rate` DOUBLE DEFAULT 0 COMMENT '附加暴击率',
  `bonus_crit_damage` DOUBLE DEFAULT 0 COMMENT '附加暴击伤害',
  `enhance_attack` INT DEFAULT 0 COMMENT '强化攻击',
  `enhance_defense` INT DEFAULT 0 COMMENT '强化防御',
  `enhance_valor` INT DEFAULT 0 COMMENT '强化武勇',
  `enhance_command` INT DEFAULT 0 COMMENT '强化统御',
  `enhance_dodge` DOUBLE DEFAULT 0 COMMENT '强化闪避',
  `enhance_mobility` INT DEFAULT 0 COMMENT '强化机动',
  `enhance_hp` INT DEFAULT 0 COMMENT '强化生命',
  `enhance_crit_rate` DOUBLE DEFAULT 0 COMMENT '强化暴击率',
  `enhance_crit_damage` DOUBLE DEFAULT 0 COMMENT '强化暴击伤害',
  `qa_attack` INT DEFAULT 0 COMMENT '品质攻击',
  `qa_defense` INT DEFAULT 0 COMMENT '品质防御',
  `qa_valor` INT DEFAULT 0 COMMENT '品质武勇',
  `qa_command` INT DEFAULT 0 COMMENT '品质统御',
  `qa_dodge` DOUBLE DEFAULT 0 COMMENT '品质闪避',
  `qa_mobility` INT DEFAULT 0 COMMENT '品质机动',
  `qa_hp` INT DEFAULT 0 COMMENT '品质生命',
  `qa_crit_rate` DOUBLE DEFAULT 0 COMMENT '品质暴击率',
  `qa_crit_damage` DOUBLE DEFAULT 0 COMMENT '品质暴击伤害',
  `source_type` VARCHAR(32) COMMENT '来源类型：dungeon/craft/market',
  `source_name` VARCHAR(64) COMMENT '来源名称',
  `source_detail` VARCHAR(128) COMMENT '来源详情',
  `set_info_json` TEXT COMMENT '套装信息JSON',
  `create_time` BIGINT COMMENT '创建时间戳',
  `update_time` BIGINT COMMENT '更新时间戳',
  PRIMARY KEY (`id`),
  KEY `idx_equip_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='装备实例表';

-- =============================================
-- 9. 阵型表
-- =============================================
CREATE TABLE IF NOT EXISTS `formation` (
  `id` VARCHAR(64) NOT NULL COMMENT '阵型ID',
  `od_user_id` VARCHAR(64) NOT NULL COMMENT '所属用户ID',
  `name` VARCHAR(64) COMMENT '阵型名称',
  `type` VARCHAR(32) COMMENT '阵型类型',
  `active` TINYINT(1) DEFAULT 1 COMMENT '是否激活',
  `create_time` BIGINT COMMENT '创建时间戳',
  `update_time` BIGINT COMMENT '更新时间戳',
  PRIMARY KEY (`id`),
  KEY `idx_formation_user` (`od_user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='阵型表';

-- =============================================
-- 9.1 阵型槽位子表
-- =============================================
CREATE TABLE IF NOT EXISTS `formation_slot` (
  `id` BIGINT AUTO_INCREMENT COMMENT '自增主键',
  `formation_id` VARCHAR(64) NOT NULL COMMENT '关联阵型ID',
  `position` INT NOT NULL COMMENT '槽位编号0-5',
  `general_id` VARCHAR(64) COMMENT '武将ID',
  `general_name` VARCHAR(64) COMMENT '武将名称',
  `general_quality` VARCHAR(16) COMMENT '武将品质',
  `general_avatar` VARCHAR(128) COMMENT '武将头像',
  `mobility` INT DEFAULT 0 COMMENT '机动值',
  PRIMARY KEY (`id`),
  KEY `idx_slot_formation` (`formation_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='阵型槽位表';

-- =============================================
-- 10. 仓库主表
-- =============================================
CREATE TABLE IF NOT EXISTS `warehouse` (
  `id` VARCHAR(64) NOT NULL COMMENT '仓库ID',
  `user_id` VARCHAR(64) NOT NULL COMMENT '所属用户ID',
  `equip_capacity` INT DEFAULT 50 COMMENT '装备仓库容量',
  `equip_base_capacity` INT DEFAULT 50 COMMENT '装备基础容量',
  `equip_expand_times` INT DEFAULT 0 COMMENT '装备扩容次数',
  `equip_used_slots` INT DEFAULT 0 COMMENT '装备已用槽位',
  `equipment_ids` TEXT COMMENT '装备ID列表逗号分隔',
  `item_capacity` INT DEFAULT 100 COMMENT '物品仓库容量',
  `item_base_capacity` INT DEFAULT 100 COMMENT '物品基础容量',
  `item_expand_times` INT DEFAULT 0 COMMENT '物品扩容次数',
  `item_used_slots` INT DEFAULT 0 COMMENT '物品已用槽位',
  `create_time` BIGINT COMMENT '创建时间戳',
  `update_time` BIGINT COMMENT '更新时间戳',
  PRIMARY KEY (`id`),
  KEY `idx_wh_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='仓库主表';

-- =============================================
-- 10.1 仓库物品子表
-- =============================================
CREATE TABLE IF NOT EXISTS `warehouse_item` (
  `id` BIGINT AUTO_INCREMENT COMMENT '自增主键',
  `warehouse_id` VARCHAR(64) NOT NULL COMMENT '关联仓库ID',
  `user_id` VARCHAR(64) NOT NULL COMMENT '所属用户ID',
  `item_id` VARCHAR(64) NOT NULL COMMENT '物品ID',
  `name` VARCHAR(64) COMMENT '物品名称',
  `icon` VARCHAR(128) COMMENT '物品图标',
  `item_type` VARCHAR(32) COMMENT '物品类型：equipment/material/consumable',
  `count` INT DEFAULT 1 COMMENT '数量',
  `quality` VARCHAR(16) COMMENT '品质',
  `description` VARCHAR(512) COMMENT '物品描述',
  `usable` TINYINT(1) DEFAULT 0 COMMENT '是否可使用',
  `bound` TINYINT(1) DEFAULT 0 COMMENT '是否绑定',
  `max_stack` INT DEFAULT 9999 COMMENT '最大堆叠数',
  PRIMARY KEY (`id`),
  KEY `idx_whi_warehouse` (`warehouse_id`),
  KEY `idx_whi_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='仓库物品表';

-- =============================================
-- 11. 生产设施表（7个设施前缀展开）
-- =============================================
CREATE TABLE IF NOT EXISTS `production` (
  `od_user_id` VARCHAR(64) NOT NULL COMMENT '用户唯一标识',
  -- 银矿
  `sm_level` INT DEFAULT 1 COMMENT '银矿等级',
  `sm_output` BIGINT DEFAULT 0 COMMENT '银矿产量/小时',
  `sm_capacity` BIGINT DEFAULT 0 COMMENT '银矿存储上限',
  `sm_stored` BIGINT DEFAULT 0 COMMENT '银矿当前存储',
  `sm_last_collect` BIGINT DEFAULT 0 COMMENT '银矿上次收取时间戳',
  `sm_upgrade_end` BIGINT DEFAULT 0 COMMENT '银矿升级完成时间戳',
  `sm_upgrade_food` BIGINT DEFAULT 0 COMMENT '银矿升级消耗粮食',
  `sm_upgrade_paper` BIGINT DEFAULT 0 COMMENT '银矿升级消耗纸张',
  -- 农场
  `fm_level` INT DEFAULT 1 COMMENT '农场等级',
  `fm_output` BIGINT DEFAULT 0 COMMENT '农场产量/小时',
  `fm_capacity` BIGINT DEFAULT 0 COMMENT '农场存储上限',
  `fm_stored` BIGINT DEFAULT 0 COMMENT '农场当前存储',
  `fm_last_collect` BIGINT DEFAULT 0 COMMENT '农场上次收取时间戳',
  `fm_upgrade_end` BIGINT DEFAULT 0 COMMENT '农场升级完成时间戳',
  `fm_upgrade_food` BIGINT DEFAULT 0 COMMENT '农场升级消耗粮食',
  `fm_upgrade_paper` BIGINT DEFAULT 0 COMMENT '农场升级消耗纸张',
  -- 伐木场
  `lm_level` INT DEFAULT 1 COMMENT '伐木场等级',
  `lm_output` BIGINT DEFAULT 0 COMMENT '伐木场产量/小时',
  `lm_capacity` BIGINT DEFAULT 0 COMMENT '伐木场存储上限',
  `lm_stored` BIGINT DEFAULT 0 COMMENT '伐木场当前存储',
  `lm_last_collect` BIGINT DEFAULT 0 COMMENT '伐木场上次收取时间戳',
  `lm_upgrade_end` BIGINT DEFAULT 0 COMMENT '伐木场升级完成时间戳',
  `lm_upgrade_food` BIGINT DEFAULT 0 COMMENT '伐木场升级消耗粮食',
  `lm_upgrade_paper` BIGINT DEFAULT 0 COMMENT '伐木场升级消耗纸张',
  -- 造纸坊
  `pm_level` INT DEFAULT 1 COMMENT '造纸坊等级',
  `pm_output` BIGINT DEFAULT 0 COMMENT '造纸坊产量/小时',
  `pm_capacity` BIGINT DEFAULT 0 COMMENT '造纸坊存储上限',
  `pm_stored` BIGINT DEFAULT 0 COMMENT '造纸坊当前存储',
  `pm_last_collect` BIGINT DEFAULT 0 COMMENT '造纸坊上次收取时间戳',
  `pm_upgrade_end` BIGINT DEFAULT 0 COMMENT '造纸坊升级完成时间戳',
  `pm_upgrade_food` BIGINT DEFAULT 0 COMMENT '造纸坊升级消耗粮食',
  `pm_upgrade_paper` BIGINT DEFAULT 0 COMMENT '造纸坊升级消耗纸张',
  -- 冶炼厂
  `mt_level` INT DEFAULT 1 COMMENT '冶炼厂等级',
  `mt_output` BIGINT DEFAULT 0 COMMENT '冶炼厂产量/小时',
  `mt_capacity` BIGINT DEFAULT 0 COMMENT '冶炼厂存储上限',
  `mt_stored` BIGINT DEFAULT 0 COMMENT '冶炼厂当前存储',
  `mt_last_collect` BIGINT DEFAULT 0 COMMENT '冶炼厂上次收取时间戳',
  `mt_upgrade_end` BIGINT DEFAULT 0 COMMENT '冶炼厂升级完成时间戳',
  `mt_upgrade_food` BIGINT DEFAULT 0 COMMENT '冶炼厂升级消耗粮食',
  `mt_upgrade_paper` BIGINT DEFAULT 0 COMMENT '冶炼厂升级消耗纸张',
  -- 军械局
  `ar_level` INT DEFAULT 1 COMMENT '军械局等级',
  `ar_output` BIGINT DEFAULT 0 COMMENT '军械局产量/小时',
  `ar_capacity` BIGINT DEFAULT 0 COMMENT '军械局存储上限',
  `ar_stored` BIGINT DEFAULT 0 COMMENT '军械局当前存储',
  `ar_last_collect` BIGINT DEFAULT 0 COMMENT '军械局上次收取时间戳',
  `ar_upgrade_end` BIGINT DEFAULT 0 COMMENT '军械局升级完成时间戳',
  `ar_upgrade_food` BIGINT DEFAULT 0 COMMENT '军械局升级消耗粮食',
  `ar_upgrade_paper` BIGINT DEFAULT 0 COMMENT '军械局升级消耗纸张',
  -- 校场
  `tp_level` INT DEFAULT 1 COMMENT '校场等级',
  `tp_output` BIGINT DEFAULT 0 COMMENT '校场产量/小时',
  `tp_capacity` BIGINT DEFAULT 0 COMMENT '校场存储上限',
  `tp_stored` BIGINT DEFAULT 0 COMMENT '校场当前存储',
  `tp_last_collect` BIGINT DEFAULT 0 COMMENT '校场上次收取时间戳',
  `tp_upgrade_end` BIGINT DEFAULT 0 COMMENT '校场升级完成时间戳',
  `tp_upgrade_food` BIGINT DEFAULT 0 COMMENT '校场升级消耗粮食',
  `tp_upgrade_paper` BIGINT DEFAULT 0 COMMENT '校场升级消耗纸张',
  `create_time` BIGINT COMMENT '创建时间戳',
  `update_time` BIGINT COMMENT '更新时间戳',
  PRIMARY KEY (`od_user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='生产设施表';

-- =============================================
-- 12. 联盟主表
-- =============================================
CREATE TABLE IF NOT EXISTS `alliance` (
  `id` VARCHAR(64) NOT NULL COMMENT '联盟ID',
  `name` VARCHAR(64) NOT NULL COMMENT '联盟名称',
  `leader_id` VARCHAR(64) COMMENT '盟主用户ID',
  `leader_name` VARCHAR(64) COMMENT '盟主名称',
  `level` INT DEFAULT 1 COMMENT '联盟等级',
  `notice` VARCHAR(512) COMMENT '联盟公告',
  `max_members` INT DEFAULT 30 COMMENT '成员上限',
  `auto_approve` TINYINT(1) DEFAULT 0 COMMENT '是否自动审批',
  `min_level` INT DEFAULT 1 COMMENT '加入最低等级',
  `create_time` BIGINT COMMENT '创建时间戳',
  `update_time` BIGINT COMMENT '更新时间戳',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='联盟主表';

-- =============================================
-- 12.1 联盟成员子表
-- =============================================
CREATE TABLE IF NOT EXISTS `alliance_member` (
  `id` BIGINT AUTO_INCREMENT COMMENT '自增主键',
  `alliance_id` VARCHAR(64) NOT NULL COMMENT '联盟ID',
  `user_id` VARCHAR(64) NOT NULL COMMENT '成员用户ID',
  `name` VARCHAR(64) COMMENT '成员名称',
  `role` VARCHAR(16) DEFAULT 'member' COMMENT '角色：leader/officer/member',
  `level` INT DEFAULT 1 COMMENT '成员等级',
  `contribution` BIGINT DEFAULT 0 COMMENT '贡献值',
  `join_time` BIGINT COMMENT '加入时间戳',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_am_alliance_user` (`alliance_id`, `user_id`),
  KEY `idx_am_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='联盟成员表';

-- =============================================
-- 12.2 联盟申请子表
-- =============================================
CREATE TABLE IF NOT EXISTS `alliance_application` (
  `id` BIGINT AUTO_INCREMENT COMMENT '自增主键',
  `alliance_id` VARCHAR(64) NOT NULL COMMENT '联盟ID',
  `user_id` VARCHAR(64) NOT NULL COMMENT '申请人用户ID',
  `user_name` VARCHAR(64) COMMENT '申请人名称',
  `user_level` INT DEFAULT 1 COMMENT '申请人等级',
  `status` VARCHAR(16) DEFAULT 'pending' COMMENT '状态：pending/approved/rejected',
  `apply_time` BIGINT COMMENT '申请时间戳',
  PRIMARY KEY (`id`),
  KEY `idx_aa_alliance` (`alliance_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='联盟申请表';

-- =============================================
-- 12.3 用户联盟关联表
-- =============================================
CREATE TABLE IF NOT EXISTS `user_alliance` (
  `user_id` VARCHAR(64) NOT NULL COMMENT '用户ID',
  `alliance_id` VARCHAR(64) COMMENT '所属联盟ID',
  PRIMARY KEY (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户联盟关联表';

-- =============================================
-- 13. 联盟战表（保留JSON存储复杂战斗数据）
-- =============================================
CREATE TABLE IF NOT EXISTS `alliance_war` (
  `war_date` VARCHAR(16) NOT NULL COMMENT '战争日期(yyyyMMdd)',
  `data` LONGTEXT COMMENT '战争数据JSON（含battles/participants/ranks）',
  PRIMARY KEY (`war_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='联盟战表';

-- =============================================
-- 14. 国战表（保留JSON存储复杂战斗数据）
-- =============================================
CREATE TABLE IF NOT EXISTS `nation_war` (
  `war_date` VARCHAR(16) NOT NULL COMMENT '战争日期(yyyyMMdd)',
  `data` LONGTEXT COMMENT '国战数据JSON（含nations/battles/ranks）',
  PRIMARY KEY (`war_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='国战表';

-- =============================================
-- 15. 战役进度表
-- =============================================
CREATE TABLE IF NOT EXISTS `campaign_progress` (
  `id` BIGINT AUTO_INCREMENT COMMENT '自增主键',
  `user_id` VARCHAR(64) NOT NULL COMMENT '用户ID',
  `campaign_id` VARCHAR(64) NOT NULL COMMENT '战役ID',
  `current_stage` INT DEFAULT 0 COMMENT '当前关卡',
  `max_cleared_stage` INT DEFAULT 0 COMMENT '最高通关关卡',
  `today_challenge_count` INT DEFAULT 0 COMMENT '今日挑战次数',
  `today_date` VARCHAR(8) COMMENT '今日日期(yyyyMMdd)',
  `status` VARCHAR(16) DEFAULT 'idle' COMMENT '状态：idle/in_progress/completed',
  `current_troops` INT DEFAULT 0 COMMENT '当前兵力',
  `max_troops` INT DEFAULT 0 COMMENT '最大兵力',
  `revive_count` INT DEFAULT 0 COMMENT '复活次数',
  `general_id` VARCHAR(64) COMMENT '出战武将ID',
  `full_cleared` TINYINT(1) DEFAULT 0 COMMENT '是否全通关',
  `total_exp_gained` BIGINT DEFAULT 0 COMMENT '累计获得经验',
  `total_silver_gained` BIGINT DEFAULT 0 COMMENT '累计获得白银',
  `start_time` BIGINT COMMENT '开始时间戳',
  `update_time` BIGINT COMMENT '更新时间戳',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_cp_user_campaign` (`user_id`, `campaign_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='战役进度表';

-- =============================================
-- 16. 副本进度表
-- =============================================
CREATE TABLE IF NOT EXISTS `dungeon_progress` (
  `user_id` VARCHAR(64) NOT NULL COMMENT '用户ID',
  `dungeon_id` VARCHAR(64) NOT NULL COMMENT '副本ID',
  `current_progress` INT DEFAULT 0 COMMENT '当前进度（已击败NPC数）',
  `defeated_npcs` TEXT COMMENT '已击败NPC编号逗号分隔',
  `today_entries` INT DEFAULT 0 COMMENT '今日进入次数',
  `last_entry_date` VARCHAR(8) COMMENT '上次进入日期(yyyyMMdd)',
  `cleared` TINYINT(1) DEFAULT 0 COMMENT '是否已通关',
  `clear_count` INT DEFAULT 0 COMMENT '通关次数',
  `create_time` BIGINT COMMENT '创建时间戳',
  `update_time` BIGINT COMMENT '更新时间戳',
  PRIMARY KEY (`user_id`, `dungeon_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='副本进度表';

-- =============================================
-- 17. 掠夺数据表
-- =============================================
CREATE TABLE IF NOT EXISTS `plunder_data` (
  `user_id` VARCHAR(64) NOT NULL COMMENT '用户ID',
  `today_plunder_count` INT DEFAULT 0 COMMENT '今日已掠夺次数',
  `today_purchased_count` INT DEFAULT 0 COMMENT '今日已购买次数',
  `last_plunder_date` VARCHAR(8) COMMENT '上次掠夺日期(yyyyMMdd)',
  `update_time` BIGINT COMMENT '更新时间戳',
  PRIMARY KEY (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='掠夺数据表';

-- =============================================
-- 17.1 掠夺记录表
-- =============================================
CREATE TABLE IF NOT EXISTS `plunder_record` (
  `id` BIGINT AUTO_INCREMENT COMMENT '自增主键',
  `attacker_id` VARCHAR(64) NOT NULL COMMENT '攻击方用户ID',
  `attacker_name` VARCHAR(64) COMMENT '攻击方名称',
  `defender_id` VARCHAR(64) NOT NULL COMMENT '防守方用户ID/NPC ID',
  `defender_name` VARCHAR(64) COMMENT '防守方名称',
  `is_npc` TINYINT(1) DEFAULT 0 COMMENT '防守方是否NPC',
  `result` VARCHAR(8) COMMENT '结果：win/lose',
  `silver_gained` BIGINT DEFAULT 0 COMMENT '获得白银',
  `wood_gained` BIGINT DEFAULT 0 COMMENT '获得木材',
  `paper_gained` BIGINT DEFAULT 0 COMMENT '获得纸张',
  `food_gained` BIGINT DEFAULT 0 COMMENT '获得粮食',
  `create_time` BIGINT COMMENT '记录时间戳',
  `server_id` INT DEFAULT 1 COMMENT '区服ID',
  PRIMARY KEY (`id`),
  KEY `idx_pr_attacker` (`attacker_id`),
  KEY `idx_pr_defender` (`defender_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='掠夺记录表';

-- =============================================
-- 17.2 阵营NPC表（掠夺用）
-- =============================================
CREATE TABLE IF NOT EXISTS `faction_npc` (
  `id` BIGINT AUTO_INCREMENT COMMENT '自增主键',
  `npc_id` VARCHAR(64) NOT NULL COMMENT 'NPC唯一标识',
  `name` VARCHAR(64) NOT NULL COMMENT 'NPC名称',
  `faction` VARCHAR(16) NOT NULL COMMENT '阵营：西凉/突厥/鲜卑/羌族',
  `level` INT NOT NULL COMMENT 'NPC等级',
  `attack` INT DEFAULT 0 COMMENT '攻击力',
  `defense` INT DEFAULT 0 COMMENT '防御力',
  `silver` BIGINT DEFAULT 0 COMMENT '白银资源',
  `wood` BIGINT DEFAULT 0 COMMENT '木材资源',
  `paper` BIGINT DEFAULT 0 COMMENT '纸张资源',
  `food` BIGINT DEFAULT 0 COMMENT '粮食资源',
  `server_id` INT DEFAULT 1 COMMENT '区服ID',
  PRIMARY KEY (`id`),
  KEY `idx_fn_level` (`level`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='阵营NPC表';

-- =============================================
-- 18. 军需运送表
-- =============================================
CREATE TABLE IF NOT EXISTS `supply_convoy` (
  `id` VARCHAR(64) NOT NULL COMMENT '军需ID',
  `user_id` VARCHAR(64) NOT NULL COMMENT '运送者用户ID',
  `grade` INT DEFAULT 1 COMMENT '军需等级1-5（普通到顶级）',
  `grade_name` VARCHAR(16) COMMENT '等级名称',
  `silver_reward` BIGINT DEFAULT 0 COMMENT '白银奖励',
  `paper_reward` BIGINT DEFAULT 0 COMMENT '纸张奖励',
  `food_reward` BIGINT DEFAULT 0 COMMENT '粮食奖励',
  `metal_reward` BIGINT DEFAULT 0 COMMENT '金属奖励',
  `robbed_count` INT DEFAULT 0 COMMENT '已被抢夺次数（上限3）',
  `robbed_silver` BIGINT DEFAULT 0 COMMENT '被抢白银总量',
  `robbed_paper` BIGINT DEFAULT 0 COMMENT '被抢纸张总量',
  `robbed_food` BIGINT DEFAULT 0 COMMENT '被抢粮食总量',
  `robbed_metal` BIGINT DEFAULT 0 COMMENT '被抢金属总量',
  `status` VARCHAR(16) DEFAULT 'delivering' COMMENT '状态：delivering/completed/robbed',
  `start_time` BIGINT COMMENT '开始运送时间戳',
  `end_time` BIGINT COMMENT '预计完成时间戳',
  `create_time` BIGINT COMMENT '创建时间戳',
  `server_id` INT DEFAULT 1 COMMENT '区服ID',
  PRIMARY KEY (`id`),
  KEY `idx_sc_user` (`user_id`),
  KEY `idx_sc_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='军需运送表';

-- =============================================
-- 19. 英雄榜排名表
-- =============================================
CREATE TABLE IF NOT EXISTS `hero_rank` (
  `id` BIGINT AUTO_INCREMENT COMMENT '自增主键',
  `user_id` VARCHAR(64) NOT NULL COMMENT '用户ID或NPC ID',
  `user_name` VARCHAR(64) COMMENT '名称',
  `is_npc` TINYINT(1) DEFAULT 0 COMMENT '是否NPC',
  `rank_position` INT NOT NULL COMMENT '排名位置',
  `power` BIGINT DEFAULT 0 COMMENT '战力值',
  `level` INT DEFAULT 1 COMMENT '等级',
  `avatar` VARCHAR(128) COMMENT '头像',
  `server_id` INT DEFAULT 1 COMMENT '区服ID',
  `update_time` BIGINT COMMENT '更新时间戳',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_hr_server_rank` (`server_id`, `rank_position`),
  KEY `idx_hr_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='英雄榜排名表';

-- =============================================
-- 19.1 英雄榜挑战数据表
-- =============================================
CREATE TABLE IF NOT EXISTS `hero_rank_challenge` (
  `user_id` VARCHAR(64) NOT NULL COMMENT '用户ID',
  `today_challenge_count` INT DEFAULT 0 COMMENT '今日挑战次数',
  `today_purchased_count` INT DEFAULT 0 COMMENT '今日已购买次数',
  `last_challenge_date` VARCHAR(8) COMMENT '上次挑战日期(yyyyMMdd)',
  `last_challenge_time` BIGINT DEFAULT 0 COMMENT '上次挑战时间戳(ms)',
  `server_id` INT DEFAULT 1 COMMENT '区服ID',
  PRIMARY KEY (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='英雄榜挑战数据表';

-- =============================================
-- 20. 充值订单表
-- =============================================
CREATE TABLE IF NOT EXISTS `recharge_order` (
  `id` VARCHAR(64) NOT NULL COMMENT '订单ID',
  `od_user_id` VARCHAR(64) NOT NULL COMMENT '用户ID',
  `amount` BIGINT DEFAULT 0 COMMENT '充值金额（分）',
  `product_id` VARCHAR(64) COMMENT '商品ID',
  `product_name` VARCHAR(64) COMMENT '商品名称',
  `payment_method` VARCHAR(16) COMMENT '支付方式：WECHAT/ALIPAY/UNIONPAY',
  `status` VARCHAR(20) COMMENT '订单状态：PENDING/PAID/FAILED/REFUNDED',
  `trade_no` VARCHAR(128) COMMENT '第三方交易号',
  `gold_amount` BIGINT DEFAULT 0 COMMENT '获得黄金数',
  `diamond_amount` BIGINT DEFAULT 0 COMMENT '获得钻石数',
  `bonus_items` TEXT COMMENT '赠品列表JSON',
  `create_time` BIGINT COMMENT '创建时间戳',
  `pay_time` BIGINT COMMENT '支付时间戳',
  `update_time` BIGINT COMMENT '更新时间戳',
  PRIMARY KEY (`id`),
  KEY `idx_ro_user` (`od_user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='充值订单表';

-- =============================================
-- 21. 市场挂牌表
-- =============================================
CREATE TABLE IF NOT EXISTS `market_listing` (
  `id` BIGINT AUTO_INCREMENT COMMENT '自增主键',
  `seller_id` VARCHAR(64) NOT NULL COMMENT '卖家用户ID',
  `seller_name` VARCHAR(64) COMMENT '卖家名称',
  `item_id` VARCHAR(64) NOT NULL COMMENT '物品ID',
  `item_name` VARCHAR(64) COMMENT '物品名称',
  `item_type` VARCHAR(32) COMMENT '物品类型：equipment/material/consumable',
  `item_snapshot` TEXT COMMENT '物品快照JSON（历史记录）',
  `price` BIGINT NOT NULL COMMENT '挂牌价格（黄金）',
  `commission` BIGINT DEFAULT 0 COMMENT '佣金（白银）',
  `status` VARCHAR(16) DEFAULT 'active' COMMENT '状态：active/sold/cancelled',
  `buyer_id` VARCHAR(64) COMMENT '买家用户ID',
  `buyer_name` VARCHAR(64) COMMENT '买家名称',
  `create_time` BIGINT COMMENT '挂牌时间戳',
  `sold_time` BIGINT COMMENT '成交时间戳',
  `server_id` INT DEFAULT 1 COMMENT '区服ID',
  PRIMARY KEY (`id`),
  KEY `idx_ml_seller` (`seller_id`),
  KEY `idx_ml_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='市场挂牌表';

-- =============================================
-- 22. 邮件表
-- =============================================
CREATE TABLE IF NOT EXISTS `mail` (
  `id` BIGINT AUTO_INCREMENT COMMENT '自增主键',
  `sender_id` VARCHAR(64) COMMENT '发送者ID（系统邮件为SYSTEM）',
  `sender_name` VARCHAR(64) COMMENT '发送者名称',
  `receiver_id` VARCHAR(64) NOT NULL COMMENT '接收者用户ID',
  `title` VARCHAR(128) COMMENT '邮件标题',
  `content` TEXT COMMENT '邮件内容',
  `mail_type` VARCHAR(16) DEFAULT 'normal' COMMENT '类型：system/reward/player/normal',
  `attachments` TEXT COMMENT '附件JSON（奖励物品列表）',
  `is_read` TINYINT(1) DEFAULT 0 COMMENT '是否已读',
  `is_claimed` TINYINT(1) DEFAULT 0 COMMENT '附件是否已领取',
  `create_time` BIGINT COMMENT '发送时间戳',
  `expire_time` BIGINT COMMENT '过期时间戳',
  `server_id` INT DEFAULT 1 COMMENT '区服ID',
  PRIMARY KEY (`id`),
  KEY `idx_mail_receiver` (`receiver_id`),
  KEY `idx_mail_type` (`mail_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='邮件表';

-- =============================================
-- 23. VIP礼包领取记录表
-- =============================================
CREATE TABLE IF NOT EXISTS `vip_gift_record` (
  `id` BIGINT AUTO_INCREMENT COMMENT '自增主键',
  `user_id` VARCHAR(64) NOT NULL COMMENT '用户ID',
  `vip_level` INT NOT NULL COMMENT '领取的VIP等级',
  `claimed_time` BIGINT COMMENT '领取时间戳',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_vgr_user_level` (`user_id`, `vip_level`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='VIP礼包领取记录表';

-- =============================================
-- 24. 区服表
-- =============================================
CREATE TABLE IF NOT EXISTS `game_server` (
  `id` INT AUTO_INCREMENT COMMENT '区服ID',
  `name` VARCHAR(64) NOT NULL COMMENT '区服名称',
  `status` VARCHAR(16) DEFAULT 'normal' COMMENT '状态：normal/hot/full/maintenance',
  `open_time` BIGINT COMMENT '开服时间戳',
  `max_players` INT DEFAULT 10000 COMMENT '最大玩家数',
  `current_players` INT DEFAULT 0 COMMENT '当前玩家数',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='区服表';

-- =============================================
-- 25. 用户区服关联表
-- =============================================
CREATE TABLE IF NOT EXISTS `user_server` (
  `user_id` VARCHAR(64) NOT NULL COMMENT '用户ID（openId）',
  `server_id` INT NOT NULL COMMENT '区服ID',
  `lord_name` VARCHAR(64) COMMENT '主公名称',
  `create_time` BIGINT COMMENT '创建角色时间戳',
  `last_login_time` BIGINT COMMENT '最后登录时间戳',
  PRIMARY KEY (`user_id`, `server_id`),
  UNIQUE KEY `uk_server_lord_name` (`server_id`, `lord_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户区服关联表';

-- =============================================
-- 26. 公告表
-- =============================================
CREATE TABLE IF NOT EXISTS `announcement` (
  `id` BIGINT AUTO_INCREMENT COMMENT '自增主键',
  `title` VARCHAR(128) NOT NULL COMMENT '公告标题',
  `content` TEXT COMMENT '公告内容',
  `type` VARCHAR(16) DEFAULT 'normal' COMMENT '类型：normal/urgent/maintenance',
  `priority` INT DEFAULT 0 COMMENT '优先级，数值越大越靠前',
  `enabled` TINYINT(1) DEFAULT 1 COMMENT '是否启用',
  `start_time` BIGINT COMMENT '生效时间戳',
  `end_time` BIGINT COMMENT '失效时间戳',
  `create_time` BIGINT COMMENT '创建时间戳',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='公告表';

-- 兼容旧表: 补充 priority 字段
ALTER TABLE `announcement` ADD COLUMN IF NOT EXISTS `priority` INT DEFAULT 0 COMMENT '优先级';

-- =============================================
-- 27. 聊天消息表
-- =============================================
CREATE TABLE IF NOT EXISTS `chat_message` (
  `id` BIGINT AUTO_INCREMENT COMMENT '自增主键',
  `sender_id` VARCHAR(64) NOT NULL COMMENT '发送者用户ID',
  `sender_name` VARCHAR(64) COMMENT '发送者名称',
  `channel` VARCHAR(16) DEFAULT 'world' COMMENT '频道：world/alliance/private',
  `target_id` VARCHAR(64) COMMENT '私聊目标用户ID',
  `content` VARCHAR(512) NOT NULL COMMENT '消息内容',
  `create_time` BIGINT COMMENT '发送时间戳',
  `server_id` INT DEFAULT 1 COMMENT '区服ID',
  PRIMARY KEY (`id`),
  KEY `idx_cm_channel` (`channel`, `server_id`),
  KEY `idx_cm_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='聊天消息表';

-- =============================================
-- VIP礼包领取记录
-- =============================================
CREATE TABLE IF NOT EXISTS `vip_gift_claim` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `user_id` VARCHAR(64) NOT NULL COMMENT '用户ID',
  `vip_level` INT NOT NULL COMMENT 'VIP等级',
  `claim_time` BIGINT NOT NULL COMMENT '领取时间',
  UNIQUE KEY `uk_user_level` (`user_id`, `vip_level`),
  KEY `idx_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='VIP礼包领取记录';

-- =============================================
-- 初始数据：区服
-- =============================================
INSERT INTO `game_server` (`name`, `status`, `open_time`, `max_players`, `current_players`)
VALUES ('风云第1服', 'normal', UNIX_TIMESTAMP() * 1000, 10000, 0)
ON DUPLICATE KEY UPDATE `name` = VALUES(`name`);

-- =============================================
-- VIP专用道具
-- =============================================
INSERT INTO `item` (`item_id`,`item_name`,`quality`) VALUES (101,'鹰扬宝箱',5) ON DUPLICATE KEY UPDATE `item_name`=VALUES(`item_name`);
INSERT INTO `item` (`item_id`,`item_name`,`quality`) VALUES (102,'虎啸宝箱',5) ON DUPLICATE KEY UPDATE `item_name`=VALUES(`item_name`);
INSERT INTO `item` (`item_id`,`item_name`,`quality`) VALUES (103,'凤鸣宝箱',5) ON DUPLICATE KEY UPDATE `item_name`=VALUES(`item_name`);
INSERT INTO `item` (`item_id`,`item_name`,`quality`) VALUES (104,'鹰扬自选券',5) ON DUPLICATE KEY UPDATE `item_name`=VALUES(`item_name`);
INSERT INTO `item` (`item_id`,`item_name`,`quality`) VALUES (105,'虎啸自选券',5) ON DUPLICATE KEY UPDATE `item_name`=VALUES(`item_name`);
INSERT INTO `item` (`item_id`,`item_name`,`quality`) VALUES (106,'凤鸣自选券',5) ON DUPLICATE KEY UPDATE `item_name`=VALUES(`item_name`);
