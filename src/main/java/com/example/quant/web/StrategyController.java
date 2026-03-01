package com.example.quant.web;

import com.example.quant.model.StrategyDefinition;
import com.example.quant.strategy.StrategyEngineService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 策略管理接口。
 */
@RestController
@RequestMapping("/api/strategies")
public class StrategyController {

    /** 示例策略仓库（生产可换成 DuckDB DAO）。 */
    private final Map<String, StrategyDefinition> strategyStore = new ConcurrentHashMap<>();

    private final StrategyEngineService strategyEngineService;

    public StrategyController(StrategyEngineService strategyEngineService) {
        this.strategyEngineService = strategyEngineService;
    }

    /**
     * 创建策略。
     *
     * @param req 请求体
     * @return 创建结果
     */
    @PostMapping
    public StrategyDefinition create(@Valid @RequestBody StrategyRequest req) {
        StrategyDefinition definition = new StrategyDefinition(
                req.id(),
                req.name(),
                req.symbol(),
                req.triggerPeriod(),
                req.strategyPath(),
                req.useOrderBook(),
                List.of()
        );
        strategyStore.put(definition.id(), definition);
        return definition;
    }

    /**
     * 获取策略列表。
     *
     * @return 策略列表
     */
    @GetMapping
    public List<StrategyDefinition> list() {
        return strategyStore.values().stream().toList();
    }

    /**
     * 启用策略。
     *
     * @param id 策略ID
     * @return 执行结果
     */
    @PostMapping("/{id}/enable")
    public String enable(@PathVariable String id) {
        StrategyDefinition definition = strategyStore.get(id);
        if (definition == null) {
            return "策略不存在";
        }
        strategyEngineService.enable(definition);
        return "已启用";
    }

    /**
     * 禁用策略。
     *
     * @param id 策略ID
     * @return 执行结果
     */
    @PostMapping("/{id}/disable")
    public String disable(@PathVariable String id) {
        strategyEngineService.disable(id);
        return "已禁用";
    }

    /**
     * 手动触发一次策略。
     *
     * @param id 策略ID
     * @return 触发结果
     */
    @PostMapping("/{id}/trigger")
    public String trigger(@PathVariable String id) {
        StrategyDefinition definition = strategyStore.get(id);
        if (definition == null) {
            return "策略不存在";
        }
        return strategyEngineService.triggerOnce(id, definition.strategyPath());
    }
}
