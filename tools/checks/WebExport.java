import com.nsoz.convert.Converter;
import com.nsoz.db.jdbc.DbManager;
import com.nsoz.item.Item;
import com.nsoz.item.ItemFactory;
import com.nsoz.item.ItemManager;
import com.nsoz.item.ItemTemplate;
import com.nsoz.option.ItemOption;
import com.nsoz.server.Config;
import com.nsoz.server.GiveItem;
import com.nsoz.store.ItemStore;
import com.nsoz.store.StoreManager;

import java.io.File;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Xuat toan bo vat pham ra JSON cho trang tra cuu tren web.
 *
 * Chay bang chinh bo may chu -- ItemManager, StoreManager, Converter -- nen chi so trong trang web
 * dung y het cai man hinh "Gui do" trong cua so quan tri. Doc bang MySQL roi tu suy ra chi so thi
 * se lech, vi chi so nam o store_data chu khong o item.
 *
 * Kem theo danh sach cua menu "Trang bi" o NPC Vua Hung: menu do dung SO THU TU trong danh sach
 * getListEquipmentWithLevelRange(0, cap nhan vat) chu khong dung ma vat pham, nen trang web luu
 * ca thu tu lan cap cua tung mon de tinh lai so thu tu theo cap ma nguoi dung nhap.
 *
 * Usage: chay voi thu muc lam viec la work/server/NSO_KEM
 *        java -cp target/Nso-jar-with-dependencies.jar:<srvcls> WebExport <duong dan ra>
 */
public class WebExport {

    public static void main(String[] a) throws Exception {
        String out = a.length > 0 ? a[0] : "items.json";
        if (!Config.getInstance().load() || !DbManager.start()) {
            System.out.println("!! khong nap duoc cau hinh/DB");
            return;
        }
        ItemManager.getInstance().load();
        StoreManager.getInstance().init();
        StoreManager.getInstance().load();
        ItemManager.getInstance().init();

        // Thu tu nay phai giong het VuaHungList: phan cua hang truoc, roi do cap 90/100 noi sau.
        List<ItemStore> vuaHung = StoreManager.getInstance()
                .getListEquipmentWithLevelRange(0, Integer.MAX_VALUE);
        List<ItemTemplate> them = com.nsoz.server.VuaHungList.extras(Integer.MAX_VALUE);

        PrintWriter w = new PrintWriter(new File(out), StandardCharsets.UTF_8.name());
        w.print("{\"items\":[");
        int n = 0;
        for (int id = 0; ; id++) {
            ItemTemplate t;
            try {
                t = ItemManager.getInstance().getItemTemplate(id);
            } catch (Exception ex) {
                break;                    // het bang: ham nay nem loi chu khong tra null
            }
            if (t == null) {
                continue;
            }
            if (n++ > 0) {
                w.print(",");
            }
            List<ItemStore> rows = GiveItem.storeRows(id);
            w.print("{\"id\":" + t.id
                    + ",\"ten\":" + json(t.name)
                    + ",\"loai\":" + t.type
                    + ",\"cap\":" + t.level
                    + ",\"gt\":" + t.gender
                    + ",\"icon\":" + t.icon
                    + ",\"fashion\":" + t.fashion
                    + ",\"chong\":" + (t.isUpToUp ? 1 : 0)
                    + ",\"upmax\":" + t.getUpMax()
                    + ",\"mota\":" + json(t.description)
                    + ",\"he\":[");
            for (int i = 0; i < rows.size(); i++) {
                w.print((i > 0 ? "," : "") + rows.get(i).getSys());
            }
            w.print("],\"cs\":[");
            // Chi so lay o mon dung tu dong cua hang dau tien, muc MAX -- dung cach may chu dung.
            //
            // Mon KHONG ban o cua hang nao (danh hieu, trang phuc phat tay, do su kien) thi khong co
            // dong store_data de doc, truoc day ra bang chi so rong. Voi nhung mon do thi dung
            // ItemFactory de tao that mot cai: initOption() chinh la noi may chu quyet dinh chi so,
            // nen so hien tren web dung y het luc cam mon do trong tay.
            //
            // Luu y: vai dong initOption co NinjaUtils.nextInt (vi thu chang han), nen nhung mon ay
            // hien mot lan quay ngau nhien chu khong phai khoang gia tri. Mon co chi so co dinh --
            // gom toan bo danh hieu va trang phuc trang bi 2 -- thi luon dung.
            Item item = null;
            if (!rows.isEmpty()) {
                item = Converter.getInstance().toItem(rows.get(0), Converter.MAX_OPTION);
            } else {
                try {
                    item = ItemFactory.getInstance().newItem(id);
                } catch (Exception ex) {
                    item = null;
                }
            }
            if (item != null && item.options != null) {
                boolean first = true;
                for (ItemOption o : item.options) {
                    if (o == null || o.optionTemplate == null) {
                        continue;
                    }
                    w.print((first ? "" : ",") + json(o.getOptionString()));
                    first = false;
                }
            }
            w.print("]}");
        }
        w.print("],\"vuahung\":[");
        for (int i = 0; i < vuaHung.size(); i++) {
            ItemTemplate t = vuaHung.get(i).getTemplate();
            w.print((i > 0 ? "," : "") + "[" + (t == null ? -1 : t.id) + ","
                    + (t == null ? -1 : t.level) + ",0]");
        }
        // Cot thu ba danh dau nhom: 0 la do cua hang, 1 la do cap 90/100 sinh bang ma. Web can
        // biet de xep dung thu tu -- nhom nay luon nam sau, va chi hien khi du cap.
        for (ItemTemplate t : them) {
            w.print(",[" + t.id + "," + t.level + ",1]");
        }
        w.print("]}");
        w.close();
        System.out.println("da ghi " + n + " vat pham, danh sach Vua Hung " + vuaHung.size() + " dong -> " + out);
        System.exit(0);
    }

    private static String json(String s) {
        if (s == null) {
            return "\"\"";
        }
        StringBuilder b = new StringBuilder("\"");
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '"' || c == '\\') {
                b.append('\\').append(c);
            } else if (c == '\n' || c == '\r' || c == '\t') {
                b.append(' ');
            } else if (c < 0x20) {
                continue;
            } else {
                b.append(c);
            }
        }
        return b.append('"').toString();
    }
}
