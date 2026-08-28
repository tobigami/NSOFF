import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.MultipleGradientPaint;
import java.awt.RadialGradientPaint;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

/**
 * Vẽ hiệu ứng danh hiệu Akatsuki: BA đám mây đỏ nhỏ trôi ngang trên một dải nền đen.
 *
 * Mây dựng bằng phép HỢP của mấy hình tròn chồng nhau chứ không vẽ đường viền tay: đường viền tay
 * ở cỡ vài chục pixel thì run và không đối xứng, còn hợp hình tròn thì mép nào cũng là cung tròn
 * trơn, thu nhỏ bao nhiêu vẫn giữ dáng. Cái đuôi xoắn bên phải thì phải vẽ riêng vì nó không phải
 * hình tròn -- đó là chi tiết duy nhất khiến người ta nhận ra mây Akatsuki chứ không phải mây bất kỳ.
 *
 * BA RÀNG BUỘC GIỐNG MỌI HIỆU ỨNG KHÁC (tasks/todo.md có ghi đủ)
 *
 * 1. Toạ độ trong effect_data chỉ có MỘT bộ, tính theo mức thu phóng 1; ảnh thì mỗi mức một tấm.
 *    Tấm mức z phải to đúng gấp z lần tấm mức 1.
 * 2. x, y, w, h mỗi khung đi qua một byte không dấu: ở mức 1, khung rộng tối đa 255 và cả tấm
 *    xếp chồng cao tối đa 255.
 * 3. Độ dài chuỗi phát tối đa 126 -- trần này đến từ con trỏ chạy chuỗi bên client là một byte,
 *    KHÔNG phải từ khâu truyền. Dài hơn là hiệu ứng lặng lẽ đứng hình.
 *
 *   java MakeAkatsukiEffect <thư mục ra> <idHiệuỨng> <mépDưới> [rộngMây] [sốKhung]
 *
 * In ra ba cột JSON của effect_data để dán thẳng vào CSDL.
 */
public final class MakeAkatsukiEffect {

    /** Bề ngang MỘT đám mây ở mức thu phóng 1. */
    private static final int CLOUD_W_Z1 = 22;

    /** Số mây trôi trên dải. Ba cái, cách đều nhau đúng một phần ba bề ngang khung. */
    private static final int CLOUDS = 3;

    /** Bề ngang cả dải nền, ở mức thu phóng 1. */
    private static final int BAND_W_Z1 = 90;

    /** Mây cao bằng 0,62 bề ngang -- tỉ lệ của biểu tượng gốc. */
    private static final double CLOUD_RATIO = 0.62;

    /** Chỗ chừa quanh mây, cho viền trắng và quầng đỏ. */
    private static final int PAD_X_Z1 = 9;
    private static final int PAD_Y_Z1 = 6;

    private static final int FRAMES = 8;

    private static final int BYTE_MAX = 255;
    private static final int SEQ_MAX = 126;

    private static final Color CLOUD = new Color(0xC0182A);
    private static final Color CLOUD_DEEP = new Color(0x7B0A18);
    private static final Color EDGE = new Color(0xF7F2EA);
    private static final Color GLOW = new Color(0xE8324A);
    private static final Color DARK = new Color(0x0A0508);

    /** Nền đen: đậm hẳn để ra "nền", nhưng vẫn tắt dần ở rìa cho khỏi thành miếng dán chữ nhật. */
    private static final int DARK_ALPHA = 215;
    private static final int GLOW_ALPHA = 46;

    private static int cloudW = CLOUD_W_Z1;
    private static int frameCount = FRAMES;

    private MakeAkatsukiEffect() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 3) {
            System.out.println("dùng: java MakeAkatsukiEffect <thư mục ra> <idHiệuỨng> <mépDưới>"
                    + " [rộngMây] [sốKhung]");
            System.exit(2);
        }
        File outDir = new File(args[0]);
        int effectId = Integer.parseInt(args[1]);
        int bottomY = Integer.parseInt(args[2]);
        if (args.length > 3) {
            cloudW = Integer.parseInt(args[3]);
        }
        if (args.length > 4) {
            frameCount = Integer.parseInt(args[4]);
        }

        int cloudH = (int) Math.round(cloudW * CLOUD_RATIO);
        int w1 = BAND_W_Z1;
        int h1 = cloudH + 2 * PAD_Y_Z1;

        if (w1 > BYTE_MAX) {
            System.out.println("!! khung rộng " + w1 + " > " + BYTE_MAX + ": hạ BAND_W_Z1.");
            System.exit(1);
        }
        if (h1 * frameCount > BYTE_MAX) {
            System.out.println("!! tấm cao " + (h1 * frameCount) + " > " + BYTE_MAX
                    + ": hạ rộngMây hoặc giảm sốKhung. Chừa dọc tốn gấp " + frameCount + " lần.");
            System.exit(1);
        }

        for (int z = 1; z <= 4; z++) {
            BufferedImage sheet = new BufferedImage(w1 * z, h1 * frameCount * z,
                    BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = sheet.createGraphics();
            for (int i = 0; i < frameCount; i++) {
                g.drawImage(frame(z, w1 * z, h1 * z, i), 0, i * h1 * z, null);
            }
            g.dispose();
            File dir = new File(outDir, String.valueOf(z));
            dir.mkdirs();
            ImageIO.write(sheet, "png", new File(dir, effectId + ".png"));
        }

        System.out.println("kích thước mức 1: khung " + w1 + "x" + h1
                + ", tấm " + w1 + "x" + (h1 * frameCount) + " (" + frameCount + " khung)");
        System.out.println();
        System.out.println("sprites = " + sprites(w1, h1));
        System.out.println("frames  = " + frames(w1, h1, bottomY));
        System.out.println("running = " + running());
    }

    private static BufferedImage frame(int zoom, int w, int h, int index) {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

        double t = index / (double) frameCount;
        backdrop(g, w, h, 0.5 + 0.5 * Math.cos(2 * Math.PI * t));

        double cw = cloudW * zoom;
        double ch = cw * CLOUD_RATIO;
        double step = w / (double) CLOUDS;

        // Trôi đúng MỘT khoảng cách giữa hai mây trong trọn vòng khung hình. Nhờ vậy khung cuối
        // trùng khít khung đầu: mây thứ nhất vừa đúng chỗ mây thứ hai lúc đầu. Cho trôi hết cả
        // bề ngang thì vòng lặp vẫn khớp nhưng mây phóng nhanh gấp ba, hết vẻ lững lờ.
        double drift = step * t;

        for (int i = 0; i < CLOUDS; i++) {
            // Vẽ thêm một bản lệch sang trái đúng một bề ngang khung: mây nào vừa trôi khỏi mép
            // phải thì bản kia đã vào từ mép trái, nên chỗ nối không hụt một khoảng trống.
            for (int wrap = -1; wrap <= 0; wrap++) {
                double cx = i * step + drift + wrap * w;
                if (cx + cw < 0 || cx > w) {
                    continue;
                }
                double bob = Math.sin(2 * Math.PI * (t + i / (double) CLOUDS)) * h * 0.07;
                drawCloud(g, cx, h / 2.0 - ch / 2 + bob, cw, ch, zoom, edgeFade(cx + cw / 2, w));
            }
        }

        g.dispose();
        return img;
    }

    /**
     * Độ mờ theo vị trí ngang: đặc ở giữa dải, tắt dần về hai mép.
     *
     * Không có nó thì mây hiện ra và biến mất đột ngột ngay tại mép khung, mắt bắt được ngay vì
     * cái mép ấy là một đường thẳng đứng -- trong khi dải nền thì bo tròn, nên chỗ vênh lộ rõ.
     */
    private static double edgeFade(double cx, double w) {
        double d = Math.min(cx, w - cx) / (w * 0.28);
        return Math.max(0, Math.min(1, d));
    }

    private static void drawCloud(Graphics2D g, double x, double y, double cw, double ch,
            int zoom, double alpha) {
        if (alpha <= 0.01) {
            return;
        }
        Shape cloud = cloud(x, y, cw, ch);
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float) alpha));

        g.setPaint(new RadialGradientPaint(
                new Point2D.Double(x + cw * 0.42, y + ch * 0.30), (float) Math.max(1, cw * 0.72),
                new float[]{0f, 1f},
                new Color[]{CLOUD, CLOUD_DEEP}));
        g.fill(cloud);

        // Viền trắng vẽ SAU phần tô, không thì bị chính phần tô đè mất một nửa.
        g.setStroke(new BasicStroke(Math.max(1f, zoom * 0.9f), BasicStroke.CAP_ROUND,
                BasicStroke.JOIN_ROUND));
        g.setColor(EDGE);
        g.draw(cloud);

        g.setComposite(AlphaComposite.SrcOver);
    }

    /** Nền tối bầu dục cho mây đỏ nổi lên, kèm quầng đỏ thở theo nhịp trôi. */
    private static void backdrop(Graphics2D g, int w, int h, double breathe) {
        double cx = w / 2.0;
        double cy = h / 2.0;
        double rx = w / 2.0;
        double ry = h / 2.0;
        AffineTransform squash = AffineTransform.getTranslateInstance(cx, cy);
        squash.scale(1.0, ry / rx);
        squash.translate(-cx, -cy);

        paintRadial(g, cx, cy, rx, squash, DARK, DARK_ALPHA,
                new float[]{0f, 0.45f, 0.78f, 1f}, new double[]{1.0, 0.8, 0.2, 0});
        int a = (int) (GLOW_ALPHA * (0.55 + 0.45 * breathe));
        paintRadial(g, cx, cy, rx, squash, GLOW, a,
                new float[]{0f, 0.34f, 0.7f, 1f}, new double[]{1.0, 0.5, 0.14, 0});
    }

    private static void paintRadial(Graphics2D g, double cx, double cy, double rx,
            AffineTransform squash, Color base, int alpha, float[] stops, double[] mul) {
        Color[] cs = new Color[stops.length];
        for (int i = 0; i < stops.length; i++) {
            cs[i] = new Color(base.getRed(), base.getGreen(), base.getBlue(),
                    (int) Math.max(0, Math.min(255, alpha * mul[i])));
        }
        g.setPaint(new RadialGradientPaint(
                new Point2D.Double(cx, cy), (float) rx, new Point2D.Double(cx, cy),
                stops, cs, MultipleGradientPaint.CycleMethod.NO_CYCLE,
                MultipleGradientPaint.ColorSpaceType.SRGB, squash));
        g.fill(new Rectangle2D.Double(0, 0, cx * 2, cy * 2));
    }

    /**
     * Đám mây Akatsuki, vẽ bằng ĐƯỜNG VIỀN chép theo hình gốc.
     *
     * Bản trước dựng bằng phép hợp mấy hình tròn chồng nhau và làm lại năm lần vẫn không ra: hợp
     * hình tròn thì mọi chỗ lồi đều là cung tròn cùng kiểu, nên ra "một cục tròn tròn" chứ không
     * ra một hình có nét riêng. Biểu tượng gốc là hình vẽ tay -- ba bướu KHÔNG cùng bán kính,
     * đáy võng chứ không phình, và cái đuôi cuộn ngược vào trong. Cách duy nhất giữ được mấy nét
     * đó là chép thẳng đường viền ra toạ độ.
     *
     * Toạ độ ghi trong ô đơn vị 0..1 (y hướng xuống) rồi mới nhân theo cỡ thật, nên đổi kích
     * thước không phải sửa lại điểm nào.
     */
    private static Shape cloud(double x, double y, double w, double h) {
        Path2D.Double p = new Path2D.Double();
        // Mũi trái
        p.moveTo(nx(x, w, 0.015), ny(y, h, 0.60));
        // Ba bướu trên, bán kính khác nhau: nhỏ - lớn - vừa
        p.curveTo(nx(x, w, 0.03), ny(y, h, 0.34), nx(x, w, 0.14), ny(y, h, 0.22),
                nx(x, w, 0.25), ny(y, h, 0.29));
        p.curveTo(nx(x, w, 0.29), ny(y, h, 0.06), nx(x, w, 0.50), ny(y, h, 0.02),
                nx(x, w, 0.57), ny(y, h, 0.19));
        p.curveTo(nx(x, w, 0.67), ny(y, h, 0.08), nx(x, w, 0.81), ny(y, h, 0.14),
                nx(x, w, 0.82), ny(y, h, 0.33));
        // Đuôi vươn lên phải rồi cuộn ngược vào trong -- nét nhận dạng của biểu tượng
        p.curveTo(nx(x, w, 0.93), ny(y, h, 0.30), nx(x, w, 1.00), ny(y, h, 0.42),
                nx(x, w, 0.95), ny(y, h, 0.55));
        p.curveTo(nx(x, w, 0.91), ny(y, h, 0.66), nx(x, w, 0.78), ny(y, h, 0.64),
                nx(x, w, 0.79), ny(y, h, 0.53));
        p.curveTo(nx(x, w, 0.80), ny(y, h, 0.46), nx(x, w, 0.88), ny(y, h, 0.45),
                nx(x, w, 0.88), ny(y, h, 0.53));
        p.curveTo(nx(x, w, 0.88), ny(y, h, 0.62), nx(x, w, 0.80), ny(y, h, 0.72),
                nx(x, w, 0.68), ny(y, h, 0.76));
        // Đáy VÕNG vào, không phình ra
        p.curveTo(nx(x, w, 0.52), ny(y, h, 0.83), nx(x, w, 0.36), ny(y, h, 0.72),
                nx(x, w, 0.24), ny(y, h, 0.80));
        p.curveTo(nx(x, w, 0.13), ny(y, h, 0.87), nx(x, w, 0.02), ny(y, h, 0.76),
                nx(x, w, 0.015), ny(y, h, 0.60));
        p.closePath();
        return p;
    }

    private static double nx(double x, double w, double u) {
        return x + w * u;
    }

    private static double ny(double y, double h, double v) {
        return y + h * v;
    }

    private static String sprites(int w1, int h1) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < frameCount; i++) {
            sb.append(i > 0 ? "," : "")
              .append("{\"w\":").append(w1).append(",\"x\":0,\"h\":").append(h1)
              .append(",\"y\":").append(i * h1).append(",\"id\":").append(i).append('}');
        }
        return sb.append(']').toString();
    }

    private static String frames(int w1, int h1, int bottomY) {
        int dx = -w1 / 2;
        int dy = bottomY - h1;
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < frameCount; i++) {
            sb.append(i > 0 ? "," : "")
              .append("[{\"id\":").append(i).append(",\"dx\":").append(dx)
              .append(",\"dy\":").append(dy).append(",\"onTop\":0,\"flip\":0}]");
        }
        return sb.append(']').toString();
    }

    /** Trôi đều, không quãng nghỉ: mây mà đứng khựng một nhịp là lộ ngay chỗ vòng lặp nối lại. */
    private static String running() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < frameCount; i++) {
            for (int k = 0; k < 6; k++) {
                sb.append(i == 0 && k == 0 ? "" : ",").append(i);
            }
        }
        int nhip = frameCount * 6;
        if (nhip > SEQ_MAX) {
            throw new IllegalStateException("chuỗi phát " + nhip + " nhịp > " + SEQ_MAX);
        }
        return "[" + sb + "]";
    }
}
