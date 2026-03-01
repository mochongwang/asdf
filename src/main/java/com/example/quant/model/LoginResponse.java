package com.example.quant.model;

/**
 * 登录响应。
 */
public record LoginResponse(
        boolean success,
        String token,
        String message
) {
}
