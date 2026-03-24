-- =============================================
-- 修复已有装备的 slot_type_id 和 base 属性
-- 原因: 旧映射 (2=头盔,3=铠甲,4=戒指,5=鞋子,6=项链)
--       与 APK (1=武器,2=戒指,3=项链,4=铠甲,5=头盔,6=靴子) 不一致
-- =============================================

-- 第一步: 先执行新的 equipment_pre 建表SQL (用户提供的APK数据)
-- 确保 equipment_pre 表已是APK版本后再执行以下脚本

-- =============================================
-- 步骤1: 修正 slot_type_id 为 APK 映射
-- =============================================

-- 武器: 旧id=1 -> 新id=1 (不变)
-- 不需要处理

-- 戒指: 旧id=4 -> 新id=2
UPDATE equipment SET slot_type_id = 2
WHERE slot_type_name IN ('戒指') AND slot_type_id = 4;

-- 项链: 旧id=6 -> 新id=3
UPDATE equipment SET slot_type_id = 3
WHERE slot_type_name IN ('项链') AND slot_type_id = 6;

-- 铠甲: 旧id=3 -> 新id=4
UPDATE equipment SET slot_type_id = 4
WHERE slot_type_name IN ('铠甲', '护甲') AND slot_type_id = 3;

-- 头盔: 旧id=2 -> 新id=5
UPDATE equipment SET slot_type_id = 5
WHERE slot_type_name IN ('头盔') AND slot_type_id = 2;

-- 靴子/鞋子: 旧id=5 -> 新id=6
UPDATE equipment SET slot_type_id = 6, slot_type_name = '靴子'
WHERE slot_type_name IN ('鞋子', '靴子') AND slot_type_id = 5;

-- 修正 slot_type_name (主武器 -> 武器)
UPDATE equipment SET slot_type_name = '武器'
WHERE slot_type_name = '主武器';

-- =============================================
-- 步骤2: 根据新 equipment_pre 修正 base 属性
-- 通过装备名称匹配模板，重新计算 base 属性
-- base 属性 = 模板属性 × (quality_value对应的attrRate / 10000)
-- 品质attrRate: 1=粗糙=8000, 2=普通=8500, 3=优良=9000, 4=无暇=9500, 5=完美=10000
-- =============================================

-- 创建临时表存储品质倍率
DROP TEMPORARY TABLE IF EXISTS tmp_quality_rate;
CREATE TEMPORARY TABLE tmp_quality_rate (
  quality_value INT,
  rate DOUBLE
);
INSERT INTO tmp_quality_rate VALUES (0, 0.8), (1, 0.8), (2, 0.85), (3, 0.9), (4, 0.95), (5, 1.0);

-- 通过名称匹配: equipment.name 包含 equipment_pre.name (去掉品质前缀)
-- 例如 "粗糙的虎啸之靴" 匹配 "虎啸之靴"
UPDATE equipment e
JOIN equipment_pre p ON e.name LIKE CONCAT('%', p.name)
JOIN tmp_quality_rate q ON q.quality_value = IFNULL(e.quality_value, 1)
SET
  e.base_attack   = FLOOR(p.gen_att * q.rate),
  e.base_defense  = FLOOR(p.gen_def * q.rate),
  e.base_valor    = FLOOR(p.gen_for * q.rate),
  e.base_command  = FLOOR(p.gen_leader * q.rate),
  e.base_hp       = FLOOR(p.army_life * q.rate),
  e.base_mobility = FLOOR(p.army_sp * q.rate),
  e.slot_type_id  = p.type,
  e.slot_type_name = p.position,
  e.icon = p.icon_url;

-- 对于名称不匹配的旧装备(如"虎啸战靴" vs "虎啸之靴")
-- 靴子/鞋子类: base_mobility 应该为 0, 应有 base_defense
-- 直接按 slot_type_name 修正: 靴子的 mobility 移到 defense
UPDATE equipment
SET base_defense = GREATEST(base_defense, base_mobility),
    base_mobility = 0
WHERE slot_type_name IN ('靴子', '鞋子') AND base_mobility > 0;

-- 项链类: 旧可能有 command, 新APK项链主加 attack
-- 保留现有值不强制修改

DROP TEMPORARY TABLE IF EXISTS tmp_quality_rate;

-- =============================================
-- 步骤3: 同步修正强化属性 (enhance_*)
-- 强化属性需要根据新的 ENHANCE_ADD_PRO 重算
-- 只对有强化等级的装备处理
-- =============================================

-- 强化属性 = base属性 * addPro[slotId] / 1000
-- 这里只清零 enhance_mobility (靴子不应有强化机动)
UPDATE equipment
SET enhance_mobility = 0
WHERE slot_type_name IN ('靴子', '鞋子') AND enhance_mobility > 0;

SELECT CONCAT('修复完成, 共影响装备: ', COUNT(*), ' 件') AS result
FROM equipment WHERE slot_type_name IS NOT NULL;
