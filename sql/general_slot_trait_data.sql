-- =============================================
-- general_slot_trait 表结构（如尚未创建则执行）
-- =============================================
CREATE TABLE IF NOT EXISTS `general_slot_trait` (
  `id` BIGINT AUTO_INCREMENT COMMENT '自增主键',
  `slot_id` INT NOT NULL COMMENT '关联 general_slot.id',
  `trait_type` VARCHAR(32) NOT NULL COMMENT '特性类型: attack/defense/valor/command/dodge/tactics_trigger',
  `trait_value` VARCHAR(64) NOT NULL COMMENT '特性值: 数值型加成或倍率(tactics_trigger=2表示翻倍)',
  PRIMARY KEY (`id`),
  KEY `idx_gst_slot` (`slot_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='武将槽位名将特性表';

-- =============================================
-- 清理旧数据后重新插入
-- =============================================
DELETE FROM `general_slot_trait`;

-- =============================================
-- 六种特性类型说明:
--   attack         增加攻击  (固定值)
--   defense        增加防御  (固定值)
--   valor          增加武勇  (固定值)
--   command        增加统御  (固定值)
--   dodge          增加闪避  (固定值, 上限50)
--   tactics_trigger 兵法发动概率翻倍 (值=2表示2倍)
-- =============================================

-- ===========================================
-- 橙色名将 (slot_id 1-6, 对应 slot_index 1-6)
-- 每个橙色名将拥有2-3个独特特性
-- ===========================================

-- slot_id=1  赵云 (橙色步兵统帅) — 浑身是胆, 攻守兼备
INSERT INTO `general_slot_trait` (`slot_id`, `trait_type`, `trait_value`) VALUES (1, 'valor',   '400');
INSERT INTO `general_slot_trait` (`slot_id`, `trait_type`, `trait_value`) VALUES (1, 'dodge',   '5');
INSERT INTO `general_slot_trait` (`slot_id`, `trait_type`, `trait_value`) VALUES (1, 'defense', '300');

-- slot_id=2  (橙色步兵猛将, 如张辽) — 威震逍遥津
INSERT INTO `general_slot_trait` (`slot_id`, `trait_type`, `trait_value`) VALUES (2, 'attack',  '450');
INSERT INTO `general_slot_trait` (`slot_id`, `trait_type`, `trait_value`) VALUES (2, 'valor',   '300');

-- slot_id=3  吕布 (橙色骑兵猛将) — 天下无双, 兵法发动翻倍
INSERT INTO `general_slot_trait` (`slot_id`, `trait_type`, `trait_value`) VALUES (3, 'attack',          '500');
INSERT INTO `general_slot_trait` (`slot_id`, `trait_type`, `trait_value`) VALUES (3, 'tactics_trigger',  '2');

-- slot_id=4  (橙色骑兵统帅, 如马超) — 锦马超
INSERT INTO `general_slot_trait` (`slot_id`, `trait_type`, `trait_value`) VALUES (4, 'attack',  '350');
INSERT INTO `general_slot_trait` (`slot_id`, `trait_type`, `trait_value`) VALUES (4, 'command', '350');

-- slot_id=5  诸葛亮 (橙色弓兵智将) — 卧龙之智, 兵法发动翻倍
INSERT INTO `general_slot_trait` (`slot_id`, `trait_type`, `trait_value`) VALUES (5, 'command',          '450');
INSERT INTO `general_slot_trait` (`slot_id`, `trait_type`, `trait_value`) VALUES (5, 'tactics_trigger',  '2');

-- slot_id=6  (橙色弓兵统帅, 如周瑜) — 火攻大师
INSERT INTO `general_slot_trait` (`slot_id`, `trait_type`, `trait_value`) VALUES (6, 'command', '400');
INSERT INTO `general_slot_trait` (`slot_id`, `trait_type`, `trait_value`) VALUES (6, 'valor',   '300');

-- ===========================================
-- 紫色名将 (slot_id 从 7 开始)
-- 每个紫色名将拥有1-2个特性, 数值略低于橙色
-- ===========================================

-- slot_id=7  张飞 (紫色步兵统帅) — 万军之中取上将首级
INSERT INTO `general_slot_trait` (`slot_id`, `trait_type`, `trait_value`) VALUES (7, 'defense', '300');
INSERT INTO `general_slot_trait` (`slot_id`, `trait_type`, `trait_value`) VALUES (7, 'valor',   '250');

-- slot_id=8  (紫色步兵猛将)
INSERT INTO `general_slot_trait` (`slot_id`, `trait_type`, `trait_value`) VALUES (8, 'attack',  '280');
INSERT INTO `general_slot_trait` (`slot_id`, `trait_type`, `trait_value`) VALUES (8, 'defense', '200');

-- slot_id=9  关羽 (紫色骑兵统帅) — 武圣
INSERT INTO `general_slot_trait` (`slot_id`, `trait_type`, `trait_value`) VALUES (9, 'attack',  '350');
INSERT INTO `general_slot_trait` (`slot_id`, `trait_type`, `trait_value`) VALUES (9, 'command', '250');

-- slot_id=10 (紫色骑兵猛将)
INSERT INTO `general_slot_trait` (`slot_id`, `trait_type`, `trait_value`) VALUES (10, 'attack', '300');
INSERT INTO `general_slot_trait` (`slot_id`, `trait_type`, `trait_value`) VALUES (10, 'valor',  '200');

-- slot_id=11 (紫色弓兵智将)
INSERT INTO `general_slot_trait` (`slot_id`, `trait_type`, `trait_value`) VALUES (11, 'command', '300');
INSERT INTO `general_slot_trait` (`slot_id`, `trait_type`, `trait_value`) VALUES (11, 'dodge',   '3');

-- slot_id=12 (紫色弓兵统帅)
INSERT INTO `general_slot_trait` (`slot_id`, `trait_type`, `trait_value`) VALUES (12, 'command', '250');
INSERT INTO `general_slot_trait` (`slot_id`, `trait_type`, `trait_value`) VALUES (12, 'attack',  '200');

-- ===========================================
-- 红色名将 (slot_id 从 13 开始)
-- 每个红色名将拥有1-2个特性, 数值低于紫色
-- ===========================================

-- slot_id=13 (红色步兵统帅)
INSERT INTO `general_slot_trait` (`slot_id`, `trait_type`, `trait_value`) VALUES (13, 'defense', '200');
INSERT INTO `general_slot_trait` (`slot_id`, `trait_type`, `trait_value`) VALUES (13, 'command', '150');

-- slot_id=14 (红色步兵猛将)
INSERT INTO `general_slot_trait` (`slot_id`, `trait_type`, `trait_value`) VALUES (14, 'attack',  '220');
INSERT INTO `general_slot_trait` (`slot_id`, `trait_type`, `trait_value`) VALUES (14, 'valor',   '150');

-- slot_id=15 (红色骑兵统帅)
INSERT INTO `general_slot_trait` (`slot_id`, `trait_type`, `trait_value`) VALUES (15, 'attack',  '200');
INSERT INTO `general_slot_trait` (`slot_id`, `trait_type`, `trait_value`) VALUES (15, 'command', '150');

-- slot_id=16 (红色骑兵猛将)
INSERT INTO `general_slot_trait` (`slot_id`, `trait_type`, `trait_value`) VALUES (16, 'attack',  '200');
INSERT INTO `general_slot_trait` (`slot_id`, `trait_type`, `trait_value`) VALUES (16, 'valor',   '120');

-- slot_id=17 貂蝉 (红色弓兵智将) — 闭月
INSERT INTO `general_slot_trait` (`slot_id`, `trait_type`, `trait_value`) VALUES (17, 'dodge',   '4');
INSERT INTO `general_slot_trait` (`slot_id`, `trait_type`, `trait_value`) VALUES (17, 'command', '200');

-- slot_id=18 (红色弓兵统帅)
INSERT INTO `general_slot_trait` (`slot_id`, `trait_type`, `trait_value`) VALUES (18, 'command', '180');
INSERT INTO `general_slot_trait` (`slot_id`, `trait_type`, `trait_value`) VALUES (18, 'defense', '120');

-- =============================================
-- 注意: 蓝色及以下品质(slot_id 19+)暂无名将特性
-- 如需扩展, 照上面格式追加 INSERT 即可
-- =============================================
