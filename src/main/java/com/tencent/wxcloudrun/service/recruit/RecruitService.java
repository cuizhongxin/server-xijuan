package com.tencent.wxcloudrun.service.recruit;

import com.tencent.wxcloudrun.config.GeneralConfig;
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
 * 招募服务 - 基于三国将领配置（打平模型）
 */
@Service
public class RecruitService {
    
    private static final Logger logger = LoggerFactory.getLogger(RecruitService.class);
    
    private static final String JUNIOR_TOKEN_ITEM_ID = "7";
    private static final String INTERMEDIATE_TOKEN_ITEM_ID = "8";
    private static final String SENIOR_TOKEN_ITEM_ID = "9";
    
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
                addWarehouseTokens(userId, "SENIOR", 1); break;
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
    
    public List<General> recruit(String userId, String tokenType, int count) {
        int currentCount = generalRepository.countByUserId(userId);
        int maxSlots = userResourceService.getMaxGeneralSlots(userId);
        if (currentCount + count > maxSlots) {
            int avail = maxSlots - currentCount;
            if (avail <= 0) { throw new BusinessException(400, "武将位已满（" + currentCount + "/" + maxSlots + "）"); }
            throw new BusinessException(400, "武将位不足，最多还能招募" + avail + "个武将");
        }
        int availableTokens = getWarehouseTokenCount(userId, tokenType);
        if (availableTokens < count) { throw new BusinessException(400, "招贤令数量不足"); }
        removeWarehouseTokens(userId, tokenType, count);
        
        List<General> recruited = new ArrayList<>();
        for (int i = 0; i < count; i++) { recruited.add(recruitOne(userId, tokenType)); }
        userResourceService.updateGeneralCount(userId, currentCount + recruited.size());
        generalRepository.saveAll(recruited);
        logger.info("用户 {} 使用 {} 招募了 {} 个武将", userId, tokenType, count);
        return recruited;
    }
    
    private General recruitOne(String userId, String tokenType) {
        String quality;
        switch (tokenType.toUpperCase()) {
            case "JUNIOR": quality = random.nextInt(100) < 70 ? "green" : "white"; break;
            case "INTERMEDIATE": quality = random.nextInt(100) < 50 ? "blue" : "red"; break;
            case "SENIOR": quality = random.nextInt(100) < 90 ? "purple" : "orange"; break;
            default: quality = "white";
        }
        
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
        int[] attrs = generalService.calcAttributes(qualityMultiplier, troopType, level);
        
        // 特征加成
        List<String> traitDescs = new ArrayList<>();
        if (template.traits != null) {
            for (GeneralConfig.Trait trait : template.traits) {
                if (trait.value instanceof Integer) {
                    int v = (Integer) trait.value;
                    switch (trait.type) {
                        case "attack": attrs[0] += v; break;
                        case "defense": attrs[1] += v; break;
                        case "valor": attrs[2] += v; break;
                        case "command": attrs[3] += v; break;
                        case "dodge": attrs[4] = (int) Math.min(attrs[4] + v, 100); break;
                        case "mobility": attrs[5] += v; break;
                    }
                }
                traitDescs.add(formatTrait(trait));
            }
        }
        
        return General.builder()
            .id(id).userId(userId).templateId(template.name).name(template.name)
            .avatar("").faction(template.faction)
            .level(level).exp(0L).maxExp(100L)
            .qualityId(qualityId).qualityName(qualityName).qualityColor(qualityColor)
            .qualityBaseMultiplier(qualityMultiplier).qualityStar(star)
            .troopType(troopType)
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
