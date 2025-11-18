package org.logx.error;

/**
 * 统一错误代码枚举 定义所有可能的错误类型和对应的错误代码
 *
 * @author OSS Appender Team
 *
 * @since 1.0.0
 */
public enum ErrorCode {

    // 配置相关错误 (1000-1099)
    CONFIG_MISSING_REQUIRED_FIELD("E1001", "Required configuration field is missing"),
    CONFIG_INVALID_FORMAT("E1002", "Configuration field has invalid format"),
    CONFIG_VALUE_OUT_OF_RANGE("E1003", "Configuration value is out of valid range"),
    CONFIG_DEPENDENCY_CONFLICT("E1004", "Configuration dependency conflict"),

    // 网络相关错误 (1100-1199)
    NETWORK_CONNECTION_FAILED("E1101", "Failed to establish network connection"),
    NETWORK_TIMEOUT("E1102", "Network operation timed out"),
    NETWORK_DNS_RESOLUTION_FAILED("E1103", "DNS resolution failed"),
    NETWORK_SSL_HANDSHAKE_FAILED("E1104", "SSL handshake failed"),

    // 认证相关错误 (1200-1299)
    AUTH_INVALID_CREDENTIALS("E1201", "Invalid authentication credentials"),
    AUTH_EXPIRED_TOKEN("E1202", "Authentication token has expired"),
    AUTH_INSUFFICIENT_PERMISSIONS("E1203", "Insufficient permissions for operation"),
    AUTH_ACCESS_DENIED("E1204", "Access denied"),

    // 队列相关错误 (1300-1399)
    QUEUE_FULL("E1301", "Queue is full"), QUEUE_INITIALIZATION_FAILED("E1302", "Queue initialization failed"),
    QUEUE_SHUTDOWN_TIMEOUT("E1303", "Queue shutdown timed out"),
    QUEUE_PRODUCER_FAILED("E1304", "Queue producer operation failed"),
    QUEUE_CONSUMER_FAILED("E1305", "Queue consumer operation failed"),

    // 存储相关错误 (1400-1499)
    STORAGE_UPLOAD_FAILED("E1401", "Storage upload operation failed"),
    STORAGE_BUCKET_NOT_FOUND("E1402", "Storage bucket not found"),
    STORAGE_QUOTA_EXCEEDED("E1403", "Storage quota exceeded"),
    STORAGE_PERMISSION_DENIED("E1404", "Storage permission denied"),
    STORAGE_CORRUPTION("E1405", "Storage data corruption detected"),

    // 序列化相关错误 (1500-1599)
    SERIALIZATION_FAILED("E1501", "Data serialization failed"),
    DESERIALIZATION_FAILED("E1502", "Data deserialization failed"),
    COMPRESSION_FAILED("E1503", "Data compression failed"), DECOMPRESSION_FAILED("E1504", "Data decompression failed"),

    // 框架特定错误 (1600-1699)
    LOG4J_APPENDER_FAILED("E1601", "Log4j appender operation failed"),
    LOG4J2_PLUGIN_FAILED("E1602", "Log4j2 plugin operation failed"),
    LOGBACK_APPENDER_FAILED("E1603", "Logback appender operation failed"),
    SPRING_BOOT_AUTO_CONFIG_FAILED("E1604", "Spring Boot auto-configuration failed"),

    // 系统相关错误 (1700-1799)
    SYSTEM_OUT_OF_MEMORY("E1701", "System out of memory"),
    SYSTEM_THREAD_POOL_EXHAUSTED("E1702", "System thread pool exhausted"),
    SYSTEM_RESOURCE_UNAVAILABLE("E1703", "System resource unavailable"),
    SYSTEM_SHUTDOWN_INTERRUPTED("E1704", "System shutdown interrupted"),

    // 业务逻辑错误 (1800-1899)
    BUSINESS_RULE_VIOLATION("E1801", "Business rule violation"),
    BUSINESS_STATE_INVALID("E1802", "Invalid business state"),
    BUSINESS_OPERATION_NOT_ALLOWED("E1803", "Business operation not allowed"),

    // 未知错误 (9999)
    UNKNOWN_ERROR("E9999", "Unknown error occurred");

    private final String code;
    private final String description;

    ErrorCode(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public String toString() {
        return code + ": " + description;
    }

    /**
     * 根据错误代码字符串查找枚举值
     */
    public static ErrorCode fromCode(String code) {
        for (ErrorCode errorCode : values()) {
            if (errorCode.code.equals(code)) {
                return errorCode;
            }
        }
        return UNKNOWN_ERROR;
    }
}
