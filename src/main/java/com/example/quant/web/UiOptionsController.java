package com.example.quant.web;

import com.example.quant.model.IndicatorType;
import com.example.quant.model.OrderType;
import com.example.quant.model.Period;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 前端可选项接口。
 *
 * <p>用于把“尽量可选，不手填”落地，前端下拉框直接读取此接口。</p>
 */
@RestController
@RequestMapping("/api/options")
public class UiOptionsController {

    /**
     * 返回页面所有常见选项。
     *
     * @return 选项集合
     */
    @GetMapping
    public Map<String, Object> options() {
        List<String> periodOptions = List.of(Period.values()).stream().map(Period::code).toList();
        List<String> indicatorOptions = List.of(IndicatorType.values()).stream().map(Enum::name).toList();
        List<String> orderTypeOptions = List.of(OrderType.values()).stream().map(Enum::name).toList();
        List<String> symbolOptions = List.of("BTCUSDT", "ETHUSDT", "BNBUSDT", "SOLUSDT");
        List<String> strategyPathOptions = List.of("ma_cross");

        return Map.of(
                "periods", periodOptions,
                "indicators", indicatorOptions,
                "orderTypes", orderTypeOptions,
                "symbols", symbolOptions,
                "strategyPaths", strategyPathOptions,
                "boolOptions", List.of(true, false)
        );
    }
}
