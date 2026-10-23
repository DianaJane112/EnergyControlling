package com.competition.energycontrollerplatform.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import lombok.Data;

/**
 * 虚拟电表（总分表拓扑）分页查询请求。
 * <p>
 * 支持按项目、总表、子表、类型、状态等条件筛选，支持关键字搜索。
 */
@Data
@Schema(description = "虚拟电表拓扑关系分页查询请求")
public class ShadowMeterPageRequest {

    @Schema(description = "页码（从1开始）")
    @Min(value = 1, message = "页码最小为1")
    private Integer pageNum = 1;

    @Schema(description = "每页条数")
    @Min(value = 1, message = "每页条数最小为1")
    private Integer pageSize = 20;

    // ── 筛选条件 ──

    @Schema(description = "关键字搜索（模糊匹配总表名称/子表名称/关系描述）")
    private String keyword;

    @Schema(description = "项目ID")
    private Long projectId;

    @Schema(description = "总表ID（上级表计）")
    private Long parentMeterId;

    @Schema(description = "子表ID（下级表计）")
    private Long childMeterId;

    @Schema(description = "影子类型：AUTO-自动生成, MANUAL-手动创建")
    private String shadowType;

    @Schema(description = "状态：0-停用，1-启用")
    private Integer status;

    @Schema(description = "表计大类筛选：0=仅水表, 1=仅电表, null=全部")
    private Integer meterType;
}
