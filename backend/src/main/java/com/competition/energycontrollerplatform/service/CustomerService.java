package com.competition.energycontrollerplatform.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.competition.energycontrollerplatform.dto.CustomerCreateRequest;
import com.competition.energycontrollerplatform.dto.CustomerExcelVO;
import com.competition.energycontrollerplatform.dto.CustomerPageRequest;
import com.competition.energycontrollerplatform.dto.CustomerResponse;
import com.competition.energycontrollerplatform.dto.CustomerUpdateRequest;

import java.util.List;

/**
 * 商户/客户管理服务接口
 */
public interface CustomerService {

    /**
     * 商户分页列表
     */
    IPage<CustomerResponse> listPage(CustomerPageRequest request);

    /**
     * 商户详情
     */
    CustomerResponse getById(Long id);

    /**
     * 商户开户
     */
    CustomerResponse create(CustomerCreateRequest request);

    /**
     * 商户信息修改
     */
    CustomerResponse update(Long id, CustomerUpdateRequest request);

    /**
     * 商户注销（逻辑删除）
     */
    void delete(Long id);

    /**
     * 商户状态变更（启用/停用）
     */
    void updateStatus(Long id, Integer status);

    /**
     * 导出商户列表（Excel）
     */
    List<CustomerExcelVO> exportList(CustomerPageRequest request);
}
