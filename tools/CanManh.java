import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.sql.*;
import java.util.*;
import java.util.List;

/**
 * Căn mảnh trang phục -- xem trước và nắn số lệch của nj_part mà không phải vào game.
 *
 * Nhân vật ghép từ ba mảnh thay được: đầu, thân, chân. Cách client dựng chúng được lấy ra từ bản
 * client không bị làm rối tên trong thư mục gpt, nên cửa sổ này vẽ đúng y như trong game:
 *
 *     int[] o = CharInfo[cf][slot];              // slot 0 đầu, 1 chân, 2 thân, 3 mảnh phụ
 *     PartImage p = manh.pi[o[0]];
 *     SmallImage.gameAB(g, p.id, cx + o[1] + p.dx, cy - o[2] + p.dy - 10, 0, 0);
 *
 * Số cuối là neo 0, tức góc trái-trên: ảnh rộng hẹp khác nhau là lệch ngang, đây chính là cái bẫy
 * làm bộ Kage lệch trục lúc đầu. Bảng CharInfo có 30 khung, đọc từ tools/charinfo.json:
 *
 *     0-1 đứng · 2-7 chạy · 8-11 lộn nhào · 12 nhảy · 13-20 đánh A · 21-29 đánh B
 *
 * Khung 8-11 chỉ vẽ mảnh đầu (khung 4-7 của mảnh đó), thân và chân tắt hẳn -- nhân vật cuộn thành
 * quả cầu xoay. Bộ trang phục nào thiếu bốn khung ấy là lộn nhào sẽ lòi ra hình đầu mặc định.
 *
 * Phím:
 *   Tab             đổi mảnh đang nắn (đầu / thân / chân)
 *   mũi tên         dịch khung đang xem 1 pixel
 *   Shift + mũi tên dịch cả bộ khung của mảnh đó
 *   , .             lùi / tới một khung hoạt ảnh
 *   1..6            nhảy tới đứng / chạy / lộn nhào / nhảy / đánh A / đánh B
 *   P               chạy hoạt ảnh
 *   O               bật tắt bộ đối chiếu mờ phía sau
 *   D               bật tắt dải xem mọi khung của mảnh đang nắn
 *   + -             phóng to thu nhỏ
 *   Ctrl + S        ghi vào cơ sở dữ liệu và tăng game.data.version
 *
 * Chạy: java -cp "build/tools:<fat jar máy chủ>" CanManh [thư mục máy chủ] [đầu thân chân] [đối chiếu...]
 * Không ghi tham số thì lấy bộ Kage 309/310/308, đối chiếu bộ Thánh Gióng 205/206/207.
 */
public final class CanManh extends JPanel {

    /** Thứ tự ô trong CharInfo, không phải thứ tự vẽ. */
    private static final int DAU = 0, CHAN = 1, THAN = 2;
    /**
     * Thứ tự vẽ đọc thẳng từ bytecode client: mảnh phụ, chân, **đầu**, rồi **thân**. Tức thân vẽ
     * sau đầu, nên tay áo phủ lên nón chứ không phải ngược lại -- vẽ sai thứ tự là xem trong tool
     * thấy đẹp mà vào game lại bị che.
     */
    private static final int[] THU_TU_VE = {CHAN, DAU, THAN};
    private static final String[] TEN = {"đầu", "chân", "thân"};
    /** Hằng số client trừ thêm sau khi cộng dy. */
    private static final int BU_Y = 10;

    private static final int[][] DOAN = {{0, 1}, {2, 7}, {8, 11}, {12, 12}, {13, 20}, {21, 29}};
    private static final String[] TEN_DOAN = {"đứng", "chạy", "lộn nhào", "nhảy", "đánh A", "đánh B"};

    private final File thuMucMayChu;
    private final File thuMucAnh;
    private final Properties cauHinhSql = new Properties();
    private final int[][][] charInfo;

    private final MangKhung[] manh = new MangKhung[3];
    private final MangKhung[] doiChieu = new MangKhung[3];
    private final Map<Integer, BufferedImage> kho = new HashMap<>();

    private int cf = 0;
    private int doan = 0;
    private int manhDangNan = DAU;
    private int phong = 6;
    private boolean veDoiChieu = true, veDai = false, dangChay = false;
    private String tin = "";

    // ------------------------------------------------------------------ dữ liệu

    private static final class MangKhung {
        int id;
        List<int[]> khung = new ArrayList<>();   // {id ảnh, dx, dy}
        boolean daSua = false;
    }

    private Connection moKetNoi() throws Exception {
        String h = cauHinhSql.getProperty("nsoz.database.host", "localhost");
        String c = cauHinhSql.getProperty("nsoz.database.port", "3306");
        String t = cauHinhSql.getProperty("nsoz.database.name");
        return DriverManager.getConnection(
                "jdbc:mysql://" + h + ":" + c + "/" + t + "?useUnicode=true&characterEncoding=UTF-8",
                cauHinhSql.getProperty("nsoz.database.user", "root"),
                cauHinhSql.getProperty("nsoz.database.pass", ""));
    }

    private MangKhung docManh(int id) {
        if (id <= 0) {
            return null;
        }
        try (Connection k = moKetNoi();
             PreparedStatement s = k.prepareStatement("SELECT part FROM nj_part WHERE id = ?")) {
            s.setInt(1, id);
            try (ResultSet r = s.executeQuery()) {
                if (!r.next()) {
                    tin = "không có mảnh " + id;
                    return null;
                }
                MangKhung m = new MangKhung();
                m.id = id;
                m.khung = tachJson(r.getString(1));
                return m;
            }
        } catch (Exception e) {
            tin = "đọc mảnh " + id + ": " + e.getMessage();
            return null;
        }
    }

    /** Bóc [{"dx":..,"dy":..,"id":..}, ...] ra, khỏi cần thư viện json cho một dạng cố định. */
    private static List<int[]> tachJson(String s) {
        List<int[]> ra = new ArrayList<>();
        for (String o : s.split("\\}")) {
            if (o.contains("{")) {
                ra.add(new int[]{soCua(o, "id"), soCua(o, "dx"), soCua(o, "dy")});
            }
        }
        return ra;
    }

    private static int soCua(String o, String ten) {
        int i = o.indexOf('"' + ten + '"');
        if (i < 0) {
            return 0;
        }
        i = o.indexOf(':', i) + 1;
        int j = i;
        while (j < o.length() && (Character.isDigit(o.charAt(j)) || o.charAt(j) == '-' || o.charAt(j) == ' ')) {
            j++;
        }
        return Integer.parseInt(o.substring(i, j).trim());
    }

    private static String dungJson(List<int[]> khung) {
        StringBuilder b = new StringBuilder("[");
        for (int i = 0; i < khung.size(); i++) {
            int[] f = khung.get(i);
            b.append(i > 0 ? ", " : "").append("{\"dx\": ").append(f[1])
                    .append(", \"dy\": ").append(f[2]).append(", \"id\": ").append(f[0]).append('}');
        }
        return b.append(']').toString();
    }

    /** charinfo.json là mảng ba tầng số nguyên, dạng đủ đơn giản để tự bóc. */
    private static int[][][] docCharInfo(File f) throws IOException {
        String s = new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8).trim();
        s = s.substring(1, s.length() - 1);                       // bỏ [ ] ngoài cùng
        List<int[][]> khung = new ArrayList<>();
        int i = 0;
        while (i < s.length()) {
            int a = s.indexOf("[[", i);
            if (a < 0) {
                break;
            }
            int b = s.indexOf("]]", a) + 2;
            String than = s.substring(a + 1, b - 1);              // "[..],[..],[..],[..]"
            List<int[]> o = new ArrayList<>();
            for (String p : than.split("\\],\\s*\\[")) {
                String[] c = p.replace("[", "").replace("]", "").split(",");
                o.add(new int[]{Integer.parseInt(c[0].trim()),
                        Integer.parseInt(c[1].trim()), Integer.parseInt(c[2].trim())});
            }
            khung.add(o.toArray(new int[0][]));
            i = b;
        }
        return khung.toArray(new int[0][][]);
    }

    private BufferedImage anh(int id) {
        if (id <= 0) {
            return null;
        }
        if (!kho.containsKey(id)) {
            BufferedImage im = null;
            try {
                File f = new File(thuMucAnh, "Small/1/Small" + id + ".png");
                if (f.exists()) {
                    im = ImageIO.read(f);
                }
            } catch (Exception ignore) {
                // ảnh thiếu thì vẽ ô đỏ cho lộ ra, đừng nuốt im lặng
            }
            kho.put(id, im);
        }
        return kho.get(id);
    }

    // ------------------------------------------------------------------ vẽ

    @Override
    protected void paintComponent(Graphics g0) {
        super.paintComponent(g0);
        Graphics2D g = (Graphics2D) g0;
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g.setColor(new Color(30, 32, 42));
        g.fillRect(0, 0, getWidth(), getHeight());
        if (veDai) {
            veDaiKhung(g);
        } else {
            veNhanVat(g, getWidth() / 2, getHeight() - 190, phong, cf, true);
            veDayKhung(g);
        }
        veBangTin(g);
    }

    /** Dựng nhân vật ở một khung hoạt ảnh, đúng công thức client. */
    private void veNhanVat(Graphics2D g, int cx, int cy, int z, int khungHh, boolean coVach) {
        if (coVach) {
            g.setColor(new Color(58, 64, 84));
            g.drawLine(cx, 0, cx, getHeight());
            g.drawLine(0, cy, getWidth(), cy);
        }
        for (int o : THU_TU_VE) {
            int[] tt = charInfo[khungHh][o];
            if (tt[0] == 0 && tt[1] == 0 && tt[2] == 0) {
                continue;                                          // ô tắt ở khung này
            }
            if (veDoiChieu) {
                veManh(g, doiChieu[o], tt, cx, cy, z, 0.28f);
            }
            veManh(g, manh[o], tt, cx, cy, z, 1f);
        }
    }

    private void veManh(Graphics2D g, MangKhung m, int[] tt, int cx, int cy, int z, float mo) {
        if (m == null || tt[0] >= m.khung.size()) {
            return;
        }
        int[] f = m.khung.get(tt[0]);
        int x = cx + (tt[1] + f[1]) * z;
        int y = cy + (-tt[2] + f[2] - BU_Y) * z;
        BufferedImage im = anh(f[0]);
        if (im == null) {
            if (f[0] > 0) {
                g.setColor(new Color(210, 70, 70, 130));
                g.fillRect(x, y, 10 * z, 10 * z);
            }
            return;
        }
        Composite cu = g.getComposite();
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, mo));
        g.drawImage(im, x, y, im.getWidth() * z, im.getHeight() * z, null);
        g.setComposite(cu);
    }

    /** Dải nhỏ chạy hết một đoạn hoạt ảnh, để soi khung nào giật. */
    private void veDayKhung(Graphics2D g) {
        int a = DOAN[doan][0], b = DOAN[doan][1], n = b - a + 1;
        int o = Math.min(150, (getWidth() - 40) / Math.max(1, n));
        for (int i = 0; i < n; i++) {
            int cx = 30 + i * o + o / 2, cy = 150;
            g.setColor(a + i == cf ? new Color(74, 92, 128) : new Color(42, 45, 58));
            g.fillRect(cx - o / 2 + 2, cy - 96, o - 4, 130);
            veNhanVat(g, cx, cy, 2, a + i, false);
            g.setColor(Color.YELLOW);
            g.drawString(String.valueOf(a + i), cx - 6, cy + 48);
        }
    }

    /** Mọi khung của mảnh đang nắn, xếp cạnh nhau, có mảnh đối chiếu chồng mờ. */
    private void veDaiKhung(Graphics2D g) {
        MangKhung m = manh[manhDangNan];
        if (m == null) {
            return;
        }
        int z = 3, o = 140, cot = Math.max(1, (getWidth() - 20) / o);
        for (int i = 0; i < m.khung.size(); i++) {
            int cx = 10 + (i % cot) * o + o / 2, cy = 130 + (i / cot) * (o + 40);
            g.setColor(new Color(42, 45, 58));
            g.fillRect(cx - o / 2 + 3, cy - 90, o - 6, o + 20);
            int[] tt = {i, 0, 0};
            if (veDoiChieu) {
                veManh(g, doiChieu[manhDangNan], tt, cx, cy, z, 0.28f);
            }
            veManh(g, m, tt, cx, cy, z, 1f);
            g.setColor(new Color(58, 64, 84));
            g.drawLine(cx, cy - 80, cx, cy + 20);
            g.setColor(Color.YELLOW);
            int[] f = m.khung.get(i);
            g.drawString(i + ":  " + f[1] + ", " + f[2], cx - o / 2 + 10, cy + 40);
        }
    }

    private void veBangTin(Graphics2D g) {
        g.setColor(new Color(0, 0, 0, 180));
        g.fillRect(0, 0, getWidth(), 84);
        g.setFont(new Font("SansSerif", Font.PLAIN, 13));
        g.setColor(Color.WHITE);
        StringBuilder b = new StringBuilder();
        for (int o : new int[]{DAU, THAN, CHAN}) {
            b.append(o == manhDangNan ? "[" : " ").append(TEN[o]).append(' ')
                    .append(manh[o] == null ? "-" : String.valueOf(manh[o].id))
                    .append(manh[o] != null && manh[o].daSua ? " *" : "")
                    .append(o == manhDangNan ? "]" : " ").append("    ");
        }
        g.drawString(b.toString(), 12, 20);
        int[] tt = charInfo[cf][manhDangNan];
        MangKhung m = manh[manhDangNan];
        String sole = m != null && tt[0] < m.khung.size()
                ? ("dx " + m.khung.get(tt[0])[1] + "  dy " + m.khung.get(tt[0])[2]) : "ô tắt";
        g.drawString(TEN_DOAN[doan] + "   khung " + cf + "   " + TEN[manhDangNan]
                + " dùng khung ảnh " + tt[0] + "   " + sole
                + "   phóng " + phong + "x" + (dangChay ? "   ▶" : ""), 12, 40);
        g.setColor(new Color(168, 174, 190));
        g.drawString("Tab đổi mảnh · mũi tên nắn · Shift cả bộ · , . đổi khung · 1..6 chọn đoạn"
                + " · P chạy · O đối chiếu · D dải · +/- phóng · Ctrl+S lưu", 12, 60);
        if (!tin.isEmpty()) {
            g.setColor(new Color(120, 220, 140));
            g.drawString(tin, 12, 78);
        }
    }

    // ------------------------------------------------------------------ thao tác

    private void nan(int dx, int dy, boolean caBo) {
        MangKhung m = manh[manhDangNan];
        if (m == null) {
            return;
        }
        if (caBo) {
            for (int[] f : m.khung) {
                if (f[0] > 0) {
                    f[1] += dx;
                    f[2] += dy;
                }
            }
        } else {
            int k = charInfo[cf][manhDangNan][0];
            if (k >= m.khung.size()) {
                return;
            }
            m.khung.get(k)[1] += dx;
            m.khung.get(k)[2] += dy;
        }
        m.daSua = true;
        repaint();
    }

    private void luu() {
        int daGhi = 0;
        try (Connection k = moKetNoi();
             PreparedStatement s = k.prepareStatement("UPDATE nj_part SET part = ? WHERE id = ?")) {
            for (MangKhung m : manh) {
                if (m != null && m.daSua) {
                    s.setString(1, dungJson(m.khung));
                    s.setInt(2, m.id);
                    s.executeUpdate();
                    m.daSua = false;
                    daGhi++;
                }
            }
        } catch (Exception e) {
            tin = "ghi hỏng: " + e.getMessage();
            repaint();
            return;
        }
        tin = daGhi == 0 ? "không có gì để ghi"
                : ("đã ghi " + daGhi + " mảnh, data.version = " + tangPhienBan()
                   + " -- khởi động lại máy chủ thì client mới tải lại");
        repaint();
    }

    /** Client chỉ tải lại mảnh khi game.data.version đổi, nên ghi xong là phải tăng. */
    private int tangPhienBan() {
        File f = new File(thuMucMayChu, "config.properties");
        try {
            List<String> dong = Files.readAllLines(f.toPath(), StandardCharsets.UTF_8);
            for (int i = 0; i < dong.size(); i++) {
                if (dong.get(i).startsWith("game.data.version=")) {
                    int moi = Integer.parseInt(dong.get(i).split("=")[1].trim()) + 1;
                    dong.set(i, "game.data.version=" + moi);
                    Files.write(f.toPath(), dong, StandardCharsets.UTF_8);
                    return moi;
                }
            }
        } catch (Exception e) {
            tin = "không tăng được phiên bản: " + e.getMessage();
        }
        return -1;
    }

    private void datDoan(int d) {
        doan = d;
        cf = DOAN[d][0];
        repaint();
    }

    private void doiKhung(int b) {
        int a = DOAN[doan][0], c = DOAN[doan][1], n = c - a + 1;
        cf = a + ((cf - a + b) % n + n) % n;
        repaint();
    }

    // ------------------------------------------------------------------ dựng

    private CanManh(File mc) throws Exception {
        thuMucMayChu = mc;
        thuMucAnh = new File(mc, "Data/Img");
        try (InputStream i = new FileInputStream(new File(mc, "mysql.properties"))) {
            cauHinhSql.load(i);
        }
        charInfo = docCharInfo(new File("tools/charinfo.json"));
        setPreferredSize(new Dimension(1240, 820));
        setFocusable(true);
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                boolean caBo = e.isShiftDown();
                boolean dieuKhien = e.isControlDown() || e.isMetaDown();
                switch (e.getKeyCode()) {
                    case KeyEvent.VK_LEFT:   nan(-1, 0, caBo); break;
                    case KeyEvent.VK_RIGHT:  nan(1, 0, caBo); break;
                    case KeyEvent.VK_UP:     nan(0, -1, caBo); break;
                    case KeyEvent.VK_DOWN:   nan(0, 1, caBo); break;
                    case KeyEvent.VK_TAB:    manhDangNan = manhDangNan == DAU ? THAN
                                                        : manhDangNan == THAN ? CHAN : DAU;
                                             repaint(); break;
                    case KeyEvent.VK_COMMA:  doiKhung(-1); break;
                    case KeyEvent.VK_PERIOD: doiKhung(1); break;
                    case KeyEvent.VK_P:      dangChay = !dangChay; repaint(); break;
                    case KeyEvent.VK_O:      veDoiChieu = !veDoiChieu; repaint(); break;
                    case KeyEvent.VK_D:      veDai = !veDai; repaint(); break;
                    case KeyEvent.VK_EQUALS: phong = Math.min(14, phong + 1); repaint(); break;
                    case KeyEvent.VK_MINUS:  phong = Math.max(2, phong - 1); repaint(); break;
                    case KeyEvent.VK_S:      if (dieuKhien) { luu(); } break;
                    default:
                        int n = e.getKeyCode() - KeyEvent.VK_1;
                        if (n >= 0 && n < DOAN.length) {
                            datDoan(n);
                        }
                }
            }
        });
        new javax.swing.Timer(130, e -> {
            if (dangChay) {
                doiKhung(1);
            }
        }).start();
    }

    private void dat(int o, int idManh, int idDoiChieu) {
        manh[o] = docManh(idManh);
        doiChieu[o] = docManh(idDoiChieu);
    }

    /**
     * Đọc rồi dựng lại mọi hàng nj_part và so bằng, để chắc bộ bóc chuỗi không làm hỏng số nào
     * trước khi cho phép ghi đè lên dữ liệu thật.
     */
    private void tuKiem() throws Exception {
        int tong = 0, lech = 0;
        try (Connection k = moKetNoi();
             Statement s = k.createStatement();
             ResultSet r = s.executeQuery("SELECT id, part FROM nj_part ORDER BY id")) {
            while (r.next()) {
                tong++;
                List<int[]> a = tachJson(r.getString(2));
                List<int[]> b = tachJson(dungJson(a));
                boolean bang = a.size() == b.size();
                for (int i = 0; bang && i < a.size(); i++) {
                    bang = Arrays.equals(a.get(i), b.get(i));
                }
                if (!bang) {
                    lech++;
                    System.out.println("  lệch ở mảnh " + r.getInt(1));
                }
            }
        }
        System.out.println("tự kiểm: " + tong + " mảnh, " + lech + " lệch");
    }

    /**
     * Dựng cả sáu đoạn hoạt ảnh ra một tấm ảnh, khỏi phải mở cửa sổ và khỏi vào game. Đây là cách
     * tự kiểm một bộ trang phục mới nhanh nhất: nhìn tấm này là thấy ngay khung nào lệch, khung nào
     * lòi ảnh sai, mà không phải khởi động lại máy chủ.
     */
    private void xuatAnh(String tep) throws IOException {
        int z = 3, o = 96, cao = 150;
        int cot = 0;
        for (int[] d : DOAN) {
            cot = Math.max(cot, d[1] - d[0] + 1);
        }
        BufferedImage ra = new BufferedImage(o * cot + 120, cao * DOAN.length, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = ra.createGraphics();
        g.setColor(new Color(30, 32, 42));
        g.fillRect(0, 0, ra.getWidth(), ra.getHeight());
        g.setFont(new Font("SansSerif", Font.BOLD, 13));
        for (int d = 0; d < DOAN.length; d++) {
            int y = d * cao;
            g.setColor(Color.YELLOW);
            g.drawString(TEN_DOAN[d], 8, y + 18);
            for (int i = DOAN[d][0]; i <= DOAN[d][1]; i++) {
                int cx = 120 + (i - DOAN[d][0]) * o + o / 2;
                g.setColor(new Color(44, 47, 60));
                g.fillRect(cx - o / 2 + 2, y + 6, o - 4, cao - 12);
                veNhanVat(g, cx, y + cao - 26, z, i, false);
                g.setColor(new Color(150, 155, 170));
                g.drawString(String.valueOf(i), cx - 6, y + cao - 8);
            }
        }
        g.dispose();
        ImageIO.write(ra, "png", new File(tep));
        System.out.println("đã xuất " + tep);
    }

    /**
     * In ra khung bao thật của ba mảnh trên màn hình, theo toạ độ gốc nhân vật. Đây mới là thứ
     * nói được "lệch bao nhiêu pixel": nhìn ảnh chỉ thấy sai, còn bảng này chỉ đúng chỗ sai và
     * sai mấy pixel. So với một bộ đã chuẩn là ra ngay lượng cần dịch.
     */
    private void doKhung(int khungHh) {
        System.out.println("khung " + khungHh + " -- toạ độ so với gốc nhân vật:");
        String[] nhan = {"đầu", "chân", "thân"};
        for (int o : new int[]{DAU, CHAN, THAN}) {
            MangKhung m = manh[o];
            int[] tt = charInfo[khungHh][o];
            if (m == null || tt[0] >= m.khung.size()) {
                continue;
            }
            int[] f = m.khung.get(tt[0]);
            BufferedImage im = anh(f[0]);
            if (im == null) {
                System.out.printf("   %-5s ô ảnh %d: thiếu tệp%n", nhan[o], f[0]);
                continue;
            }
            int x = tt[1] + f[1];
            int y = -tt[2] + f[2] - BU_Y;
            System.out.printf("   %-5s x %4d..%-4d y %4d..%-4d   tâm %5.1f  đáy %d%n",
                    nhan[o], x, x + im.getWidth(), y, y + im.getHeight(),
                    x + im.getWidth() / 2.0, y + im.getHeight());
        }
    }

    public static void main(String[] args) throws Exception {
        CanManh p = new CanManh(new File(args.length > 0 ? args[0] : "work/server/NSO_KEM"));
        if (args.length > 1 && args[1].equals("--kiem")) {
            p.tuKiem();
            System.exit(0);                       // bộ đếm giờ Swing đã dựng, không thoát là treo
        }
        if (args.length > 4 && args[1].equals("--do")) {
            p.veDoiChieu = false;
            p.dat(DAU, Integer.parseInt(args[2]), -1);
            p.dat(THAN, Integer.parseInt(args[3]), -1);
            p.dat(CHAN, Integer.parseInt(args[4]), -1);
            p.doKhung(args.length > 5 ? Integer.parseInt(args[5]) : 0);
            System.exit(0);
        }
        if (args.length > 4 && args[1].equals("--anh")) {
            p.veDoiChieu = false;
            p.dat(DAU, Integer.parseInt(args[3]), -1);
            p.dat(THAN, Integer.parseInt(args[4]), -1);
            p.dat(CHAN, Integer.parseInt(args[5]), -1);
            p.setSize(1400, 900);
            p.xuatAnh(args[2]);
            System.exit(0);
        }
        // mặc định: bộ Kage, đối chiếu bộ Thánh Gióng (mảnh 205 đầu, 206 thân, 207 chân) vốn
        // đã căn chuẩn sẵn trong game
        p.dat(DAU, so(args, 1, 309), so(args, 4, 205));
        p.dat(THAN, so(args, 2, 310), so(args, 5, 206));
        p.dat(CHAN, so(args, 3, 308), so(args, 6, 207));
        JFrame f = new JFrame("Căn mảnh trang phục");
        f.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        f.add(p);
        f.pack();
        f.setLocationRelativeTo(null);
        f.setVisible(true);
        p.requestFocusInWindow();
    }

    private static int so(String[] a, int i, int mac) {
        return a.length > i ? Integer.parseInt(a[i]) : mac;
    }
}
