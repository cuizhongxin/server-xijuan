-- ============================================
-- VIP系统
-- ============================================

-- VIP礼包领取记录
CREATE TABLE IF NOT EXISTS vip_gift_claim (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id VARCHAR(64) NOT NULL,
    vip_level INT NOT NULL,
    claim_time BIGINT NOT NULL,
    UNIQUE KEY uk_user_level (user_id, vip_level)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================
-- VIP相关道具（已存在于item表，无需重复插入）
-- ============================================
-- 11091: 鹰扬宝箱  icon=15216.jpg  使用后可随机获得一件鹰扬装备
-- 11092: 虎啸宝箱  icon=15217.jpg  使用后可随机获得一件虎啸装备
-- 11093: 宣武宝箱  icon=15206.jpg  使用后可随机获得一件宣武装备
-- 11094: 天狼宝箱  icon=15212.jpg  使用后可随机获得一件天狼装备
-- 11095: 破军宝箱  icon=15213.jpg  使用后可随机获得一件破军装备
-- 16001: 指定鹰扬装 icon=15216.jpg  使用后可以选择一件鹰扬套装部件
-- 16002: 指定虎啸装 icon=15217.jpg  使用后可以选择一件虎啸套装部件
-- 16003: 指定天狼装 icon=15212.jpg  使用后可以选择一件天狼套装部件

-- ============================================
-- 更新VIP等级阈值（元）
-- VIP1=6, VIP2=30, VIP3=98, VIP4=198,
-- VIP5=328, VIP6=648, VIP7=998, VIP8=1998,
-- VIP9=6000, VIP10=20000
-- （已在 UserResourceService.updateVipLevel 中硬编码）
-- ============================================
