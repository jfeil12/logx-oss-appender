package org.logx.storage;

import java.util.concurrent.CompletableFuture;

/**
 * 存储服务接口
 * <p>
 * 定义所有存储服务的通用接口，支持多种存储后端（S3兼容存储、SF OSS等）。
 *
 * @author OSS Appender Team
 * @since 1.0.0
 */
public interface StorageService extends StorageInterface {

    /**
     * 上传单个对象
     *
     * @param key  对象键
     * @param data 对象数据
     * @return CompletableFuture表示异步操作结果
     */
    CompletableFuture<Void> putObject(String key, byte[] data);

    /**
     * 获取协议类型
     *
     * @return 协议类型枚举（S3或SF_OSS）
     */
    ProtocolType getProtocolType();

    /**
     * 获取存储桶名称
     *
     * @return 存储桶名称
     */
    String getBucketName();

    /**
     * 获取对象键前缀
     *
     * @return 对象键前缀
     */
    String getKeyPrefix();

    /**
     * 关闭存储服务，释放资源
     */
    void close();

    /**
     * 检查当前存储服务是否支持指定的协议类型
     *
     * @param protocol 协议类型枚举（S3或SF_OSS）
     * @return 是否支持该协议
     */
    boolean supportsProtocol(ProtocolType protocol);
}