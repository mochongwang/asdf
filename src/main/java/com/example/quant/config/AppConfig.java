package com.example.quant.config;

import com.example.quant.auth.AuthService;
import org.springframework.context.annotation.Bean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.sql.DataSource;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

/**
 * 应用配置入口。
 */
@Configuration
@EnableScheduling
@EnableConfigurationProperties(AppProperties.class)
public class AppConfig implements WebMvcConfigurer {

    private final AuthService authService;
    private final AppProperties properties;

    public AppConfig(AuthService authService, AppProperties properties) {
        this.authService = authService;
        this.properties = properties;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new AuthTokenInterceptor(authService))
                .addPathPatterns("/api/**")
                .excludePathPatterns("/api/auth/login", "/api/options");
    }

    @Bean
    public DataSource dataSource() {
        DriverManagerDataSource ds = new DriverManagerDataSource();
        ds.setDriverClassName("org.duckdb.DuckDBDriver");
        ds.setUrl("jdbc:duckdb:" + properties.getData().getDuckdbPath());
        return ds;
    }
}
