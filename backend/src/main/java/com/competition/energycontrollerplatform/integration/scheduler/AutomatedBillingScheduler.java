package com.competition.energycontrollerplatform.integration.scheduler;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.competition.energycontrollerplatform.common.RedisDistributedLock;
import com.competition.energycontrollerplatform.common.WalletException;
import com.competition.energycontrollerplatform.entity.Meter;
import com.competition.energycontrollerplatform.mapper.MeterMapper;
import com.competition.energycontrollerplatform.service.EnergyConsumptionService;
import com.competition.energycontrollerplatform.service.MeterStatusService;
import com.competition.energycontrollerplatform.service.MeterOperationTraceService;
import com.competition.energycontrollerplatform.entity.MeterOperationTrace;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * 全自动能耗扣费调度器。
 * <p>
 * 核心原则：
 * <ul>
 *   <li>分页拉取激活表计（status=1），避免一次全表加载</li>
 *   <li>逐台独立扣费，单台失败不影响其他商户</li>
 *   <li>捕获余额不足异常时调用独立事务标记欠费停机</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
public class AutomatedBillingScheduler {

    private static final Logger log = LoggerFactory.getLogger(AutomatedBillingScheduler.class);

    private final MeterMapper meterMapper;
    private final EnergyConsumptionService energyConsumptionService;
    private final MeterStatusService meterStatusService;
    private final RedisDistributedLock redisDistributedLock;
    private final MeterOperationTraceService meterOperationTraceService;

    @Value("${billing.default-unit-price:0.85}")
    private BigDecimal defaultUnitPrice;

    @Value("${billing.page-size:100}")
    private int pageSize;

    /** 分布式锁 key：多实例部署时确保只有一台执行扣费 */
    private static final String BILLING_LOCK_KEY = "lock:billing:daily";
    /** 锁超时时间（秒），需大于扣费任务最长可能执行时间 */
    private static final long BILLING_LOCK_TIMEOUT_SEC = 600;

    /**
     * 定时自动扣费。
     * <p>
     * cron 默认每 5 分钟执行一次（在 application.yml 中可通过 billing.cron 覆盖）。
     * 使用 Redis 分布式锁防止多实例重复扣费：
     * 获取到锁的实例执行扣费逻辑，其他实例静默跳过。
     */
    @Scheduled(cron = "${billing.cron:0 */5 * * * *}")
    public void executeAutomatedBilling() {
        // ── 分布式锁：防止多实例并发执行 ──
        String lockToken = redisDistributedLock.tryLock(BILLING_LOCK_KEY, BILLING_LOCK_TIMEOUT_SEC);
        if (lockToken == null) {
            log.info("[调度] 本次扣费由其他实例执行，当前实例跳过");
            return;
        }

        try {
            doExecuteAutomatedBilling();
        } finally {
            redisDistributedLock.unlock(BILLING_LOCK_KEY, lockToken);
        }
    }

    /**
     * 扣费主逻辑（提取为独立方法，便于在锁内调用）。
     */
    private void doExecuteAutomatedBilling() {
        log.info("========== 自动扣费调度 开始 ==========");
        long startTime = System.currentTimeMillis();
        int totalProcessed = 0;
        int totalCharged = 0;
        int totalSkipped = 0;
        int totalOverdue = 0;

        int pageNum = 1;
        while (true) {
            // ① 分页拉取激活状态的表计（status=1 且在线的）
            LambdaQueryWrapper<Meter> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Meter::getStatus, 1)
                   .eq(Meter::getOnline, 1)
                   .eq(Meter::getMeterKind, "PHYSICAL")
                   .isNotNull(Meter::getCustomerId)
                   .orderByAsc(Meter::getId);

            Page<Meter> page = new Page<>(pageNum, pageSize);
            Page<Meter> result = meterMapper.selectPage(page, wrapper);

            if (result.getRecords().isEmpty()) {
                break;
            }

            // ② 逐台扣费
            for (Meter meter : result.getRecords()) {
                totalProcessed++;
                try {
                    BigDecimal cost = energyConsumptionService.calculateAndDebit(
                            meter.getId(), defaultUnitPrice);

                    if (cost.compareTo(BigDecimal.ZERO) > 0) {
                        totalCharged++;
                        log.info("[调度] 扣费成功 meterId={}, cost={}", meter.getId(), cost);
                    } else {
                        totalSkipped++;
                    }
                } catch (WalletException e) {
                    if (e.getCode() != null && e.getCode() == WalletException.CODE_INSUFFICIENT_BALANCE) {
                        // ③ 余额不足 → 独立事务标记欠费停机
                        totalOverdue++;
                        log.warn("[调度] 余额不足 meterId={}, customerId={}, msg={}",
                                meter.getId(), meter.getCustomerId(), e.getMessage());

                        try {
                            meterStatusService.markOverdue(
                                    meter.getId(),
                                    meter.getCustomerId(),
                                    BigDecimal.ZERO, // 当前余额从异常消息中无法解析，此处用ZERO占位
                                    BigDecimal.ZERO);
                        } catch (Exception ex) {
                            log.error("[调度] 欠费标记失败 meterId={}", meter.getId(), ex);
                        }
                    } else {
                        recordBillingFailure(meter, e);
                        log.error("[调度] 扣费异常 meterId={}, code={}, msg={}",
                                meter.getId(), e.getCode(), e.getMessage());
                    }
                } catch (Exception e) {
                    recordBillingFailure(meter, e); log.error("[调度] 未知异常 meterId={}", meter.getId(), e);
                }
            }

            if (!result.hasNext()) {
                break;
            }
            pageNum++;
        }

        long elapsed = System.currentTimeMillis() - startTime;
        log.info("========== 自动扣费调度 结束 ==========");
        log.info("处理: {} | 扣费: {} | 跳过(无增长): {} | 欠费停机: {} | 耗时: {}ms",
                totalProcessed, totalCharged, totalSkipped, totalOverdue, elapsed);
    }

    private void recordBillingFailure(Meter meter, Exception e) {
        try { MeterOperationTrace t=new MeterOperationTrace(); t.setObjectType(meter.getMeterType()!=null&&meter.getMeterType()==1?"REAL_ELECTRIC_METER":"REAL_WATER_METER"); t.setObjectId(meter.getId()); t.setMeterId(meter.getId()); t.setCustomerId(meter.getCustomerId()); t.setEventType("BILLING_FAILED"); t.setOperationTerminal("SYSTEM"); t.setOperatorName("系统自动"); t.setResultStatus("FAILED"); t.setFailureReason(e.getMessage()); t.setRemark("自动扣费失败"); meterOperationTraceService.recordFailure(t); } catch(Exception x){ log.error("记录扣费失败轨迹异常 meterId={}",meter.getId(),x); }
    }
}
