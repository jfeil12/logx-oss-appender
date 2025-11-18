# Git 项目管理指南

## 项目概述

LogX OSS Appender 采用单仓库多模块（Monorepo）架构，包含以下核心模块：

```
logx-oss-appender/
├── logx-producer/              # 核心抽象层
├── logx-s3-adapter/            # S3兼容存储适配器
├── logx-sf-oss-adapter/        # SF OSS存储适配器
├── log4j-oss-appender/         # Log4j 1.x适配器
├── log4j2-oss-appender/        # Log4j2适配器
├── logback-oss-appender/       # Logback适配器
└── docs/                       # 项目文档
```

## Git 工作流程

### 日常开发

#### 1. 克隆项目
```bash
git clone https://github.com/your-org/logx-oss-appender.git
cd logx-oss-appender
```

#### 2. 分支管理
- **main**: 主分支，稳定发布版本
- **develop**: 开发分支，集成最新功能
- **feature/***: 功能分支，开发新特性
- **hotfix/***: 热修复分支，紧急修复

#### 3. 功能开发流程
```bash
# 创建功能分支
git checkout -b feature/new-feature develop

# 开发完成后提交
git add .
git commit -m "feat: add new feature description"

# 推送到远程
git push origin feature/new-feature

# 创建Pull Request合并到develop
```

### 模块间依赖管理

#### Maven模块依赖关系
```
logx-producer (核心)
    ↓
log4j-oss-appender
log4j2-oss-appender
logback-oss-appender
```

三个适配器都直接依赖于核心模块，彼此之间没有依赖关系。

#### 修改依赖时的注意事项
1. **修改logx-producer**: 需要测试所有适配器模块
2. **修改适配器**: 只影响对应的日志框架
3. **版本发布**: 统一版本号，同步发布所有模块

### 代码提交规范

#### Commit Message格式
```
<type>(<scope>): <description>

[optional body]

[optional footer]
```

#### Type类型
- `feat`: 新功能
- `fix`: Bug修复
- `docs`: 文档更新
- `style`: 代码格式调整
- `refactor`: 重构
- `test`: 测试相关
- `chore`: 构建工具、依赖更新

#### Scope范围
- `core`: logx-producer核心模块
- `log4j`: log4j适配器
- `log4j2`: log4j2适配器
- `logback`: logback适配器
- `docs`: 文档
- `build`: 构建相关

#### 示例
```bash
feat(core): 实现DisruptorBatchingQueue高性能队列
fix(log4j2): 修复配置解析异常
docs(readme): 更新配置示例说明
```

## 版本管理

### 版本号策略
采用语义化版本控制（Semantic Versioning）：`MAJOR.MINOR.PATCH`

- **MAJOR**: 不兼容的API变更
- **MINOR**: 向后兼容的功能性新增
- **PATCH**: 向后兼容的问题修正

### 发布流程

#### 1. 准备发布
```bash
# 切换到develop分支
git checkout develop
git pull origin develop

# 创建release分支
git checkout -b release/1.2.0

# 更新版本号
mvn versions:set -DnewVersion=1.2.0
mvn versions:commit

# 提交版本变更
git add .
git commit -m "chore: bump version to 1.2.0"
```

#### 2. 完成发布
```bash
# 合并到main分支
git checkout main
git merge --no-ff release/1.2.0

# 创建标签
git tag -a v1.2.0 -m "Release version 1.2.0"

# 推送到远程
git push origin main
git push origin v1.2.0

# 合并回develop分支
git checkout develop
git merge --no-ff release/1.2.0

# 删除release分支
git branch -d release/1.2.0
git push origin --delete release/1.2.0
```

## 持续集成

### GitHub Actions工作流

#### 1. 构建测试 (`.github/workflows/ci.yml`)
```yaml
name: CI
on: [push, pull_request]
jobs:
  test:
    runs-on: ubuntu-latest
    strategy:
      matrix:
        java: [8, 11, 17]
    steps:
      - uses: actions/checkout@v3
      - uses: actions/setup-java@v3
        with:
          java-version: ${{ matrix.java }}
      - run: mvn clean test
```

#### 2. 发布部署 (`.github/workflows/release.yml`)
```yaml
name: Release
on:
  push:
    tags: ['v*']
jobs:
  deploy:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - uses: actions/setup-java@v3
      - run: mvn clean deploy
```

## 协作最佳实践

### 1. 代码审查
- 所有代码必须通过Pull Request
- 至少需要一名团队成员审查
- 确保通过所有自动化测试

### 2. 冲突解决
```bash
# 更新本地分支
git checkout feature/my-feature
git fetch origin
git rebase origin/develop

# 解决冲突后
git add .
git rebase --continue
git push --force-with-lease origin feature/my-feature
```

### 3. 紧急修复
```bash
# 从main创建hotfix分支
git checkout main
git checkout -b hotfix/critical-fix

# 修复完成后直接合并到main和develop
git checkout main
git merge --no-ff hotfix/critical-fix
git tag v1.2.1

git checkout develop
git merge --no-ff hotfix/critical-fix
```

## 工具配置

### 1. Git配置
```bash
# 设置用户信息
git config user.name "Your Name"
git config user.email "your.email@example.com"

# 设置默认分支
git config init.defaultBranch main

# 启用自动换行转换
git config core.autocrlf input
```

### 2. IDE集成
推荐配置：
- IntelliJ IDEA: 启用Git集成、代码格式化
- VS Code: 安装GitLens插件
- Eclipse: 使用EGit插件

### 3. 钩子脚本
可配置pre-commit钩子执行：
- 代码格式检查
- 单元测试
- 静态代码分析

---

*本文档基于单仓库多模块架构设计，确保代码管理的一致性和高效性*