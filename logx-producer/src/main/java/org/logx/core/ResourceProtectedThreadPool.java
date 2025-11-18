package org.logx.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.OperatingSystemMXBean;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 资源保护线程池
 * <p>
 * 实现固定大小的线程池，确保日志组件不影响业务系统性能。 通过线程优先级控制、CPU让出机制和内存保护机制防止资源无限扩张。
 * <p>
 * 主要特性：
 * <ul>
 * <li>固定线程池大小，默认2个线程</li>
 * <li>低优先级线程调度，确保业务优先</li>
 * <li>CPU让出机制，监控系统负载并主动yield</li>
 * <li>内存保护机制，防止JVM OOM</li>
 * <li>线程池监控指标和配置调优接口</li>
 * </ul>
 *
 * @author OSS Appender Team
 *
 * @since 1.0.0
 */
public class ResourceProtectedThreadPool implements AutoCloseable {

    private static final Logger logger = LoggerFactory.getLogger(ResourceProtectedThreadPool.class);

    // 默认配置
    private static final int DEFAULT_CORE_POOL_SIZE = 4;
    private static final int DEFAULT_MAXIMUM_POOL_SIZE = 4;
    private static final long DEFAULT_KEEP_ALIVE_TIME = 60L;
    private static final int DEFAULT_QUEUE_CAPACITY = 1000;

    // 80%
    private static final double DEFAULT_CPU_THRESHOLD = 0.8;

    // 85%
    private static final double DEFAULT_MEMORY_THRESHOLD = 0.85;

    // 线程池和配置
    private final ThreadPoolExecutor executor;
    private final int corePoolSize;
    private final int maximumPoolSize;
    private final long keepAliveTime;
    private final int queueCapacity;

    // 资源保护配置
    private final double cpuThreshold;
    private final double memoryThreshold;
    private final boolean enableCpuYield;
    private final boolean enableMemoryProtection;

    // 监控统计
    private final AtomicLong totalTasksSubmitted = new AtomicLong(0);
    private final AtomicLong totalTasksCompleted = new AtomicLong(0);
    private final AtomicLong totalTasksRejected = new AtomicLong(0);
    private final AtomicLong totalYieldCount = new AtomicLong(0);
    private final AtomicLong totalMemoryProtectionCount = new AtomicLong(0);

    // 系统监控
    private final OperatingSystemMXBean osMXBean;
    private final MemoryMXBean memoryMXBean;

    /**
     * 构造资源保护线程池
     *
     * @param config
     *            线程池配置
     */
    public ResourceProtectedThreadPool(Config config) {
        this.corePoolSize = config.corePoolSize;
        this.maximumPoolSize = config.maximumPoolSize;
        this.keepAliveTime = config.keepAliveTime;
        this.queueCapacity = config.queueCapacity;
        this.cpuThreshold = config.cpuThreshold;
        this.memoryThreshold = config.memoryThreshold;
        this.enableCpuYield = config.enableCpuYield;
        this.enableMemoryProtection = config.enableMemoryProtection;

        this.osMXBean = ManagementFactory.getOperatingSystemMXBean();
        this.memoryMXBean = ManagementFactory.getMemoryMXBean();

        // 创建有界队列防止OOM
        BlockingQueue<Runnable> workQueue = new ArrayBlockingQueue<>(queueCapacity);

        // 创建线程工厂，设置低优先级
        ThreadFactory threadFactory = new ResourceProtectedThreadFactory();

        // 创建拒绝处理器
        RejectedExecutionHandler rejectedHandler = new ResourceProtectedRejectedHandler();

        // 创建线程池
        this.executor = new ThreadPoolExecutor(corePoolSize, maximumPoolSize, keepAliveTime, TimeUnit.SECONDS,
                workQueue, threadFactory, rejectedHandler);

        // 允许核心线程超时
        executor.allowCoreThreadTimeOut(true);
    }

    /**
     * 使用默认配置构造线程池
     */
    public ResourceProtectedThreadPool() {
        this(Config.defaultConfig());
    }

    /**
     * 提交任务到线程池
     *
     * @param task
     *            要执行的任务
     *
     * @return Future对象，可用于取消任务或获取结果
     */
    public Future<?> submit(Runnable task) {
        totalTasksSubmitted.incrementAndGet();

        // 内存保护检查
        if (enableMemoryProtection && isMemoryPressureHigh()) {
            totalMemoryProtectionCount.incrementAndGet();
            throw new ResourceProtectionException("Memory pressure too high, rejecting task");
        }

        return executor.submit(new ProtectedTask(task));
    }

    /**
     * 提交有返回值的任务
     */
    public <T> Future<T> submit(Callable<T> task) {
        totalTasksSubmitted.incrementAndGet();

        if (enableMemoryProtection && isMemoryPressureHigh()) {
            totalMemoryProtectionCount.incrementAndGet();
            throw new ResourceProtectionException("Memory pressure too high, rejecting task");
        }

        return executor.submit(new ProtectedCallable<>(task));
    }

    /**
     * 获取线程池监控指标
     */
    public PoolMetrics getMetrics() {
        return new PoolMetrics(executor.getPoolSize(), executor.getActiveCount(), executor.getCompletedTaskCount(),
                executor.getTaskCount(), executor.getQueue().size(), totalTasksSubmitted.get(),
                totalTasksCompleted.get(), totalTasksRejected.get(), totalYieldCount.get(),
                totalMemoryProtectionCount.get(), getCurrentCpuUsage(), getCurrentMemoryUsage());
    }

    /**
     * 优雅关闭线程池
     */
    @Override
    public void close() {
        shutdown();
    }

    /**
     * 关闭线程池
     */
    public void shutdown() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
                executor.shutdownNow();
                if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                    logger.error("线程池未能正确关闭");
                }
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 检查CPU使用率是否过高
     */
    private boolean isCpuUsageHigh() {
        double cpuUsage = getCurrentCpuUsage();
        return cpuUsage > cpuThreshold;
    }

    /**
     * 检查内存压力是否过高
     */
    private boolean isMemoryPressureHigh() {
        double memoryUsage = getCurrentMemoryUsage();
        return memoryUsage > memoryThreshold;
    }

    /**
     * 获取当前CPU使用率
     */
    private double getCurrentCpuUsage() {
        try {
            // 回退方案：使用系统平均负载
            double loadAverage = osMXBean.getSystemLoadAverage();
            int availableProcessors = osMXBean.getAvailableProcessors();

            if (loadAverage >= 0 && availableProcessors > 0) {
                // 将系统负载转换为使用率（粗略估算）
                return Math.min(1.0, loadAverage / availableProcessors);
            }
        } catch (UnsupportedOperationException e) {
            // 如果系统不支持获取负载平均值，返回0
            logger.warn("System does not support getSystemLoadAverage: {}", e.getMessage());
        } catch (Exception e) {
            // 如果获取失败，记录错误并返回0
            logger.error("Failed to get CPU usage: {}", e.getMessage());
        }

        // 无法获取时返回0
        return 0.0;
    }

    /**
     * 获取当前内存使用率
     */
    private double getCurrentMemoryUsage() {
        long usedMemory = memoryMXBean.getHeapMemoryUsage().getUsed();
        long maxMemory = memoryMXBean.getHeapMemoryUsage().getMax();
        return maxMemory > 0 ? (double) usedMemory / maxMemory : 0.0;
    }

    /**
     * 资源保护的任务包装器
     */
    private class ProtectedTask implements Runnable {
        private final Runnable delegate;

        public ProtectedTask(Runnable delegate) {
            this.delegate = delegate;
        }

        @Override
        public void run() {
            try {
                // CPU让出机制
                if (enableCpuYield && isCpuUsageHigh()) {
                    totalYieldCount.incrementAndGet();
                    Thread.yield();
                }

                delegate.run();
                totalTasksCompleted.incrementAndGet();

            } catch (Exception e) {
                // 记录异常但不抛出，避免影响线程池
                logger.error("任务执行异常: {}", e.getMessage());
            }
        }
    }

    /**
     * 资源保护的Callable包装器
     */
    private class ProtectedCallable<T> implements Callable<T> {
        private final Callable<T> delegate;

        public ProtectedCallable(Callable<T> delegate) {
            this.delegate = delegate;
        }

        @Override
        public T call() throws Exception {
            try {
                if (enableCpuYield && isCpuUsageHigh()) {
                    totalYieldCount.incrementAndGet();
                    Thread.yield();
                }

                T result = delegate.call();
                totalTasksCompleted.incrementAndGet();
                return result;

            } catch (Exception e) {
                logger.error("任务执行异常: {}", e.getMessage());
                throw e;
            }
        }
    }

    /**
     * 资源保护线程工厂
     */
    private static class ResourceProtectedThreadFactory implements ThreadFactory {
        private final AtomicInteger threadNumber = new AtomicInteger(1);
        private static final String namePrefix = "oss-appender-protected-";

        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r, namePrefix + threadNumber.getAndIncrement());

            // 设置为守护线程
            t.setDaemon(true);

            // 设置最低优先级，确保业务线程优先
            t.setPriority(Thread.MIN_PRIORITY);

            return t;
        }
    }

    /**
     * 资源保护拒绝处理器
     */
    private class ResourceProtectedRejectedHandler implements RejectedExecutionHandler {
        @Override
        public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
            long rejectedCount = totalTasksRejected.incrementAndGet();

            // 数据丢失监控日志：AC5要求
            logger.error("[DATA_LOSS_ALERT] 任务被拒绝执行，可能导致数据丢失。原因：队列已满或线程池已关闭。累计拒绝次数：{}", rejectedCount);
        }
    }

    /**
     * 线程池配置类
     */
    public static class Config {
        private int corePoolSize = DEFAULT_CORE_POOL_SIZE;
        private int maximumPoolSize = DEFAULT_MAXIMUM_POOL_SIZE;
        private long keepAliveTime = DEFAULT_KEEP_ALIVE_TIME;
        private int queueCapacity = DEFAULT_QUEUE_CAPACITY;
        private double cpuThreshold = DEFAULT_CPU_THRESHOLD;
        private double memoryThreshold = DEFAULT_MEMORY_THRESHOLD;
        private boolean enableCpuYield = true;
        private boolean enableMemoryProtection = true;

        public static Config defaultConfig() {
            return new Config();
        }

        public Config corePoolSize(int corePoolSize) {
            this.corePoolSize = corePoolSize;
            return this;
        }

        public Config maximumPoolSize(int maximumPoolSize) {
            this.maximumPoolSize = maximumPoolSize;
            return this;
        }

        public Config keepAliveTime(long keepAliveTime) {
            this.keepAliveTime = keepAliveTime;
            return this;
        }

        public Config queueCapacity(int queueCapacity) {
            this.queueCapacity = queueCapacity;
            return this;
        }

        public Config cpuThreshold(double cpuThreshold) {
            this.cpuThreshold = cpuThreshold;
            return this;
        }

        public Config memoryThreshold(double memoryThreshold) {
            this.memoryThreshold = memoryThreshold;
            return this;
        }

        public Config enableCpuYield(boolean enableCpuYield) {
            this.enableCpuYield = enableCpuYield;
            return this;
        }

        public Config enableMemoryProtection(boolean enableMemoryProtection) {
            this.enableMemoryProtection = enableMemoryProtection;
            return this;
        }
    }

    /**
     * 线程池监控指标
     */
    public static class PoolMetrics {
        private final int poolSize;
        private final int activeThreadCount;
        private final long completedTaskCount;
        private final long totalTaskCount;
        private final int queueSize;
        private final long totalSubmitted;
        private final long totalCompleted;
        private final long totalRejected;
        private final long totalYieldCount;
        private final long totalMemoryProtectionCount;
        private final double currentCpuUsage;
        private final double currentMemoryUsage;

        public PoolMetrics(int poolSize, int activeThreadCount, long completedTaskCount, long totalTaskCount,
                int queueSize, long totalSubmitted, long totalCompleted, long totalRejected, long totalYieldCount,
                long totalMemoryProtectionCount, double currentCpuUsage, double currentMemoryUsage) {
            this.poolSize = poolSize;
            this.activeThreadCount = activeThreadCount;
            this.completedTaskCount = completedTaskCount;
            this.totalTaskCount = totalTaskCount;
            this.queueSize = queueSize;
            this.totalSubmitted = totalSubmitted;
            this.totalCompleted = totalCompleted;
            this.totalRejected = totalRejected;
            this.totalYieldCount = totalYieldCount;
            this.totalMemoryProtectionCount = totalMemoryProtectionCount;
            this.currentCpuUsage = currentCpuUsage;
            this.currentMemoryUsage = currentMemoryUsage;
        }

        // Getters
        public int getPoolSize() {
            return poolSize;
        }

        public int getActiveThreadCount() {
            return activeThreadCount;
        }

        public long getCompletedTaskCount() {
            return completedTaskCount;
        }

        public long getTotalTaskCount() {
            return totalTaskCount;
        }

        public int getQueueSize() {
            return queueSize;
        }

        public long getTotalSubmitted() {
            return totalSubmitted;
        }

        public long getTotalCompleted() {
            return totalCompleted;
        }

        public long getTotalRejected() {
            return totalRejected;
        }

        public long getTotalYieldCount() {
            return totalYieldCount;
        }

        public long getTotalMemoryProtectionCount() {
            return totalMemoryProtectionCount;
        }

        public double getCurrentCpuUsage() {
            return currentCpuUsage;
        }

        public double getCurrentMemoryUsage() {
            return currentMemoryUsage;
        }

        @Override
        public String toString() {
            return String.format(
                    "PoolMetrics{poolSize=%d, activeThreads=%d, queueSize=%d, "
                            + "submitted=%d, completed=%d, rejected=%d, yields=%d, memoryProtections=%d, "
                            + "cpuUsage=%.2f%%, memoryUsage=%.2f%%}",
                    poolSize, activeThreadCount, queueSize, totalSubmitted, totalCompleted, totalRejected,
                    totalYieldCount, totalMemoryProtectionCount, currentCpuUsage * 100, currentMemoryUsage * 100);
        }
    }

    /**
     * 资源保护异常
     */
    public static class ResourceProtectionException extends RuntimeException {
        public ResourceProtectionException(String message) {
            super(message);
        }
    }
}
