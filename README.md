# 加密货币量化交易系统（Java 21 / Spring Boot 3.1）

这是按你的需求文档实现的**生产可部署版本基线**（可运行、可联调、可扩展）。

> 说明：当前版本已打通完整业务流程，并补齐生产必需能力：
> - 配置化账号密码/通知/调度
> - API Token 鉴权拦截
> - 启动时序编排日志
> - 策略自动调度触发
> - 企业微信 webhook 通知通道
>
> 默认 `app.trading.simulation=true`，即“安全模拟下单模式”。生产环境请切换真实交易实现并管理密钥。

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
   - `StrategyService`：策略主表+指标明细 CRUD 和周期校验
   - `BacktestService`：回测逻辑
   - `NotificationService`：通知日志+webhook推送
   - `AccountService`：账户查询
   - `LeverageService`：杠杆信息

5. **生产增强层**
   - `AppProperties`：配置中心
   - `AuthTokenInterceptor`：API鉴权
   - `SystemStartupRunner`：启动时序执行
   - `StrategyTriggerScheduler`：策略自动触发
   - `WeComWebhookClient`：企业微信消息推送

## 接口总览

- 登录：`POST /api/auth/login`（默认 `admin` / `123456`）
- 策略：`/api/strategies`（CRUD、启停、触发、回测）
- 订单：`GET /api/orders`（分页+筛选）
- 账户：`GET /api/accounts`
- 通知：`GET /api/notifications`
- 杠杆：`GET /api/leverage`
- 可选项：`GET /api/options`
- 页面：`GET /`

> 除 `/api/auth/login` 与 `/api/options` 外，其他 `/api/**` 需带请求头：`X-Auth-Token: <token>`。

## 页面能力

- 登录
- 创建策略（下拉优先，降低输入错误）
- 启用/禁用/触发策略
- 回测
- 订单/账户/通知/杠杆查询

## 运行

```bash
mvn spring-boot:run
```

浏览器访问：`http://localhost:8080/`

## 配置（application.yml）

- `app.auth.username/password`：登录账号密码
- `app.notify.enabled/wechat-webhook`：企业微信推送
- `app.scheduler.enabled/trigger-interval-ms`：自动触发策略调度
- `app.trading.simulation`：模拟或真实交易开关

## 生产上线建议

1. 切换持久化到 DuckDB（替换内存存储）
2. 接入 Binance 私有 websocket（账户流、订单流）
3. 接入 K线 websocket + 历史K线预加载 + 指标缓存
4. 对接真实下单与风控
5. 配置企业微信 webhook 与告警策略


## 本次补齐（按需求文档）

- 指标计算统一使用 `com.tictactec.ta.lib.Core`（TA-Lib）。
- 下单：只用 Binance REST API，失败后重试1次，仍失败则报错并通知。
- K线获取：只用 Binance WebSocket API，失败后重试1次，仍失败则报错并通知。
- 取数据层：ticker 与 klines 均通过 Binance WebSocket API（ws-api）获取。
- 指标计算：新增基础指标计算器，支持 MA、RSI（并预留其他指标扩展）。
- 下单层：新增真实币安下单客户端（签名请求），并通过 `app.trading.simulation` 开关控制模拟/真实模式。

