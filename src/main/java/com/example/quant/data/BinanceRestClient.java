package com.example.quant.data;

import com.example.quant.model.KlineCandle;

import java.util.List;
import java.util.Map;

/**
 * 币安数据客户端抽象（REST + WebSocket API）。
 */
public interface BinanceRestClient {

    Map<String, Object> tickerPrice(String symbol);

    /** REST K线。 */
    List<KlineCandle> klines(String symbol, String interval, int limit);

    /** WebSocket API K线（ws-api 请求式）。 */
    List<KlineCandle> klinesByWsApi(String symbol, String interval, int limit);
}
