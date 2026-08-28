/**
 * Rút gọn những con số dài thành dạng K / M / B khi client vẽ chữ.
 *
 * Lớp font của client vẽ từng ký tự bằng hai lời gọi: String.length() để biết vẽ bao nhiêu ký tự,
 * và String.charAt(i) để lấy ký tự thứ i. Cả hai đều được trỏ sang đây, nên chỉ cần trả về nội
 * dung của chuỗi đã rút gọn là mọi chỗ vẽ số đều đổi theo, không phải đi tìm từng nơi gọi.
 *
 * Điều kiện đổi rất chặt, cố ý: chuỗi phải TOÀN chữ số và giá trị từ 100.000 trở lên. Nhờ vậy
 *   - dòng chỉ số món đồ ("Tấn công: +1234567") có chữ cái nên giữ nguyên, hiện đủ số
 *   - số lượng vật phẩm, cấp độ, giá tiền nhỏ đều dưới ngưỡng nên giữ nguyên
 *   - sát thương và kinh nghiệm, vốn là số trần và rất lớn, mới bị rút gọn
 *
 * Không dùng phép nối chuỗi bằng dấu cộng: javac hiện nay biên dịch nó thành invokedynamic, thứ
 * mà máy ảo J2ME không hiểu. Mọi chỗ ghép đều qua StringBuffer.
 */
public final class SoNgan {

    /**
     * Dưới mức này thì hiện đủ số.
     *
     * Đổi một số ở đây là đổi ngưỡng cho toàn bộ: sát thương, kinh nghiệm, HP/MP đều dùng chung.
     */
    private static final long NGUONG = 100000L;

    /** Nhớ lần gần nhất: font gọi charAt liên tiếp cho từng ký tự của cùng một chuỗi. */
    private static String nguon;
    private static String gon;

    private SoNgan() {
    }

    /**
     * Thay cho StringBuffer.append(long) ở đúng câu thông báo nhận kinh nghiệm.
     *
     * Câu đó do client ghép nên có cả chữ lẫn số, không lọt bộ lọc "chuỗi số trần" ở trên. Chặn
     * ngay lúc ghép thì rút gọn được con số mà phần chữ vẫn nguyên.
     */
    public static StringBuffer them(StringBuffer sb, long v) {
        return sb.append(tinh(String.valueOf(v)));
    }

    /** Thay cho StringBuffer.append(int) ở dòng thông tin quái: "Id:.. tên: hp/maxhp". */
    public static StringBuffer themI(StringBuffer sb, int v) {
        return sb.append(tinhSo(String.valueOf(v)));
    }

    /** Thay cho String.length(). */
    public static int len(String s) {
        return can(s).length();
    }

    /** Thay cho String.charAt(int). */
    public static char charAt(String s, int i) {
        String t = can(s);
        return i < t.length() ? t.charAt(i) : ' ';
    }

    private static String can(String s) {
        if (s == null) {
            return "";
        }
        if (s == nguon) {
            return gon;
        }
        String r = tinh(s);
        nguon = s;
        gon = r;
        return r;
    }

    private static String tinh(String s) {
        // Thanh máu quái và thanh kinh nghiệm vẽ dạng "đang có/tối đa". Rút gọn hai vế riêng,
        // mỗi vế vẫn phải tự vượt ngưỡng. Chuỗi có chữ cái thì hai vế sẽ không toàn số nên tự
        // rơi khỏi nhánh này -- dòng chỉ số món đồ vì thế vẫn nguyên.
        int vach = s.indexOf('/');
        if (vach > 0 && vach < s.length() - 1 && s.indexOf('/', vach + 1) < 0) {
            String trai = s.substring(0, vach), phai = s.substring(vach + 1);
            String a = tinhSo(trai), b = tinhSo(phai);
            if (a == trai && b == phai) {
                return s;
            }
            StringBuffer sb = new StringBuffer();
            sb.append(a);
            sb.append('/');
            sb.append(b);
            return sb.toString();
        }
        return tinhSo(s);
    }

    private static String tinhSo(String s) {
        int n = s.length();
        // Chặn trên để khỏi tràn kiểu long; chặn dưới để bỏ qua chuỗi quá ngắn cho mọi ngưỡng.
        if (n < 4 || n > 19) {
            return s;
        }
        // Số sát thương và số hồi phục có dấu đứng trước ("-12345", "+3000"); giữ nguyên dấu đó
        // rồi rút gọn phần số phía sau. Font chữ số có sẵn ô cho cả '+' lẫn '-'.
        int dau = 0;
        char kyTuDau = s.charAt(0);
        if (kyTuDau == '+' || kyTuDau == '-') {
            dau = 1;
            if (n - 1 < 4) {
                return s;
            }
        }
        long v = 0;
        for (int i = dau; i < n; i++) {
            char c = s.charAt(i);
            if (c < '0' || c > '9') {
                return s;              // có ký tự không phải số -> đây là chữ, không đụng vào
            }
            v = v * 10 + (c - '0');
        }
        if (v < NGUONG) {
            return s;
        }
        long chia;
        char hau;
        if (v >= 1000000000L) {
            chia = 1000000000L;
            hau = 'B';
        } else if (v >= 1000000L) {
            chia = 1000000L;
            hau = 'M';
        } else {
            chia = 1000L;
            hau = 'K';
        }
        long nguyen = v / chia;
        long le = (v / (chia / 10)) % 10;
        StringBuffer sb = new StringBuffer();
        if (dau == 1) {
            sb.append(kyTuDau);
        }
        sb.append(nguyen);
        sb.append('.');
        sb.append(le);
        sb.append(hau);
        return sb.toString();
    }
}
