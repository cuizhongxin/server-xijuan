-- 邮件系统表
-- 支持系统邮件（带附件奖励）和玩家间邮件

CREATE TABLE IF NOT EXISTS mail (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    sender_id VARCHAR(64) NOT NULL COMMENT '发送者ID，系统邮件为system',
    sender_name VARCHAR(64) DEFAULT '系统' COMMENT '发送者名称',
    receiver_id VARCHAR(64) NOT NULL COMMENT '接收者ID',
    mail_type VARCHAR(16) NOT NULL DEFAULT 'player' COMMENT 'system=系统邮件, player=玩家邮件',
    title VARCHAR(128) NOT NULL COMMENT '邮件标题',
    content TEXT COMMENT '邮件内容',
    has_attachment TINYINT(1) DEFAULT 0 COMMENT '是否有附件',
    attachment_claimed TINYINT(1) DEFAULT 0 COMMENT '附件是否已领取',
    is_read TINYINT(1) DEFAULT 0 COMMENT '是否已读',
    create_time BIGINT NOT NULL COMMENT '发送时间',
    expire_time BIGINT DEFAULT 0 COMMENT '过期时间，0=永不过期',
    deleted TINYINT(1) DEFAULT 0 COMMENT '是否已删除',
    INDEX idx_receiver (receiver_id, deleted, create_time),
    INDEX idx_sender (sender_id, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='邮件表';

CREATE TABLE IF NOT EXISTS mail_attachment (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    mail_id BIGINT NOT NULL COMMENT '关联邮件ID',
    item_type VARCHAR(16) NOT NULL COMMENT 'silver/gold/food/wood/paper/item/equipment',
    item_id VARCHAR(128) DEFAULT '' COMMENT '道具ID（item/equipment类型时使用）',
    item_name VARCHAR(64) NOT NULL COMMENT '物品名称',
    item_quality VARCHAR(8) DEFAULT '1' COMMENT '品质',
    count INT NOT NULL DEFAULT 1 COMMENT '数量',
    INDEX idx_mail (mail_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='邮件附件表';
