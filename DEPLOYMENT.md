# InterviewOS 部署指南

## 概述

本指南提供 InterviewOS 在 Ubuntu/Debian 服务器上的传统部署方案（非 Docker）。

## 系统要求

- 操作系统：Ubuntu 20.04+ 或 Debian 11+
- 内存：至少 2GB RAM
- 磁盘：至少 10GB 可用空间
- 网络：可访问互联网（用于安装依赖和调用 OpenAI API）

## 软件依赖

- Java 21
- Node.js 20
- PostgreSQL 14+
- Redis 6+
- Nginx

## 快速部署

### 0. 系统准备（可选但推荐）

```bash
# 更新系统包
sudo apt update && sudo apt upgrade -y

# 如果看到 "Daemons using outdated libraries" 警告
# 重启相关服务或重启系统
sudo reboot
```

### 1. 上传代码到服务器

**推荐路径**：将代码放在用户主目录下，例如 `/home/your_username/InterviewOS`

```bash
# 方式 1: 使用 git clone（推荐）
cd ~
git clone <your-repo-url>
cd InterviewOS

# 方式 2: 使用 rsync 上传
# 在本地执行：
rsync -avz --exclude 'node_modules' --exclude 'target' \
  ./ user@server_ip:~/InterviewOS/

# 然后在服务器上：
cd ~/InterviewOS
```

**注意**：
- 代码可以放在任意目录，不影响部署（部署后的文件会复制到 `/opt/interviewos`）
- 建议使用用户主目录（`~`），避免权限问题
- 确保目录有足够的磁盘空间（至少 2GB）

### 2. 执行一键部署脚本

```bash
# 确保在项目根目录
cd ~/InterviewOS

# 给脚本添加执行权限
chmod +x deploy.sh
chmod +x scripts/*.sh

# 执行部署
./deploy.sh
```

脚本会引导你完成以下步骤：
1. 安装系统依赖（Java、Node.js、PostgreSQL、Redis、Nginx）
2. 初始化数据库
3. 配置后端环境变量
4. 构建并部署后端
5. 安装 systemd 服务
6. 构建并部署前端
7. 配置 Nginx

### 3. 配置环境变量

部署脚本会创建 `backend/.env.production` 文件，你需要手动编辑以下关键配置：

```bash
vim backend/.env.production
```

必须配置的项：
- `POSTGRES_PASSWORD`: 数据库密码
- `OPENAI_API_KEY`: OpenAI API 密钥
- `APP_CORS_ALLOWED_ORIGINS`: 前端访问地址（格式：http://your_server_ip）

### 4. 访问应用

部署完成后，在浏览器访问：
```
http://your_server_ip
```

## 手动部署步骤

如果需要分步执行，可以使用以下命令：

### 1. 安装依赖

```bash
bash scripts/install-dependencies.sh
```

### 2. 初始化数据库

```bash
export POSTGRES_PASSWORD="your_secure_password"
bash scripts/init-database.sh
```

### 3. 配置后端

```bash
# 复制模板文件
cp backend/.env.production.template backend/.env.production

# 编辑配置
vim backend/.env.production
```

### 4. 部署后端

```bash
bash scripts/deploy-backend.sh
bash scripts/install-backend-service.sh
sudo systemctl start interviewos-backend
```

### 5. 部署前端

```bash
export SERVER_IP="your_server_ip"
bash scripts/deploy-frontend.sh
```

### 6. 配置 Nginx

```bash
bash scripts/configure-nginx.sh
```

## 服务管理

### 后端服务

```bash
# 启动服务
sudo systemctl start interviewos-backend

# 停止服务
sudo systemctl stop interviewos-backend

# 重启服务
sudo systemctl restart interviewos-backend

# 查看状态
sudo systemctl status interviewos-backend

# 查看日志
sudo journalctl -u interviewos-backend -f

# 查看最近 100 行日志
sudo journalctl -u interviewos-backend -n 100 --no-pager
```

### Nginx 服务

```bash
# 重载配置
sudo systemctl reload nginx

# 重启服务
sudo systemctl restart nginx

# 查看状态
sudo systemctl status nginx

# 测试配置
sudo nginx -t
```

## 日志文件

- 后端应用日志：`/opt/interviewos/logs/backend.log`
- 后端错误日志：`/opt/interviewos/logs/backend-error.log`
- Nginx 访问日志：`/var/log/nginx/access.log`
- Nginx 错误日志：`/var/log/nginx/error.log`
- PostgreSQL 日志：`/var/log/postgresql/postgresql-*-main.log`

## 更新部署

代码更新后重新部署：

```bash
# 进入项目目录
cd ~/InterviewOS

# 拉取最新代码
git pull

# 重新部署后端
bash scripts/deploy-backend.sh
sudo systemctl restart interviewos-backend

# 重新部署前端
export SERVER_IP="your_server_ip"
bash scripts/deploy-frontend.sh
```

## 验证部署

### 1. 检查后端服务

```bash
# 检查服务状态
sudo systemctl status interviewos-backend

# 测试 API
curl http://localhost:8080/api/knowledge
```

预期返回 JSON 响应。

### 2. 检查前端

```bash
# 测试前端文件
curl http://localhost/

# 在浏览器访问
http://your_server_ip
```

### 3. 检查数据库连接

```bash
# 连接数据库
psql -h localhost -U interview_user -d interview_os

# 查看表
\dt
```

### 4. 检查日志

```bash
# 查看后端日志，确认无错误
tail -f /opt/interviewos/logs/backend.log

# 查看 Nginx 错误日志
sudo tail -f /var/log/nginx/error.log
```

## 常见问题

### "Daemons using outdated libraries" 警告

这表示某些系统服务正在使用过期的库文件。

**解决方法**：
```bash
# 方法 1: 重启相关服务
sudo systemctl restart postgresql redis-server nginx

# 方法 2: 重启系统（最彻底）
sudo reboot
```

这不会影响部署，但建议在部署前处理。

### 后端无法启动

```bash
# 查看详细日志
sudo journalctl -u interviewos-backend -n 100 --no-pager

# 检查端口占用
sudo netstat -tlnp | grep 8080

# 检查环境变量
cat /opt/interviewos/backend/.env

# 手动测试启动
cd /opt/interviewos/backend
source load-env.sh .env
java -jar app.jar
```

### 前端无法访问后端（CORS 错误）

```bash
# 检查 CORS 配置
grep CORS backend/.env.production

# 确保配置正确
APP_CORS_ALLOWED_ORIGINS=http://your_server_ip

# 重启后端
sudo systemctl restart interviewos-backend
```

### 数据库连接失败

```bash
# 测试数据库连接
psql -h localhost -U interview_user -d interview_os

# 检查 PostgreSQL 状态
sudo systemctl status postgresql

# 查看 PostgreSQL 日志
sudo tail -f /var/log/postgresql/postgresql-*-main.log

# 检查数据库配置
grep POSTGRES backend/.env.production
```

### Nginx 配置错误

```bash
# 测试配置
sudo nginx -t

# 查看错误日志
sudo tail -f /var/log/nginx/error.log

# 检查配置文件
cat /etc/nginx/sites-available/interviewos
```

### 端口被占用

```bash
# 检查 8080 端口
sudo netstat -tlnp | grep 8080

# 检查 80 端口
sudo netstat -tlnp | grep :80

# 如果需要，停止占用端口的进程
sudo kill <PID>
```

## 安全建议

1. **修改默认密码**：确保修改数据库密码和其他敏感配置
2. **配置防火墙**：
   ```bash
   sudo ufw allow 80/tcp
   sudo ufw allow 22/tcp
   sudo ufw enable
   ```
3. **定期更新**：保持系统和依赖包更新
4. **备份数据库**：
   ```bash
   pg_dump -U interview_user interview_os > backup.sql
   ```
5. **使用 HTTPS**：生产环境建议配置 SSL 证书（Let's Encrypt）

## 关键文件路径

- 后端部署目录：`/opt/interviewos/backend/`
- 后端 JAR 文件：`/opt/interviewos/backend/app.jar`
- 后端环境变量：`/opt/interviewos/backend/.env`
- 前端部署目录：`/var/www/interviewos/`
- Nginx 配置：`/etc/nginx/sites-available/interviewos`
- systemd 服务：`/etc/systemd/system/interviewos-backend.service`

## 性能优化

### 1. JVM 参数调优

编辑 systemd 服务文件：
```bash
sudo vim /etc/systemd/system/interviewos-backend.service
```

修改 ExecStart 行：
```ini
ExecStart=/usr/bin/java -Xms512m -Xmx1024m -jar /opt/interviewos/backend/app.jar
```

重载并重启：
```bash
sudo systemctl daemon-reload
sudo systemctl restart interviewos-backend
```

### 2. PostgreSQL 优化

编辑 PostgreSQL 配置：
```bash
sudo vim /etc/postgresql/*/main/postgresql.conf
```

建议配置（根据服务器内存调整）：
```
shared_buffers = 256MB
effective_cache_size = 1GB
maintenance_work_mem = 64MB
```

重启 PostgreSQL：
```bash
sudo systemctl restart postgresql
```

### 3. Redis 持久化

编辑 Redis 配置：
```bash
sudo vim /etc/redis/redis.conf
```

启用 AOF：
```
appendonly yes
appendfsync everysec
```

重启 Redis：
```bash
sudo systemctl restart redis-server
```

## 监控建议

1. **系统资源监控**：使用 `htop` 或 `glances`
2. **应用日志监控**：使用 `tail -f` 或配置日志聚合工具
3. **数据库监控**：使用 `pg_stat_activity` 查看活动连接
4. **API 监控**：配置健康检查端点监控

## 支持

如遇问题，请检查：
1. 日志文件（后端、Nginx、PostgreSQL）
2. 服务状态（systemctl status）
3. 网络连接（防火墙、端口）
4. 配置文件（环境变量、Nginx）

更多信息请参考项目文档或提交 Issue。
