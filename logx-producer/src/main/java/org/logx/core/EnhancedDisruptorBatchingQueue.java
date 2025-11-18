package org.logx.core;

import com.lmax.disruptor.EventFactory;
import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.YieldingWaitStrategy;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
import org.logx.fallback.ObjectNameGenerator;
import org.logx.storage.StorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.zip.GZIPOutputStream;
import java.util.zip.CRC32;

public final class EnhancedDisruptorBatchingQueue implements AutoCloseable {

    private static final Logger logger = LoggerFactory.getLogger(EnhancedDisruptorBatchingQueue.class);

    public static class LogEvent {
        public final byte[] payload;
        public final long timestampMs;

        public LogEvent(byte[] payload, long timestampMs) {
            this.payload = payload;
            this.timestampMs = timestampMs;
        }
    }

    public interface BatchConsumer {
        boolean processBatch(byte[] batchData, int originalSize, boolean compressed, int messageCount);
    }

    private static final class LogEventHolder {
        byte[] payload;
        long timestampMs;

        void set(byte[] p, long ts) {
            this.payload = p;
            this.timestampMs = ts;
        }

        void clear() {
            this.payload = null;
            this.timestampMs = 0L;
        }
    }

    private final Config config;
    private final BatchConsumer consumer;
    private final StorageService storageService;
    private final Disruptor<LogEventHolder> disruptor;
    private final RingBuffer<LogEventHolder> ringBuffer;
    private final BatchEventHandler batchEventHandler;
    private final AtomicBoolean flushRequested = new AtomicBoolean(false);
    private final ScheduledExecutorService scheduler;

    private volatile boolean started = false;

    private final AtomicLong totalBatchesProcessed = new AtomicLong(0);
    private final AtomicLong totalMessagesProcessed = new AtomicLong(0);
    private final AtomicLong totalBytesProcessed = new AtomicLong(0);
    private final AtomicLong totalBytesCompressed = new AtomicLong(0);
    private final AtomicLong totalCompressionSavings = new AtomicLong(0);
    private final AtomicLong totalShardsCreated = new AtomicLong(0);
    private final AtomicLong totalDroppedMessages = new AtomicLong(0);
    private final AtomicLong lastDropLogTimeMs = new AtomicLong(0);
    private volatile java.util.concurrent.ExecutorService shardExecutor;
    private volatile long uploadTimeoutMs = 30000L;

    public EnhancedDisruptorBatchingQueue(Config config, BatchConsumer consumer, StorageService storageService) {
        this.config = config;
        this.consumer = consumer;
        this.storageService = storageService;
        this.shardExecutor = config.getShardExecutor();
        this.uploadTimeoutMs = config.getUploadTimeoutMs();

        logger.debug("Initializing EnhancedDisruptorBatchingQueue with config: queueCapacity={}, batchMaxMessages={}, batchMaxBytes={}, maxMessageAgeMs={}",
                config.queueCapacity, config.batchMaxMessages, config.batchMaxBytes, config.maxMessageAgeMs);

        EventFactory<LogEventHolder> factory = LogEventHolder::new;
        ProducerType type = config.multiProducer ? ProducerType.MULTI : ProducerType.SINGLE;

        this.disruptor = new Disruptor<>(
                factory,
                config.queueCapacity,
                r -> {
                    Thread t = new Thread(r, "enhanced-disruptor-consumer");
                    t.setDaemon(true);
                    t.setPriority(Thread.MIN_PRIORITY);
                    return t;
                },
                type,
                new YieldingWaitStrategy());

        this.batchEventHandler = new BatchEventHandler();
        disruptor.handleEventsWith(batchEventHandler);

        disruptor.setDefaultExceptionHandler(new com.lmax.disruptor.ExceptionHandler<LogEventHolder>() {
            @Override
            public void handleEventException(Throwable ex, long sequence, LogEventHolder event) {
                logger.error("Exception processing event for sequence {}: {}", sequence, event, ex);
            }

            @Override
            public void handleOnStartException(Throwable ex) {
                logger.error("Exception during disruptor startup", ex);
            }

            @Override
            public void handleOnShutdownException(Throwable ex) {
                logger.error("Exception during disruptor shutdown", ex);
            }
        });

        this.ringBuffer = disruptor.getRingBuffer();

        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "disruptor-batch-scheduler");
            t.setDaemon(true);
            return t;
        });
    }

    public synchronized void start() {
        if (started) {
            return;
        }
        disruptor.start();
        long checkInterval = Math.max(100, config.maxMessageAgeMs / 10);
        scheduler.scheduleAtFixedRate(batchEventHandler::checkAndProcessBatch, checkInterval, checkInterval, TimeUnit.MILLISECONDS);
        started = true;
    }

    public boolean submit(byte[] payload) {
        if (!started) {
            return false;
        }

        long ts = System.currentTimeMillis();
        while (true) {
            if (ringBuffer.hasAvailableCapacity(1)) {
                long seq = ringBuffer.next();
                try {
                    LogEventHolder slot = ringBuffer.get(seq);
                    slot.set(payload, ts);
                } finally {
                    ringBuffer.publish(seq);
                }
                return true;
            }

            if (!config.blockOnFull) {
                long drops = totalDroppedMessages.incrementAndGet();
                long now = System.currentTimeMillis();
                long lastLog = lastDropLogTimeMs.get();
                if (now - lastLog > 1000 && lastDropLogTimeMs.compareAndSet(lastLog, now)) {
                    double usage = getQueueUsageRatio();
                    long fingerprint = fingerprintPayload(payload);
                    logger.warn("[DATA_LOSS_ALERT] Queue drop detected. totalDropped={}, queueUsage={}%, payloadFingerprint={}",
                            drops, String.format("%.2f", usage * 100), fingerprint);
                }
                return false;
            }

            try {
                synchronized (this) {
                    wait(1L);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
    }

    public BatchMetrics getMetrics() {
        return new BatchMetrics(
                totalBatchesProcessed.get(),
                totalMessagesProcessed.get(),
                totalBytesProcessed.get(),
                totalBytesCompressed.get(),
                totalCompressionSavings.get(),
                config.batchMaxMessages,
                totalShardsCreated.get());
    }

    public String getQueueStatusInfo() {
        if (!started) {
            return "Queue not started";
        }

        long remainingCapacity = ringBuffer.remainingCapacity();
        long cursor = ringBuffer.getCursor();
        long bufferSize = ringBuffer.getBufferSize();
        long occupiedSlots = bufferSize - remainingCapacity;

        long approximateConsumerSequence = cursor - occupiedSlots;

        return String.format("Queue capacity: %d, occupied: %d, remaining: %d, cursor: %d, consumer sequence (approx): %d",
                bufferSize, occupiedSlots, remainingCapacity, cursor, approximateConsumerSequence);
    }

    @Override
    public synchronized void close() {
        if (!started) {
            return;
        }

        logger.info("Closing queue, forcing processing of all remaining events");

        try {
            logger.info("Step 1: Forcing flush of BatchEventHandler buffer");
            batchEventHandler.forceFlushBuffer();

            logger.info("Step 2: Shutting down Disruptor to stop new events");
            disruptor.shutdown();

            scheduler.shutdown();
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }

            synchronized (this) {
                wait(100);
            }

            logger.info("Step 3: Processing remaining events in the ring buffer");
            forceProcessAllRemainingEvents();

        } catch (Exception e) {
            logger.error("Error while closing queue: {}", e.getMessage(), e);
        } finally {
            flushRequested.set(false);
            started = false;
            logger.info("Queue closed");
        }
    }

    private void forceProcessAllRemainingEvents() {
        try {
            long cursor = ringBuffer.getCursor();
            long nextSequence = ringBuffer.getMinimumGatingSequence() + 1;

            logger.info("Forcing processing of remaining events - cursor: {}, nextSequence: {}", cursor, nextSequence);

            if (nextSequence <= cursor) {
                List<LogEvent> remainingEvents = new ArrayList<>();

                for (long seq = nextSequence; seq <= cursor; seq++) {
                    try {
                        LogEventHolder holder = ringBuffer.get(seq);
                        if (holder != null && holder.payload != null) {
                            remainingEvents.add(new LogEvent(holder.payload, holder.timestampMs));
                        }
                    } catch (Exception e) {
                        logger.warn("Error processing remaining event at sequence {}: {}", seq, e.getMessage());
                    }
                }

                if (!remainingEvents.isEmpty()) {
                    logger.info("Found {} remaining events in ring buffer, forcing upload", remainingEvents.size());
                    processRemainingEvents(remainingEvents);
                } else {
                    logger.info("No remaining events found in ring buffer");
                }
            } else {
                logger.info("Ring buffer is empty, no remaining events to process");
            }
        } catch (Exception e) {
            logger.error("Error while forcing processing of remaining events: {}", e.getMessage(), e);
        }
    }

    private void processRemainingEvents(List<LogEvent> events) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            int totalBytes = 0;

            for (LogEvent event : events) {
                if (event.payload != null) {
                    baos.write(event.payload);
                    if (event.payload.length > 0 && event.payload[event.payload.length - 1] != '\n') {
                        baos.write('\n');
                    }
                    totalBytes += event.payload.length;
                }
            }

            if (baos.size() > 0) {
                byte[] batchData = baos.toByteArray();
                byte[] finalData = batchData;
                boolean compressed = false;
                if (config.enableCompression && batchData.length > 1024) {
                    finalData = compressData(batchData);
                    compressed = true;
                }

                boolean success = consumer.processBatch(finalData, totalBytes, compressed, events.size());

                if (success) {
                    totalBatchesProcessed.incrementAndGet();
                    totalMessagesProcessed.addAndGet(events.size());
                    totalBytesProcessed.addAndGet(totalBytes);
                }
            }
        } catch (Exception e) {
            logger.error("Error processing remaining events: {}", e.getMessage(), e);
        }
    }

    private class BatchEventHandler implements EventHandler<LogEventHolder> {
        private LogEvent[] eventBuffer;
        private int bufferHead = 0;
        private int bufferTail = 0;
        private int bufferCount = 0;
        private int totalBytes = 0;
        private long oldestTimestamp = 0L;

        BatchEventHandler() {
            this.eventBuffer = new LogEvent[config.batchMaxMessages];
        }

        @Override
        public void onEvent(LogEventHolder ev, long sequence, boolean endOfBatch) {
            if (ev == null) {
                return;
            }

            if (ev.payload == null) {
                if (flushRequested.get() && bufferCount > 0) {
                    processBatch();
                    clearBuffer();
                    flushRequested.set(false);
                }
                if (flushRequested.get() && bufferCount == 0) {
                    flushRequested.set(false);
                }
                ev.clear();
                return;
            }

            LogEvent event = new LogEvent(ev.payload, ev.timestampMs);
            eventBuffer[bufferTail] = event;
            bufferTail = (bufferTail + 1) % eventBuffer.length;
            bufferCount++;

            if (bufferCount == 1) {
                oldestTimestamp = event.timestampMs;
            }
            totalBytes += event.payload.length;

            ev.clear();

            checkAndProcessBatchByCountAndSize();
        }

        private void processBatch() {
            try {
                byte[] serializedData = serializeToPatternFormat(eventBuffer, bufferHead, bufferCount);
                int originalSize = serializedData.length;

                boolean shouldCompress = config.enableCompression;
                byte[] finalData = serializedData;

                if (shouldCompress) {
                    finalData = compressData(serializedData);
                    totalBytesCompressed.addAndGet(finalData.length);
                    totalCompressionSavings.addAndGet(originalSize - finalData.length);
                }

                boolean success;
                if (config.enableSharding && originalSize > config.getShardingThreshold()) {
                    success = processSharding(serializedData);
                } else {
                    success = consumer.processBatch(finalData, originalSize, shouldCompress, bufferCount);
                }

                if (success) {
                    totalBatchesProcessed.incrementAndGet();
                    totalMessagesProcessed.addAndGet(bufferCount);
                    totalBytesProcessed.addAndGet(originalSize);
                }

            } catch (Exception e) {
                logger.error("Batch processing failed: {}", e.getMessage(), e);
            }
        }

        private void clearBuffer() {
            for (int i = 0; i < bufferCount; i++) {
                int index = (bufferHead + i) % eventBuffer.length;
                eventBuffer[index] = null;
            }

            bufferHead = 0;
            bufferTail = 0;
            bufferCount = 0;
            totalBytes = 0;
            oldestTimestamp = 0L;
        }

        private synchronized void checkAndProcessBatchByCountAndSize() {
            boolean shouldTrigger = false;
            String triggerReason = "";

            if (bufferCount >= config.batchMaxMessages) {
                shouldTrigger = true;
                triggerReason = "message count threshold reached: " + bufferCount + " >= " + config.batchMaxMessages;
            } else if (totalBytes >= config.batchMaxBytes) {
                shouldTrigger = true;
                triggerReason = "byte size threshold reached: " + totalBytes + " >= " + config.batchMaxBytes;
            }

            if (shouldTrigger) {
                logger.info("Triggering batch upload - {}", triggerReason);
                processBatch();
                clearBuffer();
            }
        }

        public synchronized void checkAndProcessBatch() {
            if (bufferCount > 0) {
                long currentTime = System.currentTimeMillis();
                long age = currentTime - oldestTimestamp;
                if (age >= config.maxMessageAgeMs) {
                    logger.info("Triggering batch upload - message age timeout: {}ms >= {}ms", age, config.maxMessageAgeMs);
                    processBatch();
                    clearBuffer();
                }
            }
        }

        public void forceFlushBuffer() {
            if (bufferCount > 0) {
                logger.info("Forcing flush of BatchEventHandler buffer with {} events", bufferCount);
                try {
                    processBatch();
                } catch (Exception e) {
                    logger.error("Error during forced flush of BatchEventHandler buffer: {}", e.getMessage(), e);
                }
                clearBuffer();
            }
        }
    }

    private byte[] serializeToPatternFormat(LogEvent[] events, int head, int count) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            for (int i = 0; i < count; i++) {
                int index = (head + i) % events.length;
                LogEvent event = events[index];
                byte[] payload = event.payload;

                if (payload.length > 0 && payload[payload.length - 1] != '\n') {
                    byte[] newPayload = new byte[payload.length + 1];
                    System.arraycopy(payload, 0, newPayload, 0, payload.length);
                    newPayload[newPayload.length - 1] = '\n';
                    baos.write(newPayload);
                } else {
                    baos.write(payload);
                }
            }
            return baos.toByteArray();
        } catch (IOException e) {
            logger.warn("IO exception during serialization, falling back to string-based method: {}", e.getMessage());
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < count; i++) {
                int index = (head + i) % events.length;
                LogEvent event = events[index];
                String logLine = new String(event.payload, StandardCharsets.UTF_8);

                if (!logLine.endsWith("\n")) {
                    sb.append(logLine).append("\n");
                } else {
                    sb.append(logLine);
                }
            }
            return sb.toString().getBytes(StandardCharsets.UTF_8);
        }
    }

    private byte[] compressData(byte[] data) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (GZIPOutputStream gzos = new GZIPOutputStream(baos)) {
            gzos.write(data);
        }
        return baos.toByteArray();
    }

    private boolean processSharding(byte[] data) {
        java.util.List<CompletableFuture<Void>> futures = new java.util.ArrayList<>();
        try {
            int shardCount = (int) Math.ceil((double) data.length / config.getShardSize());

            if (shardCount <= 1) {
                return consumer.processBatch(data, data.length, false, 1);
            }

            if (shardExecutor == null) {
                logger.warn("Shard executor not configured, falling back to synchronous upload on consumer thread");
            }

            for (int i = 0; i < shardCount; i++) {
                int start = i * config.getShardSize();
                int end = Math.min(start + config.getShardSize(), data.length);
                int length = end - start;

                byte[] shardData = new byte[length];
                System.arraycopy(data, start, shardData, 0, length);

                byte[] finalShardData = config.enableCompression ? compressData(shardData) : shardData;
                String shardKey = ObjectNameGenerator.generateObjectName(storageService.getKeyPrefix());
                totalShardsCreated.incrementAndGet();

                java.util.concurrent.Executor executor = shardExecutor != null
                        ? shardExecutor
                        : java.util.concurrent.ForkJoinPool.commonPool();

                CompletableFuture<Void> uploadFuture = CompletableFuture.runAsync(() -> {
                    try {
                        storageService.putObject(shardKey, finalShardData).get(uploadTimeoutMs, TimeUnit.MILLISECONDS);
                    } catch (Exception ex) {
                        throw new RuntimeException("Shard upload failed for key " + shardKey, ex);
                    }
                }, executor);

                futures.add(uploadFuture);
            }

            CompletableFuture<Void> all = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
            all.get(uploadTimeoutMs, TimeUnit.MILLISECONDS);
            return true;
        } catch (InterruptedException e) {
            logger.error("Sharding process interrupted: {}", e.getMessage());
            Thread.currentThread().interrupt();
            return false;
        } catch (java.util.concurrent.TimeoutException e) {
            logger.error("Sharding process timeout after {} ms", uploadTimeoutMs);
            futures.forEach(f -> f.cancel(true));
            return false;
        } catch (Exception e) {
            logger.error("Sharding process failed: {}", e.getMessage(), e);
            futures.forEach(f -> f.cancel(true));
            return false;
        }
    }

    public void setShardExecutor(java.util.concurrent.ExecutorService shardExecutor, long uploadTimeoutMs) {
        this.shardExecutor = shardExecutor;
        this.uploadTimeoutMs = uploadTimeoutMs;
    }

    private double getQueueUsageRatio() {
        long remainingCapacity = ringBuffer.remainingCapacity();
        long bufferSize = ringBuffer.getBufferSize();
        if (bufferSize <= 0) {
            return 0;
        }
        long occupied = bufferSize - remainingCapacity;
        return Math.max(0, Math.min(1, (double) occupied / bufferSize));
    }

    private long fingerprintPayload(byte[] payload) {
        if (payload == null || payload.length == 0) {
            return 0L;
        }
        java.util.zip.CRC32 crc32 = new java.util.zip.CRC32();
        int length = Math.min(payload.length, 1024);
        crc32.update(payload, 0, length);
        return crc32.getValue();
    }

    public static class Config {

        private int queueCapacity = 524288;
        private int batchMaxMessages = 8192;
        private int batchMaxBytes = 10 * 1024 * 1024;
        private long maxMessageAgeMs = 60000L;
        private boolean blockOnFull = true;
        private boolean multiProducer = true;
        private boolean enableCompression = true;
        private boolean enableSharding = true;
        private int maxUploadSizeMb = 10;
        private int consumerThreadCount = 1;
        private java.util.concurrent.ExecutorService shardExecutor;
        private long uploadTimeoutMs = 30000L;

        public static Config defaultConfig() {
            return new Config();
        }

        public Config queueCapacity(int queueCapacity) {
            this.queueCapacity = queueCapacity;
            return this;
        }

        public Config batchMaxMessages(int batchMaxMessages) {
            this.batchMaxMessages = Math.max(10, Math.min(10000, batchMaxMessages));
            return this;
        }

        public Config batchMaxBytes(int batchMaxBytes) {
            this.batchMaxBytes = batchMaxBytes;
            return this;
        }

        public Config maxMessageAgeMs(long maxMessageAgeMs) {
            this.maxMessageAgeMs = maxMessageAgeMs;
            return this;
        }

        public Config blockOnFull(boolean blockOnFull) {
            this.blockOnFull = blockOnFull;
            return this;
        }

        public Config multiProducer(boolean multiProducer) {
            this.multiProducer = multiProducer;
            return this;
        }

        public Config enableCompression(boolean enableCompression) {
            this.enableCompression = enableCompression;
            return this;
        }

        public Config enableSharding(boolean enableSharding) {
            this.enableSharding = enableSharding;
            return this;
        }

        public Config maxUploadSizeMb(int maxUploadSizeMb) {
            this.maxUploadSizeMb = maxUploadSizeMb;
            return this;
        }

        public Config consumerThreadCount(int consumerThreadCount) {
            this.consumerThreadCount = Math.max(1, Math.min(16, consumerThreadCount));
            return this;
        }

        public Config shardExecutor(java.util.concurrent.ExecutorService shardExecutor) {
            this.shardExecutor = shardExecutor;
            return this;
        }

        public Config uploadTimeoutMs(long uploadTimeoutMs) {
            this.uploadTimeoutMs = uploadTimeoutMs;
            return this;
        }

        public int getQueueCapacity() {
            return queueCapacity;
        }

        public int getBatchMaxMessages() {
            return batchMaxMessages;
        }

        public int getBatchMaxBytes() {
            return batchMaxBytes;
        }

        public long getMaxMessageAgeMs() {
            return maxMessageAgeMs;
        }

        public boolean isBlockOnFull() {
            return blockOnFull;
        }

        public boolean isMultiProducer() {
            return multiProducer;
        }

        public boolean isEnableCompression() {
            return enableCompression;
        }

        public boolean isEnableSharding() {
            return enableSharding;
        }

        public int getMaxUploadSizeMb() {
            return maxUploadSizeMb;
        }

        public int getConsumerThreadCount() {
            return consumerThreadCount;
        }

        public int getShardingThreshold() {
            return maxUploadSizeMb * 1024 * 1024;
        }

        public int getShardSize() {
            return maxUploadSizeMb * 1024 * 1024;
        }

        public java.util.concurrent.ExecutorService getShardExecutor() {
            return shardExecutor;
        }

        public long getUploadTimeoutMs() {
            return uploadTimeoutMs;
        }
    }

    public static class BatchMetrics {
        private final long totalBatchesProcessed;
        private final long totalMessagesProcessed;
        private final long totalBytesProcessed;
        private final long totalBytesCompressed;
        private final long totalCompressionSavings;
        private final int currentBatchSize;
        private final long totalShardsCreated;

        public BatchMetrics(long totalBatchesProcessed, long totalMessagesProcessed,
                            long totalBytesProcessed, long totalBytesCompressed,
                            long totalCompressionSavings, int currentBatchSize,
                            long totalShardsCreated) {
            this.totalBatchesProcessed = totalBatchesProcessed;
            this.totalMessagesProcessed = totalMessagesProcessed;
            this.totalBytesProcessed = totalBytesProcessed;
            this.totalBytesCompressed = totalBytesCompressed;
            this.totalCompressionSavings = totalCompressionSavings;
            this.currentBatchSize = currentBatchSize;
            this.totalShardsCreated = totalShardsCreated;
        }

        public long getTotalBatchesProcessed() {
            return totalBatchesProcessed;
        }

        public long getTotalMessagesProcessed() {
            return totalMessagesProcessed;
        }

        public long getTotalBytesProcessed() {
            return totalBytesProcessed;
        }

        public long getTotalBytesCompressed() {
            return totalBytesCompressed;
        }

        public long getTotalCompressionSavings() {
            return totalCompressionSavings;
        }

        public int getCurrentBatchSize() {
            return currentBatchSize;
        }

        public long getTotalShardsCreated() {
            return totalShardsCreated;
        }

        public double getCompressionRatio() {
            return totalBytesProcessed > 0
                    ? (double) totalCompressionSavings / totalBytesProcessed
                    : 0.0;
        }

        public double getAverageMessagesPerBatch() {
            return totalBatchesProcessed > 0
                    ? (double) totalMessagesProcessed / totalBatchesProcessed
                    : 0.0;
        }

        @Override
        public String toString() {
            return String.format(
                    "BatchMetrics{batches=%d, messages=%d, bytes=%d, compressed=%d, " +
                            "savings=%d (%.1f%%), currentBatchSize=%d, shards=%d}",
                    totalBatchesProcessed, totalMessagesProcessed, totalBytesProcessed,
                    totalBytesCompressed, totalCompressionSavings,
                    getCompressionRatio() * 100, currentBatchSize, totalShardsCreated);
        }
    }
}
