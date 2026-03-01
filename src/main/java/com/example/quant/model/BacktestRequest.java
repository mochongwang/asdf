package com.example.quant.model;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

/**
 * 回测请求。
 */
public record BacktestRequest(
        @NotNull Instant startTime,
        @NotNull Instant endTime,
        @Min(1) double testAmount
) {
}
