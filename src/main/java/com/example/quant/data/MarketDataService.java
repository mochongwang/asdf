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
        String key = symbol + "_" + interval + "_" + limit;
        return klineCache.get(key, k -> retryOnce(
                () -> binanceRestClient.klinesByWsApi(symbol, interval, limit),
                "K线获取失败",
                "symbol=" + symbol + ", interval=" + interval
        ));
    }

    public Map<String, Double> calculateIndicators(String symbol, String interval, List<StrategyIndicator> indicators) {
        List<KlineCandle> klines = latestKlines(symbol, interval, 200);
        Map<String, Double> values = indicatorCalculator.calculateAll(klines, indicators);
        for (Map.Entry<String, Double> entry : values.entrySet()) {
            indicatorCache.put(symbol + "_" + interval + "_" + entry.getKey(), entry.getValue());
        }
        return values;
    }

    public Double getCachedIndicator(String symbol, String interval, String indicatorName) {
        return indicatorCache.getIfPresent(symbol + "_" + interval + "_" + indicatorName);
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
