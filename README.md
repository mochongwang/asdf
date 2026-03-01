# 加密货币量化交易系统（Java 21 / Spring Boot 3.1）

这是按你的需求文档实现的**完整演示版**（可运行、可操作、可扩展）。

> 说明：当前版本已把“流程闭环”完整打通（登录、策略CRUD、启停、触发、回测、下单记录、账户/通知/杠杆查询）。
> 真实 Binance 私有 WebSocket 与真实下单因环境与密钥原因默认使用安全的模拟实现，但代码结构已预留扩展位点。

## 模块划分

1. **取数据层**（`com.example.quant.data`）
   - `ApiConcurrencyGuard`：并发和频控控制（普通用户友好）
   - `BinanceRestClientImpl`：币安 REST 调用
   - `MarketDataService`：行情读取+缓存

2. **策略层**（`com.example.quant.strategy`）
   - `StrategyTemplate`：策略模板接口
   - `DemoMaStrategy`：示例策略
   - `StrategyEngineService`：策略启停、触发、下单联动

3. **下单层**（`com.example.quant.order`）
   - `OrderService`：统一下单、强平、分页筛选查询

4. **业务服务层**（`com.example.quant.service`）
   - `StrategyService`：策略主表+指标明细（内存版）CRUD和校验
   - `BacktestService`：回测逻辑
   - `NotificationService`：通知日志
   - `AccountService`：账户查询
   - `LeverageService`：杠杆信息

5. **Web层**（`com.example.quant.web`）
   - 登录：`/api/auth/login`（写死账号密码：`admin` / `123456`）
   - 策略：`/api/strategies`（CRUD、启停、触发、回测）
   - 订单：`/api/orders`（分页+筛选）
   - 账户：`/api/accounts`
   - 通知：`/api/notifications`
   - 杠杆：`/api/leverage`
   - 可选项：`/api/options`
   - 页面：`/`

## 页面能力

- 登录
- 创建策略（下拉选项优先，减少手工输入错误）
- 启用/禁用/触发策略
- 回测
- 订单/账户/通知/杠杆查询

## 运行

```bash
mvn spring-boot:run
```

浏览器访问：`http://localhost:8080/`

## 下一步（生产化）

1. 把内存存储切换为 DuckDB 持久化
2. 接入 Binance 私有 websocket（账户流、订单流）
3. 接入 K线 websocket + 历史K线预加载 + 指标缓存
4. 对接真实下单与风控
5. 企业微信 webhook 实发通知
