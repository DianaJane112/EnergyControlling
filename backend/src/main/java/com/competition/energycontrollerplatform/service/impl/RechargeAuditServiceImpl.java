package com.competition.energycontrollerplatform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.competition.energycontrollerplatform.common.BusinessException;
import com.competition.energycontrollerplatform.dto.RechargeAuditPageRequest;
import com.competition.energycontrollerplatform.entity.AccountFlow;
import com.competition.energycontrollerplatform.entity.Customer;
import com.competition.energycontrollerplatform.entity.Meter;
import com.competition.energycontrollerplatform.entity.RechargeOrder;
import com.competition.energycontrollerplatform.mapper.AccountFlowMapper;
import com.competition.energycontrollerplatform.mapper.CustomerMapper;
import com.competition.energycontrollerplatform.mapper.MeterMapper;
import com.competition.energycontrollerplatform.mapper.RechargeOrderMapper;
import com.competition.energycontrollerplatform.miniapp.dto.RechargeHistoryVO;
import com.competition.energycontrollerplatform.miniapp.entity.RechargeRequest;
import com.competition.energycontrollerplatform.miniapp.mapper.RechargeRequestMapper;
import com.competition.energycontrollerplatform.security.SecurityUserContext;
import com.competition.energycontrollerplatform.service.RechargeAuditService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 管理端充值审核服务实现。
 * <p>
 * 审核流程：
 * <ol>
 *   <li>小程序用户提交充值 → t_recharge_request (PENDING)</li>
 *   <li>管理员在管理后台查看待审核列表</li>
 *   <li>审核通过 → 创建 t_recharge_order + 增加 t_customer.balance + 更新状态为 APPROVED</li>
 *   <li>审核拒绝 → 记录拒绝原因，状态改为 REJECTED</li>
 * </ol>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RechargeAuditServiceImpl implements RechargeAuditService {

    private final RechargeRequestMapper rechargeRequestMapper;
    private final RechargeOrderMapper rechargeOrderMapper;
    private final CustomerMapper customerMapper;
    private final MeterMapper meterMapper;
    private final AccountFlowMapper accountFlowMapper;

    @Override
    public IPage<RechargeHistoryVO> getAuditPage(RechargeAuditPageRequest request) {
        Page<RechargeRequest> page = new Page<>(request.getPageNum(), request.getPageSize());

        LambdaQueryWrapper<RechargeRequest> wrapper = new LambdaQueryWrapper<RechargeRequest>()
                .orderByDesc(RechargeRequest::getCreateTime);

        // 按审核状态筛选（不传=全部）
        if (request.getAuditStatus() != null && !request.getAuditStatus().isBlank()) {
            wrapper.eq(RechargeRequest::getAuditStatus, request.getAuditStatus().toUpperCase());
        }

        // 按状态排序：PENDING 优先
        wrapper.orderByAsc(RechargeRequest::getAuditStatus);

        IPage<RechargeRequest> resultPage = rechargeRequestMapper.selectPage(page, wrapper);

        // 批量预加载关联数据，避免 N+1
        List<RechargeRequest> entities = resultPage.getRecords();
        Map<Long, Meter> meterMap = buildMeterMap(entities);
        Map<Long, Customer> customerMap = buildCustomerMap(entities);
        List<RechargeHistoryVO> records = entities.stream()
                .map(r -> toHistoryVO(r, meterMap, customerMap))
                .collect(Collectors.toList());

        // 关键词过滤（商户名/表号/申请编号）
        if (request.getKeyword() != null && !request.getKeyword().isBlank()) {
            String kw = request.getKeyword().toLowerCase();
            records = records.stream()
                    .filter(vo -> {
                        String meterNo = vo.getMeterNo() != null ? vo.getMeterNo().toLowerCase() : "";
                        String meterName = vo.getMeterName() != null ? vo.getMeterName().toLowerCase() : "";
                        String requestNo = vo.getRequestNo() != null ? vo.getRequestNo().toLowerCase() : "";
                        return meterNo.contains(kw) || meterName.contains(kw) || requestNo.contains(kw);
                    })
                    .collect(Collectors.toList());
        }

        IPage<RechargeHistoryVO> voPage = new Page<>(request.getPageNum(), request.getPageSize(), resultPage.getTotal());
        voPage.setRecords(records);
        return voPage;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = {"dashboard", "meter_statistics"}, allEntries = true)
    public String approve(Long requestId, Long auditUserId, String remark) {
        // ① 获取充值申请
        RechargeRequest rechargeRequest = rechargeRequestMapper.selectById(requestId);
        if (rechargeRequest == null) {
            throw new BusinessException(404, "充值申请不存在");
        }
        if (!"PENDING".equals(rechargeRequest.getAuditStatus())) {
            throw new BusinessException(400, "该申请状态为 " + rechargeRequest.getAuditStatus() + "，无法重复审核");
        }

        // ② 查询关联表计和商户
        Meter meter = meterMapper.selectById(rechargeRequest.getMeterId());
        if (meter == null) {
            throw new BusinessException(404, "关联表计不存在");
        }
        // ③ 悲观锁锁定商户行，读取精确的变动前余额
        Customer locked = customerMapper.selectByIdForUpdate(rechargeRequest.getCustomerId());
        if (locked == null) {
            throw new BusinessException(404, "关联商户不存在");
        }
        java.math.BigDecimal balanceBefore = locked.getBalance() != null
                ? locked.getBalance() : java.math.BigDecimal.ZERO;
        java.math.BigDecimal balanceAfter = balanceBefore.add(rechargeRequest.getAmount());

        // ④ 生成充值订单号
        String orderNo = generateOrderNo();

        // ⑤ 创建充值订单记录
        RechargeOrder order = new RechargeOrder();
        order.setOrderNo(orderNo);
        order.setCustomerId(rechargeRequest.getCustomerId());
        order.setMeterId(rechargeRequest.getMeterId());
        order.setDbNo(meter.getDbNo());
        order.setDbName(meter.getName());
        order.setProName(meter.getProName());
        order.setAmount(rechargeRequest.getAmount());
        order.setPayamount(rechargeRequest.getAmount());
        order.setRealamount(rechargeRequest.getAmount());
        order.setBalance(balanceAfter); // 锁保护下的精确变动后余额
        order.setCharegetype(2); // 人工充值
        order.setTradeType(2);
        order.setOperatorName(SecurityUserContext.getCurrentUsername());
        order.setUserName(locked.getCustomerName());
        order.setRemark("小程序充值审核通过" + (remark != null ? " - " + remark : ""));
        order.setCreateTime(LocalDateTime.now());
        order.setUpdateTime(LocalDateTime.now());
        order.setCreateBy(SecurityUserContext.getCurrentUsername());
        rechargeOrderMapper.insert(order);

        // ⑥ 原子增加商户余额（FOR UPDATE 保护下）
        int rows = customerMapper.addBalance(rechargeRequest.getCustomerId(), rechargeRequest.getAmount());
        if (rows == 0) {
            throw new BusinessException(500, "商户余额更新失败");
        }

        // ⑥a 写入资金流水（Dashboard 财务概览实时查询此表）
        AccountFlow flow = new AccountFlow();
        flow.setFlowNo(generateFlowNo());
        flow.setCustomerId(rechargeRequest.getCustomerId());
        flow.setMeterId(rechargeRequest.getMeterId());
        flow.setBalanceBefore(balanceBefore);
        flow.setChangeAmount(rechargeRequest.getAmount());
        flow.setBalanceAfter(balanceAfter);
        flow.setTradeType(1);  // 充值
        flow.setBizNo(orderNo);
        flow.setOperatorName(SecurityUserContext.getCurrentUsername());
        flow.setOperateTime(LocalDateTime.now());
        flow.setRemark("小程序充值审核通过" + (remark != null ? " - " + remark : ""));
        flow.setCreateTime(LocalDateTime.now());
        flow.setUpdateTime(LocalDateTime.now());
        accountFlowMapper.insert(flow);

        // ⑦ 更新审核状态
        rechargeRequestMapper.updateAuditStatus(
                requestId, "APPROVED", remark, auditUserId,
                LocalDateTime.now(), orderNo);

        log.info("[充值审核] 通过 requestId={}, orderNo={}, amount={}, operator={}",
                requestId, orderNo, rechargeRequest.getAmount(), SecurityUserContext.getCurrentUsername());

        return orderNo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = {"dashboard", "meter_statistics"}, allEntries = true)
    public void reject(Long requestId, Long auditUserId, String remark) {
        // ① 获取充值申请
        RechargeRequest rechargeRequest = rechargeRequestMapper.selectById(requestId);
        if (rechargeRequest == null) {
            throw new BusinessException(404, "充值申请不存在");
        }
        if (!"PENDING".equals(rechargeRequest.getAuditStatus())) {
            throw new BusinessException(400, "该申请状态为 " + rechargeRequest.getAuditStatus() + "，无法重复审核");
        }

        // ② 更新审核状态为拒绝
        rechargeRequestMapper.updateAuditStatus(
                requestId, "REJECTED", remark, auditUserId,
                LocalDateTime.now(), null);

        log.info("[充值审核] 拒绝 requestId={}, reason={}, operator={}",
                requestId, remark, SecurityUserContext.getCurrentUsername());
    }

    // ────────────────── 私有方法 ──────────────────

    /**
     * 生成充值订单号: RECHG + 时间戳 + UUID
     */
    private String generateOrderNo() {
        String ts = String.valueOf(System.currentTimeMillis());
        String shortTs = ts.substring(ts.length() - 8);
        String shortUuid = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        return "RECHG" + shortTs + shortUuid;
    }

    /**
     * 生成资金流水号: FLW + 时间戳 + UUID
     */
    private String generateFlowNo() {
        String ts = String.valueOf(System.currentTimeMillis());
        String shortTs = ts.substring(ts.length() - 8);
        String shortUuid = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        return "FLW" + shortTs + shortUuid;
    }

    /**
     * 实体 → VO 转换（单条查询用）。
     */
    private RechargeHistoryVO toHistoryVO(RechargeRequest req) {
        Meter meter = req.getMeterId() != null ? meterMapper.selectById(req.getMeterId()) : null;
        Customer customer = req.getCustomerId() != null ? customerMapper.selectById(req.getCustomerId()) : null;
        Map<Long, Meter> mm = meter != null ? Map.of(meter.getId(), meter) : Map.of();
        Map<Long, Customer> cm = customer != null ? Map.of(customer.getId(), customer) : Map.of();
        return toHistoryVO(req, mm, cm);
    }

    /**
     * 实体 → VO 转换（批量查询用，从预加载 Map 取值，不查 DB）。
     */
    private RechargeHistoryVO toHistoryVO(RechargeRequest req, Map<Long, Meter> meterMap, Map<Long, Customer> customerMap) {
        Meter meter = meterMap.get(req.getMeterId());
        Customer customer = customerMap.get(req.getCustomerId());
        String meterNo = meter != null ? meter.getDbNo() : "未知";
        String meterName = meter != null ? meter.getName() : "未知";
        String customerName = customer != null ? customer.getCustomerName() : "未知";
        String meterType = (meter != null && meter.getMeterType() != null && meter.getMeterType() == 1)
                ? "ELECTRIC" : "WATER";

        return RechargeHistoryVO.builder()
                .id(req.getId())
                .requestNo(req.getRequestNo())
                .meterNo(meterNo)
                .meterName(meterName + " (" + customerName + ")")
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

    private Map<Long, Customer> buildCustomerMap(List<RechargeRequest> entities) {
        Set<Long> ids = entities.stream().map(RechargeRequest::getCustomerId)
                .filter(id -> id != null).collect(Collectors.toSet());
        if (ids.isEmpty()) return Map.of();
        return customerMapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(Customer::getId, c -> c, (a, b) -> a));
    }
}
