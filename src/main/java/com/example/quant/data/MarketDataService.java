package com.example.quant.data;

import com.example.quant.model.KlineCandle;
import com.example.quant.model.StrategyIndicator;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * 取数据层服务。
 */
@Service
public class MarketDataService {

    /** 缓存 key: symbol，value: ticker JSON。 */
    private final Cache<String, Map<String, Object>> tickerCache = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofSeconds(10))
            .maximumSize(1_000)
            .build();

    /** 缓存 key: symbol_interval_limit，value: klines。 */
    private final Cache<String, List<KlineCandle>> klineCache = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofSeconds(20))
            .maximumSize(2_000)
            .build();

    /** 缓存 key: symbol_interval_indicatorName，value: indicatorValue。 */
    private final Cache<String, Double> indicatorCache = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofSeconds(20))
            .maximumSize(5_000)
            .build();

    private final BinanceRestClient binanceRestClient;
    private final IndicatorCalculator indicatorCalculator;

    public MarketDataService(BinanceRestClient binanceRestClient, IndicatorCalculator indicatorCalculator) {
        this.binanceRestClient = binanceRestClient;
        this.indicatorCalculator = indicatorCalculator;
    }

    public Map<String, Object> latestTicker(String symbol) {
        return tickerCache.get(symbol, binanceRestClient::tickerPrice);
    }

    public List<KlineCandle> latestKlines(String symbol, String interval, int limit) {
        String key = symbol + "_" + interval + "_" + limit;
        return klineCache.get(key, k -> binanceRestClient.klines(symbol, interval, limit));
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
}
