package com.competition.energycontrollerplatform.controller;

import com.alibaba.excel.EasyExcel;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.competition.energycontrollerplatform.common.Result;
import com.competition.energycontrollerplatform.common.annotation.BusinessType;
import com.competition.energycontrollerplatform.common.annotation.Log;
import com.competition.energycontrollerplatform.dto.CustomerCreateRequest;
import com.competition.energycontrollerplatform.dto.CustomerExcelVO;
import com.competition.energycontrollerplatform.dto.CustomerPageRequest;
import com.competition.energycontrollerplatform.dto.CustomerResponse;
import com.competition.energycontrollerplatform.dto.CustomerStatusRequest;
import com.competition.energycontrollerplatform.dto.CustomerUpdateRequest;
import com.competition.energycontrollerplatform.security.SecurityUserContext;
import com.competition.energycontrollerplatform.service.CustomerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 商户/客户管理 Controller
 */
@Tag(name = "电能/水能管理 - 项目信息 - 商户信息", description = "商户开户、修改、查询、注销、启停等接口")
@RestController
@RequestMapping("/api/v1/merchant")
@RequiredArgsConstructor
@Validated
public class CustomerController {

    private final CustomerService customerService;

    private static final Logger log = LoggerFactory.getLogger(CustomerController.class);

    private static final DateTimeFormatter FILE_DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    @Operation(summary = "商户列表分页查询", description = "支持按商户名模糊查询，按用户编号、小区、类型、状态筛选")
    @GetMapping("/page")
    public Result<IPage<CustomerResponse>> page(@Valid CustomerPageRequest request) {
        IPage<CustomerResponse> page = customerService.listPage(request);
        return Result.success(page);
    }

    @Operation(summary = "水表客户列表", description = "仅返回关联捷先或积成水表的客户")
    @GetMapping("/water/page")
    public Result<IPage<CustomerResponse>> waterPage(@Valid CustomerPageRequest request) {
        request.setMeterType(0);
        return Result.success(customerService.listPage(request));
    }

    @Operation(summary = "电表客户列表", description = "仅返回关联昊普电表的客户")
    @GetMapping("/electric/page")
    public Result<IPage<CustomerResponse>> electricPage(@Valid CustomerPageRequest request) {
        request.setMeterType(1);
        return Result.success(customerService.listPage(request));
    }

    @Operation(summary = "商户开户", description = "新增一个商户/客户")
    @Log(title = "商户管理", businessType = BusinessType.INSERT)
    @PostMapping("/create")
    public Result<CustomerResponse> create(@Valid @RequestBody CustomerCreateRequest request) {
        CustomerResponse resp = customerService.create(request);
        return Result.success(resp);
    }

    @Operation(summary = "商户信息修改", description = "修改商户的联系方式、备注等信息")
    @Log(title = "商户管理", businessType = BusinessType.UPDATE)
    @PutMapping("/update")
    public Result<CustomerResponse> update(@Parameter(description = "商户ID") @RequestParam Long id,
                                            @Valid @RequestBody CustomerUpdateRequest request) {
        CustomerResponse resp = customerService.update(id, request);
        return Result.success(resp);
    }

    @Operation(summary = "商户注销", description = "逻辑删除商户，将 isDeleted 置为 1")
    @Log(title = "商户管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{id}")
    public Result<Void> delete(@Parameter(description = "商户ID") @PathVariable Long id) {
        customerService.delete(id);
        return Result.success("注销成功");
    }

    @Operation(summary = "导出商户信息 Excel 报表",
            description = "按当前筛选条件导出商户列表为 .xlsx 文件。非 ADMIN 仅导出自身关联商户。")
    @Log(title = "商户管理", businessType = BusinessType.EXPORT)
    @GetMapping("/export")
    public void export(@Valid CustomerPageRequest request,
                       HttpServletResponse response) throws IOException {

        List<CustomerExcelVO> excelData = customerService.exportList(request);

        log.info("[商户导出] 操作人={} isAdmin={} 行数={}",
                SecurityUserContext.getCurrentUsername(), SecurityUserContext.isAdmin(), excelData.size());

        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());

        String fileName = "商户信息_" + LocalDateTime.now().format(FILE_DATE_FMT);
        String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8)
                .replaceAll("\\+", "%20");
        response.setHeader("Content-Disposition",
                "attachment; filename=" + encodedFileName + ".xlsx");
        response.setHeader("Access-Control-Expose-Headers", "Content-Disposition");

        EasyExcel.write(response.getOutputStream(), CustomerExcelVO.class)
                .sheet("商户信息")
                .doWrite(excelData);
    }

    @Operation(summary = "导出水表客户")
    @GetMapping("/water/export")
    public void exportWater(@Valid CustomerPageRequest request, HttpServletResponse response) throws IOException {
        request.setMeterType(0);
        exportFiltered(request, response, "水表客户信息");
    }

    @Operation(summary = "导出电表客户")
    @GetMapping("/electric/export")
    public void exportElectric(@Valid CustomerPageRequest request, HttpServletResponse response) throws IOException {
        request.setMeterType(1);
        exportFiltered(request, response, "电表客户信息");
    }

    private void exportFiltered(CustomerPageRequest request, HttpServletResponse response, String title) throws IOException {
        List<CustomerExcelVO> excelData = customerService.exportList(request);
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        String encodedFileName = URLEncoder.encode(title + "_" + LocalDateTime.now().format(FILE_DATE_FMT), StandardCharsets.UTF_8)
                .replaceAll("\\+", "%20");
        response.setHeader("Content-Disposition", "attachment; filename=" + encodedFileName + ".xlsx");
        response.setHeader("Access-Control-Expose-Headers", "Content-Disposition");
        EasyExcel.write(response.getOutputStream(), CustomerExcelVO.class).sheet(title).doWrite(excelData);
    }

    @Operation(summary = "商户状态变更", description = "启用（status=1）或停用（status=0）指定商户")
    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(@Parameter(description = "商户ID") @PathVariable Long id,
                                      @Valid @RequestBody CustomerStatusRequest request) {
        customerService.updateStatus(id, request.getStatus());
        return Result.success(request.getStatus() == 1 ? "商户已启用" : "商户已停用");
    }
}
