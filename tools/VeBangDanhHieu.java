import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.charset.StandardCharsets;

/**
 * Sinh biểu ngữ danh hiệu: dòng chữ bay trên đầu nhân vật khi mặc bí kíp danh hiệu.
 *
 * Định dạng học từ hiệu ứng 201 ("Nhẫn giả thánh nhân" của Đệ Nhất) trong bảng effect_data:
 * <ul>
 *   <li>ảnh là MỘT tấm xếp dọc 8 ô, mỗi ô là một khung hình của dòng chữ;</li>
 *   <li>cột {@code sprites} khai các ô đó: {id, x, y, w, h} theo toạ độ mức phóng 1;</li>
 *   <li>cột {@code frames} nói mỗi khung vẽ ô nào, lệch bao nhiêu -- dx = -w/2 cho cân giữa,
 *       dy âm để đẩy lên trên đầu;</li>
 *   <li>cột {@code running} là dòng thời gian: dãy chỉ số khung, lặp lại bao nhiêu lần thì chữ
 *       giữ nguyên bấy nhiêu nhịp;</li>
 *   <li>tệp ảnh nằm ở Data/Img/Effect/{1,2,3,4}/{mã}.png, cỡ đúng bội 1/2/3/4.</li>
 * </ul>
 *
 * Chữ vẽ ở mức phóng 4 rồi thu xuống, giống cách làm icon -- vẽ thẳng ở 16px thì dấu tiếng Việt
 * dính vào nhau. Hoạt ảnh là một vệt sáng chạy ngang qua chữ, đúng kiểu biểu ngữ sẵn có.
 *
 *   java -cp build/tools VeBangDanhHieu <thư mục Data/Img/Effect> <tệp .sql xuất ra>
 */
public class VeBangDanhHieu {

    /** {mã hiệu ứng, mã vật phẩm, chữ, màu chính, màu vệt sáng} */
    /** {mã hiệu ứng, mã vật phẩm, chữ, kiểu khung, màu nền, màu chữ, màu vệt sáng} */
    private static final Object[][] DS = {
        {180, 1119, "Tam Nhẫn Truyền Kỳ",  "cuon",  0x4A3A12, 0xF2D97A, 0xFFF6D0},
        {181, 1120, "Tiên Nhân Diệu Mộc",  "la",    0x0F4C3A, 0x7BE8B4, 0xE9FFF6},
        {188, 1122, "Nhân Trụ Lực",        "an",    0x4A1050, 0xD98BEE, 0xFFE6FF},
        {189, 1132, "Băng Độn Huyết Kế",   "bang",  0x0B3550, 0x86D8F5, 0xE4F7FF},
        {190, 1145, "Kiếm Hào Thất Nhẫn",  "kiem",  0x2A3038, 0xDCE6F2, 0xFFFFFF},
        {191, 1149, "Ám Bộ Anbu",          "bong",  0x14181F, 0xA8B8D6, 0xE6EEFF},
        {214, 1154, "Uchiha Nhất Tộc",     "mat",   0x4A0A10, 0xF06068, 0xFFD9DB},
        {215, 1155, "Senju Nhất Tộc",      "la",    0x123D22, 0x7FDC90, 0xE7FFE9},
        {216, 1162, "Cửu Vĩ Yêu Hồ",       "lua",   0x5A1F04, 0xFFA245, 0xFFE2A8},
        {217, 1206, "Lục Đạo Tiên Nhân",   "vuong", 0x4A3A08, 0xF7DE62, 0xFFFBE6},
        {218, 1207, "Thần Uy Susanoo",     "khien", 0x1E2450, 0x93A2F5, 0xE6EBFF},
        {232, 1209, "Luân Hồi Nhãn",       "mat",   0x122A4A, 0x8CBEF7, 0xE8F1FF},
        {233, 1210, "Bát Môn Độn Giáp",    "lua",   0x4A1A02, 0xFF9440, 0xFFD98A},
        {237, 1235, "Nhẫn Giả Đào Vong",   "bong",  0x2A1214, 0xF07878, 0xFFE0E0},
        {238, 1224, "Thiên Bảng Đệ Nhất",  "sao",   0x4A3808, 0xFFD84A, 0xFFFBE6},
        {239, 1225, "Thiên Bảng Đệ Nhị",   "sao",   0x33363C, 0xD8DDE6, 0xFFFFFF},
        {240, 1226, "Thiên Bảng Đệ Tam",   "sao",   0x3E2510, 0xDC9A5A, 0xFFEEDC},
        {242, 1227, "Thiên Bảng Đệ Tứ",    "sao",   0x1C2F40, 0x8FB8D8, 0xEAF4FF},
        {243, 1228, "Thiên Bảng Đệ Ngũ",   "sao",   0x24291F, 0xAFC0A8, 0xF0F5F0},
        // Hai danh hiệu cũ chưa từng có biểu ngữ chạy được: Mangekyo chỉ có hiệu ứng hoa 231,
        // còn Akatsuki trỏ vào hiệu ứng 301-303 vốn KHÔNG tồn tại trong effect_data.
        {244, 1131, "Mangekyo Sharingan",  "mat",   0x4A0A10, 0xF06068, 0xFFD9DB},
        {254, 1133, "Akatsuki",            "may",   0x1A1A1E, 0xE84A54, 0xFFE0E0},
    };

    private static final int SO_KHUNG = 8;
    private static final int CAO = 22;      // cao một ô ở mức phóng 1, chừa chỗ cho hoạ tiết

    /**
     * Bề rộng tối đa một ô, ở mức phóng 1.
     *
     * KHÔNG được vượt 127. Máy khách giữ bề rộng ô sprite trong một byte có dấu; rộng 158 thì bên
     * kia đọc ra số âm và không vẽ gì cả -- biểu ngữ im lặng biến mất, không báo lỗi ở đâu hết.
     * Soát lại 230 hiệu ứng có sẵn của game: cái rộng nhất đúng 111. Cỡ chữ ở đây tự co cho vừa
     * khung này chứ không phải khung co theo chữ.
     */
    private static final int RONG = 112;
    /**
     * Độ cao của biểu ngữ so với chân nhân vật.
     *
     * -75 là số chép của biểu ngữ 201, nhưng ở mức đó tấm chữ đè ngay lên đầu và vai -- che mất
     * chính nhân vật, mà mặc thêm hào quang Susanoo thì chữ lọt thỏm vào giữa bộ giáp. Đẩy lên
     * -102 là chữ nằm hẳn phía trên, cách đỉnh đầu một khoảng thở.
     */
    private static final int DY = -102;

    public static void main(String[] args) throws Exception {
        String goc = args.length > 0 ? args[0] : "work/server/NSO_KEM/Data/Img/Effect";
        String raSql = args.length > 1 ? args[1] : "/tmp/bang-danh-hieu.sql";
        StringBuilder sql = new StringBuilder();
        sql.append("-- Biểu ngữ danh hiệu, sinh bằng tools/VeBangDanhHieu.java. Đừng sửa tay.\n");
        for (Object[] d : DS) {
            int ma = (Integer) d[0];
            String chu = (String) d[2];
            int rong = veTam(goc, ma, chu, (String) d[3], (Integer) d[4],
                    (Integer) d[5], (Integer) d[6]);
            sql.append(cauSql(ma, rong)).append('\n');
            System.out.println("  " + ma + "  " + chu + "  rộng " + rong);
        }
        try (Writer w = new OutputStreamWriter(new FileOutputStream(raSql), StandardCharsets.UTF_8)) {
            w.write(sql.toString());
        }
        System.out.println("xong " + DS.length + " biểu ngữ -> " + raSql);
    }

    /** Vẽ tấm 8 khung ở cả bốn mức phóng, trả về chiều rộng một ô ở mức phóng 1. */
    private static int veTam(String goc, int ma, String chu, String kieu, int nen,
            int mau, int sang) throws Exception {
        int z = 4;
        int rong1 = RONG, rong4 = rong1 * z;
        BufferedImage do1 = new BufferedImage(8, 8, BufferedImage.TYPE_INT_ARGB);
        // Co cỡ chữ cho tới khi lọt khung: tên dài như "Kiếm Hào Thất Nhẫn" phải nhỏ hơn
        // "Nhân Trụ Lực" mới cùng nằm vừa một bề rộng.
        Font f = null;
        FontMetrics fm = null;
        for (int co = CAO * z - 34; co >= 16; co -= 2) {
            f = new Font("SansSerif", Font.BOLD, co);
            fm = do1.createGraphics().getFontMetrics(f);
            if (fm.stringWidth(chu) <= rong4 - 22 * z) {
                break;
            }
        }

        BufferedImage to = new BufferedImage(rong4, CAO * z * SO_KHUNG, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = to.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setFont(f);
        for (int k = 0; k < SO_KHUNG; k++) {
            veKhung(g, kieu, k, rong4, CAO * z, k * CAO * z, z, new Color(nen), new Color(mau),
                    new Color(sang), chu, fm);
        }
        g.dispose();

        for (int m = 1; m <= 4; m++) {
            int w = rong1 * m, h = CAO * m * SO_KHUNG;
            BufferedImage ra = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
            Graphics2D gg = ra.createGraphics();
            gg.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            gg.drawImage(to, 0, 0, w, h, null);
            gg.dispose();
            File out = new File(goc + File.separator + m, ma + ".png");
            out.getParentFile().mkdirs();
            ImageIO.write(ra, "png", out);
        }
        return rong1;
    }

    /**
     * Vẽ một khung hình của biểu ngữ theo kiểu riêng của danh hiệu.
     *
     * Mỗi kiểu khác nhau ở ba chỗ: dáng tấm nền, hoạ tiết hai đầu, và cách chữ bắt sáng. Ở cỡ
     * 18-22px thì chi tiết nhỏ tan hết, nên cá tính phải nằm ở DÁNG và MÀU chứ không phải ở nét.
     */
    private static void veKhung(Graphics2D g, String kieu, int k, int W, int H, int y0, int z,
            Color nen, Color mau, Color sang, String chu, FontMetrics fm) {
        float gy = y0 + H / 2f;
        int le = 2 * z;
        float tren = y0 + 2.5f * z, duoi = y0 + H - 3f * z;
        Shape tam = dangTam(kieu, le, tren, W - le, duoi, z);

        // Nền: dốc từ màu riêng của danh hiệu xuống gần đen, cho chữ nổi trên mọi nền bản đồ.
        g.setPaint(new GradientPaint(0, tren, pha(nen, 235), 0, duoi, new Color(0, 0, 0, 215)));
        g.fill(tam);
        // Vệt sáng mảnh ở nửa trên, tạo khối.
        g.setPaint(new Color(255, 255, 255, 26));
        g.fill(new Rectangle2D.Float(le + 2 * z, tren + 1, W - 2 * le - 4 * z, (duoi - tren) / 2.4f));
        g.setStroke(new BasicStroke(1.5f * z, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(pha(mau, 225));
        g.draw(tam);

        veHoaTiet(g, kieu, W, gy, z, mau, sang, k);

        int x = (W - fm.stringWidth(chu)) / 2;
        int y = (int) (gy + (fm.getAscent() - fm.getDescent()) / 2f);
        g.setColor(new Color(0, 0, 0, 225));
        for (int dx = -2; dx <= 2; dx += 2) {
            for (int dy = -2; dy <= 2; dy += 2) {
                if (dx != 0 || dy != 0) {
                    g.drawString(chu, x + dx, y + dy);
                }
            }
        }
        float tamSang = (float) k / SO_KHUNG * (W + 200) - 100;
        g.setPaint(new LinearGradientPaint(
                new Point((int) (tamSang - 90), 0), new Point((int) (tamSang + 90), 0),
                new float[] {0f, 0.5f, 1f}, new Color[] {mau, sang, mau}));
        g.drawString(chu, x, y);
    }

    /** Dáng tấm nền theo kiểu. */
    private static Shape dangTam(String kieu, float x1, float y1, float x2, float y2, int z) {
        Path2D p = new Path2D.Float();
        float gy = (y1 + y2) / 2f, vat = 5f * z;
        if ("kiem".equals(kieu)) {           // lưỡi kiếm: hai đầu nhọn hoắt
            p.moveTo(x1, gy);
            p.lineTo(x1 + vat * 1.6f, y1);
            p.lineTo(x2 - vat * 1.6f, y1);
            p.lineTo(x2, gy);
            p.lineTo(x2 - vat * 1.6f, y2);
            p.lineTo(x1 + vat * 1.6f, y2);
            p.closePath();
            return p;
        }
        if ("bang".equals(kieu)) {           // băng: mép trên răng cưa nhọn
            p.moveTo(x1, y2);
            p.lineTo(x1 + vat, y1);
            int rang = 7;
            for (int i = 0; i < rang; i++) {
                float a = x1 + vat + (x2 - x1 - 2 * vat) * i / (float) rang;
                float b = x1 + vat + (x2 - x1 - 2 * vat) * (i + 0.5f) / (float) rang;
                p.lineTo(a, y1);
                p.lineTo(b, y1 - 1.2f * z);
            }
            p.lineTo(x2 - vat, y1);
            p.lineTo(x2, y2);
            p.closePath();
            return p;
        }
        if ("lua".equals(kieu)) {            // lửa: mép trên lượn sóng
            p.moveTo(x1, y2);
            p.curveTo(x1 - 1.5f * z, gy, x1 + z, y1, x1 + vat, y1);
            float b = (x2 - x1 - 2 * vat) / 6f;
            for (int i = 0; i < 6; i++) {
                float a = x1 + vat + b * i;
                p.quadTo(a + b / 2f, y1 - 1.6f * z, a + b, y1);
            }
            p.curveTo(x2 - z, y1, x2 + 1.5f * z, gy, x2, y2);
            p.closePath();
            return p;
        }
        if ("cuon".equals(kieu)) {           // cuộn thư: hai đầu bo tròn như trục cuộn
            return new RoundRectangle2D.Float(x1, y1, x2 - x1, y2 - y1, (y2 - y1), (y2 - y1));
        }
        if ("khien".equals(kieu)) {          // khiên: sáu cạnh
            p.moveTo(x1, gy);
            p.lineTo(x1 + vat, y1);
            p.lineTo(x2 - vat, y1);
            p.lineTo(x2, gy);
            p.lineTo(x2 - vat, y2);
            p.lineTo(x1 + vat, y2);
            p.closePath();
            return p;
        }
        if ("may".equals(kieu)) {            // mây Akatsuki: mép trên gợn thành cụm mây
            // Cụm mây nhô LÊN TRÊN mép tấm chứ không kéo mép xuống giữa -- kéo xuống thì chữ
            // nằm đè lên đường viền, đọc rối.
            p.moveTo(x1, y2);
            p.lineTo(x1, y1 + 0.6f * z);
            float b = (x2 - x1) / 5f;
            for (int i = 0; i < 5; i++) {
                p.quadTo(x1 + b * i + b / 2f, y1 - 2.2f * z, x1 + b * (i + 1), y1 + 0.6f * z);
            }
            p.lineTo(x2, y2);
            p.closePath();
            return p;
        }
        if ("bong".equals(kieu)) {           // bóng tối: chữ nhật vát nhẹ, thấp
            p.moveTo(x1 + vat * 0.6f, y1);
            p.lineTo(x2, y1);
            p.lineTo(x2 - vat * 0.6f, y2);
            p.lineTo(x1, y2);
            p.closePath();
            return p;
        }
        // mặc định: dải vát hai đầu (an, la, mat, vuong, sao)
        p.moveTo(x1 + vat, y1);
        p.lineTo(x2 - vat, y1);
        p.lineTo(x2, gy);
        p.lineTo(x2 - vat, y2);
        p.lineTo(x1 + vat, y2);
        p.lineTo(x1, gy);
        p.closePath();
        return p;
    }

    /** Hoạ tiết hai đầu: thứ làm nên cá tính rõ nhất ở cỡ nhỏ. */
    private static void veHoaTiet(Graphics2D g, String kieu, int W, float gy, int z,
            Color mau, Color sang, int k) {
        g.setStroke(new BasicStroke(1.3f * z, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        for (int b = -1; b <= 1; b += 2) {
            float x = b < 0 ? 4.5f * z : W - 4.5f * z;
            g.setColor(sang);
            if ("mat".equals(kieu)) {                 // con mắt
                g.fill(new Ellipse2D.Float(x - 3f * z, gy - 2f * z, 6f * z, 4f * z));
                g.setColor(new Color(0x1A0000));
                g.fill(new Ellipse2D.Float(x - 1.4f * z, gy - 1.4f * z, 2.8f * z, 2.8f * z));
            } else if ("la".equals(kieu)) {           // chiếc lá
                Path2D p = new Path2D.Float();
                p.moveTo(x, gy - 3f * z);
                p.quadTo(x + 2.6f * z, gy, x, gy + 3f * z);
                p.quadTo(x - 2.6f * z, gy, x, gy - 3f * z);
                p.closePath();
                g.fill(p);
            } else if ("bang".equals(kieu)) {         // tinh thể sáu cánh
                for (int i = 0; i < 6; i++) {
                    double a = Math.toRadians(i * 60);
                    g.draw(new Line2D.Double(x, gy, x + Math.cos(a) * 3f * z, gy + Math.sin(a) * 3f * z));
                }
            } else if ("lua".equals(kieu)) {          // ngọn lửa nhỏ, cao thấp theo khung
                float h = 3f * z + (k % 4) * 0.4f * z;
                Path2D p = new Path2D.Float();
                p.moveTo(x, gy - h);
                p.quadTo(x + 2f * z, gy, x, gy + 2.4f * z);
                p.quadTo(x - 2f * z, gy, x, gy - h);
                p.closePath();
                g.fill(p);
            } else if ("kiem".equals(kieu)) {         // chuôi kiếm
                g.draw(new Line2D.Float(x - 2.6f * z, gy - 2.4f * z, x + 2.6f * z, gy + 2.4f * z));
                g.draw(new Line2D.Float(x - 2.6f * z, gy + 2.4f * z, x + 2.6f * z, gy - 2.4f * z));
            } else if ("an".equals(kieu)) {           // vòng phong ấn
                g.draw(new Ellipse2D.Float(x - 3f * z, gy - 3f * z, 6f * z, 6f * z));
                g.fill(new Ellipse2D.Float(x - 1f * z, gy - 1f * z, 2f * z, 2f * z));
            } else if ("khien".equals(kieu)) {        // khiên nhỏ
                Path2D p = new Path2D.Float();
                p.moveTo(x, gy - 3.2f * z);
                p.lineTo(x + 2.6f * z, gy - 1.6f * z);
                p.lineTo(x, gy + 3.2f * z);
                p.lineTo(x - 2.6f * z, gy - 1.6f * z);
                p.closePath();
                g.fill(p);
            } else if ("may".equals(kieu)) {          // mây đỏ
                g.fill(new Ellipse2D.Float(x - 3.2f * z, gy - 1.6f * z, 4f * z, 3.2f * z));
                g.fill(new Ellipse2D.Float(x - 0.6f * z, gy - 2.6f * z, 3.6f * z, 3.6f * z));
            } else if ("vuong".equals(kieu)) {        // vương miện
                Path2D p = new Path2D.Float();
                p.moveTo(x - 3f * z, gy + 2.4f * z);
                p.lineTo(x - 3f * z, gy - 2.4f * z);
                p.lineTo(x - 1f * z, gy);
                p.lineTo(x, gy - 3f * z);
                p.lineTo(x + 1f * z, gy);
                p.lineTo(x + 3f * z, gy - 2.4f * z);
                p.lineTo(x + 3f * z, gy + 2.4f * z);
                p.closePath();
                g.fill(p);
            } else if ("sao".equals(kieu)) {          // ngôi sao
                Path2D p = new Path2D.Float();
                for (int i = 0; i < 10; i++) {
                    double a = Math.toRadians(-90 + i * 36);
                    double r = (i % 2 == 0) ? 3.4f * z : 1.5f * z;
                    double px = x + Math.cos(a) * r, py = gy + Math.sin(a) * r;
                    if (i == 0) {
                        p.moveTo(px, py);
                    } else {
                        p.lineTo(px, py);
                    }
                }
                p.closePath();
                g.fill(p);
            } else if ("bong".equals(kieu)) {         // ba vệt mờ dần
                for (int i = 0; i < 3; i++) {
                    g.setColor(new Color(mau.getRed(), mau.getGreen(), mau.getBlue(), 190 - i * 55));
                    g.draw(new Line2D.Float(x + b * i * 1.6f * z, gy - 2.6f * z,
                            x + b * i * 1.6f * z, gy + 2.6f * z));
                }
            } else {                                   // cuon: nút trục cuộn
                g.fill(new Ellipse2D.Float(x - 2.4f * z, gy - 2.4f * z, 4.8f * z, 4.8f * z));
                g.setColor(new Color(0, 0, 0, 150));
                g.fill(new Ellipse2D.Float(x - 1f * z, gy - 1f * z, 2f * z, 2f * z));
            }
        }
    }

    private static Color pha(Color c, int alpha) {
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), alpha);
    }

    private static String cauSql(int ma, int rong) {
        StringBuilder sp = new StringBuilder("[");
        StringBuilder fr = new StringBuilder("[");
        StringBuilder rn = new StringBuilder("[");
        for (int i = 0; i < SO_KHUNG; i++) {
            if (i > 0) {
                sp.append(',');
                fr.append(',');
            }
            sp.append("{\"id\":").append(i).append(",\"x\":0,\"y\":").append(i * CAO)
              .append(",\"w\":").append(rong).append(",\"h\":").append(CAO).append('}');
            fr.append("[{\"id\":").append(i).append(",\"dx\":").append(-rong / 2)
              .append(",\"dy\":").append(DY).append(",\"onTop\":0,\"flip\":0}]");
            // Mỗi khung giữ 3 nhịp: vệt sáng chạy chậm vừa mắt, không giật.
            for (int k = 0; k < 3; k++) {
                rn.append(i == 0 && k == 0 ? "" : ",").append(i);
            }
        }
        sp.append(']');
        fr.append(']');
        rn.append(']');
        return "REPLACE INTO `effect_data` (`id`,`sprites`,`frames`,`running`,`frame_char`) VALUES ("
                + ma + ",'" + sp + "','" + fr + "','" + rn + "','[[],[],[],[]]');";
    }
}
