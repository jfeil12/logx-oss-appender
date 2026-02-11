# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Critical Development Rules

**RULE 1**: 必须使用中文沟通
- 所有与用户的交流都必须使用中文（代码说明、进度报告、提交信息、测试输出）
- 只有技术关键词、API名称、配置键名可以使用英文

**RULE 2**: 代码规范强制要求（来自 [编码标准文档](docs/architecture/coding-standards.md)）
- 注释必须在代码上一行，禁止尾行注释
- 所有if/for/while语句必须使用大括号
- 禁止使用System.out.println，必须使用SLF4J日志框架

## Project Overview

LogX OSS Appender is a high-performance Java logging component suite that provides asynchronous batch log uploading to cloud object storage services (Aliyun OSS, AWS S3, MinIO). The project uses a monorepo multi-module Maven architecture with a core abstraction layer and framework-specific adapters.

## Core Architecture

The project follows a **三层模块化架构**:

- **基础核心模块**:
  - `logx-producer` - 核心基础模块，提供日志生产和队列管理
  - `logx-s3-adapter` - S3兼容存储适配器，支持AWS S3、阿里云OSS、腾讯云COS、MinIO等
  - `logx-sf-oss-adapter` - SF OSS存储适配器，专门支持SF OSS存储服务

- **框架适配器模块**:
  - `log4j-oss-appender` - Log4j 1.x版本的OSS Appender
  - `log4j2-oss-appender` - Log4j2版本的OSS Appender
  - `logback-oss-appender` - Logback版本的OSS Appender

- **All-in-One集成包**:
  - SF OSS系列: `sf-log4j-oss-appender`, `sf-log4j2-oss-appender`, `sf-logback-oss-appender`
  - S3兼容系列: `s3-log4j-oss-appender`, `s3-log4j2-oss-appender`, `s3-logback-oss-appender`

Key architectural principles:
- **Simplicity**: Highly abstracted core with consistent adapter implementations
- **High Performance**: Minimal latency, low resource usage, high throughput
- **Switchability**: Runtime storage backend switching with zero data loss
- **Resource Protection**: Fixed thread pools, low priority scheduling, CPU yielding

## Common Development Commands

### Building and Testing

```bash
# 构建所有模块
mvn clean install

# 构建特定模块
mvn clean install -pl logx-producer

# 运行特定模块的测试
mvn test -pl logx-producer

# 运行特定测试类
mvn test -Dtest=AsyncEngineIntegrationTest -pl logx-producer

# 运行单个测试方法
mvn test -Dtest=AsyncEngineIntegrationTest#shouldAchieveThroughputTarget -pl logx-producer

# 跳过测试构建（快速构建）
mvn clean install -DskipTests

# 查看模块依赖树
mvn dependency:tree -pl logback-oss-appender
```

### Code Quality

```bash
# 静态分析（SpotBugs）
mvn spotbugs:check

# 安全漏洞检查
mvn dependency-check:check -Psecurity

# 生成Javadoc
mvn javadoc:javadoc

# 完整质量检查流程
mvn clean validate compile test spotbugs:check
```

## Epic-Based Development Structure

项目遵循Epic开发方法论（详见 `docs/prd.md`）：

- **Epic 1**: 核心基础设施与存储抽象接口 ✅ **已完成**
- **Epic 2**: 高性能异步队列引擎 ✅ **已完成**
- **Epic 3**: 多框架适配器实现 🚧 **进行中**
- **Epic 4**: 生产就绪与运维支持 📋 **待开始**
- **Epic 5**: 模块化适配器设计 ✅ **已完成**

### Epic 2 核心引擎完成状态

高性能异步处理引擎已完整实现，性能指标：
- **吞吐量**: 24,777+ 消息/秒
- **延迟**: 2.21ms 平均
- **内存占用**: 6MB
- **压缩率**: 94.4%
- **可靠性**: 100% 成功率，0% 数据丢失

核心组件：
- **DisruptorBatchingQueue**: 基于LMAX Disruptor的低延迟队列
- **BatchProcessor**: 智能批处理，支持GZIP压缩和NDJSON序列化
- **RetryManager**: 指数退避重试机制
- **ShutdownHookHandler**: 优雅关闭，30秒超时保护
- **兜底文件机制**: 网络异常时的本地缓存和定时重传

## Key Technical Details

### Core Module Structure

**logx-producer** (核心引擎):
- `org.logx.core` - AsyncEngine, DisruptorBatchingQueue, ResourceProtectedThreadPool
- `org.logx.batch` - BatchProcessor, compression, serialization
- `org.logx.storage` - StorageService接口抽象
- `org.logx.retry` - ExponentialBackoffRetry重试策略
- `org.logx.config` - 统一配置管理和验证

**Framework Adapters** (框架适配器):
- `log4j-oss-appender` - Log4j 1.x，继承AppenderSkeleton
- `log4j2-oss-appender` - Log4j2，使用@Plugin注解
- `logback-oss-appender` - Logback，继承AppenderBase

**Storage Adapters** (存储适配器，通过Java SPI加载):
- `logx-s3-adapter` - S3兼容存储（AWS S3、阿里云OSS、MinIO）
- `logx-sf-oss-adapter` - SF OSS存储

**Compatibility Tests** (兼容性测试):
- `compatibility-tests/spring-boot-test` - Spring Boot集成测试
- `compatibility-tests/spring-mvc-test` - Spring MVC集成测试
- `compatibility-tests/jsp-servlet-test` - JSP/Servlet环境测试
- `compatibility-tests/multi-framework-test` - 多框架共存测试
- `compatibility-tests/performance-test` - 性能一致性测试
- `compatibility-tests/config-consistency-test` - 配置一致性测试
- `compatibility-tests/minio` - MinIO集成测试环境

### Performance Configuration

关键性能配置参数：
- **batchSize**: 500 (批处理大小，10-10000可调)
- **flushIntervalMs**: 3000 (强制刷新间隔，毫秒)
- **maxQueueSize**: 100000 (队列容量)
- **enableCompression**: true (GZIP压缩，90%+压缩率)
- **maxRetries**: 3 (失败重试次数)

线程池配置：
- **corePoolSize**: 2
- **maxPoolSize**: 4
- **priority**: Thread.MIN_PRIORITY (低优先级，不影响业务)

### Testing Strategy

测试框架：JUnit 5 + AssertJ + Mockito，目标覆盖率 90%+

#### 核心模块测试

```bash
# 运行核心集成测试（验证Epic 2完整系统）
mvn test -Dtest=AsyncEngineIntegrationTest -pl logx-producer

# 运行配置兼容性测试
mvn test -Dtest=ConfigCompatibilityTest -pl logx-producer

# 运行框架适配器测试
mvn test -pl log4j2-oss-appender
mvn test -pl logback-oss-appender
```

#### MinIO真实集成测试

项目使用MinIO进行真实的存储集成测试，支持本地和Docker两种方式：

**启动MinIO服务**：
```bash
# 方式1：本地MinIO（推荐）
cd compatibility-tests/minio
./start-minio-local.sh

# 方式2：Docker MinIO
cd compatibility-tests/minio/docker
./start-minio-docker.sh
```

**运行MinIO集成测试**：
```bash
# 运行S3适配器的MinIO集成测试
mvn test -Dtest=MinIOIntegrationTest -pl logx-s3-adapter

# 使用自定义MinIO配置
export LOGX_OSS_ENDPOINT=http://localhost:9000
export LOGX_OSS_BUCKET=logx-test-bucket
mvn test -Dtest=MinIOIntegrationTest -pl logx-s3-adapter
```

**测试配置文件**：
- `logx-s3-adapter/src/test/resources/minio-test.properties` - MinIO测试配置
- `logx-sf-oss-adapter/src/test/resources/sf-oss-test.properties` - SF OSS测试配置

配置优先级：JVM系统属性 > 环境变量 > 配置文件 > 默认值

#### Mock vs 真实集成测试

**合理使用Mock的场景**：
- `logx-producer` - 核心模块不依赖具体存储实现，使用TestTrackingStorageService
- `BatchProcessorPerformanceTest` - 性能测试需排除网络延迟，使用MockStorageService
- 单元测试 - 测试特定逻辑，不涉及外部依赖

**真实集成测试**：
- `MinIOIntegrationTest` - S3适配器的真实MinIO上传测试
- `logx-sf-oss-adapter` - SF OSS测试（需真实凭证时可运行）
- `compatibility-tests/*` - 各框架兼容性测试（可配置使用MinIO）

### Configuration Standards

所有框架适配器使用统一的配置键前缀：`logx.oss.*`

**必需参数**:
- `logx.oss.endpoint` - 存储服务端点
- `logx.oss.accessKeyId` - 访问密钥ID
- `logx.oss.accessKeySecret` - 访问密钥Secret
- `logx.oss.bucket` - 存储桶名称

**可选参数**:
- `logx.oss.region` (默认: ap-guangzhou)
- `logx.oss.keyPrefix` (默认: logs/)
- `logx.oss.ossType` (默认: S3)
- `logx.oss.pathStyleAccess` (MinIO必需为true，S3为false)
- `logx.oss.enableSsl` (默认: true，MinIO本地开发可设为false)
- `logx.oss.maxBatchCount` (默认: 100)
- `logx.oss.flushIntervalMs` (默认: 5000)

**配置优先级**（从高到低）：
1. JVM系统属性 (`-Dlogx.oss.region=ap-guangzhou`)
2. 环境变量 (`LOGX_OSS_REGION=ap-guangzhou`)
3. 配置文件属性 (`application.properties`中的`logx.oss.region=ap-guangzhou`)
4. 代码默认值

**环境变量命名规则**：
将配置键中的点号替换为下划线并转为大写
- `logx.oss.endpoint` → `LOGX_OSS_ENDPOINT`
- `logx.oss.accessKeyId` → `LOGX_OSS_ACCESS_KEY_ID`
- `logx.oss.region` → `LOGX_OSS_REGION`

**环境变量示例**：
```bash
# 基本配置
export LOGX_OSS_ACCESS_KEY_ID="your-access-key-id"
export LOGX_OSS_ACCESS_KEY_SECRET="your-access-key-secret"
export LOGX_OSS_BUCKET="your-bucket-name"
export LOGX_OSS_ENDPOINT="https://oss-cn-hangzhou.aliyuncs.com"
export LOGX_OSS_REGION="ap-guangzhou"

# MinIO本地配置
export LOGX_OSS_ENDPOINT="http://localhost:9000"
export LOGX_OSS_ACCESS_KEY_ID="minioadmin"
export LOGX_OSS_ACCESS_KEY_SECRET="minioadmin"
export LOGX_OSS_BUCKET="logx-test-bucket"
export LOGX_OSS_PATH_STYLE_ACCESS="true"
export LOGX_OSS_ENABLE_SSL="false"
```

## Java SPI模块化适配器设计

项目采用Java SPI机制实现存储适配器的运行时动态加载：

**设计优势**：
- **按需引入**: 用户只引入需要的存储适配器，避免不必要的依赖
- **运行时切换**: 支持通过`ossType`配置参数切换存储服务（SF_OSS/S3）
- **低侵入性**: logx-producer核心模块不直接依赖任何云存储SDK
- **易扩展**: 可轻松添加新的存储服务适配器

**使用方式**：
```xml
<!-- All-in-One集成包（推荐） -->
<!-- SF OSS + Logback -->
<dependency>
    <groupId>org.logx</groupId>
    <artifactId>sf-logback-oss-appender</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>

<!-- S3兼容存储 + Logback -->
<dependency>
    <groupId>org.logx</groupId>
    <artifactId>s3-logback-oss-appender</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

All-in-One包自动包含：框架适配器 + 存储适配器 + logx-producer核心模块

## Important Reminders

### 代码规范
- 所有与用户的交流必须使用中文
- 注释必须在代码上一行，禁止尾行注释
- 所有控制语句必须使用大括号
- 使用SLF4J日志框架，禁止System.out.println
- 测试覆盖率目标：90%+

### 测试规范
- **避免硬编码测试配置**：使用配置文件（如`minio-test.properties`）和环境变量
- **区分Mock和真实测试**：核心模块使用Mock，存储适配器使用真实集成测试
- **MinIO集成测试**：优先使用本地MinIO方式，确保`pathStyleAccess=true`
- **配置优先级**：JVM系统属性 > 环境变量 > 配置文件 > 默认值
- **环境变量命名**：统一使用`LOGX_OSS_*`前缀，配置键转大写下划线格式

### 配置标准
- **统一配置前缀**：必须使用`logx.oss.*`，禁止使用`s3.*`等其他前缀
- **默认值**：region默认为`ap-guangzhou`（来自PRD文档）
- **MinIO特殊配置**：`pathStyleAccess=true`, `enableSsl=false`

### 开发流程
- 遵循Epic开发方法论，当前重点：Epic 3 (多框架适配器实现)