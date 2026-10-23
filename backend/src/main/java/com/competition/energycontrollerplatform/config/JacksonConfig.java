package com.competition.energycontrollerplatform.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Jackson 序列化/反序列化配置。
 * <p>
 * 显式注册 {@link JavaTimeModule} 并指定 {@link LocalDateTime} 的格式 "yyyy-MM-dd HH:mm:ss"，
 * 确保所有 DTO 中的 {@code LocalDateTime} 字段正常序列化与反序列化。
 */
@Configuration
public class JacksonConfig {

    private static final DateTimeFormatter LOCAL_DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 将 JavaTimeModule 暴露为 Spring Bean。
     * Spring Boot 的 Jackson 自动配置会检测所有 {@link Module} Bean 并注册到 ObjectMapper。
     */
    @Bean
    public Module javaTimeModule() {
        JavaTimeModule module = new JavaTimeModule();
        module.addSerializer(LocalDateTime.class,
                new LocalDateTimeSerializer(LOCAL_DATE_TIME_FORMATTER));
        module.addDeserializer(LocalDateTime.class,
                new LocalDateTimeDeserializer(LOCAL_DATE_TIME_FORMATTER));
        return module;
    }

    @Bean
    public ObjectMapper objectMapper(Module javaTimeModule) {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(javaTimeModule);
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        return mapper;
    }
}
