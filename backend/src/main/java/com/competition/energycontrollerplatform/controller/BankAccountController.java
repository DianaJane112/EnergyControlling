package com.competition.energycontrollerplatform.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.competition.energycontrollerplatform.common.Result;
import com.competition.energycontrollerplatform.common.annotation.BusinessType;
import com.competition.energycontrollerplatform.common.annotation.Log;
import com.competition.energycontrollerplatform.dto.BankAccountRequest;
import com.competition.energycontrollerplatform.entity.BankAccount;
import com.competition.energycontrollerplatform.service.BankAccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 银行账户设置 Controller。
 * <p>提供银行账户的增删改查功能，用于即时账单模块的收款账户配置。</p>
 */
@Tag(name = "即时账单 - 银行账户设置", description = "银行账户的增删改查接口")
@RestController
@RequestMapping("/api/v1/bank-account")
@RequiredArgsConstructor
@Validated
public class BankAccountController {

    private final BankAccountService bankAccountService;

    @Operation(summary = "分页查询银行账户列表", description = "支持按关键字（银行名/账户名/账号）模糊搜索")
    @GetMapping("/page")
    public Result<IPage<BankAccount>> page(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int pageNum,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "20") int pageSize,
            @Parameter(description = "关键字（银行名/账户名/账号模糊匹配）") @RequestParam(required = false) String keyword) {
        IPage<BankAccount> page = bankAccountService.listPage(pageNum, pageSize, keyword);
        return Result.success(page);
    }

    @Operation(summary = "获取所有启用的银行账户（下拉列表）")
    @GetMapping("/enabled")
    public Result<List<BankAccount>> enabledList() {
        List<BankAccount> list = bankAccountService.listEnabled();
        return Result.success(list);
    }

    @Operation(summary = "银行账户详情")
    @GetMapping("/{id}")
    public Result<BankAccount> detail(@Parameter(description = "账户ID") @PathVariable Long id) {
        BankAccount account = bankAccountService.getById(id);
        return Result.success(account);
    }

    @Operation(summary = "新增银行账户")
    @Log(title = "银行账户", businessType = BusinessType.INSERT)
    @PostMapping
    public Result<BankAccount> create(@Valid @RequestBody BankAccountRequest request) {
        BankAccount account = bankAccountService.create(request);
        return Result.success(account);
    }

    @Operation(summary = "编辑银行账户")
    @Log(title = "银行账户", businessType = BusinessType.UPDATE)
    @PutMapping("/{id}")
    public Result<BankAccount> update(@Parameter(description = "账户ID") @PathVariable Long id,
                                      @Valid @RequestBody BankAccountRequest request) {
        BankAccount account = bankAccountService.update(id, request);
        return Result.success(account);
    }

    @Operation(summary = "删除银行账户（逻辑删除）")
    @Log(title = "银行账户", businessType = BusinessType.DELETE)
    @DeleteMapping("/{id}")
    public Result<Void> delete(@Parameter(description = "账户ID") @PathVariable Long id) {
        bankAccountService.delete(id);
        return Result.success("删除成功");
    }
}
