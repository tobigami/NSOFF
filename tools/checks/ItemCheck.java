import com.nsoz.db.jdbc.DbManager;
import com.nsoz.item.Item;
import com.nsoz.item.ItemManager;
import com.nsoz.item.ItemTemplate;
import com.nsoz.server.Config;
import com.nsoz.server.GiveItem;
import com.nsoz.store.ItemStore;
import java.util.List;

/** Dựng thử một vật phẩm đúng cách máy chủ dựng, xem có ra được món không. */
public class ItemCheck {
    public static void main(String[] a) throws Exception {
        if (!Config.getInstance().load() || !DbManager.start()) {
            System.out.println("!! không nạp được cấu hình hoặc CSDL");
            return;
        }
        ItemManager.getInstance().load();
        com.nsoz.server.GameData.getInstance().init();
        com.nsoz.store.StoreManager.getInstance().init();
        com.nsoz.store.StoreManager.getInstance().load();

        for (String s : a) {
            int id = Integer.parseInt(s);
            System.out.println("--- vật phẩm " + id + " ---");
            ItemTemplate t;
            try {
                t = ItemManager.getInstance().getItemTemplate(id);
            } catch (Exception e) {
                System.out.println("   getItemTemplate ném: " + e);
                continue;
            }
            if (t == null) {
                System.out.println("   mẫu null");
                continue;
            }
            System.out.println("   tên=" + t.name + " loại=" + t.type + " giới=" + t.gender
                    + " cấp=" + t.level + " icon=" + t.icon + " part=" + t.part
                    + " fashion=" + t.fashion);
            List<ItemStore> rows = GiveItem.storeRows(id);
            System.out.println("   dòng cửa hàng: " + rows.size());
            try {
                Item it = GiveItem.build(GiveItem.pick(rows, -1), t, 0);
                System.out.println("   dựng: " + (it == null ? "NULL" : "ok, id=" + it.id
                        + " chỉ số=" + (it.options == null ? -1 : it.options.size())
                        + " hạn=" + it.expire));
            } catch (Throwable e) {
                System.out.println("   dựng NÉM: " + e);
            }
        }
        System.exit(0);
    }
}
