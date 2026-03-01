package com.example.quant.model;

import jakarta.validation.constraints.NotBlank;

/**
 * 登录请求。
 */
public record LoginRequest(
        @NotBlank String username,
        @NotBlank String password
) {
}
