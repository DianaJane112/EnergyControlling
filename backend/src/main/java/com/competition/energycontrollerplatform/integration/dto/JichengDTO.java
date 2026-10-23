package com.competition.energycontrollerplatform.integration.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 积成接口响应内层模型
 */
public class JichengDTO {

    /** GetMeterReading 外层包裹 */
    @Data
    public static class MeterReadingResponse {
        private ReadingData data;
    }

    @Data
    public static class ReadingData {
        private List<ReadingItem> all;
    }

    @Data
    public static class ReadingItem {
        @JsonProperty("customer_code")
        private String customerCode;
        @JsonProperty("steal_no")
        private String stealNo;
        private String vender;
        @JsonProperty("read_date")
        private String readDate;
        @JsonProperty("read_val")
        private BigDecimal readVal;
        @JsonProperty("read_jval")
        private BigDecimal readJval;
        private String sign;
    }

    /** GetMeterState 外层包裹 */
    @Data
    public static class MeterStateResponse {
        private StateData data;
    }

    @Data
    public static class StateData {
        private List<StateItem> all;
    }

    @Data
    public static class StateItem {
        @JsonProperty("customer_code")
        private String customerCode;
        @JsonProperty("steal_no")
        private String stealNo;
        private String vender;
        private String status;    // 0正常/1欠费/2异常
        private String message;   // 报警信息
        @JsonProperty("record_date")
        private String recordDate;
        private String sign;
    }

    /** 通用输入请求 */
    @Data
    public static class ApiRequest {
        private String sign;
    }

    @Data
    public static class ValveControlRequest {
        private List<ValveControlRow> row;
    }

    @Data
    public static class ValveControlRow {
        private String stealNo;
        private String vender;
        private String type;
        private String operator;
        private String sign;
    }
}
