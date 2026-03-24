-- 给 general 表添加 slot_id 字段，关联 general_slot 表
-- 用于升级时精确回查基础属性，替代硬编码公式

ALTER TABLE `general` ADD COLUMN `slot_id` INT DEFAULT NULL COMMENT '关联general_slot表ID' AFTER `troop_type`;

-- 为已有武将补充 slot_id（根据名字+品质匹配）
-- 橙色步兵统帅
UPDATE `general` SET slot_id = 1 WHERE name = '赵云' AND quality_name = '橙色' AND slot_id IS NULL;
-- 紫色步兵统帅
UPDATE `general` SET slot_id = 7 WHERE name = '张飞' AND quality_name = '紫色' AND slot_id IS NULL;
-- 紫色骑兵统帅
UPDATE `general` SET slot_id = 9 WHERE name = '关羽' AND quality_name = '紫色' AND slot_id IS NULL;
-- 橙色骑兵猛将
UPDATE `general` SET slot_id = 3 WHERE name = '吕布' AND quality_name = '橙色' AND slot_id IS NULL;
-- 橙色弓兵智将
UPDATE `general` SET slot_id = 5 WHERE name = '诸葛亮' AND quality_name = '橙色' AND slot_id IS NULL;
-- 红色弓兵智将
UPDATE `general` SET slot_id = 17 WHERE name = '貂蝉' AND quality_name = '红色' AND slot_id IS NULL;

-- 通过招募系统创建的武将已自动带 slot_id，无需额外处理
