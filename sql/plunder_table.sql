-- ============================================
-- 掠夺系统数据表
-- ============================================

-- 用户掠夺每日数据（次数/购买信息）
CREATE TABLE IF NOT EXISTS plunder_data (
    user_id VARCHAR(64) PRIMARY KEY,
    today_count INT NOT NULL DEFAULT 0 COMMENT '今日已掠夺次数',
    today_purchased INT NOT NULL DEFAULT 0 COMMENT '今日已购买次数',
    last_reset_date VARCHAR(8) COMMENT '上次重置日期 yyyyMMdd',
    create_time BIGINT,
    update_time BIGINT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 掠夺NPC配置表（阵营NPC基础数据）
CREATE TABLE IF NOT EXISTS plunder_npc (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(32) NOT NULL COMMENT 'NPC名字',
    faction VARCHAR(16) NOT NULL COMMENT '阵营: 西凉/突厥/鲜卑/羌族',
    bonus_resource VARCHAR(16) NOT NULL COMMENT '优势资源: wood/paper/silver/food',
    sort_order INT DEFAULT 0 COMMENT '排序序号(同阵营内)',
    power_base INT DEFAULT 800 COMMENT '战力基础系数(每级)',
    power_extra INT DEFAULT 0 COMMENT '额外战力加成'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 掠夺记录表
CREATE TABLE IF NOT EXISTS plunder_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    attacker_id VARCHAR(64) NOT NULL COMMENT '攻击者用户ID',
    attacker_name VARCHAR(64) COMMENT '攻击者名称',
    attacker_level INT COMMENT '攻击者等级',
    defender_id VARCHAR(64) NOT NULL COMMENT '被攻击者ID(NPC为npc_xxx)',
    defender_name VARCHAR(64) COMMENT '被攻击者名称',
    defender_level INT COMMENT '被攻击者等级',
    defender_faction VARCHAR(16) COMMENT 'NPC阵营,玩家为NULL',
    is_npc TINYINT(1) DEFAULT 0 COMMENT '是否NPC',
    victory TINYINT(1) DEFAULT 0 COMMENT '攻击者是否胜利',
    silver_gain BIGINT DEFAULT 0,
    wood_gain BIGINT DEFAULT 0,
    paper_gain BIGINT DEFAULT 0,
    food_gain BIGINT DEFAULT 0,
    create_time BIGINT NOT NULL COMMENT '记录时间戳(ms)',
    INDEX idx_attacker (attacker_id, create_time),
    INDEX idx_defender (defender_id, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================
-- 初始化NPC数据（4阵营 × 6个NPC）
-- ============================================

-- 西凉（优势资源: 木材）
INSERT INTO plunder_npc (name, faction, bonus_resource, sort_order, power_base, power_extra) VALUES
('董卓', '西凉', 'wood', 1, 800, 0),
('李傕', '西凉', 'wood', 2, 800, 200),
('郭汜', '西凉', 'wood', 3, 800, 400),
('张济', '西凉', 'wood', 4, 800, 600),
('牛辅', '西凉', 'wood', 5, 800, 800),
('华雄', '西凉', 'wood', 6, 800, 1000);

-- 突厥（优势资源: 纸张）
INSERT INTO plunder_npc (name, faction, bonus_resource, sort_order, power_base, power_extra) VALUES
('阿史那骨', '突厥', 'paper', 1, 800, 0),
('阿史那思摩', '突厥', 'paper', 2, 800, 200),
('执失思力', '突厥', 'paper', 3, 800, 400),
('契苾何力', '突厥', 'paper', 4, 800, 600),
('默啜可汗', '突厥', 'paper', 5, 800, 800),
('颉利可汗', '突厥', 'paper', 6, 800, 1000);

-- 鲜卑（优势资源: 白银）
INSERT INTO plunder_npc (name, faction, bonus_resource, sort_order, power_base, power_extra) VALUES
('轲比能', '鲜卑', 'silver', 1, 800, 0),
('素利',   '鲜卑', 'silver', 2, 800, 200),
('弥加',   '鲜卑', 'silver', 3, 800, 400),
('拓跋力微', '鲜卑', 'silver', 4, 800, 600),
('慕容廆', '鲜卑', 'silver', 5, 800, 800),
('段日陆眷', '鲜卑', 'silver', 6, 800, 1000);

-- 羌族（优势资源: 粮食）
INSERT INTO plunder_npc (name, faction, bonus_resource, sort_order, power_base, power_extra) VALUES
('迷当大王', '羌族', 'food', 1, 800, 0),
('烧戈',     '羌族', 'food', 2, 800, 200),
('伐同',     '羌族', 'food', 3, 800, 400),
('注诣',     '羌族', 'food', 4, 800, 600),
('姚弋仲',   '羌族', 'food', 5, 800, 800),
('杨难当',   '羌族', 'food', 6, 800, 1000);
