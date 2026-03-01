package com.example.quant.data;

import com.example.quant.model.KlineCandle;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

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
 */
@Component
public class BinanceRestClientImpl implements BinanceRestClient {

    private final RestClient restClient;
    private final ApiConcurrencyGuard guard;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newHttpClient();

    public BinanceRestClientImpl(ApiConcurrencyGuard guard) {
        this.guard = guard;
        this.restClient = RestClient.builder()
                .baseUrl("https://api.binance.com")
                .build();
    }

    @Override
    public Map<String, Object> tickerPrice(String symbol) {
        return guard.execute(() -> restClient.get()
                .uri(uriBuilder -> uriBuilder.path("/api/v3/ticker/price")
                        .queryParam("symbol", symbol)
                        .build())
                .retrieve()
                .body(Map.class));
    }

    @Override
    public List<KlineCandle> klines(String symbol, String interval, int limit) {
        List<List<Object>> rows = guard.execute(() -> restClient.get()
                .uri(uriBuilder -> uriBuilder.path("/api/v3/klines")
                        .queryParam("symbol", symbol)
                        .queryParam("interval", interval)
                        .queryParam("limit", Math.max(1, Math.min(limit, 1000)))
                        .build())
                .retrieve()
                .body(List.class));
        return rowsToCandles(rows);
    }

    @Override
    public List<KlineCandle> klinesByWsApi(String symbol, String interval, int limit) {
        return guard.execute(() -> {
            try {
                String reqId = UUID.randomUUID().toString();
                String payload = objectMapper.writeValueAsString(Map.of(
                        "id", reqId,
                        "method", "klines",
                        "params", Map.of(
                                "symbol", symbol,
                                "interval", interval,
                                "limit", Math.max(1, Math.min(limit, 1000))
                        )
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

                Map<String, Object> resp = objectMapper.readValue(text, new TypeReference<>() {});
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
