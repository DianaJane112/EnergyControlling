package com.competition.energycontrollerplatform.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.competition.energycontrollerplatform.entity.RefundLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;

/**
 * 人工退费日志 Mapper。
 */
@Mapper
public interface RefundLogMapper extends BaseMapper<RefundLog> {

    /**
     * 统计指定商户今日退费总额。
     */
    @Select("SELECT COALESCE(SUM(refund_amount), 0) FROM t_refund_log "
            + "WHERE customer_id = #{customerId} AND DATE(operate_time) = CURDATE() AND is_deleted = 0")
    BigDecimal sumTodayRefundByCustomer(@Param("customerId") Long customerId);

    /**
     * 统计全平台今日退费总额。
     */
    @Select("SELECT COALESCE(SUM(refund_amount), 0) FROM t_refund_log "
            + "WHERE DATE(operate_time) = CURDATE() AND is_deleted = 0")
    BigDecimal sumTodayTotalRefund();
}
