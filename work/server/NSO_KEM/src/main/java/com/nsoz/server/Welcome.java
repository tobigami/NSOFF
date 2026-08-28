package com.nsoz.server;

import com.nsoz.event.Event;
import com.nsoz.model.Char;
import com.nsoz.util.NinjaUtils;

/**
 * Soạn bảng thông báo hiện ra ngay sau khi chọn nhân vật.
 *
 * Trước đây chỗ này chỉ đọc đúng một dòng chữ trong bảng options (khoá "thongbaogame") rồi hiện
 * nguyên si, nên nó đứng yên mãi một nội dung. Ở đây phần chữ tay đó vẫn giữ làm dòng đầu -- sửa
 * trong cơ sở dữ liệu là đổi được, không phải biên dịch lại -- còn phía dưới ghép thêm những số
 * liệu chỉ có lúc chạy mới biết: lượt hang động còn lại của chính người đang đăng nhập, tỉ lệ kinh
 * nghiệm, sự kiện đang bật, và số người đang chơi.
 *
 * Mọi phần đều bọc try riêng: một mục hỏng thì thiếu đúng mục đó, chứ không làm mất cả bảng thông
 * báo -- đây là thứ chạy trên đường đăng nhập, không được phép ném lỗi lên.
 */
public final class Welcome {

    private Welcome() {
    }

    public static String text(Char c, String header) {
        StringBuilder b = new StringBuilder();
        if (header != null && !header.trim().isEmpty()) {
            b.append(header.trim()).append('\n');
        }
        if (c != null) {
            b.append("\n-Chào ").append(c.name).append(", cấp ").append(c.level);
            String con = expLeft(c);
            if (con != null) {
                b.append(", còn ").append(con).append(" kinh nghiệm lên cấp");
            }
        }
        line(b, dungeon(c));
        line(b, exp());
        line(b, event());
        line(b, online());
        line(b, "-Lệnh chat: shop (mở cửa hàng), sv (xem ai đang chơi), tt (đổi chủ thân/phân"
                + " thân), hd (bảng hướng dẫn)");
        return b.toString();
    }

    private static void line(StringBuilder b, String s) {
        if (s != null) {
            b.append('\n').append(s);
        }
    }

    private static String dungeon(Char c) {
        try {
            int perDay = DailyLimit.perDay();
            if (perDay <= 1) {
                return null;                  // đang chạy luật gốc, không có gì để khoe
            }
            return "-Hang động: còn " + DailyLimit.leftToday(c) + "/" + perDay + " lượt hôm nay";
        } catch (Throwable ex) {
            return null;
        }
    }

    private static String exp() {
        try {
            int rate = Config.getInstance().getRateEXP();
            return rate > 1 ? "-Máy chủ đang x" + rate + " kinh nghiệm" : null;
        } catch (Throwable ex) {
            return null;
        }
    }

    private static String event() {
        try {
            Event now = Event.getEvent();
            if (now == null || !Event.isEvent()) {
                return null;
            }
            String ten = now.getClass().getSimpleName();
            if (ten.equalsIgnoreCase("Noevent")) {
                return null;
            }
            return "-Sự kiện đang mở: " + ten;
        } catch (Throwable ex) {
            return null;
        }
    }

    private static String online() {
        try {
            int n = 0;
            for (Char x : ServerManager.getChars()) {
                // isCleaned/bag == null là nhân vật đã thoát nhưng vỏ chưa dọn xong.
                if (x != null && x.isHuman && !x.isCleaned && x.bag != null) {
                    n++;
                }
            }
            return "-Đang chơi: " + n + " người";
        } catch (Throwable ex) {
            return null;
        }
    }

    /** Kinh nghiệm còn thiếu để lên cấp, rút gọn cho dễ đọc. Null nếu không tính được. */
    private static String expLeft(Char c) {
        try {
            long can = NinjaUtils.getExp(c.level) - c.exp;
            return can <= 0 ? null : gon(can);
        } catch (Throwable ex) {
            return null;
        }
    }

    private static String gon(long n) {
        if (n >= 1000000000L) {
            return (n / 100000000L) / 10.0 + " tỷ";
        }
        if (n >= 1000000L) {
            return (n / 100000L) / 10.0 + " triệu";
        }
        return String.valueOf(n);
    }
}
