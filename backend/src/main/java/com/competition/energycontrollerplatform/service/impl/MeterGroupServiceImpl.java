package com.competition.energycontrollerplatform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.competition.energycontrollerplatform.common.BusinessException;
import com.competition.energycontrollerplatform.dto.MeterGroupRequest;
import com.competition.energycontrollerplatform.dto.MeterGroupVO;
import com.competition.energycontrollerplatform.entity.Customer;
import com.competition.energycontrollerplatform.entity.MeterGroup;
import com.competition.energycontrollerplatform.mapper.CustomerMapper;
import com.competition.energycontrollerplatform.mapper.MeterGroupMapper;
import com.competition.energycontrollerplatform.security.SecurityUserContext;
import com.competition.energycontrollerplatform.service.MeterGroupService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 分组管理服务实现。
 * <p>
 * <b>权限隔离策略：</b>
 * <ul>
 *   <li>ADMIN 可操作/查询所有分组，可指定任意 merchantId</li>
 *   <li>非 ADMIN 强制将 merchantId 置为当前登录用户的 customerId，
 *       且查询时仅返回属于自己的分组</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class MeterGroupServiceImpl implements MeterGroupService {

    private static final Logger log = LoggerFactory.getLogger(MeterGroupServiceImpl.class);

    private final MeterGroupMapper meterGroupMapper;
    private final CustomerMapper customerMapper;

    // ──────────────────────── 分页查询 ────────────────────────

    @Override
    public IPage<MeterGroupVO> page(int pageNum, int pageSize, String keyword, Long merchantId) {
        Long effectiveMerchantId = resolveMerchantId(merchantId);

        LambdaQueryWrapper<MeterGroup> wrapper = new LambdaQueryWrapper<>();
        if (effectiveMerchantId != null) {
            // 全局分组（merchant_id 为空）对所有商户可见；商户私有分组仍按商户隔离。
            wrapper.and(w -> w.eq(MeterGroup::getMerchantId, effectiveMerchantId)
                    .or().isNull(MeterGroup::getMerchantId));
        }
        if (keyword != null && !keyword.isBlank()) {
            wrapper.like(MeterGroup::getGroupName, keyword.trim());
        }
        wrapper.orderByDesc(MeterGroup::getCreateTime);

        Page<MeterGroup> page = new Page<>(pageNum, pageSize);
        IPage<MeterGroup> result = meterGroupMapper.selectPage(page, wrapper);

        Map<Long, Customer> customerMap = buildCustomerMapForGroups(result.getRecords());
        return result.convert(g -> toVO(g, customerMap));
    }

    // ──────────────────────── 按ID查询 ────────────────────────

    @Override
    public MeterGroupVO getById(Long id) {
        MeterGroup group = meterGroupMapper.selectById(id);
        if (group == null) {
            throw BusinessException.of(404, "分组不存在");
        }
        // 非 ADMIN 校验归属
        checkOwnership(group);
        return toVO(group);
    }

    // ──────────────────────── 创建 ────────────────────────

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MeterGroupVO create(MeterGroupRequest request) {
        Long effectiveMerchantId = resolveMerchantId(request.getMerchantId());
        if (effectiveMerchantId == null) {
            throw BusinessException.of(400, "必须指定关联商户");
        }

        // 校验商户存在
        Customer customer = customerMapper.selectById(effectiveMerchantId);
        if (customer == null) {
            throw BusinessException.of(400, "关联商户不存在");
        }

        // 校验父分组存在且属于同一商户
        if (request.getParentId() != null) {
            MeterGroup parent = meterGroupMapper.selectById(request.getParentId());
            if (parent == null || parent.getIsDeleted() != null && parent.getIsDeleted() == 1) {
                throw BusinessException.of(400, "父分组不存在或已删除");
            }
            if (!parent.getMerchantId().equals(effectiveMerchantId)) {
                throw BusinessException.of(400, "父分组不属于同一商户，无法创建子分组");
            }
        }

        ensureUniqueName(effectiveMerchantId, request.getParentId(), request.getGroupName(), null);

        MeterGroup group = new MeterGroup();
        group.setParentId(request.getParentId());
        group.setGroupName(request.getGroupName());
        group.setManageType(request.getManageType());
        group.setDimensionType(request.getDimensionType() == null ? "NORMAL" : request.getDimensionType());
        group.setDescription(request.getDescription());
        group.setMerchantId(effectiveMerchantId);
        group.setStatus(request.getStatus() != null ? request.getStatus() : 1);
        group.setRemark(request.getRemark());
        group.setCreateBy(SecurityUserContext.getCurrentUsername());
        group.setCreateTime(LocalDateTime.now());
        group.setUpdateTime(LocalDateTime.now());

        meterGroupMapper.insert(group);
        log.info("[分组管理] 创建成功 id={}, groupName={}, merchantId={}",
                group.getId(), group.getGroupName(), effectiveMerchantId);

        return toVO(group);
    }

    // ──────────────────────── 更新 ────────────────────────

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MeterGroupVO update(Long id, MeterGroupRequest request) {
        MeterGroup group = meterGroupMapper.selectById(id);
        if (group == null) {
            throw BusinessException.of(404, "分组不存在");
        }
        checkOwnership(group);

        if (request.getParentId() != null) {
            // 不允许将自己设为父分组
            if (request.getParentId().equals(id)) {
                throw BusinessException.of(400, "不能将分组自身设为父分组");
            }
            // 校验父分组存在
            MeterGroup parent = meterGroupMapper.selectById(request.getParentId());
            if (parent == null || parent.getIsDeleted() != null && parent.getIsDeleted() == 1) {
                throw BusinessException.of(400, "父分组不存在或已删除");
            }
            if (!parent.getMerchantId().equals(group.getMerchantId())) {
                throw BusinessException.of(400, "父分组不属于同一商户");
            }
            ensureNotDescendant(id, request.getParentId());
            group.setParentId(request.getParentId());
        }
        if (request.getGroupName() != null) {
            Long targetParentId = request.getParentId() != null ? request.getParentId() : group.getParentId();
            ensureUniqueName(group.getMerchantId(), targetParentId, request.getGroupName(), id);
            group.setGroupName(request.getGroupName());
        }
        if (request.getManageType() != null) {
            validateManageType(request.getManageType());
            group.setManageType(request.getManageType());
        }
        if (request.getDimensionType() != null) {
            group.setDimensionType(request.getDimensionType());
        }
        if (request.getDescription() != null) {
            group.setDescription(request.getDescription());
        }
        if (request.getStatus() != null) {
            group.setStatus(request.getStatus());
        }
        if (request.getRemark() != null) {
            group.setRemark(request.getRemark());
        }
        group.setUpdateTime(LocalDateTime.now());

        meterGroupMapper.updateById(group);
        log.info("[分组管理] 更新成功 id={}, groupName={}", id, group.getGroupName());

        return toVO(group);
    }

    // ──────────────────────── 删除 ────────────────────────

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        MeterGroup group = meterGroupMapper.selectById(id);
        if (group == null) {
            throw BusinessException.of(404, "分组不存在");
        }
        checkOwnership(group);

        // 检查是否有子分组，有子分组不允许删除
        Long childCount = meterGroupMapper.selectCount(
                new LambdaQueryWrapper<MeterGroup>()
                        .eq(MeterGroup::getParentId, id)
                        .eq(MeterGroup::getIsDeleted, 0));
        if (childCount > 0) {
            throw BusinessException.of(400, "该分组下存在 " + childCount + " 个子分组，请先删除子分组");
        }

        // 使用原生 SQL 执行逻辑删除（绕过 @TableLogic 对 updateById 的拦截）
        int affected = meterGroupMapper.logicDeleteById(id);
        if (affected == 0) {
            throw BusinessException.of(404, "分组不存在或已被删除");
        }
        log.info("[分组管理] 删除成功 id={}, groupName={}", id, group.getGroupName());
    }

    // ──────────────────────── 下拉列表 ────────────────────────

    @Override
    public List<MeterGroupVO> listByMerchant(Long merchantId) {
        // 与树形结构保持一致：非 admin 也可查看全部分组。
        Long effectiveMerchantId = SecurityUserContext.isAdmin() ? merchantId : null;

        LambdaQueryWrapper<MeterGroup> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MeterGroup::getStatus, 1);
        if (effectiveMerchantId != null) {
            // 虚拟表关联需要看到管理员创建的全局根节点及其子节点。
            wrapper.and(w -> w.eq(MeterGroup::getMerchantId, effectiveMerchantId)
                    .or().isNull(MeterGroup::getMerchantId));
        }
        wrapper.orderByAsc(MeterGroup::getGroupName);

        List<MeterGroup> groups = meterGroupMapper.selectList(wrapper);
        Map<Long, Customer> customerMap = buildCustomerMapForGroups(groups);
        List<MeterGroupVO> vos = new ArrayList<>(groups.size());
        for (MeterGroup g : groups) {
            vos.add(toVO(g, customerMap));
        }
        return vos;
    }

    // ──────────────────────── 树形结构 ────────────────────────

    @Override
    public List<MeterGroupVO> getTree(Long merchantId) {
        // 虚拟电表/水表绑定分组：非 admin 也需查看全部分组，否则无分组可选、无法提交。
        Long effectiveMerchantId = SecurityUserContext.isAdmin() ? merchantId : null;

        LambdaQueryWrapper<MeterGroup> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MeterGroup::getStatus, 1);
        if (effectiveMerchantId != null) {
            // 与下拉列表保持一致：全局分组（merchant_id 为空）及当前商户分组均可关联。
            wrapper.and(w -> w.eq(MeterGroup::getMerchantId, effectiveMerchantId)
                    .or().isNull(MeterGroup::getMerchantId));
        }
        wrapper.orderByAsc(MeterGroup::getParentId).orderByAsc(MeterGroup::getCreateTime);

        List<MeterGroup> allGroups = meterGroupMapper.selectList(wrapper);
        Map<Long, Customer> customerMap = buildCustomerMapForGroups(allGroups);
        List<MeterGroupVO> allVOs = new ArrayList<>(allGroups.size());
        for (MeterGroup g : allGroups) {
            allVOs.add(toVO(g, customerMap));
        }

        // 按 parentId 分组构建树
        Map<Long, List<MeterGroupVO>> childrenMap = allVOs.stream()
                .filter(v -> v.getParentId() != null)
                .collect(Collectors.groupingBy(MeterGroupVO::getParentId));

        List<MeterGroupVO> roots = new ArrayList<>();
        for (MeterGroupVO vo : allVOs) {
            vo.setChildren(childrenMap.getOrDefault(vo.getId(), Collections.emptyList()));
            vo.setHasChildren(!vo.getChildren().isEmpty());
            if (vo.getParentId() == null) {
                roots.add(vo);
            }
        }
        for (MeterGroupVO root : roots) fillPaths(root, null);

        return roots;
    }

    private void fillPaths(MeterGroupVO node, String parentPath) {
        node.setGroupPath(parentPath == null ? node.getGroupName() : parentPath + " / " + node.getGroupName());
        for (MeterGroupVO child : node.getChildren()) fillPaths(child, node.getGroupPath());
    }

    // ──────────────────────── 私有方法 ────────────────────────

    /**
     * 权限隔离：解析目标商户 ID。
     * <p>ADMIN 可用前端传入值；非 ADMIN 强制使用当前登录用户关联的商户 ID。</p>
     */
    private Long resolveMerchantId(Long requestedMerchantId) {
        if (SecurityUserContext.isAdmin()) {
            return requestedMerchantId; // null=查全量
        }
        Long myCustomerId = SecurityUserContext.getCurrentCustomerId();
        return myCustomerId != null ? myCustomerId : -1L;
    }

    /**
     * 校验当前用户是否有权操作该分组。
     */
    private void checkOwnership(MeterGroup group) {
        if (SecurityUserContext.isAdmin()) {
            return;
        }
        Long myCustomerId = SecurityUserContext.getCurrentCustomerId();
        if (myCustomerId == null || !myCustomerId.equals(group.getMerchantId())) {
            throw BusinessException.of(403, "无权操作该分组");
        }
    }

    /**
     * Entity → VO 转换（单条查询用）。
     */
    private void ensureUniqueName(Long merchantId, Long parentId, String groupName, Long excludeId) {
        LambdaQueryWrapper<MeterGroup> wrapper = new LambdaQueryWrapper<MeterGroup>()
                .eq(MeterGroup::getMerchantId, merchantId)
                .eq(MeterGroup::getGroupName, groupName.trim())
                .eq(MeterGroup::getIsDeleted, 0);
        if (parentId == null) wrapper.isNull(MeterGroup::getParentId);
        else wrapper.eq(MeterGroup::getParentId, parentId);
        if (excludeId != null) wrapper.ne(MeterGroup::getId, excludeId);
        if (meterGroupMapper.selectCount(wrapper) > 0) {
            throw BusinessException.of(400, "同一父分组下不能存在同名分组");
        }
    }

    private void ensureNotDescendant(Long groupId, Long parentId) {
        Long cursor = parentId;
        Set<Long> visited = new java.util.HashSet<>();
        while (cursor != null && visited.add(cursor)) {
            if (cursor.equals(groupId)) {
                throw BusinessException.of(400, "不能将分组移动到自身或子孙分组下");
            }
            MeterGroup parent = meterGroupMapper.selectById(cursor);
            cursor = parent == null ? null : parent.getParentId();
        }
    }

    private MeterGroupVO toVO(MeterGroup group) {
        Map<Long, Customer> cm = Map.of();
        if (group.getMerchantId() != null) {
            Customer c = customerMapper.selectById(group.getMerchantId());
            if (c != null) cm = Map.of(c.getId(), c);
        }
        return toVO(group, cm);
    }

    /**
     * Entity → VO 转换（批量查询用）。
     */
    private MeterGroupVO toVO(MeterGroup group, Map<Long, Customer> customerMap) {
        MeterGroupVO vo = new MeterGroupVO();
        vo.setId(group.getId());
        vo.setParentId(group.getParentId());
        vo.setGroupName(group.getGroupName());
        vo.setManageType(group.getManageType());
        vo.setDimensionType(group.getDimensionType() == null ? "NORMAL" : group.getDimensionType());
        vo.setDescription(group.getDescription());
        vo.setMerchantId(group.getMerchantId());
        vo.setStatus(group.getStatus());
        vo.setRemark(group.getRemark());
        vo.setCreateTime(group.getCreateTime());
        vo.setUpdateTime(group.getUpdateTime());
        vo.setCreateBy(group.getCreateBy());

        if (group.getMerchantId() != null) {
            Customer c = customerMap.get(group.getMerchantId());
            if (c != null) vo.setMerchantName(c.getCustomerName());
        }
        return vo;
    }

    private Map<Long, Customer> buildCustomerMapForGroups(List<MeterGroup> groups) {
        Set<Long> ids = groups.stream().map(MeterGroup::getMerchantId)
                .filter(id -> id != null).collect(Collectors.toSet());
        if (ids.isEmpty()) return Map.of();
        return customerMapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(Customer::getId, c -> c, (a, b) -> a));
    }

    private void validateManageType(String manageType) {
        if (manageType == null || !("BILLING".equalsIgnoreCase(manageType) || "METERING".equalsIgnoreCase(manageType))) {
            throw BusinessException.of(400, "分组管理类型只能是BILLING或METERING");
        }
    }
}
