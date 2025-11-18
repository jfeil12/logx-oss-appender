# LogX OSS Appender

一个高性能日志上传组件套件，支持将日志异步批量上传到阿里云OSS和AWS S3兼容的对象存储服务。

## 项目概述

LogX OSS Appender 为Java应用程序提供了一套完整的日志上传解决方案，包含六个核心模块：

### 核心模块

- **[logx-producer](logx-producer)** - 核心基础模块，提供日志生产和队列管理
- **[logx-s3-adapter](logx-s3-adapter)** - S3兼容存储适配器，支持AWS S3、阿里云OSS、腾讯云COS、MinIO、SF OSS等
- **[log4j-oss-appender](log4j-oss-appender)** - Log4j 1.x版本的OSS Appender
- **[log4j2-oss-appender](log4j2-oss-appender)** - Log4j2版本的OSS Appender
- **[logback-oss-appender](logback-oss-appender)** - Logback版本的OSS Appender

### All-in-One集成包

适用于非Maven用户的开箱即用方案（位于`all-in-one/`目录）：

- **[s3-log4j-oss-appender](all-in-one/s3-log4j-oss-appender)** - Log4j 1.x + S3适配器 + 所有依赖（~25MB Fat JAR）
- **[s3-log4j2-oss-appender](all-in-one/s3-log4j2-oss-appender)** - Log4j2 + S3适配器 + 所有依赖（~25MB Fat JAR）
- **[s3-logback-oss-appender](all-in-one/s3-logback-oss-appender)** - Logback + S3适配器 + 所有依赖（~25MB Fat JAR）

所有模块都遵循统一的包命名规范和配置Key标准，确保系统的一致性和可维护性。

## 项目结构

```
logx-oss-appender/
├── pom.xml                          # 父POM，管理所有模块和依赖版本
├── README.md                        # 项目说明文档
├── IFLOW.md                         # AI工作流配置
├── AGENTS.md                        # AI代理配置
├── .claude/                         # Claude Code配置目录
├── docs/                           # 项目文档目录
├── compatibility-tests/            # 集成和兼容性测试模块（独立构建）
│   ├── pom.xml                     # 集成测试父POM
│   ├── spring-boot-test/           # Spring Boot集成和兼容性测试
│   ├── spring-mvc-test/            # Spring MVC集成和兼容性测试
│   ├── jsp-servlet-test/           # JSP/Servlet集成和兼容性测试
│   ├── multi-framework-test/       # 多框架集成和兼容性测试
│   └── config-consistency-test/    # 配置一致性集成测试
├── logx-producer/                  # 核心生产者模块
│   └── src/main/java/              # 核心队列和生产逻辑
├── logx-s3-adapter/                # S3兼容存储适配器
│   └── src/main/java/              # S3存储服务实现
├── log4j-oss-appender/             # Log4j 1.x OSS Appender
│   └── src/main/java/              # Log4j 1.x集成实现
├── log4j2-oss-appender/            # Log4j2 OSS Appender
│   └── src/main/java/              # Log4j2集成实现
├── logback-oss-appender/           # Logback OSS Appender
│   └── src/main/java/              # Logback集成实现
└── all-in-one/                     # All-in-One集成包（Fat JAR）
    ├── pom.xml                     # All-in-One父POM
    ├── s3-log4j-oss-appender/      # Log4j 1.x All-in-One包
    ├── s3-log4j2-oss-appender/     # Log4j2 All-in-One包
    └── s3-logback-oss-appender/    # Logback All-in-One包
```


### Maven构建优化

- **统一父POM版本为1.0.0-SNAPSHOT**
- **依赖管理统一**：使用dependencyManagement统一版本控制
- **构建流程标准化**：支持标准的`mvn install`命令
- **模块化架构**：清晰的模块依赖关系和职责分工

### 版本管理策略

本项目采用统一的版本管理策略，所有子模块都继承自父POM的版本号，确保版本一致性：

1. **父POM版本**：所有子模块的版本号统一由父POM管理，当前版本为`1.0.0-SNAPSHOT`
2. **子模块版本继承**：子模块无需声明版本号，自动继承父POM的版本
3. **依赖版本统一管理**：所有第三方依赖和内部模块依赖的版本在父POM的`dependencyManagement`中统一定义
4. **版本属性定义**：关键依赖的版本通过属性定义，便于统一维护和升级

### 依赖管理优化

我们对项目的依赖管理进行了优化，主要体现在以下几个方面：

1. **内部模块依赖**：所有内部模块（如`logx-producer`、`log4j2-oss-appender`等）的版本在父POM中统一管理，保证版本一致性
2. **第三方依赖版本控制**：通过`dependencyManagement`统一管理第三方依赖版本，确保所有模块使用一致的依赖版本
3. **日志框架版本属性**：为常用的日志框架（Log4j、Log4j2、Logback）定义了版本属性，便于维护和升级

## 特性

✅ **高性能异步处理** - 使用LMAX Disruptor实现低延迟队列
✅ **多云支持** - 支持阿里云OSS和AWS S3兼容存储
✅ **多框架支持** - 完整支持Log4j、Log4j2、Logback日志框架
✅ **企业级可靠性** - 全面的错误处理和重试机制
✅ **零性能影响** - 非阻塞设计，不影响应用程序性能

## 快速开始

### 系统要求

- Java 8 或更高版本
- Maven 3.6+

### 三步快速集成

LogX OSS Appender 提供两种使用方式：Maven/Gradle依赖（推荐）和All-in-One Fat JAR（非Maven用户）。

#### 方式一：Maven/Gradle依赖（推荐）

**1. 添加依赖**

根据你使用的日志框架选择对应的依赖：

**Logback + S3兼容存储**:
```xml
<dependencies>
    <!-- 日志框架适配器 -->
    <dependency>
        <groupId>org.logx</groupId>
        <artifactId>logback-oss-appender</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </dependency>

    <!-- 存储服务适配器 -->
    <dependency>
        <groupId>org.logx</groupId>
        <artifactId>logx-s3-adapter</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </dependency>
</dependencies>
```

**Log4j2 + S3兼容存储**:
```xml
<dependencies>
    <!-- 日志框架适配器 -->
    <dependency>
        <groupId>org.logx</groupId>
        <artifactId>log4j2-oss-appender</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </dependency>

    <!-- 存储服务适配器 -->
    <dependency>
        <groupId>org.logx</groupId>
        <artifactId>logx-s3-adapter</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </dependency>
</dependencies>
```

**Log4j 1.x + S3兼容存储**:
```xml
<dependencies>
    <!-- 日志框架适配器 -->
    <dependency>
        <groupId>org.logx</groupId>
        <artifactId>log4j-oss-appender</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </dependency>

    <!-- 存储服务适配器 -->
    <dependency>
        <groupId>org.logx</groupId>
        <artifactId>logx-s3-adapter</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </dependency>
</dependencies>
```

**2. 在日志框架中注册 Appender**

根据你使用的日志框架，选择对应的配置方式。所有连接信息、性能参数等通过 `logx.properties` 统一管理。

**Logback 配置** (logback.xml):
```xml
<configuration>
    <appender name="OSS_APPENDER" class="org.logx.logback.LogbackOSSAppender">
        <encoder>
            <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n</pattern>
        </encoder>
    </appender>
    <root level="INFO">
        <appender-ref ref="OSS_APPENDER"/>
    </root>
</configuration>
```

**Log4j2 配置** (log4j2.xml):
```xml
<Configuration>
    <Appenders>
        <Log4j2OSSAppender name="OSS_APPENDER">
            <PatternLayout pattern="%d{yyyy-MM-dd HH:mm:ss.SSS} [%t] %-5level %logger{36} - %msg%n"/>
        </Log4j2OSSAppender>
    </Appenders>
    <Loggers>
        <Root level="INFO">
            <AppenderRef ref="OSS_APPENDER"/>
        </Root>
    </Loggers>
</Configuration>
```

**Log4j 1.x 配置** (log4j.xml):
```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE log4j:configuration SYSTEM "log4j.dtd">
<log4j:configuration>
    <appender name="OSS_APPENDER" class="org.logx.log4j.Log4jOSSAppender">
        <layout class="org.apache.log4j.PatternLayout">
            <param name="ConversionPattern" value="%d{yyyy-MM-dd HH:mm:ss.SSS} [%t] %-5p %c{1} - %m%n"/>
        </layout>
    </appender>
    <root>
        <level value="INFO"/>
        <appender-ref ref="OSS_APPENDER"/>
    </root>
</log4j:configuration>
```

**3. 创建 `logx.properties` 控制所有参数**

默认按 `/app/deploy/conf/logx.properties` → `classpath:logx.properties` → `./logx.properties` 顺序查找。推荐开发环境放在 `src/main/resources/logx.properties`，生产环境放在外部配置目录。

**必需配置**:
```properties
# 启用开关
logx.oss.enabled=true

# 存储服务配置
logx.oss.storage.ossType=SF_S3
logx.oss.storage.endpoint=https://oss-cn-hangzhou.aliyuncs.com
logx.oss.storage.accessKeyId=your-access-key-id
logx.oss.storage.accessKeySecret=your-access-key-secret
logx.oss.storage.bucket=your-bucket-name
logx.oss.storage.region=cn-hangzhou
logx.oss.storage.keyPrefix=logx/
```

**可选优化配置**:
```properties
# 引擎与重试配置（可选，按需调整）
logx.oss.engine.batch.count=8192
logx.oss.engine.batch.maxAgeMs=60000
logx.oss.engine.queue.capacity=524288
logx.oss.engine.retry.maxRetries=3
```

#### 方式二：All-in-One Fat JAR（非Maven用户）

下载预编译的All-in-One包，开箱即用，无需管理复杂依赖关系：

- `s3-logback-oss-appender-1.0.0-SNAPSHOT.jar` - Logback + S3适配器（~25MB）
- `s3-log4j2-oss-appender-1.0.0-SNAPSHOT.jar` - Log4j2 + S3适配器（~25MB）
- `s3-log4j-oss-appender-1.0.0-SNAPSHOT.jar` - Log4j 1.x + S3适配器（~25MB）

**使用步骤**：

**1. 选择对应的All-in-One包并添加到classpath**

**Logback 用户**:
```bash
# 将JAR添加到classpath
java -cp s3-logback-oss-appender-1.0.0-SNAPSHOT.jar:your-app.jar YourApp
```

**Log4j2 用户**:
```bash
# 将JAR添加到classpath
java -cp s3-log4j2-oss-appender-1.0.0-SNAPSHOT.jar:your-app.jar YourApp
```

**Log4j 1.x 用户**:
```bash
# 将JAR添加到classpath
java -cp s3-log4j-oss-appender-1.0.0-SNAPSHOT.jar:your-app.jar YourApp
```

**2. 配置日志框架**（与方式一步骤2完全相同）

使用相同的XML配置文件，All-in-One包已包含所有必要的类和依赖。

**3. 创建 `logx.properties`**（与方式一步骤3完全相同）

配置方式与Maven/Gradle方式完全一致，确保统一的用户体验。

### Java代码示例

```java
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LogExample {
    private static final Logger logger = LoggerFactory.getLogger(LogExample.class);

    public void doSomething() {
        logger.info("开始处理业务逻辑");
        logger.warn("这是一个警告信息");
        logger.error("发生了错误", new RuntimeException("示例异常"));
    }
}
```

### 构建项目

```bash
# 克隆项目（包含所有子模块）
git clone --recursive https://github.com/logx-oss-appender/logx-oss-appender.git
cd logx-oss-appender

# 构建所有模块
mvn clean install

# 构建特定模块
mvn clean install -pl log4j2-oss-appender
```

### 安装依赖

根据你使用的日志框架和存储服务选择对应的依赖：

#### Maven 依赖

推荐使用两个核心依赖的方式，简单清晰：

```xml
<dependencies>
    <!-- 日志框架适配器（选择其一） -->
    <dependency>
        <groupId>org.logx</groupId>
        <artifactId>logback-oss-appender</artifactId>  <!-- 或 log4j-oss-appender、log4j2-oss-appender -->
        <version>1.0.0-SNAPSHOT</version>
    </dependency>

    <!-- 存储服务适配器 -->
    <dependency>
        <groupId>org.logx</groupId>
        <artifactId>logx-s3-adapter</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </dependency>
</dependencies>
```

**各框架组合示例：**

```xml
<!-- Logback + S3兼容存储 -->
<dependencies>
    <dependency>
        <groupId>org.logx</groupId>
        <artifactId>logback-oss-appender</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </dependency>
    <dependency>
        <groupId>org.logx</groupId>
        <artifactId>logx-s3-adapter</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </dependency>
</dependencies>

<!-- Log4j2 + S3兼容存储 -->
<dependencies>
    <dependency>
        <groupId>org.logx</groupId>
        <artifactId>log4j2-oss-appender</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </dependency>
    <dependency>
        <groupId>org.logx</groupId>
        <artifactId>logx-s3-adapter</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </dependency>
</dependencies>

<!-- Log4j 1.x + S3兼容存储 -->
<dependencies>
    <dependency>
        <groupId>org.logx</groupId>
        <artifactId>log4j-oss-appender</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </dependency>
    <dependency>
        <groupId>org.logx</groupId>
        <artifactId>logx-s3-adapter</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </dependency>
</dependencies>
```


#### Gradle 依赖

推荐使用两个核心依赖的方式：

```groovy
dependencies {
    // Logback + S3示例
    implementation 'org.logx:logback-oss-appender:1.0.0-SNAPSHOT'
    implementation 'org.logx:logx-s3-adapter:1.0.0-SNAPSHOT'

    // 其他组合示例：
    // Log4j 1.x + S3
    // implementation 'org.logx:log4j-oss-appender:1.0.0-SNAPSHOT'
    // implementation 'org.logx:logx-s3-adapter:1.0.0-SNAPSHOT'

    // Log4j2 + S3
    // implementation 'org.logx:log4j2-oss-appender:1.0.0-SNAPSHOT'
    // implementation 'org.logx:logx-s3-adapter:1.0.0-SNAPSHOT'
}
```

#### 非Maven/Gradle项目依赖引入

对于不使用Maven或Gradle的项目，推荐使用All-in-One Fat JAR包，开箱即用：

##### 1. All-in-One Fat JAR（推荐）

从`all-in-one/`目录获取预编译的Fat JAR包，每个包包含所有必要的依赖（~25MB）：

- **s3-logback-oss-appender-1.0.0-SNAPSHOT.jar** - Logback + S3适配器 + 所有依赖
- **s3-log4j2-oss-appender-1.0.0-SNAPSHOT.jar** - Log4j2 + S3适配器 + 所有依赖
- **s3-log4j-oss-appender-1.0.0-SNAPSHOT.jar** - Log4j 1.x + S3适配器 + 所有依赖

**使用示例**：
```bash
# 将JAR添加到classpath
java -cp s3-logback-oss-appender-1.0.0-SNAPSHOT.jar:your-app.jar YourApp
```

##### 2. 手动引入JAR包（高级用法）

如需手动管理依赖，可分别引入各组件的JAR包：

1. **核心依赖**：
   - `logx-producer-1.0.0-SNAPSHOT.jar` - 核心日志处理引擎
   - `logx-s3-adapter-1.0.0-SNAPSHOT.jar` - S3兼容存储适配器

2. **日志框架适配器**（选择其一）：
   - `log4j-oss-appender-1.0.0-SNAPSHOT.jar` - Log4j 1.x适配器
   - `log4j2-oss-appender-1.0.0-SNAPSHOT.jar` - Log4j2适配器
   - `logback-oss-appender-1.0.0-SNAPSHOT.jar` - Logback适配器

3. **第三方依赖**：
   - 需要额外引入AWS SDK、LMAX Disruptor等传递依赖（推荐使用All-in-One包避免依赖管理复杂性）

##### 3. 环境要求

- Java 8或更高版本
- 对应的日志框架版本：
  - Log4j 1.2.17或更高版本
  - Log4j2 2.22.1或更高版本
  - Logback 1.2.13或更高版本

### 配置参数说明

所有配置都通过 `logx.properties` 统一管理，支持以下配置参数：

#### 必需参数

| 参数名 | 类型 | 说明 | 示例 |
|--------|------|------|------|
| **endpoint** | String | 对象存储服务的访问端点 | `https://oss-cn-hangzhou.aliyuncs.com` |
| **accessKeyId** | String | 访问密钥ID | `${LOGX_OSS_STORAGE_ACCESS_KEY_ID}` |
| **accessKeySecret** | String | 访问密钥Secret | `${LOGX_OSS_STORAGE_ACCESS_KEY_SECRET}` |
| **bucket** | String | 存储桶名称 | `my-log-bucket` |

#### 可选参数

| 参数名 | 类型 | 默认值 | 说明 |
|--------|------|--------|------|
| **enabled** | Boolean | true | 是否启用appender，为false时appender不会处理任何日志 |
| **region** | String | ap-guangzhou | 存储区域 |
| **keyPrefix** | String | logx/ | 对象存储中的文件路径前缀 |
| **ossType** | String | SF_OSS | 存储后端类型，支持SF_OSS、S3等 |
| **maxQueueSize** | Integer | 524288 | 内存队列大小（必须是2的幂） |
| **maxBatchCount** | Integer | 8192 | 单批最大条数 |
| **maxBatchBytes** | Integer | 10485760 (10MB) | 单批最大字节 |
| **maxMessageAgeMs** | Long | 60000 | 最早消息年龄阈值（毫秒），1分钟 |
| **dropWhenQueueFull** | Boolean | false | 队列满时是否丢弃日志 |
| **multiProducer** | Boolean | false | 是否支持多生产者 |
| **maxRetries** | Integer | 3 | 最大重试次数 |
| **baseBackoffMs** | Long | 200 | 基础退避时间(毫秒) |
| **maxBackoffMs** | Long | 10000 | 最大退避时间(毫秒) |
| **maxUploadSizeMb** | Integer | 10 | 单个上传文件最大大小（MB），同时控制分片阈值和分片大小 |

#### 批处理优化参数

| 参数名 | 类型 | 默认值 | 说明 |
|--------|------|--------|------|
| **enableCompression** | Boolean | true | 是否启用数据压缩 |
| **compressionThreshold** | Integer | 1024 (1KB) | 启用压缩的数据大小阈值 |
| **enableSharding** | Boolean | true | 是否启用数据分片处理 |

#### 配置优先级

系统支持多种配置源，按以下优先级顺序读取配置（从高到低）：
1. JVM系统属性（支持两种命名风格）
   - `-Dlogx.oss.storage.region=us` （点号格式）
   - `-DLOGX_OSS_STORAGE_REGION=us` （大写下划线格式）
2. 环境变量（只支持大写下划线格式）
   - `LOGX_OSS_STORAGE_REGION=us`
3. 配置文件属性 (logx.properties中的logx.oss.storage.region=ap-guangzhou)
4. 代码默认值

**命名风格兼容性**：
- **JVM系统属性**：同时支持点号格式和大写下划线格式
- **环境变量**：只支持大写下划线格式（因为大多数shell不支持点号作为环境变量名）

#### 云服务商端点示例

```bash
# 阿里云OSS
https://oss-cn-hangzhou.aliyuncs.com    # 杭州
https://oss-cn-beijing.aliyuncs.com     # 北京
https://oss-cn-shanghai.aliyuncs.com    # 上海

# AWS S3
https://s3.us-east-1.amazonaws.com      # 美东
https://s3.eu-west-1.amazonaws.com      # 欧洲

# 腾讯云COS
https://cos.ap-beijing.myqcloud.com     # 北京
https://cos.ap-shanghai.myqcloud.com    # 上海

# MinIO (自建)
http://localhost:9000                    # 本地MinIO
```

### 环境变量配置或配置文件

支持以下两种配置方式（按优先级）：

#### 方式1：环境变量配置（推荐用于敏感信息）

**环境变量命名规则**：
将配置键转换为环境变量格式，转换规则如下：
1. 处理驼峰命名：在小写字母后紧跟大写字母的位置插入下划线
2. 全部转大写
3. 点号替换为下划线

示例：
- `logx.oss.storage.endpoint` → `LOGX_OSS_STORAGE_ENDPOINT`
- `logx.oss.storage.accessKeyId` → `LOGX_OSS_STORAGE_ACCESS_KEY_ID`（驼峰转换）
- `logx.oss.engine.batch.count` → `LOGX_OSS_ENGINE_BATCH_COUNT`
- `logx.oss.storage.region` → `LOGX_OSS_STORAGE_REGION`
- `logx.oss.enabled` → `LOGX_OSS_ENABLED`

```bash
# 设置存储配置环境变量
export LOGX_OSS_STORAGE_OSS_TYPE="SF_S3"
export LOGX_OSS_STORAGE_ACCESS_KEY_ID="your-access-key-id"
export LOGX_OSS_STORAGE_ACCESS_KEY_SECRET="your-access-key-secret"
export LOGX_OSS_STORAGE_BUCKET="your-bucket-name"
export LOGX_OSS_STORAGE_ENDPOINT="https://oss-cn-hangzhou.aliyuncs.com"
export LOGX_OSS_STORAGE_REGION="cn-hangzhou"
export LOGX_OSS_STORAGE_KEY_PREFIX="logx/"
export LOGX_OSS_ENABLED="true"

# 引擎配置（可选）
export LOGX_OSS_ENGINE_BATCH_COUNT="8192"
export LOGX_OSS_ENGINE_BATCH_MAX_AGE_MS="60000"
```

#### 方式2：logx.properties配置文件

配置文件加载优先级（针对默认配置文件`logx.properties`）：
1. `/app/deploy/conf/logx.properties` - 生产环境配置目录（最高优先级）
2. `classpath:logx.properties` - 类路径下的配置文件
3. `./logx.properties` - 当前目录下的配置文件

**配置示例（logx.properties）**：

```properties
# 存储配置
logx.oss.storage.ossType=SF_S3
logx.oss.storage.endpoint=https://oss-cn-hangzhou.aliyuncs.com
logx.oss.storage.accessKeyId=your-access-key-id
logx.oss.storage.accessKeySecret=your-access-key-secret
logx.oss.storage.bucket=your-bucket-name
logx.oss.storage.region=cn-hangzhou
logx.oss.storage.keyPrefix=logx/

# 启用开关配置
logx.oss.enabled=true

# 引擎配置（可选）
logx.oss.engine.batch.count=8192
logx.oss.engine.batch.maxAgeMs=60000
logx.oss.engine.queue.capacity=524288
logx.oss.engine.retry.maxRetries=3
```

**配置文件位置选择**：
- **开发环境**：使用`src/main/resources/logx.properties`（打包到classpath）
- **生产环境**：使用`/app/deploy/conf/logx.properties`（便于运维管理，无需重新打包）

**注意事项**：
- 环境变量优先级高于配置文件
- 敏感信息（如accessKeyId、accessKeySecret）建议使用环境变量
- 非敏感配置可使用配置文件，便于管理

### Java代码示例

```java
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LogExample {
    private static final Logger logger = LoggerFactory.getLogger(LogExample.class);

    public void doSomething() {
        logger.info("开始处理业务逻辑");
        logger.warn("这是一个警告信息");
        logger.error("发生了错误", new RuntimeException("示例异常"));
    }
}
```

## 项目结构

本项目采用单仓库多模块（Monorepo）架构，统一管理所有组件，遵循分层抽象架构和统一包命名原则：

```
logx-oss-appender/                     # 主仓库
├── .bmad-core/                   # BMAD项目管理配置
├── docs/                         # 项目文档
│   ├── architecture.md          # 架构文档
│   ├── prd.md                   # 产品需求文档
│   ├── developer-guide.md       # 开发者指南
│   └── git-management.md        # Git管理指南
├── logx-producer/              # 核心处理引擎
├── logx-s3-adapter/             # S3兼容存储适配器（支持AWS S3、阿里云OSS、MinIO、SF OSS等）
├── log4j-oss-appender/          # Log4j集成模块
├── log4j2-oss-appender/         # Log4j2集成模块
├── logback-oss-appender/        # Logback集成模块
├── all-in-one/                  # All-in-One集成包（Fat JAR）
│   ├── s3-log4j-oss-appender/   # Log4j 1.x All-in-One包
│   ├── s3-log4j2-oss-appender/  # Log4j2 All-in-One包
│   └── s3-logback-oss-appender/ # Logback All-in-One包
└── pom.xml                      # 父POM文件
```

### 模块组件

各模块功能清晰分工，构成完整的日志上传解决方案，遵循正确的依赖结构：

```
logx-producer (核心)
    ↓
log4j-oss-appender
log4j2-oss-appender
logback-oss-appender
```

三个适配器都直接依赖于核心模块，彼此之间没有依赖关系。

| 模块名称 | 功能描述 | 依赖关系 |
|---------|---------|----------|
| **logx-producer** | 核心处理引擎，提供队列管理、异步处理、存储接口抽象，包含AsyncEngine异步引擎和EnhancedDisruptorBatchingQueue一体化批处理队列 | 基础模块，无依赖 |
| **logx-s3-adapter** | S3兼容存储适配器，支持AWS S3、阿里云OSS、腾讯云COS、MinIO、SF OSS等所有S3兼容存储服务 | 依赖logx-producer |
| **log4j-oss-appender** | Log4j 1.x框架适配器，实现OSSAppender | 依赖logx-producer |
| **log4j2-oss-appender** | Log4j2框架适配器，支持插件配置 | 依赖logx-producer |
| **logback-oss-appender** | Logback框架适配器，支持Spring Boot | 依赖logx-producer |
| **all-in-one系列** | All-in-One集成包（Fat JAR），包含框架适配器 + S3适配器 + 所有传递依赖，开箱即用 | 依赖相应的框架适配器和logx-s3-adapter |

### 项目管理

本项目采用统一的Git工作流管理，详细说明请参考：
- [Git管理指南](docs/git-management.md) - 分支策略、版本发布、协作流程

## 技术栈

- **语言**: Java 8+
- **构建工具**: Maven 3.9.6
- **核心依赖**: LMAX Disruptor 3.4.4
- **云存储**: AWS SDK 2.28.16
- **测试**: JUnit 5, Mockito, AssertJ

详细技术栈信息请参考 [技术栈文档](docs/architecture/tech-stack.md)。

## 高级配置

### 性能优化建议

通过 `logx.properties` 文件或环境变量进行性能调优：

**logx.properties 性能优化配置**:
```properties
# 引擎与重试配置（可选，按需调整）
logx.oss.engine.batch.count=5000        # 增大批量大小
logx.oss.engine.batch.maxAgeMs=10000    # 降低消息年龄阈值，更快触发批处理
logx.oss.engine.queue.capacity=131072   # 增大队列大小（必须是2的幂）
logx.oss.engine.retry.maxRetries=3      # 设置重试次数
logx.oss.engine.retry.baseBackoffMs=500 # 基础退避时间
logx.oss.engine.retry.maxBackoffMs=5000 # 最大退避时间
```

**环境变量性能配置**:
```bash
# 性能调优参数
export LOGX_OSS_ENGINE_BATCH_COUNT="5000"
export LOGX_OSS_ENGINE_BATCH_MAX_AGE_MS="10000"
export LOGX_OSS_ENGINE_QUEUE_CAPACITY="131072"
```

### 生产环境最佳实践

#### 1. 安全配置
- ✅ 使用环境变量存储敏感信息
- ✅ 配置最小权限的IAM策略
- ✅ 启用OSS访问日志审计
- ✅ 定期轮换访问密钥

#### 2. 性能优化
- ✅ 根据日志量调整`logx.oss.engine.batch.count`和`logx.oss.engine.batch.maxAgeMs`
- ✅ 使用异步Logger减少应用延迟
- ✅ 启用压缩（`logx.oss.engine.enableCompression=true`）节省存储和带宽成本
- ✅ 合理设置文件大小（`logx.oss.engine.maxUploadSizeMb`）避免小文件问题

#### 3. 监控告警
- ✅ 监控OSS上传成功率
- ✅ 设置存储用量告警
- ✅ 监控应用日志队列深度
- ✅ 配置网络异常重试机制

#### 4. 成本控制
- ✅ 设置日志生命周期策略
- ✅ 配置冷存储转换规则
- ✅ 定期清理过期日志文件
- ✅ 监控存储和流量费用

### 故障排查

#### 常见问题

**问题1: 上传失败**
```bash
# 检查网络连接
curl -I https://oss-cn-hangzhou.aliyuncs.com

# 验证密钥权限
```

**问题2: 性能问题**
通过调整配置参数优化性能：
```properties
# 调整批量参数
logx.oss.engine.batch.count=1000
logx.oss.engine.batch.maxAgeMs=30000
```

**问题3: 内存占用过高**
通过调整配置参数降低内存使用：
```properties
# 减少队列大小和上传文件大小
logx.oss.engine.queue.capacity=65536
logx.oss.engine.maxUploadSizeMb=5
```

### 集成示例

#### Spring Boot集成

```yaml
# application.yml
logging:
  config: classpath:logback-spring.xml

# 环境变量
OSS_ACCESS_KEY_ID: ${LOGX_OSS_STORAGE_ACCESS_KEY_ID}
OSS_ACCESS_KEY_SECRET: ${LOGX_OSS_STORAGE_ACCESS_KEY_SECRET}
OSS_BUCKET: ${LOGX_OSS_STORAGE_BUCKET:app-logs}
LOGX_OSS_STORAGE_ENDPOINT: ${LOGX_OSS_STORAGE_ENDPOINT:https://oss-cn-hangzhou.aliyuncs.com}
LOGX_OSS_STORAGE_REGION: ${LOGX_OSS_STORAGE_REGION:cn-hangzhou}
```

#### Docker部署

```dockerfile
# Dockerfile
FROM openjdk:8-jre-alpine
COPY app.jar /app.jar

# 设置环境变量
ENV LOGX_OSS_STORAGE_ACCESS_KEY_ID=""
ENV LOGX_OSS_STORAGE_ACCESS_KEY_SECRET=""
ENV LOGX_OSS_STORAGE_BUCKET="app-logs"
ENV LOGX_OSS_STORAGE_ENDPOINT="https://oss-cn-hangzhou.aliyuncs.com"
ENV LOGX_OSS_STORAGE_REGION="cn-hangzhou"

ENTRYPOINT ["java", "-jar", "/app.jar"]
```

## 项目路线图与决策

### 当前版本 (v1.0.0 MVP)

当前MVP版本专注于核心功能，确保日志上传的**高性能**和**高可靠性**：

**✅ 已实现功能**：
- **高性能异步队列（LMAX Disruptor，目标100,000条日志/秒）**
- **日志零丢失保证（在高吞吐量负载下确保数据完整性）**
- **队列内存控制（< 512MB，内存高效使用）**
- 智能批处理优化（三触发条件：消息数、字节数、消息年龄）
- GZIP压缩（90%+压缩率）
- 数据分片处理（自动分片大文件）
- 失败重试机制（指数退避，最多5次）
- 兜底文件机制（网络异常时本地缓存）
- 优雅关闭保护（30秒超时保护）
- 多框架支持（Log4j、Log4j2、Logback）
- 多云支持（AWS S3、阿里云OSS、MinIO等）

### 核心性能指标

根据架构文档调整后的性能要求：

| 指标 | 目标值 | 说明 |
|------|--------|------|
| 吞吐量 | 100,000条日志/秒 | 高并发处理能力 |
| 日志无丢失 | 零丢失率 | 在高吞吐量负载下确保数据完整性 |
| 队列内存占用 | < 512MB | 内存高效使用，避免OOM |

### 性能验证

- **吞吐量测试**: 兼容性测试中包含10万条日志处理能力验证
- **无丢失率测试**: 高负载下验证日志完整性（需对比MinIO上传文件数量）
- **内存控制测试**: 多场景下监控队列内存峰值使用情况
- **高并发测试**: 20线程并发处理，每线程5000条日志

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

### 默认配置说明

项目使用以下重要默认值（符合PRD要求）：

| 配置项 | 默认值 | 说明 | 决策记录 |
|--------|--------|------|----------|
| region | ap-guangzhou | 存储区域 | [ADR-003](docs/DECISIONS.md#adr-003-默认region值使用ap-guangzhou) |
| maxBatchCount | 8192 | 批处理大小 | 性能测试验证 |
| maxBatchBytes | 10MB | 批处理字节数 | 性能测试验证 |
| maxMessageAgeMs | 60000 (1分钟) | 消息年龄阈值 | 平衡延迟和吞吐 |

## 文档

- [架构设计文档](docs/architecture.md) - 详细的技术架构说明
- [产品需求文档](docs/prd.md) - 项目需求和Epic定义
- [项目决策记录](docs/DECISIONS.md) - 架构和功能决策说明（新增）
- [开发者指南](docs/developer-guide.md) - 开发环境设置和贡献指南
- [Git管理指南](docs/git-management.md) - 分支策略、版本发布、协作流程

## 兼容性测试

项目包含完整的集成和兼容性测试套件，独立于主项目构建，确保各种使用场景下的稳定性。

### 测试模块说明

- **[spring-boot-test](compatibility-tests/spring-boot-test/)** - Spring Boot框架集成测试
- **[spring-mvc-test](compatibility-tests/spring-mvc-test/)** - Spring MVC框架集成测试
- **[jsp-servlet-test](compatibility-tests/jsp-servlet-test/)** - JSP/Servlet传统应用集成测试
- **[multi-framework-test](compatibility-tests/multi-framework-test/)** - 多日志框架共存兼容性测试
  - 支持 Logback、Log4j2、Log4j 1.x (1.2.17) 多框架并存
  - 验证框架间配置隔离和资源竞争处理
- **[config-consistency-test](compatibility-tests/config-consistency-test/)** - 配置一致性验证工具
  - 使用 Jackson YAML (2.15.3) 解析配置文件
  - 验证各框架配置参数的一致性

### 运行兼容性测试

```bash
# 进入测试目录
cd compatibility-tests

# 编译所有测试模块
mvn clean compile

# 运行所有兼容性测试（需要MinIO环境）
mvn clean test -Pcompatibility-tests

# 运行特定测试模块
mvn clean test -pl spring-boot-test
mvn clean test -pl multi-framework-test
```

### 测试环境要求

- Java 8+
- Maven 3.6+
- MinIO 环境（按各测试模块README配置）

详细说明请参考 [兼容性测试文档](compatibility-tests/README.md)。

## 开发

### 开发环境设置

```bash
# 1. 克隆仓库
git clone --recursive https://github.com/logx-oss-appender/logx-oss-appender.git

# 2. 完整构建
mvn clean install -DskipTests

# 3. 运行测试
mvn test

# 4. 代码质量检查
mvn spotbugs:check formatter:validate

# 5. 查看构建产物
find . -name "*.jar" -path "*/target/*" -exec ls -la {} \;
```

### 模块开发

```bash
# 构建核心模块
mvn clean install -pl logx-producer,logx-s3-adapter,log4j-oss-appender,log4j2-oss-appender,logback-oss-appender -DskipTests

# 测试特定模块
mvn test -pl logx-producer

# 检查模块依赖
mvn dependency:tree -pl logback-oss-appender

# 统一更新版本号
mvn versions:set -DnewVersion=1.0.0-SNAPSHOT
mvn versions:commit
```


详细开发指南请参考 [开发者指南](docs/developer-guide.md) 和 [编码标准](docs/architecture/coding-standards.md)。

## 许可证

本项目采用 [Apache License 2.0](LICENSE) 许可证。

## 贡献

欢迎贡献代码！请查看 [开发者指南](docs/developer-guide.md) 了解详细的贡献流程。

## 已知问题

- Epic 2：`AsyncEngineIntegrationTest.shouldMeetLatencyTarget` 在当前容器/CI 环境下可能因性能抖动导致断言失败，不影响 Epic 1 交付与评审。复现步骤、初步分析与修复建议见：`docs/issues/Epic2-AsyncEngineIntegrationTest-failure.md`。

## 支持

如果遇到问题或有建议，请：

1. 查看 [文档](docs/)
2. 搜索 [Issues](https://github.com/logx-oss-appender/logx-oss-appender/issues)
3. 创建新的 Issue

---

🚀 **LogX OSS Appender - 让日志上传更简单、更高效！**
