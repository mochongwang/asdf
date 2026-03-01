package com.example.quant.web;

import com.example.quant.model.BacktestRequest;
import com.example.quant.model.BacktestSummary;
import com.example.quant.model.StrategyEntity;
import com.example.quant.service.BacktestService;
import com.example.quant.service.StrategyService;
import com.example.quant.strategy.StrategyEngineService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 策略管理接口。
 */
@RestController
@RequestMapping("/api/strategies")
public class StrategyController {

    private final StrategyService strategyService;
    private final StrategyEngineService strategyEngineService;
    private final BacktestService backtestService;

    public StrategyController(StrategyService strategyService,
                              StrategyEngineService strategyEngineService,
                              BacktestService backtestService) {
        this.strategyService = strategyService;
        this.strategyEngineService = strategyEngineService;
        this.backtestService = backtestService;
    }

    @PostMapping
    public StrategyEntity create(@Valid @RequestBody StrategyRequest req) {
        return strategyService.create(req);
    }

    @PutMapping("/{id}")
    public StrategyEntity update(@PathVariable String id, @Valid @RequestBody StrategyRequest req) {
        return strategyService.update(id, req);
    }

    @DeleteMapping("/{id}")
    public String delete(@PathVariable String id) {
        strategyService.delete(id);
        return "已删除";
    }

    @GetMapping
    public List<StrategyEntity> list() {
        return strategyService.list();
    }

    @GetMapping("/{id}")
    public StrategyEntity detail(@PathVariable String id) {
        return strategyService.getById(id);
    }

    @PostMapping("/{id}/enable")
    public String enable(@PathVariable String id) {
        StrategyEntity entity = strategyService.getById(id);
        strategyEngineService.enable(entity);
        strategyService.setEnabled(id, true, "running");
        return "已启用";
    }

    @PostMapping("/{id}/disable")
    public String disable(@PathVariable String id) {
        strategyEngineService.disable(id);
        strategyService.setEnabled(id, false, "stopped");
        return "已禁用";
    }

    @PostMapping("/{id}/trigger")
    public String trigger(@PathVariable String id) {
        StrategyEntity entity = strategyService.getById(id);
        return strategyEngineService.triggerOnce(id, entity.strategyPath);
    }

    @PostMapping("/{id}/backtest")
    public BacktestSummary backtest(@PathVariable String id, @Valid @RequestBody BacktestRequest req) {
        StrategyEntity entity = strategyService.getById(id);
        return backtestService.runBacktest(entity, req);
    }
}
