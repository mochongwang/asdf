package com.example.quant.model;

import java.time.Instant;

/**
 * 通知日志。
 */
public class NotificationLog {
    /** 自增ID（内存序列）。 */
    public long id;
    /** 事件类型。 */
    public String eventType;
    /** 标题。 */
    public String title;
    /** 内容。 */
    public String content;
    /** 发送时间。 */
    public Instant sentAt;
    /** 创建时间。 */
    public Instant createdAt;
}
