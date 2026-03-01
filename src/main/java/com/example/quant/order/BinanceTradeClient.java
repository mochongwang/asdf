package com.example.quant.order;

import com.example.quant.model.PlaceOrderCommand;

/**
 * 币安下单客户端。
 */
public interface BinanceTradeClient {

    /**
     * 提交订单到币安。
     *
     * @param command 下单命令
     * @param quantity 下单数量
     * @return 交易所订单ID
     */
    String placeOrder(PlaceOrderCommand command, double quantity);
}
