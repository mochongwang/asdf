package com.example.quant.data;

import com.example.quant.model.KlineCandle;
import com.example.quant.model.StrategyIndicator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tictactec.ta.lib.Core;
import com.tictactec.ta.lib.MAType;
import com.tictactec.ta.lib.MInteger;
import com.tictactec.ta.lib.RetCode;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 指标计算器（使用 TA-Lib Core）。
 */
@Component
public class IndicatorCalculator {

    private final Core ta = new Core();
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 从 JSON 参数读取整型。
     */
    public int readIntParam(String json, String key, int defaultValue) {
        try {
            if (json == null || json.isBlank()) {
                return defaultValue;
            }
            Map<?, ?> map = objectMapper.readValue(json, Map.class);
            Object v = map.get(key);
            if (v == null) {
                return defaultValue;
            }
            return Integer.parseInt(String.valueOf(v));
        } catch (Exception e) {
            return defaultValue;
        }
    }

    /**
     * 从 JSON 参数读取浮点数。
     */
    public double readDoubleParam(String json, String key, double defaultValue) {
        try {
            if (json == null || json.isBlank()) {
                return defaultValue;
            }
            Map<?, ?> map = objectMapper.readValue(json, Map.class);
            Object v = map.get(key);
            if (v == null) {
                return defaultValue;
            }
            return Double.parseDouble(String.valueOf(v));
        } catch (Exception e) {
            return defaultValue;
        }
    }

    /**
     * 从 JSON 参数读取字符串。
     */
    public String readStringParam(String json, String key, String defaultValue) {
        try {
            if (json == null || json.isBlank()) {
                return defaultValue;
            }
            Map<?, ?> map = objectMapper.readValue(json, Map.class);
            Object v = map.get(key);
            if (v == null) {
                return defaultValue;
            }
            String text = String.valueOf(v).trim();
            return text.isBlank() ? defaultValue : text;
        } catch (Exception e) {
            return defaultValue;
        }
    }

    /**
     * 按指标定义批量计算（核心指标使用 TA-Lib）。
     */
    public Map<String, Double> calculateAll(List<KlineCandle> klines, List<StrategyIndicator> indicators) {
        Map<String, Double> map = new HashMap<>();
        if (indicators == null || klines == null || klines.isEmpty()) {
            return map;
        }

        double[] close = extractClose(klines);
        double[] high = extractHigh(klines);
        double[] low = extractLow(klines);
        double[] volume = extractVolume(klines);

        for (StrategyIndicator indicator : indicators) {
            double value;
            switch (indicator.type()) {
                case MA -> {
                    int period = readIntParam(indicator.paramsJson(), "period", 20);
                    value = lastMa(close, period);
                }
                case RSI -> {
                    int period = readIntParam(indicator.paramsJson(), "period", 14);
                    value = lastRsi(close, period);
                }
                case MACD -> {
                    int fast = readIntParam(indicator.paramsJson(), "fast", 12);
                    int slow = readIntParam(indicator.paramsJson(), "slow", 26);
                    int signal = readIntParam(indicator.paramsJson(), "signal", 9);
                    value = lastMacdHistogram(close, fast, slow, signal);
                }
                case ATR -> {
                    int period = readIntParam(indicator.paramsJson(), "period", 14);
                    value = lastAtr(high, low, close, period);
                }
                case BOLL -> {
                    int period = readIntParam(indicator.paramsJson(), "period", 20);
                    double devUp = readDoubleParam(indicator.paramsJson(), "devUp", 2.0);
                    double devDown = readDoubleParam(indicator.paramsJson(), "devDown", 2.0);
                    String line = readStringParam(indicator.paramsJson(), "line", "middle");
                    value = lastBoll(close, period, devUp, devDown, line);
                }
                case KDJ -> {
                    int fastK = readIntParam(indicator.paramsJson(), "fastK", 9);
                    int slowK = readIntParam(indicator.paramsJson(), "slowK", 3);
                    int slowD = readIntParam(indicator.paramsJson(), "slowD", 3);
                    String line = readStringParam(indicator.paramsJson(), "line", "j");
                    value = lastKdj(high, low, close, fastK, slowK, slowD, line);
                }
                case CCI -> {
                    int period = readIntParam(indicator.paramsJson(), "period", 14);
                    value = lastCci(high, low, close, period);
                }
                case VWAP -> value = lastVwap(close, volume);
                case VOLUME -> value = volume[volume.length - 1];
                default -> value = Double.NaN;
            }
            map.put(indicator.name(), value);
        }
        return map;
    }


    /**
     * 兼容旧代码：直接计算SMA。
     */
    public double sma(List<KlineCandle> klines, int period) {
        return lastMa(extractClose(klines), period);
    }

    /**
     * 兼容旧代码：直接计算RSI。
     */
    public double rsi(List<KlineCandle> klines, int period) {
        return lastRsi(extractClose(klines), period);
    }

    private double lastMa(double[] close, int period) {
        if (close.length < period || period <= 0) {
            return Double.NaN;
        }
        MInteger outBegIdx = new MInteger();
        MInteger outNbElement = new MInteger();
        double[] out = new double[close.length];
        RetCode code = ta.movingAverage(0, close.length - 1, close, period, MAType.Sma, outBegIdx, outNbElement, out);
        if (code != RetCode.Success || outNbElement.value <= 0) {
            return Double.NaN;
        }
        return out[outBegIdx.value + outNbElement.value - 1];
    }

    private double lastRsi(double[] close, int period) {
        if (close.length < period + 1 || period <= 0) {
            return Double.NaN;
        }
        MInteger outBegIdx = new MInteger();
        MInteger outNbElement = new MInteger();
        double[] out = new double[close.length];
        RetCode code = ta.rsi(0, close.length - 1, close, period, outBegIdx, outNbElement, out);
        if (code != RetCode.Success || outNbElement.value <= 0) {
            return Double.NaN;
        }
        return out[outBegIdx.value + outNbElement.value - 1];
    }

    private double lastMacdHistogram(double[] close, int fast, int slow, int signal) {
        if (close.length < slow + signal) {
            return Double.NaN;
        }
        MInteger outBegIdx = new MInteger();
        MInteger outNbElement = new MInteger();
        double[] macd = new double[close.length];
        double[] sig = new double[close.length];
        double[] hist = new double[close.length];
        RetCode code = ta.macd(0, close.length - 1, close, fast, slow, signal, outBegIdx, outNbElement, macd, sig, hist);
        if (code != RetCode.Success || outNbElement.value <= 0) {
            return Double.NaN;
        }
        return hist[outBegIdx.value + outNbElement.value - 1];
    }

    private double lastAtr(double[] high, double[] low, double[] close, int period) {
        if (close.length < period + 1 || high.length != low.length || low.length != close.length) {
            return Double.NaN;
        }
        MInteger outBegIdx = new MInteger();
        MInteger outNbElement = new MInteger();
        double[] out = new double[close.length];
        RetCode code = ta.atr(0, close.length - 1, high, low, close, period, outBegIdx, outNbElement, out);
        if (code != RetCode.Success || outNbElement.value <= 0) {
            return Double.NaN;
        }
        return out[outBegIdx.value + outNbElement.value - 1];
    }

    private double lastBoll(double[] close, int period, double devUp, double devDown, String line) {
        if (close.length < period || period <= 0) {
            return Double.NaN;
        }
        MInteger outBegIdx = new MInteger();
        MInteger outNbElement = new MInteger();
        double[] upper = new double[close.length];
        double[] middle = new double[close.length];
        double[] lower = new double[close.length];
        RetCode code = ta.bbands(
                0,
                close.length - 1,
                close,
                period,
                devUp,
                devDown,
                MAType.Sma,
                outBegIdx,
                outNbElement,
                upper,
                middle,
                lower
        );
        if (code != RetCode.Success || outNbElement.value <= 0) {
            return Double.NaN;
        }
        int idx = outBegIdx.value + outNbElement.value - 1;
        String key = line == null ? "middle" : line.toLowerCase();
        return switch (key) {
            case "upper", "up" -> upper[idx];
            case "lower", "down" -> lower[idx];
            default -> middle[idx];
        };
    }

    private double lastKdj(double[] high, double[] low, double[] close, int fastK, int slowK, int slowD, String line) {
        if (high.length != low.length || low.length != close.length || close.length < fastK || fastK <= 0) {
            return Double.NaN;
        }
        MInteger outBegIdx = new MInteger();
        MInteger outNbElement = new MInteger();
        double[] k = new double[close.length];
        double[] d = new double[close.length];
        RetCode code = ta.stoch(
                0,
                close.length - 1,
                high,
                low,
                close,
                fastK,
                slowK,
                MAType.Sma,
                slowD,
                MAType.Sma,
                outBegIdx,
                outNbElement,
                k,
                d
        );
        if (code != RetCode.Success || outNbElement.value <= 0) {
            return Double.NaN;
        }
        int idx = outBegIdx.value + outNbElement.value - 1;
        double kVal = k[idx];
        double dVal = d[idx];
        String key = line == null ? "j" : line.toLowerCase();
        return switch (key) {
            case "k" -> kVal;
            case "d" -> dVal;
            default -> 3 * kVal - 2 * dVal;
        };
    }

    private double lastCci(double[] high, double[] low, double[] close, int period) {
        if (high.length != low.length || low.length != close.length || close.length < period || period <= 0) {
            return Double.NaN;
        }
        MInteger outBegIdx = new MInteger();
        MInteger outNbElement = new MInteger();
        double[] out = new double[close.length];
        RetCode code = ta.cci(0, close.length - 1, high, low, close, period, outBegIdx, outNbElement, out);
        if (code != RetCode.Success || outNbElement.value <= 0) {
            return Double.NaN;
        }
        return out[outBegIdx.value + outNbElement.value - 1];
    }

    private double lastVwap(double[] close, double[] volume) {
        if (close.length == 0 || close.length != volume.length) {
            return Double.NaN;
        }
        double totalPv = 0;
        double totalV = 0;
        for (int i = 0; i < close.length; i++) {
            totalPv += close[i] * volume[i];
            totalV += volume[i];
        }
        if (totalV == 0) {
            return Double.NaN;
        }
        return totalPv / totalV;
    }

    private double[] extractClose(List<KlineCandle> klines) {
        double[] arr = new double[klines.size()];
        for (int i = 0; i < klines.size(); i++) {
            arr[i] = klines.get(i).close();
        }
        return arr;
    }

    private double[] extractHigh(List<KlineCandle> klines) {
        double[] arr = new double[klines.size()];
        for (int i = 0; i < klines.size(); i++) {
            arr[i] = klines.get(i).high();
        }
        return arr;
    }

    private double[] extractLow(List<KlineCandle> klines) {
        double[] arr = new double[klines.size()];
        for (int i = 0; i < klines.size(); i++) {
            arr[i] = klines.get(i).low();
        }
        return arr;
    }

    private double[] extractVolume(List<KlineCandle> klines) {
        double[] arr = new double[klines.size()];
        for (int i = 0; i < klines.size(); i++) {
            arr[i] = klines.get(i).volume();
        }
        return arr;
    }
}
