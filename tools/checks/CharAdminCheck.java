import com.nsoz.admin.CharAdmin;
import com.nsoz.db.jdbc.DbManager;
import com.nsoz.item.ItemManager;
import com.nsoz.server.Config;
import com.nsoz.server.GameData;
import com.nsoz.store.StoreManager;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * Kiểm bảng điều khiển nhân vật trên dữ liệu thật.
 *
 * Chạy độc lập với máy chủ, đúng kiểu các công cụ khác trong thư mục này. Việc nó làm:
 *
 * <ol>
 *   <li>In ra toàn bộ những gì màn hình sẽ hiện cho một nhân vật có thật -- trang bị từng ô kèm
 *       chỉ số từng dòng, hành trang, rương, kỹ năng, tiềm năng. Đây là phần chứng minh "đọc
 *       đúng": số in ra phải khớp với những gì người chơi thấy trong game.</li>
 *   <li>Chạy thử từng thao tác ghi lên một nhân vật <b>offline</b>, và sau mỗi lần ghi thì
 *       <b>đọc lại từ cơ sở dữ liệu</b> để đối chiếu, chứ không tin vào giá trị hàm trả về.</li>
 *   <li>Trả nguyên trạng thái ban đầu về chỗ cũ. Kiểm mà để lại dấu vết thì lần chạy sau không
 *       còn so sánh được với gì nữa.</li>
 * </ol>
 *
 * Cách chạy (từ thư mục work/server/NSO_KEM, vì Config đọc config.properties ở thư mục hiện tại):
 * <pre>
 *   java -cp "../../../build/srvcls:target/Nso-jar-with-dependencies.jar" CharAdminCheck [id]
 * </pre>
 */
public class CharAdminCheck {

    public static void main(String[] args) throws Exception {
        if (!Config.getInstance().load() || !DbManager.start()) {
            System.out.println("!! không nạp được cấu hình hoặc CSDL");
            return;
        }
        // Thứ tự bắt buộc, chép đúng Server.init. Thiếu init() thì mọi cửa hàng rỗng và món nào
        // cũng trông như không có chỉ số.
        ItemManager.getInstance().load();
        GameData.getInstance().init();
        StoreManager.getInstance().init();
        StoreManager.getInstance().load();
        ItemManager.getInstance().init();

        System.out.println("\n=== DANH SÁCH NHÂN VẬT ===");
        JSONArray ds = CharAdmin.list();
        for (Object o : ds) {
            JSONObject c = (JSONObject) o;
            System.out.printf("  #%-3s %-16s cấp %-4s %-16s %s%n",
                    c.get("id"), c.get("name"), c.get("level"), c.get("className"),
                    Boolean.TRUE.equals(c.get("online")) ? "ĐANG ONLINE" : "offline");
        }
        if (ds.isEmpty()) {
            System.out.println("  (không có ai)");
            return;
        }

        int id = args.length > 0 ? Integer.parseInt(args[0]) : chonOffline(ds);
        if (id < 0) {
            System.out.println("\n!! không có nhân vật nào offline để kiểm phần ghi");
            return;
        }

        inNhanVat(CharAdmin.view(id));
        kiemGhi(id);
        System.exit(0);
    }

    /** Ưu tiên nhân vật cấp cao nhất đang offline: nhiều đồ nhất, nên soi được nhiều nhất. */
    private static int chonOffline(JSONArray ds) {
        int id = -1, cao = -1;
        for (Object o : ds) {
            JSONObject c = (JSONObject) o;
            if (Boolean.TRUE.equals(c.get("online"))) {
                continue;
            }
            int lv = i(c.get("level"));
            if (lv > cao) {
                cao = lv;
                id = i(c.get("id"));
            }
        }
        return id;
    }

    // -------------------------------------------------------------- phần đọc

    private static void inNhanVat(JSONObject c) {
        System.out.println("\n=== " + c.get("name") + " (#" + c.get("id") + ") ===");
        System.out.println("  nguồn số liệu : " + c.get("nguon")
                + (Boolean.TRUE.equals(c.get("online")) ? "  [ĐANG ONLINE]" : ""));
        System.out.println("  phái          : " + c.get("className")
                + ", giới tính " + c.get("gender"));
        System.out.println("  cấp độ        : " + c.get("level") + ", exp " + c.get("exp"));
        System.out.println("  xu/yên/lượng  : " + c.get("xu") + " / " + c.get("yen") + " / " + c.get("luong"));

        JSONArray pot = (JSONArray) c.get("potential");
        System.out.println("  tiềm năng     :");
        for (int k = 0; k < 4; k++) {
            System.out.printf("      %-10s %8s   (%s)%n",
                    CharAdmin.POTENTIAL_NAME[k], pot.get(k), CharAdmin.POTENTIAL_NOTE[k]);
        }
        System.out.println("      đã chia " + (i(c.get("potentialTong")) - i(c.get("point")))
                + ", chưa dùng " + c.get("point") + ", trần theo cấp " + c.get("potentialTran"));
        System.out.println("  điểm kỹ năng  : chưa dùng " + c.get("spoint")
                + ", trần theo cấp " + c.get("spointTran"));
        System.out.println("  lượt tẩy      : tiềm năng " + c.get("tayTiemNang")
                + ", kỹ năng " + c.get("tayKyNang"));

        inMon("TRANG BỊ ĐANG MẶC", (JSONArray) c.get("equip"), true);
        inMon("THỜI TRANG ĐANG MẶC", (JSONArray) c.get("fashion"), true);
        inMon("HÀNH TRANG (" + ((JSONArray) c.get("bag")).size() + "/" + c.get("numberCellBag") + " ô)",
                (JSONArray) c.get("bag"), false);
        inMon("RƯƠNG (" + ((JSONArray) c.get("box")).size() + "/" + c.get("numberCellBox") + " ô)",
                (JSONArray) c.get("box"), false);

        JSONArray sk = (JSONArray) c.get("skill");
        System.out.println("\n  --- KỸ NĂNG (" + sk.size() + ") ---");
        for (Object o : sk) {
            JSONObject s = (JSONObject) o;
            System.out.printf("      #%-3s %-28s cấp %s/%s%s%n", s.get("id"), s.get("name"),
                    s.get("point"), s.get("maxPoint"),
                    Boolean.TRUE.equals(s.get("clone")) ? "   (phân thân)" : "");
        }
    }

    private static void inMon(String tieuDe, JSONArray ds, boolean laTrangBi) {
        System.out.println("\n  --- " + tieuDe + " ---");
        if (ds.isEmpty()) {
            System.out.println("      (trống)");
            return;
        }
        for (Object o : ds) {
            JSONObject m = (JSONObject) o;
            JSONArray opt = (JSONArray) m.get("options");
            System.out.printf("      %-13s %-32s mã %-5s cấp %-4s +%-3s hệ %-2s %s%s%n",
                    laTrangBi ? m.get("slotName") : "ô " + m.get("slot"),
                    m.get("name"), m.get("id"), m.get("level"), m.get("upgrade"), m.get("sys"),
                    i(m.get("quantity")) > 1 ? "x" + m.get("quantity") + " " : "",
                    opt.isEmpty() ? "(không chỉ số)" : "(" + opt.size() + " chỉ số)");
            for (Object p : opt) {
                System.out.println("            · " + ((JSONObject) p).get("text"));
            }
            JSONArray gems = (JSONArray) m.get("gems");
            for (Object g : gems) {
                JSONObject j = (JSONObject) g;
                System.out.println("            ◆ ngọc " + j.get("name") + " +" + j.get("upgrade"));
            }
        }
    }

    // -------------------------------------------------------------- phần ghi

    /**
     * Chạy từng thao tác rồi đọc lại thẳng từ bảng.
     *
     * Đọc lại bằng SQL trần chứ không gọi lại CharAdmin.view: nếu hàm đọc và hàm ghi cùng hiểu sai
     * một chỗ thì so hai bên vẫn khớp mà dữ liệu thật vẫn sai. Bảng là trọng tài.
     */
    private static void kiemGhi(int id) throws Exception {
        String[] goc = docTho(id);
        System.out.println("\n=== KIỂM PHẦN GHI trên #" + id + " (offline) ===");
        System.out.println("  trạng thái gốc: point=" + goc[0] + " spoint=" + goc[1]
                + " potential=" + goc[2]);

        int diem = 0, tong = 0;

        // 1. Cộng điểm tiềm năng
        tong++;
        int truoc = Integer.parseInt(goc[0]);
        CharAdmin.addPotentialPoint(id, 7);
        diem += ktra("cộng 7 điểm tiềm năng", docTho(id)[0], String.valueOf(truoc + 7));

        // 2. Dồn điểm vào ô Thể lực rồi rút ra, kiểm cả hai chiều
        tong++;
        int[] potTruoc = mangSo(goc[2]);
        CharAdmin.spendPotential(id, 2, 5);
        String[] sau = docTho(id);
        diem += ktra("dồn 5 điểm vào Thể lực",
                mangSo(sau[2])[2] + "/" + sau[0],
                (potTruoc[2] + 5) + "/" + (truoc + 7 - 5));

        tong++;
        CharAdmin.spendPotential(id, 2, -5);
        sau = docTho(id);
        diem += ktra("rút 5 điểm khỏi Thể lực",
                mangSo(sau[2])[2] + "/" + sau[0],
                potTruoc[2] + "/" + (truoc + 7));

        // 3. Cộng điểm kỹ năng
        tong++;
        int spTruoc = Integer.parseInt(goc[1]);
        CharAdmin.addSkillPoint(id, 3);
        diem += ktra("cộng 3 điểm kỹ năng", docTho(id)[1], String.valueOf(spTruoc + 3));

        // 4. Nâng một kỹ năng chưa kịch trần, rồi hạ lại
        JSONObject xem = CharAdmin.view(id);
        JSONObject knThu = timKyNangNangDuoc(xem);
        if (knThu == null) {
            System.out.println("  · bỏ qua phần nâng kỹ năng: không có kỹ năng nào còn nâng được");
        } else {
            int sid = i(knThu.get("id"));
            int capTruoc = i(knThu.get("point"));
            tong++;
            CharAdmin.upSkill(id, sid, 1);
            diem += ktra("nâng " + knThu.get("name") + " lên cấp " + (capTruoc + 1),
                    capKyNang(id, sid) + "/" + docTho(id)[1],
                    (capTruoc + 1) + "/" + (spTruoc + 3 - 1));

            tong++;
            CharAdmin.upSkill(id, sid, -1);
            diem += ktra("hạ " + knThu.get("name") + " về cấp " + capTruoc,
                    capKyNang(id, sid) + "/" + docTho(id)[1],
                    capTruoc + "/" + (spTruoc + 3));
        }

        // 5. Tẩy tiềm năng: bốn ô phải về đúng mức khởi đầu của phái, kho điểm phải đầy
        tong++;
        int classId = i(xem.get("classId"));
        int[] nen = CharAdmin.basePotential(classId);
        int tran = i(xem.get("potentialTran"));
        CharAdmin.resetPotential(id);
        sau = docTho(id);
        diem += ktra("tẩy điểm tiềm năng",
                sau[2].replace(" ", "") + "/" + sau[0],
                java.util.Arrays.toString(nen).replace(" ", "") + "/" + tran);

        // 6. Tẩy kỹ năng: mọi kỹ năng về cấp 1 (trừ phân thân), kho điểm về trần
        tong++;
        int spTran = i(xem.get("spointTran"));
        CharAdmin.resetSkill(id);
        sau = docTho(id);
        diem += ktra("tẩy điểm kỹ năng",
                moiKyNangCap1(id) + "/" + sau[1], "true/" + spTran);

        // 7. Nâng hết kỹ năng lên tối đa
        tong++;
        CharAdmin.maxSkill(id);
        diem += ktra("nâng hết kỹ năng lên tối đa", moiKyNangKichTran(id) + "/" + docTho(id)[1],
                "true/0");

        traLai(id, goc);
        String[] cuoi = docTho(id);
        tong++;
        diem += ktra("trả lại nguyên trạng",
                cuoi[0] + "|" + cuoi[1] + "|" + cuoi[2].replace(" ", "") + "|" + cuoi[3],
                goc[0] + "|" + goc[1] + "|" + goc[2].replace(" ", "") + "|" + goc[3]);

        System.out.println("\n  ===> " + diem + "/" + tong + " phép kiểm đạt");
        if (diem != tong) {
            System.exit(1);
        }
    }

    private static int ktra(String ten, String thucTe, String mongDoi) {
        boolean dat = thucTe.equals(mongDoi);
        System.out.printf("  %s %-46s %s%n", dat ? "[ĐẠT ]" : "[HỎNG]", ten,
                dat ? "= " + thucTe : "được " + thucTe + ", mong đợi " + mongDoi);
        return dat ? 1 : 0;
    }

    private static JSONObject timKyNangNangDuoc(JSONObject c) {
        for (Object o : (JSONArray) c.get("skill")) {
            JSONObject s = (JSONObject) o;
            if (Boolean.TRUE.equals(s.get("clone"))) {
                continue;
            }
            int max = i(s.get("maxPoint"));
            if (max > 0 && i(s.get("point")) < max) {
                return s;
            }
        }
        return null;
    }

    // ------------------------------------------------- đọc thẳng từ bảng

    /** {point, spoint, potential, skill} đọc trần từ bảng players. */
    private static String[] docTho(int id) throws Exception {
        try (Connection conn = DbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT `point`, `spoint`, `potential`, `skill` FROM `players`"
                             + " WHERE `id` = ? LIMIT 1;")) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                rs.next();
                return new String[] { rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4) };
            }
        }
    }

    private static void traLai(int id, String[] goc) throws Exception {
        try (Connection conn = DbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "UPDATE `players` SET `point` = ?, `spoint` = ?, `potential` = ?, `skill` = ?"
                             + " WHERE `id` = ? LIMIT 1;")) {
            stmt.setInt(1, Integer.parseInt(goc[0]));
            stmt.setInt(2, Integer.parseInt(goc[1]));
            stmt.setString(3, goc[2]);
            stmt.setString(4, goc[3]);
            stmt.setInt(5, id);
            stmt.executeUpdate();
        }
    }

    private static int capKyNang(int id, int skillId) throws Exception {
        JSONArray arr = (JSONArray) org.json.simple.JSONValue.parse(docTho(id)[3]);
        for (Object o : arr) {
            JSONObject s = (JSONObject) o;
            if (i(s.get("id")) == skillId) {
                return i(s.get("point"));
            }
        }
        return -1;
    }

    private static boolean moiKyNangCap1(int id) throws Exception {
        JSONArray arr = (JSONArray) org.json.simple.JSONValue.parse(docTho(id)[3]);
        for (Object o : arr) {
            JSONObject s = (JSONObject) o;
            int sid = i(s.get("id"));
            if (sid >= 67 && sid <= 72) {
                continue;   // phân thân giữ nguyên, đúng như Char.tayKyNang
            }
            if (i(s.get("point")) != 1) {
                return false;
            }
        }
        return true;
    }

    private static boolean moiKyNangKichTran(int id) throws Exception {
        JSONObject c = CharAdmin.view(id);
        for (Object o : (JSONArray) c.get("skill")) {
            JSONObject s = (JSONObject) o;
            if (Boolean.TRUE.equals(s.get("clone"))) {
                continue;
            }
            int max = i(s.get("maxPoint"));
            if (max > 0 && i(s.get("point")) != max) {
                return false;
            }
        }
        return true;
    }

    private static int[] mangSo(String json) {
        JSONArray arr = (JSONArray) org.json.simple.JSONValue.parse(json);
        int[] out = new int[4];
        for (int k = 0; k < 4 && k < arr.size(); k++) {
            out[k] = i(arr.get(k));
        }
        return out;
    }

    private static int i(Object o) {
        return o == null ? 0 : (int) Double.parseDouble(o.toString());
    }
}
