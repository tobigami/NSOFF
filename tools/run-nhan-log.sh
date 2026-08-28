#!/bin/bash
# Chạy chỗ nhận log ở tiền cảnh cho pm2 quản. Gắn vào IP Tailscale, tra lúc chạy.
set -e
cd "$(dirname "$0")/.."
PORT=${LOG_PORT:-8090}
IP=$(tailscale ip -4 2>/dev/null || true)
[ -n "$IP" ] || { echo "Tailscale chưa chạy -- không có IP để gắn vào"; exit 1; }
exec python3 tools/nhan-log.py "$IP" "$PORT"
