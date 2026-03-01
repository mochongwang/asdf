package com.example.quant.order;

import com.example.quant.config.AppProperties;
import com.example.quant.data.BinanceRestClient;
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
    private final AppProperties properties;
    private final BinanceRestClient marketClient;
    private final BinanceTradeClient binanceTradeClient;

    public OrderService(AppProperties properties,
                        BinanceRestClient marketClient,
                        BinanceTradeClient binanceTradeClient) {
        this.properties = properties;
        this.marketClient = marketClient;
        this.binanceTradeClient = binanceTradeClient;
    }

    public String placeOrder(PlaceOrderCommand cmd) {
        String localOrderId = UUID.randomUUID().toString();
        OrderRecord record = new OrderRecord();
        record.localOrderId = localOrderId;
        record.strategyId = cmd.strategyId();
        record.symbol = cmd.symbol();
        record.side = cmd.side();
        record.orderType = cmd.orderType();
        record.amountUsdt = cmd.amountUsdt();
        record.createdAt = Instant.now();
        record.remark = cmd.remark();

        double quantity = calculateQuantity(cmd.symbol(), cmd.amountUsdt());
        record.quantity = quantity;

        if (properties.getTrading().isSimulation()) {
            record.exchangeOrderId = "SIM-" + localOrderId.substring(0, 8);
            record.status = "模拟已提交";
        } else {
            String exchangeOrderId = binanceTradeClient.placeOrder(cmd, quantity);
            record.exchangeOrderId = exchangeOrderId;
            record.status = "真实已提交";
        }

        orderStore.put(localOrderId, record);
        return localOrderId;
    }

    public void forceCloseBySymbol(String symbol, String remark) {
        String localOrderId = UUID.randomUUID().toString();
        OrderRecord closeRecord = new OrderRecord();
        closeRecord.localOrderId = localOrderId;
        closeRecord.exchangeOrderId = "FORCE-" + localOrderId.substring(0, 8);
        closeRecord.strategyId = "SYSTEM";
        closeRecord.symbol = symbol;
        closeRecord.side = OrderSide.CLOSE_LONG;
        closeRecord.orderType = OrderType.MARKET;
        closeRecord.amountUsdt = 0;
        closeRecord.quantity = 0;
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

    private double calculateQuantity(String symbol, double amountUsdt) {
        Map<String, Object> ticker = marketClient.tickerPrice(symbol);
        Object priceObj = ticker.get("price");
        if (priceObj == null) {
            throw new IllegalArgumentException("无法获取最新价格，不能计算下单数量");
        }
        double price = Double.parseDouble(String.valueOf(priceObj));
        if (price <= 0) {
            throw new IllegalArgumentException("价格异常: " + price);
        }
        return amountUsdt / price;
    }
}
