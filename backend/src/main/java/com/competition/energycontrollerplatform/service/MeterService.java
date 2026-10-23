package com.competition.energycontrollerplatform.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.competition.energycontrollerplatform.dto.*;

import java.util.List;

/**
 * 表计管理服务接口
 */
public interface MeterService {

    /**
     * 分页查询表计列表
     */
    IPage<MeterResponse> listPage(MeterPageRequest request);

    /**
     * 根据ID查询表计详情
     */
    MeterResponse getById(Long id);

    /**
     * 新增表计
     */
    MeterResponse create(MeterRequest request);

    /**
     * 编辑表计
     */
    MeterResponse update(Long id, MeterRequest request);

    /**
     * 删除表计（逻辑删除）
     */
    void delete(Long id);

    /**
     * 表计状态变更（启用/停用/欠费停机）
     */
    void updateStatus(Long id, Integer status);

    /**
     * 表计绑定商户
     */
    void bindCustomer(Long meterId, Long customerId);

    /**
     * 批量导入表计
     *
     * @return 成功导入条数
     */
    int batchImport(List<MeterRequest> meters);

    /**
     * 余额清零：将指定表计关联商户的余额清零。
     * <p>仅 ADMIN 可执行。清零前记录清零前余额到资金流水，同时更新表计状态位。</p>
     *
     * @param request 清零请求（表计ID、清零原因）
     * @return 清零前余额
     */
    java.math.BigDecimal clearBalance(ClearBalanceRequest request);

    /**
     * 批量修改电价及数段设置。
     * <p>仅 ADMIN 可执行。支持按表计ID列表、按商户、或全局三种模式。</p>
     *
     * @param request 批量修改请求
     * @return 批量操作结果统计
     */
    BatchUpdatePriceResponse batchUpdatePrice(BatchUpdatePriceRequest request);

    // ──────────────── 批量/全局修改电价/水价（拆分后） ────────────────

    /**
     * 批量修改电价（按表计ID列表或按商户）。
     * <p>仅 ADMIN 可执行。仅修改电表（meterType=1）。</p>
     */
    BatchUpdatePriceResponse batchUpdateElectricityPrice(BatchPriceRequest request);

    /**
     * 批量修改水价（按表计ID列表或按商户）。
     * <p>仅 ADMIN 可执行。仅修改水表（meterType=0）。</p>
     */
    BatchUpdatePriceResponse batchUpdateWaterPrice(BatchPriceRequest request);

    /**
     * 全局修改电价（全园区所有电表）。
     * <p>仅 ADMIN 可执行。仅修改电表（meterType=1）。</p>
     */
    BatchUpdatePriceResponse globalUpdateElectricityPrice(GlobalPriceRequest request);

    /**
     * 全局修改水价（全园区所有水表）。
     * <p>仅 ADMIN 可执行。仅修改水表（meterType=0）。</p>
     */
    BatchUpdatePriceResponse globalUpdateWaterPrice(GlobalPriceRequest request);

    /**
     * 查询已删除电表分页列表（绕过逻辑删除过滤），支持多条件筛选。
     * <p>仅 ADMIN 可查全量；非 ADMIN 强制过滤当前商户。</p>
     *
     * @param request 分页查询请求（含筛选条件）
     * @return 已删除电表分页结果
     */
    com.baomidou.mybatisplus.core.metadata.IPage<MeterResponse> listDeleted(DeletedMeterPageRequest request);

    /**
     * 查询已删除电表/水表详情。
     *
     * @param id 表计ID
     * @return 表计详情
     */
    MeterResponse getDeletedById(Long id);

    /**
     * 恢复已删除电表。
     * <p>仅 ADMIN 可执行。将 is_deleted 从 1 原子更新回 0。</p>
     *
     * @param id 电表ID
     */
    void restore(Long id);

    /**
     * 批量恢复已删除电表/水表。
     * <p>仅 ADMIN 可执行。批量将 is_deleted 从 1 恢复为 0。</p>
     *
     * @param ids 电表ID列表
     * @return 成功恢复数量
     */
    int batchRestore(java.util.List<Long> ids);

    /**
     * 远程开阀（合闸/通电）。
     * <p>仅 ADMIN 可执行。下发开阀指令并写入操作日志。</p>
     *
     * @param id     电表ID
     * @param reason 操作原因
     */
    void openValve(Long id, String reason);

    /**
     * 远程关阀（分闸/断电）。
     * <p>仅 ADMIN 可执行。下发关阀指令并写入操作日志。</p>
     *
     * @param id     电表ID
     * @param reason 操作原因
     */
    void closeValve(Long id, String reason);

    // ──────────────── 批量操作 ────────────────

    /**
     * 批量修改表计信息。
     * <p>仅 ADMIN 可执行。支持批量修改名称、分组、类型、标记状态、告警开关、CT/PT变比等。</p>
     */
    BatchOperationResponse batchUpdateMeterInfo(BatchUpdateMeterRequest request);

    /**
     * 批量导出表计数据列表（用于Excel/CSV导出）。
     * <p>返回指定筛选条件下的全量表计数据，不分页。</p>
     */
    java.util.List<MeterResponse> exportMeterList(MeterPageRequest request);

    /**
     * 按筛选条件导出表计数据为 Excel 文件。
     * <p>使用 EasyExcel 流式写入 {@link jakarta.servlet.http.HttpServletResponse}，
     * 前端以 blob 方式下载。</p>
     */
    void exportMeterListToExcel(jakarta.servlet.http.HttpServletResponse response, MeterPageRequest request);

    /**
     * 按 ID 列表批量导出表计数据为 Excel 文件。
     * <p>非 ADMIN 仅可导出本商户数据。</p>
     */
    void exportMeterListByIdsToExcel(jakarta.servlet.http.HttpServletResponse response, java.util.List<Long> ids);

    /**
     * 批量/全局开关阀（分合闸）。
     * <p>仅 ADMIN 可执行。支持按ID列表、按商户、全局三种模式。</p>
     */
    BatchOperationResponse batchValveControl(BatchValveControlRequest request);

    /**
     * 批量/全局设置时段策略。
     * <p>仅 ADMIN 可执行。将费率策略批量分配到表计。</p>
     */
    BatchOperationResponse batchSetTimeSlot(BatchTimeSlotRequest request);

    /**
     * 表计统计汇总。
     * <p>按类型、状态、业务类别、标记状态、通讯状态等维度汇总表计数据。</p>
     *
     * @return 统计结果
     */
    MeterStatisticsVO getStatistics();

    // ──────────────── 二维码 ────────────────

    /**
     * 为物理表计生成二维码 PNG 图片（300×300）。
     * <p>内容格式：{@code METER:<meterType>:<dbNo>}，扫码后跳转小程序绑定页面。</p>
     *
     * @param meterId 表计ID
     * @return PNG 图片字节数组
     */
    byte[] generateQrCodeImage(Long meterId);

    /**
     * 获取表计二维码信息（管理端预览用）。
     *
     * @param meterId 表计ID
     * @return 二维码内容及表计摘要
     */
    MeterQrCodeVO getQrCodeInfo(Long meterId);

    /**
     * 解绑表计与商户的关联（仅管理员）。
     * <p>幂等：已解绑的表计再次调用直接返回成功。</p>
     *
     * @param meterId 表计ID
     */
    void unbindCustomer(Long meterId);
}
