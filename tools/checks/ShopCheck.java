import com.nsoz.convert.Converter;
import com.nsoz.db.jdbc.DbManager;
import com.nsoz.item.Item;
import com.nsoz.item.ItemManager;
import com.nsoz.option.ItemOption;
import com.nsoz.server.Config;
import com.nsoz.store.ItemStore;
import com.nsoz.store.MaxStore;
import com.nsoz.store.Store;
import com.nsoz.store.StoreManager;

/** Kiem cua hang do max truoc khi nap: dung lop nao, so mon co vuot gioi han byte khong, chi so co max khong. */
public class ShopCheck {
    static final int[] T = {2,20,21,22,23,24,25,26,27,28,29,16,17,18,19};

    public static void main(String[] a) throws Exception {
        if (!Config.getInstance().load() || !DbManager.start()) { System.out.println("!! khong nap duoc cau hinh/DB"); return; }
        ItemManager.getInstance().load();
        StoreManager.getInstance().init();
        StoreManager.getInstance().load();
        ItemManager.getInstance().init();

        System.out.println("\n1) lop cua tung cua hang + so mon (gioi han goi tin la 127)");
        boolean allMax = true, overflow = false;
        for (int t : T) {
            Store s = StoreManager.getInstance().find((byte) t);
            if (s == null) { System.out.printf("   type %-3d KHONG CO%n", t); continue; }
            boolean isMax = s instanceof MaxStore;
            allMax &= isMax;
            int n = s.getItems().size();
            if (n > 127) overflow = true;
            System.out.printf("   type %-3d %-16s %-10s %3d mon %s%n", t, s.getName(),
                    isMax ? "MaxStore" : s.getClass().getSimpleName(), n, n > 127 ? "<-- VUOT 127!" : "");
        }
        System.out.println("   => tat ca la MaxStore: " + allMax + " | co cua hang vuot 127: " + overflow);

        System.out.println("\n2) cong tac tat -> mua thuong; bat -> mua max");
        Store weapon = StoreManager.getInstance().find((byte) 2);
        ItemStore row = null;
        for (ItemStore is : weapon.getItems()) if (is.getTemplate().id == 117) { row = is; break; }
        System.out.println("   tim Triet Ta Tieu trong cua hang vu khi: " + (row != null ? "co" : "KHONG"));
        if (row != null) {
            Item mx = Converter.getInstance().toItem(row, Converter.MAX_OPTION);
            Item rd = Converter.getInstance().toItem(row, Converter.RANDOM_OPTION);
            System.out.print("   MAX   :"); for (ItemOption o : mx.options) System.out.print(" " + o.param);
            System.out.print("\n   RANDOM:"); for (ItemOption o : rd.options) System.out.print(" " + o.param);
            System.out.println("\n   gia: " + row.getCoin() + " xu");
        }
        System.exit(0);
    }
}
