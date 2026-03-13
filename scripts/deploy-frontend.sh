#!/usr/bin/env bash
set -euo pipefail

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
FRONTEND_DIR="$PROJECT_ROOT/frontend"
NGINX_ROOT="/var/www/interviewos"
SERVER_IP="${SERVER_IP:-localhost}"

echo "=========================================="
echo "前端部署脚本"
echo "=========================================="

# 进入前端目录
cd "$FRONTEND_DIR"

# 配置生产环境 API 地址
echo "配置 API 地址..."
cat > .env.production <<EOF
VITE_API_BASE_URL=http://$SERVER_IP:8080/api
EOF

echo "API 地址: http://$SERVER_IP:8080/api"

# 安装依赖
echo "安装依赖..."
npm install

# 构建项目
echo "构建前端项目..."
npm run build

# 创建 Nginx 目录
sudo mkdir -p "$NGINX_ROOT"

# 复制构建文件
echo "复制文件到 Nginx 目录..."
sudo cp -r dist/* "$NGINX_ROOT/"

# 设置权限
sudo chown -R www-data:www-data "$NGINX_ROOT"

echo "✓ 前端部署完成"
echo "  部署目录: $NGINX_ROOT"
