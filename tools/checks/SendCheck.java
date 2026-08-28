import com.nsoz.convert.Converter;
import com.nsoz.db.jdbc.DbManager;
import com.nsoz.item.Item;
import com.nsoz.item.ItemManager;
import com.nsoz.item.ItemTemplate;
import com.nsoz.option.ItemOption;
import com.nsoz.server.Config;
import com.nsoz.store.ItemStore;
import com.nsoz.store.Store;
import com.nsoz.store.StoreManager;

import javax.swing.JFrame;
import javax.swing.JTable;
import java.awt.Component;
import java.awt.Container;
import java.io.File;

/** Nạp đúng dữ liệu Server.init() nạp, rồi dựng cửa sổ gửi đồ và kiểm từng phần. */
public class SendCheck {

    public static void main(String[] a) throws Exception {
        if (!Config.getInstance().load()) { System.out.println("!! doc config that bai"); return; }
        if (!DbManager.start())            { System.out.println("!! ket noi DB that bai"); return; }
        ItemManager.getInstance().load();
        StoreManager.getInstance().init();
        if (!StoreManager.getInstance().load()) { System.out.println("!! nap cua hang that bai"); return; }
        ItemManager.getInstance().init();

        int n = 0;
        try { while (ItemManager.getInstance().getItemTemplate(n) != null) n++; }
        catch (IndexOutOfBoundsException e) { /* het */ }
        System.out.println("1) ItemManager duyet duoc " + n + " vat pham");

        // Triet Ta Tieu qua dung duong may chu dung
        ItemStore row = null;
        for (int t : new int[]{StoreManager.TYPE_WEAPON}) {
            Store s = StoreManager.getInstance().find((byte) t);
            for (ItemStore is : s.getItems())
                if (is.getTemplate().id == 117) { row = is; break; }
        }
        System.out.println("2) dong cua hang cua 117: " + (row != null ? "co, he " + row.getSys() : "KHONG CO"));

        Item max = Converter.getInstance().toItem(row, Converter.MAX_OPTION);
        System.out.print("3) MAX_OPTION 117 :");
        for (ItemOption o : max.options) System.out.print(" " + o.optionTemplate.id + "=" + o.param);
        System.out.println();
        Item min = Converter.getInstance().toItem(row, Converter.MIN_OPTION);
        System.out.print("   MIN_OPTION 117 :");
        for (ItemOption o : min.options) System.out.print(" " + o.optionTemplate.id + "=" + o.param);
        System.out.println();
        Item up = Converter.getInstance().toItem(row, Converter.MAX_OPTION);
        up.next(12);
        System.out.print("   MAX +12        :");
        for (ItemOption o : up.options) System.out.print(" " + o.optionTemplate.id + "=" + o.param);
        System.out.println("   (upgrade=" + up.upgrade + ")");

        // anh
        int have = 0, miss = 0;
        for (int i = 0; i < n; i++) {
            ItemTemplate t = ItemManager.getInstance().getItemTemplate(i);
            if (new File("Data/Img/Small/4/Small" + t.icon + ".png").isFile()) have++; else miss++;
        }
        System.out.println("4) anh icon: co " + have + ", thieu " + miss);

        // dung cua so that va dem so dong sau khi loc
        Class<?> c = Class.forName("com.nsoz.server.SendItemAdmin");
        JFrame f = (JFrame) c.getDeclaredConstructor().newInstance();
        JTable table = findTable(f);
        System.out.println("5) cua so dung duoc, bang co " + table.getRowCount() + " dong");

        java.lang.reflect.Field sf = c.getDeclaredField("search");
        sf.setAccessible(true);
        javax.swing.JTextField search = (javax.swing.JTextField) sf.get(f);
        search.setText("triet ta");           // go khong dau
        System.out.println("6) tim \"triet ta\" -> " + table.getRowCount() + " ket qua: "
                + (table.getRowCount() > 0 ? table.getValueAt(0, 2) + " id " + table.getValueAt(0, 1) : "-"));
        for (String q : new String[]{"mac sau", "tieu", "long luc", "DAN", "117"}) {
            search.setText(q);
            System.out.println("   tim \"" + q + "\" -> " + table.getRowCount() + " ket qua"
                    + (table.getRowCount() > 0 ? ", dau tien: " + table.getValueAt(0, 2) : ""));
        }
        f.dispose();
        System.exit(0);
    }

    static JTable findTable(Container c) {
        for (Component x : c.getComponents()) {
            if (x instanceof JTable) return (JTable) x;
            if (x instanceof Container) { JTable t = findTable((Container) x); if (t != null) return t; }
        }
        return null;
    }
}
