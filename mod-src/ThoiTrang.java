/**
 * Áp mảng thời trang mà máy chủ gửi xuống lên hình một nhân vật.
 *
 * Client gốc đọc đủ mười số này chỉ để giữ nhịp gói tin rồi bỏ đi -- không có trường nào để chứa,
 * cũng không có chỗ nào vẽ. Nên trang bị 2 xưa nay cộng chỉ số thì được mà không đổi được hình.
 *
 * Ba số đầu là tóc, thân, chân. Ở đây chúng được hiểu thẳng là chỉ số mảnh trong bảng nj_part --
 * đúng bảng mà client vốn đã biết vẽ cho trang bị thường -- nên máy chủ trỏ vào mảnh nào là ra
 * mảnh đó, thêm bộ trang phục mới không phải phát hành lại client.
 *
 * Số âm nghĩa là "không cải trang", giữ nguyên đồ đang mặc. Máy chủ chịu trách nhiệm chỉ gửi số
 * dương cho những bộ đã dựng mảnh tử tế.
 *
 * Nhân vật phải truyền vào chứ không tự lấy nhân vật của mình: trong ba chỗ gọi thì có một chỗ
 * nằm ở hàm tĩnh nhận nhân vật khác qua tham số, lấy nhầm là đồ người khác đắp lên người mình.
 */
public final class ThoiTrang {

    private ThoiTrang() {
    }

    public static void ap(IllIlllIIllIIlIIlIlIIIllIIllIIlllIIIlIIlIIlllIIlIlIIIlIlIIIlIIllIlllIIIlllIIllIllIllIIlllIlIllIIlIlIIIlIlIlllIIIlIlllIIlIIllIIlIIllIllIIllIlllIIIllIllIlIIIllIIllIIllIIlIIIlIIlIlIIIllIIllIllIlllIIIllIIIllIIIlllIIlIIIlIlll ai, short[] tt) {
        if (ai == null || tt == null || tt.length < 3) {
            return;
        }
        if (tt[0] >= 0) {
            ai.s = tt[0];          // tóc / đầu
        }
        if (tt[1] >= 0) {
            ai.u = tt[1];          // thân
        }
        if (tt[2] >= 0) {
            ai.t = tt[2];          // chân
        }
    }
}
