package com.competition.energycontrollerplatform.service.impl;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.support.ExcelTypeEnum;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.competition.energycontrollerplatform.dto.BatchOperationResponse;
import com.competition.energycontrollerplatform.dto.BillCreateRequest;
import com.competition.energycontrollerplatform.dto.BillExportVO;
import com.competition.energycontrollerplatform.dto.BillPageRequest;
import com.competition.energycontrollerplatform.entity.Bill;
import com.competition.energycontrollerplatform.entity.BillCategory;
import com.competition.energycontrollerplatform.mapper.BillCategoryMapper;
import com.competition.energycontrollerplatform.mapper.BillMapper;
import com.competition.energycontrollerplatform.security.SecurityUserContext;
import com.competition.energycontrollerplatform.service.BillCategoryService;
import com.competition.energycontrollerplatform.service.BillService;
import com.competition.energycontrollerplatform.service.RechargeAuditService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 账单记录服务实现。
 */
@Service
@RequiredArgsConstructor
public class BillServiceImpl implements BillService {

    private static final Logger log = LoggerFactory.getLogger(BillServiceImpl.class);
    private static final DateTimeFormatter FILE_DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    private final BillMapper billMapper;
    private final BillCategoryMapper billCategoryMapper;
    private final BillCategoryService billCategoryService;
    private final RechargeAuditService rechargeAuditService;

    // ── 分页列表 ──────────────────────────────────────────────────────────

    @Override
    public IPage<Bill> listPage(BillPageRequest request) {
        int pageNum = request.getPageNum() != null ? request.getPageNum() : 1;
        int pageSize = request.getPageSize() != null ? request.getPageSize() : 10;
        Page<Bill> page = new Page<>(pageNum, pageSize);

        String merchantName = trim(request.getMerchantName());
        String meterNo = trim(request.getMeterNo());
        String virtualMerchantName = trim(request.getVirtualMerchantName());
        String virtualMeterNo = trim(request.getVirtualMeterNo());
        String paymentStatus = trim(request.getPaymentStatus());

        LambdaQueryWrapper<Bill> wrapper = new LambdaQueryWrapper<>();

        if (StringUtils.hasText(merchantName))
            wrapper.like(Bill::getMerchantName, merchantName);
        if (StringUtils.hasText(meterNo))
            wrapper.like(Bill::getMeterNo, meterNo);
        if (StringUtils.hasText(virtualMerchantName))
            wrapper.like(Bill::getVirtualMerchantName, virtualMerchantName);
        if (StringUtils.hasText(virtualMeterNo))
            wrapper.like(Bill::getVirtualMeterNo, virtualMeterNo);
        if (request.getCategoryId() != null)
            wrapper.eq(Bill::getCategoryId, request.getCategoryId());
        if (StringUtils.hasText(paymentStatus))
            wrapper.eq(Bill::getPaymentStatus, paymentStatus);
        if (StringUtils.hasText(request.getBillCreateStart()))
            wrapper.ge(Bill::getBillCreateTime, request.getBillCreateStart());
        if (StringUtils.hasText(request.getBillCreateEnd()))
            wrapper.le(Bill::getBillCreateTime, request.getBillCreateEnd() + " 23:59:59");
        if (StringUtils.hasText(request.getPayStart()))
            wrapper.ge(Bill::getActualPaymentTime, request.getPayStart());
        if (StringUtils.hasText(request.getPayEnd()))
            wrapper.le(Bill::getActualPaymentTime, request.getPayEnd() + " 23:59:59");

        wrapper.orderByDesc(Bill::getBillCreateTime);

        return billMapper.selectPage(page, wrapper);
    }

    // ── 详情 ──────────────────────────────────────────────────────────────

    @Override
    public Bill getById(Long id) {
        Bill bill = billMapper.selectById(id);
        if (bill == null || bill.getIsDeleted() == 1) {
            return null;
        }
        return bill;
    }

    // ── 新增 ──────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public Bill create(BillCreateRequest request) {
        Bill bill = new Bill();
        String now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        bill.setBillNo("ZD" + now + String.format("%04d", (int) (Math.random() * 10000)));

        fillBillFromRequest(bill, request);

        // 填入分类名称
        if (request.getCategoryId() != null) {
            BillCategory cat = billCategoryService.getById(request.getCategoryId());
            if (cat != null) bill.setCategoryName(cat.getCategoryName());
        }

        billMapper.insert(bill);
        return bill;
    }

    // ── 批量新增 ──────────────────────────────────────────────────────────

    @Override
    @Transactional
    public BatchOperationResponse batchCreate(List<BillCreateRequest> requests) {
        if (requests == null || requests.isEmpty()) {
            return new BatchOperationResponse(0, 0, 0, "请求列表不能为空");
        }
        // 批量预加载票据类别，避免 N+1
        Map<Long, String> categoryNameMap = buildCategoryNameMap(requests);
        int success = 0, failed = 0;
        for (BillCreateRequest req : requests) {
            try {
                Bill bill = new Bill();
                String nowStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"));
                bill.setBillNo("ZD" + nowStr + String.format("%03d", success + 1));

                fillBillFromRequest(bill, req);

                if (req.getCategoryId() != null) {
                    bill.setCategoryName(categoryNameMap.get(req.getCategoryId()));
                }
                billMapper.insert(bill);
                success++;
            } catch (Exception e) {
                failed++;
            }
        }
        return new BatchOperationResponse(
                requests.size(), success, failed,
                "批量新增完成：成功 " + success + " 条" + (failed > 0 ? "，失败 " + failed + " 条" : ""));
    }

    // ── 编辑 ──────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public Bill update(Long id, BillCreateRequest request) {
        Bill bill = billMapper.selectById(id);
        if (bill == null || bill.getIsDeleted() == 1) {
            return null;
        }

        // 保存旧状态，供钩子判断
        final String oldPaymentStatus = bill.getPaymentStatus();

        fillBillFromRequest(bill, request);

        if (request.getCategoryId() != null) {
            BillCategory cat = billCategoryService.getById(request.getCategoryId());
            if (cat != null) bill.setCategoryName(cat.getCategoryName());
        }

        // 钩子：支付状态从"待审核"→"已支付"时自动执行充值到账
        String newStatus = request.getPaymentStatus() != null ? request.getPaymentStatus() : "未支付";
        if ("待审核".equals(oldPaymentStatus) && "已支付".equals(newStatus)
                && bill.getRechargeRequestId() != null) {
            rechargeAuditService.approve(
                    bill.getRechargeRequestId(),
                    SecurityUserContext.getCurrentUserId(),
                    "管理员在即时账单中审核通过"
            );
            bill.setActualPaymentTime(LocalDateTime.now());
        }

        bill.setUpdateTime(LocalDateTime.now());
        billMapper.updateById(bill);
        return bill;
    }

    // ── 删除 ──────────────────────────────────────────────────────────────

    @Override
    public void delete(Long id) {
        billMapper.logicDeleteById(id);
    }

    // ── 批量删除 ──────────────────────────────────────────────────────────

    @Override
    @Transactional
    public BatchOperationResponse batchDelete(String rawBody) {
        List<Long> ids;
        try {
            if (rawBody == null || rawBody.isBlank()) {
                throw new IllegalArgumentException("请求体不能为空");
            }
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(rawBody);
            ids = new ArrayList<>();
            // 兼容 {"ids": [1, 2, 3]}
            if (root.has("ids") && root.get("ids").isArray()) {
                for (JsonNode item : root.get("ids")) {
                    ids.add(item.asLong());
                }
            }
            // 兼容 [1, 2, 3]
            else if (root.isArray()) {
                for (JsonNode item : root) {
                    ids.add(item.asLong());
                }
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("请求体解析失败：" + e.getMessage());
        }
        if (ids.isEmpty()) {
            throw new IllegalArgumentException("ID 列表不能为空，期望格式：{\"ids\":[1,2,3]} 或 [1,2,3]");
        }
        int rows = billMapper.logicDeleteByIds(ids);
        return new BatchOperationResponse(
                ids.size(), rows, ids.size() - rows,
                "成功删除 " + rows + " 条" + (ids.size() - rows > 0 ? "，" + (ids.size() - rows) + " 条失败" : ""));
    }

    // ── 导出 ──────────────────────────────────────────────────────────────

    @Override
    public void exportList(HttpServletResponse response, BillPageRequest request) throws IOException {
        String merchantName = trim(request.getMerchantName());
        String meterNo = trim(request.getMeterNo());
        String virtualMerchantName = trim(request.getVirtualMerchantName());
        String virtualMeterNo = trim(request.getVirtualMeterNo());
        String paymentStatus = trim(request.getPaymentStatus());

        LambdaQueryWrapper<Bill> wrapper = new LambdaQueryWrapper<>();

        if (StringUtils.hasText(merchantName))
            wrapper.like(Bill::getMerchantName, merchantName);
        if (StringUtils.hasText(meterNo))
            wrapper.like(Bill::getMeterNo, meterNo);
        if (StringUtils.hasText(virtualMerchantName))
            wrapper.like(Bill::getVirtualMerchantName, virtualMerchantName);
        if (StringUtils.hasText(virtualMeterNo))
            wrapper.like(Bill::getVirtualMeterNo, virtualMeterNo);
        if (request.getCategoryId() != null)
            wrapper.eq(Bill::getCategoryId, request.getCategoryId());
        if (StringUtils.hasText(paymentStatus))
            wrapper.eq(Bill::getPaymentStatus, paymentStatus);
        if (StringUtils.hasText(request.getBillCreateStart()))
            wrapper.ge(Bill::getBillCreateTime, request.getBillCreateStart());
        if (StringUtils.hasText(request.getBillCreateEnd()))
            wrapper.le(Bill::getBillCreateTime, request.getBillCreateEnd() + " 23:59:59");
        if (StringUtils.hasText(request.getPayStart()))
            wrapper.ge(Bill::getActualPaymentTime, request.getPayStart());
        if (StringUtils.hasText(request.getPayEnd()))
            wrapper.le(Bill::getActualPaymentTime, request.getPayEnd() + " 23:59:59");

        wrapper.orderByDesc(Bill::getBillCreateTime);

        List<Bill> bills = billMapper.selectList(wrapper);

        List<BillExportVO> exportList = bills.stream().map(bill -> {
            BillExportVO vo = new BillExportVO();
            vo.setBillNo(bill.getBillNo());
            vo.setBillName(bill.getBillName());
            vo.setCategoryName(bill.getCategoryName());
            vo.setMerchantName(bill.getMerchantName());
            vo.setMeterNo(bill.getMeterNo());
            vo.setVirtualMerchantName(bill.getVirtualMerchantName());
            vo.setVirtualMeterNo(bill.getVirtualMeterNo());
            vo.setReceivedAmount(bill.getReceivedAmount());
            vo.setPendingAmount(bill.getPendingAmount());
            vo.setPaymentStatus(bill.getPaymentStatus());
            vo.setArrearsHandling(bill.getArrearsHandling());
            vo.setSupportOnlinePaymentLabel(
                    bill.getSupportOnlinePayment() != null && bill.getSupportOnlinePayment() == 1
                            ? "允许" : "不允许");
            vo.setDescription(bill.getDescription());
            vo.setBillCreateTime(bill.getBillCreateTime());
            vo.setLastPaymentTime(bill.getLastPaymentTime());
            vo.setActualPaymentTime(bill.getActualPaymentTime());
            vo.setRemark(bill.getRemark());
            return vo;
        }).toList();

        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        String fileName = URLEncoder.encode(
                "即时账单导出_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".xlsx",
                StandardCharsets.UTF_8).replace("+", "%20");
        response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + fileName);

        EasyExcel.write(response.getOutputStream(), BillExportVO.class)
                .excelType(ExcelTypeEnum.XLSX)
                .sheet("账单数据")
                .doWrite(exportList);
    }

    // ── 模板下载 ──────────────────────────────────────────────────────────

    @Override
    public void downloadTemplate(HttpServletResponse response) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        String fileName = URLEncoder.encode("账单批量导入模板.xlsx", StandardCharsets.UTF_8).replace("+", "%20");
        response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + fileName);

        List<List<String>> head = Arrays.asList(
                Arrays.asList("账单名称"), Arrays.asList("商户名"), Arrays.asList("表号"),
                Arrays.asList("虚拟表商户名"), Arrays.asList("虚拟表号"),
                Arrays.asList("收款金额"), Arrays.asList("待收金额"),
                Arrays.asList("支付状态"), Arrays.asList("欠费处理"), Arrays.asList("备注")
        );

        List<Map<String, Object>> data = List.of(new LinkedHashMap<>() {{
            put("账单名称", "2026年6月水费");
            put("商户名", "天辰超市");
            put("表号", "202309150001");
            put("虚拟表商户名", "天辰超市");
            put("虚拟表号", "V202309150001");
            put("收款金额", 1088.00);
            put("待收金额", 0);
            put("支付状态", "已支付");
            put("欠费处理", "余额扣除");
            put("备注", "示例");
        }});

        EasyExcel.write(response.getOutputStream())
                .head(head)
                .excelType(ExcelTypeEnum.XLSX)
                .sheet("账单导入模板")
                .doWrite(data);
    }

    // ── 私有辅助方法 ──────────────────────────────────────────────────────

    private Map<Long, String> buildCategoryNameMap(List<BillCreateRequest> requests) {
        java.util.Set<Long> ids = requests.stream()
                .map(BillCreateRequest::getCategoryId)
                .filter(id -> id != null)
                .collect(java.util.stream.Collectors.toSet());
        if (ids.isEmpty()) return java.util.Map.of();
        return billCategoryMapper.selectBatchIds(ids).stream()
                .collect(java.util.stream.Collectors.toMap(BillCategory::getId, BillCategory::getCategoryName, (a, b) -> a));
    }

    private void fillBillFromRequest(Bill bill, BillCreateRequest request) {
        bill.setBillName(request.getBillName());
        bill.setCategoryId(request.getCategoryId());
        bill.setMeterId(request.getMeterId());
        bill.setMeterNo(request.getMeterNo());
        bill.setVirtualMeterId(request.getVirtualMeterId());
        bill.setVirtualMeterNo(request.getVirtualMeterNo());
        bill.setMerchantName(request.getMerchantName());
        bill.setVirtualMerchantName(request.getVirtualMerchantName());
        bill.setReceivedAmount(request.getReceivedAmount() != null ? request.getReceivedAmount() : java.math.BigDecimal.ZERO);
        bill.setPendingAmount(request.getPendingAmount() != null ? request.getPendingAmount() : java.math.BigDecimal.ZERO);
        bill.setPaymentStatus(request.getPaymentStatus() != null ? request.getPaymentStatus() : "未支付");
        bill.setArrearsHandling(request.getArrearsHandling());
        bill.setSupportOnlinePayment(request.getSupportOnlinePayment() != null ? request.getSupportOnlinePayment() : 1);
        bill.setDescription(request.getDescription());
        bill.setBillCreateTime(request.getBillCreateTime() != null ? request.getBillCreateTime() : LocalDateTime.now());
        bill.setLastPaymentTime(request.getLastPaymentTime());
        bill.setActualPaymentTime(request.getActualPaymentTime());
        bill.setRemark(request.getRemark());
    }

    private String trim(String s) {
        return s != null ? s.trim() : null;
    }
}
