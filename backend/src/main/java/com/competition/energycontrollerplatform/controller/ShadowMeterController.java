package com.competition.energycontrollerplatform.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.competition.energycontrollerplatform.common.Result;
import com.competition.energycontrollerplatform.common.annotation.BusinessType;
import com.competition.energycontrollerplatform.common.annotation.Log;
import com.competition.energycontrollerplatform.dto.*;
import com.competition.energycontrollerplatform.service.ShadowMeterService;
import com.competition.energycontrollerplatform.service.MeterService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 虚拟电表（总分表拓扑）Controller。
 * <p>
 * 管理总表→分表层级拓扑关系，提供线损/损耗分析接口。
 * 线损 = 总表最新读数 - 所有分表最新读数之和。
 */
@Tag(name = "电能/水能管理 - 项目信息 - 虚拟电表/水表", description = "总分表拓扑管理、线损/损耗分析接口")
@RestController
@RequestMapping("/api/v1/shadow-meter")
@RequiredArgsConstructor
@Validated
public class ShadowMeterController {

    private final ShadowMeterService shadowMeterService;
    private final MeterService meterService;

    /**
     * Returns physical meters that can be selected when building an electric
     * virtual meter.  This deliberately reuses the authoritative meter
     * management query (including merchant/name, meter number and group
     * filters) so the selector never creates local placeholder meters.
     */
    @Operation(summary = "查询可绑定的真实电表")
    @GetMapping("/available-real-meters")
    public Result<IPage<MeterResponse>> availableRealMeters(
            @Valid MeterPageRequest request,
            @RequestParam(defaultValue = "1") Integer meterType) {
        if (meterType != 0 && meterType != 1) {
            meterType = 1;
        }
        request.setMeterType(meterType);
        request.setMeterKind("PHYSICAL");
        return Result.success(meterService.listPage(request));
    }

    @Operation(summary = "查询虚拟表关联真实表")
    @GetMapping("/{id}/real-meters")
    public Result<List<ShadowMeterRealMeterVO>> realMeters(@PathVariable Long id) {
        return Result.success(shadowMeterService.listRealMeters(id));
    }

    @Operation(summary = "为虚拟表关联真实电表")
    @PostMapping("/{id}/real-meters")
    public Result<Void> bindRealMeters(
            @Parameter(description = "虚拟电表ID") @PathVariable Long id,
            @Valid @RequestBody BindRealMetersRequest request) {
        shadowMeterService.bindRealMeters(id, request.getMeterIds());
        return Result.success();
    }

    @Operation(summary = "解除虚拟表与真实电表关联")
    @DeleteMapping("/real-meters/{relationId}")
    public Result<Void> unbindRealMeter(
            @Parameter(description = "关联关系ID") @PathVariable Long relationId) {
        shadowMeterService.unbindRealMeter(relationId);
        return Result.success();
    }

    // ──────────────── 数据列表 & 统计 ────────────────

    @Operation(summary = "虚拟电表统计汇总", description = "按类型、状态、项目等维度汇总拓扑关系数据")
    @GetMapping("/statistics")
    public Result<ShadowMeterStatisticsVO> statistics() {
        ShadowMeterStatisticsVO stats = shadowMeterService.getStatistics();
        return Result.success(stats);
    }

    @Operation(summary = "分页查询总分表拓扑关系", description = "支持按项目、总表、子表、类型、状态筛选，支持关键字搜索（匹配总表名/子表名/关系描述）")
    @GetMapping("/page")
    public Result<IPage<ShadowMeterVO>> page(@Valid ShadowMeterPageRequest request) {
        IPage<ShadowMeterVO> page = shadowMeterService.page(request);
        return Result.success(page);
    }

    @Operation(summary = "查询拓扑关系详情")
    @GetMapping("/{id}")
    public Result<ShadowMeterVO> getById(@Parameter(description = "关系ID") @PathVariable Long id) {
        ShadowMeterVO vo = shadowMeterService.getById(id);
        return Result.success(vo);
    }

    // ──────────────── CRUD ────────────────

    @Operation(summary = "创建总分表拓扑关系", description = "为一个总表挂载一个子表，建立层级拓扑。同时可设置子表的商户名、分组、告警额、透支额、付费方式等属性。")
    @PostMapping
    public Result<ShadowMeterVO> create(@Valid @RequestBody ShadowMeterRequest request) {
        ShadowMeterVO vo = shadowMeterService.create(request);
        return Result.success(vo);
    }

    @Operation(summary = "更新拓扑关系", description = "修改拓扑关系及同步修改子表信息（分组、商户名、告警额、透支额、付费方式、CT/PT变比等）。")
    @PutMapping("/{id}")
    public Result<ShadowMeterVO> update(
            @Parameter(description = "关系ID") @PathVariable Long id,
            @Valid @RequestBody ShadowMeterRequest request) {
        ShadowMeterVO vo = shadowMeterService.update(id, request);
        return Result.success(vo);
    }

    @Operation(summary = "删除拓扑关系（逻辑删除）")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@Parameter(description = "关系ID") @PathVariable Long id) {
        shadowMeterService.delete(id);
        return Result.success();
    }

    @Operation(summary = "批量删除拓扑关系")
    @DeleteMapping("/batch")
    public Result<String> batchDelete(@Parameter(description = "关系ID列表") @RequestBody List<Long> ids) {
        int count = shadowMeterService.batchDelete(ids);
        return Result.success("批量删除成功，共删除 " + count + " 条拓扑关系", String.valueOf(count));
    }

    // ──────────────── 批量导出 ────────────────

    @Operation(summary = "导出全量虚拟电表/水表 Excel", description = "按筛选条件导出全量虚拟电表/水表拓扑关系为 Excel .xlsx 文件。使用 EasyExcel 流式写入。")
    @Log(title = "虚拟电表管理", businessType = BusinessType.EXPORT)
    @GetMapping("/export")
    public void exportList(@Valid ShadowMeterPageRequest request, HttpServletResponse response) throws Exception {
        shadowMeterService.exportToExcel(response, request);
    }

    @Operation(summary = "按ID批量导出虚拟电表/水表 Excel", description = "按ID列表批量导出虚拟电表/水表拓扑关系为 .xlsx 文件。")
    @Log(title = "虚拟电表管理", businessType = BusinessType.EXPORT)
    @GetMapping("/export-batch")
    public void exportBatch(
            @Parameter(description = "关系ID列表（逗号分隔）", required = true)
            @RequestParam List<Long> ids,
            HttpServletResponse response) throws Exception {
        shadowMeterService.exportByIdsToExcel(response, ids);
    }

    // ──────────────── 线损分析 ────────────────

    @Operation(summary = "线损/损耗分析", description = "计算指定总表与其所有分表之间的线路损耗。"
            + "<p><b>计算逻辑：</b>"
            + "<ol><li>获取总表最新读数</li>"
            + "<li>遍历所有绑定子表，获取每台子表最新读数并累加</li>"
            + "<li>绝对损耗量 = 总表读数 - 分表读数总和</li>"
            + "<li>损耗率 = 绝对损耗 / 总表读数 × 100%</li></ol>"
            + "返回每台子表的明细读数、合计值和损耗分析结果。")
    @GetMapping("/{masterId}/loss-analysis")
    public Result<MasterLossVO> lossAnalysis(
            @Parameter(description = "总表ID", required = true) @PathVariable Long masterId) {
        MasterLossVO vo = shadowMeterService.lossAnalysis(masterId);
        return Result.success(vo);
    }

    // ──────────────── 更多操作 ────────────────

    @Operation(summary = "查询子表充值记录", description = "查询指定子表的所有充值订单记录，支持分页。")
    @GetMapping("/{childMeterId}/recharge-records")
    public Result<IPage<RechargeRecordVO>> rechargeRecords(
            @Parameter(description = "子表ID") @PathVariable Long childMeterId,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int pageNum,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "20") int pageSize) {
        IPage<RechargeRecordVO> page = shadowMeterService.getRechargeRecords(childMeterId, pageNum, pageSize);
        return Result.success(page);
    }

    @Operation(summary = "查询子表操作记录", description = "查询指定子表关联的所有操作日志，支持分页。")
    @GetMapping("/{childMeterId}/operation-logs")
    public Result<IPage<OperationLogVO>> operationLogs(
            @Parameter(description = "子表ID") @PathVariable Long childMeterId,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int pageNum,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "20") int pageSize) {
        IPage<OperationLogVO> page = shadowMeterService.getOperationLogs(childMeterId, pageNum, pageSize);
        return Result.success(page);
    }

    @Operation(summary = "获取子表二维码信息", description = "返回子表的基本信息用于前端生成二维码（含表号、商户名、表计类型等）。")
    @GetMapping("/{childMeterId}/qrcode")
    public Result<MeterResponse> qrcode(
            @Parameter(description = "子表ID") @PathVariable Long childMeterId) {
        MeterResponse info = shadowMeterService.getQrCodeInfo(childMeterId);
        return Result.success(info);
    }

    @Operation(summary = "下载子表二维码图片", description = "生成并返回指定子表的二维码 PNG 图片（300×300），可直接打印张贴到电表/水表上。扫码后可通过小程序绑定接口绑定到商户。")
    @GetMapping("/{childMeterId}/qrcode-image")
    public ResponseEntity<byte[]> qrcodeImage(
            @Parameter(description = "子表ID") @PathVariable Long childMeterId) {
        byte[] imageBytes = shadowMeterService.generateQrCodeImage(childMeterId);
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=meter_qrcode_" + childMeterId + ".png")
                .body(imageBytes);
    }

    @Operation(summary = "查询子表计费轨迹", description = "查询指定子表的资金流水明细（消费扣费记录），展示每笔扣费前后的余额变化，支持分页。")
    @GetMapping("/{childMeterId}/billing-track")
    public Result<IPage<BillingTrackVO>> billingTrack(
            @Parameter(description = "子表ID") @PathVariable Long childMeterId,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int pageNum,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "20") int pageSize) {
        IPage<BillingTrackVO> page = shadowMeterService.getBillingTrack(childMeterId, pageNum, pageSize);
        return Result.success(page);
    }
}
