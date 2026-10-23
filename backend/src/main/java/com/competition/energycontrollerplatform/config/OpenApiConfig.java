package com.competition.energycontrollerplatform.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Knife4j / SpringDoc 接口文档配置
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI energyPlatformOpenAPI() {
        final String securitySchemeName = "BearerAuth";
        return new OpenAPI()
                .info(new Info()
                        .title("水电能耗运营管理平台 - API说明书")
                        .description("智能水电能耗管理平台后端接口文档，包含表计管理、商户管理、充值缴费、报表统计等模块。")
                        .version("v1.0")
                        .contact(new Contact()
                            .name("能耗管理平台开发组")))
                // 使用相对路径，Swagger 会基于当前页面地址发起请求
                .servers(List.of(new Server().url("")))
                // 添加 JWT 认证方案（使用 APIKEY 类型以兼容 Knife4j 4.5.0 在 Spring Boot 4.x 上的已知问题）
                .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
                .components(new Components()
                        .addSecuritySchemes(securitySchemeName,
                                new SecurityScheme()
                                        .name("Authorization")
                                        .type(SecurityScheme.Type.APIKEY)
                                        .in(SecurityScheme.In.HEADER)
                                        .description("输入登录返回的 Token，格式：Bearer <token>（注意 Bearer 后有一个空格）")));
    }
}
