-- 修复 general_template 和 general 表中 avatar 字段包含零宽不可见字符的问题
-- 零宽非连接符 U+200C (UTF-8: 0xE2 0x80 0x8C) 会导致云存储文件 404

-- 清理 general_template 表中所有 avatar 的不可见字符
UPDATE `general_template`
SET `avatar` = REPLACE(`avatar`, UNHEX('E2808C'), '')
WHERE `avatar` LIKE CONCAT('%', UNHEX('E2808C'), '%');

-- 清理零宽连接符 U+200D
UPDATE `general_template`
SET `avatar` = REPLACE(`avatar`, UNHEX('E2808D'), '')
WHERE `avatar` LIKE CONCAT('%', UNHEX('E2808D'), '%');

-- 清理零宽空格 U+200B
UPDATE `general_template`
SET `avatar` = REPLACE(`avatar`, UNHEX('E2808B'), '')
WHERE `avatar` LIKE CONCAT('%', UNHEX('E2808B'), '%');

-- 同步修复已创建的 general 表中的 avatar
UPDATE `general`
SET `avatar` = REPLACE(`avatar`, UNHEX('E2808C'), '')
WHERE `avatar` LIKE CONCAT('%', UNHEX('E2808C'), '%');

UPDATE `general`
SET `avatar` = REPLACE(`avatar`, UNHEX('E2808D'), '')
WHERE `avatar` LIKE CONCAT('%', UNHEX('E2808D'), '%');

UPDATE `general`
SET `avatar` = REPLACE(`avatar`, UNHEX('E2808B'), '')
WHERE `avatar` LIKE CONCAT('%', UNHEX('E2808B'), '%');

-- 同步修复 formation_slot 中的武将头像
UPDATE `formation_slot`
SET `general_avatar` = REPLACE(`general_avatar`, UNHEX('E2808C'), '')
WHERE `general_avatar` LIKE CONCAT('%', UNHEX('E2808C'), '%');
