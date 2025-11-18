package org.logx.storage.s3;

import org.logx.storage.ProtocolType;
import org.logx.storage.StorageConfig;
import org.logx.storage.StorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.net.URI;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * S3存储服务适配器实现
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
 * @since 1.0.0
 */
public final class S3StorageServiceAdapter implements StorageService, AutoCloseable {

    private static final Logger logger = LoggerFactory.getLogger(S3StorageServiceAdapter.class);
    /**
     * 适配器类型：标准S3协议
     */
    private static final ProtocolType ADAPTER_TYPE = ProtocolType.S3;

    private S3Client s3Client;
    private String bucketName;
    private String keyPrefix;
    private String endpoint;
    private String ossType;

    /**
     * 无参构造函数（用于SPI实例化）
     */
    public S3StorageServiceAdapter() {
        // 无参构造函数用于SPI实例化
    }

    /**
     * 构造S3存储适配器
     *
     * @param config 存储配置
     */
    public S3StorageServiceAdapter(StorageConfig config) {
        initialize(config);
    }

    /**
     * 初始化存储服务
     * @param config 存储配置
     */
    public void initialize(StorageConfig config) {
        if (this.s3Client != null) {
            return; // 已经初始化
        }
        
        String region = config.getRegion();
        String accessKeyId = config.getAccessKeyId();
        String secretAccessKey = config.getAccessKeySecret();
        String bucketName = config.getBucket();
        String endpoint = config.getEndpoint();
        this.bucketName = bucketName;
        this.keyPrefix = config.getKeyPrefix() != null ? config.getKeyPrefix().replaceAll("^/+|/+$", "") : "logx";
        this.endpoint = endpoint;
        this.ossType = config.getOssType();

        // 构建S3客户端，完整应用config中的所有配置
        S3ClientBuilder clientBuilder = S3Client.builder()
                .credentialsProvider(
                        StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKeyId, secretAccessKey)))
                .region(Region.of(region != null ? region : "US"));

        // 设置自定义endpoint（MinIO、SF OSS等）
        if (endpoint != null && !endpoint.trim().isEmpty()) {
            try {
                clientBuilder.endpointOverride(URI.create(endpoint));
            } catch (Exception e) {
                logger.warn("Invalid endpoint URI: {}. Ignoring endpoint override.", endpoint);
            }
        }

        // 只有当pathStyleAccess为true时才显式设置S3配置
        // false时使用SDK默认行为（虚拟主机风格）
        if (config.isPathStyleAccess()) {
            S3Configuration s3Config = S3Configuration.builder()
                    .pathStyleAccessEnabled(true)
                    .build();
            clientBuilder.serviceConfiguration(s3Config);
        }

        this.s3Client = clientBuilder.build();
    }

    @Override
    public CompletableFuture<Void> putObject(String key, byte[] data) {
        ensureInitialized();
        
        if (key == null || key.trim().isEmpty()) {
            CompletableFuture<Void> future = new CompletableFuture<>();
            future.completeExceptionally(new IllegalArgumentException("Key cannot be null or empty"));
            return future;
        }
        if (data == null) {
            CompletableFuture<Void> future = new CompletableFuture<>();
            future.completeExceptionally(new IllegalArgumentException("Data cannot be null"));
            return future;
        }

        try {
            // 检查客户端是否已关闭
            if (s3Client == null) {
                throw new IllegalStateException("S3 client has been closed");
            }
            
            // 设置正确的Content-Type
            String contentType = "text/plain; charset=utf-8";
            // 如果数据是gzip压缩的，设置相应的Content-Type
            if (data.length > 2 && data[0] == (byte) 0x1f && data[1] == (byte) 0x8b) {
                contentType = "application/gzip";
            }

            PutObjectRequest.Builder requestBuilder = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .contentLength((long) data.length)
                    .contentType(contentType);

            // SF S3特殊处理：设置文件有效期元数据，默认保存一年
            // 当ossType为SF_S3时，需要设置X-Delete-After元数据
            if ("SF_S3".equals(ossType)) {
                Map<String, String> metadata = new HashMap<>();
                metadata.put("X-Delete-After", "157680000");
                requestBuilder.metadata(metadata);
            }

            PutObjectRequest putRequest = requestBuilder.build();
            RequestBody requestBody = RequestBody.fromBytes(data);

            // 同步执行上传（调用方已经在uploadExecutor线程中）
            s3Client.putObject(putRequest, requestBody);
            
            logger.debug("Successfully uploaded object: endpoint={}, bucket={}, key={}, size={} bytes",
                endpoint, bucketName, key, data.length);
            
            return CompletableFuture.completedFuture(null);
            
        } catch (IllegalStateException e) {
            // 检查是否是连接池关闭的异常
            CompletableFuture<Void> future = new CompletableFuture<>();
            if (e.getMessage() != null && e.getMessage().contains("Connection pool shut down")) {
                future.completeExceptionally(
                    new IllegalStateException("S3 client connection pool has been shut down", e));
            } else {
                future.completeExceptionally(e);
            }
            return future;
        } catch (Exception e) {
            // 记录详细的错误信息用于排查
            logger.error("Failed to upload object to S3. Endpoint: {}, Bucket: {}, Key: {}, Size: {} bytes, Error: {}",
                endpoint, bucketName, key, data.length, e.getMessage());
            // 返回失败的Future，由核心层处理重试和错误处理
            CompletableFuture<Void> future = new CompletableFuture<>();
            future.completeExceptionally(
                new RuntimeException("Failed to upload object to S3: " + e.getMessage(), e));
            return future;
        }
    }

    @Override
    public ProtocolType getProtocolType() {
        return ADAPTER_TYPE;
    }

    @Override
    public String getBucketName() {
        ensureInitialized();
        return bucketName;
    }

    @Override
    public String getKeyPrefix() {
        ensureInitialized();
        return keyPrefix;
    }

    /**
     * 确保存储服务已初始化
     */
    private void ensureInitialized() {
        if (s3Client == null) {
            throw new IllegalStateException("Storage service not initialized or has been closed. Use StorageServiceFactory instead.");
        }
    }

    @Override
    public void close() {
        if (s3Client != null) {
            s3Client.close();
            s3Client = null; // 设置为null，以便后续检查
        }
    }

    @Override
    public boolean supportsProtocol(ProtocolType protocol) {
        return ADAPTER_TYPE == protocol;
    }

}