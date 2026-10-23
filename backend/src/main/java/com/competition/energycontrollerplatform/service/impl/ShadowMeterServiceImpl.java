package com.competition.energycontrollerplatform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.competition.energycontrollerplatform.common.BusinessException;
import com.competition.energycontrollerplatform.dto.*;
import com.competition.energycontrollerplatform.entity.*;
import com.competition.energycontrollerplatform.mapper.*;
import com.competition.energycontrollerplatform.security.SecurityUserContext;
import com.competition.energycontrollerplatform.service.ShadowMeterService;
import com.competition.energycontrollerplatform.service.GroupAssociationService;
import com.competition.energycontrollerplatform.service.MeterOperationTraceService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.alibaba.excel.EasyExcel;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 虚拟电表（总分表拓扑）服务实现。
 * <p>
 * <b>核心逻辑：</b>
 * <ul>
 *   <li>CRUD 管理总表→分表拓扑关系</li>
 *   <li>线损分析：在 JVM 内存中做 BigDecimal 精确计算，不在 SQL 中聚合</li>
 *   <li>读数提取：优先 kwh，其次 read_val</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class ShadowMeterServiceImpl implements ShadowMeterService {

    private static final Logger log = LoggerFactory.getLogger(ShadowMeterServiceImpl.class);

    private final ShadowMeterMapper shadowMeterMapper;
    private final MeterMapper meterMapper;
    private final MeterReadingMapper meterReadingMapper;
    private final ProjectMapper projectMapper;
    private final RechargeOrderMapper rechargeOrderMapper;
    private final OperationLogMapper operationLogMapper;
    private final AccountFlowMapper accountFlowMapper;
    private final CustomerMapper customerMapper;
    private final GroupAssociationService groupAssociationService;
    private final MeterOperationTraceService meterOperationTraceService;

    // ──────────────────────── 分页查询（增强版） ────────────────────────

    @Override
    public IPage<ShadowMeterVO> page(ShadowMeterPageRequest request) {
        LambdaQueryWrapper<ShadowMeter> wrapper = new LambdaQueryWrapper<>();
        if (request.getProjectId() != null) {
            wrapper.eq(ShadowMeter::getProjectId, request.getProjectId());
        }
        if (request.getParentMeterId() != null) {
            wrapper.eq(ShadowMeter::getParentMeterId, request.getParentMeterId());
        }
        if (request.getChildMeterId() != null) {
            wrapper.eq(ShadowMeter::getChildMeterId, request.getChildMeterId());
        }
        if (StringUtils.hasText(request.getShadowType())) {
            wrapper.eq(ShadowMeter::getShadowType, request.getShadowType());
        }
        if (request.getStatus() != null) {
            wrapper.eq(ShadowMeter::getStatus, request.getStatus());
        }
        // 关键字搜索：模糊匹配总表名、子表名、关系描述（通过子查询实现）
        if (StringUtils.hasText(request.getKeyword())) {
            String keyword = request.getKeyword();
            wrapper.and(w -> w
                    .like(ShadowMeter::getRelationDesc, keyword)
                    .or().inSql(ShadowMeter::getParentMeterId,
                            "SELECT id FROM t_meter WHERE name LIKE '%" + keyword + "%' AND is_deleted = 0")
                    .or().inSql(ShadowMeter::getChildMeterId,
                            "SELECT id FROM t_meter WHERE name LIKE '%" + keyword + "%' AND is_deleted = 0"));
        }
        // 表计大类过滤：总表和子表必须同时满足指定类型；
        // 商户型虚拟表无总表（parent_meter_id 为 NULL），此时只需校验子表类型
        if (request.getMeterType() != null) {
            String typeSql = String.valueOf(request.getMeterType());
            wrapper.and(w -> w.isNull(ShadowMeter::getParentMeterId)
                    .or().inSql(ShadowMeter::getParentMeterId,
                            "SELECT id FROM t_meter WHERE meter_type = " + typeSql + " AND is_deleted = 0"));
            wrapper.inSql(ShadowMeter::getChildMeterId,
                    "SELECT id FROM t_meter WHERE meter_type = " + typeSql + " AND is_deleted = 0");
        }
        // 排除"真实电表关联"记录（这些仅作为电表数统计与关联详情，不参与主列表展示）；
        // 兼容 shadow_type 为 NULL 的历史数据
        wrapper.and(w -> w.isNull(ShadowMeter::getShadowType)
                .or().ne(ShadowMeter::getShadowType, "REAL_METER"));
        wrapper.orderByDesc(ShadowMeter::getCreateTime);

        Page<ShadowMeter> page = new Page<>(request.getPageNum(), request.getPageSize());
        IPage<ShadowMeter> result = shadowMeterMapper.selectPage(page, wrapper);
        Map<Long, Project> projectMap = buildProjectMap(result.getRecords());
        Map<Long, Meter> meterMap = buildMeterMapForShadow(result.getRecords());
        Map<Long, Long> realMeterCountMap = buildRealMeterCountMap(result.getRecords());
        return result.convert(e -> {
            ShadowMeterVO vo = toVO(e, projectMap, meterMap);
            vo.setRealMeterCount(realMeterCountMap.getOrDefault(e.getChildMeterId(), 0L));
            return vo;
        });
    }

    // ──────────────────────── 按ID查询 ────────────────────────

    @Override
    public ShadowMeterVO getById(Long id) {
        ShadowMeter entity = shadowMeterMapper.selectById(id);
        if (entity == null) {
            throw BusinessException.of(400, "拓扑关系不存在");
        }
        return toVO(entity);
    }

    // ──────────────────────── 创建 ────────────────────────

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ShadowMeterVO create(ShadowMeterRequest request) {
        if ("MERCHANT".equalsIgnoreCase(request.getShadowType())) {
            Long customerId = request.getCustomerId();
            MeterGroup linkedGroup = null;
            if (request.getGroupId() != null) {
                linkedGroup = groupAssociationService.requireLinkable(request.getGroupId(), null);
            }
            if (customerId == null && linkedGroup != null) {
                customerId = linkedGroup.getMerchantId();
            }
            customerId = resolveSettlementCustomer(request, customerId);
            Integer type = request.getMeterType() == null ? 1 : request.getMeterType();
            String childName = StringUtils.hasText(request.getName()) ? request.getName().trim() : ("虚拟" + (type == 1 ? "电" : "水") + "表");
            Meter child = new Meter(); child.setCustomerId(customerId); child.setGroupId(request.getGroupId()); child.setMeterType(type); child.setMeterKind("VIRTUAL");
            child.setDbNo(("VM" + (type == 1 ? "E" : "W") + UUID.randomUUID().toString().replace("-", "").substring(0, 9)).toUpperCase());
            child.setName(childName); child.setProName(request.getProjectName()); child.setStatus(1); child.setOnline(0); child.setIsDeleted(0); child.setCreateBy("MERCHANT_VIRTUAL_METER"); child.setCreateTime(LocalDateTime.now()); child.setUpdateTime(LocalDateTime.now());
            child.setWarnline(request.getWarnline()); child.setCredit(request.getCredit()); child.setFeetype(request.getFeetype()); meterMapper.insert(child);
            ShadowMeter entity = new ShadowMeter(); entity.setProjectId(request.getProjectId()); entity.setChildMeterId(child.getId()); entity.setShadowType("MERCHANT"); entity.setCoefficient(request.getCoefficient() == null ? BigDecimal.ONE : request.getCoefficient()); entity.setRelationDesc(request.getRelationDesc()); entity.setEffectiveDate(request.getEffectiveDate() == null ? LocalDate.now() : request.getEffectiveDate()); entity.setExpireDate(request.getExpireDate()); entity.setStatus(request.getStatus() == null ? 1 : request.getStatus()); entity.setCreateBy(SecurityUserContext.getCurrentUsername()); entity.setCreateTime(LocalDateTime.now()); entity.setUpdateTime(LocalDateTime.now()); shadowMeterMapper.insert(entity);
            return toVO(entity);
        }
        // 总表/分表均为可选：未指定分表时自动生成一台虚拟子表，未指定总表时不关联上级表计
        boolean hasParent = request.getParentMeterId() != null || StringUtils.hasText(request.getParentMeterNo());
        boolean hasChild = request.getChildMeterId() != null || StringUtils.hasText(request.getChildMeterNo());
        Meter parent = hasParent ? resolveMeter(request.getParentMeterId(), request.getParentMeterNo(), request, true) : null;
        Meter child = hasChild ? resolveMeter(request.getChildMeterId(), request.getChildMeterNo(), request, false) : createVirtualChildMeter(request);
        // 不能自己挂自己（仅在总表、子表都指定时校验）
        if (parent != null && parent.getId().equals(child.getId())) {
            throw BusinessException.of(400, "总表和子表不能是同一台表计");
        }
        // 总表和子表类型必须一致（仅在总表、子表都指定时校验）
        if (parent != null && !parent.getMeterType().equals(child.getMeterType())) {
            throw BusinessException.of(400, "总表和子表类型不一致：总表为"
                    + (parent.getMeterType() == 1 ? "电表" : "水表")
                    + "，子表为"
                    + (child.getMeterType() == 1 ? "电表" : "水表")
                    + "，请选择相同类型的表计");
        }
        // 唯一性校验（仅在总表、子表都指定时校验）
        if (parent != null) {
            int exists = shadowMeterMapper.countByParentAndChild(parent.getId(), child.getId());
            if (exists > 0) {
                throw BusinessException.of(400, "该总分表关系已存在");
            }
        }

        ShadowMeter entity = new ShadowMeter();
        entity.setProjectId(request.getProjectId());
        entity.setParentMeterId(parent != null ? parent.getId() : null);
        entity.setChildMeterId(child.getId());
        entity.setShadowType(request.getShadowType() != null ? request.getShadowType() : "MANUAL");
        entity.setCoefficient(request.getCoefficient() != null ? request.getCoefficient() : BigDecimal.ONE);
        entity.setRelationDesc(request.getRelationDesc());
        entity.setEffectiveDate(request.getEffectiveDate() != null ? request.getEffectiveDate() : LocalDate.now());
        entity.setExpireDate(request.getExpireDate());
        entity.setStatus(request.getStatus() != null ? request.getStatus() : 1);
        entity.setCreateBy(SecurityUserContext.getCurrentUsername());
        entity.setCreateTime(LocalDateTime.now());
        entity.setUpdateTime(LocalDateTime.now());

        shadowMeterMapper.insert(entity);
        recordTrace(entity, "CREATE", "创建虚拟表关系");
        log.info("[虚拟电表] 创建总分表关系 id={}, parentMeterId={}, childMeterId={}",
                entity.getId(), entity.getParentMeterId(), entity.getChildMeterId());

        // 同步设置子表的可编辑字段（分组、商户名、告警额、透支额、付费方式等）
        applyChildMeterFields(entity.getChildMeterId(), request);

        return toVO(entity);
    }

    /** Resolve a typed table number, creating a minimal local record when needed. */
    private Meter resolveMeter(Long meterId, String meterNo, ShadowMeterRequest request, boolean parent) {
        if (meterId != null) {
            Meter meter = meterMapper.selectById(meterId);
            if (meter == null) throw BusinessException.of(400, parent ? "总表不存在" : "分表不存在");
            return meter;
        }
        if (!StringUtils.hasText(meterNo)) {
            throw BusinessException.of(400, parent ? "请填写总表表号" : "请填写分表表号");
        }
        if (request.getMeterType() == null || (request.getMeterType() != 0 && request.getMeterType() != 1)) {
            throw BusinessException.of(400, "表计类型必须为水表或电表");
        }

        String tableNo = meterNo.trim();
        Meter existing = meterMapper.selectOne(new LambdaQueryWrapper<Meter>()
                .eq(Meter::getMeterType, request.getMeterType())
                .and(w -> w.eq(Meter::getDbNo, tableNo)
                        .or().eq(Meter::getStealNo, tableNo)
                        .or().eq(Meter::getMeteraddress, tableNo))
                .last("LIMIT 1"));
        if (existing != null) return existing;

        Meter meter = new Meter();
        meter.setMeterType(request.getMeterType());
        meter.setMeterKind("PHYSICAL");
        meter.setDbNo(tableNo);
        if (request.getMeterType() == 0) meter.setStealNo(tableNo);
        meter.setName(parent ? "总表-" + tableNo
                : (StringUtils.hasText(request.getName()) ? request.getName().trim() : "分表-" + tableNo));
        meter.setProName(StringUtils.hasText(request.getProjectName()) ? request.getProjectName().trim() : null);
        meter.setStatus(1);
        meter.setOnline(0);
        meter.setCreateBy("SHADOW_METER_MANUAL");
        meter.setCreateTime(LocalDateTime.now());
        meter.setUpdateTime(LocalDateTime.now());
        meter.setIsDeleted(0);
        meterMapper.insert(meter);
        log.info("[shadow-meter] created local {} placeholder id={}, tableNo={}",
                parent ? "parent" : "child", meter.getId(), tableNo);
        return meter;
    }

    /** 未指定分表表号时，自动创建一台虚拟子表（表号自动生成，不强制关联总表）。 */
    private Meter createVirtualChildMeter(ShadowMeterRequest request) {
        Integer type = request.getMeterType() == null ? 1 : request.getMeterType();
        Meter child = new Meter();
        child.setCustomerId(resolveSettlementCustomer(request, null));
        child.setMeterType(type);
        child.setMeterKind("VIRTUAL");
        child.setDbNo(("VM" + (type == 1 ? "E" : "W") + UUID.randomUUID().toString().replace("-", "").substring(0, 9)).toUpperCase());
        child.setName(StringUtils.hasText(request.getName()) ? request.getName().trim() : ("虚拟" + (type == 1 ? "电" : "水") + "表"));
        child.setProName(StringUtils.hasText(request.getProjectName()) ? request.getProjectName().trim() : null);
        child.setStatus(1);
        child.setOnline(0);
        child.setIsDeleted(0);
        child.setCreateBy("SHADOW_METER_MANUAL");
        child.setCreateTime(LocalDateTime.now());
        child.setUpdateTime(LocalDateTime.now());
        meterMapper.insert(child);
        log.info("[虚拟电表] 自动创建虚拟子表 id={}, dbNo={}", child.getId(), child.getDbNo());
        return child;
    }

    /**
     * Every newly created virtual meter is a settlement account carrier.  It
     * must therefore reference an existing customer account or create one in
     * the same transaction; physical topology placeholders remain unaffected.
     */
    private Long resolveSettlementCustomer(ShadowMeterRequest request, Long fallbackCustomerId) {
        Long customerId = request.getCustomerId() != null ? request.getCustomerId() : fallbackCustomerId;
        boolean create = "CREATE".equalsIgnoreCase(request.getAccountMode())
                || StringUtils.hasText(request.getAccountName());
        if (create) {
            if (!StringUtils.hasText(request.getAccountName())) {
                throw BusinessException.of(400, "创建结算账户时必须填写账户名称");
            }
            BigDecimal balance = request.getInitialBalance() == null
                    ? BigDecimal.ZERO : request.getInitialBalance();
            if (balance.compareTo(BigDecimal.ZERO) < 0) {
                throw BusinessException.of(400, "初始余额不能小于0");
            }
            Customer customer = new Customer();
            customer.setCustomerCode("VA" + UUID.randomUUID().toString().replace("-", "")
                    .substring(0, 14).toUpperCase(Locale.ROOT));
            customer.setCustomerName(request.getAccountName().trim());
            customer.setPhone(StringUtils.hasText(request.getAccountPhone())
                    ? request.getAccountPhone().trim() : null);
            customer.setCustomerType("VIRTUAL_ACCOUNT");
            customer.setBalance(balance);
            customer.setStatus(1);
            customer.setCreateBy(SecurityUserContext.getCurrentUsername());
            customer.setCreateTime(LocalDateTime.now());
            customer.setUpdateTime(LocalDateTime.now());
            customer.setIsDeleted(0);
            customerMapper.insert(customer);
            return customer.getId();
        }
        if (customerId == null) customerId = SecurityUserContext.getCurrentCustomerId();
        if (customerId == null) {
            throw BusinessException.of(400, "虚拟表必须选择已有结算账户或创建新账户");
        }
        Customer customer = customerMapper.selectById(customerId);
        if (customer == null || !Integer.valueOf(1).equals(customer.getStatus())) {
            throw BusinessException.of(400, "结算账户不存在或已停用");
        }
        return customerId;
    }

    // ──────────────────────── 更新（含子表信息同步） ────────────────────────

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ShadowMeterVO update(Long id, ShadowMeterRequest request) {
        ShadowMeter entity = shadowMeterMapper.selectById(id);
        if (entity == null) {
            throw BusinessException.of(400, "拓扑关系不存在");
        }

        if (request.getProjectId() != null) {
            entity.setProjectId(request.getProjectId());
        }
        if (request.getParentMeterId() != null) {
            entity.setParentMeterId(request.getParentMeterId());
        }
        if (request.getChildMeterId() != null) {
            entity.setChildMeterId(request.getChildMeterId());
        }
        if (request.getShadowType() != null) {
            entity.setShadowType(request.getShadowType());
        }
        if (request.getCoefficient() != null) {
            entity.setCoefficient(request.getCoefficient());
        }
        if (request.getRelationDesc() != null) {
            entity.setRelationDesc(request.getRelationDesc());
        }
        if (request.getEffectiveDate() != null) {
            entity.setEffectiveDate(request.getEffectiveDate());
        }
        if (request.getExpireDate() != null) {
            entity.setExpireDate(request.getExpireDate());
        }
        if (request.getStatus() != null) {
            entity.setStatus(request.getStatus());
        }
        entity.setUpdateTime(LocalDateTime.now());

        shadowMeterMapper.updateById(entity);
        recordTrace(entity, "UPDATE", "修改虚拟表关系");

        // 同步修改子表(Meter)的可编辑字段
        applyChildMeterFields(entity.getChildMeterId(), request);

        log.info("[虚拟电表] 更新拓扑关系 id={}", id);

        return toVO(entity);
    }

    /**
     * 将请求中的子表可编辑字段应用到指定Meter记录。
     */
    private void applyChildMeterFields(Long childMeterId, ShadowMeterRequest request) {
        Meter child = meterMapper.selectById(childMeterId);
        if (child == null) return;

        boolean needUpdate = false;
        if (Boolean.TRUE.equals(request.getClearGroup())) {
            child.setGroupId(null);
            needUpdate = true;
        } else if (request.getGroupId() != null) {
            groupAssociationService.requireLinkable(request.getGroupId(), child.getCustomerId());
            child.setGroupId(request.getGroupId());
            needUpdate = true;
        }
        if (StringUtils.hasText(request.getName())) { child.setName(request.getName()); needUpdate = true; }
        if (request.getWarnline() != null) { child.setWarnline(request.getWarnline()); needUpdate = true; }
        if (request.getCredit() != null) { child.setCredit(request.getCredit()); needUpdate = true; }
        if (request.getFeetype() != null) { child.setFeetype(request.getFeetype()); needUpdate = true; }
        if (request.getCtRatio() != null) { child.setCtRatio(request.getCtRatio()); needUpdate = true; }
        if (request.getPtRatio() != null) { child.setPtRatio(request.getPtRatio()); needUpdate = true; }
        if (StringUtils.hasText(request.getMarkStatus())) { child.setMarkStatus(request.getMarkStatus()); needUpdate = true; }
        if (StringUtils.hasText(request.getCommStatusDesc())) { child.setCommStatusDesc(request.getCommStatusDesc()); needUpdate = true; }
        if (request.getDisableWarning() != null) { child.setDisableWarning(request.getDisableWarning()); needUpdate = true; }
        if (StringUtils.hasText(request.getAddress())) { child.setAddress(request.getAddress()); needUpdate = true; }

        if (needUpdate) {
            child.setUpdateTime(LocalDateTime.now());
            meterMapper.updateById(child);
            log.info("[虚拟电表] 同步更新子表信息 childMeterId={}", childMeterId);
        }
    }

    // ──────────────────────── 删除 ────────────────────────

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        ShadowMeter entity = shadowMeterMapper.selectById(id);
        if (entity == null) {
            throw BusinessException.of(400, "拓扑关系不存在");
        }
        shadowMeterMapper.logicDeleteById(id); // 绕过 @TableLogic 的 update_time 空值问题
        recordTrace(entity, "UNBIND", "解除虚拟表与子表关联");
        log.info("[虚拟电表] 删除拓扑关系 id={}", id);
    }

    // ──────────────────────── 批量删除 ────────────────────────

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int batchDelete(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw BusinessException.of(400, "请至少选择一条拓扑关系");
        }
        int count = shadowMeterMapper.logicDeleteBatchIds(ids); // 绕过 @TableLogic
        log.info("[虚拟电表] 批量删除拓扑关系 ids={}, count={}", ids, count);
        return count;
    }

    // ──────────────────────── 统计 ────────────────────────

    @Override
    public ShadowMeterStatisticsVO getStatistics() {
        List<ShadowMeter> all = shadowMeterMapper.selectList(
                new LambdaQueryWrapper<ShadowMeter>().eq(ShadowMeter::getIsDeleted, 0));

        ShadowMeterStatisticsVO stats = new ShadowMeterStatisticsVO();
        stats.setTotalCount(all.size());
        stats.setActiveCount(all.stream().filter(s -> Integer.valueOf(1).equals(s.getStatus())).count());
        stats.setInactiveCount(all.stream().filter(s -> Integer.valueOf(0).equals(s.getStatus())).count());
        stats.setAutoCount(all.stream().filter(s -> "AUTO".equalsIgnoreCase(s.getShadowType())).count());
        stats.setManualCount(all.stream().filter(s -> "MANUAL".equalsIgnoreCase(s.getShadowType())).count());

        Map<Long, Long> byProject = all.stream()
                .collect(Collectors.groupingBy(
                        s -> s.getProjectId() != null ? s.getProjectId() : -1L,
                        LinkedHashMap::new, Collectors.counting()));
        stats.setByProject(byProject);

        Map<String, Long> byShadowType = all.stream()
                .collect(Collectors.groupingBy(
                        s -> s.getShadowType() != null ? s.getShadowType() : "UNKNOWN",
                        LinkedHashMap::new, Collectors.counting()));
        stats.setByShadowType(byShadowType);

        return stats;
    }

    // ────────────────── 关联真实电表 ──────────────────

    @Override
    public List<ShadowMeterRealMeterVO> listRealMeters(Long virtualMeterId) {
        List<Map<String, Object>> relations = shadowMeterMapper.findRealMeterRelations(virtualMeterId);
        if (relations.isEmpty()) return List.of();

        List<Long> meterIds = relations.stream()
                .map(r -> ((Number) r.get("meterId")).longValue())
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<Long, Meter> meterMap = meterIds.isEmpty() ? Map.of()
                : meterMapper.selectBatchIds(meterIds).stream()
                    .collect(Collectors.toMap(Meter::getId, m -> m, (a, b) -> a));
        Map<Long, BigDecimal> todayMap = buildTodayActiveEnergyMapByIds(meterIds);

        return relations.stream().map(r -> {
            Long relationId = ((Number) r.get("relationId")).longValue();
            Long meterId = ((Number) r.get("meterId")).longValue();
            Meter m = meterMap.get(meterId);
            ShadowMeterRealMeterVO vo = new ShadowMeterRealMeterVO();
            vo.setRelationId(relationId);
            vo.setMeterId(meterId);
            if (m != null) {
                vo.setMeterNo(resolveMeterNo(m));
                vo.setMeterName(m.getName());
                vo.setMarkStatus(m.getMarkStatus());
                vo.setPowerStatus(m.getStateValve());
                vo.setOnline(m.getOnline());
                vo.setCtRatio(m.getCtRatio());
                vo.setPtRatio(m.getPtRatio());
                vo.setUpdateTime(m.getUpdateTime());
                GroupAssociationService.GroupAssociation association =
                        groupAssociationService.describe(m.getGroupId(), m.getCustomerId());
                vo.setGroupName(association.name());
            }
            vo.setTodayActiveEnergy(todayMap.get(meterId));
            return vo;
        }).toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void bindRealMeters(Long virtualMeterId, List<Long> meterIds) {
        if (meterIds == null || meterIds.isEmpty()) {
            throw BusinessException.of(400, "请至少选择一台电表");
        }
        Meter virtualMeter = meterMapper.selectById(virtualMeterId);
        if (virtualMeter == null) {
            throw BusinessException.of(400, "虚拟电表不存在");
        }
        int added = 0;
        for (Long meterId : meterIds) {
            if (meterId == null) continue;
            if (meterId.equals(virtualMeterId)) {
                throw BusinessException.of(400, "不能将虚拟电表关联到自身");
            }
            Meter realMeter = meterMapper.selectById(meterId);
            if (realMeter == null) {
                throw BusinessException.of(400, "所选电表不存在");
            }
            if (!"PHYSICAL".equalsIgnoreCase(realMeter.getMeterKind())) {
                throw BusinessException.of(400, "所选表计不是真实电表");
            }
            boolean boundElsewhere = shadowMeterMapper.findActiveByChildId(meterId).stream()
                    .anyMatch(r -> "REAL_METER".equalsIgnoreCase(r.getShadowType())
                            && r.getParentMeterId() != null
                            && !virtualMeterId.equals(r.getParentMeterId()));
            if (boundElsewhere) {
                throw BusinessException.of(409, "所选真实表已绑定其他虚拟结算账户");
            }
            if (virtualMeter.getMeterType() != null && realMeter.getMeterType() != null
                    && !virtualMeter.getMeterType().equals(realMeter.getMeterType())) {
                throw BusinessException.of(400, "虚拟电表与真实电表类型不一致");
            }
            if (shadowMeterMapper.countByParentAndChild(virtualMeterId, meterId) > 0) {
                continue; // 已存在关联，跳过
            }
            ShadowMeter entity = new ShadowMeter();
            entity.setParentMeterId(virtualMeterId);
            entity.setChildMeterId(meterId);
            entity.setShadowType("REAL_METER");
            entity.setCoefficient(BigDecimal.ONE);
            entity.setStatus(1);
            entity.setEffectiveDate(LocalDate.now());
            entity.setCreateBy(SecurityUserContext.getCurrentUsername());
            entity.setCreateTime(LocalDateTime.now());
            entity.setUpdateTime(LocalDateTime.now());
            shadowMeterMapper.insert(entity);
            added++;
        }
        log.info("[虚拟电表] 关联真实电表 virtualMeterId={}, added={}", virtualMeterId, added);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unbindRealMeter(Long relationId) {
        ShadowMeter entity = shadowMeterMapper.selectById(relationId);
        if (entity == null) {
            throw BusinessException.of(400, "关联关系不存在");
        }
        shadowMeterMapper.logicDeleteById(relationId);
        log.info("[虚拟电表] 解除真实电表关联 relationId={}", relationId);
    }

    // ────────────────── 更多操作：充值记录 ──────────────────

    @Override
    public IPage<RechargeRecordVO> getRechargeRecords(Long childMeterId, int pageNum, int pageSize) {
        Meter child = meterMapper.selectById(childMeterId);
        if (child == null) {
            throw BusinessException.of(400, "子表不存在");
        }

        LambdaQueryWrapper<RechargeOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RechargeOrder::getMeterId, childMeterId)
               .eq(RechargeOrder::getIsDeleted, 0)
               .orderByDesc(RechargeOrder::getCreateTime);

        Page<RechargeOrder> page = new Page<>(pageNum, pageSize);
        IPage<RechargeOrder> result = rechargeOrderMapper.selectPage(page, wrapper);
        return result.convert(this::toRechargeVO);
    }

    private RechargeRecordVO toRechargeVO(RechargeOrder order) {
        RechargeRecordVO vo = new RechargeRecordVO();
        vo.setId(order.getId());
        vo.setOrderNo(order.getOrderNo());
        vo.setDbNo(order.getDbNo());
        vo.setAmount(order.getAmount());
        vo.setPayamount(order.getPayamount());
        vo.setRealamount(order.getRealamount());
        vo.setBalance(order.getBalance());
        vo.setCharegetype(order.getCharegetype());
        vo.setPassrate(order.getPassrate());
        vo.setDbName(order.getDbName());
        vo.setProName(order.getProName());
        vo.setCzy(order.getCzy());
        vo.setRemark(order.getRemark());
        vo.setCreateTime(order.getCreateTime());
        return vo;
    }

    // ────────────────── 更多操作：操作记录 ──────────────────

    @Override
    public IPage<OperationLogVO> getOperationLogs(Long childMeterId, int pageNum, int pageSize) {
        Meter child = meterMapper.selectById(childMeterId);
        if (child == null) {
            throw BusinessException.of(400, "子表不存在");
        }

        // 通过表计ID模糊匹配操作日志中的请求参数（含meterId）
        LambdaQueryWrapper<OperationLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(OperationLog::getRequestParams, "\"meterId\":" + childMeterId)
               .or().like(OperationLog::getOperationDesc, "表计")
               .orderByDesc(OperationLog::getOperationTime);

        Page<OperationLog> page = new Page<>(pageNum, pageSize);
        IPage<OperationLog> result = operationLogMapper.selectPage(page, wrapper);
        return result.convert(this::toLogVO);
    }

    private OperationLogVO toLogVO(OperationLog log) {
        OperationLogVO vo = new OperationLogVO();
        vo.setId(log.getId());
        vo.setOperatorName(log.getOperatorName());
        vo.setModuleName(log.getModuleName());
        vo.setOperationType(log.getOperationType());
        vo.setOperationDesc(log.getOperationDesc());
        vo.setRequestParams(log.getRequestParams());
        vo.setResponseResult(log.getResponseResult());
        vo.setIpAddress(log.getIpAddress());
        vo.setOperationTime(log.getOperationTime());
        return vo;
    }

    // ────────────────── 更多操作：二维码信息 ──────────────────

    @Override
    public byte[] generateQrCodeImage(Long childMeterId) {
        Meter child = meterMapper.selectById(childMeterId);
        if (child == null) {
            throw BusinessException.of(400, "子表不存在");
        }
        String dbNo = child.getDbNo();
        if (dbNo == null || dbNo.isBlank()) {
            throw BusinessException.of(400, "该表计缺少编号（dbNo），无法生成二维码");
        }
        Integer meterType = child.getMeterType();
        String content = com.competition.energycontrollerplatform.common.QrCodeGenerator
                .buildContent(meterType != null ? meterType : -1, dbNo);
        return com.competition.energycontrollerplatform.common.QrCodeGenerator.generate(content);
    }

    @Override
    public MeterResponse getQrCodeInfo(Long childMeterId) {
        Meter child = meterMapper.selectById(childMeterId);
        if (child == null) {
            throw BusinessException.of(400, "子表不存在");
        }
        // 复用 MeterServiceImpl 中的 toResponse 逻辑，这里做简化版
        MeterResponse resp = new MeterResponse();
        resp.setId(child.getId());
        resp.setCustomerId(child.getCustomerId());

        MeterResponse.General general = new MeterResponse.General();
        general.setMeterType(child.getMeterType());
        general.setStatus(child.getStatus());
        general.setOnline(child.getOnline());
        general.setGroupId(child.getGroupId());
        general.setCtRatio(child.getCtRatio());
        general.setPtRatio(child.getPtRatio());
        general.setMarkStatus(child.getMarkStatus());
        general.setEnabledTime(child.getEnabledTime());
        general.setDisableWarning(child.getDisableWarning());
        general.setCommStatusDesc(child.getCommStatusDesc());
        populateGroupInfo(general, child.getGroupId(), child.getCustomerId());
        resp.setGeneral(general);

        MeterResponse.Chenman cm = new MeterResponse.Chenman();
        cm.setDbNo(child.getDbNo());
        cm.setName(child.getName());
        cm.setBjlx(child.getBjlx());
        cm.setCredit(child.getCredit());
        cm.setWarnline(child.getWarnline());
        cm.setFeetype(child.getFeetype());
        cm.setProName(child.getProName());
        cm.setPlusPrice(child.getPlusPrice());
        cm.setPriceType(child.getPriceType());
        cm.setCollectorDeviceNo(child.getCollectorDeviceNo());
        resp.setChenman(cm);

        MeterResponse.Jicheng jc = new MeterResponse.Jicheng();
        jc.setStealNo(child.getStealNo());
        jc.setVender(child.getVender());
        jc.setMeterTypeName(child.getMeterTypeName());
        jc.setCaliber(child.getCaliber());
        jc.setTerminalName(child.getTerminalName());
        jc.setAddress(child.getAddress());
        resp.setJicheng(jc);

        MeterResponse.Jiexian jx = new MeterResponse.Jiexian();
        jx.setMeteraddress(child.getMeteraddress());
        jx.setCompanycode(child.getCompanycode());
        jx.setCustomerNo(child.getCustomerNo());
        jx.setMeterCaliber(child.getMeterCaliber());
        jx.setControlling(child.getControlling());
        jx.setStateValve(child.getStateValve());
        jx.setSignalStrength(child.getSignalStrength());
        jx.setBatteryVoltage(child.getBatteryVoltage());
        resp.setJiexian(jx);

        MeterResponse.CommParams comm = new MeterResponse.CommParams();
        comm.setCollectorCode(child.getCollectorCode());
        comm.setBpsType(child.getBpsType());
        comm.setBitType(child.getBitType());
        comm.setProtoType(child.getProtoType());
        comm.setSerial(child.getSerial());
        comm.setDeviceType(child.getDeviceType());
        resp.setCommParams(comm);

        resp.setCreateTime(child.getCreateTime());
        resp.setUpdateTime(child.getUpdateTime());
        resp.setCreateBy(child.getCreateBy());

        if (child.getCustomerId() != null) {
            resp.setCustomerName(child.getName());
        }

        return resp;
    }

    // ────────────────── 更多操作：计费轨迹 ──────────────────

    @Override
    public IPage<BillingTrackVO> getBillingTrack(Long childMeterId, int pageNum, int pageSize) {
        Meter child = meterMapper.selectById(childMeterId);
        if (child == null) {
            throw BusinessException.of(400, "子表不存在");
        }

        LambdaQueryWrapper<AccountFlow> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AccountFlow::getMeterId, childMeterId)
               .eq(AccountFlow::getIsDeleted, 0)
               .orderByDesc(AccountFlow::getOperateTime);

        Page<AccountFlow> page = new Page<>(pageNum, pageSize);
        IPage<AccountFlow> result = accountFlowMapper.selectPage(page, wrapper);
        return result.convert(this::toBillingTrackVO);
    }

    private BillingTrackVO toBillingTrackVO(AccountFlow flow) {
        BillingTrackVO vo = new BillingTrackVO();
        vo.setId(flow.getId());
        vo.setFlowNo(flow.getFlowNo());
        vo.setBalanceBefore(flow.getBalanceBefore());
        vo.setChangeAmount(flow.getChangeAmount());
        vo.setBalanceAfter(flow.getBalanceAfter());
        vo.setTradeType(flow.getTradeType());
        vo.setTradeTypeName(getTradeTypeName(flow.getTradeType()));
        vo.setBizNo(flow.getBizNo());
        vo.setOperatorName(flow.getOperatorName());
        vo.setOperateTime(flow.getOperateTime());
        vo.setRemark(flow.getRemark());
        return vo;
    }

    private String getTradeTypeName(Integer tradeType) {
        if (tradeType == null) return "未知";
        switch (tradeType) {
            case 1: return "充值";
            case 2: return "消费扣费";
            case 3: return "退费";
            case 4: return "调账";
            case 5: return "人工缴费";
            case 6: return "余额清零";
            case 7: return "公共分摊";
            default: return "其他";
        }
    }

    // ──────────────────────── 线损分析 ────────────────────────

    @Override
    public MasterLossVO lossAnalysis(Long masterId) {
        // ① 获取总表信息
        Meter masterMeter = meterMapper.selectById(masterId);
        if (masterMeter == null) {
            throw BusinessException.of(400, "总表不存在");
        }

        // ② 获取总表最新读数
        MeterReading masterReading = meterReadingMapper.getLatestByMeterId(masterId);
        BigDecimal masterKwh = extractKwh(masterReading);

        // ③ 查询总表下所有启用的子表关系
        List<ShadowMeter> subRelations = shadowMeterMapper.findActiveByMasterId(masterId);

        // ④ 遍历子表，获取每台子表的最新读数并累加
        MasterLossVO vo = new MasterLossVO();
        vo.setMasterMeterId(masterId);
        vo.setMasterMeterNo(masterMeter.getStealNo() != null ? masterMeter.getStealNo() : masterMeter.getDbNo());
        vo.setMasterMeterName(masterMeter.getName());
        vo.setMasterReading(masterKwh);

        List<MasterLossVO.SubMeterReading> subMeterReadings = new ArrayList<>();
        BigDecimal subTotal = BigDecimal.ZERO;

        for (ShadowMeter rel : subRelations) {
            Long childId = rel.getChildMeterId();
            Meter childMeter = meterMapper.selectById(childId);
            if (childMeter == null) {
                log.warn("[线损分析] 子表不存在 childMeterId={}, 跳过", childId);
                continue;
            }

            MeterReading childReading = meterReadingMapper.getLatestByMeterId(childId);
            BigDecimal childKwh = extractKwh(childReading);

            MasterLossVO.SubMeterReading smr = new MasterLossVO.SubMeterReading();
            smr.setMeterId(childId);
            smr.setMeterNo(childMeter.getStealNo() != null ? childMeter.getStealNo() : childMeter.getDbNo());
            smr.setMeterName(childMeter.getName());
            smr.setReading(childKwh != null ? childKwh : BigDecimal.ZERO);
            smr.setCoefficient(rel.getCoefficient());
            subMeterReadings.add(smr);

            // 累加时考虑系数
            BigDecimal coeff = rel.getCoefficient() != null ? rel.getCoefficient() : BigDecimal.ONE;
            if (childKwh != null) {
                subTotal = subTotal.add(childKwh.multiply(coeff));
            }
        }

        vo.setSubMeterCount(subMeterReadings.size());
        vo.setSubMetersTotalReading(subTotal);
        vo.setSubMeters(subMeterReadings);

        // ⑤ 计算损耗
        vo.calculateLoss();

        log.info("[线损分析] masterId={}, masterKwh={}, subTotal={}, loss={}, rate={}%",
                masterId, masterKwh, subTotal, vo.getAbsoluteLoss(), vo.getLossRatePercent());

        return vo;
    }

    // ──────────────────────── 私有方法 ────────────────────────

    /**
     * 从 MeterReading 中提取总电量读数。
     * <p>优先使用 kwh，其次 read_val。</p>
     */
    private BigDecimal extractKwh(MeterReading reading) {
        if (reading == null) {
            return BigDecimal.ZERO;
        }
        if (reading.getKwh() != null && reading.getKwh().compareTo(BigDecimal.ZERO) > 0) {
            return reading.getKwh();
        }
        if (reading.getReadVal() != null && reading.getReadVal().compareTo(BigDecimal.ZERO) > 0) {
            return reading.getReadVal();
        }
        return BigDecimal.ZERO;
    }

    /**
     * Entity → VO 转换（单条查询用）。
     */
    private ShadowMeterVO toVO(ShadowMeter entity) {
        Map<Long, Project> pm = entity.getProjectId() != null
                ? projectMapper.selectBatchIds(List.of(entity.getProjectId())).stream()
                    .collect(Collectors.toMap(Project::getId, p -> p, (a, b) -> a))
                : Map.of();
        Set<Long> meterIds = new java.util.HashSet<>();
        if (entity.getParentMeterId() != null) meterIds.add(entity.getParentMeterId());
        if (entity.getChildMeterId() != null) meterIds.add(entity.getChildMeterId());
        Map<Long, Meter> mm = meterIds.isEmpty() ? Map.of()
                : meterMapper.selectBatchIds(meterIds).stream()
                    .collect(Collectors.toMap(Meter::getId, m -> m, (a, b) -> a));
        return toVO(entity, pm, mm);
    }

    /**
     * Entity → VO 转换（批量查询用，从预加载 Map 取值，不查 DB）。
     */
    private ShadowMeterVO toVO(ShadowMeter entity, Map<Long, Project> projectMap, Map<Long, Meter> meterMap) {
        ShadowMeterVO vo = new ShadowMeterVO();
        vo.setId(entity.getId());
        vo.setProjectId(entity.getProjectId());
        vo.setParentMeterId(entity.getParentMeterId());
        vo.setChildMeterId(entity.getChildMeterId());
        vo.setShadowType(entity.getShadowType());
        vo.setCoefficient(entity.getCoefficient());
        vo.setRelationDesc(entity.getRelationDesc());
        vo.setEffectiveDate(entity.getEffectiveDate());
        vo.setExpireDate(entity.getExpireDate());
        vo.setStatus(entity.getStatus());
        vo.setCreateTime(entity.getCreateTime());
        vo.setCreateBy(entity.getCreateBy());

        if (entity.getProjectId() != null) {
            Project p = projectMap.get(entity.getProjectId());
            if (p != null) vo.setProjectName(p.getProjectName());
        }

        Meter parent = meterMap.get(entity.getParentMeterId());
        if (parent != null) {
            vo.setParentMeterNo(parent.getStealNo() != null ? parent.getStealNo() : parent.getDbNo());
            vo.setParentMeterName(parent.getName());
            vo.setParentMeterType(parent.getMeterType());
        }

        Meter child = meterMap.get(entity.getChildMeterId());
        if (child != null) {
            vo.setChildMeterNo(child.getStealNo() != null ? child.getStealNo() : child.getDbNo());
            vo.setChildMeterName(child.getName());
            vo.setChildMeterType(child.getMeterType());
            vo.setChildGroupId(child.getGroupId());
            GroupAssociationService.GroupAssociation association =
                    groupAssociationService.describe(child.getGroupId(), child.getCustomerId());
            vo.setChildGroupName(association.name());
            vo.setChildGroupPath(association.path());
            vo.setChildGroupStatus(association.status());
            vo.setChildGroupRelationStatus(association.relationStatus());
            vo.setChildCustomerId(child.getCustomerId());
            vo.setChildWarnline(child.getWarnline());
            vo.setChildCredit(child.getCredit());
            vo.setChildFeetype(child.getFeetype());
            vo.setChildCtRatio(child.getCtRatio());
            vo.setChildPtRatio(child.getPtRatio());
            vo.setChildMarkStatus(child.getMarkStatus());
            vo.setChildCommStatusDesc(child.getCommStatusDesc());
            vo.setChildDisableWarning(child.getDisableWarning());
            vo.setChildStatus(child.getStatus());
            vo.setChildOnline(child.getOnline());
            if (!StringUtils.hasText(vo.getProjectName()) && StringUtils.hasText(child.getProName())) {
                vo.setProjectName(child.getProName());
            }
        }

        return vo;
    }

    private void recordTrace(ShadowMeter entity, String eventType, String remark) {
        if (meterOperationTraceService == null || entity == null) return;
        MeterOperationTrace trace = new MeterOperationTrace();
        trace.setObjectType(resolveObjectType(entity)); trace.setObjectId(entity.getId()); trace.setProjectId(entity.getProjectId());
        trace.setParentMeterId(entity.getParentMeterId()); trace.setChildMeterId(entity.getChildMeterId());
        trace.setEventType(eventType); trace.setOperatorId(SecurityUserContext.getCurrentUserId());
        trace.setOperatorName(SecurityUserContext.getCurrentUsername()); trace.setRemark(remark);
        meterOperationTraceService.record(trace);
    }

    private String resolveObjectType(ShadowMeter entity) {
        Meter child = entity.getChildMeterId() == null ? null : meterMapper.selectById(entity.getChildMeterId());
        return child != null && Integer.valueOf(1).equals(child.getMeterType()) ? "VIRTUAL_ELECTRIC_METER" : "VIRTUAL_WATER_METER";
    }

    private void populateGroupInfo(MeterResponse.General general, Long groupId, Long merchantId) {
        GroupAssociationService.GroupAssociation association =
                groupAssociationService.describe(groupId, merchantId);
        general.setGroupName(association.name());
        general.setGroupPath(association.path());
        general.setGroupStatus(association.status());
        general.setGroupRelationStatus(association.relationStatus());
    }

    private Map<Long, Project> buildProjectMap(List<ShadowMeter> entities) {
        Set<Long> ids = entities.stream().map(ShadowMeter::getProjectId)
                .filter(id -> id != null).collect(Collectors.toSet());
        if (ids.isEmpty()) return Map.of();
        return projectMapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(Project::getId, p -> p, (a, b) -> a));
    }

    private Map<Long, Meter> buildMeterMapForShadow(List<ShadowMeter> entities) {
        Set<Long> ids = new java.util.HashSet<>();
        for (ShadowMeter e : entities) {
            if (e.getParentMeterId() != null) ids.add(e.getParentMeterId());
            if (e.getChildMeterId() != null) ids.add(e.getChildMeterId());
        }
        if (ids.isEmpty()) return Map.of();
        return meterMapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(Meter::getId, m -> m, (a, b) -> a));
    }

    /** 批量统计每个虚拟电表（child_meter_id）下关联的真实电表数量。 */
    private Map<Long, Long> buildRealMeterCountMap(List<ShadowMeter> entities) {
        List<Long> parentIds = entities.stream()
                .map(ShadowMeter::getChildMeterId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (parentIds.isEmpty()) return Map.of();
        return shadowMeterMapper.countRealMetersByParentIds(parentIds).stream()
                .filter(row -> row.get("virtualMeterId") != null && row.get("cnt") != null)
                .collect(Collectors.toMap(
                        row -> ((Number) row.get("virtualMeterId")).longValue(),
                        row -> ((Number) row.get("cnt")).longValue(),
                        (a, b) -> a));
    }

    /** 批量计算一组电表今日有功电量（kWh）。 */
    private Map<Long, BigDecimal> buildTodayActiveEnergyMapByIds(List<Long> meterIds) {
        if (meterIds == null || meterIds.isEmpty()) return Map.of();
        return meterReadingMapper.selectTodayActiveEnergy(meterIds).stream()
                .filter(row -> row.get("meterId") != null && row.get("todayActiveEnergy") != null)
                .collect(Collectors.toMap(
                        row -> ((Number) row.get("meterId")).longValue(),
                        row -> new BigDecimal(row.get("todayActiveEnergy").toString()),
                        (a, b) -> a));
    }

    /** 解析表计表号：优先积成钢印号/辰鳗档案号，其次捷先表地址（水表）。 */
    private String resolveMeterNo(Meter m) {
        if (StringUtils.hasText(m.getStealNo())) return m.getStealNo();
        if (StringUtils.hasText(m.getDbNo())) return m.getDbNo();
        if (StringUtils.hasText(m.getMeteraddress())) return m.getMeteraddress();
        return null;
    }

    // ──────────────────────── 批量导出 ────────────────────────

    @Override
    public void exportToExcel(jakarta.servlet.http.HttpServletResponse response, ShadowMeterPageRequest request) {
        // 全量查询（不分页）
        request.setPageNum(1);
        request.setPageSize(Integer.MAX_VALUE);
        IPage<ShadowMeterVO> pageResult = page(request);
        List<ShadowMeterExcelVO> excelData = pageResult.getRecords().stream()
                .map(this::toExcelVO)
                .collect(Collectors.toList());

        writeShadowMeterExcel(response, excelData, "虚拟电表水表列表");
    }

    @Override
    public void exportByIdsToExcel(jakarta.servlet.http.HttpServletResponse response, List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw BusinessException.of(400, "导出ID列表不能为空");
        }
        // 批量查询并预加载关联数据，避免 N+1
        List<ShadowMeter> entities = shadowMeterMapper.selectBatchIds(ids);
        Map<Long, Project> projectMap = buildProjectMap(entities);
        Map<Long, Meter> meterMap = buildMeterMapForShadow(entities);
        List<ShadowMeterExcelVO> excelData = entities.stream()
                .map(e -> toExcelVO(toVO(e, projectMap, meterMap)))
                .collect(Collectors.toList());

        writeShadowMeterExcel(response, excelData, "虚拟电表水表_批量");
    }

    // ──────────────────────── Excel 写入 ────────────────────────

    private void writeShadowMeterExcel(jakarta.servlet.http.HttpServletResponse response,
                                       List<ShadowMeterExcelVO> data, String sheetName) {
        try {
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());

            String fileName = sheetName + "_" + LocalDateTime.now()
                    .format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8)
                    .replaceAll("\\+", "%20");
            response.setHeader("Content-Disposition",
                    "attachment; filename=" + encodedFileName + ".xlsx");
            response.setHeader("Access-Control-Expose-Headers", "Content-Disposition");

            EasyExcel.write(response.getOutputStream(), ShadowMeterExcelVO.class)
                    .sheet(sheetName)
                    .doWrite(data);
        } catch (IOException e) {
            log.error("[虚拟电表导出] Excel写入失败", e);
            throw BusinessException.of(500, "Excel导出失败：" + e.getMessage());
        }
    }

    // ──────────────────────── VO → ExcelVO 转换 ────────────────────────

    private ShadowMeterExcelVO toExcelVO(ShadowMeterVO vo) {
        ShadowMeterExcelVO excel = new ShadowMeterExcelVO();
        excel.setId(vo.getId());
        excel.setProjectName(vo.getProjectName());

        excel.setParentMeterNo(vo.getParentMeterNo());
        excel.setParentMeterName(vo.getParentMeterName());
        excel.setParentMeterTypeDesc(meterTypeDesc(vo.getParentMeterType()));

        excel.setChildMeterNo(vo.getChildMeterNo());
        excel.setChildMeterName(vo.getChildMeterName());
        excel.setChildMeterTypeDesc(meterTypeDesc(vo.getChildMeterType()));
        excel.setChildWarnline(vo.getChildWarnline());
        excel.setChildCredit(vo.getChildCredit());
        excel.setChildFeetypeDesc(feetypeDesc(vo.getChildFeetype()));
        excel.setChildCtRatio(vo.getChildCtRatio());
        excel.setChildPtRatio(vo.getChildPtRatio());
        excel.setChildMarkStatus(vo.getChildMarkStatus());
        excel.setChildCommStatusDesc(vo.getChildCommStatusDesc());
        excel.setChildDisableWarningDesc(vo.getChildDisableWarning() == null ? "" :
                (vo.getChildDisableWarning() == 1 ? "已停用" : "启用中"));
        excel.setChildStatusDesc(statusDesc(vo.getChildStatus()));
        excel.setChildOnlineDesc(vo.getChildOnline() == null ? "" :
                (vo.getChildOnline() == 1 ? "在线" : "离线"));

        excel.setShadowTypeDesc("AUTO".equalsIgnoreCase(vo.getShadowType()) ? "自动生成" : "手动创建");
        excel.setCoefficient(vo.getCoefficient());
        excel.setRelationDesc(vo.getRelationDesc());
        excel.setEffectiveDate(vo.getEffectiveDate());
        excel.setExpireDate(vo.getExpireDate());
        excel.setStatusDesc(vo.getStatus() != null && vo.getStatus() == 1 ? "启用" : "停用");
        excel.setCreateTime(vo.getCreateTime());
        excel.setCreateBy(vo.getCreateBy());

        return excel;
    }

    private static String meterTypeDesc(Integer type) {
        if (type == null) return "";
        return type == 1 ? "电表" : "水表";
    }

    private static String feetypeDesc(Integer feetype) {
        if (feetype == null) return "";
        return feetype == 1 ? "预付费" : "后付费";
    }

    private static String statusDesc(Integer status) {
        if (status == null) return "";
        switch (status) {
            case 0: return "停用";
            case 1: return "正常运行";
            case 2: return "欠费停机";
            default: return "未知";
        }
    }
}
