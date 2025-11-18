#!/bin/bash
# MinIO测试环境启动脚本

set -e

echo "========================================="
echo "  LogX OSS Appender - MinIO测试环境"
echo "========================================="
echo ""

# 检查Docker是否安装
if ! command -v docker &> /dev/null; then
    echo "❌ 错误: Docker未安装"
    echo "请先安装Docker: https://docs.docker.com/get-docker/"
    exit 1
fi

# 检查Docker是否运行
if ! docker info &> /dev/null; then
    echo "❌ 错误: Docker守护进程未运行"
    echo ""
    echo "请启动Docker服务:"
    echo "  - macOS: 启动Docker Desktop"
    echo "  - Linux: sudo systemctl start docker"
    echo "  - Windows: 启动Docker Desktop"
    exit 1
fi

echo "✓ Docker服务已就绪"
echo ""

# 检查docker-compose
if ! command -v docker-compose &> /dev/null && ! docker compose version &> /dev/null; then
    echo "❌ 错误: docker-compose未安装"
    exit 1
fi

echo "✓ docker-compose已就绪"
echo ""

# 停止并清理旧容器
echo "清理旧容器..."
docker-compose down -v 2>/dev/null || true

# 启动MinIO服务
echo "启动MinIO服务..."
docker-compose up -d

# 等待MinIO启动
echo ""
echo "等待MinIO服务启动..."
sleep 5

# 检查服务状态
if docker-compose ps | grep -q "Up"; then
    echo ""
    echo "========================================="
    echo "  ✓ MinIO服务启动成功！"
    echo "========================================="
    echo ""
    echo "MinIO API地址: http://localhost:9000"
    echo "MinIO控制台:   http://localhost:9001"
    echo ""
    echo "登录凭证:"
    echo "  用户名: minioadmin"
    echo "  密码:   minioadmin"
    echo ""
    echo "测试Bucket: logx-test-bucket"
    echo ""
    echo "运行集成测试:"
    echo "  mvn test -Dtest=MinIOIntegrationTest -pl logx-producer"
    echo ""
    echo "查看日志:"
    echo "  docker-compose logs -f minio"
    echo ""
    echo "停止服务:"
    echo "  docker-compose down"
    echo ""
else
    echo ""
    echo "❌ MinIO服务启动失败"
    echo ""
    echo "查看日志:"
    echo "  docker-compose logs"
    exit 1
fi
