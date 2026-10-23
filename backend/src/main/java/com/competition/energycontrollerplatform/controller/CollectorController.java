package com.competition.energycontrollerplatform.controller;

import com.alibaba.excel.EasyExcel;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.competition.energycontrollerplatform.common.Result;
import com.competition.energycontrollerplatform.common.annotation.BusinessType;
import com.competition.energycontrollerplatform.common.annotation.Log;
import com.competition.energycontrollerplatform.dto.*;
import com.competition.energycontrollerplatform.service.CollectorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 基站/集中器管理 Controller。
 * <p>
 * 对应需求文档中集中器/采集器的标准 CRUD。
 * 所有写操作与查询均强制注入 customer_id 进行商户隔离。
 */
@Tag(name = "电能/水能管理 - 项目信息 - 基站管理", description = "集中器/采集器的新增、编辑、查询、删除、统计、导出、电表绑定/解绑接口")
@RestController
@RequestMapping("/api/v1/collector")
@RequiredArgsConstructor
public class CollectorController {

    private static final Logger log = LoggerFactory.getLogger(CollectorController.class);
    private static final DateTimeFormatter FILE_DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    private final CollectorService collectorService;

    // ──────────────────────── 分页列表 ────────────────────────

    @Operation(summary = "集中器分页列表", description = "支持按关键字（编号/名称/位置）、商户ID、区域代码筛选")
    @GetMapping("/page")
    public Result<IPage<CollectorVO>> page(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int pageNum,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "20") int pageSize,
            @Parameter(description = "关键字（编号/名称/位置模糊匹配）") @RequestParam(required = false) String keyword,
            @Parameter(description = "商户ID（非ADMIN强制覆盖）") @RequestParam(required = false) Long customerId,
            @Parameter(description = "区域代码") @RequestParam(required = false) String areaCode) {
        IPage<CollectorVO> page = collectorService.page(pageNum, pageSize, keyword, customerId, areaCode);
        return Result.success(page);
    }

    // ──────────────────────── 详情 ────────────────────────

    @Operation(summary = "集中器详情")
    @GetMapping("/{id}")
    public Result<CollectorVO> detail(@Parameter(description = "集中器ID") @PathVariable Long id) {
        CollectorVO vo = collectorService.getById(id);
        return Result.success(vo);
    }

    // ──────────────────────── 新增 ────────────────────────

    @Operation(summary = "新增集中器")
    @Log(title = "基站管理", businessType = BusinessType.INSERT)
    @PostMapping
    public Result<CollectorVO> create(@Valid @RequestBody CollectorRequest request) {
        CollectorVO vo = collectorService.create(request);
        return Result.success(vo);
    }

    // ──────────────────────── 编辑 ────────────────────────

    @Operation(summary = "编辑集中器")
    @Log(title = "基站管理", businessType = BusinessType.UPDATE)
    @PutMapping("/{id}")
    public Result<CollectorVO> update(@Parameter(description = "集中器ID") @PathVariable Long id,
                                      @Valid @RequestBody CollectorRequest request) {
        CollectorVO vo = collectorService.update(id, request);
        return Result.success(vo);
    }

    // ──────────────────────── 删除 ────────────────────────

    @Operation(summary = "删除集中器（逻辑删除）")
    @Log(title = "基站管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{id}")
    public Result<Void> delete(@Parameter(description = "集中器ID") @PathVariable Long id) {
        collectorService.delete(id);
        return Result.success("删除成功");
    }

    // ──────────────────────── 下拉列表 ────────────────────────

    @Operation(summary = "按商户查询集中器下拉列表", description = "返回启用状态的集中器列表，供前端下拉框使用")
    @GetMapping("/list-by-customer")
    public Result<List<CollectorVO>> listByCustomer(
            @Parameter(description = "商户ID") @RequestParam(required = false) Long customerId) {
        List<CollectorVO> list = collectorService.listByCustomer(customerId);
        return Result.success(list);
    }

    // ──────────────────────── 统计 ────────────────────────

    @Operation(summary = "集中器统计", description = "返回集中器总数及按区域分组统计。"
            + "ADMIN 可查全量，非 ADMIN 仅统计本商户数据。")
    @GetMapping("/statistics")
    public Result<CollectorStatisticsVO> statistics(
            @Parameter(description = "商户ID（非ADMIN强制覆盖）") @RequestParam(required = false) Long customerId,
            @Parameter(description = "区域代码筛选") @RequestParam(required = false) String areaCode) {
        CollectorStatisticsVO vo = collectorService.statistics(customerId, areaCode);
        return Result.success(vo);
    }

    // ──────────────────────── 导出 ────────────────────────

    @Operation(summary = "导出集中器列表 Excel", description = "按筛选条件导出集中器列表为 .xlsx 文件。"
            + "使用 EasyExcel 流式写入，支持大数据量导出。")
    @Log(title = "基站管理", businessType = BusinessType.EXPORT)
    @GetMapping("/export")
    public void export(
            @Parameter(description = "关键字（编号/名称/位置模糊匹配）") @RequestParam(required = false) String keyword,
            @Parameter(description = "商户ID（非ADMIN强制覆盖）") @RequestParam(required = false) Long customerId,
            @Parameter(description = "区域代码") @RequestParam(required = false) String areaCode,
            HttpServletResponse response) throws IOException {

        List<CollectorExcelVO> data = collectorService.exportList(keyword, customerId, areaCode);

        log.info("[集中器导出] 操作人={} 行数={}", com.competition.energycontrollerplatform.security.SecurityUserContext.getCurrentUsername(), data.size());

        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());

        String fileName = "基站列表_" + LocalDateTime.now().format(FILE_DATE_FMT);
        String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8)
                .replaceAll("\\+", "%20");
        response.setHeader("Content-Disposition",
                "attachment; filename=" + encodedFileName + ".xlsx");
        response.setHeader("Access-Control-Expose-Headers", "Content-Disposition");

        EasyExcel.write(response.getOutputStream(), CollectorExcelVO.class)
                .sheet("基站列表")
                .doWrite(data);
    }

    @Operation(summary = "批量导出集中器 Excel", description = "按ID列表批量导出集中器为 .xlsx 文件。"
            + "非 ADMIN 仅可导出自己商户下的集中器。")
    @Log(title = "基站管理", businessType = BusinessType.EXPORT)
    @GetMapping("/export-batch")
    public void exportBatch(
            @Parameter(description = "集中器ID列表（逗号分隔）", required = true)
            @RequestParam List<Long> ids,
            HttpServletResponse response) throws IOException {

        List<CollectorExcelVO> data = collectorService.exportBatch(ids);

        log.info("[集中器批量导出] 操作人={} 行数={}", com.competition.energycontrollerplatform.security.SecurityUserContext.getCurrentUsername(), data.size());

        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());

        String fileName = "基站列表_批量_" + LocalDateTime.now().format(FILE_DATE_FMT);
        String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8)
                .replaceAll("\\+", "%20");
        response.setHeader("Content-Disposition",
                "attachment; filename=" + encodedFileName + ".xlsx");
        response.setHeader("Access-Control-Expose-Headers", "Content-Disposition");

        EasyExcel.write(response.getOutputStream(), CollectorExcelVO.class)
                .sheet("基站列表")
                .doWrite(data);
    }

    // ──────────────────────── 电表/水表绑定 ────────────────────────

    @Operation(summary = "查询基站下绑定的电表/水表", description = "分页查询指定集中器下绑定的电表/水表列表。"
            + "支持按关键字（钢印号/表号/名称）和表计类型筛选。")
    @GetMapping("/{id}/meters")
    public Result<IPage<CollectorMeterVO>> listMeters(
            @Parameter(description = "集中器ID") @PathVariable Long id,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int pageNum,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "20") int pageSize,
            @Parameter(description = "关键字（模糊匹配钢印号/表号/名称）") @RequestParam(required = false) String keyword,
            @Parameter(description = "表计类型：0=水表, 1=电表, 不传=全部") @RequestParam(required = false) Integer meterType) {
        IPage<CollectorMeterVO> page = collectorService.listMeters(id, pageNum, pageSize, keyword, meterType);
        return Result.success(page);
    }

    @Operation(summary = "绑定电表/水表到基站", description = "将指定表计绑定到该集中器（设置表计的 collectorCode）。"
            + "如果表计已绑定到其他集中器，需要先解绑。")
    @Log(title = "基站管理", businessType = BusinessType.UPDATE)
    @PostMapping("/{id}/bind-meter")
    public Result<Void> bindMeter(
            @Parameter(description = "集中器ID") @PathVariable Long id,
            @Valid @RequestBody CollectorBindMeterRequest request) {
        collectorService.bindMeter(id, request.getMeterId());
        return Result.success("绑定成功");
    }

    @Operation(summary = "解除单个电表/水表绑定", description = "将指定表计与该集中器解绑（清空表计的 collectorCode）。")
    @Log(title = "基站管理", businessType = BusinessType.UPDATE)
    @DeleteMapping("/{id}/unbind-meter/{meterId}")
    public Result<Void> unbindMeter(
            @Parameter(description = "集中器ID") @PathVariable Long id,
            @Parameter(description = "表计ID") @PathVariable Long meterId) {
        collectorService.unbindMeter(id, meterId);
        return Result.success("解绑成功");
    }

    @Operation(summary = "解绑所有电表/水表", description = "将该集中器下所有已绑定的电表/水表全部解绑。")
    @Log(title = "基站管理", businessType = BusinessType.UPDATE)
    @DeleteMapping("/{id}/unbind-all-meters")
    public Result<String> unbindAllMeters(
            @Parameter(description = "集中器ID") @PathVariable Long id) {
        int count = collectorService.unbindAllMeters(id);
        return Result.success("已解绑 " + count + " 个表计", String.valueOf(count));
    }

    // ──────────────────────── 一键绑定 ────────────────────────

    @Operation(summary = "一键扫描绑定在线表计", description = "自动扫描所有在线且未绑定的电表/水表，"
            + "将同区域、同厂商的表计一键绑定到该集中器。仅 ADMIN 可执行。"
            + "<p>匹配规则：</p>"
            + "<ul>"
            + "<li>表计状态为在线（online=1）且未绑定到任何集中器</li>"
            + "<li>若集中器已配置区域代码，优先匹配同区域表计</li>"
            + "<li>若集中器已配置厂商代码，匹配同厂商表计</li>"
            + "</ul>")
    @Log(title = "基站管理", businessType = BusinessType.UPDATE)
    @PostMapping("/{id}/bind-all-online")
    public Result<String> bindAllOnlineMeters(
            @Parameter(description = "集中器ID") @PathVariable Long id) {
        int count = collectorService.bindAllOnlineMeters(id);
        return Result.success("一键绑定完成，共绑定 " + count + " 个在线表计", String.valueOf(count));
    }

    // ──────────────────────── 批量导入 ────────────────────────

    @Operation(summary = "批量导入集中器", description = "一次性导入多条集中器记录，单次最多500条。仅 ADMIN 可执行。")
    @Log(title = "基站管理", businessType = BusinessType.IMPORT)
    @PostMapping("/batch")
    public Result<String> batchImport(
            @Parameter(description = "集中器导入列表", required = true)
            @Valid @RequestBody List<CollectorRequest> requests) {
        int count = collectorService.batchImport(requests);
        return Result.success("批量导入成功，共导入 " + count + " 条记录", String.valueOf(count));
    }
}
