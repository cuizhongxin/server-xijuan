-- ============================================
-- 交易市场系统
-- ============================================

DROP TABLE IF EXISTS market_listing;
CREATE TABLE `market_listing` (
    `id`            BIGINT AUTO_INCREMENT PRIMARY KEY,
    `seller_id`     VARCHAR(64)  NOT NULL COMMENT '卖家ID',
    `seller_name`   VARCHAR(64)           COMMENT '卖家名',
    `item_type`     VARCHAR(16)  NOT NULL COMMENT 'equipment / item',
    `item_id`       VARCHAR(128) NOT NULL COMMENT '装备ID或道具itemId',
    `item_name`     VARCHAR(64)           COMMENT '物品名称',
    `item_icon`     VARCHAR(256)          COMMENT '物品图标URL',
    `item_level`    INT          DEFAULT 0 COMMENT '物品等级',
    `item_quality`  INT          DEFAULT 1 COMMENT '品质等级 1-6',
    `item_count`    INT          DEFAULT 1 COMMENT '道具数量(装备固定1)',
    `price`         BIGINT       NOT NULL COMMENT '黄金售价',
    `commission`    BIGINT       NOT NULL DEFAULT 0 COMMENT '佣金(白银)',
    `item_snapshot` TEXT                  COMMENT '物品完整快照JSON',
    `status`        VARCHAR(16)  DEFAULT 'active' COMMENT 'active/sold/cancelled',
    `buyer_id`      VARCHAR(64)           COMMENT '买家ID',
    `buyer_name`    VARCHAR(64)           COMMENT '买家名',
    `create_time`   BIGINT                COMMENT '创建时间',
    `update_time`   BIGINT                COMMENT '更新时间',
    INDEX `idx_seller`  (`seller_id`, `status`),
    INDEX `idx_status`  (`status`, `item_type`),
    INDEX `idx_create`  (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='交易市场挂牌表';

-- 交易记录
DROP TABLE IF EXISTS market_trade_log;
CREATE TABLE `market_trade_log` (
    `id`          BIGINT AUTO_INCREMENT PRIMARY KEY,
    `listing_id`  BIGINT       NOT NULL COMMENT '关联挂牌ID',
    `seller_id`   VARCHAR(64)  NOT NULL COMMENT '卖家ID',
    `buyer_id`    VARCHAR(64)  NOT NULL COMMENT '买家ID',
    `item_type`   VARCHAR(16)           COMMENT 'equipment / item',
    `item_name`   VARCHAR(64)           COMMENT '物品名称',
    `price`       BIGINT                COMMENT '成交价格',
    `create_time` BIGINT                COMMENT '成交时间',
    INDEX `idx_seller` (`seller_id`),
    INDEX `idx_buyer`  (`buyer_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='交易记录表';

-- 聊天消息表
CREATE TABLE IF NOT EXISTS chat_message (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    sender_id VARCHAR(64) NOT NULL,
    sender_name VARCHAR(64),
    channel VARCHAR(16) DEFAULT 'world' COMMENT 'world/alliance/system',
    content VARCHAR(500) NOT NULL,
    create_time BIGINT NOT NULL,
    INDEX idx_channel_time (channel, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 公告表
CREATE TABLE IF NOT EXISTS announcement (
    id INT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(128) NOT NULL,
    content TEXT NOT NULL,
    priority INT DEFAULT 0 COMMENT '优先级越高越靠前',
    active TINYINT(1) DEFAULT 1,
    start_time BIGINT COMMENT '生效开始时间',
    end_time BIGINT COMMENT '生效结束时间',
    create_time BIGINT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO announcement (title, content, priority, active, create_time) VALUES
('欢迎来到三国志·战役', '新版本已上线，英雄榜、市场交易系统全面开放！祝各位主公游戏愉快。', 10, 1, UNIX_TIMESTAMP()*1000),
('交易市场上线', '交易市场已开放，可以挂牌出售装备和道具，使用黄金交易。挂牌需支付佣金（白银），撤销可退还。', 5, 1, UNIX_TIMESTAMP()*1000);
