-- ============================================
-- 英雄榜 + 爵位 + 兵种升级 系统
-- ============================================

-- 英雄榜排行数据（战力排名）
CREATE TABLE IF NOT EXISTS hero_rank (
    user_id VARCHAR(64) PRIMARY KEY,
    user_name VARCHAR(64),
    level INT DEFAULT 1,
    power INT DEFAULT 0 COMMENT '综合战力',
    fame BIGINT DEFAULT 0 COMMENT '声望',
    rank_name VARCHAR(16) COMMENT '爵位名称',
    ranking INT DEFAULT 0 COMMENT '排名(每日结算更新)',
    today_challenge INT DEFAULT 0 COMMENT '今日挑战次数',
    today_wins INT DEFAULT 0 COMMENT '今日胜利次数',
    today_purchased INT DEFAULT 0 COMMENT '今日已购买次数',
    last_reset_date VARCHAR(8),
    last_challenge_time BIGINT DEFAULT 0 COMMENT '上次挑战时间戳(ms)',
    update_time BIGINT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 英雄榜挑战记录
CREATE TABLE IF NOT EXISTS hero_rank_battle (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    attacker_id VARCHAR(64) NOT NULL,
    attacker_name VARCHAR(64),
    attacker_level INT,
    defender_id VARCHAR(64) NOT NULL,
    defender_name VARCHAR(64),
    defender_level INT,
    victory TINYINT(1) DEFAULT 0,
    fame_gain BIGINT DEFAULT 0 COMMENT '获得声望',
    create_time BIGINT NOT NULL,
    create_date VARCHAR(8),
    INDEX idx_attacker (attacker_id, create_date),
    INDEX idx_defender (defender_id, create_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 每日排名奖励发放记录
CREATE TABLE IF NOT EXISTS hero_rank_reward_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id VARCHAR(64) NOT NULL,
    ranking INT NOT NULL,
    fame_reward BIGINT DEFAULT 0,
    silver_reward BIGINT DEFAULT 0,
    settle_date VARCHAR(8) NOT NULL COMMENT '结算日期',
    create_time BIGINT,
    INDEX idx_user (user_id),
    INDEX idx_date (settle_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 爵位配置表
CREATE TABLE IF NOT EXISTS peerage_config (
    id INT AUTO_INCREMENT PRIMARY KEY,
    rank_name VARCHAR(16) NOT NULL COMMENT '爵位名称',
    fame_required BIGINT NOT NULL COMMENT '需要声望',
    level_required INT NOT NULL COMMENT '需要等级',
    max_soldier_tier INT DEFAULT 1 COMMENT '解锁兵种最高阶',
    color VARCHAR(16) DEFAULT '#ffffff',
    sort_order INT DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO peerage_config (rank_name, fame_required, level_required, max_soldier_tier, color, sort_order) VALUES
('白身',   0,      1,  1, '#aaaaaa', 1),
('平民',   5000,   10, 2, '#55cc55', 2),
('士人',   10000,  20, 3, '#55cc55', 3),
('男',     20000,  30, 4, '#5588ff', 4),
('子',     50000,  40, 5, '#5588ff', 5),
('伯',     100000, 50, 6, '#bb55ff', 6),
('侯',     200000, 60, 7, '#bb55ff', 7),
('公',     500000, 80, 8, '#ffaa00', 8),
('王',     1000000,100,9, '#ff4444', 9);

-- 兵种阶级配置表
CREATE TABLE IF NOT EXISTS soldier_tier (
    id INT AUTO_INCREMENT PRIMARY KEY,
    tier INT NOT NULL COMMENT '兵阶(1-9)',
    troop_category VARCHAR(16) NOT NULL COMMENT '兵种大类: 步/骑/弓',
    name VARCHAR(32) NOT NULL COMMENT '兵种名称',
    icon VARCHAR(16),
    power_multiplier DOUBLE DEFAULT 1.0 COMMENT '战力加成倍率',
    upgrade_silver BIGINT DEFAULT 0 COMMENT '升级所需白银',
    peerage_required VARCHAR(16) COMMENT '需要爵位',
    description VARCHAR(128)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 步兵线
INSERT INTO soldier_tier (tier, troop_category, name, icon, power_multiplier, upgrade_silver, peerage_required, description) VALUES
(1, '步', '民兵',     '🛡', 1.0, 0,      '白身', '最基础的步兵'),
(2, '步', '轻步兵',   '🛡', 1.15, 5000,   '平民', '装备简单甲胄'),
(3, '步', '重步兵',   '🛡', 1.35, 15000,  '士人', '身披重甲'),
(4, '步', '刀盾兵',   '🛡', 1.55, 30000,  '男',   '刀盾协同防御'),
(5, '步', '陷阵营',   '🛡', 1.80, 60000,  '子',   '高顺所创精锐'),
(6, '步', '虎卫军',   '🛡', 2.10, 100000, '伯',   '曹操亲卫精锐'),
(7, '步', '白毦兵',   '🛡', 2.45, 200000, '侯',   '刘备精锐护卫'),
(8, '步', '大戟士',   '🛡', 2.85, 400000, '公',   '袁绍麾下精锐'),
(9, '步', '先登死士', '🛡', 3.30, 800000, '王',   '无畏先登之士');

-- 骑兵线
INSERT INTO soldier_tier (tier, troop_category, name, icon, power_multiplier, upgrade_silver, peerage_required, description) VALUES
(1, '骑', '轻骑',     '🐎', 1.0, 0,      '白身', '基础骑兵'),
(2, '骑', '游骑兵',   '🐎', 1.15, 5000,   '平民', '灵活游击'),
(3, '骑', '突骑',     '🐎', 1.35, 15000,  '士人', '突击先锋'),
(4, '骑', '铁骑',     '🐎', 1.55, 30000,  '男',   '铠甲骑兵'),
(5, '骑', '虎豹骑',   '🐎', 1.80, 60000,  '子',   '曹操虎豹精骑'),
(6, '骑', '白马义从', '🐎', 2.10, 100000, '伯',   '公孙瓒精锐骑'),
(7, '骑', '西凉铁骑', '🐎', 2.45, 200000, '侯',   '西凉精锐骑兵'),
(8, '骑', '并州狼骑', '🐎', 2.85, 400000, '公',   '吕布麾下精骑'),
(9, '骑', '飞熊军',   '🐎', 3.30, 800000, '王',   '董卓麾下飞熊');

-- 弓兵线
INSERT INTO soldier_tier (tier, troop_category, name, icon, power_multiplier, upgrade_silver, peerage_required, description) VALUES
(1, '弓', '弓手',     '🏹', 1.0, 0,      '白身', '基础弓手'),
(2, '弓', '弩手',     '🏹', 1.15, 5000,   '平民', '使用弩机'),
(3, '弓', '连弩兵',   '🏹', 1.35, 15000,  '士人', '诸葛连弩兵'),
(4, '弓', '神射手',   '🏹', 1.55, 30000,  '男',   '百步穿杨'),
(5, '弓', '无当飞军', '🏹', 1.80, 60000,  '子',   '王平训练精兵'),
(6, '弓', '元戎弩兵', '🏹', 2.10, 100000, '伯',   '诸葛亮元戎弩'),
(7, '弓', '破军弩手', '🏹', 2.45, 200000, '侯',   '大型弩机操手'),
(8, '弓', '天雷弓手', '🏹', 2.85, 400000, '公',   '掌握火矢技术'),
(9, '弓', '射声营',   '🏹', 3.30, 800000, '王',   '百发百中射声');

-- 声望符道具（插入到 item 表）
-- 如果item表已存在，直接INSERT；如果不存在则需先建表
INSERT INTO item (item_id, item_name, quality) VALUES
(901, '初级声望符', 2),
(902, '中级声望符', 3),
(903, '高级声望符', 4),
(904, '极品声望符', 5)
ON DUPLICATE KEY UPDATE item_name = VALUES(item_name);

-- 声望符配置表（声望符效果）
CREATE TABLE IF NOT EXISTS fame_token_config (
    item_id INT PRIMARY KEY COMMENT '对应item表的item_id',
    fame_amount BIGINT NOT NULL COMMENT '使用后增加的声望',
    name VARCHAR(32)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO fame_token_config VALUES
(901, 500,   '初级声望符'),
(902, 2000,  '中级声望符'),
(903, 8000,  '高级声望符'),
(904, 30000, '极品声望符');
