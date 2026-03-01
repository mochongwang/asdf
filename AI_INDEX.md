# AI_INDEX.md

> 面向 AI 助手的项目索引文件（用于后续任务快速理解上下文）。

## 1. 项目目标
- 项目名称：`crypto-quant-system`
- 技术栈：Java 21 + Spring Boot 3.1
- 目标：币安量化交易系统（当前为“可运行生产基线 + 模拟默认开启”）

## 2. 当前关键实现状态（高优先）
- ✅ **取数据层只用 WebSocket API（ws-api）**：`ticker.price` + `klines`。
- ✅ **下单层只用 REST API**：真实模式走签名下单。
- ✅ Binance 对接失败策略：**重试1次**，仍失败则**通知并抛错**。
- ✅ 指标计算统一使用 **TA-Lib Core**（`com.tictactec.ta.lib.Core`）。
- ✅ 指标已覆盖：MA / RSI / MACD / ATR / BOLL / KDJ / CCI / VWAP / VOLUME。
- ⚠️ 默认 `app.trading.simulation=true`（模拟下单）。
- ⚠️ 已接入 DuckDB 并持久化策略、订单、通知日志、账户、杠杆（其余能力继续迭代）。
- ⚠️ 回测已升级为基于历史K线的MA交叉模拟（非完整撮合引擎）。
- ✅ 新增 orderbook 深度快照读取（ws-api depth）与前N档查询接口。
- ✅ 新增 orderbook 订阅管理与定时刷新（策略启用/停用自动订阅与释放）。
- ✅ 通知日志支持全字段筛选查询（按需求扩展查询参数）。

## 3. 关键目录索引
- 启动入口：`src/main/java/com/example/quant/QuantApplication.java`
- 配置与鉴权：`src/main/java/com/example/quant/config/`
- 登录鉴权：`src/main/java/com/example/quant/auth/`
- 数据层：`src/main/java/com/example/quant/data/`（含 `OrderBookSubscriptionService` 订阅刷新）
- 策略层：`src/main/java/com/example/quant/strategy/`
- 下单层：`src/main/java/com/example/quant/order/`
- 业务服务：`src/main/java/com/example/quant/service/`
- 通知通道：`src/main/java/com/example/quant/notify/`
- 调度任务：`src/main/java/com/example/quant/scheduler/`
- 启动时序：`src/main/java/com/example/quant/startup/`
- 接口层：`src/main/java/com/example/quant/web/`
- 页面模板：`src/main/resources/templates/index.html`
- 主配置：`src/main/resources/application.yml`
- 测试：`src/test/java/com/example/quant/`

## 4. 运行与鉴权约定
1. 登录接口：`POST /api/auth/login`
2. 除登录和选项接口外，其他 `/api/**` 需要请求头：`X-Auth-Token: <token>`
3. 页面入口：`GET /`

## 5. 关键配置
- `app.trading.simulation`：模拟/真实下单开关
- `app.trading.api-key` / `app.trading.api-secret`：真实下单密钥
- `app.notify.enabled` / `app.notify.wechat-webhook`：通知配置
- `app.scheduler.enabled` / `app.scheduler.trigger-interval-ms`：自动触发策略配置

## 6. 常用排查入口（AI 建议优先）
- Binance 数据问题（ws-api）：
  - `data/BinanceRestClientImpl.java`
  - `data/MarketDataService.java`
- 指标问题（TA-Lib）：
  - `data/IndicatorCalculator.java`
- 下单问题（REST + 重试）：
  - `order/BinanceTradeClientImpl.java`
  - `order/OrderService.java`
- 策略执行问题：
  - `strategy/StrategyEngineService.java`
  - `strategy/DemoMaStrategy.java`
- 通知问题：
  - `service/NotificationService.java`
  - `notify/WeComWebhookClient.java`

## 7. 给下次 AI 任务的建议提示词
你下次可直接贴：

> 请先读取 `AI_INDEX.md` 和 `docs/运行步骤与代码映射.md`，然后按当前实现（ws-api取数据、REST下单、失败重试1次并通知）修改以下问题：...
