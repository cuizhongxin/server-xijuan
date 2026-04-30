package com.tencent.wxcloudrun.service.simulation;

import com.tencent.wxcloudrun.dao.GameServerMapper;
import com.tencent.wxcloudrun.model.Equipment;
import com.tencent.wxcloudrun.model.General;
import com.tencent.wxcloudrun.model.Production.Recipe;
import com.tencent.wxcloudrun.model.SecretRealm;
import com.tencent.wxcloudrun.model.Shop;
import com.tencent.wxcloudrun.service.ShopService;
import com.tencent.wxcloudrun.service.PlayerNameResolver;
import com.tencent.wxcloudrun.service.alliance.AllianceWarService;
import com.tencent.wxcloudrun.service.bosswar.BossWarService;
import com.tencent.wxcloudrun.service.campaign.CampaignService;
import com.tencent.wxcloudrun.service.chat.ChatService;
import com.tencent.wxcloudrun.service.equipment.EquipmentService;
import com.tencent.wxcloudrun.service.formation.FormationService;
import com.tencent.wxcloudrun.service.general.GeneralService;
import com.tencent.wxcloudrun.service.herorank.HeroRankService;
import com.tencent.wxcloudrun.service.market.MarketService;
import com.tencent.wxcloudrun.service.mail.MailService;
import com.tencent.wxcloudrun.service.nationwar.NationWarService;
import com.tencent.wxcloudrun.service.plunder.PlunderService;
import com.tencent.wxcloudrun.service.production.ProductionService;
import com.tencent.wxcloudrun.service.recruit.RecruitService;
import com.tencent.wxcloudrun.service.refine.RefineService;
import com.tencent.wxcloudrun.service.supply.SupplyService;
import com.tencent.wxcloudrun.service.training.TrainingService;
import com.tencent.wxcloudrun.service.warehouse.WarehouseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.time.LocalTime;

@Service
public class PlayerSimulationService {

    private static final Logger logger = LoggerFactory.getLogger(PlayerSimulationService.class);

    private static final String[] WORLD_CHAT_TEMPLATES = new String[] {
            "有一起打军需的吗？",
            "今天榜单冲一冲！",
            "谁来切磋下阵容？",
            "联盟战报名别忘了~",
            "有没有市场低价材料出手？",
            "国战今晚见，冲城！",
            "BOSS快开了，兄弟们集合！",
            "刚掠夺了一波，收益不错。"
    };

    private final GameServerMapper gameServerMapper;
    private final ChatService chatService;
    private final HeroRankService heroRankService;
    private final SupplyService supplyService;
    private final PlunderService plunderService;
    private final NationWarService nationWarService;
    private final AllianceWarService allianceWarService;
    private final BossWarService bossWarService;
    private final MarketService marketService;
    private final CampaignService campaignService;
    private final RecruitService recruitService;
    private final TrainingService trainingService;
    private final ProductionService productionService;
    private final MailService mailService;
    private final ShopService shopService;
    private final EquipmentService equipmentService;
    private final RefineService refineService;
    private final FormationService formationService;
    private final GeneralService generalService;
    private final WarehouseService warehouseService;
    private final PlayerNameResolver playerNameResolver;

    private final Random random = new Random();

    public PlayerSimulationService(GameServerMapper gameServerMapper,
                                   ChatService chatService,
                                   HeroRankService heroRankService,
                                   SupplyService supplyService,
                                   PlunderService plunderService,
                                   NationWarService nationWarService,
                                   AllianceWarService allianceWarService,
                                   BossWarService bossWarService,
                                   MarketService marketService,
                                   CampaignService campaignService,
                                   RecruitService recruitService,
                                   TrainingService trainingService,
                                   ProductionService productionService,
                                   MailService mailService,
                                   ShopService shopService,
                                   EquipmentService equipmentService,
                                   RefineService refineService,
                                   FormationService formationService,
                                   GeneralService generalService,
                                   WarehouseService warehouseService,
                                   PlayerNameResolver playerNameResolver) {
        this.gameServerMapper = gameServerMapper;
        this.chatService = chatService;
        this.heroRankService = heroRankService;
        this.supplyService = supplyService;
        this.plunderService = plunderService;
        this.nationWarService = nationWarService;
        this.allianceWarService = allianceWarService;
        this.bossWarService = bossWarService;
        this.marketService = marketService;
        this.campaignService = campaignService;
        this.recruitService = recruitService;
        this.trainingService = trainingService;
        this.productionService = productionService;
        this.mailService = mailService;
        this.shopService = shopService;
        this.equipmentService = equipmentService;
        this.refineService = refineService;
        this.formationService = formationService;
        this.generalService = generalService;
        this.warehouseService = warehouseService;
        this.playerNameResolver = playerNameResolver;
    }

    public Map<String, Object> runSimulationOnce(int maxPlayers, boolean includeWarModules) {
        return runSimulationOnce(maxPlayers, includeWarModules, "medium", Collections.emptyMap());
    }

    public Map<String, Object> runSimulationOnce(int maxPlayers,
                                                 boolean includeWarModules,
                                                 String activityProfile,
                                                 Map<Integer, Double> serverWeights) {
        List<Map<String, Object>> players = gameServerMapper.findAllPlayerServers();
        if (players == null || players.isEmpty()) {
            Map<String, Object> empty = new LinkedHashMap<>();
            empty.put("totalPlayers", 0);
            empty.put("sampledPlayers", 0);
            empty.put("successActions", 0);
            empty.put("failedActions", 0);
            empty.put("actionStats", Collections.emptyMap());
            empty.put("message", "没有可模拟的玩家");
            return empty;
        }

        String profile = normalizeProfile(activityProfile);
        List<Map<String, Object>> weighted = applyServerWeight(players, serverWeights);
        int sampled = Math.min(Math.max(1, maxPlayers), weighted.size());
        LocalTime now = LocalTime.now();
        boolean isDaytime = now.getHour() >= 9 && now.getHour() < 18;
        boolean isNightPvpTime = now.getHour() >= 19 && now.getHour() <= 23;

        int successActions = 0;
        int failedActions = 0;
        Map<String, Integer> actionStats = new LinkedHashMap<>();

        for (int i = 0; i < sampled; i++) {
            Map<String, Object> p = weighted.get(i);
            String rawUserId = str(p.get("userId"));
            int serverId = intVal(p.get("serverId"), 1);
            String userId = rawUserId + "_" + serverId;
            String playerName = resolvePlayerName(p, userId);
            int level = intVal(p.get("roleLevel"), 30);
            long power = Math.max(8000L, level * 350L);
            String sid = String.valueOf(serverId);

            if (shouldDo(0.65, profile, isDaytime, isNightPvpTime, "social")) {
                if (safeAction(actionStats, "chat", () -> doChat(userId, playerName))) successActions++;
                else failedActions++;
            }

            if (shouldDo(0.55, profile, isDaytime, isNightPvpTime, "pvp")) {
                if (safeAction(actionStats, "heroRank", () -> doHeroRank(userId))) successActions++;
                else failedActions++;
            }

            if (shouldDo(0.55, profile, isDaytime, isNightPvpTime, "pve")) {
                if (safeAction(actionStats, "supply", () -> doSupply(userId))) successActions++;
                else failedActions++;
            }

            if (shouldDo(0.50, profile, isDaytime, isNightPvpTime, "pvp")) {
                if (safeAction(actionStats, "plunder", () -> doPlunder(userId))) successActions++;
                else failedActions++;
            }

            if (shouldDo(0.40, profile, isDaytime, isNightPvpTime, "economy")) {
                if (safeAction(actionStats, "market", () -> doMarket(userId))) successActions++;
                else failedActions++;
            }

            if (shouldDo(0.35, profile, isDaytime, isNightPvpTime, "pve")) {
                if (safeAction(actionStats, "boss", () -> doBoss(userId))) successActions++;
                else failedActions++;
            }

            if (shouldDo(0.55, profile, isDaytime, isNightPvpTime, "pve")) {
                if (safeAction(actionStats, "campaign", () -> doCampaign(userId))) successActions++;
                else failedActions++;
            }

            if (shouldDo(0.50, profile, isDaytime, isNightPvpTime, "growth")) {
                if (safeAction(actionStats, "recruit", () -> doRecruit(userId, sid))) successActions++;
                else failedActions++;
            }

            if (shouldDo(0.45, profile, isDaytime, isNightPvpTime, "growth")) {
                if (safeAction(actionStats, "training", () -> doTraining(userId))) successActions++;
                else failedActions++;
            }

            if (shouldDo(0.45, profile, isDaytime, isNightPvpTime, "production")) {
                if (safeAction(actionStats, "production", () -> doProduction(userId))) successActions++;
                else failedActions++;
            }

            if (shouldDo(0.28, profile, isDaytime, isNightPvpTime, "pve")) {
                if (safeAction(actionStats, "secretRealm", () -> doSecretRealm(userId, level))) successActions++;
                else failedActions++;
            }

            if (shouldDo(0.40, profile, isDaytime, isNightPvpTime, "social")) {
                if (safeAction(actionStats, "mail", () -> doMail(userId))) successActions++;
                else failedActions++;
            }

            if (shouldDo(0.48, profile, isDaytime, isNightPvpTime, "economy")) {
                if (safeAction(actionStats, "shopAndUse", () -> doShopAndUse(userId))) successActions++;
                else failedActions++;
            }

            if (shouldDo(0.50, profile, isDaytime, isNightPvpTime, "growth")) {
                if (safeAction(actionStats, "equipmentAndFormation", () -> doEquipmentAndFormation(userId))) successActions++;
                else failedActions++;
            }

            if (shouldDo(0.42, profile, isDaytime, isNightPvpTime, "economy")) {
                if (safeAction(actionStats, "sellUnusedEquipment", () -> doSellUnusedEquipment(userId))) successActions++;
                else failedActions++;
            }

            if (shouldDo(0.45, profile, isDaytime, isNightPvpTime, "growth")) {
                if (safeAction(actionStats, "refine", () -> doRefine(userId))) successActions++;
                else failedActions++;
            }

            if (includeWarModules) {
                if (shouldDo(0.30, profile, isDaytime, isNightPvpTime, "pvp")) {
                    if (safeAction(actionStats, "nationWar", () -> doNationWar(userId, playerName, level, power))) successActions++;
                    else failedActions++;
                }
                if (shouldDo(0.25, profile, isDaytime, isNightPvpTime, "pvp")) {
                    if (safeAction(actionStats, "allianceWar", () -> doAllianceWar(userId, playerName, level, power))) successActions++;
                    else failedActions++;
                }
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalPlayers", players.size());
        result.put("sampledPlayers", sampled);
        result.put("successActions", successActions);
        result.put("failedActions", failedActions);
        result.put("actionStats", actionStats);
        result.put("includeWarModules", includeWarModules);
        result.put("activityProfile", profile);
        result.put("timeWindow", isNightPvpTime ? "night-pvp" : (isDaytime ? "day-production" : "normal"));
        result.put("serverWeights", serverWeights == null ? Collections.emptyMap() : serverWeights);
        return result;
    }

    private void doChat(String userId, String playerName) {
        String msg = WORLD_CHAT_TEMPLATES[random.nextInt(WORLD_CHAT_TEMPLATES.length)];
        chatService.sendMessage(userId, playerName, "world", msg);
    }

    private void doHeroRank(String userId) {
        Map<String, Object> info = heroRankService.getInfo(userId);
        if (info == null) return;
        Map<String, Object> my = castMap(info.get("myInfo"));
        List<Map<String, Object>> rankings = castList(info.get("rankings"));
        if (my == null || rankings == null || rankings.isEmpty()) return;
        int myRank = intVal(my.get("ranking"), Integer.MAX_VALUE);
        String targetId = null;
        for (Map<String, Object> row : rankings) {
            String uid = str(row.get("userId"));
            int rank = intVal(row.get("ranking"), Integer.MAX_VALUE);
            if (!uid.equals(userId) && rank < myRank) {
                targetId = uid;
                break;
            }
        }
        if (targetId != null) {
            heroRankService.challenge(userId, targetId);
        }
    }

    private void doSupply(String userId) {
        Map<String, Object> info = supplyService.getSupplyInfo(userId);
        if (info == null) return;

        Map<String, Object> active = castMap(info.get("activeTransport"));
        if (active == null) {
            int todayTransport = intVal(info.get("todayTransport"), 0);
            int transportLimit = intVal(info.get("transportLimit"), 3);
            if (todayTransport < transportLimit) {
                supplyService.startTransport(userId);
            }
        } else {
            boolean completed = boolVal(active.get("completed"));
            long transportId = longVal(active.get("id"), 0L);
            if (completed && transportId > 0) {
                supplyService.collectTransport(userId, transportId);
            }
        }

        if (chance(0.30)) {
            Map<String, Object> map = supplyService.getMapTransports(userId);
            List<Map<String, Object>> transports = castList(map.get("transports"));
            if (transports != null && !transports.isEmpty()) {
                List<Map<String, Object>> targets = new ArrayList<>();
                for (Map<String, Object> t : transports) {
                    if (!boolVal(t.get("isOwn")) && !boolVal(t.get("completed"))) targets.add(t);
                }
                if (!targets.isEmpty()) {
                    Map<String, Object> pick = targets.get(random.nextInt(targets.size()));
                    long id = longVal(pick.get("id"), 0L);
                    if (id > 0) supplyService.robTransport(userId, id);
                }
            }
        }
    }

    private void doPlunder(String userId) {
        Map<String, Object> info = plunderService.getPlunderInfo(userId);
        int available = intVal(info.get("availableCount"), 0);
        if (available <= 0) return;

        Map<String, Object> targets = plunderService.getTargetList(userId, 0);
        List<Map<String, Object>> list = castList(targets.get("targets"));
        if (list == null || list.isEmpty()) return;
        for (Map<String, Object> t : list) {
            String tid = str(t.get("id"));
            if (tid.startsWith("npc_")) {
                plunderService.doPlunder(userId, tid);
                return;
            }
        }
    }

    private void doMarket(String userId) {
        Map<String, Object> page = marketService.browse(userId, null, null, 0);
        List<Map<String, Object>> listings = castList(page.get("listings"));
        if (listings == null || listings.isEmpty()) return;
        List<Map<String, Object>> candidates = new ArrayList<>();
        for (Map<String, Object> row : listings) {
            String sellerId = str(row.get("sellerId"));
            if (!userId.equals(sellerId)) candidates.add(row);
        }
        if (candidates.isEmpty()) return;
        Map<String, Object> pick = candidates.get(random.nextInt(candidates.size()));
        long listingId = longVal(pick.get("id"), 0L);
        if (listingId > 0) marketService.buy(userId, listingId);
    }

    private void doSellUnusedEquipment(String userId) {
        Map<String, Object> my = marketService.myListings(userId);
        int activeCount = intVal(my.get("activeCount"), 0);
        int maxListings = intVal(my.get("maxListings"), 10);
        if (activeCount >= maxListings) return;

        List<Equipment> bag = equipmentService.getUserBagEquipments(userId);
        if (bag == null || bag.isEmpty()) return;
        Collections.shuffle(bag, random);

        Equipment pick = null;
        for (Equipment eq : bag) {
            if (eq == null) continue;
            if (boolVal(readObjProp(eq, "bound"))) continue;
            if (boolVal(readObjProp(eq, "equipped"))) continue;
            pick = eq;
            break;
        }
        if (pick == null) return;

        String equipId = objString(pick, "id");
        int level = objInt(pick, "level", 1);
        int quality = resolveEquipQuality(pick);
        if (equipId.isEmpty()) return;

        long price = Math.min(50000L, Math.max(300L, level * 120L + quality * 180L));
        marketService.listEquipment(userId, equipId, price);
    }

    private void doBoss(String userId) {
        Map<String, Object> info = bossWarService.getInfo(userId);
        Map<String, Object> boss = castMap(info.get("currentBoss"));
        if (boss == null) return;
        String status = str(boss.get("status"));
        if (!"active".equals(status)) return;
        int bossId = intVal(boss.get("id"), 0);
        double cooldown = doubleVal(boss.get("cooldown"), 0D);
        if (bossId > 0 && cooldown <= 0) {
            bossWarService.attack(userId, bossId);
        }
    }

    private void doCampaign(String userId) {
        List<Map<String, Object>> campaigns = campaignService.getCampaignList(userId);
        if (campaigns == null || campaigns.isEmpty()) return;

        List<Map<String, Object>> unlocked = new ArrayList<>();
        for (Map<String, Object> c : campaigns) {
            if (boolVal(c.get("unlocked"))) unlocked.add(c);
        }
        if (unlocked.isEmpty()) return;

        Map<String, Object> pick = unlocked.get(random.nextInt(unlocked.size()));
        String campaignId = str(pick.get("id"));
        if (campaignId.isEmpty()) return;

        String generalId = pickRandomGeneralId(userId);
        if (generalId == null) return;

        campaignService.startCampaign(userId, campaignId, generalId);
        if (chance(0.75)) {
            campaignService.attack(userId, campaignId);
        }
    }

    private void doRecruit(String userId, String serverId) {
        if (chance(0.25)) {
            recruitService.claimDailyTokens(userId);
        }

        String[] order = new String[] {"SENIOR", "INTERMEDIATE", "JUNIOR"};
        for (String tokenType : order) {
            try {
                recruitService.recruit(userId, tokenType, serverId);
                return;
            } catch (Exception ignore) {
                // try next
            }
        }

        // no token: buy one and recruit
        String tokenType = chance(0.4) ? "JUNIOR" : "INTERMEDIATE";
        recruitService.buyToken(userId, tokenType);
        recruitService.recruit(userId, tokenType, serverId);
    }

    private void doTraining(String userId) {
        List<General> generals = generalService.getUserGenerals(userId);
        if (generals == null || generals.isEmpty()) return;
        General g = generals.get(random.nextInt(generals.size()));
        String generalId = objString(g, "id");
        if (generalId.isEmpty()) return;

        String trainingType = chance(0.5) ? "lord" : "general";
        String[] grades = new String[] {"basic", "advanced", "premium"};
        String grade = grades[random.nextInt(grades.length)];
        int count = 1 + random.nextInt(2);

        try {
            trainingService.train(userId, generalId, trainingType, grade, count);
        } catch (Exception e) {
            // food may be insufficient, buy and retry once
            trainingService.buyFood(userId, grade, Math.max(5, count * 3));
            trainingService.train(userId, generalId, trainingType, grade, count);
        }
    }

    private void doProduction(String userId) {
        String[] facilities = new String[] {"silver", "metal", "food", "paper"};
        String f = facilities[random.nextInt(facilities.length)];
        productionService.produce(userId, f);

        String[] manufactureTypes = new String[] {"arsenal", "workshop", "academy"};
        String mType = manufactureTypes[random.nextInt(manufactureTypes.length)];
        List<Recipe> recipes = productionService.getRecipes(mType, 1);
        if (recipes != null && !recipes.isEmpty()) {
            Recipe recipe = recipes.get(random.nextInt(recipes.size()));
            String recipeId = objString(recipe, "id");
            if (!recipeId.isEmpty()) {
                productionService.manufacture(userId, recipeId);
            }
        }
    }

    private void doSecretRealm(String userId, int level) {
        List<SecretRealm> realms = equipmentService.getUnlockedSecretRealms(level);
        if (realms == null || realms.isEmpty()) return;
        SecretRealm realm = realms.get(random.nextInt(realms.size()));
        String realmId = objString(realm, "id");
        if (realmId.isEmpty()) return;
        equipmentService.secretRealmExplore(userId, realmId, level);
    }

    private void doMail(String userId) {
        mailService.claimAll(userId);
        if (chance(0.5)) {
            mailService.readAll(userId);
        }
    }

    private void doShopAndUse(String userId) {
        List<Shop> goods = shopService.getShopGoods("all");
        if (goods != null && !goods.isEmpty()) {
            Collections.shuffle(goods, random);
            int tryCount = Math.min(5, goods.size());
            for (int i = 0; i < tryCount; i++) {
                Shop g = goods.get(i);
                long goodsId = objLong(g, "id", 0L);
                if (goodsId <= 0) continue;
                try {
                    shopService.buyGoods(userId, goodsId, 1);
                    break;
                } catch (Exception ignore) {
                    // try next good
                }
            }
        }

        // Use one item to emulate "buy and consume for growth"
        String[] preferredItemIds = new String[] {
                "11042", "11043", "11044", "11045", // exp symbols
                "11001", "11002",                   // fame symbols
                "11011", "11012", "11013",          // silver
                "11021", "11022", "11023", "11024", // gold/bound gold
                "15011", "15012", "15013"           // recruit tokens
        };
        for (String itemId : preferredItemIds) {
            if (warehouseService.getItemCount(userId, itemId) > 0) {
                warehouseService.useItem(userId, itemId, 1);
                return;
            }
        }
    }

    private void doEquipmentAndFormation(String userId) {
        List<General> generals = generalService.getUserGenerals(userId);
        if (generals == null || generals.isEmpty()) return;

        // adjust formation
        List<String> ids = new ArrayList<>();
        for (General g : generals) {
            String gid = objString(g, "id");
            if (!gid.isEmpty()) ids.add(gid);
        }
        if (!ids.isEmpty()) {
            Collections.shuffle(ids, random);
            formationService.setFormation(userId, ids.subList(0, Math.min(6, ids.size())));
        }

        // equip one bag equipment to one general
        List<Equipment> bag = equipmentService.getUserBagEquipments(userId);
        if (bag == null || bag.isEmpty()) return;
        Equipment eq = bag.get(random.nextInt(bag.size()));
        General g = generals.get(random.nextInt(generals.size()));
        String equipId = objString(eq, "id");
        String gid = objString(g, "id");
        int gl = objInt(g, "level", 1);
        if (equipId.isEmpty() || gid.isEmpty()) return;
        equipmentService.equipToGeneral(userId, equipId, gid, gl);
    }

    private void doRefine(String userId) {
        List<Equipment> all = equipmentService.getUserEquipments(userId);
        if (all == null || all.isEmpty()) return;
        Equipment eq = all.get(random.nextInt(all.size()));
        String equipId = objString(eq, "id");
        if (equipId.isEmpty()) return;

        // try enhance, then quality upgrade
        try {
            refineService.enhance(userId, equipId, false);
        } catch (Exception ignore) {
            // best effort
        }
        refineService.upgradeQuality(userId, equipId);
    }

    private void doNationWar(String userId, String playerName, int level, long power) {
        Map<String, Object> overview = nationWarService.getSessionOverview();
        String phase = str(overview.get("phase"));
        if ("REGISTRATION".equals(phase)) {
            List<Map<String, Object>> attackable = castListFromCities(nationWarService.getAttackableCities(userId));
            if (attackable.isEmpty()) return;
            Map<String, Object> city = attackable.get(random.nextInt(attackable.size()));
            String cityId = str(city.get("id"));
            if (!cityId.isEmpty()) {
                nationWarService.signUpV2(userId, playerName, level, (int) power, cityId);
            }
        } else if ("BATTLE".equals(phase)) {
            Map<String, Object> battleState = nationWarService.getBattleState(userId);
            Map<String, Object> playerState = castMap(battleState.get("playerState"));
            List<Map<String, Object>> availableCities = castList(battleState.get("availableCities"));
            if (playerState == null) {
                if (availableCities != null && !availableCities.isEmpty()) {
                    Map<String, Object> pick = availableCities.get(random.nextInt(availableCities.size()));
                    nationWarService.joinCity(userId, playerName, level, (int) power, str(pick.get("cityId")));
                }
            } else if (boolVal(playerState.get("canSwitch")) && availableCities != null && !availableCities.isEmpty()) {
                Map<String, Object> pick = availableCities.get(random.nextInt(availableCities.size()));
                nationWarService.switchCity(userId, str(pick.get("cityId")));
            }
        }
    }

    private void doAllianceWar(String userId, String playerName, int level, long power) {
        Map<String, Object> status = allianceWarService.getWarStatus(userId);
        String warStatus = str(status.get("status"));
        boolean registered = boolVal(status.get("registered"));
        if ("REGISTERING".equals(warStatus) && !registered) {
            allianceWarService.register(userId, playerName, level, power);
        }
    }

    private String normalizeProfile(String profile) {
        String p = profile == null ? "" : profile.trim().toLowerCase();
        if ("light".equals(p) || "low".equals(p) || "轻度".equals(p)) return "light";
        if ("heavy".equals(p) || "high".equals(p) || "重度".equals(p)) return "heavy";
        return "medium";
    }

    private List<Map<String, Object>> applyServerWeight(List<Map<String, Object>> players, Map<Integer, Double> serverWeights) {
        List<Map<String, Object>> picked = new ArrayList<>(players);
        if (serverWeights == null || serverWeights.isEmpty()) {
            Collections.shuffle(picked, random);
            return picked;
        }

        picked.sort(Comparator.comparingDouble(p -> {
            int serverId = intVal(p.get("serverId"), 1);
            double weight = Math.max(0.05D, serverWeights.getOrDefault(serverId, 1.0D));
            double rand = Math.max(1e-9D, random.nextDouble());
            return -Math.log(rand) / weight;
        }));
        return picked;
    }

    private boolean shouldDo(double baseChance,
                             String profile,
                             boolean isDaytime,
                             boolean isNightPvpTime,
                             String actionType) {
        double profileMul = profileMultiplier(profile);
        double timeMul = timeMultiplier(actionType, isDaytime, isNightPvpTime);
        double p = Math.min(0.95D, Math.max(0.02D, baseChance * profileMul * timeMul));
        return chance(p);
    }

    private double profileMultiplier(String profile) {
        if ("light".equals(profile)) return 0.72D;
        if ("heavy".equals(profile)) return 1.35D;
        return 1.0D;
    }

    private double timeMultiplier(String actionType, boolean isDaytime, boolean isNightPvpTime) {
        if ("production".equals(actionType) || "economy".equals(actionType)) {
            if (isDaytime) return 1.35D;
            if (isNightPvpTime) return 0.82D;
            return 1.0D;
        }
        if ("pvp".equals(actionType)) {
            if (isNightPvpTime) return 1.45D;
            if (isDaytime) return 0.85D;
            return 1.0D;
        }
        return 1.0D;
    }

    private int resolveEquipQuality(Equipment eq) {
        Object q = readObjProp(eq, "quality");
        if (q == null) return 1;
        Object qid = readObjProp(q, "id");
        return intVal(qid, 1);
    }

    private boolean safeAction(Map<String, Integer> actionStats, String action, Runnable runnable) {
        try {
            runnable.run();
            actionStats.put(action, actionStats.getOrDefault(action, 0) + 1);
            return true;
        } catch (Exception e) {
            logger.debug("模拟行为失败 action={} err={}", action, e.getMessage());
            return false;
        }
    }

    private boolean chance(double p) {
        return random.nextDouble() < p;
    }

    private String pickRandomGeneralId(String userId) {
        List<General> generals = generalService.getUserGenerals(userId);
        if (generals == null || generals.isEmpty()) return null;
        General g = generals.get(random.nextInt(generals.size()));
        if (g == null) return null;
        String gid = objString(g, "id");
        return gid.isEmpty() ? null : gid;
    }

    private String resolvePlayerName(Map<String, Object> playerRow, String userId) {
        String lordName = str(playerRow.get("lordName"));
        if (!lordName.isEmpty()) return lordName;
        try {
            String resolved = playerNameResolver.resolve(userId);
            return resolved == null || resolved.isEmpty() ? "主公" : resolved;
        } catch (Exception e) {
            return "主公";
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> castMap(Object obj) {
        if (obj instanceof Map) return (Map<String, Object>) obj;
        return null;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> castList(Object obj) {
        if (obj instanceof List) return (List<Map<String, Object>>) obj;
        return null;
    }

    private List<Map<String, Object>> castListFromCities(List<?> cities) {
        List<Map<String, Object>> result = new ArrayList<>();
        if (cities == null) return result;
        for (Object c : cities) {
            if (!(c instanceof Map)) continue;
            @SuppressWarnings("unchecked")
            Map<String, Object> row = (Map<String, Object>) c;
            result.add(row);
        }
        return result;
    }

    private String str(Object v) {
        return v == null ? "" : String.valueOf(v);
    }

    private int intVal(Object v, int def) {
        if (v == null) return def;
        if (v instanceof Number) return ((Number) v).intValue();
        try {
            return Integer.parseInt(String.valueOf(v));
        } catch (Exception e) {
            return def;
        }
    }

    private long longVal(Object v, long def) {
        if (v == null) return def;
        if (v instanceof Number) return ((Number) v).longValue();
        try {
            return Long.parseLong(String.valueOf(v));
        } catch (Exception e) {
            return def;
        }
    }

    private double doubleVal(Object v, double def) {
        if (v == null) return def;
        if (v instanceof Number) return ((Number) v).doubleValue();
        try {
            return Double.parseDouble(String.valueOf(v));
        } catch (Exception e) {
            return def;
        }
    }

    private boolean boolVal(Object v) {
        if (v instanceof Boolean) return (Boolean) v;
        if (v == null) return false;
        String s = String.valueOf(v);
        return "1".equals(s) || "true".equalsIgnoreCase(s);
    }

    private String objString(Object obj, String prop) {
        Object v = readObjProp(obj, prop);
        return v == null ? "" : String.valueOf(v);
    }

    private int objInt(Object obj, String prop, int def) {
        Object v = readObjProp(obj, prop);
        return intVal(v, def);
    }

    private long objLong(Object obj, String prop, long def) {
        Object v = readObjProp(obj, prop);
        return longVal(v, def);
    }

    private Object readObjProp(Object obj, String prop) {
        if (obj == null || prop == null || prop.isEmpty()) return null;
        try {
            String getter = "get" + Character.toUpperCase(prop.charAt(0)) + prop.substring(1);
            java.lang.reflect.Method m = obj.getClass().getMethod(getter);
            return m.invoke(obj);
        } catch (Exception ignore) {
            try {
                java.lang.reflect.Field f = obj.getClass().getDeclaredField(prop);
                f.setAccessible(true);
                return f.get(obj);
            } catch (Exception ignore2) {
                return null;
            }
        }
    }
}
