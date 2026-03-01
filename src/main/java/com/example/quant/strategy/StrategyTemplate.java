package com.example.quant.strategy;

import com.example.quant.model.PlaceOrderCommand;
import com.example.quant.model.StrategyDefinition;

import java.util.Optional;

/**
 * 策略模板接口。
 *
 * <p>业务方后续新增策略时，直接实现这个接口即可。</p>
 */
public interface StrategyTemplate {

    /**
     * @return 策略路径标识（对应数据库 strategy_path）
     */
    String strategyPath();

    /**
     * 执行一次策略判断。
     *
     * @param definition 策略定义
     * @param context 执行上下文（缓存、行情等）
     * @return 可选下单命令（无信号则 empty）
     */
    Optional<PlaceOrderCommand> evaluate(StrategyDefinition definition, StrategyRuntimeContext context);
}
