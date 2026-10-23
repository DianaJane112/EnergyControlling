package com.competition.energycontrollerplatform.miniapp.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.competition.energycontrollerplatform.miniapp.entity.RechargeRequest;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;

/**
 * 小程序充值申请 Mapper
 */
@Mapper
public interface RechargeRequestMapper extends BaseMapper<RechargeRequest> {

    /**
     * 更新充值申请的审核状态。
     *
     * @param id             申请ID
     * @param auditStatus    审核状态 (APPROVED / REJECTED)
     * @param auditRemark    审核备注
     * @param auditUserId    审核人ID
     * @param auditTime      审核时间
     * @param relatedOrderNo 关联充值订单号（审核通过时必填）
     * @return 影响行数
     */
    @Update("UPDATE t_recharge_request SET audit_status = #{auditStatus}, " +
            "audit_remark = #{auditRemark}, audit_user_id = #{auditUserId}, " +
            "audit_time = #{auditTime}, related_order_no = #{relatedOrderNo}, " +
            "update_time = NOW() " +
            "WHERE id = #{id} AND is_deleted = 0")
    int updateAuditStatus(@Param("id") Long id,
                          @Param("auditStatus") String auditStatus,
                          @Param("auditRemark") String auditRemark,
                          @Param("auditUserId") Long auditUserId,
                          @Param("auditTime") LocalDateTime auditTime,
                          @Param("relatedOrderNo") String relatedOrderNo);
}
