package com.competition.energycontrollerplatform.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.competition.energycontrollerplatform.dto.*;
import jakarta.servlet.http.HttpServletResponse;

import java.util.List;

/**
 * 公共分摊服务接口。
 * <p>
 * 将公共区域（水泵、公共照明等）产生的费用按策略分摊到各商户/分户表计。
 * 分摊操作在 {@code @Transactional} 事务内执行，确保余额扣减与流水写入原子一致。
 */
public interface PublicAllocationService {

    /**
     * 公共分摊分页列表查询。
     *
     * @param request 分页查询请求
     * @return 分页结果
     */
    IPage<PublicAllocationResultVO> listPage(PublicAllocationPageRequest request);

    /**
     * 执行公共分摊。
     * <p>
     * <b>分摊策略：</b>
     * <ul>
     *   <li>{@code PER_HOUSEHOLD} — 按户等分：总金额 / 目标表计数</li>
     *   <li>{@code BY_USAGE_RATIO} — 按历史用量比例分摊：各表近期用电量 / 总用电量 × 总金额</li>
     * </ul>
     * <p>
     * <b>事务保障：</b>
     * <ul>
     *   <li>使用 SQL 原子更新 {@code SET balance = balance - #{amount} AND balance >= #{amount}}</li>
     *   <li>每台表计扣减成功后写入 t_account_flow（trade_type=7 公共分摊）</li>
     *   <li>写入 t_public_allocation 分摊记录</li>
     * </ul>
     *
     * @param request 分摊请求
     * @return 分摊结果（含各表明细）
     */
    PublicAllocationResultVO execute(PublicAllocationExecuteRequest request);

    /**
     * 导出公共分摊列表（Excel）。
     * <p>
     * 支持按筛选条件导出全部，或按指定 ID 列表批量导出。
     *
     * @param request 筛选条件（可为 null，表示无筛选）
     * @param ids     指定要导出的分摊记录 ID 列表（可与筛选条件同时使用）
     * @return Excel 导出数据列表
     */
    List<PublicAllocationExcelVO> exportList(PublicAllocationPageRequest request, List<Long> ids);

    /**
     * 公共分摊统计聚合查询。
     * <p>
     * 按日/月/年维度聚合分摊总金额、分摊次数、平均金额。
     * 聚合在 MySQL 层通过 DATE_FORMAT + SUM + GROUP BY 完成，不捞回 JVM。
     * 公共分摊为公区费用，不做商户隔离。
     *
     * @param request 统计查询请求
     * @return 统计结果列表
     */
    List<PublicAllocationSummaryVO> getAllocationSummary(PublicAllocationSummaryRequest request);

    /**
     * 导出公共分摊统计为 Excel。
     *
     * @param response HTTP 响应
     * @param request  统计查询请求
     */
    void exportAllocationSummary(HttpServletResponse response, PublicAllocationSummaryRequest request);
}
