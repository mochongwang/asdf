# AI_INDEX.md

> 面向 AI 助手的项目索引文件（用于后续任务快速理解上下文）。

## 1. 项目目标
- 项目名称：`crypto-quant-system`
- 技术栈：Java 21 + Spring Boot 3.1
- 目标：加密货币量化交易系统（当前为生产基线 + 模拟交易模式）

## 2. 关键目录索引
- 启动入口：`src/main/java/com/example/quant/QuantApplication.java`
- 配置与鉴权：`src/main/java/com/example/quant/config/`
- 登录鉴权：`src/main/java/com/example/quant/auth/`
- 数据层：`src/main/java/com/example/quant/data/`
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

## 3. 运行与鉴权约定
1. 登录接口：`POST /api/auth/login`
2. 除登录和选项接口外，其他 `/api/**` 默认需要请求头：
   - `X-Auth-Token: <token>`
3. 页面入口：`GET /`

## 4. 当前实现状态（供 AI 快速判断）
- ✅ 已有：登录、策略 CRUD、策略启停、手动触发、自动调度、回测、订单查询、账户查询、通知日志、杠杆信息。
- ✅ 已有：并发与频控保护、全局异常处理、企业微信 webhook 通道（可配置开关）。
- ⚠️ 当前默认：模拟交易（`app.trading.simulation=true`）。
- ⚠️ 生产待补：真实下单、完整 Binance WebSocket（账户/订单/K线/orderbook）、DuckDB 持久化全量替换。

## 5. 常用排查入口（AI 建议优先）
- 鉴权问题：
  - `config/AuthTokenInterceptor.java`
  - `auth/AuthService.java`
- 策略执行问题：
  - `strategy/StrategyEngineService.java`
  - `strategy/DemoMaStrategy.java`
  - `service/StrategyService.java`
- 自动触发问题：
  - `scheduler/StrategyTriggerScheduler.java`
  - `application.yml` 中 `app.scheduler.*`
- 通知问题：
  - `service/NotificationService.java`
  - `notify/WeComWebhookClient.java`
  - `application.yml` 中 `app.notify.*`

## 6. 给下次 AI 任务的建议提示词
你下次可直接贴这段：

> 请先读取 `AI_INDEX.md`，按其中索引定位代码，再修改以下问题：...

这样 AI 可以先看索引再改，能减少无效扫描和理解时间。
