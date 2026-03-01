package com.example.quant.service;

import com.example.quant.model.NotificationLog;
import com.example.quant.notify.WeComWebhookClient;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 通知服务。
 */
@Service
public class NotificationService {
    private final AtomicLong sequence = new AtomicLong(1);
    private final List<NotificationLog> logs = new CopyOnWriteArrayList<>();
    private final WeComWebhookClient webhookClient;

    public NotificationService(WeComWebhookClient webhookClient) {
        this.webhookClient = webhookClient;
    }

    public void notify(String eventType, String title, String content) {
        NotificationLog log = new NotificationLog();
        log.id = sequence.getAndIncrement();
        log.eventType = eventType;
        log.title = title;
        log.content = content;
        log.sentAt = Instant.now();
        log.createdAt = Instant.now();
        logs.add(log);

        webhookClient.sendText("[" + eventType + "] " + title + "\n" + content);
    }

    public List<NotificationLog> list() {
        return new ArrayList<>(logs);
    }
}
