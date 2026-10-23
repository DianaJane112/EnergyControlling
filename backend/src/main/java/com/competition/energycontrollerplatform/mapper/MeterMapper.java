package com.competition.energycontrollerplatform.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.competition.energycontrollerplatform.entity.Meter;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Mapper
public interface MeterMapper extends BaseMapper<Meter> {

    /** 更新表计运行状态 */
    @Update("UPDATE t_meter SET status = #{status}, update_time = NOW() WHERE id = #{id} AND is_deleted = 0")
    int updateStatus(@Param("id") Long id, @Param("status") Integer status);

    /** 更新最近一次结算读数和时间 */
    @Update("UPDATE t_meter SET last_settled_reading = #{reading}, last_settled_time = #{time}, update_time = NOW() WHERE id = #{id} AND is_deleted = 0")
    int updateLastSettledReading(@Param("id") Long id,
                                 @Param("reading") BigDecimal reading,
                                 @Param("time") LocalDateTime time);

    /** 批量更新表计电价（按ID列表，可选按meterType过滤） */
    @Update("<script>"
            + "UPDATE t_meter SET price_type = #{priceType}, price_content = #{priceContent}, "
            + "plus_price = #{plusPrice}, rate_config_id = #{rateConfigId}, update_time = NOW() "
            + "WHERE is_deleted = 0 AND id IN "
            + "<foreach collection='ids' item='id' open='(' separator=',' close=')'>#{id}</foreach>"
            + "<if test='meterType != null'> AND meter_type = #{meterType} </if>"
            + "</script>")
    int batchUpdatePrice(@Param("ids") java.util.List<Long> ids,
                         @Param("priceType") Integer priceType,
                         @Param("priceContent") String priceContent,
                         @Param("plusPrice") BigDecimal plusPrice,
                         @Param("rateConfigId") Long rateConfigId,
                         @Param("meterType") Integer meterType);

    /** 按商户批量更新表计电价（可选按meterType过滤） */
    @Update("<script>"
            + "UPDATE t_meter SET price_type = #{priceType}, price_content = #{priceContent}, "
            + "plus_price = #{plusPrice}, rate_config_id = #{rateConfigId}, update_time = NOW() "
            + "WHERE customer_id = #{customerId} AND is_deleted = 0"
            + "<if test='meterType != null'> AND meter_type = #{meterType} </if>"
            + "</script>")
    int batchUpdatePriceByCustomer(@Param("customerId") Long customerId,
                                   @Param("priceType") Integer priceType,
                                   @Param("priceContent") String priceContent,
                                   @Param("plusPrice") BigDecimal plusPrice,
                                   @Param("rateConfigId") Long rateConfigId,
                                   @Param("meterType") Integer meterType);

    /** 全局更新所有表计电价（仅ADMIN，可选按meterType过滤） */
    @Update("<script>"
            + "UPDATE t_meter SET price_type = #{priceType}, price_content = #{priceContent}, "
            + "plus_price = #{plusPrice}, rate_config_id = #{rateConfigId}, update_time = NOW() "
            + "WHERE is_deleted = 0"
            + "<if test='meterType != null'> AND meter_type = #{meterType} </if>"
            + "</script>")
    int batchUpdatePriceGlobal(@Param("priceType") Integer priceType,
                               @Param("priceContent") String priceContent,
                               @Param("plusPrice") BigDecimal plusPrice,
                               @Param("rateConfigId") Long rateConfigId,
                               @Param("meterType") Integer meterType);

    /** 更新表计分合闸状态 */
    @Update("UPDATE t_meter SET status = #{status}, state_valve = #{stateValve}, update_time = NOW() WHERE id = #{id} AND is_deleted = 0")
    int updateValveState(@Param("id") Long id,
                         @Param("status") Integer status,
                         @Param("stateValve") Integer stateValve);

    /**
     * 悲观锁行级锁定并返回表计当前数据（SELECT ... FOR UPDATE）。
     * <p>
     * 在充值/退费后更新表计状态位（updateMeterStatusInDb）时使用，
     * 防止与管理员手动修改表计状态（如销户 status=3）之间产生 TOCTOU 竞态。
     * 仅在同一事务内有效，事务提交后锁自动释放。
     * </p>
     *
     * @param id 表计ID
     * @return 被锁定的表计实体，{@code null} 表示表计不存在或已删除
     */
    @Select("SELECT * FROM t_meter WHERE id = #{id} AND is_deleted = 0 FOR UPDATE")
    Meter selectByIdForUpdate(@Param("id") Long id);

    // ──────────────── 批量更新表计信息 ────────────────

    /**
     * 批量更新表计通用信息（名称、分组、类型、标记状态、告警开关等）。
     */
    @Update("<script>"
            + "UPDATE t_meter SET update_time = NOW() "
            + "<if test='name != null'>, name = #{name} </if>"
            + "<if test='groupId != null'>, group_id = #{groupId} </if>"
            + "<if test='bjlx != null'>, bjlx = #{bjlx} </if>"
            + "<if test='ctRatio != null'>, ct_ratio = #{ctRatio} </if>"
            + "<if test='ptRatio != null'>, pt_ratio = #{ptRatio} </if>"
            + "<if test='markStatus != null'>, mark_status = #{markStatus} </if>"
            + "<if test='disableWarning != null'>, disable_warning = #{disableWarning} </if>"
            + "<if test='commStatusDesc != null'>, comm_status_desc = #{commStatusDesc} </if>"
            + "<if test='status != null'>, status = #{status} </if>"
            + "<if test='address != null'>, address = #{address} </if>"
            + " WHERE is_deleted = 0 AND id IN "
            + "<foreach collection='ids' item='id' open='(' separator=',' close=')'>#{id}</foreach>"
            + "</script>")
    int batchUpdateMeterInfo(@Param("ids") java.util.List<Long> ids,
                             @Param("name") String name,
                             @Param("groupId") Long groupId,
                             @Param("bjlx") Integer bjlx,
                             @Param("ctRatio") java.math.BigDecimal ctRatio,
                             @Param("ptRatio") java.math.BigDecimal ptRatio,
                             @Param("markStatus") String markStatus,
                             @Param("disableWarning") Integer disableWarning,
                             @Param("commStatusDesc") String commStatusDesc,
                             @Param("status") Integer status,
                             @Param("address") String address);

    // ──────────────── 批量开关阀 ────────────────

    /** 按ID列表批量开关阀（可选按meterType过滤） */
    @Update("<script>"
            + "UPDATE t_meter SET state_valve = #{stateValve}, status = #{status}, update_time = NOW() "
            + "WHERE is_deleted = 0 AND id IN "
            + "<foreach collection='ids' item='id' open='(' separator=',' close=')'>#{id}</foreach>"
            + "<if test='meterType != null'> AND meter_type = #{meterType} </if>"
            + "</script>")
    int batchUpdateValveState(@Param("ids") java.util.List<Long> ids,
                              @Param("stateValve") Integer stateValve,
                              @Param("status") Integer status,
                              @Param("meterType") Integer meterType);

    /** 按商户批量开关阀（可选按meterType过滤） */
    @Update("<script>"
            + "UPDATE t_meter SET state_valve = #{stateValve}, status = #{status}, update_time = NOW() "
            + "WHERE customer_id = #{customerId} AND is_deleted = 0"
            + "<if test='meterType != null'> AND meter_type = #{meterType} </if>"
            + "</script>")
    int batchUpdateValveStateByCustomer(@Param("customerId") Long customerId,
                                        @Param("stateValve") Integer stateValve,
                                        @Param("status") Integer status,
                                        @Param("meterType") Integer meterType);

    /** 全局开关阀（仅ADMIN，可选按meterType过滤） */
    @Update("<script>"
            + "UPDATE t_meter SET state_valve = #{stateValve}, status = #{status}, update_time = NOW() "
            + "WHERE is_deleted = 0"
            + "<if test='meterType != null'> AND meter_type = #{meterType} </if>"
            + "</script>")
    int batchUpdateValveStateGlobal(@Param("stateValve") Integer stateValve,
                                    @Param("status") Integer status,
                                    @Param("meterType") Integer meterType);

    // ──────────────── 批量设置时段策略 ────────────────

    /** 按ID列表批量设置费率策略（可选按meterType过滤） */
    @Update("<script>"
            + "UPDATE t_meter SET rate_config_id = #{rateConfigId}, update_time = NOW() "
            + "WHERE is_deleted = 0 AND id IN "
            + "<foreach collection='ids' item='id' open='(' separator=',' close=')'>#{id}</foreach>"
            + "<if test='meterType != null'> AND meter_type = #{meterType} </if>"
            + "</script>")
    int batchSetRateConfig(@Param("ids") java.util.List<Long> ids,
                           @Param("rateConfigId") Long rateConfigId,
                           @Param("meterType") Integer meterType);

    /** 按商户批量设置费率策略（可选按meterType过滤） */
    @Update("<script>"
            + "UPDATE t_meter SET rate_config_id = #{rateConfigId}, update_time = NOW() "
            + "WHERE customer_id = #{customerId} AND is_deleted = 0"
            + "<if test='meterType != null'> AND meter_type = #{meterType} </if>"
            + "</script>")
    int batchSetRateConfigByCustomer(@Param("customerId") Long customerId,
                                     @Param("rateConfigId") Long rateConfigId,
                                     @Param("meterType") Integer meterType);

    /** 全局设置费率策略（仅ADMIN，可选按meterType过滤） */
    @Update("<script>"
            + "UPDATE t_meter SET rate_config_id = #{rateConfigId}, update_time = NOW() "
            + "WHERE is_deleted = 0"
            + "<if test='meterType != null'> AND meter_type = #{meterType} </if>"
            + "</script>")
    int batchSetRateConfigGlobal(@Param("rateConfigId") Long rateConfigId,
                                 @Param("meterType") Integer meterType);

    // ──────────────── 逻辑删除穿透查询与恢复 ────────────────

    /**
     * 查询已删除电表列表（绕过 @TableLogic 全局过滤），支持多条件筛选。
     * <p>显式声明 is_deleted = 1，同时强制注入 customer_id 做权限隔离。</p>
     *
     * @param customerId 商户ID（NULL=ADMIN查全量）
     * @param keyword    关键字（模糊匹配 steal_no / db_no / name）
     * @param meterType  表计大类：0=水表, 1=电表
     * @param status     运行状态
     * @param groupId    分组ID
     * @param startTime  删除时间起
     * @param endTime    删除时间止
     * @return 已删除电表列表
     */
    @Select("<script>"
            + "SELECT * FROM t_meter WHERE is_deleted = 1 "
            + "<if test='customerId != null'> AND customer_id = #{customerId} </if>"
            + "<if test='keyword != null and keyword != \"\"'>"
            + " AND (steal_no LIKE CONCAT('%', #{keyword}, '%') "
            + "   OR db_no LIKE CONCAT('%', #{keyword}, '%') "
            + "   OR name LIKE CONCAT('%', #{keyword}, '%') "
            + "   OR meteraddress LIKE CONCAT('%', #{keyword}, '%'))"
            + "</if>"
            + "<if test='meterType != null'> AND meter_type = #{meterType} </if>"
            + "<if test='status != null'> AND status = #{status} </if>"
            + "<if test='groupId != null'> AND group_id = #{groupId} </if>"
            + "<if test='startTime != null and startTime != \"\"'> AND update_time &gt;= #{startTime} </if>"
            + "<if test='endTime != null and endTime != \"\"'> AND update_time &lt;= #{endTime} </if>"
            + "ORDER BY update_time DESC"
            + "</script>")
    java.util.List<com.competition.energycontrollerplatform.entity.Meter> selectDeletedList(
            @Param("customerId") Long customerId,
            @Param("keyword") String keyword,
            @Param("meterType") Integer meterType,
            @Param("status") Integer status,
            @Param("groupId") Long groupId,
            @Param("startTime") String startTime,
            @Param("endTime") String endTime);

    /**
     * 按 ID 查询电表（含已删除），绕过 @TableLogic 全局过滤。
     * <p>用于已删除电表详情查看，BaseMapper.selectById 会被 @TableLogic 注入 is_deleted=0 导致查不到。</p>
     *
     * @param id 电表ID
     * @return 电表实体（含已删除），未找到返回 null
     */
    @Select("SELECT * FROM t_meter WHERE id = #{id}")
    com.competition.energycontrollerplatform.entity.Meter selectByIdIncludingDeleted(@Param("id") Long id);

    /**
     * 逻辑删除表计（绕过 MyBatis-Plus @TableLogic 的 update_time 空值问题）。
     * <p>MyBatis-Plus 的 deleteById 在逻辑删除时不会触发 MetaObjectHandler，
     * 导致 update_time 被设为 NULL，而数据库该列有 NOT NULL 约束。
     * 此方法直接执行原生 UPDATE，确保 update_time 被正确刷新。</p>
     *
     * @param id 表计ID
     * @return 影响行数
     */
    @Update("UPDATE t_meter SET is_deleted = 1, update_time = NOW() WHERE id = #{id} AND is_deleted = 0")
    int logicDeleteById(@Param("id") Long id);

    /**
     * 恢复已删除电表（原子更新 is_deleted = 0）。
     * <p>绕过 MyBatis-Plus 的 @TableLogic 拦截，直接执行原生 UPDATE。</p>
     *
     * @param id 电表ID
     * @return 影响行数
     */
    @Update("UPDATE t_meter SET is_deleted = 0, update_time = NOW() WHERE id = #{id} AND is_deleted = 1")
    int restoreById(@Param("id") Long id);

    /**
     * 从捷先 getMeterDate 接口同步水表基本信息。
     * <p>更新口径、阀控能力、阀门状态、信号强度、电池电压等元数据字段。</p>
     */
    @Update("UPDATE t_meter SET meter_caliber = #{meterCaliber}, controlling = #{controlling}, "
            + "state_valve = #{stateValve}, signal_strength = #{signalStrength}, "
            + "battery_voltage = #{batteryVoltage}, update_time = NOW() WHERE id = #{id} AND is_deleted = 0")
    int updateJiexianMeterInfo(@Param("id") Long id,
                               @Param("meterCaliber") String meterCaliber,
                               @Param("controlling") Integer controlling,
                               @Param("stateValve") Integer stateValve,
                               @Param("signalStrength") Integer signalStrength,
                               @Param("batteryVoltage") java.math.BigDecimal batteryVoltage);

    /**
     * 批量恢复已删除电表/水表。
     *
     * @param ids 电表ID列表
     * @return 影响行数
     */
    @Update("<script>"
            + "UPDATE t_meter SET is_deleted = 0, update_time = NOW() "
            + "WHERE is_deleted = 1 AND id IN "
            + "<foreach collection='ids' item='id' open='(' separator=',' close=')'>#{id}</foreach>"
            + "</script>")
    int batchRestoreByIds(@Param("ids") java.util.List<Long> ids);

    // ──────────────── 编号生成辅助 ────────────────

    /**
     * 获取指定前缀下的最大序号（提取 db_no 中前缀后的数字部分）。
     * <p>
     * 用于 {@link com.competition.energycontrollerplatform.common.DbNoGenerator}
     * 计算下一个编号序列号。
     * </p>
     *
     * @param prefix 编号前缀（如 "01" / "ELEC"）
     * @return 当前最大序号，无匹配记录时返回 0
     */
    @Select("SELECT COALESCE(MAX(CAST(SUBSTRING(db_no, LENGTH(#{prefix}) + 1) AS UNSIGNED)), 0) "
            + "FROM t_meter "
            + "WHERE is_deleted = 0 "
            + "  AND db_no LIKE CONCAT(#{prefix}, '%') "
            + "  AND db_no REGEXP CONCAT('^', #{prefix}, '[0-9]+$')")
    long getMaxSeqByPrefix(@Param("prefix") String prefix);
}
