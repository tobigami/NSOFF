import com.nsoz.convert.Converter;
import com.nsoz.db.jdbc.DbManager;
import com.nsoz.item.Item;
import com.nsoz.item.ItemManager;
import com.nsoz.item.ItemTemplate;
import com.nsoz.server.Config;
import com.nsoz.store.ItemStore;
import com.nsoz.server.BestGear;
import com.nsoz.store.Store;
import com.nsoz.store.StoreManager;

/**
 * Kiem bo do "tot nhat" truoc khi nap: voi vai cap tieu bieu, tung o chon duoc mon nao, muc nang
 * cap toi da la bao nhieu, va chi so co thuc su la MAX khong.
 */
public class GearCheck {
    static final int[][] SLOT = {
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
    static final String[] NAME = { "Vu khi", "Non", "Ao", "Quan", "Gang", "Giay",
                                   "Day chuyen", "Nhan", "Ngoc boi", "Bua" };

    public static void main(String[] a) throws Exception {
        if (!Config.getInstance().load() || !DbManager.start()) {
            System.out.println("!! khong nap duoc cau hinh/DB");
            return;
        }
        ItemManager.getInstance().load();
        StoreManager.getInstance().init();
        StoreManager.getInstance().load();
        ItemManager.getInstance().init();

        int[] levels = { 20, 50, 90 };
        int gender = 1, classId = 1;   // gender 1 la nhom do trong cua hang TYPE_MEN_*
        byte sys = 1;
        for (int lv : levels) {
            System.out.println("\n=== cap " + lv + " (phai " + classId + ", he " + sys + ")");
            System.out.println("   (kiem lai bang chinh ham BestGear dung: so o = "
                    + BestGear.SLOT.length + ")");
            for (int i = 0; i < SLOT.length; i++) {
                ItemStore best = best(SLOT[i][0], SLOT[i][gender == 1 ? 1 : 2], lv, gender, classId, sys);
                if (best == null) {
                    System.out.printf("  %-11s khong co mon phu hop%n", NAME[i]);
                    continue;
                }
                ItemTemplate t = best.getTemplate();
                Item item = Converter.getInstance().toItem(best, Converter.MAX_OPTION);
                int up = t.getUpMax();
                if (up > 0) item.next(up);
                System.out.printf("  %-11s %-26s cap %-3d +%-3d he %d, %d chi so%n",
                        NAME[i], t.name, t.level, item.upgrade, best.getSys(),
                        item.options == null ? 0 : item.options.size());
            }
        }
        System.exit(0);
    }

    static ItemStore best(int slot, int storeType, int wantLevel, int gender, int classId, byte sys) {
        Store store = StoreManager.getInstance().find((byte) storeType);
        if (store == null) return null;
        ItemStore best = null;
        for (ItemStore s : store.getItems()) {
            ItemTemplate t = s.getTemplate();
            if (t == null || t.type != slot || t.level > wantLevel) continue;
            if (t.gender < 2 && t.gender != gender) continue;
            if (slot == ItemTemplate.TYPE_VUKHI && classId > 0 && !t.checkSys(classId)) continue;
            if (best == null) { best = s; continue; }
            ItemTemplate bt = best.getTemplate();
            if (t.level > bt.level) best = s;
            else if (t.level == bt.level && s.getSys() == sys && best.getSys() != sys) best = s;
        }
        return best;
    }
}
