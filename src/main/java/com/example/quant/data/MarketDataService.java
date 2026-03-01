package com.example.quant.data;

import com.example.quant.model.KlineCandle;
import com.example.quant.model.StrategyIndicator;
import com.example.quant.service.NotificationService;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * 取数据层服务（只用 WebSocket API）。
 */
@Service
public class MarketDataService {

    private final Cache<String, Map<String, Object>> tickerCache = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofSeconds(10))
            .maximumSize(1_000)
            .build();

    private final Cache<String, List<KlineCandle>> klineCache = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofSeconds(20))
            .maximumSize(2_000)
            .build();

    private final Cache<String, Double> indicatorCache = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofSeconds(20))
            .maximumSize(5_000)
            .build();

    private final Cache<String, Map<String, Object>> orderBookCache = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofSeconds(5))
            .maximumSize(2_000)
            .build();

    private final BinanceRestClient binanceRestClient;
    private final IndicatorCalculator indicatorCalculator;
    private final NotificationService notificationService;

    public MarketDataService(BinanceRestClient binanceRestClient,
                             IndicatorCalculator indicatorCalculator,
                             NotificationService notificationService) {
        this.binanceRestClient = binanceRestClient;
        this.indicatorCalculator = indicatorCalculator;
        this.notificationService = notificationService;
    }

    public Map<String, Object> latestTicker(String symbol) {
        return tickerCache.get(symbol, k -> retryOnce(
                () -> binanceRestClient.tickerPrice(symbol),
                "ticker获取失败",
                "symbol=" + symbol
        ));
    }

    public List<KlineCandle> latestKlines(String symbol, String interval, int limit) {
        return latestKlinesWithNamespace("", symbol, interval, limit);
    }


    public List<KlineCandle> latestKlinesWithNamespace(String namespace, String symbol, String interval, int limit) {
        String ns = (namespace == null || namespace.isBlank()) ? "" : namespace + "_";
        String key = ns + symbol + "_" + interval + "_" + limit;
        return klineCache.get(key, k -> retryOnce(
                () -> binanceRestClient.klinesByWsApi(symbol, interval, limit),
                "K线获取失败",
                "namespace=" + ns + ", symbol=" + symbol + ", interval=" + interval
        ));
    }

    public Map<String, Double> calculateIndicatorsWithNamespace(String namespace,
                                                                String symbol,
                                                                String interval,
                                                                List<StrategyIndicator> indicators) {
        List<KlineCandle> klines = latestKlinesWithNamespace(namespace, symbol, interval, 200);
        Map<String, Double> values = indicatorCalculator.calculateAll(klines, indicators);
        String ns = (namespace == null || namespace.isBlank()) ? "" : namespace + "_";
        for (Map.Entry<String, Double> entry : values.entrySet()) {
            indicatorCache.put(ns + symbol + "_" + interval + "_" + entry.getKey(), entry.getValue());
        }
        return values;
    }

    public Map<String, Object> latestOrderBook(String symbol, int limit) {
        String key = symbol + "_" + limit;
        return orderBookCache.get(key, k -> retryOnce(
                () -> binanceRestClient.depth(symbol, limit),
                "OrderBook获取失败",
                "symbol=" + symbol + ", limit=" + limit
        ));
    }

    public Map<String, Object> topLevelsOrderBook(String symbol, int levels) {
        Map<String, Object> book = latestOrderBook(symbol, 50);
        List<List<Object>> bids = toLevels(book.get("bids"), levels);
        List<List<Object>> asks = toLevels(book.get("asks"), levels);
        return Map.of(
                "symbol", symbol,
                "lastUpdateId", book.getOrDefault("lastUpdateId", 0),
                "bids", bids,
                "asks", asks
        );
    }

    public Map<String, Double> calculateIndicators(String symbol, String interval, List<StrategyIndicator> indicators) {
        return calculateIndicatorsWithNamespace("", symbol, interval, indicators);
    }

    public Double getCachedIndicator(String symbol, String interval, String indicatorName) {
        return indicatorCache.getIfPresent(symbol + "_" + interval + "_" + indicatorName);
    }

    @SuppressWarnings("unchecked")
    private List<List<Object>> toLevels(Object raw, int levels) {
        if (!(raw instanceof List<?> rows)) {
            return List.of();
        }
        int size = Math.max(1, levels);
        return rows.stream()
                .filter(List.class::isInstance)
                .map(r -> (List<Object>) r)
                .limit(size)
                .toList();
    }

    private <T> T retryOnce(RetryTask<T> task, String title, String content) {
        Exception first = null;
        for (int attempt = 1; attempt <= 2; attempt++) {
            try {
                return task.run();
            } catch (Exception e) {
                if (attempt == 1) {
                    first = e;
                } else {
                    String msg = title + "，重试1次后仍失败: " + content;
                    notificationService.notify("BINANCE", title, msg);
                    throw new IllegalStateException(msg, first == null ? e : first);
                }
            }
        }
        throw new IllegalStateException("不可达代码");
    }

    @FunctionalInterface
    private interface RetryTask<T> {
        T run();
    }
}
