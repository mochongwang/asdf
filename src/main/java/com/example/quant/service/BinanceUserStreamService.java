package com.example.quant.service;

import com.example.quant.config.AppProperties;
import com.example.quant.order.OrderService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.WebSocket;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.CompletionStage;

/**
 * 币安用户数据流（listenKey + websocket 事件推送）。
 */
@Service
public class BinanceUserStreamService {

    private static final Logger log = LoggerFactory.getLogger(BinanceUserStreamService.class);

    private final AppProperties properties;
    private final AccountService accountService;
    private final OrderService orderService;
    private final NotificationService notificationService;
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private volatile String listenKey;
    private volatile WebSocket webSocket;

    public BinanceUserStreamService(AppProperties properties,
                                    AccountService accountService,
                                    OrderService orderService,
                                    NotificationService notificationService) {
        this.properties = properties;
        this.accountService = accountService;
        this.orderService = orderService;
        this.notificationService = notificationService;
    }

    @Scheduled(fixedDelay = 10000)
    public void ensureUserStreamConnected() {
        if (properties.getTrading().getApiKey() == null || properties.getTrading().getApiKey().isBlank()) {
            return;
        }
        try {
            if (listenKey == null || listenKey.isBlank()) {
                listenKey = createListenKey();
            }
            if (webSocket == null || webSocket.isInputClosed() || webSocket.isOutputClosed()) {
                connectWebSocket(listenKey);
            }
        } catch (Exception e) {
            notificationService.notify("BINANCE", "用户流连接失败", e.getMessage());
            log.warn("user stream connect failed", e);
        }
    }

    @Scheduled(fixedDelay = 30 * 60 * 1000)
    public void keepAliveListenKey() {
        if (listenKey == null || listenKey.isBlank()) {
            return;
        }
        try {
            String url = "https://api.binance.com/api/v3/userDataStream";
            String body = "listenKey=" + listenKey;
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("X-MBX-APIKEY", properties.getTrading().getApiKey())
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .method("PUT", HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                    .build();
            httpClient.send(req, HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            log.warn("listenKey keepalive failed", e);
        }
    }

    private String createListenKey() throws Exception {
        String url = "https://api.binance.com/api/v3/userDataStream";
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("X-MBX-APIKEY", properties.getTrading().getApiKey())
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();
        HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
        Map<String, Object> map = objectMapper.readValue(resp.body(), new TypeReference<>() {});
        Object lk = map.get("listenKey");
        if (lk == null) {
            throw new IllegalStateException("创建listenKey失败: " + resp.body());
        }
        return String.valueOf(lk);
    }

    private void connectWebSocket(String lk) {
        String wsUrl = "wss://stream.binance.com:9443/ws/" + lk;
        webSocket = httpClient.newWebSocketBuilder()
                .buildAsync(URI.create(wsUrl), new UserDataListener())
                .join();
        log.info("binance user stream connected");
    }

    private final class UserDataListener implements WebSocket.Listener {
        @Override
        public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
            try {
                Map<String, Object> payload = objectMapper.readValue(data.toString(), new TypeReference<>() {});
                handleEvent(payload);
            } catch (Exception e) {
                log.warn("parse user stream message failed: {}", e.getMessage());
            }
            return WebSocket.Listener.super.onText(webSocket, data, last);
        }

        @Override
        public void onError(WebSocket webSocket, Throwable error) {
            log.warn("user stream websocket error: {}", error.getMessage());
            BinanceUserStreamService.this.webSocket = null;
        }
    }

    @SuppressWarnings("unchecked")
    private void handleEvent(Map<String, Object> payload) {
        String eventType = String.valueOf(payload.getOrDefault("e", ""));
        if ("outboundAccountPosition".equals(eventType)) {
            Object balances = payload.get("B");
            if (balances instanceof Iterable<?> it) {
                double free = 0;
                double locked = 0;
                for (Object o : it) {
                    if (!(o instanceof Map<?, ?> m)) {
                        continue;
                    }
                    if (!"USDT".equals(String.valueOf(m.getOrDefault("a", "")))) {
                        continue;
                    }
                    free = parseDouble(m.get("f"));
                    locked = parseDouble(m.get("l"));
                    break;
                }
                accountService.upsertSnapshot(
                        properties.getTrading().getApiKey(),
                        free + locked,
                        free,
                        locked,
                        Instant.now().toEpochMilli()
                );
            }
        } else if ("executionReport".equals(eventType)) {
            String symbol = String.valueOf(payload.getOrDefault("s", ""));
            String orderId = String.valueOf(payload.getOrDefault("i", ""));
            String status = String.valueOf(payload.getOrDefault("X", "UNKNOWN"));
            String side = String.valueOf(payload.getOrDefault("S", "BUY"));
            double executedQty = parseDouble(payload.get("z"));
            double price = parseDouble(payload.get("L"));
            orderService.syncExecutionReport(symbol, orderId, status, side, executedQty, price);
        }
    }

    private double parseDouble(Object raw) {
        if (raw == null) {
            return 0d;
        }
        try {
            return Double.parseDouble(String.valueOf(raw));
        } catch (Exception e) {
            return 0d;
        }
    }
}
