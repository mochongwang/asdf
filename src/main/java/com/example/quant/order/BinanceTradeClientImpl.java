package com.example.quant.order;

import com.example.quant.config.AppProperties;
import com.example.quant.model.OrderType;
import com.example.quant.model.PlaceOrderCommand;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Map;

/**
 * 币安现货下单实现（真实下单）。
 */
@Component
public class BinanceTradeClientImpl implements BinanceTradeClient {

    private final RestClient restClient = RestClient.builder().baseUrl("https://api.binance.com").build();
    private final AppProperties properties;

    public BinanceTradeClientImpl(AppProperties properties) {
        this.properties = properties;
    }

    @Override
    public String placeOrder(PlaceOrderCommand command, double quantity) {
        String apiKey = properties.getTrading().getApiKey();
        String secret = properties.getTrading().getApiSecret();
        if (apiKey == null || apiKey.isBlank() || secret == null || secret.isBlank()) {
            throw new IllegalArgumentException("真实下单模式下，必须配置 app.trading.api-key/api-secret");
        }

        long ts = Instant.now().toEpochMilli();
        String side = command.side().name().startsWith("OPEN") ? "BUY" : "SELL";
        String type = command.orderType() == OrderType.MARKET ? "MARKET" : "LIMIT";

        StringBuilder query = new StringBuilder();
        query.append("symbol=").append(command.symbol())
                .append("&side=").append(side)
                .append("&type=").append(type)
                .append("&quantity=").append(quantity);
        if (command.orderType() == OrderType.LIMIT && command.limitPrice() != null) {
            query.append("&timeInForce=GTC")
                    .append("&price=").append(command.limitPrice());
        }
        query.append("&timestamp=").append(ts);

        String signature = hmacSha256(secret, query.toString());
        String fullQuery = query + "&signature=" + signature;

        Map<String, Object> resp = restClient.post()
                .uri("/api/v3/order?" + fullQuery)
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-MBX-APIKEY", apiKey)
                .retrieve()
                .body(Map.class);

        Object orderId = resp == null ? null : resp.get("orderId");
        return orderId == null ? "UNKNOWN" : String.valueOf(orderId);
    }

    private String hmacSha256(String secret, String message) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] bytes = mac.doFinal(message.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes);
        } catch (Exception e) {
            throw new IllegalStateException("签名失败", e);
        }
    }
}
