package org.logx.reliability;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 重试管理器
 * <p>
 * 实现智能的失败重试策略，支持指数退避重试、错误分类、 重试状态跟踪和性能优化。
 * <p>
 * 重试策略：
 * <ul>
 * <li>最多重试3次</li>
 * <li>指数退避：1s, 2s, 4s</li>
 * <li>网络错误重试，权限错误不重试</li>
 * <li>重试状态跟踪和监控</li>
 * </ul>
 *
 * @author OSS Appender Team
 *
 * @since 1.0.0
 */
public class RetryManager {

    private static final Logger logger = LoggerFactory.getLogger(RetryManager.class);

    private static final int DEFAULT_MAX_RETRIES = 3;
    private static final long DEFAULT_BASE_DELAY_MS = 1000;

    private final int maxRetries;
    private final long baseDelayMs;
    private final RetryPolicy retryPolicy;

    // 统计信息
    private final AtomicLong totalAttempts = new AtomicLong(0);
    private final AtomicLong totalSuccesses = new AtomicLong(0);
    private final AtomicLong totalRetries = new AtomicLong(0);
    private final AtomicLong totalFailures = new AtomicLong(0);

    /**
     * 重试策略接口
     */
    public interface RetryPolicy {
        /**
         * 判断异常是否应该重试
         *
         * @param exception
         *            发生的异常
         * @param attemptNumber
         *            当前尝试次数（从1开始）
         *
         * @return 是否应该重试
         */
        boolean shouldRetry(Exception exception, int attemptNumber);

        /**
         * 计算重试延迟时间
         *
         * @param attemptNumber
         *            当前尝试次数（从1开始）
         * @param baseDelayMs
         *            基础延迟时间
         *
         * @return 延迟时间（毫秒）
         */
        long calculateDelay(int attemptNumber, long baseDelayMs);
    }

    /**
     * 重试任务接口
     */
    @FunctionalInterface
    public interface RetryableTask<T> {
        T execute() throws Exception;
    }

    /**
     * 重试结果
     */
    public static class RetryResult<T> {
        private final T result;
        private final boolean success;
        private final int attemptCount;
        private final Exception lastException;

        public RetryResult(T result, boolean success, int attemptCount, Exception lastException) {
            this.result = result;
            this.success = success;
            this.attemptCount = attemptCount;
            // 存储异常的副本以避免内部表示暴露
            this.lastException = lastException != null ? new RuntimeException(lastException.getMessage(), lastException.getCause()) : null;
        }

        public T getResult() {
            return result;
        }

        public boolean isSuccess() {
            return success;
        }

        public int getAttemptCount() {
            return attemptCount;
        }

        public Exception getLastException() {
            // 返回异常的副本以避免内部表示暴露
            return lastException != null ? new RuntimeException(lastException.getMessage(), lastException.getCause()) : null;
        }
    }

    /**
     * 构造重试管理器
     *
     * @param maxRetries
     *            最大重试次数
     * @param baseDelayMs
     *            基础延迟时间
     * @param retryPolicy
     *            重试策略
     */
    public RetryManager(int maxRetries, long baseDelayMs, RetryPolicy retryPolicy) {
        this.maxRetries = Math.max(0, maxRetries);
        this.baseDelayMs = Math.max(0, baseDelayMs);
        this.retryPolicy = retryPolicy;
    }

    /**
     * 使用默认配置构造
     */
    public RetryManager() {
        this(DEFAULT_MAX_RETRIES, DEFAULT_BASE_DELAY_MS, new DefaultRetryPolicy());
    }

    /**
     * 执行可重试任务
     *
     * @param task
     *            要执行的任务
     *
     * @return 重试结果
     */
    public <T> RetryResult<T> execute(RetryableTask<T> task) {
        totalAttempts.incrementAndGet();

        Exception lastException = null;
        int attempt = 0;

        while (attempt <= maxRetries) {
            attempt++;

            try {
                T result = task.execute();
                totalSuccesses.incrementAndGet();
                if (attempt > 1) {
                    totalRetries.incrementAndGet();
                }
                return new RetryResult<>(result, true, attempt, null);

            } catch (Exception e) {
                lastException = e;

                // 如果是最后一次尝试，直接失败
                if (attempt > maxRetries) {
                    break;
                }

                // 检查是否应该重试
                if (!retryPolicy.shouldRetry(e, attempt)) {
                    logger.debug("Not retrying due to policy: {}: {}", e.getClass().getSimpleName(), e.getMessage());
                    break;
                }

                // 计算延迟时间并等待
                long delay = retryPolicy.calculateDelay(attempt, baseDelayMs);
                logger.debug("Retry attempt {} failed, retrying in {}ms. Error: {}", attempt, delay, e.getMessage());

                try {
                    Thread.sleep(delay);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }

        long failureCount = totalFailures.incrementAndGet();

        // 数据丢失监控日志：AC5要求
        logger.error("[DATA_LOSS_ALERT] 重试失败达到最大次数，数据已丢失。重试次数：{}，累计失败次数：{}，最后异常：{}",
            attempt, failureCount, lastException != null ? lastException.getMessage() : "未知");

        return new RetryResult<>(null, false, attempt, lastException);
    }

    /**
     * 执行可重试任务（简化版本，抛出异常）
     */
    public <T> T executeWithException(RetryableTask<T> task) throws Exception {
        RetryResult<T> result = execute(task);
        if (result.isSuccess()) {
            return result.getResult();
        } else {
            throw result.getLastException();
        }
    }

    /**
     * 获取重试统计信息
     */
    public RetryMetrics getMetrics() {
        long attempts = totalAttempts.get();
        long successes = totalSuccesses.get();
        long retries = totalRetries.get();
        long failures = totalFailures.get();

        double successRate = attempts > 0 ? (double) successes / attempts : 0.0;
        double retryRate = attempts > 0 ? (double) retries / attempts : 0.0;

        return new RetryMetrics(attempts, successes, retries, failures, successRate, retryRate);
    }

    /**
     * 重试统计指标
     */
    public static class RetryMetrics {
        private final long totalAttempts;
        private final long totalSuccesses;
        private final long totalRetries;
        private final long totalFailures;
        private final double successRate;
        private final double retryRate;

        public RetryMetrics(long totalAttempts, long totalSuccesses, long totalRetries, long totalFailures,
                double successRate, double retryRate) {
            this.totalAttempts = totalAttempts;
            this.totalSuccesses = totalSuccesses;
            this.totalRetries = totalRetries;
            this.totalFailures = totalFailures;
            this.successRate = successRate;
            this.retryRate = retryRate;
        }

        // Getters
        public long getTotalAttempts() {
            return totalAttempts;
        }

        public long getTotalSuccesses() {
            return totalSuccesses;
        }

        public long getTotalRetries() {
            return totalRetries;
        }

        public long getTotalFailures() {
            return totalFailures;
        }

        public double getSuccessRate() {
            return successRate;
        }

        public double getRetryRate() {
            return retryRate;
        }

        @Override
        public String toString() {
            return String.format(
                    "RetryMetrics{attempts=%d, successes=%d, retries=%d, failures=%d, "
                            + "successRate=%.2f%%, retryRate=%.2f%%}",
                    totalAttempts, totalSuccesses, totalRetries, totalFailures, successRate * 100, retryRate * 100);
        }
    }

    /**
     * 默认重试策略
     */
    public static class DefaultRetryPolicy implements RetryPolicy {

        @Override
        public boolean shouldRetry(Exception exception, int attemptNumber) {
            // 网络相关错误重试
            if (exception instanceof IOException || exception instanceof SocketException
                    || exception instanceof SocketTimeoutException) {
                return true;
            }

            // RuntimeException中的特定错误重试
            if (exception instanceof RuntimeException) {
                String message = exception.getMessage();
                if (message != null) {
                    // 网络连接相关错误
                    if (message.contains("Connection") || message.contains("timeout") || message.contains("network")
                            || message.contains("Service Unavailable") || message.contains("Internal Server Error")) {
                        return true;
                    }

                    // 权限相关错误不重试
                    if (message.contains("Access Denied") || message.contains("Unauthorized")
                            || message.contains("Forbidden") || message.contains("Invalid credentials")) {
                        return false;
                    }
                }
            }

            // 其他错误默认不重试
            return false;
        }

        @Override
        public long calculateDelay(int attemptNumber, long baseDelayMs) {
            // 指数退避：1s, 2s, 4s, 8s...
            return baseDelayMs * (1L << (attemptNumber - 1));
        }
    }

    /**
     * 创建自定义重试策略
     */
    public static RetryPolicy createCustomPolicy(Class<?>[] retryableExceptions, long[] delays) {
        return new RetryPolicy() {
            @Override
            public boolean shouldRetry(Exception exception, int attemptNumber) {
                for (Class<?> retryableException : retryableExceptions) {
                    if (retryableException.isInstance(exception)) {
                        return true;
                    }
                }
                return false;
            }

            @Override
            public long calculateDelay(int attemptNumber, long baseDelayMs) {
                int index = Math.min(attemptNumber - 1, delays.length - 1);
                return delays[index];
            }
        };
    }

    /**
     * 创建固定延迟重试策略
     */
    public static RetryPolicy createFixedDelayPolicy(long delayMs) {
        return new RetryPolicy() {
            @Override
            public boolean shouldRetry(Exception exception, int attemptNumber) {
                return new DefaultRetryPolicy().shouldRetry(exception, attemptNumber);
            }

            @Override
            public long calculateDelay(int attemptNumber, long baseDelayMs) {
                return delayMs;
            }
        };
    }

    /**
     * 重置统计信息
     */
    public void resetMetrics() {
        totalAttempts.set(0);
        totalSuccesses.set(0);
        totalRetries.set(0);
        totalFailures.set(0);
    }
}
