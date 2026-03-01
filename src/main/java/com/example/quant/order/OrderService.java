package com.example.quant.order;

import com.example.quant.model.PlaceOrderCommand;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 下单层服务。
 *
 * <p>当前为演示版：
 * 1) 先记录本地订单
 * 2) 预留真实交易所下单扩展点
 * </p>
 */
@Service
public class OrderService {

    /** 内存订单库，key=localOrderId。 */
    private final Map<String, OrderRecord> orderStore = new ConcurrentHashMap<>();

    /**
     * 提交订单。
     *
     * @param cmd 下单命令
     * @return 本地订单ID
     */
    public String placeOrder(PlaceOrderCommand cmd) {
        String localOrderId = UUID.randomUUID().toString();

        OrderRecord record = new OrderRecord(
                localOrderId,
                cmd.strategyId(),
                cmd.symbol(),
                cmd.side(),
                cmd.orderType(),
                cmd.amountUsdt(),
                "已提交",
                Instant.now(),
                cmd.remark()
        );

        orderStore.put(localOrderId, record);
        return localOrderId;
    }

    /**
     * 强制平仓（示例：仅做记录）。
     *
     * @param symbol 交易对
     * @param remark 备注
     */
    public void forceCloseBySymbol(String symbol, String remark) {
        String localOrderId = UUID.randomUUID().toString();
        OrderRecord closeRecord = new OrderRecord(
                localOrderId,
                "SYSTEM",
                symbol,
                com.example.quant.model.OrderSide.CLOSE_LONG,
                com.example.quant.model.OrderType.MARKET,
                0,
                "强平提交",
                Instant.now(),
                remark
        );
        orderStore.put(localOrderId, closeRecord);
    }

    /**
     * 查询全部订单（按内存快照返回）。
     *
     * @return 订单列表
     */
    public List<OrderRecord> listOrders() {
        return new ArrayList<>(orderStore.values());
    }
}
