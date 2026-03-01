package com.example.quant.order;

import com.example.quant.model.OrderSide;
import com.example.quant.model.OrderType;

import java.time.Instant;

/**
 * 订单记录（示例版，当前使用内存存储）。
 *
 * @param localOrderId 本地订单ID
 * @param strategyId 策略ID
 * @param symbol 交易对
 * @param side 方向
 * @param orderType 类型
 * @param amountUsdt 金额
 * @param status 状态
 * @param createdAt 创建时间
 * @param remark 备注
 */
public record OrderRecord(
        String localOrderId,
        String strategyId,
        String symbol,
        OrderSide side,
        OrderType orderType,
        double amountUsdt,
        String status,
        Instant createdAt,
        String remark
) {
}
