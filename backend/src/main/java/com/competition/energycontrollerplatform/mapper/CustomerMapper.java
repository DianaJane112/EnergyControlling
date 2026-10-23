package com.competition.energycontrollerplatform.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.competition.energycontrollerplatform.entity.Customer;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.math.BigDecimal;

@Mapper
public interface CustomerMapper extends BaseMapper<Customer> {

    @Select("SELECT * FROM t_customer WHERE id = #{id} LIMIT 1")
    Customer selectIncludingDeleted(@Param("id") Long id);

    /**
     * 原子增加余额：UPDATE t_customer SET balance = balance + #{amount} WHERE id = #{id} AND is_deleted = 0
     * @return 受影响行数，0 表示商户不存在或已注销
     */
    @Update("UPDATE t_customer SET balance = balance + #{amount}, update_time = NOW() WHERE id = #{id} AND is_deleted = 0")
    int addBalance(@Param("id") Long id, @Param("amount") BigDecimal amount);

    /**
     * 原子扣减余额（带余额校验）：UPDATE t_customer SET balance = balance - #{amount} WHERE id = #{id} AND balance >= #{amount} AND is_deleted = 0
     * @return 受影响行数，0 表示余额不足、商户不存在或已注销
     */
    @Update("UPDATE t_customer SET balance = balance - #{amount}, update_time = NOW() WHERE id = #{id} AND balance >= #{amount} AND is_deleted = 0")
    int subtractBalance(@Param("id") Long id, @Param("amount") BigDecimal amount);

    /**
     * 悲观锁行级锁定并返回商户当前数据（SELECT ... FOR UPDATE）。
     * <p>
     * 在资金操作（充值/退费/调账）中，先调用此方法锁定商户行，
     * 读取精确的 {@code balanceBefore}，再执行 {@link #addBalance} / {@link #subtractBalance}。
     * 仅在同一事务内有效，事务提交后锁自动释放。
     * </p>
     * <p>
     * <b>性能注意：</b>必须在事务中调用，且锁定后必须尽快完成后续操作。
     * 锁定期间其他事务对同一行的写操作将被阻塞。
     * </p>
     *
     * @param id 商户ID
     * @return 被锁定的商户实体，{@code null} 表示商户不存在或已删除
     */
    @Select("SELECT * FROM t_customer WHERE id = #{id} AND is_deleted = 0 FOR UPDATE")
    Customer selectByIdForUpdate(@Param("id") Long id);

    /** 全盘商户总资金池余额 */
    @Select("SELECT COALESCE(SUM(balance), 0) FROM t_customer WHERE is_deleted = 0")
    BigDecimal sumTotalBalance();

    /**
     * 逻辑删除商户（绕过 MyBatis-Plus @TableLogic 的 update_time 空值问题）。
     * <p>MyBatis-Plus 的 deleteById 在逻辑删除时不会触发 MetaObjectHandler，
     * 导致 update_time 被设为 NULL，而数据库该列有 NOT NULL 约束。</p>
     *
     * @param id 商户ID
     * @return 影响行数
     */
    @Update("UPDATE t_customer SET is_deleted = 1, update_time = NOW() WHERE id = #{id} AND is_deleted = 0")
    int logicDeleteById(@Param("id") Long id);
}
