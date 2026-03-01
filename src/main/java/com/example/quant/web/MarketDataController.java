package com.example.quant.web;

import com.example.quant.data.MarketDataService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 行情接口。
 */
@RestController
@RequestMapping("/api/market")
public class MarketDataController {

    private final MarketDataService marketDataService;

    public MarketDataController(MarketDataService marketDataService) {
        this.marketDataService = marketDataService;
    }

    @GetMapping("/orderbook")
    public Map<String, Object> orderBook(
            @RequestParam String symbol,
            @RequestParam(defaultValue = "5") int levels
    ) {
        return marketDataService.topLevelsOrderBook(symbol, levels);
    }
}
