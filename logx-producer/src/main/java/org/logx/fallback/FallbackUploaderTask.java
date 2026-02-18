package org.logx.fallback;

import org.logx.storage.StorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.StandardCopyOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Iterator;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

/**
 * 兜底文件上传任务
 * <p>
 * 负责定时扫描兜底目录并重新上传文件到云存储
 *
 * @author OSS Appender Team
 * @since 1.0.0
 */
public class FallbackUploaderTask implements Runnable {
    
    private static final Logger logger = LoggerFactory.getLogger(FallbackUploaderTask.class);

    // 兜底文件后缀（统一使用.log.gz格式）
    private static final String FALLBACK_FILE_SUFFIX = ".log.gz";
    private static final int UPLOAD_TIMEOUT_SECONDS = 30;
    private static final int STREAM_BUFFER_SIZE = 8192;
    private static final int BINARY_SAMPLE_BYTES = 1024;
    private static final long DEFAULT_MAX_RETRY_FILE_BYTES = 10L * 1024 * 1024;
    private static final int DEFAULT_MAX_RETRY_FILES_PER_ROUND = 100;
    private static final long DEFAULT_MAX_RETRY_BYTES_PER_ROUND = 50L * 1024 * 1024;
    
    private final StorageService storageService;
    private final String fallbackPath;
    private final String absoluteFallbackPath;
    private final int retentionDays;
    private final long maxRetryFileBytes;
    private final int maxRetryFilesPerRound;
    private final long maxRetryBytesPerRound;

    /**
     * @deprecated fileName参数已废弃，ObjectNameGenerator使用固定默认值
     */
    @Deprecated
    public FallbackUploaderTask(StorageService storageService, String fallbackPath, String fileName, int retentionDays) {
        this(storageService, fallbackPath, fileName, retentionDays,
            DEFAULT_MAX_RETRY_FILE_BYTES,
            DEFAULT_MAX_RETRY_FILES_PER_ROUND,
            DEFAULT_MAX_RETRY_BYTES_PER_ROUND);
    }

    /**
     * @deprecated fileName参数已废弃，ObjectNameGenerator使用固定默认值
     */
    @Deprecated
    public FallbackUploaderTask(StorageService storageService, String fallbackPath, String fileName, int retentionDays,
                                long maxRetryFileBytes, int maxRetryFilesPerRound, long maxRetryBytesPerRound) {
        this.storageService = storageService;
        this.fallbackPath = fallbackPath;
        this.absoluteFallbackPath = FallbackPathResolver.resolveAbsolutePath(fallbackPath);
        this.retentionDays = retentionDays;
        this.maxRetryFileBytes = Math.max(1L, maxRetryFileBytes);
        this.maxRetryFilesPerRound = Math.max(1, maxRetryFilesPerRound);
        this.maxRetryBytesPerRound = Math.max(1L, maxRetryBytesPerRound);
    }
    
    @Override
    public void run() {
        try {
            // 首先清理过期文件
            cleanupExpiredFiles();
            
            // 然后重传现有的兜底文件
            retryUploadFiles();
        } catch (Exception e) {
            logger.error("Failed to execute fallback upload task", e);
        }
    }
    
    /**
     * 清理过期的兜底文件
     */
    private void cleanupExpiredFiles() {
        try {
            FallbackFileCleaner.cleanupExpiredFiles(fallbackPath, retentionDays);
        } catch (Exception e) {
            logger.warn("Failed to cleanup expired fallback files", e);
        }
    }
    
    /**
     * 重传兜底文件
     */
    private void retryUploadFiles() {
        try {
            Path fallbackDir = Paths.get(absoluteFallbackPath);
            
            if (!Files.exists(fallbackDir) || !Files.isDirectory(fallbackDir)) {
                logger.warn("Fallback directory does not exist or is not a directory: {}", absoluteFallbackPath);
                return;
            }
            
            int processedFiles = 0;
            long processedBytes = 0L;

            try (Stream<Path> files = Files.walk(fallbackDir)) {
                Iterator<Path> iterator = files
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(FALLBACK_FILE_SUFFIX))
                    .iterator();

                while (iterator.hasNext()) {
                    if (processedFiles >= maxRetryFilesPerRound || processedBytes >= maxRetryBytesPerRound) {
                        logger.info("Fallback retry quota reached, files: {}, bytes: {}", processedFiles, processedBytes);
                        break;
                    }

                    Path file = iterator.next();
                    long fileSize = Files.size(file);

                    if (processedFiles > 0 && processedBytes + fileSize > maxRetryBytesPerRound) {
                        logger.info("Fallback retry byte quota reached before file: {}", file.getFileName());
                        break;
                    }

                    retryUpload(file, fileSize);
                    processedFiles++;
                    processedBytes += fileSize;
                }
            }
        } catch (IOException e) {
            logger.error("Failed to scan fallback directory: {}", absoluteFallbackPath, e);
        }
    }

    private void retryUpload(Path file, long fileSize) {
        try {
            if (fileSize > maxRetryFileBytes) {
                quarantineOversizedFile(file, fileSize);
                return;
            }

            // 使用源文件的相对路径作为对象名，保留原有的日期和时间信息
            String retryObjectName = getRelativePath(file);

            byte[] formattedData = formatLogData(file, fileSize);

            // 上传到存储服务
            CompletableFuture<Void> future = storageService.putObject(retryObjectName, formattedData);

            // 30秒超时
            future.get(UPLOAD_TIMEOUT_SECONDS, TimeUnit.SECONDS);

            // 上传成功后删除本地文件
            Files.delete(file);
            logger.info("Successfully resent fallback file as: {}", retryObjectName);
        } catch (Exception e) {
            logger.error("Failed to retry upload for file: {}", file.getFileName(), e);
        }
    }

    private void quarantineOversizedFile(Path file, long fileSize) {
        try {
            Path quarantineDir = Paths.get(absoluteFallbackPath, "quarantine");
            Files.createDirectories(quarantineDir);

            String quarantineName = file.getFileName() + "." + System.currentTimeMillis() + ".quarantine";
            Path quarantinePath = quarantineDir.resolve(quarantineName);
            Files.move(file, quarantinePath, StandardCopyOption.REPLACE_EXISTING);

            logger.error("Fallback file exceeds max retry size and moved to quarantine, file: {}, size: {}, limit: {}, quarantine: {}",
                file.getFileName(), fileSize, maxRetryFileBytes, quarantinePath);
        } catch (Exception e) {
            logger.error("Failed to quarantine oversized fallback file: {}", file.getFileName(), e);
        }
    }
    
    /**
     * 将原始日志数据格式化为友好的pattern格式
     * 
     * @param file 原始日志文件
     * @param fileSize 文件大小
     * @return 格式化后的日志数据
     */
    private byte[] formatLogData(Path file, long fileSize) throws IOException {
        if (isBinaryData(file)) {
            String message = String.format("[%s] [INFO] FallbackRetry - 重试上传兜底文件，原始大小: %d 字节%n",
                LocalDateTime.now(), fileSize);
            return message.getBytes(StandardCharsets.UTF_8);
        }

        try (InputStream inputStream = Files.newInputStream(file);
             BufferedInputStream bufferedInputStream = new BufferedInputStream(inputStream, STREAM_BUFFER_SIZE);
             InputStreamReader inputStreamReader = new InputStreamReader(bufferedInputStream, StandardCharsets.UTF_8);
             BufferedReader reader = new BufferedReader(inputStreamReader, STREAM_BUFFER_SIZE);
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.trim().isEmpty()) {
                    outputStream.write(line.getBytes(StandardCharsets.UTF_8));
                    outputStream.write('\n');
                }
            }
            return outputStream.toByteArray();
        }
    }
    
    /**
     * 判断是否为二进制数据
     * 
     * @param file 原始文件
     * @return 是否为二进制数据
     */
    private boolean isBinaryData(Path file) {
        int printableCount = 0;
        int totalCount = 0;

        try (InputStream inputStream = Files.newInputStream(file);
             BufferedInputStream bufferedInputStream = new BufferedInputStream(inputStream, STREAM_BUFFER_SIZE)) {

            byte[] sample = new byte[BINARY_SAMPLE_BYTES];
            int read = bufferedInputStream.read(sample);

            if (read <= 0) {
                return false;
            }

            totalCount = Math.min(read, BINARY_SAMPLE_BYTES);
            for (int i = 0; i < totalCount; i++) {
                int value = sample[i] & 0xFF;
                if ((value >= 32 && value <= 126) || value == '\n' || value == '\r' || value == '\t') {
                    printableCount++;
                }
            }
        } catch (IOException e) {
            logger.warn("Failed to sample fallback file for binary detection: {}", file.getFileName(), e);
            return false;
        }

        return totalCount > 0 && ((double) printableCount / totalCount) < 0.7;
    }
    
    private String getRelativePath(Path file) {
        try {
            String fullPath = file.toString();
            
            if (fullPath.startsWith(absoluteFallbackPath)) {
                return fullPath.substring(absoluteFallbackPath.length() + 1) + "/logx/";
            }
        } catch (Exception e) {
            logger.warn("Failed to extract relative path for file: {}", file.getFileName(), e);
        }
        return file.getFileName().toString();
    }
}
