# NSOFF — Ninja School Online → Offline

Mục tiêu: biến `NinjaSchool_251.jar` (game online J2ME) thành bản chơi offline hoàn toàn,
không còn bất kỳ kết nối server nào.

## Kiến trúc

Không viết lại game. Giữ nguyên toàn bộ class gốc, chỉ **thay lớp vận chuyển** và **nhúng một
server nội bộ** vào chính file JAR.

```
main.a (game canvas)  →  dh (Session)  →  bs (connector)
                                            │
                            ┌───────────────┴───────────────┐
                            │  TRƯỚC: Connector.open(       │
                            │         "socket://host:port") │
                            │  SAU:   nsoff.Chan (pipe RAM) │
                            └───────────────┬───────────────┘
                                            ↓
                                       nsoff.Srv  ←→  nsoff.World
                                     (framing +        (logic game)
                                      handshake)
```

### Vì sao patch được

Class nào ở **default package** đều recompile lại được bằng javac hiện đại (`bs`, `dh`, `w`,
`dq`, `dc`, …). Class trong package `main` thì **không** — Java ≥1.4 cấm package có tên tham
chiếu class ở unnamed package. Nên mọi thay đổi đều đi qua default package.

### Chi tiết giao thức (đã dịch ngược)

Khung tin:
```
client → server : [cmd:1][len:2][payload]
server → client : [cmd:1][len:2][payload]            khi len ≤ 0xFFFF
                  [-32][cmd:1][len:4][payload]       khi lớn hơn
```

Handshake: client gửi `-27` rỗng, server trả `-27` + `[n][key…]`. Client tự XOR luỹ tiến
`key[i+1] ^= key[i]` rồi bật `dh.l = true`. **Bắt buộc bật** vì thread gửi (`aq.run`) chỉ flush
khi `l == true`. Ta trả key 1 byte giá trị `0` → XOR thành phép đồng nhất → toàn bộ luồng là
plaintext, không cần cài mã hoá.

Nhóm lệnh: `-29` NOT_LOGIN, `-28` NOT_MAP, `-30` SUB_COMMAND (byte sub theo sau), còn lại là
opcode trực tiếp. Tổng cộng 158 case top-level trong `an.a(ce)`.

Luồng đăng nhập:
1. C→S `-29/-125` client info, `-29/-127` login(user, pass, ver, "", "", random, h)
2. S→C `-28/-123` 4 byte version (data, map, skill, item)
3. C→S xin blob thiếu: `-28/-122` data · `-28/-121` map · `-28/-120` skill · `-28/-119` item
4. S→C trả blob cùng sub-opcode
5. C→S `-28/-101` sẵn sàng
6. S→C `-28/-126` danh sách nhân vật (3 slot) hoặc vào tạo nhân vật
7. S→C `-30/-127` thông tin nhân vật → `-18` vào map → payload map (`an.d`)

Format bảng sprite (đều nằm trong blob data `-28/-122`, mỗi bảng bọc bởi `[len:4][bytes]`):
- `nj_image` : `s16 n; n×{u8 atlas, s16 x, s16 y, s16 w, s16 h}`
- `nj_part`  : `s16 n; n×{u8 type; k×{s16 imgId, s8 dx, s8 dy}}` với k = 8/18/10/2 theo type 0/1/2/3
- `nj_effect`: `s16 n; n×{s16 id; u8 f; f×{s16 imgId, s8 dx, s8 dy}}`
- `nj_arrow` : `s16 n; n×{s16 _; s16 a0; s16 a1; s16 a2}`
- `nj_skill` : `s16 n; n×{s16 id; s16 a; u8 _; u8 b; b×ar; u8 c; c×ar}` (ar = 1 byte + 12 short)

### Nội dung game

Ban đầu phải tự dựng lại nội dung từ atlas trong JAR (không có bảng index). **Sau khi user cung
cấp `SETUP_LOCAL/` chứa source server private (`SRC_GAME/NSO_KEM.zip`), toàn bộ nội dung đã được
thay bằng dữ liệu gốc.**

Nguồn: `work/server/NSO_KEM/`
- `nso_test.sql` — database đầy đủ: `nj_image` (3049), `nj_part` (308), `nj_effect`, `nj_arrow`,
  `nj_skill`, `item` (1241), `monster` (261), `npc` (48), `map` (179), `skill_template` (91),
  `clazz` (7), `effect`, `others` (bảng exp), `task`
- `Data/Img/Mob/1/` — sprite quái thật (JAR không có)
- `Data/Img/Small/1/` — sprite rời cho các id nằm ngoài atlas
- `Data/Map/` — tile data cho cả 179 map (JAR chỉ có 60)
- `src/` — source Java server, dùng để **port đúng từng serializer** thay vì đoán

Đã xác nhận **5 atlas `Big0..Big4.png` trong JAR trùng byte-for-byte** với client của server này,
nên bảng `nj_image`/`nj_part` áp thẳng vào được.

Build time parse thẳng file `.sql` (không cần MySQL, không chạy server) rồi đóng blob vào JAR.

## Checklist

- [x] Giải nén + decompile JAR (CFR + Vineflower; Vineflower giải được `an.a` mà CFR fail)
- [x] Dựng harness chạy + chụp màn hình tự động (MicroEmulator + `Component.printAll`)
- [x] Build script: compile → guard API CLDC → repack JAR
- [x] Thay `bs`: socket → pipe nội bộ (`nsoff.Chan`), handshake key rỗng
- [x] Bỏ tải danh sách server qua HTTP (patch `w.a("NJlink")`) — menu hiện "Máy Chủ: Offline"
- [x] Xác nhận client gửi được login qua pipe (đã thấy `-29/-125`, `-29/-127`)
- [x] Xoá sạch URL còn sót trong bytecode (`StripUrls`) + guard trong build chặn tái xuất hiện
- [x] ~~Tự tách sprite từ atlas~~ → **thay bằng dữ liệu gốc từ DB server**
- [x] Parser dump MySQL + JSON (`SqlDump`, `Json`) — không cần MySQL
- [x] Port đúng serializer của server cho cả 4 blob + `nj_*` (đối chiếu source, không đoán)
- [x] Sprite quái thật (260 template, 648 KB) + sprite rời (1831 ảnh) + 119 tile map còn thiếu
- [x] Blob template: map/npc/mob (`-28/-121`), skill (`-28/-120`), item (`-28/-119`)
- [x] Đăng nhập → tạo nhân vật → `-30/-127` char info
- [x] Vào map: `-18` + payload (mob, npc, vị trí spawn dò từ tilemap)
- [x] Sprite quái: JAR không có → ghép từ part nhân vật, phát qua `-28/-108`
- [x] Gameplay: di chuyển, đánh (cmd 60), mob AI + aggro, exp/level, drop, nhặt đồ, túi đồ
- [x] Mặc trang bị (cmd 11) — id loại item đã đánh đúng chuẩn client (1 vũ khí, 2 quần, 6 áo)
- [x] Nhân vật mới có sẵn kiếm gỗ + thuốc; hồi máu dần; gục thì hồi sinh sau 6s
- [x] Lưu tiến trình vào RMS (`nsoff.Save`): chỉ số, nhiệm vụ, skill, túi đồ — ghi ngay mỗi hành động
- [x] Hạ class version về 47 + strip StackMapTable (`Downgrade`)
- [x] Bộ verify 2 tầng: `ProtoTest` (protocol) + `Harness` (emulator thật)
- [x] Đi lại giữa map bằng waypoint (`-17`) — cả 179 map tới được
- [x] Đối chiếu lại serializer với source server: sửa `t` = **tốc độ di chuyển** (đang gửi nhầm level)
- [x] Tương tác NPC: chạm NPC (40) → menu (63) → chọn (29) → hộp thoại (-26)
- [x] Trả shortcut thanh skill (`-30/-65`) — client treo thanh skill nếu không trả
- [x] Nhiệm vụ: giao (47), tiến bước (48), hoàn thành (49); 43 nhiệm vụ gốc từ `task_template`
- [x] Sửa 2 crash: skill id giả (nhân vật mới chỉ có 1 chiêu), và `bg` index âm
- [x] Bảng nhiệm vụ trên màn hình — chỉ vẽ khi cờ **isHuman** bật (`dg.java:5342` gate bằng
      `bp.d().A()`); tôi gửi `false` nên nó không hiện suốt
- [x] Luồng nhận nhiệm vụ đúng bản gốc: phải tới NPC nhận trước, xong mới hiện; hoàn thành
      xong KHÔNG tự giao nhiệm vụ kế tiếp
- [x] Bước giết quái / nhặt đồ (`kill_mob`, `pick_item`, `counts`) + cập nhật tiến độ (lệnh 50)
- [x] Phần thưởng khi hoàn thành (exp + xu, lệnh 13)
- [x] Shop NPC (23 cửa hàng, 560 dòng hàng, giá thật) + dùng đồ ăn/thuốc
- [x] Dùng được bình HP (type 16) và bình MP (type 17), lượng hồi theo đúng từng item id của
      server; kèm hiệu ứng `-30/-101` để client vẽ icon buff
- [x] Gộp chồng vật phẩm theo cột `isUpToUp` (đá không stack là luật của game, thuốc/vật liệu có)
- [x] Sửa mảng trắng đè lên nhân vật mỗi khi bị đánh: trường hiệu ứng trong `-3` phải là **-1**
      như `Service.npcAttackMe`, tôi tự điền `1` nên client phát hiệu ứng số 1 lên người chơi
- [x] Ảnh rời: kiểm biên cả hình chữ nhật (`x+w <= 255`) thay vì từng số riêng lẻ
- [x] Lên cấp: server suy cấp từ bảng exp, tăng maxHP/maxMP, hồi đầy và báo client qua `-30/-109`.
      Server gốc tính từ điểm tiềm năng người chơi tự cộng (`AbilityFromEquip:198`); bản này chia
      sẵn theo tỉ lệ cố định vì chưa có màn cộng điểm
- [ ] Màn cộng điểm tiềm năng / điểm kỹ năng (server: `potentialPoint = level * 10`)
- [x] Sửa mặc đồ làm tụt tốc độ: lệnh 11 đi qua `bp.a(ce)` = readParam, byte 2 là **speed** chứ
      không phải level (`an.java:789` → `bp.java:446`)
- [x] Sửa mất tiến trình khi vào lại: túi đồ/skill chưa được lưu, và nhánh "chọn nhân vật cũ"
      cấp lại đồ khởi đầu đè lên; thêm test reconnect so túi đồ trước/sau
- [x] Bỏ 2 nhiệm vụ nói chuyện mở đầu — nhân vật mới bắt đầu thẳng ở task 2 "NV Lần đầu dùng kiếm"
- [x] Sửa `-2` trong bảng `task` bị đọc thành 254 (ghi/đọc bằng byte không dấu). Đây là gốc của
      cái fallback "NPC không tồn tại thì ai cũng được", vốn khiến nói chuyện với NPC bất kỳ là
      xong bước "Sử dụng vũ khí". Nay ghi bằng short có dấu trong `world.bin`
- [x] Nới ô waypoint gửi cho client thêm 1 tile: `bp.G()` so khít nên ô 24px sát rìa map rất khó
      chạm; server chọn waypoint gần nhất thay vì cái đầu tiên khớp
- [x] Chặn nhảy map vô hạn do việc nới ô gây ra: điểm đáp được đẩy ra khỏi mọi ô lối ra của map
      đích (gốc rễ), và nếu client vẫn xin đi thì server gửi lại map đang đứng (có throttle 1s) để
      màn "Đang tải…" không bao giờ treo
- [x] Sửa AI quái theo đúng server: bù nhìn/mộc nhân/thảo dược không bao giờ tấn công; aggro cần
      cùng cao độ + `rangeMove + 20` (quái bay `type==4` mới đuổi cả hai trục)
- [x] Sửa **quái không hiện hình**: byte trạng thái trong bản tin map phải là **5 = sống** /
      0 = chết (`Mob.java:167`,`:331`); tôi gửi `1` = *vừa chết* nên `ci.d()` không vẽ
- [x] Sửa **mặc đồ làm mất đồ**: `equip()` xoá món khỏi túi mà không lưu đi đâu. Nay theo dõi
      `Player.equipped` (chỉ số = type của item, đúng cách client đánh chỉ số), món đang mặc rơi
      ngược về ô túi, char info phát lại đồ đang mặc, save v6 lưu kèm
- [x] Chỉ số tính lại từ base + đồ đang mặc thay vì cộng dồn mỗi lần equip
- [x] Sửa exp bù nhìn 25.000 → hợp lý: kẹp HP về một chỗ duy nhất (lúc build) để exp dẫn xuất đúng
- [x] Port đúng bảng rơi đồ của server (`RandomItem.ITEM` + `Mob.randomItemID`): có trọng số và
      quy đổi theo cấp quái, thay cho danh sách 4 item tự chọn theo tên khiến bù nhìn rơi đồ lv70
- [x] Yên (item type 19) nhặt được cộng vào ô tiền thay vì nằm trong túi; char info + lệnh 13 phát
      đủ cả Xu/Yên/Lượng. Bộ đồ khởi đầu cũng không cấp Yên dạng vật phẩm nữa
- [x] Sửa dòng chữ chạy bị bóp còn 6px: `ae` chừa chỗ cho nút cảm ứng theo `main.a.g`, không theo
      kích thước màn thật; đồng thời nâng nó lên trên panel Lv/HP/MP thay vì đè lên
- [ ] Bán đồ, nâng cấp đồ
- [x] Bỏ UI online lúc khởi động — recompile `co` (màn menu), vào thẳng game bằng guest login
- [ ] PvP. Chat/guild: user không cần.

## Cách dùng

```bash
./build.sh     # → out/NinjaSchool_Offline.jar
./test.sh      # protocol test + boot test + kiểm tra không còn URL mạng
```

## Review

### Kết quả

`out/NinjaSchool_Offline.jar` (2.2 MB) — chạy hoàn toàn offline, **không mở socket, không gọi
HTTP**, dùng **dữ liệu gốc** của game.

`./test.sh` pass toàn bộ (exit 0):
- 33 check protocol: login → 4 blob → tạo nhân vật → vào "Trường Hirosaki" (4 quái, 17 NPC) →
  **đi waypoint sang map 16 quái** → đánh chết → exp → rơi đồ → nhặt → mặc đủ 3 món trang bị →
  đồ tiêu hao bị từ chối mặc → quái hồi sinh
- Boot thật trong MicroEmulator: menu → tạo nhân vật → vào map, ảnh trong `build/shots/`
- Quét JAR: không còn URL mạng nào

Nội dung thật: 179 map, 261 quái (sprite gốc), 48 NPC, 1241 item, 91 skill template, 3049 sprite
index, 308 part nhân vật.

### Việc đã làm, theo lớp

| Lớp | Thay đổi |
|-----|----------|
| Vận chuyển | `bs` mở `nsoff.Chan` (pipe RAM) thay `Connector.open("socket://…")` |
| Handshake | Trả key 1 byte `0` → XOR thành phép đồng nhất, luồng plaintext, `dh.l=true` để thread gửi chịu flush |
| Danh sách server | `w.a("NJlink")` trả list nội bộ 1 entry → `GameMidlet.d()` không bao giờ gọi `c()` (HTTP) |
| URL còn sót | `StripUrls` blank 20 hằng chuỗi trong `main/GameMidlet`, `dc`, `an`, `at`, `main/a` |
| Nội dung | `SqlDump`+`Json` parse `nso_test.sql`; `Content` port đúng serializer server cho 4 blob + `nj_*` |
| Sprite quái | 260 template lấy thẳng `Data/Img/Mob/1`, phát qua `-28/-108` |
| Sprite rời | 1831 ảnh ngoài atlas, phát qua `-28/-115` |
| Map | 179 map từ DB; bổ sung 119 tile map JAR thiếu; đi lại giữa map qua waypoint (`-17`) |
| Server | `nsoff.Srv` (framing) + `nsoff.World` (logic) + tick 600ms cho respawn/aggro/hồi máu |
| Item | Loại item đánh số theo đúng chuẩn client (1/2/6 = vũ khí/quần/áo, 18 thuốc, 19 xu), part id của trang bị khớp số phần tử từng slot nên mặc đồ không crash |
| Lưu | `nsoff.Save` → RecordStore: chỉ số, nhiệm vụ, skill **và túi đồ**; ghi ngay mỗi hành động, không throttle |
| Tương thích | Class hạ về major 47, strip StackMapTable; guard chặn API ngoài CLDC |

### Giới hạn đã biết

1. **Chưa có**: bán đồ (mới chỉ mua được), nâng cấp trang bị, PvP. Chat và guild nằm ngoài phạm
   vi theo yêu cầu. Server gốc còn khoá shop tới khi xong nhiệm vụ "Diệt sên trừ cóc" — tôi bỏ
   khoá đó cho dễ chơi.
2. **7 dòng `nj_skill` bị bỏ** vì trỏ tới skill template mà bản dump này không có (id 91..99 trong
   khi chỉ có 91 template). Client dùng số template để cấp phát mảng nên giữ lại sẽ crash.
3. **Sát thương quái** suy ra từ level (bảng `monster` không có cột damage).
4. **Bug của client, đã né chứ chưa vá**: `dg.d()` kẹp cận trên nhưng không kẹp cận dưới khi
   tính ô thanh skill (`ez[this.eJ]`), chỉ kích hoạt khi nhân vật biết **≥2 skill**. Nhân vật
   mới chỉ có 1 chiêu nên chưa chạm tới. Khi làm hệ thống học skill phải vá bytecode `dg`.
5. **`dg.class` không recompile được** (nó tham chiếu class tên `do`, là từ khoá Java). Muốn đổi
   gì trong đó — như vị trí dòng chữ nhiệm vụ — phải vá bytecode; xem `tools/MoveQuestText.java`.
6. **Message của client hay parse qua helper.** Trước khi định dạng payload phải mở helper ra
   xem, đừng đoán theo tên message — xem lesson về `readParam` trong `tasks/lessons.md`.
5. **Mảng NPC của nhiệm vụ lệch 1**: phần tử 0 là NPC *giao* nhiệm vụ, nên bước `i` nhắm tới
   `npcs[i+1]`. Chính client cũng cộng `+1` (`dg.F()`, dg.java:13774). Lúc đầu tôi quên bước dịch
   này nên nói chuyện NPC nào cũng qua bước.
6. **Map khởi đầu là 22 "Làng Tone", toạ độ (1741, 264)** — lấy thẳng từ `players.map`
   DEFAULT `'[22,1741,264]'`. Điểm spawn nằm ngay cạnh Tajima (NPC giao nhiệm vụ), và làng này
   có đủ cả 7 NPC mà nhiệm vụ mở đầu gọi tên.
7. **Lệnh 40 mang TEMPLATE ID chứ không phải chỉ số NPC trên map** (`dg.java:3293` gửi
   `bp.d().aU.cm.a`). Ban đầu tôi tra nhầm vào mảng NPC của map → hiện sai NPC, và với
   Okanechan (template 24 > số NPC trên map) thì không hiện gì → treo.
8. **Luồng NPC theo đúng server** (`Char.initMenu`, model/Char.java:15732): NPC của bước nhiệm
   vụ hiện tại được chèn thêm mục "Làm nhiệm vụ" / "Hoàn thành nhiệm vụ" lên đầu menu; chỉ mục
   đó mới nhích bước. Kèm lời thoại NPC qua lệnh 38 (`OPEN_UI_SAY`) để nhiệm vụ mở đầu đúng ý
   nghĩa "đi hỏi xem NPC nào làm gì".
9. **NPC 13 "Khu vực"** không đi qua lệnh 40 — client tự xử lý bằng lệnh 36 (đổi khu).
10. **Cờ `isHuman` (trường `b` trong char info) điều khiển nhiều thứ hơn tên gọi**: bảng nhiệm
    vụ trên màn hình chỉ vẽ khi nó bật, và client đọc bảng skill từ `skill` thay vì
    `skillnhanban`. Gửi `false` làm mất bảng nhiệm vụ mà không báo lỗi gì.
11. **HP quái cấp thấp bị thổi phồng trong dump**: bù nhìn cấp 1 có 500.000 HP, trong khi nhiệm
    vụ tutorial đòi giết 10 con. Đã kẹp lại HP khi nó lệch quá xa so với level.
4. **Máy CLDC thật (KVM)** cần preverify; toolchain preverify không có trên macOS ARM. Chạy tốt
   trên emulator (MicroEmulator đã verify; KEmulator/J2ME Loader theo class version 47 thì hợp lệ).

## Hướng khác: chạy server thật (SETUP_LOCAL)

`work/server/NSO_KEM/` là server Java thật, Maven, target 1.8, **fat jar dựng sẵn**
`target/Nso-jar-with-dependencies.jar`. Chạy trên macOS khả thi:

- DB: MySQL/MariaDB, `nso_test`, root/không mật khẩu, port 3306. Dump `nso_test.sql` 2.8 MB.
- **MongoDB không cần** — `DbManager` chỉ dùng Hikari + JDBC MySQL; `mongodb.bat=false`.
- Client localhost: `SETUP_LOCAL/PB/Jar_local.zip`, chạy bằng MicroEmulator có sẵn.
- Bỏ qua toàn bộ mục mở port Firewall trong hướng dẫn (chỉ áp dụng cho Windows).

Rủi ro đã lường: `joor`/Netty/log4j cũ có thể vỡ trên JDK 17 (module system) → dự phòng cài
Temurin 8/11; `main()` mở một cửa sổ Swing.

Đã dọn `SETUP_LOCAL` từ 2.5 GB xuống 438 MB (bỏ các bộ cài Windows), giữ lại emulator và bản PC
theo yêu cầu.

## Máy chủ đang chạy — những thay đổi đã áp dụng

Ghi lại ở đây vì `dist/share/PHIEN-BAN.txt` chỉ ghi các bản client; phần máy chủ không có chỗ nào
tra lại được. Vòng triển khai: `javac -cp "build/srvcls:<fatjar>" -d build/srvcls <file>` →
`zip -u` vào `work/server/NSO_KEM/target/Nso-jar-with-dependencies.jar` → `pm2 restart nso`.
**Classpath phải để `build/srvcls` trước jar**, không thì javac lấy nhầm lớp cũ trong jar.

Sáu lớp dùng Lombok nên không biên dịch lại được, chỉ vá bytecode với chuỗi lệnh dài bằng hoặc
ngắn hơn: `Char`, `Session`, `Item`, `World`, `Group`, `Event`.

### 20/08/2026 — vật phẩm dùng một lần không còn ăn cả chồng

`tools/FixTayKyNang.java` vá `Char.class` hai chỗ:

- Nhánh vật phẩm 241 (giấy phép tẩy kỹ năng) tăng đúng `tayKyNang` nhưng câu thông báo lại in
  `tayTiemNang` — chép nhầm biến từ nhánh 240 ngay phía trên. Người chơi thấy số không nhúc nhích
  nên tưởng vật phẩm hỏng.
- `removeItem(item.index, item.getQuantity(), true)` xoá cả chồng trong khi chỉ định dùng một
  cái. Lỗi vô hình khi trong tay chỉ có một món nên sống rất lâu. Quét theo mẫu 13 byte khớp cả
  hai chỉ số hằng `getQuantity()I` và `removeItem(IIZ)V`, vá **12 chỗ**: giấy phép tẩy tiềm năng
  và tẩy kỹ năng, ba sách mở giới hạn (252/253/308), sách bàng hoa (309), nấm linh chi, linh chi
  ngàn năm, linh chi vạn năm, bạch biến lệnh, hai lệnh về làng.

### 20/08/2026 — NPC Tashino vào Làng Echigo (map 43)

`Char.npcTashino()` cài đặt đầy đủ "Luyện bí kíp" và "Nâng cấp bí kíp", bảng `npc` có id 39 với
menu đủ ba mục, nhưng **không map nào đặt NPC này** — quét cả 1183 map chỉ thấy 35 NPC được đặt.
Tính năng nằm trong mã mà không ai chạm tới được, nên sáu quyển bí kíp cấp 60 (397–402) mãi mãi
trắng chỉ số. Chúng trống là ĐÚNG thiết kế: Tashino mới là nơi bốc 1–5 chỉ số cho chúng.

Chọn Làng Echigo bằng tiêu chí đo được — đếm số bước qua đồ thị cửa map từ mỗi làng tới khu cày
cấp 60: Echigo 2 bước tới Hang núi Kurai và 3 tới Động Tamatamo, các làng khác đều trên 8.
Bản lùi: `build/dbbackup/map43-npc-truoc.json`.

Hai lỗi nhỏ trong hàm đó chưa sửa: kiểm `gold >= 1000` nhưng chỉ trừ `addGold(-500)`; và vòng bốc
chỉ số dùng `nextInt(options.size() - 1)` nên phần tử cuối rổ không bao giờ được chọn.

### 20/08/2026 — trần cấp độ 130 → 145

Trần đến từ **dữ liệu**, không phải cấu hình: `game.server.maxLV=-1` nghĩa là dùng hết bảng, nên
trần chính là độ dài bảng `others.exp`. Nối thêm 15 mốc theo đúng nhịp cũ (cụm 4–5 cấp, mỗi cụm
cộng 50 tỷ): 131–135 mỗi cấp 550 tỷ, 136–140 là 600 tỷ, 141–144 là 650 tỷ, 145 là 700 tỷ. Tổng
để đạt 145 là 19.761.676.205.701.

Phải nâng `game.data.version` 18 → 19 vì bảng exp nằm trong khối dữ liệu có đánh số phiên bản;
không nâng thì client vẫn dùng bảng cũ và thanh exp hiện sai từ cấp 130 trở lên.

Cần biết: `NinjaUtils.getLevel` trả về **cấp 1** nếu exp vượt hết bảng. Thứ chặn không rơi vào
nhánh đó là `Server.EXP_MAX`, vốn cộng dồn từ chính bảng rồi trừ 1. Nối thêm mốc thì EXP_MAX tự
tính lại lúc khởi động nên vẫn an toàn.

Bản lùi: `build/dbbackup/exp-truoc.json`, `build/dbbackup/config-truoc.properties`.

### 20/08/2026 — bảng điều khiển nhân vật (xem/sửa cả khi offline)

Trang web ở `com/nsoz/admin/CharAdminHttp.java` + `charadmin.html`, logic ở
`com/nsoz/admin/CharAdmin.java`. Xem trang bị từng ô kèm chỉ số từng dòng, thời trang, hành trang,
rương, kỹ năng, tiềm năng, cấp độ, exp. Sửa: cộng/rút điểm tiềm năng theo từng ô, đặt thẳng bốn ô,
cộng điểm tiềm năng và điểm kỹ năng, nâng/hạ từng kỹ năng, tẩy tiềm năng, tẩy kỹ năng, nâng hết kỹ
năng lên trần, cấp thêm lượt tẩy ở NPC.

**Cái bẫy và cách rẽ nhánh.** `Char.saveData()` ghi đè CẢ hàng `players` lúc người chơi thoát game,
nên sửa thẳng CSDL trong lúc họ online là mất trắng. Nên mọi thao tác ghi đi qua một cửa duy nhất:
online thì sửa đối tượng `Char` trong bộ nhớ rồi `saveData()`, offline thì UPDATE thẳng và chỉ đụng
đúng cột cần đổi. Phần ĐỌC thì không chặn — người đang online vẫn xem được ảnh từ lần lưu gần nhất.

**Chọn trang web, không chọn menu trong game.** `BagAdmin`/`GearAdmin`/`TeleportAdmin` có sẵn đều
duyệt `ServerManager.getChars()` nên chỉ thấy người đang online — đúng thứ cần tránh. Menu trong
game cũng không hiện nổi 16 món × 14 dòng chỉ số + 40 ô hành trang.

**Hai cách chạy, hai cổng khác nhau.** Trong máy chủ (tự mở lúc khởi động, cổng 8765) thì sửa được
cả người online. Chạy độc lập `tools/run-charadmin.sh` (cổng 8766) thì không cần khởi động lại máy
chủ, nhưng gặp người đang online là từ chối ghi và nói rõ lý do. Hai cổng khác nhau để mở cả hai
cũng không giẫm chân nhau. Lệnh chat `bang` bật/tắt bản trong máy chủ.

Chỉ nghe ở 127.0.0.1 và địa chỉ Tailscale (dò dải 100.64.0.0/10 bằng `NetworkInterface`, không gọi
lệnh `tailscale`), không bao giờ 0.0.0.0.

Nhận biết online: cột `players.online` (đặt 1 lúc đăng nhập ở `User.java`, 0 lúc đăng xuất, dọn về
0 lúc khởi động ở `Server.java` và trong `tools/run-server.sh`). Bản chạy trong máy chủ đối chiếu
thêm `ServerManager.findCharById` — đó mới là nguồn thật.

Kiểm: `tools/checks/CharAdminCheck.java`. In toàn bộ trang bị + chỉ số của một nhân vật thật, rồi
chạy từng thao tác ghi và **đọc lại bằng SQL trần** để đối chiếu (không gọi lại hàm đọc của chính
mình), cuối cùng trả nguyên trạng. 10/10 đạt trên cả `o0ollto0o` (#2, cấp 82, phái ngoại) lẫn
`quat1` (#3, cấp 51, phái nội).

Triển khai bằng `tools/deploy-charadmin.sh` (biên dịch → `zip -u` vào jar).

Nhánh sửa người đang online đã kiểm được sau lần khởi động lại 16:20, trên `itachi` đang chơi thật.
Phép chứng minh dứt điểm là ghi thẳng `spoint=999` vào CSDL rồi đọc qua bảng: bảng vẫn trả về 41 —
tức là nó thật sự đọc đối tượng trong bộ nhớ chứ không đọc bảng. Ghi tiếp một thao tác qua bảng
làm `saveData()` chạy và **xoá sạch `spoint=999` về 41** — chính là hiện tượng ghi đè mà cả thiết
kế này sinh ra để tránh. Sau đó itachi được trả về đúng nguyên trạng 860/41/[5,5,170,2200].

### 20/08/2026 — băng-rôn danh hiệu Đệ Nhất Ninja thu gọn thành một dòng chữ

Bản gốc là tấm 124x41 ở mức thu phóng 1, năm khung rồng lửa nhấp nháy — rộng gấp ba lần nhân vật.
Thay bằng dòng chữ "Nhẫn giả thánh nhân" màu đỏ, Tahoma Bold cao 7, 104x16, tám khung chỉ để một
vệt sáng quét ngang rồi nghỉ. Không đụng một dòng mã nào: `Char.java:933` vẫn phát hiệu ứng 201
như cũ, chỉ ảnh và hàng `effect_data` đổi. Chỉ số của món đồ nằm ở `Item.java:632`, không liên
quan, nên người mặc vẫn ăn đủ 5000 công / 5000 thủ / 100 chí mạng.

Dựng bằng `tools/MakeTitleEffect.java`; chạy lại là ra đúng bộ đang chạy. Font lấy Tahoma vì
chính client dùng Tahoma cho mọi chữ trong game (`font/tahoma_7`, `tahoma_7b`, `tahoma_8b` nằm
trong jar), nên băng-rôn nhìn như một phần giao diện chứ không như ảnh dán thêm.

**Cái trần không ai ghi ở đâu.** `EffectData.setData()` ghi `x`, `y`, `w`, `h` của mỗi khung bằng
**một byte**, client đọc lại không dấu. Nghĩa là ở mức thu phóng 1 mỗi khung rộng tối đa 255, và
cả tấm ảnh xếp chồng cũng chỉ cao tối đa 255 — đó mới là lý do bản gốc rộng đúng 124 chứ không
rộng hơn. Vượt trần thì không ai báo lỗi: máy chủ vẫn chạy, client vẫn nhận, chỉ có hình vẽ ra là
rác. `tools/checks/EffectCheck.java` kiểm đúng chỗ đó, cộng thêm việc tấm mức 2, 3, 4 phải to
đúng gấp 2, 3, 4 lần tấm mức 1 (toạ độ trong `effect_data` chỉ có một bộ, tính theo mức 1).

**Phải khởi động lại máy chủ.** `GameData.loadFile` giữ byte ảnh trong bộ nhớ vĩnh viễn — vòng dọn
cache theo hạn đã bị comment hết — và `EffectDataManager.load()` chỉ chạy lúc khởi động. Đổi file
với đổi CSDL xong mà không restart thì vẫn ra ảnh cũ. Client thì không lưu ảnh hiệu ứng vào RMS
(kho `vj<n>` chỉ có data/map/skill/item/nj_*), nên người chơi chỉ cần vào lại game.

Bản lùi: `build/dbbackup/effect201-truoc.tsv`, `build/dbbackup/effect201-anh-truoc/`.

Làm cho cả họ danh hiệu thì chạy lại cùng công cụ: Đệ Nhị 202, Đệ Tam 203, Đệ Tứ 204, V_VIP
301–303, Trùm Phái 12–17, Fan Cứng 231 — tất cả đều là băng-rôn lửa năm khung, cùng khối
`// hiệu ứng danh hiệu` ở `Char.java:883`.

### 20/08/2026 — danh hiệu Đệ Tam đổi thành hai con mắt Luân Hồi

Hiệu ứng 203 (danh hiệu "Đệ Tam", vật phẩm 1152) từ băng-rôn lửa 104x39 năm khung thành hai con
mắt tím mở ra giữa một vầng aura trên nền tối, 84x27, chín khung. Dựng bằng `tools/MakeRinneganEffect.java` — vẽ vector nên mỗi
mức thu phóng render riêng chứ không phóng to ảnh mức 1, và số vòng gợn thưa dần theo mức: mức 1
hai vòng, mức 4 năm vòng. Nhồi năm vòng vào con mắt cao 16 pixel của mức 1 thì chúng dính thành
một mảng tím đặc.

Hai chỗ dễ sai khi vẽ hình quả hạnh bằng cung bậc hai: đỉnh cung nằm ở `(P0 + 2*P1 + P2) / 4`, nên
điểm điều khiển phải đặt ở **hai lần** chiều cao mong muốn — đặt thẳng chiều cao thì mắt chỉ hé
được một nửa. Và chuỗi phát phải nối được hai đầu với nhau vì nó tự lặp: mở hẳn 18 nhịp, khép dần
về khung 0, rồi mở lại — thành ra một cái chớp mắt thay vì một cú giật mỗi vòng.

Cùng cách làm với hiệu ứng 201, không đụng mã: `Char.java:883` vẫn phát 203 như cũ, chỉ ảnh và
hàng `effect_data` đổi. Vẫn phải `pm2 restart nso` vì `GameData.loadFile` giữ byte ảnh vĩnh viễn.

Sửa vòng hai theo góp ý: thêm vầng aura tím, thu nhỏ mắt (cao 16 → 11), và kéo chậm nhịp chớp
(36 → 98 nhịp). Ba thứ này kéo nhau: aura cần chỗ toả nên khung nới từ 75x22 lên 84x27, mà chiều
cao tấm bị trần 255 chặn nên số khung phải hạ 10 → 9 (9 x 27 = 243, mười khung là 270, vượt).

Aura vẽ bằng `RadialGradientPaint` có ma trận bóp trục dọc để vùng sáng thành bầu dục nằm ngang
ôm đôi mắt — hình tròn thì thừa trên thừa dưới, vừa tốn chiều cao khung vừa đội lên che tên nhân
vật. Độ đậm buộc vào độ mở của mắt nên aura bừng lên lúc mắt mở mà không tốn thêm khung nào.
Chốt màu bằng cách render thử trên ba nền — cỏ, hang tối, tuyết: bản đầu dùng 0x8A4FD8 alpha 108
đọc ra mảng xám đục trên nền cỏ, phải lên 0xB06BFF alpha 150 và co lõi sáng lại mới ra quầng.

Tốc độ chớp không có tham số nào chỉnh được: client chạy chuỗi phát một nhịp mỗi khung hình, nên
muốn chậm thì phải giữ mỗi nấc nhiều nhịp (`STEP_HOLD`). Độ dài chuỗi cũng đi qua một byte nên
trần là 255 nhịp — công cụ tự chặn.

Vòng bốn: thêm lớp nền tối dưới aura (`veil`, đậm 190) để tím đọc ra tím đậm thay vì tím xỉn.

Bản đầu của lớp này làm hỏng cả hiệu ứng và bài học đáng ghi lại: **một lớp tối bán trong suốt
phủ lên nền sáng thì ra màu XÁM**, rồi aura tím phủ tiếp lên xám thì thành tím đục. Phản xạ đầu
tiên là hạ độ đậm xuống — sai hướng, càng hạ càng xám. Cách chữa đúng là ngược lại: đẩy lõi lên
gần đặc để nó ra màu tối thật, đồng thời thu bán kính lại để phần rìa không còn mảng xám nào,
nhường chỗ cho aura toả ra ngoài vùng tối chứ không chồng lên.

Chốt mức đậm bằng cách dựng bốn bản (0/120/190/240) rồi xem cạnh nhau trên ba nền game. Không so
được như vậy thì rất dễ chọn mức chỉ hợp với đúng cái nền mình tình cờ thử: bản 240 nhìn trên nền
hang tối thì tuyệt, nhưng trên nền tuyết là một bầu dục đen rõ rệt.

Vòng ba: kéo quãng mắt mở hẳn 45 → 195 nhịp, chuỗi thành 248 nhịp. **Bản này hỏng**, và cách nó
hỏng đáng ghi lại: hiệu ứng đứng hình hẳn, không chớp nữa.

Trần của `running` không phải 255 mà là **126**, và nó không đến từ khâu truyền. Độ dài ghi bằng
`writeByte`, client đọc bằng `readUnsignedByte` — tới đó vẫn 255. Cái chặn nằm ở con trỏ chạy
chuỗi trong client: `private byte dD`, quay vòng bằng `if (dD > dB.length) dD = 0`. Dài quá 126
thì dD đếm tới 127 rồi tràn xuống -128, điều kiện quay vòng vĩnh viễn sai, `dB[-128]` ném lỗi —
mà cả khối nằm trong `catch (Exception) {}` rỗng nên không có dấu hiệu gì, chỉ đứng ở khung đang
vẽ dở. Lúc treo dC đang giữ `dB[127]`, mà 127 < 195 nên đó là khung mở hẳn: mắt đứng im mở trừng.

Vòng năm chốt lại 124 nhịp: mở hẳn 74 (60% chu kỳ), khép/mở mỗi nấc 3 nhịp, nhắm 5. Đây là mức
mở lâu nhất còn giữ được động tác mượt; muốn lâu hơn phải hạ `STEP_HOLD` xuống 2, được thêm 15
nhịp nhưng khép mở giật hơn. `EffectCheck` đã sửa để bắt đúng ca này — trước đó nó điền trần 255
nên báo ĐẠT cho chính bản hỏng. Đã soát cả bảng `effect_data`, không hiệu ứng nào khác vượt 126.

Bản lùi: `build/dbbackup/effect203-truoc.tsv`, `build/dbbackup/effect203-anh-truoc/`.

Đã xem tận mắt trong game trên nhân vật `itachi` (id 1) — chính lần xem đó phát hiện ra lỗi con
trỏ byte ở vòng ba, thứ mà ảnh render và `EffectCheck` bản cũ đều không bắt được.

### 20/08/2026 — danh hiệu Fan Cứng đổi thành mắt Mangekyō của Itachi

Hiệu ứng 231 (vật phẩm 1131) từ băng-rôn chữ "Fan Cứng" xanh lá 73x23, 20 khung xếp lưới 3x7,
thành hai con mắt đỏ với hoa văn chong chóng ba lưỡi hai mắt khung truyện tranh, hoa văn xoắn, aura mỏng, 102x35, bảy khung.

Gộp luôn `MakeRinneganEffect.java` thành `tools/MakeEyeEffect.java`: phần hình quả hạnh, động tác
hé mí, aura, nền tối, giới hạn byte và xuất JSON đều dùng chung, chỉ hoa văn trong tròng và bảng
màu là khác. Ba kiểu: `luanhoi`, `sharingan`, `mangekyo`. Trước khi bỏ công cụ cũ đã dựng lại
hiệu ứng 203 bằng công cụ mới và `cmp` từng tấm — trùng **đúng từng byte** với bản đang chạy, nên
biết chắc phần gộp không làm lệch gì.

**Neo bán kính hoa văn theo NỬA CHIỀU CAO mắt, không theo chiều rộng.** Bản đầu tôi lấy theo chiều
rộng; mắt rộng gấp 1,9 lần chiều cao nên hoa văn vọt ra ngoài mí và bị cắt sạch phần trên dưới —
Sharingan chỉ còn trơ lại mỗi cái vòng tròn, mất cả ba dấu phẩy.

Và bỏ hẳn vòng tròn nối ba dấu phẩy: ở cỡ này vòng, ba dấu phẩy với con ngươi dính thành một cục
đen. Bỏ vòng, thu con ngươi lại thì ba dấu phẩy mới tách bạch. Mangekyō thì ngược lại — ba lưỡi đã
chụm sẵn vào tâm nên bỏ luôn con ngươi.

Việc này còn sửa một lỗi có sẵn: ảnh 231 mức 3 và mức 4 vốn **chỉ là bản sao của mức 2**
(438x322 cả ba), trong khi toạ độ trong `effect_data` được client nhân theo mức — nên hiệu ứng
gốc vẽ sai ở hai mức thu phóng cao. `EffectCheck 231` chạy trên bản gốc báo SAI đúng hai dòng đó.
Bản mới sinh đủ bốn mức đúng tỉ lệ.

Nới vùng aura đỏ: khung 84x27 → **160x35** (rộng gấp đôi, cao thêm 30%). Vùng đỏ chính là cỡ
khung nên muốn nó rộng ra thì phải nới phần chừa quanh mắt — và hai chiều KHÔNG cùng một ngân
sách. Chừa ngang gần như thoải mái: khung rộng tối đa 255 mà hai con mắt chỉ chiếm 48. Chừa dọc
thì chật vì nó **nhân với số khung**: cả tấm cao tối đa 255, nên cao thêm 8 pixel mỗi khung là
phải bỏ 2 khung của động tác hé mí (9 → 7). Công cụ tự chặn và nói rõ chừa dọc tốn gấp mấy lần.

Vẽ lại hoa văn cho sát nguyên tác: ba lưỡi liềm xoáy + vòng tròng mắt + chấm tâm, và mắt cao
11 → 13 (bù bằng chừa dọc 12 → 11 nên khung không đổi chiều cao).

**Điểm mấu chốt để ra chong chóng chứ không ra nan quạt: đầu trong của lưỡi phải lệch góc so với
đầu ngoài.** Nan quạt là hai đầu cùng một hướng bán kính; lưỡi liềm là đầu trong bị xoay đi một
góc, nên cả lưỡi vừa thu nhỏ vừa quay khi chạy vào tâm.

Bản đầu tôi đặt điểm điều khiển Bezier bằng tay và ra ba cái nêm béo nuốt hết phần đỏ — đường bậc
ba chỉ uốn được hai lần, mà vừa xoay vừa thu thì phải uốn liên tục. Cách chữa: bỏ Bezier, đi bộ
dọc một đường xoắn (nội suy đều cả góc lẫn bán kính rồi nối điểm). Hình ra đúng theo công thức,
khỏi mò, và ba tham số SPAN / TWIST / R_IN chỉnh được trực tiếp.

Khung mắt bỏ hình quả hạnh đối xứng, chuyển sang kiểu truyện tranh: khoé trong hạ thấp và nhọn,
khoé ngoài nâng cao vuốt thành đuôi hất, đỉnh mí trên lệch về phía khoé trong, mí dưới gần thẳng.
Mí trên đối xứng chính là thứ làm hình quả hạnh trông hiền. Hai mắt lật gương nhau, không thì cả
khuôn mặt như đang liếc.

Hai chỗ phải sửa kèm theo, đều do khung hết đối xứng:

- **Hoa văn phải neo vào tâm phần mắt MỞ, không phải tâm khung.** Khung truyện tranh dồn phần mở
  về phía khoé trong và hơi lên trên; neo vào giữa khung thì hoa văn lệch ra rìa, mà hai mắt soi
  gương nên lệch ngược chiều nhau, nhìn rất rõ.
- **Khung quả hạnh phải trả về thẳng, không đi qua phép lật.** Nó vốn đối xứng nên lật hay không
  cũng cùng một hình, nhưng lật rồi lật lại làm toạ độ lệch một phần nghìn pixel, đủ để khử răng
  cưa ra khác và bài kiểm "dựng lại 203 phải trùng từng byte" hoá đỏ.

Bản khung elip đã sao lưu riêng ở `build/dbbackup/effect231-mat-elip/` (có README kèm lệnh lùi).
Đừng nhầm với `effect231-anh-truoc/` — cái đó là băng-rôn chữ xanh lá nguyên bản của server.

Kiểu thứ ba, `glare` — dựng theo ảnh mẫu user đưa (hai con mắt Sharingan trong bóng tối). Khác
hai kiểu kia ở bốn chỗ, và cả bốn đều cần thiết:

- **Có lòng trắng.** Tròng đỏ là một vòng TRÒN nhỏ hơn khe mắt và bị mí trên cắt mất chỏm, thay
  vì đổ đỏ kín cả khe. Chính khoảng trắng còn lại ở hai khoé mới cho ra cái nhìn gằn — đổ kín màu
  thì mắt nào cũng thành mắt thú.
- **Nền đen tuyền, không quầng đỏ.** Dùng nền ngả đỏ của bảng màu sharingan thì quanh mắt hiện
  quầng nâu; thứ làm ảnh mẫu ấn tượng chính là bóng tối KHÔNG màu. Quầng sát mí đổi sang ánh xám
  mờ, như da bắt sáng.
- **Mí trên gần THẲNG.** Điểm điều khiển đặt sát đường nối hai đầu. Cung tròn dù nhọn hai đầu vẫn
  cho cảm giác hiền.
- **Bộ tỉ lệ riêng: mắt dẹt hơn (2,55 thay vì 1,9) và hai mắt cách nhau đúng một bề ngang mắt.**
  Đây là thứ tôi bỏ sót ở bản dựng đầu và phải làm lại: vẽ đúng từng chi tiết nhưng giữ tỉ lệ cũ
  thì tổng thể vẫn ra "hai con mắt dán cạnh nhau" chứ không ra "một ánh nhìn trong bóng tối".
  Ảnh mẫu là một khuôn mặt, không phải hai con mắt.

**User xem thật và bác cả hai bản đổi khung (manga và glare), quay về khung elip.** Ghi lại vì
đây là thứ ảnh render không nói được: cả hai bản kia soi phóng to đều đẹp và bản glare bám sát
ảnh mẫu, nhưng ở cỡ thật trong game thì không. Khung elip đối xứng, lòng mắt đổ đỏ kín, hoa văn
to rõ — đó mới là thứ đọc được ở cỡ vài chục pixel. Lần sau đổi hình dạng thì phải xem trong game
trước khi đầu tư thêm vào chi tiết.

Kiểu thứ tư, `goc` — khung mắt là một **hình lục giác dẹt, không đoạn cong nào**: mí trên và mí
dưới là hai đoạn thẳng nằm ngang, nối vào hai khoé nhọn bằng bốn đoạn xiên. Chỗ gãy giữa đoạn
ngang và đoạn xiên chính là thứ cho ra vẻ vuông vức; hình quả hạnh cong đều nên góc nào cũng tù.
Đoạn thẳng chiếm khoảng bốn phần mười bề ngang — rộng hơn thì thành chữ nhật vát góc, hẹp hơn thì
lại gần về quả hạnh.

Kèm theo là **hạ aura**: nền tối 190 → 100, quầng đỏ 180 → 40, chừa ngang 56 → 22. Khung 170x35 →
102x35, hẹp hơn 40%. Chốt bằng cách dựng ba mức rồi xem ở CỠ THẬT trên ba nền game — không phóng
to. Đây là bản đang chạy.

Kiểu thứ năm, `tron` — dựng theo ảnh mẫu thứ hai user đưa: **MỘT đĩa đỏ ở giữa**, hoa văn ba
lưỡi **xoay tròn**, không mí mắt. Đã sao lưu ở `build/dbbackup/effect231-dia-xoay/`.

Ba thứ khác hẳn bốn kiểu kia:

- **Cái động là hoa văn xoay, không phải mí hé.** `openness` trả thẳng 1.0, còn mỗi khung xoay
  thêm 120/số-khung độ. Hoa văn đối xứng ba cánh nên quay đủ 120 độ là trùng lại chính nó --
  khung cuối nối liền khung đầu, không giật. Chuỗi phát chạy đều 35 nhịp, không quãng nghỉ.
- **Lưỡi mảnh, nhọn CẢ HAI đầu.** Dựng từ hai đường xoắn trùng nhau ở hai đầu và tách ra ở quãng
  giữa, độ tách đi theo sin(pi*t) nên bằng 0 ở cả hai mũi. Cho bề dày cố định thì hai đầu cụt,
  nhìn thành ba cái dấu phẩy.
- **Chấm tâm màu ĐỎ, không vòng bao ngoài.** Ba lưỡi chụm lại đã kín tâm, khoét một lỗ đỏ mới ra
  đúng hình bản gốc; còn vòng bao thì trên nền đĩa đỏ trơn là thừa.

**Một lỗi suýt lọt:** nhánh chuỗi phát cho kiểu xoay chèn trượt vì mẫu thay thế không khớp (có
dòng chú thích chen giữa), mà `str.replace` của Python thì **im lặng khi trượt**. Ảnh ra đúng nên
nhìn qua tưởng xong, chỉ có chuỗi phát vẫn là kiểu chớp mắt 112 nhịp. Bắt được nhờ đọc con số
"chuỗi phát" trong log dựng. Từ đó mọi phép thay thế trong script sửa mã đều kèm `assert`.

**Quay lại `manga` sau khi có hoa văn xoắn — và lần này user nhận.** Cùng một khung mí đã bị bác
hôm trước, chỉ khác hai thứ: hoa văn từ ba lưỡi thẳng thành ba lưỡi liềm xoắn, và aura hạ từ 180
xuống 40. Bài học: một hình dạng bị bác chưa chắc là do hình dạng đó sai — có thể nó chỉ đang
gánh một hoa văn xấu và một vầng aura loang. Đáng thử lại sau khi đã sửa những thứ xung quanh,
thay vì gạch tên nó vĩnh viễn. Đây là bản đang chạy.

Năm bản đều dựng lại được, mỗi bản một lệnh (KHÁC mặc định, vì mặc định giữ cho hiệu ứng 203):

    manga  java -cp build/tools MakeEyeEffect mangekyo <ra> 231 -72 13 7 100  40 22 11 manga
    tron   java -cp build/tools MakeEyeEffect mangekyo <ra> 231 -72 26 7 100  40 14  5 tron
    goc    java -cp build/tools MakeEyeEffect mangekyo <ra> 231 -72 13 7 100  40 22 11 goc

    glare  java -cp build/tools MakeEyeEffect mangekyo <ra> 231 -72 14 7 235  85 20 11 glare
    manga  java -cp build/tools MakeEyeEffect mangekyo <ra> 231 -72 13 7 190 180 56 11 manga
    elip   java -cp build/tools MakeEyeEffect mangekyo <ra> 231 -72 13 7 190 180 56 11

Hai bản không dùng đã sao lưu kèm README: `build/dbbackup/effect231-mat-elip/` và
`effect231-mat-manga/`. Đừng nhầm với `effect231-anh-truoc/` — đó là băng-rôn chữ xanh lá gốc.

Bản lùi: `build/dbbackup/effect231-truoc.tsv`, `build/dbbackup/effect231-anh-truoc/`.

### 20/08/2026 — danh hiệu Đệ Tứ đổi thành 四代目火影 chữ vàng, tia chớp

Hiệu ứng 204 (vật phẩm 1153) từ băng-rôn lửa 104x35 năm khung thành dòng chữ Nhật **四代目火影**
-- đúng dòng chữ sau lưng áo choàng Hokage Đệ Tứ -- màu vàng kèm tia chớp loé, 59x15, tám khung.
Vàng và tia chớp là nhất quán với nhân vật: Minato là "Tia Chớp Vàng của Làng Lá".

`tools/MakeTitleEffect.java` nay nhận thêm id hiệu ứng và mép dưới nên dùng được cho mọi băng-rôn,
không riêng 201; thêm hai kiểu màu `hokage` (trắng ngà viền đỏ lửa, theo bảng màu cái áo) và
`chop` (vàng + tia chớp). Chữ Nhật dựng bằng font HiraginoSans-W6 có sẵn trên máy.

**Chữ đen như trên áo thật thì không dùng được** -- băng-rôn bay trên đầu, map tối là mất hút.
Đây là chỗ phải rời nguyên tác để giữ chức năng.

**Tia chớp vẽ hai lần mới ra.** Bản đầu là hai tia dài vắt hết bề ngang, đỉnh gãy lệch ngẫu nhiên
quanh một đường ngang -- kết quả nhìn như hai vệt gạch bỏ chữ. Lệch ngẫu nhiên quanh một đường thì
trung bình vẫn là đường thẳng. Sửa: tia NGẮN (chừng một phần ba bề ngang, rải rác), đỉnh gãy ĐỔI
CHIỀU luân phiên với biên độ lớn, cộng vài đốm sáng rời. Chính mấy đốm rời mới khiến mắt đọc ra
"tia lửa điện" chứ không "nét vẽ".

Mọi số ngẫu nhiên gieo theo chỉ số khung, không dùng `Math.random`, nên chạy lại công cụ ra đúng
bộ ảnh cũ và `cmp` đối chiếu được.

Cỡ chữ chốt ở 8 (cao chữ ở mức thu phóng 1). Cao 10 thì to hơn băng-rôn Đệ Nhất Ninja; cao 7 thì
chữ 影 bắt đầu dính nét trong. Chọn bằng cách render cả ba ở CỠ THẬT rồi so, không phóng to.

Bản lùi: `build/dbbackup/effect204-truoc/` (băng-rôn lửa gốc, kèm README).

Có thử thêm **kunai ba mũi của Phi Lôi Thần** kẹp hai đầu dòng chữ, user xem xong bảo bỏ. Đã gỡ
sạch khỏi công cụ chứ không để lại sau một cờ tắt: code chết thì lần sau có người bật nhầm. Nếu
sau này muốn làm lại thì lưu ý cái bẫy đã gặp -- hàm vẽ phải nhận thẳng toạ độ MŨI và toạ độ
CHUÔI, đừng nhận một cờ "chĩa ra ngoài" rồi tự tính từ mép khung: làm vậy con bên trái chạy hẳn
ra ngoài ảnh, hướng thì đúng mà gốc toạ độ sai.

    java -cp build/tools MakeTitleEffect "四代目火影" <ra> chop HiraginoSans-W6 8 204 -55

### 20/08/2026 — danh hiệu Đệ Nhị đổi thành "Dân chơi cấm thuật", hệ thuỷ

Hiệu ứng 202 (vật phẩm 1151) từ băng-rôn lửa 104x58 năm khung thành dòng chữ xanh nước, gợn sóng
chạy trên mặt chữ và bọt khí nổi lên, 98x14, tám khung. Thêm kiểu `thuy` vào `MakeTitleEffect`.

Hai chỗ quyết định việc nó ra "nước" chứ không ra "mấy đường kẻ":

- **Gợn sóng cắt theo HÌNH CHỮ.** Chỉ sáng trên nét chữ, không vắt ngang nền -- đúng cảm giác ánh
  sáng khúc xạ qua nước rọi lên vật. Không cắt thì thành sợi dây vắt qua băng-rôn, đúng cái lỗi
  đã gặp ở tia chớp của hiệu ứng 204.
- **Hai đường sin khác tần số và NGƯỢC CHIỀU chạy.** Cùng tần số thì hai đường chồng thành một
  vạch dày đều, mất hẳn vẻ lăn tăn.

Bọt khí thì không cắt theo chữ (chúng nổi trước mặt chữ), đi lên đều theo số khung và quay vòng
bằng phép chia dư nên khung cuối nối liền khung đầu.

**Chuỗi phát chạy đều, không có quãng nghỉ** -- ngược hẳn với tia chớp. Nước mà có một khung trơ
ra là mắt bắt được ngay chỗ vòng lặp nối lại. 8 khung x 4 nhịp = 32 nhịp, lặp liên tục.

Bản lùi: `build/dbbackup/effect202-truoc/` (băng-rôn lửa gốc, kèm README).

    java -cp build/tools MakeTitleEffect "Dân chơi cấm thuật" <ra> thuy Tahoma-Bold 7 202 -42

### 20/08/2026 — danh hiệu Akatsuki (mây đỏ), lấp một ô vốn hỏng sẵn

Vật phẩm 1133 đổi tên `V_VIP` → **Akatsuki**, hiệu ứng 211/212/213 là đám mây đỏ viền trắng trôi
bồng bềnh, 52x33, sáu khung. Dựng bằng `tools/MakeAkatsukiEffect.java`.

**Đây là thêm mới chứ không phải thay thế.** Trước đó `Char.java` vẫn gọi `addEffect(301…)` cho
người mặc V_VIP, nhưng 301/302/303 (id gốc) **không có ảnh và không có hàng `effect_data` nào** — ai mặc
V_VIP cũng chỉ thấy một món trắng trơn. Nên phải INSERT ba hàng mới, không phải UPDATE, và không
có gì để lùi về.

Ba hàng giống hệt nhau vì `Char.java` chọn theo `getSys()` (1 hoa / 2 băng / 3 phong) mà Akatsuki
không chia hệ. Lưu ý `getSys()` trả **0** cho người chưa vào trường (classId 0) — những người đó
mặc vào vẫn không thấy gì, đúng như mã gốc.

**Vì sao chọn 1133 chứ không phải một ô Noname.** User hỏi làm từ Noname (1154/1155/1162), nhưng
ba ô đó **không có nhánh nào** trong khối `// hiệu ứng danh hiệu`, nên phải vá bytecode `Char.class`
(Lombok). Có một bản vá 2 byte rất gọn — nhánh V_VIP test `sipush 1133`, đổi thành `1154` là
chuyển nguyên nhánh sang, không dời offset, không dựng lại StackMapTable. Nhưng làm vậy thì V_VIP
mất nhánh, y như đổi tên nó, tức hai đường tốn NGANG NHAU. Chọn đường không đụng bytecode để mã
nguồn và class không nói hai đằng. Cả bốn món đều không ai sở hữu nên không ảnh hưởng ai.

**Phải nâng `game.item.version` 24 → 25.** Client cache bảng item theo số này; không nâng thì
trong game vẫn hiện tên `V_VIP`, và rất dễ ngồi nghi ngờ câu UPDATE có chạy không.

**Mây làm lại sáu lần, và năm lần đầu sai ở PHƯƠNG PHÁP chứ không ở tham số.** Tôi dựng mây
bằng phép hợp mấy hình tròn chồng nhau rồi chỉnh đi chỉnh lại cái đuôi — user xem xong: "chả
giống đám mây gì cả". Đúng, và không cách nào chỉnh cho đúng được: hợp hình tròn thì mọi chỗ lồi
đều là cung tròn cùng kiểu, nên kết quả luôn là một cục tròn tròn.

Biểu tượng gốc là hình vẽ tay, và ba nét làm nên nó thì phép hợp hình tròn không tạo ra được:
ba bướu **khác bán kính**, đáy **võng** chứ không phình, đuôi **cuộn ngược vào trong**. Cách duy
nhất là chép thẳng đường viền ra toạ độ — hàm `cloud()` giờ là một `Path2D` với các điểm ghi
trong ô đơn vị 0..1 rồi nhân theo cỡ thật, nên đổi kích thước không phải sửa điểm nào.

Bài học: khi chỉnh tham số ba lần mà kết quả vẫn cùng một loại sai, thì cái sai nằm ở cách dựng
hình, không nằm ở con số. Dừng lại đổi cách sớm hơn.

**Id hiệu ứng phải <= 255, và đây là chỗ đã làm mất một vòng.** Dựng xong, đổi tên xong, khởi
động lại xong — vào game không hiện gì, trong khi mọi danh hiệu khác vẫn bình thường. Nguyên nhân:
id đi qua HAI chặng với hai bề rộng khác nhau.

    MapService.addEffect    ds.writeShort(id)                   -> 301 lọt bình thường
    Service.sendImgEffect   isVersionAbove(239) ? writeShort     -> client 1.4.8 (=148) KHÔNG đạt
                                                : writeByte      -> 301 & 0xFF = 45

Client được báo "vẽ hiệu ứng 301" nhưng nhận ảnh dán nhãn 45, nên đi tìm 301 không thấy và không
vẽ gì — lặng lẽ, không lỗi. Mọi hiệu ứng đang chạy đều <= 255 nên chưa từng chạm trần này; và
đây cũng chính là lý do danh hiệu V_VIP của server gốc chưa bao giờ hiện gì.

Sửa bằng `tools/FixAkatsukiEffectId.java`: đổi ba hằng `sipush 301|302|303` trong `Char.class`
thành **211/212/213** (ba id trống, không có hàng effect_data và không có ảnh). Mỗi chỗ 2 byte,
dài y hệt lệnh cũ — không dời offset, không dựng lại StackMapTable, không đụng bể hằng. Đã kiểm
bằng javap rằng 11 chỗ khác trong lớp có 211/212/213 đều là so sánh, id vật phẩm hoặc id quái,
không phải lời gọi addEffect. Bản lùi: `build/dbbackup/char-truoc-akatsuki/Char.class`.

    java -cp build/tools MakeAkatsukiEffect <thư mục ra> 211 -58
    (rồi chép ảnh sang 212.png và 213.png, ba hàng effect_data giống hệt nhau)

### Vật phẩm chưa có chỉ số (khảo sát, chưa sửa)

`tools/checks/StatCheck.java` dựng thử cả 526 món trang bị bằng đúng đường `GiveItem.build` rồi
đếm chỉ số. Kết quả: 155 món ra món trắng, trong đó 48 là đồ thời trang (cột `fashion` > 0, trống
là đúng) và khoảng 63 món là bộ tân thủ cấp 1–9 với các bộ khoác ngoài. Còn lại là lỗ hổng thật:

- **18 vĩ thú Sơ Cấp, id 924–941.** `Item.initOption` có khối chỉ số cho trung cấp (994–1011),
  cao cấp (1012–1029), siêu cấp (1030–1047) nhưng không có khối nào cho sơ cấp — đúng kiểu lỗ
  hổng đã gặp ở Thiên Vương.
- **11 mặt nạ cấp 20–80**, đáng chú ý là Deidara và Tobi cấp 80 trắng trơn trong khi Mặt nạ sát
  thủ cấp 20 có đủ chỉ số.
- **7 thú nuôi cấp 70**: Hỏa long, Hải mã 1–3, Dị Long 1–3.

Hai món có dòng cửa hàng nhưng cột `options` rỗng, sửa thẳng CSDL được: Dơi đen (246) và Mặt nạ
fasshon (345).

## Bộ trang phục Kage — xong ngày 21/08

Ba món mới, mỗi món mang mảnh hình riêng, dựng theo đúng lối mặt nạ Itachi:

| id | tên | loại | mảnh | biểu tượng |
|----|-----|------|------|-----------|
| 1242 | Nón Kage | 11 (mặt nạ) | 309 (type 0, 8 khung) | 3082 |
| 1243 | Áo Kage | 2 (áo) | 310 (type 1, 18 khung) | 3083 |
| 1244 | Quần Kage | 6 (quần) | 311 (type 2, 10 khung) | 3084 |

Ảnh mảnh nằm ở `nj_image` 3053–3055 (nón) và 3056–3080 (thân, chân), liền mạch không
thủng lỗ; bốn biểu tượng để **trên** mức cao nhất của `nj_image` nên không có hàng nào —
đúng luật, vì biểu tượng có hàng là client cắt lố khung 255×255 rồi treo. Ảnh cắt từ
`Data/Img/Effect/<1..4>/205,206,207.png`.

**Món hoàn chỉnh** là cái nón: `FashionFromEquip` có nhánh `case 1242` đặt luôn
`body = 310`, `leg = 311`, nên chỉ đội nón là ra cả bộ — y như Mặt nạ Thánh Gióng. Ba
món vẫn mặc rời được từng cái.

Số lệch của mảnh áo (310) ban đầu chép nguyên từ bộ Thánh Gióng (206) nên áo lệch trục
so với quần và nón — bộ đó có `dx` âm từ -4 đến -15 vì ảnh gốc của nó cắt chừa lề trái,
còn ảnh áo Kage cắt sát viền. Nay đặt `dx = 0` cho mọi khung theo đúng lối mảnh áo của
Mặt nạ Jirai (224), `dy` căn mép dưới theo mảnh đó. Đã xem lại cả lúc đứng lẫn lúc chạy.

Mảnh nón (309) cũng dính lỗi cùng kiểu, chỉ khác dấu: nó chép số lệch của mặt nạ Itachi
(mảnh 308) nhưng ảnh nón rộng hơn ảnh mặt nạ 6 pixel, mà neo vẽ là góc trái-trên nên nón
thừa ra bên phải. Nay `dx` trừ đi nửa phần rộng dôi (`-3,-3,-3,-1`), `dy` chốt ở `-2,1,1,-1`.

Riêng khung 1 của mảnh áo là tư thế đứng, giữ nguyên; các khung còn lại (chạy, nhảy,
đánh) hạ thêm 2 pixel vì cánh tay bị treo cao. Xem bảng khung ở
`build/selftest` — dựng bằng `Sheet.java` để biết khung nào là tư thế nào.

Đã bump `game.data.version` 29→36 **và** `game.item.version` 11→12. Bỏ quên cái thứ hai
là vật phẩm hiện ra không tên không id, bấm Sử dụng không ăn.

Đã tự kiểm trong game bằng tài khoản `kiemthu`/`test123` (nhân vật `kagetest`, id 5):
vào màn chọn nhân vật không treo, mở túi không treo, mặc từng món và mặc nón đều lên
đúng hình. Ba món cũng đã bỏ sẵn vào túi nhân vật `itachi`.

## Tool căn mảnh trang phục — `tools/CanManh.java`

Dựng ngày 21/08 để thôi phải vào game mỗi lần nắn một pixel. Cửa sổ Swing nói thẳng với MySQL,
không cần máy chủ chạy, không cần đăng nhập.

```
javac -encoding UTF-8 -cp work/server/NSO_KEM/target/Nso-jar-with-dependencies.jar -d build/tools tools/CanManh.java
java -cp "build/tools:work/server/NSO_KEM/target/Nso-jar-with-dependencies.jar" CanManh work/server/NSO_KEM
java ... CanManh work/server/NSO_KEM --kiem      # đọc rồi dựng lại cả 312 mảnh, báo chỗ lệch
```

Tham số sau thư mục máy chủ: `đầu thân chân` rồi `đầu thân chân đối chiếu`. Mặc định là bộ Kage
(309/310/311) đối chiếu bộ Thánh Gióng (308/206/207).

**Công thức vẽ lấy đúng từ client** (bản không bị làm rối tên ở thư mục `gpt`, lớp `Char.loadData`):

```
int[] o = CharInfo[cf][slot];                 // slot 0 đầu, 1 chân, 2 thân, 3 mảnh phụ
PartImage p = manh.pi[o[0]];
SmallImage.gameAB(g, p.id, cx + o[1] + p.dx, cy - o[2] + p.dy - 10, 0, 0);
```

Số cuối là neo **0 = góc trái-trên**, nên ảnh rộng hẹp khác nhau là lệch ngang — chính là cái bẫy
làm bộ Kage lệch trục. `CharInfo` là mảng ba tầng dựng thẳng bằng bytecode trong `loadData`, đã mô
phỏng lại bytecode để bóc ra, lưu ở `tools/charinfo.json`. 30 khung:

| khung | trạng thái |
|-------|-----------|
| 0–1 | đứng |
| 2–7 | chạy |
| 8–11 | **lộn nhào** — chỉ vẽ mảnh đầu, khung ảnh 4–7; thân và chân tắt hẳn |
| 12 | nhảy |
| 13–20 | đánh A |
| 21–29 | đánh B |

Nhờ bảng này mà phát hiện bộ Kage thiếu hẳn đoạn lộn nhào: bốn khung cuối của mảnh 309 vẫn để ảnh
đầu mặc định 1–4. Tấm `Effect/205.png` có sẵn bốn ô xoáy đỏ trắng (ô 6–9) đúng cho việc đó, cùng
kích thước với ảnh mặc định nên `dx, dy` để 0 là khớp. Nay đã cắt thành ảnh 3081–3084 và ghép vào.
Bốn biểu tượng vật phẩm dời lên 3085–3088 để `nj_image` không thủng lỗ.

Bộ mặt nạ Itachi (mảnh 308) vẫn còn thiếu đoạn lộn nhào y như vậy — chưa xử lý.

## Trang bị 2 đổi được diện mạo — vá client, ngày 21/08

**Vì sao xưa nay không đổi được.** Máy chủ vẫn tính và gửi đủ mười số thời trang trong mỗi gói mô
tả nhân vật. Client thì đọc đủ mười số ấy chỉ để giữ nhịp luồng rồi **vứt luôn**:

```
bipush 10 · newarray short · astore n     dựng mảng
vòng lặp readShort() mười lần              đọc cho khỏi lệch
goto ...                                   không ai đọc lại biến đó nữa
```

Lớp `Char` của client cũng không có trường nào để chứa. Bản v4 trong thư mục `gpt` y hệt, nên
không có gì để chép sang — phải tự làm.

**Cách vá.** `tools/ThoiTrangHook.java` dùng ASM chèn hai lệnh ngay sau vòng lặp, gọi
`ThoiTrang.ap(nhânVật, mảng)` (`mod-src/ThoiTrang.java`). Ba số đầu áp lên `head/body/leg` khi
không âm — đúng ba trường client vốn đã biết vẽ từ `nj_part`. Không thêm nhánh rẽ nào nên bảng
khung ngăn xếp cũ vẫn đúng, chỉ cần ASM tính lại độ sâu.

Có **ba** chỗ đọc mảng ấy, và không cùng một chủ: hai chỗ lấy nhân vật qua `getMyChar`, chỗ thứ ba
nằm trong hàm tĩnh nhận nhân vật khác qua tham số. Bộ vá không đoán mà nhìn lệnh `putfield` gán
tóc/thân/chân ngay phía trên rồi nhân bản đúng lệnh đã đẩy nhân vật lên ngăn xếp — lệnh ấy luôn
cách `putfield` đúng bốn nhịp. Lấy nhầm là đồ người khác đắp lên người mình.

**Phía máy chủ phải chặn lại.** Sau khi client biết vẽ, mọi mã cải trang cũ sẽ bỗng vẽ ra mảnh
`nj_part` trùng số — mà những số đó vốn lấy nhầm từ bảng khác (mã 205 trỏ 206/207 chính là thân và
chân bộ Thánh Gióng). Nên `FashionFromEquip` nay dồn hết bốn chỗ ghi `ID_HAIR/ID_BODY/ID_LEG` về
một cửa `datCaiTrang()`, tra bảng `caiTrang()`; mã không có trong bảng thì trả `-1` và giữ nguyên
hành vi cũ là không đổi hình. Hiện bảng có đúng một dòng:

| mã | đầu | thân | chân | bộ |
|----|-----|------|------|-----|
| 205 | 309 | 310 | 308 | Kage nam (vật phẩm 1236 "Áo Kage nam") |

Mấy mã cũ 37/40/55/58/67/70 và mã 208 (Áo Kage nữ) để ngoài bảng, cần dò lại từng cái mới thêm.

**Kiểm.** Cả 187 lớp trong jar mới nạp và qua kiểm bytecode của máy ảo, không lỗi; lớp có chèn và
lớp phụ đều nạp riêng được. Bản thử nằm ở `test/NSO-mod.jar`, chưa đẩy ra `dist/share`.

### Ba món Kage nay dùng được ở cả hai ô, mỗi món một phần

Trước đó đội mỗi cái nón là ra cả bộ, vì `FashionFromEquip` có nhánh `case 1242` đặt luôn thân và
chân. Nhánh đó đã bỏ. Nay mỗi món chỉ đổi đúng phần của nó, ở cả trang bị thường lẫn trang bị 2.

| id | tên | loại | mảnh (tb1) | mã cải trang (tb2) | đổi phần |
|----|-----|------|-----------|--------------------|----------|
| 1242 | Nón Kage | 11 | 309 | 260 | đầu |
| 1243 | Áo Kage | 2 | 310 | 261 | thân |
| 1241 | Quần Kage | 6 | 308 | 262 | chân |
| 1236 | Áo Kage nam | 2 | 0 | 205 | cả bộ (món cũ, giữ nguyên) |

Giữ nguyên cột `part` chứ không xoá, nên nếu client không chịu nhận món nào vào ô trang bị 2 thì
món đó vẫn mặc được như trang bị thường y như cũ.

`gopCaiTrang()` gộp chứ không ghi đè: mỗi món chỉ ghi phần nó có, phần nào để `-1` thì giữ nguyên
thứ đang mặc. Nhờ vậy ba món rời đắp chồng lên nhau ra đủ bộ, mà đeo một món thì chỉ đổi một chỗ.

**Chưa chắc**: chưa từng có món **quần** nào làm trang bị 2 trong bản này -- lọc cả bảng `item`
thì loại 6 vắng mặt hoàn toàn ở cột `fashion`. Rất có thể client không cho bỏ quần vào ô đó. Nếu
vậy thì gộp phần chân vào mã của áo là xong.

Icon của 1236 đang là cái nón, đã trỏ lại icon áo (3087).

### Đổi tên thành bộ Hokage, và bỏ vai trò trang bị 1

Ba món nay chỉ còn là trang phục trang bị 2:

| id | tên | loại | mã cải trang | mảnh áp lên |
|----|-----|------|--------------|-------------|
| 1242 | Nón Hokage | 11 | 260 | đầu 309 |
| 1243 | Áo Hokage | 2 | 261 | thân 310 |
| 1241 | Quần Hokage | 6 | 262 | chân 308 |

Cột `part` của cả ba đã về 0 (trước là 309/310/308). Muốn trả lại vai trò trang bị 1 thì đặt lại
đúng ba số đó là xong, mảnh vẫn còn nguyên trong `nj_part`.

Nỗi lo "quần không làm trang bị 2 được" đã bị bác bằng chứng cứ: `players.fashion` của nhân vật
thử có đủ cả ba món, nghĩa là client nhận loại 6 vào ô đó bình thường -- chỉ là bản gốc chưa từng
làm món quần nào như vậy.

Đổi tên phải sửa ở hai nơi: bảng `item`, và chuỗi `name` nằm sẵn trong `bag`/`fashion` của từng
nhân vật. Chỗ thứ hai chỉ là nhãn máy chủ ghi lại lúc lưu, vào lại là lấy tên mới từ bảng item.

### Dò hoạt ảnh nón theo đúng bản gốc

`effect_data` còn hai cột chưa ai đụng tới: `frames` và `frame_char`. Với hiệu ứng 205 thì `frames`
là bảng **60 khung**, và 30 khung đầu khớp một-một với 30 khung `CharInfo` của nhân vật. Mỗi khung
ghi rõ dùng ô nào trên tấm và lệch bao nhiêu. Đây là bản thiết kế gốc của cái nón, quý hơn mọi
phỏng đoán bằng mắt.

Gom 13 ô của tấm 205 lại thì **chỉ có 9 hình khác nhau** -- năm ô nhìn thẳng (0, 1, 5, 11, 12)
giống nhau từng điểm ảnh. Nhờ vậy hệ mảnh 8 khung của client đủ chỗ gần như trọn vẹn:

| ô mảnh | dùng cho | ô ảnh bản gốc dùng | ghi chú |
|--------|----------|--------------------|---------|
| 0 | đứng và vài khung lẻ | 0, 1, 5 → cùng một hình | khớp trọn vẹn |
| 1 | chạy | 2, 3, 4 | phải chọn một trong ba |
| 2 | đánh | 2, 3, 4 | phải chọn một trong ba |
| 3 | khung 21 | 10 | khớp trọn vẹn |
| 4–7 | lộn nhào | 6, 7, 8, 9 | khớp trọn vẹn |

**Không có hoạt ảnh nón riêng cho cái nhún lúc đứng yên.** Hai khung đứng trỏ vào hai ô khác số
nhưng cùng một hình; cái nhún do `CharInfo` lo (lệch y 32 rồi 33), và bản gốc cũng chênh đúng 1
pixel như vậy. Phần này vốn đã chạy đúng.

Nhưng lấy `CharInfo[cf][0][2] + frames[cf].dy` ra so thì lộ ba chỗ lệch thật, đã sửa:

| ô mảnh | nên lệch y | đang để | sửa thành |
|--------|-----------|---------|-----------|
| 1 chạy | −0.6 | +1 | **−1** |
| 2 đánh | −3.0 | +1 | **−3** |
| 3 khung 21 | −2.0 | −1 | **−2** |

Ô 0 (đứng) tính ra −2.8 nhưng để nguyên −2 vì đó là con số đã nắn bằng mắt và ưng rồi. Bốn ô lộn
nhào bản gốc để cao hơn 3 pixel, nhưng ảnh xoay của mình trùng đúng kích thước ảnh đầu mặc định
nên để 0 cho khớp quả cầu gốc; chưa đổi.

Còn một chỗ có thể làm nữa: ô mảnh 2 (đánh) đang dùng ô ảnh 2, mà đếm trong bản gốc thì ô 4 hay
gặp hơn (5 lần so với 3). Chênh lệch nhỏ, chưa làm.

### Xoá "Áo Kage nam" (1236), Áo Hokage dồn vào chỗ trống

Bảng vật phẩm gửi xuống client **theo vị trí, không kèm id**: `ItemManager.setData()` ghi số lượng
rồi đổ từng dòng, client đánh số theo thứ tự. Nên id phải liền mạch, xoá giữa bảng là phải dồn.

Cách làm: gỡ 1236 khỏi túi và ô trang bị của mọi nhân vật → xoá dòng 1236 → dời **1243 Áo Hokage**
xuống 1236 → đổi luôn id ấy trong `bag`/`fashion` của những ai đang giữ. Ba món Hokage nay là
1236 áo, 1241 quần, 1242 nón; bảng chạy 0..1242 không lỗ.

Nhánh `case 205` trong `caiTrang()` đã bỏ vì món dùng nó không còn.

Đã sao lưu trước khi xoá: `build/backup/1236-item.sql` và `players-truoc-xoa-1236.sql`.

### Vì sao nón không động được theo nhịp chạy

Đã thử hướng gắn nón như **hiệu ứng** (giống danh hiệu "Đệ Nhất Ninja" dùng `addEffect`). Hướng này
chết, và bytecode client nói rõ:

```
i0 = i0 + 1
if (i0 >= eff0.arrEfInfo.length) { eff0 = null; dx0 = dy0 = i0 = 0; }
```

Hiệu ứng tự tăng chỉ số khung mỗi lần vẽ, chạy hết mảng là **tự xoá mình đi**. Nó là hoạt ảnh một
lần, không hề bám theo tư thế nhân vật. Vậy 60 khung trong `effect_data[205].frames` là một đoạn
hoạt ảnh chơi một lượt, không phải bộ trang phục thường trực.

Hướng mảnh thì đụng trần cứng. `Part(int type)` cấp phát số khung theo loại, cả hai đầu đều ghi
chết -- client là `bipush 8` cho loại 0, máy chủ là `new Part(type)`. Mảnh đầu có đúng **8 ô** và
cả 8 đã dùng hết: ô 0 đứng, ô 1 chạy, ô 2 đánh, ô 3 khung 21, ô 4–7 lộn nhào. `CharInfo` dồn cả
**năm khung chạy vào chung ô 1**, nên một ảnh phải gánh cả nhịp chạy, trong khi bản gốc đổi qua ba
ảnh 2 → 3 → 4 → 3 → 2.

Muốn nón động theo nhịp chạy thì phải nới số khung của mảnh đầu, tức vá **ba** chỗ ăn khớp nhau:
`bipush 8` trong client, `Part` bên máy chủ, và bảng `CharInfo` để trỏ sang ô mới. Chỗ thứ ba là
mảng ba tầng dựng thẳng bằng bytecode. Đổi cấu trúc này là đụng tới mọi mặt nạ trong game, đổi lấy
việc nón nghiêng theo nhịp chạy -- chưa làm.

Có làm được một chỗ nhỏ: ô mảnh 2 (đánh) trước dùng ô ảnh 2, nay đổi sang **ô ảnh 4** (ảnh 3082) vì
đếm trong bản gốc thì ô 4 hay gặp hơn -- 5 lần so với 3.

### Dựng lại thân và quần theo đúng bảng gốc — sửa lỗi chân thò ra

Hai phát hiện làm sáng tỏ mọi thứ, đều nằm ở cột `frames` của `effect_data`:

**Tấm 207 mỗi khung có hai lớp.** Một lớp thấp (`dy` khoảng −8…−12) là cái quần, một lớp cao
(`dy` khoảng −42…−45) là thứ khác vẽ ngang tầm đầu. Thứ tự hai lớp **đảo qua đảo lại** giữa các
khung, nên cách đọc cũ là lấy lớp `[0]` sẽ nhặt nhầm lớp cao ở một số khung. Đó chính là khung có
"cái chân thò hẳn ra ngoài". Cách đọc đúng: chọn lớp có `dy` gần 0 nhất.

**Tấm 206 khung 13–20 có tới năm lớp.** Lớp đầu là thân, mấy lớp sau là hiệu ứng đòn đánh -- mấy
mảng màu tím. Lớp thân luôn là `[0]`.

Sau khi đọc đúng lớp thì bảng khớp một-một sạch sẽ, và lộ ra lỗi thứ hai: `CharInfo` **không dùng
ô mảnh 9** của thân, nhảy thẳng từ 8 sang 10. Bản cũ gán ảnh tuần tự 1..16 nên từ ô 9 trở đi lệch
hết một nhịp.

| ô mảnh thân | 1 | 2 | 3 | 4 | 5 | 6 | 7 | 8 | 9 | 10 | 11 | 12 | 13 | 14 | 15 | 16 | 17 |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| ô ảnh tấm 206 | 0 | 1 | 2 | 3 | 4 | 5 | 6 | 7 | – | 8 | 13 | 18 | 19 | 21 | 22 | 23 | 24 |

| ô mảnh chân | 1 | 2 | 3 | 4 | 5 | 6 | 7 | 8 | 9 |
|---|---|---|---|---|---|---|---|---|---|
| ô ảnh tấm 207 | 1 | 4 | 5 | 7 | 9 | 11 | 14 | 15 | 16 |

Lệch ngang tính ra gần như bằng 0 khắp nơi, đúng như đã đặt. Lệch dọc nay lấy thẳng từ bản gốc:
−1 cho hầu hết, −3 cho nhóm đòn đánh, −4 cho ô chân 7.

Số ảnh cần đúng bằng số cũ (16 thân, 9 chân) nên ghi đè tại chỗ lên `nj_image` 3056–3080, id
không xê dịch, không phải dồn bảng.

### Bộ Kage nữ (tấm 208–210) và chế độ xuất ảnh của tool

Ba tấm 208/209/210 có cấu trúc y hệt 205/206/207 (13/25/19 ô, 60/30/60 khung), nên áp nguyên
phương pháp đã rút ra: đọc `effect_data.frames`, chiếu qua `CharInfo`, chọn đúng lớp (đầu và thân
lấy lớp `[0]`, chân lấy lớp có `dy` gần 0 nhất). Bảng ra khớp một-một, cùng bộ ô ảnh như bản nam.

| | mảnh | ảnh | vật phẩm | mã cải trang |
|---|------|-----|----------|--------------|
| nón | 311 | 3083–3090 | 1243 | 263 |
| áo | 312 | 3091–3106 | 1244 | 264 |
| quần | 313 | 3107–3115 | 1245 | 265 |

Vật phẩm để `gender = 0`. Ba biểu tượng ở 3119–3121, và **biểu tượng bộ nam phải dời lên 3116–3118
trước khi cắt**, vì id cũ 3086–3088 nay rơi vào vùng ảnh mới -- biểu tượng mà có hàng `nj_image` là
client cắt lố khung rồi treo.

**Chế độ xuất ảnh**: `CanManh ... --anh <tệp> <đầu> <thân> <chân>` dựng cả sáu đoạn hoạt ảnh ra một
tấm png, khỏi mở cửa sổ và khỏi vào game. Đây là cách kiểm một bộ mới nhanh nhất -- nhìn tấm là
thấy ngay khung nào lệch hay lòi ảnh sai. Đã dùng nó xác nhận bản nam sau khi dựng lại đã trọn vẹn
cả 30 khung.

**Lưu ý về tên**: bộ 208–210 vẽ màu **xanh dương, tay trần**, không phải trắng đỏ của Hokage. Tên
"Hokage nữ" đang đặt theo lời gọi, chưa khớp với hình.

### Đổi icon bộ Hokage và đổi tên hai danh hiệu

Icon áo và quần Hokage trước lấy đại một khung giữa chừng nên ra tay áo vắt chéo, nhìn khó hiểu.
Nay lấy đúng khung đứng như bên Mizukage: áo dùng ảnh thân ô 0 (đủ hai tay), quần dùng ảnh chân
ô 1 (đủ đôi dép).

Quan trọng: **cấp id ảnh mới 3122/3123 chứ không ghi đè lên 3117/3118**. Client nhớ ảnh theo id,
ghi đè cùng id thì máy nào đã tải rồi vẫn giữ ảnh cũ. Hai id mới nằm trên mức cao nhất của
`nj_image` (3115) nên không có hàng, đúng luật biểu tượng.

Đổi tên hai danh hiệu: 1131 "Fan Cứng" → **Mangekyo Sharigan**, 1152 "Đệ Tam" → **Rinegan**.

### Phân thân khác giới tính với chủ thể

Trước đây `CloneChar.load()` chép cứng ba thứ từ chủ: `gender`, `original_head` và `name`. Bảng
`clone_char` cũng không có cột nào để lưu khác đi. Nay thêm hai cột:

```sql
ALTER TABLE clone_char ADD COLUMN gender TINYINT NOT NULL DEFAULT -1,
                       ADD COLUMN head   SMALLINT NOT NULL DEFAULT -1;
```

Để `-1` thì theo chủ y như cũ, không phân thân nào đang chạy bị ảnh hưởng. Đặt số thật thì phân
thân dùng số đó. **Hai cột phải đi cùng nhau**: `head` chính là kiểu tóc chọn lúc tạo nhân vật và
nó theo giới tính (nam 2/23, nữ 26), đổi giới mà giữ tóc của chủ là ra mặt sai giới.

Lệnh `UPDATE` lúc lưu phân thân không đụng tới hai cột này nên chúng không bị ghi đè.

Phân thân của `itachi` (id `-10000001`, quy tắc là `-(10000000 + id chủ)`) nay để giới 0, tóc 26,
và đã gỡ sạch `equiped` lẫn `fashion` -- đồ cũ toàn đồ nam, giữ lại là vướng. Bản sao lưu ở
`build/backup/clone-itachi-truoc.sql`.

Lưu ý: `fashion` cũ của phân thân còn giữ id 1236 vốn là "Áo Kage nam" -- món đó đã xoá và id 1236
nay là "Áo Hokage", nên nếu không dọn thì phân thân sẽ mặc nhầm.

### Gói quy trình thêm trang phục thành một lệnh — `tools/them-trang-phuc.py`

Bộ thứ ba (Deidara) là đủ để thấy mọi bước lặp y hệt, nên gói lại:

```
python3 tools/them-trang-phuc.py Deidara 225 226 227 1 266
```

Tham số: tên bộ, ba tấm hiệu ứng (đầu, thân, chân), giới tính, mã cải trang đầu tiên. Kịch bản tự
đọc `effect_data`, chiếu qua `CharInfo`, cắt ảnh, nối `nj_image`/`nj_part`/`item` vào cuối bảng,
tạo biểu tượng, rồi in ra ba dòng cần dán vào `caiTrang()`.

**Biểu tượng nay để ở vùng 30000.** Trước đó chúng nằm ngay trên mức cao nhất của `nj_image`, nên
mỗi lần thêm bộ mới là ảnh mới lại nuốt mất id biểu tượng cũ và phải dời cả loạt. Vùng 30000 tách
hẳn -- icon cao nhất của bản gốc là 26916 nên không đụng ai, và `nj_image` có mọc thêm bao nhiêu
cũng không tới.

Bộ Deidara: vật phẩm 1246–1248, mảnh 314–316, ảnh 3116–3148, icon 30007–30009, mã 266–268.

**Lỗi vấp lúc viết kịch bản, đáng nhớ**: khoá của bảng tra là số nguyên mà tra bằng chuỗi, nên mọi
ô mảnh rơi hết vào nhánh dự phòng -- ô 0 thành rỗng, các ô còn lại dùng chung một ảnh. Nhìn ảnh
xuất ra là thấy ngay: mấy khung đứng mất đầu, còn lộn nhào lại ra tóc thay vì quả cầu. Không có
chế độ `--anh` thì lỗi này lọt thẳng vào game.

### Bộ Akatsuki bản nữ (tấm 228–230)

| | vật phẩm | mảnh | ảnh | icon | mã |
|---|---------|------|-----|------|-----|
| Nón Deidara Nữ | 1249 | 317 | 3149–3156 | 30010 | 269 |
| Áo Akatsuki Nữ | 1250 | 318 | 3157–3172 | 30011 | 270 |
| Quần Akatsuki Nữ | 1251 | 319 | 3173–3181 | 30012 | 271 |

Dựng bằng một lệnh `them-trang-phuc.py`, nhưng vấp một chỗ đáng ghi: **vài ô trong `effect_data`
vượt biên tấm ảnh** -- tấm 229 cao 57 mà có ô đòi `y+h = 58`. Bộ cắt ném lỗi và dừng cả mẻ. Nay
`CatO` tự bó ô về trong biên: thiếu một hàng pixel thì không ai thấy, còn dừng giữa chừng thì
hỏng việc.

Hai nhân vật thử `kagetest` và `hokagenu` đã nâng lên cấp 100 (`data.exp` = tổng 100 mốc đầu của
bảng kinh nghiệm, vì `NinjaUtils.getLevel` trừ dần từng mốc).

### Bộ Naruto Cửu Vĩ (tấm 245–247)

| | vật phẩm | mảnh | ảnh | icon | mã |
|---|---------|------|-----|------|-----|
| Nón Naruto Cửu Vĩ | 1252 | 320 | 3182–3189 | 30013 | 272 |
| Áo Naruto Cửu Vĩ | 1253 | 321 | 3190–3205 | 30014 | 273 |
| Quần Naruto Cửu Vĩ | 1254 | 322 | 3206–3214 | 30015 | 274 |

Một lệnh, không vấp gì. Tấm chân 247 có tới 26 ô (nhiều hơn hẳn các bộ trước) nhưng luật chọn lớp
"lấy lớp có dy gần 0 nhất" vẫn ra đúng chín ô quần.

Đã mặc sẵn cho `kagetest` để xem ngay.

### Bộ Obito Lục Đạo (tấm 219–221)

| | vật phẩm | mảnh | ảnh | icon | mã |
|---|---------|------|-----|------|-----|
| Nón Obito Lục Đạo | 1255 | 323 | 3215–3222 | 30016 | 275 |
| Áo Obito Lục Đạo | 1256 | 324 | 3223–3238 | 30017 | 276 |
| Quần Obito Lục Đạo | 1257 | 325 | 3239–3247 | 30018 | 277 |

Ba tấm này có số khung lẻ (32, 32, 120) chứ không phải 30/60 như các bộ trước, nhưng ba mươi khung
đầu vẫn khớp một-một với `CharInfo` nên không phải chỉnh gì.

### Sửa lỗi 67 vật phẩm hiện sai biểu tượng

`nj_image` kéo dài tới 3247 đã nuốt mất id biểu tượng của 67 vật phẩm dùng biểu tượng theo tệp
(3053–3247), nên client vẽ mảnh trang phục thay cho icon. Bản gốc lấy lại từ
`gpt/ZnsoC 2/ZnsoC/NsoC/res/assets/icon/<cỡ>/`, chép sang id mới **30100–30163**. Hai icon thiếu
một cỡ thì phóng lại từ cỡ khác.

`them-trang-phuc.py` nay tự dò và dời biểu tượng đụng dải ảnh mới trước khi chèn, nên lỗi này
không lặp lại.

### Nhập trang phục từ bản hồi ức — `tools/nhap-tu-hoiuc.py` và `tools/gom-art-hoiuc.py`

Bản hồi ức (`nso_hoiuc`, nguồn ở `gpt/ZnsoC 2/ZnsoC/NsoC`) có sẵn mảnh đã dựng, nên nhập nhanh hơn
cắt từ tấm hiệu ứng nhiều. Hai bản đánh số ảnh khác nhau, nhưng **rất nhiều ảnh trùng khít từng
byte** -- bộ nhập tự so, ảnh nào giống thì giữ id, ảnh nào chỉ bên kia có thì chép và đánh số lại.

Kho ảnh của hồi ức: `res/assets/icon/<cỡ>/Small<id>.png` -- tên là "icon" nhưng chứa tất cả ảnh
nhỏ, cả biểu tượng lẫn mảnh ghép. Cùng quy ước khung với bên mình (loại 0→8, 1→18, 2→10, 3→2), đọc
từ `src/cache/Part.java` của nó.

**Naruto Hiền Nhân** (mảnh hồi ức 233/234/235): vật phẩm 1258–1260, mảnh 326–328, mã 278–280.
Cả 33 ảnh bên mình đã có sẵn giống hệt -- art vốn nằm trong dữ liệu từ đầu, chỉ thiếu mảnh dùng nó.

**Mặt nạ Pain** (mảnh hồi ức 224): vật phẩm 1261, mảnh 329, mã 281. Bảy ảnh đều đã có sẵn. Món này
để cả `part` lẫn `fashion` nên dùng được ở cả ô trang bị 1 lẫn 2.

`gom-art-hoiuc.py` chép art về `art/nhap/<tên>/` kèm bảng xem và ghi chú tình trạng, không đụng cơ
sở dữ liệu -- dùng để soi trước những bộ art còn dở.

**Madara** (`art/nhap/madara/`, ảnh 15000–15028) chưa nhập được: chỉ có **duy nhất cỡ 1**, và thiếu
hẳn 15019 với 15022. Đó cũng là lý do hồi ức chỉ có mảnh đầu (357) và thân (358), không có mảnh
chân. Muốn dùng thì phải phóng thêm ba cỡ và vá hai ảnh khuyết.

### Dọn đường băng cho nj_image

`nj_image` bắt buộc liền mạch từ 1 nên chỉ nối được vào cuối, mà biểu tượng vật phẩm theo tệp lại
nằm rải ngay phía trên. Sau bốn bộ trang phục, mức cao nhất chạm 3247 và **id kế tiếp 3248 đã có
món dùng** -- hết sạch chỗ, thêm một ảnh nữa là hỏng biểu tượng.

Đã dời cả khối **259 biểu tượng ở 3248–4087 lên 27000–27258**. Vùng 26917–29999 vốn trống hoàn
toàn (chỉ 26916 có người dùng), và 27258 vẫn dưới **trần 32767** -- `ItemManager.setData()` ghi
biểu tượng bằng `writeShort` nên đó là trần cứng, không được vượt.

Tệp chép từ kho của mình (còn nguyên), kho hồi ức chỉ dự phòng. Kiểm lại: 259/259 biểu tượng đủ
bốn cỡ, không món nào còn icon trong 3248..6500.

**Kết quả: `nj_image` chạy được tới 6500** -- hơn 3.250 id trống, đủ khoảng một trăm bộ trang phục
nữa mà không phải dời vặt lần nào.

Sao lưu trước khi dọn: `build/backup/item-truoc-don-icon.sql`.

### Vì sao không gộp ảnh mảnh vào tấm lớn

Client nạp đúng năm tấm gộp nhúng trong jar: `x<cỡ>/img/Big0.png`..`Big4.png`, đọc từ bytecode
`SmallImage`. Số đầu của mỗi hàng `nj_image` chính là chọn một trong năm tấm đó; toạ độ vượt ra
ngoài tấm thì client quay sang xin tệp `Small<id>.png` riêng.

Không gộp được vì: mỗi tấm chỉ **255×255 và đã kín 93–95%**, cả năm tấm cộng lại chứa hơn 1.200
ảnh trong khi **2.030 ảnh của chính bản gốc đã phải xin tệp riêng** -- bản gốc cũng tràn từ lâu.
Nặng hơn nữa: năm tấm nằm trong client jar, sửa là phải phát hành lại client cho mọi người mỗi
lần thêm một bộ đồ. Cách hiện tại chỉ tốn 4,7 MB đĩa và một lần xin mỗi ảnh, xin xong client nhớ.

### Bộ Madara (ảnh 15000–15028 của bản hồi ức)

| | vật phẩm | mảnh | icon | mã |
|---|---------|------|------|-----|
| Nón Madara | 1262 | 330 | 30023 | 282 |
| Áo Madara | 1263 | 331 | 30024 | 283 |
| Quần Madara | 1264 | 332 | 30025 | 284 |

Ảnh mới 3248–3274 -- đợt đầu tiên dùng tới đường băng vừa dọn, và không đụng biểu tượng nào.

Hai chỗ phải xử lý vì art bên kia dở dang:

- **Chỉ có cỡ 1.** Đã phóng lên cỡ 2/3/4 bằng cách nhân điểm ảnh nguyên bội (`PhongAnh.java`),
  không nội suy -- nội suy sẽ làm nhoè viền pixel.
- **Thiếu hẳn 15019 và 15022**, nên hồi ức không có mảnh chân. Mảnh chân ở đây dựng tay từ
  15020–15028, chỗ khuyết 15022 lấy tạm khung liền trước cho khỏi hụt nhịp.

Mảnh đầu và thân lấy nguyên từ mảnh 357, 358 của hồi ức, chỉ đánh số lại ảnh.

**Sửa chân Madara**: đặt bừa `dx = dy = 0` nên quần lệch hẳn khỏi trục thân. Luật đúng đọc ra từ
mảnh chân chuẩn (Quần Hokage, mảnh 308): **canh theo mép trên, `dy` gần như luôn là −1**, còn `dx`
thì tuỳ lề trái của từng ảnh -- art Madara cắt lề khác nên phải tính lại `dx` theo tâm ngang của
ô tương ứng bên mảnh chuẩn.

**Sửa đầu Madara**: bản gốc để `dy = -4`, mép dưới 26 trong khi mảnh đầu chuẩn chỉ 20–21 nên đầu
tụt thấp. Nâng hẳn lên 20 thì lại hở cổ vì ảnh Madara cao 30px (tóc xoè cả hai bên), chốt ở
`dy = -7`. Khung chạy bản gốc dùng chung ảnh với khung đứng; nay cho nó dùng ảnh nghiêng 15001
(3249) để lúc di chuyển đầu có hướng riêng.

### Chế độ đo của tool — `CanManh ... --do <đầu> <thân> <chân> [khung]`

Nhìn ảnh chỉ thấy "sai", không nói được sai mấy pixel. Chế độ này in khung bao thật của ba mảnh
theo toạ độ gốc nhân vật, đúng công thức client dùng:

```
x = CharInfo[cf][ô][1] + dx        y = -CharInfo[cf][ô][2] + dy - 10
```

So với một bộ đã chuẩn là ra ngay lượng cần dịch. Ca Madara: đo xong thấy cả ba mảnh **lệch trái
2px và tụt 2–3px** -- đáy chân −7 trong khi chuẩn là −10, tức bàn chân lún xuống dưới vạch đất.
Dịch đều cả mảnh (đầu +2/−3, thân +2/−2, chân +1/−3) là khớp, và giữ nguyên nhịp giữa các khung
vốn có của art gốc.

Con số chuẩn để đối chiếu về sau, ở khung đứng: **đáy đầu −22, đáy thân −12, đáy chân −10, tâm ≈ 0**.

**Madara chốt lại**: đáy đầu −20, đáy thân −15, đáy chân −10, tâm ≈ 0. Chân theo đúng bộ chuẩn;
thân nâng thêm 3 và đầu hạ thêm 2 so với chuẩn, vì ảnh đầu cao 30px (chuẩn 22px) làm cằm nằm cao
hơn vai — canh đúng số chuẩn thì hở cổ.

### Làm sắc art Madara — `tools/LamSacAnh.java`

Bộ này nhìn nhoè và cánh tay lẫn vào thân. Đo ra thì rõ ngay, art nó **không cùng lối vẽ** với game:

| | điểm mờ một phần | số màu |
|---|---|---|
| Hokage (art gốc của game) | **0%** | 12–27 |
| Madara (lấy từ hồi ức) | **16–39%** | 93–131 |

Art game là pixel sắc cạnh; art Madara đã bị thu nhỏ bằng nội suy nên viền có quầng mờ và bảng màu
phình lên hàng trăm màu. Ở cỡ nhỏ thì các mảng màu gần nhau dính vào nhau.

Bộ làm sạch chạy hai bước: cắt phăng độ trong suốt (đục hơn nửa thành đặc, nhạt hơn thành trong),
rồi gom bảng màu về 24 màu.

**Vấp một lần đáng nhớ**: lần đầu chọn bảng màu theo tần suất, nghe hợp lý mà sai nặng -- tóc xám
chiếm diện tích lớn giành hết 24 suất, **khuôn mặt biến mất sạch**. Đổi sang phép chia hộp (cắt hộp
màu theo kênh trải rộng nhất) thì màu hiếm vẫn giữ được phần, mặt trở lại.

Ảnh sau khi làm sạch phải cấp **id mới** (3275–3301) chứ không ghi đè lên 3248–3274: client nhớ ảnh
theo id, ghi đè cùng id thì máy nào đã tải rồi vẫn hiện bản nhoè. Biểu tượng ba món cũng đổi theo.

## Phát hành bản 2.4 — ngày 21/08

Client 2.3 **không thấy được trang phục nào** vì phần vá thời trang làm sau khi phát hành 2.3. Bản
2.4 mang nó theo, nên đây là bản bắt buộc cập nhật với ai muốn xem trang phục.

Bảy bộ đang có, mỗi bộ ba món rời, trộn giữa các bộ được:

| bộ | giới | mã cải trang |
|----|------|--------------|
| Hokage | nam | 260–262 |
| Mizukage | nữ | 263–265 |
| Deidara / Akatsuki Nam | nam | 266–268 |
| Deidara Nữ / Akatsuki Nữ | nữ | 269–271 |
| Naruto Cửu Vĩ | nam | 272–274 |
| Obito Lục Đạo | nam | 275–277 |
| Naruto Hiền Nhân | nam | 278–280 |
| Mặt nạ Pain | cả hai | 281 |

Lời chào trong game (`options.thongbaogame`) đã cập nhật, có nhắc phải tải lại client.

### Mặt nạ Madara — vật phẩm 1262

Bộ Madara đầy đủ bị bỏ vì art thân là mấy tấm giáp tay rời, không có thân áo, ở cỡ 20 pixel đọc
ra thành cục. Chỉ giữ lại **cái đầu**, làm thành mặt nạ rời để mặc cùng áo choàng Akatsuki (mã
267/268) -- vừa hợp truyện vừa dùng art đã hoàn chỉnh.

Mảnh 330, mã cải trang 285, dùng được ở cả ô trang bị 1 lẫn 2.

Ảnh đầu do người dùng vẽ lại: 25×24, viền dứt khoát, 26 màu. **Cả ba khung dùng chung một ảnh
nhìn thẳng** -- ảnh nghiêng vẽ ra không đạt, mà mảnh Madara gốc bên hồi ức cũng để đứng và chạy
chung một ảnh, nên không phải sáng tạo gì.

Đầu đặt ở đáy **−23**, cao hơn chuẩn −22 một pixel. Lý do: client vẽ thân sau đầu và áo choàng
chồm lên 6 pixel; ảnh Madara để mặt sát mép dưới nên canh đúng chuẩn thì áo ăn mất mặt. Nếu sau
này đẩy mặt lên trong ảnh và chừa chỗ dưới cằm cho tóc thì hạ về −22 được.

### Mặt nạ ANBU — vật phẩm 1263

Art người dùng vẽ mới, để ở `art/mat-na-anbu-itachi/` kèm nguồn `ve-mat-na.java` (lưới ký tự, mỗi
ký tự một pixel). Hai trạng thái thẳng 19×23 và nghiêng 17×23, đủ bốn cỡ, **0% điểm mờ, 6 màu**.

Đây là lần đầu một bộ art **lên đúng ngay lần đầu, không phải nắn** -- vì nó theo đúng chuẩn của
game nên chỉ cần đặt số chuẩn là khít: đáy −22, tâm 0. Đối lại là bộ Madara phải qua bảy vòng
chỉnh vì art mịn viền, sai tỉ lệ và mặt đặt sát mép dưới.

Mảnh 331, mã cải trang 286, dùng được ở cả hai ô trang bị.

### Mặt nạ Itachi — vật phẩm 1263

Art người dùng vẽ, `art/mat-itachi/` (nguồn `VeMatItachi.java`, lưới ký tự). Thẳng 21x24, chạy
22x21, đủ bốn cỡ, 0% điểm mờ, 10-11 màu.

Ô mảnh 331: 0 nhìn thẳng · 1-2 chạy · 3 nhìn thẳng · **4-7 dùng ảnh chạy** thay vì để mặc định,
nên lộn nhào cũng giữ mặt nạ -- đây là chỗ bộ Madara còn để lọt.

Số canh tính thẳng từ CharInfo chứ không nắn mắt: dx = -trungBình(o[1]) - rộng/2, dy = trungBình(o[2])
- 12 - cao, tức tâm 0 và đáy -22. Ra đúng ngay, chỉ khung đứng lệch 1px do làm tròn nên hạ dy thêm 1.

Lưu ý dùng CanManh: phải có driver mysql trên classpath, tức
`java -cp "build/tools:<fatjar>" CanManh work/server/NSO_KEM --do <đầu> <thân> <chân> [khung]`.
Thiếu jar thì nó **im lặng in ra bảng rỗng** chứ không báo lỗi.

### Chỉ số và yêu cầu cấp cho 23 món trang phục trang bị 2

**Gỡ được ràng buộc cũ: `Item.java` nay dịch lại được, không cần Lombok.** Cả file chỉ dùng
`@Getter @Setter` ở mức lớp, và trong 117 phương thức của bản trong jar thì 38 cái là accessor do
Lombok sinh. Đọc danh sách ấy ra bằng `javap` rồi viết thẳng 38 accessor vào nguồn, bỏ hai dòng
`import lombok.*` và hai chú thích. Ba trường boolean vốn đã tên `isGiahan/isLock/isNew` là bẫy --
Lombok sinh `isLock()`/`setLock(boolean)` chứ không phải `isIsLock()`, bộ sinh tự động bỏ sót, phải
viết tay. Dịch lại rồi so `javap` hai bên: **giống hệt, không thiếu không thừa một phương thức nào**.

Điều này quan trọng hơn cái việc trước mắt: `Item.java` là một trong mấy file bị liệt "chỉ vá được
bằng bytecode". Vá bytecode ở đây bất khả thi thật -- lớp máy chủ là major 60 (Java 16) mà ASM có
sẵn chỉ là 3.1 (đọc tới major 50), trong fat jar không có javassist, và không được nối mạng để tải
Lombok. Cách này giải quyết dứt điểm mà không cần gì thêm.

Chỉ số đặt trong `initOptionTrangPhucMoi()`, gọi ngay đầu `initOption()`. **Chép đúng bộ mã chỉ số
mà máy chủ vốn dành cho trang phục trang bị 2** (bộ Fairies/Sixgirl/Buffalo dùng đúng ngần ấy mã),
nên client hiển thị y như đồ có sẵn, không phải đụng gì tới phần vẽ bảng chỉ số:

| | mã | giá trị |
|---|---|---|
| nón, mặt nạ | 82 / 87 / 69 / 58 | HP 2000 · tấn công 2000 · chí mạng 12 · tiềm năng +25% |
| áo, quần | 125 / 117 / 94 / 127 / 130 / 131 | HP 5000 · MP 5000 · tấn công +12% · ba kháng hệ 12% |

Bỏ hạn ba ngày và mã 136 quay số của bộ cũ. Yêu cầu cấp 100 cho cả 23 món.

`initOption()` chỉ chạy lúc tạo món, nên món đã phát trước đó vẫn `"options": []` --
`tools/nap-chi-so-trang-phuc.py` quét lại bag/box/fashion/equiped của mọi người chơi. **Bảng chỉ số
trong tool chép lại từ nguồn máy chủ, sửa một bên phải sửa bên kia.** Thứ tự bắt buộc vẫn như cũ:
sửa nguồn -> khởi động lại -> mới dọn kho.

### Thưởng theo bộ cho trang phục trang bị 2

Mặc 2 mảnh được một mớ chỉ số, đủ 3 mảnh được thêm mớ nữa. Bảy bộ, mỗi bộ một hướng riêng hợp
với nhân vật (`com/nsoz/ability/BoTrangPhuc.java`).

**Chỗ móc là `AbilityFromEquip.setAbility()`** -- nơi máy chủ dồn mọi chỉ số từ đồ vào mảng
`owner.options`. Đây mới là điểm mấu chốt của cả tính năng: máy chủ *có* tự tính chỉ số nhân vật
từ đồ, nên cộng thẳng vào mảng ấy là thưởng bộ đi đúng đường của chỉ số thường -- vào sát thương,
vào HP tối đa, vào kháng -- mà không phải đụng gì tới client.

Đã cân nhắc rồi loại hai hướng khác:

- *Nhét chỉ số vào chính món đồ khi đủ bộ*: hỏng, vì món đồ lưu xuống cơ sở dữ liệu. Cởi ra một
  mảnh mà chỉ số cộng thêm vẫn nằm đó. Mảng `owner.options` thì dựng lại từ đầu mỗi lần tính nên
  cởi ra là mất, không cần dọn gì.
- *Để client tự tính*: không được. `resetPoint()` chỉ gửi CMD rỗng bảo client tính lại từ bản sao
  của chính nó, không kèm dữ liệu -- và không có đường nào đẩy lại chỉ số một món đang mặc.

Mã chỉ số chỉ lấy trong nhóm máy chủ **thật sự có đọc** (dò `options[<số>]` trong mã nguồn: cả 16
mã đang dùng đều có chỗ đọc), và tránh nhóm type 3..7 vì nhóm ấy còn đòi bậc nâng cấp mới tính.

Đếm mảnh phải soi **ba** chỗ: ô trang bị 1, ô trang bị 2, và ô mặt nạ riêng `Char.mask` --
FashionFromEquip cũng đọc cả ba. Bỏ sót chỗ nào thì bộ đủ ba mảnh lại tính thành hai.

Mô tả bộ ghi thẳng vào cột `description` của 21 món, sinh từ chính bảng trong `BoTrangPhuc` (chạy
`java com.nsoz.ability.BoTrangPhuc`) nên không lệch với chỉ số thật. Một dòng, không xuống dòng:
cả bảng vật phẩm không có mô tả nào chứa ký tự xuống dòng nên không rõ client dựng lại thế nào.

### Mở cho người cùng wifi chơi chung

Hoá ra phần máy chủ chẳng phải làm gì: cổng trò chơi vốn đã nghe `*:14444` (mọi giao diện), tường
lửa macOS đang tắt. Chỉ thiếu hai mắt xích phía phát hành.

**Client**: địa chỉ máy chủ nhúng cứng trong jar, nhưng `mod.sh` vốn đã nhận tham số ip --
`./mod.sh build 192.168.1.129` là ra bản LAN. Chép vào `dist/share/NSO-mobile-lan.jar`, để cạnh
bản Tailscale `NSO-mobile-ts.jar`; hai bản khác nhau đúng một chuỗi địa chỉ.

**Chỗ tải**: `run-share.sh` trước chỉ gắn vào IP Tailscale nên người cùng wifi không với tới. Thêm
`SHARE_MODE=lan` (cổng 8081, chạy song song bản ts ở 8080 dưới tên pm2 `nso-share-lan`).

Giữ nguyên nguyên tắc cũ của tệp đó: **gắn vào một địa chỉ cụ thể, không bao giờ 0.0.0.0**. Gắn
0.0.0.0 nghĩa là hễ máy cắm vào mạng nào -- wifi quán, wifi khách sạn -- là cả mạng đó thấy thư
mục mà mình không biết. Gắn đúng IP của giao diện đang dùng thì đổi mạng là cổng chết theo.

Bẫy nhỏ: trên máy này **en0 là dây, en1 mới là wifi** -- đoán `en0 = wifi` là sai. Script lấy giao
diện từ tuyến mặc định (`route -n get default`) chứ không ghi cứng tên.

Chưa làm: IP 192.168.1.129 do DHCP cấp nên đổi được; muốn khỏi phải dựng lại client mỗi lần thì
đặt IP tĩnh trong trang quản lý bộ định tuyến.

### Gỡ chỗ đè id ảnh -- NPC Tashino và 12 mảnh khác

Người dùng báo NPC 39 Tashino hiện ra thành đống hình chồng chéo, bảo xoá đi. **Không xoá** -- kiểm
ra là chính ta làm hỏng: `them-trang-phuc.py` cấp id ảnh từ `MAX(nj_image.id)+1` rồi `CatO` ghi đè
lên 115 tấm ảnh đang có chủ. Chi tiết nguyên nhân ghi trong `tasks/lessons.md`.

Chỗ hỏng: NPC 39 Tashino (mảnh 276-278), vật phẩm 711 Mặt nạ Jirai (223), vật phẩm 786 Sumimura
(270), và các mảnh 271-275, 279-281.

Sửa bằng `tools/go-dung-anh.py`: dời ảnh trang phục sang id 3255-3369, trả 115 tệp gốc từ kho v4 về
đúng id cũ, trỏ lại 14 mảnh trang phục. Dời phía trang phục chứ không dời phía cũ, vì id cũ là thứ
client gốc và các bảng khác vẫn trỏ tới. Dựng lại bằng `CanManh --anh` thì cả NPC lẫn bộ Akatsuki
đều đúng. Ba bảng vẫn liền mạch, tự kiểm 331 mảnh 0 lệch.

Vá gốc: `tools/idanh.py` -- `id_moi` giữ đúng luật liền mạch và cảnh báo khi có tệp mang id lớn
hơn hàng cuối; `don_duong` dọn dải id trước khi ghi. Hai bộ thêm trang phục đều gọi nó, và gọi
**trước** lúc cắt ảnh.

Còn treo: 428 tệp ảnh id > 3369 chưa rõ ai dùng (không nằm trong nj_part, không phải biểu tượng
vật phẩm). `don_duong` sẽ chép chúng đi và báo ra chứ không tự trỏ lại được -- nếu sau này đụng
phải thì phải dò xem bảng nào giữ chúng (nhiều khả năng là hiệu ứng).

### Bản 2.2 trở lên không mở được trên Windows -- phát hành 2.5

Bạn bè dùng Windows không mở được bản 2.4, chỉ chạy được tới 2.1. Nguyên nhân: `mod.sh` biên dịch
`mod-src` không kèm `--release` nên lớp ra major 61 (Java 17), trong khi JRE đi kèm Micro_AngelChip
là Java 8. Chi tiết và cách lần ra ghi trong `tasks/lessons.md`.

Sửa: thêm `--release 7` vào lệnh javac trong `mod.sh`. Kiểm lại: bản mới có 183 lớp major 50 và 4
lớp major 51, không còn lớp nào trên 52. Đã phát hành **NSO-2.5.jar** và dựng lại
`NSO-mobile-lan.jar` bằng cùng bộ dựng.

Nên kiểm mỗi lần phát hành: `lớp cao nhất phải <= 52`. Chưa gắn vào `mod.sh promote` -- việc còn treo.

### Đường lấy log từ máy bạn bè trên Windows

Trước đây bạn bè báo lỗi thì chỉ có câu "không vào được", không có gì để đọc. Nay có hai đầu:

**Bên máy chủ.** Bốn nhánh `session.disconnect()` trong `User.selectChar` xưa nay **im lặng tuyệt
đối** -- đá người chơi ra mà không ghi một dòng. Đó là lý do log sạch trơn trong khi người chơi bị
văng. Đã chèn nhật ký vào cả bốn, cộng một dòng mốc "nạp xong, đang dựng chỉ số" để biết đi tới
đâu thì chết. Dùng `System.out.println` chứ không `Log.debug`, vì `Log.debug` còn bị `isShowLog()`
chặn và đi qua log4j, không chắc ra tới `build/server.log`.

`User.java` dịch lại được bình thường, không dính Lombok.

**Bên Windows.** `CHAY.bat` ghi mọi thứ ra `log.txt` rồi **tự gửi về** `tools/nhan-log.py` qua
Tailscale (pm2 `nso-log`, cổng 8090), lưu vào `build/log-ban-be/<tên>-<thời điểm>.txt`. Bạn bè chỉ
việc sửa một dòng `set "TEN=..."` trong CHAY.bat, không phải đi tìm file gửi tay. Dùng `curl.exe`
có sẵn từ Windows 10, hụt thì lùi về PowerShell.

Chỗ nhận log gắn vào **IP Tailscale**, không phải 0.0.0.0, và **chỉ nhận POST** -- máy này còn giữ
cơ sở dữ liệu với mã nguồn, không mở thêm đường nào đọc đĩa. Tên tệp lọc còn chữ và số (chặn kiểu
`../../`), mỗi lần tối đa 2 MB.

Nhớ gỡ mấy dòng `[vao-game]` khi hết cần -- chúng in ra mỗi lần có người vào game.

### Bảng điều khiển web: thêm phần nhiệm vụ

Cổng 8765 nay có tab **Nhiệm vụ** bên cạnh Tổng quan / Trang bị / Hành trang / Rương / Kỹ năng.
Hiện nhiệm vụ số mấy, tên, bước mấy trên mấy, việc phải làm, tiến độ; và ba nút: bỏ qua một bước,
bỏ qua cả nhiệm vụ, nhảy tới nhiệm vụ N.

Phần lõi **không viết lại** -- dùng lại đúng cách `TaskAdmin` (cửa sổ Swing) đã làm: đi qua
`Char.finishTask(true)` bằng reflection, tức đúng đường mà vật phẩm "Lệnh bài hoàn thành" đi, nên
phần thưởng, vật phẩm mở đầu và gói tin gửi xuống máy khách giống hệt lúc chơi thật. Người chơi
thấy nhiệm vụ đổi ngay, không phải đăng nhập lại.

Một chỗ **ngược với mọi thao tác khác** trong bảng này: sửa tiềm năng/kỹ năng thì offline mới tiện
(ghi thẳng cơ sở dữ liệu), còn bỏ qua nhiệm vụ thì **bắt buộc người chơi đang online** -- vì đường
phát thưởng chỉ có trong bộ nhớ máy chủ. Sửa thẳng cột `taskId` thì nhiệm vụ nhảy số nhưng người
chơi mất phần thưởng và mất luôn vật phẩm mở đầu, kẹt nặng hơn lúc đầu. Giao diện nói rõ lý do
thay vì chỉ làm mờ nút.

Nhân vật offline vẫn **xem** được đang ở nhiệm vụ nào (đọc cột `taskId` + `task`).

### Bảng điều khiển web: quản lý hành trang

Tab Hành trang và Rương nay có:

- **Ảnh biểu tượng** từng món, đọc thẳng kho ảnh máy chủ qua đường `/anh?icon=<id>&co=<1..4>`.
  Đường này **chỉ nhận số** -- id ghép vào đường dẫn tệp nên để lọt chuỗi tự do là mở đường đọc mọi
  tệp trên đĩa. Không có tệp thì trả 404 để phía web tự vẽ ô rỗng, khỏi tốn lượt tải.
- **Mô tả món** (cột `description` của bảng item), thêm vào `itemJson` cùng với `icon`.
- **Xoá cả ô** (kể cả ô chồng nhiều cái) và **xoá 1** với ô chồng.
- **Ô đánh dấu** từng món + thanh "Chọn hết / Xoá N ô đã chọn".

**Xoá được cả món khoá và đồ đã nâng cấp, cố ý như vậy.** Khoá với độ nâng cấp là để chặn người
chơi lỡ tay bán mất đồ quý; còn đây là chỗ của người quản trị, và lý do hay gặp nhất để mở bảng
này ra chính là gỡ một món đang làm kẹt nhân vật -- món khoá lại càng hay là thủ phạm. Chặn ở đây
thì công cụ vô dụng đúng lúc cần nhất.

Hai đường như mọi thao tác khác: online thì đi qua `Char.removeItem(index, qty, true)` để máy khách
thấy ngay không cần đăng nhập lại; offline thì sửa thẳng cột JSON. Ô nhận dạng bằng khoá `index`
trong chính hàng đó **chứ không phải vị trí trong mảng** -- mảng lưu xuống không có ô trống, ô số 5
có thể nằm ở phần tử thứ ba.

Trang bị đang mặc thì **không** cho xoá, phải cởi ra đã: xoá thẳng khỏi ô mặc sẽ để lại chỉ số
đang cộng mà không còn món.

Dấu chọn xoá sạch khi đổi tab hoặc đổi nhân vật -- số ô là của kho này, mang sang kho khác là xoá
nhầm món.

### Bảng điều khiển web: đợt sửa giao diện

- **Thêm món**: nút "+ Thêm món" mở hộp chọn, tìm theo tên hoặc mã, nhập số lượng.
  `/api/items` trả danh mục 1263 món (id, tên, loại, cấp, biểu tượng), tải một lần rồi giữ lại.
  Hộp chỉ vẽ **200 dòng đầu** sau khi lọc và báo còn bao nhiêu -- hơn nghìn dòng vẽ hết là giật.
  Phía máy chủ đi qua `ItemFactory.newItem` + `Char.addItemToBag`, nên chỉ số món do chính máy chủ
  sinh ra và túi được xếp đúng chỗ trống. **Đòi người chơi đang online**, cùng lý do với bỏ qua
  nhiệm vụ.
- **Kỹ năng**: thêm ảnh, rê chuột hiện mô tả, tách bảng riêng cho **kỹ năng phân thân**, và
  **chặn nút khi đã kịch trần** (`point >= maxPoint`) hoặc đã chạm đáy (`point <= 1`).
- **Kỹ năng phân thân nay nâng được.** Trước bị chặn cứng với câu "không nâng được". Kiểm lại thì
  bảng `skill` có đủ 11 bản (cấp 0-10) cho cả sáu mã 67-72, y hệt kỹ năng thường. Mỗi phái một mã
  riêng, và phép tra theo (phái, mã, cấp) tự loại mã sai phái nên không cần chặn thêm.
- **Tab trang bị**: thêm bảng chỉ số như phần xem thông tin trong game (HP, MP, sát thương, giảm
  sát thương, chính xác, né, chí mạng, ba kháng). Đọc thẳng các trường đã tính sẵn trên `Char`
  **chứ không tự cộng lại từ đồ** -- công thức thật ở `AbilityFromEquip` còn nhân theo tiềm năng,
  cộng phần trăm, cộng chỉ số hỗ trợ của kỹ năng; chép lại là chép sai sớm muộn. Vì thế chỉ hiện
  với người đang online, offline thì nói rõ lý do.
- **Thanh xoá ghim cố định góc dưới bên phải** thay vì nằm đầu danh sách: túi có sáu bảy chục ô,
  chọn ở cuối trang mà nút xoá nằm tít trên đầu thì phải cuộn ngược lên.

Bẫy đã dính: `data-them` vốn đã có chủ (cộng điểm tiềm năng/kỹ năng), đặt trùng cho nút thêm món
thì nó rơi vào nhánh cũ. Đổi thành `data-themmon`.

Chưa thử được đường thêm món chạy thật, vì lúc làm chỉ có một người online và không tiện đụng vào
túi của người đang chơi.

### Bảng điều khiển web: kỹ năng của phân thân, và dọn giao diện

**Phân biệt hai thứ dễ lẫn.** "Kỹ năng phân thân" có hai nghĩa khác hẳn nhau:

- sáu mã **67-72** trong bộ kỹ năng của người chơi -- đó là kỹ năng *triệu hồi* phân thân, nằm bên
  chủ. Đợt trước tôi mở khoá đúng cái này.
- **phân thân là một nhân vật riêng**: hàng riêng trong bảng `clone_char`, phái riêng, giới tính
  riêng, tiềm năng riêng, điểm kỹ năng riêng, bộ kỹ năng riêng. Mã hàng suy ra từ mã người chơi:
  `-(10000000 + id)`, xem `CloneChar` dòng 48. Đây mới là thứ người dùng hỏi.

Nay quản lý được cả hai, hai bảng riêng trong tab Kỹ năng. Đường ghi: chủ online thì sửa đối tượng
trong bộ nhớ rồi gọi `saveData()` của **chủ** -- hàm ấy lưu luôn phân thân, xem cuối `Char.saveData`;
chủ offline thì sửa thẳng hàng `clone_char`, lúc đó không có bản nào trong bộ nhớ để bị ghi đè.

**Tìm kiếm bỏ dấu.** Trước phải gõ đúng dấu mới ra, gần như không dùng được. Nay `khongDau()` chuẩn
hoá NFD rồi xoá dấu ở cả hai vế (đ/Đ phải thay tay vì không phải nguyên âm có dấu). Áp cho cả ô tìm
vật phẩm lẫn ô tìm nhân vật.

**Ô nhập.** Trước chỉ `#tim` và `.oso` có kiểu, nên ô nào mới thêm cũng rơi về mặc định trình duyệt
-- nền trắng, góc vuông, viền xanh khi bấm, chửi nhau với nền tối. Nay đặt kiểu **chung** cho
`input[type=text|search|number]` nên không tái diễn nữa.

**Bỏ tab Rương** khỏi giao diện. Phía máy chủ vẫn nhận kho `box` nếu sau này cần bật lại.

Lưu ý vận hành: `charadmin.html` nằm trong jar và JVM giữ bản đã nạp, nên **sửa HTML vẫn phải khởi
động lại máy chủ** mới thấy. Đã mất một vòng vì tưởng không cần.

### Tiềm năng phân thân, và lý do ô nhập vẫn xấu sau lần sửa đầu

Tab Kỹ năng nay có đủ ba bảng cho phân thân: **tiềm năng**, kỹ năng của phân thân, và kỹ năng
triệu hồi bên chủ. Tiềm năng dùng lại đúng bố cục của nhân vật chính (−10/−1/+1/+10/Dồn hết), cho
cả điểm âm để rút bớt ra.

**Vì sao lần sửa ô nhập đầu tiên không ăn.** Tôi đặt `input[type=text],input[type=search],
input[type=number]{...}`, tưởng thế là hết. Nhưng ô tìm nhân vật viết là `<input id="tim">` --
**không có thuộc tính `type`**. Trình duyệt coi nó là text khi chạy, nhưng bộ chọn
`input[type=text]` thì không khớp, vì nó soi thuộc tính có mặt trong HTML chứ không soi kiểu lúc
chạy. Nên ô ấy vẫn trắng bốp giữa nền tối.

Sửa bằng cách nới thành `input:not([type=checkbox]):not([type=radio]):not([type=button])` -- bắt
theo lối loại trừ nên ô nào quên ghi `type` cũng dính. Bài học: với `input`, **liệt kê kiểu muốn
bắt là sẽ sót**; loại trừ kiểu không muốn bắt mới chắc.

### Tắt bảo trì tự động, và gộp vòng triển khai vào một lệnh

**Bảo trì tự động đã tắt.** Nó hẹn 0h00 mỗi ngày: loa báo trước 5 phút, rồi `Server.stop()` lưu
bang hội / gian hàng / thiên địa và đóng cổng. Vấn đề nằm ở phần *bật lại*: đoạn đó gọi
`taskkill /F /IM cmd.exe` rồi mở lại `run.bat` -- **chỉ chạy được trên Windows**, đầu file
`AutoMaintenance` ghi rõ `// khi chạy windows`. Trên macOS `taskkill` không có, ném IOException
ngay dòng đầu, nên `System.exit(1)` đứng sau trong cùng khối `try` không bao giờ chạy.

Kết cục: đúng 0h00 máy chủ đóng cổng, đá hết người ra rồi **nằm im** -- JVM không thoát nên pm2
không thấy tiến trình chết mà bật lại. Mấy hôm nay chưa lộ vì tôi khởi động lại liên tục, bộ hẹn
giờ đếm lại từ đầu mỗi lần nên chưa lần nào chạm 0h00.

Muốn bật lại thì sửa `AutoMaintenance` cho gọi thẳng `System.exit(0)`; pm2 có `autorestart` nên sẽ
tự dựng lại sạch sẽ.

**`tools/nap.sh`** gộp cả vòng: tìm file .java mới hơn jar -> dịch -> chép charadmin.html -> nhét
vào jar -> khởi động lại -> chờ báo xong -> đếm lỗi trong log.

Bẫy đã dính khi viết: **chờ theo cổng 14444 là sai**. Cổng vẫn mở một lúc sau `pm2 restart` vì
tiến trình cũ chưa chết hẳn, nên script báo "xong sau 2s" trong khi máy chủ mới còn chưa nạp bản
đồ. Đổi sang chờ dòng "Bảng điều khiển nhân vật" trong log -- log đã bị cắt trắng trước khi khởi
động nên dòng nào hiện ra cũng là của lần chạy mới.

### Ngọc Lục Đạo -- áo choàng trang bị 2, và bộ vá thứ 11 cho client

Bộ dò cho kết quả tốt hơn dự đoán: **không phải đụng tới kênh `ID_PP`**. Client 148 **có sẵn** phần
xử lý gói áo choàng (lệnh con −56) -- nó đọc `id, hp, maxHP, coat` rồi ghi `coat` vào trường **`w`
(short)** của lớp nhân vật. Lần dò đầu tìm hụt vì lệnh con nằm trong **bảng nhảy rộng** chứ không
so bằng `bipush`; phải lần theo đúng nhánh của khoá −56 mới thấy (`tools/DoLenh.java`).

Client tra art áo choàng bằng **bảng ghi cứng**: một hàm trả về `int[]`, đọc `w` rồi trả bốn mã ảnh
-- ví dụ `w == 420` (Faiyaa yoroi) trả `{1635, 1636, 1637, 1636}`, tức ba khung bay lặp kiểu đi-về.
Mã nào không có trong bảng thì **không vẽ gì**, nên món mới thêm sẽ vô hình. Đó là chỗ phải vá.

Bốn phần đã làm:

1. `tools/GhepCau.java` ghép ba khung từ tấm hiệu ứng 221: sáu quả cầu, ba mỗi bên, chừa giữa cho
   nhân vật. Mỗi chỗ một quả khác nhau và xê dịch giữa các khung -- dùng chung một quả cho cả sáu
   chỗ thì nhìn như sáu hạt đậu xếp hàng. Ảnh 3370-3372, đủ bốn cỡ.
2. Vật phẩm **1263 Ngọc Lục Đạo**, loại 12 (đúng ô yoroi), `fashion = -1` để đi đường `coat`.
3. `FashionFromEquip` đọc ô áo choàng ở **cả trang bị 1 lẫn trang bị 2** -- bản gốc chỉ đọc trang
   bị 1, cùng kiểu bỏ sót đã thấy ở tóc/thân/chân.
4. `tools/AoChoangHook.java` chèn vào **đầu** hàm tra bảng: `if (w == 1263) return new int[]{...}`.
   Chèn ở đầu nên không cần hiểu phần còn lại của hàm.

Nhận diện hàm bằng **chữ ký** chứ không bằng tên: "hàm trả về int[] có đọc trường w kiểu short" --
chỉ có đúng một, và không đổi theo tên bị làm rối.

Chèn nhánh rẽ làm bảng khung ngăn xếp cũ sai, nên **hạ phiên bản lớp xuống 49**: từ 50 trở lên máy
ảo đòi bảng khung, còn 49 thì dùng bộ kiểm suy diễn đời cũ. Rẻ hơn bắt ASM 3.1 dựng lại khung cho
lớp đã bị làm rối tên. Chưa thử trên máy Java thật -- máy thật đòi mã đã tiền kiểm tra, đây là chỗ
có thể vấp.

Bẫy trong `nap.sh`: cắt trắng log rồi chờ dòng báo xong là **sai** -- pm2 giữ tay cầm tệp nên nội
dung cũ còn nguyên, dòng báo xong của lần trước bị đọc nhầm thành của lần này. Đổi sang đếm số dòng
trước khi khởi động rồi chờ số ấy tăng.

### Ngọc Lục Đạo chưa hiện -- đang chia đôi để khoanh

Trạng thái: **máy chủ gửi đúng, client vá đúng, mà vẫn không vẽ.**

Đã chứng minh chắc chắn:
- Máy chủ đặt và gửi `coat=1263` -- dòng nhật ký tạm `[ao-choang]` trong `FashionFromEquip` ghi
  `kagetest coat=1263 ID_PP=-1 tb1=1263 tb2=-`.
- Bộ vá nằm đúng trong jar và đúng hình dạng: `getfield w; sipush 1263; if_icmpne; iconst_4;
  newarray int; ...; areturn`.
- Hàm bị vá đúng là hàm nằm trên đường vẽ (được gọi từ hàm nhận tham số Graphics).
- Hạ phiên bản lớp xuống 49 không làm hỏng gì: client chạy, không có VerifyError.

Còn hai khả năng, và bản dựng hiện tại **đang chia đôi chúng**: `test/NSO-mod.jar` tạm ánh xạ
`1263 -> {1635, 1636, 1637}` tức art của Faiyaa yoroi.

- Nếu hiện ra **áo choàng đỏ** -> đường vẽ thông, lỗi nằm ở ba ảnh mới 3370-3372 (client không nạp
  được chúng). Khi ấy phải xem cloak lấy ảnh từ bảng nào, có phải `nj_image` không.
- Nếu **vẫn không có gì** -> lỗi ở đường vẽ, phải đọc tiếp phần sau chỗ gọi hàm: nó lưu mảng vào
  biến 7 rồi rẽ theo trường trạng thái (`gb`), chưa lần tới chỗ vẽ ảnh thật.

`mod.sh` nay nhận biến môi trường `AO_CHOANG="<mã món> <ảnh1> <ảnh2> <ảnh3>"` để đổi nhanh mà không
phải sửa file.

Nhớ gỡ dòng nhật ký `[ao-choang]` khi xong.

### Ngọc Lục Đạo -- đường áo choàng: những gì đã chứng minh, và chỗ bí

Mục tiêu: món trang bị 2 loại 12 (áo choàng) hiện sáu quả cầu sau lưng. **Chưa hiện.**

Đã chứng minh chắc chắn, bằng nhật ký hai đầu chứ không phải suy đoán:

1. **Máy chủ gửi đúng.** `[ac] kagetest coat=1263`, và cả ba nhịp hoãn 3s/8s/15s đều ghi
   `[ac-gui] gui coat=1263`. Gói `loadCoat` thật sự bay đi.
2. **Client không bao giờ nhận.** Bộ vá in giá trị trường `w` mỗi khung hình: **luôn là -1**,
   hơn một nghìn lần, kể cả sau khi cởi ra mặc lại trong game.
3. **Trường `w` chỉ có hai chỗ ghi** trong toàn client: hàm khởi tạo đặt -1, và bộ xử lý gói
   `loadCoat`. Bộ xử lý ấy tra nhân vật theo id rồi **bỏ qua lặng lẽ nếu không thấy** -- nhân vật
   của chính mình không nằm trong sổ đó, nên gói rơi vào chỗ trống. Gói này chỉ để vẽ áo choàng
   của **người khác**.
4. **Bảng art áo choàng chỉ có một** (`C769.C226()[I`, chứa mã ảnh 1635/1636/1637 của Faiyaa
   yoroi), và nó đọc `w`. Bộ vá đã chèn nhánh cho mã 1263 vào đầu bảng ấy, đã kiểm bytecode là
   đúng hình dạng.
5. **Chốt chặn trước lời gọi** (`mảnh.pi[khung].id > 4`) đã mở, không phải nguyên nhân.
6. Người dùng xác nhận **Faiyaa yoroi vẫn hiện trên nhân vật của chính họ**.

Điểm (2)+(4)+(6) mâu thuẫn nhau: áo choàng hiện mà `w` chưa bao giờ được đặt. Nên phải còn một
đường vẽ thứ hai chưa tìm ra. Manh mối duy nhất: `C483.C999` đọc **món đang mặc trong mô hình túi
đồ của chính client** (`C29.qi.eo` = mã món) rồi so với **420** -- tức client tự biết mình mặc gì,
không cần máy chủ nói. Nhưng hàm ấy không chứa mã ảnh nào, nên chưa rõ nó nối vào đâu.

Việc còn lại nếu quay lại đường này: dò tiếp `C483.C999` xem nó dẫn tới đâu, rồi vá thêm mã 1263
vào phép so sánh đó.

Công cụ đã dựng, dùng lại được: `tools/DoLenh.java` (dò lệnh máy chủ trong client, lần được cả
nhánh của bảng nhảy rộng), `tools/AoChoangHook.java` (chèn nhánh art + mở chốt + tuỳ chọn in giá
trị `w` qua biến môi trường `AO_CHOANG_LOG`), `tools/GhepCau.java` (ghép ba khung cầu từ tấm 221).

Ảnh 3370-3372 và vật phẩm 1263 đã có sẵn trong cơ sở dữ liệu, dùng lại được cho đường khác.

### Cầu lục đạo -- món trang bị 2 có hiệu ứng xoay quanh người

**Chạy được.** Hai màu: 1263 tím, 1264 đen. Cấp 130, ô áo choàng của trang bị 2.

Đường đi, sau khi đã đâm vào hai bức tường:

1. **Hình nhân vật chỉ có bốn lớp** -- vũ khí, chân, đầu, thân -- vẽ đúng thứ tự đó. Lớp duy nhất
   nằm sau lưng là **lớp vũ khí**, và không có lớp trống nào để thêm trang trí. Nên món này mượn
   lớp vũ khí: đặt `owner.weapon` = mã mảnh loại 3. Cái giá: mặc vào mất **hình** vũ khí.
2. **Client không áp gói cập nhật diện mạo cho nhân vật của chính mình.** Đo và xác nhận hai lần:
   gói áo choàng (`loadCoat`) và gói vũ khí (`loadWeapon`) đều tới nơi -- log máy chủ ghi rõ đổi
   mảnh liên tục -- mà hình vẫn đứng im. Đó là lý do đường áo choàng chết hẳn.
3. **Lối thoát: gọi thẳng `Char.setFashion()` mỗi nhịp.** Nó đi thêm đường `updateInfoMe`, tức gói
   mô tả lại chính mình, và client **chịu áp** gói ấy. Đây mới là mấu chốt làm hiệu ứng chạy được.

Hoạt ảnh: lớp vũ khí chỉ dùng **một khung ảnh** cho cả mười khung hình nhân vật (bảng CharInfo ghi
cứng), nên không thể làm hoạt ảnh trong mảnh. Thay vào đó `NgocLucDao` giữ một pha toàn cục, đổi
mỗi 300ms, và `FashionFromEquip` lấy mảnh theo pha ấy. Sáu ảnh là sáu pha của cùng quỹ đạo elip,
mỗi pha lệch 10 độ; sáu quả cách đều 60 độ nên sau sáu pha quả thứ k rơi vào chỗ quả k+1 -- vòng
khép kín, không giật.

`tools/GhepQuay.java` sinh ảnh: elip bẹp (ngang gấp đôi dọc) để đọc ra là vòng nhìn nghiêng; quả
nửa dưới dùng cầu to, nửa trên cầu nhỏ để gợi chiều sâu -- cả lớp đều vẽ sau lưng nên không cho
quả nào ra trước được. Tham số `den` rút màu theo độ sáng mắt người rồi ép xuống dải 0..150, giữ
khối cầu và điểm sáng thay vì tô đen phẳng.

Thêm màu mới: sinh 6 ảnh, tạo 6 mảnh loại 3, thêm một dòng vào bảng `BANG` trong `NgocLucDao`.

### Dò khả thi: nhảy đôi (double jump)

**Phía máy chủ: không phải làm gì.** Chuyển động do client tự tính rồi gửi toạ độ lên; máy chủ chỉ
kiểm ba thứ, và không thứ nào chạm tới trục dọc:

- `Zone.isCanMove(...)` **luôn trả `true`** (Zone.java:236) -- không kiểm gì
- giới hạn tốc độ chỉ áp **trục ngang**: `|x - preX| > speed * 17`
- `GameData.ANTICROSS_MAP` (ép y xuống mặt đất) **đang tắt** (`= false`)

Nên nhảy cao gấp đôi hay nhảy lần hai giữa không trung, máy chủ nhận hết. Người xung quanh cũng
thấy đúng vì họ nhận toạ độ qua `playerMove`.

**Phía client, đã dò được:**

- Phím vào `C582.fS` (vừa bấm) và `C582.fQ` (đang giữ), hai mảng `boolean`, do `C43.C889(I)V` điền
  từ một `lookupswitch` trên mã phím.
- Chỉ số phím: **2 = lên/nhảy**, 4 = trái, 6 = phải, 8 = xuống, 5 = hành động.
- Nơi tiêu thụ là **`C483.C45` -- 3767 lệnh**, vòng lặp chính gộp cả nhập liệu lẫn di chuyển; và
  `C483.C606` (4219 lệnh). `fQ[2]` được đọc **22 lần** trong đó.
- Hai ngã rẽ đã loại: `C483.C372()` chỉ đặt lại cờ (huỷ tự đi), `C483.C45(B)V` đẩy byte hành động
  lên máy chủ. Vật lý nhảy không nằm ở hai chỗ này.

**Đánh giá:** làm được, nhưng chỗ phải vá là một phương thức gần 3800 lệnh đã bị làm rối tên, với
22 chỗ đọc phím nhảy trộn lẫn cùng leo, bay, trèo. Nặng hơn vụ áo choàng, mà vụ đó đã ngốn gần
trọn một phiên.

**Bước kế nếu làm tiếp:** tìm **trường vận tốc dọc** -- thứ được gán lúc nhảy rồi bị trọng lực trừ
dần mỗi nhịp. Biết trường đó thì nhánh nhảy lộ ra ngay, và bộ vá chỉ là thêm một bộ đếm đặt lại
khi tiếp đất.

Công cụ dò dùng lại được: `tools/DoLenh.java`, và mấy bộ dò tạm trong /tmp (TimSo, DumpTen,
ChiSoPhim, DocTruong, TheoDau) -- nên chuyển vào tools/ nếu quay lại việc này.

## Kỹ năng hỗ trợ hỏng (dò ngày 22/08/2026, chưa sửa)

- [ ] **Bousouhayate (#52, Ninja quạt, cấp 40) không có tác dụng thật.** Chiêu tạo `Effect(20)`
      đúng quy trình, ghi vào bộ hiệu ứng và gửi xuống client -- người chơi thấy biểu tượng buff
      và đồng hồ đếm ngược -- nhưng effect đó mang `type = 16`, mà `EffectManager.effect()` không
      có nhánh nào cho 16, và không dòng nào trong mã nguồn đọc `type == 16` hay `findByID(20)`.
      Nên thời gian bỏng/đóng băng/choáng không hề đổi. Chỗ duy nhất thật sự rút ngắn mấy hiệu
      ứng đó là `Char.java:8066`, và nó đọc chỉ số trang bị (mã 41, 44), không phải chiêu.
      *Sửa:* thêm `case 16` vào `EffectManager.effect()`, đọc `param`/`param2` trừ vào thời gian.
      `EffectManager.java` biên dịch lại được bình thường.

- [ ] **`reductionRate` bị đảo dấu**, `Char.java`, trong `effectResistance` / `revival` /
      `buffIminity` (dùng chung cho Bousouhayate, Suishou, Hayatemi):
      ```java
      double reductionRate = 0.03 * skill.point;  // chú thích: "Mỗi cấp tăng thêm 3%"
      param -= param * reductionRate;             // nhưng lại TRỪ
      ```
      Đổ điểm vào lại làm buff yếu đi. Bousouhayate 12 điểm đáng ra 70 giây, tính xong còn ~45
      giây -- thua cả mức 8 điểm. *Chặn:* `Char.java` còn kẹt Lombok, phải gỡ trước như đã làm
      với `Item.java`.

- [ ] **Totaaigo (#58, Ninja cung)**: lượng né ghi cứng `15000` ở `Char.java:6821`
      (`new Effect(11, options[0].param, 15000)`), bảng kỹ năng chỉ điều khiển thời gian.
      1 điểm hay 15 điểm né mạnh như nhau, chỉ khác giữ được 2 giây hay 10 giây. Cũng cần gỡ
      Lombok cho `Char.java`. Không gấp -- chiêu vẫn chạy, chỉ là thang điểm vô nghĩa một nửa.

- [ ] **Sogekihei (#31, Ninja cung)** chạy đúng nhưng chỉ ăn khi quái đánh (`Mob.java:962`).
      Đường PvP có biến `dameMp` khai báo bằng 0 rồi không bao giờ được gán. Đúng mô tả của chiêu
      nên không phải lỗi -- ghi lại phòng khi muốn cho nó ăn cả PvP.

## Phá trần kỹ năng (limit break) -- ĐÃ GỠ ngày 22/08/2026

Làm xong rồi gỡ theo yêu cầu vì quá nhiều lỗi trong lúc dựng. Ghi lại những gì đã dò được, để
nếu quay lại thì không phải mò lần nữa:

- **Bảng `skill_template.skillTemplates` (cột JSON) ĐÃ CHẾT.** Trông y như nơi chứa dữ liệu từng
  cấp nhưng đoạn nạp nó trong `GameData` bị chú thích hết, và số liệu bên trong đã lệch thực tế
  (chiêu 86: cột JSON ghi max_fight 6, bảng thật ghi 7). Nguồn thật là **BẢNG `skill`**,
  `LOAD_SKILL = "SELECT * FROM skill WHERE template_id = ? ORDER BY point ASC"`.
- **Trần cứng của giao thức** (`Server.setSkill`): maxPoint / số cấp / cấp nhân vật yêu cầu đều
  `writeByte` nên <= 127; `option.param` và `manaUse` là `writeShort` nên <= 32767.
- **`max_point` là một con số dùng chung cho cả máy chủ**, gửi một lần lúc đăng nhập, không thể
  khác nhau theo từng người. Đây là gốc của mọi rắc rối hiển thị:
  - Để bằng trần cao nhất -> ai chưa phá trần cũng thấy "Cấp tối đa 11", khó hiểu.
  - Để bằng trần gốc -> khi phá trần xong, **cấp hiện tại vượt cấp tối đa** và **khung thông tin
    trong game vỡ** (cụt, mất nút Cộng/Gắn/Đóng). Kiểm chứng: cấp 2 khung đủ nút, cấp 11 thì cụt.
  - Muốn đúng cả hai thì phải vá client, dò chỗ vẽ khung.
- **Khung thông tin chỉ vừa 9 dòng.** Mô tả chiêu dài 97 ký tự (3 dòng) làm khung cụt; rút xuống
  26 ký tự là vừa. Sáu chiêu cấp 120 đều đang có mô tả 81-115 ký tự.
- **Quy ước lên cấp của game** (rút từ chiêu 83, 18, 78, 62): thời gian chờ GIẢM tuyến tính mỗi
  cấp (500->350 ms), MP chỉ nhích rất nhẹ (177->210 qua 12 cấp). Giảm thời gian chờ mới là phần
  thưởng chính.
- **`type 27` không xếp chồng** (`ItemTemplate.isTypeStack()` chỉ cho HP/MP). Muốn phát 5 quyển
  sách thì phải 5 ô riêng, ghi `quantity: 5` vào một ô sẽ bị chuẩn hoá về 1.
- Bảng quản trị và trong game **kiểm trần bằng hai luật khác nhau** nếu không cẩn thận:
  `CharAdmin` dùng `template.maxPoint`, còn `Char.upSkill` dùng trần riêng -> tạo được trạng thái
  cấp cao hơn trần, và từ đó ra lỗi tụt cấp.

Đã trả nguyên trạng: bảng `skill` chỉ còn cấp 0-1 cho chiêu 86, `max_point`=1, mô tả gốc, xoá
vật phẩm 1265, xoá `PhaTran.java` và `tools/pha-tran-ky-nang.py`, dọn `phaTran` khỏi dữ liệu
người chơi. `Char.java` đối chiếu `javap` khớp đúng 1182/1182 chữ ký như trước.

## Việc ngày 23/08/2026 (từ tasks/23.8.md)

Bảng quản trị web -- gom thành một đợt vì đụng cùng một tệp charadmin.html:
- [x] 1. Modal thêm món tự đóng ngay khi bấm thêm -- phải giữ modal để thêm tiếp
- [x] 2. Kỹ năng: thay `title=` bằng tooltip thật; mô tả kỹ năng phải chi tiết, không chung chung
- [x] 3. Thêm món: cũng cần tooltip, hiện nhanh hơn
- [x] 4. Điểm kỹ năng còn lại hiện dạng x/y cho dễ đọc
- [x] 5. Thêm nút reset kỹ năng về như chưa từng học
- [x] 6. Gộp tiềm năng và kỹ năng thành tab
- [x] 7. Ô tìm kiếm: thêm tab lọc sẵn (vũ khí, sách, tb2, mặt nạ...)
- [x] 0. Làm lại giao diện tổng thể (làm sau cùng, sau khi mấy phần trên đã đúng)

Máy chủ:
- [x] 8. Dựng lại toàn bộ phá trần kỹ năng (limit break) đã gỡ hôm qua
- [x] 9. Cầu lục đạo: chỉ xoay khi đứng yên, đang di chuyển hoặc nhảy thì đứng hình. Tự test.

## 23/08 — Bí kíp danh hiệu: tên, chỉ số, icon, biểu ngữ
- Chỉ số: `com.nsoz.item.DanhHieu` (bảng duy nhất, gồm cả bộ Hokage và thiên bảng chuyển từ
  Item.initOption sang, không đổi số cũ).
- Tên: 14 danh hiệu Noname + 5 thiên bảng đã đặt tên trong bảng `item`.
- Icon: 23 icon riêng, mã 26121-26143, sinh bằng `tools/VeIconDanhHieu.java`, xuất đủ 4 mức phóng
  vào Data/Img/Small/{1,2,3,4}.
- Biểu ngữ bay trên đầu: 19 cái, mã hiệu ứng 254-272, sinh bằng `tools/VeBangDanhHieu.java`
  (ảnh + câu SQL cho bảng effect_data). Bốn Hokage giữ biểu ngữ vẽ tay cũ 201-204.
- game.item.version 61 -> 63.

Còn lại
- Món đã nằm trong túi người chơi giữ chỉ số cũ (1 Akatsuki, 2 Ám Bộ Anbu đang rỗng) — muốn có
  chỉ số phải xoá đi phát lại.
- Biểu ngữ mới là chữ trên tấm nền có tia sáng; biểu ngữ vẽ tay của game (hiệu ứng 12-20) có cánh
  và hào quang, đẹp hơn hẳn. Muốn ngang ngửa thì phải vẽ tay từng cái.
