package org.logx.compatibility.jsp.servlet;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import org.apache.log4j.Logger;
import org.junit.Test;

/**
 * Business log generation scenarios for the classic JSP/Servlet stack using log4j 1.x.
 *
 * <p>The scenarios mirror the Spring Boot compatibility suite: they validate batch flush triggers,
 * OSS connectivity, and QPS throughput while keeping the runtime lightweight enough for CI.</p>
 */
public class BusinessLogGenerationTest {

    private static final Logger LOGGER = Logger.getLogger(BusinessLogGenerationTest.class);
    private final BusinessLogScenario scenario = new BusinessLogScenario(new Log4jFacade(LOGGER));

    @Test
    public void testPerformanceStressLogsGeneration() throws Exception {
        scenario.runPerformanceStressSuite();
    }

    @Test
    public void testOssConnectionAndLogging() throws Exception {
        scenario.runOssConnectivityDiagnostics();
    }

    @Test
    public void testQpsPerformance() throws Exception {
        scenario.runQpsPerformanceProbe(6000, 5000);
    }

    private static final class Log4jFacade implements LoggerFacade {

        private final Logger delegate;

        private Log4jFacade(Logger delegate) {
            this.delegate = delegate;
        }

        @Override
        public void info(String message) {
            delegate.info(message);
        }

        @Override
        public void warn(String message) {
            delegate.warn(message);
        }

        @Override
        public void error(String message) {
            delegate.error(message);
        }
    }

    private interface LoggerFacade {
        void info(String message);

        void warn(String message);

        void error(String message);
    }

    private static final class BusinessLogScenario {

        private static final int BATCH_TARGET_BYTES = 5 * 1024 * 1024;
        private static final String ALPHANUMERIC = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

        private final LoggerFacade logger;
        private final ThreadLocalRandom random = ThreadLocalRandom.current();

        private BusinessLogScenario(LoggerFacade logger) {
            this.logger = logger;
        }

        private void runPerformanceStressSuite() throws InterruptedException {
            logger.info("=== Performance suite: verifying log aggregation triggers ===");

            long initialMemory = usedMemoryBytes();
            logger.info(String.format(Locale.US, "Initial heap usage: %.2f MB", initialMemory / 1024.0 / 1024.0));

            simulateMessageCountTrigger(2500);
            TimeUnit.MILLISECONDS.sleep(500);

            simulateBatchSizeTrigger(BATCH_TARGET_BYTES);
            TimeUnit.MILLISECONDS.sleep(500);

            simulateMessageAgeTrigger();
            TimeUnit.MILLISECONDS.sleep(500);

            simulateEmergencyMemoryGuard(initialMemory, 256 * 1024 * 1024);

            logger.info("Performance suite completed.");
        }

        private void runOssConnectivityDiagnostics() throws InterruptedException {
            logger.info("=== OSS connectivity diagnostics ===");

            logEnvVariable("LOGX_OSS_ENDPOINT", "http://localhost:9000");
            logEnvVariable("LOGX_OSS_ACCESS_KEY_ID", "minioadmin");
            logEnvVariable("LOGX_OSS_ACCESS_KEY_SECRET", "<not set>");
            logEnvVariable("LOGX_OSS_BUCKET", "logx-test-bucket");
            logEnvVariable("LOGX_OSS_OSS_TYPE", "S3");

            for (int i = 1; i <= 20; i++) {
                logger.info(String.format(Locale.US,
                        "OSS diagnostic log #%d - status=OK, timestamp=%d", i, System.currentTimeMillis()));
                if (i % 5 == 0) {
                    logger.warn(String.format(Locale.US,
                            "OSS diagnostic warning #%d - slow upload detected", i));
                }
                if (i % 10 == 0) {
                    logger.error(String.format(Locale.US,
                            "OSS diagnostic error #%d - 测试错误日志: temporary credential issue", i));
                }
                TimeUnit.MILLISECONDS.sleep(75);
            }

            logger.info("Waiting for uploader threads to flush pending batches...");
            TimeUnit.SECONDS.sleep(4);

            for (int i = 1; i <= 120; i++) {
                logger.info(String.format(Locale.US,
                        "Batch trigger log #%d - controller=legacyServlet, action=upload", i));
                if (i % 15 == 0) {
                    TimeUnit.MILLISECONDS.sleep(40);
                }
            }

            logger.info("OSS diagnostics completed. Verify assets in the target bucket if available.");
        }

        private void runQpsPerformanceProbe(int messageCount, int expectedQps) {
            logger.info(String.format(Locale.US,
                    "=== QPS probe: generating %d log entries (target >= %d msg/s) ===",
                    messageCount, expectedQps));

            long start = System.nanoTime();
            for (int i = 0; i < messageCount; i++) {
                logger.info(String.format(Locale.US,
                        "QPS probe #%d - servlet=OrderDispatcher, traceId=%s, elapsed=%d",
                        i + 1,
                        randomAlphanumeric(8),
                        random.nextInt(5, 80)));
            }
            double elapsedSeconds = (System.nanoTime() - start) / 1_000_000_000.0;
            double actualQps = messageCount / elapsedSeconds;

            logger.info(String.format(Locale.US,
                    "QPS probe finished: produced %d entries in %.3f s (%.0f msg/s)",
                    messageCount, elapsedSeconds, actualQps));

            if (actualQps < expectedQps) {
                logger.warn(String.format(Locale.US,
                        "Observed QPS %.0f is below target %d. Review batch settings and thread pools.",
                        actualQps, expectedQps));
            }
        }

        private void simulateMessageCountTrigger(int messageTarget) throws InterruptedException {
            logger.info(String.format(Locale.US,
                    "--- Trigger 1: message count threshold (%d entries) ---", messageTarget));
            long start = System.currentTimeMillis();

            for (int i = 0; i < messageTarget; i++) {
                logger.info(String.format(Locale.US,
                        "Count trigger #%d - session=%s, order=%05d, ts=%d",
                        i + 1,
                        randomAlphanumeric(6),
                        random.nextInt(10000),
                        System.currentTimeMillis()));
                if ((i + 1) % 500 == 0) {
                    TimeUnit.MILLISECONDS.sleep(10);
                }
            }

            logger.info(String.format(Locale.US,
                    "Message count trigger completed in %d ms",
                    System.currentTimeMillis() - start));
        }

        private void simulateBatchSizeTrigger(int targetBytes) throws InterruptedException {
            logger.info(String.format(Locale.US,
                    "--- Trigger 2: batch payload threshold (target %d bytes) ---", targetBytes));

            String payload = buildLargeMessage();
            int payloadBytes = payload.getBytes(StandardCharsets.UTF_8).length;
            int requiredMessages = (int) Math.ceil(targetBytes / (double) payloadBytes) + 2;

            logger.info(String.format(Locale.US,
                    "Payload size %.2f KB, emitting %d messages", payloadBytes / 1024.0, requiredMessages));

            for (int i = 0; i < requiredMessages; i++) {
                logger.info(String.format(Locale.US,
                        "Size trigger #%d - payload=%s", i + 1, payload));
                if (i % 5 == 0) {
                    TimeUnit.MILLISECONDS.sleep(25);
                }
            }

            logger.info("Batch payload trigger completed.");
        }

        private void simulateMessageAgeTrigger() throws InterruptedException {
            logger.info("--- Trigger 3: message age threshold (simulated 6 seconds) ---");

            for (int i = 0; i < 80; i++) {
                logger.info(String.format(Locale.US,
                        "Age trigger #%d - user=%s, status=%s, ts=%d",
                        i + 1,
                        randomAlphanumeric(6),
                        randomStatus(),
                        System.currentTimeMillis()));
                if ((i + 1) % 20 == 0) {
                    TimeUnit.MILLISECONDS.sleep(30);
                }
            }

            logger.info("Sleeping 6 seconds to allow time-based flush");
            TimeUnit.SECONDS.sleep(6);
            logger.info("Age trigger completed.");
        }

        private void simulateEmergencyMemoryGuard(long baseline, long thresholdBytes) throws InterruptedException {
            logger.info(String.format(Locale.US,
                    "--- Trigger 4: emergency memory guard (threshold %.1f MB) ---",
                    thresholdBytes / 1024.0 / 1024.0));

            String largePayload = randomAlphanumeric(4096);
            long peak = baseline;

            for (int i = 1; i <= 2000; i++) {
                logger.info(String.format(Locale.US,
                        "Memory guard #%d - payload=%s", i, largePayload));
                if (i % 200 == 0) {
                    System.gc();
                    TimeUnit.MILLISECONDS.sleep(15);
                    peak = Math.max(peak, usedMemoryBytes());
                    logger.info(String.format(Locale.US,
                            "Memory guard checkpoint %d - current=%.2f MB", i, peak / 1024.0 / 1024.0));
                    if (peak - baseline > thresholdBytes) {
                        logger.warn(String.format(Locale.US,
                                "Memory usage increased by %.2f MB, stopping early",
                                (peak - baseline) / 1024.0 / 1024.0));
                        break;
                    }
                }
            }

            logger.info("Emergency memory guard completed.");
        }

        private void logEnvVariable(String name, String fallback) {
            String value = System.getenv(name);
            logger.info(String.format(Locale.US, "%s=%s", name, value != null ? value : fallback));
        }

        private String buildLargeMessage() {
            String[] words = {
                    "order", "user", "inventory", "payment", "shipment", "discount", "warehouse",
                    "analytics", "promotion", "workflow", "audit", "retry", "invoice", "balance",
                    "fraud", "delivery", "tracking", "refund", "voucher", "loyalty"
            };
            StringBuilder builder = new StringBuilder(64 * 1024);
            for (int i = 0; i < 3200; i++) {
                builder.append(words[random.nextInt(words.length)]);
                builder.append('=');
                builder.append(randomAlphanumeric(6));
                builder.append(';');
            }
            return builder.toString();
        }

        private long usedMemoryBytes() {
            Runtime runtime = Runtime.getRuntime();
            return runtime.totalMemory() - runtime.freeMemory();
        }

        private String randomStatus() {
            String[] statuses = {"PENDING", "CONFIRMED", "SHIPPED", "DELIVERED", "FAILED"};
            return statuses[random.nextInt(statuses.length)];
        }

        private String randomAlphanumeric(int length) {
            char[] chars = new char[length];
            for (int i = 0; i < length; i++) {
                chars[i] = ALPHANUMERIC.charAt(random.nextInt(ALPHANUMERIC.length()));
            }
            return new String(chars);
        }
    }
}
