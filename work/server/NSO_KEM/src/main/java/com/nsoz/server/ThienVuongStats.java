package com.nsoz.server;

import com.nsoz.item.Item;
import com.nsoz.item.ItemFactory;
import com.nsoz.item.ItemManager;
import com.nsoz.item.ItemTemplate;
import com.nsoz.option.ItemOption;

import java.util.HashMap;
import java.util.Map;

/**
 * Vá chỉ số cho nhóm giáp và trang sức Thiên Vương -- bản máy chủ gốc bỏ trống.
 *
 * Chỉ số của đồ cấp 90/100 không nằm ở cơ sở dữ liệu mà viết cứng trong Item.randomOptionItem9x
 * và randomOptionItem10x, mỗi món một khối. Hàm cấp 100 chỉ có khối cho 6 vũ khí Thiên Vương và
 * cho dải 1163-1176 (bộ Minh Giác Cốt Ngọc). Mười bốn món 1097-1110 rơi vào khoảng trống: hàm
 * chạy qua không gán gì, món ra không có chỉ số nào.
 *
 * Ở đây không tự nghĩ ra số: mỗi món rỗng được gán đúng bảng chỉ số của món Minh Giác CÙNG Ô và
 * CÙNG GIỚI TÍNH -- hai bộ này soi gương nhau từng ô, cấp chỉ xê xích 1 đến 4. Lấy số của chính
 * game thì không sợ lệch cân bằng, và nếu sau này tác giả bổ sung khối chỉ số thật thì phần vá
 * này tự tắt, vì nó chỉ chạm vào món có danh sách chỉ số rỗng.
 */
public final class ThienVuongStats {

    /** Dải id của bộ Minh Giác Cốt Ngọc, dùng làm khuôn. */
    private static final int KHUON_DAU = 1163, KHUON_CUOI = 1176;

    /** Khoá là loại ô * 10 + giới tính. */
    private static final Map<Integer, Integer> KHUON = new HashMap<>();

    private ThienVuongStats() {
    }

    /**
     * Bù chỉ số nếu món đang rỗng. Trả về true nếu có vá.
     *
     * Món nào đã có chỉ số thì không đụng tới -- kể cả vũ khí Thiên Vương, vốn đủ chỉ số sẵn.
     */
    public static boolean fill(Item item, ItemTemplate t) {
        if (item == null || t == null || (item.options != null && !item.options.isEmpty())) {
            return false;
        }
        ItemTemplate khuon = khuon(t);
        if (khuon == null) {
            return false;
        }
        Item mau = ItemFactory.getInstance().newItem10X(khuon.id, true);
        if (mau.options == null || mau.options.isEmpty()) {
            return false;
        }
        if (item.options == null) {
            item.options = new java.util.ArrayList<ItemOption>();
        }
        for (ItemOption o : mau.options) {
            if (o != null && o.optionTemplate != null) {
                // Phải nhân bản: dùng chung đối tượng thì hai món cùng trỏ một chỉ số, nâng cấp
                // món này là món kia đổi theo.
                item.options.add(o.clone());
            }
        }
        item.sys = mau.sys;
        return true;
    }

    /** Món Minh Giác cùng ô, cùng giới tính. Null nếu không có khuôn hợp. */
    private static ItemTemplate khuon(ItemTemplate t) {
        napKhuon();
        Integer id = KHUON.get(Integer.valueOf(t.type * 10 + t.gender));
        if (id == null) {
            return null;
        }
        try {
            return ItemManager.getInstance().getItemTemplate(id.intValue());
        } catch (Exception ex) {
            return null;
        }
    }

    private static synchronized void napKhuon() {
        if (!KHUON.isEmpty()) {
            return;
        }
        for (int id = KHUON_DAU; id <= KHUON_CUOI; id++) {
            try {
                ItemTemplate t = ItemManager.getInstance().getItemTemplate(id);
                if (t != null) {
                    KHUON.put(Integer.valueOf(t.type * 10 + t.gender), Integer.valueOf(id));
                }
            } catch (Exception ignored) {
            }
        }
    }
}
