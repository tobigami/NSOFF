import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.MultipleGradientPaint;
import java.awt.RadialGradientPaint;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

/**
 * Vẽ hiệu ứng danh hiệu dạng "hai con mắt mở ra", chọn được hoa văn trong tròng.
 *
 * Mỗi mắt là một hình quả hạnh, bên trong vẽ hoa văn tuỳ kiểu. Chuyển động là mí mắt hé dần:
 * chiều cao mắt chạy từ gần khép tới mở hẳn qua từng khung, còn chuỗi phát thì giữ mắt mở phần
 * lớn thời gian rồi chớp một cái. Phần hình học, hoạt hoạ và giới hạn dùng chung cho mọi kiểu --
 * chỉ hoa văn và bảng màu là khác.
 *
 *   luanhoi   -- Luân Hồi: vòng gợn đồng tâm, tím
 *   sharingan -- Sharingan ba dấu phẩy, đỏ
 *   mangekyo  -- Mangekyō của Itachi: ba lưỡi cong xoay như chong chóng, đỏ
 *
 * BA RÀNG BUỘC PHẢI GIỮ (giống hệt MakeTitleEffect, và tasks/todo.md có ghi lại)
 *
 * 1. Toạ độ trong effect_data chỉ có MỘT bộ, tính theo mức thu phóng 1; ảnh thì mỗi mức một tấm.
 *    Nên tấm mức z phải to đúng gấp z lần tấm mức 1, không lệch một pixel.
 * 2. EffectData.setData() ghi x, y, w, h mỗi khung bằng một byte, client đọc không dấu: ở mức 1
 *    mỗi khung rộng tối đa 255 và cả tấm xếp chồng cao tối đa 255.
 * 3. Vượt trần thì không ai báo lỗi -- máy chủ chạy, client nhận, chỉ hình vẽ ra là rác. Nên
 *    chương trình tự chặn, và tools/checks/EffectCheck.java kiểm lại lần nữa sau khi vào CSDL.
 *
 * Chi tiết hoa văn thưa dần theo mức thu phóng: ở mức 1 con mắt chỉ cao 11 pixel, nhồi hết chi
 * tiết vào đó thì chúng dính thành một mảng đặc. Mỗi mức một tấm riêng nên hạ chi tiết ở mức thấp
 * không ảnh hưởng gì mức cao.
 *
 *   java MakeEyeEffect <kiểu> <thư mục ra> <idHiệuỨng> <mépDưới>
 *                      [caoMắt] [sốKhung] [đậmNền] [đậmAura] [chừaNgang] [chừaDọc] [elip|goc|tron|manga|glare]
 *
 * In ra ba cột JSON của effect_data để dán thẳng vào CSDL.
 */
public final class MakeEyeEffect {

    /** Chiều cao mắt lúc mở hẳn, ở mức thu phóng 1. Mọi kích thước khác suy ra từ đây. */
    private static final int EYE_H_Z1 = 11;

    /** Mắt rộng gấp đôi chiều cao -- tỉ lệ quả hạnh, hẹp hơn thì nhìn như hạt đậu. */
    private static final double EYE_ASPECT = 1.9;

    /** Khoảng hở giữa hai mắt, theo phần chiều rộng một mắt. */
    private static final double GAP_RATIO = 0.30;

    /**
     * Kiểu glare có bộ tỉ lệ riêng, không dùng chung với hai kiểu kia.
     *
     * Ảnh mẫu là một khuôn mặt trong bóng tối chứ không phải hai con mắt dán cạnh nhau: mắt DẸT
     * hơn nhiều và hai mắt CÁCH XA nhau đúng bằng bề ngang một con mắt. Giữ tỉ lệ của kiểu elip
     * thì dù vẽ đúng từng chi tiết, tổng thể vẫn ra "hai con mắt" chứ không ra "ánh nhìn".
     */
    private static final double GLARE_ASPECT = 2.55;
    private static final double GLARE_GAP = 1.05;

    private static double aspect() {
        if (shape.equals("tron")) {
            return 1.0;                       // đĩa tròn: rộng bằng cao
        }
        return shape.equals("glare") ? GLARE_ASPECT : EYE_ASPECT;
    }

    private static double gapRatio() {
        return shape.equals("glare") ? GLARE_GAP : GAP_RATIO;
    }

    /**
     * Chỗ chừa quanh hình, ở mức thu phóng 1. Đây chính là chỗ cho vầng aura toả ra, nên nó rộng
     * hơn hẳn phần viền -- chừa hẹp thì aura bị cắt vuông ở mép khung, nhìn như một miếng dán.
     *
     * Chừa ngang nhiều hơn dọc vì đôi mắt vốn nằm ngang: vầng sáng bè ra hai bên mới thuận mắt,
     * còn nống lên trên thì vừa tốn chiều cao khung -- thứ đang bị trần 255 chặn -- vừa đội lên
     * che mất tên nhân vật.
     */
    private static final int PAD_X_Z1 = 18;
    private static final int PAD_Y_Z1 = 8;

    /**
     * Hai số chừa này quyết định vùng aura rộng bao nhiêu, và chúng KHÔNG cùng một ngân sách.
     *
     * Chừa ngang gần như thoải mái: khung rộng tối đa 255, mà hai con mắt chỉ chiếm 48 -- còn tới
     * hơn trăm pixel mỗi bên. Chừa dọc thì chật, vì nó nhân với số khung: cả tấm cao tối đa 255
     * nên chín khung là mỗi khung chỉ được 28. Muốn aura cao thêm thì phải bớt khung của động tác
     * hé mí, không có đường nào khác.
     */
    private static int padX = PAD_X_Z1;
    private static int padY = PAD_Y_Z1;

    /**
     * Số khung của động tác mở mắt. Khung 0 gần khép, khung cuối mở hẳn.
     *
     * Chín chứ không phải mười: khung cao 27 (mắt 11 + hai lần chừa 8), mười khung là tấm cao 270,
     * vượt trần một byte. Chín khung ra 243, còn dư một chút.
     */
    private static final int FRAMES = 9;

    /** Độ hé của khung 0: không để 0 hẳn vì mắt khép kín thành một vạch, nhìn như mất hình. */
    private static final double OPEN_MIN = 0.08;

    /** Mép dưới hiệu ứng tính từ chân nhân vật. Giữ đúng chỗ mép dưới của bản gốc 203 (-95 + 39). */
    private static final int BOTTOM_Y = -56;

    private static final int BYTE_MAX = 255;

    /**
     * Trần THẬT của độ dài chuỗi phát, và nó không phải 255.
     *
     * Server ghi độ dài bằng writeByte còn client đọc bằng readUnsignedByte, nên nhìn qua tưởng
     * được tới 255. Cái chặn nằm chỗ khác: con trỏ chạy chuỗi trong client là **một byte**
     * (`private byte dD`), và cách nó quay vòng là `if (dD > dB.length) dD = 0`. Chuỗi dài hơn
     * 126 thì dD đếm tới 127 rồi tràn xuống -128, điều kiện quay vòng không bao giờ đúng nữa,
     * `dB[-128]` ném lỗi -- và client bọc cả khối trong `catch (Exception) {}` rỗng, nên không có
     * dấu hiệu nào cả: hiệu ứng chỉ lặng lẽ đứng hình ở khung đang vẽ dở.
     *
     * 126 chứ không phải 127: quay vòng cần dD đạt tới length + 1, mà 128 đã tràn.
     */
    private static final int SEQ_MAX = 126;

    /** Bảng màu và hoa văn của một kiểu mắt. Mọi thứ còn lại dùng chung. */
    private static final class Skin {
        final Color irisIn, irisMid, irisOut, mark, pupil, lid, glow, aura, dark;

        Skin(int irisIn, int irisMid, int irisOut, int mark, int pupil, int lid,
                int glow, int aura, int dark) {
            this.irisIn = new Color(irisIn);
            this.irisMid = new Color(irisMid);
            this.irisOut = new Color(irisOut);
            this.mark = new Color(mark);
            this.pupil = new Color(pupil);
            this.lid = new Color(lid);
            this.glow = new Color(glow);
            this.aura = new Color(aura);
            this.dark = new Color(dark);
        }
    }

    private static Skin skin(String kind) {
        switch (kind) {
            case "luanhoi":
                return new Skin(0xE0CCF5, 0xB18BDE, 0x7A4BB5, 0x4A1F7D, 0x24093F, 0x160726,
                        0x9B6BE0, 0xB06BFF, 0x0B0414);
            case "sharingan":
            case "mangekyo":
                // Tròng đỏ tươi ở giữa, sẫm dần ra rìa; hoa văn và con ngươi cùng một màu đen ngả
                // đỏ chứ không đen tuyền -- đen tuyền trên nền đỏ nhìn như thủng một lỗ.
                return new Skin(0xFF6A5A, 0xE01B18, 0x8E0A0A, 0x1A0203, 0x1A0203, 0x140304,
                        0xFF5B4A, 0xFF4436, 0x140406);
            default:
                throw new IllegalArgumentException("kiểu mắt lạ: " + kind);
        }
    }

    /**
     * Hình khung mắt:
     *   elip  -- quả hạnh đối xứng
     *   manga -- sắc, lệch, có đuôi hất; lòng mắt đổ kín một màu
     *   tron  -- MỘT đĩa tròn ở giữa, không mí mắt; hoa văn xoay tròn qua từng khung thay cho
     *            động tác hé mí
     *   goc   -- góc cạnh: mí trên và mí dưới là hai đoạn THẲNG nằm ngang, nối vào hai khoé
     *            nhọn bằng bốn đoạn xiên. Không có một đoạn cong nào
     *   glare -- ánh mắt gắt trong bóng tối: mí xếch và nhọn, CÓ lòng trắng, tròng đỏ là một
     *            vòng tròn nằm trong đó và bị mí trên cắt mất chỏm, nền tối thay cho quầng đỏ
     */
    private static String shape = "elip";

    private static String kind = "luanhoi";
    private static Skin skin = skin("luanhoi");
    private static int effectId = 203;
    private static int bottomY = BOTTOM_Y;

    private static final Color SCLERA_IN = new Color(0xFAFAFA);
    private static final Color SCLERA_OUT = new Color(0xB9BDC6);
    private static final Color IRIS_HOT = new Color(0xE23A2E);
    private static final Color IRIS_DEEP = new Color(0x6E0808);
    private static final Color SKIN_GLOW = new Color(0xC9CCD4);

    /** Độ đậm nhất của aura ở lõi, lúc mắt mở hẳn. Trên 170 là nó át mất chính đôi mắt. */
    private static final int AURA_ALPHA = 180;

    /**
     * Độ đậm nhất của lớp nền tối. Nó nằm dưới aura và làm nền cho tím sáng bật lên -- tím trên
     * nền cỏ xanh thì xỉn, tím trên nền tối mới ra tím.
     *
     * Đừng đẩy quá 200: lớp này che luôn cả cảnh game phía sau, đậm quá thì thành một miếng dán
     * đen chứ không còn là quầng khí.
     */
    private static final int DARK_ALPHA = 190;

    private static int eyeH = EYE_H_Z1;
    private static int frameCount = FRAMES;
    private static int darkAlpha = DARK_ALPHA;
    private static int auraAlpha = AURA_ALPHA;

    private MakeEyeEffect() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 4) {
            System.out.println("dùng: java MakeEyeEffect <luanhoi|sharingan|mangekyo> <thư mục ra>"
                    + " <idHiệuỨng> <mépDưới> [caoMắt] [sốKhung] [đậmNền] [đậmAura]"
                    + " [chừaNgang] [chừaDọc] [elip|goc|tron|manga|glare]");
            System.exit(2);
        }
        kind = args[0];
        skin = skin(kind);
        File outDir = new File(args[1]);
        effectId = Integer.parseInt(args[2]);
        bottomY = Integer.parseInt(args[3]);
        if (args.length > 4) {
            eyeH = Integer.parseInt(args[4]);
        }
        if (args.length > 5) {
            frameCount = Integer.parseInt(args[5]);
        }
        if (args.length > 6) {
            darkAlpha = Integer.parseInt(args[6]);
        }
        if (args.length > 7) {
            auraAlpha = Integer.parseInt(args[7]);
        }
        if (args.length > 8) {
            padX = Integer.parseInt(args[8]);
        }
        if (args.length > 9) {
            padY = Integer.parseInt(args[9]);
        }
        if (args.length > 10) {
            shape = args[10];
            if (!shape.equals("elip") && !shape.equals("manga") && !shape.equals("glare")
                    && !shape.equals("goc") && !shape.equals("tron")) {
                System.out.println("!! hình khung lạ: " + shape
                        + " (chỉ có elip | goc | tron | manga | glare)");
                System.exit(2);
            }
        }

        int eyeW = (int) Math.round(eyeH * aspect());
        int gap = (int) Math.round(eyeW * gapRatio());
        int w1 = shape.equals("tron") ? eyeW + 2 * padX : 2 * eyeW + gap + 2 * padX;
        int h1 = eyeH + 2 * padY;

        if (w1 > BYTE_MAX) {
            System.out.println("!! khung rộng " + w1 + " > " + BYTE_MAX
                    + " ở mức 1: hạ chừaNgang hoặc caoMắt.");
            System.exit(1);
        }
        if (h1 * frameCount > BYTE_MAX) {
            System.out.println("!! tấm cao " + (h1 * frameCount) + " > " + BYTE_MAX
                    + " ở mức 1: hạ chừaDọc, hạ caoMắt, hoặc giảm sốKhung."
                    + " Chừa dọc nhân với số khung nên nó tốn gấp " + frameCount + " lần.");
            System.exit(1);
        }

        for (int z = 1; z <= 4; z++) {
            BufferedImage sheet = new BufferedImage(w1 * z, h1 * frameCount * z,
                    BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = sheet.createGraphics();
            for (int i = 0; i < frameCount; i++) {
                g.drawImage(frame(z, w1 * z, h1 * z, openness(i), i), 0, i * h1 * z, null);
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
        System.out.println("frames  = " + frames(w1, h1));
        System.out.println("running = " + running());
    }

    /**
     * Độ mở của khung thứ i, 0..1. Bình phương ngược lại cho mắt bật nhanh ở đoạn đầu rồi chậm
     * dần lúc gần mở hẳn -- mở đều tăm tắp nhìn như cửa cuốn chứ không như mí mắt.
     */
    private static double openness(int i) {
        if (shape.equals("tron")) {
            return 1.0;                       // không hé mí; cái động là hoa văn xoay
        }
        double t = i / (double) (frameCount - 1);
        double eased = 1 - (1 - t) * (1 - t);
        return OPEN_MIN + (1 - OPEN_MIN) * eased;
    }

    private static BufferedImage frame(int zoom, int w, int h, double open, int index) {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

        double eyeW = eyeH * aspect() * zoom;
        double gap = eyeW * gapRatio();
        double cy = h / 2.0;
        double left = padX * zoom;

        veil(g, w, h, open);
        aura(g, w, h, open);
        if (shape.equals("tron")) {
            drawEye(g, zoom, left, cy, eyeW, open, false, index);
            return img;
        }
        // Mắt trái lật ngang so với mắt phải: khung manga lệch hẳn về một bên nên hai mắt phải
        // soi gương nhau, không thì cả khuôn mặt nhìn như đang liếc.
        drawEye(g, zoom, left, cy, eyeW, open, true, index);
        drawEye(g, zoom, left + eyeW + gap, cy, eyeW, open, false, index);

        g.dispose();
        return img;
    }

    /**
     * Lớp nền tối, vẽ trước aura và trước đôi mắt.
     *
     * Cùng hình bầu dục và cùng nhịp tắt/bừng như aura, nhưng CO HẸP HƠN aura: lõi tối gần như
     * đặc rồi tắt sớm, để vành tím toả ra ngoài rìa vùng tối chứ không chồng lên nó.
     *
     * Chồng lên là hỏng, và đây là chỗ tôi làm sai ở bản đầu: một lớp tối bán trong suốt phủ lên
     * nền sáng cho ra màu XÁM, rồi tím phủ tiếp lên xám thì ra tím xỉn. Cách chữa không phải là
     * hạ độ đậm -- càng hạ càng xám -- mà là đẩy lõi lên gần đặc để nó ra màu tối thật, và thu
     * bán kính lại để phần rìa không còn mảng xám nào.
     *
     * Phải đi theo độ mở giống aura, không thì lúc mắt nhắm còn trơ lại một vệt tối trên đầu
     * nhân vật, nhìn như lỗi ảnh.
     */
    private static void veil(Graphics2D g, int w, int h, double open) {
        double cx = w / 2.0;
        double cy = h / 2.0;
        double rx = w / 2.0;
        double ry = h / 2.0;
        if (rx <= 0 || ry <= 0) {
            return;
        }
        // Kiểu glare lấy nền ĐEN TUYỀN. Dùng nền ngả đỏ của bảng màu sharingan thì quanh mắt
        // hiện quầng nâu, mà thứ làm ảnh mẫu ấn tượng chính là bóng tối không màu.
        Color base = shape.equals("glare") ? new Color(0x050506) : skin.dark;
        int a = (int) Math.round(darkAlpha * open);
        AffineTransform squash = AffineTransform.getTranslateInstance(cx, cy);
        squash.scale(1.0, ry / rx);
        squash.translate(-cx, -cy);
        g.setPaint(new RadialGradientPaint(
                new Point2D.Double(cx, cy), (float) rx, new Point2D.Double(cx, cy),
                new float[]{0f, 0.5f, 0.8f, 1f},
                new Color[]{
                        new Color(base.getRed(), base.getGreen(), base.getBlue(), a),
                        new Color(base.getRed(), base.getGreen(), base.getBlue(), (int) (a * 0.85)),
                        new Color(base.getRed(), base.getGreen(), base.getBlue(), (int) (a * 0.25)),
                        new Color(base.getRed(), base.getGreen(), base.getBlue(), 0)},
                MultipleGradientPaint.CycleMethod.NO_CYCLE,
                MultipleGradientPaint.ColorSpaceType.SRGB,
                squash));
        g.fill(new Rectangle2D.Double(0, 0, w, h));
    }

    /**
     * Vầng aura tím phủ kín khung, đậm ở tâm và tan dần ra mép.
     *
     * Vẽ bằng RadialGradientPaint có ma trận biến đổi bóp trục dọc, nên vùng sáng là hình bầu dục
     * nằm ngang ôm lấy đôi mắt chứ không phải hình tròn -- tròn thì phần trên phần dưới thừa ra,
     * vừa tốn chiều cao khung vừa nhìn như quả bóng.
     *
     * Độ đậm đi theo độ mở của mắt: mắt khép thì aura gần như tắt, mắt mở hẳn thì bừng lên. Nhờ
     * vậy không cần thêm khung riêng cho aura -- thứ mà trần 255 của chiều cao tấm không cho phép.
     *
     * Mép ngoài phải về đúng alpha 0, không thì thấy một đường viền chữ nhật ở rìa khung.
     */
    private static void aura(Graphics2D g, int w, int h, double open) {
        double cx = w / 2.0;
        double cy = h / 2.0;
        double rx = w / 2.0;
        double ry = h / 2.0;
        if (rx <= 0 || ry <= 0) {
            return;
        }
        // Kiểu glare không có quầng đỏ: thứ sáng lên quanh mắt là DA bắt sáng mờ, màu xám.
        Color tone = shape.equals("glare") ? SKIN_GLOW : skin.aura;
        int a = (int) Math.round(auraAlpha * open);
        AffineTransform squash = AffineTransform.getTranslateInstance(cx, cy);
        squash.scale(1.0, ry / rx);
        squash.translate(-cx, -cy);
        g.setPaint(new RadialGradientPaint(
                new Point2D.Double(cx, cy), (float) rx, new Point2D.Double(cx, cy),
                // Lõi sáng co lại quanh đôi mắt rồi tắt nhanh: trải đều ra cả khung thì nó thành
                // một mảng phẳng đục, mất hẳn cảm giác toả sáng.
                new float[]{0f, 0.28f, 0.6f, 1f},
                new Color[]{
                        new Color(tone.getRed(), tone.getGreen(), tone.getBlue(), a),
                        new Color(tone.getRed(), tone.getGreen(), tone.getBlue(), (int) (a * 0.52)),
                        new Color(tone.getRed(), tone.getGreen(), tone.getBlue(), (int) (a * 0.14)),
                        new Color(tone.getRed(), tone.getGreen(), tone.getBlue(), 0)},
                MultipleGradientPaint.CycleMethod.NO_CYCLE,
                MultipleGradientPaint.ColorSpaceType.SRGB,
                squash));
        g.fill(new Rectangle2D.Double(0, 0, w, h));
    }

    private static void drawEye(Graphics2D g, int zoom, double x, double cy, double eyeW,
            double open, boolean mirror, int index) {
        double halfH = eyeH * zoom * open / 2.0;
        Shape lens = eyeShape(x, cy, eyeW, halfH, mirror);

        // Quầng tím toả ra, đậm dần theo độ mở. Vẽ bằng mấy lớp ellipse mờ chồng nhau vì Java2D
        // không có blur sẵn, mà kéo thêm ConvolveOp cho một cái quầng thì không đáng.
        for (int i = 3; i >= 1; i--) {
            double grow = i * 1.6 * zoom;
            Color lit = shape.equals("glare") ? SKIN_GLOW : skin.glow;
            g.setColor(new Color(lit.getRed(), lit.getGreen(), lit.getBlue(),
                    (int) ((shape.equals("glare") ? 26 : 44) * open / i)));
            g.fill(eyeShape(x - grow, cy, eyeW + 2 * grow, halfH + grow, mirror));
        }

        // Tròng mắt: sáng ở giữa, tối dần ra rìa. Tâm gradient đặt hơi lệch lên cho có khối.
        double r = Math.max(eyeW, halfH * 2) * 0.62;
        g.setPaint(new RadialGradientPaint(
                new Point2D.Double(x + eyeW / 2, cy - halfH * 0.25),
                (float) r,
                new float[]{0f, 0.45f, 1f},
                new Color[]{skin.irisIn, skin.irisMid, skin.irisOut}));
        g.fill(lens);

        if (shape.equals("glare")) {
            glareEye(g, zoom, x, cy, eyeW, halfH, lens, mirror);
            g.setComposite(AlphaComposite.SrcOver);
            g.setStroke(new BasicStroke(Math.max(1f, zoom * 1.7f)));
            g.setColor(skin.lid);
            g.draw(lens);
            return;
        }

        // Tâm của hoa văn. Khung quả hạnh thì đúng giữa khung; khung truyện tranh thì phần mắt
        // mở dồn về phía khoé trong và hơi lên trên, nên phải dịch theo -- neo vào giữa khung là
        // hoa văn lệch hẳn ra rìa, mà hai mắt soi gương nhau nên lệch ngược chiều, càng rõ.
        double pcx = x + eyeW * (shape.equals("manga") ? 0.44 : 0.5);
        double pcy = cy - (shape.equals("manga") ? halfH * 0.14 : 0);
        if (mirror) {
            pcx = x + eyeW - (pcx - x);
        }

        // Hoa văn trong tròng, cắt gọn trong hình mắt để lúc mí khép nó bị che dần chứ không
        // tràn ra ngoài. Đây là chỗ duy nhất ba kiểu mắt khác nhau.
        Shape clip = g.getClip();
        g.clip(lens);
        if (shape.equals("tron")) {
            // Một vòng quay đủ 120 độ trải đều trên toàn bộ khung. Hoa văn đối xứng ba cánh nên
            // quay 120 độ là trùng lại chính nó -- khung cuối nối liền khung đầu, không giật.
            double spin = Math.toRadians(120.0) * index / frameCount;
            mangekyoDisc(g, zoom, pcx, pcy, halfH * 0.94, spin);
        } else if (kind.equals("luanhoi")) {
            rings(g, zoom, pcx, pcy, eyeW);
        } else if (kind.equals("mangekyo")) {
            mangekyo(g, zoom, pcx, pcy, halfH * 0.80);
        } else {
            tomoe(g, zoom, pcx, pcy, eyeW, halfH);
        }
        g.setClip(clip);

        // Con ngươi nhỏ ở tâm, luôn tròn nên lúc mắt hé thì nó bị mí che bớt -- đúng như thật.
        // Mangekyō thì bỏ: ba lưỡi đã chụm sẵn vào tâm, thêm con ngươi là thành một cục đen.
        if (!kind.equals("mangekyo") && !shape.equals("tron")) {
            double pr = kind.equals("luanhoi")
                    ? Math.max(zoom * 0.8, eyeW * 0.058)
                    : Math.max(zoom * 0.75, halfH * 0.24);
            Area pupil = new Area(new Ellipse2D.Double(pcx - pr, pcy - pr, pr * 2, pr * 2));
            pupil.intersect(new Area(lens));
            g.setColor(skin.pupil);
            g.fill(pupil);
        }

        // Viền mí, vẽ sau cùng để đè lên mọi thứ bên trong.
        g.setComposite(AlphaComposite.SrcOver);
        g.setStroke(new BasicStroke(Math.max(1f, zoom * (shape.equals("manga") ? 1.5f : 1.1f))));
        g.setColor(skin.lid);
        g.draw(lens);
    }


    /**
     * Vòng gợn đồng tâm của Luân Hồi. Thưa dần ở mức thu phóng thấp: mức 1 chỉ hai vòng, mức 4
     * được năm vòng. Vẽ dày đặc ở mức 1 thì chúng dính thành một mảng đặc.
     */
    private static void rings(Graphics2D g, int zoom, double cx, double cy, double eyeW) {
        int rings = Math.min(5, 1 + zoom);
        g.setStroke(new BasicStroke(Math.max(1f, zoom * 0.7f)));
        g.setColor(skin.mark);
        double step = eyeW / 2.0 / (rings + 1);
        for (int i = 1; i <= rings; i++) {
            double rr = step * i;
            g.draw(new Ellipse2D.Double(cx - rr, cy - rr, rr * 2, rr * 2));
        }
    }

    /**
     * Ba dấu phẩy của Sharingan, đặt cách nhau 120 độ trên một vòng quanh con ngươi.
     *
     * Mọi bán kính neo theo NỬA CHIỀU CAO mắt chứ không theo chiều rộng. Neo theo chiều rộng là
     * sai và đã trả giá: mắt rộng gấp 1,9 lần chiều cao, nên vòng tính theo chiều rộng vọt ra
     * ngoài mí, và hai dấu phẩy trên dưới bị cắt sạch -- chỉ còn thấy mỗi cái vòng.
     *
     * Ở mức thu phóng 1 con mắt chỉ cao 11 pixel nên đuôi cong của dấu phẩy chỉ còn một pixel
     * loè nhoè: vẽ tròn trơn thay cho dấu phẩy, sạch hơn hẳn. Từ mức 2 mới thêm đuôi.
     */
    private static void tomoe(Graphics2D g, int zoom, double cx, double cy, double eyeW, double halfH) {
        double ringR = halfH * 0.62;
        double dotR = Math.max(zoom * 0.75, halfH * 0.155);

        // Cố tình KHÔNG vẽ vòng tròn nối ba dấu phẩy. Ở cỡ này vòng, ba dấu phẩy và con ngươi
        // dính thành một cục đen, nhìn ra mỗi cái vòng. Bỏ vòng đi thì ba dấu phẩy mới tách bạch.
        g.setColor(skin.mark);

        for (int i = 0; i < 3; i++) {
            double ang = Math.toRadians(-90 + i * 120);
            double px = cx + Math.cos(ang) * ringR;
            double py = cy + Math.sin(ang) * ringR;
            g.fill(new Ellipse2D.Double(px - dotR, py - dotR, dotR * 2, dotR * 2));

            if (zoom >= 2) {
                // Đuôi vuốt theo chiều kim đồng hồ: một tam giác cong men theo vòng, thon dần về
                // mũi. Dựng bằng hai cung bậc hai nên khỏi phải ghép cung tròn.
                double a2 = ang + Math.toRadians(72);
                double ma = ang + Math.toRadians(36);
                Path2D.Double tail = new Path2D.Double();
                tail.moveTo(px + Math.cos(ang) * dotR * 0.9, py + Math.sin(ang) * dotR * 0.9);
                tail.quadTo(cx + Math.cos(ma) * (ringR + dotR * 0.95),
                        cy + Math.sin(ma) * (ringR + dotR * 0.95),
                        cx + Math.cos(a2) * ringR, cy + Math.sin(a2) * ringR);
                tail.quadTo(cx + Math.cos(ma) * (ringR - dotR * 0.15),
                        cy + Math.sin(ma) * (ringR - dotR * 0.15),
                        px - Math.cos(ang) * dotR * 0.6, py - Math.sin(ang) * dotR * 0.6);
                tail.closePath();
                g.fill(tail);
            }
        }
    }


    /**
     * Hoa văn Mangekyō cho đĩa tròn: ba lưỡi liềm MẢNH vươn tới sát mép đĩa, nhọn cả hai đầu.
     *
     * Khác bản dùng cho khung mắt ở ba chỗ, và cả ba đều lấy từ ảnh mẫu:
     *   - lưỡi mảnh và dài hơn, mũi chạm mép đĩa chứ không dừng ở trong;
     *   - KHÔNG có vòng tròn bao ngoài -- trên nền đĩa đỏ trơn thì vòng đó thừa;
     *   - chấm tâm màu ĐỎ chứ không đen: ba lưỡi chụm lại đã kín tâm, khoét một lỗ đỏ mới ra
     *     đúng hình bản gốc.
     *
     * Mỗi lưỡi dựng từ HAI đường xoắn trùng nhau ở hai đầu và tách ra ở quãng giữa: độ tách đi
     * theo sin(pi*t) nên bằng 0 ở cả hai mũi. Đó là cách cho ra lưỡi nhọn hai đầu mà không phải
     * ghép hình -- cho bề dày cố định thì hai đầu cụt, nhìn như ba cái dấu phẩy.
     */
    private static void mangekyoDisc(Graphics2D g, int zoom, double cx, double cy, double r,
            double spin) {
        final double TWIST = Math.toRadians(78);   // lưỡi quét ngược bao nhiêu khi chạy ra mép
        final double WIDTH = Math.toRadians(21);   // độ tách hai mép ở quãng giữa
        final double R_IN = 0.09;
        int steps = 16;

        g.setColor(skin.mark);
        for (int i = 0; i < 3; i++) {
            double aOut = spin + Math.toRadians(-90 + i * 120);
            double aIn = aOut + TWIST;
            Path2D.Double blade = new Path2D.Double();
            for (int k = 0; k <= steps; k++) {
                double t = k / (double) steps;
                double a = aIn + (aOut - aIn) * t;
                double rr = r * (R_IN + (1 - R_IN) * t);
                double px = cx + Math.cos(a) * rr;
                double py = cy + Math.sin(a) * rr;
                if (k == 0) {
                    blade.moveTo(px, py);
                } else {
                    blade.lineTo(px, py);
                }
            }
            for (int k = steps; k >= 0; k--) {
                double t = k / (double) steps;
                double a = aIn + (aOut - aIn) * t + WIDTH * Math.sin(Math.PI * t);
                double rr = r * (R_IN + (1 - R_IN) * t);
                blade.lineTo(cx + Math.cos(a) * rr, cy + Math.sin(a) * rr);
            }
            blade.closePath();
            g.fill(blade);
        }

        double dot = Math.max(1, r * 0.085);
        g.setColor(skin.irisMid);
        g.fill(new Ellipse2D.Double(cx - dot, cy - dot, dot * 2, dot * 2));
    }

    /**
     * Mangekyō của Itachi: ba lưỡi liềm xoáy quanh tâm, viền bởi vòng tròng mắt.
     *
     * Điểm mấu chốt để ra dáng chong chóng chứ không ra nan quạt: đầu trong của lưỡi phải LỆCH
     * GÓC so với đầu ngoài. Nan quạt là đầu trong và đầu ngoài cùng một hướng bán kính; lưỡi
     * liềm là đầu trong bị xoay đi một góc (TWIST), nên cả lưỡi vừa thu nhỏ vừa quay khi chạy
     * vào tâm.
     *
     * Hai mép của lưỡi vẽ bằng cách đi bộ dọc một đường xoắn -- nội suy đều cả góc lẫn bán kính
     * rồi nối các điểm. Lần trước tôi thử đặt điểm điều khiển Bezier bằng tay và ra ba cái nêm
     * béo: đường bậc ba chỉ cho uốn hai lần, mà muốn vừa xoay vừa thu thì phải uốn liên tục.
     * Đi bộ theo xoắn thì hình đúng theo công thức, khỏi phải mò.
     *
     * Ba lớp theo đúng thứ tự bản gốc: vòng tròng bao ngoài (mũi lưỡi chạm vào nó), ba lưỡi,
     * rồi chấm tâm nhỏ cho chỗ giao ba lưỡi gọn lại.
     *
     * Mức thu phóng 1 bỏ vòng tròng và chấm tâm: con mắt cao 13 pixel, thêm hai chi tiết nữa là
     * tất cả dính thành một mảng đen.
     */
    private static void mangekyo(Graphics2D g, int zoom, double cx, double cy, double r) {

        /** Bề rộng lưỡi ở vành, tính bằng góc. Rộng quá thì ba lưỡi nuốt hết phần đỏ. */
        final double SPAN = Math.toRadians(40);
        /** Góc mà đầu trong bị xoay đi so với đầu ngoài -- chính là độ xoáy của chong chóng. */
        final double TWIST = Math.toRadians(64);
        /** Đầu trong không chụm hẳn vào tâm, chừa lại một chút cho chấm tâm ngồi vào. */
        final double R_IN = 0.14;

        g.setColor(skin.mark);

        if (zoom >= 2) {
            g.setStroke(new BasicStroke(Math.max(1f, zoom * 0.5f)));
            g.draw(new Ellipse2D.Double(cx - r, cy - r, r * 2, r * 2));
        }

        for (int i = 0; i < 3; i++) {
            double a0 = Math.toRadians(-90 + i * 120);
            double aIn = a0 + SPAN + TWIST;
            Path2D.Double blade = new Path2D.Double();
            spiral(blade, cx, cy, aIn, r * R_IN, a0, r * 0.97, true);
            arc(blade, cx, cy, a0, a0 + SPAN, r * 0.97);
            spiral(blade, cx, cy, a0 + SPAN, r * 0.97, aIn, r * R_IN, false);
            blade.closePath();
            g.fill(blade);
        }

        if (zoom >= 2) {
            double dot = Math.max(zoom * 0.6, r * 0.15);
            g.fill(new Ellipse2D.Double(cx - dot, cy - dot, dot * 2, dot * 2));
        }
    }

    /** Nối các điểm dọc một đường xoắn: góc và bán kính cùng nội suy đều từ đầu này sang đầu kia. */
    private static void spiral(Path2D.Double p, double cx, double cy,
            double a0, double r0, double a1, double r1, boolean moveFirst) {
        int steps = 14;
        for (int i = 0; i <= steps; i++) {
            double t = i / (double) steps;
            double a = a0 + (a1 - a0) * t;
            double rr = r0 + (r1 - r0) * t;
            double px = cx + Math.cos(a) * rr;
            double py = cy + Math.sin(a) * rr;
            if (i == 0 && moveFirst) {
                p.moveTo(px, py);
            } else {
                p.lineTo(px, py);
            }
        }
    }

    /** Men theo vành từ góc này sang góc kia, bán kính không đổi. */
    private static void arc(Path2D.Double p, double cx, double cy, double a0, double a1, double r) {
        int steps = 8;
        for (int i = 1; i <= steps; i++) {
            double a = a0 + (a1 - a0) * i / (double) steps;
            p.lineTo(cx + Math.cos(a) * r, cy + Math.sin(a) * r);
        }
    }


    /**
     * Khung mắt kiểu "ánh mắt gắt trong bóng tối": mí xếch, hai khoé nhọn hoắt.
     *
     * Khác hai khung kia ở chỗ mí trên gần như một đường THẲNG xiên chứ không phải cung tròn --
     * đó mới là thứ tạo ra cái nhìn gằn. Cung tròn dù nhọn hai đầu vẫn cho cảm giác hiền.
     */
    private static Shape glare(double x, double cy, double w, double halfH) {
        double xi = x;                        // khoé trong, thấp và nhọn
        double yi = cy + halfH * 0.52;
        double xo = x + w;                    // khoé ngoài, cao và nhọn
        double yo = cy - halfH * 0.30;

        Path2D.Double p = new Path2D.Double();
        p.moveTo(xi, yi);
        // Mí trên: gần THẲNG một đoạn dài rồi mới bẻ xuống mũi ngoài. Điểm điều khiển đặt sát
        // đường nối hai đầu nên đoạn này gần như đường thẳng -- đó mới là cái nhìn gằn.
        p.curveTo(x + w * 0.13, cy - halfH * 0.62,
                x + w * 0.34, cy - halfH * 1.08,
                x + w * 0.56, cy - halfH * 1.06);
        p.curveTo(x + w * 0.76, cy - halfH * 1.04,
                x + w * 0.92, cy - halfH * 0.78,
                xo, yo);
        // Mí dưới: thẳng hơn nữa, chỉ võng rất nhẹ.
        p.curveTo(x + w * 0.78, cy + halfH * 0.16,
                x + w * 0.52, cy + halfH * 0.74,
                x + w * 0.30, cy + halfH * 0.80);
        p.curveTo(x + w * 0.19, cy + halfH * 0.83,
                x + w * 0.08, cy + halfH * 0.76,
                xi, yi);
        p.closePath();
        return p;
    }

    /**
     * Ruột của kiểu glare: lòng trắng, tròng đỏ hình tròn, hoa văn, rồi chấm sáng phản quang.
     *
     * Điểm khiến nó giống ảnh mẫu hơn hẳn hai kiểu kia: tròng đỏ là một VÒNG TRÒN nhỏ hơn khe
     * mắt và bị mí trên cắt mất chỏm, thay vì đổ đỏ kín cả khe. Chính khoảng trắng còn lại ở hai
     * khoé mới cho ra cái nhìn gằn -- đổ kín màu thì mắt nào cũng thành mắt thú.
     *
     * Tròng đặt lệch về phía khoé trong, đúng như người đang nhìn xoáy vào giữa.
     */
    private static void glareEye(Graphics2D g, int zoom, double x, double cy, double eyeW,
            double halfH, Shape lens, boolean mirror) {
        Shape clip = g.getClip();
        g.clip(lens);

        Rectangle2D b = lens.getBounds2D();
        g.setPaint(new RadialGradientPaint(
                new Point2D.Double(b.getCenterX(), b.getCenterY()),
                (float) Math.max(1, b.getWidth() * 0.55),
                new float[]{0f, 1f},
                new Color[]{SCLERA_IN, SCLERA_OUT}));
        g.fill(lens);

        double ix = x + eyeW * 0.40;
        if (mirror) {
            ix = x + eyeW - (ix - x);
        }
        double iy = cy + halfH * 0.16;
        double ir = halfH * 0.82;

        g.setPaint(new RadialGradientPaint(
                new Point2D.Double(ix, iy - ir * 0.25), (float) (ir * 1.15),
                new float[]{0f, 0.55f, 1f},
                new Color[]{IRIS_HOT, skin.irisMid, IRIS_DEEP}));
        g.fill(new Ellipse2D.Double(ix - ir, iy - ir, ir * 2, ir * 2));

        mangekyo(g, zoom, ix, iy, ir * 0.70);

        // Chấm phản quang: nhỏ, lệch lên phía khoé ngoài. Bỏ ở mức 1 vì một pixel trắng giữa
        // tròng đỏ trông như lỗi ảnh chứ không như ánh sáng.
        if (zoom >= 2) {
            double hr = Math.max(1, ir * 0.11);
            double hx = mirror ? ix - ir * 0.46 : ix + ir * 0.46;
            g.setColor(Color.WHITE);
            g.fill(new Ellipse2D.Double(hx - hr, iy - ir * 0.5 - hr, hr * 2, hr * 2));
        }

        g.setClip(clip);
    }

    /** Chọn hình khung mắt theo tham số, và lật ngang cho mắt bên trái. */
    private static Shape eyeShape(double x, double cy, double w, double halfH, boolean mirror) {
        if (shape.equals("glare")) {
            Shape sh = glare(x, cy, w, halfH);
            return mirror ? flipX(sh, x, w) : sh;
        }
        if (shape.equals("tron")) {
            return new Ellipse2D.Double(x + w / 2 - halfH, cy - halfH, halfH * 2, halfH * 2);
        }
        if (shape.equals("goc")) {
            return goc(x, cy, w, halfH);       // đối xứng, lật hay không cũng vậy
        }
        if (!shape.equals("manga")) {
            // Hình quả hạnh vốn đối xứng nên lật hay không cũng vậy -- trả về thẳng, khỏi đi qua
            // phép biến đổi. Không phải chuyện làm đẹp: lật rồi lật lại làm toạ độ lệch đi một
            // phần nghìn pixel, đủ để khử răng cưa ra khác, và bài kiểm "dựng lại hiệu ứng 203
            // phải trùng từng byte" sẽ đỏ.
            return lens(x, cy, w, halfH);
        }
        Shape sh = manga(x, cy, w, halfH);
        return mirror ? flipX(sh, x, w) : sh;
    }

    private static Shape flipX(Shape sh, double x, double w) {
        AffineTransform flip = AffineTransform.getTranslateInstance(x + w / 2, 0);
        flip.scale(-1, 1);
        flip.translate(-(x + w / 2), 0);
        return flip.createTransformedShape(sh);
    }

    /**
     * Hình quả hạnh: hai cung bậc hai nối hai khoé mắt.
     *
     * Cung bậc hai từ P0 tới P2 với điểm điều khiển P1 có đỉnh ở (P0 + 2*P1 + P2) / 4, nên muốn
     * đỉnh cao đúng halfH thì điểm điều khiển phải đặt ở 2*halfH -- đặt thẳng halfH là mắt chỉ hé
     * được một nửa so với ý định.
     */
    private static Shape lens(double x, double cy, double w, double halfH) {
        Path2D.Double p = new Path2D.Double();
        p.moveTo(x, cy);
        p.quadTo(x + w / 2, cy - 2 * halfH, x + w, cy);
        p.quadTo(x + w / 2, cy + 2 * halfH, x, cy);
        p.closePath();
        return p;
    }

    /**
     * Khung mắt góc cạnh: một hình lục giác dẹt, không có đoạn cong nào.
     *
     * Mí trên và mí dưới là hai đoạn THẲNG nằm ngang, nối vào hai khoé nhọn bằng bốn đoạn xiên.
     * Chỗ gãy giữa đoạn ngang và đoạn xiên chính là thứ cho ra vẻ vuông vức -- hình quả hạnh cong
     * đều nên góc nào cũng tù, nhìn mềm.
     *
     * Đoạn ngang chiếm khoảng bốn phần mười bề ngang. Rộng hơn nữa thì thành hình chữ nhật vát
     * góc, hẹp hơn thì lại gần về quả hạnh.
     */
    private static Shape goc(double x, double cy, double w, double halfH) {
        double flat = w * 0.21;               // nửa bề dài đoạn mí thẳng
        double cx = x + w / 2;
        Path2D.Double p = new Path2D.Double();
        p.moveTo(x, cy);                       // khoé trái
        p.lineTo(cx - flat, cy - halfH);
        p.lineTo(cx + flat, cy - halfH);
        p.lineTo(x + w, cy);                   // khoé phải
        p.lineTo(cx + flat, cy + halfH);
        p.lineTo(cx - flat, cy + halfH);
        p.closePath();
        return p;
    }

    /**
     * Khung mắt kiểu truyện tranh: không đối xứng, hai khoé nhọn, đuôi hất lên.
     *
     * Ba thứ tách nó khỏi hình quả hạnh:
     *   - khoé trong hạ thấp và nhọn, khoé ngoài nâng cao và vuốt thành đuôi hất;
     *   - đỉnh mí trên KHÔNG ở giữa mà lệch về phía khoé trong, nên cả con mắt như đang chồm
     *     tới -- mí trên đối xứng là thứ khiến hình quả hạnh trông hiền;
     *   - mí dưới gần thẳng, chỉ võng nhẹ, tương phản với mí trên cong mạnh.
     *
     * Mọi độ lệch dọc đều nhân với halfH, mà halfH lại nhân với độ mở, nên khung tự khép lại
     * đúng nhịp như hình quả hạnh -- phần hoạt hoạ không phải sửa gì.
     */
    private static Shape manga(double x, double cy, double w, double halfH) {
        double xi = x;                       // khoé trong
        double yi = cy + halfH * 0.16;
        double xo = x + w;                   // khoé ngoài, cao hơn khoé trong
        double yo = cy - halfH * 0.46;

        Path2D.Double p = new Path2D.Double();
        p.moveTo(xi, yi);
        // Mí trên: dựng gấp từ khoé trong, đỉnh lệch về phía trong, rồi xuôi dài ra đuôi.
        p.curveTo(x + w * 0.06, cy - halfH * 1.05,
                x + w * 0.20, cy - halfH * 1.42,
                x + w * 0.38, cy - halfH * 1.38);
        p.curveTo(x + w * 0.62, cy - halfH * 1.32,
                x + w * 0.86, cy - halfH * 0.92,
                xo, yo);
        // Mí dưới: gần thẳng, võng nhẹ, về khoé trong.
        p.curveTo(x + w * 0.80, cy + halfH * 0.30,
                x + w * 0.52, cy + halfH * 0.86,
                x + w * 0.30, cy + halfH * 0.80);
        p.curveTo(x + w * 0.18, cy + halfH * 0.76,
                x + w * 0.07, cy + halfH * 0.52,
                xi, yi);
        p.closePath();
        return p;
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

    private static String frames(int w1, int h1) {
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

    /**
     * Mở hẳn phần lớn thời gian rồi chớp một cái thật chậm.
     *
     * Client chạy chuỗi này một nhịp mỗi khung hình, nên muốn động tác chậm lại thì phải giữ mỗi
     * khung ảnh nhiều nhịp chứ không có chỗ nào chỉnh tốc độ được. STEP_HOLD là số nhịp cho mỗi
     * nấc khép/mở; HOLD_OPEN là quãng đứng yên giữa hai lần chớp.
     *
     * Chuỗi tự lặp nên hai đầu phải nối liền: kết thúc ở nấc gần mở hẳn rồi vòng về quãng mở hẳn,
     * không thì mỗi vòng lại giật một cái.
     *
     * Độ dài chuỗi cũng đi qua một byte (writeByte trong EffectData.setData) nên trần là 255.
     */
    private static String running() {
        if (shape.equals("tron")) {
            // Xoay đều, không có quãng nghỉ: mỗi khung giữ năm nhịp cho vòng quay thong thả.
            // Chèn quãng đứng yên là lộ ngay chỗ vòng lặp nối lại.
            StringBuilder sp = new StringBuilder();
            for (int i = 0; i < frameCount; i++) {
                for (int k = 0; k < 5; k++) {
                    sp.append(i == 0 && k == 0 ? "" : ",").append(i);
                }
            }
            return "[" + sp + "]";
        }
        // Ngân sách là SEQ_MAX = 126 nhịp cho TRỌN một vòng. Động tác khép rồi mở hết 45 nhịp
        // (8 nấc xuống + 7 nấc lên, mỗi nấc STEP_HOLD), nhắm hết 5, nên quãng mở hẳn còn 74 --
        // đó là mức lâu nhất giữ được động tác mượt. Muốn mở lâu hơn nữa thì phải hạ STEP_HOLD
        // xuống 2, được thêm 15 nhịp nhưng động tác khép mở giật hơn.
        final int HOLD_OPEN = 74;
        final int HOLD_SHUT = 5;
        final int STEP_HOLD = 3;
        int last = frameCount - 1;

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < HOLD_OPEN; i++) {
            sb.append(',').append(last);
        }
        for (int i = last - 1; i >= 0; i--) {
            for (int k = 0; k < STEP_HOLD; k++) {
                sb.append(',').append(i);
            }
        }
        for (int i = 0; i < HOLD_SHUT; i++) {
            sb.append(',').append(0);
        }
        for (int i = 1; i < last; i++) {
            for (int k = 0; k < STEP_HOLD; k++) {
                sb.append(',').append(i);
            }
        }

        int nhip = sb.length() - sb.toString().replace(",", "").length();
        if (nhip > SEQ_MAX) {
            throw new IllegalStateException("chuỗi phát " + nhip + " nhịp > " + SEQ_MAX
                    + ": con trỏ chạy chuỗi của client là byte, dài hơn là hiệu ứng đứng hình."
                    + " Giảm HOLD_OPEN hoặc STEP_HOLD");
        }
        return "[" + sb.substring(1) + "]";
    }
}
