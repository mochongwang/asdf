package com.example.quant.model;

/**
 * K 线数据对象。
 *
 * @param openTime 开盘时间（毫秒时间戳）
 * @param open 开盘价
 * @param high 最高价
 * @param low 最低价
 * @param close 收盘价
 * @param volume 成交量
 * @param closeTime 收盘时间（毫秒时间戳）
 */
public record KlineCandle(
        long openTime,
        double open,
        double high,
        double low,
        double close,
        double volume,
        long closeTime
) {
}
