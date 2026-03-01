package com.example.quant.model;

import java.util.List;
import java.util.Map;

/**
 * 策略运行时数据。
 *
 * @param klines K线列表
 * @param indicatorValues 指标值（key=指标名称）
 */
public record StrategyRuntimeData(
        List<KlineCandle> klines,
        Map<String, Double> indicatorValues
) {
}
