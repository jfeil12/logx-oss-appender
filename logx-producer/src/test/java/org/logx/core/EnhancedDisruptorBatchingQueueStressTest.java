package org.logx.core;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.logx.storage.ProtocolType;
import org.logx.storage.StorageService;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class EnhancedDisruptorBatchingQueueStressTest {

    @Test
    @DisplayName("大批次分片上传应受控并发且避免Full GC风暴")
    void shouldControlPeakShardConcurrencyWhenLargeBatchSharding() throws Exception {
        long maxHeap = Runtime.getRuntime().maxMemory();
        if (maxHeap < 700L * 1024 * 1024) {
            return;
        }

        int logicalSize = 200 * 1024 * 1024;
        int shardSizeMb = 8;
        int maxConcurrentShardUploads = 3;
        int expectedShards = (int) Math.ceil((double) logicalSize / (shardSizeMb * 1024 * 1024));

        StressStorageService storageService = new StressStorageService(expectedShards);
        EnhancedDisruptorBatchingQueue.Config config = new EnhancedDisruptorBatchingQueue.Config()
                .queueCapacity(1024)
                .batchMaxMessages(4)
                .batchMaxBytes(logicalSize + 1024)
                .maxMessageAgeMs(100)
                .enableCompression(false)
                .enableSharding(true)
                .maxUploadSizeMb(shardSizeMb)
                .maxConcurrentShardUploads(maxConcurrentShardUploads)
                .uploadTimeoutMs(20000L);

        EnhancedDisruptorBatchingQueue queue = new EnhancedDisruptorBatchingQueue(config,
                (batchData, originalSize, compressed, messageCount) -> true,
                storageService);

        long fullGcBefore = readFullGcCount();
        queue.start();

        byte[] payload = new byte[logicalSize];
        for (int i = 0; i < payload.length; i++) {
            payload[i] = 'a';
        }

        assertTrue(queue.submit(payload), "大批次消息应成功进入队列");
        assertTrue(storageService.await(30, TimeUnit.SECONDS), "分片上传应在超时时间内完成");

        queue.close();

        assertFalse(storageService.wasByteArrayUploadCalled(), "分片上传应使用ByteBuffer接口避免数组复制");
        assertTrue(storageService.getMaxInFlight() <= maxConcurrentShardUploads,
                "并发分片上传数量应被限制");

        long fullGcAfter = readFullGcCount();
        if (fullGcBefore >= 0 && fullGcAfter >= 0) {
            assertTrue(fullGcAfter - fullGcBefore <= 1,
                    "压力测试期间不应出现Full GC风暴");
        }
    }

    private long readFullGcCount() {
        List<GarbageCollectorMXBean> gcBeans = ManagementFactory.getGarbageCollectorMXBeans();
        long total = 0L;
        boolean found = false;
        for (GarbageCollectorMXBean gcBean : gcBeans) {
            String name = gcBean.getName().toLowerCase();
            if (name.contains("old") || name.contains("mark") || name.contains("full")) {
                long count = gcBean.getCollectionCount();
                if (count >= 0) {
                    total += count;
                    found = true;
                }
            }
        }
        return found ? total : -1L;
    }

    private static class StressStorageService implements StorageService {
        private final CountDownLatch latch;
        private final AtomicInteger inFlight = new AtomicInteger(0);
        private final AtomicInteger maxInFlight = new AtomicInteger(0);
        private final AtomicBoolean byteArrayUploadCalled = new AtomicBoolean(false);

        private StressStorageService(int shardCount) {
            this.latch = new CountDownLatch(shardCount);
        }

        @Override
        public CompletableFuture<Void> putObject(String key, byte[] data) {
            byteArrayUploadCalled.set(true);
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public CompletableFuture<Void> putObject(String key, ByteBuffer buffer) {
            return CompletableFuture.runAsync(() -> {
                int current = inFlight.incrementAndGet();
                maxInFlight.accumulateAndGet(current, Math::max);
                try {
                    Thread.sleep(30L);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    inFlight.decrementAndGet();
                    latch.countDown();
                }
            });
        }

        @Override
        public ProtocolType getProtocolType() {
            return ProtocolType.S3;
        }

        @Override
        public String getBucketName() {
            return "stress";
        }

        @Override
        public String getKeyPrefix() {
            return "stress";
        }

        @Override
        public void close() {
        }

        @Override
        public boolean supportsProtocol(ProtocolType protocol) {
            return ProtocolType.S3 == protocol;
        }

        private boolean await(long timeout, TimeUnit timeUnit) throws InterruptedException {
            return latch.await(timeout, timeUnit);
        }

        private int getMaxInFlight() {
            return maxInFlight.get();
        }

        private boolean wasByteArrayUploadCalled() {
            return byteArrayUploadCalled.get();
        }
    }
}
