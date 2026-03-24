CREATE TABLE IF NOT EXISTS alliance_boss (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    boss_level INT DEFAULT 1,
    boss_name VARCHAR(32) DEFAULT '远古巨兽',
    max_hp BIGINT DEFAULT 1000000,
    current_hp BIGINT DEFAULT 1000000,
    status VARCHAR(16) DEFAULT 'idle',
    feed_count INT DEFAULT 0,
    feed_target INT DEFAULT 100,
    last_reset_date DATE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS alliance_boss_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id VARCHAR(64) NOT NULL,
    action_type VARCHAR(16) NOT NULL,
    damage BIGINT DEFAULT 0,
    feed_amount INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id),
    INDEX idx_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
