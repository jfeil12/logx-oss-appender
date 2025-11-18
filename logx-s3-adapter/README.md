# S3 适配器模块

> **📘 完整文档**: 详细特性、配置说明、使用示例请参考 [项目主文档](../README.md)
> 本文档仅包含logx-s3-adapter模块特定说明

S3 适配器模块为 LogX OSS Appender 提供对 AWS S3、阿里云OSS、腾讯云COS、MinIO 等 S3 兼容存储服务的支持。

## 架构变更说明 (2025-09-24)

根据最新的架构设计，S3 适配器的职责已简化：
- **只负责具体的上传实现**
- **不再处理数据分片逻辑**（由核心层统一处理）
- **不再提供putObjects方法，只提供putObject方法**

这种设计使得适配器更加简洁，同时将数据分片的控制权集中在核心层，确保所有存储后端的行为一致性。

## 核心组件

### S3StorageServiceAdapter
实现了 `StorageService` 接口，提供 S3 兼容存储服务的具体实现。基于 AWS SDK，支持所有 S3 兼容的存储服务。

## 使用方式

完整的使用指南请参考 [主文档](../README.md)

### Maven依赖

```xml
<dependency>
    <groupId>org.logx</groupId>
    <artifactId>logx-s3-adapter</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

## 配置说明

详细配置参数请参考 [主文档配置参数章节](../README.md#配置参数说明)

### S3适配器特定说明

- 通过Java SPI机制自动加载（配置文件：`META-INF/services/org.logx.storage.StorageService`）
- 支持的存储类型：AWS S3、阿里云OSS、腾讯云COS、华为云OBS、MinIO、通用S3兼容存储
- MinIO配置要求：`pathStyleAccess=true`，`enableSsl=false`（本地开发）

## 设计特点

1. **低耦合**：适配器只关注具体的上传实现，不处理数据分片等通用逻辑
2. **高内聚**：所有 S3 相关的实现都集中在该模块中
3. **易扩展**：通过实现统一的存储服务接口，可以轻松扩展支持其他存储后端
4. **简化实现**：移除了复杂的数据分片逻辑，只提供单对象上传方法
5. **兼容性强**：基于 AWS SDK，支持所有 S3 兼容的存储服务

## 许可证

Apache-2.0
