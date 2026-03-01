package com.example.quant.data;

import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * 币安接口并发与速率控制器。
 *
 * <p>普通用户场景下，限制：
 * 1) 同时在途请求数量（Semaphore）
 * 2) 每秒允许请求次数（按周期补充令牌）
 * </p>
 */
@Component
public class ApiConcurrencyGuard {

    /** 最大并发请求数。 */
    private static final int MAX_IN_FLIGHT = 3;
    /** 每秒请求数上限。 */
    private static final int REQUESTS_PER_SECOND = 8;

    private final Semaphore inFlightSemaphore = new Semaphore(MAX_IN_FLIGHT);
    private final Semaphore rateTokens = new Semaphore(REQUESTS_PER_SECOND);
    private final ScheduledExecutorService refillExecutor = Executors.newSingleThreadScheduledExecutor();

    public ApiConcurrencyGuard() {
        refillExecutor.scheduleAtFixedRate(() -> {
            int missing = REQUESTS_PER_SECOND - rateTokens.availablePermits();
            if (missing > 0) {
                rateTokens.release(missing);
            }
        }, 1, 1, TimeUnit.SECONDS);
    }

    /**
     * 执行受限调用。
     *
     * @param task 受限任务
     * @param <T> 返回值类型
     * @return 任务返回值
     */
    public <T> T execute(GuardedTask<T> task) {
        try {
            boolean rateOk = rateTokens.tryAcquire(Duration.ofSeconds(2));
            if (!rateOk) {
                throw new IllegalStateException("触发频控：请稍后重试");
            }
            boolean inFlightOk = inFlightSemaphore.tryAcquire(Duration.ofSeconds(2));
            if (!inFlightOk) {
                throw new IllegalStateException("触发并发限制：请稍后重试");
            }
            try {
                return task.run();
            } finally {
                inFlightSemaphore.release();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("调用被中断", e);
        }
    }

    @PreDestroy
    void shutdown() {
        refillExecutor.shutdownNow();
    }

    /**
     * 带返回值任务接口。
     */
    @FunctionalInterface
    public interface GuardedTask<T> {
        T run();
    }
}
