-- ============================================
-- 图标URL字段扩展
-- 后端存储"云托管对象存储的相对路径"，如: tactics/t_infantry_1.png、关羽.png
-- 前端通过 BUCKET_URL + 路径 拼接完整 HTTPS 下载地址:
--   https://7072-prod-2gzit899ac020df6-1400670220.tcb.qcloud.la/关羽.png
-- ============================================
-- 注: 兵种图标(idle/attack)为前端本地资源，
--     由 ImageLoader.getSoldierPath() 动态计算路径，无需后端存储。

-- 1. 装备模板表: 添加 icon_url 字段（存储云托管存储路径）
ALTER TABLE `equipment_pre`
  ADD COLUMN `icon_url` VARCHAR(256) DEFAULT NULL COMMENT '装备图标存储路径' AFTER `position`;

-- 按部位批量设置默认存储路径
UPDATE equipment_pre SET icon_url = CONCAT('equipment/', LOWER(position), '_lv', level, '.png');

-- 2. 扩展 avatar 字段长度
ALTER TABLE `general_template` MODIFY COLUMN `avatar` VARCHAR(256) COMMENT '头像存储路径';
ALTER TABLE `general` MODIFY COLUMN `avatar` VARCHAR(256) COMMENT '头像存储路径';
ALTER TABLE `formation_slot` MODIFY COLUMN `general_avatar` VARCHAR(256) COMMENT '武将头像存储路径';
