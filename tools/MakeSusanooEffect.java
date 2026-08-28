import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.MultipleGradientPaint;
import java.awt.RadialGradientPaint;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

/**
 * Susanoo của Itachi: bán thân chakra bán trong suốt, mũ sừng, lồng ngực xương sườn, khiên Yata
 * bên trái và kiếm bên phải.
 *
 * CÁCH VẼ NÀY RÚT RA TỪ ĐO BẢN VẼ TAY CỦA USER (Effect/1/255.png), không phải tự nghĩ
 *
 * Đo ra ba con số quyết định tất cả:
 *   - pixel ĐẶC (alpha >= 224):  0
 *   - alpha cao nhất:            229
 *   - alpha trung bình:          51,7
 *
 * Nghĩa là Susanoo KHÔNG có mảng đặc nào. Toàn bộ hình là nét mảnh bán trong suốt chồng trên một
 * lớp sương rất nhạt -- nhìn xuyên qua được, đúng như khối chakra trong truyện. Hai bản trước tôi
 * tô mảng đặc rồi kẻ viền (alpha trung bình 206), nên ra một con robot sơn đỏ chứ không ra chakra.
 *
 * Ba điều nữa chép theo bản vẽ tay:
 *   - CHỈ HAI MÀU: đỏ thẫm cho nét thân, kem nhạt cho sườn / sừng / vành khiên. Bảy màu như bản
 *     trước làm hình vụn, mỗi mảng một tông.
 *   - Tua chakra là SỢI CONG XOẮN, không phải gạch thẳng. Gạch thẳng đọc ra là hạt bụi.
 *   - Quầng là một vệt tối rất mềm, không nhìn thấy mép. Đắp oval cứng thì ra vòng bia bắn.
 *
 * Khử răng cưa BẬT: bản vẽ tay có 1461 màu phân biệt trên một khung 124x96, tức là nó mượt chứ
 * không phải pixel art. Bản pixel tôi làm trước đó đi sai hướng.
 *
 * RÀNG BUỘC: x, y, w, h mỗi khung đi qua MỘT BYTE không dấu nên mỗi toạ độ phải <= 255. Bề rộng
 * cả tấm thì không sao -- client chỉ nhận toạ độ, không nhận kích thước tấm. Khung 124x96 xếp
 * lưới 3 cột: x lớn nhất 248, y lớn nhất 192, lọt.
 *
 *   java MakeSusanooEffect <thư mục ra> <idHiệuỨng> <mépDưới> [tênFile] [sốKhung] [itachi|sasuke]
 *                          [cung|magatama|kiem]
 *
 * Hai kiểu dùng CHUNG toàn bộ phần kỹ thuật (nét bán trong suốt, hai màu, quầng mềm, tua xoắn)
 * và chỉ khác bảng màu với mấy bộ phận đặc trưng:
 *   itachi -- đỏ, khiên Yata bát giác bên trái, kiếm bên phải
 *   sasuke -- TÍM, giáp vai tầng lớn, cung và mũi tên
 */
public final class MakeSusanooEffect {

    private static final int W = 124;
    private static final int H = 96;
    private static final int COLS = 3;

    private static final int BYTE_MAX = 255;
    private static final int SEQ_MAX = 126;

    /**
     * Hai bảng màu, mỗi bảng đúng HAI tông cộng một tông sáng cho khe mắt.
     *
     * Giữ đúng hai tông là chỗ học từ bản vẽ tay: dùng bảy màu như bản đầu của tôi thì hình vụn
     * ra, mỗi mảng một hướng. Susanoo Itachi đỏ, Susanoo Sasuke tím -- đó là dấu nhận biết nhân
     * vật, nên tông chính không được pha loãng.
     */
    private static Color RED = new Color(0xD8342A);
    private static Color CREAM = new Color(0xF0C89A);
    private static Color HOT = new Color(0xFFF4E0);
    private static Color HAZE = new Color(0xC03028);
    private static int VIGNETTE = 0x6B1410;

    private static boolean sasuke = false;

    /** Vũ khí của bản Sasuke: cung | magatama | kiem. */
    private static String weapon = "cung";

    private static void palette() {
        if (!sasuke) {
            return;
        }
        RED = new Color(0x9B4DE8);       // tím chakra
        CREAM = new Color(0xE2CCFF);     // tử đinh hương nhạt
        HOT = new Color(0xFFF2FF);
        HAZE = new Color(0x7A2CC0);
        VIGNETTE = 0x3A0B5C;
    }

    /** Thân Sasuke dịch sang phải bấy nhiêu pixel để nhường nửa trái cho cây cung. */
    private static final int BODY_DX = 14;

    /**
     * Góc nghiêng của cụm cung-tên, tính bằng radian, xoay quanh chỗ lắp tên.
     *
     * Để cung nằm ngang thì mũi tên chiếm gần trọn bề ngang khung -- hình bè ra hai bên, mất cân.
     * Nghiêng chéo lên thì cùng chiều dài ấy trải trên cả hai trục, bề ngang thu lại rõ rệt mà
     * cây cung vẫn nguyên kích thước.
     */
    private static final double BOW_TILT = 0.52;

    /** Chỗ lắp tên -- tâm xoay của cả cụm cung. */
    private static final double NOCK_X = 46;
    private static final double NOCK_Y = 52;

    private static int frameCount = 8;

    private MakeSusanooEffect() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 3) {
            System.out.println("dùng: java MakeSusanooEffect <thư mục ra> <idHiệuỨng> <mépDưới>"
                    + " [tênFile] [sốKhung]");
            System.exit(2);
        }
        File outDir = new File(args[0]);
        int effectId = Integer.parseInt(args[1]);
        int bottomY = Integer.parseInt(args[2]);
        String name = args.length > 3 ? args[3] : effectId + ".png";
        if (args.length > 4) {
            frameCount = Integer.parseInt(args[4]);
        }
        if (args.length > 5) {
            sasuke = args[5].equals("sasuke");
            if (!args[5].equals("sasuke") && !args[5].equals("itachi")) {
                System.out.println("!! kiểu lạ: " + args[5] + " (chỉ có itachi | sasuke)");
                System.exit(2);
            }
        }
        if (args.length > 6) {
            weapon = args[6];
            if (!weapon.equals("cung") && !weapon.equals("magatama")
                    && !weapon.equals("kiem") && !weapon.equals("saulung")
                    && !weapon.equals("kiemlung")) {
                System.out.println("!! vũ khí lạ: " + weapon
                        + " (cung | saulung | kiemlung | magatama | kiem)");
                System.exit(2);
            }
        }
        palette();

        int rows = (frameCount + COLS - 1) / COLS;
        int maxX = (Math.min(frameCount, COLS) - 1) * W;
        int maxY = (rows - 1) * H;
        if (maxX > BYTE_MAX || maxY > BYTE_MAX) {
            System.out.println("!! toạ độ khung lớn nhất " + maxX + "," + maxY + " vượt " + BYTE_MAX
                    + ": x và y mỗi khung đi qua một byte không dấu. Giảm sốKhung.");
            System.exit(1);
        }

        for (int z = 1; z <= 4; z++) {
            BufferedImage sheet = new BufferedImage(Math.min(frameCount, COLS) * W * z,
                    rows * H * z, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = sheet.createGraphics();
            for (int i = 0; i < frameCount; i++) {
                g.drawImage(frame(z, i), (i % COLS) * W * z, (i / COLS) * H * z, null);
            }
            g.dispose();
            File dir = new File(outDir, String.valueOf(z));
            dir.mkdirs();
            ImageIO.write(sheet, "png", new File(dir, name));
        }

        System.out.println("kích thước mức 1: khung " + W + "x" + H + ", tấm "
                + (Math.min(frameCount, COLS) * W) + "x" + (rows * H)
                + " (" + frameCount + " khung, lưới " + COLS + " cột x " + rows + " hàng)");
        System.out.println();
        System.out.println("sprites = " + sprites());
        System.out.println("frames  = " + framesJson(bottomY));
        System.out.println("running = " + running());
    }

    private static BufferedImage frame(int z, int index) {
        int w = W * z;
        int h = H * z;
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

        double t = 2 * Math.PI * index / frameCount;
        double breathe = 0.5 + 0.5 * Math.cos(t);
        double bob = Math.sin(t) * h * 0.008;

        vignette(g, w, h, breathe);

        AffineTransform old = g.getTransform();
        g.translate(0, bob);

        wisps(g, z, index, breathe);
        if (sasuke && weapon.equals("kiemlung")) {
            bowOnBack(g, z, breathe);
            torso(g, z, breathe);
            pauldrons(g, z, breathe);
            armsKatana(g, z);
            head(g, z, breathe);
            katana(g, z, breathe);
        } else if (sasuke && weapon.equals("saulung")) {
            // Cung đeo sau lưng: vẽ TRƯỚC thân nên thân che mất phần giữa, chỉ hai đầu cung ló
            // ra. Đó chính là chỗ ăn tiền của tư thế này -- cùng một cây cung mà chiếm ít bề
            // ngang hơn hẳn, vì phần thân cung nằm khuất chứ không chìa ngang.
            bowOnBack(g, z, breathe);
            torso(g, z, breathe);
            pauldrons(g, z, breathe);
            armsRest(g, z);
            head(g, z, breathe);
        } else if (sasuke && !weapon.equals("cung")) {
            // Tư thế triệu hồi: thân đứng giữa khung, hai tay giơ ngang, vũ khí xoay quanh.
            // Không phải nhường nửa khung cho cây cung nữa nên thân về đúng tâm.
            torso(g, z, breathe);
            pauldrons(g, z, breathe);
            armsRaised(g, z);
            head(g, z, breathe);
            if (weapon.equals("magatama")) {
                magatama(g, z, index, breathe);
            } else if (weapon.equals("kiem")) {
                katana(g, z, breathe);
            }
        } else if (sasuke) {
            // Cung chiếm hẳn nửa trái khung nên thân phải dịch sang phải, không thì cung đè lên
            // ngực. Cung, tên và tay vẽ theo toạ độ MÀN HÌNH; chỉ thân, giáp vai và đầu mới dịch,
            // vì tay phải nối được từ vai sang chỗ nắm cung.
            AffineTransform beforeBow = g.getTransform();
            g.rotate(BOW_TILT, p(z, NOCK_X), p(z, NOCK_Y));
            bow(g, z, breathe, true, 1.0);
            arrow(g, z, breathe);
            g.setTransform(beforeBow);
            AffineTransform keep = g.getTransform();
            g.translate(BODY_DX * z, 0);
            torso(g, z, breathe);
            pauldrons(g, z, breathe);
            head(g, z, breathe);
            g.setTransform(keep);
            armsSasuke(g, z);
        } else {
            blade(g, z, breathe);
            arms(g, z);
            torso(g, z, breathe);
            head(g, z, breathe);
            mirror(g, z, breathe);
        }
        sparks(g, z, index);

        g.setTransform(old);
        g.dispose();
        return img;
    }

    /** Nét mảnh bán trong suốt -- đơn vị dựng hình của cả bản này. */
    private static void line(Graphics2D g, int z, Shape s, Color c, int alpha, double width) {
        g.setStroke(new BasicStroke((float) Math.max(0.8, width * z), BasicStroke.CAP_ROUND,
                BasicStroke.JOIN_ROUND));
        g.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), alpha));
        g.draw(s);
    }

    /** Lớp sương trong lòng một hình. Alpha cố tình rất thấp: đây là chỗ hai bản trước làm hỏng. */
    private static void haze(Graphics2D g, Shape s, int alpha) {
        g.setColor(new Color(HAZE.getRed(), HAZE.getGreen(), HAZE.getBlue(), alpha));
        g.fill(s);
    }

    /** Quầng tối rất mềm, không thấy mép. */
    private static void vignette(Graphics2D g, int w, int h, double breathe) {
        double cx = w / 2.0;
        double cy = h * 0.56;
        double rx = w * 0.54;
        AffineTransform squash = AffineTransform.getTranslateInstance(cx, cy);
        squash.scale(1.0, (h * 0.60) / rx);
        squash.translate(-cx, -cy);
        int a = (int) (40 + 14 * breathe);
        g.setPaint(new RadialGradientPaint(
                new Point2D.Double(cx, cy), (float) rx, new Point2D.Double(cx, cy),
                new float[]{0f, 0.5f, 0.82f, 1f},
                new Color[]{vig(a), vig(a * 6 / 10), vig(a * 2 / 10), vig(0)},
                MultipleGradientPaint.CycleMethod.NO_CYCLE,
                MultipleGradientPaint.ColorSpaceType.SRGB, squash));
        g.fill(new Rectangle2D.Double(0, 0, w, h));
    }

    private static Color vig(int a) {
        return new Color((VIGNETTE >> 16) & 0xFF, (VIGNETTE >> 8) & 0xFF, VIGNETTE & 0xFF, a);
    }

    /**
     * Mũ: đường viền mảnh, hai sừng là cung HỞ chứ không phải khối tô.
     *
     * Sừng tô đặc thì thành hai cái que; để hở nét thì đọc ra vệt chakra bốc lên, và đó là cách
     * bản vẽ tay làm.
     */
    private static void head(Graphics2D g, int z, double breathe) {
        Path2D.Double helm = new Path2D.Double();
        helm.moveTo(p(z, 55), p(z, 27));
        helm.curveTo(p(z, 58), p(z, 22), p(z, 66), p(z, 22), p(z, 69), p(z, 27));
        helm.curveTo(p(z, 72), p(z, 36), p(z, 68), p(z, 45), p(z, 62), p(z, 48));
        helm.curveTo(p(z, 56), p(z, 45), p(z, 52), p(z, 36), p(z, 55), p(z, 27));
        helm.closePath();
        haze(g, helm, 30);
        line(g, z, helm, RED, 165, 1.2);

        // Vành mũ
        line(g, z, seg(z, 52, 33, 72, 33), RED, 140, 1.1);

        for (int sgn = -1; sgn <= 1; sgn += 2) {
            Path2D.Double horn = new Path2D.Double();
            horn.moveTo(p(z, 62 + sgn * 6), p(z, 24));
            horn.curveTo(p(z, 62 + sgn * 16), p(z, 16), p(z, 62 + sgn * 17), p(z, 8),
                    p(z, 62 + sgn * 11), p(z, 3));
            line(g, z, horn, CREAM, (int) (150 + 40 * breathe), 1.3);
        }

        // Khe mắt xếch -- chỗ sáng nhất, nhưng vẫn không đặc (alpha 205, không phải 255)
        for (int sgn = -1; sgn <= 1; sgn += 2) {
            Path2D.Double eye = new Path2D.Double();
            eye.moveTo(p(z, 62 + sgn * 3), p(z, 34));
            eye.lineTo(p(z, 62 + sgn * 8), p(z, 32));
            eye.lineTo(p(z, 62 + sgn * 7), p(z, 36));
            eye.closePath();
            g.setColor(new Color(HOT.getRed(), HOT.getGreen(), HOT.getBlue(), 205));
            g.fill(eye);
        }
        // Sống mũi
        line(g, z, seg(z, 62, 36, 62, 44), RED, 120, 1.0);
    }

    /** Thân: viền khiên, sườn kem, sống giữa. Trong lòng chỉ có sương. */
    private static void torso(Graphics2D g, int z, double breathe) {
        Path2D.Double body = new Path2D.Double();
        body.moveTo(p(z, 47), p(z, 50));
        body.lineTo(p(z, 77), p(z, 50));
        body.curveTo(p(z, 81), p(z, 62), p(z, 76), p(z, 78), p(z, 62), p(z, 90));
        body.curveTo(p(z, 48), p(z, 78), p(z, 43), p(z, 62), p(z, 47), p(z, 50));
        body.closePath();
        haze(g, body, 26);
        line(g, z, body, RED, 170, 1.3);

        // Tấm ngực: hai đường chéo, nét riêng của bản này
        line(g, z, seg(z, 52, 54, 62, 60), RED, 110, 1.0);
        line(g, z, seg(z, 72, 54, 62, 60), RED, 110, 1.0);

        // Sườn: cung kem thu dần, mờ dần xuống dưới
        for (int i = 0; i < 5; i++) {
            double f = i / 4.0;
            double y = 62 + f * 22;
            double half = 13 - f * 7;
            Path2D.Double rib = new Path2D.Double();
            rib.moveTo(p(z, 62 - half), p(z, y));
            rib.quadTo(p(z, 62), p(z, y + 3), p(z, 62 + half), p(z, y));
            line(g, z, rib, CREAM, (int) (175 - 22 * i + 20 * breathe), 1.1);
        }
        line(g, z, seg(z, 62, 52, 62, 88), CREAM, (int) (130 + 30 * breathe), 1.0);
    }

    /** Vai và tay: cung mảnh, đầu mút xoè ba ngón. */
    private static void arms(Graphics2D g, int z) {
        for (int sgn = -1; sgn <= 1; sgn += 2) {
            Path2D.Double sh = new Path2D.Double();
            sh.moveTo(p(z, 62 + sgn * 15), p(z, 51));
            sh.curveTo(p(z, 62 + sgn * 26), p(z, 48), p(z, 62 + sgn * 33), p(z, 54),
                    p(z, 62 + sgn * 34), p(z, 64));
            line(g, z, sh, RED, 150, 1.2);

            Path2D.Double low = new Path2D.Double();
            low.moveTo(p(z, 62 + sgn * 15), p(z, 60));
            low.curveTo(p(z, 62 + sgn * 24), p(z, 58), p(z, 62 + sgn * 30), p(z, 60),
                    p(z, 62 + sgn * 32), p(z, 66));
            line(g, z, low, RED, 120, 1.0);

            for (int k = -1; k <= 1; k++) {
                line(g, z, seg(z, 62 + sgn * 33, 64, 62 + sgn * (38 + k * 2), 68 + k * 3),
                        CREAM, 130, 0.9);
            }
        }
    }

    /** Khiên Yata: bát giác viền mảnh, ba vành đồng tâm kem, chấm giữa. */
    private static void mirror(Graphics2D g, int z, double breathe) {
        double cx = 26;
        double cy = 70;
        double r = 15;
        Path2D.Double oct = new Path2D.Double();
        for (int i = 0; i < 8; i++) {
            double a = Math.toRadians(22.5 + i * 45);
            double x = cx + Math.cos(a) * r;
            double y = cy + Math.sin(a) * r;
            if (i == 0) {
                oct.moveTo(p(z, x), p(z, y));
            } else {
                oct.lineTo(p(z, x), p(z, y));
            }
        }
        oct.closePath();
        haze(g, oct, 30);
        line(g, z, oct, RED, 170, 1.3);
        for (int i = 3; i >= 1; i--) {
            double rr = r * (0.22 * i + 0.08);
            line(g, z, new Ellipse2D.Double(p(z, cx - rr), p(z, cy - rr), p(z, rr * 2), p(z, rr * 2)),
                    CREAM, (int) (170 - 20 * i + 20 * breathe), 1.1);
        }
        double d = 2.2;
        g.setColor(new Color(CREAM.getRed(), CREAM.getGreen(), CREAM.getBlue(), 200));
        g.fill(new Ellipse2D.Double(p(z, cx - d), p(z, cy - d), p(z, d * 2), p(z, d * 2)));
    }


    /**
     * Cung chakra: hai cánh cong ngược ở đầu mút, dây kéo căng thành hình chữ V.
     *
     * Đầu mút cong NGƯỢC lại là chi tiết làm nó ra cây cung chiến chứ không ra vành trăng khuyết.
     * Dây vẽ mảnh hơn thân cung và sáng hơn, vì dây căng thì bắt sáng.
     */
    /**
     * @param dim hệ số mờ. Lối vẽ bán trong suốt KHÔNG có phía sau: vẽ trước thân thì thân không
     *            che được, vì thân chỉ là nét viền với lớp sương mỏng. Thứ duy nhất báo hiệu
     *            "ở sau" là mờ hơn, nên tư thế đeo lưng phải hạ hẳn độ đậm xuống.
     */
    private static void bow(Graphics2D g, int z, double breathe, boolean drawn, double dim) {
        // Thân cung vẽ HAI đường song song lệch nhau, không phải một nét đơn. Một nét thì dù dày
        // bao nhiêu vẫn là sợi dây; hai đường có mặt cánh ở giữa mới đọc ra bề dày.
        Path2D.Double limb = new Path2D.Double();
        limb.moveTo(p(z, 24), p(z, 20));
        limb.curveTo(p(z, 11), p(z, 33), p(z, 9), p(z, 63), p(z, 24), p(z, 80));

        Path2D.Double face = new Path2D.Double();
        face.append(limb, false);
        face.lineTo(p(z, 26), p(z, 76));
        face.curveTo(p(z, 14), p(z, 61), p(z, 16), p(z, 35), p(z, 26), p(z, 24));
        face.closePath();
        haze(g, face, (int) (30 * dim));

        line(g, z, limb, RED, (int) (175 * dim), 1.6);
        Path2D.Double inner = new Path2D.Double();
        inner.moveTo(p(z, 26), p(z, 24));
        inner.curveTo(p(z, 16), p(z, 35), p(z, 14), p(z, 61), p(z, 26), p(z, 76));
        line(g, z, inner, RED, (int) (115 * dim), 1.0);

        // Gân ngang trên mặt cánh, thưa dần về hai đầu mút
        for (double t : new double[]{0.24, 0.40, 0.56, 0.72}) {
            double yy = 20 + 60 * t;
            double xa = 25 - 13 * Math.sin(Math.PI * t);
            line(g, z, seg(z, xa, yy, xa + 4, yy - 1), CREAM, (int) (115 * dim), 0.8);
        }
        // Khấc lắp dây
        line(g, z, seg(z, 27, 22, 31, 21), CREAM, (int) (155 * dim), 0.9);
        line(g, z, seg(z, 27, 78, 31, 79), CREAM, (int) (155 * dim), 0.9);

        // Hai đầu mút cong ngược
        Path2D.Double tipTop = new Path2D.Double();
        tipTop.moveTo(p(z, 24), p(z, 20));
        tipTop.quadTo(p(z, 31), p(z, 17), p(z, 30), p(z, 25));
        Path2D.Double tipBot = new Path2D.Double();
        tipBot.moveTo(p(z, 24), p(z, 80));
        tipBot.quadTo(p(z, 31), p(z, 83), p(z, 30), p(z, 75));
        line(g, z, tipTop, RED, (int) (165 * dim), 1.4);
        line(g, z, tipBot, RED, (int) (165 * dim), 1.4);

        // Chỗ nắm, dày hơn
        line(g, z, seg(z, 10, 45, 10, 57), RED, (int) (190 * dim), 2.2);
        // Quấn chuôi: mấy vòng ngang, thứ khiến chỗ nắm đọc ra tay cầm chứ không phải chỗ dày lên
        for (int k = 0; k < 4; k++) {
            line(g, z, seg(z, 8, 46.5 + k * 3, 13, 46.5 + k * 3), CREAM, (int) (145 * dim), 0.8);
        }

        if (drawn) {
            // Dây kéo: đỉnh chữ V chỉ tới x=46, KHÔNG kéo sát thân. Kéo xa hơn thì hai đoạn dây
            // thành hai đường chéo cắt ngang cả người -- đọc ra vết chém chứ không ra cây cung.
            line(g, z, seg(z, 30, 25, 46, 50), CREAM, (int) (165 + 30 * breathe), 1.0);
            line(g, z, seg(z, 30, 75, 46, 54), CREAM, (int) (165 + 30 * breathe), 1.0);
        } else {
            // Đeo sau lưng thì dây CHÙNG, nối thẳng hai đầu mút. Vẽ dây căng hình chữ V trong khi
            // không ai kéo là chi tiết sai mà mắt bắt được ngay dù không gọi tên được.
            line(g, z, seg(z, 30, 25, 30, 75), CREAM, (int) ((140 + 25 * breathe) * dim), 0.9);
        }
    }

    /** Mũi tên đã lắp: thân, đầu nhọn, đuôi lông. Sáng nhất trong nhóm vũ khí. */
    private static void arrow(Graphics2D g, int z, double breathe) {
        int a = (int) (185 + 25 * breathe);
        line(g, z, seg(z, 46, 52, 6, 52), CREAM, a, 1.2);

        Path2D.Double head = new Path2D.Double();
        head.moveTo(p(z, 3), p(z, 52));
        head.lineTo(p(z, 12), p(z, 48));
        head.lineTo(p(z, 12), p(z, 56));
        head.closePath();
        haze(g, head, 40);
        line(g, z, head, CREAM, a, 1.1);

        for (int k = 0; k < 2; k++) {
            int sgn = k == 0 ? -1 : 1;
            line(g, z, seg(z, 44, 52, 38, 52 + sgn * 4), CREAM, 150, 1.0);
            line(g, z, seg(z, 40, 52, 34, 52 + sgn * 4), CREAM, 130, 0.9);
        }
    }

    /**
     * Giáp vai: hai tấm xếp tầng, nét đặc trưng nhất của Susanoo Sasuke.
     *
     * Ba tầng chồng lệch nhau chứ không phải một mảng: chính cái bậc thang đó cho ra khối, mà ở
     * đây không được tô đặc nên khối phải đến từ đường viền xếp lớp. Tấm ngoài to và chếch xuống,
     * tấm trong nhỏ và dựng hơn.
     */
    /**
     * Giáp vai Sasuke: tấm TAM GIÁC chìa ra ngoài, mặt tấm kẻ gân dọc.
     *
     * Ba tấm chữ nhật xếp bậc như bản trước ra cái mái ngói, không ra giáp. Tam giác nhọn hướng
     * ra ngoài mới đúng dáng, và mấy đường gân dọc là thứ khiến nó đọc ra tấm giáp có bề dày chứ
     * không phải một miếng phẳng -- ở đây không được tô đặc nên bề dày phải đến từ vân mặt.
     *
     * Gân vẽ theo tham số chạy dọc từ trong ra mũi: mỗi gân là một đoạn thẳng đứng nối mép trên
     * với mép dưới của tam giác tại đúng vị trí đó, nên càng ra mũi gân càng ngắn và tự tắt.
     */
    private static void pauldrons(Graphics2D g, int z, double breathe) {
        // OUT 33 -> 27: mũi tam giác chìa xa quá thì nó, chứ không phải cây cung, mới là thứ
        // quyết định bề ngang cả hình. Nghiêng cung mà không thu mũi giáp thì đo ra chỉ hẹp được
        // 3 pixel -- công cốc.
        final double IN = 14, OUT = 27;
        final double TOP_IN = 46, TOP_OUT = 57, BOT_IN = 66;

        for (int sgn = -1; sgn <= 1; sgn += 2) {
            Path2D.Double plate = new Path2D.Double();
            plate.moveTo(p(z, 62 + sgn * IN), p(z, TOP_IN));
            plate.lineTo(p(z, 62 + sgn * OUT), p(z, TOP_OUT));
            plate.lineTo(p(z, 62 + sgn * (IN + 1)), p(z, BOT_IN));
            plate.closePath();
            haze(g, plate, 26);
            line(g, z, plate, RED, 180, 1.3);

            for (double t : new double[]{0.16, 0.34, 0.52, 0.70, 0.86}) {
                double x = IN + (OUT - IN) * t;
                double yTop = TOP_IN + (TOP_OUT - TOP_IN) * t;
                double yBot = BOT_IN + (TOP_OUT - BOT_IN) * t;
                line(g, z, seg(z, 62 + sgn * x, yTop + 1, 62 + sgn * x, yBot - 1),
                        CREAM, (int) (120 + 40 * breathe - 40 * t), 0.9);
            }

            // Gai nhọn nối dài từ mũi tam giác
            line(g, z, seg(z, 62 + sgn * OUT, TOP_OUT, 62 + sgn * (OUT + 5), TOP_OUT - 3),
                    CREAM, (int) (150 + 30 * breathe), 1.1);
        }
    }


    /**
     * Ba viên Yasaka Magatama xoay quanh người.
     *
     * Mỗi viên là một dấu phẩy: đầu tròn, đuôi vuốt cong theo chiều xoay. Dựng bằng hai đường
     * xoắn trùng nhau ở mũi và tách ra ở gốc -- cùng cách đã dùng cho lưỡi liềm Mangekyō, và
     * cũng vì cùng một lý do: cho bề dày cố định thì đuôi cụt, nhìn ra cái nòng nọc.
     *
     * Ba viên cách nhau 120 độ và cả cụm quay đúng 120 độ trong trọn vòng khung hình, nên khung
     * cuối trùng khít khung đầu -- viên thứ nhất vừa đúng chỗ viên thứ hai lúc đầu.
     */
    private static void magatama(Graphics2D g, int z, int index, double breathe) {
        double cx = 62, cy = 50;
        double rx = 40, ry = 30;
        double spin = Math.toRadians(120) * index / frameCount;

        for (int i = 0; i < 3; i++) {
            double a = spin + Math.toRadians(-90 + i * 120);
            double px = cx + Math.cos(a) * rx;
            double py = cy + Math.sin(a) * ry;

            // Viên ở nửa sau quỹ đạo thì mờ đi, cho ra cảm giác vòng quay có chiều sâu chứ không
            // phải ba chấm trượt trên mặt phẳng.
            double depth = 0.62 + 0.38 * Math.sin(a);
            // Viên magatama phải ra TÍM, không ra bi xám. Lấy tông tím chính cho thân viên và chỉ
            // chấm một lõi sáng ở giữa; lấy tông nhạt cho cả viên thì trên nền tối nó bạc phếch.
            int alpha = (int) ((185 + 60 * breathe) * depth);

            double head = 5.4 * (0.78 + 0.30 * depth);
            g.setColor(new Color(RED.getRed(), RED.getGreen(), RED.getBlue(), Math.min(255, alpha)));
            g.fill(new Ellipse2D.Double(p(z, px - head), p(z, py - head),
                    p(z, head * 2), p(z, head * 2)));
            g.setColor(new Color(HOT.getRed(), HOT.getGreen(), HOT.getBlue(),
                    Math.min(255, (int) (alpha * 0.85))));
            g.fill(new Ellipse2D.Double(p(z, px - head * 0.38), p(z, py - head * 0.38),
                    p(z, head * 0.76), p(z, head * 0.76)));

            // Đuôi vuốt ngược chiều xoay
            Path2D.Double tail = new Path2D.Double();
            int steps = 12;
            for (int k = 0; k <= steps; k++) {
                double t2 = k / (double) steps;
                double ang = a - Math.toRadians(46) * t2;
                double rr = 1.0 - 0.55 * t2;
                double tx = cx + Math.cos(ang) * rx * (0.99 + 0.05 * t2);
                double ty = cy + Math.sin(ang) * ry * (0.99 + 0.05 * t2);
                double off = head * rr;
                double nx = Math.cos(ang + Math.PI / 2) * off;
                double ny = Math.sin(ang + Math.PI / 2) * off;
                if (k == 0) {
                    tail.moveTo(p(z, tx + nx), p(z, ty + ny));
                } else {
                    tail.lineTo(p(z, tx + nx), p(z, ty + ny));
                }
            }
            for (int k = steps; k >= 0; k--) {
                double t2 = k / (double) steps;
                double ang = a - Math.toRadians(46) * t2;
                double rr = 1.0 - 0.55 * t2;
                double tx = cx + Math.cos(ang) * rx * (0.99 + 0.05 * t2);
                double ty = cy + Math.sin(ang) * ry * (0.99 + 0.05 * t2);
                double off = head * rr;
                tail.lineTo(p(z, tx - Math.cos(ang + Math.PI / 2) * off),
                        p(z, ty - Math.sin(ang + Math.PI / 2) * off));
            }
            tail.closePath();
            g.setColor(new Color(RED.getRed(), RED.getGreen(), RED.getBlue(),
                    Math.min(255, (int) (alpha * 0.80))));
            g.fill(tail);
        }
    }

    /**
     * Katana chakra: lưỡi cong giơ chéo lên phải, chuôi DÀI hạ về giữa cho hai tay cùng nắm.
     *
     * Chuôi phải đủ dài mới chứa nổi hai bàn tay xếp trên dưới. Bản trước chuôi ngắn nên chỉ một
     * tay nắm được, tay kia thừa ra.
     */
    private static void katana(Graphics2D g, int z, double breathe) {
        Path2D.Double edge = new Path2D.Double();
        edge.moveTo(p(z, 84), p(z, 58));
        edge.quadTo(p(z, 102), p(z, 36), p(z, 112), p(z, 10));
        Path2D.Double back = new Path2D.Double();
        back.moveTo(p(z, 79), p(z, 57));
        back.quadTo(p(z, 97), p(z, 35), p(z, 108), p(z, 9));
        line(g, z, back, CREAM, (int) (190 + 20 * breathe), 1.3);
        line(g, z, edge, CREAM, 150, 1.0);
        line(g, z, seg(z, 108, 9, 112, 10), CREAM, 195, 1.3);

        double r = 4.8;
        line(g, z, new Ellipse2D.Double(p(z, 81 - r), p(z, 60 - r), p(z, r * 2), p(z, r * 2)),
                RED, 185, 1.4);

        line(g, z, seg(z, 79, 64, 67, 80), RED, 175, 1.8);
        for (int k = 0; k < 4; k++) {
            double t = 0.15 + k * 0.22;
            double hx = 79 - 12 * t;
            double hy = 64 + 16 * t;
            line(g, z, seg(z, hx - 2, hy - 1, hx + 2, hy + 1), CREAM, 145, 0.8);
        }
        line(g, z, seg(z, 66, 81, 64, 84), RED, 165, 1.4);
    }

    /**
     * Tay cầm katana: tay phải vươn xuống nắm đúng chuôi, tay trái buông.
     *
     * Bản katana trước dùng chung tay giơ ngang của tư thế triệu hồi, nên thanh kiếm lơ lửng cạnh
     * một bàn tay không nắm gì -- sai mà nhìn là thấy ngay. Toạ độ bàn tay ở đây chốt đúng chỗ
     * chuôi mà katana() vẽ, nên sửa vị trí kiếm thì phải sửa cả chỗ này.
     */
    private static void armsKatana(Graphics2D g, int z) {
        // Hai bàn tay xếp trên dưới trên cùng đoạn chuôi: tay phải sát chắn tay, tay trái ở đuôi
        // chuôi. Toạ độ chốt đúng chỗ chuôi mà katana() vẽ -- dời kiếm thì phải dời cả hai chỗ
        // này, không thì lại thành cầm hụt như bản một tay trước đó.
        double[] rh = {76, 68};
        double[] lh = {69, 77};

        Path2D.Double right = new Path2D.Double();
        right.moveTo(p(z, 78), p(z, 54));
        right.curveTo(p(z, 87), p(z, 58), p(z, 84), p(z, 64), p(z, rh[0]), p(z, rh[1]));
        line(g, z, right, RED, 165, 1.4);

        Path2D.Double left = new Path2D.Double();
        left.moveTo(p(z, 46), p(z, 56));
        left.curveTo(p(z, 43), p(z, 70), p(z, 56), p(z, 79), p(z, lh[0]), p(z, lh[1]));
        line(g, z, left, RED, 165, 1.4);

        for (double[] hand : new double[][]{rh, lh}) {
            for (int k = -1; k <= 1; k++) {
                line(g, z, seg(z, hand[0], hand[1], hand[0] + 4 + k, hand[1] + 3 - k * 2),
                        CREAM, 150, 0.9);
            }
        }
    }

    /**
     * Cung đeo sau lưng: mờ hẳn, hạ thấp dưới đầu, thu nhỏ.
     *
     * Mờ là thứ DUY NHẤT báo được "ở phía sau" trong lối vẽ này -- thân chỉ là nét viền với lớp
     * sương mỏng nên không che khuất được gì. Hạ thấp vì đặt cao thì cánh cung cắt ngang mặt,
     * đọc ra vệt chém; chỉ cần hai đầu ló khỏi vai và hông là đủ.
     */
    private static void bowOnBack(Graphics2D g, int z, double breathe) {
        AffineTransform bk = g.getTransform();
        g.translate(p(z, 46), p(z, 18));
        g.rotate(-0.52, p(z, 17), p(z, 50));
        g.scale(0.86, 0.86);
        // 0.72 chứ không 0.55: mờ quá thì mọi chi tiết vừa thêm (sống trong, gân ngang, quấn
        // chuôi, khấc dây) đều tan hết, cung lại về thành một sợi cong. Đây là chỗ đánh đổi --
        // sáng hơn thì cung nổi khối rõ, nhưng cảm giác "ở phía sau" yếu đi.
        bow(g, z, breathe, false, 0.72);
        g.setTransform(bk);
    }

    /** Tay buông xuôi hơi khuỳnh -- tư thế đứng, dùng khi cung đeo sau lưng. */
    private static void armsRest(Graphics2D g, int z) {
        for (int sgn = -1; sgn <= 1; sgn += 2) {
            Path2D.Double arm = new Path2D.Double();
            arm.moveTo(p(z, 62 + sgn * 16), p(z, 56));
            arm.curveTo(p(z, 62 + sgn * 26), p(z, 60), p(z, 62 + sgn * 30), p(z, 68),
                    p(z, 62 + sgn * 29), p(z, 80));
            line(g, z, arm, RED, 160, 1.4);
            for (int k = -1; k <= 1; k++) {
                line(g, z, seg(z, 62 + sgn * 29, 80, 62 + sgn * (27 + k * 3), 87),
                        CREAM, 140, 0.9);
            }
        }
    }

    /** Tay giơ ngang hai bên -- tư thế triệu hồi, dùng cho magatama và katana. */
    private static void armsRaised(Graphics2D g, int z) {
        for (int sgn = -1; sgn <= 1; sgn += 2) {
            Path2D.Double arm = new Path2D.Double();
            arm.moveTo(p(z, 62 + sgn * 16), p(z, 54));
            arm.curveTo(p(z, 62 + sgn * 28), p(z, 56), p(z, 62 + sgn * 34), p(z, 50),
                    p(z, 62 + sgn * 36), p(z, 40));
            line(g, z, arm, RED, 160, 1.4);
            for (int k = -1; k <= 1; k++) {
                line(g, z, seg(z, 62 + sgn * 36, 40, 62 + sgn * (34 + k * 3), 32),
                        CREAM, 140, 0.9);
            }
        }
    }

    /** Xoay một điểm quanh chỗ lắp tên đúng bằng BOW_TILT. */
    private static double[] rot(double x, double y) {
        double dx = x - NOCK_X;
        double dy = y - NOCK_Y;
        double c = Math.cos(BOW_TILT);
        double sn = Math.sin(BOW_TILT);
        return new double[]{NOCK_X + dx * c - dy * sn, NOCK_Y + dx * sn + dy * c};
    }

    /** Tay Sasuke: tay trái duỗi thẳng nắm cung, tay phải co kéo dây. */
    private static void armsSasuke(Graphics2D g, int z) {
        // Tay giương cung: từ vai trái vươn dài sang chỗ nắm
        // Chỗ nắm cung sau khi xoay. Tính lại bằng chính BOW_TILT thay vì gõ tay toạ độ mới:
        // đổi góc nghiêng là tay tự bám theo, không bị hụt ra ngoài cây cung.
        double[] grip = rot(10, 51);
        Path2D.Double bowArm = new Path2D.Double();
        bowArm.moveTo(p(z, 62), p(z, 58));
        bowArm.curveTo(p(z, 48), p(z, 58), p(z, grip[0] + 12), p(z, grip[1] + 8),
                p(z, grip[0] + 3), p(z, grip[1] + 2));
        line(g, z, bowArm, RED, 160, 1.4);
        double[] gA = rot(10, 45);
        double[] gB = rot(10, 57);
        line(g, z, seg(z, gA[0], gA[1], gB[0], gB[1]), RED, 175, 1.6);

        // Tay kéo dây: từ vai phải gập vào chỗ lắp tên
        Path2D.Double drawArm = new Path2D.Double();
        drawArm.moveTo(p(z, 92), p(z, 58));
        drawArm.curveTo(p(z, 82), p(z, 50), p(z, 66), p(z, 48), p(z, 50), p(z, 52));
        line(g, z, drawArm, RED, 160, 1.4);
        for (int k = -1; k <= 1; k++) {
            line(g, z, seg(z, 50, 52, 45, 52 + k * 3), CREAM, 140, 0.9);
        }
    }

    /** Kiếm: lưỡi thẳng có chắn tay, khác lưỡi cong của bản vẽ tay. */
    private static void blade(Graphics2D g, int z, double breathe) {
        Path2D.Double edge = new Path2D.Double();
        edge.moveTo(p(z, 92), p(z, 60));
        edge.lineTo(p(z, 116), p(z, 14));
        Path2D.Double back = new Path2D.Double();
        back.moveTo(p(z, 88), p(z, 58));
        back.lineTo(p(z, 112), p(z, 12));
        Path2D.Double tip = new Path2D.Double();
        tip.moveTo(p(z, 112), p(z, 12));
        tip.lineTo(p(z, 116), p(z, 14));

        haze(g, poly(z, 88, 58, 112, 12, 116, 14, 92, 60), 34);
        line(g, z, back, CREAM, (int) (185 + 20 * breathe), 1.2);
        line(g, z, edge, CREAM, 150, 1.0);
        line(g, z, tip, CREAM, 190, 1.2);
        // Chắn tay và chuôi
        line(g, z, seg(z, 84, 64, 96, 56), RED, 165, 1.4);
        line(g, z, seg(z, 84, 66, 90, 70), RED, 145, 1.2);
    }

    /**
     * Tua chakra: sợi CONG XOẮN bốc quanh thân.
     *
     * Bản trước tôi vẽ gạch thẳng một pixel và nó đọc ra hạt bụi. Sợi cong mới ra vẻ lửa liếm --
     * đây là chi tiết khiến bản vẽ tay của user sống động hơn hẳn.
     */
    private static void wisps(Graphics2D g, int z, int index, double breathe) {
        long[] seed = {6151L * index + 97L};
        for (int i = 0; i < 7; i++) {
            int sgn = i % 2 == 0 ? -1 : 1;
            double x = 62 + sgn * (20 + rnd(seed) * 22);
            double y = 46 + rnd(seed) * 40 - (index % 4) * 2;
            double len = 6 + rnd(seed) * 7;
            double curl = (rnd(seed) - 0.5) * 10;
            Path2D.Double s = new Path2D.Double();
            s.moveTo(p(z, x), p(z, y));
            s.curveTo(p(z, x + curl), p(z, y - len * 0.4),
                    p(z, x - curl), p(z, y - len * 0.75),
                    p(z, x + curl * 0.6), p(z, y - len));
            line(g, z, s, CREAM, (int) (95 + 45 * breathe), 1.0);
        }
    }

    /** Chấm và dấu cộng lấp lánh, thưa thớt. */
    private static void sparks(Graphics2D g, int z, int index) {
        long[] seed = {2749L * index + 13L};
        for (int i = 0; i < 5; i++) {
            double x = 20 + rnd(seed) * 86;
            double y = 30 + rnd(seed) * 54;
            boolean cross = rnd(seed) > 0.55;
            int a = 120 + (int) (rnd(seed) * 70);
            if (cross) {
                line(g, z, seg(z, x - 2, y, x + 2, y), CREAM, a, 0.9);
                line(g, z, seg(z, x, y - 2, x, y + 2), CREAM, a, 0.9);
            } else {
                double d = 1.2;
                g.setColor(new Color(CREAM.getRed(), CREAM.getGreen(), CREAM.getBlue(), a));
                g.fill(new Ellipse2D.Double(p(z, x - d), p(z, y - d), p(z, d * 2), p(z, d * 2)));
            }
        }
    }

    private static double p(int z, double v) {
        return v * z;
    }

    private static Shape seg(int z, double x1, double y1, double x2, double y2) {
        Path2D.Double s = new Path2D.Double();
        s.moveTo(p(z, x1), p(z, y1));
        s.lineTo(p(z, x2), p(z, y2));
        return s;
    }

    private static Shape poly(int z, double... pts) {
        Path2D.Double s = new Path2D.Double();
        s.moveTo(p(z, pts[0]), p(z, pts[1]));
        for (int i = 2; i + 1 < pts.length; i += 2) {
            s.lineTo(p(z, pts[i]), p(z, pts[i + 1]));
        }
        s.closePath();
        return s;
    }

    private static double rnd(long[] seed) {
        seed[0] = seed[0] * 6364136223846793005L + 1442695040888963407L;
        return ((seed[0] >>> 33) % 10000) / 10000.0;
    }

    private static String sprites() {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < frameCount; i++) {
            sb.append(i > 0 ? "," : "")
              .append("{\"id\":").append(i)
              .append(",\"x\":").append((i % COLS) * W)
              .append(",\"y\":").append((i / COLS) * H)
              .append(",\"w\":").append(W).append(",\"h\":").append(H).append('}');
        }
        return sb.append(']').toString();
    }

    private static String framesJson(int bottomY) {
        int dx = -W / 2;
        int dy = bottomY - H;
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < frameCount; i++) {
            sb.append(i > 0 ? "," : "")
              .append("[{\"id\":").append(i).append(",\"dx\":").append(dx)
              .append(",\"dy\":").append(dy).append(",\"onTop\":0,\"flip\":0}]");
        }
        return sb.append(']').toString();
    }

    private static String running() {
        StringBuilder sb = new StringBuilder();
        int hold = 3;
        for (int i = 0; i < frameCount; i++) {
            for (int k = 0; k < hold; k++) {
                sb.append(i == 0 && k == 0 ? "" : ",").append(i);
            }
        }
        if (frameCount * hold > SEQ_MAX) {
            throw new IllegalStateException("chuỗi phát vượt " + SEQ_MAX);
        }
        return "[" + sb + "]";
    }
}
