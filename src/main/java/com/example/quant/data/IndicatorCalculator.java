package com.example.quant.data;

import com.example.quant.model.KlineCandle;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 指标计算器（基础实现）。
 */
@Component
public class IndicatorCalculator {

    /**
     * 计算简单均线（SMA）。
     */
    public double sma(List<KlineCandle> klines, int period) {
        if (klines == null || klines.size() < period || period <= 0) {
            return Double.NaN;
        }
        double sum = 0;
        for (int i = klines.size() - period; i < klines.size(); i++) {
            sum += klines.get(i).close();
        }
        return sum / period;
    }

    /**
     * 计算 RSI。
     */
    public double rsi(List<KlineCandle> klines, int period) {
        if (klines == null || klines.size() < period + 1 || period <= 0) {
            return Double.NaN;
        }
        double gain = 0;
        double loss = 0;
        for (int i = klines.size() - period; i < klines.size(); i++) {
            double diff = klines.get(i).close() - klines.get(i - 1).close();
            if (diff >= 0) {
                gain += diff;
            } else {
                loss += -diff;
            }
        }
        if (loss == 0) {
            return 100;
        }
        double rs = (gain / period) / (loss / period);
        return 100 - 100 / (1 + rs);
    }

    /**
     * 从简单 JSON 字符串提取整型参数。
     */
    public int readIntParam(String json, String key, int defaultValue) {
        if (json == null || json.isBlank()) {
            return defaultValue;
        }
        String token = "\"" + key + "\":";
        int idx = json.indexOf(token);
        if (idx < 0) {
            return defaultValue;
        }
        int start = idx + token.length();
        int end = start;
        while (end < json.length() && Character.isDigit(json.charAt(end))) {
            end++;
        }
        if (end <= start) {
            return defaultValue;
        }
        return Integer.parseInt(json.substring(start, end));
    }

    /**
     * 按指标定义批量计算（当前支持 MA、RSI）。
     */
    public Map<String, Double> calculateAll(List<KlineCandle> klines, List<com.example.quant.model.StrategyIndicator> indicators) {
        Map<String, Double> map = new HashMap<>();
        if (indicators == null) {
            return map;
        }
        for (var indicator : indicators) {
            switch (indicator.type()) {
                case MA -> {
                    int period = readIntParam(indicator.paramsJson(), "period", 20);
                    map.put(indicator.name(), sma(klines, period));
                }
                case RSI -> {
                    int period = readIntParam(indicator.paramsJson(), "period", 14);
                    map.put(indicator.name(), rsi(klines, period));
                }
                default -> map.put(indicator.name(), Double.NaN);
            }
        }
        return map;
    }
}
