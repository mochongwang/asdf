package com.example.quant.data;

import com.example.quant.model.IndicatorType;
import com.example.quant.model.KlineCandle;
import com.example.quant.model.Period;
import com.example.quant.model.StrategyIndicator;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;

class IndicatorCalculatorTest {

    private final IndicatorCalculator calculator = new IndicatorCalculator();

    @Test
    void shouldCalculateBollKdjCci() {
        List<KlineCandle> klines = buildKlines(80);
        List<StrategyIndicator> indicators = List.of(
                new StrategyIndicator("boll_middle", IndicatorType.BOLL, "{\"period\":20,\"line\":\"middle\"}", Period.M15),
                new StrategyIndicator("kdj_j", IndicatorType.KDJ, "{\"fastK\":9,\"slowK\":3,\"slowD\":3,\"line\":\"j\"}", Period.M15),
                new StrategyIndicator("cci_14", IndicatorType.CCI, "{\"period\":14}", Period.M15)
        );

        Map<String, Double> result = calculator.calculateAll(klines, indicators);

        assertFalse(result.get("boll_middle").isNaN());
        assertFalse(result.get("kdj_j").isNaN());
        assertFalse(result.get("cci_14").isNaN());
    }

    private List<KlineCandle> buildKlines(int size) {
        List<KlineCandle> list = new ArrayList<>();
        long start = 1_700_000_000_000L;
        for (int i = 0; i < size; i++) {
            double base = 100 + i * 0.8;
            list.add(new KlineCandle(
                    start + i * 60_000L,
                    base - 0.5,
                    base + 1.2,
                    base - 1.1,
                    base,
                    1000 + i,
                    start + (i + 1) * 60_000L - 1
            ));
        }
        return list;
    }
}
