package com.example.quant.strategy;

import com.example.quant.data.MarketDataService;
import com.example.quant.data.OrderBookSubscriptionService;
import com.example.quant.model.PlaceOrderCommand;
import com.example.quant.model.StrategyDefinition;
import com.example.quant.model.StrategyEntity;
import com.example.quant.order.OrderService;
import com.example.quant.service.NotificationService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 策略层核心服务。
 */
@Service
public class StrategyEngineService {

    private final Map<String, StrategyEntity> activeStrategies = new ConcurrentHashMap<>();
    private final Map<String, StrategyTemplate> strategyTemplates = new ConcurrentHashMap<>();

    private final MarketDataService marketDataService;
    private final OrderService orderService;
    private final NotificationService notificationService;
    private final OrderBookSubscriptionService orderBookSubscriptionService;

    public StrategyEngineService(List<StrategyTemplate> templates,
                                 MarketDataService marketDataService,
                                 OrderService orderService,
                                 NotificationService notificationService,
                                 OrderBookSubscriptionService orderBookSubscriptionService) {
        this.marketDataService = marketDataService;
        this.orderService = orderService;
        this.notificationService = notificationService;
        this.orderBookSubscriptionService = orderBookSubscriptionService;
        for (StrategyTemplate template : templates) {
            strategyTemplates.put(template.strategyPath(), template);
        }
    }

    public void enable(StrategyEntity entity) {
        activeStrategies.put(entity.id, entity);
        if (entity.useOrderBook) {
            orderBookSubscriptionService.subscribe(entity.symbol);
        }
        notificationService.notify("STRATEGY", "策略启用", "策略已启用: " + entity.name);
    }

    public void disable(String strategyId) {
        StrategyEntity removed = activeStrategies.remove(strategyId);
        if (removed != null) {
            orderService.forceCloseBySymbol(removed.symbol, "策略禁用触发强平");
            if (removed.useOrderBook && activeStrategies.values().stream().noneMatch(s -> s.useOrderBook && s.symbol.equalsIgnoreCase(removed.symbol))) {
                orderBookSubscriptionService.unsubscribe(removed.symbol);
            }
            notificationService.notify("STRATEGY", "策略禁用", "策略已禁用: " + removed.name);
        }
    }

    public String triggerOnce(String strategyId, String strategyPath) {
        StrategyEntity entity = activeStrategies.get(strategyId);
        if (entity == null) {
            return "策略未启用";
        }
        StrategyTemplate template = strategyTemplates.get(strategyPath);
        if (template == null) {
            return "未找到策略模板: " + strategyPath;
        }

        StrategyDefinition definition = new StrategyDefinition(
                entity.id,
                entity.name,
                entity.symbol,
                entity.triggerPeriod,
                entity.strategyPath,
                entity.useOrderBook,
                entity.indicators
        );

        String interval = entity.triggerPeriod.code();
        Map<String, Object> ticker = marketDataService.latestTicker(entity.symbol);
        List<com.example.quant.model.KlineCandle> klines = marketDataService.latestKlines(entity.symbol, interval, 200);
        Map<String, Double> indicators = marketDataService.calculateIndicators(entity.symbol, interval, entity.indicators);
        Map<String, Object> orderBook = entity.useOrderBook
                ? orderBookSubscriptionService.topLevels(entity.symbol, 5)
                : Map.of();

        StrategyRuntimeContext context = new StrategyRuntimeContext(entity.symbol, interval, ticker, klines, indicators, orderBook);

        Optional<PlaceOrderCommand> cmd = template.evaluate(definition, context);
        if (cmd.isEmpty()) {
            return "无下单信号";
        }

        String localOrderId = orderService.placeOrder(cmd.get());
        notificationService.notify("ORDER", "策略下单", "已提交订单: " + localOrderId);
        return "已提交订单: " + localOrderId;
    }

    public List<String> triggerAllActive() {
        List<String> results = new ArrayList<>();
        for (StrategyEntity entity : activeStrategies.values()) {
            results.add(entity.id + ": " + triggerOnce(entity.id, entity.strategyPath));
        }
        return results;
    }
}
