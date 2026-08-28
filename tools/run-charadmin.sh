#!/bin/bash
# Bảng điều khiển nhân vật, chạy như một tiến trình riêng.
#
# Vì sao có bản chạy riêng trong khi máy chủ cũng tự mở bảng này lúc khởi động: bản riêng không
# đụng gì tới máy chủ đang chạy, nên sửa được nhân vật offline ngay mà không phải khởi động lại
# và không phiền người đang chơi. Đổi lại, nó không với tới bộ nhớ của máy chủ, nên gặp nhân vật
# đang online thì nó từ chối ghi thay vì âm thầm ghi vào CSDL rồi bị lần lưu sau xoá mất.
#
# Chỉ nghe ở 127.0.0.1 và địa chỉ Tailscale -- việc chọn địa chỉ nằm trong CharAdminHttp, không
# gắn vào 0.0.0.0 ở bất kỳ đường nào.
set -e
cd "$(dirname "$0")/.."

# Cổng 8766 chứ không phải 8765: 8765 là cổng bảng chạy trong máy chủ. Để hai bản dùng chung
# một cổng thì bản nào lên sau cũng chỉ ghi một dòng "bỏ qua" rồi im, rất dễ tưởng nó đang chạy
# mà thật ra đang xem qua bản kia. Khác cổng thì mở cả hai cũng không ai giẫm chân ai.
PORT=${CHARADMIN_PORT:-8766}
JAR=target/Nso-jar-with-dependencies.jar
CLS=$(pwd)/build/srvcls

# Chạy từ work/server/NSO_KEM vì Config đọc config.properties ở thư mục hiện tại.
cd work/server/NSO_KEM

# build/srvcls đứng TRƯỚC jar: các lớp vừa biên dịch phải che được bản cũ nằm trong jar, không
# thì sửa xong vẫn chạy mã cũ mà không có dấu hiệu gì.
exec java -Dfile.encoding=UTF-8 -cp "$CLS:$JAR" com.nsoz.admin.CharAdminHttp "$PORT"
