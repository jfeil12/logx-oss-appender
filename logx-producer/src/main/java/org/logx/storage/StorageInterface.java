package org.logx.storage;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/**
 * 存储接口
 * <p>
 * 定义所有存储服务的通用接口，支持多种存储后端（S3兼容存储、SF OSS等）。
 *
 * @author OSS Appender Team
 *
 * @since 1.0.0
 */
public interface StorageInterface {

    /**
     * 上传单个对象
     *
     * @param key
     *            对象键
     * @param data
     *            对象数据
     *
     * @return CompletableFuture表示异步操作结果
     */
    CompletableFuture<Void> putObject(String key, byte[] data);

    /**
     * 上传单个对象（InputStream）。
     *
     * @param key         对象键
     * @param inputStream 对象数据输入流
     * @param dataLength  数据长度
     * @return CompletableFuture表示异步操作结果
     */
    default CompletableFuture<Void> putObject(String key, InputStream inputStream, long dataLength) {
        Objects.requireNonNull(inputStream, "Input stream cannot be null");
        if (dataLength < 0 || dataLength > Integer.MAX_VALUE) {
            CompletableFuture<Void> future = new CompletableFuture<>();
            future.completeExceptionally(new IllegalArgumentException("Data length out of range: " + dataLength));
            return future;
        }

        byte[] payload = new byte[(int) dataLength];
        try {
            int offset = 0;
            while (offset < payload.length) {
                int read = inputStream.read(payload, offset, payload.length - offset);
                if (read < 0) {
                    throw new IllegalArgumentException("Input stream ended before expected length: " + dataLength);
                }
                offset += read;
            }
            return putObject(key, payload);
        } catch (Exception ex) {
            CompletableFuture<Void> future = new CompletableFuture<>();
            future.completeExceptionally(ex);
            return future;
        }
    }

    /**
     * 上传单个对象（ByteBuffer）。
     *
     * @param key    对象键
     * @param buffer 对象数据缓冲区
     * @return CompletableFuture表示异步操作结果
     */
    default CompletableFuture<Void> putObject(String key, ByteBuffer buffer) {
        Objects.requireNonNull(buffer, "Buffer cannot be null");
        ByteBuffer readOnlyBuffer = buffer.asReadOnlyBuffer();
        byte[] payload = new byte[readOnlyBuffer.remaining()];
        readOnlyBuffer.get(payload);
        return putObject(key, new ByteArrayInputStream(payload), payload.length);
    }

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
}
