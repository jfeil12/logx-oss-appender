# Spring MVC兼容性测试应用

## 概述
此应用用于验证OSS Appender在Spring MVC环境中的兼容性和性能表现。

## 功能特性
1. 验证XML配置方式的兼容性
2. 测试程序化配置方式
3. 验证与Spring上下文的集成
4. 执行性能基准测试
5. 真实业务日志生成测试（电商业务场景）
6. 高并发访问测试

## 性能指标

根据架构文档调整后的性能要求：

### 核心性能指标
| 指标 | 目标值 | 说明 |
|------|--------|------|
| 吞吐量 | 10,000+条日志/秒 | 高并发处理能力 |
| 日志无丢失 | 零丢失率 | 在高吞吐量负载下确保数据完整性 |
| 队列内存占用 | < 512MB | 内存高效使用，避免OOM |

### 测试验证
- **业务场景测试**: 电商业务日志生成（多级别）
- **异常处理测试**: 带堆栈跟踪的异常日志
- **高容量测试**: 大量日志生成和处理
- **并发测试**: 多线程并发访问
- **配置测试**: 环境变量覆盖和配置验证

## 构建和部署

### 构建项目
```bash
mvn clean package
```

### 部署应用
将生成的WAR文件部署到支持Servlet 4.0的Web容器中，如Tomcat 9+。

## 测试端点
- `GET /test-log` - 生成各种级别的日志消息
- `GET /test-exception` - 生成异常日志消息

## 配置方式

### XML配置
使用 `web.xml` 文件配置Spring MVC和Logback

### 程序化配置
使用 `WebConfig` 和 `WebAppInitializer` 类进行程序化配置

### 系统属性配置
支持通过系统属性覆盖配置（logx.oss前缀，两层结构）：

**存储配置** (`logx.oss.storage.*`):
- `logx.oss.storage.endpoint` - 存储端点
- `logx.oss.storage.region` - 存储区域
- `logx.oss.storage.accessKeyId` - 访问密钥ID
- `logx.oss.storage.accessKeySecret` - 秘密访问密钥
- `logx.oss.storage.bucket` - 存储桶名称
- `logx.oss.storage.keyPrefix` - 对象key前缀
- `logx.oss.storage.pathStyleAccess` - 路径风格访问
- `logx.oss.storage.enableSsl` - SSL启用