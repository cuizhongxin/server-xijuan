-- ============================================
-- equipment_pre 装备图片同步 (APK equipShow_cfg)
-- 为每条 equipment_pre 记录设置对应的 APK 装备图片
-- ============================================

-- 确保 icon_url 列存在
ALTER TABLE equipment_pre ADD COLUMN IF NOT EXISTS icon_url VARCHAR(128) DEFAULT NULL;

-- ========== Lv1 新手套 → 黑铁 (22001-22006) ==========
UPDATE equipment_pre SET icon_url = '22001.jpg' WHERE id = 1;   -- 新手长剑 → 黑铁剑
UPDATE equipment_pre SET icon_url = '22002.jpg' WHERE id = 2;   -- 新手戒指 → 黑铁戒
UPDATE equipment_pre SET icon_url = '22004.jpg' WHERE id = 3;   -- 新手布甲 → 黑铁锴
UPDATE equipment_pre SET icon_url = '22003.jpg' WHERE id = 4;   -- 新手项链 → 黑铁项链
UPDATE equipment_pre SET icon_url = '22005.jpg' WHERE id = 5;   -- 新手布帽 → 黑铁盔
UPDATE equipment_pre SET icon_url = '22006.jpg' WHERE id = 6;   -- 新手布鞋 → 黑铁靴

-- ========== Lv20 宣武套 → 宣武 (23021-23026) ==========
UPDATE equipment_pre SET icon_url = '23021.jpg' WHERE id = 7;   -- 宣武长剑 → 宣武剑
UPDATE equipment_pre SET icon_url = '23022.jpg' WHERE id = 8;   -- 宣武戒指 → 宣武戒指
UPDATE equipment_pre SET icon_url = '23024.jpg' WHERE id = 9;   -- 宣武战甲 → 宣武铠
UPDATE equipment_pre SET icon_url = '23023.jpg' WHERE id = 10;  -- 宣武项链 → 宣武项链
UPDATE equipment_pre SET icon_url = '23025.jpg' WHERE id = 11;  -- 宣武头盔 → 宣武盔
UPDATE equipment_pre SET icon_url = '23026.jpg' WHERE id = 12;  -- 宣武战靴 → 宣武靴

-- ========== Lv40 折冲套 → 折冲 (23031-23036) ==========
UPDATE equipment_pre SET icon_url = '23031.jpg' WHERE id = 13;  -- 折冲长剑 → 折冲剑
UPDATE equipment_pre SET icon_url = '23032.jpg' WHERE id = 14;  -- 折冲戒指 → 折冲戒指
UPDATE equipment_pre SET icon_url = '23034.jpg' WHERE id = 15;  -- 折冲战甲 → 折冲锴
UPDATE equipment_pre SET icon_url = '23033.jpg' WHERE id = 16;  -- 折冲项链 → 折冲项链
UPDATE equipment_pre SET icon_url = '23035.jpg' WHERE id = 17;  -- 折冲头盔 → 折冲盔
UPDATE equipment_pre SET icon_url = '23036.jpg' WHERE id = 18;  -- 折冲战靴 → 折冲靴

-- ========== Lv40 陷阵套 → 陷阵 (23061-23066) ==========
UPDATE equipment_pre SET icon_url = '23061.jpg' WHERE id = 19;  -- 陷阵长枪 → 陷阵之刃
UPDATE equipment_pre SET icon_url = '23062.jpg' WHERE id = 20;  -- 陷阵戒指 → 陷阵之戒
UPDATE equipment_pre SET icon_url = '23064.jpg' WHERE id = 21;  -- 陷阵重甲 → 陷阵之锴
UPDATE equipment_pre SET icon_url = '23063.jpg' WHERE id = 22;  -- 陷阵项链 → 陷阵项链
UPDATE equipment_pre SET icon_url = '23065.jpg' WHERE id = 23;  -- 陷阵头盔 → 陷阵之盔
UPDATE equipment_pre SET icon_url = '23066.jpg' WHERE id = 24;  -- 陷阵战靴 → 陷阵之靴

-- ========== Lv40 鹰扬套 → 鹰扬 (23121-23126) ==========
UPDATE equipment_pre SET icon_url = '23121.jpg' WHERE id = 25;  -- 鹰扬战刀 → 鹰扬刀
UPDATE equipment_pre SET icon_url = '23122.jpg' WHERE id = 26;  -- 鹰扬戒指 → 鹰扬戒
UPDATE equipment_pre SET icon_url = '23124.jpg' WHERE id = 27;  -- 鹰扬护甲 → 鹰扬锴
UPDATE equipment_pre SET icon_url = '23123.jpg' WHERE id = 28;  -- 鹰扬项链 → 鹰扬项链
UPDATE equipment_pre SET icon_url = '23125.jpg' WHERE id = 29;  -- 鹰扬头盔 → 鹰扬盔
UPDATE equipment_pre SET icon_url = '23126.jpg' WHERE id = 30;  -- 鹰扬战靴 → 鹰扬靴

-- ========== Lv50 狂战套 → 狂战 (23071-23076) ==========
UPDATE equipment_pre SET icon_url = '23071.jpg' WHERE id = 31;  -- 狂战巨斧 → 狂战刀
UPDATE equipment_pre SET icon_url = '23072.jpg' WHERE id = 32;  -- 狂战戒指 → 狂战之戒
UPDATE equipment_pre SET icon_url = '23074.jpg' WHERE id = 33;  -- 狂战重甲 → 狂战甲
UPDATE equipment_pre SET icon_url = '23073.jpg' WHERE id = 34;  -- 狂战项链 → 狂战项链
UPDATE equipment_pre SET icon_url = '23075.jpg' WHERE id = 35;  -- 狂战头盔 → 狂战盔
UPDATE equipment_pre SET icon_url = '23076.jpg' WHERE id = 36;  -- 狂战战靴 → 狂战靴

-- ========== Lv60 天狼套 → 天狼 (23081-23086) ==========
UPDATE equipment_pre SET icon_url = '23081.jpg' WHERE id = 37;  -- 天狼战刃 → 天狼剑
UPDATE equipment_pre SET icon_url = '23082.jpg' WHERE id = 38;  -- 天狼戒指 → 天狼戒
UPDATE equipment_pre SET icon_url = '23084.jpg' WHERE id = 39;  -- 天狼战甲 → 天狼甲
UPDATE equipment_pre SET icon_url = '23083.jpg' WHERE id = 40;  -- 天狼项链 → 天狼项链
UPDATE equipment_pre SET icon_url = '23085.jpg' WHERE id = 41;  -- 天狼头盔 → 天狼盔
UPDATE equipment_pre SET icon_url = '23086.jpg' WHERE id = 42;  -- 天狼战靴 → 天狼靴

-- ========== Lv60 玄铁套 → 亮银 (22031-22036, 绿色散件Lv60) ==========
UPDATE equipment_pre SET icon_url = '22031.jpg' WHERE id = 43;  -- 玄铁重剑 → 亮银剑
UPDATE equipment_pre SET icon_url = '22032.jpg' WHERE id = 44;  -- 玄铁戒指 → 亮银戒
UPDATE equipment_pre SET icon_url = '22034.jpg' WHERE id = 45;  -- 玄铁战甲 → 亮银锴
UPDATE equipment_pre SET icon_url = '22033.jpg' WHERE id = 46;  -- 玄铁项链 → 亮银项链
UPDATE equipment_pre SET icon_url = '22035.jpg' WHERE id = 47;  -- 玄铁头盔 → 亮银盔
UPDATE equipment_pre SET icon_url = '22036.jpg' WHERE id = 48;  -- 玄铁战靴 → 亮银靴

-- ========== Lv60 虎啸套 → 虎啸 (24131-24136) ==========
UPDATE equipment_pre SET icon_url = '24131.jpg' WHERE id = 49;  -- 虎啸战刀 → 虎啸之剑
UPDATE equipment_pre SET icon_url = '24132.jpg' WHERE id = 50;  -- 虎啸戒指 → 虎啸之戒
UPDATE equipment_pre SET icon_url = '24134.jpg' WHERE id = 51;  -- 虎啸护甲 → 虎啸之锴
UPDATE equipment_pre SET icon_url = '24133.jpg' WHERE id = 52;  -- 虎啸项链 → 虎啸项链
UPDATE equipment_pre SET icon_url = '24135.jpg' WHERE id = 53;  -- 虎啸头盔 → 虎啸之盔
UPDATE equipment_pre SET icon_url = '24136.jpg' WHERE id = 54;  -- 虎啸战靴 → 虎啸之靴

-- ========== Lv60 熊王套 → 骁勇 (23041-23046, 蓝色Lv60) ==========
UPDATE equipment_pre SET icon_url = '23041.jpg' WHERE id = 55;  -- 熊王巨锤 → 骁勇长枪
UPDATE equipment_pre SET icon_url = '23042.jpg' WHERE id = 56;  -- 熊王戒指 → 骁勇戒
UPDATE equipment_pre SET icon_url = '23044.jpg' WHERE id = 57;  -- 熊王重甲 → 骁勇之甲
UPDATE equipment_pre SET icon_url = '23043.jpg' WHERE id = 58;  -- 熊王项链 → 骁勇项链
UPDATE equipment_pre SET icon_url = '23045.jpg' WHERE id = 59;  -- 熊王头盔 → 骁勇盔
UPDATE equipment_pre SET icon_url = '23046.jpg' WHERE id = 60;  -- 熊王战靴 → 骁勇靴

-- ========== Lv70 天诛套 → 天诛 (24151-24156) ==========
UPDATE equipment_pre SET icon_url = '24151.jpg' WHERE id = 61;  -- 天诛神剑 → 天诛之刃
UPDATE equipment_pre SET icon_url = '24152.jpg' WHERE id = 62;  -- 天诛戒指 → 天诛之戒
UPDATE equipment_pre SET icon_url = '24154.jpg' WHERE id = 63;  -- 天诛战甲 → 天诛之甲
UPDATE equipment_pre SET icon_url = '24153.jpg' WHERE id = 64;  -- 天诛项链 → 天诛项链
UPDATE equipment_pre SET icon_url = '24155.jpg' WHERE id = 65;  -- 天诛头盔 → 天诛之盔
UPDATE equipment_pre SET icon_url = '24156.jpg' WHERE id = 66;  -- 天诛战靴 → 天诛之靴

-- ========== Lv70 地煞套 → 地煞 (24141-24146) ==========
UPDATE equipment_pre SET icon_url = '24141.jpg' WHERE id = 67;  -- 地煞战刀 → 地煞之枪
UPDATE equipment_pre SET icon_url = '24142.jpg' WHERE id = 68;  -- 地煞戒指 → 地煞之戒
UPDATE equipment_pre SET icon_url = '24144.jpg' WHERE id = 69;  -- 地煞护甲 → 地煞之锴
UPDATE equipment_pre SET icon_url = '24143.jpg' WHERE id = 70;  -- 地煞项链 → 地煞项链
UPDATE equipment_pre SET icon_url = '24145.jpg' WHERE id = 71;  -- 地煞头盔 → 地煞之盔
UPDATE equipment_pre SET icon_url = '24146.jpg' WHERE id = 72;  -- 地煞战靴 → 地煞之靴

-- ========== Lv70 幽冥套 → 幽冥 (24161-24166) ==========
UPDATE equipment_pre SET icon_url = '24161.jpg' WHERE id = 73;  -- 幽冥战刃 → 幽冥之剑
UPDATE equipment_pre SET icon_url = '24162.jpg' WHERE id = 74;  -- 幽冥戒指 → 幽冥之戒
UPDATE equipment_pre SET icon_url = '24164.jpg' WHERE id = 75;  -- 幽冥战甲 → 幽冥之甲
UPDATE equipment_pre SET icon_url = '24163.jpg' WHERE id = 76;  -- 幽冥项链 → 幽冥项链
UPDATE equipment_pre SET icon_url = '24165.jpg' WHERE id = 77;  -- 幽冥头盔 → 幽冥之盔
UPDATE equipment_pre SET icon_url = '24166.jpg' WHERE id = 78;  -- 幽冥战靴 → 幽冥之靴

-- ========== Lv80 雄狮套 → 龙威 (24101-24106, 紫色) ==========
UPDATE equipment_pre SET icon_url = '24101.jpg' WHERE id = 79;  -- 雄狮战刃 → 龙威之剑
UPDATE equipment_pre SET icon_url = '24102.jpg' WHERE id = 80;  -- 雄狮戒指 → 龙威之戒
UPDATE equipment_pre SET icon_url = '24104.jpg' WHERE id = 81;  -- 雄狮战甲 → 龙威之锴
UPDATE equipment_pre SET icon_url = '24103.jpg' WHERE id = 82;  -- 雄狮项链 → 龙威项链
UPDATE equipment_pre SET icon_url = '24105.jpg' WHERE id = 83;  -- 雄狮头盔 → 龙威之盔
UPDATE equipment_pre SET icon_url = '24106.jpg' WHERE id = 84;  -- 雄狮战靴 → 龙威之靴

-- ========== Lv80 凤鸣套 → 征戎 (23091-23096, 蓝色Lv80) ==========
UPDATE equipment_pre SET icon_url = '23091.jpg' WHERE id = 85;  -- 凤鸣神剑 → 征戎之刃
UPDATE equipment_pre SET icon_url = '23092.jpg' WHERE id = 86;  -- 凤鸣戒指 → 征戎之戒
UPDATE equipment_pre SET icon_url = '23094.jpg' WHERE id = 87;  -- 凤鸣护甲 → 征戎甲
UPDATE equipment_pre SET icon_url = '23093.jpg' WHERE id = 88;  -- 凤鸣项链 → 征戎项链
UPDATE equipment_pre SET icon_url = '23095.jpg' WHERE id = 89;  -- 凤鸣头盔 → 征戎盔
UPDATE equipment_pre SET icon_url = '23096.jpg' WHERE id = 90;  -- 凤鸣战靴 → 征戎靴

-- ========== Lv80 精金套 → 百炼 (22041-22046, 绿色散件Lv80) ==========
UPDATE equipment_pre SET icon_url = '22041.jpg' WHERE id = 91;  -- 精金重剑 → 百炼枪
UPDATE equipment_pre SET icon_url = '22042.jpg' WHERE id = 92;  -- 精金戒指 → 百炼戒
UPDATE equipment_pre SET icon_url = '22044.jpg' WHERE id = 93;  -- 精金战甲 → 百炼锴
UPDATE equipment_pre SET icon_url = '22043.jpg' WHERE id = 94;  -- 精金项链 → 百炼项链
UPDATE equipment_pre SET icon_url = '22045.jpg' WHERE id = 95;  -- 精金头盔 → 百炼盔
UPDATE equipment_pre SET icon_url = '22046.jpg' WHERE id = 96;  -- 精金战靴 → 百炼靴

-- ========== Lv90 圣象套 → 破军 (24091-24096, 紫色) ==========
UPDATE equipment_pre SET icon_url = '24091.jpg' WHERE id = 97;  -- 圣象神兵 → 破军之戟
UPDATE equipment_pre SET icon_url = '24092.jpg' WHERE id = 98;  -- 圣象戒指 → 破军之戒
UPDATE equipment_pre SET icon_url = '24094.jpg' WHERE id = 99;  -- 圣象战甲 → 破军之锴
UPDATE equipment_pre SET icon_url = '24093.jpg' WHERE id = 100; -- 圣象项链 → 破军项链
UPDATE equipment_pre SET icon_url = '24095.jpg' WHERE id = 101; -- 圣象头盔 → 破军之盔
UPDATE equipment_pre SET icon_url = '24096.jpg' WHERE id = 102; -- 圣象战靴 → 破军之靴

-- ========== Lv100 龙吟套 → 诛邪 (25181-25186, 橙色) ==========
UPDATE equipment_pre SET icon_url = '25181.jpg' WHERE id = 103; -- 龙吟神剑 → 诛邪之刃
UPDATE equipment_pre SET icon_url = '25182.jpg' WHERE id = 104; -- 龙吟戒指 → 诛邪之戒
UPDATE equipment_pre SET icon_url = '25184.jpg' WHERE id = 105; -- 龙吟护甲 → 诛邪之甲
UPDATE equipment_pre SET icon_url = '25183.jpg' WHERE id = 106; -- 龙吟项链 → 诛邪项链
UPDATE equipment_pre SET icon_url = '25185.jpg' WHERE id = 107; -- 龙吟头盔 → 诛邪之盔
UPDATE equipment_pre SET icon_url = '25186.jpg' WHERE id = 108; -- 龙吟战靴 → 诛邪之靴

-- ========== Lv100 玄武套 → 战神 (25111-25116, 橙色) ==========
UPDATE equipment_pre SET icon_url = '25111.jpg' WHERE id = 109; -- 玄武战刃 → 战神之戟
UPDATE equipment_pre SET icon_url = '25112.jpg' WHERE id = 110; -- 玄武戒指 → 战神之戒
UPDATE equipment_pre SET icon_url = '25114.jpg' WHERE id = 111; -- 玄武战甲 → 战神之甲
UPDATE equipment_pre SET icon_url = '25113.jpg' WHERE id = 112; -- 玄武项链 → 战神项链
UPDATE equipment_pre SET icon_url = '25115.jpg' WHERE id = 113; -- 玄武头盔 → 战神之盔
UPDATE equipment_pre SET icon_url = '25116.jpg' WHERE id = 114; -- 玄武战靴 → 战神之靴

-- ========== Lv100 秘银套 → 破俘 (23051-23056, 蓝色Lv80) ==========
UPDATE equipment_pre SET icon_url = '23051.jpg' WHERE id = 115; -- 秘银神剑 → 破俘枪
UPDATE equipment_pre SET icon_url = '23052.jpg' WHERE id = 116; -- 秘银戒指 → 破俘戒
UPDATE equipment_pre SET icon_url = '23054.jpg' WHERE id = 117; -- 秘银战甲 → 破俘甲
UPDATE equipment_pre SET icon_url = '23053.jpg' WHERE id = 118; -- 秘银项链 → 破俘项链
UPDATE equipment_pre SET icon_url = '23055.jpg' WHERE id = 119; -- 秘银头盔 → 破俘盔
UPDATE equipment_pre SET icon_url = '23056.jpg' WHERE id = 120; -- 秘银战靴 → 破俘靴
