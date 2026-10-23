package com.competition.energycontrollerplatform.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 费率时段策略实体（复费率：尖峰平谷时段电价配置）。
 * <p>
 * 支持三种策略类型：
 * <ul>
 *   <li>1 - 固定单价：直接使用 {@code fixedPrice}</li>
 *   <li>2 - 分时电价：通过 {@code timeSlots} JSON 配置尖峰平谷各时段单价</li>
 *   <li>3 - 阶梯电价：通过 {@code timeSlots} JSON 配置各阶梯用量+单价</li>
 * </ul>
 */
@Data
@TableName("t_price_strategy")
public class PriceStrategy {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 策略名称 */
    @TableField("strategy_name")
    private String strategyName;

    /** 策略编码（唯一） */
    @TableField("strategy_code")
    private String strategyCode;

    /** Stable logical series shared by all historical price versions. */
    @TableField("version_group_code")
    private String versionGroupCode;

    @TableField("version_no")
    private Integer versionNo;

    /** 关联项目ID（NULL=全局策略） */
    @TableField("project_id")
    private Long projectId;

    /** Meter category: 0-water, 1-electric. */
    @TableField("meter_type")
    private Integer meterType;

    /** 策略类型：1-固定单价, 2-分时电价(尖峰平谷), 3-阶梯电价 */
    @TableField("price_type")
    private Integer priceType;

    /**
     * 时段电价 JSON。
     * <p>分时电价示例：
     * <pre>{@code
     * [{"slotName":"尖","startHour":19,"endHour":21,"price":1.20},
     *  {"slotName":"峰","startHour":8,"endHour":11,"price":1.00},
     *  {"slotName":"平","startHour":7,"endHour":8,"price":0.65},
     *  {"slotName":"谷","startHour":23,"endHour":7,"price":0.35}]
     * }</pre>
     */
    @TableField("time_slots")
    private String timeSlots;

    /** 固定单价（priceType=1时使用） */
    @TableField("fixed_price")
    private BigDecimal fixedPrice;

    /** 附加费（元/kWh） */
    @TableField("plus_price")
    private BigDecimal plusPrice;

    /** 生效日期 */
    @TableField("effective_date")
    private LocalDate effectiveDate;

    /** 失效日期（NULL=永久有效） */
    @TableField("expire_date")
    private LocalDate expireDate;

    /** 状态：0-停用, 1-启用 */
    private Integer status;

    /** 备注 */
    private String remark;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableField("create_by")
    private String createBy;

    @TableLogic
    @TableField("is_deleted")
    private Integer isDeleted;
}
