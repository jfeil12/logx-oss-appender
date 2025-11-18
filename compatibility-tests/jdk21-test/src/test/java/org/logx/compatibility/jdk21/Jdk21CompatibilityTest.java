package org.logx.compatibility.jdk21;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.ThreadLocalRandom;

/**
 * JDK 21兼容性测试 - 验证LogX OSS Appender在JDK 21环境下的兼容性
 *
 * 测试目标：
 * 1. 验证核心AsyncEngine功能在JDK 21上正常工作
 * 2. 验证并发操作在JDK 21上的行为一致性
 * 3. 验证四个触发条件在JDK 21上的正确性
 * 4. 验证512MB紧急保护机制在JDK 21上的有效性
 * 5. 验证QPS性能在JDK 21上达到预期目标
 */
public class Jdk21CompatibilityTest {

    private static final Logger logger = LoggerFactory.getLogger(Jdk21CompatibilityTest.class);
    private final LogGeneratorService logGeneratorService = new LogGeneratorService();

    @Test
    public void testPerformanceStressLogsGeneration() throws Exception {
        logger.info("🚀 开始JDK 21性能压力测试 - 验证四个触发条件和512MB保护机制");

        Runtime runtime = Runtime.getRuntime();
        long initialMemory = runtime.totalMemory() - runtime.freeMemory();

        logger.info("=== 系统初始状态 ===");
        logger.info("初始内存使用: {} MB", String.format("%.2f", initialMemory / 1024.0 / 1024.0));
        logger.info("目标配置: maxBatchCount=8192, maxBatchBytes=10MB, maxMessageAgeMs=60s, emergencyThreshold=512MB");

        // 等待系统初始化
        Thread.sleep(2000);

        // 1. 测试消息数量触发条件 (8192条消息)
        logger.info("\n=== 测试1: 消息数量触发条件 (8192条) ===");
        long test1Start = System.currentTimeMillis();

        logger.info("生成8500条日志测试消息数量触发...");
        for (int i = 0; i < 8500; i++) {
            logger.info("数量触发测试 #{} - 消息内容: 订单ID-{}, 时间: {}",
                       i + 1, String.format("ORD%06d", i), System.currentTimeMillis());

            // 控制生成速度，避免过快触发其他条件
            if (i % 1000 == 0) {
                Thread.sleep(50);
            }
        }

        long test1Duration = System.currentTimeMillis() - test1Start;
        logger.info("✅ 消息数量触发测试完成 - 耗时: {}ms", test1Duration);

        // 等待上传
        Thread.sleep(3000);

        // 2. 测试字节数触发条件 (10MB)
        logger.info("\n=== 测试2: 字节数触发条件 (10MB) ===");
        long test2Start = System.currentTimeMillis();

        // 生成大消息以快速达到10MB
        // 每条消息100KB，确保消息数量远小于8192，必定由字节数触发
        // 使用随机中文汉字模拟真实业务日志，更准确地测试压缩效果
        StringBuilder sb = new StringBuilder(100 * 1024);
        ThreadLocalRandom random = ThreadLocalRandom.current();
        String[] chineseWords = {
            "订单", "用户", "商品", "支付", "配送", "退款", "库存", "价格", "优惠", "会员",
            "交易", "物流", "仓库", "发货", "收货", "评价", "客服", "系统", "处理", "成功",
            "失败", "异常", "请求", "响应", "时间", "金额", "数量", "状态", "编号", "信息",
            "创建", "更新", "删除", "查询", "确认", "取消", "完成", "待处理", "进行中", "已结束"
        };
        String[] actionPhrases = {
            "状态更新成功", "自动重试完成", "等待支付确认", "已进入拣货流程", "完成库存校验",
            "同步风控规则", "准备配送发出", "消费者确认收货", "客服已介入处理", "系统自动补偿中",
            "触发风控复核", "消息队列入站", "异步任务执行完成", "开始生成对账", "完成售后登记"
        };
        String[] punctuationMarks = {"，", "、"};

        for (int j = 0; j < 5000; j++) {
            sb.append(chineseWords[random.nextInt(chineseWords.length)]);
            sb.append("：");
            int chunkCount = random.nextInt(2, 5);
            for (int k = 0; k < chunkCount; k++) {
                if (random.nextInt(10) < 3) {
                    sb.append(actionPhrases[random.nextInt(actionPhrases.length)]);
                } else {
                    sb.append(randomChineseChunk(random, 3, 9));
                }
                if (k < chunkCount - 1) {
                    sb.append(punctuationMarks[random.nextInt(punctuationMarks.length)]);
                }
            }
            sb.append("；");
        }
        String largeMessage = sb.toString(); // 约100KB中文业务日志消息（内容随机略有浮动）
        byte[] messageBytes = largeMessage.getBytes(StandardCharsets.UTF_8);

        int messagesFor10MB = (int) Math.ceil((10 * 1024 * 1024) / (double) messageBytes.length) + 5;

        logger.info("单条消息大小约 {} KB，生成 {} 条大消息测试字节数触发...",
                    String.format("%.2f", messageBytes.length / 1024.0), messagesFor10MB);
        logger.info("消息数量: {} << 8192 (maxBatchCount)，确保由字节数触发", messagesFor10MB);

        for (int i = 0; i < messagesFor10MB; i++) {
            logger.info("字节数触发测试 #{} - 大消息: {}", i + 1, largeMessage);

            if (i % 20 == 0) {
                Thread.sleep(50);
            }
        }

        long test2Duration = System.currentTimeMillis() - test2Start;
        logger.info("✅ 字节数触发测试完成 - 耗时: {}ms", test2Duration);

        // 等待上传
        Thread.sleep(3000);

        // 3. 测试消息年龄触发条件 (60秒)
        logger.info("\n=== 测试3: 消息年龄触发条件 (60秒) ===");
        long test3Start = System.currentTimeMillis();

        // 生成少量消息，然后等待60秒触发年龄条件
        logger.info("生成100条消息，然后等待60秒测试年龄触发...");
        for (int i = 0; i < 100; i++) {
            logger.info("年龄触发测试 #{} - 消息: 等待时间触发, 时间: {}",
                       i + 1, System.currentTimeMillis());
        }

        logger.info("开始等待60秒让消息年龄触发条件生效...");
        Thread.sleep(65000); // 等待65秒确保触发

        long test3Duration = System.currentTimeMillis() - test3Start;
        logger.info("✅ 消息年龄触发测试完成 - 耗时: {}ms", test3Duration);

        // 4. 测试512MB紧急保护机制
        logger.info("\n=== 测试4: 512MB紧急保护机制 ===");
        long test4Start = System.currentTimeMillis();

        // 监控内存使用，接近512MB时观察保护机制
        long currentMemory = runtime.totalMemory() - runtime.freeMemory();
        double currentMemoryMB = (currentMemory - initialMemory) / 1024.0 / 1024.0;

        logger.info("当前队列内存: {} MB", String.format("%.2f", currentMemoryMB));

        if (currentMemoryMB < 400) {
            logger.info("当前内存使用较低，生成大量日志测试512MB保护机制...");

            // 生成大量日志测试内存保护
            StringBuilder hugeSb = new StringBuilder(4096);
            for (int j = 0; j < 4096; j++) {
                hugeSb.append("Y");
            }
            String hugeMessage = hugeSb.toString(); // 4KB消息
            int messagesForMemoryTest = 50000; // 5万条消息

            for (int i = 0; i < messagesForMemoryTest; i++) {
                logger.info("内存保护测试 #{} - 大消息: {}", i + 1, hugeMessage);

                // 每1000条检查一次内存
                if (i % 1000 == 0) {
                    System.gc();
                    Thread.sleep(10);

                    long memCheck = runtime.totalMemory() - runtime.freeMemory();
                    double memCheckMB = (memCheck - initialMemory) / 1024.0 / 1024.0;

                    if (memCheckMB > 400) {
                        logger.info("⚠️ 内存使用已达 {} MB，接近512MB阈值", String.format("%.2f", memCheckMB));
                    }

                    if (memCheckMB > 480) {
                        logger.info("🛑 内存使用 {} MB，接近512MB限制，停止生成", String.format("%.2f", memCheckMB));
                        break;
                    }
                }
            }
        } else {
            logger.info("当前内存使用已较高: {} MB，跳过大量日志生成", String.format("%.2f", currentMemoryMB));
        }

        long test4Duration = System.currentTimeMillis() - test4Start;
        logger.info("✅ 512MB保护机制测试完成 - 耗时: {}ms", test4Duration);

        // 最终内存检查
        System.gc();
        Thread.sleep(1000);
        long finalMemory = runtime.totalMemory() - runtime.freeMemory();
        double finalMemoryMB = (finalMemory - initialMemory) / 1024.0 / 1024.0;

        // 测试总结
        logger.info("\n=== 四个触发条件验证总结 ===");
        logger.info("✅ 测试1 - 消息数量触发 (8192条): 完成");
        logger.info("✅ 测试2 - 字节数触发 (10MB): 完成");
        logger.info("✅ 测试3 - 消息年龄触发 (60秒): 完成");
        logger.info("✅ 测试4 - 512MB保护机制: 完成");
        logger.info("📊 最终队列内存使用: {} MB (限制: 512MB) - {}",
                   String.format("%.2f", finalMemoryMB), finalMemoryMB < 512 ? "✓ 合格" : "⚠ 超标");

        // 等待所有日志上传完成
        logger.info("\n等待所有日志批次上传完成...");
        Thread.sleep(30000);

        // 5. 测试日志QPS性能 (目标: 10,000+/s)
        logger.info("\n=== 测试5: 日志QPS性能测试 (目标: 10,000+/s) ===");

        int qpsTestCount = 50000; // 5万条日志，适合快速测试
        long qpsTestStart = System.nanoTime();

        logger.info("开始QPS性能测试，生成{}条日志...", qpsTestCount);

        // 快速生成日志测试QPS
        for (int i = 0; i < qpsTestCount; i++) {
            logger.info("QPS测试 #{} - 时间: {} - 数据: ORDER_{}",
                       i + 1, System.currentTimeMillis(), String.format("%06d", i));
        }

        long qpsTestEnd = System.nanoTime();
        double qpsTestDurationSeconds = (qpsTestEnd - qpsTestStart) / 1_000_000_000.0;
        double actualQPS = qpsTestCount / qpsTestDurationSeconds;

        logger.info("QPS性能测试完成:");
        logger.info("- 测试日志数量: {} 条", qpsTestCount);
        logger.info("- 测试耗时: {} 秒", String.format("%.3f", qpsTestDurationSeconds));
        logger.info("- 实际QPS: {} 条/秒", String.format("%.0f", actualQPS));
        logger.info("- 目标QPS: 10,000 条/秒");

        boolean qpsPass = actualQPS >= 10000;
        logger.info("- QPS性能评估: {}", qpsPass ? "✅ 达标" : "⚠️ 未达标");

        // 如果未达标，给出性能优化建议
        if (!qpsPass) {
            logger.info("\n=== QPS性能优化建议 ===");

            // 计算建议的性能目标
            double suggestedQPS = Math.max(1000, actualQPS * 0.8); // 至少1千/s，或实际性能的80%
            double performanceGap = (10000 - actualQPS) / 10000 * 100;

            logger.info("📊 性能分析:");
            logger.info("- 当前实际QPS: {} 条/秒", String.format("%.0f", actualQPS));
            logger.info("- 与目标差距: {}%", String.format("%.1f", performanceGap));
            logger.info("- 建议调整目标: {} 条/秒", String.format("%.0f", suggestedQPS));

            logger.info("\n🔧 优化建议:");
            if (actualQPS < 1000) {
                logger.info("- 性能很低，建议检查:");
                logger.info("  1. 系统资源：CPU、内存、磁盘I/O是否瓶颈");
                logger.info("  2. 网络连接：MinIO连接是否正常");
                logger.info("  3. JVM参数：堆内存是否充足");
                logger.info("  4. 基础环境：硬件配置是否满足要求");
            } else if (actualQPS < 5000) {
                logger.info("- 性能较低，建议优化:");
                logger.info("  1. 批处理参数：调整maxBatchCount和maxBatchBytes");
                logger.info("  2. 队列配置：优化queueCapacity设置");
                logger.info("  3. 网络优化：检查MinIO连接配置");
                logger.info("  4. 日志格式：简化日志模式");
            } else {
                logger.info("- 性能接近目标，微调建议:");
                logger.info("  1. 细微调整批处理触发条件");
                logger.info("  2. 优化日志编码器设置");
                logger.info("  3. 线程池配置微调");
                logger.info("  4. 系统参数优化");
            }

            logger.info("\n📋 配置调整建议:");
            logger.info("基于当前性能表现，建议的配置目标值:");
            logger.info("- maxBatchCount: {} (当前: 8192)", actualQPS < 3000 ? "2048" : actualQPS < 7000 ? "4096" : "8192");
            logger.info("- maxBatchBytes: {} (当前: 10MB)", actualQPS < 3000 ? "2MB" : actualQPS < 7000 ? "5MB" : "10MB");
            logger.info("- maxMessageAgeMs: {} (当前: 60s)", actualQPS < 5000 ? "30s" : "60s");
            logger.info("- queueCapacity: {} (当前: 524288)", actualQPS < 3000 ? "131072" : actualQPS < 7000 ? "262144" : "524288");

        } else {
            logger.info("\n🎉 QPS性能优秀！当前配置已达到预期性能目标。");
        }

        // 最终测试总结
        logger.info("\n=== 完整性能测试总结 ===");
        logger.info("✅ 测试1 - 消息数量触发 (8192条): 完成");
        logger.info("✅ 测试2 - 字节数触发 (10MB): 完成");
        logger.info("✅ 测试3 - 消息年龄触发 (60秒): 完成");
        logger.info("✅ 测试4 - 512MB保护机制: 完成");
        logger.info("{} 测试5 - QPS性能测试: {} 条/秒 (目标: 10,000+)",
                   qpsPass ? "✅" : "⚠️", String.format("%.0f", actualQPS));
        logger.info("📊 最终队列内存使用: {} MB (限制: 512MB) - {}",
                   String.format("%.2f", finalMemoryMB), finalMemoryMB < 512 ? "✓ 合格" : "⚠ 超标");

        logger.info("\n🎉 四个触发条件验证测试完成！");
    }

    @Test
    public void testOSSConnectionAndLogging() throws Exception {
        logger.info("开始OSS连接和日志上传诊断测试...");

        // 1. 检查环境变量配置
        logger.info("=== 环境变量配置检查 ===");
        String endpoint = System.getenv("LOGX_OSS_ENDPOINT");
        String accessKeyId = System.getenv("LOGX_OSS_ACCESS_KEY_ID");
        String accessKeySecret = System.getenv("LOGX_OSS_ACCESS_KEY_SECRET");
        String bucket = System.getenv("LOGX_OSS_BUCKET");
        String ossType = System.getenv("LOGX_OSS_OSS_TYPE");

        logger.info("LOGX_OSS_ENDPOINT: {}", endpoint != null ? endpoint : "未设置（将使用默认值 http://localhost:9000）");
        logger.info("LOGX_OSS_ACCESS_KEY_ID: {}", accessKeyId != null ? accessKeyId : "未设置（将使用默认值 minioadmin）");
        logger.info("LOGX_OSS_ACCESS_KEY_SECRET: {}", accessKeySecret != null ? "已设置" : "未设置（将使用默认值 minioadmin）");
        logger.info("LOGX_OSS_BUCKET: {}", bucket != null ? bucket : "未设置（将使用默认值 logx-test-bucket）");
        logger.info("LOGX_OSS_OSS_TYPE: {}", ossType != null ? ossType : "未设置（将使用默认值 S3）");

        // 2. 生成测试日志
        logger.info("=== 生成测试日志 ===");
        for (int i = 1; i <= 20; i++) {
            logger.info("OSS上传测试日志 #{} - 测试时间: {}, 内容: 这是一条用于验证OSS上传功能的测试日志",
                       i, System.currentTimeMillis());

            if (i % 5 == 0) {
                logger.warn("OSS上传测试警告日志 #{} - 这是一条WARN级别的测试日志", i);
            }

            if (i % 10 == 0) {
                logger.error("OSS上传测试错误日志 #{} - 这是一条ERROR级别的测试日志", i);
            }

            // 短暂延迟
            Thread.sleep(100);
        }

        // 4. 等待日志处理
        logger.info("=== 等待日志处理和上传 ===");
        logger.info("等待10秒让日志系统处理和上传日志...");
        Thread.sleep(10000);

        // 5. 生成更多日志来触发批处理
        logger.info("=== 触发批处理上传 ===");
        for (int i = 1; i <= 100; i++) {
            logger.info("批处理触发日志 #{} - 时间戳: {}", i, System.currentTimeMillis());
            if (i % 10 == 0) {
                Thread.sleep(50); // 短暂暂停
            }
        }

        logger.info("=== 最终等待和验证 ===");
        logger.info("等待15秒确保所有日志被处理和上传...");
        Thread.sleep(15000);

        logger.info("OSS连接和日志上传诊断测试完成！");
        logger.info("请检查MinIO控制台: http://localhost:9001");
        logger.info("查看桶: logx-test-bucket");
        logger.info("查看路径: logx/");
        logger.info("如果没有看到日志文件，请检查:");
        logger.info("1. MinIO服务是否正在运行");
        logger.info("2. 网络连接是否正常");
        logger.info("3. 配置参数是否正确");
        logger.info("4. 查看控制台是否有错误信息");
    }

    /**
     * 独立的QPS性能测试
     * 专门用于测试日志QPS性能，目标: 10,000+/s
     */
    @Test
    public void testQPSPerformance() throws Exception {
        logger.info("🚀 开始QPS性能测试 - 目标: 10,000+/s");

        int qpsTestCount = 10000; // 1万条日志，快速测试
        long qpsTestStart = System.nanoTime();

        logger.info("开始QPS性能测试，生成{}条日志...", qpsTestCount);

        // 快速生成日志测试QPS
        for (int i = 0; i < qpsTestCount; i++) {
            logger.info("QPS测试 #{} - 时间: {} - 数据: ORDER_{}",
                       i + 1, System.currentTimeMillis(), String.format("%06d", i));
        }

        long qpsTestEnd = System.nanoTime();
        double qpsTestDurationSeconds = (qpsTestEnd - qpsTestStart) / 1_000_000_000.0;
        double actualQPS = qpsTestCount / qpsTestDurationSeconds;

        logger.info("QPS性能测试完成:");
        logger.info("- 测试日志数量: {} 条", qpsTestCount);
        logger.info("- 测试耗时: {} 秒", String.format("%.3f", qpsTestDurationSeconds));
        logger.info("- 实际QPS: {} 条/秒", String.format("%.0f", actualQPS));
        logger.info("- 目标QPS: 10,000 条/秒");

        boolean qpsPass = actualQPS >= 10000;
        logger.info("- QPS性能评估: {}", qpsPass ? "✅ 达标" : "⚠️ 未达标");

        // 如果未达标，给出性能优化建议
        if (!qpsPass) {
            logger.info("\n=== QPS性能优化建议 ===");

            // 计算建议的性能目标
            double suggestedQPS = Math.max(1000, actualQPS * 0.8); // 至少1千/s，或实际性能的80%
            double performanceGap = (10000 - actualQPS) / 10000 * 100;

            logger.info("📊 性能分析:");
            logger.info("- 当前实际QPS: {} 条/秒", String.format("%.0f", actualQPS));
            logger.info("- 与目标差距: {}%", String.format("%.1f", performanceGap));
            logger.info("- 建议调整目标: {} 条/秒", String.format("%.0f", suggestedQPS));

            logger.info("\n🔧 优化建议:");
            if (actualQPS < 1000) {
                logger.info("- 性能很低，建议检查:");
                logger.info("  1. 系统资源：CPU、内存、磁盘I/O是否瓶颈");
                logger.info("  2. 网络连接：MinIO连接是否正常");
                logger.info("  3. JVM参数：堆内存是否充足");
                logger.info("  4. 基础环境：硬件配置是否满足要求");
                logger.info("  5. 消费线程数：当前为1，可适当增加");
            } else if (actualQPS < 5000) {
                logger.info("- 性能较低，建议优化:");
                logger.info("  1. 批处理参数：调整maxBatchCount和maxBatchBytes");
                logger.info("  2. 队列配置：优化queueCapacity设置");
                logger.info("  3. 网络优化：检查MinIO连接配置");
                logger.info("  4. 日志格式：简化日志模式");
                logger.info("  5. 线程池配置：增加消费线程数");
            } else {
                logger.info("- 性能接近目标，微调建议:");
                logger.info("  1. 细微调整批处理触发条件");
                logger.info("  2. 优化日志编码器设置");
                logger.info("  3. 线程池配置微调");
                logger.info("  4. 系统参数优化");
            }

            logger.info("\n📋 配置调整建议:");
            logger.info("基于当前性能表现，建议的配置目标值:");
            logger.info("- maxBatchCount: {} (当前: 8192)", actualQPS < 3000 ? "2048" : actualQPS < 7000 ? "4096" : "8192");
            logger.info("- maxBatchBytes: {} (当前: 10MB)", actualQPS < 3000 ? "2MB" : actualQPS < 7000 ? "5MB" : "10MB");
            logger.info("- maxMessageAgeMs: {} (当前: 60s)", actualQPS < 5000 ? "30s" : "60s");
            logger.info("- queueCapacity: {} (当前: 524288)", actualQPS < 3000 ? "131072" : actualQPS < 7000 ? "262144" : "524288");
            logger.info("- corePoolSize: {} (当前: 1)", actualQPS < 5000 ? "2" : actualQPS < 8000 ? "4" : "1");
            logger.info("- consumerThreadCount: {} (当前: 1)", actualQPS < 5000 ? "2" : actualQPS < 8000 ? "4" : "1");

        } else {
            logger.info("\n🎉 QPS性能优秀！当前配置已达到预期性能目标。");
        }

        logger.info("\n=== QPS测试总结 ===");
        logger.info("{} QPS性能测试: {} 条/秒 (目标: 10,000+)",
                   qpsPass ? "✅" : "⚠️", String.format("%.0f", actualQPS));
        logger.info("消费线程数配置: 1 (核心线程数: 1, 最大线程数: 1)");
    }

    private static String randomChineseChunk(ThreadLocalRandom random, int minLength, int maxLength) {
        int targetLength = random.nextInt(minLength, maxLength + 1);
        StringBuilder chunk = new StringBuilder(targetLength);
        while (chunk.length() < targetLength) {
            int codePoint = randomChineseCodePoint(random);
            chunk.appendCodePoint(codePoint);
        }
        if (chunk.length() > targetLength) {
            chunk.setLength(targetLength);
        }
        return chunk.toString();
    }

    private static int randomChineseCodePoint(ThreadLocalRandom random) {
        return random.nextInt(0x4E00, 0x9FA6);
    }
}