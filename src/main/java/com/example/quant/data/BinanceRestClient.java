package com.example.quant.data;

import java.util.Map;

/**
 * 币安 REST 客户端抽象。
 *
 * <p>把真实 HTTP 调用隔离出来，便于替换 mock 或真实实现。</p>
 */
public interface BinanceRestClient {

    /**
     * 查询最新价格。
     *
     * @param symbol 交易对，例如 BTCUSDT
     * @return 简化后的返回数据
     */
    Map<String, Object> tickerPrice(String symbol);
}
