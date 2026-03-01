package com.example.quant.config;

import com.example.quant.auth.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * API Token 拦截器。
 */
public class AuthTokenInterceptor implements HandlerInterceptor {

    private final AuthService authService;

    public AuthTokenInterceptor(AuthService authService) {
        this.authService = authService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 允许 CORS 预检请求直接通过。
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String token = request.getHeader("X-Auth-Token");
        if (authService.validate(token)) {
            return true;
        }
        response.setStatus(401);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"success\":false,\"message\":\"未登录或Token无效\"}");
        return false;
    }
}
