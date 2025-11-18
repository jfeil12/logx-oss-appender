# 开发者指南

本文档为LogX OSS Appender项目的开发者提供详细的开发环境设置、工作流程和贡献指南。

## 目录

- [开发环境设置](#开发环境设置)
- [单仓库多模块工作流](#单仓库多模块工作流)
- [构建和测试](#构建和测试)
- [代码规范](#代码规范)
- [贡献流程](#贡献流程)
- [故障排除](#故障排除)

## 开发环境设置

### 系统要求

- **Java**: OpenJDK 8u392 或更高版本
- **Maven**: 3.9.6 或更高版本
- **Git**: 2.0+ (支持submodules)
- **IDE**: IntelliJ IDEA 或 Eclipse（推荐IntelliJ IDEA）

### 环境变量

```bash
export JAVA_HOME=/path/to/java8
export MAVEN_HOME=/path/to/maven
export PATH=$JAVA_HOME/bin:$MAVEN_HOME/bin:$PATH
```

### 克隆项目

```bash
# 完整克隆（包含所有子模块）
git clone --recursive https://github.com/logx-oss-appender/logx-oss-appender.git
cd logx-oss-appender

# 如果已克隆但缺少子模块
git submodule update --init --recursive
```

## 单仓库多模块工作流

### 理解项目结构

本项目使用单仓库多模块管理六个独立的组件：

- `logx-producer` - 核心库
- `logx-s3-adapter` - S3兼容存储适配器
- `logx-sf-oss-adapter` - SF OSS存储适配器
- `log4j-oss-appender` - Log4j适配器
- `log4j2-oss-appender` - Log4j2适配器
- `logback-oss-appender` - Logback适配器

### 常用Submodule命令

```bash
# 更新所有子模块到最新版本
git submodule update --remote

# 更新特定子模块
git submodule update --remote logx-producer

# 查看子模块状态
git submodule status

# 进入子模块进行开发
cd logx-producer
git checkout main
# 进行修改...
git add .
git commit -m "修改描述"
git push origin main

# 返回主仓库并提交子模块更新
cd ..
git add logx-producer
git commit -m "更新logx-producer子模块"
```

### 子模块开发工作流

1. **在子模块中开发**
   ```bash
   cd logx-producer
   git checkout -b feature/新功能
   # 开发新功能...
   git push origin feature/新功能
   ```

2. **创建Pull Request**（在子模块仓库）

3. **合并后更新主仓库**
   ```bash
   git submodule update --remote logx-producer
   git add logx-producer
   git commit -m "更新logx-producer到最新版本"
   ```

## 构建和测试

### 完整构建

```bash
# 清理并构建所有模块
mvn clean install

# 并行构建（更快）
mvn clean install -T 1C

# 跳过测试的快速构建
mvn clean install -DskipTests
```

### 单模块构建

```bash
# 构建特定模块
mvn clean install -pl log4j2-oss-appender

# 构建模块及其依赖
mvn clean install -pl log4j2-oss-appender -am
```

### 测试执行

```bash
# 运行所有测试
mvn test

# 运行特定模块测试
mvn test -pl logx-producer

# 运行集成测试
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

## 代码规范

### Java编码标准

- **编译目标**: Java 8
- **编码格式**: UTF-8
- **代码风格**: Google Java Style（通过Maven Formatter Plugin强制）
- **命名约定**:
  - 类名: PascalCase (`QueueManager`)
  - 方法名: camelCase (`processLogEvent`)
  - 常量: UPPER_SNAKE_CASE (`DEFAULT_BATCH_SIZE`)
  - 包名: lowercase.dotted (`org.logx.core`)

### 关键规则

- **日志**: 使用SLF4J进行内部日志，禁止在生产代码中使用`System.out`
- **异常处理**: 将云存储异常包装在库特定异常中
- **线程安全**: 所有公共API必须是线程安全的
- **配置**: 对配置对象使用构建器模式
- **依赖**: 将日志框架标记为`provided`作用域

### 测试标准

- **框架**: JUnit 5.10.1
- **模拟**: Mockito 5.8.0
- **断言**: AssertJ 3.24.2
- **测试文件**: `*Test.java`位于`src/test/java`
- **覆盖率**: 核心逻辑最低90%

## 贡献流程

### 1. Fork和克隆

```bash
# Fork主仓库和相关子模块仓库到你的GitHub账户
# 然后克隆你的fork

git clone --recursive https://github.com/你的用户名/logx-oss-appender.git
cd logx-oss-appender

# 添加upstream远程仓库
git remote add upstream https://github.com/logx-oss-appender/logx-oss-appender.git
```

### 2. 创建功能分支

```bash
git checkout -b feature/新功能描述

# 如果修改子模块，也要在子模块中创建分支
cd logx-producer
git checkout -b feature/新功能描述
cd ..
```

### 3. 开发和测试

```bash
# 进行代码修改...

# 运行测试确保没有破坏现有功能
mvn test

# 运行代码质量检查
mvn formatter:validate spotbugs:check
```

### 4. 提交更改

```bash
# 如果修改了子模块，先提交子模块更改
cd logx-producer
git add .
git commit -m "feat: 添加新功能描述"
git push origin feature/新功能描述
cd ..

# 提交主仓库更改
git add .
git commit -m "feat: 在主仓库中集成新功能"
git push origin feature/新功能描述
```

### 5. 创建Pull Request

1. 为修改的子模块创建PR（如适用）
2. 为主仓库创建PR
3. 确保PR描述清晰，包含更改摘要和测试信息

### 6. 代码审查

- 响应审查意见
- 进行必要的修改
- 确保CI检查通过

## IDE配置

### IntelliJ IDEA

1. **导入项目**
   - File → Open → 选择logx-oss-appender目录
   - 选择"Import as Maven project"

2. **配置Java SDK**
   - File → Project Structure → Project → Project SDK → Java 8

3. **代码格式化**
   - File → Settings → Editor → Code Style → Java
   - 导入Google Java Style配置

4. **Submodule支持**
   - File → Settings → Version Control → Git
   - 启用"Use credential helper"

### VS Code

创建`.vscode/settings.json`:

```json
{
    "java.home": "/path/to/java8",
    "maven.executable.path": "/path/to/maven/bin/mvn",
    "java.format.settings.url": "https://raw.githubusercontent.com/google/styleguide/gh-pages/eclipse-java-google-style.xml"
}
```

## 故障排除

### 常见问题

#### 1. 子模块更新问题

```bash
# 如果子模块没有正确更新
git submodule deinit -f .
git submodule update --init --recursive
```

#### 2. Maven构建失败

```bash
# 清理所有缓存
mvn clean
rm -rf ~/.m2/repository/io/github/logxossappender

# 重新构建
mvn clean install
```

#### 3. 代码格式问题

```bash
# 自动修复格式问题
mvn formatter:format

# 检查格式
mvn formatter:validate
```

#### 4. 依赖冲突

```bash
# 查看依赖树
mvn dependency:tree

# 解决冲突后重新构建
mvn clean install -U
```

### 调试技巧

#### Maven调试

```bash
# 详细输出
mvn clean install -X

# 离线模式
mvn clean install -o

# 强制更新依赖
mvn clean install -U
```

#### Git Submodule调试

```bash
# 查看子模块详细状态
git submodule foreach --recursive git status

# 重置子模块到正确状态
git submodule foreach --recursive git reset --hard
git submodule update --init --recursive
```

## 发布流程

### 准备发布

1. **更新版本号**
   ```bash
   mvn versions:set -DnewVersion=1.0.0
   mvn versions:commit
   ```

2. **运行完整测试**
   ```bash
   mvn clean verify -Psecurity
   ```

3. **创建发布标签**
   ```bash
   git tag -a v1.0.0 -m "Release version 1.0.0"
   git push origin v1.0.0
   ```

### 发布到Maven Central

```bash
# 使用发布profile
mvn clean deploy -Prelease

# 签名和上传
mvn clean deploy -Prelease -Dgpg.passphrase=你的GPG密码
```

## 联系方式

如果在开发过程中遇到问题：

1. **查看文档**: [docs/](../docs/)
2. **搜索Issues**: [GitHub Issues](https://github.com/logx-oss-appender/logx-oss-appender/issues)
3. **创建新Issue**: 详细描述问题和重现步骤
4. **讨论**: [GitHub Discussions](https://github.com/logx-oss-appender/logx-oss-appender/discussions)

---

感谢你的贡献！🎉