package com.example.quant.data;

import com.example.quant.model.KlineCandle;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 币安 REST 客户端实现。
 *
 * <p>所有请求都通过 {@link ApiConcurrencyGuard} 做并发和频率控制。</p>
 */
@Component
public class BinanceRestClientImpl implements BinanceRestClient {

    private final RestClient restClient;
    private final ApiConcurrencyGuard guard;

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
