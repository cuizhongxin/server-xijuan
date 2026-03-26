-- ========== 世界Boss 建表（从内存迁移到DB） ==========

CREATE TABLE IF NOT EXISTS world_boss_state (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    server_id INT NOT NULL COMMENT '区服ID',
    boss_id INT NOT NULL COMMENT 'Boss模板ID (1001/2001/3001)',
    status VARCHAR(16) NOT NULL DEFAULT 'waiting' COMMENT 'waiting/active/killed/escaped',
    unit_soldiers VARCHAR(256) NOT NULL DEFAULT '1000,1000,1000,1000,1000,1000' COMMENT '各单元剩余兵力(逗号分隔)',
    last_killer VARCHAR(64) DEFAULT '' COMMENT '最后一击玩家ID',
    window_start_ms BIGINT DEFAULT 0 COMMENT '当前窗口开始时间戳',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_server_boss (server_id, boss_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS world_boss_damage (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    server_id INT NOT NULL COMMENT '区服ID',
    boss_id INT NOT NULL COMMENT 'Boss模板ID',
    user_id VARCHAR(64) NOT NULL COMMENT '玩家ID',
    total_damage BIGINT DEFAULT 0 COMMENT '本轮累计伤害',
    attack_count INT DEFAULT 0 COMMENT '本轮攻击次数',
    cooldown_until BIGINT DEFAULT 0 COMMENT '冷却结束时间戳(ms)',
    window_start_ms BIGINT DEFAULT 0 COMMENT '所属窗口(用于区分轮次)',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_server_boss_user_window (server_id, boss_id, user_id, window_start_ms),
    INDEX idx_server_boss_damage (server_id, boss_id, window_start_ms, total_damage)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ========== 联盟Boss 区服隔离 ==========

ALTER TABLE alliance_boss ADD COLUMN server_id INT NOT NULL DEFAULT 1 COMMENT '区服ID';
ALTER TABLE alliance_boss ADD INDEX idx_server_id (server_id);

ALTER TABLE alliance_boss_record ADD COLUMN server_id INT NOT NULL DEFAULT 1 COMMENT '区服ID';
ALTER TABLE alliance_boss_record ADD INDEX idx_server_id (server_id);
