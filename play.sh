#!/bin/bash
# Mở client bằng MicroEmulator để tự thử ngay trên máy này.
#
#   ./play.sh                bản đã phát hành (dist/share/NSO-mobile-ts.jar)
#   ./play.sh lan            bản trỏ về máy chủ trong mạng nội bộ
#   ./play.sh e72            bản Ninja Mobile của máy E72, đã trỏ về mạng nội bộ
#   ./play.sh mod            bản đang sửa (test/NSO-mod.jar)
#   ./play.sh emu            emulator trống, tự chọn file trong File > Open MIDlet File
#   ./play.sh hoiuc          máy khách bản hồi ức (ZnsoC v4), nối vào máy chủ hồi ức cổng 14455
#
#   ./play.sh mobile 2       chỉ định ô lưu số 2
#   ./play.sh mobile 2 fg    giữ ở tiền cảnh để xem log chạy trực tiếp
#
# Gọi lại lệnh là mở thêm một cửa sổ nữa, cửa sổ đang chạy không bị đụng tới. Không ghi số thì
# script tự lấy ô lưu còn trống (build/emu-<loại><số>) -- phải tách ô vì hai cửa sổ dùng chung
# một thư mục lưu sẽ cùng ghi một bản ghi RMS, cái nào lưu sau thì đè cái kia.
set -e
cd "$(dirname "$0")"

# Jar để mở: lấy đúng tệp .jar nằm trong test/ của CHÍNH cây nguồn này.
#
# Không ghi cứng tên, cũng không trỏ sang cây kia. Mỗi cây chỉ giữ đúng một jar (xem
# tools/kiem-jar.sh), và jar ấy đã trỏ sẵn vào máy chủ của cây đó -- dev thì 14446, prod thì
# 14444. Nên `play.sh mod` gõ ở đâu là mở bản của chỗ đó, không phải nhớ gì thêm.
. ./moi-truong.sh
JAR_MOD=$(ls test/*.jar 2>/dev/null | head -1)

# Nói rõ sắp nối vào đâu TRƯỚC khi mở.
#
# Hai cây nguồn giống hệt nhau, cùng một lệnh, chỉ khác thư mục đang đứng -- không có gì trên màn
# hình cho biết mình vừa mở bản nào. Đã có lần ngồi thử tính năng cả buổi trên máy chủ thật mà
# tưởng đang ở dev. Một dòng chữ là đủ để chuyện đó không lặp lại.
if [ -n "$JAR_MOD" ]; then
  DIA_CHI=$(unzip -p "$JAR_MOD" 2>/dev/null | strings \
      | grep -oE "[0-9]{1,3}(\.[0-9]{1,3}){3}:[0-9]{4,5}" | sort -u | head -1)
  echo "Sắp mở: $JAR_MOD"
  if [ "$TEN_MT" = "prod" ]; then
    echo "  ***  BẢN CHẠY THẬT (prod)  ***  -> ${DIA_CHI:-không rõ địa chỉ}"
    echo "  Muốn vào bản thử nghiệm thì: cd ${CAY_DEV:-../NSOFF-dev} && ./play.sh mod"
  else
    echo "  bản thử nghiệm ($TEN_MT) -> ${DIA_CHI:-không rõ địa chỉ}"
  fi
fi


LIBS=$(ls lib/*.jar | tr '\n' ':')

KIND=mobile
SLOT=""
FG=""
for a in "$@"; do
  case "$a" in
    mobile|lan|e72|mod|emu|hoiuc) KIND=$a ;;
    fg)                     FG=fg ;;
    [0-9]*)                 SLOT=$a ;;
    *) echo "không hiểu tham số: $a"; exit 1 ;;
  esac
done

# Tìm ô lưu chưa có cửa sổ nào dùng. pgrep cần "--" vì mẫu bắt đầu bằng dấu gạch; thiếu nó
# pgrep tưởng đó là tuỳ chọn và không khớp ai cả.
free_slot() {
  local base=$1 n=""
  while pgrep -f -- "-Duser.home=$PWD/$base$n " > /dev/null 2>&1; do
    n=$(( ${n:-1} + 1 ))
  done
  echo "$n"
}

# Chạy nền và cắt khỏi terminal, trừ khi gọi kèm "fg". nohup chặn SIGHUP lúc đóng cửa sổ terminal,
# disown gỡ khỏi danh sách việc của shell -- thiếu một trong hai là đóng terminal mất luôn game.
run() {
  if [ "$FG" = "fg" ]; then
    exec "$@"
  fi
  # Log riêng theo ô: mở nhiều cửa sổ mà chung một file thì cái sau xoá mất log cái trước.
  local log="build/play-$KIND$SLOT.log"
  nohup "$@" > "$log" 2>&1 &
  disown
  echo "đã mở (pid $!) -- ô lưu: $HOME_DIR, log: $log"
}

case "$KIND" in
  mobile)
    [ -n "$SLOT" ] || SLOT=$(free_slot build/emu-mobile)
    HOME_DIR="$PWD/build/emu-mobile$SLOT"
    run java -Duser.home="$HOME_DIR" -cp "$LIBS" \
        org.microemu.app.Main "$PWD/dist/share/NSO-mobile-ts.jar"
    ;;
  hoiuc)
    # Máy khách bản hồi ức (ZnsoC v4). Nó trỏ sẵn 100.98.117.102:14455 -- cổng của máy chủ hồi ức
    # chạy từ build/hoiuc, KHÔNG phải máy chủ của mình. Hai bên khác cơ sở dữ liệu lẫn lược đồ.
    [ -n "$SLOT" ] || SLOT=$(free_slot build/emu-hoiuc)
    HOME_DIR="$PWD/build/emu-hoiuc$SLOT"
    run java -Duser.home="$HOME_DIR" -cp "$LIBS" \
        org.microemu.app.Main "$PWD/luutru/jar-cu/LocalPhuong-hoiuc.jar"
    ;;
  lan)
    [ -n "$SLOT" ] || SLOT=$(free_slot build/emu-lan)
    HOME_DIR="$PWD/build/emu-lan$SLOT"
    run java -Duser.home="$HOME_DIR" -cp "$LIBS" \
        org.microemu.app.Main "$PWD/dist/share/NSO-mobile-lan.jar"
    ;;
  e72)
    [ -n "$SLOT" ] || SLOT=$(free_slot build/emu-e72)
    HOME_DIR="$PWD/build/emu-e72$SLOT"
    run java -Duser.home="$HOME_DIR" -cp "$LIBS" \
        org.microemu.app.Main "$PWD/dist/share/NinjaMobile-e72-lan.jar"
    ;;
  mod)
    [ -n "$SLOT" ] || SLOT=$(free_slot build/emu-mod)
    HOME_DIR="$PWD/build/emu-mod$SLOT"
    run java -Duser.home="$HOME_DIR" -cp "$LIBS" \
        org.microemu.app.Main "$PWD/$JAR_MOD"
    ;;
  emu)
    HOME_DIR="$PWD/build/emu-home$SLOT"
    # Gọi Main không tham số thì nó thoát ngay không nói gì: Common.initParams() trả về false khi
    # danh sách tham số rỗng. Chỉ định thẳng loại thiết bị là đủ để nó dựng cửa sổ lên.
    run java -Duser.home="$HOME_DIR" -cp "$LIBS" org.microemu.app.Main \
        --device org.microemu.device.j2se.J2SEDevice
    ;;
esac
