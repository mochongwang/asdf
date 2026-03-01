package com.example.quant.notify;

import com.example.quant.config.AppProperties;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

/**
 * 企业微信 webhook 客户端。
 */
@Component
public class WeComWebhookClient {

    private final RestClient restClient = RestClient.builder().build();
    private final AppProperties properties;

    public WeComWebhookClient(AppProperties properties) {
        this.properties = properties;
    }

    public void sendText(String text) {
        if (!properties.getNotify().isEnabled()) {
            return;
        }
        String url = properties.getNotify().getWechatWebhook();
        if (url == null || url.isBlank()) {
            return;
        }
        restClient.post()
                .uri(url)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of(
                        "msgtype", "text",
                        "text", Map.of("content", text)
                ))
                .retrieve()
                .toBodilessEntity();
    }
}
