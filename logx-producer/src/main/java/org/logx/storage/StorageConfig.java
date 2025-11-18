package org.logx.storage;

import org.logx.config.properties.LogxOssProperties;

public class StorageConfig {

    private LogxOssProperties properties;

    // 存储配置
    private String ossType;
    private String endpoint;
    private String region;
    private String accessKeyId;
    private String accessKeySecret;
    private String bucket;
    private String keyPrefix;
    private boolean pathStyleAccess;
    private long uploadTimeoutMs = 30000L;

    // 无参构造函数，用于测试和Builder模式
    public StorageConfig() {
        this.properties = new LogxOssProperties();
    }

    // 从LogxOssProperties构造
    public StorageConfig(LogxOssProperties properties) {
        this.properties = properties;

        // 复制属性到本地字段
        this.ossType = properties.getStorage().getOssType();
        this.endpoint = properties.getStorage().getEndpoint();
        this.region = properties.getStorage().getRegion();
        this.accessKeyId = properties.getStorage().getAccessKeyId();
        this.accessKeySecret = properties.getStorage().getAccessKeySecret();
        this.bucket = properties.getStorage().getBucket();
        this.keyPrefix = properties.getStorage().getKeyPrefix();
        this.pathStyleAccess = properties.getStorage().isPathStyleAccess();
        this.uploadTimeoutMs = properties.getStorage().getUploadTimeoutMs();
    }

    public String getOssType() {
        return ossType != null ? ossType : properties.getStorage().getOssType();
    }

    public void setOssType(String ossType) {
        this.ossType = ossType;
        if (properties != null) {
            properties.getStorage().setOssType(ossType);
        }
    }

    public String getEndpoint() {
        return endpoint != null ? endpoint : properties.getStorage().getEndpoint();
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
        if (properties != null) {
            properties.getStorage().setEndpoint(endpoint);
        }
    }

    public String getRegion() {
        return region != null ? region : properties.getStorage().getRegion();
    }

    public void setRegion(String region) {
        this.region = region;
        if (properties != null) {
            properties.getStorage().setRegion(region);
        }
    }

    public String getAccessKeyId() {
        return accessKeyId != null ? accessKeyId : properties.getStorage().getAccessKeyId();
    }

    public void setAccessKeyId(String accessKeyId) {
        this.accessKeyId = accessKeyId;
        if (properties != null) {
            properties.getStorage().setAccessKeyId(accessKeyId);
        }
    }

    public String getAccessKeySecret() {
        return accessKeySecret != null ? accessKeySecret : properties.getStorage().getAccessKeySecret();
    }

    public void setAccessKeySecret(String accessKeySecret) {
        this.accessKeySecret = accessKeySecret;
        if (properties != null) {
            properties.getStorage().setAccessKeySecret(accessKeySecret);
        }
    }

    public String getBucket() {
        return bucket != null ? bucket : properties.getStorage().getBucket();
    }

    public void setBucket(String bucket) {
        this.bucket = bucket;
        if (properties != null) {
            properties.getStorage().setBucket(bucket);
        }
    }

    public String getKeyPrefix() {
        return keyPrefix != null ? keyPrefix : properties.getStorage().getKeyPrefix();
    }

    public void setKeyPrefix(String keyPrefix) {
        this.keyPrefix = keyPrefix;
        if (properties != null) {
            properties.getStorage().setKeyPrefix(keyPrefix);
        }
    }

    public boolean isPathStyleAccess() {
        return pathStyleAccess || properties.getStorage().isPathStyleAccess();
    }

    public void setPathStyleAccess(boolean pathStyleAccess) {
        this.pathStyleAccess = pathStyleAccess;
        if (properties != null) {
            properties.getStorage().setPathStyleAccess(pathStyleAccess);
        }
    }

    public long getUploadTimeoutMs() {
        return uploadTimeoutMs != 0 ? uploadTimeoutMs : properties.getStorage().getUploadTimeoutMs();
    }

    public void setUploadTimeoutMs(long uploadTimeoutMs) {
        this.uploadTimeoutMs = uploadTimeoutMs;
        if (properties != null) {
            properties.getStorage().setUploadTimeoutMs(uploadTimeoutMs);
        }
    }

    public int getQueueCapacity() {
        return properties.getEngine().getQueue().getCapacity();
    }

    public void setQueueCapacity(int queueCapacity) {
        if (properties != null) {
            properties.getEngine().getQueue().setCapacity(queueCapacity);
        }
    }

    public int getBatchMaxMessages() {
        return properties.getEngine().getBatch().getCount();
    }

    public void setBatchMaxMessages(int batchMaxMessages) {
        if (properties != null) {
            properties.getEngine().getBatch().setCount(batchMaxMessages);
        }
    }

    public int getBatchMaxBytes() {
        return properties.getEngine().getBatch().getBytes();
    }

    public void setBatchMaxBytes(int batchMaxBytes) {
        if (properties != null) {
            properties.getEngine().getBatch().setBytes(batchMaxBytes);
        }
    }

    public long getMaxMessageAgeMs() {
        return properties.getEngine().getBatch().getMaxAgeMs();
    }

    public void setMaxMessageAgeMs(long maxMessageAgeMs) {
        if (properties != null) {
            properties.getEngine().getBatch().setMaxAgeMs(maxMessageAgeMs);
        }
    }

    public boolean isDropWhenQueueFull() {
        return properties.getEngine().getQueue().isDropWhenFull();
    }

    public void setDropWhenQueueFull(boolean dropWhenQueueFull) {
        if (properties != null) {
            properties.getEngine().getQueue().setDropWhenFull(dropWhenQueueFull);
        }
    }

    // 获取内部的LogxOssProperties对象
    public LogxOssProperties getProperties() {
        return properties;
    }

    // Builder模式
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final StorageConfig config = new StorageConfig();

        public Builder ossType(String ossType) {
            config.setOssType(ossType);
            return this;
        }

        public Builder endpoint(String endpoint) {
            config.setEndpoint(endpoint);
            return this;
        }

        public Builder region(String region) {
            config.setRegion(region);
            return this;
        }

        public Builder accessKeyId(String accessKeyId) {
            config.setAccessKeyId(accessKeyId);
            return this;
        }

        public Builder accessKeySecret(String accessKeySecret) {
            config.setAccessKeySecret(accessKeySecret);
            return this;
        }

        public Builder bucket(String bucket) {
            config.setBucket(bucket);
            return this;
        }

        public Builder keyPrefix(String keyPrefix) {
            config.setKeyPrefix(keyPrefix);
            return this;
        }

        public Builder pathStyleAccess(boolean pathStyleAccess) {
            config.setPathStyleAccess(pathStyleAccess);
            return this;
        }

        public StorageConfig build() {
            return config;
        }
    }
}
