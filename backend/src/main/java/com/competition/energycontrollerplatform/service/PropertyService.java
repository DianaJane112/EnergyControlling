package com.competition.energycontrollerplatform.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.competition.energycontrollerplatform.dto.*;
import jakarta.servlet.http.HttpServletResponse;

import java.util.List;

/**
 * 物业管理/即时账单服务接口。
 * <p>提供即时账单生成、分页列表、导出、批量操作、缴费轨迹等物业管理功能。</p>
 */
public interface PropertyService {

    /**
     * 生成即时账单（单表计）。
     * <p>
     * 实时获取当前表计最新读数与上次结算读数，计算消耗用量和应付金额：
     * <pre>{@code billAmount = (currentReading - lastSettledReading) × multiplier × unitPrice}</pre>
     *
     * @param meterId 表计ID
     * @return 即时账单 VO
     */
    InstantBillVO generateInstantBill(Long meterId);

    /**
     * 即时账单分页列表查询。
     * <p>支持按商户、表计号、账单分类、时间范围、账单金额范围筛选。</p>
     *
     * @param request 分页查询请求
     * @return 分页列表
     */
    IPage<InstantBillListVO> listInstantBills(InstantBillPageRequest request);

    /**
     * 即时账单统计汇总。
     * <p>返回总账单数、总金额、已缴/未缴/逾期数量。</p>
     *
     * @param request 筛选条件
     * @return 统计汇总
     */
    java.util.Map<String, Object> getInstantBillStats(InstantBillPageRequest request);

    /**
     * 导出即时账单列表（Excel）。
     *
     * @param response HttpServletResponse
     * @param request  筛选条件
     */
    void exportInstantBills(HttpServletResponse response, InstantBillPageRequest request);

    // ──────────────────────── 批量操作 ────────────────────────

    /**
     * 批量创建账单。
     * <p>为指定表计列表批量生成即时账单并保存。</p>
     *
     * @param request 批量创建请求
     * @return 创建结果（成功数、失败数）
     */
    BatchOperationResponse batchCreateBills(BatchBillCreateRequest request);

    /**
     * 批量延期账单。
     *
     * @param request 批量延期请求
     * @return 操作结果
     */
    BatchOperationResponse batchDeferBills(BatchDeferRequest request);

    /**
     * 批量删除账单。
     * <p>删除指定表计关联的最近消费扣费流水记录（trade_type=2）。</p>
     *
     * @param meterIds 表计ID列表
     * @return 操作结果
     */
    BatchOperationResponse batchDeleteBills(List<Long> meterIds);

    /**
     * 批量导入账单（Excel 解析后入参）。
     *
     * @param request 导入请求
     * @return 操作结果
     */
    BatchOperationResponse batchImportBills(BillImportRequest request);

    /**
     * 下载批量导入模板（Excel）。
     *
     * @param response HttpServletResponse
     */
    void downloadImportTemplate(HttpServletResponse response);

    // ──────────────────────── 缴费轨迹 ────────────────────────

    /**
     * 缴费轨迹分页查询。
     * <p>基于 t_account_flow 表查询资金流水记录。</p>
     *
     * @param request 分页查询请求
     * @return 分页列表
     */
    IPage<BillingTrackVO> listBillingTracks(BillingTrackPageRequest request);

    /**
     * 导出缴费轨迹（Excel）。
     *
     * @param response HttpServletResponse
     * @param request  筛选条件
     */
    void exportBillingTracks(HttpServletResponse response, BillingTrackPageRequest request);
}
