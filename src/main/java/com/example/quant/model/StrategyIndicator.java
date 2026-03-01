package com.example.quant.model;

/**
 * 策略指标定义。
 *
 * @param name 指标名称（用于缓存 key）
 * @param type 指标类型
 * @param period 指标周期
 * @param paramsJson 指标参数 JSON
 */
public record StrategyIndicator(
        String name,
        IndicatorType type,
        Period period,
        String paramsJson
) {
}
