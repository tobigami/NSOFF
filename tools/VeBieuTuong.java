import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.charset.StandardCharsets;

/**
 * Biểu tượng danh hiệu bay trên đầu -- KHÔNG chữ.
 *
 * Vì sao bỏ chữ: một con mắt Sharingan quay là đã nói hết ý, không cần dòng "Uchiha Nhất Tộc" nào.
 * Chữ ở cỡ 18-22px lại buộc phải to và thô mới đọc nổi, chiếm hết chỗ mà chẳng thêm được gì. Đổi
 * lại, biểu tượng dùng ĐÚNG hoạ tiết của icon món đó (mượn VeIconDanhHieu.veMotif) nên nhìn dòng
 * trên đầu là biết ngay món nào trong túi.
 *
 * Mỗi danh hiệu có một kiểu động riêng:
 *   quay  - xoay tròn, hợp với vòng luân hồi, phong ấn, ngôi sao
 *   nhip  - phồng lên xẹp xuống kèm hào quang, hợp với con mắt
 *   chop  - rung và bập bùng, hợp với lửa
 *   toa   - tia sáng quay phía sau, hoạ tiết đứng yên
 *
 * Hai trần phải nhớ: bề rộng ô sprite KHÔNG quá 127 (máy khách giữ trong byte có dấu), chiều cao
 * nên dưới 132 -- mức cao nhất trong 230 hiệu ứng sẵn có của game.
 */
public class VeBieuTuong {

    /** {mã hiệu ứng, hoạ tiết, kiểu động, màu chính, màu hào quang} */
    /** {mã hiệu ứng, hoạ tiết, kiểu động, màu chính, màu hào quang, nhãn} */
    private static final Object[][] DS = {
        {180, "cuon",   "toa",  0xF2D97A, 0xFFF6D0, "Tam Nhẫn"},
        {181, "xoay",   "quay", 0x7BE8B4, 0xE9FFF6, "Tiên Nhân"},
        {188, "thu",    "quay", 0xD98BEE, 0xFFE6FF, "Nhân Trụ Lực"},
        {189, "bong",   "nhip", 0x86D8F5, 0xE4F7FF, "Băng Độn"},
        {190, "kiem2",  "toa",  0xDCE6F2, 0xFFFFFF, "Kiếm Hào"},
        {191, "matna",  "nhip", 0xA8B8D6, 0xE6EEFF, "Ám Bộ"},
        {214, "mat3",   "quay", 0xF06068, 0xFFD9DB, "Uchiha"},
        {215, "la",     "nhip", 0x7FDC90, 0xE7FFE9, "Senju"},
        {216, "duoi",   "chop", 0xFFA245, 0xFFE2A8, "Cửu Vĩ"},
        {217, "vuong",  "toa",  0xF7DE62, 0xFFFBE6, "Lục Đạo"},
        {218, "khien",  "nhip", 0x93A2F5, 0xE6EBFF, "Susanoo"},
        {232, "vong",   "quay", 0x8CBEF7, 0xE8F1FF, "Luân Hồi Nhãn"},
        {233, "cong",   "chop", 0xFF9440, 0xFFD98A, "Bát Môn"},
        {237, "saoden", "nhip", 0xF07878, 0xFFE0E0, "Đào Vong"},
        {238, "sao1",   "toa",  0xFFD84A, 0xFFFBE6, "Đệ Nhất"},
        {239, "sao2",   "toa",  0xD8DDE6, 0xFFFFFF, "Đệ Nhị"},
        {240, "sao3",   "toa",  0xDC9A5A, 0xFFEEDC, "Đệ Tam"},
        {242, "sao4",   "toa",  0x8FB8D8, 0xEAF4FF, "Đệ Tứ"},
        {243, "sao5",   "toa",  0xAFC0A8, 0xF0F5F0, "Đệ Ngũ"},
        {244, "mat1",   "quay", 0xF06068, 0xFFD9DB, "Mangekyo"},
        {254, "may",    "nhip", 0xE84A54, 0xFFE0E0, "Akatsuki"},
    };

    private static final int SO_KHUNG = 8;
    private static final int RONG = 96;   // < 127, trần bề rộng ô sprite của máy khách
    private static final int CAO = 54;    // 40 cho biểu tượng + 14 cho dải nhãn
    private static final int BT = 40;     // phần dành cho biểu tượng
    private static final int DY = -80;

    public static void main(String[] args) throws Exception {
        String goc = args.length > 0 ? args[0] : "work/server/NSO_KEM/Data/Img/Effect";
        String raSql = args.length > 1 ? args[1] : "/tmp/bieu-tuong.sql";
        StringBuilder sql = new StringBuilder("-- Biểu tượng danh hiệu, sinh bằng tools/VeBieuTuong.java\n");
        for (Object[] d : DS) {
            int ma = (Integer) d[0];
            ve(goc, ma, (String) d[1], (String) d[2], (Integer) d[3], (Integer) d[4],
                    (String) d[5]);
            sql.append(cauSql(ma)).append('\n');
            System.out.println("  " + ma + "  " + d[1] + " / " + d[2]);
        }
        try (Writer w = new OutputStreamWriter(new FileOutputStream(raSql), StandardCharsets.UTF_8)) {
            w.write(sql.toString());
        }
        System.out.println("xong " + DS.length + " biểu tượng -> " + raSql);
    }

    private static void ve(String goc, int ma, String motif, String kieu, int mau, int sang,
            String nhan) throws Exception {
        int z = 4, W = RONG * z, H = CAO * z, S = BT * z;
        BufferedImage to = new BufferedImage(W, H * SO_KHUNG, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = to.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        Color cMau = new Color(mau), cSang = new Color(sang);
        int cd = (int) (S * 0.74);                       // cạnh hoạ tiết
        BufferedImage hinh = VeIconDanhHieu.veMotif(motif, mau, cd);

        for (int k = 0; k < SO_KHUNG; k++) {
            int y0 = k * H;
            float gx = W / 2f, gy = y0 + S / 2f;
            double pha = k / (double) SO_KHUNG;
            float nhip = (float) (0.5 + 0.5 * Math.sin(pha * Math.PI * 2));

            // Hào quang toả sau, phập phồng theo khung -- thứ làm biểu tượng "sống".
            float ban = S * (0.34f + 0.10f * nhip);
            g.setPaint(new RadialGradientPaint(new Point2D.Float(gx, gy), ban,
                    new float[] {0f, 0.55f, 1f},
                    new Color[] {new Color(cSang.getRed(), cSang.getGreen(), cSang.getBlue(),
                            (int) (120 + 70 * nhip)),
                        new Color(cMau.getRed(), cMau.getGreen(), cMau.getBlue(), (int) (70 + 40 * nhip)),
                        new Color(mau & 0xFFFFFF, true)}));
            g.fill(new Ellipse2D.Float(gx - ban, gy - ban, ban * 2, ban * 2));

            if ("toa".equals(kieu)) {                    // tia sáng quay phía sau
                g.setStroke(new BasicStroke(2.2f * z, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                for (int i = 0; i < 8; i++) {
                    double a = Math.toRadians(i * 45 + pha * 45);
                    g.setColor(new Color(cSang.getRed(), cSang.getGreen(), cSang.getBlue(),
                            i % 2 == 0 ? 120 : 60));
                    g.draw(new Line2D.Double(gx + Math.cos(a) * S * 0.24, gy + Math.sin(a) * S * 0.24,
                            gx + Math.cos(a) * S * 0.44, gy + Math.sin(a) * S * 0.44));
                }
            }

            AffineTransform cu = g.getTransform();
            g.translate(gx, gy);
            if ("quay".equals(kieu)) {
                g.rotate(pha * Math.PI * 2);
            } else if ("nhip".equals(kieu)) {
                double t = 0.88 + 0.12 * nhip;
                g.scale(t, t);
            } else if ("chop".equals(kieu)) {
                double t = 0.9 + 0.1 * ((k * 5) % 3) / 2.0;
                g.scale(t, 1.0 / t);
                g.rotate(Math.toRadians((k % 2 == 0 ? 1 : -1) * 3));
            }
            g.drawImage(hinh, -cd / 2, -cd / 2, null);
            g.setTransform(cu);

            // Nhãn nhỏ dưới chân biểu tượng.
            //
            // Chữ chỉ để xác nhận, không phải để đọc từ xa -- nên cỡ nhỏ, nằm gọn trong một dải
            // tối mảnh. Biểu tượng vẫn là thứ nhìn phát biết ngay.
            if (nhan != null && !nhan.isEmpty()) {
                Font f = null;
                FontMetrics fm = null;
                for (int co = 11 * z; co >= 5 * z; co -= 2) {
                    f = new Font("SansSerif", Font.BOLD, co);
                    fm = g.getFontMetrics(f);
                    if (fm.stringWidth(nhan) <= W - 10 * z) {
                        break;
                    }
                }
                g.setFont(f);
                int rongChu = fm.stringWidth(nhan);
                float dy = y0 + S + (H - S) / 2f;
                g.setColor(new Color(0, 0, 0, 170));
                g.fill(new RoundRectangle2D.Float((W - rongChu) / 2f - 3 * z, dy - 4.6f * z,
                        rongChu + 6 * z, 9.2f * z, 4 * z, 4 * z));
                int x = (W - rongChu) / 2;
                int y = (int) (dy + (fm.getAscent() - fm.getDescent()) / 2f);
                g.setColor(new Color(0, 0, 0, 220));
                for (int dx2 = -1; dx2 <= 1; dx2 += 2) {
                    for (int dy2 = -1; dy2 <= 1; dy2 += 2) {
                        g.drawString(nhan, x + dx2 * z / 2, y + dy2 * z / 2);
                    }
                }
                g.setColor(nhip > 0.5f ? cSang : cMau);
                g.drawString(nhan, x, y);
            }
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
            // Ba nhịp mỗi khung: đủ chậm để thấy hoạ tiết, đủ nhanh để thấy nó động.
            for (int k = 0; k < 3; k++) {
                rn.append(i == 0 && k == 0 ? "" : ",").append(i);
            }
        }
        return "REPLACE INTO `effect_data` (`id`,`sprites`,`frames`,`running`,`frame_char`) VALUES ("
                + ma + ",'" + sp + "]','" + fr + "]','" + rn + "]','[[],[],[],[]]');";
    }
}
