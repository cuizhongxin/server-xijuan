-- 区服隔离迁移脚本
-- 1. hero_rank 表添加 server_id
-- 2. hero_rank_battle 表添加 server_id
-- 3. 清理旧的全局NPC数据（将由创建区服时按服重新生成）

-- ========== hero_rank ==========
ALTER TABLE hero_rank
    ADD COLUMN server_id INT NOT NULL DEFAULT 1 COMMENT '区服ID' AFTER update_time,
    ADD COLUMN rank_position INT DEFAULT 0 AFTER ranking,
    ADD COLUMN nation VARCHAR(8) DEFAULT 'WEI' AFTER rank_name,
    ADD COLUMN pending_fame BIGINT DEFAULT 0 AFTER last_challenge_time,
    ADD COLUMN pending_silver BIGINT DEFAULT 0 AFTER pending_fame,
    ADD COLUMN pending_exp BIGINT DEFAULT 0 AFTER pending_silver,
    ADD COLUMN reward_claimed TINYINT DEFAULT 1 AFTER pending_exp,
    ADD COLUMN settle_date VARCHAR(8) DEFAULT '' AFTER reward_claimed;

-- 如果上面某些列已经存在会报错，可忽略，下面再单独加 server_id
-- ALTER TABLE hero_rank ADD COLUMN server_id INT NOT NULL DEFAULT 1 COMMENT '区服ID';

ALTER TABLE hero_rank ADD INDEX idx_server_ranking (server_id, ranking);

-- 清理旧的全局NPC（格式 npc_hero_00001），新NPC格式为 npc_hero_s{serverId}_{序号}
DELETE FROM hero_rank WHERE user_id LIKE 'npc_hero_%' AND user_id NOT LIKE 'npc_hero_s%';

-- ========== alliance ==========
ALTER TABLE alliance ADD COLUMN server_id INT NOT NULL DEFAULT 1 COMMENT '区服ID';
ALTER TABLE alliance ADD INDEX idx_server (server_id);

-- ========== chat_message ==========
ALTER TABLE chat_message ADD COLUMN server_id INT NOT NULL DEFAULT 1 COMMENT '区服ID';
ALTER TABLE chat_message ADD INDEX idx_server_channel (server_id, channel);

-- ========== market_listing ==========
ALTER TABLE market_listing ADD COLUMN server_id INT NOT NULL DEFAULT 1 COMMENT '区服ID';
ALTER TABLE market_listing ADD INDEX idx_server_status (server_id, status);

-- ========== supply_transport ==========
ALTER TABLE supply_transport ADD COLUMN server_id INT NOT NULL DEFAULT 1 COMMENT '区服ID';
ALTER TABLE supply_transport ADD INDEX idx_server_status (server_id, status);

-- ========== nation_war_city_owner ==========
ALTER TABLE nation_war_city_owner ADD COLUMN server_id INT NOT NULL DEFAULT 1 COMMENT '区服ID';
-- 原有 UNIQUE KEY 可能只在 city_id 上，需改为 (city_id, server_id)
-- ALTER TABLE nation_war_city_owner DROP PRIMARY KEY, ADD PRIMARY KEY (city_id, server_id);

-- ========== player_nation ==========
ALTER TABLE player_nation ADD COLUMN server_id INT NOT NULL DEFAULT 1 COMMENT '区服ID';
ALTER TABLE player_nation ADD INDEX idx_server_nation (server_id, nation);

-- ========== cornucopia_period ==========
ALTER TABLE cornucopia_period ADD COLUMN server_id INT NOT NULL DEFAULT 1 COMMENT '区服ID';
ALTER TABLE cornucopia_period ADD INDEX idx_server_status (server_id, status);

-- ========== 注意 ==========
-- 执行此脚本后，需要通过管理员接口（或重新进入区服）触发各区服的NPC重新初始化
-- POST /server/admin/create 或进入已有区服时会自动调用 ensureNpcExists(serverId)
