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
        @Min(1) double testAmount,
        Double feeRatePct,
        Double slippagePct,
        Integer latencyBars
) {

    public double feeRatePctOrDefault() {
        return safeNonNegative(feeRatePct, 0.04d);
    }

    public double slippagePctOrDefault() {
        return safeNonNegative(slippagePct, 0.02d);
    }

    public int latencyBarsOrDefault() {
        int raw = latencyBars == null ? 1 : latencyBars;
        return Math.max(0, raw);
    }

    private static double safeNonNegative(Double raw, double fallback) {
        if (raw == null || Double.isNaN(raw) || Double.isInfinite(raw)) {
            return fallback;
        }
        return Math.max(0d, raw);
    }
}
