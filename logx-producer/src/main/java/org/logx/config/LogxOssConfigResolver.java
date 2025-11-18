package org.logx.config;

import org.logx.config.properties.LogxOssProperties;

public class LogxOssConfigResolver {

    private final ConfigManager configManager;

    public LogxOssConfigResolver(ConfigManager configManager) {
        this.configManager = configManager;
    }

    public LogxOssProperties resolve() {
        LogxOssProperties properties = new LogxOssProperties();
        resolveEnabled(properties);
        resolveStorage(properties.getStorage());
        resolveEngine(properties.getEngine());
        return properties;
    }

    private String resolve(String value) {
        return configManager.resolvePlaceholders(value);
    }

    private void resolveEnabled(LogxOssProperties properties) {
        properties.setEnabled(configManager.getBooleanProperty("logx.oss.enabled", properties.isEnabled()));
    }

    private void resolveStorage(LogxOssProperties.Storage storage) {
        storage.setOssType(resolve(configManager.getProperty("logx.oss.storage.ossType", storage.getOssType())));
        storage.setEndpoint(resolve(configManager.getProperty("logx.oss.storage.endpoint")));
        storage.setRegion(resolve(configManager.getProperty("logx.oss.storage.region")));
        storage.setAccessKeyId(resolve(configManager.getProperty("logx.oss.storage.accessKeyId")));
        storage.setAccessKeySecret(resolve(configManager.getProperty("logx.oss.storage.accessKeySecret")));
        storage.setBucket(resolve(configManager.getProperty("logx.oss.storage.bucket")));
        storage.setKeyPrefix(resolve(configManager.getProperty("logx.oss.storage.keyPrefix", storage.getKeyPrefix())));
        storage.setPathStyleAccess(configManager.getBooleanProperty("logx.oss.storage.pathStyleAccess", storage.isPathStyleAccess()));
        // Default values
        if(storage.getOssType() == null) {
            storage.setOssType("sf_s3");
        }
        if(storage.getRegion() == null) {
            storage.setRegion("US");
        }
        // Personalized default values
        if ("minio".equalsIgnoreCase(storage.getOssType()) || "sf_s3".equalsIgnoreCase(storage.getOssType())) {
            storage.setPathStyleAccess(true);
        } else if ("aws_s3".equalsIgnoreCase(storage.getOssType())) {
            if (storage.getEndpoint() == null) {
                storage.setEndpoint("s3.amazonaws.com");
            }
        }
    }

    private void resolveEngine(LogxOssProperties.Engine engine) {
        resolveBatch(engine.getBatch());
        resolveRetry(engine.getRetry());
        resolveQueue(engine.getQueue());
        resolveFallback(engine.getFallback());
        resolveThreadPool(engine.getThreadPool());
        resolveOtherEngineConfigs(engine);
    }

    private void resolveBatch(LogxOssProperties.Batch batch) {
        batch.setCount(configManager.getIntProperty("logx.oss.engine.batch.count", batch.getCount()));
        batch.setBytes(configManager.getIntProperty("logx.oss.engine.batch.bytes", batch.getBytes()));
        batch.setMaxAgeMs(configManager.getLongProperty("logx.oss.engine.batch.maxAgeMs", batch.getMaxAgeMs()));
    }

    private void resolveRetry(LogxOssProperties.Retry retry) {
        retry.setMaxRetries(configManager.getIntProperty("logx.oss.engine.retry.maxRetries", retry.getMaxRetries()));
        retry.setBaseBackoffMs(configManager.getLongProperty("logx.oss.engine.retry.baseBackoffMs", retry.getBaseBackoffMs()));
        retry.setMaxBackoffMs(configManager.getLongProperty("logx.oss.engine.retry.maxBackoffMs", retry.getMaxBackoffMs()));
    }

    private void resolveQueue(LogxOssProperties.Queue queue) {
        queue.setCapacity(configManager.getIntProperty("logx.oss.engine.queue.capacity", queue.getCapacity()));
        queue.setDropWhenFull(configManager.getBooleanProperty("logx.oss.engine.queue.dropWhenFull", queue.isDropWhenFull()));
    }

    private void resolveFallback(LogxOssProperties.Fallback fallback) {
        fallback.setPath(resolve(configManager.getProperty("logx.oss.engine.fallback.path", fallback.getPath())));
        fallback.setRetentionDays(configManager.getIntProperty("logx.oss.engine.fallback.retentionDays", fallback.getRetentionDays()));
        fallback.setScanIntervalSeconds(configManager.getIntProperty("logx.oss.engine.fallback.scanIntervalSeconds", fallback.getScanIntervalSeconds()));
    }

    private void resolveThreadPool(LogxOssProperties.ThreadPool threadPool) {
        threadPool.setCorePoolSize(configManager.getIntProperty("logx.oss.engine.threadPool.corePoolSize", threadPool.getCorePoolSize()));
        threadPool.setMaximumPoolSize(configManager.getIntProperty("logx.oss.engine.threadPool.maximumPoolSize", threadPool.getMaximumPoolSize()));
    }

    private void resolveOtherEngineConfigs(LogxOssProperties.Engine engine) {
        engine.setMultiProducer(configManager.getBooleanProperty("logx.oss.engine.multiProducer", engine.isMultiProducer()));
        engine.setEnableCpuYield(configManager.getBooleanProperty("logx.oss.engine.enableCpuYield", engine.isEnableCpuYield()));
        engine.setEnableMemoryProtection(configManager.getBooleanProperty("logx.oss.engine.enableMemoryProtection", engine.isEnableMemoryProtection()));
        engine.setMaxShutdownWaitMs(configManager.getLongProperty("logx.oss.engine.maxShutdownWaitMs", engine.getMaxShutdownWaitMs()));
        engine.setLogFileName(resolve(configManager.getProperty("logx.oss.engine.logFileName", engine.getLogFileName())));
        engine.setEmergencyMemoryThresholdMb(configManager.getIntProperty("logx.oss.engine.emergencyMemoryThresholdMb", engine.getEmergencyMemoryThresholdMb()));
        engine.setEnableCompression(configManager.getBooleanProperty("logx.oss.engine.enableCompression", engine.isEnableCompression()));
        engine.setCompressionThreshold(configManager.getIntProperty("logx.oss.engine.compressionThreshold", engine.getCompressionThreshold()));
        engine.setEnableSharding(configManager.getBooleanProperty("logx.oss.engine.enableSharding", engine.isEnableSharding()));
        engine.setMaxUploadSizeMb(configManager.getIntProperty("logx.oss.engine.maxUploadSizeMb", engine.getMaxUploadSizeMb()));
    }
}