-- =============================================
-- 日常任务系统 — DDL
-- =============================================

-- 每日任务进度（每日0点重置）
CREATE TABLE IF NOT EXISTS `daily_task_progress` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `user_id` VARCHAR(64) NOT NULL,
  `task_date` DATE NOT NULL,
  `task_type` VARCHAR(32) NOT NULL COMMENT 'campaign/plunder/training/supply/recruit/boss/enhance/produce/herorank',
  `progress` INT DEFAULT 0,
  `claimed` TINYINT(1) DEFAULT 0,
  UNIQUE KEY `uk_dtp_user_date_type` (`user_id`, `task_date`, `task_type`),
  INDEX `idx_dtp_user_date` (`user_id`, `task_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='每日任务进度';

-- 阶段奖励领取记录
CREATE TABLE IF NOT EXISTS `daily_stage_claim` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `user_id` VARCHAR(64) NOT NULL,
  `claim_date` DATE NOT NULL,
  `stage` INT NOT NULL COMMENT '3/6/9',
  INDEX `idx_dsc_user_date` (`user_id`, `claim_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='每日阶段奖励领取';

-- 一次性成就进度
CREATE TABLE IF NOT EXISTS `achievement_progress` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `user_id` VARCHAR(64) NOT NULL,
  `achievement_type` VARCHAR(32) NOT NULL COMMENT 'level_10/campaign_1/orange_3 等',
  `claimed` TINYINT(1) DEFAULT 0,
  UNIQUE KEY `uk_ap_user_ach` (`user_id`, `achievement_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='一次性成就';

-- UserResource 新增精力恢复时间戳字段
ALTER TABLE `user_resource` ADD COLUMN IF NOT EXISTS `last_stamina_recover_time` BIGINT DEFAULT 0
  COMMENT '上次精力恢复计算时间戳(ms)' AFTER `max_stamina`;
