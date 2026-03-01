package com.example.quant.model;

import java.time.Instant;

/**
 * 账户信息。
 */
public class AccountInfo {
    /** apikey。 */
    public String apikey;
    /** 总余额。 */
    public double balance;
    /** 可用余额。 */
    public double availableBalance;
    /** 冻结余额。 */
    public double frozenBalance;
    /** 更新时间。 */
    public Instant updatedAt;
}
