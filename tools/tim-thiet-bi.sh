#!/bin/bash
# Tìm xem một máy có thật sự vào được mạng nội bộ hay không, bằng cách so hai lần quét.
#
# Dùng khi máy đó không nói cho ta biết IP của nó (điện thoại đời cũ, máy in, thiết bị lạ) và ta
# cũng không vào được trang quản lý bộ định tuyến. Quét lúc máy tắt mạng, quét lại lúc máy bật
# mạng, cái nào mới mọc ra chính là nó.
#
# Quét bằng cách ping cả dải rồi đọc bảng ARP: máy nào trả lời thì hệ điều hành ghi lại địa chỉ
# vật lý của nó. Máy không trả lời ping vẫn có thể đang ở đó, nên "không thấy" là dấu hiệu chứ
# chưa phải bằng chứng.
#
#   ./tools/tim-thiet-bi.sh moc     ghi mốc (lúc máy đang TẮT mạng)
#   ./tools/tim-thiet-bi.sh so      quét lại và so với mốc (lúc máy đã BẬT mạng)
set -e
cd "$(dirname "$0")/.."
MOC=build/moc-mang.txt

quet() {
  local dai
  dai=$(ipconfig getifaddr "$(route -n get default 2>/dev/null | awk '/interface:/{print $2}')" | cut -d. -f1-3)
  for i in $(seq 1 254); do (ping -c1 -W 150 "$dai.$i" >/dev/null 2>&1 &) ; done
  sleep 7
  arp -an | grep -E "\($dai\." | grep -v incomplete | grep -v '255)' \
      | awk '{print $2, $4}' | tr -d '()' | sort -u
}

case "${1:-so}" in
  moc)
    mkdir -p build
    quet > "$MOC"
    echo "đã ghi mốc: $(wc -l < "$MOC" | tr -d ' ') thiết bị -- giờ hãy BẬT mạng trên máy cần tìm"
    ;;
  so)
    [ -f "$MOC" ] || { echo "chưa có mốc, chạy: ./tools/tim-thiet-bi.sh moc"; exit 1; }
    quet > /tmp/quet-moi.txt
    moi=$(comm -13 <(cut -d' ' -f1 "$MOC" | sort) <(cut -d' ' -f1 /tmp/quet-moi.txt | sort))
    if [ -z "$moi" ]; then
      echo "KHÔNG có thiết bị nào mới xuất hiện -- máy đó chưa vào được mạng này"
    else
      echo "thiết bị mới xuất hiện (đây là máy cần tìm):"
      for ip in $moi; do grep "^$ip " /tmp/quet-moi.txt | sed 's/^/  /'; done
    fi
    ;;
esac
