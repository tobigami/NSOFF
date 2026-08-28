import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;

/**
 * Sinh các khung "sáu quả cầu quay quanh người" cho Ngọc Lục Đạo.
 *
 * Mỗi khung là một pha của cùng một quỹ đạo elip: sáu quả cách đều 60 độ, khung sau xoay thêm
 * {@code 60/SO_KHUNG} độ. Xoay đúng bằng ấy nên sau {@code SO_KHUNG} khung, quả thứ k rơi vào đúng
 * chỗ quả thứ k+1 -- vòng lặp khép kín, không có cú giật lúc quay lại khung đầu.
 *
 * Elip bẹp (bán trục ngang gấp đôi dọc) để đọc ra là vòng tròn nhìn nghiêng, chứ vẽ tròn đều thì
 * nhìn như sáu quả xếp trên một mặt phẳng dựng đứng.
 *
 * Quả ở nửa dưới (gần người xem) dùng cầu to, nửa trên dùng cầu nhỏ. Cả lớp này đều vẽ sau lưng
 * nhân vật nên không thể cho quả nào ra trước được; đổi cỡ là cách duy nhất còn lại để gợi chiều
 * sâu.
 *
 * Truyền thêm "den" thì đổi màu cầu sang đen: giữ nguyên độ sáng tối của từng điểm ảnh rồi rút
 * hết màu, sau đó ép xuống dải tối. Làm vậy quả cầu vẫn còn khối và điểm sáng, chứ tô đen phẳng
 * thì thành cái lỗ trên màn hình.
 *
 * Usage: GhepQuay <thư mục Effect> <thư mục Small> <id ảnh đầu> [den]
 */
public final class GhepQuay {

    /** Quả cầu cắt từ tấm 221: x, y, rộng, cao (cỡ 1). Nhỏ -> to. */
    private static final int[] CAU_NHO = {2, 20, 6, 6};
    private static final int[] CAU_VUA = {94, 67, 7, 9};
    private static final int[] CAU_TO = {123, 4, 9, 9};

    private static final int W = 48, H = 32;
    private static final int TAM_X = 24, TAM_Y = 15;
    private static final int BAN_NGANG = 20, BAN_DOC = 10;
    private static final int SO_CAU = 6;
    public static final int SO_KHUNG = 6;

    private GhepQuay() {
    }

    /**
     * Đổi một quả cầu sang tông đen.
     *
     * Lấy độ sáng theo công thức mắt người (lục nặng nhất), rồi ép vào dải 0..150 thay vì 0..255.
     * Giữ nguyên thứ tự sáng tối giữa các điểm nên khối cầu và điểm sáng còn nguyên; chỉ mất màu
     * và tối đi. Điểm trong suốt để nguyên trong suốt.
     */
    private static BufferedImage doiDen(BufferedImage v) {
        BufferedImage r = new BufferedImage(v.getWidth(), v.getHeight(), BufferedImage.TYPE_INT_ARGB);
        for (int x = 0; x < v.getWidth(); x++) {
            for (int y = 0; y < v.getHeight(); y++) {
                int c = v.getRGB(x, y);
                int a = (c >>> 24) & 0xff;
                if (a == 0) {
                    continue;
                }
                int sang = (int) (0.299 * ((c >> 16) & 0xff) + 0.587 * ((c >> 8) & 0xff)
                        + 0.114 * (c & 0xff));
                int m = sang * 150 / 255;
                r.setRGB(x, y, (a << 24) | (m << 16) | (m << 8) | m);
            }
        }
        return r;
    }

    public static void main(String[] args) throws Exception {
        String eff = args[0], small = args[1];
        int idDau = Integer.parseInt(args[2]);
        boolean den = args.length > 3 && "den".equals(args[3]);
        for (int z = 1; z <= 4; z++) {
            BufferedImage tam = ImageIO.read(new File(eff + "/" + z + "/221.png"));
            for (int k = 0; k < SO_KHUNG; k++) {
                BufferedImage ra = new BufferedImage(W * z, H * z, BufferedImage.TYPE_INT_ARGB);
                Graphics2D g = ra.createGraphics();
                double pha = Math.toRadians(60.0 * k / SO_KHUNG);
                for (int i = 0; i < SO_CAU; i++) {
                    double goc = pha + Math.toRadians(60.0 * i);
                    int x = (int) Math.round(TAM_X + BAN_NGANG * Math.cos(goc));
                    int y = (int) Math.round(TAM_Y + BAN_DOC * Math.sin(goc));
                    // sin > 0 là nửa dưới, tức gần người xem
                    double s = Math.sin(goc);
                    int[] c = s > 0.4 ? CAU_TO : (s < -0.4 ? CAU_NHO : CAU_VUA);
                    BufferedImage q = tam.getSubimage(c[0] * z, c[1] * z, c[2] * z, c[3] * z);
                    if (den) {
                        q = doiDen(q);
                    }
                    g.drawImage(q, (x - c[2] / 2) * z, (y - c[3] / 2) * z, null);
                }
                g.dispose();
                ImageIO.write(ra, "png", new File(small + "/" + z + "/Small" + (idDau + k) + ".png"));
                if (z == 1) {
                    System.out.println("  khung " + k + " -> Small" + (idDau + k) + ".png");
                }
            }
        }
    }
}
