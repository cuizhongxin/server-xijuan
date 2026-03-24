-- ============================================
-- 爵位、等级经验系统调整 (对齐APK配置)
-- ============================================

-- 1. 更新爵位配置 (来自APK BanneretID.json)
-- 先清空旧数据再插入，保证一致
TRUNCATE TABLE peerage_config;

INSERT INTO peerage_config (rank_name, fame_required, level_required, max_soldier_tier, color, sort_order) VALUES
('平民',   0,        1,   1,  '#aaaaaa', 1),
('公士',   1000,     10,  2,  '#55cc55', 2),
('民爵',   3000,     20,  3,  '#55cc55', 3),
('勋爵',   9000,     30,  4,  '#5588ff', 4),
('男爵',   20000,    40,  5,  '#5588ff', 5),
('子爵',   45000,    50,  6,  '#bb55ff', 6),
('伯爵',   90000,    60,  7,  '#bb55ff', 7),
('侯爵',   220000,   70,  8,  '#ffaa00', 8),
('公爵',   600000,   75,  9,  '#ffaa00', 9),
('王',     1200000,  80,  10, '#ff4444', 10);


-- 2. 更新兵种阶级的爵位需求 (soldier_tier)
UPDATE soldier_tier SET peerage_required = '平民' WHERE tier = 1;
UPDATE soldier_tier SET peerage_required = '公士' WHERE tier = 2;
UPDATE soldier_tier SET peerage_required = '民爵' WHERE tier = 3;
UPDATE soldier_tier SET peerage_required = '勋爵' WHERE tier = 4;
UPDATE soldier_tier SET peerage_required = '男爵' WHERE tier = 5;
UPDATE soldier_tier SET peerage_required = '子爵' WHERE tier = 6;
UPDATE soldier_tier SET peerage_required = '伯爵' WHERE tier = 7;
UPDATE soldier_tier SET peerage_required = '侯爵' WHERE tier = 8;
UPDATE soldier_tier SET peerage_required = '公爵' WHERE tier = 9;


-- 3. 迁移现有用户爵位名称 (user_resource.rank)
-- 先处理高爵位(避免名称冲突: 旧"平民"→新"公士")
UPDATE user_resource SET rank = '侯爵' WHERE rank = '公';
UPDATE user_resource SET rank = '伯爵' WHERE rank = '侯';
UPDATE user_resource SET rank = '子爵' WHERE rank = '伯';
UPDATE user_resource SET rank = '男爵' WHERE rank = '子';
UPDATE user_resource SET rank = '勋爵' WHERE rank = '男';
UPDATE user_resource SET rank = '民爵' WHERE rank = '士人';
UPDATE user_resource SET rank = '公士' WHERE rank = '平民';
UPDATE user_resource SET rank = '平民' WHERE rank = '白身';
-- '王' 保持不变
UPDATE user_resource SET rank = '平民' WHERE rank IS NULL OR rank = '';

-- 4. 同步 hero_rank 表的爵位名称
UPDATE hero_rank SET rank_name = '侯爵' WHERE rank_name = '公';
UPDATE hero_rank SET rank_name = '伯爵' WHERE rank_name = '侯';
UPDATE hero_rank SET rank_name = '子爵' WHERE rank_name = '伯';
UPDATE hero_rank SET rank_name = '男爵' WHERE rank_name = '子';
UPDATE hero_rank SET rank_name = '勋爵' WHERE rank_name = '男';
UPDATE hero_rank SET rank_name = '民爵' WHERE rank_name = '士人';
UPDATE hero_rank SET rank_name = '公士' WHERE rank_name = '平民';
UPDATE hero_rank SET rank_name = '平民' WHERE rank_name = '白身';
UPDATE hero_rank SET rank_name = '平民' WHERE rank_name IS NULL OR rank_name = '';
