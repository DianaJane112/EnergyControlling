package com.competition.energycontrollerplatform.service;

import com.competition.energycontrollerplatform.entity.Customer;
import com.competition.energycontrollerplatform.entity.Meter;
import com.competition.energycontrollerplatform.entity.MeterState;
import com.competition.energycontrollerplatform.integration.hook.ValveControlHook;
import com.competition.energycontrollerplatform.mapper.CustomerMapper;
import com.competition.energycontrollerplatform.mapper.MeterMapper;
import com.competition.energycontrollerplatform.mapper.MeterStateMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 表计状态变更服务。
 * <p>
 * 使用 {@code REQUIRES_NEW} 独立事务，确保即使扣费事务回滚，
 * 欠费停机标记也能成功持久化。
 */
@Service
@RequiredArgsConstructor
public class MeterStatusService {

    private static final Logger log = LoggerFactory.getLogger(MeterStatusService.class);

    private final MeterMapper meterMapper;
    private final MeterStateMapper meterStateMapper;
    private final CustomerMapper customerMapper;
    private final ValveControlHook valveControlHook;

    /** 正常 */
    public static final int STATUS_NORMAL = 1;
    /** 欠费停机 */
    public static final int STATUS_OVERDUE = 2;

    /**
     * 标记表计为欠费停机（独立事务，不受调用方回滚影响）。
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void markOverdue(Long meterId, Long customerId, BigDecimal balance, BigDecimal required) {
        Meter meter = meterMapper.selectById(meterId);
        if (meter == null) {
            log.error("[欠费停机] 表计不存在 meterId={}", meterId);
            return;
        }

        // 从数据库查询当前余额（独立事务中获取最新值）
        BigDecimal currentBalance = balance;
        if (currentBalance == null || currentBalance.compareTo(BigDecimal.ZERO) <= 0) {
            // 如果外部没传入有效余额，尝试从t_customer查
            Customer customer = customerMapper.selectById(customerId);
            if (customer != null && customer.getBalance() != null) {
                currentBalance = customer.getBalance();
            }
        }

        int rows = meterMapper.updateStatus(meterId, STATUS_OVERDUE);
        log.warn("[欠费停机] 表计状态已更新 meterId={}, status={}, rows={}", meterId, STATUS_OVERDUE, rows);

        // 触发关阀 Hook（独立事务中，不因 Hook 异常回滚状态标记）
        try {
            valveControlHook.closeValve(meterId, meter.getStealNo(),
                    "AUTO_BILLING", "自动扣费余额不足关阀");
        } catch (Exception e) {
            log.error("[欠费停机] 关阀 Hook 调用异常 meterId={}: {}", meterId, e.getMessage());
        }

        // 写入状态变更记录到 t_meter_state（审计）
        MeterState state = new MeterState();
        state.setMeterId(meterId);
        state.setCustomerCode(meter.getCustomerNo());
        state.setStealNo(meter.getStealNo());
        state.setVender(meter.getVender());
        state.setStatus(String.valueOf(STATUS_OVERDUE));
        state.setMessage(String.format("自动扣费失败：余额不足。当前余额 %s，需扣 %s",
                currentBalance.toPlainString(), required.toPlainString()));
        state.setRecordDate(LocalDateTime.now());
        state.setCreateTime(LocalDateTime.now());
        state.setUpdateTime(LocalDateTime.now());
        meterStateMapper.insert(state);

        log.warn("[欠费停机] 审计记录已写入 meterId={}, balance={}, required={}", meterId, currentBalance, required);
    }

    /**
     * 恢复表计为正常运行。
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void markNormal(Long meterId) {
        meterMapper.updateStatus(meterId, STATUS_NORMAL);
        log.info("[恢复运行] meterId={}, status={}", meterId, STATUS_NORMAL);
    }
}
