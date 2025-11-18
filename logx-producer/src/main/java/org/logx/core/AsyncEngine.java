package org.logx.core;

import org.logx.storage.StorageService;

import java.util.concurrent.TimeUnit;

/**
 * 异步处理引擎接口
 * <p>
 * 定义了异步日志处理引擎的核心API，负责接收日志数据、 启动和停止处理流程，并提供优雅停机支持。
 *
 * @author OSS Appender Team
 *
 * @since 1.0.0
 */
public interface AsyncEngine {

    /**
     * 启动异步引擎
     */
    void start();

    /**
     * 停止异步引擎
     *
     * @param timeout
     *            停机超时时间
     * @param timeUnit
     *            超时时间单位
     */
    void stop(long timeout, TimeUnit timeUnit);

    /**
     * 将日志数据放入处理队列
     *
     * @param data
     *            日志数据
     */
    void put(byte[] data);

    /**
     * 创建并返回一个AsyncEngine的实例
     *
     * @param config
     *            异步引擎配置
     *
     * @return AsyncEngine的实例
     */
    static AsyncEngine create(AsyncEngineConfig config) {
        return new AsyncEngineImpl(config);
    }
    
    /**
     * 创建并返回一个AsyncEngine的实例
     *
     * @param config
     *            异步引擎配置
     *
     * @return AsyncEngine的实例
     */
    static AsyncEngine create(StorageService storage, AsyncEngineConfig config) {
        return new AsyncEngineImpl(config, storage);
    }
    
    /**
     * 创建并返回一个AsyncEngine的实例，使用默认配置构建器
     *
     * @return AsyncEngine的实例
     */
    static AsyncEngine createWithConfig() {
        return new AsyncEngineImpl(AsyncEngineConfigBuilder.buildConfig());
    }
}