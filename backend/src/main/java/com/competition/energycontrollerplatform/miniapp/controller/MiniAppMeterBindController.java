package com.competition.energycontrollerplatform.miniapp.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.competition.energycontrollerplatform.common.BusinessException;
import com.competition.energycontrollerplatform.common.QrCodeGenerator;
import com.competition.energycontrollerplatform.common.Result;
import com.competition.energycontrollerplatform.entity.Customer;
import com.competition.energycontrollerplatform.entity.Meter;
import com.competition.energycontrollerplatform.mapper.CustomerMapper;
import com.competition.energycontrollerplatform.mapper.MeterMapper;
import com.competition.energycontrollerplatform.mapper.VirtualMeterViewerMapper;
import com.competition.energycontrollerplatform.miniapp.dto.MeterDetailVO;
import com.competition.energycontrollerplatform.miniapp.dto.MeterScanResultVO;
import com.competition.energycontrollerplatform.miniapp.dto.ScanMeterRequest;
import com.competition.energycontrollerplatform.miniapp.dto.UnbindMeterRequest;
import com.competition.energycontrollerplatform.miniapp.security.MiniappSecurityContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 小程序 - 扫码绑定表计控制器。
 * <p>
 * 客户通过微信小程序扫描表计上的二维码，
 * 将未绑定的电表/水表关联到自己的商户账户下。
 * <p>
 * 绑定规则：
 * <ul>
 *   <li>表计已绑定到当前商户 → 直接返回表计详情（幂等）</li>
 *   <li>表计未绑定（customer_id IS NULL） → 绑定到当前商户</li>
 *   <li>表计已绑定到其他商户 → 返回错误，不可抢绑</li>
 * </ul>
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/miniapp/meter")
@RequiredArgsConstructor
@Tag(name = "小程序-扫码绑定表计", description = "扫码二维码自动绑定电表/水表到商户")
public class MiniAppMeterBindController {

    private final MeterMapper meterMapper;
    private final CustomerMapper customerMapper;
    private final VirtualMeterViewerMapper viewerMapper;

    @PostMapping("/scan-bind")
    @Operation(summary = "扫码绑定表计", description = "扫描表计二维码，将未绑定过的电表/水表自动关联到当前商户。二维码内容格式：METER:<type>:<dbNo>")
    @Transactional(rollbackFor = Exception.class)
    public Result<MeterDetailVO> scanBind(@Valid @RequestBody ScanMeterRequest body) {
        Long customerId = MiniappSecurityContext.getCurrentCustomerId();
        String qrContent = body.getQrContent();
        if (qrContent == null || qrContent.isBlank()) {
            throw new BusinessException("二维码内容不能为空");
        }

        // 1. 解析二维码内容：METER:<type>:<dbNo>
        String[] parsed = QrCodeGenerator.parseContent(qrContent.trim());
        if (parsed == null) {
            // 兼容旧格式：直接作为表号（不带类型前缀）
            return bindByDbNoOnly(customerId, qrContent.trim());
        }

        int meterType = Integer.parseInt(parsed[0]);
        String dbNo = parsed[1];

        // 2. 按类型和编号查找表计
        Meter meter = meterMapper.selectOne(
                new LambdaQueryWrapper<Meter>()
                        .eq(Meter::getDbNo, dbNo)
                        .eq(Meter::getMeterType, meterType)
                        .eq(Meter::getIsDeleted, 0)
        );

        if (meter == null) {
            String typeName = meterType == 1 ? "电表" : "水表";
            throw new BusinessException(404, "未找到" + typeName + "（编号：" + dbNo + "），请确认二维码是否正确");
        }

        // 3. 绑定判断
        return doBind(customerId, meter);
    }

    /**
     * 兼容旧格式：不带类型前缀，直接用表号模糊查找（优先电表，其次水表）。
     */
    private Result<MeterDetailVO> bindByDbNoOnly(Long customerId, String dbNo) {
        // 优先查电表
        Meter meter = meterMapper.selectOne(
                new LambdaQueryWrapper<Meter>()
                        .eq(Meter::getDbNo, dbNo)
                        .eq(Meter::getMeterType, 1)
                        .eq(Meter::getIsDeleted, 0)
        );
        if (meter == null) {
            // 再查水表
            meter = meterMapper.selectOne(
                    new LambdaQueryWrapper<Meter>()
                            .eq(Meter::getDbNo, dbNo)
                            .eq(Meter::getMeterType, 0)
                            .eq(Meter::getIsDeleted, 0)
            );
        }
        if (meter == null) {
            throw new BusinessException(404, "未找到表计（编号：" + dbNo + "），请确认二维码是否正确");
        }
        return doBind(customerId, meter);
    }

    /**
     * 执行绑定逻辑。
     */
    private Result<MeterDetailVO> doBind(Long customerId, Meter meter) {
        if (meter.getCustomerId() != null && meter.getCustomerId().equals(customerId)) {
            // 已绑定到当前商户，直接返回详情（幂等）
            Customer customer = customerMapper.selectById(customerId);
            return Result.success("该表计已绑定到您的账户", buildDetailVO(meter, customer));
        }

        if (meter.getCustomerId() != null) {
            // 已绑定到其他商户，不可抢绑
            throw new BusinessException(403, "该表计已被其他商户绑定，如需变更请联系管理员");
        }

        // 未绑定 → 执行绑定
        meter.setCustomerId(customerId);
        meter.setUpdateTime(LocalDateTime.now());
        meterMapper.updateById(meter);

        Customer customer = customerMapper.selectById(customerId);
        String typeName = (meter.getMeterType() != null && meter.getMeterType() == 1) ? "电表" : "水表";
        log.info("扫码绑定表计成功: customerId={}, meterId={}, dbNo={}, meterType={}",
                customerId, meter.getId(), meter.getDbNo(), meter.getMeterType());

        return Result.success(typeName + "绑定成功", buildDetailVO(meter, customer));
    }

    /**
     * 构建表计详情 VO（复用现有结构）。
     */
    private MeterDetailVO buildDetailVO(Meter meter, Customer customer) {
        return MeterDetailVO.builder()
                .meterId(meter.getId())
                .meterNo(meter.getDbNo())
                .meterType(meter.getMeterType() != null && meter.getMeterType() == 1 ? "ELECTRIC" : "WATER")
                .meterName(meter.getName())
                .customerName(customer != null ? customer.getCustomerName() : null)
                .totalUsage(meter.getLastSettledReading())
                .balance(customer != null ? customer.getBalance() : BigDecimal.ZERO)
                .lastReading(meter.getLastSettledReading())
                .lastReadingTime(meter.getLastSettledTime())
                .status(meter.getStatus() != null && meter.getStatus() == 1 ? "运行中" : "已停用")
                .onlineStatus(meter.getOnline() != null && meter.getOnline() == 1 ? "在线" : "离线")
                .address(meter.getAddress())
                .warnline(meter.getWarnline())
                .credit(meter.getCredit())
                .build();
    }

    // ──────────────── 扫码预览（只读） ────────────────

    @PostMapping("/scan-info")
    @Operation(summary = "扫码查询表计信息", description = "扫描表计二维码获取表计详情及绑定状态（只读，不执行绑定）。用于展示「可绑定」/「已绑定」/「已被他人绑定」三种状态。")
    @Transactional(readOnly = true)
    public Result<MeterScanResultVO> scanInfo(@Valid @RequestBody ScanMeterRequest body) {
        Long customerId = MiniappSecurityContext.getCurrentCustomerId();
        String qrContent = body.getQrContent();
        if (qrContent == null || qrContent.isBlank()) {
            throw new BusinessException("二维码内容不能为空");
        }

        // 解析二维码内容
        String[] parsed = QrCodeGenerator.parseContent(qrContent.trim());
        String dbNo;
        Integer meterType = null;
        if (parsed != null) {
            meterType = Integer.parseInt(parsed[0]);
            dbNo = parsed[1];
        } else {
            // 兼容旧格式：直接作为表号
            dbNo = qrContent.trim();
        }

        // 查找表计
        LambdaQueryWrapper<Meter> wrapper = new LambdaQueryWrapper<Meter>()
                .eq(Meter::getDbNo, dbNo)
                .eq(Meter::getIsDeleted, 0);
        if (meterType != null) {
            wrapper.eq(Meter::getMeterType, meterType);
        }
        Meter meter = meterMapper.selectOne(wrapper);
        if (meter == null) {
            String typeHint = meterType != null ? (meterType == 1 ? "电表" : "水表") : "表计";
            throw new BusinessException(404, "未找到" + typeHint + "（编号：" + dbNo + "），请确认二维码是否正确");
        }

        // 绑定状态
        boolean isBound = meter.getCustomerId() != null;
        boolean boundToCurrentUser = isBound && meter.getCustomerId().equals(customerId);
        String boundCustomerName = null;
        if (isBound && !boundToCurrentUser) {
            Customer boundCustomer = customerMapper.selectById(meter.getCustomerId());
            if (boundCustomer != null) {
                boundCustomerName = boundCustomer.getCustomerName();
            }
        }

        // 商户余额（仅已绑定到当前用户时展示）
        BigDecimal balance = null;
        if (boundToCurrentUser) {
            Customer currentCustomer = customerMapper.selectById(customerId);
            if (currentCustomer != null) {
                balance = currentCustomer.getBalance();
            }
        }

        MeterScanResultVO vo = MeterScanResultVO.builder()
                .meterId(meter.getId())
                .meterNo(meter.getDbNo())
                .meterType(meter.getMeterType() != null && meter.getMeterType() == 1 ? "ELECTRIC" : "WATER")
                .meterName(meter.getName())
                .address(meter.getAddress())
                .balance(balance)
                .status(meter.getStatus() != null && meter.getStatus() == 1 ? "运行中" : "已停用")
                .onlineStatus(meter.getOnline() != null && meter.getOnline() == 1 ? "在线" : "离线")
                .warnline(meter.getWarnline())
                .credit(meter.getCredit())
                .isBound(isBound)
                .boundToCurrentUser(boundToCurrentUser)
                .boundCustomerName(boundCustomerName)
                .build();

        return Result.success(vo);
    }

    // ──────────────── 我的表计列表 ────────────────

    @GetMapping("/my-meters")
    @Operation(summary = "我的绑定表计列表", description = "查询当前商户下所有已绑定的电表和水表")
    public Result<List<MeterDetailVO>> myMeters() {
        Long customerId = MiniappSecurityContext.getCurrentCustomerId();
        Long userId = MiniappSecurityContext.getCurrentUserId();
        List<Long> authorizedMeterIds = viewerMapper.selectActiveMeterIds(userId);
        LambdaQueryWrapper<Meter> wrapper = new LambdaQueryWrapper<Meter>()
                .eq(Meter::getIsDeleted, 0)
                .orderByDesc(Meter::getCreateTime);
        if (customerId != null && !authorizedMeterIds.isEmpty()) {
            wrapper.and(q -> q.eq(Meter::getCustomerId, customerId)
                    .or().in(Meter::getId, authorizedMeterIds));
        } else if (customerId != null) {
            wrapper.eq(Meter::getCustomerId, customerId);
        } else if (!authorizedMeterIds.isEmpty()) {
            wrapper.in(Meter::getId, authorizedMeterIds);
        } else {
            return Result.success(List.of());
        }
        List<Meter> meters = meterMapper.selectList(wrapper);
        List<MeterDetailVO> result = meters.stream()
                .map(m -> buildDetailVO(m, customerMapper.selectById(m.getCustomerId())))
                .toList();
        return Result.success(result);
    }

    // ──────────────── 解绑表计 ────────────────

    @PostMapping("/unbind")
    @Operation(summary = "解绑表计", description = "商户自行解绑已关联的表计。仅可解绑本商户自己的表计。")
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> unbind(@Valid @RequestBody UnbindMeterRequest request) {
        Long customerId = MiniappSecurityContext.getCurrentCustomerId();
        Meter meter = meterMapper.selectById(request.getMeterId());
        if (meter == null) {
            throw new BusinessException(404, "表计不存在");
        }
        if (meter.getCustomerId() == null) {
            // 已解绑，幂等返回
            return Result.success("该表计当前未绑定任何商户");
        }
        if (!customerId.equals(meter.getCustomerId())) {
            throw new BusinessException(403, "该表计不属于当前商户，无法解绑");
        }
        meter.setCustomerId(null);
        meter.setUpdateTime(LocalDateTime.now());
        meterMapper.updateById(meter);
        log.info("[小程序解绑] customerId={}, meterId={}, dbNo={}", customerId, meter.getId(), meter.getDbNo());
        return Result.success("解绑成功");
    }
}
