-- shop 商品表
CREATE TABLE `shop` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name` varchar(512) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT '商品名称',
  `price` int(10) NOT NULL DEFAULT '0' COMMENT '价格',
  `desc` varchar(256) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT '商品描述',
  `currency` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT '货币',
  `icon` varchar(256) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT '图标',
  `classify` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT '商品分类',
  `quality` tinyint(4) NOT NULL DEFAULT '1' COMMENT '品质 1白色 2绿色 3蓝色 4红色 5紫色 6橙色',
  `item_id` bigint(20) NOT NULL DEFAULT '0' COMMENT '道具id，关联item表',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- item 道具表（item_id 对齐 APK PropShow.json）
CREATE TABLE `item` (
  `item_id` int(11) NOT NULL COMMENT '道具ID（APK PropShow ID）',
  `item_name` varchar(50) NOT NULL COMMENT '道具名称',
  `quality` tinyint(4) NOT NULL COMMENT '道具品质，1~6：白、绿、蓝、紫、橙、红',
  `icon` varchar(128) NOT NULL DEFAULT '' COMMENT '道具图标文件名（如 11001.jpg）',
  `description` varchar(256) NOT NULL DEFAULT '' COMMENT '道具描述',
  PRIMARY KEY (`item_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='道具表';
