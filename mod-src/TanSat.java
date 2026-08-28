/**
 * Menu "Tàn sát" chỉ liệt kê quái có trong map hiện tại.
 *
 * Client vốn dựng menu đó từ một Vector tĩnh tên pF: mỗi lần một con quái được nạp vào map, mẫu
 * quái của nó được thêm vào pF nếu chưa có. pF không bao giờ được dọn khi đổi map, nên đi càng
 * nhiều map thì danh sách càng dài, lẫn cả quái của những map đã rời đi từ lâu.
 *
 * Không đụng vào pF -- nó vẫn được nạp như cũ, phòng khi chỗ khác còn dùng. Thay vào đó hai lệnh
 * getstatic pF trong vòng lặp dựng menu được đổi thành lời gọi hàm dưới đây (getstatic và
 * invokestatic đều dài 3 byte nên không lệnh nhảy nào xê dịch).
 *
 * Nguồn thay thế là il -- danh sách quái đang sống trong map hiện tại, được dọn sạch mỗi lần đổi
 * map. Mẫu quái tra theo đúng cách mà chính client làm lúc nạp quái: sU[mob.tk].
 *
 * Menu gọi hàm này hai lần mỗi vòng lặp (một cho size, một cho elementAt) nên nó dựng lại danh
 * sách nhiều lần. Danh sách chỉ vài chục phần tử, rẻ hơn nhiều so với việc phải chèn thêm mã.
 */
public final class TanSat {

    private TanSat() {
    }

    /** Mẫu quái có mặt trong map hiện tại, không trùng lặp. Kiểu trả về phải khớp kiểu của pF. */
    public static IlllIlllIIlllIIIllIIlllIllIIlIIlIIlIllIlllIllIlIIIlllIIIlIllIIllIIIllIllIIlIIIlIIlllIllIlIIllIIllIIlllIIIllIIllIIIlllIIIllIIllIIIlllIllIIIlllIIllIIIlIIllIlIllIIllIIlllIIllIIIllIIIlIIllIIlllIIIlIlllIIIllIIllIIIllIIllIIlIllIIlIlIIlIllIIlll cur() {
        IlllIlllIIlllIIIllIIlllIllIIlIIlIIlIllIlllIllIlIIIlllIIIlIllIIllIIIllIllIIlIIIlIIlllIllIlIIllIIllIIlllIIIllIIllIIIlllIIIllIIllIIIlllIllIIIlllIIllIIIlIIllIlIllIIllIIlllIIllIIIllIIIlIIllIIlllIIIlIlllIIIllIIllIIIllIIllIIlIllIIlIlIIlIllIIlll out = new IlllIlllIIlllIIIllIIlllIllIIlIIlIIlIllIlllIllIlIIIlllIIIlIllIIllIIIllIllIIlIIIlIIlllIllIlIIllIIllIIlllIIIllIIllIIIlllIIIllIIllIIIlllIllIIIlllIIllIIIlIIllIlIllIIllIIlllIIllIIIllIIIlIIllIIlllIIIlIlllIIIllIIllIIIllIIllIIlIllIIlIlIIlIllIIlll();
        try {
            IlllIlllIIlllIIIllIIlllIllIIlIIlIIlIllIlllIllIlIIIlllIIIlIllIIllIIIllIllIIlIIIlIIlllIllIlIIllIIllIIlllIIIllIIllIIIlllIIIllIIllIIIlllIllIIIlllIIllIIIlIIllIlIllIIllIIlllIIllIIIllIIIlIIllIIlllIIIlIlllIIIllIIllIIIllIIllIIlIllIIlIlIIlIllIIlll live = IIllIlIIlllIlllIllIllIlIIIlllIlIIlIIIllIlllIIllIllIIllIIllIIIllIlllIllIIIlllIIlIlllIIlIIlIIlIIllIIlIlIIlIIllIIlIlIllIIlIlIllIIlIIllIIlIllIlIlIlllIllIIIllIlIIlllIIlIllIlIIlIlIIIlIllIIlllIIlIllIIlIIlllIIlll.il;
            if (live == null) {
                return out;
            }
            for (int i = 0; i < live.size(); i++) {
                Object o = live.elementAt(i);
                if (!(o instanceof IIIlllIlIIlllIIlllIlIlIlIlllIIIllIIIllIIlIlIIIllIlIlIIllIIllIIlllIIllIlllIIIllIIIlIIIlIIllIIllIIIlIllIIllIIIllIIlllIllIIIllIIllIIIllIlIIIllIIIlllIllIlIlIIIlllIIlllIIllIIlllIIllIlIIlllIIIlllIIllIIlIIllIlIllIIIllIlllIIlllIlIlIIllIlll)) {
                    continue;
                }
                IIIlllIlIIlllIIlllIlIlIlIlllIIIllIIIllIIlIlIIIllIlIlIIllIIllIIlllIIllIlllIIIllIIIlIIIlIIllIIllIIIlIllIIllIIIllIIlllIllIIIllIIllIIIllIlIIIllIIIlllIllIlIlIIIlllIIlllIIllIIlllIIllIlIIlllIIIlllIIllIIlIIllIlIllIIIllIlllIIlllIlIlIIllIlll m = (IIIlllIlIIlllIIlllIlIlIlIlllIIIllIIIllIIlIlIIIllIlIlIIllIIllIIlllIIllIlllIIIllIIIlIIIlIIllIIllIIIlIllIIllIIIllIIlllIllIIIllIIllIIIllIlIIIllIIIlllIllIlIlIIIlllIIlllIIllIIlllIIllIlIIlllIIIlllIIllIIlIIllIlIllIIIllIlllIIlllIlIlIIllIlll) o;
                IIllIIlllIlIIIllIIIllIIIlllIIllIIlllIllIIIllIllIIIllIlIllIlIlIIllIIllIllIIlIIlllIIlIlIlIllIllIIlIlllIIllIIllIIlIlllIIIllIIIllIIlIIIlllIIIlllIIllIIllIllIIIlllIIlIIllIlIIlllIIlIIIllIlllIIlIllIlIlIIllIIlIIllIIllIllIIIllIIIllIIlll[] tpl = IIIlllIlIIlllIIlllIlIlIlIlllIIIllIIIllIIlIlIIIllIlIlIIllIIllIIlllIIllIlllIIIllIIIlIIIlIIllIIllIIIlIllIIllIIIllIIlllIllIIIllIIllIIIllIlIIIllIIIlllIllIlIlIIIlllIIlllIIllIIlllIIllIlIIlllIIIlllIIllIIlIIllIlIllIIIllIlllIIlllIlIlIIllIlll.sU;
                if (tpl == null || m.tk < 0 || m.tk >= tpl.length) {
                    continue;
                }
                IIllIIlllIlIIIllIIIllIIIlllIIllIIlllIllIIIllIllIIIllIlIllIlIlIIllIIllIllIIlIIlllIIlIlIlIllIllIIlIlllIIllIIllIIlIlllIIIllIIIllIIlIIIlllIIIlllIIllIIllIllIIIlllIIlIIllIlIIlllIIlIIIllIlllIIlIllIlIlIIllIIlIIllIIllIllIIIllIIIllIIlll t = tpl[m.tk];
                if (t != null && !out.contains(t)) {
                    out.addElement(t);
                }
            }
        } catch (Throwable ignored) {
            // Menu hỏng thì thôi, tuyệt đối không được làm sập game.
        }
        return out;
    }
}
