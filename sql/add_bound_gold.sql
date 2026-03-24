-- 添加绑金字段到 user_resource 表
ALTER TABLE user_resource ADD COLUMN bound_gold BIGINT DEFAULT 0 AFTER gold;
