package com.tencent.wxcloudrun.service.recruit;

import com.tencent.wxcloudrun.config.GeneralConfig;
import com.tencent.wxcloudrun.dao.GameServerMapper;
import com.tencent.wxcloudrun.dto.RecruitResult;
import com.tencent.wxcloudrun.exception.BusinessException;
import com.tencent.wxcloudrun.model.General;
import com.tencent.wxcloudrun.model.UserResource;
import com.tencent.wxcloudrun.model.Warehouse;
import com.tencent.wxcloudrun.repository.GeneralRepository;
import com.tencent.wxcloudrun.repository.UserResourceRepository;
import com.tencent.wxcloudrun.service.UserResourceService;
import com.tencent.wxcloudrun.service.chat.ChatService;
import com.tencent.wxcloudrun.service.general.GeneralService;
import com.tencent.wxcloudrun.service.nationwar.NationWarService;
import com.tencent.wxcloudrun.service.warehouse.WarehouseService;
import com.tencent.wxcloudrun.dao.StoryProgressMapper;
import com.tencent.wxcloudrun.model.StoryProgress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

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
    
    private static final String JUNIOR_TOKEN_ITEM_ID = "15011";
    private static final String INTERMEDIATE_TOKEN_ITEM_ID = "15012";
    private static final String SENIOR_TOKEN_ITEM_ID = "15013";
    
    /** 将魂召唤橙将所需点数 */
    private static final int SOUL_SUMMON_COST = 200;
    /** 高级招募将魂产出范围 */
    private static final int SOUL_MIN = 5;
    private static final int SOUL_MAX = 20;
    
    /** 开服前N天橙色概率提升期 */
    private static final long LAUNCH_BONUS_DAYS = 2;
    /** 开服期间高级招募出橙色的概率(%) */
    private static final int LAUNCH_ORANGE_RATE = 30;
    /** 正常高级招募出橙色的概率(%) */
    private static final int NORMAL_ORANGE_RATE = 10;
    /** 橙色8号将在橙色池中的权重(%) */
    private static final int ORANGE_NO8_WEIGHT_PCT = 5;
    /** 橙色9号将不可通过招募获得 */
    private static final int ORANGE_EXCLUDED_SLOT_INDEX = 9;
    /** 橙色池可招募的最大 slot_index */
    private static final int ORANGE_MAX_RECRUITABLE = 8;
    
    @Autowired private UserResourceRepository resourceRepository;
    @Autowired private GeneralRepository generalRepository;
    @Autowired private GeneralConfig generalConfig;
    @Autowired private UserResourceService userResourceService;
    @Autowired private WarehouseService warehouseService;
    @Autowired private NationWarService nationWarService;
    @Autowired private GeneralService generalService;
    @Autowired private GameServerMapper gameServerMapper;
    @Autowired private ChatService chatService;
    @Autowired private StoryProgressMapper storyProgressMapper;
    
    private static final String GUIDE_RECRUIT_SENIOR = "马腾";
    private static final String GUIDE_RECRUIT_INTERMEDIATE = "韩遂";
    
    /** 红色(4)/紫色(5)/橙色(6) 以上发全服通告 */
    private static final int ANNOUNCE_MIN_QUALITY_ID = 4;
    
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
    
    private static final Map<String, Set<String>> TOKEN_ALIAS_MAP = new HashMap<>();
    static {
        TOKEN_ALIAS_MAP.put("JUNIOR", new HashSet<>(Arrays.asList("15011", "7")));
        TOKEN_ALIAS_MAP.put("INTERMEDIATE", new HashSet<>(Arrays.asList("15012", "8", "item_8")));
        TOKEN_ALIAS_MAP.put("SENIOR", new HashSet<>(Arrays.asList("15013", "9", "item_9")));
    }

    private int getWarehouseTokenCount(String userId, String tokenType) {
        Set<String> aliases = TOKEN_ALIAS_MAP.getOrDefault(tokenType.toUpperCase(), Collections.emptySet());
        Warehouse warehouse = warehouseService.getWarehouse(userId);
        List<Warehouse.WarehouseItem> items = warehouse.getItemStorage().getItems();
        if (items == null) return 0;
        int total = 0;
        for (Warehouse.WarehouseItem item : items) {
            if (aliases.contains(item.getItemId())) {
                total += item.getCount() != null ? item.getCount() : 0;
            }
        }
        return total;
    }
    
    private void addWarehouseTokens(String userId, String tokenType, int count) {
        String itemId = getTokenItemId(tokenType);
        String name, icon, quality, description;
        switch (tokenType.toUpperCase()) {
            case "JUNIOR":
                name = "初级招贤令"; icon = "images/item/15011.jpg"; quality = "2";
                description = "可进行一次初级招募"; break;
            case "INTERMEDIATE":
                name = "中级招贤令"; icon = "images/item/15012.jpg"; quality = "3";
                description = "可进行一次中级招募"; break;
            case "SENIOR":
                name = "高级招贤令"; icon = "images/item/15013.jpg"; quality = "4";
                description = "可进行一次高级招募"; break;
            default: throw new BusinessException(400, "无效的招贤令类型");
        }
        Warehouse.WarehouseItem item = Warehouse.WarehouseItem.builder()
                .itemId(itemId).itemType("consumable").name(name).icon(icon)
                .quality(quality).count(count).maxStack(9999)
                .description(description).usable(false).build();
        warehouseService.addItem(userId, item);
    }
    
    private void removeWarehouseTokens(String userId, String tokenType, int count) {
        Set<String> aliases = TOKEN_ALIAS_MAP.getOrDefault(tokenType.toUpperCase(), Collections.emptySet());
        Warehouse warehouse = warehouseService.getWarehouse(userId);
        List<Warehouse.WarehouseItem> items = warehouse.getItemStorage().getItems();
        int remaining = count;
        if (items != null) {
            for (Warehouse.WarehouseItem item : items) {
                if (remaining <= 0) break;
                if (aliases.contains(item.getItemId())) {
                    int available = item.getCount() != null ? item.getCount() : 0;
                    int toRemove = Math.min(available, remaining);
                    if (toRemove > 0) {
                        warehouseService.removeItem(userId, item.getItemId(), toRemove);
                        remaining -= toRemove;
                    }
                }
            }
        }
        if (remaining > 0) {
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
    public RecruitResult recruit(String userId, String tokenType, String serverId) {
        // 检查武将位
        int currentCount = generalRepository.countByUserId(userId);
        int maxSlots = userResourceService.getMaxGeneralSlots(userId);
        if (currentCount >= maxSlots) {
            throw new BusinessException(400, "武将位已满（" + currentCount + "/" + maxSlots + "）");
        }
        
        // 引导期间招募：免费 + 固定武将
        if (isInGuide(userId)) {
            String fixedName = null;
            String fixedQuality = null;
            if ("SENIOR".equalsIgnoreCase(tokenType)) {
                fixedName = GUIDE_RECRUIT_SENIOR;
                fixedQuality = "purple";
            } else if ("INTERMEDIATE".equalsIgnoreCase(tokenType)) {
                fixedName = GUIDE_RECRUIT_INTERMEDIATE;
                fixedQuality = "blue";
            }
            if (fixedName != null) {
                General guideGeneral = tryGuideRecruit(userId, fixedName, fixedQuality);
                if (guideGeneral == null) {
                    logger.warn("引导招募: 固定武将{}获取失败, 降级为免费随机招募(quality={})", fixedName, fixedQuality);
                    guideGeneral = recruitOneByQuality(userId, fixedQuality);
                }
                userResourceService.updateGeneralCount(userId, currentCount + 1);
                generalRepository.saveAll(Collections.singletonList(guideGeneral));
                UserResource resource = getUserResource(userId);
                logger.info("引导招募: userId={}, 获得{}", userId, guideGeneral.getName());
                return RecruitResult.builder()
                        .general(guideGeneral)
                        .soulPointGained(0)
                        .totalSoulPoint(resource.getSoulPoint() != null ? resource.getSoulPoint() : 0)
                        .remainingTokens(0)
                        .tokenType(tokenType)
                        .build();
            }
        }
        
        // 检查并消耗招贤令
        int availableTokens = getWarehouseTokenCount(userId, tokenType);
        if (availableTokens < 1) {
            throw new BusinessException(400, "招贤令数量不足");
        }
        removeWarehouseTokens(userId, tokenType, 1);
        
        // 执行单抽（带开服加成判断）
        General general = recruitOne(userId, tokenType, serverId);
        userResourceService.updateGeneralCount(userId, currentCount + 1);
        generalRepository.saveAll(Collections.singletonList(general));
        
        // 红色以上名将全服通告
        tryAnnounceRecruit(userId, general);
        
        // 高级招募额外获得将魂
        int soulGained = 0;
        if ("SENIOR".equalsIgnoreCase(tokenType)) {
            soulGained = SOUL_MIN + random.nextInt(SOUL_MAX - SOUL_MIN + 1);
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
     * 同样遵循橙色招募规则（slot9不可招、不重复、#8概率5%）
     */
    public RecruitResult soulSummon(String userId, String serverId) {
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
        
        // 直接招募一个橙色武将（应用橙色专属规则）
        General general = recruitOrangeGeneral(userId);
        userResourceService.updateGeneralCount(userId, currentCount + 1);
        generalRepository.saveAll(Collections.singletonList(general));
        
        // 全服通告
        tryAnnounceRecruit(userId, general);
        
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
    
    private General recruitOne(String userId, String tokenType, String serverId) {
        String quality;
        switch (tokenType.toUpperCase()) {
            case "JUNIOR": quality = random.nextInt(100) < 70 ? "green" : "white"; break;
            case "INTERMEDIATE": quality = random.nextInt(100) < 50 ? "blue" : "red"; break;
            case "SENIOR": {
                int orangeRate = isWithinLaunchBonus(serverId) ? LAUNCH_ORANGE_RATE : NORMAL_ORANGE_RATE;
                quality = random.nextInt(100) < (100 - orangeRate) ? "purple" : "orange";
                break;
            }
            default: quality = "white";
        }
        if ("orange".equals(quality)) {
            return recruitOrangeGeneral(userId);
        }
        return recruitOneByQuality(userId, quality);
    }
    
    /**
     * 按指定品质招募一个武将（非橙色通用逻辑）
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
    
    /**
     * 橙色武将专属招募逻辑：
     *  1. slot_index=9 的橙色武将不可通过招募获得
     *  2. 1-8号全部集齐前，同一武将只会招募到一次
     *  3. 8号橙色武将在橙色池中占5%概率，最难获得
     *  4. 如果1-8号已全部集齐，则重新开放全池随机（可重复）
     */
    private General recruitOrangeGeneral(String userId) {
        // 获取全部橙色模板
        String playerFaction = nationIdToFaction(nationWarService.getPlayerNation(userId));
        List<GeneralConfig.GeneralTemplate> allOrange;
        if (playerFaction != null) {
            allOrange = generalConfig.getRecruitableGeneralsByQuality("orange", playerFaction);
            if (allOrange == null || allOrange.isEmpty()) {
                allOrange = generalConfig.getAllGeneralsByQuality("orange");
            }
        } else {
            allOrange = generalConfig.getAllGeneralsByQuality("orange");
        }
        if (allOrange == null || allOrange.isEmpty()) {
            throw new BusinessException(500, "橙色品质暂无可招募将领");
        }
        
        // 规则1: 过滤掉 slot_index = 9
        List<GeneralConfig.GeneralTemplate> pool = allOrange.stream()
                .filter(t -> t.slotIndex != ORANGE_EXCLUDED_SLOT_INDEX)
                .collect(Collectors.toList());
        if (pool.isEmpty()) {
            throw new BusinessException(500, "橙色品质暂无可招募将领");
        }
        
        // 规则2: 查询用户已拥有的橙色武将模板名
        List<General> userGenerals = generalRepository.findByUserId(userId);
        Set<String> ownedOrangeNames = new HashSet<>();
        for (General g : userGenerals) {
            if (g.getQualityId() != null && g.getQualityId() == 6) {
                ownedOrangeNames.add(g.getTemplateId());
            }
        }
        
        // 检查1-8号是否已全部集齐
        Set<String> allRecruitableNames = pool.stream()
                .map(t -> t.name)
                .collect(Collectors.toSet());
        boolean allCollected = ownedOrangeNames.containsAll(allRecruitableNames);
        
        // 未全部集齐时，排除已拥有的
        List<GeneralConfig.GeneralTemplate> candidates;
        if (!allCollected) {
            candidates = pool.stream()
                    .filter(t -> !ownedOrangeNames.contains(t.name))
                    .collect(Collectors.toList());
            if (candidates.isEmpty()) {
                candidates = pool;
            }
        } else {
            candidates = pool;
        }
        
        // 规则3+4: 加权随机选择，8号占5%
        GeneralConfig.GeneralTemplate selected = weightedOrangeSelect(candidates);
        
        logger.info("橙色招募: userId={}, 已拥有={}, 候选池大小={}, 命中={}(slot_index={})",
                userId, ownedOrangeNames, candidates.size(), selected.name, selected.slotIndex);
        
        return createGeneralFromTemplate(userId, selected);
    }
    
    /**
     * 带权重的橙色武将随机选择
     * slot_index=8 占5%权重，其余平分95%
     */
    private GeneralConfig.GeneralTemplate weightedOrangeSelect(List<GeneralConfig.GeneralTemplate> candidates) {
        if (candidates.size() == 1) return candidates.get(0);
        
        boolean hasNo8 = candidates.stream().anyMatch(t -> t.slotIndex == ORANGE_MAX_RECRUITABLE);
        if (!hasNo8) {
            return candidates.get(random.nextInt(candidates.size()));
        }
        
        int roll = random.nextInt(100);
        if (roll < ORANGE_NO8_WEIGHT_PCT) {
            // 命中8号
            for (GeneralConfig.GeneralTemplate t : candidates) {
                if (t.slotIndex == ORANGE_MAX_RECRUITABLE) return t;
            }
        }
        // 从非8号中等概率选择
        List<GeneralConfig.GeneralTemplate> nonNo8 = candidates.stream()
                .filter(t -> t.slotIndex != ORANGE_MAX_RECRUITABLE)
                .collect(Collectors.toList());
        if (nonNo8.isEmpty()) {
            return candidates.get(random.nextInt(candidates.size()));
        }
        return nonNo8.get(random.nextInt(nonNo8.size()));
    }
    
    private boolean isInGuide(String userId) {
        try {
            int idx = userId.lastIndexOf('_');
            String rawUserId = idx > 0 ? userId.substring(0, idx) : userId;
            int serverId = idx > 0 ? Integer.parseInt(userId.substring(idx + 1)) : 1;
            StoryProgress sp = storyProgressMapper.findByUserAndServer(rawUserId, serverId);
            return sp == null || !Boolean.TRUE.equals(sp.getCompleted());
        } catch (Exception e) {
            return false;
        }
    }
    
    private General tryGuideRecruit(String userId, String generalName, String qualityCode) {
        try {
            List<GeneralConfig.GeneralTemplate> pool = generalConfig.getAllGeneralsByQuality(qualityCode);
            GeneralConfig.GeneralTemplate template = pool.stream()
                    .filter(t -> generalName.equals(t.name))
                    .findFirst()
                    .orElse(null);
            if (template == null) {
                logger.warn("引导招募: 未找到{}模板(quality={})", generalName, qualityCode);
                return null;
            }
            List<General> owned = generalRepository.findByUserId(userId);
            boolean alreadyOwned = owned.stream().anyMatch(g -> generalName.equals(g.getName()));
            if (alreadyOwned) {
                logger.info("引导招募: 用户已拥有{}, 走正常招募流程", generalName);
                return null;
            }
            return createGeneralFromTemplate(userId, template);
        } catch (Exception e) {
            logger.warn("引导招募异常, userId={}, general={}", userId, generalName, e);
            return null;
        }
    }
    
    /**
     * 判断当前是否处于开服前N天奖励期
     */
    private boolean isWithinLaunchBonus(String serverId) {
        if (serverId == null || serverId.isEmpty()) return false;
        try {
            int sid = Integer.parseInt(serverId);
            Map<String, Object> server = gameServerMapper.findServerById(sid);
            if (server == null) return false;
            Object openTimeObj = server.get("open_time");
            if (openTimeObj == null) openTimeObj = server.get("openTime");
            if (openTimeObj == null) return false;
            long openTime = ((Number) openTimeObj).longValue();
            long elapsed = System.currentTimeMillis() - openTime;
            return elapsed < LAUNCH_BONUS_DAYS * 24 * 60 * 60 * 1000L;
        } catch (Exception e) {
            logger.warn("检查开服奖励期异常, serverId={}", serverId, e);
            return false;
        }
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
            .soldierRank(1).soldierCount(100).soldierMaxCount(100)
            .traits(traitDescs)
            .statusLocked(false).statusInBattle(false).statusInjured(false).statusMorale(100)
            .statTotalBattles(0).statVictories(0).statDefeats(0).statKills(0).statMvpCount(0)
            .createTime(System.currentTimeMillis()).updateTime(System.currentTimeMillis())
            .build();
    }
    
    private String formatTrait(GeneralConfig.Trait trait) {
        if ("special".equals(trait.type)) { return trait.value.toString(); }
        if ("tactics_trigger".equals(trait.type)) { return "兵法发动概率翻倍"; }
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
    
    /**
     * 红色以上名将招募成功后发全服通告
     */
    private void tryAnnounceRecruit(String userId, General general) {
        try {
            if (general.getQualityId() == null || general.getQualityId() < ANNOUNCE_MIN_QUALITY_ID) return;
            if (general.getSlotId() == null || general.getSlotId() <= 0) return;
            
            String lordName = resolveLordName(userId);
            String qualityTag;
            switch (general.getQualityId()) {
                case 6: qualityTag = "橙色"; break;
                case 5: qualityTag = "紫色"; break;
                case 4: qualityTag = "红色"; break;
                default: qualityTag = "";
            }
            String msg = "恭喜玩家【" + lordName + "】成功招募到" + qualityTag + "名将【" + general.getName() + "】！";
            int serverId = extractServerId(userId);
            chatService.sendSystemMessage(serverId, "world", msg);
        } catch (Exception e) {
            logger.warn("发送招募通告异常", e);
        }
    }
    
    /**
     * 从复合userId中解析主公名称
     */
    private String resolveLordName(String compositeUserId) {
        try {
            int lastUnderscore = compositeUserId.lastIndexOf('_');
            if (lastUnderscore > 0) {
                String rawUserId = compositeUserId.substring(0, lastUnderscore);
                String serverIdStr = compositeUserId.substring(lastUnderscore + 1);
                int serverId = Integer.parseInt(serverIdStr);
                Map<String, Object> ps = gameServerMapper.findPlayerServer(rawUserId, serverId);
                if (ps != null && ps.get("lordName") != null) {
                    return (String) ps.get("lordName");
                }
            }
        } catch (Exception e) {
            logger.debug("解析主公名称失败: {}", compositeUserId);
        }
        return "无名英雄";
    }
    
    private static int extractServerId(String compositeUserId) {
        if (compositeUserId == null) return 1;
        int idx = compositeUserId.lastIndexOf('_');
        if (idx > 0) {
            try { return Integer.parseInt(compositeUserId.substring(idx + 1)); }
            catch (NumberFormatException e) { return 1; }
        }
        return 1;
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
