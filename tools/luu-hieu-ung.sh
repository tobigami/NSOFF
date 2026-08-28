#!/bin/bash
# Sao lưu một hiệu ứng trước khi vẽ đè: cả bốn tệp ảnh lẫn dòng dữ liệu trong bảng.
#
# Vì sao cần: mỗi lần chỉnh hình là ghi đè thẳng lên Data/Img/Effect, mà kích thước ô sprite đổi
# theo hình -- nên roll back mà chỉ chép lại ảnh thì máy khách cắt sai, phải có kèm dòng
# effect_data của đúng lúc đó. Hai thứ luôn đi cặp.
#
#   ./tools/luu-hieu-ung.sh 255 "truoc-khi-lam-kiem-lua"
#   ./tools/luu-hieu-ung.sh --xem 255          liệt kê các bản đã lưu
#   ./tools/luu-hieu-ung.sh --phuchoi 255 <thư mục>   đưa về bản đó
set -e
cd "$(dirname "$0")/.."
KHO=luutru/hieu-ung
MYCNF=/private/tmp/claude-501/-Users-avada-Code-NSOFF/50cbaee3-b55b-4bce-a33c-5b8adf133524/scratchpad/my.cnf
[ -f "$MYCNF" ] || MYCNF=""

db() {
  if [ -n "$MYCNF" ]; then mysql --defaults-extra-file="$MYCNF" "$@"; else mysql -uroot "$@"; fi
}

case "${1:-}" in
  --xem)
    ls -1 "$KHO/${2:?thiếu mã hiệu ứng}" 2>/dev/null || echo "chưa có bản lưu nào cho hiệu ứng $2"
    exit 0 ;;
  --phuchoi)
    MA=${2:?thiếu mã}; BAN=${3:?thiếu tên bản}
    D="$KHO/$MA/$BAN"
    [ -d "$D" ] || { echo "không có $D"; exit 1; }
    for z in 1 2 3 4; do cp "$D/$z.png" "work/server/NSO_KEM/Data/Img/Effect/$z/$MA.png"; done
    for DB in nso_test nso_dev; do db -D $DB < "$D/effect_data.sql"; done
    echo "đã phục hồi hiệu ứng $MA về bản $BAN (nhớ khởi động lại máy chủ)"
    exit 0 ;;
esac

MA=${1:?dùng: ./tools/luu-hieu-ung.sh <mã hiệu ứng> [ghi chú]}
GHI=${2:-}
TEN="$(date +%Y%m%d-%H%M%S)${GHI:+-$GHI}"
D="$KHO/$MA/$TEN"
mkdir -p "$D"
for z in 1 2 3 4; do
  f="work/server/NSO_KEM/Data/Img/Effect/$z/$MA.png"
  [ -f "$f" ] && cp "$f" "$D/$z.png"
done
# Dòng dữ liệu: xuất thành câu REPLACE để phục hồi là chạy được ngay.
db -N -B --raw -D nso_test -e "
SELECT CONCAT('REPLACE INTO \`effect_data\` (\`id\`,\`sprites\`,\`frames\`,\`running\`,\`frame_char\`) VALUES (',
  id, ',', QUOTE(sprites), ',', QUOTE(frames), ',', QUOTE(running), ',', QUOTE(frame_char), ');')
FROM effect_data WHERE id = $MA;" > "$D/effect_data.sql"
echo "đã lưu hiệu ứng $MA -> $D"
ls -1 "$D" | sed 's/^/    /'
