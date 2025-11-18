package org.logx.fallback;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

/**
 * 兜底文件清理器
 * <p>
 * 负责清理过期的兜底文件，防止磁盘空间无限增长
 *
 * @author OSS Appender Team
 * @since 1.0.0
 */
public class FallbackFileCleaner {
    
    private static final Logger logger = LoggerFactory.getLogger(FallbackFileCleaner.class);
    
    /**
     * 清理过期的兜底文件
     * 
     * @param fallbackPath 兜底文件路径
     * @param retentionDays 保留天数
     */
    public static void cleanupExpiredFiles(String fallbackPath, int retentionDays) {
        try {
            String absolutePath = FallbackPathResolver.resolveAbsolutePath(fallbackPath);
            Path fallbackDir = Paths.get(absolutePath);
            
            if (!Files.exists(fallbackDir) || !Files.isDirectory(fallbackDir)) {
                logger.debug("Fallback directory does not exist or is not a directory: {}", absolutePath);
                return;
            }
            
            // 计算过期时间
            LocalDateTime expiryTime = LocalDateTime.now().minusDays(retentionDays);
            
            // 遍历并清理过期文件
            AtomicInteger deletedCount = new AtomicInteger(0);
            try (Stream<Path> files = Files.walk(fallbackDir)) {
                files.filter(Files::isRegularFile)
                     .filter(file -> isFileExpired(file, expiryTime))
                     .forEach(file -> {
                         if (deleteFile(file)) {
                             deletedCount.incrementAndGet();
                         }
                     });
            }
            
            logger.info("Cleanup completed. Deleted {} expired fallback files from: {}", 
                deletedCount.get(), absolutePath);
        } catch (IOException e) {
            logger.error("Failed to cleanup expired fallback files in path: {}", fallbackPath, e);
        }
    }
    
    /**
     * 检查文件是否过期
     * 
     * @param file 文件路径
     * @param expiryTime 过期时间
     * @return 是否过期
     */
    private static boolean isFileExpired(Path file, LocalDateTime expiryTime) {
        try {
            // 使用文件的最后修改时间来判断是否过期
            FileTime lastModified = Files.getLastModifiedTime(file);
            LocalDateTime fileTime = LocalDateTime.ofInstant(
                lastModified.toInstant(), ZoneId.systemDefault());
            return fileTime.isBefore(expiryTime);
        } catch (Exception e) {
            logger.warn("Failed to get last modified time for file: {}", file, e);
            return false;
        }
    }
    
    /**
     * 删除文件
     * 
     * @param file 文件路径
     * @return 是否删除成功
     */
    private static boolean deleteFile(Path file) {
        try {
            Files.delete(file);
            logger.debug("Deleted expired fallback file: {}", file);
            return true;
        } catch (IOException e) {
            logger.warn("Failed to delete fallback file: {}", file, e);
            return false;
        }
    }
}