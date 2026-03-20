#!/usr/bin/env bash
set -euo pipefail

NGINX_CONF="/etc/nginx/sites-available/interviewos"
ALLOWED_IP="${1:-}"

echo "=========================================="
echo "配置 API IP 白名单"
echo "=========================================="
echo ""

if [ -z "$ALLOWED_IP" ]; then
    echo "用法: $0 <允许的IP地址>"
    echo "示例: $0 60.176.133.173"
    exit 1
fi

echo "将允许以下 IP 访问 API:"
echo "  - $ALLOWED_IP"
echo "  - 127.0.0.1 (本地)"
echo ""

read -p "是否继续？(y/n): " CONFIRM
if [[ "$CONFIRM" != "y" ]]; then
    echo "已取消"
    exit 0
fi

# 备份当前配置
echo "备份当前配置..."
if [ -f "$NGINX_CONF" ]; then
    sudo cp "$NGINX_CONF" "$NGINX_CONF.backup.$(date +%Y%m%d_%H%M%S)"
fi

# 删除旧的符号链接（如果存在）
if [ -L /etc/nginx/sites-enabled/interviewos ]; then
    sudo rm /etc/nginx/sites-enabled/interviewos
fi

# 创建新配置
echo "创建新配置..."
sudo bash -c "cat > $NGINX_CONF" <<EOF
server {
    listen 80;
    server_name _;

    root /var/www/interviewos;
    index index.html;

    # 前端路由（SPA）- 所有人可访问
    location / {
        try_files \$uri \$uri/ /index.html;
    }

    # 代理后端 API - IP 白名单保护
    location /api/ {
        # IP 白名单
        allow $ALLOWED_IP;
        allow 127.0.0.1;
        deny all;

        proxy_pass http://localhost:8080/api/;
        proxy_http_version 1.1;
        proxy_set_header Upgrade \$http_upgrade;
        proxy_set_header Connection 'upgrade';
        proxy_set_header Host \$host;
        proxy_cache_bypass \$http_upgrade;
        proxy_set_header X-Real-IP \$remote_addr;
        proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto \$scheme;

        # 超时设置
        proxy_connect_timeout 60s;
        proxy_send_timeout 60s;
        proxy_read_timeout 60s;
    }

    # 静态资源缓存
    location ~* \.(js|css|png|jpg|jpeg|gif|ico|svg|woff|woff2|ttf|eot)$ {
        expires 1y;
        add_header Cache-Control "public, immutable";
    }

    # Gzip 压缩
    gzip on;
    gzip_vary on;
    gzip_min_length 1024;
    gzip_types text/plain text/css application/json application/javascript text/xml application/xml application/xml+rss text/javascript;

    # 安全头
    add_header X-Frame-Options "SAMEORIGIN" always;
    add_header X-Content-Type-Options "nosniff" always;
    add_header X-XSS-Protection "1; mode=block" always;
}
EOF

# 重新创建符号链接
echo "创建符号链接..."
sudo ln -sf "$NGINX_CONF" /etc/nginx/sites-enabled/interviewos

# 测试配置
echo ""
echo "测试 Nginx 配置..."
if sudo nginx -t; then
    echo ""
    echo "✓ 配置测试通过"

    # 重载 Nginx
    echo "重载 Nginx..."
    sudo systemctl reload nginx

    echo ""
    echo "=========================================="
    echo "IP 白名单配置完成！"
    echo "=========================================="
    echo ""
    echo "允许访问的 IP:"
    echo "  - $ALLOWED_IP"
    echo "  - 127.0.0.1"
    echo ""
    echo "其他 IP 将无法访问 /api/ 路径"
    echo ""
    echo "如需添加更多 IP，编辑配置文件:"
    echo "  sudo vim $NGINX_CONF"
    echo ""
    echo "在 location /api/ 块中添加:"
    echo "  allow 新的IP地址;"
    echo ""
    echo "然后重载 Nginx:"
    echo "  sudo nginx -t"
    echo "  sudo systemctl reload nginx"
else
    echo ""
    echo "✗ 配置测试失败"
    echo ""
    echo "恢复备份:"
    echo "  sudo cp $NGINX_CONF.backup.* $NGINX_CONF"
    echo "  sudo systemctl reload nginx"
    exit 1
fi
