-- 签到系统表

CREATE TABLE IF NOT EXISTS sign_in_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id VARCHAR(64) NOT NULL COMMENT '用户ID',
    sign_date DATE NOT NULL COMMENT '签到日期',
    is_makeup TINYINT(1) DEFAULT 0 COMMENT '是否为补签 0=正常签到 1=补签',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY uk_user_date (user_id, sign_date),
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='签到记录表';
