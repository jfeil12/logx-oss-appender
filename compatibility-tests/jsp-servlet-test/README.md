# JSP/Servlet兼容性测试应用

## 概述
此应用用于验证OSS Appender在传统JSP/Servlet环境中的兼容性和性能表现。

## 功能特性
1. 验证web.xml配置方式的兼容性
2. 测试系统属性和环境变量配置
3. 验证与传统Web容器的兼容性
4. 执行性能基准测试
5. 真实业务日志生成测试（订单、安全、通用业务场景）
6. 高并发Servlet访问测试

## 性能指标

根据架构文档调整后的性能要求：

### 核心性能指标
| 指标 | 目标值 | 说明 |
|------|--------|------|
| 吞吐量 | 10,000+条日志/秒 | 高并发处理能力 |
| 日志无丢失 | 零丢失率 | 在高吞吐量负载下确保数据完整性 |
| 队列内存占用 | < 512MB | 内存高效使用，避免OOM |

### 测试验证
- **业务场景测试**: 订单业务、安全审计、通用业务日志生成
- **并发测试**: 5线程并发Servlet访问
- **环境配置测试**: 验证MinIO连接和配置加载
- **内存监控**: 测试期间监控队列内存使用情况

## 构建和部署

### 构建项目
```bash
mvn clean package
```

### 部署应用
将生成的WAR文件部署到支持Servlet 4.0的Web容器中，如Tomcat 9+。

## 测试端点
- `GET /test-log` - 生成各种级别的日志消息（Servlet）
- `GET /test-exception` - 生成异常日志消息（Servlet）
- `GET /test-log.jsp` - 生成各种级别的日志消息（JSP）

## 配置方式

### web.xml配置
使用 `web.xml` 文件配置Logback

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

### 环境变量配置
支持通过环境变量覆盖配置（LOGX_OSS前缀，两层结构）：

**存储配置环境变量**:
- `LOGX_OSS_STORAGE_ENDPOINT` - 存储端点
- `LOGX_OSS_STORAGE_REGION` - 存储区域
- `LOGX_OSS_STORAGE_ACCESS_KEY_ID` - 访问密钥ID
- `LOGX_OSS_STORAGE_ACCESS_KEY_SECRET` - 秘密访问密钥
- `LOGX_OSS_STORAGE_BUCKET` - 存储桶名称
- `LOGX_OSS_STORAGE_KEY_PREFIX` - 对象key前缀
- `LOGX_OSS_STORAGE_OSS_TYPE` - OSS类型

**引擎配置环境变量**:
- `LOGX_OSS_ENGINE_MAX_UPLOAD_SIZE_MB` - 最大上传文件大小