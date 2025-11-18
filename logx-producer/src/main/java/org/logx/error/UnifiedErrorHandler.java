package org.logx.error;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 简化版统一错误处理器 只处理所有框架适配器（Log4j、Log4j2、Logback）的异常情况，记录错误日志
 *
 * @author OSS Appender Team
 *
 * @since 1.0.0
 */
public class UnifiedErrorHandler {

    private static final Logger logger = LoggerFactory.getLogger(UnifiedErrorHandler.class);

    private final String componentName;

    public UnifiedErrorHandler() {
        this("UnknownComponent");
    }

    public UnifiedErrorHandler(String componentName) {
        this.componentName = componentName;
    }

    /**
     * 处理错误 - 只记录错误日志
     */
    public boolean handleError(ErrorContext context) {
        try {
            // 记录错误日志
            logError(context);
            return true;
        } catch (Exception e) {
            // 错误处理器本身出错，记录但不抛出异常
            logger.error("[{}] Error handler failed", componentName, e);
            return false;
        }
    }

    /**
     * 处理警告 - 只记录警告日志
     */
    public void handleWarning(ErrorContext context) {
        try {
            // 记录警告日志
            logWarning(context);
        } catch (Exception e) {
            logger.warn("[{}] Warning handler failed", componentName, e);
        }
    }

    /**
     * 处理致命错误 - 只记录致命错误日志
     */
    public void handleFatalError(ErrorContext context) {
        try {
            // 记录致命错误
            logFatalError(context);
        } catch (Exception e) {
            logger.error("[{}] Fatal error handler failed", componentName, e);
        }
    }

    /**
     * 记录错误日志
     */
    private void logError(ErrorContext context) {
        String message = formatErrorMessage(context);

        switch (context.getType()) {
            case CONFIGURATION_ERROR:
                logger.error("[{}] [CONFIG-ERROR] {}", componentName, message, context.getThrowable());
                break;
            case NETWORK_ERROR:
                logger.error("[{}] [NETWORK-ERROR] {}", componentName, message, context.getThrowable());
                break;
            case AUTHENTICATION_ERROR:
                logger.error("[{}] [AUTH-ERROR] {}", componentName, message);
                break;
            case QUEUE_ERROR:
                logger.error("[{}] [QUEUE-ERROR] {}", componentName, message, context.getThrowable());
                break;
            case STORAGE_ERROR:
                logger.error("[{}] [STORAGE-ERROR] {}", componentName, message, context.getThrowable());
                break;
            default:
                logger.error("[{}] [UNKNOWN-ERROR] {}", componentName, message, context.getThrowable());
        }
    }

    /**
     * 记录警告日志
     */
    private void logWarning(ErrorContext context) {
        String message = formatErrorMessage(context);
        logger.warn("[{}] [WARNING] {}", componentName, message, context.getThrowable());
    }

    /**
     * 记录致命错误日志
     */
    private void logFatalError(ErrorContext context) {
        String message = formatErrorMessage(context);
        logger.error("[{}] [FATAL-ERROR] {}", componentName, message, context.getThrowable());
    }

    /**
     * 格式化错误消息
     */
    private String formatErrorMessage(ErrorContext context) {
        StringBuilder message = new StringBuilder();
        message.append("[").append(context.getErrorCode()).append("] ");
        message.append(context.getMessage());

        if (context.getDetails() != null && !context.getDetails().isEmpty()) {
            message.append(" Details: ").append(context.getDetails());
        }

        if (context.getSuggestion() != null && !context.getSuggestion().isEmpty()) {
            message.append(" Suggestion: ").append(context.getSuggestion());
        }

        return message.toString();
    }
}
