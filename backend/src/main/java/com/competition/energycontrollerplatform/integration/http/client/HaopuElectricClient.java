package com.competition.energycontrollerplatform.integration.http.client;

import com.competition.energycontrollerplatform.integration.config.VendorApiProperties;
import com.competition.energycontrollerplatform.integration.dto.HaopuDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.List;

/** 昊普电表接口二/接口四客户端。文档规定两者均为 GET 且不带请求体。 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HaopuElectricClient {

    private final VendorApiProperties vendorApiProperties;
    private final RestTemplate restTemplate = new RestTemplate();

    public List<HaopuDTO.MeterArchiveItem> fetchMeterArchives(Long collectorCode) {
        return get("/collector/list/" + collectorCode,
                new ParameterizedTypeReference<List<HaopuDTO.MeterArchiveItem>>() {});
    }

    public List<HaopuDTO.ReadingItem> fetchLatestReadings(Long collectorCode) {
        return get("/collector/kwh/" + collectorCode,
                new ParameterizedTypeReference<List<HaopuDTO.ReadingItem>>() {});
    }

    private <T> List<T> get(String path, ParameterizedTypeReference<List<T>> type) {
        String baseUrl = vendorApiProperties.getHaopu().getBaseUrl();
        if (!StringUtils.hasText(baseUrl)
                || baseUrl.contains("your-haopu-api-host.com")) {
            log.debug("昊普接口未配置 baseUrl，跳过档案/点抄请求");
            return List.of();
        }
        String url = baseUrl.replaceAll("/+$", "") + path;
        try {
            ResponseEntity<List<T>> response = restTemplate.exchange(url, org.springframework.http.HttpMethod.GET,
                    null, type);
            return response.getBody() == null ? List.of() : response.getBody();
        } catch (RestClientException e) {
            log.warn("昊普接口请求失败 url={} reason={}", url, e.getMessage());
            return List.of();
        }
    }
}
