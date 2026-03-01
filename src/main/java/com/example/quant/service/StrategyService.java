package com.example.quant.service;

import com.example.quant.model.StrategyEntity;
import com.example.quant.model.StrategyIndicator;
import com.example.quant.web.StrategyRequest;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 策略 CRUD 服务。
 */
@Service
public class StrategyService {

    /** 策略仓库。 */
    private final Map<String, StrategyEntity> strategyStore = new ConcurrentHashMap<>();

    public StrategyEntity create(StrategyRequest req) {
        if (strategyStore.containsKey(req.id())) {
            throw new IllegalArgumentException("策略ID已存在: " + req.id());
        }

        StrategyEntity entity = new StrategyEntity();
        entity.id = req.id();
        entity.name = req.name();
        entity.symbol = req.symbol();
        entity.stopLossPct = req.stopLossPct();
        entity.takeProfitPct = req.takeProfitPct();
        entity.leverage = req.leverage();
        entity.maxAmount = req.maxAmount();
        entity.triggerPeriod = req.triggerPeriod();
        entity.strategyPath = req.strategyPath();
        entity.useOrderBook = req.useOrderBook();
        entity.enabled = false;
        entity.status = "stopped";
        entity.createdAt = Instant.now();
        entity.updatedAt = Instant.now();
        entity.indicators = new ArrayList<>(req.indicators());
        validatePeriods(entity.triggerPeriod, entity.indicators);
        strategyStore.put(entity.id, entity);
        return entity;
    }

    public StrategyEntity update(String id, StrategyRequest req) {
        if (!id.equals(req.id())) {
            throw new IllegalArgumentException("路径ID与请求体ID不一致");
        }

        StrategyEntity old = getById(id);
        old.name = req.name();
        old.symbol = req.symbol();
        old.stopLossPct = req.stopLossPct();
        old.takeProfitPct = req.takeProfitPct();
        old.leverage = req.leverage();
        old.maxAmount = req.maxAmount();
        old.triggerPeriod = req.triggerPeriod();
        old.strategyPath = req.strategyPath();
        old.useOrderBook = req.useOrderBook();
        old.indicators = new ArrayList<>(req.indicators());
        validatePeriods(old.triggerPeriod, old.indicators);
        old.updatedAt = Instant.now();
        return old;
    }

    public void delete(String id) {
        strategyStore.remove(id);
    }

    public StrategyEntity getById(String id) {
        StrategyEntity entity = strategyStore.get(id);
        if (entity == null) {
            throw new IllegalArgumentException("策略不存在: " + id);
        }
        return entity;
    }

    public List<StrategyEntity> list() {
        return strategyStore.values().stream().toList();
    }

    public void setEnabled(String id, boolean enabled, String status) {
        StrategyEntity entity = getById(id);
        entity.enabled = enabled;
        entity.status = status;
        entity.updatedAt = Instant.now();
    }

    private void validatePeriods(com.example.quant.model.Period triggerPeriod, List<StrategyIndicator> indicators) {
        if (indicators == null || indicators.isEmpty()) {
            throw new IllegalArgumentException("指标明细不能为空");
        }
        boolean matched = indicators.stream().anyMatch(i -> i.period() == triggerPeriod);
        if (!matched) {
            throw new IllegalArgumentException("主表触发周期必须和明细表至少一条周期一致");
        }
    }
}
