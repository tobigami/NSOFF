#!/bin/bash
# Chép lại cơ sở dữ liệu từ bản chạy thật sang bản dev.
#
# Chạy khi muốn dev có dữ liệu mới của người chơi thật. KHÔNG chạy tự động: mỗi lần chép là xoá
# sạch nhân vật và đồ thử nghiệm bên dev.
#
# Chiều duy nhất là prod -> dev. Không có chiều ngược lại, và cũng không nên có: dữ liệu thử
# nghiệm không được phép chảy ngược vào chỗ người ta đang chơi.
set -e
cd "$(dirname "$0")/.."
. ./moi-truong.sh

if [ "$TEN_MT" = "prod" ]; then
  echo "!! Đang đứng ở cây prod. Script này phải chạy trong cây dev."
  exit 1
fi

NGUON="${NGUON:-nso_test}"
echo "Chép $NGUON  ->  $DB_TEN"
echo "  Mọi thứ đang có trong $DB_TEN sẽ MẤT."
mysql -uroot -N -e "SELECT CONCAT('  bên dev đang có ', COUNT(*), ' nhân vật') FROM \`$DB_TEN\`.players;" 2>/dev/null || true
printf "  Gõ 'dong y' để tiếp tục: "
read -r tra
[ "$tra" = "dong y" ] || { echo "  đã huỷ."; exit 1; }

# Dừng máy chủ dev trước: nó đang giữ nhân vật trong bộ nhớ, để chạy tiếp thì lúc thoát nó ghi đè
# lên bản vừa chép.
pm2 stop "$PM2_TEN" > /dev/null 2>&1 || true

mysql -uroot -e "DROP DATABASE IF EXISTS \`$DB_TEN\`;
  CREATE DATABASE \`$DB_TEN\` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
mysqldump -uroot --single-transaction --routines --triggers "$NGUON" | mysql -uroot "$DB_TEN"

echo "  xong: $(mysql -uroot -N -e "SELECT COUNT(*) FROM \`$DB_TEN\`.players") nhân vật," \
     "$(mysql -uroot -N -e "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='$DB_TEN'") bảng"
pm2 start "$PM2_TEN" > /dev/null 2>&1 || pm2 start ecosystem.config.js > /dev/null 2>&1
echo "  đã bật lại $PM2_TEN"
