import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.sql.*;
import java.util.*;

/**
 * Bảng tra vật phẩm của bản hồi ức: xem icon, chỉ số, và gửi đồ vào túi nhân vật.
 *
 * Vì sao viết riêng thay vì dùng lại bảng của bản mình: hai máy chủ khác lược đồ từ gốc. Bên hồi
 * ức là `item` với cột `ItemOption` (JSON), `iconID`, tên chỉ số nằm ở bảng `optionitem`; nhân vật
 * ở bảng `ninja` với túi trong cột `ItemBag`. Bên mình là `item` + `item_option` + `players`. Cố
 * nhét chung một bảng điều khiển thì cả hai cùng khó đọc.
 *
 * Mục đích chính là ĐỐI CHIẾU: nhìn xem bên kia có món nào hay để đem về, nên phần xem quan trọng
 * hơn phần sửa. Việc gửi đồ chỉ ghi thẳng vào cột `ItemBag` và CHỈ cho nhân vật đang offline --
 * máy chủ hồi ức giữ nhân vật trong bộ nhớ y như bản mình, ghi đè lúc họ đang chơi là mất.
 *
 *   javac -cp build/hoiuc/lib8 -d build/tools tools/BangHoiUc.java
 *   java -cp build/tools:build/hoiuc/lib8 BangHoiUc [cổng]
 */
public class BangHoiUc {

    private static final String DB = "jdbc:mysql://127.0.0.1:3306/nso_hoiuc"
            + "?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Ho_Chi_Minh";
    private static final String KHO_ANH =
            "thamkhao/ZnsoC 2/ZnsoC/NsoC/res/assets/icon";
    private static String user = "root";
    private static String pass = "";

    /** Tên chỉ số, khoá là mã. Nạp một lần lúc khởi động. */
    private static final Map<Integer, String> TEN_CHI_SO = new HashMap<>();

    public static void main(String[] args) throws Exception {
        int cong = args.length > 0 ? Integer.parseInt(args[0]) : 8770;
        docMatKhau();
        napTenChiSo();
        HttpServer sv = HttpServer.create(new InetSocketAddress("0.0.0.0", cong), 0);
        sv.createContext("/", ex -> {
            String p = ex.getRequestURI().getPath();
            try {
                if ("/".equals(p)) {
                    tra(ex, 200, "text/html; charset=utf-8", TRANG.getBytes(StandardCharsets.UTF_8));
                } else if ("/api/mon".equals(p)) {
                    traJson(ex, danhSachMon(thamSo(ex, "tim")));
                } else if ("/api/nguoi".equals(p)) {
                    traJson(ex, danhSachNguoi());
                } else if ("/api/tang".equals(p)) {
                    traJson(ex, tangMon(Integer.parseInt(thamSo(ex, "nv")),
                            Integer.parseInt(thamSo(ex, "mon")),
                            Integer.parseInt(thamSo(ex, "sl"))));
                } else if ("/anh".equals(p)) {
                    anh(ex, thamSo(ex, "icon"), thamSo(ex, "co"));
                } else {
                    tra(ex, 404, "text/plain; charset=utf-8", "không có".getBytes(StandardCharsets.UTF_8));
                }
            } catch (Exception loi) {
                byte[] b = ("{\"loi\":\"" + thoat(String.valueOf(loi.getMessage())) + "\"}")
                        .getBytes(StandardCharsets.UTF_8);
                try {
                    tra(ex, 400, "application/json; charset=utf-8", b);
                } catch (IOException bo) {
                    // máy khách đã ngắt, không còn gì để nói
                }
            }
        });
        sv.setExecutor(null);
        sv.start();
        System.out.println("Bảng hồi ức: http://127.0.0.1:" + cong + "/");
        System.out.println("             http://100.98.117.102:" + cong + "/");
    }

    /** Mật khẩu lấy từ chính cấu hình máy chủ của mình, khỏi phải gõ lại ở đây. */
    private static void docMatKhau() {
        try {
            for (String d : Files.readAllLines(Paths.get("work/server/NSO_KEM/mysql.properties"))) {
                if (d.startsWith("nsoz.database.user=")) {
                    user = d.substring(d.indexOf('=') + 1).trim();
                } else if (d.startsWith("nsoz.database.pass=")) {
                    pass = d.substring(d.indexOf('=') + 1).trim();
                }
            }
        } catch (Exception bo) {
            System.out.println("không đọc được mysql.properties, dùng root không mật khẩu");
        }
    }

    private static Connection ket() throws SQLException {
        // Trình điều khiển lấy từ thư mục lớp đã bung ra, KHÔNG phải từ một tệp jar, nên thiếu
        // META-INF/services -- java.sql.DriverManager không tự tìm thấy. Gọi tên thẳng một lần.
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException bo) {
            throw new SQLException("thiếu trình điều khiển MySQL trên classpath", bo);
        }
        return DriverManager.getConnection(DB, user, pass);
    }

    private static void napTenChiSo() throws SQLException {
        try (Connection c = ket();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT id, name FROM optionitem")) {
            while (rs.next()) {
                TEN_CHI_SO.put(rs.getInt("id"), rs.getString("name"));
            }
        }
        System.out.println("nạp " + TEN_CHI_SO.size() + " tên chỉ số");
    }

    /**
     * Danh sách vật phẩm, chỉ số đã dịch sang chữ.
     *
     * Cột `ItemOption` là chuỗi JSON dạng [{"id":82,"param":5000}]; tên chỉ số trong `optionitem`
     * có dấu # để thay bằng giá trị, y như bản mình.
     */
    private static String danhSachMon(String tim) throws SQLException {
        StringBuilder sb = new StringBuilder("[");
        String sql = "SELECT id, name, type, level, iconID, description, ItemOption FROM item"
                + (tim == null || tim.isEmpty() ? "" : " WHERE name LIKE ? OR id = ?")
                + " ORDER BY id";
        try (Connection c = ket(); PreparedStatement st = c.prepareStatement(sql)) {
            if (tim != null && !tim.isEmpty()) {
                st.setString(1, "%" + tim + "%");
                st.setInt(2, so(tim));
            }
            try (ResultSet rs = st.executeQuery()) {
                boolean dau = true;
                while (rs.next()) {
                    if (!dau) {
                        sb.append(',');
                    }
                    dau = false;
                    sb.append("{\"id\":").append(rs.getInt("id"))
                      .append(",\"ten\":\"").append(thoat(rs.getString("name")))
                      .append("\",\"loai\":").append(rs.getInt("type"))
                      .append(",\"cap\":").append(rs.getInt("level"))
                      .append(",\"icon\":").append(rs.getInt("iconID"))
                      .append(",\"mota\":\"").append(thoat(rs.getString("description")))
                      .append("\",\"chiSo\":[");
                    boolean d2 = true;
                    for (String d : chiSo(rs.getString("ItemOption"))) {
                        if (!d2) {
                            sb.append(',');
                        }
                        d2 = false;
                        sb.append('"').append(thoat(d)).append('"');
                    }
                    sb.append("]}");
                }
            }
        }
        return sb.append(']').toString();
    }

    /** Dịch chuỗi JSON chỉ số thành các dòng chữ đọc được. */
    private static List<String> chiSo(String json) {
        List<String> ra = new ArrayList<>();
        if (json == null) {
            return ra;
        }
        // Không kéo cả thư viện JSON vào chỉ để đọc hai khoá: bóc bằng tay cho gọn.
        for (String khoi : json.split("\\},")) {
            Integer ma = null;
            Integer gt = null;
            for (String cap : khoi.replaceAll("[\\[\\]{}\"]", "").split(",")) {
                String[] kv = cap.split(":");
                if (kv.length != 2) {
                    continue;
                }
                if (kv[0].trim().equals("id")) {
                    ma = so(kv[1]);
                } else if (kv[0].trim().equals("param")) {
                    gt = so(kv[1]);
                }
            }
            if (ma == null) {
                continue;
            }
            String ten = TEN_CHI_SO.get(ma);
            if (ten == null) {
                ten = "chỉ số " + ma + ": #";
            }
            ra.add(ten.replace("#", String.valueOf(gt == null ? 0 : gt)));
        }
        return ra;
    }

    private static String danhSachNguoi() throws SQLException {
        StringBuilder sb = new StringBuilder("[");
        try (Connection c = ket();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(
                     "SELECT n.id, n.name, n.level, p.online FROM ninja n"
                     + " LEFT JOIN player p ON p.id = n.id ORDER BY n.level DESC LIMIT 300")) {
            boolean dau = true;
            while (rs.next()) {
                if (!dau) {
                    sb.append(',');
                }
                dau = false;
                sb.append("{\"id\":").append(rs.getInt("id"))
                  .append(",\"ten\":\"").append(thoat(rs.getString("name")))
                  .append("\",\"cap\":").append(rs.getInt("level"))
                  .append(",\"online\":").append(rs.getInt("online") == 1).append('}');
            }
        }
        return sb.append(']').toString();
    }

    /**
     * Bỏ món vào túi nhân vật bằng cách nối thẳng vào cột ItemBag.
     *
     * CHỈ cho người đang offline. Máy chủ hồi ức giữ nhân vật trong bộ nhớ và ghi đè cả cột khi họ
     * thoát, nên sửa lúc đang chơi là mất trắng.
     *
     * Món dựng theo đúng hình dạng bên đó: chỉ số chép nguyên từ `item.ItemOption`, cấp nâng cấp
     * để 0 -- đây là công cụ xem thử, không phải chỗ phát đồ mạnh.
     */
    private static String tangMon(int nv, int mon, int sl) throws SQLException {
        if (sl <= 0) {
            sl = 1;
        }
        try (Connection c = ket()) {
            try (PreparedStatement st = c.prepareStatement(
                    "SELECT p.online FROM ninja n LEFT JOIN player p ON p.id = n.id WHERE n.id = ?")) {
                st.setInt(1, nv);
                try (ResultSet rs = st.executeQuery()) {
                    if (!rs.next()) {
                        return "{\"loi\":\"không có nhân vật này\"}";
                    }
                    if (rs.getInt("online") == 1) {
                        return "{\"loi\":\"nhân vật đang online -- máy chủ giữ túi trong bộ nhớ,"
                                + " ghi vào lúc này là mất khi họ thoát\"}";
                    }
                }
            }
            String opt = "[]";
            String ten = "món " + mon;
            try (PreparedStatement st = c.prepareStatement(
                    "SELECT name, ItemOption FROM item WHERE id = ?")) {
                st.setInt(1, mon);
                try (ResultSet rs = st.executeQuery()) {
                    if (!rs.next()) {
                        return "{\"loi\":\"không có vật phẩm mã " + mon + "\"}";
                    }
                    ten = rs.getString("name");
                    String o = rs.getString("ItemOption");
                    if (o != null && !o.isEmpty()) {
                        opt = o;
                    }
                }
            }
            String tui;
            try (PreparedStatement st = c.prepareStatement("SELECT ItemBag FROM ninja WHERE id = ?")) {
                st.setInt(1, nv);
                try (ResultSet rs = st.executeQuery()) {
                    rs.next();
                    tui = rs.getString("ItemBag");
                }
            }
            if (tui == null || tui.trim().isEmpty()) {
                tui = "[]";
            }
            int chiSoO = demO(tui);
            StringBuilder them = new StringBuilder();
            for (int i = 0; i < sl; i++) {
                them.append(them.length() == 0 && tui.trim().equals("[]") ? "" : ",")
                    .append("{\"isLock\":false,\"sale\":0,\"quantity\":1,\"upgrade\":0,\"index\":")
                    .append(chiSoO + i)
                    .append(",\"id\":").append(mon)
                    .append(",\"sys\":0,\"isExpires\":false,\"option\":").append(opt).append('}');
            }
            String moi = tui.trim().equals("[]")
                    ? "[" + them.substring(them.charAt(0) == ',' ? 1 : 0) + "]"
                    : tui.trim().substring(0, tui.trim().length() - 1) + them + "]";
            try (PreparedStatement st = c.prepareStatement("UPDATE ninja SET ItemBag = ? WHERE id = ?")) {
                st.setString(1, moi);
                st.setInt(2, nv);
                st.executeUpdate();
            }
            return "{\"ok\":\"đã bỏ " + sl + " " + thoat(ten) + " vào túi (ô " + chiSoO + " trở đi)\"}";
        }
    }

    /** Đếm số món trong chuỗi túi, để đánh chỉ số ô cho món mới. */
    private static int demO(String tui) {
        int n = 0;
        for (int i = 0; i < tui.length(); i++) {
            if (tui.charAt(i) == '{') {
                int sau = 0;
                for (int j = 0; j < i; j++) {
                    if (tui.charAt(j) == '{') {
                        sau++;
                    } else if (tui.charAt(j) == '}') {
                        sau--;
                    }
                }
                if (sau == 0) {
                    n++;
                }
            }
        }
        return n;
    }

    private static void anh(HttpExchange ex, String icon, String co) throws IOException {
        int id = so(icon) == null ? -1 : so(icon);
        String z = co == null || co.isEmpty() ? "2" : co;
        File f = new File(KHO_ANH + File.separator + z, "Small" + id + ".png");
        if (!f.isFile()) {
            f = new File(KHO_ANH + File.separator + z, id + ".png");
        }
        if (!f.isFile()) {
            tra(ex, 404, "text/plain", "không có ảnh".getBytes(StandardCharsets.UTF_8));
            return;
        }
        byte[] b = Files.readAllBytes(f.toPath());
        ex.getResponseHeaders().add("Cache-Control", "max-age=86400");
        tra(ex, 200, "image/png", b);
    }

    private static Integer so(String s) {
        try {
            return Integer.valueOf(s.trim());
        } catch (Exception bo) {
            return null;
        }
    }

    private static String thamSo(HttpExchange ex, String ten) {
        String q = ex.getRequestURI().getQuery();
        if (q == null) {
            return "";
        }
        for (String c : q.split("&")) {
            int i = c.indexOf('=');
            if (i > 0 && c.substring(0, i).equals(ten)) {
                try {
                    return java.net.URLDecoder.decode(c.substring(i + 1), "UTF-8");
                } catch (Exception bo) {
                    return c.substring(i + 1);
                }
            }
        }
        return "";
    }

    private static String thoat(String s) {
        return s == null ? "" : s.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", " ").replace("\r", " ");
    }

    private static void traJson(HttpExchange ex, String json) throws IOException {
        tra(ex, 200, "application/json; charset=utf-8", json.getBytes(StandardCharsets.UTF_8));
    }

    private static void tra(HttpExchange ex, int ma, String kieu, byte[] than) throws IOException {
        ex.getResponseHeaders().add("Content-Type", kieu);
        ex.sendResponseHeaders(ma, than.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(than);
        }
    }

    private static final String TRANG = TrangHoiUc.HTML;
}
