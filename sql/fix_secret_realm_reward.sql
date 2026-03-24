-- =============================================
-- 更新秘境奖励表: equip_pre_id 关联新 equipment_pre (APK数据)
-- 后端 createEquipmentFromRow 已改为通过 equip_pre_id 查 equipment_pre 获取属性
-- 此 SQL 同步修正 reward 表中的冗余属性和名称
-- =============================================

-- =============================================
-- 蓬莱秘宝 - 鹰扬套装 (旧id 25~30 -> APK id 23121~23126)
-- APK数据: equipment_pre_apk_sync.sql
-- suit_config id=12, 鹰扬套装, 3件=攻击+400, 6件=统帅+20
-- =============================================

-- 鹰扬刀 (武器, type=1, gen_att=310)
UPDATE secret_realm_reward SET
  equip_pre_id = 23121, item_id = 'equip_23121',
  name = '鹰扬刀', icon = 'images/equip/23121.jpg',
  position = '武器', set_name = '鹰扬',
  set_effect_3 = '攻击+400', set_effect_6 = '统帅+20',
  attack = 310, defense = 0, soldier_hp = 0, mobility = 0
WHERE realm_id = 'penglai' AND reward_type = 'equipment' AND sort_order = 1;

-- 鹰扬戒 (戒指, type=2, gen_att=250)
UPDATE secret_realm_reward SET
  equip_pre_id = 23122, item_id = 'equip_23122',
  name = '鹰扬戒', icon = 'images/equip/23122.jpg',
  position = '戒指', set_name = '鹰扬',
  set_effect_3 = '攻击+400', set_effect_6 = '统帅+20',
  attack = 250, defense = 0, soldier_hp = 0, mobility = 0
WHERE realm_id = 'penglai' AND reward_type = 'equipment' AND sort_order = 2;

-- 鹰扬项链 (项链, type=3, gen_att=210)
-- 注: 旧表 sort_order=3 是铠甲, sort_order=4 是项链, 需交换
UPDATE secret_realm_reward SET
  equip_pre_id = 23124, item_id = 'equip_23124',
  name = '鹰扬锴', icon = 'images/equip/23124.jpg',
  position = '铠甲', set_name = '鹰扬',
  set_effect_3 = '攻击+400', set_effect_6 = '统帅+20',
  attack = 0, defense = 300, soldier_hp = 0, mobility = 0
WHERE realm_id = 'penglai' AND reward_type = 'equipment' AND sort_order = 3;

UPDATE secret_realm_reward SET
  equip_pre_id = 23123, item_id = 'equip_23123',
  name = '鹰扬项链', icon = 'images/equip/23123.jpg',
  position = '项链', set_name = '鹰扬',
  set_effect_3 = '攻击+400', set_effect_6 = '统帅+20',
  attack = 210, defense = 0, soldier_hp = 0, mobility = 0
WHERE realm_id = 'penglai' AND reward_type = 'equipment' AND sort_order = 4;

-- 鹰扬盔 (头盔, type=5, gen_def=250)
UPDATE secret_realm_reward SET
  equip_pre_id = 23125, item_id = 'equip_23125',
  name = '鹰扬盔', icon = 'images/equip/23125.jpg',
  position = '头盔', set_name = '鹰扬',
  set_effect_3 = '攻击+400', set_effect_6 = '统帅+20',
  attack = 0, defense = 250, soldier_hp = 0, mobility = 0
WHERE realm_id = 'penglai' AND reward_type = 'equipment' AND sort_order = 5;

-- 鹰扬靴 (靴子, type=6, gen_def=220, mobility=0)
UPDATE secret_realm_reward SET
  equip_pre_id = 23126, item_id = 'equip_23126',
  name = '鹰扬靴', icon = 'images/equip/23126.jpg',
  position = '靴子', set_name = '鹰扬',
  set_effect_3 = '攻击+400', set_effect_6 = '统帅+20',
  attack = 0, defense = 220, soldier_hp = 0, mobility = 0
WHERE realm_id = 'penglai' AND reward_type = 'equipment' AND sort_order = 6;


-- =============================================
-- 昆仑秘宝 - 虎啸套装 (旧id 49~54 -> APK id 24131~24136)
-- suit_config id=13, 虎啸套装, 3件=防御+400, 6件=统帅+20,闪避+10
-- =============================================

-- 虎啸之剑 (武器, type=1, gen_att=495)
UPDATE secret_realm_reward SET
  equip_pre_id = 24131, item_id = 'equip_24131',
  name = '虎啸之剑', icon = 'images/equip/24131.jpg',
  position = '武器', set_name = '虎啸',
  set_effect_3 = '防御+400', set_effect_6 = '统帅+20, 闪避+10',
  attack = 495, defense = 0, soldier_hp = 0, mobility = 0
WHERE realm_id = 'kunlun' AND reward_type = 'equipment' AND sort_order = 1;

-- 虎啸之戒 (戒指, type=2, gen_att=410)
UPDATE secret_realm_reward SET
  equip_pre_id = 24132, item_id = 'equip_24132',
  name = '虎啸之戒', icon = 'images/equip/24132.jpg',
  position = '戒指', set_name = '虎啸',
  set_effect_3 = '防御+400', set_effect_6 = '统帅+20, 闪避+10',
  attack = 410, defense = 0, soldier_hp = 0, mobility = 0
WHERE realm_id = 'kunlun' AND reward_type = 'equipment' AND sort_order = 2;

-- 虎啸之锴 (铠甲, type=4, gen_def=415)
-- 注: 旧表 sort_order=3 是铠甲, sort_order=4 是项链
UPDATE secret_realm_reward SET
  equip_pre_id = 24134, item_id = 'equip_24134',
  name = '虎啸之锴', icon = 'images/equip/24134.jpg',
  position = '铠甲', set_name = '虎啸',
  set_effect_3 = '防御+400', set_effect_6 = '统帅+20, 闪避+10',
  attack = 0, defense = 415, soldier_hp = 0, mobility = 0
WHERE realm_id = 'kunlun' AND reward_type = 'equipment' AND sort_order = 3;

-- 虎啸项链 (项链, type=3, gen_att=360)
UPDATE secret_realm_reward SET
  equip_pre_id = 24133, item_id = 'equip_24133',
  name = '虎啸项链', icon = 'images/equip/24133.jpg',
  position = '项链', set_name = '虎啸',
  set_effect_3 = '防御+400', set_effect_6 = '统帅+20, 闪避+10',
  attack = 360, defense = 0, soldier_hp = 0, mobility = 0
WHERE realm_id = 'kunlun' AND reward_type = 'equipment' AND sort_order = 4;

-- 虎啸之盔 (头盔, type=5, gen_def=335)
UPDATE secret_realm_reward SET
  equip_pre_id = 24135, item_id = 'equip_24135',
  name = '虎啸之盔', icon = 'images/equip/24135.jpg',
  position = '头盔', set_name = '虎啸',
  set_effect_3 = '防御+400', set_effect_6 = '统帅+20, 闪避+10',
  attack = 0, defense = 335, soldier_hp = 0, mobility = 0
WHERE realm_id = 'kunlun' AND reward_type = 'equipment' AND sort_order = 5;

-- 虎啸之靴 (靴子, type=6, gen_def=300, mobility=0)
UPDATE secret_realm_reward SET
  equip_pre_id = 24136, item_id = 'equip_24136',
  name = '虎啸之靴', icon = 'images/equip/24136.jpg',
  position = '靴子', set_name = '虎啸',
  set_effect_3 = '防御+400', set_effect_6 = '统帅+20, 闪避+10',
  attack = 0, defense = 300, soldier_hp = 0, mobility = 0
WHERE realm_id = 'kunlun' AND reward_type = 'equipment' AND sort_order = 6;


SELECT '秘境奖励表更新完成' AS result;
