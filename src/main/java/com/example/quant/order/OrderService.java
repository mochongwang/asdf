package com.example.quant.order;

import com.example.quant.model.OrderQuery;
import com.example.quant.model.OrderSide;
import com.example.quant.model.OrderType;
import com.example.quant.model.PlaceOrderCommand;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 下单层服务。
 */
@Service
public class OrderService {

    private final Map<String, OrderRecord> orderStore = new ConcurrentHashMap<>();

    public String placeOrder(PlaceOrderCommand cmd) {
        String localOrderId = UUID.randomUUID().toString();
        OrderRecord record = new OrderRecord();
        record.localOrderId = localOrderId;
        record.strategyId = cmd.strategyId();
        record.symbol = cmd.symbol();
        record.side = cmd.side();
        record.orderType = cmd.orderType();
        record.amountUsdt = cmd.amountUsdt();
        record.status = "已提交";
        record.createdAt = Instant.now();
        record.remark = cmd.remark();
        orderStore.put(localOrderId, record);
        return localOrderId;
    }

    public void forceCloseBySymbol(String symbol, String remark) {
        String localOrderId = UUID.randomUUID().toString();
        OrderRecord closeRecord = new OrderRecord();
        closeRecord.localOrderId = localOrderId;
        closeRecord.strategyId = "SYSTEM";
        closeRecord.symbol = symbol;
        closeRecord.side = OrderSide.CLOSE_LONG;
        closeRecord.orderType = OrderType.MARKET;
        closeRecord.amountUsdt = 0;
        closeRecord.status = "强平提交";
        closeRecord.createdAt = Instant.now();
        closeRecord.remark = remark;
        orderStore.put(localOrderId, closeRecord);
    }

    public List<OrderRecord> query(OrderQuery query) {
        int page = query.page() == null ? 1 : Math.max(query.page(), 1);
        int size = query.size() == null ? 20 : Math.max(query.size(), 1);

        return orderStore.values().stream()
                .filter(o -> query.symbol() == null || query.symbol().isBlank() || o.symbol.equalsIgnoreCase(query.symbol()))
                .filter(o -> query.status() == null || query.status().isBlank() || o.status.equalsIgnoreCase(query.status()))
                .filter(o -> query.fromEpochSecond() == null || o.createdAt.getEpochSecond() >= query.fromEpochSecond())
                .filter(o -> query.toEpochSecond() == null || o.createdAt.getEpochSecond() <= query.toEpochSecond())
                .sorted(Comparator.comparing((OrderRecord o) -> o.createdAt).reversed())
                .skip((long) (page - 1) * size)
                .limit(size)
                .toList();
    }

    public List<OrderRecord> listOrders() {
        return new ArrayList<>(orderStore.values());
    }
}
