package com.competition.energycontrollerplatform.controller;

import com.alibaba.excel.EasyExcel;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.competition.energycontrollerplatform.common.Result;
import com.competition.energycontrollerplatform.common.annotation.BusinessType;
import com.competition.energycontrollerplatform.common.annotation.Log;
import com.competition.energycontrollerplatform.dto.*;
import com.competition.energycontrollerplatform.security.SecurityUserContext;
import com.competition.energycontrollerplatform.service.PublicAllocationService;
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
 * 公共分摊 Controller。
 * <p>
 * 将公共区域（水泵、公共照明等）产生的公摊费用按策略划分到各商户/分户表计中。
 * 仅 ADMIN 角色可执行分摊操作。
 */
@Tag(name = "公共分摊 - 公共分摊", description = "公区费用分摊计算与资金扣减接口")
@RestController
@RequestMapping("/api/v1/public-allocation")
@RequiredArgsConstructor
@Validated
public class PublicAllocationController {

    private static final Logger log = LoggerFactory.getLogger(PublicAllocationController.class);

    private final PublicAllocationService publicAllocationService;

    private static final DateTimeFormatter FILE_DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    // ──────────────────────── 数据列表 ────────────────────────

    @Operation(summary = "公共分摊列表分页查询", description = "支持按分摊名称模糊查询、按分摊方式、状态、生效日期范围筛选")
    @GetMapping("/page")
    public Result<IPage<PublicAllocationResultVO>> page(@Valid PublicAllocationPageRequest request) {
        IPage<PublicAllocationResultVO> page = publicAllocationService.listPage(request);
        return Result.success(page);
    }

    // ──────────────────────── 执行分摊 ────────────────────────

    @Operation(summary = "执行公共分摊", description = "将待分摊总金额按指定策略分配到目标电表列表。"
            + "<p><b>分摊策略：</b>"
            + "<ul>"
            + "<li>PER_HOUSEHOLD — 按户等分：总金额 ÷ 表计数</li>"
            + "<li>BY_USAGE_RATIO — 按历史用量比例分摊：各表近30天用电量占比 × 总金额</li>"
            + "</ul>"
            + "<p><b>工程保障：</b>"
            + "<ul>"
            + "<li>@Transactional 事务确保余额扣减与流水写入原子一致</li>"
            + "<li>BigDecimal 精确计算 + 尾差处理</li>"
            + "<li>SQL 原子更新 SET balance = balance - #{amount} AND balance >= #{amount}</li>"
            + "<li>逐表写入 t_account_flow（trade_type=7 公共分摊）</li>"
            + "</ul>")
    @Log(title = "公共分摊", businessType = BusinessType.INSERT)
    @PostMapping("/execute")
    public Result<PublicAllocationResultVO> execute(@Valid @RequestBody PublicAllocationExecuteRequest request) {
        PublicAllocationResultVO result = publicAllocationService.execute(request);
        return Result.success(result);
    }

    // ──────────────────────── 导出 ────────────────────────

    @Operation(summary = "导出公共分摊 Excel 报表",
            description = "按筛选条件导出公共分摊记录为 .xlsx 文件。支持通过 ids 参数批量选择导出，也支持按名称/方式/状态/日期筛选导出。仅 ADMIN 可操作。")
    @Log(title = "公共分摊", businessType = BusinessType.EXPORT)
    @GetMapping("/export")
    public void export(
            @Parameter(description = "要导出的分摊记录 ID 列表（逗号分隔），批量选择时使用", example = "1,2,3")
            @RequestParam(required = false) List<Long> ids,

            @Parameter(description = "分摊名称（模糊查询）")
            @RequestParam(required = false) String allocationName,

            @Parameter(description = "分摊方式")
            @RequestParam(required = false) String allocationType,

            @Parameter(description = "状态")
            @RequestParam(required = false) Integer status,

            @Parameter(description = "生效日期起（含）", example = "2026-05-01")
            @RequestParam(required = false) String startDate,

            @Parameter(description = "生效日期止（含）", example = "2026-05-31")
            @RequestParam(required = false) String endDate,

            HttpServletResponse response) throws IOException {

        // 构建查询条件
        PublicAllocationPageRequest request = new PublicAllocationPageRequest();
        request.setAllocationName(allocationName);
        request.setAllocationType(allocationType);
        request.setStatus(status);
        request.setStartDate(startDate);
        request.setEndDate(endDate);

        List<PublicAllocationExcelVO> excelData = publicAllocationService.exportList(request, ids);

        log.info("[分摊导出] 操作人={} isAdmin={} 行数={}",
                SecurityUserContext.getCurrentUsername(), SecurityUserContext.isAdmin(), excelData.size());

        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());

        String fileName = "公共分摊_" + LocalDateTime.now().format(FILE_DATE_FMT);
        String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8)
                .replaceAll("\\+", "%20");
        response.setHeader("Content-Disposition",
                "attachment; filename=" + encodedFileName + ".xlsx");
        response.setHeader("Access-Control-Expose-Headers", "Content-Disposition");

        EasyExcel.write(response.getOutputStream(), PublicAllocationExcelVO.class)
                .sheet("公共分摊")
                .doWrite(excelData);
    }

    // ──────────────────────── 分摊统计 ────────────────────────

    @Operation(summary = "公共分摊统计聚合", description = "按日/月/年维度聚合分摊总金额、分摊次数、平均金额。"
            + "聚合在 MySQL 层通过 DATE_FORMAT + SUM + GROUP BY 完成，不捞回 JVM。"
            + "公共分摊为公区费用，不做商户隔离。")
    @GetMapping("/summary")
    public Result<List<PublicAllocationSummaryVO>> summary(@Valid PublicAllocationSummaryRequest request) {
        List<PublicAllocationSummaryVO> summary = publicAllocationService.getAllocationSummary(request);
        return Result.success(summary);
    }

    @Operation(summary = "导出公共分摊统计 Excel", description = "按日/月/年粒度导出分摊统计数据为 Excel 文件。"
            + "导出字段：统计时段、分摊总金额、分摊次数、平均单次分摊金额。"
            + "使用 EasyExcel 流式写入，支持大数据量导出不 OOM。")
    @Log(title = "公共分摊", businessType = BusinessType.EXPORT)
    @GetMapping("/export/summary")
    public void exportSummary(@Valid PublicAllocationSummaryRequest request, HttpServletResponse response) {
        publicAllocationService.exportAllocationSummary(response, request);
    }
}
