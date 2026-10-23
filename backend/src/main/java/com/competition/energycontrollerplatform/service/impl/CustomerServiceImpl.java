package com.competition.energycontrollerplatform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.competition.energycontrollerplatform.common.BusinessException;
import com.competition.energycontrollerplatform.common.CacheNullValueHelper;
import com.competition.energycontrollerplatform.dto.CustomerCreateRequest;
import com.competition.energycontrollerplatform.dto.CustomerExcelVO;
import com.competition.energycontrollerplatform.dto.CustomerPageRequest;
import com.competition.energycontrollerplatform.dto.CustomerResponse;
import com.competition.energycontrollerplatform.dto.CustomerUpdateRequest;
import com.competition.energycontrollerplatform.entity.Customer;
import com.competition.energycontrollerplatform.entity.OperationLog;
import com.competition.energycontrollerplatform.mapper.CustomerMapper;
import com.competition.energycontrollerplatform.mapper.OperationLogMapper;
import com.competition.energycontrollerplatform.mapper.MeterMapper;
import com.competition.energycontrollerplatform.security.SecurityUserContext;
import com.competition.energycontrollerplatform.service.CustomerService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 商户/客户管理服务实现
 */
@Service
@RequiredArgsConstructor
public class CustomerServiceImpl implements CustomerService {

    private final CustomerMapper customerMapper;
    private final CacheNullValueHelper cacheNullValueHelper;
    private final OperationLogMapper operationLogMapper;
    private final MeterMapper meterMapper;

    @Override
    public IPage<CustomerResponse> listPage(CustomerPageRequest request) {
        LambdaQueryWrapper<Customer> wrapper = new LambdaQueryWrapper<>();

        // 权限隔离：非 ADMIN 只能查看自己商户
        if (!SecurityUserContext.isAdmin()) {
            Long myCustomerId = SecurityUserContext.getCurrentCustomerId();
            if (myCustomerId == null) {
                throw BusinessException.of(403, "无权限查看商户列表");
            }
            wrapper.eq(Customer::getId, myCustomerId);
        }

        // 客户管理按关联表计类型隔离：水表客户和电表客户共用客户表，但列表不混合展示。
        if (request.getMeterType() != null) {
            wrapper.inSql(Customer::getId,
                    "SELECT DISTINCT customer_id FROM t_meter "
                            + "WHERE is_deleted = 0 AND customer_id IS NOT NULL "
                            + "AND meter_type = " + request.getMeterType());
        }

        wrapper.like(StringUtils.hasText(request.getCustomerName()), Customer::getCustomerName, request.getCustomerName())
               .eq(StringUtils.hasText(request.getCustomerCode()), Customer::getCustomerCode, request.getCustomerCode())
               .like(StringUtils.hasText(request.getAreaName()), Customer::getAreaName, request.getAreaName())
               .eq(StringUtils.hasText(request.getCustomerType()), Customer::getCustomerType, request.getCustomerType())
               .eq(request.getStatus() != null, Customer::getStatus, request.getStatus())
               .orderByDesc(Customer::getCreateTime);

        Page<Customer> page = new Page<>(request.getPageNum(), request.getPageSize());
        IPage<Customer> result = customerMapper.selectPage(page, wrapper);
        return result.convert(this::toResponse);
    }

    @Override
    @Cacheable(value = "customer", key = "#id")
    public CustomerResponse getById(Long id) {
        // 空值缓存快速否定（防缓存穿透）
        if (cacheNullValueHelper.isNullCached("customer", id)) {
            throw BusinessException.of(400, "商户不存在");
        }
        Customer customer = customerMapper.selectById(id);
        if (customer == null) {
            cacheNullValueHelper.cacheNull("customer", id);
            throw BusinessException.of(400, "商户不存在");
        }
        checkCustomerOwnership(customer);
        return toResponse(customer);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CustomerResponse create(CustomerCreateRequest request) {
        // 仅 ADMIN 可开户
        if (!SecurityUserContext.isAdmin()) {
            throw BusinessException.of(403, "仅管理员可执行商户开户操作");
        }
        // 预校验：customer_code 唯一性
        Long count = customerMapper.selectCount(
                new LambdaQueryWrapper<Customer>()
                        .eq(Customer::getCustomerCode, request.getCustomerCode()));
        if (count != null && count > 0) {
            throw BusinessException.of(400, "用户编号 [" + request.getCustomerCode() + "] 已存在，请使用其他编号");
        }
        Customer customer = new Customer();
        BeanUtils.copyProperties(request, customer);
        customer.setStatus(1);
        customer.setCreateTime(LocalDateTime.now());
        customer.setUpdateTime(LocalDateTime.now());
        customerMapper.insert(customer);
        return toResponse(customer);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "customer", key = "#id")
    public CustomerResponse update(Long id, CustomerUpdateRequest request) {
        Customer customer = customerMapper.selectById(id);
        if (customer == null) {
            throw BusinessException.of(400, "商户不存在");
        }
        checkCustomerOwnership(customer);
        BeanUtils.copyProperties(request, customer, "id", "customerCode", "createDate", "type",
                "oldStealNo", "oldVal", "status", "createTime", "updateTime", "createBy", "isDeleted");
        customer.setUpdateTime(LocalDateTime.now());
        customerMapper.updateById(customer);
        return toResponse(customer);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "customer", key = "#id")
    public void delete(Long id) {
        Customer customer = customerMapper.selectById(id);
        if (customer == null) {
            customer = customerMapper.selectIncludingDeleted(id);
        }
        if (customer == null) {
            throw BusinessException.of(400, "商户不存在");
        }
        checkCustomerOwnership(customer);
        // 仅 ADMIN 可注销商户
        if (!SecurityUserContext.isAdmin()) {
            throw BusinessException.of(403, "仅管理员可执行商户注销操作");
        }
        if (Integer.valueOf(1).equals(customer.getIsDeleted())) {
            return;
        }
        Long meterCount = meterMapper == null ? 0L : meterMapper.selectCount(new LambdaQueryWrapper<com.competition.energycontrollerplatform.entity.Meter>()
                .eq(com.competition.energycontrollerplatform.entity.Meter::getCustomerId, id));
        OperationLog archiveLog = new OperationLog();
        archiveLog.setOperatorName(SecurityUserContext.getCurrentUsername());
        archiveLog.setModuleName("商户管理");
        archiveLog.setOperationType("ARCHIVE");
        archiveLog.setOperationDesc("归档客户，保留关联表计、虚拟表关系及历史读数");
        archiveLog.setRequestParams(String.format("{\"customerId\":%d,\"customerCode\":\"%s\",\"customerName\":\"%s\",\"meterCount\":%d}", id, customer.getCustomerCode(), customer.getCustomerName(), meterCount == null ? 0 : meterCount));
        archiveLog.setOperationTime(LocalDateTime.now());
        if (operationLogMapper != null) {
            operationLogMapper.insert(archiveLog);
        }
        customerMapper.logicDeleteById(id); // 仅归档客户，不触碰任何关联表和读数
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "customer", key = "#id")
    public void updateStatus(Long id, Integer status) {
        Customer customer = customerMapper.selectById(id);
        if (customer == null) {
            throw BusinessException.of(400, "商户不存在");
        }
        checkCustomerOwnership(customer);
        customer.setStatus(status);
        customer.setUpdateTime(LocalDateTime.now());
        customerMapper.updateById(customer);
    }

    @Override
    public List<CustomerExcelVO> exportList(CustomerPageRequest request) {
        LambdaQueryWrapper<Customer> wrapper = new LambdaQueryWrapper<>();

        // 权限隔离：非 ADMIN 只能查看自己商户
        if (!SecurityUserContext.isAdmin()) {
            Long myCustomerId = SecurityUserContext.getCurrentCustomerId();
            if (myCustomerId == null) {
                throw BusinessException.of(403, "无权限导出商户列表");
            }
            wrapper.eq(Customer::getId, myCustomerId);
        }

        if (request.getMeterType() != null) {
            wrapper.inSql(Customer::getId,
                    "SELECT DISTINCT customer_id FROM t_meter "
                            + "WHERE is_deleted = 0 AND customer_id IS NOT NULL "
                            + "AND meter_type = " + request.getMeterType());
        }

        wrapper.like(StringUtils.hasText(request.getCustomerName()), Customer::getCustomerName, request.getCustomerName())
               .eq(StringUtils.hasText(request.getCustomerCode()), Customer::getCustomerCode, request.getCustomerCode())
               .like(StringUtils.hasText(request.getAreaName()), Customer::getAreaName, request.getAreaName())
               .eq(StringUtils.hasText(request.getCustomerType()), Customer::getCustomerType, request.getCustomerType())
               .eq(request.getStatus() != null, Customer::getStatus, request.getStatus())
               .orderByDesc(Customer::getCreateTime);

        List<Customer> customers = customerMapper.selectList(wrapper);
        return customers.stream().map(this::toExcelVO).collect(Collectors.toList());
    }

    private CustomerExcelVO toExcelVO(Customer customer) {
        CustomerExcelVO vo = new CustomerExcelVO();
        vo.setId(customer.getId());
        vo.setCustomerCode(customer.getCustomerCode());
        vo.setCustomerName(customer.getCustomerName());
        vo.setAreaName(customer.getAreaName());
        vo.setAddress(customer.getAddress());
        vo.setPhone(customer.getPhone());
        vo.setCustomerType(customer.getCustomerType());
        vo.setUseNature(customer.getUseNature());
        vo.setCreateDate(customer.getCreateDate());
        vo.setType(customer.getType());
        vo.setStatusLabel(customer.getStatus() != null && customer.getStatus() == 1 ? "启用" : "停用");
        vo.setOperator(customer.getOperator());
        vo.setRemark(customer.getRemark());
        vo.setCreateTime(customer.getCreateTime());
        vo.setUpdateTime(customer.getUpdateTime());
        return vo;
    }

    /**
     * 校验当前用户是否有权操作该商户。
     * <p>
     * ADMIN 可操作任意商户；非 ADMIN 仅可操作自己的商户。
     */
    private void checkCustomerOwnership(Customer customer) {
        if (SecurityUserContext.isAdmin()) {
            return;
        }
        Long myCustomerId = SecurityUserContext.getCurrentCustomerId();
        if (myCustomerId == null || !myCustomerId.equals(customer.getId())) {
            throw BusinessException.of(403, "无权操作该商户");
        }
    }

    private CustomerResponse toResponse(Customer customer) {
        CustomerResponse resp = new CustomerResponse();
        BeanUtils.copyProperties(customer, resp);
        return resp;
    }
}
