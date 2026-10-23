package com.competition.energycontrollerplatform.service.impl;

import com.competition.energycontrollerplatform.common.WalletException;
import com.competition.energycontrollerplatform.dto.AdjustBalanceRequest;
import com.competition.energycontrollerplatform.dto.RechargeRequest;
import com.competition.energycontrollerplatform.dto.RefundRequest;
import com.competition.energycontrollerplatform.entity.*;
import com.competition.energycontrollerplatform.mapper.*;
import com.competition.energycontrollerplatform.miniapp.entity.Invoice;
import com.competition.energycontrollerplatform.miniapp.mapper.InvoiceMapper;
import com.competition.energycontrollerplatform.security.SecurityUserContext;
import com.competition.energycontrollerplatform.service.MeterOperationTraceService;
import com.competition.energycontrollerplatform.service.BillingService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 资金计费服务实现。
 * <p>
 * 核心原则：
 * <ul>
 *   <li>绝不先 SELECT 再 UPDATE —— 使用 Mapper 原生 @Update SQL 做数据库原子操作</li>
 *   <li>扣费/退费 SQL 中含 {@code AND balance >= ?} 防止负数钱包</li>
 *   <li>受影响行数为 0 时抛 {@link WalletException}，触发事务回滚</li>
 *   <li>每笔资金变动都写入 t_account_flow 审计流水</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class BillingServiceImpl implements BillingService {

    private static final Logger log = LoggerFactory.getLogger(BillingServiceImpl.class);

    private final CustomerMapper customerMapper;
    private final RechargeOrderMapper rechargeOrderMapper;
    private final RefundDetailMapper refundDetailMapper;
    private final BalanceAdjustmentMapper balanceAdjustmentMapper;
    private final AccountFlowMapper accountFlowMapper;
    private final InvoiceMapper invoiceMapper;
    private final MeterOperationTraceService meterOperationTraceService;

    // ──────────────────────── 充值 ────────────────────────

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = {"dashboard", "meter_statistics"}, allEntries = true)
    public String recharge(RechargeRequest request) {
        // 权限隔离：非 ADMIN 只允许给自己商户充值，无视前端传入的 customerId
        Long customerId = resolveCustomerId(request.getCustomerId());
        BigDecimal amount = request.getAmount();

        log.info("[充值] customerId={}, amount={}, operator={}", customerId, amount, request.getOperatorName());

        // ① 悲观锁锁定商户行，读取精确的变动前余额（FOR UPDATE 保证事务内一致性）
        Customer locked = customerMapper.selectByIdForUpdate(customerId);
        if (locked == null) {
            throw WalletException.customerNotFound(customerId);
        }
        BigDecimal balanceBefore = locked.getBalance() != null ? locked.getBalance() : BigDecimal.ZERO;

        // ② 原子增加余额（FOR UPDATE 保护下，不受并发干扰）
        int rows = customerMapper.addBalance(customerId, amount);
        if (rows == 0) {
            throw WalletException.concurrencyConflict(customerId, "充值");
        }

        BigDecimal balanceAfter = balanceBefore.add(amount);

        // ③ 写充值订单
        String orderNo = generateBizNo("RCH");
        RechargeOrder order = new RechargeOrder();
        order.setOrderNo(orderNo);
        order.setCustomerId(customerId);
        order.setAmount(amount);
        order.setPayamount(amount);
        order.setRealamount(amount);
        order.setBalance(balanceAfter);
        order.setCharegetype(2);
        order.setCzy(request.getOperatorName());
        order.setOperatorName(request.getOperatorName());
        order.setUserName(request.getUserName());
        order.setTradeType(request.getTradeType());
        order.setRemark(request.getRemark());
        order.setCreateTime(LocalDateTime.now());
        order.setUpdateTime(LocalDateTime.now());
        rechargeOrderMapper.insert(order);

        // ④ 写流水
        insertFlow(customerId, null, balanceBefore, amount, balanceAfter,
                1, orderNo, request.getOperatorName(), request.getRemark());
        recordTrace(customerId, null, "RECHARGE", balanceBefore, amount, balanceAfter, orderNo, request.getRemark());

        // ⑤ 创建票据（供小程序端查询）
        createInvoice(order.getId(), customerId, null, amount, request.getTradeType(), request.getRemark());

        log.info("[充值] 成功 orderNo={}, balance {} -> {}", orderNo, balanceBefore, balanceAfter);
        return orderNo;
    }

    // ──────────────────────── 退费 ────────────────────────

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = {"dashboard", "meter_statistics"}, allEntries = true)
    public String refund(RefundRequest request) {
        // 权限隔离
        Long customerId = resolveCustomerId(request.getCustomerId());
        BigDecimal amount = request.getAmount();

        log.info("[退费] customerId={}, amount={}, operator={}", customerId, amount, request.getOperatorName());

        // ① 悲观锁锁定商户行，读取精确的变动前余额
        Customer locked = customerMapper.selectByIdForUpdate(customerId);
        if (locked == null) {
            throw WalletException.customerNotFound(customerId);
        }
        BigDecimal balanceBefore = locked.getBalance() != null ? locked.getBalance() : BigDecimal.ZERO;

        // ② 余额不足预检（在锁保护下安全判断，避免无效 SQL 执行）
        if (balanceBefore.compareTo(amount) < 0) {
            throw WalletException.insufficientBalance(customerId, balanceBefore, amount);
        }

        // ③ 原子扣减余额（SQL 含 AND balance >= amount 双保险）
        int rows = customerMapper.subtractBalance(customerId, amount);
        if (rows == 0) {
            throw WalletException.concurrencyConflict(customerId, "退费");
        }

        BigDecimal balanceAfter = balanceBefore.subtract(amount);

        // ④ 写退费单
        String refundNo = generateBizNo("RFD");
        RefundDetail refund = new RefundDetail();
        refund.setRefundNo(refundNo);
        refund.setCustomerId(customerId);
        refund.setRefundAmount(amount);
        refund.setRefundType("PARTIAL_REFUND");
        refund.setRefundReason(request.getReason());
        refund.setOperatorName(request.getOperatorName());
        refund.setRefundTime(LocalDateTime.now());
        refund.setRemark(request.getRemark());
        refund.setCreateTime(LocalDateTime.now());
        refund.setUpdateTime(LocalDateTime.now());
        refundDetailMapper.insert(refund);

        // ④ 写流水
        insertFlow(customerId, null, balanceBefore, amount.negate(), balanceAfter,
                3, refundNo, request.getOperatorName(),
                request.getReason() != null ? request.getReason() : request.getRemark());
        recordTrace(customerId, null, "REFUND", balanceBefore, amount.negate(), balanceAfter, refundNo, request.getReason());

        log.info("[退费] 成功 refundNo={}, balance {} -> {}", refundNo, balanceBefore, balanceAfter);
        return refundNo;
    }

    // ──────────────────────── 调账 ────────────────────────

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = {"dashboard", "meter_statistics"}, allEntries = true)
    public String adjustBalance(AdjustBalanceRequest request) {
        // 仅 ADMIN 可执行调账操作
        if (!SecurityUserContext.isAdmin()) {
            throw new com.competition.energycontrollerplatform.common.BusinessException(
                    403, "仅管理员可执行调账操作");
        }
        Long customerId = request.getCustomerId();
        BigDecimal amount = request.getAmount();

        if (amount.compareTo(BigDecimal.ZERO) == 0) {
            throw WalletException.invalidAmount("调整金额不能为0");
        }

        log.info("[调账] customerId={}, amount={}, operator={}, reason={}",
                customerId, amount, request.getOperatorName(), request.getReason());

        boolean isIncrease = amount.compareTo(BigDecimal.ZERO) > 0;
        String adjustmentType = isIncrease ? "INCREASE" : "DECREASE";

        // ② 悲观锁锁定商户行，读取精确的变动前余额
        Customer locked = customerMapper.selectByIdForUpdate(customerId);
        if (locked == null) {
            throw WalletException.customerNotFound(customerId);
        }
        BigDecimal balanceBefore = locked.getBalance() != null ? locked.getBalance() : BigDecimal.ZERO;

        // ③ 扣减场景预检余额（避免无效 SQL 执行）
        if (!isIncrease && balanceBefore.compareTo(amount.abs()) < 0) {
            throw WalletException.insufficientBalance(customerId, balanceBefore, amount.abs());
        }

        // ④ 原子执行余额变动
        int rows;
        if (isIncrease) {
            rows = customerMapper.addBalance(customerId, amount);
        } else {
            rows = customerMapper.subtractBalance(customerId, amount.abs());
        }

        if (rows == 0) {
            throw WalletException.concurrencyConflict(customerId, "调账");
        }

        BigDecimal balanceAfter = balanceBefore.add(amount);

        // ⑤ 写调账记录
        String adjustmentNo = generateBizNo("ADJ");
        BalanceAdjustment adjustment = new BalanceAdjustment();
        adjustment.setAdjustmentNo(adjustmentNo);
        adjustment.setCustomerId(customerId);
        adjustment.setBalance(amount);
        adjustment.setAdjustmentType(adjustmentType);
        adjustment.setBz(request.getReason());
        adjustment.setOperatorName(request.getOperatorName());
        adjustment.setAdjustmentTime(LocalDateTime.now());
        adjustment.setRemark(request.getRemark());
        adjustment.setCreateTime(LocalDateTime.now());
        adjustment.setUpdateTime(LocalDateTime.now());
        balanceAdjustmentMapper.insert(adjustment);

        // ④ 写流水
        insertFlow(customerId, null, balanceBefore, amount, balanceAfter,
                4, adjustmentNo, request.getOperatorName(), request.getReason());
        recordTrace(customerId, null, "ADJUST", balanceBefore, amount, balanceAfter, adjustmentNo, request.getReason());

        log.info("[调账] 成功 adjustmentNo={}, type={}, balance {} -> {}",
                adjustmentNo, adjustmentType, balanceBefore, balanceAfter);
        return adjustmentNo;
    }

    private void recordTrace(Long customerId, Long meterId, String event, BigDecimal before, BigDecimal change, BigDecimal after, String requestId, String remark) {
        if (meterOperationTraceService == null) return;
        MeterOperationTrace t = new MeterOperationTrace();
        t.setObjectType(meterId == null ? "CUSTOMER" : "REAL_METER"); t.setObjectId(meterId); t.setMeterId(meterId);
        t.setCustomerId(customerId); t.setEventType(event); t.setOperationTerminal("WEB");
        t.setOperatorId(SecurityUserContext.getCurrentUserId()); t.setOperatorName(SecurityUserContext.getCurrentUsername());
        t.setBalanceBefore(before); t.setChangeAmount(change); t.setCreditedAmount(event.equals("RECHARGE") ? change : null);
        t.setBalanceAfter(after); t.setRequestId(requestId); t.setRemark(remark);
        meterOperationTraceService.record(t);
    }

    // ──────────────────────── 私有 ────────────────────────

    /**
     * 解析资金操作的目标商户 ID，同时执行权限校验。
     * <p>
     * ADMIN 可使用前端传入的 customerId（操作任意商户）；
     * 非 ADMIN 无视前端传入值，强制使用当前用户关联的商户 ID。
     *
     * @param requestedCustomerId 前端请求中的商户 ID
     * @return 经过权限校验后的真实商户 ID
     * @throws BusinessException 非 ADMIN 且未关联商户时抛出 403
     */
    private Long resolveCustomerId(Long requestedCustomerId) {
        if (SecurityUserContext.isAdmin()) {
            if (requestedCustomerId == null) {
                throw new com.competition.energycontrollerplatform.common.BusinessException(
                        400, "ADMIN 执行资金操作必须指定商户ID");
            }
            return requestedCustomerId;
        }
        Long myCustomerId = SecurityUserContext.getCurrentCustomerId();
        if (myCustomerId == null) {
            throw new com.competition.energycontrollerplatform.common.BusinessException(
                    403, "无权限执行资金操作");
        }
        return myCustomerId;
    }

    private void insertFlow(Long customerId, Long meterId,
                            BigDecimal balanceBefore, BigDecimal changeAmount, BigDecimal balanceAfter,
                            Integer tradeType, String bizNo, String operatorName, String remark) {
        AccountFlow flow = new AccountFlow();
        flow.setFlowNo(generateBizNo("FLW"));
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

    private String generateBizNo(String prefix) {
        String ts = String.valueOf(System.currentTimeMillis());
        String shortTs = ts.substring(ts.length() - 8);
        String shortUuid = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        return prefix + shortTs + shortUuid;
    }

    /**
     * 创建票据记录（供小程序端查询）。
     * <p>
     * 每次充值成功后自动生成一条未开票的票据，商户可在小程序端查看和申请开票。
     *
     * @param rechargeOrderId 关联充值订单ID
     * @param customerId      商户ID
     * @param meterId         表计ID（可为null，表示商户级充值）
     * @param amount          票据金额
     * @param tradeType       交易类型：1-电费, 2-水费
     * @param remark          备注
     */
    private void createInvoice(Long rechargeOrderId, Long customerId, Long meterId,
                               BigDecimal amount, Integer tradeType, String remark) {
        Invoice invoice = new Invoice();
        invoice.setInvoiceNo(generateBizNo("INV"));
        invoice.setCustomerId(customerId);
        invoice.setMeterId(meterId);
        invoice.setRechargeOrderId(rechargeOrderId);
        invoice.setAmount(amount);
        // 根据交易类型映射票据类型：1-电费→ELECTRIC, 2-水费→WATER
        invoice.setInvoiceType(tradeType != null && tradeType == 2 ? "WATER" : "ELECTRIC");
        invoice.setInvoiceStatus("NOT_ISSUED");
        invoice.setRemark(remark);
        invoice.setCreateTime(LocalDateTime.now());
        invoice.setUpdateTime(LocalDateTime.now());
        invoiceMapper.insert(invoice);
        log.info("[票据] 已创建 invoiceNo={}, rechargeOrderId={}, customerId={}, amount={}",
                invoice.getInvoiceNo(), rechargeOrderId, customerId, amount);
    }
}
