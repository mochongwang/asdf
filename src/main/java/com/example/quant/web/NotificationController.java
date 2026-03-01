package com.example.quant.web;

import com.example.quant.model.NotificationLog;
import com.example.quant.service.NotificationService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 通知日志查询接口。
 */
@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public List<NotificationLog> list(@RequestParam(required = false) String eventType,
                                      @RequestParam(required = false) String title,
                                      @RequestParam(required = false) String content) {
        return notificationService.list().stream()
                .filter(x -> eventType == null || eventType.isBlank() || x.eventType.contains(eventType))
                .filter(x -> title == null || title.isBlank() || x.title.contains(title))
                .filter(x -> content == null || content.isBlank() || x.content.contains(content))
                .toList();
    }
}
