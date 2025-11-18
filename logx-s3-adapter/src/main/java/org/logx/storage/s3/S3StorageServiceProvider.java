package org.logx.storage.s3;

import org.logx.storage.ProtocolType;
import org.logx.storage.StorageConfig;
import org.logx.storage.StorageService;
import java.util.concurrent.CompletableFuture;

/**
 * S3存储服务提供者实现
 * <p>
 * 这个类是专门为Java SPI机制设计的，提供无参构造函数。
 * 它会创建实际的S3StorageAdapter实例。
 *
 * @author OSS Appender Team
 * @since 1.0.0
 */
public class S3StorageServiceProvider implements StorageService {
    /**
     * 适配器类型：标准S3协议
     * 用于匹配所有使用S3协议的存储服务（AWS S3, MinIO, 阿里云OSS, 腾讯云COS, SF S3等）
     */
    private static final ProtocolType ADAPTER_TYPE = ProtocolType.S3;
    private S3StorageAdapter adapter;

    /**
     * 构造S3存储服务（用于SPI实例化）
     */
    public S3StorageServiceProvider() {
        // 无参构造函数用于SPI实例化
    }

    @Override
    public CompletableFuture<Void> putObject(String key, byte[] data) {
        ensureInitialized();
        return adapter.putObject(key, data);
    }

    @Override
    public ProtocolType getProtocolType() {
        return ADAPTER_TYPE;
    }

    @Override
    public String getBucketName() {
        ensureInitialized();
        return adapter.getBucketName();
    }

    @Override
    public String getKeyPrefix() {
        ensureInitialized();
        return adapter.getKeyPrefix();
    }

    @Override
    public void close() {
        if (adapter != null) {
            adapter.close();
        }
    }

    @Override
    public boolean supportsProtocol(ProtocolType protocol) {
        return ADAPTER_TYPE == protocol;
    }

    /**
     * 初始化存储服务
     * @param config 存储配置
     */
    public void initialize(StorageConfig config) {
        if (this.adapter == null) {
            this.adapter = new S3StorageAdapter(config);
        }
    }

    /**
     * 确保存储服务已初始化
     */
    private void ensureInitialized() {
        if (adapter == null) {
            throw new IllegalStateException("Storage service not initialized. Use StorageServiceFactory instead.");
        }
    }
}