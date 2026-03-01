package com.example.quant.order;

import com.example.quant.model.OrderSide;
import com.example.quant.model.OrderType;

import java.time.Instant;

/**
 * 订单记录（内存版）。
 */
public class OrderRecord {
    public String localOrderId;
    public String exchangeOrderId;
    public String strategyId;
    public String symbol;
    public OrderSide side;
    public OrderType orderType;
    public double amountUsdt;
    public double quantity;
    public String status;
    public Instant createdAt;
    public String remark;
}
