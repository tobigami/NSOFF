#!/bin/bash
# Dựng bản mod của client mobile để thử, và phát hành khi ưng.
#
# Bốn bước, luôn bắt đầu từ jar gốc chưa đụng vào trong work/mobileclient:
#   1. ép địa chỉ máy chủ về IP Tailscale hiện tại
#   2. ghi đè bằng những file trong mod/ -- cây thư mục mod/ soi gương cấu trúc bên trong jar
#   3. biên dịch mod-src/*.java
#   4. vá một lệnh chat để trỏ sang lớp mới đó
#   5. tắt dãy hình thoi vẽ theo mức nâng cấp (đồ +24 vẽ 13 cái, tràn khung thông tin)
#   6. tắt nhấp nháy tên món với đồ từ +15 trở lên
#   7. menu Tàn sát chỉ liệt kê quái của map hiện tại
#   8. bỏ tiền tố "[null]" ở thông báo do client tự sinh
#   9. số lớn hiện dạng K/M/B (ảnh font thêm ký tự nằm trong mod/)
#  10. nối mảng thời trang của máy chủ vào hình nhân vật, cho trang bị 2 đổi được diện mạo
#  11. dạy client vẽ áo choàng "Ngọc Lục Đạo" -- bảng mã áo choàng vốn ghi cứng trong client
#  12. đổi nhãn chí mạng / né đòn ở bảng Thông tin cho đúng nghĩa
#  13. hạ lệnh Thread.sleep(500) ghi cứng trong hàm đánh -- nút thắt nhịp đánh thật sự
#
# Phải dựng lại từ bản gốc chứ không phải từ bản đã phát hành: vá chồng lên bản đã vá thì bước 4
# tìm không ra lệnh cũ (nó đã bị đổi tên rồi) và cả mẻ build sẽ hỏng.
# dist/share chỉ bị thay khi bạn gọi promote.
#
#   ./mod.sh build [ip] dựng bản thử vào test/
#                       không ghi ip thì lấy IP Tailscale của chính máy này;
#                       ghi ip thì dựng bản trỏ về máy khác, tên tệp kèm luôn ip đó
#   ./mod.sh diff       liệt kê file nào khác bản gốc
#   ./mod.sh promote <số> [mô tả]
#                       phát hành ra dist/share cho bạn bè (chỉ khi đã thử kỹ)
#                       ví dụ: ./mod.sh promote 1.1 "thêm lệnh xyz"
set -e
cd "$(dirname "$0")"

# prod hay dev -- lấy cổng và tên bản từ đây, đừng ghi cứng. Bản dev nghe cổng khác, jar dựng ra
# phải trỏ đúng cổng ấy thì mới vào được.
. ./moi-truong.sh
GAME_PORT=$CONG_GAME
# Áo choàng tự thêm: <mã món> <ảnh1> <ảnh2> <ảnh3>. Client giữ bảng cứng mã món -> mã ảnh, mã nào
# không có trong bảng thì mặc vào không hiện gì; bộ vá chèn thêm một nhánh cho món của mình.
AO_CHOANG="${AO_CHOANG:-1263 3370 3371 3372}"
# Nhãn ở bảng Thông tin. Chí mạng đổi sang phần trăm (máy chủ đã chia 10 trước khi gửi); né đòn
# ghi rõ chỉ ăn khi đánh người, vì đánh quái thì máy chủ không đọc chỉ số này.
NHAN="Chí mạng: =>Chí mạng (%): ;Khả năng né đòn: =>Né đòn (đấu người): "

# Lệnh ngủ ghi cứng trong hàm đánh của client, tính bằng ms. Bản gốc là 500 -- đó là thứ khiến đòn
# ra chậm dù máy chủ khai báo thời gian chờ 100ms, và không có cách nào hạ từ phía máy chủ.
# Đặt lại bằng mức chờ ngắn nhất mà máy chủ có thể khai báo; để cao hơn là tự bóp lại nhịp đánh.
NGU_DANH="${NGU_DANH:-100}"
MOBILE_SRC=work/mobileclient/NSO_mobile_148.jar
# Tên jar mang theo tên môi trường: hai bản nằm cùng thư mục test/, trùng tên là mở nhầm.
MOBILE_OUT=test/NSO-mod.jar
[ "$TEN_MT" = "prod" ] || MOBILE_OUT="test/NSO-$TEN_MT.jar"
MOBILE_PUB=dist/share/NSO-mobile-ts.jar
MOBILE_HOOK="hds dan Dan.fill"
# thư mục ghi đè vào jar, và: lệnh chat bị chiếm chỗ, tên mới, hàm được gọi
MOBILE_OVER=mod

case "${1:-build}" in
  build)
    [ -d "$MOBILE_OVER" ] || { echo "thiếu thư mục $MOBILE_OVER"; exit 1; }
    IP=${2:-$(tailscale ip -4 2>/dev/null || true)}
    [ -n "$IP" ] || { echo "Tailscale chưa chạy, hoặc truyền ip: ./mod.sh build 100.x.y.z"; exit 1; }
    # Bản trỏ về máy khác để tên riêng, khỏi đè lên bản thử của máy này rồi lẫn lộn lúc mở.
    # Luôn ghi đè đúng một tệp. Trước đây đặt tên theo địa chỉ nên test/ đầy jar trỏ đi khắp
    # nơi, mở nhầm lúc nào không biết. Muốn giữ một bản thì tự chép ra chỗ khác.
    OUT=$MOBILE_OUT
    mkdir -p test build/mod build/tools
    ASM=$(ls lib/asm*.jar | tr '\n' ':')
    javac -nowarn -d build/tools tools/ForceServer.java tools/HookCommand.java \
        tools/StripUpgradeStars.java tools/StopUpgradeBlink.java \
        tools/TanSatMap.java tools/NoNullTag.java tools/ShortNumbers.java tools/ShortExpMsg.java tools/ShortMobHp.java
    javac -nowarn -cp "$ASM" -d build/tools tools/ThoiTrangHook.java tools/AoChoangHook.java tools/SuaNhan.java

    STEP=build/mod/step-mobile.jar
    java -cp build/tools ForceServer "$MOBILE_SRC" "$STEP" "$IP" "$GAME_PORT"
    ( cd "$MOBILE_OVER" && find . -type f ! -name '.DS_Store' -print0 | xargs -0 zip -q -u "../$STEP" )
    rm -rf build/modcls && mkdir -p build/modcls
    # Nhắm Java 7: máy bạn bè chạy JRE 8 đi kèm Micro_AngelChip, mà javac mặc định nhắm JDK đang
    # dùng (17, major 61) -- lớp ấy JRE 8 không nạp nổi. Xem tasks/lessons.md.
    javac -nowarn --release 7 -cp "$STEP" -d build/modcls mod-src/*.java
    java -cp build/tools HookCommand "$STEP" "$STEP.hook" $MOBILE_HOOK build/modcls
    java -cp build/tools StripUpgradeStars "$STEP.hook" "$STEP.stars"
    java -cp build/tools StopUpgradeBlink "$STEP.stars" "$STEP.blink"
    java -cp build/tools TanSatMap "$STEP.blink" "$STEP.ts"
    java -cp build/tools NoNullTag "$STEP.ts" "$STEP.null"
    java -cp build/tools ShortNumbers "$STEP.null" "$STEP.num"
    java -cp build/tools ShortExpMsg "$STEP.num" "$STEP.exp"
    java -cp build/tools ShortMobHp "$STEP.exp" "$STEP.hp"
    java -cp "build/tools:$ASM" ThoiTrangHook "$STEP.hp" "$STEP.tt"
    java -cp "build/tools:$ASM" AoChoangHook "$STEP.tt" "$STEP.ac" $AO_CHOANG
    java -cp "build/tools:$ASM" SuaNhan "$STEP.ac" "$STEP.nhan" "$NHAN"
    python3 tools/ha-ngu-danh.py "$STEP.nhan" "$OUT" "$NGU_DANH"
    rm -f "$STEP" "$STEP.hook" "$STEP.stars" "$STEP.blink" "$STEP.ts" "$STEP.null" "$STEP.num" "$STEP.exp" "$STEP.hp" "$STEP.tt" "$STEP.ac" "$STEP.nhan"

    echo "xong: $OUT  (máy chủ $IP:$GAME_PORT)"
    ;;
  diff)
    for f in $(cd mod && find . -type f ! -name '.DS_Store' | sed 's|^\./||'); do
      if unzip -p "$MOBILE_SRC" "$f" 2>/dev/null | diff -q - "mod/$f" >/dev/null 2>&1; then
        echo "giống bản gốc: $f"
      else
        echo "khác bản gốc:  $f"
      fi
    done
    ;;
  promote)
    [ -f "$MOBILE_OUT" ] || { echo "chưa dựng -- chạy ./mod.sh build"; exit 1; }
    LABEL=${2:-}
    # Chỉ nhận dạng số như 1.0 hay 2.13: tên có chữ thì mỗi lần phát hành lại đặt một kiểu,
    # nhìn danh sách không biết bản nào mới hơn bản nào.
    case "$LABEL" in
      *[!0-9.]*|"" ) echo "phiên bản phải là số, ví dụ: ./mod.sh promote 1.1 \"thêm lệnh xyz\""
                     exit 1 ;;
    esac
    NOTE=${3:-}
    VERSIONED="dist/share/NSO-$LABEL.jar"

    # Hai bản cùng nội dung, khác vai trò: bản có nhãn để tra lại về sau, còn NSO-mobile-ts.jar
    # là tên cố định mà bạn bè đã lưu link -- đổi tên nó là mọi liên kết đã gửi thành hỏng.
    [ -f "$MOBILE_PUB" ] && cp "$MOBILE_PUB" \
        "build/mod/da-phat-hanh-$(date +%Y%m%d-%H%M%S).jar"
    cp "$MOBILE_OUT" "$VERSIONED"
    cp "$MOBILE_OUT" "$MOBILE_PUB"

    LOG=dist/share/PHIEN-BAN.txt
    [ -f "$LOG" ] || echo "Các bản đã phát hành (mới nhất ở trên cùng)" > "$LOG"
    TMP=$(mktemp)
    { head -1 "$LOG"
      echo ""
      echo "$(date '+%d/%m/%Y %H:%M')  NSO-$LABEL.jar"
      [ -n "$NOTE" ] && echo "    $NOTE"
      tail -n +2 "$LOG"
    } > "$TMP"
    mv "$TMP" "$LOG"

    echo "đã phát hành:"
    echo "  $VERSIONED"
    echo "  $MOBILE_PUB   (tên cố định, luôn trỏ bản mới nhất)"
    ;;
  *) echo "dùng: ./mod.sh build|diff|promote"; exit 1 ;;
esac
