import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;

/**
 * Đưa một ảnh bất kỳ về đúng khung sprite của game rồi xuất đủ bốn cỡ.
 *
 * Ảnh do công cụ vẽ ra thường rất lớn (cả nghìn pixel) và có khử răng cưa, không dùng thẳng được:
 * nj_image ghi sẵn khung cắt nên sai một pixel là hình bị xén hoặc lệch khỏi cổ nhân vật.
 *
 * Ba bước:
 *   1. Cắt sát phần có hình -- ảnh sinh ra hay chừa viền trong suốt rất rộng, để nguyên thì thu
 *      nhỏ xong nhân vật chỉ còn nhúm giữa khung.
 *   2. Thu về lưới đích bằng cách lấy MÀU HAY GẶP NHẤT trong ô nguồn, không lấy trung bình.
 *      Trung bình làm mỗi biên giới màu thành một dải chuyển sắc: sprite gốc của game chỉ 21-22
 *      màu, còn bản thu bằng trung bình lên tới 133 màu, nhìn ở 21 pixel là nhoè hết. Lấy màu hay
 *      gặp nhất thì giữ nguyên mảng phẳng và biên cứng, đúng chất pixel art.
 *   3. Cắt trong suốt theo ngưỡng, vì J2ME không pha alpha nửa vời -- điểm nào mờ quá thì bỏ hẳn.
 *
 * Bốn cỡ là cùng hình nhân 1, 2, 3, 4 lần, phóng theo điểm gần nhất để giữ nét pixel.
 *
 * Usage: FitSprite <ảnh vào> <thư mục Small> <số hiệu> <rộng> <cao> [đậm] [cắt trái %]
 *        đậm: hệ số tăng tương phản, mặc định 1.15
 *        số màu: gom bảng màu về bấy nhiêu màu sau khi thu (0 là không gom). Sprite gốc của game
 *                chỉ 21-22 màu; để nguyên vài chục sắc độ na ná nhau thì nhìn ở 21 pixel là nhoè.
 *        lề trái/trên/phải/dưới (pixel): chừa trống quanh hình trong khung đích. Ảnh lấp kín
 *                    khung sẽ hiện to hơn và lệch, vì nj_part đặt vị trí theo mép khung chứ không
 *                    theo phần có hình -- lề phải khớp với ảnh gốc thì đầu mới ngồi đúng cổ.
 *        cắt trái/phải/trên/dưới %: bỏ bớt từng phía trước khi thu. Dùng để dựng khung nhìn
 *                    nghiêng từ ảnh chính diện (bỏ một bên), hoặc để lấy đúng dải mắt khi khung
 *                    đích nằm ngang mà ảnh nguồn lại dựng đứng.
 */
public final class FitSprite {

    /** Điểm nào có alpha dưới mức này thì coi như nền. */
    private static final int NGUONG_ALPHA = 96;

    public static void main(String[] args) throws Exception {
        BufferedImage src = ImageIO.read(new File(args[0]));
        File goc = new File(args[1]);
        int soHieu = Integer.parseInt(args[2]);
        int rong = Integer.parseInt(args[3]), cao = Integer.parseInt(args[4]);
        double dam = args.length > 5 ? Double.parseDouble(args[5]) : 1.15;

        BufferedImage cat = catSatHinh(src);
        cat = cat(cat, phanTram(args, 6), phanTram(args, 7), phanTram(args, 8), phanTram(args, 9));
        int lt = so(args, 10), ltr = so(args, 11), lp = so(args, 12), ld = so(args, 13);
        BufferedImage nho;
        if (lt + ltr + lp + ld == 0) {
            nho = thuNho(cat, rong, cao, dam);
        } else {
            BufferedImage trong = thuNho(cat, rong - lt - lp, cao - ltr - ld, dam);
            nho = new BufferedImage(rong, cao, BufferedImage.TYPE_INT_ARGB);
            nho.getGraphics().drawImage(trong, lt, ltr, null);
        }
        int soMau = so(args, 14);
        if (soMau > 0) {
            nho = gomMau(nho, soMau);
        }
        for (int co = 1; co <= 4; co++) {
            ImageIO.write(phong(nho, co), "png", new File(goc, co + "/Small" + soHieu + ".png"));
        }
        System.out.println("  ảnh " + soHieu + ": nguồn " + src.getWidth() + "x" + src.getHeight()
                + " -> cắt " + cat.getWidth() + "x" + cat.getHeight()
                + " -> " + rong + "x" + cao + ", đã ghi 4 cỡ");
    }

    private static int so(String[] args, int i) {
        return args.length > i ? Integer.parseInt(args[i]) : 0;
    }

    private static double phanTram(String[] args, int i) {
        return args.length > i ? Double.parseDouble(args[i]) : 0;
    }

    /** Bỏ bớt mỗi phía theo phần trăm, để lấy đúng vùng muốn giữ. */
    private static BufferedImage cat(BufferedImage img, double trai, double phai,
            double tren, double duoi) {
        int x1 = (int) (img.getWidth() * trai / 100);
        int x2 = img.getWidth() - (int) (img.getWidth() * phai / 100);
        int y1 = (int) (img.getHeight() * tren / 100);
        int y2 = img.getHeight() - (int) (img.getHeight() * duoi / 100);
        if (x2 - x1 < 4 || y2 - y1 < 4) {
            return img;
        }
        return img.getSubimage(x1, y1, x2 - x1, y2 - y1);
    }

    /** Bỏ viền trong suốt quanh hình. */
    private static BufferedImage catSatHinh(BufferedImage img) {
        int x1 = img.getWidth(), y1 = img.getHeight(), x2 = -1, y2 = -1;
        for (int y = 0; y < img.getHeight(); y++) {
            for (int x = 0; x < img.getWidth(); x++) {
                if (((img.getRGB(x, y) >>> 24) & 0xFF) >= NGUONG_ALPHA) {
                    if (x < x1) x1 = x;
                    if (y < y1) y1 = y;
                    if (x > x2) x2 = x;
                    if (y > y2) y2 = y;
                }
            }
        }
        if (x2 < 0) {
            return img;
        }
        return img.getSubimage(x1, y1, x2 - x1 + 1, y2 - y1 + 1);
    }

    /** Thu nhỏ bằng cách lấy màu hay gặp nhất trong từng ô, giữ mảng phẳng và biên cứng. */
    private static BufferedImage thuNho(BufferedImage src, int rong, int cao, double dam) {
        BufferedImage ra = new BufferedImage(rong, cao, BufferedImage.TYPE_INT_ARGB);
        double bx = (double) src.getWidth() / rong, by = (double) src.getHeight() / cao;
        for (int y = 0; y < cao; y++) {
            for (int x = 0; x < rong; x++) {
                int x1 = (int) (x * bx), x2 = Math.max(x1 + 1, (int) ((x + 1) * bx));
                int y1 = (int) (y * by), y2 = Math.max(y1 + 1, (int) ((y + 1) * by));
                java.util.HashMap<Integer, Integer> dem = new java.util.HashMap<>();
                int duc = 0, tong = 0;
                for (int j = y1; j < Math.min(y2, src.getHeight()); j++) {
                    for (int i = x1; i < Math.min(x2, src.getWidth()); i++) {
                        int c = src.getRGB(i, j);
                        tong++;
                        if (((c >>> 24) & 0xFF) < NGUONG_ALPHA) {
                            continue;
                        }
                        duc++;
                        int mau = c & 0xFFFFFF;
                        dem.merge(Integer.valueOf(mau), Integer.valueOf(1), Integer::sum);
                    }
                }
                // Ô nào quá nửa là nền thì để trống, tránh viền hình dày thêm một vành.
                if (tong == 0 || duc * 2 < tong || dem.isEmpty()) {
                    continue;
                }
                int mauNhat = 0, lanNhat = -1;
                for (java.util.Map.Entry<Integer, Integer> e : dem.entrySet()) {
                    if (e.getValue().intValue() > lanNhat) {
                        lanNhat = e.getValue().intValue();
                        mauNhat = e.getKey().intValue();
                    }
                }
                int r = nen((mauNhat >> 16) & 0xFF, dam);
                int g = nen((mauNhat >> 8) & 0xFF, dam);
                int b = nen(mauNhat & 0xFF, dam);
                ra.setRGB(x, y, 0xFF000000 | (r << 16) | (g << 8) | b);
            }
        }
        return ra;
    }

    /** Kéo giãn quanh mức giữa để hình đỡ bệt sau khi thu nhỏ. */
    private static int nen(int v, double dam) {
        double k = 128 + (v - 128) * dam;
        return (int) Math.max(0, Math.min(255, k));
    }

    /**
     * Gom bảng màu về n màu bằng cách cắt hộp theo trung vị (median cut).
     *
     * Chia khối màu theo chiều nào trải rộng nhất, cắt đôi ở trung vị, lặp tới khi đủ số hộp; mỗi
     * hộp lấy màu trung bình làm đại diện. Cách này giữ được mấy mảng nhỏ mà nổi -- như con mắt đỏ
     * -- thay vì nuốt chúng vào màu nền như khi làm tròn màu đều tay.
     */
    private static BufferedImage gomMau(BufferedImage img, int n) {
        java.util.List<int[]> diem = new java.util.ArrayList<>();
        for (int y = 0; y < img.getHeight(); y++) {
            for (int x = 0; x < img.getWidth(); x++) {
                int c = img.getRGB(x, y);
                if (((c >>> 24) & 0xFF) >= NGUONG_ALPHA) {
                    diem.add(new int[] { (c >> 16) & 0xFF, (c >> 8) & 0xFF, c & 0xFF });
                }
            }
        }
        if (diem.isEmpty()) {
            return img;
        }
        java.util.List<java.util.List<int[]>> hop = new java.util.ArrayList<>();
        hop.add(diem);
        while (hop.size() < n) {
            java.util.List<int[]> to = null;
            int rong = -1, chieu = 0;
            for (java.util.List<int[]> h : hop) {
                if (h.size() < 2) {
                    continue;
                }
                for (int k = 0; k < 3; k++) {
                    int min = 255, max = 0;
                    for (int[] p : h) {
                        min = Math.min(min, p[k]);
                        max = Math.max(max, p[k]);
                    }
                    if (max - min > rong) {
                        rong = max - min;
                        to = h;
                        chieu = k;
                    }
                }
            }
            if (to == null || rong <= 0) {
                break;
            }
            final int ch = chieu;
            to.sort((a, b) -> a[ch] - b[ch]);
            java.util.List<int[]> nua = new java.util.ArrayList<>(to.subList(to.size() / 2, to.size()));
            to.subList(to.size() / 2, to.size()).clear();
            hop.add(nua);
        }
        int[][] bang = new int[hop.size()][3];
        for (int i = 0; i < hop.size(); i++) {
            long r = 0, g = 0, b = 0;
            for (int[] p : hop.get(i)) {
                r += p[0];
                g += p[1];
                b += p[2];
            }
            int m = Math.max(1, hop.get(i).size());
            bang[i] = new int[] { (int) (r / m), (int) (g / m), (int) (b / m) };
        }
        BufferedImage ra = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < img.getHeight(); y++) {
            for (int x = 0; x < img.getWidth(); x++) {
                int c = img.getRGB(x, y);
                if (((c >>> 24) & 0xFF) < NGUONG_ALPHA) {
                    continue;
                }
                int r = (c >> 16) & 0xFF, g = (c >> 8) & 0xFF, b = c & 0xFF;
                int tot = 0, gan = Integer.MAX_VALUE;
                for (int i = 0; i < bang.length; i++) {
                    int d = (r - bang[i][0]) * (r - bang[i][0]) + (g - bang[i][1]) * (g - bang[i][1])
                            + (b - bang[i][2]) * (b - bang[i][2]);
                    if (d < gan) {
                        gan = d;
                        tot = i;
                    }
                }
                ra.setRGB(x, y, 0xFF000000 | (bang[tot][0] << 16) | (bang[tot][1] << 8) | bang[tot][2]);
            }
        }
        return ra;
    }

    private static BufferedImage phong(BufferedImage src, int lan) {
        BufferedImage ra = new BufferedImage(src.getWidth() * lan, src.getHeight() * lan,
                BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < ra.getHeight(); y++) {
            for (int x = 0; x < ra.getWidth(); x++) {
                ra.setRGB(x, y, src.getRGB(x / lan, y / lan));
            }
        }
        return ra;
    }
}
