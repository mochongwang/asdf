package com.example.quant.data;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * OrderBook 订阅管理（当前实现为基于 ws-api depth 的周期刷新）。
 */
@Service
public class OrderBookSubscriptionService {

    private final MarketDataService marketDataService;
    private final Set<String> subscribedSymbols = ConcurrentHashMap.newKeySet();
    private final Map<String, Map<String, Object>> latestBooks = new ConcurrentHashMap<>();

    public OrderBookSubscriptionService(MarketDataService marketDataService) {
        this.marketDataService = marketDataService;
    }

    public void subscribe(String symbol) {
        if (symbol == null || symbol.isBlank()) {
            return;
        }
        subscribedSymbols.add(symbol.toUpperCase());
    }

    public void unsubscribe(String symbol) {
        if (symbol == null || symbol.isBlank()) {
            return;
        }
        String normalized = symbol.toUpperCase();
        subscribedSymbols.remove(normalized);
        latestBooks.remove(normalized);
    }

    public Map<String, Object> topLevels(String symbol, int levels) {
        String normalized = symbol.toUpperCase();
        Map<String, Object> latest = latestBooks.get(normalized);
        if (latest != null) {
            return trimLevels(normalized, latest, levels);
        }
        return marketDataService.topLevelsOrderBook(normalized, levels);
    }

    @Scheduled(fixedDelay = 1000)
    public void refreshSubscribedBooks() {
        for (String symbol : subscribedSymbols) {
            Map<String, Object> snapshot = marketDataService.latestOrderBook(symbol, 50);
            latestBooks.put(symbol, snapshot);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> trimLevels(String symbol, Map<String, Object> book, int levels) {
        int size = Math.max(1, levels);
        List<List<Object>> bids = (book.get("bids") instanceof List<?> rawBids)
                ? rawBids.stream().filter(List.class::isInstance).map(v -> (List<Object>) v).limit(size).toList()
                : List.of();
        List<List<Object>> asks = (book.get("asks") instanceof List<?> rawAsks)
                ? rawAsks.stream().filter(List.class::isInstance).map(v -> (List<Object>) v).limit(size).toList()
                : List.of();
        return Map.of(
                "symbol", symbol,
                "lastUpdateId", book.getOrDefault("lastUpdateId", 0),
                "bids", bids,
                "asks", asks
        );
    }
}
