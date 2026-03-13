#!/usr/bin/env bash
set -euo pipefail

SERVICE_FILE="/etc/systemd/system/interviewos-backend.service"
DEPLOY_DIR="/opt/interviewos"

# 自动检测 Java 路径
JAVA_PATH=$(which java)
if [ -z "$JAVA_PATH" ]; then
    echo "错误: 找不到 Java 可执行文件"
    echo "请确保 Java 已安装并在 PATH 中"
    exit 1
fi

echo "检测到 Java 路径: $JAVA_PATH"
java -version

echo "创建 systemd 服务..."

sudo tee "$SERVICE_FILE" > /dev/null <<EOF
[Unit]
Description=InterviewOS Backend Service
After=network.target postgresql.service redis-server.service
Wants=postgresql.service redis-server.service

[Service]
Type=simple
User=$USER
WorkingDirectory=$DEPLOY_DIR/backend
EnvironmentFile=$DEPLOY_DIR/backend/.env
ExecStart=$JAVA_PATH -Xms512m -Xmx1024m -jar $DEPLOY_DIR/backend/app.jar
StandardOutput=append:$DEPLOY_DIR/logs/backend.log
StandardError=append:$DEPLOY_DIR/logs/backend-error.log
Restart=on-failure
RestartSec=10
SuccessExitStatus=143

# 安全设置
NoNewPrivileges=true
PrivateTmp=true

[Install]
WantedBy=multi-user.target
EOF

# 重载 systemd
sudo systemctl daemon-reload

# 启用服务
sudo systemctl enable interviewos-backend

echo "✓ systemd 服务已创建"
echo ""
echo "使用以下命令管理服务："
echo "  启动: sudo systemctl start interviewos-backend"
echo "  停止: sudo systemctl stop interviewos-backend"
echo "  重启: sudo systemctl restart interviewos-backend"
echo "  状态: sudo systemctl status interviewos-backend"
echo "  日志: sudo journalctl -u interviewos-backend -f"
