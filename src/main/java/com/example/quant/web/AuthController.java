package com.example.quant.web;

import com.example.quant.auth.AuthService;
import com.example.quant.model.LoginRequest;
import com.example.quant.model.LoginResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 登录控制器。
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest req) {
        String token = authService.login(req.username(), req.password());
        if (token == null) {
            return new LoginResponse(false, null, "用户名或密码错误");
        }
        return new LoginResponse(true, token, "登录成功");
    }
}
