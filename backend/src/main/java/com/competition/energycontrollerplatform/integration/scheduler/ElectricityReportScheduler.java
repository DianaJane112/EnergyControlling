package com.competition.energycontrollerplatform.integration.scheduler;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.competition.energycontrollerplatform.common.RedisDistributedLock;
import com.competition.energycontrollerplatform.entity.DailyFreezeRecord;
import com.competition.energycontrollerplatform.entity.Meter;
import com.competition.energycontrollerplatform.entity.MeterReading;
import com.competition.energycontrollerplatform.entity.ReportDailyElectricity;
import com.competition.energycontrollerplatform.mapper.DailyFreezeRecordMapper;
import com.competition.energycontrollerplatform.mapper.MeterMapper;
import com.competition.energycontrollerplatform.mapper.MeterReadingMapper;
import com.competition.energycontrollerplatform.mapper.ReportDailyElectricityMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 用电量日报表定时任务。
 * <p>
 * 每天凌晨 1:00 执行，为每一台表计计算前一日的用电量，
 * 写入预聚合表 t_report_daily_electricity，避免前端查询时全表扫描历史读数。
 * <p>
 * <b>计算逻辑：</b>
 * <pre>{@code
 * 当日用电量 = (前一天23:59:59的最新读数) - (再前一天23:59:59的最新读数)
 * }</pre>
 * 差值 <= 0 或读数缺失 → 跳过（不写入，避免无效数据）。
 */
@Component
@RequiredArgsConstructor
public class ElectricityReportScheduler {

    private static final Logger log = LoggerFactory.getLogger(ElectricityReportScheduler.class);

    private final MeterMapper meterMapper;
    private final MeterReadingMapper meterReadingMapper;
    private final ReportDailyElectricityMapper reportDailyElectricityMapper;
    private final DailyFreezeRecordMapper dailyFreezeRecordMapper;
    private final RedisDistributedLock redisDistributedLock;
    private final JdbcTemplate jdbcTemplate;
    private final PlatformTransactionManager transactionManager;

    /** 分布式锁 key：多实例部署时确保只有一台执行日报聚合 */
    private static final String REPORT_LOCK_KEY = "lock:report:daily";
    /** 月度聚合锁 key */
    private static final String MONTHLY_LOCK_KEY = "lock:report:monthly";
    /** 锁超时时间（秒），需大于日报聚合任务最长可能执行时间 */
    private static final long REPORT_LOCK_TIMEOUT_SEC = 300;
    /** 月度聚合锁超时（秒），月度聚合涉及全表扫描，需要更长时间 */
    private static final long MONTHLY_LOCK_TIMEOUT_SEC = 600;
    /** 月度批写入每批条数 */
    private static final int MONTHLY_BATCH_SIZE = 500;

    /** 月度聚合 INSERT ... ON DUPLICATE KEY UPDATE SQL（模板） */
    private static final String MONTHLY_UPSERT_SQL =
            "INSERT INTO tb_meter_monthly_report "
            + "(project_id, group_id, merchant_id, device_no, meter_id, meter_type, "
            + " total_usage, peak_usage, busy_usage, normal_usage, idle_usage, "
            + " forward_start_reading, forward_start_time, forward_end_reading, forward_end_time, forward_usage, "
            + " reverse_start_reading, reverse_start_time, reverse_end_reading, reverse_end_time, reverse_usage, reverse_ratio, "
            + " total_start_reading, total_end_reading, "
            + " previous_balance, current_balance, "
            + " data_update_time, stat_month, stat_year, daily_count) "
            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) "
            + "ON DUPLICATE KEY UPDATE "
            + "total_usage = VALUES(total_usage), peak_usage = VALUES(peak_usage), "
            + "busy_usage = VALUES(busy_usage), normal_usage = VALUES(normal_usage), "
            + "idle_usage = VALUES(idle_usage), "
            + "forward_start_reading = VALUES(forward_start_reading), "
            + "forward_end_reading = VALUES(forward_end_reading), forward_usage = VALUES(forward_usage), "
            + "reverse_start_reading = VALUES(reverse_start_reading), "
            + "reverse_end_reading = VALUES(reverse_end_reading), reverse_usage = VALUES(reverse_usage), "
            + "reverse_ratio = VALUES(reverse_ratio), "
            + "total_start_reading = VALUES(total_start_reading), "
            + "total_end_reading = VALUES(total_end_reading), "
            + "previous_balance = VALUES(previous_balance), "
            + "current_balance = VALUES(current_balance), "
            + "daily_count = VALUES(daily_count), "
            + "data_update_time = NOW(), update_time = NOW()";

    /**
     * 每天凌晨 1:00 自动执行。
     * <p>cron = "秒 分 时 日 月 星期"</p>
     * <p>使用 Redis 分布式锁防止多实例重复写入预聚合数据。</p>
     */
    @Scheduled(cron = "0 0 1 * * ?")
    @Transactional(rollbackFor = Exception.class)
    public void aggregateDailyElectricity() {
        // ── 分布式锁：防止多实例并发执行 ──
        String lockToken = redisDistributedLock.tryLock(REPORT_LOCK_KEY, REPORT_LOCK_TIMEOUT_SEC);
        if (lockToken == null) {
            log.info("[日报聚合] 本次聚合由其他实例执行，当前实例跳过");
            return;
        }

        try {
            LocalDate today = LocalDate.now();
            LocalDate yesterday = today.minusDays(1);
            LocalDate dayBeforeYesterday = yesterday.minusDays(1);

        log.info("[日报聚合] 开始聚合 {} 的用电量数据", yesterday);

        // ── 查所有有效表计 ──
        LambdaQueryWrapper<Meter> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Meter::getIsDeleted, 0);
        List<Meter> allMeters = meterMapper.selectList(wrapper);

        if (allMeters.isEmpty()) {
            log.info("[日报聚合] 无有效表计，跳过");
            return;
        }

        // ── 时间边界 ──
        LocalDateTime yesterdayEnd = LocalDateTime.of(yesterday, LocalTime.MAX);       // 昨天 23:59:59
        LocalDateTime dayBeforeEnd = LocalDateTime.of(dayBeforeYesterday, LocalTime.MAX); // 前天 23:59:59

        List<ReportDailyElectricity> batchList = new ArrayList<>();
        int skipCount = 0;

        for (Meter meter : allMeters) {
            Long meterId = meter.getId();

            // ① 前一天 23:59:59 之前的最新读数
            MeterReading yesterdayReading = meterReadingMapper.getLatestBeforeTime(meterId, yesterdayEnd);

            // ② 再前一天 23:59:59 之前的最新读数
            MeterReading dayBeforeReading = meterReadingMapper.getLatestBeforeTime(meterId, dayBeforeEnd);

            // ③ 任一缺失 → 跳过
            if (yesterdayReading == null || dayBeforeReading == null) {
                skipCount++;
                continue;
            }

            // ④ 提取读数（优先 kwh 字段，其次 read_val）
            BigDecimal yesterdayKwh = extractKwh(yesterdayReading);
            BigDecimal dayBeforeKwh = extractKwh(dayBeforeReading);

            if (yesterdayKwh == null || dayBeforeKwh == null) {
                skipCount++;
                continue;
            }

            // ⑤ 差值 = 昨天 - 前天
            BigDecimal consumed = yesterdayKwh.subtract(dayBeforeKwh);
            if (consumed.compareTo(BigDecimal.ZERO) <= 0) {
                // 读数倒转或未增长，跳过
                skipCount++;
                continue;
            }

            // ⑥ 复费率：尖峰平谷各时段差值
            BigDecimal peakConsumed = safeSubtract(yesterdayReading.getKwhPeak(), dayBeforeReading.getKwhPeak());
            BigDecimal busyConsumed = safeSubtract(yesterdayReading.getKwhBusy(), dayBeforeReading.getKwhBusy());
            BigDecimal normalConsumed = safeSubtract(yesterdayReading.getKwhNormal(), dayBeforeReading.getKwhNormal());
            BigDecimal idleConsumed = safeSubtract(yesterdayReading.getKwhIdle(), dayBeforeReading.getKwhIdle());

            // ⑦ 组装预聚合记录
            ReportDailyElectricity report = new ReportDailyElectricity();
            report.setReportDate(yesterday);
            report.setMeterId(meterId);
            report.setCustomerId(meter.getCustomerId());
            report.setGroupId(0L); // 分组暂时默认为0，后续可从 t_shadow_meter 推导
            report.setElectricityConsumed(consumed);
            report.setPeakConsumed(peakConsumed);
            report.setBusyConsumed(busyConsumed);
            report.setNormalConsumed(normalConsumed);
            report.setIdleConsumed(idleConsumed);
            // ⑧ 正向/反向有功（从 t_daily_freeze_record 推算，缺失则正向=总量，反向=0）
            BigDecimal forwardConsumed = tryGetForwardReverse(meterId, yesterday, dayBeforeYesterday, true);
            BigDecimal reverseConsumed = tryGetForwardReverse(meterId, yesterday, dayBeforeYesterday, false);
            if (forwardConsumed.compareTo(BigDecimal.ZERO) <= 0 && reverseConsumed.compareTo(BigDecimal.ZERO) <= 0) {
                // 无分项数据时，默认全部为正向有功
                forwardConsumed = consumed;
                reverseConsumed = BigDecimal.ZERO;
            }
            report.setForwardConsumed(forwardConsumed);
            report.setReverseConsumed(reverseConsumed);

            report.setPeakChargeAmount(BigDecimal.ZERO);
            report.setBusyChargeAmount(BigDecimal.ZERO);
            report.setNormalChargeAmount(BigDecimal.ZERO);
            report.setIdleChargeAmount(BigDecimal.ZERO);
            report.setChargeAmount(BigDecimal.ZERO); // 实际扣费由 AutomatedBillingScheduler 负责

            batchList.add(report);
        }

        // ⑧ 批量 INSERT（在同一事务内逐条写入，利用事务批量提交）
        if (!batchList.isEmpty()) {
            for (ReportDailyElectricity report : batchList) {
                reportDailyElectricityMapper.insert(report);
            }
            log.info("[日报聚合] 完成！写入 {} 条，跳过 {} 条（读数缺失/无增长）",
                    batchList.size(), skipCount);
        } else {
            log.info("[日报聚合] 无有效数据写入，跳过 {} 条", skipCount);
        }

        // ⑨ 数据完整性校验：对比日报写入行数与昨日有效读数表计数的偏差
        checkDailyIntegrity(yesterday, batchList.size(), allMeters.size());
        } finally {
            redisDistributedLock.unlock(REPORT_LOCK_KEY, lockToken);
        }
    }

    /**
     * 数据完整性校验：日报写入行数是否与预期一致。
     * <p>仅记录 WARN 日志，不阻塞任务流程。运维可根据日志判断数据采集是否正常。</p>
     *
     * @param date        统计日期
     * @param writtenRows 实际写入日报行数
     * @param totalMeters 有效表计总数
     */
    private void checkDailyIntegrity(LocalDate date, int writtenRows, int totalMeters) {
        try {
            Integer readingMeters = jdbcTemplate.queryForObject(
                    "SELECT COUNT(DISTINCT meter_id) FROM t_meter_reading "
                    + "WHERE read_time >= ? AND read_time < ?",
                    Integer.class,
                    date.atStartOfDay(),
                    date.plusDays(1).atStartOfDay());
            int readingCount = readingMeters != null ? readingMeters : 0;
            if (readingCount > 0 && writtenRows < readingCount / 2) {
                log.warn("[数据完整性] {} 日报写入 {} 条，但昨日有 {} 个表计产生读数，"
                        + "写入量不足读数表计数的一半，可能存在数据采集或计算异常",
                        date, writtenRows, readingCount);
            }
        } catch (Exception e) {
            log.warn("[数据完整性] 校验失败（非关键，不阻塞）: {}", e.getMessage());
        }
    }

    /**
     * 每月 1 号凌晨 2:00 自动执行。
     * <p>对上一自然月做月度预聚合，写入 {@code tb_meter_monthly_report}，
     * 供月度报表查询使用。</p>
     * <p>
     * <b>架构设计：读写分离 + 分批小事务</b>
     * <ol>
     *   <li>Phase 1 — 纯读（零事务锁）：6 次批量 SELECT，全部数据载入内存 Map</li>
     *   <li>Phase 2 — 内存拼接：循环仅做数据组装，零数据库调用</li>
     *   <li>Phase 3 — 分批写入：每 {@value #MONTHLY_BATCH_SIZE} 条一个独立小事务，
     *       失败只回滚当前批次，不影响已提交的</li>
     * </ol>
     *
     * @implNote 不使用 {@code @Transactional}——长事务会阻塞充值/扣费接口。
     *           写入阶段通过 {@link TransactionTemplate} 以 {@code REQUIRES_NEW} 分批提交。
     */
    @Scheduled(cron = "0 0 2 1 * ?")
    public void aggregateMonthlyElectricity() {
        String lockToken = redisDistributedLock.tryLock(MONTHLY_LOCK_KEY, MONTHLY_LOCK_TIMEOUT_SEC);
        if (lockToken == null) {
            log.info("[月度聚合] 本次聚合由其他实例执行，当前实例跳过");
            return;
        }

        try {
            YearMonth lastMonth = YearMonth.now().minusMonths(1);
            String statMonth = lastMonth.toString();
            String statYear = String.valueOf(lastMonth.getYear());
            LocalDate monthStart = lastMonth.atDay(1);
            LocalDate monthEnd = lastMonth.atEndOfMonth();

            log.info("[月度聚合] 开始聚合 {}（{} ~ {}）", statMonth, monthStart, monthEnd);

            // ═══════════════════════ Phase 1: 批量 SELECT（无事务锁） ═══════════════════════

            // 1a. 查询上月所有有日报数据的表计（含已删除），同时拿到用量聚合结果
            List<Map<String, Object>> monthlyRows = jdbcTemplate.queryForList(
                    "SELECT rde.meter_id, rde.customer_id, rde.group_id, "
                    + "       COALESCE(SUM(rde.electricity_consumed), 0) AS total_usage, "
                    + "       COALESCE(SUM(rde.peak_consumed), 0) AS peak_usage, "
                    + "       COALESCE(SUM(rde.busy_consumed), 0) AS busy_usage, "
                    + "       COALESCE(SUM(rde.normal_consumed), 0) AS normal_usage, "
                    + "       COALESCE(SUM(rde.idle_consumed), 0) AS idle_usage, "
                    + "       COUNT(DISTINCT rde.report_date) AS daily_count "
                    + "FROM t_report_daily_electricity rde "
                    + "WHERE rde.report_date >= ? AND rde.report_date <= ? "
                    + "GROUP BY rde.meter_id, rde.customer_id, rde.group_id",
                    monthStart, monthEnd);

            if (monthlyRows.isEmpty()) {
                log.info("[月度聚合] {} 无日报数据，跳过", statMonth);
                return;
            }

            // 收集所有 meter_id 和 customer_id
            Set<Long> allMeterIds = new HashSet<>();
            Set<Long> allCustomerIds = new HashSet<>();
            for (Map<String, Object> row : monthlyRows) {
                Long mid = toLong(row.get("meter_id"));
                Long cid = toLong(row.get("customer_id"));
                if (mid != null) allMeterIds.add(mid);
                if (cid != null) allCustomerIds.add(cid);
            }

            // 1b. 批量查表计信息（含已删除）
            Map<Long, Map<String, Object>> meterMap = new HashMap<>();
            if (!allMeterIds.isEmpty()) {
                List<Long> meterIdList = new ArrayList<>(allMeterIds);
                List<Map<String, Object>> meterRows = batchQueryByIds(
                        "SELECT m.id, m.steal_no, m.db_no, m.meter_type, m.customer_id, "
                        + "m.pro_id, m.group_id, m.is_deleted FROM t_meter m",
                        "m.id", meterIdList);
                for (Map<String, Object> mr : meterRows) {
                    meterMap.put(toLong(mr.get("id")), mr);
                }
            }

            // 1c. 批量查月初第一条 / 月末最后一条读数（窗口函数，每表计最多 2 行）
            Map<Long, Map<String, Object>> firstReadingMap = new HashMap<>();
            Map<Long, Map<String, Object>> lastReadingMap = new HashMap<>();
            if (!allMeterIds.isEmpty()) {
                String inPlaceholders = placeholders(allMeterIds.size());
                List<Object> params = new ArrayList<>();
                params.addAll(allMeterIds);
                params.add(monthStart.atStartOfDay());
                params.add(monthEnd.plusDays(1).atStartOfDay());

                List<Map<String, Object>> readingRows = jdbcTemplate.queryForList(
                        "SELECT * FROM ("
                        + "  SELECT meter_id, kwh, read_val, read_time, "
                        + "    ROW_NUMBER() OVER (PARTITION BY meter_id ORDER BY read_time ASC) AS rn_asc, "
                        + "    ROW_NUMBER() OVER (PARTITION BY meter_id ORDER BY read_time DESC) AS rn_desc "
                        + "  FROM t_meter_reading "
                        + "  WHERE meter_id IN (" + inPlaceholders + ") "
                        + "    AND read_time >= ? AND read_time < ?"
                        + ") t WHERE rn_asc = 1 OR rn_desc = 1",
                        params.toArray());

                for (Map<String, Object> rr : readingRows) {
                    Long mid = toLong(rr.get("meter_id"));
                    Number rnAsc = (Number) rr.get("rn_asc");
                    Number rnDesc = (Number) rr.get("rn_desc");
                    if (rnAsc != null && rnAsc.intValue() == 1) {
                        firstReadingMap.put(mid, rr);
                    }
                    if (rnDesc != null && rnDesc.intValue() == 1) {
                        lastReadingMap.put(mid, rr);
                    }
                }
            }

            // 1d. 批量查冻结记录（每表计月初+月末各一条）
            Map<Long, Map<String, Object>> freezeFirstMap = new HashMap<>();
            Map<Long, Map<String, Object>> freezeLastMap = new HashMap<>();
            if (!allMeterIds.isEmpty()) {
                String inPlaceholders = placeholders(allMeterIds.size());
                List<Object> params = new ArrayList<>();
                params.addAll(allMeterIds);
                params.add(monthStart);
                params.add(monthEnd);

                List<Map<String, Object>> freezeRows = jdbcTemplate.queryForList(
                        "SELECT meter_id, record_date, zxyg, fxyg "
                        + "FROM t_daily_freeze_record "
                        + "WHERE meter_id IN (" + inPlaceholders + ") "
                        + "AND record_date IN (?, ?) "
                        + "ORDER BY meter_id, record_date ASC",
                        params.toArray());

                for (Map<String, Object> fr : freezeRows) {
                    Long mid = toLong(fr.get("meter_id"));
                    // 第一条遇到的是月初，最后覆盖的是月末（同 meter 的已按 record_date ASC 排序）
                    if (!freezeFirstMap.containsKey(mid)) {
                        freezeFirstMap.put(mid, fr);
                    }
                    freezeLastMap.put(mid, fr); // 不断覆盖，最终保留最后一条
                }
            }

            // 1e. 批量查商户余额
            Map<Long, BigDecimal> balanceMap = new HashMap<>();
            if (!allCustomerIds.isEmpty()) {
                List<Long> custIdList = new ArrayList<>(allCustomerIds);
                List<Map<String, Object>> balanceRows = batchQueryByIds(
                        "SELECT id, balance FROM t_customer", "id", custIdList);
                for (Map<String, Object> br : balanceRows) {
                    balanceMap.put(toLong(br.get("id")), toBigDecimal(br.get("balance")));
                }
            }

            log.info("[月度聚合] Phase 1 完成：{} 表计, {} 商户, 读数/冻结/余额已加载",
                    allMeterIds.size(), allCustomerIds.size());

            // ═══════════════════════ Phase 2: 内存拼接（零数据库调用） ═══════════════════════

            List<Object[]> allParams = new ArrayList<>();
            int skipCount = 0;

            for (Map<String, Object> row : monthlyRows) {
                Long meterId = toLong(row.get("meter_id"));
                Long customerId = toLong(row.get("customer_id"));
                Long groupId = toLong(row.get("group_id"));
                BigDecimal totalUsage = toBigDecimal(row.get("total_usage"));
                BigDecimal peakUsage = toBigDecimal(row.get("peak_usage"));
                BigDecimal busyUsage = toBigDecimal(row.get("busy_usage"));
                BigDecimal normalUsage = toBigDecimal(row.get("normal_usage"));
                BigDecimal idleUsage = toBigDecimal(row.get("idle_usage"));
                int dailyCount = ((Number) row.get("daily_count")).intValue();

                // 表计信息（从批量查询的 Map 取）
                Map<String, Object> meterRow = meterMap.get(meterId);
                if (meterRow == null) {
                    log.debug("[月度聚合] meterId={} 无表计信息，跳过", meterId);
                    skipCount++;
                    continue;
                }

                // deviceNo：优先 steal_no → db_no → 空字符串（防止 String.valueOf(null) 产生 "null"）
                String deviceNo;
                Object stealNoObj = meterRow.get("steal_no");
                Object dbNoObj = meterRow.get("db_no");
                if (stealNoObj != null) {
                    deviceNo = String.valueOf(stealNoObj);
                } else if (dbNoObj != null) {
                    deviceNo = String.valueOf(dbNoObj);
                } else {
                    deviceNo = "";
                }

                Integer meterType = meterRow.get("meter_type") != null
                        ? ((Number) meterRow.get("meter_type")).intValue() : 1;

                // 月初/月末读数
                Map<String, Object> firstR = firstReadingMap.get(meterId);
                Map<String, Object> lastR = lastReadingMap.get(meterId);
                BigDecimal totalStart = null, totalEnd = null;
                if (firstR != null) {
                    totalStart = toBigDecimal(
                            firstR.get("kwh") != null ? firstR.get("kwh") : firstR.get("read_val"));
                }
                if (lastR != null) {
                    totalEnd = toBigDecimal(
                            lastR.get("kwh") != null ? lastR.get("kwh") : lastR.get("read_val"));
                }

                // 正向/反向有功起止示数
                Map<String, Object> firstFr = freezeFirstMap.get(meterId);
                Map<String, Object> lastFr = freezeLastMap.get(meterId);
                BigDecimal forwardStart = null, forwardEnd = null;
                BigDecimal reverseStart = null, reverseEnd = null;
                if (firstFr != null) {
                    forwardStart = toBigDecimal(firstFr.get("zxyg"));
                    reverseStart = toBigDecimal(firstFr.get("fxyg"));
                }
                if (lastFr != null) {
                    forwardEnd = toBigDecimal(lastFr.get("zxyg"));
                    reverseEnd = toBigDecimal(lastFr.get("fxyg"));
                }

                // 余额快照
                BigDecimal currentBalance = balanceMap.getOrDefault(customerId, BigDecimal.ZERO);
                // 商户不存在于 t_customer → 跳过，不写入伪造数据
                if (!balanceMap.containsKey(customerId) && customerId != null) {
                    log.warn("[月度聚合] customerId={} 商户不存在于 t_customer，"
                            + "跳过 meterId={} 的月度记录", customerId, meterId);
                    skipCount++;
                    continue;
                }

                // 上期余额 = 当前余额 + 本月总扣费（近似）
                BigDecimal monthCharge = BigDecimal.ZERO;
                try {
                    Double chargeVal = jdbcTemplate.queryForObject(
                            "SELECT COALESCE(SUM(charge_amount), 0) FROM t_report_daily_electricity "
                            + "WHERE meter_id = ? AND report_date >= ? AND report_date <= ?",
                            Double.class, meterId, monthStart, monthEnd);
                    if (chargeVal != null) monthCharge = BigDecimal.valueOf(chargeVal);
                } catch (Exception e) {
                    log.warn("[月度聚合] meterId={} 月扣费查询异常，使用 0 代替: {}",
                            meterId, e.getMessage());
                }
                BigDecimal previousBalance = currentBalance.add(monthCharge);

                // 正向/反向用量
                BigDecimal forwardUsage = (forwardStart != null && forwardEnd != null)
                        ? forwardEnd.subtract(forwardStart).max(BigDecimal.ZERO) : null;
                BigDecimal reverseUsage = (reverseStart != null && reverseEnd != null)
                        ? reverseEnd.subtract(reverseStart).max(BigDecimal.ZERO) : null;
                BigDecimal reverseRatio = null;
                if (reverseUsage != null && totalUsage.compareTo(BigDecimal.ZERO) > 0) {
                    reverseRatio = reverseUsage.divide(totalUsage, 4, RoundingMode.HALF_UP);
                }

                // 组装参数数组
                Object[] params = new Object[]{
                        meterRow.get("pro_id"), groupId, customerId, deviceNo, meterId,
                        meterType == 1 ? "ELECTRIC" : "WATER",
                        totalUsage, peakUsage, busyUsage, normalUsage, idleUsage,
                        forwardStart, null, forwardEnd, null, forwardUsage,
                        reverseStart, null, reverseEnd, null, reverseUsage, reverseRatio,
                        totalStart, totalEnd,
                        previousBalance, currentBalance,
                        LocalDateTime.now(), statMonth, statYear, dailyCount
                };
                allParams.add(params);
            }

            if (allParams.isEmpty()) {
                log.info("[月度聚合] {} 无有效数据（跳过 {} 条），不写入", statMonth, skipCount);
                return;
            }

            log.info("[月度聚合] Phase 2 完成：{} 条有效记录，跳过 {} 条", allParams.size(), skipCount);

            // ═══════════════════════ Phase 3: 分批小事务写入 ═══════════════════════

            TransactionTemplate txTemplate = new TransactionTemplate(transactionManager);
            txTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
            txTemplate.setTimeout(30); // 单个 batch 最多 30 秒

            int totalWritten = 0;
            for (int i = 0; i < allParams.size(); i += MONTHLY_BATCH_SIZE) {
                int end = Math.min(i + MONTHLY_BATCH_SIZE, allParams.size());
                List<Object[]> batch = allParams.subList(i, end);
                final int batchNum = (i / MONTHLY_BATCH_SIZE) + 1;

                try {
                    txTemplate.execute(status -> {
                        jdbcTemplate.batchUpdate(MONTHLY_UPSERT_SQL, batch);
                        return null;
                    });
                    totalWritten += batch.size();
                    log.info("[月度聚合] batch {} 提交成功（{} 条），进度 {}/{}",
                            batchNum, batch.size(), end, allParams.size());
                } catch (Exception e) {
                    log.error("[月度聚合] batch {} 写入失败（{} 条），"
                            + "已回滚该批次，继续下一批。错误: {}",
                            batchNum, batch.size(), e.getMessage());
                    // 不抛异常，继续下一批
                }
            }

            log.info("[月度聚合] 完成！{} 共写入 {}/{} 条月度记录（跳过 {} 条）",
                    statMonth, totalWritten, allParams.size(), skipCount);
        } finally {
            redisDistributedLock.unlock(MONTHLY_LOCK_KEY, lockToken);
        }
    }

    /**
     * 按 ID 列表批量查询，自动拼接 IN (?,?,...) 占位符。
     * <p>用于替代循环中逐条 SELECT 的 N+1 反模式。</p>
     */
    private List<Map<String, Object>> batchQueryByIds(String baseSql, String idColumn,
                                                       List<Long> ids) {
        if (ids.isEmpty()) return List.of();
        String inPlaceholders = placeholders(ids.size());
        String sql = baseSql + " WHERE " + idColumn + " IN (" + inPlaceholders + ")";
        return jdbcTemplate.queryForList(sql, ids.toArray());
    }

    /**
     * 生成指定数量的 IN 占位符：{@code ?, ?, ?, ...}
     */
    private String placeholders(int count) {
        StringBuilder sb = new StringBuilder(count * 2);
        for (int i = 0; i < count; i++) {
            if (i > 0) sb.append(", ");
            sb.append("?");
        }
        return sb.toString();
    }

    // ──────── 私有工具方法 ────────

    /**
     * 安全转换为 Long。
     */
    private Long toLong(Object value) {
        if (value == null) return null;
        if (value instanceof Long l) return l;
        if (value instanceof Number n) return n.longValue();
        try {
            return Long.parseLong(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * 安全转换为 BigDecimal。
     */
    private BigDecimal toBigDecimal(Object value) {
        if (value == null) return BigDecimal.ZERO;
        if (value instanceof BigDecimal bd) return bd;
        if (value instanceof Double d) return BigDecimal.valueOf(d);
        if (value instanceof Number n) return BigDecimal.valueOf(n.doubleValue());
        try {
            return new BigDecimal(value.toString());
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }

    /**
     * 从 MeterReading 中提取总电量读数。
     * <p>优先使用 kwh（电表采集简述的组合有功总电量），其次 read_val（积成抄表读数）。</p>
     */
    private BigDecimal extractKwh(MeterReading reading) {
        if (reading.getKwh() != null && reading.getKwh().compareTo(BigDecimal.ZERO) > 0) {
            return reading.getKwh();
        }
        if (reading.getReadVal() != null && reading.getReadVal().compareTo(BigDecimal.ZERO) > 0) {
            return reading.getReadVal();
        }
        return null;
    }

    /**
     * 安全计算差值，任一为 null 返回 ZERO。
     */
    private BigDecimal safeSubtract(BigDecimal a, BigDecimal b) {
        if (a == null || b == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal diff = a.subtract(b);
        return diff.compareTo(BigDecimal.ZERO) > 0 ? diff : BigDecimal.ZERO;
    }

    /**
     * 尝试从日冻结记录推算正向/反向有功日用电量。
     * <p>
     * 日冻结记录存储的是累计值（总电能），日用量 = 昨天累计 - 前天累计。
     * </p>
     *
     * @param meterId            表计ID
     * @param yesterday          昨天
     * @param dayBeforeYesterday 前天
     * @param forward            true=正向有功(zxyg), false=反向有功(fxyg)
     * @return 日用电量（kWh），无数据返回 ZERO
     */
    private BigDecimal tryGetForwardReverse(Long meterId, LocalDate yesterday,
                                            LocalDate dayBeforeYesterday, boolean forward) {
        try {
            LambdaQueryWrapper<DailyFreezeRecord> w = new LambdaQueryWrapper<>();
            w.eq(DailyFreezeRecord::getMeterId, meterId)
             .in(DailyFreezeRecord::getRecordDate, yesterday, dayBeforeYesterday)
             .orderByDesc(DailyFreezeRecord::getRecordDate);
            List<DailyFreezeRecord> records = dailyFreezeRecordMapper.selectList(w);

            DailyFreezeRecord yesterdayRec = null;
            DailyFreezeRecord dayBeforeRec = null;
            for (DailyFreezeRecord r : records) {
                if (r.getRecordDate().equals(yesterday)) yesterdayRec = r;
                if (r.getRecordDate().equals(dayBeforeYesterday)) dayBeforeRec = r;
            }

            if (yesterdayRec == null || dayBeforeRec == null) return BigDecimal.ZERO;

            BigDecimal yesterdayVal = forward ? yesterdayRec.getZxyg() : yesterdayRec.getFxyg();
            BigDecimal dayBeforeVal = forward ? dayBeforeRec.getZxyg() : dayBeforeRec.getFxyg();

            if (yesterdayVal == null || dayBeforeVal == null) return BigDecimal.ZERO;

            BigDecimal diff = yesterdayVal.subtract(dayBeforeVal);
            return diff.compareTo(BigDecimal.ZERO) > 0 ? diff : BigDecimal.ZERO;
        } catch (Exception e) {
            log.debug("[日报聚合] 正向/反向有功查询异常 meterId={}, forward={}: {}", meterId, forward, e.getMessage());
            return BigDecimal.ZERO;
        }
    }
}
