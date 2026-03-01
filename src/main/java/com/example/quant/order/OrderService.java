package com.example.quant.order;

import com.example.quant.config.AppProperties;
import com.example.quant.data.BinanceRestClient;
import com.example.quant.model.OrderQuery;
import com.example.quant.model.OrderSide;
import com.example.quant.model.OrderType;
import com.example.quant.model.PlaceOrderCommand;
import com.example.quant.service.NotificationService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 下单服务。
 *
 * <p>职责：</p>
 * <ul>
 *   <li>统一订单入口（模拟单/真实单）</li>
 *   <li>真实下单失败“重试 1 次 + 通知”</li>
 *   <li>订单记录落地 DuckDB</li>
 *   <li>接收交易所轮询/用户流回报并回写状态</li>
 * </ul>
 *
 * <p>需求覆盖：</p>
 * <ul>
 *   <li>下单只走 REST API（真实模式）</li>
 *   <li>默认 simulation=true 的安全开关</li>
 *   <li>失败重试一次并通知</li>
 *   <li>订单持久化与状态同步</li>
 * </ul>
 */
@Service
public class OrderService {

    /** 业务配置（含 simulation 开关、API 凭证等）。 */
    private final AppProperties properties;

    /** 行情客户端：用于下单前按最新价格估算数量。 */
    private final BinanceRestClient marketClient;

    /** 真实交易客户端（REST 签名下单）。 */
    private final BinanceTradeClient binanceTradeClient;

    /** 通知服务（重试失败后告警）。 */
    private final NotificationService notificationService;

    /** DuckDB 访问模板。 */
    private final JdbcTemplate jdbcTemplate;

    public OrderService(AppProperties properties,
                        BinanceRestClient marketClient,
                        BinanceTradeClient binanceTradeClient,
                        NotificationService notificationService,
                        JdbcTemplate jdbcTemplate) {
        this.properties = properties;
        this.marketClient = marketClient;
        this.binanceTradeClient = binanceTradeClient;
        this.notificationService = notificationService;
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 统一下单入口。
     *
     * <p>需求覆盖：模拟/真实下单分流，真实模式失败重试一次并通知，订单记录持久化。</p>
     *
     * @param cmd 下单命令
     * @return 本地订单 ID（localOrderId）
     */
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
            Exception first = null;
            String exchangeOrderId = null;
            for (int attempt = 1; attempt <= 2; attempt++) {
                try {
                    exchangeOrderId = binanceTradeClient.placeOrder(cmd, quantity);
                    break;
                } catch (Exception e) {
                    if (attempt == 1) {
                        first = e;
                    } else {
                        String msg = "币安下单失败，重试1次后仍失败: symbol=" + cmd.symbol();
                        notificationService.notify("BINANCE", "下单失败", msg);
                        throw new IllegalStateException(msg, first == null ? e : first);
                    }
                }
            }
            record.exchangeOrderId = exchangeOrderId;
            record.status = "真实已提交";
        }

        insertOrder(record);
        return localOrderId;
    }

    /**
     * 按交易对写入强平记录（系统内部指令）。
     *
     * @param symbol 交易对
     * @param remark 备注说明
     */
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
        insertOrder(closeRecord);
    }

    /**
     * 同步交易所订单状态（请求式补偿同步路径）。
     *
     * @param exchangeOrderId 交易所订单号
     * @param symbol 交易对
     * @param status 交易所状态
     */
    public void syncExchangeOrder(String exchangeOrderId, String symbol, String status) {
        if (exchangeOrderId == null || exchangeOrderId.isBlank()) {
            return;
        }
        int updated = jdbcTemplate.update(
                """
                UPDATE orders
                SET status=?
                WHERE exchange_order_id=?
                """,
                status,
                exchangeOrderId
        );
        if (updated == 0 && symbol != null && !symbol.isBlank()) {
            jdbcTemplate.update(
                    """
                    INSERT INTO orders(local_order_id,exchange_order_id,strategy_id,symbol,side,order_type,amount_usdt,quantity,status,created_at,remark)
                    VALUES (?,?,?,?,?,?,?,?,?,?,?)
                    """,
                    "SYNC-" + exchangeOrderId,
                    exchangeOrderId,
                    "EXCHANGE_SYNC",
                    symbol,
                    OrderSide.CLOSE_LONG.name(),
                    OrderType.MARKET.name(),
                    0d,
                    0d,
                    status,
                    Instant.now().toEpochMilli(),
                    "交易所订单状态同步"
            );
        }
    }

    /**
     * 同步 executionReport（用户流推送路径）。
     *
     * <p>需求覆盖：私有用户流订单状态同步到本地订单表。</p>
     *
     * @param symbol 交易对
     * @param exchangeOrderId 交易所订单号
     * @param status 订单状态
     * @param side 买卖方向（BUY/SELL）
     * @param executedQty 已成交数量
     * @param price 成交价（或订单价格）
     */
    public void syncExecutionReport(String symbol,
                                    String exchangeOrderId,
                                    String status,
                                    String side,
                                    double executedQty,
                                    double price) {
        if (exchangeOrderId == null || exchangeOrderId.isBlank()) {
            return;
        }
        String normalizedStatus = (status == null || status.isBlank()) ? "UNKNOWN" : status;
        int updated = jdbcTemplate.update(
                """
                UPDATE orders
                SET status=?, quantity=?
                WHERE exchange_order_id=?
                """,
                normalizedStatus,
                executedQty > 0 ? executedQty : 0,
                exchangeOrderId
        );
        if (updated == 0 && symbol != null && !symbol.isBlank()) {
            String mappedSide = "BUY".equalsIgnoreCase(side) ? OrderSide.OPEN_LONG.name() : OrderSide.CLOSE_LONG.name();
            jdbcTemplate.update(
                    """
                    INSERT INTO orders(local_order_id,exchange_order_id,strategy_id,symbol,side,order_type,amount_usdt,quantity,status,created_at,remark)
                    VALUES (?,?,?,?,?,?,?,?,?,?,?)
                    """,
                    "STREAM-" + exchangeOrderId,
                    exchangeOrderId,
                    "EXCHANGE_STREAM",
                    symbol,
                    mappedSide,
                    OrderType.MARKET.name(),
                    0d,
                    executedQty,
                    normalizedStatus,
                    Instant.now().toEpochMilli(),
                    "用户流 executionReport 同步"
            );
        }
    }

    /**
     * 按条件分页查询订单。
     *
     * @param query 查询条件
     * @return 订单列表
     */
    public List<OrderRecord> query(OrderQuery query) {
        int page = query.page() == null ? 1 : Math.max(query.page(), 1);
        int size = query.size() == null ? 20 : Math.max(query.size(), 1);
        long fromSec = query.fromEpochSecond() == null ? 0 : query.fromEpochSecond();
        long toSec = query.toEpochSecond() == null ? Long.MAX_VALUE : query.toEpochSecond();
        String symbol = query.symbol() == null ? "" : query.symbol().trim();
        String status = query.status() == null ? "" : query.status().trim();

        return jdbcTemplate.query(
                """
                SELECT * FROM orders
                WHERE (? = '' OR lower(symbol)=lower(?))
                  AND (? = '' OR lower(status)=lower(?))
                  AND created_at >= ?
                  AND created_at <= ?
                ORDER BY created_at DESC
                LIMIT ? OFFSET ?
                """,
                this::mapRow,
                symbol, symbol,
                status, status,
                fromSec * 1000,
                toSec * 1000,
                size,
                (long) (page - 1) * size
        );
    }

    /**
     * 查询全部订单。
     *
     * @return 全量订单列表（按创建时间倒序）
     */
    public List<OrderRecord> listOrders() {
        return jdbcTemplate.query("SELECT * FROM orders ORDER BY created_at DESC", this::mapRow);
    }

    /**
     * 插入订单记录。
     *
     * @param record 订单实体
     */
    private void insertOrder(OrderRecord record) {
        jdbcTemplate.update(
                """
                INSERT INTO orders(local_order_id,exchange_order_id,strategy_id,symbol,side,order_type,amount_usdt,quantity,status,created_at,remark)
                VALUES (?,?,?,?,?,?,?,?,?,?,?)
                """,
                record.localOrderId,
                record.exchangeOrderId,
                record.strategyId,
                record.symbol,
                record.side.name(),
                record.orderType.name(),
                record.amountUsdt,
                record.quantity,
                record.status,
                record.createdAt.toEpochMilli(),
                record.remark
        );
    }

    /**
     * JDBC 行映射。
     *
     * @param rs 结果集
     * @param rowNum 行号
     * @return 订单实体
     * @throws SQLException 数据库访问异常
     */
    private OrderRecord mapRow(ResultSet rs, int rowNum) throws SQLException {
        OrderRecord r = new OrderRecord();
        r.localOrderId = rs.getString("local_order_id");
        r.exchangeOrderId = rs.getString("exchange_order_id");
        r.strategyId = rs.getString("strategy_id");
        r.symbol = rs.getString("symbol");
        r.side = OrderSide.valueOf(rs.getString("side"));
        r.orderType = OrderType.valueOf(rs.getString("order_type"));
        r.amountUsdt = rs.getDouble("amount_usdt");
        r.quantity = rs.getDouble("quantity");
        r.status = rs.getString("status");
        r.createdAt = Instant.ofEpochMilli(rs.getLong("created_at"));
        r.remark = rs.getString("remark");
        return r;
    }

    /**
     * 按最新行情换算下单数量。
     *
     * <p>需求覆盖：下单金额（USDT）转换为数量，统一以最新 ticker 价格计算。</p>
     *
     * @param symbol 交易对
     * @param amountUsdt 下单金额（USDT）
     * @return 下单数量
     */
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
