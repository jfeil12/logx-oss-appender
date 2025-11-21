# 跨平台兼容性测试运行器

这是一个基于Java的跨平台测试运行器，支持**Windows**和**Linux**系统。

## 功能特性

✅ **跨平台兼容** - Windows和Linux使用同一套代码
✅ **实时监控** - 监控所有测试模块的错误日志
✅ **自动停止** - 发现错误日志立即停止测试并报告
✅ **详细报告** - 清晰显示错误位置和日志内容

## 运行方式

### 方式1：使用Maven运行（推荐）

**Windows:**
```powershell
cd compatibility-tests
mvn compile exec:java -pl test-runner
```

**Linux/macOS:**
```bash
cd compatibility-tests
mvn compile exec:java -pl test-runner
```

### 方式2：编译后运行

**第一步：编译**
```bash
cd compatibility-tests
mvn clean compile -pl test-runner
```

**第二步：运行（Windows）**
```powershell
cd test-runner
mvn exec:java
```

**第二步：运行（Linux/macOS）**
```bash
cd test-runner
mvn exec:java
```

### 方式3：从项目根目录运行

```bash
# 从项目根目录直接运行
mvn compile exec:java -pl compatibility-tests/test-runner
```

## 运行流程

1. **清理日志** - 删除所有测试模块的旧日志文件
2. **启动监控** - 启动后台线程监控错误日志
3. **运行测试** - 依次运行以下模块：
   - `config-consistency-test`
   - `jsp-servlet-test`
   - `multi-framework-test`
   - `spring-boot-test`
   - `spring-mvc-test`
   - `all-in-one-test/s3-all-in-one-logback-test`
   - `all-in-one-test/s3-all-in-one-log4j-test`
   - `all-in-one-test/s3-all-in-one-log4j2-test`
   - `jdk21-test`
4. **检测错误** - 每2秒检查一次 `logs/application-error.log`
5. **自动停止** - 发现错误立即停止并报告位置

## 监控的日志文件

测试运行器会监控以下位置的错误日志：

```
compatibility-tests/config-consistency-test/logs/application-error.log
compatibility-tests/jsp-servlet-test/logs/application-error.log
compatibility-tests/multi-framework-test/logs/application-error.log
compatibility-tests/spring-boot-test/logs/application-error.log
compatibility-tests/spring-mvc-test/logs/application-error.log
compatibility-tests/all-in-one-test/s3-all-in-one-logback-test/logs/application-error.log
compatibility-tests/all-in-one-test/s3-all-in-one-log4j-test/logs/application-error.log
compatibility-tests/all-in-one-test/s3-all-in-one-log4j2-test/logs/application-error.log
compatibility-tests/jdk21-test/logs/application-error.log
```

## 输出示例

### 成功场景

```
=== 清理旧的日志文件 ===
  已清理: compatibility-tests/spring-boot-test/logs

=== 启动错误日志监控 ===
监控进程已启动

=== 开始运行兼容性测试 ===

--------------------------------------
运行测试: config-consistency-test
--------------------------------------
[Maven测试输出...]
✓ config-consistency-test 测试通过

=== ✓ 所有测试通过，无错误日志 ===
```

### 错误场景

```
--------------------------------------
运行测试: spring-boot-test
--------------------------------------
[Maven测试输出...]

=====================================
❌ 检测到错误日志!
模块: compatibility-tests/spring-boot-test
=====================================

错误日志内容:
2025-10-17 10:30:45 ERROR [main] - 连接OSS失败
java.net.ConnectException: Connection refused
...

=====================================
检测到错误日志，停止测试

=== ✗ 测试失败或发现错误日志 ===
错误位置: compatibility-tests/spring-boot-test/logs/application-error.log
```

## 技术实现

- **语言**: Java 8+
- **构建工具**: Maven
- **并发**: ExecutorService后台监控
- **跨平台**: 自动检测操作系统（Windows使用mvn.cmd，Linux使用mvn）
- **监控间隔**: 2秒检查一次错误日志

## 常见问题

**Q: Windows上报错找不到mvn命令？**
A: 确保Maven已安装并添加到PATH环境变量中。

**Q: 如何单独运行某个测试模块？**
A: 使用标准Maven命令：
```bash
mvn test -pl compatibility-tests/spring-boot-test
```

**Q: JDK 21测试有什么特殊要求？**
A: JDK 21测试需要使用JDK 21运行环境：
```bash
# 切换到JDK 21
export JAVA_HOME=/path/to/jdk21
java -version

# 运行JDK 21测试
mvn test -pl compatibility-tests/jdk21-test

# 测试完成后切换回JDK 8
export JAVA_HOME=/path/to/jdk8
java -version
```

**Q: 如何查看详细的Maven输出？**
A: 添加 `-X` 参数：
```bash
mvn compile exec:java -pl test-runner -X
```

## 对比bash脚本方案

| 特性 | Bash脚本 | Java运行器 |
|------|----------|-----------|
| Windows支持 | ❌ 需要WSL/Git Bash | ✅ 原生支持 |
| Linux支持 | ✅ | ✅ |
| 依赖 | bash, pkill等工具 | 仅需Java和Maven |
| 可维护性 | 中等 | 高（Java代码） |
| 集成度 | 独立脚本 | Maven项目一部分 |
