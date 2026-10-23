package com.competition.energycontrollerplatform.config;

import com.competition.energycontrollerplatform.common.IdempotentInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

/**
 * Web MVC 额外配置。
 * <p>
 * 消除浏览器自动请求 /favicon.ico 导致的 NoResourceFoundException 噪音日志，
 * 为本地文件存储提供静态资源映射，并注册幂等性拦截器。
 */
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final StorageProperties storageProperties;
    private final IdempotentInterceptor idempotentInterceptor;

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        // 浏览器自动请求标签页图标时返回 204 No Content，不产生异常
        registry.addViewController("/favicon.ico").setStatusCode(HttpStatus.NO_CONTENT);
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 本地文件存储的静态资源映射
        // 将 /files/** 请求映射到 storage.local.rootPath 目录
        // 无论当前使用哪种存储后端，此映射都会注册（云存储模式下此路径不会被访问，无害）
        String urlPrefix = storageProperties.getLocal().getUrlPrefix();
        // 去掉开头的 /（如果有）作为 URL 模式
        String urlPattern = urlPrefix.startsWith("/") ? urlPrefix : "/" + urlPrefix;
        urlPattern = urlPattern + "/**";

        String rootPath = storageProperties.getLocal().getRootPath();
        String location = Paths.get(rootPath).toAbsolutePath().normalize().toUri().toString();
        // 确保以 / 结尾
        if (!location.endsWith("/")) {
            location = location + "/";
        }

        registry.addResourceHandler(urlPattern)
                .addResourceLocations(location);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // ⚠️ 幂等性拦截器必须排在 HandlerInterceptor 链末端（order=100）。
        // Servlet Filter 层 JWT 鉴权天然先于 HandlerInterceptor 执行，
        // 未认证请求会被 401 拦截，不会到此消耗 Redis 资源。
        registry.addInterceptor(idempotentInterceptor)
                .addPathPatterns("/api/**")
                .order(100);
    }
}
