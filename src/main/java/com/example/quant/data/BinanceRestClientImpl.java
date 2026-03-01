package com.example.quant.data;

import com.example.quant.model.KlineCandle;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * 币安客户端实现。
 *
 * <p>取数据只使用 WebSocket API（ws-api）。</p>
 */
@Component
public class BinanceRestClientImpl implements BinanceRestClient {

    private final ApiConcurrencyGuard guard;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newHttpClient();

    public BinanceRestClientImpl(ApiConcurrencyGuard guard) {
        this.guard = guard;
    }

    @Override
    public Map<String, Object> tickerPrice(String symbol) {
        return guard.execute(() -> {
            try {
                Map<String, Object> resp = callWsApi("ticker.price", Map.of("symbol", symbol));
                Object result = resp.get("result");
                if (!(result instanceof Map<?, ?> map)) {
                    throw new IllegalStateException("ws-api ticker.price 返回格式异常");
                }
                return (Map<String, Object>) map;
            } catch (Exception e) {
                throw new IllegalStateException("WebSocket API 获取ticker失败", e);
            }
        });
    }

    @Override
    public List<KlineCandle> klines(String symbol, String interval, int limit) {
        return klinesByWsApi(symbol, interval, limit);
    }

    @Override
    public List<KlineCandle> klinesByWsApi(String symbol, String interval, int limit) {
        return guard.execute(() -> {
            try {
                Map<String, Object> resp = callWsApi("klines", Map.of(
                        "symbol", symbol,
                        "interval", interval,
                        "limit", Math.max(1, Math.min(limit, 1000))
                ));
                Object resultObj = resp.get("result");
                if (!(resultObj instanceof List<?> list)) {
                    return List.of();
                }

                List<List<Object>> rows = new ArrayList<>();
                for (Object rowObj : list) {
                    if (rowObj instanceof List<?> raw) {
                        rows.add(new ArrayList<>(raw));
                    }
                }
                return rowsToCandles(rows);
            } catch (Exception e) {
                throw new IllegalStateException("WebSocket API 获取K线失败", e);
            }
        });
    }


    @Override
    public Map<String, Object> depth(String symbol, int limit) {
        return guard.execute(() -> {
            try {
                Map<String, Object> resp = callWsApi("depth", Map.of(
                        "symbol", symbol,
                        "limit", normalizeDepthLimit(limit)
                ));
                Object result = resp.get("result");
                if (!(result instanceof Map<?, ?> map)) {
                    throw new IllegalStateException("ws-api depth 返回格式异常");
                }
                return (Map<String, Object>) map;
            } catch (Exception e) {
                throw new IllegalStateException("WebSocket API 获取orderbook失败", e);
            }
        });
    }

    private int normalizeDepthLimit(int limit) {
        int normalized = Math.max(5, limit);
        int[] allowed = {5, 10, 20, 50, 100, 500, 1000, 5000};
        for (int candidate : allowed) {
            if (normalized <= candidate) {
                return candidate;
            }
        }
        return 5000;
    }


    @Override
    public Map<String, Object> wsApiCall(String method, Map<String, Object> params) {
        return guard.execute(() -> {
            try {
                return callWsApi(method, params == null ? Map.of() : params);
            } catch (Exception e) {
                throw new IllegalStateException("WebSocket API 通用请求失败: " + method, e);
            }
        });
    }

    private Map<String, Object> callWsApi(String method, Map<String, Object> params) throws Exception {
        String reqId = UUID.randomUUID().toString();
        String payload = objectMapper.writeValueAsString(Map.of(
                "id", reqId,
                "method", method,
                "params", params
        ));

        CompletableFuture<String> responseFuture = new CompletableFuture<>();
        WebSocket ws = httpClient.newWebSocketBuilder()
                .buildAsync(URI.create("wss://ws-api.binance.com:443/ws-api/v3"), new WebSocket.Listener() {
                    @Override
                    public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
                        responseFuture.complete(data.toString());
                        return WebSocket.Listener.super.onText(webSocket, data, last);
                    }

                    @Override
                    public void onError(WebSocket webSocket, Throwable error) {
                        responseFuture.completeExceptionally(error);
                    }
                }).join();

        ws.sendText(payload, true).join();
        String text = responseFuture.join();
        ws.sendClose(WebSocket.NORMAL_CLOSURE, "done").join();

        return objectMapper.readValue(text, new TypeReference<>() {});
    }

    private List<KlineCandle> rowsToCandles(List<List<Object>> rows) {
        List<KlineCandle> result = new ArrayList<>();
        if (rows == null) {
            return result;
        }
        for (List<Object> row : rows) {
            if (row == null || row.size() < 7) {
                continue;
            }
            result.add(new KlineCandle(
                    Long.parseLong(String.valueOf(row.get(0))),
                    Double.parseDouble(String.valueOf(row.get(1))),
                    Double.parseDouble(String.valueOf(row.get(2))),
                    Double.parseDouble(String.valueOf(row.get(3))),
                    Double.parseDouble(String.valueOf(row.get(4))),
                    Double.parseDouble(String.valueOf(row.get(5))),
                    Long.parseLong(String.valueOf(row.get(6)))
            ));
        }
        return result;
    }
}
