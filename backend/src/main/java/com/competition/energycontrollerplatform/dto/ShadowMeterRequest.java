package com.competition.energycontrollerplatform.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 虚拟电表（总分表拓扑）请求 DTO。
 */
@Data
@Schema(description = "虚拟电表拓扑关系请求")
public class ShadowMeterRequest {

    @Schema(description = "项目ID")
    private Long projectId;

    @Schema(description = "总表ID（上级表计）", example = "1")
    private Long parentMeterId;

    @Schema(description = "子表ID（下级表计）", example = "2")
    private Long childMeterId;

    /** Optional text project identifier for manual virtual-meter creation. */
    private String projectName;

    /** Manual meter identifiers. They are resolved to local meter records on create. */
    private String parentMeterNo;

    private String childMeterNo;

    /** 0=water, 1=electric. Required when manual table numbers are supplied. */
    private Integer meterType;

    /** 商户型虚拟表归属商户；未指定时使用当前登录商户。 */
    private Long customerId;

    /** EXISTING=bind an existing settlement account, CREATE=create one transactionally. */
    private String accountMode;

    /** Required when accountMode is CREATE. */
    private String accountName;

    private String accountPhone;

    private BigDecimal initialBalance;

    /** Optional mini-app users who may view this virtual meter. */
    private List<Long> viewerUserIds;

    @Schema(description = "影子类型：AUTO-自动生成, MANUAL-手动创建", example = "MANUAL")
    private String shadowType;

    @Schema(description = "系数（子表占总表比例），默认1.0", example = "1.0")
    private BigDecimal coefficient;

    @Schema(description = "关系描述", example = "A栋总表→101商铺分表")
    private String relationDesc;

    @Schema(description = "生效日期")
    private LocalDate effectiveDate;

    @Schema(description = "失效日期（NULL=永久有效）")
    private LocalDate expireDate;

    @Schema(description = "状态：0-停用，1-启用")
    private Integer status;

    // ── 子表(Meter)可编辑字段（编辑虚拟电表时同步修改子表信息） ──

    @Schema(description = "子表关联分组ID（外键→t_meter_group.id）")
    private Long groupId;

    @Schema(description = "是否清除子表的分组关联；用于区分未提交 groupId 与主动清空")
    private Boolean clearGroup;

    @Schema(description = "子表商户名（辰鳗name）")
    private String name;

    @Schema(description = "子表告警额度（元）")
    private java.math.BigDecimal warnline;

    @Schema(description = "子表透支额度（元）")
    private java.math.BigDecimal credit;

    @Schema(description = "子表付费方式：1-预付费, 2-后付费")
    private Integer feetype;

    @Schema(description = "子表CT变比")
    private java.math.BigDecimal ctRatio;

    @Schema(description = "子表PT变比")
    private java.math.BigDecimal ptRatio;

    @Schema(description = "子表标记状态：NORMAL/ATTENTION/ABNORMAL/MAINTENANCE")
    private String markStatus;

    @Schema(description = "子表通讯状态描述")
    private String commStatusDesc;

    @Schema(description = "子表是否停用警告：0-启用, 1-停用")
    private Integer disableWarning;

    @Schema(description = "子表安装地址")
    private String address;
}
