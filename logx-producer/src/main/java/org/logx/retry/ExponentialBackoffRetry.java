package org.logx.retry;

import org.logx.exception.StorageException;

import java.time.Duration;
import java.util.Objects;
import java.util.Random;

/**
 * 指数退避重试策略实现
 * <p>
 * 实现指数退避算法的重试策略，随着重试次数增加，等待时间呈指数增长。 包含随机抖动机制，避免大量并发请求同时重试造成的雪崩效应。
 * <p>
 * 重试策略特点：
 * <ul>
 * <li>支持可配置的最大重试次数</li>
 * <li>指数增长的等待时间</li>
 * <li>随机抖动减少请求冲突</li>
 * <li>基于异常类型的重试判断</li>
 * </ul>
 *
 * @author OSS Appender Team
 *
 * @since 1.0.0
 */
public class ExponentialBackoffRetry implements RetryStrategy {

    private static final int DEFAULT_MAX_RETRIES = 3;
    private static final Duration DEFAULT_INITIAL_DELAY = Duration.ofMillis(200);
    private static final double DEFAULT_MULTIPLIER = 2.0;
    private static final Duration DEFAULT_MAX_DELAY = Duration.ofSeconds(30);
    private static final double DEFAULT_JITTER_FACTOR = 0.1;

    private final int maxRetries;
    private final Duration initialDelay;
    private final double multiplier;
    private final Duration maxDelay;
    private final double jitterFactor;
    private final Random random;

    /**
     * 使用默认参数构造重试策略
     */
    public ExponentialBackoffRetry() {
        this(DEFAULT_MAX_RETRIES, DEFAULT_INITIAL_DELAY, DEFAULT_MULTIPLIER, DEFAULT_MAX_DELAY, DEFAULT_JITTER_FACTOR);
    }

    /**
     * 构造自定义参数的重试策略
     *
     * @param maxRetries
     *            最大重试次数
     * @param initialDelay
     *            初始延迟时间
     * @param multiplier
     *            延迟时间倍数
     * @param maxDelay
     *            最大延迟时间
     * @param jitterFactor
     *            抖动因子（0.0-1.0）
     */
    public ExponentialBackoffRetry(int maxRetries, Duration initialDelay, double multiplier, Duration maxDelay,
            double jitterFactor) {
        if (maxRetries < 0) {
            throw new IllegalArgumentException("maxRetries must be non-negative");
        }
        if (multiplier <= 1.0) {
            throw new IllegalArgumentException("multiplier must be greater than 1.0");
        }
        if (jitterFactor < 0.0 || jitterFactor > 1.0) {
            throw new IllegalArgumentException("jitterFactor must be between 0.0 and 1.0");
        }

        this.maxRetries = maxRetries;
        this.initialDelay = Objects.requireNonNull(initialDelay, "initialDelay cannot be null");
        this.multiplier = multiplier;
        this.maxDelay = Objects.requireNonNull(maxDelay, "maxDelay cannot be null");
        this.jitterFactor = jitterFactor;
        this.random = new Random();
    }

    @Override
    public boolean shouldRetry(StorageException exception, int attemptNumber) {
        // 超过最大重试次数
        if (attemptNumber > maxRetries) {
            return false;
        }

        // 检查异常是否可重试
        if (!exception.isRetryable()) {
            return false;
        }

        // 根据错误类型判断是否重试
        switch (exception.getErrorType()) {
            case NETWORK_ERROR:
            case SERVER_ERROR:
                // 网络错误和服务器错误通常可以重试
                return true;

            case AUTHENTICATION_ERROR:
            case CLIENT_ERROR:
            case CONFIGURATION_ERROR:
                // 认证错误、客户端错误和配置错误通常不可重试
                return false;

            case UNKNOWN_ERROR:
                // 未知错误保守处理，允许重试
                return true;

            default:
                return false;
        }
    }

    @Override
    public Duration calculateDelay(int attemptNumber) {
        if (attemptNumber <= 1) {
            return Duration.ZERO;
        }

        // 计算基础延迟时间: initialDelay * multiplier^(attemptNumber-2)
        long baseDelayMillis = initialDelay.toMillis();
        for (int i = 2; i < attemptNumber; i++) {
            baseDelayMillis = (long) (baseDelayMillis * multiplier);
        }

        Duration baseDelay = Duration.ofMillis(baseDelayMillis);

        // 限制最大延迟时间
        if (baseDelay.compareTo(maxDelay) > 0) {
            baseDelay = maxDelay;
        }

        // 添加随机抖动
        if (jitterFactor > 0.0) {
            long jitterMillis = (long) (baseDelay.toMillis() * jitterFactor * random.nextDouble());
            baseDelay = baseDelay.plus(Duration.ofMillis(jitterMillis));
        }

        return baseDelay;
    }

    @Override
    public int getMaxRetries() {
        return maxRetries;
    }

    @Override
    public String getStrategyName() {
        return "ExponentialBackoffRetry";
    }

    /**
     * 获取初始延迟时间
     */
    public Duration getInitialDelay() {
        return initialDelay;
    }

    /**
     * 获取延迟倍数
     */
    public double getMultiplier() {
        return multiplier;
    }

    /**
     * 获取最大延迟时间
     */
    public Duration getMaxDelay() {
        return maxDelay;
    }

    /**
     * 获取抖动因子
     */
    public double getJitterFactor() {
        return jitterFactor;
    }

    @Override
    public String toString() {
        return "ExponentialBackoffRetry{" + "maxRetries=" + maxRetries + ", initialDelay=" + initialDelay
                + ", multiplier=" + multiplier + ", maxDelay=" + maxDelay + ", jitterFactor=" + jitterFactor + '}';
    }

    /**
     * 创建默认的指数退避重试策略
     *
     * @return 默认配置的重试策略实例
     */
    public static ExponentialBackoffRetry defaultStrategy() {
        return new ExponentialBackoffRetry();
    }

    /**
     * 创建快速重试策略（较短的延迟时间）
     *
     * @return 快速重试策略实例
     */
    public static ExponentialBackoffRetry fastRetry() {
        return new ExponentialBackoffRetry(3, Duration.ofMillis(100), 1.5, Duration.ofSeconds(5), 0.1);
    }

    /**
     * 创建保守重试策略（较长的延迟时间）
     *
     * @return 保守重试策略实例
     */
    public static ExponentialBackoffRetry conservativeRetry() {
        return new ExponentialBackoffRetry(5, Duration.ofSeconds(1), 2.5, Duration.ofMinutes(2), 0.2);
    }
}
