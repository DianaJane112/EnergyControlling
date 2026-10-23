package com.competition.energycontrollerplatform.integration.hook;

/**
 * Netty 阀控指令下发 Hook 接口。
 * <p>
 * 当余额清零、欠费停机等业务操作需要联动硬件侧执行分合闸时，
 * 业务层通过此接口下发控制指令，由 Netty 通信层完成实际的 TCP 指令发送。
 * <p>
 * 默认实现为空操作（No-Op），待 Netty 通道就绪后可替换为真实实现。
 */
public interface ValveControlHook {

    /**
     * 下发开阀指令。
     *
     * @param meterId    表计ID
     * @param stealNo    表钢印号（积成协议）/ 水表编号（捷先协议）
     * @param operator   操作人
     * @param reason     操作原因
     */
    default void openValve(Long meterId, String stealNo, String operator, String reason) {
        // NO-OP: 默认空实现，待 Netty 通道就绪后替换
    }

    /**
     * 下发关阀指令。
     *
     * @param meterId    表计ID
     * @param stealNo    表钢印号（积成协议）/ 水表编号（捷先协议）
     * @param operator   操作人
     * @param reason     操作原因
     */
    default void closeValve(Long meterId, String stealNo, String operator, String reason) {
        // NO-OP: 默认空实现，待 Netty 通道就绪后替换
    }

    /**
     * 下发余额清零后的状态同步指令。
     *
     * @param meterId    表计ID
     * @param stealNo    表钢印号
     * @param operator   操作人
     */
    default void syncAfterClearBalance(Long meterId, String stealNo, String operator) {
        // NO-OP: 默认空实现
    }
}
