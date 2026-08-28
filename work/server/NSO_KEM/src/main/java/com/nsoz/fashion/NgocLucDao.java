package com.nsoz.fashion;

import com.nsoz.item.Equip;
import com.nsoz.item.ItemTemplate;
import com.nsoz.model.Char;
import com.nsoz.network.Service;
import com.nsoz.server.ServerManager;

/**
 * Làm sáu quả cầu của Ngọc Lục Đạo nhấp nhô, kể cả lúc nhân vật đứng yên.
 *
 * Món này mượn **lớp vũ khí** để hiện hình (xem chú thích trong {@link FashionFromEquip}), mà lớp
 * ấy chỉ dùng đúng **một khung ảnh** cho cả mười khung hình nhân vật -- bảng CharInfo bên client
 * ghi cứng như vậy. Nên không thể làm hoạt ảnh bằng cách thêm khung vào mảnh.
 *
 * Cách còn lại: máy chủ luân phiên đổi **mảnh** rồi báo cho client. Ba mảnh 331/332/333 dùng ba ảnh
 * cầu khác nhau và ba độ cao khác nhau, đổi vòng thì vừa đổi hình vừa nhấp nhô.
 *
 * Đi bằng gói vũ khí chứ không phải gói áo choàng, và đó là điểm mấu chốt: bộ xử lý gói vũ khí bên
 * client dùng **tham chiếu tĩnh** tới nhân vật của mình, còn gói áo choàng thì tra theo id trong sổ
 * người xung quanh -- nhân vật của chính mình không nằm trong sổ đó nên gói rơi vào chỗ trống. Đó
 * là lý do đường áo choàng chết còn đường này sống.
 *
 * Một luồng duy nhất quét mọi người đang online, thay vì mỗi người một bộ hẹn giờ: người mặc món
 * này ít, mà bộ hẹn giờ thì phải tự dọn khi người ta thoát game -- dễ sót.
 */
public final class NgocLucDao {

    /** Mã món -> sáu mảnh quỹ đạo của nó. Thêm màu mới thì thêm một dòng ở đây. */
    private static final int[][] BANG = {
            {1263, 334, 335, 336, 337, 338, 339},   // Cầu lục đạo (tím)
            {1264, 340, 341, 342, 343, 344, 345},   // Cầu lục đạo (đen)
    };

    /** Số pha của một vòng, bằng số mảnh mỗi màu. */
    private static final int SO_PHA = 6;


    /**
     * Nhịp đổi pha. Sáu pha trải đúng 60 độ, nên một vòng đầy đủ mất 6 x 6 x nhịp.
     * 180ms cho vòng chừng 6,5 giây -- đủ chậm để đọc ra là quay, đủ nhanh để không như đứng hình.
     */
    private static final int NHIP = 300;

    private static volatile boolean dangChay = false;

    /**
     * Trạng thái quỹ đạo của TỪNG người: {pha, x lần trước, y lần trước}.
     *
     * Trước đây chỉ có một pha dùng chung cho cả máy chủ, nên ai mặc cũng quay cùng nhịp và quay
     * kể cả lúc đang chạy. Nhìn lúc di chuyển thì rối mắt: nhân vật đã trôi ngang, mấy quả cầu
     * lại nhảy vòng quanh, hai chuyển động chồng lên nhau. Nên tách riêng theo người và chỉ quay
     * khi người đó ĐỨNG YÊN.
     *
     * ConcurrentHashMap vì luồng nền đọc ghi trong khi luồng khác gọi manhHienTai lúc thay đồ.
     */
    private static final java.util.Map<Integer, int[]> QUY_DAO = new java.util.concurrent.ConcurrentHashMap<>();

    /** Mảnh ứng với pha hiện tại của người này. Không mặc thì trả -1. */
    public static short manhHienTai(Char c) {
        int ma = maDangMac(c);
        if (ma == -1) {
            return -1;
        }
        int[] tt = QUY_DAO.get(c.id);
        int pha = tt == null ? 0 : tt[0];
        for (int[] d : BANG) {
            if (d[0] == ma) {
                return (short) d[1 + (pha % SO_PHA)];
            }
        }
        return -1;
    }

    /** Mã món Cầu lục đạo người này đang đeo ở ô trang bị 2, hoặc -1. */
    public static int maDangMac(Char c) {
        if (c == null || c.isCleaned || c.fashion == null) {
            return -1;
        }
        Equip e = c.fashion[ItemTemplate.TYPE_AOCHOANG];
        if (e == null) {
            return -1;
        }
        for (int[] d : BANG) {
            if (d[0] == e.id) {
                return e.id;
            }
        }
        return -1;
    }

    private NgocLucDao() {
    }

    /** Gọi lúc có người mặc món này. Gọi nhiều lần cũng chỉ dựng một luồng. */
    public static synchronized void batDau() {
        if (dangChay) {
            return;
        }
        dangChay = true;
        Thread t = new Thread(NgocLucDao::vong, "ngoc-luc-dao");
        t.setDaemon(true);
        t.start();
    }

    private static void vong() {
        while (true) {
            try {
                Thread.sleep(NHIP);
                for (Char c : ServerManager.getChars()) {
                    if (!dangMac(c)) {
                        if (c != null) {
                            QUY_DAO.remove(c.id);   // cởi ra thì bỏ luôn, không giữ rác
                        }
                        continue;
                    }
                    int[] tt = QUY_DAO.computeIfAbsent(c.id, k -> new int[]{0, Integer.MIN_VALUE, Integer.MIN_VALUE});
                    boolean dungYen = tt[1] == c.x && tt[2] == c.y;
                    tt[1] = c.x;
                    tt[2] = c.y;
                    if (!dungYen) {
                        // Nhúc nhích là bảng thông tin đã đóng: đứng yên mới mở được nó.
                        c.dangXemNguoiKhac = false;
                        // Đang chạy hoặc đang nhảy: giữ nguyên pha và KHÔNG gửi gì cả. Gửi lại
                        // cùng một mảnh chỉ tốn băng thông, mà mỗi lần gửi là một lần client vẽ lại
                        // cả nhân vật -- đúng lúc đang di chuyển thì dễ thành giật hình.
                        continue;
                    }
                    tt[0] = (tt[0] + 1) % SO_PHA;
                    /*
                     * Chỉ gửi ĐÚNG hai gói cần thiết, không gọi setFashion.
                     *
                     * setFashion phát kèm updateInfoChar -- cả bản mô tả nhân vật -- cho toàn khu,
                     * ba lần mỗi giây. Người đứng cạnh thấy nhân vật bị dựng lại liên tục, và
                     * client kèm theo đó một nhúm khói mỗi lần (chính người mặc thì không thấy, vì
                     * client bỏ qua gói mô tả gửi cho chính mình -- đúng bức tường đã gặp ở áo
                     * choàng). Ngoài ra còn tốn băng thông vô ích.
                     *
                     * Hai gói thật sự cần:
                     *   loadWeapon    phát theo khu -> người khác thấy quả cầu đổi vị trí
                     *   updateInfoMe  gửi riêng cho người mặc -> chính họ mới thấy, vì client
                     *                 không áp gói đổi diện mạo cho nhân vật của mình
                     */
                    short manh = manhHienTai(c);
                    if (manh <= 0 || c.zone == null) {
                        continue;
                    }
                    // Đang cầm lái phân thân thì chủ thân phải im.
                    //
                    // Hai người dùng chung một Service (switchToMe chỉ trỏ nó sang người kia), nên
                    // updateInfoMe gọi từ chủ thân lại rơi vào phân thân -- ba lần mỗi giây, dựng
                    // lại trạng thái nhân vật ngay giữa cú nhảy: phân thân không nhảy nổi, cứ lộn
                    // vòng tại chỗ. Chủ thân lúc ấy cũng chẳng ai nhìn thấy, nên không mất gì.
                    Service sv = c.getService();
                    if (sv == null || sv.getChar() != c) {
                        continue;
                    }
                    c.weapon = manh;
                    c.ID_WEA_PONE = -1;
                    c.zone.getService().loadWeapon(c);
                    // Người khác vẫn thấy quỹ đạo chạy (gói trên phát cho cả khu), chỉ nhịn đúng
                    // gói tự-cập-nhật: nó dùng chung màn hình với bảng thông tin đang mở, gửi vào
                    // là bảng nháy một cái rồi nhảy về thông tin của chính người đang xem.
                    if (!c.dangXemNguoiKhac) {
                        c.getService().updateInfoMe();
                    }
                }
            } catch (Throwable ignore) {
                // Một người lỗi thì không được làm chết cả vòng.
            }
        }
    }

    private static boolean dangMac(Char c) {
        return maDangMac(c) != -1;
    }
}
