-- ========== 世界Boss 总血量池迁移 ==========
-- unit_soldiers 列现在只存储 remainingHp (纯数字)
-- 总HP(兵力): 黄巾=3000000, 董卓=5000000, 异族=8000000

-- 1) 修改列注释和默认值
ALTER TABLE world_boss_state
    MODIFY COLUMN unit_soldiers VARCHAR(256) NOT NULL DEFAULT '0'
    COMMENT 'Boss剩余兵力(remainingHp)';

-- 2) 重置现有Boss状态
UPDATE world_boss_state SET
    status = 'waiting',
    unit_soldiers = '0',
    last_killer = '',
    window_start_ms = 0;

-- 3) 清空旧伤害记录
DELETE FROM world_boss_damage WHERE 1=1;

-- 4) 上期战报持久化表
CREATE TABLE IF NOT EXISTS world_boss_last_report (
    server_id INT NOT NULL,
    report_json TEXT NOT NULL DEFAULT '{}',
    updated_at BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (server_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='世界Boss上期战报(每服一条)';
