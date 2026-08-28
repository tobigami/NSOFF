import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;

/**
 * Dựng một tấm để soi icon trước khi chốt -- không phải thứ máy chủ dùng, chỉ để xem và góp ý.
 *
 * Vì sao cần: icon mức phóng 1 chỉ có 20x20, mở bằng trình xem ảnh thì nó bé bằng hạt đậu, không
 * nhìn ra được cái gì hỏng. Tấm này ghép ba cách nhìn cạnh nhau:
 *   - bản gốc to, để soi nét vẽ;
 *   - bản phóng to theo kiểu răng cưa (không làm mượt), để thấy ĐÚNG từng điểm ảnh mà máy khách
 *     sẽ vẽ ra -- đây mới là chỗ lộ ra chi tiết nào bị nát khi thu nhỏ;
 *   - bản đúng cỡ thật nằm trên nền tối giống khung đồ trong game, để ước lượng lúc chơi.
 */
public class XemIconSusano {

    private static final int[] CO = {20, 40, 60, 80};
    private static final Color NEN = new Color(0x14141A);
    private static final Color O = new Color(0x2A2A33);
    private static final Color CHU = new Color(0xC8C8D2);

    public static void main(String[] args) throws Exception {
        String goc = args.length > 0 ? args[0] : "work/server/NSO_KEM/Data/Img/Small";
        int ma = args.length > 1 ? Integer.parseInt(args[1]) : 6502;
        String ra = args.length > 2 ? args[2] : "/tmp/icon-susano-xem.png";

        BufferedImage[] ic = new BufferedImage[4];
        for (int i = 0; i < 4; i++) {
            ic[i] = ImageIO.read(new File(goc + File.separator + (i + 1), "Small" + ma + ".png"));
        }

        int le = 24, o = 168, khe = 16;
        int W = le * 2 + o * 4 + khe * 3;
        int H = le + 26 + o + 20 + 34 + 78 + le;

        BufferedImage t = new BufferedImage(W, H, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = t.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(NEN);
        g.fillRect(0, 0, W, H);

        g.setFont(new Font("SansSerif", Font.BOLD, 15));
        g.setColor(CHU);
        g.drawString("Icon 6502 - Susano Itachi   (phong to kieu rang cua: dung tung diem anh may khach ve)", le, le + 4);

        // Hàng phóng to
        int y = le + 26;
        for (int i = 0; i < 4; i++) {
            int x = le + i * (o + khe);
            g.setColor(O);
            g.fillRect(x, y, o, o);
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            g.drawImage(ic[i], x, y, o, o, null);
            g.setColor(new Color(0x44444F));
            g.drawRect(x, y, o, o);
            g.setFont(new Font("SansSerif", Font.PLAIN, 13));
            g.setColor(CHU);
            g.drawString("Muc " + (i + 1) + "  -  " + CO[i] + "x" + CO[i], x + 2, y + o + 17);
        }

        // Hàng đúng cỡ thật
        y = y + o + 20 + 26;
        g.setFont(new Font("SansSerif", Font.BOLD, 14));
        g.setColor(CHU);
        g.drawString("Co that, tren nen khung do trong game:", le, y - 8);
        int x = le;
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        for (int i = 0; i < 4; i++) {
            int pad = 8;
            g.setColor(O);
            g.fillRect(x, y, CO[i] + pad * 2, CO[i] + pad * 2);
            g.setColor(new Color(0x44444F));
            g.drawRect(x, y, CO[i] + pad * 2, CO[i] + pad * 2);
            g.drawImage(ic[i], x + pad, y + pad, null);
            x += CO[i] + pad * 2 + 22;
        }

        g.dispose();
        ImageIO.write(t, "png", new File(ra));
        System.out.println("xem tai: " + ra);
    }
}
