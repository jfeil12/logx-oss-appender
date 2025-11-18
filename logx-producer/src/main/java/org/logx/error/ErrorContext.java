package org.logx.error;

import java.util.HashMap;
import java.util.Map;

/**
 * 错误上下文 包含错误的详细信息和上下文数据
 *
 * @author OSS Appender Team
 *
 * @since 1.0.0
 */
public class ErrorContext {

    private final ErrorCode errorCode;
    private final ErrorType type;
    private final ErrorSeverity severity;
    private final String message;
    private final Throwable throwable;
    private final String details;
    private final String suggestion;
    private final Map<String, Object> contextData;
    private final long timestamp;

    private ErrorContext(Builder builder) {
        this.errorCode = builder.errorCode;
        this.type = builder.type;
        this.severity = builder.severity;
        this.message = builder.message;
        this.throwable = builder.throwable;
        this.details = builder.details;
        this.suggestion = builder.suggestion;
        this.contextData = new HashMap<>(builder.contextData);
        this.timestamp = System.currentTimeMillis();
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    public ErrorType getType() {
        return type;
    }

    public ErrorSeverity getSeverity() {
        return severity;
    }

    public String getMessage() {
        return message;
    }

    public Throwable getThrowable() {
        // 返回异常的副本以避免内部表示暴露
        return throwable != null ? new RuntimeException(throwable.getMessage(), throwable.getCause()) : null;
    }

    public String getDetails() {
        return details;
    }

    public String getSuggestion() {
        return suggestion;
    }

    public Map<String, Object> getContextData() {
        return new HashMap<>(contextData);
    }

    public long getTimestamp() {
        return timestamp;
    }

    /**
     * 创建错误上下文构建器
     */
    public static Builder builder(ErrorCode errorCode, String message) {
        return new Builder(errorCode, message);
    }

    /**
     * 错误上下文构建器
     */
    public static class Builder {
        private ErrorCode errorCode;
        private String message;
        private ErrorType type = ErrorType.UNKNOWN;
        private ErrorSeverity severity = ErrorSeverity.MEDIUM;
        private Throwable throwable;
        private String details;
        private String suggestion;
        private Map<String, Object> contextData = new HashMap<>();

        public Builder() {
        }

        private Builder(ErrorCode errorCode, String message) {
            this.errorCode = errorCode;
            this.message = message;
        }

        public Builder errorCode(ErrorCode errorCode) {
            this.errorCode = errorCode;
            return this;
        }

        public Builder message(String message) {
            this.message = message;
            return this;
        }

        public Builder type(ErrorType type) {
            this.type = type;
            return this;
        }

        public Builder severity(ErrorSeverity severity) {
            this.severity = severity;
            return this;
        }

        public Builder throwable(Throwable throwable) {
            // 存储异常的副本以避免内部表示暴露
            this.throwable = throwable != null ? new RuntimeException(throwable.getMessage(), throwable.getCause()) : null;
            return this;
        }

        public Builder details(String details) {
            this.details = details;
            return this;
        }

        public Builder suggestion(String suggestion) {
            this.suggestion = suggestion;
            return this;
        }

        public Builder addContextData(String key, Object value) {
            this.contextData.put(key, value);
            return this;
        }

        public Builder contextData(Map<String, Object> contextData) {
            this.contextData.putAll(contextData);
            return this;
        }

        public ErrorContext build() {
            if (errorCode == null) {
                throw new IllegalArgumentException("errorCode is required");
            }
            if (message == null || message.trim().isEmpty()) {
                throw new IllegalArgumentException("message is required");
            }
            return new ErrorContext(this);
        }
    }
}
