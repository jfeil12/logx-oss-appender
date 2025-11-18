package org.logx.fallback;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 兜底文件路径解析器
 * <p>
 * 负责解析和管理兜底文件的存储路径
 *
 * @author OSS Appender Team
 * @since 1.0.0
 */
public class FallbackPathResolver {
    
    private static final Logger logger = LoggerFactory.getLogger(FallbackPathResolver.class);
    private static final String DEFAULT_FALLBACK_PATH = "fallback";
    
    /**
     * 解析兜底文件的绝对路径
     * @param relativePath 相对路径
     * @return 绝对路径
     * @throws IllegalArgumentException 如果路径参数无效
     */
    public static String resolveAbsolutePath(String relativePath) {
        String pathToResolve = getPathToResolve(relativePath);
        
        try {
            // 如果已经是绝对路径，直接返回
            Path path = Paths.get(pathToResolve);
            if (path.isAbsolute()) {
                return path.toString();
            }
            
            // 获取应用启动目录
            String userDir = System.getProperty("user.dir");
            if (userDir == null || userDir.isEmpty()) {
                logger.warn("System property 'user.dir' is not available, using current directory");
                userDir = ".";
            }
            
            // 构建绝对路径
            Path absolutePath = Paths.get(userDir, pathToResolve).toAbsolutePath().normalize();
            return absolutePath.toString();
        } catch (Exception e) {
            logger.error("Failed to resolve absolute path for: {}", relativePath, e);
            // Fallback to a safe default
            return Paths.get(DEFAULT_FALLBACK_PATH).toAbsolutePath().normalize().toString();
        }
    }
    
    /**
     * 确保兜底目录存在
     * @param fallbackPath 兜底路径
     */
    public static void ensureFallbackDirectoryExists(String fallbackPath) {
        String pathToEnsure = getPathToResolve(fallbackPath);
        
        try {
            String absolutePath = resolveAbsolutePath(pathToEnsure);
            Path path = Paths.get(absolutePath);
            
            if (!Files.exists(path)) {
                logger.info("Creating fallback directory: {}", absolutePath);
                Files.createDirectories(path);
            } else if (!Files.isDirectory(path)) {
                logger.error("Fallback path exists but is not a directory: {}", absolutePath);
                throw new IOException("Fallback path exists but is not a directory: " + absolutePath);
            } else {
                logger.debug("Fallback directory already exists: {}", absolutePath);
            }
        } catch (IOException e) {
            logger.error("Failed to create or validate fallback directory: {}", pathToEnsure, e);
            throw new RuntimeException("Unable to ensure fallback directory exists: " + pathToEnsure, e);
        } catch (Exception e) {
            logger.error("Unexpected error while ensuring fallback directory exists: {}", pathToEnsure, e);
            throw new RuntimeException("Unexpected error while ensuring fallback directory exists: " + pathToEnsure, e);
        }
    }
    
    /**
     * 获取要解析的路径
     * @param path 原始路径
     * @return 处理后的路径
     */
    private static String getPathToResolve(String path) {
        if (path == null) {
            return DEFAULT_FALLBACK_PATH;
        }
        
        String trimmedPath = path.trim();
        return trimmedPath.isEmpty() ? DEFAULT_FALLBACK_PATH : trimmedPath;
    }
}