package com.competition.energycontrollerplatform.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 操作日志响应 VO。
 */
@Data
@Schema(description = "操作日志条目")
public class OperationLogVO {

    @Schema(description = "日志ID")
    private Long id;

    @Schema(description = "操作员")
    private String operatorName;

    @Schema(description = "操作模块")
    private String moduleName;

    @Schema(description = "操作类型：QUERY-查询, CREATE-新增, UPDATE-修改, DELETE-删除, EXPORT-导出")
    private String operationType;

    @Schema(description = "操作描述")
    private String operationDesc;

    @Schema(description = "请求参数（JSON）")
    private String requestParams;

    @Schema(description = "响应结果（JSON）")
    private String responseResult;

    @Schema(description = "IP地址")
    private String ipAddress;

    @Schema(description = "操作时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private LocalDateTime operationTime;

    @Schema(description = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private LocalDateTime createTime;
}
