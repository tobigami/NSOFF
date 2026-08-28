import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Vẽ ảnh cho những vật phẩm được thêm vào cơ sở dữ liệu nhưng chưa kèm phần đồ hoạ.
 *
 * Client xin ảnh theo số hiệu rồi máy chủ đọc thẳng một tệp:
 *
 *     Data/Img/Small/<cỡ>/Small<số hiệu>.png
 *
 * Không bảng tra, không gói dữ liệu, không phải nâng version -- đặt tệp vào đúng chỗ là xong.
 * Bốn cỡ 1..4 là cùng một hình nhân 1, 2, 3, 4 lần, nên ở đây vẽ ở lưới 20x20 rồi phóng theo kiểu
 * lấy điểm gần nhất; phóng mượt sẽ làm nhoè, mất chất pixel của bộ ảnh gốc.
 *
 * Hình vẽ bằng bảng ký tự cho dễ đọc và dễ sửa: mỗi ký tự là một màu trong bảng của chính hình đó,
 * dấu chấm là trong suốt. Không cố vẽ đẹp -- chỉ cần nhìn ra đó là loại gì và phân biệt được với
 * món bên cạnh.
 *
 * Usage: MakeIcons <thư mục Data/Img/Small>
 */
public final class MakeIcons {

    /** Bí kíp: cuộn giấy sẫm, giữa có con mắt đỏ. */
    private static final String[] BI_KIP = {
        "....................",
        "....................",
        "..oooooooooooooooo..",
        "..oVVVVVVVVVVVVVVo..",
        "..oooooooooooooooo..",
        "..oddddddddddddddo..",
        "..oddddddddddddddo..",
        "..oddddRRRRRRddddo..",
        "..odddRWWWWWWRdddo..",
        "..odddRWWEEWWRdddo..",
        "..odddRWWEEWWRdddo..",
        "..odddRWWWWWWRdddo..",
        "..oddddRRRRRRddddo..",
        "..oddddddddddddddo..",
        "..oddddddddddddddo..",
        "..oooooooooooooooo..",
        "..oVVVVVVVVVVVVVVo..",
        "..oooooooooooooooo..",
        "....................",
        "...................."
    };


    /** Phiếu: tấm vé có khía hai bên, dải màu ở giữa để phân biệt mức giảm. */
    private static final String[] PHIEU = {
        "....................",
        "....................",
        "..oooooooooooooooo..",
        "..oYYYYYYYYYYYYYYo..",
        "..oYYYYYYYYYYYYYYo..",
        "o.oYYCCCCCCCCCCYYo.o",
        "..oYYCCCCCCCCCCYYo..",
        "..oYYCCCCCCCCCCYYo..",
        "o.oYYCCCCCCCCCCYYo.o",
        "..oYYYYYYYYYYYYYYo..",
        "..oYYYYYYYYYYYYYYo..",
        "..oooooooooooooooo..",
        "....................",
        "....................",
        "....................",
        "....................",
        "....................",
        "....................",
        "....................",
        "...................."
    };

    /** Cỏ thuốc: thân xanh, hoa ở ngọn. */
    private static final String[] CO_THUOC = {
        "....................",
        "........FFF.........",
        ".......FFFFF........",
        "......FFFHFFF.......",
        ".......FFFFF........",
        "........FFF.........",
        ".......GsG..........",
        "......G.s.G.........",
        ".....G..s..G........",
        "......G.s.G.........",
        ".......GsG..........",
        "........s...........",
        "........s...........",
        ".....oooooooo.......",
        "....oBBBBBBBBo......",
        "....oBBBBBBBBo......",
        ".....oooooooo.......",
        "....................",
        "....................",
        "...................."
    };

    /** Cát: đống cát nhiều màu. */
    private static final String[] CAT = {
        "....................",
        "....................",
        "....................",
        "........1...........",
        ".......121..........",
        "......12321.........",
        ".....1234321........",
        "....123454321.......",
        "...12345554321......",
        "..1234555554321.....",
        ".123455555554321....",
        "12345555555554321...",
        "ooooooooooooooooo...",
        "....................",
        "....................",
        "....................",
        "....................",
        "....................",
        "....................",
        "...................."
    };

    /** Vĩ thú: viên ngọc tròn, số chấm quanh viền cho biết mấy đuôi. */
    private static final String[] VI_THU = {
        "....................",
        "......oooooo........",
        ".....oCCCCCCo.......",
        "....oCCCCCCCCo......",
        "...oCCCWWCCCCCo.....",
        "...oCCWWWWCCCCo.....",
        "...oCCWWWWCCCCo.....",
        "...oCCCWWCCCCCo.....",
        "....oCCCCCCCCo......",
        ".....oCCCCCCo.......",
        "......oooooo........",
        "....................",
        "....................",
        "....................",
        "....................",
        "....................",
        "....................",
        "....................",
        "....................",
        "...................."
    };

    public static void main(String[] args) throws Exception {
        File goc = new File(args.length > 0 ? args[0] : "Data/Img/Small");

        // Chín vĩ thú, mỗi con một màu, kèm số đuôi vẽ thành chấm dưới viên ngọc.
        int[] mauViThu = { 0xE05555, 0xE08A2B, 0x4FAF4F, 0x3F7FE0, 0xB05FD0,
                           0xE0C040, 0x50C0B0, 0x8A6A4F, 0xE06AA0 };
        Map<Integer, int[]> viec = new LinkedHashMap<>();   // số hiệu ảnh -> {kiểu, tham số}
        for (int i = 0; i < 9; i++) {
            viec.put(3239 + i, new int[] { 4, mauViThu[i], i + 1 });   // sơ cấp
            viec.put(3248 + i, new int[] { 4, mauViThu[i], i + 1 });   // bảo bảo sơ cấp
        }
        int[] mauPhieu = { 0x7FBF3F, 0x4FAF6F, 0x3F9FBF, 0x3F6FD0, 0x8A5FD0, 0xD04F8A };
        for (int i = 0; i < 6; i++) {
            viec.put(3382 + i, new int[] { 1, mauPhieu[i], 0 });       // phiếu giảm giá 10..60%
        }
        viec.put(3599, new int[] { 2, 0xF0D040, 0 });                  // Kim tước thảo, hoa vàng
        viec.put(3600, new int[] { 2, 0xB060D0, 0 });                  // Tử hoa địa đinh, hoa tím
        viec.put(6501, new int[] { 3, 0, 0 });                         // Cát ngũ sắc
        viec.put(6502, new int[] { 0, 0, 0 });                         // Tà thuật Uchi

        int n = 0;
        for (Map.Entry<Integer, int[]> e : viec.entrySet()) {
            BufferedImage nen = ve(e.getValue()[0], e.getValue()[1], e.getValue()[2]);
            for (int co = 1; co <= 4; co++) {
                File ra = new File(goc, co + "/Small" + e.getKey() + ".png");
                ImageIO.write(phong(nen, co), "png", ra);
            }
            n++;
        }
        System.out.println("  đã vẽ " + n + " ảnh, mỗi ảnh 4 cỡ");
    }

    private static BufferedImage ve(int kieu, int mau, int soDuoi) {
        String[] hinh;
        Map<Character, Integer> bang = new LinkedHashMap<>();
        bang.put('o', 0xFF2A1F14);                      // viền nâu sẫm, dùng chung
        switch (kieu) {
            case 0:
                hinh = BI_KIP;
                bang.put('V', 0xFF6B4A2A);              // mép cuộn
                bang.put('d', 0xFFE8D8B0);              // mặt giấy
                bang.put('R', 0xFFB02020);              // vòng đỏ
                bang.put('W', 0xFFF0F0F0);              // lòng trắng mắt
                bang.put('E', 0xFF101010);              // con ngươi
                break;
            case 1:
                hinh = PHIEU;
                bang.put('Y', 0xFFF0E0A0);              // nền vé
                bang.put('C', 0xFF000000 | mau);        // dải màu theo mức giảm
                break;
            case 2:
                hinh = CO_THUOC;
                bang.put('F', 0xFF000000 | mau);        // cánh hoa
                bang.put('H', 0xFFFFF4C0);              // nhuỵ
                bang.put('G', 0xFF3F8F3F);              // lá
                bang.put('s', 0xFF2F6F2F);              // thân
                bang.put('B', 0xFF8A6A4F);              // chậu
                break;
            case 3:
                hinh = CAT;
                bang.put('1', 0xFFE0C060);
                bang.put('2', 0xFFE08A40);
                bang.put('3', 0xFFC05050);
                bang.put('4', 0xFF6080C0);
                bang.put('5', 0xFF70B070);
                break;
            default:
                hinh = VI_THU;
                bang.put('C', 0xFF000000 | mau);        // thân ngọc
                bang.put('W', 0xFFFFFFFF);              // đốm sáng
                break;
        }
        BufferedImage img = new BufferedImage(20, 20, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < hinh.length && y < 20; y++) {
            for (int x = 0; x < hinh[y].length() && x < 20; x++) {
                Integer c = bang.get(Character.valueOf(hinh[y].charAt(x)));
                if (c != null) {
                    img.setRGB(x, y, c.intValue());
                }
            }
        }
        // Số đuôi: hàng chấm ngay dưới viên ngọc, căn giữa để nhìn là đếm được.
        for (int i = 0; i < soDuoi; i++) {
            // Mỗi đuôi là một vạch dọc 1 pixel, cách nhau 1 pixel -- vẽ dày hơn thì 9 đuôi dính
            // liền thành một thanh, không đếm được nữa.
            int x = 10 - soDuoi + i * 2;
            if (x >= 0 && x < 20) {
                img.setRGB(x, 12, 0xFF2A1F14);
                img.setRGB(x, 13, 0xFF000000 | mau);
                img.setRGB(x, 14, 0xFF2A1F14);
            }
        }
        return img;
    }

    /** Phóng theo kiểu lấy điểm gần nhất, giữ đúng nét pixel. */
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
