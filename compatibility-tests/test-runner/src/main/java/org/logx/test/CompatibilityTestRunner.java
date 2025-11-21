package org.logx.test;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * 跨平台的兼容性测试运行器，支持Windows和Linux
 *
 * 功能：
 * 1. 运行所有compatibility-tests模块的测试
 * 2. 实时监控logs/application-error.log
 * 3. 发现错误日志立即停止测试并报告
 */
public class CompatibilityTestRunner {

    private static final String[] TEST_MODULES = {
        "config-consistency-test",
        "jsp-servlet-test",
        "multi-framework-test",
        "spring-boot-test",
        "spring-mvc-test",
        "all-in-one-test/s3-all-in-one-logback-test",
        "all-in-one-test/s3-all-in-one-log4j-test",
        "all-in-one-test/s3-all-in-one-log4j2-test",
        "jdk21-test"
    };

    private static final String ANSI_RESET = "\u001B[0m";
    private static final String ANSI_RED = "\u001B[31m";
    private static final String ANSI_GREEN = "\u001B[32m";
    private static final String ANSI_YELLOW = "\u001B[33m";
    private static final String ANSI_BLUE = "\u001B[34m";

    private final Path rootPath;
    private final ExecutorService monitorExecutor;
    private volatile boolean shouldStop = false;
    private volatile String errorModule = null;

    public CompatibilityTestRunner(Path rootPath) {
        this.rootPath = rootPath;
        this.monitorExecutor = Executors.newSingleThreadExecutor();
    }

    public static void main(String[] args) {
        Path rootPath = Paths.get(System.getProperty("user.dir"));

        if (!Files.exists(rootPath.resolve("compatibility-tests"))) {
            rootPath = rootPath.getParent();
        }

        CompatibilityTestRunner runner = new CompatibilityTestRunner(rootPath);
        int exitCode = runner.runTests();
        System.exit(exitCode);
    }

    public int runTests() {
        try {
            cleanupLogs();
            startLogMonitoring();
            return runAllTests();
        } finally {
            monitorExecutor.shutdownNow();
        }
    }

    private void cleanupLogs() {
        System.out.println(ANSI_BLUE + "=== 清理旧的日志文件和兜底文件 ===" + ANSI_RESET);

        for (String module : TEST_MODULES) {
            // 处理嵌套模块路径
            String cleanModulePath = module.replace("/", File.separator);
            Path logDir = rootPath.resolve("compatibility-tests").resolve(cleanModulePath).resolve("logs");
            Path logxDir = rootPath.resolve("compatibility-tests").resolve(cleanModulePath).resolve("logx");

            if (Files.exists(logDir)) {
                try {
                    deleteDirectory(logDir);
                    System.out.println("  已清理logs: " + logDir);
                } catch (IOException e) {
                    System.err.println("  清理失败: " + logDir + " - " + e.getMessage());
                }
            }

            if (Files.exists(logxDir)) {
                try {
                    deleteDirectory(logxDir);
                    System.out.println("  已清理logx: " + logxDir);
                } catch (IOException e) {
                    System.err.println("  清理失败: " + logxDir + " - " + e.getMessage());
                }
            }
        }
        System.out.println();
    }

    private void deleteDirectory(Path directory) throws IOException {
        if (Files.exists(directory)) {
            Files.walk(directory)
                .sorted(Comparator.reverseOrder())
                .forEach(path -> {
                    try {
                        Files.delete(path);
                    } catch (IOException e) {
                        // 忽略删除错误
                    }
                });
        }
    }

    private void startLogMonitoring() {
        System.out.println(ANSI_GREEN + "=== 启动错误日志监控 ===" + ANSI_RESET);

        monitorExecutor.submit(() -> {
            while (!shouldStop && !Thread.currentThread().isInterrupted()) {
                try {
                    Thread.sleep(2000);
                    checkErrorLogs();
                } catch (InterruptedException e) {
                    break;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        System.out.println(ANSI_GREEN + "监控进程已启动" + ANSI_RESET);
        System.out.println();
    }

    private void checkErrorLogs() {
        for (String module : TEST_MODULES) {
            // 处理嵌套模块路径
            String cleanModulePath = module.replace("/", File.separator);
            Path errorLog = rootPath.resolve("compatibility-tests")
                .resolve(cleanModulePath)
                .resolve("logs")
                .resolve("application-error.log");

            if (Files.exists(errorLog)) {
                try {
                    long fileSize = Files.size(errorLog);

                    if (fileSize > 0) {
                        if (hasRealErrors(errorLog)) {
                            reportError(module, errorLog);
                            shouldStop = true;
                            errorModule = module;
                            return;
                        }
                    }
                } catch (IOException e) {
                    // 忽略读取错误
                }
            }
        }
    }

    private boolean hasRealErrors(Path errorLog) {
        try {
            List<String> lines = Files.readAllLines(errorLog);

            for (String line : lines) {
                if (line.contains("ERROR") && !line.contains("测试日志") && !line.contains("测试错误日志")) {
                    return true;
                }
            }

            return false;
        } catch (IOException e) {
            return true;
        }
    }

    private void reportError(String module, Path errorLog) {
        System.out.println();
        System.out.println(ANSI_RED + "=====================================" + ANSI_RESET);
        System.out.println(ANSI_RED + "❌ 检测到真实错误日志!" + ANSI_RESET);
        System.out.println(ANSI_RED + "模块: compatibility-tests/" + module + ANSI_RESET);
        System.out.println(ANSI_RED + "=====================================" + ANSI_RESET);
        System.out.println();
        System.out.println(ANSI_YELLOW + "真实错误日志内容（已过滤测试日志）:" + ANSI_RESET);

        try {
            List<String> lines = Files.readAllLines(errorLog);
            int realErrorCount = 0;

            for (String line : lines) {
                if (!line.contains("测试错误日志")) {
                    System.out.println(line);
                    realErrorCount++;
                }
            }

            if (realErrorCount == 0) {
                System.out.println(ANSI_GREEN + "（所有错误日志都是测试日志，已过滤）" + ANSI_RESET);
            }
        } catch (IOException e) {
            System.err.println("读取错误日志失败: " + e.getMessage());
        }

        System.out.println();
        System.out.println(ANSI_RED + "=====================================" + ANSI_RESET);
    }

    private int runAllTests() {
        System.out.println(ANSI_BLUE + "=== 开始运行兼容性测试 ===" + ANSI_RESET);
        System.out.println();

        boolean allPassed = true;

        for (String module : TEST_MODULES) {
            if (shouldStop) {
                System.out.println(ANSI_RED + "检测到错误日志，停止测试" + ANSI_RESET);
                allPassed = false;
                break;
            }

            System.out.println(ANSI_BLUE + "--------------------------------------" + ANSI_RESET);
            System.out.println(ANSI_BLUE + "运行测试: " + module + ANSI_RESET);
            System.out.println(ANSI_BLUE + "--------------------------------------" + ANSI_RESET);

            boolean testPassed = runModuleTest(module);

            if (!testPassed || shouldStop) {
                System.out.println(ANSI_RED + "✗ " + module + " 测试失败" + ANSI_RESET);
                allPassed = false;
                break;
            }

            System.out.println(ANSI_GREEN + "✓ " + module + " 测试通过" + ANSI_RESET);
            System.out.println();
        }

        System.out.println();

        if (allPassed && !shouldStop) {
            boolean hasFallbackFiles = checkFallbackFiles();

            if (hasFallbackFiles) {
                System.out.println(ANSI_RED + "=== ✗ 发现兜底文件，重试机制有异常 ===" + ANSI_RESET);
                return 1;
            }

            System.out.println(ANSI_GREEN + "=== ✓ 所有测试通过，无错误日志，无兜底文件 ===" + ANSI_RESET);
            return 0;
        } else {
            System.out.println(ANSI_RED + "=== ✗ 测试失败或发现错误日志 ===" + ANSI_RESET);

            if (errorModule != null) {
                System.out.println(ANSI_RED + "错误位置: compatibility-tests/" + errorModule + "/logs/application-error.log" + ANSI_RESET);
            }

            return 1;
        }
    }

    private boolean checkFallbackFiles() {
        System.out.println(ANSI_BLUE + "=== 检查兜底文件（logx目录） ===" + ANSI_RESET);

        boolean foundFallbackFiles = false;

        for (String module : TEST_MODULES) {
            // 处理嵌套模块路径
            String cleanModulePath = module.replace("/", File.separator);
            Path logxDir = rootPath.resolve("compatibility-tests")
                .resolve(cleanModulePath)
                .resolve("logx");

            if (Files.exists(logxDir) && Files.isDirectory(logxDir)) {
                try {
                    long fileCount = Files.list(logxDir)
                        .filter(Files::isRegularFile)
                        .count();

                    if (fileCount > 0) {
                        System.out.println(ANSI_RED + "✗ 发现兜底文件: " + module + "/logx/ (" + fileCount + " 个文件)" + ANSI_RESET);

                        Files.list(logxDir)
                            .filter(Files::isRegularFile)
                            .forEach(file -> {
                                System.out.println(ANSI_YELLOW + "  - " + file.getFileName() + ANSI_RESET);
                            });

                        foundFallbackFiles = true;
                    } else {
                        System.out.println(ANSI_GREEN + "✓ " + module + ": 无兜底文件" + ANSI_RESET);
                    }
                } catch (IOException e) {
                    System.err.println(ANSI_YELLOW + "⚠ 检查失败: " + module + "/logx/ - " + e.getMessage() + ANSI_RESET);
                }
            } else {
                System.out.println(ANSI_GREEN + "✓ " + module + ": logx目录不存在（正常）" + ANSI_RESET);
            }
        }

        System.out.println();
        return foundFallbackFiles;
    }

    private boolean runModuleTest(String module) {
        try {
            String mvnCommand = System.getProperty("os.name").toLowerCase().contains("windows")
                ? "mvn.cmd"
                : "mvn";

            // 处理嵌套模块路径
            String modulePath = "compatibility-tests/" + module;

            System.out.println(ANSI_YELLOW + "  执行命令: " + mvnCommand + " test -pl " + modulePath + ANSI_RESET);

            ProcessBuilder pb = new ProcessBuilder(
                mvnCommand,
                "test",
                "-pl",
                modulePath
            );

            pb.directory(rootPath.toFile());

            File devNull = new File(System.getProperty("os.name").toLowerCase().contains("windows")
                ? "NUL"
                : "/dev/null");
            pb.redirectOutput(devNull);
            pb.redirectError(devNull);

            System.out.println(ANSI_YELLOW + "  正在运行测试（输出已隐藏以提升性能）..." + ANSI_RESET);

            Process process = pb.start();

            while (process.isAlive()) {
                Thread.sleep(1000);

                if (shouldStop) {
                    System.out.println(ANSI_RED + "  检测到错误，终止测试进程..." + ANSI_RESET);
                    process.destroyForcibly();
                    return false;
                }
            }

            int exitCode = process.waitFor();

            if (exitCode != 0) {
                System.out.println(ANSI_RED + "  测试失败，退出码: " + exitCode + ANSI_RESET);
                System.out.println();

                checkAndReportFailureDetails(module);
            }

            return exitCode == 0;

        } catch (Exception e) {
            System.err.println(ANSI_RED + "运行测试失败: " + e.getMessage() + ANSI_RESET);
            e.printStackTrace();
            return false;
        }
    }

    private void checkAndReportFailureDetails(String module) {
        System.out.println(ANSI_YELLOW + "检查失败详情..." + ANSI_RESET);

        // 处理嵌套模块路径
        String cleanModulePath = module.replace("/", File.separator);
        Path errorLog = rootPath.resolve("compatibility-tests")
            .resolve(cleanModulePath)
            .resolve("logs")
            .resolve("application-error.log");

        if (Files.exists(errorLog)) {
            try {
                List<String> lines = Files.readAllLines(errorLog);
                List<String> realErrors = new ArrayList<>();

                for (String line : lines) {
                    if (!line.contains("测试错误日志")) {
                        realErrors.add(line);
                    }
                }

                if (!realErrors.isEmpty()) {
                    System.out.println(ANSI_RED + "发现错误日志 (" + realErrors.size() + " 行):" + ANSI_RESET);

                    int displayLimit = 20;

                    for (int i = 0; i < Math.min(displayLimit, realErrors.size()); i++) {
                        System.out.println(realErrors.get(i));
                    }

                    if (realErrors.size() > displayLimit) {
                        System.out.println(ANSI_YELLOW + "... (" + (realErrors.size() - displayLimit) + " 行已省略)" + ANSI_RESET);
                    }
                } else {
                    System.out.println(ANSI_GREEN + "错误日志文件存在，但都是测试日志（已过滤）" + ANSI_RESET);
                }
            } catch (IOException e) {
                System.err.println(ANSI_YELLOW + "无法读取错误日志: " + e.getMessage() + ANSI_RESET);
            }
        } else {
            System.out.println(ANSI_YELLOW + "未找到错误日志文件" + ANSI_RESET);
        }

        Path surefireReports = rootPath.resolve("compatibility-tests")
            .resolve(cleanModulePath)
            .resolve("target")
            .resolve("surefire-reports");

        if (Files.exists(surefireReports)) {
            System.out.println(ANSI_YELLOW + "Maven测试报告位置: " + surefireReports + ANSI_RESET);
        }

        System.out.println();
    }
}
