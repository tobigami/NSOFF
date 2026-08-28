package com.nsoz.model;

import com.nsoz.item.Item;
import com.nsoz.network.Message;
import com.nsoz.util.NinjaUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * Tự dùng vài món đan ngay khi đăng nhập, để khỏi phải bấm lại từng món mỗi lần vào game.
 *
 * Dùng đúng đường mà người chơi bấm tay đi: dựng gói tin chứa ô hành trang rồi gọi Char.useItem(),
 * nên hiệu ứng, gói tin gửi xuống máy khách và việc trừ vật phẩm đều giống hệt lúc bấm thật.
 * Viết lại phần đó ở đây thì sớm muộn cũng lệch với bản gốc.
 *
 * Nằm trong package com.nsoz.model vì cần đọc Char.em -- trường đó là protected, và soi qua
 * reflection chỉ để hỏi "hiệu ứng còn không" thì rườm rà hơn là đặt lớp đúng chỗ.
 *
 * Cấu hình ở tệp autouse.properties cạnh jar máy chủ, đọc lại mỗi khi tệp đổi nên sửa xong không
 * cần khởi động lại:
 *
 *   items    = 275,276,277,278     id vật phẩm tự dùng, theo đúng thứ tự này
 *   drop     = 0,1                 id vật phẩm tự xoá khỏi hành trang khi đăng nhập
 *   accounts =                     để trống là mọi tài khoản, hoặc liệt kê: avada,bieu
 *   delay    = 5                   đợi bấy nhiêu giây sau khi vào game rồi mới chạy
 */
public final class AutoUseLogin {

    private static final String FILE = "autouse.properties";
    /** Đan nào sinh ra hiệu ứng nào -- lấy từ nhánh xử lý vật phẩm 275..278 trong Char. */
    private static final int[][] EFFECT_OF = { { 275, 24 }, { 276, 25 }, { 277, 26 }, { 278, 27 } };

    private static Properties config = new Properties();
    private static long loadedAt = -1;

    private AutoUseLogin() {
    }

    /** Gọi ngay sau khi nhân vật vào thế giới. Không ném lỗi ra ngoài để đăng nhập không bị hỏng. */
    public static void apply(final Char p) {
        try {
            if (p == null || !p.isHuman) {
                return;
            }
            reload();
            final List<Integer> items = ids("items");
            final List<Integer> junk = ids("drop");
            if ((items.isEmpty() && junk.isEmpty()) || !allowed(p)) {
                return;
            }
            int delay = number(config.getProperty("delay"), 5);
            // Đợi một nhịp: lúc vừa đăng nhập máy khách còn đang nạp bản đồ, gửi hiệu ứng xuống
            // ngay thì nó hiện nửa vời hoặc không hiện.
            NinjaUtils.setTimeout(new Runnable() {
                public void run() {
                    useAll(p, items);
                    dropAll(p, junk);
                }
            }, delay * 1000);
        } catch (Exception ex) {
            System.out.println("AutoUseLogin: " + ex);
        }
    }

    private static void useAll(Char p, List<Integer> items) {
        try {
            // Người chơi có thể đã thoát ngay trong lúc chờ; bag == null là vỏ đã bị dọn.
            if (p.isCleaned || p.bag == null) {
                return;
            }
            for (Integer id : items) {
                use(p, id.intValue());
            }
        } catch (Exception ex) {
            System.out.println("AutoUseLogin: " + ex);
        }
    }

    /** Xoá sạch những id rác khỏi hành trang. Trả về số ô đã dọn. */
    public static int dropAll(Char p, List<Integer> junk) {
        int cleared = 0;
        try {
            if (p == null || p.isCleaned || p.bag == null || junk.isEmpty()) {
                return 0;
            }
            for (int i = 0; i < p.bag.length; i++) {
                Item item = p.bag[i];
                if (item == null || !junk.contains(Integer.valueOf(item.id))) {
                    continue;
                }
                // Xoá đúng số lượng đang có, vì một ô có thể xếp chồng nhiều món.
                p.removeItem(i, item.getQuantity(), true);
                cleared++;
            }
        } catch (Exception ex) {
            System.out.println("AutoUseLogin: " + ex);
        }
        return cleared;
    }

    private static void use(Char p, int itemId) throws Exception {
        if (hasEffectOf(p, itemId)) {
            return;                       // hiệu ứng còn hạn, dùng nữa là phí một món
        }
        int index = p.getIndexItemByIdInBag(itemId);
        if (index < 0) {
            return;                       // không có trong hành trang thì thôi
        }
        Message out = new Message();
        out.writer().writeByte(index);
        out.writer().flush();
        p.useItem(new Message((byte) 0, out.getData()));
    }

    private static boolean hasEffectOf(Char p, int itemId) {
        for (int[] pair : EFFECT_OF) {
            if (pair[0] == itemId) {
                return p.em != null && p.em.findByID(pair[1]) != null;
            }
        }
        return false;
    }

    private static boolean allowed(Char p) {
        String list = config.getProperty("accounts", "").trim();
        if (list.isEmpty()) {
            return true;
        }
        for (String name : list.split(",")) {
            if (p.user != null && name.trim().equalsIgnoreCase(p.user.username)) {
                return true;
            }
        }
        return false;
    }

    /** Đọc một con số từ cấu hình, dùng chung tệp với phần tự dùng đan. */
    public static int setting(String key, int fallback) {
        reload();
        return number(config.getProperty(key), fallback);
    }

    /** Đọc một danh sách id từ cấu hình. Công khai để màn hình quản lý dùng chung một nguồn. */
    public static List<Integer> ids(String key) {
        reload();
        List<Integer> ids = new ArrayList<>();
        for (String part : config.getProperty(key, "").split(",")) {
            // Không lọc theo id > 0: Đá cấp 1 mang đúng id 0, bỏ qua số 0 là mất luôn thứ
            // người ta hay muốn dọn nhất.
            int id = number(part, -1);
            if (id >= 0) {
                ids.add(Integer.valueOf(id));
            }
        }
        return ids;
    }

    private static int number(String text, int fallback) {
        try {
            return Integer.parseInt(text.trim());
        } catch (Exception ex) {
            return fallback;
        }
    }

    /** Đọc lại tệp cấu hình khi nó vừa được sửa, để đổi danh sách mà không phải khởi động lại. */
    private static synchronized void reload() {
        File file = new File(FILE);
        if (!file.exists()) {
            config = new Properties();
            loadedAt = -1;
            return;
        }
        if (file.lastModified() == loadedAt) {
            return;
        }
        Properties next = new Properties();
        InputStream in = null;
        try {
            in = new FileInputStream(file);
            next.load(in);
            config = next;
            loadedAt = file.lastModified();
        } catch (Exception ex) {
            System.out.println("AutoUseLogin: không đọc được " + FILE + ": " + ex);
        } finally {
            try {
                if (in != null) {
                    in.close();
                }
            } catch (Exception ignored) {
            }
        }
    }
}
