package com.example.quant.auth;

import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 简单鉴权服务（账号密码写死）。
 */
@Service
public class AuthService {

    private static final String USERNAME = "admin";
    private static final String PASSWORD = "123456";

    private final Set<String> tokenStore = ConcurrentHashMap.newKeySet();

    public String login(String username, String password) {
        if (USERNAME.equals(username) && PASSWORD.equals(password)) {
            String token = UUID.randomUUID().toString();
            tokenStore.add(token);
            return token;
        }
        return null;
    }

    public boolean validate(String token) {
        return token != null && tokenStore.contains(token);
    }
}
