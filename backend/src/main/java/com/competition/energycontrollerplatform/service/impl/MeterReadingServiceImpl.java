package com.competition.energycontrollerplatform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.competition.energycontrollerplatform.common.BusinessException;
import com.competition.energycontrollerplatform.dto.CurvePointVO;
import com.competition.energycontrollerplatform.dto.MeterReadingPageRequest;
import com.competition.energycontrollerplatform.dto.MeterReadingVO;
import com.competition.energycontrollerplatform.entity.Meter;
import com.competition.energycontrollerplatform.entity.MeterReading;
import com.competition.energycontrollerplatform.mapper.MeterMapper;
import com.competition.energycontrollerplatform.mapper.MeterReadingMapper;
import com.competition.energycontrollerplatform.security.SecurityUserContext;
import com.competition.energycontrollerplatform.service.MeterReadingService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MeterReadingServiceImpl implements MeterReadingService {

    private final MeterReadingMapper meterReadingMapper;
    private final MeterMapper meterMapper;

    @Override
    public IPage<MeterReadingVO> listPage(MeterReadingPageRequest request) {
        if (request.getMeterId() == null) {
            throw BusinessException.of("表计ID不能为空");
        }

        // 权限校验：非 ADMIN 只能查看自己商户表计的读数
        checkMeterAccess(request.getMeterId());

        LambdaQueryWrapper<MeterReading> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MeterReading::getMeterId, request.getMeterId());

        if (request.getStartTime() != null) {
            wrapper.ge(MeterReading::getTimestamp, toLocalDateTime(request.getStartTime()));
        }
        if (request.getEndTime() != null) {
            wrapper.le(MeterReading::getTimestamp, toLocalDateTime(request.getEndTime()));
        }

        wrapper.orderByDesc(MeterReading::getTimestamp);

        Page<MeterReading> page = new Page<>(request.getPageNum(), request.getPageSize());
        IPage<MeterReading> result = meterReadingMapper.selectPage(page, wrapper);
        return result.convert(this::toVO);
    }

    @Override
    public List<CurvePointVO> getCurve(Long meterId) {
        if (meterId == null) {
            throw BusinessException.of("表计ID不能为空");
        }

        // 权限校验
        checkMeterAccess(meterId);

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime twentyFourHoursAgo = now.minusHours(24);

        LambdaQueryWrapper<MeterReading> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MeterReading::getMeterId, meterId)
               .ge(MeterReading::getTimestamp, twentyFourHoursAgo)
               .le(MeterReading::getTimestamp, now)
               .orderByAsc(MeterReading::getTimestamp);

        List<MeterReading> readings = meterReadingMapper.selectList(wrapper);

        return readings.stream().map(r -> {
            CurvePointVO vo = new CurvePointVO();
            vo.setTimestamp(toEpochMillis(r.getTimestamp()));
            vo.setVoltage(r.getVoltA());
            vo.setVoltageB(r.getVoltB());
            vo.setVoltageC(r.getVoltC());
            vo.setCurrent(r.getAmpA());
            vo.setCurrentB(r.getAmpB());
            vo.setCurrentC(r.getAmpC());
            vo.setPower(r.getAct());
            vo.setKwh(r.getKwh());
            return vo;
        }).collect(Collectors.toList());
    }

    /**
     * 校验当前用户是否有权访问指定表计的数据。
     * <p>
     * ADMIN 可查看任意表计；非 ADMIN 仅可查看属于自己商户的表计。
     */
    private void checkMeterAccess(Long meterId) {
        if (SecurityUserContext.isAdmin()) {
            return;
        }
        Long myCustomerId = SecurityUserContext.getCurrentCustomerId();
        if (myCustomerId == null) {
            throw BusinessException.of(403, "无权限查看表计读数");
        }
        Meter meter = meterMapper.selectById(meterId);
        if (meter == null) {
            throw BusinessException.of("表计不存在");
        }
        if (!myCustomerId.equals(meter.getCustomerId())) {
            throw BusinessException.of(403, "无权查看该表计的读数");
        }
    }

    private MeterReadingVO toVO(MeterReading r) {
        MeterReadingVO vo = new MeterReadingVO();
        BeanUtils.copyProperties(r, vo);
        vo.setTimestamp(toEpochMillis(r.getTimestamp()));
        return vo;
    }

    private LocalDateTime toLocalDateTime(Long epochMillis) {
        return Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).toLocalDateTime();
    }

    private Long toEpochMillis(LocalDateTime value) {
        return value == null ? null : value.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }
}
