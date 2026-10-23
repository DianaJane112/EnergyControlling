package com.competition.energycontrollerplatform.common.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

/**
 * 表计类型枚举（电表 vs 水表）。
 * <p>
 * 用于替代原先通过厂商字段隐式推断表计类型的方式，
 * 在 t_meter 表上以显式 INT 列（meter_type）存储。
 * </p>
 *
 * <ul>
 *   <li>{@link #ELECTRIC} — 电表（辰鳗等）</li>
 *   <li>{@link #WATER} — 水表（积成、捷先等）</li>
 * </ul>
 */
@Getter
public enum MeterType {

    /** 水表 */
    WATER(0, "水表"),

    /** 电表 */
    ELECTRIC(1, "电表");

    @EnumValue
    private final int code;

    @JsonValue
    private final String label;

    MeterType(int code, String label) {
        this.code = code;
        this.label = label;
    }

    /**
     * 根据 code 反解析枚举。
     *
     * @param code 0=水表, 1=电表
     * @return 对应的枚举，若 code 非法则返回 null
     */
    public static MeterType fromCode(Integer code) {
        if (code == null) return null;
        for (MeterType t : values()) {
            if (t.code == code) return t;
        }
        return null;
    }

    /**
     * 返回大写字符串标识，与报表表（tb_meter_daily_report / tb_meter_monthly_report）
     * 中的 meter_type 字符串值保持一致。
     */
    public String toReportType() {
        return this == ELECTRIC ? "ELECTRIC" : "WATER";
    }
}
