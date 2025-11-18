# All-in-One JAR测试方法说明

## 背景

All-in-One JAR是为非Maven项目用户提供的集成包，包含框架适配器和所有依赖项。为了测试这些包在真实应用场景下的兼容性，需要特殊的测试配置。

## 测试目录结构

```
compatibility-tests/all-in-one-test/
├── s3-all-in-one-log4j2-test/      # Log4j2 All-in-One测试
├── s3-all-in-one-logback-test/     # Logback All-in-One测试
└── s3-all-in-one-log4j-test/       # Log4j 1.x All-in-One测试
```

## 测试特点

### 1. 项目依赖
All-in-One测试使用标准的Maven项目依赖，直接引用构建生成的All-in-One JAR文件：

```xml
<dependency>
    <groupId>org.logx</groupId>
    <artifactId>s3-log4j2-oss-appender</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

### 2. 运行方式

#### 推荐方式（命令行）
```bash
# 运行单个all-in-one测试模块
mvn test -pl compatibility-tests/all-in-one-test/s3-all-in-one-log4j2-test

# 运行所有all-in-one测试
mvn test -pl compatibility-tests/all-in-one-test/s3-all-in-one-log4j2-test,compatibility-tests/all-in-one-test/s3-all-in-one-logback-test,compatibility-tests/all-in-one-test/s3-all-in-one-log4j-test

# 运行特定测试类
mvn test -Dtest=Log4j2AllInOneTest -pl compatibility-tests/all-in-one-test/s3-all-in-one-log4j2-test
```

#### 构建依赖准备
运行测试前需要确保JAR文件存在：

```bash
# 先构建整个项目，确保all-in-one JAR存在
mvn clean install -DskipTests

# 或仅构建all-in-one模块
mvn clean install -pl all-in-one/s3-log4j2-oss-appender -DskipTests
```

## 测试类型

### 1. 基础兼容性测试
- `Log4j2AllInOneTest.java` - Log4j2 All-in-One兼容性验证
- `LogbackAllInOneTest.java` - Logback All-in-One兼容性验证
- `Log4jAllInOneTest.java` - Log4j 1.x All-in-One兼容性验证

### 2. 性能压力测试
- 消息数量触发条件测试（8192条消息）
- 字节数触发条件测试（10MB）
- 消息年龄触发条件测试（60秒）

### 3. MinIO集成测试
- 确保在MinIO环境正常上传日志
- 验证All-in-One包与外部存储服务的集成

## 配置说明

### Log4j2配置 (log4j2.xml)
```xml
<Log4j2OSSAppender name="OSS_APPENDER">
    <PatternLayout pattern="%d{yyyy-MM-dd HH:mm:ss.SSS} [%t] %-5level %logger{36} - %msg%n"/>
</Log4j2OSSAppender>
```

### Logback配置 (logback-spring.xml)
```xml
<appender name="OSS_APPENDER" class="org.logx.logback.LogbackOSSAppender">
    <layout class="ch.qos.logback.classic.PatternLayout">
        <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n</pattern>
    </layout>
</appender>
```

### 运行时配置 (logx.properties)
```properties
# 激活OSS功能
logx.oss.enabled=true

# 存储配置
logx.oss.storage.endpoint=${LOGX_OSS_STORAGE_ENDPOINT:http://localhost:9000}
logx.oss.storage.region=${LOGX_OSS_STORAGE_REGION:ap-guangzhou}
logx.oss.storage.accessKeyId=${LOGX_OSS_STORAGE_ACCESS_KEY_ID:minioadmin}
logx.oss.storage.accessKeySecret=${LOGX_OSS_STORAGE_ACCESS_KEY_SECRET:minioadmin}
logx.oss.storage.bucket=${LOGX_OSS_STORAGE_BUCKET:logx-test-bucket}
logx.oss.storage.keyPrefix=${LOGX_OSS_STORAGE_KEY_PREFIX:all-in-one-test/}
logx.oss.storage.ossType=${LOGX_OSS_STORAGE_OSS_TYPE:MINIO}

# MinIO特定配置
logx.oss.storage.pathStyleAccess=${LOGX_OSS_STORAGE_PATH_STYLE_ACCESS:true}
logx.oss.storage.enableSsl=${LOGX_OSS_STORAGE_ENABLE_SSL:false}
```

## 常见问题

### 1. JAR文件不存在
**问题**：测试构建失败，提示找不到JAR文件
**解决**：先构建整个项目或相关all-in-one模块

### 2. 版本升级
当升级主项目版本时，需要同时更新以下位置的版本号：
- All-in-One测试模块的pom.xml中的依赖版本

## 设计哲学

选择标准项目依赖的设计是为了：
1. 符合Maven最佳实践，避免使用已废弃的system scope
2. 真实模拟非Maven项目的使用环境
3. 验证All-in-One JAR在真实场景下的兼容性
4. 确保集成包的完整性和独立性