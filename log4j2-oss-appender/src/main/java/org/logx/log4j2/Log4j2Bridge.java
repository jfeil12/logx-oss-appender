package org.logx.log4j2;

import org.logx.adapter.AbstractUniversalAdapter;
import org.logx.storage.StorageConfig;
import org.logx.storage.StorageService;
import org.logx.storage.StorageServiceFactory;
import org.logx.core.AsyncEngine;
import org.logx.core.AsyncEngineConfig;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.logx.core.LogPayloadSanitizer;

import java.io.Serializable;

/**
 * Log4j2 桥接器
 * 实现通用适配器接口，处理Log4j2特定的逻辑
 */
public class Log4j2Bridge extends AbstractUniversalAdapter {
    private static final Logger logger = LogManager.getLogger(Log4j2Bridge.class);
    private Layout<? extends Serializable> layout;
    private AsyncEngineConfig engineConfig;
    
    public Log4j2Bridge(StorageConfig config) {
        this(config, null);
    }
    
    public Log4j2Bridge(StorageConfig config, AsyncEngineConfig engineConfig) {
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
    
    public void setLayout(Layout<? extends Serializable> layout) {
        this.layout = layout;
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
                LogPayloadSanitizer.SanitizedPayload sanitized = LogPayloadSanitizer.sanitize(logLine, maxBytes);
                if (sanitized.sanitized || sanitized.truncated) {
                    logger.warn("Log4j2 payload sanitized={}, truncated={}, originalBytes={}",
                            sanitized.sanitized, sanitized.truncated, sanitized.originalBytes);
                }
                asyncEngine.put(sanitized.bytes);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to process log event", e);
        }
    }
    
    /**
     * 将Log4j2事件转换为字符串
     */
    private String convertEvent(Object event) {
        if (!(event instanceof LogEvent)) {
            return null;
        }
        
        LogEvent logEvent = (LogEvent) event;
        
        // 使用Layout格式化日志
        if (layout != null) {
            byte[] bytes = layout.toByteArray(logEvent);
            return new String(bytes, java.nio.charset.StandardCharsets.UTF_8);
        } else {
            // 默认格式
            return logEvent.getMessage().getFormattedMessage() + "\n";
        }
    }
}
