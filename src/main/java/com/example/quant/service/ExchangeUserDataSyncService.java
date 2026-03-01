package com.example.quant.service;

import com.example.quant.data.BinanceRestClient;
import com.example.quant.order.OrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * 交易所用户数据同步服务（ws-api 请求式轮询）。
 */
@Service
public class ExchangeUserDataSyncService {

    private static final Logger log = LoggerFactory.getLogger(ExchangeUserDataSyncService.class);

    private final BinanceRestClient binanceRestClient;
    private final AccountService accountService;
    private final OrderService orderService;
    private final NotificationService notificationService;

    public ExchangeUserDataSyncService(BinanceRestClient binanceRestClient,
                                       AccountService accountService,
                                       OrderService orderService,
                                       NotificationService notificationService) {
        this.binanceRestClient = binanceRestClient;
        this.accountService = accountService;
        this.orderService = orderService;
        this.notificationService = notificationService;
    }

    @Scheduled(fixedDelay = 5000)
    public void syncAccountAndOrders() {
        try {
            syncAccount();
            syncOrders();
        } catch (Exception e) {
            String msg = "用户数据同步失败: " + e.getMessage();
            notificationService.notify("BINANCE", "用户数据同步失败", msg);
            log.warn(msg, e);
        }
    }

    @SuppressWarnings("unchecked")
    private void syncAccount() {
        Map<String, Object> resp = binanceRestClient.wsApiCall("account.status", Map.of());
        Object result = resp.get("result");
        if (!(result instanceof Map<?, ?> resultMap)) {
            return;
        }
        String apiKey = String.valueOf(resultMap.getOrDefault("accountAlias", "sync-api-key"));
        double balance = parseDouble(resultMap.get("totalWalletBalance"));
        double available = parseDouble(resultMap.get("availableBalance"));
        double frozen = Math.max(0, balance - available);
        accountService.upsertSnapshot(apiKey, balance, available, frozen, Instant.now().toEpochMilli());
    }

    @SuppressWarnings("unchecked")
    private void syncOrders() {
        Map<String, Object> resp = binanceRestClient.wsApiCall("openOrders.status", Map.of());
        Object result = resp.get("result");
        if (!(result instanceof List<?> orders)) {
            return;
        }
        for (Object o : orders) {
            if (!(o instanceof Map<?, ?> m)) {
                continue;
            }
            String exchangeOrderId = String.valueOf(m.getOrDefault("orderId", ""));
            String symbol = String.valueOf(m.getOrDefault("symbol", ""));
            String status = String.valueOf(m.getOrDefault("status", "UNKNOWN"));
            orderService.syncExchangeOrder(exchangeOrderId, symbol, status);
        }
    }

    private double parseDouble(Object raw) {
        if (raw == null) {
            return 0d;
        }
        try {
            return Double.parseDouble(String.valueOf(raw));
        } catch (Exception ignored) {
            return 0d;
        }
    }
}
