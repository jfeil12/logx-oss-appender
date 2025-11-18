# Logback + S3 非Maven方式对接文档

## 概述

本文档介绍如何在不使用Maven/Gradle构建工具的情况下，通过All-in-One Fat JAR方式将Logback日志框架与S3兼容对象存储服务（如AWS S3、阿里云OSS、腾讯云COS、MinIO等）进行集成。

LogX OSS Appender提供开箱即用的All-in-One集成包，每个包约25MB，包含所有必要的依赖项，可直接用于非Maven项目。

## 适用场景

- 传统Java项目（不使用Maven/Gradle）
- 只能通过JAR包依赖管理的项目
- 需要快速集成日志上传功能的项目

## 准备工作

### 1. 获取All-in-One JAR包

从项目的`all-in-one/s3-logback-oss-appender/target/`目录获取预编译的Fat JAR包：

```
s3-logback-oss-appender-1.0.0-SNAPSHOT.jar  (~25MB)
```

### 2. 系统要求

- Java 8 或更高版本
- Logback 1.2.13 或更高版本（已包含在All-in-One包中）

## 集成步骤

### 1. 添加JAR到项目

将` s3-logback-oss-appender-1.0.0-SNAPSHOT.jar`文件添加到项目的classpath中：

#### 方式一：命令行方式
```bash
java -cp s3-logback-oss-appender-1.0.0-SNAPSHOT.jar:your-app.jar YourMainClass
```

#### 方式二：IDE配置
在IDE中将JAR文件添加到项目的依赖库中。

### 2. 配置Logback

创建或修改`logback.xml`配置文件，添加LogX OSS Appender配置：

```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <!-- OSS Appender配置 -->
    <appender name="OSS_APPENDER" class="org.logx.logback.LogbackOSSAppender">
        <!-- 编码器配置 -->
        <encoder class="ch.qos.logback.classic.encoder.PatternLayoutEncoder">
            <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n</pattern>
        </encoder>
    </appender>

    <!-- 根日志配置 -->
    <root level="INFO">
        <appender-ref ref="OSS_APPENDER"/>
    </root>
</configuration>
```

### 3. 配置存储服务参数

推荐使用环境变量方式配置敏感信息：

```bash
# 存储服务配置
export LOGX_OSS_STORAGE_OSS_TYPE="SF_S3"
export LOGX_OSS_STORAGE_ENDPOINT="https://oss-cn-hangzhou.aliyuncs.com"
export LOGX_OSS_STORAGE_ACCESS_KEY_ID="your-access-key-id"
export LOGX_OSS_STORAGE_ACCESS_KEY_SECRET="your-access-key-secret"
export LOGX_OSS_STORAGE_BUCKET="your-bucket-name"
export LOGX_OSS_STORAGE_REGION="cn-hangzhou"
export LOGX_OSS_STORAGE_KEY_PREFIX="logx/"

# 引擎配置（可选）
export LOGX_OSS_ENGINE_BATCH_COUNT="8192"
export LOGX_OSS_ENGINE_BATCH_MAX_AGE_MS="60000"
```

### 4. 使用logx.properties配置文件（可选）

除了环境变量，您也可以使用`logx.properties`配置文件来配置参数：

#### 配置文件位置
系统按以下优先级查找配置文件：
1. `/app/deploy/conf/logx.properties` - 生产环境配置目录（最高优先级）
2. `classpath:logx.properties` - 类路径下的配置文件
3. `./logx.properties` - 当前目录下的配置文件

#### 配置示例
创建`logx.properties`文件：

```properties
# 存储配置
logx.oss.storage.ossType=SF_S3
logx.oss.storage.endpoint=https://oss-cn-hangzhou.aliyuncs.com
logx.oss.storage.accessKeyId=your-access-key-id
logx.oss.storage.accessKeySecret=your-access-key-secret
logx.oss.storage.bucket=your-bucket-name
logx.oss.storage.region=cn-hangzhou
logx.oss.storage.keyPrefix=logx/

# 启用开关配置（需要重启服务生效）
logx.oss.enabled=true

# 引擎配置（可选）
logx.oss.engine.batch.count=8192
logx.oss.engine.batch.maxAgeMs=60000
logx.oss.engine.queue.capacity=524288
logx.oss.engine.retry.maxRetries=3
```

### 5. 编写日志代码

在Java代码中使用标准的SLF4J API进行日志记录：

```java
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LogExample {
    private static final Logger logger = LoggerFactory.getLogger(LogExample.class);

    public static void main(String[] args) {
        logger.info("应用启动");
        logger.warn("这是一个警告信息");
        logger.error("发生了错误", new RuntimeException("示例异常"));
        
        // 模拟业务日志
        for (int i = 0; i < 100; i++) {
            logger.info("处理业务数据 - 记录 {}", i);
        }
    }
}
```

## 配置参数说明

### 必需参数

| 参数名 | 类型 | 说明 | 示例 |
|--------|------|------|------|
| **endpoint** | String | 对象存储服务的访问端点 | `https://oss-cn-hangzhou.aliyuncs.com` |
| **accessKeyId** | String | 访问密钥ID | `${LOGX_OSS_STORAGE_ACCESS_KEY_ID}` |
| **accessKeySecret** | String | 访问密钥Secret | `${LOGX_OSS_STORAGE_ACCESS_KEY_SECRET}` |
| **bucket** | String | 存储桶名称 | `my-log-bucket` |

### 可选参数

| 参数名 | 类型 | 默认值 | 说明 |
|--------|------|--------|------|
| **enabled** | Boolean | true | 是否启用appender，为false时appender不会处理任何日志（需要重启服务生效） |
| **region** | String | US | 存储区域 |
| **keyPrefix** | String | logx/ | 对象存储中的文件路径前缀 |
| **ossType** | String | SF_S3 | 存储后端类型，支持SF_S3、S3等 |
| **pathStyleAccess** | Boolean | 根据云服务商类型自动识别 | 是否使用路径风格访问 |

### 引擎配置参数

| 参数名 | 类型 | 默认值 | 说明 |
|--------|------|--------|------|
| **maxBatchCount** | Integer | 8192 | 单批最大条数 |
| **maxBatchBytes** | Integer | 10485760 (10MB) | 单批最大字节 |
| **maxMessageAgeMs** | Long | 60000 | 最早消息年龄阈值（毫秒），1分钟 |
| **queueCapacity** | Integer | 524288 | 内存队列大小（必须是2的幂） |
| **dropWhenQueueFull** | Boolean | false | 队列满时是否丢弃日志 |
| **maxRetries** | Integer | 3 | 最大重试次数 |
| **baseBackoffMs** | Long | 200 | 基础退避时间(毫秒) |
| **maxBackoffMs** | Long | 10000 | 最大退避时间(毫秒) |
| **maxUploadSizeMb** | Integer | 10 | 单个上传文件最大大小（MB） |

### 批处理优化参数

| 参数名 | 类型 | 默认值 | 说明 |
|--------|------|--------|------|
| **enableCompression** | Boolean | true | 是否启用数据压缩 |
| **compressionThreshold** | Integer | 1024 (1KB) | 启用压缩的数据大小阈值 |
| **enableSharding** | Boolean | true | 是否启用数据分片处理 |

## 配置优先级

系统支持多种配置源，按以下优先级顺序读取配置：

1. **JVM系统属性**
   - `-Dlogx.oss.storage.region=us` （点号格式，优先）
   - `-DLOGX_OSS_STORAGE_REGION=us` （大写下划线格式）

2. **环境变量**（推荐用于敏感信息）
   - `LOGX_OSS_STORAGE_REGION=us`

3. **XML配置文件中设置的字段值**

4. **代码默认值**

## 云服务商端点示例

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

## 环境变量配置

### 环境变量命名规则

将配置键转换为环境变量格式，转换规则如下：
1. 处理驼峰命名：在小写字母后紧跟大写字母的位置插入下划线
2. 全部转大写
3. 点号替换为下划线

示例：
- `logx.oss.storage.endpoint` → `LOGX_OSS_STORAGE_ENDPOINT`
- `logx.oss.storage.accessKeyId` → `LOGX_OSS_STORAGE_ACCESS_KEY_ID`（驼峰转换）
- `logx.oss.engine.batch.count` → `LOGX_OSS_ENGINE_BATCH_COUNT`

### 常用环境变量配置

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

## 高级配置示例

### 高性能配置

```xml
<configuration>
    <appender name="OSS_APPENDER" class="org.logx.logback.LogbackOSSAppender">
        <encoder class="ch.qos.logback.classic.encoder.PatternLayoutEncoder">
            <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n</pattern>
        </encoder>
        
        <!-- 性能调优参数 -->
        <maxBatchCount>5000</maxBatchCount>
        <maxMessageAgeMs>10000</maxMessageAgeMs>
        <queueCapacity>131072</queueCapacity>
        <maxRetries>3</maxRetries>
        <baseBackoffMs>500</baseBackoffMs>
        <maxBackoffMs>5000</maxBackoffMs>
    </appender>

    <root level="INFO">
        <appender-ref ref="OSS_APPENDER"/>
    </root>
</configuration>
```

## 生产环境最佳实践

### 1. 安全配置
- ✅ 使用环境变量存储敏感信息
- ✅ 配置最小权限的IAM策略
- ✅ 启用OSS访问日志审计
- ✅ 定期轮换访问密钥

### 2. 性能优化
- ✅ 根据日志量调整`maxBatchCount`和`maxMessageAgeMs`
- ✅ 启用压缩节省存储和带宽成本
- ✅ 合理设置文件大小避免小文件问题

### 3. 监控告警
- ✅ 监控OSS上传成功率
- ✅ 设置存储用量告警
- ✅ 监控应用日志队列深度
- ✅ 配置网络异常重试机制

### 4. 成本控制
- ✅ 设置日志生命周期策略
- ✅ 配置冷存储转换规则
- ✅ 定期清理过期日志文件
- ✅ 监控存储和流量费用

## 故障排查

### 常见问题

**问题1: 上传失败**
```bash
# 检查网络连接
curl -I https://oss-cn-hangzhou.aliyuncs.com

# 验证密钥权限
ossutil ls oss://your-bucket-name --config-file ~/.ossutilconfig
```

**问题2: 性能问题**
```xml
<!-- 调整批量参数 -->
<maxBatchCount>1000</maxBatchCount>
<maxMessageAgeMs>30000</maxMessageAgeMs>
```

## 默认配置说明

| 配置项 | 默认值 | 说明 |
|--------|--------|------|
| region | US | 默认存储区域 |
| maxBatchCount | 8192 | 批处理大小 |
| maxBatchBytes | 10MB | 批处理字节数 |
| maxMessageAgeMs | 60000 (1分钟) | 消息年龄阈值 |
| queueCapacity | 524288 | 队列容量 |
| maxRetries | 3 | 最大重试次数 |
| keyPrefix | logx/ | 文件路径前缀 |

## 技术架构

All-in-One包包含以下核心组件：

1. **Logback适配器** - Logback框架集成
2. **核心处理引擎** - 基于LMAX Disruptor的高性能队列
3. **S3存储适配器** - 支持多种S3兼容存储服务
4. **所有依赖项** - 包括Logback、SLF4J、AWS SDK等

## 性能指标

- **吞吐量**: 100,000条日志/秒
- **队列内存占用**: < 512MB
- **日志无丢失**: 零丢失率（在高吞吐量负载下）
- **压缩率**: 90%+（启用GZIP压缩）

## 许可证

本项目采用 [Apache License 2.0](../LICENSE) 许可证。