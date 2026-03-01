package com.example.quant.notify;

import com.example.quant.config.AppProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

/**
 * 企业微信 webhook 客户端。
 */
@Component
public class WeComWebhookClient {

    private static final Logger log = LoggerFactory.getLogger(WeComWebhookClient.class);

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
            log.warn("企业微信通知已启用但 webhook 为空，已跳过发送");
            return;
        }

        try {
            restClient.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of(
                            "msgtype", "text",
                            "text", Map.of("content", text)
                    ))
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception e) {
            // 通知失败不应阻断主交易流程。
            log.error("企业微信通知发送失败: {}", e.getMessage());
        }
    }
}
