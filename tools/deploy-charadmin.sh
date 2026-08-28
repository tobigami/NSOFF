#!/bin/bash
# Biên dịch bảng điều khiển nhân vật rồi nhét vào jar máy chủ. KHÔNG khởi động lại máy chủ.
#
# Không tự khởi động lại vì có người đang chơi: script này chỉ chuẩn bị sẵn, còn `pm2 restart nso`
# để người dùng bấm lúc thấy tiện. Trước khi khởi động lại thì máy chủ đang chạy vẫn dùng mã cũ --
# `zip` ghi ra tệp tạm rồi đổi tên đè lên, nên tiến trình đang mở jar giữ nguyên bản cũ và không
# bị ảnh hưởng giữa chừng.
set -e
cd "$(dirname "$0")/.."

JAR=work/server/NSO_KEM/target/Nso-jar-with-dependencies.jar
SRC=work/server/NSO_KEM/src/main/java
CLS=build/srvcls

FILES="
$SRC/com/nsoz/admin/CharAdmin.java
$SRC/com/nsoz/admin/CharAdminHttp.java
$SRC/com/nsoz/admin/AdminService.java
$SRC/com/nsoz/server/Server.java
"

# build/srvcls đứng TRƯỚC jar, nếu không javac lấy nhầm bản cũ nằm trong jar và bản vừa sửa
# không bao giờ được dùng.
echo "== biên dịch"
javac -nowarn -encoding UTF-8 -cp "$CLS:$JAR" -d "$CLS" $FILES

# Trang HTML nằm cạnh lớp trong jar, CharAdminHttp đọc nó bằng getResourceAsStream.
cp "$SRC/com/nsoz/admin/charadmin.html" "$CLS/com/nsoz/admin/charadmin.html"

echo "== nhét vào jar"
( cd "$CLS" && zip -q -u "../../$JAR" \
    com/nsoz/admin/CharAdmin*.class \
    com/nsoz/admin/charadmin.html \
    com/nsoz/admin/AdminService*.class \
    com/nsoz/server/Server*.class )

echo "== xong. Máy chủ đang chạy VẪN dùng mã cũ."
echo "   Khi nào tiện thì: pm2 restart nso"
echo "   Sau khi khởi động lại, bảng điều khiển tự mở ở cổng 8765 và sửa được cả người đang online."
