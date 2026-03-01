package com.example.quant.auth;

import com.example.quant.config.AppProperties;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 简单鉴权服务（账号密码由配置提供）。
 */
@Service
public class AuthService {

    private final Set<String> tokenStore = ConcurrentHashMap.newKeySet();
    private final AppProperties properties;

    public AuthService(AppProperties properties) {
        this.properties = properties;
    }

    public String login(String username, String password) {
        if (properties.getAuth().getUsername().equals(username)
                && properties.getAuth().getPassword().equals(password)) {
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
