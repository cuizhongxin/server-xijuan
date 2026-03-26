-- 聊天系统多频道升级
ALTER TABLE `chat_message` ADD COLUMN IF NOT EXISTS `nation_id` VARCHAR(16) DEFAULT NULL
  COMMENT '国家ID(nation频道用)' AFTER `target_id`;
ALTER TABLE `chat_message` ADD COLUMN IF NOT EXISTS `alliance_id` VARCHAR(64) DEFAULT NULL
  COMMENT '联盟ID(alliance频道用)' AFTER `nation_id`;

-- 为频道查询加索引
ALTER TABLE `chat_message` ADD INDEX IF NOT EXISTS `idx_cm_nation` (`channel`, `server_id`, `nation_id`, `create_time`);
ALTER TABLE `chat_message` ADD INDEX IF NOT EXISTS `idx_cm_alliance` (`channel`, `server_id`, `alliance_id`, `create_time`);
ALTER TABLE `chat_message` ADD INDEX IF NOT EXISTS `idx_cm_private` (`channel`, `server_id`, `sender_id`, `target_id`, `create_time`);
