package com.competition.energycontrollerplatform.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import lombok.Data;

/**
 * 已删除电表/水表分页查询请求。
 * <p>用于"删除电表/水表"模块的列表查询与重置筛选。</p>
 */
@Data
@Schema(description = "已删除电表/水表分页查询请求")
public class DeletedMeterPageRequest {

    @Schema(description = "页码（从1开始）", example = "1")
    @Min(value = 1, message = "页码最小为1")
    private Integer pageNum = 1;

    @Schema(description = "每页条数", example = "20")
    @Min(value = 1, message = "每页条数最小为1")
    private Integer pageSize = 20;

    @Schema(description = "关键字搜索（模糊匹配：钢印号/表号/商户名）", example = "A栋101")
    private String keyword;

    @Schema(description = "表计大类：0=水表, 1=电表", example = "1")
    private Integer meterType;

    @Schema(description = "运行状态：0-停用, 1-正常运行, 2-欠费停机")
    private Integer status;

    @Schema(description = "分组ID")
    private Long groupId;

    @Schema(description = "删除开始时间（yyyy-MM-dd HH:mm:ss）", example = "2026-05-01 00:00:00")
    private String startTime;

    @Schema(description = "删除结束时间（yyyy-MM-dd HH:mm:ss）", example = "2026-05-31 23:59:59")
    private String endTime;
}
