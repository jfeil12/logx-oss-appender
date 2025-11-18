# S3StorageInterface API 文档

## 概述

`S3StorageInterface` 是LogX OSS Appender项目的核心存储抽象接口，基于S3标准API设计，支持AWS S3、阿里云OSS、MinIO等S3兼容存储服务的统一访问。

该接口遵循异步操作模式，所有方法都返回`CompletableFuture`，确保非阻塞的高性能日志上传能力。

## 接口定义

```java
public interface S3StorageInterface {
    CompletableFuture<Void> putObject(String key, byte[] data);
    CompletableFuture<Void> putObjects(Map<String, byte[]> objects);
    CompletableFuture<Boolean> healthCheck();
    String getBackendType();
    String getBucketName();
}
```

## 方法详解

### putObject(String key, byte[] data)

异步上传单个对象到S3存储。

**参数：**
- `key`: 存储对象的键（路径），不能为空
- `data`: 要上传的数据内容，不能为空

**返回值：**
- `CompletableFuture<Void>`: 异步上传操作的结果

**异常：**
- `IllegalArgumentException`: 如果key为空或data为空

**使用示例：**
```java
S3StorageInterface storage = new S3StorageAdapter(...);
byte[] logData = "Hello World".getBytes(StandardCharsets.UTF_8);

storage.putObject("logx/2024/01/01/app.log", logData)
    .thenRun(() -> System.out.println("上传成功"))
    .exceptionally(throwable -> {
        System.err.println("上传失败: " + throwable.getMessage());
        return null;
    });
```

### putObjects(Map<String, byte[]> objects)

异步批量上传多个对象到S3存储。

**参数：**
- `objects`: 要上传的对象映射，key为存储路径，value为数据内容

**返回值：**
- `CompletableFuture<Void>`: 异步批量上传操作的结果

**使用示例：**
```java
Map<String, byte[]> logFiles = new HashMap<>();
logFiles.put("logx/2024/01/01/app-1.log", data1);
logFiles.put("logx/2024/01/01/app-2.log", data2);

storage.putObjects(logFiles)
    .thenRun(() -> System.out.println("批量上传完成"))
    .exceptionally(throwable -> {
        System.err.println("批量上传失败: " + throwable.getMessage());
        return null;
    });
```

### healthCheck()

检查存储连接的健康状态。

**返回值：**
- `CompletableFuture<Boolean>`: 健康检查结果，true表示连接正常

**使用示例：**
```java
storage.healthCheck()
    .thenAccept(healthy -> {
        if (healthy) {
            System.out.println("存储服务连接正常");
        } else {
            System.out.println("存储服务连接异常");
        }
    });
```

### getBackendType()

获取存储后端的类型标识。

**返回值：**
- `String`: 存储后端类型（如"AWS_S3", "ALIYUN_OSS", "MINIO"等）

### getBucketName()

获取当前使用的存储桶名称。

**返回值：**
- `String`: 存储桶名称

## 配置示例

### AWS S3配置

```java
AwsS3Config config = AwsS3Config.builder()
    .endpoint("https://s3.us.amazonaws.com")
    .region("US")
    .accessKeyId("AKIA...")
    .accessKeySecret("your-secret-key")
    .bucket("my-log-bucket")
    .build();

S3StorageInterface storage = new S3StorageAdapter(
    config.getRegion(),
    config.getAccessKeyId(),
    config.getAccessKeySecret(),
    config.getBucket()
);
```

### SF OSS配置

```java
StorageConfig config = StorageConfig.builder()
    .endpoint("https://sf-oss-cn-north-1.sf-oss.com")
    .region("cn-north-1")
    .accessKeyId("SFIA...")
    .accessKeySecret("your-secret-key")
    .bucket("my-log-bucket")
    .build();

S3StorageInterface storage = new SfOssStorageAdapter(config);
```

### 从环境变量配置

```java
AwsS3Config config = AwsS3Config.fromEnvironment();
```

**环境变量：**
```bash
export AWS_ACCESS_KEY_ID="your-access-key-id"
export AWS_SECRET_ACCESS_KEY="your-secret-access-key"
export AWS_DEFAULT_REGION="US"
export AWS_S3_BUCKET="my-log-bucket"
```

对于SF OSS：
```java
StorageConfig config = StorageConfig.fromEnvironment();
```

**环境变量：**
```bash
export SF_OSS_ACCESS_KEY_ID="your-access-key-id"
export SF_OSS_SECRET_ACCESS_KEY="your-secret-access-key"
export LOG_SF_OSS_DEFAULT_REGION="cn-north-1"
export SF_OSS_BUCKET="my-log-bucket"
```

## 支持的存储后端

| 后端类型 | 标识符 | 说明 |
|---------|--------|------|
| AWS S3 | AWS_S3 | 原生AWS S3服务 |
| 阿里云OSS | ALIYUN_OSS | 阿里云对象存储 |
| MinIO | MINIO | 开源S3兼容存储 |
| 腾讯云COS | TENCENT_COS | 腾讯云对象存储 |
| 华为云OBS | HUAWEI_OBS | 华为云对象存储 |
| SF OSS | SF_OSS | SF对象存储服务 |
| 通用S3 | GENERIC_S3 | 其他S3兼容服务 |

## 错误处理

所有存储操作可能抛出`StorageException`，包含以下错误类型：

- `NETWORK_ERROR`: 网络连接错误
- `AUTHENTICATION_ERROR`: 认证失败错误
- `SERVER_ERROR`: 服务器内部错误
- `CLIENT_ERROR`: 客户端请求错误
- `CONFIGURATION_ERROR`: 配置错误
- `UNKNOWN_ERROR`: 未知错误

**错误处理示例：**
```java
storage.putObject("test.log", data)
    .exceptionally(throwable -> {
        if (throwable.getCause() instanceof StorageException) {
            StorageException ex = (StorageException) throwable.getCause();
            if (ex.isRetryable()) {
                // 可重试的错误，可以重新尝试
                System.out.println("可重试错误: " + ex.getMessage());
            } else {
                // 不可重试的错误，如认证失败
                System.err.println("致命错误: " + ex.getMessage());
            }
        }
        return null;
    });
```

## 最佳实践

### 1. 资源管理

实现了`AutoCloseable`的适配器应该使用try-with-resources：

```java
try (S3StorageAdapter storage = new S3StorageAdapter(...)) {
    storage.putObject("test.log", data).join();
}
```

### 2. 异步操作

避免阻塞主线程，使用异步回调：

```java
// 好的做法
storage.putObject("test.log", data)
    .thenRun(() -> logger.info("上传完成"));

// 避免的做法
storage.putObject("test.log", data).join(); // 阻塞等待
```

### 3. 批量操作

对于多个文件，优先使用批量上传：

```java
// 优化的批量上传
Map<String, byte[]> files = new HashMap<>();
files.put("file1.log", data1);
files.put("file2.log", data2);
storage.putObjects(files);

// 避免的单个上传循环
for (String file : files.keySet()) {
    storage.putObject(file, files.get(file)); // 效率较低
}
```

### 4. 健康检查

在关键路径前进行健康检查：

```java
storage.healthCheck()
    .thenCompose(healthy -> {
        if (healthy) {
            return storage.putObject("test.log", data);
        } else {
            return CompletableFuture.failedFuture(
                new RuntimeException("存储服务不可用"));
        }
    });
```

## 性能考虑

1. **大文件处理**: >5MB的文件自动使用multipart upload
2. **并发控制**: 实现内部连接池，默认最大50个并发连接
3. **重试机制**: 自动重试临时性错误，最多3次
4. **超时设置**: 合理设置连接和读取超时时间

## 版本兼容性

- 最低Java版本：Java 8+
- 线程安全：所有实现都是线程安全的
- API稳定性：遵循语义化版本控制，向后兼容