#!/bin/bash
# Mở thư mục dist/share cho bạn bè tải, chạy ở tiền cảnh cho pm2 quản.
#
#   SHARE_MODE=ts    (mặc định) gắn vào IP Tailscale -- bạn bè ở xa, qua tailnet
#   SHARE_MODE=lan              gắn vào IP mạng nội bộ -- bạn bè ngồi cùng wifi
#
# Cả hai chế độ đều gắn vào **một địa chỉ cụ thể**, không bao giờ 0.0.0.0. Tailscale thì hiển
# nhiên. Còn LAN: gắn 0.0.0.0 nghĩa là hễ máy cắm vào mạng nào -- wifi quán cà phê, wifi khách sạn
# -- là cả mạng đó thấy thư mục, mà mình không hề biết. Gắn đúng IP của giao diện đang dùng thì
# đổi mạng là cổng tự chết theo, phải chạy lại mới mở, tức là có chủ ý.
#
# Địa chỉ tra lúc chạy chứ không ghi cứng: IP Tailscale do tailnet cấp, IP nội bộ do bộ định tuyến
# cấp theo DHCP nên đổi sau mỗi lần khởi động lại là chuyện thường.
set -e
cd "$(dirname "$0")/.."

MODE=${SHARE_MODE:-ts}
case "$MODE" in
  ts)
    PORT=${SHARE_PORT:-8080}
    IP=$(tailscale ip -4 2>/dev/null || true)
    [ -n "$IP" ] || { echo "Tailscale chưa chạy -- không có IP để gắn vào"; exit 1; }
    ;;
  lan)
    PORT=${SHARE_PORT:-8081}
    # Lấy giao diện của tuyến mặc định, không đoán en0: trên máy này en0 là dây còn en1 mới là wifi.
    IFACE=$(route -n get default 2>/dev/null | awk '/interface:/{print $2}')
    IP=$(ipconfig getifaddr "$IFACE" 2>/dev/null || true)
    [ -n "$IP" ] || { echo "Không tra được IP nội bộ (giao diện '$IFACE') -- máy chưa vào mạng?"; exit 1; }
    ;;
  *)
    echo "SHARE_MODE phải là ts hoặc lan, nhận được '$MODE'"; exit 1
    ;;
esac

echo "phát tệp $MODE: http://$IP:$PORT/"
# Dùng share_server.py chứ không phải python3 -m http.server: bản có sẵn gửi .txt không kèm
# bảng mã, trình duyệt đoán ra latin-1 và tiếng Việt trong PHIEN-BAN.txt vỡ hết.
exec python3 tools/share_server.py "$IP" "$PORT" dist/share
