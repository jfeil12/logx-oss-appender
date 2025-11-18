# MinIO集成测试环境使用指南

## 概述

MinIO是一个高性能的对象存储服务，兼容Amazon S3 API。本指南帮助您快速搭建MinIO测试环境，用于LogX OSS Appender的集成测试。

**推荐安装方式**：直接在本地安装MinIO（简单、快速、无需Docker）

## 快速开始（推荐）

### 安装MinIO

#### 方式1：本地安装（推荐）

##### Linux

```bash
# 下载MinIO服务端
wget https://dl.min.io/server/minio/release/linux-amd64/minio

# 下载MinIO客户端（用于创建bucket）
wget https://dl.min.io/client/mc/release/linux-amd64/mc

# 如果下载慢，可以使用axel多线程下载（更快）
# 安装axel：sudo apt-get install axel 或 sudo yum install axel
# axel -n 10 https://dl.min.io/server/minio/release/linux-amd64/minio
# axel -n 10 https://dl.min.io/client/mc/release/linux-amd64/mc

# 添加执行权限
chmod +x minio mc

# 移动到系统路径（可选）
sudo mv minio /usr/local/bin/
sudo mv mc /usr/local/bin/
```

##### macOS

```bash
# 使用Homebrew安装
brew install minio/stable/minio
brew install minio/stable/mc
```

##### Windows

```powershell
# 使用Chocolatey安装
choco install minio

# 或手动下载
# 访问 https://dl.min.io/server/minio/release/windows-amd64/minio.exe
# 下载并添加到系统PATH
```

### 启动MinIO

#### 使用启动脚本（推荐）

```bash
# 赋予执行权限
chmod +x start-minio-local.sh

# 启动MinIO
./start-minio-local.sh
```

#### 手动启动

```bash
# 创建数据目录
mkdir -p ~/minio-data

# 启动MinIO服务器
MINIO_ROOT_USER=minioadmin \
MINIO_ROOT_PASSWORD=minioadmin \
minio server ~/minio-data --console-address ":9001"

# 在另一个终端创建测试bucket
mc alias set local http://localhost:9000 minioadmin minioadmin
mc mb local/logx-test-bucket
```

### 方式2：使用Docker（可选）

如果您已经安装Docker，可以使用Docker方式：

```bash
# 进入docker目录
cd docker

# 使用Docker Compose启动
./start-minio-docker.sh

# 或手动启动Docker容器
docker-compose up -d
```

详细Docker配置请参见 [docker/README.md](docker/README.md)

## 文件清单

### 1. 启动脚本

- **start-minio-local.sh** - 本地MinIO启动脚本（推荐）
  - 自动检查MinIO安装
  - 启动MinIO服务
  - 自动创建测试bucket
  - 显示访问信息

- **docker/start-minio-docker.sh** - Docker方式启动脚本（可选）
  - 需要Docker环境
  - 自动启动MinIO容器
  - 自动创建测试bucket

### 2. 测试配置文件

- **logx-producer/src/test/resources/minio-test.properties**
  - MinIO连接配置
  - 测试环境专用参数（较小的触发阈值）

### 3. 集成测试类

- **logx-s3-adapter/src/test/java/org/logx/integration/MinIOIntegrationTest.java**
  - 基本上传功能测试
  - 消息数量触发测试
  - 时间触发测试
  - 压缩功能测试

### 4. 文档

- **docker/README.md** - Docker环境详细说明（可选）

### 访问MinIO控制台

浏览器访问：**http://localhost:9001**

```
用户名：minioadmin
密码：minioadmin
```

### 运行集成测试

```bash
# 运行所有MinIO集成测试
mvn test -Dtest=MinIOIntegrationTest -pl logx-s3-adapter

# 运行单个测试方法
mvn test -Dtest=MinIOIntegrationTest#shouldUploadLogsToMinIO -pl logx-s3-adapter
```

## MinIO配置信息

| 配置项 | 值 |
|-------|-----|
| API端点 | http://localhost:9000 |
| 控制台 | http://localhost:9001 |
| Access Key ID | minioadmin |
| Access Key Secret | minioadmin |
| 测试Bucket | logx-test-bucket |
| 对象前缀 | logx/ |

## 测试用例说明

### 1. shouldUploadLogsToMinIO
- **测试目标**：验证基本的日志上传功能
- **步骤**：发送150条测试日志
- **验证点**：
  - 文件成功上传到MinIO
  - 文件内容可正确解压
  - 日志内容格式正确

### 2. shouldTriggerUploadByMessageCount
- **测试目标**：验证消息数量触发机制
- **触发条件**：超过4096条消息
- **验证点**：达到阈值后自动触发上传

### 3. shouldTriggerUploadByTime
- **测试目标**：验证时间触发机制
- **触发条件**：10分钟超时
- **验证点**：超时后自动触发上传

## 环境变量配置

集成测试使用配置文件 `logx-producer/src/test/resources/minio-test.properties` 中的默认值，您可以通过环境变量覆盖这些配置：

### 配置优先级（从高到低）

1. **JVM系统属性**：`mvn test -Dlogx.oss.storage.endpoint=http://localhost:9000`
2. **环境变量**：`export LOGX_OSS_STORAGE_ENDPOINT=http://localhost:9000`
3. **配置文件默认值**：`minio-test.properties`

### 环境变量命名规则

将配置键中的点号替换为下划线并转为大写:
- `logx.oss.storage.endpoint` → `LOGX_OSS_STORAGE_ENDPOINT`
- `logx.oss.storage.accessKeyId` → `LOGX_OSS_STORAGE_ACCESS_KEY_ID`

### 常用环境变量示例

```bash
# MinIO服务端点（默认：http://localhost:9000）
export LOGX_OSS_STORAGE_ENDPOINT="http://localhost:9000"

# MinIO区域（默认：us）
export LOGX_OSS_STORAGE_REGION="us"

# MinIO访问凭证（默认：minioadmin/minioadmin）
export LOGX_OSS_STORAGE_ACCESS_KEY_ID="minioadmin"
export LOGX_OSS_STORAGE_ACCESS_KEY_SECRET="minioadmin"

# MinIO存储桶（默认：logx-test-bucket）
export LOGX_OSS_STORAGE_BUCKET="logx-test-bucket"

# 对象键前缀（默认：logx/）
export LOGX_OSS_STORAGE_KEY_PREFIX="logx/"

# OSS类型（默认：S3）
export LOGX_OSS_STORAGE_OSS_TYPE="S3"
```

### 使用自定义MinIO实例

如果您的MinIO运行在不同的地址或端口:

```bash
# 自定义端点和凭证
export LOGX_OSS_STORAGE_ENDPOINT="http://192.168.1.100:19000"
export LOGX_OSS_STORAGE_ACCESS_KEY_ID="your-custom-key"
export LOGX_OSS_STORAGE_ACCESS_KEY_SECRET="your-custom-secret"
export LOGX_OSS_STORAGE_BUCKET="your-custom-bucket"

# 运行测试
mvn test -Dtest=AsyncEngineIntegrationTest -pl logx-producer
```

## 停止MinIO服务

```bash
# 停止容器
docker-compose down

# 停止并删除数据卷（完全清理）
docker-compose down -v
```

## 常见问题

### 1. MinIO命令未找到

**错误信息**：`minio: command not found`

**解决方法**：
- 检查MinIO是否已安装：`which minio`
- 如果使用手动下载方式，确保minio在系统PATH中
- Linux/macOS：`export PATH=$PATH:$(pwd)` 或将minio移动到`/usr/local/bin/`

### 2. 端口被占用

**错误信息**：`bind: address already in use`

**解决方法**：修改MinIO启动端口
```bash
# 修改API端口为19000，控制台端口为19001
MINIO_ROOT_USER=minioadmin \
MINIO_ROOT_PASSWORD=minioadmin \
minio server ~/minio-data --address ":19000" --console-address ":19001"
```

### 3. Bucket未自动创建

**解决方法**：手动创建bucket
```bash
# 设置MinIO别名
mc alias set local http://localhost:9000 minioadmin minioadmin

# 创建bucket
mc mb local/logx-test-bucket

# 验证bucket已创建
mc ls local
```

### 4. 权限错误

**错误信息**：`permission denied`

**解决方法**：
```bash
# 确保数据目录有写权限
chmod -R 755 ~/minio-data

# 或使用sudo启动（不推荐）
sudo minio server ~/minio-data --console-address ":9001"
```

### 5. Windows环境问题

**常见问题**：
- 环境变量设置方式不同
- 路径格式使用反斜杠

**解决方法**：
```powershell
# Windows PowerShell设置环境变量
$env:MINIO_ROOT_USER="minioadmin"
$env:MINIO_ROOT_PASSWORD="minioadmin"
minio.exe server C:\minio-data --console-address ":9001"
```

## 测试环境配置

测试环境使用较小的阈值以便快速触发：

| 配置项 | 测试环境 | 生产环境 |
|-------|---------|---------|
| maxBatchCount | 100条 | 4096条 |
| maxBatchBytes | 1MB | 10MB |
| maxMessageAgeMs | 5秒 | 10分钟 |
| queueCapacity | 1024 | 8192 |

## 性能测试建议

1. **吞吐量测试**：使用大量消息测试系统承载能力
2. **并发测试**：多线程同时写入测试并发性能
3. **故障恢复测试**：停止MinIO服务测试兜底机制
4. **压缩效果测试**：对比压缩前后的文件大小

## 兼容性测试配置

项目中的兼容性测试（`compatibility-tests/`）默认使用MinIO配置。所有测试配置文件已使用环境变量和默认值：

### 默认配置值

所有兼容性测试使用以下默认值:
- **storage.endpoint**: `http://localhost:9000` (MinIO本地地址)
- **storage.region**: `us` (符合PRD文档规范)
- **storage.accessKeyId**: `minioadmin`
- **storage.accessKeySecret**: `minioadmin`
- **storage.bucket**: `logx-test-bucket`
- **storage.ossType**: `S3`

### 运行兼容性测试

```bash
# 1. 启动MinIO
cd compatibility-tests/minio
./start-minio-local.sh

# 2. 运行兼容性测试（使用默认MinIO配置）
cd ../..
mvn clean install

# 3. 如需自定义配置，可设置环境变量
export LOGX_OSS_STORAGE_ENDPOINT="http://192.168.1.100:9000"
export LOGX_OSS_STORAGE_ACCESS_KEY_ID="custom-key"
export LOGX_OSS_STORAGE_ACCESS_KEY_SECRET="custom-secret"
mvn clean install
```

### 兼容性测试项目清单

已配置MinIO支持的测试项目：
- `spring-boot-test` - Spring Boot集成测试
- `spring-mvc-test` - Spring MVC集成测试
- `multi-framework-test` - 多框架兼容性测试（Log4j/Log4j2/Logback）
- `jsp-servlet-test` - JSP Servlet环境测试

## 下一步

1. **安装MinIO**（选择适合您系统的安装方式）
   - Linux/macOS：使用wget或Homebrew
   - Windows：使用Chocolatey或手动下载

2. **启动MinIO服务**
   ```bash
   # 使用启动脚本（推荐）
   ./start-minio-local.sh

   # 或手动启动
   minio server ~/minio-data --console-address ":9001"
   ```

3. **创建测试bucket**（如果脚本未自动创建）
   ```bash
   mc alias set local http://localhost:9000 minioadmin minioadmin
   mc mb local/logx-test-bucket
   ```

4. **执行集成测试验证功能**
   ```bash
   mvn test -Dtest=MinIOIntegrationTest -pl logx-s3-adapter
   ```

5. **查看MinIO控制台确认文件上传**
   - 访问 http://localhost:9001
   - 用户名/密码：minioadmin/minioadmin

## 注意事项

✅ **推荐方式**：
- 优先使用本地安装MinIO方式（无需Docker，更简单）
- 适用于所有开发环境（Windows、Linux、macOS）
- 性能更好，资源占用更低

⚠️ **Docker方式**（可选）：
- 需要Docker环境支持
- 容器化环境中可能无法启动Docker守护进程
- 详见 [docker/README.md](docker/README.md)

✅ **已完成配置**：
- MinIO本地安装启动脚本
- MinIO Docker编排配置（可选）
- 测试配置文件
- 集成测试代码（已移除@Disabled注解）

## 技术支持

- **MinIO文档**：https://min.io/docs/minio/linux/index.html
- **Docker文档**：https://docs.docker.com/
- **项目文档**：docs/architecture/
