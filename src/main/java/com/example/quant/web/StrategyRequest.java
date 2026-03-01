package com.example.quant.web;

import com.example.quant.model.Period;
import com.example.quant.model.StrategyIndicator;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * 创建/更新策略请求。
 */
public record StrategyRequest(
        @NotBlank String id,
        @NotBlank String name,
        @NotBlank String symbol,
        @Min(0) double stopLossPct,
        @Min(0) double takeProfitPct,
        @Min(1) int leverage,
        @Min(1) int maxAmount,
        @NotNull Period triggerPeriod,
        @NotBlank String strategyPath,
        boolean useOrderBook,
        @NotNull List<StrategyIndicator> indicators
) {
}
