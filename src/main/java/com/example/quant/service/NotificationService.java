package com.example.quant.service;

import com.example.quant.model.NotificationLog;
import com.example.quant.notify.WeComWebhookClient;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;

/**
 * 通知服务（日志持久化到 DuckDB）。
 */
@Service
public class NotificationService {

    private final WeComWebhookClient webhookClient;
    private final JdbcTemplate jdbcTemplate;

    public NotificationService(WeComWebhookClient webhookClient, JdbcTemplate jdbcTemplate) {
        this.webhookClient = webhookClient;
        this.jdbcTemplate = jdbcTemplate;
    }

    public void notify(String eventType, String title, String content) {
        NotificationLog log = new NotificationLog();
        log.eventType = eventType;
        log.title = title;
        log.content = content;
        log.sentAt = Instant.now();
        log.createdAt = Instant.now();

        jdbcTemplate.update(
                """
                INSERT INTO notification_logs(event_type,title,content,sent_at,created_at)
                VALUES (?,?,?,?,?)
                """,
                log.eventType,
                log.title,
                log.content,
                log.sentAt.toEpochMilli(),
                log.createdAt.toEpochMilli()
        );

        webhookClient.sendText("[" + eventType + "] " + title + "\n" + content);
    }

    public List<NotificationLog> list() {
        return jdbcTemplate.query(
                "SELECT id,event_type,title,content,sent_at,created_at FROM notification_logs ORDER BY created_at DESC",
                this::mapRow
        );
    }

    private NotificationLog mapRow(ResultSet rs, int rowNum) throws SQLException {
        NotificationLog log = new NotificationLog();
        log.id = rs.getLong("id");
        log.eventType = rs.getString("event_type");
        log.title = rs.getString("title");
        log.content = rs.getString("content");
        log.sentAt = Instant.ofEpochMilli(rs.getLong("sent_at"));
        log.createdAt = Instant.ofEpochMilli(rs.getLong("created_at"));
        return log;
    }
}
