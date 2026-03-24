-- =============================================
-- 兵法配置表 (从 APK WarBookShow_cfg.json 提取)
-- 12个兵法，每个10级，包含效果描述
-- =============================================

CREATE TABLE IF NOT EXISTS `warbook_config` (
  `cfg_id` INT NOT NULL COMMENT '兵法配置ID (如33001)',
  `name` VARCHAR(32) NOT NULL COMMENT '兵法名称',
  `color` INT DEFAULT 3 COMMENT '品质',
  `type` VARCHAR(16) NOT NULL COMMENT '类型: 连击/穿透/强击/偷袭/强袭/伏击/防御/闪避/反伤/斩将',
  `army_type` VARCHAR(16) NOT NULL COMMENT '适用兵种: 步兵/骑兵/弓兵/全兵种',
  `pic_name` VARCHAR(128) COMMENT '图片路径',
  PRIMARY KEY (`cfg_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='兵法基础配置(APK提取)';

CREATE TABLE IF NOT EXISTS `warbook_level_config` (
  `id` BIGINT AUTO_INCREMENT,
  `cfg_id` INT NOT NULL COMMENT '关联 warbook_config.cfg_id',
  `level` INT NOT NULL COMMENT '兵法等级 1-10',
  `description` VARCHAR(256) NOT NULL COMMENT '该等级效果描述',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_cfg_level` (`cfg_id`, `level`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='兵法等级效果配置(APK提取)';

DELETE FROM `warbook_level_config`;
DELETE FROM `warbook_config`;

INSERT INTO `warbook_config` VALUES (33001, '连射', 3, '连击', '弓兵', 'image/Prop/33001.jpg');
INSERT INTO `warbook_config` VALUES (33002, '长虹贯日', 3, '穿透', '弓兵', 'image/Prop/33002.jpg');
INSERT INTO `warbook_config` VALUES (33003, '落月弓', 3, '强击', '弓兵', 'image/Prop/33003.jpg');
INSERT INTO `warbook_config` VALUES (33004, '声东击西', 3, '偷袭', '骑兵', 'image/Prop/33004.jpg');
INSERT INTO `warbook_config` VALUES (33005, '铁骑冲锋', 3, '强袭', '骑兵', 'image/Prop/33005.jpg');
INSERT INTO `warbook_config` VALUES (33006, '以逸待劳', 3, '伏击', '骑兵', 'image/Prop/33006.jpg');
INSERT INTO `warbook_config` VALUES (33007, '方圆阵', 3, '防御', '步兵', 'image/Prop/33007.jpg');
INSERT INTO `warbook_config` VALUES (33008, '偃月阵', 3, '防御', '步兵', 'image/Prop/33008.jpg');
INSERT INTO `warbook_config` VALUES (33009, '长蛇阵', 3, '防御', '步兵', 'image/Prop/33009.jpg');
INSERT INTO `warbook_config` VALUES (33010, '雁行阵', 3, '闪避', '全兵种', 'image/Prop/33010.jpg');
INSERT INTO `warbook_config` VALUES (33011, '却月阵', 3, '反伤', '步兵', 'image/Prop/33011.jpg');
INSERT INTO `warbook_config` VALUES (33012, '战神突击', 4, '贯穿', '骑兵', 'image/Prop/33012.jpg');

INSERT INTO `warbook_level_config` (`cfg_id`, `level`, `description`) VALUES (33001, 1, '弓兵兵法，连续发动2次攻击');
INSERT INTO `warbook_level_config` (`cfg_id`, `level`, `description`) VALUES (33001, 2, '弓兵兵法，连续发动2次攻击，极小概率发动3次攻击');
INSERT INTO `warbook_level_config` (`cfg_id`, `level`, `description`) VALUES (33001, 3, '弓兵兵法，连续发动2次攻击，较小概率发动3次攻击');
INSERT INTO `warbook_level_config` (`cfg_id`, `level`, `description`) VALUES (33001, 4, '弓兵兵法，连续发动2次攻击，较小概率发动3次攻击');
INSERT INTO `warbook_level_config` (`cfg_id`, `level`, `description`) VALUES (33001, 5, '弓兵兵法，连续发动2次攻击，较小概率发动3次攻击');
INSERT INTO `warbook_level_config` (`cfg_id`, `level`, `description`) VALUES (33001, 6, '弓兵兵法，连续发动2次或者3次攻击，最多发动4次攻击');
INSERT INTO `warbook_level_config` (`cfg_id`, `level`, `description`) VALUES (33001, 7, '弓兵兵法，连续发动2次或者3次攻击，最多发动4次攻击');
INSERT INTO `warbook_level_config` (`cfg_id`, `level`, `description`) VALUES (33001, 8, '弓兵兵法，连续发动2次或者3次攻击，最多发动4次攻击');
INSERT INTO `warbook_level_config` (`cfg_id`, `level`, `description`) VALUES (33001, 9, '弓兵兵法，连续发动2次或者3次攻击，最多发动4次攻击');
INSERT INTO `warbook_level_config` (`cfg_id`, `level`, `description`) VALUES (33001, 10, '弓兵兵法，连续发动2次或者3次攻击，最多发动4次攻击');
INSERT INTO `warbook_level_config` (`cfg_id`, `level`, `description`) VALUES (33002, 1, '弓兵兵法，穿透性攻击，对同一行所有敌人造成40%伤害');
INSERT INTO `warbook_level_config` (`cfg_id`, `level`, `description`) VALUES (33002, 2, '弓兵兵法，穿透性攻击，对同一行所有敌人造成43%伤害');
INSERT INTO `warbook_level_config` (`cfg_id`, `level`, `description`) VALUES (33002, 3, '弓兵兵法，穿透性攻击，对同一行所有敌人造成46%伤害');
INSERT INTO `warbook_level_config` (`cfg_id`, `level`, `description`) VALUES (33002, 4, '弓兵兵法，穿透性攻击，对同一行所有敌人造成49%伤害');
INSERT INTO `warbook_level_config` (`cfg_id`, `level`, `description`) VALUES (33002, 5, '弓兵兵法，穿透性攻击，对同一行所有敌人造成52%伤害');
INSERT INTO `warbook_level_config` (`cfg_id`, `level`, `description`) VALUES (33002, 6, '弓兵兵法，穿透性攻击，对同一行所有敌人造成55%伤害');
INSERT INTO `warbook_level_config` (`cfg_id`, `level`, `description`) VALUES (33002, 7, '弓兵兵法，穿透性攻击，对同一行所有敌人造成58%伤害');
INSERT INTO `warbook_level_config` (`cfg_id`, `level`, `description`) VALUES (33002, 8, '弓兵兵法，穿透性攻击，对同一行所有敌人造成61%伤害');
INSERT INTO `warbook_level_config` (`cfg_id`, `level`, `description`) VALUES (33002, 9, '弓兵兵法，穿透性攻击，对同一行所有敌人造成64%伤害');
INSERT INTO `warbook_level_config` (`cfg_id`, `level`, `description`) VALUES (33002, 10, '弓兵兵法，穿透性攻击，对同一行所有敌人造成67%伤害');
INSERT INTO `warbook_level_config` (`cfg_id`, `level`, `description`) VALUES (33003, 1, '弓兵兵法，增加20点攻击力，造成110%的伤害');
INSERT INTO `warbook_level_config` (`cfg_id`, `level`, `description`) VALUES (33003, 2, '弓兵兵法，增加40点攻击力，造成120%的伤害');
INSERT INTO `warbook_level_config` (`cfg_id`, `level`, `description`) VALUES (33003, 3, '弓兵兵法，增加60点攻击力，造成130%的伤害');
INSERT INTO `warbook_level_config` (`cfg_id`, `level`, `description`) VALUES (33003, 4, '弓兵兵法，增加80点攻击力，造成140%的伤害');
INSERT INTO `warbook_level_config` (`cfg_id`, `level`, `description`) VALUES (33003, 5, '弓兵兵法，增加100点攻击力，造成150%的伤害');
INSERT INTO `warbook_level_config` (`cfg_id`, `level`, `description`) VALUES (33003, 6, '弓兵兵法，增加120点攻击力，造成160%的伤害');
INSERT INTO `warbook_level_config` (`cfg_id`, `level`, `description`) VALUES (33003, 7, '弓兵兵法，增加140点攻击力，造成170%的伤害');
INSERT INTO `warbook_level_config` (`cfg_id`, `level`, `description`) VALUES (33003, 8, '弓兵兵法，增加160点攻击力，造成180%的伤害');
INSERT INTO `warbook_level_config` (`cfg_id`, `level`, `description`) VALUES (33003, 9, '弓兵兵法，增加180点攻击力，造成190%的伤害');
INSERT INTO `warbook_level_config` (`cfg_id`, `level`, `description`) VALUES (33003, 10, '弓兵兵法，增加200点攻击力，造成200%的伤害');
INSERT INTO `warbook_level_config` (`cfg_id`, `level`, `description`) VALUES (33004, 1, '骑兵兵法，优先对敌方阵中的弓兵部队发动偷袭，附加15点攻击力，造成110%伤害');
INSERT INTO `warbook_level_config` (`cfg_id`, `level`, `description`) VALUES (33004, 2, '骑兵兵法，优先对敌方阵中的弓兵部队发动偷袭，附加30点攻击力，造成120%伤害');
INSERT INTO `warbook_level_config` (`cfg_id`, `level`, `description`) VALUES (33004, 3, '骑兵兵法，优先对敌方阵中的弓兵部队发动偷袭，附加45点攻击力，造成130%伤害');
INSERT INTO `warbook_level_config` (`cfg_id`, `level`, `description`) VALUES (33004, 4, '骑兵兵法，优先对敌方阵中的弓兵部队发动偷袭，附加60点攻击力，造成140%伤害');
INSERT INTO `warbook_level_config` (`cfg_id`, `level`, `description`) VALUES (33004, 5, '骑兵兵法，优先对敌方阵中的弓兵部队发动偷袭，附加75点攻击力，造成150%伤害');
INSERT INTO `warbook_level_config` (`cfg_id`, `level`, `description`) VALUES (33004, 6, '骑兵兵法，优先对敌方阵中的弓兵部队发动偷袭，附加90点攻击力，造成160%伤害');
INSERT INTO `warbook_level_config` (`cfg_id`, `level`, `description`) VALUES (33004, 7, '骑兵兵法，优先对敌方阵中的弓兵部队发动偷袭，附加105点攻击力，造成170%伤害');
INSERT INTO `warbook_level_config` (`cfg_id`, `level`, `description`) VALUES (33004, 8, '骑兵兵法，优先对敌方阵中的弓兵部队发动偷袭，附加120点攻击力，造成180%伤害');
INSERT INTO `warbook_level_config` (`cfg_id`, `level`, `description`) VALUES (33004, 9, '骑兵兵法，优先对敌方阵中的弓兵部队发动偷袭，附加135点攻击力，造成190%伤害');
INSERT INTO `warbook_level_config` (`cfg_id`, `level`, `description`) VALUES (33004, 10, '骑兵兵法，优先对敌方阵中的弓兵部队发动偷袭，附加150点攻击力，造成200%伤害');
INSERT INTO `warbook_level_config` (`cfg_id`, `level`, `description`) VALUES (33005, 1, '骑兵兵法，增加30点攻击力，对目标造成105%伤害');
INSERT INTO `warbook_level_config` (`cfg_id`, `level`, `description`) VALUES (33005, 2, '骑兵兵法，增加60点攻击力，对目标造成110%伤害');
INSERT INTO `warbook_level_config` (`cfg_id`, `level`, `description`) VALUES (33005, 3, '骑兵兵法，增加70点攻击力，对目标造成115%伤害');
INSERT INTO `warbook_level_config` (`cfg_id`, `level`, `description`) VALUES (33005, 4, '骑兵兵法，增加120点攻击力，对目标造成120%伤害');
INSERT INTO `warbook_level_config` (`cfg_id`, `level`, `description`) VALUES (33005, 5, '骑兵兵法，增加150点攻击力，对目标造成125%伤害');
INSERT INTO `warbook_level_config` (`cfg_id`, `level`, `description`) VALUES (33005, 6, '骑兵兵法，增加180点攻击力，对目标造成130%伤害');
INSERT INTO `warbook_level_config` (`cfg_id`, `level`, `description`) VALUES (33005, 7, '骑兵兵法，增加210点攻击力，对目标造成135%伤害');
INSERT INTO `warbook_level_config` (`cfg_id`, `level`, `description`) VALUES (33005, 8, '骑兵兵法，增加240点攻击力，对目标造成140%伤害');
INSERT INTO `warbook_level_config` (`cfg_id`, `level`, `description`) VALUES (33005, 9, '骑兵兵法，增加270点攻击力，对目标造成145%伤害');
INSERT INTO `warbook_level_config` (`cfg_id`, `level`, `description`) VALUES (33005, 10, '骑兵兵法，增加300点攻击力，对目标造成150%伤害');
INSERT INTO `warbook_level_config` (`cfg_id`, `level`, `description`) VALUES (33006, 1, '骑兵兵法，一定概率打断敌方的偷袭，并对目标造成30%伤害');
INSERT INTO `warbook_level_config` (`cfg_id`, `level`, `description`) VALUES (33006, 2, '骑兵兵法，一定概率打断敌方的偷袭，并对目标造成40%伤害');
INSERT INTO `warbook_level_config` (`cfg_id`, `level`, `description`) VALUES (33006, 3, '骑兵兵法，一定概率打断敌方的偷袭，并对目标造成50%伤害');
INSERT INTO `warbook_level_config` (`cfg_id`, `level`, `description`) VALUES (33006, 4, '骑兵兵法，一定概率打断敌方的偷袭，并对目标造成60%伤害');
INSERT INTO `warbook_level_config` (`cfg_id`, `level`, `description`) VALUES (33006, 5, '骑兵兵法，一定概率打断敌方的偷袭，并对目标造成70%伤害');
INSERT INTO `warbook_level_config` (`cfg_id`, `level`, `description`) VALUES (33006, 6, '骑兵兵法，一定概率打断敌方的偷袭，并对目标造成80%伤害');
INSERT INTO `warbook_level_config` (`cfg_id`, `level`, `description`) VALUES (33006, 7, '骑兵兵法，一定概率打断敌方的偷袭，并对目标造成90%伤害');
INSERT INTO `warbook_level_config` (`cfg_id`, `level`, `description`) VALUES (33006, 8, '骑兵兵法，一定概率打断敌方的偷袭，并对目标造成100%伤害');
INSERT INTO `warbook_level_config` (`cfg_id`, `level`, `description`) VALUES (33006, 9, '骑兵兵法，一定概率打断敌方的偷袭，并对目标造成110%伤害');
INSERT INTO `warbook_level_config` (`cfg_id`, `level`, `description`) VALUES (33006, 10, '骑兵兵法，一定概率打断敌方的偷袭，并对目标造成120%伤害');
INSERT INTO `warbook_level_config` (`cfg_id`, `level`, `description`) VALUES (33007, 1, '步兵防御阵型，降低2%的伤害');
INSERT INTO `warbook_level_config` (`cfg_id`, `level`, `description`) VALUES (33007, 2, '步兵防御阵型，降低4%的伤害');
INSERT INTO `warbook_level_config` (`cfg_id`, `level`, `description`) VALUES (33007, 3, '步兵防御阵型，降低6%的伤害');
INSERT INTO `warbook_level_config` (`cfg_id`, `level`, `description`) VALUES (33007, 4, '步兵防御阵型，降低8%的伤害');
INSERT INTO `warbook_level_config` (`cfg_id`, `level`, `description`) VALUES (33007, 5, '步兵防御阵型，降低10%的伤害');
INSERT INTO `warbook_level_config` (`cfg_id`, `level`, `description`) VALUES (33007, 6, '步兵防御阵型，降低12%的伤害');
INSERT INTO `warbook_level_config` (`cfg_id`, `level`, `description`) VALUES (33007, 7, '步兵防御阵型，降低14%的伤害');
INSERT INTO `warbook_level_config` (`cfg_id`, `level`, `description`) VALUES (33007, 8, '步兵防御阵型，降低16%的伤害');
INSERT INTO `warbook_level_config` (`cfg_id`, `level`, `description`) VALUES (33007, 9, '步兵防御阵型，降低18%的伤害');
INSERT INTO `warbook_level_config` (`cfg_id`, `level`, `description`) VALUES (33007, 10, '步兵防御阵型，降低20%的伤害');
INSERT INTO `warbook_level_config` (`cfg_id`, `level`, `description`) VALUES (33008, 1, '步兵防御阵型，降低3%的骑兵攻击伤害');
INSERT INTO `warbook_level_config` (`cfg_id`, `level`, `description`) VALUES (33008, 2, '步兵防御阵型，降低6%的骑兵攻击伤害');
INSERT INTO `warbook_level_config` (`cfg_id`, `level`, `description`) VALUES (33008, 3, '步兵防御阵型，降低9%的骑兵攻击伤害');
INSERT INTO `warbook_level_config` (`cfg_id`, `level`, `description`) VALUES (33008, 4, '步兵防御阵型，降低12%的骑兵攻击伤害');
INSERT INTO `warbook_level_config` (`cfg_id`, `level`, `description`) VALUES (33008, 5, '步兵防御阵型，降低15%的骑兵攻击伤害');
INSERT INTO `warbook_level_config` (`cfg_id`, `level`, `description`) VALUES (33008, 6, '步兵防御阵型，降低18%的骑兵攻击伤害');
INSERT INTO `warbook_level_config` (`cfg_id`, `level`, `description`) VALUES (33008, 7, '步兵防御阵型，降低21%的骑兵攻击伤害');
INSERT INTO `warbook_level_config` (`cfg_id`, `level`, `description`) VALUES (33008, 8, '步兵防御阵型，降低24%的骑兵攻击伤害');
INSERT INTO `warbook_level_config` (`cfg_id`, `level`, `description`) VALUES (33008, 9, '步兵防御阵型，降低27%的骑兵攻击伤害');
INSERT INTO `warbook_level_config` (`cfg_id`, `level`, `description`) VALUES (33008, 10, '步兵防御阵型，降低30%的骑兵攻击伤害');
INSERT INTO `warbook_level_config` (`cfg_id`, `level`, `description`) VALUES (33009, 1, '步兵防御阵型，降低3%的弓兵攻击伤害');
INSERT INTO `warbook_level_config` (`cfg_id`, `level`, `description`) VALUES (33009, 2, '步兵防御阵型，降低6%的弓兵攻击伤害');
INSERT INTO `warbook_level_config` (`cfg_id`, `level`, `description`) VALUES (33009, 3, '步兵防御阵型，降低9%的弓兵攻击伤害');
INSERT INTO `warbook_level_config` (`cfg_id`, `level`, `description`) VALUES (33009, 4, '步兵防御阵型，降低12%的弓兵攻击伤害');
INSERT INTO `warbook_level_config` (`cfg_id`, `level`, `description`) VALUES (33009, 5, '步兵防御阵型，降低15%的弓兵攻击伤害');
INSERT INTO `warbook_level_config` (`cfg_id`, `level`, `description`) VALUES (33009, 6, '步兵防御阵型，降低18%的弓兵攻击伤害');
INSERT INTO `warbook_level_config` (`cfg_id`, `level`, `description`) VALUES (33009, 7, '步兵防御阵型，降低21%的弓兵攻击伤害');
INSERT INTO `warbook_level_config` (`cfg_id`, `level`, `description`) VALUES (33009, 8, '步兵防御阵型，降低24%的弓兵攻击伤害');
INSERT INTO `warbook_level_config` (`cfg_id`, `level`, `description`) VALUES (33009, 9, '步兵防御阵型，降低27%的弓兵攻击伤害');
INSERT INTO `warbook_level_config` (`cfg_id`, `level`, `description`) VALUES (33009, 10, '步兵防御阵型，降低30%的弓兵攻击伤害');
INSERT INTO `warbook_level_config` (`cfg_id`, `level`, `description`) VALUES (33010, 1, '灵活的行军阵型，增加部队闪避值2');
INSERT INTO `warbook_level_config` (`cfg_id`, `level`, `description`) VALUES (33010, 2, '灵活的行军阵型，增加部队闪避值4');
INSERT INTO `warbook_level_config` (`cfg_id`, `level`, `description`) VALUES (33010, 3, '灵活的行军阵型，增加部队闪避值6');
INSERT INTO `warbook_level_config` (`cfg_id`, `level`, `description`) VALUES (33010, 4, '灵活的行军阵型，增加部队闪避值8');
INSERT INTO `warbook_level_config` (`cfg_id`, `level`, `description`) VALUES (33010, 5, '灵活的行军阵型，增加部队闪避值10');
INSERT INTO `warbook_level_config` (`cfg_id`, `level`, `description`) VALUES (33010, 6, '灵活的行军阵型，增加部队闪避值12');
INSERT INTO `warbook_level_config` (`cfg_id`, `level`, `description`) VALUES (33010, 7, '灵活的行军阵型，增加部队闪避值14');
INSERT INTO `warbook_level_config` (`cfg_id`, `level`, `description`) VALUES (33010, 8, '灵活的行军阵型，增加部队闪避值16');
INSERT INTO `warbook_level_config` (`cfg_id`, `level`, `description`) VALUES (33010, 9, '灵活的行军阵型，增加部队闪避值18');
INSERT INTO `warbook_level_config` (`cfg_id`, `level`, `description`) VALUES (33010, 10, '灵活的行军阵型，增加部队闪避值20');
INSERT INTO `warbook_level_config` (`cfg_id`, `level`, `description`) VALUES (33011, 1, '步兵防御阵型，承受额外的弓兵伤害，降低15%的骑兵攻击伤害并将伤害反弹');
INSERT INTO `warbook_level_config` (`cfg_id`, `level`, `description`) VALUES (33011, 2, '步兵防御阵型，承受额外的弓兵伤害，降低17%的骑兵攻击伤害并将伤害反弹');
INSERT INTO `warbook_level_config` (`cfg_id`, `level`, `description`) VALUES (33011, 3, '步兵防御阵型，承受额外的弓兵伤害，降低19%的骑兵攻击伤害并将伤害反弹');
INSERT INTO `warbook_level_config` (`cfg_id`, `level`, `description`) VALUES (33011, 4, '步兵防御阵型，承受额外的弓兵伤害，降低21%的骑兵攻击伤害并将伤害反弹');
INSERT INTO `warbook_level_config` (`cfg_id`, `level`, `description`) VALUES (33011, 5, '步兵防御阵型，承受额外的弓兵伤害，降低23%的骑兵攻击伤害并将伤害反弹');
INSERT INTO `warbook_level_config` (`cfg_id`, `level`, `description`) VALUES (33011, 6, '步兵防御阵型，承受额外的弓兵伤害，降低25%的骑兵攻击伤害并将伤害反弹');
INSERT INTO `warbook_level_config` (`cfg_id`, `level`, `description`) VALUES (33011, 7, '步兵防御阵型，承受额外的弓兵伤害，降低27%的骑兵攻击伤害并将伤害反弹');
INSERT INTO `warbook_level_config` (`cfg_id`, `level`, `description`) VALUES (33011, 8, '步兵防御阵型，承受额外的弓兵伤害，降低29%的骑兵攻击伤害并将伤害反弹');
INSERT INTO `warbook_level_config` (`cfg_id`, `level`, `description`) VALUES (33011, 9, '步兵防御阵型，承受额外的弓兵伤害，降低34%的骑兵攻击伤害并将伤害反弹');
INSERT INTO `warbook_level_config` (`cfg_id`, `level`, `description`) VALUES (33011, 10, '步兵防御阵型，承受额外的弓兵伤害，降低37%的骑兵攻击伤害并将伤害反弹');
INSERT INTO `warbook_level_config` (`cfg_id`, `level`, `description`) VALUES (33012, 1, '吕布专属兵法，发挥骑兵强大的突击能力，对同一行所有敌人造成50%伤害');
INSERT INTO `warbook_level_config` (`cfg_id`, `level`, `description`) VALUES (33012, 2, '吕布专属兵法，发挥骑兵强大的突击能力，对同一行所有敌人造成52%伤害');
INSERT INTO `warbook_level_config` (`cfg_id`, `level`, `description`) VALUES (33012, 3, '吕布专属兵法，发挥骑兵强大的突击能力，对同一行所有敌人造成54%伤害');
INSERT INTO `warbook_level_config` (`cfg_id`, `level`, `description`) VALUES (33012, 4, '吕布专属兵法，发挥骑兵强大的突击能力，对同一行所有敌人造成56%伤害');
INSERT INTO `warbook_level_config` (`cfg_id`, `level`, `description`) VALUES (33012, 5, '吕布专属兵法，发挥骑兵强大的突击能力，对同一行所有敌人造成58%伤害');
INSERT INTO `warbook_level_config` (`cfg_id`, `level`, `description`) VALUES (33012, 6, '吕布专属兵法，发挥骑兵强大的突击能力，对同一行所有敌人造成60%伤害');
INSERT INTO `warbook_level_config` (`cfg_id`, `level`, `description`) VALUES (33012, 7, '吕布专属兵法，发挥骑兵强大的突击能力，对同一行所有敌人造成62%伤害');
INSERT INTO `warbook_level_config` (`cfg_id`, `level`, `description`) VALUES (33012, 8, '吕布专属兵法，发挥骑兵强大的突击能力，对同一行所有敌人造成64%伤害');
INSERT INTO `warbook_level_config` (`cfg_id`, `level`, `description`) VALUES (33012, 9, '吕布专属兵法，发挥骑兵强大的突击能力，对同一行所有敌人造成66%伤害');
INSERT INTO `warbook_level_config` (`cfg_id`, `level`, `description`) VALUES (33012, 10, '吕布专属兵法，发挥骑兵强大的突击能力，对同一行所有敌人造成68%伤害');