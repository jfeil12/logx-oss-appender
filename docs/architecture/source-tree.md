# 源码树结构 - LogX OSS Appender

## 概述

本文档详细描述LogX OSS Appender项目的源码组织结构，遵循**分层抽象**架构和**统一包命名**原则。项目采用Maven多模块架构，核心抽象层与框架适配层清晰分离。

## 项目根目录结构

```
logx-oss-appender/
├── docs/                           # 项目文档
│   ├── architecture/              # 架构文档
│   ├── stories/                   # 用户故事
│   ├── prd.md                     # 产品需求文档
│   └── architecture.md            # 架构总览
├── logx-producer/                 # 核心抽象层 (高性能组件)
├── logx-s3-adapter/              # S3兼容存储适配器
├── logx-sf-oss-adapter/          # SF OSS存储适配器
├── log4j-oss-appender/           # Log4j 1.x 适配器
├── log4j2-oss-appender/          # Log4j2 适配器
├── logback-oss-appender/         # Logback 适配器
├── .bmad-core/                   # 开发工具链配置
├── pom.xml                       # 父项目Maven配置
└── README.md                     # 项目说明
```

## 核心组件: logx-producer

### 目录结构
```
logx-producer/
├── src/main/java/org/logx/
│   ├── adapter/                  # 适配器接口
│   ├── config/                   # 配置管理
│   ├── core/                     # 核心引擎
│   │   ├── AsyncEngine.java                      # 异步引擎
│   │   ├── EnhancedDisruptorBatchingQueue.java   # 增强的Disruptor批处理队列（2025-10-05架构重构）
│   │   ├── ResourceProtectedThreadPool.java      # 资源保护线程池
│   │   └── UploadHooks.java                     # 上传钩子接口
│   ├── error/                    # 错误处理
│   ├── exception/                # 自定义异常
│   ├── reliability/              # 可靠性保障
│   ├── retry/                    # 重试机制
│   └── storage/                  # 存储抽象
│       ├── StorageOssType.java          # 存储后端接口
│       ├── StorageConfig.java           # 存储配置接口
│       ├── StorageInterface.java        # 存储接口
│       └── s3/                  # S3存储实现
│           ├── AwsS3Config.java         # AWS S3配置
│           ├── S3Config.java                  # S3配置管理
│           ├── S3StorageAdapter.java          # S3存储适配器
│           ├── S3ConfigValidator.java         # S3配置验证器
│           ├── S3StorageAdapter.java          # S3存储适配器
│           ├── S3StorageFactory.java          # S3存储工厂
│           └── S3StorageInterface.java        # S3存储接口
├── src/test/java/                # 单元测试
└── pom.xml                       # 模块配置
```

### 核心类职责

#### 队列引擎层 (`core/`) - 2025-10-05架构重构
```java
// 高性能异步引擎
AsyncEngine.java
├── 功能: 异步处理引擎核心
├── 特性: 资源保护、线程隔离
└── 优化: 固定线程池、背压控制

// 增强的Disruptor批处理队列（合并原BatchProcessor和DisruptorBatchingQueue）
EnhancedDisruptorBatchingQueue.java
├── 功能: 基于LMAX Disruptor的一体化批处理队列
├── 队列管理: 无锁环形缓冲区，固定容量65536，背压控制
├── 批处理聚合: 三个触发条件（消息数4096、总字节10MB、消息年龄10分钟）
├── NDJSON序列化: LogEvent列表序列化为NDJSON格式
├── GZIP压缩: 阈值1KB，自动压缩批次数据
├── 数据分片: 阈值20MB，自动分片大文件为10MB块
├── 性能特性: 纳秒级延迟、百万级TPS、调用层次减少43%
└── 架构优化: 消除过度抽象，减少247行代码（-24%）

// 资源保护线程池
ResourceProtectedThreadPool.java
├── 功能: 固定大小线程池
├── 特性: 资源隔离、防止线程耗尽
└── 优化: 低优先级、守护线程

// 上传生命周期钩子
UploadHooks.java
├── 功能: 上传前后的回调接口
├── 用途: 监控、日志、清理
└── 扩展: 支持自定义业务逻辑
```

#### 存储抽象层 (`storage/`)
```java
// 存储后端接口
StorageOssType.java
├── 功能: 存储后端抽象接口
├── 实现: 支持多种存储类型
└── 扩展: 可自定义存储实现

// 存储配置接口
StorageConfig.java
├── 功能: 存储配置抽象接口
├── 实现: 统一配置管理
└── 扩展: 支持多种配置源

// 存储接口
StorageInterface.java
├── 功能: 存储操作抽象接口
├── 实现: 统一存储操作API
└── 扩展: 支持多种存储操作
```

#### S3存储实现层 (`storage/s3/`)
```java
// AWS S3配置
AwsS3Config.java
├── 功能: AWS S3专用配置
├── 特性: AWS SDK集成
└── 扩展: 支持AWS特有配置项

// S3配置管理
S3Config.java
├── 功能: S3配置实现
├── 多云支持: AWS S3, 阿里云OSS, 腾讯云COS
└── 运行时切换: 热配置更新

// S3配置验证器
S3ConfigValidator.java
├── 功能: S3配置验证
├── 实现: 参数合法性检查
└── 扩展: 支持自定义验证规则

// S3存储适配器
S3StorageAdapter.java
├── 功能: S3存储适配器实现
├── 实现: 适配不同云服务商
└── 扩展: 支持新的云存储服务

// S3存储工厂
S3StorageFactory.java
├── 功能: S3存储实例工厂
├── 实现: 根据配置创建存储实例
└── 扩展: 支持新的存储类型

// S3存储接口
S3StorageInterface.java
├── 功能: S3存储操作接口
├── 实现: 统一S3操作API
└── 扩展: 支持新的S3操作

// S3存储适配器
S3StorageAdapter.java
├── 功能: S3存储适配器实现
├── 实现: 适配不同云服务商
├── 多云SDK适配 (AWS, Aliyun)
└── 失败重试与降级策略
```

## 框架适配器模块

### Logback适配器: logback-oss-appender

```
logback-oss-appender/               # Logback集成模块
├── pom.xml                         # 模块POM文件
├── README.md                       # 使用说明
├── src/
│   ├── main/
│   │   └── java/org/logx/logback/
│   │       ├── LogbackBridge.java            # Logback桥接器
│   │       └── LogbackOSSAppender.java       # 异步Appender实现
│   └── test/
│       └── java/org/logx/logback/
│           └── *Test.java                    # 测试类
└── target/                         # 构建输出目录
```

### Log4j2适配器: log4j2-oss-appender

```
log4j2-oss-appender/
├── src/main/java/org/logx/log4j2/
│   ├── Log4j2Bridge.java            # Log4j2桥接器
│   ├── Log4j2OSSAppender.java       # Log4j2 Appender插件
│   └── OSSLookupPlugin.java         # OSS查找插件
├── src/main/resources/META-INF/org/apache/logging/log4j/core/config/plugins/
│   └── Log4j2Plugins.dat            # Log4j2插件注册
└── pom.xml
```

### Log4j 1.x适配器: log4j-oss-appender

```
log4j-oss-appender/
├── src/main/java/org/logx/log4j/
│   ├── Log4j1xBridge.java           # Log4j 1.x桥接器
│   └── Log4jOSSAppender.java        # Log4j 1.x Appender实现
└── pom.xml
```

## 包命名规范

### 统一包前缀策略
```java
// 核心抽象包
org.logx.adapter.*                // 适配器接口
org.logx.config.*                 // 配置管理
org.logx.core.*                   // 高性能引擎
org.logx.error.*                  // 错误处理
org.logx.exception.*              // 自定义异常
org.logx.reliability.*            // 可靠性保障
org.logx.retry.*                  // 重试机制
org.logx.storage.*                // 存储抽象

// 框架适配包
org.logx.logback.*                // Logback专用包
org.logx.log4j.*                  // Log4j 1.x专用包
org.logx.log4j2.*                 // Log4j2专用包
```

### 配置Key统一
```java
// 所有适配器使用相同配置前缀和两层结构
public static final String CONFIG_PREFIX = "logx.oss";

// 存储配置项命名 (logx.oss.storage.*)
logx.oss.storage.endpoint          // 对象存储服务的访问端点
logx.oss.storage.accessKeyId       // 访问密钥ID
logx.oss.storage.accessKeySecret   // 访问密钥Secret
logx.oss.storage.bucket            // 存储桶名称
logx.oss.storage.region            // 存储区域，默认值为ap-guangzhou
logx.oss.storage.keyPrefix         // 对象存储中的文件路径前缀，默认为logx/
logx.oss.storage.ossType           // 存储后端类型，默认为SF_OSS，支持SF_OSS、S3等

// 引擎配置项命名 (logx.oss.engine.*)
logx.oss.engine.maxUploadSizeMb    // 单个上传文件最大大小（MB），默认10MB
logx.oss.engine.batch.count        // 批处理最大消息数，默认8192
logx.oss.engine.batch.maxAgeMs     // 最早消息年龄阈值（毫秒），默认60000
logx.oss.engine.queue.capacity     // 队列容量，默认524288
```

## 文档组织结构

### docs/architecture/ (架构文档)
```
docs/architecture/
├── coding-standards.md           # 编码规范标准
├── tech-stack.md                # 技术栈选型说明
└── source-tree.md               # 源码树结构 (本文档)
```

### docs/stories/ (用户故事)
按Epic组织，支持敏捷开发追踪:
```
docs/stories/
├── 1.x.*.md                     # Epic 1: 核心基础设施
├── 2.x.*.md                     # Epic 2: 高性能异步队列
├── 3.x.*.md                     # Epic 3: 多框架适配器
└── 4.x.*.md                     # Epic 4: 生产就绪特性
```

## 测试目录结构

### 单元测试组织
```java
// 每个模块的测试结构
src/test/java/
├── <对应包结构>/               # 与main包结构对应
│   ├── *Test.java              # 单元测试类
│   ├── *IntegrationTest.java   # 集成测试类
│   └── testutils/              # 测试工具类
└── resources/
    ├── logback-test.xml        # 测试环境日志配置
    └── test-*.properties       # 测试配置文件
```

## 构建产物结构

### Maven输出目录
```
target/
├── classes/                    # 编译后的类文件
├── test-classes/              # 测试类文件
├── logx-oss-appender-*.jar         # 构建的JAR包
├── logx-oss-appender-*-sources.jar # 源码包
└── logx-oss-appender-*-javadoc.jar # 文档包
```

## 依赖关系图

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│ logback-oss-    │    │ log4j2-oss-     │    │ log4j-oss-      │
│ appender        │    │ appender        │    │ appender        │
└─────────┬───────┘    └─────────┬───────┘    └─────────┬───────┘
          │                      │                      │
          └──────────────────────┼──────────────────────┘
                                 │
                     ┌───────────▼───────────┐
                     │                       │
                     │  logx-producer        │
                     │  (核心抽象层)          │
                     │                       │
                     └───────────┬───────────┘
                                 │
            ┌────────────────────┼────────────────────┐
            │                    │                    │
┌───────────▼───────────┐┌───────▼────────┐┌──────────▼──────────┐
│ logx-s3-adapter       ││logx-sf-oss-    ││(其他存储适配器)      │
│ (S3兼容存储)          ││adapter         ││(可选)              │
│                       ││(SF OSS存储)    ││                    │
└───────────────────────┘└────────────────┘└─────────────────────┘
```

## 部署包结构

### 发布包组织
```
logx-oss-appender-release/
├── lib/
│   ├── logx-producer-*.jar           # 核心组件
│   ├── logx-s3-adapter-*.jar          # S3兼容存储适配器
│   ├── logx-sf-oss-adapter-*.jar      # SF OSS存储适配器
│   ├── logback-oss-appender-*.jar     # Logback适配器
│   ├── log4j2-oss-appender-*.jar      # Log4j2适配器
│   └── log4j-oss-appender-*.jar       # Log4j适配器
├── docs/                              # 完整文档
├── examples/                          # 配置示例
└── LICENSE
```

这个源码树结构确保了OSS Appender的**简洁性、高性能和可切换性**目标，通过清晰的分层和统一的包命名实现了架构的一致性和可维护性。