package com.competition.energycontrollerplatform.miniapp.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.competition.energycontrollerplatform.common.BusinessException;
import com.competition.energycontrollerplatform.common.Result;
import com.competition.energycontrollerplatform.miniapp.dto.MeterDetailVO;
import com.competition.energycontrollerplatform.miniapp.dto.ScanMeterRequest;
import com.competition.energycontrollerplatform.entity.Customer;
import com.competition.energycontrollerplatform.entity.Meter;
import com.competition.energycontrollerplatform.mapper.CustomerMapper;
import com.competition.energycontrollerplatform.mapper.MeterMapper;
import com.competition.energycontrollerplatform.mapper.VirtualMeterViewerMapper;
import com.competition.energycontrollerplatform.miniapp.security.MiniappSecurityContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

/**
 * 小程序 - 水表查询控制器。
 * <p>
 * 仅查询 meter_type=0 的水表，包含按ID、按表号、扫码三种查询方式。
 */
@RestController
@RequestMapping("/api/v1/miniapp/water-meter")
@RequiredArgsConstructor
@Tag(name = "小程序-水表查询", description = "水表详情查询、扫码查询、表号查询")
public class MiniAppWaterMeterController {

    private static final int METER_TYPE_WATER = 0;

    private final MeterMapper meterMapper;
    private final CustomerMapper customerMapper;
    private final VirtualMeterViewerMapper viewerMapper;

    @GetMapping("/{meterId}")
    @Operation(summary = "查询水表详情", description = "根据表计ID查询水表详情")
    public Result<MeterDetailVO> getDetail(@PathVariable Long meterId) {
        Long customerId = MiniappSecurityContext.getCurrentCustomerId();

        Meter meter = meterMapper.selectById(meterId);
        if (meter == null) {
            throw new BusinessException(404, "水表不存在");
        }
        if (!canView(meter, customerId)) {
            throw new BusinessException(403, "无权查看该水表");
        }
        if (meter.getMeterType() != null && meter.getMeterType() != METER_TYPE_WATER) {
            throw new BusinessException(400, "该表计不是水表，请使用电表查询接口");
        }

        return Result.success(buildMeterDetailVO(meter, meter.getCustomerId()));
    }

    @GetMapping("/by-no/{meterNo}")
    @Operation(summary = "按表号查询水表", description = "通过输入表号查询水表详情")
    public Result<MeterDetailVO> getByNo(@PathVariable String meterNo) {
        Long customerId = MiniappSecurityContext.getCurrentCustomerId();
        final String trimmedNo = meterNo.trim();

        Meter meter = meterMapper.selectOne(
                new LambdaQueryWrapper<Meter>()
                        .eq(Meter::getDbNo, trimmedNo)
                        .eq(Meter::getMeterType, METER_TYPE_WATER)
                        .eq(Meter::getIsDeleted, 0)
        );
        if (meter == null) {
            throw new BusinessException(404, "未找到表号为 " + trimmedNo + " 的水表");
        }
        if (!canView(meter, customerId)) {
            throw new BusinessException(403, "该水表不属于当前商户");
        }

        return Result.success(buildMeterDetailVO(meter, meter.getCustomerId()));
    }

    @PostMapping("/scan")
    @Operation(summary = "扫码查询水表", description = "通过扫描二维码获取水表详情")
    public Result<MeterDetailVO> scan(@Valid @RequestBody ScanMeterRequest body) {
        Long customerId = MiniappSecurityContext.getCurrentCustomerId();
        String qrContent = body.getQrContent();
        if (qrContent == null || qrContent.isBlank()) {
            throw new BusinessException("二维码内容不能为空");
        }
        qrContent = qrContent.trim();

        String meterNo = qrContent;
        if (qrContent.startsWith("METER:")) {
            meterNo = qrContent.substring(6).trim();
        }

        Meter meter = meterMapper.selectOne(
                new LambdaQueryWrapper<Meter>()
                        .eq(Meter::getDbNo, meterNo)
                        .eq(Meter::getMeterType, METER_TYPE_WATER)
                        .eq(Meter::getIsDeleted, 0)
        );
        if (meter == null) {
            throw new BusinessException(404, "未找到对应的水表，请确认二维码是否正确");
        }
        if (!canView(meter, customerId)) {
            throw new BusinessException(403, "该水表不属于当前商户");
        }

        return Result.success(buildMeterDetailVO(meter, meter.getCustomerId()));
    }

    private boolean canView(Meter meter, Long customerId) {
        if (customerId != null && customerId.equals(meter.getCustomerId())) return true;
        Long userId = MiniappSecurityContext.getCurrentUserId();
        return "VIRTUAL".equalsIgnoreCase(meter.getMeterKind()) && userId != null
                && viewerMapper.countActivePermission(meter.getId(), userId) > 0;
    }

    private MeterDetailVO buildMeterDetailVO(Meter meter, Long customerId) {
        Customer customer = customerMapper.selectById(customerId);

        return MeterDetailVO.builder()
                .meterId(meter.getId())
                .meterNo(meter.getDbNo())
                .meterType("WATER")
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
}
