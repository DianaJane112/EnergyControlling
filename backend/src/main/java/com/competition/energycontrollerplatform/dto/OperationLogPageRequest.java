package com.competition.energycontrollerplatform.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 操作日志分页查询请求 DTO。
 * <p>
 * 支持按操作员、操作模块、操作类型、时间范围进行高级检索。
 */
@Data
@Schema(description = "操作日志分页查询请求")
public class OperationLogPageRequest {

    @Schema(description = "页码", example = "1", defaultValue = "1")
    private Integer pageNum;

    @Schema(description = "每页条数", example = "20", defaultValue = "20")
    private Integer pageSize;

    @Schema(description = "操作员（模糊匹配）", example = "admin")
    private String operatorName;

    @Schema(description = "操作模块（精确匹配）", example = "表计管理")
    private String moduleName;

    @Schema(description = "操作类型：QUERY-查询, CREATE-新增, UPDATE-修改, DELETE-删除, EXPORT-导出", example = "CREATE")
    private String operationType;

    @Schema(description = "操作描述关键字（模糊匹配）")
    private String keyword;

    @Schema(description = "开始时间（yyyy-MM-dd HH:mm:ss）", example = "2026-05-01 00:00:00")
    private String startTime;

    @Schema(description = "结束时间（yyyy-MM-dd HH:mm:ss）", example = "2026-05-31 23:59:59")
    private String endTime;

    @Schema(description = "表计ID（筛选该表计相关操作记录）", example = "1")
    private Long meterId;
}
