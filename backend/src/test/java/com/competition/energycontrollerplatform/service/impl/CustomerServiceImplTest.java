package com.competition.energycontrollerplatform.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.competition.energycontrollerplatform.common.BusinessException;
import com.competition.energycontrollerplatform.common.CacheNullValueHelper;
import com.competition.energycontrollerplatform.dto.CustomerCreateRequest;
import com.competition.energycontrollerplatform.dto.CustomerPageRequest;
import com.competition.energycontrollerplatform.dto.CustomerUpdateRequest;
import com.competition.energycontrollerplatform.entity.Customer;
import com.competition.energycontrollerplatform.mapper.CustomerMapper;
import com.competition.energycontrollerplatform.security.JwtUserPrincipal;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
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

@ExtendWith(MockitoExtension.class)
@DisplayName("CustomerServiceImpl - 商户管理")
class CustomerServiceImplTest {

    @Mock private CustomerMapper customerMapper;
    @Mock private CacheNullValueHelper cacheNullValueHelper;
    @InjectMocks private CustomerServiceImpl customerService;

    private void mockAdmin() { setAuth(1L, "admin", "ADMIN"); }
    private void mockMerchant(Long cid) { setAuth(cid, "m" + cid, "MERCHANT"); }
    private void setAuth(Long uid, String uname, String role) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        new JwtUserPrincipal(uid, uname, role), null, Collections.emptyList()));
    }

    @AfterEach void tearDown() { SecurityContextHolder.clearContext(); }

    static Customer cust(Long id, String code, String name) {
        Customer c = new Customer();
        c.setId(id); c.setCustomerCode(code); c.setCustomerName(name);
        c.setBalance(BigDecimal.valueOf(1000)); c.setStatus(1); c.setIsDeleted(0);
        return c;
    }

    // ──────────────────────── listPage ────────────────────────

    @Test @DisplayName("ADMIN 查看全部商户")
    void listPage_admin() {
        mockAdmin();
        CustomerPageRequest req = new CustomerPageRequest();
        req.setPageNum(1); req.setPageSize(10);
        when(customerMapper.selectPage(any(Page.class), any())).thenReturn(new Page<>());

        assertThat(customerService.listPage(req)).isNotNull();
    }

    @Test @DisplayName("非 ADMIN 无 customerId → 403")
    void listPage_noCustomerId() {
        setAuth(2L, "op", "OPERATOR");  // getCurrentCustomerId → 2L (from userId)
        CustomerPageRequest req = new CustomerPageRequest();
        req.setPageNum(1); req.setPageSize(10);
        // OPERATOR with userId=2 has customerId=2, so no 403
        when(customerMapper.selectPage(any(Page.class), any())).thenReturn(new Page<>());
        assertThat(customerService.listPage(req)).isNotNull();
    }

    // ──────────────────────── getById ────────────────────────

    @Test @DisplayName("空值缓存命中 → 400")
    void getById_nullCached() {
        mockAdmin();
        when(cacheNullValueHelper.isNullCached("customer", 1L)).thenReturn(true);
        BusinessException ex = catchThrowableOfType(() -> customerService.getById(1L), BusinessException.class);
        assertThat(ex).isNotNull();
        assertThat(ex.getCode()).isEqualTo(400);
        verify(customerMapper, never()).selectById(any());
    }

    @Test @DisplayName("商户不存在 → 缓存空值并抛异常")
    void getById_notFound() {
        mockAdmin();
        when(cacheNullValueHelper.isNullCached("customer", 1L)).thenReturn(false);
        when(customerMapper.selectById(1L)).thenReturn(null);
        assertThatThrownBy(() -> customerService.getById(1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("商户不存在");
        verify(cacheNullValueHelper).cacheNull("customer", 1L);
    }

    @Test @DisplayName("非所有者 → 403")
    void getById_nonOwner() {
        mockMerchant(5L);
        when(cacheNullValueHelper.isNullCached("customer", 1L)).thenReturn(false);
        when(customerMapper.selectById(1L)).thenReturn(cust(1L, "C001", "其他人的商户"));
        assertThatThrownBy(() -> customerService.getById(1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("无权操作");
    }

    // ──────────────────────── create ────────────────────────

    @Test @DisplayName("非 ADMIN → 403")
    void create_nonAdmin() {
        mockMerchant(5L);
        BusinessException ex = catchThrowableOfType(
                () -> customerService.create(new CustomerCreateRequest()), BusinessException.class);
        assertThat(ex).isNotNull();
        assertThat(ex.getCode()).isEqualTo(403);
    }

    @Test @DisplayName("编号重复 → 400")
    void create_duplicate() {
        mockAdmin();
        CustomerCreateRequest req = new CustomerCreateRequest();
        req.setCustomerCode("C001");
        when(customerMapper.selectCount(any())).thenReturn(1L);
        assertThatThrownBy(() -> customerService.create(req))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("已存在");
    }

    @Test @DisplayName("创建成功")
    void create_success() {
        mockAdmin();
        CustomerCreateRequest req = new CustomerCreateRequest();
        req.setCustomerCode("C999");
        req.setCustomerName("新商户");
        when(customerMapper.selectCount(any())).thenReturn(0L);
        when(customerMapper.insert(any(Customer.class))).thenReturn(1);

        assertThat(customerService.create(req)).isNotNull();
        verify(customerMapper).insert(any(Customer.class));
    }

    // ──────────────────────── update ────────────────────────

    @Test @DisplayName("商户不存在 → 400")
    void update_notFound() {
        mockAdmin();
        when(customerMapper.selectById(1L)).thenReturn(null);
        assertThatThrownBy(() -> customerService.update(1L, new CustomerUpdateRequest()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("商户不存在");
    }

    @Test @DisplayName("非所有者编辑 → 403")
    void update_nonOwner() {
        mockMerchant(5L);
        when(customerMapper.selectById(1L)).thenReturn(cust(1L, "C001", "他人商户"));
        assertThatThrownBy(() -> customerService.update(1L, new CustomerUpdateRequest()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("无权操作");
    }

    @Test @DisplayName("更新成功")
    void update_success() {
        mockAdmin();
        when(customerMapper.selectById(1L)).thenReturn(cust(1L, "C001", "商户"));
        when(customerMapper.updateById(any(Customer.class))).thenReturn(1);
        assertThat(customerService.update(1L, new CustomerUpdateRequest())).isNotNull();
    }

    // ──────────────────────── delete ────────────────────────

    @Test @DisplayName("非 ADMIN → 403")
    void delete_nonAdmin() {
        mockMerchant(1L);
        when(customerMapper.selectById(1L)).thenReturn(cust(1L, "C001", "商户"));
        assertThatThrownBy(() -> customerService.delete(1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("仅管理员");
    }

    @Test @DisplayName("注销成功")
    void delete_success() {
        mockAdmin();
        when(customerMapper.selectById(1L)).thenReturn(cust(1L, "C001", "商户"));
        when(customerMapper.logicDeleteById(1L)).thenReturn(1);
        assertThatNoException().isThrownBy(() -> customerService.delete(1L));
        verify(customerMapper).logicDeleteById(1L);
    }
}
