package com.example.quant.strategy;

import java.util.Map;

/**
 * 策略运行上下文。
 *
 * @param symbol 交易对
 * @param triggerPeriod 触发周期
 * @param latestTicker 最新行情
 */
public record StrategyRuntimeContext(
        String symbol,
        String triggerPeriod,
        Map<String, Object> latestTicker
) {
}
