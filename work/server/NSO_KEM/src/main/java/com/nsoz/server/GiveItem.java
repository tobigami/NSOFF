package com.nsoz.server;

import com.nsoz.convert.Converter;
import com.nsoz.item.Item;
import com.nsoz.item.ItemFactory;
import com.nsoz.item.ItemManager;
import com.nsoz.item.ItemTemplate;
import com.nsoz.model.Char;
import com.nsoz.store.ItemStore;
import com.nsoz.store.Store;
import com.nsoz.store.StoreManager;

import java.util.ArrayList;
import java.util.List;

/**
 * Cấp một vật phẩm bất kỳ theo id vào hành trang.
 *
 * Lệnh "body" có sẵn của máy chủ cấp theo CẤP ĐỘ chứ không theo id, còn "gem" chỉ nhận đúng bốn
 * id ngọc -- nên muốn lấy một món cụ thể thì trước đây phải mở màn hình "Gửi đồ". Lớp này gom
 * phần dựng món ra chỗ dùng chung, để lệnh chat và màn hình đó không bao giờ cho ra hai kết quả
 * khác nhau.
 *
 * Chỉ số KHÔNG nằm ở ItemTemplate mà ở store_data: cùng một id có thể có vài dòng cửa hàng khác
 * nhau ở hệ. Nên nếu tìm được dòng cửa hàng thì dựng từ đó (Converter.toItem), còn không có dòng
 * nào -- đan dược, nguyên liệu, đồ nhiệm vụ -- thì ItemFactory.newItem là đủ.
 */
public final class GiveItem {

    /**
     * Mọi cửa hàng có dòng trong store_data.
     *
     * Không riêng nhóm trang bị: đồ thời trang như Thiên Nguyệt Chi Nữ giữ chỉ số ở cửa hàng tạp
     * hoá (14), còn 32 và 34 là thời trang và gia tộc. Dò thiếu thì món ra không có chỉ số nào.
     */
    private static final int[] STORE_TYPES = { 2, 6, 7, 8, 9, 14, 15,
            StoreManager.TYPE_NECKLACE, StoreManager.TYPE_RING, StoreManager.TYPE_PEARL,
            StoreManager.TYPE_SPELL, StoreManager.TYPE_MEN_HAT, StoreManager.TYPE_WOMEN_HAT,
            StoreManager.TYPE_MEN_SHIRT, StoreManager.TYPE_WOMEN_SHIRT,
            StoreManager.TYPE_MEN_GLOVES, StoreManager.TYPE_WOMEN_GLOVES,
            StoreManager.TYPE_MEN_PANT, StoreManager.TYPE_WOMEN_PANT,
            StoreManager.TYPE_MEN_SHOES, StoreManager.TYPE_WOMEN_SHOES,
            StoreManager.TYPE_FASHION, StoreManager.TYPE_CLAN };

    private GiveItem() {
    }

    /** Mọi dòng cửa hàng của một id. Rỗng là món này không có chỉ số ở đâu cả. */
    public static List<ItemStore> storeRows(int itemId) {
        List<ItemStore> out = new ArrayList<>();
        for (int type : STORE_TYPES) {
            Store store = StoreManager.getInstance().find((byte) type);
            if (store == null) {
                continue;
            }
            for (ItemStore s : store.getItems()) {
                if (s.getTemplate() != null && s.getTemplate().id == itemId) {
                    out.add(s);
                }
            }
        }
        return out;
    }

    /**
     * Dựng món cấp 90/100 -- nhóm có chỉ số sinh bằng mã chứ không có dòng cửa hàng.
     *
     * Cờ true là lấy bản kịch trần. ThienVuongStats bù chỗ máy chủ gốc bỏ trống (14 món giáp và
     * trang sức Thiên Vương). Ba nơi cùng dựng nhóm này -- lệnh item, lệnh gear, menu NPC Vua
     * Hùng -- đều đi qua đây để không nơi nào cho ra món khác nơi kia.
     */
    public static Item codeGen(ItemTemplate t) {
        Item item = t.level >= 100
                ? ItemFactory.getInstance().newItem10X(t.id, true)
                : ItemFactory.getInstance().newItem9X(t.id, true);
        ThienVuongStats.fill(item, t);
        return item;
    }

    /** Dòng hợp hệ mong muốn, không có thì lấy dòng đầu. sys < 0 là không kén. */
    public static ItemStore pick(List<ItemStore> rows, int sys) {
        if (rows.isEmpty()) {
            return null;
        }
        if (sys >= 0) {
            for (ItemStore r : rows) {
                if (r.getSys() == sys) {
                    return r;
                }
            }
        }
        return rows.get(0);
    }

    /**
     * Dựng một món đúng cách máy chủ dựng: từ dòng cửa hàng nếu có, rồi cộng nâng cấp.
     *
     * Đồ cấp 90 (Thái Dương) và cấp 100 (Thiên Vương) không có dòng nào trong store_data -- chỉ số
     * của chúng sinh bằng mã trong Item.randomOptionItem9x/10x, mỗi món một khối riêng. Dựng bằng
     * newItem thường thì ra món rỗng không chỉ số, nên phải rẽ sang đúng hai hàm đó, và luôn lấy
     * bản kịch trần (đường chơi thật chỉ ra hàng max với xác suất 1/30 hoặc 1/3).
     */
    /**
     * Món này có nâng cấp được không -- chép đúng điều kiện của đường nâng cấp trong game.
     *
     * Không dùng getUpMax() làm câu trả lời: hàm ấy chỉ tra theo cấp của món, nên món nào cấp cao
     * cũng ra 16 dù thuộc loại không bao giờ nâng được.
     */
    public static boolean nangCapDuoc(ItemTemplate t) {
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

    public static Item build(ItemStore store, ItemTemplate t, int up) {
        Item item;
        if (store != null) {
            item = Converter.getInstance().toItem(store, Converter.MAX_OPTION);
            item.sys = store.getSys();
        } else if (t.level >= 90) {
            item = codeGen(t);
        } else {
            item = ItemFactory.getInstance().newItem(t.id);
        }
        if (up > 0) {
            item.next(up);
        }
        return item;
    }

    /**
     * Bỏ vật phẩm vào hành trang. Trả về câu trả lời cho người gõ lệnh, không bao giờ null.
     *
     * up < 0 là tự lấy mức nâng cấp tối đa của món đó; sys < 0 là không kén hệ.
     */
    public static String give(Char c, int itemId, int quantity, int up, int sys) {
        return give(c, itemId, quantity, up, sys, false);
    }

    /** Như trên, boost = true thì nhồi bộ chỉ số của menu NPC Vua Hùng vào món. */
    public static String give(Char c, int itemId, int quantity, int up, int sys, boolean boost) {
        if (c == null) {
            return "Không có nhân vật.";
        }
        if (itemId < 0) {
            return "Mã vật phẩm phải từ 0 trở lên.";
        }
        if (quantity <= 0) {
            return "Số lượng phải lớn hơn 0.";
        }
        ItemTemplate t;
        try {
            t = ItemManager.getInstance().getItemTemplate(itemId);
        } catch (Exception ex) {
            t = null;
        }
        if (t == null) {
            return "Không có vật phẩm mã " + itemId + ".";
        }
        ItemStore store = pick(storeRows(itemId), sys);
        // Chỉ nâng cấp món THẬT SỰ nâng cấp được.
        //
        // getUpMax() chỉ nhìn CẤP của món, không nhìn loại -- nên mặt nạ cấp 120 cũng trả về 16.
        // Kết quả là mặt nạ, áo choàng, bí kíp phát ra kèm +12/+16 trong khi trong game chúng
        // không nâng cấp được lấy một cấp. Món ấy mạnh hơn đồ thật, mà nhìn vào không ai ngờ.
        //
        // Luật ở đây chép đúng đường nâng cấp trong game (Char.java, nhánh nâng cấp ở NPC):
        // phải là quần áo / trang sức / vũ khí, VÀ không phải đồ thời trang (fashion > -1), trừ
        // vũ khí 100x. Sai một trong hai thì mức nâng cấp phải là 0.
        int upgrade = nangCapDuoc(t) ? (up < 0 ? t.getUpMax() : up) : 0;
        int sent = 0;
        Item cuoi = null;   // món vừa dựng, để báo cho đúng là nó có chỉ số hay không
        try {
            if (t.isUpToUp && !boost) {
                // Đồ xếp chồng đi một món mang cả số lượng; tách ra nhiều món sẽ bị gộp lại sai.
                Item item = build(store, t, upgrade);
                cuoi = item;
                item.setQuantity(quantity);
                sent = c.addItemToBag(item) ? quantity : 0;
            } else {
                for (int i = 0; i < quantity; i++) {
                    // Mỗi ô phải là một đối tượng riêng, dùng lại một Item thì các ô cùng trỏ một chỗ.
                    Item item = build(store, t, boost ? 0 : upgrade);
                    cuoi = item;
                    if (boost) {
                        // Đồ nhồi chỉ số không xếp chồng được: mỗi món mang chỉ số riêng, gộp ô là
                        // mất hết chỉ số của những món sau.
                        VuaHungBoost.apply(item, item.sys > 0 ? item.sys : (c.getSys()), nangCapDuoc(t));
                    }
                    item.setQuantity(1);
                    if (!c.addItemToBag(item)) {
                        break;
                    }
                    sent++;
                }
            }
        } catch (Throwable ex) {
            System.out.println("GiveItem: " + ex);
            return "Không cấp được " + t.name + ": " + ex;
        }
        if (sent == 0) {
            return "Hành trang đã đầy, không nhận được " + t.name + ".";
        }
        String about = "Đã nhận " + sent + " " + t.name + " (mã " + itemId + ")";
        if (boost) {
            about += " (chỉ số Vua Hùng)";
        } else if (upgrade > 0) {
            about += " +" + upgrade;
        }
        // Cảnh báo "không có chỉ số" chỉ đúng khi món THẬT SỰ ra đời rỗng.
        //
        // Trước đây chỉ nhìn "không có dòng cửa hàng và cấp dưới 90" rồi kết luận -- sai với mọi
        // món có chỉ số viết cứng trong Item.initOption, mà bí kíp danh hiệu là cả một bảng như
        // thế. Nói ngược sự thật thì lần sau không ai tin dòng cảnh báo này nữa.
        if (cuoi != null && (cuoi.options == null || cuoi.options.isEmpty())) {
            about += ", món này không có chỉ số";
        }
        return about + ".";
    }
}
