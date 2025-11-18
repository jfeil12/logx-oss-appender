# JDK版本管理指南

本文档说明如何使用SDKMAN管理不同版本的JDK来进行LogX OSS Appender项目的兼容性测试。

## 为什么使用SDKMAN

1. **环境隔离**：可以轻松在不同JDK版本之间切换，避免Maven profile的复杂配置
2. **一致性**：确保编译和运行时使用相同的JDK版本
3. **简单易用**：命令行工具，易于自动化和脚本化

## 安装与初始化

### macOS/Linux/WSL

```bash
# 安装SDKMAN
curl -s "https://get.sdkman.io" | bash

# 加载环境
source "$HOME/.sdkman/bin/sdkman-init.sh"
```

### Windows

建议使用WSL或Git Bash来使用SDKMAN（原生CMD/PowerShell不支持）。

## 安装JDK版本

### 查看可用版本

```bash
sdk list java
```

### 安装JDK 8和JDK 21

```bash
# 安装JDK 8 (示例标识，可按需选择vendor)
sdk install java 8.0.462.fx-zulu

# 安装JDK 21 (示例标识，可按需选择vendor)
sdk install java 21.0.8.crac-zulu
```

## 切换JDK版本

### 临时切换（仅当前终端有效）

```bash
# 切换到JDK 21
sdk use java 21.0.8.crac-zulu

# 切换到JDK 8
sdk use java 8.0.462.fx-zulu
```

### 持久切换（新开终端也生效）

```bash
# 设为默认JDK 21
sdk default java 21.0.8.crac-zulu

# 设为默认JDK 8
sdk default java 8.0.462.fx-zulu
```

## 验证与配置

### 验证当前版本

```bash
java -version
sdk current java
```

### 验证JAVA_HOME

```bash
echo $JAVA_HOME
```
应该显示为 `$HOME/.sdkman/candidates/java/current`（SDKMAN会自动设置，无需手动export）。

## 项目级配置

### 在项目根目录生成或更新.sdkmanrc

```bash
# 生成.sdkmanrc文件
sdk env init
```

### 按需在.sdkmanrc中固定版本

#### 使用JDK 21

在.sdkmanrc中配置：
```
java=21.0.8.crac-zulu
```

然后应用配置：
```bash
sdk env
```

#### 使用JDK 8

在.sdkmanrc中配置：
```
java=8.0.462.fx-zulu
```

然后应用配置：
```bash
sdk env
```

## 测试不同JDK版本

### 使用JDK 8进行测试

```bash
# 切换到JDK 8
sdk use java 8.0.462.fx-zulu

# 验证版本
java -version

# 运行兼容性测试
cd compatibility-tests
mvn clean test
```

### 使用JDK 21进行测试

```bash
# 切换到JDK 21
sdk use java 21.0.8.crac-zulu

# 验证版本
java -version

# 运行JDK 21兼容性测试
sdk use java 21.0.8.crac-zulu && cd compatibility-tests && mvn clean test -pl jdk21-test
```

## 注意事项

1. **JVM参数**：JDK 21可能需要额外的JVM参数，如`--add-opens`，这些应该在测试代码或运行脚本中处理，而不是在Maven配置中
2. **依赖兼容性**：确保所有依赖项与目标JDK版本兼容
3. **性能测试**：在不同JDK版本上运行性能测试以确保没有性能下降
4. **环境一致性**：确保CI/CD环境也使用相同的JDK版本管理方式

## 常见问题

### 1. JAVA_HOME未正确设置

确保使用`sdk use`或`sdk default`命令而不是手动设置JAVA_HOME。

### 2. Maven仍使用错误的JDK版本

检查是否在Maven的settings.xml或项目的pom.xml中有硬编码的JDK配置。

### 3. 测试失败与JDK版本相关

检查是否需要添加JDK特定的JVM参数，特别是在JDK 21上运行时。