#!/usr/bin/env bash
set -euo pipefail

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$PROJECT_ROOT"

echo "=========================================="
echo "InterviewOS 一键部署脚本"
echo "=========================================="

# 检查是否为 root 或有 sudo 权限
if ! sudo -n true 2>/dev/null; then
    echo "此脚本需要 sudo 权限，请确保当前用户在 sudoers 中"
    exit 1
fi

# 询问服务器 IP
read -p "请输入服务器 IP 地址 [默认: $(hostname -I | awk '{print $1}')]: " SERVER_IP
SERVER_IP=${SERVER_IP:-$(hostname -I | awk '{print $1}')}
export SERVER_IP

echo ""
echo "将使用 IP: $SERVER_IP"
echo ""

# 步骤 1: 安装依赖
read -p "是否需要安装依赖软件？(y/n) [n]: " INSTALL_DEPS
if [[ "$INSTALL_DEPS" == "y" ]]; then
    bash scripts/install-dependencies.sh
fi

# 步骤 2: 初始化数据库
read -p "是否需要初始化数据库？(y/n) [n]: " INIT_DB
if [[ "$INIT_DB" == "y" ]]; then
    read -p "请输入数据库密码: " -s POSTGRES_PASSWORD
    echo ""
    export POSTGRES_PASSWORD
    bash scripts/init-database.sh
fi

# 步骤 3: 配置后端环境变量
if [ ! -f backend/.env.production ]; then
    echo ""
    echo "创建后端环境变量文件..."

    # 检查模板文件是否存在
    if [ -f backend/.env.production.template ]; then
        cp backend/.env.production.template backend/.env.production
    elif [ -f backend/.env.example ]; then
        cp backend/.env.example backend/.env.production
    else
        echo "错误: 找不到环境变量模板文件"
        exit 1
    fi

    # 自动替换部分配置
    sed -i "s|POSTGRES_PASSWORD=your_secure_password_here|POSTGRES_PASSWORD=${POSTGRES_PASSWORD:-your_password}|g" backend/.env.production
    sed -i "s|APP_CORS_ALLOWED_ORIGINS=http://your_server_ip|APP_CORS_ALLOWED_ORIGINS=http://$SERVER_IP|g" backend/.env.production

    echo "✓ 已创建 backend/.env.production"
    echo ""
    echo "请编辑该文件并填写以下必需配置："
    echo "  - POSTGRES_PASSWORD (数据库密码)"
    echo "  - OPENAI_API_KEY (OpenAI API 密钥)"
    echo ""
    read -p "按回车继续..."
fi

# 步骤 4: 部署后端
echo ""
echo "部署后端..."
bash scripts/deploy-backend.sh

# 步骤 5: 安装后端服务
bash scripts/install-backend-service.sh

# 步骤 6: 启动后端
sudo systemctl start interviewos-backend
sleep 5

# 检查后端状态
if sudo systemctl is-active --quiet interviewos-backend; then
    echo "✓ 后端服务启动成功"
else
    echo "✗ 后端服务启动失败，请查看日志："
    echo "  sudo journalctl -u interviewos-backend -n 50"
    exit 1
fi

# 步骤 7: 部署前端
echo ""
echo "部署前端..."
bash scripts/deploy-frontend.sh

# 步骤 8: 配置 Nginx
bash scripts/configure-nginx.sh

echo ""
echo "=========================================="
echo "部署完成！"
echo "=========================================="
echo ""
echo "访问地址: http://$SERVER_IP"
echo ""
echo "服务管理命令："
echo "  后端状态: sudo systemctl status interviewos-backend"
echo "  后端日志: sudo journalctl -u interviewos-backend -f"
echo "  后端重启: sudo systemctl restart interviewos-backend"
echo ""
echo "  Nginx 状态: sudo systemctl status nginx"
echo "  Nginx 重启: sudo systemctl reload nginx"
echo ""
echo "日志文件："
echo "  后端日志: /opt/interviewos/logs/backend.log"
echo "  Nginx 日志: /var/log/nginx/access.log"
echo "=========================================="
