package com.competition.energycontrollerplatform.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.competition.energycontrollerplatform.dto.BankAccountRequest;
import com.competition.energycontrollerplatform.entity.BankAccount;

import java.util.List;

/**
 * 银行账户服务接口。
 */
public interface BankAccountService {

    /**
     * 分页查询银行账户列表。
     */
    IPage<BankAccount> listPage(int pageNum, int pageSize, String keyword);

    /**
     * 获取所有启用的银行账户（下拉列表）。
     */
    List<BankAccount> listEnabled();

    /**
     * 详情。
     */
    BankAccount getById(Long id);

    /**
     * 新增银行账户。
     */
    BankAccount create(BankAccountRequest request);

    /**
     * 编辑银行账户。
     */
    BankAccount update(Long id, BankAccountRequest request);

    /**
     * 删除银行账户（逻辑删除）。
     */
    void delete(Long id);
}
