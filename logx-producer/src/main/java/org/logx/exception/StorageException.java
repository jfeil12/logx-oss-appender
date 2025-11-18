package org.logx.exception;

/**
 * 存储操作统一异常类
 * <p>
 * 封装所有S3存储操作中可能发生的异常，提供统一的错误处理机制。 支持错误分类，便于不同类型错误的特定处理策略。
 *
 * @author OSS Appender Team
 *
 * @since 1.0.0
 */
public class StorageException extends RuntimeException {

    /**
     * 错误类型枚举
     */
    public enum ErrorType {
        /**
         * 网络连接错误
         */
        NETWORK_ERROR,

        /**
         * 认证失败错误
         */
        AUTHENTICATION_ERROR,

        /**
         * 服务器内部错误
         */
        SERVER_ERROR,

        /**
         * 客户端请求错误
         */
        CLIENT_ERROR,

        /**
         * 配置错误
         */
        CONFIGURATION_ERROR,

        /**
         * 未知错误
         */
        UNKNOWN_ERROR
    }

    private final ErrorType errorType;
    private final String errorCode;
    private final boolean retryable;

    /**
     * 构造函数
     *
     * @param message
     *            错误消息
     * @param errorType
     *            错误类型
     */
    public StorageException(String message, ErrorType errorType) {
        this(message, errorType, null, null, true);
    }

    /**
     * 构造函数
     *
     * @param message
     *            错误消息
     * @param errorType
     *            错误类型
     * @param cause
     *            原始异常
     */
    public StorageException(String message, ErrorType errorType, Throwable cause) {
        this(message, errorType, null, cause, true);
    }

    /**
     * 构造函数
     *
     * @param message
     *            错误消息
     * @param errorType
     *            错误类型
     * @param errorCode
     *            错误代码
     * @param cause
     *            原始异常
     * @param retryable
     *            是否可重试
     */
    public StorageException(String message, ErrorType errorType, String errorCode, Throwable cause, boolean retryable) {
        super(message, cause);
        this.errorType = errorType;
        this.errorCode = errorCode;
        this.retryable = retryable;
    }

    /**
     * 获取错误类型
     */
    public ErrorType getErrorType() {
        return errorType;
    }

    /**
     * 获取错误代码
     */
    public String getErrorCode() {
        return errorCode;
    }

    /**
     * 判断是否可重试
     */
    public boolean isRetryable() {
        return retryable;
    }

    /**
     * 创建网络错误异常
     */
    public static StorageException networkError(String message, Throwable cause) {
        return new StorageException(message, ErrorType.NETWORK_ERROR, cause);
    }

    /**
     * 创建认证错误异常
     */
    public static StorageException authenticationError(String message, String errorCode) {
        return new StorageException(message, ErrorType.AUTHENTICATION_ERROR, errorCode, null, false);
    }

    /**
     * 创建服务器错误异常
     */
    public static StorageException serverError(String message, String errorCode, Throwable cause) {
        return new StorageException(message, ErrorType.SERVER_ERROR, errorCode, cause, true);
    }

    /**
     * 创建客户端错误异常
     */
    public static StorageException clientError(String message, String errorCode) {
        return new StorageException(message, ErrorType.CLIENT_ERROR, errorCode, null, false);
    }

    /**
     * 创建配置错误异常
     */
    public static StorageException configurationError(String message) {
        return new StorageException(message, ErrorType.CONFIGURATION_ERROR, null, null, false);
    }

    @Override
    public String toString() {
        return "StorageException{" + "errorType=" + errorType + ", errorCode='" + errorCode + '\'' + ", retryable="
                + retryable + ", message='" + getMessage() + '\'' + '}';
    }
}
