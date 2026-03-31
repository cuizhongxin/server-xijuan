-- =============================================
-- 按 general_data_full_rebuild.sql 对齐 APK 槽位数据（非重建版）
-- 目的：
-- 1) 不 DROP/CREATE 表，直接用 UPSERT 对齐 general_slot
-- 2) 对齐后按 APK 风格重算现有 general 的四维属性
-- =============================================

START TRANSACTION;

-- =============================================
-- 1) 对齐 general_slot（来自 general_data_full_rebuild.sql）
-- =============================================
INSERT INTO general_slot (
  id, quality_code, troop_type, slot_index, type,
  base_attack, base_defense, base_valor, base_command, base_dodge, base_mobility,
  base_power, sort_order, is_virtual, base_tactics_trigger_bonus,
  growth_attack, growth_defense, growth_valor, growth_command
) VALUES
-- orange
(1,'orange','骑',1,'统帅',178,132,25,26,6.00,31,1060,1,0,12,0,0,0,0),
(2,'orange','骑',2,'猛将',185,122,30,15,5.50,32,1055,2,0,15,0,0,0,0),
(3,'orange','骑',3,'猛将',180,120,28,18,5.50,34,1045,3,0,16,0,0,0,0),
(4,'orange','弓',4,'智将',195,108,20,28,7.50,19,1010,4,0,19,0,0,0,0),
(5,'orange','弓',5,'智将',190,112,22,29,8.50,24,1000,5,0,20,0,0,0,0),
(6,'orange','步',6,'统帅',152,158,22,24,6.50,21,1015,6,0,11,0,0,0,0),
(7,'orange','步',7,'猛将',148,168,27,15,12.00,20,1020,7,0,10,0,0,0,0),
(8,'orange','骑',8,'猛将',192,118,30,14,6.00,34,1100,8,0,15,0,0,0,0),
(9,'orange','弓',9,'智将',196,105,18,30,10.00,22,980,9,0,20,0,0,0,0),
-- purple
(10,'purple','骑',1,'猛将',145,115,23,15,5.00,28,618,10,0,8,0,0,0,0),
(11,'purple','骑',2,'统帅',135,118,20,18,6.00,26,608,11,0,8,0,0,0,0),
(12,'purple','骑',3,'猛将',142,115,22,16,5.00,28,613,12,0,8,0,0,0,0),
(13,'purple','弓',4,'猛将',155,96,22,15,5.00,18,618,13,0,8,0,0,0,0),
(14,'purple','弓',5,'智将',148,100,16,22,7.00,18,588,14,0,12,0,0,0,0),
(15,'purple','步',6,'猛将',120,140,22,14,7.00,18,618,15,0,8,0,0,0,0),
(16,'purple','步',7,'智将',112,145,16,23,7.50,18,593,16,0,9,0,0,0,0),
(17,'purple','步',8,'猛将',125,138,24,12,5.00,20,618,17,0,8,0,0,0,0),
(18,'purple','骑',9,'猛将',147,112,24,14,5.00,28,618,18,0,8,0,0,0,0),
(19,'purple','骑',10,'智将',136,118,18,20,6.00,26,593,19,0,10,0,0,0,0),
(20,'purple','弓',11,'猛将',152,98,20,16,5.00,18,588,20,0,8,0,0,0,0),
(21,'purple','步',12,'智将',115,142,20,18,6.00,18,593,21,0,8,0,0,0,0),
(22,'purple','步',13,'猛将',122,140,22,16,6.00,20,593,22,0,8,0,0,0,0),
(23,'purple','骑',14,'智将',138,116,20,18,6.00,28,593,23,0,10,0,0,0,0),
-- blue
(24,'blue','步',1,'猛将',72,88,12,10,4.00,18,0,24,0,0,0,0,0,0),
(25,'blue','骑',2,'统帅',82,72,11,11,4.00,24,0,25,0,0,0,0,0,0),
(26,'blue','弓',3,'智将',92,60,10,14,5.50,16,0,26,0,0,0,0,0,0),
(27,'blue','步',4,'智将',65,94,10,13,5.50,16,0,27,0,0,0,0,0,0),
(28,'blue','步',5,'猛将',74,86,14,8,3.00,17,0,28,0,0,0,0,0,0),
(29,'blue','骑',6,'猛将',85,68,12,10,4.00,24,0,29,0,0,0,0,0,0),
(30,'blue','弓',7,'猛将',90,58,9,13,5.00,16,0,30,0,0,0,0,0,0),
(31,'blue','弓',8,'智将',86,62,8,14,6.00,16,0,31,0,0,0,0,0,0),
(32,'blue','步',9,'智将',62,90,9,13,5.00,16,0,32,0,0,0,0,0,0),
(33,'blue','步',10,'猛将',70,88,14,8,3.00,17,0,33,0,0,0,0,0,0),
(34,'blue','弓',11,'智将',82,65,8,14,7.00,16,0,34,0,0,0,0,0,0),
(35,'blue','弓',12,'猛将',94,56,10,11,4.00,16,0,35,0,0,0,0,0,0),
-- green
(36,'green','骑',1,'猛将',52,46,8,5,3.00,20,0,36,0,0,0,0,0,0),
(37,'green','步',2,'猛将',42,60,8,5,3.00,14,0,37,0,0,0,0,0,0),
(38,'green','骑',3,'智将',48,48,6,8,3.00,18,0,38,0,0,0,0,0,0),
(39,'green','骑',4,'统帅',48,50,7,8,4.00,20,0,39,0,0,0,0,0,0),
(40,'green','弓',5,'猛将',58,40,6,9,5.00,12,0,40,0,0,0,0,0,0),
(41,'green','弓',6,'猛将',58,40,6,9,5.00,12,0,41,0,0,0,0,0,0),
(42,'green','弓',7,'智将',54,42,4,10,6.00,12,0,42,0,0,0,0,0,0),
(43,'green','步',8,'统帅',40,58,7,8,4.00,16,0,43,0,0,0,0,0,0),
(44,'green','步',9,'智将',38,62,5,9,5.00,13,0,44,0,0,0,0,0,0),
(45,'green','步',10,'智将',40,60,6,8,4.00,14,0,45,0,0,0,0,0,0),
(46,'green','步',11,'智将',38,58,5,9,6.00,14,0,46,0,0,0,0,0,0),
(47,'green','弓',12,'智将',54,42,4,10,5.00,12,0,47,0,0,0,0,0,0),
-- story
(48,'blue','步',13,'智将',60,86,8,12,4.00,16,0,48,0,0,0,0,0,0),
-- purple virtual
(50,'purple','步',15,'猛将',122,138,22,14,5.00,20,568,50,1,8,0,0,0,0),
(51,'purple','步',16,'智将',114,140,16,20,7.00,18,548,51,1,10,0,0,0,0),
(52,'purple','步',17,'统帅',118,139,20,18,6.50,20,558,52,1,9,0,0,0,0),
(53,'purple','骑',15,'猛将',144,114,24,14,5.00,28,608,53,1,8,0,0,0,0),
(54,'purple','骑',16,'智将',136,117,18,20,6.00,24,593,54,1,10,0,0,0,0),
(55,'purple','骑',17,'统帅',138,116,20,18,6.00,26,608,55,1,8,0,0,0,0),
(56,'purple','弓',15,'猛将',152,98,20,14,5.00,18,588,56,1,8,0,0,0,0),
(57,'purple','弓',16,'智将',146,102,14,22,8.00,18,568,57,1,10,0,0,0,0),
-- blue virtual
(60,'blue','步',13,'智将',66,90,9,12,5.00,17,0,60,1,0,0,0,0,0),
(61,'blue','骑',13,'猛将',83,70,12,10,4.00,24,0,61,1,0,0,0,0,0),
(62,'blue','弓',13,'智将',88,62,8,14,6.00,16,0,62,1,0,0,0,0,0),
(63,'blue','弓',14,'猛将',92,58,10,12,5.00,16,0,63,1,0,0,0,0,0),
-- green virtual
(70,'green','步',13,'智将',39,58,6,8,4.00,14,0,70,1,0,0,0,0,0),
(71,'green','骑',13,'猛将',50,46,8,5,3.00,20,0,71,1,0,0,0,0,0),
(72,'green','弓',13,'智将',55,42,4,10,5.00,12,0,72,1,0,0,0,0,0),
(73,'green','弓',14,'猛将',57,40,6,8,5.00,12,0,73,1,0,0,0,0,0),
-- white virtual
(80,'white','步',1,'智将',30,42,4,5,3.00,11,0,80,1,0,0,0,0,0),
(81,'white','骑',1,'猛将',36,36,5,4,2.00,15,0,81,1,0,0,0,0,0),
(82,'white','弓',1,'智将',40,30,3,6,4.00,10,0,82,1,0,0,0,0,0),
(83,'white','弓',2,'猛将',42,28,5,4,3.00,10,0,83,1,0,0,0,0,0)
ON DUPLICATE KEY UPDATE
  quality_code = VALUES(quality_code),
  troop_type = VALUES(troop_type),
  slot_index = VALUES(slot_index),
  type = VALUES(type),
  base_attack = VALUES(base_attack),
  base_defense = VALUES(base_defense),
  base_valor = VALUES(base_valor),
  base_command = VALUES(base_command),
  base_dodge = VALUES(base_dodge),
  base_mobility = VALUES(base_mobility),
  base_power = VALUES(base_power),
  sort_order = VALUES(sort_order),
  is_virtual = VALUES(is_virtual),
  base_tactics_trigger_bonus = VALUES(base_tactics_trigger_bonus),
  growth_attack = VALUES(growth_attack),
  growth_defense = VALUES(growth_defense),
  growth_valor = VALUES(growth_valor),
  growth_command = VALUES(growth_command);

-- =============================================
-- 1.1) 填充 general_slot.growth_*（APK风格成长率）
--      说明：general_data_full_rebuild.sql 中 growth_* 为 0，
--            这里按品质成长率回填为“每级成长值”。
-- =============================================
UPDATE general_slot
SET
  growth_attack = GREATEST(1, ROUND(base_attack * (
    CASE quality_code
      WHEN 'orange' THEN 0.06
      WHEN 'purple' THEN 0.05
      WHEN 'red' THEN 0.045
      WHEN 'blue' THEN 0.04
      WHEN 'green' THEN 0.035
      ELSE 0.03
    END
  ))),
  growth_defense = GREATEST(1, ROUND(base_defense * (
    CASE quality_code
      WHEN 'orange' THEN 0.06
      WHEN 'purple' THEN 0.05
      WHEN 'red' THEN 0.045
      WHEN 'blue' THEN 0.04
      WHEN 'green' THEN 0.035
      ELSE 0.03
    END
  ))),
  growth_valor = GREATEST(1, ROUND(base_valor * (
    CASE quality_code
      WHEN 'orange' THEN 0.06
      WHEN 'purple' THEN 0.05
      WHEN 'red' THEN 0.045
      WHEN 'blue' THEN 0.04
      WHEN 'green' THEN 0.035
      ELSE 0.03
    END
  ))),
  growth_command = GREATEST(1, ROUND(base_command * (
    CASE quality_code
      WHEN 'orange' THEN 0.06
      WHEN 'purple' THEN 0.05
      WHEN 'red' THEN 0.045
      WHEN 'blue' THEN 0.04
      WHEN 'green' THEN 0.035
      ELSE 0.03
    END
  )));

-- =============================================
-- 2) 按 APK 风格重算现有 general 四维
--    规则与 GeneralService 对齐：
--    attr = base + growth * (lv-1)
--    若 growth=0，回退 base * qualityRate
--    狂化武将（name like '%(狂)'）攻防成长额外 +1/级
-- =============================================
UPDATE general g
JOIN general_slot s ON g.slot_id = s.id
SET
  g.attr_attack = FLOOR(
    s.base_attack
    + (
      (CASE
         WHEN IFNULL(s.growth_attack, 0) > 0 THEN s.growth_attack
         WHEN s.quality_code = 'orange' THEN s.base_attack * 0.06
         WHEN s.quality_code = 'purple' THEN s.base_attack * 0.05
         WHEN s.quality_code = 'red' THEN s.base_attack * 0.045
         WHEN s.quality_code = 'blue' THEN s.base_attack * 0.04
         WHEN s.quality_code = 'green' THEN s.base_attack * 0.035
         ELSE s.base_attack * 0.03
       END)
      + (CASE WHEN g.name LIKE '%(狂)' THEN 1 ELSE 0 END)
    ) * GREATEST(g.level - 1, 0)
  ),
  g.attr_defense = FLOOR(
    s.base_defense
    + (
      (CASE
         WHEN IFNULL(s.growth_defense, 0) > 0 THEN s.growth_defense
         WHEN s.quality_code = 'orange' THEN s.base_defense * 0.06
         WHEN s.quality_code = 'purple' THEN s.base_defense * 0.05
         WHEN s.quality_code = 'red' THEN s.base_defense * 0.045
         WHEN s.quality_code = 'blue' THEN s.base_defense * 0.04
         WHEN s.quality_code = 'green' THEN s.base_defense * 0.035
         ELSE s.base_defense * 0.03
       END)
      + (CASE WHEN g.name LIKE '%(狂)' THEN 1 ELSE 0 END)
    ) * GREATEST(g.level - 1, 0)
  ),
  g.attr_valor = FLOOR(
    s.base_valor
    + (CASE
         WHEN IFNULL(s.growth_valor, 0) > 0 THEN s.growth_valor
         WHEN s.quality_code = 'orange' THEN s.base_valor * 0.06
         WHEN s.quality_code = 'purple' THEN s.base_valor * 0.05
         WHEN s.quality_code = 'red' THEN s.base_valor * 0.045
         WHEN s.quality_code = 'blue' THEN s.base_valor * 0.04
         WHEN s.quality_code = 'green' THEN s.base_valor * 0.035
         ELSE s.base_valor * 0.03
       END) * GREATEST(g.level - 1, 0)
  ),
  g.attr_command = FLOOR(
    s.base_command
    + (CASE
         WHEN IFNULL(s.growth_command, 0) > 0 THEN s.growth_command
         WHEN s.quality_code = 'orange' THEN s.base_command * 0.06
         WHEN s.quality_code = 'purple' THEN s.base_command * 0.05
         WHEN s.quality_code = 'red' THEN s.base_command * 0.045
         WHEN s.quality_code = 'blue' THEN s.base_command * 0.04
         WHEN s.quality_code = 'green' THEN s.base_command * 0.035
         ELSE s.base_command * 0.03
       END) * GREATEST(g.level - 1, 0)
  ),
  g.attr_dodge = LEAST(
    50,
    FLOOR(
      s.base_dodge + s.base_dodge * (CASE
        WHEN s.quality_code = 'orange' THEN 0.06
        WHEN s.quality_code = 'purple' THEN 0.05
        WHEN s.quality_code = 'red' THEN 0.045
        WHEN s.quality_code = 'blue' THEN 0.04
        WHEN s.quality_code = 'green' THEN 0.035
        ELSE 0.03
      END) * GREATEST(g.level - 1, 0)
    )
  ),
  g.attr_mobility = FLOOR(
    s.base_mobility + s.base_mobility * (CASE
      WHEN s.quality_code = 'orange' THEN 0.06
      WHEN s.quality_code = 'purple' THEN 0.05
      WHEN s.quality_code = 'red' THEN 0.045
      WHEN s.quality_code = 'blue' THEN 0.04
      WHEN s.quality_code = 'green' THEN 0.035
      ELSE 0.03
    END) * GREATEST(g.level - 1, 0)
  ),
  g.update_time = UNIX_TIMESTAMP() * 1000
WHERE g.slot_id IS NOT NULL;

COMMIT;

