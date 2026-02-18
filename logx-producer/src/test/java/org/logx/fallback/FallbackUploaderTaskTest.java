package org.logx.fallback;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.logx.storage.ProtocolType;
import org.logx.storage.StorageService;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FallbackUploaderTaskTest {

    @Test
    @DisplayName("当文件超过上限时应隔离并跳过上传")
    void shouldQuarantineWhenFileExceedsMaxRetryFileBytes() throws IOException {
        Path fallbackDir = Files.createTempDirectory("fallback-uploader-test");
        Path oversizedFile = fallbackDir.resolve("oversized.log.gz");
        Files.write(oversizedFile, "0123456789".getBytes(StandardCharsets.UTF_8));

        RecordingStorageService storageService = new RecordingStorageService();
        FallbackUploaderTask task = new FallbackUploaderTask(
            storageService,
            fallbackDir.toString(),
            "applogx",
            7,
            5,
            10,
            1024
        );

        task.run();

        assertEquals(0, storageService.getUploadedKeys().size());
        assertFalse(Files.exists(oversizedFile));
        assertTrue(Files.exists(fallbackDir.resolve("quarantine")));
    }

    @Test
    @DisplayName("当达到每轮文件配额时应停止继续重传")
    void shouldStopRetryWhenReachMaxFilesPerRound() throws IOException {
        Path fallbackDir = Files.createTempDirectory("fallback-uploader-test-quota");
        createFallbackFile(fallbackDir.resolve("a.log.gz"), "line-a");
        createFallbackFile(fallbackDir.resolve("b.log.gz"), "line-b");
        createFallbackFile(fallbackDir.resolve("c.log.gz"), "line-c");

        RecordingStorageService storageService = new RecordingStorageService();
        FallbackUploaderTask task = new FallbackUploaderTask(
            storageService,
            fallbackDir.toString(),
            "applogx",
            7,
            1024,
            2,
            1024
        );

        task.run();

        assertEquals(2, storageService.getUploadedKeys().size());
        long remainingCount;
        try (java.util.stream.Stream<Path> stream = Files.list(fallbackDir)) {
            remainingCount = stream
                .filter(Files::isRegularFile)
                .filter(path -> path.toString().endsWith(".log.gz"))
                .count();
        }
        assertEquals(1, remainingCount);
    }

    private void createFallbackFile(Path file, String content) throws IOException {
        Files.write(file, (content + "\n\n").getBytes(StandardCharsets.UTF_8));
    }

    private static class RecordingStorageService implements StorageService {
        private final List<String> uploadedKeys = new ArrayList<>();

        @Override
        public CompletableFuture<Void> putObject(String key, byte[] data) {
            uploadedKeys.add(key);
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public ProtocolType getProtocolType() {
            return ProtocolType.S3;
        }

        @Override
        public String getBucketName() {
            return "test-bucket";
        }

        @Override
        public String getKeyPrefix() {
            return "test-prefix";
        }

        @Override
        public void close() {
        }

        @Override
        public boolean supportsProtocol(ProtocolType protocol) {
            return protocol == ProtocolType.S3;
        }

        public List<String> getUploadedKeys() {
            return uploadedKeys;
        }
    }
}
