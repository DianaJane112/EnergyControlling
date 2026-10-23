package com.competition.energycontrollerplatform.miniapp.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.competition.energycontrollerplatform.common.BusinessException;
import com.competition.energycontrollerplatform.miniapp.dto.RechargeHistoryVO;
import com.competition.energycontrollerplatform.miniapp.dto.RechargeSubmitRequest;
import com.competition.energycontrollerplatform.entity.Bill;
import com.competition.energycontrollerplatform.entity.Customer;
import com.competition.energycontrollerplatform.entity.Meter;
import com.competition.energycontrollerplatform.miniapp.entity.RechargeRequest;
import com.competition.energycontrollerplatform.mapper.BillMapper;
import com.competition.energycontrollerplatform.mapper.CustomerMapper;
import com.competition.energycontrollerplatform.mapper.MeterMapper;
import com.competition.energycontrollerplatform.miniapp.mapper.RechargeRequestMapper;
import com.competition.energycontrollerplatform.miniapp.service.MiniAppRechargeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 小程序充值申请服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MiniAppRechargeServiceImpl extends ServiceImpl<RechargeRequestMapper, RechargeRequest>
        implements MiniAppRechargeService {

    private final MeterMapper meterMapper;
    private final BillMapper billMapper;
    private final CustomerMapper customerMapper;

    @Override
    @Transactional
    public String submitRecharge(Long userId, Long customerId, RechargeSubmitRequest request) {
        // 1. 校验表计归属
        Meter meter = meterMapper.selectById(request.getMeterId());
        if (meter == null) {
            throw new BusinessException(404, "表计不存在");
        }
        if (!customerId.equals(meter.getCustomerId())) {
            throw new BusinessException(403, "该表计不属于当前商户，无法充值");
        }

        // 2. 校验金额
        if (request.getAmount().compareTo(java.math.BigDecimal.ZERO) <= 0) {
            throw new BusinessException("充值金额必须大于0");
        }

        // 3. 生成申请编号
        String requestNo = generateRequestNo();

        // 4. 创建充值申请记录
        RechargeRequest rechargeRequest = new RechargeRequest();
        rechargeRequest.setRequestNo(requestNo);
        rechargeRequest.setUserId(userId);
        rechargeRequest.setCustomerId(customerId);
        rechargeRequest.setMeterId(request.getMeterId());
        rechargeRequest.setAmount(request.getAmount());
        rechargeRequest.setPayMethod(request.getPayMethod() != null ? request.getPayMethod() : "VIRTUAL");
        rechargeRequest.setAuditStatus("PENDING");

        save(rechargeRequest);

        // ⑤ 同步创建即时账单（payment_status = "待审核"）
        Customer customer = customerMapper.selectById(customerId);
        Bill bill = new Bill();
        bill.setBillNo("CZ" + requestNo.substring(3));
        bill.setBillName("小程序充值 - " + (meter.getName() != null ? meter.getName() : meter.getDbNo()));
        bill.setRechargeRequestId(rechargeRequest.getId());
        bill.setMeterId(request.getMeterId());
        bill.setMeterNo(meter.getDbNo());
        bill.setMerchantName(customer != null ? customer.getCustomerName() : "未知商户");
        bill.setReceivedAmount(request.getAmount());
        bill.setPendingAmount(java.math.BigDecimal.ZERO);
        bill.setPaymentStatus("待审核");
        bill.setSupportOnlinePayment(1);
        bill.setDescription("小程序用户充值申请，待管理员审核");
        bill.setBillCreateTime(LocalDateTime.now());
        billMapper.insert(bill);
        log.info("即时账单已生成: billNo={}, rechargeRequestId={}", bill.getBillNo(), rechargeRequest.getId());

        log.info("充值申请已提交: requestNo={}, userId={}, meterId={}, amount={}",
                requestNo, userId, request.getMeterId(), request.getAmount());

        return requestNo;
    }

    @Override
    public IPage<RechargeHistoryVO> getRechargeHistory(Long userId, Integer pageNum, Integer pageSize, String auditStatus) {
        Page<RechargeRequest> page = new Page<>(pageNum, pageSize);

        LambdaQueryWrapper<RechargeRequest> wrapper = new LambdaQueryWrapper<RechargeRequest>()
                .eq(RechargeRequest::getUserId, userId)
                .orderByDesc(RechargeRequest::getCreateTime);

        if (auditStatus != null && !auditStatus.isBlank()) {
            wrapper.eq(RechargeRequest::getAuditStatus, auditStatus.toUpperCase());
        }

        IPage<RechargeRequest> resultPage = page(page, wrapper);

        // 批量预加载表计，避免 N+1
        Map<Long, Meter> meterMap = buildMeterMap(resultPage.getRecords());
        List<RechargeHistoryVO> records = resultPage.getRecords().stream()
                .map(r -> toHistoryVO(r, meterMap))
                .collect(Collectors.toList());

        IPage<RechargeHistoryVO> voPage = new Page<>(pageNum, pageSize, resultPage.getTotal());
        voPage.setRecords(records);
        return voPage;
    }

    @Override
    public RechargeHistoryVO getRechargeDetail(Long requestId, Long userId) {
        RechargeRequest request = getById(requestId);
        if (request == null) {
            throw new BusinessException(404, "充值申请不存在");
        }
        if (!userId.equals(request.getUserId())) {
            throw new BusinessException(403, "无权查看该充值申请");
        }
        return toHistoryVO(request);
    }

    // ────────────────── 私有方法 ──────────────────

    /**
     * 生成申请编号: MPR + 时间戳后8位 + UUID前8位
     */
    private String generateRequestNo() {
        String timestamp = String.valueOf(System.currentTimeMillis());
        String ts8 = timestamp.substring(timestamp.length() - 8);
        String uuid8 = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        return "MPR" + ts8 + uuid8;
    }

    /**
     * 实体 → VO 转换（单条查询用）。
     */
    private RechargeHistoryVO toHistoryVO(RechargeRequest req) {
        Meter meter = req.getMeterId() != null ? meterMapper.selectById(req.getMeterId()) : null;
        return toHistoryVO(req, meter != null ? Map.of(meter.getId(), meter) : Map.of());
    }

    /**
     * 实体 → VO 转换（批量查询用，从预加载 Map 取值）。
     */
    private RechargeHistoryVO toHistoryVO(RechargeRequest req, Map<Long, Meter> meterMap) {
        Meter meter = meterMap.get(req.getMeterId());
        String meterNo = meter != null ? meter.getDbNo() : "未知";
        String meterName = meter != null ? meter.getName() : "未知";
        String meterType = (meter != null && meter.getMeterType() != null && meter.getMeterType() == 1)
                ? "ELECTRIC" : "WATER";

        return RechargeHistoryVO.builder()
                .id(req.getId())
                .requestNo(req.getRequestNo())
                .meterNo(meterNo)
                .meterName(meterName)
                .meterType(meterType)
                .amount(req.getAmount())
                .payMethod(req.getPayMethod())
                .auditStatus(req.getAuditStatus())
                .auditRemark(req.getAuditRemark())
                .relatedOrderNo(req.getRelatedOrderNo())
                .createTime(req.getCreateTime())
                .auditTime(req.getAuditTime())
                .build();
    }

    private Map<Long, Meter> buildMeterMap(List<RechargeRequest> entities) {
        Set<Long> ids = entities.stream().map(RechargeRequest::getMeterId)
                .filter(id -> id != null).collect(Collectors.toSet());
        if (ids.isEmpty()) return Map.of();
        return meterMapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(Meter::getId, m -> m, (a, b) -> a));
    }
}
