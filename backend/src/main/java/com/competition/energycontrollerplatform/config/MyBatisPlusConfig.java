package com.competition.energycontrollerplatform.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@MapperScan({"com.competition.energycontrollerplatform.mapper", "com.competition.energycontrollerplatform.miniapp.mapper"})
public class MyBatisPlusConfig {

    /**
     * MyBatis-Plus 分页插件。
     * <p>
     * 从 3.5.9 开始，PaginationInnerInterceptor 拆分到 mybatis-plus-jsqlparser 模块，
     * 需在 pom.xml 中显式引入该依赖。MybatisPlusInterceptor Bean 会被 Spring Boot
     * 的 MyBatis-Plus 自动配置自动发现并注入到 SqlSessionFactory，无需手动创建
     * SqlSessionFactory / SqlSessionTemplate。
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        return interceptor;
    }
}
