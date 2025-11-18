package org.logx.retry;

import org.logx.exception.StorageException;

import java.time.Duration;

/**
 * 重试策略接口
 * <p>
 * 定义存储操作失败时的重试逻辑抽象，支持不同的重试算法实现。 提供灵活的重试策略配置，包括重试次数、延迟算法等。
 *
 * @author OSS Appender Team
 *
 * @since 1.0.0
 */
public interface RetryStrategy {

    /**
     * 判断异常是否应该重试
     *
     * @param exception
     *            发生的异常
     * @param attemptNumber
     *            当前尝试次数（从1开始）
     *
     * @return true如果应该重试，false如果不应该重试
     */
    boolean shouldRetry(StorageException exception, int attemptNumber);

    /**
     * 计算下次重试前的等待时间
     *
     * @param attemptNumber
     *            当前尝试次数（从1开始）
     *
     * @return 等待时间，如果不需要等待返回Duration.ZERO
     */
    Duration calculateDelay(int attemptNumber);

    /**
     * 获取最大重试次数
     *
     * @return 最大重试次数
     */
    int getMaxRetries();

    /**
     * 获取策略名称，用于日志记录
     *
     * @return 策略名称
     */
    String getStrategyName();
}
