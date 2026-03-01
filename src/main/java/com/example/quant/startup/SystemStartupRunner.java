package com.example.quant.startup;

import com.example.quant.service.LeverageService;
import com.example.quant.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * 系统启动时序编排。
 */
@Component
public class SystemStartupRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(SystemStartupRunner.class);

    private final LeverageService leverageService;
    private final NotificationService notificationService;

    public SystemStartupRunner(LeverageService leverageService, NotificationService notificationService) {
        this.leverageService = leverageService;
        this.notificationService = notificationService;
    }

    @Override
    public void run(ApplicationArguments args) {
        log.info("[启动时序] 1) JVM 已启动");
        log.info("[启动时序] 2) Web 服务已启动，WebSocket位点已预留");
        log.info("[启动时序] 3) 更新杠杆信息表，当前记录数: {}", leverageService.list().size());
        log.info("[启动时序] 4) 账户信息监控位点已预留");
        log.info("[启动时序] 5) 订单信息监控位点已预留");
        notificationService.notify("SYSTEM", "系统启动", "系统启动完成，核心服务已就绪");
    }
}
