package org.logx.fallback;

import org.logx.core.EnhancedDisruptorBatchingQueue;
import org.logx.storage.StorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
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
    
    private final StorageService storageService;
    private final String fallbackPath;
    private final String absoluteFallbackPath;
    private final int retentionDays;

    /**
     * @deprecated fileName参数已废弃，ObjectNameGenerator使用固定默认值
     */
    @Deprecated
    public FallbackUploaderTask(StorageService storageService, String fallbackPath, String fileName, int retentionDays) {
        this.storageService = storageService;
        this.fallbackPath = fallbackPath;
        this.absoluteFallbackPath = FallbackPathResolver.resolveAbsolutePath(fallbackPath);
        this.retentionDays = retentionDays;
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
            
            // 遍历兜底目录中的所有文件
            try (Stream<Path> files = Files.walk(fallbackDir)) {
                files.filter(Files::isRegularFile)
                     .filter(path -> path.toString().endsWith(FALLBACK_FILE_SUFFIX))
                     .forEach(this::retryUpload);
            }
        } catch (IOException e) {
            logger.error("Failed to scan fallback directory: {}", absoluteFallbackPath, e);
        }
    }
    
    private void retryUpload(Path file) {
        try {
            // 使用源文件的相对路径作为对象名，保留原有的日期和时间信息
            String retryObjectName = getRelativePath(file);

            byte[] rawData = Files.readAllBytes(file);

            // 将原始数据转换为格式化的日志数据
            byte[] formattedData = formatLogData(rawData);

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
    
    /**
     * 将原始日志数据格式化为友好的pattern格式
     * 
     * @param rawData 原始日志数据
     * @return 格式化后的日志数据
     */
    private byte[] formatLogData(byte[] rawData) {
        try {
            // 将原始数据转换为字符串
            String rawContent = new String(rawData, java.nio.charset.StandardCharsets.UTF_8);
            
            // 如果已经是格式化的日志内容（包含换行符且不包含明显的二进制数据特征），直接返回
            if (rawContent.contains("\n") && !isBinaryData(rawContent)) {
                return rawData;
            }
            
            // 创建一个模拟的日志事件列表
            List<EnhancedDisruptorBatchingQueue.LogEvent> events = new ArrayList<>();
            long timestamp = System.currentTimeMillis();
            
            // 如果是二进制数据，尝试以友好的方式显示
            if (isBinaryData(rawContent)) {
                String formattedLine = String.format("[%s] [INFO] FallbackRetry - 重试上传兜底文件，原始大小: %d 字节%n", 
                    java.time.LocalDateTime.now().toString(), rawData.length);
                events.add(new EnhancedDisruptorBatchingQueue.LogEvent(
                    formattedLine.getBytes(java.nio.charset.StandardCharsets.UTF_8), timestamp));
            } else {
                // 对于文本数据，按行分割并格式化
                String[] lines = rawContent.split("\n");
                for (String line : lines) {
                    if (!line.trim().isEmpty()) {
                        String formattedLine = line + "\n";
                        events.add(new EnhancedDisruptorBatchingQueue.LogEvent(
                            formattedLine.getBytes(java.nio.charset.StandardCharsets.UTF_8), timestamp));
                    }
                }
            }
            
            // 使用与正常处理相同的序列化方法
            List<EnhancedDisruptorBatchingQueue.LogEvent> eventList = new ArrayList<>(events);
            byte[] formattedData = serializeToPatternFormat(eventList);
            
            return formattedData;
        } catch (Exception e) {
            logger.warn("Failed to format log data, using raw data", e);
            return rawData;
        }
    }
    
    /**
     * 判断是否为二进制数据
     * 
     * @param content 内容
     * @return 是否为二进制数据
     */
    private boolean isBinaryData(String content) {
        // 简单判断：如果包含大量不可打印字符，可能是二进制数据
        int printableCount = 0;
        int totalCount = Math.min(content.length(), 1000); // 只检查前1000个字符
        
        for (int i = 0; i < totalCount; i++) {
            char c = content.charAt(i);
            if (c >= 32 && c <= 126 || c == '\n' || c == '\r' || c == '\t') {
                printableCount++;
            }
        }
        
        // 如果可打印字符比例小于70%，认为是二进制数据
        return totalCount > 0 && ((double) printableCount / totalCount) < 0.7;
    }
    
    /**
     * 序列化为Pattern格式（与EnhancedDisruptorBatchingQueue中的方法保持一致）
     * 
     * @param events 日志事件列表
     * @return 格式化后的字节数组
     */
    private byte[] serializeToPatternFormat(List<EnhancedDisruptorBatchingQueue.LogEvent> events) {
        StringBuilder sb = new StringBuilder();
        for (EnhancedDisruptorBatchingQueue.LogEvent event : events) {
            // 使用标准的日志格式: timestamp [level] logger - message
            String logLine = new String(event.payload, java.nio.charset.StandardCharsets.UTF_8);
            
            // 如果日志行不以换行符结尾，添加换行符
            if (!logLine.endsWith("\n")) {
                sb.append(logLine).append("\n");
            } else {
                sb.append(logLine);
            }
        }
        return sb.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
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