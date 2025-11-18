# MinIO集成测试环境 - Docker方式（可选）

本目录包含用于集成测试的MinIO Docker环境配置。

**注意**：这是可选的Docker方式，推荐使用MinIO本地安装方式（更简单、无需Docker）。详见[上级目录README](../README-MINIO.md)。

## 前置条件

- Docker 已安装并运行
- Docker Compose 已安装
- 端口9000和9001可用

## 快速启动

在本目录执行启动脚本：

```bash
# 一键启动MinIO（自动检查环境并启动）
./start-minio-docker.sh
```

## 手动启动

在本目录执行：

```bash
# 启动MinIO服务
docker-compose up -d

# 查看服务状态
docker-compose ps

# 查看日志
docker-compose logs -f minio
```

## MinIO配置信息

- **MinIO API地址**: http://localhost:9000
- **MinIO控制台**: http://localhost:9001
- **访问密钥ID**: minioadmin
- **访问密钥Secret**: minioadmin
- **测试Bucket**: logx-test-bucket

## 访问MinIO控制台

浏览器访问: http://localhost:9001

登录凭证:
- Username: minioadmin
- Password: minioadmin

## 停止服务

```bash
# 停止并删除容器
docker-compose down

# 停止并删除容器及数据卷
docker-compose down -v
```

## 集成测试配置

测试配置文件位于: `logx-producer/src/test/resources/minio-test.properties`

配置项:
- endpoint: http://localhost:9000
- bucket: logx-test-bucket
- accessKeyId: minioadmin
- accessKeySecret: minioadmin

## 常见问题

### 端口冲突

如果9000或9001端口被占用，修改`docker-compose.yml`中的端口映射：

```yaml
ports:
  - "19000:9000"  # 修改主机端口
  - "19001:9001"
```

### 容器无法启动

检查Docker守护进程是否运行：

```bash
docker info
```

### 清理环境

```bash
# 完全清理（包括数据卷）
docker-compose down -v

# 重新启动
docker-compose up -d
```

## 手动初始化Bucket

如果自动初始化失败，可以手动创建bucket：

```bash
# 进入minio容器
docker exec -it logx-minio mc alias set myminio http://localhost:9000 minioadmin minioadmin

# 创建bucket
docker exec -it logx-minio mc mb myminio/logx-test-bucket
```
