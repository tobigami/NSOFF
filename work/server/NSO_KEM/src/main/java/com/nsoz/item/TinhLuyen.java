package com.nsoz.item;

import com.nsoz.option.ItemOption;

/**
 * Luật tinh luyện trang bị -- một bản duy nhất cho cả đường trong game lẫn bảng quản trị.
 *
 * Mọi con số ở đây bê nguyên từ {@code Char.tinhLuyen} (nhánh {@code CMD.TINH_LUYEN}), nơi trước
 * đây chúng nằm rải trong một chuỗi if-else dài. Tách ra vì có hai nơi cần cùng phép tính: NPC
 * trong game và nút tinh luyện trên bảng web. Chép làm hai bản thì sớm muộn lệch nhau, mà lệch ở
 * đây nghĩa là món tinh luyện qua web mạnh yếu khác món tinh luyện trong game -- người chơi không
 * có cách nào biết vì sao.
 *
 * Máy khách cũng giữ một bản sao của ba bảng phí/tỉ lệ/số thạch để vẽ dòng "Độ tinh luyện: 1
 * (thành công 60%) -- Cần 150.000 Yên, 3 tử tinh thạch sơ". Sửa số ở đây thì phải vá cả client,
 * nếu không màn hình hiện một đằng máy chủ trừ một nẻo.
 */
public final class TinhLuyen {

    /** Độ tinh luyện cao nhất. Ba bảng dưới đều dài đúng 9, chỉ số là ĐỘ ĐANG CÓ (0-8). */
    public static final int TRAN = 9;

    /** Tỉ lệ thành công theo độ đang có, phần trăm. */
    public static final int[] TI_LE = {60, 45, 34, 26, 20, 15, 11, 8, 6};

    /** Phí mỗi lần, tính bằng yên (thiếu yên thì trừ sang xu). */
    public static final int[] PHI = {150000, 247500, 408375, 673819, 1111801, 2056832, 4010822,
        7420021, 12243035};

    /** Số tử tinh thạch mỗi lần. */
    public static final int[] SO_THACH = {3, 5, 9, 4, 7, 10, 5, 7, 9};

    /** Mã ba loại tử tinh thạch: sơ cấp, trung cấp, cao cấp. */
    public static final int THACH_SO = 455, THACH_TRUNG = 456, THACH_CAO = 457;

    /** Mức cộng cho từng chỉ số theo độ đang có. Khoá là mã chỉ số. */
    private static final int[][] BANG = {
        {94, 10, 10, 10, 20, 20, 30, 40, 50, 60},
        {86, 25, 30, 35, 40, 50, 60, 80, 115, 165},
        {87, 50, 60, 70, 90, 130, 180, 250, 330, 500},
        {88, 50, 70, 100, 140, 190, 250, 320, 400, 500},
        {89, 50, 70, 100, 140, 190, 250, 320, 400, 500},
        {90, 50, 70, 100, 140, 190, 250, 320, 400, 500},
        {92, 5, 5, 5, 5, 5, 5, 10, 10, 20},
        {95, 5, 5, 5, 5, 5, 5, 10, 10, 15},
        {96, 5, 5, 5, 5, 5, 5, 10, 10, 15},
        {97, 5, 5, 5, 5, 5, 5, 10, 10, 15},
        {82, 40, 60, 80, 100, 140, 220, 300, 420, 590},
        {83, 40, 60, 80, 100, 140, 220, 300, 420, 590},
        {84, 25, 30, 35, 40, 50, 60, 80, 115, 165},
        {79, 1, 2, 2, 2, 2, 2, 3, 3, 4},
        {81, 1, 2, 2, 2, 2, 2, 3, 3, 4},
        {80, 5, 5, 5, 5, 10, 10, 15, 15, 20},
        {91, 5, 5, 5, 5, 5, 5, 10, 10, 15},
    };

    private TinhLuyen() {
    }

    /**
     * Món này có thuộc nhóm tinh luyện được không.
     *
     * Chép đúng bộ lọc trong game: quần áo, trang sức (trừ 1117), vũ khí, áo choàng (loại 12) và
     * thú cưỡi trừ loại 33. Có thuộc nhóm vẫn chưa đủ -- còn phải mang sẵn dòng "Độ tinh luyện",
     * thứ chỉ có được sau khi dịch chuyển trang bị.
     */
    public static boolean nhomTinhLuyen(ItemTemplate t) {
        if (t == null) {
            return false;
        }
        return t.isTypeClothe()
                || (t.isTypeAdorn() && t.id != 1117)
                || t.isTypeWeapon()
                || t.type == 12
                || (t.isTypeMount() && t.type != 33);
    }

    /** Dòng "Độ tinh luyện" của món, hoặc null nếu món chưa có. */
    public static ItemOption doTinhLuyen(Item it) {
        if (it == null || it.options == null) {
            return null;
        }
        for (ItemOption o : it.options) {
            if (o != null && o.optionTemplate != null && o.optionTemplate.id == 85) {
                return o;
            }
        }
        return null;
    }

    /** Tinh luyện được ngay bây giờ không: đúng nhóm, có dòng độ tinh luyện, và chưa kịch trần. */
    public static boolean sanSang(Item it) {
        ItemOption d = doTinhLuyen(it);
        return it != null && nhomTinhLuyen(it.template) && d != null && d.param < TRAN;
    }

    /** Mức cộng của một chỉ số ở độ tinh luyện đang có; 0 nếu chỉ số không nằm trong bảng. */
    public static int mucCong(int maChiSo, int cap) {
        if (cap < 0 || cap >= TRAN) {
            return 0;
        }
        for (int[] d : BANG) {
            if (d[0] == maChiSo) {
                return d[1 + cap];
            }
        }
        return 0;
    }

    /** Loại thạch cần ở độ tinh luyện đang có. */
    public static int maThach(int cap) {
        return cap >= 6 ? THACH_CAO : (cap >= 3 ? THACH_TRUNG : THACH_SO);
    }

    /**
     * Cộng một bậc tinh luyện cho món. Trả về false nếu món không tinh luyện được hoặc đã kịch trần.
     *
     * Chỉ đụng vào chỉ số loại 8 -- đúng như trong game. Dòng "Độ tinh luyện" tự tăng 1 ở cuối.
     */
    public static boolean motLan(Item it) {
        ItemOption cap = doTinhLuyen(it);
        if (it == null || !nhomTinhLuyen(it.template) || cap == null || cap.param >= TRAN) {
            return false;
        }
        int muc = cap.param;
        for (ItemOption o : it.options) {
            if (o == null || o.optionTemplate == null || o.optionTemplate.type != 8
                    || o.optionTemplate.id == 85) {
                continue;
            }
            o.param += mucCong(o.optionTemplate.id, muc);
        }
        cap.param++;
        return true;
    }
}
