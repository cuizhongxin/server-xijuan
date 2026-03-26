-- 签到累签里程碑领取记录表
CREATE TABLE IF NOT EXISTS sign_in_milestone (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id VARCHAR(64) NOT NULL COMMENT '用户ID',
    `year_month` VARCHAR(7) NOT NULL COMMENT '年月 e.g. 2025-01',
    milestone INT NOT NULL COMMENT '累签天数里程碑 10/20/30',
    reward_amount BIGINT DEFAULT 0 COMMENT '奖励绑金数量',
    claimed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '领取时间',
    UNIQUE KEY uk_user_month_milestone (user_id, `year_month`, milestone),
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='签到累签里程碑领取记录';
