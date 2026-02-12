-- 原有的 Counters 表
CREATE TABLE IF NOT EXISTS `Counters` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `count` int(11) NOT NULL DEFAULT '1',
  `createdAt` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updatedAt` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8;

-- =============================================
-- 以下是游戏业务数据表
-- =============================================

-- 1. 用户ID映射表（openId <-> userId）
CREATE TABLE IF NOT EXISTS `user_id_mapping` (
  `open_id` VARCHAR(128) NOT NULL,
  `user_id` BIGINT NOT NULL AUTO_INCREMENT,
  PRIMARY KEY (`open_id`),
  UNIQUE KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4;

-- 2. 用户资源表
CREATE TABLE IF NOT EXISTS `user_resource` (
  `od_user_id` VARCHAR(64) NOT NULL,
  `data` LONGTEXT,
  `create_time` BIGINT,
  `update_time` BIGINT,
  PRIMARY KEY (`od_user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 3. 用户等级表
CREATE TABLE IF NOT EXISTS `user_level` (
  `user_id` VARCHAR(64) NOT NULL,
  `data` LONGTEXT,
  `create_time` BIGINT,
  `update_time` BIGINT,
  PRIMARY KEY (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 4. 用户材料表
CREATE TABLE IF NOT EXISTS `user_material` (
  `user_id` VARCHAR(64) NOT NULL,
  `material_id` VARCHAR(64) NOT NULL,
  `count` INT NOT NULL DEFAULT 0,
  PRIMARY KEY (`user_id`, `material_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 5. 武将表
CREATE TABLE IF NOT EXISTS `general` (
  `id` VARCHAR(64) NOT NULL,
  `user_id` VARCHAR(64) NOT NULL,
  `data` LONGTEXT,
  `create_time` BIGINT,
  `update_time` BIGINT,
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 6. 装备表
CREATE TABLE IF NOT EXISTS `equipment` (
  `id` VARCHAR(64) NOT NULL,
  `user_id` VARCHAR(64) NOT NULL,
  `data` LONGTEXT,
  `create_time` BIGINT,
  `update_time` BIGINT,
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 7. 阵型表
CREATE TABLE IF NOT EXISTS `formation` (
  `id` VARCHAR(64) NOT NULL,
  `od_user_id` VARCHAR(64) NOT NULL,
  `data` LONGTEXT,
  `create_time` BIGINT,
  `update_time` BIGINT,
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`od_user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 8. 仓库表
CREATE TABLE IF NOT EXISTS `warehouse` (
  `id` VARCHAR(64) NOT NULL,
  `user_id` VARCHAR(64) NOT NULL,
  `data` LONGTEXT,
  `create_time` BIGINT,
  `update_time` BIGINT,
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 9. 战役进度表
CREATE TABLE IF NOT EXISTS `campaign_progress` (
  `user_id` VARCHAR(64) NOT NULL,
  `campaign_id` VARCHAR(64) NOT NULL,
  `data` LONGTEXT,
  `update_time` BIGINT,
  PRIMARY KEY (`user_id`, `campaign_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 10. 副本进度表
CREATE TABLE IF NOT EXISTS `dungeon_progress` (
  `user_id` VARCHAR(64) NOT NULL,
  `dungeon_id` VARCHAR(64) NOT NULL,
  `data` LONGTEXT,
  `create_time` BIGINT,
  `update_time` BIGINT,
  PRIMARY KEY (`user_id`, `dungeon_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 11. 联盟表
CREATE TABLE IF NOT EXISTS `alliance` (
  `id` VARCHAR(64) NOT NULL,
  `name` VARCHAR(128),
  `leader_id` VARCHAR(64),
  `data` LONGTEXT,
  `create_time` BIGINT,
  `update_time` BIGINT,
  PRIMARY KEY (`id`),
  KEY `idx_leader_id` (`leader_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 12. 用户联盟映射表
CREATE TABLE IF NOT EXISTS `user_alliance` (
  `od_user_id` VARCHAR(64) NOT NULL,
  `alliance_id` VARCHAR(64) NOT NULL,
  PRIMARY KEY (`od_user_id`),
  KEY `idx_alliance_id` (`alliance_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 13. 联盟战表
CREATE TABLE IF NOT EXISTS `alliance_war` (
  `war_date` VARCHAR(20) NOT NULL,
  `data` LONGTEXT,
  PRIMARY KEY (`war_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 14. 国战表
CREATE TABLE IF NOT EXISTS `nation_war` (
  `id` VARCHAR(128) NOT NULL,
  `war_date` VARCHAR(20),
  `data` LONGTEXT,
  PRIMARY KEY (`id`),
  KEY `idx_date` (`war_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 15. 玩家国籍表
CREATE TABLE IF NOT EXISTS `player_nation` (
  `od_user_id` VARCHAR(64) NOT NULL,
  `nation` VARCHAR(20) NOT NULL,
  PRIMARY KEY (`od_user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 16. 玩家军功表
CREATE TABLE IF NOT EXISTS `player_merit` (
  `od_user_id` VARCHAR(64) NOT NULL,
  `merit` INT NOT NULL DEFAULT 0,
  PRIMARY KEY (`od_user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 17. 生产数据表
CREATE TABLE IF NOT EXISTS `production` (
  `od_user_id` VARCHAR(64) NOT NULL,
  `data` LONGTEXT,
  PRIMARY KEY (`od_user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 18. 武将兵法表
CREATE TABLE IF NOT EXISTS `general_tactics` (
  `general_id` VARCHAR(64) NOT NULL,
  `learned_data` LONGTEXT,
  `equipped_data` LONGTEXT,
  PRIMARY KEY (`general_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 19. 用户已学习兵法表
CREATE TABLE IF NOT EXISTS `user_learned_tactics` (
  `user_id` VARCHAR(64) NOT NULL,
  `tactics_ids` LONGTEXT,
  PRIMARY KEY (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 20. 充值订单表
CREATE TABLE IF NOT EXISTS `recharge_order` (
  `id` VARCHAR(128) NOT NULL,
  `od_user_id` VARCHAR(64) NOT NULL,
  `status` VARCHAR(20),
  `data` LONGTEXT,
  `create_time` BIGINT,
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`od_user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
