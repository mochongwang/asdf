package com.example.quant.data;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;

/**
 * OrderBook 订阅管理（基于 depth 快照 + websocket 增量更新）。
 */
@Service
public class OrderBookSubscriptionService {

    private static final Logger log = LoggerFactory.getLogger(OrderBookSubscriptionService.class);

    private final MarketDataService marketDataService;
    private final Set<String> subscribedSymbols = ConcurrentHashMap.newKeySet();
    private final Map<String, OrderBookState> states = new ConcurrentHashMap<>();
    private final Map<String, WebSocket> sockets = new ConcurrentHashMap<>();
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public OrderBookSubscriptionService(MarketDataService marketDataService) {
        this.marketDataService = marketDataService;
    }

    public void subscribe(String symbol) {
        if (symbol == null || symbol.isBlank()) {
            return;
        }
        String normalized = symbol.toUpperCase();
        subscribedSymbols.add(normalized);
        if (!states.containsKey(normalized)) {
            initFromSnapshot(normalized);
        }
        ensureSocket(normalized);
    }

    public void unsubscribe(String symbol) {
        if (symbol == null || symbol.isBlank()) {
            return;
        }
        String normalized = symbol.toUpperCase();
        subscribedSymbols.remove(normalized);
        states.remove(normalized);
        WebSocket ws = sockets.remove(normalized);
        if (ws != null) {
            ws.sendClose(WebSocket.NORMAL_CLOSURE, "unsubscribe");
        }
    }

    public Map<String, Object> topLevels(String symbol, int levels) {
        String normalized = symbol.toUpperCase();
        OrderBookState state = states.get(normalized);
        if (state == null) {
            subscribe(normalized);
            state = states.get(normalized);
        }
        if (state == null) {
            return marketDataService.topLevelsOrderBook(normalized, levels);
        }
        return state.toTopLevels(normalized, levels);
    }

    /**
     * 定时确保订阅符号都存在活跃连接（断线自动重建）。
     */
    @Scheduled(fixedDelay = 3000)
    public void ensureConnections() {
        for (String symbol : subscribedSymbols) {
            ensureSocket(symbol);
        }
    }

    @SuppressWarnings("unchecked")
    private void initFromSnapshot(String symbol) {
        Map<String, Object> snapshot = marketDataService.latestOrderBook(symbol, 50);
        OrderBookState state = new OrderBookState();
        state.lastUpdateId = parseLong(snapshot.get("lastUpdateId"));

        Object bidsObj = snapshot.get("bids");
        if (bidsObj instanceof List<?> bids) {
            for (Object rowObj : bids) {
                if (rowObj instanceof List<?> row && row.size() >= 2) {
                    double price = parseDouble(row.get(0));
                    double qty = parseDouble(row.get(1));
                    if (qty > 0) {
                        state.bids.put(price, qty);
                    }
                }
            }
        }

        Object asksObj = snapshot.get("asks");
        if (asksObj instanceof List<?> asks) {
            for (Object rowObj : asks) {
                if (rowObj instanceof List<?> row && row.size() >= 2) {
                    double price = parseDouble(row.get(0));
                    double qty = parseDouble(row.get(1));
                    if (qty > 0) {
                        state.asks.put(price, qty);
                    }
                }
            }
        }
        states.put(symbol, state);
    }

    private void ensureSocket(String symbol) {
        WebSocket ws = sockets.get(symbol);
        if (ws != null && !ws.isInputClosed() && !ws.isOutputClosed()) {
            return;
        }
        sockets.remove(symbol);

        String stream = symbol.toLowerCase() + "@depth@100ms";
        URI uri = URI.create("wss://stream.binance.com:9443/ws/" + stream);
        WebSocket socket = httpClient.newWebSocketBuilder().buildAsync(uri, new DepthListener(symbol)).join();
        sockets.put(symbol, socket);
        log.info("orderbook stream connected: {}", symbol);
    }

    private final class DepthListener implements WebSocket.Listener {
        private final String symbol;

        private DepthListener(String symbol) {
            this.symbol = symbol;
        }

        @Override
        public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
            try {
                Map<String, Object> payload = objectMapper.readValue(data.toString(), new TypeReference<>() {});
                applyDepthEvent(symbol, payload);
            } catch (Exception e) {
                log.warn("parse depth event failed for {}: {}", symbol, e.getMessage());
            }
            return WebSocket.Listener.super.onText(webSocket, data, last);
        }

        @Override
        public void onError(WebSocket webSocket, Throwable error) {
            log.warn("orderbook stream error for {}: {}", symbol, error.getMessage());
            sockets.remove(symbol);
        }
    }

    @SuppressWarnings("unchecked")
    private void applyDepthEvent(String symbol, Map<String, Object> payload) {
        OrderBookState state = states.get(symbol);
        if (state == null) {
            return;
        }

        long firstUpdateId = parseLong(payload.get("U"));
        long finalUpdateId = parseLong(payload.get("u"));

        if (finalUpdateId <= state.lastUpdateId) {
            return;
        }

        // Binance depth 一致性校验：必须满足 U <= lastUpdateId+1 <= u
        if (!(firstUpdateId <= state.lastUpdateId + 1 && finalUpdateId >= state.lastUpdateId + 1)) {
            log.warn("orderbook gap detected for {}, resync snapshot: local={}, eventU={}, eventu={}",
                    symbol, state.lastUpdateId, firstUpdateId, finalUpdateId);
            initFromSnapshot(symbol);
            state = states.get(symbol);
            if (state == null) {
                return;
            }
            if (!(firstUpdateId <= state.lastUpdateId + 1 && finalUpdateId >= state.lastUpdateId + 1)) {
                // 若重载后该事件仍不连续，跳过等待下个事件
                return;
            }
        }

        Object bidsObj = payload.get("b");
        if (bidsObj instanceof List<?> bids) {
            applyLevels(state.bids, bids);
        }

        Object asksObj = payload.get("a");
        if (asksObj instanceof List<?> asks) {
            applyLevels(state.asks, asks);
        }

        state.lastUpdateId = finalUpdateId;
    }

    private void applyLevels(NavigableMap<Double, Double> sideMap, List<?> updates) {
        for (Object rowObj : updates) {
            if (!(rowObj instanceof List<?> row) || row.size() < 2) {
                continue;
            }
            double price = parseDouble(row.get(0));
            double qty = parseDouble(row.get(1));
            if (qty <= 0) {
                sideMap.remove(price);
            } else {
                sideMap.put(price, qty);
            }
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

    private long parseLong(Object raw) {
        if (raw == null) {
            return 0L;
        }
        try {
            return Long.parseLong(String.valueOf(raw));
        } catch (Exception e) {
            return 0L;
        }
    }

    private static final class OrderBookState {
        private long lastUpdateId;
        private final NavigableMap<Double, Double> bids = new TreeMap<>((a, b) -> Double.compare(b, a));
        private final NavigableMap<Double, Double> asks = new TreeMap<>();

        private Map<String, Object> toTopLevels(String symbol, int levels) {
            int size = Math.max(1, levels);
            List<List<Object>> bidRows = new ArrayList<>();
            List<List<Object>> askRows = new ArrayList<>();

            int i = 0;
            for (Map.Entry<Double, Double> e : bids.entrySet()) {
                if (i++ >= size) {
                    break;
                }
                bidRows.add(List.of(e.getKey(), e.getValue()));
            }
            i = 0;
            for (Map.Entry<Double, Double> e : asks.entrySet()) {
                if (i++ >= size) {
                    break;
                }
                askRows.add(List.of(e.getKey(), e.getValue()));
            }

            return Map.of(
                    "symbol", symbol,
                    "lastUpdateId", lastUpdateId,
                    "bids", bidRows,
                    "asks", askRows
            );
        }
    }
}
