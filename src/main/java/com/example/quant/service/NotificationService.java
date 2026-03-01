package com.example.quant.service;

import com.example.quant.model.NotificationLog;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import com.example.quant.notify.WeComWebhookClient;

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
        return query(null, null, null, null, null, null, null);
    }

    public List<NotificationLog> query(Long id,
                                       String eventType,
                                       String title,
                                       String content,
                                       Long sentFromEpochSecond,
                                       Long sentToEpochSecond,
                                       Long createdFromEpochSecond) {
        long sentFrom = sentFromEpochSecond == null ? 0 : sentFromEpochSecond * 1000;
        long sentTo = sentToEpochSecond == null ? Long.MAX_VALUE : sentToEpochSecond * 1000;
        long createdFrom = createdFromEpochSecond == null ? 0 : createdFromEpochSecond * 1000;
        String event = eventType == null ? "" : eventType.trim();
        String t = title == null ? "" : title.trim();
        String c = content == null ? "" : content.trim();

        return jdbcTemplate.query(
                """
                SELECT id,event_type,title,content,sent_at,created_at
                FROM notification_logs
                WHERE (? IS NULL OR id = ?)
                  AND (? = '' OR event_type LIKE CONCAT('%', ?, '%'))
                  AND (? = '' OR title LIKE CONCAT('%', ?, '%'))
                  AND (? = '' OR content LIKE CONCAT('%', ?, '%'))
                  AND sent_at >= ?
                  AND sent_at <= ?
                  AND created_at >= ?
                ORDER BY created_at DESC
                """,
                this::mapRow,
                id, id,
                event, event,
                t, t,
                c, c,
                sentFrom,
                sentTo,
                createdFrom
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
