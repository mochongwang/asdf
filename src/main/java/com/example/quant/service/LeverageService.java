package com.example.quant.service;

import com.example.quant.model.LeverageInfo;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 杠杆服务（演示：启动预置，可扩展成定时REST拉取）。
 */
@Service
public class LeverageService {

    private final Map<String, LeverageInfo> leverageStore = new ConcurrentHashMap<>();

    public LeverageService() {
        put("BTCUSDT", 10, 10);
        put("ETHUSDT", 10, 10);
        put("BNBUSDT", 8, 8);
    }

    public void put(String symbol, int cross, int isolated) {
        LeverageInfo info = new LeverageInfo();
        info.symbol = symbol;
        info.crossLeverage = cross;
        info.isolatedLeverage = isolated;
        info.createdAt = Instant.now();
        leverageStore.put(symbol, info);
    }

    public List<LeverageInfo> list() {
        return leverageStore.values().stream().toList();
    }
}
