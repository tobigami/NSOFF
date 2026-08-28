import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.LinearGradientPaint;
import java.awt.MultipleGradientPaint;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.font.GlyphVector;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

/**
 * Vẽ lại băng-rôn danh hiệu thành một dòng chữ trơn, hiệu ứng duy nhất là vệt sáng chạy ngang.
 *
 * Bản gốc của "Đệ Nhất Ninja" (hiệu ứng 201) là tấm 124x41 ở mức thu phóng 1, năm khung rồng lửa
 * nhấp nháy -- rộng gấp ba lần nhân vật. Bản này bỏ hết phần trang trí, chỉ còn chữ đổ màu,
 * viền tối một pixel, và một vệt sáng quét từ trái sang phải rồi nghỉ.
 *
 * HAI RÀNG BUỘC CHI PHỐI MỌI KÍCH THƯỚC Ở ĐÂY
 *
 * 1. Toạ độ trong bảng effect_data tính theo mức thu phóng 1, còn ảnh thì mỗi mức một tấm
 *    (Data/Img/Effect/1..4). Tấm mức z phải rộng và cao đúng gấp z lần tấm mức 1, không được lệch
 *    một pixel -- lệch là chữ bị cắt ở đúng mức đó mà các mức khác vẫn bình thường. Nên khung
 *    được chốt ở mức 1 rồi nhân lên, không đo lại chữ ở từng mức.
 *
 * 2. EffectData.setData() ghi x, y, w, h của mỗi khung bằng **một byte**, client đọc lại không
 *    dấu. Nghĩa là ở mức thu phóng 1: mỗi khung rộng tối đa 255, và cả tấm ảnh xếp chồng cũng chỉ
 *    cao tối đa 255. Đó là lý do bản gốc rộng đúng 124 chứ không rộng hơn. Chương trình tự kiểm
 *    và báo lỗi thay vì đẻ ra tấm ảnh mà client đọc sai.
 *
 *   java MakeTitleEffect <chữ> <thư mục ra> [kiểu] [font] [caoChữHoa] [idHiệuỨng] [mépDưới]
 *
 *   kiểu: do (mặc định) | vang | trang | lam | hokage | chop | thuy
 *
 * In ra ba cột JSON của effect_data để dán thẳng vào CSDL.
 */
public final class MakeTitleEffect {

    /**
     * Chiều cao chữ hoa ở mức thu phóng 1. Cỡ chữ suy ra từ đây chứ không đặt thẳng, vì mỗi font
     * quy đổi cỡ sang chiều cao thật một kiểu.
     */
    private static final int CAP_HEIGHT_Z1 = 7;

    /** Chỗ chừa quanh chữ ở mức 1, đủ cho viền một pixel và cái bóng của nó. */
    private static final int PAD_Z1 = 2;

    /** Số khung của vệt sáng. Khung 0 là chữ trơn, các khung sau là vệt ở từng vị trí. */
    private static final int FRAMES = 8;

    /** Mép dưới băng-rôn, tính từ chân nhân vật. Mặc định là chỗ mép dưới của hiệu ứng 201 gốc. */
    private static final int BOTTOM_Y = -59;

    /**
     * Kiểu "chop" thay vệt sáng quét ngang bằng tia chớp loé: khung 0 là chữ đứng yên, các khung
     * sau mỗi khung một tia khác nhau. Hình tia sinh từ một bộ sinh số giả ngẫu nhiên GIEO THEO
     * CHỈ SỐ KHUNG, nên chạy lại công cụ bao nhiêu lần cũng ra đúng bộ ảnh cũ -- dùng Math.random
     * thì mỗi lần dựng lại là một bộ khác, không đối chiếu được bằng cmp nữa.
     */
    private static boolean lightning = false;

    /**
     * Kiểu "thuy" chảy LIÊN TỤC chứ không loé: mọi khung đều có gợn sóng và bọt khí, chỉ khác pha.
     * Khác hẳn tia chớp -- chớp thì phải thưa và giật, nước thì phải đều và không có khung nào
     * đứng yên, có một khung trơ ra là mắt bắt được ngay chỗ vòng lặp nối lại.
     */
    private static boolean water = false;

    private static int effectId = 201;
    private static int bottomY = BOTTOM_Y;

    /** Trần của x, y, w, h trong effect_data vì chúng đi qua một byte không dấu. */
    private static final int BYTE_MAX = 255;

    private MakeTitleEffect() {
    }

    private static final class Style {
        final Color top;
        final Color bottom;
        final Color outline;

        Style(int top, int bottom, int outline) {
            this.top = new Color(top);
            this.bottom = new Color(bottom);
            this.outline = new Color(outline);
        }
    }

    private static Style style(String name) {
        switch (name) {
            case "do":    return new Style(0xFF6A5A, 0xC81018, 0x2B0000);
            case "vang":  return new Style(0xFFE9A3, 0xE8921C, 0x3A2200);
            case "trang": return new Style(0xFFFFFF, 0xC9D2DE, 0x1B1B1B);
            case "lam":   return new Style(0xBFE9FF, 0x2E86D6, 0x06203A);
            // Áo choàng Hokage Đệ Tứ: nền trắng ngà, lửa đỏ viền gấu áo. Chữ lấy trắng ngà, viền
            // đỏ lửa -- lấy đúng chữ đen như trên áo thì trên map tối không đọc nổi.
            case "hokage": return new Style(0xFFFFFF, 0xEFE4D2, 0xC1121F);
            // Tia Chớp Vàng của Làng Lá: chữ vàng chói, viền nâu sẫm cho nổi trên mọi nền.
            case "chop":   return new Style(0xFFF6B0, 0xF0A800, 0x3A2600);
            // Hệ Thuỷ: xanh nước biển, sáng ở đỉnh chữ như mặt nước bắt sáng, sẫm dần xuống đáy.
            case "thuy":   return new Style(0xC4F0FF, 0x1477C8, 0x04202F);
            default: throw new IllegalArgumentException("kiểu lạ: " + name);
        }
    }

    /**
     * Chính client dùng Tahoma cho mọi chữ trong game (font/tahoma_7, tahoma_7b, tahoma_8b nằm
     * trong jar), nên băng-rôn dùng cùng font thì nhìn như một phần của giao diện chứ không như
     * ảnh dán thêm. Đổi được bằng tham số thứ tư khi cần thử font khác.
     */
    private static String font = "Tahoma-Bold";

    /** Đặt qua tham số thứ năm; mặc định lấy CAP_HEIGHT_Z1. */
    private static int capHeight = CAP_HEIGHT_Z1;

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.out.println("dùng: java MakeTitleEffect <chữ> <thư mục ra>"
                    + " [do|vang|trang|lam|hokage|chop|thuy] [font] [caoChữHoa]"
                    + " [idHiệuỨng] [mépDưới]");
            System.exit(2);
        }
        String text = args[0];
        File outDir = new File(args[1]);
        Style st = style(args.length > 2 ? args[2] : "do");
        lightning = args.length > 2 && args[2].equals("chop");
        water = args.length > 2 && args[2].equals("thuy");
        if (args.length > 3) {
            font = args[3];
        }
        if (args.length > 4) {
            capHeight = Integer.parseInt(args[4]);
        }
        if (args.length > 5) {
            effectId = Integer.parseInt(args[5]);
        }
        if (args.length > 6) {
            bottomY = Integer.parseInt(args[6]);
        }

        int[] box = measure(text);
        int w1 = box[0];
        int h1 = box[1];

        if (w1 > BYTE_MAX) {
            System.out.println("!! khung rộng " + w1 + " > " + BYTE_MAX
                    + " ở mức 1: client đọc w bằng một byte nên sẽ cắt cụt. Hạ chiều cao chữ"
                    + " hoặc rút ngắn chữ.");
            System.exit(1);
        }
        if (h1 * FRAMES > BYTE_MAX) {
            System.out.println("!! tấm ảnh cao " + (h1 * FRAMES) + " > " + BYTE_MAX
                    + " ở mức 1: khung cuối có y vượt một byte. Giảm FRAMES hoặc hạ chiều cao chữ.");
            System.exit(1);
        }

        for (int z = 1; z <= 4; z++) {
            BufferedImage sheet = new BufferedImage(w1 * z, h1 * FRAMES * z, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = sheet.createGraphics();
            for (int i = 0; i < FRAMES; i++) {
                BufferedImage frame = draw(st, text, z, w1 * z, h1 * z, i);
                g.drawImage(frame, 0, i * h1 * z, null);
            }
            g.dispose();
            File dir = new File(outDir, String.valueOf(z));
            dir.mkdirs();
            ImageIO.write(sheet, "png", new File(dir, effectId + ".png"));
        }

        System.out.println("kích thước mức 1: khung " + w1 + "x" + h1
                + ", tấm " + w1 + "x" + (h1 * FRAMES) + " (" + FRAMES + " khung)");
        System.out.println();
        System.out.println("sprites = " + sprites(w1, h1));
        System.out.println("frames  = " + frames(w1, h1));
        System.out.println("running = " + running());
    }

    /** Khung chữ ở mức thu phóng 1, đã cộng chỗ chừa. Đo bằng visual bounds nên dấu và đuôi chữ g đều vào khung. */
    private static int[] measure(String text) {
        BufferedImage probe = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = probe.createGraphics();
        Rectangle2D r = fontFor(g, 1).createGlyphVector(g.getFontRenderContext(), text).getVisualBounds();
        g.dispose();
        return new int[]{
                (int) Math.ceil(r.getWidth()) + 2 * PAD_Z1,
                (int) Math.ceil(r.getHeight()) + 2 * PAD_Z1
        };
    }

    /** Cỡ chữ sao cho chiều cao chữ hoa đúng bằng capHeight * zoom, đo bằng chính glyph chứ không theo metric của font. */
    private static Font fontFor(Graphics2D g, int zoom) {
        int target = capHeight * zoom;
        Font f = new Font(font, Font.BOLD, target);
        for (int i = 0; i < 12; i++) {
            double capNow = f.createGlyphVector(g.getFontRenderContext(), "H").getVisualBounds().getHeight();
            if (capNow <= 0) {
                break;
            }
            double scale = target / capNow;
            if (Math.abs(scale - 1.0) < 0.005) {
                break;
            }
            f = f.deriveFont((float) (f.getSize2D() * scale));
        }
        return f;
    }

    private static BufferedImage draw(Style st, String text, int zoom, int w, int h, int frame) {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

        GlyphVector gv = fontFor(g, zoom).createGlyphVector(g.getFontRenderContext(), text);
        Rectangle2D vb = gv.getVisualBounds();
        Shape shape = AffineTransform
                .getTranslateInstance(PAD_Z1 * zoom - vb.getX(), PAD_Z1 * zoom - vb.getY())
                .createTransformedShape(gv.getOutline());
        Rectangle2D sb = shape.getBounds2D();

        // Viền đắp bốn hướng, không đắp chéo: nét stroke ở cỡ nhỏ bị hụt góc, còn đắp cả tám
        // hướng thì hai tầng dấu của chữ ẫ dính vào nhau thành một cục.
        g.setComposite(AlphaComposite.Src);
        g.setColor(st.outline);
        for (int[] d : new int[][]{{-zoom, 0}, {zoom, 0}, {0, -zoom}, {0, zoom}}) {
            g.fill(AffineTransform.getTranslateInstance(d[0], d[1]).createTransformedShape(shape));
        }

        g.setComposite(AlphaComposite.SrcOver);
        g.setPaint(new GradientPaint(
                0f, (float) sb.getMinY(), st.top,
                0f, (float) sb.getMaxY(), st.bottom));
        g.fill(shape);

        if (water) {
            water(g, w, h, zoom, frame, shape);
        } else if (frame > 0) {
            if (lightning) {
                bolts(g, w, h, zoom, frame, shape);
            } else {
                shine(g, shape, sb, frame);
            }
        }

        g.dispose();
        return img;
    }

    /**
     * Vệt sáng: một dải trắng mờ dần hai bên, cắt theo đúng hình chữ nên chỉ chữ sáng lên, nền
     * vẫn trong suốt. Tâm dải chạy từ ngoài mép trái sang ngoài mép phải theo số khung.
     */
    private static void shine(Graphics2D g, Shape shape, Rectangle2D sb, int frame) {
        double span = sb.getWidth();
        double band = span * 0.30;
        double center = -band + (span + 2 * band) * frame / (double) (FRAMES - 1);
        float x0 = (float) (sb.getMinX() + center - band);
        float x1 = (float) (sb.getMinX() + center + band);
        if (x1 - x0 < 1f) {
            return;
        }
        g.setPaint(new LinearGradientPaint(
                new Point2D.Float(x0, 0f), new Point2D.Float(x1, 0f),
                new float[]{0f, 0.5f, 1f},
                new Color[]{new Color(255, 255, 255, 0), new Color(255, 255, 255, 190),
                        new Color(255, 255, 255, 0)},
                MultipleGradientPaint.CycleMethod.NO_CYCLE));
        Shape old = g.getClip();
        g.clip(shape);
        g.fill(sb);
        g.setClip(old);
    }




    /**
     * Hiệu ứng hệ thuỷ: gợn sóng chạy trên mặt chữ, cộng bọt khí nổi lên.
     *
     * Gợn sóng là hai đường hình sin CẮT THEO HÌNH CHỮ, nên nó chỉ sáng trên nét chữ chứ không
     * vắt ngang nền -- đúng cảm giác ánh sáng khúc xạ qua nước rọi lên vật, thay vì một sợi dây
     * vắt qua. Hai đường khác tần số và ngược chiều chạy: cùng tần số thì thành một vạch dày đều,
     * mất hẳn vẻ lăn tăn.
     *
     * Bọt khí thì KHÔNG cắt theo chữ -- chúng nổi trước mặt chữ. Mỗi bọt đi lên đều theo số khung
     * và quay vòng bằng phép chia dư, nên khung cuối nối liền khung đầu, không thấy chỗ giật.
     */
    private static void water(Graphics2D g, int w, int h, int zoom, int frame, Shape text) {
        double phase = 2 * Math.PI * frame / (double) FRAMES;

        Shape clip = g.getClip();
        g.clip(text);
        for (int line = 0; line < 2; line++) {
            double freq = line == 0 ? 1.6 : 2.7;
            double dir = line == 0 ? 1 : -1;
            double amp = h * (line == 0 ? 0.20 : 0.13);
            double mid = h * (line == 0 ? 0.42 : 0.62);
            Path2D.Double wave = new Path2D.Double();
            int segs = 40;
            for (int i = 0; i <= segs; i++) {
                double x = w * i / (double) segs;
                double y = mid + amp * Math.sin(freq * 2 * Math.PI * i / segs + dir * phase);
                if (i == 0) {
                    wave.moveTo(x, y);
                } else {
                    wave.lineTo(x, y);
                }
            }
            g.setStroke(new BasicStroke(Math.max(1f, zoom * (line == 0 ? 2.0f : 1.2f)),
                    BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.setColor(new Color(228, 250, 255, line == 0 ? 105 : 70));
            g.draw(wave);
        }
        g.setClip(clip);

        long[] seed = {5099L};
        for (int b = 0; b < 4; b++) {
            double bx = w * (0.10 + 0.80 * rnd(seed));
            double speed = 0.7 + 0.6 * rnd(seed);
            double t = ((frame * speed / FRAMES) + b * 0.25) % 1.0;
            double by = h * (0.92 - 0.84 * t);
            double r = Math.max(1, h * (0.055 + 0.03 * rnd(seed)));
            int alpha = (int) (200 * Math.sin(Math.PI * t));   // mờ ở hai đầu hành trình
            g.setStroke(new BasicStroke(Math.max(1f, zoom * 0.7f)));
            g.setColor(new Color(214, 246, 255, Math.max(0, alpha)));
            g.draw(new java.awt.geom.Ellipse2D.Double(bx - r, by - r, r * 2, r * 2));
        }
    }

    /**
     * Tia chớp loé quanh dòng chữ: mấy tia NGẮN gãy khúc gắt, cộng vài đốm lửa nhỏ.
     *
     * Bản đầu tôi vẽ hai tia dài vắt hết bề ngang, nét thoải -- ra hai vệt gạch ngang chữ, nhìn
     * như bị gạch bỏ chứ không như phóng điện. Ba thứ chữa được:
     *   - tia NGẮN, chỉ chiếm chừng một phần ba bề ngang, đặt rải rác chứ không vắt suốt;
     *   - đỉnh gãy ĐỔI CHIỀU luân phiên và biên độ lớn, thay vì lệch ngẫu nhiên quanh một đường
     *     ngang -- lệch ngẫu nhiên thì trung bình vẫn là đường thẳng;
     *   - thêm vài đốm sáng rời, thứ khiến mắt đọc ra "tia lửa điện" chứ không "nét vẽ".
     *
     * Mọi con số ngẫu nhiên gieo theo CHỈ SỐ KHUNG nên chạy lại công cụ ra đúng bộ ảnh cũ.
     */
    private static void bolts(Graphics2D g, int w, int h, int zoom, int frame, Shape text) {
        long[] seed = {8191L * frame + 104729L};

        int nBolt = 2 + (frame % 2);
        for (int b = 0; b < nBolt; b++) {
            double len = w * (0.22 + 0.16 * rnd(seed));
            double x0 = (w - len) * rnd(seed);
            double base = h * (b % 2 == 0 ? 0.30 : 0.72);
            int segs = 4;

            Path2D.Double path = new Path2D.Double();
            for (int i = 0; i <= segs; i++) {
                double x = x0 + len * i / (double) segs;
                // Đổi chiều luân phiên: đây mới là thứ tạo hình răng cưa của tia điện.
                double swing = (i % 2 == 0 ? -1 : 1) * h * (0.14 + 0.16 * rnd(seed));
                double y = base + (i == 0 || i == segs ? 0 : swing);
                if (i == 0) {
                    path.moveTo(x, y);
                } else {
                    path.lineTo(x, y);
                }
            }
            g.setStroke(new BasicStroke(Math.max(1f, zoom * 1.4f), BasicStroke.CAP_ROUND,
                    BasicStroke.JOIN_MITER));
            g.setColor(new Color(255, 206, 80, 85));
            g.draw(path);
            g.setStroke(new BasicStroke(Math.max(1f, zoom * 0.6f), BasicStroke.CAP_ROUND,
                    BasicStroke.JOIN_MITER));
            g.setColor(new Color(255, 255, 228, 240));
            g.draw(path);
        }

        int nSpark = 3;
        g.setColor(new Color(255, 250, 215, 235));
        for (int i = 0; i < nSpark; i++) {
            double sx = w * rnd(seed);
            double sy = h * (0.18 + 0.64 * rnd(seed));
            double r = Math.max(0.7, zoom * 0.55);
            g.fill(new java.awt.geom.Ellipse2D.Double(sx - r, sy - r, r * 2, r * 2));
        }

        // Cả băng-rôn cùng loé một nhịp: chỉ có tia chạy qua thì như dây vắt ngang, phải có nhịp
        // sáng bùng lên mới ra cảm giác phóng điện. Cắt theo hình chữ nên nền vẫn trong suốt.
        if (frame == 2 || frame == 5) {
            Shape clip = g.getClip();
            g.clip(text);
            g.setColor(new Color(255, 255, 255, 110));
            g.fill(new Rectangle2D.Double(0, 0, w, h));
            g.setClip(clip);
        }
    }

    /** Số giả ngẫu nhiên 0..1, gieo tuần tự từ hạt truyền vào. */
    private static double rnd(long[] seed) {
        seed[0] = seed[0] * 6364136223846793005L + 1442695040888963407L;
        return ((seed[0] >>> 33) % 100000) / 100000.0;
    }

    private static String sprites(int w1, int h1) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < FRAMES; i++) {
            sb.append(i > 0 ? "," : "")
              .append("{\"w\":").append(w1).append(",\"x\":0,\"h\":").append(h1)
              .append(",\"y\":").append(i * h1).append(",\"id\":").append(i).append('}');
        }
        return sb.append(']').toString();
    }

    private static String frames(int w1, int h1) {
        int dx = -w1 / 2;
        int dy = bottomY - h1;
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < FRAMES; i++) {
            sb.append(i > 0 ? "," : "")
              .append("[{\"id\":").append(i).append(",\"dx\":").append(dx)
              .append(",\"dy\":").append(dy).append(",\"onTop\":0,\"flip\":0}]");
        }
        return sb.append(']').toString();
    }

    /**
     * Nghỉ lâu rồi quét một nhát: chữ đứng yên phần lớn thời gian, thỉnh thoảng loé qua một cái.
     * Quét mỗi khung giữ hai nhịp cho đỡ giật. Độ dài 34 nhịp, xấp xỉ bản gốc.
     */
    private static String running() {
        StringBuilder sb = new StringBuilder("[");
        if (water) {
            // Nước chảy đều, không nghỉ: quét hết lượt khung rồi quay lại đầu, mỗi khung giữ bốn
            // nhịp cho sóng lăn tăn chậm. Chèn quãng đứng yên như tia chớp là hỏng cả hiệu ứng.
            for (int i = 0; i < FRAMES; i++) {
                for (int k = 0; k < 4; k++) {
                    sb.append(i == 0 && k == 0 ? "" : ",").append(i);
                }
            }
            return sb.append(']').toString();
        }
        if (lightning) {
            // Chớp thì phải thưa và nhanh: đứng yên thật lâu rồi giật một tràng, mỗi tia chỉ
            // một nhịp. Giữ tia hai nhịp như vệt sáng là nó thành đèn nhấp nháy.
            for (int i = 0; i < 46; i++) {
                sb.append(i > 0 ? "," : "").append(0);
            }
            for (int i = 1; i < FRAMES; i++) {
                sb.append(',').append(i);
            }
            for (int i = 0; i < 6; i++) {
                sb.append(",0");
            }
            for (int i = FRAMES - 1; i >= 1; i--) {
                sb.append(',').append(i);
            }
            return sb.append(']').toString();
        }
        for (int i = 0; i < 20; i++) {
            sb.append(i > 0 ? "," : "").append(0);
        }
        for (int i = 1; i < FRAMES; i++) {
            sb.append(',').append(i).append(',').append(i);
        }
        return sb.append(']').toString();
    }
}
