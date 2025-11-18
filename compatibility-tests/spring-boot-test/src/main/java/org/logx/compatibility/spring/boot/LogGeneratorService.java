package org.logx.compatibility.spring.boot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 业务日志生成服务 - 用于生成各种真实业务场景的日志
 */
@Service
public class LogGeneratorService {

    private static final Logger logger = LoggerFactory.getLogger(LogGeneratorService.class);
    private final ExecutorService executorService = Executors.newFixedThreadPool(5);

    /**
     * 生成电商业务日志
     */
    public String generateECommerceBusinessLogs(int count, String level) {
        String sessionId = UUID.randomUUID().toString().substring(0, 8);

        logger.info("[{}] 开始生成电商业务日志 - 数量: {}, 级别: {}", sessionId, count, level);

        long startTime = System.currentTimeMillis();

        for (int i = 1; i <= count; i++) {
            String orderId = "ORD" + String.format("%08d", ThreadLocalRandom.current().nextInt(10000000, 99999999));
            String userId = "USER" + ThreadLocalRandom.current().nextInt(1000, 9999);

            if ("all".equals(level) || "trace".equals(level)) {
                logger.trace("[{}] 用户行为跟踪 - 第{}次操作: 用户{}浏览商品页面, 商品ID={}",
                           sessionId, i, userId, ThreadLocalRandom.current().nextInt(1000, 9999));
            }

            if ("all".equals(level) || "debug".equals(level)) {
                logger.debug("[{}] 系统调试 - 第{}次处理: 订单{}状态检查, 库存验证通过, 耗时{}ms",
                           sessionId, i, orderId, ThreadLocalRandom.current().nextInt(10, 100));
            }

            if ("all".equals(level) || "info".equals(level)) {
                // 订单创建日志
                logger.info("[{}] 订单创建 - 订单号: {}, 用户: {}, 商品数量: {}, 总金额: {}.{}元, 支付方式: {}",
                          sessionId, orderId, userId,
                          ThreadLocalRandom.current().nextInt(1, 5),
                          ThreadLocalRandom.current().nextInt(100, 999),
                          ThreadLocalRandom.current().nextInt(10, 99),
                          ThreadLocalRandom.current().nextBoolean() ? "支付宝" : "微信支付");

                // 库存更新日志
                logger.info("[{}] 库存更新 - 商品ID: {}, 原库存: {}, 扣减数量: {}, 剩余库存: {}",
                          sessionId, ThreadLocalRandom.current().nextInt(1000, 9999),
                          ThreadLocalRandom.current().nextInt(50, 200),
                          ThreadLocalRandom.current().nextInt(1, 5),
                          ThreadLocalRandom.current().nextInt(45, 195));

                // 物流信息日志
                logger.info("[{}] 物流信息 - 订单: {}, 物流公司: {}, 运单号: {}, 预计送达: {}",
                          sessionId, orderId,
                          getRandomLogisticsCompany(),
                          "SF" + ThreadLocalRandom.current().nextLong(100000000000L, 999999999999L),
                          LocalDateTime.now().plusDays(ThreadLocalRandom.current().nextInt(1, 3)));
            }

            if ("all".equals(level) || "warn".equals(level)) {
                if (i % 5 == 0) {
                    logger.warn("[{}] 库存预警 - 商品ID: {}, 当前库存: {}, 安全库存: {}, 建议及时补货",
                              sessionId, ThreadLocalRandom.current().nextInt(1000, 9999),
                              ThreadLocalRandom.current().nextInt(1, 10),
                              ThreadLocalRandom.current().nextInt(20, 50));
                }

                if (i % 7 == 0) {
                    logger.warn("[{}] 性能监控 - 订单处理耗时: {}ms, 超过预期阈值100ms, 建议优化",
                              sessionId, ThreadLocalRandom.current().nextInt(100, 300));
                }
            }

            if ("all".equals(level) || "error".equals(level)) {
                if (i % 8 == 0) {
                    logger.error("[{}] 支付失败 - 订单: {}, 金额: {}.{}元, 错误: 银行卡余额不足",
                               sessionId, orderId,
                               ThreadLocalRandom.current().nextInt(100, 999),
                               ThreadLocalRandom.current().nextInt(10, 99));
                }

                if (i % 10 == 0) {
                    logger.error("[{}] 系统异常 - 第三方接口调用失败: 物流查询API超时, 订单: {}",
                               sessionId, orderId);
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
        logger.info("[{}] 电商业务日志生成完成 - 耗时: {}ms, 生成数量: {}组",
                   sessionId, endTime - startTime, count);

        return String.format("电商业务日志生成完成 - 会话ID: %s, 数量: %d组", sessionId, count);
    }

    /**
     * 生成金融业务日志
     */
    public String generateFinancialBusinessLogs(int count) {
        String sessionId = UUID.randomUUID().toString().substring(0, 8);

        logger.info("[{}] 开始生成金融业务日志 - 数量: {}", sessionId, count);

        for (int i = 1; i <= count; i++) {
            String transactionId = "TXN" + String.format("%012d", ThreadLocalRandom.current().nextLong(100000000000L, 999999999999L));
            String accountNo = "ACC" + ThreadLocalRandom.current().nextInt(100000, 999999);

            // 交易记录
            logger.info("[{}] 金融交易 - 交易ID: {}, 账户: {}, 类型: {}, 金额: {}.{}元, 余额: {}.{}元",
                      sessionId, transactionId, accountNo,
                      getRandomTransactionType(),
                      ThreadLocalRandom.current().nextInt(1, 10000),
                      ThreadLocalRandom.current().nextInt(10, 99),
                      ThreadLocalRandom.current().nextInt(1000, 50000),
                      ThreadLocalRandom.current().nextInt(10, 99));

            // 风控检查
            logger.info("[{}] 风控检查 - 交易: {}, 风险等级: {}, 反欺诈评分: {}, 状态: {}",
                      sessionId, transactionId,
                      getRandomRiskLevel(),
                      ThreadLocalRandom.current().nextInt(60, 100),
                      ThreadLocalRandom.current().nextBoolean() ? "通过" : "人工审核");

            // 合规审计
            logger.info("[{}] 合规审计 - 交易: {}, 监管要求: {}, 合规状态: {}, 审计时间: {}",
                      sessionId, transactionId,
                      "反洗钱检查",
                      ThreadLocalRandom.current().nextBoolean() ? "合规" : "待审核",
                      LocalDateTime.now());

            if (i % 6 == 0) {
                logger.warn("[{}] 异常监控 - 账户: {}, 单日交易次数: {}, 超过限制10次",
                          sessionId, accountNo, ThreadLocalRandom.current().nextInt(10, 20));
            }

            if (i % 12 == 0) {
                logger.error("[{}] 交易异常 - 交易: {}, 错误: 账户被冻结, 需要人工处理",
                           sessionId, transactionId);
            }
        }

        logger.info("[{}] 金融业务日志生成完成 - 数量: {}组", sessionId, count);
        return String.format("金融业务日志生成完成 - 会话ID: %s, 数量: %d组", sessionId, count);
    }

    /**
     * 异步生成高并发业务日志
     */
    public CompletableFuture<String> generateHighConcurrencyLogs(int threadCount, int logsPerThread) {
        String sessionId = UUID.randomUUID().toString().substring(0, 8);

        logger.info("[{}] 开始高并发日志生成 - 线程数: {}, 每线程日志数: {}", sessionId, threadCount, logsPerThread);

        CompletableFuture<String>[] futures = new CompletableFuture[threadCount];

        for (int t = 0; t < threadCount; t++) {
            final int threadId = t;
            futures[t] = CompletableFuture.supplyAsync(() -> {
                String threadSessionId = sessionId + "-T" + threadId;

                for (int i = 1; i <= logsPerThread; i++) {
                    logger.info("[{}] 高并发测试 - 线程{}, 第{}条日志, API调用: /api/user/{}/order, 响应时间: {}ms",
                              threadSessionId, threadId, i,
                              ThreadLocalRandom.current().nextInt(1000, 9999),
                              ThreadLocalRandom.current().nextInt(10, 200));

                    if (i % 10 == 0) {
                        logger.debug("[{}] 线程{}进度 - 已生成{}/{}条日志", threadSessionId, threadId, i, logsPerThread);
                    }

                    if (i % 20 == 0) {
                        logger.warn("[{}] 线程{}性能监控 - 当前TPS: {}, CPU使用率: {}%",
                                  threadSessionId, threadId,
                                  ThreadLocalRandom.current().nextInt(500, 1000),
                                  ThreadLocalRandom.current().nextInt(30, 80));
                    }

                    try {
                        Thread.sleep(ThreadLocalRandom.current().nextInt(1, 5));
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }

                return threadSessionId + " 完成";
            }, executorService);
        }

        return CompletableFuture.allOf(futures)
                .thenApply(v -> {
                    logger.info("[{}] 高并发日志生成完成 - 总线程数: {}, 总日志数: 约{}条",
                               sessionId, threadCount, threadCount * logsPerThread * 3);
                    return String.format("高并发日志生成完成 - 会话ID: %s, 线程数: %d", sessionId, threadCount);
                });
    }

    /**
     * 生成系统监控日志
     */
    public String generateSystemMonitoringLogs(int count) {
        String sessionId = UUID.randomUUID().toString().substring(0, 8);

        logger.info("[{}] 开始生成系统监控日志 - 数量: {}", sessionId, count);

        for (int i = 1; i <= count; i++) {
            // JVM监控
            logger.info("[{}] JVM监控 - 堆内存使用: {}MB/{}MB, GC次数: {}, GC耗时: {}ms",
                      sessionId,
                      ThreadLocalRandom.current().nextInt(200, 800),
                      ThreadLocalRandom.current().nextInt(800, 1024),
                      ThreadLocalRandom.current().nextInt(10, 50),
                      ThreadLocalRandom.current().nextInt(10, 100));

            // 数据库监控
            logger.info("[{}] 数据库监控 - 连接池: 活跃{}/最大{}, 慢查询数: {}, 平均响应时间: {}ms",
                      sessionId,
                      ThreadLocalRandom.current().nextInt(10, 80),
                      ThreadLocalRandom.current().nextInt(80, 100),
                      ThreadLocalRandom.current().nextInt(0, 10),
                      ThreadLocalRandom.current().nextInt(20, 200));

            // 接口监控
            logger.info("[{}] 接口监控 - 端点: /api/orders, QPS: {}, 平均延迟: {}ms, 错误率: {}%",
                      sessionId,
                      ThreadLocalRandom.current().nextInt(100, 500),
                      ThreadLocalRandom.current().nextInt(50, 300),
                      ThreadLocalRandom.current().nextDouble(0.1, 5.0));

            if (i % 5 == 0) {
                logger.warn("[{}] 系统警告 - CPU使用率: {}%, 超过阈值80%",
                          sessionId, ThreadLocalRandom.current().nextInt(80, 95));
            }

            if (i % 10 == 0) {
                logger.error("[{}] 系统错误 - 磁盘空间不足: 剩余{}GB, 少于阈值10GB",
                           sessionId, ThreadLocalRandom.current().nextInt(1, 10));
            }
        }

        logger.info("[{}] 系统监控日志生成完成 - 数量: {}组", sessionId, count);
        return String.format("系统监控日志生成完成 - 会话ID: %s, 数量: %d组", sessionId, count);
    }

    // 辅助方法
    private String getRandomLogisticsCompany() {
        String[] companies = {"顺丰快递", "圆通速递", "中通快递", "韵达速递", "申通快递", "百世快递"};
        return companies[ThreadLocalRandom.current().nextInt(companies.length)];
    }

    private String getRandomTransactionType() {
        String[] types = {"转账", "支付", "提现", "充值", "退款"};
        return types[ThreadLocalRandom.current().nextInt(types.length)];
    }

    private String getRandomRiskLevel() {
        String[] levels = {"低风险", "中风险", "高风险"};
        return levels[ThreadLocalRandom.current().nextInt(levels.length)];
    }

    public void shutdown() {
        executorService.shutdown();
    }
}