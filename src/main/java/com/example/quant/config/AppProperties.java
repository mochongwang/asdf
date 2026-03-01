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

    public Auth getAuth() { return auth; }
    public Trading getTrading() { return trading; }
    public Notify getNotify() { return notify; }
    public Scheduler getScheduler() { return scheduler; }

    public static class Auth {
        private String username = "admin";
        private String password = "123456";
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }

    public static class Trading {
        /** 是否模拟下单。 */
        private boolean simulation = true;
        /** 币安 API KEY。 */
        private String apiKey = "";
        /** 币安 API SECRET。 */
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
}
