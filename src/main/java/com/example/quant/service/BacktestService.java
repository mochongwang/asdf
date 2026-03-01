package com.example.quant.service;

import com.example.quant.data.MarketDataService;
import com.example.quant.model.BacktestOrderResult;
import com.example.quant.model.BacktestRequest;
import com.example.quant.model.BacktestSummary;
import com.example.quant.model.KlineCandle;
import com.example.quant.model.StrategyEntity;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 回测服务（基于历史K线回放 + 止盈止损/均线信号）。
 */
@Service
public class BacktestService {

    private static final int DEFAULT_TIMEOUT_BARS = 20;

    private final MarketDataService marketDataService;

    public BacktestService(MarketDataService marketDataService) {
        this.marketDataService = marketDataService;
    }

    public BacktestSummary runBacktest(StrategyEntity strategy, BacktestRequest req) {
        if (req.endTime().isBefore(req.startTime()) || req.endTime().equals(req.startTime())) {
            throw new IllegalArgumentException("回测结束时间必须晚于开始时间");
        }

        String namespace = "HC_" + strategy.id;
        List<KlineCandle> source = marketDataService.latestKlinesWithNamespace(namespace, strategy.symbol, strategy.triggerPeriod.code(), 500);
        // 回测时先在 HC_ 命名空间计算一次指标，避免污染实时缓存。
        marketDataService.calculateIndicatorsWithNamespace(namespace, strategy.symbol, strategy.triggerPeriod.code(), strategy.indicators);
        List<KlineCandle> klines = source.stream()
                .filter(k -> k.openTime() >= req.startTime().toEpochMilli() && k.closeTime() <= req.endTime().toEpochMilli())
                .sorted(Comparator.comparingLong(KlineCandle::openTime))
                .toList();

        if (klines.size() < 40) {
            throw new IllegalArgumentException("回测区间内K线不足，至少需要40根");
        }

        List<BacktestOrderResult> details = new ArrayList<>();
        double totalPnl = 0;

        boolean inPosition = false;
        double entryPrice = 0;
        int entryIndex = -1;
        int orderIndex = 1;

        double stopLossPct = Math.max(0, strategy.stopLossPct) / 100.0;
        double takeProfitPct = Math.max(0, strategy.takeProfitPct) / 100.0;

        for (int i = 35; i < klines.size(); i++) {
            KlineCandle candle = klines.get(i);

            if (inPosition) {
                double tpPrice = entryPrice * (1 + takeProfitPct);
                double slPrice = entryPrice * (1 - stopLossPct);
                boolean takeProfitHit = takeProfitPct > 0 && candle.high() >= tpPrice;
                boolean stopLossHit = stopLossPct > 0 && candle.low() <= slPrice;
                boolean timeoutHit = (i - entryIndex) >= DEFAULT_TIMEOUT_BARS;

                if (takeProfitHit || stopLossHit || timeoutHit) {
                    double exitPrice = takeProfitHit ? tpPrice : (stopLossHit ? slPrice : candle.close());
                    double pnl = req.testAmount() * ((exitPrice - entryPrice) / entryPrice);
                    totalPnl += pnl;
                    details.add(new BacktestOrderResult(orderIndex++, entryPrice, exitPrice, pnl));
                    inPosition = false;
                    continue;
                }
            }

            double fastPrev = smaClose(klines, i - 1, 5);
            double fastNow = smaClose(klines, i, 5);
            double slowPrev = smaClose(klines, i - 1, 20);
            double slowNow = smaClose(klines, i, 20);

            if (!inPosition && fastPrev <= slowPrev && fastNow > slowNow) {
                inPosition = true;
                entryPrice = candle.close();
                entryIndex = i;
            } else if (inPosition && fastPrev >= slowPrev && fastNow < slowNow) {
                double exitPrice = candle.close();
                double pnl = req.testAmount() * ((exitPrice - entryPrice) / entryPrice);
                totalPnl += pnl;
                details.add(new BacktestOrderResult(orderIndex++, entryPrice, exitPrice, pnl));
                inPosition = false;
            }
        }

        if (inPosition) {
            double exitPrice = klines.get(klines.size() - 1).close();
            double pnl = req.testAmount() * ((exitPrice - entryPrice) / entryPrice);
            totalPnl += pnl;
            details.add(new BacktestOrderResult(orderIndex, entryPrice, exitPrice, pnl));
        }

        return new BacktestSummary(strategy.id, details.size(), totalPnl, details);
    }

    private double smaClose(List<KlineCandle> klines, int endIndex, int period) {
        int start = endIndex - period + 1;
        if (start < 0) {
            return Double.NaN;
        }
        double sum = 0;
        for (int i = start; i <= endIndex; i++) {
            sum += klines.get(i).close();
        }
        return sum / period;
    }
}
