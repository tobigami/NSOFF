# Mục lục công cụ

Thư mục để phẳng, không chia thư mục con — đường dẫn `tools/<tên>` xuất hiện khắp nơi trong ghi
chú và trong chính các script, chia nhỏ ra là hỏng hết. Chỗ tra cứu là tệp này.

Mỗi công cụ đều có phần chú thích đầu tệp nói rõ nó làm gì và **vì sao phải làm thế** — đọc phần
đó trước khi sửa, nhiều chỗ có cái bẫy không nhìn ra từ mã.

## Vận hành hằng ngày

| Công cụ | Việc |
|---|---|
| `nap.sh` | Dịch phần đã sửa, nhét vào jar, khởi động lại máy chủ. **Lệnh dùng nhiều nhất.** |
| `run-server.sh` | Chạy máy chủ ở tiền cảnh cho pm2 quản |
| `run-share.sh` | Mở `dist/share` cho bạn bè tải (cổng 8080 tailnet, 8081 nội bộ) |
| `run-charadmin.sh` | Bảng quản trị chạy như tiến trình riêng |
| `deploy-charadmin.sh` | Nhét bảng quản trị vào jar, không khởi động lại |
| `export-web.sh` | Dựng lại trang tra cứu vật phẩm |
| `run-nhan-log.sh`, `nhan-log.py` | Nhận log từ máy bạn bè qua Tailscale |
| `tim-thiet-bi.sh` | Dò xem một máy có vào được mạng nội bộ không |
| `share_server.py` | Máy chủ tệp tĩnh (run-share.sh gọi) |
| `Play.java` | Mở client trong MicroEmulator, đặt cửa sổ đúng màn hình |

## Vá client — `mod.sh` gọi theo thứ tự

`ThoiTrangHook` · `AoChoangHook` · `SuaNhan` · `ForceServer` · `HookCommand` · `NoNullTag` ·
`ShortExpMsg` · `ShortMobHp` · `ShortNumbers` · `StopUpgradeBlink` · `StripUpgradeStars` ·
`TanSatMap`

Quan trọng nhất: **`ThoiTrangHook`** nối mảng thời trang máy chủ vào hình nhân vật (client gốc đọc
đủ mười số rồi vứt), và **`SuaNhan`** đổi hằng chuỗi trong bytecode — cách rẻ nhất để đổi chữ hiển
thị mà không đụng bảng khung ngăn xếp.

## Dò bytecode client

`DoLenh` tìm chỗ client xử lý một lệnh máy chủ · `SetConstant` đổi một hằng chuỗi trong .class ·
`SetServer` ghi địa chỉ máy chủ vào jar · `Harness` khung chạy thử.

## Dữ liệu game

| Công cụ | Việc |
|---|---|
| `idanh.py` | **Cấp id ảnh mới an toàn.** Đừng bao giờ tự lấy `MAX(id)+1` — có ảnh nằm trên đĩa mà không có dòng trong bảng. |
| `kiem-mon-chet.py` | Dò nhân vật còn giữ vật phẩm đã bị xoá khỏi bảng `item` |
| `nap-chi-so-trang-phuc.py` | Nạp chỉ số cho món đã nằm sẵn trong tay người chơi (`initOption` chỉ chạy lúc tạo món) |
| `pha-tran-ky-nang.py` | Nối cấp cho một chiêu + tạo sách phá trần |
| `them-trang-phuc.py` | Thêm bộ trang phục ba mảnh từ ảnh hiệu ứng |
| `nhap-tu-hoiuc.py`, `gom-art-hoiuc.py` | Nhập / gom art từ bản hồi ức (`thamkhao/ZnsoC 2/`) |
| `go-dung-anh.py` | Gỡ chỗ đè id ảnh — công cụ chữa cháy, mong là không phải dùng lại |

## Ảnh và sprite

`CanManh` căn mảnh sprite · `FitSprite` đưa ảnh về đúng khung · `SplitSheet` tách dải ảnh ·
`LamSacAnh` làm sắc lại art · `TinhChinh` đổi màu theo bảng, xoá điểm lẻ · `KeyBg` bóc nền ·
`FaceBase` bóc khuôn mặt chung của mặt nạ · `VeToc` vẽ lại tóc · `CatBoKage`, `CatNonKage` cắt bộ
Kage · `GhepCau`, `GhepQuay` dựng Cầu lục đạo · `MakeIcons` vẽ icon cho món thiếu ·
`MakeNumberFont` thêm `.KMB` vào font số · `Make*Effect` vẽ hiệu ứng · `XepAnhSoSanh` xếp ảnh cạnh
nhau để so · `doc-pxo.py` lấy ảnh ra khỏi tệp Pixelorama.

## Bảo trì mã nguồn

`go-lombok-char.py` — gỡ Lombok khỏi `Char.java` bằng cách viết tay đúng những accessor Lombok vốn
sinh ra. Máy không có `lombok.jar` và đang offline, nên không có nó thì `Char.java` **không biên
dịch lại được** và mọi sửa đổi phải đi đường vá bytecode. Script lấy chuẩn từ chính `Char.class`
trong jar (`javap`), không đoán theo quy tắc đặt tên — quy tắc ấy có bẫy.

## Bản vá đã áp một lần, giữ để tham khảo

`BuNhinHook` · `FixAkatsukiEffectId` · `FixTayKyNang` · `DisableMaintenance` · `MoveHudText` ·
`OpenChatCommands` · `OpenMenuGate` — không nằm trong `mod.sh`, đã dùng xong. Giữ vì chúng cho
thấy cách vá một kiểu lỗi cụ thể.
