# VSCode测试执行器故障排除指南

## 问题：All-in-One测试无法在VSCode中直接点击执行

### 问题现象

在VSCode中，以下测试类无法直接通过点击类名或测试方法名运行：
- `Log4j2AllInOneTest`
- `Log4jAllInOneTest`
- `LogbackAllInOneTest`

而其他测试类如 `BusinessLogGenerationTest` 可以正常执行。

### 根本原因

All-in-One测试模块使用了标准项目依赖，这类依赖需要确保所有依赖项都正确构建和安装。

### 技术细节

**All-in-One测试模块**（位于 `compatibility-tests/all-in-one-test/`）:
```xml
<dependency>
    <groupId>org.logx</groupId>
    <artifactId>s3-log4j2-oss-appender</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

**常规测试模块**（位于 `compatibility-tests/spring-boot-test/`）:
```xml
<dependency>
    <groupId>org.logx</groupId>
    <artifactId>logback-oss-appender</artifactId>
</dependency>
```

### 解决方案

#### 方法1：使用命令行运行测试（推荐）

```bash
# 运行单个all-in-one测试模块
mvn test -pl compatibility-tests/all-in-one-test/s3-all-in-one-log4j2-test

# 运行所有all-in-one测试
mvn test -pl compatibility-tests/all-in-one-test/s3-all-in-one-log4j2-test,compatibility-tests/all-in-one-test/s3-all-in-one-logback-test,compatibility-tests/all-in-one-test/s3-all-in-one-log4j-test
```

#### 方法2：构建项目确保JAR存在

```bash
# 先执行完整构建
mvn clean install -DskipTests

# 然后再次尝试在VSCode中执行测试
```

#### 方法3：在VSCode中配置Java运行时参数

在VSCode的 `launch.json` 中添加系统依赖路径：
```json
{
    "type": "java",
    "name": "Test All-in-One",
    "request": "launch",
    "mainClass": "org.logx.compatibility.s3.allinone.Log4j2AllInOneTest",
    "projectName": "s3-all-in-one-log4j2-integration-test",
    "vmArgs": "-Djava.library.path=/path/to/project/compatibility-tests/all-in-one-test/s3-all-in-one-log4j2-test/lib"
}
```

### 验证步骤

1. 确保MinIO服务正在运行（参考 `compatibility-tests/minio/README-MINIO.md`）
2. 使用命令行运行测试，确认功能正常：
   ```bash
   mvn test -Dtest=Log4j2AllInOneTest -pl compatibility-tests/all-in-one-test/s3-all-in-one-log4j2-test
   ```
3. 监控MinIO控制台确认日志成功上传

### 注意事项

- All-in-One JAR是为非Maven项目的用户提供的一体化依赖方案
- 在开发环境中，建议优先使用命令行运行这些特定测试
- 这是设计上的权衡，确保All-in-One包的独立性和完整性的代价是IDE集成的复杂性
- 常规模块的测试（如spring-boot-test）仍然可以通过IDE直接运行

### 相关文件

- `compatibility-tests/all-in-one-test/s3-all-in-one-log4j2-test/pom.xml`
- `compatibility-tests/all-in-one-test/s3-all-in-one-logback-test/pom.xml`
- `compatibility-tests/all-in-one-test/s3-all-in-one-log4j-test/pom.xml`