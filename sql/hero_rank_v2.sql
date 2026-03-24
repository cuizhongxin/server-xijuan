-- ============================================
-- 英雄榜 V2 — 按APK设计重构（排名互换制）
-- ============================================

-- 0. 清除旧数据重新初始化（可选，首次部署V2时执行）
-- DELETE FROM hero_rank WHERE user_id LIKE 'npc_hero_%';

-- 1. hero_rank 新增字段
ALTER TABLE hero_rank
  ADD COLUMN IF NOT EXISTS pending_fame   BIGINT DEFAULT 0 COMMENT '待领取声望',
  ADD COLUMN IF NOT EXISTS pending_silver BIGINT DEFAULT 0 COMMENT '待领取白银',
  ADD COLUMN IF NOT EXISTS pending_exp    BIGINT DEFAULT 0 COMMENT '待领取经验',
  ADD COLUMN IF NOT EXISTS reward_claimed TINYINT(1) DEFAULT 1 COMMENT '奖励是否已领取(1=已领/无奖励)',
  ADD COLUMN IF NOT EXISTS settle_date   VARCHAR(8) DEFAULT '' COMMENT '奖励对应结算日期',
  ADD COLUMN IF NOT EXISTS nation        VARCHAR(8) DEFAULT '' COMMENT '国家(WEI/SHU/WU)';

-- 2. hero_rank_battle 新增排名变动 + 战报存储字段
ALTER TABLE hero_rank_battle
  ADD COLUMN IF NOT EXISTS atk_old_rank INT DEFAULT 0 COMMENT '攻方原排名',
  ADD COLUMN IF NOT EXISTS atk_new_rank INT DEFAULT 0 COMMENT '攻方新排名',
  ADD COLUMN IF NOT EXISTS def_old_rank INT DEFAULT 0 COMMENT '守方原排名',
  ADD COLUMN IF NOT EXISTS def_new_rank INT DEFAULT 0 COMMENT '守方新排名',
  ADD COLUMN IF NOT EXISTS battle_report TEXT COMMENT '完整战报JSON(用于回放)';

-- 3. 新增 battle 索引按 defender 查询（被挑战记录）
-- ALTER TABLE hero_rank_battle ADD INDEX IF NOT EXISTS idx_defender (defender_id, create_date);
-- (已存在则忽略)

-- 4. 更新爵位配置为APK版本（BanneretID.json）
DELETE FROM peerage_config;
INSERT INTO peerage_config (rank_name, fame_required, level_required, max_soldier_tier, color, sort_order) VALUES
('平民',   0,        1,  1, '#aaaaaa', 1),
('公士',   1000,    10,  2, '#55cc55', 2),
('民爵',   3000,    20,  3, '#55cc55', 3),
('勋爵',   9000,    30,  4, '#5588ff', 4),
('男爵',   20000,   40,  5, '#5588ff', 5),
('子爵',   45000,   50,  6, '#bb55ff', 6),
('伯爵',   90000,   60,  7, '#bb55ff', 7),
('侯爵',   220000,  70,  8, '#ffaa00', 8),
('公爵',   600000,  75,  9, '#ffaa00', 9),
('王',     1200000, 80, 10, '#ff4444', 10);
