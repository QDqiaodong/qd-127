#!/bin/bash

set -e

echo "=========================================="
echo "  商业步行街公共休息长凳管理系统"
echo "  启动脚本"
echo "=========================================="

if [ ! -f ".env" ]; then
    echo "未找到 .env 文件，使用默认配置"
else
    set -a
    source .env
    set +a
fi

echo ""
echo "启动服务..."
echo ""

docker compose up -d --build

echo ""
echo "等待服务启动..."
sleep 30

echo ""
echo "检查服务状态..."
docker compose ps

echo ""
echo "=========================================="
echo "  服务启动完成"
echo "=========================================="
echo ""
echo "前端地址: http://localhost:${FRONTEND_PORT:-8227}"
echo "后端API: http://localhost:${BACKEND_PORT:-8327}"
echo "MySQL: localhost:${MYSQL_PORT:-3527}"
echo "Redis: localhost:${REDIS_PORT:-6627}"
echo ""
