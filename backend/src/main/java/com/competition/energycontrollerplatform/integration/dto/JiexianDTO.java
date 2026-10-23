package com.competition.energycontrollerplatform.integration.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;
import java.math.BigDecimal;

/**
 * 捷先接口响应内层模型
 */
public class JiexianDTO {

    /** 通用外层响应 */
    @Data
    public static class ApiResponse<T> {
        private Integer code;
        private List<T> data;
        private String msg;
    }

    /** getDataValue 输出 item */
    @Data
    public static class DataValueItem {
        @JsonProperty("Meteraddress")
        private String meteraddress;
        @JsonProperty("METERADDRESS")
        private String meteraddressUpper;
        @JsonProperty("Data_dt")
        private String dataDt;
        @JsonProperty("DATA_DT")
        private String dataDtUpper;
        @JsonProperty("Data_value0")
        private BigDecimal dataValue0;
        @JsonProperty("DATA_VALUE0")
        private BigDecimal dataValue0Upper;
        private String loadDt;
        private String chargeCustomerNo;
        private String customerName;
        private String address;
        private String meterCaliber;
        private String signalStrength;
        private String batteryVoltage;
        private String statusName;
        private String facCode;

        public String effectiveMeteraddress() {
            return meteraddressUpper != null ? meteraddressUpper : meteraddress;
        }

        public String effectiveDataDt() {
            return dataDtUpper != null ? dataDtUpper : dataDt;
        }

        public BigDecimal effectiveDataValue0() {
            return dataValue0Upper != null ? dataValue0Upper : dataValue0;
        }
    }

    /** getMeterDate 输出 item */
    @Data
    public static class MeterDateItem {
        @JsonProperty("Custoer_no")   // 原文档拼写：Custoer_no
        private String customerNo;
        @JsonProperty("CUSTOMER_NO")
        private String customerNoUpper;
        @JsonProperty("Meteraddress")
        private String meteraddress;
        @JsonProperty("METERADDRESS")
        private String meteraddressUpper;
        @JsonProperty("Meter_caliber")
        private String meterCaliber;
        @JsonProperty("METER_CALIBER")
        private String meterCaliberUpper;
        @JsonProperty("Controlling")
        private Integer controlling;
        @JsonProperty("CONTROLLING")
        private Integer controllingUpper;
        @JsonProperty("StateValve")
        private Integer stateValve;
        @JsonProperty("STATEVALVE")
        private Integer stateValveUpper;
        @JsonProperty("signal_strength")
        private Integer signalStrength;
        @JsonProperty("SIGNAL_STRENGTH")
        private Integer signalStrengthUpper;
        @JsonProperty("battery_voltage")
        private Double batteryVoltage;
        @JsonProperty("BATTERY_VOLTAGE")
        private Double batteryVoltageUpper;

        public String effectiveCustomerNo() { return customerNoUpper != null ? customerNoUpper : customerNo; }
        public String effectiveMeteraddress() { return meteraddressUpper != null ? meteraddressUpper : meteraddress; }
        public String effectiveMeterCaliber() { return meterCaliberUpper != null ? meterCaliberUpper : meterCaliber; }
        public Integer effectiveControlling() { return controllingUpper != null ? controllingUpper : controlling; }
        public Integer effectiveStateValve() { return stateValveUpper != null ? stateValveUpper : stateValve; }
        public Integer effectiveSignalStrength() { return signalStrengthUpper != null ? signalStrengthUpper : signalStrength; }
        public Double effectiveBatteryVoltage() { return batteryVoltageUpper != null ? batteryVoltageUpper : batteryVoltage; }
    }

    /** getFmState 输出 item */
    @Data
    public static class FmStateItem {
        @JsonProperty("Meteraddress")
        private String meteraddress;
        @JsonProperty("Task_status")
        private String taskStatus;
    }

    /** sendTask / getFmState 输入 */
    @Data
    public static class ValveRequest {
        private String companycode;
        private List<ValveData> data;
    }

    @Data
    public static class ValveData {
        private String meteraddress;
        private String customerno;
        private String sendbody;
        @JsonProperty("CTRL_CODE1")
        private Integer ctrlCode1;
        @JsonProperty("CTRL_CODE2")
        private Integer ctrlCode2;
        private String batchid;
    }

    /** getDataValue 输入 */
    @Data
    public static class ReadingRequest {
        private String companycode;
        private List<ReadingParam> data;
    }

    @Data
    public static class ReadingParam {
        private String meteraddress;
        private String customerno;
        @JsonProperty("data_dt")
        private String dataDt;
        private Integer limit;
    }
}
