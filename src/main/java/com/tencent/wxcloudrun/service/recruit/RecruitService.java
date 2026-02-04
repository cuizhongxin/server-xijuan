package com.tencent.wxcloudrun.service.recruit;

import com.tencent.wxcloudrun.config.GeneralConfig;
import com.tencent.wxcloudrun.exception.BusinessException;
import com.tencent.wxcloudrun.model.General;
import com.tencent.wxcloudrun.model.UserResource;
import com.tencent.wxcloudrun.repository.GeneralRepository;
import com.tencent.wxcloudrun.repository.UserResourceRepository;
import com.tencent.wxcloudrun.service.UserResourceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.*;

/**
 * 招募服务 - 基于三国将领配置
 */
@Service
public class RecruitService {
    
    private static final Logger logger = LoggerFactory.getLogger(RecruitService.class);
    
    @Autowired
    private UserResourceRepository resourceRepository;
    
    @Autowired
    private GeneralRepository generalRepository;
    
    @Autowired
    private GeneralConfig generalConfig;
    
    @Autowired
    private UserResourceService userResourceService;
    
    private Random random = new Random();
    
    /**
     * 获取或初始化用户资源
     */
    public UserResource getUserResource(String userId) {
        UserResource resource = resourceRepository.findByUserId(userId);
        if (resource == null) {
            resource = resourceRepository.initUserResource(userId);
        }
        return resource;
    }
    
    /**
     * 每日领取初级招贤令
     */
    public UserResource claimDailyTokens(String userId) {
        UserResource resource = getUserResource(userId);
        
        String today = new SimpleDateFormat("yyyyMMdd").format(new Date());
        
        // 检查是否已领取
        if (today.equals(resource.getLastClaimDate()) && resource.getDailyTokenClaimed() >= 3) {
            throw new BusinessException(400, "今日已领取完所有招贤令");
        }
        
        // 重置每日领取次数
        if (!today.equals(resource.getLastClaimDate())) {
            resource.setDailyTokenClaimed(0);
            resource.setLastClaimDate(today);
        }
        
        // 领取3个初级招贤令
        resource.setJuniorToken(resource.getJuniorToken() + 3);
        resource.setDailyTokenClaimed(resource.getDailyTokenClaimed() + 1);
        
        return resourceRepository.save(resource);
    }
    
    /**
     * 购买招贤令
     */
    public UserResource buyToken(String userId, String tokenType) {
        UserResource resource = getUserResource(userId);
        
        switch (tokenType.toUpperCase()) {
            case "JUNIOR":
                // 10000银两购买初级招贤令
                if (resource.getSilver() < 10000) {
                    throw new BusinessException(400, "银两不足");
                }
                resource.setSilver(resource.getSilver() - 10000);
                resource.setJuniorToken(resource.getJuniorToken() + 1);
                break;
                
            case "INTERMEDIATE":
                // 15黄金购买中级招贤令
                if (resource.getGold() < 15) {
                    throw new BusinessException(400, "黄金不足");
                }
                resource.setGold(resource.getGold() - 15);
                resource.setIntermediateToken(resource.getIntermediateToken() + 1);
                break;
                
            case "SENIOR":
                // 200黄金购买高级招贤令
                if (resource.getGold() < 200) {
                    throw new BusinessException(400, "黄金不足");
                }
                resource.setGold(resource.getGold() - 200);
                resource.setSeniorToken(resource.getSeniorToken() + 1);
                break;
                
            default:
                throw new BusinessException(400, "无效的招贤令类型");
        }
        
        return resourceRepository.save(resource);
    }
    
    /**
     * 合成高级招贤令
     */
    public UserResource composeToken(String userId, String fromType) {
        UserResource resource = getUserResource(userId);
        
        switch (fromType.toUpperCase()) {
            case "JUNIOR":
                // 15个初级 + 5000银两 → 1个高级
                if (resource.getJuniorToken() < 15) {
                    throw new BusinessException(400, "初级招贤令不足");
                }
                if (resource.getSilver() < 5000) {
                    throw new BusinessException(400, "银两不足");
                }
                resource.setJuniorToken(resource.getJuniorToken() - 15);
                resource.setSilver(resource.getSilver() - 5000);
                resource.setSeniorToken(resource.getSeniorToken() + 1);
                break;
                
            case "INTERMEDIATE":
                // 15个中级 + 5黄金 → 1个高级
                if (resource.getIntermediateToken() < 15) {
                    throw new BusinessException(400, "中级招贤令不足");
                }
                if (resource.getGold() < 5) {
                    throw new BusinessException(400, "黄金不足");
                }
                resource.setIntermediateToken(resource.getIntermediateToken() - 15);
                resource.setGold(resource.getGold() - 5);
                resource.setSeniorToken(resource.getSeniorToken() + 1);
                break;
                
            default:
                throw new BusinessException(400, "无效的合成类型");
        }
        
        return resourceRepository.save(resource);
    }
    
    /**
     * 招募武将
     */
    public List<General> recruit(String userId, String tokenType, int count) {
        UserResource resource = getUserResource(userId);
        
        // 检查武将数量限制
        int currentGeneralCount = generalRepository.countByUserId(userId);
        int maxSlots = userResourceService.getMaxGeneralSlots(userId);
        
        if (currentGeneralCount + count > maxSlots) {
            int availableSlots = maxSlots - currentGeneralCount;
            if (availableSlots <= 0) {
                throw new BusinessException(400, "武将位已满（" + currentGeneralCount + "/" + maxSlots + "），请先扩充武将位或遣散武将");
            }
            throw new BusinessException(400, "武将位不足，最多还能招募" + availableSlots + "个武将");
        }
        
        // 检查招贤令数量
        int availableTokens = 0;
        switch (tokenType.toUpperCase()) {
            case "JUNIOR":
                availableTokens = resource.getJuniorToken();
                break;
            case "INTERMEDIATE":
                availableTokens = resource.getIntermediateToken();
                break;
            case "SENIOR":
                availableTokens = resource.getSeniorToken();
                break;
            default:
                throw new BusinessException(400, "无效的招贤令类型");
        }
        
        if (availableTokens < count) {
            throw new BusinessException(400, "招贤令数量不足");
        }
        
        // 扣除招贤令
        switch (tokenType.toUpperCase()) {
            case "JUNIOR":
                resource.setJuniorToken(resource.getJuniorToken() - count);
                break;
            case "INTERMEDIATE":
                resource.setIntermediateToken(resource.getIntermediateToken() - count);
                break;
            case "SENIOR":
                resource.setSeniorToken(resource.getSeniorToken() - count);
                break;
        }
        resourceRepository.save(resource);
        
        // 执行招募
        List<General> recruitedGenerals = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            General general = recruitOne(userId, tokenType);
            recruitedGenerals.add(general);
        }
        
        // 更新用户武将数量
        userResourceService.updateGeneralCount(userId, currentGeneralCount + recruitedGenerals.size());
        
        // 保存招募到的武将
        generalRepository.saveAll(recruitedGenerals);
        
        logger.info("用户 {} 使用 {} 招募了 {} 个武将", userId, tokenType, count);
        
        return recruitedGenerals;
    }
    
    /**
     * 招募一个武将 - 使用将领配置
     */
    private General recruitOne(String userId, String tokenType) {
        String quality;
        
        // 根据招贤令类型确定品质
        switch (tokenType.toUpperCase()) {
            case "JUNIOR":
                // 初级：绿色70%，白色30%
                quality = random.nextInt(100) < 70 ? "green" : "white";
                break;
                
            case "INTERMEDIATE":
                // 中级：蓝色50%，红色50%
                quality = random.nextInt(100) < 50 ? "blue" : "red";
                break;
                
            case "SENIOR":
                // 高级：紫色90%，橙色10%
                quality = random.nextInt(100) < 90 ? "purple" : "orange";
                break;
                
            default:
                quality = "white";
        }
        
        // 从配置中随机选择该品质的将领模板
        List<GeneralConfig.GeneralTemplate> templates = generalConfig.getAllGeneralsByQuality(quality);
        GeneralConfig.GeneralTemplate template = templates.get(random.nextInt(templates.size()));
        
        return createGeneralFromTemplate(userId, template);
    }
    
    /**
     * 根据模板创建武将
     */
    private General createGeneralFromTemplate(String userId, GeneralConfig.GeneralTemplate template) {
        String generalId = "general_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8);
        
        // 创建品质
        GeneralConfig.Quality configQuality = GeneralConfig.QUALITIES.get(template.quality);
        General.Quality quality = createQuality(template.quality, configQuality);
        
        // 创建类型
        General.GeneralType type = createGeneralTypeFromString(template.type);
        
        // 随机兵种
        int troopTypeId = random.nextInt(3) + 1;
        General.TroopType troopType = createTroopType(troopTypeId);
        
        int level = 1;
        
        // 计算基础属性
        General.Attributes attributes = calculateAttributes(quality, type, troopType, level);
        
        // 应用特征加成
        if (template.traits != null && !template.traits.isEmpty()) {
            attributes = applyTraits(attributes, template.traits);
        }
        
        // 士兵信息
        int soldierRank = random.nextInt(3) + 1;
        General.Soldiers soldiers = createSoldiers(troopType, soldierRank);
        
        // 构建特征描述
        List<String> traitDescriptions = new ArrayList<>();
        if (template.traits != null) {
            for (GeneralConfig.Trait trait : template.traits) {
                traitDescriptions.add(formatTrait(trait));
            }
        }
        
        // 装备（初始为空，6个槽位）
        General.Equipment equipment = General.Equipment.builder()
            .weaponId(null)
            .helmetId(null)
            .armorId(null)
            .ringId(null)
            .shoesId(null)
            .necklaceId(null)
            .build();
        
        // 兵法（初始为空）
        General.Tactics tactics = General.Tactics.builder()
            .primary(null)
            .secondary(null)
            .build();
        
        // 状态
        General.Status status = General.Status.builder()
            .locked(false)
            .inBattle(false)
            .injured(false)
            .morale(100)
            .build();
        
        // 战斗统计
        General.Stats stats = General.Stats.builder()
            .totalBattles(0)
            .victories(0)
            .defeats(0)
            .kills(0)
            .mvpCount(0)
            .build();
        
        return General.builder()
            .id(generalId)
            .userId(userId)
            .name(template.name)
            .quality(quality)
            .type(type)
            .troopType(troopType)
            .level(level)
            .exp(0L)
            .maxExp(100L)
            .avatar("")
            .faction(template.faction)
            .traits(traitDescriptions)
            .attributes(attributes)
            .soldiers(soldiers)
            .equipment(equipment)
            .tactics(tactics)
            .status(status)
            .stats(stats)
            .createTime(System.currentTimeMillis())
            .updateTime(System.currentTimeMillis())
            .build();
    }
    
    /**
     * 格式化特征描述
     */
    private String formatTrait(GeneralConfig.Trait trait) {
        if ("special".equals(trait.type)) {
            return trait.value.toString();
        }
        
        String attrName;
        switch (trait.type) {
            case "attack": attrName = "攻击力"; break;
            case "defense": attrName = "防御力"; break;
            case "valor": attrName = "武勇"; break;
            case "command": attrName = "统御"; break;
            case "dodge": attrName = "闪避"; break;
            case "mobility": attrName = "机动性"; break;
            default: attrName = trait.type;
        }
        return attrName + "+" + trait.value;
    }
    
    /**
     * 应用特征加成到属性
     */
    private General.Attributes applyTraits(General.Attributes base, List<GeneralConfig.Trait> traits) {
        int attack = base.getAttack();
        int defense = base.getDefense();
        int valor = base.getValor();
        int command = base.getCommand();
        double dodge = base.getDodge();
        int mobility = base.getMobility();
        
        for (GeneralConfig.Trait trait : traits) {
            if (trait.value instanceof Integer) {
                int value = (Integer) trait.value;
                switch (trait.type) {
                    case "attack": attack += value; break;
                    case "defense": defense += value; break;
                    case "valor": valor += value; break;
                    case "command": command += value; break;
                    case "dodge": dodge = Math.min(dodge + value, 100); break;
                    case "mobility": mobility += value; break;
                }
            }
        }
        
        int power = (int)(attack * 1.2 + defense * 1.2 + valor * 1.5 + command * 1.5 + dodge * 2 + mobility * 1.0);
        
        return General.Attributes.builder()
            .attack(attack)
            .defense(defense)
            .valor(valor)
            .command(command)
            .dodge(dodge)
            .mobility(mobility)
            .power(power)
            .build();
    }
    
    /**
     * 根据字符串创建武将类型
     */
    private General.GeneralType createGeneralTypeFromString(String typeName) {
        int id;
        Map<String, Double> attributes = new HashMap<>();
        String icon;
        
        switch (typeName) {
            case "猛将":
                id = 1;
                icon = "⚔️";
                attributes.put("attack", 1.3);
                attributes.put("defense", 0.9);
                attributes.put("valor", 1.3);
                attributes.put("command", 0.7);
                attributes.put("dodge", 1.0);
                attributes.put("mobility", 1.1);
                break;
            case "智将":
                id = 7;
                icon = "📚";
                attributes.put("attack", 0.7);
                attributes.put("defense", 0.9);
                attributes.put("valor", 0.7);
                attributes.put("command", 1.5);
                attributes.put("dodge", 1.0);
                attributes.put("mobility", 1.0);
                break;
            case "统帅":
                id = 5;
                icon = "👑";
                attributes.put("attack", 1.0);
                attributes.put("defense", 1.1);
                attributes.put("valor", 1.0);
                attributes.put("command", 1.2);
                attributes.put("dodge", 1.0);
                attributes.put("mobility", 1.1);
                break;
            default: // 普通
                id = 5;
                icon = "⚖️";
                attributes.put("attack", 1.0);
                attributes.put("defense", 1.0);
                attributes.put("valor", 1.0);
                attributes.put("command", 1.0);
                attributes.put("dodge", 1.0);
                attributes.put("mobility", 1.0);
        }
        
        return General.GeneralType.builder()
            .id(id)
            .name(typeName)
            .description("")
            .icon(icon)
            .attributes(attributes)
            .build();
    }
    
    private General.Quality createQuality(String qualityKey, GeneralConfig.Quality configQuality) {
        int id;
        int star;
        String icon;
        
        switch (qualityKey) {
            case "orange": id = 6; star = 5; icon = "🟠"; break;
            case "purple": id = 5; star = 4; icon = "🟣"; break;
            case "red": id = 4; star = 4; icon = "🔴"; break;
            case "blue": id = 3; star = 3; icon = "🔵"; break;
            case "green": id = 2; star = 2; icon = "🟢"; break;
            default: id = 1; star = 1; icon = "⚪";
        }
        
        return General.Quality.builder()
            .id(id)
            .name(configQuality.name)
            .color(configQuality.color)
            .baseMultiplier(configQuality.attrMultiplier)
            .star(star)
            .icon(icon)
            .build();
    }
    
    private General.TroopType createTroopType(int id) {
        Map<String, Object> troopData = new HashMap<>();
        Map<String, Double> attributes = new HashMap<>();
        
        switch (id) {
            case 1: // 步兵
                troopData.put("name", "步兵");
                troopData.put("icon", "🛡️");
                troopData.put("restrains", "ARCHER");
                troopData.put("restrainedBy", "CAVALRY");
                attributes.put("attack", 0.8);
                attributes.put("defense", 1.3);
                attributes.put("dodge", 1.5);
                break;
            case 2: // 骑兵
                troopData.put("name", "骑兵");
                troopData.put("icon", "🐎");
                troopData.put("restrains", "INFANTRY");
                troopData.put("restrainedBy", "ARCHER");
                attributes.put("attack", 1.0);
                attributes.put("defense", 1.0);
                attributes.put("dodge", 1.0);
                break;
            case 3: // 弓兵
                troopData.put("name", "弓兵");
                troopData.put("icon", "🏹");
                troopData.put("restrains", "CAVALRY");
                troopData.put("restrainedBy", "INFANTRY");
                attributes.put("attack", 1.3);
                attributes.put("defense", 0.7);
                attributes.put("dodge", 1.0);
                break;
        }
        
        return General.TroopType.builder()
            .id(id)
            .name((String) troopData.get("name"))
            .icon((String) troopData.get("icon"))
            .description("")
            .attributes(attributes)
            .restrains((String) troopData.get("restrains"))
            .restrainedBy((String) troopData.get("restrainedBy"))
            .restrainBonus(0.3)
            .build();
    }
    
    private General.Attributes calculateAttributes(General.Quality quality, 
                                                  General.GeneralType type,
                                                  General.TroopType troopType,
                                                  int level) {
        int baseAttack = 100;
        int baseDefense = 100;
        int baseValor = 50;
        int baseCommand = 50;
        double baseDodge = 10.0;
        int baseMobility = 50;
        
        double qualityMultiplier = quality.getBaseMultiplier();
        Map<String, Double> typeAttr = type.getAttributes();
        Map<String, Double> troopAttr = troopType.getAttributes();
        
        int attack = (int)(baseAttack * qualityMultiplier * typeAttr.get("attack") * troopAttr.get("attack"));
        int defense = (int)(baseDefense * qualityMultiplier * typeAttr.get("defense") * troopAttr.get("defense"));
        int valor = (int)(baseValor * qualityMultiplier * typeAttr.get("valor"));
        int command = (int)(baseCommand * qualityMultiplier * typeAttr.get("command"));
        double dodge = Math.min(baseDodge * qualityMultiplier * typeAttr.get("dodge") * troopAttr.get("dodge"), 100.0);
        int mobility = (int)(baseMobility * qualityMultiplier * typeAttr.get("mobility"));
        
        int power = (int)(attack * 1.2 + defense * 1.2 + valor * 1.5 + command * 1.5 + dodge * 2 + mobility * 1.0);
        
        return General.Attributes.builder()
            .attack(attack)
            .defense(defense)
            .valor(valor)
            .command(command)
            .dodge(dodge)
            .mobility(mobility)
            .power(power)
            .build();
    }
    
    private General.Soldiers createSoldiers(General.TroopType troopType, int rank) {
        General.SoldierRankInfo rankInfo = General.SoldierRankInfo.builder()
            .level(rank)
            .name("士兵" + rank + "级")
            .icon("⚔️")
            .powerMultiplier(0.1 * rank)
            .build();
        
        return General.Soldiers.builder()
            .type(troopType)
            .rank(rank)
            .rankInfo(rankInfo)
            .count(1000)
            .maxCount(1000)
            .build();
    }
}
