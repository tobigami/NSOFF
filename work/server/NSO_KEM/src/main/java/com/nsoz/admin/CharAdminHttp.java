package com.nsoz.admin;

import com.nsoz.db.jdbc.DbManager;
import com.nsoz.item.ItemManager;
import com.nsoz.server.Config;
import com.nsoz.server.GameData;
import com.nsoz.store.StoreManager;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;

/**
 * Bảng điều khiển nhân vật: một trang web nhỏ nói chuyện với {@link CharAdmin}.
 *
 * <h3>Vì sao là trang web chứ không phải menu trong game</h3>
 * Mấy màn quản trị sẵn có ({@code BagAdmin}, {@code GearAdmin}, {@code TeleportAdmin}) đều duyệt
 * {@code ServerManager.getChars()}, nghĩa là chỉ thấy người đang online -- đúng thứ cần tránh ở
 * đây. Menu trong game thì còn một giới hạn nữa: giao thức chỉ có danh sách dòng chữ ngắn, mà
 * một nhân vật cấp cao có mười món trang bị, mỗi món mười bốn dòng chỉ số, cộng ba mươi ô hành
 * trang và ba mươi ô rương -- xem hết bằng menu là chuyện không làm nổi. Trang web hiện được cả
 * bảng, và quan trọng hơn: nó không cần ai đăng nhập vào game cả.
 *
 * <h3>Hai cách chạy</h3>
 * <ul>
 *   <li><b>Độc lập</b> ({@link #main}): tiến trình riêng, chỉ nói chuyện với MySQL. Không cần
 *       khởi động lại máy chủ, không đụng gì tới người đang chơi. Sửa được nhân vật offline; gặp
 *       nhân vật đang online thì từ chối ghi và nói rõ lý do.</li>
 *   <li><b>Trong máy chủ</b> ({@link #start}): {@code Server} gọi lúc khởi động. Lúc này sửa được
 *       cả người đang online, vì với tới được đối tượng {@code Char} trong bộ nhớ.</li>
 * </ul>
 *
 * <h3>Chỉ nghe trong nhà</h3>
 * Không bao giờ gắn vào 0.0.0.0. Chỉ gắn vào 127.0.0.1 và địa chỉ Tailscale (dải 100.64.0.0/10),
 * cùng nguyên tắc với {@code tools/run-share.sh}. Địa chỉ Tailscale dò bằng
 * {@link NetworkInterface} chứ không gọi lệnh {@code tailscale} -- không phụ thuộc lệnh ngoài,
 * và không có Tailscale thì vẫn chạy được ở localhost.
 */
public final class CharAdminHttp {

    public static final int PORT_MAC_DINH = 8765;

    /**
     * Cổng thật sự dùng, lấy từ biến môi trường NSO_ADMIN_PORT nếu có.
     *
     * Phải đọc từ môi trường chứ không ghi cứng theo từng cây nguồn: bản dev và bản chạy thật dùng
     * CHUNG mã nguồn, mỗi lần đồng bộ từ dev sang prod mà mã khác nhau ở chỗ này là ghi đè mất.
     * Khác nhau chỉ nên nằm ở tệp cấu hình và biến môi trường.
     */
    public static int cong() {
        String s = System.getenv("NSO_ADMIN_PORT");
        if (s != null && !s.trim().isEmpty()) {
            try {
                return Integer.parseInt(s.trim());
            } catch (NumberFormatException ignore) {
                // Đặt sai thì dùng cổng mặc định, đừng vì thế mà không mở được bảng điều khiển.
            }
        }
        return PORT_MAC_DINH;
    }

    /** Dải CGNAT mà Tailscale cấp cho máy trong tailnet. */
    private static final byte TAILSCALE_PREFIX = 100;

    private static final List<HttpServer> dangChay = new ArrayList<>();

    private CharAdminHttp() {
    }

    // -------------------------------------------------------------- khởi động

    /**
     * Chạy độc lập, không cần máy chủ game.
     *
     * Thứ tự nạp là thứ tự bắt buộc chép từ {@code Server.init}: {@code ItemManager.load} rồi
     * {@code GameData.init} rồi {@code StoreManager.init} rồi {@code StoreManager.load}. Thiếu
     * {@code init()} thì mọi cửa hàng rỗng và món nào cũng trông như không có chỉ số.
     */
    public static void main(String[] args) throws Exception {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : cong();
        if (!Config.getInstance().load() || !DbManager.start()) {
            System.out.println("!! không nạp được cấu hình hoặc CSDL");
            System.exit(1);
        }
        ItemManager.getInstance().load();
        GameData.getInstance().init();
        StoreManager.getInstance().init();
        StoreManager.getInstance().load();
        ItemManager.getInstance().init();

        start(port);
        System.out.println("Chế độ độc lập: chỉ sửa được nhân vật offline.");
        System.out.println("Ctrl+C để dừng.");
        // Bể xử lý toàn luồng nền nên phải giữ luồng chính sống, không thì JVM thoát ngay.
        Thread.currentThread().join();
    }

    /** Máy chủ game gọi hàm này; từ đây trở đi sửa được cả nhân vật đang online. */
    public static void startInServer(int port) {
        CharAdmin.markInServer();
        try {
            start(port);
        } catch (Exception ex) {
            System.out.println("CharAdminHttp: không mở được bảng điều khiển: " + ex);
        }
    }

    /** Mở cổng nghe trên mọi địa chỉ trong nhà tìm được. Trả về số cổng đã mở. */
    public static synchronized void start(int port) throws IOException {
        if (!dangChay.isEmpty()) {
            System.out.println("CharAdminHttp: đã chạy rồi.");
            return;
        }
        for (InetAddress addr : diaChiTrongNha()) {
            try {
                HttpServer server = HttpServer.create(new InetSocketAddress(addr, port), 0);
                server.createContext("/", new Trang());
                server.setExecutor(Executors.newFixedThreadPool(4, luongNen()));
                server.start();
                dangChay.add(server);
                System.out.println("Bảng điều khiển nhân vật: http://" + addr.getHostAddress() + ":" + port + "/");
            } catch (IOException ex) {
                System.out.println("CharAdminHttp: bỏ qua " + addr.getHostAddress() + " (" + ex.getMessage() + ")");
            }
        }
        if (dangChay.isEmpty()) {
            throw new IOException("Không gắn được vào địa chỉ nào ở cổng " + port);
        }
    }

    /**
     * Luồng nền (daemon) cho bể xử lý.
     *
     * Máy chủ game thoát bằng System.exit ở đường bình thường, nhưng nếu có đường nào thoát bằng
     * cách để hàm main chạy hết thì một bể luồng thường sẽ giữ JVM sống mãi -- và lúc đó dữ liệu
     * người chơi chưa kịp lưu. Bảng điều khiển là thứ phụ, không được phép cản việc tắt máy chủ.
     */
    private static java.util.concurrent.ThreadFactory luongNen() {
        return r -> {
            Thread t = new Thread(r, "char-admin");
            t.setDaemon(true);
            return t;
        };
    }

    public static synchronized void stop() {
        for (HttpServer s : dangChay) {
            s.stop(0);
        }
        dangChay.clear();
        System.out.println("Bảng điều khiển nhân vật đã dừng.");
    }

    public static synchronized boolean isRunning() {
        return !dangChay.isEmpty();
    }

    /**
     * Localhost, cộng địa chỉ Tailscale nếu có.
     *
     * Cố tình không có 0.0.0.0 trong danh sách: gắn vào đó là cả wifi nhà lẫn wifi quán cà phê
     * đều mò vào sửa nhân vật được, mà trang này không có mật khẩu.
     */
    private static List<InetAddress> diaChiTrongNha() throws IOException {
        List<InetAddress> out = new ArrayList<>();
        out.add(InetAddress.getByName("127.0.0.1"));
        for (NetworkInterface ni : Collections.list(NetworkInterface.getNetworkInterfaces())) {
            if (!ni.isUp()) {
                continue;
            }
            for (InetAddress addr : Collections.list(ni.getInetAddresses())) {
                if (!(addr instanceof Inet4Address)) {
                    continue;
                }
                byte[] b = addr.getAddress();
                // 100.64.0.0/10: byte đầu là 100 và sáu bit cao của byte thứ hai nằm trong 64..127.
                if (b[0] == TAILSCALE_PREFIX && (b[1] & 0xC0) == 0x40) {
                    out.add(addr);
                }
            }
        }
        return out;
    }

    // ------------------------------------------------------------- xử lý yêu cầu

    private static final class Trang implements HttpHandler {

        @Override
        public void handle(HttpExchange ex) throws IOException {
            String path = ex.getRequestURI().getPath();
            try {
                switch (path) {
                    case "/":
                    case "/index.html":
                        traVe(ex, 200, "text/html; charset=utf-8", trangHtml());
                        return;
                    case "/api/meta": {
                        JSONObject o = CharAdmin.meta();
                        o.put("danhSach", CharAdmin.list());
                        traJson(ex, 200, o.toJSONString());
                        return;
                    }
                    case "/api/char": {
                        int id = Integer.parseInt(thamSo(ex.getRequestURI()).getOrDefault("id", "0"));
                        traJson(ex, 200, CharAdmin.view(id).toJSONString());
                        return;
                    }
                    case "/anh": {
                        Map<String, String> t = thamSo(ex.getRequestURI());
                        anh(ex, t.getOrDefault("icon", "-1"), t.getOrDefault("co", "2"));
                        return;
                    }
                    case "/api/items":
                        traJson(ex, 200, CharAdmin.danhMucMon().toJSONString());
                        return;
                    case "/api/do":
                        traJson(ex, 200, thucHien(docThan(ex)).toJSONString());
                        return;
                    default:
                        traVe(ex, 404, "text/plain; charset=utf-8",
                                "Không có trang này".getBytes(StandardCharsets.UTF_8));
                }
            } catch (Exception loi) {
                JSONObject o = new JSONObject();
                o.put("ok", false);
                o.put("message", loi.getMessage() == null ? loi.toString() : loi.getMessage());
                try {
                    traJson(ex, 400, o.toJSONString());
                } catch (IOException ignore) {
                }
            }
        }

        /**
         * Một cổng vào duy nhất cho mọi thao tác ghi.
         *
         * Gom về một chỗ để nhánh online/offline chỉ phải quyết định một lần, trong
         * {@code CharAdmin}, thay vì mỗi đường dẫn lại tự nhớ phải kiểm tra.
         */
        /** Chuỗi trong yêu cầu; null thành rỗng để nơi nhận khỏi phải tự chống null. */
        private static String chuoi(Object o) {
            return o == null ? "" : String.valueOf(o);
        }

        private JSONObject thucHien(String body) throws Exception {
            Object parsed = JSONValue.parse(body);
            if (!(parsed instanceof JSONObject)) {
                throw new IllegalArgumentException("Yêu cầu không hợp lệ.");
            }
            JSONObject req = (JSONObject) parsed;
            int id = so(req.get("id"));
            String op = String.valueOf(req.get("op"));
            switch (op) {
                case "themDiemTiemNang":
                    return CharAdmin.addPotentialPoint(id, so(req.get("value")));
                case "themDiemKyNang":
                    return CharAdmin.addSkillPoint(id, so(req.get("value")));
                case "chiaTiemNang":
                    return CharAdmin.spendPotential(id, so(req.get("index")), so(req.get("value")));
                case "datTiemNang": {
                    org.json.simple.JSONArray arr = (org.json.simple.JSONArray) req.get("value");
                    int[] v = new int[4];
                    for (int i = 0; i < 4; i++) {
                        v[i] = so(arr.get(i));
                    }
                    return CharAdmin.setPotential(id, v);
                }
                case "tayTiemNang":
                    return CharAdmin.resetPotential(id);
                case "tayKyNang":
                    return CharAdmin.resetSkill(id);
                case "doiChiSoBiKip":
                    return CharAdmin.doiChiSoBiKip(id, so(req.get("soDong")), chuoi(req.get("ai")));
                case "tinhLuyenBiKip":
                    return CharAdmin.tinhLuyenBiKip(id, so(req.get("soLan")), chuoi(req.get("ai")));
                case "tinhLuyenMon":
                    return CharAdmin.tinhLuyenMon(id, chuoi(req.get("ai")), chuoi(req.get("kho")),
                            so(req.get("slot")), so(req.get("soLan")));
                case "doiToaDo":
                    return CharAdmin.doiToaDo(id, so(req.get("map")), so(req.get("x")),
                            so(req.get("y")), Boolean.TRUE.equals(req.get("macDinh")));
                case "datDiemLuu":
                    return CharAdmin.datDiemLuu(id, so(req.get("map")));
                case "datPhaTran":
                    return CharAdmin.datPhaTran(id, so(req.get("maChieu")), so(req.get("soLan")));
                case "quenKyNang":
                    return CharAdmin.quenKyNang(id, so(req.get("skill")));
                case "napKyNang":
                    return CharAdmin.upSkill(id, so(req.get("skill")), so(req.get("value")));
                case "toiDaKyNang":
                    return CharAdmin.maxSkill(id);
                case "chiaTiemNangPhanThan":
                    return CharAdmin.chiaTiemNangPhanThan(id, so(req.get("index")), so(req.get("value")));
                case "napKyNangPhanThan":
                    return CharAdmin.napKyNangPhanThan(id, so(req.get("skill")), so(req.get("value")));
                case "themMon":
                    return CharAdmin.themMon(id, so(req.get("idMon")), so(req.get("soLuong")));
                case "xoaMon":
                    return CharAdmin.xoaMon(id, String.valueOf(req.get("kho")),
                            so(req.get("slot")), so(req.get("soLuong")));
                case "xoaNhieuMon": {
                    org.json.simple.JSONArray a = (org.json.simple.JSONArray) req.get("slots");
                    int[] v = new int[a == null ? 0 : a.size()];
                    for (int i = 0; i < v.length; i++) {
                        v[i] = so(a.get(i));
                    }
                    return CharAdmin.xoaNhieuMon(id, String.valueOf(req.get("kho")), v);
                }
                case "boQuaBuoc":
                    return CharAdmin.boQuaBuoc(id);
                case "boQuaNhiemVu":
                    return CharAdmin.boQuaNhiemVu(id);
                case "nhayToiNhiemVu":
                    return CharAdmin.nhayToiNhiemVu(id, so(req.get("value")));
                case "themLuotTay":
                    return CharAdmin.addResetTicket(id, so(req.get("tiemNang")), so(req.get("kyNang")));
                default:
                    throw new IllegalArgumentException("Không có thao tác " + op);
            }
        }
    }

    // ------------------------------------------------------------- tiện ích

    /**
     * Gửi ảnh biểu tượng của vật phẩm, đọc thẳng từ kho ảnh của máy chủ.
     *
     * Chỉ nhận **số**: id ảnh ghép vào đường dẫn tệp, nên để lọt chuỗi tự do là mở đường đọc mọi
     * tệp trên đĩa. Cỡ cũng chỉ nhận 1 tới 4, đúng bốn mức thu phóng đang có.
     *
     * Không có ảnh thì trả 404 chứ không trả ảnh rỗng -- để phía web tự vẽ ô trống, khỏi tốn một
     * lượt tải cho thứ không tồn tại.
     */
    private static void anh(HttpExchange ex, String icon, String co) throws IOException {
        int id;
        int z;
        try {
            id = Integer.parseInt(icon.trim());
            z = Integer.parseInt(co.trim());
        } catch (NumberFormatException e) {
            traVe(ex, 400, "text/plain; charset=utf-8", "id ảnh phải là số".getBytes(StandardCharsets.UTF_8));
            return;
        }
        if (id < 0 || z < 1 || z > 4) {
            traVe(ex, 404, "text/plain; charset=utf-8", "không có".getBytes(StandardCharsets.UTF_8));
            return;
        }
        File f = new File(KHO_ANH, z + File.separator + "Small" + id + ".png");
        if (!f.isFile()) {
            traVe(ex, 404, "text/plain; charset=utf-8", "không có".getBytes(StandardCharsets.UTF_8));
            return;
        }
        byte[] than = java.nio.file.Files.readAllBytes(f.toPath());
        ex.getResponseHeaders().add("Cache-Control", "max-age=86400");
        traVe(ex, 200, "image/png", than);
    }

    /** Kho ảnh nhỏ của máy chủ, nơi có cả biểu tượng vật phẩm lẫn mảnh ghép nhân vật. */
    private static final String KHO_ANH = "Data" + File.separator + "Img" + File.separator + "Small";

    private static int so(Object o) {
        if (o == null) {
            return 0;
        }
        return (int) Double.parseDouble(o.toString());
    }

    private static Map<String, String> thamSo(URI uri) {
        Map<String, String> out = new HashMap<>();
        String q = uri.getRawQuery();
        if (q == null) {
            return out;
        }
        for (String phan : q.split("&")) {
            int i = phan.indexOf('=');
            if (i > 0) {
                out.put(phan.substring(0, i), java.net.URLDecoder.decode(phan.substring(i + 1),
                        StandardCharsets.UTF_8));
            }
        }
        return out;
    }

    private static String docThan(HttpExchange ex) throws IOException {
        try (InputStream in = ex.getRequestBody()) {
            return new String(doc(in), StandardCharsets.UTF_8);
        }
    }

    private static void traJson(HttpExchange ex, int code, String json) throws IOException {
        traVe(ex, code, "application/json; charset=utf-8", json.getBytes(StandardCharsets.UTF_8));
    }

    private static void traVe(HttpExchange ex, int code, String kieu, byte[] than) throws IOException {
        ex.getResponseHeaders().set("Content-Type", kieu);
        ex.getResponseHeaders().set("Cache-Control", "no-store");
        ex.sendResponseHeaders(code, than.length);
        try (OutputStream out = ex.getResponseBody()) {
            out.write(than);
        }
    }

    /**
     * Trang HTML nằm cạnh lớp này trong jar.
     *
     * Để rời ra tệp .html chứ không nhúng thành chuỗi Java: sửa giao diện thì không phải thoát
     * dấu nháy, và không phải biên dịch lại chỉ vì đổi một dòng chữ.
     */
    private static byte[] trangHtml() throws IOException {
        try (InputStream in = CharAdminHttp.class.getResourceAsStream("charadmin.html")) {
            if (in == null) {
                return "Thiếu tệp charadmin.html trong jar".getBytes(StandardCharsets.UTF_8);
            }
            return doc(in);
        }
    }

    private static byte[] doc(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int n;
        while ((n = in.read(buf)) > 0) {
            out.write(buf, 0, n);
        }
        return out.toByteArray();
    }
}
