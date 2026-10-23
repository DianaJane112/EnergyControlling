package com.competition.energycontrollerplatform.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.competition.energycontrollerplatform.dto.*;
import jakarta.servlet.http.HttpServletResponse;

import java.util.List;

/**
 * 财务管理服务接口。
 * <p>
 * 提供充值明细分页、充值统计聚合、退费明细分页、Excel 导出四大功能。
 * 所有聚合统计均在 MySQL 层面完成，严禁 JVM 内存遍历。
 */
public interface FinancialService {

    /**
     * 充值明细分页查询。
     * <p>支持按时段、按商户、关键字（订单号/商户名）过滤。</p>
     */
    IPage<DepositLogVO> listDepositLog(FinancialPageRequest request);

    /**
     * 充值统计（按日/月/年聚合）。
     * <p>直接在 MySQL 层使用 DATE_FORMAT + SUM + GROUP BY 聚合。</p>
     */
    List<DepositSummaryVO> getDepositSummary(DepositSummaryRequest request);

    /**
     * 退费明细分页查询。
     * <p>数据来源：t_refund_log 联查 t_customer。</p>
     */
    IPage<RefundLogVO> listRefundLog(FinancialPageRequest request);

    /**
     * 充值明细分页导出（Excel，EasyExcel 文件流写入）。
     * <p>根据筛选条件拉取全量记录，以异步流式方式写入 Excel，避免 OOM。</p>
     *
     * @param response HttpServletResponse（直接写入 outputStream）
     * @param request  筛选条件
     */
    void exportDepositLog(HttpServletResponse response, FinancialPageRequest request);

    /**
     * 充值统计导出（Excel，EasyExcel 文件流写入）。
     * <p>按指定粒度导出充值汇总数据。</p>
     *
     * @param response HttpServletResponse（直接写入 outputStream）
     * @param request  筛选条件（granularity 决定日/月/年维度）
     */
    void exportDepositSummary(HttpServletResponse response, DepositSummaryRequest request);

    /**
     * 根据 ID 查询单条充值明细（用于打印缴费记录）。
     *
     * @param id 充值订单ID
     * @return 充值明细 VO，若不存在或无权访问返回 null
     */
    DepositLogVO getDepositLogById(Long id);

    /**
     * 退费明细导出（Excel，EasyExcel 文件流写入）。
     * <p>根据筛选条件拉取全量退费记录，以流式方式写入 Excel，避免 OOM。</p>
     *
     * @param response HttpServletResponse（直接写入 outputStream）
     * @param request  筛选条件
     */
    void exportRefundLog(HttpServletResponse response, FinancialPageRequest request);
}
