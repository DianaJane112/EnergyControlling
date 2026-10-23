package com.competition.energycontrollerplatform.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.competition.energycontrollerplatform.entity.ValveControl;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface ValveControlMapper extends BaseMapper<ValveControl> {

    /** 查询所有待轮询的阀控任务（task_status = 'PENDING'） */
    @Select("SELECT * FROM t_valve_control WHERE task_status = 'PENDING' "
            + "AND is_deleted = 0 ORDER BY create_time ASC")
    List<ValveControl> selectPendingTasks();

    /** 更新阀控任务执行状态 */
    @Update("UPDATE t_valve_control SET task_status = #{taskStatus}, "
            + "returnvalue = #{returnValue}, msg = #{msg}, "
            + "update_time = NOW() WHERE batchid = #{batchid}")
    int updateTaskStatus(@Param("batchid") String batchid,
                         @Param("taskStatus") String taskStatus,
                         @Param("returnValue") String returnValue,
                         @Param("msg") String msg);

    /** 将超时的 PENDING 任务标记为 TIMEOUT */
    @Update("UPDATE t_valve_control SET task_status = 'TIMEOUT', "
            + "msg = '轮询超时：超过最大等待时间未收到确认', "
            + "update_time = NOW() WHERE task_status = 'PENDING' "
            + "AND create_time < DATE_SUB(NOW(), INTERVAL #{timeoutMinutes} MINUTE)")
    int markTimeoutTasks(@Param("timeoutMinutes") int timeoutMinutes);
}
