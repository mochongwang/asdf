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
    private static final String USER_STREAM_URL = "https://api.binance.com/api/v3/userDataStream";
    private static final long BASE_BACKOFF_MS = 5_000;
    private static final long MAX_BACKOFF_MS = 120_000;

    private final AppProperties properties;
    private final AccountService accountService;
    private final OrderService orderService;
    private final NotificationService notificationService;
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private volatile String listenKey;
    private volatile WebSocket webSocket;
    private volatile long reconnectAfterEpochMs;
    private volatile int reconnectAttempts;

    public BinanceUserStreamService(AppProperties properties,
                                    AccountService accountService,
                                    OrderService orderService,
                                    NotificationService notificationService) {
        this.properties = properties;
        this.accountService = accountService;
        this.orderService = orderService;
        this.notificationService = notificationService;
    }

    @Scheduled(fixedDelay = 10_000)
    public void ensureUserStreamConnected() {
        if (properties.getTrading().getApiKey() == null || properties.getTrading().getApiKey().isBlank()) {
            return;
        }
        if (System.currentTimeMillis() < reconnectAfterEpochMs) {
            return;
        }
        try {
            if (listenKey == null || listenKey.isBlank()) {
                listenKey = createListenKey();
                reconnectAttempts = 0;
            }
            if (webSocket == null || webSocket.isInputClosed() || webSocket.isOutputClosed()) {
                connectWebSocket(listenKey);
                reconnectAttempts = 0;
            }
        } catch (Exception e) {
            scheduleReconnect("用户流连接失败", e);
        }
    }

    @Scheduled(fixedDelay = 30 * 60 * 1000)
    public void keepAliveListenKey() {
        if (listenKey == null || listenKey.isBlank()) {
            return;
        }
        try {
            String body = "listenKey=" + listenKey;
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(USER_STREAM_URL))
                    .header("X-MBX-APIKEY", properties.getTrading().getApiKey())
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .method("PUT", HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                    .build();
            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() / 100 != 2) {
                log.warn("listenKey keepalive failed status={}, body={}", resp.statusCode(), resp.body());
                rotateListenKey();
            }
        } catch (Exception e) {
            log.warn("listenKey keepalive failed", e);
            rotateListenKey();
        }
    }

    private String createListenKey() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(USER_STREAM_URL))
                .header("X-MBX-APIKEY", properties.getTrading().getApiKey())
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();
        HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() / 100 != 2) {
            throw new IllegalStateException("创建listenKey失败, status=" + resp.statusCode() + ", body=" + resp.body());
        }
        Map<String, Object> map = objectMapper.readValue(resp.body(), new TypeReference<>() {
        });
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

    private void rotateListenKey() {
        closeSocketQuietly();
        listenKey = null;
        reconnectAttempts = 0;
    }

    private void closeSocketQuietly() {
        WebSocket ws = this.webSocket;
        this.webSocket = null;
        if (ws != null) {
            try {
                ws.sendClose(WebSocket.NORMAL_CLOSURE, "reconnect").join();
            } catch (Exception ignore) {
                // ignore close errors
            }
        }
    }

    private void scheduleReconnect(String title, Exception e) {
        reconnectAttempts++;
        long delay = Math.min(MAX_BACKOFF_MS, BASE_BACKOFF_MS * (1L << Math.min(reconnectAttempts, 4)));
        reconnectAfterEpochMs = System.currentTimeMillis() + delay;
        closeSocketQuietly();
        listenKey = null;
        notificationService.notify("BINANCE", title, e.getMessage() + ", backoffMs=" + delay);
        log.warn("user stream connect failed, backoffMs={}", delay, e);
    }

    private final class UserDataListener implements WebSocket.Listener {
        @Override
        public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
            try {
                Map<String, Object> payload = objectMapper.readValue(data.toString(), new TypeReference<>() {
                });
                handleEvent(payload);
            } catch (Exception e) {
                log.warn("parse user stream message failed: {}", e.getMessage());
            }
            return WebSocket.Listener.super.onText(webSocket, data, last);
        }

        @Override
        public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
            log.warn("user stream websocket closed status={}, reason={}", statusCode, reason);
            BinanceUserStreamService.this.webSocket = null;
            reconnectAfterEpochMs = System.currentTimeMillis() + BASE_BACKOFF_MS;
            return WebSocket.Listener.super.onClose(webSocket, statusCode, reason);
        }

        @Override
        public void onError(WebSocket webSocket, Throwable error) {
            log.warn("user stream websocket error: {}", error.getMessage());
            BinanceUserStreamService.this.webSocket = null;
            reconnectAfterEpochMs = System.currentTimeMillis() + BASE_BACKOFF_MS;
        }
    }

    @SuppressWarnings("unchecked")
    private void handleEvent(Map<String, Object> payload) {
        String eventType = String.valueOf(payload.getOrDefault("e", ""));
        if ("listenKeyExpired".equals(eventType)) {
            log.warn("listenKey expired, rotate and reconnect");
            rotateListenKey();
            return;
        }

        if ("outboundAccountPosition".equals(eventType) || "balanceUpdate".equals(eventType)) {
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
            if (price <= 0) {
                price = parseDouble(payload.get("p"));
            }
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
