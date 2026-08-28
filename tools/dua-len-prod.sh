#!/bin/bash
# Đưa mã và dữ liệu game từ bản dev sang bản chạy thật.
#
# Chép những gì:
#   src/           mã nguồn máy chủ
#   Data/          ảnh, bản đồ -- thứ dev hay thêm vào
#   tools/ mod/ mod-src/   công cụ và bản vá client
#   ba số phiên bản trong config.properties (data / item / skill)
#
# KHÔNG chép, và không được phép chép:
#   mysql.properties      tên cơ sở dữ liệu
#   moi-truong.sh         danh tính cây nguồn
#   ecosystem.config.js   tên tiến trình pm2 và cổng
#   phần cổng trong config.properties
#
# Thay đổi trong CƠ SỞ DỮ LIỆU (vật phẩm, kỹ năng, cửa hàng) KHÔNG tự sang được. Script chỉ nhắc,
# vì mỗi thay đổi mỗi khác: có cái là thêm dòng, có cái là sửa dòng đang có, chép cả bảng thì đè
# mất đồ của người chơi.
set -e
cd "$(dirname "$0")/.."
. ./moi-truong.sh

if [ "$TEN_MT" = "prod" ]; then
  echo "!! Đang đứng ở cây prod. Script này phải chạy trong cây dev."
  exit 1
fi

PROD="${PROD:-/Users/avada/Code/NSOFF}"
[ -f "$PROD/moi-truong.sh" ] || { echo "!! không thấy $PROD/moi-truong.sh"; exit 1; }
grep -q 'TEN_MT="prod"' "$PROD/moi-truong.sh" || { echo "!! $PROD không phải cây prod"; exit 1; }

echo "Đưa từ  $(pwd)"
echo "sang    $PROD"
echo
echo "Khác nhau ở phần mã nguồn:"
diff -rq work/server/NSO_KEM/src "$PROD/work/server/NSO_KEM/src" 2>/dev/null | head -20 || true
echo
printf "Gõ 'dua len prod' để tiếp tục: "
read -r tra
[ "$tra" = "dua len prod" ] || { echo "  đã huỷ."; exit 1; }

rsync -a --delete work/server/NSO_KEM/src/ "$PROD/work/server/NSO_KEM/src/"
rsync -a work/server/NSO_KEM/Data/ "$PROD/work/server/NSO_KEM/Data/"
for d in tools mod mod-src; do rsync -a "$d/" "$PROD/$d/"; done
# tools/ vừa bị đè -> trả lại moi-truong.sh của prod là thứ nằm ngoài tools nên không sao,
# nhưng ecosystem và mysql.properties thì phải chắc chắn không bị đụng: hai tệp ấy không nằm
# trong danh sách rsync ở trên.

# Ba số phiên bản: dev tăng thì prod phải tăng theo, không thì client cũ giữ bản trong bộ nhớ đệm.
for k in game.data.version game.item.version game.skill.version; do
  v=$(grep -m1 "^$k=" work/server/NSO_KEM/config.properties | cut -d= -f2)
  sed -i '' "s/^$k=.*/$k=$v/" "$PROD/work/server/NSO_KEM/config.properties"
  echo "  $k=$v"
done

echo
echo "Đã chép xong. Còn hai việc PHẢI tự làm:"
echo "  1. Thay đổi trong cơ sở dữ liệu (vật phẩm, kỹ năng, cửa hàng) chưa sang."
echo "     Chạy lại đúng script đã dùng bên dev, nhưng trỏ vào nso_test."
echo "  2. Nạp và khởi động lại bản chạy thật -- sẽ đá hết người đang chơi ra:"
echo "        cd $PROD && ./tools/nap.sh"
