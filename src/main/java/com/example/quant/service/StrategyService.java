package com.example.quant.service;

import com.example.quant.model.StrategyEntity;
import com.example.quant.model.StrategyIndicator;
import com.example.quant.web.StrategyRequest;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;

/**
 * 策略 CRUD 服务（DuckDB 持久化）。
 */
@Service
public class StrategyService {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public StrategyService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public StrategyEntity create(StrategyRequest req) {
        if (exists(req.id())) {
            throw new IllegalArgumentException("策略ID已存在: " + req.id());
        }

        StrategyEntity entity = fromRequest(req);
        entity.enabled = false;
        entity.status = "stopped";
        entity.createdAt = Instant.now();
        entity.updatedAt = Instant.now();
        validatePeriods(entity.triggerPeriod, entity.indicators);
        insert(entity);
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
        old.indicators = req.indicators();
        validatePeriods(old.triggerPeriod, old.indicators);
        old.updatedAt = Instant.now();
        updateRow(old);
        return old;
    }

    public void delete(String id) {
        jdbcTemplate.update("DELETE FROM strategies WHERE id = ?", id);
    }

    public StrategyEntity getById(String id) {
        List<StrategyEntity> list = jdbcTemplate.query(
                "SELECT * FROM strategies WHERE id = ?",
                this::mapRow,
                id
        );
        if (list.isEmpty()) {
            throw new IllegalArgumentException("策略不存在: " + id);
        }
        return list.get(0);
    }

    public List<StrategyEntity> list() {
        return jdbcTemplate.query("SELECT * FROM strategies ORDER BY updated_at DESC", this::mapRow);
    }

    public void setEnabled(String id, boolean enabled, String status) {
        StrategyEntity entity = getById(id);
        entity.enabled = enabled;
        entity.status = status;
        entity.updatedAt = Instant.now();
        updateRow(entity);
    }

    private boolean exists(String id) {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM strategies WHERE id = ?", Integer.class, id);
        return count != null && count > 0;
    }

    private void insert(StrategyEntity e) {
        jdbcTemplate.update("""
                INSERT INTO strategies(
                    id,name,symbol,stop_loss_pct,take_profit_pct,leverage,max_amount,
                    trigger_period,use_orderbook,strategy_path,enabled,status,indicators_json,created_at,updated_at
                ) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
                """,
                e.id,
                e.name,
                e.symbol,
                e.stopLossPct,
                e.takeProfitPct,
                e.leverage,
                e.maxAmount,
                e.triggerPeriod.name(),
                e.useOrderBook,
                e.strategyPath,
                e.enabled,
                e.status,
                toJson(e.indicators),
                e.createdAt.toEpochMilli(),
                e.updatedAt.toEpochMilli()
        );
    }

    private void updateRow(StrategyEntity e) {
        jdbcTemplate.update("""
                UPDATE strategies SET
                    name=?,symbol=?,stop_loss_pct=?,take_profit_pct=?,leverage=?,max_amount=?,
                    trigger_period=?,use_orderbook=?,strategy_path=?,enabled=?,status=?,indicators_json=?,updated_at=?
                WHERE id=?
                """,
                e.name,
                e.symbol,
                e.stopLossPct,
                e.takeProfitPct,
                e.leverage,
                e.maxAmount,
                e.triggerPeriod.name(),
                e.useOrderBook,
                e.strategyPath,
                e.enabled,
                e.status,
                toJson(e.indicators),
                e.updatedAt.toEpochMilli(),
                e.id
        );
    }

    private StrategyEntity mapRow(ResultSet rs, int rowNum) throws SQLException {
        StrategyEntity e = new StrategyEntity();
        e.id = rs.getString("id");
        e.name = rs.getString("name");
        e.symbol = rs.getString("symbol");
        e.stopLossPct = rs.getDouble("stop_loss_pct");
        e.takeProfitPct = rs.getDouble("take_profit_pct");
        e.leverage = rs.getInt("leverage");
        e.maxAmount = rs.getInt("max_amount");
        e.triggerPeriod = com.example.quant.model.Period.valueOf(rs.getString("trigger_period"));
        e.useOrderBook = rs.getBoolean("use_orderbook");
        e.strategyPath = rs.getString("strategy_path");
        e.enabled = rs.getBoolean("enabled");
        e.status = rs.getString("status");
        e.indicators = fromJson(rs.getString("indicators_json"));
        e.createdAt = Instant.ofEpochMilli(rs.getLong("created_at"));
        e.updatedAt = Instant.ofEpochMilli(rs.getLong("updated_at"));
        return e;
    }

    private StrategyEntity fromRequest(StrategyRequest req) {
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
        entity.indicators = req.indicators();
        return entity;
    }

    private String toJson(List<StrategyIndicator> indicators) {
        try {
            return objectMapper.writeValueAsString(indicators == null ? List.of() : indicators);
        } catch (Exception e) {
            throw new IllegalStateException("策略指标序列化失败", e);
        }
    }

    private List<StrategyIndicator> fromJson(String json) {
        try {
            if (json == null || json.isBlank()) {
                return List.of();
            }
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            throw new IllegalStateException("策略指标反序列化失败", e);
        }
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
