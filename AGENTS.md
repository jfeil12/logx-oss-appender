# LogX OSS Appender - 代码开发指南

## 构建与测试命令

### Maven构建命令
```bash
# 完整构建所有模块
mvn clean install

# 并行构建（更快）
mvn clean install -T 1C

# 跳过测试的快速构建
mvn clean install -DskipTests

# 构建特定模块
mvn clean install -pl log4j2-oss-appender

# 构建模块及其依赖
mvn clean install -pl log4j2-oss-appender -am
```

### 测试执行命令
```bash
# 运行所有测试
mvn test

# 运行特定模块测试
mvn test -pl logx-producer
mvn test -pl log4j2-oss-appender
mvn test -pl logback-oss-appender

# 运行单个测试类
mvn test -pl logx-producer -Dtest=AsyncEngineTest

# 运行单个测试方法
mvn test -pl logx-producer -Dtest=AsyncEngineTest#shouldStartCorrectly

# 运行集成测试（需要MinIO环境）
mvn verify -Pintegration-tests

# 生成测试报告
mvn surefire-report:report
```

### 代码质量检查
```bash
# 代码格式检查
mvn formatter:validate

# 自动格式化代码
mvn formatter:format

# 静态代码分析
mvn spotbugs:check

# 安全扫描
mvn org.owasp:dependency-check-maven:check -Psecurity
```

### 兼容性测试
```bash
# 进入兼容性测试目录
cd compatibility-tests

# 编译所有测试模块
mvn clean compile

# 运行所有兼容性测试（需要MinIO环境）
mvn clean test -Pcompatibility-tests

# 运行特定测试模块
mvn clean test -pl spring-boot-test
mvn clean test -pl multi-framework-test
```

### 全量集成门禁验证（必须）
```bash
# 代码编写完成后，必须执行统一验证入口
# quick: 快速验证（MinIO核心链路）
bash scripts/integration-verify.sh quick

# full: 严格全量（MinIO + compatibility-tests/test-runner + jdk21-test）
# 仅 full 通过才可视为“实现完成”
bash scripts/integration-verify.sh full
```

通过标准：
- quick 模式：MinIO 可用 + MinIOIntegrationTest 通过
- full 模式：quick 通过 + compatibility-tests/test-runner 通过 + jdk21-test 通过
- 任一必选项失败或未覆盖：FAIL（不允许进入完成状态）

## 代码风格指南

### 导入规范
- 优先使用import语句而非完全限定类名
- 按以下顺序组织导入：
  1. java标准库
  2. javax包
  3. 第三方库
  4. 项目内部模块（org.logx.*）
  5. 静态导入

```java
// 正确示例
import java.util.concurrent.CompletableFuture;
import java.util.List;

import com.lmax.disruptor.dsl.Disruptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.logx.storage.StorageService;
import org.logx.core.AsyncEngine;
```

### 格式化规范
- 使用Google Java Style（eclipse-java-google-style.xml）
- 缩进：4个空格，不使用Tab
- 行长度：100字符
- 左大括号不换行

### 命名约定
- 类名：PascalCase（如：AsyncEngine, StorageService）
- 方法名：camelCase（如：start(), putObject()）
- 常量：UPPER_SNAKE_CASE（如：MAX_BATCH_SIZE）
- 变量：camelCase（如：asyncEngine, storageService）
- 包名：小写点分隔（如：org.logx.core）

### 注释规范
- 禁止使用尾行注释，注释必须在代码上一行
- 所有public类和方法必须有Javadoc注释
- 复杂逻辑必须添加注释说明

```java
// 正确：注释在上一行
String message = "Hello World";

// 错误：尾行注释
String message = "Hello World"; // 这是尾行注释，禁止使用
```

### 控制结构规范
- 所有if语句必须使用大括号，即使只有一行代码
- for、while、do-while等控制结构也必须使用大括号

```java
// 正确：使用大括号
if (condition) {
    doSomething();
}

// 错误：缺少大括号
if (condition) doSomething();
```

### 日志规范
- 禁止使用System.out.println进行日志输出
- 必须使用SLF4J日志框架
- Logger定义：`private static final Logger logger = LoggerFactory.getLogger(ClassName.class);`
- 使用相应的日志级别：logger.info(), logger.debug(), logger.warn(), logger.error()

### 异常处理
- 使用有意义的异常消息
- 优先使用具体的异常类型而非Exception
- 在finally块中释放资源
- 使用try-with-resources处理AutoCloseable资源

```java
try (InputStream is = new FileInputStream(file)) {
    // 处理输入流
} catch (FileNotFoundException e) {
    logger.error("文件不存在: {}", file.getPath(), e);
    throw new StorageException("文件无法读取: " + file.getPath(), e);
}
```

### 类型与泛型
- 优先使用接口类型声明变量
- 避免原始类型（Raw Types）
- 合理使用泛型，避免@SuppressWarnings("unchecked")

```java
// 正确：使用接口类型
List<String> items = new ArrayList<>();

// 错误：使用具体实现类型
ArrayList<String> items = new ArrayList<>();
```

### 配置规范
- 所有配置通过logx.properties统一管理
- 配置项命名：logx.oss.{category}.{specific}
- 支持环境变量覆盖：LOGX_OSS_{CATEGORY}_{SPECIFIC}
- 配置优先级：JVM系统属性 > 环境变量 > 配置文件 > 默认值

### 性能规范
- 优先使用StringBuilder进行字符串拼接
- 避免在循环中创建对象
- 合理使用缓存
- 注意线程安全

### 模块依赖规范
- 核心模块（logx-producer）不依赖具体存储实现
- 框架适配器（log4j/logback/log4j2）只依赖核心模块
- 存储适配器（logx-s3-adapter）依赖核心模块
- All-in-One模块依赖框架适配器+存储适配器

### 测试规范
- 单元测试类命名：{ClassName}Test
- 测试方法命名：should{ExpectedBehavior}When{Condition}
- 使用@DisplayName提供中文测试描述
- 合理使用Mockito进行依赖隔离

### Git提交规范
- 格式：`<type>(<scope>): <description>`
- Type：feat, fix, docs, style, refactor, test, chore
- Scope：core, log4j, log4j2, logback, s3, sf, docs, build
- 示例：`feat(core): 增加数据分片处理功能`

### 重要开发规则（Critical Rules）

**RULE 1**: 必须使用中文沟通
- 所有交流必须使用中文，包括代码说明、进度报告、提交信息、测试输出等
- 只有技术关键词、API名称、配置键名可以使用英文

**RULE 2**: 代码不能尾行注释
- 注释必须在代码上一行，禁止尾行注释

**RULE 3**: if语句一定要有大括号
- 所有控制结构必须使用大括号，即使只有一行代码

**RULE 4**: 禁止使用System.out.println
- 所有日志输出必须使用SLF4J日志框架

**RULE 5**: 优先使用import语句而非完全限定类名
- 避免在代码中使用完全限定类名，应通过import导入

### 关键配置文件
- `eclipse-java-google-style.xml`：Google Java代码风格配置
- `pom.xml`：Maven构建配置，包含插件管理
- `logx.properties`：默认配置文件模板

### 项目技术栈
- Java 8+（目标1.8）
- Maven 3.9.6+
- LMAX Disruptor 3.4.4（高性能队列）
- AWS SDK 2.28.16
- JUnit 5.10.1（测试框架）
- Mockito 5.8.0（Mock框架）
- AssertJ 3.24.2（断言库）

### 常用Maven插件
- `maven-formatter-plugin`：代码格式化（已禁用，可选启用）
- `spotbugs-maven-plugin`：静态代码分析
- `maven-surefire-plugin`：单元测试执行
- `dependency-check-maven`：安全扫描（需-Psecurity）

### 环境要求
- Java：OpenJDK 8u392+
- Maven：3.9.6+
- Git：2.0+（支持submodules）

### 故障排除
```bash
# 清理Maven缓存
mvn clean
rm -rf ~/.m2/repository/org/logx

# 更新子模块
git submodule update --init --recursive

# 检查依赖冲突
mvn dependency:tree
```
