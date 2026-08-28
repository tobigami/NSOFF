# Lessons — NSOFF

## Từ phản hồi của user

**1. Không tự ý kết nối mạng để lấy dữ liệu, kể cả khi kỹ thuật cho phép.**
Tôi đề xuất "harvest 1 lần từ server thật" để có nội dung gốc. User chọn phương án tự sinh
từ atlas trong JAR. → Với mục tiêu "offline hoàn toàn", mặc định là **không đụng mạng ở bất kỳ
khâu nào**, kể cả build time. Nếu thấy một hướng cần mạng, hỏi trước, đừng làm rồi báo.

**2. Khi một hạng mục phụ thuộc thông tin user sẽ cung cấp → dừng đúng hạng mục đó, làm tiếp phần khác.**
User nói phần ghép hình nhân vật "tạm dựng lại đã, tôi sẽ cung cấp thêm thông tin sau".
→ Không đầu tư thêm vào hạng mục đó, nhưng cũng không dừng cả dự án. Ghi rõ trạng thái "đang
chờ" rồi chuyển sang hạng mục khác.

**3. User hỏi "đang khó ở phần nào" giữa chừng = cần trạng thái ngắn gọn, phân biệt rõ
"khó vì thiếu dữ liệu" vs "khó vì chưa làm xong".**
Lần đầu tôi trả lời gộp cả hai làm user hiểu nhầm là đang bế tắc. Phải tách:
- cái đã giải xong (cơ chế layout part)
- cái **không thể giải** vì dữ liệu chưa từng có trong file (bảng `nj_part`)
- cái chỉ tốn thời gian (dò id chéo trong protocol)

**4. Hỏi user xem có sẵn tài nguyên gì trước khi tự dựng lại từ đầu.**
Tôi đã bỏ nhiều công tách sprite từ atlas và tự soạn database item/mob/skill. Sau đó user đưa
`SETUP_LOCAL/` có nguyên source server + dump MySQL — toàn bộ phần tự dựng thành thừa.
→ Khi phát hiện "dữ liệu này không có trong file", câu hỏi tiếp theo phải là **"bạn có bộ
server/data nào không?"**, hỏi NGAY, trước khi đầu tư vào phương án tái tạo.

## Kỹ thuật — tránh làm lại

**Class ở package có tên không tham chiếu được class ở unnamed package** (Java ≥1.4).
→ Mọi patch phải đi qua default package (`bs`, `w`, `dh`, `dq`, `dc`…). Với `main.*` chỉ còn
cách sửa bytecode. Kiểm tra điều này TRƯỚC khi lên kế hoạch patch, đừng để viết xong mới phát hiện.

**Đừng đoán ý nghĩa field trong protocol obfuscate — truy ngược chỗ dùng.**
Đã trả giá 2 lần:
- gửi `player.x` vào chỗ thực ra là *effect id* → `dg.D[420]` ArrayIndexOutOfBounds
- gán `A[i].a = imageId` trong khi client dùng nó làm `dg.D[a-1]`
→ Quy trình đúng: grep chỗ field đó được ĐỌC, không chỉ chỗ được GHI.

**Client hardcode nhiều index cố định vào bảng do server gửi.**
`dg.D[53,56,57,62,66]`, `dg.A[38]`, image id 988/989/1060/1224/1291. Bảng sinh ra phải dư chỗ,
không được vừa khít số phần tử mình thực sự dùng.

**Số phần tử của part phải khớp slot.** `bp.bA[pose][slot]` index tới 7/9/17/1 cho slot
0/1/2/3 → part gán sai slot là crash. Thứ tự 4 short override là `bO, bR, bQ, bP`
(đầu, vũ khí, chân, thân) — KHÔNG phải thứ tự trực giác.

**Verify bằng 2 tầng, đừng chỉ dựa vào emulator.**
- `ProtoTest` nói chuyện thẳng với server qua pipe → xác định logic đúng/sai, chạy nhanh, không
  phụ thuộc input.
- Harness + MicroEmulator → xác định client thật sự render được.
Tầng 1 bắt được lỗi mà tầng 2 khó dựng tình huống (vd: đánh chết quái cần chọn target bằng tap).

**`Component.printAll()` chụp được cửa sổ nằm ở macOS Space khác** — `screencapture` thì không.

**Có source server thì port serializer, đừng đoán từ client.** Sau khi có `NSO_KEM/src`, mọi
format đều đối chiếu được 1-1. Việc này bắt được 2 chỗ tôi suy luận sai từ client:
- `af.o` không phải "tilemap id" mà là **mapId** (file `/map/<mapId>`)
- `dg.Y` không phải "loại item" mà là **ItemOptionTemplate**

**Test bắt được vấn đề thiết kế, không chỉ bug.** Test đánh quái fail vì map 1 toàn bù nhìn
500k HP — đó là dữ liệu đúng, cái sai là **chưa có đường đi sang map khác**. Nếu chỉ sửa test
cho pass thì đã bỏ sót tính năng thật sự thiếu (waypoint).

**JVM treo vì thread non-daemon.** `ProtoTest` chỉ `System.exit` khi FAIL → khi PASS thì thread
server giữ JVM sống, `test.sh` đứng im. Test harness phải luôn exit tường minh.

**Client đọc payload theo hàm helper, phải trace tới tận nơi.** Message 11 (mặc đồ) nhìn qua thì
`an.java:782` đọc `byte slot` rồi `bp.d().a(var1)` — cái `a(ce)` đó là `readParam`, và nó đọc
**speed, maxHp, maxMp** (`bp.java:446`). Tôi tưởng byte thứ 2 là level nên gửi level vào ô speed →
mặc kiếm xong nhân vật bò 1 px/frame. Bài học: khi client gọi một helper để parse, phải mở helper
ra xem nó đọc field nào, đừng suy ra từ tên message.

**Đừng để test xoá state của người đang chơi.** `test.sh` có `rm -rf ~/.microemulator` để harness
tạo char mới — nhưng đó cũng là nơi MicroEmulator lưu save thật, nên mỗi lần chạy test là mất
tiến trình của user. `org.microemu.app.Config` đọc `user.home`, nên chạy harness với
`-Duser.home=build/emu-home` là cô lập được. Test cần state sạch thì **cấp state riêng cho test**,
đừng xoá state chung.

**Lưu game phải lưu *đủ*, và đừng ghi đè state đã nạp.** Save chỉ chứa chỉ số + nhiệm vụ, còn túi
đồ và skill thì không — tệ hơn, nhánh "chọn nhân vật cũ" gọi `giveStarterKit()` vô điều kiện, đè
sạch mọi thứ nhặt/mua được. Nhân vật trông như còn nhưng đồ về mo. Bài học: state nào thuộc về
nhân vật thì để trong `Player` và lưu hết một lượt; nhánh "tạo mới" và "vào lại" phải khác nhau rõ
ràng, đừng dùng chung code khởi tạo.

**Không có sự kiện "thoát game" thì đừng throttle việc lưu.** `saveThrottled()` bỏ hẳn lần ghi
trong cửa sổ 15s → mặc kiếm xong tắt emulator là kiếm quay lại túi. Hoãn-rồi-flush-ở-tick cũng
không cứu được, vì tiến trình chết trước khi tick chạy — test tự dựng lại đúng kịch bản đó và vẫn
fail. Các chỗ gọi đều là hành động rời rạc (giết quái, mặc, ăn, mua, đổi map) và record chỉ vài
trăm byte, nên throttle là tối ưu hoá vô nghĩa: bỏ đi, ghi thẳng.

**Không test được thì stub cái chặn đường.** `nsoff.Save` không chạy trong `ProtoTest` vì
RecordStore của MicroEmulator cần `MIDletBridge` có emulator thật, nên phần lưu chưa từng được
test. Viết `tools/testrms/.../RecordStore.java` in-memory rồi đặt trước stub jar trên classpath là
test được vòng lưu/nạp mà không cần boot emulator.

**Client gate ở đâu thì phải sửa dữ liệu gửi cho client, không phải logic server.** Waypoint đổi
map: server gốc khớp có dung sai ±100px (`TileMap.findWaypoint`), nhưng **client** (`bp.G()`) mới
là chỗ quyết định có gửi lệnh `-17` hay không, và nó so **khít** với đúng các hình chữ nhật server
gửi xuống. Nới dung sai phía server là vô nghĩa; phải nới chính hình chữ nhật trong bản tin map.

**Test env bẩn thì kết luận sai.** Chạy harness với `user.home` cũ còn save → client dừng ở màn
chọn nhân vật, không vào map, và tôi suýt kết luận "nhân vật không di chuyển được". Harness cần
state sạch phải tự xoá state riêng của nó trước mỗi lần chạy.

**Đọc số có dấu bằng unsigned là bug ẩn, và cái "fallback" tôi thêm chỉ che nó đi.** Bảng `task`
dùng `-2` nghĩa là "bước này không có NPC". Tôi ghi/đọc bằng byte không dấu → `-2` thành `254`,
sinh ra NPC ma. Thay vì truy nguyên, tôi từng thêm fallback "NPC không tồn tại ở map nào thì cho
NPC bất kỳ hoàn thành bước" — che được triệu chứng nhưng khiến **nói chuyện với ai cũng xong bước
"Sử dụng vũ khí"**. Gặp giá trị vô lý (254 trong bảng chỉ có 48 NPC) thì phải nghi ngờ khâu
encode, đừng viết luật để sống chung với nó. Sửa đúng: ghi bằng short có dấu (map id tới 178 nên
byte có dấu không đủ).

**Nới dung sai thì phải chặn hiệu ứng phụ của chính nó.** Nới ô waypoint để dễ chạm vào lối ra
làm điểm đáp bên kia rơi luôn vào ô lối về → client (vốn hỏi mỗi frame khi đứng trong ô) nhảy qua
nhảy lại hai map vô hạn. Sửa: lối ra chỉ tính lại sau khi người chơi đã bước ra khỏi mọi ô trên
map đó. Bài học: mọi lần nới điều kiện kích hoạt đều phải hỏi "cái gì bây giờ vô tình thoả mãn
điều kiện này?".

**AI quái phải port từ server, đừng tự chế cho "hợp lý".** Tôi cho mọi quái trong ô 48×48 đánh
người chơi với xác suất 25%/tick. Thực tế server: (1) loại trừ hẳn bù nhìn / mộc nhân / thảo dược
— chúng là bia tập, không bao giờ đánh trả (`Mob.java:1602`); (2) điều kiện aggro là **cùng cao
độ** và `|dx| < rangeMove + 20`, riêng quái bay (`type == 4`) mới đuổi theo cả hai trục. Hậu quả
của bản tự chế: đứng cạnh bù nhìn ở Làng Tone là bị đánh và thấy nó vung vũ khí dù người chơi
không làm gì. Có source server thì mọi luật gameplay đều tra được — đừng đoán.

**Giá trị enum phải tra trong source, đừng suy từ "0 chắc là tắt, 1 chắc là bật".** Byte trạng
thái quái trong bản tin map: server dùng **5 = sống**, **0 = chết** (`Mob.java:167`, `:331`), còn
`1` là trạng thái *vừa chết*. Tôi gửi `1` và ghi comment "0 sẽ làm quái không chọn được" — đoán
sai hoàn toàn, hậu quả là **không con quái nào hiện lên**. Triệu chứng dễ đổ cho render/ảnh, và
tôi đã mất thời gian kiểm tra PNG, tầm nhìn camera, tường chắn trước khi tra đúng chỗ. Có source
server thì tra thẳng giá trị, đừng suy diễn.

**Test dựa vào xác suất phải có cửa sổ đủ rộng.** Check "quái thật vẫn tấn công" chờ 6s, mà quái
chỉ đánh với xác suất 25% mỗi tick 600ms → ~6% số lần chạy fail oan. Suýt nữa tôi tưởng là
regression của bản sửa aggro. Nới lên 15s (~25 tick) thì fail oan còn ~0.3%, mà lỗi thật vẫn lộ.

**Client từ chối hành động thì nó nói lý do — đọc string ra là biết.** Không đánh được quái vì
`dg.java:3216` kiểm tra `bp.aD[1]` (ô trang bị **vũ khí**) rỗng thì hiện `df.gF` = "Vũ khí không
thích hợp." rồi thoát. Cái "hộp nhỏ" user thấy chính là thông báo đó. Tra thẳng hằng chuỗi trong
`df.java` nhanh hơn nhiều so với dò luồng.

**Đừng xoá dữ liệu khi mới chỉ chuyển chỗ.** `equip()` cũ set `inventory[slot] = 0` mà không lưu
món đồ đi đâu → mặc xong là **mất vũ khí vĩnh viễn**, đăng nhập lại không bao giờ đánh được nữa.
Client (`bp.a(int)`, `bp.java:541`) làm đúng: chuyển đồ vào ô theo `type`, món đang mặc rơi ngược
về đúng ô túi. Server phải làm y hệt, và char info phải phát lại đồ đang mặc, nếu không client
tưởng nhân vật ở trần.

**Cộng dồn chỉ số khi mặc đồ là sai.** `dmg += 2` mỗi lần equip nghĩa là tháo ra không trả lại, và
mặc lại cùng món thì cộng tiếp. Tính lại từ base + đồ đang mặc mới đúng.

**Kẹp giá trị ở một chỗ, dùng ở chỗ khác là sai.** HP bù nhìn bị kẹt từ 500.000 xuống 100 lúc
spawn, nhưng công thức exp (`lv*3 + hp/20`) lại chạy lúc build trên HP **gốc** → một con bù nhìn
level 1 trả 25.000 exp. Dồn phép kẹp về đúng một nơi (lúc build), rồi mọi thứ dẫn xuất từ đó.

**Item "tiền" không phải item.** Item type 19 ("Yên") rơi ra được nhặt vào **túi đồ** như vật
phẩm, thay vì cộng vào ô tiền. Client hiển thị 3 loại tiền qua lệnh 13: `ar`=Xu, `at`=Yên,
`au`=Lượng (`df.eD/eE/eF`); tôi gửi Yên và Lượng luôn bằng 0.

**Client giả định máy cảm ứng thì màn hình rộng.** `main.a.g` bật cho mọi máy có pointer (emulator
luôn có chuột), và `ae.a(t)` khi đó chừa 170px hai bên cho nút cảm ứng → màn 176px chỉ còn **6px**
cho dòng chữ chạy. Khi port sang màn nhỏ hơn thiết bị gốc, phải rà lại các layout gate bằng "có
cảm ứng" chứ không phải bằng kích thước thật.

**Sửa một đường vào thì phải rà mọi đường vào còn lại.** Tôi sửa `pickUp()` để Yên chảy vào ví,
test pass, nhưng user vẫn thấy Yên nằm trong túi — vì `giveStarterKit()` cũng nhét 3 món đầu của
bảng rơi đồ vào túi, mà `drops[1]` chính là Yên. Cùng một khái niệm ("tiền không phải vật phẩm")
phải áp ở **mọi** chỗ item đi vào túi, không chỉ chỗ đang debug.

**Bảng rơi đồ tự chế = sai hoàn toàn.** Tôi chọn 4 item bằng cách khớp tiền tố tên ("thuốc", "xu",
"yên", "đá", "mảnh") rồi random đều — hậu quả: bù nhìn level 1 rơi ra "Thuốc cải tiến" **level
70**. Server thật có `RandomItem.ITEM` với trọng số (5/5/5/1/1/1/1/1 cho Đá cấp 1..5, Phúc nang,
Bình HP/MP nhỏ nhất) **cộng với quy đổi theo cấp quái** trong `Mob.randomItemID()`: Đá cấp 1 →
`level/15`, bình HP/MP → bậc theo mốc level 10/30/40/70/90. Đã port nguyên. Bài học lặp lại: có
source server thì port, đừng tự nghĩ heuristic.

**Regex trên file lớn phải tránh alternation lồng quantifier.** `\((\d+), '(?:[^']|'')*', ...`
chạy trên dump SQL 30MB gây backtracking thảm hoạ, test treo 6 phút mà tôi tưởng nó chạy chậm.
Parser tay theo trường thì tuyến tính và đọc rõ hơn.

**Chỉ xử lý một loại item là bỏ sót cả hệ thống.** `equip()` chỉ nhận `type == 18` (thức ăn), nên
**bình HP (16) và bình MP (17) bấm vào không làm gì** — user báo "item k sử dụng được". Server có
nhánh riêng cho từng loại trong `Char.useItem` với lượng hồi cụ thể theo từng item id.

**Hiệu ứng phải gửi cho client mới thấy.** Dùng thức ăn/thuốc mà không gửi `-30/-101`
(`Service.addEffect`) thì client không vẽ icon buff → nhìn như không có tác dụng gì, dù server đã
cộng máu. Thay đổi trạng thái ngầm luôn cần một thông báo tương ứng ra UI.

**Đá không stack là đúng luật game, không phải bug.** Cột `isUpToUp` trong bảng `item` quyết định
(`Char.java:6066`): bình thuốc/vật liệu = 1, đá cấp 1..5 và cơm nắm = 0. Nhưng server tôi **chưa
bao giờ gộp chồng bất cứ thứ gì** — đó mới là bug. Trước khi kết luận "game nó thế", phải tra cột
dữ liệu quyết định.

**Từ chối một yêu cầu không có nghĩa là im lặng.** Guard chống nhảy map vô hạn của tôi bỏ qua lệnh
`-17` khi chưa "armed" — nhưng client đã kịp bật `bp.bG` (màn "Đang tải…") ngay khi gửi, và cờ đó
**chỉ tắt khi nhận được bản tin map**. Không trả lời = treo vĩnh viễn. Sửa hai lớp: (1) điểm đáp
được đẩy ra khỏi mọi ô lối ra nên client không xin đi tiếp nữa, (2) nếu vẫn xin thì server gửi lại
chính map đang đứng để cờ được tắt. Bài học: mỗi lệnh client gửi mà nó *chờ phản hồi* thì luôn
phải có phản hồi, kể cả khi từ chối.

**Sửa một triệu chứng có thể tạo triệu chứng ngược lại.** Trước khi có guard: nhảy map qua lại vô
hạn. Sau khi có guard: treo. Cả hai đều xuất phát từ việc điểm đáp nằm trong ô lối về — đó mới là
gốc, và tôi đã vá ở nhánh sai.

**Trả lời cho một yêu cầu lặp mỗi khung hình thì phải có giới hạn tần suất.** Tôi sửa treo map
bằng cách "từ chối thì gửi lại map đang đứng" — nhưng client gửi `-17` mỗi frame khi còn đứng
trong ô lối ra, nên server nạp lại map liên tục, mỗi lần đều `dg.b()` + `System.gc()` phía client.
Thêm throttle 1 giây. Bài học: bất kỳ phản hồi nào sinh ra từ một sự kiện client lặp liên tục đều
cần chặn tần suất, nếu không là tự tạo flood.

**Kiểm biên phải kiểm cả hình chữ nhật, không phải từng số riêng lẻ.** Chọn ảnh rời tôi viết
`x < 255 && w < 255` — nên sprite bắt đầu trong atlas nhưng tràn ra ngoài mép vẫn bị coi là "nằm
trong", và client vẽ ra rác. Đúng phải là `x + w <= 255 && y + h <= 255`.

**Atlas trong JAR chỉ 255×255 nhưng bảng `nj_image` mô tả tới 569×261.** Nghĩa là 1832/3049 sprite
của bản DB này *không* nằm trong atlas mà phải phát rời qua `-28/-115` — con số lớn bất ngờ, và nó
giải thích vì sao nhiều ảnh phụ thuộc hoàn toàn vào đường ảnh rời hoạt động đúng.

**Trường "không có gì" cũng là một giá trị cụ thể — phải tra, đừng điền bừa.** Bản tin quái đánh
người chơi (`-3`) có một trường hiệu ứng đặc biệt; server gửi **-1** (`Service.npcAttackMe`), và
client dịch -1 thành hiệu ứng va chạm thường (59). Tôi tự điền `1`, nên mỗi lần bị đánh client
phát **hiệu ứng số 1** đè lên nhân vật — chính là mảng trắng user thấy. Với mọi trường tôi không
hiểu, giá trị đúng nằm trong source server, không phải trong trực giác của tôi.

**Triệu chứng user mô tả sai vẫn có giá trị — nhưng phải kiểm lại.** User đoán mảng trắng là
animation nhảy/lộn vòng; tôi test nhảy thì không tái hiện được và cũng không có ảnh nào bị thiếu.
Đến khi user nói "có vẻ bị khi quái đánh trúng" thì mới khoanh đúng vào bản tin `-3`. Đoán của
user là dữ liệu, không phải kết luận — nhưng lần thứ hai thì trúng đích.

**Client hiển thị đúng không có nghĩa là server đã có hệ thống đó.** Thanh HP/MP/EXP vẽ đầy đủ và
cấp độ hiện đúng — vì **client tự suy cấp từ tổng exp**. Nhưng phía server tôi chưa từng tăng
`level`, `maxHp`, `maxMp`: đánh mãi vẫn 200/100. Nhìn màn hình thì tưởng xong, thực tế cả nhánh
lên cấp chưa tồn tại. Với mọi chỉ số client tự tính được, phải kiểm riêng phía server.

**Tôi kết luận sai "client không có tự đánh".** Màn hình user gửi có chữ **"Tự đánh"** (`df.M`) —
client *có* chế độ này, bật bằng cách chạm hai lần (`dg.java:2185`, biến `cb`). Cái nó **không**
làm là tìm mục tiêu mới: vòng tự đánh chỉ chạy khi `bp.d().aR != null`, và `bp.J()` xoá mục tiêu
khi quái chết. Lần trước tôi chỉ tra màn hình "Tự động" trong menu (auto uống thuốc/nhặt đồ) rồi
kết luận cho cả tính năng — tra thiếu một chỗ là kết luận sai cả hướng.

**Kill pattern phải khớp đúng dòng lệnh mình tạo ra.** Tôi dọn emulator bằng
`pkill -f "org.microemu"`, nhưng tiến trình do `Play` khởi động có dòng lệnh là
`java ... Play main.GameMidlet` — không chứa chuỗi đó. Kết quả: mỗi lần "mở lại" chỉ **thêm một
cửa sổ**, cửa sổ cũ user đang dùng vẫn chạy bản cũ, và suốt nhiều lượt user báo "chưa fix được gì"
trong khi bản mới chạy ở cửa sổ khác. Tệ hơn: tôi tự xác nhận bằng dòng log `displays: 2` — thứ đó
chỉ chứng minh tiến trình **mới** chạy được, không chứng minh cái cũ đã tắt. Kiểm chứng đúng là so
**giờ khởi động tiến trình với giờ build**. Nay `Play` tự tắt mọi bản cũ trước khi mở.

**Khởi động tiến trình nền phải detach, và phải kiểm nó *còn sống* sau đó.** Tôi mở emulator bằng
`&` bên trong một tác vụ nền của harness → tác vụ kết thúc là tiến trình con bị SIGTERM theo
(exit 144). Tôi báo "đang chạy" dựa trên kiểm tra ngay lúc khởi động, nên nói sai. Đúng: dùng
`nohup ... & disown`, rồi kiểm lại sau vài chục giây.

**Đọc một hàm khởi tạo rồi kết luận cho cả cơ chế là sai.** Tôi tra `Item.initOption()`, thấy chỉ
gán chỉ số cho id 799/800/1156/1157, và kết luận "vũ khí thường không có chỉ số random, mọi bản sao
đều giống nhau, không có bản max". User sửa lại: cây Xà Nhãn Dao lv20 họ nhặt từ lật hình max toàn
bộ chỉ số — và họ đúng. Chỉ số trang bị **không** đến từ `initOption()` mà từ bảng `store_data`
(`options` = mức TỐI ĐA), rồi `Converter.toItem(itemStore, type)` chọn `MAX_OPTION` / `MIN_OPTION` /
`RANDOM_OPTION`; mức tối thiểu do `ItemStore.setMinOptions()` trừ đi một lượng cố định theo từng
loại chỉ số. Bài học: khi một hàm khởi tạo trả về rỗng cho đúng thứ mình đang tra, đó là dấu hiệu
**còn đường khác** chưa tìm ra, chứ không phải bằng chứng là "không có". Phải grep hết các nơi tạo
ra vật phẩm đó (`grep -rn "toItem("`) trước khi kết luận.

**Đừng đoán ý nghĩa cột rồi in ra như sự thật.** Tôi tự chế bảng ánh xạ `type` → tên loại trang bị
và in ra "Dây Thanh Tuyến — Vũ khí (kiếm)". Cột phân biệt hệ vũ khí là `part` (12 kiếm, 41 dao,
13 đao, 14 cung, 15 tiêu, 16 quạt), còn `type` là ô trang bị. Tra bằng
`SELECT type, GROUP_CONCAT(name) ... GROUP BY type` mất 10 giây và cho đáp án chắc chắn.

**Đừng phát hành bản sửa dựa trên giả thuyết chưa kiểm.** Bạn của user báo lệnh `dan` treo trên
KEmulator. Tôi đoán thủ phạm là lời gọi hiện thông báo, bỏ nó đi rồi phát hành 1.1 luôn. Hoá ra
không chữa được gì, mà lại xoá mất dấu hiệu duy nhất cho biết lệnh đã chạy — user tưởng lệnh hỏng
và bảo trả về như cũ. Đúng ra phải **đo trước khi sửa**: dựng một bản có `System.out.println` để
biết nhánh lệnh có chạy không, hoặc lấy thread dump lúc treo. Sau đó mới biết nguyên nhân thật của
hai lỗi khung thông tin là dãy hình thoi vẽ lặp (`nâng cấp/2 + 1` cái) và nhánh vẽ tên lần hai khi
`nâng cấp >= 15` — cả hai tìm ra bằng cách đọc bytecode chỗ vẽ, không phải bằng cách đoán.

**Đọc lại cơ sở dữ liệu, đừng dùng bản dump cũ trong /tmp.** Tôi trả lời "itachi cấp 86" từ file
JSON đã lưu lúc trưa, trong khi nhân vật đã lên 103 — user phải sửa lại. Dữ liệu nhân vật đổi liên
tục (máy chủ tự lưu mỗi 10 giây). File trong scratchpad chỉ dùng cho việc đang làm dở, hết lượt là
coi như hết hạn.

**Khoảng trống trong mã sinh chỉ số là chuyện có thật.** `Item.randomOptionItem10x` chỉ có khối
chỉ số cho 6 vũ khí Thiên Vương và dải 1163-1176; 14 món giáp/trang sức Thiên Vương (1097-1110)
rơi vào khoảng trống nên dựng ra là món trắng trơn. Cách vá không bịa số: gán đúng bảng chỉ số của
món Minh Giác **cùng ô, cùng giới tính** — hai bộ soi gương nhau từng ô. Và chỉ chạm món có danh
sách chỉ số rỗng, để sau này bản gốc bổ sung thì phần vá tự tắt.

**Mã nguồn dịch ngược không phải sự thật — hành vi thực tế mới là.** Tôi khẳng định `clone.service`
luôn null (quét cả bytecode: `Char.setService` chỉ có đúng một nơi gọi là `User.selectChar`), rồi
dựng nguyên một giả thuyết NPE làm hỏng thanh kỹ năng lúc đổi thân. User bác bỏ bằng một câu:
"thanh kỹ năng vẫn đang nạp đúng". Thứ tôi bỏ sót là `CloneChar` **ghi đè** `getService()` —
trả `NoService` khi đang triệu hồi, trả service của chủ thân khi đang điều khiển. Quy tắc rút ra:
khi kết luận từ mã va vào một hành vi mà user quan sát được, hành vi thắng; và trước khi đổ cho
lớp cha, phải kiểm danh sách phương thức của **lớp con** trong bytecode, không chỉ grep mã nguồn.

**Một triệu chứng có thể do lỗi khác hẳn cái đang nhìn.** Sách tẩy kỹ năng "không tăng số lần" hoá
ra chỉ là in nhầm biến (`tayTiemNang` thay vì `tayKyNang`) — biến đếm vẫn tăng đúng. Ngay dòng bên
cạnh lại có lỗi thứ hai không liên quan: `removeItem(index, item.getQuantity(), true)` xoá cả chồng
khi chỉ định dùng một cái. Lỗi này vô hình khi trong tay chỉ có một món, nên sống sót rất lâu.
Khi báo cáo phải tách bạch hai lỗi, đừng gộp làm một kẻo user hiểu nhầm nhân quả.

**Đừng bao giờ viết `catch` nuốt lỗi trong mã đang dò.** Tính năng bù nhìn tập luyện mất **bốn
vòng thử của user** chỉ vì hai khối `catch (Throwable ignored) {}` giấu mất một
`NoClassDefFoundError`. Khi log cuối cùng được bật, nguyên nhân hiện ra trong một dòng. Quy tắc:
mã mới viết thì `catch` phải in ra, kể cả khi mục đích là "không được làm sập thứ khác".

**Đóng gói lớp phải lấy cả `Ten$Long.class`.** Lớp lồng bên trong sinh ra thành file riêng;
`zip -u` mỗi `BuNhin.class` mà quên `BuNhin$Do.class` thì lớp nạp được nhưng ném
`NoClassDefFoundError` đúng lúc chạm tới lớp lồng. Lỗi này im lặng cho tới khi đúng nhánh đó chạy.

**Kiểm đường đi trước khi vá, đừng vá chỗ trông giống.** Tôi móc vào `Mob.Fight` bên trong
`Mob.java`, trong khi cú đánh của người chơi đi qua `Char.attackMonster` -- một lời gọi khác, ở
lớp khác. Mã chạy suốt mà không ai gọi tới. Cách phát hiện nhanh: in một dòng ngay đầu hàm móc,
không thấy dòng nào là biết vá nhầm đường.

**Người dùng mô tả hiện tượng, không phải chẩn đoán.** Khi user nói "có hiện", tôi kết luận bong
bóng theo id quái đã chạy. Thực ra cái họ thấy là số sát thương bình thường của game, còn bong
bóng của tôi chưa từng hiện. Dấu hiệu đáng lẽ phải bắt được: họ tả "giống như lúc đánh quái" --
đó chính là mô tả của số sát thương. Phải tự dựng bằng chứng chứ đừng suy từ lời mô tả.

**Trần của một trường không nằm ở chỗ ghi nó, mà ở chỗ dùng nó.** Chuỗi phát của hiệu ứng
(`effect_data.running`) ghi bằng `writeByte` và client đọc bằng `readUnsignedByte`, nên tôi kết
luận trần là 255 và đẩy lên 248 nhịp cho mắt mở lâu hơn. Hiệu ứng đứng hình luôn. Cái chặn thật
nằm cách đó vài trăm dòng: con trỏ chạy chuỗi trong client là **một byte** (`private byte dD`), và
nó quay vòng bằng `if (dD > dB.length) dD = 0`. Dài quá 126 thì con trỏ đếm tới 127 rồi tràn xuống
-128, điều kiện quay vòng vĩnh viễn sai, `dB[-128]` ném lỗi — mà cả khối nằm trong
`catch (Exception) {}` **rỗng**, nên không log, không crash, chỉ lặng lẽ đứng ở khung đang vẽ dở.
Bài học: kiểm được "số này có lọt qua khâu truyền không" mới là nửa việc; phải grep tiếp xem bên
nhận **cất nó vào biến kiểu gì** và **so sánh với cái gì**. Và ba trần trong cùng một bản tin không
hề bằng nhau: x/y/w/h tới 255 (đọc không dấu), số khung ảnh tới 127 (đọc có dấu), chuỗi phát tới
126 (con trỏ là byte). Suy một con số ra cho cả ba là sai.

**Bộ kiểm tự viết chỉ mạnh bằng con số tôi điền vào nó.** `EffectCheck` của tôi báo ĐẠT cho đúng
cái bản đang hỏng, vì tôi điền trần 255 cho chuỗi phát — kiểm sai thì còn tệ hơn không kiểm, vì nó
cấp cho tôi sự tự tin để phát hành. Khi một bộ kiểm báo đạt mà thực tế hỏng, việc đầu tiên là sửa
bộ kiểm cho nó bắt được ca đó, rồi mới sửa dữ liệu — làm ngược lại thì lần sau vẫn lọt.

## Ô túi tự viết tay phải đủ khoá, thiếu là văng về màn hình chính

Nhét thẳng vật phẩm vào `players.bag` bằng SQL thì phải chép đúng hình dạng ô mà máy chủ
tự ghi ra. Ô đồ trang bị có **`upgrade`, `sys`, `options`** ngoài mười hai khoá chung
(`isLock, new, yen, isExtend, quantity, updated_at, expire, name, created_at, index, id,
isUpToUp`); `gems` thì có cũng được không có cũng được. Thiếu ba khoá kia, nhân vật vẫn
hiện ở màn chọn nhân vật (danh sách đó không đọc túi) nhưng bấm vào là **rơi thẳng về
menu trong chưa tới hai giây**, và **máy chủ không ghi lấy một dòng log nào**. Rất dễ đổ
oan cho dữ liệu ảnh hoặc mảnh.

Cách khoanh vùng rẻ nhất: chép đè túi của một nhân vật đang chạy tốt sang, vào lại. Vào
được thì lỗi nằm ở ô mình viết, không phải ở `nj_image`/`nj_part`/`item`.

## Tự kiểm bằng mắt: bàn phím đi đường AppleScript, chuột đi đường Robot

`osascript ... keystroke` và `key code` bắn phím vào MicroEmulator ngon lành, nhưng
`click at` của System Events là hộp đen không làm gì cả. Chuột phải dùng `java.awt.Robot`
trong một JVM riêng (`mouseMove` + `mousePress/Release`) — cùng quyền trợ năng đó nhưng
Robot bắn thật. Kèm theo:

- Đưa đúng cửa sổ lên trước bằng `AXRaise` trên `window "MicroEmulator"`, vì máy đang có
  hai tiến trình `java` (bảng quản lý của máy chủ và máy giả lập).
- F2 là phím mềm phải (OK/Thoát), F1 là phím mềm trái, Enter là phím bắn ở menu.
- Chụp `screencapture -x -t png` rồi `sips -c 365 552 --cropOffset 25 443` để cắt riêng
  khung máy giả lập, `sips -Z` để phóng to xem cho rõ hình nhân vật.
- Muốn xem một món trong túi mà khỏi cuộn: xếp nó lên `index` 0 trong cơ sở dữ liệu.

**Ảnh render phóng to không thay được việc xem ở cỡ thật.** Tôi dựng hai bản khung mắt mới cho
hiệu ứng danh hiệu, soi ở mức thu phóng 4 phóng thêm 4 lần nữa thì bản nào cũng đẹp, bản dựng
theo ảnh mẫu còn bám sát từng chi tiết. User vào game xem: "trông tệ quá", quay về bản cũ. Ở cỡ
thật vài chục pixel thì thứ thắng là hình đối xứng, mảng màu lớn, hoa văn to — đúng những thứ mà
lúc phóng to trông có vẻ thô. Quy trình đúng: đổi hình dạng thì xem trong game TRƯỚC, rồi mới
đầu tư vào chi tiết; đừng làm ngược lại rồi mới phát hiện cả hướng đi là sai.

**Sao lưu trước mỗi bước là thứ cứu được cả buổi.** Ba lần đổi hiệu ứng 231 là ba thư mục sao lưu
kèm README ghi đúng lệnh dựng lại. Nhờ vậy lúc user bảo quay về, việc hoàn tác là chép bốn file
cộng một câu UPDATE, không phải dựng lại từ đầu và không phải đoán tham số cũ là gì.

## Thứ tự vẽ bốn mảnh: thân đè lên đầu, không phải ngược lại

Đọc bytecode phần vẽ nhân vật của client thì thấy bốn lệnh vẽ lặp đúng một thứ tự: **mảnh phụ →
chân → đầu → thân**. Tức thân vẽ **sau** đầu, nên tay áo phủ lên nón chứ không phải nón nằm trên.

Hệ quả thật: bộ Kage nam có tay áo choàng trắng to, khi ra đòn thì che mất phần vải rủ hai bên
nón. Bộ nữ tay trần mảnh hơn nên không che. Đây là tính chất của client, bản gốc cũng vậy, không
sửa được bằng dữ liệu -- muốn đổi phải vá lại thứ tự vẽ, mà cái đó đụng mọi trang phục trong game.

Bài học cho tool: `tools/CanManh.java` lúc đầu vẽ chân → thân → đầu (đầu trên cùng), nên trong tool
nhìn đẹp mà vào game lại bị che. **Bộ xem trước mà sai thứ tự vẽ thì còn tệ hơn không có**, vì nó
cho cảm giác đã kiểm rồi. Nay đã sửa cho khớp.

## Nhét id vật phẩm mới vào nhân vật thì phải khởi động lại máy chủ trước

Thêm hàng vào bảng `item` rồi nhét id ấy vào `fashion`/`bag` của một nhân vật, mà máy chủ đang chạy
vẫn giữ bảng cũ trong bộ nhớ, thì chọn nhân vật đó là `itemTemplates.get(id)` tra ra ngoài mảng --
nạp nhân vật hỏng và client rớt thẳng, **không có dòng log nào**. Thứ tự đúng: chèn dữ liệu, tăng
`game.item.version`, khởi động lại, rồi mới trang bị cho nhân vật.

**Phép thay thế chuỗi trong script sửa mã phải có `assert`, vì trượt thì nó im lặng.** Tôi chèn
một nhánh mới vào `running()` bằng `str.replace`, nhưng mẫu tìm không khớp do có dòng chú thích
chen giữa -- Python trả về nguyên văn cũ, không báo gì. Ảnh dựng ra vẫn đúng nên nhìn qua tưởng
xong; chỉ có chuỗi phát là vẫn kiểu cũ, tức hiệu ứng chạy sai nhịp. Bắt được nhờ đọc con số in ra
lúc dựng chứ không phải nhờ nhìn ảnh. Từ nay mọi `replace` trong script sửa mã đều `assert` mẫu
tồn tại trước, và sau khi dựng thì đọc lại các con số trong log chứ không chỉ xem ảnh.

## nj_image mọc lên là nuốt mất id biểu tượng của vật phẩm khác

Biểu tượng vật phẩm có hai kiểu: id nhỏ thì cắt từ khung ảnh có sẵn trong client (có hàng
`nj_image`), id lớn thì client xin thẳng tệp `Small<id>.png` từ máy chủ (**không** có hàng). Hơn
bốn trăm vật phẩm của bản gốc dùng kiểu thứ hai, id rải từ 3187 trở lên.

`nj_image` bắt buộc liền mạch từ 1 nên thêm ảnh là chỉ nối được vào cuối. Thêm đủ nhiều thì mức
cao nhất vượt qua 3187, và mọi biểu tượng trong vùng đó **đột nhiên có hàng** -- client thôi xin
tệp, quay sang vẽ mảnh trang phục. Tệ hơn: bộ cắt ghi đè luôn `Small<id>.png` nên tệp gốc mất.

Lần này 67 vật phẩm hỏng biểu tượng trước khi phát hiện. Cứu được nhờ bản v4 trong `gpt` còn
nguyên bộ icon (`res/assets/icon/<cỡ>/`), ánh xạ cỡ trùng 1:1 với `Data/Img/Small/<cỡ>/`.

Hai việc rút ra:
- Biểu tượng tự thêm phải để ở vùng cao hẳn (đang dùng 30000+), không bao giờ đặt ngay trên mức
  cao nhất hiện thời của `nj_image`.
- Trước khi nối ảnh mới, phải dò xem dải id sắp dùng có đụng biểu tượng của ai không, và dời họ đi
  trước. `tools/them-trang-phuc.py` nay tự làm bước này.

**Chỉnh tham số ba lần mà vẫn cùng một loại sai thì lỗi nằm ở cách dựng hình, không ở con số.**
Tôi vẽ biểu tượng mây Akatsuki bằng phép hợp mấy hình tròn chồng nhau, rồi chỉnh cái đuôi năm
lần: dài quá, ngắn quá, chui vào thân, vống ra ngoài. User xem: "chả giống đám mây gì cả". Không
có bộ tham số nào chữa được, vì hợp hình tròn thì mọi chỗ lồi đều là cung tròn cùng kiểu -- muốn
ba bướu khác bán kính, đáy võng và đuôi cuộn ngược thì phải chép thẳng đường viền ra toạ độ. Đổi
cách xong là ra ngay lần đầu. Dấu hiệu để dừng sớm: mỗi vòng sửa lại hỏng ở một chỗ MỚI thay vì
chỗ cũ đỡ dần -- đó là hình đang bị kéo giữa các ràng buộc của chính phương pháp, không phải
đang hội tụ.

**Một giá trị đi qua nhiều chặng thì trần của nó là chặng HẸP NHẤT, không phải chặng mình vừa
đọc.** Tôi kiểm `MapService.addEffect` thấy `writeShort(id)` nên yên tâm dùng id hiệu ứng 301.
Vào game không hiện gì. Chặng thứ hai — `Service.sendImgEffect`, nơi gửi chính tấm ảnh — mới là
chỗ chặn: nó ghi `writeShort` cho client >= 239 và `writeByte` cho client cũ, mà client ở đây là
1.4.8 tức 148. Client được báo "vẽ hiệu ứng 301" nhưng nhận ảnh dán nhãn 45 (= 301 & 0xFF), đi
tìm 301 không thấy, và không vẽ gì. Không lỗi, không log. Quy tắc: trước khi chọn một con số mới,
grep HẾT các chặng nó đi qua (gửi lệnh, gửi tài nguyên, lưu trữ) rồi lấy trần nhỏ nhất; đọc đúng
một chặng rồi kết luận là cách chắc chắn để mất một vòng.

## Số đo của bộ chuẩn là điểm khởi đầu, không phải đích đến

Ba mảnh trang phục có bộ số để đối chiếu, đo bằng `CanManh ... --do` ở khung đứng:
**đáy đầu −22, đáy thân −12, đáy chân −10, tâm ngang ≈ 0**. Dịch một bộ mới về đúng mấy số này là
hết lệch trục và hết lún chân — đó là phần máy làm được.

Nhưng khớp số xong vẫn phải **nhìn chỗ nối**. Bộ Madara có ảnh đầu cao 30px trong khi bộ chuẩn chỉ
22px, và ảnh thân cao 17px thay vì 14px. Canh đáy theo đúng chuẩn thì cằm với vai lệch nhau ba
pixel, hở hẳn một khoảng nền giữa đầu và áo. Phải nâng riêng mảnh thân lên 3 mới liền.

Nói cách khác: số đo bắt được lỗi **trục và độ cao tổng thể**, còn chỗ nối giữa các mảnh thì phải
phóng to ra xem. Hai việc khác nhau, làm đủ cả hai mới xong.

## Xoá vật phẩm xong phải dọn túi nhân vật, và dọn sau khi khởi động lại

Máy chủ nạp nhân vật bằng cách tra mẫu vật phẩm theo id. Id đã xoá thì tra ra ngoài mảng, nạp
hỏng, client **rơi thẳng về màn hình chính và không có lấy một dòng log**. Nhìn như crash vô cớ.

Vấp hai lần trong một buổi. Lần thứ hai đau hơn: tôi có dọn túi, kịch bản còn in "đã gỡ", nhưng
dọn **trước** lúc khởi động lại nên máy chủ ghi đè lại từ bộ nhớ. Ba món chết quay về túi, và nhân
vật không vào được.

Thứ tự đúng: xoá bảng → khởi động lại → dọn túi → **đọc lại để xác nhận đã sạch**. Có
`tools/kiem-mon-chet.py` để dò, thêm `--don` thì gỡ luôn; nó cũng cảnh báo nếu đang có người trong
game, vì dọn lúc đó cũng bị ghi đè y hệt.

## Đừng vội tin danh sách "file không dịch lại được"

`Item.java` bị ghi là chỉ vá được bằng bytecode suốt nhiều phiên. Thật ra nó chỉ thiếu Lombok, mà
Lombok ở file này chỉ sinh accessor -- đọc ra bằng `javap` rồi viết thẳng vào nguồn là xong, mười
lăm phút. Cái giá của việc không kiểm lại: mọi thay đổi chỉ số item trước đó đều phải đi đường vòng.

Cách kiểm: `javac` thử với annotation giả, đếm lỗi. Hai lỗi thì gỡ được, hai trăm thì thôi.
Rồi so `javap -p` hai bản để chắc API không xê dịch -- đây mới là bước không được bỏ, vì thiếu một
accessor là lớp khác gọi vào sẽ `NoSuchMethodError` lúc chạy chứ không lỗi lúc dịch.

## `MAX(id) + 1` không phải id trống

Bộ thêm trang phục cấp id ảnh bằng `MAX(nj_image.id) + 1`. Nghe hiển nhiên mà sai: **`nj_part` được
phép trỏ tới id ảnh không có hàng nào trong `nj_image`** -- kho gốc có hơn hai nghìn tấm nằm dạng
tệp rời, client cứ theo id mà xin tệp. Lúc đó bảng cao nhất 3128 trong khi trên đĩa có tệp tới
260915. Bộ cấp phát lấy 3129 trở đi và `CatO` ghi đè thẳng lên ảnh của NPC Tashino cùng 12 mảnh cũ
khác -- 115 tấm. NPC hiện ra thành đống hình chồng chéo, mà mãi mới có người nhìn thấy.

Bài học rộng hơn: **"id lớn nhất trong bảng" và "id lớn nhất đang được dùng" là hai thứ khác nhau**
khi dữ liệu còn nằm ở nơi khác ngoài bảng. Trước khi cấp id, hỏi: ngoài bảng này ra, còn ai giữ id
nữa không? Ở đây là thư mục tệp.

Chỗ khó là không được né: `nj_image` bắt buộc liền mạch nên hàng mới chỉ có thể nối ngay sau hàng
cuối, không nhảy cóc qua id đang bận được. Nên phải **dọn chỗ** -- `idanh.don_duong` chép ảnh đang
nằm trong dải sang vùng cao rồi trỏ lại `nj_part` và `item.icon`. Vùng dời phải **dưới 32767** vì
`Server.loadParts` đọc id ảnh bằng `shortValue()`.

Và thứ tự bắt buộc: **dọn đường xong mới được cắt**. `CatO` ghi thẳng lên tệp, cắt trước thì tệp
gốc đã mất, còn gì để dọn. Bộ chặn biểu tượng cũ đặt SAU lúc cắt nên nó đi cứu bằng bản đã chết --
đó chính là lý do lần trước phải lấy lại từ kho v4.

## Người dùng bảo xoá thứ đang hỏng: kiểm xem có phải mình làm hỏng không

"NPC này lỗi, xoá đi, tôi nghĩ nó k work đâu" -- nhưng NPC ấy vốn chạy tốt, chính ta ghi đè mất
ảnh của nó vài giờ trước. Xoá đi là mất một NPC có chức năng (luyện bí kíp) và giấu luôn cái lỗi
đang âm thầm phá 12 mảnh khác.

Cách kiểm nhanh trước khi xoá bất cứ thứ gì đang "hỏng": xem thời điểm sửa lần cuối của tệp liên
quan, và so với kho gốc. Ở đây `ls -l` cho ra 13:12 và 13:24 cùng ngày -- đúng giờ chạy bộ thêm
trang phục. Ba mươi giây để hỏi, đổi lại tránh được một cái xoá nhầm và tìm ra lỗi gốc.

## javac mặc định nhắm JDK của máy đang dựng, không phải máy sẽ chạy

`mod.sh` biên dịch `mod-src/*.java` không kèm `--release`, nên lớp ra mang major 61 (Java 17) trong
khi 183 lớp còn lại của client là major 50 (Java 6). Máy bạn bè chạy JRE 8 đi kèm Micro_AngelChip
(nhận ra qua `jre/lib/rt.jar`, không có `lib/modules`), nạp major 61 là chết ngay.

Chỗ khiến nó khó lần ra: **bản 2.1 vẫn chạy được** dù cũng có hai lớp major 61. Vì `Dan` chỉ nạp
khi gõ đúng lệnh chat, `TanSat` chỉ nạp khi mở menu Tàn sát -- bạn bè không đụng tới thì không bao
giờ nạp. Còn `SoNgan` thêm ở 2.2 thì gọi mỗi lần vẽ số sát thương, nạp ngay lúc vào game. Nên lỗi
"phiên bản Java" lại đội lốt "bản 2.2 hỏng".

Bài học: khi một bản chạy còn bản kia không, **so mục lục hai jar** trước đã. Ở đây khác đúng một
mục (`SoNgan.class`), và từ đó lần ra ngay. Và câu "chỉ chạy được tới 2.1" của người dùng đáng giá
hơn cả buổi đoán mò -- hỏi "bản nào là bản cuối cùng còn chạy" nên là câu hỏi đầu tiên.

Sửa: `javac --release 7`. Nhắm thấp cho khớp phần còn lại của client, chứ không nhắm theo JDK sẵn có.

## 23/08 - Hỏi "tinh luyện" hay "re-roll" trước khi viết
Người dùng nói "tinh luyện bí kíp liên tục" - tôi hiểu thành cộng dồn chỉ số, làm xong mới biết
cái cần là **quay lại dàn chỉ số** (mục "Luyện bí kíp" của NPC Tashino). Hai việc khác hẳn nhau:
một cái cộng thêm vào chỉ số đang có, một cái đổi hẳn sang dàn khác.
**Cách tránh:** với tính năng chép từ trong game, đọc đúng đoạn mã của NPC trước khi viết, rồi nói
lại một câu "cái này làm X, đúng ý chưa" - rẻ hơn viết xong mới sửa.

## 23/08 - "+24" không phải do đường phát đồ của web
Truy lỗi "trang bị 2 vẫn +24" mà chỉ nhìn GiveItem là đi sai đường: mức +24 sinh ở
`VuaHungBoost.apply` (gán 12 rồi next(12) cộng dồn), không phải `getUpMax()` (trần 16).
**Cách tránh:** trước khi đoán, quét thẳng dữ liệu thật - so từng món trong túi/ô mặc của mọi nhân
vật với luật nâng cấp - rồi mới lần ngược về nơi sinh ra con số đó.

## 23/08 - Chép tính năng thì chép cả GIỚI HẠN của nó
Bản tinh luyện bí kíp trên web chép đúng công thức cộng chỉ số nhưng bỏ quên hai thứ chỉ có ở
đoạn mã trong game: trần độ tinh luyện 9 (`cap >= 9` Tashino từ chối) và việc vòng quay dựng món
MỚI nên bí kíp danh hiệu lấy lại được bộ chỉ số riêng. Thiếu cái đầu thì 50 lần bấm cho chỉ số
gấp 11 lần; thiếu cái sau thì re-roll xoá sạch chỉ số của Đệ Nhất/Nhị/Tam/Tứ.
**Cách tránh:** khi chép một nhánh trong game, đọc HẾT nhánh đó từ điều kiện vào tới lúc kết thúc
(chặn cấp, chặn tiền, chặn ô trống, món mới hay món cũ) rồi mới quyết bỏ cái nào - và ghi rõ cái
nào cố ý bỏ.

## Vá client NSO: đừng để ASM ghi lại cả lớp

MicroEmulator tự chạy ASM lên **từng lớp** lúc nạp jar. Lớp nào do ASM 3.1 của mình ghi lại thì nó
bỏ nguyên cả jar, báo `NullPointerException: Cannot read the array length because "byteCode" is null`
ở `MIDletClassLoader.findClass` -- nhìn như jar hỏng, thật ra chỉ một lớp làm nó nghẹn.

Đo được: jar sao chép thuần nạp 59 lớp, jar có một lớp bị ASM ghi lại nạp **0** lớp.

Nên với client NSO, vá phải **giữ nguyên kích thước lớp**:
- đổi số hiệu hằng số trong `ldc2_w` (2 byte) -- xem `tools/ha-ngu-danh.py`
- thay lệnh bằng lệnh cùng độ dài -- xem `tools/OpenChatCommands.java`

Cách vá bằng ASM chỉ dùng được cho client X1 (major 52), không dùng được cho NSO qua MicroEmulator.

## Nhịp đánh chậm: đo trước, đoán sau

Ba lần tôi kết luận sai vì đoán:
1. "Client có sàn 0,56s" -- sai. Client tuân thủ đúng con số máy chủ gửi.
2. "max_fight không ảnh hưởng nhịp đánh" -- sai. Hạ 7 xuống 2 làm trung vị tụt 449 -> 245ms.
3. "Khoảng trống là do chọn lại mục tiêu" -- sai. Đánh boss không chết vẫn y nguyên.

Bộ đo đúng: log ở `Skill.danhDau` (đòn được nhận) **cộng với** log ở `Skill.isCooldown` (đòn bị
chặn). Thiếu vế thứ hai thì "client chưa bắn" và "client bắn rồi nhưng bị chặn" nhìn giống hệt
nhau -- mà hai thứ ấy đòi hai cách sửa ngược nhau.

## Đi từ máy chủ sang client qua thứ tự gói tin

Tên lớp/hàm/trường trong client NSO là 200+ ký tự I/l, không đọc được. Nhưng THỨ TỰ đọc gói buộc
phải khớp từng nhịp với thứ tự ghi bên máy chủ. Dùng chuỗi read* làm vân tay là đi thẳng được từ
`Server.setSkill` tới hàm đọc bên client, rồi ra tên trường. Xem `tools/TimDocGoi.java`.

## 23/08 - "static final int" bị nhúng vào lớp gọi, nap.sh không thấy
Đổi `PhaTran.TOI_DA` từ 5 xuống 3, nạp xong máy chủ vẫn kẹp ở 5. Lý do: `static final int x = 3`
là hằng số biên dịch, javac nhúng thẳng con số vào MỌI lớp gọi tới, mà `tools/nap.sh` chỉ dịch lại
file nào mới hơn jar - `PhaTran.class` mang số mới còn `Char.class`/`CharAdmin.class` vẫn mang số cũ.
Máy chủ chạy hai luật cùng lúc, không báo lỗi gì.
**Cách tránh:** hằng số có thể đổi thì gán trong khối `static { }` chứ đừng gán ngay chỗ khai báo -
javac buộc phải đọc trường lúc chạy. Hoặc khi sửa một hằng số, `touch` mọi file có nhắc tên nó
trước khi chạy nap.sh.

## Vẽ cho sprite nhỏ: chi tiết nằm TRONG đường bao thì chết, nằm NGOÀI thì sống

Susanoo (hiệu ứng 255 + icon 6502). Chủ dự án chỉ ra Susanoo của Itachi là mặt thiên cẩu mũi dài,
KHÔNG sừng -- tôi vẽ sừng là sai nguyên tác. Sửa đúng nguyên tác xong thì cả hai bản đều tệ hơn,
và cuối cùng quay lại bản có sừng.

**Vì sao:** hình cao 96 điểm ảnh, cái đầu chỉ chiếm chừng 15. Ở cỡ đó cái mũi -- nằm gọn giữa
khuôn mặt, cùng tông màu với mặt -- nhoè mất hoàn toàn. Cặp sừng thì nhô ra NGOÀI đường bao của
đầu, in bóng lên nền, nên thu nhỏ cỡ nào cũng còn. Đã dựng thử đủ 4 kiểu tả mũi (chính diện ngắn,
chính diện gợi bóng, quay 3/4, quay nghiêng hẳn) rồi so ở cỡ thật: cả 4 gần như y hệt nhau.

**Rút ra:**
- Trước khi đầu tư vào một chi tiết, hỏi: ở cỡ HIỂN THỊ THẬT nó có nằm ngoài đường bao không?
  Nằm trong thì dù vẽ đẹp mấy cũng không ai thấy.
- Góc chính diện là góc tệ nhất để tả thứ chìa về phía người xem: nó bị nén gần hết chiều dài,
  vẽ dài ra chỉ thành thõng xuống. Nhưng quay đầu đi thì bóng đầu lệch, thu nhỏ lại nhìn như méo.
- **Luôn so ở cỡ thật trước khi chốt.** Tôi so ở bản phóng 4x nên ba lần liên tiếp tưởng đã ổn.
  Từ đó mọi công cụ vẽ đều tự ghi kèm ảnh `.temp.png` có cả hàng cỡ thật.
- Đúng nguyên tác mà không nhìn ra thì thua sai nguyên tác mà nhận ra được. Hỏi chủ dự án chứ
  đừng tự quyết bằng lý lẽ "cho giống bản gốc".

**Cách quay lui:** đừng chép đè ảnh cũ -- thêm hẳn một kiểu trong công cụ (`-Dmui=0`) rồi đối
chiếu md5 với ảnh sao lưu. Chép đè thì lần sau ai chạy công cụ là bản cũ biến mất.
