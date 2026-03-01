package com.example.quant.model;

/**
 * 订单查询参数。
 */
public record OrderQuery(
        String symbol,
        String status,
        Long fromEpochSecond,
        Long toEpochSecond,
        Integer page,
        Integer size
) {
}
