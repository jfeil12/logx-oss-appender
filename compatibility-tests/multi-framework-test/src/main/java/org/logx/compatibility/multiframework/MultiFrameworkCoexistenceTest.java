package org.logx.compatibility.multiframework;

import org.slf4j.LoggerFactory;
import org.apache.logging.log4j.LogManager;

/**
 * 多框架共存测试类
 * 用于验证Logback、Log4j2和Log4j 1.x在同一应用中协同工作
 */
public class MultiFrameworkCoexistenceTest {

    // Logback logger (通过SLF4J)
    private static final org.slf4j.Logger logbackLogger = LoggerFactory.getLogger("LogbackLogger");

    // Log4j2 logger
    private static final org.apache.logging.log4j.Logger log4j2Logger = LogManager.getLogger("Log4j2Logger");

    // Log4j 1.x logger
    private static final org.apache.log4j.Logger log4j1Logger = org.apache.log4j.Logger.getLogger("Log4j1Logger");

    public static void main(String[] args) {
        // 使用Logback记录日志
        logbackLogger.trace("这是一条Logback TRACE级别的日志消息");
        logbackLogger.debug("这是一条Logback DEBUG级别的日志消息");
        logbackLogger.info("这是一条Logback INFO级别的日志消息");
        logbackLogger.warn("这是一条Logback WARN级别的日志消息");
        logbackLogger.error("这是一条Logback ERROR级别的日志消息");

        // 使用Log4j2记录日志
        log4j2Logger.trace("这是一条Log4j2 TRACE级别的日志消息");
        log4j2Logger.debug("这是一条Log4j2 DEBUG级别的日志消息");
        log4j2Logger.info("这是一条Log4j2 INFO级别的日志消息");
        log4j2Logger.warn("这是一条Log4j2 WARN级别的日志消息");
        log4j2Logger.error("这是一条Log4j2 ERROR级别的日志消息");

        // 使用Log4j 1.x记录日志
        log4j1Logger.trace("这是一条Log4j 1.x TRACE级别的日志消息");
        log4j1Logger.debug("这是一条Log4j 1.x DEBUG级别的日志消息");
        log4j1Logger.info("这是一条Log4j 1.x INFO级别的日志消息");
        log4j1Logger.warn("这是一条Log4j 1.x WARN级别的日志消息");
        log4j1Logger.error("这是一条Log4j 1.x ERROR级别的日志消息");

        logbackLogger.info("多框架日志消息已生成");
    }

    public static void generateLogMessages() {
        // 使用Logback记录日志
        logbackLogger.trace("这是一条Logback TRACE级别的日志消息");
        logbackLogger.debug("这是一条Logback DEBUG级别的日志消息");
        logbackLogger.info("这是一条Logback INFO级别的日志消息");
        logbackLogger.warn("这是一条Logback WARN级别的日志消息");
        logbackLogger.error("这是一条Logback ERROR级别的日志消息");

        // 使用Log4j2记录日志
        log4j2Logger.trace("这是一条Log4j2 TRACE级别的日志消息");
        log4j2Logger.debug("这是一条Log4j2 DEBUG级别的日志消息");
        log4j2Logger.info("这是一条Log4j2 INFO级别的日志消息");
        log4j2Logger.warn("这是一条Log4j2 WARN级别的日志消息");
        log4j2Logger.error("这是一条Log4j2 ERROR级别的日志消息");

        // 使用Log4j 1.x记录日志
        log4j1Logger.trace("这是一条Log4j 1.x TRACE级别的日志消息");
        log4j1Logger.debug("这是一条Log4j 1.x DEBUG级别的日志消息");
        log4j1Logger.info("这是一条Log4j 1.x INFO级别的日志消息");
        log4j1Logger.warn("这是一条Log4j 1.x WARN级别的日志消息");
        log4j1Logger.error("这是一条Log4j 1.x ERROR级别的日志消息");
    }
}