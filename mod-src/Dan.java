/**
 * Bật "Tự Dùng" cho mấy viên đan bằng một lệnh chat, thay vì mở hành trang chỉ vào từng món.
 *
 * Client vốn đã có sẵn tính năng tự dùng: nó giữ một danh sách id vật phẩm trong bộ nhớ và cứ
 * hai giây lại quét danh sách đó rồi dùng món tương ứng. Cái nó không làm là NHỚ danh sách --
 * danh sách được tạo rỗng mỗi lần mở game, nên lần nào cũng phải bật lại bằng tay.
 *
 * Lớp này chỉ gọi đúng hàm mà nút "Tự Dùng" trong menu vật phẩm vẫn gọi, cho cả bốn viên đan
 * một lượt. Không tự viết lại phần dùng đồ, nên hành vi giống hệt bấm tay.
 *
 * Tên lớp và tên hàm dài ngoằng là do bản client đã bị làm rối; đây là tên thật của chúng.
 */
public final class Dan {

    /** Bốn viên đan buff 10 phút. 605 Tiến Hoá Đan cố tình không có ở đây: nó để tiến hoá thần thú. */
    private static final int[] IDS = { 275, 276, 277, 278 };

    private Dan() {
    }

    /**
     * Nhánh lệnh chat được vá để gọi thẳng vào đây; chữ ký ()V phải khớp lệnh cũ bị thay.
     *
     * Dòng thông báo cuối hàm là bản gốc, đã bỏ đi ở 1.1 rồi trả lại ở 1.2. Lý do trả lại: bỏ nó
     * không chữa được lỗi treo trên KEmulator, mà lại làm mất dấu hiệu duy nhất cho biết lệnh đã
     * chạy -- người chơi gõ xong không thấy gì nên tưởng lệnh hỏng.
     *
     * Lỗi treo trên KEmulator vẫn còn đó, chưa tìm ra nguyên nhân thật; xử lý sau, đừng bỏ dòng
     * này lần nữa mà không có bằng chứng nó chính là thủ phạm.
     */
    public static void fill() {
        try {
            for (int i = 0; i < IDS.length; i++) {
                IIllIIlllIllIIllIIIlllIIlllIIllIIllIIlIIllIIllIllIIlllIlllIIlIIllIIllIlllIllIIllIIIlllIllIlllIIlIIlllIllIIIllIlllIIIlIllIIIllIIlIIllIIlIIIllIIIllIIIllIIIllIIIllIlIIllIIIlIIllIIllIllIIlllIIlllIIIllIIlIlIIllIlIIllIIlIIlIIlllIllIIlllIIllIIIl.IIllIllIIllIlIIlIlIIlllIllIIllIIllIlIIllIIIllIIIllIIllIIIlIllIIlIIlllIllIIlIllIllIIlIIlIlIIIlIllIIllIlllIllIIIllIIlIIlllIllIIlIIIlIIllIIllIIlIlIIlIllIIlllIlIIlllIIlIlIIlIIlIlllIIlIIIlIIllIllIlllIIlllIIIlIIllIIIlll(IDS[i]);
            }
            IIllIlIIlllIlllIllIllIlIIIlllIlIIlIIIllIlllIIllIllIIllIIllIIIllIlllIllIIIlllIIlIlllIIlIIlIIlIIllIIlIlIIlIIllIIlIlIllIIlIlIllIIlIIllIIlIllIlIlIlllIllIIIllIlIIlllIIlIllIlIIlIlIIIlIllIIlllIIlIllIIlIIlllIIlll.IIlIlllIIllIllIlllIIlIlIllIIlIIllIIllIIIllIlIIlllIIIllIIllIIlllIlllIIllIlllIllIIIlIIllIIllIIlllIIIllIIIlIIllIllIIlIIlIIlllIlIIlIllIlllIIlllIIIlIIIllIIllIIlIIIllIlllIIlIlIlllIIlIIllIIlIllIIlIIlllIIIlllIIlllIIlllIllIIlIllIIIlIIll("Da bat Tu Dung: 275 276 277 278");
        } catch (Throwable t) {
            // Lệnh chat hỏng thì thôi, không được phép làm sập game.
        }
    }
}
