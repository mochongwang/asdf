package com.example.quant.data;

import com.example.quant.model.KlineCandle;

import java.util.List;
import java.util.Map;

/**
 * 币安 REST 客户端抽象。
 */
public interface BinanceRestClient {

    /**
     * 查询最新价格。
     *
     * @param symbol 交易对，例如 BTCUSDT
     * @return 简化后的返回数据
     */
    Map<String, Object> tickerPrice(String symbol);

    /**
     * 查询历史 K 线。
     *
     * @param symbol 交易对
     * @param interval 周期（1m/5m/15m/1h/4h/1d）
     * @param limit 数量（最大1000）
     * @return K线列表
     */
    List<KlineCandle> klines(String symbol, String interval, int limit);
}
