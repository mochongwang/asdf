package com.example.quant.scheduler;

import com.example.quant.config.AppProperties;
import com.example.quant.strategy.StrategyEngineService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 策略自动触发调度器。
 */
@Component
public class StrategyTriggerScheduler {

    private static final Logger log = LoggerFactory.getLogger(StrategyTriggerScheduler.class);

    private final StrategyEngineService strategyEngineService;
    private final AppProperties properties;

    public StrategyTriggerScheduler(StrategyEngineService strategyEngineService, AppProperties properties) {
        this.strategyEngineService = strategyEngineService;
        this.properties = properties;
    }

    @Scheduled(fixedDelayString = "${app.scheduler.trigger-interval-ms:30000}")
    public void run() {
        if (!properties.getScheduler().isEnabled()) {
            return;
        }
        var result = strategyEngineService.triggerAllActive();
        if (!result.isEmpty()) {
            log.info("自动触发策略结果: {}", result);
        }
    }
}
