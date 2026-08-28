package com.nsoz.server;

import com.nsoz.item.Item;
import com.nsoz.item.ItemTemplate;
import com.nsoz.model.Char;
import com.nsoz.network.Message;

import java.io.DataOutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Nâng cấp liên tục một món tới mức mong muốn, tự lấy đá và bảo hiểm trong hành trang.
 *
 * Không tính lại công thức: mỗi lượt dựng đúng gói tin mà máy khách gửi khi bấm nâng cấp
 * (cờ dùng lượng, ô trang bị, rồi lần lượt các ô đá và bảo hiểm) rồi gọi thẳng Char.upgradeItem.
 * Nhờ vậy tỉ lệ, tiền công, tụt cấp khi hỏng, ghi lịch sử và gói tin báo kết quả về máy khách đều
 * do chính máy chủ làm, không có bản sao công thức nào để lệch pha về sau.
 *
 * Chỗ duy nhất phải bù: upgradeItem xoá đá bằng removeItem(..., false), tức cố tình không báo cho
 * máy khách, vì bình thường máy khách tự biết nó vừa gửi đá nào. Ở đây máy khách không gửi gì cả,
 * nên phải gọi Service.removeItem cho từng ô đã dùng, không thì túi đồ trên máy hiện vẫn còn đá.
 */
public final class AutoUpgrade {

    /** upgradeItem nhận nhiều nhất 18 ô; chừa một ô cho bảo hiểm. */
    private static final int MAX_STONES = 17;
    /** Bảo hiểm nào còn tác dụng ở mức nâng cấp nào (theo chính upgradeItem). */
    private static final int[] INSURANCE_ID = { 242, 284, 285, 475 };
    private static final int[] INSURANCE_UNTIL = { 8, 12, 14, 16 };

    private AutoUpgrade() {
    }

    /** Kết quả một lần chạy, đủ để in ra cho người dùng xem. */
    public static final class Result {
        public int from;
        public int to;
        public int tries;
        public int stones;
        public boolean insuranceUsed;
        public String stop = "";
        public final List<String> log = new ArrayList<>();
    }

    public static boolean canUpgrade(Item item) {
        if (item == null) {
            return false;
        }
        ItemTemplate t = item.template;
        if (t == null) {
            return false;
        }
        if (!t.isTypeClothe() && !t.isTypeAdorn() && !t.isTypeWeapon()) {
            return false;
        }
        if (t.fashion > -1 && !t.isVk100x()) {
            return false;
        }
        return t.getUpMax() > 0;
    }

    /**
     * @param bagIndex ô hành trang chứa món cần nâng -- món đang mặc không nâng được, đúng như
     *                 trong game, vì upgradeItem chỉ đọc bag[].
     */
    public static Result run(Char p, int bagIndex, int target, boolean useInsurance,
                             boolean useGold, int maxTries) {
        Result r = new Result();
        if (!TaskAdmin.alive(p)) {
            r.stop = "nhân vật đã thoát game";
            return r;
        }
        Item item = bagIndex >= 0 && bagIndex < p.numberCellBag ? p.bag[bagIndex] : null;
        if (!canUpgrade(item)) {
            r.stop = "ô " + bagIndex + " không phải trang bị nâng cấp được";
            return r;
        }
        r.from = item.upgrade;
        r.to = item.upgrade;
        int upMax = item.template.getUpMax();
        if (target > upMax) {
            target = upMax;
            r.log.add("Món này chỉ lên tới +" + upMax + ", đã hạ mục tiêu xuống mức đó.");
        }
        if (item.upgrade >= target) {
            r.stop = "đã ở +" + item.upgrade + ", không cần nâng";
            return r;
        }

        while (r.tries < maxTries && item.upgrade < target) {
            if (!TaskAdmin.alive(p)) {
                r.stop = "nhân vật thoát game giữa chừng";
                break;
            }
            int up = item.upgrade;
            long need = stoneValueForMaxPercent(item, up);
            List<Integer> stones = pickStones(p, need, bagIndex);
            if (stones.isEmpty()) {
                r.stop = "hết đá nâng cấp trong hành trang";
                break;
            }
            long got = 0;
            for (Integer s : stones) {
                got += GameData.UP_CRYSTAL[p.bag[s].id];
            }
            int insurance = useInsurance ? findInsurance(p, up) : -1;

            List<Integer> used = new ArrayList<>(stones);
            if (insurance >= 0) {
                used.add(Integer.valueOf(insurance));
            }
            if (!fire(p, bagIndex, used, useGold)) {
                r.stop = "không dựng được gói tin nâng cấp";
                break;
            }
            r.tries++;

            // Đá đã bị máy chủ xoá nhưng máy khách chưa biết -- báo lại từng ô.
            int consumed = 0;
            for (Integer s : used) {
                if (p.bag[s.intValue()] == null) {
                    p.getService().removeItem(s.intValue());
                    consumed++;
                }
            }
            if (consumed == 0) {
                // Không món nào bị dùng nghĩa là upgradeItem đã từ chối (chưa về làng, hết tiền,
                // đang khoá tài khoản...). Chạy tiếp chỉ lặp vô ích.
                r.stop = "máy chủ từ chối lượt nâng -- kiểm tra: đã về làng chưa, còn đủ xu/yên không";
                break;
            }
            r.stones += stones.size();
            if (insurance >= 0) {
                r.insuranceUsed = true;
            }

            item = p.bag[bagIndex];
            if (item == null) {
                r.stop = "trang bị không còn ở ô " + bagIndex;
                break;
            }
            // Mỗi lượt bắn vài gói tin xuống máy khách; dồn một mạch thì màn hình nâng cấp trong
            // game không kịp vẽ và người chơi chỉ thấy kết quả nhảy cóc.
            try {
                Thread.sleep(150);
            } catch (InterruptedException stop) {
                Thread.currentThread().interrupt();
                r.stop = "bị dừng giữa chừng";
                break;
            }

            int pct = percentOf(item, up, got, useGold);
            r.log.add(String.format("lần %d: +%d, %d đá (%s%d%%)%s -> +%d",
                    r.tries, up, stones.size(), useGold ? "dùng lượng, " : "", pct,
                    insurance >= 0 ? ", có bảo hiểm" : "", item.upgrade));
            r.to = item.upgrade;
        }
        if (r.stop.isEmpty()) {
            r.stop = item != null && item.upgrade >= target
                    ? "đã đạt +" + target
                    : "hết " + maxTries + " lượt cho phép";
        }
        r.to = item != null ? item.upgrade : r.to;
        return r;
    }

    /** Dựng gói tin y như máy khách gửi rồi cho máy chủ tự xử lý. */
    private static boolean fire(Char p, int bagIndex, List<Integer> slots, boolean useGold) {
        try {
            Message out = new Message();
            DataOutputStream w = out.writer();
            w.writeBoolean(useGold);
            w.writeByte(bagIndex);
            for (Integer s : slots) {
                w.writeByte(s.intValue());
            }
            w.flush();
            Message in = new Message((byte) 0, out.getData());
            p.upgradeItem(in);
            in.cleanup();
            out.cleanup();
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    /** Bảng điểm dùng cho loại trang bị này. */
    private static int[] table(Item item) {
        if (item.template.isTypeClothe()) {
            return GameData.UP_CLOTHE;
        }
        if (item.template.isTypeAdorn()) {
            return GameData.UP_ADORN;
        }
        return GameData.UP_WEAPON;
    }

    private static int maxPercent(int up) {
        int m = GameData.MAX_PERCENT[up];
        // upgradeItem dùng "m += m * add" -- phép gán gộp tự cắt về int, giữ y hệt để khỏi lệch.
        return (int) (m + m * Config.getInstance().getMaxPercentAdd());
    }

    private static int percentOf(Item item, int up, long stoneValue, boolean useGold) {
        int pct = (int) (stoneValue * 100 / table(item)[up]);
        int cap = maxPercent(up);
        if (pct > cap) {
            pct = cap;
        }
        return useGold ? (int) (pct * 1.5) : pct;
    }

    /**
     * Tổng điểm đá vừa đủ chạm trần tỉ lệ. Bỏ thêm đá quá mức này không tăng cơ hội, chỉ phí đá --
     * đó là chỗ việc tự động ăn đứt việc bấm tay.
     */
    private static long stoneValueForMaxPercent(Item item, int up) {
        long v = (long) Math.ceil(maxPercent(up) * (double) table(item)[up] / 100.0);
        return v < 1 ? 1 : v;
    }

    /**
     * Chọn tổ hợp đá **rẻ nhất** mà vẫn chạm trần tỉ lệ.
     *
     * "Rẻ" ở đây là tổng điểm bỏ ra, không phải số viên: điểm đá tăng gấp bốn mỗi cấp, nên bỏ một
     * Đá cấp 10 (262.144 điểm) vào chỗ chỉ cần 7.117 là phí gần bốn mươi lần. Cách làm: đi từ loại
     * to xuống loại nhỏ, mỗi bước thử "dừng ở đây rồi bù nốt bằng một viên vừa đủ lớn", và giữ lại
     * phương án tổng điểm thấp nhất. Vì các mốc điểm là luỹ thừa của 4 nên cách này cho kết quả
     * tối ưu, mà vẫn xét được cả hai thái cực: một viên to duy nhất, hay nhiều viên nhỏ cộng lại.
     */
    private static List<Integer> pickStones(Char p, long need, int skip) {
        List<Integer> slots = new ArrayList<>();
        for (int i = 0; i < p.numberCellBag; i++) {
            Item it = p.bag[i];
            if (i != skip && it != null && it.template != null && it.template.isTypeCrystal()
                    && it.id >= 0 && it.id < GameData.UP_CRYSTAL.length && it.getQuantity() == 1) {
                slots.add(Integer.valueOf(i));
            }
        }
        if (slots.isEmpty()) {
            return slots;
        }
        // điểm giảm dần
        slots.sort((a, b) -> GameData.UP_CRYSTAL[p.bag[b.intValue()].id]
                - GameData.UP_CRYSTAL[p.bag[a.intValue()].id]);

        long[] value = new long[slots.size()];
        for (int i = 0; i < value.length; i++) {
            value[i] = GameData.UP_CRYSTAL[p.bag[slots.get(i).intValue()].id];
        }
        List<Integer> pick = new ArrayList<>();
        for (int i : choose(value, need)) {
            pick.add(slots.get(i));
        }
        return pick;
    }

    /**
     * Chọn các chỉ mục trong {@code value} (đã sắp giảm dần) sao cho tổng >= {@code need} với tổng
     * nhỏ nhất. Tách riêng khỏi hành trang để kiểm chứng được bằng số liệu thuần.
     */
    static List<Integer> choose(long[] value, long need) {
        List<Integer> running = new ArrayList<>();
        long got = 0;
        List<Integer> best = null;
        long bestCost = Long.MAX_VALUE;
        int at = 0;

        while (true) {
            long left = need - got;
            if (left <= 0) {
                if (got < bestCost) {
                    bestCost = got;
                    best = new ArrayList<>(running);
                }
                break;
            }
            if (running.size() < MAX_STONES) {
                // Phương án "dừng ở đây": bù nốt phần thiếu bằng viên nhỏ nhất mà một mình đã đủ.
                for (int j = value.length - 1; j >= at; j--) {
                    if (value[j] >= left) {
                        if (got + value[j] < bestCost) {
                            bestCost = got + value[j];
                            best = new ArrayList<>(running);
                            best.add(Integer.valueOf(j));
                        }
                        break;
                    }
                }
            }
            if (running.size() >= MAX_STONES) {
                break;
            }
            // Đi tiếp: lấy viên to nhất mà **không vượt** phần còn thiếu. Vượt qua thì đã được xét ở
            // nhánh bù bên trên rồi, thêm vào đây chỉ làm đắt lên.
            while (at < value.length && value[at] > left) {
                at++;
            }
            if (at >= value.length) {
                break;
            }
            running.add(Integer.valueOf(at));
            got += value[at];
            at++;
        }

        if (best != null) {
            return best;
        }
        // Không tổ hợp nào chạm trần: dồn hết những viên to nhất, chấp nhận tỉ lệ thấp hơn.
        List<Integer> all = new ArrayList<>();
        for (int i = 0; i < value.length && all.size() < MAX_STONES; i++) {
            all.add(Integer.valueOf(i));
        }
        return all;
    }

    /** Ô chứa bảo hiểm còn tác dụng ở mức nâng cấp hiện tại, -1 nếu không có. */
    private static int findInsurance(Char p, int up) {
        for (int k = 0; k < INSURANCE_ID.length; k++) {
            if (up >= INSURANCE_UNTIL[k]) {
                continue;
            }
            for (int i = 0; i < p.numberCellBag; i++) {
                Item it = p.bag[i];
                if (it != null && it.id == INSURANCE_ID[k] && it.getQuantity() == 1) {
                    return i;
                }
            }
        }
        return -1;
    }
}
