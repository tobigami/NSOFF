import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;

/**
 * Cắt phần thân và chân của bộ Kage từ hai tấm hiệu ứng 206 và 207.
 *
 * Hai tấm này trộn lẫn mảnh trang phục với mảnh hiệu ứng xoáy tím, nên danh sách ô dưới đây là
 * kết quả nhìn từng mảnh sau khi phóng to, chỉ giữ lại mảnh áo trắng và quần đỏ.
 *
 * Toạ độ lấy thẳng từ cột sprites của effect_data. Tấm ảnh có đủ bốn cỡ theo đúng bội số 1:2:3:4
 * nên chỉ cần nhân toạ độ lên.
 *
 * Usage: CatBoKage <thư mục Data/Img> <id ảnh đầu cho thân> <id ảnh đầu cho chân>
 */
public final class CatBoKage {

    /** Ô áo choàng trắng trên tấm 206: x, y, rộng, cao. */
    private static final int[][] THAN = {
        {43,49,21,14},{0,81,15,12},{0,55,21,13},{27,68,15,12},{43,89,19,12},{21,55,15,13},
        {15,81,18,12},{0,0,26,21},{0,40,18,15},{0,93,22,12},{13,68,14,13},{43,76,19,13},
        {43,17,25,16},{43,33,25,16},{26,0,17,21},{43,0,24,17},
    };

    /** Ô quần và giày đỏ trên tấm 207. */
    private static final int[][] CHAN = {
        {42,116,14,8},{28,106,14,12},{57,106,20,10},{42,106,14,10},{0,109,21,10},
        {83,113,16,10},{83,102,13,11},{83,74,11,14},{83,88,13,14},
    };

    public static void main(String[] args) throws Exception {
        File goc = new File(args[0]);
        int idThan = Integer.parseInt(args[1]), idChan = Integer.parseInt(args[2]);
        xuat(goc, 206, THAN, idThan, "thân");
        xuat(goc, 207, CHAN, idChan, "chân");
    }

    private static void xuat(File goc, int tam, int[][] o, int idDau, String ten) throws Exception {
        for (int z = 1; z <= 4; z++) {
            BufferedImage im = ImageIO.read(new File(goc, "Effect/" + z + "/" + tam + ".png"));
            for (int i = 0; i < o.length; i++) {
                int x = o[i][0] * z, y = o[i][1] * z, w = o[i][2] * z, h = o[i][3] * z;
                if (x + w > im.getWidth() || y + h > im.getHeight()) {
                    System.out.println("!! ô " + i + " của tấm " + tam + " vượt biên ở cỡ " + z);
                    return;
                }
                ImageIO.write(im.getSubimage(x, y, w, h), "png",
                        new File(goc, "Small/" + z + "/Small" + (idDau + i) + ".png"));
            }
        }
        System.out.println("  " + ten + ": " + o.length + " ảnh, id " + idDau + ".." + (idDau + o.length - 1));
    }
}
