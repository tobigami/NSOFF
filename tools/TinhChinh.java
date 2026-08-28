import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * Tinh chỉnh sprite bằng máy: đổi vài màu theo bảng, và xoá điểm lẻ loi.
 *
 * Hai việc này làm bằng tay ở cỡ 21 pixel rất mệt và dễ không đều: đổi màu da thì phải nhớ chính
 * xác từng mã, còn điểm lẻ loi -- điểm đục mà bốn phía đều trống -- thì nhìn ở màn hình game chỉ
 * thấy lấm tấm chứ không biết nó nằm đâu.
 *
 * Usage: TinhChinh <ảnh vào> <ảnh ra> [cũ=mới ...]
 *        màu viết dạng RRGGBB. Thêm "xoaledoi" để bật xoá điểm lẻ.
 */
public final class TinhChinh {

    public static void main(String[] args) throws Exception {
        BufferedImage src = ImageIO.read(new File(args[0]));
        Map<Integer, Integer> doi = new HashMap<>();
        boolean xoaLe = false;
        for (int i = 2; i < args.length; i++) {
            if (args[i].equals("xoaledoi")) {
                xoaLe = true;
                continue;
            }
            String[] p = args[i].split("=");
            doi.put(Integer.valueOf((int) Long.parseLong(p[0], 16)),
                    Integer.valueOf((int) Long.parseLong(p[1], 16)));
        }
        int w = src.getWidth(), h = src.getHeight();
        BufferedImage ra = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        int daDoi = 0;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int c = src.getRGB(x, y);
                if (((c >>> 24) & 0xFF) < 96) {
                    continue;
                }
                Integer moi = doi.get(Integer.valueOf(c & 0xFFFFFF));
                if (moi != null) {
                    c = 0xFF000000 | moi.intValue();
                    daDoi++;
                }
                ra.setRGB(x, y, c);
            }
        }
        int daXoa = 0;
        if (xoaLe) {
            BufferedImage tam = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    int c = ra.getRGB(x, y);
                    if (((c >>> 24) & 0xFF) < 96) {
                        continue;
                    }
                    int hangXom = 0;
                    if (x > 0 && duc(ra.getRGB(x - 1, y))) hangXom++;
                    if (x < w - 1 && duc(ra.getRGB(x + 1, y))) hangXom++;
                    if (y > 0 && duc(ra.getRGB(x, y - 1))) hangXom++;
                    if (y < h - 1 && duc(ra.getRGB(x, y + 1))) hangXom++;
                    if (hangXom == 0) {
                        daXoa++;
                        continue;               // điểm lẻ loi, bỏ
                    }
                    tam.setRGB(x, y, c);
                }
            }
            ra = tam;
        }
        ImageIO.write(ra, "png", new File(args[1]));
        System.out.println("  " + new File(args[1]).getName() + ": đổi " + daDoi
                + " điểm màu, xoá " + daXoa + " điểm lẻ loi");
    }

    private static boolean duc(int c) {
        return ((c >>> 24) & 0xFF) >= 96;
    }
}
