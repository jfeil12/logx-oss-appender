# Log4j OSS Appender

> **📘 完整文档**: 详细特性、配置说明、使用示例请参考 [项目主文档](../README.md)
> 本文档仅包含Log4j 1.x模块特定说明

Log4j 1.x框架的OSS Appender，用于将日志异步上传到对象存储服务。

## 快速开始

完整的快速开始指南请参考 [主文档快速开始章节](../README.md#log4j-1x--sf-oss快速开始)

### Maven依赖

```xml
<dependencies>
    <!-- Log4j 1.x适配器 -->
    <dependency>
        <groupId>org.logx</groupId>
        <artifactId>log4j-oss-appender</artifactId>
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

### Log4j特定配置

Log4j配置继承自`AppenderSkeleton`，支持所有标准Log4j配置选项。

## 许可证

Apache-2.0
