-- =============================================
-- 兵种配置表 (从 APK ArmyService.json 提取)
-- 步兵 armyType=1, 骑兵 armyType=2, 弓兵 armyType=3
-- =============================================

CREATE TABLE IF NOT EXISTS `army_config` (
  `cfg_id` INT NOT NULL COMMENT '兵种配置ID (如10101)',
  `name` VARCHAR(32) NOT NULL COMMENT '兵种名称',
  `level` INT NOT NULL COMMENT '兵种等级 1-10',
  `army_type` INT NOT NULL COMMENT '兵种类型: 1步兵 2骑兵 3弓兵',
  `use_up` DOUBLE DEFAULT 0 COMMENT '消耗系数',
  `king_level_need` INT DEFAULT 1 COMMENT '解锁需要的君主等级',
  `silver_need` INT DEFAULT 0 COMMENT '升级需要的白银',
  `life` INT DEFAULT 0 COMMENT '士兵生命',
  `att` INT DEFAULT 0 COMMENT '士兵攻击',
  `def` INT DEFAULT 0 COMMENT '士兵防御',
  `sp` INT DEFAULT 0 COMMENT '士兵速度',
  `hit` INT DEFAULT 0 COMMENT '命中',
  `mis` INT DEFAULT 0 COMMENT '闪避',
  `is_npc` TINYINT(1) DEFAULT 0 COMMENT '是否NPC兵种',
  PRIMARY KEY (`cfg_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='兵种配置表(APK提取)';

DELETE FROM `army_config`;

INSERT INTO `army_config` VALUES (10101, '乡勇', 1, 1, 0.01, 1, 500, 240, 110, 120, 15, 0, 0, 0);
INSERT INTO `army_config` VALUES (10102, '民兵', 2, 1, 0.015, 2, 1000, 260, 210, 250, 17, 0, 0, 0);
INSERT INTO `army_config` VALUES (10103, '轻步兵', 3, 1, 0.02, 3, 3000, 280, 290, 400, 19, 0, 0, 0);
INSERT INTO `army_config` VALUES (10104, '朴刀兵', 4, 1, 0.025, 4, 8000, 300, 320, 600, 21, 0, 0, 0);
INSERT INTO `army_config` VALUES (10105, '刀盾兵', 5, 1, 0.03, 5, 15000, 320, 400, 750, 23, 0, 0, 0);
INSERT INTO `army_config` VALUES (10106, '藤甲兵', 6, 1, 0.035, 6, 30000, 340, 480, 900, 25, 0, 0, 0);
INSERT INTO `army_config` VALUES (10107, '重步兵', 7, 1, 0.04, 7, 100000, 360, 560, 1050, 26, 0, 0, 0);
INSERT INTO `army_config` VALUES (10108, '大戟士', 8, 1, 0.045, 8, 200000, 380, 640, 1200, 27, 0, 0, 0);
INSERT INTO `army_config` VALUES (10109, '陌刀兵', 9, 1, 0.05, 9, 300000, 400, 720, 1350, 28, 0, 0, 0);
INSERT INTO `army_config` VALUES (10110, '虎贲禁卫军', 10, 1, 0.06, 10, 500000, 420, 800, 1500, 29, 0, 0, 0);
INSERT INTO `army_config` VALUES (10201, '预备骑兵', 1, 2, 0.01, 1, 500, 230, 150, 110, 20, 0, 0, 0);
INSERT INTO `army_config` VALUES (10202, '轻骑兵', 2, 2, 0.015, 2, 1000, 240, 300, 210, 22, 0, 0, 0);
INSERT INTO `army_config` VALUES (10203, '游骑兵', 3, 2, 0.02, 3, 3000, 250, 450, 290, 24, 0, 0, 0);
INSERT INTO `army_config` VALUES (10204, '突骑兵', 4, 2, 0.025, 4, 8000, 260, 600, 320, 26, 0, 0, 0);
INSERT INTO `army_config` VALUES (10205, '枪骑兵', 5, 2, 0.03, 5, 15000, 270, 750, 400, 28, 0, 0, 0);
INSERT INTO `army_config` VALUES (10206, '骁骑兵', 6, 2, 0.035, 6, 30000, 280, 900, 480, 30, 0, 0, 0);
INSERT INTO `army_config` VALUES (10207, '重骑兵', 7, 2, 0.04, 7, 100000, 290, 1050, 560, 31, 0, 0, 0);
INSERT INTO `army_config` VALUES (10208, '铁骑兵', 8, 2, 0.045, 8, 200000, 300, 1200, 640, 32, 0, 0, 0);
INSERT INTO `army_config` VALUES (10209, '玄甲精骑', 9, 2, 0.05, 9, 300000, 310, 1350, 720, 33, 0, 0, 0);
INSERT INTO `army_config` VALUES (10210, '羽林禁卫军', 10, 2, 0.06, 10, 500000, 320, 1500, 800, 34, 0, 0, 0);
INSERT INTO `army_config` VALUES (10301, '猎人', 1, 3, 0.01, 1, 500, 230, 200, 100, 10, 0, 0, 0);
INSERT INTO `army_config` VALUES (10302, '短弓手', 2, 3, 0.015, 2, 1000, 240, 400, 150, 12, 0, 0, 0);
INSERT INTO `army_config` VALUES (10303, '长弓兵', 3, 3, 0.02, 3, 3000, 250, 650, 200, 14, 0, 0, 0);
INSERT INTO `army_config` VALUES (10304, '铁胎弓', 4, 3, 0.025, 4, 8000, 260, 900, 250, 16, 0, 0, 0);
INSERT INTO `army_config` VALUES (10305, '强弩兵', 5, 3, 0.03, 5, 15000, 270, 1200, 300, 18, 0, 0, 0);
INSERT INTO `army_config` VALUES (10306, '连弩兵', 6, 3, 0.035, 6, 30000, 280, 1500, 350, 20, 0, 0, 0);
INSERT INTO `army_config` VALUES (10307, '诸葛弩', 7, 3, 0.04, 7, 100000, 290, 1800, 400, 21, 0, 0, 0);
INSERT INTO `army_config` VALUES (10308, '神臂弩', 8, 3, 0.045, 8, 200000, 300, 2000, 450, 22, 0, 0, 0);
INSERT INTO `army_config` VALUES (10309, '床子弩', 9, 3, 0.05, 9, 300000, 310, 2300, 500, 23, 0, 0, 0);
INSERT INTO `army_config` VALUES (10310, '神机营', 10, 3, 0.06, 10, 500000, 320, 2600, 550, 24, 0, 0, 0);
INSERT INTO `army_config` VALUES (20101, '1级步兵', 1, 1, 0.01, 21, 500, 240, 110, 120, 15, 0, 0, 1);
INSERT INTO `army_config` VALUES (20102, '2级步兵', 2, 1, 0.015, 22, 1000, 260, 210, 250, 17, 0, 0, 1);
INSERT INTO `army_config` VALUES (20103, '3级步兵', 3, 1, 0.02, 23, 3000, 280, 290, 400, 19, 0, 0, 1);
INSERT INTO `army_config` VALUES (20104, '4级步兵', 4, 1, 0.025, 24, 8000, 310, 320, 600, 21, 0, 3, 1);
INSERT INTO `army_config` VALUES (20105, '5级步兵', 5, 1, 0.03, 25, 15000, 340, 400, 750, 23, 0, 5, 1);
INSERT INTO `army_config` VALUES (20106, '6级步兵', 6, 1, 0.035, 26, 30000, 370, 480, 900, 30, 0, 6, 1);
INSERT INTO `army_config` VALUES (20107, '7级步兵', 7, 1, 0.04, 27, 100000, 400, 560, 1050, 37, 0, 7, 1);
INSERT INTO `army_config` VALUES (20108, '8级步兵', 8, 1, 0.045, 28, 200000, 440, 640, 1200, 48, 0, 8, 1);
INSERT INTO `army_config` VALUES (20109, '9级步兵', 9, 1, 0.05, 29, 300000, 460, 720, 1350, 49, 0, 9, 1);
INSERT INTO `army_config` VALUES (20110, '10级步兵', 10, 1, 0.06, 30, 500000, 480, 800, 1500, 50, 0, 10, 1);
INSERT INTO `army_config` VALUES (20201, '1级骑兵', 1, 2, 0.01, 21, 500, 230, 150, 110, 10, 0, 0, 1);
INSERT INTO `army_config` VALUES (20202, '2级骑兵', 2, 2, 0.015, 22, 1000, 240, 300, 210, 12, 0, 0, 1);
INSERT INTO `army_config` VALUES (20203, '3级骑兵', 3, 2, 0.02, 23, 3000, 250, 450, 290, 14, 0, 0, 1);
INSERT INTO `army_config` VALUES (20204, '4级骑兵', 4, 2, 0.025, 24, 8000, 260, 650, 320, 16, 0, 0, 1);
INSERT INTO `army_config` VALUES (20205, '5级骑兵', 5, 2, 0.03, 25, 15000, 270, 900, 400, 20, 0, 0, 1);
INSERT INTO `army_config` VALUES (20206, '6级骑兵', 6, 2, 0.035, 26, 30000, 280, 1220, 480, 28, 0, 0, 1);
INSERT INTO `army_config` VALUES (20207, '7级骑兵', 7, 2, 0.04, 27, 100000, 290, 1400, 560, 42, 0, 0, 1);
INSERT INTO `army_config` VALUES (20208, '8级骑兵', 8, 2, 0.045, 28, 200000, 300, 1600, 640, 53, 0, 0, 1);
INSERT INTO `army_config` VALUES (20209, '9级骑兵', 9, 2, 0.05, 29, 300000, 310, 1800, 720, 54, 0, 0, 1);
INSERT INTO `army_config` VALUES (20210, '10级骑兵', 10, 2, 0.06, 30, 500000, 320, 2000, 800, 55, 0, 0, 1);
INSERT INTO `army_config` VALUES (20301, '1级弓兵', 1, 3, 0.01, 21, 500, 230, 200, 100, 10, 0, 0, 1);
INSERT INTO `army_config` VALUES (20302, '2级弓兵', 2, 3, 0.015, 22, 1000, 240, 400, 150, 12, 0, 0, 1);
INSERT INTO `army_config` VALUES (20303, '3级弓兵', 3, 3, 0.02, 23, 3000, 250, 650, 200, 14, 0, 0, 1);
INSERT INTO `army_config` VALUES (20304, '4级弓兵', 4, 3, 0.025, 24, 8000, 260, 900, 250, 16, 0, 0, 1);
INSERT INTO `army_config` VALUES (20305, '5级弓兵', 5, 3, 0.03, 25, 15000, 270, 1370, 300, 18, 0, 0, 1);
INSERT INTO `army_config` VALUES (20306, '6级弓兵', 6, 3, 0.035, 26, 30000, 280, 1820, 350, 25, 0, 0, 1);
INSERT INTO `army_config` VALUES (20307, '7级弓兵', 7, 3, 0.04, 27, 100000, 290, 2120, 400, 32, 0, 0, 1);
INSERT INTO `army_config` VALUES (20308, '8级弓兵', 8, 3, 0.045, 28, 200000, 300, 2400, 450, 43, 0, 0, 1);
INSERT INTO `army_config` VALUES (20309, '9级弓兵', 9, 3, 0.05, 29, 300000, 310, 2700, 500, 44, 0, 0, 1);
INSERT INTO `army_config` VALUES (20310, '10级弓兵', 10, 3, 0.06, 30, 500000, 320, 3100, 550, 45, 0, 0, 1);
INSERT INTO `army_config` VALUES (30102, '黄巾流寇步兵', 2, 1, 0.02, 28, 200000, 340, 160, 250, 12, 0, 0, 1);
INSERT INTO `army_config` VALUES (30103, '董卓军团步兵', 3, 1, 0.02, 28, 200000, 380, 220, 400, 12, 0, 0, 1);
INSERT INTO `army_config` VALUES (30104, '黄巾卫队步兵', 4, 1, 0.02, 28, 200000, 430, 240, 600, 12, 0, 0, 1);
INSERT INTO `army_config` VALUES (30106, '董卓卫队步兵', 6, 1, 0.02, 28, 200000, 540, 360, 900, 12, 0, 0, 1);
INSERT INTO `army_config` VALUES (30202, '黄巾流寇骑兵', 2, 2, 0.02, 28, 200000, 315, 200, 210, 18, 0, 0, 1);
INSERT INTO `army_config` VALUES (30203, '董卓军团骑兵', 3, 2, 0.02, 28, 200000, 330, 300, 290, 20, 0, 0, 1);
INSERT INTO `army_config` VALUES (30204, '黄巾卫队骑兵', 4, 2, 0.02, 28, 200000, 360, 400, 320, 24, 0, 0, 1);
INSERT INTO `army_config` VALUES (30206, '董卓卫队骑兵', 6, 2, 0.02, 28, 200000, 430, 600, 480, 27, 0, 0, 1);
INSERT INTO `army_config` VALUES (30302, '黄巾流寇弓兵', 2, 3, 0.02, 28, 200000, 315, 280, 150, 10, 0, 0, 1);
INSERT INTO `army_config` VALUES (30303, '董卓军团弓兵', 3, 3, 0.02, 28, 200000, 330, 400, 200, 10, 0, 0, 1);
INSERT INTO `army_config` VALUES (30304, '黄巾卫队弓兵', 4, 3, 0.02, 28, 200000, 350, 550, 250, 10, 0, 0, 1);
INSERT INTO `army_config` VALUES (30306, '董卓卫队弓兵', 6, 3, 0.02, 28, 200000, 420, 860, 350, 10, 0, 0, 1);
INSERT INTO `army_config` VALUES (31104, '异族军团步兵', 4, 1, 0.02, 28, 200000, 530, 360, 660, 12, 0, 0, 1);
INSERT INTO `army_config` VALUES (31108, '首领卫队步兵', 8, 1, 0.02, 28, 200000, 840, 720, 1500, 12, 0, 0, 1);
INSERT INTO `army_config` VALUES (31204, '异族军团骑兵', 4, 2, 0.02, 28, 200000, 430, 660, 420, 20, 0, 0, 1);
INSERT INTO `army_config` VALUES (31208, '首领卫队骑兵', 8, 2, 0.02, 28, 200000, 730, 1600, 800, 27, 0, 0, 1);
INSERT INTO `army_config` VALUES (31304, '异族军团弓兵', 4, 3, 0.02, 28, 200000, 430, 900, 360, 10, 0, 0, 1);
INSERT INTO `army_config` VALUES (31308, '首领卫队弓兵', 8, 3, 0.02, 28, 200000, 730, 2400, 700, 10, 0, 0, 1);