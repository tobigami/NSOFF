package com.nsoz.server;

import com.nsoz.constants.CMD;
import com.nsoz.mob.Mob;
import com.nsoz.model.Char;
import com.nsoz.network.Message;
import java.io.DataOutputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Bù nhìn tập luyện: không chết, và báo cho người đánh biết sát thương cao nhất của họ.
 *
 * Dùng để tự đo sức đánh mà không phải đi tìm quái đủ máu. Đặt ở ba trường và sáu làng.
 *
 * Cách móc vào: đúng một lời gọi trong Mob, chỗ xử lý một cú đánh trúng quái:
 *
 *     addHp(-damage);
 *     this.Fight(p, damage);                            <- đổi thành BuNhin.danh(this, p, damage)
 *     zone.getService().attackMonster(damage, false, this);
 *     int hp = Math.abs(nextHP - preHP);
 *     p.addExp(this, hp);
 *     if (this.hp <= 0) this.die();
 *
 * Đặt lại hp = maxHP ngay tại đó giải quyết cùng lúc ba việc, không phải sửa thêm dòng nào:
 * bù nhìn không bao giờ chết, không cho kinh nghiệm (vì nextHP hoá ra bằng preHP nên chênh lệch
 * bằng 0), và có chỗ ghi lại sát thương.
 *
 * KHÔNG tạo mẫu quái mới mà dùng luôn mẫu bù nhìn sẵn có, phân biệt bằng VỊ TRÍ ĐẶT. Nhờ vậy
 * client không phải tải lại bảng mẫu quái, và mấy con bù nhìn phục vụ nhiệm vụ tân thủ ở Trường
 * Hirosaki vẫn chết bình thường -- làm chúng bất tử là hỏng nhiệm vụ "giết 10 bù nhìn".
 *
 * Bảng ghi sát thương chỉ nằm trong bộ nhớ, cố ý không lưu xuống CSDL: đây là chỗ thử ngay lúc
 * đó, không phải bảng xếp hạng.
 */
public final class BuNhin {

    /** Mẫu quái bù nhìn. */
    private static final int MAU = 0;
    /** Không ăn đòn nào quá bấy nhiêu mili giây thì coi như hết một loạt đo. */
    private static final long NGHI = 10000L;

    /** Vị trí các bù nhìn tập luyện, dạng "map:x:y". Khớp đúng với dữ liệu đặt trong bảng map. */
    private static final Set<String> CHO_DUNG = new HashSet<>();
    static {
        CHO_DUNG.add("1:1644:360");      // Trường Hirosaki
        CHO_DUNG.add("27:300:144");      // Trường Haruna
        CHO_DUNG.add("72:252:432");      // Trường Ookaza
        CHO_DUNG.add("10:564:264");      // Làng Kojin
        CHO_DUNG.add("17:900:240");      // Làng Sanzu
        CHO_DUNG.add("32:900:336");      // Làng chài
        CHO_DUNG.add("38:1524:240");     // Làng Chakumi
        CHO_DUNG.add("43:1812:456");     // Làng Echigo
        CHO_DUNG.add("48:828:408");      // Làng Oshin
    }

    /** Sổ đo của một người: mốc đánh gần nhất, mức cao nhất của loạt, mức đã báo. */
    private static final class Do {
        long lanCuoi;
        long max;

    }

    /** Theo id nhân vật. */
    private static final Map<Integer, Do> BANG = new HashMap<>();

    private BuNhin() {
    }

    /**
     * Thay cho Mob.Fight(Char, int). Giữ nguyên hành vi cũ rồi mới xử lý phần bù nhìn.
     */
    public static void danh(Mob m, Char p, int dame) {
        m.Fight(p, dame);
        try {
            if (!laBuNhin(m) || p == null) {
                return;
            }
            m.hp = m.maxHP;
            ghiNhan(m, p, dame);
        } catch (Throwable t) {
            // Không để cú đánh bình thường hỏng theo, nhưng PHẢI in ra chứ không nuốt lặng lẽ:
            // chính vì nuốt mà lỗi thiếu lớp BuNhin$Do ẩn suốt bốn vòng thử.
            System.out.println("BU NHIN loi: " + t);
            t.printStackTrace();
        }
    }

    private static boolean laBuNhin(Mob m) {
        if (m == null || m.template == null || m.template.id != MAU) {
            return false;
        }
        if (m.zone == null || m.zone.map == null) {
            return false;
        }
        StringBuilder k = new StringBuilder();
        k.append(m.zone.map.id).append(':').append(m.x).append(':').append(m.y);
        boolean ok = CHO_DUNG.contains(k.toString());
        return ok;
    }

    /**
     * Ghi nhận một cú đánh vào bù nhìn, và chỉ báo khi xác lập ĐỈNH MỚI.
     *
     * Bản trước nuôi một luồng nhắc lại mỗi 1,2 giây để con số đứng yên trên đầu nhân vật, nhưng
     * thực tế nó đẩy ra hàng chục dòng cùng một giá trị -- đọc rất khó chịu. Giờ mỗi đỉnh chỉ nói
     * đúng một lần, nên đánh một tràng dài cũng chỉ thấy vài dòng, mỗi dòng là một kỷ lục thật.
     *
     * Quá NGHI giây không ăn đòn nào thì mức cao nhất đặt lại từ đầu, lần đo sau bắt đầu sạch.
     */
    private static void ghiNhan(Mob m, Char p, int dame) {
        long now = System.currentTimeMillis();
        Do o;
        synchronized (BANG) {
            o = BANG.get(Integer.valueOf(p.id));
            if (o == null) {
                o = new Do();
                BANG.put(Integer.valueOf(p.id), o);
            }
            don(now);
        }
        long dinh;
        synchronized (o) {
            if (now - o.lanCuoi >= NGHI) {
                o.max = 0L;
            }
            o.lanCuoi = now;
            if (dame <= o.max) {
                return;                 // không phải kỷ lục mới thì im lặng
            }
            o.max = dame;
            dinh = o.max;
        }
        noi(m, p, nhan(dinh));
    }

    /** Bỏ sổ đo của người đã lâu không đánh, khỏi phình bộ nhớ theo thời gian. */
    private static void don(long now) {
        if (BANG.size() <= 32) {
            return;
        }
        java.util.Iterator<Map.Entry<Integer, Do>> it = BANG.entrySet().iterator();
        while (it.hasNext()) {
            Do d = it.next().getValue();
            if (now - d.lanCuoi > 600000L) {
                it.remove();
            }
        }
    }

    /**
     * Chữ hiện lên, tự rút gọn cho đọc được ở khung nhỏ: "MAX 182.3K".
     *
     * Rút gọn ngay tại máy chủ chứ không nhờ client, vì phần rút gọn bên client chỉ động vào
     * chuỗi TOÀN chữ số, mà ở đây có chữ "MAX" đứng trước.
     */
    private static String nhan(long v) {
        StringBuffer sb = new StringBuffer();
        sb.append("MAX ");
        if (v < 100000L) {
            sb.append(v);
            return sb.toString();
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
        sb.append(v / chia).append('.').append((v / (chia / 10)) % 10).append(hau);
        return sb.toString();
    }

    /**
     * Đẩy bong bóng thoại, gửi RIÊNG cho phiên của người đánh nên hai người cùng đánh một bù nhìn
     * vẫn thấy số của mình.
     *
     * Bong bóng gắn theo ID NHÂN VẬT chứ không phải id quái. Lần đầu tôi thử gắn theo id quái để
     * chữ nằm đúng trên đầu bù nhìn, nhưng client không hiểu -- không bong bóng nào hiện ra, thứ
     * nhìn thấy chỉ là số sát thương bình thường của game. Mọi lời gọi CHAT_MAP trong mã gốc đều
     * dùng id nhân vật hoặc bot, không chỗ nào cho quái nói. Chữ vì thế hiện trên đầu chính người
     * đánh, vẫn ngay cạnh bù nhìn vì phải đứng sát mới đánh tới.
     */
    private static void noi(Mob m, Char p, String text) {
        try {
            // Dùng đúng lời gọi mà mã gốc vẫn dùng để cho nhân vật nói (xem Char.java:1064).
            // Trước đó tôi tự dựng gói CHAT_MAP rồi gửi riêng cho một phiên -- gói giống hệt
            // nhưng client không hiện gì, nên bỏ đường tự dựng, đi đường đã được chứng minh.
            if (p.zone != null) {
                p.zone.getService().chat(p.id, text);
            }
            // Dòng chữ vàng: kênh chắc chắn đọc được, phòng khi bong bóng vẫn không hiện.
            p.serverMessage(text);
        } catch (Throwable t) {
            System.out.println("BU NHIN loi: " + t);
        }
    }
}
