package org.logx.error;

/**
 * 错误类型枚举 按照错误的性质分类
 *
 * @author OSS Appender Team
 *
 * @since 1.0.0
 */
public enum ErrorType {

    /** 配置错误 */
    CONFIGURATION_ERROR,

    /** 网络错误 */
    NETWORK_ERROR,

    /** 认证错误 */
    AUTHENTICATION_ERROR,

    /** 队列错误 */
    QUEUE_ERROR,

    /** 存储错误 */
    STORAGE_ERROR,

    /** 序列化错误 */
    SERIALIZATION_ERROR,

    /** 系统错误 */
    SYSTEM_ERROR,

    /** 业务逻辑错误 */
    BUSINESS_ERROR,

    /** 未知错误 */
    UNKNOWN
}
