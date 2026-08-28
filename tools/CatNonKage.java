import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;

/**
 * Cắt ba khung nón Kage từ tấm hiệu ứng 205 thành ảnh mảnh ghép riêng.
 *
 * Bộ Kage trong dữ liệu chỉ tồn tại ở dải ảnh HIỆU ỨNG, nơi hệ trang phục không với tới. Nhưng
 * hệ MẶT NẠ thì đọc từ nj_part -> nj_image -> file Small<id>.png, và đường đó đã chứng minh chạy
 * được với mặt nạ Itachi. Chỗ này chuyển hình từ dải hiệu ứng sang dải mảnh ghép.
 *
 * Toạ độ ô lấy thẳng từ cột sprites của effect_data 205 nên không phải đoán chỗ cắt. Tấm ảnh có
 * đủ bốn cỡ và đúng bội số 1:2:3:4, nên toạ độ chỉ cần nhân lên theo cỡ.
 *
 * Ba khung được chọn sau khi phóng to nhìn từng ô: ô 0 nhìn thẳng, ô 2 nhìn nghiêng, ô 10 là tư
 * thế còn lại -- khớp đúng bố cục mảnh mặt nạ (thẳng, nghiêng, nghiêng, đứng yên).
 *
 * Usage: CatNonKage <thư mục Data/Img> <id ảnh đầu tiên>
 */
public final class CatNonKage {

    /** Ô trên tấm cỡ 1: x, y, rộng, cao -- chép từ effect_data.sprites của hiệu ứng 205. */
    private static final int[][] O = {
        { 0, 57, 27, 22 },     // ô 0  -- nhìn thẳng
        { 29, 74, 26, 21 },    // ô 2  -- nhìn nghiêng
        { 29, 29, 27, 23 },    // ô 10 -- đứng yên
    };

    public static void main(String[] args) throws Exception {
        File goc = new File(args[0]);
        int idDau = Integer.parseInt(args[1]);
        for (int z = 1; z <= 4; z++) {
            File sheet = new File(goc, "Effect/" + z + "/205.png");
            BufferedImage im = ImageIO.read(sheet);
            for (int i = 0; i < O.length; i++) {
                int x = O[i][0] * z, y = O[i][1] * z, w = O[i][2] * z, h = O[i][3] * z;
                if (x + w > im.getWidth() || y + h > im.getHeight()) {
                    System.out.println("!! ô " + i + " vượt tấm cỡ " + z);
                    return;
                }
                BufferedImage cat = im.getSubimage(x, y, w, h);
                File ra = new File(goc, "Small/" + z + "/Small" + (idDau + i) + ".png");
                ImageIO.write(cat, "png", ra);
                if (z == 1) {
                    System.out.println("  ảnh " + (idDau + i) + ": " + w + "x" + h);
                }
            }
        }
        System.out.println("  đã xuất " + O.length + " ảnh cho cả 4 cỡ");
    }
}
