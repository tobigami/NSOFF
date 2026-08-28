import com.nsoz.db.jdbc.DbManager;
import com.nsoz.item.ItemManager;
import com.nsoz.item.ItemTemplate;
import com.nsoz.server.Config;

/** Kiem bo loc "chi do thoi trang": co bao nhieu mon, va mon nao lot vao ca hai nhom. */
public class FashionCheck {
    public static void main(String[] a) throws Exception {
        if (!Config.getInstance().load() || !DbManager.start()) {
            System.out.println("!! khong nap duoc cau hinh/DB");
            return;
        }
        ItemManager.getInstance().load();
        int tong = 0, thoiTrang = 0;
        StringBuilder vidu = new StringBuilder();
        // ItemManager khong co ham dem, duyet toi khi het chi muc -- giong cach SendItemAdmin lam.
        for (int i = 0; ; i++) {
            ItemTemplate t;
            try {
                t = ItemManager.getInstance().getItemTemplate(i);
            } catch (Exception ex) {
                break;
            }
            if (t == null) break;
            tong++;
            if (t.fashion != -1) {
                thoiTrang++;
                if (thoiTrang <= 5) {
                    vidu.append(String.format("%n   %d %s (type %d, fashion %d)",
                            t.id, t.name, t.type, t.fashion));
                }
            }
        }
        System.out.println("tong so vat pham: " + tong);
        System.out.println("do thoi trang (Tbi 2): " + thoiTrang);
        System.out.println("do thuong (Tbi 1): " + (tong - thoiTrang));
        System.out.println("vai vi du:" + vidu);
        System.exit(0);
    }
}
