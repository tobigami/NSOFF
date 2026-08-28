import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Thêm bốn ký tự '.', 'K', 'M', 'B' vào font chữ số của client.
 *
 * Font chữ số tra ô ảnh bằng vị trí ký tự trong một chuỗi bộ ký tự:
 *
 *     int idx = boKyTu.indexOf(chuoi.charAt(i));   // boKyTu = " 0123456789+-"
 *     if (idx == -1) idx = 0;                      // không có thì vẽ ô trống
 *
 * Nên muốn vẽ được chữ K chỉ cần ba việc khớp nhau: thêm ký tự vào chuỗi đó (làm ở công cụ khác
 * vì nằm trong bể hằng của lớp), thêm ô vào bảng kích thước font/number, và vẽ ô đó vào dải ảnh.
 * Chỗ này lo hai việc sau.
 *
 * Dải ảnh cao 97 pixel cho 13 ô, nối thêm 32 pixel thành 129 cho 4 ô mới. Riêng bản trắng vốn
 * cao 128 và có sẵn nội dung thừa sau dòng 97 -- nội dung đó không ô nào trỏ tới, nên bị đè.
 *
 * Bảng màu lấy từ chính từng file: bốn bản màu có nền đen, thân sáng, góc tối; bản trắng không có
 * nền đen mà chỉ hai sắc xám. Không ghi cứng màu nào, cứ đếm tần suất pixel mà suy ra.
 *
 * Usage: MakeNumberFont <thư mục ảnh gốc> <thư mục mod>
 */
public final class MakeNumberFont {

    /** Cao mỗi ô, và số ô cũ. */
    private static final int H = 8, CU = 13, CAO_CU = 97;
    private static final String[] MAU = { "red", "orange", "yellow", "green", "white" };

    /** Dải ảnh gốc rộng 5; nới ra 7 để chữ cái có 5 cột nét, đọc rõ hơn hẳn 3 cột. */
    private static final int RONG_ANH = 7;

    /**
     * Nét chữ trong khung 5 cột x 6 dòng, nằm lọt giữa viền. 'f' là nét, dấu chấm là nền.
     * Ô dấu chấm chỉ dùng 3 cột đầu nên phần thừa bỏ trống.
     */
    private static final Map<Character, String[]> NET = new HashMap<>();
    static {
        NET.put('K', new String[] { "f...f", "f..f.", "f.f..", "fff..", "f..f.", "f...f" });
        NET.put('M', new String[] { "f...f", "ff.ff", "f.f.f", "f...f", "f...f", "f...f" });
        NET.put('B', new String[] { "fff..", "f..f.", "fff..", "f..f.", "f..f.", "fff.." });
        NET.put('.', new String[] { ".....", ".....", ".....", ".....", ".....", ".f..." });
    }
    /** Ký tự mới, theo đúng thứ tự sẽ nối vào chuỗi bộ ký tự. */
    private static final char[] THEM = { '.', 'K', 'M', 'B' };
    /** Ô dấu chấm hẹp hơn cho khỏi thưa chữ, giống ô số 1 vốn chỉ rộng 3. */
    private static final Map<Character, Integer> RONG = new HashMap<>();
    static {
        RONG.put('.', 3);
        RONG.put('K', RONG_ANH);
        RONG.put('M', RONG_ANH);
        RONG.put('B', RONG_ANH);
    }

    public static void main(String[] args) throws Exception {
        File goc = new File(args[0]), mod = new File(args[1]);
        new File(mod, "x1/font").mkdirs();
        new File(mod, "font").mkdirs();

        for (String m : MAU) {
            BufferedImage src = ImageIO.read(new File(goc, "number_" + m + ".png"));
            int[] pal = bangMau(src);            // { nền, thân, góc }
            BufferedImage out = new BufferedImage(RONG_ANH, CAO_CU + THEM.length * H,
                    BufferedImage.TYPE_INT_ARGB);
            // Ô cũ vẫn khai báo x=0 rộng 5 nên chỉ đọc 5 cột đầu; phần nới thêm để trống.
            for (int y = 0; y < CAO_CU; y++) {
                for (int x = 0; x < RONG_ANH; x++) {
                    out.setRGB(x, y, x < src.getWidth() && y < src.getHeight() ? src.getRGB(x, y) : 0);
                }
            }
            for (int i = 0; i < THEM.length; i++) {
                ve(out, CAO_CU + i * H, THEM[i], RONG.get(THEM[i]), pal);
            }
            File f = new File(mod, "x1/font/number_" + m + ".png");
            ImageIO.write(out, "png", f);
            System.out.println("  " + f.getName() + "  " + src.getWidth() + "x" + src.getHeight()
                    + " -> " + out.getWidth() + "x" + out.getHeight());
        }

        // Bảng kích thước: giữ nguyên 13 ô cũ, nối 4 ô mới.
        byte[] cu = java.nio.file.Files.readAllBytes(new File(goc, "number").toPath());
        File bang = new File(mod, "font/number");
        try (DataOutputStream o = new DataOutputStream(new FileOutputStream(bang))) {
            o.writeShort(CU + THEM.length);
            o.write(cu, 2, CU * 8);
            for (int i = 0; i < THEM.length; i++) {
                o.writeShort(0);                       // x
                o.writeShort(CAO_CU + i * H);          // y
                o.writeShort(RONG.get(THEM[i]));       // rộng
                o.writeShort(H);                       // cao
            }
        }
        System.out.println("  font/number  " + CU + " ô -> " + (CU + THEM.length) + " ô");
    }

    /** Vẽ một ô: nền phủ kín, nét nằm ở cột 1..3 dòng 1..6, hai đầu nét dùng màu góc. */
    private static void ve(BufferedImage im, int y0, char c, int rong, int[] pal) {
        String[] net = NET.get(c);
        boolean coNen = pal[0] != 0;
        for (int y = 0; y < H; y++) {
            for (int x = 0; x < rong; x++) {
                im.setRGB(x, y0 + y, coNen ? pal[0] : 0);
            }
        }
        for (int r = 0; r < net.length; r++) {
            for (int col = 0; col < net[r].length(); col++) {
                if (net[r].charAt(col) != 'f') {
                    continue;
                }
                boolean dauNet = r == 0 || r == net.length - 1;
                im.setRGB(1 + col, y0 + 1 + r, dauNet ? pal[2] : pal[1]);
            }
        }
    }

    /**
     * Suy ra bảng màu từ chính ảnh: màu nhiều pixel nhất là nền (bản trắng không có nền nên trả
     * về 0), sáng nhất là thân nét, còn lại là màu góc.
     */
    private static int[] bangMau(BufferedImage im) {
        Map<Integer, Integer> dem = new HashMap<>();
        for (int y = 0; y < im.getHeight(); y++) {
            for (int x = 0; x < im.getWidth(); x++) {
                int p = im.getRGB(x, y);
                if ((p >>> 24) == 0) {
                    continue;
                }
                dem.merge(p, 1, Integer::sum);
            }
        }
        int nen = 0, sang = 0, toi = 0, maxDem = -1, maxSang = -1, minSang = Integer.MAX_VALUE;
        for (Map.Entry<Integer, Integer> e : dem.entrySet()) {
            int p = e.getKey(), s = ((p >> 16) & 255) + ((p >> 8) & 255) + (p & 255);
            if ((p & 0xFFFFFF) == 0) {
                nen = p;                     // đen tuyền là nền
                continue;
            }
            if (s > maxSang) { maxSang = s; sang = p; }
            if (s < minSang) { minSang = s; toi = p; }
            if (e.getValue() > maxDem) { maxDem = e.getValue(); }
        }
        if (toi == sang) {
            toi = sang;
        }
        return new int[] { nen, sang, toi };
    }
}
