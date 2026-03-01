package com.example.quant.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * 策略主表实体（内存版）。
 */
public class StrategyEntity {

    /** 策略唯一ID。 */
    public String id;
    /** 策略名称。 */
    public String name;
    /** 交易对。 */
    public String symbol;
    /** 止损百分比。 */
    public double stopLossPct;
    /** 止盈百分比。 */
    public double takeProfitPct;
    /** 杠杆倍数。 */
    public int leverage;
    /** 最大下单金额。 */
    public int maxAmount;
    /** 触发周期。 */
    public Period triggerPeriod;
    /** 是否启用 orderbook。 */
    public boolean useOrderBook;
    /** 策略路径。 */
    public String strategyPath;
    /** 是否启用。 */
    public boolean enabled;
    /** 状态。 */
    public String status;
    /** 创建时间。 */
    public Instant createdAt;
    /** 更新时间。 */
    public Instant updatedAt;
    /** 指标列表。 */
    public List<StrategyIndicator> indicators = new ArrayList<>();
}
