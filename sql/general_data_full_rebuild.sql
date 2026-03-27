-- =============================================
-- 武将配置数据完整重建（共享槽位版 — 支持转国）
--
-- 设计核心:
--   同槽位的魏/蜀/吴武将共享相同base stats
--   转国时只换 general_template, 保留 slot_id
--   槽位映射规则: APK picID 末位相同 = 同槽位
--     xx51=slot1, xx52=slot2 ... xx57=slot7
--
-- 兵种定位:
--   弓兵将: 攻击最高,防御最低 (最强杀伤力,极致输出)
--   骑兵将: 攻击中高,防御中等,机动最高 (擅长捕捉战机)
--   步兵将: 攻击最低,防御最高 (皮糙血厚,擅长防御)
--
-- 数据来源:
--   名将特性: FamousGenAttr.json
--   兵种数值: ArmyService.json (弓att最高2600>骑1500>步800)
--   装备属性: equipInfo_cfg.json
--   套装效果: suit_cfg.json
--   编制加成: formation_cfg.json
--   进阶: GenAdvShow_cfg.json
-- =============================================

-- =============================================
-- 1. general_slot — 全量重建（共享槽位）
-- =============================================
DROP TABLE IF EXISTS `general_slot`;
CREATE TABLE IF NOT EXISTS `general_slot` (
  `id` INT AUTO_INCREMENT PRIMARY KEY,
  `quality_code` VARCHAR(10) NOT NULL COMMENT '品质',
  `troop_type` VARCHAR(4) NOT NULL COMMENT '兵种: 步/骑/弓',
  `slot_index` INT NOT NULL DEFAULT 0,
  `type` VARCHAR(10) NOT NULL DEFAULT '智将',
  `base_attack` INT NOT NULL DEFAULT 0,
  `base_defense` INT NOT NULL DEFAULT 0,
  `base_valor` INT NOT NULL DEFAULT 0,
  `base_command` INT NOT NULL DEFAULT 0,
  `base_dodge` VARCHAR(10) NOT NULL DEFAULT '0.00',
  `base_mobility` INT NOT NULL DEFAULT 0,
  `base_power` INT NOT NULL DEFAULT 0,
  `sort_order` INT NOT NULL DEFAULT 0,
  `is_virtual` TINYINT NOT NULL DEFAULT 0,
  `base_tactics_trigger_bonus` INT NOT NULL DEFAULT 0,
  `growth_attack` INT NOT NULL DEFAULT 0,
  `growth_defense` INT NOT NULL DEFAULT 0,
  `growth_valor` INT NOT NULL DEFAULT 0,
  `growth_command` INT NOT NULL DEFAULT 0,
  INDEX idx_quality (`quality_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='武将槽位表(共享)';

-- =====================================================
-- 橙色共享 slot 1-7 (魏/蜀/吴 各7人共用)
-- + 群独占 slot 8-9
-- APK picID: xx51→slot1 ... xx57→slot7
-- 弓att(190-196) > 骑att(178-185) > 步att(148-152)
-- 步def(158-168) > 骑def(118-132) > 弓def(105-112)
-- =====================================================
INSERT INTO `general_slot` (`id`,`quality_code`,`troop_type`,`slot_index`,`type`,
  `base_attack`,`base_defense`,`base_valor`,`base_command`,`base_dodge`,`base_mobility`,
  `base_power`,`sort_order`,`is_virtual`,`base_tactics_trigger_bonus`,
  `growth_attack`,`growth_defense`,`growth_valor`,`growth_command`) VALUES
-- slot1(xx51): 张辽/关羽/凌统 → damage400+count100 → 骑统帅
( 1,'orange','骑',1,'统帅', 178,132,25,26,'6.00', 31, 1060, 1,0,12, 0,0,0,0),
-- slot2(xx52): 许褚/赵云/太史慈 → damage410+骑tactics → 骑猛将
( 2,'orange','骑',2,'猛将', 185,122,30,15,'5.50', 32, 1055, 2,0,15, 0,0,0,0),
-- slot3(xx53): 乐进/马超/吕蒙 → mob4+骑/兵法tactics → 骑猛将
( 3,'orange','骑',3,'猛将', 180,120,28,18,'5.50', 34, 1045, 3,0,16, 0,0,0,0),
-- slot4(xx54): 徐晃/黄忠/陆逊 → 弓tactics+弓damage390 → 弓智将(最高攻击)
( 4,'orange','弓',4,'智将', 195,108,20,28,'7.50', 19, 1010, 4,0,19, 0,0,0,0),
-- slot5(xx55): 张郃/姜维/甘宁 → mob4+tactics_prob → 弓智将
( 5,'orange','弓',5,'智将', 190,112,22,29,'8.50', 24, 1000, 5,0,20, 0,0,0,0),
-- slot6(xx56): 于禁/张飞/徐盛 → life12%+count100 → 步统帅(最高防御)
( 6,'orange','步',6,'统帅', 152,158,22,24,'6.50', 21, 1015, 6,0,11, 0,0,0,0),
-- slot7(xx57): 典韦/魏延/周泰 → defense400+dodge12 → 步猛将(极限防御)
( 7,'orange','步',7,'猛将', 148,168,27,15,'12.00',20, 1020, 7,0,10, 0,0,0,0),
-- slot8: 吕布(群独占) → damage500+骑tactics → 骑猛将(全游戏最强)
( 8,'orange','骑',8,'猛将', 192,118,30,14,'6.00', 34, 1100, 8,0,15, 0,0,0,0),
-- slot9: 貂蝉(群独占) → immune+mob3+tactics → 弓智将(极限输出+控制)
( 9,'orange','弓',9,'智将', 196,105,18,30,'10.00',22, 980,  9,0,20, 0,0,0,0);

-- =====================================================
-- 紫色共享 slot 10-16 (魏/蜀/吴 各7人共用)
-- + 群独占 slot 17-23
-- APK picID: xx41→slot10 ... xx47→slot16
-- 弓att(148-155) > 骑att(135-147) > 步att(112-125)
-- 步def(138-145) > 骑def(112-118) > 弓def(95-102)
-- =====================================================
INSERT INTO `general_slot` (`id`,`quality_code`,`troop_type`,`slot_index`,`type`,
  `base_attack`,`base_defense`,`base_valor`,`base_command`,`base_dodge`,`base_mobility`,
  `base_power`,`sort_order`,`is_virtual`,`base_tactics_trigger_bonus`,
  `growth_attack`,`growth_defense`,`growth_valor`,`growth_command`) VALUES
-- slot10(xx41): 曹仁/关平/丁奉 → 骑damage320 → 骑猛将
(10,'purple','骑',1,'猛将', 145,115,23,15,'5.00', 28, 618, 10,0, 8, 0,0,0,0),
-- slot11(xx42): 曹洪/廖化/韩当 → mob3 → 骑统帅
(11,'purple','骑',2,'统帅', 135,118,20,18,'6.00', 26, 608, 11,0, 8, 0,0,0,0),
-- slot12(xx43): 庞德/张翼/朱桓 → 骑attack300 → 骑猛将
(12,'purple','骑',3,'猛将', 142,115,22,16,'5.00', 28, 613, 12,0, 8, 0,0,0,0),
-- slot13(xx44): 曹真/关索/凌操 → army_attack310 → 弓猛将(弓最高攻击)
(13,'purple','弓',4,'猛将', 155,96,22,15,'5.00', 18, 618, 13,0, 8, 0,0,0,0),
-- slot14(xx45): 夏侯渊/关兴/程普 → 弓tactics → 弓智将
(14,'purple','弓',5,'智将', 148,100,16,22,'7.00', 18, 588, 14,0,12, 0,0,0,0),
-- slot15(xx46): 夏侯惇/张苞/黄盖 → life12% → 步猛将(步最高防御)
(15,'purple','步',6,'猛将', 120,140,22,14,'7.00', 18, 618, 15,0, 8, 0,0,0,0),
-- slot16(xx47): 李典/向宠/陆抗 → defense350 → 步智将
(16,'purple','步',7,'智将', 112,145,16,23,'7.50', 18, 593, 16,0, 9, 0,0,0,0),
-- 群独占 slot17-23
(17,'purple','步', 8,'猛将', 125,138,24,12,'5.00', 20, 618, 17,0, 8, 0,0,0,0),  -- 华雄: damage330
(18,'purple','骑', 9,'猛将', 147,112,24,14,'5.00', 28, 618, 18,0, 8, 0,0,0,0),  -- 颜良: 骑attack310
(19,'purple','骑',10,'智将', 136,118,18,20,'6.00', 26, 593, 19,0,10, 0,0,0,0),  -- 马腾: 骑tactics
(20,'purple','弓',11,'猛将', 152,98,20,16,'5.00', 18, 588, 20,0, 8, 0,0,0,0),  -- 鞠义: 弓damage320
(21,'purple','步',12,'智将', 115,142,20,18,'6.00', 18, 593, 21,0, 8, 0,0,0,0),  -- 高顺: 步defense350
(22,'purple','步',13,'猛将', 122,140,22,16,'6.00', 20, 593, 22,0, 8, 0,0,0,0),  -- 文丑: resist330
(23,'purple','骑',14,'智将', 138,116,20,18,'6.00', 28, 593, 23,0,10, 0,0,0,0);  -- 公孙瓒: 骑tactics

-- =====================================================
-- 蓝色共享 slot 24-27 (魏/蜀/吴 各4人共用)
-- + 群独占 slot 28-35
-- APK picID: xx31→slot24 ... xx34→slot27
-- 弓att(82-94) > 骑att(80-85) > 步att(62-74)
-- 步def(86-94) > 骑def(68-75) > 弓def(56-65)
-- =====================================================
INSERT INTO `general_slot` (`id`,`quality_code`,`troop_type`,`slot_index`,`type`,
  `base_attack`,`base_defense`,`base_valor`,`base_command`,`base_dodge`,`base_mobility`,
  `base_power`,`sort_order`,`is_virtual`,`base_tactics_trigger_bonus`,
  `growth_attack`,`growth_defense`,`growth_valor`,`growth_command`) VALUES
-- slot24(xx31): 文聘/马岱/祖茂 → damage190 → 步猛将
(24,'blue','步',1,'猛将', 72,88,12,10,'4.00', 18, 0, 24,0,0, 0,0,0,0),
-- slot25(xx32): 郝昭/高翔/蒋钦 → mob2 → 骑统帅
(25,'blue','骑',2,'统帅', 82,72,11,11,'4.00', 24, 0, 25,0,0, 0,0,0,0),
-- slot26(xx33): 邓艾/刘封/全祎 → 弓damage200 → 弓智将(弓最高攻击)
(26,'blue','弓',3,'智将', 92,60,10,14,'5.50', 16, 0, 26,0,0, 0,0,0,0),
-- slot27(xx34): 满宠/周仓/朱然 → 步defense200 → 步智将(步最高防御)
(27,'blue','步',4,'智将', 65,94,10,13,'5.50', 16, 0, 27,0,0, 0,0,0,0),
-- 群独占 slot28-35
(28,'blue','步', 5,'猛将', 74,86,14, 8,'3.00', 17, 0, 28,0,0, 0,0,0,0),  -- 潘凤: damage150
(29,'blue','骑', 6,'猛将', 85,68,12,10,'4.00', 24, 0, 29,0,0, 0,0,0,0),  -- 韩遂: 骑damage160
(30,'blue','弓', 7,'猛将', 90,58, 9,13,'5.00', 16, 0, 30,0,0, 0,0,0,0),  -- 韩性: 弓damage150
(31,'blue','弓', 8,'智将', 86,62, 8,14,'6.00', 16, 0, 31,0,0, 0,0,0,0),  -- 曹猛: 弓tactics
(32,'blue','步', 9,'智将', 62,90, 9,13,'5.00', 16, 0, 32,0,0, 0,0,0,0),  -- 徐荣: life7%
(33,'blue','步',10,'猛将', 70,88,14, 8,'3.00', 17, 0, 33,0,0, 0,0,0,0),  -- 孟获: resist180
(34,'blue','弓',11,'智将', 82,65, 8,14,'7.00', 16, 0, 34,0,0, 0,0,0,0),  -- 张角: dodge7
(35,'blue','弓',12,'猛将', 94,56,10,11,'4.00', 16, 0, 35,0,0, 0,0,0,0);  -- 沙摩柯: 弓damage160

-- =====================================================
-- 绿色 slot 36-47 (全部群, 各自独占)
-- 弓att(54-60) > 骑att(48-52) > 步att(38-44)
-- 步def(56-62) > 骑def(46-50) > 弓def(38-44)
-- =====================================================
INSERT INTO `general_slot` (`id`,`quality_code`,`troop_type`,`slot_index`,`type`,
  `base_attack`,`base_defense`,`base_valor`,`base_command`,`base_dodge`,`base_mobility`,
  `base_power`,`sort_order`,`is_virtual`,`base_tactics_trigger_bonus`,
  `growth_attack`,`growth_defense`,`growth_valor`,`growth_command`) VALUES
(36,'green','骑', 1,'猛将', 52,46,8,5,'3.00', 20, 0, 36,0,0, 0,0,0,0),  -- 张燕: 骑damage80
(37,'green','步', 2,'猛将', 42,60,8,5,'3.00', 14, 0, 37,0,0, 0,0,0,0),  -- 宋宪: att160
(38,'green','骑', 3,'智将', 48,48,6,8,'3.00', 18, 0, 38,0,0, 0,0,0,0),  -- 李催: 骑tactics
(39,'green','骑', 4,'统帅', 48,50,7,8,'4.00', 20, 0, 39,0,0, 0,0,0,0),  -- 郭汜: mob1
(40,'green','弓', 5,'猛将', 58,40,6,9,'5.00', 12, 0, 40,0,0, 0,0,0,0),  -- 蔡瑁: 弓damage85
(41,'green','弓', 6,'猛将', 58,40,6,9,'5.00', 12, 0, 41,0,0, 0,0,0,0),  -- 邓贤: 弓att81
(42,'green','弓', 7,'智将', 54,42,4,10,'6.00',12, 0, 42,0,0, 0,0,0,0),  -- 杨怀: 弓tactics
(43,'green','步', 8,'统帅', 40,58,7, 8,'4.00',16, 0, 43,0,0, 0,0,0,0),  -- 泠苞: mob1
(44,'green','步', 9,'智将', 38,62,5, 9,'5.00',13, 0, 44,0,0, 0,0,0,0),  -- 张英: def85
(45,'green','步',10,'智将', 40,60,6, 8,'4.00',14, 0, 45,0,0, 0,0,0,0),  -- 雷铜: life4%
(46,'green','步',11,'智将', 38,58,5, 9,'6.00',14, 0, 46,0,0, 0,0,0,0),  -- 高沛: dodge3
(47,'green','弓',12,'智将', 54,42,4,10,'5.00',12, 0, 47,0,0, 0,0,0,0),  -- 樊稠: count20
-- slot48: 梁婉(引导赠送) → 蓝步智将
(48,'blue','步',13,'智将', 60,86,8,12,'4.00',16, 0, 48,0,0, 0,0,0,0);

-- =====================================================
-- 紫色虚构 slot 50-57 (8种类型, 共50虚构将)
-- 弓att > 骑att > 步att; 步def > 骑def > 弓def
-- =====================================================
INSERT INTO `general_slot` (`id`,`quality_code`,`troop_type`,`slot_index`,`type`,
  `base_attack`,`base_defense`,`base_valor`,`base_command`,`base_dodge`,`base_mobility`,
  `base_power`,`sort_order`,`is_virtual`,`base_tactics_trigger_bonus`,
  `growth_attack`,`growth_defense`,`growth_valor`,`growth_command`) VALUES
(50,'purple','步',15,'猛将', 122,138,22,14,'5.00', 20, 568, 50,1, 8, 0,0,0,0),
(51,'purple','步',16,'智将', 114,140,16,20,'7.00', 18, 548, 51,1,10, 0,0,0,0),
(52,'purple','步',17,'统帅', 118,139,20,18,'6.50', 20, 558, 52,1, 9, 0,0,0,0),
(53,'purple','骑',15,'猛将', 144,114,24,14,'5.00', 28, 608, 53,1, 8, 0,0,0,0),
(54,'purple','骑',16,'智将', 136,117,18,20,'6.00', 24, 593, 54,1,10, 0,0,0,0),
(55,'purple','骑',17,'统帅', 138,116,20,18,'6.00', 26, 608, 55,1, 8, 0,0,0,0),
(56,'purple','弓',15,'猛将', 152,98, 20,14,'5.00', 18, 588, 56,1, 8, 0,0,0,0),
(57,'purple','弓',16,'智将', 146,102,14,22,'8.00', 18, 568, 57,1,10, 0,0,0,0);

-- =====================================================
-- 蓝色虚构 slot 60-63
-- =====================================================
INSERT INTO `general_slot` (`id`,`quality_code`,`troop_type`,`slot_index`,`type`,
  `base_attack`,`base_defense`,`base_valor`,`base_command`,`base_dodge`,`base_mobility`,
  `base_power`,`sort_order`,`is_virtual`,`base_tactics_trigger_bonus`,
  `growth_attack`,`growth_defense`,`growth_valor`,`growth_command`) VALUES
(60,'blue','步',13,'智将', 66,90, 9,12,'5.00', 17, 0, 60,1,0, 0,0,0,0),
(61,'blue','骑',13,'猛将', 83,70,12,10,'4.00', 24, 0, 61,1,0, 0,0,0,0),
(62,'blue','弓',13,'智将', 88,62, 8,14,'6.00', 16, 0, 62,1,0, 0,0,0,0),
(63,'blue','弓',14,'猛将', 92,58,10,12,'5.00', 16, 0, 63,1,0, 0,0,0,0);

-- =====================================================
-- 绿色虚构 slot 70-73
-- =====================================================
INSERT INTO `general_slot` (`id`,`quality_code`,`troop_type`,`slot_index`,`type`,
  `base_attack`,`base_defense`,`base_valor`,`base_command`,`base_dodge`,`base_mobility`,
  `base_power`,`sort_order`,`is_virtual`,`base_tactics_trigger_bonus`,
  `growth_attack`,`growth_defense`,`growth_valor`,`growth_command`) VALUES
(70,'green','步',13,'智将', 39,58,6,8,'4.00', 14, 0, 70,1,0, 0,0,0,0),
(71,'green','骑',13,'猛将', 50,46,8,5,'3.00', 20, 0, 71,1,0, 0,0,0,0),
(72,'green','弓',13,'智将', 55,42,4,10,'5.00',12, 0, 72,1,0, 0,0,0,0),
(73,'green','弓',14,'猛将', 57,40,6, 8,'5.00',12, 0, 73,1,0, 0,0,0,0);

-- =====================================================
-- 白色虚构 slot 80-83
-- =====================================================
INSERT INTO `general_slot` (`id`,`quality_code`,`troop_type`,`slot_index`,`type`,
  `base_attack`,`base_defense`,`base_valor`,`base_command`,`base_dodge`,`base_mobility`,
  `base_power`,`sort_order`,`is_virtual`,`base_tactics_trigger_bonus`,
  `growth_attack`,`growth_defense`,`growth_valor`,`growth_command`) VALUES
(80,'white','步',1,'智将', 30,42,4,5,'3.00', 11, 0, 80,1,0, 0,0,0,0),
(81,'white','骑',1,'猛将', 36,36,5,4,'2.00', 15, 0, 81,1,0, 0,0,0,0),
(82,'white','弓',1,'智将', 40,30,3,6,'4.00', 10, 0, 82,1,0, 0,0,0,0),
(83,'white','弓',2,'猛将', 42,28,5,4,'3.00', 10, 0, 83,1,0, 0,0,0,0);


-- =============================================
-- 2. general_template — 全量重建
-- =============================================
-- 同slot的魏/蜀/吴武将指向相同slot_id
-- 转国时: 换template保留slot

DROP TABLE IF EXISTS `general_template`;
CREATE TABLE IF NOT EXISTS `general_template` (
  `id` INT AUTO_INCREMENT PRIMARY KEY,
  `name` VARCHAR(20) NOT NULL,
  `faction` VARCHAR(4) NOT NULL COMMENT '阵营: 魏/蜀/吴/群',
  `slot_id` INT NOT NULL,
  `sort_order` INT NOT NULL DEFAULT 0,
  `avatar` VARCHAR(64) DEFAULT NULL,
  INDEX idx_slot (`slot_id`),
  INDEX idx_faction (`faction`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='武将模版表';

-- -----------------------------------------------
-- 橙色 (23人)
-- slot1: 张辽/关羽/凌统 (damage+count, 骑统帅)
-- slot2: 许褚/赵云/太史慈 (damage+骑tactics, 骑猛将)
-- slot3: 乐进/马超/吕蒙 (mob+tactics, 骑猛将)
-- slot4: 徐晃/黄忠/陆逊 (弓tactics+弓damage, 弓智将)
-- slot5: 张郃/姜维/甘宁 (mob+tactics_prob, 弓智将)
-- slot6: 于禁/张飞/徐盛 (life+count, 步统帅)
-- slot7: 典韦/魏延/周泰 (defense+dodge, 步猛将)
-- slot8: 吕布 / slot9: 貂蝉
-- -----------------------------------------------
INSERT INTO `general_template` (`id`,`name`,`faction`,`slot_id`,`sort_order`,`avatar`) VALUES
-- 魏·橙 (按slot顺序)
( 1,'张辽','魏', 1, 1,'images/general/4151.jpg'),
( 2,'许褚','魏', 2, 2,'images/general/4152.jpg'),
( 3,'乐进','魏', 3, 3,'images/general/4153.jpg'),
( 4,'徐晃','魏', 4, 4,'images/general/4154.jpg'),
( 5,'张郃','魏', 5, 5,'images/general/4155.jpg'),
( 6,'于禁','魏', 6, 6,'images/general/4156.jpg'),
( 7,'典韦','魏', 7, 7,'images/general/4157.jpg'),
-- 蜀·橙
( 8,'关羽','蜀', 1, 8,'images/general/4251.jpg'),
( 9,'赵云','蜀', 2, 9,'images/general/4252.jpg'),
(10,'马超','蜀', 3,10,'images/general/4253.jpg'),
(11,'黄忠','蜀', 4,11,'images/general/4254.jpg'),
(12,'姜维','蜀', 5,12,'images/general/4255.jpg'),
(13,'张飞','蜀', 6,13,'images/general/4256.jpg'),
(14,'魏延','蜀', 7,14,'images/general/4257.jpg'),
-- 吴·橙
(15,'凌统','吴', 1,15,'images/general/4351.jpg'),
(16,'太史慈','吴',2,16,'images/general/4352.jpg'),
(17,'吕蒙','吴', 3,17,'images/general/4353.jpg'),
(18,'陆逊','吴', 4,18,'images/general/4354.jpg'),
(19,'甘宁','吴', 5,19,'images/general/4355.jpg'),
(20,'徐盛','吴', 6,20,'images/general/4356.jpg'),
(21,'周泰','吴', 7,21,'images/general/4357.jpg'),
-- 群·橙
(22,'吕布','群', 8,22,'images/general/4051.jpg'),
(23,'貂蝉','群', 9,23,'images/general/4052.jpg');

-- -----------------------------------------------
-- 紫色 (28人)
-- slot10: 曹仁/关平/丁奉  slot11: 曹洪/廖化/韩当
-- slot12: 庞德/张翼/朱桓  slot13: 曹真/关索/凌操
-- slot14: 夏侯渊/关兴/程普 slot15: 夏侯惇/张苞/黄盖
-- slot16: 李典/向宠/陆抗
-- slot17-23: 群独占
-- -----------------------------------------------
INSERT INTO `general_template` (`id`,`name`,`faction`,`slot_id`,`sort_order`,`avatar`) VALUES
-- 魏·紫
(100,'曹仁','魏',  10,30,'images/general/4141.jpg'),
(101,'曹洪','魏',  11,31,'images/general/4142.jpg'),
(102,'庞德','魏',  12,32,'images/general/4143.jpg'),
(103,'曹真','魏',  13,33,'images/general/4144.jpg'),
(104,'夏侯渊','魏',14,34,'images/general/4145.jpg'),
(105,'夏侯惇','魏',15,35,'images/general/4146.jpg'),
(106,'李典','魏',  16,36,'images/general/4147.jpg'),
-- 蜀·紫
(107,'关平','蜀',  10,37,'images/general/4241.jpg'),
(108,'廖化','蜀',  11,38,'images/general/4242.jpg'),
(109,'张翼','蜀',  12,39,'images/general/4243.jpg'),
(110,'关索','蜀',  13,40,'images/general/4244.jpg'),
(111,'关兴','蜀',  14,41,'images/general/4245.jpg'),
(112,'张苞','蜀',  15,42,'images/general/4246.jpg'),
(113,'向宠','蜀',  16,43,'images/general/4247.jpg'),
-- 吴·紫
(114,'丁奉','吴',  10,44,'images/general/4341.jpg'),
(115,'韩当','吴',  11,45,'images/general/4342.jpg'),
(116,'朱桓','吴',  12,46,'images/general/4343.jpg'),
(117,'凌操','吴',  13,47,'images/general/4344.jpg'),
(118,'程普','吴',  14,48,'images/general/4345.jpg'),
(119,'黄盖','吴',  15,49,'images/general/4346.jpg'),
(120,'陆抗','吴',  16,50,'images/general/4347.jpg'),
-- 群·紫
(121,'华雄','群',  17,51,'images/general/4041.jpg'),
(122,'颜良','群',  18,52,'images/general/4042.jpg'),
(123,'马腾','群',  19,53,'images/general/4043.jpg'),
(124,'鞠义','群',  20,54,'images/general/4044.jpg'),
(125,'高顺','群',  21,55,'images/general/4045.jpg'),
(126,'文丑','群',  22,56,'images/general/4046.jpg'),
(127,'公孙瓒','群',23,57,'images/general/4047.jpg');

-- -----------------------------------------------
-- 蓝色 (20人)
-- slot24: 文聘/马岱/祖茂  slot25: 郝昭/高翔/蒋钦
-- slot26: 邓艾/刘封/全祎  slot27: 满宠/周仓/朱然
-- slot28-35: 群独占
-- -----------------------------------------------
INSERT INTO `general_template` (`id`,`name`,`faction`,`slot_id`,`sort_order`,`avatar`) VALUES
-- 魏·蓝
(200,'文聘','魏',24,60,'images/general/4131.jpg'),
(201,'郝昭','魏',25,61,'images/general/4132.jpg'),
(202,'邓艾','魏',26,62,'images/general/4133.jpg'),
(203,'满宠','魏',27,63,'images/general/4134.jpg'),
-- 蜀·蓝
(204,'马岱','蜀',24,64,'images/general/4231.jpg'),
(205,'高翔','蜀',25,65,'images/general/4232.jpg'),
(206,'刘封','蜀',26,66,'images/general/4233.jpg'),
(207,'周仓','蜀',27,67,'images/general/4234.jpg'),
-- 吴·蓝
(208,'祖茂','吴',24,68,'images/general/4331.jpg'),
(209,'蒋钦','吴',25,69,'images/general/4332.jpg'),
(210,'全祎','吴',26,70,'images/general/4333.jpg'),
(211,'朱然','吴',27,71,'images/general/4334.jpg'),
-- 群·蓝
(212,'潘凤','群',28,72,'images/general/4031.jpg'),
(213,'韩遂','群',29,73,'images/general/4032.jpg'),
(214,'韩性','群',30,74,'images/general/4033.jpg'),
(215,'曹猛','群',31,75,'images/general/4034.jpg'),
(216,'徐荣','群',32,76,'images/general/4035.jpg'),
(217,'孟获','群',33,77,'images/general/4036.jpg'),
(218,'张角','群',34,78,'images/general/4037.jpg'),
(219,'沙摩柯','群',35,79,'images/general/4038.jpg'),
-- 引导赠送
(220,'梁婉','群',48,80,'images/story/player/shoufa.png');

-- -----------------------------------------------
-- 绿色 (12人, 全群)
-- -----------------------------------------------
INSERT INTO `general_template` (`id`,`name`,`faction`,`slot_id`,`sort_order`,`avatar`) VALUES
(300,'张燕','群',36,80,'images/general/4021.jpg'),
(301,'宋宪','群',37,81,'images/general/4022.jpg'),
(302,'李催','群',38,82,'images/general/4023.jpg'),
(303,'郭汜','群',39,83,'images/general/4024.jpg'),
(304,'蔡瑁','群',40,84,'images/general/4025.jpg'),
(305,'邓贤','群',41,85,'images/general/4026.jpg'),
(306,'杨怀','群',42,86,'images/general/4027.jpg'),
(307,'泠苞','群',43,87,'images/general/4028.jpg'),
(308,'张英','群',44,88,'images/general/4029.jpg'),
(309,'雷铜','群',45,89,'images/general/4060.jpg'),
(310,'高沛','群',46,90,'images/general/4061.jpg'),
(311,'樊稠','群',47,91,'images/general/4062.jpg');

-- -----------------------------------------------
-- 紫色虚构 (50人, slot 50-57)
-- -----------------------------------------------
INSERT INTO `general_template` (`id`,`name`,`faction`,`slot_id`,`sort_order`,`avatar`) VALUES
-- slot50 步猛将 ×7
(400,'石友青','群',50,100,'wujiang1.png'),
(401,'段德凡','群',50,101,'wujiang2.png'),
(402,'王腾君','群',50,102,'wujiang3.png'),
(403,'汪采波','群',50,103,'wujiang4.png'),
(404,'唐丁集','群',50,104,'wujiang5.png'),
(405,'李思远','群',50,105,'wujiang6.png'),
(406,'张婉清','群',50,106,'wujiang7.png'),
-- slot51 步智将 ×6
(407,'夏从丹','群',51,107,'wujiang8.png'),
(408,'陈友谅','群',51,108,'wujiang9.png'),
(409,'杨军','群',  51,109,'wujiang10.png'),
(410,'刘志明','群',51,110,'wujiang1.png'),
(411,'赵雨桐','群',51,111,'wujiang2.png'),
(412,'吴俊杰','群',51,112,'wujiang3.png'),
-- slot52 步统帅 ×6
(413,'秦夏容','群',52,113,'wujiang4.png'),
(414,'郭采凝','群',52,114,'wujiang5.png'),
(415,'魏香双','群',52,115,'wujiang6.png'),
(416,'周若兰','群',52,116,'wujiang7.png'),
(417,'徐静雅','群',52,117,'wujiang8.png'),
(418,'孙浩然','群',52,118,'wujiang9.png'),
-- slot53 骑猛将 ×7
(419,'马晓月','群',53,119,'wujiang10.png'),
(420,'朱文轩','群',53,120,'wujiang1.png'),
(421,'林志强','群',53,121,'wujiang2.png'),
(422,'何雨晴','群',53,122,'wujiang3.png'),
(423,'高逸凡','群',53,123,'wujiang4.png'),
(424,'谢云飞','群',53,124,'wujiang5.png'),
(425,'郑明辉','群',53,125,'wujiang6.png'),
-- slot54 骑智将 ×6
(426,'梁雅琴','群',54,126,'wujiang7.png'),
(427,'宋慧敏','群',54,127,'wujiang8.png'),
(428,'胡梦琪','群',54,128,'wujiang9.png'),
(429,'罗雪梅','群',54,129,'wujiang10.png'),
(430,'唐俊豪','群',54,130,'wujiang1.png'),
(431,'韩德明','群',54,131,'wujiang2.png'),
-- slot55 骑统帅 ×6
(432,'邓志伟','群',55,132,'wujiang3.png'),
(433,'彭俊峰','群',55,133,'wujiang4.png'),
(434,'曾雨欣','群',55,134,'wujiang5.png'),
(435,'萧文博','群',55,135,'wujiang6.png'),
(436,'田思涵','群',55,136,'wujiang7.png'),
(437,'许静雯','群',55,137,'wujiang8.png'),
-- slot56 弓猛将 ×6
(438,'曹雅婷','群',56,138,'wujiang9.png'),
(439,'冯晓燕','群',56,139,'wujiang10.png'),
(440,'董明远','群',56,140,'wujiang1.png'),
(441,'于婉如','群',56,141,'wujiang2.png'),
(442,'蒋俊熙','群',56,142,'wujiang3.png'),
(443,'苏志鹏','群',56,143,'wujiang4.png'),
-- slot57 弓智将 ×6
(444,'余德昌','群',57,144,'wujiang5.png'),
(445,'叶文斌','群',57,145,'wujiang6.png'),
(446,'程雨萱','群',57,146,'wujiang7.png'),
(447,'袁雪莲','群',57,147,'wujiang8.png'),
(448,'蔡晓琳','群',57,148,'wujiang9.png'),
(449,'杜若曦','群',57,149,'wujiang10.png');

-- -----------------------------------------------
-- 蓝色虚构 (10人, slot 60-63)
-- -----------------------------------------------
INSERT INTO `general_template` (`id`,`name`,`faction`,`slot_id`,`sort_order`,`avatar`) VALUES
(500,'郑博文','群',60,150,'wujiang1.png'),
(501,'高天宇','群',60,151,'wujiang2.png'),
(502,'韩景行','群',60,152,'wujiang3.png'),
(503,'曹文轩','群',61,153,'wujiang4.png'),
(504,'冯逸飞','群',61,154,'wujiang5.png'),
(505,'沈国栋','群',61,155,'wujiang6.png'),
(506,'邓思颖','群',62,156,'wujiang7.png'),
(507,'彭雪晴','群',62,157,'wujiang8.png'),
(508,'董诗涵','群',63,158,'wujiang9.png'),
(509,'范月华','群',63,159,'wujiang10.png');

-- -----------------------------------------------
-- 绿色虚构 (10人, slot 70-73)
-- -----------------------------------------------
INSERT INTO `general_template` (`id`,`name`,`faction`,`slot_id`,`sort_order`,`avatar`) VALUES
(600,'周子轩','群',70,160,'wujiang1.png'),
(601,'吴浩然','群',70,161,'wujiang2.png'),
(602,'赵明哲','群',70,162,'wujiang3.png'),
(603,'孙启航','群',71,163,'wujiang4.png'),
(604,'马天翔','群',71,164,'wujiang5.png'),
(605,'丁锐峰','群',71,165,'wujiang6.png'),
(606,'许瑶琳','群',72,166,'wujiang7.png'),
(607,'谢芷若','群',72,167,'wujiang8.png'),
(608,'宋雨薇','群',73,168,'wujiang9.png'),
(609,'唐诗韵','群',73,169,'wujiang10.png');

-- -----------------------------------------------
-- 白色虚构 (10人, slot 80-83)
-- -----------------------------------------------
INSERT INTO `general_template` (`id`,`name`,`faction`,`slot_id`,`sort_order`,`avatar`) VALUES
(700,'蒋子豪','群',80,170,'wujiang1.png'),
(701,'田浩宇','群',80,171,'wujiang2.png'),
(702,'潘睿阳','群',80,172,'wujiang3.png'),
(703,'任凌云','群',81,173,'wujiang4.png'),
(704,'姜逸尘','群',81,174,'wujiang5.png'),
(705,'方志远','群',81,175,'wujiang6.png'),
(706,'石若兰','群',82,176,'wujiang7.png'),
(707,'龙雅萱','群',82,177,'wujiang8.png'),
(708,'夏紫嫣','群',83,178,'wujiang9.png'),
(709,'秦月瑶','群',83,179,'wujiang10.png');


-- =============================================
-- 3. general_famous_trait — 全量重建
-- =============================================
-- 同slot武将有相同的效果(effect_type+value),但名称不同
-- 这是个性化的部分, 转国时名将特性跟着新武将走

DROP TABLE IF EXISTS `general_famous_trait`;
CREATE TABLE IF NOT EXISTS `general_famous_trait` (
  `id` INT AUTO_INCREMENT PRIMARY KEY,
  `general_template_id` INT NOT NULL COMMENT '关联 general_template.id',
  `trait_name` VARCHAR(20) NOT NULL,
  `trait_desc` VARCHAR(100) NOT NULL,
  `effect_type` VARCHAR(32) NOT NULL,
  `effect_value` INT NOT NULL DEFAULT 0,
  `troop_restrict` TINYINT DEFAULT 0 COMMENT '0=全 1=步 2=骑 3=弓',
  `sort_order` INT DEFAULT 0,
  INDEX idx_general_template_id (`general_template_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='名将特性表';

-- =====================================================
-- 橙色特性 (每人2个, 貂蝉3个)
-- 同slot武将效果相同, 名称不同(阵营风味)
-- =====================================================

-- --- slot1 (damage400 + count100): 张辽/关羽/凌统 ---
INSERT INTO general_famous_trait (general_template_id, trait_name, trait_desc, effect_type, effect_value, troop_restrict, sort_order) VALUES
( 1,'身先士卒','统领士卒伤害＋400','soldier_damage',400,0,1),
( 1,'大将之才','统领的士兵人数额外＋100','soldier_count',100,0,2),
( 8,'武    圣','统领士卒伤害＋400','soldier_damage',400,0,1),
( 8,'五虎上将','统领的士兵人数额外＋100','soldier_count',100,0,2),
(15,'旋略勇进','统领士卒伤害＋400','soldier_damage',400,0,1),
(15,'大将之才','统领的士兵人数额外＋100','soldier_count',100,0,2);

-- --- slot2 (damage410 + 骑tactics): 许褚/赵云/太史慈 ---
INSERT INTO general_famous_trait (general_template_id, trait_name, trait_desc, effect_type, effect_value, troop_restrict, sort_order) VALUES
( 2,'赤膊上阵','统领士兵伤害＋410','soldier_damage',410,0,1),
( 2,'虎痴威名','骑兵兵法发动概率提升','troop_tactics',0,2,2),
( 9,'龙胆英雄','统领士兵伤害＋410','soldier_damage',410,0,1),
( 9,'白马义从','骑兵兵法发动概率提升','troop_tactics',0,2,2),
(16,'气勇胆烈','统领士兵伤害＋410','soldier_damage',410,0,1),
(16,'狼骑悍将','骑兵兵法发动概率提升','troop_tactics',0,2,2);

-- --- slot3 (mob4 + 骑/兵法tactics): 乐进/马超/吕蒙 ---
INSERT INTO general_famous_trait (general_template_id, trait_name, trait_desc, effect_type, effect_value, troop_restrict, sort_order) VALUES
( 3,'迅猛先登','军队机动性＋4','army_mobility',4,0,1),
( 3,'并驾齐驱','骑兵兵法发动概率提升','troop_tactics',0,2,2),
(10,'雄烈奋进','军队机动性＋4','army_mobility',4,0,1),
(10,'铁骑世家','骑兵兵法发动概率提升','troop_tactics',0,2,2),
(17,'见状明判','军队机动性＋4','army_mobility',4,0,1),
(17,'熟读兵书','兵法发动概率提升','tactics_prob',0,0,2);

-- --- slot4 (弓tactics + 弓damage390): 徐晃/黄忠/陆逊 ---
INSERT INTO general_famous_trait (general_template_id, trait_name, trait_desc, effect_type, effect_value, troop_restrict, sort_order) VALUES
( 4,'统御弓兵','射击类兵法概率提升','troop_tactics',0,3,1),
( 4,'齐射之阵','弓兵部队伤害＋390','troop_damage',390,3,2),
(11,'神 射 手','射击类兵法概率提升','troop_tactics',0,3,1),
(11,'老当益壮','弓兵部队伤害＋390','troop_damage',390,3,2),
(18,'审时度势','善于捕捉战机，提升兵法发动概率','tactics_prob',0,0,1),
(18,'火烧连营','弓兵部队伤害＋390','troop_damage',390,3,2);

-- --- slot5 (mob4 + tactics_prob): 张郃/姜维/甘宁 ---
INSERT INTO general_famous_trait (general_template_id, trait_name, trait_desc, effect_type, effect_value, troop_restrict, sort_order) VALUES
( 5,'先发制人','军队机动性＋4','army_mobility',4,0,1),
( 5,'临敌巧变','兵法发动概率提升','tactics_prob',0,0,2),
(12,'料敌机先','军队机动性＋4','army_mobility',4,0,1),
(12,'兵法大家','兵法发动概率提升','tactics_prob',0,0,2),
(19,'轻舟快马','军队机动性＋4','army_mobility',4,0,1),
(19,'百步穿杨','兵法发动概率提升','tactics_prob',0,0,2);

-- --- slot6 (life12% + count100): 于禁/张飞/徐盛 ---
INSERT INTO general_famous_trait (general_template_id, trait_name, trait_desc, effect_type, effect_value, troop_restrict, sort_order) VALUES
( 6,'统兵毅重','士兵生命值＋12%','soldier_life_pct',12,0,1),
( 6,'五子良将','统领的士兵人数额外＋100','soldier_count',100,0,2),
(13,'刚猛不屈','士兵生命值＋12%','soldier_life_pct',12,0,1),
(13,'五虎上将','统领的士兵人数额外＋100','soldier_count',100,0,2),
(20,'浴血奋战','士兵生命值＋12%','soldier_life_pct',12,0,1),
(20,'治军有道','统领的士兵人数额外＋100','soldier_count',100,0,2);

-- --- slot7 (defense400 + dodge12): 典韦/魏延/周泰 ---
INSERT INTO general_famous_trait (general_template_id, trait_name, trait_desc, effect_type, effect_value, troop_restrict, sort_order) VALUES
( 7,'恶来铁卫','军队防御＋400','army_defense',400,0,1),
( 7,'折冲之军','军队闪避＋12','army_dodge',12,0,2),
(14,'勇烈刚毅','军队防御＋400','army_defense',400,0,1),
(14,'折冲外御','军队闪避＋12','army_dodge',12,0,2),
(21,'豪勇持重','军队防御＋400','army_defense',400,0,1),
(21,'折冲外御','军队闪避＋12','army_dodge',12,0,2);

-- --- slot8: 吕布 ---
INSERT INTO general_famous_trait (general_template_id, trait_name, trait_desc, effect_type, effect_value, troop_restrict, sort_order) VALUES
(22,'战    神','属下士兵伤害＋500','soldier_damage',500,0,1),
(22,'赤兔飞将','骑兵兵法发动概率增加','troop_tactics',0,2,2);

-- --- slot9: 貂蝉 (3特性) ---
INSERT INTO general_famous_trait (general_template_id, trait_name, trait_desc, effect_type, effect_value, troop_restrict, sort_order) VALUES
(23,'神魂颠倒','干扰敌军士气，免疫偷袭类兵法','immune_ambush',1,0,1),
(23,'鼓舞士气','激励士兵奋勇争先，军队机动性＋3','army_mobility',3,0,2),
(23,'连 环 计','提升兵法发动概率','tactics_prob',0,0,3);

-- =====================================================
-- 紫色特性 (每人1个)
-- 同slot武将效果相同, 名称不同
-- =====================================================

-- --- slot10 (骑damage320): 曹仁/关平/丁奉 ---
INSERT INTO general_famous_trait (general_template_id, trait_name, trait_desc, effect_type, effect_value, troop_restrict, sort_order) VALUES
(100,'虎豹骑兵','骑兵伤害＋320','troop_damage',320,2,1),
(107,'武圣遗风','士兵伤害＋320','soldier_damage',320,0,1),
(114,'雪奋短兵','骑兵伤害＋320','troop_damage',320,2,1);

-- --- slot11 (mob3): 曹洪/廖化/韩当 ---
INSERT INTO general_famous_trait (general_template_id, trait_name, trait_desc, effect_type, effect_value, troop_restrict, sort_order) VALUES
(101,'魏国先锋','军队机动性＋3','army_mobility',3,0,1),
(108,'蜀国先锋','军队机动性＋3','army_mobility',3,0,1),
(115,'吴国先锋','军队机动性＋3','army_mobility',3,0,1);

-- --- slot12 (骑attack300): 庞德/张翼/朱桓 ---
INSERT INTO general_famous_trait (general_template_id, trait_name, trait_desc, effect_type, effect_value, troop_restrict, sort_order) VALUES
(102,'西凉铁骑','骑兵部队攻击＋300','troop_attack',300,2,1),
(109,'亢维之锐','骑兵部队攻击＋300','troop_attack',300,2,1),
(116,'勇烈奋威','骑兵部队攻击＋300','troop_attack',300,2,1);

-- --- slot13 (army_attack310): 曹真/关索/凌操 ---
INSERT INTO general_famous_trait (general_template_id, trait_name, trait_desc, effect_type, effect_value, troop_restrict, sort_order) VALUES
(103,'神鬼之勇','军队攻击＋310','army_attack',310,0,1),
(110,'豪勇奋发','军队攻击＋310','army_attack',310,0,1),
(117,'骁果麤猛','军队攻击＋310','army_attack',310,0,1);

-- --- slot14 (弓tactics): 夏侯渊/关兴/程普 ---
INSERT INTO general_famous_trait (general_template_id, trait_name, trait_desc, effect_type, effect_value, troop_restrict, sort_order) VALUES
(104,'百步穿杨','弓兵兵法发动概率提升','troop_tactics',0,3,1),
(111,'射雕英雄','弓兵兵法发动概率提升','troop_tactics',0,3,1),
(118,'连珠神箭','弓兵兵法发动概率提升','troop_tactics',0,3,1);

-- --- slot15 (life12%): 夏侯惇/张苞/黄盖 ---
INSERT INTO general_famous_trait (general_template_id, trait_name, trait_desc, effect_type, effect_value, troop_restrict, sort_order) VALUES
(105,'悍勇不屈','士兵生命＋12%','soldier_life_pct',12,0,1),
(112,'悍勇不屈','士兵生命＋12%','soldier_life_pct',12,0,1),
(119,'苦 肉 计','士兵生命＋12%','soldier_life_pct',12,0,1);

-- --- slot16 (defense350): 李典/向宠/陆抗 ---
INSERT INTO general_famous_trait (general_template_id, trait_name, trait_desc, effect_type, effect_value, troop_restrict, sort_order) VALUES
(106,'豪勇持重','军队防御＋350','army_defense',350,0,1),
(113,'行阵和睦','军队防御＋350','army_defense',350,0,1),
(120,'豪勇持重','军队防御＋350','army_defense',350,0,1);

-- --- 群紫色 slot17-23 (各1个) ---
INSERT INTO general_famous_trait (general_template_id, trait_name, trait_desc, effect_type, effect_value, troop_restrict, sort_order) VALUES
(121,'虎牢扬威','统领士兵伤害＋330','soldier_damage',330,0,1),
(122,'冠军之勇','骑兵部队攻击＋310','troop_attack',310,2,1),
(123,'铁骑世家','提升骑兵兵法触发概率','troop_tactics',0,2,1),
(124,'先登死士','弓兵射击伤害＋320','troop_damage',320,3,1),
(125,'陷阵之将','步兵部队防御＋350','troop_defense',350,1,1),
(126,'庭柱之将','军队攻击抵抗＋330','damage_resist',330,0,1),
(127,'白马义从','骑兵兵法发动概率提升','troop_tactics',0,2,1);

-- =====================================================
-- 蓝色特性 (每人1个)
-- =====================================================

-- slot24 (damage190): 文聘/马岱/祖茂
INSERT INTO general_famous_trait (general_template_id, trait_name, trait_desc, effect_type, effect_value, troop_restrict, sort_order) VALUES
(200,'武勇过人','军队士兵伤害＋190','soldier_damage',190,0,1),
(204,'武勇过人','军队士兵伤害＋190','soldier_damage',190,0,1),
(208,'武勇过人','军队士兵伤害＋190','soldier_damage',190,0,1);

-- slot25 (mob2): 郝昭/高翔/蒋钦
INSERT INTO general_famous_trait (general_template_id, trait_name, trait_desc, effect_type, effect_value, troop_restrict, sort_order) VALUES
(201,'抢占先机','部队机动性＋2','army_mobility',2,0,1),
(205,'抢占先机','部队机动性＋2','army_mobility',2,0,1),
(209,'抢占先机','部队机动性＋2','army_mobility',2,0,1);

-- slot26 (弓damage200): 邓艾/刘封/全祎
INSERT INTO general_famous_trait (general_template_id, trait_name, trait_desc, effect_type, effect_value, troop_restrict, sort_order) VALUES
(202,'弓马娴熟','弓兵部队伤害＋200','troop_damage',200,3,1),
(206,'弓马娴熟','弓兵部队伤害＋200','troop_damage',200,3,1),
(210,'弓马娴熟','弓兵部队伤害＋200','troop_damage',200,3,1);

-- slot27 (步defense200): 满宠/周仓/朱然
INSERT INTO general_famous_trait (general_template_id, trait_name, trait_desc, effect_type, effect_value, troop_restrict, sort_order) VALUES
(203,'立志刚毅','步兵士兵防御＋200','troop_defense',200,1,1),
(207,'忠勇坚韧','步兵士兵防御＋200','troop_defense',200,1,1),
(211,'胆守无惧','步兵士兵防御＋200','troop_defense',200,1,1);

-- 群蓝色 slot28-35
INSERT INTO general_famous_trait (general_template_id, trait_name, trait_desc, effect_type, effect_value, troop_restrict, sort_order) VALUES
(212,'饥渴大斧','属下军队士兵伤害＋150','soldier_damage',150,0,1),
(213,'西凉铁骑','骑兵部队伤害＋160','troop_damage',160,2,1),
(214,'狙杀射手','弓兵部队伤害＋150','troop_damage',150,3,1),
(215,'弓兵之术','提升弓兵兵法发动概率','troop_tactics',0,3,1),
(216,'坚韧持重','士兵生命＋7%','soldier_life_pct',7,0,1),
(217,'藤甲之术','士兵伤害抵抗＋180','damage_resist',180,0,1),
(218,'太平遁术','军队闪避＋7','army_dodge',7,0,1),
(219,'蛮族之箭','弓兵部队伤害＋160','troop_damage',160,3,1),
-- 引导赠送
(220,'初心守护','步兵部队防御＋150','troop_defense',150,1,1);

-- =====================================================
-- 绿色特性 (每人1个, 全群)
-- =====================================================
INSERT INTO general_famous_trait (general_template_id, trait_name, trait_desc, effect_type, effect_value, troop_restrict, sort_order) VALUES
(300,'黑山骑兵','骑兵部队伤害＋80','troop_damage',80,2,1),
(301,'强化攻击','士兵攻击＋160','army_attack',160,0,1),
(302,'骑兵突击','小幅提升骑兵兵法发动概率','troop_tactics',0,2,1),
(303,'迅捷如风','士兵机动＋1','army_mobility',1,0,1),
(304,'善射之士','远程部队伤害＋85','troop_damage',85,3,1),
(305,'射术精通','远程士兵攻击＋81','troop_attack',81,3,1),
(306,'弓兵统领','小幅提升弓兵部队兵法发动概率','troop_tactics',0,3,1),
(307,'快速反应','士兵机动＋1','army_mobility',1,0,1),
(308,'严防死守','军队防御＋85','army_defense',85,0,1),
(309,'秉性坚韧','士兵生命＋4%','soldier_life_pct',4,0,1),
(310,'用兵灵动','士兵闪避＋3','army_dodge',3,0,1),
(311,'统兵之术','统领兵力＋20','soldier_count',20,0,1);


-- =============================================
-- 4. 废弃表清理
-- =============================================
DROP TABLE IF EXISTS `general_slot_trait`;
