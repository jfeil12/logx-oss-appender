package org.logx.compatibility.multi.framework;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

/**
 * Baseline business log generation scenarios for the multi-framework compatibility module.
 *
 * <p>The scenarios mirror the Spring Boot compatibility suite and are executed against Logback,
 * log4j and log4j2 to ensure consistent batching behaviour and throughput expectations.</p>
 */
public class BusinessLogGenerationTest {

    @Test
    public void testLogbackScenario() throws Exception {
        ScenarioRunner scenario = new ScenarioRunner("logback", new Slf4jFacade(LoggerFactory.getLogger("multi-framework-logback")));
        scenario.runFullSuite();
    }

    @Test
    public void testLog4jScenario() throws Exception {
        ScenarioRunner scenario = new ScenarioRunner("log4j", new Log4jFacade(org.apache.log4j.Logger.getLogger("multi-framework-log4j")));
        scenario.runFullSuite();
    }

    @Test
    public void testLog4j2Scenario() throws Exception {
        ScenarioRunner scenario = new ScenarioRunner("log4j2", new Log4j2Facade(LogManager.getLogger("multi-framework-log4j2")));
        scenario.runFullSuite();
    }

    private interface LoggerFacade {
        void info(String message);

        void warn(String message);

        void error(String message);
    }

    private static final class Slf4jFacade implements LoggerFacade {

        private final org.slf4j.Logger delegate;

        private Slf4jFacade(org.slf4j.Logger delegate) {
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

    private static final class Log4jFacade implements LoggerFacade {

        private final org.apache.log4j.Logger delegate;

        private Log4jFacade(org.apache.log4j.Logger delegate) {
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

    private static final class Log4j2Facade implements LoggerFacade {

        private final Logger delegate;

        private Log4j2Facade(Logger delegate) {
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

    private static final class ScenarioRunner {

        private static final int BATCH_TARGET_BYTES = 5 * 1024 * 1024;
        private static final String ALPHANUMERIC = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

        private final String framework;
        private final LoggerFacade logger;
        private final ThreadLocalRandom random = ThreadLocalRandom.current();

        private ScenarioRunner(String framework, LoggerFacade logger) {
            this.framework = framework;
            this.logger = logger;
        }

        private void runFullSuite() throws InterruptedException {
            runPerformanceStressSuite();
            TimeUnit.MILLISECONDS.sleep(200);
            runOssConnectivityDiagnostics();
            TimeUnit.MILLISECONDS.sleep(200);
            runQpsPerformanceProbe(6000, 5000);
        }

        private void runPerformanceStressSuite() throws InterruptedException {
            logger.info(String.format(Locale.US, "=== [%s] Performance suite: verifying log aggregation triggers ===", framework));

            long initialMemory = usedMemoryBytes();
            logger.info(String.format(Locale.US, "[%s] Initial heap usage: %.2f MB", framework, initialMemory / 1024.0 / 1024.0));

            simulateMessageCountTrigger(2500);
            TimeUnit.MILLISECONDS.sleep(400);

            simulateBatchSizeTrigger(BATCH_TARGET_BYTES);
            TimeUnit.MILLISECONDS.sleep(400);

            simulateMessageAgeTrigger();
            TimeUnit.MILLISECONDS.sleep(400);

            simulateEmergencyMemoryGuard(initialMemory, 256 * 1024 * 1024);

            logger.info(String.format(Locale.US, "[%s] Performance suite completed.", framework));
        }

        private void runOssConnectivityDiagnostics() throws InterruptedException {
            logger.info(String.format(Locale.US, "=== [%s] OSS connectivity diagnostics ===", framework));

            logEnvVariable("LOGX_OSS_ENDPOINT", "http://localhost:9000");
            logEnvVariable("LOGX_OSS_ACCESS_KEY_ID", "minioadmin");
            logEnvVariable("LOGX_OSS_ACCESS_KEY_SECRET", "<not set>");
            logEnvVariable("LOGX_OSS_BUCKET", "logx-test-bucket");
            logEnvVariable("LOGX_OSS_OSS_TYPE", "S3");

            for (int i = 1; i <= 20; i++) {
                logger.info(String.format(Locale.US,
                        "[%s] OSS diagnostic log #%d - timestamp=%d", framework, i, System.currentTimeMillis()));
                if (i % 5 == 0) {
                    logger.warn(String.format(Locale.US,
                            "[%s] OSS diagnostic warning #%d - slow upload detected", framework, i));
                }
                if (i % 10 == 0) {
                    logger.error(String.format(Locale.US,
                            "[%s] OSS diagnostic error #%d - 测试错误日志: temporary credential issue", framework, i));
                }
                TimeUnit.MILLISECONDS.sleep(60);
            }

            logger.info(String.format(Locale.US, "[%s] Waiting for uploader threads to flush pending batches...", framework));
            TimeUnit.SECONDS.sleep(4);

            for (int i = 1; i <= 120; i++) {
                logger.info(String.format(Locale.US,
                        "[%s] Batch trigger log #%d - component=Dispatcher, action=upload", framework, i));
                if (i % 15 == 0) {
                    TimeUnit.MILLISECONDS.sleep(35);
                }
            }

            logger.info(String.format(Locale.US, "[%s] OSS diagnostics completed. Verify target bucket when available.", framework));
        }

        private void runQpsPerformanceProbe(int messageCount, int expectedQps) {
            logger.info(String.format(Locale.US,
                    "=== [%s] QPS probe: generating %d log entries (target >= %d msg/s) ===",
                    framework, messageCount, expectedQps));

            long start = System.nanoTime();
            for (int i = 0; i < messageCount; i++) {
                logger.info(String.format(Locale.US,
                        "[%s] QPS probe #%d - traceId=%s, latency=%d",
                        framework,
                        i + 1,
                        randomAlphanumeric(8),
                        random.nextInt(5, 70)));
            }
            double elapsedSeconds = (System.nanoTime() - start) / 1_000_000_000.0;
            double actualQps = messageCount / elapsedSeconds;

            logger.info(String.format(Locale.US,
                    "[%s] QPS probe finished: produced %d entries in %.3f s (%.0f msg/s)",
                    framework, messageCount, elapsedSeconds, actualQps));

            if (actualQps < expectedQps) {
                logger.warn(String.format(Locale.US,
                        "[%s] Observed QPS %.0f below target %d. Review batch settings and worker pools.",
                        framework, actualQps, expectedQps));
            }
        }

        private void simulateMessageCountTrigger(int messageTarget) throws InterruptedException {
            logger.info(String.format(Locale.US,
                    "--- [%s] Trigger 1: message count threshold (%d entries) ---", framework, messageTarget));
            long start = System.currentTimeMillis();

            for (int i = 0; i < messageTarget; i++) {
                logger.info(String.format(Locale.US,
                        "[%s] Count trigger #%d - requestId=%s, ts=%d",
                        framework,
                        i + 1,
                        randomAlphanumeric(6),
                        System.currentTimeMillis()));
                if ((i + 1) % 500 == 0) {
                    TimeUnit.MILLISECONDS.sleep(10);
                }
            }

            logger.info(String.format(Locale.US,
                    "[%s] Message count trigger completed in %d ms",
                    framework, System.currentTimeMillis() - start));
        }

        private void simulateBatchSizeTrigger(int targetBytes) throws InterruptedException {
            logger.info(String.format(Locale.US,
                    "--- [%s] Trigger 2: batch payload threshold (target %d bytes) ---", framework, targetBytes));

            String payload = buildLargeMessage();
            int payloadBytes = payload.getBytes(StandardCharsets.UTF_8).length;
            int requiredMessages = (int) Math.ceil(targetBytes / (double) payloadBytes) + 2;

            logger.info(String.format(Locale.US,
                    "[%s] Payload size %.2f KB, emitting %d messages",
                    framework, payloadBytes / 1024.0, requiredMessages));

            for (int i = 0; i < requiredMessages; i++) {
                logger.info(String.format(Locale.US,
                        "[%s] Size trigger #%d - payload=%s", framework, i + 1, payload));
                if (i % 5 == 0) {
                    TimeUnit.MILLISECONDS.sleep(20);
                }
            }

            logger.info(String.format(Locale.US, "[%s] Batch payload trigger completed.", framework));
        }

        private void simulateMessageAgeTrigger() throws InterruptedException {
            logger.info(String.format(Locale.US, "--- [%s] Trigger 3: message age threshold (simulated 6 seconds) ---", framework));

            for (int i = 0; i < 80; i++) {
                logger.info(String.format(Locale.US,
                        "[%s] Age trigger #%d - status=%s, ts=%d",
                        framework,
                        i + 1,
                        randomStatus(),
                        System.currentTimeMillis()));
                if ((i + 1) % 20 == 0) {
                    TimeUnit.MILLISECONDS.sleep(25);
                }
            }

            logger.info(String.format(Locale.US, "[%s] Sleeping 6 seconds to allow time-based flush", framework));
            TimeUnit.SECONDS.sleep(6);
            logger.info(String.format(Locale.US, "[%s] Age trigger completed.", framework));
        }

        private void simulateEmergencyMemoryGuard(long baseline, long thresholdBytes) throws InterruptedException {
            logger.info(String.format(Locale.US,
                    "--- [%s] Trigger 4: emergency memory guard (threshold %.1f MB) ---",
                    framework, thresholdBytes / 1024.0 / 1024.0));

            String largePayload = randomAlphanumeric(4096);
            long peak = baseline;

            for (int i = 1; i <= 2000; i++) {
                logger.info(String.format(Locale.US,
                        "[%s] Memory guard #%d - payload=%s", framework, i, largePayload));
                if (i % 200 == 0) {
                    System.gc();
                    TimeUnit.MILLISECONDS.sleep(15);
                    peak = Math.max(peak, usedMemoryBytes());
                    logger.info(String.format(Locale.US,
                            "[%s] Memory guard checkpoint %d - current=%.2f MB",
                            framework, i, peak / 1024.0 / 1024.0));
                    if (peak - baseline > thresholdBytes) {
                        logger.warn(String.format(Locale.US,
                                "[%s] Memory usage increased by %.2f MB, stopping early",
                                framework, (peak - baseline) / 1024.0 / 1024.0));
                        break;
                    }
                }
            }

            logger.info(String.format(Locale.US, "[%s] Emergency memory guard completed.", framework));
        }

        private void logEnvVariable(String name, String fallback) {
            String value = System.getenv(name);
            logger.info(String.format(Locale.US, "[%s] %s=%s", framework, name, value != null ? value : fallback));
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
            String[] statuses = {"PENDING", "PROCESSING", "SUCCEEDED", "FAILED", "RETRY"};
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
