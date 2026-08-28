package com.nsoz.store;

import com.nsoz.convert.Converter;
import com.nsoz.item.Item;
import com.nsoz.item.ItemTemplate;
import com.nsoz.model.Char;
import com.nsoz.model.History;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Cửa hàng thường, nhưng có công tắc "mua ra hàng max chỉ số" bật riêng cho từng người.
 *
 * Không thể làm một loại cửa hàng mới: máy khách nhận gói tin cửa hàng rồi *switch theo mã loại*
 * để chọn màn hình đổ danh sách vào (ap.l, ap.aw, ap.ac...). Mã loại lạ rơi hết các nhánh, cửa
 * hàng sẽ trống trơn. Nên phải dùng lại đúng các loại máy khách đã biết, và thay đổi nằm ở hành vi
 * mua chứ không ở giao diện.
 *
 * Chỉ số trang bị nằm ở bảng cửa hàng, mỗi dòng ghi mức TỐI ĐA; mua thường thì
 * Converter.toItem(..., RANDOM_OPTION) bốc ngẫu nhiên giữa sàn và trần. Ở đây chỉ đổi sang
 * MAX_OPTION. Người không bật công tắc thì đi nguyên đường cũ của Store, nên lối chơi bình thường
 * không suy suyển gì.
 */
public class MaxStore extends Store {

    /** id nhân vật đang bật chế độ mua hàng max. */
    private static final Set<Integer> ON = Collections.synchronizedSet(new HashSet<>());

    public MaxStore(int type, String name) {
        super(type, name);
    }

    public static boolean isOn(Char p) {
        return p != null && ON.contains(Integer.valueOf(p.id));
    }

    /** Bật/tắt, trả về trạng thái sau khi đổi. */
    public static boolean toggle(Char p) {
        Integer key = Integer.valueOf(p.id);
        if (ON.remove(key)) {
            return false;
        }
        ON.add(key);
        return true;
    }

    public static void off(Char p) {
        ON.remove(Integer.valueOf(p.id));
    }

    @Override
    public void buy(Char p, int indexUI, int quantity) {
        if (!isOn(p)) {
            super.buy(p, indexUI, quantity);
            return;
        }
        ItemStore item = get(indexUI);
        if (item == null) {
            return;
        }
        ItemTemplate template = item.getTemplate();
        int slotNull = p.getSlotNull();
        if ((template.isUpToUp && slotNull == 0) || (!template.isUpToUp && slotNull < quantity)) {
            p.warningBagFull();
            return;
        }
        long giaXu = ((long) item.getCoin()) * ((long) quantity);
        long giaYen = ((long) item.getYen()) * ((long) quantity);
        long giaLuong = ((long) item.getGold()) * ((long) quantity);
        if (giaXu < 0 || giaYen < 0 || giaLuong < 0) {
            return;
        }
        if (giaXu > p.coin || giaLuong > p.user.gold || giaYen > p.yen) {
            p.serverDialog(p.language.getString("NOT_ENOUGH_MONEY"));
            return;
        }

        History history = new History(p.id, History.MUA_VAT_PHAM);
        history.setPrice((int) giaXu, (int) giaYen, (int) giaLuong);
        history.setBefore(p.coin, p.user.gold, p.yen);
        p.coin -= giaXu;
        p.user.gold -= giaLuong;
        p.yen -= giaYen;
        p.getService().buy();
        history.setAfter(p.coin, p.user.gold, p.yen);

        int n = template.isUpToUp ? 1 : quantity;
        for (int i = 0; i < n; i++) {
            Item newItem = Converter.getInstance().toItem(item, Converter.MAX_OPTION);
            newItem.setQuantity(template.isUpToUp ? quantity : 1);
            if (giaYen > 0 || giaLuong > 0) {
                newItem.isLock = true;
            }
            p.addItemToBag(newItem);
            history.addItem(newItem);
        }
        history.setTime(System.currentTimeMillis());
        History.insert(history);
        p.serverMessage("Đã mua " + quantity + " " + template.name + " (max chỉ số)");
    }
}
