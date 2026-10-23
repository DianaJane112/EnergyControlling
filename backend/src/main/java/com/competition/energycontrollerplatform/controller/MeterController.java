package com.competition.energycontrollerplatform.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.competition.energycontrollerplatform.common.Result;
import com.competition.energycontrollerplatform.common.annotation.BusinessType;
import com.competition.energycontrollerplatform.common.annotation.Log;
import com.competition.energycontrollerplatform.dto.*;
import com.competition.energycontrollerplatform.service.MeterService;
import com.competition.energycontrollerplatform.service.OperationLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

/**
 * 表计管理 Controller
 */
@Tag(name = "电能/水能管理 - 项目信息 - 表计管理", description = "表计/设备的新增、编辑、查询、删除、状态变更、批量导入、商户绑定接口")
@RestController
@RequestMapping("/api/v1/meters")
@RequiredArgsConstructor
@Validated
public class MeterController {

    private final MeterService meterService;
    private final OperationLogService operationLogService;

    @Operation(summary = "表计统计汇总", description = "按类型、状态、业务类别、标记状态、通讯状态等维度汇总表计数据")
    @GetMapping("/statistics")
    public Result<MeterStatisticsVO> statistics() {
        MeterStatisticsVO stats = meterService.getStatistics();
        return Result.success(stats);
    }

    @Operation(summary = "表计分页列表", description = "支持按钢印号、电表号、商户名、客户、状态等16个条件筛选")
    @GetMapping
    public Result<IPage<MeterResponse>> list(@Valid MeterPageRequest request) {
        IPage<MeterResponse> page = meterService.listPage(request);
        return Result.success(page);
    }

    @Operation(summary = "表计详情")
    @GetMapping("/{id}")
    public Result<MeterResponse> detail(@Parameter(description = "表计ID") @PathVariable Long id) {
        MeterResponse resp = meterService.getById(id);
        return Result.success(resp);
    }

    @Operation(summary = "新增表计")
    @Log(title = "表计管理", businessType = BusinessType.INSERT)
    @PostMapping
    public Result<MeterResponse> create(@Valid @RequestBody MeterRequest request) {
        MeterResponse resp = meterService.create(request);
        return Result.success(resp);
    }

    @Operation(summary = "编辑表计")
    @Log(title = "表计管理", businessType = BusinessType.UPDATE)
    @PutMapping("/{id}")
    public Result<MeterResponse> update(@Parameter(description = "表计ID") @PathVariable Long id,
                                         @Valid @RequestBody MeterRequest request) {
        MeterResponse resp = meterService.update(id, request);
        return Result.success(resp);
    }

    @Operation(summary = "删除表计（逻辑删除）")
    @Log(title = "表计管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{id}")
    public Result<Void> delete(@Parameter(description = "表计ID") @PathVariable Long id) {
        meterService.delete(id);
        return Result.success("删除成功");
    }

    @Operation(summary = "表计状态变更", description = "变更表计运行状态：0-停用，1-正常运行，2-欠费停机")
    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(@Parameter(description = "表计ID") @PathVariable Long id,
                                      @Valid @RequestBody MeterStatusRequest request) {
        meterService.updateStatus(id, request.getStatus());
        return Result.success("状态更新成功");
    }

    @Operation(summary = "表计绑定商户", description = "将表计关联到指定商户")
    @PutMapping("/{id}/bind-customer")
    public Result<Void> bindCustomer(@Parameter(description = "表计ID") @PathVariable Long id,
                                      @Valid @RequestBody MeterBindCustomerRequest request) {
        meterService.bindCustomer(id, request.getCustomerId());
        return Result.success("绑定商户成功");
    }

    @Operation(summary = "批量导入表计", description = "一次性导入多条表计记录，单次最多500条")
    @PostMapping("/batch")
    public Result<String> batchImport(@Valid @RequestBody MeterBatchImportRequest request) {
        int count = meterService.batchImport(request.getMeters());
        return Result.success("批量导入成功，共导入 " + count + " 条记录", String.valueOf(count));
    }

    @Operation(summary = "余额清零", description = "将指定表计关联商户的余额清零。"
            + "仅 ADMIN 可执行。清零前记录清零前余额到资金流水，并自动更新表计状态为欠费停机。")
    @Log(title = "表计管理", businessType = BusinessType.OTHER)
    @PutMapping("/{id}/clear-balance")
    public Result<String> clearBalance(@Parameter(description = "表计ID") @PathVariable Long id,
                                        @Valid @RequestBody ClearBalanceRequest request) {
        // 强制使用路径中的 id，杜绝前端篡改
        request.setMeterId(id);
        java.math.BigDecimal clearedAmount = meterService.clearBalance(request);
        return Result.success("余额清零成功，清零金额：" + clearedAmount.toPlainString() + " 元",
                clearedAmount.toPlainString());
    }

    // ──────────────── 批量/全局修改电价/水价（拆分后 4 个独立接口） ────────────────

    @Operation(summary = "批量修改电价", description = "批量修改电表电价及数段设置。仅 ADMIN 可执行。"
            + "支持按表计ID列表或按商户两种模式。仅修改电表（meterType=1）。")
    @Log(title = "表计管理", businessType = BusinessType.UPDATE)
    @PutMapping("/batch-update-electricity-price")
    public Result<BatchUpdatePriceResponse> batchUpdateElectricityPrice(@Valid @RequestBody BatchPriceRequest request) {
        BatchUpdatePriceResponse response = meterService.batchUpdateElectricityPrice(request);
        return Result.success(String.format("批量电价修改完成：共 %d 台，成功 %d 台，失败 %d 台",
                response.getTotal(), response.getSuccess(), response.getFailed()), response);
    }

    @Operation(summary = "批量修改水价", description = "批量修改水表水价及数段设置。仅 ADMIN 可执行。"
            + "支持按表计ID列表或按商户两种模式。仅修改水表（meterType=0）。")
    @Log(title = "表计管理", businessType = BusinessType.UPDATE)
    @PutMapping("/batch-update-water-price")
    public Result<BatchUpdatePriceResponse> batchUpdateWaterPrice(@Valid @RequestBody BatchPriceRequest request) {
        BatchUpdatePriceResponse response = meterService.batchUpdateWaterPrice(request);
        return Result.success(String.format("批量水价修改完成：共 %d 台，成功 %d 台，失败 %d 台",
                response.getTotal(), response.getSuccess(), response.getFailed()), response);
    }

    @Operation(summary = "全局修改电价", description = "全局修改全园区所有电表的电价及数段设置。仅 ADMIN 可执行。仅修改电表（meterType=1）。")
    @Log(title = "表计管理", businessType = BusinessType.UPDATE)
    @PutMapping("/global-update-electricity-price")
    public Result<BatchUpdatePriceResponse> globalUpdateElectricityPrice(@Valid @RequestBody GlobalPriceRequest request) {
        BatchUpdatePriceResponse response = meterService.globalUpdateElectricityPrice(request);
        return Result.success(String.format("全局电价修改完成：共 %d 台，成功 %d 台，失败 %d 台",
                response.getTotal(), response.getSuccess(), response.getFailed()), response);
    }

    @Operation(summary = "全局修改水价", description = "全局修改全园区所有水表的水价及数段设置。仅 ADMIN 可执行。仅修改水表（meterType=0）。")
    @Log(title = "表计管理", businessType = BusinessType.UPDATE)
    @PutMapping("/global-update-water-price")
    public Result<BatchUpdatePriceResponse> globalUpdateWaterPrice(@Valid @RequestBody GlobalPriceRequest request) {
        BatchUpdatePriceResponse response = meterService.globalUpdateWaterPrice(request);
        return Result.success(String.format("全局水价修改完成：共 %d 台，成功 %d 台，失败 %d 台",
                response.getTotal(), response.getSuccess(), response.getFailed()), response);
    }

    // ──────────────── 批量操作（新增） ────────────────

    @Operation(summary = "批量修改表计信息", description = "批量修改表计的名称、分组、类型、标记状态、告警开关、CT/PT变比等信息。仅 ADMIN 可执行。")
    @Log(title = "表计管理", businessType = BusinessType.UPDATE)
    @PutMapping("/batch-update-info")
    public Result<BatchOperationResponse> batchUpdateMeterInfo(@Valid @RequestBody BatchUpdateMeterRequest request) {
        BatchOperationResponse response = meterService.batchUpdateMeterInfo(request);
        return Result.success(response.getMessage(), response);
    }

    @Operation(summary = "批量导出表计数据", description = "按筛选条件导出全量表计数据为 Excel .xlsx 文件。"
            + "使用 EasyExcel 流式写入，支持大数据量导出。")
    @Log(title = "表计管理", businessType = BusinessType.EXPORT)
    @GetMapping("/export")
    public void exportList(@Valid MeterPageRequest request, HttpServletResponse response) throws IOException {
        meterService.exportMeterListToExcel(response, request);
    }

    @Operation(summary = "批量导出选中表计 Excel", description = "按ID列表批量导出表计为 .xlsx 文件。"
            + "非 ADMIN 仅可导出自己商户下的表计。")
    @Log(title = "表计管理", businessType = BusinessType.EXPORT)
    @GetMapping("/export-batch")
    public void exportBatch(
            @Parameter(description = "表计ID列表（逗号分隔）", required = true)
            @RequestParam List<Long> ids,
            HttpServletResponse response) throws IOException {
        meterService.exportMeterListByIdsToExcel(response, ids);
    }

    @Operation(summary = "批量/全局开关阀（分合闸）", description = "批量或全局控制表计的开关阀（分合闸）。仅 ADMIN 可执行。"
            + "支持三种模式：按表计ID列表 / 按商户 / 全局操作。")
    @Log(title = "表计管理", businessType = BusinessType.OTHER)
    @PutMapping("/batch-valve-control")
    public Result<BatchOperationResponse> batchValveControl(@Valid @RequestBody BatchValveControlRequest request) {
        BatchOperationResponse response = meterService.batchValveControl(request);
        return Result.success(response.getMessage(), response);
    }

    @Operation(summary = "批量/全局设置时段策略", description = "将费率时段策略批量分配到指定表计或全局所有表计。仅 ADMIN 可执行。"
            + "支持三种模式：按表计ID列表 / 按商户 / 全局设置。")
    @Log(title = "表计管理", businessType = BusinessType.UPDATE)
    @PutMapping("/batch-set-time-slot")
    public Result<BatchOperationResponse> batchSetTimeSlot(@Valid @RequestBody BatchTimeSlotRequest request) {
        BatchOperationResponse response = meterService.batchSetTimeSlot(request);
        return Result.success(response.getMessage(), response);
    }

    // ════════════════════════════════════════════════════════════
    //  删除电表（meterType=1）
    // ════════════════════════════════════════════════════════════

    @Tag(name = "电能/水能管理 - 项目信息 - 删除电表", description = "已删除电表列表查询（含查询/重置）、详情查看、单表恢复、批量恢复、操作记录分类筛选")
    @Operation(summary = "已删除电表列表", description = "查询逻辑删除的电表列表（绕过 MyBatis-Plus 全局过滤），自动筛选 meterType=1（电表）。"
            + "<p><b>查询条件：</b>关键字（钢印号/表号/商户名模糊匹配）、运行状态、分组、删除时间范围。"
            + "<p><b>数据展示：</b>返回完整的电表信息（含积成/辰鳗/捷先三系统全部字段、商户名）。"
            + "<p><b>重置：</b>不传任何筛选参数即可获取全部已删除电表列表。"
            + "<p>非 ADMIN 仅可查看本商户已删除电表。")
    @GetMapping("/deleted-electric-list")
    public Result<IPage<MeterResponse>> deletedElectricList(@Valid DeletedMeterPageRequest request) {
        request.setMeterType(1); // 强制仅查电表
        IPage<MeterResponse> page = meterService.listDeleted(request);
        return Result.success(page);
    }

    @Tag(name = "电能/水能管理 - 项目信息 - 删除电表")
    @Operation(summary = "已删除电表详情", description = "查看已删除电表（meterType=1）的完整信息。仅 ADMIN 可查看。")
    @GetMapping("/deleted-electric/{id}")
    public Result<MeterResponse> deletedElectricDetail(@Parameter(description = "电表ID") @PathVariable Long id) {
        MeterResponse resp = meterService.getDeletedById(id);
        return Result.success(resp);
    }

    @Tag(name = "电能/水能管理 - 项目信息 - 删除电表")
    @Operation(summary = "恢复已删除电表（单表）", description = "将单台逻辑删除的电表恢复为正常状态。仅 ADMIN 可执行。")
    @Log(title = "表计管理", businessType = BusinessType.UPDATE)
    @PutMapping("/{id}/restore-electric")
    public Result<Void> restoreElectric(@Parameter(description = "电表ID") @PathVariable Long id) {
        meterService.restore(id);
        return Result.success("电表恢复成功");
    }

    @Tag(name = "电能/水能管理 - 项目信息 - 删除电表")
    @Operation(summary = "批量恢复已删除电表", description = "批量将已删除的电表恢复为正常状态。仅 ADMIN 可执行。")
    @Log(title = "表计管理", businessType = BusinessType.UPDATE)
    @PutMapping("/batch-restore-electric")
    public Result<String> batchRestoreElectric(@Parameter(description = "电表ID列表") @RequestBody List<Long> ids) {
        int count = meterService.batchRestore(ids);
        return Result.success("批量恢复成功，共恢复 " + count + " 台电表", String.valueOf(count));
    }

    @Tag(name = "电能/水能管理 - 项目信息 - 删除电表")
    @Operation(summary = "电表操作记录（分类筛选）", description = "查询指定电表的所有操作日志，支持多维分类筛选。"
            + "<p><b>分类筛选：</b>"
            + "<ul><li>operationType — 操作类型精确筛选：QUERY(查询) / CREATE(新增) / UPDATE(修改) / DELETE(删除) / EXPORT(导出)</li>"
            + "<li>startTime / endTime — 操作时间范围筛选（格式：yyyy-MM-dd HH:mm:ss）</li>"
            + "<li>keyword — 操作描述关键字模糊搜索</li></ul>"
            + "<p>仅 ADMIN 可查看操作日志。")
    @GetMapping("/{id}/electric-operation-logs")
    public Result<IPage<OperationLogVO>> electricOperationLogs(
            @Parameter(description = "电表ID") @PathVariable Long id,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int pageNum,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "20") int pageSize,
            @Parameter(description = "操作类型筛选：QUERY/CREATE/UPDATE/DELETE/EXPORT")
            @RequestParam(required = false) String operationType,
            @Parameter(description = "操作时间起（yyyy-MM-dd HH:mm:ss）", example = "2026-05-01 00:00:00")
            @RequestParam(required = false) String startTime,
            @Parameter(description = "操作时间止（yyyy-MM-dd HH:mm:ss）", example = "2026-05-31 23:59:59")
            @RequestParam(required = false) String endTime,
            @Parameter(description = "操作描述关键字（模糊搜索）")
            @RequestParam(required = false) String keyword) {
        OperationLogPageRequest logRequest = new OperationLogPageRequest();
        logRequest.setPageNum(pageNum);
        logRequest.setPageSize(pageSize);
        logRequest.setMeterId(id);
        logRequest.setOperationType(operationType);
        logRequest.setStartTime(startTime);
        logRequest.setEndTime(endTime);
        logRequest.setKeyword(keyword);
        IPage<OperationLogVO> page = operationLogService.page(logRequest);
        return Result.success(page);
    }

    // ════════════════════════════════════════════════════════════
    //  删除水表（meterType=0）
    // ════════════════════════════════════════════════════════════

    @Tag(name = "电能/水能管理 - 项目信息 - 删除水表", description = "已删除水表列表查询（含查询/重置）、详情查看、单表恢复、批量恢复、操作记录分类筛选")
    @Operation(summary = "已删除水表列表", description = "查询逻辑删除的水表列表（绕过 MyBatis-Plus 全局过滤），自动筛选 meterType=0（水表）。"
            + "<p><b>查询条件：</b>关键字（钢印号/表号/商户名模糊匹配）、运行状态、分组、删除时间范围。"
            + "<p><b>数据展示：</b>返回完整的水表信息（含积成/辰鳗/捷先三系统全部字段、商户名）。"
            + "<p><b>重置：</b>不传任何筛选参数即可获取全部已删除水表列表。"
            + "<p>非 ADMIN 仅可查看本商户已删除水表。")
    @GetMapping("/deleted-water-list")
    public Result<IPage<MeterResponse>> deletedWaterList(@Valid DeletedMeterPageRequest request) {
        request.setMeterType(0); // 强制仅查水表
        IPage<MeterResponse> page = meterService.listDeleted(request);
        return Result.success(page);
    }

    @Tag(name = "电能/水能管理 - 项目信息 - 删除水表")
    @Operation(summary = "已删除水表详情", description = "查看已删除水表（meterType=0）的完整信息。仅 ADMIN 可查看。")
    @GetMapping("/deleted-water/{id}")
    public Result<MeterResponse> deletedWaterDetail(@Parameter(description = "水表ID") @PathVariable Long id) {
        MeterResponse resp = meterService.getDeletedById(id);
        return Result.success(resp);
    }

    @Tag(name = "电能/水能管理 - 项目信息 - 删除水表")
    @Operation(summary = "恢复已删除水表（单表）", description = "将单台逻辑删除的水表恢复为正常状态。仅 ADMIN 可执行。")
    @Log(title = "表计管理", businessType = BusinessType.UPDATE)
    @PutMapping("/{id}/restore-water")
    public Result<Void> restoreWater(@Parameter(description = "水表ID") @PathVariable Long id) {
        meterService.restore(id);
        return Result.success("水表恢复成功");
    }

    @Tag(name = "电能/水能管理 - 项目信息 - 删除水表")
    @Operation(summary = "批量恢复已删除水表", description = "批量将已删除的水表恢复为正常状态。仅 ADMIN 可执行。")
    @Log(title = "表计管理", businessType = BusinessType.UPDATE)
    @PutMapping("/batch-restore-water")
    public Result<String> batchRestoreWater(@Parameter(description = "水表ID列表") @RequestBody List<Long> ids) {
        int count = meterService.batchRestore(ids);
        return Result.success("批量恢复成功，共恢复 " + count + " 台水表", String.valueOf(count));
    }

    @Tag(name = "电能/水能管理 - 项目信息 - 删除水表")
    @Operation(summary = "水表操作记录（分类筛选）", description = "查询指定水表的所有操作日志，支持多维分类筛选。"
            + "<p><b>分类筛选：</b>"
            + "<ul><li>operationType — 操作类型精确筛选：QUERY(查询) / CREATE(新增) / UPDATE(修改) / DELETE(删除) / EXPORT(导出)</li>"
            + "<li>startTime / endTime — 操作时间范围筛选（格式：yyyy-MM-dd HH:mm:ss）</li>"
            + "<li>keyword — 操作描述关键字模糊搜索</li></ul>"
            + "<p>仅 ADMIN 可查看操作日志。")
    @GetMapping("/{id}/water-operation-logs")
    public Result<IPage<OperationLogVO>> waterOperationLogs(
            @Parameter(description = "水表ID") @PathVariable Long id,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int pageNum,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "20") int pageSize,
            @Parameter(description = "操作类型筛选：QUERY/CREATE/UPDATE/DELETE/EXPORT")
            @RequestParam(required = false) String operationType,
            @Parameter(description = "操作时间起（yyyy-MM-dd HH:mm:ss）", example = "2026-05-01 00:00:00")
            @RequestParam(required = false) String startTime,
            @Parameter(description = "操作时间止（yyyy-MM-dd HH:mm:ss）", example = "2026-05-31 23:59:59")
            @RequestParam(required = false) String endTime,
            @Parameter(description = "操作描述关键字（模糊搜索）")
            @RequestParam(required = false) String keyword) {
        OperationLogPageRequest logRequest = new OperationLogPageRequest();
        logRequest.setPageNum(pageNum);
        logRequest.setPageSize(pageSize);
        logRequest.setMeterId(id);
        logRequest.setOperationType(operationType);
        logRequest.setStartTime(startTime);
        logRequest.setEndTime(endTime);
        logRequest.setKeyword(keyword);
        IPage<OperationLogVO> page = operationLogService.page(logRequest);
        return Result.success(page);
    }

    // ──────────────── 远程分合闸 ────────────────

    @Operation(summary = "远程开阀（合闸/通电）", description = "向设备下发开阀指令。仅 ADMIN 可执行。")
    @Log(title = "表计管理", businessType = BusinessType.OTHER)
    @PutMapping("/{id}/valve/open")
    public Result<Void> openValve(@Parameter(description = "表计ID") @PathVariable Long id,
                                   @Valid @RequestBody ValveControlRequest request) {
        meterService.openValve(id, request.getReason());
        return Result.success("远程开阀指令已下发");
    }

    @Operation(summary = "远程关阀（分闸/断电）", description = "向设备下发关阀指令。仅 ADMIN 可执行。")
    @Log(title = "表计管理", businessType = BusinessType.OTHER)
    @PutMapping("/{id}/valve/close")
    public Result<Void> closeValve(@Parameter(description = "表计ID") @PathVariable Long id,
                                    @Valid @RequestBody ValveControlRequest request) {
        meterService.closeValve(id, request.getReason());
        return Result.success("远程关阀指令已下发");
    }

    // ──────────────── 二维码与管理解绑 ────────────────

    @Operation(summary = "获取表计二维码信息", description = "返回表计基本信息及二维码内容字符串，供管理端预览确认。")
    @GetMapping("/{id}/qrcode")
    public Result<MeterQrCodeVO> qrcode(@Parameter(description = "表计ID") @PathVariable Long id) {
        MeterQrCodeVO vo = meterService.getQrCodeInfo(id);
        return Result.success(vo);
    }

    @Operation(summary = "下载表计二维码图片", description = "生成并返回表计二维码 PNG 图片（300×300），扫码后跳转小程序绑定页面。")
    @GetMapping("/{id}/qrcode-image")
    public ResponseEntity<byte[]> qrcodeImage(@Parameter(description = "表计ID") @PathVariable Long id) {
        byte[] imageBytes = meterService.generateQrCodeImage(id);
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=meter_qrcode_" + id + ".png")
                .body(imageBytes);
    }

    @Operation(summary = "解绑表计商户", description = "将表计与当前关联商户解绑。仅 ADMIN 可执行。")
    @Log(title = "表计管理", businessType = BusinessType.UPDATE)
    @DeleteMapping("/{id}/unbind-customer")
    public Result<Void> unbindCustomer(@Parameter(description = "表计ID") @PathVariable Long id) {
        meterService.unbindCustomer(id);
        return Result.success("解绑商户成功");
    }
}
