import com.nsoz.db.jdbc.DbManager;
import com.nsoz.event.Event;
import com.nsoz.item.ItemManager;
import com.nsoz.server.Config;
import com.nsoz.store.StoreManager;

/**
 * Kiem viec doi su kien luc dang chay: nap duoc lop khong, loadEventPoint va initStore co chay
 * tron khong, va isEvent() tra ve gi. Lam that tren tung lop, khong doan.
 */
public class EventCheck {
    static final String[] NAMES = { "Noevent", "Halloween", "TrungThuNew", "Noel", "LunarNewYear",
                                    "CoHon", "He", "KoroKing", "VietnameseWomensDay",
                                    "InternationalWomensDay" };

    public static void main(String[] a) throws Exception {
        if (!Config.getInstance().load() || !DbManager.start()) {
            System.out.println("!! khong nap duoc cau hinh/DB");
            return;
        }
        ItemManager.getInstance().load();
        StoreManager.getInstance().init();
        StoreManager.getInstance().load();
        ItemManager.getInstance().init();

        for (String n : NAMES) {
            String cls = "com.nsoz.event." + n;
            Config.getInstance().setEvent(cls);
            Event.init();
            Event e = Event.getEvent();
            if (e == null) {
                System.out.printf("%-24s KHONG NAP DUOC%n", n);
                continue;
            }
            String note = "";
            try {
                e.loadEventPoint();
                e.initStore();
            } catch (Throwable ex) {
                note = " (loi: " + ex + ")";
            }
            System.out.printf("%-24s lop=%-22s con hieu luc=%-5s%s%n",
                    n, e.getClass().getSimpleName(), Event.isEvent(), note);
        }
        System.exit(0);
    }
}
