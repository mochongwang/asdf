package com.example.quant.startup;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * DuckDB 表初始化。
 */
@Component
public class DatabaseSchemaInitializer implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    public DatabaseSchemaInitializer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS strategies (
                    id VARCHAR PRIMARY KEY,
                    name VARCHAR NOT NULL,
                    symbol VARCHAR NOT NULL,
                    stop_loss_pct DOUBLE,
                    take_profit_pct DOUBLE,
                    leverage INTEGER,
                    max_amount INTEGER,
                    trigger_period VARCHAR,
                    use_orderbook BOOLEAN,
                    strategy_path VARCHAR,
                    enabled BOOLEAN,
                    status VARCHAR,
                    indicators_json VARCHAR,
                    created_at BIGINT,
                    updated_at BIGINT
                )
                """);
    }
}
