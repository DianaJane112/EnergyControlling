package com.competition.energycontrollerplatform.common;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 表计编号（dbNo）生成器。
 *
 * <h3>用途</h3>
 * <p>在新增表计时，若前端未传入 {@code dbNo}，则根据配置的编号格式自动生成。
 * 交付后甲方如需改变编号格式，只需修改 {@code application.yml} 中的
 * {@code meter.no.*} 配置项即可，无需改动 Java 代码。</p>
 *
 * <h3>配置示例（application.yml）</h3>
 * <pre>{@code
 * meter:
 *   no:
 *     # 电表编号前缀（meterType=1）
 *     electric-prefix: "ELEC"
 *     # 水表编号前缀（meterType=0）
 *     water-prefix: "WATER"
 *     # 编号总长度（前缀 + 零填充序列号），不足左侧补0
 *     total-length: 12
 * }</pre>
 *
 * <h3>生成规则</h3>
 * <p>{@code 前缀 + 零填充序列号}，总长度固定为 {@code total-length}。
 * 例如：电表前缀 "ELEC" + 总长度 12 + 序列号 1 → {@code ELEC000000001}。
 * 序列号位数 = total-length - prefix.length()。</p>
 *
 * <h3>如何自定义编号格式</h3>
 * <p>甲方运维人员只需编辑 {@code application.yml}（或通过环境变量覆盖）：</p>
 * <ul>
 *   <li>修改前缀：{@code electric-prefix} / {@code water-prefix}</li>
 *   <li>修改总长度：{@code total-length}</li>
 *   <li>如需更复杂的格式（如日期+流水号），可修改本类的 {@link #generate(Integer, long)} 方法</li>
 * </ul>
 *
 * @author EnergyControllerPlatform
 * @since 1.0.0
 */
@Component
public class DbNoGenerator {

    /** 电表编号前缀（默认 "01"） */
    @Value("${meter.no.electric-prefix:01}")
    private String electricPrefix;

    /** 水表编号前缀（默认 "02"） */
    @Value("${meter.no.water-prefix:02}")
    private String waterPrefix;

    /** 编号总长度（默认 12 位），包含前缀 */
    @Value("${meter.no.total-length:12}")
    private int totalLength;

    // ──────────────────────── 公共方法 ────────────────────────

    /**
     * 根据表计类型生成下一个编号。
     *
     * @param meterType 表计类型（0=水表, 1=电表）
     * @param sequence  自增序列号（由调用方从数据库查询最大序号 +1 得到）
     * @return 格式化的表计编号，如 "010000000025"
     */
    public String generate(Integer meterType, long sequence) {
        String prefix = getPrefix(meterType);
        int numLen = totalLength - prefix.length();
        if (numLen < 1) {
            numLen = 10; // 保底：前缀过长时保证至少10位数字
        }
        String format = "%0" + numLen + "d";
        return prefix + String.format(format, sequence);
    }

    /**
     * 根据表计编号（dbNo）提取前缀。
     * <p>由于前缀可能是纯数字或字母，无法通过简单规则拆分，
     * 这里直接返回当前配置的前缀。子类可覆写以支持更复杂逻辑。</p>
     *
     * @param meterType 表计类型（0=水表, 1=电表）
     * @return 对应类型的前缀
     */
    public String getPrefix(Integer meterType) {
        return (meterType != null && meterType == 1) ? electricPrefix : waterPrefix;
    }

    // ──────────────────────── Getter（便于甲方调试/审计） ────────────────────────

    public String getElectricPrefix() {
        return electricPrefix;
    }

    public String getWaterPrefix() {
        return waterPrefix;
    }

    public int getTotalLength() {
        return totalLength;
    }
}
