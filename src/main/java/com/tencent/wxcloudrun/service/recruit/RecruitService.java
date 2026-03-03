package com.tencent.wxcloudrun.service.recruit;

import com.tencent.wxcloudrun.config.GeneralConfig;
import com.tencent.wxcloudrun.dto.RecruitResult;
import com.tencent.wxcloudrun.exception.BusinessException;
import com.tencent.wxcloudrun.model.General;
import com.tencent.wxcloudrun.model.UserResource;
import com.tencent.wxcloudrun.model.Warehouse;
import com.tencent.wxcloudrun.repository.GeneralRepository;
import com.tencent.wxcloudrun.repository.UserResourceRepository;
import com.tencent.wxcloudrun.service.UserResourceService;
import com.tencent.wxcloudrun.service.general.GeneralService;
import com.tencent.wxcloudrun.service.nationwar.NationWarService;
import com.tencent.wxcloudrun.service.warehouse.WarehouseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.*;

/**
 * 招募服务 - 仅支持单抽
 * 初级招募：消耗1初级招贤令，概率出白/绿武将
 * 中级招募：消耗1中级招贤令，概率出蓝/红武将
 * 高级招募：消耗1高级招贤令，概率出紫/橙武将，额外获得5-20将魂
 * 200将魂可直接召唤一个橙色武将
 */
@Service
public class RecruitService {
    
    private static final Logger logger = LoggerFactory.getLogger(RecruitService.class);
    
    private static final String JUNIOR_TOKEN_ITEM_ID = "7";
    private static final String INTERMEDIATE_TOKEN_ITEM_ID = "8";
    private static final String SENIOR_TOKEN_ITEM_ID = "9";
    
    /** 将魂召唤橙将所需点数 */
    private static final int SOUL_SUMMON_COST = 200;
    /** 高级招募将魂产出范围 */
    private static final int SOUL_MIN = 5;
    private static final int SOUL_MAX = 20;
    
    @Autowired private UserResourceRepository resourceRepository;
    @Autowired private GeneralRepository generalRepository;
    @Autowired private GeneralConfig generalConfig;
    @Autowired private UserResourceService userResourceService;
    @Autowired private WarehouseService warehouseService;
    @Autowired private NationWarService nationWarService;
    @Autowired private GeneralService generalService;
    
    private Random random = new Random();
    
    public UserResource getUserResource(String userId) {
        UserResource resource = resourceRepository.findByUserId(userId);
        if (resource == null) { resource = resourceRepository.initUserResource(userId); }
        resource.setJuniorToken(getWarehouseTokenCount(userId, "JUNIOR"));
        resource.setIntermediateToken(getWarehouseTokenCount(userId, "INTERMEDIATE"));
        resource.setSeniorToken(getWarehouseTokenCount(userId, "SENIOR"));
        // 确保将魂字段不为null
        if (resource.getSoulPoint() == null) {
            resource.setSoulPoint(0);
        }
        return resource;
    }
    
    private String getTokenItemId(String tokenType) {
        switch (tokenType.toUpperCase()) {
            case "JUNIOR": return JUNIOR_TOKEN_ITEM_ID;
            case "INTERMEDIATE": return INTERMEDIATE_TOKEN_ITEM_ID;
            case "SENIOR": return SENIOR_TOKEN_ITEM_ID;
            default: throw new BusinessException(400, "无效的招贤令类型: " + tokenType);
        }
    }
    
    private int getWarehouseTokenCount(String userId, String tokenType) {
        String itemId = getTokenItemId(tokenType);
        Warehouse warehouse = warehouseService.getWarehouse(userId);
        List<Warehouse.WarehouseItem> items = warehouse.getItemStorage().getItems();
        if (items == null) return 0;
        for (Warehouse.WarehouseItem item : items) {
            if (itemId.equals(item.getItemId())) {
                return item.getCount() != null ? item.getCount() : 0;
            }
        }
        return 0;
    }
    
    private void addWarehouseTokens(String userId, String tokenType, int count) {
        String itemId = getTokenItemId(tokenType);
        String name, icon, quality, description;
        switch (tokenType.toUpperCase()) {
            case "JUNIOR":
                name = "初级招贤令"; icon = "📜"; quality = "green";
                description = "可进行一次初级招募"; break;
            case "INTERMEDIATE":
                name = "中级招贤令"; icon = "📃"; quality = "blue";
                description = "可进行一次中级招募"; break;
            case "SENIOR":
                name = "高级招贤令"; icon = "📋"; quality = "purple";
                description = "可进行一次高级招募"; break;
            default: throw new BusinessException(400, "无效的招贤令类型");
        }
        Warehouse.WarehouseItem item = Warehouse.WarehouseItem.builder()
                .itemId(itemId).itemType("token").name(name).icon(icon)
                .quality(quality).count(count).maxStack(9999)
                .description(description).usable(false).build();
        warehouseService.addItem(userId, item);
    }
    
    private void removeWarehouseTokens(String userId, String tokenType, int count) {
        String itemId = getTokenItemId(tokenType);
        if (!warehouseService.removeItem(userId, itemId, count)) {
            throw new BusinessException(400, "招贤令数量不足");
        }
    }
    
    public UserResource claimDailyTokens(String userId) {
        UserResource resource = resourceRepository.findByUserId(userId);
        if (resource == null) { resource = resourceRepository.initUserResource(userId); }
        String today = new SimpleDateFormat("yyyyMMdd").format(new Date());
        if (today.equals(resource.getLastClaimDate()) && resource.getDailyTokenClaimed() >= 3) {
            throw new BusinessException(400, "今日已领取完所有招贤令");
        }
        if (!today.equals(resource.getLastClaimDate())) {
            resource.setDailyTokenClaimed(0);
            resource.setLastClaimDate(today);
        }
        addWarehouseTokens(userId, "JUNIOR", 3);
        resource.setDailyTokenClaimed(resource.getDailyTokenClaimed() + 1);
        resourceRepository.save(resource);
        return getUserResource(userId);
    }
    
    public UserResource buyToken(String userId, String tokenType) {
        UserResource resource = resourceRepository.findByUserId(userId);
        if (resource == null) { resource = resourceRepository.initUserResource(userId); }
        switch (tokenType.toUpperCase()) {
            case "JUNIOR":
                if (resource.getSilver() < 10000) { throw new BusinessException(400, "银两不足"); }
                resource.setSilver(resource.getSilver() - 10000); break;
            case "INTERMEDIATE":
                if (resource.getGold() < 15) { throw new BusinessException(400, "黄金不足"); }
                resource.setGold(resource.getGold() - 15); break;
            case "SENIOR":
                if (resource.getGold() < 200) { throw new BusinessException(400, "黄金不足"); }
                resource.setGold(resource.getGold() - 200); break;
            default: throw new BusinessException(400, "无效的招贤令类型");
        }
        resourceRepository.save(resource);
        addWarehouseTokens(userId, tokenType, 1);
        return getUserResource(userId);
    }
    
    public UserResource composeToken(String userId, String fromType) {
        UserResource resource = resourceRepository.findByUserId(userId);
        if (resource == null) { resource = resourceRepository.initUserResource(userId); }
        switch (fromType.toUpperCase()) {
            case "JUNIOR":
                if (getWarehouseTokenCount(userId, "JUNIOR") < 15) { throw new BusinessException(400, "初级招贤令不足"); }
                if (resource.getSilver() < 5000) { throw new BusinessException(400, "银两不足"); }
                removeWarehouseTokens(userId, "JUNIOR", 15);
                resource.setSilver(resource.getSilver() - 5000);
                resourceRepository.save(resource);
                addWarehouseTokens(userId, "INTERMEDIATE", 1); break;
            case "INTERMEDIATE":
                if (getWarehouseTokenCount(userId, "INTERMEDIATE") < 15) { throw new BusinessException(400, "中级招贤令不足"); }
                if (resource.getGold() < 5) { throw new BusinessException(400, "黄金不足"); }
                removeWarehouseTokens(userId, "INTERMEDIATE", 15);
                resource.setGold(resource.getGold() - 5);
                resourceRepository.save(resource);
                addWarehouseTokens(userId, "SENIOR", 1); break;
            default: throw new BusinessException(400, "无效的合成类型");
        }
        return getUserResource(userId);
    }
    
    /**
     * 单抽招募（不再支持十连抽）
     */
    public RecruitResult recruit(String userId, String tokenType) {
        // 检查武将位
        int currentCount = generalRepository.countByUserId(userId);
        int maxSlots = userResourceService.getMaxGeneralSlots(userId);
        if (currentCount >= maxSlots) {
            throw new BusinessException(400, "武将位已满（" + currentCount + "/" + maxSlots + "）");
        }
        
        // 检查并消耗招贤令
        int availableTokens = getWarehouseTokenCount(userId, tokenType);
        if (availableTokens < 1) {
            throw new BusinessException(400, "招贤令数量不足");
        }
        removeWarehouseTokens(userId, tokenType, 1);
        
        // 执行单抽
        General general = recruitOne(userId, tokenType);
        userResourceService.updateGeneralCount(userId, currentCount + 1);
        generalRepository.saveAll(Collections.singletonList(general));
        
        // 高级招募额外获得将魂
        int soulGained = 0;
        if ("SENIOR".equalsIgnoreCase(tokenType)) {
            soulGained = SOUL_MIN + random.nextInt(SOUL_MAX - SOUL_MIN + 1); // 5-20
            addSoulPoint(userId, soulGained);
        }
        
        // 获取更新后的资源
        UserResource resource = getUserResource(userId);
        int remainingTokens = 0;
        switch (tokenType.toUpperCase()) {
            case "JUNIOR": remainingTokens = resource.getJuniorToken(); break;
            case "INTERMEDIATE": remainingTokens = resource.getIntermediateToken(); break;
            case "SENIOR": remainingTokens = resource.getSeniorToken(); break;
        }
        
        logger.info("用户 {} 使用 {} 单抽招募了武将: {}, 获得将魂: {}", 
                    userId, tokenType, general.getName(), soulGained);
        
        return RecruitResult.builder()
                .general(general)
                .soulPointGained(soulGained)
                .totalSoulPoint(resource.getSoulPoint())
                .remainingTokens(remainingTokens)
                .tokenType(tokenType.toUpperCase())
                .build();
    }
    
    /**
     * 将魂召唤 - 消耗200将魂直接召唤一个橙色武将
     */
    public RecruitResult soulSummon(String userId) {
        // 检查武将位
        int currentCount = generalRepository.countByUserId(userId);
        int maxSlots = userResourceService.getMaxGeneralSlots(userId);
        if (currentCount >= maxSlots) {
            throw new BusinessException(400, "武将位已满（" + currentCount + "/" + maxSlots + "）");
        }
        
        // 检查将魂
        UserResource resource = getUserResource(userId);
        int currentSoul = resource.getSoulPoint() != null ? resource.getSoulPoint() : 0;
        if (currentSoul < SOUL_SUMMON_COST) {
            throw new BusinessException(400, "将魂不足，需要" + SOUL_SUMMON_COST + "点，当前" + currentSoul + "点");
        }
        
        // 消耗将魂
        addSoulPoint(userId, -SOUL_SUMMON_COST);
        
        // 直接招募一个橙色武将
        General general = recruitOneByQuality(userId, "orange");
        userResourceService.updateGeneralCount(userId, currentCount + 1);
        generalRepository.saveAll(Collections.singletonList(general));
        
        // 获取更新后的资源
        resource = getUserResource(userId);
        
        logger.info("用户 {} 使用将魂召唤了橙色武将: {}", userId, general.getName());
        
        return RecruitResult.builder()
                .general(general)
                .soulPointGained(-SOUL_SUMMON_COST)
                .totalSoulPoint(resource.getSoulPoint())
                .remainingTokens(0)
                .tokenType("SOUL")
                .build();
    }
    
    /**
     * 增减将魂点数
     */
    private void addSoulPoint(String userId, int amount) {
        UserResource resource = resourceRepository.findByUserId(userId);
        if (resource == null) { resource = resourceRepository.initUserResource(userId); }
        int current = resource.getSoulPoint() != null ? resource.getSoulPoint() : 0;
        resource.setSoulPoint(Math.max(0, current + amount));
        resource.setUpdateTime(System.currentTimeMillis());
        resourceRepository.save(resource);
    }
    
    private General recruitOne(String userId, String tokenType) {
        String quality;
        switch (tokenType.toUpperCase()) {
            case "JUNIOR": quality = random.nextInt(100) < 70 ? "green" : "white"; break;
            case "INTERMEDIATE": quality = random.nextInt(100) < 50 ? "blue" : "red"; break;
            case "SENIOR": quality = random.nextInt(100) < 90 ? "purple" : "orange"; break;
            default: quality = "white";
        }
        return recruitOneByQuality(userId, quality);
    }
    
    /**
     * 按指定品质招募一个武将
     */
    private General recruitOneByQuality(String userId, String quality) {
        String playerFaction = nationIdToFaction(nationWarService.getPlayerNation(userId));
        List<GeneralConfig.GeneralTemplate> templates;
        if (playerFaction != null) {
            templates = generalConfig.getRecruitableGeneralsByQuality(quality, playerFaction);
            if (templates == null || templates.isEmpty()) {
                templates = generalConfig.getAllGeneralsByQuality(quality);
            }
        } else {
            templates = generalConfig.getAllGeneralsByQuality(quality);
        }
        if (templates == null || templates.isEmpty()) {
            throw new BusinessException(500, "该品质暂无可招募将领");
        }
        GeneralConfig.GeneralTemplate template = templates.get(random.nextInt(templates.size()));
        return createGeneralFromTemplate(userId, template);
    }
    
    private General createGeneralFromTemplate(String userId, GeneralConfig.GeneralTemplate template) {
        String id = "general_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8);
        
        // 品质映射
        int qualityId; int star; String qualityColor;
        GeneralConfig.Quality cq = generalConfig.getQualities().get(template.quality);
        switch (template.quality) {
            case "orange": qualityId = 6; star = 5; qualityColor = "#FF8C00"; break;
            case "purple": qualityId = 5; star = 4; qualityColor = "#9370DB"; break;
            case "red": qualityId = 4; star = 4; qualityColor = "#DC143C"; break;
            case "blue": qualityId = 3; star = 3; qualityColor = "#4169E1"; break;
            case "green": qualityId = 2; star = 2; qualityColor = "#32CD32"; break;
            default: qualityId = 1; star = 1; qualityColor = "#FFFFFF";
        }
        double qualityMultiplier = cq != null ? cq.attrMultiplier : 1.0;
        String qualityName = cq != null ? cq.name : "白色";
        
        // 兵种
        String troopType = template.troopType != null ? template.troopType : (new String[]{"步","骑","弓"})[random.nextInt(3)];
        
        int level = 1;
        int[] attrs;
        
        // 优先用 slotId 精确计算（已包含特性加成），降级用旧公式+手动加特性
        if (template.slotId > 0) {
            attrs = generalService.calcAttributesBySlot(template.slotId, level);
        } else {
            attrs = generalService.calcAttributes(qualityMultiplier, troopType, level);
            // 手动加特性（旧路径兼容）
            if (template.traits != null) {
                for (GeneralConfig.Trait trait : template.traits) {
                    if (trait.value instanceof Integer) {
                        int v = (Integer) trait.value;
                        switch (trait.type) {
                            case "attack": attrs[0] += v; break;
                            case "defense": attrs[1] += v; break;
                            case "valor": attrs[2] += v; break;
                            case "command": attrs[3] += v; break;
                            case "dodge": attrs[4] = (int) Math.min(50, attrs[4] + v); break;
                            case "mobility": attrs[5] += v; break;
                        }
                    }
                }
            }
        }
        
        // 特征描述（用于前端展示）
        List<String> traitDescs = new ArrayList<>();
        if (template.traits != null) {
            for (GeneralConfig.Trait trait : template.traits) {
                traitDescs.add(formatTrait(trait));
            }
        }
        
        return General.builder()
            .id(id).userId(userId).templateId(template.name).name(template.name)
            .avatar(template.avatar).faction(template.faction)
            .level(level).exp(0L).maxExp(100L)
            .qualityId(qualityId).qualityName(qualityName).qualityColor(qualityColor)
            .qualityBaseMultiplier(qualityMultiplier).qualityStar(star)
            .troopType(troopType)
            .slotId(template.slotId > 0 ? template.slotId : null)
            .attrAttack(attrs[0]).attrDefense(attrs[1]).attrValor(attrs[2])
            .attrCommand(attrs[3]).attrDodge((double) attrs[4]).attrMobility(attrs[5])
            .soldierRank(random.nextInt(3) + 1).soldierCount(1000).soldierMaxCount(1000)
            .traits(traitDescs)
            .statusLocked(false).statusInBattle(false).statusInjured(false).statusMorale(100)
            .statTotalBattles(0).statVictories(0).statDefeats(0).statKills(0).statMvpCount(0)
            .createTime(System.currentTimeMillis()).updateTime(System.currentTimeMillis())
            .build();
    }
    
    private String formatTrait(GeneralConfig.Trait trait) {
        if ("special".equals(trait.type)) { return trait.value.toString(); }
        String name;
        switch (trait.type) {
            case "attack": name = "攻击力"; break;
            case "defense": name = "防御力"; break;
            case "valor": name = "武勇"; break;
            case "command": name = "统御"; break;
            case "dodge": name = "闪避"; break;
            case "mobility": name = "机动性"; break;
            default: name = trait.type;
        }
        return name + "+" + trait.value;
    }
    
    private String nationIdToFaction(String nationId) {
        if (nationId == null || nationId.isEmpty()) return null;
        switch (nationId.toUpperCase()) {
            case "WEI": return "魏";
            case "SHU": return "蜀";
            case "WU": return "吴";
            default: return null;
        }
    }
}
