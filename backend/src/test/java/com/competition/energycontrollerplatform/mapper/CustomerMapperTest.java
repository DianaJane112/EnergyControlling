package com.competition.energycontrollerplatform.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.competition.energycontrollerplatform.entity.Customer;
import com.competition.energycontrollerplatform.testbase.TestMockBeans;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.*;

/**
 * CustomerMapper 集成测试 —— 使用 H2 内存数据库验证原子 SQL。
 * 测试通过直接调用 Mapper 方法准备数据，确保与 MyBatis-Plus 事务一致。
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(TestMockBeans.class)
@Transactional
@DisplayName("CustomerMapper - 原子资金操作")
class CustomerMapperTest {

    @Autowired
    private CustomerMapper customerMapper;

    // ──────────────────────── addBalance ────────────────────────

    @Test @DisplayName("addBalance — 余额增加")
    void addBalance_increases() {
        Long id = insertCustomer("T001", "100");
        int rows = customerMapper.addBalance(id, BigDecimal.valueOf(50));
        assertThat(rows).isEqualTo(1);
        assertThat(reSelectBalance(id)).isEqualByComparingTo("150.0000");
    }

    @Test @DisplayName("addBalance — 不存在商户返回 0")
    void addBalance_nonexistent() {
        assertThat(customerMapper.addBalance(9999L, BigDecimal.valueOf(50))).isEqualTo(0);
    }

    @Test @DisplayName("addBalance — 已删除商户返回 0（WHERE is_deleted=0 守卫）")
    void addBalance_deleted() {
        Long id = insertCustomer("T002", "100");
        customerMapper.logicDeleteById(id);   // 逻辑删除
        assertThat(customerMapper.addBalance(id, BigDecimal.valueOf(50))).isEqualTo(0);
    }

    // ──────────────────────── subtractBalance ────────────────────────

    @Test @DisplayName("subtractBalance — 余额充足时扣减")
    void subtractBalance_sufficient() {
        Long id = insertCustomer("T003", "100");
        int rows = customerMapper.subtractBalance(id, BigDecimal.valueOf(30));
        assertThat(rows).isEqualTo(1);
        assertThat(reSelectBalance(id)).isEqualByComparingTo("70.0000");
    }

    @Test @DisplayName("subtractBalance — 余额不足返回 0（守卫 balance >= amount）")
    void subtractBalance_insufficient() {
        Long id = insertCustomer("T004", "100");
        int rows = customerMapper.subtractBalance(id, BigDecimal.valueOf(150));
        assertThat(rows).isEqualTo(0);
        assertThat(reSelectBalance(id)).isEqualByComparingTo("100.0000");
    }

    @Test @DisplayName("subtractBalance — 恰好扣到 0")
    void subtractBalance_exact() {
        Long id = insertCustomer("T005", "100");
        assertThat(customerMapper.subtractBalance(id, BigDecimal.valueOf(100))).isEqualTo(1);
        assertThat(reSelectBalance(id)).isEqualByComparingTo("0.0000");
    }

    // ──────────────────────── selectByIdForUpdate ────────────────────────

    @Test @DisplayName("selectByIdForUpdate — 锁定并返回")
    void selectByIdForUpdate_found() {
        Long id = insertCustomer("T006", "200");
        Customer c = customerMapper.selectByIdForUpdate(id);
        assertThat(c).isNotNull();
        assertThat(c.getCustomerCode()).isEqualTo("T006");
    }

    @Test @DisplayName("selectByIdForUpdate — 已删除商户返回 null")
    void selectByIdForUpdate_deleted() {
        Long id = insertCustomer("T007", "200");
        customerMapper.logicDeleteById(id);
        assertThat(customerMapper.selectByIdForUpdate(id)).isNull();
    }

    // ──────────────────────── logicDeleteById ────────────────────────

    @Test @DisplayName("logicDeleteById — 设置 is_deleted=1")
    void logicDeleteById_setsDeleted() {
        Long id = insertCustomer("T008", "100");
        assertThat(customerMapper.logicDeleteById(id)).isEqualTo(1);
        // @TableLogic 过滤：selectById 查不到
        assertThat(customerMapper.selectById(id)).isNull();
    }

    @Test @DisplayName("logicDeleteById — 重复删除返回 0")
    void logicDeleteById_alreadyDeleted() {
        Long id = insertCustomer("T009", "100");
        customerMapper.logicDeleteById(id);
        assertThat(customerMapper.logicDeleteById(id)).isEqualTo(0);
    }

    // ──────────────────────── helpers ────────────────────────

    /** 插入测试商户并返回自增 ID */
    private Long insertCustomer(String code, String balance) {
        Customer c = new Customer();
        c.setCustomerCode(code);
        c.setCustomerName("测试商户");
        c.setBalance(new BigDecimal(balance));
        c.setStatus(1);
        customerMapper.insert(c);
        return c.getId();
    }

    private BigDecimal reSelectBalance(Long id) {
        Customer c = customerMapper.selectById(id);
        return c != null ? c.getBalance() : null;
    }
}
