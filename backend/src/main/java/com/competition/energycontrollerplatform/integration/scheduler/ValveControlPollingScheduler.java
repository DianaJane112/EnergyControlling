package com.competition.energycontrollerplatform.integration.scheduler;

import com.competition.energycontrollerplatform.entity.Meter;
import com.competition.energycontrollerplatform.entity.ValveControl;
import com.competition.energycontrollerplatform.integration.dto.JiexianDTO;
import com.competition.energycontrollerplatform.integration.http.client.JiexianWaterClient;
import com.competition.energycontrollerplatform.mapper.MeterMapper;
import com.competition.energycontrollerplatform.mapper.ValveControlMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 阀控指令状态轮询调度器。
 * <p>
 * 每 30 秒查询 {@code t_valve_control} 中 {@code task_status = 'PENDING'} 的任务，
 * 调用捷先 {@code getFmState} 接口查询指令执行结果，更新任务状态和表计阀门状态。
 * <p>
 * <b>轮询策略：</b>
 * <ul>
 *   <li>频率：每 30 秒一次，首次延迟 60 秒（等待 sendTask 完成后有结果可查）</li>
 *   <li>超时处理：超过 5 分钟的 PENDING 任务自动标记为 TIMEOUT</li>
 *   <li>失败重试：不自动重试，由管理员在后台手动重新下发</li>
 * </ul>
 * <p>
 * <b>状态流转：</b>
 * <pre>
 * PENDING → (getFmState) → SUCCESS / FAILED / OFFLINE / TIMEOUT
 * </pre>
 */
@Component
@RequiredArgsConstructor
public class ValveControlPollingScheduler {

    private static final Logger log = LoggerFactory.getLogger(ValveControlPollingScheduler.class);

    /** 轮询超时时间（分钟） */
    private static final int TIMEOUT_MINUTES = 5;

    private final JiexianWaterClient jiexianClient;
    private final ValveControlMapper valveControlMapper;
    private final MeterMapper meterMapper;

    /**
     * 定时轮询 PENDING 状态的阀控指令执行结果。
     * <p>
     * 每 30 秒执行一次，首次延迟 60 秒。
     * 先处理超时任务（超过 5 分钟未完成），再逐条查询捷先平台侧的执行状态。
     */
    @Scheduled(fixedDelay = 30_000, initialDelay = 60_000)
    public void pollValveControlStatus() {
        try {
            // ① 先处理超时任务
            int timedOut = valveControlMapper.markTimeoutTasks(TIMEOUT_MINUTES);
            if (timedOut > 0) {
                log.warn("[阀控轮询] 已将 {} 条超时任务标记为 TIMEOUT", timedOut);
            }

            // ② 查询当前 PENDING 任务
            List<ValveControl> pendingTasks = valveControlMapper.selectPendingTasks();
            if (pendingTasks.isEmpty()) {
                log.debug("[阀控轮询] 无待轮询的阀控任务");
                return;
            }

            log.info("[阀控轮询] 开始轮询 {} 条 PENDING 阀控任务", pendingTasks.size());
            int successCount = 0;
            int failCount = 0;

            for (ValveControl task : pendingTasks) {
                try {
                    // ③ 调用捷先 getFmState 查询执行结果
                    JiexianDTO.ApiResponse<JiexianDTO.FmStateItem> resp =
                            jiexianClient.getFmState(task.getBatchid(),
                                    task.getMeterId() != null && meterMapper != null
                                            ? (meterMapper.selectById(task.getMeterId()) != null
                                            ? meterMapper.selectById(task.getMeterId()).getWaterVendorId()
                                            : null)
                                            : null);

                    if (resp == null) {
                        log.debug("[阀控轮询] getFmState 返回 null batchid={}，等待下次轮询", task.getBatchid());
                        continue;
                    }

                    if (resp.getCode() != null && resp.getCode() != 200) {
                        // API 返回错误，暂不更新状态，等待下次轮询或超时
                        log.warn("[阀控轮询] getFmState 返回错误 batchid={} code={} msg={}",
                                task.getBatchid(), resp.getCode(), resp.getMsg());
                        continue;
                    }

                    if (resp.getData() == null || resp.getData().isEmpty()) {
                        log.debug("[阀控轮询] getFmState 无 data batchid={}，等待下次轮询", task.getBatchid());
                        continue;
                    }

                    // ④ 解析 taskStatus 并更新
                    JiexianDTO.FmStateItem item = resp.getData().get(0);
                    String taskStatus = item.getTaskStatus();
                    if (taskStatus == null || taskStatus.isBlank()) {
                        continue;
                    }

                    valveControlMapper.updateTaskStatus(task.getBatchid(),
                            taskStatus, null, resp.getMsg());

                    // ⑤ 同步更新 t_meter.state_valve
                    if (meterMapper != null && task.getMeterId() != null) {
                        syncMeterValveState(task, taskStatus);
                    }

                    if ("SUCCESS".equalsIgnoreCase(taskStatus) || "1".equals(taskStatus)) {
                        successCount++;
                        log.info("[阀控轮询] 任务成功 batchid={} meterId={} status={}",
                                task.getBatchid(), task.getMeterId(), taskStatus);
                    } else {
                        failCount++;
                        log.warn("[阀控轮询] 任务失败 batchid={} meterId={} status={} msg={}",
                                task.getBatchid(), task.getMeterId(), taskStatus, resp.getMsg());
                    }
                } catch (Exception e) {
                    log.error("[阀控轮询] 轮询异常 batchid={} meterId={}: {}",
                            task.getBatchid(), task.getMeterId(), e.getMessage());
                }
            }

            log.info("[阀控轮询] 本轮完成: 成功 {} 条, 失败 {} 条, 仍有 {} 条 PENDING",
                    successCount, failCount,
                    pendingTasks.size() - successCount - failCount);
        } catch (Exception e) {
            log.error("[阀控轮询] 整体轮询异常", e);
        }
    }

    /**
     * 根据阀控任务执行结果同步更新 {@code t_meter.state_valve}。
     * <p>
     * 捷先返回的 taskStatus 可能为：
     * <ul>
     *   <li>"1" / "SUCCESS" → 命令执行成功，保持当前 state_valve</li>
     *   <li>"0" / "FAILED"  → 命令执行失败，回滚 state_valve（开阀失败→恢复关阀状态，关阀失败→恢复开阀状态）</li>
     *   <li>"112" → 终端不在线</li>
     *   <li>"123" → 集中器超时</li>
     * </ul>
     */
    private void syncMeterValveState(ValveControl task, String taskStatus) {
        try {
            Meter meter = meterMapper.selectById(task.getMeterId());
            if (meter == null) return;

            boolean isOpenCmd = task.getCtrlCode1() != null && task.getCtrlCode1() == 1;
            boolean isFailed = "0".equals(taskStatus) || "FAILED".equalsIgnoreCase(taskStatus)
                    || "112".equals(taskStatus) || "123".equals(taskStatus);

            if (isFailed) {
                // 指令失败 → 回滚到操作前的状态
                int rollbackValveState = isOpenCmd ? 2 : 1; // 开阀失败→保持关阀; 关阀失败→保持开阀
                int rollbackStatus = isOpenCmd ? 2 : 1;
                meterMapper.updateValveState(task.getMeterId(), rollbackStatus, rollbackValveState);
                log.warn("[阀控轮询] 阀控失败回滚 meterId={} action={} taskStatus={} rollbackStateValve={}",
                        task.getMeterId(), isOpenCmd ? "OPEN" : "CLOSE",
                        taskStatus, rollbackValveState);
            }
            // 成功时 state_valve 已在 openValve/closeValve 中预设，无需额外处理
        } catch (Exception e) {
            log.error("[阀控轮询] 同步表计阀门状态失败 meterId={}: {}",
                    task.getMeterId(), e.getMessage());
        }
    }
}
