package com.competition.energycontrollerplatform.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.competition.energycontrollerplatform.dto.RechargeAuditPageRequest;
import com.competition.energycontrollerplatform.miniapp.dto.RechargeHistoryVO;

/**
 * 管理端 — 充值审核服务
 */
public interface RechargeAuditService {

    /**
     * 分页查询所有充值申请（管理端可查看全部商户）。
     */
    IPage<RechargeHistoryVO> getAuditPage(RechargeAuditPageRequest request);

    /**
     * 审核通过：创建充值订单 + 增加商户余额 + 更新审核状态。
     *
     * @param requestId   充值申请ID
     * @param auditUserId 审核人（管理员）ID
     * @param remark      审核备注
     * @return 充值订单号
     */
    String approve(Long requestId, Long auditUserId, String remark);

    /**
     * 审核拒绝：记录拒绝原因。
     *
     * @param requestId   充值申请ID
     * @param auditUserId 审核人（管理员）ID
     * @param remark      拒绝原因
     */
    void reject(Long requestId, Long auditUserId, String remark);
}
