package com.example.quant.service;

import com.example.quant.model.LeverageInfo;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;

/**
 * 杠杆服务（DuckDB 持久化）。
 */
@Service
public class LeverageService {

    private final JdbcTemplate jdbcTemplate;

    public LeverageService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        bootstrapIfEmpty();
    }

    public void put(String symbol, int cross, int isolated) {
        long now = Instant.now().toEpochMilli();
        int updated = jdbcTemplate.update(
                """
                UPDATE leverage_info
                SET cross_leverage=?, isolated_leverage=?, created_at=?
                WHERE symbol=?
                """,
                cross,
                isolated,
                now,
                symbol
        );
        if (updated == 0) {
            jdbcTemplate.update(
                    """
                    INSERT INTO leverage_info(symbol,cross_leverage,isolated_leverage,created_at)
                    VALUES (?,?,?,?)
                    """,
                    symbol,
                    cross,
                    isolated,
                    now
            );
        }
    }

    public List<LeverageInfo> list() {
        return jdbcTemplate.query(
                "SELECT symbol,cross_leverage,isolated_leverage,created_at FROM leverage_info ORDER BY symbol",
                this::mapRow
        );
    }

    private void bootstrapIfEmpty() {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM leverage_info", Integer.class);
        if (count != null && count > 0) {
            return;
        }
        put("BTCUSDT", 10, 10);
        put("ETHUSDT", 10, 10);
        put("BNBUSDT", 8, 8);
    }

    private LeverageInfo mapRow(ResultSet rs, int rowNum) throws SQLException {
        LeverageInfo info = new LeverageInfo();
        info.symbol = rs.getString("symbol");
        info.crossLeverage = rs.getInt("cross_leverage");
        info.isolatedLeverage = rs.getInt("isolated_leverage");
        info.createdAt = Instant.ofEpochMilli(rs.getLong("created_at"));
        return info;
    }
}
