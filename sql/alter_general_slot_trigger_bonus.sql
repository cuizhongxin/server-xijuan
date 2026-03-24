-- general_slot 表新增兵法发动概率加成字段
-- 品质顺序: 白色 < 绿色 < 蓝色 < 红色 < 紫色 < 橙色

ALTER TABLE `general_slot`
  ADD COLUMN `base_tactics_trigger_bonus` DOUBLE DEFAULT 0 COMMENT '兵法发动概率加成(%)，叠加到兵法自身发动率上';

-- 按品质批量更新加成值
UPDATE `general_slot` SET base_tactics_trigger_bonus = 0  WHERE quality_code = 'white';
UPDATE `general_slot` SET base_tactics_trigger_bonus = 10  WHERE quality_code = 'green';
UPDATE `general_slot` SET base_tactics_trigger_bonus = 15  WHERE quality_code = 'blue';
UPDATE `general_slot` SET base_tactics_trigger_bonus = 20  WHERE quality_code = 'red';
UPDATE `general_slot` SET base_tactics_trigger_bonus = 25 WHERE quality_code = 'purple';
UPDATE `general_slot` SET base_tactics_trigger_bonus = 40 WHERE quality_code = 'orange';
