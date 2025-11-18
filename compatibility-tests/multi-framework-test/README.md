# 多框架共存兼容性测试应用

## 概述
此应用用于验证OSS Appender在多个日志框架共存环境中的兼容性。

## 支持的日志框架
- **Logback**: 通过 SLF4J API 集成
- **Log4j2**: 现代化的 Log4j 框架
- **Log4j 1.x (1.2.17)**: 传统 Log4j 框架，确保向后兼容性

## 功能特性
1. 验证Logback、Log4j2和Log4j 1.x在同一应用中协同工作
2. 验证配置隔离和独立工作
3. 测试资源竞争和线程安全
4. 验证日志输出的一致性
5. 执行并发性能测试
6. 验证配置参数一致性
7. 验证环境变量覆盖一致性
8. 验证错误处理一致性

## 构建和运行

### 构建项目
```bash
mvn clean compile
```

### 运行应用
```bash
mvn exec:java -Dexec.mainClass="org.logx.compatibility.multiframework.MultiFrameworkCoexistenceTest"
```

或者
```bash
java -cp target/classes:target/dependency/* org.logx.compatibility.multiframework.MultiFrameworkCoexistenceTest
```

### 运行测试
```bash
mvn test
```

### 运行综合测试套件
```bash
mvn test -Dtest=org.logx.compatibility.multiframework.ComprehensiveCompatibilityTestSuite
```

## 配置方式

### Logback配置
使用 `logback.xml` 文件配置

### Log4j2配置
使用 `log4j2.xml` 文件配置

### Log4j 1.x配置
使用 `log4j.xml` 文件配置

## 测试说明

### 多框架共存测试
验证多个日志框架在同一应用中协同工作，包括：
- 日志记录器初始化测试
- 并发访问测试
- 高并发性能测试
- 日志记录器创建性能测试
- 异常日志记录测试

### 配置隔离测试
验证每个框架使用独立的配置，包括：
- 配置文件分离测试
- 日志记录器名称隔离测试
- Appender隔离测试
- 配置重载隔离测试
- 日志级别配置隔离测试

### 日志输出一致性测试
验证不同框架输出的日志格式一致，包括：
- 日志格式一致性测试
- 日志级别一致性测试
- 时间戳一致性测试
- 消息内容一致性测试
- 异常日志一致性测试
- 参数化消息一致性测试

### 资源竞争测试
验证多线程环境下日志记录的安全性，包括：
- 线程安全性测试
- 内存使用测试
- CPU使用测试
- 资源清理测试
- 高并发资源竞争测试
- 文件句柄泄漏检测
- 缓冲区管理测试

### 性能测试
验证多框架共存的性能表现，包括：
- 并发性能测试
- 内存开销测试
- 吞吐量测试
- 延迟测试
- 负载下的可扩展性测试
- 压力下的资源消耗测试
- 性能一致性测试
- 各框架特定性能测试

每个框架使用不同的日志前缀以确保日志输出的隔离性：
- Logback: `logx/logback/`
- Log4j2: `logx/log4j2/`
- Log4j 1.x: `logx/log4j1/`

这样可以验证每个框架的日志都能正确上传到指定位置，而不会相互干扰。