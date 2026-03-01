package com.example.quant.strategy;

import com.example.quant.data.MarketDataService;
import com.example.quant.model.PlaceOrderCommand;
import com.example.quant.model.StrategyDefinition;
import com.example.quant.order.OrderService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 策略层核心服务。
 *
 * <p>职责：</p>
 * <ul>
 *     <li>维护策略注册与启停状态</li>
 *     <li>按策略 path 分发到具体策略模板</li>
 *     <li>把策略信号交给下单层</li>
 * </ul>
 */
@Service
public class StrategyEngineService {

    /** 已启用策略缓存，key=策略ID。 */
    private final Map<String, StrategyDefinition> activeStrategies = new ConcurrentHashMap<>();
    /** 策略模板注册表，key=strategyPath。 */
    private final Map<String, StrategyTemplate> strategyTemplates = new ConcurrentHashMap<>();

    private final MarketDataService marketDataService;
    private final OrderService orderService;

    public StrategyEngineService(List<StrategyTemplate> templates,
                                 MarketDataService marketDataService,
                                 OrderService orderService) {
        this.marketDataService = marketDataService;
        this.orderService = orderService;
        for (StrategyTemplate template : templates) {
            strategyTemplates.put(template.strategyPath(), template);
        }
    }

    /**
     * 启用策略。
     *
     * @param definition 策略定义
     */
    public void enable(StrategyDefinition definition) {
        activeStrategies.put(definition.id(), definition);
    }

    /**
     * 禁用策略。
     *
     * @param strategyId 策略ID
     */
    public void disable(String strategyId) {
        activeStrategies.remove(strategyId);
    }

    /**
     * 人工触发一次策略执行。
     *
     * @param strategyId 策略ID
     * @param strategyPath 策略路径标识
     * @return 执行结果说明
     */
    public String triggerOnce(String strategyId, String strategyPath) {
        StrategyDefinition definition = activeStrategies.get(strategyId);
        if (definition == null) {
            return "策略未启用";
        }
        StrategyTemplate template = strategyTemplates.get(strategyPath);
        if (template == null) {
            return "未找到策略模板: " + strategyPath;
        }

        Map<String, Object> ticker = marketDataService.latestTicker(definition.symbol());
        StrategyRuntimeContext context = new StrategyRuntimeContext(
                definition.symbol(),
                definition.triggerPeriod().code(),
                ticker
        );

        Optional<PlaceOrderCommand> cmd = template.evaluate(definition, context);
        if (cmd.isEmpty()) {
            return "无下单信号";
        }

        String localOrderId = orderService.placeOrder(cmd.get());
        return "已提交订单: " + localOrderId;
    }
}
