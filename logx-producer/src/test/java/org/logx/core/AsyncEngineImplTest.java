package org.logx.core;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.logx.fallback.FallbackManager;
import org.logx.storage.ProtocolType;
import org.logx.storage.StorageService;
import org.mockito.Mockito;

class AsyncEngineImplTest {

    @Test
    @DisplayName("当payload大小等于上限时应该允许入队")
    void shouldSubmitWhenPayloadEqualsLimit() {
        AsyncEngineConfig config = AsyncEngineConfig.defaultConfig()
                .payloadMaxBytes(1024)
                .oversizePayloadPolicy(AsyncEngineConfig.OversizePayloadPolicy.DROP);
        StorageService storageService = mockStorageService();
        EnhancedDisruptorBatchingQueue queue = Mockito.mock(EnhancedDisruptorBatchingQueue.class);
        FallbackManager fallbackManager = Mockito.mock(FallbackManager.class);
        when(queue.submit(any(byte[].class))).thenReturn(true);

        AsyncEngineImpl engine = new AsyncEngineImpl(config, storageService, queue, fallbackManager);
        engine.start();

        byte[] data = new byte[1024];
        engine.put(data);

        verify(queue, times(1)).submit(data);
        verify(fallbackManager, never()).writeFallbackFile(any(byte[].class));
    }

    @Test
    @DisplayName("当payload超过上限且策略为DROP时应该拒绝入队")
    void shouldRejectWhenPayloadExceedsLimitWithDropPolicy() {
        AsyncEngineConfig config = AsyncEngineConfig.defaultConfig()
                .payloadMaxBytes(1024)
                .oversizePayloadPolicy(AsyncEngineConfig.OversizePayloadPolicy.DROP);
        StorageService storageService = mockStorageService();
        EnhancedDisruptorBatchingQueue queue = Mockito.mock(EnhancedDisruptorBatchingQueue.class);
        FallbackManager fallbackManager = Mockito.mock(FallbackManager.class);

        AsyncEngineImpl engine = new AsyncEngineImpl(config, storageService, queue, fallbackManager);
        engine.start();

        byte[] data = new byte[1025];
        engine.put(data);

        verify(queue, never()).submit(any(byte[].class));
        verify(fallbackManager, never()).writeFallbackFile(any(byte[].class));
    }

    @Test
    @DisplayName("当payload极大且超过兜底文件限制时应该拒绝入队")
    void shouldRejectHugePayloadWithoutQueueSubmit() {
        AsyncEngineConfig config = AsyncEngineConfig.defaultConfig()
                .payloadMaxBytes(1024 * 1024)
                .oversizePayloadPolicy(AsyncEngineConfig.OversizePayloadPolicy.FALLBACK_FILE)
                .oversizeFallbackMaxBytes(5 * 1024 * 1024);
        StorageService storageService = mockStorageService();
        EnhancedDisruptorBatchingQueue queue = Mockito.mock(EnhancedDisruptorBatchingQueue.class);
        FallbackManager fallbackManager = Mockito.mock(FallbackManager.class);

        AsyncEngineImpl engine = new AsyncEngineImpl(config, storageService, queue, fallbackManager);
        engine.start();

        byte[] data = new byte[11 * 1024 * 1024];
        engine.put(data);

        verify(queue, never()).submit(any(byte[].class));
        verify(fallbackManager, never()).writeFallbackFile(any(byte[].class));
    }

    private StorageService mockStorageService() {
        StorageService storageService = Mockito.mock(StorageService.class);
        when(storageService.getKeyPrefix()).thenReturn("test-prefix");
        when(storageService.getProtocolType()).thenReturn(ProtocolType.S3);
        when(storageService.getBucketName()).thenReturn("test-bucket");
        when(storageService.supportsProtocol(ProtocolType.S3)).thenReturn(true);
        when(storageService.putObject(any(String.class), any(byte[].class))).thenReturn(CompletableFuture.completedFuture(null));
        return storageService;
    }
}
