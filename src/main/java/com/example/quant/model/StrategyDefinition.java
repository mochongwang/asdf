package com.example.quant.model;

import java.util.List;

/**
 * 策略定义对象。
 *
 * @param id 策略ID
 * @param name 策略名称
 * @param symbol 交易对
 * @param triggerPeriod 触发周期
 * @param strategyPath 策略模板路径
 * @param useOrderBook 是否使用 orderbook
 * @param indicators 指标列表
 */
public record StrategyDefinition(
        String id,
        String name,
        String symbol,
        Period triggerPeriod,
        String strategyPath,
        boolean useOrderBook,
        List<StrategyIndicator> indicators
) {
}
