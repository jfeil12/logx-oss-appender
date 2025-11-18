package org.logx.compatibility.spring.mvc.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 测试控制器，用于生成各种级别的业务日志消息
 */
@Controller
public class TestLogController {

    private static final Logger logger = LoggerFactory.getLogger(TestLogController.class);

    @GetMapping(value = "/test-log", produces = "text/plain;charset=UTF-8")
    @ResponseBody
    public String testLog(@RequestParam(value = "level", defaultValue = "all") String level,
                         @RequestParam(value = "count", defaultValue = "10") int count) {

        String sessionId = UUID.randomUUID().toString().substring(0, 8);
        long startTime = System.currentTimeMillis();

        logger.info("开始生成业务日志测试 - 会话ID: {}, 级别: {}, 数量: {}", sessionId, level, count);

        for (int i = 1; i <= count; i++) {
            String requestId = UUID.randomUUID().toString().substring(0, 8);

            if ("all".equals(level) || "trace".equals(level)) {
                logger.trace("[{}][{}] 用户操作跟踪 - 步骤{}: 进入业务逻辑处理", sessionId, requestId, i);
            }

            if ("all".equals(level) || "debug".equals(level)) {
                logger.debug("[{}][{}] 调试信息 - 第{}次处理: 参数验证通过, 耗时: {}ms",
                           sessionId, requestId, i, ThreadLocalRandom.current().nextInt(1, 50));
            }

            if ("all".equals(level) || "info".equals(level)) {
                // 模拟业务日志
                logger.info("[{}][{}] 业务操作 - 用户查询商品: 商品ID={}, 用户IP=192.168.1.{}, 时间={}",
                          sessionId, requestId, ThreadLocalRandom.current().nextInt(1000, 9999),
                          ThreadLocalRandom.current().nextInt(1, 255), LocalDateTime.now());

                logger.info("[{}][{}] 系统监控 - 内存使用: {}MB, CPU使用率: {}%, 请求处理时间: {}ms",
                          sessionId, requestId, ThreadLocalRandom.current().nextInt(100, 800),
                          ThreadLocalRandom.current().nextInt(10, 90), ThreadLocalRandom.current().nextInt(10, 200));
            }

            if ("all".equals(level) || "warn".equals(level)) {
                if (i % 3 == 0) {
                    logger.warn("[{}][{}] 性能警告 - 响应时间超过阈值: {}ms > 100ms, 建议优化查询",
                              sessionId, requestId, ThreadLocalRandom.current().nextInt(100, 300));
                }
            }

            if ("all".equals(level) || "error".equals(level)) {
                if (i % 5 == 0) {
                    logger.error("[{}][{}] 业务错误 - 库存不足（测试错误日志）: 商品ID={}, 请求数量={}, 剩余库存={}",
                               sessionId, requestId, ThreadLocalRandom.current().nextInt(1000, 9999),
                               ThreadLocalRandom.current().nextInt(1, 10), ThreadLocalRandom.current().nextInt(0, 5));
                }
            }

            // 模拟处理延迟
            try {
                Thread.sleep(ThreadLocalRandom.current().nextInt(1, 10));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        long endTime = System.currentTimeMillis();
        logger.info("业务日志生成完成 - 会话ID: {}, 总耗时: {}ms, 生成日志数量: 约{}条",
                   sessionId, endTime - startTime, count * 4);

        return String.format("业务日志已生成 - 会话ID: %s, 级别: %s, 数量: %d条", sessionId, level, count);
    }

    @GetMapping(value = "/test-exception", produces = "text/plain;charset=UTF-8")
    @ResponseBody
    public String testException(@RequestParam(value = "stacktrace", defaultValue = "true") boolean includeStackTrace) {
        String sessionId = UUID.randomUUID().toString().substring(0, 8);

        logger.info("[{}] 开始异常测试 - 包含堆栈跟踪: {}", sessionId, includeStackTrace);

        try {
            // 模拟多层调用栈的异常
            simulateBusinessProcess(sessionId);
        } catch (Exception e) {
            if (includeStackTrace) {
                logger.error("[{}] 业务处理异常（测试错误日志） - 订单处理失败", sessionId, e);
            } else {
                logger.error("[{}] 业务处理异常（测试错误日志） - 订单处理失败: {}", sessionId, e.getMessage());
            }
        }

        // 模拟一些额外的业务日志
        logger.warn("[{}] 异常恢复 - 尝试使用备用处理流程", sessionId);
        logger.info("[{}] 业务降级 - 转入手工处理队列", sessionId);

        return String.format("异常日志已生成 - 会话ID: %s", sessionId);
    }

    private void simulateBusinessProcess(String sessionId) throws Exception {
        logger.debug("[{}] 进入业务处理流程", sessionId);

        // 模拟订单验证
        validateOrder(sessionId);
    }

    private void validateOrder(String sessionId) throws Exception {
        logger.debug("[{}] 验证订单信息", sessionId);

        // 模拟支付处理
        processPayment(sessionId);
    }

    private void processPayment(String sessionId) throws Exception {
        logger.debug("[{}] 处理支付请求", sessionId);

        // 模拟支付失败
        throw new RuntimeException("支付网关连接超时 - 第三方服务不可用");
    }

    @GetMapping(value = "/test-high-volume", produces = "text/plain;charset=UTF-8")
    @ResponseBody
    public String testHighVolume(@RequestParam(value = "volume", defaultValue = "100") int volume) {
        String sessionId = UUID.randomUUID().toString().substring(0, 8);

        logger.info("[{}] 开始高容量日志测试 - 目标数量: {}条", sessionId, volume);

        long startTime = System.currentTimeMillis();

        for (int i = 1; i <= volume; i++) {
            String transactionId = UUID.randomUUID().toString().substring(0, 8);

            // 模拟高频业务日志
            logger.info("[{}][{}] 交易记录 - 订单号: ORD{}, 金额: {}.{}元, 支付方式: {}",
                      sessionId, transactionId,
                      String.format("%06d", i),
                      ThreadLocalRandom.current().nextInt(10, 999),
                      ThreadLocalRandom.current().nextInt(10, 99),
                      ThreadLocalRandom.current().nextBoolean() ? "微信支付" : "支付宝");

            if (i % 10 == 0) {
                logger.debug("[{}] 批次处理进度 - 已处理: {}/{} ({}%)",
                           sessionId, i, volume, (i * 100) / volume);
            }

            if (i % 50 == 0) {
                logger.warn("[{}] 性能监控 - 当前TPS: {}, 平均响应时间: {}ms",
                          sessionId, ThreadLocalRandom.current().nextInt(800, 1200),
                          ThreadLocalRandom.current().nextInt(20, 80));
            }
        }

        long endTime = System.currentTimeMillis();
        logger.info("[{}] 高容量日志测试完成 - 生成{}条日志, 总耗时: {}ms, 平均TPS: {}",
                   sessionId, volume, endTime - startTime,
                   volume * 1000 / Math.max(1, endTime - startTime));

        return String.format("高容量日志已生成 - 会话ID: %s, 数量: %d条", sessionId, volume);
    }
}