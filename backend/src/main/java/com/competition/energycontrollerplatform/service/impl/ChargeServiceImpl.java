package com.competition.energycontrollerplatform.service.impl;

import com.competition.energycontrollerplatform.common.BusinessException;
import com.competition.energycontrollerplatform.common.WalletException;
import com.competition.energycontrollerplatform.dto.DepositRequest;
import com.competition.energycontrollerplatform.dto.ManualRefundRequest;
import com.competition.energycontrollerplatform.entity.*;
import com.competition.energycontrollerplatform.integration.hook.ValveControlHook;
import com.competition.energycontrollerplatform.mapper.*;
import com.competition.energycontrollerplatform.security.SecurityUserContext;
import com.competition.energycontrollerplatform.service.ChargeService;
import com.competition.energycontrollerplatform.service.MeterOperationTraceService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 人工收费服务实现。
 * <p>
 * <b>核心安全原则：</b>
 * <ul>
 *   <li>绝不先 SELECT 再 UPDATE —— 使用 Mapper 原生 @Update SQL 做原子操作</li>
 *   <li>扣费 SQL 中含 {@code AND balance >= #{amount}} 防止负数钱包</li>
 *   <li>受影响行数为 0 时抛 {@link WalletException}，触发事务回滚</li>
 *   <li>每笔资金变动均写入 t_account_flow 审计流水</li>
 *   <li>人工退费额外写入 t_refund_log 审计日志</li>
 *   <li>非 ADMIN 角色强制隔离，杜绝水平越权</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class ChargeServiceImpl implements ChargeService {

    private static final Logger log = LoggerFactory.getLogger(ChargeServiceImpl.class);

    private final CustomerMapper customerMapper;
    private final MeterMapper meterMapper;
    private final AccountFlowMapper accountFlowMapper;
    private final RefundLogMapper refundLogMapper;
    private final RefundDetailMapper refundDetailMapper;
    private final RechargeOrderMapper rechargeOrderMapper;
    private final ShadowMeterMapper shadowMeterMapper;
    private final MeterOperationTraceService meterOperationTraceService;
    private final ValveControlHook valveControlHook;

    // ──────────────────────── 人工缴费 ────────────────────────

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String deposit(DepositRequest request) {
        // ① 权限隔离：解析真实商户 ID
        Meter requestedMeter = request.getMeterId() == null ? null : meterMapper.selectById(request.getMeterId());
        Meter billingMeter = resolveBillingMeter(requestedMeter);
        Long requestedCustomerId = billingMeter != null && billingMeter.getCustomerId() != null
                ? billingMeter.getCustomerId() : request.getCustomerId();
        Long customerId = resolveCustomerId(requestedCustomerId);
        if (billingMeter != null && billingMeter.getCustomerId() != null
                && !billingMeter.getCustomerId().equals(customerId)) {
            throw BusinessException.of(403, "无权为该结算账户缴费");
        }
        BigDecimal amount = request.getAmount();

        log.info("[人工缴费] customerId={}, amount={}, meterId={}, operator={}",
                customerId, amount, request.getMeterId(), SecurityUserContext.getCurrentUsername());

        // ② 悲观锁锁定商户行，读取精确的变动前余额
        Customer locked = customerMapper.selectByIdForUpdate(customerId);
        if (locked == null) {
            throw WalletException.customerNotFound(customerId);
        }
        BigDecimal balanceBefore = locked.getBalance() != null ? locked.getBalance() : BigDecimal.ZERO;

        // ③ 原子增加商户余额（FOR UPDATE 保护下，不受并发干扰）
        int rows = customerMapper.addBalance(customerId, amount);
        if (rows == 0) {
            throw WalletException.concurrencyConflict(customerId, "人工缴费");
        }

        BigDecimal balanceAfter = balanceBefore.add(amount);

        // ④ 写入资金流水（trade_type=5 人工缴费）
        String flowNo = generateBizNo("DEP");
        Long billingMeterId = billingMeter != null ? billingMeter.getId() : request.getMeterId();
        insertAccountFlow(customerId, billingMeterId, balanceBefore, amount, balanceAfter,
                5, flowNo, SecurityUserContext.getCurrentUsername(),
                "人工缴费" + (request.getPaymentMethod() != null ? " [" + request.getPaymentMethod() + "]" : "")
                + (request.getRemark() != null ? " - " + request.getRemark() : ""));

        // 与资金流水同事务写充值订单，保证充值明细与分账统计使用同一笔业务数据。
        RechargeOrder order = new RechargeOrder();
        order.setOrderNo(flowNo);
        order.setCustomerId(customerId);
        order.setMeterId(billingMeterId);
        if (billingMeter != null) {
            order.setDbNo(billingMeter.getDbNo());
            order.setDbName(billingMeter.getName());
            order.setProName(billingMeter.getProName());
        }
        order.setAmount(amount);
        order.setPayamount(amount);
        order.setRealamount(amount);
        order.setBalance(balanceAfter);
        order.setCharegetype(2);
        order.setCzy(SecurityUserContext.getCurrentUsername());
        order.setOperatorName(SecurityUserContext.getCurrentUsername());
        order.setTradeType(5);
        order.setRemark(request.getRemark());
        order.setCreateBy(SecurityUserContext.getCurrentUsername());
        order.setCreateTime(LocalDateTime.now());
        order.setUpdateTime(LocalDateTime.now());
        rechargeOrderMapper.insert(order);

        MeterOperationTrace trace = new MeterOperationTrace();
        trace.setObjectType(billingMeter != null && "VIRTUAL".equalsIgnoreCase(billingMeter.getMeterKind())
                ? "VIRTUAL_METER" : "REAL_METER");
        trace.setObjectId(billingMeterId);
        trace.setMeterId(billingMeterId);
        trace.setChildMeterId(requestedMeter != null && !requestedMeter.getId().equals(billingMeterId)
                ? requestedMeter.getId() : null);
        trace.setCustomerId(customerId);
        trace.setEventType("RECHARGE");
        trace.setOperationTerminal("WEB");
        trace.setOperatorId(SecurityUserContext.getCurrentUserId());
        trace.setOperatorName(SecurityUserContext.getCurrentUsername());
        trace.setBalanceBefore(balanceBefore);
        trace.setChangeAmount(amount);
        trace.setCreditedAmount(amount);
        trace.setBalanceAfter(balanceAfter);
        trace.setPaymentMethod(request.getPaymentMethod());
        trace.setRequestId(flowNo);
        trace.setRemark(request.getRemark());
        meterOperationTraceService.record(trace);

        // ⑤ 表计状态位更新（纯 DB 操作，不包含外部 I/O）
        if (request.getMeterId() != null) {
            updateMeterStatusInDb(request.getMeterId(), balanceAfter);
            // ⑥ 阀控指令注册到事务提交后执行（HTTP/TCP 外部调用，不占用数据库行锁）
            final Long meterId = request.getMeterId();
            final BigDecimal finalBalance = balanceAfter;
            TransactionSynchronizationManager.registerSynchronization(
                    new TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            triggerValveControlIfNeeded(meterId, finalBalance);
                        }
                    });
        }

        log.info("[人工缴费] 成功 flowNo={}, balance {} -> {}", flowNo, balanceBefore, balanceAfter);
        return flowNo;
    }

    // ──────────────────────── 人工退费 ────────────────────────

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String manualRefund(ManualRefundRequest request) {
        // ① 权限隔离
        Long customerId = resolveCustomerId(request.getCustomerId());
        BigDecimal amount = request.getAmount();
        Long meterId = request.getMeterId();
        String currentUser = SecurityUserContext.getCurrentUsername();
        Long currentUserId = SecurityUserContext.getCurrentUserId();

        log.info("[人工退费] meterId={}, customerId={}, amount={}, operator={}, reason={}",
                meterId, customerId, amount, currentUser, request.getReason());

        // ② 校验表计存在且已绑定商户
        Meter meter = meterMapper.selectById(meterId);
        if (meter == null) {
            throw BusinessException.of(404, "表计不存在");
        }
        if (meter.getCustomerId() == null) {
            throw BusinessException.of(400, "表计未绑定商户，无法退费");
        }
        // 非 ADMIN 校验表计归属
        if (!SecurityUserContext.isAdmin() && !meter.getCustomerId().equals(customerId)) {
            throw BusinessException.of(403, "无权操作该表计");
        }
        // 若请求中未指定 customerId，从表计推导
        if (request.getCustomerId() == null) {
            customerId = meter.getCustomerId();
        }

        // ③ 悲观锁锁定商户行，读取精确的变动前余额
        Customer locked = customerMapper.selectByIdForUpdate(customerId);
        if (locked == null) {
            throw WalletException.customerNotFound(customerId);
        }
        BigDecimal balanceBefore = locked.getBalance() != null ? locked.getBalance() : BigDecimal.ZERO;

        // ④ 余额不足预检（锁保护下安全判断，避免无效扣减）
        if (balanceBefore.compareTo(amount) < 0) {
            throw WalletException.insufficientBalance(customerId, balanceBefore, amount);
        }

        // ⑤ 原子扣减商户余额（SQL 含 AND balance >= amount 双保险）
        int rows = customerMapper.subtractBalance(customerId, amount);
        if (rows == 0) {
            throw WalletException.concurrencyConflict(customerId, "人工退费");
        }

        BigDecimal balanceAfter = balanceBefore.subtract(amount);

        // ⑥ 写入 t_refund_log 退费日志
        String flowNo = generateBizNo("RFL");
        RefundLog refundLog = new RefundLog();
        refundLog.setFlowNo(flowNo);
        refundLog.setMeterId(meterId);
        refundLog.setCustomerId(customerId);
        refundLog.setBalanceBefore(balanceBefore);
        refundLog.setRefundAmount(amount);
        refundLog.setBalanceAfter(balanceAfter);
        refundLog.setOperatorId(currentUserId);
        refundLog.setOperatorName(currentUser);
        refundLog.setRefundReason(request.getReason());
        refundLog.setOperateTime(LocalDateTime.now());
        refundLog.setRemark(request.getRemark());
        refundLog.setCreateTime(LocalDateTime.now());
        refundLog.setUpdateTime(LocalDateTime.now());
        refundLogMapper.insert(refundLog);

        // ⑥ 写入 t_account_flow 资金流水（trade_type=3 退费）
        insertAccountFlow(customerId, meterId, balanceBefore, amount.negate(), balanceAfter,
                3, flowNo, currentUser,
                "人工退费 - " + request.getReason()
                + (request.getRemark() != null ? " - " + request.getRemark() : ""));

        // ⑦ 写入 t_refund_detail 退费明细
        String refundNo = generateBizNo("RFD");
        RefundDetail refundDetail = new RefundDetail();
        refundDetail.setRefundNo(refundNo);
        refundDetail.setMeterId(meterId);
        refundDetail.setCustomerId(customerId);
        refundDetail.setRefundAmount(amount);
        refundDetail.setRefundType("PARTIAL_REFUND");
        refundDetail.setRefundReason(request.getReason());
        refundDetail.setOperatorName(currentUser);
        refundDetail.setRefundTime(LocalDateTime.now());
        refundDetail.setRemark(request.getRemark());
        refundDetail.setCreateTime(LocalDateTime.now());
        refundDetail.setUpdateTime(LocalDateTime.now());
        refundDetailMapper.insert(refundDetail);

        // ⑨ 表计状态位更新（纯 DB 操作，不包含外部 I/O）
        updateMeterStatusInDb(meterId, balanceAfter);
        // ⑩ 阀控指令注册到事务提交后执行（HTTP/TCP 外部调用，不占用数据库行锁）
        final BigDecimal finalBalance = balanceAfter;
        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        triggerValveControlIfNeeded(meterId, finalBalance);
                    }
                });

        log.info("[人工退费] 成功 flowNo={}, refundNo={}, balance {} -> {}",
                flowNo, refundNo, balanceBefore, balanceAfter);
        return flowNo;
    }

    // ──────────────────────── 私有方法 ────────────────────────

    private Meter resolveBillingMeter(Meter sourceMeter) {
        if (sourceMeter == null || "VIRTUAL".equalsIgnoreCase(sourceMeter.getMeterKind())) {
            return sourceMeter;
        }
        java.util.List<ShadowMeter> parents = shadowMeterMapper.findActiveByChildId(sourceMeter.getId()).stream()
                .filter(r -> "REAL_METER".equalsIgnoreCase(r.getShadowType()) && r.getParentMeterId() != null)
                .toList();
        if (parents.size() > 1) {
            throw BusinessException.of(409, "该真实表关联了多个有效虚拟结算账户，请先修复绑定关系");
        }
        if (parents.size() == 1) {
            Meter parent = meterMapper.selectById(parents.get(0).getParentMeterId());
            if (parent != null && "VIRTUAL".equalsIgnoreCase(parent.getMeterKind())) return parent;
        }
        return sourceMeter;
    }

    /**
     * 权限隔离：解析资金操作的目标商户 ID。
     * <p>ADMIN 可使用前端传入值；非 ADMIN 强制使用当前登录用户关联的商户 ID。</p>
     */
    private Long resolveCustomerId(Long requestedCustomerId) {
        if (SecurityUserContext.isAdmin()) {
            if (requestedCustomerId == null) {
                throw BusinessException.of(400, "ADMIN 必须指定商户ID");
            }
            return requestedCustomerId;
        }
        Long myCustomerId = SecurityUserContext.getCurrentCustomerId();
        if (myCustomerId == null) {
            throw BusinessException.of(403, "无权限执行资金操作：未关联商户");
        }
        return myCustomerId;
    }

    /**
     * 写入资金流水到 t_account_flow。
     */
    private void insertAccountFlow(Long customerId, Long meterId,
                                   BigDecimal balanceBefore, BigDecimal changeAmount, BigDecimal balanceAfter,
                                   Integer tradeType, String bizNo, String operatorName, String remark) {
        AccountFlow flow = new AccountFlow();
        flow.setFlowNo(bizNo);
        flow.setCustomerId(customerId);
        flow.setMeterId(meterId);
        flow.setBalanceBefore(balanceBefore);
        flow.setChangeAmount(changeAmount);
        flow.setBalanceAfter(balanceAfter);
        flow.setTradeType(tradeType);
        flow.setBizNo(bizNo);
        flow.setOperatorName(operatorName);
        flow.setOperateTime(LocalDateTime.now());
        flow.setRemark(remark);
        flow.setCreateTime(LocalDateTime.now());
        flow.setUpdateTime(LocalDateTime.now());
        accountFlowMapper.insert(flow);
    }

    /**
     * 更新表计状态位（纯数据库操作，不包含外部 I/O）。
     * <p>使用 SELECT ... FOR UPDATE 锁住表计行，防止与管理员并发修改状态
     * （如销户 status=3）产生 TOCTOU 竞态。在 FOR UPDATE 事务内执行，仅做
     * UPDATE t_meter，不触发阀控 Hook。</p>
     *
     * @param meterId 表计ID
     * @param balance 变动后的商户余额
     */
    private void updateMeterStatusInDb(Long meterId, BigDecimal balance) {
        Meter meter = meterMapper.selectByIdForUpdate(meterId);
        if (meter == null) {
            return;
        }

        BigDecimal credit = meter.getCredit() != null ? meter.getCredit() : BigDecimal.ZERO;
        BigDecimal warnline = meter.getWarnline() != null ? meter.getWarnline() : BigDecimal.ZERO;

        // 余额 <= 0 且无透支额度 → 欠费停机
        if (balance.compareTo(BigDecimal.ZERO) <= 0 && credit.compareTo(BigDecimal.ZERO) <= 0) {
            meterMapper.updateValveState(meterId, 2, 2); // status=2(欠费停机) + stateValve=2(分闸)
            log.warn("[表计状态] meterId={} 因余额不足自动标记欠费停机, balance={}", meterId, balance);
        }
        // 余额 > 0 且之前是欠费状态 → 恢复
        else if (balance.compareTo(BigDecimal.ZERO) > 0 && meter.getStatus() != null && meter.getStatus() == 2) {
            meterMapper.updateValveState(meterId, 1, 1); // status=1(正常) + stateValve=1(合闸)
            log.info("[表计状态] meterId={} 因充值恢复运行, balance={}", meterId, balance);
        }

        // 余额低于警告线（仅日志告警，不改变 DB 状态）
        if (balance.compareTo(warnline) <= 0 && balance.compareTo(BigDecimal.ZERO) > 0) {
            log.warn("[表计状态] meterId={} 余额低于警告线, balance={}, warnline={}", meterId, balance, warnline);
        }
    }

    /**
     * 事务提交后执行阀控指令（HTTP/TCP 外部 I/O，不占用数据库行锁）。
     * <p>阀控是 best-effort 副作用：余额已正确变动，关阀失败仅记 ERROR 日志，</p>
     * <p>不抛异常（事务已提交，无法回滚）。</p>
     *
     * @param meterId 表计ID
     * @param balance 变动后的商户余额
     */
    private void triggerValveControlIfNeeded(Long meterId, BigDecimal balance) {
        try {
            Meter meter = meterMapper.selectById(meterId);
            if (meter == null) {
                return;
            }
            BigDecimal credit = meter.getCredit() != null ? meter.getCredit() : BigDecimal.ZERO;

            if (balance.compareTo(BigDecimal.ZERO) <= 0 && credit.compareTo(BigDecimal.ZERO) <= 0) {
                valveControlHook.closeValve(meterId, meter.getStealNo(),
                        SecurityUserContext.getCurrentUsername(), "余额清零自动关阀");
            } else if (balance.compareTo(BigDecimal.ZERO) > 0
                    && meter.getStatus() != null && meter.getStatus() == 2) {
                valveControlHook.openValve(meterId, meter.getStealNo(),
                        SecurityUserContext.getCurrentUsername(), "充值后余额恢复自动开阀");
            }
        } catch (Exception e) {
            log.error("[阀控] 事务提交后阀控指令下发失败 meterId={}, balance={}: {}",
                    meterId, balance, e.getMessage());
        }
    }

    /**
     * 生成业务流水号。
     */
    private String generateBizNo(String prefix) {
        String ts = String.valueOf(System.currentTimeMillis());
        String shortTs = ts.substring(ts.length() - 8);
        String shortUuid = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        return prefix + shortTs + shortUuid;
    }
}
