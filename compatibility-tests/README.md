# LogX OSS Appender 集成和兼容性测试

这个模块包含了 LogX OSS Appender 的所有集成测试和兼容性测试，独立于主项目构建。

## 模块结构

```
compatibility-tests/
├── pom.xml                     # 集成测试父POM
├── spring-boot-test/           # Spring Boot集成和兼容性测试
├── spring-mvc-test/            # Spring MVC集成和兼容性测试
├── jsp-servlet-test/           # JSP/Servlet集成和兼容性测试
├── multi-framework-test/       # 多框架集成和兼容性测试
└── config-consistency-test/    # 配置一致性集成测试
```

## 使用方式

### 独立构建所有测试
```bash
cd compatibility-tests
mvn clean test
```

### 构建特定测试模块
```bash
cd compatibility-tests
mvn clean test -pl spring-boot-test
```

### 运行兼容性测试
```bash
cd compatibility-tests
mvn clean test -Pcompatibility-tests
```

## 测试说明

- **spring-boot-test**: 验证与Spring Boot框架的集成和兼容性
- **spring-mvc-test**: 验证与Spring MVC框架的集成和兼容性
- **jsp-servlet-test**: 验证与传统JSP/Servlet的集成和兼容性
- **multi-framework-test**: 验证多个日志框架的集成和兼容性
- **config-consistency-test**: 验证配置在各框架间的一致性

## 依赖管理

所有子模块都继承自 `integration-compatibility-tests-parent`，统一管理：
- LogX OSS Appender 依赖版本
- 测试框架版本（JUnit 5 和 JUnit 4）
- 日志框架版本（Log4j 1.x、Log4j2、Logback）
- Jackson YAML 依赖（用于配置解析）
- 编译和测试配置

### 特殊依赖说明

- **Log4j 1.x (1.2.17)**: 用于 multi-framework-test 模块验证多框架兼容性
- **Jackson YAML (2.15.3)**: 用于 config-consistency-test 模块解析YAML配置文件
- **Spring Boot BOM (2.7.18)**: 统一管理Spring相关依赖版本
- **JUnit BOM (5.10.1)**: 统一管理测试框架版本

## 注意事项

- 这些测试需要真实的MinIO环境，请按照各测试模块中的README说明配置
- 测试默认跳过，需要使用 `-Pcompatibility-tests` 参数激活
- 所有测试都使用两个核心依赖的方式集成LogX OSS Appender