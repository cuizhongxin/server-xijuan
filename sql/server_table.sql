-- ============================================
-- 区服系统
-- ============================================

CREATE TABLE IF NOT EXISTS game_server (
    id INT AUTO_INCREMENT PRIMARY KEY,
    server_name VARCHAR(64) NOT NULL COMMENT '区服名',
    server_status VARCHAR(16) DEFAULT 'normal' COMMENT 'normal/hot/new/maintenance',
    max_players INT DEFAULT 5000,
    current_players INT DEFAULT 0,
    open_time BIGINT COMMENT '开服时间',
    create_time BIGINT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 玩家-区服绑定（记录玩家在哪些区有角色）
CREATE TABLE IF NOT EXISTS player_server (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id VARCHAR(64) NOT NULL,
    server_id INT NOT NULL,
    player_level INT DEFAULT 1,
    last_login BIGINT,
    create_time BIGINT,
    UNIQUE KEY uk_user_server (user_id, server_id),
    INDEX idx_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 预置区服
INSERT INTO game_server (server_name, server_status, open_time, create_time) VALUES
('天下一区', 'hot', UNIX_TIMESTAMP()*1000, UNIX_TIMESTAMP()*1000),
('群雄逐鹿', 'normal', UNIX_TIMESTAMP()*1000, UNIX_TIMESTAMP()*1000),
('三足鼎立', 'normal', UNIX_TIMESTAMP()*1000, UNIX_TIMESTAMP()*1000),
('龙争虎斗', 'new', UNIX_TIMESTAMP()*1000, UNIX_TIMESTAMP()*1000),
('烽火连天', 'new', UNIX_TIMESTAMP()*1000, UNIX_TIMESTAMP()*1000);
