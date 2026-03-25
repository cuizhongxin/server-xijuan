package com.tencent.wxcloudrun.service.cornucopia;

import com.tencent.wxcloudrun.dao.CornucopiaMapper;
import com.tencent.wxcloudrun.exception.BusinessException;
import com.tencent.wxcloudrun.service.UserResourceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 聚宝盆 —— APK 彩票制还原
 *
 * 规则（对齐APK UICorn_BG.jpg）：
 * - 每周二、四、六 22:00 自动开奖
 * - 每购买1份 → 获得1个系统号码 + 奖池增加10绑金
 * - 每期个人限买10份
 * - 开奖前1小时内不能购买
 * - 特等奖 = 奖池50%，一等奖 = 20%，10%滚入下期，20%为系统收益
 */
@Service
public class CornucopiaService {

    private static final Logger logger = LoggerFactory.getLogger(CornucopiaService.class);

    private static final int MAX_TICKETS_PER_PERIOD = 10;
    private static final long POOL_ADD_PER_TICKET = 10;
    private static final int LOCK_HOURS_BEFORE_DRAW = 1;
    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");
    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final String FORTUNE_TALISMAN_ID = "11104";

    @Autowired
    private CornucopiaMapper mapper;

    @Autowired
    private UserResourceService userResourceService;

    @Autowired
    private com.tencent.wxcloudrun.service.warehouse.WarehouseService warehouseService;

    // ══════════════════════ 查询信息 ══════════════════════

    public Map<String, Object> getInfo(String userId) {
        ensureActivePeriod();
        Map<String, Object> period = mapper.findActivePeriod();
        Map<String, Object> lastDrawn = mapper.findLastDrawnPeriod();

        Map<String, Object> result = new LinkedHashMap<>();
        if (period != null) {
            long periodId = ((Number) period.get("periodNum")).longValue();
            long pid = ((Number) period.get("id")).longValue();
            result.put("periodNum", periodId);
            result.put("prizePool", period.get("prizePool"));
            result.put("drawTime", String.valueOf(period.get("drawTime")));
            result.put("status", period.get("status"));

            LocalDateTime drawDt = parseDrawTime(period.get("drawTime"));
            LocalDateTime now = LocalDateTime.now(ZONE);
            long secondsLeft = Duration.between(now, drawDt).getSeconds();
            result.put("secondsToDrawTime", Math.max(0, secondsLeft));

            boolean locked = secondsLeft <= LOCK_HOURS_BEFORE_DRAW * 3600L && secondsLeft > 0;
            result.put("purchaseLocked", locked);

            int userTicketCount = mapper.countUserTickets(userId, pid);
            List<Map<String, Object>> myTickets = mapper.findUserTickets(userId, pid);
            result.put("myTicketCount", userTicketCount);
            result.put("maxTickets", MAX_TICKETS_PER_PERIOD);
            result.put("myTickets", myTickets);
        }

        if (lastDrawn != null) {
            Map<String, Object> last = new LinkedHashMap<>();
            last.put("periodNum", lastDrawn.get("periodNum"));
            last.put("grandNumber", lastDrawn.get("grandNumber"));
            last.put("firstNumber", lastDrawn.get("firstNumber"));
            last.put("grandPrize", lastDrawn.get("grandPrize"));
            last.put("firstPrize", lastDrawn.get("firstPrize"));
            last.put("grandWinnerId", lastDrawn.get("grandWinnerId"));
            last.put("firstWinnerId", lastDrawn.get("firstWinnerId"));
            result.put("lastPeriod", last);
        }

        result.put("fortuneTalismanCount", warehouseService.getItemCount(userId, FORTUNE_TALISMAN_ID));

        return result;
    }

    // ══════════════════════ 购票 ══════════════════════

    @Transactional
    public Map<String, Object> buyTicket(String userId, int count) {
        if (count < 1 || count > MAX_TICKETS_PER_PERIOD) {
            throw new BusinessException(400, "购买数量无效（1-" + MAX_TICKETS_PER_PERIOD + "）");
        }

        ensureActivePeriod();
        Map<String, Object> period = mapper.findActivePeriod();
        if (period == null) throw new BusinessException(400, "当前无进行中的期数");

        long pid = ((Number) period.get("id")).longValue();
        LocalDateTime drawDt = parseDrawTime(period.get("drawTime"));
        LocalDateTime now = LocalDateTime.now(ZONE);
        long secondsLeft = Duration.between(now, drawDt).getSeconds();
        if (secondsLeft <= LOCK_HOURS_BEFORE_DRAW * 3600L && secondsLeft > 0) {
            throw new BusinessException(400, "开奖前1小时内不能购买");
        }

        int owned = mapper.countUserTickets(userId, pid);
        if (owned + count > MAX_TICKETS_PER_PERIOD) {
            throw new BusinessException(400, "每期最多购买" + MAX_TICKETS_PER_PERIOD + "份，已购" + owned + "份");
        }

        boolean consumed = warehouseService.consumeItem(userId, FORTUNE_TALISMAN_ID, count);
        if (!consumed) {
            throw new BusinessException(400, "招财符不足，需要" + count + "张（可在商店购买）");
        }

        List<String> numbers = new ArrayList<>();
        Random rng = new Random();
        for (int i = 0; i < count; i++) {
            String num = String.format("%04d", rng.nextInt(10000));
            mapper.insertTicket(userId, pid, num);
            numbers.add(num);
        }
        mapper.addToPool(pid, POOL_ADD_PER_TICKET * count);

        logger.info("用户 {} 购买聚宝盆 {}份, 消耗招财符x{}, 号码: {}", userId, count, count, numbers);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);
        result.put("numbers", numbers);
        result.put("costItem", "招财符 x" + count);
        result.put("myTicketCount", owned + count);
        result.put("fortuneTalismanCount", warehouseService.getItemCount(userId, FORTUNE_TALISMAN_ID));
        Map<String, Object> updatedPeriod = mapper.findPeriodById(pid);
        result.put("prizePool", updatedPeriod != null ? updatedPeriod.get("prizePool") : 0);
        return result;
    }

    // ══════════════════════ 定时开奖 ══════════════════════

    @Scheduled(cron = "0 0 22 * * TUE,THU,SAT", zone = "Asia/Shanghai")
    public void scheduledDraw() {
        logger.info("聚宝盆定时开奖触发");
        try {
            executeDraw();
        } catch (Exception e) {
            logger.error("聚宝盆开奖异常", e);
        }
    }

    @Transactional
    public Map<String, Object> executeDraw() {
        Map<String, Object> period = mapper.findActivePeriod();
        if (period == null) {
            logger.warn("无进行中的期数，跳过开奖");
            return Collections.singletonMap("message", "无进行中的期数");
        }

        long pid = ((Number) period.get("id")).longValue();
        int periodNum = ((Number) period.get("periodNum")).intValue();
        long pool = ((Number) period.get("prizePool")).longValue();

        List<Map<String, Object>> allTickets = mapper.findAllTicketsByPeriod(pid);
        if (allTickets.isEmpty()) {
            long carryover = pool;
            mapper.finishDraw(pid, "----", "----", null, null, 0, 0);
            createNextPeriod(carryover);
            logger.info("第{}期无人购票，奖池 {} 全额滚入下期", periodNum, carryover);
            return Collections.singletonMap("message", "无人参与，奖池滚入下期");
        }

        Random rng = new Random();
        List<String> allNumbers = new ArrayList<>();
        for (Map<String, Object> t : allTickets) allNumbers.add((String) t.get("ticketNumber"));

        int grandIdx = rng.nextInt(allTickets.size());
        String grandNumber = allNumbers.get(grandIdx);
        String grandWinnerId = (String) allTickets.get(grandIdx).get("userId");

        int firstIdx = grandIdx;
        if (allTickets.size() > 1) {
            while (firstIdx == grandIdx) firstIdx = rng.nextInt(allTickets.size());
        }
        String firstNumber = allNumbers.get(firstIdx);
        String firstWinnerId = (String) allTickets.get(firstIdx).get("userId");

        long grandPrize = (long) (pool * 0.50);
        long firstPrize = (long) (pool * 0.20);
        long carryover = (long) (pool * 0.10);

        mapper.finishDraw(pid, grandNumber, firstNumber, grandWinnerId, firstWinnerId, grandPrize, firstPrize);

        if (grandPrize > 0) userResourceService.addBoundGold(grandWinnerId, grandPrize);
        if (firstPrize > 0) userResourceService.addBoundGold(firstWinnerId, firstPrize);

        createNextPeriod(carryover);

        logger.info("第{}期开奖完成: 奖池={}, 特等奖={}({}), 一等奖={}({}), 结转={}",
                periodNum, pool, grandNumber, grandWinnerId, firstNumber, firstWinnerId, carryover);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("periodNum", periodNum);
        result.put("grandNumber", grandNumber);
        result.put("grandWinnerId", grandWinnerId);
        result.put("grandPrize", grandPrize);
        result.put("firstNumber", firstNumber);
        result.put("firstWinnerId", firstWinnerId);
        result.put("firstPrize", firstPrize);
        result.put("carryover", carryover);
        return result;
    }

    // ══════════════════════ 期数管理 ══════════════════════

    private void ensureActivePeriod() {
        Map<String, Object> active = mapper.findActivePeriod();
        if (active != null) {
            LocalDateTime drawDt = parseDrawTime(active.get("drawTime"));
            if (drawDt.isBefore(LocalDateTime.now(ZONE))) {
                executeDraw();
            }
            return;
        }
        Map<String, Object> last = mapper.findLastDrawnPeriod();
        long carryover = 0;
        if (last != null) {
            long pool = ((Number) last.get("prizePool")).longValue();
            carryover = (long) (pool * 0.10);
        }
        createNextPeriod(carryover);
    }

    private void createNextPeriod(long carryover) {
        LocalDateTime next = nextDrawTime();
        Map<String, Object> last = mapper.findLastDrawnPeriod();
        int nextNum = 1;
        if (last != null && last.get("periodNum") != null) {
            nextNum = ((Number) last.get("periodNum")).intValue() + 1;
        }
        mapper.insertPeriod(nextNum, next.format(DT_FMT), carryover);
        logger.info("创建聚宝盆第{}期, 开奖时间: {}, 结转: {}", nextNum, next, carryover);
    }

    private LocalDateTime nextDrawTime() {
        LocalDateTime now = LocalDateTime.now(ZONE);
        DayOfWeek dow = now.getDayOfWeek();

        int[] drawDays = { DayOfWeek.TUESDAY.getValue(), DayOfWeek.THURSDAY.getValue(), DayOfWeek.SATURDAY.getValue() };
        for (int d : drawDays) {
            int diff = d - dow.getValue();
            if (diff < 0) diff += 7;
            LocalDateTime candidate = now.toLocalDate().plusDays(diff).atTime(22, 0);
            if (candidate.isAfter(now)) return candidate;
        }
        int diff = drawDays[0] - dow.getValue();
        if (diff <= 0) diff += 7;
        return now.toLocalDate().plusDays(diff).atTime(22, 0);
    }

    private LocalDateTime parseDrawTime(Object drawTimeObj) {
        if (drawTimeObj instanceof LocalDateTime) return (LocalDateTime) drawTimeObj;
        String s = String.valueOf(drawTimeObj);
        if (s.length() > 19) s = s.substring(0, 19);
        return LocalDateTime.parse(s, DT_FMT);
    }
}
