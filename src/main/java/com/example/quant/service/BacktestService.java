package com.example.quant.service;

import com.example.quant.model.BacktestOrderResult;
import com.example.quant.model.BacktestRequest;
import com.example.quant.model.BacktestSummary;
import com.example.quant.model.StrategyEntity;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 回测服务（模拟实现）。
 */
@Service
public class BacktestService {

    public BacktestSummary runBacktest(StrategyEntity strategy, BacktestRequest req) {
        if (req.endTime().isBefore(req.startTime()) || req.endTime().equals(req.startTime())) {
            throw new IllegalArgumentException("回测结束时间必须晚于开始时间");
        }

        long minutes = Duration.between(req.startTime(), req.endTime()).toMinutes();
        int orderCount = (int) Math.max(1, Math.min(200, minutes / 30));

        List<BacktestOrderResult> details = new ArrayList<>();
        double totalPnl = 0;
        for (int i = 1; i <= orderCount; i++) {
            double entry = 100 + ThreadLocalRandom.current().nextDouble(0, 200);
            double pct = ThreadLocalRandom.current().nextDouble(-0.03, 0.05);
            double exit = entry * (1 + pct);
            double pnl = req.testAmount() * pct;
            totalPnl += pnl;
            details.add(new BacktestOrderResult(i, entry, exit, pnl));
        }

        return new BacktestSummary(strategy.id, orderCount, totalPnl, details);
    }
}
