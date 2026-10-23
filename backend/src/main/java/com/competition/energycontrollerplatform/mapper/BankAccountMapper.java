package com.competition.energycontrollerplatform.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.competition.energycontrollerplatform.entity.BankAccount;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 银行账户 Mapper。
 */
@Mapper
public interface BankAccountMapper extends BaseMapper<BankAccount> {

    /**
     * 逻辑删除银行账户（绕过 MyBatis-Plus @TableLogic 的 update_time 空值问题）。
     */
    @Update("UPDATE t_bank_account SET is_deleted = 1, update_time = NOW() WHERE id = #{id} AND is_deleted = 0")
    int logicDeleteById(@Param("id") Long id);
}
