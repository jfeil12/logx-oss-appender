# Logback OSS Appender

> **📘 完整文档**: 详细特性、配置说明、使用示例请参考 [项目主文档](../README.md)
> 本文档仅包含Logback模块特定说明

Logback框架的OSS Appender，用于将日志异步上传到对象存储服务。

## 快速开始

完整的快速开始指南请参考 [主文档快速开始章节](../README.md#主要示例logback--sf-oss快速开始)

### Maven依赖

```xml
<dependencies>
    <!-- Logback适配器 -->
    <dependency>
        <groupId>org.logx</groupId>
        <artifactId>logback-oss-appender</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </dependency>

    <!-- 存储适配器（选择其一） -->
    <dependency>
        <groupId>org.logx</groupId>
        <artifactId>logx-s3-adapter</artifactId>  <!-- 或 logx-sf-oss-adapter -->
        <version>1.0.0-SNAPSHOT</version>
    </dependency>
</dependencies>
```

## 配置说明

详细配置参数请参考 [主文档配置参数章节](../README.md#配置参数说明)

### Logback特定配置

Logback配置继承自`AppenderBase<ILoggingEvent>`，支持所有标准Logback配置选项。

## 许可证

Apache-2.0