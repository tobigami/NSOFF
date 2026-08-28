# Việc còn tồn đọng

Cập nhật 20/08/2026. Đánh dấu `[x]` khi xong. Mỗi mục ghi sẵn chỗ cần sửa và những gì đã biết,
để bắt tay vào làm mà không phải khảo sát lại từ đầu.

---

## Vật phẩm thiếu chỉ số

Nguồn: `tools/checks/StatCheck.java` — dựng thử cả 526 món bằng đúng đường `GiveItem.build` rồi
đếm chỉ số. Chạy lại lúc nào cũng được để đối chiếu sau khi vá.

- [ ] **18 vĩ thú Sơ Cấp, id 924–941** — lỗ hổng rõ nhất, cùng dạng đã gặp ở Thiên Vương.
      `Item.initOption` có khối chỉ số cho trung cấp (994–1011), cao cấp (1012–1029), siêu cấp
      (1030–1047) nhưng **không có khối nào cho sơ cấp**. Cách lấp không bịa số: nội suy xuống từ
      khối trung cấp. `Item.java` không dính Lombok nên biên dịch lại được bình thường.

- [ ] **11 mặt nạ cấp 20–80 trắng chỉ số** — 921 Khẩu trang (c20), 306/307 Hannya (c30),
      343/345 fasshon (c30), 745 Mặt nạ chuột (c40), 541/542 Thuỷ tinh và Sơn tinh (c50),
      594 Thánh Gióng (c50), **615 Deidara (c80)**, **616 Tobi (c80)**. Hai món cấp 80 trắng
      trơn là bất thường rõ nhất: Mặt nạ sát thủ cấp 20 (id 273) có đủ chỉ số để soi theo.

- [ ] **7 thú nuôi cấp 70** — 583 Hoả long, 584–586 Hải mã 1–3, 587–589 Dị Long 1–3.

- [ ] **2 món có dòng cửa hàng nhưng cột `options` rỗng** — 246 Dơi đen, 345 Mặt nạ fasshon.
      Sửa thẳng trong CSDL, không cần đụng mã.

---

## Phân thân

- [ ] **Chia exp từ chủ thân sang phân thân.** Đã khảo sát xong, chưa làm.
      Cách đúng: ghi đè `addExp(long)` trong `CloneChar.java` (**không** dính Lombok, khỏi vá
      bytecode). Hai cái bẫy bắt buộc phải tránh:
      1. **Vòng lặp vô hạn** — dòng `if (isNhanBan) human.addExp(exp)` biến mọi lời gọi `addExp`
         lên phân thân thành cú bật ngược về chủ thân. Tuyệt đối không gọi `clone.addExp()` từ
         phía chủ thân; phải cộng thẳng vào `clone.exp` rồi tự tính lại cấp.
      2. **Nhân hệ số hai lần** — `addExp(long)` mở đầu bằng `exp *= Config.getRateEXP()`, rate
         đang là 20. Đi qua hai lần là nhân 400.
      Điểm móc gọn nhất tìm được: `Service.addExp(long)` ở `Service.java:1200`, nhận đúng con số
      cuối cùng đã nhân rate và đã qua cửa `isNhanBan`. `Service.java` biên dịch lại được.
      Còn phải chốt: **chia đôi** (chủ thân mất phần đưa sang) hay **nhân bản** (chủ thân giữ
      nguyên, tổng exp vào server tăng), và tỷ lệ bao nhiêu phần trăm.

- [ ] **`CloneChar.findWorld` không trỏ về chủ thân khi đang điều khiển phân thân.**
      Sau `switchToMe` thì `isNhanBan` thành false nên `findWorld` dùng danh sách world riêng của
      phân thân, mà danh sách đó không có hang động. Hệ quả: mở hang bằng chủ thân rồi đổi sang
      phân thân cày thì điểm PB, đếm giờ và kiểm boss (`Char.java:7140` và `7565`) không nhìn
      thấy hang, nhiều khả năng mất điểm và mất thưởng cuối hang. Sửa một dòng trong
      `CloneChar.java:60-65`.

- [ ] **Phân thân không tự mở được hang động.** Hai rào chắn độc lập: cờ `countPB` chỉ cấp cho
      thân người thật (`Char.java:17569`, và `DailyLimit.java:92` bỏ qua `!c.isHuman`); và cổng
      cấp độ tính theo cấp của phân thân. Chỉ làm nếu thấy cần — đây là thiết kế gốc chứ chưa
      chắc là lỗi.

- [ ] **Chưa giải thích được: phân thân bắn ra hiệu ứng trông giống chiêu tiêu.** Đã loại trừ
      giả thuyết thanh kỹ năng không nạp (`CloneChar.getService()` trả service của chủ thân khi
      đang điều khiển, nên các gói tin nạp thanh kỹ năng vẫn được gửi). Cơ chế bên dưới thì đúng
      là của quạt: tầm 56×29 của chiêu Ouchia. Còn lại thuần là chuyện hiển thị.

---

## NPC Tashino (đã đặt ở Làng Echigo, map 43)

- [ ] **Thu tiền không khớp lời thoại** — kiểm `user.gold >= 1000` và nhắc "Hãy đưa ta 1.000
      lượng" nhưng chỉ trừ `addGold(-500)`. Chốt sửa bên nào: thu đủ 1.000, hay hạ lời thoại
      xuống 500. Nằm trong `Char.java` nên phải vá bytecode.

- [ ] **Vòng bốc chỉ số bỏ sót phần tử cuối** — dùng `nextInt(options.size() - 1)` nên dòng nằm
      cuối rổ ở mỗi lượt bốc không bao giờ được chọn. Bỏ `- 1` là xong, nhưng cũng nằm trong
      `Char.java`.

---

## Client

- [ ] **Con số thứ hai trong câu nhận kinh nghiệm** — ảnh chụp thấy "…39757344" sau dấu phẩy,
      nhiều khả năng là điểm gia tộc hoặc yên, thuộc một câu khác. Vá y hệt cách đã làm với câu
      kinh nghiệm: neo theo nhãn của câu đó rồi đổi lời gọi `append` sang `SoNgan.them`.
      Xem `tools/ShortExpMsg.java`.

- [x] **Mặt nạ Itachi — bỏ mục này (23/08/2026).** Vật phẩm đã xoá theo yêu cầu ngày 22/08
      ("xấu vãi thôi xoá item này cho toi"), thư mục `art/mat-na-itachi/` (có `bo4-nguon.png`)
      xoá cùng lúc, và ba ô ảnh 3050–3052 nay là **nón Kage** của mảnh 309. Bản vẽ tay đã cứu
      lại từ hai tệp Pixelorama, để ở `art/mat-itachi-ve-tay/` (xem `tools/pxo-ra-png.py`).
      Muốn làm lại thì phải xin lại ảnh nguồn bộ 4 và cấp ô ảnh MỚI.

---

## Dọn dẹp

- [x] **Gỡ log `XIN ẢNH` trong `Service.requestIcon`** — xong 20/08/2026.

---

## Bù nhìn tập luyện (đã xong 20/08/2026, còn một việc dọn)

Đặt ở 3 trường và 6 làng, không chết, báo sát thương cao nhất khi xác lập đỉnh mới, nghỉ 10 giây
thì đo lại từ đầu. Dùng luôn mẫu bù nhìn sẵn có, nhận diện bằng vị trí đặt. Xem `BuNhin.java` và
`tools/BuNhinHook.java`.

- [ ] **Bỏ cách nhận diện theo toạ độ chính xác.** Hiện phải nhắm đúng con ở toạ độ đã ghi, mà
      bốn con bù nhìn ở Trường Hirosaki nhìn giống hệt nhau nên rất khó dùng. Hướng thay: mọi bù
      nhìn trong **sáu làng** đều là bù nhìn tập luyện (làng không có nhiệm vụ tân thủ), còn ba
      trường thì giữ nguyên một con để nhiệm vụ "giết 10 bù nhìn" vẫn chạy.

## Đang có người làm

- [ ] **Màn quản lý nhân vật cho admin** — giao cho một agent riêng. Xem trang bị đang mặc, chỉ
      số từng món, hành trang, và sửa được điểm kỹ năng / tiềm năng / tẩy điểm **khi nhân vật
      đang offline**. Cái bẫy đã dặn trước: nhân vật đang online thì máy chủ giữ bản trong bộ nhớ
      và sẽ ghi đè CSDL lúc lưu, nên phải phân nhánh online/offline.
