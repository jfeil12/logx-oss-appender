# 兜底机制配置文档

本文档详细介绍LogX OSS Appender的兜底机制配置参数和使用方法。

## 目录

- [概述](#概述)
- [配置参数](#配置参数)
- [配置方式](#配置方式)
- [使用示例](#使用示例)
- [最佳实践](#最佳实践)

## 概述

兜底机制是LogX OSS Appender提供的本地文件存储和定时重传功能，用于在网络异常或云存储服务不可用时确保日志数据的最终一致性。该机制通过以下组件实现：

1. **FallbackManager**: 负责兜底文件的存储管理
2. **FallbackUploaderTask**: 定时扫描和重传兜底文件
3. **FallbackFileCleaner**: 清理过期的兜底文件

## 配置参数

### 核心配置参数

| 参数名 | 类型 | 默认值 | 描述 |
|--------|------|--------|------|
| `fallbackPath` | String | "fallback" | 兜底文件存储路径，支持相对路径和绝对路径 |
| `fallbackRetentionDays` | Integer | 7 | 兜底文件保留天数，超过此天数的文件将被自动清理 |
| `fallbackScanIntervalSeconds` | Integer | 60 | 兜底文件扫描间隔（秒），定时任务检查和重传兜底文件的间隔 |
| `emergencyMemoryThresholdMb` | Integer | 512 | 紧急保护阈值（MB），当队列内存占用超过此值时，直接将新消息写入兜底文件 |

### 配置参数详细说明

#### fallbackPath
- **描述**: 指定兜底文件的存储路径
- **默认值**: "fallback"
- **说明**: 
  - 支持相对路径（相对于应用启动目录）和绝对路径
  - 如果路径不存在，系统会自动创建
  - 建议使用相对路径以便于部署和管理

#### fallbackRetentionDays
- **描述**: 指定兜底文件的保留天数
- **默认值**: 7
- **说明**: 
  - 超过指定天数的兜底文件将被自动清理
  - 设置为0表示不保留任何兜底文件（不推荐）
  - 建议根据磁盘空间和业务需求合理设置

#### fallbackScanIntervalSeconds
- **描述**: 指定兜底文件的扫描间隔
- **默认值**: 60
- **说明**: 
  - 定时任务检查和重传兜底文件的时间间隔（秒）
  - 值过小可能影响系统性能，值过大可能导致数据重传延迟
  - 建议根据网络状况和业务需求合理设置

#### emergencyMemoryThresholdMb
- **描述**: 指定紧急保护阈值（MB）
- **默认值**: 512
- **说明**: 
  - 当队列内存占用超过此值时，直接将新消息写入兜底文件
  - 此机制防止JVM OOM，是最后一道防线
  - 建议根据应用可用内存和日志量合理设置

## 配置方式

### 1. 属性文件配置

在`application.properties`或`log4j.properties`等配置文件中添加以下配置：

```properties
# 兜底机制配置（引擎配置）
logx.oss.engine.fallback.path=fallback
logx.oss.engine.fallback.retentionDays=7
logx.oss.engine.fallback.scanIntervalSeconds=60
logx.oss.engine.emergencyMemoryThresholdMb=512
```

### 2. 环境变量配置

```bash
export LOGX_OSS_ENGINE_FALLBACK_PATH="fallback"
export LOGX_OSS_ENGINE_FALLBACK_RETENTION_DAYS="7"
export LOGX_OSS_ENGINE_FALLBACK_SCAN_INTERVAL_SECONDS="60"
export LOGX_OSS_ENGINE_EMERGENCY_MEMORY_THRESHOLD_MB="512"
```

### 3. JVM系统属性配置

```bash
java -Dlogx.oss.engine.fallback.path=fallback \
     -Dlogx.oss.engine.fallback.retentionDays=7 \
     -Dlogx.oss.engine.fallback.scanIntervalSeconds=60 \
     -Dlogx.oss.engine.emergencyMemoryThresholdMb=512 \
     -jar your-application.jar
```

### 4. 代码配置（以Logback为例）

```java
import org.logx.config.AsyncEngineConfig;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.Logger;
import org.slf4j.LoggerFactory;

public class LogbackConfigExample {
    public static void configureFallback() {
        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        Logger logger = context.getLogger("ROOT");
        
        // 获取或创建AsyncEngineConfig
        AsyncEngineConfig config = AsyncEngineConfig.defaultConfig()
            .fallbackPath("my-fallback")  // 设置兜底路径
            .fallbackRetentionDays(14)    // 设置保留天数为14天
            .fallbackScanIntervalSeconds(30) // 设置扫描间隔为30秒
            .emergencyMemoryThresholdMb(1024); // 设置紧急内存阈值为1024MB
        
        // 应用配置到logger
        // 注意：具体的配置应用方式取决于框架适配器的实现
    }
}
```

## 使用示例

### 示例1: 基本配置

```properties
# logx.properties
# 存储配置
logx.oss.storage.endpoint=https://oss-cn-hangzhou.aliyuncs.com
logx.oss.storage.accessKeyId=your-access-key-id
logx.oss.storage.accessKeySecret=your-access-key-secret
logx.oss.storage.bucket=your-bucket-name
# 引擎配置 - 兜底机制
logx.oss.engine.fallback.path=fallback
logx.oss.engine.fallback.retentionDays=7
logx.oss.engine.fallback.scanIntervalSeconds=60
logx.oss.engine.emergencyMemoryThresholdMb=512
```

### 示例2: 自定义路径和保留策略

```properties
# log4j2.properties
# 存储配置
logx.oss.storage.endpoint=https://oss-cn-hangzhou.aliyuncs.com
logx.oss.storage.accessKeyId=your-access-key-id
logx.oss.storage.accessKeySecret=your-access-key-secret
logx.oss.storage.bucket=your-bucket-name
# 引擎配置 - 兜底机制（自定义路径和保留策略）
logx.oss.engine.fallback.path=/var/log/myapp/fallback
logx.oss.engine.fallback.retentionDays=30
logx.oss.engine.fallback.scanIntervalSeconds=120
logx.oss.engine.emergencyMemoryThresholdMb=1024
```

### 示例3: 开发环境配置

```properties
# development.properties
# 存储配置
logx.oss.storage.endpoint=https://oss-cn-hangzhou.aliyuncs.com
logx.oss.storage.accessKeyId=your-access-key-id
logx.oss.storage.accessKeySecret=your-access-key-secret
logx.oss.storage.bucket=your-bucket-name
# 引擎配置 - 兜底机制（开发环境配置）
logx.oss.engine.fallback.path=temp/fallback
logx.oss.engine.fallback.retentionDays=1
logx.oss.engine.fallback.scanIntervalSeconds=10
logx.oss.engine.emergencyMemoryThresholdMb=256
```

## 最佳实践

### 1. 路径配置建议

- **生产环境**: 使用绝对路径，确保路径具有适当的读写权限
- **开发环境**: 使用相对路径，便于项目迁移和部署
- **路径权限**: 确保应用具有对指定路径的读写权限

### 2. 保留天数设置

- **一般业务**: 建议设置为7-14天
- **重要业务**: 建议设置为30天或更长
- **存储受限**: 可适当减少天数，但不应少于3天

### 4. 扫描间隔优化

- **网络稳定**: 可设置较长间隔（60-300秒）
- **网络不稳定**: 建议设置较短间隔（30-60秒）
- **性能敏感**: 避免设置过短间隔，以免影响系统性能

### 5. 紧急内存保护阈值设置

- **内存充足**: 可设置较大值（1024MB或更高）
- **内存受限**: 建议设置较小值（256MB或更低）
- **一般应用**: 建议使用默认值（512MB）

### 6. 监控和告警

建议监控以下指标：
- 兜底文件数量和增长趋势
- 重传成功率
- 磁盘使用情况
- 清理任务执行情况
- 紧急内存保护触发次数

### 7. 故障排查

当遇到兜底机制相关问题时，可检查：
- 配置参数是否正确
- 指定路径是否存在且具有读写权限
- 磁盘空间是否充足
- 网络连接是否正常
- 紧急内存保护是否频繁触发