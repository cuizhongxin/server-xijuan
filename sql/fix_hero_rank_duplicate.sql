-- 修复 hero_rank 表重复数据问题
-- 原因: 唯一键 uk_hr_user 只有 user_id，多区服场景可能产生重复

-- 1. 清理重复数据（保留id最大的那条）
DELETE h1 FROM hero_rank h1
INNER JOIN hero_rank h2
ON h1.user_id = h2.user_id AND h1.id < h2.id;

-- 2. 如果原唯一键存在则先删除，重建为 (user_id) 确保不重复
-- （唯一键已存在则此步骤为安全确认）
-- ALTER TABLE hero_rank DROP INDEX IF EXISTS uk_hr_user;
-- ALTER TABLE hero_rank ADD UNIQUE KEY uk_hr_user (user_id);
