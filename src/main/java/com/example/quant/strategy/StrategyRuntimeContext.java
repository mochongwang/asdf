package com.example.quant.strategy;

import com.example.quant.model.KlineCandle;

import java.util.List;
import java.util.Map;

/**
 * 策略运行上下文。
 *
 * @param symbol 交易对
 * @param triggerPeriod 触发周期
 * @param latestTicker 最新行情
 * @param klines 最近K线
 * @param indicatorValues 已计算指标值
 */
public record StrategyRuntimeContext(
        String symbol,
        String triggerPeriod,
        Map<String, Object> latestTicker,
        List<KlineCandle> klines,
        Map<String, Double> indicatorValues
) {
}
