-- 兵法系统重构 SQL

-- 1. 新建 user_tactics 表（替代 user_learned_tactics）
CREATE TABLE IF NOT EXISTS `user_tactics` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `user_id` VARCHAR(64) NOT NULL,
  `tactics_id` VARCHAR(32) NOT NULL,
  `level` INT DEFAULT 1,
  `create_time` BIGINT,
  UNIQUE KEY `uk_user_tactics` (`user_id`, `tactics_id`),
  INDEX `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户拥有的兵法（含等级）';

-- 2. General 表加兵法发动概率字段
ALTER TABLE `general` ADD COLUMN IF NOT EXISTS `tactics_trigger_rate` DOUBLE DEFAULT 0 COMMENT '兵法发动概率(%)';

-- 3. 迁移旧数据（将 user_learned_tactics 中逗号分隔的ID迁移到新表，默认等级1）
-- 仅在旧表存在时执行:
-- INSERT IGNORE INTO user_tactics (user_id, tactics_id, level, create_time)
-- SELECT user_id, TRIM(SUBSTRING_INDEX(SUBSTRING_INDEX(tactics_ids, ',', n.n), ',', -1)) as tactics_id, 1, UNIX_TIMESTAMP()*1000
-- FROM user_learned_tactics
-- CROSS JOIN (SELECT 1 n UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5
--             UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9 UNION SELECT 10) n
-- WHERE n.n <= 1 + LENGTH(tactics_ids) - LENGTH(REPLACE(tactics_ids, ',', ''))
-- AND tactics_ids IS NOT NULL AND tactics_ids != '';
