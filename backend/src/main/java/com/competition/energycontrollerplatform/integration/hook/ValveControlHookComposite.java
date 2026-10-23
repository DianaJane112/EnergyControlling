package com.competition.energycontrollerplatform.integration.hook;

import com.competition.energycontrollerplatform.entity.Meter;
import com.competition.energycontrollerplatform.mapper.MeterMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 阀控 Hook 组合分发器。
 * <p>
 * 根据表计的厂商类型自动分发到对应的 Hook 实现：
 * <ul>
 *   <li>有 {@code meteraddress} → 捷先水表 → {@link JiexianValveControlHook}（HTTP sendTask）</li>
 *   <li>有 {@code stealNo} → 积成水表 → {@link JichengValveControlHook}（TCP Netty）</li>
 *   <li>两者都为空 → 跳过，记录 warn 日志</li>
 * </ul>
 * <p>
 * 实现 {@link ValveControlHook} 接口，可直接替换原有的 No-Op 实现，
 * 对业务层（{@code MeterServiceImpl} / {@code ChargeServiceImpl} / {@code MeterStatusService}）透明。
 */
@Component
@RequiredArgsConstructor
public class ValveControlHookComposite implements ValveControlHook {

    private static final Logger log = LoggerFactory.getLogger(ValveControlHookComposite.class);

    private final MeterMapper meterMapper;
    private final JiexianValveControlHook jiexianHook;
    private final JichengValveControlHook jichengHook;

    @Override
    public void openValve(Long meterId, String stealNo, String operator, String reason) {
        dispatch(meterId, stealNo, operator, reason, Action.OPEN);
    }

    @Override
    public void closeValve(Long meterId, String stealNo, String operator, String reason) {
        dispatch(meterId, stealNo, operator, reason, Action.CLOSE);
    }

    @Override
    public void syncAfterClearBalance(Long meterId, String stealNo, String operator) {
        // 余额清零 → 关阀
        dispatch(meterId, stealNo, operator, "余额清零自动关阀", Action.CLOSE);
    }

    // ──────── 内部分发 ────────

    private enum Action { OPEN, CLOSE }

    private void dispatch(Long meterId, String stealNo, String operator,
                          String reason, Action action) {
        Meter meter = meterMapper.selectById(meterId);
        if (meter == null) {
            log.warn("[阀控分发] 表计不存在 meterId={}", meterId);
            return;
        }

        // 捷先路径：有 meteraddress
        if (meter.getMeteraddress() != null && !meter.getMeteraddress().isBlank()) {
            if (action == Action.OPEN) {
                jiexianHook.openValve(meterId, meter.getMeteraddress(), operator, reason);
            } else {
                jiexianHook.closeValve(meterId, meter.getMeteraddress(), operator, reason);
            }
            return;
        }

        // 积成路径：有 stealNo
        if (meter.getStealNo() != null && !meter.getStealNo().isBlank()) {
            if (action == Action.OPEN) {
                jichengHook.openValve(meterId, meter.getStealNo(), operator, reason);
            } else {
                jichengHook.closeValve(meterId, meter.getStealNo(), operator, reason);
            }
            return;
        }

        // 无法识别厂商
        log.warn("[阀控分发] 表计无法识别厂商类型 meterId={} meteraddress 和 stealNo 均为空，"
                + "跳过阀控操作 action={}", meterId, action);
    }
}
