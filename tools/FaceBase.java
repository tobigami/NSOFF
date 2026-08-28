import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;

/**
 * Bóc phần khuôn mặt dùng chung của các mặt nạ.
 *
 * Mọi mặt nạ trong game vẽ trên cùng một khuôn mặt: chỉ tóc và phụ kiện khác nhau, còn mắt mũi
 * miệng thì trùng khít từng pixel. Công cụ này giao nhiều sprite lại, giữ những điểm giống hệt
 * nhau ở MỌI ảnh và bỏ phần còn lại -- kết quả là cái khuôn để vẽ mặt nạ mới lên trên.
 *
 * Usage: FaceBase <ảnh ra> <ảnh 1> <ảnh 2> [ảnh 3...]
 */
public final class FaceBase {

    private static final int NGUONG = 96;

    public static void main(String[] args) throws Exception {
        BufferedImage[] anh = new BufferedImage[args.length - 1];
        for (int i = 1; i < args.length; i++) {
            anh[i - 1] = ImageIO.read(new File(args[i]));
        }
        int w = anh[0].getWidth(), h = anh[0].getHeight();
        for (BufferedImage m : anh) {
            if (m.getWidth() != w || m.getHeight() != h) {
                System.out.println("!! các ảnh phải cùng cỡ");
                return;
            }
        }
        BufferedImage ra = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        int giu = 0;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int c = anh[0].getRGB(x, y);
                if (((c >>> 24) & 0xFF) < NGUONG) {
                    continue;
                }
                boolean chung = true;
                for (BufferedImage m : anh) {
                    int c2 = m.getRGB(x, y);
                    if (((c2 >>> 24) & 0xFF) < NGUONG || (c2 & 0xFFFFFF) != (c & 0xFFFFFF)) {
                        chung = false;
                        break;
                    }
                }
                if (chung) {
                    ra.setRGB(x, y, c);
                    giu++;
                }
            }
        }
        ImageIO.write(ra, "png", new File(args[0]));
        System.out.println("  " + new File(args[0]).getName() + ": giữ " + giu
                + " điểm chung trong khung " + w + "x" + h);
    }
}
