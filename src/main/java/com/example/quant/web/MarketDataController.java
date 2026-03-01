package com.example.quant.web;

import com.example.quant.data.OrderBookSubscriptionService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 行情接口。
 */
@RestController
@RequestMapping("/api/market")
public class MarketDataController {

    private final OrderBookSubscriptionService orderBookSubscriptionService;

    public MarketDataController(OrderBookSubscriptionService orderBookSubscriptionService) {
        this.orderBookSubscriptionService = orderBookSubscriptionService;
    }

    @GetMapping("/orderbook")
    public Map<String, Object> orderBook(
            @RequestParam String symbol,
            @RequestParam(defaultValue = "5") int levels
    ) {
        return orderBookSubscriptionService.topLevels(symbol, levels);
    }

    @PostMapping("/orderbook/subscribe")
    public String subscribeOrderBook(@RequestParam String symbol) {
        orderBookSubscriptionService.subscribe(symbol);
        return "已订阅: " + symbol;
    }

    @PostMapping("/orderbook/unsubscribe")
    public String unsubscribeOrderBook(@RequestParam String symbol) {
        orderBookSubscriptionService.unsubscribe(symbol);
        return "已取消订阅: " + symbol;
    }
}

