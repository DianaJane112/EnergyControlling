package com.competition.energycontrollerplatform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.competition.energycontrollerplatform.common.BusinessException;
import com.competition.energycontrollerplatform.dto.BankAccountRequest;
import com.competition.energycontrollerplatform.entity.BankAccount;
import com.competition.energycontrollerplatform.mapper.BankAccountMapper;
import com.competition.energycontrollerplatform.security.SecurityUserContext;
import com.competition.energycontrollerplatform.service.BankAccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 银行账户服务实现。
 */
@Service
@RequiredArgsConstructor
public class BankAccountServiceImpl implements BankAccountService {

    private final BankAccountMapper bankAccountMapper;

    @Override
    public IPage<BankAccount> listPage(int pageNum, int pageSize, String keyword) {
        LambdaQueryWrapper<BankAccount> wrapper = new LambdaQueryWrapper<>();
        // 使用 and() 包裹 OR 条件，确保与 @TableLogic 的 is_deleted=0 正确嵌套
        wrapper.and(StringUtils.hasText(keyword), w -> w
                .like(BankAccount::getBankName, keyword)
                .or()
                .like(BankAccount::getAccountName, keyword)
                .or()
                .like(BankAccount::getAccountNumber, keyword))
                .orderByDesc(BankAccount::getCreateTime);

        Page<BankAccount> page = new Page<>(pageNum, pageSize);
        return bankAccountMapper.selectPage(page, wrapper);
    }

    @Override
    public List<BankAccount> listEnabled() {
        LambdaQueryWrapper<BankAccount> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BankAccount::getStatus, 1)
                .orderByDesc(BankAccount::getCreateTime);
        return bankAccountMapper.selectList(wrapper);
    }

    @Override
    public BankAccount getById(Long id) {
        BankAccount account = bankAccountMapper.selectById(id);
        if (account == null) {
            throw BusinessException.of(400, "银行账户不存在");
        }
        return account;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BankAccount create(BankAccountRequest request) {
        if (!SecurityUserContext.isAdmin()) {
            throw BusinessException.of(403, "仅管理员可设置银行账户");
        }

        // 如果设置为默认账户，先取消其他默认账户
        if (request.getIsDefault() != null && request.getIsDefault() == 1) {
            clearDefaultAccounts();
        }

        BankAccount account = new BankAccount();
        account.setBankName(request.getBankName());
        account.setAccountName(request.getAccountName());
        account.setAccountNumber(request.getAccountNumber());
        account.setBankBranch(request.getBankBranch());
        account.setBankCode(request.getBankCode());
        account.setAccountType(request.getAccountType() != null ? request.getAccountType() : "COMPANY");
        account.setIsDefault(request.getIsDefault() != null ? request.getIsDefault() : 0);
        account.setContactPerson(request.getContactPerson());
        account.setContactPhone(request.getContactPhone());
        account.setRemark(request.getRemark());
        account.setStatus(request.getStatus() != null ? request.getStatus() : 1);
        account.setCreateTime(LocalDateTime.now());
        account.setUpdateTime(LocalDateTime.now());
        bankAccountMapper.insert(account);
        return account;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BankAccount update(Long id, BankAccountRequest request) {
        BankAccount account = bankAccountMapper.selectById(id);
        if (account == null) {
            throw BusinessException.of(400, "银行账户不存在");
        }

        // 如果设置为默认账户，先取消其他默认账户
        if (request.getIsDefault() != null && request.getIsDefault() == 1
                && (account.getIsDefault() == null || account.getIsDefault() != 1)) {
            clearDefaultAccounts();
        }

        account.setBankName(request.getBankName());
        account.setAccountName(request.getAccountName());
        account.setAccountNumber(request.getAccountNumber());
        account.setBankBranch(request.getBankBranch());
        account.setBankCode(request.getBankCode());
        account.setAccountType(request.getAccountType() != null ? request.getAccountType() : account.getAccountType());
        account.setIsDefault(request.getIsDefault() != null ? request.getIsDefault() : account.getIsDefault());
        account.setContactPerson(request.getContactPerson());
        account.setContactPhone(request.getContactPhone());
        account.setRemark(request.getRemark());
        account.setStatus(request.getStatus() != null ? request.getStatus() : account.getStatus());
        account.setUpdateTime(LocalDateTime.now());
        bankAccountMapper.updateById(account);
        return account;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        if (!SecurityUserContext.isAdmin()) {
            throw BusinessException.of(403, "仅管理员可删除银行账户");
        }
        BankAccount account = bankAccountMapper.selectById(id);
        if (account == null) {
            throw BusinessException.of(400, "银行账户不存在");
        }
        bankAccountMapper.logicDeleteById(id); // 绕过 @TableLogic 的 update_time 空值问题
    }

    /**
     * 清除所有默认账户标记。
     */
    private void clearDefaultAccounts() {
        LambdaQueryWrapper<BankAccount> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BankAccount::getIsDefault, 1);
        List<BankAccount> defaults = bankAccountMapper.selectList(wrapper);
        // 在同一个事务内批量 UPDATE（后续可优化为 updateBatchById 需要 IService）
        for (BankAccount acc : defaults) {
            acc.setIsDefault(0);
            acc.setUpdateTime(LocalDateTime.now());
            bankAccountMapper.updateById(acc);
        }
    }
}
