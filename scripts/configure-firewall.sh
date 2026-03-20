#!/usr/bin/env bash
set -euo pipefail

echo "=========================================="
echo "配置防火墙规则"
echo "=========================================="
echo ""

# 检查是否有 sudo 权限
if ! sudo -n true 2>/dev/null; then
    echo "错误: 需要 sudo 权限"
    exit 1
fi

# 检查 UFW 是否安装
if ! command -v ufw &> /dev/null; then
    echo "UFW 未安装，正在安装..."
    sudo apt update
    sudo apt install -y ufw
fi

echo "当前防火墙状态:"
sudo ufw status
echo ""

# 询问是否继续
read -p "是否要配置防火墙规则？(y/n): " CONFIRM
if [[ "$CONFIRM" != "y" ]]; then
    echo "已取消"
    exit 0
fi

echo ""
echo "配置防火墙规则..."
echo ""

# 1. 允许 SSH（重要！避免被锁）
echo "1. 开放 SSH 端口 (22)"
sudo ufw allow 22/tcp
echo "   ✓ 已开放端口 22 (SSH)"

# 2. 允许 HTTP
echo ""
echo "2. 开放 HTTP 端口 (80)"
sudo ufw allow 80/tcp
echo "   ✓ 已开放端口 80 (HTTP)"

# 3. 询问是否开放 HTTPS
echo ""
read -p "是否开放 HTTPS 端口 (443)？(y/n) [n]: " HTTPS
if [[ "$HTTPS" == "y" ]]; then
    sudo ufw allow 443/tcp
    echo "   ✓ 已开放端口 443 (HTTPS)"
fi

# 4. 询问是否开放后端直接访问
echo ""
read -p "是否开放后端直接访问端口 (8080)？(y/n) [n]: " BACKEND
if [[ "$BACKEND" == "y" ]]; then
    sudo ufw allow 8080/tcp
    echo "   ✓ 已开放端口 8080 (后端)"
fi

# 5. 启用防火墙
echo ""
echo "启用防火墙..."
sudo ufw --force enable

echo ""
echo "=========================================="
echo "防火墙配置完成"
echo "=========================================="
echo ""
echo "当前规则:"
sudo ufw status numbered
echo ""
echo "管理命令:"
echo "  查看状态: sudo ufw status"
echo "  查看详细: sudo ufw status verbose"
echo "  禁用防火墙: sudo ufw disable"
echo "  重新加载: sudo ufw reload"
echo ""
echo "添加规则:"
echo "  开放端口: sudo ufw allow <port>/tcp"
echo "  关闭端口: sudo ufw deny <port>/tcp"
echo "  删除规则: sudo ufw delete allow <port>/tcp"
echo ""
