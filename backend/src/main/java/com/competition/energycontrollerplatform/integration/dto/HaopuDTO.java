package com.competition.energycontrollerplatform.integration.dto;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import lombok.Data;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 昊普电表接口 DTO。
 * <p>
 * 对应《电表数据采集接口文档》定义的 4 个接口的 JSON 结构。
 * 接口一推送数据与接口四返回数据结构一致，共用 {@link ReadingItem}。
 */
public class HaopuDTO {

    /** 接口二：采集器下的电表通信档案。 */
    @Data
    public static class MeterArchiveItem {
        private Integer serial;
        private String bpsType;
        private String bitType;
        private String protoType;
        private String deviceType;
        private Long meterCode;
    }

    // ======================== 接口一 & 接口四：电表采集数据 ========================

    /**
     * 单条电表采集数据（25 个字段）。
     * <p>
     * 接口一（主动推送）请求体为 JSON 数组，数组内每个对象即此结构。
     * 接口四（点抄）返回数据同样使用此结构，字段完全一致。
     * <p>
     * 所有数值类字段默认值为 0.0000，代表设备未采集到对应数据或数据异常，
     * 业务层需做无效数据过滤处理。
     */
    @Data
    public static class ReadingItem {

        private final Map<String, Object> extraFields = new LinkedHashMap<>();

        @JsonAnySetter
        public void captureExtraField(String name, Object value) {
            extraFields.put(name, value);
        }

        /** 是否为主动上报，接口一固定为 true */
        private Boolean report;

        /** 电表设备唯一编码（核心必填） */
        private Long meterCode;

        /** 采集设备唯一编码（核心必填） */
        private Long collectorCode;

        /** 数据采集时间戳（核心必填，Unix 毫秒） */
        private Long timestamp;

        // ==================== 电压（V） ====================

        /** A 相电压，默认 0.0000 */
        private BigDecimal voltA;

        /** B 相电压，默认 0.0000 */
        private BigDecimal voltB;

        /** C 相电压，默认 0.0000 */
        private BigDecimal voltC;

        // ==================== 频率（Hz） ====================

        /** 电网频率，默认 0.0000 */
        private BigDecimal freq;

        // ==================== 电流（A） ====================

        /** A 相电流，默认 0.0000 */
        private BigDecimal ampA;

        /** B 相电流，默认 0.0000 */
        private BigDecimal ampB;

        /** C 相电流，默认 0.0000 */
        private BigDecimal ampC;

        // ==================== 功率因数 ====================

        /** 总功率因数，默认 0.0000 */
        private BigDecimal fact;

        /** A 相功率因数，默认 0.0000 */
        private BigDecimal factA;

        /** B 相功率因数，默认 0.0000 */
        private BigDecimal factB;

        /** C 相功率因数，默认 0.0000 */
        private BigDecimal factC;

        // ==================== 有功功率（kW） ====================

        /** 总有功功率，默认 0.0000 */
        private BigDecimal act;

        /** A 相有功功率，默认 0.0000 */
        private BigDecimal actA;

        /** B 相有功功率，默认 0.0000 */
        private BigDecimal actB;

        /** C 相有功功率，默认 0.0000 */
        private BigDecimal actC;

        // ==================== 无功功率（kvar） ====================

        /** 总无功功率，默认 0.0000 */
        private BigDecimal react;

        /** A 相无功功率，默认 0.0000 */
        private BigDecimal reactA;

        /** B 相无功功率，默认 0.0000 */
        private BigDecimal reactB;

        /** C 相无功功率，默认 0.0000 */
        private BigDecimal reactC;

        // ==================== 电量（kWh） ====================

        /** 组合有功总电量，默认 0.0000 */
        private BigDecimal kwh;

        /** 组合有功尖电量，默认 0.0000 */
        private BigDecimal kwhPeak;

        /** 组合有功峰电量，默认 0.0000 */
        private BigDecimal kwhBusy;

        /** 组合有功平电量，默认 0.0000 */
        private BigDecimal kwhNormal;

        /** 组合有功谷电量，默认 0.0000 */
        private BigDecimal kwhIdle;
    }
}
