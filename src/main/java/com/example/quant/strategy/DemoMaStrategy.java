package com.example.quant.strategy;

import com.example.quant.data.IndicatorCalculator;
import com.example.quant.model.OrderSide;
import com.example.quant.model.OrderType;
import com.example.quant.model.PlaceOrderCommand;
import com.example.quant.model.StrategyDefinition;
import com.example.quant.model.StrategyIndicator;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * 示例策略模板：ma_cross。
 *
 * <p>规则：短均线向上突破长均线时开多；向下跌破时平多。</p>
 */
@Component
public class DemoMaStrategy implements StrategyTemplate {

    private final IndicatorCalculator indicatorCalculator;

    public DemoMaStrategy(IndicatorCalculator indicatorCalculator) {
        this.indicatorCalculator = indicatorCalculator;
    }

    @Override
    public String strategyPath() {
        return "ma_cross";
    }

    @Override
    public Optional<PlaceOrderCommand> evaluate(StrategyDefinition definition, StrategyRuntimeContext context) {
        if (context.klines() == null || context.klines().size() < 30) {
            return Optional.empty();
        }

        int fast = 7;
        int slow = 25;
        for (StrategyIndicator i : definition.indicators()) {
            if (i.type().name().equals("MA")) {
                fast = indicatorCalculator.readIntParam(i.paramsJson(), "fast", fast);
                slow = indicatorCalculator.readIntParam(i.paramsJson(), "slow", slow);
                break;
            }
        }

        double fastNow = indicatorCalculator.sma(context.klines(), fast);
        double slowNow = indicatorCalculator.sma(context.klines(), slow);

        var previous = context.klines().subList(0, context.klines().size() - 1);
        double fastPrev = indicatorCalculator.sma(previous, fast);
        double slowPrev = indicatorCalculator.sma(previous, slow);

        if (Double.isNaN(fastNow) || Double.isNaN(slowNow) || Double.isNaN(fastPrev) || Double.isNaN(slowPrev)) {
            return Optional.empty();
        }

        // 金叉：开多
        if (fastPrev <= slowPrev && fastNow > slowNow) {
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
                    "MA金叉开多"
            ));
        }

        // 死叉：平多
        if (fastPrev >= slowPrev && fastNow < slowNow) {
            return Optional.of(new PlaceOrderCommand(
                    definition.id(),
                    definition.symbol(),
                    OrderSide.CLOSE_LONG,
                    OrderType.MARKET,
                    10.0,
                    null,
                    5.0,
                    10.0,
                    10,
                    120,
                    "MA死叉平多"
            ));
        }

        return Optional.empty();
    }
}
