#!/bin/bash

# MinIO本地启动脚本
# 用于快速搭建MinIO测试环境，无需Docker

set -e

echo "========================================="
echo "  LogX OSS Appender - MinIO本地启动脚本"
echo "========================================="
echo ""

# 检查MinIO是否已安装
if ! command -v minio &> /dev/null; then
    echo "❌ 错误: MinIO未安装"
    echo ""
    echo "请先安装MinIO:"
    echo ""
    echo "Linux:"
    echo "  wget https://dl.min.io/server/minio/release/linux-amd64/minio"
    echo "  chmod +x minio"
    echo "  sudo mv minio /usr/local/bin/"
    echo ""
    echo "macOS:"
    echo "  brew install minio/stable/minio"
    echo ""
    echo "Windows:"
    echo "  choco install minio"
    echo ""
    exit 1
fi

# 检查mc是否已安装
if ! command -v mc &> /dev/null; then
    echo "⚠️  警告: MinIO客户端(mc)未安装，将无法自动创建bucket"
    echo ""
    echo "建议安装mc:"
    echo ""
    echo "Linux:"
    echo "  wget https://dl.min.io/client/mc/release/linux-amd64/mc"
    echo "  chmod +x mc"
    echo "  sudo mv mc /usr/local/bin/"
    echo ""
    echo "macOS:"
    echo "  brew install minio/stable/mc"
    echo ""
    echo "Windows:"
    echo "  choco install minio-client"
    echo ""
    MC_AVAILABLE=false
else
    MC_AVAILABLE=true
fi

echo "✅ MinIO已安装: $(which minio)"
if [ "$MC_AVAILABLE" = true ]; then
    echo "✅ MinIO客户端(mc)已安装: $(which mc)"
fi
echo ""

# 配置参数
MINIO_DATA_DIR="${MINIO_DATA_DIR:-$HOME/minio-data}"
MINIO_ROOT_USER="${MINIO_ROOT_USER:-minioadmin}"
MINIO_ROOT_PASSWORD="${MINIO_ROOT_PASSWORD:-minioadmin}"
MINIO_API_PORT="${MINIO_API_PORT:-9000}"
MINIO_CONSOLE_PORT="${MINIO_CONSOLE_PORT:-9001}"
BUCKET_NAME="${BUCKET_NAME:-logx-test-bucket}"

echo "📋 配置信息:"
echo "  数据目录: $MINIO_DATA_DIR"
echo "  用户名: $MINIO_ROOT_USER"
echo "  密码: $MINIO_ROOT_PASSWORD"
echo "  API端口: $MINIO_API_PORT"
echo "  控制台端口: $MINIO_CONSOLE_PORT"
echo "  测试Bucket: $BUCKET_NAME"
echo ""

# 创建数据目录
if [ ! -d "$MINIO_DATA_DIR" ]; then
    echo "📁 创建数据目录: $MINIO_DATA_DIR"
    mkdir -p "$MINIO_DATA_DIR"
fi

# 检查端口是否被占用
if lsof -Pi :$MINIO_API_PORT -sTCP:LISTEN -t >/dev/null 2>&1 ; then
    echo "⚠️  端口 $MINIO_API_PORT 已被占用"
    echo ""
    read -p "是否停止现有MinIO进程并重启? (y/n) " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        echo "🛑 停止现有MinIO进程..."
        pkill -f "minio server" || true
        sleep 2
    else
        echo "❌ 启动取消"
        exit 1
    fi
fi

# 启动MinIO服务器（后台运行）
echo "🚀 启动MinIO服务器..."
MINIO_ROOT_USER=$MINIO_ROOT_USER \
MINIO_ROOT_PASSWORD=$MINIO_ROOT_PASSWORD \
nohup minio server "$MINIO_DATA_DIR" \
    --address ":$MINIO_API_PORT" \
    --console-address ":$MINIO_CONSOLE_PORT" \
    > "$MINIO_DATA_DIR/minio.log" 2>&1 &

MINIO_PID=$!
echo "✅ MinIO已启动 (PID: $MINIO_PID)"
echo "📄 日志文件: $MINIO_DATA_DIR/minio.log"
echo ""

# 等待MinIO启动完成
echo "⏳ 等待MinIO启动完成..."
MAX_RETRIES=30
RETRY_COUNT=0

while [ $RETRY_COUNT -lt $MAX_RETRIES ]; do
    if curl -s -o /dev/null -w "%{http_code}" http://localhost:$MINIO_API_PORT/minio/health/live | grep -q "200"; then
        echo "✅ MinIO启动成功!"
        echo ""
        break
    fi

    RETRY_COUNT=$((RETRY_COUNT + 1))
    if [ $RETRY_COUNT -eq $MAX_RETRIES ]; then
        echo "❌ MinIO启动超时"
        echo "请查看日志文件: $MINIO_DATA_DIR/minio.log"
        exit 1
    fi

    sleep 1
    echo -n "."
done

# 创建测试bucket
if [ "$MC_AVAILABLE" = true ]; then
    echo "🪣 创建测试bucket..."

    # 配置MinIO别名
    mc alias set local http://localhost:$MINIO_API_PORT $MINIO_ROOT_USER $MINIO_ROOT_PASSWORD --api S3v4 > /dev/null 2>&1

    # 创建bucket（如果已存在则忽略）
    if mc mb local/$BUCKET_NAME --ignore-existing > /dev/null 2>&1; then
        echo "✅ Bucket '$BUCKET_NAME' 已创建"
    else
        echo "✅ Bucket '$BUCKET_NAME' 已存在"
    fi
    echo ""
else
    echo "⚠️  请手动创建bucket:"
    echo "  mc alias set local http://localhost:$MINIO_API_PORT $MINIO_ROOT_USER $MINIO_ROOT_PASSWORD"
    echo "  mc mb local/$BUCKET_NAME"
    echo ""
fi

# 显示访问信息
echo "========================================="
echo "  MinIO已成功启动！"
echo "========================================="
echo ""
echo "📍 访问信息:"
echo "  API端点: http://localhost:$MINIO_API_PORT"
echo "  控制台: http://localhost:$MINIO_CONSOLE_PORT"
echo "  用户名: $MINIO_ROOT_USER"
echo "  密码: $MINIO_ROOT_PASSWORD"
echo ""
echo "🧪 运行集成测试:"
echo "  mvn test -Dtest=MinIOIntegrationTest -pl logx-s3-adapter"
echo ""
echo "🛑 停止MinIO:"
echo "  pkill -f 'minio server'"
echo ""
echo "📄 查看日志:"
echo "  tail -f $MINIO_DATA_DIR/minio.log"
echo ""
echo "========================================="
