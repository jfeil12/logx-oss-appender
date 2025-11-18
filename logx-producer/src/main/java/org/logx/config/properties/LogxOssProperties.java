package org.logx.config.properties;

public class LogxOssProperties {

    private boolean enabled = true;
    private Storage storage = new Storage();
    private Engine engine = new Engine();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Storage getStorage() {
        return storage;
    }

    public void setStorage(Storage storage) {
        this.storage = storage;
    }

    public Engine getEngine() {
        return engine;
    }

    public void setEngine(Engine engine) {
        this.engine = engine;
    }

    public static class Storage {
        private String endpoint;
        private String region;
        private String accessKeyId;
        private String accessKeySecret;
        private String bucket;
        private String keyPrefix = "logx/";
        private String ossType = "SF_S3";
        private boolean pathStyleAccess;
        private long uploadTimeoutMs = 30000L;

        public String getEndpoint() {
            return endpoint;
        }

        public void setEndpoint(String endpoint) {
            this.endpoint = endpoint;
        }

        public String getRegion() {
            return region;
        }

        public void setRegion(String region) {
            this.region = region;
        }

        public String getAccessKeyId() {
            return accessKeyId;
        }

        public void setAccessKeyId(String accessKeyId) {
            this.accessKeyId = accessKeyId;
        }

        public String getAccessKeySecret() {
            return accessKeySecret;
        }

        public void setAccessKeySecret(String accessKeySecret) {
            this.accessKeySecret = accessKeySecret;
        }

        public String getBucket() {
            return bucket;
        }

        public void setBucket(String bucket) {
            this.bucket = bucket;
        }

        public String getKeyPrefix() {
            return keyPrefix;
        }

        public void setKeyPrefix(String keyPrefix) {
            this.keyPrefix = keyPrefix;
        }

        public String getOssType() {
            return ossType;
        }

        public void setOssType(String ossType) {
            this.ossType = ossType;
        }

        public boolean isPathStyleAccess() {
            return pathStyleAccess;
        }

        public void setPathStyleAccess(boolean pathStyleAccess) {
            this.pathStyleAccess = pathStyleAccess;
        }

        public long getUploadTimeoutMs() {
            return uploadTimeoutMs;
        }

        public void setUploadTimeoutMs(long uploadTimeoutMs) {
            this.uploadTimeoutMs = uploadTimeoutMs;
        }
    }

    public static class Batch {
        private int count = 8192;
        private int bytes = 10 * 1024 * 1024;
        private long maxAgeMs = 60000L;

        public int getCount() {
            return count;
        }

        public void setCount(int count) {
            this.count = count;
        }

        public int getBytes() {
            return bytes;
        }

        public void setBytes(int bytes) {
            this.bytes = bytes;
        }

        public long getMaxAgeMs() {
            return maxAgeMs;
        }

        public void setMaxAgeMs(long maxAgeMs) {
            this.maxAgeMs = maxAgeMs;
        }
    }

    public static class Retry {
        private int maxRetries = 3;
        private long baseBackoffMs = 200L;
        private long maxBackoffMs = 10000L;

        public int getMaxRetries() {
            return maxRetries;
        }

        public void setMaxRetries(int maxRetries) {
            this.maxRetries = maxRetries;
        }

        public long getBaseBackoffMs() {
            return baseBackoffMs;
        }

        public void setBaseBackoffMs(long baseBackoffMs) {
            this.baseBackoffMs = baseBackoffMs;
        }

        public long getMaxBackoffMs() {
            return maxBackoffMs;
        }

        public void setMaxBackoffMs(long maxBackoffMs) {
            this.maxBackoffMs = maxBackoffMs;
        }
    }

    public static class Queue {
        private int capacity = 524288;
        private boolean dropWhenFull = false;

        public int getCapacity() {
            return capacity;
        }

        public void setCapacity(int capacity) {
            this.capacity = capacity;
        }

        public boolean isDropWhenFull() {
            return dropWhenFull;
        }

        public void setDropWhenFull(boolean dropWhenFull) {
            this.dropWhenFull = dropWhenFull;
        }
    }

    /**
     * 引擎配置类
     * 包含所有与AsyncEngine相关的配置项
     */
    public static class Engine {
        // 内部嵌套的配置类
        private Batch batch = new Batch();
        private Retry retry = new Retry();
        private Queue queue = new Queue();
        private Fallback fallback = new Fallback();
        private ThreadPool threadPool = new ThreadPool();

        // 其他引擎配置
        private boolean multiProducer = false;
        private boolean enableCpuYield = true;
        private boolean enableMemoryProtection = true;
        private long maxShutdownWaitMs = 30000L;
        private String logFileName = "applogx";
        private int emergencyMemoryThresholdMb = 512;
        private boolean enableCompression = true;
        private int compressionThreshold = 1024;
        private boolean enableSharding = true;
        private int maxUploadSizeMb = 10;
        private int payloadMaxBytes = 512 * 1024;

        public Batch getBatch() {
            return batch;
        }

        public void setBatch(Batch batch) {
            this.batch = batch;
        }

        public Retry getRetry() {
            return retry;
        }

        public void setRetry(Retry retry) {
            this.retry = retry;
        }

        public Queue getQueue() {
            return queue;
        }

        public void setQueue(Queue queue) {
            this.queue = queue;
        }

        public Fallback getFallback() {
            return fallback;
        }

        public void setFallback(Fallback fallback) {
            this.fallback = fallback;
        }

        public ThreadPool getThreadPool() {
            return threadPool;
        }

        public void setThreadPool(ThreadPool threadPool) {
            this.threadPool = threadPool;
        }

        public boolean isMultiProducer() {
            return multiProducer;
        }

        public void setMultiProducer(boolean multiProducer) {
            this.multiProducer = multiProducer;
        }

        public boolean isEnableCpuYield() {
            return enableCpuYield;
        }

        public void setEnableCpuYield(boolean enableCpuYield) {
            this.enableCpuYield = enableCpuYield;
        }

        public boolean isEnableMemoryProtection() {
            return enableMemoryProtection;
        }

        public void setEnableMemoryProtection(boolean enableMemoryProtection) {
            this.enableMemoryProtection = enableMemoryProtection;
        }

        public long getMaxShutdownWaitMs() {
            return maxShutdownWaitMs;
        }

        public void setMaxShutdownWaitMs(long maxShutdownWaitMs) {
            this.maxShutdownWaitMs = maxShutdownWaitMs;
        }

        public String getLogFileName() {
            return logFileName;
        }

        public void setLogFileName(String logFileName) {
            this.logFileName = logFileName;
        }

        public int getEmergencyMemoryThresholdMb() {
            return emergencyMemoryThresholdMb;
        }

        public void setEmergencyMemoryThresholdMb(int emergencyMemoryThresholdMb) {
            this.emergencyMemoryThresholdMb = emergencyMemoryThresholdMb;
        }

        public boolean isEnableCompression() {
            return enableCompression;
        }

        public void setEnableCompression(boolean enableCompression) {
            this.enableCompression = enableCompression;
        }

        public int getCompressionThreshold() {
            return compressionThreshold;
        }

        public void setCompressionThreshold(int compressionThreshold) {
            this.compressionThreshold = compressionThreshold;
        }

        public boolean isEnableSharding() {
            return enableSharding;
        }

        public void setEnableSharding(boolean enableSharding) {
            this.enableSharding = enableSharding;
        }

        public int getMaxUploadSizeMb() {
            return maxUploadSizeMb;
        }

        public void setMaxUploadSizeMb(int maxUploadSizeMb) {
            this.maxUploadSizeMb = maxUploadSizeMb;
        }

        public int getPayloadMaxBytes() {
            return payloadMaxBytes;
        }

        public void setPayloadMaxBytes(int payloadMaxBytes) {
            this.payloadMaxBytes = payloadMaxBytes;
        }
    }

    /**
     * 兜底机制配置
     */
    public static class Fallback {
        private String path = "fallback/logs";
        private int retentionDays = 7;
        private int scanIntervalSeconds = 60;

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }

        public int getRetentionDays() {
            return retentionDays;
        }

        public void setRetentionDays(int retentionDays) {
            this.retentionDays = retentionDays;
        }

        public int getScanIntervalSeconds() {
            return scanIntervalSeconds;
        }

        public void setScanIntervalSeconds(int scanIntervalSeconds) {
            this.scanIntervalSeconds = scanIntervalSeconds;
        }
    }

    /**
     * 线程池配置
     */
    public static class ThreadPool {
        private int corePoolSize = 1;
        private int maximumPoolSize = 1;

        public int getCorePoolSize() {
            return corePoolSize;
        }

        public void setCorePoolSize(int corePoolSize) {
            this.corePoolSize = corePoolSize;
        }

        public int getMaximumPoolSize() {
            return maximumPoolSize;
        }

        public void setMaximumPoolSize(int maximumPoolSize) {
            this.maximumPoolSize = maximumPoolSize;
        }
    }
}
