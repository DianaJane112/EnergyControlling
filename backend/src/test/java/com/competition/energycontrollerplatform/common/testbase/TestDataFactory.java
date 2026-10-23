package com.competition.energycontrollerplatform.common.testbase;

import com.competition.energycontrollerplatform.entity.*;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 测试数据工厂 —— 提供共享的测试数据构建方法。
 * <p>
 * 所有方法返回的实体/DTO均为纯 Java 对象，未持久化。
 * 配合 Mapper 层的 insert 方法即可完成数据准备。
 */
public final class TestDataFactory {

    private TestDataFactory() {}

    // ──────────────────────── 商户 ────────────────────────

    public static Customer createCustomer(Long id, String code, String name, BigDecimal balance) {
        Customer c = new Customer();
        c.setId(id);
        c.setCustomerCode(code);
        c.setCustomerName(name);
        c.setBalance(balance != null ? balance : BigDecimal.ZERO);
        c.setStatus(1);
        c.setCreateTime(LocalDateTime.now());
        c.setUpdateTime(LocalDateTime.now());
        c.setIsDeleted(0);
        return c;
    }

    public static Customer createCustomer(Long id, String code, String name, double balance) {
        return createCustomer(id, code, name, BigDecimal.valueOf(balance));
    }

    // ──────────────────────── 表计 ────────────────────────

    public static Meter createElectricMeter(Long id, Long customerId, String dbNo, String name) {
        Meter m = new Meter();
        m.setId(id);
        m.setCustomerId(customerId);
        m.setDbNo(dbNo);
        m.setName(name);
        m.setMeterType(1);       // 电表
        m.setVender("chenman");
        m.setStatus(1);
        m.setOnline(1);
        m.setStateValve(1);
        m.setBjlx(1);
        m.setFeetype(1);
        m.setCredit(BigDecimal.valueOf(100));
        m.setWarnline(BigDecimal.valueOf(50));
        m.setBei(1);
        m.setIsDeleted(0);
        m.setCreateTime(LocalDateTime.now());
        m.setUpdateTime(LocalDateTime.now());
        return m;
    }

    public static Meter createWaterMeter(Long id, Long customerId, String stealNo, String dbNo, String name) {
        Meter m = new Meter();
        m.setId(id);
        m.setCustomerId(customerId);
        m.setStealNo(stealNo);
        m.setDbNo(dbNo);
        m.setName(name);
        m.setMeterType(0);       // 水表
        m.setVender("jicheng");
        m.setStatus(1);
        m.setOnline(1);
        m.setStateValve(1);
        m.setBjlx(4);
        m.setFeetype(1);
        m.setIsDeleted(0);
        m.setCreateTime(LocalDateTime.now());
        m.setUpdateTime(LocalDateTime.now());
        return m;
    }

    public static Meter createDeletedMeter(Long id) {
        Meter m = createElectricMeter(id, 1L, "DELETED" + id, "已删除电表" + id);
        m.setIsDeleted(1);
        return m;
    }

    // ──────────────────────── 账户流水 ────────────────────────

    public static AccountFlow createAccountFlow(Long customerId, BigDecimal before, BigDecimal change,
                                                 BigDecimal after, Integer tradeType, String bizNo) {
        AccountFlow f = new AccountFlow();
        f.setFlowNo("FLW_TEST_" + System.nanoTime());
        f.setCustomerId(customerId);
        f.setBalanceBefore(before);
        f.setChangeAmount(change);
        f.setBalanceAfter(after);
        f.setTradeType(tradeType);
        f.setBizNo(bizNo);
        f.setOperatorName("test-operator");
        f.setOperateTime(LocalDateTime.now());
        f.setCreateTime(LocalDateTime.now());
        f.setUpdateTime(LocalDateTime.now());
        return f;
    }
}
