import com.nsoz.db.jdbc.DbManager;
import com.nsoz.item.ItemManager;
import com.nsoz.server.Config;
import com.nsoz.store.Store;
import com.nsoz.store.StoreManager;

/** Kiem 10 cua hang cua lenh "shop": co ton tai khong, bao nhieu mon, co vuot gioi han goi tin 127 khong. */
public class ShopQuickCheck {
    static final int[] TYPE = { 6, 8, 14, 2, 15, 16, 17, 18, 19, 32 };
    static final String[] NAME = { "Duoc pham", "Thuc an", "Tap hoa", "Vu khi", "Sach",
                                   "Day chuyen", "Nhan", "Ngoc boi", "Bua", "Thoi trang" };

    public static void main(String[] a) throws Exception {
        if (!Config.getInstance().load() || !DbManager.start()) {
            System.out.println("!! khong nap duoc cau hinh/DB");
            return;
        }
        ItemManager.getInstance().load();
        StoreManager.getInstance().init();
        StoreManager.getInstance().load();
        ItemManager.getInstance().init();
        for (int i = 0; i < TYPE.length; i++) {
            Store s = StoreManager.getInstance().find((byte) TYPE[i]);
            if (s == null) {
                System.out.printf("%-12s type %-3d KHONG CO%n", NAME[i], TYPE[i]);
                continue;
            }
            int n = s.getItems().size();
            System.out.printf("%-12s type %-3d %3d mon %s%n", NAME[i], TYPE[i], n,
                    n > 127 ? "<- VUOT 127, goi tin se cat bot" : "");
        }
        System.exit(0);
    }
}
