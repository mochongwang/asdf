package com.example.quant.model;

import java.time.Instant;

/**
 * 杠杆信息。
 */
public class LeverageInfo {
    /** 交易对。 */
    public String symbol;
    /** 全仓杠杆。 */
    public int crossLeverage;
    /** 逐仓杠杆。 */
    public int isolatedLeverage;
    /** 创建时间。 */
    public Instant createdAt;
}
