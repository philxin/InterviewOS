#!/usr/bin/env bash
set -euo pipefail

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
BACKEND_DIR="$PROJECT_ROOT/backend"
DEPLOY_DIR="/opt/interviewos"
ENV_FILE="$BACKEND_DIR/.env.production"

echo "=========================================="
echo "后端部署脚本"
echo "=========================================="

# 检查环境变量文件
if [ ! -f "$ENV_FILE" ]; then
    echo "错误: $ENV_FILE 不存在"
    echo "请先创建并配置环境变量文件"
    exit 1
fi

# 进入后端目录
cd "$BACKEND_DIR"

# 确保 mvnw 有执行权限
chmod +x mvnw

# 构建项目（不需要加载环境变量）
echo "构建后端项目..."
./mvnw clean package -DskipTests

# 创建部署目录
sudo mkdir -p "$DEPLOY_DIR/backend"
sudo mkdir -p "$DEPLOY_DIR/logs"

# 复制 jar 文件
echo "复制文件到部署目录..."
sudo cp target/InterviewOS-0.0.1-SNAPSHOT.jar "$DEPLOY_DIR/backend/app.jar"
sudo cp "$ENV_FILE" "$DEPLOY_DIR/backend/.env"

# 设置权限
sudo chown -R $USER:$USER "$DEPLOY_DIR"

echo "✓ 后端部署完成"
echo "  部署目录: $DEPLOY_DIR/backend"
echo "  JAR 文件: $DEPLOY_DIR/backend/app.jar"
