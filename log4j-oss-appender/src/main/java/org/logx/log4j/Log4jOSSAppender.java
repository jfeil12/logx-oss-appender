package org.logx.log4j;

import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.Logger;
import org.apache.log4j.spi.LoggingEvent;
import org.logx.config.ConfigManager;
import org.logx.config.properties.LogxOssProperties;
import org.logx.config.validation.ConfigValidationUtils;
import org.logx.core.AsyncEngineConfig;
import org.logx.storage.StorageConfig;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public class Log4jOSSAppender extends AppenderSkeleton {

    private static final Logger logger = Logger.getLogger(Log4jOSSAppender.class);

    private Log4j1xBridge adapter;

    // 全局初始化标志，防止重复初始化
    private static final AtomicBoolean alreadyInitialized = new AtomicBoolean(false);

    // XML配置字段
    private final Map<String, String> xmlConfig = new HashMap<>();

    @Override
    public void activateOptions() {
        super.activateOptions();

        // 检查是否已经初始化过
        if (alreadyInitialized.get()) {
            logger.info("Log4jOSSAppender already initialized, skipping initialization");
            return;
        }

        // 尝试设置为已初始化状态
        if (!alreadyInitialized.compareAndSet(false, true)) {
            logger.info("Log4jOSSAppender already initialized by another thread, skipping initialization");
            return;
        }

        try {
            ConfigManager configManager = new ConfigManager();
            LogxOssProperties properties = configManager.getLogxOssProperties();

            // 预先解析xmlConfig中的所有占位符
            ConfigManager.resolveMapPlaceholders(xmlConfig);

            // 应用XML配置（如果有的话）
            applyXmlConfig(properties);

            // 检查enabled开关，如果为false则不初始化任何资源
            if (!properties.isEnabled()) {
                logger.info("Log4jOSSAppender is disabled by configuration (logx.oss.enabled=false), skipping initialization");
                return;
            }

            ConfigValidationUtils.validateRequiredStorage(properties, msg -> logger.error(msg));

            StorageConfig storageConfig = new StorageConfig(properties);

            AsyncEngineConfig engineConfig = AsyncEngineConfig.defaultConfig();
            engineConfig.enabled(properties.isEnabled());
            engineConfig.queueCapacity(properties.getEngine().getQueue().getCapacity());
            engineConfig.batchMaxMessages(properties.getEngine().getBatch().getCount());
            engineConfig.batchMaxBytes(properties.getEngine().getBatch().getBytes());
            engineConfig.maxMessageAgeMs(properties.getEngine().getBatch().getMaxAgeMs());
            engineConfig.blockOnFull(!properties.getEngine().getQueue().isDropWhenFull());
            engineConfig.uploadTimeoutMs(properties.getStorage().getUploadTimeoutMs());
            engineConfig.payloadMaxBytes(properties.getEngine().getPayloadMaxBytes());

            this.adapter = new Log4j1xBridge(storageConfig, engineConfig);
            this.adapter.setLayout(layout);
            this.adapter.start();

        } catch (Exception e) {
            getErrorHandler().error("Failed to initialize Log4jOSSAppender: " + e.getMessage(), e, 1);
        }
    }

    private void applyXmlConfig(LogxOssProperties properties) {
        // 全局配置 - enabled开关
        if (xmlConfig.containsKey("logx.oss.enabled")) {
            properties.setEnabled(Boolean.parseBoolean(xmlConfig.get("logx.oss.enabled")));
        }

        // 存储配置（占位符已在resolveXmlConfigPlaceholders中解析）
        if (xmlConfig.containsKey("logx.oss.storage.endpoint")) {
            properties.getStorage().setEndpoint(xmlConfig.get("logx.oss.storage.endpoint"));
        }
        if (xmlConfig.containsKey("logx.oss.storage.region")) {
            properties.getStorage().setRegion(xmlConfig.get("logx.oss.storage.region"));
        }
        if (xmlConfig.containsKey("logx.oss.storage.accessKeyId")) {
            properties.getStorage().setAccessKeyId(xmlConfig.get("logx.oss.storage.accessKeyId"));
        }
        if (xmlConfig.containsKey("logx.oss.storage.accessKeySecret")) {
            properties.getStorage().setAccessKeySecret(xmlConfig.get("logx.oss.storage.accessKeySecret"));
        }
        if (xmlConfig.containsKey("logx.oss.storage.bucket")) {
            properties.getStorage().setBucket(xmlConfig.get("logx.oss.storage.bucket"));
        }
        if (xmlConfig.containsKey("logx.oss.storage.keyPrefix")) {
            properties.getStorage().setKeyPrefix(xmlConfig.get("logx.oss.storage.keyPrefix"));
        }
        if (xmlConfig.containsKey("logx.oss.storage.ossType")) {
            properties.getStorage().setOssType(xmlConfig.get("logx.oss.storage.ossType"));
        }
        if (xmlConfig.containsKey("logx.oss.storage.pathStyleAccess")) {
            properties.getStorage().setPathStyleAccess(Boolean.parseBoolean(xmlConfig.get("logx.oss.storage.pathStyleAccess")));
        }
        if (xmlConfig.containsKey("logx.oss.storage.uploadTimeoutMs")) {
            properties.getStorage().setUploadTimeoutMs(Long.parseLong(xmlConfig.get("logx.oss.storage.uploadTimeoutMs")));
        }

        // 引擎配置 - 批处理
        if (xmlConfig.containsKey("logx.oss.engine.batch.count")) {
            properties.getEngine().getBatch().setCount(Integer.parseInt(xmlConfig.get("logx.oss.engine.batch.count")));
        }
        if (xmlConfig.containsKey("logx.oss.engine.batch.bytes")) {
            properties.getEngine().getBatch().setBytes(Integer.parseInt(xmlConfig.get("logx.oss.engine.batch.bytes")));
        }
        if (xmlConfig.containsKey("logx.oss.engine.batch.maxAgeMs")) {
            properties.getEngine().getBatch().setMaxAgeMs(Long.parseLong(xmlConfig.get("logx.oss.engine.batch.maxAgeMs")));
        }

        // 引擎配置 - 队列
        if (xmlConfig.containsKey("logx.oss.engine.queue.capacity")) {
            properties.getEngine().getQueue().setCapacity(Integer.parseInt(xmlConfig.get("logx.oss.engine.queue.capacity")));
        }
        if (xmlConfig.containsKey("logx.oss.engine.queue.dropWhenFull")) {
            properties.getEngine().getQueue().setDropWhenFull(Boolean.parseBoolean(xmlConfig.get("logx.oss.engine.queue.dropWhenFull")));
        }

        // 引擎配置 - 重试
        if (xmlConfig.containsKey("logx.oss.engine.retry.maxRetries")) {
            properties.getEngine().getRetry().setMaxRetries(Integer.parseInt(xmlConfig.get("logx.oss.engine.retry.maxRetries")));
        }
        if (xmlConfig.containsKey("logx.oss.engine.retry.baseBackoffMs")) {
            properties.getEngine().getRetry().setBaseBackoffMs(Long.parseLong(xmlConfig.get("logx.oss.engine.retry.baseBackoffMs")));
        }
        if (xmlConfig.containsKey("logx.oss.engine.retry.maxBackoffMs")) {
            properties.getEngine().getRetry().setMaxBackoffMs(Long.parseLong(xmlConfig.get("logx.oss.engine.retry.maxBackoffMs")));
        }

        if (xmlConfig.containsKey("logx.oss.engine.payload.maxBytes")) {
            properties.getEngine().setPayloadMaxBytes(Integer.parseInt(xmlConfig.get("logx.oss.engine.payload.maxBytes")));
        }
    }

    @Override
    protected void append(LoggingEvent event) {
        if (!isAsSevereAsThreshold(event.getLevel()) || adapter == null) {
            return;
        }

        try {
            adapter.append(event);
        } catch (Exception e) {
            getErrorHandler().error("Failed to append log event", e, 1, event);
        }
    }

    @Override
    public void close() {
        if (closed) {
            return;
        }

        try {
            if (adapter != null) {
                adapter.stop();
            }
        } catch (Exception e) {
            getErrorHandler().error("Failed to close OSSAppender", e, 1);
        }

        closed = true;
    }

    @Override
    public boolean requiresLayout() {
        return true;
    }

    // Setter方法保存XML配置，使用新格式
    public void setEndpoint(String endpoint) {
        xmlConfig.put("logx.oss.storage.endpoint", endpoint);
    }

    public void setRegion(String region) {
        xmlConfig.put("logx.oss.storage.region", region);
    }

    public void setAccessKeyId(String accessKeyId) {
        xmlConfig.put("logx.oss.storage.accessKeyId", accessKeyId);
    }

    public void setAccessKeySecret(String accessKeySecret) {
        xmlConfig.put("logx.oss.storage.accessKeySecret", accessKeySecret);
    }

    public void setBucket(String bucket) {
        xmlConfig.put("logx.oss.storage.bucket", bucket);
    }

    public void setKeyPrefix(String keyPrefix) {
        xmlConfig.put("logx.oss.storage.keyPrefix", keyPrefix);
    }

    public void setOssType(String ossType) {
        xmlConfig.put("logx.oss.storage.ossType", ossType);
    }

    public void setQueueCapacity(String queueCapacity) {
        xmlConfig.put("logx.oss.engine.queue.capacity", queueCapacity);
    }

    public void setMaxBatchCount(String maxBatchCount) {
        xmlConfig.put("logx.oss.engine.batch.count", maxBatchCount);
    }

    public void setMaxBatchBytes(String maxBatchBytes) {
        xmlConfig.put("logx.oss.engine.batch.bytes", maxBatchBytes);
    }

    public void setMaxMessageAgeMs(String maxMessageAgeMs) {
        xmlConfig.put("logx.oss.engine.batch.maxAgeMs", maxMessageAgeMs);
    }

    public void setDropWhenQueueFull(String dropWhenQueueFull) {
        xmlConfig.put("logx.oss.engine.queue.dropWhenFull", dropWhenQueueFull);
    }

    public void setMaxRetries(String maxRetries) {
        xmlConfig.put("logx.oss.engine.retry.maxRetries", maxRetries);
    }

    public void setBaseBackoffMs(String baseBackoffMs) {
        xmlConfig.put("logx.oss.engine.retry.baseBackoffMs", baseBackoffMs);
    }

    public void setMaxBackoffMs(String maxBackoffMs) {
        xmlConfig.put("logx.oss.engine.retry.maxBackoffMs", maxBackoffMs);
    }

    public void setPathStyleAccess(String pathStyleAccess) {
        xmlConfig.put("logx.oss.storage.pathStyleAccess", pathStyleAccess);
    }

    public void setEnabled(String enabled) {
        xmlConfig.put("logx.oss.enabled", enabled);
    }
}
