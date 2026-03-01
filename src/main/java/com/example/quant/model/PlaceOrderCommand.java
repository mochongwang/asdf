package com.example.quant.model;

/**
 * 下单命令。
 *
 * @param strategyId 策略ID
 * @param symbol 交易对
 * @param side 下单方向
 * @param orderType 订单类型
 * @param amountUsdt 下单金额（USDT）
 * @param limitPrice 限价价格（市价单可为 null）
 * @param stopLossPct 止损百分比
 * @param takeProfitPct 止盈百分比
 * @param leverage 杠杆
 * @param timeoutSeconds 超时秒数
 * @param remark 订单备注
 */
public record PlaceOrderCommand(
        String strategyId,
        String symbol,
        OrderSide side,
        OrderType orderType,
        double amountUsdt,
        Double limitPrice,
        double stopLossPct,
        double takeProfitPct,
        int leverage,
        int timeoutSeconds,
        String remark
) {
}
