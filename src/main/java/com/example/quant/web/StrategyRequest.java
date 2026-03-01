package com.example.quant.web;

import com.example.quant.model.Period;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 创建策略请求。
 *
 * @param id 策略ID
 * @param name 策略名称
 * @param symbol 交易对
 * @param triggerPeriod 触发周期
 * @param strategyPath 策略模板路径
 * @param useOrderBook 是否使用 orderbook
 */
public record StrategyRequest(
        @NotBlank String id,
        @NotBlank String name,
        @NotBlank String symbol,
        @NotNull Period triggerPeriod,
        @NotBlank String strategyPath,
        boolean useOrderBook
) {
}
