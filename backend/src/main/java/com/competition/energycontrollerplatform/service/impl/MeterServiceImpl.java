package com.competition.energycontrollerplatform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.competition.energycontrollerplatform.common.BloomFilterHelper;
import com.competition.energycontrollerplatform.common.BusinessException;
import com.competition.energycontrollerplatform.common.CacheNullValueHelper;
import com.competition.energycontrollerplatform.common.DbNoGenerator;
import com.competition.energycontrollerplatform.common.QrCodeGenerator;
import com.competition.energycontrollerplatform.common.WalletException;
import com.competition.energycontrollerplatform.dto.*;
import com.competition.energycontrollerplatform.entity.AccountFlow;
import com.competition.energycontrollerplatform.entity.Customer;
import com.competition.energycontrollerplatform.entity.Meter;
import com.competition.energycontrollerplatform.entity.MeterReading;
import com.competition.energycontrollerplatform.entity.OperationLog;
import com.competition.energycontrollerplatform.entity.PriceStrategy;
import com.competition.energycontrollerplatform.integration.hook.ValveControlHook;
import com.competition.energycontrollerplatform.mapper.AccountFlowMapper;
import com.competition.energycontrollerplatform.mapper.CustomerMapper;
import com.competition.energycontrollerplatform.mapper.MeterMapper;
import com.competition.energycontrollerplatform.mapper.MeterReadingMapper;
import com.competition.energycontrollerplatform.mapper.OperationLogMapper;
import com.competition.energycontrollerplatform.mapper.PriceStrategyMapper;
import com.competition.energycontrollerplatform.security.SecurityUserContext;
import com.competition.energycontrollerplatform.service.MeterService;
import com.competition.energycontrollerplatform.service.GroupAssociationService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import jakarta.annotation.PostConstruct;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 表计管理服务实现
 */
@Service
@RequiredArgsConstructor
public class MeterServiceImpl implements MeterService {

    private static final Logger log = LoggerFactory.getLogger(MeterServiceImpl.class);

    private final MeterMapper meterMapper;
    private final MeterReadingMapper meterReadingMapper;
    private final CustomerMapper customerMapper;
    private final AccountFlowMapper accountFlowMapper;
    private final OperationLogMapper operationLogMapper;
    private final PriceStrategyMapper priceStrategyMapper;
    private final ValveControlHook valveControlHook;
    private final BloomFilterHelper bloomFilterHelper;
    private final CacheNullValueHelper cacheNullValueHelper;
    private final DbNoGenerator dbNoGenerator;
    private final GroupAssociationService groupAssociationService;

    @Value("${app.redis.enabled:true}")
    private boolean redisEnabled;

    // ──────────────── 分页列表 ────────────────

    @Override
    public IPage<MeterResponse> listPage(MeterPageRequest request) {
        LambdaQueryWrapper<Meter> wrapper = new LambdaQueryWrapper<>();

        String meterKind = normalizeMeterKind(request.getMeterKind());

        // ── 权限隔离：非 ADMIN 强制过滤 customerId，禁止从前端接收 ──
        boolean isAdmin = SecurityUserContext.isAdmin();
        if (!isAdmin) {
            Long myCustomerId = SecurityUserContext.getCurrentCustomerId();
            if (myCustomerId == null) {
                // 未关联商户的用户（如 OPERATOR）不允许查询表计
                throw BusinessException.of(403, "无权限查看表计列表");
            }
            // 无视前端传入的 customerId，强制使用当前用户关联的商户 ID
            wrapper.eq(Meter::getCustomerId, myCustomerId);
        } else {
            // ADMIN 可按前端筛选条件查询
            wrapper.eq(request.getCustomerId() != null, Meter::getCustomerId, request.getCustomerId());
        }

        // 动态筛选条件
        wrapper.eq(StringUtils.hasText(request.getStealNo()), Meter::getStealNo, request.getStealNo())
               .eq(StringUtils.hasText(request.getDbNo()), Meter::getDbNo, request.getDbNo())
               .eq(StringUtils.hasText(request.getMeteraddress()), Meter::getMeteraddress, request.getMeteraddress())
               .like(StringUtils.hasText(request.getName()), Meter::getName, request.getName())
               .eq(request.getProjectId() != null, Meter::getProId, request.getProjectId())
               .eq(request.getStatus() != null, Meter::getStatus, request.getStatus())
               .eq(request.getOnline() != null, Meter::getOnline, request.getOnline())
               .eq(request.getBjlx() != null, Meter::getBjlx, request.getBjlx())
               .eq(request.getMeterType() != null, Meter::getMeterType, request.getMeterType())
               .eq(StringUtils.hasText(meterKind), Meter::getMeterKind, meterKind)
               .like(StringUtils.hasText(request.getTerminalName()), Meter::getTerminalName, request.getTerminalName())
               .eq(StringUtils.hasText(request.getMarkStatus()), Meter::getMarkStatus, request.getMarkStatus())
               .eq(StringUtils.hasText(request.getCommStatusDesc()), Meter::getCommStatusDesc, request.getCommStatusDesc())
               .eq(request.getDisableWarning() != null, Meter::getDisableWarning, request.getDisableWarning())
               .eq(request.getGroupId() != null, Meter::getGroupId, request.getGroupId());

        // 厂家筛选：按厂商特征字段区分积成/捷先水表
        applyVendorFilter(wrapper, request.getVender());

        // 电表管理默认按表号升序排列。
        if (Integer.valueOf(1).equals(request.getMeterType())) {
            wrapper.orderByAsc(Meter::getDbNo);
        } else {
            wrapper.orderByDesc(Meter::getCreateTime);
        }

        Page<Meter> page = new Page<>(request.getPageNum(), request.getPageSize());
        IPage<Meter> meterPage = meterMapper.selectPage(page, wrapper);

        // 批量预加载商户，避免 N+1 查询
        Map<Long, Customer> customerMap = buildCustomerMap(meterPage.getRecords());
        Map<Long, BigDecimal> todayActiveEnergyMap = request.getMeterType() != null && request.getMeterType() == 1
                ? buildTodayActiveEnergyMap(meterPage.getRecords()) : Map.of();
        Map<Long, BigDecimal> currentActiveEnergyMap = request.getMeterType() != null && request.getMeterType() == 1
                ? buildCurrentActiveEnergyMap(meterPage.getRecords()) : Map.of();
        return meterPage.convert(m -> toResponse(m, customerMap, todayActiveEnergyMap, currentActiveEnergyMap));
    }

    /**
     * 厂家筛选：按厂商特征字段区分积成 / 捷先水表。
     * <p>
     * 注意：积成与捷先同步时都会互相回填 {@code stealNo} / {@code meteraddress}
     * （便于按编号互查），因此不能仅凭“有 stealNo / 有 meteraddress”判断厂商。
     * 捷先水表独有 {@code companycode}（厂家编号）、{@code meterCaliber}（表口径）、
     * {@code controlling} / {@code stateValve}（阀控）、{@code signalStrength} /
     * {@code batteryVoltage}（信号/电池）；积成水表不具备这些字段。
     */
    private void applyVendorFilter(LambdaQueryWrapper<Meter> wrapper, String vender) {
        if (!StringUtils.hasText(vender)) {
            return;
        }
        if ("捷先".equals(vender)) {
            wrapper.and(w -> w
                    .isNotNull(Meter::getCompanycode).ne(Meter::getCompanycode, "")
                    .or()
                    .isNotNull(Meter::getMeterCaliber)
                    .or()
                    .isNotNull(Meter::getControlling)
                    .or()
                    .isNotNull(Meter::getStateValve)
                    .or()
                    .isNotNull(Meter::getSignalStrength)
                    .or()
                    .isNotNull(Meter::getBatteryVoltage));
        } else if ("青岛积成电子".equals(vender)) {
            wrapper.and(w -> w
                    .and(w2 -> w2.isNull(Meter::getCompanycode).or().eq(Meter::getCompanycode, ""))
                    .isNull(Meter::getMeterCaliber)
                    .isNull(Meter::getControlling)
                    .isNull(Meter::getStateValve)
                    .isNull(Meter::getSignalStrength)
                    .isNull(Meter::getBatteryVoltage));
        } else {
            wrapper.eq(Meter::getVender, vender);
        }
    }

    // ──────────────── 查看详情 ────────────────

    @Override
    public MeterResponse getById(Long id) {
        // ── 布隆过滤器快速否定（防止缓存穿透） ──
        if (!bloomFilterHelper.mightContain(String.valueOf(id))) {
            throw BusinessException.of(404, "表计不存在");
        }
        Meter meter = meterMapper.selectById(id);
        // 正常查询未命中时，穿透 @TableLogic 过滤查询已删除电表
        if (meter == null) {
            meter = meterMapper.selectByIdIncludingDeleted(id);
            if (meter != null && meter.getIsDeleted() != null && meter.getIsDeleted() == 1) {
                throw BusinessException.of(404, "该表计已被删除，如需查看历史记录请联系管理员");
            }
        }
        if (meter == null) {
            // ⚠️ 布隆过滤器假阳性兜底（~1%）：DB 确实不存在 → 缓存空值防止后续穿透
            cacheNullValueHelper.cacheNull("meter", id, 60);
            throw BusinessException.of(404, "表计不存在");
        }
        checkOwnership(meter);
        MeterResponse response = toResponse(meter);
        // 详情接口补充分时电表的今日有功电量（与列表页保持一致）
        if (Integer.valueOf(1).equals(meter.getMeterType())) {
            Map<Long, BigDecimal> todayMap = buildTodayActiveEnergyMap(List.of(meter));
            response.setTodayActiveEnergy(todayMap.get(meter.getId()));
        }
        // 详情接口直接携带最新读数（功率与累计电量），前端无需依赖曲线接口才能显示。
        MeterReading latest = meterReadingMapper.getLatestByMeterId(id);
        if (latest != null) {
            MeterResponse.LatestReading latestReading = new MeterResponse.LatestReading();
            latestReading.setTimestamp(latest.getTimestamp());
            latestReading.setPower(latest.getAct());
            latestReading.setKwh(latest.getKwh());
            latestReading.setFact(latest.getFact());
            latestReading.setNegativeActive(toBigDecimal(latest.getFxyg()));
            latestReading.setCombinedActive(toBigDecimal(latest.getZhyg()));
            latestReading.setKwhPeak(latest.getKwhPeak());
            latestReading.setKwhBusy(latest.getKwhBusy());
            latestReading.setKwhNormal(latest.getKwhNormal());
            latestReading.setKwhIdle(latest.getKwhIdle());
            latestReading.setAmpA(latest.getAmpA());
            latestReading.setAmpB(latest.getAmpB());
            latestReading.setAmpC(latest.getAmpC());
            latestReading.setTimeInterval(latest.getTimeInterval());
            response.setLatestReading(latestReading);
            response.setCurrentActiveEnergy(latest.getKwh());
        }
        return response;
    }

    // ──────────────── 新增 ────────────────

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MeterResponse create(MeterRequest request) {
        // ── 自动生成表计编号（dbNo），甲方可修改 application.yml 中的 meter.no.* 配置项来改变编号格式 ──
        if (!StringUtils.hasText(request.getDbNo())) {
            Integer meterType = request.getMeterType() != null ? request.getMeterType() : 0;
            String prefix = dbNoGenerator.getPrefix(meterType);
            long maxSeq = meterMapper.getMaxSeqByPrefix(prefix);
            String generatedDbNo = dbNoGenerator.generate(meterType, maxSeq + 1);
            request.setDbNo(generatedDbNo);
            log.info("自动生成表计编号: meterType={}, dbNo={}", meterType, generatedDbNo);
        }
        // 校验：至少提供一种表计标识（钢印号/电表号/水表编号）
        if (!StringUtils.hasText(request.getStealNo())
                && !StringUtils.hasText(request.getDbNo())
                && !StringUtils.hasText(request.getMeteraddress())) {
            throw BusinessException.of("表钢印号、电表号、水表编号至少填写一项");
        }
        // 解析商户名称 → customerId
        resolveCustomerName(request);
        // 非 ADMIN 强制绑定当前商户
        if (!SecurityUserContext.isAdmin()) {
            Long myCustomerId = SecurityUserContext.getCurrentCustomerId();
            if (myCustomerId == null) {
                throw BusinessException.of(403, "无权限新增表计");
            }
            request.setCustomerId(myCustomerId);
        }
        Meter meter = new Meter();
        applyRequestToMeter(request, meter);
        meter.setMeterKind("PHYSICAL");
        meter.setCreateTime(LocalDateTime.now());
        meter.setUpdateTime(LocalDateTime.now());
        meterMapper.insert(meter);
        // 新表计加入布隆过滤器
        bloomFilterHelper.add(String.valueOf(meter.getId()));
        // 清除可能存在的空值缓存（之前查询过该 ID 不存在）
        cacheNullValueHelper.evictNull("meter", meter.getId());
        return toResponse(meter);
    }

    // ──────────────── 编辑 ────────────────

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MeterResponse update(Long id, MeterRequest request) {
        Meter meter = meterMapper.selectById(id);
        if (meter == null) {
            throw BusinessException.of("表计不存在");
        }
        checkOwnership(meter);
        // 解析商户名称 → customerId（仅 ADMIN 有效，非 ADMIN 会被覆盖）
        resolveCustomerName(request);
        // 非 ADMIN 不允许修改 customerId
        if (!SecurityUserContext.isAdmin()) {
            request.setCustomerId(meter.getCustomerId());
        }
        // 编辑时未显式传商户信息（customerName/customerId），保留原商户绑定，避免被清空
        if (request.getCustomerId() == null) {
            request.setCustomerId(meter.getCustomerId());
        }
        // 前端编辑未回传表计类型时，沿用原表类型，保证 CT/PT 等类型相关字段能正常更新
        if (request.getMeterType() == null) {
            request.setMeterType(meter.getMeterType());
        }
        // 编辑接口只允许修改编辑弹窗中的字段，不能用“新增表计”的全量映射覆盖
        // 未提交的采集、费率、档案等字段。
        applyEditableRequestToMeter(request, meter);
        meter.setUpdateTime(LocalDateTime.now());
        meterMapper.updateById(meter);
        return toResponse(meter);
    }

    /** 编辑弹窗的增量字段映射，保留请求中未出现的原始表计数据。 */
    private void applyEditableRequestToMeter(MeterRequest src, Meter target) {
        if (src.getBjlx() != null && (src.getBjlx() < 1 || src.getBjlx() > 6)) {
            throw BusinessException.of(400, "表计类型必须为1-6");
        }
        if (src.getStatus() != null && src.getStatus() != 0 && src.getStatus() != 1) {
            throw BusinessException.of(400, "表计状态只能为正常或停用");
        }
        if (src.getCtRatio() != null && src.getCtRatio().signum() <= 0) {
            throw BusinessException.of(400, "CT变比必须大于0");
        }
        if (src.getPtRatio() != null && src.getPtRatio().signum() <= 0) {
            throw BusinessException.of(400, "PT变比必须大于0");
        }
        if (src.getGroupId() != null) {
            groupAssociationService.requireLinkable(src.getGroupId(), src.getCustomerId());
            target.setGroupId(src.getGroupId());
        }
        if (src.getName() != null) target.setName(src.getName());
        if (src.getCustomerName() != null && src.getName() == null) target.setName(src.getCustomerName());
        if (src.getBjlx() != null) target.setBjlx(src.getBjlx());
        if (src.getStatus() != null) target.setStatus(src.getStatus());
        if (src.getCtRatio() != null) target.setCtRatio(src.getCtRatio());
        if (src.getPtRatio() != null) target.setPtRatio(src.getPtRatio());
        if (src.getCustomerId() != null) target.setCustomerId(src.getCustomerId());
    }

    // ──────────────── 删除 ────────────────

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        Meter meter = meterMapper.selectById(id);
        if (meter == null) {
            throw BusinessException.of("表计不存在");
        }
        checkOwnership(meter);
        // 使用自定义逻辑删除（绕过 MyBatis-Plus @TableLogic 的 update_time 空值问题）
        meterMapper.logicDeleteById(id);
    }

    // ──────────────── 状态变更 ────────────────

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long id, Integer status) {
        Meter meter = meterMapper.selectById(id);
        if (meter == null) {
            throw BusinessException.of(400, "表计不存在");
        }
        checkOwnership(meter);
        int rows = meterMapper.updateStatus(id, status);
        if (rows == 0) {
            throw BusinessException.of("表计状态更新失败");
        }
    }

    // ──────────────── 绑定商户 ────────────────

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void bindCustomer(Long meterId, Long customerId) {
        Meter meter = meterMapper.selectById(meterId);
        if (meter == null) {
            throw BusinessException.of(400, "表计不存在");
        }
        // 仅 ADMIN 可执行绑定操作
        if (!SecurityUserContext.isAdmin()) {
            throw BusinessException.of(403, "仅管理员可执行表计绑定操作");
        }
        // 校验商户存在
        Customer customer = customerMapper.selectById(customerId);
        if (customer == null) {
            throw BusinessException.of(400, "商户不存在");
        }
        meter.setCustomerId(customerId);
        meter.setUpdateTime(LocalDateTime.now());
        meterMapper.updateById(meter);
    }

    // ──────────────── 批量导入 ────────────────

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int batchImport(List<MeterRequest> meters) {
        if (meters == null || meters.isEmpty()) return 0;
        // 批量插入：在同一个事务内执行，使用 MyBatis-Plus 逐条 insert
        // （BaseMapper 不支持批量 insert，如需极致性能可添加 insertBatch 自定义 SQL）
        LocalDateTime now = LocalDateTime.now();
        for (MeterRequest req : meters) {
            // 批量导入同样按商户名自动关联/开户，保证导入后即可充值缴费
            resolveCustomerName(req);
            Meter meter = new Meter();
            applyRequestToMeter(req, meter);
            meter.setCreateTime(now);
            meter.setUpdateTime(now);
            meterMapper.insert(meter);
        }
        return meters.size();
    }

    // ──────────────── 余额清零 ────────────────

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BigDecimal clearBalance(ClearBalanceRequest request) {
        // 仅 ADMIN 可执行余额清零
        if (!SecurityUserContext.isAdmin()) {
            throw BusinessException.of(403, "仅管理员可执行余额清零操作");
        }

        Long meterId = request.getMeterId();
        Meter meter = meterMapper.selectById(meterId);
        if (meter == null) {
            throw BusinessException.of(400, "表计不存在");
        }
        if (meter.getCustomerId() == null) {
            throw BusinessException.of(400, "表计未绑定商户，无法清零");
        }

        Customer customer = customerMapper.selectById(meter.getCustomerId());
        if (customer == null) {
            throw BusinessException.of(400, "关联商户不存在");
        }

        BigDecimal balanceBefore = customer.getBalance();
        if (balanceBefore == null) {
            throw BusinessException.of(400, "商户余额数据异常，无法清零");
        }
        if (balanceBefore.compareTo(BigDecimal.ZERO) == 0) {
            throw BusinessException.of(400, "余额已为0，无需清零");
        }

        // ① 原子扣减：将余额减至0
        int rows = customerMapper.subtractBalance(customer.getId(), balanceBefore);
        if (rows == 0) {
            throw WalletException.concurrencyConflict(customer.getId(), "余额清零");
        }

        // ② 写入资金流水（trade_type=6 余额清零）
        String flowNo = generateFlowNo("CLR");
        AccountFlow flow = new AccountFlow();
        flow.setFlowNo(flowNo);
        flow.setCustomerId(customer.getId());
        flow.setMeterId(meterId);
        flow.setBalanceBefore(balanceBefore);
        flow.setChangeAmount(balanceBefore.negate());
        flow.setBalanceAfter(BigDecimal.ZERO);
        flow.setTradeType(6);
        flow.setBizNo(flowNo);
        flow.setOperatorName(SecurityUserContext.getCurrentUsername());
        flow.setOperateTime(LocalDateTime.now());
        flow.setRemark("余额清零 - " + request.getReason());
        flow.setCreateTime(LocalDateTime.now());
        flow.setUpdateTime(LocalDateTime.now());
        accountFlowMapper.insert(flow);

        // ③ 更新表计状态为欠费停机（status=2）
        meterMapper.updateStatus(meterId, 2);

        // ④ 触发关阀 Hook
        valveControlHook.syncAfterClearBalance(meterId, meter.getStealNo(),
                SecurityUserContext.getCurrentUsername());

        log.warn("[余额清零] meterId={}, customerId={}, balanceBefore={}, reason={}, operator={}",
                meterId, customer.getId(), balanceBefore, request.getReason(),
                SecurityUserContext.getCurrentUsername());

        return balanceBefore;
    }

    // ──────────────── 批量修改电价 ────────────────

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BatchUpdatePriceResponse batchUpdatePrice(BatchUpdatePriceRequest request) {
        // 仅 ADMIN 可执行批量修改电价
        if (!SecurityUserContext.isAdmin()) {
            throw BusinessException.of(403, "仅管理员可执行批量电价修改");
        }

        int total;
        int success;

        boolean isGlobal = Boolean.TRUE.equals(request.getIsGlobal());
        Integer meterType = request.getMeterType();

        if (isGlobal && (request.getMeterIds() == null || request.getMeterIds().isEmpty())) {
            // 全局修改（可选按meterType过滤）
            LambdaQueryWrapper<Meter> countWrapper = new LambdaQueryWrapper<Meter>()
                    .eq(Meter::getIsDeleted, 0);
            if (meterType != null) countWrapper.eq(Meter::getMeterType, meterType);
            total = Math.toIntExact(meterMapper.selectCount(countWrapper));
            success = meterMapper.batchUpdatePriceGlobal(
                    request.getPriceType(), request.getPriceContent(),
                    request.getPlusPrice(), request.getRateConfigId(), meterType);
        } else if (request.getCustomerId() != null) {
            // 按商户批量修改（可选按meterType过滤）
            Customer customer = customerMapper.selectById(request.getCustomerId());
            if (customer == null) {
                throw BusinessException.of(400, "商户不存在");
            }
            LambdaQueryWrapper<Meter> countWrapper = new LambdaQueryWrapper<Meter>()
                    .eq(Meter::getCustomerId, request.getCustomerId())
                    .eq(Meter::getIsDeleted, 0);
            if (meterType != null) countWrapper.eq(Meter::getMeterType, meterType);
            total = Math.toIntExact(meterMapper.selectCount(countWrapper));
            success = meterMapper.batchUpdatePriceByCustomer(
                    request.getCustomerId(), request.getPriceType(),
                    request.getPriceContent(), request.getPlusPrice(), request.getRateConfigId(), meterType);
        } else if (request.getMeterIds() != null && !request.getMeterIds().isEmpty()) {
            // 按表计ID列表修改（可选按meterType过滤）
            total = request.getMeterIds().size();
            success = meterMapper.batchUpdatePrice(
                    request.getMeterIds(), request.getPriceType(),
                    request.getPriceContent(), request.getPlusPrice(), request.getRateConfigId(), meterType);
        } else {
            throw BusinessException.of(400, "请指定表计ID列表、商户ID，或开启全局修改");
        }

        log.info("[批量修改电价] total={}, success={}, priceType={}, operator={}",
                total, success, request.getPriceType(), SecurityUserContext.getCurrentUsername());

        return new BatchUpdatePriceResponse(total, success, total - success);
    }

    // ──────────────── 批量修改电价（拆分后） ────────────────

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BatchUpdatePriceResponse batchUpdateElectricityPrice(BatchPriceRequest request) {
        return doBatchUpdatePrice(request, 1); // meterType=1 仅电表
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BatchUpdatePriceResponse batchUpdateWaterPrice(BatchPriceRequest request) {
        return doBatchUpdatePrice(request, 0); // meterType=0 仅水表
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BatchUpdatePriceResponse globalUpdateElectricityPrice(GlobalPriceRequest request) {
        return doGlobalUpdatePrice(request, 1); // meterType=1 仅电表
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BatchUpdatePriceResponse globalUpdateWaterPrice(GlobalPriceRequest request) {
        return doGlobalUpdatePrice(request, 0); // meterType=0 仅水表
    }

    /**
     * 批量修改单价核心逻辑（按表计ID列表或按商户）。
     */
    private BatchUpdatePriceResponse doBatchUpdatePrice(BatchPriceRequest request, Integer meterType) {
        if (!SecurityUserContext.isAdmin()) {
            throw BusinessException.of(403, "仅管理员可执行批量单价修改");
        }

        int total;
        int success;
        String typeLabel = meterType == 1 ? "电价" : "水价";
        if (request.getRateConfigId() != null) {
            validatePriceStrategyMeterType(request.getRateConfigId(), meterType);
        }

        if (request.getCustomerId() != null) {
            // 按商户批量修改
            Customer customer = customerMapper.selectById(request.getCustomerId());
            if (customer == null) {
                throw BusinessException.of(400, "商户不存在");
            }
            LambdaQueryWrapper<Meter> countWrapper = new LambdaQueryWrapper<Meter>()
                    .eq(Meter::getCustomerId, request.getCustomerId())
                    .eq(Meter::getIsDeleted, 0);
            if (meterType != null) countWrapper.eq(Meter::getMeterType, meterType);
            total = Math.toIntExact(meterMapper.selectCount(countWrapper));
            success = meterMapper.batchUpdatePriceByCustomer(
                    request.getCustomerId(), request.getPriceType(),
                    request.getPriceContent(), request.getPlusPrice(), request.getRateConfigId(), meterType);
        } else if (request.getMeterIds() != null && !request.getMeterIds().isEmpty()) {
            // 按表计ID列表修改
            total = request.getMeterIds().size();
            success = meterMapper.batchUpdatePrice(
                    request.getMeterIds(), request.getPriceType(),
                    request.getPriceContent(), request.getPlusPrice(), request.getRateConfigId(), meterType);
        } else {
            throw BusinessException.of(400, "请指定表计ID列表或商户ID");
        }

        log.info("[批量修改{}] total={}, success={}, priceType={}, operator={}",
                typeLabel, total, success, request.getPriceType(), SecurityUserContext.getCurrentUsername());

        return new BatchUpdatePriceResponse(total, success, total - success);
    }

    /**
     * 全局修改单价核心逻辑（全园区所有电表或水表）。
     */
    private BatchUpdatePriceResponse doGlobalUpdatePrice(GlobalPriceRequest request, Integer meterType) {
        if (!SecurityUserContext.isAdmin()) {
            throw BusinessException.of(403, "仅管理员可执行全局单价修改");
        }

        String typeLabel = meterType == 1 ? "电价" : "水价";
        if (request.getRateConfigId() != null) {
            validatePriceStrategyMeterType(request.getRateConfigId(), meterType);
        }

        LambdaQueryWrapper<Meter> countWrapper = new LambdaQueryWrapper<Meter>()
                .eq(Meter::getIsDeleted, 0);
        if (meterType != null) countWrapper.eq(Meter::getMeterType, meterType);
        int total = Math.toIntExact(meterMapper.selectCount(countWrapper));
        int success = meterMapper.batchUpdatePriceGlobal(
                request.getPriceType(), request.getPriceContent(),
                request.getPlusPrice(), request.getRateConfigId(), meterType);

        log.info("[全局修改{}] total={}, success={}, priceType={}, operator={}",
                typeLabel, total, success, request.getPriceType(), SecurityUserContext.getCurrentUsername());

        return new BatchUpdatePriceResponse(total, success, total - success);
    }

    // ──────────────── 已删除电表列表 ────────────────

    @Override
    public IPage<MeterResponse> listDeleted(DeletedMeterPageRequest request) {
        Long customerId = null;
        if (!SecurityUserContext.isAdmin()) {
            customerId = SecurityUserContext.getCurrentCustomerId();
            if (customerId == null) {
                throw BusinessException.of(403, "无权限查看已删除电表");
            }
        }

        int pageNum = request.getPageNum() != null ? request.getPageNum() : 1;
        int pageSize = request.getPageSize() != null ? request.getPageSize() : 20;

        // 调用增强后的动态 SQL，支持 keyword/meterType/status/groupId/时间范围筛选
        List<Meter> allDeleted = meterMapper.selectDeletedList(
                customerId,
                request.getKeyword(),
                request.getMeterType(),
                request.getStatus(),
                request.getGroupId(),
                request.getStartTime(),
                request.getEndTime());

        // 手动分页
        int total = allDeleted.size();
        int fromIndex = (pageNum - 1) * pageSize;
        int toIndex = Math.min(fromIndex + pageSize, total);

        List<Meter> pageContent;
        if (fromIndex >= total) {
            pageContent = List.of();
        } else {
            pageContent = allDeleted.subList(fromIndex, toIndex);
        }

        List<MeterResponse> records = new ArrayList<>(pageContent.size());
        Map<Long, Customer> customerMap = buildCustomerMap(pageContent);
        for (Meter meter : pageContent) {
            records.add(toResponse(meter, customerMap));
        }

        Page<MeterResponse> page = new Page<>(pageNum, pageSize, total);
        page.setRecords(records);
        return page;
    }

    @Override
    public MeterResponse getDeletedById(Long id) {
        // 直接查询已删除电表（不含正常电表）
        Meter meter = meterMapper.selectByIdIncludingDeleted(id);
        if (meter == null || meter.getIsDeleted() == null || meter.getIsDeleted() != 1) {
            throw BusinessException.of(404, "已删除电表不存在");
        }
        checkOwnership(meter);
        return toResponse(meter);
    }

    // ──────────────── 恢复已删除电表 ────────────────

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void restore(Long id) {
        if (!SecurityUserContext.isAdmin()) {
            throw new BusinessException(403, "仅管理员可执行电表恢复操作");
        }

        // 绕过 @TableLogic 直接查询，确认电表存在且处于已删除状态
        Meter meter = meterMapper.selectByIdIncludingDeleted(id);
        if (meter == null) {
            throw new BusinessException(404, "电表不存在");
        }
        if (meter.getIsDeleted() == null || meter.getIsDeleted() != 1) {
            throw new BusinessException(400, "当前电表未被删除，无需恢复");
        }

        int rows = meterMapper.restoreById(id);
        if (rows == 0) {
            throw BusinessException.of("电表恢复失败");
        }

        log.warn("[电表恢复] id={}, stealNo={}, operator={}",
                id, meter.getStealNo(), SecurityUserContext.getCurrentUsername());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int batchRestore(List<Long> ids) {
        if (!SecurityUserContext.isAdmin()) {
            throw BusinessException.of(403, "仅管理员可执行电表恢复操作");
        }
        if (ids == null || ids.isEmpty()) {
            throw BusinessException.of(400, "请至少选择一条已删除电表");
        }

        int rows = meterMapper.batchRestoreByIds(ids);
        log.warn("[电表批量恢复] ids={}, count={}, operator={}",
                ids, rows, SecurityUserContext.getCurrentUsername());
        return rows;
    }

    // ──────────────── 远程开阀（合闸/通电） ────────────────

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void openValve(Long id, String reason) {
        if (!SecurityUserContext.isAdmin()) {
            throw BusinessException.of(403, "仅管理员可执行远程开阀操作");
        }

        Meter meter = meterMapper.selectById(id);
        if (meter == null) {
            throw BusinessException.of(400, "表计不存在");
        }

        // 更新分合闸状态：1=合闸（开阀）
        meterMapper.updateValveState(id, 1, 1);

        // 下发硬件开阀指令
        valveControlHook.openValve(id, meter.getStealNo(),
                SecurityUserContext.getCurrentUsername(), reason);

        // 写入操作日志
        writeValveOperationLog(meter, "VALVE_OPEN", reason);

        log.warn("[远程开阀] meterId={}, stealNo={}, reason={}, operator={}",
                id, meter.getStealNo(), reason, SecurityUserContext.getCurrentUsername());
    }

    // ──────────────── 远程关阀（分闸/断电） ────────────────

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void closeValve(Long id, String reason) {
        if (!SecurityUserContext.isAdmin()) {
            throw BusinessException.of(403, "仅管理员可执行远程关阀操作");
        }

        Meter meter = meterMapper.selectById(id);
        if (meter == null) {
            throw BusinessException.of(400, "表计不存在");
        }

        // 更新分合闸状态：2=分闸（关阀），同时设状态为欠费停机
        meterMapper.updateValveState(id, 2, 2);

        // 下发硬件关阀指令
        valveControlHook.closeValve(id, meter.getStealNo(),
                SecurityUserContext.getCurrentUsername(), reason);

        // 写入操作日志
        writeValveOperationLog(meter, "VALVE_CLOSE", reason);

        log.warn("[远程关阀] meterId={}, stealNo={}, reason={}, operator={}",
                id, meter.getStealNo(), reason, SecurityUserContext.getCurrentUsername());
    }

    // ──────────────── 私有方法 ────────────────

    /**
     * 校验当前用户是否有权操作该表计。
     * <p>
     * ADMIN 可操作任意表计；非 ADMIN 仅可操作属于自己商户的表计。
     * 校验不通过时抛出 403 {@link BusinessException}。
     */
    private void checkOwnership(Meter meter) {
        if (SecurityUserContext.isAdmin()) {
            return;
        }
        Long myCustomerId = SecurityUserContext.getCurrentCustomerId();
        if (myCustomerId == null || !myCustomerId.equals(meter.getCustomerId())) {
            throw BusinessException.of(403, "无权操作该表计");
        }
    }

    /**
     * 生成业务流水号。
     */
    private String generateFlowNo(String prefix) {
        String ts = String.valueOf(System.currentTimeMillis());
        String shortTs = ts.substring(ts.length() - 8);
        String shortUuid = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        return prefix + shortTs + shortUuid;
    }

    /**
     * 写入阀门操作审计日志。
     */
    private void writeValveOperationLog(Meter meter, String operationType, String reason) {
        OperationLog logEntry = new OperationLog();
        logEntry.setOperatorName(SecurityUserContext.getCurrentUsername());
        logEntry.setModuleName("表计管理");
        logEntry.setOperationType(operationType);
        logEntry.setOperationDesc(String.format("表计[%s/%s] %s，原因：%s",
                meter.getStealNo() != null ? meter.getStealNo() : meter.getDbNo(),
                meter.getName(),
                "VALVE_OPEN".equals(operationType) ? "远程合闸" : "远程分闸",
                reason));
        logEntry.setRequestParams(String.format("{\"meterId\":%d,\"reason\":\"%s\"}", meter.getId(), reason));
        logEntry.setResponseResult("{\"result\":\"success\"}");
        logEntry.setOperationTime(LocalDateTime.now());
        logEntry.setCreateTime(LocalDateTime.now());
        operationLogMapper.insert(logEntry);
    }

    /**
     * 根据表计「商户名」自动关联商户（商户 ID）。
     * <p>商户名优先取 name（表计管理里编辑的商户名），其次取 customerName（历史字段）。
     * 存在同名商户则直接关联；不存在则自动开户，使「表计管理」里的商户名
     * 与「电表客户信息」里的商户名称保持一致，从而让表计可正常充值/缴费。</p>
     */
    private void resolveCustomerName(MeterRequest request) {
        // 商户名优先取 customerName（商户名称），其次取 name（表计管理里编辑的商户名）
        String merchantName = StringUtils.hasText(request.getCustomerName())
                ? request.getCustomerName().trim()
                : (StringUtils.hasText(request.getName()) ? request.getName().trim() : null);
        if (merchantName == null) {
            return; // 没写商户名，不做处理
        }
        // 按商户名称精确匹配（未删除）
        Customer customer = customerMapper.selectOne(
                new LambdaQueryWrapper<Customer>()
                        .eq(Customer::getCustomerName, merchantName)
                        .eq(Customer::getIsDeleted, 0)
                        .last("LIMIT 1"));
        if (customer == null) {
            // 自动开户：商户名写入商户档案，并绑定到当前表计
            customer = new Customer();
            customer.setCustomerName(merchantName);
            customer.setCustomerCode(generateCustomerCode());
            customer.setStatus(1);
            customer.setBalance(BigDecimal.ZERO);
            customer.setCreateTime(LocalDateTime.now());
            customer.setUpdateTime(LocalDateTime.now());
            customerMapper.insert(customer);
        }
        request.setCustomerId(customer.getId());
    }

    /** 自动开户时生成唯一用户编号（KH + 时间戳 + 随机后缀）。 */
    private String generateCustomerCode() {
        String code;
        do {
            code = "KH" + System.currentTimeMillis() + (int) (Math.random() * 1000);
        } while (customerMapper.selectCount(
                new LambdaQueryWrapper<Customer>().eq(Customer::getCustomerCode, code)) > 0);
        return code;
    }

    /**
     * 将请求参数写入实体
     */
    private void applyRequestToMeter(MeterRequest src, Meter target) {
        groupAssociationService.requireLinkable(src.getGroupId(), src.getCustomerId());
        target.setCustomerId(src.getCustomerId());
        target.setGroupId(src.getGroupId());
        // 积成
        target.setStealNo(src.getStealNo());
        target.setVender(src.getVender());
        target.setMeterTypeName(src.getMeterTypeName());
        target.setCaliber(src.getCaliber());
        target.setSwitchOp(src.getSwitchOp());
        target.setStartVal(src.getStartVal());
        target.setTerminalName(src.getTerminalName());
        target.setChannelType(src.getChannelType());
        target.setMeterPhone(src.getMeterPhone());
        target.setTelecommSn(src.getTelecommSn());
        target.setImei(src.getImei());
        target.setIccid(src.getIccid());
        target.setAddress(src.getAddress());
        target.setType(src.getType());
        // 辰鳗
        target.setDbNo(src.getDbNo());
        target.setName(src.getName());
        target.setBei(src.getBei());
        target.setBjlx(src.getBjlx());
        target.setMeterType(src.getMeterType());
        target.setCredit(src.getCredit());
        target.setWarnline(src.getWarnline());
        target.setCreatuser(src.getCreatuser());
        target.setCttime(src.getCttime());
        target.setFeetype(src.getFeetype());
        target.setProId(src.getProId());
        target.setProName(src.getProName());
        target.setPlusPrice(src.getPlusPrice());
        target.setPriceType(src.getPriceType());
        target.setPriceContent(src.getPriceContent());
        target.setJoinPhone(src.getJoinPhone());
        target.setCollectorDeviceNo(src.getCollectorDeviceNo());
        target.setPhase(src.getPhase());
        // 捷先
        target.setMeteraddress(src.getMeteraddress());
        target.setCompanycode(src.getCompanycode());
        target.setWaterVendorId(src.getWaterVendorId());
        target.setCustomerNo(src.getCustomerNo());
        target.setMeterCaliber(src.getMeterCaliber());
        target.setControlling(src.getControlling());
        target.setStateValve(src.getStateValve());
        target.setSignalStrength(src.getSignalStrength());
        target.setBatteryVoltage(src.getBatteryVoltage());
        // 通用
        target.setStatus(src.getStatus());
        target.setOnline(src.getOnline());
        target.setMeterSignal(src.getMeterSignal());
        // 通信参数
        target.setCollectorCode(src.getCollectorCode());
        target.setBpsType(src.getBpsType());
        target.setBitType(src.getBitType());
        target.setProtoType(src.getProtoType());
        target.setSerial(src.getSerial());
        target.setDeviceType(src.getDeviceType());
        // 费率策略
        target.setRateConfigId(src.getRateConfigId());
        // CT/PT仅属于电表；历史空值兼容为1倍，显式非法值拒绝
        if (Integer.valueOf(1).equals(src.getMeterType())) {
            target.setCtRatio(validateRatio(src.getCtRatio(), "CT"));
            target.setPtRatio(validateRatio(src.getPtRatio(), "PT"));
        }
        // 标记与告警
        target.setMarkStatus(src.getMarkStatus());
        target.setEnabledTime(src.getEnabledTime());
        target.setDisableWarning(src.getDisableWarning());
        target.setCommStatusDesc(src.getCommStatusDesc());
    }

    private BigDecimal validateRatio(BigDecimal value, String label) {
        if (value == null) return BigDecimal.ONE;
        if (value.signum() <= 0 || value.scale() > 4 || value.compareTo(new BigDecimal("999999.9999")) > 0) {
            throw BusinessException.of(400, label + "变比必须是大于0且不超过999999.9999的有效数字");
        }
        return value;
    }

    private BigDecimal toBigDecimal(Double value) {
        return value == null ? null : BigDecimal.valueOf(value);
    }

    /**
     * 批量预加载商户 Map（避免 N+1 查询）。
     */
    private Map<Long, Customer> buildCustomerMap(List<Meter> entities) {
        Set<Long> ids = entities.stream()
                .map(Meter::getCustomerId)
                .filter(id -> id != null)
                .collect(Collectors.toSet());
        if (ids.isEmpty()) return Map.of();
        return customerMapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(Customer::getId, c -> c, (a, b) -> a));
    }

    /**
     * 实体 → 响应（单条查询用，内部委托到批量版本）。
     */
    private MeterResponse toResponse(Meter meter) {
        Map<Long, Customer> cm;
        if (meter.getCustomerId() != null) {
            List<Customer> list = customerMapper.selectBatchIds(List.of(meter.getCustomerId()));
            cm = list.stream().collect(Collectors.toMap(Customer::getId, c -> c, (a, b) -> a));
        } else {
            cm = Map.of();
        }
        return toResponse(meter, cm);
    }

    /**
     * 实体 → 响应（批量查询用，从预加载的 Map 取值，不再查 DB）。
     */
    private MeterResponse toResponse(Meter meter, Map<Long, Customer> customerMap) {
        return toResponse(meter, customerMap, Map.of());
    }

    private MeterResponse toResponse(Meter meter, Map<Long, Customer> customerMap,
                                     Map<Long, BigDecimal> todayActiveEnergyMap) {
        return toResponse(meter, customerMap, todayActiveEnergyMap, Map.of());
    }

    private MeterResponse toResponse(Meter meter, Map<Long, Customer> customerMap,
                                     Map<Long, BigDecimal> todayActiveEnergyMap,
                                     Map<Long, BigDecimal> currentActiveEnergyMap) {
        MeterResponse resp = new MeterResponse();
        resp.setId(meter.getId());
        resp.setMeterKind(meter.getMeterKind());
        resp.setCustomerId(meter.getCustomerId());
        resp.setCreateTime(meter.getCreateTime());
        resp.setUpdateTime(meter.getUpdateTime());
        resp.setCreateBy(meter.getCreateBy());
        resp.setTodayActiveEnergy(todayActiveEnergyMap.get(meter.getId()));
        resp.setCurrentActiveEnergy(currentActiveEnergyMap.get(meter.getId()));

        // 从预加载的 Map 中获取商户名称和余额
        if (meter.getCustomerId() != null) {
            Customer customer = customerMap.get(meter.getCustomerId());
            if (customer != null) {
                resp.setCustomerName(customer.getCustomerName());
                resp.setCustomerBalance(customer.getBalance());
            }
        }

        // 积成
        MeterResponse.Jicheng jc = new MeterResponse.Jicheng();
        BeanUtils.copyProperties(meter, jc);
        resp.setJicheng(jc);

        // 辰鳗
        MeterResponse.Chenman cm = new MeterResponse.Chenman();
        BeanUtils.copyProperties(meter, cm);
        resp.setChenman(cm);

        // 捷先
        MeterResponse.Jiexian jx = new MeterResponse.Jiexian();
        BeanUtils.copyProperties(meter, jx);
        resp.setJiexian(jx);

        // 通用状态
        MeterResponse.General general = new MeterResponse.General();
        BeanUtils.copyProperties(meter, general);
        general.setMeterType(meter.getMeterType());
        general.setCtRatio(meter.getCtRatio());
        general.setPtRatio(meter.getPtRatio());
        general.setMarkStatus(meter.getMarkStatus());
        general.setEnabledTime(meter.getEnabledTime());
        general.setDisableWarning(meter.getDisableWarning());
        general.setCommStatusDesc(meter.getCommStatusDesc());
        populateGroupInfo(general, meter.getGroupId(), meter.getCustomerId());
        resp.setGeneral(general);

        // 通信参数
        MeterResponse.CommParams comm = new MeterResponse.CommParams();
        BeanUtils.copyProperties(meter, comm);
        resp.setCommParams(comm);

        return resp;
    }

    private String normalizeMeterKind(String meterKind) {
        if (!StringUtils.hasText(meterKind)) {
            return null;
        }
        String normalized = meterKind.trim().toUpperCase(java.util.Locale.ROOT);
        if (!"PHYSICAL".equals(normalized) && !"VIRTUAL".equals(normalized)) {
            throw BusinessException.of(400, "表计性质仅支持 PHYSICAL 或 VIRTUAL");
        }
        return normalized;
    }

    private void populateGroupInfo(MeterResponse.General general, Long groupId, Long merchantId) {
        GroupAssociationService.GroupAssociation association =
                groupAssociationService.describe(groupId, merchantId);
        general.setGroupName(association.name());
        general.setGroupPath(association.path());
        general.setGroupStatus(association.status());
        general.setGroupRelationStatus(association.relationStatus());
    }

    private Map<Long, BigDecimal> buildTodayActiveEnergyMap(List<Meter> meters) {
        List<Long> meterIds = meters.stream().map(Meter::getId).filter(id -> id != null).toList();
        if (meterIds.isEmpty()) return Map.of();
        return meterReadingMapper.selectTodayActiveEnergy(meterIds).stream()
                .filter(row -> row.get("meterId") != null && row.get("todayActiveEnergy") != null)
                .collect(Collectors.toMap(
                        row -> ((Number) row.get("meterId")).longValue(),
                        row -> new BigDecimal(row.get("todayActiveEnergy").toString()),
                        (left, right) -> right));
    }

    private Map<Long, BigDecimal> buildCurrentActiveEnergyMap(List<Meter> meters) {
        List<Long> meterIds = meters.stream().map(Meter::getId).filter(id -> id != null).toList();
        if (meterIds.isEmpty()) return Map.of();
        return meterReadingMapper.selectLatestActiveEnergy(meterIds).stream()
                .filter(row -> row.get("meterId") != null && row.get("currentActiveEnergy") != null)
                .collect(Collectors.toMap(
                        row -> ((Number) row.get("meterId")).longValue(),
                        row -> new BigDecimal(row.get("currentActiveEnergy").toString()),
                        (left, right) -> right));
    }

    // ──────────────── 批量修改表计信息 ────────────────

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BatchOperationResponse batchUpdateMeterInfo(BatchUpdateMeterRequest request) {
        if (!SecurityUserContext.isAdmin()) {
            throw BusinessException.of(403, "仅管理员可执行批量修改表计信息");
        }

        int total = request.getMeterIds().size();
        int success = meterMapper.batchUpdateMeterInfo(
                request.getMeterIds(),
                request.getName(),
                request.getGroupId(),
                request.getBjlx(),
                request.getCtRatio(),
                request.getPtRatio(),
                request.getMarkStatus(),
                request.getDisableWarning(),
                request.getCommStatusDesc(),
                request.getStatus(),
                request.getAddress());

        log.info("[批量修改表计信息] total={}, success={}, operator={}",
                total, success, SecurityUserContext.getCurrentUsername());

        return new BatchOperationResponse(total, success, total - success,
                String.format("批量修改完成：共 %d 台，成功 %d 台，失败 %d 台", total, success, total - success));
    }

    // ──────────────── 批量导出表计数据 ────────────────

    @Override
    public List<MeterResponse> exportMeterList(MeterPageRequest request) {
        // 使用分页查询但设置较大的pageSize来获取全量数据用于导出
        LambdaQueryWrapper<Meter> wrapper = new LambdaQueryWrapper<>();

        boolean isAdmin = SecurityUserContext.isAdmin();
        if (!isAdmin) {
            Long myCustomerId = SecurityUserContext.getCurrentCustomerId();
            if (myCustomerId == null) {
                throw BusinessException.of(403, "无权限导出表计列表");
            }
            wrapper.eq(Meter::getCustomerId, myCustomerId);
        } else {
            wrapper.eq(request.getCustomerId() != null, Meter::getCustomerId, request.getCustomerId());
        }

        wrapper.eq(StringUtils.hasText(request.getStealNo()), Meter::getStealNo, request.getStealNo())
               .eq(StringUtils.hasText(request.getDbNo()), Meter::getDbNo, request.getDbNo())
               .eq(StringUtils.hasText(request.getMeteraddress()), Meter::getMeteraddress, request.getMeteraddress())
               .like(StringUtils.hasText(request.getName()), Meter::getName, request.getName())
               .eq(request.getProjectId() != null, Meter::getProId, request.getProjectId())
               .eq(request.getStatus() != null, Meter::getStatus, request.getStatus())
               .eq(request.getOnline() != null, Meter::getOnline, request.getOnline())
               .eq(request.getBjlx() != null, Meter::getBjlx, request.getBjlx())
               .eq(request.getMeterType() != null, Meter::getMeterType, request.getMeterType())
               .eq(StringUtils.hasText(request.getMarkStatus()), Meter::getMarkStatus, request.getMarkStatus())
               .eq(StringUtils.hasText(request.getCommStatusDesc()), Meter::getCommStatusDesc, request.getCommStatusDesc())
               .eq(request.getDisableWarning() != null, Meter::getDisableWarning, request.getDisableWarning())
               .eq(request.getGroupId() != null, Meter::getGroupId, request.getGroupId());

        // 厂家筛选：与列表页保持一致
        applyVendorFilter(wrapper, request.getVender());

        wrapper.orderByDesc(Meter::getCreateTime);

        List<Meter> meters = meterMapper.selectList(wrapper);
        Map<Long, Customer> customerMap = buildCustomerMap(meters);
        List<MeterResponse> result = new ArrayList<>(meters.size());
        for (Meter meter : meters) {
            result.add(toResponse(meter, customerMap));
        }
        return result;
    }

    // ──────────────── 按筛选条件导出Excel ────────────────

    @Override
    public void exportMeterListToExcel(jakarta.servlet.http.HttpServletResponse response, MeterPageRequest request) {
        List<MeterResponse> meterResponses = exportMeterList(request);
        List<MeterExcelVO> excelData = meterResponses.stream()
                .map(this::toExcelVO)
                .collect(Collectors.toList());

        writeExcel(response, excelData, "表计列表");
    }

    // ──────────────── 按ID列表批量导出Excel ────────────────

    @Override
    public void exportMeterListByIdsToExcel(jakarta.servlet.http.HttpServletResponse response, List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw BusinessException.of(400, "导出ID列表不能为空");
        }

        boolean isAdmin = SecurityUserContext.isAdmin();
        LambdaQueryWrapper<Meter> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(Meter::getId, ids);

        if (!isAdmin) {
            Long myCustomerId = SecurityUserContext.getCurrentCustomerId();
            if (myCustomerId == null) {
                throw BusinessException.of(403, "无权限导出表计数据");
            }
            wrapper.eq(Meter::getCustomerId, myCustomerId);
        }

        List<Meter> meters = meterMapper.selectList(wrapper);
        log.info("[批量导出表计] 操作人={} 请求ID数={} 实际查到={}",
                SecurityUserContext.getCurrentUsername(), ids.size(), meters.size());

        Map<Long, Customer> customerMap = buildCustomerMap(meters);
        List<MeterExcelVO> excelData = meters.stream()
                .map(m -> toExcelVO(toResponse(m, customerMap)))
                .collect(Collectors.toList());

        writeExcel(response, excelData, "表计列表_批量");
    }

    // ──────────────── Excel 写入 ────────────────

    private void writeExcel(jakarta.servlet.http.HttpServletResponse response,
                            List<MeterExcelVO> data, String sheetName) {
        try {
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setCharacterEncoding(java.nio.charset.StandardCharsets.UTF_8.name());

            String fileName = sheetName + "_" + java.time.LocalDateTime.now()
                    .format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String encodedFileName = java.net.URLEncoder.encode(fileName, java.nio.charset.StandardCharsets.UTF_8)
                    .replaceAll("\\+", "%20");
            response.setHeader("Content-Disposition",
                    "attachment; filename=" + encodedFileName + ".xlsx");
            response.setHeader("Access-Control-Expose-Headers", "Content-Disposition");

            com.alibaba.excel.EasyExcel.write(response.getOutputStream(), MeterExcelVO.class)
                    .sheet(sheetName)
                    .doWrite(data);
        } catch (java.io.IOException e) {
            log.error("[表计导出] Excel写入失败", e);
            throw BusinessException.of(500, "Excel导出失败：" + e.getMessage());
        }
    }

    // ──────────────── Meter → MeterExcelVO 转换 ────────────────

    private MeterExcelVO toExcelVO(MeterResponse resp) {
        MeterExcelVO vo = new MeterExcelVO();

        // 基本信息
        vo.setId(resp.getId());
        vo.setCustomerName(resp.getCustomerName());

        // 表计类型描述
        Integer meterType = resp.getGeneral() != null ? resp.getGeneral().getMeterType() : null;
        vo.setMeterTypeDesc(meterType == null ? "" : (meterType == 1 ? "电表" : "水表"));

        // 积成
        if (resp.getJicheng() != null) {
            MeterResponse.Jicheng jc = resp.getJicheng();
            vo.setStealNo(jc.getStealNo());
            vo.setVender(jc.getVender());
            vo.setMeterTypeName(jc.getMeterTypeName());
            vo.setCaliber(jc.getCaliber());
            vo.setSwitchOp(jc.getSwitchOp());
            vo.setStartVal(jc.getStartVal());
            vo.setTerminalName(jc.getTerminalName());
            vo.setChannelType(jc.getChannelType());
            vo.setMeterPhone(jc.getMeterPhone());
            vo.setTelecommSn(jc.getTelecommSn());
            vo.setImei(jc.getImei());
            vo.setIccid(jc.getIccid());
            vo.setAddress(jc.getAddress());
            vo.setCreateDate(jc.getCreateDate());
        }

        // 辰鳗
        if (resp.getChenman() != null) {
            MeterResponse.Chenman cm = resp.getChenman();
            vo.setDbNo(cm.getDbNo());
            vo.setName(cm.getName());
            vo.setBei(cm.getBei());
            vo.setBjlx(cm.getBjlx());
            vo.setCredit(cm.getCredit());
            vo.setWarnline(cm.getWarnline());
            vo.setCreatuser(cm.getCreatuser());
            vo.setCttime(cm.getCttime());
            vo.setFeetype(cm.getFeetype());
            vo.setProId(cm.getProId());
            vo.setProName(cm.getProName());
            vo.setPlusPrice(cm.getPlusPrice());
            vo.setPriceType(cm.getPriceType());
            vo.setPriceContent(cm.getPriceContent());
            vo.setJoinPhone(cm.getJoinPhone());
            vo.setCollectorDeviceNo(cm.getCollectorDeviceNo());
            vo.setPhase(cm.getPhase());
        }

        // 捷先
        if (resp.getJiexian() != null) {
            MeterResponse.Jiexian jx = resp.getJiexian();
            vo.setMeteraddress(jx.getMeteraddress());
            vo.setCompanycode(jx.getCompanycode());
            vo.setCustomerNo(jx.getCustomerNo());
            vo.setMeterCaliber(jx.getMeterCaliber());
            vo.setControlling(jx.getControlling());
            vo.setStateValve(jx.getStateValve());
            vo.setSignalStrength(jx.getSignalStrength());
            vo.setBatteryVoltage(jx.getBatteryVoltage());
        }

        // 通用状态
        if (resp.getGeneral() != null) {
            MeterResponse.General g = resp.getGeneral();
            vo.setCtRatio(g.getCtRatio());
            vo.setPtRatio(g.getPtRatio());
            vo.setMarkStatus(g.getMarkStatus());
            vo.setEnabledTime(g.getEnabledTime());
            vo.setDisableWarningDesc(g.getDisableWarning() != null && g.getDisableWarning() == 1 ? "已停用" : "正常");
            vo.setCommStatusDesc(g.getCommStatusDesc());

            // 运行状态
            Integer status = g.getStatus();
            vo.setStatusDesc(status == null ? "" : (status == 0 ? "停用" : status == 1 ? "正常运行" : "欠费停机"));

            // 在线状态
            Integer online = g.getOnline();
            vo.setOnlineDesc(online == null ? "" : (online == 0 ? "离线" : "在线"));

            vo.setMeterSignal(g.getMeterSignal());
            vo.setGroupId(g.getGroupId());
        }

        // 通信参数
        if (resp.getCommParams() != null) {
            MeterResponse.CommParams cp = resp.getCommParams();
            vo.setCollectorCode(cp.getCollectorCode());
            vo.setBpsType(cp.getBpsType());
            vo.setBitType(cp.getBitType());
            vo.setProtoType(cp.getProtoType());
            vo.setSerial(cp.getSerial());
            vo.setDeviceType(cp.getDeviceType());
        }

        // 审计
        vo.setCreateTime(resp.getCreateTime());
        vo.setUpdateTime(resp.getUpdateTime());
        vo.setCreateBy(resp.getCreateBy());

        return vo;
    }

    // ──────────────── 批量/全局开关阀 ────────────────

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BatchOperationResponse batchValveControl(BatchValveControlRequest request) {
        if (!SecurityUserContext.isAdmin()) {
            throw BusinessException.of(403, "仅管理员可执行批量开关阀操作");
        }

        boolean isOpen = "OPEN".equalsIgnoreCase(request.getAction());
        int stateValve = isOpen ? 1 : 2;  // 1=开阀(合闸), 2=关阀(分闸)
        int status = isOpen ? 1 : 2;       // 1=正常, 2=欠费停机
        Integer meterType = request.getMeterType();

        int total;
        int success;
        String actionDesc = isOpen ? "开阀(合闸)" : "关阀(分闸)";

        if (Boolean.TRUE.equals(request.getIsGlobal()) && (request.getMeterIds() == null || request.getMeterIds().isEmpty())) {
            LambdaQueryWrapper<Meter> countWrapper = new LambdaQueryWrapper<Meter>()
                    .eq(Meter::getIsDeleted, 0);
            if (meterType != null) countWrapper.eq(Meter::getMeterType, meterType);
            total = Math.toIntExact(meterMapper.selectCount(countWrapper));
            success = meterMapper.batchUpdateValveStateGlobal(stateValve, status, meterType);
        } else if (request.getCustomerId() != null) {
            LambdaQueryWrapper<Meter> countWrapper = new LambdaQueryWrapper<Meter>()
                    .eq(Meter::getCustomerId, request.getCustomerId())
                    .eq(Meter::getIsDeleted, 0);
            if (meterType != null) countWrapper.eq(Meter::getMeterType, meterType);
            total = Math.toIntExact(meterMapper.selectCount(countWrapper));
            success = meterMapper.batchUpdateValveStateByCustomer(request.getCustomerId(), stateValve, status, meterType);
        } else if (request.getMeterIds() != null && !request.getMeterIds().isEmpty()) {
            total = request.getMeterIds().size();
            success = meterMapper.batchUpdateValveState(request.getMeterIds(), stateValve, status, meterType);
        } else {
            throw BusinessException.of(400, "请指定表计ID列表、商户ID，或开启全局操作");
        }

        log.warn("[批量{}] total={}, success={}, reason={}, operator={}",
                actionDesc, total, success, request.getReason(), SecurityUserContext.getCurrentUsername());

        // 逐台下发阀控硬件指令
        String operator = SecurityUserContext.getCurrentUsername();
        String reason = request.getReason();
        if (request.getMeterIds() != null && !request.getMeterIds().isEmpty()) {
            for (Long meterId : request.getMeterIds()) {
                try {
                    if (isOpen) {
                        valveControlHook.openValve(meterId, null, operator, reason);
                    } else {
                        valveControlHook.closeValve(meterId, null, operator, reason);
                    }
                } catch (Exception e) {
                    log.error("[批量{}] 阀控指令下发异常 meterId={}: {}", actionDesc, meterId, e.getMessage());
                }
            }
        }

        // 批量写入操作日志
        OperationLog logEntry = new OperationLog();
        logEntry.setOperatorName(SecurityUserContext.getCurrentUsername());
        logEntry.setModuleName("表计管理");
        logEntry.setOperationType(isOpen ? "BATCH_VALVE_OPEN" : "BATCH_VALVE_CLOSE");
        logEntry.setOperationDesc(String.format("批量%s：共%d台，成功%d台，原因：%s",
                actionDesc, total, success, request.getReason()));
        logEntry.setRequestParams("{\"action\":\"" + request.getAction() + "\",\"total\":" + total + ",\"reason\":\"" + request.getReason() + "\"}");
        logEntry.setResponseResult("{\"success\":" + success + ",\"failed\":" + (total - success) + "}");
        logEntry.setOperationTime(LocalDateTime.now());
        logEntry.setCreateTime(LocalDateTime.now());
        operationLogMapper.insert(logEntry);

        return new BatchOperationResponse(total, success, total - success,
                String.format("批量%s完成：共 %d 台，成功 %d 台，失败 %d 台", actionDesc, total, success, total - success));
    }

    // ──────────────── 批量/全局设置时段策略 ────────────────

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BatchOperationResponse batchSetTimeSlot(BatchTimeSlotRequest request) {
        if (!SecurityUserContext.isAdmin()) {
            throw BusinessException.of(403, "仅管理员可执行批量设置时段策略");
        }

        Integer meterType = request.getMeterType();
        validatePriceStrategyMeterType(request.getRateConfigId(), meterType);
        int total;
        int success;

        if (Boolean.TRUE.equals(request.getIsGlobal()) && (request.getMeterIds() == null || request.getMeterIds().isEmpty())) {
            LambdaQueryWrapper<Meter> countWrapper = new LambdaQueryWrapper<Meter>()
                    .eq(Meter::getIsDeleted, 0);
            if (meterType != null) countWrapper.eq(Meter::getMeterType, meterType);
            total = Math.toIntExact(meterMapper.selectCount(countWrapper));
            success = meterMapper.batchSetRateConfigGlobal(request.getRateConfigId(), meterType);
        } else if (request.getCustomerId() != null) {
            LambdaQueryWrapper<Meter> countWrapper = new LambdaQueryWrapper<Meter>()
                    .eq(Meter::getCustomerId, request.getCustomerId())
                    .eq(Meter::getIsDeleted, 0);
            if (meterType != null) countWrapper.eq(Meter::getMeterType, meterType);
            total = Math.toIntExact(meterMapper.selectCount(countWrapper));
            success = meterMapper.batchSetRateConfigByCustomer(request.getCustomerId(), request.getRateConfigId(), meterType);
        } else if (request.getMeterIds() != null && !request.getMeterIds().isEmpty()) {
            total = request.getMeterIds().size();
            success = meterMapper.batchSetRateConfig(request.getMeterIds(), request.getRateConfigId(), meterType);
        } else {
            throw BusinessException.of(400, "请指定表计ID列表、商户ID，或开启全局设置");
        }

        log.info("[批量设置时段策略] total={}, success={}, rateConfigId={}, operator={}",
                total, success, request.getRateConfigId(), SecurityUserContext.getCurrentUsername());

        return new BatchOperationResponse(total, success, total - success,
                String.format("批量设置时段策略完成：共 %d 台，成功 %d 台，失败 %d 台", total, success, total - success));
    }

    private void validatePriceStrategyMeterType(Long strategyId, Integer meterType) {
        if (strategyId == null || meterType == null) {
            throw BusinessException.of(400, "费率策略ID和表计类型不能为空");
        }
        PriceStrategy strategy = priceStrategyMapper.selectById(strategyId);
        if (strategy == null || !meterType.equals(strategy.getMeterType())) {
            throw BusinessException.of(400, meterType == 0
                    ? "水表只能绑定水表费率策略"
                    : "电表只能绑定电表费率策略");
        }
    }

    // ──────────────── 表计统计 ────────────────

    @Override
    @Cacheable(value = "meter_statistics",
               key = "T(com.competition.energycontrollerplatform.security.SecurityUserContext).isAdmin() " +
                     "? 'admin' " +
                     ": 'cust_' + T(com.competition.energycontrollerplatform.security.SecurityUserContext).getCurrentCustomerId()")
    public MeterStatisticsVO getStatistics() {
        List<Meter> allMeters = meterMapper.selectList(
                new LambdaQueryWrapper<Meter>().eq(Meter::getIsDeleted, 0));

        MeterStatisticsVO stats = new MeterStatisticsVO();
        stats.setTotalCount(allMeters.size());

        // 按 meterType（0=水表, 1=电表）分组
        long electricCount = allMeters.stream().filter(m -> Integer.valueOf(1).equals(m.getMeterType())).count();
        long waterCount = allMeters.stream().filter(m -> Integer.valueOf(0).equals(m.getMeterType())).count();
        stats.setElectricCount(electricCount);
        stats.setWaterCount(waterCount);

        // 在线/离线
        long onlineCount = allMeters.stream().filter(m -> Integer.valueOf(1).equals(m.getOnline())).count();
        long offlineCount = allMeters.stream().filter(m -> Integer.valueOf(0).equals(m.getOnline())).count();
        stats.setOnlineCount(onlineCount);
        stats.setOfflineCount(offlineCount);

        // 按运行状态分组
        Map<Integer, Long> byStatus = allMeters.stream()
                .collect(Collectors.groupingBy(
                        m -> m.getStatus() != null ? m.getStatus() : -1,
                        LinkedHashMap::new,
                        Collectors.counting()));
        stats.setByStatus(byStatus);

        // 按业务类型 bjlx 分组
        Map<Integer, Long> byBjlx = allMeters.stream()
                .collect(Collectors.groupingBy(
                        m -> m.getBjlx() != null ? m.getBjlx() : -1,
                        LinkedHashMap::new,
                        Collectors.counting()));
        stats.setByBjlx(byBjlx);

        // 按标记状态分组
        Map<String, Long> byMarkStatus = allMeters.stream()
                .collect(Collectors.groupingBy(
                        m -> m.getMarkStatus() != null ? m.getMarkStatus() : "UNKNOWN",
                        LinkedHashMap::new,
                        Collectors.counting()));
        stats.setByMarkStatus(byMarkStatus);

        // 按通讯状态分组
        Map<String, Long> byCommStatus = allMeters.stream()
                .collect(Collectors.groupingBy(
                        m -> m.getCommStatusDesc() != null ? m.getCommStatusDesc() : "UNKNOWN",
                        LinkedHashMap::new,
                        Collectors.counting()));
        stats.setByCommStatus(byCommStatus);

        return stats;
    }

    // ──────────────── 二维码 ────────────────

    @Override
    public byte[] generateQrCodeImage(Long meterId) {
        Meter meter = meterMapper.selectById(meterId);
        if (meter == null) {
            throw BusinessException.of(400, "表计不存在");
        }
        String dbNo = meter.getDbNo();
        if (dbNo == null || dbNo.isBlank()) {
            throw BusinessException.of(400, "该表计缺少编号（dbNo），无法生成二维码");
        }
        Integer meterType = meter.getMeterType();
        String content = QrCodeGenerator.buildContent(
                meterType != null ? meterType : -1, dbNo);
        return QrCodeGenerator.generate(content);
    }

    @Override
    public MeterQrCodeVO getQrCodeInfo(Long meterId) {
        Meter meter = meterMapper.selectById(meterId);
        if (meter == null) {
            throw BusinessException.of(400, "表计不存在");
        }
        String typeName;
        if (meter.getMeterType() != null && meter.getMeterType() == 1) {
            typeName = "电表";
        } else if (meter.getMeterType() != null && meter.getMeterType() == 0) {
            typeName = "水表";
        } else {
            typeName = "未知";
        }
        String customerName = null;
        if (meter.getCustomerId() != null) {
            Customer customer = customerMapper.selectById(meter.getCustomerId());
            if (customer != null) {
                customerName = customer.getCustomerName();
            }
        }
        String qrContent = QrCodeGenerator.buildContent(
                meter.getMeterType() != null ? meter.getMeterType() : -1,
                meter.getDbNo());
        return MeterQrCodeVO.builder()
                .meterId(meter.getId())
                .dbNo(meter.getDbNo())
                .meterName(meter.getName())
                .meterType(meter.getMeterType())
                .meterTypeName(typeName)
                .customerName(customerName)
                .qrContent(qrContent)
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unbindCustomer(Long meterId) {
        if (!SecurityUserContext.isAdmin()) {
            throw BusinessException.of(403, "仅管理员可执行解绑操作");
        }
        Meter meter = meterMapper.selectById(meterId);
        if (meter == null) {
            throw BusinessException.of(400, "表计不存在");
        }
        if (meter.getCustomerId() == null) {
            // 已解绑，幂等返回
            return;
        }
        log.info("[表计解绑] meterId={}, 解绑商户 customerId={}", meterId, meter.getCustomerId());
        meter.setCustomerId(null);
        meter.setUpdateTime(LocalDateTime.now());
        meterMapper.updateById(meter);
    }

    // ──────────────── 布隆过滤器生命周期 ────────────────

    /** 启动时加载所有未删除表计 ID 到布隆过滤器 */
    @PostConstruct
    public void initBloomFilter() {
        if (!redisEnabled) {
            return;
        }
        try {
            List<Meter> allMeters = meterMapper.selectList(
                    new LambdaQueryWrapper<Meter>().eq(Meter::getIsDeleted, 0));
            List<String> ids = allMeters.stream()
                    .map(m -> String.valueOf(m.getId()))
                    .collect(Collectors.toList());
            bloomFilterHelper.rebuild(ids);
        } catch (Exception e) {
            log.error("Bloom Filter 初始化失败，降级为跳过布隆检查", e);
        }
    }

    /** 每小时重建布隆过滤器（清理已删除表计的误判残留） */
    @Scheduled(fixedRate = 3600000)
    public void refreshBloomFilter() {
        initBloomFilter();
    }
}
