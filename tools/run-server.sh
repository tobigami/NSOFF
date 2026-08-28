#!/bin/bash
# Chạy máy chủ ở tiền cảnh cho pm2 quản. Không tự tách tiến trình -- pm2 cần giữ được tiến trình
# con thì mới theo dõi và bật lại được.
#
# Việc riêng của script này so với gọi thẳng java: dọn cờ online còn sót. Lần tắt nào không sạch
# cũng để lại players.online = 1, và cờ đó chặn đăng nhập ở lần bật kế tiếp.
set -e
cd "$(dirname "$0")/.."

# prod hay dev -- xem moi-truong.sh
. ./moi-truong.sh

MARIADB=/opt/homebrew/opt/mariadb/bin/mariadb
"$MARIADB" -uroot "$DB_TEN" -e "UPDATE players SET online=0 WHERE online<>0;" 2>/dev/null || true

cd work/server/NSO_KEM
exec java -server -Dfile.encoding=UTF-8 -Xms1G -Xmx2G -jar target/Nso-jar-with-dependencies.jar
