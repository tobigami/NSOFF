package com.nsoz.server;

import com.nsoz.convert.Converter;
import com.nsoz.item.Item;
import com.nsoz.item.ItemFactory;
import com.nsoz.item.ItemManager;
import com.nsoz.item.ItemTemplate;
import com.nsoz.model.Char;
import com.nsoz.store.ItemStore;
import com.nsoz.store.StoreManager;

import java.util.ArrayList;
import java.util.List;

/**
 * Danh sách trang bị của menu "Trang bị" ở NPC Vua Hùng, có nối thêm đồ cấp 90 và 100.
 *
 * Menu gốc dùng StoreManager.getListEquipmentWithLevelRange, mà hàm đó chỉ quét 15 cửa hàng trang
 * bị. Dòng Thái Dương (cấp 90) và Thiên Vương (cấp 100) không nằm trong cửa hàng nào -- chỉ số của
 * chúng sinh bằng mã trong Item.randomOptionItem9x/10x -- nên menu mãi dừng ở cấp 80 dù nhân vật
 * lên cấp bao nhiêu.
 *
 * Ở đây giữ nguyên phần cửa hàng ở ĐẦU danh sách, đúng thứ tự cũ, rồi mới nối đồ 90/100 vào cuối.
 * Nhờ vậy mọi số thứ tự người chơi đã quen vẫn trỏ đúng món cũ; đồ mới lấy số tiếp theo.
 *
 * Nhân tiện vá luôn lỗi cũ: menu gốc gọi thẳng list.get(số) không kiểm biên, gõ quá tay là ném
 * IndexOutOfBoundsException và menu im lặng không báo gì.
 */
public final class VuaHungList {

    /** Cấp thấp nhất của nhóm đồ sinh bằng mã. Dưới mức này đều có dòng cửa hàng. */
    private static final int CAP_SINH_BANG_MA = 90;

    private VuaHungList() {
    }

    /** Món ở vị trí này, đã nhồi chỉ số kiểu Vua Hùng. Null nghĩa là số vượt ngoài danh sách. */
    public static Item build(Char p, int index) {
        if (p == null || index < 0) {
            return null;
        }
        List<ItemStore> cuaHang = StoreManager.getInstance()
                .getListEquipmentWithLevelRange(0, p.level);
        if (index < cuaHang.size()) {
            ItemStore s = cuaHang.get(index);
            if (s == null || s.getTemplate() == null) {
                return null;
            }
            Item itm = Converter.getInstance().toItem(s, Converter.MAX_OPTION);
            VuaHungBoost.apply(itm, p.getSys());
            return itm;
        }
        List<ItemTemplate> them = extras(p.level);
        int i = index - cuaHang.size();
        if (i >= them.size()) {
            return null;
        }
        ItemTemplate t = them.get(i);
        Item itm = GiveItem.codeGen(t);
        // Chỉ số gốc của đồ 90/100 giữ nguyên, phần nhồi thêm cộng lên trên.
        VuaHungBoost.apply(itm, p.getSys());
        return itm;
    }

    /** Tổng số dòng cho một cấp nhân vật, để báo cho người gõ biết số lớn nhất. */
    public static int size(Char p) {
        if (p == null) {
            return 0;
        }
        return StoreManager.getInstance().getListEquipmentWithLevelRange(0, p.level).size()
                + extras(p.level).size();
    }

    /** Vị trí đầu tiên của nhóm đồ 90/100, để nhắc người dùng khỏi phải dò. */
    public static int firstExtra(Char p) {
        return p == null ? -1 : StoreManager.getInstance()
                .getListEquipmentWithLevelRange(0, p.level).size();
    }

    /**
     * Trang bị cấp 90 trở lên, xếp theo cấp rồi theo mã.
     *
     * Quét thẳng ItemManager chứ không ghi cứng danh sách mã: thêm bộ đồ mới vào cơ sở dữ liệu là
     * nó tự có mặt, không phải sửa lại lớp này.
     *
     * BestGear cũng gọi hàm này, để lệnh "gear" và menu NPC nhìn thấy cùng một kho đồ.
     */
    public static List<ItemTemplate> extras(int capNhanVat) {
        List<ItemTemplate> out = new ArrayList<>();
        for (int id = 0; ; id++) {
            ItemTemplate t;
            try {
                t = ItemManager.getInstance().getItemTemplate(id);
            } catch (Exception ex) {
                break;                    // hết bảng: hàm này ném lỗi chứ không trả null
            }
            if (t == null || t.level < CAP_SINH_BANG_MA || t.level > capNhanVat) {
                continue;
            }
            if (t.type < ItemTemplate.TYPE_NON || t.type > ItemTemplate.TYPE_PHU) {
                continue;                 // chỉ 10 ô trang bị, bỏ thú nuôi và đồ thời trang
            }
            if (!GiveItem.storeRows(id).isEmpty()) {
                continue;                 // đã có trong phần cửa hàng ở trên, đừng kể hai lần
            }
            out.add(t);
        }
        out.sort((a, b) -> a.level != b.level ? a.level - b.level : a.id - b.id);
        return out;
    }
}
