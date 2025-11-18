package org.logx.logback;

import org.logx.adapter.AbstractUniversalAdapter;
import org.logx.storage.StorageConfig;
import org.logx.storage.StorageService;
import org.logx.storage.StorageServiceFactory;
import org.logx.core.AsyncEngine;
import org.logx.core.AsyncEngineConfig;
import org.logx.core.LogPayloadSanitizer;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.encoder.Encoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Logback 桥接器
 * 实现通用适配器接口，处理Logback特定的逻辑
 */
public class LogbackBridge extends AbstractUniversalAdapter {
    private static final Logger logger = LoggerFactory.getLogger(LogbackBridge.class);
    private Encoder<ILoggingEvent> encoder;
    private AsyncEngineConfig engineConfig;
    
    public LogbackBridge(StorageConfig config) {
        this(config, null);
    }
    
    public LogbackBridge(StorageConfig config, AsyncEngineConfig engineConfig) {
        // 使用存储服务工厂创建存储服务
        StorageService storageService = StorageServiceFactory.createStorageService(config);

        // 从存储服务获取存储接口
        this.s3Storage = storageService;

        // 保存引擎配置
        this.engineConfig = engineConfig;

        // 设置StorageConfig到引擎配置
        if (engineConfig != null) {
            engineConfig.setStorageConfig(config);
            this.asyncEngine = AsyncEngine.create(storageService, engineConfig);
        } else {
            AsyncEngineConfig defaultConfig = AsyncEngineConfig.defaultConfig();
            defaultConfig.setStorageConfig(config);
            this.asyncEngine = AsyncEngine.create(storageService, defaultConfig);
        }
    }
    
    public void setEncoder(Encoder<ILoggingEvent> encoder) {
        this.encoder = encoder;
    }
    
    public void setEngineConfig(AsyncEngineConfig engineConfig) {
        this.engineConfig = engineConfig;
        // Note: In a real implementation, we would need to recreate the asyncEngine
        // when the configuration changes, but for now we're just storing the config
    }

    public AsyncEngineConfig getEngineConfig() {
        return engineConfig;
    }
    
    @Override
    public void append(Object event) {
        if (!isStarted() || asyncEngine == null) {
            return;
        }
        
        try {
            String logLine = convertEvent(event);
            if (logLine != null) {
                int maxBytes = engineConfig != null ? engineConfig.getPayloadMaxBytes() : 512 * 1024;
                LogPayloadSanitizer.SanitizedPayload sanitized =
                        LogPayloadSanitizer.sanitize(logLine, maxBytes);
                if (sanitized.sanitized || sanitized.truncated) {
                    logger.warn("Logback payload sanitized={}, truncated={}, originalBytes={}",
                            sanitized.sanitized, sanitized.truncated, sanitized.originalBytes);
                }
                asyncEngine.put(sanitized.bytes);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to process log event", e);
        }
    }
    
    /**
     * 将Logback事件转换为字符串
     */
    private String convertEvent(Object event) {
        if (!(event instanceof ILoggingEvent)) {
            return null;
        }
        
        ILoggingEvent loggingEvent = (ILoggingEvent) event;
        
        if (encoder != null) {
            try {
                byte[] encoded = encoder.encode(loggingEvent);
                return new String(encoded, java.nio.charset.StandardCharsets.UTF_8);
            } catch (Exception e) {
                return loggingEvent.getFormattedMessage() + "\n";
            }
        }
        return loggingEvent.getFormattedMessage() + "\n";
    }
}
