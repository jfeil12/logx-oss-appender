package org.logx.core;

import org.logx.config.ConfigManager;

/**
 * 异步引擎配置构建器
 * <p>
 * 从系统属性、环境变量或配置文件中读取配置参数，构建AsyncEngineConfig实例。
 *
 * @author OSS Appender Team
 * @since 1.0.0
 */
public class AsyncEngineConfigBuilder {

    private static final String CONFIG_PREFIX = "logx.oss.engine.";
    
    /**
     * 从配置管理器构建AsyncEngineConfig
     *
     * @return AsyncEngineConfig实例
     */
    public static AsyncEngineConfig buildConfig() {
        return buildConfig(new ConfigManager());
    }
    
    /**
     * 从指定的配置管理器构建AsyncEngineConfig
     *
     * @param configManager 配置管理器
     * @return AsyncEngineConfig实例
     */
    public static AsyncEngineConfig buildConfig(ConfigManager configManager) {
        AsyncEngineConfig config = AsyncEngineConfig.defaultConfig();
        
        // 从配置管理器读取配置
        config.queueCapacity(configManager.getIntProperty(CONFIG_PREFIX + "queue.capacity", config.getQueueCapacity()));
        config.batchMaxMessages(configManager.getIntProperty(CONFIG_PREFIX + "batch.max.messages", config.getBatchMaxMessages()));
        config.batchMaxBytes(configManager.getIntProperty(CONFIG_PREFIX + "batch.max.bytes", config.getBatchMaxBytes()));
        config.maxMessageAgeMs(configManager.getLongProperty(CONFIG_PREFIX + "max.message.age.ms", config.getMaxMessageAgeMs()));
        config.blockOnFull(configManager.getBooleanProperty(CONFIG_PREFIX + "block.on.full", config.isBlockOnFull()));
        config.multiProducer(configManager.getBooleanProperty(CONFIG_PREFIX + "multi.producer", config.isMultiProducer()));
        config.corePoolSize(configManager.getIntProperty(CONFIG_PREFIX + "threadpool.core.size", config.getCorePoolSize()));
        config.maximumPoolSize(configManager.getIntProperty(CONFIG_PREFIX + "threadpool.max.size", config.getMaximumPoolSize()));
        config.queueCapacityThreadPool(configManager.getIntProperty(CONFIG_PREFIX + "threadpool.queue.capacity", config.getQueueCapacityThreadPool()));
        config.enableCpuYield(configManager.getBooleanProperty(CONFIG_PREFIX + "cpu.yield.enable", config.isEnableCpuYield()));
        config.enableMemoryProtection(configManager.getBooleanProperty(CONFIG_PREFIX + "memory.protection.enable", config.isEnableMemoryProtection()));
        config.maxShutdownWaitMs(configManager.getLongProperty(CONFIG_PREFIX + "shutdown.wait.ms", config.getMaxShutdownWaitMs()));
        config.logFileName(configManager.getProperty(CONFIG_PREFIX + "log.file.name", config.getLogFileName()));
        config.emergencyMemoryThresholdMb(configManager.getIntProperty(CONFIG_PREFIX + "emergency.memory.threshold.mb", config.getEmergencyMemoryThresholdMb()));
        config.uploadTimeoutMs(configManager.getLongProperty("logx.oss.storage.uploadTimeoutMs", config.getUploadTimeoutMs()));
        config.payloadMaxBytes(configManager.getIntProperty(CONFIG_PREFIX + "payload.max.bytes", config.getPayloadMaxBytes()));
        
        return config;
    }
}
