package com.competition.energycontrollerplatform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.competition.energycontrollerplatform.common.BusinessException;
import com.competition.energycontrollerplatform.dto.*;
import com.competition.energycontrollerplatform.entity.*;
import com.competition.energycontrollerplatform.mapper.*;
import com.competition.energycontrollerplatform.miniapp.entity.MiniAppUser;
import com.competition.energycontrollerplatform.miniapp.mapper.MiniAppUserMapper;
import com.competition.energycontrollerplatform.security.SecurityUserContext;
import com.competition.energycontrollerplatform.service.VirtualMeterViewerService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VirtualMeterViewerServiceImpl implements VirtualMeterViewerService {
    private final VirtualMeterViewerMapper viewerMapper;
    private final MeterMapper meterMapper;
    private final CustomerMapper customerMapper;
    private final MiniAppUserMapper miniAppUserMapper;

    @Override
    public List<VirtualMeterViewerVO> list(Long virtualMeterId) {
        requireVirtualMeter(virtualMeterId);
        List<VirtualMeterViewer> rows = viewerMapper.selectList(new LambdaQueryWrapper<VirtualMeterViewer>()
                .eq(VirtualMeterViewer::getVirtualMeterId, virtualMeterId)
                .orderByDesc(VirtualMeterViewer::getCreateTime));
        Map<Long, MiniAppUser> users = rows.isEmpty() ? Map.of() : miniAppUserMapper
                .selectBatchIds(rows.stream().map(VirtualMeterViewer::getMiniappUserId).toList())
                .stream().collect(Collectors.toMap(MiniAppUser::getId, Function.identity(), (a, b) -> a));
        return rows.stream().map(row -> toVO(row, users.get(row.getMiniappUserId()))).toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public VirtualMeterViewerVO bind(Long virtualMeterId, VirtualMeterViewerRequest request) {
        requireVirtualMeter(virtualMeterId);
        MiniAppUser user = miniAppUserMapper.selectById(request.getMiniappUserId());
        if (user == null) throw BusinessException.of(400, "查看用户不存在");
        VirtualMeterViewer existing = viewerMapper.selectIncludingDeleted(virtualMeterId, user.getId());
        if (existing != null && !Integer.valueOf(1).equals(existing.getIsDeleted())) {
            throw BusinessException.of(400, "该用户已拥有查看权限");
        }
        VirtualMeterViewer row;
        if (existing != null) {
            viewerMapper.restore(existing.getId());
            row = viewerMapper.selectById(existing.getId());
        } else {
            row = new VirtualMeterViewer();
            row.setVirtualMeterId(virtualMeterId);
            row.setMiniappUserId(user.getId());
            row.setPermissionType("VIEW");
            row.setStatus(1);
            row.setEffectiveTime(request.getEffectiveTime() == null ? LocalDateTime.now() : request.getEffectiveTime());
            row.setExpireTime(request.getExpireTime());
            row.setRemark(request.getRemark());
            row.setCreateBy(SecurityUserContext.getCurrentUsername());
            row.setCreateTime(LocalDateTime.now());
            row.setUpdateTime(LocalDateTime.now());
            row.setIsDeleted(0);
            viewerMapper.insert(row);
        }
        return toVO(row, user);
    }

    @Override
    public void delete(Long virtualMeterId, Long viewerId) {
        requireVirtualMeter(virtualMeterId);
        VirtualMeterViewer row = requireViewer(virtualMeterId, viewerId);
        viewerMapper.deleteById(row.getId());
    }

    @Override
    public void updateStatus(Long virtualMeterId, Long viewerId, Integer status) {
        requireVirtualMeter(virtualMeterId);
        VirtualMeterViewer row = requireViewer(virtualMeterId, viewerId);
        row.setStatus(status);
        row.setUpdateTime(LocalDateTime.now());
        viewerMapper.updateById(row);
    }

    @Override
    public VirtualMeterAccountVO accountDetail(Long virtualMeterId) {
        Meter meter = requireVirtualMeter(virtualMeterId);
        Customer customer = customerMapper.selectById(meter.getCustomerId());
        if (customer == null) throw BusinessException.of(400, "虚拟表未关联有效结算账户");
        VirtualMeterAccountVO vo = new VirtualMeterAccountVO();
        vo.setVirtualMeterId(meter.getId());
        vo.setVirtualMeterNo(StringUtils.hasText(meter.getStealNo()) ? meter.getStealNo() : meter.getDbNo());
        vo.setCustomerId(customer.getId());
        vo.setCustomerCode(customer.getCustomerCode());
        vo.setCustomerName(customer.getCustomerName());
        vo.setPhone(customer.getPhone());
        vo.setBalance(customer.getBalance());
        vo.setStatus(customer.getStatus());
        return vo;
    }

    @Override
    public List<MiniAppUser> candidates(String keyword) {
        LambdaQueryWrapper<MiniAppUser> query = new LambdaQueryWrapper<MiniAppUser>()
                .eq(MiniAppUser::getStatus, 1)
                .and(StringUtils.hasText(keyword), q -> q.like(MiniAppUser::getNickname, keyword)
                        .or().like(MiniAppUser::getPhone, keyword))
                .orderByDesc(MiniAppUser::getCreateTime)
                .last("LIMIT 100");
        return miniAppUserMapper.selectList(query);
    }

    private Meter requireVirtualMeter(Long id) {
        Meter meter = meterMapper.selectById(id);
        if (meter == null || !"VIRTUAL".equalsIgnoreCase(meter.getMeterKind())) {
            throw BusinessException.of(400, "虚拟表不存在");
        }
        Long currentCustomerId = SecurityUserContext.getCurrentCustomerId();
        if (!SecurityUserContext.isAdmin() && currentCustomerId != null
                && !Objects.equals(currentCustomerId, meter.getCustomerId())) {
            throw BusinessException.of(403, "无权管理该虚拟表");
        }
        return meter;
    }

    private VirtualMeterViewer requireViewer(Long meterId, Long viewerId) {
        VirtualMeterViewer row = viewerMapper.selectById(viewerId);
        if (row == null || !Objects.equals(row.getVirtualMeterId(), meterId)) {
            throw BusinessException.of(400, "查看用户授权不存在");
        }
        return row;
    }

    private VirtualMeterViewerVO toVO(VirtualMeterViewer row, MiniAppUser user) {
        VirtualMeterViewerVO vo = new VirtualMeterViewerVO();
        vo.setId(row.getId()); vo.setVirtualMeterId(row.getVirtualMeterId());
        vo.setMiniappUserId(row.getMiniappUserId()); vo.setPermissionType(row.getPermissionType());
        vo.setStatus(row.getStatus()); vo.setEffectiveTime(row.getEffectiveTime());
        vo.setExpireTime(row.getExpireTime()); vo.setRemark(row.getRemark()); vo.setCreateTime(row.getCreateTime());
        if (user != null) { vo.setNickname(user.getNickname()); vo.setPhone(user.getPhone()); }
        return vo;
    }
}
