package com.competition.energycontrollerplatform.service.impl;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.support.ExcelTypeEnum;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.competition.energycontrollerplatform.dto.BalanceAdjustmentExecuteRequest;
import com.competition.energycontrollerplatform.dto.BalanceAdjustmentPageRequest;
import com.competition.energycontrollerplatform.dto.BalanceAdjustmentSummaryVO;
import com.competition.energycontrollerplatform.dto.BalanceAdjustmentVO;
import com.competition.energycontrollerplatform.entity.AccountFlow;
import com.competition.energycontrollerplatform.entity.BalanceAdjustment;
import com.competition.energycontrollerplatform.entity.Customer;
import com.competition.energycontrollerplatform.entity.Meter;
import com.competition.energycontrollerplatform.mapper.AccountFlowMapper;
import com.competition.energycontrollerplatform.mapper.BalanceAdjustmentMapper;
import com.competition.energycontrollerplatform.mapper.CustomerMapper;
import com.competition.energycontrollerplatform.mapper.MeterMapper;
import com.competition.energycontrollerplatform.security.SecurityUserContext;
import com.competition.energycontrollerplatform.service.BalanceAdjustmentService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 余额调整服务实现。
 * <p>
 * <b>核心规范：</b>
 * <ul>
 *   <li>所有金额字段使用 {@link java.math.BigDecimal}，严禁 float/double</li>
 *   <li>非 ADMIN 角色强制注入当前商户 ID 做数据隔离</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class BalanceAdjustmentServiceImpl implements BalanceAdjustmentService {

    private static final Logger log = LoggerFactory.getLogger(BalanceAdjustmentServiceImpl.class);
    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final BalanceAdjustmentMapper balanceAdjustmentMapper;
    private final CustomerMapper customerMapper;
    private final MeterMapper meterMapper;
    private final AccountFlowMapper accountFlowMapper;

    // ──────────────────────── 分页查询 ────────────────────────

    @Override
    public IPage<BalanceAdjustmentVO> listAdjustments(BalanceAdjustmentPageRequest request) {
        LambdaQueryWrapper<BalanceAdjustment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BalanceAdjustment::getIsDeleted, 0);

        // ── 权限隔离 ──
        Long effectiveCustomerId = resolveCustomerId(request.getCustomerId());
        if (effectiveCustomerId != null) {
            wrapper.eq(BalanceAdjustment::getCustomerId, effectiveCustomerId);
        }

        // 时段筛选
        if (StringUtils.hasText(request.getStartTime())) {
            wrapper.ge(BalanceAdjustment::getAdjustmentTime, parseDateTime(request.getStartTime()));
        }
        if (StringUtils.hasText(request.getEndTime())) {
            wrapper.le(BalanceAdjustment::getAdjustmentTime, parseDateTime(request.getEndTime()));
        }

        // 调整类型筛选
        if (StringUtils.hasText(request.getAdjustmentType())) {
            wrapper.eq(BalanceAdjustment::getAdjustmentType, request.getAdjustmentType().toUpperCase());
        }

        // 关键字搜索（调整编号/表号/操作员/原因/分组/商户名）
        if (StringUtils.hasText(request.getKeyword())) {
            String keyword = request.getKeyword();
            wrapper.and(w -> w
                    .like(BalanceAdjustment::getAdjustmentNo, keyword)
                    .or()
                    .like(BalanceAdjustment::getDbNo, keyword)
                    .or()
                    .like(BalanceAdjustment::getOperatorName, keyword)
                    .or()
                    .like(BalanceAdjustment::getBz, keyword)
                    .or()
                    .like(BalanceAdjustment::getGroupName, keyword)
                    .or()
                    .exists("SELECT 1 FROM t_customer WHERE t_customer.id = customer_id AND t_customer.is_deleted = 0 AND t_customer.customer_name LIKE CONCAT('%', {0}, '%')", keyword));
        }

        // 分组名称筛选
        if (StringUtils.hasText(request.getGroupName())) {
            wrapper.eq(BalanceAdjustment::getGroupName, request.getGroupName());
        }

        wrapper.orderByDesc(BalanceAdjustment::getAdjustmentTime);

        int pageNum = request.getPageNum() != null ? request.getPageNum() : 1;
        int pageSize = request.getPageSize() != null ? request.getPageSize() : 20;
        Page<BalanceAdjustment> page = new Page<>(pageNum, pageSize);
        IPage<BalanceAdjustment> result = balanceAdjustmentMapper.selectPage(page, wrapper);

        // 批量预加载关联数据，避免 N+1 查询
        java.util.List<BalanceAdjustment> entities = result.getRecords();
        java.util.Map<Long, Customer> customerMap = buildCustomerMap(entities);
        java.util.Map<Long, Meter> meterMap = buildMeterMap(entities);

        return result.convert(entity -> toVO(entity, customerMap, meterMap));
    }

    // ──────────────────────── 详情 ────────────────────────

    @Override
    public BalanceAdjustmentVO getAdjustmentById(Long id) {
        BalanceAdjustment entity = balanceAdjustmentMapper.selectById(id);
        if (entity == null || entity.getIsDeleted() != null && entity.getIsDeleted() == 1) {
            return null;
        }

        // 权限隔离：非 ADMIN 只能查看自己商户的记录
        if (!SecurityUserContext.isAdmin()) {
            Long myCustomerId = SecurityUserContext.getCurrentCustomerId();
            if (myCustomerId == null || !myCustomerId.equals(entity.getCustomerId())) {
                log.warn("[权限拒绝] 非ADMIN尝试查看其他商户的调账记录 id={}", id);
                return null;
            }
        }

        return toVO(entity);
    }

    /**
     * 单条记录的 VO 转换（无预加载，适用于 getById 等单条查询场景）。
     */
    private BalanceAdjustmentVO toVO(BalanceAdjustment entity) {
        java.util.Map<Long, Customer> cm = entity.getCustomerId() != null
                ? customerMapper.selectBatchIds(java.util.List.of(entity.getCustomerId())).stream()
                    .collect(java.util.stream.Collectors.toMap(Customer::getId, c -> c, (a, b) -> a))
                : java.util.Map.of();
        java.util.Map<Long, Meter> mm = entity.getMeterId() != null
                ? meterMapper.selectBatchIds(java.util.List.of(entity.getMeterId())).stream()
                    .collect(java.util.stream.Collectors.toMap(Meter::getId, m -> m, (a, b) -> a))
                : java.util.Map.of();
        return toVO(entity, cm, mm);
    }

    // ──────────────────────── 统计（SQL 聚合） ────────────────────────

    @Override
    public List<BalanceAdjustmentSummaryVO> getAdjustmentSummary(BalanceAdjustmentPageRequest request) {
        Long effectiveCustomerId = resolveCustomerId(request.getCustomerId());

        List<java.util.Map<String, Object>> rows = balanceAdjustmentMapper.sumByDay(
                request.getStartTime(), request.getEndTime(), effectiveCustomerId);

        List<BalanceAdjustmentSummaryVO> result = new ArrayList<>(rows.size());
        for (java.util.Map<String, Object> row : rows) {
            BalanceAdjustmentSummaryVO vo = new BalanceAdjustmentSummaryVO();
            vo.setPeriod((String) row.get("period"));
            vo.setTotalAmount(toBigDecimal(row.get("totalAmount")));
            vo.setAdjustCount(toLong(row.get("adjustCount")));
            vo.setCustomerCount(toLong(row.get("customerCount")));
            result.add(vo);
        }

        log.info("[余额调整统计] customerId={}, rows={}", effectiveCustomerId, result.size());
        return result;
    }

    private java.math.BigDecimal toBigDecimal(Object value) {
        if (value == null) return java.math.BigDecimal.ZERO;
        if (value instanceof java.math.BigDecimal bd) return bd;
        if (value instanceof Double d) return java.math.BigDecimal.valueOf(d);
        if (value instanceof Number n) return java.math.BigDecimal.valueOf(n.doubleValue());
        try {
            return new java.math.BigDecimal(value.toString());
        } catch (NumberFormatException e) {
            return java.math.BigDecimal.ZERO;
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

    // ──────────────────────── Excel 导出 ────────────────────────

    @Override
    public void exportAdjustments(HttpServletResponse response, BalanceAdjustmentPageRequest request) {
        // 1. 构建查询条件（不分页）
        LambdaQueryWrapper<BalanceAdjustment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BalanceAdjustment::getIsDeleted, 0);

        Long effectiveCustomerId = resolveCustomerId(request.getCustomerId());
        if (effectiveCustomerId != null) {
            wrapper.eq(BalanceAdjustment::getCustomerId, effectiveCustomerId);
        }

        if (StringUtils.hasText(request.getStartTime())) {
            wrapper.ge(BalanceAdjustment::getAdjustmentTime, parseDateTime(request.getStartTime()));
        }
        if (StringUtils.hasText(request.getEndTime())) {
            wrapper.le(BalanceAdjustment::getAdjustmentTime, parseDateTime(request.getEndTime()));
        }

        if (StringUtils.hasText(request.getAdjustmentType())) {
            wrapper.eq(BalanceAdjustment::getAdjustmentType, request.getAdjustmentType().toUpperCase());
        }

        if (StringUtils.hasText(request.getKeyword())) {
            String keyword = request.getKeyword();
            wrapper.and(w -> w
                    .like(BalanceAdjustment::getAdjustmentNo, keyword)
                    .or()
                    .like(BalanceAdjustment::getDbNo, keyword)
                    .or()
                    .like(BalanceAdjustment::getOperatorName, keyword)
                    .or()
                    .like(BalanceAdjustment::getBz, keyword)
                    .or()
                    .like(BalanceAdjustment::getGroupName, keyword)
                    .or()
                    .exists("SELECT 1 FROM t_customer WHERE t_customer.id = customer_id AND t_customer.is_deleted = 0 AND t_customer.customer_name LIKE CONCAT('%', {0}, '%')", keyword));
        }

        // 分组名称筛选
        if (StringUtils.hasText(request.getGroupName())) {
            wrapper.eq(BalanceAdjustment::getGroupName, request.getGroupName());
        }

        wrapper.orderByDesc(BalanceAdjustment::getAdjustmentTime);

        List<BalanceAdjustment> entities = balanceAdjustmentMapper.selectList(wrapper);
        java.util.Map<Long, Customer> customerMap = buildCustomerMap(entities);
        java.util.Map<Long, Meter> meterMap = buildMeterMap(entities);
        List<BalanceAdjustmentVO> voList = entities.stream()
                .map(entity -> toVO(entity, customerMap, meterMap))
                .collect(java.util.stream.Collectors.toList());

        // 2. 设置 HTTP 响应头
        setExcelResponse(response, "余额调整记录.xlsx");

        // 3. 流式写入 Excel
        try {
            EasyExcel.write(response.getOutputStream(), BalanceAdjustmentVO.class)
                    .excelType(ExcelTypeEnum.XLSX)
                    .sheet("余额调整记录")
                    .doWrite(voList);
        } catch (IOException e) {
            log.error("[Excel导出] 余额调整导出失败", e);
            throw new RuntimeException("Excel 导出失败", e);
        }

        log.info("[Excel导出] 余额调整导出完成，共 {} 条记录", voList.size());
    }

    // ──────────────────────── 余额退补统一执行 ────────────────────────

    @Override
    @org.springframework.transaction.annotation.Transactional(rollbackFor = Exception.class)
    public BalanceAdjustmentVO executeAdjustment(BalanceAdjustmentExecuteRequest request) {
        // 仅 ADMIN 可执行退补操作
        if (!SecurityUserContext.isAdmin()) {
            throw new com.competition.energycontrollerplatform.common.BusinessException(
                    403, "仅管理员可执行余额退补操作");
        }

        // 解析表计：优先用 dbNo，其次 meterId
        Meter meter = null;
        if (com.baomidou.mybatisplus.core.toolkit.StringUtils.isNotBlank(request.getDbNo())) {
            meter = meterMapper.selectOne(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Meter>()
                            .eq(Meter::getDbNo, request.getDbNo())
                            .eq(Meter::getIsDeleted, 0));
        }
        if (meter == null && request.getMeterId() != null) {
            meter = meterMapper.selectById(request.getMeterId());
        }
        if (meter == null) {
            throw new com.competition.energycontrollerplatform.common.BusinessException(404,
                    "表计不存在（请检查表号或ID）");
        }

        Long customerId = meter.getCustomerId();
        if (customerId == null) {
            throw new com.competition.energycontrollerplatform.common.BusinessException(400, "该表计未关联商户，无法执行退补");
        }

        Customer customer = customerMapper.selectById(customerId);
        if (customer == null) {
            throw new com.competition.energycontrollerplatform.common.BusinessException(404, "关联商户不存在");
        }

        BigDecimal amountChange = request.getAmountChange();
        if (amountChange.compareTo(java.math.BigDecimal.ZERO) == 0) {
            throw new com.competition.energycontrollerplatform.common.BusinessException(400, "金额变动不能为0");
        }

        String executeType = request.getExecuteType() != null
                ? request.getExecuteType().toUpperCase() : "BALANCE_ADJUST";
        String operator = SecurityUserContext.getCurrentUsername();
        String reason = request.getReason();
        LocalDateTime now = LocalDateTime.now();

        // ── 1. 如果指定了 adjustedPrice 且与当前电价不同，记录电价变更 ──
        boolean priceChanged = false;
        java.math.BigDecimal oldPrice = meter.getPlusPrice();
        if ("PRICE_ADJUST".equals(executeType)
                && request.getAdjustedPrice() != null
                && request.getAdjustedPrice().compareTo(java.math.BigDecimal.ZERO) > 0
                && (meter.getPlusPrice() == null
                    || request.getAdjustedPrice().compareTo(meter.getPlusPrice()) != 0)) {

            oldPrice = meter.getPlusPrice();
            if (!Boolean.TRUE.equals(request.getPreviewOnly())) {
                meter.setPlusPrice(request.getAdjustedPrice());
                meter.setUpdateTime(now);
                meterMapper.updateById(meter);
                priceChanged = true;
                log.info("[余额退补] 电价已调整 meterId={}, oldPrice={}, newPrice={}",
                        request.getMeterId(), oldPrice, request.getAdjustedPrice());
            }
        }

        // ── 2. 仅预览模式：返回预算结果不执行 ──
        java.math.BigDecimal balanceBefore = customer.getBalance() != null
                ? customer.getBalance() : java.math.BigDecimal.ZERO;
        java.math.BigDecimal balanceAfter = balanceBefore.add(amountChange);

        java.math.BigDecimal warnline = meter.getWarnline() != null
                ? meter.getWarnline() : java.math.BigDecimal.ZERO;
        boolean willTriggerShutdown = balanceAfter.compareTo(warnline) < 0;

        if (Boolean.TRUE.equals(request.getPreviewOnly())) {
            BalanceAdjustmentVO preview = new BalanceAdjustmentVO();
            preview.setMeterId(request.getMeterId());
            preview.setDbNo(meter.getDbNo());
            preview.setCustomerId(customerId);
            preview.setCustomerName(customer.getCustomerName());
            preview.setBalance(balanceBefore);  // 当前余额
            preview.setAdjustmentType(amountChange.compareTo(java.math.BigDecimal.ZERO) > 0
                    ? "INCREASE" : "DECREASE");
            preview.setAdjustmentTypeLabel(amountChange.compareTo(java.math.BigDecimal.ZERO) > 0
                    ? "余额增加" : "余额扣减");
            preview.setOriginalPrice(oldPrice != null ? oldPrice : meter.getPlusPrice());
            preview.setOriginalSurcharge(meter.getPlusPrice());
            if ("PRICE_ADJUST".equals(executeType) && request.getAdjustedPrice() != null) {
                preview.setAdjustPrice(request.getAdjustedPrice());
            }
            preview.setAdjustSurcharge(request.getAdjustedPrice());
            preview.setPaymentStatus("—");
            preview.setDeductMethod(request.getDeductMethod() != null
                    ? ("immediate".equals(request.getDeductMethod()) ? "立即扣除" : "延期扣除")
                    : "余额扣除");
            preview.setRemark(request.getRemark());
            preview.setGroupName(meter.getProName());
            preview.setBz(reason + (priceChanged
                    ? String.format(" [电价将从%s调整为%s]", oldPrice, request.getAdjustedPrice()) : ""));
            // 复用 bz 字段存预览信息
            preview.setBz(preview.getBz()
                    + String.format(" [预览] 退补前余额=%.2f, 金额变动=%.2f, 退补后余额=%.2f, 触发欠费=%s",
                    balanceBefore, amountChange, balanceAfter, willTriggerShutdown ? "是" : "否"));
            return preview;
        }

        // ── 3. 执行余额变动（悲观锁 + 原子 SQL） ──
        // 用 SELECT ... FOR UPDATE 锁住商户行，确保 balanceBefore 精准，
        // 不受并发充值/退费/调账干扰。锁在事务提交时自动释放。
        Customer lockedCustomer = customerMapper.selectByIdForUpdate(customerId);
        if (lockedCustomer == null) {
            throw new com.competition.energycontrollerplatform.common.BusinessException(404, "商户不存在或已注销");
        }
        java.math.BigDecimal lockedBalanceBefore = lockedCustomer.getBalance() != null
                ? lockedCustomer.getBalance() : java.math.BigDecimal.ZERO;

        // 扣减场景预检余额（避免无效 SQL，同时给出明确错误信息）
        if (amountChange.compareTo(java.math.BigDecimal.ZERO) < 0
                && lockedBalanceBefore.compareTo(amountChange.abs()) < 0) {
            throw new com.competition.energycontrollerplatform.common.WalletException(
                    com.competition.energycontrollerplatform.common.WalletException.CODE_INSUFFICIENT_BALANCE,
                    "余额不足，当前余额：" + lockedBalanceBefore
                    + "，需要扣减：" + amountChange.abs());
        }

        int rows;
        if (amountChange.compareTo(java.math.BigDecimal.ZERO) > 0) {
            rows = customerMapper.addBalance(customerId, amountChange);
        } else {
            rows = customerMapper.subtractBalance(customerId, amountChange.abs());
        }

        if (rows == 0) {
            throw new com.competition.energycontrollerplatform.common.BusinessException(500, "余额变动失败，请重试");
        }

        java.math.BigDecimal lockedBalanceAfter = lockedBalanceBefore.add(amountChange);

        // ── 4. 写调账记录 ──
        String adjustmentNo = generateAdjNo("ADJ");
        boolean isIncrease = amountChange.compareTo(java.math.BigDecimal.ZERO) > 0;
        BalanceAdjustment adjustment = new BalanceAdjustment();
        adjustment.setAdjustmentNo(adjustmentNo);
        adjustment.setMeterId(request.getMeterId());
        adjustment.setCustomerId(customerId);
        adjustment.setDbNo(meter.getDbNo());
        adjustment.setBalance(amountChange);
        adjustment.setAdjustmentType(isIncrease ? "INCREASE" : "DECREASE");
        // 新增字段：记录退补时的电价信息
        adjustment.setOriginalPrice(oldPrice != null ? oldPrice : meter.getPlusPrice());
        adjustment.setOriginalSurcharge(meter.getPlusPrice());
        if ("PRICE_ADJUST".equals(executeType) && request.getAdjustedPrice() != null) {
            adjustment.setAdjustPrice(request.getAdjustedPrice());
        }
        adjustment.setAdjustSurcharge(request.getAdjustedPrice());
        adjustment.setPaymentStatus("已完成");
        adjustment.setDeductMethod(request.getDeductMethod() != null
                ? ("immediate".equals(request.getDeductMethod()) ? "立即扣除" : "延期扣除")
                : "余额扣除");
        adjustment.setDeductTime(now);
        adjustment.setGroupName(meter.getProName()); // 项目名称作为分组
        adjustment.setBz(reason + (priceChanged
                ? String.format(" [电价: %.4f→%.4f]", oldPrice, request.getAdjustedPrice()) : ""));
        adjustment.setOperatorName(operator);
        adjustment.setAdjustmentTime(now);
        adjustment.setRemark(request.getRemark());
        adjustment.setCreateTime(now);
        adjustment.setUpdateTime(now);
        balanceAdjustmentMapper.insert(adjustment);

        // ── 5. 写资金流水 ──
        com.competition.energycontrollerplatform.entity.AccountFlow flow =
                new com.competition.energycontrollerplatform.entity.AccountFlow();
        flow.setFlowNo(generateAdjNo("FLW"));
        flow.setCustomerId(customerId);
        flow.setMeterId(request.getMeterId());
        flow.setBalanceBefore(lockedBalanceBefore);
        flow.setChangeAmount(amountChange);
        flow.setBalanceAfter(lockedBalanceAfter);
        flow.setTradeType(4); // 调账
        flow.setBizNo(adjustmentNo);
        flow.setOperatorName(operator);
        flow.setOperateTime(now);
        flow.setRemark(reason + (priceChanged ? " [已同步调整电价]" : ""));
        flow.setCreateTime(now);
        flow.setUpdateTime(now);
        accountFlowMapper.insert(flow);

        log.info("[余额退补] 执行成功 meterId={}, customerId={}, adjustmentNo={}, type={}, "
                + "priceChanged={}, balance {}→{}, amount={}",
                request.getMeterId(), customerId, adjustmentNo,
                isIncrease ? "INCREASE" : "DECREASE",
                priceChanged, lockedBalanceBefore, lockedBalanceAfter, amountChange);

        // ── 6. 构建返回 VO ──
        BalanceAdjustmentVO vo = new BalanceAdjustmentVO();
        vo.setId(adjustment.getId());
        vo.setAdjustmentNo(adjustmentNo);
        vo.setCustomerId(customerId);
        vo.setCustomerName(lockedCustomer.getCustomerName());
        vo.setMeterId(request.getMeterId());
        vo.setDbNo(meter.getDbNo());
        vo.setBalance(amountChange);
        vo.setAdjustmentType(isIncrease ? "INCREASE" : "DECREASE");
        vo.setAdjustmentTypeLabel(isIncrease ? "余额增加" : "余额扣减");
        vo.setOriginalPrice(oldPrice != null ? oldPrice : meter.getPlusPrice());
        vo.setOriginalSurcharge(meter.getPlusPrice());
        if ("PRICE_ADJUST".equals(executeType) && request.getAdjustedPrice() != null) {
            vo.setAdjustPrice(request.getAdjustedPrice());
        }
        vo.setAdjustSurcharge(request.getAdjustedPrice());
        vo.setPaymentStatus("已完成");
        vo.setDeductMethod("余额扣除");
        vo.setDeductTime(now);
        vo.setGroupName(meter.getProName());
        vo.setBz(adjustment.getBz());
        vo.setOperatorName(operator);
        vo.setAdjustmentTime(now);
        vo.setRemark(request.getRemark());
        vo.setCreateTime(now);
        return vo;
    }

    // ──────────────────────── 私有方法 ────────────────────────

    /**
     * 权限隔离：解析目标商户 ID。
     * <p>非 ADMIN 且未关联商户时返回 null，调用方判空后返回空结果。
     */
    private Long resolveCustomerId(Long requestedCustomerId) {
        if (SecurityUserContext.isAdmin()) {
            return requestedCustomerId;
        }
        Long myCustomerId = SecurityUserContext.getCurrentCustomerId();
        if (myCustomerId == null) {
            log.warn("[权限拒绝] 非 ADMIN 且未关联商户的用户尝试访问余额调整数据");
            return null;
        }
        return myCustomerId;
    }

    /**
     * 实体 → VO 转换（从预加载的 Map 中取关联数据，避免 N+1 查询）。
     */
    private BalanceAdjustmentVO toVO(BalanceAdjustment entity,
                                     java.util.Map<Long, Customer> customerMap,
                                     java.util.Map<Long, Meter> meterMap) {
        BalanceAdjustmentVO vo = new BalanceAdjustmentVO();
        vo.setId(entity.getId());
        vo.setAdjustmentNo(entity.getAdjustmentNo());
        vo.setCustomerId(entity.getCustomerId());
        vo.setMeterId(entity.getMeterId());
        vo.setDbNo(entity.getDbNo());
        vo.setBalance(entity.getBalance());
        vo.setAdjustmentType(entity.getAdjustmentType());
        vo.setAdjustmentTypeLabel(adjustmentTypeLabel(entity.getAdjustmentType()));
        vo.setBz(entity.getBz());
        vo.setOperatorName(entity.getOperatorName());
        vo.setAdjustmentTime(entity.getAdjustmentTime());
        vo.setRemark(entity.getRemark());
        vo.setCreateTime(entity.getCreateTime());

        // 新增字段
        vo.setOriginalPrice(entity.getOriginalPrice());
        vo.setOriginalSurcharge(entity.getOriginalSurcharge());
        vo.setAdjustPrice(entity.getAdjustPrice());
        vo.setAdjustSurcharge(entity.getAdjustSurcharge());
        vo.setPaymentStatus(entity.getPaymentStatus());
        vo.setDeductMethod(entity.getDeductMethod());
        vo.setDeductTime(entity.getDeductTime());
        vo.setGroupName(entity.getGroupName());

        // 填充商户名称（从预加载 Map）
        if (entity.getCustomerId() != null) {
            Customer c = customerMap.get(entity.getCustomerId());
            if (c != null) {
                vo.setCustomerName(c.getCustomerName());
            }
        }

        // 填充表号（从预加载 Map）
        if (vo.getDbNo() == null && entity.getMeterId() != null) {
            Meter m = meterMap.get(entity.getMeterId());
            if (m != null) {
                vo.setDbNo(m.getDbNo());
            }
        }

        return vo;
    }

    /**
     * 批量构建商户 Map（用于 toVO 预加载）。
     */
    private java.util.Map<Long, Customer> buildCustomerMap(java.util.List<BalanceAdjustment> entities) {
        java.util.Set<Long> ids = entities.stream()
                .map(BalanceAdjustment::getCustomerId)
                .filter(id -> id != null)
                .collect(java.util.stream.Collectors.toSet());
        if (ids.isEmpty()) return java.util.Map.of();
        return customerMapper.selectBatchIds(ids).stream()
                .collect(java.util.stream.Collectors.toMap(Customer::getId, c -> c, (a, b) -> a));
    }

    /**
     * 批量构建表计 Map（用于 toVO 预加载）。
     */
    private java.util.Map<Long, Meter> buildMeterMap(java.util.List<BalanceAdjustment> entities) {
        java.util.Set<Long> ids = entities.stream()
                .map(BalanceAdjustment::getMeterId)
                .filter(id -> id != null)
                .collect(java.util.stream.Collectors.toSet());
        if (ids.isEmpty()) return java.util.Map.of();
        return meterMapper.selectBatchIds(ids).stream()
                .collect(java.util.stream.Collectors.toMap(Meter::getId, m -> m, (a, b) -> a));
    }

    private String adjustmentTypeLabel(String type) {
        if (type == null) return "未知";
        return switch (type.toUpperCase()) {
            case "INCREASE" -> "余额增加";
            case "DECREASE" -> "余额扣减";
            default -> type;
        };
    }

    /**
     * 生成业务单号（退补/流水）。
     */
    private String generateAdjNo(String prefix) {
        String ts = String.valueOf(System.currentTimeMillis());
        String shortTs = ts.substring(ts.length() - 8);
        String shortUuid = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        return prefix + shortTs + shortUuid;
    }

    private LocalDateTime parseDateTime(String str) {
        try {
            return LocalDateTime.parse(str, DTF);
        } catch (Exception e) {
            try {
                return LocalDateTime.parse(str + " 00:00:00", DTF);
            } catch (Exception ex) {
                log.warn("时间解析失败: {}", str);
                return LocalDateTime.now();
            }
        }
    }

    /**
     * 设置 Excel 文件下载响应头。
     */
    private void setExcelResponse(HttpServletResponse response, String fileName) {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("UTF-8");
        String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8)
                .replaceAll("\\+", "%20");
        response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + encodedFileName);
        response.setHeader("Access-Control-Expose-Headers", "Content-Disposition");
    }
}
