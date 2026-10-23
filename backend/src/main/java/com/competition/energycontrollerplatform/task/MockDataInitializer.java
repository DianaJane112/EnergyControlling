package com.competition.energycontrollerplatform.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.competition.energycontrollerplatform.entity.Meter;
import com.competition.energycontrollerplatform.entity.MeterReading;
import com.competition.energycontrollerplatform.mapper.MeterMapper;
import com.competition.energycontrollerplatform.mapper.MeterReadingMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;

/**
 * Mock 数据定时生成器（已停用）。
 * 每 10 秒为现有表计插入一条逼真的模拟读数，
 * 用于在无硬件对接时驱动前端图表。
 *
 * @deprecated 真实 Netty TCP 网关已上线，Mock 任务已禁用。
 *             如需恢复测试，取消下方 {@code @Component} 注释并取消 {@code @Scheduled} 注释。
 */
// @Component — 已由真实 Netty TCP 网关取代
@RequiredArgsConstructor
@Deprecated
public class MockDataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(MockDataInitializer.class);

    private final MeterMapper meterMapper;
    private final MeterReadingMapper meterReadingMapper;
    private final Random random = new Random();

    @Override
    public void run(String... args) {
        log.info("Mock 数据生成器已就绪，每 10 秒生成一次模拟读数");
    }

    /* 对接真实三方后注释掉，保留方法体供参考 */
    // @Scheduled(fixedDelay = 10_000, initialDelay = 3_000)
    public void generateMockReading() {
        List<Meter> meters = meterMapper.selectList(new LambdaQueryWrapper<>());
        if (meters.isEmpty()) {
            log.debug("暂无表计，跳过 Mock 数据生成");
            return;
        }

        for (Meter meter : meters) {
            try {
                MeterReading reading = buildReading(meter);
                meterReadingMapper.insert(reading);
            } catch (Exception e) {
                log.error("生成 Mock 数据失败 meterId={}", meter.getId(), e);
            }
        }
        log.info("已为 {} 块表计生成模拟读数", meters.size());
    }

    /**
     * 构造一条模拟读数
     */
    private MeterReading buildReading(Meter meter) {
        BigDecimal voltA = randomVoltage();
        BigDecimal voltB = randomVoltage();
        BigDecimal voltC = randomVoltage();

        BigDecimal ampA = randomCurrent();
        BigDecimal ampB = randomCurrent();
        BigDecimal ampC = randomCurrent();

        BigDecimal actA = voltA.multiply(ampA).setScale(4, RoundingMode.HALF_UP);
        BigDecimal actB = voltB.multiply(ampB).setScale(4, RoundingMode.HALF_UP);
        BigDecimal actC = voltC.multiply(ampC).setScale(4, RoundingMode.HALF_UP);
        BigDecimal act = actA.add(actB).add(actC);

        BigDecimal freq = BigDecimal.valueOf(49.8 + random.nextDouble() * 0.4)
                .setScale(4, RoundingMode.HALF_UP);

        BigDecimal fact = BigDecimal.valueOf(0.85 + random.nextDouble() * 0.14)
                .setScale(4, RoundingMode.HALF_UP);

        // 增量 kWh：按 10 秒间隔计算 (P * 10s / 3600 / 1000)
        BigDecimal kwhIncrement = act.multiply(BigDecimal.valueOf(10))
                .divide(BigDecimal.valueOf(3_600_000), 6, RoundingMode.HALF_UP);

        // 查询最近一次的累计电量
        BigDecimal lastKwh = getLastKwh(meter.getId());
        BigDecimal kwh = (lastKwh != null ? lastKwh : BigDecimal.ZERO).add(kwhIncrement);

        long now = System.currentTimeMillis();

        MeterReading r = new MeterReading();
        r.setMeterId(meter.getId());
        r.setReport(false);
        r.setCollectorCode(1001L);
        r.setMeterCode((long) meter.getId());
        r.setTimestamp(LocalDateTime.now());

        r.setVoltA(voltA);
        r.setVoltB(voltB);
        r.setVoltC(voltC);
        r.setFreq(freq);
        r.setAmpA(ampA);
        r.setAmpB(ampB);
        r.setAmpC(ampC);
        r.setFact(fact);
        r.setAct(act);
        r.setActA(actA);
        r.setActB(actB);
        r.setActC(actC);
        r.setKwh(kwh);
        r.setCreateTime(LocalDateTime.now());
        r.setUpdateTime(LocalDateTime.now());

        return r;
    }

    private BigDecimal getLastKwh(Long meterId) {
        LambdaQueryWrapper<MeterReading> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MeterReading::getMeterId, meterId)
               .isNotNull(MeterReading::getKwh)
               .orderByDesc(MeterReading::getTimestamp)
               .last("LIMIT 1");
        List<MeterReading> list = meterReadingMapper.selectList(wrapper);
        return list.isEmpty() ? null : list.get(0).getKwh();
    }

    private BigDecimal randomVoltage() {
        double v = 218.0 + random.nextDouble() * 7.0; // 218 ~ 225
        return BigDecimal.valueOf(v).setScale(4, RoundingMode.HALF_UP);
    }

    private BigDecimal randomCurrent() {
        double a = 1.5 + random.nextDouble() * 6.5; // 1.5 ~ 8.0
        return BigDecimal.valueOf(a).setScale(4, RoundingMode.HALF_UP);
    }
}
