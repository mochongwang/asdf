# 加密货币量化交易系统（Java 21 / Spring Boot 3.1）

这是一个按你给的需求文档实现的**可运行后端骨架**，重点先把分层和可扩展结构搭好。

## 模块划分

1. **取数据层**：`com.example.quant.data`
   - `ApiConcurrencyGuard`：并发 + 频控（普通用户友好）。
   - `BinanceRestClientImpl`：调用币安公开 REST 接口。
   - `MarketDataService`：行情读取与短缓存。

2. **策略层**：`com.example.quant.strategy`
   - `StrategyTemplate`：策略模板接口。
   - `DemoMaStrategy`：示例策略（可直接改造成你的策略）。
   - `StrategyEngineService`：策略启停、触发、信号分发。

3. **下单层**：`com.example.quant.order`
   - `OrderService`：统一下单入口（当前先内存记录，后续可接真实币安下单）。

## Web 页面

- 访问 `/` 可看到示例页面。
- 页面里的关键输入尽量用下拉框（交易对、周期、策略模板等）。
- 前端可选项来自 `/api/options`。

## 运行

```bash
mvn spring-boot:run
```

## 后续建议（下一步可继续开发）

- 接入 DuckDB 持久化（策略表、指标表、订单表）。
- 补齐 WebSocket（K线、订单流、账户流）。
- 完整回测引擎（HC_ 缓存前缀）。
- 登录页和权限控制。
