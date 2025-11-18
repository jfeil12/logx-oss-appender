package org.logx.fallback;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 兜底文件管理器
 * <p>
 * 负责兜底文件的存储和管理
 *
 * @author OSS Appender Team
 * @since 1.0.0
 */
public class FallbackManager {

    private static final Logger logger = LoggerFactory.getLogger(FallbackManager.class);

    private final String fallbackPath;
    private final String absoluteFallbackPath;
    private final String keyPrefix;

    /**
     * 构造兜底文件管理器
     *
     * @param fallbackPath 兜底文件存储路径
     * @param keyPrefix 对象键前缀
     * @throws IllegalArgumentException 如果参数为null或空
     */
    public FallbackManager(String fallbackPath, String keyPrefix) {
        if (fallbackPath == null || fallbackPath.trim().isEmpty()) {
            throw new IllegalArgumentException("Fallback path cannot be null or empty");
        }

        this.fallbackPath = fallbackPath.trim();
        this.absoluteFallbackPath = FallbackPathResolver.resolveAbsolutePath(this.fallbackPath);
        this.keyPrefix = keyPrefix;
        FallbackPathResolver.ensureFallbackDirectoryExists(this.fallbackPath);
    }
    
    /**
     * 写入兜底文件
     * @param data 日志数据
     * @return 是否写入成功
     */
    public boolean writeFallbackFile(byte[] data) {
        if (data == null) {
            logger.warn("Attempted to write null data to fallback file");
            return false;
        }
        
        if (data.length == 0) {
            logger.warn("Attempted to write empty data to fallback file");
            return false;
        }
        
        try {
            String fallbackObjectName = ObjectNameGenerator.generateObjectName(keyPrefix);
            Path fallbackFile = Paths.get(absoluteFallbackPath, fallbackObjectName);
            
            // 确保目录存在
            Path parentDir = fallbackFile.getParent();
            if (parentDir != null) {
                Files.createDirectories(parentDir);
            }
            
            Files.write(fallbackFile, data);
            
            logger.info("Wrote fallback file: {} (size: {} bytes)", fallbackObjectName, data.length);
            return true;
        } catch (IOException e) {
            logger.error("Failed to write fallback file with data size: {} bytes", data.length, e);
            return false;
        } catch (Exception e) {
            logger.error("Unexpected error while writing fallback file with data size: {} bytes", data.length, e);
            return false;
        }
    }
    
    /**
     * 获取兜底路径
     * @return 兜底路径
     */
    public String getFallbackPath() {
        return fallbackPath;
    }
    
    /**
     * 获取绝对兜底路径
     * @return 绝对兜底路径
     */
    public String getAbsoluteFallbackPath() {
        return absoluteFallbackPath;
    }
}