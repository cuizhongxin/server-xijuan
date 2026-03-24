-- ============================================
-- 英雄榜NPC模拟数据 - 10000个NPC
-- NPC的user_id格式: npc_hero_XXXX
-- 战力从高到低递减，名字使用三国风格随机组合
-- ============================================

DROP PROCEDURE IF EXISTS generate_hero_rank_npcs;

DELIMITER $$
CREATE PROCEDURE generate_hero_rank_npcs()
BEGIN
    DECLARE i INT DEFAULT 1;
    DECLARE npc_id VARCHAR(64);
    DECLARE npc_name VARCHAR(64);
    DECLARE npc_level INT;
    DECLARE npc_power INT;
    DECLARE npc_fame BIGINT;
    DECLARE npc_rank VARCHAR(16);
    DECLARE surname_idx INT;
    DECLARE given_idx INT;
    DECLARE title_idx INT;

    -- 三国风格姓氏 (30个)
    SET @surnames = '司马,诸葛,公孙,夏侯,上官,慕容,皇甫,独孤,欧阳,令狐,赵,钱,孙,李,周,吴,郑,王,冯,陈,褚,卫,蒋,沈,韩,杨,朱,秦,许,何';
    -- 三国风格名字 (40个)
    SET @givens = '云龙,飞虎,破军,铁骑,烈焰,苍狼,青龙,白虎,朱雀,玄武,天策,风雷,雷霆,寒冰,烈日,星辰,紫电,银河,金戈,铁马,无双,霸王,枭雄,英豪,战神,虎将,猛士,锐卒,精骑,劲弩,铁壁,坚盾,长枪,利剑,弯弓,神弩,飞羽,疾风,落雷,奔雷';
    -- 称号 (20个)
    SET @titles = '武安君,大将军,骠骑将军,镇北将军,征西将军,安东将军,讨逆将军,平南将军,破虏将军,定远将军,奋威将军,昭武将军,建威将军,振武将军,扬威将军,宣威将军,明威将军,广威将军,忠武将军,壮武将军';

    -- 清理现有NPC数据
    DELETE FROM hero_rank WHERE user_id LIKE 'npc_hero_%';

    WHILE i <= 10000 DO
        SET npc_id = CONCAT('npc_hero_', LPAD(i, 5, '0'));

        -- 姓氏随机 (1-30)
        SET surname_idx = FLOOR(1 + RAND() * 30);
        -- 名字随机 (1-40)
        SET given_idx = FLOOR(1 + RAND() * 40);
        -- 称号随机 (1-20)
        SET title_idx = FLOOR(1 + RAND() * 20);

        SET npc_name = CONCAT(
            SUBSTRING_INDEX(SUBSTRING_INDEX(@surnames, ',', surname_idx), ',', -1),
            SUBSTRING_INDEX(SUBSTRING_INDEX(@givens, ',', given_idx), ',', -1)
        );

        -- 第1名NPC最强，逐渐递减，加入一些随机波动
        -- 等级: 前100名100级，101-500名80-99级，501-2000名60-79级，2001-5000名40-59级，5001-10000名1-39级
        IF i <= 100 THEN
            SET npc_level = 100;
            SET npc_power = 50000 - (i - 1) * 200 + FLOOR(RAND() * 100);
            SET npc_fame = 2000000 - i * 10000 + FLOOR(RAND() * 5000);
            SET npc_rank = '王';
        ELSEIF i <= 500 THEN
            SET npc_level = 80 + FLOOR(RAND() * 20);
            SET npc_power = 30000 - (i - 100) * 40 + FLOOR(RAND() * 200);
            SET npc_fame = 1000000 - (i - 100) * 1500 + FLOOR(RAND() * 3000);
            IF npc_fame >= 1000000 THEN SET npc_rank = '王';
            ELSEIF npc_fame >= 500000 THEN SET npc_rank = '公';
            ELSE SET npc_rank = '侯'; END IF;
        ELSEIF i <= 2000 THEN
            SET npc_level = 60 + FLOOR(RAND() * 20);
            SET npc_power = 14000 - (i - 500) * 5 + FLOOR(RAND() * 300);
            SET npc_fame = 400000 - (i - 500) * 150 + FLOOR(RAND() * 2000);
            IF npc_fame >= 200000 THEN SET npc_rank = '侯';
            ELSEIF npc_fame >= 100000 THEN SET npc_rank = '伯';
            ELSE SET npc_rank = '子'; END IF;
        ELSEIF i <= 5000 THEN
            SET npc_level = 40 + FLOOR(RAND() * 20);
            SET npc_power = 6500 - (i - 2000) * 1 + FLOOR(RAND() * 200);
            SET npc_fame = 150000 - (i - 2000) * 30 + FLOOR(RAND() * 1000);
            IF npc_fame >= 100000 THEN SET npc_rank = '伯';
            ELSEIF npc_fame >= 50000 THEN SET npc_rank = '子';
            ELSEIF npc_fame >= 20000 THEN SET npc_rank = '男';
            ELSE SET npc_rank = '士人'; END IF;
        ELSE
            SET npc_level = 1 + FLOOR(RAND() * 39);
            SET npc_power = 3500 - (i - 5000) * 0.5 + FLOOR(RAND() * 300);
            SET npc_fame = 50000 - (i - 5000) * 8 + FLOOR(RAND() * 500);
            IF npc_fame >= 20000 THEN SET npc_rank = '男';
            ELSEIF npc_fame >= 10000 THEN SET npc_rank = '士人';
            ELSEIF npc_fame >= 5000 THEN SET npc_rank = '平民';
            ELSE SET npc_rank = '白身'; END IF;
        END IF;

        -- 确保power和fame不为负
        IF npc_power < 100 THEN SET npc_power = 100 + FLOOR(RAND() * 200); END IF;
        IF npc_fame < 0 THEN SET npc_fame = FLOOR(RAND() * 1000); END IF;

        INSERT INTO hero_rank (user_id, user_name, level, power, fame, rank_name, ranking,
                               today_challenge, today_wins, today_purchased, last_reset_date, update_time)
        VALUES (npc_id, npc_name, npc_level, npc_power, npc_fame, npc_rank, i,
                0, 0, 0, DATE_FORMAT(NOW(), '%Y%m%d'), UNIX_TIMESTAMP() * 1000);

        SET i = i + 1;
    END WHILE;

    -- 更新排名
    SET @r = 0;
    UPDATE hero_rank SET ranking = (@r := @r + 1) ORDER BY power DESC;

END$$
DELIMITER ;

CALL generate_hero_rank_npcs();
DROP PROCEDURE IF EXISTS generate_hero_rank_npcs;
