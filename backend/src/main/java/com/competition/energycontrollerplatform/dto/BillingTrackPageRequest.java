package com.competition.energycontrollerplatform.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

/**
 * 缴费轨迹分页查询请求。
 */
@Data
@Schema(description = "缴费轨迹分页查询请求")
public class BillingTrackPageRequest {

    @Schema(description = "页码", example = "1", defaultValue = "1")
    private Integer pageNum;

    @Schema(description = "每页条数", example = "20", defaultValue = "20")
    private Integer pageSize;

    @Schema(description = "商户ID（ADMIN可指定，非ADMIN强制覆盖）")
    private Long customerId;

    @Schema(description = "表计ID")
    private Long meterId;

    @Schema(description = "交易类型：1-充值, 2-消费扣费, 3-退费, 4-调账, 5-人工缴费, 6-余额清零, 7-公共分摊")
    private Integer tradeType;

    @Schema(description = "开始时间")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;

    @Schema(description = "结束时间")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;

    @Schema(description = "关键字（流水号/操作人模糊搜索）")
    private String keyword;
}
