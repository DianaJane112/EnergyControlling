package com.competition.energycontrollerplatform.controller;

import com.alibaba.excel.EasyExcel;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.competition.energycontrollerplatform.common.BusinessException;
import com.competition.energycontrollerplatform.common.Result;
import com.competition.energycontrollerplatform.common.annotation.BusinessType;
import com.competition.energycontrollerplatform.common.annotation.Log;
import com.competition.energycontrollerplatform.dto.AccountFlowExcelVO;
import com.competition.energycontrollerplatform.dto.AccountFlowPageRequest;
import com.competition.energycontrollerplatform.dto.AccountFlowSummaryExcelVO;
import com.competition.energycontrollerplatform.dto.AccountFlowSummaryVO;
import com.competition.energycontrollerplatform.dto.AccountFlowVO;
import com.competition.energycontrollerplatform.entity.AccountFlow;
import com.competition.energycontrollerplatform.entity.Customer;
import com.competition.energycontrollerplatform.mapper.AccountFlowMapper;
import com.competition.energycontrollerplatform.mapper.CustomerMapper;
import com.competition.energycontrollerplatform.security.SecurityUserContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 分账明细（资金流水）查询与导出 Controller。
 * <p>
 * 提供分账明细分页查询及 Excel 报表导出功能。
 * 权限隔离：ADMIN 可查看全局流水，非 ADMIN 仅可查看自身关联商户的流水。
 */
@Tag(name = "财务管理 - 分账明细", description = "分账明细（资金流水）查询、Excel 报表导出接口")
@RestController
@RequestMapping("/api/v1/account-flow")
@RequiredArgsConstructor
public class AccountFlowController {

    private static final Logger log = LoggerFactory.getLogger(AccountFlowController.class);

    private final AccountFlowMapper accountFlowMapper;
    private final CustomerMapper customerMapper;

    private static final DateTimeFormatter FILE_DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
    private static final DateTimeFormatter QUERY_DTF = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // ──────────────────────── 分账统计聚合查询 ────────────────────────

    @Tag(name = "财务管理 - 分账统计")
    @Operation(summary = "分账统计聚合查询", description = "按日/月/年维度聚合分账数据，返回充值、扣费、退费、调账、分摊、净流水、笔数。"
            + "支持按时段、按商户筛选。非 ADMIN 角色仅可查看自身关联商户的统计数据。")
    @GetMapping("/summary")
    public Result<List<AccountFlowSummaryVO>> summary(
            @Parameter(description = "聚合粒度：daily/monthly/yearly，默认 daily", example = "monthly")
            @RequestParam(defaultValue = "daily") String granularity,
            @Parameter(description = "开始日期（含）", example = "2026-05-01 00:00:00")
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @Parameter(description = "结束日期（含）", example = "2026-05-31 23:59:59")
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime,
            @Parameter(description = "商户ID（仅ADMIN可指定）")
            @RequestParam(required = false) Long customerId) {

        // ── 权限隔离 ──
        boolean isAdmin = SecurityUserContext.isAdmin();
        Long effectiveCustomerId;
        if (isAdmin) {
            effectiveCustomerId = customerId;
        } else {
            effectiveCustomerId = SecurityUserContext.getCurrentCustomerId();
            if (effectiveCustomerId == null) {
                throw BusinessException.of(403, "无权限查看分账统计：当前用户未关联商户");
            }
        }

        String startStr = startTime != null ? startTime.format(QUERY_DTF) : null;
        String endStr = endTime != null ? endTime.format(QUERY_DTF) : null;

        List<Map<String, Object>> rows;
        switch (granularity) {
            case "monthly" -> rows = accountFlowMapper.sumFlowByMonth(startStr, endStr, effectiveCustomerId);
            case "yearly"  -> rows = accountFlowMapper.sumFlowByYear(startStr, endStr, effectiveCustomerId);
            default        -> rows = accountFlowMapper.sumFlowByDate(startStr, endStr, effectiveCustomerId);
        }

        List<AccountFlowSummaryVO> result = new ArrayList<>(rows.size());
        for (Map<String, Object> row : rows) {
            AccountFlowSummaryVO vo = new AccountFlowSummaryVO();
            vo.setPeriod(toStringValue(row.get("period")));
            vo.setTotalRecharge(toBigDecimal(row.get("totalRecharge")));
            vo.setTotalDebit(toBigDecimal(row.get("totalDebit")));
            vo.setTotalRefund(toBigDecimal(row.get("totalRefund")));
            vo.setTotalAdjust(toBigDecimal(row.get("totalAdjust")));
            vo.setTotalAllocation(toBigDecimal(row.get("totalAllocation")));
            vo.setNetFlow(toBigDecimal(row.get("netFlow")));
            vo.setFlowCount(toLong(row.get("flowCount")));
            result.add(vo);
        }

        log.info("[分账统计] 操作人={} isAdmin={} customerId={} granularity={} rows={}",
                SecurityUserContext.getCurrentUsername(), isAdmin, effectiveCustomerId, granularity, result.size());

        return Result.success(result);
    }

    // ──────────────────────── 分账明细分页查询 ────────────────────────

    @Operation(summary = "分账明细分页查询", description = "支持按时段、按商户、按交易类型过滤。"
            + "非 ADMIN 角色仅可查看自身关联商户的流水。")
    @GetMapping("/list")
    public Result<IPage<AccountFlowVO>> list(@Valid AccountFlowPageRequest request) {

        boolean isAdmin = SecurityUserContext.isAdmin();
        Long effectiveCustomerId;
        if (isAdmin) {
            effectiveCustomerId = request.getCustomerId();
        } else {
            effectiveCustomerId = SecurityUserContext.getCurrentCustomerId();
            if (effectiveCustomerId == null) {
                throw BusinessException.of(403, "无权限查看分账明细：当前用户未关联商户");
            }
        }

        // ── 构建查询条件 ──
        LambdaQueryWrapper<AccountFlow> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AccountFlow::getIsDeleted, 0);

        if (effectiveCustomerId != null) {
            wrapper.eq(AccountFlow::getCustomerId, effectiveCustomerId);
        }
        if (request.getTradeType() != null) {
            wrapper.eq(AccountFlow::getTradeType, request.getTradeType());
        }
        if (StringUtils.hasText(request.getStartTime())) {
            wrapper.ge(AccountFlow::getOperateTime, parseDateTime(request.getStartTime()));
        }
        if (StringUtils.hasText(request.getEndTime())) {
            wrapper.le(AccountFlow::getOperateTime, parseDateTime(request.getEndTime()));
        }
        if (StringUtils.hasText(request.getKeyword())) {
            wrapper.and(w -> w
                    .like(AccountFlow::getFlowNo, request.getKeyword())
                    .or()
                    .like(AccountFlow::getBizNo, request.getKeyword()));
        }
        wrapper.orderByDesc(AccountFlow::getOperateTime);

        // ── 分页查询 ──
        Page<AccountFlow> page = new Page<>(request.getPageNum(), request.getPageSize());
        IPage<AccountFlow> flowPage = accountFlowMapper.selectPage(page, wrapper);

        Map<Long, Customer> customerMap = buildCustomerMap(flowPage.getRecords());
        return Result.success(flowPage.convert(f -> toAccountFlowVO(f, customerMap)));
    }

    // ──────────────────────── Excel 导出 ────────────────────────

    @Operation(summary = "导出分账明细 Excel 报表",
            description = "根据时间范围和商户 ID 筛选导出资金流水为 .xlsx 文件。" +
                    "ADMIN 不传 customerId 导出全局，非 ADMIN 忽略前端传入 customerId 仅导出自身。")
    @Log(title = "分账明细", businessType = BusinessType.EXPORT)
    @GetMapping("/export")
    public void export(
            @Parameter(description = "流水开始时间（含）", example = "2026-05-01 00:00:00")
            @RequestParam(required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,

            @Parameter(description = "流水结束时间（含）", example = "2026-05-31 23:59:59")
            @RequestParam(required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime,

            @Parameter(description = "商户 ID（仅 ADMIN 可指定）")
            @RequestParam(required = false) Long customerId,

            HttpServletResponse response) throws IOException {

        // ── 权限隔离 ──
        boolean isAdmin = SecurityUserContext.isAdmin();
        Long effectiveCustomerId;
        if (isAdmin) {
            effectiveCustomerId = customerId; // ADMIN 可指定任意商户
        } else {
            effectiveCustomerId = SecurityUserContext.getCurrentCustomerId();
            if (effectiveCustomerId == null) {
                throw BusinessException.of(403, "无权限导出流水：当前用户未关联商户");
            }
        }

        // ── 构建查询条件 ──
        LambdaQueryWrapper<AccountFlow> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AccountFlow::getIsDeleted, 0);

        if (effectiveCustomerId != null) {
            wrapper.eq(AccountFlow::getCustomerId, effectiveCustomerId);
        }
        if (startTime != null) {
            wrapper.ge(AccountFlow::getOperateTime, startTime);
        }
        if (endTime != null) {
            wrapper.le(AccountFlow::getOperateTime, endTime);
        }
        wrapper.orderByDesc(AccountFlow::getOperateTime);

        // ── 查询数据 ──
        List<AccountFlow> flows = accountFlowMapper.selectList(wrapper);
        List<AccountFlowExcelVO> excelData = new ArrayList<>(flows.size());
        for (AccountFlow f : flows) {
            excelData.add(toExcelVO(f));
        }

        log.info("[分账明细导出] 操作人={} isAdmin={} customerId={} 行数={}",
                SecurityUserContext.getCurrentUsername(), isAdmin, effectiveCustomerId, excelData.size());

        // ── 写出 Excel ──
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());

        String fileName = "分账明细_" + LocalDateTime.now().format(FILE_DATE_FMT);
        String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8)
                .replaceAll("\\+", "%20");
        response.setHeader("Content-Disposition",
                "attachment; filename=" + encodedFileName + ".xlsx");
        // 允许前端 JS 读取文件名
        response.setHeader("Access-Control-Expose-Headers", "Content-Disposition");

        EasyExcel.write(response.getOutputStream(), AccountFlowExcelVO.class)
                .sheet("分账明细")
                .doWrite(excelData);
    }

    // ──────────────────────── 分账统计汇总导出 ────────────────────────

    @Tag(name = "财务管理 - 分账统计")
    @Operation(summary = "导出分账统计汇总 Excel 报表",
            description = "导出分账统计页面（按日/月/年聚合）的汇总数据为 .xlsx 文件。" +
                    "导出字段：统计时段、充值总额、扣费总额、退费总额、调账总额、分摊总额、净流水、笔数。" +
                    "权限隔离：ADMIN 可导出全局/指定商户，非 ADMIN 仅可导出自身关联商户。")
    @Log(title = "分账统计", businessType = BusinessType.EXPORT)
    @GetMapping("/export-summary")
    public void exportSummary(
            @Parameter(description = "聚合粒度：daily/monthly/yearly，默认 daily", example = "monthly")
            @RequestParam(defaultValue = "daily") String granularity,
            @Parameter(description = "开始日期（含）", example = "2026-05-01 00:00:00")
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @Parameter(description = "结束日期（含）", example = "2026-05-31 23:59:59")
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime,
            @Parameter(description = "商户ID（仅ADMIN可指定）")
            @RequestParam(required = false) Long customerId,
            HttpServletResponse response) throws IOException {

        // ── 权限隔离 ──
        boolean isAdmin = SecurityUserContext.isAdmin();
        Long effectiveCustomerId;
        if (isAdmin) {
            effectiveCustomerId = customerId;
        } else {
            effectiveCustomerId = SecurityUserContext.getCurrentCustomerId();
            if (effectiveCustomerId == null) {
                throw BusinessException.of(403, "无权限导出分账统计：当前用户未关联商户");
            }
        }

        String startStr = startTime != null ? startTime.format(QUERY_DTF) : null;
        String endStr = endTime != null ? endTime.format(QUERY_DTF) : null;

        // ── 与 summary 接口完全一致的聚合查询 ──
        List<Map<String, Object>> rows;
        switch (granularity) {
            case "monthly" -> rows = accountFlowMapper.sumFlowByMonth(startStr, endStr, effectiveCustomerId);
            case "yearly"  -> rows = accountFlowMapper.sumFlowByYear(startStr, endStr, effectiveCustomerId);
            default        -> rows = accountFlowMapper.sumFlowByDate(startStr, endStr, effectiveCustomerId);
        }

        // ── 转换为 Excel VO ──
        List<AccountFlowSummaryExcelVO> excelData = new ArrayList<>(rows.size());
        for (Map<String, Object> row : rows) {
            AccountFlowSummaryExcelVO vo = new AccountFlowSummaryExcelVO();
            vo.setPeriod(toStringValue(row.get("period")));
            vo.setTotalRecharge(toBigDecimal(row.get("totalRecharge")));
            vo.setTotalDebit(toBigDecimal(row.get("totalDebit")));
            vo.setTotalRefund(toBigDecimal(row.get("totalRefund")));
            vo.setTotalAdjust(toBigDecimal(row.get("totalAdjust")));
            vo.setTotalAllocation(toBigDecimal(row.get("totalAllocation")));
            vo.setNetFlow(toBigDecimal(row.get("netFlow")));
            vo.setFlowCount(toLong(row.get("flowCount")));
            excelData.add(vo);
        }

        log.info("[分账统计导出] 操作人={} isAdmin={} customerId={} granularity={} 行数={}",
                SecurityUserContext.getCurrentUsername(), isAdmin, effectiveCustomerId, granularity, excelData.size());

        // ── 确定文件名中的粒度标签 ──
        String granularityLabel = switch (granularity) {
            case "monthly" -> "月报";
            case "yearly"  -> "年报";
            default        -> "日报";
        };

        // ── 写出 Excel ──
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());

        String fileName = "分账统计汇总_" + granularityLabel + "_" + LocalDateTime.now().format(FILE_DATE_FMT);
        String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8)
                .replaceAll("\\+", "%20");
        response.setHeader("Content-Disposition",
                "attachment; filename=" + encodedFileName + ".xlsx");
        response.setHeader("Access-Control-Expose-Headers", "Content-Disposition");

        EasyExcel.write(response.getOutputStream(), AccountFlowSummaryExcelVO.class)
                .sheet("分账统计汇总")
                .doWrite(excelData);
    }

    // ──────── 模型转换 ────────

    /**
     * AccountFlow → AccountFlowVO（分页列表展示）。
     */
    private AccountFlowVO toAccountFlowVO(AccountFlow f) {
        Map<Long, Customer> cm = Map.of();
        if (f.getCustomerId() != null) {
            Customer c = customerMapper.selectById(f.getCustomerId());
            if (c != null) cm = Map.of(c.getId(), c);
        }
        return toAccountFlowVO(f, cm);
    }

    private AccountFlowVO toAccountFlowVO(AccountFlow f, Map<Long, Customer> customerMap) {
        AccountFlowVO vo = new AccountFlowVO();
        vo.setId(f.getId());
        vo.setFlowNo(f.getFlowNo());
        vo.setCustomerId(f.getCustomerId());
        vo.setMeterId(f.getMeterId());
        vo.setBalanceBefore(f.getBalanceBefore());
        vo.setChangeAmount(f.getChangeAmount());
        vo.setBalanceAfter(f.getBalanceAfter());
        vo.setTradeType(f.getTradeType());
        vo.setTradeTypeLabel(tradeTypeLabel(f.getTradeType()));
        vo.setBizNo(f.getBizNo());
        vo.setOperatorName(f.getOperatorName());
        vo.setRemark(f.getRemark());
        vo.setOperateTime(f.getOperateTime());

        if (f.getCustomerId() != null) {
            Customer c = customerMap.get(f.getCustomerId());
            if (c != null) vo.setCustomerName(c.getCustomerName());
        }
        return vo;
    }

    private Map<Long, Customer> buildCustomerMap(List<AccountFlow> flows) {
        Set<Long> ids = flows.stream().map(AccountFlow::getCustomerId)
                .filter(id -> id != null).collect(java.util.stream.Collectors.toSet());
        if (ids.isEmpty()) return Map.of();
        return customerMapper.selectBatchIds(ids).stream()
                .collect(java.util.stream.Collectors.toMap(Customer::getId, c -> c, (a, b) -> a));
    }

    private AccountFlowExcelVO toExcelVO(AccountFlow f) {
        AccountFlowExcelVO vo = new AccountFlowExcelVO();
        vo.setFlowNo(f.getFlowNo());
        vo.setCustomerId(f.getCustomerId());
        vo.setBalanceBefore(f.getBalanceBefore());
        vo.setChangeAmount(f.getChangeAmount());
        vo.setBalanceAfter(f.getBalanceAfter());
        vo.setTradeType(f.getTradeType());
        vo.setTradeTypeLabel(tradeTypeLabel(f.getTradeType()));
        vo.setBizNo(f.getBizNo());
        vo.setOperatorName(f.getOperatorName());
        vo.setRemark(f.getRemark());
        vo.setOperateTime(f.getOperateTime());
        return vo;
    }

    private String tradeTypeLabel(Integer tradeType) {
        if (tradeType == null) return "未知";
        return switch (tradeType) {
            case 1  -> "充值";
            case 2  -> "消费扣费";
            case 3  -> "退费";
            case 4  -> "调账";
            case 5  -> "人工缴费";
            case 6  -> "余额清零";
            case 7  -> "公共分摊";
            default -> "其他(" + tradeType + ")";
        };
    }

    private BigDecimal toBigDecimal(Object value) {
        if (value == null) return BigDecimal.ZERO;
        if (value instanceof BigDecimal bd) return bd;
        if (value instanceof Double d) return BigDecimal.valueOf(d);
        if (value instanceof Number n) return BigDecimal.valueOf(n.doubleValue());
        try {
            return new BigDecimal(value.toString());
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }

    private Long toLong(Object value) {
        if (value == null) return 0L;
        if (value instanceof Long l) return l;
        if (value instanceof Number n) return n.longValue();
        try {
            return Long.parseLong(value.toString());
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    /**
     * 安全地将数据库返回值转为字符串。
     * <p>MySQL 的 DATE() 返回 {@link java.sql.Date}，DATE_FORMAT() 返回 {@link String}，
     * 统一转为字符串避免 ClassCastException。</p>
     */
    private String toStringValue(Object value) {
        if (value == null) return null;
        if (value instanceof String s) return s;
        if (value instanceof java.sql.Date sd) return sd.toLocalDate().toString();
        if (value instanceof java.time.LocalDate ld) return ld.toString();
        if (value instanceof java.time.LocalDateTime ldt) return ldt.toLocalDate().toString();
        return value.toString();
    }

    private LocalDateTime parseDateTime(String str) {
        try {
            return LocalDateTime.parse(str, QUERY_DTF);
        } catch (Exception e) {
            try {
                return LocalDateTime.parse(str + " 00:00:00", QUERY_DTF);
            } catch (Exception ex) {
                log.warn("时间解析失败: {}", str);
                return LocalDateTime.now();
            }
        }
    }
}
