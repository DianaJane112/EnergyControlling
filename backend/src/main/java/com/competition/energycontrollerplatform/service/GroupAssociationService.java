package com.competition.energycontrollerplatform.service;

import com.competition.energycontrollerplatform.common.BusinessException;
import com.competition.energycontrollerplatform.entity.MeterGroup;
import com.competition.energycontrollerplatform.mapper.MeterGroupMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayDeque;
import java.util.Deque;

@Service
@RequiredArgsConstructor
public class GroupAssociationService {
    private final MeterGroupMapper meterGroupMapper;

    public MeterGroup requireLinkable(Long groupId, Long merchantId) {
        if (groupId == null) return null;
        MeterGroup group = meterGroupMapper.selectById(groupId);
        if (group == null || Integer.valueOf(1).equals(group.getIsDeleted())) {
            throw BusinessException.of(400, "所选分组不存在或已删除");
        }
        if (!Integer.valueOf(1).equals(group.getStatus())) {
            throw BusinessException.of(400, "所选分组已禁用，不能建立关联");
        }
        // 非 admin 也可关联任意启用分组（与「非 admin 可查看全部分组」保持一致），
        // 否则非 admin 即使能选到分组，提交虚拟表时仍会因跨商户校验被 403 拦截。
        return group;
    }

    public String buildPath(MeterGroup group) {
        if (group == null) return null;
        Deque<String> names = new ArrayDeque<>();
        MeterGroup cursor = group;
        int guard = 0;
        while (cursor != null && guard++ < 100) {
            names.addFirst(cursor.getGroupName());
            cursor = cursor.getParentId() == null ? null : meterGroupMapper.selectById(cursor.getParentId());
        }
        return String.join(" / ", names);
    }

    public GroupAssociation describe(Long groupId, Long merchantId) {
        if (groupId == null) {
            return new GroupAssociation(null, null, null, "UNASSIGNED");
        }
        MeterGroup group = meterGroupMapper.selectById(groupId);
        if (group == null || Integer.valueOf(1).equals(group.getIsDeleted())) {
            return new GroupAssociation(null, null, null, "DELETED");
        }
        // 非 admin 已可查看/关联全部分组，故不再按商户隐藏分组名（否则列表里分组名显示为空）。
        String status = Integer.valueOf(1).equals(group.getStatus()) ? "ACTIVE" : "DISABLED";
        return new GroupAssociation(group.getGroupName(), buildPath(group), group.getStatus(), status);
    }

    public record GroupAssociation(String name, String path, Integer status, String relationStatus) {}
}
