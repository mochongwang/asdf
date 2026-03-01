package com.example.quant.data;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

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
}
