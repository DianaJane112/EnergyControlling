package com.competition.energycontrollerplatform.service.impl;

import com.competition.energycontrollerplatform.common.BusinessException;
import com.competition.energycontrollerplatform.common.WalletException;
import com.competition.energycontrollerplatform.dto.AdjustBalanceRequest;
import com.competition.energycontrollerplatform.dto.RechargeRequest;
import com.competition.energycontrollerplatform.dto.RefundRequest;
import com.competition.energycontrollerplatform.entity.*;
import com.competition.energycontrollerplatform.mapper.*;
import com.competition.energycontrollerplatform.miniapp.entity.Invoice;
import com.competition.energycontrollerplatform.miniapp.mapper.InvoiceMapper;
import com.competition.energycontrollerplatform.security.JwtUserPrincipal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.util.Collections;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * BillingServiceImpl 单元测试 —— 资金安全核心。
 * 覆盖充值/退费/调账：权限隔离、并发冲突、余额不足、成功流程。
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("BillingServiceImpl - 资金计费服务")
class BillingServiceImplTest {

    @Mock private CustomerMapper customerMapper;
    @Mock private RechargeOrderMapper rechargeOrderMapper;
    @Mock private RefundDetailMapper refundDetailMapper;
    @Mock private BalanceAdjustmentMapper balanceAdjustmentMapper;
    @Mock private AccountFlowMapper accountFlowMapper;
    @Mock private InvoiceMapper invoiceMapper;

    @InjectMocks
    private BillingServiceImpl billingService;

    private void mockAsAdmin() {
        setAuth(1L, "admin", "ADMIN");
    }
    private void mockAsMerchant(Long customerId) {
        setAuth(customerId, "merchant_" + customerId, "MERCHANT");
    }
    private void mockAsOperator() {
        setAuth(3L, "operator", "OPERATOR");
    }
    private void setAuth(Long userId, String username, String role) {
        JwtUserPrincipal p = new JwtUserPrincipal(userId, username, role);
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(p, null, Collections.emptyList()));
    }

    @AfterEach
    void tearDown() { SecurityContextHolder.clearContext(); }

    // ──────────────────────── 充值 ────────────────────────
    @Nested @DisplayName("充值")
    class RechargeTests {

        @Test @DisplayName("ADMIN 未指定商户ID → 400")
        void missingCustomerId() {
            mockAsAdmin();
            RechargeRequest req = reqR(100, "admin");
            req.setCustomerId(null);
            BusinessException ex = catchThrowableOfType(() -> billingService.recharge(req), BusinessException.class);
            assertThat(ex).isNotNull();
            assertThat(ex.getCode()).isEqualTo(400);
        }

        @Test @DisplayName("MERCHANT 强制用自己的商户ID，无视前端传值")
        void forcedToOwnCustomerId() {
            mockAsMerchant(5L);
            RechargeRequest req = reqR(100, "test");
            req.setCustomerId(99L);
            when(customerMapper.selectByIdForUpdate(5L)).thenReturn(cust(5L, 1000));
            when(customerMapper.addBalance(eq(5L), any())).thenReturn(1);
            when(rechargeOrderMapper.insert(any(RechargeOrder.class))).thenReturn(1);
            when(accountFlowMapper.insert(any(AccountFlow.class))).thenReturn(1);
            when(invoiceMapper.insert(any(Invoice.class))).thenReturn(1);
            assertThat(billingService.recharge(req)).startsWith("RCH");
            verify(customerMapper).selectByIdForUpdate(5L);
        }

        @Test @DisplayName("OPERATOR 充值 → 强制使用自己的 customerId（userId 映射为 customerId）")
        void operatorUsesOwnCustomerId() {
            mockAsOperator();  // userId=3 → customerId=3
            RechargeRequest req = reqR(100, "op");
            req.setCustomerId(99L);   // 前端传值被忽略
            // 用户ID=3对应的商户必须存在
            when(customerMapper.selectByIdForUpdate(3L)).thenReturn(cust(3L, 500));
            when(customerMapper.addBalance(eq(3L), any())).thenReturn(1);
            when(rechargeOrderMapper.insert(any(RechargeOrder.class))).thenReturn(1);
            when(accountFlowMapper.insert(any(AccountFlow.class))).thenReturn(1);
            when(invoiceMapper.insert(any(Invoice.class))).thenReturn(1);

            assertThat(billingService.recharge(req)).startsWith("RCH");
            verify(customerMapper).selectByIdForUpdate(3L);  // 使用 3L 而非 99L
        }

        @Test @DisplayName("商户不存在 → WalletException")
        void customerNotFound() {
            mockAsAdmin();
            when(customerMapper.selectByIdForUpdate(1L)).thenReturn(null);
            assertThatThrownBy(() -> billingService.recharge(reqR(100, "admin")))
                    .isInstanceOf(WalletException.class)
                    .matches(e -> ((WalletException) e).getCode() == 4004);
        }

        @Test @DisplayName("addBalance 返回 0 → 并发冲突")
        void concurrencyConflict() {
            mockAsAdmin();
            when(customerMapper.selectByIdForUpdate(1L)).thenReturn(cust(1L, 1000));
            when(customerMapper.addBalance(eq(1L), any())).thenReturn(0);
            assertThatThrownBy(() -> billingService.recharge(reqR(100, "admin")))
                    .isInstanceOf(WalletException.class)
                    .matches(e -> ((WalletException) e).getCode() == 4002);
            verify(rechargeOrderMapper, never()).insert(any(RechargeOrder.class));
        }

        @Test @DisplayName("成功：订单+流水+票据全部写入，余额计算正确")
        void success() {
            mockAsAdmin();
            RechargeRequest req = reqR(500, "admin");
            req.setTradeType(1);
            when(customerMapper.selectByIdForUpdate(1L)).thenReturn(cust(1L, 1000));
            when(customerMapper.addBalance(eq(1L), eq(bd(500)))).thenReturn(1);
            when(rechargeOrderMapper.insert(any(RechargeOrder.class))).thenReturn(1);
            when(accountFlowMapper.insert(any(AccountFlow.class))).thenReturn(1);
            when(invoiceMapper.insert(any(Invoice.class))).thenReturn(1);

            String orderNo = billingService.recharge(req);
            assertThat(orderNo).startsWith("RCH");

            ArgumentCaptor<AccountFlow> fc = ArgumentCaptor.forClass(AccountFlow.class);
            verify(accountFlowMapper).insert(fc.capture());
            AccountFlow f = fc.getValue();
            assertThat(f.getTradeType()).isEqualTo(1);
            assertThat(f.getBalanceBefore()).isEqualByComparingTo("1000.0000");
            assertThat(f.getChangeAmount()).isEqualByComparingTo("500.0000");
            assertThat(f.getBalanceAfter()).isEqualByComparingTo("1500.0000");
            verify(invoiceMapper).insert(any(Invoice.class));
        }

        @Test @DisplayName("商户余额为 null → 视为 0")
        void nullBalance() {
            mockAsAdmin();
            when(customerMapper.selectByIdForUpdate(1L)).thenReturn(cust(1L, null));
            when(customerMapper.addBalance(eq(1L), any())).thenReturn(1);
            when(rechargeOrderMapper.insert(any(RechargeOrder.class))).thenReturn(1);
            when(accountFlowMapper.insert(any(AccountFlow.class))).thenReturn(1);
            when(invoiceMapper.insert(any(Invoice.class))).thenReturn(1);
            assertThatNoException().isThrownBy(() -> billingService.recharge(reqR(500, "admin")));
        }
    }

    // ──────────────────────── 退费 ────────────────────────
    @Nested @DisplayName("退费")
    class RefundTests {

        @Test @DisplayName("余额不足 → insufficientBalance")
        void insufficientBalance() {
            mockAsAdmin();
            when(customerMapper.selectByIdForUpdate(1L)).thenReturn(cust(1L, 100));
            assertThatThrownBy(() -> billingService.refund(reqF(500, "admin")))
                    .isInstanceOf(WalletException.class)
                    .matches(e -> ((WalletException) e).getCode() == 4001);
        }

        @Test @DisplayName("subtractBalance 返回 0 → 并发冲突")
        void concurrencyConflict() {
            mockAsAdmin();
            when(customerMapper.selectByIdForUpdate(1L)).thenReturn(cust(1L, 500));
            when(customerMapper.subtractBalance(eq(1L), any())).thenReturn(0);
            assertThatThrownBy(() -> billingService.refund(reqF(100, "admin")))
                    .isInstanceOf(WalletException.class)
                    .matches(e -> ((WalletException) e).getCode() == 4002);
        }

        @Test @DisplayName("成功：changeAmount 为负数")
        void success() {
            mockAsAdmin();
            RefundRequest req = reqF(300, "admin");
            req.setReason("退费原因");
            when(customerMapper.selectByIdForUpdate(1L)).thenReturn(cust(1L, 1000));
            when(customerMapper.subtractBalance(eq(1L), eq(bd(300)))).thenReturn(1);
            when(refundDetailMapper.insert(any(RefundDetail.class))).thenReturn(1);
            when(accountFlowMapper.insert(any(AccountFlow.class))).thenReturn(1);

            assertThat(billingService.refund(req)).startsWith("RFD");

            ArgumentCaptor<AccountFlow> fc = ArgumentCaptor.forClass(AccountFlow.class);
            verify(accountFlowMapper).insert(fc.capture());
            AccountFlow f = fc.getValue();
            assertThat(f.getTradeType()).isEqualTo(3);
            assertThat(f.getBalanceBefore()).isEqualByComparingTo("1000.0000");
            assertThat(f.getChangeAmount()).isEqualByComparingTo("-300.0000");
            assertThat(f.getBalanceAfter()).isEqualByComparingTo("700.0000");
        }
    }

    // ──────────────────────── 调账 ────────────────────────
    @Nested @DisplayName("调账")
    class AdjustTests {

        @Test @DisplayName("非 ADMIN → 403")
        void nonAdmin() {
            mockAsMerchant(5L);
            BusinessException ex = catchThrowableOfType(
                    () -> billingService.adjustBalance(reqA(100, "test")), BusinessException.class);
            assertThat(ex).isNotNull();
            assertThat(ex.getCode()).isEqualTo(403);
        }

        @Test @DisplayName("金额为 0 → INVALID_AMOUNT")
        void zeroAmount() {
            mockAsAdmin();
            AdjustBalanceRequest req = reqA(0, "admin");
            assertThatThrownBy(() -> billingService.adjustBalance(req))
                    .isInstanceOf(WalletException.class)
                    .matches(e -> ((WalletException) e).getCode() == 4003);
        }

        @Test @DisplayName("增加 → INCREASE")
        void increase() {
            mockAsAdmin();
            when(customerMapper.selectByIdForUpdate(1L)).thenReturn(cust(1L, 500));
            when(customerMapper.addBalance(eq(1L), eq(bd(200)))).thenReturn(1);
            when(balanceAdjustmentMapper.insert(any(BalanceAdjustment.class))).thenReturn(1);
            when(accountFlowMapper.insert(any(AccountFlow.class))).thenReturn(1);

            assertThat(billingService.adjustBalance(reqA(200, "admin"))).startsWith("ADJ");
            ArgumentCaptor<AccountFlow> fc = ArgumentCaptor.forClass(AccountFlow.class);
            verify(accountFlowMapper).insert(fc.capture());
            assertThat(fc.getValue().getTradeType()).isEqualTo(4);
            assertThat(fc.getValue().getBalanceAfter()).isEqualByComparingTo("700.0000");
        }

        @Test @DisplayName("减少但余额不足 → CODE_INSUFFICIENT_BALANCE")
        void decreaseInsufficient() {
            mockAsAdmin();
            AdjustBalanceRequest req = reqA(-600, "admin");
            when(customerMapper.selectByIdForUpdate(1L)).thenReturn(cust(1L, 500));
            assertThatThrownBy(() -> billingService.adjustBalance(req))
                    .isInstanceOf(WalletException.class)
                    .matches(e -> ((WalletException) e).getCode() == 4001);
        }

        @Test @DisplayName("减少成功 → DECREASE")
        void decreaseSuccess() {
            mockAsAdmin();
            AdjustBalanceRequest req = reqA(-150, "admin");
            req.setReason("费用冲抵");
            when(customerMapper.selectByIdForUpdate(1L)).thenReturn(cust(1L, 500));
            when(customerMapper.subtractBalance(eq(1L), eq(bd(150)))).thenReturn(1);
            when(balanceAdjustmentMapper.insert(any(BalanceAdjustment.class))).thenReturn(1);
            when(accountFlowMapper.insert(any(AccountFlow.class))).thenReturn(1);
            assertThat(billingService.adjustBalance(req)).startsWith("ADJ");
        }
    }

    // ──────────────────────── helpers ────────────────────────
    private static Customer cust(Long id, Integer balanceCents) {
        Customer c = new Customer();
        c.setId(id); c.setCustomerCode("C" + id); c.setCustomerName("商户" + id);
        c.setBalance(balanceCents != null ? bd(balanceCents) : null);
        c.setStatus(1); c.setIsDeleted(0);
        return c;
    }
    private static BigDecimal bd(int cents) { return BigDecimal.valueOf(cents); }
    private static RechargeRequest reqR(int amount, String op) {
        RechargeRequest r = new RechargeRequest(); r.setCustomerId(1L);
        r.setAmount(bd(amount)); r.setOperatorName(op); return r;
    }
    private static RefundRequest reqF(int amount, String op) {
        RefundRequest r = new RefundRequest(); r.setCustomerId(1L);
        r.setAmount(bd(amount)); r.setOperatorName(op); return r;
    }
    private static AdjustBalanceRequest reqA(int amount, String op) {
        AdjustBalanceRequest r = new AdjustBalanceRequest(); r.setCustomerId(1L);
        r.setAmount(bd(amount)); r.setOperatorName(op); return r;
    }
}
