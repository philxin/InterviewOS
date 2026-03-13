#!/usr/bin/env bash
set -euo pipefail

echo "=========================================="
echo "InterviewOS 部署检查脚本"
echo "=========================================="
echo ""

DEPLOY_DIR="/opt/interviewos"
NGINX_ROOT="/var/www/interviewos"

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

check_pass() {
    echo -e "${GREEN}✓${NC} $1"
}

check_fail() {
    echo -e "${RED}✗${NC} $1"
}

check_warn() {
    echo -e "${YELLOW}⚠${NC} $1"
}

# 1. 检查系统依赖
echo "1. 检查系统依赖"
echo "-------------------"

if command -v java &> /dev/null && java -version 2>&1 | grep -q "21"; then
    check_pass "Java 21 已安装"
else
    check_fail "Java 21 未安装或版本不正确"
fi

if command -v node &> /dev/null; then
    check_pass "Node.js 已安装 ($(node -v))"
else
    check_fail "Node.js 未安装"
fi

if command -v psql &> /dev/null; then
    check_pass "PostgreSQL 已安装"
else
    check_fail "PostgreSQL 未安装"
fi

if command -v redis-cli &> /dev/null; then
    check_pass "Redis 已安装"
else
    check_fail "Redis 未安装"
fi

if command -v nginx &> /dev/null; then
    check_pass "Nginx 已安装"
else
    check_fail "Nginx 未安装"
fi

echo ""

# 2. 检查服务状态
echo "2. 检查服务状态"
echo "-------------------"

if sudo systemctl is-active --quiet postgresql; then
    check_pass "PostgreSQL 服务运行中"
else
    check_fail "PostgreSQL 服务未运行"
fi

if sudo systemctl is-active --quiet redis-server; then
    check_pass "Redis 服务运行中"
else
    check_fail "Redis 服务未运行"
fi

if sudo systemctl is-active --quiet nginx; then
    check_pass "Nginx 服务运行中"
else
    check_fail "Nginx 服务未运行"
fi

if sudo systemctl is-active --quiet interviewos-backend; then
    check_pass "InterviewOS 后端服务运行中"
else
    check_fail "InterviewOS 后端服务未运行"
fi

echo ""

# 3. 检查部署文件
echo "3. 检查部署文件"
echo "-------------------"

if [ -f "$DEPLOY_DIR/backend/app.jar" ]; then
    check_pass "后端 JAR 文件存在"
else
    check_fail "后端 JAR 文件不存在"
fi

if [ -f "$DEPLOY_DIR/backend/.env" ]; then
    check_pass "后端环境变量文件存在"
else
    check_fail "后端环境变量文件不存在"
fi

if [ -d "$NGINX_ROOT" ] && [ -f "$NGINX_ROOT/index.html" ]; then
    check_pass "前端文件已部署"
else
    check_fail "前端文件未部署"
fi

if [ -f "/etc/nginx/sites-available/interviewos" ]; then
    check_pass "Nginx 配置文件存在"
else
    check_fail "Nginx 配置文件不存在"
fi

echo ""

# 4. 检查端口占用
echo "4. 检查端口占用"
echo "-------------------"

if sudo netstat -tlnp 2>/dev/null | grep -q ":8080"; then
    check_pass "端口 8080 已被占用（后端）"
else
    check_warn "端口 8080 未被占用"
fi

if sudo netstat -tlnp 2>/dev/null | grep -q ":80"; then
    check_pass "端口 80 已被占用（Nginx）"
else
    check_warn "端口 80 未被占用"
fi

if sudo netstat -tlnp 2>/dev/null | grep -q ":5432"; then
    check_pass "端口 5432 已被占用（PostgreSQL）"
else
    check_warn "端口 5432 未被占用"
fi

if sudo netstat -tlnp 2>/dev/null | grep -q ":6379"; then
    check_pass "端口 6379 已被占用（Redis）"
else
    check_warn "端口 6379 未被占用"
fi

echo ""

# 5. 测试数据库连接
echo "5. 测试数据库连接"
echo "-------------------"

if sudo -u postgres psql -lqt 2>/dev/null | cut -d \| -f 1 | grep -qw interview_os; then
    check_pass "数据库 interview_os 存在"
else
    check_fail "数据库 interview_os 不存在"
fi

echo ""

# 6. 测试 API 连接
echo "6. 测试 API 连接"
echo "-------------------"

if curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/api/knowledge | grep -q "200"; then
    check_pass "后端 API 响应正常"
else
    check_warn "后端 API 无响应或返回错误"
fi

echo ""

# 7. 检查日志文件
echo "7. 检查日志文件"
echo "-------------------"

if [ -f "$DEPLOY_DIR/logs/backend.log" ]; then
    check_pass "后端日志文件存在"
    echo "   最后 5 行日志："
    tail -n 5 "$DEPLOY_DIR/logs/backend.log" | sed 's/^/   /'
else
    check_warn "后端日志文件不存在"
fi

echo ""

# 8. 检查 Nginx 配置
echo "8. 检查 Nginx 配置"
echo "-------------------"

if sudo nginx -t &> /dev/null; then
    check_pass "Nginx 配置语法正确"
else
    check_fail "Nginx 配置语法错误"
    sudo nginx -t 2>&1 | sed 's/^/   /'
fi

echo ""
echo "=========================================="
echo "检查完成"
echo "=========================================="
echo ""
echo "如果发现问题，请查看："
echo "  - 后端日志: sudo journalctl -u interviewos-backend -n 50"
echo "  - Nginx 日志: sudo tail -f /var/log/nginx/error.log"
echo "  - PostgreSQL 日志: sudo tail -f /var/log/postgresql/postgresql-*-main.log"
