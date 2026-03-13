#!/usr/bin/env bash
set -euo pipefail

echo "=========================================="
echo "InterviewOS 依赖安装脚本（Ubuntu/Debian）"
echo "=========================================="

# 更新包索引
sudo apt update

# 安装 Java 21
if ! command -v java &> /dev/null || ! java -version 2>&1 | grep -q "21"; then
    echo "安装 Java 21..."
    sudo apt install -y openjdk-21-jdk
else
    echo "✓ Java 21 已安装"
fi

# 安装 Node.js 20
if ! command -v node &> /dev/null || ! node -v | grep -q "v20"; then
    echo "安装 Node.js 20..."
    curl -fsSL https://deb.nodesource.com/setup_20.x | sudo -E bash -
    sudo apt install -y nodejs
else
    echo "✓ Node.js 已安装"
fi

# 安装 PostgreSQL 14+
if ! command -v psql &> /dev/null; then
    echo "安装 PostgreSQL..."
    sudo apt install -y postgresql postgresql-contrib
    sudo systemctl enable postgresql
    sudo systemctl start postgresql
else
    echo "✓ PostgreSQL 已安装"
fi

# 安装 Redis
if ! command -v redis-cli &> /dev/null; then
    echo "安装 Redis..."
    sudo apt install -y redis-server
    sudo systemctl enable redis-server
    sudo systemctl start redis-server
else
    echo "✓ Redis 已安装"
fi

# 安装 Nginx
if ! command -v nginx &> /dev/null; then
    echo "安装 Nginx..."
    sudo apt install -y nginx
    sudo systemctl enable nginx
else
    echo "✓ Nginx 已安装"
fi

echo ""
echo "=========================================="
echo "依赖安装完成！"
echo "=========================================="
java -version
node -v
psql --version
redis-cli --version
nginx -v
