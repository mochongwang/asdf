package com.example.quant.model;

import java.util.List;

/**
 * 回测汇总。
 */
public record BacktestSummary(
        String strategyId,
        int orderCount,
        double totalPnl,
        List<BacktestOrderResult> details
) {
}
