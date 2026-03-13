#!/usr/bin/env bash
set -euo pipefail

DB_NAME="interview_os"
DB_USER="interview_user"
DB_PASSWORD="${POSTGRES_PASSWORD:-interview_password_change_me}"

echo "创建数据库和用户..."

sudo -u postgres psql <<EOF
-- 创建用户
CREATE USER $DB_USER WITH PASSWORD '$DB_PASSWORD';

-- 创建数据库
CREATE DATABASE $DB_NAME OWNER $DB_USER;

-- 授予权限
GRANT ALL PRIVILEGES ON DATABASE $DB_NAME TO $DB_USER;

-- 连接到数据库并授予 schema 权限
\c $DB_NAME
GRANT ALL ON SCHEMA public TO $DB_USER;

-- 安装 pgvector 扩展（可选，为未来功能准备）
-- CREATE EXTENSION IF NOT EXISTS vector;

\q
EOF

echo "✓ 数据库初始化完成"
echo "  数据库名: $DB_NAME"
echo "  用户名: $DB_USER"
echo "  密码: $DB_PASSWORD"
