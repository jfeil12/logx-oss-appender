package org.logx.reliability;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * JVM关闭钩子处理器
 * <p>
 * 确保应用关闭时能够优雅处理剩余的队列数据，避免数据丢失。 提供30秒超时保护，避免关闭过程无限等待。
 * <p>
 * 主要特性：
 * <ul>
 * <li>自动注册JVM shutdown hook</li>
 * <li>协调多个组件的优雅关闭</li>
 * <li>30秒超时保护机制</li>
 * <li>关闭状态监控和报告</li>
 * </ul>
 *
 * @author OSS Appender Team
 *
 * @since 1.0.0
 */
public class ShutdownHookHandler {

    private static final Logger logger = LoggerFactory.getLogger(ShutdownHookHandler.class);

    private static final long DEFAULT_SHUTDOWN_TIMEOUT_SECONDS = 30;

    private final long shutdownTimeoutSeconds;
    private final List<ShutdownCallback> callbacks;
    private final AtomicBoolean shutdownInProgress = new AtomicBoolean(false);
    private final AtomicBoolean registered = new AtomicBoolean(false);

    private Thread shutdownHook;

    /**
     * 关闭回调接口
     */
    public interface ShutdownCallback {
        /**
         * 执行关闭操作
         *
         * @param timeoutSeconds
         *            剩余超时时间（秒）
         *
         * @return 是否成功关闭
         */
        boolean shutdown(long timeoutSeconds);

        /**
         * 获取组件名称（用于日志）
         */
        String getComponentName();
    }

    /**
     * 构造关闭钩子处理器
     *
     * @param shutdownTimeoutSeconds
     *            关闭超时时间（秒）
     */
    public ShutdownHookHandler(long shutdownTimeoutSeconds) {
        this.shutdownTimeoutSeconds = shutdownTimeoutSeconds;
        this.callbacks = new ArrayList<>();
    }

    /**
     * 使用默认超时时间构造
     */
    public ShutdownHookHandler() {
        this(DEFAULT_SHUTDOWN_TIMEOUT_SECONDS);
    }

    /**
     * 注册关闭回调
     *
     * @param callback
     *            关闭回调
     */
    public synchronized void registerCallback(ShutdownCallback callback) {
        if (shutdownInProgress.get()) {
            throw new IllegalStateException("Cannot register callback during shutdown");
        }
        callbacks.add(callback);
    }

    /**
     * 注册JVM关闭钩子
     */
    public synchronized void registerShutdownHook() {
        if (registered.get()) {
            // 已经注册过了
            return;
        }

        shutdownHook = new Thread(this::executeShutdown, "oss-appender-shutdown-hook");

        try {
            Runtime.getRuntime().addShutdownHook(shutdownHook);
            registered.set(true);
            logger.info("OSS Appender shutdown hook registered successfully, thread: {}", shutdownHook.getName());
        } catch (Exception e) {
            logger.error("Failed to register shutdown hook: {}", e.getMessage(), e);
        }
    }

    /**
     * 手动执行关闭（用于测试或主动关闭）
     */
    public void executeShutdown() {
        if (!shutdownInProgress.compareAndSet(false, true)) {
            logShutdownMessage("Shutdown process already in progress, skipping duplicate execution");
            return;
        }

        // 记录shutdown开始
        logShutdownMessage("OSS Appender shutdown process started, timeout: {}s", shutdownTimeoutSeconds);
        long startTime = System.currentTimeMillis();

        try {
            for (int i = 0; i < callbacks.size(); i++) {
                ShutdownCallback callback = callbacks.get(i);
                long elapsed = (System.currentTimeMillis() - startTime) / 1000;
                long remaining = Math.max(0, shutdownTimeoutSeconds - elapsed);

                if (remaining <= 0) {
                    logShutdownMessage("Shutdown timeout reached, forcing exit");
                    break;
                }

                logShutdownMessage("Shutting down component: {}, remaining timeout: {}s", callback.getComponentName(), remaining);

                try {
                    boolean success = callback.shutdown(remaining);
                    if (success) {
                        logShutdownMessage("Component {} shutdown successfully", callback.getComponentName());
                    } else {
                        logShutdownMessage("Component {} shutdown failed", callback.getComponentName());
                    }
                } catch (Exception e) {
                    logShutdownMessage("Error shutting down component {}: {}", callback.getComponentName(), e.getMessage());
                }
            }

            long totalTime = (System.currentTimeMillis() - startTime) / 1000;
            logShutdownMessage("OSS Appender shutdown process completed in {}s", totalTime);

        } catch (RuntimeException e) {
            logShutdownMessage("Runtime error during shutdown process: {}", e.getMessage());
        } catch (Error e) {
            logShutdownMessage("Error during shutdown process: {}", e.getMessage());
        } finally {
            logShutdownMessage("Shutdown hook execution finished");
        }
    }
    
    /**
     * 统一处理shutdown过程中的日志记录
     * 优先使用logger，如果不可用则使用System.err作为后备
     */
    private void logShutdownMessage(String message, Object... params) {
        try {
            // 尝试使用正常的日志系统
            if (params.length == 0) {
                logger.info(message);
            } else if (params.length == 1) {
                logger.info(message, params[0]);
            } else if (params.length == 2) {
                logger.info(message, params[0], params[1]);
            } else {
                logger.info(message, params);
            }
        } catch (Throwable t) {
            // 如果日志系统不可用，使用System.err作为后备
            try {
                String formattedMessage = message;
                if (params.length > 0) {
                    formattedMessage = String.format(message.replace("{}", "%s"), params);
                }
                logger.error("[SHUTDOWN] {}", formattedMessage);
            } catch (Throwable t2) {
                // 如果格式化也失败了，至少输出原始消息
                logger.error("[SHUTDOWN] {}", message);
            }
        }
    }

    /**
     * 移除JVM关闭钩子（用于测试）
     */
    public synchronized void unregisterShutdownHook() {
        if (!registered.get() || shutdownHook == null) {
            return;
        }

        try {
            Runtime.getRuntime().removeShutdownHook(shutdownHook);
            registered.set(false);
            logger.info("OSS Appender shutdown hook unregistered");
        } catch (IllegalStateException e) {
            logger.info("Shutdown hook is already running, cannot unregister");
        } catch (Exception e) {
            logger.error("Failed to unregister shutdown hook: {}", e.getMessage());
        }
    }

    /**
     * 检查是否正在关闭
     */
    public boolean isShutdownInProgress() {
        return shutdownInProgress.get();
    }

    /**
     * 检查是否已注册关闭钩子
     */
    public boolean isRegistered() {
        return registered.get();
    }

    /**
     * 获取注册的回调数量
     */
    public int getCallbackCount() {
        return callbacks.size();
    }

    /**
     * 辅助方法：等待指定时间或直到条件满足
     */
    public static boolean waitForCondition(BooleanSupplier condition, long timeoutSeconds) {
        long endTime = System.currentTimeMillis() + timeoutSeconds * 1000;

        while (System.currentTimeMillis() < endTime) {
            if (condition.getAsBoolean()) {
                return true;
            }

            try {
                // 100ms检查间隔
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }

        return false;
    }

    /**
     * 函数式接口，用于条件等待
     */
    @FunctionalInterface
    public interface BooleanSupplier {
        boolean getAsBoolean();
    }

    /**
     * 创建基于CountDownLatch的关闭回调
     */
    public static ShutdownCallback createLatchCallback(String componentName, CountDownLatch latch,
            long maxWaitSeconds) {
        return new ShutdownCallback() {
            @Override
            public boolean shutdown(long timeoutSeconds) {
                try {
                    long waitTime = Math.min(timeoutSeconds, maxWaitSeconds);
                    return latch.await(waitTime, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return false;
                }
            }

            @Override
            public String getComponentName() {
                return componentName;
            }
        };
    }

    /**
     * 创建基于AutoCloseable的关闭回调
     */
    public static ShutdownCallback createCloseableCallback(String componentName, AutoCloseable closeable) {
        return new ShutdownCallback() {
            @Override
            public boolean shutdown(long timeoutSeconds) {
                try {
                    closeable.close();
                    return true;
                } catch (Exception e) {
                    logger.error("Failed to close {}: {}", componentName, e.getMessage());
                    return false;
                }
            }

            @Override
            public String getComponentName() {
                return componentName;
            }
        };
    }
}
