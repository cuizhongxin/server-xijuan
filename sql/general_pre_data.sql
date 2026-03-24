-- =============================================
-- 武将预设模板表 (基于APK FamousGenShow_cfg.json)
-- =============================================
-- APK ID 编码规则: XYFZZ
--   X: 1=普通, 2=特殊, 3=狂化
--   Y: 0=群, 1=魏, 2=蜀, 3=吴
--   F: 品质 (2=绿, 3=蓝, 4=紫, 5=橙)
--   ZZ: 序号
--
-- slot_id 对照 (来自 general_slot 表):
--   橙色: 1=步统帅, 2=步猛将, 3=骑猛将, 4=骑统帅, 5=弓智将, 6=弓统帅
--   紫色: 7=步统帅, 8=步猛将, 9=骑统帅, 10=骑猛将, 11=弓智将, 12=弓统帅
--   红色: 13=步统帅, 14=步猛将, 15=骑统帅, 16=骑猛将, 17=弓智将, 18=弓统帅
--   蓝色: 19=步统帅, 20=步猛将, 21=骑统帅, 22=骑猛将, 23=弓智将, 24=弓统帅
--   绿色: 25=步统帅, 26=步猛将, 27=骑统帅, 28=骑猛将, 29=弓智将, 30=弓统帅
-- =============================================

CREATE TABLE IF NOT EXISTS `general_pre` (
  `id` INT NOT NULL COMMENT 'APK武将ID',
  `name` VARCHAR(20) NOT NULL COMMENT '武将名称',
  `faction` VARCHAR(4) NOT NULL COMMENT '阵营: 群/魏/蜀/吴',
  `quality` TINYINT NOT NULL COMMENT '品质: 2=绿, 3=蓝, 4=紫, 5=橙',
  `quality_code` VARCHAR(16) NOT NULL COMMENT '品质代码: green/blue/purple/orange',
  `troop_type` VARCHAR(4) NOT NULL COMMENT '兵种: 步/骑/弓',
  `general_type` VARCHAR(8) NOT NULL COMMENT '类型: 统帅/猛将/智将',
  `slot_id` INT NOT NULL COMMENT '关联general_slot表ID(决定基础属性)',
  `pic_id` INT NOT NULL COMMENT 'APK图片ID(generalShow_cfg)',
  `recruit_pool` VARCHAR(16) NOT NULL DEFAULT 'normal' COMMENT '招募池: normal/advanced/special',
  `is_berserk` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否狂化版',
  `base_id` INT DEFAULT NULL COMMENT '狂化版对应原始武将ID',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='武将预设模板表(APK数据)';

DELETE FROM `general_pre`;

-- =============================================
-- 群 · 绿色 (quality=2, 12将)
-- =============================================
INSERT INTO `general_pre` VALUES (10201, '张燕',   '群', 2, 'green', '步', '统帅', 25, 4021, 'normal', 0, NULL);
INSERT INTO `general_pre` VALUES (10202, '宋宪',   '群', 2, 'green', '步', '猛将', 26, 4022, 'normal', 0, NULL);
INSERT INTO `general_pre` VALUES (10203, '李催',   '群', 2, 'green', '骑', '猛将', 28, 4023, 'normal', 0, NULL);
INSERT INTO `general_pre` VALUES (10204, '郭汜',   '群', 2, 'green', '骑', '统帅', 27, 4024, 'normal', 0, NULL);
INSERT INTO `general_pre` VALUES (10205, '蔡瑁',   '群', 2, 'green', '弓', '智将', 29, 4025, 'normal', 0, NULL);
INSERT INTO `general_pre` VALUES (10206, '邓贤',   '群', 2, 'green', '步', '猛将', 26, 4026, 'normal', 0, NULL);
INSERT INTO `general_pre` VALUES (10207, '杨怀',   '群', 2, 'green', '步', '统帅', 25, 4027, 'normal', 0, NULL);
INSERT INTO `general_pre` VALUES (10208, '泠苞',   '群', 2, 'green', '弓', '统帅', 30, 4028, 'normal', 0, NULL);
INSERT INTO `general_pre` VALUES (10209, '张英',   '群', 2, 'green', '弓', '智将', 29, 4029, 'normal', 0, NULL);
INSERT INTO `general_pre` VALUES (10210, '雷铜',   '群', 2, 'green', '骑', '统帅', 27, 4060, 'normal', 0, NULL);
INSERT INTO `general_pre` VALUES (10211, '高沛',   '群', 2, 'green', '骑', '猛将', 28, 4061, 'normal', 0, NULL);
INSERT INTO `general_pre` VALUES (10212, '樊稠',   '群', 2, 'green', '弓', '统帅', 30, 4062, 'normal', 0, NULL);

-- =============================================
-- 群 · 蓝色 (quality=3, 8将)
-- =============================================
INSERT INTO `general_pre` VALUES (10301, '潘凤',   '群', 3, 'blue', '步', '猛将', 20, 4031, 'normal', 0, NULL);
INSERT INTO `general_pre` VALUES (10302, '韩性',   '群', 3, 'blue', '弓', '统帅', 24, 4033, 'normal', 0, NULL);
INSERT INTO `general_pre` VALUES (10303, '曹猛',   '群', 3, 'blue', '骑', '猛将', 22, 4034, 'normal', 0, NULL);
INSERT INTO `general_pre` VALUES (10304, '韩遂',   '群', 3, 'blue', '骑', '统帅', 21, 4032, 'normal', 0, NULL);
INSERT INTO `general_pre` VALUES (10305, '徐荣',   '群', 3, 'blue', '步', '统帅', 19, 4035, 'normal', 0, NULL);
INSERT INTO `general_pre` VALUES (10306, '孟获',   '群', 3, 'blue', '步', '猛将', 20, 4036, 'normal', 0, NULL);
INSERT INTO `general_pre` VALUES (10307, '张角',   '群', 3, 'blue', '弓', '智将', 23, 4037, 'normal', 0, NULL);
INSERT INTO `general_pre` VALUES (10308, '沙摩柯', '群', 3, 'blue', '骑', '猛将', 22, 4038, 'normal', 0, NULL);

-- =============================================
-- 群 · 紫色 (quality=4, 7将)
-- =============================================
INSERT INTO `general_pre` VALUES (10401, '华雄',   '群', 4, 'purple', '步', '猛将',  8, 4041, 'advanced', 0, NULL);
INSERT INTO `general_pre` VALUES (10402, '颜良',   '群', 4, 'purple', '骑', '猛将', 10, 4042, 'advanced', 0, NULL);
INSERT INTO `general_pre` VALUES (10403, '马腾',   '群', 4, 'purple', '骑', '统帅',  9, 4043, 'advanced', 0, NULL);
INSERT INTO `general_pre` VALUES (10404, '鞠义',   '群', 4, 'purple', '弓', '智将', 11, 4044, 'advanced', 0, NULL);
INSERT INTO `general_pre` VALUES (10405, '高顺',   '群', 4, 'purple', '步', '统帅',  7, 4045, 'advanced', 0, NULL);
INSERT INTO `general_pre` VALUES (10406, '文丑',   '群', 4, 'purple', '骑', '猛将', 10, 4046, 'advanced', 0, NULL);
INSERT INTO `general_pre` VALUES (10407, '公孙瓒', '群', 4, 'purple', '弓', '统帅', 12, 4047, 'advanced', 0, NULL);

-- =============================================
-- 群 · 橙色 (quality=5, 1将)
-- =============================================
INSERT INTO `general_pre` VALUES (10501, '吕布',   '群', 5, 'orange', '骑', '猛将',  3, 4051, 'advanced', 0, NULL);

-- =============================================
-- 魏 · 蓝色 (quality=3, 4将)
-- =============================================
INSERT INTO `general_pre` VALUES (11301, '文聘',   '魏', 3, 'blue', '步', '统帅', 19, 4131, 'normal', 0, NULL);
INSERT INTO `general_pre` VALUES (11302, '郝昭',   '魏', 3, 'blue', '弓', '统帅', 24, 4132, 'normal', 0, NULL);
INSERT INTO `general_pre` VALUES (11303, '邓艾',   '魏', 3, 'blue', '骑', '统帅', 21, 4133, 'normal', 0, NULL);
INSERT INTO `general_pre` VALUES (11304, '满宠',   '魏', 3, 'blue', '弓', '智将', 23, 4134, 'normal', 0, NULL);

-- =============================================
-- 魏 · 紫色 (quality=4, 7将)
-- =============================================
INSERT INTO `general_pre` VALUES (11401, '曹仁',   '魏', 4, 'purple', '步', '统帅',  7, 4141, 'advanced', 0, NULL);
INSERT INTO `general_pre` VALUES (11402, '曹洪',   '魏', 4, 'purple', '步', '猛将',  8, 4142, 'advanced', 0, NULL);
INSERT INTO `general_pre` VALUES (11403, '庞德',   '魏', 4, 'purple', '骑', '猛将', 10, 4143, 'advanced', 0, NULL);
INSERT INTO `general_pre` VALUES (11404, '曹真',   '魏', 4, 'purple', '骑', '统帅',  9, 4144, 'advanced', 0, NULL);
INSERT INTO `general_pre` VALUES (11405, '夏侯渊', '魏', 4, 'purple', '骑', '猛将', 10, 4145, 'advanced', 0, NULL);
INSERT INTO `general_pre` VALUES (11406, '夏侯敦', '魏', 4, 'purple', '步', '猛将',  8, 4146, 'advanced', 0, NULL);
INSERT INTO `general_pre` VALUES (11407, '李典',   '魏', 4, 'purple', '弓', '智将', 11, 4147, 'advanced', 0, NULL);

-- =============================================
-- 魏 · 橙色 (quality=5, 7将)
-- =============================================
INSERT INTO `general_pre` VALUES (11501, '张辽',   '魏', 5, 'orange', '骑', '统帅',  4, 4151, 'advanced', 0, NULL);
INSERT INTO `general_pre` VALUES (11502, '许诸',   '魏', 5, 'orange', '步', '猛将',  2, 4152, 'advanced', 0, NULL);
INSERT INTO `general_pre` VALUES (11503, '乐进',   '魏', 5, 'orange', '步', '统帅',  1, 4153, 'advanced', 0, NULL);
INSERT INTO `general_pre` VALUES (11504, '徐晃',   '魏', 5, 'orange', '步', '猛将',  2, 4154, 'advanced', 0, NULL);
INSERT INTO `general_pre` VALUES (11505, '于禁',   '魏', 5, 'orange', '弓', '统帅',  6, 4156, 'advanced', 0, NULL);
INSERT INTO `general_pre` VALUES (11506, '典韦',   '魏', 5, 'orange', '步', '猛将',  2, 4157, 'advanced', 0, NULL);
INSERT INTO `general_pre` VALUES (11507, '张颌',   '魏', 5, 'orange', '骑', '猛将',  3, 4155, 'advanced', 0, NULL);

-- =============================================
-- 蜀 · 蓝色 (quality=3, 4将)
-- =============================================
INSERT INTO `general_pre` VALUES (12301, '马岱',   '蜀', 3, 'blue', '骑', '猛将', 22, 4231, 'normal', 0, NULL);
INSERT INTO `general_pre` VALUES (12302, '高翔',   '蜀', 3, 'blue', '弓', '统帅', 24, 4232, 'normal', 0, NULL);
INSERT INTO `general_pre` VALUES (12303, '刘封',   '蜀', 3, 'blue', '步', '猛将', 20, 4233, 'normal', 0, NULL);
INSERT INTO `general_pre` VALUES (12304, '周仓',   '蜀', 3, 'blue', '步', '猛将', 20, 4234, 'normal', 0, NULL);

-- =============================================
-- 蜀 · 紫色 (quality=4, 7将)
-- =============================================
INSERT INTO `general_pre` VALUES (12401, '关平',   '蜀', 4, 'purple', '步', '猛将',  8, 4241, 'advanced', 0, NULL);
INSERT INTO `general_pre` VALUES (12402, '廖化',   '蜀', 4, 'purple', '骑', '统帅',  9, 4242, 'advanced', 0, NULL);
INSERT INTO `general_pre` VALUES (12403, '张翼',   '蜀', 4, 'purple', '弓', '统帅', 12, 4243, 'advanced', 0, NULL);
INSERT INTO `general_pre` VALUES (12404, '关索',   '蜀', 4, 'purple', '骑', '猛将', 10, 4244, 'advanced', 0, NULL);
INSERT INTO `general_pre` VALUES (12405, '关兴',   '蜀', 4, 'purple', '步', '猛将',  8, 4245, 'advanced', 0, NULL);
INSERT INTO `general_pre` VALUES (12406, '张苞',   '蜀', 4, 'purple', '骑', '猛将', 10, 4246, 'advanced', 0, NULL);
INSERT INTO `general_pre` VALUES (12407, '向宠',   '蜀', 4, 'purple', '步', '统帅',  7, 4247, 'advanced', 0, NULL);

-- =============================================
-- 蜀 · 橙色 (quality=5, 7将)
-- =============================================
INSERT INTO `general_pre` VALUES (12501, '关羽',   '蜀', 5, 'orange', '骑', '统帅',  4, 4251, 'advanced', 0, NULL);
INSERT INTO `general_pre` VALUES (12502, '赵云',   '蜀', 5, 'orange', '骑', '猛将',  3, 4252, 'advanced', 0, NULL);
INSERT INTO `general_pre` VALUES (12503, '马超',   '蜀', 5, 'orange', '骑', '猛将',  3, 4253, 'advanced', 0, NULL);
INSERT INTO `general_pre` VALUES (12504, '黄忠',   '蜀', 5, 'orange', '弓', '智将',  5, 4254, 'advanced', 0, NULL);
INSERT INTO `general_pre` VALUES (12505, '张飞',   '蜀', 5, 'orange', '步', '猛将',  2, 4256, 'advanced', 0, NULL);
INSERT INTO `general_pre` VALUES (12506, '魏延',   '蜀', 5, 'orange', '步', '统帅',  1, 4257, 'advanced', 0, NULL);
INSERT INTO `general_pre` VALUES (12507, '姜维',   '蜀', 5, 'orange', '弓', '统帅',  6, 4255, 'advanced', 0, NULL);

-- =============================================
-- 吴 · 蓝色 (quality=3, 4将)
-- =============================================
INSERT INTO `general_pre` VALUES (13301, '祖茂',   '吴', 3, 'blue', '步', '猛将', 20, 4331, 'normal', 0, NULL);
INSERT INTO `general_pre` VALUES (13302, '蒋钦',   '吴', 3, 'blue', '弓', '智将', 23, 4332, 'normal', 0, NULL);
INSERT INTO `general_pre` VALUES (13303, '全祎',   '吴', 3, 'blue', '步', '统帅', 19, 4333, 'normal', 0, NULL);
INSERT INTO `general_pre` VALUES (13304, '朱然',   '吴', 3, 'blue', '弓', '统帅', 24, 4334, 'normal', 0, NULL);

-- =============================================
-- 吴 · 紫色 (quality=4, 7将)
-- =============================================
INSERT INTO `general_pre` VALUES (13401, '丁奉',   '吴', 4, 'purple', '步', '猛将',  8, 4341, 'advanced', 0, NULL);
INSERT INTO `general_pre` VALUES (13402, '韩当',   '吴', 4, 'purple', '骑', '猛将', 10, 4342, 'advanced', 0, NULL);
INSERT INTO `general_pre` VALUES (13403, '朱桓',   '吴', 4, 'purple', '步', '统帅',  7, 4343, 'advanced', 0, NULL);
INSERT INTO `general_pre` VALUES (13404, '凌操',   '吴', 4, 'purple', '骑', '统帅',  9, 4344, 'advanced', 0, NULL);
INSERT INTO `general_pre` VALUES (13405, '程普',   '吴', 4, 'purple', '步', '统帅',  7, 4345, 'advanced', 0, NULL);
INSERT INTO `general_pre` VALUES (13406, '黄盖',   '吴', 4, 'purple', '弓', '智将', 11, 4346, 'advanced', 0, NULL);
INSERT INTO `general_pre` VALUES (13407, '陆抗',   '吴', 4, 'purple', '弓', '统帅', 12, 4347, 'advanced', 0, NULL);

-- =============================================
-- 吴 · 橙色 (quality=5, 7将)
-- =============================================
INSERT INTO `general_pre` VALUES (13501, '凌统',   '吴', 5, 'orange', '步', '猛将',  2, 4351, 'advanced', 0, NULL);
INSERT INTO `general_pre` VALUES (13502, '太史慈', '吴', 5, 'orange', '弓', '智将',  5, 4352, 'advanced', 0, NULL);
INSERT INTO `general_pre` VALUES (13503, '吕蒙',   '吴', 5, 'orange', '步', '统帅',  1, 4353, 'advanced', 0, NULL);
INSERT INTO `general_pre` VALUES (13504, '陆逊',   '吴', 5, 'orange', '弓', '智将',  5, 4354, 'advanced', 0, NULL);
INSERT INTO `general_pre` VALUES (13505, '徐盛',   '吴', 5, 'orange', '弓', '统帅',  6, 4356, 'advanced', 0, NULL);
INSERT INTO `general_pre` VALUES (13506, '周泰',   '吴', 5, 'orange', '步', '猛将',  2, 4357, 'advanced', 0, NULL);
INSERT INTO `general_pre` VALUES (13507, '甘宁',   '吴', 5, 'orange', '骑', '猛将',  3, 4355, 'advanced', 0, NULL);

-- =============================================
-- 特殊 · 橙色 (貂蝉)
-- =============================================
INSERT INTO `general_pre` VALUES (20501, '貂蝉',   '群', 5, 'orange', '弓', '智将',  5, 4052, 'special', 0, NULL);

-- =============================================
-- 狂化版武将 (进阶后的强化版本)
-- 狂化版保持原始武将的兵种和类型, 使用相同 slot_id
-- base_id 指向原始武将, 用于进阶关联
-- =============================================
INSERT INTO `general_pre` VALUES (30501, '吕布(狂)',     '群', 5, 'orange', '骑', '猛将',  3, 4051, 'special', 1, 10501);
INSERT INTO `general_pre` VALUES (31501, '张辽(狂)',     '魏', 5, 'orange', '骑', '统帅',  4, 4151, 'special', 1, 11501);
INSERT INTO `general_pre` VALUES (32501, '关羽(狂)',     '蜀', 5, 'orange', '骑', '统帅',  4, 4251, 'special', 1, 12501);
INSERT INTO `general_pre` VALUES (33501, '凌统(狂)',     '吴', 5, 'orange', '步', '猛将',  2, 4351, 'special', 1, 13501);
INSERT INTO `general_pre` VALUES (30401, '华雄(狂)',     '群', 4, 'purple', '步', '猛将',  8, 4041, 'special', 1, 10401);
INSERT INTO `general_pre` VALUES (30405, '高顺(狂)',     '群', 4, 'purple', '步', '统帅',  7, 4045, 'special', 1, 10405);
INSERT INTO `general_pre` VALUES (31405, '夏侯渊(狂)',   '魏', 4, 'purple', '骑', '猛将', 10, 4145, 'special', 1, 11405);
INSERT INTO `general_pre` VALUES (32405, '关兴(狂)',     '蜀', 4, 'purple', '步', '猛将',  8, 4245, 'special', 1, 12405);
INSERT INTO `general_pre` VALUES (33405, '程普(狂)',     '吴', 4, 'purple', '步', '统帅',  7, 4345, 'special', 1, 13405);
INSERT INTO `general_pre` VALUES (31401, '曹仁(狂)',     '魏', 4, 'purple', '步', '统帅',  7, 4141, 'special', 1, 11401);
INSERT INTO `general_pre` VALUES (32401, '关平(狂)',     '蜀', 4, 'purple', '步', '猛将',  8, 4241, 'special', 1, 12401);
INSERT INTO `general_pre` VALUES (33401, '丁奉(狂)',     '吴', 4, 'purple', '步', '猛将',  8, 4341, 'special', 1, 13401);
INSERT INTO `general_pre` VALUES (30407, '公孙瓒(狂)',   '群', 4, 'purple', '弓', '统帅', 12, 4047, 'special', 1, 10407);

-- =============================================
-- 数据统计:
--   普通武将: 69 (绿12 + 蓝16 + 紫28 + 橙22 + 特殊1)
--   狂化武将: 13
--   合计: 82
--
-- 各阵营分布:
--   群: 绿12 + 蓝8 + 紫7 + 橙1 = 28
--   魏: 蓝4 + 紫7 + 橙7 = 18
--   蜀: 蓝4 + 紫7 + 橙7 = 18
--   吴: 蓝4 + 紫7 + 橙7 = 18
--   特殊: 橙1 (貂蝉)
--
-- 兵种分布 (非狂化):
--   步兵: ~24  骑兵: ~23  弓兵: ~22
-- =============================================
