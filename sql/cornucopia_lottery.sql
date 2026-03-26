-- 聚宝盆彩票期数表
CREATE TABLE IF NOT EXISTS cornucopia_period (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    period_num INT NOT NULL COMMENT '期号',
    prize_pool BIGINT DEFAULT 0 COMMENT '当期奖池绑金总额',
    carryover BIGINT DEFAULT 0 COMMENT '上期结转金额',
    status TINYINT DEFAULT 0 COMMENT '0=进行中 1=已开奖',
    draw_time DATETIME NOT NULL COMMENT '开奖时间',
    grand_number VARCHAR(10) DEFAULT NULL COMMENT '特等奖号码',
    first_number VARCHAR(10) DEFAULT NULL COMMENT '一等奖号码',
    grand_winner_id VARCHAR(64) DEFAULT NULL COMMENT '特等奖用户ID',
    first_winner_id VARCHAR(64) DEFAULT NULL COMMENT '一等奖用户ID',
    grand_prize BIGINT DEFAULT 0 COMMENT '特等奖金额',
    first_prize BIGINT DEFAULT 0 COMMENT '一等奖金额',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_period_num (period_num),
    INDEX idx_draw_time (draw_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='聚宝盆彩票期数';

-- 聚宝盆用户购票记录
CREATE TABLE IF NOT EXISTS cornucopia_ticket (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id VARCHAR(64) NOT NULL,
    period_id BIGINT NOT NULL COMMENT '关联期数ID',
    ticket_number VARCHAR(10) NOT NULL COMMENT '系统分配号码',
    purchased_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user_period (user_id, period_id),
    INDEX idx_period (period_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='聚宝盆购票记录';
