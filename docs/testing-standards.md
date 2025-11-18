# 测试规范：OSS文件命名和配置

本文档定义了LogX OSS Appender项目中所有测试用例必须遵循的文件命名和配置标准。

## 核心原则

### 1. 统一文件命名格式
所有测试用例都必须使用 `ObjectNameGenerator.generateObjectName()` 生成标准化的文件名，**禁止**使用硬编码的文件名。

#### 标准命名格式
```
logx/yyyy/MM/dd/HHmmssSSS-applog-IP-uniqueId.log.gz
```

示例：`logx/2025/10/14/143250200-applog-192.168.1.100-abc12345.log.gz`

#### 格式说明
- `logx/`: 默认对象前缀
- `yyyy/MM/dd/`: 日期目录结构
- `HHmmssSSS`: 时分秒毫秒（紧凑格式）
- `applog`: 固定标识符
- `IP`: 本机IP地址
- `uniqueId`: 8位UUID（防止文件名冲突）
- `.log.gz`: 压缩后的日志文件后缀

### 2. 一致的前缀配置
所有测试环境中，`keyPrefix` 必须统一设置为 `logx/`，不得使用其他前缀如：
- ❌ `integration-test/`
- ❌ `test-logs/`
- ❌ `logs/`
- ❌ `custom-logs/`

## 测试用例实现要求

### 正确示例

```java
@Test
public void testFileUpload() {
    // ✅ 正确：使用ObjectNameGenerator
    ObjectNameGenerator nameGenerator = new ObjectNameGenerator("test-app");
    String objectKey = nameGenerator.generateObjectName();

    // 或者通过AsyncEngineImpl间接使用
    AsyncEngineImpl engine = new AsyncEngineImpl(config, storageService);
    engine.start();

    // 上传操作会自动使用规范的文件名
    CompletableFuture<Void> uploadFuture = storageService.putObject(objectKey, data);
    // ...
}
```

### 错误示例

```java
@Test
public void testFileUploadWrong() {
    // ❌ 错误：硬编码文件名，不符合规范
    String objectKey = "test-logs/integration-test-" + System.currentTimeMillis() + ".log";

    // ❌ 错误：非标准格式
    storageService.putObject(bucket, "test-key", data);

    // ❌ 错误：非标准前缀
    System.setProperty("logx.oss.storage.keyPrefix", "custom-logs/");
}
```

## 配置文件标准

### 测试属性文件
所有 `*-test.properties` 文件中的 `keyPrefix` 必须设置为 `logx/`：

```properties
# 正确配置
logx.oss.storage.keyPrefix=logx/

# 错误配置（禁止使用）
# logx.oss.storage.keyPrefix=integration-test/
# logx.oss.storage.keyPrefix=test-logs/
# logx.oss.storage.keyPrefix=custom-logs/
```

### 环境变量配置
测试代码中设置环境变量时也必须遵循此标准：

```properties
# 正确
LOGX_OSS_STORAGE_KEY_PREFIX=logx/

# 错误（禁止使用）
# LOGX_OSS_STORAGE_KEY_PREFIX=integration-test/
# LOGX_OSS_STORAGE_KEY_PREFIX=custom-logs/
```

## 具体实施位置

### 已规范化文件
这些文件已正确实现，无需修改：
- `FallbackMechanismTest.java` - 已使用ObjectNameGenerator
- `FallbackIntegrationTest.java` - 已使用ObjectNameGenerator
- `AsyncEngineImpl.java` - 内部已使用nameGenerator

### 需要修改的文件（已完成）
以下文件已修改为符合规范：
- `MinIOIntegrationTest.java` - 修改为使用ObjectNameGenerator和logx/前缀
- `LogxSfOssClientTest.java` - 修改为使用ObjectNameGenerator
- 所有测试配置文件中的keyPrefix已统一为logx/

## Mock测试要求

即使是Mock测试，也必须遵循文件命名规范：

```java
@Test
public void testMockPutObject() {
    // ✅ 正确：Mock测试也使用规范命名
    ObjectNameGenerator nameGenerator = new ObjectNameGenerator("mock-test");
    String objectKey = nameGenerator.generateObjectName();

    try (MockedStatic<StorageService> mockedStorage = mockStatic(StorageService.class)) {
        mockedStorage.when(() -> storageService.putObject(objectKey, data))
                    .thenReturn(CompletableFuture.completedFuture(null));
        // 测试逻辑
    }
}
```

### 验证方法
在测试中验证文件命名格式是否正确：

```java
@Test
public void testObjectNameFormat() {
    ObjectNameGenerator nameGenerator = new ObjectNameGenerator("validation-test");
    String objectName = nameGenerator.generateObjectName();

    // 验证格式
    assertTrue(objectName.startsWith("logx/"));
    assertTrue(objectName.contains("/applog-"));
    assertTrue(objectName.endsWith(".log.gz"));
    assertTrue(objectName.matches("logx/\\d{4}/\\d{2}/\\d{2}/\\d{10}-applog-[^-]+-[a-zA-Z0-9]{8}\\.log\\.gz"));
}
```

## 兼容性测试

所有兼容性测试（Log4j、Log4j2、Logback）必须统一使用 `logx/` 作为前缀，确保跨框架的一致性。

```properties
# 所有框架配置示例
log4j.appender.oss.keyPrefix=logx/
log4j2.oss.keyPrefix=logx/
logback.oss.keyPrefix=logx/
logging.logback.oss.key-prefix=logx/
```

## 违规检查和使用权限制

### 检查规则
运行以下命令检查可能的违规代码：

```bash
# 检查硬编码文件名
grep -r "test-logs\|integration-test\|custom-logs" src/test/

# 检查非标准putObject调用
grep -r "putObject.*\"[^\"]*\"" src/test/

# 检查非标准前缀
grep -r "keyPrefix.*=" src/test/resources/
```

### 工具支持
在未来版本中，可考虑添加：
- 编译时检查规则
- 单元测试验证命名格式
- IDE插件提示和自动修正

## 文档维护

本文档是测试规范的核心文档，所有测试代码的修改都必须参照此文档执行。如有特殊需求偏离标准，需要：

1. 在相关测试中添加详细注释说明原因
2. 在测试代码评审中明确说明偏离标准的理由
3. 考虑将标准本身进行适当调整

## 生效范围

本文档适用于：
- 所有单元测试 (`src/test/java/**/*.java`)
- 所有集成测试 (`compatibility-tests/**/*.java`)
- 所有测试配置文件 (`src/test/resources/**/*.properties`)
- 所有环境变量配置示例

**生效日期**: 2025年10月14日

**维护责任**: 测试团队和代码审查者共同维护