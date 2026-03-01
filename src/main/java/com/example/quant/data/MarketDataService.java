package com.example.quant.data;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;

/**
 * 取数据层服务。
 *
 * <p>负责：
 * 1) 从币安取基础行情
 * 2) 将热点数据做短期缓存
 * </p>
 */
@Service
public class MarketDataService {

    /** 缓存 key: symbol，value: ticker JSON。 */
    private final Cache<String, Map<String, Object>> tickerCache = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofSeconds(10))
            .maximumSize(1_000)
            .build();

    private final BinanceRestClient binanceRestClient;

    public MarketDataService(BinanceRestClient binanceRestClient) {
        this.binanceRestClient = binanceRestClient;
    }

    /**
     * 获取最新价格数据（优先缓存）。
     *
     * @param symbol 交易对，如 BTCUSDT
     * @return 价格信息
     */
    public Map<String, Object> latestTicker(String symbol) {
        return tickerCache.get(symbol, binanceRestClient::tickerPrice);
    }
}
