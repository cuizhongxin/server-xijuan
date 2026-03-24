-- ============================================
-- 战役关卡NPC配置表
-- 每个战役的每一关可以有多个NPC(对应阵型的6个位置)
-- ============================================

CREATE TABLE IF NOT EXISTS `campaign_stage_npc` (
  `id` BIGINT AUTO_INCREMENT,
  `campaign_id` VARCHAR(64) NOT NULL COMMENT '战役ID',
  `stage_num` INT NOT NULL COMMENT '关卡序号(1-7)',
  `position` INT NOT NULL COMMENT '阵位(0-5, 0-2前排, 3-5后排)',
  `name` VARCHAR(64) NOT NULL COMMENT 'NPC名称',
  `avatar` VARCHAR(256) DEFAULT NULL COMMENT '头像存储路径',
  `level` INT DEFAULT 1 COMMENT '等级',
  `troop_type` VARCHAR(4) DEFAULT '步' COMMENT '兵种: 步/骑/弓',
  `soldier_count` INT DEFAULT 100 COMMENT '士兵数',
  `soldier_tier` INT DEFAULT 1 COMMENT '兵阶(1-9)',
  `attack` INT DEFAULT 50 COMMENT '攻击力',
  `defense` INT DEFAULT 30 COMMENT '防御力',
  `valor` INT DEFAULT 10 COMMENT '武勇(暴击)',
  `command` INT DEFAULT 10 COMMENT '统御(伤害加成)',
  `dodge` INT DEFAULT 5 COMMENT '闪避',
  `mobility` INT DEFAULT 50 COMMENT '机动',
  `hp` INT DEFAULT 1000 COMMENT '武将HP',
  `is_boss` TINYINT(1) DEFAULT 0 COMMENT '是否BOSS',
  `tactics_id` VARCHAR(64) DEFAULT NULL COMMENT '装备的兵法ID',
  `exp_reward` INT DEFAULT 0 COMMENT '击败经验奖励',
  `silver_reward` INT DEFAULT 0 COMMENT '击败白银奖励',
  `drop_config` TEXT DEFAULT NULL COMMENT '掉落配置JSON',
  PRIMARY KEY (`id`),
  KEY `idx_csn_campaign_stage` (`campaign_id`, `stage_num`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='战役关卡NPC配置';

-- ============================================
-- 扩展 campaign_progress 表: 支持阵型兵力持久化
-- current_troops_json 存储每个武将的剩余兵力 {"generalId1": 80, "generalId2": 100, ...}
-- ============================================

ALTER TABLE `campaign_progress`
  ADD COLUMN `current_troops_json` TEXT DEFAULT NULL COMMENT '阵型中各武将剩余兵力JSON' AFTER `current_troops`;

-- ============================================
-- 初始数据: 乱世枭雄 (campaign_1) 7关NPC
-- 难度递增，每关1-6个NPC
-- ============================================

-- 第1关: 1个NPC
INSERT INTO campaign_stage_npc (campaign_id, stage_num, position, name, avatar, level, troop_type, soldier_count, soldier_tier, attack, defense, valor, command, dodge, mobility, hp, is_boss, exp_reward, silver_reward) VALUES
('campaign_1', 1, 0, '黄巾贼兵', '黄巾贼兵.png', 3, '步', 60, 1, 30, 20, 5, 5, 2, 40, 600, 0, 50, 100);

-- 第2关: 2个NPC
INSERT INTO campaign_stage_npc (campaign_id, stage_num, position, name, avatar, level, troop_type, soldier_count, soldier_tier, attack, defense, valor, command, dodge, mobility, hp, is_boss, exp_reward, silver_reward) VALUES
('campaign_1', 2, 0, '黄巾弓手', '黄巾弓手.png', 5, '弓', 70, 1, 40, 20, 6, 5, 3, 45, 700, 0, 60, 120),
('campaign_1', 2, 1, '黄巾刀兵', '黄巾刀兵.png', 5, '步', 80, 1, 35, 30, 5, 6, 2, 40, 800, 0, 60, 120);

-- 第3关: 3个NPC
INSERT INTO campaign_stage_npc (campaign_id, stage_num, position, name, avatar, level, troop_type, soldier_count, soldier_tier, attack, defense, valor, command, dodge, mobility, hp, is_boss, exp_reward, silver_reward) VALUES
('campaign_1', 3, 0, '黄巾骑兵', '黄巾骑兵.png', 8, '骑', 90, 1, 55, 30, 8, 7, 4, 60, 900, 0, 80, 160),
('campaign_1', 3, 1, '黄巾枪兵', '黄巾枪兵.png', 7, '步', 85, 1, 45, 40, 6, 6, 3, 42, 850, 0, 70, 140),
('campaign_1', 3, 2, '黄巾射手', '黄巾射手.png', 7, '弓', 80, 1, 50, 25, 7, 5, 3, 48, 800, 0, 70, 140);

-- 第4关: 4个NPC(2行×2列)
INSERT INTO campaign_stage_npc (campaign_id, stage_num, position, name, avatar, level, troop_type, soldier_count, soldier_tier, attack, defense, valor, command, dodge, mobility, hp, is_boss, exp_reward, silver_reward) VALUES
('campaign_1', 4, 0, '黄巾校尉', '黄巾校尉.png', 10, '步', 110, 2, 65, 50, 10, 8, 4, 50, 1100, 0, 100, 200),
('campaign_1', 4, 1, '黄巾弓将', '黄巾弓将.png', 10, '弓', 100, 2, 70, 35, 9, 7, 5, 55, 1000, 0, 100, 200),
('campaign_1', 4, 3, '黄巾马军', '黄巾马军.png', 9, '骑', 95, 1, 60, 35, 8, 7, 4, 65, 950, 0, 90, 180),
('campaign_1', 4, 4, '黄巾盾兵', '黄巾盾兵.png', 9, '步', 100, 1, 45, 55, 7, 8, 3, 40, 1000, 0, 90, 180);

-- 第5关: 5个NPC
INSERT INTO campaign_stage_npc (campaign_id, stage_num, position, name, avatar, level, troop_type, soldier_count, soldier_tier, attack, defense, valor, command, dodge, mobility, hp, is_boss, exp_reward, silver_reward) VALUES
('campaign_1', 5, 0, '黄巾渠帅', '黄巾渠帅.png', 13, '步', 130, 2, 80, 60, 12, 10, 5, 52, 1300, 0, 120, 250),
('campaign_1', 5, 1, '黄巾火弓', '黄巾火弓.png', 12, '弓', 120, 2, 85, 40, 10, 8, 5, 58, 1200, 0, 110, 220),
('campaign_1', 5, 2, '黄巾铁骑', '黄巾铁骑.png', 12, '骑', 115, 2, 75, 45, 10, 9, 6, 70, 1150, 0, 110, 220),
('campaign_1', 5, 3, '黄巾力士', '黄巾力士.png', 11, '步', 110, 2, 70, 65, 8, 8, 3, 45, 1100, 0, 100, 200),
('campaign_1', 5, 4, '黄巾妖兵', '黄巾妖兵.png', 11, '弓', 105, 2, 80, 35, 9, 7, 5, 55, 1050, 0, 100, 200);

-- 第6关: 6个NPC(满阵)
INSERT INTO campaign_stage_npc (campaign_id, stage_num, position, name, avatar, level, troop_type, soldier_count, soldier_tier, attack, defense, valor, command, dodge, mobility, hp, is_boss, exp_reward, silver_reward) VALUES
('campaign_1', 6, 0, '张梁', '张梁.png', 16, '步', 160, 3, 100, 75, 14, 12, 6, 55, 1600, 0, 150, 300),
('campaign_1', 6, 1, '黄巾猛将', '黄巾猛将.png', 15, '骑', 150, 2, 95, 55, 12, 10, 6, 72, 1500, 0, 140, 280),
('campaign_1', 6, 2, '黄巾神射', '黄巾神射.png', 15, '弓', 140, 2, 100, 45, 13, 9, 7, 60, 1400, 0, 140, 280),
('campaign_1', 6, 3, '黄巾精锐', '黄巾精锐.png', 14, '步', 140, 2, 85, 70, 10, 10, 5, 48, 1400, 0, 130, 260),
('campaign_1', 6, 4, '黄巾铁骑', '黄巾铁骑.png', 14, '骑', 135, 2, 80, 50, 10, 9, 6, 68, 1350, 0, 130, 260),
('campaign_1', 6, 5, '黄巾长弓', '黄巾长弓.png', 13, '弓', 130, 2, 90, 40, 11, 8, 5, 56, 1300, 0, 120, 240);

-- 第7关: BOSS关 张角 + 护卫
INSERT INTO campaign_stage_npc (campaign_id, stage_num, position, name, avatar, level, troop_type, soldier_count, soldier_tier, attack, defense, valor, command, dodge, mobility, hp, is_boss, tactics_id, exp_reward, silver_reward, drop_config) VALUES
('campaign_1', 7, 0, '张角', '张角.png', 20, '弓', 200, 3, 130, 80, 18, 15, 8, 60, 2500, 1, 't_archer_2', 300, 600, '[{"type":"EQUIPMENT","itemId":"equip_boss","quality":"精良","dropRate":50,"minCount":1,"maxCount":1}]'),
('campaign_1', 7, 1, '张宝', '张宝.png', 18, '步', 180, 3, 110, 90, 14, 12, 6, 52, 2000, 0, NULL, 200, 400, NULL),
('campaign_1', 7, 2, '张梁', '张梁.png', 18, '骑', 170, 3, 115, 70, 15, 11, 7, 75, 1900, 0, NULL, 200, 400, NULL),
('campaign_1', 7, 3, '黄巾力士', '黄巾力士.png', 16, '步', 150, 2, 90, 75, 10, 10, 5, 48, 1600, 0, NULL, 150, 300, NULL),
('campaign_1', 7, 4, '黄巾精骑', '黄巾精骑.png', 16, '骑', 145, 2, 95, 55, 11, 9, 6, 70, 1500, 0, NULL, 150, 300, NULL),
('campaign_1', 7, 5, '黄巾神射', '黄巾神射.png', 15, '弓', 140, 2, 100, 45, 12, 8, 6, 58, 1400, 0, NULL, 140, 280, NULL);
