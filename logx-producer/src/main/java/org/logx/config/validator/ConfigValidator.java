package org.logx.config.validator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 配置验证器接口
 * <p>
 * 定义配置验证的标准规范，支持参数完整性检查、格式验证和业务逻辑验证。
 *
 * @author OSS Appender Team
 *
 * @since 1.0.0
 */
public interface ConfigValidator {

    /**
     * 验证配置的有效性
     *
     * @param config
     *            要验证的配置对象
     *
     * @return 验证结果
     */
    ValidationResult validate(Object config);

    /**
     * 配置验证结果
     */
    class ValidationResult {
        private final boolean valid;
        private final List<ValidationError> errors;

        public ValidationResult(boolean valid, List<ValidationError> errors) {
            this.valid = valid;
            this.errors = errors != null ? errors : Collections.emptyList();
        }

        /**
         * 配置是否有效
         *
         * @return true如果配置有效
         */
        public boolean isValid() {
            return valid;
        }

        /**
         * 获取验证错误列表
         *
         * @return 验证错误列表
         */
        public List<ValidationError> getErrors() {
            // 返回错误列表的副本以避免内部表示暴露
            return new ArrayList<>(errors);
        }

        /**
         * 获取错误消息摘要
         *
         * @return 错误消息字符串
         */
        public String getErrorSummary() {
            if (valid) {
                return "Configuration is valid";
            }
            StringBuilder summary = new StringBuilder("Configuration validation failed:");
            for (ValidationError error : errors) {
                summary.append("\n  - ").append(error.getMessage());
            }
            return summary.toString();
        }

        /**
         * 创建有效的验证结果
         *
         * @return 有效的验证结果
         */
        public static ValidationResult valid() {
            return new ValidationResult(true, Collections.emptyList());
        }

        /**
         * 创建无效的验证结果
         *
         * @param errors
         *            验证错误列表
         *
         * @return 无效的验证结果
         */
        public static ValidationResult invalid(List<ValidationError> errors) {
            return new ValidationResult(false, errors != null ? errors : Collections.emptyList());
        }
    }

    /**
     * 配置验证错误
     */
    class ValidationError {
        private final String field;
        private final String message;
        private final String suggestion;
        private final ErrorType type;
        private final Throwable cause;

        public ValidationError(String field, String message, String suggestion, ErrorType type) {
            this(field, message, suggestion, type, null);
        }

        public ValidationError(String field, String message, String suggestion, ErrorType type, Throwable cause) {
            this.field = field;
            this.message = message;
            this.suggestion = suggestion;
            this.type = type;
            // 存储异常的副本以避免内部表示暴露
            this.cause = cause != null ? new RuntimeException(cause.getMessage(), cause.getCause()) : null;
        }

        public String getField() {
            return field;
        }

        public String getMessage() {
            return message;
        }

        public String getSuggestion() {
            return suggestion;
        }

        public ErrorType getType() {
            return type;
        }

        public Throwable getCause() {
            // 返回异常的副本以避免内部表示暴露
            return cause != null ? new RuntimeException(cause.getMessage(), cause.getCause()) : null;
        }

        @Override
        public String toString() {
            return String.format("[%s] %s: %s%s", type, field, message,
                    suggestion != null ? " (Suggestion: " + suggestion + ")" : "");
        }
    }

    /**
     * 验证错误类型
     */
    enum ErrorType {
        /** 必需字段缺失 */
        REQUIRED_FIELD_MISSING,
        /** 字段格式无效 */
        INVALID_FORMAT,
        /** 字段值超出范围 */
        VALUE_OUT_OF_RANGE,
        /** 字段依赖冲突 */
        DEPENDENCY_CONFLICT,
        /** 网络连接问题 */
        NETWORK_ERROR,
        /** 权限认证问题 */
        AUTHENTICATION_ERROR,
        /** 业务逻辑错误 */
        BUSINESS_LOGIC_ERROR,
        /** 配置警告 */
        WARNING
    }

    /**
     * 验证选项
     */
    class ValidationOptions {
        private final boolean strictMode;
        private final boolean includeWarnings;

        private ValidationOptions(boolean strictMode, boolean includeWarnings) {
            this.strictMode = strictMode;
            this.includeWarnings = includeWarnings;
        }

        public boolean isStrictMode() {
            return strictMode;
        }

        public boolean isIncludeWarnings() {
            return includeWarnings;
        }

        public static ValidationOptions defaultOptions() {
            return new ValidationOptions(false, false);
        }

        public static ValidationOptions strict() {
            return new ValidationOptions(true, true);
        }

        public static ValidationOptions withWarnings() {
            return new ValidationOptions(false, true);
        }
    }

    /**
     * 带选项的验证方法
     *
     * @param config
     *            要验证的配置对象
     * @param options
     *            验证选项
     *
     * @return 验证结果
     */
    default ValidationResult validate(Object config, ValidationOptions options) {
        // 默认实现忽略选项，保持向后兼容
        return validate(config);
    }
}