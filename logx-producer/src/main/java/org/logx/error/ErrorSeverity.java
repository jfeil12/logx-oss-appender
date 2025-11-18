package org.logx.error;

/**
 * 错误严重程度枚举 定义错误的严重程度级别
 *
 * @author OSS Appender Team
 *
 * @since 1.0.0
 */
public enum ErrorSeverity {

    /** 低严重程度 - 不影响核心功能 */
    LOW,

    /** 中等严重程度 - 部分影响功能 */
    MEDIUM,

    /** 高严重程度 - 严重影响功能 */
    HIGH,

    /** 致命 - 导致系统无法正常工作 */
    CRITICAL
}
