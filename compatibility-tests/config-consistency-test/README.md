# 配置一致性验证工具

## 概述
此工具用于验证OSS Appender在不同框架中的配置参数一致性。

## 技术依赖
- **Jackson YAML (2.15.3)**: 用于解析和处理YAML格式配置文件
- **Jackson Core**: 用于JSON和配置数据处理

## 功能特性
1. 验证所有框架使用相同配置参数名称
2. 测试环境变量覆盖的一致性
3. 验证配置验证机制的一致性
4. 测试错误配置的处理一致性
5. 验证配置加载机制的一致性
6. 验证配置更新机制的一致性
7. 验证配置值的一致性

## 构建和运行

### 构建项目
```bash
mvn clean compile
```

### 运行工具
```bash
mvn exec:java -Dexec.mainClass="org.logx.compatibility.config.ConfigConsistencyVerificationMain"
```

### 运行测试
```bash
mvn test
```

## 验证内容

### 配置参数一致性
验证所有框架使用相同的配置参数名称（logx.oss前缀，两层结构）：

**存储配置** (`logx.oss.storage.*`):
- `logx.oss.storage.bucket` - 存储桶名称
- `logx.oss.storage.keyPrefix` - 对象key前缀
- `logx.oss.storage.region` - 存储区域
- `logx.oss.storage.accessKeyId` - 访问密钥ID
- `logx.oss.storage.accessKeySecret` - 秘密访问密钥
- `logx.oss.storage.endpoint` - 存储端点
- `logx.oss.storage.ossType` - OSS类型
- `logx.oss.storage.pathStyleAccess` - 路径风格访问
- `logx.oss.storage.enableSsl` - SSL启用
- `logx.oss.storage.maxConnections` - 最大连接数
- `logx.oss.storage.connectTimeout` - 连接超时
- `logx.oss.storage.readTimeout` - 读取超时

### 环境变量一致性
验证所有框架支持相同的环境变量（LOGX_OSS前缀，两层结构）：

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

### 验证机制一致性
验证所有框架使用相同的配置验证机制：
- 所有框架都使用S3ConfigValidator进行配置验证
- 所有框架都使用相同的验证规则
- 所有框架都使用相同的验证错误消息

### 错误处理一致性
验证所有框架使用相同的错误处理机制：
- 所有框架都使用统一的错误处理机制
- 所有框架都使用相同的错误日志格式
- 所有框架都使用相同的错误恢复策略

### 配置加载机制一致性
验证所有框架使用相同的配置加载机制：
- 所有框架都遵循相同的配置加载优先级：环境变量 > 系统属性 > 配置文件
- 所有框架都支持相同的配置回退机制

### 配置更新机制一致性
验证所有框架使用相同的配置更新机制：
- 所有框架都支持运行时配置更新
- 所有框架都支持配置重新加载

### 配置值一致性
验证所有框架配置值的一致性：
- 验证关键配置参数在所有框架中的值是否一致
- 验证配置参数缺失情况的一致性检测