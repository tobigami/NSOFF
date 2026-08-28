import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Tách một dải ảnh nhiều tư thế thành từng tệp riêng.
 *
 * Không chia đều theo số phần: công cụ vẽ hay đặt các tư thế lệch nhau, chia đều là cắt cụt tóc.
 * Ở đây dò theo cột -- cột nào không có điểm đục nào là khoảng trống giữa hai tư thế -- rồi cắt
 * đúng từng cụm, kèm cắt sát trên dưới.
 *
 * Usage: SplitSheet <ảnh vào> <thư mục ra> <tên>
 */
public final class SplitSheet {

    private static final int NGUONG = 96;

    public static void main(String[] args) throws Exception {
        BufferedImage img = ImageIO.read(new File(args[0]));
        File ra = new File(args[1]);
        String ten = args[2];

        boolean[] cot = new boolean[img.getWidth()];
        for (int x = 0; x < img.getWidth(); x++) {
            for (int y = 0; y < img.getHeight(); y++) {
                if (((img.getRGB(x, y) >>> 24) & 0xFF) >= NGUONG) {
                    cot[x] = true;
                    break;
                }
            }
        }
        List<int[]> cum = new ArrayList<>();
        int dau = -1;
        for (int x = 0; x < cot.length; x++) {
            if (cot[x] && dau < 0) {
                dau = x;
            } else if (!cot[x] && dau >= 0) {
                cum.add(new int[] { dau, x - 1 });
                dau = -1;
            }
        }
        if (dau >= 0) {
            cum.add(new int[] { dau, cot.length - 1 });
        }
        // Bỏ cụm quá hẹp: đó là vệt lẻ chứ không phải một tư thế.
        cum.removeIf(c -> c[1] - c[0] < img.getWidth() / 20);

        System.out.println("  tìm thấy " + cum.size() + " tư thế");
        for (int i = 0; i < cum.size(); i++) {
            int x1 = cum.get(i)[0], x2 = cum.get(i)[1];
            int y1 = img.getHeight(), y2 = -1;
            for (int y = 0; y < img.getHeight(); y++) {
                for (int x = x1; x <= x2; x++) {
                    if (((img.getRGB(x, y) >>> 24) & 0xFF) >= NGUONG) {
                        if (y < y1) y1 = y;
                        if (y > y2) y2 = y;
                        break;
                    }
                }
            }
            BufferedImage cat = img.getSubimage(x1, y1, x2 - x1 + 1, y2 - y1 + 1);
            File f = new File(ra, ten + (i + 1) + ".png");
            ImageIO.write(cat, "png", f);
            System.out.println("    " + f.getName() + "  " + cat.getWidth() + "x" + cat.getHeight());
        }
    }
}
