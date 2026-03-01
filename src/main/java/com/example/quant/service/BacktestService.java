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

        double feeRate = req.feeRatePctOrDefault() / 100.0;
        double slippageRate = req.slippagePctOrDefault() / 100.0;
        int latencyBars = req.latencyBarsOrDefault();

        List<BacktestOrderResult> details = new ArrayList<>();
        double totalPnl = 0;
        double totalFee = 0;

        boolean inPosition = false;
        double entryPrice = 0;
        int entryIndex = -1;
        int orderIndex = 1;
        double equity = 0;
        double peakEquity = 0;
        double maxDrawdown = 0;
        int wins = 0;

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
                    String reason = takeProfitHit ? "TAKE_PROFIT" : (stopLossHit ? "STOP_LOSS" : "TIMEOUT");
                    double targetExit = takeProfitHit ? tpPrice : (stopLossHit ? slPrice : candle.close());
                    TradeSettlement settlement = settleTrade(req.testAmount(), entryPrice, targetExit, feeRate, slippageRate);
                    totalPnl += settlement.netPnl;
                    totalFee += settlement.fee;
                    equity += settlement.netPnl;
                    peakEquity = Math.max(peakEquity, equity);
                    maxDrawdown = Math.max(maxDrawdown, peakEquity - equity);
                    if (settlement.netPnl > 0) {
                        wins++;
                    }
                    details.add(new BacktestOrderResult(
                            orderIndex++,
                            settlement.executedEntry,
                            settlement.executedExit,
                            settlement.netPnl,
                            settlement.fee,
                            reason,
                            i - entryIndex
                    ));
                    inPosition = false;
                    continue;
                }
            }

            double fastPrev = smaClose(klines, i - 1, 5);
            double fastNow = smaClose(klines, i, 5);
            double slowPrev = smaClose(klines, i - 1, 20);
            double slowNow = smaClose(klines, i, 20);

            if (!inPosition && fastPrev <= slowPrev && fastNow > slowNow) {
                int entryExecIndex = Math.min(i + latencyBars, klines.size() - 1);
                entryPrice = klines.get(entryExecIndex).close();
                inPosition = true;
                entryIndex = entryExecIndex;
            } else if (inPosition && fastPrev >= slowPrev && fastNow < slowNow) {
                int exitExecIndex = Math.min(i + latencyBars, klines.size() - 1);
                double rawExitPrice = klines.get(exitExecIndex).close();
                TradeSettlement settlement = settleTrade(req.testAmount(), entryPrice, rawExitPrice, feeRate, slippageRate);
                totalPnl += settlement.netPnl;
                totalFee += settlement.fee;
                equity += settlement.netPnl;
                peakEquity = Math.max(peakEquity, equity);
                maxDrawdown = Math.max(maxDrawdown, peakEquity - equity);
                if (settlement.netPnl > 0) {
                    wins++;
                }
                details.add(new BacktestOrderResult(
                        orderIndex++,
                        settlement.executedEntry,
                        settlement.executedExit,
                        settlement.netPnl,
                        settlement.fee,
                        "MA_CROSS_EXIT",
                        Math.max(1, exitExecIndex - entryIndex)
                ));
                inPosition = false;
            }
        }

        if (inPosition) {
            double rawExitPrice = klines.get(klines.size() - 1).close();
            TradeSettlement settlement = settleTrade(req.testAmount(), entryPrice, rawExitPrice, feeRate, slippageRate);
            totalPnl += settlement.netPnl;
            totalFee += settlement.fee;
            equity += settlement.netPnl;
            peakEquity = Math.max(peakEquity, equity);
            maxDrawdown = Math.max(maxDrawdown, peakEquity - equity);
            if (settlement.netPnl > 0) {
                wins++;
            }
            details.add(new BacktestOrderResult(
                    orderIndex,
                    settlement.executedEntry,
                    settlement.executedExit,
                    settlement.netPnl,
                    settlement.fee,
                    "FORCE_CLOSE",
                    Math.max(1, klines.size() - 1 - entryIndex)
            ));
        }

        double winRate = details.isEmpty() ? 0 : wins * 1.0 / details.size();
        return new BacktestSummary(strategy.id, details.size(), totalPnl, totalFee, winRate, maxDrawdown, details);
    }

    private TradeSettlement settleTrade(double notional,
                                        double rawEntry,
                                        double rawExit,
                                        double feeRate,
                                        double slippageRate) {
        double executedEntry = rawEntry * (1 + slippageRate);
        double executedExit = rawExit * (1 - slippageRate);
        double grossPnl = notional * ((executedExit - executedEntry) / executedEntry);
        double fee = notional * feeRate * 2;
        double netPnl = grossPnl - fee;
        return new TradeSettlement(executedEntry, executedExit, fee, netPnl);
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

    private record TradeSettlement(double executedEntry, double executedExit, double fee, double netPnl) {
    }
}
