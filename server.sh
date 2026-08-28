#!/bin/bash
# Bật/tắt/khởi động lại máy chủ Ninja School.
#
# Nút "Bảo trì" trong cửa sổ quản lý gọi System.exit(0), tức là cả JVM biến mất -- không còn cửa sổ
# nào để bấm bật lại. Script này là đường vào từ bên ngoài cho đúng lúc đó.
#
#   ./server.sh start | stop | restart | status | log
set -u
cd "$(dirname "$0")"

SRV=work/server/NSO_KEM
JAR=target/Nso-jar-with-dependencies.jar
LOG=build/server.log
PORT=14444
PATTERN="Nso-jar-with-dependencies"

pid() { pgrep -f "$PATTERN" | head -1; }

listening() { lsof -nP -iTCP:$PORT -sTCP:LISTEN >/dev/null 2>&1; }

do_start() {
  if [ -n "$(pid)" ]; then
    echo "máy chủ đã chạy (pid $(pid))"
    return 0
  fi
  # pm2 đang quản thì bật tay nữa là có hai máy chủ giành cổng 14444.
  if command -v pm2 >/dev/null 2>&1 && pm2 pid nso >/dev/null 2>&1 \
     && [ -n "$(pm2 pid nso 2>/dev/null | tr -d '[:space:]')" ]; then
    echo "pm2 đang quản máy chủ -- dùng: pm2 start ecosystem.config.js"
    return 1
  fi
  if listening; then
    echo "cổng $PORT đang bị tiến trình khác giữ -- không khởi động"
    return 1
  fi
  # Cờ online còn sót lại từ lần tắt không sạch sẽ chặn đăng nhập, dọn trước cho chắc.
  /opt/homebrew/opt/mariadb/bin/mariadb -uroot nso_test \
      -e "UPDATE players SET online=0 WHERE online<>0;" 2>/dev/null

  ( cd "$SRV" && nohup java -server -Dfile.encoding=UTF-8 -Xms1G -Xmx2G -jar "$JAR" \
      >> "../../../$LOG" 2>&1 & disown )
  echo -n "đang khởi động"
  for i in $(seq 1 40); do
    sleep 1
    echo -n "."
    if listening; then
      echo " xong (pid $(pid)), nghe cổng $PORT"
      return 0
    fi
  done
  echo " KHÔNG lên -- xem $LOG"
  return 1
}

do_stop() {
  local p
  p="$(pid)"
  if [ -z "$p" ]; then
    echo "máy chủ không chạy"
    return 0
  fi
  # SIGTERM trước để JVM còn kịp chạy shutdown hook, cứng tay chỉ khi nó lì.
  kill "$p" 2>/dev/null
  for i in $(seq 1 15); do
    sleep 1
    [ -z "$(pid)" ] && { echo "đã dừng"; return 0; }
  done
  kill -9 "$p" 2>/dev/null
  sleep 2
  [ -z "$(pid)" ] && echo "đã dừng (phải kill -9)" || echo "KHÔNG dừng được (pid $p)"
}

case "${1:-status}" in
  start)   do_start ;;
  stop)    do_stop ;;
  restart) do_stop; sleep 2; do_start ;;
  status)
    p="$(pid)"
    if [ -n "$p" ]; then
      echo "đang chạy: pid $p, khởi động lúc $(ps -o lstart= -p "$p")"
      listening && echo "cổng $PORT: đang nghe" || echo "cổng $PORT: CHƯA nghe"
      echo -n "người chơi online: "
      /opt/homebrew/opt/mariadb/bin/mariadb -uroot nso_test -N -B \
          -e "SELECT COUNT(*) FROM players WHERE online=1;" 2>/dev/null
    else
      echo "không chạy"
    fi ;;
  log)     tail -f "$LOG" ;;
  *)       echo "dùng: ./server.sh start|stop|restart|status|log"; exit 1 ;;
esac
