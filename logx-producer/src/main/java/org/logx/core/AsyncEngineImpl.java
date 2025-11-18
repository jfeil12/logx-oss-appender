package org.logx.core;

import org.logx.core.EnhancedDisruptorBatchingQueue.LogEvent;
import org.logx.fallback.FallbackManager;
import org.logx.fallback.FallbackUploaderTask;
import org.logx.fallback.ObjectNameGenerator;
import org.logx.reliability.ShutdownHookHandler;
import org.logx.storage.StorageService;
import org.logx.storage.StorageServiceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public class AsyncEngineImpl implements AsyncEngine, AutoCloseable {

    private static final Logger logger = LoggerFactory.getLogger(AsyncEngineImpl.class);

    private final long emergencyMemoryThreshold;
    private final StorageService storageService;
    private final ShutdownHookHandler shutdownHandler;
    private final EnhancedDisruptorBatchingQueue batchingQueue;
    private final AsyncEngineConfig config;
    private final FallbackManager fallbackManager;
    private ScheduledExecutorService fallbackScheduler;
    private java.util.concurrent.ExecutorService uploadExecutor;
    private ScheduledExecutorService queueMonitor;
    private final AtomicBoolean started = new AtomicBoolean(false);
    private final AtomicBoolean stopped = new AtomicBoolean(false);
    private final AtomicLong currentMemoryUsage = new AtomicLong(0);

    public AsyncEngineImpl(AsyncEngineConfig config) {
        this.config = config;
        this.storageService = StorageServiceFactory.createStorageService(config.getStorageConfig());
        this.emergencyMemoryThreshold = (long) config.getEmergencyMemoryThresholdMb() * 1024 * 1024;
        this.fallbackManager = new FallbackManager(config.getLogFilePrefix(), storageService.getKeyPrefix());

        int maxUploadSizeMb = 10;
        boolean enableSharding = true;
        boolean enableCompression = true;
        org.logx.config.properties.LogxOssProperties props = config.getStorageConfig() != null
                ? config.getStorageConfig().getProperties()
                : null;
        if (props != null) {
            enableSharding = props.getEngine().isEnableSharding();
            enableCompression = props.getEngine().isEnableCompression();
            maxUploadSizeMb = props.getEngine().getMaxUploadSizeMb();
        }

        EnhancedDisruptorBatchingQueue.Config queueConfig = new EnhancedDisruptorBatchingQueue.Config()
                .queueCapacity(config.getQueueCapacity())
                .batchMaxMessages(config.getBatchMaxMessages())
                .batchMaxBytes(config.getBatchMaxBytes())
                .maxMessageAgeMs(config.getMaxMessageAgeMs())
                .blockOnFull(config.isBlockOnFull())
                .multiProducer(config.isMultiProducer())
                .enableCompression(enableCompression)
                .enableSharding(enableSharding)
                .maxUploadSizeMb(maxUploadSizeMb)
                .uploadTimeoutMs(config.getUploadTimeoutMs());

        this.batchingQueue = new EnhancedDisruptorBatchingQueue(queueConfig, this::onBatch, storageService);
        this.shutdownHandler = new ShutdownHookHandler();
        this.shutdownHandler.registerCallback(new ShutdownHookHandler.ShutdownCallback() {
            @Override
            public boolean shutdown(long timeoutSeconds) {
                try {
                    AsyncEngineImpl.this.stop(timeoutSeconds, TimeUnit.SECONDS);
                    return true;
                } catch (Exception e) {
                    logger.error("Failed to shutdown AsyncEngine: {}", e.getMessage(), e);
                    return false;
                }
            }

            @Override
            public String getComponentName() {
                return "AsyncEngine";
            }
        });
        this.shutdownHandler.registerShutdownHook();
    }

    // 包级别可见的测试构造函数，允许传入Mock的StorageService
    AsyncEngineImpl(AsyncEngineConfig config, StorageService storageService) {
        this.config = config;
        this.storageService = storageService;
        this.emergencyMemoryThreshold = (long) config.getEmergencyMemoryThresholdMb() * 1024 * 1024;
        this.fallbackManager = new FallbackManager(config.getLogFilePrefix(), storageService.getKeyPrefix());

        int maxUploadSizeMb = 10;
        boolean enableSharding = true;
        boolean enableCompression = true;
        org.logx.config.properties.LogxOssProperties props = config.getStorageConfig() != null
                ? config.getStorageConfig().getProperties()
                : null;
        if (props != null) {
            enableSharding = props.getEngine().isEnableSharding();
            enableCompression = props.getEngine().isEnableCompression();
            maxUploadSizeMb = props.getEngine().getMaxUploadSizeMb();
        }

        EnhancedDisruptorBatchingQueue.Config queueConfig = new EnhancedDisruptorBatchingQueue.Config()
                .queueCapacity(config.getQueueCapacity())
                .batchMaxMessages(config.getBatchMaxMessages())
                .batchMaxBytes(config.getBatchMaxBytes())
                .maxMessageAgeMs(config.getMaxMessageAgeMs())
                .blockOnFull(config.isBlockOnFull())
                .multiProducer(config.isMultiProducer())
                .enableCompression(enableCompression)
                .enableSharding(enableSharding)
                .maxUploadSizeMb(maxUploadSizeMb)
                .uploadTimeoutMs(config.getUploadTimeoutMs());

        this.batchingQueue = new EnhancedDisruptorBatchingQueue(queueConfig, this::onBatch, storageService);
        this.shutdownHandler = new ShutdownHookHandler();
        this.shutdownHandler.registerCallback(new ShutdownHookHandler.ShutdownCallback() {
            @Override
            public boolean shutdown(long timeoutSeconds) {
                try {
                    AsyncEngineImpl.this.stop(timeoutSeconds, TimeUnit.SECONDS);
                    return true;
                } catch (Exception e) {
                    logger.error("Failed to shutdown AsyncEngine: {}", e.getMessage(), e);
                    return false;
                }
            }

            @Override
            public String getComponentName() {
                return "AsyncEngine";
            }
        });
        this.shutdownHandler.registerShutdownHook();
    }

    @Override
    public void start() {
        if (!started.compareAndSet(false, true)) {
            return;
        }

        batchingQueue.start();
        startFallbackScheduler();
        startUploadExecutor();
        batchingQueue.setShardExecutor(uploadExecutor, config.getUploadTimeoutMs());

        if (config.isEnableDynamicBatching()) {
            startQueuePressureMonitor();
        }

        shutdownHandler.registerShutdownHook();

        logger.info("AsyncEngine started successfully with {} parallel upload threads, dynamic batching: {}",
                config.getParallelUploadThreads(), config.isEnableDynamicBatching());
    }

    @Override
    public void stop(long timeout, TimeUnit timeUnit) {
        if (!stopped.compareAndSet(false, true)) {
            logger.info("AsyncEngine already stopped");
            return;
        }

        logger.info("Stopping AsyncEngine with timeout: {} {}", timeout, timeUnit);

        long timeoutMillis = timeUnit.toMillis(timeout);
        long startTime = System.currentTimeMillis();

        try {
            if (queueMonitor != null) {
                queueMonitor.shutdown();
                try {
                    if (!queueMonitor.awaitTermination(5, TimeUnit.SECONDS)) {
                        queueMonitor.shutdownNow();
                    }
                } catch (InterruptedException e) {
                    queueMonitor.shutdownNow();
                    Thread.currentThread().interrupt();
                }
            }

            if (fallbackScheduler != null) {
                fallbackScheduler.shutdown();
                try {
                    if (!fallbackScheduler.awaitTermination(30, TimeUnit.SECONDS)) {
                        fallbackScheduler.shutdownNow();
                    }
                } catch (InterruptedException e) {
                    fallbackScheduler.shutdownNow();
                    Thread.currentThread().interrupt();
                }
            }

            batchingQueue.close();

            if (uploadExecutor != null) {
                uploadExecutor.shutdown();
                try {
                    long elapsed = System.currentTimeMillis() - startTime;
                    long remaining = Math.max(5, timeoutMillis - elapsed);
                    if (!uploadExecutor.awaitTermination(remaining, TimeUnit.MILLISECONDS)) {
                        uploadExecutor.shutdownNow();
                    }
                } catch (InterruptedException e) {
                    uploadExecutor.shutdownNow();
                    Thread.currentThread().interrupt();
                }
            }

            if (storageService != null) {
                try {
                    storageService.close();
                } catch (Exception e) {
                    logger.error("Error closing storage service: {}", e.getMessage());
                }
            }

            logger.info("AsyncEngine stopped successfully");
        } catch (Exception e) {
            logger.error("Error stopping AsyncEngine: {}", e.getMessage());
        }
    }

    @Override
    public void close() {
        stop(5, TimeUnit.SECONDS);
    }

    @Override
    public void put(byte[] data) {
        if (!started.get() || stopped.get() || data == null || data.length == 0) {
            return;
        }

        long currentMemory = currentMemoryUsage.get();

        if (currentMemory > emergencyMemoryThreshold) {
            logger.warn("Emergency fallback triggered: memory usage {} MB > {} MB, writing directly to fallback file",
                    currentMemory / 1024 / 1024, emergencyMemoryThreshold / 1024 / 1024);
            fallbackManager.writeFallbackFile(data);
            return;
        }

        if (batchingQueue.submit(data)) {
            currentMemoryUsage.addAndGet(data.length);
        }
    }

    private boolean onBatch(byte[] batchData, int originalSize, boolean compressed, int messageCount) {
        String key = ObjectNameGenerator.generateObjectName(storageService.getKeyPrefix());

        if (uploadExecutor != null && !uploadExecutor.isShutdown()) {
            uploadExecutor.submit(() -> {
                try {
                    storageService.putObject(key, batchData).get(config.getUploadTimeoutMs(), TimeUnit.MILLISECONDS);
                    currentMemoryUsage.addAndGet(-originalSize);
                } catch (Exception e) {
                    logger.error("Parallel upload failed for {}: {}", key, e.getMessage(), e);
                    if (fallbackManager.writeFallbackFile(batchData)) {
                        currentMemoryUsage.addAndGet(-originalSize);
                    }
                }
            });
            return true;
        } else {
            return onBatchSync(batchData, originalSize, compressed, messageCount, key);
        }
    }

    private boolean onBatchSync(byte[] batchData, int originalSize, boolean compressed, int messageCount, String key) {
        try {
            storageService.putObject(key, batchData).get(config.getUploadTimeoutMs(), TimeUnit.MILLISECONDS);
            currentMemoryUsage.addAndGet(-originalSize);
            return true;
        } catch (Exception e) {
            logger.error("Sync upload failed for {}: {}", key, e.getMessage(), e);
            if (fallbackManager.writeFallbackFile(batchData)) {
                currentMemoryUsage.addAndGet(-originalSize);
                return true;
            }
            currentMemoryUsage.addAndGet(-originalSize);
            return false;
        }
    }

    private void startFallbackScheduler() {
        fallbackScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "fallback-uploader");
            t.setDaemon(true);
            t.setPriority(Thread.MIN_PRIORITY);
            return t;
        });

        int fallbackRetentionDays = config.getFallbackRetentionDays();
        int fallbackScanIntervalSeconds = config.getFallbackScanIntervalSeconds();

        fallbackScheduler.scheduleWithFixedDelay(
                new FallbackUploaderTask(storageService, config.getLogFilePrefix(), config.getLogFileName(), fallbackRetentionDays),
                1, fallbackScanIntervalSeconds, TimeUnit.SECONDS
        );
    }

    private void startUploadExecutor() {
        int threads = config.getParallelUploadThreads();
        this.uploadExecutor = Executors.newFixedThreadPool(threads, r -> {
            Thread t = new Thread(r, "parallel-uploader-" + System.currentTimeMillis());
            t.setDaemon(true);
            return t;
        });
    }

    private void startQueuePressureMonitor() {
        queueMonitor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "queue-pressure-monitor");
            t.setDaemon(true);
            t.setPriority(Thread.MIN_PRIORITY);
            return t;
        });

        queueMonitor.scheduleWithFixedDelay(
                this::monitorQueuePressure,
                config.getQueuePressureMonitorIntervalMs(),
                config.getQueuePressureMonitorIntervalMs(),
                TimeUnit.MILLISECONDS
        );
    }

    private void monitorQueuePressure() {
        try {
            String queueInfo = batchingQueue.getQueueStatusInfo();
            double usageRatio = estimateQueueUsage(queueInfo);

            if (usageRatio > config.getHighPressureThreshold()) {
                adjustBatchingForHighPressure();
            } else if (usageRatio < config.getLowPressureThreshold()) {
                adjustBatchingForLowPressure();
            }
        } catch (Exception e) {
            logger.warn("Queue pressure monitoring failed: {}", e.getMessage());
        }
    }

    private double estimateQueueUsage(String queueInfo) {
        try {
            if (queueInfo.contains("remaining: ") && queueInfo.contains("capacity: ")) {
                String[] parts = queueInfo.split(",");
                long totalCapacity = 0;
                long remainingCapacity = 0;

                for (String part : parts) {
                    if (part.contains("capacity: ")) {
                        totalCapacity = Long.parseLong(part.split(": ")[1]);
                    } else if (part.contains("remaining: ")) {
                        remainingCapacity = Long.parseLong(part.split(": ")[1]);
                    }
                }

                if (totalCapacity > 0) {
                    return 1.0 - ((double) remainingCapacity / totalCapacity);
                }
            }
        } catch (Exception e) {
            logger.warn("Failed to parse queue usage: {}", e.getMessage());
        }
        return 0.2;
    }

    private void adjustBatchingForHighPressure() {
        logger.debug("High queue pressure detected, applying high pressure batching strategy");
    }

    private void adjustBatchingForLowPressure() {
        logger.debug("Low queue pressure detected, applying low pressure batching strategy");
    }
}
