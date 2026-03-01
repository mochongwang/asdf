package com.example.quant.config;

import com.example.quant.auth.AuthService;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 应用配置入口。
 */
@Configuration
@EnableScheduling
@EnableConfigurationProperties(AppProperties.class)
public class AppConfig implements WebMvcConfigurer {

    private final AuthService authService;

    public AppConfig(AuthService authService) {
        this.authService = authService;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new AuthTokenInterceptor(authService))
                .addPathPatterns("/api/**")
                .excludePathPatterns("/api/auth/login", "/api/options");
    }
}
