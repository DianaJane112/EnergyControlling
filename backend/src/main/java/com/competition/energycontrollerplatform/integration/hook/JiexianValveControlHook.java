package com.competition.energycontrollerplatform.integration.hook;

import com.competition.energycontrollerplatform.entity.Meter;
import com.competition.energycontrollerplatform.entity.ValveControl;
import com.competition.energycontrollerplatform.integration.dto.JiexianDTO;
import com.competition.energycontrollerplatform.integration.http.client.JiexianWaterClient;
import com.competition.energycontrollerplatform.mapper.MeterMapper;
import com.competition.energycontrollerplatform.mapper.ValveControlMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 捷先水表阀控 Hook（HTTP 路径）。
 * <p>
 * 通过捷先 HTTP API（sendTask）下发阀控指令，
 * 写入 {@code t_valve_control} 审计记录供后续 {@code ValveControlPollingScheduler} 轮询执行结果。
 * <p>
 * 控制码说明（捷先协议）：
 * <ul>
 *   <li>ctrlCode1 = 1 → 开阀（合闸）</li>
 *   <li>ctrlCode1 = 2 → 关阀（分闸）</li>
 *   <li>ctrlCode2 = 0 → 备用，通常为 0</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
public class JiexianValveControlHook {

    private static final Logger log = LoggerFactory.getLogger(JiexianValveControlHook.class);

    /** 开阀控制码 */
    private static final int CTRL_OPEN = 1;
    /** 关阀控制码 */
    private static final int CTRL_CLOSE = 2;

    private final JiexianWaterClient jiexianClient;
    private final ValveControlMapper valveControlMapper;
    private final MeterMapper meterMapper;

    /**
     * 下发开阀指令。
     *
     * @param meterId      表计ID
     * @param meteraddress 捷先表计编号
     * @param operator     操作人
     * @param reason       操作原因
     */
    public void openValve(Long meterId, String meteraddress, String operator, String reason) {
        sendValveCommand(meterId, meteraddress, operator, reason, CTRL_OPEN, "OPEN");
    }

    /**
     * 下发关阀指令。
     *
     * @param meterId      表计ID
     * @param meteraddress 捷先表计编号
     * @param operator     操作人
     * @param reason       操作原因
     */
    public void closeValve(Long meterId, String meteraddress, String operator, String reason) {
        sendValveCommand(meterId, meteraddress, operator, reason, CTRL_CLOSE, "CLOSE");
    }

    // ──────── 私有方法 ────────

    private void sendValveCommand(Long meterId, String meteraddress, String operator,
                                   String reason, int ctrlCode, String actionType) {
        if (meteraddress == null || meteraddress.isBlank()) {
            log.warn("[捷先阀控] meteraddress 为空，跳过 meterId={}", meterId);
            return;
        }

        Meter meter = meterMapper.selectById(meterId);
        if (meter == null) {
            log.warn("[捷先阀控] 表计不存在 meterId={}", meterId);
            return;
        }

        // 构造送电/断电指令体
        String sendbody = buildSendBody(actionType, meter);

        // 调用捷先 sendTask API
        JiexianDTO.ApiResponse<JiexianDTO.ValveData> resp =
                jiexianClient.sendTask(meteraddress, meter.getCustomerNo(),
                        sendbody, ctrlCode, 0, meter.getWaterVendorId());

        if (resp == null || resp.getCode() == null || resp.getCode() != 200) {
            log.error("[捷先阀控] sendTask 失败 meterId={} meteraddress={} action={} "
                    + "resp={}", meterId, meteraddress, actionType, resp);
            return;
        }

        // 提取 batchid 写入审计表
        String batchid = null;
        if (resp.getData() != null && !resp.getData().isEmpty()) {
            batchid = resp.getData().get(0).getBatchid();
        }

        ValveControl vc = new ValveControl();
        vc.setMeterId(meterId);
        vc.setMeteraddress(meteraddress);
        vc.setCompanycode(meter.getCompanycode());
        vc.setCustomerno(meter.getCustomerNo());
        vc.setSendbody(sendbody);
        vc.setCtrlCode1(ctrlCode);
        vc.setCtrlCode2(0);
        vc.setBatchid(batchid != null ? batchid : "N/A");
        vc.setTaskStatus("PENDING");
        vc.setType("JX_HTTP");
        vc.setStealNo(meter.getStealNo());
        vc.setVender(meter.getVender());
        vc.setOperator(operator);
        vc.setOperateDate(LocalDateTime.now());
        vc.setMsg(reason);
        vc.setCreateTime(LocalDateTime.now());
        vc.setUpdateTime(LocalDateTime.now());
        vc.setCreateBy(operator);
        valveControlMapper.insert(vc);

        log.info("[捷先阀控] sendTask 成功 meterId={} meteraddress={} action={} batchid={}",
                meterId, meteraddress, actionType, batchid);
    }

    /**
     * 构造捷先阀控指令体（JSON 格式）。
     * <p>
     * 具体格式需根据捷先协议文档调整，此处提供一个通用模板。
     */
    private String buildSendBody(String actionType, Meter meter) {
        // 捷先 sendbody 格式待协议文档确认，此处生成合规 JSON
        return String.format("{\"action\":\"%s\",\"meteraddress\":\"%s\"}",
                actionType, meter.getMeteraddress());
    }
}
