package com.tencent.wxcloudrun.service.formation;

import com.tencent.wxcloudrun.exception.BusinessException;
import com.tencent.wxcloudrun.model.Equipment;
import com.tencent.wxcloudrun.model.Formation;
import com.tencent.wxcloudrun.model.General;
import com.tencent.wxcloudrun.repository.EquipmentRepository;
import com.tencent.wxcloudrun.repository.FormationRepository;
import com.tencent.wxcloudrun.repository.GeneralRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 阵型服务
 */
@Service
public class FormationService {
    
    private static final Logger logger = LoggerFactory.getLogger(FormationService.class);
    
    @Autowired
    private FormationRepository formationRepository;
    
    @Autowired
    private GeneralRepository generalRepository;
    
    @Autowired
    private EquipmentRepository equipmentRepository;
    
    /**
     * 获取用户阵型
     */
    public Formation getFormation(String odUserId) {
        Formation formation = formationRepository.findByUserId(odUserId);
        if (formation == null) {
            formation = formationRepository.initFormation(odUserId);
        }
        return formation;
    }
    
    /**
     * 获取阵型详情（包含武将完整信息）
     */
    public Map<String, Object> getFormationDetail(String odUserId) {
        Formation formation = getFormation(odUserId);
        
        // 获取阵型中的武将详情
        List<Map<String, Object>> slotDetails = new ArrayList<>();
        for (Formation.FormationSlot slot : formation.getSlots()) {
            Map<String, Object> slotInfo = new HashMap<>();
            slotInfo.put("index", slot.getIndex());
            
            if (slot.getGeneralId() != null) {
                General general = generalRepository.findById(slot.getGeneralId());
                if (general != null) {
                    slotInfo.put("generalId", general.getId());
                    slotInfo.put("generalName", general.getName());
                    slotInfo.put("quality", general.getQualityName());
                    slotInfo.put("level", general.getLevel());
                    slotInfo.put("avatar", general.getAvatar());
                    slotInfo.put("mobility", general.getAttrMobility() != null ? general.getAttrMobility() : 0);
                    slotInfo.put("attack", general.getAttrAttack() != null ? general.getAttrAttack() : 0);
                    slotInfo.put("defense", general.getAttrDefense() != null ? general.getAttrDefense() : 0);
                    slotInfo.put("power", general.getAttrValor() != null ? general.getAttrValor() : 0);
                    slotInfo.put("troopType", general.getTroopType());
                    
                    // 士兵信息
                    slotInfo.put("soldierCount", general.getSoldierCount() != null ? general.getSoldierCount() : 0);
                    slotInfo.put("maxSoldierCount", general.getSoldierMaxCount() != null ? general.getSoldierMaxCount() : 0);
                    slotInfo.put("soldierTypeName", general.getTroopType());
                    slotInfo.put("soldierRank", general.getSoldierRank() != null ? general.getSoldierRank() : 1);
                    int soldierHp = 100;
                    slotInfo.put("soldierHp", soldierHp);
                    
                    // 装备加成
                    Map<String, Integer> equipBonus = calculateEquipmentBonus(general.getId());
                    slotInfo.put("equipAttack", equipBonus.getOrDefault("attack", 0));
                    slotInfo.put("equipDefense", equipBonus.getOrDefault("defense", 0));
                    slotInfo.put("equipHp", equipBonus.getOrDefault("hp", 0));
                    slotInfo.put("equipMobility", equipBonus.getOrDefault("mobility", 0));
                    
                    // 计算综合HP
                    int basePower = general.getAttrValor() != null ? general.getAttrValor() : 500;
                    int soldierCount = general.getSoldierCount() != null ? general.getSoldierCount() : 0;
                    int soldierHpVal = (Integer) slotInfo.get("soldierHp");
                    int totalHp = basePower * 10 + (soldierCount * soldierHpVal) / 10;
                    slotInfo.put("hp", totalHp);
                    slotInfo.put("maxHp", totalHp);
                    
                    slotInfo.put("empty", false);
                } else {
                    slotInfo.put("empty", true);
                }
            } else {
                slotInfo.put("empty", true);
            }
            
            slotDetails.add(slotInfo);
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("id", formation.getId());
        result.put("name", formation.getName());
        result.put("slots", slotDetails);
        result.put("active", formation.getActive());
        
        return result;
    }
    
    /**
     * 设置阵型槽位
     */
    public Formation setSlot(String odUserId, int slotIndex, String generalId) {
        if (slotIndex < 0 || slotIndex > 5) {
            throw new BusinessException(400, "无效的槽位索引");
        }
        
        Formation formation = getFormation(odUserId);
        
        // 如果generalId为空，清空该槽位
        if (generalId == null || generalId.isEmpty()) {
            formation.getSlots().get(slotIndex).setGeneralId(null);
            formation.getSlots().get(slotIndex).setGeneralName(null);
            formation.getSlots().get(slotIndex).setQuality(null);
            formation.getSlots().get(slotIndex).setAvatar(null);
            formation.getSlots().get(slotIndex).setMobility(0);
            return formationRepository.save(formation);
        }
        
        // 验证武将是否属于该用户
        General general = generalRepository.findById(generalId);
        if (general == null || !odUserId.equals(general.getUserId())) {
            throw new BusinessException(400, "武将不存在或不属于当前用户");
        }
        
        // 检查武将是否已在其他槽位
        for (int i = 0; i < formation.getSlots().size(); i++) {
            Formation.FormationSlot slot = formation.getSlots().get(i);
            if (generalId.equals(slot.getGeneralId()) && i != slotIndex) {
                // 清空原槽位
                slot.setGeneralId(null);
                slot.setGeneralName(null);
                slot.setQuality(null);
                slot.setAvatar(null);
                slot.setMobility(0);
            }
        }
        
        // 设置新槽位
        Formation.FormationSlot targetSlot = formation.getSlots().get(slotIndex);
        targetSlot.setGeneralId(generalId);
        targetSlot.setGeneralName(general.getName());
        targetSlot.setQuality(general.getQualityName());
        targetSlot.setAvatar(general.getAvatar());
        targetSlot.setMobility(general.getAttrMobility() != null ? general.getAttrMobility() : 0);
        
        logger.info("用户 {} 设置阵型槽位 {}: {}", odUserId, slotIndex, general.getName());
        
        return formationRepository.save(formation);
    }
    
    /**
     * 批量设置阵型
     */
    public Formation setFormation(String odUserId, List<String> generalIds) {
        if (generalIds == null || generalIds.size() > 6) {
            throw new BusinessException(400, "阵型最多配置6个武将");
        }
        
        Formation formation = getFormation(odUserId);
        
        // 清空所有槽位
        for (Formation.FormationSlot slot : formation.getSlots()) {
            slot.setGeneralId(null);
            slot.setGeneralName(null);
            slot.setQuality(null);
            slot.setAvatar(null);
            slot.setMobility(0);
        }
        
        // 设置新阵型
        Set<String> usedIds = new HashSet<>();
        for (int i = 0; i < generalIds.size() && i < 6; i++) {
            String generalId = generalIds.get(i);
            if (generalId == null || generalId.isEmpty() || usedIds.contains(generalId)) {
                continue;
            }
            
            General general = generalRepository.findById(generalId);
            if (general != null && odUserId.equals(general.getUserId())) {
                Formation.FormationSlot slot = formation.getSlots().get(i);
                slot.setGeneralId(generalId);
                slot.setGeneralName(general.getName());
                slot.setQuality(general.getQualityName());
                slot.setAvatar(general.getAvatar());
                slot.setMobility(general.getAttrMobility() != null ? general.getAttrMobility() : 0);
                usedIds.add(generalId);
            }
        }
        
        logger.info("用户 {} 批量设置阵型，共 {} 个武将", odUserId, usedIds.size());
        
        return formationRepository.save(formation);
    }
    
    /**
     * 交换两个槽位的武将
     */
    public Formation swapSlots(String odUserId, int slotIndex1, int slotIndex2) {
        if (slotIndex1 < 0 || slotIndex1 > 5 || slotIndex2 < 0 || slotIndex2 > 5) {
            throw new BusinessException(400, "无效的槽位索引");
        }
        
        if (slotIndex1 == slotIndex2) {
            return getFormation(odUserId);
        }
        
        Formation formation = getFormation(odUserId);
        
        Formation.FormationSlot slot1 = formation.getSlots().get(slotIndex1);
        Formation.FormationSlot slot2 = formation.getSlots().get(slotIndex2);
        
        // 交换数据
        String tempId = slot1.getGeneralId();
        String tempName = slot1.getGeneralName();
        String tempQuality = slot1.getQuality();
        String tempAvatar = slot1.getAvatar();
        Integer tempMobility = slot1.getMobility();
        
        slot1.setGeneralId(slot2.getGeneralId());
        slot1.setGeneralName(slot2.getGeneralName());
        slot1.setQuality(slot2.getQuality());
        slot1.setAvatar(slot2.getAvatar());
        slot1.setMobility(slot2.getMobility());
        
        slot2.setGeneralId(tempId);
        slot2.setGeneralName(tempName);
        slot2.setQuality(tempQuality);
        slot2.setAvatar(tempAvatar);
        slot2.setMobility(tempMobility);
        
        return formationRepository.save(formation);
    }
    
    /**
     * 获取战斗顺序（根据位置和机动性排序）
     * 规则：机动性高的先行动，机动性相同时位置序号小的先行动
     * 包含装备加成
     */
    public List<General> getBattleOrder(String odUserId) {
        Formation formation = getFormation(odUserId);
        
        List<General> generals = new ArrayList<>();
        for (Formation.FormationSlot slot : formation.getSlots()) {
            if (slot.getGeneralId() != null) {
                General general = generalRepository.findById(slot.getGeneralId());
                if (general != null) {
                    // 临时存储槽位索引用于排序
                    general.setExp((long) slot.getIndex()); // 借用exp字段临时存储索引
                    
                    // 计算装备加成
                    Map<String, Integer> equipBonus = calculateEquipmentBonus(general.getId());
                    general.setEquipmentBonus(equipBonus);
                    
                    generals.add(general);
                }
            }
        }
        
        // 按机动性降序，机动性相同时按槽位索引升序
        generals.sort((a, b) -> {
            int mobA = a.getAttrMobility() != null ? a.getAttrMobility() : 0;
            int mobB = b.getAttrMobility() != null ? b.getAttrMobility() : 0;
            // 加上装备机动性加成
            mobA += a.getEquipmentBonus() != null ? a.getEquipmentBonus().getOrDefault("mobility", 0) : 0;
            mobB += b.getEquipmentBonus() != null ? b.getEquipmentBonus().getOrDefault("mobility", 0) : 0;
            if (mobA != mobB) {
                return mobB - mobA; // 机动性高的在前
            }
            return a.getExp().intValue() - b.getExp().intValue(); // 索引小的在前
        });
        
        return generals;
    }
    
    /**
     * 计算武将的装备加成
     */
    private Map<String, Integer> calculateEquipmentBonus(String generalId) {
        Map<String, Integer> bonus = new HashMap<>();
        bonus.put("attack", 0);
        bonus.put("defense", 0);
        bonus.put("valor", 0);
        bonus.put("command", 0);
        bonus.put("mobility", 0);
        bonus.put("hp", 0);
        
        // 获取武将已装备的装备
        List<Equipment> equipments = equipmentRepository.findEquippedByGeneralId(generalId);
        
        for (Equipment equipment : equipments) {
            if (equipment.getBaseAttributes() != null) {
                Equipment.Attributes base = equipment.getBaseAttributes();
                bonus.put("attack", bonus.get("attack") + (base.getAttack() != null ? base.getAttack() : 0));
                bonus.put("defense", bonus.get("defense") + (base.getDefense() != null ? base.getDefense() : 0));
                bonus.put("valor", bonus.get("valor") + (base.getValor() != null ? base.getValor() : 0));
                bonus.put("command", bonus.get("command") + (base.getCommand() != null ? base.getCommand() : 0));
                bonus.put("mobility", bonus.get("mobility") + (base.getMobility() != null ? base.getMobility() : 0));
                bonus.put("hp", bonus.get("hp") + (base.getHp() != null ? base.getHp() : 0));
            }
            
            if (equipment.getBonusAttributes() != null) {
                Equipment.Attributes bonusAttr = equipment.getBonusAttributes();
                bonus.put("attack", bonus.get("attack") + (bonusAttr.getAttack() != null ? bonusAttr.getAttack() : 0));
                bonus.put("defense", bonus.get("defense") + (bonusAttr.getDefense() != null ? bonusAttr.getDefense() : 0));
                bonus.put("valor", bonus.get("valor") + (bonusAttr.getValor() != null ? bonusAttr.getValor() : 0));
                bonus.put("command", bonus.get("command") + (bonusAttr.getCommand() != null ? bonusAttr.getCommand() : 0));
                bonus.put("mobility", bonus.get("mobility") + (bonusAttr.getMobility() != null ? bonusAttr.getMobility() : 0));
                bonus.put("hp", bonus.get("hp") + (bonusAttr.getHp() != null ? bonusAttr.getHp() : 0));
            }
        }
        
        return bonus;
    }
    
    /**
     * 获取阵型中的所有武将ID列表
     */
    public List<String> getFormationGeneralIds(String odUserId) {
        Formation formation = getFormation(odUserId);
        return formation.getSlots().stream()
            .filter(slot -> slot.getGeneralId() != null)
            .map(Formation.FormationSlot::getGeneralId)
            .collect(Collectors.toList());
    }
}

