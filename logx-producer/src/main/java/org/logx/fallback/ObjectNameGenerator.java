package org.logx.fallback;

import org.logx.util.IPUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * 对象名生成器（静态工具类）
 * <p>
 * 负责生成OSS对象名和本地兜底文件名
 * <p>
 * <b>统一时间格式规则</b>：
 * <ul>
 * <li>年月日：yyyy/MM/dd</li>
 * <li>时间戳：HHmmssSSS（时分秒毫秒，紧凑格式无分隔符）</li>
 * </ul>
 * <p>
 * <b>文件格式（OSS上传、本地兜底、重试上传统一）</b>：
 * <pre>
 * yyyy/MM/dd/HHmmssSSS-{fileName}-IP-uniqueId.log.gz
 * 示例：2025/10/14/143250200-applogx-100.119.145.245-abc12345.log.gz
 * </pre>
 *
 * @author OSS Appender Team
 * @since 1.0.0
 */
public final class ObjectNameGenerator {

    private static final Logger logger = LoggerFactory.getLogger(ObjectNameGenerator.class);

    private static final DateTimeFormatter YEAR_FORMATTER = DateTimeFormatter.ofPattern("yyyy");
    private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("MM");
    private static final DateTimeFormatter DAY_FORMATTER = DateTimeFormatter.ofPattern("dd");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HHmmssSSS");
    private static final String DEFAULT_KEY_PREFIX = "logx";
    private static final String DEFAULT_FILE_NAME_PREFIX = "applogx";
    private static final String UNKNOWN_HOST = "unknown-host";
    private static final String FILE_SUFFIX = ".log.gz";

    /**
     * 私有构造器，防止实例化
     */
    private ObjectNameGenerator() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    /**
     * 生成对象名（静态方法）
     * <p>
     * 格式：yyyy/MM/dd/HHmmssSSS-applogx-IP-uniqueId.log.gz
     *
     * @return 对象名
     */
    public static String generateObjectName(String keyPrefix) {
        if (keyPrefix == null || keyPrefix.trim().isEmpty()) {
            keyPrefix = DEFAULT_KEY_PREFIX;
        } else {
            keyPrefix = keyPrefix.replaceAll("^/+|/+$", "");
        }
        

        String ip = getLocalIPForGeneration();
        LocalDateTime nowTime = LocalDateTime.now();
        String year = nowTime.format(YEAR_FORMATTER);
        String month = nowTime.format(MONTH_FORMATTER);
        String day = nowTime.format(DAY_FORMATTER);
        String time = nowTime.format(TIME_FORMATTER);
        String uniqueId = UUID.randomUUID().toString().substring(0, 8);
        return keyPrefix + "/" + year + "/" + month + "/" + day + "/" + time + "-" + DEFAULT_FILE_NAME_PREFIX + "-" + ip + "-" + uniqueId + FILE_SUFFIX;
    }

    /**
     * 获取本地IP用于生成对象名（静态方法）
     *
     * @return 本地IP地址或默认值
     */
    private static String getLocalIPForGeneration() {
        try {
            String ip = IPUtil.getLocalIP();
            if (ip == null || ip.trim().isEmpty()) {
                logger.warn("IPUtil returned null or empty IP, using default");
                return UNKNOWN_HOST;
            }
            return ip.trim();
        } catch (Exception e) {
            logger.warn("Failed to get local IP, using default", e);
            return UNKNOWN_HOST + "-" + UUID.randomUUID().toString().substring(0, 8);
        }
    }

}
