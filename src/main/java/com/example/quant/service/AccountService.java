package com.example.quant.service;

import com.example.quant.model.AccountInfo;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 账户服务。
 */
@Service
public class AccountService {

    private final Map<String, AccountInfo> accountStore = new ConcurrentHashMap<>();

    public AccountService() {
        AccountInfo info = new AccountInfo();
        info.apikey = "demo-api-key";
        info.balance = 10000;
        info.availableBalance = 9000;
        info.frozenBalance = 1000;
        info.updatedAt = Instant.now();
        accountStore.put(info.apikey, info);
    }

    public List<AccountInfo> list() {
        return accountStore.values().stream().toList();
    }
}
