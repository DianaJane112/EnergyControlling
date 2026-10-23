package com.competition.energycontrollerplatform.service.impl;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.support.ExcelTypeEnum;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.competition.energycontrollerplatform.common.BusinessException;
import com.competition.energycontrollerplatform.dto.*;
import com.competition.energycontrollerplatform.entity.*;
import com.competition.energycontrollerplatform.mapper.*;
import com.competition.energycontrollerplatform.security.SecurityUserContext;
import com.competition.energycontrollerplatform.service.PropertyService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 物业管理/即时账单服务实现。
 * <p>
 * 即时账单计算逻辑：
 * <ol>
 *   <li>从 t_meter_reading 查最新一条读数作为当前读数</li>
 *   <li>从 t_meter.last_settled_reading 获取上次结算底数</li>
 *   <li>在内存中用 BigDecimal 计算差值，杜绝浮点数精度丢失</li>
 *   <li>返回完整账单结构：当前读数、上次读数、消耗用量、账单金额、计算时间戳</li>
 * </ol>
 */
@Service
@RequiredArgsConstructor
public class PropertyServiceImpl implements PropertyService {

    private static final Logger log = LoggerFactory.getLogger(PropertyServiceImpl.class);

    private final MeterReadingMapper meterReadingMapper;
    private final MeterMapper meterMapper;
    private final CustomerMapper customerMapper;
    private final AccountFlowMapper accountFlowMapper;

    // ──────────────────────── 单表即时账单 ────────────────────────

    @Override
    public InstantBillVO generateInstantBill(Long meterId) {
        // ① 校验表计存在
        Meter meter = meterMapper.selectById(meterId);
        if (meter == null) {
            throw BusinessException.of(404, "表计不存在");
        }

        // ── 权限隔离：非 ADMIN 仅可查自身表计 ──
        if (!SecurityUserContext.isAdmin()) {
            Long myCustomerId = SecurityUserContext.getCurrentCustomerId();
            if (myCustomerId == null || !myCustomerId.equals(meter.getCustomerId())) {
                throw BusinessException.of(403, "无权查看该表计的即时账单");
            }
        }

        return buildInstantBillVO(meter);
    }

    // ──────────────────────── 即时账单分页列表 ────────────────────────

    @Override
    public IPage<InstantBillListVO> listInstantBills(InstantBillPageRequest request) {
        LambdaQueryWrapper<Meter> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Meter::getIsDeleted, 0);

        // 权限隔离
        Long effectiveCustomerId = resolveCustomerId(request.getCustomerId());
        if (effectiveCustomerId != null) {
            wrapper.eq(Meter::getCustomerId, effectiveCustomerId);
        }

        // 筛选条件
        if (StringUtils.hasText(request.getMeterNo())) {
            wrapper.and(w -> w
                    .like(Meter::getStealNo, request.getMeterNo())
                    .or()
                    .like(Meter::getDbNo, request.getMeterNo())
                    .or()
                    .like(Meter::getMeteraddress, request.getMeterNo()));
        }
        if (request.getMinAmount() != null || request.getMaxAmount() != null) {
            // 金额筛选在 JVM 侧完成（需先计算账单）
            // 此处仅标记，实际在 convert 后过滤
        }
        if (StringUtils.hasText(request.getBillStatus())) {
            // 按余额推断状态
            if ("OVERDUE".equals(request.getBillStatus())) {
                wrapper.eq(Meter::getStatus, 2); // 欠费停机
            } else if ("UNPAID".equals(request.getBillStatus())) {
                wrapper.apply("customer_id IN (SELECT id FROM t_customer WHERE balance <= 0 AND is_deleted = 0)");
            }
        }

        wrapper.orderByDesc(Meter::getUpdateTime);

        int pageNum = request.getPageNum() != null ? request.getPageNum() : 1;
        int pageSize = request.getPageSize() != null ? request.getPageSize() : 20;
        Page<Meter> page = new Page<>(pageNum, pageSize);
        IPage<Meter> meterPage = meterMapper.selectPage(page, wrapper);

        // 转换为 InstantBillListVO，实时计算每条账单
        IPage<InstantBillListVO> resultPage = meterPage.convert(meter -> {
            InstantBillVO bill = buildInstantBillVO(meter);
            return toListVO(meter, bill);
        });

        // JVM 侧金额筛选
        if (request.getMinAmount() != null || request.getMaxAmount() != null) {
            List<InstantBillListVO> filtered = new ArrayList<>();
            for (InstantBillListVO vo : resultPage.getRecords()) {
                BigDecimal amount = vo.getBillAmount();
                if (request.getMinAmount() != null && amount.compareTo(request.getMinAmount()) < 0) continue;
                if (request.getMaxAmount() != null && amount.compareTo(request.getMaxAmount()) > 0) continue;
                filtered.add(vo);
            }
            resultPage.setRecords(filtered);
            resultPage.setTotal(filtered.size());
        }

        return resultPage;
    }

    @Override
    public Map<String, Object> getInstantBillStats(InstantBillPageRequest request) {
        Map<String, Object> stats = new LinkedHashMap<>();

        Long effectiveCustomerId = resolveCustomerId(request.getCustomerId());

        // ── 总表计数（即账单总数） ──
        LambdaQueryWrapper<Meter> meterWrapper = new LambdaQueryWrapper<>();
        meterWrapper.eq(Meter::getIsDeleted, 0);
        if (effectiveCustomerId != null) {
            meterWrapper.eq(Meter::getCustomerId, effectiveCustomerId);
        }
        long totalCount = meterMapper.selectCount(meterWrapper);

        // ── 欠费停机数（逾期） ──
        LambdaQueryWrapper<Meter> overdueWrapper = new LambdaQueryWrapper<>();
        overdueWrapper.eq(Meter::getIsDeleted, 0).eq(Meter::getStatus, 2);
        if (effectiveCustomerId != null) {
            overdueWrapper.eq(Meter::getCustomerId, effectiveCustomerId);
        }
        long overdueCount = meterMapper.selectCount(overdueWrapper);

        // ── 未缴数：余额 <= 0 的商户关联的表计（非逾期） ──
        List<Long> unpaidCustomerIds;
        if (effectiveCustomerId != null) {
            Customer c = customerMapper.selectById(effectiveCustomerId);
            unpaidCustomerIds = (c != null && c.getBalance().compareTo(BigDecimal.ZERO) <= 0)
                    ? List.of(effectiveCustomerId) : List.of();
        } else {
            LambdaQueryWrapper<Customer> customerWrapper = new LambdaQueryWrapper<>();
            customerWrapper.eq(Customer::getIsDeleted, 0).le(Customer::getBalance, BigDecimal.ZERO);
            unpaidCustomerIds = customerMapper.selectList(customerWrapper).stream()
                    .map(Customer::getId).collect(java.util.stream.Collectors.toList());
        }
        long unpaidCount = 0;
        if (!unpaidCustomerIds.isEmpty()) {
            LambdaQueryWrapper<Meter> unpaidWrapper = new LambdaQueryWrapper<>();
            unpaidWrapper.eq(Meter::getIsDeleted, 0)
                    .ne(Meter::getStatus, 2)  // 排除逾期的
                    .in(Meter::getCustomerId, unpaidCustomerIds);
            unpaidCount = meterMapper.selectCount(unpaidWrapper);
        }

        // ── 已缴数：余额 > 0 的商户关联的表计 ──
        List<Long> paidCustomerIds;
        if (effectiveCustomerId != null) {
            Customer c = customerMapper.selectById(effectiveCustomerId);
            paidCustomerIds = (c != null && c.getBalance().compareTo(BigDecimal.ZERO) > 0)
                    ? List.of(effectiveCustomerId) : List.of();
        } else {
            LambdaQueryWrapper<Customer> customerWrapper = new LambdaQueryWrapper<>();
            customerWrapper.eq(Customer::getIsDeleted, 0).gt(Customer::getBalance, BigDecimal.ZERO);
            paidCustomerIds = customerMapper.selectList(customerWrapper).stream()
                    .map(Customer::getId).collect(java.util.stream.Collectors.toList());
        }
        long paidCount = 0;
        if (!paidCustomerIds.isEmpty()) {
            LambdaQueryWrapper<Meter> paidWrapper = new LambdaQueryWrapper<>();
            paidWrapper.eq(Meter::getIsDeleted, 0).in(Meter::getCustomerId, paidCustomerIds);
            paidCount = meterMapper.selectCount(paidWrapper);
        }

        // ── 总金额（商户总余额） ──
        BigDecimal totalAmount = customerMapper.sumTotalBalance();
        if (effectiveCustomerId != null) {
            Customer c = customerMapper.selectById(effectiveCustomerId);
            totalAmount = c != null ? c.getBalance() : BigDecimal.ZERO;
        }

        // ── 已缴金额（余额 > 0 的商户余额合计） ──
        BigDecimal paidAmount = BigDecimal.ZERO;
        if (!paidCustomerIds.isEmpty()) {
            paidAmount = paidCustomerIds.stream()
                    .map(id -> {
                        Customer c = customerMapper.selectById(id);
                        return c != null ? c.getBalance() : BigDecimal.ZERO;
                    })
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }
        if (effectiveCustomerId != null && paidAmount.compareTo(BigDecimal.ZERO) == 0) {
            // 单个商户且已缴金额为0，直接用totalAmount
            if (totalAmount != null && totalAmount.compareTo(BigDecimal.ZERO) > 0) {
                paidAmount = totalAmount;
            }
        }

        // ── 今日消费 ──
        BigDecimal todayDebit = accountFlowMapper.sumTodayDebit();

        stats.put("totalCount", totalCount);
        stats.put("unpaidCount", unpaidCount);
        stats.put("paidCount", paidCount);
        stats.put("overdueCount", overdueCount);
        stats.put("totalAmount", totalAmount != null ? totalAmount : BigDecimal.ZERO);
        stats.put("paidAmount", paidAmount);
        stats.put("todayDebit", todayDebit != null ? todayDebit : BigDecimal.ZERO);

        return stats;
    }

    // ──────────────────────── 导出 ────────────────────────

    @Override
    public void exportInstantBills(HttpServletResponse response, InstantBillPageRequest request) {
        // 拉取全量数据（不分页）
        request.setPageNum(1);
        request.setPageSize(Integer.MAX_VALUE);
        IPage<InstantBillListVO> page = listInstantBills(request);

        setExcelResponse(response, "即时账单导出.xlsx");
        try {
            EasyExcel.write(response.getOutputStream(), InstantBillListVO.class)
                    .excelType(ExcelTypeEnum.XLSX)
                    .sheet("即时账单")
                    .doWrite(page.getRecords());
        } catch (IOException e) {
            log.error("[即时账单导出] 导出失败", e);
            throw new RuntimeException("Excel 导出失败", e);
        }
        log.info("[即时账单导出] 导出完成，共 {} 条记录", page.getRecords().size());
    }

    // ──────────────────────── 批量创建账单 ────────────────────────

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BatchOperationResponse batchCreateBills(BatchBillCreateRequest request) {
        if (!SecurityUserContext.isAdmin()) {
            throw BusinessException.of(403, "仅管理员可批量创建账单");
        }

        int success = 0, failed = 0;
        List<String> errors = new ArrayList<>();

        for (Long meterId : request.getMeterIds()) {
            try {
                Meter meter = meterMapper.selectById(meterId);
                if (meter == null) {
                    failed++;
                    errors.add("表计ID " + meterId + " 不存在");
                    continue;
                }

                // 生成即时账单
                InstantBillVO bill = buildInstantBillVO(meter);

                // 如果有固定金额则使用固定金额
                if (request.getFixedAmount() != null && request.getFixedAmount().compareTo(BigDecimal.ZERO) > 0) {
                    bill.setBillAmount(request.getFixedAmount());
                }

                // 写入资金流水（作为计费记录）
                if (meter.getCustomerId() != null) {
                    Customer customer = customerMapper.selectById(meter.getCustomerId());
                    if (customer != null) {
                        String flowNo = generateBizNo("BIL");
                        AccountFlow flow = new AccountFlow();
                        flow.setFlowNo(flowNo);
                        flow.setCustomerId(meter.getCustomerId());
                        flow.setMeterId(meterId);
                        flow.setBalanceBefore(customer.getBalance());
                        flow.setChangeAmount(bill.getBillAmount().negate());
                        flow.setBalanceAfter(customer.getBalance().subtract(bill.getBillAmount()));
                        flow.setTradeType(2); // 消费扣费
                        flow.setBizNo(flowNo);
                        flow.setOperatorName(request.getOperatorName());
                        flow.setOperateTime(LocalDateTime.now());
                        flow.setRemark(request.getBillDescription() != null ? request.getBillDescription()
                                : (request.getRemark() != null ? request.getRemark() : "批量生成账单"));
                        flow.setCreateTime(LocalDateTime.now());
                        flow.setUpdateTime(LocalDateTime.now());
                        accountFlowMapper.insert(flow);
                    }
                }

                success++;
            } catch (Exception e) {
                failed++;
                errors.add("表计ID " + meterId + ": " + e.getMessage());
            }
        }

        BatchOperationResponse response = new BatchOperationResponse();
        response.setTotal(request.getMeterIds().size());
        response.setSuccess(success);
        response.setFailed(failed);
        response.setMessage(String.format("批量创建账单完成：共 %d 条，成功 %d 条，失败 %d 条",
                request.getMeterIds().size(), success, failed));
        response.setErrors(errors);
        return response;
    }

    // ──────────────────────── 批量延期 ────────────────────────

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BatchOperationResponse batchDeferBills(BatchDeferRequest request) {
        if (!SecurityUserContext.isAdmin()) {
            throw BusinessException.of(403, "仅管理员可批量延期");
        }

        int success = 0, failed = 0;
        List<String> errors = new ArrayList<>();

        for (Long meterId : request.getMeterIds()) {
            try {
                // 验证表计存在
                Meter meter = meterMapper.selectById(meterId);
                if (meter == null) {
                    failed++;
                    errors.add("表计ID " + meterId + " 不存在");
                    continue;
                }

                // 查找该表计最近一条消费扣费流水，标记延期
                LambdaQueryWrapper<AccountFlow> wrapper = new LambdaQueryWrapper<>();
                wrapper.eq(AccountFlow::getMeterId, meterId)
                        .eq(AccountFlow::getTradeType, 2)
                        .eq(AccountFlow::getIsDeleted, 0)
                        .orderByDesc(AccountFlow::getOperateTime)
                        .last("LIMIT 1");
                AccountFlow flow = accountFlowMapper.selectOne(wrapper);
                if (flow != null) {
                    flow.setRemark((flow.getRemark() != null ? flow.getRemark() : "")
                            + " [已延期至" + request.getDeferTo() + "，原因：" + request.getDeferReason() + "]");
                    flow.setUpdateTime(LocalDateTime.now());
                    accountFlowMapper.updateById(flow);
                }
                success++;
            } catch (Exception e) {
                failed++;
                errors.add("表计ID " + meterId + ": " + e.getMessage());
            }
        }

        BatchOperationResponse response = new BatchOperationResponse();
        response.setTotal(request.getMeterIds().size());
        response.setSuccess(success);
        response.setFailed(failed);
        response.setMessage(String.format("批量延期完成：共 %d 条，成功 %d 条，失败 %d 条",
                request.getMeterIds().size(), success, failed));
        response.setErrors(errors);
        return response;
    }

    // ──────────────────────── 批量删除 ────────────────────────

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BatchOperationResponse batchDeleteBills(List<Long> meterIds) {
        if (!SecurityUserContext.isAdmin()) {
            throw BusinessException.of(403, "仅管理员可批量删除账单");
        }

        int success = 0, failed = 0;
        List<String> errors = new ArrayList<>();

        for (Long meterId : meterIds) {
            try {
                // 验证表计存在
                Meter meter = meterMapper.selectById(meterId);
                if (meter == null) {
                    failed++;
                    errors.add("表计ID " + meterId + " 不存在");
                    continue;
                }

                // 删除该表计关联的消费扣费流水记录（trade_type=2），按时间倒序取最近一条
                LambdaQueryWrapper<AccountFlow> wrapper = new LambdaQueryWrapper<>();
                wrapper.eq(AccountFlow::getMeterId, meterId)
                        .eq(AccountFlow::getTradeType, 2)
                        .eq(AccountFlow::getIsDeleted, 0)
                        .orderByDesc(AccountFlow::getOperateTime)
                        .last("LIMIT 1");
                AccountFlow flow = accountFlowMapper.selectOne(wrapper);
                if (flow != null) {
                    accountFlowMapper.logicDeleteById(flow.getId());
                }
                success++;
            } catch (Exception e) {
                failed++;
                errors.add("表计ID " + meterId + ": " + e.getMessage());
            }
        }

        BatchOperationResponse response = new BatchOperationResponse();
        response.setTotal(meterIds.size());
        response.setSuccess(success);
        response.setFailed(failed);
        response.setMessage(String.format("批量删除完成：共 %d 条，成功 %d 条，失败 %d 条",
                meterIds.size(), success, failed));
        response.setErrors(errors);
        return response;
    }

    // ──────────────────────── 批量导入 ────────────────────────

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BatchOperationResponse batchImportBills(BillImportRequest request) {
        if (!SecurityUserContext.isAdmin()) {
            throw BusinessException.of(403, "仅管理员可批量导入账单");
        }

        int success = 0, failed = 0;
        List<String> errors = new ArrayList<>();

        if (request.getBills() == null || request.getBills().isEmpty()) {
            BatchOperationResponse response = new BatchOperationResponse();
            response.setTotal(0);
            response.setSuccess(0);
            response.setFailed(0);
            response.setMessage("导入数据为空");
            return response;
        }

        for (BillImportRequest.BillImportItem item : request.getBills()) {
            try {
                // 查找表计
                Meter meter = findMeterByNo(item.getMeterNo());
                if (meter == null) {
                    failed++;
                    errors.add("表号 " + item.getMeterNo() + " 对应的表计不存在");
                    continue;
                }

                Long customerId = meter.getCustomerId();
                if (customerId == null) {
                    // 尝试通过商户编号查找
                    if (StringUtils.hasText(item.getCustomerCode())) {
                        LambdaQueryWrapper<Customer> cw = new LambdaQueryWrapper<>();
                        cw.eq(Customer::getCustomerCode, item.getCustomerCode());
                        Customer customer = customerMapper.selectOne(cw);
                        if (customer != null) {
                            customerId = customer.getId();
                        }
                    }
                }

                if (customerId == null) {
                    failed++;
                    errors.add("表号 " + item.getMeterNo() + " 未绑定商户");
                    continue;
                }

                Customer customer = customerMapper.selectById(customerId);
                if (customer == null) {
                    failed++;
                    errors.add("表号 " + item.getMeterNo() + " 对应的商户不存在");
                    continue;
                }

                BigDecimal billAmount = item.getBillAmount() != null ? item.getBillAmount() : BigDecimal.ZERO;

                // 写入资金流水
                String flowNo = generateBizNo("IMP");
                AccountFlow flow = new AccountFlow();
                flow.setFlowNo(flowNo);
                flow.setCustomerId(customerId);
                flow.setMeterId(meter.getId());
                flow.setBalanceBefore(customer.getBalance());
                flow.setChangeAmount(billAmount.negate());
                flow.setBalanceAfter(customer.getBalance().subtract(billAmount));
                flow.setTradeType(2);
                flow.setBizNo(flowNo);
                flow.setOperatorName(request.getOperatorName());
                flow.setOperateTime(LocalDateTime.now());
                flow.setRemark(item.getRemark());
                flow.setCreateTime(LocalDateTime.now());
                flow.setUpdateTime(LocalDateTime.now());
                accountFlowMapper.insert(flow);

                success++;
            } catch (Exception e) {
                failed++;
                errors.add("表号 " + item.getMeterNo() + ": " + e.getMessage());
            }
        }

        BatchOperationResponse response = new BatchOperationResponse();
        response.setTotal(request.getBills().size());
        response.setSuccess(success);
        response.setFailed(failed);
        response.setMessage(String.format("批量导入完成：共 %d 条，成功 %d 条，失败 %d 条",
                request.getBills().size(), success, failed));
        response.setErrors(errors);
        return response;
    }

    // ──────────────────────── 模板下载 ────────────────────────

    @Override
    public void downloadImportTemplate(HttpServletResponse response) {
        setExcelResponse(response, "批量导入账单模板.xlsx");
        try {
            // 写入模板表头
            List<BillImportRequest.BillImportItem> template = new ArrayList<>();
            BillImportRequest.BillImportItem sample = new BillImportRequest.BillImportItem();
            sample.setMeterNo("ST20230001");
            sample.setCustomerCode("CUST001");
            sample.setCustomerName("XX公司");
            sample.setBillAmount(new BigDecimal("1580.50"));
            sample.setConsumption(new BigDecimal("100.5"));
            sample.setUnitPrice(new BigDecimal("0.85"));
            sample.setRemark("示例数据");
            template.add(sample);

            EasyExcel.write(response.getOutputStream(), BillImportRequest.BillImportItem.class)
                    .excelType(ExcelTypeEnum.XLSX)
                    .sheet("批量导入模板")
                    .doWrite(template);
        } catch (IOException e) {
            log.error("[模板下载] 失败", e);
            throw new RuntimeException("模板下载失败", e);
        }
        log.info("[模板下载] 批量导入模板已下载");
    }

    // ──────────────────────── 缴费轨迹 ────────────────────────

    @Override
    public IPage<BillingTrackVO> listBillingTracks(BillingTrackPageRequest request) {
        LambdaQueryWrapper<AccountFlow> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AccountFlow::getIsDeleted, 0);

        // 权限隔离
        Long effectiveCustomerId = resolveCustomerId(request.getCustomerId());
        if (effectiveCustomerId != null) {
            wrapper.eq(AccountFlow::getCustomerId, effectiveCustomerId);
        }

        // 筛选条件
        if (request.getMeterId() != null) {
            wrapper.eq(AccountFlow::getMeterId, request.getMeterId());
        }
        if (request.getTradeType() != null) {
            wrapper.eq(AccountFlow::getTradeType, request.getTradeType());
        }
        if (request.getStartTime() != null) {
            wrapper.ge(AccountFlow::getOperateTime, request.getStartTime());
        }
        if (request.getEndTime() != null) {
            wrapper.le(AccountFlow::getOperateTime, request.getEndTime());
        }
        if (StringUtils.hasText(request.getKeyword())) {
            wrapper.and(w -> w
                    .like(AccountFlow::getFlowNo, request.getKeyword())
                    .or()
                    .like(AccountFlow::getOperatorName, request.getKeyword()));
        }

        wrapper.orderByDesc(AccountFlow::getOperateTime);

        int pageNum = request.getPageNum() != null ? request.getPageNum() : 1;
        int pageSize = request.getPageSize() != null ? request.getPageSize() : 20;
        Page<AccountFlow> page = new Page<>(pageNum, pageSize);
        IPage<AccountFlow> flowPage = accountFlowMapper.selectPage(page, wrapper);

        return flowPage.convert(this::toBillingTrackVO);
    }

    @Override
    public void exportBillingTracks(HttpServletResponse response, BillingTrackPageRequest request) {
        // 拉取全量数据（不分页）
        request.setPageNum(1);
        request.setPageSize(Integer.MAX_VALUE);
        IPage<BillingTrackVO> page = listBillingTracks(request);

        setExcelResponse(response, "缴费轨迹导出.xlsx");
        try {
            EasyExcel.write(response.getOutputStream(), BillingTrackVO.class)
                    .excelType(ExcelTypeEnum.XLSX)
                    .sheet("缴费轨迹")
                    .doWrite(page.getRecords());
        } catch (IOException e) {
            log.error("[缴费轨迹导出] 导出失败", e);
            throw new RuntimeException("Excel 导出失败", e);
        }
        log.info("[缴费轨迹导出] 导出完成，共 {} 条记录", page.getRecords().size());
    }

    // ──────────────────────── 私有方法 ────────────────────────

    /**
     * 构建单条即时账单 VO。
     */
    private InstantBillVO buildInstantBillVO(Meter meter) {
        Long meterId = meter.getId();

        // 获取最新读数（当前读数）
        MeterReading latestReading = meterReadingMapper.getLatestByMeterId(meterId);
        BigDecimal currentReading = BigDecimal.ZERO;
        if (latestReading != null) {
            currentReading = latestReading.getKwh() != null
                    ? latestReading.getKwh()
                    : (latestReading.getReadVal() != null ? latestReading.getReadVal() : BigDecimal.ZERO);
        }

        // 获取上次结算读数
        BigDecimal lastSettledReading = meter.getLastSettledReading() != null
                ? meter.getLastSettledReading()
                : BigDecimal.ZERO;

        // BigDecimal 差值计算
        BigDecimal consumption = currentReading.subtract(lastSettledReading);
        if (consumption.compareTo(BigDecimal.ZERO) < 0) {
            log.warn("[即时账单] meterId={} 当前读数小于上次结算读数，可能换表。current={}, last={}",
                    meterId, currentReading, lastSettledReading);
            consumption = BigDecimal.ZERO;
        }

        // 获取倍率和单价
        // CT变比用于电表计费折算；旧 bei 字段仅作历史兼容回退。PT不参与计费。
        BigDecimal multiplier = meter.getCtRatio() != null
                ? meter.getCtRatio() : BigDecimal.valueOf(meter.getBei() != null ? meter.getBei() : 1);
        BigDecimal unitPrice = resolveUnitPrice(meter);

        // 账单金额 = 消耗用量 × 倍率 × 单价
        BigDecimal billAmount = consumption
                .multiply(multiplier)
                .multiply(unitPrice)
                .setScale(2, RoundingMode.HALF_UP);

        // 查询商户余额
        BigDecimal currentBalance = BigDecimal.ZERO;
        String customerName = null;
        Long customerId = meter.getCustomerId();
        if (customerId != null) {
            Customer customer = customerMapper.selectById(customerId);
            if (customer != null) {
                currentBalance = customer.getBalance();
                customerName = customer.getCustomerName();
            }
        }

        InstantBillVO vo = new InstantBillVO();
        vo.setMeterId(meterId);
        vo.setMeterNo(meter.getStealNo() != null ? meter.getStealNo()
                : (meter.getDbNo() != null ? meter.getDbNo() : meter.getMeteraddress()));
        vo.setCustomerId(customerId);
        vo.setCustomerName(customerName);
        vo.setCurrentReading(currentReading);
        vo.setLastSettledReading(lastSettledReading);
        vo.setConsumption(consumption);
        vo.setMultiplier(multiplier.intValue());
        vo.setUnitPrice(unitPrice);
        vo.setBillAmount(billAmount);
        vo.setCurrentBalance(currentBalance);
        vo.setCalculateTime(LocalDateTime.now());

        return vo;
    }

    /**
     * 解析单价。
     */
    private BigDecimal resolveUnitPrice(Meter meter) {
        BigDecimal unitPrice = BigDecimal.ZERO;
        if (meter.getPriceContent() != null) {
            try {
                unitPrice = new BigDecimal(meter.getPriceContent().replaceAll("[^0-9.]", ""));
            } catch (NumberFormatException e) {
                unitPrice = BigDecimal.valueOf(0.85);
            }
        }
        if (unitPrice.compareTo(BigDecimal.ZERO) == 0 && meter.getPlusPrice() != null) {
            unitPrice = meter.getPlusPrice().compareTo(BigDecimal.ZERO) > 0
                    ? meter.getPlusPrice() : BigDecimal.valueOf(0.85);
        }
        if (unitPrice.compareTo(BigDecimal.ZERO) == 0) {
            unitPrice = BigDecimal.valueOf(0.85);
        }
        return unitPrice;
    }

    /**
     * 转换为列表 VO。
     */
    private InstantBillListVO toListVO(Meter meter, InstantBillVO bill) {
        InstantBillListVO vo = new InstantBillListVO();
        vo.setMeterId(bill.getMeterId());
        vo.setMeterName(meter.getName());
        vo.setMeterNo(bill.getMeterNo());
        vo.setCustomerId(bill.getCustomerId());
        vo.setCustomerName(bill.getCustomerName());
        vo.setCurrentReading(bill.getCurrentReading());
        vo.setLastSettledReading(bill.getLastSettledReading());
        vo.setConsumption(bill.getConsumption());
        vo.setMultiplier(bill.getMultiplier());
        vo.setUnitPrice(bill.getUnitPrice());
        vo.setBillAmount(bill.getBillAmount());
        vo.setCurrentBalance(bill.getCurrentBalance());
        vo.setMeterType(meter.getMeterType());
        vo.setStatus(meter.getStatus());
        vo.setCalculateTime(bill.getCalculateTime());
        return vo;
    }

    /**
     * 转换为缴费轨迹 VO。
     */
    private BillingTrackVO toBillingTrackVO(AccountFlow flow) {
        BillingTrackVO vo = new BillingTrackVO();
        vo.setId(flow.getId());
        vo.setFlowNo(flow.getFlowNo());
        vo.setBalanceBefore(flow.getBalanceBefore());
        vo.setChangeAmount(flow.getChangeAmount());
        vo.setBalanceAfter(flow.getBalanceAfter());
        vo.setTradeType(flow.getTradeType());
        vo.setTradeTypeName(tradeTypeLabel(flow.getTradeType()));
        vo.setBizNo(flow.getBizNo());
        vo.setOperatorName(flow.getOperatorName());
        vo.setOperateTime(flow.getOperateTime());
        vo.setRemark(flow.getRemark());
        return vo;
    }

    private String tradeTypeLabel(Integer tradeType) {
        if (tradeType == null) return "未知";
        return switch (tradeType) {
            case 1 -> "充值";
            case 2 -> "消费扣费";
            case 3 -> "退费";
            case 4 -> "调账";
            case 5 -> "人工缴费";
            case 6 -> "余额清零";
            case 7 -> "公共分摊";
            default -> "其他(" + tradeType + ")";
        };
    }

    /**
     * 权限隔离：解析目标商户 ID。
     */
    private Long resolveCustomerId(Long requestedCustomerId) {
        if (SecurityUserContext.isAdmin()) {
            return requestedCustomerId;
        }
        Long myCustomerId = SecurityUserContext.getCurrentCustomerId();
        if (myCustomerId == null) {
            log.warn("[权限拒绝] 非 ADMIN 且未关联商户的用户尝试访问物业账单数据");
            throw new BusinessException(403, "无权限：未关联商户");
        }
        return myCustomerId;
    }

    /**
     * 按表号查找表计。
     */
    private Meter findMeterByNo(String meterNo) {
        if (!StringUtils.hasText(meterNo)) return null;
        LambdaQueryWrapper<Meter> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Meter::getIsDeleted, 0)
                .and(w -> w.eq(Meter::getStealNo, meterNo)
                        .or().eq(Meter::getDbNo, meterNo)
                        .or().eq(Meter::getMeteraddress, meterNo));
        return meterMapper.selectOne(wrapper);
    }

    private String generateBizNo(String prefix) {
        String ts = String.valueOf(System.currentTimeMillis());
        String shortTs = ts.substring(ts.length() - 8);
        String shortUuid = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        return prefix + shortTs + shortUuid;
    }

    private void setExcelResponse(HttpServletResponse response, String fileName) {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("UTF-8");
        String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8)
                .replaceAll("\\+", "%20");
        response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + encodedFileName);
        response.setHeader("Access-Control-Expose-Headers", "Content-Disposition");
    }
}
