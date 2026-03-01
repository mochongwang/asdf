package com.example.quant.model;

/**
 * 回测单次结果。
 */
public record BacktestOrderResult(
        int index,
        double entryPrice,
        double exitPrice,
        double pnl,
        double fee,
        String closeReason,
        int barsHeld
) {
}
