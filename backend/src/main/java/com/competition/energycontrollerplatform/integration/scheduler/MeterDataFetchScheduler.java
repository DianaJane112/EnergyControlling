package com.competition.energycontrollerplatform.integration.scheduler;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.competition.energycontrollerplatform.entity.Meter;
import com.competition.energycontrollerplatform.integration.http.client.JiexianWaterClient;
import com.competition.energycontrollerplatform.integration.http.client.JiexianV5Client;
import com.competition.energycontrollerplatform.integration.http.client.JichengWaterClient;
import com.competition.energycontrollerplatform.integration.dto.JiexianDTO;
import com.competition.energycontrollerplatform.integration.dto.JichengDTO;
import com.competition.energycontrollerplatform.integration.service.MeterDataSyncService;
import com.competition.energycontrollerplatform.integration.config.WaterMeterVendorConfigProvider;
import com.competition.energycontrollerplatform.mapper.MeterMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

/**
 * 三方数据定时拉取调度器。
 * 每 1 小时分别拉取积成/捷先水表数据，任何一家失败不影响其余。
 */
@Component
@RequiredArgsConstructor
public class MeterDataFetchScheduler {

    private static final Logger log = LoggerFactory.getLogger(MeterDataFetchScheduler.class);

    private final JichengWaterClient jichengClient;
    private final JiexianWaterClient jiexianClient;
    private final JiexianV5Client jiexianV5Client;
    private final MeterDataSyncService syncService;
    private final MeterMapper meterMapper;
    private final WaterMeterVendorConfigProvider waterMeterVendorConfigProvider;

    /** 每小时通过 iES-CMR GetMeterReading 拉取积成水表读数。 */
    @Scheduled(fixedDelay = 3_600_000, initialDelay = 30_000)
    public void fetchJichengData() {
        log.info("=== 开始拉取积成水表数据 ===");
        try {
            List<JichengDTO.ReadingItem> readings = jichengClient.fetchMeterReadings();
            if (readings == null) {
                log.warn("积成水表拉取失败或配置不完整，本轮跳过");
                return;
            }
            syncService.syncJichengReadings(readings);
            log.info("=== 积成水表数据拉取完成 共 {} 条 ===", readings.size());
        } catch (Exception e) {
            log.error("积成水表整体拉取失败", e);
        }
    }

    /**
     * 定时拉取捷先水表读数（每小时执行一次）
     */
    @Scheduled(fixedDelay = 3_600_000, initialDelay = 45_000)
    public void fetchJiexianData() {
        log.info("=== 开始拉取捷先数据 ===");
        try {
            if (jiexianV5Client.isEnabled()) {
                List<JiexianDTO.DataValueItem> readings = jiexianV5Client.fetchRecentReadings();
                syncService.syncJiexianReadings(readings);
                log.info("=== Jiexian V5 sync completed, {} readings received ===", readings.size());
                return;
            }
            // 只拉取捷先水表：激活状态、未删除、水表类型、有表地址
            LambdaQueryWrapper<Meter> jiexianWrapper = new LambdaQueryWrapper<>();
            jiexianWrapper.eq(Meter::getMeterType, 0)
                          .eq(Meter::getStatus, 1)
                          .eq(Meter::getIsDeleted, 0)
                          .isNotNull(Meter::getMeteraddress)
                          .ne(Meter::getMeteraddress, "");
            List<Meter> meters = meterMapper.selectList(jiexianWrapper);
            if (meters == null || meters.isEmpty()) {
                log.warn("捷先拉取: 无符合条件的水表，跳过");
                return;
            }
            int synced = 0;
            for (Meter meter : meters) {
                if (meter.getMeteraddress() == null || meter.getMeteraddress().isBlank()) continue;
                try {
                    JiexianDTO.ApiResponse<JiexianDTO.DataValueItem> resp =
                            jiexianClient.fetchDataValue(meter.getMeteraddress(), null, meter.getWaterVendorId());
                    if (resp != null && resp.getCode() != null && resp.getCode() == 200
                            && resp.getData() != null) {
                        syncService.syncJiexianReadings(resp.getData());
                        synced++;
                    }
                } catch (Exception e) {
                    log.error("捷先单表拉取异常 meteraddress={}: {}", meter.getMeteraddress(), e.getMessage());
                }
            }
            log.info("=== 捷先数据拉取完成 成功 {} 条 ===", synced);
        } catch (Exception e) {
            log.error("捷先整体拉取失败", e);
        }
    }

    /** 启动约 1 分钟后首次同步，之后每 24 小时同步一次捷先水表档案。 */
    @Scheduled(initialDelay = 60_000, fixedDelay = 86_400_000)
    public void syncJiexianMeterArchives() {
        log.info("=== 开始同步捷先水表档案 ===");
        List<Long> vendorIds = waterMeterVendorConfigProvider.listEnabledVendorIdsByProtocol(
                WaterMeterVendorConfigProvider.PROTOCOL_JIEXIAN_HTTP);
        if (vendorIds.isEmpty()) vendorIds = Collections.singletonList(null);
        for (Long vendorId : vendorIds) {
            int page = 1;
            int total = 0;
            while (true) {
                JiexianDTO.ApiResponse<JiexianDTO.MeterDateItem> response =
                        jiexianClient.fetchMeterArchivePage(page, vendorId);
                if (response == null || response.getCode() == null || response.getCode() != 200
                        || response.getData() == null || response.getData().isEmpty()) break;
                total += syncService.syncJiexianMeterArchives(response.getData(), vendorId);
                if (response.getData().size() < 50) break;
                page++;
            }
            log.info("=== 捷先水表档案同步完成 vendorId={} total={} ===", vendorId, total);
        }
    }

    /**
     * 定时同步捷先水表基本信息（口径、阀控能力、信号强度、电池电压等）。
     * <p>
     * 每天凌晨 3:00 执行。捷先 getMeterDate 接口单次最多 50 条，
     * 分页拉取直到没有更多数据。
     */
    @Scheduled(cron = "0 0 3 * * ?")
    public void fetchJiexianMeterInfo() {
        log.info("=== 开始同步捷先水表基本信息 ===");
        try {
            // 查询所有需要同步基本信息的捷先水表
            LambdaQueryWrapper<Meter> jiexianWrapper = new LambdaQueryWrapper<>();
            jiexianWrapper.eq(Meter::getMeterType, 0)
                          .eq(Meter::getIsDeleted, 0)
                          .isNotNull(Meter::getMeteraddress)
                          .ne(Meter::getMeteraddress, "");
            List<Meter> meters = meterMapper.selectList(jiexianWrapper);
            if (meters == null || meters.isEmpty()) {
                log.info("捷先水表信息同步: 无符合条件的捷先水表，跳过");
                return;
            }
            log.info("捷先水表信息同步: 共 {} 台水表待同步基本信息", meters.size());

            int synced = 0;
            int skipped = 0;
            for (Meter meter : meters) {
                try {
                    JiexianDTO.ApiResponse<JiexianDTO.MeterDateItem> resp =
                            jiexianClient.fetchMeterDate(meter.getMeteraddress(), 1, meter.getWaterVendorId());
                    if (resp == null || resp.getCode() == null || resp.getCode() != 200
                            || resp.getData() == null || resp.getData().isEmpty()) {
                        log.debug("捷先水表信息同步: meteraddress={} 无返回数据，跳过", meter.getMeteraddress());
                        skipped++;
                        continue;
                    }

                    JiexianDTO.MeterDateItem item = resp.getData().get(0);
                    BigDecimal batteryVoltage = null;
                    if (item.getBatteryVoltage() != null) {
                        batteryVoltage = BigDecimal.valueOf(item.getBatteryVoltage());
                    }

                    int rows = meterMapper.updateJiexianMeterInfo(
                            meter.getId(),
                            item.getMeterCaliber(),
                            item.getControlling(),
                            item.getStateValve(),
                            item.getSignalStrength(),
                            batteryVoltage);
                    if (rows > 0) {
                        synced++;
                        log.debug("捷先水表信息同步: meterId={} meteraddress={} "
                                + "caliber={} controlling={} signal={} battery={}",
                                meter.getId(), meter.getMeteraddress(),
                                item.getMeterCaliber(), item.getControlling(),
                                item.getSignalStrength(), item.getBatteryVoltage());
                    }
                } catch (Exception e) {
                    log.error("捷先水表信息同步异常 meteraddress={}: {}",
                            meter.getMeteraddress(), e.getMessage());
                    skipped++;
                }
            }
            log.info("=== 捷先水表基本信息同步完成 成功 {} 条, 跳过 {} 条 ===", synced, skipped);
        } catch (Exception e) {
            log.error("捷先水表信息整体同步失败", e);
        }
    }
}
