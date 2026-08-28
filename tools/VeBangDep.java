import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.charset.StandardCharsets;

/**
 * Biểu ngữ danh hiệu bản có hoạ tiết: huy hiệu, cánh, hào quang -- không chỉ chữ trên tấm nền.
 *
 * Khác bản {@link VeBangDanhHieu} ở chỗ dựng nhiều lớp chồng nhau, đúng cách mấy biểu ngữ vẽ tay
 * của game (hiệu ứng 12-20) làm:
 *   1. hào quang toả sau huy hiệu, phập phồng theo khung;
 *   2. hai cánh vuốt ra hai bên;
 *   3. tấm nền hai lớp, viền ngoài màu danh hiệu, viền trong tối;
 *   4. huy hiệu tròn bên trái, mang ĐÚNG hoạ tiết của icon món đó (mượn VeIconDanhHieu.veMotif)
 *      -- để người chơi nối được món trong túi với dòng chữ trên đầu;
 *   5. chữ có vệt sáng chạy ngang.
 *
 * Khung 124x40 ở mức phóng 1. Hai trần phải nhớ: bề rộng ô sprite KHÔNG quá 127 (máy khách giữ
 * trong byte có dấu), và chiều cao nên dưới 132 -- đó là mức cao nhất trong 230 hiệu ứng sẵn có.
 */
public class VeBangDep {

    /** {mã hiệu ứng, chữ, hoạ tiết, màu nền, màu chữ, màu sáng} */
    private static final Object[][] DS = {
        {180, "Tam Nhẫn Truyền Kỳ",  "cuon",   0x4A3A12, 0xF2D97A, 0xFFF6D0},
        {181, "Tiên Nhân Diệu Mộc",  "xoay",   0x0F4C3A, 0x7BE8B4, 0xE9FFF6},
        {188, "Nhân Trụ Lực",        "thu",    0x4A1050, 0xD98BEE, 0xFFE6FF},
        {189, "Băng Độn Huyết Kế",   "bong",   0x0B3550, 0x86D8F5, 0xE4F7FF},
        {190, "Kiếm Hào Thất Nhẫn",  "kiem2",  0x2A3038, 0xDCE6F2, 0xFFFFFF},
        {191, "Ám Bộ Anbu",          "matna",  0x14181F, 0xA8B8D6, 0xE6EEFF},
        {214, "Uchiha Nhất Tộc",     "mat3",   0x4A0A10, 0xF06068, 0xFFD9DB},
        {215, "Senju Nhất Tộc",      "la",     0x123D22, 0x7FDC90, 0xE7FFE9},
        {216, "Cửu Vĩ Yêu Hồ",       "duoi",   0x5A1F04, 0xFFA245, 0xFFE2A8},
        {217, "Lục Đạo Tiên Nhân",   "vuong",  0x4A3A08, 0xF7DE62, 0xFFFBE6},
        {218, "Thần Uy Susanoo",     "khien",  0x1E2450, 0x93A2F5, 0xE6EBFF},
        {232, "Luân Hồi Nhãn",       "vong",   0x122A4A, 0x8CBEF7, 0xE8F1FF},
        {233, "Bát Môn Độn Giáp",    "cong",   0x4A1A02, 0xFF9440, 0xFFD98A},
        {237, "Nhẫn Giả Đào Vong",   "saoden", 0x2A1214, 0xF07878, 0xFFE0E0},
        {238, "Thiên Bảng Đệ Nhất",  "sao1",   0x4A3808, 0xFFD84A, 0xFFFBE6},
        {239, "Thiên Bảng Đệ Nhị",   "sao2",   0x33363C, 0xD8DDE6, 0xFFFFFF},
        {240, "Thiên Bảng Đệ Tam",   "sao3",   0x3E2510, 0xDC9A5A, 0xFFEEDC},
        {242, "Thiên Bảng Đệ Tứ",    "sao4",   0x1C2F40, 0x8FB8D8, 0xEAF4FF},
        {243, "Thiên Bảng Đệ Ngũ",   "sao5",   0x24291F, 0xAFC0A8, 0xF0F5F0},
        {244, "Mangekyo Sharingan",  "mat1",   0x4A0A10, 0xF06068, 0xFFD9DB},
        {254, "Akatsuki",            "may",    0x1A1A1E, 0xE84A54, 0xFFE0E0},
    };

    private static final int SO_KHUNG = 8;
    private static final int RONG = 124;   // < 127
    private static final int CAO = 40;
    private static final int DY = -78;

    public static void main(String[] args) throws Exception {
        String goc = args.length > 0 ? args[0] : "work/server/NSO_KEM/Data/Img/Effect";
        String raSql = args.length > 1 ? args[1] : "/tmp/bang-dep.sql";
        StringBuilder sql = new StringBuilder("-- Biểu ngữ danh hiệu bản hoạ tiết, sinh bằng tools/VeBangDep.java\n");
        for (Object[] d : DS) {
            int ma = (Integer) d[0];
            ve(goc, ma, (String) d[1], (String) d[2], (Integer) d[3], (Integer) d[4], (Integer) d[5]);
            sql.append(cauSql(ma)).append('\n');
            System.out.println("  " + ma + "  " + d[1]);
        }
        try (Writer w = new OutputStreamWriter(new FileOutputStream(raSql), StandardCharsets.UTF_8)) {
            w.write(sql.toString());
        }
        System.out.println("xong " + DS.length + " biểu ngữ -> " + raSql);
    }

    private static void ve(String goc, int ma, String chu, String motif, int nen, int mau, int sang)
            throws Exception {
        int z = 4, W = RONG * z, H = CAO * z;
        BufferedImage to = new BufferedImage(W, H * SO_KHUNG, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = to.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

        Color cMau = new Color(mau), cSang = new Color(sang), cNen = new Color(nen);
        int huy = 30 * z;                                   // đường kính huy hiệu
        BufferedImage motifImg = VeIconDanhHieu.veMotif(motif, mau, huy);

        // Chữ nằm bên phải huy hiệu, tự co cho vừa phần còn lại.
        Font f = null;
        FontMetrics fm = null;
        int oChu = W - huy - 16 * z;
        for (int co = 22 * z; co >= 14; co -= 2) {
            f = new Font("SansSerif", Font.BOLD, co);
            fm = g.getFontMetrics(f);
            if (fm.stringWidth(chu) <= oChu) {
                break;
            }
        }
        g.setFont(f);

        for (int k = 0; k < SO_KHUNG; k++) {
            int y0 = k * H;
            float gy = y0 + H / 2f;
            float nhip = (float) (0.5 + 0.5 * Math.sin(k / (double) SO_KHUNG * Math.PI * 2));

            // 1. hào quang sau huy hiệu, phập phồng
            float hx = 8 * z + huy / 2f;
            g.setPaint(new RadialGradientPaint(new Point2D.Float(hx, gy), huy * (0.75f + 0.25f * nhip),
                    new float[] {0f, 1f},
                    new Color[] {new Color(cSang.getRed(), cSang.getGreen(), cSang.getBlue(),
                            (int) (90 + 60 * nhip)), new Color(mau & 0xFFFFFF, true)}));
            g.fill(new Ellipse2D.Float(hx - huy, gy - huy, huy * 2, huy * 2));

            // 2. hai cánh vuốt ra
            for (int b = -1; b <= 1; b += 2) {
                float x = b < 0 ? 6 * z : W - 6 * z;
                Path2D canh = new Path2D.Float();
                canh.moveTo(x, gy);
                canh.quadTo(x + b * 14 * z, gy - 12 * z, x + b * 26 * z, gy - 4 * z);
                canh.quadTo(x + b * 14 * z, gy - 2 * z, x, gy);
                canh.closePath();
                g.setColor(new Color(cMau.getRed(), cMau.getGreen(), cMau.getBlue(), 150));
                g.fill(canh);
                AffineTransform cu = g.getTransform();
                g.translate(0, gy * 2);
                g.scale(1, -1);
                g.fill(canh);
                g.setTransform(cu);
            }

            // 3. tấm nền hai lớp
            Shape ngoai = new RoundRectangle2D.Float(4 * z, y0 + 8 * z, W - 8 * z, H - 16 * z,
                    9 * z, 9 * z);
            g.setPaint(new GradientPaint(0, y0 + 8 * z, new Color(cNen.getRed(), cNen.getGreen(),
                    cNen.getBlue(), 245), 0, y0 + H - 8 * z, new Color(0, 0, 0, 225)));
            g.fill(ngoai);
            g.setStroke(new BasicStroke(1.6f * z));
            g.setColor(new Color(cMau.getRed(), cMau.getGreen(), cMau.getBlue(), 235));
            g.draw(ngoai);
            g.setStroke(new BasicStroke(0.8f * z));
            g.setColor(new Color(255, 255, 255, 45));
            g.draw(new RoundRectangle2D.Float(6.5f * z, y0 + 10.5f * z, W - 13 * z, H - 21 * z,
                    7 * z, 7 * z));

            // 4. huy hiệu tròn: cùng hoạ tiết với icon trong túi
            g.setPaint(new GradientPaint(hx - huy / 2f, gy - huy / 2f, new Color(cNen.getRGB()),
                    hx + huy / 2f, gy + huy / 2f, Color.BLACK));
            g.fill(new Ellipse2D.Float(hx - huy / 2f, gy - huy / 2f, huy, huy));
            g.setStroke(new BasicStroke(1.4f * z));
            g.setColor(cMau);
            g.draw(new Ellipse2D.Float(hx - huy / 2f, gy - huy / 2f, huy, huy));
            g.drawImage(motifImg, (int) (hx - huy / 2f), (int) (gy - huy / 2f), null);

            // 5. chữ với vệt sáng chạy
            int x = (int) (hx + huy / 2f + 5 * z);
            int y = (int) (gy + (fm.getAscent() - fm.getDescent()) / 2f);
            g.setColor(new Color(0, 0, 0, 230));
            for (int dx = -2; dx <= 2; dx += 2) {
                for (int dy = -2; dy <= 2; dy += 2) {
                    if (dx != 0 || dy != 0) {
                        g.drawString(chu, x + dx, y + dy);
                    }
                }
            }
            float tam = (float) k / SO_KHUNG * (W + 200) - 100;
            g.setPaint(new LinearGradientPaint(new Point((int) (tam - 90), 0),
                    new Point((int) (tam + 90), 0), new float[] {0f, 0.5f, 1f},
                    new Color[] {cMau, cSang, cMau}));
            g.drawString(chu, x, y);
        }
        g.dispose();

        for (int m = 1; m <= 4; m++) {
            BufferedImage ra = new BufferedImage(RONG * m, CAO * m * SO_KHUNG, BufferedImage.TYPE_INT_ARGB);
            Graphics2D gg = ra.createGraphics();
            gg.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            gg.drawImage(to, 0, 0, ra.getWidth(), ra.getHeight(), null);
            gg.dispose();
            File out = new File(goc + File.separator + m, ma + ".png");
            out.getParentFile().mkdirs();
            ImageIO.write(ra, "png", out);
        }
    }

    private static String cauSql(int ma) {
        StringBuilder sp = new StringBuilder("["), fr = new StringBuilder("["), rn = new StringBuilder("[");
        for (int i = 0; i < SO_KHUNG; i++) {
            if (i > 0) {
                sp.append(',');
                fr.append(',');
            }
            sp.append("{\"id\":").append(i).append(",\"x\":0,\"y\":").append(i * CAO)
              .append(",\"w\":").append(RONG).append(",\"h\":").append(CAO).append('}');
            fr.append("[{\"id\":").append(i).append(",\"dx\":").append(-RONG / 2)
              .append(",\"dy\":").append(DY).append(",\"onTop\":0,\"flip\":0}]");
            for (int k = 0; k < 3; k++) {
                rn.append(i == 0 && k == 0 ? "" : ",").append(i);
            }
        }
        return "REPLACE INTO `effect_data` (`id`,`sprites`,`frames`,`running`,`frame_char`) VALUES ("
                + ma + ",'" + sp + "]','" + fr + "]','" + rn + "]','[[],[],[],[]]');";
    }
}
