#!/bin/bash
# Dịch, nhét vào jar rồi khởi động lại máy chủ -- gộp cả vòng triển khai vào một lệnh.
#
#   ./tools/nap.sh                     dịch mọi file .java đã sửa gần đây rồi nạp
#   ./tools/nap.sh <đường/dẫn.java>... chỉ dịch mấy file nêu tên
#   ./tools/nap.sh --khong-restart     dịch và nạp jar nhưng không khởi động lại
#
# Vì sao vẫn phải khởi động lại: máy chủ nạp bảng vật phẩm, nj_part, nj_image, bản đồ, cửa hàng
# vào bộ nhớ **một lần lúc khởi động**, và JVM giữ luôn các lớp đã nạp lẫn tệp charadmin.html đọc
# từ trong jar. Sửa jar mà không khởi động lại thì tiến trình đang chạy vẫn dùng bản cũ.
#
# Sửa thẳng dữ liệu người chơi trong CSDL thì KHÁC: hàng người chơi chỉ được đọc lúc họ đăng nhập,
# nên không cần khởi động lại -- miễn là người đó đang offline.
set -e
cd "$(dirname "$0")/.."

# Cây nguồn này là prod hay dev? Đọc từ moi-truong.sh chứ đừng đoán -- xem chú thích trong đó.
. ./moi-truong.sh

JAR=work/server/NSO_KEM/target/Nso-jar-with-dependencies.jar
SRC=work/server/NSO_KEM/src/main/java
RESTART=1
FILES=()
for a in "$@"; do
  case "$a" in
    --khong-restart) RESTART=0 ;;
    *) FILES+=("$a") ;;
  esac
done

if [ ${#FILES[@]} -eq 0 ]; then
  # Không nêu tên thì lấy file .java mới hơn jar -- đúng những file đã sửa từ lần nạp trước.
  while IFS= read -r f; do FILES+=("$f"); done < <(find "$SRC" -name '*.java' -newer "$JAR")
fi

if [ ${#FILES[@]} -eq 0 ]; then
  echo "không có file .java nào mới hơn jar"
else
  echo "dịch ${#FILES[@]} file:"
  printf '  %s\n' "${FILES[@]#$SRC/}"
  javac -encoding UTF-8 -nowarn -cp "build/srvcls:$JAR" -d build/srvcls "${FILES[@]}"
fi

# charadmin.html nằm trong jar và JVM giữ bản đã nạp, nên chép lại rồi mới đóng gói.
cp "$SRC/com/nsoz/admin/charadmin.html" build/srvcls/com/nsoz/admin/charadmin.html 2>/dev/null || true
( cd build/srvcls && find . -newer "../../$JAR" \( -name '*.class' -o -name '*.html' \) -print0 \
    | xargs -0 -r zip -q -u "../../$JAR" )
echo "đã nạp vào jar"

[ "$RESTART" = 1 ] || { echo "bỏ qua khởi động lại"; exit 0; }

if [ "$TEN_MT" = "prod" ] && [ -z "$NSO_DONG_Y_PROD" ]; then
  echo
  echo "!! Đây là BẢN CHẠY THẬT ($PM2_TEN, $DB_TEN, cổng $CONG_GAME)."
  echo "   Khởi động lại sẽ đá hết người đang chơi ra."
  printf "   Gõ 'prod' để tiếp tục, Enter để huỷ: "
  read -r tra
  [ "$tra" = "prod" ] || { echo "   đã huỷ."; exit 1; }
fi

ai=$(mysql -uroot "$DB_TEN" -N -e "SELECT IFNULL(GROUP_CONCAT(name),'') FROM players WHERE online=1;")
[ -z "$ai" ] || echo "!! sắp đá ra khỏi game: $ai"

mysql -uroot "$DB_TEN" -e "UPDATE players SET online=0 WHERE online=1;"

# Đếm dòng báo xong TRƯỚC khi khởi động lại, rồi chờ số ấy tăng. Cắt trắng log không đáng tin:
# pm2 đang giữ tay cầm tệp nên nội dung cũ có thể còn nguyên, và khi ấy dòng báo xong của lần chạy
# TRƯỚC lại bị đọc nhầm thành của lần này -- script báo "xong sau 2s" trong khi máy chủ còn đang nạp.
# grep -c trả mã lỗi khác 0 khi không khớp dòng nào, nên "|| echo 0" lại nối thêm một số nữa ->
# biến thành "0\n0" và phép so sánh số phía dưới đổ. Dùng grep -c rồi hứng riêng.
# "|| true" là bắt buộc: grep -c IN ra 0 nhưng TRẢ VỀ mã 1 khi không khớp dòng nào, mà script
# chạy dưới `set -e` -- nên log vừa bị xoá (không còn dòng báo xong của lần trước) là nap.sh chết
# ngay tại đây, im lặng, đúng trước lệnh pm2 restart: báo "đã nạp vào jar" rồi thoát mã 1 mà không
# khởi động lại gì cả. Nhìn màn hình tưởng xong, thật ra máy chủ vẫn chạy mã cũ.
truoc=$(grep -c "Bảng điều khiển nhân vật" build/server.log 2>/dev/null || true); truoc=${truoc:-0}
pm2 restart "$PM2_TEN" > /dev/null
# Chờ theo DÒNG BÁO XONG trong log, không chờ theo cổng. Cổng 14444 vẫn còn mở một lúc sau khi
# gọi pm2 restart -- tiến trình cũ chưa chết hẳn -- nên dò cổng sẽ báo "xong" ngay lập tức trong
# khi máy chủ mới còn chưa nạp xong bản đồ. Log thì đã bị cắt trắng ở trên, dòng nào hiện ra cũng
# là của lần chạy mới.
printf "đang khởi động"
for i in $(seq 1 60); do
  sleep 1; printf "."
  nay=$(grep -c "Bảng điều khiển nhân vật" build/server.log 2>/dev/null || true); nay=${nay:-0}
  if [ "$nay" -gt "$truoc" ]; then
    echo " xong sau ${i}s"
    n=$(grep -icE "exception|error" build/server.log || true)
    echo "lỗi trong log: $n"
    nc -z 127.0.0.1 "$CONG_GAME" 2>/dev/null && echo "cổng $CONG_GAME mở" || echo "!! cổng $CONG_GAME CHƯA mở"
    exit 0
  fi
done
echo
echo "!! quá 60 giây chưa thấy máy chủ báo xong -- xem build/server.log"
exit 1
