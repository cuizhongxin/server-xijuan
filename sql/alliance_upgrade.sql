-- 联盟系统完善 - 数据库升级脚本

-- 1. 联盟主表增加 faction 字段
ALTER TABLE `alliance` ADD COLUMN `faction` VARCHAR(16) DEFAULT NULL COMMENT '联盟所属国家(WEI/SHU/WU)' AFTER `name`;

-- 2. 成员表增加盟战积分字段
ALTER TABLE `alliance_member` ADD COLUMN `war_score` INT DEFAULT 0 COMMENT '本期盟战积分' AFTER `contribution`;
ALTER TABLE `alliance_member` ADD COLUMN `last_war_score` INT DEFAULT 0 COMMENT '上期盟战积分' AFTER `war_score`;
ALTER TABLE `alliance_member` ADD COLUMN `last_login_time` BIGINT DEFAULT 0 COMMENT '最后登录时间' AFTER `last_war_score`;

-- 3. 联盟Boss表增加召唤时间窗口字段
ALTER TABLE `alliance_boss` ADD COLUMN `summon_min_time` INT DEFAULT 2000 COMMENT '每日可召唤最早时间(HHMM格式,如2000=20:00)' AFTER `feed_target`;
ALTER TABLE `alliance_boss` ADD COLUMN `summon_max_time` INT DEFAULT 2200 COMMENT '每日可召唤最晚时间(HHMM格式,如2200=22:00)' AFTER `summon_min_time`;
ALTER TABLE `alliance_boss` ADD COLUMN `alliance_id` VARCHAR(64) DEFAULT NULL COMMENT '所属联盟ID' AFTER `id`;

-- 4. 联盟Boss记录表增加联盟ID
ALTER TABLE `alliance_boss_record` ADD COLUMN `alliance_id` VARCHAR(64) DEFAULT NULL COMMENT '所属联盟ID' AFTER `id`;

-- 5. 联盟奖励领取记录表
CREATE TABLE IF NOT EXISTS `alliance_war_reward` (
  `id` BIGINT AUTO_INCREMENT COMMENT '自增主键',
  `war_date` VARCHAR(16) NOT NULL COMMENT '盟战日期',
  `user_id` VARCHAR(64) NOT NULL COMMENT '用户ID',
  `reward_type` VARCHAR(32) NOT NULL COMMENT '奖励类型: participate/personal_rank/alliance_rank/flag_bonus',
  `reward_items` TEXT COMMENT '奖励内容JSON',
  `claimed` TINYINT(1) DEFAULT 0 COMMENT '是否已领取',
  `create_time` BIGINT COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_awr_user_date` (`user_id`, `war_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='盟战奖励记录表';
