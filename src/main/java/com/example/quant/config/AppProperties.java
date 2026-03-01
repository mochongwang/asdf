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
        public boolean isSimulation() { return simulation; }
        public void setSimulation(boolean simulation) { this.simulation = simulation; }
    }

    public static class Notify {
        /** 企业微信 webhook 地址。 */
        private String wechatWebhook = "";
        /** 是否启用 webhook 推送。 */
        private boolean enabled = false;
        public String getWechatWebhook() { return wechatWebhook; }
        public void setWechatWebhook(String wechatWebhook) { this.wechatWebhook = wechatWebhook; }
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
    }

    public static class Scheduler {
        /** 自动触发周期毫秒。 */
        private long triggerIntervalMs = 30_000;
        /** 自动轮询是否开启。 */
        private boolean enabled = true;
        public long getTriggerIntervalMs() { return triggerIntervalMs; }
        public void setTriggerIntervalMs(long triggerIntervalMs) { this.triggerIntervalMs = triggerIntervalMs; }
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
    }
}
