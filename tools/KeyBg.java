import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Bóc nền của ảnh không có kênh trong suốt.
 *
 * Công cụ vẽ nhiều khi vẽ luôn cả ô ca-rô giả trong suốt thành pixel thật, hoặc trả về nền trắng
 * hay nền tối. Màu nền được lấy TỪ BỐN GÓC ẢNH chứ không đoán trước là sáng hay tối -- ảnh nền
 * tối mà đoán nhầm là sáng thì bóc xong mất luôn phần tóc đen.
 * Ở đây loang từ mép ảnh vào, chỉ xoá những điểm nối liền với mép và có màu gần màu nền -- nhờ vậy
 * mấy mảng sáng NẰM TRONG hình (tấm kim loại trên băng trán, tròng mắt) không bị xoá lây, thứ mà
 * lọc theo màu toàn ảnh chắc chắn làm hỏng.
 *
 * Usage: KeyBg <ảnh vào> <ảnh ra> [dung sai]
 */
public final class KeyBg {

    public static void main(String[] args) throws Exception {
        BufferedImage src = ImageIO.read(new File(args[0]));
        int dungSai = args.length > 2 ? Integer.parseInt(args[2]) : 24;
        int w = src.getWidth(), h = src.getHeight();

        BufferedImage ra = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                ra.setRGB(x, y, 0xFF000000 | (src.getRGB(x, y) & 0xFFFFFF));
            }
        }

        int[][] nen = tongNen(src, dungSai);
        System.out.print("  màu nền nhận ra:");
        for (int[] n : nen) {
            System.out.printf(" #%02X%02X%02X", n[0], n[1], n[2]);
        }
        System.out.println();

        boolean[] xong = new boolean[w * h];
        Deque<int[]> hang = new ArrayDeque<>();
        for (int x = 0; x < w; x++) {
            hang.add(new int[] { x, 0 });
            hang.add(new int[] { x, h - 1 });
        }
        for (int y = 0; y < h; y++) {
            hang.add(new int[] { 0, y });
            hang.add(new int[] { w - 1, y });
        }

        int xoa = 0;
        while (!hang.isEmpty()) {
            int[] p = hang.poll();
            int x = p[0], y = p[1];
            if (x < 0 || y < 0 || x >= w || y >= h || xong[y * w + x]) {
                continue;
            }
            xong[y * w + x] = true;
            if (!laNen(src.getRGB(x, y), nen, dungSai)) {
                continue;
            }
            ra.setRGB(x, y, 0x00000000);
            xoa++;
            hang.add(new int[] { x + 1, y });
            hang.add(new int[] { x - 1, y });
            hang.add(new int[] { x, y + 1 });
            hang.add(new int[] { x, y - 1 });
        }
        ImageIO.write(ra, "png", new File(args[1]));
        System.out.printf("  %s: xoá %d điểm nền (%.1f%% ảnh)%n",
                new File(args[1]).getName(), xoa, xoa * 100.0 / (w * h));
    }

    /** Các tông màu nền, lấy từ bốn góc: ô ca-rô có hai tông nên góc nào cũng là một trong hai. */
    private static boolean laNen(int c, int[][] nen, int dungSai) {
        int[] m = mau(c);
        for (int[] n : nen) {
            if (gan(m, n, dungSai)) {
                return true;
            }
        }
        return false;
    }

    private static int[][] tongNen(BufferedImage img, int dungSai) {
        int w = img.getWidth(), h = img.getHeight();
        int[][] goc = {
            mau(img.getRGB(0, 0)), mau(img.getRGB(w - 1, 0)),
            mau(img.getRGB(0, h - 1)), mau(img.getRGB(w - 1, h - 1)),
            mau(img.getRGB(w / 2, 0)), mau(img.getRGB(0, h / 2)),
        };
        java.util.List<int[]> ra = new java.util.ArrayList<>();
        for (int[] g : goc) {
            boolean co = false;
            for (int[] r : ra) {
                if (gan(g, r, dungSai)) {
                    co = true;
                    break;
                }
            }
            if (!co) {
                ra.add(g);
            }
        }
        return ra.toArray(new int[0][]);
    }

    private static int[] mau(int c) {
        return new int[] { (c >> 16) & 0xFF, (c >> 8) & 0xFF, c & 0xFF };
    }

    private static boolean gan(int[] a, int[] b, int dungSai) {
        return Math.abs(a[0] - b[0]) <= dungSai && Math.abs(a[1] - b[1]) <= dungSai
                && Math.abs(a[2] - b[2]) <= dungSai;
    }

}
