import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.io.File;

/**
 * Vẽ icon cho các bí kíp danh hiệu, xuất đủ bốn mức phóng của game.
 *
 * Vì sao phải tự vẽ: 27 danh hiệu đang dùng chung đúng hai icon cuộn giấy, mở túi ra không phân
 * biệt nổi cái nào với cái nào. Trong game không có sẵn hình cho mấy khái niệm này (Bát Môn,
 * Luân Hồi Nhãn, Cửu Vĩ...), mà máy không có mạng nên cũng không tải về được.
 *
 * Cách làm: vẽ ở 72px với khử răng cưa rồi thu xuống 54/36/18 -- đúng bốn cỡ mà game dùng
 * (Data/Img/Small/{1,2,3,4}/SmallNNNNN.png, tỉ lệ 1:2:3:4 của bản 18px). Vẽ to rồi thu nhỏ cho
 * nét mềm hơn hẳn so với vẽ thẳng ở 18px.
 *
 * Mỗi icon = một khung nền bo góc theo tông màu riêng + một hình khối đơn giản, đậm nét. Ở 18px
 * thì chi tiết nhỏ biến mất hết, nên hình nào cũng phải đọc được chỉ bằng bóng và màu.
 *
 *   javac -d build/tools tools/VeIconDanhHieu.java
 *   java -cp build/tools VeIconDanhHieu work/server/NSO_KEM/Data/Img/Small
 */
public class VeIconDanhHieu {

    /** {mã icon, hình, màu nền 1, màu nền 2, màu hình} */
    private static final Object[][] DS = {
        {26121, "mat3",    0x7A1220, 0xC02234, 0xFFE9B0},  // Uchiha -- mắt ba tomoe
        {26122, "vong",    0x1B3A6B, 0x3D74C4, 0xE8F1FF},  // Luân Hồi Nhãn -- mắt vòng tròn
        {26123, "duoi",    0x8A3A08, 0xE07B1E, 0xFFE2A8},  // Cửu Vĩ -- đuôi cáo
        {26124, "cong",    0x6B2A00, 0xD86A12, 0xFFD98A},  // Bát Môn -- cổng lửa
        {26125, "khien",   0x2A2F6B, 0x5866C4, 0xE6EBFF},  // Susanoo -- khiên chakra
        {26126, "la",      0x14532D, 0x2E9E52, 0xE7FFE9},  // Senju -- lá cây
        {26127, "kiem2",   0x39424D, 0x78899C, 0xFFFFFF},  // Kiếm Hào -- hai kiếm bắt chéo
        {26128, "matna",   0x1F2430, 0x49536B, 0xD6DEF0},  // Ám Bộ -- mặt nạ
        {26129, "bong",    0x0E3A52, 0x2E86B5, 0xE4F7FF},  // Băng Độn -- bông tuyết
        {26130, "xoay",    0x0F4C3A, 0x2FA07A, 0xE9FFF6},  // Tiên Nhân -- xoáy tự nhiên
        {26131, "thu",     0x5A1E5E, 0xA23BA8, 0xFFE6FF},  // Nhân Trụ Lực -- dấu phong ấn
        {26132, "cuon",    0x4A3A12, 0x9A7A2A, 0xFFF3CC},  // Tam Nhẫn -- cuộn thư
        {26133, "saoden",  0x2B2B2B, 0x5E5E5E, 0xFF6B6B},  // Nhẫn Giả Đào Vong -- vạch xoá làng
        {26134, "vuong",   0x6B5310, 0xD8B02A, 0xFFF6D0},  // Lục Đạo -- vương miện
        {26135, "sao1",    0x6B5310, 0xE0BE38, 0xFFFBE6},  // Thiên bảng nhất
        {26136, "sao2",    0x4A4A52, 0x9AA0AD, 0xFFFFFF},  // Thiên bảng nhị
        {26137, "sao3",    0x5A3A18, 0xB07A38, 0xFFEEDC},  // Thiên bảng tam
        {26138, "sao4",    0x2A4256, 0x5A7E9C, 0xEAF4FF},  // Thiên bảng tứ
        {26139, "sao5",    0x333B33, 0x6E7A6E, 0xF0F5F0},  // Thiên bảng ngũ
        {26140, "co",      0x5B1414, 0xB02A2A, 0xFFE0E0},  // Trùm Phái -- cờ hiệu
        {26141, "sach",    0x123A5B, 0x2E7CB5, 0xE8F6FF},  // Trùm Trường -- sách
        {26142, "mat1",    0x5B1414, 0xC03030, 0xFFE9B0},  // Mangekyo -- mắt hoa văn
        {26143, "may",     0x4A0E14, 0xA01E28, 0xFFFFFF},  // Akatsuki -- mây đỏ
    };

    public static void main(String[] args) throws Exception {
        String goc = args.length > 0 ? args[0] : "work/server/NSO_KEM/Data/Img/Small";
        for (Object[] d : DS) {
            int ma = (Integer) d[0];
            BufferedImage to = ve((String) d[1], (Integer) d[2], (Integer) d[3], (Integer) d[4]);
            for (int z = 1; z <= 4; z++) {
                int canh = 18 * z;
                BufferedImage ra = thuNho(to, canh);
                File f = new File(goc + File.separator + z, "Small" + ma + ".png");
                f.getParentFile().mkdirs();
                ImageIO.write(ra, "png", f);
            }
            System.out.println("  " + ma + "  " + d[1]);
        }
        System.out.println("xong " + DS.length + " icon x 4 mức phóng");
    }

    /** Thu nhỏ hai bước cho nét đỡ vỡ: 72 -> cạnh đích bằng phép nội suy song tuyến. */
    private static BufferedImage thuNho(BufferedImage to, int canh) {
        BufferedImage ra = new BufferedImage(canh, canh, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = ra.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.drawImage(to, 0, 0, canh, canh, null);
        g.dispose();
        return ra;
    }

    private static BufferedImage ve(String hinh, int nen1, int nen2, int mau) {
        int S = 72;
        BufferedImage img = new BufferedImage(S, S, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

        // Khung nền: bo góc, dốc màu từ trên xuống, viền tối để nổi trên nền hành trang.
        Shape khung = new RoundRectangle2D.Float(3, 3, S - 6, S - 6, 16, 16);
        g.setPaint(new GradientPaint(0, 0, new Color(nen2), 0, S, new Color(nen1)));
        g.fill(khung);
        g.setColor(new Color(0, 0, 0, 150));
        g.setStroke(new BasicStroke(3f));
        g.draw(khung);
        // Vệt sáng phía trên cho có khối.
        g.setPaint(new Color(255, 255, 255, 40));
        g.fill(new RoundRectangle2D.Float(6, 6, S - 12, 22, 12, 12));

        g.setColor(new Color(mau));
        Color toi = new Color(0, 0, 0, 110);
        veHinh(g, hinh, S, new Color(mau), toi);
        g.dispose();
        return img;
    }

    /**
     * Vẽ riêng hoạ tiết ra một tấm trong suốt, để công cụ biểu ngữ mượn lại.
     *
     * Cùng một bộ hình cho icon trong túi và huy hiệu trên biểu ngữ -- hai nơi vẽ hai kiểu thì
     * người chơi không nối được món trong túi với dòng chữ trên đầu.
     */
    public static BufferedImage veMotif(String hinh, int mau, int S) {
        BufferedImage img = new BufferedImage(S, S, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.scale(S / 72.0, S / 72.0);
        g.setColor(new Color(mau));
        veHinh(g, hinh, 72, new Color(mau), new Color(0, 0, 0, 110));
        g.dispose();
        return img;
    }

    private static void veHinh(Graphics2D g, String hinh, int S, Color mau, Color toi) {
        float c = S / 2f;
        switch (hinh) {
            case "mat3":
            case "mat1": {
                g.setColor(toi);
                g.fill(new Ellipse2D.Float(14, 22, S - 28, S - 44));
                g.setColor(mau);
                g.fill(new Ellipse2D.Float(16, 24, S - 32, S - 48));
                g.setColor(new Color(0x1A0000));
                g.fill(new Ellipse2D.Float(c - 9, c - 9, 18, 18));
                g.setColor(mau);
                if (hinh.equals("mat3")) {
                    for (int i = 0; i < 3; i++) {
                        double a = Math.toRadians(90 + i * 120);
                        g.fill(new Ellipse2D.Double(c + Math.cos(a) * 13 - 3.5,
                                c + Math.sin(a) * 13 - 3.5, 7, 7));
                    }
                } else {
                    g.setStroke(new BasicStroke(3.5f));
                    for (int i = 0; i < 6; i++) {
                        double a = Math.toRadians(i * 60);
                        g.draw(new Line2D.Double(c, c, c + Math.cos(a) * 15, c + Math.sin(a) * 15));
                    }
                }
                break;
            }
            case "vong": {
                g.setStroke(new BasicStroke(3.2f));
                for (int r = 8; r <= 26; r += 6) {
                    g.draw(new Ellipse2D.Float(c - r, c - r, r * 2, r * 2));
                }
                g.fill(new Ellipse2D.Float(c - 4, c - 4, 8, 8));
                break;
            }
            case "duoi": {
                // Ba cái đuôi xoè từ gốc dưới bên trái, mỗi cái cong theo một hướng và thon dần --
                // vẽ bằng đa giác chứ không phải nét kẻ, nét kẻ đều bề ngang trông như mũi tên.
                for (int i = 0; i < 3; i++) {
                    double a = Math.toRadians(-108 + i * 34);
                    double dai = 30 + i * 2;
                    double gx = 20, gy = S - 16;
                    double nx = gx + Math.cos(a) * dai, ny = gy + Math.sin(a) * dai;
                    double px = Math.cos(a + Math.PI / 2), py = Math.sin(a + Math.PI / 2);
                    Path2D p = new Path2D.Double();
                    p.moveTo(gx + px * 7, gy + py * 7);
                    p.quadTo(gx + Math.cos(a) * dai * 0.55 + px * 11,
                             gy + Math.sin(a) * dai * 0.55 + py * 11, nx, ny);
                    p.quadTo(gx + Math.cos(a) * dai * 0.55 - px * 4,
                             gy + Math.sin(a) * dai * 0.55 - py * 4, gx - px * 7, gy - py * 7);
                    p.closePath();
                    g.fill(p);
                }
                break;
            }
            case "cong": {
                g.setStroke(new BasicStroke(6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.draw(new Line2D.Float(16, 20, S - 16, 20));
                g.draw(new Line2D.Float(21, 20, 21, S - 16));
                g.draw(new Line2D.Float(S - 21, 20, S - 21, S - 16));
                g.setStroke(new BasicStroke(4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.draw(new Line2D.Float(18, 32, S - 18, 32));
                break;
            }
            case "khien": {
                Path2D p = new Path2D.Float();
                p.moveTo(c, 14);
                p.lineTo(S - 16, 24);
                p.lineTo(S - 20, c + 10);
                p.lineTo(c, S - 14);
                p.lineTo(20, c + 10);
                p.lineTo(16, 24);
                p.closePath();
                g.setColor(toi);
                g.fill(p);
                g.setColor(mau);
                g.setStroke(new BasicStroke(4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.draw(p);
                break;
            }
            case "la": {
                Path2D p = new Path2D.Float();
                p.moveTo(c, 14);
                p.quadTo(S - 14, c - 6, c, S - 14);
                p.quadTo(14, c - 6, c, 14);
                p.closePath();
                g.fill(p);
                g.setColor(toi);
                g.setStroke(new BasicStroke(3f));
                g.draw(new Line2D.Float(c, 18, c, S - 18));
                break;
            }
            case "kiem2": {
                g.setStroke(new BasicStroke(6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.draw(new Line2D.Float(18, S - 18, S - 18, 18));
                g.draw(new Line2D.Float(18, 18, S - 18, S - 18));
                g.setColor(toi);
                g.setStroke(new BasicStroke(4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.draw(new Line2D.Float(24, S - 24, 30, S - 30));
                g.draw(new Line2D.Float(S - 24, S - 24, S - 30, S - 30));
                break;
            }
            case "matna": {
                // Mặt nạ Anbu: trán rộng, cằm nhọn, hai khe mắt xếch. Khe mắt phải là hình thoi
                // chứ không phải bầu dục -- ở 18px bầu dục nhoè thành hai chấm tròn vô hồn.
                Path2D p = new Path2D.Float();
                p.moveTo(c, 16);
                p.lineTo(S - 18, 26);
                p.lineTo(S - 22, c + 8);
                p.lineTo(c, S - 15);
                p.lineTo(22, c + 8);
                p.lineTo(18, 26);
                p.closePath();
                g.fill(p);
                g.setColor(toi);
                for (int i = -1; i <= 1; i += 2) {
                    Path2D m = new Path2D.Double();
                    m.moveTo(c + i * 8, 36);
                    m.lineTo(c + i * 22, 32);
                    m.lineTo(c + i * 20, 42);
                    m.lineTo(c + i * 8, 42);
                    m.closePath();
                    g.fill(m);
                }
                g.setStroke(new BasicStroke(3.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.draw(new Line2D.Float(c - 10, 52, c + 10, 52));
                break;
            }
            case "bong": {
                g.setStroke(new BasicStroke(4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                for (int i = 0; i < 6; i++) {
                    double a = Math.toRadians(i * 60);
                    double x = c + Math.cos(a) * 24, y = c + Math.sin(a) * 24;
                    g.draw(new Line2D.Double(c, c, x, y));
                    double b1 = a + Math.toRadians(35), b2 = a - Math.toRadians(35);
                    double mx = c + Math.cos(a) * 15, my = c + Math.sin(a) * 15;
                    g.draw(new Line2D.Double(mx, my, mx + Math.cos(b1) * 9, my + Math.sin(b1) * 9));
                    g.draw(new Line2D.Double(mx, my, mx + Math.cos(b2) * 9, my + Math.sin(b2) * 9));
                }
                break;
            }
            case "xoay": {
                g.setStroke(new BasicStroke(5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                Path2D p = new Path2D.Float();
                for (double t = 0; t < Math.PI * 3; t += 0.15) {
                    double r = 4 + t * 3.1;
                    double x = c + Math.cos(t) * r, y = c + Math.sin(t) * r;
                    if (t == 0) {
                        p.moveTo(x, y);
                    } else {
                        p.lineTo(x, y);
                    }
                }
                g.draw(p);
                break;
            }
            case "thu": {
                g.setStroke(new BasicStroke(4.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.draw(new Ellipse2D.Float(c - 22, c - 22, 44, 44));
                for (int i = 0; i < 4; i++) {
                    double a = Math.toRadians(45 + i * 90);
                    g.draw(new Line2D.Double(c + Math.cos(a) * 8, c + Math.sin(a) * 8,
                            c + Math.cos(a) * 22, c + Math.sin(a) * 22));
                }
                g.fill(new Ellipse2D.Float(c - 6, c - 6, 12, 12));
                break;
            }
            case "cuon":
            case "sach": {
                g.fill(new RoundRectangle2D.Float(16, 18, S - 32, S - 36, 6, 6));
                g.setColor(toi);
                g.setStroke(new BasicStroke(3f));
                for (int i = 0; i < 3; i++) {
                    g.draw(new Line2D.Float(24, 30 + i * 9, S - 24, 30 + i * 9));
                }
                if (hinh.equals("cuon")) {
                    g.setColor(mau);
                    g.fill(new RoundRectangle2D.Float(12, 14, S - 24, 8, 8, 8));
                    g.fill(new RoundRectangle2D.Float(12, S - 22, S - 24, 8, 8, 8));
                }
                break;
            }
            case "saoden": {
                g.setStroke(new BasicStroke(5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.setColor(new Color(0xE8E8E8));
                g.draw(new Ellipse2D.Float(c - 20, c - 20, 40, 40));
                g.setColor(mau);
                g.setStroke(new BasicStroke(6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.draw(new Line2D.Float(c - 22, c - 22, c + 22, c + 22));
                break;
            }
            case "vuong": {
                Path2D p = new Path2D.Float();
                p.moveTo(16, S - 22);
                p.lineTo(20, 24);
                p.lineTo(c - 8, c - 2);
                p.lineTo(c, 18);
                p.lineTo(c + 8, c - 2);
                p.lineTo(S - 20, 24);
                p.lineTo(S - 16, S - 22);
                p.closePath();
                g.fill(p);
                g.setColor(toi);
                g.fill(new RoundRectangle2D.Float(16, S - 24, S - 32, 8, 4, 4));
                break;
            }
            case "co": {
                g.setStroke(new BasicStroke(5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.draw(new Line2D.Float(22, 14, 22, S - 14));
                Path2D p = new Path2D.Float();
                p.moveTo(24, 18);
                p.lineTo(S - 16, 26);
                p.lineTo(24, 40);
                p.closePath();
                g.fill(p);
                break;
            }
            case "may": {
                Path2D p = new Path2D.Float();
                p.moveTo(c - 4, S - 22);
                p.curveTo(c - 30, S - 30, c - 22, 20, c, 24);
                p.curveTo(c + 20, 18, c + 28, S - 32, c + 4, S - 22);
                p.closePath();
                g.fill(p);
                g.setColor(toi);
                g.setStroke(new BasicStroke(3.5f));
                g.draw(p);
                break;
            }
            default: {  // sao1..sao5
                int canh = hinh.startsWith("sao") ? Integer.parseInt(hinh.substring(3)) : 5;
                Path2D p = new Path2D.Float();
                for (int i = 0; i < 10; i++) {
                    double a = Math.toRadians(-90 + i * 36);
                    double r = (i % 2 == 0) ? 26 : 11;
                    double x = c + Math.cos(a) * r, y = c + Math.sin(a) * r;
                    if (i == 0) {
                        p.moveTo(x, y);
                    } else {
                        p.lineTo(x, y);
                    }
                }
                p.closePath();
                g.fill(p);
                g.setColor(toi);
                g.setFont(new Font("SansSerif", Font.BOLD, 26));
                String s = String.valueOf(canh);
                FontMetrics fm = g.getFontMetrics();
                g.drawString(s, c - fm.stringWidth(s) / 2f, c + fm.getAscent() / 2f - 3);
                break;
            }
        }
    }
}
