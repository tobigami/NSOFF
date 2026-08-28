#!/bin/bash
# Kiểm luật: jar trong dist/ phải trỏ vào bản chạy thật, jar trong test/ phải trỏ vào đúng cây của nó.
#
# Mở nhầm jar là loại lỗi im lặng nhất: không báo gì, chỉ là bạn tưởng đang thử nghiệm trong khi
# đang chơi trên máy chủ người khác đang dùng. Chạy cái này sau mỗi lần dựng lại client.
set -e
cd "$(dirname "$0")/.."
. ./moi-truong.sh

diaChi() { unzip -p "$1" 2>/dev/null | strings | grep -oE "[0-9]{1,3}(\.[0-9]{1,3}){3}:[0-9]{4,5}" | sort -u | head -1; }

loi=0
echo "Cây $TEN_MT — cổng phải là $CONG_GAME"
for j in test/*.jar; do
  [ -f "$j" ] || continue
  a=$(diaChi "$j"); cong="${a##*:}"
  if [ "$cong" = "$CONG_GAME" ]; then echo "  ok   $j  ->  $a"
  else echo "  SAI  $j  ->  ${a:-không rõ}  (phải là cổng $CONG_GAME)"; loi=1; fi
done

if [ -d dist/share ] && [ "$TEN_MT" = "prod" ]; then
  echo
  echo "dist/share — phải trỏ vào bản chạy thật, cổng $CONG_GAME"
  for j in dist/share/*.jar; do
    [ -f "$j" ] || continue
    a=$(diaChi "$j"); cong="${a##*:}"
    if [ "$cong" = "$CONG_GAME" ]; then echo "  ok   $(basename "$j")  ->  $a"
    else echo "  SAI  $(basename "$j")  ->  ${a:-không rõ}"; loi=1; fi
  done
fi

echo
[ "$loi" = 0 ] && echo "Tất cả đều đúng." || echo "!! Có jar trỏ sai chỗ — dựng lại bằng ./mod.sh build"
exit $loi
