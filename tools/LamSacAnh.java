import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Làm sắc lại art đã bị mịn viền, cho khớp lối vẽ của game.
 *
 * Art gốc của game là pixel sắc cạnh: viền dứt khoát, không có điểm trong suốt một phần, bảng màu
 * chỉ hơn chục tới ba chục màu. Art lấy từ nơi khác thường bị thu nhỏ bằng phép nội suy, để lại
 * quầng mờ quanh viền và bảng màu phình lên hàng trăm màu. Ở cỡ nhỏ trong game thì nó thành nhoè,
 * các mảng màu gần nhau dính vào nhau -- cánh tay lẫn vào thân chẳng hạn.
 *
 * Hai bước, đúng thứ tự:
 *
 *   1. Cắt phăng độ trong suốt: đục hơn nửa thì thành đặc hẳn, nhạt hơn thì thành trong hẳn.
 *      Quầng mờ biến mất, viền trở lại dứt khoát.
 *   2. Gom bảng màu về N màu rồi kéo mọi điểm về màu gần nhất. Mấy màu trung gian do nội suy
 *      sinh ra bị hút về màu gốc, các mảng lại tách bạch.
 *
 * Chọn bảng màu bằng phép chia hộp (median cut) chứ **không** theo tần suất. Chọn theo tần suất
 * nghe hợp lý mà sai nặng: mảng lớn nuốt hết suất, mảng nhỏ mất màu. Thử lần đầu với Madara thì
 * tóc xám chiếm diện tích lớn giành hết 24 suất, khuôn mặt biến mất sạch. Chia hộp thì mỗi lần
 * cắt hộp màu theo kênh trải rộng nhất, nên màu hiếm vẫn có phần.
 *
 * Usage: LamSacAnh <số màu giữ lại> <tệp...>
 */
public final class LamSacAnh {

    private LamSacAnh() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.out.println("dùng: LamSacAnh <số màu> <tệp...>");
            return;
        }
        int soMau = Integer.parseInt(args[0]);
        for (int i = 1; i < args.length; i++) {
            File f = new File(args[i]);
            BufferedImage im = ImageIO.read(f);
            int w = im.getWidth(), h = im.getHeight();

            // 1. cắt độ trong suốt
            int[][] px = new int[w][h];
            Map<Integer, Integer> dem = new HashMap<>();
            for (int x = 0; x < w; x++) {
                for (int y = 0; y < h; y++) {
                    int c = im.getRGB(x, y);
                    if (((c >>> 24) & 0xff) < 128) {
                        px[x][y] = 0;
                    } else {
                        int rgb = c & 0xffffff;
                        px[x][y] = 0xff000000 | rgb;
                        dem.merge(rgb, 1, Integer::sum);
                    }
                }
            }

            // 2. dựng bảng màu bằng phép chia hộp, rồi kéo mọi điểm về màu gần nhất
            int[] giu = chiaHop(new ArrayList<>(dem.keySet()), soMau);
            BufferedImage ra = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
            for (int x = 0; x < w; x++) {
                for (int y = 0; y < h; y++) {
                    if (px[x][y] == 0) {
                        continue;
                    }
                    ra.setRGB(x, y, 0xff000000 | ganNhat(px[x][y] & 0xffffff, giu));
                }
            }
            ImageIO.write(ra, "png", f);
            System.out.printf("  %-16s %d màu -> %d%n", f.getName(), dem.size(), giu.length);
        }
    }

    /**
     * Chia hộp màu: mỗi bước lấy hộp có kênh trải rộng nhất, sắp theo kênh đó rồi cắt đôi. Dừng
     * khi đủ số hộp, mỗi hộp trả về màu trung bình. Nhờ cắt theo độ trải nên một mảng màu rộng
     * chiếm nhiều hộp, còn màu hiếm nhưng khác biệt vẫn giữ được hộp riêng.
     */
    private static int[] chiaHop(List<Integer> mau, int soHop) {
        List<List<Integer>> hop = new ArrayList<>();
        hop.add(mau);
        while (hop.size() < soHop) {
            int chon = -1, traiNhat = -1, kenh = 0;
            for (int i = 0; i < hop.size(); i++) {
                List<Integer> h = hop.get(i);
                if (h.size() < 2) {
                    continue;
                }
                for (int k = 0; k < 3; k++) {
                    int lo = 255, hi = 0;
                    for (int c : h) {
                        int v = (c >> (16 - 8 * k)) & 0xff;
                        lo = Math.min(lo, v);
                        hi = Math.max(hi, v);
                    }
                    if (hi - lo > traiNhat) {
                        traiNhat = hi - lo;
                        chon = i;
                        kenh = k;
                    }
                }
            }
            if (chon < 0 || traiNhat <= 0) {
                break;
            }
            List<Integer> h = hop.remove(chon);
            final int kk = kenh;
            h.sort(Comparator.comparingInt(c -> (c >> (16 - 8 * kk)) & 0xff));
            int giua = h.size() / 2;
            hop.add(new ArrayList<>(h.subList(0, giua)));
            hop.add(new ArrayList<>(h.subList(giua, h.size())));
        }
        int[] ra = new int[hop.size()];
        for (int i = 0; i < hop.size(); i++) {
            long r = 0, g = 0, b = 0;
            for (int c : hop.get(i)) {
                r += (c >> 16) & 0xff;
                g += (c >> 8) & 0xff;
                b += c & 0xff;
            }
            int n = hop.get(i).size();
            ra[i] = (int) ((r / n) << 16 | (g / n) << 8 | (b / n));
        }
        return ra;
    }

    /** Khoảng cách trong không gian RGB, đủ dùng cho bảng màu nhỏ. */
    private static int ganNhat(int c, int[] giu) {
        int r = (c >> 16) & 0xff, g = (c >> 8) & 0xff, b = c & 0xff;
        int tot = giu[0], min = Integer.MAX_VALUE;
        for (int k : giu) {
            int dr = ((k >> 16) & 0xff) - r, dg = ((k >> 8) & 0xff) - g, db = (k & 0xff) - b;
            int d = dr * dr + dg * dg + db * db;
            if (d < min) {
                min = d;
                tot = k;
            }
        }
        return tot;
    }
}
