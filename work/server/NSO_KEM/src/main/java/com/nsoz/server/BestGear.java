package com.nsoz.server;

import com.nsoz.convert.Converter;
import com.nsoz.item.Item;
import com.nsoz.item.ItemFactory;
import com.nsoz.item.ItemTemplate;
import com.nsoz.model.Char;
import com.nsoz.store.ItemStore;
import com.nsoz.store.Store;
import com.nsoz.store.StoreManager;

/**
 * Chọn và mặc bộ trang bị tốt nhất theo cấp.
 *
 * Tách khỏi màn hình quản lý để lệnh chat "gear" và nút "Mặc đồ tốt nhất" dùng chung một đường --
 * hai bản chép tay thì sớm muộn cũng lệch nhau.
 *
 * Chỉ số trang bị nằm ở bảng store_data chứ không nằm trong ItemTemplate, nên phải dựng qua
 * Converter.toItem(ItemStore, MAX_OPTION); dựng Item trần thì món nào cũng trắng chỉ số.
 */
public final class BestGear {

    /** Mười ô trang bị chính; bỏ thú nuôi, mặt nạ, áo choàng, bí kíp vì chúng có luật riêng. */
    public static final int[] SLOT = {
        ItemTemplate.TYPE_VUKHI, ItemTemplate.TYPE_NON, ItemTemplate.TYPE_AO,
        ItemTemplate.TYPE_QUAN, ItemTemplate.TYPE_GANGTAY, ItemTemplate.TYPE_GIAY,
        ItemTemplate.TYPE_LIEN, ItemTemplate.TYPE_NHAN, ItemTemplate.TYPE_NGOCBOI,
        ItemTemplate.TYPE_PHU,
    };
    public static final String[] SLOT_NAME = {
        "Vũ khí", "Nón", "Áo", "Quần", "Găng tay", "Giày", "Dây chuyền", "Nhẫn", "Ngọc bội", "Bùa",
    };
    /**
     * {ô, cửa hàng cho gender 1, cửa hàng cho gender 0}.
     *
     * Đối chiếu dữ liệu thật chứ không tin theo tên hằng: TYPE_MEN_* chứa đồ gender 1, TYPE_WOMEN_*
     * chứa gender 0, còn dây chuyền/nhẫn/ngọc/bùa là gender 2 nên dùng chung một cửa hàng.
     */
    private static final int[][] STORE_OF = {
        { ItemTemplate.TYPE_VUKHI,   StoreManager.TYPE_WEAPON,     StoreManager.TYPE_WEAPON },
        { ItemTemplate.TYPE_NON,     StoreManager.TYPE_MEN_HAT,    StoreManager.TYPE_WOMEN_HAT },
        { ItemTemplate.TYPE_AO,      StoreManager.TYPE_MEN_SHIRT,  StoreManager.TYPE_WOMEN_SHIRT },
        { ItemTemplate.TYPE_QUAN,    StoreManager.TYPE_MEN_PANT,   StoreManager.TYPE_WOMEN_PANT },
        { ItemTemplate.TYPE_GANGTAY, StoreManager.TYPE_MEN_GLOVES, StoreManager.TYPE_WOMEN_GLOVES },
        { ItemTemplate.TYPE_GIAY,    StoreManager.TYPE_MEN_SHOES,  StoreManager.TYPE_WOMEN_SHOES },
        { ItemTemplate.TYPE_LIEN,    StoreManager.TYPE_NECKLACE,   StoreManager.TYPE_NECKLACE },
        { ItemTemplate.TYPE_NHAN,    StoreManager.TYPE_RING,       StoreManager.TYPE_RING },
        { ItemTemplate.TYPE_NGOCBOI, StoreManager.TYPE_PEARL,      StoreManager.TYPE_PEARL },
        { ItemTemplate.TYPE_PHU,     StoreManager.TYPE_SPELL,      StoreManager.TYPE_SPELL },
    };

    private BestGear() {
    }

    /** Mặc cả bộ. Trả về báo cáo từng ô, mỗi ô một dòng. */
    public static String dress(Char c, int wantLevel, byte wantSys, boolean replaceWeaker) {
        return dress(c, wantLevel, wantSys, replaceWeaker, false);
    }

    /**
     * Như trên, thêm lựa chọn nhồi chỉ số kiểu menu NPC Vua Hùng.
     *
     * Khác biệt duy nhất là bộ chỉ số: cách chọn món vẫn giữ nguyên, tức là vẫn lọc giới tính, lọc
     * phái cho vũ khí và chọn đúng ô -- thứ mà menu Vua Hùng không làm, nên món nó phát ra nhiều
     * khi mặc không được.
     */
    public static String dress(Char c, int wantLevel, byte wantSys, boolean replaceWeaker,
            boolean boost) {
        StringBuilder sb = new StringBuilder();
        int done = 0;
        for (int i = 0; i < SLOT.length; i++) {
            StringBuilder dropped = new StringBuilder();
            String why = wear(c, SLOT[i], wantLevel, wantSys, replaceWeaker, dropped, boost);
            sb.append(SLOT_NAME[i]).append(": ")
                    .append(why != null ? why
                            : dropped.length() == 0 ? "đã mặc"
                            : "đã mặc, bỏ " + dropped)
                    .append('\n');
            if (why == null) {
                done++;
            }
        }
        c.setAbility();
        sb.append("Xong ").append(done).append('/').append(SLOT.length).append(" ô.");
        return sb.toString();
    }

    /** Trả về null nếu đã mặc được, còn lại là lý do bỏ qua. */
    public static String wear(Char c, int slot, int wantLevel, byte wantSys, boolean replaceWeaker) {
        return wear(c, slot, wantLevel, wantSys, replaceWeaker, null);
    }

    /**
     * Như trên, thêm chỗ ghi tên món bị thay ra.
     *
     * Tên món cũ phải đi bằng đường riêng chứ không dùng giá trị trả về, vì bên gọi đọc null là
     * "đã mặc xong" -- trả chuỗi về là thành công bị đếm nhầm thành thất bại.
     */
    public static String wear(Char c, int slot, int wantLevel, byte wantSys, boolean replaceWeaker,
            StringBuilder dropped) {
        return wear(c, slot, wantLevel, wantSys, replaceWeaker, dropped, false);
    }

    public static String wear(Char c, int slot, int wantLevel, byte wantSys, boolean replaceWeaker,
            StringBuilder dropped, boolean boost) {
        ItemStore best = best(c, slot, wantLevel, wantSys);
        // Đồ cấp 90/100 không có dòng cửa hàng nên best() không bao giờ thấy chúng; tìm riêng rồi
        // lấy món cấp cao hơn trong hai bên.
        ItemTemplate extra = bestExtra(c, slot, wantLevel);
        if (best == null && extra == null) {
            return "không có món nào phù hợp";
        }
        boolean dungExtra = extra != null
                && (best == null || extra.level > best.getTemplate().level);
        ItemTemplate t = dungExtra ? extra : best.getTemplate();
        Item worn = c.equipment[slot] == null ? null
                : Converter.getInstance().toItem(c.equipment[slot]);
        if (worn != null && worn.template != null) {
            if (!replaceWeaker) {
                return "đang mặc " + describe(worn) + ", bỏ qua";
            }
            if (worn.template.level > t.level) {
                return "đang mặc " + describe(worn) + " xịn hơn, giữ nguyên";
            }
        }
        if (c.getSlotNull() == 0) {
            return "hành trang đầy, không có chỗ để đổi";
        }
        Item item;
        if (dungExtra) {
            item = GiveItem.codeGen(t);
        } else {
            item = Converter.getInstance().toItem(best, Converter.MAX_OPTION);
            item.sys = best.getSys();
        }
        if (boost) {
            // VuaHungBoost tự lo phần nâng cấp (và tự cộng thành +24), nên không gọi next ở đây
            // nữa -- gọi cả hai là cộng chồng lên nhau.
            VuaHungBoost.apply(item, item.sys, GiveItem.nangCapDuoc(t));
        } else {
            int up = t.getUpMax();
            if (up > 0) {
                item.next(up);
            }
        }
        if (!c.addItemToBag(item)) {
            return "không bỏ được vào hành trang";
        }
        // useEquipment lấy ô hành trang từ item.index và trả món cũ về đúng ô đó.
        int borrowed = item.index;
        c.useEquipment(item);
        String old = discard(c, borrowed);
        if (old != null && dropped != null) {
            dropped.append(old);
        }
        return null;
    }

    /**
     * Xoá món vừa bị thay ra khỏi hành trang.
     *
     * useEquipment trả món cũ về đúng ô vừa mượn để bỏ món mới vào, nên chỉ cần nhìn lại ô đó.
     * Không xoá thì mặc cả bộ mười ô là hành trang có thêm mười món cũ vô dụng, đầy ngay.
     *
     * Trả về tên món đã xoá, hoặc null nếu ô đó trống (trước đó không mặc gì).
     */
    private static String discard(Char c, int slot) {
        if (slot < 0 || c.bag == null || slot >= c.bag.length) {
            return null;
        }
        Item old = c.bag[slot];
        if (old == null) {
            return null;
        }
        String name = describe(old);
        // Xoá đúng số lượng đang có: removeItem trừ dần chứ không dọn sạch ô.
        c.removeItem(slot, old.getQuantity(), true);
        return name;
    }

    public static String describe(Item item) {
        if (item == null || item.template == null) {
            return "(trống)";
        }
        return item.template.name + " cấp " + item.template.level
                + (item.upgrade > 0 ? " +" + item.upgrade : "");
    }

    /**
     * Món cấp 90 trở lên hợp ô này, cấp cao nhất mà vẫn <= cấp yêu cầu. Null nếu không có.
     *
     * Lọc y hệt best(): đúng loại ô, hợp giới tính, và với vũ khí thì phải hợp phái -- nếu không
     * sẽ mặc không được. Danh sách nguồn dùng chung với menu NPC Vua Hùng qua VuaHungList.extras.
     */
    public static ItemTemplate bestExtra(Char c, int slot, int wantLevel) {
        ItemTemplate best = null;
        for (ItemTemplate t : VuaHungList.extras(wantLevel)) {
            if (t.type != slot) {
                continue;
            }
            if (t.gender < 2 && t.gender != c.gender) {
                continue;
            }
            if (slot == ItemTemplate.TYPE_VUKHI && !t.checkSys(c.classId)) {
                continue;
            }
            if (best == null || t.level > best.level) {
                best = t;
            }
        }
        return best;
    }

    /** Món cấp cao nhất mà vẫn <= cấp yêu cầu, hợp giới tính và hợp phái (với vũ khí). */
    public static ItemStore best(Char c, int slot, int wantLevel, byte wantSys) {
        int storeType = -1;
        for (int[] row : STORE_OF) {
            if (row[0] == slot) {
                storeType = c.gender == 1 ? row[1] : row[2];
                break;
            }
        }
        Store store = StoreManager.getInstance().find((byte) storeType);
        if (store == null) {
            return null;
        }
        ItemStore best = null;
        for (ItemStore s : store.getItems()) {
            ItemTemplate t = s.getTemplate();
            if (t == null || t.type != slot || t.level > wantLevel) {
                continue;
            }
            if (t.gender < 2 && t.gender != c.gender) {
                continue;
            }
            // Vũ khí phải đúng phái, không thì useEquipment trả về "Vũ khí không thích hợp".
            if (slot == ItemTemplate.TYPE_VUKHI && c.classId > 0 && !t.checkSys(c.classId)) {
                continue;
            }
            if (best == null) {
                best = s;
                continue;
            }
            ItemTemplate bt = best.getTemplate();
            if (t.level > bt.level) {
                best = s;
            } else if (t.level == bt.level && s.getSys() == wantSys && best.getSys() != wantSys) {
                // Cùng cấp thì lấy dòng đúng hệ -- mỗi hệ là một dòng riêng trong store_data.
                best = s;
            }
        }
        return best;
    }
}
