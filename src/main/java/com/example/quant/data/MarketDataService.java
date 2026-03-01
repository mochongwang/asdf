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
 * 行情数据服务。
 *
 * <p>职责：</p>
 * <ul>
 *   <li>封装 ws-api 行情读取（ticker / klines / depth）</li>
 *   <li>提供缓存，降低上游调用频率</li>
 *   <li>统一重试一次并通知的失败处理策略</li>
 *   <li>提供策略执行所需指标计算入口</li>
 * </ul>
 *
 * <p>需求覆盖：</p>
 * <ul>
 *   <li>“取数据层只用 WebSocket API（ws-api）”</li>
 *   <li>“Binance 失败重试 1 次并通知”</li>
 *   <li>“指标统一由 TA-Lib 计算”</li>
 * </ul>
 */
@Service
public class MarketDataService {

    /** ticker 缓存：短 TTL，适配实时价格。 */
    private final Cache<String, Map<String, Object>> tickerCache = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofSeconds(10))
            .maximumSize(1_000)
            .build();

    /** K 线缓存：中短 TTL，兼顾实时性与性能。 */
    private final Cache<String, List<KlineCandle>> klineCache = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofSeconds(20))
            .maximumSize(2_000)
            .build();

    /** 指标缓存：按 symbol + period + indicatorName 缓存最新结果。 */
    private final Cache<String, Double> indicatorCache = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofSeconds(20))
            .maximumSize(5_000)
            .build();

    /** orderbook 缓存：TTL 更短，保证盘口实时性。 */
    private final Cache<String, Map<String, Object>> orderBookCache = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofSeconds(5))
            .maximumSize(2_000)
            .build();

    /** Binance ws-api 客户端。 */
    private final BinanceRestClient binanceRestClient;

    /** TA-Lib 指标计算组件。 */
    private final IndicatorCalculator indicatorCalculator;

    /** 通知服务（重试失败后触发告警）。 */
    private final NotificationService notificationService;

    public MarketDataService(BinanceRestClient binanceRestClient,
                             IndicatorCalculator indicatorCalculator,
                             NotificationService notificationService) {
        this.binanceRestClient = binanceRestClient;
        this.indicatorCalculator = indicatorCalculator;
        this.notificationService = notificationService;
    }

    /**
     * 获取最新 ticker。
     *
     * <p>需求覆盖：ws-api ticker + 失败重试通知。</p>
     *
     * @param symbol 交易对，例如 BTCUSDT
     * @return ticker 字典（至少包含 price）
     */
    public Map<String, Object> latestTicker(String symbol) {
        return tickerCache.get(symbol, k -> retryOnce(
                () -> binanceRestClient.tickerPrice(symbol),
                "ticker获取失败",
                "symbol=" + symbol
        ));
    }

    /**
     * 获取最新 K 线（默认命名空间）。
     *
     * @param symbol 交易对
     * @param interval 周期，例如 1m/5m/1h
     * @param limit 返回条数
     * @return K 线列表（按时间顺序）
     */
    public List<KlineCandle> latestKlines(String symbol, String interval, int limit) {
        return latestKlinesWithNamespace("", symbol, interval, limit);
    }

    /**
     * 获取指定命名空间下的 K 线。
     *
     * <p>需求覆盖：HC_ 命名空间与实时缓存隔离（回测与实盘互不污染）。</p>
     *
     * @param namespace 缓存命名空间，空字符串表示默认
     * @param symbol 交易对
     * @param interval 周期
     * @param limit 条数
     * @return K 线列表
     */
    public List<KlineCandle> latestKlinesWithNamespace(String namespace, String symbol, String interval, int limit) {
        String ns = (namespace == null || namespace.isBlank()) ? "" : namespace + "_";
        String key = ns + symbol + "_" + interval + "_" + limit;
        return klineCache.get(key, k -> retryOnce(
                () -> binanceRestClient.klinesByWsApi(symbol, interval, limit),
                "K线获取失败",
                "namespace=" + ns + ", symbol=" + symbol + ", interval=" + interval
        ));
    }

    /**
     * 计算并缓存命名空间下的指标。
     *
     * <p>需求覆盖：指标统一由 TA-Lib 计算，并支持回测与实时隔离。</p>
     *
     * @param namespace 缓存命名空间
     * @param symbol 交易对
     * @param interval 周期
     * @param indicators 策略指标定义
     * @return 指标名称到值的映射
     */
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

    /**
     * 获取原始 orderbook（缓存 + 重试）。
     *
     * @param symbol 交易对
     * @param limit 深度档位数
     * @return orderbook 原始结构
     */
    public Map<String, Object> latestOrderBook(String symbol, int limit) {
        String key = symbol + "_" + limit;
        return orderBookCache.get(key, k -> retryOnce(
                () -> binanceRestClient.depth(symbol, limit),
                "OrderBook获取失败",
                "symbol=" + symbol + ", limit=" + limit
        ));
    }

    /**
     * 获取前 N 档盘口。
     *
     * <p>需求覆盖：提供策略可直接消费的前 N 档盘口数据。</p>
     *
     * @param symbol 交易对
     * @param levels 返回档位数
     * @return 标准化盘口对象（symbol / lastUpdateId / bids / asks）
     */
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

    /**
     * 计算默认命名空间指标。
     *
     * @param symbol 交易对
     * @param interval 周期
     * @param indicators 指标定义
     * @return 指标计算结果
     */
    public Map<String, Double> calculateIndicators(String symbol, String interval, List<StrategyIndicator> indicators) {
        return calculateIndicatorsWithNamespace("", symbol, interval, indicators);
    }

    /**
     * 查询已缓存指标值。
     *
     * @param symbol 交易对
     * @param interval 周期
     * @param indicatorName 指标名称（与缓存 key 一致）
     * @return 缓存命中值；未命中返回 null
     */
    public Double getCachedIndicator(String symbol, String interval, String indicatorName) {
        return indicatorCache.getIfPresent(symbol + "_" + interval + "_" + indicatorName);
    }

    /**
     * 将原始盘口对象裁剪为前 N 档列表。
     *
     * @param raw 原始字段
     * @param levels 档位数
     * @return 前 N 档（每档通常为 price/qty）
     */
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

    /**
     * 通用“重试一次”包装器。
     *
     * <p>需求覆盖：外部行情调用失败后必须重试 1 次；若仍失败则通知并抛错。</p>
     *
     * @param task 实际执行逻辑
     * @param title 通知标题
     * @param content 通知内容
     * @param <T> 返回值类型
     * @return 成功结果
     */
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

    /** 可抛异常的函数接口，用于重试包装。 */
    @FunctionalInterface
    private interface RetryTask<T> {
        /**
         * 执行任务。
         *
         * @return 任务结果
         */
        T run();
    }
}
