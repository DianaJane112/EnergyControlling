package com.competition.energycontrollerplatform.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.competition.energycontrollerplatform.dto.*;
import jakarta.servlet.http.HttpServletResponse;

/**
 * 退费服务接口。
 * <p>
 * 提供退费明细分页、详情查询、导出，以及退费预览功能。
 */
public interface RefundService {

    /**
     * 退费明细分页查询。
     * <p>数据来源：t_refund_detail 联查 t_customer、t_meter。</p>
     */
    IPage<RefundDetailVO> listRefundDetails(FinancialPageRequest request);

    /**
     * 查询单条退费明细详情。
     */
    RefundDetailVO getRefundDetailById(Long id);

    /**
     * 退费预览：根据表计ID和拟退费金额，展示退费前后余额对比。
     * <p>包含表计信息、商户信息、当前电价、退费后是否触发停机等。</p>
     */
    RefundPreviewVO previewRefund(RefundPreviewRequest request);

    /**
     * 退费明细导出（Excel，EasyExcel 流式写入）。
     *
     * @param response HttpServletResponse
     * @param request  筛选条件
     */
    void exportRefundDetails(HttpServletResponse response, FinancialPageRequest request);
}
