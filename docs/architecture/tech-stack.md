# 技术栈 - OSS Appender

## 概述

本文档详细介绍OSS Appender项目的完整技术栈选型，包括核心依赖、开发工具链、测试框架和部署技术等。技术选型遵循**简洁、高性能、可切换**的设计原则。

## 核心技术栈

### Java平台
```yaml
Java版本: Java 8+
编译目标: Java 8 (最大兼容性)
编码格式: UTF-8
构建工具: Apache Maven 3.9.6
```

**选型理由**: Java 8保证最大企业兼容性，满足大部分生产环境要求

### 高性能组件

#### LMAX Disruptor
```xml
<dependency>
    <groupId>com.lmax</groupId>
    <artifactId>disruptor</artifactId>
    <version>3.4.4</version>
</dependency>
```
**作用**: 高性能异步队列引擎
**选型理由**:
- 超低延迟（纳秒级）
- 无锁设计，避免线程竞争
- 支持高吞吐量（每秒百万级消息）
- 内存效率高，预分配环形缓冲区

#### 日志框架适配

##### Logback 集成
```xml
<dependency>
    <groupId>ch.qos.logback</groupId>
    <artifactId>logback-classic</artifactId>
    <version>1.2.13</version>
</dependency>
```

##### Log4j2 集成
```xml
<dependency>
    <groupId>org.apache.logging.log4j</groupId>
    <artifactId>log4j-core</artifactId>
    <version>2.21.1</version>
</dependency>
```

##### Log4j 1.x 集成
```xml
<dependency>
    <groupId>log4j</groupId>
    <artifactId>log4j</artifactId>
    <version>1.2.17</version>
</dependency>
```

## 云存储SDK

### AWS S3 SDK v2
```xml
<dependency>
    <groupId>software.amazon.awssdk</groupId>
    <artifactId>bom</artifactId>
    <version>2.28.16</version>
    <type>pom</type>
    <scope>import</scope>
</dependency>
```
**优势**:
- 原生S3支持
- 异步非阻塞API
- 内置重试和错误处理
- 支持多区域

### 阿里云OSS SDK
```xml
<dependency>
    <groupId>com.aliyun.oss</groupId>
    <artifactId>aliyun-sdk-oss</artifactId>
    <version>3.17.4</version>
</dependency>
```
**优势**:
- 国内云服务优化
- S3兼容接口
- 高可用性设计

### 腾讯云COS支持
- 通过S3兼容接口支持
- 无需额外SDK依赖

## 序列化与格式化

### Jackson JSON处理
```xml
<dependency>
    <groupId>com.fasterxml.jackson.core</groupId>
    <artifactId>jackson-databind</artifactId>
    <version>2.17.2</version>
</dependency>
```
**作用**: JSON Lines格式日志序列化
**选型理由**:
- 高性能JSON处理
- 内存效率优化
- 广泛企业采用

## 测试技术栈

### 单元测试框架
```xml
<!-- JUnit 5 -->
<dependency>
    <groupId>org.junit</groupId>
    <artifactId>junit-bom</artifactId>
    <version>5.10.1</version>
    <type>pom</type>
    <scope>import</scope>
</dependency>

<!-- Mockito -->
<dependency>
    <groupId>org.mockito</groupId>
    <artifactId>mockito-core</artifactId>
    <version>5.8.0</version>
    <scope>test</scope>
</dependency>

<!-- AssertJ -->
<dependency>
    <groupId>org.assertj</groupId>
    <artifactId>assertj-core</artifactId>
    <version>3.24.2</version>
    <scope>test</scope>
</dependency>
```

### 集成测试
```xml
<!-- Testcontainers for LocalStack -->
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>junit-jupiter</artifactId>
    <version>1.19.3</version>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>localstack</artifactId>
    <version>1.19.3</version>
    <scope>test</scope>
</dependency>
```

## 构建与质量工具

### Maven插件栈
```xml
<!-- 编译插件 -->
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-compiler-plugin</artifactId>
    <version>3.11.0</version>
</plugin>

<!-- 测试插件 -->
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-surefire-plugin</artifactId>
    <version>3.2.5</version>
</plugin>

<!-- 代码格式化 -->
<plugin>
    <groupId>net.revelc.code.formatter</groupId>
    <artifactId>formatter-maven-plugin</artifactId>
    <version>2.23.0</version>
</plugin>

<!-- 静态代码分析 -->
<plugin>
    <groupId>com.github.spotbugs</groupId>
    <artifactId>spotbugs-maven-plugin</artifactId>
    <version>4.7.3.6</version>
</plugin>

<!-- 安全扫描 -->
<plugin>
    <groupId>org.owasp</groupId>
    <artifactId>dependency-check-maven</artifactId>
    <version>9.0.7</version>
</plugin>
```

### 代码质量标准
- **代码格式**: Google Java Style Guide
- **静态分析**: SpotBugs + 自定义规则
- **安全扫描**: OWASP Dependency Check
- **测试覆盖率**: JaCoCo (目标 > 90%)

## 性能监控技术

### JVM性能监控
```java
// JVM内置监控
MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();
GarbageCollectorMXBean gcBean = ManagementFactory.getGarbageCollectorMXBeans();
```

### 应用指标
```java
// Micrometer集成 (可选)
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-core</artifactId>
    <version>1.12.1</version>
    <optional>true</optional>
</dependency>
```

## 部署与运行时

### 容器化支持
```dockerfile
# 基础镜像
FROM openjdk:8-jre-alpine

# JVM优化参数
ENV JAVA_OPTS="-XX:+UseG1GC -XX:MaxGCPauseMillis=200 -Xms256m -Xmx512m"
```

### JVM调优参数
```bash
# 生产环境推荐JVM参数
-XX:+UseG1GC                    # G1垃圾收集器
-XX:MaxGCPauseMillis=200        # 最大GC暂停时间
-XX:+UseStringDeduplication     # 字符串去重
-XX:+ExitOnOutOfMemoryError     # OOM时退出
-Xms256m -Xmx512m              # 堆内存设置
```

## 版本管理策略

### 依赖版本规范
```xml
<properties>
    ```xml
<!-- 主要依赖版本 -->
    <disruptor.version>4.0.0</disruptor.version>
    <aws.sdk.version>2.28.16</aws.sdk.version>
    <aliyun.oss.version>3.17.4</aliyun.oss.version>

    <!-- 测试依赖版本 -->
    <junit.version>5.10.1</junit.version>
    <mockito.version>5.8.0</mockito.version>
    <assertj.version>3.24.2</assertj.version>
</properties>
```

### 向后兼容性
- **Java**: 最低Java 8，向上兼容Java 11/17/21
- **日志框架**: 支持主流版本范围
- **云SDK**: 定期更新，保持安全性

## 开发工具推荐

### IDE配置
- **IntelliJ IDEA**: 推荐IDE
- **代码格式**: Google Java Style Guide配置
- **插件**: SpotBugs, SonarLint, Lombok Support

### 构建命令
```bash
# 完整构建
mvn clean compile test package

# 快速测试
mvn test

# 代码格式检查
mvn formatter:validate

# 安全扫描
mvn org.owasp:dependency-check-maven:check

# 静态分析
mvn spotbugs:check
```

## 技术选型原则总结

1. **简洁性**: 最小化依赖，避免过度工程化
2. **高性能**: 选择proven性能组件（Disruptor, Jackson）
3. **兼容性**: Java 8+基线，支持主流日志框架
4. **可维护性**: 广泛采用的开源组件，活跃社区
5. **安全性**: 定期更新，安全扫描集成
6. **云原生**: S3标准化，多云支持

此技术栈确保OSS Appender实现**高性能、低延迟、资源可控**的企业级日志处理能力。