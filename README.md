# NSOFF — máy chủ NinjaSchool tự dựng (BẢN CHẠY THẬT)

> Đây là bản người khác đang chơi. Khởi động lại là đá hết mọi người ra.
> Chỗ để thử nghiệm là **`/Users/avada/Code/NSOFF-dev`** — DB `nso_dev`, cổng 14446, pm2 `nso-dev`.
> `tools/nap.sh` ở đây sẽ hỏi lại trước khi khởi động lại.

## Bố cục

    work/           Mã nguồn máy chủ (work/server/NSO_KEM). Sửa ở đây rồi chạy tools/nap.sh.
    tools/          Công cụ: nạp máy chủ, vá client, dựng ảnh, nạp dữ liệu.
    mod-src/        Mã Java tự thêm vào client, mod.sh biên dịch rồi nhét vào jar.
    mod/            Tài nguyên client (font, ảnh) mod.sh ghi đè vào jar.
    lib/            Thư viện dùng lúc dựng: ASM (đọc bytecode J2ME), MicroEmulator.
    dist/           Bản phát hành. dist/share là thư mục phát cho người chơi tải (cổng 8080).
    test/           Jar client đã vá, mỗi tệp trỏ tới một địa chỉ máy chủ khác nhau.
    art/            Ảnh nguồn: art/pixelorama (tệp .pxo), art/lam-viec (ảnh dựng dở).
    thamkhao/       Bản dựng và tài liệu để đối chiếu, KHÔNG phải mã đang chạy.
                    Vài công cụ đọc kho ảnh ở thamkhao/ZnsoC 2/ -- đừng đổi tên thư mục này.
    tasks/          Ghi chú công việc: todo.md, lessons.md.
    build/          Thứ sinh ra tự động. Xoá được, chạy lại là có. KHÔNG cất gì quý ở đây.
    luutru/         Rác chờ xoá -- xem luutru/DOC-TRUOC.txt.

## Lệnh hay dùng

    ./tools/nap.sh              dịch phần đã sửa, nhét vào jar, khởi động lại máy chủ
    ./mod.sh build              dựng lại client đã vá -> test/NSO-mod.jar
    ./play.sh mod               mở client trong MicroEmulator
    ./tools/export-web.sh       dựng lại trang tra cứu vật phẩm
    pm2 logs nso                xem log máy chủ
    pm2 flush nso               dọn log (đừng xoá build/server.log bằng tay)

## Ba thứ dễ quên nhất

**Sửa gì cũng phải khởi động lại máy chủ.** Bảng vật phẩm, nj_part, nj_image, bản đồ, cửa hàng đều
nạp vào bộ nhớ một lần lúc khởi động; JVM còn giữ luôn các lớp đã nạp và cả charadmin.html đọc từ
trong jar. `tools/nap.sh` lo việc này.

**Đổi dữ liệu thì phải bump version trong `work/server/NSO_KEM/config.properties`,** không thì
client dùng bản cũ trong bộ nhớ đệm:

    game.data.version    nj_part, nj_image, ảnh
    game.item.version    bảng vật phẩm
    game.skill.version   bảng kỹ năng

**Id phải liên tục.** `item`, `nj_part`, `nj_image` được truyền theo vị trí, có lỗ là client đọc
lệch toàn bộ. Dùng `tools/idanh.py` để xin id mới, đừng lấy MAX(id)+1 -- có những ảnh nằm trên đĩa
mà không có dòng trong bảng.

## Cấu hình

    work/server/NSO_KEM/mysql.properties     tên DB, tài khoản MySQL
    work/server/NSO_KEM/config.properties    cổng, phiên bản dữ liệu, tham số game

Cổng đang dùng: 14444 game · 8765 bảng quản trị · 8080 phát tệp · 10000 RMI · 8020 WebSocket.
