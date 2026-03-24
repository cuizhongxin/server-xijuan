-- =====================================================================
-- 三国策略游戏 - 完整战役系统 v2
-- 12个战役覆盖 Lv1 ~ Lv200
--
-- 关卡数: 战役1-4为7关, 战役5-12为20关
-- NPC阵型: 战役1-2为递增(1→6), 战役3-12为全阵(6NPC/关)
-- 装备掉落: 关联 equipment_pre 表, 按战役等级匹配套装
-- 道具掉落: 关联 item 表, 按战役等级匹配道具池
--
-- 战役列表:
--   1. 黄巾之乱     Lv1-10    5关   解锁Lv1
--   2. 诸侯讨董     Lv10-20   7关   解锁Lv10
--   3. 乱世华雄     Lv20-40   10关  解锁Lv20
--   4. 官渡之战     Lv40-50   15关  解锁Lv40
--   5. 赤壁之战     Lv50-60   20关  解锁Lv50
--   6. 定军山之战   Lv60-80   20关  解锁Lv60
--   7. 战神吕布     Lv80-100  20关  解锁Lv80
--   8. 夷陵烽火     Lv100-120 20关  解锁Lv100 (预留)
--   9. 五丈原       Lv120-140 20关  解锁Lv120 (预留)
--  10. 姜维北伐     Lv140-160 20关  解锁Lv140 (预留)
--  11. 钟会伐蜀     Lv160-180 20关  解锁Lv160 (预留)
--  12. 一统天下     Lv180-200 20关  解锁Lv180 (预留)
--
-- NPC阵型数据由 CampaignService 启动时动态生成（含完整六维属性）
-- 本SQL提供配置表和掉落映射关系
-- =====================================================================


-- =====================================================================
-- 1. 战役配置总表
-- =====================================================================
DROP TABLE IF EXISTS `campaign_config`;
CREATE TABLE IF NOT EXISTS `campaign_config` (
  `campaign_id` VARCHAR(64) NOT NULL COMMENT '战役ID',
  `name` VARCHAR(64) NOT NULL COMMENT '战役名称',
  `description` VARCHAR(512) COMMENT '战役描述',
  `icon` VARCHAR(256) COMMENT '战役图标路径',
  `bg_image` VARCHAR(256) COMMENT '战役背景图路径',
  `enemy_level_min` INT DEFAULT 1 COMMENT '敌人最低等级',
  `enemy_level_max` INT DEFAULT 10 COMMENT '敌人最高等级',
  `required_level` INT DEFAULT 1 COMMENT '解锁所需君主等级',
  `daily_limit` INT DEFAULT 3 COMMENT '每日可挑战次数',
  `stamina_cost` INT DEFAULT 5 COMMENT '体力消耗',
  `stage_count` INT DEFAULT 7 COMMENT '关卡数(7或20)',
  `full_formation` TINYINT(1) DEFAULT 0 COMMENT '是否每关6NPC阵型',
  `faction` VARCHAR(16) COMMENT '敌方阵营(黄巾/西凉/袁军/曹军/吕布/吴军/魏军)',
  `sort_order` INT DEFAULT 0 COMMENT '排序序号',
  `enabled` TINYINT(1) DEFAULT 1 COMMENT '是否开放',
  `reserved` TINYINT(1) DEFAULT 0 COMMENT '是否预留(未完整开放)',
  PRIMARY KEY (`campaign_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='战役配置总表';

INSERT INTO campaign_config VALUES
('campaign_huangjin','黄巾之乱','苍天已死黄天当立！讨伐张角三兄弟','/images/campaign/huangjin.png','/images/campaign/bg_huangjin.jpg',1,10,1,5,4,5,0,'黄巾',1,1,0),
('campaign_dongzhuo','诸侯讨董','十八路诸侯会盟讨董，西凉铁骑势不可挡','/images/campaign/dongzhuo.png','/images/campaign/bg_dongzhuo.jpg',10,20,10,4,5,7,0,'西凉',2,1,0),
('campaign_huaxiong','乱世华雄','华雄威震汜水关，斩杀联军数将','/images/campaign/huaxiong.png','/images/campaign/bg_huaxiong.jpg',20,40,20,3,6,10,1,'西凉',3,1,0),
('campaign_guandu','官渡之战','袁绍携河北四州之众南下，曹操以少胜多','/images/campaign/guandu.png','/images/campaign/bg_guandu.jpg',40,50,40,3,8,15,1,'袁军',4,1,0),
('campaign_chibi','赤壁之战','曹操率八十万大军南下，孙刘联军火烧赤壁','/images/campaign/chibi.png','/images/campaign/bg_chibi.jpg',50,60,50,3,8,20,1,'曹军',5,1,0),
('campaign_dingjun','定军山之战','刘备取汉中，黄忠于定军山阵斩夏侯渊','/images/campaign/dingjun.png','/images/campaign/bg_dingjun.jpg',60,80,60,2,10,20,1,'曹军',6,1,0),
('campaign_lvbu','战神吕布','人中吕布马中赤兔，虎牢关前无人能敌','/images/campaign/lvbu.png','/images/campaign/bg_lvbu.jpg',80,100,80,2,12,20,1,'吕布',7,1,0),
('campaign_yiling','夷陵烽火','刘备为关羽报仇东征孙吴，陆逊火烧连营','/images/campaign/yiling.png','/images/campaign/bg_yiling.jpg',100,120,100,2,14,20,1,'吴军',8,0,1),
('campaign_wuzhang','五丈原','诸葛亮六出祁山，秋风五丈原','/images/campaign/wuzhang.png','/images/campaign/bg_wuzhang.jpg',120,140,120,2,16,20,1,'魏军',9,0,1),
('campaign_jiangwei','姜维北伐','继承丞相遗志，九伐中原','/images/campaign/jiangwei.png','/images/campaign/bg_jiangwei.jpg',140,160,140,2,18,20,1,'魏军',10,0,1),
('campaign_zhonghui','钟会伐蜀','魏国大举伐蜀，蜀汉风雨飘摇','/images/campaign/zhonghui.png','/images/campaign/bg_zhonghui.jpg',160,180,160,2,20,20,1,'魏军',11,0,1),
('campaign_yitong','一统天下','天下分久必合，谁能问鼎天下？','/images/campaign/yitong.png','/images/campaign/bg_yitong.jpg',180,200,180,1,22,20,1,'魏军',12,0,1);


-- =====================================================================
-- 2. 战役装备掉落映射 (campaign等级 → equipment_pre.id)
-- 每个战役可掉落的装备模板，来自 equipment_pre 表中 source='副本掉落'/'副本产出'
-- =====================================================================
DROP TABLE IF EXISTS `campaign_equip_drop`;
CREATE TABLE IF NOT EXISTS `campaign_equip_drop` (
  `id` BIGINT AUTO_INCREMENT,
  `campaign_id` VARCHAR(64) NOT NULL COMMENT '战役ID',
  `equip_pre_id` INT NOT NULL COMMENT '关联 equipment_pre.id',
  `drop_rate_normal` INT DEFAULT 15 COMMENT '普通关掉率(%)',
  `drop_rate_boss` INT DEFAULT 60 COMMENT 'BOSS关掉率(%)',
  PRIMARY KEY (`id`),
  KEY `idx_ced_campaign` (`campaign_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='战役装备掉落映射';

-- 黄巾之乱 → 新手套装 (equipment_pre id 1-6, level=1)
INSERT INTO campaign_equip_drop (campaign_id, equip_pre_id, drop_rate_normal, drop_rate_boss) VALUES
('campaign_huangjin',1,12,60),('campaign_huangjin',2,12,60),('campaign_huangjin',3,12,60),
('campaign_huangjin',4,12,60),('campaign_huangjin',5,12,60),('campaign_huangjin',6,12,60);

-- 诸侯讨董 → 宣武套装 (id 7-12, level=20)
INSERT INTO campaign_equip_drop (campaign_id, equip_pre_id, drop_rate_normal, drop_rate_boss) VALUES
('campaign_dongzhuo',7,12,55),('campaign_dongzhuo',8,12,55),('campaign_dongzhuo',9,12,55),
('campaign_dongzhuo',10,12,55),('campaign_dongzhuo',11,12,55),('campaign_dongzhuo',12,12,55);

-- 乱世华雄 → 陷阵套装 (id 19-24, level=40, 副本掉落)
INSERT INTO campaign_equip_drop (campaign_id, equip_pre_id, drop_rate_normal, drop_rate_boss) VALUES
('campaign_huaxiong',19,10,50),('campaign_huaxiong',20,10,50),('campaign_huaxiong',21,10,50),
('campaign_huaxiong',22,10,50),('campaign_huaxiong',23,10,50),('campaign_huaxiong',24,10,50);

-- 官渡之战 → 狂战套装 (id 31-36, level=50, 副本产出)
INSERT INTO campaign_equip_drop (campaign_id, equip_pre_id, drop_rate_normal, drop_rate_boss) VALUES
('campaign_guandu',31,10,50),('campaign_guandu',32,10,50),('campaign_guandu',33,10,50),
('campaign_guandu',34,10,50),('campaign_guandu',35,10,50),('campaign_guandu',36,10,50);

-- 赤壁之战 → 天狼套装(37-42) + 熊王套装(55-60) (level=60, 副本产出)
INSERT INTO campaign_equip_drop (campaign_id, equip_pre_id, drop_rate_normal, drop_rate_boss) VALUES
('campaign_chibi',37,8,45),('campaign_chibi',38,8,45),('campaign_chibi',39,8,45),
('campaign_chibi',40,8,45),('campaign_chibi',41,8,45),('campaign_chibi',42,8,45),
('campaign_chibi',55,8,45),('campaign_chibi',56,8,45),('campaign_chibi',57,8,45),
('campaign_chibi',58,8,45),('campaign_chibi',59,8,45),('campaign_chibi',60,8,45);

-- 定军山之战 → 熊王套装(55-60) + 雄狮套装(79-84) (level=60-80)
INSERT INTO campaign_equip_drop (campaign_id, equip_pre_id, drop_rate_normal, drop_rate_boss) VALUES
('campaign_dingjun',55,7,40),('campaign_dingjun',56,7,40),('campaign_dingjun',57,7,40),
('campaign_dingjun',58,7,40),('campaign_dingjun',59,7,40),('campaign_dingjun',60,7,40),
('campaign_dingjun',79,7,40),('campaign_dingjun',80,7,40),('campaign_dingjun',81,7,40),
('campaign_dingjun',82,7,40),('campaign_dingjun',83,7,40),('campaign_dingjun',84,7,40);

-- 战神吕布 → 雄狮套装(79-84) + 圣象套装(97-102) (level=80-90)
INSERT INTO campaign_equip_drop (campaign_id, equip_pre_id, drop_rate_normal, drop_rate_boss) VALUES
('campaign_lvbu',79,6,35),('campaign_lvbu',80,6,35),('campaign_lvbu',81,6,35),
('campaign_lvbu',82,6,35),('campaign_lvbu',83,6,35),('campaign_lvbu',84,6,35),
('campaign_lvbu',97,6,35),('campaign_lvbu',98,6,35),('campaign_lvbu',99,6,35),
('campaign_lvbu',100,6,35),('campaign_lvbu',101,6,35),('campaign_lvbu',102,6,35);

-- 战役8-12 (Lv100+) 装备掉落预留，后续开放时再配置


-- =====================================================================
-- 3. 战役道具掉落池 (campaign等级段 → item.item_id)
-- 不同等级段的战役掉落不同档次的道具
-- =====================================================================
DROP TABLE IF EXISTS `campaign_item_pool`;
CREATE TABLE IF NOT EXISTS `campaign_item_pool` (
  `id` BIGINT AUTO_INCREMENT,
  `tier` VARCHAR(16) NOT NULL COMMENT '等级段: low/mid/high/ultra',
  `item_id` INT NOT NULL COMMENT '关联 item.item_id',
  `item_name` VARCHAR(64) COMMENT '道具名称(冗余)',
  `drop_rate` INT DEFAULT 20 COMMENT '掉率(%)',
  `min_count` INT DEFAULT 1 COMMENT '最小数量',
  `max_count` INT DEFAULT 2 COMMENT '最大数量',
  PRIMARY KEY (`id`),
  KEY `idx_cip_tier` (`tier`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='战役道具掉落池';

-- low: 战役1-2 (Lv1-20)
INSERT INTO campaign_item_pool (tier, item_id, item_name, drop_rate, min_count, max_count) VALUES
('low', 1,  '1级强化石',    30, 1, 3),
('low', 14, '初级粮食包',   25, 1, 2),
('low', 17, '初级木材包',   25, 1, 2),
('low', 20, '初级纸张包',   25, 1, 2),
('low', 28, '经验丹(小)',   20, 1, 2),
('low', 32, '初级声望符',   15, 1, 1);

-- mid: 战役3-4 (Lv20-50)
INSERT INTO campaign_item_pool (tier, item_id, item_name, drop_rate, min_count, max_count) VALUES
('mid', 36, '2级强化石',    25, 1, 2),
('mid', 37, '3级强化石',    20, 1, 2),
('mid', 15, '中级粮食包',   25, 1, 2),
('mid', 18, '中级木材包',   25, 1, 2),
('mid', 21, '中级纸张包',   25, 1, 2),
('mid', 7,  '初级招贤令',   15, 1, 1),
('mid', 29, '经验丹(中)',   20, 1, 2),
('mid', 33, '中级声望符',   15, 1, 1);

-- high: 战役5-7 (Lv50-100)
INSERT INTO campaign_item_pool (tier, item_id, item_name, drop_rate, min_count, max_count) VALUES
('high', 2,  '4级强化石',   20, 1, 2),
('high', 38, '5级强化石',   15, 1, 1),
('high', 16, '高级粮食包',  20, 1, 2),
('high', 19, '高级木材包',  20, 1, 2),
('high', 22, '高级纸张包',  20, 1, 2),
('high', 8,  '中级招贤令',  12, 1, 1),
('high', 29, '经验丹(中)',  20, 1, 2),
('high', 30, '经验丹(大)',  10, 1, 1),
('high', 34, '高级声望符',  10, 1, 1);

-- ultra: 战役8-12 (Lv100-200) 道具掉落预留，后续开放时再配置


-- =====================================================================
-- 4. 掉落预览表 (战役列表UI展示)
-- =====================================================================
DROP TABLE IF EXISTS `campaign_drop_preview`;
CREATE TABLE IF NOT EXISTS `campaign_drop_preview` (
  `id` BIGINT AUTO_INCREMENT,
  `campaign_id` VARCHAR(64) NOT NULL,
  `name` VARCHAR(64) COMMENT '物品名称',
  `quality` VARCHAR(16) COMMENT '品质',
  `sort_order` INT DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_cdp_campaign` (`campaign_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='战役掉落预览表';

INSERT INTO campaign_drop_preview (campaign_id, name, quality, sort_order) VALUES
('campaign_huangjin','新手长剑','普通',1),('campaign_huangjin','新手布甲','普通',2),('campaign_huangjin','新手布帽','优秀',3),
('campaign_dongzhuo','宣武长剑','优秀',1),('campaign_dongzhuo','宣武战甲','优秀',2),('campaign_dongzhuo','宣武头盔','精良',3),
('campaign_huaxiong','陷阵长枪','精良',1),('campaign_huaxiong','陷阵重甲','精良',2),('campaign_huaxiong','陷阵头盔','精良',3),
('campaign_guandu','狂战巨斧','史诗',1),('campaign_guandu','狂战重甲','史诗',2),('campaign_guandu','狂战头盔','史诗',3),
('campaign_chibi','天狼战刃','史诗',1),('campaign_chibi','天狼战甲','史诗',2),('campaign_chibi','熊王巨锤','传说',3),
('campaign_dingjun','熊王巨锤','传说',1),('campaign_dingjun','雄狮战刃','传说',2),('campaign_dingjun','雄狮战甲','传说',3),
('campaign_lvbu','雄狮战刃','传说',1),('campaign_lvbu','圣象神兵','传说',2),('campaign_lvbu','方天画戟','传说',3),
('campaign_yiling','玄武战刃','传说',1),('campaign_yiling','玄武战甲','传说',2),
('campaign_wuzhang','玄武战刃','传说',1),('campaign_wuzhang','秘银神剑','传说',2),
('campaign_jiangwei','秘银神剑','传说',1),('campaign_jiangwei','秘银战甲','传说',2),
('campaign_zhonghui','秘银神剑','传说',1),('campaign_zhonghui','秘银战甲','传说',2),
('campaign_yitong','秘银神剑','传说',1),('campaign_yitong','秘银战甲','传说',2);


-- =====================================================================
-- 5. campaign_stage_npc 表结构（保留，数据由 CampaignService 动态生成）
-- =====================================================================
CREATE TABLE IF NOT EXISTS `campaign_stage_npc` (
  `id` BIGINT AUTO_INCREMENT,
  `campaign_id` VARCHAR(64) NOT NULL COMMENT '战役ID',
  `stage_num` INT NOT NULL COMMENT '关卡序号',
  `position` INT NOT NULL COMMENT '阵位(0-5, 0-2前排, 3-5后排)',
  `name` VARCHAR(64) NOT NULL COMMENT 'NPC名称',
  `avatar` VARCHAR(256) DEFAULT NULL COMMENT '头像',
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
  `tactics_id` VARCHAR(64) DEFAULT NULL COMMENT '兵法ID',
  `exp_reward` INT DEFAULT 0 COMMENT '击败经验奖励',
  `silver_reward` INT DEFAULT 0 COMMENT '击败白银奖励',
  `drop_config` TEXT DEFAULT NULL COMMENT '掉落配置JSON',
  PRIMARY KEY (`id`),
  KEY `idx_csn_campaign_stage` (`campaign_id`, `stage_num`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='战役关卡NPC配置';
