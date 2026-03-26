-- ═══════════════════════════════════════════════════════════════
-- 商城数据对齐 APK PropShow.json 道具体系（完整重建版）
-- 请按顺序全部执行
-- ═══════════════════════════════════════════════════════════════

-- 1. 确保 item 表有 icon 和 description 列
ALTER TABLE `item` ADD COLUMN IF NOT EXISTS `icon` varchar(128) DEFAULT '' COMMENT '道具图标文件名';
ALTER TABLE `item` ADD COLUMN IF NOT EXISTS `description` varchar(256) DEFAULT '' COMMENT '道具描述';

-- 2. 彻底清空旧数据（用 DELETE 避免 TRUNCATE 被外键阻止）
DELETE FROM `shop`;
ALTER TABLE `shop` AUTO_INCREMENT = 1;
DELETE FROM `item`;

-- 3. 插入 APK 道具数据（item_id 对齐 PropShow.json）
INSERT INTO `item` (`item_id`, `item_name`, `quality`, `icon`, `description`) VALUES
(11001, '初级声望符', 3, '11001.jpg', '使用后直接获得100点声望'),
(11002, '高级声望符', 4, '11002.jpg', '使用后直接获得500点声望'),
(11012, '银锭',       3, '11012.jpg', '使用后获得10000白银'),
(11013, '银票',       4, '11013.jpg', '使用后获得50000白银'),
(11026, '将魂符',     5, '11026.jpg', '使用后直接获得50武魂'),
(11101, '精力丹',     3, '11101.jpg', '使用后补充5点精力'),
(11104, '招财符',     3, '11104.jpg', '可在聚宝盆中购买幸运号码，有机会赢取大量绑金奖励'),
(14001, '1级强化石',  2, '14001.jpg', '用于第1级装备强化，可精炼合成2级强化石'),
(14004, '4级强化石',  3, '14004.jpg', '用于第4级装备强化，可精炼合成5级强化石'),
(14006, '6级强化石',  4, '14006.jpg', '用于第6级装备强化，可精炼合成7级强化石'),
(15011, '初级招贤令', 2, '15011.jpg', '可在"将领招募"界面发布招贤令，获得白色或者绿色将领'),
(15012, '中级招贤令', 3, '15012.jpg', '可在"将领招募"界面发布招贤令，获得绿色或者蓝色将领'),
(15013, '高级招贤令', 4, '15013.jpg', '可在"将领招募"界面发布招贤令，获得紫色或者橙色将领'),
(15042, '特训符',     3, '15042.jpg', '可在"将领训练"界面让正在训练的将领直接增长经验'),
(15051, '虎符',       3, '15051.jpg', '使用虎符可对战役进行扫荡'),
(15052, '军需令',     3, '15052.jpg', '用于刷新护送的军资等级');

-- 4. 插入商城商品（对齐 APK 道具 ID + 用户指定价格）
-- 新品热卖: 6 条 | 常用道具: 10 条 | 活动专区: 0 条
INSERT INTO `shop` (`name`, `price`, `desc`, `currency`, `icon`, `classify`, `quality`, `item_id`) VALUES
('招财符',     10,  '可在聚宝盆中购买幸运号码',           'gold', '11104.jpg', 'new_products', 3, 11104),
('特训符',      5,  '让正在训练的将领直接增长经验',         'gold', '15042.jpg', 'new_products', 3, 15042),
('高级声望符', 30,  '使用后直接获得500点声望',             'gold', '11002.jpg', 'new_products', 4, 11002),
('将魂符',    500,  '使用后直接获得50武魂',               'gold', '11026.jpg', 'new_products', 5, 11026),
('精力丹',     20,  '使用后补充5点精力',                  'gold', '11101.jpg', 'new_products', 3, 11101),
('虎符',        5,  '使用虎符可对战役进行扫荡',            'gold', '15051.jpg', 'new_products', 3, 15051),
('初级声望符', 10,  '使用后直接获得100点声望',             'gold', '11001.jpg', 'common_props', 3, 11001),
('银锭',       10,  '使用后获得10000白银',                'gold', '11012.jpg', 'common_props', 3, 11012),
('银票',       50,  '使用后获得50000白银',                'gold', '11013.jpg', 'common_props', 4, 11013),
('1级强化石',   1,  '用于第1级装备强化',                   'gold', '14001.jpg', 'common_props', 2, 14001),
('4级强化石',  20,  '用于第4级装备强化',                   'gold', '14004.jpg', 'common_props', 3, 14004),
('6级强化石', 150,  '用于第6级装备强化',                   'gold', '14006.jpg', 'common_props', 4, 14006),
('初级招贤令',  1,  '获得白色或者绿色将领',                'gold', '15011.jpg', 'common_props', 2, 15011),
('中级招贤令', 15,  '获得绿色或者蓝色将领',                'gold', '15012.jpg', 'common_props', 3, 15012),
('高级招贤令',200,  '获得紫色或者橙色将领',                'gold', '15013.jpg', 'common_props', 4, 15013),
('军需令',     10,  '用于刷新护送的军资等级',              'gold', '15052.jpg', 'common_props', 3, 15052);

-- 5. 验证结果（执行后核对）
SELECT '=== 验证 shop 表 ===' AS info;
SELECT classify, COUNT(*) AS cnt FROM `shop` GROUP BY classify ORDER BY classify;
SELECT '=== 验证 item 表 ===' AS info;
SELECT COUNT(*) AS total_items FROM `item`;
SELECT '=== 期望结果 ===' AS info;
SELECT 'new_products=6, common_props=10, active=0, item总数=16' AS expected;
