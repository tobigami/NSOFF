import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;

/**
 * Vẽ lại phần tóc của sprite đầu, giữ nguyên khuôn mặt.
 *
 * Ở lưới 21x22 thì tóc là thứ quyết định nhìn ra nhân vật nào, mà sửa tay từng điểm rất khó đều.
 * Ở đây tóc được mô tả bằng bản đồ ký tự cho dễ đọc và dễ sửa:
 *
 *   H  tóc, tông đậm        S  vệt sáng trên tóc
 *   h  tóc, tông vừa        .  giữ nguyên điểm cũ (mặt, băng trán)
 *   x  xoá thành trong suốt (cắt bớt viền tóc cũ)
 *
 * Ảnh nền và bản đồ phải cùng cỡ; lệch một dòng là tóc trượt khỏi đầu.
 *
 * Usage: VeToc <ảnh vào> <ảnh ra>
 */
public final class VeToc {

    /** Tóc dài rẽ ngôi giữa, hai lọn buông dài hai bên má. 21 cột x 22 dòng. */
    private static final String[] TOC = {
        "xxxxxxHHxHHHxxHxxxxxx",
        "xxxxHHHHHHHHHHHHhxxxx",
        "xxxHHHHHSHHHHHHHHHxxx",
        "xxHHHHHHSHHHHSHHHHHxx",
        "xHHHHhHHSHHHHSHHHhHHx",
        "xHHHHhHHSHHHHSHHHhHHH",
        "HHHHHhHHSHHHHSHHHhHHH",
        "HHHHhhHHSHHHHSHHhhHHH",
        "HHHHh.HHSHHHHSHH.hHHH",
        "HHHHh.........hHHHHHH",
        "HHHh...........hHHHHH",
        "HHHh............hHHHH",
        "HHh..............hHHH",
        "HHh..............hHHH",
        "HHh...............HHH",
        "HHh...............HHH",
        "HHH...............HHH",
        "xHH...............HHH",
        "xHH...............HHx",
        "xHh...............Hxx",
        "xxH...............xxx",
        "xxH...............xxx"
    };

    private static final int DAM = 0xFF14141C, VUA = 0xFF1E1C26, SANG = 0xFF322F3D;

    public static void main(String[] args) throws Exception {
        BufferedImage src = ImageIO.read(new File(args[0]));
        int w = src.getWidth(), h = src.getHeight();
        if (TOC.length != h) {
            System.out.println("!! bản đồ " + TOC.length + " dòng, ảnh " + h + " dòng");
            return;
        }
        BufferedImage ra = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < h; y++) {
            if (TOC[y].length() != w) {
                System.out.println("!! dòng " + y + " dài " + TOC[y].length() + ", cần " + w);
                return;
            }
            for (int x = 0; x < w; x++) {
                char k = TOC[y].charAt(x);
                switch (k) {
                    case 'H': ra.setRGB(x, y, DAM); break;
                    case 'h': ra.setRGB(x, y, VUA); break;
                    case 'S': ra.setRGB(x, y, SANG); break;
                    case 'x': break;                       // để trong suốt
                    default:  ra.setRGB(x, y, src.getRGB(x, y)); break;
                }
            }
        }
        ImageIO.write(ra, "png", new File(args[1]));
        System.out.println("  đã vẽ lại tóc -> " + new File(args[1]).getName());
    }
}
