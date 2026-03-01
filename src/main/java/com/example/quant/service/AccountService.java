package com.example.quant.service;

import com.example.quant.model.AccountInfo;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;

/**
 * 账户服务（DuckDB 持久化）。
 */
@Service
public class AccountService {

    private final JdbcTemplate jdbcTemplate;

    public AccountService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        bootstrapIfEmpty();
    }

    public List<AccountInfo> list() {
        return jdbcTemplate.query(
                "SELECT apikey,balance,available_balance,frozen_balance,updated_at FROM accounts ORDER BY updated_at DESC",
                this::mapRow
        );
    }

    private void bootstrapIfEmpty() {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM accounts", Integer.class);
        if (count != null && count > 0) {
            return;
        }
        jdbcTemplate.update(
                """
                INSERT INTO accounts(apikey,balance,available_balance,frozen_balance,updated_at)
                VALUES (?,?,?,?,?)
                """,
                "demo-api-key",
                10000.0,
                9000.0,
                1000.0,
                Instant.now().toEpochMilli()
        );
    }

    private AccountInfo mapRow(ResultSet rs, int rowNum) throws SQLException {
        AccountInfo info = new AccountInfo();
        info.apikey = rs.getString("apikey");
        info.balance = rs.getDouble("balance");
        info.availableBalance = rs.getDouble("available_balance");
        info.frozenBalance = rs.getDouble("frozen_balance");
        info.updatedAt = Instant.ofEpochMilli(rs.getLong("updated_at"));
        return info;
    }
}
