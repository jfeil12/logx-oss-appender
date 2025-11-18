# LogX OSS Appender - iFlow配置文档

## 项目概述

LogX OSS Appender 是一个高性能日志上传组件套件，支持将日志异步批量上传到多种云对象存储服务。项目采用单仓库多模块（Monorepo）架构，提供完整的日志上传解决方案。

### 核心特性

✅ **高性能异步处理** - 使用LMAX Disruptor实现低延迟队列  
✅ **多云支持** - 支持阿里云OSS、AWS S3、腾讯云COS、MinIO、SF OSS等所有S3兼容存储  
✅ **多框架支持** - 完整支持Log4j、Log4j2、Logback日志框架  
✅ **企业级可靠性** - 全面的错误处理、重试机制和兜底文件机制  
✅ **零性能影响** - 非阻塞设计，不影响应用程序性能  
✅ **模块化设计** - 通过Java SPI机制实现低侵入性架构  
✅ **统一配置标准** - 所有框架使用一致的配置参数和环境变量命名  

## 项目架构

### 模块结构

```
logx-oss-appender/
├── logx-producer/              # 核心处理引擎
├── logx-s3-adapter/            # S3兼容存储适配器
├── logx-sf-oss-adapter/        # SF OSS存储适配器
├── log4j-oss-appender/         # Log4j 1.x版本的OSS Appender
├── log4j2-oss-appender/        # Log4j2版本的OSS Appender
├── logback-oss-appender/       # Logback版本的OSS Appender
├── compatibility-tests/        # 兼容性测试模块
└── docs/                       # 项目文档
```

### 依赖关系

```
logx-producer (核心)
    ↓
log4j-oss-appender
log4j2-oss-appender
logback-oss-appender
```

三个框架适配器都直接依赖于核心模块，彼此之间没有依赖关系。

## 核心组件设计

### 1. logx-producer (核心抽象层)

#### EnhancedDisruptorBatchingQueue (2025-10-05架构优化)
- 技术: LMAX Disruptor 3.4.4
- 队列管理: RingBuffer容量524288，YieldingWaitStrategy
- 批处理聚合: 事件驱动触发机制
  * 三个触发条件: 消息数8192、总字节数10MB、消息年龄60000毫秒(1分钟)
  * 触发时机: 新消息到达或批次结束时检查，无主动定时器线程
- NDJSON序列化: 将LogEvent列表序列化为NDJSON格式
- GZIP压缩: 阈值1KB，自动压缩批次数据
- 数据分片: 阈值10MB，自动分片大文件
- 性能统计: BatchMetrics（批次数、消息数、字节数、压缩率、分片数等）
- 容量控制: 失败重试3次 + 队列满时丢弃 + 限制队列大小
- 多生产者模式: 支持并发日志写入（thread-safe）
- 架构优化: 合并原BatchProcessor和DisruptorBatchingQueue，减少247行代码(-24%)，调用层次减少43%

#### AsyncEngine
- 技术: 基于EnhancedDisruptorBatchingQueue实现
- 职责: 协调整个日志处理流程，管理组件生命周期
- 资源管理: 统一管理兜底文件、关闭钩子等资源
- 数据流: 接收日志数据 → EnhancedDisruptorBatchingQueue → 存储服务
- 紧急保护: 内存超过512MB时直接写入兜底文件
- 并行上传: 支持多线程并行上传，默认1个线程

#### 存储服务接口设计

项目采用双层接口设计模式：

##### StorageInterface (基础存储接口)
```java
public interface StorageInterface {
    CompletableFuture<Void> putObject(String key, byte[] data);
    String getOssType();
    String getBucketName();
    void close();
    boolean supportsOssType(String ossType);
}
```

##### StorageService (扩展存储服务接口)
```java
// 继承StorageInterface
public interface StorageService extends StorageInterface {
    // 继承StorageInterface的所有方法
    // 可扩展更高级的服务方法
}
```

#### StorageOssType (存储后端类型枚举)
```java
public enum StorageOssType {
    ALIYUN_OSS, AWS_S3, MINIO, TENCENT_COS, HUAWEI_OBS, SF_OSS, SF_S3, GENERIC_S3
}
```

#### ThreadPoolManager
- 固定线程池: 默认1个线程，可配置
- 低优先级: Thread.MIN_PRIORITY
- CPU让出: CPU繁忙时主动yield
- 优雅关闭: 配合shutdown hook

#### ShutdownHookHandler
- JVM shutdown hook注册
- 30秒超时上传剩余日志
- 线程池协调关闭

#### FallbackManager
- 本地文件存储: 网络异常时将日志存储到应用相对路径
- 文件命名一致性: 保持与OSS相同的目录结构和命名规则
- 存储路径配置: 支持相对路径和绝对路径配置
- 文件清理机制: 定期清理过期兜底文件

#### FallbackScheduler
- 定时扫描: 定期扫描兜底目录中的文件
- 文件重传: 将兜底文件重新上传到云存储
- 成功清理: 上传成功后删除本地兜底文件
- 重试策略: 支持失败重试和指数退避

### 2. 框架适配器层

#### log4j-oss-appender
- 继承: AppenderSkeleton
- 配置: 与log4j2/logback保持一致的key
- 转换: LoggingEvent → 内部LogEvent格式

#### log4j2-oss-appender
- 继承: AbstractAppender
- 配置: 统一配置key标准
- 异步: 优化的异步事件处理
- 插件: 支持Log4j2插件机制

#### logback-oss-appender
- 继承: AppenderBase<ILoggingEvent>
- 配置: 统一配置key标准
- SLF4J: 完整兼容性支持

### 3. 存储适配器层

#### logx-s3-adapter
- 只负责具体的上传实现
- 不再处理数据分片逻辑
- 依赖核心层的数据分片处理
- 不再提供putObjects方法，只提供putObject方法
- 支持多种S3兼容存储（AWS S3、阿里云OSS、腾讯云COS、MinIO等）

#### logx-sf-oss-adapter
- 只负责具体的上传实现
- 不再提供putObjects方法，只提供putObject方法
- 依赖核心层的数据分片处理
- 使用SF OSS专有协议

## 技术栈

| 组件 | 名称 | 版本 | 用途 |
|------|------|------|------|
| **语言** | Java | 8+ | 核心开发语言 |
| **构建工具** | Maven | 3.9.6 | 项目构建 |
| **高性能队列** | LMAX Disruptor | 3.4.4 | 异步处理 |
| **云存储SDK** | AWS SDK | 2.28.16 | S3兼容存储 |
| **测试框架** | JUnit 5 | 5.10.1 | 单元测试 |
| **断言库** | AssertJ | 3.24.2 | 流式断言 |
| **Mock框架** | Mockito | 5.8.0 | Mock测试 |

## 配置管理

### 统一配置标准

```xml
<!-- 三个框架的统一配置key -->
<appender name="OSS" class="org.logx.{framework}.OSSAppender">
    <!-- 必需参数（存储配置） -->
    <endpoint>${LOGX_OSS_STORAGE_ENDPOINT:-https://oss-cn-hangzhou.aliyuncs.com}</endpoint>
    <accessKeyId>${LOGX_OSS_STORAGE_ACCESS_KEY_ID}</accessKeyId>
    <accessKeySecret>${LOGX_OSS_STORAGE_ACCESS_KEY_SECRET}</accessKeySecret>
    <bucket>${LOGX_OSS_STORAGE_BUCKET:-my-log-bucket}</bucket>
    <!-- 可选参数（存储配置） -->
    <region>${LOGX_OSS_STORAGE_REGION:-US}</region>
    <keyPrefix>${LOGX_OSS_STORAGE_KEY_PREFIX:-logx/}</keyPrefix>
    <ossType>${LOGX_OSS_STORAGE_OSS_TYPE:-SF_OSS}</ossType>
    <pathStyleAccess>${LOGX_OSS_STORAGE_PATH_STYLE_ACCESS:-false}</pathStyleAccess>
    <!-- 可选参数（引擎配置） -->
    <maxBatchCount>${LOGX_OSS_ENGINE_BATCH_COUNT:-8192}</maxBatchCount>
    <maxMessageAgeMs>${LOGX_OSS_ENGINE_BATCH_MAX_AGE_MS:-60000}</maxMessageAgeMs>
    <maxUploadSizeMb>${LOGX_OSS_ENGINE_MAX_UPLOAD_SIZE_MB:-10}</maxUploadSizeMb>
</appender>
```

### 配置优先级

系统支持多种配置源,按以下优先级顺序读取配置:
1. JVM系统属性 (-Dlogx.oss.storage.region=US)
2. 环境变量 (LOGX_OSS_STORAGE_REGION=US)
3. 配置文件属性 (logx.properties中的logx.oss.storage.region=US)
4. 代码默认值

### 高级配置参数

#### 存储配置参数
| 参数名 | 类型 | 默认值 | 描述 |
|--------|------|--------|------|
| `endpoint` | String | 根据云服务商自动识别 | 对象存储服务的访问端点 |
| `region` | String | US | 存储区域 |
| `accessKeyId` | String | 必需 | 访问密钥ID |
| `accessKeySecret` | String | 必需 | 访问密钥Secret |
| `bucket` | String | my-log-bucket | 存储桶名称 |
| `keyPrefix` | String | logx/ | 对象存储中的文件路径前缀 |
| `ossType` | String | SF_OSS | 存储后端类型，支持SF_OSS、S3等 |
| `pathStyleAccess` | Boolean | 根据云服务商类型自动识别 | 是否使用路径风格访问（Path-style Access） |

#### 引擎配置参数
| 参数名 | 类型 | 默认值 | 描述 |
|--------|------|--------|------|
| `maxBatchCount` | Integer | 8192 | 批处理最大消息数 |
| `maxMessageAgeMs` | Long | 60000 | 最早消息年龄阈值（毫秒） |
| `maxUploadSizeMb` | Integer | 10 | 单个上传文件最大大小（MB），同时控制分片阈值 |
| `queueCapacity` | Integer | 524288 | 队列容量（必须是2的幂） |
| `dropWhenQueueFull` | Boolean | false | 队列满时是否丢弃日志 |
| `multiProducer` | Boolean | false | 是否支持多生产者 |
| `maxRetries` | Integer | 3 | 最大重试次数 |
| `baseBackoffMs` | Long | 200 | 基础退避时间(毫秒) |
| `maxBackoffMs` | Long | 10000 | 最大退避时间(毫秒) |
| `enableCompression` | Boolean | true | 是否启用数据压缩 |
| `compressionThreshold` | Integer | 1024 | 启用压缩的数据大小阈值(字节) |
| `enableSharding` | Boolean | true | 是否启用数据分片处理 |
| `emergencyMemoryThresholdMb` | Integer | 512 | 紧急保护阈值（MB） |
| `fallbackRetentionDays` | Integer | 7 | 兜底文件保留天数 |
| `fallbackScanIntervalSeconds` | Integer | 60 | 兜底文件扫描间隔（秒） |

## Maven项目结构

### 版本管理策略

本项目采用统一的版本管理策略，所有子模块都继承自父POM的版本号，确保版本一致性：

1. **父POM版本**：所有子模块的版本号统一由父POM管理，当前版本为`1.0.0-SNAPSHOT`
2. **子模块版本继承**：子模块无需声明版本号，自动继承父POM的版本
3. **依赖版本统一管理**：所有第三方依赖和内部模块依赖的版本在父POM的`dependencyManagement`中统一定义
4. **版本属性定义**：关键依赖的版本通过属性定义，便于统一维护和升级

### 依赖管理优化

我们对项目的依赖管理进行了优化，主要体现在以下几个方面：

1. **内部模块依赖**：所有内部模块（如`logx-producer`、`log4j2-oss-appender`等）的版本在父POM中统一管理，子模块直接引用无需指定版本号
2. **第三方依赖版本控制**：通过`dependencyManagement`统一管理第三方依赖版本，确保所有模块使用一致的依赖版本
3. **日志框架版本属性**：为常用的日志框架（Log4j、Log4j2、Logback）定义了版本属性，便于维护和升级
4. **模块化适配器设计**：具体的云存储SDK依赖放在独立的适配器模块中，降低核心模块的依赖侵入性

## 开发环境

### 系统要求

- **Java**: OpenJDK 8u392 或更高版本
- **Maven**: 3.9.6 或更高版本
- **Git**: 2.0+ (支持submodules)

### 克隆项目

```bash
# 完整克隆（包含所有子模块）
git clone --recursive https://github.com/logx-oss-appender/logx-oss-appender.git
cd logx-oss-appender

# 如果已克隆但缺少子模块
git submodule update --init --recursive
```

## 构建和测试

### 完整构建

```bash
# 清理并构建所有模块
mvn clean install

# 并行构建（更快）
mvn clean install -T 1C

# 跳过测试的快速构建
mvn clean install -DskipTests
```

### 单模块构建

```bash
# 构建特定模块
mvn clean install -pl log4j2-oss-appender

# 构建模块及其依赖
mvn clean install -pl log4j2-oss-appender -am
```

### 测试执行

```bash
# 运行所有测试
mvn test

# 运行特定模块测试
mvn test -pl logx-producer

# 运行集成测试（需要MinIO环境）
mvn verify -Pintegration-tests

# 生成测试报告
mvn surefire-report:report
```

### 代码质量检查

```bash
# 代码格式检查
mvn formatter:validate

# 自动格式化代码
mvn formatter:format

# 静态代码分析
mvn spotbugs:check

# 安全扫描
mvn org.owasp:dependency-check-maven:check -Psecurity
```

## Git工作流程

### 分支管理

- **main**: 主分支，稳定发布版本
- **develop**: 开发分支，集成最新功能
- **feature/***: 功能分支，开发新特性
- **hotfix/***: 热修复分支，紧急修复

### Commit Message格式

```
<type>(<scope>): <description>

[optional body]

[optional footer]
```

#### Type类型
- `feat`: 新功能
- `fix`: Bug修复
- `docs`: 文档更新
- `style`: 代码格式调整
- `refactor`: 重构
- `test`: 测试相关
- `chore`: 构建工具、依赖更新

#### Scope范围
- `core`: logx-producer核心模块
- `log4j`: log4j适配器
- `log4j2`: log4j2适配器
- `logback`: logback适配器
- `s3`: S3存储适配器
- `sf`: SF OSS存储适配器
- `docs`: 文档
- `build`: 构建相关

## 故障排除

### 常见问题

#### 1. 子模块更新问题

```bash
# 如果子模块没有正确更新
git submodule deinit -f .
git submodule update --init --recursive
```

#### 2. Maven构建失败

```bash
# 清理所有缓存
mvn clean
rm -rf ~/.m2/repository/org/logx

# 重新构建
mvn clean install
```

#### 3. 代码格式问题

```bash
# 自动修复格式问题
mvn formatter:format

# 检查格式
mvn formatter:validate
```

#### 4. 依赖冲突

```bash
# 查看依赖树
mvn dependency:tree

# 解决冲突后重新构建
mvn clean install -U
```

### MinIO集成测试环境

运行集成测试需要MinIO环境：

```bash
# 进入兼容性测试目录
cd compatibility-tests/minio

# 启动MinIO本地环境
./start-minio-local.sh

# 运行集成测试
mvn test -pl logx-s3-adapter
```

## 发布流程

### 准备发布

1. **更新版本号**
   ```bash
   mvn versions:set -DnewVersion=1.0.0
   mvn versions:commit
   ```

2. **运行完整测试**
   ```bash
   mvn clean verify -Psecurity
   ```

3. **创建发布标签**
   ```bash
   git tag -a v1.0.0 -m "Release version 1.0.0"
   git push origin v1.0.0
   ```

## 项目路线图

### 当前版本 (v1.0.0 MVP)

当前MVP版本专注于核心功能，确保日志上传的**高性能**和**高可靠性**：

**✅ 已实现功能**：
- 高性能异步队列（LMAX Disruptor，100,000+ 消息/秒）
- 智能批处理优化（三触发条件：消息数、字节数、消息年龄）
- GZIP压缩（90%+压缩率）
- 数据分片处理（自动分片大文件）
- 失败重试机制（指数退避，最多3次）
- 兜底文件机制（网络异常时本地缓存）
- 优雅关闭保护（30秒超时保护）
- 多框架支持（Log4j、Log4j2、Logback）
- 多云支持（AWS S3、阿里云OSS、腾讯云COS、MinIO、SF OSS等）
- 模块化适配器设计（通过Java SPI机制支持运行时动态加载）

**❌ 明确不在当前版本范围的功能**：

根据项目决策记录，以下功能不在MVP版本实现：

1. **监控和告警接口** ([ADR-001](docs/DECISIONS.md#adr-001-mvp版本不实现监控和告警接口))
   - 原因：核心可靠性机制已满足需求，监控需求差异大
   - 替代方案：通过日志集成到现有监控系统（Prometheus、ELK等）
   - 未来计划：v2.0可能添加Metrics API、Callback API、JMX支持

2. **动态自适应批处理算法** ([ADR-002](docs/DECISIONS.md#adr-002-mvp版本不实现动态自适应批处理算法))
   - 原因：固定配置参数已满足核心需求，自适应算法复杂且难以通用
   - 替代方案：提供三个灵活的配置参数（maxBatchCount、maxBatchBytes、maxMessageAgeMs）
   - 未来计划：v2.0可能添加预设配置模式（低延迟模式、高吞吐模式）

### 未来版本规划

**v1.1.0** (性能优化版本)
- 优化内存占用
- 增强兜底文件管理
- 添加更多性能指标

**v2.0.0** (企业增强版本)
- 监控和告警接口
- 预设配置模式
- 更丰富的扩展点

详细的架构决策和理由请参考 **[项目决策记录](docs/DECISIONS.md)**。

## 文档资源

- [架构设计文档](docs/architecture.md) - 详细的技术架构说明
- [产品需求文档](docs/prd.md) - 项目需求和Epic定义
- [项目决策记录](docs/DECISIONS.md) - 架构和功能决策说明
- [开发者指南](docs/developer-guide.md) - 开发环境设置和贡献指南
- [Git管理指南](docs/git-management.md) - 分支策略、版本发布、协作流程
- [兜底机制配置文档](docs/fallback-configuration.md) - 兜底文件机制配置说明
- [测试规范文档](docs/testing-standards.md) - 测试标准和规范

## 许可证

本项目采用 [Apache License 2.0](LICENSE) 许可证。