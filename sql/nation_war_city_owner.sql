-- 国战城市归属持久化表
-- 用于记录国战中城市所有权变更，服务重启后自动恢复
CREATE TABLE IF NOT EXISTS `nation_war_city_owner` (
  `city_id` VARCHAR(32) NOT NULL COMMENT '城市ID（如 LUOYANG、CHIBI）',
  `owner` VARCHAR(16) NOT NULL COMMENT '所属国家(WEI/SHU/WU/NEUTRAL)',
  `update_time` BIGINT DEFAULT 0 COMMENT '最后变更时间戳',
  PRIMARY KEY (`city_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='国战城市归属表';
