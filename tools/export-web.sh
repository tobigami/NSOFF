#!/bin/bash
# Dựng lại trang tra cứu vật phẩm trong thư mục chia sẻ.
#
# Chạy lại khi bảng item hoặc store_data đổi. Dữ liệu được xuất bằng chính bộ máy chủ
# (ItemManager/StoreManager/Converter) chứ không đọc SQL trực tiếp -- chỉ số nằm ở store_data nên
# đọc thẳng bảng item sẽ ra thiếu.
set -e
cd "$(dirname "$0")/.."

RA=dist/share/vat-pham
JAR=work/server/NSO_KEM/target/Nso-jar-with-dependencies.jar
mkdir -p "$RA/icon"

javac -nowarn -encoding UTF-8 -cp "$JAR:build/srvcls" -d build/srvcls tools/checks/WebExport.java

# WebExport đọc Data/Img và config.properties theo đường dẫn tương đối, nên phải chạy trong
# thư mục máy chủ.
(cd work/server/NSO_KEM && java -cp "target/Nso-jar-with-dependencies.jar:$OLDPWD/build/srvcls" \
    WebExport "$OLDPWD/$RA/items.json" | tail -1)

python3 - "$RA" <<'PY'
import json, os, shutil, sys
ra = sys.argv[1]
data = json.load(open(ra + "/items.json", encoding="utf-8"))
src = "work/server/NSO_KEM/Data/Img/Small"
chep = thieu = 0
for ic in sorted({v["icon"] for v in data["items"] if v["icon"] is not None and v["icon"] >= 0}):
    # Cỡ 2 là 30x30: nét trên màn hình mà cả bộ vẫn vừa phải.
    for z in (2, 3, 4, 1):
        p = f"{src}/{z}/Small{ic}.png"
        if os.path.isfile(p):
            shutil.copy(p, f"{ra}/icon/{ic}.png"); chep += 1; break
    else:
        thieu += 1
print(f"icon: chép {chep}, thiếu {thieu}")
PY
echo "xong: http://100.98.117.102:8080/vat-pham/"
