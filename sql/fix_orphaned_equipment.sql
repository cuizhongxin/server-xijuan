-- 修复已解雇武将的孤儿装备：将指向已删除武将的装备恢复为未装备状态
UPDATE equipment e
LEFT JOIN general g ON e.equipped_general_id = g.id
SET e.equipped = false, e.equipped_general_id = NULL
WHERE e.equipped = true
  AND e.equipped_general_id IS NOT NULL
  AND g.id IS NULL;
