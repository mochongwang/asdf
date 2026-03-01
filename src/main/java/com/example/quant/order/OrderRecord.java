package com.example.quant.order;

import com.example.quant.model.OrderSide;
import com.example.quant.model.OrderType;

import java.time.Instant;

/**
 * 订单记录（内存版）。
 */
public class OrderRecord {
    /** 本地订单ID。 */
    public String localOrderId;
    /** 策略ID。 */
    public String strategyId;
    /** 交易对。 */
    public String symbol;
    /** 方向。 */
    public OrderSide side;
    /** 类型。 */
    public OrderType orderType;
    /** 金额。 */
    public double amountUsdt;
    /** 状态。 */
    public String status;
    /** 创建时间。 */
    public Instant createdAt;
    /** 备注。 */
    public String remark;
}
