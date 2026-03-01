package com.example.quant.service;

import com.example.quant.model.NotificationLog;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 通知服务（当前先记录日志，预留企业微信 webhook 扩展）。
 */
@Service
public class NotificationService {
    private final AtomicLong sequence = new AtomicLong(1);
    private final List<NotificationLog> logs = new CopyOnWriteArrayList<>();

    public void notify(String eventType, String title, String content) {
        NotificationLog log = new NotificationLog();
        log.id = sequence.getAndIncrement();
        log.eventType = eventType;
        log.title = title;
        log.content = content;
        log.sentAt = Instant.now();
        log.createdAt = Instant.now();
        logs.add(log);
    }

    public List<NotificationLog> list() {
        return new ArrayList<>(logs);
    }
}
