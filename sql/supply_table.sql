-- ============================================
-- 军需运送系统数据表
-- ============================================

-- 军需等级配置表
CREATE TABLE IF NOT EXISTS supply_grade (
    id INT PRIMARY KEY,
    name VARCHAR(16) NOT NULL COMMENT '等级名称',
    color VARCHAR(16) NOT NULL COMMENT '品质颜色',
    silver_base BIGINT NOT NULL COMMENT '白银基础值',
    silver_level_mult INT NOT NULL COMMENT '白银等级系数',
    silver_rand_max INT NOT NULL COMMENT '白银随机上限',
    other_base BIGINT NOT NULL COMMENT '其他资源基础值',
    other_level_mult INT NOT NULL COMMENT '其他资源等级系数',
    other_rand_max INT NOT NULL COMMENT '其他资源随机上限',
    transport_minutes INT NOT NULL COMMENT '运送时长(分钟)',
    refresh_probability DOUBLE NOT NULL COMMENT '初始刷出概率'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO supply_grade VALUES
(1, '普通', '#aaaaaa', 10000,  100,  1000,  3000,  100,  1000,  30, 0.70),
(2, '初级', '#55cc55', 25000,  200,  2500,  8000,  200,  2500,  45, 0.18),
(3, '中级', '#5588ff', 50000,  300,  5000, 15000,  300,  5000,  60, 0.08),
(4, '高级', '#bb55ff', 75000,  400,  7500, 22000,  400,  7500,  90, 0.03),
(5, '顶级', '#ffaa00', 100000, 500, 10000, 30000,  500, 10000, 120, 0.01);

-- 军需运送记录表
CREATE TABLE IF NOT EXISTS supply_transport (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id VARCHAR(64) NOT NULL,
    grade_id INT NOT NULL,
    grade_name VARCHAR(16),
    silver_reward BIGINT DEFAULT 0,
    paper_reward BIGINT DEFAULT 0,
    food_reward BIGINT DEFAULT 0,
    metal_reward BIGINT DEFAULT 0,
    start_time BIGINT NOT NULL,
    end_time BIGINT NOT NULL,
    speed_up_minutes INT DEFAULT 0,
    robbed_count INT DEFAULT 0,
    robbed_silver BIGINT DEFAULT 0,
    robbed_paper BIGINT DEFAULT 0,
    robbed_food BIGINT DEFAULT 0,
    robbed_metal BIGINT DEFAULT 0,
    status VARCHAR(16) DEFAULT 'active' COMMENT 'active/collected',
    create_date VARCHAR(8) NOT NULL,
    INDEX idx_user_date (user_id, create_date),
    INDEX idx_status (status, end_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 军需抢夺记录表
CREATE TABLE IF NOT EXISTS supply_robbery (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    attacker_id VARCHAR(64) NOT NULL,
    attacker_name VARCHAR(64),
    defender_id VARCHAR(64) NOT NULL,
    defender_name VARCHAR(64),
    transport_id BIGINT NOT NULL,
    grade_name VARCHAR(16),
    victory TINYINT(1) DEFAULT 0,
    silver_stolen BIGINT DEFAULT 0,
    paper_stolen BIGINT DEFAULT 0,
    food_stolen BIGINT DEFAULT 0,
    metal_stolen BIGINT DEFAULT 0,
    create_time BIGINT NOT NULL,
    create_date VARCHAR(8) NOT NULL,
    INDEX idx_attacker (attacker_id, create_date),
    INDEX idx_defender (defender_id, create_date),
    INDEX idx_transport (transport_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 用户军需每日数据
CREATE TABLE IF NOT EXISTS supply_data (
    user_id VARCHAR(64) PRIMARY KEY,
    today_transport INT DEFAULT 0 COMMENT '今日运送次数',
    today_robbery INT DEFAULT 0 COMMENT '今日抢夺次数',
    current_grade_id INT DEFAULT 1 COMMENT '当前刷出的军需等级',
    refresh_tokens INT DEFAULT 0 COMMENT '军需令数量',
    last_reset_date VARCHAR(8),
    update_time BIGINT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
