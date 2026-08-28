import com.nsoz.model.AutoUseLogin;
import com.nsoz.server.Config;
import com.nsoz.db.jdbc.DbManager;

import java.util.List;

/**
 * Kiem phan doc danh sach id truoc khi nap. Diem quan trong: "Da cap 1" mang id 0, neu bo loc
 * con id > 0 thi mon hay bi vut nhat lai bi bo qua.
 */
public class BagCheck {
    public static void main(String[] a) throws Exception {
        if (!Config.getInstance().load() || !DbManager.start()) {
            System.out.println("!! khong nap duoc cau hinh/DB");
            return;
        }
        List<Integer> items = AutoUseLogin.ids("items");
        List<Integer> drop = AutoUseLogin.ids("drop");
        System.out.println("items = " + items);
        System.out.println("drop  = " + drop);
        System.out.println("nhan id 0 (Da cap 1) -> " + drop.contains(Integer.valueOf(0)));
        System.exit(0);
    }
}
