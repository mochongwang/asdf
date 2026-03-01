package com.example.quant.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 应用配置。
 */
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private final Auth auth = new Auth();
    private final Trading trading = new Trading();
    private final Notify notify = new Notify();
    private final Scheduler scheduler = new Scheduler();
    private final Data data = new Data();

    public Auth getAuth() { return auth; }
    public Trading getTrading() { return trading; }
    public Notify getNotify() { return notify; }
    public Scheduler getScheduler() { return scheduler; }
    public Data getData() { return data; }

    public static class Auth {
        private String username = "admin";
        private String password = "123456";
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }

    public static class Trading {
        private boolean simulation = true;
        private String apiKey = "";
        private String apiSecret = "";

        public boolean isSimulation() { return simulation; }
        public void setSimulation(boolean simulation) { this.simulation = simulation; }
        public String getApiKey() { return apiKey; }
        public void setApiKey(String apiKey) { this.apiKey = apiKey; }
        public String getApiSecret() { return apiSecret; }
        public void setApiSecret(String apiSecret) { this.apiSecret = apiSecret; }
    }

    public static class Notify {
        private String wechatWebhook = "";
        private boolean enabled = false;
        public String getWechatWebhook() { return wechatWebhook; }
        public void setWechatWebhook(String wechatWebhook) { this.wechatWebhook = wechatWebhook; }
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
    }

    public static class Scheduler {
        private long triggerIntervalMs = 30_000;
        private boolean enabled = true;
        public long getTriggerIntervalMs() { return triggerIntervalMs; }
        public void setTriggerIntervalMs(long triggerIntervalMs) { this.triggerIntervalMs = triggerIntervalMs; }
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
    }

    /**
     * 数据层配置。
     */
    public static class Data {
        /** K线是否优先使用 WebSocket API 请求（ws-api）。 */
        private boolean klineUseWsApi = true;
        /** DuckDB 文件路径。 */
        private String duckdbPath = "./data/quant.duckdb";

        public boolean isKlineUseWsApi() {
            return klineUseWsApi;
        }

        public void setKlineUseWsApi(boolean klineUseWsApi) {
            this.klineUseWsApi = klineUseWsApi;
        }

        public String getDuckdbPath() {
            return duckdbPath;
        }

        public void setDuckdbPath(String duckdbPath) {
            this.duckdbPath = duckdbPath;
        }
    }
}
