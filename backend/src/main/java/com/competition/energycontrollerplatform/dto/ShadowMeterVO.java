package com.competition.energycontrollerplatform.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 虚拟电表拓扑关系响应 VO。
 */
@Data
@Schema(description = "虚拟电表拓扑关系")
public class ShadowMeterVO {

    @Schema(description = "关系ID")
    private Long id;

    @Schema(description = "项目ID")
    private Long projectId;

    @Schema(description = "项目名称")
    private String projectName;

    @Schema(description = "总表ID")
    private Long parentMeterId;

    @Schema(description = "总表钢印号/表号")
    private String parentMeterNo;

    @Schema(description = "总表名称")
    private String parentMeterName;

    @Schema(description = "总表表计大类：0=水表, 1=电表")
    private Integer parentMeterType;

    @Schema(description = "子表ID")
    private Long childMeterId;

    @Schema(description = "子表钢印号/表号")
    private String childMeterNo;

    @Schema(description = "子表名称")
    private String childMeterName;

    @Schema(description = "子表表计大类：0=水表, 1=电表")
    private Integer childMeterType;

    @Schema(description = "子表关联分组ID")
    private Long childGroupId;

    private String childGroupName;

    private String childGroupPath;

    private Integer childGroupStatus;

    private String childGroupRelationStatus;

    @Schema(description = "子表关联商户ID")
    private Long childCustomerId;

    @Schema(description = "子表告警额度（元）")
    private java.math.BigDecimal childWarnline;

    @Schema(description = "子表透支额度（元）")
    private java.math.BigDecimal childCredit;

    @Schema(description = "子表付费方式：1-预付费, 2-后付费")
    private Integer childFeetype;

    @Schema(description = "子表CT变比")
    private java.math.BigDecimal childCtRatio;

    @Schema(description = "子表PT变比")
    private java.math.BigDecimal childPtRatio;

    @Schema(description = "子表标记状态")
    private String childMarkStatus;

    @Schema(description = "子表通讯状态描述")
    private String childCommStatusDesc;

    @Schema(description = "子表是否停用警告")
    private Integer childDisableWarning;

    @Schema(description = "子表运行状态：0-停用, 1-正常运行, 2-欠费停机")
    private Integer childStatus;

    @Schema(description = "子表在线状态：0-离线, 1-在线")
    private Integer childOnline;

    @Schema(description = "影子类型：AUTO-自动生成, MANUAL-手动创建")
    private String shadowType;

    @Schema(description = "系数")
    private BigDecimal coefficient;

    @Schema(description = "关系描述")
    private String relationDesc;

    @Schema(description = "生效日期")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate effectiveDate;

    @Schema(description = "失效日期")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate expireDate;

    @Schema(description = "状态：0-停用，1-启用")
    private Integer status;

    @Schema(description = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private LocalDateTime createTime;

    @Schema(description = "创建人")
    private String createBy;

    @Schema(description = "关联的真实电表数量")
    private Long realMeterCount;
}
