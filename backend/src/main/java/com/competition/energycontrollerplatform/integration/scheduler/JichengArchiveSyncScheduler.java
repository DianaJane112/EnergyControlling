package com.competition.energycontrollerplatform.integration.scheduler;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.competition.energycontrollerplatform.entity.Customer;
import com.competition.energycontrollerplatform.entity.Meter;
import com.competition.energycontrollerplatform.integration.config.WaterMeterVendorConfigProvider;
import com.competition.energycontrollerplatform.integration.http.client.JichengWaterClient;
import com.competition.energycontrollerplatform.mapper.CustomerMapper;
import com.competition.energycontrollerplatform.mapper.MeterMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 积成 iES-CMR 档案同步调度器。
 */
@Component
@ConditionalOnProperty(prefix = "integration.jicheng", name = "legacy-archive-sync-enabled", havingValue = "true")
@RequiredArgsConstructor
public class JichengArchiveSyncScheduler {

    private static final Logger log = LoggerFactory.getLogger(JichengArchiveSyncScheduler.class);

    private final JichengWaterClient jichengClient;
    private final WaterMeterVendorConfigProvider waterMeterVendorConfigProvider;
    private final MeterMapper meterMapper;
    private final CustomerMapper customerMapper;

    @Scheduled(cron = "0 0 4 * * ?")
    public void syncArchives() {
        log.info("=== 积成档案同步 开始 ===");
        try {
            List<Long> vendorIds = waterMeterVendorConfigProvider.listEnabledVendorIdsByProtocol(
                    WaterMeterVendorConfigProvider.PROTOCOL_JICHENG_HTTP);
            if (vendorIds == null || vendorIds.isEmpty()) {
                vendorIds = Collections.singletonList(null);
            } else {
                vendorIds = new ArrayList<>(vendorIds);
            }

            int customerSynced = 0;
            int meterSynced = 0;
            for (Long vendorId : vendorIds) {
                customerSynced += syncCustomerArchives(vendorId);
                meterSynced += syncMeterArchives(vendorId);
            }
            log.info("=== 积成档案同步 完成: 客户 {} 条, 表计 {} 条 ===", customerSynced, meterSynced);
        } catch (Exception e) {
            log.error("积成档案同步整体失败", e);
        }
    }

    private int syncCustomerArchives(Long vendorId) {
        int synced = 0;
        int page = 1;
        while (true) {
            List<Map<String, Object>> items = jichengClient.fetchCustomerArchive(page, vendorId);
            if (items == null || items.isEmpty()) {
                break;
            }

            for (Map<String, Object> item : items) {
                try {
                    syncOneCustomer(item);
                    synced++;
                } catch (Exception e) {
                    log.error("积成客户档案同步异常 item={}, {}", item, e.getMessage());
                }
            }
            page++;
        }
        return synced;
    }

    @Transactional(rollbackFor = Exception.class)
    public void syncOneCustomer(Map<String, Object> item) {
        String customerCode = strVal(item, "customer_code");
        String customerName = strVal(item, "customer_name");
        String areaName = strVal(item, "area_name");
        String phone = strVal(item, "phone");
        String address = strVal(item, "address");

        if (customerCode == null || customerCode.isBlank()) {
            log.warn("积成客户档案: customerCode 为空，跳过 item={}", item);
            return;
        }

        Customer existing = customerMapper.selectOne(
                new LambdaQueryWrapper<Customer>()
                        .eq(Customer::getCustomerCode, customerCode));

        if (existing != null) {
            if (customerName != null) existing.setCustomerName(customerName);
            if (areaName != null) existing.setAreaName(areaName);
            if (phone != null) existing.setPhone(phone);
            if (address != null) existing.setAddress(address);
            existing.setUpdateTime(LocalDateTime.now());
            customerMapper.updateById(existing);
        } else {
            Customer customer = new Customer();
            customer.setCustomerCode(customerCode);
            customer.setCustomerName(customerName);
            customer.setAreaName(areaName);
            customer.setPhone(phone);
            customer.setAddress(address);
            customer.setBalance(BigDecimal.ZERO);
            customer.setStatus(1);
            customer.setCreateTime(LocalDateTime.now());
            customer.setUpdateTime(LocalDateTime.now());
            customer.setCreateBy("JICHENG_SYNC");
            customerMapper.insert(customer);
        }
    }

    private int syncMeterArchives(Long vendorId) {
        int synced = 0;
        int page = 1;
        while (true) {
            List<Map<String, Object>> items = jichengClient.fetchMeterArchive(page, vendorId);
            if (items == null || items.isEmpty()) {
                break;
            }

            for (Map<String, Object> item : items) {
                try {
                    syncOneMeter(item, vendorId);
                    synced++;
                } catch (Exception e) {
                    log.error("积成表计档案同步异常 item={}, {}", item, e.getMessage());
                }
            }
            page++;
        }
        return synced;
    }

    @Transactional(rollbackFor = Exception.class)
    public void syncOneMeter(Map<String, Object> item, Long vendorId) {
        String stealNo = strVal(item, "steal_no");
        String meterTypeName = strVal(item, "meter_type_name");
        String vender = strVal(item, "vender");
        String caliber = strVal(item, "caliber");
        String switchOp = strVal(item, "switch_op");
        String startVal = strVal(item, "start_val");
        String address = strVal(item, "address");
        String customerCode = strVal(item, "customer_code");

        if (stealNo == null || stealNo.isBlank()) {
            log.warn("积成表计档案: stealNo 为空，跳过 item={}", item);
            return;
        }

        Meter existing = meterMapper.selectOne(
                new LambdaQueryWrapper<Meter>()
                        .eq(Meter::getStealNo, stealNo));

        if (existing != null) {
            if (meterTypeName != null) existing.setMeterTypeName(meterTypeName);
            if (vender != null) existing.setVender(vender);
            if (caliber != null) existing.setCaliber(caliber);
            if (switchOp != null) existing.setSwitchOp(switchOp);
            if (startVal != null) existing.setStartVal(startVal);
            if (address != null) existing.setAddress(address);
            if (vendorId != null) existing.setWaterVendorId(vendorId);
            existing.setUpdateTime(LocalDateTime.now());
            meterMapper.updateById(existing);
        } else {
            Meter meter = new Meter();
            meter.setStealNo(stealNo);
            meter.setMeterTypeName(meterTypeName);
            meter.setVender(vender);
            meter.setCaliber(caliber);
            meter.setSwitchOp(switchOp);
            meter.setStartVal(startVal);
            meter.setAddress(address);
            meter.setMeterType(0);
            meter.setWaterVendorId(vendorId);
            meter.setStatus(1);
            meter.setOnline(0);
            meter.setCreateTime(LocalDateTime.now());
            meter.setUpdateTime(LocalDateTime.now());
            meter.setCreateBy("JICHENG_SYNC");

            if (customerCode != null && !customerCode.isBlank()) {
                Customer customer = customerMapper.selectOne(
                        new LambdaQueryWrapper<Customer>()
                                .eq(Customer::getCustomerCode, customerCode));
                if (customer != null) {
                    meter.setCustomerId(customer.getId());
                }
            }

            meterMapper.insert(meter);
        }
    }

    private String strVal(Map<String, Object> map, String key) {
        Object val = map.get(key);
        return val != null ? val.toString().trim() : null;
    }
}
