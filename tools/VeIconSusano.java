import javax.imageio.ImageIO;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RadialGradientPaint;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.File;

/**
 * Icon cho Susanoo -- thay cái khung nâu chung chung mà mọi bí kíp đều dùng.
 *
 * VÌ SAO KHÔNG VẼ NGUYÊN CON SUSANOO THU NHỎ: mức phóng 1 chỉ có 20x20 điểm ảnh. Thu cả thân,
 * khiên và kiếm vào đó thì mọi chi tiết nhoè thành một vũng đỏ. Soát mấy icon mạnh sẵn có trong
 * game (Sukaigan là một con mắt kín khung, Cầu lục đạo là một quả cầu) thì thấy luật chung: icon
 * nào cũng chỉ CÓ MỘT hình, tràn viền, không khung.
 *
 * Nên ở đây lấy đúng phần dễ nhận nhất của Susanoo -- cái mặt nạ quỷ có sừng -- vẽ chính diện,
 * và dồn điểm sáng mạnh nhất vào hai con mắt. Ở 20x20 người chơi đọc ra ba thứ theo thứ tự:
 * quầng đỏ, bóng sừng, hai đốm mắt cháy. Chừng đó là đủ để không lẫn với món nào khác.
 *
 * ĐÃ THỬ VÀ BỎ: nhét cả khiên Bát Chỉ Kính lẫn kiếm Totsuka ra sau mặt nạ. Ở khung vuông, hai
 * món đó không có chỗ nào đứng mà không đè lên gò má, nên chúng biến thành hai vật thể trôi nổi
 * hai bên đầu -- rối mà vẫn chẳng ai nhận ra là khiên với kiếm. Thay bằng mấy lưỡi lửa toả sau
 * đầu: cùng nhiệm vụ lấp góc trống, nhưng không tranh chỗ đọc với mặt nạ.
 *
 * Vẽ một lần ở 640x640 rồi thu dần bằng cách chia đôi nhiều bước. Thu thẳng 640 -> 20 trong một
 * nhát thì phép nội suy hai tuyến chỉ lấy 2x2 điểm quanh chỗ cần, bỏ sót gần hết ảnh gốc và ra
 * rìa răng cưa; chia đôi từng bước thì mỗi bước đều gộp đủ bốn điểm, xuống tới 20 vẫn mượt.
 */
public class VeIconSusano {

    /** Bốn mức phóng của máy khách, theo đúng thứ tự thư mục Small/1..4. */
    private static final int[] CO = {20, 40, 60, 80};

    /** Susanoo của ai -- chọn bằng -Dnhanvat=itachi|sasuke, khớp với VeSusano. */
    private static final String NHAN_VAT = System.getProperty("nhanvat", "itachi").toLowerCase();
    private static final boolean SASUKE = "sasuke".equals(NHAN_VAT);
    private static final boolean MADARA = "madara".equals(NHAN_VAT);

    // Chỉ đổi BẢNG MÀU, giữ nguyên hình. Hai Susanoo cùng một bộ giáp chakra, khác nhau ở màu --
    // và ở cỡ icon 20x20 thì màu gần như là thứ duy nhất còn phân biệt được.
    private static final Color DO = new Color(MADARA ? 0x3A6FE0 : (SASUKE ? 0x8A3FD0 : 0xD8322C));      // màu chính
    private static final Color DO_TOI = new Color(MADARA ? 0x2350B8 : (SASUKE ? 0x6B23AE : 0xB8281F));  // đáy khối, vẫn sáng hơn nền
    private static final Color SANG = new Color(MADARA ? 0xC2DBFF : (SASUKE ? 0xE2C4FF : 0xFFC08A));    // viền
    private static final Color CHAY = new Color(MADARA ? 0xEDF5FF : (SASUKE ? 0xF6ECFF : 0xFFF3D0));    // lõi mắt

    public static void main(String[] args) throws Exception {
        String goc = args.length > 0 ? args[0] : "work/server/NSO_KEM/Data/Img/Small";
        int ma = args.length > 1 ? Integer.parseInt(args[1]) : 6502;

        BufferedImage lon = ve(640);
        for (int i = 0; i < CO.length; i++) {
            BufferedImage ra = thuNho(lon, CO[i]);
            File out = new File(goc + File.separator + (i + 1), "Small" + ma + ".png");
            out.getParentFile().mkdirs();
            ImageIO.write(ra, "png", out);
            ghiBanXem(ra, out);
            System.out.println("  " + out.getPath() + "  " + CO[i] + "x" + CO[i]);
        }
        System.out.println("xong icon " + ma);
    }

    /**
     * Ghi bản xem NGAY CẠNH ảnh thật, đổi đuôi .png thành .temp.png.
     *
     * Icon mức 1 chỉ 20x20, mở bằng trình xem ảnh thì bé bằng hạt đậu, chẳng soi được gì. Bản xem
     * phóng to theo kiểu răng cưa (không làm mượt) nên hiện ĐÚNG từng điểm ảnh máy khách sẽ vẽ.
     * Máy chủ đọc ảnh theo đường dẫn chính xác (chỉ Data/Lang mới bị quét cả thư mục) nên mấy
     * file .temp.png nằm cạnh là vô hại.
     */
    private static void ghiBanXem(BufferedImage ic, File that) throws Exception {
        int o = 360, cao = o + 30;
        BufferedImage t = new BufferedImage(o, cao, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = t.createGraphics();
        g.setColor(new Color(0x14141A));
        g.fillRect(0, 0, o, cao);
        g.setColor(new Color(0x2A2A33));
        g.fillRect(0, 0, o, o);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g.drawImage(ic, 0, 0, o, o, null);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(new Color(0xC8C8D2));
        g.setFont(new java.awt.Font("SansSerif", java.awt.Font.PLAIN, 14));
        g.drawString(that.getName() + "  -  " + ic.getWidth() + "x" + ic.getHeight()
                + "  (phong to, dung tung diem anh)", 6, o + 20);
        int th = ic.getWidth(), lx = o - th - 10, ly = o - th - 10;
        g.setColor(new Color(0x14141A));
        g.fillRect(lx - 4, ly - 4, th + 8, th + 8);
        g.setColor(new Color(0x50505E));
        g.drawRect(lx - 4, ly - 4, th + 8, th + 8);
        g.drawImage(ic, lx, ly, null);
        g.dispose();
        ImageIO.write(t, "png", new File(that.getParentFile(),
                that.getName().replaceAll("\\.png$", ".temp.png")));
    }

    /** Chia đôi dần cho tới khi còn trong khoảng gấp đôi đích, rồi mới hạ nốt bước cuối. */
    private static BufferedImage thuNho(BufferedImage nguon, int dich) {
        BufferedImage cur = nguon;
        int w = cur.getWidth();
        while (w / 2 >= dich) {
            w /= 2;
            BufferedImage b = new BufferedImage(w, w, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = b.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g.drawImage(cur, 0, 0, w, w, null);
            g.dispose();
            cur = b;
        }
        if (w == dich) {
            return cur;
        }
        BufferedImage ra = new BufferedImage(dich, dich, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = ra.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.drawImage(cur, 0, 0, dich, dich, null);
        g.dispose();
        return ra;
    }

    private static BufferedImage ve(int n) {
        BufferedImage img = new BufferedImage(n, n, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

        float cx = n * 0.5f;

        // ---- 1. Quầng chakra: hai lớp, ngoài rộng mờ, trong hẹp đậm ----
        // Một lớp thôi thì ra cái đèn nền dán phía sau; hai lớp mới thành thứ tự bốc ra từ hình.
        g.setPaint(new RadialGradientPaint(new Point2D.Float(cx, n * 0.52f), n * 0.50f,
                new float[] {0f, 0.62f, 0.88f, 1f},
                new Color[] {MADARA ? new Color(10, 22, 54, 250) : (SASUKE ? new Color(34, 10, 58, 250) : new Color(58, 8, 12, 250)),
                    MADARA ? new Color(7, 15, 40, 242) : (SASUKE ? new Color(24, 6, 42, 242) : new Color(40, 5, 9, 242)),
                    MADARA ? new Color(4, 8, 24, 190) : (SASUKE ? new Color(14, 3, 26, 190) : new Color(24, 2, 5, 190)),
                    MADARA ? new Color(2, 4, 14, 0) : (SASUKE ? new Color(8, 1, 16, 0) : new Color(14, 1, 3, 0))}));
        g.fill(new Ellipse2D.Float(0, 0, n, n));
        g.setPaint(new RadialGradientPaint(new Point2D.Float(cx, n * 0.50f), n * 0.34f,
                new float[] {0f, 1f},
                new Color[] {MADARA ? new Color(70, 130, 235, 130) : (SASUKE ? new Color(150, 70, 220, 130) : new Color(220, 60, 34, 130)),
                    MADARA ? new Color(28, 70, 170, 0) : (SASUKE ? new Color(90, 30, 160, 0) : new Color(150, 24, 18, 0))}));
        g.fill(new Ellipse2D.Float(n * 0.16f, n * 0.16f, n * 0.68f, n * 0.68f));

        // ---- 2. (bỏ trống) ----
        // Từng thử lưỡi lửa toả quanh đầu. Vùng trên đã kín sừng, nên lửa chỉ còn chỗ ở ngang
        // tầm tai -- và đó đúng là thứ nó biến thành: hai cái tai nhọn. Hạ xuống dưới thì lửa
        // chúc ngược, còn sai hơn. Để quầng sáng lo phần nền, hình chỉ giữ MỘT khối là mặt nạ.

        // ---- 3. Sừng: hai cánh cong vuốt lên, thứ giữ được bóng ở cỡ nhỏ nhất ----
        veSung(g, n, cx, false);
        veSung(g, n, cx, true);

        // ---- 4. Mũ giáp và mặt nạ ----
        // Dáng: đỉnh tròn, phình ở gò má, vuốt nhọn xuống cằm -- bóng của mặt nạ quỷ.
        Path2D mat = new Path2D.Float();
        mat.moveTo(cx, n * 0.185f);
        mat.curveTo(cx + n * 0.150f, n * 0.195f, cx + n * 0.205f, n * 0.330f, cx + n * 0.200f, n * 0.470f);
        mat.curveTo(cx + n * 0.196f, n * 0.610f, cx + n * 0.120f, n * 0.745f, cx, n * 0.815f);
        mat.curveTo(cx - n * 0.120f, n * 0.745f, cx - n * 0.196f, n * 0.610f, cx - n * 0.200f, n * 0.470f);
        mat.curveTo(cx - n * 0.205f, n * 0.330f, cx - n * 0.150f, n * 0.195f, cx, n * 0.185f);
        mat.closePath();
        // Bóng đổ sát ngoài mép: một vành tối mảnh, đủ để mặt nạ không dính vào nền.
        g.setColor(mau(0x0A0102, 0x06010E, 170));
        g.setStroke(new BasicStroke(n * 0.038f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(mat);
        veKhoi(g, mat, DO, SANG, 240, 235, n);

        // Tấm trán: dải sẫm cắt ngang, tách phần mũ khỏi phần mặt
        Path2D tran = new Path2D.Float();
        tran.moveTo(cx - n * 0.185f, n * 0.395f);
        tran.curveTo(cx - n * 0.09f, n * 0.350f, cx + n * 0.09f, n * 0.350f, cx + n * 0.185f, n * 0.395f);
        tran.curveTo(cx + n * 0.150f, n * 0.436f, cx - n * 0.150f, n * 0.436f, cx - n * 0.185f, n * 0.395f);
        tran.closePath();
        g.setColor(mau(0x8A1A16, 0x5A22A0, 235));
        g.fill(tran);
        g.setColor(mau(0xFFC08A, 0xD6B4FF, 150));
        g.setStroke(new BasicStroke(n * 0.010f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(tran);

        // Sống mũi: một nét dọc, đủ để mặt không phẳng
        g.setColor(mau(0x7E1210, 0x4B1690, 190));
        g.setStroke(new BasicStroke(n * 0.020f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(new Line2D.Float(cx, n * 0.470f, cx, n * 0.615f));

        // ---- 5. Mõm: nan dọc kiểu mặt nạ quỷ ----
        g.setColor(mau(0x5C0E0C, 0x36105C, 225));
        Path2D mom = new Path2D.Float();
        mom.moveTo(cx - n * 0.098f, n * 0.640f);
        mom.curveTo(cx, n * 0.612f, cx, n * 0.612f, cx + n * 0.098f, n * 0.640f);
        mom.curveTo(cx + n * 0.072f, n * 0.735f, cx - n * 0.072f, n * 0.735f, cx - n * 0.098f, n * 0.640f);
        mom.closePath();
        g.fill(mom);
        Shape luuMom = g.getClip();
        g.clip(mom);
        g.setColor(mau(0xFF965A, 0xBE82FF, 165));
        g.setStroke(new BasicStroke(n * 0.014f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER));
        for (int i = -2; i <= 2; i++) {
            float x = cx + i * n * 0.038f;
            g.draw(new Line2D.Float(x, n * 0.612f, x, n * 0.740f));
        }
        g.setClip(luuMom);
        g.setColor(mau(0xFFC08A, 0xD6B4FF, 140));
        g.setStroke(new BasicStroke(n * 0.011f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(mom);

        // ---- 6. MẮT -- thứ sáng nhất ảnh, chỗ duy nhất còn đọc được ở 20x20 ----
        veMat(g, n, cx - n * 0.105f, n * 0.505f, false);
        veMat(g, n, cx + n * 0.105f, n * 0.505f, true);

        // ---- 7. Viền hắt sáng mép trên trái: tách mặt nạ khỏi nền đỏ phía sau ----
        g.setColor(mau(0xFFD6AA, 0xDCC2FF, 130));
        g.setStroke(new BasicStroke(n * 0.013f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(new Arc2D.Float(cx - n * 0.200f, n * 0.185f, n * 0.400f, n * 0.430f, 108, 82, Arc2D.OPEN));

        g.dispose();
        return img;
    }

    /** Sừng cong vuốt lên, gốc dày ngọn nhọn. */
    private static void veSung(Graphics2D g, int n, float cx, boolean phai) {
        float d = phai ? 1f : -1f;
        // Gốc bám hẳn vào vành mũ rồi mới vuốt ra: bản trước gốc quá mảnh và tách rời, nhìn
        // thành hai cái râu côn trùng chứ không phải sừng mọc từ giáp.
        Path2D s = new Path2D.Float();
        s.moveTo(cx + d * n * 0.108f, n * 0.318f);
        s.curveTo(cx + d * n * 0.290f, n * 0.300f, cx + d * n * 0.392f, n * 0.212f,
                cx + d * n * 0.430f, n * 0.072f);
        s.curveTo(cx + d * n * 0.352f, n * 0.152f, cx + d * n * 0.268f, n * 0.196f,
                cx + d * n * 0.098f, n * 0.222f);
        s.closePath();
        veKhoi(g, s, DO, SANG, 238, 200, n);
    }

    /** Mắt: bloom toả ra ngoài, hình hạnh nhân, lõi trắng nóng. */
    private static void veMat(Graphics2D g, int n, float x, float y, boolean phai) {
        float w = n * 0.108f, h = n * 0.062f;
        g.setPaint(new RadialGradientPaint(new Point2D.Float(x, y), w * 1.55f,
                new float[] {0f, 1f},
                new Color[] {mau(0xFFBE78, 0xC896FF, 175), mau(0xFF8C46, 0x9650E6, 0)}));
        g.fill(new Ellipse2D.Float(x - w * 1.55f, y - w * 1.55f, w * 3.1f, w * 3.1f));

        float d = phai ? 1f : -1f;
        Path2D e = new Path2D.Float();
        e.moveTo(x - d * w * 0.98f, y + h * 0.10f);
        e.curveTo(x - d * w * 0.40f, y - h * 1.00f, x + d * w * 0.55f, y - h * 0.86f,
                x + d * w * 0.98f, y - h * 0.10f);
        e.curveTo(x + d * w * 0.50f, y + h * 0.92f, x - d * w * 0.42f, y + h * 0.98f,
                x - d * w * 0.98f, y + h * 0.10f);
        e.closePath();
        g.setColor(mau(0xFFECBE, 0xF0E1FF, 250));
        g.fill(e);
        g.setColor(CHAY);
        g.fill(new Ellipse2D.Float(x - w * 0.34f, y - h * 0.40f, w * 0.68f, h * 0.80f));
        g.setColor(mau(0x7E1210, 0x4B1690, 235));
        g.setStroke(new BasicStroke(n * 0.011f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(e);
    }

    /** Tô khối: dốc sáng trên tối dưới, viền, rồi vệt hắt sáng mép trên. */
    private static void veKhoi(Graphics2D g, Shape s, Color nen, Color vien, int aNen, int aVien, int n) {
        java.awt.Rectangle b = s.getBounds();
        g.setPaint(new java.awt.GradientPaint(0, b.y, pha(nen, 34, aNen),
                0, b.y + b.height, pha(DO_TOI, -10, aNen)));
        g.fill(s);
        g.setColor(new Color(vien.getRed(), vien.getGreen(), vien.getBlue(), ap(aVien - 70)));
        g.setStroke(new BasicStroke(n * 0.012f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(s);
    }

    private static Color pha(Color c, int them, int alpha) {
        return new Color(ap(c.getRed() + them), ap(c.getGreen() + them), ap(c.getBlue() + them), ap(alpha));
    }

    /**
     * Chọn màu theo nhân vật. Mấy chi tiết nhỏ (vành trán, mõm, quầng mắt) trước đây viết cứng
     * màu cam ngay trong thân hàm, nên đổi bảng màu chính sang tím thì chúng vẫn ám cam -- mặt
     * tím mà viền cam, nhìn như tô nhầm. Gom hết về đây.
     */
    private static Color mau(int itachi, int sasuke, int alpha) {
        int v = MADARA ? doiSangXanh(itachi) : (SASUKE ? sasuke : itachi);
        return new Color((v >> 16) & 255, (v >> 8) & 255, v & 255, ap(alpha));
    }

    /**
     * Đổi một màu ấm sang tông xanh bằng cách hoán vị kênh: đỏ nhất -> xanh lam nhất.
     *
     * Làm vậy thay vì liệt kê tay từng màu cho Madara: mấy chi tiết nhỏ (vành trán, mõm, quầng
     * mắt) chỉ cần đúng TÔNG, không cần chọn riêng. Liệt kê tay ba bộ màu cho ba nhân vật là ba
     * chỗ phải nhớ sửa mỗi lần chỉnh, mà chỉnh sót một cái là mặt xanh viền cam ngay.
     */
    private static int doiSangXanh(int rgb) {
        int r = (rgb >> 16) & 255, g = (rgb >> 8) & 255, b = rgb & 255;
        return (b << 16) | (g << 8) | r;
    }

    private static int ap(int v) {
        return v < 0 ? 0 : (v > 255 ? 255 : v);
    }
}
