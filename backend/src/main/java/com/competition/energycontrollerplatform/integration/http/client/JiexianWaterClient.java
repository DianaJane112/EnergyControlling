package com.competition.energycontrollerplatform.integration.http.client;

import com.competition.energycontrollerplatform.integration.config.WaterMeterVendorConfigProvider;
import com.competition.energycontrollerplatform.integration.dto.JiexianDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.UUID;

/**
 * 捷先水表 HTTP 客户端。
 */
@Component
public class JiexianWaterClient {

    private static final Logger log = LoggerFactory.getLogger(JiexianWaterClient.class);

    private final RestTemplate restTemplate;
    private final WaterMeterVendorConfigProvider configProvider;
    private final ObjectMapper objectMapper;

    public JiexianWaterClient(WaterMeterVendorConfigProvider configProvider, ObjectMapper objectMapper) {
        this.configProvider = configProvider;
        this.objectMapper = objectMapper;
        this.restTemplate = new RestTemplate();
    }

    public JiexianDTO.ApiResponse<JiexianDTO.DataValueItem> fetchDataValue(String meteraddress, String dataDt) {
        return fetchDataValue(meteraddress, dataDt, null);
    }

    public JiexianDTO.ApiResponse<JiexianDTO.DataValueItem> fetchDataValue(
            String meteraddress, String dataDt, Long vendorId) {
        WaterMeterVendorConfigProvider.JiexianVendorCredentials credentials =
                configProvider.resolveJiexianCredentials(vendorId);
        if (isMissingConfig(credentials, "getDataValue")) {
            return null;
        }

        JiexianDTO.ReadingRequest request = new JiexianDTO.ReadingRequest();
        request.setCompanycode(credentials.companyCode());
        JiexianDTO.ReadingParam param = new JiexianDTO.ReadingParam();
        param.setMeteraddress(meteraddress);
        param.setDataDt(dataDt);
        request.setData(List.of(param));

        try {
            String url = credentials.baseUrl() + "/getDataValue.action?param={param}";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<JiexianDTO.ApiResponse<JiexianDTO.DataValueItem>> resp = restTemplate.exchange(
                    url, HttpMethod.POST, entity, new ParameterizedTypeReference<>() {},
                    objectMapper.writeValueAsString(request));
            log.debug("捷先 getDataValue meteraddress={} success vendorId={}", meteraddress, vendorId);
            return resp.getBody();
        } catch (JsonProcessingException | RestClientException e) {
            log.error("捷先 getDataValue 请求失败 meteraddress={} vendorId={}: {}",
                    meteraddress, vendorId, e.getMessage());
            return null;
        }
    }

    public JiexianDTO.ApiResponse<JiexianDTO.MeterDateItem> fetchMeterDate(String meteraddress, int page) {
        return fetchMeterDate(meteraddress, page, null);
    }

    public JiexianDTO.ApiResponse<JiexianDTO.MeterDateItem> fetchMeterDate(
            String meteraddress, int page, Long vendorId) {
        WaterMeterVendorConfigProvider.JiexianVendorCredentials credentials =
                configProvider.resolveJiexianCredentials(vendorId);
        if (isMissingConfig(credentials, "getMeterDate")) {
            return null;
        }

        JiexianDTO.ReadingRequest request = new JiexianDTO.ReadingRequest();
        request.setCompanycode(credentials.companyCode());
        JiexianDTO.ReadingParam param = new JiexianDTO.ReadingParam();
        param.setMeteraddress(meteraddress);
        param.setLimit(page);
        request.setData(List.of(param));

        try {
            String url = credentials.baseUrl() + "/getMeterDate.action?param={param}";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<JiexianDTO.ApiResponse<JiexianDTO.MeterDateItem>> resp = restTemplate.exchange(
                    url, HttpMethod.POST, entity, new ParameterizedTypeReference<>() {},
                    objectMapper.writeValueAsString(request));
            log.info("捷先 getMeterDate success page={} vendorId={}", page, vendorId);
            return resp.getBody();
        } catch (JsonProcessingException | RestClientException e) {
            log.error("捷先 getMeterDate 请求失败 page={} vendorId={}: {}", page, vendorId, e.getMessage());
            return null;
        }
    }

    /** 拉取捷先全部水表档案的单页；不传 meteraddress 时按 limit 分页。 */
    public JiexianDTO.ApiResponse<JiexianDTO.MeterDateItem> fetchMeterArchivePage(int page, Long vendorId) {
        WaterMeterVendorConfigProvider.JiexianVendorCredentials credentials =
                configProvider.resolveJiexianCredentials(vendorId);
        if (isMissingConfig(credentials, "getMeterDate archive")) return null;
        JiexianDTO.ReadingRequest request = new JiexianDTO.ReadingRequest();
        request.setCompanycode(credentials.companyCode());
        JiexianDTO.ReadingParam param = new JiexianDTO.ReadingParam();
        param.setLimit(page);
        request.setData(List.of(param));
        try {
            String url = credentials.baseUrl() + "/getMeterDate.action?param={param}";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            ResponseEntity<JiexianDTO.ApiResponse<JiexianDTO.MeterDateItem>> resp = restTemplate.exchange(
                    url, HttpMethod.POST, new HttpEntity<Void>(headers), new ParameterizedTypeReference<>() {},
                    objectMapper.writeValueAsString(request));
            return resp.getBody();
        } catch (JsonProcessingException | RestClientException e) {
            log.error("捷先 getMeterDate archive failed page={} vendorId={}: {}", page, vendorId, e.getMessage());
            return null;
        }
    }

    public JiexianDTO.ApiResponse<JiexianDTO.ValveData> sendTask(
            String meteraddress, String customerno, String sendbody,
            Integer ctrlCode1, Integer ctrlCode2) {
        return sendTask(meteraddress, customerno, sendbody, ctrlCode1, ctrlCode2, null);
    }

    public JiexianDTO.ApiResponse<JiexianDTO.ValveData> sendTask(
            String meteraddress, String customerno, String sendbody,
            Integer ctrlCode1, Integer ctrlCode2, Long vendorId) {
        WaterMeterVendorConfigProvider.JiexianVendorCredentials credentials =
                configProvider.resolveJiexianCredentials(vendorId);
        if (isMissingConfig(credentials, "sendTask")) {
            return null;
        }

        String url = credentials.baseUrl() + "/sendTask.action";

        JiexianDTO.ValveRequest request = new JiexianDTO.ValveRequest();
        request.setCompanycode(credentials.companyCode());

        JiexianDTO.ValveData data = new JiexianDTO.ValveData();
        data.setMeteraddress(meteraddress);
        data.setCustomerno(customerno);
        data.setSendbody(sendbody);
        data.setCtrlCode1(ctrlCode1);
        data.setCtrlCode2(ctrlCode2 != null ? ctrlCode2 : 0);
        String ts = String.valueOf(System.currentTimeMillis());
        data.setBatchid("V" + ts.substring(ts.length() - 8)
                + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase());
        request.setData(List.of(data));

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<JiexianDTO.ValveRequest> entity = new HttpEntity<>(request, headers);

            ResponseEntity<JiexianDTO.ApiResponse<JiexianDTO.ValveData>> resp =
                    restTemplate.exchange(url, HttpMethod.POST, entity,
                            new ParameterizedTypeReference<>() {});
            log.info("捷先 sendTask meteraddress={} ctrlCode1={} batchid={} success vendorId={}",
                    meteraddress, ctrlCode1, data.getBatchid(), vendorId);
            return resp.getBody();
        } catch (RestClientException e) {
            log.error("捷先 sendTask 请求失败 meteraddress={} ctrlCode1={} vendorId={}: {}",
                    meteraddress, ctrlCode1, vendorId, e.getMessage());
            return null;
        }
    }

    public JiexianDTO.ApiResponse<JiexianDTO.FmStateItem> getFmState(String batchid) {
        return getFmState(batchid, null);
    }

    public JiexianDTO.ApiResponse<JiexianDTO.FmStateItem> getFmState(String batchid, Long vendorId) {
        WaterMeterVendorConfigProvider.JiexianVendorCredentials credentials =
                configProvider.resolveJiexianCredentials(vendorId);
        if (isMissingConfig(credentials, "getFmState")) {
            return null;
        }

        String url = credentials.baseUrl() + "/getFmState.action";

        JiexianDTO.ValveRequest request = new JiexianDTO.ValveRequest();
        request.setCompanycode(credentials.companyCode());

        JiexianDTO.ValveData data = new JiexianDTO.ValveData();
        data.setBatchid(batchid);
        request.setData(List.of(data));

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<JiexianDTO.ValveRequest> entity = new HttpEntity<>(request, headers);

            ResponseEntity<JiexianDTO.ApiResponse<JiexianDTO.FmStateItem>> resp =
                    restTemplate.exchange(url, HttpMethod.POST, entity,
                            new ParameterizedTypeReference<>() {});
            log.debug("捷先 getFmState batchid={} success vendorId={}", batchid, vendorId);
            return resp.getBody();
        } catch (RestClientException e) {
            log.error("捷先 getFmState 请求失败 batchid={} vendorId={}: {}", batchid, vendorId, e.getMessage());
            return null;
        }
    }

    private boolean isMissingConfig(WaterMeterVendorConfigProvider.JiexianVendorCredentials credentials,
                                    String action) {
        if (credentials.baseUrl() == null || credentials.baseUrl().isBlank()) {
            log.warn("捷先 baseUrl 未配置，跳过 {}", action);
            return true;
        }
        if (credentials.companyCode() == null || credentials.companyCode().isBlank()) {
            log.warn("捷先 companyCode 未配置，跳过 {}", action);
            return true;
        }
        return false;
    }
}
