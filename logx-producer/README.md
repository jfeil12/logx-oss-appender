# logx-producer

> **📘 完整文档**: 详细特性、配置说明、使用示例请参考 [项目主文档](../README.md)
> 本文档仅包含logx-producer核心模块特定说明

核心日志处理引擎，提供高性能异步队列、批处理和存储抽象接口。

## 模块架构

logx-producer作为核心模块，不直接依赖任何具体的云存储SDK。存储适配器通过Java SPI机制动态加载：

- **logx-s3-adapter** - S3兼容存储适配器（支持AWS S3、阿里云OSS、腾讯云COS、MinIO等）
- **logx-sf-oss-adapter** - SF OSS存储适配器

## 核心组件

### 队列引擎层（2025-10-05架构重构）
- `EnhancedDisruptorBatchingQueue` - 增强的Disruptor批处理队列（合并原DisruptorBatchingQueue和BatchProcessor）
- `ResourceProtectedThreadPool` - 资源保护线程池

### 存储抽象层
- `StorageService` - 统一存储服务接口
- `StorageConfig` - 存储配置管理
- `StorageServiceFactory` - 存储服务工厂（通过SPI加载适配器）

### 配置管理层
- `ConfigManager` - 配置管理器，支持多级配置优先级

### 错误处理层
- `UnifiedErrorHandler` - 统一错误处理器
- `ErrorContext` - 错误上下文

## 使用方式

完整的使用指南请参考 [主文档](../README.md)

### Maven依赖

```xml
<dependency>
  <groupId>org.logx</groupId>
  <artifactId>logx-producer</artifactId>
  <version>1.0.0-SNAPSHOT</version>
</dependency>

<!-- 需要额外引入具体的存储适配器模块 -->
<dependency>
  <groupId>org.logx</groupId>
  <artifactId>logx-s3-adapter</artifactId>  <!-- 或 logx-sf-oss-adapter -->
  <version>1.0.0-SNAPSHOT</version>
</dependency>
```

## 配置说明

详细配置参数和优先级说明请参考 [主文档配置参数章节](../README.md#配置参数说明)

### logx-producer特定说明

- 通过Java SPI机制自动加载存储适配器（无需手动配置）
- 支持运行时通过`ossType`参数切换存储服务
- 提供统一的`StorageService`接口抽象

## 许可证

Apache-2.0
