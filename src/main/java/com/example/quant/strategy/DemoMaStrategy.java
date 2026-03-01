package com.example.quant.strategy;

import com.example.quant.model.OrderSide;
import com.example.quant.model.OrderType;
import com.example.quant.model.PlaceOrderCommand;
import com.example.quant.model.StrategyDefinition;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * 示例策略模板：ma_cross。
 *
 * <p>这里只演示模板结构，具体指标计算由你后续按业务补充。</p>
 */
@Component
public class DemoMaStrategy implements StrategyTemplate {

    @Override
    public String strategyPath() {
        return "ma_cross";
    }

    @Override
    public Optional<PlaceOrderCommand> evaluate(StrategyDefinition definition, StrategyRuntimeContext context) {
        // 这里演示最简单规则：当价格字段存在时，构建一个“空信号”（不下单）。
        Object priceObj = context.latestTicker().get("price");
        if (priceObj == null) {
            return Optional.empty();
        }

        // TODO: 在这里按你的指标逻辑替换。
        return Optional.of(new PlaceOrderCommand(
                definition.id(),
                definition.symbol(),
                OrderSide.OPEN_LONG,
                OrderType.MARKET,
                10.0,
                null,
                5.0,
                10.0,
                10,
                120,
                "模板信号-请替换为真实策略"
        ));
    }
}
