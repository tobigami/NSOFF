import com.nsoz.db.jdbc.DbManager;
import com.nsoz.item.Item;
import com.nsoz.item.ItemManager;
import com.nsoz.item.ItemTemplate;
import com.nsoz.server.Config;
import com.nsoz.server.GiveItem;
import com.nsoz.store.ItemStore;
import java.util.ArrayList;
import java.util.List;

/**
 * Liệt kê những món trang bị có trong dữ liệu nhưng dựng ra không có chỉ số nào.
 *
 * Chỉ số của một món đến từ một trong hai nguồn: dòng trong store_data (cột options), hoặc mã sinh
 * trong Item.randomOptionItem9x/10x với nhóm cấp 90 và 100. Món nào không thuộc nguồn nào thì mặc
 * vào là món trắng trơn.
 *
 * Dựng bằng chính GiveItem.build -- đúng đường mà lệnh item, lệnh gear và NPC Vua Hùng đều đi --
 * nên kết quả ở đây là thứ người chơi thật sự nhận được, không phải suy đoán từ bảng dữ liệu.
 */
public class StatCheck {

    static final String[] LOAI = {
        "Nón", "Vũ khí", "Áo", "Dây chuyền", "Găng tay", "Nhẫn", "Quần", "Ngọc bội",
        "Giày", "Bùa", "Thú nuôi", "Mặt nạ", "Áo choàng", "Bao tay", "Mắt thần", "Bí kíp"
    };

    public static void main(String[] a) throws Exception {
        if (!Config.getInstance().load() || !DbManager.start()) {
            System.out.println("!! không nạp được cấu hình hoặc CSDL");
            return;
        }
        // Thứ tự bắt buộc, chép đúng Server.init: init() dựng các Store rỗng, load() mới đổ dữ
        // liệu vào. Gọi mỗi load() thì mọi cửa hàng đều rỗng và món nào cũng trông như không có
        // chỉ số -- đúng cái bẫy làm lần chạy đầu báo nhầm 339 món.
        ItemManager.getInstance().load();
        com.nsoz.server.GameData.getInstance().init();
        com.nsoz.store.StoreManager.getInstance().init();
        if (!com.nsoz.store.StoreManager.getInstance().load()) {
            System.out.println("!! không nạp được dữ liệu cửa hàng");
            return;
        }

        List<int[]> trong = new ArrayList<>();      // {id, type, level}
        List<String> ten = new ArrayList<>();
        int tongTrangBi = 0;

        for (int id = 0; id < 5000; id++) {
            ItemTemplate t;
            try {
                t = ItemManager.getInstance().getItemTemplate(id);
            } catch (Exception e) {
                break;                              // hết bảng
            }
            if (t == null || t.type < ItemTemplate.TYPE_BODY_MIN
                    || t.type > ItemTemplate.TYPE_BODY_MAX) {
                continue;
            }
            tongTrangBi++;

            // Thử mọi dòng cửa hàng của món; chỉ cần một dòng có chỉ số là món đó ổn.
            List<ItemStore> rows = GiveItem.storeRows(id);
            int nhieuNhat = 0;
            if (rows.isEmpty()) {
                nhieuNhat = dem(GiveItem.build(null, t, 0));
            } else {
                for (ItemStore r : rows) {
                    nhieuNhat = Math.max(nhieuNhat, dem(GiveItem.build(r, t, 0)));
                }
            }
            if (nhieuNhat == 0) {
                trong.add(new int[] { id, t.type, t.level, rows.size() });
                ten.add(t.name);
            }
        }

        System.out.println("Đã soi " + tongTrangBi + " món trang bị, "
                + trong.size() + " món không có chỉ số nào:");
        System.out.println();
        System.out.println("id\ttên\tloại\tcấp\tdòng cửa hàng");
        for (int i = 0; i < trong.size(); i++) {
            int[] r = trong.get(i);
            System.out.printf("%d\t%s\t%s\t%d\t%s%n", r[0], ten.get(i),
                    r[1] < LOAI.length ? LOAI[r[1]] : ("loại " + r[1]), r[2],
                    r[3] == 0 ? "không có" : (r[3] + " dòng nhưng rỗng"));
        }
        System.exit(0);
    }

    static int dem(Item it) {
        return it == null || it.options == null ? 0 : it.options.size();
    }

    static String cut(String s) {
        if (s == null) {
            return "";
        }
        return s.length() <= 34 ? s : s.substring(0, 33) + "…";
    }
}
