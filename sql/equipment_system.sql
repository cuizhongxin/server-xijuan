-- ============================================================
-- 装备系统 SQL - 建表 + 初始化数据
-- ============================================================

-- 装备模板表（如果不存在则创建）
CREATE TABLE IF NOT EXISTS `equipment_pre` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(50) NOT NULL COMMENT '装备名称',
  `level` int(11) NOT NULL DEFAULT 1 COMMENT '装备等级',
  `source` varchar(30) NOT NULL DEFAULT '副本掉落' COMMENT '获取来源: 副本掉落/副本产出/手工制作/秘境产出/天地宝箱',
  `position` varchar(10) NOT NULL COMMENT '装备部位: 武器/戒指/铠甲/项链/头盔/鞋子',
  `set_name` varchar(20) DEFAULT NULL COMMENT '套装名称',
  `set_effect_3` varchar(100) DEFAULT NULL COMMENT '3件套效果',
  `set_effect_6` varchar(100) DEFAULT NULL COMMENT '6件套效果',
  `attack` int(11) NOT NULL DEFAULT 0 COMMENT '攻击力',
  `defense` int(11) NOT NULL DEFAULT 0 COMMENT '防御力',
  `soldier_hp` int(11) NOT NULL DEFAULT 0 COMMENT '士兵生命',
  `mobility` int(11) NOT NULL DEFAULT 0 COMMENT '机动性',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='装备模板表';

-- 清空旧数据（如果需要重新导入）
-- TRUNCATE TABLE equipment_pre;

-- ============================================================
-- 1级 - 新手套装（副本掉落，无套装效果）
-- 掉落来源: DUNGEON_1 最终Boss
-- ============================================================
INSERT INTO `equipment_pre` (`id`,`name`,`level`,`source`,`position`,`attack`,`defense`,`soldier_hp`,`mobility`) VALUES (1,'新手长剑',1,'副本掉落','武器',10,0,0,0);
INSERT INTO `equipment_pre` (`id`,`name`,`level`,`source`,`position`,`attack`,`defense`,`soldier_hp`,`mobility`) VALUES (2,'新手戒指',1,'副本掉落','戒指',3,0,0,0);
INSERT INTO `equipment_pre` (`id`,`name`,`level`,`source`,`position`,`attack`,`defense`,`soldier_hp`,`mobility`) VALUES (3,'新手布甲',1,'副本掉落','铠甲',0,15,0,0);
INSERT INTO `equipment_pre` (`id`,`name`,`level`,`source`,`position`,`attack`,`defense`,`soldier_hp`,`mobility`) VALUES (4,'新手项链',1,'副本掉落','项链',0,0,50,0);
INSERT INTO `equipment_pre` (`id`,`name`,`level`,`source`,`position`,`attack`,`defense`,`soldier_hp`,`mobility`) VALUES (5,'新手布帽',1,'副本掉落','头盔',0,5,0,0);
INSERT INTO `equipment_pre` (`id`,`name`,`level`,`source`,`position`,`attack`,`defense`,`soldier_hp`,`mobility`) VALUES (6,'新手布鞋',1,'副本掉落','鞋子',0,0,0,5);

-- ============================================================
-- 20级 - 宣武套装（副本掉落，无套装效果）
-- 掉落来源: DUNGEON_20 最终Boss
-- ============================================================
INSERT INTO `equipment_pre` (`id`,`name`,`level`,`source`,`position`,`attack`,`defense`,`soldier_hp`,`mobility`) VALUES (7,'宣武长剑',20,'副本掉落','武器',50,0,0,0);
INSERT INTO `equipment_pre` (`id`,`name`,`level`,`source`,`position`,`attack`,`defense`,`soldier_hp`,`mobility`) VALUES (8,'宣武戒指',20,'副本掉落','戒指',15,0,0,0);
INSERT INTO `equipment_pre` (`id`,`name`,`level`,`source`,`position`,`attack`,`defense`,`soldier_hp`,`mobility`) VALUES (9,'宣武战甲',20,'副本掉落','铠甲',0,80,0,0);
INSERT INTO `equipment_pre` (`id`,`name`,`level`,`source`,`position`,`attack`,`defense`,`soldier_hp`,`mobility`) VALUES (10,'宣武项链',20,'副本掉落','项链',0,0,200,0);
INSERT INTO `equipment_pre` (`id`,`name`,`level`,`source`,`position`,`attack`,`defense`,`soldier_hp`,`mobility`) VALUES (11,'宣武头盔',20,'副本掉落','头盔',0,25,0,0);
INSERT INTO `equipment_pre` (`id`,`name`,`level`,`source`,`position`,`attack`,`defense`,`soldier_hp`,`mobility`) VALUES (12,'宣武战靴',20,'副本掉落','鞋子',0,0,0,20);

-- ============================================================
-- 40级 - 折冲套装（手工制作，军械局制作）
-- 制作消耗: 纸张50 + 金属100 + 银币5000 + 木材80
-- ============================================================
INSERT INTO `equipment_pre` (`id`,`name`,`level`,`source`,`position`,`set_name`,`set_effect_3`,`set_effect_6`,`attack`,`defense`,`soldier_hp`,`mobility`) VALUES (13,'折冲长剑',40,'手工制作','武器','折冲','攻击+60','士兵生命+20',120,0,0,0);
INSERT INTO `equipment_pre` (`id`,`name`,`level`,`source`,`position`,`set_name`,`set_effect_3`,`set_effect_6`,`attack`,`defense`,`soldier_hp`,`mobility`) VALUES (14,'折冲戒指',40,'手工制作','戒指','折冲','攻击+60','士兵生命+20',40,0,0,0);
INSERT INTO `equipment_pre` (`id`,`name`,`level`,`source`,`position`,`set_name`,`set_effect_3`,`set_effect_6`,`attack`,`defense`,`soldier_hp`,`mobility`) VALUES (15,'折冲战甲',40,'手工制作','铠甲','折冲','攻击+60','士兵生命+20',0,150,0,0);
INSERT INTO `equipment_pre` (`id`,`name`,`level`,`source`,`position`,`set_name`,`set_effect_3`,`set_effect_6`,`attack`,`defense`,`soldier_hp`,`mobility`) VALUES (16,'折冲项链',40,'手工制作','项链','折冲','攻击+60','士兵生命+20',0,0,400,0);
INSERT INTO `equipment_pre` (`id`,`name`,`level`,`source`,`position`,`set_name`,`set_effect_3`,`set_effect_6`,`attack`,`defense`,`soldier_hp`,`mobility`) VALUES (17,'折冲头盔',40,'手工制作','头盔','折冲','攻击+60','士兵生命+20',0,50,0,0);
INSERT INTO `equipment_pre` (`id`,`name`,`level`,`source`,`position`,`set_name`,`set_effect_3`,`set_effect_6`,`attack`,`defense`,`soldier_hp`,`mobility`) VALUES (18,'折冲战靴',40,'手工制作','鞋子','折冲','攻击+60','士兵生命+20',0,0,0,40);

-- ============================================================
-- 40级 - 陷阵套装（副本掉落）
-- 掉落来源: DUNGEON_40 最终Boss
-- ============================================================
INSERT INTO `equipment_pre` (`id`,`name`,`level`,`source`,`position`,`set_name`,`set_effect_3`,`set_effect_6`,`attack`,`defense`,`soldier_hp`,`mobility`) VALUES (19,'陷阵长枪',40,'副本掉落','武器','陷阵','攻击+100','防御+30',130,0,0,0);
INSERT INTO `equipment_pre` (`id`,`name`,`level`,`source`,`position`,`set_name`,`set_effect_3`,`set_effect_6`,`attack`,`defense`,`soldier_hp`,`mobility`) VALUES (20,'陷阵戒指',40,'副本掉落','戒指','陷阵','攻击+100','防御+30',45,0,0,0);
INSERT INTO `equipment_pre` (`id`,`name`,`level`,`source`,`position`,`set_name`,`set_effect_3`,`set_effect_6`,`attack`,`defense`,`soldier_hp`,`mobility`) VALUES (21,'陷阵重甲',40,'副本掉落','铠甲','陷阵','攻击+100','防御+30',0,160,0,0);
INSERT INTO `equipment_pre` (`id`,`name`,`level`,`source`,`position`,`set_name`,`set_effect_3`,`set_effect_6`,`attack`,`defense`,`soldier_hp`,`mobility`) VALUES (22,'陷阵项链',40,'副本掉落','项链','陷阵','攻击+100','防御+30',0,0,450,0);
INSERT INTO `equipment_pre` (`id`,`name`,`level`,`source`,`position`,`set_name`,`set_effect_3`,`set_effect_6`,`attack`,`defense`,`soldier_hp`,`mobility`) VALUES (23,'陷阵头盔',40,'副本掉落','头盔','陷阵','攻击+100','防御+30',0,55,0,0);
INSERT INTO `equipment_pre` (`id`,`name`,`level`,`source`,`position`,`set_name`,`set_effect_3`,`set_effect_6`,`attack`,`defense`,`soldier_hp`,`mobility`) VALUES (24,'陷阵战靴',40,'副本掉落','鞋子','陷阵','攻击+100','防御+30',0,0,0,45);

-- ============================================================
-- 40级 - 鹰扬套装（秘境产出）
-- ============================================================
INSERT INTO `equipment_pre` (`id`,`name`,`level`,`source`,`position`,`set_name`,`set_effect_3`,`set_effect_6`,`attack`,`defense`,`soldier_hp`,`mobility`) VALUES (25,'鹰扬战刀',40,'秘境产出','武器','鹰扬','防御+100','统御+12',110,0,0,0);
INSERT INTO `equipment_pre` (`id`,`name`,`level`,`source`,`position`,`set_name`,`set_effect_3`,`set_effect_6`,`attack`,`defense`,`soldier_hp`,`mobility`) VALUES (26,'鹰扬戒指',40,'秘境产出','戒指','鹰扬','防御+100','统御+12',35,0,0,0);
INSERT INTO `equipment_pre` (`id`,`name`,`level`,`source`,`position`,`set_name`,`set_effect_3`,`set_effect_6`,`attack`,`defense`,`soldier_hp`,`mobility`) VALUES (27,'鹰扬护甲',40,'秘境产出','铠甲','鹰扬','防御+100','统御+12',0,200,0,0);
INSERT INTO `equipment_pre` (`id`,`name`,`level`,`source`,`position`,`set_name`,`set_effect_3`,`set_effect_6`,`attack`,`defense`,`soldier_hp`,`mobility`) VALUES (28,'鹰扬项链',40,'秘境产出','项链','鹰扬','防御+100','统御+12',0,0,500,0);
INSERT INTO `equipment_pre` (`id`,`name`,`level`,`source`,`position`,`set_name`,`set_effect_3`,`set_effect_6`,`attack`,`defense`,`soldier_hp`,`mobility`) VALUES (29,'鹰扬头盔',40,'秘境产出','头盔','鹰扬','防御+100','统御+12',0,70,0,0);
INSERT INTO `equipment_pre` (`id`,`name`,`level`,`source`,`position`,`set_name`,`set_effect_3`,`set_effect_6`,`attack`,`defense`,`soldier_hp`,`mobility`) VALUES (30,'鹰扬战靴',40,'秘境产出','鞋子','鹰扬','防御+100','统御+12',0,0,0,50);

-- ============================================================
-- 50级 - 狂战套装（副本产出）
-- 掉落来源: DUNGEON_60 第10个NPC
-- ============================================================
INSERT INTO `equipment_pre` (`id`,`name`,`level`,`source`,`position`,`set_name`,`set_effect_3`,`set_effect_6`,`attack`,`defense`,`soldier_hp`,`mobility`) VALUES (31,'狂战巨斧',50,'副本产出','武器','狂战','攻击+150','防御+60',180,0,0,0);
INSERT INTO `equipment_pre` (`id`,`name`,`level`,`source`,`position`,`set_name`,`set_effect_3`,`set_effect_6`,`attack`,`defense`,`soldier_hp`,`mobility`) VALUES (32,'狂战戒指',50,'副本产出','戒指','狂战','攻击+150','防御+60',60,0,0,0);
INSERT INTO `equipment_pre` (`id`,`name`,`level`,`source`,`position`,`set_name`,`set_effect_3`,`set_effect_6`,`attack`,`defense`,`soldier_hp`,`mobility`) VALUES (33,'狂战重甲',50,'副本产出','铠甲','狂战','攻击+150','防御+60',0,220,0,0);
INSERT INTO `equipment_pre` (`id`,`name`,`level`,`source`,`position`,`set_name`,`set_effect_3`,`set_effect_6`,`attack`,`defense`,`soldier_hp`,`mobility`) VALUES (34,'狂战项链',50,'副本产出','项链','狂战','攻击+150','防御+60',0,0,600,0);
INSERT INTO `equipment_pre` (`id`,`name`,`level`,`source`,`position`,`set_name`,`set_effect_3`,`set_effect_6`,`attack`,`defense`,`soldier_hp`,`mobility`) VALUES (35,'狂战头盔',50,'副本产出','头盔','狂战','攻击+150','防御+60',0,75,0,0);
INSERT INTO `equipment_pre` (`id`,`name`,`level`,`source`,`position`,`set_name`,`set_effect_3`,`set_effect_6`,`attack`,`defense`,`soldier_hp`,`mobility`) VALUES (36,'狂战战靴',50,'副本产出','鞋子','狂战','攻击+150','防御+60',0,0,0,60);

-- ============================================================
-- 60级 - 天狼套装（副本产出）
-- 掉落来源: DUNGEON_60 最终Boss
-- ============================================================
INSERT INTO `equipment_pre` (`id`,`name`,`level`,`source`,`position`,`set_name`,`set_effect_3`,`set_effect_6`,`attack`,`defense`,`soldier_hp`,`mobility`) VALUES (37,'天狼战刃',60,'副本产出','武器','天狼','攻击+200','防御+100',250,0,0,0);
INSERT INTO `equipment_pre` (`id`,`name`,`level`,`source`,`position`,`set_name`,`set_effect_3`,`set_effect_6`,`attack`,`defense`,`soldier_hp`,`mobility`) VALUES (38,'天狼戒指',60,'副本产出','戒指','天狼','攻击+200','防御+100',80,0,0,0);
INSERT INTO `equipment_pre` (`id`,`name`,`level`,`source`,`position`,`set_name`,`set_effect_3`,`set_effect_6`,`attack`,`defense`,`soldier_hp`,`mobility`) VALUES (39,'天狼战甲',60,'副本产出','铠甲','天狼','攻击+200','防御+100',0,300,0,0);
INSERT INTO `equipment_pre` (`id`,`name`,`level`,`source`,`position`,`set_name`,`set_effect_3`,`set_effect_6`,`attack`,`defense`,`soldier_hp`,`mobility`) VALUES (40,'天狼项链',60,'副本产出','项链','天狼','攻击+200','防御+100',0,0,800,0);
INSERT INTO `equipment_pre` (`id`,`name`,`level`,`source`,`position`,`set_name`,`set_effect_3`,`set_effect_6`,`attack`,`defense`,`soldier_hp`,`mobility`) VALUES (41,'天狼头盔',60,'副本产出','头盔','天狼','攻击+200','防御+100',0,100,0,0);
INSERT INTO `equipment_pre` (`id`,`name`,`level`,`source`,`position`,`set_name`,`set_effect_3`,`set_effect_6`,`attack`,`defense`,`soldier_hp`,`mobility`) VALUES (42,'天狼战靴',60,'副本产出','鞋子','天狼','攻击+200','防御+100',0,0,0,80);

-- ============================================================
-- 60级 - 玄铁套装（手工制作，军械局制作）
-- 制作消耗: 纸张120 + 金属250 + 银币15000 + 木材200
-- ============================================================
INSERT INTO `equipment_pre` (`id`,`name`,`level`,`source`,`position`,`set_name`,`set_effect_3`,`set_effect_6`,`attack`,`defense`,`soldier_hp`,`mobility`) VALUES (43,'玄铁重剑',60,'手工制作','武器','玄铁','攻击+160','士兵生命+40',230,0,0,0);
INSERT INTO `equipment_pre` (`id`,`name`,`level`,`source`,`position`,`set_name`,`set_effect_3`,`set_effect_6`,`attack`,`defense`,`soldier_hp`,`mobility`) VALUES (44,'玄铁戒指',60,'手工制作','戒指','玄铁','攻击+160','士兵生命+40',75,0,0,0);
INSERT INTO `equipment_pre` (`id`,`name`,`level`,`source`,`position`,`set_name`,`set_effect_3`,`set_effect_6`,`attack`,`defense`,`soldier_hp`,`mobility`) VALUES (45,'玄铁战甲',60,'手工制作','铠甲','玄铁','攻击+160','士兵生命+40',0,280,0,0);
INSERT INTO `equipment_pre` (`id`,`name`,`level`,`source`,`position`,`set_name`,`set_effect_3`,`set_effect_6`,`attack`,`defense`,`soldier_hp`,`mobility`) VALUES (46,'玄铁项链',60,'手工制作','项链','玄铁','攻击+160','士兵生命+40',0,0,900,0);
INSERT INTO `equipment_pre` (`id`,`name`,`level`,`source`,`position`,`set_name`,`set_effect_3`,`set_effect_6`,`attack`,`defense`,`soldier_hp`,`mobility`) VALUES (47,'玄铁头盔',60,'手工制作','头盔','玄铁','攻击+160','士兵生命+40',0,90,0,0);
INSERT INTO `equipment_pre` (`id`,`name`,`level`,`source`,`position`,`set_name`,`set_effect_3`,`set_effect_6`,`attack`,`defense`,`soldier_hp`,`mobility`) VALUES (48,'玄铁战靴',60,'手工制作','鞋子','玄铁','攻击+160','士兵生命+40',0,0,0,75);

-- ============================================================
-- 60级 - 虎啸套装（秘境产出）
-- ============================================================
INSERT INTO `equipment_pre` (`id`,`name`,`level`,`source`,`position`,`set_name`,`set_effect_3`,`set_effect_6`,`attack`,`defense`,`soldier_hp`,`mobility`) VALUES (49,'虎啸战刀',60,'秘境产出','武器','虎啸','防御+400','闪避率+20%，统御+50',200,0,0,0);
INSERT INTO `equipment_pre` (`id`,`name`,`level`,`source`,`position`,`set_name`,`set_effect_3`,`set_effect_6`,`attack`,`defense`,`soldier_hp`,`mobility`) VALUES (50,'虎啸戒指',60,'秘境产出','戒指','虎啸','防御+400','闪避率+20%，统御+50',65,0,0,0);
INSERT INTO `equipment_pre` (`id`,`name`,`level`,`source`,`position`,`set_name`,`set_effect_3`,`set_effect_6`,`attack`,`defense`,`soldier_hp`,`mobility`) VALUES (51,'虎啸护甲',60,'秘境产出','铠甲','虎啸','防御+400','闪避率+20%，统御+50',0,500,0,0);
INSERT INTO `equipment_pre` (`id`,`name`,`level`,`source`,`position`,`set_name`,`set_effect_3`,`set_effect_6`,`attack`,`defense`,`soldier_hp`,`mobility`) VALUES (52,'虎啸项链',60,'秘境产出','项链','虎啸','防御+400','闪避率+20%，统御+50',0,0,1000,0);
INSERT INTO `equipment_pre` (`id`,`name`,`level`,`source`,`position`,`set_name`,`set_effect_3`,`set_effect_6`,`attack`,`defense`,`soldier_hp`,`mobility`) VALUES (53,'虎啸头盔',60,'秘境产出','头盔','虎啸','防御+400','闪避率+20%，统御+50',0,170,0,0);
INSERT INTO `equipment_pre` (`id`,`name`,`level`,`source`,`position`,`set_name`,`set_effect_3`,`set_effect_6`,`attack`,`defense`,`soldier_hp`,`mobility`) VALUES (54,'虎啸战靴',60,'秘境产出','鞋子','虎啸','防御+400','闪避率+20%，统御+50',0,0,0,85);

-- ============================================================
-- 60级 - 熊王套装（副本产出）
-- 掉落来源: DUNGEON_60 最终Boss（与天狼套装共享掉落池）
-- ============================================================
INSERT INTO `equipment_pre` (`id`,`name`,`level`,`source`,`position`,`set_name`,`set_effect_3`,`set_effect_6`,`attack`,`defense`,`soldier_hp`,`mobility`) VALUES (55,'熊王巨锤',60,'副本产出','武器','熊王','攻击+300','防御+100',300,0,0,0);
INSERT INTO `equipment_pre` (`id`,`name`,`level`,`source`,`position`,`set_name`,`set_effect_3`,`set_effect_6`,`attack`,`defense`,`soldier_hp`,`mobility`) VALUES (56,'熊王戒指',60,'副本产出','戒指','熊王','攻击+300','防御+100',100,0,0,0);
INSERT INTO `equipment_pre` (`id`,`name`,`level`,`source`,`position`,`set_name`,`set_effect_3`,`set_effect_6`,`attack`,`defense`,`soldier_hp`,`mobility`) VALUES (57,'熊王重甲',60,'副本产出','铠甲','熊王','攻击+300','防御+100',0,350,0,0);
INSERT INTO `equipment_pre` (`id`,`name`,`level`,`source`,`position`,`set_name`,`set_effect_3`,`set_effect_6`,`attack`,`defense`,`soldier_hp`,`mobility`) VALUES (58,'熊王项链',60,'副本产出','项链','熊王','攻击+300','防御+100',0,0,950,0);
INSERT INTO `equipment_pre` (`id`,`name`,`level`,`source`,`position`,`set_name`,`set_effect_3`,`set_effect_6`,`attack`,`defense`,`soldier_hp`,`mobility`) VALUES (59,'熊王头盔',60,'副本产出','头盔','熊王','攻击+300','防御+100',0,115,0,0);
INSERT INTO `equipment_pre` (`id`,`name`,`level`,`source`,`position`,`set_name`,`set_effect_3`,`set_effect_6`,`attack`,`defense`,`soldier_hp`,`mobility`) VALUES (60,'熊王战靴',60,'副本产出','鞋子','熊王','攻击+300','防御+100',0,0,0,90);

-- ============================================================
-- 70级 - 天诛/地煞/幽冥套装（天地宝箱）
-- 掉落来源: DUNGEON_80 第10个NPC
-- ============================================================
INSERT INTO `equipment_pre` (`id`,`name`,`level`,`source`,`position`,`set_name`,`set_effect_3`,`set_effect_6`,`attack`,`defense`,`soldier_hp`,`mobility`) VALUES (61,'天诛神剑',70,'天地宝箱','武器','天诛','攻击+600','武勇+30',450,0,0,0);
INSERT INTO `equipment_pre` (`id`,`name`,`level`,`source`,`position`,`set_name`,`set_effect_3`,`set_effect_6`,`attack`,`defense`,`soldier_hp`,`mobility`) VALUES (62,'天诛戒指',70,'天地宝箱','戒指','天诛','攻击+600','武勇+30',150,0,0,0);
INSERT INTO `equipment_pre` (`id`,`name`,`level`,`source`,`position`,`set_name`,`set_effect_3`,`set_effect_6`,`attack`,`defense`,`soldier_hp`,`mobility`) VALUES (63,'天诛战甲',70,'天地宝箱','铠甲','天诛','攻击+600','武勇+30',0,550,0,0);
INSERT INTO `equipment_pre` (`id`,`name`,`level`,`source`,`position`,`set_name`,`set_effect_3`,`set_effect_6`,`attack`,`defense`,`soldier_hp`,`mobility`) VALUES (64,'天诛项链',70,'天地宝箱','项链','天诛','攻击+600','武勇+30',0,0,1400,0);
INSERT INTO `equipment_pre` (`id`,`name`,`level`,`source`,`position`,`set_name`,`set_effect_3`,`set_effect_6`,`attack`,`defense`,`soldier_hp`,`mobility`) VALUES (65,'天诛头盔',70,'天地宝箱','头盔','天诛','攻击+600','武勇+30',0,180,0,0);
INSERT INTO `equipment_pre` (`id`,`name`,`level`,`source`,`position`,`set_name`,`set_effect_3`,`set_effect_6`,`attack`,`defense`,`soldier_hp`,`mobility`) VALUES (66,'天诛战靴',70,'天地宝箱','鞋子','天诛','攻击+600','武勇+30',0,0,0,120);
INSERT INTO `equipment_pre` (`id`,`name`,`level`,`source`,`position`,`set_name`,`set_effect_3`,`set_effect_6`,`attack`,`defense`,`soldier_hp`,`mobility`) VALUES (67,'地煞战刀',70,'天地宝箱','武器','地煞','防御+400','统御+20',380,0,0,0);
INSERT INTO `equipment_pre` (`id`,`name`,`level`,`source`,`position`,`set_name`,`set_effect_3`,`set_effect_6`,`attack`,`defense`,`soldier_hp`,`mobility`) VALUES (68,'地煞戒指',70,'天地宝箱','戒指','地煞','防御+400','统御+20',125,0,0,0);
INSERT INTO `equipment_pre` (`id`,`name`,`level`,`source`,`position`,`set_name`,`set_effect_3`,`set_effect_6`,`attack`,`defense`,`soldier_hp`,`mobility`) VALUES (69,'地煞护甲',70,'天地宝箱','铠甲','地煞','防御+400','统御+20',0,700,0,0);
INSERT INTO `equipment_pre` (`id`,`name`,`level`,`source`,`position`,`set_name`,`set_effect_3`,`set_effect_6`,`attack`,`defense`,`soldier_hp`,`mobility`) VALUES (70,'地煞项链',70,'天地宝箱','项链','地煞','防御+400','统御+20',0,0,1500,0);
INSERT INTO `equipment_pre` (`id`,`name`,`level`,`source`,`position`,`set_name`,`set_effect_3`,`set_effect_6`,`attack`,`defense`,`soldier_hp`,`mobility`) VALUES (71,'地煞头盔',70,'天地宝箱','头盔','地煞','防御+400','统御+20',0,230,0,0);
INSERT INTO `equipment_pre` (`id`,`name`,`level`,`source`,`position`,`set_name`,`set_effect_3`,`set_effect_6`,`attack`,`defense`,`soldier_hp`,`mobility`) VALUES (72,'地煞战靴',70,'天地宝箱','鞋子','地煞','防御+400','统御+20',0,0,0,130);
INSERT INTO `equipment_pre` (`id`,`name`,`level`,`source`,`position`,`set_name`,`set_effect_3`,`set_effect_6`,`attack`,`defense`,`soldier_hp`,`mobility`) VALUES (73,'幽冥战刃',70,'天地宝箱','武器','幽冥','闪避+5%','士兵生命+300',400,0,0,0);
INSERT INTO `equipment_pre` (`id`,`name`,`level`,`source`,`position`,`set_name`,`set_effect_3`,`set_effect_6`,`attack`,`defense`,`soldier_hp`,`mobility`) VALUES (74,'幽冥戒指',70,'天地宝箱','戒指','幽冥','闪避+5%','士兵生命+300',130,0,0,0);
INSERT INTO `equipment_pre` (`id`,`name`,`level`,`source`,`position`,`set_name`,`set_effect_3`,`set_effect_6`,`attack`,`defense`,`soldier_hp`,`mobility`) VALUES (75,'幽冥战甲',70,'天地宝箱','铠甲','幽冥','闪避+5%','士兵生命+300',0,600,0,0);
INSERT INTO `equipment_pre` (`id`,`name`,`level`,`source`,`position`,`set_name`,`set_effect_3`,`set_effect_6`,`attack`,`defense`,`soldier_hp`,`mobility`) VALUES (76,'幽冥项链',70,'天地宝箱','项链','幽冥','闪避+5%','士兵生命+300',0,0,1800,0);
INSERT INTO `equipment_pre` (`id`,`name`,`level`,`source`,`position`,`set_name`,`set_effect_3`,`set_effect_6`,`attack`,`defense`,`soldier_hp`,`mobility`) VALUES (77,'幽冥头盔',70,'天地宝箱','头盔','幽冥','闪避+5%','士兵生命+300',0,200,0,0);
INSERT INTO `equipment_pre` (`id`,`name`,`level`,`source`,`position`,`set_name`,`set_effect_3`,`set_effect_6`,`attack`,`defense`,`soldier_hp`,`mobility`) VALUES (78,'幽冥战靴',70,'天地宝箱','鞋子','幽冥','闪避+5%','士兵生命+300',0,0,0,140);

-- ============================================================
-- 80级 - 雄狮套装（副本产出）
-- 掉落来源: DUNGEON_80 最终Boss
-- ============================================================
INSERT INTO `equipment_pre` (`id`,`name`,`level`,`source`,`position`,`set_name`,`set_effect_3`,`set_effect_6`,`attack`,`defense`,`soldier_hp`,`mobility`) VALUES (79,'雄狮战刃',80,'副本产出','武器','雄狮','攻击+800','防御+200',600,0,0,0);
INSERT INTO `equipment_pre` (`id`,`name`,`level`,`source`,`position`,`set_name`,`set_effect_3`,`set_effect_6`,`attack`,`defense`,`soldier_hp`,`mobility`) VALUES (80,'雄狮戒指',80,'副本产出','戒指','雄狮','攻击+800','防御+200',200,0,0,0);
INSERT INTO `equipment_pre` (`id`,`name`,`level`,`source`,`position`,`set_name`,`set_effect_3`,`set_effect_6`,`attack`,`defense`,`soldier_hp`,`mobility`) VALUES (81,'雄狮战甲',80,'副本产出','铠甲','雄狮','攻击+800','防御+200',0,800,0,0);
INSERT INTO `equipment_pre` (`id`,`name`,`level`,`source`,`position`,`set_name`,`set_effect_3`,`set_effect_6`,`attack`,`defense`,`soldier_hp`,`mobility`) VALUES (82,'雄狮项链',80,'副本产出','项链','雄狮','攻击+800','防御+200',0,0,2000,0);
INSERT INTO `equipment_pre` (`id`,`name`,`level`,`source`,`position`,`set_name`,`set_effect_3`,`set_effect_6`,`attack`,`defense`,`soldier_hp`,`mobility`) VALUES (83,'雄狮头盔',80,'副本产出','头盔','雄狮','攻击+800','防御+200',0,260,0,0);
INSERT INTO `equipment_pre` (`id`,`name`,`level`,`source`,`position`,`set_name`,`set_effect_3`,`set_effect_6`,`attack`,`defense`,`soldier_hp`,`mobility`) VALUES (84,'雄狮战靴',80,'副本产出','鞋子','雄狮','攻击+800','防御+200',0,0,0,160);

-- ============================================================
-- 80级 - 凤鸣套装（秘境产出）
-- ============================================================
INSERT INTO `equipment_pre` (`id`,`name`,`level`,`source`,`position`,`set_name`,`set_effect_3`,`set_effect_6`,`attack`,`defense`,`soldier_hp`,`mobility`) VALUES (85,'凤鸣神剑',80,'秘境产出','武器','凤鸣','防御+1200','统御+100，士兵生命+500',550,0,0,0);
INSERT INTO `equipment_pre` (`id`,`name`,`level`,`source`,`position`,`set_name`,`set_effect_3`,`set_effect_6`,`attack`,`defense`,`soldier_hp`,`mobility`) VALUES (86,'凤鸣戒指',80,'秘境产出','戒指','凤鸣','防御+1200','统御+100，士兵生命+500',180,0,0,0);
INSERT INTO `equipment_pre` (`id`,`name`,`level`,`source`,`position`,`set_name`,`set_effect_3`,`set_effect_6`,`attack`,`defense`,`soldier_hp`,`mobility`) VALUES (87,'凤鸣护甲',80,'秘境产出','铠甲','凤鸣','防御+1200','统御+100，士兵生命+500',0,1200,0,0);
INSERT INTO `equipment_pre` (`id`,`name`,`level`,`source`,`position`,`set_name`,`set_effect_3`,`set_effect_6`,`attack`,`defense`,`soldier_hp`,`mobility`) VALUES (88,'凤鸣项链',80,'秘境产出','项链','凤鸣','防御+1200','统御+100，士兵生命+500',0,0,2500,0);
INSERT INTO `equipment_pre` (`id`,`name`,`level`,`source`,`position`,`set_name`,`set_effect_3`,`set_effect_6`,`attack`,`defense`,`soldier_hp`,`mobility`) VALUES (89,'凤鸣头盔',80,'秘境产出','头盔','凤鸣','防御+1200','统御+100，士兵生命+500',0,400,0,0);
INSERT INTO `equipment_pre` (`id`,`name`,`level`,`source`,`position`,`set_name`,`set_effect_3`,`set_effect_6`,`attack`,`defense`,`soldier_hp`,`mobility`) VALUES (90,'凤鸣战靴',80,'秘境产出','鞋子','凤鸣','防御+1200','统御+100，士兵生命+500',0,0,0,180);

-- ============================================================
-- 80级 - 精金套装（手工制作，军械局制作）
-- 制作消耗: 纸张250 + 金属500 + 银币40000 + 木材400
-- ============================================================
INSERT INTO `equipment_pre` (`id`,`name`,`level`,`source`,`position`,`set_name`,`set_effect_3`,`set_effect_6`,`attack`,`defense`,`soldier_hp`,`mobility`) VALUES (91,'精金重剑',80,'手工制作','武器','精金','攻击+600','士兵生命+200',580,0,0,0);
INSERT INTO `equipment_pre` (`id`,`name`,`level`,`source`,`position`,`set_name`,`set_effect_3`,`set_effect_6`,`attack`,`defense`,`soldier_hp`,`mobility`) VALUES (92,'精金戒指',80,'手工制作','戒指','精金','攻击+600','士兵生命+200',190,0,0,0);
INSERT INTO `equipment_pre` (`id`,`name`,`level`,`source`,`position`,`set_name`,`set_effect_3`,`set_effect_6`,`attack`,`defense`,`soldier_hp`,`mobility`) VALUES (93,'精金战甲',80,'手工制作','铠甲','精金','攻击+600','士兵生命+200',0,750,0,0);
INSERT INTO `equipment_pre` (`id`,`name`,`level`,`source`,`position`,`set_name`,`set_effect_3`,`set_effect_6`,`attack`,`defense`,`soldier_hp`,`mobility`) VALUES (94,'精金项链',80,'手工制作','项链','精金','攻击+600','士兵生命+200',0,0,2200,0);
INSERT INTO `equipment_pre` (`id`,`name`,`level`,`source`,`position`,`set_name`,`set_effect_3`,`set_effect_6`,`attack`,`defense`,`soldier_hp`,`mobility`) VALUES (95,'精金头盔',80,'手工制作','头盔','精金','攻击+600','士兵生命+200',0,250,0,0);
INSERT INTO `equipment_pre` (`id`,`name`,`level`,`source`,`position`,`set_name`,`set_effect_3`,`set_effect_6`,`attack`,`defense`,`soldier_hp`,`mobility`) VALUES (96,'精金战靴',80,'手工制作','鞋子','精金','攻击+600','士兵生命+200',0,0,0,170);

-- ============================================================
-- 90级 - 圣象套装（副本产出）
-- 掉落来源: DUNGEON_100 第10个NPC
-- ============================================================
INSERT INTO `equipment_pre` (`id`,`name`,`level`,`source`,`position`,`set_name`,`set_effect_3`,`set_effect_6`,`attack`,`defense`,`soldier_hp`,`mobility`) VALUES (97,'圣象神兵',90,'副本产出','武器','圣象','攻击+1000','武勇+60',750,0,0,0);
INSERT INTO `equipment_pre` (`id`,`name`,`level`,`source`,`position`,`set_name`,`set_effect_3`,`set_effect_6`,`attack`,`defense`,`soldier_hp`,`mobility`) VALUES (98,'圣象戒指',90,'副本产出','戒指','圣象','攻击+1000','武勇+60',250,0,0,0);
INSERT INTO `equipment_pre` (`id`,`name`,`level`,`source`,`position`,`set_name`,`set_effect_3`,`set_effect_6`,`attack`,`defense`,`soldier_hp`,`mobility`) VALUES (99,'圣象战甲',90,'副本产出','铠甲','圣象','攻击+1000','武勇+60',0,1000,0,0);
INSERT INTO `equipment_pre` (`id`,`name`,`level`,`source`,`position`,`set_name`,`set_effect_3`,`set_effect_6`,`attack`,`defense`,`soldier_hp`,`mobility`) VALUES (100,'圣象项链',90,'副本产出','项链','圣象','攻击+1000','武勇+60',0,0,2800,0);
INSERT INTO `equipment_pre` (`id`,`name`,`level`,`source`,`position`,`set_name`,`set_effect_3`,`set_effect_6`,`attack`,`defense`,`soldier_hp`,`mobility`) VALUES (101,'圣象头盔',90,'副本产出','头盔','圣象','攻击+1000','武勇+60',0,330,0,0);
INSERT INTO `equipment_pre` (`id`,`name`,`level`,`source`,`position`,`set_name`,`set_effect_3`,`set_effect_6`,`attack`,`defense`,`soldier_hp`,`mobility`) VALUES (102,'圣象战靴',90,'副本产出','鞋子','圣象','攻击+1000','武勇+60',0,0,0,200);

-- ============================================================
-- 100级 - 龙吟套装（秘境产出）
-- ============================================================
INSERT INTO `equipment_pre` (`id`,`name`,`level`,`source`,`position`,`set_name`,`set_effect_3`,`set_effect_6`,`attack`,`defense`,`soldier_hp`,`mobility`) VALUES (103,'龙吟神剑',100,'秘境产出','武器','龙吟','防御+2000','统御+200，闪避+30%，士兵生命+1000',800,0,0,0);
INSERT INTO `equipment_pre` (`id`,`name`,`level`,`source`,`position`,`set_name`,`set_effect_3`,`set_effect_6`,`attack`,`defense`,`soldier_hp`,`mobility`) VALUES (104,'龙吟戒指',100,'秘境产出','戒指','龙吟','防御+2000','统御+200，闪避+30%，士兵生命+1000',260,0,0,0);
INSERT INTO `equipment_pre` (`id`,`name`,`level`,`source`,`position`,`set_name`,`set_effect_3`,`set_effect_6`,`attack`,`defense`,`soldier_hp`,`mobility`) VALUES (105,'龙吟护甲',100,'秘境产出','铠甲','龙吟','防御+2000','统御+200，闪避+30%，士兵生命+1000',0,2000,0,0);
INSERT INTO `equipment_pre` (`id`,`name`,`level`,`source`,`position`,`set_name`,`set_effect_3`,`set_effect_6`,`attack`,`defense`,`soldier_hp`,`mobility`) VALUES (106,'龙吟项链',100,'秘境产出','项链','龙吟','防御+2000','统御+200，闪避+30%，士兵生命+1000',0,0,4000,0);
INSERT INTO `equipment_pre` (`id`,`name`,`level`,`source`,`position`,`set_name`,`set_effect_3`,`set_effect_6`,`attack`,`defense`,`soldier_hp`,`mobility`) VALUES (107,'龙吟头盔',100,'秘境产出','头盔','龙吟','防御+2000','统御+200，闪避+30%，士兵生命+1000',0,670,0,0);
INSERT INTO `equipment_pre` (`id`,`name`,`level`,`source`,`position`,`set_name`,`set_effect_3`,`set_effect_6`,`attack`,`defense`,`soldier_hp`,`mobility`) VALUES (108,'龙吟战靴',100,'秘境产出','鞋子','龙吟','防御+2000','统御+200，闪避+30%，士兵生命+1000',0,0,0,250);

-- ============================================================
-- 100级 - 玄武套装（副本产出）
-- 掉落来源: DUNGEON_100 最终Boss
-- ============================================================
INSERT INTO `equipment_pre` (`id`,`name`,`level`,`source`,`position`,`set_name`,`set_effect_3`,`set_effect_6`,`attack`,`defense`,`soldier_hp`,`mobility`) VALUES (109,'玄武战刃',100,'副本产出','武器','玄武','攻击+1200','防御+400',900,0,0,0);
INSERT INTO `equipment_pre` (`id`,`name`,`level`,`source`,`position`,`set_name`,`set_effect_3`,`set_effect_6`,`attack`,`defense`,`soldier_hp`,`mobility`) VALUES (110,'玄武戒指',100,'副本产出','戒指','玄武','攻击+1200','防御+400',300,0,0,0);
INSERT INTO `equipment_pre` (`id`,`name`,`level`,`source`,`position`,`set_name`,`set_effect_3`,`set_effect_6`,`attack`,`defense`,`soldier_hp`,`mobility`) VALUES (111,'玄武战甲',100,'副本产出','铠甲','玄武','攻击+1200','防御+400',0,1200,0,0);
INSERT INTO `equipment_pre` (`id`,`name`,`level`,`source`,`position`,`set_name`,`set_effect_3`,`set_effect_6`,`attack`,`defense`,`soldier_hp`,`mobility`) VALUES (112,'玄武项链',100,'副本产出','项链','玄武','攻击+1200','防御+400',0,0,3200,0);
INSERT INTO `equipment_pre` (`id`,`name`,`level`,`source`,`position`,`set_name`,`set_effect_3`,`set_effect_6`,`attack`,`defense`,`soldier_hp`,`mobility`) VALUES (113,'玄武头盔',100,'副本产出','头盔','玄武','攻击+1200','防御+400',0,400,0,0);
INSERT INTO `equipment_pre` (`id`,`name`,`level`,`source`,`position`,`set_name`,`set_effect_3`,`set_effect_6`,`attack`,`defense`,`soldier_hp`,`mobility`) VALUES (114,'玄武战靴',100,'副本产出','鞋子','玄武','攻击+1200','防御+400',0,0,0,220);

-- ============================================================
-- 100级 - 秘银套装（手工制作，军械局制作）
-- 制作消耗: 纸张500 + 金属1000 + 银币100000 + 木材800
-- ============================================================
INSERT INTO `equipment_pre` (`id`,`name`,`level`,`source`,`position`,`set_name`,`set_effect_3`,`set_effect_6`,`attack`,`defense`,`soldier_hp`,`mobility`) VALUES (115,'秘银神剑',100,'手工制作','武器','秘银','攻击+1000','士兵生命+500',850,0,0,0);
INSERT INTO `equipment_pre` (`id`,`name`,`level`,`source`,`position`,`set_name`,`set_effect_3`,`set_effect_6`,`attack`,`defense`,`soldier_hp`,`mobility`) VALUES (116,'秘银戒指',100,'手工制作','戒指','秘银','攻击+1000','士兵生命+500',280,0,0,0);
INSERT INTO `equipment_pre` (`id`,`name`,`level`,`source`,`position`,`set_name`,`set_effect_3`,`set_effect_6`,`attack`,`defense`,`soldier_hp`,`mobility`) VALUES (117,'秘银战甲',100,'手工制作','铠甲','秘银','攻击+1000','士兵生命+500',0,1100,0,0);
INSERT INTO `equipment_pre` (`id`,`name`,`level`,`source`,`position`,`set_name`,`set_effect_3`,`set_effect_6`,`attack`,`defense`,`soldier_hp`,`mobility`) VALUES (118,'秘银项链',100,'手工制作','项链','秘银','攻击+1000','士兵生命+500',0,0,3500,0);
INSERT INTO `equipment_pre` (`id`,`name`,`level`,`source`,`position`,`set_name`,`set_effect_3`,`set_effect_6`,`attack`,`defense`,`soldier_hp`,`mobility`) VALUES (119,'秘银头盔',100,'手工制作','头盔','秘银','攻击+1000','士兵生命+500',0,370,0,0);
INSERT INTO `equipment_pre` (`id`,`name`,`level`,`source`,`position`,`set_name`,`set_effect_3`,`set_effect_6`,`attack`,`defense`,`soldier_hp`,`mobility`) VALUES (120,'秘银战靴',100,'手工制作','鞋子','秘银','攻击+1000','士兵生命+500',0,0,0,230);
