package com.competition.energycontrollerplatform.integration.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.competition.energycontrollerplatform.entity.Meter;
import com.competition.energycontrollerplatform.entity.MeterReading;
import com.competition.energycontrollerplatform.entity.MeterState;
import com.competition.energycontrollerplatform.entity.Customer;
import com.competition.energycontrollerplatform.integration.dto.HaopuDTO;
import com.competition.energycontrollerplatform.integration.dto.HaopuArchiveSyncResult;
import com.competition.energycontrollerplatform.integration.dto.HaopuReadingSyncResult;
import com.competition.energycontrollerplatform.integration.dto.JichengDTO;
import com.competition.energycontrollerplatform.integration.dto.JiexianDTO;
import com.competition.energycontrollerplatform.mapper.MeterMapper;
import com.competition.energycontrollerplatform.mapper.MeterReadingMapper;
import com.competition.energycontrollerplatform.mapper.MeterStateMapper;
import com.competition.energycontrollerplatform.mapper.CustomerMapper;
import com.competition.energycontrollerplatform.mapper.HaopuAuditMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.ZoneId;
import java.util.List;

/**
 * 三方报文解析 → 持久化服务。
 * <p>所有数值计算统一使用 BigDecimal。</p>
 * <p>
 * <b>合理性阈值校验（防毒数据）：</b>在原始数据写入 t_meter_reading 之前，
 * 对超出物理极限的畸形读数做最后一层拦截。只有核心计费字段 kwh 畸形
 * （负值或超物理上限）才整条丢弃；电压/电流/频率/功率因数等辅助量异常或未采集
 * 时只把对应字段置空，保留有效电量，不让毒数据污染后续日报聚合/月度聚合/计费扣费流水。
 * </p>
 */
@Service
@RequiredArgsConstructor
public class MeterDataSyncService {

    private static final Logger log = LoggerFactory.getLogger(MeterDataSyncService.class);

    // ──────── 物理合理性阈值 ────────

    /** 单次读数最大总电量（kWh）：超过此值视为传感器故障/数据错乱 */
    private static final BigDecimal MAX_KWH_PER_READING = new BigDecimal("999999.9999");
    /** 单次读数最小总电量（kWh）：负值视为回滚异常 */
    private static final BigDecimal MIN_KWH_PER_READING = BigDecimal.ZERO;
    /** 电压合理范围（V） */
    private static final double MIN_VOLTAGE = 50.0;
    private static final double MAX_VOLTAGE = 500.0;
    /** 电流合理范围（A）：住宅/商业表计 */
    private static final double MIN_CURRENT = 0.0;
    private static final double MAX_CURRENT = 1000.0;
    /** 频率合理范围（Hz） */
    private static final double MIN_FREQ = 45.0;
    private static final double MAX_FREQ = 65.0;
    /** 功率因数合理范围 */
    private static final double MIN_POWER_FACTOR = -1.0;
    private static final double MAX_POWER_FACTOR = 1.0;

    private final MeterMapper meterMapper;
    private final MeterReadingMapper meterReadingMapper;
    private final MeterStateMapper meterStateMapper;
    private final CustomerMapper customerMapper;
    private final HaopuAuditMapper haopuAuditMapper;

    private static final DateTimeFormatter JICHENG_DATE_TIME =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // ──────── 积成 GetMeterReading ────────

    @Transactional(rollbackFor = Exception.class)
    public void syncJichengReadings(List<JichengDTO.ReadingItem> items) {
        if (items == null || items.isEmpty()) return;
        for (JichengDTO.ReadingItem item : items) {
            try {
                if (item.getStealNo() == null || item.getStealNo().isBlank()) {
                    log.warn("积成抄表数据: stealNo 为空，跳过");
                    continue;
                }
                Meter meter = findByStealNo(item.getStealNo());
                if (meter == null) meter = createJichengArchive(item);

                // 合理性阈值校验
                String label = "stealNo=" + item.getStealNo();
                if (!isTotalReadingReasonable(item.getReadVal(), label)) continue;

                LocalDateTime readDate = parseJichengDate(item.getReadDate());
                if (readDate != null) {
                    Long existing = meterReadingMapper.selectCount(
                            new LambdaQueryWrapper<MeterReading>()
                                    .eq(MeterReading::getMeterId, meter.getId())
                                    .eq(MeterReading::getReadDate, readDate));
                    if (existing != null && existing > 0) continue;
                }

                MeterReading r = new MeterReading();
                r.setMeterId(meter.getId());
                r.setStealNo(item.getStealNo());
                r.setCustomerCode(item.getCustomerCode());
                r.setVender(item.getVender());
                r.setReadVal(item.getReadVal());
                r.setReadJval(item.getReadJval());
                r.setKwh(item.getReadVal());
                r.setCreateBy("JICHENG_SYNC");

                r.setReadDate(readDate);
                r.setTimestamp(LocalDateTime.now());
                r.setCreateTime(LocalDateTime.now());
                r.setUpdateTime(LocalDateTime.now());
                meterReadingMapper.insert(r);
                log.debug("积成抄表入库 meterId={} stealNo={}", meter.getId(), item.getStealNo());
            } catch (Exception e) {
                log.error("积成抄表持久化失败 stealNo={}: {}", item.getStealNo(), e.getMessage());
            }
        }
    }

    private Meter createJichengArchive(JichengDTO.ReadingItem item) {
        Customer customer = null;
        if (item.getCustomerCode() != null && !item.getCustomerCode().isBlank()) {
            customer = customerMapper.selectOne(new LambdaQueryWrapper<Customer>()
                    .eq(Customer::getCustomerCode, item.getCustomerCode()));
            if (customer == null) {
                customer = new Customer();
                customer.setCustomerCode(item.getCustomerCode());
                customer.setCustomerName(item.getCustomerCode());
                customer.setBalance(BigDecimal.ZERO);
                customer.setStatus(1);
                customer.setSign(item.getSign());
                customer.setCreateBy("JICHENG_SYNC");
                customer.setCreateTime(LocalDateTime.now());
                customer.setUpdateTime(LocalDateTime.now());
                customerMapper.insert(customer);
            }
        }

        Meter meter = new Meter();
        meter.setCustomerId(customer == null ? null : customer.getId());
        meter.setStealNo(item.getStealNo());
        meter.setDbNo(item.getStealNo().length() <= 12 ? item.getStealNo() : null);
        meter.setMeteraddress(item.getStealNo());
        meter.setCustomerNo(item.getCustomerCode());
        meter.setName(item.getCustomerCode());
        meter.setVender(item.getVender());
        meter.setSign(item.getSign());
        meter.setMeterType(0);
        meter.setStatus(1);
        meter.setOnline(1);
        meter.setCreateBy("JICHENG_SYNC");
        meter.setCreateTime(LocalDateTime.now());
        meter.setUpdateTime(LocalDateTime.now());
        meterMapper.insert(meter);
        log.info("积成读数驱动建档完成 meterId={} stealNo={} customerCode={}",
                meter.getId(), item.getStealNo(), item.getCustomerCode());
        return meter;
    }

    private LocalDateTime parseJichengDate(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return LocalDateTime.parse(value, JICHENG_DATE_TIME);
        } catch (Exception e) {
            log.warn("积成 read_date 解析失败: {}", value);
            return null;
        }
    }

    // ──────── 积成 GetMeterState ────────

    @Transactional(rollbackFor = Exception.class)
    public void syncJichengStates(List<JichengDTO.StateItem> items) {
        if (items == null || items.isEmpty()) return;
        for (JichengDTO.StateItem item : items) {
            try {
                Meter meter = findByStealNo(item.getStealNo());
                if (meter == null) {
                    log.warn("积成状态数据: 未匹配到表计 stealNo={}", item.getStealNo());
                    continue;
                }
                MeterState state = new MeterState();
                state.setMeterId(meter.getId());
                state.setCustomerCode(item.getCustomerCode());
                state.setStealNo(item.getStealNo());
                state.setVender(item.getVender());
                state.setStatus(item.getStatus());
                state.setMessage(item.getMessage());
                state.setSign(item.getSign());
                if (item.getRecordDate() != null) {
                    try {
                        state.setRecordDate(LocalDateTime.parse(item.getRecordDate()));
                    } catch (Exception ignored) {}
                }
                state.setCreateTime(LocalDateTime.now());
                state.setUpdateTime(LocalDateTime.now());
                meterStateMapper.insert(state);
                log.debug("积成状态入库 meterId={} stealNo={}", meter.getId(), item.getStealNo());
            } catch (Exception e) {
                log.error("积成状态持久化失败 stealNo={}: {}", item.getStealNo(), e.getMessage());
            }
        }
    }

    // ──────── 捷先 getDataValue ────────

    @Transactional(rollbackFor = Exception.class)
    public void syncJiexianReadings(List<JiexianDTO.DataValueItem> items) {
        if (items == null || items.isEmpty()) return;
        for (JiexianDTO.DataValueItem item : items) {
            try {
                String meteraddress = item.effectiveMeteraddress();
                String dataDt = item.effectiveDataDt();
                BigDecimal dataValue0 = item.effectiveDataValue0();
                Meter meter = findByMeteraddress(meteraddress);
                if (meter == null) {
                    meter = createJiexianArchive(item);
                }

                // 合理性阈值校验（水表累计读数，不能为负）
                String label = "meteraddress=" + meteraddress;
                if (dataValue0 != null && dataValue0.signum() < 0) {
                    log.warn("[毒数据拦截] {} 水表累计读数为负值 dataValue0={}，已丢弃",
                            label, dataValue0);
                    continue;
                }

                Long existing = meterReadingMapper.selectCount(
                        new LambdaQueryWrapper<MeterReading>()
                                .eq(MeterReading::getMeterId, meter.getId())
                                .eq(MeterReading::getDataDt, dataDt));
                if (existing != null && existing > 0) continue;

                MeterReading r = new MeterReading();
                r.setMeterId(meter.getId());
                r.setMeteraddress(meteraddress);
                r.setDataDt(dataDt);
                r.setDataValue0(dataValue0 == null ? null : dataValue0.intValue());
                r.setReadVal(dataValue0);
                r.setReadDate(parseJiexianDate(dataDt));
                r.setCreateBy("JIEXIAN_SYNC");
                r.setTimestamp(LocalDateTime.now());
                r.setCreateTime(LocalDateTime.now());
                r.setUpdateTime(LocalDateTime.now());
                meterReadingMapper.insert(r);
                log.debug("捷先读数入库 meterId={} meteraddress={}", meter.getId(), meteraddress);
            } catch (Exception e) {
                log.error("捷先读数持久化失败 meteraddress={}: {}", item.getMeteraddress(), e.getMessage());
            }
        }
    }

    private Meter createJiexianArchive(JiexianDTO.DataValueItem item) {
        String customerCode = item.getChargeCustomerNo();
        if (customerCode == null || customerCode.isBlank()) customerCode = item.getMeteraddress();
        Customer customer = customerMapper.selectOne(new LambdaQueryWrapper<Customer>()
                .eq(Customer::getCustomerCode, customerCode));
        if (customer == null) {
            customer = new Customer();
            customer.setCustomerCode(customerCode);
            customer.setCustomerName(item.getCustomerName() == null ? customerCode : item.getCustomerName());
            customer.setAddress(item.getAddress());
            customer.setBalance(BigDecimal.ZERO);
            customer.setStatus(1);
            customer.setCreateBy("JIEXIAN_V5_SYNC");
            customer.setCreateTime(LocalDateTime.now());
            customer.setUpdateTime(LocalDateTime.now());
            customerMapper.insert(customer);
        }
        Meter meter = new Meter();
        meter.setCustomerId(customer.getId());
        meter.setMeteraddress(item.getMeteraddress());
        meter.setStealNo(item.getMeteraddress());
        meter.setCustomerNo(customerCode);
        meter.setName(item.getCustomerName() == null ? item.getMeteraddress() : item.getCustomerName());
        meter.setAddress(item.getAddress());
        meter.setMeterCaliber(item.getMeterCaliber());
        meter.setCompanycode(item.getFacCode());
        meter.setVender("jiexian-v5");
        meter.setMeterType(0);
        meter.setStatus(1);
        meter.setOnline(1);
        meter.setCreateBy("JIEXIAN_V5_SYNC");
        meter.setCreateTime(LocalDateTime.now());
        meter.setUpdateTime(LocalDateTime.now());
        meterMapper.insert(meter);
        log.info("Jiexian V5 archive created meterId={} meteraddress={}", meter.getId(), item.getMeteraddress());
        return meter;
    }

    @Transactional(rollbackFor = Exception.class)
    public int syncJiexianMeterArchives(List<JiexianDTO.MeterDateItem> items, Long vendorId) {
        if (items == null || items.isEmpty()) return 0;
        int synced = 0;
        for (JiexianDTO.MeterDateItem item : items) {
            String address = item.effectiveMeteraddress();
            if (address == null || address.isBlank()) continue;
            Meter meter = findByMeteraddress(address);
            if (meter == null) {
                meter = new Meter();
                meter.setMeteraddress(address);
                meter.setStealNo(address);
                meter.setMeterType(0);
                meter.setStatus(1);
                meter.setOnline(1);
                meter.setCreateBy("JIEXIAN_SYNC");
                meter.setCreateTime(LocalDateTime.now());
                meter.setIsDeleted(0);
            }
            meter.setWaterVendorId(vendorId);
            String customerCode = item.effectiveCustomerNo();
            meter.setCustomerNo(customerCode);
            Customer customer = resolveJiexianCustomer(customerCode, item);
            if (customer != null) meter.setCustomerId(customer.getId());
            if (meter.getName() == null || meter.getName().isBlank()) {
                meter.setName("捷先水表-" + address);
            }
            meter.setMeterCaliber(item.effectiveMeterCaliber());
            meter.setControlling(item.effectiveControlling());
            meter.setStateValve(item.effectiveStateValve());
            if (item.effectiveSignalStrength() != null) meter.setSignalStrength(item.effectiveSignalStrength());
            if (item.effectiveBatteryVoltage() != null) meter.setBatteryVoltage(BigDecimal.valueOf(item.effectiveBatteryVoltage()));
            meter.setUpdateTime(LocalDateTime.now());
            if (meter.getId() == null) meterMapper.insert(meter); else meterMapper.updateById(meter);
            synced++;
        }
        log.info("捷先水表档案同步入库 {} 条", synced);
        return synced;
    }

    private Customer resolveJiexianCustomer(String customerCode, JiexianDTO.MeterDateItem item) {
        if (customerCode == null || customerCode.isBlank()) return null;
        Customer customer = customerMapper.selectOne(new LambdaQueryWrapper<Customer>()
                .eq(Customer::getCustomerCode, customerCode));
        if (customer != null) return customer;

        customer = new Customer();
        customer.setCustomerCode(customerCode);
        customer.setCustomerName(customerCode);
        customer.setBalance(BigDecimal.ZERO);
        customer.setStatus(1);
        customer.setCreateBy("JIEXIAN_SYNC");
        customer.setCreateTime(LocalDateTime.now());
        customer.setUpdateTime(LocalDateTime.now());
        customerMapper.insert(customer);
        log.info("捷先客户档案自动创建 customerCode={}，待补充商户名称", customerCode);
        return customer;
    }

    private LocalDateTime parseJiexianDate(String value) {
        if (value == null || value.isBlank()) return null;
        for (DateTimeFormatter formatter : List.of(
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
                DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"),
                DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))) {
            try { return LocalDateTime.parse(value, formatter); } catch (Exception ignored) {}
        }
        return null;
    }

    // ──────── 昊普 电表数据采集（主动推送） ────────

    /**
     * 昊普接口一：电表数据采集（主动推送）→ 批量持久化。
     * <p>
     * 昊普通过 webhook POST 推送 JSON 数组，每条数据包含 25 个采集字段。
     * 通过 (collectorCode, meterCode) 匹配 t_meter 中的电表记录，
     * 未匹配到的数据跳过并记录 warn 日志，不阻断其他数据的入库。
     *
     * @param items 昊普推送的电表读数列表
     */
    @Transactional(rollbackFor = Exception.class)
    public HaopuReadingSyncResult syncHaopuReadings(List<HaopuDTO.ReadingItem> items) {
        if (items == null || items.isEmpty()) return new HaopuReadingSyncResult(0, 0, 0);
        int inserted = 0, rejected = 0, failed = 0;
        for (HaopuDTO.ReadingItem item : items) {
            if (item == null || item.getCollectorCode() == null || item.getMeterCode() == null) {
                rejected++;
                log.warn("Haopu reading rejected: missing device identity");
                continue;
            }
            try {
                // 通过采集器编码 + 电表编码匹配平台内的电表
                Meter meter = findByHaopuMeterCode(item.getCollectorCode(), item.getMeterCode());
                if (meter == null) {
                    meter = createHaopuPendingArchive(item);
                    log.info("昊普电表档案自动创建，待分配商户 collectorCode={} meterCode={}",
                            item.getCollectorCode(), item.getMeterCode());
                }

                // 合理性阈值校验：仅核心计费字段 kwh 畸形时才丢弃整条读数；
                // 电压/电流/频率/功率因数等辅助量异常或未采集（0 值）时只把对应字段置空，保留有效电量。
                String label = "collectorCode=" + item.getCollectorCode()
                        + " meterCode=" + item.getMeterCode();
                if (!isKwhReasonable(item.getKwh(), label)) {
                    rejected++;
                    continue;
                }
                BigDecimal voltA = sanitizeVoltage(item.getVoltA(), "A", label);
                BigDecimal voltB = sanitizeVoltage(item.getVoltB(), "B", label);
                BigDecimal voltC = sanitizeVoltage(item.getVoltC(), "C", label);
                BigDecimal ampA = sanitizeCurrent(item.getAmpA(), "A", label);
                BigDecimal ampB = sanitizeCurrent(item.getAmpB(), "B", label);
                BigDecimal ampC = sanitizeCurrent(item.getAmpC(), "C", label);
                BigDecimal freq = sanitizeFrequency(item.getFreq(), label);
                BigDecimal fact = sanitizePowerFactor(item.getFact(), label);

                MeterReading r = new MeterReading();
                r.setMeterId(meter.getId());

                // 标识字段。现场旧库补列后，这三项也是昊普读数可追溯和正常落库的必要字段。
                r.setReport(item.getReport());
                r.setCollectorCode(item.getCollectorCode());
                r.setMeterCode(item.getMeterCode());
                r.setTimestamp(item.getTimestamp() == null ? LocalDateTime.now()
                        : java.time.Instant.ofEpochMilli(item.getTimestamp())
                        .atZone(java.time.ZoneId.systemDefault()).toLocalDateTime());

                // 电压 + 频率（未采集/越界字段已置空）
                r.setVoltA(voltA);
                r.setVoltB(voltB);
                r.setVoltC(voltC);
                r.setFreq(freq);

                // 电流
                r.setAmpA(ampA);
                r.setAmpB(ampB);
                r.setAmpC(ampC);

                // 功率因数
                r.setFact(fact);
                r.setFactA(item.getFactA());
                r.setFactB(item.getFactB());
                r.setFactC(item.getFactC());

                // 有功功率
                r.setAct(item.getAct());
                r.setActA(item.getActA());
                r.setActB(item.getActB());
                r.setActC(item.getActC());

                // 无功功率
                r.setReact(item.getReact());
                r.setReactA(item.getReactA());
                r.setReactB(item.getReactB());
                r.setReactC(item.getReactC());

                // 电量（总 + 分时）
                r.setKwh(item.getKwh());
                // 统一结算读数口径：自动扣费读取 read_val，昊普累计有功电量即为电表当前表底。
                r.setReadVal(item.getKwh());
                r.setKwhPeak(item.getKwhPeak());
                r.setKwhBusy(item.getKwhBusy());
                r.setKwhNormal(item.getKwhNormal());
                r.setKwhIdle(item.getKwhIdle());

                // 审计字段
                r.setCreateBy("HAOPU_WEBHOOK");
                r.setCreateTime(LocalDateTime.now());
                r.setUpdateTime(LocalDateTime.now());

                meterReadingMapper.insert(r);
                inserted++;
                log.debug("昊普读数入库 meterId={} collectorCode={} meterCode={}",
                        meter.getId(), item.getCollectorCode(), item.getMeterCode());
            } catch (Exception e) {
                failed++;
                log.error("昊普读数持久化失败 collectorCode={} meterCode={}: {}",
                        item.getCollectorCode(), item.getMeterCode(), e.getMessage(), e);
            }
        }
        return new HaopuReadingSyncResult(inserted, rejected, failed);
    }

    /** 根据昊普接口二同步采集器下的电表通信档案。 */
    public void recordHaopuReceive(HaopuDTO.ReadingItem item) {
        try {
            haopuAuditMapper.upsertReceiveStat(item.getCollectorCode(), item.getMeterCode(), item.getTimestamp(), item.getReport());
        } catch (Exception e) {
            log.error("Haopu receive audit failed collectorCode={} meterCode={}: {}",
                    item.getCollectorCode(), item.getMeterCode(), e.getMessage());
        }
    }

    public void recordHaopuArchiveSync(Long collectorCode, HaopuArchiveSyncResult result, String errorMessage) {
        try {
            haopuAuditMapper.insertArchiveSyncLog(collectorCode, result.responseCount(), result.validCount(),
                    result.createdCount(), result.updatedCount(), result.rejectedCount(), errorMessage);
        } catch (Exception e) {
            log.error("Haopu archive audit failed collectorCode={}: {}", collectorCode, e.getMessage());
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public int syncHaopuArchives(Long collectorCode, List<HaopuDTO.MeterArchiveItem> items) {
        if (collectorCode == null || items == null || items.isEmpty()) return 0;
        int synced = 0;
        for (HaopuDTO.MeterArchiveItem item : items) {
            if (item == null || item.getMeterCode() == null) continue;
            Meter meter = findByHaopuMeterCode(collectorCode, item.getMeterCode());
            if (meter == null) {
                meter = new Meter();
                meter.setCustomerId(getHaopuPendingCustomer().getId());
                meter.setMeterType(1);
                meter.setStatus(1);
                meter.setOnline(1);
                meter.setCreateBy("HAOPU_ARCHIVE_SYNC");
                meter.setCreateTime(LocalDateTime.now());
                meter.setIsDeleted(0);
            }
            meter.setMeterCode(item.getMeterCode());
            meter.setCollectorCode(String.valueOf(collectorCode));
            // db_no is globally unique; meterCode is scoped to a collector.
            // Preserve any manually assigned business number on existing archives.
            meter.setName("昊普电表-" + item.getMeterCode());
            meter.setVender("haopu");
            meter.setBpsType(item.getBpsType());
            meter.setBitType(item.getBitType());
            meter.setProtoType(item.getProtoType());
            meter.setDeviceType(item.getDeviceType());
            meter.setSerial(item.getSerial());
            meter.setUpdateTime(LocalDateTime.now());
            if (meter.getId() == null) meterMapper.insert(meter); else meterMapper.updateById(meter);
            synced++;
        }
        log.info("昊普电表档案同步完成 collectorCode={} count={}", collectorCode, synced);
        return synced;
    }

    /**
     * 昊普只推送设备和读数，不提供本平台客户档案，因此首次收到陌生设备时先自动建档。
     * 使用固定的待分配客户承接数据，后续可在平台中把电表转移到真实客户。
     */
    @Transactional(rollbackFor = Exception.class)
    public HaopuArchiveSyncResult syncHaopuArchivesWithResult(Long collectorCode, List<HaopuDTO.MeterArchiveItem> items) {
        int response = items == null ? 0 : items.size();
        if (collectorCode == null || items == null || items.isEmpty()) {
            return new HaopuArchiveSyncResult(response, 0, 0, 0, response);
        }
        int valid = 0;
        int existing = 0;
        for (HaopuDTO.MeterArchiveItem item : items) {
            if (item == null || item.getMeterCode() == null) continue;
            valid++;
            if (findByHaopuMeterCode(collectorCode, item.getMeterCode()) != null) existing++;
        }
        int synced = syncHaopuArchives(collectorCode, items);
        int updated = Math.min(existing, synced);
        int created = Math.max(0, synced - updated);
        HaopuArchiveSyncResult result = new HaopuArchiveSyncResult(response, valid, created, updated, response - valid);
        log.info("Haopu archive sync collectorCode={} response={} valid={} created={} updated={} rejected={}",
                collectorCode, response, valid, created, updated, response - valid);
        return result;
    }

    private Meter createHaopuPendingArchive(HaopuDTO.ReadingItem item) {
        if (item.getCollectorCode() == null || item.getMeterCode() == null) {
            throw new IllegalArgumentException("昊普读数缺少 collectorCode 或 meterCode");
        }

        Customer customer = getHaopuPendingCustomer();

        Meter meter = new Meter();
        meter.setCustomerId(customer.getId());
        meter.setMeterCode(item.getMeterCode());
        meter.setCollectorCode(String.valueOf(item.getCollectorCode()));
        // Leave the global business number unset until an authoritative one is assigned.
        // The device identity remains (collectorCode, meterCode), including short codes.
        meter.setName("昊普电表-" + item.getMeterCode());
        meter.setVender("haopu");
        meter.setMeterType(1);
        meter.setDeviceType("ELEC");
        meter.setStatus(1);
        meter.setOnline(1);
        meter.setCreateBy("HAOPU_SYNC");
        meter.setCreateTime(LocalDateTime.now());
        meter.setUpdateTime(LocalDateTime.now());
        meterMapper.insert(meter);
        return meter;
    }

    private Customer getHaopuPendingCustomer() {
        final String pendingCustomerCode = "HAOPU_PENDING";
        Customer customer = customerMapper.selectOne(new LambdaQueryWrapper<Customer>()
                .eq(Customer::getCustomerCode, pendingCustomerCode));
        if (customer != null) return customer;
        customer = new Customer();
        customer.setCustomerCode(pendingCustomerCode);
        customer.setCustomerName("昊普电表待分配");
        customer.setCustomerType("SYSTEM_PENDING");
        customer.setUseNature("待分配");
        customer.setBalance(BigDecimal.ZERO);
        customer.setStatus(1);
        customer.setCreateBy("HAOPU_SYNC");
        customer.setCreateTime(LocalDateTime.now());
        customer.setUpdateTime(LocalDateTime.now());
        customerMapper.insert(customer);
        return customer;
    }

    // ──────── 辅助：表计匹配 ────────

    private Meter findByStealNo(String stealNo) {
        if (stealNo == null || stealNo.isBlank()) return null;
        return meterMapper.selectOne(
                new LambdaQueryWrapper<Meter>().eq(Meter::getStealNo, stealNo));
    }

    private Meter findByMeteraddress(String meteraddress) {
        if (meteraddress == null || meteraddress.isBlank()) return null;
        return meterMapper.selectOne(
                new LambdaQueryWrapper<Meter>().eq(Meter::getMeteraddress, meteraddress));
    }

    /**
     * 昊普表计匹配：通过采集器编码 + 电表编码唯一匹配。
     * <p>
     * Meter 表中 collectorCode 存储为 String（兼容不同厂商），
     * 昊普推送数据中为 Long，匹配时需做类型转换。
     */
    private Meter findByHaopuMeterCode(Long collectorCode, Long meterCode) {
        if (collectorCode == null || meterCode == null) return null;
        return meterMapper.selectOne(
                new LambdaQueryWrapper<Meter>()
                        .eq(Meter::getCollectorCode, String.valueOf(collectorCode))
                        .eq(Meter::getMeterCode, meterCode));
    }

    // ──────── 物理合理性校验（防毒数据） ────────

    /**
     * 校验电表读数是否在物理合理范围内（BigDecimal 版本，用于昊普）。
     */
    private boolean isKwhReasonable(BigDecimal kwh, String label) {
        if (kwh == null) return true;
        return isTotalReadingReasonable(kwh, label);
    }

    /**
     * 电压清洗（昊普）：昊普以 0.0000 表示“未采集/缺相/离线/直流表”，
     * 此类 0 值与越界值一并置为 null（缺数），不再整条丢弃，保留有效电量。
     */
    private BigDecimal sanitizeVoltage(BigDecimal volt, String phase, String label) {
        if (volt == null) return null;
        double v = volt.doubleValue();
        if (v == 0.0) {
            log.debug("{} 电压{}未采集（0V），按缺数置空", label, phase);
            return null;
        }
        if (v < MIN_VOLTAGE || v > MAX_VOLTAGE) {
            log.warn("[数据清洗] {} 电压{}越界 volt{}={}V（合理范围 {}-{}V），该字段置空，保留电量读数",
                    label, phase, phase, volt, MIN_VOLTAGE, MAX_VOLTAGE);
            return null;
        }
        return volt;
    }

    /**
     * 电流清洗（昊普）：0 电流为“空载”合法值，保留；仅负值/超上限等越界值置空。
     */
    private BigDecimal sanitizeCurrent(BigDecimal amp, String phase, String label) {
        if (amp == null) return null;
        double a = amp.doubleValue();
        if (a < MIN_CURRENT || a > MAX_CURRENT) {
            log.warn("[数据清洗] {} 电流{}越界 amp{}={}A（合理范围 {}-{}A），该字段置空，保留电量读数",
                    label, phase, phase, amp, MIN_CURRENT, MAX_CURRENT);
            return null;
        }
        return amp;
    }

    /**
     * 频率清洗（昊普）：0 表示未采集（直流表/离线），与越界值一并置空，保留电量。
     */
    private BigDecimal sanitizeFrequency(BigDecimal freq, String label) {
        if (freq == null) return null;
        double f = freq.doubleValue();
        if (f == 0.0) {
            log.debug("{} 频率未采集（0Hz），按缺数置空", label);
            return null;
        }
        if (f < MIN_FREQ || f > MAX_FREQ) {
            log.warn("[数据清洗] {} 频率越界 freq={}Hz（合理范围 {}-{}Hz），该字段置空，保留电量读数",
                    label, freq, MIN_FREQ, MAX_FREQ);
            return null;
        }
        return freq;
    }

    /**
     * 功率因数清洗（昊普）：0 为“空载/未采集”合法值，保留；仅越界值置空。
     */
    private BigDecimal sanitizePowerFactor(BigDecimal pf, String label) {
        if (pf == null) return null;
        double f = pf.doubleValue();
        if (f < MIN_POWER_FACTOR || f > MAX_POWER_FACTOR) {
            log.warn("[数据清洗] {} 功率因数越界 pf={}（合理范围 {}-{}），该字段置空，保留电量读数",
                    label, pf, MIN_POWER_FACTOR, MAX_POWER_FACTOR);
            return null;
        }
        return pf;
    }

    /**
     * 校验总读数是否在物理合理范围内（核心校验逻辑）。
     * <p>所有 isKwhReasonable / isTotalReadingReasonable 最终收敛于此方法。</p>
     */
    private boolean isTotalReadingReasonable(BigDecimal val, String label) {
        if (val == null) return true;
        if (val.compareTo(MIN_KWH_PER_READING) < 0) {
            log.warn("[毒数据拦截] {} 总读数为负值 val={}，疑似表计回滚或数据错乱，已丢弃", label, val);
            return false;
        }
        if (val.compareTo(MAX_KWH_PER_READING) > 0) {
            log.warn("[毒数据拦截] {} 总读数超出物理上限 val={}（上限 {}），疑似传感器故障，已丢弃",
                    label, val, MAX_KWH_PER_READING);
            return false;
        }
        return true;
    }
}
