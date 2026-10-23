package com.competition.energycontrollerplatform.dto;

import jakarta.validation.constraints.Min;
import lombok.Data;

/**
 * 表计分页查询请求
 */
@Data
public class MeterPageRequest {

    /** 页码（从1开始） */
    @Min(value = 1, message = "页码最小为1")
    private Integer pageNum = 1;

    /** 每页条数 */
    @Min(value = 1, message = "每页条数最小为1")
    private Integer pageSize = 10;

    // ── 筛选条件 ──

    /** 表钢印号（积成） */
    private String stealNo;

    /** 电表表号（辰鳗） */
    private String dbNo;

    /** 水表编号（捷先） */
    private String meteraddress;

    /** 商户名 */
    private String name;

    /** 所属客户ID */
    private Long customerId;

    /** 所属项目ID */
    private Long projectId;

    /** 生产厂家 */
    private String vender;

    /** 运行状态 0/1 */
    private Integer status;

    /** 在线状态 0/1 */
    private Integer online;

    /** 表计类型 1租户/2总表/3公共/4其他（辰鳗bjlx） */
    private Integer bjlx;

    /** 表计大类筛选：0=水表, 1=电表 */
    private Integer meterType;

    /** 表计性质筛选：PHYSICAL=真实表，VIRTUAL=虚拟结算表 */
    private String meterKind;

    /** 终端名称/集中器名称 */
    private String terminalName;

    /** 标记状态筛选：NORMAL/ATTENTION/ABNORMAL/MAINTENANCE */
    private String markStatus;

    /** 通讯状态描述筛选 */
    private String commStatusDesc;

    /** 是否停用警告：0-否, 1-是 */
    private Integer disableWarning;

    /** 分组ID筛选 */
    private Long groupId;
}
