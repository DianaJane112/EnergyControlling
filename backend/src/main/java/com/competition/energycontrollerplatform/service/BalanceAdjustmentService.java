package com.competition.energycontrollerplatform.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.competition.energycontrollerplatform.dto.BalanceAdjustmentExecuteRequest;
import com.competition.energycontrollerplatform.dto.BalanceAdjustmentPageRequest;
import com.competition.energycontrollerplatform.dto.BalanceAdjustmentSummaryVO;
import com.competition.energycontrollerplatform.dto.BalanceAdjustmentVO;
import jakarta.servlet.http.HttpServletResponse;

import java.util.List;

/**
 * 余额调整服务接口。
 * <p>
 * 提供余额调整记录的分页查询、详情查询、统计聚合、Excel 导出功能。
 * 所有聚合统计均在 MySQL 层面完成。
 */
public interface BalanceAdjustmentService {

    /**
     * 余额调整记录分页查询。
     * <p>支持按时段、商户ID、调整类型、关键字过滤。</p>
     */
    IPage<BalanceAdjustmentVO> listAdjustments(BalanceAdjustmentPageRequest request);

    /**
     * 查询单条余额调整详情。
     */
    BalanceAdjustmentVO getAdjustmentById(Long id);

    /**
     * 余额调整统计（按日聚合）。
     * <p>直接在 MySQL 层使用 DATE_FORMAT + SUM + GROUP BY 聚合。</p>
     */
    List<BalanceAdjustmentSummaryVO> getAdjustmentSummary(BalanceAdjustmentPageRequest request);

    /**
     * 余额调整记录导出（Excel，EasyExcel 流式写入）。
     *
     * @param response HttpServletResponse（直接写入 outputStream）
     * @param request  筛选条件
     */
    void exportAdjustments(HttpServletResponse response, BalanceAdjustmentPageRequest request);

    /**
     * 余额退补统一执行。
     * <p>
     * 串联"调整电价 → 选择表计 → 预览 → 执行"全流程：
     * <ol>
     *   <li>校验表计与商户信息</li>
     *   <li>若指定 adjustedPrice 且与当前电价不同，则更新表计电价</li>
     *   <li>执行余额变动（增加=退费，扣减=补收），原子 SQL 保障余额一致性</li>
     *   <li>写入 t_balance_adjustment 审计日志和 t_account_flow 资金流水</li>
     * </ol>
     * 若 previewOnly=true，仅计算预览结果不执行。
     * </p>
     *
     * @param request 退补执行请求
     * @return 预览或执行结果
     */
    BalanceAdjustmentVO executeAdjustment(BalanceAdjustmentExecuteRequest request);
}
