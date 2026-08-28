import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;

/**
 * Ghép ảnh áo choàng "Ngọc Lục Đạo" từ mấy quả cầu cắt ra khỏi tấm hiệu ứng 221.
 *
 * Ô áo choàng vẽ **phía sau** nhân vật, nên sáu quả cầu chia hai bên -- ba trái ba phải, chừa
 * khoảng giữa cho người. Mỗi khung dùng bộ cầu khác nhau và xê dịch vài điểm ảnh, để ba khung nối
 * lại thành vòng lơ lửng chứ không đứng im.
 *
 * Toạ độ ghi theo cỡ 1; mấy cỡ còn lại nhân lên vì tấm hiệu ứng có sẵn cả bốn mức.
 *
 * Usage: GhepCau <thư mục Effect> <thư mục Small> <id ảnh đầu>
 */
public final class GhepCau {

    /** Quả cầu cắt từ tấm 221: x, y, rộng, cao (theo cỡ 1). */
    private static final int[][] CAU = {
            {2, 20, 6, 6},        // 0 cầu trơn nhỏ
            {94, 67, 7, 9},       // 1 cầu dài
            {123, 4, 9, 9},       // 2 cầu to
            {101, 30, 14, 10},    // 3 cầu kèm tia ngang
            {0, 29, 11, 13},      // 4 cầu kèm tia dọc
            {122, 79, 7, 9},      // 5 cầu dài, dáng khác
    };

    /** Khung ảnh, theo cỡ 1. Rộng chừa giữa cho nhân vật (~21 điểm ảnh). */
    private static final int W = 48, H = 32;

    /** Bên trái lệch xuống bấy nhiêu điểm ảnh, mỗi khung một mức, để phá thế đối xứng cứng. */
    private static final int[] LECH_TRAI = {2, -1, 3};

    /**
     * Ba khung. Mỗi khung sáu chỗ: {mã cầu, x, y}. Ba chỗ đầu bên trái, ba chỗ sau bên phải.
     *
     * Cố ý trộn cầu khác nhau ở mỗi chỗ và mỗi khung: dùng chung một quả cho cả sáu chỗ thì nhìn
     * như sáu hạt đậu xếp hàng, mất hẳn vẻ lơ lửng.
     */
    /**
     * Ba khung. Mỗi khung ghi **ba chỗ bên PHẢI**; bên trái lấy đối xứng gương qua trục giữa.
     *
     * Lấy bên phải làm gốc chứ không phải bên trái -- cách xếp bên phải mới là cách nhìn thuận
     * mắt, bên trái chỉ soi lại. Xếp tay cả hai bên thì lệch ngay.
     *
     * Mỗi chỗ: {mã cầu, x, y}.
     */
    private static final int[][][] KHUNG = {
            { {3, 34, 3}, {2, 39, 14}, {0, 36, 24} },
            { {5, 38, 5}, {4, 35, 13}, {2, 39, 23} },
            { {2, 36, 2}, {0, 40, 15}, {3, 33, 22} },
    };

    private GhepCau() {
    }

    public static void main(String[] args) throws Exception {
        String thuMucEffect = args[0], thuMucSmall = args[1];
        int idDau = Integer.parseInt(args[2]);
        for (int z = 1; z <= 4; z++) {
            BufferedImage tam = ImageIO.read(new File(thuMucEffect + "/" + z + "/221.png"));
            for (int k = 0; k < KHUNG.length; k++) {
                BufferedImage ra = new BufferedImage(W * z, H * z, BufferedImage.TYPE_INT_ARGB);
                Graphics2D g = ra.createGraphics();
                for (int[] cho : KHUNG[k]) {
                    int[] c = CAU[cho[0]];
                    BufferedImage q = tam.getSubimage(c[0] * z, c[1] * z, c[2] * z, c[3] * z);
                    // bên phải: đúng chỗ đã ghi
                    g.drawImage(q, cho[1] * z, cho[2] * z, null);
                    // Bên trái: lật gương, nhưng lệch xuống vài điểm ảnh và mỗi khung một khác.
                    // Đối xứng tuyệt đối nhìn ra ngay là dán máy; lệch nhẹ thì mắt đọc thành sáu
                    // quả đang lơ lửng quanh người.
                    int trai = (W - cho[1] - c[2]) * z;
                    int lech = LECH_TRAI[k % LECH_TRAI.length];
                    g.drawImage(q, trai + c[2] * z, (cho[2] + lech) * z, -c[2] * z, c[3] * z, null);
                }
                g.dispose();
                File f = new File(thuMucSmall + "/" + z + "/Small" + (idDau + k) + ".png");
                ImageIO.write(ra, "png", f);
                if (z == 1) {
                    System.out.println("  khung " + k + " -> " + f.getName() + "  " + (W) + "x" + H);
                }
            }
        }
    }
}
