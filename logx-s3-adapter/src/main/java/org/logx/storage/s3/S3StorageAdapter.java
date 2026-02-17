package org.logx.storage.s3;

import org.logx.storage.ProtocolType;
import org.logx.storage.StorageInterface;
import org.logx.storage.StorageConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;


/**
 * AWS S3存储适配器实现
 * <p>
 * 基于AWS SDK v2实现的S3存储适配器，支持标准AWS S3服务。
 * 提供基本的单个上传功能，分片和重试等通用功能由核心层统一处理。
 * <p>
 * 注意：根据2025-09-24的架构变更，该适配器不再处理数据分片逻辑和重试机制，
 * 这些功能已移至核心层的BatchProcessor中统一处理。
 * <p>
 * 主要特性：
 * <ul>
 * <li>基本上传功能：处理单个对象的上传</li>
 * <li>AWS特定配置和认证</li>
 * <li>区域配置和键前缀处理</li>
 * </ul>
 *
 * @author OSS Appender Team
 *
 * @since 1.0.0
 */
public final class S3StorageAdapter implements StorageInterface, AutoCloseable {

    private static final Logger logger = LoggerFactory.getLogger(S3StorageAdapter.class);
    private static final ProtocolType PROTOCOL_TYPE = ProtocolType.S3;
    private static final Region DEFAULT_REGION = Region.US_EAST_1;

    private final S3Client s3Client;
    private final String bucketName;
    private final String keyPrefix;

    /**
     * 构造S3存储适配器
     *
     * @param config 存储配置
     */
    public S3StorageAdapter(StorageConfig config) {
        Objects.requireNonNull(config, "StorageConfig cannot be null");

        String accessKeyId = requireNonBlank(config.getAccessKeyId(), "AccessKeyId");
        String secretAccessKey = requireNonBlank(config.getAccessKeySecret(), "AccessKeySecret");
        this.bucketName = requireNonBlank(config.getBucket(), "Bucket");
        this.keyPrefix = normalizeKeyPrefix(config.getKeyPrefix());

        Region awsRegion = resolveRegion(config.getRegion());

        // 构建S3客户端 - 标准AWS S3配置
        this.s3Client = S3Client.builder()
                .credentialsProvider(
                        StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKeyId, secretAccessKey)))
                .region(awsRegion).build();
    }

    private Region resolveRegion(String regionValue) {
        String trimmed = requireNonBlank(regionValue, "Region");
        try {
            return Region.of(trimmed);
        } catch (Exception e) {
            logger.warn("Invalid region '{}', falling back to {}", trimmed, DEFAULT_REGION.id());
            return DEFAULT_REGION;
        }
    }

    private static String requireNonBlank(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(fieldName + " cannot be null or empty");
        }
        return value.trim();
    }

    private static String normalizeKeyPrefix(String keyPrefix) {
        if (keyPrefix == null || keyPrefix.trim().isEmpty()) {
            return "logx";
        }
        return keyPrefix.trim().replaceAll("^/+|/+$", "");
    }

    @Override
    public CompletableFuture<Void> putObject(String key, byte[] data) {
        if (data == null) {
            CompletableFuture<Void> future = new CompletableFuture<>();
            future.completeExceptionally(new IllegalArgumentException("Data cannot be null"));
            return future;
        }
        return putObject(key, ByteBuffer.wrap(data));
    }

    @Override
    public CompletableFuture<Void> putObject(String key, ByteBuffer buffer) {
        if (key == null || key.trim().isEmpty()) {
            CompletableFuture<Void> future = new CompletableFuture<>();
            future.completeExceptionally(new IllegalArgumentException("Key cannot be null or empty"));
            return future;
        }
        if (buffer == null) {
            CompletableFuture<Void> future = new CompletableFuture<>();
            future.completeExceptionally(new IllegalArgumentException("Buffer cannot be null"));
            return future;
        }

        ByteBuffer readOnlyBuffer = buffer.asReadOnlyBuffer();
        return putObjectInternal(key, RequestBody.fromByteBuffer(readOnlyBuffer), readOnlyBuffer.remaining());
    }

    @Override
    public CompletableFuture<Void> putObject(String key, InputStream inputStream, long dataLength) {
        if (key == null || key.trim().isEmpty()) {
            CompletableFuture<Void> future = new CompletableFuture<>();
            future.completeExceptionally(new IllegalArgumentException("Key cannot be null or empty"));
            return future;
        }
        if (inputStream == null) {
            CompletableFuture<Void> future = new CompletableFuture<>();
            future.completeExceptionally(new IllegalArgumentException("Input stream cannot be null"));
            return future;
        }

        if (dataLength < 0 || dataLength > Integer.MAX_VALUE) {
            CompletableFuture<Void> future = new CompletableFuture<>();
            future.completeExceptionally(new IllegalArgumentException("Data length out of range: " + dataLength));
            return future;
        }

        return putObjectInternal(key, RequestBody.fromInputStream(inputStream, dataLength), (int) dataLength);
    }

    private CompletableFuture<Void> putObjectInternal(String key, RequestBody requestBody, int dataLength) {

        // ObjectNameGenerator已经生成完整路径，直接使用
        // 执行标准上传，不处理分片和重试，这些由核心层处理
        return CompletableFuture.runAsync(() -> {
            PutObjectRequest putRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .contentLength((long) dataLength)
                    .build();
            
            try {
                s3Client.putObject(putRequest, requestBody);
            } catch (Exception e) {
                // 直接抛出异常，由核心层处理重试和错误处理
                throw new RuntimeException("Failed to upload object to S3: " + e.getMessage(), e);
            }
        });
    }

    @Override
    public ProtocolType getProtocolType() {
        return PROTOCOL_TYPE;
    }

    @Override
    public String getBucketName() {
        return bucketName;
    }

    @Override
    public String getKeyPrefix() {
        return keyPrefix;
    }

    @Override
    public void close() {
        if (s3Client != null) {
            s3Client.close();
        }
    }
}
