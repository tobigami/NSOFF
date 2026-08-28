package com.nsoz.skill;

/**
 * Phá trần kỹ năng: dùng sách để đưa một chiêu vượt qua trần thường, mỗi chiêu tối đa 3 lần.
 *
 * Sách vừa nới trần vừa CỘNG THẲNG CẤP, không tốn điểm kỹ năng. Bể điểm ở cấp 130 chỉ có 121 mà
 * max hết 15 chiêu cần 146 -- bắt trả thêm điểm nữa thì phá trần thành khoản lỗ chứ không phải
 * phần thưởng.
 *
 * Vì sao trần phải chốt ở hai chỗ: `skill_template.max_point` được gửi thẳng xuống client và là
 * MỘT CON SỐ DÙNG CHUNG cho cả máy chủ, không thể khác nhau theo từng người. Trong bảng nó được
 * đặt bằng TRẦN CAO NHẤT (gốc + 3); để bằng trần gốc thì lúc ai đó phá trần lên cấp 7, cấp hiện
 * tại vượt cấp tối đa và khung thông tin trong game vỡ. Trần thật của từng người tính ở đây.
 *
 * Số lần đã phá nằm trong cột `data` của bảng players, khoá "phaTran", dạng chuỗi gọn
 * "mãChiêu:sốLần,...". Chọn chuỗi thay vì JSON lồng để soi cơ sở dữ liệu bằng mắt vẫn đọc được.
 */
public final class PhaTran {

    /**
     * Số lần phá trần tối đa cho mỗi chiêu.
     *
     * Ba lần, và bảng `skill` chỉ có bản tới cấp gốc + 3. Đổi số này thì PHẢI thêm/bớt bản cấp
     * tương ứng trong bảng `skill` và sửa `skill_template.max_point` cho khớp -- nhân vật đang ở
     * cấp không còn bản trong bảng sẽ hỏng lúc đăng nhập.
     */
    public static final int TOI_DA;

    static {
        // Gán trong khối static chứ KHÔNG viết "= 3" ngay chỗ khai báo -- có lý do.
        //
        // "static final int x = 3" là hằng số biên dịch: javac nhúng thẳng số 3 vào MỌI lớp gọi
        // tới, và tools/nap.sh chỉ dịch lại file nào mới hơn jar. Sửa số ở đây rồi nạp, lớp này
        // mang số mới còn Char và CharAdmin vẫn mang số cũ đã nhúng -- máy chủ chạy hai luật khác
        // nhau cùng lúc mà không báo gì. Gán trong khối static thì javac buộc phải đọc trường lúc
        // chạy, nên đổi một chỗ là cả máy chủ đổi theo.
        TOI_DA = 3;
    }

    /**
     * {mã chiêu, trần gốc, mã sách phá trần}.
     *
     * Trần gốc ghi ở đây chứ không đọc từ `skill_template.max_point`, vì cột ấy đã bị nâng sẵn
     * lên mức cao nhất (xem phần trên).
     */
    private static final int[][] BANG = {
        // Trần gốc HẠ TỪ 6 XUỐNG 1 (26/08). Chiêu 120 của năm lớp kia đều max_point = 1;
        // riêng tiêu để 6 nên cộng thẳng lên 6 rồi phá trần tiếp lên 9 -- lệch hẳn.
        // Giờ 1 + tối đa 3 lần phá = trần thật 4, và skill_template.max_point phải là 4.
        {86, 1, 1265},   // Tiêu Hoả Phi Long -- phi tiêu, mở ở cấp 120
    };

    private PhaTran() {
    }

    /** Trần gốc của chiêu, hoặc -1 nếu chiêu này không nằm trong cơ chế phá trần. */
    public static int tranGoc(int maChieu) {
        for (int[] d : BANG) {
            if (d[0] == maChieu) {
                return d[1];
            }
        }
        return -1;
    }

    /** Mã quyển sách phá trần của chiêu, hoặc -1. */
    public static int sachCuaChieu(int maChieu) {
        for (int[] d : BANG) {
            if (d[0] == maChieu) {
                return d[2];
            }
        }
        return -1;
    }

    /** Mã chiêu mà quyển sách này phá trần, hoặc -1 nếu vật phẩm không phải sách phá trần. */
    public static int chieuCuaSach(int maSach) {
        for (int[] d : BANG) {
            if (d[2] == maSach) {
                return d[0];
            }
        }
        return -1;
    }

    /** Trần THẬT của chiêu với một người, tính cả số lần đã phá. */
    public static int tranThat(int maChieu, int tranBang, String luu) {
        int goc = tranGoc(maChieu);
        if (goc < 0) {
            return tranBang;
        }
        // Không kẹp theo tranBang: phần phá trần nằm NGOÀI trần gốc.
        return goc + Math.min(doc(luu, maChieu), TOI_DA);
    }

    /** Đọc số lần đã phá của một chiêu từ chuỗi đã lưu. */
    public static int doc(String luu, int maChieu) {
        if (luu == null || luu.isEmpty()) {
            return 0;
        }
        for (String phan : luu.split(",")) {
            int v = phan.indexOf(':');
            if (v > 0) {
                try {
                    if (Integer.parseInt(phan.substring(0, v).trim()) == maChieu) {
                        return Integer.parseInt(phan.substring(v + 1).trim());
                    }
                } catch (NumberFormatException ignore) {
                    // một mục hỏng không được phép làm mất những mục còn lại
                }
            }
        }
        return 0;
    }

    /** Ghi số lần phá mới vào chuỗi, trả về chuỗi đã cập nhật. */
    public static String ghi(String luu, int maChieu, int soLan) {
        StringBuilder sb = new StringBuilder();
        boolean thay = false;
        if (luu != null && !luu.isEmpty()) {
            for (String phan : luu.split(",")) {
                int v = phan.indexOf(':');
                if (v <= 0) {
                    continue;
                }
                int ma;
                try {
                    ma = Integer.parseInt(phan.substring(0, v).trim());
                } catch (NumberFormatException ignore) {
                    continue;
                }
                if (sb.length() > 0) {
                    sb.append(',');
                }
                if (ma == maChieu) {
                    sb.append(ma).append(':').append(soLan);
                    thay = true;
                } else {
                    sb.append(phan.trim());
                }
            }
        }
        if (!thay) {
            if (sb.length() > 0) {
                sb.append(',');
            }
            sb.append(maChieu).append(':').append(soLan);
        }
        return sb.toString();
    }
}
