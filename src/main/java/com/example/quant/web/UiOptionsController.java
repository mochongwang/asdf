package com.example.quant.web;

import com.example.quant.model.IndicatorType;
import com.example.quant.model.OrderSide;
import com.example.quant.model.OrderType;
import com.example.quant.model.Period;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 前端可选项接口。
 */
@RestController
@RequestMapping("/api/options")
public class UiOptionsController {

    @GetMapping
    public Map<String, Object> options() {
        return Map.of(
                "periods", List.of(Period.values()).stream().map(Period::code).toList(),
                "periodEnums", List.of(Period.values()).stream().map(Enum::name).toList(),
                "indicators", List.of(IndicatorType.values()).stream().map(Enum::name).toList(),
                "orderTypes", List.of(OrderType.values()).stream().map(Enum::name).toList(),
                "orderSides", List.of(OrderSide.values()).stream().map(Enum::name).toList(),
                "symbols", List.of("BTCUSDT", "ETHUSDT", "BNBUSDT", "SOLUSDT"),
                "strategyPaths", List.of("ma_cross"),
                "boolOptions", List.of(true, false)
        );
    }
}
