-- 剧情/引导进度表
CREATE TABLE IF NOT EXISTS `story_progress` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `user_id` VARCHAR(64) NOT NULL COMMENT '用户ID',
  `server_id` INT NOT NULL DEFAULT 1 COMMENT '区服ID',
  `current_node` INT NOT NULL DEFAULT 110 COMMENT '当前剧情节点ID',
  `completed` TINYINT(1) DEFAULT 0 COMMENT '是否已全部完成',
  `create_time` BIGINT COMMENT '创建时间戳',
  `update_time` BIGINT COMMENT '更新时间戳',
  UNIQUE KEY `uk_user_server` (`user_id`, `server_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='剧情/新手引导进度';
