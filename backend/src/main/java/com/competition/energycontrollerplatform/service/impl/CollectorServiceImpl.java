package com.competition.energycontrollerplatform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.competition.energycontrollerplatform.common.BusinessException;
import com.competition.energycontrollerplatform.dto.*;
import com.competition.energycontrollerplatform.entity.Collector;
import com.competition.energycontrollerplatform.entity.Customer;
import com.competition.energycontrollerplatform.entity.Meter;
import com.competition.energycontrollerplatform.mapper.CollectorMapper;
import com.competition.energycontrollerplatform.mapper.CustomerMapper;
import com.competition.energycontrollerplatform.mapper.MeterMapper;
import com.competition.energycontrollerplatform.security.SecurityUserContext;
import com.competition.energycontrollerplatform.service.CollectorService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 基站/集中器管理服务实现。
 * <p>
 * <b>权限隔离策略：</b>
 * <ul>
 *   <li>ADMIN 可操作/查询所有集中器，可指定任意 customerId</li>
 *   <li>非 ADMIN 强制将 customerId 置为当前登录用户的 customerId，
 *       且查询时仅返回属于自己的集中器</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class CollectorServiceImpl implements CollectorService {

    private static final Logger log = LoggerFactory.getLogger(CollectorServiceImpl.class);

    private final CollectorMapper collectorMapper;
    private final CustomerMapper customerMapper;
    private final MeterMapper meterMapper;

    // ──────────────────────── 分页查询 ────────────────────────

    @Override
    public IPage<CollectorVO> page(int pageNum, int pageSize, String keyword, Long customerId, String areaCode) {
        Long effectiveCustomerId = resolveCustomerId(customerId);

        LambdaQueryWrapper<Collector> wrapper = new LambdaQueryWrapper<>();
        if (effectiveCustomerId != null) {
            wrapper.eq(Collector::getCustomerId, effectiveCustomerId);
        }
        if (keyword != null && !keyword.isBlank()) {
            wrapper.and(w -> w.like(Collector::getCollCode, keyword.trim())
                             .or().like(Collector::getCollName, keyword.trim())
                             .or().like(Collector::getLocation, keyword.trim()));
        }
        if (areaCode != null && !areaCode.isBlank()) {
            wrapper.eq(Collector::getAreaCode, areaCode.trim());
        }
        wrapper.orderByDesc(Collector::getCreateTime);

        Page<Collector> page = new Page<>(pageNum, pageSize);
        IPage<Collector> result = collectorMapper.selectPage(page, wrapper);

        Map<Long, Customer> customerMap = buildCustomerMapForCollector(result.getRecords());
        return result.convert(c -> toVO(c, customerMap));
    }

    // ──────────────────────── 按ID查询 ────────────────────────

    @Override
    public CollectorVO getById(Long id) {
        Collector collector = collectorMapper.selectById(id);
        if (collector == null) {
            throw BusinessException.of(400, "集中器不存在");
        }
        checkOwnership(collector);
        return toVO(collector);
    }

    // ──────────────────────── 创建 ────────────────────────

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CollectorVO create(CollectorRequest request) {
        Long effectiveCustomerId = resolveCustomerId(request.getCustomerId());
        if (effectiveCustomerId == null) {
            throw BusinessException.of(400, "必须指定关联商户");
        }

        // 校验商户存在
        Customer customer = customerMapper.selectById(effectiveCustomerId);
        if (customer == null) {
            throw BusinessException.of(400, "关联商户不存在");
        }

        // 校验集中器编号唯一性
        Long exists = collectorMapper.selectCount(
                new LambdaQueryWrapper<Collector>()
                        .eq(Collector::getCollCode, request.getCollCode()));
        if (exists > 0) {
            throw BusinessException.of(400, "集中器编号已存在: " + request.getCollCode());
        }

        Collector collector = new Collector();
        collector.setCustomerId(effectiveCustomerId);
        collector.setCollCode(request.getCollCode());
        collector.setCollName(request.getCollName());
        collector.setFactCode(request.getFactCode());
        collector.setLocation(request.getLocation());
        collector.setAreaCode(request.getAreaCode());
        collector.setMemo(request.getMemo());
        collector.setSign(request.getSign());
        collector.setCreateBy(SecurityUserContext.getCurrentUsername());
        collector.setCreateTime(LocalDateTime.now());
        collector.setUpdateTime(LocalDateTime.now());

        collectorMapper.insert(collector);
        log.info("[集中器管理] 创建成功 id={}, collCode={}, customerId={}",
                collector.getId(), collector.getCollCode(), effectiveCustomerId);

        return toVO(collector);
    }

    // ──────────────────────── 更新 ────────────────────────

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CollectorVO update(Long id, CollectorRequest request) {
        Collector collector = collectorMapper.selectById(id);
        if (collector == null) {
            throw BusinessException.of(400, "集中器不存在");
        }
        checkOwnership(collector);

        // 非 ADMIN 不允许修改 customerId
        if (!SecurityUserContext.isAdmin()) {
            request.setCustomerId(collector.getCustomerId());
        } else if (request.getCustomerId() != null
                && !request.getCustomerId().equals(collector.getCustomerId())) {
            // ADMIN 修改 customerId 时校验目标商户存在
            Customer customer = customerMapper.selectById(request.getCustomerId());
            if (customer == null) {
                throw BusinessException.of(400, "目标关联商户不存在");
            }
            collector.setCustomerId(request.getCustomerId());
        }

        if (request.getCollCode() != null && !request.getCollCode().equals(collector.getCollCode())) {
            // 校验新编号唯一性
            Long exists = collectorMapper.selectCount(
                    new LambdaQueryWrapper<Collector>()
                            .eq(Collector::getCollCode, request.getCollCode())
                            .ne(Collector::getId, id));
            if (exists > 0) {
                throw BusinessException.of(400, "集中器编号已存在: " + request.getCollCode());
            }
            collector.setCollCode(request.getCollCode());
        }
        if (request.getCollName() != null) {
            collector.setCollName(request.getCollName());
        }
        if (request.getFactCode() != null) {
            collector.setFactCode(request.getFactCode());
        }
        if (request.getLocation() != null) {
            collector.setLocation(request.getLocation());
        }
        if (request.getAreaCode() != null) {
            collector.setAreaCode(request.getAreaCode());
        }
        if (request.getMemo() != null) {
            collector.setMemo(request.getMemo());
        }
        if (request.getSign() != null) {
            collector.setSign(request.getSign());
        }
        collector.setUpdateTime(LocalDateTime.now());

        collectorMapper.updateById(collector);
        log.info("[集中器管理] 更新成功 id={}, collCode={}", id, collector.getCollCode());

        return toVO(collector);
    }

    // ──────────────────────── 删除 ────────────────────────

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        Collector collector = collectorMapper.selectById(id);
        if (collector == null) {
            throw BusinessException.of(400, "集中器不存在");
        }
        checkOwnership(collector);

        collectorMapper.logicDeleteById(id); // 绕过 @TableLogic 的 update_time 空值问题
        log.info("[集中器管理] 删除成功 id={}, collCode={}", id, collector.getCollCode());
    }

    // ──────────────────────── 下拉列表 ────────────────────────

    @Override
    public List<CollectorVO> listByCustomer(Long customerId) {
        Long effectiveCustomerId = resolveCustomerId(customerId);

        LambdaQueryWrapper<Collector> wrapper = new LambdaQueryWrapper<>();
        if (effectiveCustomerId != null) {
            wrapper.eq(Collector::getCustomerId, effectiveCustomerId);
        }
        wrapper.orderByAsc(Collector::getCollName);

        List<Collector> collectors = collectorMapper.selectList(wrapper);
        Map<Long, Customer> customerMap = buildCustomerMapForCollector(collectors);
        List<CollectorVO> vos = new ArrayList<>(collectors.size());
        for (Collector c : collectors) {
            vos.add(toVO(c, customerMap));
        }
        return vos;
    }

    // ──────────────────────── 统计 ────────────────────────

    @Override
    public CollectorStatisticsVO statistics(Long customerId, String areaCode) {
        Long effectiveCustomerId = resolveCustomerId(customerId);

        LambdaQueryWrapper<Collector> wrapper = new LambdaQueryWrapper<>();
        if (effectiveCustomerId != null) {
            wrapper.eq(Collector::getCustomerId, effectiveCustomerId);
        }
        if (areaCode != null && !areaCode.isBlank()) {
            wrapper.eq(Collector::getAreaCode, areaCode.trim());
        }

        List<Collector> collectors = collectorMapper.selectList(wrapper);

        long total = collectors.size();

        // 按 areaCode 分组统计
        Map<String, Long> areaCountMap = collectors.stream()
                .collect(Collectors.groupingBy(
                        c -> c.getAreaCode() != null ? c.getAreaCode() : "未分类",
                        Collectors.counting()));

        List<CollectorStatisticsVO.AreaStat> areaStats = areaCountMap.entrySet().stream()
                .map(e -> CollectorStatisticsVO.AreaStat.builder()
                        .areaCode(e.getKey())
                        .areaName(e.getKey())
                        .count(e.getValue())
                        .build())
                .sorted(Comparator.comparing(CollectorStatisticsVO.AreaStat::getAreaCode))
                .collect(Collectors.toList());

        return CollectorStatisticsVO.builder()
                .totalCollectors(total)
                .areaStats(areaStats)
                .build();
    }

    // ──────────────────────── 导出 ────────────────────────

    @Override
    public List<CollectorExcelVO> exportList(String keyword, Long customerId, String areaCode) {
        Long effectiveCustomerId = resolveCustomerId(customerId);

        LambdaQueryWrapper<Collector> wrapper = new LambdaQueryWrapper<>();
        if (effectiveCustomerId != null) {
            wrapper.eq(Collector::getCustomerId, effectiveCustomerId);
        }
        if (keyword != null && !keyword.isBlank()) {
            wrapper.and(w -> w.like(Collector::getCollCode, keyword.trim())
                             .or().like(Collector::getCollName, keyword.trim())
                             .or().like(Collector::getLocation, keyword.trim()));
        }
        if (areaCode != null && !areaCode.isBlank()) {
            wrapper.eq(Collector::getAreaCode, areaCode.trim());
        }
        wrapper.orderByDesc(Collector::getCreateTime);

        List<Collector> collectors = collectorMapper.selectList(wrapper);
        Map<Long, Customer> customerMap = buildCustomerMapForCollector(collectors);
        return collectors.stream().map(c -> toExcelVO(c, customerMap)).collect(Collectors.toList());
    }

    @Override
    public List<CollectorExcelVO> exportBatch(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }

        List<Collector> collectors = collectorMapper.selectBatchIds(ids);

        // 权限校验：非 ADMIN 只能导出自己的
        if (!SecurityUserContext.isAdmin()) {
            Long myCustomerId = SecurityUserContext.getCurrentCustomerId();
            for (Collector c : collectors) {
                if (myCustomerId == null || !myCustomerId.equals(c.getCustomerId())) {
                    throw BusinessException.of(403, "无权导出集中器: " + c.getCollCode());
                }
            }
        }

        Map<Long, Customer> customerMap = buildCustomerMapForCollector(collectors);
        return collectors.stream().map(c -> toExcelVO(c, customerMap)).collect(Collectors.toList());
    }

    // ──────────────────────── 电表/水表绑定 ────────────────────────

    @Override
    public IPage<CollectorMeterVO> listMeters(Long collectorId, int pageNum, int pageSize,
                                              String keyword, Integer meterType) {
        Collector collector = collectorMapper.selectById(collectorId);
        if (collector == null) {
            throw BusinessException.of(400, "集中器不存在");
        }
        checkOwnership(collector);

        LambdaQueryWrapper<Meter> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Meter::getCollectorCode, collector.getCollCode());

        if (meterType != null) {
            wrapper.eq(Meter::getMeterType, meterType);
        }
        if (keyword != null && !keyword.isBlank()) {
            String kw = keyword.trim();
            wrapper.and(w -> w.like(Meter::getStealNo, kw)
                             .or().like(Meter::getDbNo, kw)
                             .or().like(Meter::getMeteraddress, kw)
                             .or().like(Meter::getName, kw));
        }
        wrapper.orderByDesc(Meter::getCreateTime);

        Page<Meter> page = new Page<>(pageNum, pageSize);
        IPage<Meter> result = meterMapper.selectPage(page, wrapper);

        return result.convert(this::toMeterVO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void bindMeter(Long collectorId, Long meterId) {
        Collector collector = collectorMapper.selectById(collectorId);
        if (collector == null) {
            throw BusinessException.of(400, "集中器不存在");
        }
        checkOwnership(collector);

        Meter meter = meterMapper.selectById(meterId);
        if (meter == null) {
            throw BusinessException.of(400, "表计不存在");
        }

        // 检查是否已绑定到其他集中器
        if (meter.getCollectorCode() != null
                && !meter.getCollectorCode().isBlank()
                && !meter.getCollectorCode().equals(collector.getCollCode())) {
            throw BusinessException.of(400, "该表计已绑定到集中器: " + meter.getCollectorCode() + "，请先解绑");
        }

        meter.setCollectorCode(collector.getCollCode());
        if (collector.getFactCode() != null) {
            meter.setCollectorDeviceNo(collector.getFactCode());
        }
        meter.setUpdateTime(LocalDateTime.now());
        meterMapper.updateById(meter);

        log.info("[集中器管理] 绑定表计 collectorId={}, collCode={}, meterId={}",
                collectorId, collector.getCollCode(), meterId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unbindMeter(Long collectorId, Long meterId) {
        Collector collector = collectorMapper.selectById(collectorId);
        if (collector == null) {
            throw BusinessException.of(400, "集中器不存在");
        }
        checkOwnership(collector);

        Meter meter = meterMapper.selectById(meterId);
        if (meter == null) {
            throw BusinessException.of(400, "表计不存在");
        }

        // 校验该表计确实绑定在此集中器上
        if (meter.getCollectorCode() == null
                || !meter.getCollectorCode().equals(collector.getCollCode())) {
            throw BusinessException.of(400, "该表计未绑定到此集中器");
        }

        meter.setCollectorCode(null);
        meter.setCollectorDeviceNo(null);
        meter.setUpdateTime(LocalDateTime.now());
        meterMapper.updateById(meter);

        log.info("[集中器管理] 解绑表计 collectorId={}, collCode={}, meterId={}",
                collectorId, collector.getCollCode(), meterId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int unbindAllMeters(Long collectorId) {
        Collector collector = collectorMapper.selectById(collectorId);
        if (collector == null) {
            throw BusinessException.of(400, "集中器不存在");
        }
        checkOwnership(collector);

        // 查询该集中器下所有已绑定表计
        LambdaQueryWrapper<Meter> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Meter::getCollectorCode, collector.getCollCode());
        List<Meter> meters = meterMapper.selectList(wrapper);

        if (meters.isEmpty()) {
            return 0;
        }

        // 批量解绑（在同一个事务内执行）
        LocalDateTime now = LocalDateTime.now();
        for (Meter meter : meters) {
            meter.setCollectorCode(null);
            meter.setCollectorDeviceNo(null);
            meter.setUpdateTime(now);
            meterMapper.updateById(meter);
        }

        log.info("[集中器管理] 批量解绑 collectorId={}, collCode={}, 解绑数量={}",
                collectorId, collector.getCollCode(), meters.size());

        return meters.size();
    }

    // ──────────────────────── 一键扫描绑定 ────────────────────────

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int bindAllOnlineMeters(Long collectorId) {
        Collector collector = collectorMapper.selectById(collectorId);
        if (collector == null) {
            throw BusinessException.of(400, "集中器不存在");
        }
        checkOwnership(collector);

        // 查询所有在线（online=1）、未删除、未绑定到任何集中器的表计
        LambdaQueryWrapper<Meter> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Meter::getOnline, 1)
               .eq(Meter::getIsDeleted, 0)
               .and(w -> w.isNull(Meter::getCollectorCode)
                        .or()
                        .eq(Meter::getCollectorCode, ""));

        // 如果集中器指定了区域代码，优先匹配同区域的表计
        if (collector.getAreaCode() != null && !collector.getAreaCode().isBlank()) {
            // 按 address 字段做区域模糊匹配（address 字符前几位通常代表区域）
            wrapper.likeRight(Meter::getAddress, collector.getAreaCode());
        }

        // 如果有厂商代码，匹配同厂商的表计
        if (collector.getFactCode() != null && !collector.getFactCode().isBlank()) {
            wrapper.eq(Meter::getVender, collector.getFactCode());
        }

        List<Meter> onlineMeters = meterMapper.selectList(wrapper);

        if (onlineMeters.isEmpty()) {
            log.info("[集中器管理] 一键绑定：未发现在线且未绑定的表计 collectorId={}, collCode={}",
                    collectorId, collector.getCollCode());
            return 0;
        }

        // 批量绑定（在同一个事务内执行）
        LocalDateTime now = LocalDateTime.now();
        for (Meter meter : onlineMeters) {
            meter.setCollectorCode(collector.getCollCode());
            if (collector.getFactCode() != null) {
                meter.setCollectorDeviceNo(collector.getFactCode());
            }
            meter.setUpdateTime(now);
            meterMapper.updateById(meter);
        }

        log.info("[集中器管理] 一键绑定完成 collectorId={}, collCode={}, 绑定数量={}",
                collectorId, collector.getCollCode(), onlineMeters.size());

        return onlineMeters.size();
    }

    // ──────────────────────── 批量导入 ────────────────────────

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int batchImport(List<CollectorRequest> requests) {
        if (requests == null || requests.isEmpty()) {
            return 0;
        }
        if (requests.size() > 500) {
            throw BusinessException.of(400, "单次批量导入最多500条");
        }

        Long effectiveCustomerId = resolveCustomerId(
                requests.get(0).getCustomerId());

        int imported = 0;
        List<String> errors = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        String operator = SecurityUserContext.getCurrentUsername();

        for (int i = 0; i < requests.size(); i++) {
            CollectorRequest req = requests.get(i);
            try {
                // 校验编号唯一性
                Long exists = collectorMapper.selectCount(
                        new LambdaQueryWrapper<Collector>()
                                .eq(Collector::getCollCode, req.getCollCode()));
                if (exists > 0) {
                    errors.add(String.format("第%d行: 集中器编号 %s 已存在", i + 1, req.getCollCode()));
                    continue;
                }

                Collector collector = new Collector();
                collector.setCustomerId(
                        effectiveCustomerId != null ? effectiveCustomerId : req.getCustomerId());
                collector.setCollCode(req.getCollCode());
                collector.setCollName(req.getCollName());
                collector.setFactCode(req.getFactCode());
                collector.setLocation(req.getLocation());
                collector.setAreaCode(req.getAreaCode());
                collector.setMemo(req.getMemo());
                collector.setSign(req.getSign());
                collector.setCreateBy(operator);
                collector.setCreateTime(now);
                collector.setUpdateTime(now);

                collectorMapper.insert(collector);
                imported++;
            } catch (Exception e) {
                errors.add(String.format("第%d行: %s", i + 1, e.getMessage()));
                log.warn("[集中器批量导入] 第{}行导入失败: {}", i + 1, e.getMessage());
            }
        }

        if (!errors.isEmpty() && imported == 0) {
            throw BusinessException.of(400, "批量导入全部失败: " + String.join("; ", errors));
        }

        log.info("[集中器批量导入] 总数={}, 成功={}, 失败={}", requests.size(), imported, errors.size());
        return imported;
    }

    // ──────────────────────── 私有方法 ────────────────────────

    /**
     * 权限隔离：解析目标商户 ID。
     * <p>ADMIN 可用前端传入值；非 ADMIN 强制使用当前登录用户关联的商户 ID。</p>
     */
    private Long resolveCustomerId(Long requestedCustomerId) {
        if (SecurityUserContext.isAdmin()) {
            return requestedCustomerId; // null=查全量
        }
        Long myCustomerId = SecurityUserContext.getCurrentCustomerId();
        return myCustomerId != null ? myCustomerId : -1L;
    }

    /**
     * 校验当前用户是否有权操作该集中器。
     */
    private void checkOwnership(Collector collector) {
        if (SecurityUserContext.isAdmin()) {
            return;
        }
        Long myCustomerId = SecurityUserContext.getCurrentCustomerId();
        if (myCustomerId == null || !myCustomerId.equals(collector.getCustomerId())) {
            throw BusinessException.of(403, "无权操作该集中器");
        }
    }

    /**
     * Entity → VO 转换（单条查询用）。
     */
    private CollectorVO toVO(Collector collector) {
        Map<Long, Customer> cm = Map.of();
        if (collector.getCustomerId() != null) {
            Customer c = customerMapper.selectById(collector.getCustomerId());
            if (c != null) cm = Map.of(c.getId(), c);
        }
        return toVO(collector, cm);
    }

    /**
     * Entity → VO 转换（批量查询用，从预加载 Map 取值，不查 DB）。
     */
    private CollectorVO toVO(Collector collector, Map<Long, Customer> customerMap) {
        CollectorVO vo = new CollectorVO();
        vo.setId(collector.getId());
        vo.setCustomerId(collector.getCustomerId());
        vo.setCollCode(collector.getCollCode());
        vo.setCollName(collector.getCollName());
        vo.setFactCode(collector.getFactCode());
        vo.setLocation(collector.getLocation());
        vo.setAreaCode(collector.getAreaCode());
        vo.setMemo(collector.getMemo());
        vo.setSign(collector.getSign());
        vo.setCreateTime(collector.getCreateTime());
        vo.setUpdateTime(collector.getUpdateTime());
        vo.setCreateBy(collector.getCreateBy());

        if (collector.getCustomerId() != null) {
            Customer c = customerMap.get(collector.getCustomerId());
            if (c != null) {
                vo.setCustomerName(c.getCustomerName());
            }
        }
        return vo;
    }

    /**
     * Entity → ExcelVO 转换（单条查询用）。
     */
    private CollectorExcelVO toExcelVO(Collector collector) {
        Map<Long, Customer> cm = Map.of();
        if (collector.getCustomerId() != null) {
            Customer c = customerMapper.selectById(collector.getCustomerId());
            if (c != null) cm = Map.of(c.getId(), c);
        }
        return toExcelVO(collector, cm);
    }

    /**
     * Entity → ExcelVO 转换（批量查询用）。
     */
    private CollectorExcelVO toExcelVO(Collector collector, Map<Long, Customer> customerMap) {
        CollectorExcelVO vo = new CollectorExcelVO();
        vo.setCollCode(collector.getCollCode());
        vo.setCollName(collector.getCollName());
        vo.setFactCode(collector.getFactCode());
        vo.setLocation(collector.getLocation());
        vo.setAreaCode(collector.getAreaCode());
        vo.setMemo(collector.getMemo());
        vo.setCreateTime(collector.getCreateTime());
        vo.setUpdateTime(collector.getUpdateTime());
        vo.setCreateBy(collector.getCreateBy());

        if (collector.getCustomerId() != null) {
            Customer c = customerMap.get(collector.getCustomerId());
            if (c != null) {
                vo.setCustomerName(c.getCustomerName());
            }
        }
        return vo;
    }

    private Map<Long, Customer> buildCustomerMapForCollector(List<Collector> collectors) {
        Set<Long> ids = collectors.stream()
                .map(Collector::getCustomerId)
                .filter(id -> id != null)
                .collect(Collectors.toSet());
        if (ids.isEmpty()) return Map.of();
        return customerMapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(Customer::getId, c -> c, (a, b) -> a));
    }

    /**
     * Meter Entity → CollectorMeterVO 转换。
     */
    private CollectorMeterVO toMeterVO(Meter meter) {
        CollectorMeterVO vo = new CollectorMeterVO();
        vo.setId(meter.getId());
        vo.setStealNo(meter.getStealNo());
        vo.setDbNo(meter.getDbNo());
        vo.setMeteraddress(meter.getMeteraddress());
        vo.setName(meter.getName());
        vo.setMeterType(meter.getMeterType());
        vo.setMeterTypeName(meter.getMeterTypeName());
        vo.setStatus(meter.getStatus());
        vo.setOnline(meter.getOnline());
        vo.setCollectorCode(meter.getCollectorCode());
        vo.setCollectorDeviceNo(meter.getCollectorDeviceNo());
        vo.setCustomerId(meter.getCustomerId());
        vo.setCreateTime(meter.getCreateTime());
        return vo;
    }
}
