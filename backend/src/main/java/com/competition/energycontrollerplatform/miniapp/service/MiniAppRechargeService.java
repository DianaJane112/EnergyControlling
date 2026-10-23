package com.competition.energycontrollerplatform.miniapp.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.competition.energycontrollerplatform.miniapp.dto.RechargeHistoryVO;
import com.competition.energycontrollerplatform.miniapp.dto.RechargeSubmitRequest;
import com.competition.energycontrollerplatform.miniapp.entity.RechargeRequest;

/**
 * 小程序充值申请服务接口
 */
public interface MiniAppRechargeService extends IService<RechargeRequest> {

    /**
     * 提交充值申请。
     * <p>
     * 用户在小程序端发起虚拟充值，生成一条 PENDING 状态的申请记录。
     * 管理员在后台审核通过后才会执行实际充值操作。
     *
     * @param userId     小程序用户ID
     * @param customerId 商户ID（用于权限校验）
     * @param request    充值请求（含表计ID和金额）
     * @return 申请编号
     */
    String submitRecharge(Long userId, Long customerId, RechargeSubmitRequest request);

    /**
     * 分页查询充值历史记录。
     *
     * @param userId      小程序用户ID
     * @param pageNum     页码
     * @param pageSize    每页条数
     * @param auditStatus 审核状态筛选（PENDING/APPROVED/REJECTED，null=全部）
     * @return 分页结果
     */
    IPage<RechargeHistoryVO> getRechargeHistory(Long userId, Integer pageNum, Integer pageSize, String auditStatus);

    /**
     * 查询充值申请详情。
     *
     * @param requestId 申请ID
     * @param userId    小程序用户ID（用于权限校验）
     * @return 充值历史VO
     */
    RechargeHistoryVO getRechargeDetail(Long requestId, Long userId);
}
